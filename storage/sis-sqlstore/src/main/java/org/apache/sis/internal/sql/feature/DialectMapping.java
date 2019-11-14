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
import java.sql.SQLException;
import java.util.Optional;

import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.metadata.sql.Dialect;
import org.apache.sis.setup.GeometryLibrary;

/**
 * Specifies mapping between values stored in database and Java. It is particularly useful for geo-related information,
 * as geometries.
 *
 * @implNote For now, only two example implementations can be found in SIS:
 * <ul>
 *     <li>{@link PostGISMapping PostGIS (geographic) specific mapping}</li>
 *     <li>{@link ANSIMapping Default mapping for simple types (text, numbers, temporal, etc.)}</li>
 * </ul>
 */
public interface DialectMapping extends SQLCloseable {

    /**
     *
     * @return Provider that served to create this mapping. Should never be null.
     */
    Spi getSpi();

    /**
     * Analyze given column definition to determine a mapping to java values.
     * @param columnDefinition Information about the column to extract values from and expose through Java API.
     * @return an empty shell if this dialect is not capable of founding a proper mapping,
     */
    Optional<ColumnAdapter<?>> getMapping(final SQLColumn columnDefinition);

    /**
     * TODO: expose that through Service loader.
     * @implNote See {@link PostGISMapping} for an example.
     */
    interface Spi {
        /**
         * Checks if database is compliant with this service specification, and create a mapper in such case.
         *
         * @param geometryDriver The library to use if the given mapping needs to create geometries. It mainly serves
         *                       to obtain a geometry factory through {@link Geometries#implementation(GeometryLibrary)}.
         * @param c The connection to use to connect to the database. It will be read-only.
         * @return A component compatible with database of given connection, or nothing if the database is not supported
         * by this component.
         * @throws SQLException If an error occurs while fetching information from database.
         */
        Optional<DialectMapping> create(final GeometryLibrary geometryDriver, final Connection c) throws SQLException;

        /**
         *
         * @return The target dialect accepted by this service. This is a vital information, because the system will
         * only ask for a mapping if its service provider match current database dialect.
         */
        Dialect getDialect();
    }
}
