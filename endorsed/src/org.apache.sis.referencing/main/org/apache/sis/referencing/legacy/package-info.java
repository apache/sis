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
 * Referencing objects that existed in legacy international standards but have been removed in more recent editions.
 * Those objects are kept for compatibility with, for example, Geographic Markup Language (<abbr>GML</abbr>) but are
 * not anymore in public <abbr>API</abbr>.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cédric Briançon (Geomatys)
 */
@XmlSchema(location = "http://schemas.opengis.net/gml/3.2.1/coordinateReferenceSystems.xsd",
           elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.GML, xmlns =
{
    @XmlNs(prefix = "gml", namespaceURI = Namespaces.GML),
    @XmlNs(prefix = "gmd", namespaceURI = LegacyNamespaces.GMD),
    @XmlNs(prefix = "gco", namespaceURI = Namespaces.GCO),
    @XmlNs(prefix = "xsi", namespaceURI = Namespaces.XSI)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    /*
     * Do NOT declare the following adapters in this package-info:
     *
     *   - CS_CoordinateSystem
     *   - SC_SingleCRS
     *   - SC_CRS
     *
     * Because the above types are the base type of many other types,
     * adding the above adapters is a cause of confusion for JAXB.
     *
     * Note: be careful with CS_AffineCS and CS_CartesianCS relationship.
     */
    @XmlJavaTypeAdapter(CD_ImageDatum.class),
    @XmlJavaTypeAdapter(CS_CartesianCS.class),      // Must be before CS_AffineCS.
    @XmlJavaTypeAdapter(CS_AffineCS.class)
})
package org.apache.sis.referencing.legacy;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.bind.referencing.*;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
