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
 * <p>Some objects defined in various standards have overlapping functionalities. For example the
 * Metadata (ISO 19115), Referencing by Coordinates (ISO 19111) and Web Processing Service (WPS)
 * all define their own parameter objects. An other example is ISO 19115 defining basic referencing
 * information, which is clearly ISO 19111 subject. GeoAPI tries to provide a uniform API by merging
 * objects, or by omitting an object from one standard in favor of the equivalent object of another
 * standard. However at XML (un)marshalling time, we still need to temporarily recreate the omitted
 * object as defined in the original standard. This package is used for such replacement.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.GMD, xmlns = {
    @XmlNs(prefix = "gmd", namespaceURI = Namespaces.GMD),
    @XmlNs(prefix = "srv", namespaceURI = Namespaces.SRV),
    @XmlNs(prefix = "gco", namespaceURI = Namespaces.GCO),
    @XmlNs(prefix = "xsi", namespaceURI = Namespaces.XSI)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(RS_Identifier.class),

    // Java types, primitive types and basic OGC types handling
    @XmlJavaTypeAdapter(StringAdapter.class),
    @XmlJavaTypeAdapter(InternationalStringAdapter.class),
    @XmlJavaTypeAdapter(GO_GenericName.class),
    @XmlJavaTypeAdapter(GO_Boolean.class), @XmlJavaTypeAdapter(type=boolean.class, value=GO_Boolean.class)
})
package org.apache.sis.internal.jaxb.metadata.replace;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.internal.jaxb.gco.GO_Boolean;
import org.apache.sis.internal.jaxb.gco.GO_GenericName;
import org.apache.sis.internal.jaxb.gco.StringAdapter;
import org.apache.sis.internal.jaxb.gco.InternationalStringAdapter;
import org.apache.sis.internal.jaxb.metadata.RS_Identifier;
import org.apache.sis.xml.Namespaces;
