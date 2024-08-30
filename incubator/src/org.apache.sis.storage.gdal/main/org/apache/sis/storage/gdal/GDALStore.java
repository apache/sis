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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.text.ParseException;
import java.net.URI;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.ref.Cleaner;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import org.opengis.util.NameSpace;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreClosedException;
import org.apache.sis.storage.InternalDataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.base.ResourceOnFileSystem;
import org.apache.sis.storage.base.URIDataStore;
import org.apache.sis.util.privy.UnmodifiableArrayList;
import org.apache.sis.util.iso.DefaultNameFactory;


/**
 * A data store using the <abbr>GDAL</abbr> library for all data accesses.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Quentin Bialota (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public class GDALStore extends DataStore implements Aggregate, ResourceOnFileSystem {
    /**
     * The {@link GDALStoreProvider#LOCATION} parameter value, or {@code null} if none.
     *
     * @see #getOpenParameters()
     */
    private final URI location;

    /**
     * Path of the file opened by this data store, or {@code null} if none.
     *
     * @see #getComponentFiles()
     */
    private final Path path;

    /**
     * The factory to use for creating the identifiers.
     */
    final NameFactory factory;

    /**
     * The identifier of this resource as a name space, or {@code null} if none.
     * This field should not be modified after construction.
     *
     * @see #getIdentifier()
     */
    NameSpace namespace;

    /**
     * Pointer to the <abbr>GDAL</abbr> object in native memory, or {@code null} if disposed.
     * This is a {@code GDALDatasetH} in the C/C++ <abbr>API</abbr>.
     *
     * @see #handle()
     * @see #dispose()
     */
    private MemorySegment handle;

    /**
     * The action to execute for closing the {@link GDALStore}.
     */
    private final Cleaner.Cleanable closer;

    /**
     * A description of this data set, or {@code null} if none.
     * This can be used as the {@linkplain #metadata} title.
     */
    String description;

    /**
     * A description of this resource as an unmodifiable metadata, or {@code null} if not yet computed.
     *
     * @see #getMetadata()
     */
    private Metadata metadata;

    /**
     * The components of this data set, created when first requested.
     *
     * @see #components()
     */
    private List<? extends Resource> components;

    /**
     * The object to use for parsing and formatting <abbr>CRS</abbr> definitions from/to <abbr>GDAL</abbr>.
     * This object must be used in a block synchronized on {@code this}. This is created when first needed.
     *
     * @see #wktFormat()
     * @see #parseCRS(GDAL, String)
     */
    private WKTFormat wktFormat;

    /**
     * Creates a new data store for reading the file specified by the given connector.
     * The {@code provider} argument is mandatory for this data store,
     * because it provides the links to the <var>GDAL</var> library.
     *
     * @param  provider   the factory that created this {@code GDALStore} instance.
     * @param  connector  information about the storage. Should be convertible to a URL.
     * @throws DataStoreException if an error occurred while creating the data store for the given storage.
     */
    @SuppressWarnings("this-escape")
    public GDALStore(final GDALStoreProvider provider, final StorageConnector connector) throws DataStoreException {
        super(Objects.requireNonNull(provider), connector);
        factory = DefaultNameFactory.provider();
        final String filename = IOUtilities.filename(connector.getStorage());
        if (filename != null) {
            namespace = factory.createNameSpace(factory.createLocalName(null, filename), null);
        }
        final String[] drivers;
        final Opener opener;
        drivers  = connector.getOption(GDALStoreProvider.DRIVERS_OPTION_KEY);
        path     = connector.getStorageAs(Path.class);
        location = connector.commit(URI.class, GDALStoreProvider.NAME);
        opener   = Opener.read(provider, Opener.toURL(location, path), drivers);
        closer   = GDALStoreProvider.CLEANERS.register(this, opener);   // Must do now in case of exception before completion.
        handle   = opener.handle;
    }

    /**
     * Creates a new data store as a child of the given store.
     * This constructor is the {@link Subdataset} constructor only.
     *
     * @param  parent  the parent data store.
     * @param  url     <abbr>URL</abbr> for <var>GDAL</var> of the data store to open.
     * @param  driver  name of the driver to use.
     * @throws DataStoreException if an error occurred while creating the data store for the given storage.
     */
    @SuppressWarnings("this-escape")
    GDALStore(final GDALStore parent, final String url, final String driver) throws DataStoreException {
        super(parent, parent.getProvider(), new StorageConnector(url), false);
        final Opener opener;
        path     = parent.path;
        location = parent.location;
        factory  = parent.factory;
        opener   = Opener.read(getProvider(), url, driver);
        closer   = GDALStoreProvider.CLEANERS.register(this, opener);   // Must do now in case of exception before completion.
        handle   = opener.handle;
    }

    /**
     * Returns the <abbr>GDAL</abbr> handle, or throws an exception if the data set is closed.
     * Must be invoked from a method synchronized on {@code this}.
     *
     * @see #close()
     */
    final MemorySegment handle() throws DataStoreClosedException {
        assert Thread.holdsLock(this);
        if (handle != null) return handle;
        throw new DataStoreClosedException("Data store is closed.");
    }

    /**
     * Returns the factory that created this {@code DataStore} instance.
     * The provider determines which <abbr>GDAL</abbr> library is used.
     *
     * @return the factory that created this {@code DataStore} instance.
     */
    @Override
    public final GDALStoreProvider getProvider() {
        return (GDALStoreProvider) super.getProvider();
    }

    /**
     * Returns the <abbr>GDAL</abbr> driver used for opening the file.
     *
     * @return the <abbr>GDAL</abbr> driver used for opening the file.
     * @throws DataStoreException if this data store is closed.
     */
    public synchronized Driver getDriver() throws DataStoreException {
        final GDAL gdal = getProvider().GDAL();
        final MemorySegment driver;
        try {
            driver = (MemorySegment) gdal.getDatasetDriver.invokeExact(handle());
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        // Should never be null, but verify as a safety.
        return GDAL.isNull(driver) ? null : new Driver(getProvider(), driver);
    }

    /**
     * Returns the name of the driver used for opening the file.
     * This name can be used as driver identifier for opening sub-components.
     *
     * @param  gdal  set of handles for invoking <abbr>GDAL</abbr> functions.
     * @return name of the <abbr>GDAL</abbr> driver used for opening the file.
     * @throws DataStoreException if the driver name cannot be fetched.
     */
    final String getDriverName(final GDAL gdal) throws DataStoreException {
        try {
            var result = (MemorySegment) gdal.getDatasetDriver.invokeExact(handle());
            if (!GDAL.isNull(result)) {     // Paranoiac check.
                result = (MemorySegment) gdal.getName.invokeExact(result);
                return GDAL.toString(result);
            }
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        throw new InternalDataStoreException("Cannot get the driver name.");
    }

    /**
     * Returns the parameters used for opening this <abbr>GDAL</abbr> data store.
     * The parameters are described by {@link GDALStoreProvider#getOpenParameters()} and contains at least
     * a parameter named {@value org.apache.sis.storage.DataStoreProvider#LOCATION} with a {@link URI} value.
     * The return value may be empty if the storage input cannot be described by a URI.
     *
     * @return parameters used for opening this data store.
     */
    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        final Parameters param = Parameters.castOrWrap(URIDataStore.parameters(provider, location));
        if (param != null) try {
            param.getOrCreate(GDALStoreProvider.DRIVERS_PARAM).setValue(new String[] {
                getDriver().getIdentifier()}
            );
        } catch (DataStoreException e) {
            listeners.warning(e);
        }
        return Optional.ofNullable(param);
    }

    /**
     * Returns an identifier for the root resource of this data store, or an empty value if none.
     *
     * @return an identifier for the root resource of this data store.
     * @throws DataStoreException if an error occurred while fetching the identifier.
     */
    @Override
    public final Optional<GenericName> getIdentifier() throws DataStoreException {
        return (namespace != null) ? Optional.of(namespace.name()) : Optional.empty();
    }

    /**
     * Returns information about the data store as a whole.
     *
     * @return information about resources in the data store.
     * @throws DataStoreException if an error occurred while reading the metadata.
     */
    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) {
            final var builder = new MetadataBuilder();
            builder.addTitle(description);
            builder.addIdentifier(getIdentifier().orElse(null), MetadataBuilder.Scope.RESOURCE);
            // TODO: add more information.
            return builder.buildAndFreeze();
        }
        return metadata;
    }

    /**
     * Returns the children resources of this aggregate.
     *
     * @return the resources that are contained in this aggregate.
     * @throws DataStoreException if an error occurred in a <abbr>GDAL</abbr> function.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")   // Because unmodifable.
    public synchronized Collection<? extends Resource> components() throws DataStoreException {
        if (components == null) {
            final GDAL gdal = getProvider().GDAL();
            final List<Subdataset> subdatasets = groupBySubset(gdal);
            if (subdatasets != null && !subdatasets.isEmpty()) {
                components = subdatasets;
            } else {
                final TiledResource[] rasters = TiledResource.groupBySizeAndType(this, gdal, handle);
                if (gdal.checkCPLErr(this, "components", true)) {
                    components = UnmodifiableArrayList.wrap(rasters);
                } else {
                    components = List.of();
                }
            }
        }
        return components;
    }

    /**
     * If the data set declares sub-components, creates sub-data stores for those sub-components.
     * The sub-components are defined in a <abbr>GDAL</abbr> metadata having a content like below:
     *
     * <pre>SUBDATASET_1_NAME=NETCDF:"/run/media/.../C1_2020.nc":elevation
     * SUBDATASET_1_DESC=[9004x9482] geoid_height_above_reference_ellipsoid (32-bit floating-point)
     * SUBDATASET_2_NAME=NETCDF:"/run/media/.../C1_2020.nc":value_count
     * SUBDATASET_2_DESC=[9004x9482] value_count (32-bit integer)</pre>
     *
     * @param  gdal  set of handles for invoking <abbr>GDAL</abbr> functions.
     * @return the sub-components, or {@code null} if none. May be empty if no metadata has been recognized.
     */
    List<Subdataset> groupBySubset(final GDAL gdal) throws DataStoreException {
        final MemorySegment result;
        try (var arena = Arena.ofConfined()) {
            final MemorySegment domain = arena.allocateFrom("SUBDATASETS");
            result = (MemorySegment) gdal.getMetadata.invokeExact(handle, domain);
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        final List<String> all = GDAL.toStringArray(result);
        if (all != null) {
            final String driver = getDriverName(gdal);
            if (driver != null) {   // Should never be null.
                return new SubdatasetList(gdal, this, all);
            }
        }
        return null;
    }

    /**
     * Gets the paths to files potentially used by this resource.
     * This method returns the path to the main file opened by the <abbr>GDAL</abbr> driver,
     * and applies heuristic rules for guessing which other paths may be part of the format.
     * Heuristic rules are used because <abbr>GDAL</abbr> provides no <abbr>API</abbr> for
     * listing which files are used.
     *
     * @return files used by this resource, or an empty array if unknown.
     * @throws DataStoreException if an error on the file system prevent the creation of the list.
     */
    @Override
    public Path[] getComponentFiles() throws DataStoreException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final Path path = this.path;
        if (path == null) {
            return new Path[0];
        }
        final DirectoryStream.Filter<Path> filter;
        switch (getDriver().getIdentifier()) {
            default: {
                return new Path[] {path};
            }
            case "AIG": {       // Arc/Info Binary Grid
                // Special case: we must take "metadata.xml" and all "*.adf" in the folder.
                filter = (Path entry) -> ("metadata.xml".equalsIgnoreCase(entry.getFileName().toString())
                                          || "adf".equalsIgnoreCase(IOUtilities.extension(entry)));
                break;
            }
            // TODO: list more case where we want the same default as GeoTIFF.
            case "GTiff": {
                // List all existing paths with the same file name but possibly with different suffixes.
                final String filename = path.getFileName().toString();
                final int s = filename.lastIndexOf('.');
                final String base = (s >= 0) ? filename.substring(0, s+1) : filename;
                filter = (Path entry) -> entry.getFileName().toString().startsWith(base);
                break;
            }
        }
        final var paths = new ArrayList<Path>();
        paths.addFirst(path);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path.getParent(),
                (entry) -> !entry.equals(path) && filter.accept(entry) && Files.isRegularFile(entry)))
        {
            stream.forEach(paths::add);
        } catch (IOException ex) {
            throw cannotExecute(ex);
        }
        // Ensure that the main path is first.
        return paths.toArray(Path[]::new);
    }

    /**
     * Returns the object to use for parsing and formatting <abbr>CRS</abbr> definitions from/to <abbr>GDAL</abbr>.
     * This object must be used in a block synchronized on {@code this}.
     */
    private WKTFormat wktFormat() {
        if (wktFormat == null) {
            wktFormat = new WKTFormat(null, null);
            wktFormat.setConvention(Convention.WKT1_COMMON_UNITS);
        }
        return wktFormat;
    }

    /**
     * Gets the <abbr>CRS</abbr> of the data set by parsing its <abbr>WKT</abbr> representation.
     * This method must be invoked from a method synchronized on {@code this}.
     *
     * @param  gdal    set of handles for invoking <abbr>GDAL</abbr> functions.
     * @param  caller  name of the {@code GDALStore} method invoking this method.
     * @return the parsed <abbr>CRS</abbr>, or {@code null} if none.
     * @throws DataStoreException if a fatal error occurred according <abbr>GDAL</abbr>.
     */
    final CoordinateReferenceSystem parseCRS(final GDAL gdal, final String caller) throws DataStoreException {
        MemorySegment result = null;
        try {
            gdal.errorReset();
            result = (MemorySegment) gdal.getSpatialRef.invokeExact(handle());
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        if (gdal.checkCPLErr(this, caller, false)) {
            final String wkt = GDAL.toString(result);
            if (wkt != null && !wkt.isBlank()) try {
                return (CoordinateReferenceSystem) wktFormat().parseObject(wkt);
            } catch (ParseException | ClassCastException e) {
                warning(caller, "Cannot parse the CRS of " + getDisplayName(), e);
            }
        }
        return null;
    }

    /**
     * Returns the exception to throw for the given cause.
     *
     * @param  cause    the cause of the error. Cannot be null.
     * @return the data store exception to throw.
     */
    private static DataStoreException cannotExecute(final Exception cause) {
        return new DataStoreException(cause.getMessage(), cause);
    }

    /**
     * Sends a warning to the listeners registered in the {@code GDALStore}.
     *
     * @param caller   the method to report as the emitted of the warning.
     * @param message  the message.
     * @param cause    the cause of the warning.
     */
    final void warning(final String caller, final String message, final Exception cause) {
        var record = new LogRecord(Level.WARNING, message);
        record.setThrown(cause);
        warning(caller, record);
    }

    /**
     * Sends a warning to the listeners, or log the warning if there is no listeners.
     * This method set the logger name, source class and method class on the record.
     *
     * @param  caller  the {@code GDALStore} method to report as the emitter.
     * @param  record  the warning to report.
     */
    final void warning(final String caller, final LogRecord record) {
        record.setLoggerName(GDALStoreProvider.LOGGER.getName());
        record.setSourceClassName(GDALStore.class.getCanonicalName());
        record.setSourceMethodName(caller);
        listeners.warning(record);
    }

    /**
     * Closes this data store and releases any underlying resources.
     * A {@link CloseEvent} is sent to listeners before the data store is closed.
     * Any attempt to use this store after closing will result in {@link DataStoreClosedException}.
     */
    @Override
    public void close() throws DataStoreException {
        try {
            listeners.close();      // Should never fail.
        } finally {
            synchronized (this) {
                handle = null;
                closer.clean();
            }
        }
    }
}
