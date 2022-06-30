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

/**
 * {@link org.apache.sis.storage.DataStore} implementation for Coma Separated Values (CSV) files.
 * This package implements the following specifications (more may be added in any future version):
 *
 * <ul>
 *   <li><a href="http://docs.opengeospatial.org/is/14-084r2/14-084r2.html">OGC® Moving Features Encoding Extension:
 *     Simple Comma Separated Values (CSV)</a> with some rules relaxed. The Apache SIS implementation allows the CSV
 *     file to have no date, thus allowing use of OGC 14-084 syntax for static features in addition to moving ones.</li>
 *   <li><a href="http://www.ietf.org/rfc/rfc4180.txt">Common Format and MIME Type for Comma-Separated Values (CSV) Files</a>.
 *     This is supported indirectly since above OGC specification is an extension of this IETF specification.</li>
 * </ul>
 *
 * Example of moving features CSV file (adapted from OGC specification):
 *
 * {@preformat text
 *   &#64;stboundedby, urn:x-ogc:def:crs:EPSG::4326, 2D, 9.23 50.23, 9.27 50.31, 2012-01-17T12:33:41Z, 2012-01-17T12:37:00Z, sec
 *   &#64;columns,mfidref,trajectory,state,xsd:token,”type code”,xsd:integer
 *   a,  10, 150, 11.0 2.0 12.0 3.0, walking, 1
 *   b,  10, 190, 10.0 2.0 11.0 3.0, walking, 2
 *   a, 150, 190, 12.0 3.0 10.0 3.0, walking, 2
 *   c,  10, 190, 12.0 1.0 10.0 2.0 11.0 3.0, vehicle, 1
 * }
 *
 * <h2>Departures from OGC specification</h2>
 * Current implementation is not strictly compliant with the Moving Features specification.
 * Departures are:
 *
 * <ul>
 *   <li>Character encoding is not necessarily UTF-8 since a different encoding can be specified with
 *       {@link org.apache.sis.setup.OptionKey#ENCODING} in the {@link org.apache.sis.storage.StorageConnector}.
 *       If not specified, Apache SIS uses the Java platform default encoding (which is often UTF-8).</li>
 *   <li>The Apache SIS implementation does not replace the XML entities by the referenced characters.
 *       XML entities, if present, are included verbatim in the parsed text.</li>
 *   <li>The Apache SIS implementation does not replace the \\s, \\t, and \\b escape sequences by space, tab, and comma.
 *       Those escape sequences are included verbatim in the parsed text.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   0.7
 * @module
 */
package org.apache.sis.internal.storage.csv;
