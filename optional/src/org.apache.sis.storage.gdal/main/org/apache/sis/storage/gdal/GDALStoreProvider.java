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
package org.apache.sis.storage.gdal;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.function.Function;
import java.time.LocalDate;
import java.nio.file.Path;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.base.StoreMetadata;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.URIDataStoreProvider;
import org.apache.sis.storage.panama.LibraryStatus;
import org.apache.sis.storage.panama.Resources;
import org.apache.sis.io.stream.InternalOptionKey;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.system.Cleaners;
import org.apache.sis.util.Version;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.collection.TreeTable;


/**
 * The provider of {@code GDALStore} instances.
 * Given a {@link StorageConnector} input, this class tries to instantiate a {@link GDALStore}.
 * This data store uses the <abbr>GDAL</abbr> native library for loading data.
 *
 * <p>Each {@code GDALStoreProvider} instance is linked to a <abbr>GDAL</abbr> library specified
 * at construction time, or to a <abbr>JVM</abbr>-wide shared <abbr>GDAL</abbr> library if the
 * no-argument constructor is used. In the non-shared case, <abbr>GDAL</abbr> will be unloaded
 * when this {@code GDALStoreProvider} instance is garbage collected.</p>
 *
 * @author  Quentin Bialota (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
@StoreMetadata(formatName    = Constants.GDAL,
               capabilities  = {Capability.READ},
               resourceTypes = {Aggregate.class, GridCoverageResource.class},
               yieldPriority = true)    // For trying Java implementations before GDAL.
public class GDALStoreProvider extends DataStoreProvider {
    /**
     * The logger used by <abbr>GDAL</abbr> stores.
     *
     * @see #getLogger()
     */
    static final Logger LOGGER = Logger.getLogger("org.apache.sis.storage.gdal");

    /**
     * Key for declaring <abbr>GDAL</abbr> drivers that may be used for opening the file.
     * Drivers are identified by their {@linkplain Driver#getIdentifier() short names}.
     * The drivers will be tested in the order they are specified.
     *
     * @see Driver#getIdentifier()
     */
    public static final OptionKey<String[]> DRIVERS_OPTION_KEY = new InternalOptionKey<>("DRIVERS", String[].class);

    /**
     * Parameter for declaring <abbr>GDAL</abbr> drivers that may be used for opening the file.
     * Drivers are identified by their {@linkplain Driver#getIdentifier() short names}.
     * The drivers will be tested in the order they are specified.
     *
     * @see Driver#getIdentifier()
     */
    static final ParameterDescriptor<String[]> DRIVERS_PARAM;

    /**
     * The parameter descriptor to be returned by {@link #getOpenParameters()}.
     */
    private static final ParameterDescriptorGroup OPEN_DESCRIPTOR;
    static {
        final var builder = new ParameterBuilder();
        DRIVERS_PARAM = builder.addName("drivers")
                .setDescription(Resources.formatInternational(Resources.Keys.AllowedDrivers_1, Constants.GDAL))
                .create(String[].class, null);
        OPEN_DESCRIPTOR = builder.addName(Constants.GDAL).createGroup(URIDataStoreProvider.LOCATION_PARAM, DRIVERS_PARAM);
    }

    /**
     * Handles to <abbr>GDAL</abbr> native functions, or {@code null} for global.
     * Can also be reset to {@code null} if <abbr>GDAL</abbr> reported a fatal error.
     */
    private GDAL nativeFunctions;

    /**
     * The status of {@link #nativeFunctions}, or {@code null} if the global set of functions is used.
     */
    private LibraryStatus status;

    /**
     * Creates a new provider which will load the <abbr>GDAL</abbr> library from the default library path.
     */
    public GDALStoreProvider() {
    }

    /**
     * Creates a new provider which will load the <abbr>GDAL</abbr> library from the specified file.
     * The library will be unloaded when this provider will be garbage-collected.
     *
     * @throws IllegalArgumentException if the GDAL library has not been found.
     * @throws NoSuchElementException if a <abbr>GDAL</abbr> function has not been found in the library.
     * @throws IllegalCallerException if this Apache SIS module is not authorized to call native methods.
     */
    @SuppressWarnings("this-escape")
    public GDALStoreProvider(final Path library) {
        Cleaners.SHARED.register(this, nativeFunctions = GDAL.load(library));
        status = LibraryStatus.LOADED;
    }

    /**
     * Returns the set of <abbr>GDAL</abbr> native functions.
     *
     * @return the set of native functions.
     * @throws DataStoreException if the native library is not available.
     */
    final synchronized GDAL GDAL() throws DataStoreException {
        if (status == null) {
            return GDAL.global();       // Fetch each time (no cache) because may have changed outside this class.
        }
        status.report(Constants.GDAL, null);        // Should never return if `nativeFunctions` is null.
        return nativeFunctions;
    }

    /**
     * Tries to load <abbr>GDAL</abbr> if not already done, without throwing an exception in case of error.
     * Instead, the error is logged and {@code null} is returned. This is used for probing.
     *
     * @param  classe  the class which is invoking this method (for logging purpose).
     * @param  method  the name of the method which is invoking this method (for logging purpose).
     * @return the set of native functions, or {@code null} if not available.
     */
    final synchronized Optional<GDAL> tryGDAL(final Class<?> classe, final String method) {
        if (status == null) {
            return GDAL.tryGlobal(classe, method);
        }
        return Optional.ofNullable(nativeFunctions);
    }

    /**
     * Invoked when <abbr>GDAL</abbr> reported a fatal error and tells us that we should not use it anymore.
     *
     * @todo Not sure it is effective, because <abbr>GDAL</abbr> documentation of {@code CPLSetErrorHandler}
     *       said that it invokes {@code abort()} on fatal errors. The call to {@code abort()} can be avoided
     *       by a {@code longjmp()}, but it seems unlikely that we can do that.
     */
    final synchronized void fatalError() {
        nativeFunctions = null;
        status = LibraryStatus.FATAL_ERROR;
    }

    /**
     * Returns a generic name for this data store, used mostly in warnings or error messages.
     *
     * @return a short name or abbreviation for the data store.
     */
    @Override
    public String getShortName() {
        return Constants.GDAL;
    }

    /**
     * Returns a value from the <abbr>GDAL</abbr> {@code GDALVersionInfo} function.
     *
     * @param  <V>      type of object to build from the version information.
     * @param  caller   the name of the method which is invoking this method.
     * @param  request  the desired version information. See {@link GDAL#version(String)} for more information.
     * @param  mapper   the function for converting the string to the desired object.
     * @return the version information computed from the string provided by GDAL.
     */
    private <V> Optional<V> version(final String caller, final String request, final Function<String, V> mapper) {
        return tryGDAL(GDALStoreProvider.class, caller).flatMap((gdal) -> gdal.version(request)).map(mapper);
    }

    /**
     * Returns the version number of the <abbr>GDAL</abbr> library.
     * The returned version contains 4 components. Example: 3.6.3.0.
     *
     * @return <abbr>GDAL</abbr> version, or empty if the <abbr>GDAL</abbr> library has not been found.
     */
    public Optional<Version> getVersion() {
        return version("getVersion", "VERSION_NUM", (version) -> {
            int n = Integer.parseInt(version);
            final int[] components = new int[4];
            for (int i=components.length; --i >= 0;) {
                components[i] = n % 100;
                n /= 100;
            }
            return Version.valueOf(components);
        });
    }

    /**
     * Returns the release date of the <abbr>GDAL</abbr> library.
     *
     * @return the release date, or empty if the <abbr>GDAL</abbr> library has not been found.
     */
    public Optional<LocalDate> getReleaseDate() {
        return version("getReleaseDate", "RELEASE_DATE", (version) -> {
            int year  = Integer.parseInt(version);
            int day   = year % 100; year /= 100;
            int month = year % 100; year /= 100;
            return LocalDate.of(year, month, day);
        });
    }

    /**
     * Returns an unmodifiable list of all <abbr>GDAL</abbr> drivers currently available.
     *
     * @return all <abbr>GDAL</abbr> drivers, or an empty list if the <abbr>GDAL</abbr> library has not been found.
     */
    public List<Driver> getDrivers() {
        return Driver.list(this, tryGDAL(GDALStoreProvider.class, "getDrivers").orElse(null));
    }

    /**
     * Returns the <abbr>GDAL</abbr> version number together with the list of drivers.
     * The <abbr>GDAL</abbr> {@linkplain #getVersion() version} is in the root node,
     * and drivers are listed as children. The table contains the following columns:
     *
     * <ol>
     *   <li>{@link TableColumn#NAME} for the value returned by {@link #getName()}.</li>
     *   <li>{@link TableColumn#IDENTIFIER} for the value returned by {@link #getIdentifier()}.</li>
     * </ol>
     *
     * The current version lists only the drivers having raster capability,
     * because this is the only type of data supported by the current implementation of this module.
     *
     * @return a table of available <abbr>GDAL</abbr> drivers.
     * @throws DataStoreException if an error occurred while querying <abbr>GDAL</abbr> metadata.
     */
    public TreeTable configuration() throws DataStoreException {
        return Driver.configuration(this, tryGDAL(GDALStoreProvider.class, "configuration").orElse(null));
    }

    /**
     * Returns a description of all parameters accepted by this provider for opening a file with GDAL.
     *
     * @return description of available parameters for opening a file with <abbr>GDAL</abbr>.
     */
    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return OPEN_DESCRIPTOR;
    }

    /**
     * Returns the MIME type if the given storage appears to be supported by this data store.
     * A {@linkplain ProbeResult#isSupported() supported} status does not guarantee that reading
     * or writing will succeed, only that there appears to be a reasonable chance according GDAL.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @return a {@linkplain ProbeResult#isSupported() supported} status with the MIME type
     *         if the given storage seems to be readable by {@code GDALStore} instances.
     * @throws DataStoreException if an I/O error occurred.
     */
    @Override
    public ProbeResult probeContent(final StorageConnector connector) throws DataStoreException {
        return Opener.probeContent(this, connector);
    }

    /**
     * Creates a {@code GDALStore} instance associated with this provider.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @return a data store implementation associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final StorageConnector connector) throws DataStoreException {
        return new GDALStore(this, connector);
    }

    /**
     * Creates a {@code GDALStore} instance from the given parameters.
     *
     * @param  parameters  opening parameters as defined by {@link #getOpenParameters()}.
     * @return a data store instance associated with this provider for the given parameters.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final ParameterValueGroup parameters) throws DataStoreException {
        final var p = Parameters.castOrWrap(parameters);
        final var connector = new StorageConnector(p.getValue(URIDataStoreProvider.LOCATION_PARAM));
        final String[] drivers = p.getValue(DRIVERS_PARAM);
        if (drivers != null) {
            connector.setOption(DRIVERS_OPTION_KEY, drivers);
        }
        return open(connector);
    }

    /**
     * Returns the logger used by <abbr>GDAL</abbr> stores.
     */
    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
