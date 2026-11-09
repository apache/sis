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
 * Styled Layer Descriptor document model, for describing which styles apply to which layers.
 * The classes in this package are derived from
 * OGC 05-078r4 — <a href="https://www.ogc.org/standard/sld/">Styled Layer Descriptor</a> Implementation Specification 1.1.0.
 * That document defines the {@code <StyledLayerDescriptor>} root element and its children,
 * which reference or embed the Symbology Encoding elements defined in the
 * {@link org.apache.sis.style.se1} package.
 * The root element is {@link org.apache.sis.style.sld1.StyledLayerDescriptor}.
 * The {@code <UserStyle>} element is represented by {@link org.apache.sis.style.se1.Symbology}.
 *
 * <h2>Future evolution</h2>
 * This package defines an XML encoding. It is not an abstract model for sophisticated styling.
 * More generic styling API may be provided in a future Apache SIS version in the {@code org.apache.sis.style} package.
 *
 * <h2>Synchronization</h2>
 * Unless otherwise specified in the Javadoc, classes in this package are not thread-safe.
 * Synchronization, if desired, must be done by the caller.
 *
 * @author  Johann Sorel (Geomatys)
 */
package org.apache.sis.style.sld1;
