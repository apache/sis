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
 * <p>{@code DataStore} provides the methods for reading or writing geospatial data in a given storage.
 * A storage may be a file, a directory, a connection to a database or any other implementation specific mechanism.
 * Suitable {@code DataStore} implementation for a given storage can be discovered and opened by the static methods
 * provided in {@link org.apache.sis.storage.DataStores}.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
package org.apache.sis.storage;
