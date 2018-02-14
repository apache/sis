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
 * A transaction resource.
 * This transaction include both this resource and all children resources
 * if it is an {@linkplain Aggregate}.
 *
 * @author Johann Sorel (Geomatys)
 * @since 1.0
 * @module
 */
public interface ResourceTransaction extends Resource {

    /**
     * Apply all the changes made in this transaction.
     *
     * @throws DataStoreException
     */
    void commit() throws DataStoreException;

    /**
     * Revert all changes made in this session.
     *
     * @throws DataStoreException
     */
    void rollback() throws DataStoreException;

}
