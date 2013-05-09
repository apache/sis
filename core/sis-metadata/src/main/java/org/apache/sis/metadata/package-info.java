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
 * Root package for various metadata implementations. For a global overview of metadata in SIS,
 * see the <a href="{@docRoot}/../sis-metadata/index.html">Metadata page on the project web site</a>.
 *
 * <p>This root package can work with different {@linkplain org.apache.sis.metadata.MetadataStandard
 * metadata standards}, not just ISO 19115. In this package, a metadata standard is defined by a
 * collection of Java interfaces defined in a specific package and its sub-packages. For example
 * the {@linkplain org.apache.sis.metadata.MetadataStandard#ISO_19115 ISO 19115} standard is
 * defined by the interfaces in the {@link org.opengis.metadata} package and sub-packages.
 * This {@code org.apache.sis.metadata} package uses Java reflection for performing basic
 * operations like comparisons and copies.</p>
 *
 * <p>All metadata can be view {@linkplain org.apache.sis.metadata.AbstractMetadata#asMap() as a map}
 * for use with Java collections, or {@linkplain org.apache.sis.metadata.AbstractMetadata#asTreeTable()
 * as a tree table} for use in GUI applications.</p>
 *
 * <p>ISO 19115 metadata can be marshalled and unmarshalled in XML using the
 * {@link org.apache.sis.xml.XML} convenience methods.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.0)
 * @version 0.3
 * @module
 */
package org.apache.sis.metadata;
