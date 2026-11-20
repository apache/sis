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
 * Conversions between GeoJSON (<a href="https://datatracker.ietf.org/doc/html/rfc7946">RFC 7946</a>) objects
 * and SIS features, geometries and metadata.
 *
 * <p>{@link GeoJSON} and {@link GeoJSONMapper} convert GeoJSON features and geometries.
 * {@link MetadataJSON} reads and writes SIS metadata as ISO/TR 19115-4 metadata records,
 * which are GeoJSON features with the {@code MD_Metadata} content as {@code properties}.
 * The JSON bindings are in the {@code binding} sub-package.
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026"
 * @see <a href="https://schemas.isotc211.org/19115/-4/">ISO 19115-4 JSON schemas</a>
 */
package org.apache.sis.storage.geojson;
