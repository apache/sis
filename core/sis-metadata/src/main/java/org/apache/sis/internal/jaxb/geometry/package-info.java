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
 * JAXB adapters for geometries.
 * This package regroups all adapters mapping GeoAPI interfaces to their SIS
 * implementation. We must use adapters since JAXB can not annotate interfaces.
 * Consequently the purpose of these adapters is to replace interfaces.
 *
 * <p>Every time JAXB tries to marshall or unmarshall an interface, the adapter
 * will be substituted to that interface.</p>
 *
 * @author  Guilhem Legal (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.GML, xmlns = {
    @XmlNs(prefix = "gml", namespaceURI = Namespaces.GML)
})
@XmlAccessorType(XmlAccessType.NONE)
package org.apache.sis.internal.jaxb.geometry;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import org.apache.sis.xml.Namespaces;
