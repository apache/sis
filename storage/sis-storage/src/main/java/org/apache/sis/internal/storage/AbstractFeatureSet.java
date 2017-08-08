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
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.util.logging.WarningListeners;


/**
 * Base implementation of feature sets contained in data stores.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 *
 * @todo this class may be removed if we refactor {@link FeatureSet} as an abstract class.
 */
public abstract class AbstractFeatureSet extends AbstractResource implements FeatureSet {
    /**
     * Creates a new resource.
     *
     * @param store      the data store which contains this resource.
     * @param listeners  the set of registered warning listeners for the data store.
     */
    protected AbstractFeatureSet(final DataStore store, final WarningListeners<DataStore> listeners) {
        super(store, listeners);
    }
}
