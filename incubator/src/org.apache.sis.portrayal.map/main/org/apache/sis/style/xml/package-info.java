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
 * Reader and writer of OGC Styled Layer Descriptor and Symbology Encoding documents, using the StAX cursor API.
 * The documents are read into, and written from, the objects of the {@link org.apache.sis.style.sld1}
 * and {@link org.apache.sis.style.se1} packages. The entry points are {@link org.apache.sis.style.xml.StyleReader}
 * and {@link org.apache.sis.style.xml.StyleWriter}.
 *
 * <p>All official versions of the standards are supported:</p>
 * <ul>
 *   <li>Styled Layer Descriptor 1.0.0 (OGC 02-070), with Filter Encoding 1.0.0 and GML 2;</li>
 *   <li>Styled Layer Descriptor 1.1.0 (OGC 05-078r4) and Symbology Encoding 1.1.0 (OGC 05-077r4),
 *       with Filter Encoding 1.1.0 and GML 3.1.</li>
 * </ul>
 *
 * The reader accepts all versions without the need to know the version in advance.
 * The writer targets the version specified by the caller.
 * The geometries embedded in spatial filters are read and written with the {@link org.apache.sis.gml} package.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 2.0
 * @since SIS 2.0
 */
package org.apache.sis.style.xml;
