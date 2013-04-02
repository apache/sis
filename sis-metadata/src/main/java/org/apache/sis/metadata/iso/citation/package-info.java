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
 * {@linkplain org.apache.sis.metadata.iso.citation.DefaultCitation Citation} implementation.
 * An explanation for this package is provided in the {@linkplain org.opengis.metadata.citation OpenGIS® javadoc}.
 * The remaining discussion on this page is specific to the SIS implementation.
 *
 * {@section Overview}
 * For a global overview of metadata in SIS, see the
 * <a href="{@docRoot}/../sis-metadata/index.html">Metadata page on the project web site</a>.
 *
 * <table class="sis"><tr>
 *   <th>Class hierarchy</th>
 *   <th>Aggregation hierarchy</th>
 * </tr><tr><td>
 * <ul>
 *   <li>{@linkplain org.apache.sis.metadata.iso.ISOMetadata ISO-19115 metadata}<ul>
 *     <li>{@linkplain org.apache.sis.metadata.iso.citation.DefaultCitation Citation}</li>
 *     <li>{@linkplain org.apache.sis.metadata.iso.citation.DefaultCitationDate Citation date}</li>
 *     <li>{@linkplain org.apache.sis.metadata.iso.citation.DefaultResponsibleParty Responsible party}</li>
 *     <li>{@linkplain org.apache.sis.metadata.iso.citation.DefaultContact Contact}</li>
 *     <li>{@linkplain org.apache.sis.metadata.iso.citation.DefaultTelephone Telephone}</li>
 *     <li>{@linkplain org.apache.sis.metadata.iso.citation.DefaultAddress Address}</li>
 *     <li>{@linkplain org.apache.sis.metadata.iso.citation.DefaultOnlineResource Online resource}</li>
 *     <li>{@linkplain org.apache.sis.metadata.iso.citation.DefaultSeries Series}</li>
 *   </ul></li>
 *   <li>{@linkplain org.opengis.util.CodeList}<ul>
 *     <li>{@linkplain org.opengis.metadata.citation.DateType Date type}</li>
 *     <li>{@linkplain org.opengis.metadata.citation.OnLineFunction Online function}</li>
 *     <li>{@linkplain org.opengis.metadata.citation.PresentationForm Presentation form}</li>
 *     <li>{@linkplain org.opengis.metadata.citation.Role Role}</li>
 *   </ul></li>
 * </ul>
 * </td><td>
 * <ul>
 *   <li>{@linkplain org.apache.sis.metadata.iso.citation.DefaultCitation Citation}<ul>
 *     <li>{@linkplain org.apache.sis.metadata.iso.citation.DefaultCitationDate Citation date}<ul>
 *       <li>{@linkplain org.opengis.metadata.citation.DateType Date type} (a code list)</li>
 *     </ul></li>
 *     <li>{@linkplain org.apache.sis.metadata.iso.citation.DefaultResponsibleParty Responsible party}<ul>
 *       <li>{@linkplain org.apache.sis.metadata.iso.citation.DefaultContact Contact}<ul>
 *         <li>{@linkplain org.apache.sis.metadata.iso.citation.DefaultTelephone Telephone}</li>
 *         <li>{@linkplain org.apache.sis.metadata.iso.citation.DefaultAddress Address}</li>
 *         <li>{@linkplain org.apache.sis.metadata.iso.citation.DefaultOnlineResource Online resource}<ul>
 *           <li>{@linkplain org.opengis.metadata.citation.OnLineFunction Online function} (a code list)</li>
 *         </ul></li>
 *       </ul></li>
 *       <li>{@linkplain org.opengis.metadata.citation.Role Role} (a code list)</li>
 *     </ul></li>
 *     <li>{@linkplain org.opengis.metadata.citation.PresentationForm Presentation form} (a code list)</li>
 *     <li>{@linkplain org.apache.sis.metadata.iso.citation.DefaultSeries Series}</li>
 *   </ul></li>
 * </ul>
 * </td></tr></table>
 *
 * {@section Unified identifiers view}
 * Apache SIS provides a unified view of all metadata identifiers. This view includes the citation
 * {@linkplain org.apache.sis.metadata.iso.citation.DefaultCitation#getISBN() ISBN} and
 * {@linkplain org.apache.sis.metadata.iso.citation.DefaultCitation#getISSN() ISSN} codes,
 * except at XML marshalling time (for ISO 19139 compliance).
 * See {@link org.apache.sis.xml.IdentifierMap} for more information.
 *
 * {@section Collections and null values}
 * Unless otherwise noted in the Javadoc, all constructors and setter methods accept {@code null} argument.
 * A null argument value means that the metadata element can not be provided, and the reason for that is unspecified.
 * Alternatively, users can specify why a metadata element is missing by providing a value created by
 * {@link org.apache.sis.xml.NilReason#createNilObject NilReason.createNilObject(Class)}.
 *
 * <p>Unless otherwise noted in the Javadoc, all getter methods may return an empty collection,
 * an empty array or {@code null} if there is no value. More specifically:</p>
 * <ul>
 *   <li>If the return type is a collection, the method may return an empty collection (never {@code null}).</li>
 *   <li>If the return type is an array, the method may return an empty array (never {@code null}).</li>
 *   <li>Otherwise the method may return {@code null}.</li>
 * </ul>
 *
 * Unless the metadata object has been marked as unmodifiable and unless otherwise noted in the Javadoc,
 * all collections returned by getter methods are <cite>live</cite>: adding new elements in the collection
 * modify directly the underlying metadata object.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.GMD, xmlns = {
    @XmlNs(prefix = "gmd", namespaceURI = Namespaces.GMD),
    @XmlNs(prefix = "gco", namespaceURI = Namespaces.GCO),
    @XmlNs(prefix = "xsi", namespaceURI = Namespaces.XSI)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(CI_Address.class),
    @XmlJavaTypeAdapter(CI_Contact.class),
    @XmlJavaTypeAdapter(CI_Date.class),
    @XmlJavaTypeAdapter(CI_DateTypeCode.class),
    @XmlJavaTypeAdapter(CI_OnLineFunctionCode.class),
    @XmlJavaTypeAdapter(CI_OnlineResource.class),
    @XmlJavaTypeAdapter(CI_PresentationFormCode.class),
    @XmlJavaTypeAdapter(CI_ResponsibleParty.class),
    @XmlJavaTypeAdapter(CI_RoleCode.class),
    @XmlJavaTypeAdapter(CI_Series.class),
    @XmlJavaTypeAdapter(CI_Telephone.class),
    @XmlJavaTypeAdapter(MD_Identifier.class),

    // Java types, primitive types and basic OGC types handling
//    @XmlJavaTypeAdapter(GO_URL.class), // TODO
//    @XmlJavaTypeAdapter(GO_DateTime.class), // TODO
    @XmlJavaTypeAdapter(StringAdapter.class),
    @XmlJavaTypeAdapter(InternationalStringAdapter.class)
})
package org.apache.sis.metadata.iso.citation;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.internal.jaxb.gco.*;
import org.apache.sis.internal.jaxb.gmd.*;
import org.apache.sis.internal.jaxb.code.*;
import org.apache.sis.internal.jaxb.metadata.*;
