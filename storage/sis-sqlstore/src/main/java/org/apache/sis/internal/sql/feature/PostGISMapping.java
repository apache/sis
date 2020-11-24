/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.internal.sql.feature;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.metadata.sql.Dialect;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.internal.system.Modules;

import static org.apache.sis.internal.sql.feature.OGC06104r4.Reader;
import static org.apache.sis.internal.sql.feature.OGC06104r4.getGeometricClass;

/**
 * Maps geometric values between PostGIS natural representation (Hexadecimal EWKT) and SIS.
 * For more information about EWKB format, see:
 * <ul>
 *     <li><a href="http://postgis.refractions.net/documentation/manual-1.3/ch04.html#id2571020">PostGIS manual, section 4.1.2</a></li>
 *     <li><a href="https://www.ibm.com/support/knowledgecenter/SSGU8G_14.1.0/com.ibm.spatial.doc/ids_spat_285.htm">IBM WKB description</a></li>
 * </ul>
 *
 * @author Alexis Manin (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class PostGISMapping implements DialectMapping {

    final PostGISMapping.Spi spi;
    final GeometryIdentification identifyGeometries;
    final GeometryIdentification identifyGeographies;

    final Geometries library;

    /**
     * A cache valid ONLY FOR A DATASOURCE. IT'S IMPORTANT ! Why ? Because :
     * <ul>
     *     <li>CRS definition could differ between databases (PostGIS version, user alterations, etc.)</li>
     *     <li>Avoid inter-database locking</li>
     * </ul>
     */
    final Cache<Integer, CoordinateReferenceSystem> sessionCache;

    private PostGISMapping(final PostGISMapping.Spi spi, GeometryLibrary geometryDriver, Connection c) throws SQLException {
        this.spi = spi;
        sessionCache = new Cache<>(7, 0, true);
        this.identifyGeometries = new GeometryIdentification(c, "geometry_columns", "f_geometry_column", "type", sessionCache);
        this.identifyGeographies = new GeometryIdentification(c, "geography_columns", "f_geography_column", "type", sessionCache);

        this.library = Geometries.implementation(geometryDriver);
    }

    @Override
    public Spi getSpi() {
        return spi;
    }

    @Override
    public Optional<ColumnAdapter<?>> getMapping(SQLColumn definition) {
        return Optional.ofNullable(forGeometry(definition));
    }

    private ColumnAdapter<?> forGeometry(SQLColumn definition) {
        if (definition.typeName == null) return null;
        switch (definition.typeName.trim().toLowerCase()) {
            case "geometry":
                return forGeometry(definition, identifyGeometries);
            case "geography":
                return forGeometry(definition, identifyGeographies);
            default: return null;
        }
    }

    private ColumnAdapter<?> forGeometry(SQLColumn definition, GeometryIdentification ident) {
        // In case of a computed column, geometric definition could be null.
        final GeometryIdentification.GeometryColumn geomDef;
        try {
            geomDef = ident.fetch(definition).orElse(null);
        } catch (SQLException | ParseException e) {
            throw new BackingStoreException(e);
        }
        String geometryType = geomDef == null ? null : geomDef.type;
        final Class geomClass = getGeometricClass(geometryType, library);

        if (geomDef == null || geomDef.crs == null) {
            return new HexEWKBDynamicCrs(geomClass);
        } else {
            // TODO: activate optimisation : WKB is lighter, but we need to modify user query, and to know CRS in advance.
            //geometryDecoder = new WKBReader(geomDef.crs);
            return new HexEWKBFixedCrs(geomClass, geomDef.crs);
        }
    }

    @Override
    public void close() throws SQLException {
        identifyGeometries.close();
    }

    public static final class Spi implements DialectMapping.Spi {

        @Override
        public Optional<DialectMapping> create(GeometryLibrary geometryDriver, Connection c) throws SQLException {
            try {
                checkPostGISVersion(c);
            } catch (SQLException e) {
                final Logger logger = Logging.getLogger(Modules.SQL);
                logger.warning("No compatible PostGIS version found. Binding deactivated. See debug logs for more information");
                logger.log(Level.FINE, "Cannot determine PostGIS version", e);
                return Optional.empty();
            }
            return Optional.of(new PostGISMapping(this, geometryDriver, c));
        }

        private void checkPostGISVersion(final Connection c) throws SQLException {
            try (
                    Statement st = c.createStatement();
                    ResultSet result = st.executeQuery("SELECT PostGIS_version();");
            ) {
                result.next();
                final String pgisVersion = result.getString(1);
                if (!pgisVersion.startsWith("2.")) throw new SQLException("Incompatible PostGIS version. Only 2.x is supported for now, but database declares: ");
            }
        }

        @Override
        public Dialect getDialect() {
            return Dialect.POSTGRESQL;
        }
    }

    private final class HexEWKBFixedCrs extends Reader {
        final CoordinateReferenceSystem crsToApply;

        public HexEWKBFixedCrs(Class geomClass, CoordinateReferenceSystem crsToApply) {
            super(geomClass);
            this.crsToApply = crsToApply;
        }

        @Override
        public SQLBiFunction prepare(Connection target) {
            return new HexEWKBReader(new EWKBReader(library).forCrs(crsToApply));
        }

        @Override
        public Optional<CoordinateReferenceSystem> getCrs() {
            return Optional.ofNullable(crsToApply);
        }
    }

    private final class HexEWKBDynamicCrs extends Reader {

        public HexEWKBDynamicCrs(Class geomClass) {
            super(geomClass);
        }

        @Override
        public SQLBiFunction prepare(Connection target) {
            // TODO: this component is not properly closed. As connection closing should also close this component
            // statement, it should be Ok.However, a proper management would be better.
            final CRSIdentification crsIdent;
            try {
                crsIdent = new CRSIdentification(target, sessionCache);
            } catch (SQLException e) {
                throw new BackingStoreException(e);
            }
            return new HexEWKBReader(
                    new EWKBReader(library)
                            .withResolver(crsIdent::fetchCrs)
            );
        }
    }

    private static final class HexEWKBReader implements SQLBiFunction<ResultSet, Integer, Object> {

        final EWKBReader reader;

        private HexEWKBReader(EWKBReader reader) {
            this.reader = reader;
        }

        @Override
        public Object apply(ResultSet resultSet, Integer integer) throws SQLException {
            final String hexa = resultSet.getString(integer);
            return hexa == null ? null : reader.readHexa(hexa);
        }
    }
}
