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
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.NoSuchElementException;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.identification.Identification;
import org.apache.sis.util.Localized;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.WarningListener;
import org.apache.sis.util.logging.WarningListeners;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.internal.util.Citations;


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
     * Returns information about the data store as a whole. The returned metadata object can contain
     * information such as the spatiotemporal extent of all contained {@linkplain Resource resources},
     * contact information about the creator or distributor, data quality, update frequency, usage constraints,
     * file format and more.
     *
     * @return information about resources in the data store, or {@code null} if none.
     * @throws DataStoreException if an error occurred while reading the data.
     *
     * @see Resource#getMetadata()
     */
    public abstract Metadata getMetadata() throws DataStoreException;

    /**
     * Returns the starting point from which all resources in this data store can be accessed.
     * A resource can be for example a air temperature map or the set of all bridges in a city.
     * If this data store contains only one resource, then that resource is returned directly.
     * Otherwise if this data store contains more than one resource, then this method returns
     * an {@link Aggregate} from which other resources can be accessed.
     *
     * @return the starting point of all resources in this data store,
     *         or {@code null} if this data store does not contain any resources.
     * @throws DataStoreException if an error occurred while reading the data.
     */
    public abstract Resource getRootResource() throws DataStoreException;

    /**
     * Searches for a resource identified by the given identifier.
     * The given identifier should match the following metadata element of a resource:
     *
     * <blockquote>{@link Resource#getMetadata() metadata} /
     * {@link org.apache.sis.metadata.iso.DefaultMetadata#getIdentificationInfo() identificationInfo} /
     * {@link org.apache.sis.metadata.iso.identification.AbstractIdentification#getCitation() citation} /
     * {@link org.apache.sis.metadata.iso.citation.DefaultCitation#getIdentifiers() identifier}</blockquote>
     *
     * Implementation may also accept aliases for convenience. For example if the full name of a resource
     * is {@code "foo:bar"}, then this method may accept {@code "bar"} as a synonymous of {@code "foo:bar"}
     * provided that it does not introduce ambiguity.
     *
     * <p>The default implementation verifies the {@linkplain #getRootResource() root resource}, then iterates over
     * components of {@link Aggregate}s. If a match is found without ambiguity, the associated resource is returned.
     * Otherwise an exception is thrown. Subclasses are encouraged to override this method with a more efficient
     * implementation.</p>
     *
     * @param  identifier  identifier of the resource to fetch. Must be non-null.
     * @return resource associated to the given identifier (never {@code null}).
     * @throws IllegalNameException if no resource is found for the given identifier, or if more than one resource is found.
     * @throws DataStoreException if another kind of error occurred while searching resources.
     */
    public Resource findResource(final String identifier) throws DataStoreException {
        ArgumentChecks.ensureNonEmpty("identifier", identifier);
        final Resource resource = findResource(identifier, getRootResource(), new IdentityHashMap<Resource,Boolean>());
        if (resource != null) {
            return resource;
        }
        throw new IllegalNameException(Resources.forLocale(getLocale())
                .getString(Resources.Keys.ResourceNotFound_2, getDisplayName(), identifier));
    }

    /**
     * Recursively searches for a resource identified by the given identifier.
     * This is the implementation of {@link #findResource(String)}.
     *
     * @param  identifier  identifier of the resource to fetch.
     * @param  candidate   a resource to compare against the identifier.
     * @param  visited     resources visited so-far, for avoiding never-ending loops if cycles exist.
     * @return resource associated to the given identifier, or {@code null} if not found.
     */
    private Resource findResource(final String identifier, final Resource candidate,
            final Map<Resource,Boolean> visited) throws DataStoreException
    {
        if (candidate != null && visited.put(candidate, Boolean.TRUE) == null) {
            final Metadata metadata = candidate.getMetadata();
            if (metadata != null) {
                for (final Identification identification : metadata.getIdentificationInfo()) {
                    if (identification != null) {                                                   // Paranoiac check.
                        if (Citations.identifierMatches(identification.getCitation(), null, identifier)) {
                            return candidate;
                        }
                    }
                }
            }
            if (candidate instanceof Aggregate) {
                Resource result = null;
                for (final Resource child : ((Aggregate) candidate).components()) {
                    final Resource match = findResource(identifier, child, visited);
                    if (match != null) {
                        if (result == null) {
                            result = match;
                        } else {
                            throw new IllegalNameException(Resources.forLocale(getLocale())
                                    .getString(Resources.Keys.ResourceIdentifierCollision_2, getDisplayName(), identifier));
                        }
                    }
                }
                return result;
            }
        }
        return null;
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
