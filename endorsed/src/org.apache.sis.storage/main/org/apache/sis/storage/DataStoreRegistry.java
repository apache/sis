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

import java.util.LinkedList;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.function.Predicate;
import java.nio.file.StandardOpenOption;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.io.stream.InternalOptionKey;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.system.Reflect;
import org.apache.sis.system.Modules;
import org.apache.sis.system.SystemListener;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.StoreMetadata;
import org.apache.sis.referencing.privy.LazySet;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.privy.Strings;


/**
 * Creates {@link DataStore} instances for a given storage object by scanning all providers on the module path.
 * Storage objects are typically {@link java.io.File} or {@link javax.sql.DataSource} instances,
 * but can also be any other objects documented in the {@link StorageConnector} class.
 *
 * <h2>Thread safety</h2>
 * The same {@code DataStoreRegistry} instance can be safely used by many threads without synchronization
 * on the part of the caller.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class DataStoreRegistry extends LazySet<DataStoreProvider> {
    /**
     * The unique instance of this registry.
     */
    static final DataStoreRegistry INSTANCE = new DataStoreRegistry();
    static {
        SystemListener.add(new SystemListener(Modules.STORAGE) {
            @Override protected void classpathChanged() {
                INSTANCE.reload();
            }
        });
    }

    /**
     * The loader to use for searching for {@link DataStoreProvider} implementations.
     * Note that {@code ServiceLoader} are not thread-safe - usage of this field must
     * be protected in a synchronized block.
     */
    private final ServiceLoader<DataStoreProvider> loader;

    /**
     * Creates a new registry which will look for data stores accessible to the default class loader.
     * The default is the current thread {@linkplain Thread#getContextClassLoader() context class loader},
     * provided that it can access at least the Apache SIS stores.
     */
    public DataStoreRegistry() {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        ServiceLoader<DataStoreProvider> loader;
        try {
            loader = ServiceLoader.load(DataStoreProvider.class, Reflect.getContextClassLoader());
        } catch (SecurityException e) {
            Reflect.log(DataStoreRegistry.class, "<init>", e);
            loader = ServiceLoader.load(DataStoreProvider.class);
        }
        this.loader = loader;
    }

    /**
     * Creates the iterator over the data stores.
     */
    @Override
    protected Iterator<DataStoreProvider> createSourceIterator() {
        synchronized (loader) {
            final Iterator<DataStoreProvider> providers = loader.iterator();
            return new Iterator<DataStoreProvider>() {
                @Override public boolean hasNext() {
                    synchronized (loader) {
                        return providers.hasNext();
                    }
                }

                @Override public DataStoreProvider next() {
                    synchronized (loader) {
                        return providers.next();
                    }
                }
            };
        }
    }

    /**
     * Returns the MIME type of the storage file format, or {@code null} if unknown or not applicable.
     *
     * @param  storage  the input/output object as a URL, file, image input stream, <i>etc.</i>.
     * @return the storage MIME type, or {@code null} if unknown or not applicable.
     * @throws DataStoreException if an error occurred while opening the storage.
     */
    public String probeContentType(final Object storage) throws DataStoreException {
        ArgumentChecks.ensureNonNull("storage", storage);
        final ProbeProviderPair p = lookup(storage, Capability.READ, null, false);
        return (p != null) ? p.probe.getMimeType() : null;
    }

    /**
     * Creates a {@link DataStore} for reading the given storage.
     * The {@code storage} argument can be any of the following types:
     *
     * <ul>
     *   <li>A {@link java.nio.file.Path} or a {@link java.io.File} for a file or a directory.</li>
     *   <li>A {@link java.net.URI} or a {@link java.net.URL} to a distant resource.</li>
     *   <li>A {@link java.lang.CharSequence} interpreted as a filename or a URL.</li>
     *   <li>A {@link java.nio.channels.Channel}, {@link java.io.DataInput}, {@link java.io.InputStream} or {@link java.io.Reader}.</li>
     *   <li>A {@link javax.sql.DataSource} or a {@link java.sql.Connection} to a JDBC database.</li>
     *   <li>Any other {@code DataStore}-specific object, for example {@link ucar.nc2.NetcdfFile}.</li>
     *   <li>An existing {@link StorageConnector} instance.</li>
     * </ul>
     *
     * @param  storage     the input/output object as a URL, file, image input stream, <i>etc.</i>.
     * @param  capability  the capability that the data store must have (read, write, create).
     * @param  preferred   a filter for selecting the providers to try first, or {@code null}.
     * @return the object to use for reading geospatial data from the given storage.
     * @throws UnsupportedStorageException if no {@link DataStoreProvider} is found for a given storage object.
     * @throws DataStoreException if an error occurred while opening the storage.
     */
    public DataStore open(Object storage, Capability capability, Predicate<DataStoreProvider> preferred)
            throws UnsupportedStorageException, DataStoreException
    {
        ArgumentChecks.ensureNonNull("storage", storage);
        return lookup(storage, capability, preferred, true).store;
    }

    /**
     * The kind of providers to test. The provider are divided in 5 categories depending on whether
     * the file suffix matches the suffix expected by the provider, and whether the provider should
     * be tested last for giving a chance to specialized providers to open the file.
     */
    private enum Category {
        /** Providers selected using preference filter and file suffix. */
        PREFERRED(true, true, false),

        /** Providers selected using the preference filter only. */
        PREFERRED_IGNORE_SUFFIX(true, false, false),

        /** Non-deferred providers selected using file suffix. */
        SUFFIX_MATCH(false, true, false),

        /** All others non-deferred providers. */
        IGNORE_SUFFIX(false, false, false),

        /** Providers to be tested last, filtered by file suffix. */
        DEFERRED(false, true, true),

        /** Providers tested last because too generic. */
        DEFERRED_IGNORE_SUFFIX(false, false, true);

        /** Whether this category uses the preference filter. */
        final boolean preferred;

        /** Whether this category checks if the suffix matches. */
        final boolean useSuffix;

        /** Whether this category is for providers to test in last resort. */
        final boolean yieldPriority;

        /** Creates a new enumeration value. */
        private Category(final boolean preferred, final boolean useSuffix, final boolean yieldPriority) {
            this.preferred     = preferred;
            this.useSuffix     = useSuffix;
            this.yieldPriority = yieldPriority;
        }
    }

    /**
     * Implementation of {@link #probeContentType(Object)} and {@link #open(Object, Capability, Function)}.
     *
     * @param  storage     the input/output object as a URL, file, image input stream, <i>etc.</i>.
     * @param  capability  the capability that the data store must have (read, write, create).
     * @param  preferred   a filter for selecting the providers to try first, or {@code null}.
     * @param  open        {@code true} for creating a {@link DataStore}, or {@code false} if not needed.
     * @return the result, or {@code null} if the format is not recognized and {@code open} is {@code false}.
     * @throws UnsupportedStorageException if no {@link DataStoreProvider} is found for a given storage object.
     * @throws DataStoreException if an error occurred while opening the storage.
     *
     * @todo Iterate on {@code ServiceLoader.Provider.type()} on JDK9.
     *       However the use of {@code Stream} is not convenience because of the need to synchronize.
     *       Ideally, we would want the {@code Iterator} that {@code ServiceLoader} is creating anyway.
     */
    private ProbeProviderPair lookup(final Object storage, final Capability capability,
            Predicate<DataStoreProvider> preferred, final boolean open)
            throws DataStoreException
    {
        final boolean writable;
        StorageConnector connector;                 // Will be reset to `null` if it shall not be closed.
        if (storage instanceof StorageConnector) {
            connector = (StorageConnector) storage;
            writable = (capability == Capability.WRITE) && connector.getOption(OptionKey.OPEN_OPTIONS) == null;
            final var filter = connector.getOption(InternalOptionKey.PREFERRED_PROVIDERS);
            if (filter != null) {
                preferred = (preferred != null) ? preferred.and(filter) : filter;
            }
        } else {
            connector = new StorageConnector(storage);
            writable = (capability == Capability.WRITE);
        }
        /*
         * If this method is invoked by `DataStores.openWritable(…)`, add NIO open options if not already present.
         * Note that this code may modify a user provided storage connector. It should be okay considering that
         * each `StorageConnector` instance should be short-lived and used only once.
         */
        if (writable) {
            connector.setOption(OptionKey.OPEN_OPTIONS, new StandardOpenOption[] {
                StandardOpenOption.CREATE, StandardOpenOption.WRITE
            });
        }
        if (preferred != null) {
            connector.setOption(InternalOptionKey.PREFERRED_PROVIDERS, preferred);
        }
        /*
         * If we can get a filename extension from the given storage (file, URL, etc.), we may perform two times
         * more iterations on the provider list. One serie of iterations will use only the providers that declare
         * capability to read or write files of that suffix (Category.SUFFIX_MATCH). Only if no provider has been
         * able to read or write that file, we will do another iteration on other providers (Category.IGNORE_SUFFIX).
         * The intent is to avoid DataStoreProvider.probeContent(…) invocations loading large dependencies.
         */
        final String      extension   = connector.getFileExtension();
        final boolean     useSuffix   = !Strings.isNullOrEmpty(extension);
        final boolean     isWriteOnly = (capability == Capability.WRITE) && IOUtilities.isWriteOnly(connector.getStorage());
        ProbeProviderPair selected    = null;
        final var needMoreBytes = new LinkedList<ProbeProviderPair>();
        try {
            boolean isFirstIteration = true;
search:     for (final Category category : Category.values()) {
                if (category.preferred && (preferred == null)) continue;
                if (category.useSuffix && !useSuffix) continue;
                /*
                 * All usages of `loader` and its `providers` iterator must be protected in a synchronized block,
                 * because ServiceLoader is not thread-safe. We try to keep the synhronization block as small as
                 * possible for less contention. In particular, the `probeContent(connector)` method call may be
                 * costly.
                 */
                final Iterator<DataStoreProvider> providers;
                DataStoreProvider provider;
                synchronized (loader) {
                    providers = loader.iterator();
                    provider = providers.hasNext() ? providers.next() : null;
                }
                while (provider != null) {
                    /*
                     * Check if the provider should be tested in current iteration.
                     * The criteria for testing a provider is determined by comparing
                     * provider metadata with the category tested in current iteration.
                     */
                    boolean accept;
                    final StoreMetadata md = provider.getClass().getAnnotation(StoreMetadata.class);
                    if (md == null) {
                        accept = isFirstIteration;      // If no metadata, test only during one iteration.
                    } else {
                        accept = (category.preferred || md.yieldPriority() == category.yieldPriority) &&
                                 ArraysExt.contains(md.capabilities(), capability);
                        if (accept & useSuffix) {
                            accept = ArraysExt.containsIgnoreCase(md.fileSuffixes(), extension) == category.useSuffix;
                        }
                    }
                    if (accept & (preferred != null)) {
                        accept = (preferred.test(provider) == category.preferred);
                    }
                    /*
                     * At this point, it has been determined whether the provider should be tested in current iteration.
                     * If accepted, perform now the probing operation for checking if the current provider is suitable.
                     * The `connector.probing` field is set to a non-null value for telling `StorageConnector` to not
                     * create empty file if the file does not exist (it has no effect in read-only mode).
                     */
                    if (accept) {
                        final var candidate = new ProbeProviderPair(provider);
                        if (isWriteOnly) {
                            /*
                             * We cannot probe a write-only storage. Rely on the filtering done before this block,
                             * which was based on format name and file suffix, and use the first filtered provider.
                             */
                            selected = candidate;
                            break search;
                        }
                        final ProbeProviderPair old = connector.probing;
                        final ProbeResult probe;
                        try {
                            connector.probing = candidate;
                            probe = provider.probeContent(connector);
                        } finally {
                            connector.probing = old;
                        }
                        candidate.probe = probe;
                        if (probe.isSupported()) {
                            /*
                             * Stop at the first provider claiming to be able to read the storage.
                             * Do not iterate over the list of deferred providers (if any).
                             */
                            selected = candidate;
                            break search;
                        }
                        if (ProbeResult.INSUFFICIENT_BYTES.equals(probe)) {
                            /*
                             * If a provider doesn't have enough bytes for answering the question,
                             * try again after this loop with more bytes in the buffer, unless we
                             * found another provider.
                             */
                            needMoreBytes.add(candidate);
                        } else if (ProbeResult.UNDETERMINED.equals(probe)) {
                            /*
                             * If a provider doesn't know whether it can open the given storage,
                             * we will try it only if we find no provider retuning SUPPORTED.
                             * We select the first provider because it is more likely to be the
                             * one for the file extension of the given storage.
                             */
                            if (selected == null) {
                                selected = candidate;
                            }
                        }
                    }
                    synchronized (loader) {
                        provider = providers.hasNext() ? providers.next() : null;
                    }
                }
                /*
                 * If any provider did not had enough bytes for answering the `probeContent(…)` question,
                 * get more bytes and try again. We try to prefetch more bytes only if we have no choice
                 * in order to avoid latency on network connection.
                 */
                while (!needMoreBytes.isEmpty() && connector.prefetch()) {
                    for (final Iterator<ProbeProviderPair> it = needMoreBytes.iterator(); it.hasNext();) {
                        final ProbeProviderPair p = it.next();
                        p.probe = p.provider.probeContent(connector);
                        if (p.probe.isSupported()) {
                            selected = p;
                            break search;
                        }
                        if (!ProbeResult.INSUFFICIENT_BYTES.equals(p.probe)) {
                            if (selected == null && ProbeResult.UNDETERMINED.equals(p.probe)) {
                                selected = p;                   // To be used only if we don't find a better match.
                            }
                            it.remove();        // UNSUPPORTED_* or UNDETERMINED: do not try again those providers.
                        }
                    }
                }
                /*
                 * If we filtered providers by the file extension without finding a suitable provider,
                 * try again with all other providers (even if they are for another file extension).
                 * We do that by moving to the next `Category`.
                 */
                isFirstIteration = false;
            }
            /*
             * If a provider has been found, or if a provider returned UNDETERMINED, use that one
             * for opening a DataStore. Note that if more than one provider returned UNDETERMINED,
             * the selected one is arbitrary and may change in different execution. Implementers
             * shall avoid the UNDETERMINED value as much as possible (this value should be used
             * only for RAW image format).
             */
            if (open && selected != null) {
                selected.store = selected.provider.open(connector);
                connector = null;                                               // For preventing it to be closed.
            }
        } finally {
            if (connector != null && connector != storage) {
                connector.closeAllExcept(null);
            }
        }
        if (open && selected == null) {
            @SuppressWarnings("null")                       // `connector` is null only if `selected` is non-null.
            final String name = connector.getStorageName();
            throw new UnsupportedStorageException(null, Resources.Keys.UnknownFormatFor_1, name);
        }
        return selected;
    }

    /**
     * Notifies this registry that it should re-fetch the elements from the source.
     */
    @Override
    public synchronized void reload() {
        loader.reload();
        super.reload();
    }
}
