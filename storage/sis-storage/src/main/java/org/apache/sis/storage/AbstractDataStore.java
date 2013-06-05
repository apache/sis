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
import org.apache.sis.util.Localized;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.WarningListener;
import org.apache.sis.util.logging.WarningListeners;


/**
 * Skeleton implementation of {@link DataStore}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public abstract class AbstractDataStore implements DataStore, Localized {
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
    protected AbstractDataStore() {
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
     * Adds a listener to be notified when a warning occurred while reading from or writing to the storage.
     *
     * @param  listener The listener to add.
     * @throws IllegalArgumentException If the given listener is already registered in this data store.
     */
    @Override
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
    @Override
    public void removeWarningListener(final WarningListener<? super DataStore> listener)
            throws NoSuchElementException
    {
        listeners.removeWarningListener(listener);
    }
}
