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
 * Reference to the data or service (citation, responsible party, contact information).
 * An explanation for this package is provided in the {@linkplain org.opengis.metadata.citation OpenGIS® javadoc}.
 * The remaining discussion on this page is specific to the SIS implementation.
 *
 * <h2>Overview</h2>
 * For a global overview of metadata in SIS, see the {@link org.apache.sis.metadata} package javadoc.
 *
 * <table class="sis">
 * <caption>Package overview</caption>
 * <tr>
 *   <th>Class hierarchy</th>
 *   <th class="sep">Aggregation hierarchy</th>
 * </tr><tr><td style="width: 50%; white-space: nowrap">
 * {@linkplain org.apache.sis.metadata.iso.ISOMetadata ISO 19115 metadata}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.citation.DefaultCitation         Citation}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.citation.DefaultCitationDate     Citation date}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.citation.DefaultResponsibility   Responsibility}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.citation.AbstractParty           Party}<br>
 * {@code  │   ├─} {@linkplain org.apache.sis.metadata.iso.citation.DefaultIndividual   Individual}<br>
 * {@code  │   └─} {@linkplain org.apache.sis.metadata.iso.citation.DefaultOrganisation Organisation}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.citation.DefaultContact          Contact}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.citation.DefaultTelephone        Telephone}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.citation.DefaultAddress          Address}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.citation.DefaultOnlineResource   Online resource}<br>
 * {@code  └─} {@linkplain org.apache.sis.metadata.iso.citation.DefaultSeries           Series}<br>
 * {@linkplain org.opengis.util.CodeList Code list}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.citation.DateType         Date type}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.citation.OnLineFunction   Online function}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.citation.PresentationForm Presentation form}<br>
 * {@code  └─} {@linkplain org.opengis.metadata.citation.Role             Role}<br>
 * </td><td class="sep" style="width: 50%; white-space: nowrap">
 *                         {@linkplain org.apache.sis.metadata.iso.citation.DefaultCitation         Citation}<br>
 * {@code  ├─}             {@linkplain org.apache.sis.metadata.iso.citation.DefaultCitationDate     Citation date}<br>
 * {@code  │   └─}         {@linkplain org.opengis.metadata.citation.DateType                       Date type} «code list»<br>
 * {@code  ├─}             {@linkplain org.apache.sis.metadata.iso.citation.DefaultResponsibility   Responsibility}<br>
 * {@code  │   ├─}         {@linkplain org.apache.sis.metadata.iso.citation.AbstractParty           Party}<br>
 * {@code  │   │   └─}     {@linkplain org.apache.sis.metadata.iso.citation.DefaultContact          Contact}<br>
 * {@code  │   │       ├─} {@linkplain org.apache.sis.metadata.iso.citation.DefaultTelephone        Telephone}<br>
 * {@code  │   │       ├─} {@linkplain org.apache.sis.metadata.iso.citation.DefaultAddress          Address}<br>
 * {@code  │   │       └─} {@linkplain org.apache.sis.metadata.iso.citation.DefaultOnlineResource   Online resource}<br>
 * {@code  │   │           └─} {@linkplain org.opengis.metadata.citation.OnLineFunction             Online function} «code list»<br>
 * {@code  │   └─}         {@linkplain org.opengis.metadata.citation.Role                           Role} «code list»<br>
 * {@code  ├─}             {@linkplain org.opengis.metadata.citation.PresentationForm               Presentation form} «code list»<br>
 * {@code  └─}             {@linkplain org.apache.sis.metadata.iso.citation.DefaultSeries           Series}<br>
 * </td></tr></table>
 *
 * <h2>Unified identifiers view</h2>
 * Apache SIS provides a unified view of all metadata identifiers. This view includes the citation
 * {@linkplain org.apache.sis.metadata.iso.citation.DefaultCitation#getISBN() ISBN} and
 * {@linkplain org.apache.sis.metadata.iso.citation.DefaultCitation#getISSN() ISSN} codes,
 * except at XML marshalling time (for ISO 19115-3 compliance).
 * See {@link org.apache.sis.xml.IdentifierMap} for more information.
 *
 * <h2>Null values, nil objects and collections</h2>
 * All constructors and setter methods accept {@code null} arguments.
 * A null argument value means that the metadata element cannot be provided, and the reason for that is unspecified.
 * Alternatively, users can specify why a metadata element is missing by providing a value created by
 * {@link org.apache.sis.xml.NilReason#createNilObject NilReason.createNilObject(Class)}.
 *
 * <p>Unless otherwise noted in the Javadoc, all getter methods may return an empty collection,
 * an empty array or {@code null} if the type is neither a collection or an array.
 * Note that non-null values may be {@link org.apache.sis.xml.NilObject}s.</p>
 *
 * <p>Unless the metadata object has been marked as unmodifiable and unless otherwise noted in the Javadoc,
 * all collections returned by getter methods are <em>live</em>: adding new elements in the collection
 * modify directly the underlying metadata object.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.5
 * @since   0.3
 */
@XmlSchema(location="https://schemas.isotc211.org/19115/-3/cit/1.0/cit.xsd",
           elementFormDefault=XmlNsForm.QUALIFIED, namespace=Namespaces.CIT,
           xmlns = {
                @XmlNs(prefix = "cit", namespaceURI = Namespaces.CIT),      // Citation and responsible party information
                @XmlNs(prefix = "mcc", namespaceURI = Namespaces.MCC),      // Metadata Common Classes
                @XmlNs(prefix = "gmd", namespaceURI = LegacyNamespaces.GMD)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(CI_Address.class),
    @XmlJavaTypeAdapter(CI_Contact.class),
    @XmlJavaTypeAdapter(CI_Date.class),
    @XmlJavaTypeAdapter(CI_DateTypeCode.class),
    @XmlJavaTypeAdapter(CI_OnLineFunctionCode.class),
    @XmlJavaTypeAdapter(CI_OnlineResource.class),
    @XmlJavaTypeAdapter(CI_Party.class),
    @XmlJavaTypeAdapter(CI_PresentationFormCode.class),
    @XmlJavaTypeAdapter(CI_ResponsibleParty.class),
    @XmlJavaTypeAdapter(CI_RoleCode.class),
    @XmlJavaTypeAdapter(CI_Series.class),
    @XmlJavaTypeAdapter(CI_Telephone.class),
    @XmlJavaTypeAdapter(EX_Extent.class),
    @XmlJavaTypeAdapter(GO_DateTime.class),
    @XmlJavaTypeAdapter(GO_Temporal.class),
    @XmlJavaTypeAdapter(MD_BrowseGraphic.class),
    @XmlJavaTypeAdapter(MD_Identifier.class),

    // Java types, primitive types and basic OGC types handling
    @XmlJavaTypeAdapter(StringAdapter.class),
    @XmlJavaTypeAdapter(InternationalStringAdapter.class)
})
package org.apache.sis.metadata.iso.citation;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.xml.bind.gco.*;
import org.apache.sis.xml.bind.metadata.*;
import org.apache.sis.xml.bind.metadata.code.*;
