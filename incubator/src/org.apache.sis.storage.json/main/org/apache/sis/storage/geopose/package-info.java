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
 * Mapping for OGC GeoPose 1.0 JSON.
 *
 * <p>{@link org.apache.sis.storage.geopose.GeoPose} reads and writes documents,
 * {@link org.apache.sis.storage.geopose.GeoPoseMapper} converts the bindings to Apache SIS
 * referencing and geometry types, and {@link org.apache.sis.storage.geopose.LocalTangentPlane}
 * builds the east-north-up reference system which is the inner frame of the Basic targets.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0 Data Exchange Standard"
 */
package org.apache.sis.storage.geopose;
