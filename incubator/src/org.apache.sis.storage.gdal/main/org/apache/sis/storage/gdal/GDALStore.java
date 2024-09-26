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
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.net.URI;
import java.nio.file.Path;
import java.lang.ref.Cleaner;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import org.opengis.util.NameSpace;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreClosedException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.base.URIDataStore;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.privy.UnmodifiableArrayList;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.system.Cleaners;


/**
 * A data store using the <abbr>GDAL</abbr> library for all data accesses.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Quentin Bialota (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public class GDALStore extends DataStore implements Aggregate {
    /**
     * The {@link GDALStoreProvider#LOCATION} parameter value, or {@code null} if none.
     *
     * @see #getOpenParameters()
     */
    private final URI location;

    /**
     * Path of the file opened by this data store, or {@code null} if none.
     *
     * @see #getFileSet()
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
        drivers    = connector.getOption(GDALStoreProvider.DRIVERS_OPTION_KEY);
        location   = connector.getStorageAs(URI.class);
        path       = connector.getStorageAs(Path.class);
        String url = connector.commit(String.class, Constants.GDAL);
        if (location != null) {
            url = Opener.toURL(location, path, true);
        }
        Opener opener;
        opener = new Opener(provider, url, drivers);
        closer = Cleaners.SHARED.register(this, opener);    // Must do now in case of exception before completion.
        handle = opener.handle;
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
        opener   = new Opener(getProvider(), url, driver);
        closer   = Cleaners.SHARED.register(this, opener);      // Must do now in case of exception before completion.
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
        throw new DataStoreClosedException(getLocale(), Constants.GDAL);
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
        } finally {
            ErrorHandler.throwOnFailure(this, "getDriver");
        }
        // Should never be null, but verify as a safety.
        return GDAL.isNull(driver) ? null : new Driver(getProvider(), driver);
    }

    /**
     * Returns the name of the driver used for opening the file.
     * This name can be used as driver identifier for opening sub-components.
     *
     * @param  gdal  set of handles for invoking <abbr>GDAL</abbr> functions.
     * @return name of the <abbr>GDAL</abbr> driver used for opening the file, or {@code null} if none.
     */
    private String getDriverName(final GDAL gdal) {
        try {
            var result = (MemorySegment) gdal.getDatasetDriver.invokeExact(handle());
            if (!GDAL.isNull(result)) {     // Paranoiac check.
                result = (MemorySegment) gdal.getName.invokeExact(result);
                return GDAL.toString(result);
            }
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        return null;
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
     * Returns an array of files believed to be part of this resource.
     *
     * @todo This method is often used for copying a resources from one location to another.
     *       <abbr>GDAL</abbr> provides a {@code GDALCopyDatasetFiles} function for this purpose.
     *       That function is not yet used by Apache <abbr>SIS</abbr>.
     *
     * @return files used by this resource, or an empty value if unknown.
     * @throws DataStoreException if the list of files cannot be obtained.
     */
    @Override
    public synchronized Optional<FileSet> getFileSet() throws DataStoreException {
        final GDAL gdal = getProvider().GDAL();
        final List<String> files;
        try {
            final var list = (MemorySegment) gdal.getFileList.invokeExact(handle());
            try {
                files = GDAL.fromNullTerminatedStrings(list);
            } finally {
                gdal.destroy.invokeExact(list);
            }
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        final FileSet fs;
        if (files != null && !files.isEmpty()) {
            final var paths = new Path[files.size()];
            for (int i=0; i < paths.length; i++) {
                var item = Path.of(files.get(i));
                if (path != null) {
                    item = path.resolveSibling(item);
                }
                paths[i] = item;
            }
            fs = getDriver().new FileList(paths, path, location);
        } else if (path != null) {
            fs = new FileSet(path);
        } else {
            return Optional.empty();
        }
        return Optional.of(fs);
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
            addFormatInfo(builder);
            // TODO: add more information.
            return builder.buildAndFreeze();
        }
        return metadata;
    }

    /**
     * Adds information about the data format.
     *
     * @param  builder  the builder where to add information.
     */
    final void addFormatInfo(final MetadataBuilder builder) throws DataStoreException {
        final Driver driver = getDriver();
        if (driver != null) {
            builder.addFormatName(driver.getName());
            String id = driver.getIdentifier();
            if (id != null) {
                builder.addFormatReader(new ImmutableIdentifier(Citations.GDAL, Constants.GDAL, id),
                                        getProvider().getVersion().orElse(null));
            }
        }
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
        if (components == null) try {
            final GDAL gdal = getProvider().GDAL();
            final List<Subdataset> subdatasets = groupBySubset(gdal);
            if (subdatasets != null && !subdatasets.isEmpty()) {
                components = subdatasets;
            } else {
                components = UnmodifiableArrayList.wrap(TiledResource.groupBySizeAndType(this, gdal, handle()));
            }
        } finally {
            ErrorHandler.throwOnFailure(this, "components");
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
            result = (MemorySegment) gdal.getMetadata.invokeExact(handle(), domain);
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        final List<String> all = GDAL.fromNullTerminatedStrings(result);
        if (all != null) {
            final String driver = getDriverName(gdal);
            if (driver != null) {   // Should never be null.
                return new SubdatasetList(gdal, this, driver, all);
            }
        }
        return null;
    }

    /**
     * Returns the object to use for parsing and formatting <abbr>CRS</abbr> definitions from/to <abbr>GDAL</abbr>.
     * This object must be used in a block synchronized on {@code this}.
     */
    final WKTFormat wktFormat() {
        if (wktFormat == null) {
            wktFormat = new WKTFormat(null, null);
            wktFormat.setConvention(Convention.WKT1_COMMON_UNITS);
        }
        return wktFormat;
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
        final class Flush implements AutoCloseable {
            @Override public void close() throws DataStoreException {
                ErrorHandler.throwOnFailure(GDALStore.this, "close");
            }
        }
        try (Flush _ = new Flush()) {
            closeRecursively();
        }
    }

    /**
     * Closes all child components (if any) recursively, then closes this data store.
     */
    private void closeRecursively() {
        RuntimeException error = null;
        try {
            listeners.close();
        } catch (RuntimeException e) {
            error = e;      // Should not happen even if a listener threw an exception, but we want to be safe.
        } finally {
            try {
                final List<? extends Resource> children;
                synchronized (this) {
                    children = components;
                    components = null;
                }
                if (children != null) {
                    for (Resource child : children) {
                        if (child instanceof Subdataset) try {
                            ((GDALStore) child).closeRecursively();
                        } catch (RuntimeException e) {
                            if (error == null) error = e;
                            else error.addSuppressed(e);
                        }
                    }
                }
            } finally {
                synchronized (this) {
                    handle = null;
                    closer.clean();     // Important to always invoke, even if an exception occurred before.
                }
            }
        }
        if (error != null) {
            throw error;
        }
    }
}
