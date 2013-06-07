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
 * {@linkplain org.apache.sis.storage.DataStore Data store} base types for retrieving and saving geospatial data
 * in various storage formats.
 *
 * <p>Different {@code DataStore} implementations will want different kind of input/output objects. Some examples are
 * {@link java.lang.String}, {@link java.nio.file.Path}, {@link java.io.File}, {@link java.net.URI}, {@link java.net.URL},
 * {@link java.io.InputStream}, {@link javax.imageio.stream.ImageInputStream}, {@link java.nio.channels.ReadableChannel},
 * JDBC {@link java.sql.Connection} or {@link javax.sql.DataSource}, or even
 * datastore-specific objects like {@link ucar.nc2.NetcdfFile}.
 * Because of this variety, SIS does not know which kind of object to accept before the appropriate {@code DataStore}
 * instance has been found. For this reason, storages are represented by arbitrary {@link java.lang.Object} encapsulated
 * in {@link org.apache.sis.storage.StorageConnector}. The later can open the object in various ways, for example
 * as {@link java.io.DataInput} or as {@link java.nio.ByteBuffer}, depending on {@code DataStore needs}.
 * Future versions may contain additional information like login/password.</p>
 *
 * <p>{@code StorageConnector} is used only for the "discovery" phase, and discarded once the actual
 * {@code DataStore} instance has been created.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.10)
 * @version 0.3
 * @module
 */
package org.apache.sis.storage;
