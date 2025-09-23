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
 * Provides objects that simulate the behavior of complex or unavailable real objects.
 * Mocks are often used as a replacement for objects to be defined only in dependent modules.
 *
 * <p>This package does not provide all mocks defined by SIS, but only the mocks that could not be put in
 * the right package for the interface that they implement. To get a list of all mocks used in SIS tests,
 * we need to search for classes ending in {@code *Mock}.</p>
 *
 * <p>Objects defined in this package are only for SIS testing purpose any many change
 * in any future version without notice.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.GML, xmlns = {
    @XmlNs(prefix = "gml", namespaceURI = Namespaces.GML),
    @XmlNs(prefix = "mdb", namespaceURI = Namespaces.MDB),
    @XmlNs(prefix = "gco", namespaceURI = Namespaces.GCO),
    @XmlNs(prefix = "gmd", namespaceURI = LegacyNamespaces.GMD),
    @XmlNs(prefix = "gfc", namespaceURI = LegacyNamespaces.GFC)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(GO_GenericName.class)
})
package org.apache.sis.test.mock;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.bind.gco.GO_GenericName;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
