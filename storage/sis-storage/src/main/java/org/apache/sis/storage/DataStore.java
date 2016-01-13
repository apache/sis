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

import java.util.Locale;
import java.util.NoSuchElementException;
import org.opengis.metadata.Metadata;
import org.apache.sis.util.Localized;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.WarningListener;
import org.apache.sis.util.logging.WarningListeners;

// Related to JDK7
import org.apache.sis.internal.jdk7.AutoCloseable;


/**
 * Manages a series of features, coverages or sensor data.
 *
 * <div class="section">Thread safety policy</div>
 * This {@code DataStore} base class is thread-safe. However subclasses are usually not.
 * Unless otherwise specified by subclasses, users should assume that {@code DataStore}
 * instances are not thread-safe.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see DataStores#open(Object)
 */
@AutoCloseable
public abstract class DataStore implements Localized {
    /**
     * The locale to use for formatting warnings.
     *
     * @see #getLocale()
     * @see #setLocale(Locale)
     */
    private Locale locale;

    /**
     * The set of registered {@link WarningListener}s for this data store.
     */
    protected final WarningListeners<DataStore> listeners;

    /**
     * Creates a new instance with initially no listener.
     */
    protected DataStore() {
        locale = Locale.getDefault();
        listeners = new WarningListeners<DataStore>(this);
    }

    /**
     * The locale to use for formatting warnings and other messages. This locale if for user interfaces
     * only - it has no effect on the data to be read or written from/to the data store.
     *
     * <p>The default value is the {@linkplain Locale#getDefault() system default locale}.</p>
     */
    @Override
    public synchronized Locale getLocale() {
        return locale;
    }

    /**
     * Sets the locale to use for formatting warnings and other messages.
     *
     * @param locale The new locale to use.
     */
    public synchronized void setLocale(final Locale locale) {
        ArgumentChecks.ensureNonNull("locale", locale);
        this.locale = locale;
    }

    /**
     * Returns information about the dataset as a whole. The returned metadata object, if any, can contain
     * information such as the spatiotemporal extent of the dataset, contact information about the creator
     * or distributor, data quality, update frequency, usage constraints and more.
     *
     * @return Information about the dataset, or {@code null} if none.
     * @throws DataStoreException If an error occurred while reading the data.
     */
    public abstract Metadata getMetadata() throws DataStoreException;

    /**
     * Adds a listener to be notified when a warning occurred while reading from or writing to the storage.
     * When a warning occurs, there is a choice:
     *
     * <ul>
     *   <li>If this data store has no warning listener, then the warning is logged at
     *       {@link java.util.logging.Level#WARNING}.</li>
     *   <li>If this data store has at least one warning listener, then all listeners are notified
     *       and the warning is <strong>not</strong> logged by this data store instance.</li>
     * </ul>
     *
     * Consider invoking this method in a {@code try} … {@code finally} block if the {@code DataStore}
     * lifetime is longer than the listener lifetime, as below:
     *
     * {@preformat java
     *     datastore.addWarningListener(listener);
     *     try {
     *         // Do some work...
     *     } finally {
     *         datastore.removeWarningListener(listener);
     *     }
     * }
     *
     * @param  listener The listener to add.
     * @throws IllegalArgumentException If the given listener is already registered in this data store.
     */
    public void addWarningListener(final WarningListener<? super DataStore> listener)
            throws IllegalArgumentException
    {
        listeners.addWarningListener(listener);
    }

    /**
     * Removes a previously registered listener.
     *
     * @param  listener The listener to remove.
     * @throws NoSuchElementException If the given listener is not registered in this data store.
     */
    public void removeWarningListener(final WarningListener<? super DataStore> listener)
            throws NoSuchElementException
    {
        listeners.removeWarningListener(listener);
    }

    /**
     * Closes this data store and releases any underlying resources.
     *
     * @throws DataStoreException If an error occurred while closing this data store.
     */
    public abstract void close() throws DataStoreException;
}
