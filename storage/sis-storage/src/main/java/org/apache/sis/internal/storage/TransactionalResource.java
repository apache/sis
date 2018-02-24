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

import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;


/**
 * Identifies resources capable to create transactions. A {@code TransactionalResource} is not directly writable,
 * but the transactions created by {@link #newTransaction()} are writable. Each transaction is a unit of work
 * independent of other transactions until a commit or rollback operation is performed.
 *
 * <p>Common cases of transactional resources are {@link DataStore} backed by databases or by
 * transactional Web Feature Services (WFS-T).</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public interface TransactionalResource extends Resource {
    /**
     * Starts a new transaction on this resource.
     * The returned resource should have the same capabilities than this resource
     * with the addition of write capabilities.
     *
     * @return a new writable resource that can be changed in a all-or-nothing way.
     * @throws DataStoreException if an error occurred while creating the transaction.
     */
    ResourceTransaction newTransaction() throws DataStoreException;
}
