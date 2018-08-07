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
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;


/**
 * A writable resource containing data (features or coverages) that can be changed in a all-or-nothing way.
 * Resource transaction shall also implement at least one of the {@code Writable*} interfaces.
 * If the resource is an {@link Aggregate}, then this transaction includes both this resource
 * and all children resources.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public interface ResourceTransaction extends Resource {
    /**
     * Makes permanent all changes that have been in current transaction.
     * The current transaction contains the changes performed since the {@code ResourceTransaction}
     * creation, or since the last call to either the {@code commit()} or {@link #rollback()} methods.
     *
     * @throws DataStoreException if an error occurred while committing the transaction.
     *         In such case, the system shall be in state before the commit attempt.
     */
    void commit() throws DataStoreException;

    /**
     * Undoes all changes made in the current transaction.
     * The current transaction contains the changes performed since the {@code ResourceTransaction}
     * creation, or since the last call to either the {@link #commit()} or {@code rollback()} methods.
     *
     * @throws DataStoreException if an error occurred while rolling back the transaction.
     *         In such case, the system shall be in state before the rollback attempt.
     */
    void rollback() throws DataStoreException;
}
