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
 * JAXB adapters for metadata objects without their wrapper. This package contains adapters for
 * the same objects than the ones handled by {@link org.apache.sis.internal.jaxb.metadata},
 * except that the XML is formatted in a "direct" way, without wrappers.
 *
 * <p><b>Example:</b> given an attribute named {@code "myAttribute"} of type
 * {@link org.opengis.metadata.citation.OnlineResource}, the adapter provided
 * in the parent package would marshal that attribute as below:</p>
 *
 * {@preformat xml
 *   <myAttribute>
 *     <gmd:CI_OnlineResource>
 *       <gmd:linkage>
 *         <gmd:URL>http://blabla.com</gmd:URL>
 *       </gmd:linkage>
 *     </gmd:CI_OnlineResource>
 *   </myAttribute>
 * }
 *
 * Using the adapter provided in this class, the result would rather be:
 *
 * {@preformat xml
 *   <myAttribute>
 *     <gmd:linkage>
 *       <gmd:URL>http://blabla.com</gmd:URL>
 *     </gmd:linkage>
 *   </myAttribute>
 * }
 *
 * @author  Guilhem Legal (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.GMD, xmlns = {
    @XmlNs(prefix = "gmd", namespaceURI = Namespaces.GMD),
    @XmlNs(prefix = "gco", namespaceURI = Namespaces.GCO)
})
package org.apache.sis.internal.jaxb.metadata.direct;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import org.apache.sis.xml.Namespaces;
