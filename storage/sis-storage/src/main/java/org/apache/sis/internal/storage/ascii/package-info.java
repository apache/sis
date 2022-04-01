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
 * {@link org.apache.sis.storage.DataStore} implementation for ESRI ASCII grid format.
 * This is a very simple format for reading and writing single-banded raster data.
 * As the "ASCII" name implies, files are text files in US-ASCII character encoding
 * no matter what the {@link org.apache.sis.setup.OptionKey#ENCODING} value is,
 * and numbers are parsed or formatted according the US locale no matter
 * what the {@link org.apache.sis.setup.OptionKey#LOCALE} value is.
 *
 * <p>ASCII grid files contains a header before the actual data.
 * The header contains (<var>key</var> <var>value</var>) pairs,
 * one pair per line and using spaces as separator between keys and values
 * (Apache SIS accepts also {@code '='} and {@code ':'} as separators).
 * The valid keys are listed in the table below
 * (note that some of them are extensions to the ESRI ASCII Grid format).</p>
 *
 * <table class="sis">
 *   <caption>Recognized keywords in ASCII Grid header</caption>
 *   <tr>
 *     <th>Keyword</th>
 *     <th>Value type</th>
 *     <th>Obligation</th>
 *   </tr>
 *   <tr>
 *     <td>{@code NCOLS}</td>
 *     <td>Integer</td>
 *     <td>Mandatory</td>
 *   </tr>
 *   <tr>
 *     <td>{@code NROWS}</td>
 *     <td>Integer</td>
 *     <td>Mandatory</td>
 *   </tr>
 *   <tr>
 *     <td>{@code XLLCORNER} or {@code XLLCENTER}</td>
 *     <td>Floating point</td>
 *     <td>Mandatory</td>
 *   </tr>
 *   <tr>
 *     <td>{@code YLLCORNER} or {@code YLLCENTER}</td>
 *     <td>Floating point</td>
 *     <td>Mandatory</td>
 *   </tr>
 *   <tr>
 *     <td>{@code CELLSIZE}</td>
 *     <td>Floating point</td>
 *     <td>Mandatory, unless {@code DX} and {@code DY} are present</td>
 *   </tr>
 *   <tr>
 *     <td>{@code DX} and {@code DY}</td>
 *     <td>Floating point</td>
 *     <td>Accepted but non-standard</td>
 *   </tr>
 *   <tr>
 *     <td>{@code NODATA_VALUE}</td>
 *     <td>Floating point</td>
 *     <td>Optional</td>
 *   </tr>
 * </table>
 *
 * <h2>Extensions</h2>
 * The implementation in this package adds the following extensions
 * (some of them are taken from GDAL):
 *
 * <ul class="verbose">
 *   <li>Coordinate reference system specified by auxiliary {@code *.prj} file.
 *       If the format is WKT 1, the GDAL variant is used (that variant differs from
 *       the OGC 01-009 standard in their interpretation of units of measurement).</li>
 *   <li>{@code DX} and {@code DY} parameters in the header are used instead of {@code CELLSIZE}
 *       if the pixels are non-square.</li>
 *   <li>Lines in the header starting with {@code '#'} are ignored as comment lines.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
package org.apache.sis.internal.storage.ascii;
