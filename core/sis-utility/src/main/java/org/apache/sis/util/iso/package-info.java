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
 * The main content of this package are:
 *
 * <ul>
 *   <li>Implementations of {@link org.opengis.util.InternationalString}
 *       (related to the {@code <gmd:textGroup>} XML element found in ISO specifications):
 *     <ul>
 *       <li>{@link org.apache.sis.util.iso.SimpleInternationalString}   for wrapping a single {@link java.lang.String};</li>
 *       <li>{@link org.apache.sis.util.iso.DefaultInternationalString}  for providing many localizations in a {@link java.util.Map};</li>
 *       <li>{@link org.apache.sis.util.iso.ResourceInternationalString} for providing localizations from a {@link java.util.ResourceBundle}.</li>
 *       <li>{@link org.apache.sis.util.iso.Types#getCodeTitle Types.getCodeTitle(CodeList)} for wrapping a {@link org.opengis.util.CodeList} value.</li>
 *     </ul>
 *   </li>
 *   <li>Implementations of {@link org.opengis.util.GenericName} (derived from ISO 19103):
 *     <ul>
 *       <li>{@link org.apache.sis.util.iso.DefaultLocalName}  for identifier within a {@linkplain org.apache.sis.util.iso.DefaultNameSpace name space}.</li>
 *       <li>{@link org.apache.sis.util.iso.DefaultMemberName} for identifying a member of a {@linkplain org.apache.sis.util.iso.DefaultRecord record}.</li>
 *       <li>{@link org.apache.sis.util.iso.DefaultTypeName}   for identifying an attribute type associated to a member.</li>
 *       <li>{@link org.apache.sis.util.iso.DefaultScopedName} for a composite of a <cite>head</cite> name and a <cite>tail</cite> name.</li>
 *     </ul>
 *   </li>
 *   <li>Implementations of {@link org.opengis.util.Record} and related classes (derived from ISO 19103):
 *     <ul>
 *       <li>{@link org.apache.sis.util.iso.DefaultRecord}       for a list of logically related elements as (<var>name</var>, <var>value</var>) pairs.</li>
 *       <li>{@link org.apache.sis.util.iso.DefaultRecordType}   for definition of the type of a {@code Record}.</li>
 *       <li>{@link org.apache.sis.util.iso.DefaultRecordSchema} for a collection of {@code RecordType}s in a given namespace.</li>
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
 * <div class="section">Anatomy of a name</div>
 * Names may be {@linkplain org.apache.sis.util.iso.AbstractName#toFullyQualifiedName() fully qualified}
 * (like {@code "urn:ogc:def:crs:EPSG::4326"}),
 * or they may be relative to a {@linkplain org.apache.sis.util.iso.AbstractName#scope() scope}
 * (like {@code "EPSG::4326"} in the {@code "urn:ogc:def:crs"} scope).
 * In the following illustration, each line is one possible construction for {@code "urn:ogc:crs:epsg:4326"}
 * (taken as an abridged form of above URN for this example only).
 * For each construction:
 * <ul>
 *   <li>The first columns shows the <span style="background:LawnGreen">name</span> in a green background.</li>
 *   <li>The second and third columns show the
 *       (<span style="background:LightSkyBlue"><var>head</var></span>.<span style="background:Yellow"><var>tail</var></span>) and
 *       (<span style="background:LightSkyBlue"><var>path</var></span>.<span style="background:Yellow"><var>tip</var></span>)
 *       components, respectively.</li>
 *   <li>The parts without colored background do not appear in the
 *       {@linkplain org.apache.sis.util.iso.AbstractName#toString() string representation} or in the
 *       {@linkplain org.apache.sis.util.iso.AbstractName#getParsedNames() list of parsed names}.</li>
 * </ul>
 *
 * <table class="compact" style="border-spacing:21pt 0; white-space: nowrap" summary="Anatomy of a name">
 *   <tr>
 *     <th>{@linkplain org.apache.sis.util.iso.AbstractName#scope() scope}.name</th>
 *     <th>{@linkplain org.apache.sis.util.iso.AbstractName#head() head}.{@linkplain org.apache.sis.util.iso.DefaultScopedName#tail() tail}</th>
 *     <th>{@linkplain org.apache.sis.util.iso.DefaultScopedName#path() path}.{@linkplain org.apache.sis.util.iso.AbstractName#tip() tip}</th>
 *     <th>Type</th>
 *   </tr><tr>
 *     <td><code><span style="background:LawnGreen">urn:ogc:crs:epsg:4326</span></code></td>
 *     <td><code><span style="background:LightSkyBlue">urn:</span><span style="background:Yellow">ogc:crs:epsg:4326</span></code></td>
 *     <td><code><span style="background:LightSkyBlue">urn:ogc:crs:epsg:</span><span style="background:Yellow">4326</span></code></td>
 *     <td>{@linkplain org.apache.sis.util.iso.DefaultScopedName Scoped name} with
 *         {@linkplain org.apache.sis.util.iso.DefaultNameSpace#isGlobal() global namespace}</td>
 *   </tr><tr>
 *     <td><code>urn:<span style="background:LawnGreen">ogc:crs:epsg:4326</span></code></td>
 *     <td><code>urn:<span style="background:LightSkyBlue">ogc:</span><span style="background:Yellow">crs:epsg:4326</span></code></td>
 *     <td><code>urn:<span style="background:LightSkyBlue">ogc:crs:epsg:</span><span style="background:Yellow">4326</span></code></td>
 *     <td>{@linkplain org.apache.sis.util.iso.DefaultScopedName Scoped name}</td>
 *   </tr><tr>
 *     <td><code>urn:ogc:<span style="background:LawnGreen">crs:epsg:4326</span></code></td>
 *     <td><code>urn:ogc:<span style="background:LightSkyBlue">crs:</span><span style="background:Yellow">epsg:4326</span></code></td>
 *     <td><code>urn:ogc:<span style="background:LightSkyBlue">crs:epsg:</span><span style="background:Yellow">4326</span></code></td>
 *     <td>{@linkplain org.apache.sis.util.iso.DefaultScopedName Scoped name}</td>
 *   </tr><tr>
 *     <td><code>urn:ogc:crs:<span style="background:LawnGreen">epsg:4326</span></code></td>
 *     <td><code>urn:ogc:crs:<span style="background:LightSkyBlue">epsg:</span><span style="background:Yellow">4326</span></code></td>
 *     <td><code>urn:ogc:crs:<span style="background:LightSkyBlue">epsg:</span><span style="background:Yellow">4326</span></code></td>
 *     <td>{@linkplain org.apache.sis.util.iso.DefaultScopedName Scoped name}</td>
 *   </tr><tr>
 *     <td><code>urn:ogc:crs:epsg:<span style="background:LawnGreen">4326</span></code></td>
 *     <td><code>urn:ogc:crs:epsg:<span style="background:LightSkyBlue">4326</span></code></td>
 *     <td><code>urn:ogc:crs:epsg:<span style="background:Yellow">4326</span></code></td>
 *     <td>{@linkplain org.apache.sis.util.iso.DefaultLocalName Local name}</td>
 *   </tr>
 * </table>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.7
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
