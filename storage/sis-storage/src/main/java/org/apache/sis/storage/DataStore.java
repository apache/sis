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

import java.util.Collection;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import org.apache.sis.internal.metadata.NameToIdentifier;
import org.opengis.metadata.Metadata;
import org.apache.sis.util.Localized;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.WarningListener;
import org.apache.sis.util.logging.WarningListeners;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.Identification;


/**
 * Manages a series of features, coverages or sensor data.
 *
 * <div class="section">Thread safety policy</div>
 * This {@code DataStore} base class is thread-safe. However subclasses do not need to be thread-safe.
 * Unless otherwise specified, users should assume that {@code DataStore} instances are not thread-safe.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 *
 * @see DataStores#open(Object)
 *
 * @since 0.3
 * @module
 */
public abstract class DataStore implements Localized, AutoCloseable {
    /**
     * The factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * This information can be useful for fetching information common to all {@code DataStore}
     * instances of the same class.
     *
     * @since 0.8
     */
    protected final DataStoreProvider provider;

    /**
     * The store name (typically filename) for formatting error messages, or {@code null} if unknown.
     * Shall <strong>not</strong> be used as an identifier.
     *
     * @see #getDisplayName()
     */
    private final String name;

    /**
     * The locale to use for formatting warnings.
     * This is not the locale for formatting data in the storage.
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
     * Creates a new instance with no provider and initially no listener.
     */
    protected DataStore() {
        provider  = null;
        name      = null;
        locale    = Locale.getDefault(Locale.Category.DISPLAY);
        listeners = new WarningListeners<>(this);
    }

    /**
     * Creates a new instance for the given storage (typically file or database).
     * The {@code provider} argument is an optional information.
     * The {@code connector} argument is mandatory.
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, reader instance, <i>etc</i>).
     * @throws DataStoreException if an error occurred while creating the data store for the given storage.
     *
     * @since 0.8
     */
    protected DataStore(final DataStoreProvider provider, final StorageConnector connector) throws DataStoreException {
        ArgumentChecks.ensureNonNull("connector", connector);
        this.provider  = provider;
        this.name      = connector.getStorageName();
        this.locale    = Locale.getDefault(Locale.Category.DISPLAY);
        this.listeners = new WarningListeners<>(this);
        /*
         * Above locale is NOT OptionKey.LOCALE because we are not talking about the same locale.
         * The one in this DataStore is for warning and exception messages, not for parsing data.
         */
    }

    /**
     * Returns a short name or label for this data store.
     * The returned name can be used in user interfaces or in error messages.
     * It may be a title in natural language, but should be relatively short.
     * The name may be localized in the language specified by the value of {@link #getLocale()}
     * if this data store is capable to produce a name in various languages.
     *
     * <p>This name should not be used as an identifier since there is no guarantee that the name
     * is unique among data stores, and no guarantee that the name is the same in all locales.
     * The name may also contain any Unicode characters, including characters usually not allowed
     * in identifiers like white spaces.</p>
     *
     * <p>This method should never throw an exception since it may be invoked for producing error
     * messages, in which case throwing an exception here would mask the original exception.</p>
     *
     * <p>Default implementation returns the {@link StorageConnector#getStorageName()} value,
     * or {@code null} if this data store has been created by the no-argument constructor.
     * Note that this default value may change in any future SIS version. Subclasses should
     * override this method if they can provide a better name.</p>
     *
     * @return a short name of label for this data store, or {@code null} if unknown.
     *
     * @since 0.8
     */
    public String getDisplayName() {
        return name;
    }

    /**
     * The locale to use for formatting warnings and other messages. This locale if for user interfaces
     * only – it has no effect on the data to be read or written from/to the data store.
     *
     * <p>The default value is the {@linkplain Locale#getDefault() system default locale}.</p>
     */
    @Override
    public synchronized Locale getLocale() {
        return locale;
    }

    /**
     * Sets the locale to use for formatting warnings and other messages.
     * In a client-server architecture, it should be the locale on the <em>client</em> side.
     *
     * <p>This locale is used on a <cite>best-effort</cite> basis; whether messages will honor this locale or not
     * depends on the code that logged warnings or threw exceptions. In Apache SIS implementation, this locale has
     * better chances to be honored by the {@link DataStoreException#getLocalizedMessage()} method rather than
     * {@code getMessage()}. See {@code getLocalizedMessage()} javadoc for more information.</p>
     *
     * @param locale  the new locale to use.
     *
     * @see DataStoreException#getLocalizedMessage()
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
     * @return information about the dataset, or {@code null} if none.
     * @throws DataStoreException if an error occurred while reading the data.
     */
    public abstract Metadata getMetadata() throws DataStoreException;

    /**
     * Get root data store resource.
     *
     * @return Resource, may be null
     * @throws DataStoreException if an I/O or decoding error occurs.
     */
    public abstract Resource getRootResource() throws DataStoreException;

    /**
     * Search for a resource identified by given name.
     *
     * @param  name  identifier of the data to acquire. Must be non-null.
     * @return resource associated to the given input name, never null.
     * @throws DataStoreException if an I/O error occurs
     * @throws IllegalNameException if input name is not found.
     */
    public Resource findResource(final String name) throws DataStoreException, IllegalNameException {
        ArgumentChecks.ensureNonEmpty("Name of the searched resource", name);

        final Resource root = getRootResource();
        if (root==null) throw new IllegalNameException("No resource found for name : "+name);

        //recursive search
        Object res = new Function<Resource,Object>() {
            @Override
            public Object apply(final Resource candidate) {

                final Metadata metadata;
                try { metadata = candidate.getMetadata(); }
                catch (DataStoreException ex) { return ex; }

                final boolean match = metadata.getIdentificationInfo().stream()
                   .map(Identification::getCitation)
                   .filter(Objects::nonNull)
                   .map(Citation::getIdentifiers)
                   .anyMatch((Collection<? extends Identifier> t) -> NameToIdentifier.isHeuristicMatchForIdentifier(t, name));

                Object result = match ? candidate : null;

                if (candidate instanceof Aggregate) {
                    final Aggregate agg = (Aggregate) candidate;
                    for (Resource comp : agg.components()) {
                        Object rr = apply(comp);
                        if (rr instanceof DataStoreException) {
                            return rr;
                        } else if (rr instanceof Resource) {
                            if (result!=null) {
                                return new IllegalNameException("Multiple resources match the name : "+name);
                            }
                            result = rr;
                        }
                    }
                }
                return result;
            }
        }.apply(root);

        if (res==null) {
            throw new IllegalNameException("No resource found for name : "+name);
        } else if (res instanceof DataStoreException) {
            throw (DataStoreException) res;
        }
        return (Resource) res;
    }

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
     * @param  listener  the listener to add.
     * @throws IllegalArgumentException if the given listener is already registered in this data store.
     */
    public void addWarningListener(final WarningListener<? super DataStore> listener)
            throws IllegalArgumentException
    {
        listeners.addWarningListener(listener);
    }

    /**
     * Removes a previously registered listener.
     *
     * @param  listener  the listener to remove.
     * @throws NoSuchElementException if the given listener is not registered in this data store.
     */
    public void removeWarningListener(final WarningListener<? super DataStore> listener)
            throws NoSuchElementException
    {
        listeners.removeWarningListener(listener);
    }

    /**
     * Closes this data store and releases any underlying resources.
     *
     * @throws DataStoreException if an error occurred while closing this data store.
     */
    @Override
    public abstract void close() throws DataStoreException;
}
