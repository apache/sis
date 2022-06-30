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
package org.apache.sis.internal.storage;

import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.Resource;


/**
 * A resource produced directly by a data store.
 * This interface can be implemented by the following resources:
 *
 * <ul>
 *   <li>{@link DataStore} itself, in which case {@code getOriginator()} returns {@code this}.</li>
 *   <li>Resources returned by the {@link DataStore#findResource(String)} method.</li>
 *   <li>If the data store is an aggregate, resources returned by {@link Aggregate#components()}.</li>
 * </ul>
 *
 * This interface should <em>not</em> be implemented by resources that are the result of some operation,
 * including filtering applied by {@code subset(Query)} methods.
 *
 * <h2>Use case</h2>
 * This interface provides information about which {@link DataStore} produced this resource.
 * It allows for example to fetch the {@linkplain DataStore#getOpenParameters() parameters}
 * used for opening the data store. Combined with the {@linkplain #getIdentifier() resource identifier},
 * it makes possible to save information needed for reopening the same resource later.
 * This use case is the reason why this interface should be implemented only by resources produced
 * <em>directly</em> by a data store, because otherwise the parameters and identifiers would not be
 * sufficient information for identifying the resource.
 *
 * <h2>Future evolution</h2>
 * This interface is not yet in public API. Whether we should commit this interface in public API
 * is an open question. See <a href="https://issues.apache.org/jira/browse/SIS-416">SIS-416</a>.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public interface StoreResource extends Resource {
    /**
     * Returns the data store that produced this resource.
     * If this resource is already a {@link DataStore} instance, then this method returns {@code this}.
     *
     * @return the data store that created this resource.
     */
    DataStore getOriginator();
}
