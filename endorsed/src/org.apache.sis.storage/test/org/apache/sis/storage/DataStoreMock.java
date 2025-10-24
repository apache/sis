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

import java.util.Optional;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.storage.event.StoreListeners;


/**
 * A dummy data store
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DataStoreMock extends DataStore {
    /**
     * The display name.
     */
    private final String name;

    /**
     * Creates a new data store mock with the given display name.
     *
     * @param  name  data store display name.
     */
    public DataStoreMock(final String name) {
        this.name = name;
    }

    /**
     * Returns the display name specified at construction time.
     */
    @Override
    public String getDisplayName() {
        return name;
    }

    /**
     * Returns empty optional since there are no open parameters.
     */
    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        return Optional.empty();
    }

    /**
     * Returns {@code null} since there is no metadata.
     */
    @Override
    public Metadata getMetadata() {
        return null;
    }

    /**
     * Notifies listeners if any. Otherwise does nothing.
     */
    @Override
    public void close() {
        listeners.close();
    }

    /**
     * Opens access to the data store listeners.
     *
     * @return the data store listeners.
     */
    public StoreListeners listeners() {
        return listeners;
    }

    /**
     * Sends a pseudo-warning message for testing purpose. This method is defined in this class
     * for allowing {@link StoreListeners} to detect that the warning come from a data store by
     * inspecting the stack frame.
     *
     * @param  message  the message to send.
     */
    public void simulateWarning(String message) {
        listeners.warning(message);
    }

    /**
     * Returns a new dummy child having this data store as a parent.
     *
     * @return a dummy child.
     */
    public Resource newChild() {
        return new AbstractResource(this) {
        };
    }
}
