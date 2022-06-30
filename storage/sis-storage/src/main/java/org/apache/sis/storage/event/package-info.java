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
 * Provides interfaces and classes for dealing with different types of events fired by resources.
 * The different types of events are specified by the {@link StoreEvent} subclasses.
 * For example if a warning occurred while reading data from a file,
 * then the {@link org.apache.sis.storage.DataStore} implementation should fire a {@link WarningEvent}.
 *
 * <p>Events may occur in the following situations:</p>
 * <ul>
 *   <li>When a warning occurred.</li>
 *   <li>When the data store content changed (e.g. new feature instance added or removed).</li>
 *   <li>When the data store structure changed (e.g. a column is added in tabular data).</li>
 *   <li>Any other change at implementation choice.</li>
 * </ul>
 *
 * Users can {@linkplain org.apache.sis.storage.Resource#addListener declare their interest
 * to a specific kind of event}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   1.3
 * @version 1.0
 * @module
 */
package org.apache.sis.storage.event;
