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
 * Provides methods for marshalling and unmarshalling SIS objects in XML.
 * The XML format is compliant with ISO 19139 specification for metadata, and
 * compliant with GML for referencing objects.
 *
 * <p>The main class in this package is {@link org.apache.sis.xml.XML}, which provides
 * property keys that can be used for configuring (un)marshallers and convenience
 * static methods. For example the following code:</p>
 *
 * {@preformat java
 *     XML.marshal(Citations.OGC, System.out);
 * }
 *
 * will produce a string like below:
 *
 * {@preformat xml
 *   <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
 *   <gmd:CI_Citation xmlns:gmd="http://www.isotc211.org/2005/gmd"
 *                    xmlns:gco="http://www.isotc211.org/2005/gco">
 *     <gmd:title>
 *       <gco:CharacterString>Open Geospatial Consortium</gco:CharacterString>
 *     </gmd:title>
 *     ... much more XML below this point ...
 *   </gmd:CI_Citation>
 * }
 *
 * <div class="section">Customizing the XML</div>
 * In order to parse and format ISO 19139 compliant documents, SIS needs its own
 * {@link javax.xml.bind.Marshaller} and {@link javax.xml.bind.Unmarshaller} instances
 * (which are actually wrappers around standard instances). Those instances are created
 * and cached by {@link org.apache.sis.xml.MarshallerPool}, which is used internally by
 * the above-cited {@code XML} class. However developers can instantiate their own
 * {@code MarshallerPool} in order to get more control on the marshalling and unmarshalling
 * processes, including the namespace URLs and the errors handling.
 *
 * <p>The most common namespace URLs are defined in the {@link org.apache.sis.xml.Namespaces} class.
 * The parsing of some objects like {@link java.net.URL} and {@link java.util.UUID},
 * together with the behavior in case of parsing error, can be specified by the
 * {@link org.apache.sis.xml.ValueConverter} class.</p>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
package org.apache.sis.xml;
