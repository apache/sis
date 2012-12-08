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
 * Implementation of GeoAPI types from the {@link org.opengis.util} package.
 * All those types except {@code Factory} and {@code InternationalString} are
 * derived from the ISO 19103 specification. The main content of this package are:
 *
 * <ul>
 *   <li>Implementations of {@link org.opengis.util.InternationalString}:
 *     <ul>
 *       <li>{@link org.apache.sis.util.type.SimpleInternationalString} for wrapping a single {@link java.lang.String};</li>
 *       <li>{@link org.apache.sis.util.type.DefaultInternationalString} for providing many localizations in a {@link java.util.Map};</li>
 *       <li>{@link org.apache.sis.util.type.ResourceInternationalString} for providing localizations from a {@link java.util.ResourceBundle}.</li>
 *     </ul>
 *   </li>
 *   <li>Implementations of {@link org.opengis.util.GenericName}:
 *     <ul>
 *       <li>{@link org.apache.sis.util.type.DefaultLocalName} for identifier within a namespace.</li>
 *       <li>{@link org.apache.sis.util.type.DefaultScopedName} for a composite of a <cite>head</cite>
 *           name and a <cite>tail</cite> name.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * {@section Relationship between naming types}
 * Names may be {@linkplain org.apache.sis.util.type.AbstractName#toFullyQualifiedName()
 * fully qualified} (like {@code "org.opengis.util.Record"}), or they may be relative to a
 * {@linkplain org.apache.sis.util.type.AbstractName#scope() scope} (like {@code "util.Record"}
 * in the {@code "org.opengis"} scope). The illustration below shows all possible constructions
 * for {@code "org.opengis.util.Record"}:
 *
 * <blockquote><table border="1" cellpadding="15"><tr><td><table border="0" cellspacing="0">
 *   <tr>
 *     <th align="right">org</th>
 *     <th>.</th><th>opengis</th>
 *     <th>.</th><th>util</th>
 *     <th>.</th><th>Record</th>
 *     <th width="50"></th>
 *     <th>{@link org.apache.sis.util.type.AbstractName#scope() scope()}</th>
 *     <th>{@link org.apache.sis.util.type.AbstractName#getParsedNames() getParsedNames()}</th>
 *     <th width="50"></th>
 *     <th>Type</th>
 *   </tr>
 *
 *   <tr align="center">
 *     <td bgcolor="palegoldenrod" colspan="1"><font size="-1">{@linkplain org.apache.sis.util.type.AbstractName#head() head}</font></td><td></td>
 *     <td bgcolor="palegoldenrod" colspan="5"><font size="-1">{@linkplain org.apache.sis.util.type.DefaultScopedName#tail() tail}</font></td>
 *     <td rowspan="2"></td>
 *     <td rowspan="2" bgcolor="beige" align="left">{@linkplain org.apache.sis.util.type.DefaultNameSpace#isGlobal() global}</td>
 *     <td rowspan="2" bgcolor="beige" align="right">{@literal {"org", "opengis", "util", "Record"}}</td>
 *     <td rowspan="2"></td>
 *     <td rowspan="2">{@link org.apache.sis.util.type.DefaultScopedName ScopedName}</td>
 *   </tr>
 *   <tr align="center">
 *     <td bgcolor="wheat" colspan="5"><font size="-1">{@linkplain org.apache.sis.util.type.DefaultScopedName#path() path}</font></td><td></td>
 *     <td bgcolor="wheat" colspan="1"><font size="-1">{@linkplain org.apache.sis.util.type.AbstractName#tip() tip}</font></td>
 *   </tr>
 *
 *   <tr><td colspan="9" height="2"></td></tr>
 *   <tr align="center">
 *     <td bgcolor="palegoldenrod" colspan="1" rowspan="2"><font size="-1">{@linkplain org.apache.sis.util.type.AbstractName#scope() scope}</font></td><td rowspan="2"></td>
 *     <td bgcolor="palegoldenrod" colspan="1"><font size="-1">head</font></td><td></td>
 *     <td bgcolor="palegoldenrod" colspan="3"><font size="-1">tail</font></td>
 *     <td rowspan="2"></td>
 *     <td rowspan="2" bgcolor="beige" align="left">{@literal "org"}</td>
 *     <td rowspan="2" bgcolor="beige" align="right">{@literal {"opengis", "util", "Record"}}</td>
 *     <td rowspan="2"></td>
 *     <td rowspan="2">{@code ScopedName}</td>
 *   </tr>
 *   <tr align="center">
 *     <td bgcolor="wheat" colspan="3"><font size="-1">path</font></td><td></td>
 *     <td bgcolor="wheat" colspan="1"><font size="-1">tip</font></td>
 *   </tr>
 *
 *   <tr><td colspan="9" height="3"></td></tr>
 *   <tr align="center">
 *     <td bgcolor="palegoldenrod" colspan="3" rowspan="2"><font size="-1">scope</font></td><td rowspan="2"></td>
 *     <td bgcolor="palegoldenrod" colspan="1"><font size="-1">head</font></td><td></td>
 *     <td bgcolor="palegoldenrod" colspan="1"><font size="-1">tail</font></td>
 *     <td rowspan="2"></td>
 *     <td rowspan="2" bgcolor="beige" align="left">{@literal "org.opengis"}</td>
 *     <td rowspan="2" bgcolor="beige" align="right">{@literal {"util", "Record"}}</td>
 *     <td rowspan="2"></td>
 *     <td rowspan="2">{@code ScopedName}</td>
 *   </tr>
 *   <tr align="center">
 *     <td bgcolor="wheat" colspan="1"><font size="-1">path</font></td><td></td>
 *     <td bgcolor="wheat" colspan="1"><font size="-1">tip</font></td>
 *   </tr>
 *
 *   <tr><td colspan="9" height="3"></td></tr>
 *   <tr align="center">
 *     <td bgcolor="palegoldenrod" colspan="5" rowspan="2"><font size="-1">scope</font></td><td rowspan="2"></td>
 *     <td bgcolor="palegoldenrod" colspan="1"><font size="-1">head</font></td>
 *     <td rowspan="2"></td>
 *     <td rowspan="2" bgcolor="beige" align="left">{@literal "org.opengis.util"}</td>
 *     <td rowspan="2" bgcolor="beige" align="right">{@literal {"Record"}}</td>
 *     <td rowspan="2"></td>
 *     <td rowspan="2">{@link org.apache.sis.util.type.DefaultLocalName LocalName}</td>
 *   </tr>
 *   <tr align="center">
 *     <td bgcolor="wheat" colspan="1"><font size="-1">tip</font></td>
 *   </tr>
 * </table></td></tr></table></blockquote>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
package org.apache.sis.util.type;
