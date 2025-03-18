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
     */
    WKB,

    /**
     * Use Well-Known Text (<abbr>WKT</abbr>) format.
     */
    WKT
}
