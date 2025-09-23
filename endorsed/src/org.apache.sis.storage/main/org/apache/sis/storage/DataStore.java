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
import java.util.Objects;
import java.util.Optional;
import org.opengis.util.ScopedName;
import org.opengis.util.GenericName;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.Identification;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.util.Localized;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.storage.base.StoreUtilities;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.storage.event.CloseEvent;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.storage.event.StoreListeners;


/**
 * Manages a series of features, coverages or sensor data.
 * Different {@code DataStore} subclasses exist for different formats (netCDF, GeoTIFF, <i>etc.</i>).
 * The supported format can be identified by the {@linkplain #getProvider() provider}.
 *
 * <p>Each data store is itself a {@link Resource}. The data store subclasses should implement
 * a more specialized {@code Resource} interface depending on the format characteristics.
 * For example, a {@code DataStore} for ShapeFiles will implement the {@link FeatureSet} interface,
 * while a {@code DataStore} for netCDF files will implement the {@link Aggregate} interface.</p>
 *
 * <h2>Thread safety policy</h2>
 * Data stores should be thread-safe, but their synchronization lock is implementation-dependent.
 * This base class uses only the {@code synchronized} keyword, applied on the following methods:
 *
 * <ul>
 *   <li>{@link #getLocale()}</li>
 *   <li>{@link #setLocale(Locale)}</li>
 * </ul>
 *
 * Since above properties are used only for information purpose, concurrent modifications during a read or write
 * operation should be harmless. Consequently, subclasses are free use their own synchronization mechanism instead
 * than {@code synchronized(this)} lock.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 *
 * @see DataStores#open(Object)
 *
 * @since 0.3
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
    private final String displayName;

    /**
     * The locale to use for formatting warnings.
     * This is not the locale for formatting data in the storage.
     *
     * @see #getLocale()
     * @see #setLocale(Locale)
     */
    private Locale locale;

    /**
     * The set of registered {@link StoreListener}s for this data store.
     */
    protected final StoreListeners listeners;

    /**
     * Creates a new instance with no provider and initially no listener.
     */
    @SuppressWarnings("this-escape")    // `this` appears in a cyclic graph.
    protected DataStore() {
        provider    = null;
        displayName = null;
        locale      = Locale.getDefault(Locale.Category.DISPLAY);
        listeners   = new StoreListeners(null, this);
    }

    /**
     * Creates a new instance for the given storage (typically file or database).
     * The {@code provider} argument is an optional but recommended information.
     * The {@code connector} argument is mandatory.
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, reader instance, <i>etc</i>).
     * @throws DataStoreException if an error occurred while creating the data store for the given storage.
     *
     * @since 0.8
     */
    @SuppressWarnings("this-escape")    // `this` appears in a cyclic graph. Should not be accessible before completion.
    protected DataStore(final DataStoreProvider provider, final StorageConnector connector) throws DataStoreException {
        this.provider    = provider;
        this.displayName = connector.getStorageName();
        this.locale      = Locale.getDefault(Locale.Category.DISPLAY);
        this.listeners   = new StoreListeners(connector.getOption(DataOptionKey.PARENT_LISTENERS), this);
        /*
         * Above locale is NOT OptionKey.LOCALE because we are not talking about the same locale.
         * The one in this DataStore is for warning and exception messages, not for parsing data.
         */
    }

    /**
     * Creates a new instance as a child of another data store instance.
     * Events will be sent not only to {@linkplain #addListener registered listeners} of this {@code DataStore},
     * but also to listeners of the {@code parent} data store. Each listener will be notified only once, even if
     * the same listener is registered in the two places.
     *
     * @param  parent     the parent that contains this new {@code DataStore} component, or {@code null} if none.
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, reader instance, <i>etc</i>).
     * @param  hidden     {@code true} if this store will not be directly accessible from the parent.
     *                    It is the case if this store is an {@link Aggregate} and the parent store will
     *                    expose only some components of the aggregate instead of the aggregate itself.
     * @throws DataStoreException if an error occurred while creating the data store for the given storage.
     *
     * @since 1.1
     */
    @SuppressWarnings("this-escape")    // `this` appears in a cyclic graph. Should not be accessible before completion.
    protected DataStore(final DataStore parent, final DataStoreProvider provider, final StorageConnector connector,
                        final boolean hidden) throws DataStoreException
    {
        this.provider = provider;
        displayName = connector.getStorageName();
        final StoreListeners forwardTo;
        if (parent != null) {
            locale = parent.locale;
            if (hidden) {
                listeners = parent.listeners;
                return;
            }
            forwardTo = parent.listeners;
        } else {
            locale    = Locale.getDefault(Locale.Category.DISPLAY);
            forwardTo = null;
        }
        listeners = new StoreListeners(forwardTo, this);
    }

    /**
     * Returns the factory that created this {@code DataStore} instance.
     * The provider gives additional information on this {@code DataStore} such as a format description
     * and a list of parameters that can be used for opening data stores of the same class.
     *
     * <p>The return value should never be null if this {@code DataStore} has been created by
     * {@link DataStores#open(Object)} or by a {@link DataStoreProvider} {@code open(…)} method.
     * However, it may be null if this object has been instantiated by a direct call to its constructor.</p>
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
     * <p>In some cases, for stores reading in-memory data or other inputs that cannot fit with
     * {@code ParameterDescriptorGroup} requirements (for example an {@link java.io.InputStream}
     * connected to unknown or no {@link java.net.URL}), this method may return an empty value.</p>
     *
     * @return parameters used for opening this {@code DataStore}.
     *
     * @see DataStoreProvider#getOpenParameters()
     *
     * @since 0.8
     */
    public abstract Optional<ParameterValueGroup> getOpenParameters();

    /**
     * Sets the locale to use for formatting warnings and other messages.
     * In a client-server architecture, it should be the locale on the <em>client</em> side.
     *
     * <p>This locale is used on a <em>best effort</em> basis; whether messages will honor this locale or not
     * depends on the code that logged warnings or threw exceptions. In Apache SIS implementation, this locale has
     * better chances to be honored by the {@link DataStoreException#getLocalizedMessage()} method rather than
     * {@code getMessage()}. See {@code getLocalizedMessage()} javadoc for more information.</p>
     *
     * @param locale  the new locale to use.
     *
     * @see DataStoreException#getLocalizedMessage()
     */
    public synchronized void setLocale(final Locale locale) {
        this.locale = Objects.requireNonNull(locale);
    }
    // See class javadoc for a note on synchronization.

    /**
     * The locale to use for formatting warnings and other messages. This locale is for user interfaces
     * only – it has no effect on the data to be read or written from/to the data store.
     *
     * <p>The default value is the {@linkplain Locale#getDefault() system default locale}.</p>
     *
     * @see org.apache.sis.storage.event.StoreEvent#getLocale()
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
     * messages, in which case throwing an exception here would hide the original exception.</p>
     *
     * <p>This method differs from {@link #getIdentifier()} in that it is typically a file name
     * known at construction time instead of a property read from metadata.
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
        return displayName;
    }

    /**
     * Returns an identifier for the root resource of this data store, or an empty value if none.
     * If this data store contains many resources (as in an {@link Aggregate}),
     * the returned identifier shall be different than the identifiers of those child resources.
     * In other words, the following equality shall hold without ambiguity:
     *
     * {@snippet lang="java" :
     *     findResource(getIdentifier().toString()) == this
     *     }
     *
     * Note that this identifier is not guaranteed to be unique between different {@code DataStore} instances;
     * it only needs to be unique among the resources provided by this data store instance.
     *
     * <h4>Default implementation</h4>
     * <p>The default implementation searches for an identifier in the metadata,
     * at the location shown below, provided that conditions are met:</p>
     *
     * <blockquote>
     * <p><b>Path:</b> {@link Resource#getMetadata() metadata} /
     * {@link org.apache.sis.metadata.iso.DefaultMetadata#getIdentificationInfo() identificationInfo} /
     * {@link org.apache.sis.metadata.iso.identification.AbstractIdentification#getCitation() citation} /
     * {@link org.apache.sis.metadata.iso.citation.DefaultCitation#getIdentifiers() identifier}</p>
     *
     * <p><b>Condition:</b> in default implementation, the identifier is presents only if exactly one
     * {@code citation} is found at above path. If two or more {@code citation} instances are found,
     * the identification is considered ambiguous and an empty value is returned.</p>
     *
     * <p><b>Selection:</b> the first identifier implementing the {@code GenericName} interface is returned.
     * If there is no such identifier, then a {@link org.apache.sis.referencing.NamedIdentifier} is created
     * from the first identifier. If there is no identifier at all, then an empty value is returned.</p>
     * </blockquote>
     *
     * Subclasses are encouraged to override this method with more efficient implementations.
     *
     * @return an identifier for the root resource of this data store.
     * @throws DataStoreException if an error occurred while fetching the identifier.
     *
     * @see #getMetadata()
     * @see #getDisplayName()
     *
     * @since 1.0
     */
    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        final Metadata metadata = getMetadata();
        if (metadata != null) {
            Citation citation = null;
            for (final Identification id : metadata.getIdentificationInfo()) {
                final Citation c = id.getCitation();
                if (c != null) {
                    if (citation != null && citation != c) {
                        return Optional.empty();                    // Ambiguity.
                    }
                    citation = c;
                }
            }
            if (citation != null) {
                Identifier first = null;
                for (final Identifier c : citation.getIdentifiers()) {
                    if (c instanceof GenericName) {
                        return Optional.of((GenericName) c);
                    } else if (first == null) {
                        first = c;
                    }
                }
                if (first != null) {
                    return Optional.of(new NamedIdentifier(first));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns information about the data store as a whole. The returned metadata object can contain
     * information such as the spatiotemporal extent of all contained {@linkplain Resource resources},
     * contact information about the creator or distributor, data quality, update frequency, usage constraints,
     * file format and more.
     *
     * @return information about resources in the data store. Should not be {@code null}.
     * @throws DataStoreException if an error occurred while reading the metadata.
     *
     * @see #getIdentifier()
     * @see org.apache.sis.metadata.iso.DefaultMetadata#deepCopy(Metadata)
     */
    @Override
    public abstract Metadata getMetadata() throws DataStoreException;

    /**
     * Returns implementation-specific metadata. The structure of those metadata varies for each file format.
     * The {@linkplain #getMetadata() standard metadata} should be preferred since they allow abstraction of
     * format details, but those native metadata are sometimes useful when an information is not provided by
     * the standard metadata.
     *
     * <p>The tree table should contain at least the following columns:</p>
     * <ul>
     *   <li>{@link TableColumn#NAME}  — a name for the metadata property, e.g. "Title".</li>
     *   <li>{@link TableColumn#VALUE} — the property value typically as a string, number or date.</li>
     * </ul>
     *
     * The {@link TableColumn#NAME} of the root node should be a format name such as "NetCDF" or "GeoTIFF".
     * That name should be short since it may be used in widget as a designation of implementation-specific
     * details.
     *
     * @return resources information structured in an implementation-specific way.
     * @throws DataStoreException if an error occurred while reading the metadata.
     *
     * @since 1.1
     */
    public Optional<TreeTable> getNativeMetadata() throws DataStoreException {
        return Optional.empty();
    }

    /**
     * Searches for a resource identified by the given identifier. The given identifier should be the string
     * representation of the return value of {@link Resource#getIdentifier()} on the desired resource.
     * Implementation may also accept aliases for convenience. For example if the full name of a resource
     * is {@code "foo:bar"}, then this method may accept {@code "bar"} as a synonymous of {@code "foo:bar"}
     * provided that it is unambiguous.
     *
     * <p>The default implementation verifies if above criterion apply to this {@code DataStore}
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
            GenericName name = candidate.getIdentifier().orElse(null);
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
     * Registers a listener to notify when the specified kind of event occurs in this data store or in a resource.
     * The data store will call the {@link StoreListener#eventOccured(StoreEvent)} method when new events matching
     * the {@code eventType} occur. An event may be a change in data store content or structure, or a warning that
     * occurred during a read or write operation.
     *
     * <p>Registering a listener for a given {@code eventType} also register the listener for all event sub-types.
     * The same listener can be registered many times, but its {@link StoreListener#eventOccured(StoreEvent)}
     * method will be invoked only once per event. This filtering applies even if the listener is registered
     * on individual resources of this data store.</p>
     *
     * <p>If this data store may produce events of the given type, then the given listener is kept by strong reference;
     * it will not be garbage collected unless {@linkplain #removeListener(Class, StoreListener) explicitly removed}
     * or unless this {@code DataStore} is itself garbage collected. However if the given type of events can never
     * happen with this data store, then this method is not required to keep a reference to the given listener.</p>
     *
     * <h4>Warning events</h4>
     * If {@code eventType} is assignable from <code>{@linkplain org.apache.sis.storage.event.WarningEvent}.class</code>,
     * then registering that listener turns off logging of warning messages for this data store.
     * This side-effect is applied on the assumption that the registered listener will handle
     * warnings in its own way, for example by showing warnings in a widget.
     *
     * @param  <T>        compile-time value of the {@code eventType} argument.
     * @param  eventType  type of {@link StoreEvent} to listen (cannot be {@code null}).
     * @param  listener   listener to notify about events.
     *
     * @since 1.0
     */
    @Override
    public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
        listeners.addListener(eventType, listener);
    }

    /**
     * Unregisters a listener previously added to this data store for the given type of events.
     * The {@code eventType} must be the exact same class as the one given to the {@code addListener(…)} method;
     * this method does not remove listeners registered for subclasses and does not remove listeners registered in
     * children resources.
     *
     * <p>If the same listener has been registered many times for the same even type, then this method removes only
     * the most recent registration. In other words if {@code addListener(type, ls)} has been invoked twice, then
     * {@code removeListener(type, ls)} needs to be invoked twice in order to remove all instances of that listener.
     * If the given listener is not found, then this method does nothing (no exception is thrown).</p>
     *
     * <h4>Warning events</h4>
     * If {@code eventType} is <code>{@linkplain org.apache.sis.storage.event.WarningEvent}.class</code>
     * and if, after this method invocation, there are no remaining listeners for warning events,
     * then this {@code DataStore} will send future warnings to the loggers.
     *
     * @param  <T>        compile-time value of the {@code eventType} argument.
     * @param  eventType  type of {@link StoreEvent} which were listened (cannot be {@code null}).
     * @param  listener   listener to stop notifying about events.
     *
     * @since 1.0
     */
    @Override
    public <T extends StoreEvent> void removeListener(Class<T> eventType, StoreListener<? super T> listener) {
        listeners.removeListener(eventType, listener);
    }

    /**
     * Closes this data store and releases any underlying resources.
     * A {@link CloseEvent} is sent to listeners before the data store is closed.
     *
     * <p>If this method is invoked asynchronously while a read operation is in progress in another thread,
     * then the behavior is implementation dependent. Some implementations will interrupt the read process,
     * for example with an {@link java.nio.channels.AsynchronousCloseException}. This is useful if the data
     * store was downloading a large file from a network connection.</p>
     *
     * <h4>Note for implementers</h4>
     * Implementations should invoke {@code listeners.close()} on their first line
     * for sending notification to all listeners before the data store is actually
     * closed.
     *
     * @throws DataStoreException if an error occurred while closing this data store.
     *
     * @see StoreListeners#close()
     */
    @Override
    public abstract void close() throws DataStoreException;

    /*
     * Data stores should not override `Object.equals(Object)` and `hashCode()` methods,
     * because comparisons other than identity comparisons may confuse cache mechanisms
     * (e.g. caches may think that a data store has already been closed).
     */

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
