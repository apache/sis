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
 * Provides methods for marshalling and unmarshalling SIS objects in <abbr>XML</abbr>.
 * The <abbr>XML</abbr> format is compliant with <abbr>ISO</abbr> 19115-3 specification for metadata,
 * and compliant with <abbr>GML</abbr> for referencing objects.
 *
 * <p>The main class in this package is {@link org.apache.sis.xml.XML}, which provides
 * property keys that can be used for configuring (un)marshallers and convenience static methods.
 * For example, the following code:</p>
 *
 * {@snippet lang="java" :
 *     XML.marshal(Citations.OGC, System.out);
 *     }
 *
 * will produce a string like below:
 *
 * {@snippet lang="xml" :
 *   <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
 *   <cit:CI_Citation xmlns:gmd="http://www.isotc211.org/2005/gmd"
 *                    xmlns:gco="http://www.isotc211.org/2005/gco">
 *     <cit:title>
 *       <gco:CharacterString>Open Geospatial Consortium</gco:CharacterString>
 *     </cit:title>
 *     ... much more XML below this point ...
 *   </cit:CI_Citation>
 *   }
 *
 * <h2>Customizing the <abbr>XML</abbr></h2>
 * In order to parse and format <abbr>ISO</abbr> 19115-3 compliant documents,
 * Apache <abbr>SIS</abbr> needs its own {@link jakarta.xml.bind.Marshaller}
 * and {@link jakarta.xml.bind.Unmarshaller} instances
 * (which are actually wrappers around standard instances).
 * Those instances are created and cached by {@link org.apache.sis.xml.MarshallerPool}.
 * Developers can instantiate their own {@code MarshallerPool} if they need to configure,
 * properties such as the namespace <abbr>URL</abbr>s and the errors handling.
 *
 * <p>The most common namespace <abbr>URL</abbr>s are defined in the {@link org.apache.sis.xml.Namespaces} class.
 * The parsing of some objects like {@link java.net.URL} and {@link java.util.UUID},
 * together with the behavior in case of parsing error, can be specified by the
 * {@link org.apache.sis.xml.ValueConverter} class.</p>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.5
 * @since   0.3
 */
package org.apache.sis.xml;
