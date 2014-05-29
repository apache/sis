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
 * Implementations of GeoAPI types from the {@link org.opengis.util} package.
 * {@code InternationalString} implementations are closely related to the {@code <gmd:textGroup>}
 * XML element found in ISO specifications. All other non-static types except the {@code Factory}
 * implementations are derived from the ISO 19103 specification.
 * The main content of this package are:
 *
 * <ul>
 *   <li>Implementations of {@link org.opengis.util.InternationalString}:
 *     <ul>
 *       <li>{@link org.apache.sis.util.iso.SimpleInternationalString}   for wrapping a single {@link java.lang.String};</li>
 *       <li>{@link org.apache.sis.util.iso.DefaultInternationalString}  for providing many localizations in a {@link java.util.Map};</li>
 *       <li>{@link org.apache.sis.util.iso.ResourceInternationalString} for providing localizations from a {@link java.util.ResourceBundle}.</li>
 *     </ul>
 *   </li>
 *   <li>Implementations of {@link org.opengis.util.GenericName}:
 *     <ul>
 *       <li>{@link org.apache.sis.util.iso.DefaultLocalName}  for identifier within a {@linkplain org.apache.sis.util.iso.DefaultNameSpace name space}.</li>
 *       <li>{@link org.apache.sis.util.iso.DefaultMemberName} for identifying a member of a {@linkplain org.apache.sis.util.iso.DefaultRecord record}.</li>
 *       <li>{@link org.apache.sis.util.iso.DefaultTypeName}   for identifying an attribute type associated to a member.</li>
 *       <li>{@link org.apache.sis.util.iso.DefaultScopedName} for a composite of a <cite>head</cite> name and a <cite>tail</cite> name.</li>
 *     </ul>
 *   </li>
 *   <li>Static utility methods:
 *     <ul>
 *       <li>{@link org.apache.sis.util.iso.Types} for working with UML identifiers and description of GeoAPI types.</li>
 *       <li>{@link org.apache.sis.util.iso.Names} for simple creation and operations on {@code GenericName}.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * {@section Anatomy of a name}
 * Names may be {@linkplain org.apache.sis.util.iso.AbstractName#toFullyQualifiedName()
 * fully qualified} (like {@code "org.opengis.util.Record"}), or they may be relative to a
 * {@linkplain org.apache.sis.util.iso.AbstractName#scope() scope} (like {@code "util.Record"}
 * in the {@code "org.opengis"} scope). In the following illustration,
 * each line is one possible construction for {@code "org.apache.sis.util.iso"}.
 * For each construction, the first columns shows the name in a yellow background. The second and third columns show the
 * (<span style="background:LightSkyBlue"><var>head</var></span>.<span style="background:Yellow"><var>tail</var></span>) and
 * (<span style="background:LightSkyBlue"><var>path</var></span>.<span style="background:Yellow"><var>tip</var></span>)
 * pairs of attributes, respectively:
 *
 * <table class="compact" style="border-spacing:21pt 0; white-space: nowrap" summary="Anatomy of a name">
 *   <tr>
 *     <th>{@linkplain org.apache.sis.util.iso.AbstractName#scope() scope}.name</th>
 *     <th>{@linkplain org.apache.sis.util.iso.AbstractName#head() head}.{@linkplain org.apache.sis.util.iso.DefaultScopedName#tail() tail}</th>
 *     <th>{@linkplain org.apache.sis.util.iso.DefaultScopedName#path() path}.{@linkplain org.apache.sis.util.iso.AbstractName#tip() tip}</th>
 *     <th>Type</th>
 *   </tr><tr>
 *     <td><code><span style="background:Yellow">org.apache.sis.util.iso</span></code></td>
 *     <td><code><span style="background:LightSkyBlue">org.</span><span style="background:Yellow">apache.sis.util.iso</span></code></td>
 *     <td><code><span style="background:LightSkyBlue">org.apache.sis.util.</span><span style="background:Yellow">iso</span></code></td>
 *     <td>{@linkplain org.apache.sis.util.iso.DefaultScopedName Scoped name} with
 *         {@linkplain org.apache.sis.util.iso.DefaultNameSpace#isGlobal() global namespace}</td>
 *   </tr><tr>
 *     <td><code><span style="background:LightSkyBlue">org</span>.<span style="background:Yellow">apache.sis.util.iso</span></code></td>
 *     <td><code>org.<span style="background:LightSkyBlue">apache.</span><span style="background:Yellow">sis.util.iso</span></code></td>
 *     <td><code>org.<span style="background:LightSkyBlue">apache.sis.util.</span><span style="background:Yellow">iso</span></code></td>
 *     <td>{@linkplain org.apache.sis.util.iso.DefaultScopedName Scoped name}</td>
 *   </tr><tr>
 *     <td><code><span style="background:LightSkyBlue">org.apache</span>.<span style="background:Yellow">sis.util.iso</span></code></td>
 *     <td><code>org.apache.<span style="background:LightSkyBlue">sis.</span><span style="background:Yellow">util.iso</span></code></td>
 *     <td><code>org.apache.<span style="background:LightSkyBlue">sis.util.</span><span style="background:Yellow">iso</span></code></td>
 *     <td>{@linkplain org.apache.sis.util.iso.DefaultScopedName Scoped name}</td>
 *   </tr><tr>
 *     <td><code><span style="background:LightSkyBlue">org.apache.sis</span>.<span style="background:Yellow">util.iso</span></code></td>
 *     <td><code>org.apache.sis.<span style="background:LightSkyBlue">util.</span><span style="background:Yellow">iso</span></code></td>
 *     <td><code>org.apache.sis.<span style="background:LightSkyBlue">util.</span><span style="background:Yellow">iso</span></code></td>
 *     <td>{@linkplain org.apache.sis.util.iso.DefaultScopedName Scoped name}</td>
 *   </tr><tr>
 *     <td><code><span style="background:LightSkyBlue">org.apache.sis.util</span>.<span style="background:Yellow">iso</span></code></td>
 *     <td><code>org.apache.sis.util.<span style="background:LightSkyBlue">iso</span></code></td>
 *     <td><code>org.apache.sis.util.<span style="background:Yellow">iso</span></code></td>
 *     <td>{@linkplain org.apache.sis.util.iso.DefaultLocalName Local name}</td>
 *   </tr>
 * </table>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.5
 * @module
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.GCO, xmlns = {
    @XmlNs(prefix = "gco", namespaceURI = Namespaces.GCO)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(GO_GenericName.class)
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
