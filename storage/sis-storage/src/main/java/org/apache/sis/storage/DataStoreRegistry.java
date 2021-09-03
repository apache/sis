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

import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.Iterator;
import java.util.ServiceLoader;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.internal.storage.StoreMetadata;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.referencing.LazySet;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;


/**
 * Creates {@link DataStore} instances for a given storage object by scanning all providers on the classpath.
 * Storage objects are typically {@link java.io.File} or {@link javax.sql.DataSource} instances, but can also
 * be any other objects documented in the {@link StorageConnector} class.
 *
 * <div class="note"><b>API note:</b>
 * this class is package-private for now in order to get more experience about what could be a good API.
 * This class may become public in a future SIS version.</div>
 *
 * <h2>Thread safety</h2>
 * The same {@code DataStoreRegistry} instance can be safely used by many threads without synchronization
 * on the part of the caller.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.4
 * @module
 */
final class DataStoreRegistry {
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
        loader = DefaultFactories.createServiceLoader(DataStoreProvider.class);
    }

    /**
     * Creates a new registry which will look for data stores accessible to the given class loader.
     *
     * @param  loader  the class loader to use for loading {@link DataStoreProvider} implementations.
     */
    public DataStoreRegistry(final ClassLoader loader) {
        ArgumentChecks.ensureNonNull("loader", loader);
        this.loader = ServiceLoader.load(DataStoreProvider.class, loader);
    }

    /**
     * Returns the list of data store providers available at this method invocation time.
     * More providers may be added later if new modules are added on the classpath.
     *
     * @return descriptions of available data stores.
     *
     * @since 0.8
     */
    public Set<DataStoreProvider> providers() {
        synchronized (loader) {
            final Iterator<DataStoreProvider> providers = loader.iterator();
            return new LazySet<>(new Iterator<DataStoreProvider>() {
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
            });
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
        final ProbeProviderPair p = lookup(storage, false);
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
     * @param  storage  the input/output object as a URL, file, image input stream, <i>etc.</i>.
     * @return the object to use for reading geospatial data from the given storage.
     * @throws UnsupportedStorageException if no {@link DataStoreProvider} is found for a given storage object.
     * @throws DataStoreException if an error occurred while opening the storage.
     */
    public DataStore open(final Object storage) throws UnsupportedStorageException, DataStoreException {
        ArgumentChecks.ensureNonNull("storage", storage);
        return lookup(storage, true).store;
    }

    /**
     * The kind of providers to test. The provider are divided in 4 categories depending on whether
     * the file suffix matches the suffix expected by the provider, and whether the provider should
     * be tested last for giving a chance to specialized providers to open the file.
     */
    private enum Category {
        /** The provider can be tested now and the file suffix matches. */
        SUFFIX_MATCH(true, false),

        /** The provider could be tested now but the file suffix does not match. */
        IGNORE_SUFFIX(false, false),

        /** The provider should be tested last, but the file suffix matches. */
        DEFERRED(true, true),

        /** The provider should be tested last because it is too generic. */
        DEFERRED_IGNORE_SUFFIX(false, true);

        /** Whether this category checks if suffix matches. */
        final boolean useSuffix;

        /** Whether this category is for providers to test last. */
        final boolean yieldPriority;

        /** Creates a new enumeration value. */
        private Category(final boolean useSuffix, final boolean yieldPriority) {
            this.useSuffix     = useSuffix;
            this.yieldPriority = yieldPriority;
        }

        /** Returns the categories, optionally ignoring file suffix. */
        static Category[] values(final boolean useSuffix) {
            return useSuffix ? values() : new Category[] {IGNORE_SUFFIX, DEFERRED_IGNORE_SUFFIX};
        }
    }

    /**
     * Implementation of {@link #probeContentType(Object)} and {@link #open(Object)}.
     *
     * @param  storage  the input/output object as a URL, file, image input stream, <i>etc.</i>.
     * @param  open     {@code true} for creating a {@link DataStore}, or {@code false} if not needed.
     * @return the result, or {@code null} if the format is not recognized and {@code open} is {@code false}.
     * @throws UnsupportedStorageException if no {@link DataStoreProvider} is found for a given storage object.
     * @throws DataStoreException if an error occurred while opening the storage.
     *
     * @todo Iterate on {@code ServiceLoader.Provider.type()} on JDK9.
     */
    private ProbeProviderPair lookup(final Object storage, final boolean open) throws DataStoreException {
        StorageConnector connector;                 // Will be reset to `null` if it shall not be closed.
        if (storage instanceof StorageConnector) {
            connector = (StorageConnector) storage;
        } else {
            connector = new StorageConnector(storage);
        }
        /*
         * If we can get a filename extension from the given storage (file, URL, etc.), then we may perform two iterations
         * on the provider list. The first iteration will use only the providers which declare capability to read files of
         * that suffix (Category.SUFFIX_MATCH). Only if no provider has been able to read that file, we will do a second
         * iteration on other providers (Category.IGNORE_SUFFIX). The intent is to avoid DataStoreProvider.probeContent(…)
         * invocations loading large dependencies.
         */
        final String      extension  = connector.getFileExtension();
        final boolean     useSuffix  = !(extension == null || extension.isEmpty());
        final Category[]  categories = Category.values(useSuffix);
        ProbeProviderPair selected   = null;
        final List<ProbeProviderPair> needMoreBytes = new LinkedList<>();
        try {
search:     for (int ci=0; ci < categories.length; ci++) {
                final Category category = categories[ci];
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
                        accept = (ci == 0);         // If no metadata, test only in first iteration.
                    } else {
                        accept = (md.yieldPriority() == category.yieldPriority);
                        if (accept & useSuffix) {
                            accept = ArraysExt.containsIgnoreCase(md.fileSuffixes(), extension) == category.useSuffix;
                        }
                    }
                    if (accept) {
                        final ProbeResult probe = provider.probeContent(connector);
                        if (probe.isSupported()) {
                            /*
                             * Stop at the first provider claiming to be able to read the storage.
                             * Do not iterate over the list of deferred providers (if any).
                             */
                            selected = new ProbeProviderPair(provider, probe);
                            break search;
                        }
                        if (ProbeResult.INSUFFICIENT_BYTES.equals(probe)) {
                            /*
                             * If a provider doesn't have enough bytes for answering the question,
                             * try again after this loop with more bytes in the buffer, unless we
                             * found an other provider.
                             */
                            needMoreBytes.add(new ProbeProviderPair(provider, probe));
                        } else if (ProbeResult.UNDETERMINED.equals(probe)) {
                            /*
                             * If a provider doesn't know whether it can open the given storage,
                             * we will try it only if we find no provider retuning SUPPORTED.
                             * We select the first provider because it is more likely to be the
                             * one for the file extension of the given storage.
                             */
                            if (selected == null) {
                                selected = new ProbeProviderPair(provider, probe);
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
                 * We do that by changing moving to the next `Category`.
                 */
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
            @SuppressWarnings("null")
            final String name = connector.getStorageName();
            throw new UnsupportedStorageException(null, Resources.Keys.UnknownFormatFor_1, name);
        }
        return selected;
    }
}
