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
 * Information about the options for obtaining a resource (data or service).
 * An explanation for this package is provided in the {@linkplain org.opengis.metadata.distribution OpenGIS® javadoc}.
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
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.distribution.DefaultDistribution           Distribution}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.distribution.DefaultDistributor            Distributor}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.distribution.DefaultMedium                 Medium}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.distribution.DefaultFormat                 Format}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.distribution.DefaultStandardOrderProcess   Standard order process}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.distribution.DefaultDigitalTransferOptions Digital transfer options}<br>
 * {@code  └─} {@linkplain org.apache.sis.metadata.iso.distribution.DefaultDataFile               Data file}<br>
 * {@linkplain org.opengis.util.CodeList Code list}<br>
 * {@code  └─} {@linkplain org.opengis.metadata.distribution.MediumFormat Medium format}<br>
 * </td><td class="sep" style="width: 50%; white-space: nowrap">
 *                     {@linkplain org.apache.sis.metadata.iso.distribution.DefaultDistribution           Distribution}<br>
 * {@code  ├─}         {@linkplain org.apache.sis.metadata.iso.distribution.DefaultFormat                 Format}<br>
 * {@code  ├─}         {@linkplain org.apache.sis.metadata.iso.distribution.DefaultDistributor            Distributor}<br>
 * {@code  │   └─}     {@linkplain org.apache.sis.metadata.iso.distribution.DefaultStandardOrderProcess   Standard order process}<br>
 * {@code  └─}         {@linkplain org.apache.sis.metadata.iso.distribution.DefaultDigitalTransferOptions Digital transfer options}<br>
 * {@code      └─}     {@linkplain org.apache.sis.metadata.iso.distribution.DefaultMedium                 Medium}<br>
 * {@code          └─} {@linkplain org.opengis.metadata.distribution.MediumFormat                         Medium format} «code list»<br>
 *                     {@linkplain org.apache.sis.metadata.iso.distribution.DefaultDataFile               Data file}<br>
 * </td></tr></table>
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
@XmlSchema(location="https://schemas.isotc211.org/19115/-3/mrd/1.0/mrd.xsd",
           elementFormDefault=XmlNsForm.QUALIFIED, namespace=Namespaces.MRD,
           xmlns = {
                @XmlNs(prefix = "mrd", namespaceURI = Namespaces.MRD),      // Metadata for Resource Distribution
                @XmlNs(prefix = "mdt", namespaceURI = Namespaces.MDT),      // Metadata for Data Transfer
                @XmlNs(prefix = "mcc", namespaceURI = Namespaces.MCC),      // Metadata Common Classes
                @XmlNs(prefix = "gmd", namespaceURI = LegacyNamespaces.GMD),
                @XmlNs(prefix = "gmx", namespaceURI = LegacyNamespaces.GMX)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(CI_OnlineResource.class),
    @XmlJavaTypeAdapter(CI_Responsibility.class),
    @XmlJavaTypeAdapter(GO_Temporal.class),
    @XmlJavaTypeAdapter(GO_Integer.class),
    @XmlJavaTypeAdapter(GO_GenericName.class),
    @XmlJavaTypeAdapter(GO_Real.class),
    @XmlJavaTypeAdapter(MD_DigitalTransferOptions.class),
    @XmlJavaTypeAdapter(MD_Distributor.class),
    @XmlJavaTypeAdapter(MD_Format.class),
    @XmlJavaTypeAdapter(MD_Medium.class),
    @XmlJavaTypeAdapter(MD_MediumFormatCode.class),
    @XmlJavaTypeAdapter(MD_MediumNameCode.class),
    @XmlJavaTypeAdapter(MD_StandardOrderProcess.class),

    // Java types, primitive types and basic OGC types handling
    @XmlJavaTypeAdapter(URIAdapter.class),
    @XmlJavaTypeAdapter(UnitAdapter.class),
    @XmlJavaTypeAdapter(InternationalStringAdapter.class)
})
package org.apache.sis.metadata.iso.distribution;

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
