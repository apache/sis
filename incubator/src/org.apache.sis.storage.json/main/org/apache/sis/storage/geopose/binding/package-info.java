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
 * JSON bindings for the OGC GeoPose 1.0 data exchange standard.
 *
 * <p>Each class is the direct image of one JSON schema published at
 * <a href="https://schemas.opengis.net/geopose/1.0/schemata/">schemas.opengis.net/geopose/1.0</a>.
 * The nine standardization targets are {@link org.apache.sis.storage.geopose.binding.BasicYPR},
 * {@link org.apache.sis.storage.geopose.binding.BasicQuaternion},
 * {@link org.apache.sis.storage.geopose.binding.BasicEuler},
 * {@link org.apache.sis.storage.geopose.binding.Advanced},
 * {@link org.apache.sis.storage.geopose.binding.Chain},
 * {@link org.apache.sis.storage.geopose.binding.Graph},
 * {@link org.apache.sis.storage.geopose.binding.RegularSeries},
 * {@link org.apache.sis.storage.geopose.binding.IrregularSeries} and
 * {@link org.apache.sis.storage.geopose.binding.Stream}.</p>
 *
 * <ul>
 *   <li>These classes carry no behavior beyond JSON mapping and equality.</li>
 *   <li>Conversion to Apache SIS referencing and geometry types is done by the
 *       {@code org.apache.sis.storage.geopose} package.</li>
 *   <li>Times are expressed in milliseconds since 1970-01-01T00:00:00Z, as in the standard.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0 Data Exchange Standard"
 */
package org.apache.sis.storage.geopose.binding;
