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

import java.lang.reflect.Field;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.util.logging.WarningListeners;


/**
 * Base implementation of resources contained in data stores.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public abstract class AbstractResource implements Resource {
    /**
     * An accessor to the {@code DataStore.listeners} protected field.
     * This hack will be removed if we move {@code AbstractResource} to the {@code org.apache.sis.storage} package.
     */
    private static final Field LISTENERS;
    static {
        try {
            LISTENERS = DataStore.class.getDeclaredField("listeners");
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
        LISTENERS.setAccessible(true);
    }

    /**
     * The data store which contains this resource.
     */
    protected final DataStore store;

    /**
     * Creates a new resource.
     *
     * @param store  the data store which contains this resource.
     */
    protected AbstractResource(final DataStore store) {
        this.store = store;
    }

    /**
     * Returns the set of registered warning listeners for the data store.
     *
     * @return the registered warning listeners for the data store.
     */
    @SuppressWarnings("unchecked")
    protected final WarningListeners<DataStore> listeners() {
        try {
            return (WarningListeners<DataStore>) LISTENERS.get(store);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);        // Should never happen since we have made the field accessible.
        }
    }
}
