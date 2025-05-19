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
package org.apache.sis.storage.sql.feature;

import java.util.EnumMap;


/**
 * The encoding to use for reading or writing geometries from a {@code ResultSet}, in preference order.
 * In theory, the use of a binary format should be more efficient. But some <abbr>JDBC</abbr> drivers
 * have issues with extracting bytes from geometry columns. It also happens sometime that, surprisingly
 * the use of <abbr>WKT</abbr> appear to be faster than <abbr>WKB</abbr> with some databases.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public enum GeometryEncoding {
    /**
     * Use Well-Known Binary (<abbr>WKB</abbr>) format.
     * Includes the Geopackage geometry encoding extension, which is identified by the "GP" prefix.
     *
     * <p>If the extended <abbr>WKB</abbr> format is supported, then {@code SQLStore} will use that function
     * despite the fact that it is non-standard, in order to get the coordinate reference system associated
     * with the geometry. Otherwise, the <abbr>SQLMM</abbr> standard function for fetching this value from
     * a database is {@code "ST_AsBinary"}. However, some databases expect {@code "ST_AsWKB"} instead.</p>
     */
    WKB(new String[] {"ST_AsEWKB", "ST_AsBinary", "ST_AsWKB"}),

    /**
     * Use Well-Known Text (<abbr>WKT</abbr>) format.
     *
     * <p>The <abbr>SQLMM</abbr> standard function for fetching this value from a database is {@code "ST_AsText"}.
     * However, some databases expect {@code "ST_AsWKT"} instead.</p>
     */
    WKT(new String[] {"ST_AsText", "ST_AsWKT"});

    /**
     * The functions to use, in preference order, for getting the value from the database.
     */
    private final String[] readers;

    /**
     * Creates a new enumeration value.
     *
     * @param readers  the functions to use, in preference order, for getting the value from the database.
     */
    private GeometryEncoding(final String[] readers) {
        this.readers = readers;
    }

    /**
     * All enumeration values, fetching once for avoiding multiple array creations.
     */
    private static final GeometryEncoding[] VALUES = values();

    /**
     * Creates an initially empty array to use as argument in the calls to {@code checkSupport(…)} method.
     * Should be considered as an opaque storage mechanism used by this class only.
     *
     * @see #checkSupport(String[][], String)
     */
    static String[][] initial() {
        return new String[VALUES.length][];
    }

    /**
     * Invoked in a loop over for identifying which functions are supported for fetching or storing geometries.
     *
     * @param accessors  the array created by {@link #initial()}.
     * @param function   a function of the database.
     *
     * @todo Add a loop over {@code writers} after we implemented write support.
     */
    static void checkSupport(final String[][] accessors, final String function) {
        for (int j=0; j < VALUES.length; j++) {
            final GeometryEncoding encoding = VALUES[j];
            final String[] readers = encoding.readers;
            for (int i=0; i < readers.length; i++) {
                if (readers[i].equalsIgnoreCase(function)) {
                    String[] functions = accessors[j];
                    if (functions == null) {
                        functions = new String[readers.length];
                        accessors[j] = functions;
                    }
                    functions[i] = function;    // Keep the case used by the database.
                }
            }
        }
    }

    /**
     * Puts in the given map the preferred functions for fetching or storing geometries.
     * If many functions are supported, the standard one is preferred.
     *
     * @param  accessors  the array created by {@link #initial()}.
     * @param  target     where to store the preferred functions for fetching or storing geometries.
     *
     * @todo Add an argument for specifying whether read or write operation is desired.
     */
    static void store(final String[][] accessors, final EnumMap<GeometryEncoding, String> target) {
next:   for (int j=0; j < accessors.length; j++) {
            final GeometryEncoding encoding = VALUES[j];
            final String[] functions = accessors[j];
            if (functions != null) {
                for (String function : functions) {
                    if (function != null) {
                        target.put(encoding, function);     // We want the function at the lowest index.
                        continue next;
                    }
                }
            }
            target.remove(encoding);
        }
    }
}
