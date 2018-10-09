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
package org.apache.sis.storage;

import org.apache.sis.storage.event.ChangeEvent;
import org.apache.sis.storage.event.ChangeListener;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;


/**
 * A dummy data store
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
final strictfp class DataStoreMock extends DataStore {
    /**
     * The display name.
     */
    private final String name;

    /**
     * Creates a new data store mock with the given display name.
     */
    DataStoreMock(final String name) {
        this.name = name;
    }

    /**
     * Mock data store has no identifier.
     * @return null
     */
    @Override
    public GenericName getIdentifier() {
        return null;
    }

    /**
     * Returns the display name specified at construction time.
     */
    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public ParameterValueGroup getOpenParameters() {
        return null;
    }

    @Override
    public Metadata getMetadata() {
        return null;
    }

    @Override
    public <T extends ChangeEvent> void addListener(ChangeListener<? super T> listener, Class<T> eventType) {
    }

    @Override
    public <T extends ChangeEvent> void removeListener(ChangeListener<? super T> listener, Class<T> eventType) {
    }

    @Override
    public void close() {
    }
}
