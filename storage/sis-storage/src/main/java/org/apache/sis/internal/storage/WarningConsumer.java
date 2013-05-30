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

import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.NoSuchElementException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.WarningListener;


/**
 * The leaf of a chain of {@link WarningProducer}, which hold the list of {@link WarningListener}s to notify.
 *
 * @param <T> The type of the object declared as warnings emitter.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final class WarningConsumer<T> extends WarningProducer {
    /**
     * The declared source of warnings. This is not necessarily the real source,
     * but this is the source that we declare in public API.
     */
    private final T source;

    /**
     * Where to log the warnings when there is no registered listener.
     */
    private final Logger logger;

    /**
     * The listeners, or {@code null} if none. This is a <cite>copy on write</cite> array:
     * no elements are modified once the array have been created.
     */
    private volatile WarningListener<? super T>[] listeners;

    /**
     * Creates a new instance with initially no listener.
     * Warnings will be logger to the given logger, unless at least one listener is registered.
     *
     * @param source The declared source of warnings. This is not necessarily the real source,
     *               but this is the source that we declare in public API.
     * @param logger Where to log the warnings when there is no registered listener.
     */
    public WarningConsumer(final T source, final Logger logger) {
        super(null);
        this.source = source;
        this.logger = logger;
    }

    /**
     * Invoked when a new warning has been emitted. This method notifies the listeners if any,
     * or log the warning otherwise.
     */
    @Override
    void sendWarning(final LogRecord record) {
        final WarningListener[] current = listeners;
        if (current != null) {
            for (final WarningListener<? super T> listener : listeners) {
                listener.warningOccured(source, record);
            }
        } else {
            record.setLoggerName(logger.getName());
            logger.log(record);
        }
    }

    /**
     * Adds a listener to be notified when a warning occurred while reading from or writing to the storage.
     *
     * @param  listener The listener to add.
     * @throws IllegalArgumentException If the given listener is already registered in this data store.
     */
    public void addWarningListener(final WarningListener<? super T> listener) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("listener", listener);
        final WarningListener<? super T>[] current = listeners;
        final int length = (current != null) ? current.length : 0;

        @SuppressWarnings({"unchecked", "rawtypes"}) // Generic array creation.
        final WarningListener<? super T>[] copy = new WarningListener[length + 1];
        for (int i=0; i<length; i++) {
            final WarningListener<? super T> c = current[i];
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
    public void removeWarningListener(final WarningListener<? super T> listener) throws NoSuchElementException {
        final WarningListener<? super T>[] current = listeners;
        if (current != null) {
            for (int i=0; i<current.length; i++) {
                if (current[i] == listener) {
                    if (current.length == 1) {
                        listeners = null;
                    } else {
                        @SuppressWarnings({"unchecked", "rawtypes"}) // Generic array creation.
                        final WarningListener<? super T>[] copy = new WarningListener[current.length - 1];
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
}
