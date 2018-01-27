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
 *     {@linkplain org.opengis.metadata.identification.CharacterSet character set}:
 *     {@code <gmd:MD_CharacterSetCode
 *       codeList="http://schemas.opengis.net/iso/19139/20070417/resources/Codelist/gmxCodelists.xml#MD_CharacterSetCode"
 *       codeListValue="utf8"/>}
 *   </li>
 * </ul>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 *
 * @see javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter
 * @see org.opengis.util.CodeList
 *
 * @since 0.3
 * @module
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, xmlns = {
    @XmlNs(prefix = "mdb", namespaceURI = Namespaces.MDB),
    @XmlNs(prefix = "cit", namespaceURI = Namespaces.CIT),
    @XmlNs(prefix = "lan", namespaceURI = Namespaces.LAN),
    @XmlNs(prefix = "mcc", namespaceURI = Namespaces.MCC),
    @XmlNs(prefix = "mex", namespaceURI = Namespaces.MEX),
    @XmlNs(prefix = "mmi", namespaceURI = Namespaces.MMI),
    @XmlNs(prefix = "mrc", namespaceURI = Namespaces.MRC),
    @XmlNs(prefix = "mri", namespaceURI = Namespaces.MRI),
    @XmlNs(prefix = "msr", namespaceURI = Namespaces.MSR),
    @XmlNs(prefix = "srv", namespaceURI = Namespaces.SRV),
    @XmlNs(prefix = "gmd", namespaceURI = Namespaces.GMD),
    @XmlNs(prefix = "gmi", namespaceURI = Namespaces.GMI)
})
package org.apache.sis.internal.jaxb.code;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import org.apache.sis.xml.Namespaces;
