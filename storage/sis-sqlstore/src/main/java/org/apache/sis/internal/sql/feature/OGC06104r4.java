package org.apache.sis.internal.sql.feature;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Optional;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.metadata.sql.Dialect;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.collection.Cache;

/**
 * Geometric SQL mapping based on <a href="https://www.opengeospatial.org/standards/sfs">OpenGIS® Implementation Standard for Geographic information -Simple feature access -Part 2: SQL option</a>
 *
 * @implNote WARNING: This class will (almost certainly) not work as is. It provides a base implementation for geometry
 * access on any SQL simple feature compliant database, but the standard does not specify precisely what mode of
 * representation is the default (WKB or WKT). The aim is to base specific drivers on this class (see {@link PostGISMapping}
 * for an example).
 */
final class OGC06104r4 implements DialectMapping {

    final OGC06104r4.Spi spi;
    final GeometryIdentification identifyGeometries;

    final Geometries library;

    /**
     * A cache valid ONLY FOR A DATASOURCE. IT'S IMPORTANT ! Why ? Because :
     * <ul>
     *     <li>CRS definition could differ between databases (PostGIS version, user alterations, etc.)</li>
     *     <li>Avoid inter-database locking</li>
     * </ul>
     */
    final Cache<Integer, CoordinateReferenceSystem> sessionCache;

    private OGC06104r4(final OGC06104r4.Spi spi, Connection c) throws SQLException {
        this.spi = spi;
        sessionCache = new Cache<>(7, 0, true);
        this.identifyGeometries = new GeometryIdentification(c, sessionCache);

        this.library = Geometries.implementation(null);
    }

    @Override
    public OGC06104r4.Spi getSpi() {
        return spi;
    }

    @Override
    public Optional<ColumnAdapter<?>> getMapping(SQLColumn columnDefinition) {
        if (columnDefinition.typeName != null && "geometry".equalsIgnoreCase(columnDefinition.typeName)) {
            return Optional.of(forGeometry(columnDefinition));
        }

        return Optional.empty();
    }

    private ColumnAdapter<?> forGeometry(SQLColumn definition) {
        // In case of a computed column, geometric definition could be null.
        final GeometryIdentification.GeometryColumn geomDef;
        try {
            geomDef = identifyGeometries.fetch(definition).orElse(null);
        } catch (SQLException | ParseException e) {
            throw new BackingStoreException(e);
        }
        String geometryType = geomDef == null ? null : geomDef.type;
        final Class geomClass = getGeometricClass(geometryType, library);

        return new WKBReader(geomClass, geomDef == null ? null : geomDef.crs);
    }

    @Override
    public void close() throws SQLException {
        identifyGeometries.close();
    }

    static Class getGeometricClass(String geometryType, final Geometries library) {
        if (geometryType == null) return library.rootClass;

        // remove Z, M or ZM suffix
        if (geometryType.endsWith("M")) geometryType = geometryType.substring(0, geometryType.length()-1);
        if (geometryType.endsWith("Z")) geometryType = geometryType.substring(0, geometryType.length()-1);

        final Class geomClass;
        switch (geometryType) {
            case "POINT":
                geomClass = library.pointClass;
                break;
            case "LINESTRING":
                geomClass = library.polylineClass;
                break;
            case "POLYGON":
                geomClass = library.polygonClass;
                break;
            default: geomClass = library.rootClass;
        }
        return geomClass;
    }

    abstract static class Reader implements ColumnAdapter {

        final Class geomClass;

        public Reader(Class geomClass) {
            this.geomClass = geomClass;
        }

        @Override
        public Class getJavaType() {
            return geomClass;
        }
    }

    private final class WKBReader extends Reader implements SQLBiFunction<ResultSet, Integer, Object> {

        final CoordinateReferenceSystem crsToApply;

        private WKBReader(Class geomClass, CoordinateReferenceSystem crsToApply) {
            super(geomClass);
            this.crsToApply = crsToApply;
        }

        @Override
        public Object apply(ResultSet resultSet, Integer integer) throws SQLException {
            final byte[] bytes = resultSet.getBytes(integer);
            if (bytes == null) return null;
            // EWKB reader should be compliant with standard WKB format. However, it is not sure that database driver
            // will return WKB, so we should find a way to ensure SQL queries would use ST_AsBinary function.
            return new EWKBReader(library).forCrs(crsToApply).read(bytes);
        }

        @Override
        public SQLBiFunction prepare(Connection target) {
            return this;
        }

        @Override
        public Optional<CoordinateReferenceSystem> getCrs() {
            return Optional.ofNullable(crsToApply);
        }
    }

    public static final class Spi implements DialectMapping.Spi {

        @Override
        public Optional<DialectMapping> create(Connection c) throws SQLException {
            return Optional.of(new OGC06104r4(this, c));
        }

        @Override
        public Dialect getDialect() {
            return Dialect.ANSI;
        }
    }
}
