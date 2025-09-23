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
 * Classes that are normally omitted from public API because they duplicate existing classes,
 * but still temporarily used at XML (un)marshalling time for standards compliance.
 *
 * <p>Some objects defined in various standards have overlapping functionalities.
 * For example, the Metadata (ISO 19115), Referencing by Coordinates (ISO 19111),
 * Data Quality (ISO 19157) and Web Processing Service (WPS) standards all define their own parameter objects.
 * Another example is ISO 19115 defining basic referencing information, which is clearly ISO 19111 work.
 * GeoAPI tries to provide an uniform API by merging objects, or by omitting an object from one standard
 * in favor of the equivalent object of another standard. However, at XML (un)marshalling time,
 * we still need to temporarily recreate the omitted object as defined in the original standard.
 * This package is used for such replacement.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, xmlns = {
    @XmlNs(prefix = "mcc",  namespaceURI = Namespaces.MCC),        // Metadata Common Classes
    @XmlNs(prefix = "mrs",  namespaceURI = Namespaces.MRS),        // Metadata for Reference System
    @XmlNs(prefix = "srv",  namespaceURI = Namespaces.SRV),        // Metadata for Services 2.0
    @XmlNs(prefix = "srv1", namespaceURI = LegacyNamespaces.SRV),  // Metadata for Services 1.0
    @XmlNs(prefix = "gmd",  namespaceURI = LegacyNamespaces.GMD)   // Metadata ISO 19139:2007
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(GO_Boolean.class),
    @XmlJavaTypeAdapter(MD_Identifier.class),
    @XmlJavaTypeAdapter(DQM_Description.class),
    @XmlJavaTypeAdapter(DQM_ValueStructure.class),
    @XmlJavaTypeAdapter(SV_ParameterDirection.class),
    @XmlJavaTypeAdapter(MD_ReferenceSystemTypeCode.class),

    // Java types, primitive types and basic OGC types handling
    @XmlJavaTypeAdapter(StringAdapter.class),
    @XmlJavaTypeAdapter(InternationalStringAdapter.class),
    @XmlJavaTypeAdapter(value=GO_Boolean.class, type=boolean.class)
})
package org.apache.sis.xml.bind.metadata.replace;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.xml.bind.metadata.MD_Identifier;
import org.apache.sis.xml.bind.metadata.DQM_Description;
import org.apache.sis.xml.bind.gco.*;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.apache.sis.xml.bind.metadata.code.MD_ReferenceSystemTypeCode;
import org.apache.sis.xml.bind.metadata.code.SV_ParameterDirection;
import org.apache.sis.xml.bind.metadata.code.DQM_ValueStructure;
