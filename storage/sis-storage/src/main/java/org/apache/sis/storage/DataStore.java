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
import org.opengis.util.ScopedName;
import org.opengis.util.GenericName;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.Identification;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.util.Localized;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.WarningListener;
import org.apache.sis.util.logging.WarningListeners;
import org.apache.sis.internal.storage.StoreUtilities;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.internal.util.Strings;
import org.apache.sis.referencing.NamedIdentifier;

// Branch-specific imports
import org.opengis.referencing.ReferenceIdentifier;


/**
 * Manages a series of features, coverages or sensor data.
 * Different {@code DataStore} subclasses exist for different formats (netCDF, GeoTIFF, <i>etc.</i>).
 * The supported format can be identified by the {@linkplain #getProvider() provider}.
 *
 * <p>Each data store is itself a {@link Resource}. The data store subclasses should implement
 * a more specialized {@code Resource} interface depending on the format characteristics.
 * For example a {@code DataStore} for ShapeFiles will implement the {@link FeatureSet} interface,
 * while a {@code DataStore} for netCDF files will implement the {@link Aggregate} interface.</p>
 *
 * <div class="section">Thread safety policy</div>
 * Data stores should be thread-safe, but their synchronization lock is implementation-dependent.
 * This base class uses only the {@code synchronized} keyword, applied on the following methods:
 *
 * <ul>
 *   <li>{@link #getLocale()}</li>
 *   <li>{@link #setLocale(Locale)}</li>
 * </ul>
 *
 * Since above properties are used only for information purpose, concurrent modifications during a read or write
 * operation should be harmless. Consequently subclasses are free use their own synchronization mechanism instead
 * than {@code synchronized(this)} lock.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see DataStores#open(Object)
 *
 * @since 0.3
 * @module
 */
public abstract class DataStore implements Resource, Localized, AutoCloseable {
    /**
     * The factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * This information can be useful for fetching information common to all {@code DataStore}
     * instances of the same class.
     *
     * @since 0.8
     *
     * @see #getProvider()
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
     * Creates a new instance as a child of another data store instance.
     * The new instance inherits the parent {@linkplain #getProvider() provider}.
     * The parent and the child share the same listeners: adding or removing a listener to a parent
     * adds or removes the same listeners to all children, and conversely.
     *
     * @param  parent     the parent data store, or {@code null} if none.
     * @param  connector  information about the storage (URL, stream, reader instance, <i>etc</i>).
     * @throws DataStoreException if an error occurred while creating the data store for the given storage.
     *
     * @since 0.8
     */
    protected DataStore(final DataStore parent, final StorageConnector connector) throws DataStoreException {
        ArgumentChecks.ensureNonNull("connector", connector);
        if (parent != null) {
            provider  = parent.provider;
            locale    = parent.locale;
            listeners = parent.listeners;
        } else {
            provider  = null;
            locale    = Locale.getDefault(Locale.Category.DISPLAY);
            listeners = new WarningListeners<>(this);
        }
        name = connector.getStorageName();
    }

    /**
     * Returns the factory that created this {@code DataStore} instance.
     * The provider gives additional information on this {@code DataStore} such as a format description
     * and a list of parameters that can be used for opening data stores of the same class.
     *
     * @return the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     *
     * @see #provider
     *
     * @since 0.8
     */
    public DataStoreProvider getProvider() {
        return provider;
    }

    /**
     * Returns the parameters used to open this data store.
     * The collection of legal parameters is implementation-dependent
     * ({@linkplain org.apache.sis.parameter.DefaultParameterValue#getDescriptor() their description}
     * is given by {@link DataStoreProvider#getOpenParameters()}),
     * but should contain at least a parameter named {@value org.apache.sis.storage.DataStoreProvider#LOCATION}
     * with a {@link java.net.URI}, {@link java.nio.file.Path} or {@link javax.sql.DataSource} value.
     *
     * <p>In the event a data store must be closed and reopened later, those parameters can be stored in a file or
     * database and used for {@linkplain DataStoreProvider#open(ParameterValueGroup) creating a new store} later.</p>
     *
     * <p>In some cases, for stores reading in-memory data or other inputs that can not fit with
     * {@code ParameterDescriptorGroup} requirements (for example an {@link java.io.InputStream}
     * connected to unknown or no {@link java.net.URL}), this method may return null.</p>
     *
     * @return parameters used for opening this {@code DataStore}, or {@code null} if not available.
     *
     * @see DataStoreProvider#getOpenParameters()
     *
     * @since 0.8
     */
    public abstract ParameterValueGroup getOpenParameters();

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
    // See class javadoc for a note on synchronization.

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
     * <p>This method differs from {@link #getIdentifier()} in that it is typically a file name
     * known at construction time instead than a property read from metadata.
     * Default implementation returns the {@link StorageConnector#getStorageName()} value,
     * or {@code null} if this data store has been created by the no-argument constructor.
     * Subclasses should override this method if they can provide a better name.</p>
     *
     * @return a short name of label for this data store, or {@code null} if unknown.
     *
     * @see #getIdentifier()
     * @see #getLocale()
     *
     * @since 0.8
     */
    public String getDisplayName() {
        return name;
    }

    /**
     * Returns an identifier for the root resource of this data store, or {@code null} if none.
     * If this data store contains many resources (as in an {@link Aggregate}),
     * the returned identifier shall be different than the identifiers of those child resources.
     * In other words, the following equality shall hold without ambiguity:
     *
     * {@preformat java
     *     findResource(getIdentifier().toString()) == this
     * }
     *
     * Note that this identifier is not guaranteed to be unique between different {@code DataStore} instances;
     * it only needs to be unique among the resources provided by this data store instance.
     *
     * <div class="section">Default implementation</div>
     * <p>The default implementation searches for an identifier in the metadata,
     * at the location shown below, provided that conditions are met:</p>
     *
     * <blockquote>
     * <p><b>Path:</b> {@link Resource#getMetadata() metadata} /
     * {@link org.apache.sis.metadata.iso.DefaultMetadata#getIdentificationInfo() identificationInfo} /
     * {@link org.apache.sis.metadata.iso.identification.AbstractIdentification#getCitation() citation} /
     * {@link org.apache.sis.metadata.iso.citation.DefaultCitation#getIdentifiers() identifier}</p>
     *
     * <p><b>Condition:</b> default implementation returns a non-null identifier only if exactly one
     * {@code citation} is found at above path. If two or more {@code citation} instances are found,
     * the identification is considered ambiguous and {@code null} is returned.</p>
     *
     * <p><b>Selection:</b> the first identifier implementing the {@code GenericName} interface is returned.
     * If there is no such identifier, then a {@link org.apache.sis.referencing.NamedIdentifier} is created
     * from the first identifier. If there is no identifier at all, then {@code null} is returned.</p>
     * </blockquote>
     *
     * Subclasses are encouraged to override this method with more efficient implementations.
     *
     * @return an identifier for the root resource of this data store, or {@code null} if none.
     * @throws DataStoreException if an error occurred while fetching the identifier.
     *
     * @see #getMetadata()
     * @see #getDisplayName()
     *
     * @since 1.0
     */
    @Override
    public GenericName getIdentifier() throws DataStoreException {
        final Metadata metadata = getMetadata();
        if (metadata != null) {
            Citation citation = null;
            for (final Identification id : metadata.getIdentificationInfo()) {
                final Citation c = id.getCitation();
                if (c != null) {
                    if (citation != null && citation != c) return null;                 // Ambiguity.
                    citation = c;
                }
            }
            if (citation != null) {
                ReferenceIdentifier first = null;
                for (final Identifier c : citation.getIdentifiers()) {
                    if (c instanceof GenericName) {
                        return (GenericName) c;
                    } else if (first == null && c instanceof ReferenceIdentifier) {
                        first = (ReferenceIdentifier) c;
                    }
                }
                if (first != null) {
                    return new NamedIdentifier(first);
                }
            }
        }
        return null;
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
     * @see #getIdentifier()
     */
    @Override
    public abstract Metadata getMetadata() throws DataStoreException;

    /**
     * Searches for a resource identified by the given identifier. The given identifier should be the string
     * representation of the return value of {@link Resource#getIdentifier()} on the desired resource.
     * Implementation may also accept aliases for convenience. For example if the full name of a resource
     * is {@code "foo:bar"}, then this method may accept {@code "bar"} as a synonymous of {@code "foo:bar"}
     * provided that it does not introduce ambiguity.
     *
     * <p>The default implementation verifies if above criterion matches to this {@code DataStore}
     * (which is itself a resource), then iterates recursively over {@link Aggregate} components
     * if this data store is an aggregate.
     * If a match is found without ambiguity, the associated resource is returned. Otherwise an exception is thrown.
     * Subclasses are encouraged to override this method with a more efficient implementation.</p>
     *
     * @param  identifier  identifier of the resource to fetch. Must be non-null.
     * @return resource associated to the given identifier (never {@code null}).
     * @throws IllegalNameException if no resource is found for the given identifier, or if more than one resource is found.
     * @throws DataStoreException if another kind of error occurred while searching resources.
     *
     * @see Resource#getIdentifier()
     * @see FeatureNaming
     */
    public Resource findResource(final String identifier) throws DataStoreException {
        ArgumentChecks.ensureNonEmpty("identifier", identifier);
        final Resource resource = findResource(identifier, this, new IdentityHashMap<>());
        if (resource != null) {
            return resource;
        }
        throw new IllegalNameException(StoreUtilities.resourceNotFound(this, identifier));
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
            GenericName name = candidate.getIdentifier();
            if (name != null) {
                do if (identifier.equals(name.toString())) {
                    return candidate;
                }
                while ((name instanceof ScopedName) && name != (name = ((ScopedName) name).tail()));
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

    /**
     * Returns a string representation of this data store for debugging purpose.
     * The content of the string returned by this method may change in any future SIS version.
     *
     * @return a string representation of this data store for debugging purpose.
     */
    @Override
    public String toString() {
        return Strings.bracket(getClass(), getDisplayName());
    }
}
