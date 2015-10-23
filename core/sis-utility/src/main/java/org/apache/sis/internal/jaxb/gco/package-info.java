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
 * Miscellaneous objects and adapters defined in the {@code "gco"} namespace.
 * For example, a {@link java.lang.String} value has to be marshalled this way:
 *
 * {@preformat text
 *     <gco:CharacterString>my text</gco:CharacterString>
 * }
 *
 * In the above example, {@code gco} is the prefix for the {@code http://www.isotc211.org/2005/gco}
 * namespace URL.
 *
 * <p>This package includes:</p>
 *
 * <ul class="verbose">
 *   <li><b>JAXB adapters for primitive types.</b><br>
 *   JAXB can write directly Java primitive type at marshalling time "as is". However ISO-19139
 *   requires those values to be surrounded by elements representing the data type. The role of
 *   these adapters is to add these elements around the value.</li>
 *
 *   <li><b>JAXB adapters for <cite>unit of measure</cite></b> as specified in the ISO-19103 specifications.<br>
 *   For example, a measure marshalled with JAXB will be formatted like {@code <gco:Measure uom="m">220.0</gco:Measure>}.</li>
 *
 *   <li>JAXB adapters for date and time.</li>
 * </ul>
 *
 * Classes prefixed by two letters, like {@code "GO_Decimal"}, are also wrappers around the actual
 * object to be marshalled. See the {@link org.apache.sis.internal.jaxb.metadata} package for more
 * explanation about wrappers. Note that the two-letters prefixes used in this package (not to be
 * confused with the three-letters prefixes used in XML documents) are not defined by OGC/ISO
 * specifications; they are used only for consistency with current practice in
 * {@link org.apache.sis.internal.jaxb.metadata} and similar packages.
 *
 * <div class="section">Object identification and reference</div>
 * <ul class="verbose">
 *   <li><code>org.apache.sis.<b>metadata.iso</b></code> public packages:
 *   <ul>
 *     <li>Implement the ISO 19139 {@code Foo_Type}, where <var>Foo</var> is the ISO name of a class.</li>
 *     <li>Contains the {@code gco:ObjectIdentification} group of attributes ({@code id}, {@code uuid}).</li>
 *     <li>Conceptually could have been subclasses of {@code ObjectIdentification} defined in this package.</li>
 *   </ul></li>
 *   <li><code>org.apache.sis.<b>internal.jaxb</b></code> private packages:
 *   <ul>
 *     <li>Implement the ISO 19139 {@code Foo_PropertyType} as subclasses of the {@link org.apache.sis.internal.jaxb.gco.PropertyType} class.</li>
 *     <li>Contains the {@code gco:ObjectReference} group of attributes ({@code xlink}, {@code uuidref}).</li>
 *     <li>Attributes are declared in the {@link org.apache.sis.internal.jaxb.gco.ObjectReference} Java class.</li>
 *    </ul></li>
 * </ul>
 *
 * <p>Those two kinds of types are marshalled as below:</p>
 *
 * {@preformat xml
 *   <MD_MetaData>
 *     <property uuidref="…">
 *       <Foo_Type uuid="…">
 *         ...
 *       </Foo_Type>
 *     </property>
 *   </MD_MetaData>
 * }
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 *
 * @see javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.GCO, xmlns = {
    @XmlNs(prefix = "gco", namespaceURI = Namespaces.GCO),
    @XmlNs(prefix = "gmx", namespaceURI = Namespaces.GMX)
})
@XmlAccessorType(XmlAccessType.NONE)
/*
 * Do NOT define a package-level adapter for InternationalString,
 * because such adapter shall NOT apply to GO_CharacterString.getAnchor().
 */
package org.apache.sis.internal.jaxb.gco;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import org.apache.sis.xml.Namespaces;
