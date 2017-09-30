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
 * Adapters for (un)marshalling ISO 19103 generic names.
 * For example, a {@link java.lang.String} value has to be marshalled this way:
 * Classes prefixed by two letters, like {@code "GO_GenericName"}, are also wrappers around the actual
 * object to be marshalled. See the {@link org.apache.sis.internal.jaxb.metadata} package for more
 * explanation about wrappers. Note that the two-letters prefixes used in this package (not to be
 * confused with the three-letters prefixes used in XML documents) are not defined by OGC/ISO
 * specifications; they are used only for consistency with current practice in
 * {@link org.apache.sis.internal.jaxb.metadata} and similar packages.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.3
 * @module
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.GCO, xmlns = {
    @XmlNs(prefix = "gco", namespaceURI = Namespaces.GCO),
    @XmlNs(prefix = "gml", namespaceURI = Namespaces.GML)
})
@XmlAccessorType(XmlAccessType.NONE)
/*
 * Do NOT define a package-level adapter for InternationalString,
 * because such adapter shall NOT apply to GO_CharacterString.getAnchor().
 */
package org.apache.sis.internal.jaxb.name;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import org.apache.sis.xml.Namespaces;
