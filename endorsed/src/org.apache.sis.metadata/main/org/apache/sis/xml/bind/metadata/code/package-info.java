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
 * JAXB adapters for code {@linkplain org.opengis.util.CodeList code lists}.
 * Every time JAXB will try to marshal or unmarshal a code list, an adapter will replace the
 * code list value (which would otherwise be written directly by JAXB) by an element like below:
 *
 * <ul>
 *   <li>
 *     {@linkplain org.opengis.metadata.citation.Role Role}:
 *     {@code <cit:CI_RoleCode
 *       codeList="http://standards.iso.org/iso/19115/resources/Codelist/cat/codelists.xml#CI_RoleCode"
         codeListValue="originator">Originator</cit:CI_RoleCode>}
 *   </li>
 * </ul>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 *
 * @see jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter
 * @see org.opengis.util.CodeList
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, xmlns = {
    @XmlNs(prefix = "mri", namespaceURI = Namespaces.MRI),      // Metadata for Resource Identification
    @XmlNs(prefix = "mrc", namespaceURI = Namespaces.MRC),      // Metadata for Resource Content
    @XmlNs(prefix = "mrd", namespaceURI = Namespaces.MRD),      // Metadata for Resource Distribution
    @XmlNs(prefix = "mmi", namespaceURI = Namespaces.MMI),      // Metadata for Maintenance Information
    @XmlNs(prefix = "msr", namespaceURI = Namespaces.MSR),      // Metadata for Spatial Representation
    @XmlNs(prefix = "mex", namespaceURI = Namespaces.MEX),      // Metadata with Schema Extensions
    @XmlNs(prefix = "mac", namespaceURI = Namespaces.MAC),      // Metadata for Acquisition
    @XmlNs(prefix = "mdq", namespaceURI = Namespaces.MDQ),      // Metadata for Data Quality
    @XmlNs(prefix = "dqm", namespaceURI = Namespaces.DQM),      // Metadata for Data Quality Measures
    @XmlNs(prefix = "mco", namespaceURI = Namespaces.MCO),      // Metadata for Constraints
    @XmlNs(prefix = "srv", namespaceURI = Namespaces.SRV),      // Metadata for Services
    @XmlNs(prefix = "cit", namespaceURI = Namespaces.CIT),      // Citation and responsible party information
    @XmlNs(prefix = "lan", namespaceURI = Namespaces.LAN),      // Language localization
    @XmlNs(prefix = "mcc", namespaceURI = Namespaces.MCC),      // Metadata Common Classes
    @XmlNs(prefix = "gmd", namespaceURI = LegacyNamespaces.GMD)
})
package org.apache.sis.xml.bind.metadata.code;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
