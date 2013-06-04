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
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.NoSuchElementException;
import org.apache.sis.util.Localized;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.WarningListener;


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
     * The listeners, or {@code null} if none. This is a <cite>copy on write</cite> array:
     * no elements are modified once the array have been created.
     */
    private WarningListener<? super DataStore>[] listeners;

    /**
     * Creates a new instance with initially no listener.
     */
    public AbstractDataStore() {
        locale = Locale.getDefault(Locale.Category.DISPLAY);
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
    public synchronized void addWarningListener(final WarningListener<? super DataStore> listener)
            throws IllegalArgumentException
    {
        ArgumentChecks.ensureNonNull("listener", listener);
        final WarningListener<? super DataStore>[] current = listeners;
        final int length = (current != null) ? current.length : 0;

        @SuppressWarnings({"unchecked", "rawtypes"}) // Generic array creation.
        final WarningListener<? super DataStore>[] copy = new WarningListener[length + 1];
        for (int i=0; i<length; i++) {
            final WarningListener<? super DataStore> c = current[i];
            if (c == listener) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.ElementAlreadyPresent_1, listener));
            }
            copy[i] = c;
        }
        copy[length] = listener;
        listeners = copy;
    }

    /**
     * Removes a previously registered listener.
     *
     * @param  listener The listener to remove.
     * @throws NoSuchElementException If the given listener is not registered in this data store.
     */
    @Override
    public synchronized void removeWarningListener(final WarningListener<? super DataStore> listener)
            throws NoSuchElementException
    {
        final WarningListener<? super DataStore>[] current = listeners;
        if (current != null) {
            for (int i=0; i<current.length; i++) {
                if (current[i] == listener) {
                    if (current.length == 1) {
                        listeners = null;
                    } else {
                        @SuppressWarnings({"unchecked", "rawtypes"}) // Generic array creation.
                        final WarningListener<? super DataStore>[] copy = new WarningListener[current.length - 1];
                        System.arraycopy(current, 0, copy, 0, i);
                        System.arraycopy(current, i+1, copy, i, copy.length - i);
                        listeners = copy;
                    }
                    return;
                }
            }
        }
        throw new NoSuchElementException(Errors.format(Errors.Keys.NoSuchElement_1, listener));
    }

    /**
     * Invoked when a new warning has been emitted.
     * The default implementation makes the following choice:
     *
     * <ul>
     *   <li>If at least one warning listener has been {@linkplain #addWarningListener(WarningListener) registered},
     *       then this method notifies all listeners and the log record is <strong>not</strong> logged.</li>
     *   <li>Otherwise this method logs the given record to the logger returned by {@link #getLogger()}</li>
     * </ul>
     *
     * @param warning The warning message together with programmatic information.
     *
     * @see WarningListener#warningOccured(Object, LogRecord)
     */
    protected void fireWarningOccurred(final LogRecord warning) {
        final WarningListener[] current;
        synchronized (this) {
            current = listeners;
        }
        if (current != null) {
            for (final WarningListener<? super DataStore> listener : listeners) {
                listener.warningOccured(this, warning);
            }
        } else {
            final Logger logger = getLogger();
            warning.setLoggerName(logger.getName());
            logger.log(warning);
        }
    }

    /**
     * Returns the logger where to send warnings when there is registered warning listeners.
     * This logger may also be used for other purpose like configuration or debugging information.
     *
     * <p>The default implementation returns the {@code "org.apache.sis.storage"} logger.
     * Subclasses should override for specifying a logger in their own namespace.</p>
     *
     * @return The logger where to send warnings and other messages produced by this data store.
     */
    protected Logger getLogger() {
        return Logging.getLogger(DataStore.class);
    }
}
