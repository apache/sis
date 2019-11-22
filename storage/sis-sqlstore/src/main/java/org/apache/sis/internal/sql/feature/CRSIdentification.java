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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.collection.Cache;

/**
 *
 * @author Alexis Manin (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
final class CRSIdentification implements SQLCloseable {

    final PreparedStatement wktFromSrid;
    private final WKTFormat wktReader;

    private final Cache<Integer, CoordinateReferenceSystem> sessionCache;

    CRSIdentification(final Connection c, final Cache<Integer, CoordinateReferenceSystem> sessionCache) throws SQLException {
        wktFromSrid = c.prepareStatement("SELECT auth_name, auth_srid, srtext FROM spatial_ref_sys WHERE srid=?");
        wktReader = new WKTFormat(null, null);
        wktReader.setConvention(Convention.WKT1_COMMON_UNITS);
        this.sessionCache = sessionCache;
    }

    /**
     * Try to fetch spatial system relative to given SRID.
     *
     * @param pgSrid The SRID as defined by the database (see
     *               <a href="http://postgis.refractions.net/documentation/manual-1.3/ch04.html#id2571265">Official PostGIS documentation</a> for details).
     * @return If input was 0 or less, a null value is returned. Otherwise, return the CRS decoded from database WKT.
     * @throws IllegalArgumentException If given SRID is above 0, but no coordinate system definition can be found for
     * it in the database, or found object is not a database, or no WKT is available, but authority code is not
     * supported by SIS.
     * @throws IllegalStateException If more than one match is found for given SRID.
     */
    CoordinateReferenceSystem fetchCrs(int pgSrid) throws IllegalArgumentException {
        if (pgSrid <= 0) return null;

        return sessionCache.computeIfAbsent(pgSrid, this::fetch);
    }

    private CoordinateReferenceSystem fetch(final int pgSrid) {
        try {
            wktFromSrid.setInt(1, pgSrid);
            try (ResultSet result = wktFromSrid.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("No entry found for SRID " + pgSrid);
                final String authority = result.getString(1);
                final int authorityCode = result.getInt(2);
                final String pgWkt = result.getString(3);

                // That should never happen, but if it does, there's a serious problem !
                if (result.next())
                    throw new IllegalStateException("More than one definition available for SRID " + pgSrid);

                if (pgWkt == null || pgWkt.trim().isEmpty()) {
                    try {
                        return CRS.getAuthorityFactory(authority).createCoordinateReferenceSystem(Integer.toString(authorityCode));
                    } catch (FactoryException e) {
                        throw new IllegalArgumentException(String.format(
                                "Input SRID (%d) does not provide any WKT, but its authority code (%s:%d) is not supported by SIS",
                                pgSrid, authority, authorityCode
                        ), e);
                    }
                }
                final Object parsedWkt = wktReader.parseObject(pgWkt);
                if (parsedWkt instanceof CoordinateReferenceSystem) {
                    return (CoordinateReferenceSystem) parsedWkt;
                } else throw new ParseException(String.format(
                        "WKT of given SRID cannot be interprated as a CRS.%nInput SRID: %d%nOutput type: %s",
                        pgSrid, parsedWkt.getClass().getCanonicalName()
                ), 0);
            } finally {
                wktFromSrid.clearParameters();
            }
        } catch (SQLException | ParseException e) {
            throw new BackingStoreException(e);
        }
    }

    @Override
    public void close() throws SQLException {
        wktFromSrid.close();
    }
}
