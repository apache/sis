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
package org.apache.sis.storage.gsf;

import java.net.URI;
import java.util.Optional;
import java.util.logging.Logger;
import java.nio.file.Path;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.base.StoreMetadata;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.URIDataStoreProvider;
import org.apache.sis.storage.panama.LibraryStatus;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.system.Cleaners;


/**
 * The provider of {@code GFSStore} instances.
 * Given a {@link StorageConnector} input, this class tries to instantiate a {@link GFSStore}.
 * This data store uses the <abbr>GSF</abbr> native library for loading data.
 *
 * <p>Each {@code GSFStoreProvider} instance is linked to a <abbr>GSF</abbr> library specified
 * at construction time, or to a <abbr>JVM</abbr>-wide shared <abbr>GSF</abbr> library if the
 * no-argument constructor is used. In the non-shared case, <abbr>GSF</abbr> will be unloaded
 * when this {@code GSFStoreProvider} instance is garbage collected.</p>
 *
 * @author  Johann Sorel (Geomatys)
 */
@StoreMetadata(formatName    = GSFStoreProvider.NAME,
               capabilities  = {Capability.READ},
               resourceTypes = {},
               yieldPriority = true)    // For trying Java implementations before GSF.
public class GSFStoreProvider extends DataStoreProvider {
    /**
     * The name of this data store provider.
     */
    static final String NAME = "GSF";

    /**
     * The logger used by <abbr>GSF</abbr> stores.
     *
     * @see #getLogger()
     */
    static final Logger LOGGER = Logger.getLogger("org.apache.sis.storage.gsf");

    /**
     * The parameter descriptor to be returned by {@link #getOpenParameters()}.
     */
    private static final ParameterDescriptorGroup OPEN_DESCRIPTOR;
    static {
        final var builder = new ParameterBuilder();
        OPEN_DESCRIPTOR = builder.addName(NAME).createGroup(URIDataStoreProvider.LOCATION_PARAM);
    }

    /**
     * Handles to <abbr>GSF</abbr> native functions, or {@code null} for global.
     * Can also be reset to {@code null} if <abbr>GSF</abbr> reported a fatal error.
     */
    private GSF nativeFunctions;

    /**
     * The status of {@link #nativeFunctions}, or {@code null} if the global set of functions is used.
     */
    private LibraryStatus status;

    /**
     * Creates a new provider which will load the <abbr>GSF</abbr> library from the default library path.
     */
    public GSFStoreProvider() {
    }

    /**
     * Creates a new provider which will load the <abbr>GSF</abbr> library from the specified file.
     * The library will be unloaded when this provider will be garbage-collected.
     *
     * @throws IllegalArgumentException if the GSF library has not been found.
     * @throws NoSuchElementException if a <abbr>GSF</abbr> function has not been found in the library.
     * @throws IllegalCallerException if this Apache SIS module is not authorized to call native methods.
     */
    @SuppressWarnings("this-escape")
    public GSFStoreProvider(final Path library) {
        Cleaners.SHARED.register(this, nativeFunctions = GSF.load(library));
        status = LibraryStatus.LOADED;
    }

    /**
     * Returns the set of <abbr>GSF</abbr> native functions.
     *
     * @return the set of native functions.
     * @throws DataStoreException if the native library is not available.
     */
    final synchronized GSF GSF() throws DataStoreException {
        if (status == null) {
            return GSF.global();       // Fetch each time (no cache) because may have changed outside this class.
        }
        status.report(null);            // Should never return if `nativeFunctions` is null.
        return nativeFunctions;
    }

    /**
     * Tries to load <abbr>GSF</abbr> if not already done, without throwing an exception in case of error.
     * Instead, the error is logged and {@code null} is returned. This is used for probing.
     *
     * @param  caller  the name of the method which is invoking this method.
     * @return the set of native functions, or {@code null} if not available.
     */
    final synchronized Optional<GSF> tryGSF(final String caller) {
        if (status == null) {
            return GSF.tryGlobal(caller);
        }
        return Optional.ofNullable(nativeFunctions);
    }

    /**
     * Returns a generic name for this data store, used mostly in warnings or error messages.
     *
     * @return a short name or abbreviation for the data store.
     */
    @Override
    public String getShortName() {
        return NAME;
    }

    /**
     * Returns a description of all parameters accepted by this provider for opening a file with GSF.
     *
     * @return description of available parameters for opening a file with <abbr>GSF</abbr>.
     */
    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return OPEN_DESCRIPTOR;
    }

    /**
     * Returns the MIME type if the given storage appears to be supported by this data store.
     * A {@linkplain ProbeResult#isSupported() supported} status does not guarantee that reading
     * or writing will succeed, only that there appears to be a reasonable chance according GSF.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @return a {@linkplain ProbeResult#isSupported() supported} status with the MIME type
     *         if the given storage seems to be readable by {@code GSFStore} instances.
     * @throws DataStoreException if an I/O error occurred.
     */
    @Override
    public ProbeResult probeContent(final StorageConnector connector) throws DataStoreException {
        final URI url = connector.getStorageAs(URI.class);
        if (url != null && url.toString().toLowerCase().endsWith(".gsf")) {
            final GSF gsf = tryGSF("probeContent").orElse(null);
            if (gsf != null) {
                return new ProbeResult(true, "application/gsf", null);
            }
        }
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

    /**
     * Creates a {@code GSFStore} instance associated with this provider.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @return a data store implementation associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public GSFStore open(final StorageConnector connector) throws DataStoreException {
        return new GSFStore(this, connector);
    }

    /**
     * Creates a {@code GSFStore} instance from the given parameters.
     *
     * @param  parameters  opening parameters as defined by {@link #getOpenParameters()}.
     * @return a data store instance associated with this provider for the given parameters.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public GSFStore open(final ParameterValueGroup parameters) throws DataStoreException {
        final var p = Parameters.castOrWrap(parameters);
        final var connector = new StorageConnector(p.getValue(URIDataStoreProvider.LOCATION_PARAM));
        return open(connector);
    }

    /**
     * Returns the logger used by <abbr>GSF</abbr> stores.
     */
    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
