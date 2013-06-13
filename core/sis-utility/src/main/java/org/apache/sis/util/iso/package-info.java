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
 * {@code InternationalString} implementations are closely related to the {@code <gmd:textGroup>}
 * XML element found in ISO specifications. All other non-static types except the {@code Factory}
 * implementations are derived from the ISO 19103 specification.
 * The main content of this package are:
 *
 * <ul>
 *   <li>Implementations of {@link org.opengis.util.InternationalString}:
 *     <ul>
 *       <li>{@link org.apache.sis.util.iso.SimpleInternationalString} for wrapping a single {@link java.lang.String};</li>
 *       <li>{@link org.apache.sis.util.iso.DefaultInternationalString} for providing many localizations in a {@link java.util.Map};</li>
 *       <li>{@link org.apache.sis.util.iso.ResourceInternationalString} for providing localizations from a {@link java.util.ResourceBundle}.</li>
 *     </ul>
 *   </li>
 *   <li>Implementations of {@link org.opengis.util.GenericName}:
 *     <ul>
 *       <li>{@link org.apache.sis.util.iso.DefaultLocalName} for identifier within a namespace.</li>
 *       <li>{@link org.apache.sis.util.iso.DefaultScopedName} for a composite of a <cite>head</cite> name and a <cite>tail</cite> name.</li>
 *       <li>{@link org.apache.sis.util.iso.DefaultMemberName} for identifying a member of a record.</li>
 *       <li>{@link org.apache.sis.util.iso.DefaultTypeName} for identifying an attribute type associated to a member.</li>
 *       <li>{@link org.apache.sis.util.iso.DefaultNameSpace} for identifying the domain in which above names are defined.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * {@section Relationship between naming types}
 * Names may be {@linkplain org.apache.sis.util.iso.AbstractName#toFullyQualifiedName()
 * fully qualified} (like {@code "org.opengis.util.Record"}), or they may be relative to a
 * {@linkplain org.apache.sis.util.iso.AbstractName#scope() scope} (like {@code "util.Record"}
 * in the {@code "org.opengis"} scope). The illustration below shows all possible constructions
 * for {@code "org.opengis.util.Record"}:
 *
 * <blockquote><table class="compact"><tr><td><table class="compact">
 *   <tr>
 *     <th align="right">org</th>
 *     <th>.</th><th>opengis</th>
 *     <th>.</th><th>util</th>
 *     <th>.</th><th>Record</th>
 *     <th width="50"></th>
 *     <th>{@link org.apache.sis.util.iso.AbstractName#scope() scope()}</th>
 *     <th>{@link org.apache.sis.util.iso.AbstractName#getParsedNames() getParsedNames()}</th>
 *     <th width="50"></th>
 *     <th>Type</th>
 *   </tr>
 *
 *   <tr align="center">
 *     <td style="background:palegoldenrod" colspan="1"><font size="-1">{@linkplain org.apache.sis.util.iso.AbstractName#head() head}</font></td><td></td>
 *     <td style="background:palegoldenrod" colspan="5"><font size="-1">{@linkplain org.apache.sis.util.iso.DefaultScopedName#tail() tail}</font></td>
 *     <td rowspan="2"></td>
 *     <td rowspan="2" style="background:beige" align="left">{@linkplain org.apache.sis.util.iso.DefaultNameSpace#isGlobal() global}</td>
 *     <td rowspan="2" style="background:beige" align="right">{@code {"org", "opengis", "util", "Record"}}</td>
 *     <td rowspan="2"></td>
 *     <td rowspan="2">{@link org.apache.sis.util.iso.DefaultScopedName ScopedName}</td>
 *   </tr>
 *   <tr align="center">
 *     <td style="background:wheat" colspan="5"><font size="-1">{@linkplain org.apache.sis.util.iso.DefaultScopedName#path() path}</font></td><td></td>
 *     <td style="background:wheat" colspan="1"><font size="-1">{@linkplain org.apache.sis.util.iso.AbstractName#tip() tip}</font></td>
 *   </tr>
 *
 *   <tr><td colspan="9" height="2"></td></tr>
 *   <tr align="center">
 *     <td style="background:palegoldenrod" colspan="1" rowspan="2"><font size="-1">{@linkplain org.apache.sis.util.iso.AbstractName#scope() scope}</font></td><td rowspan="2"></td>
 *     <td style="background:palegoldenrod" colspan="1"><font size="-1">head</font></td><td></td>
 *     <td style="background:palegoldenrod" colspan="3"><font size="-1">tail</font></td>
 *     <td rowspan="2"></td>
 *     <td rowspan="2" style="background:beige" align="left">{@code "org"}</td>
 *     <td rowspan="2" style="background:beige" align="right">{@code {"opengis", "util", "Record"}}</td>
 *     <td rowspan="2"></td>
 *     <td rowspan="2">{@code ScopedName}</td>
 *   </tr>
 *   <tr align="center">
 *     <td style="background:wheat" colspan="3"><font size="-1">path</font></td><td></td>
 *     <td style="background:wheat" colspan="1"><font size="-1">tip</font></td>
 *   </tr>
 *
 *   <tr><td colspan="9" height="3"></td></tr>
 *   <tr align="center">
 *     <td style="background:palegoldenrod" colspan="3" rowspan="2"><font size="-1">scope</font></td><td rowspan="2"></td>
 *     <td style="background:palegoldenrod" colspan="1"><font size="-1">head</font></td><td></td>
 *     <td style="background:palegoldenrod" colspan="1"><font size="-1">tail</font></td>
 *     <td rowspan="2"></td>
 *     <td rowspan="2" style="background:beige" align="left">{@code "org.opengis"}</td>
 *     <td rowspan="2" style="background:beige" align="right">{@code {"util", "Record"}}</td>
 *     <td rowspan="2"></td>
 *     <td rowspan="2">{@code ScopedName}</td>
 *   </tr>
 *   <tr align="center">
 *     <td style="background:wheat" colspan="1"><font size="-1">path</font></td><td></td>
 *     <td style="background:wheat" colspan="1"><font size="-1">tip</font></td>
 *   </tr>
 *
 *   <tr><td colspan="9" height="3"></td></tr>
 *   <tr align="center">
 *     <td style="background:palegoldenrod" colspan="5" rowspan="2"><font size="-1">scope</font></td><td rowspan="2"></td>
 *     <td style="background:palegoldenrod" colspan="1"><font size="-1">head</font></td>
 *     <td rowspan="2"></td>
 *     <td rowspan="2" style="background:beige" align="left">{@code "org.opengis.util"}</td>
 *     <td rowspan="2" style="background:beige" align="right">{@code {"Record"}}</td>
 *     <td rowspan="2"></td>
 *     <td rowspan="2">{@link org.apache.sis.util.iso.DefaultLocalName LocalName}</td>
 *   </tr>
 *   <tr align="center">
 *     <td style="background:wheat" colspan="1"><font size="-1">tip</font></td>
 *   </tr>
 * </table></td></tr></table></blockquote>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.GCO, xmlns = {
    @XmlNs(prefix = "gco", namespaceURI = Namespaces.GCO)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(GO_GenericName.class),
    @XmlJavaTypeAdapter(LocalNameAdapter.class),
    @XmlJavaTypeAdapter(ScopedNameAdapter.class)
})
package org.apache.sis.util.iso;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.internal.jaxb.gco.*;
