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
 * Extends some classes from the {@link org.apache.sis.metadata.iso} package in order
 * to give them the {@code "gmi"} namespace. This is required for XML (un)marshalling
 * because GeoAPI merged some classes which were dissociated in the ISO specifications.
 * The GeoAPI merge were done in order to simplify the conceptual model for developers,
 * since the classes were different in ISO specifications for historical reasons - not
 * conceptual reasons.
 *
 * <p>In SIS implementation, users need to care only about the public classes defined in
 * the {@link org.apache.sis.metadata.iso} package. When marshalling, the adapters will
 * inspect the properties that are ISO 19115-2 extensions and copy automatically the
 * {@code "gmd"} metadata into a {@code "gmi"} metadata if any ISO 19115-2 property is
 * non-null or non-empty. This work is performed by a {@code castOrCopy} static method
 * defined in each class of this package.</p>
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.GMI, xmlns = {
    @XmlNs(prefix = "gmi", namespaceURI = Namespaces.GMI),
    @XmlNs(prefix = "gco", namespaceURI = Namespaces.GCO)
})
@XmlAccessorType(XmlAccessType.NONE)
package org.apache.sis.internal.jaxb.gmi;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import org.apache.sis.xml.Namespaces;
