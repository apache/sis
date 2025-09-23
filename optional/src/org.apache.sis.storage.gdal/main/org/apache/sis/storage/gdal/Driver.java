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
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Optional;
import java.util.Objects;
import java.net.URI;
import java.io.IOException;
import java.nio.file.Path;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.collection.DefaultTreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.panama.Resources;


/**
 * Information about a <abbr>GDAL</abbr> driver.
 *
 * @author  Quentin Bialota (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public final class Driver {
    /**
     * Owner of the set of handles that this class will use for invoking <abbr>GDAL</abbr> functions.
     * Another purpose of this reference is to prevent premature garbage-collection,
     * because it would unload the <abbr>GDAL</abbr> library.
     */
    private final GDALStoreProvider owner;

    /**
     * Pointer to the <abbr>GDAL</abbr> object in native memory.
     * This is a {@code GDALDriverH} in the C/C++ <abbr>API</abbr>.
     */
    private final MemorySegment handle;

    /**
     * Creates a new instance.
     *
     * @param  owner   owner of the set of handles for invoking <abbr>GDAL</abbr> functions.
     * @param  handle  pointer to the <abbr>GDAL</abbr> driver in native memory.
     */
    Driver(final GDALStoreProvider owner, final MemorySegment handle) {
        this.owner  = owner;
        this.handle = handle;
    }

    /**
     * Returns the long name of the driver. For the GeoTIFF driver, this is {@code "GeoTIFF"}.
     *
     * @return the driver long name, or {@code null}.
     * @throws DataStoreException if an error occurred while invoking a <abbr>GDAL</abbr> function.
     */
    public String getName() throws DataStoreException {
        final var gdal = owner.GDAL().getName;
        final MemorySegment result;
        try {
            result = (MemorySegment) gdal.invokeExact(handle);
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        return GDAL.toString(result);
    }

    /**
     * Returns the short name of the driver. For the GeoTIFF driver, this is {@code "GTiff"}.
     * This is the name used by <abbr>GDAL</abbr> for identifying a driver on the command-line.
     *
     * @return the driver short name, or {@code null}.
     * @throws DataStoreException if an error occurred while invoking a <abbr>GDAL</abbr> function.
     *
     * @see GDALStoreProvider#DRIVERS_OPTION_KEY
     */
    public String getIdentifier() throws DataStoreException {
        final var gdal = owner.GDAL().getIdentifier;
        final MemorySegment result;
        try {
            result = (MemorySegment) gdal.invokeExact(handle);
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        return GDAL.toString(result);
    }

    /**
     * Returns the MIME type handled by the driver.
     *
     * @return the MIME type handled by the driver, or {@code null} if unspecified.
     * @throws DataStoreException if an error occurred while invoking a <abbr>GDAL</abbr> function.
     */
    public String getMIMEType() throws DataStoreException {
        return getMetadataItem("DMD_MIMETYPE", null);
    }

    /**
     * Returns the file extensions handled by the driver.
     *
     * @return the file extensions handled by the driver, or an empty array if none.
     * @throws DataStoreException if an error occurred while invoking a <abbr>GDAL</abbr> function.
     *
     * @see javax.imageio.spi.ImageReaderWriterSpi#ImageReaderWriterSpi()
     */
    public String[] getFileSuffixes() throws DataStoreException {
        return (String[]) CharSequences.split(getMetadataItem("DMD_EXTENSIONS", null), ' ');
    }

    /**
     * Returns whether the driver has raster capability.
     *
     * @return whether the driver has raster capability.
     * @throws DataStoreException if an error occurred while invoking a <abbr>GDAL</abbr> function.
     */
    public boolean supportRasters() throws DataStoreException {
        return "YES".equalsIgnoreCase(getMetadataItem("DCAP_RASTER", null));
    }

    /**
     * Fetches a single metadata item. The following table lists some metadata:
     *
     * <table class="sis">
     *   <caption>Some metadata keys</caption>
     *   <tr><th>Key</th>                      <th>Description</th></tr>
     *   <tr><td>{@code "DMD_MIMETYPE"}</td>   <td>MIME type handled by the driver.</td></tr>
     *   <tr><td>{@code "DMD_EXTENSIONS"}</td> <td>space separated list of file extensions handled by the driver.</td></tr>
     *   <tr><td>{@code "DCAP_RASTER"}</td>    <td>Whether the driver has raster capability.</td></tr>
     * </table>
     *
     * @param  name    the key for the metadata item to fetch.
     * @param  domain  the domain to fetch for, or {@code null} for the default domain.
     * @return the metadata item value, or {@code null} if none.
     * @throws DataStoreException if an error occurred while invoking a <abbr>GDAL</abbr> function.
     */
    public String getMetadataItem(final String name, final String domain) throws DataStoreException {
        final var gdal = owner.GDAL().getMetadataItem;
        final MemorySegment result;
        try (var arena = Arena.ofConfined()) {
            final MemorySegment opt = (domain != null) ? arena.allocateFrom(domain) : MemorySegment.NULL;
            result = (MemorySegment) gdal.invokeExact(handle, arena.allocateFrom(name), opt);
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        return GDAL.toString(result);
    }

    /**
     * List of files of a <abbr>GDAL</abbr> data set, together with methods for copying or deleting them.
     * The list of files is fetched in advance by the caller because the list needs to stay available after
     * {@link GDALStore} has been closed. This class overrides the {@link #copy(Path)} and {@link #delete()}
     * methods for delegating the operation to <abbr>GDAL</abbr> when possible. If not possible, this class
     * fallbacks on the default Java implementation.
     */
    final class FileList extends Resource.FileSet {
        /**
         * Location of the data store as a path. It should be the first element of {@link #getPaths()},
         * but is stored anyway because we have no guarantee that <abbr>GDAL</abbr> includes that file
         * or puts it first in its list of files. May be {@code null} if not available.
         *
         * @see GDALStore#path
         */
        private final Path path;

        /**
         * Location of the data store as a <abbr>URL</abbr>. Should never be null.
         *
         * @see GDALStore#location
         */
        private final URI location;

        /**
         * Creates a new {@code FileSet} for the given files.
         *
         * @param  paths     the files to be returned by {@link #getPaths()}.
         * @param  path      the main file, or {@code null} if not available.
         * @param  location  location of the data store as a <abbr>URL</abbr>.
         */
        FileList(final Path[] paths, final Path path, final URI location) {
            super(paths);
            this.path = path;
            this.location = location;
        }

        /**
         * Copies the files to the given directory. The source should be a <abbr>URL</abbr> recognized
         * by <abbr>GDAL</abbr> because this {@code FileList} can be returned only by {@link GDALStore}.
         * However, the destination directory given in argument to this method may be a Java file system.
         * In the latter case, this method delegates on the Java implementation instead of <abbr>GDAL</abbr>.
         */
        @Override
        public Path copy(final Path destDir) throws IOException {
            final Path   file   = destDir.resolve(path.getFileName());
            final String target = Opener.toURL(file.toUri(), file, false);
            if (target != null && invoke("copy", "GDALCopyDatasetFiles", target)) {
                return file;
            }
            return super.copy(destDir);
        }

        /**
         * Deletes the files of the data set. This method delegates to <abbr>GDAL</abbr>,
         * but fallbacks on Java code if the <abbr>GDAL</abbr> function fails.
         */
        @Override
        public void delete() throws IOException {
            if (!invoke("delete", "GDALDeleteDataset", null)) {
                super.delete();
            }
        }

        /**
         * Optionally invokes the <abbr>GDAL</abbr> function for copying or deleting a data set.
         * We fetch the method handle in this method rather than in the {@link GDAL} class
         * because those methods should be rarely needed, and also because they are optional.
         * The <abbr>GDAL</abbr> functions handled by this method are:
         * <ul>
         *   <li>{@code CPLErr GDALCopyDatasetFiles(GDALDriverH, const char *pszNewName, const char *pszOldName)}</li>
         *   <li>{@code CPLErr GDALDeleteDataset(GDALDriverH, const char*)}</li>
         * </ul>
         *
         * @param  caller    name of the Java method invoking this method, for logging purpose only.
         * @param  function  name of the <abbr>GDAL</abbr> function to invoke.
         * @param  target    the target directory if invoking the copy function, or null for the delete function.
         * @return whether the operation has been successful.
         */
        @SuppressWarnings("restricted")
        private boolean invoke(final String caller, final String function, final String target) {
            Optional<MethodHandle> method = owner.tryGDAL(Driver.class, caller).flatMap((gdal) -> {
                return gdal.symbols.find(function).map((address) -> {
                    var signature = new ValueLayout[(target != null) ? 3 : 2];
                    Arrays.fill(signature, ValueLayout.ADDRESS);
                    return gdal.linker.downcallHandle(address, FunctionDescriptor.of(ValueLayout.JAVA_INT, signature));
                });
            });
            if (method.isEmpty()) {
                return false;
            }
            /*
             * In both "GDALCopyDatasetFiles" and "GDALDeleteDataset" function, the driver
             * handle is the first argument and the dataset URL is the last argument.
             */
            final String url = Opener.toURL(location, path, true);
            final int err;
            try (var arena = Arena.ofConfined()) {
                final var source = arena.allocateFrom(url);
                if (target != null) {
                    err = (int) method.get().invokeExact(handle, arena.allocateFrom(target), source);
                } else {
                    err = (int) method.get().invokeExact(handle, source);
                }
            } catch (Throwable e) {
                throw GDAL.propagate(e);
            }
            return err == 0;
        }
    }

    /**
     * Returns the <abbr>GDAL</abbr> version number together with the list of drivers.
     * See {@link GDALStoreProvider#configuration()} for the public documentation.
     *
     * @param  owner  owner of the set of handles to <abbr>GDAL</abbr> functions.
     * @param  gdal   handles to <abbr>GDAL</abbr> native functions, or {@code null} if none.
     * @return a table of available <abbr>GDAL</abbr> drivers.
     */
    static TreeTable configuration(final GDALStoreProvider owner, final GDAL gdal) {
        final var longNameColumn  = TableColumn.NAME;
        final var shortNameColumn = TableColumn.IDENTIFIER;
        final var table = new DefaultTreeTable(shortNameColumn, longNameColumn);
        final TreeTable.Node root = table.getRoot();
        final String version = (gdal != null) ? gdal.version("--version").orElse(null) : null;
        root.setValue(shortNameColumn, (gdal != null) ? gdal.libraryName : Resources.format(Resources.Keys.LibraryNotFound_1, Constants.GDAL));
        root.setValue(longNameColumn, (version != null) ? version : Vocabulary.format(Vocabulary.Keys.NotKnown));

        DataStoreException error = null;
        for (final Driver driver : list(owner, gdal)) {
            try {
                if (driver.supportRasters()) {
                    final TreeTable.Node node = root.newChild();
                    node.setValue(shortNameColumn, driver.getIdentifier());
                    node.setValue (longNameColumn, driver.getName());
                }
           } catch (DataStoreException e) {
               if (error == null) error = e;
               else error.addSuppressed(e);
           }
        }
        if (error != null) {
            Logging.unexpectedException(GDALStoreProvider.LOGGER, GDALStoreProvider.class, "configuration", error);
        }
        return table;
    }

    /**
     * Returns an unmodifiable list of all <abbr>GDAL</abbr> drivers currently available.
     *
     * @param  owner  owner of the set of handles to <abbr>GDAL</abbr> functions.
     * @param  gdal   handles to <abbr>GDAL</abbr> native functions, or {@code null} if none.
     * @return all <abbr>GDAL</abbr> drivers, or an empty list if the <abbr>GDAL</abbr> library has not been found.
     * @throws DataStoreException if an error occurred while invoking a <abbr>GDAL</abbr> function.
     */
    static List<Driver> list(final GDALStoreProvider owner, final GDAL gdal) {
        final MemorySegment address;
        if (gdal == null || (address = gdal.symbols.find("GDALGetDriver").orElse(null)) == null) {
            return List.of();
        }
        @SuppressWarnings("restricted")
        final MethodHandle getDriver = gdal.linker.downcallHandle(address,
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        final int count = gdal.invokeGetInt("GDALGetDriverCount").orElse(0);
        return new AbstractList<Driver>() {
            /** Returns the number of drivers. */
            @Override public int size() {
                return count;
            }

            /** Fetches a driver by index. */
            @Override public Driver get(final int index) {
                final MemorySegment handle;
                try {
                    handle = (MemorySegment) getDriver.invokeExact(Objects.checkIndex(index, count));
                } catch (Throwable e) {
                    throw GDAL.propagate(e);
                }
                return GDAL.isNull(handle) ? null : new Driver(owner, handle);      // The check for null is paranoiac.
            }
        };
    }

    /**
     * Returns a string representation of this driver for debugging purposes.
     *
     * @return a string representation of this driver.
     */
    @Override
    public String toString() {
        Object[] properties;
        try {
            properties = new Object[] {
                "name",       getName(),
                "identifier", getIdentifier(),
                "MIME",       getMIMEType(),
                "suffixes",   getFileSuffixes()
            };
        } catch (DataStoreException e) {
            properties = new Object[] {
                "error", e
            };
        }
        return Strings.toString(getClass(), properties);
    }
}
