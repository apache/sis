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
package org.apache.sis.internal.xml;

import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * Common base class for {@code StaxStreamReader} and {@code StaxStreamWriter}.
 * {@code StaxStreamIO} subclasses are not used directly (they are Apache SIS internal mechanic);
 * they are rather used as helper classes for {@link org.apache.sis.storage.DataStore} implementations.
 * Those {@code DataStore}s will typically manage {@code StaxStreamReader} and {@code StaxStreamWriter}
 * instances on which they delegate their read and write operations.
 *
 * <div class="section">Multi-threading</div>
 * This class and subclasses are not tread-safe. Synchronization shall be done by the {@code DataStore}
 * that contains the {@code StaxStreamIO} instances.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
abstract class StaxStreamIO implements AutoCloseable {
    /**
     * The data store for which this reader or writer has been created.
     */
    protected final StaxDataStore owner;

    /**
     * For sub-classes constructors.
     *
     * @param owner  the data store for which this reader or writer is created.
     */
    StaxStreamIO(final StaxDataStore owner) {
        ArgumentChecks.ensureNonNull("owner", owner);
        this.owner = owner;
    }

    /**
     * Returns the error resources in the current locale.
     */
    protected final Errors errors() {
        return Errors.getResources(owner.getLocale());
    }
}
