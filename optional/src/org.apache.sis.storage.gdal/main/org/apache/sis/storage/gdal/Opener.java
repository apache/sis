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

import java.util.Arrays;
import java.util.Set;
import java.util.Locale;
import java.net.URI;
import java.nio.file.Path;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.resources.Errors;


/**
 * Opens a <abbr>GDAL</abbr> {@code GDALStore} and provides an action for closing it.
 * The close action is a safety in case the user does not call {@code close()} explicitly.
 * In the latter case, the {@code GDALStore} is closed when the {@link GDALStore} is garbage-collected.
 *
 * <h2>Constraint</h2>
 * This class shall not contain any reference to {@link GDALStore}, including indirect references such as
 * {@link org.apache.sis.storage.event.StoreListeners}. This is required by {@link java.lang.ref.Cleaner}.
 *
 * @author  Quentin Bialota (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Opener implements Runnable {
    /**
     * Schemes of URLs to open with <var>GDAL</var> Virtual File Systems backed by <abbr>CURL</abbr>.
     */
    private static final Set<String> VSICURL = Set.of(Constants.HTTP, Constants.HTTPS, "ftp", "ftps");

    /**
     * Owner of the set of handles that this class will use for invoking <abbr>GDAL</abbr> functions.
     * Another purpose of this reference is to prevent premature garbage-collection,
     * because it would unload the <abbr>GDAL</abbr> library.
     */
    private final GDALStoreProvider owner;

    /**
     * Pointer to the <abbr>GDAL</abbr> object in native memory, or {@code null} if the file couldn't be opened.
     * This is a {@code GDALDatasetH} in the C/C++ <abbr>API</abbr>.
     */
    final MemorySegment handle;

    /**
     * Arenas which will need to be closed when the {@link GDALStore} is garbage-collected.
     * This is a safety in case the user forgot to close the feature streams.
     * May contain null elements.
     */
    Arena[] arenasToClose;

    /**
     * Creates a new instance for read operations on the given file or <abbr>URL</abbr>.
     *
     * @param  owner           owner of the set of handles for invoking <abbr>GDAL</abbr> functions.
     * @param  url             <abbr>URL</abbr> (<var>GDAL</var> syntax) of the data store to open.
     * @param  allowedDrivers  short names (identifiers) of drivers that may be used, or {@code null} for any driver.
     * @throws DataStoreException if <var>GDAL</var> cannot open the data set.
     */
    Opener(final GDALStoreProvider owner, final String url, final String... allowedDrivers) throws DataStoreException {
        this(owner, url, new OpenFlag[] {OpenFlag.RASTER, OpenFlag.VECTOR, OpenFlag.SHARED}, allowedDrivers, null, null);
    }

    /**
     * Creates a new instance for the given file or <abbr>URL</abbr>.
     * The {@code options} argument is driver-specific, except for the
     * following values that are supported by all <abbr>GDAL</abbr> drivers:
     *
     * <table class="sis">
     *   <caption>Options supported by all <abbr>GDAL</abbr> drivers</caption>
     *   <tr><th>Option</th>                           <th>Description</th></tr>
     *   <tr><td>{@code OVERVIEW_LEVEL=n}</td>         <td>Start the pyramid at the given level where <var>n</var> ≥ 0.</td></tr>
     *   <tr><td>{@code VALIDATE_OPEN_OPTIONS=NO}</td> <td>Do not emit a warning if an option is not recognized.</td></tr>
     * </table>
     *
     * @param  owner           owner of the set of handles for invoking <abbr>GDAL</abbr> functions.
     * @param  url             <abbr>URL</abbr> (<var>GDAL</var> syntax) of the data store to open.
     * @param  openFlags       open flags to give to <abbr>GDAL</abbr>.
     * @param  allowedDrivers  short names (identifiers) of drivers that may be used, or {@code null} for any driver.
     * @param  driverOptions   driver-dependent options, or {@code null} if none.
     * @param  siblingFiles    names of auxiliary files in the directory of the main file, or {@code null} for automatic.
     * @throws DataStoreException if <var>GDAL</var> cannot open the data set.
     */
    Opener(final GDALStoreProvider owner,
           final String     url,
           final OpenFlag[] openFlags,
           final String[]   allowedDrivers,
           final String[]   driverOptions,
           final String[]   siblingFiles) throws DataStoreException
    {
        this.owner = owner;
        arenasToClose = new Arena[4];
        final GDAL gdal = owner.GDAL();
        try (var arena = Arena.ofConfined()) {
            handle = (MemorySegment) gdal.open.invokeExact(
                    arena.allocateFrom(url),
                    OpenFlag.mask(openFlags),
                    GDAL.toNullTerminatedStrings(arena, allowedDrivers),
                    GDAL.toNullTerminatedStrings(arena, driverOptions),
                    GDAL.toNullTerminatedStrings(arena, siblingFiles));
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        if (GDAL.isNull(handle)) {
            ErrorHandler.throwOnFailure(null, "open");
            throw new DataStoreException(Errors.format(Errors.Keys.CanNotOpen_1, url));
        }
    }

    /**
     * Infers a <var>GDAL</var> <abbr>URL</abbr> from the given location and/or path.
     *
     * @param  location  URL to the file to open (mandatory).
     * @param  path      URL as a path on the file system, or {@code null} if none.
     * @param  fallback  whether to use a fallback value if the URI is not recognized.
     * @return <abbr>URL</abbr> for <var>GDAL</var>. May be {@code null} if unrecognized and no fallback is used.
     */
    static String toURL(final URI location, final Path path, final boolean fallback) {
        String url;
        final String scheme = location.getScheme();
        if (path != null && "file".equalsIgnoreCase(scheme)) {
            url = path.toString();
        } else {
            url = location.toString();
            if (scheme != null && VSICURL.contains(scheme.toLowerCase(Locale.US))) {
                url = "/vsicurl/".concat(url);
            } else if (!fallback) {
                return null;
            }
        }
        return url;
    }

    /**
     * Returns the MIME type if the given storage appears to be supported by this data store.
     *
     * @param  owner      the provider which is probing the file.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @return a support status with the MIME type, or {@code null} if the given URL is unrecognized.
     * @throws DataStoreException if an error occurred while invoking a <abbr>GDAL</abbr> function.
     */
    static ProbeResult probeContent(final GDALStoreProvider owner, final StorageConnector connector)
            throws DataStoreException
    {
        String url;
        final URI location = connector.getStorageAs(URI.class);
        if (location != null) {
            url = toURL(location, connector.getStorageAs(Path.class), true);
        } else {
            url = connector.getStorageAs(String.class);
        }
        if (url != null) {
            final GDAL gdal = owner.tryGDAL(GDALStoreProvider.class, "probeContent").orElse(null);
            if (gdal != null) {
                try (var arena = Arena.ofConfined()) {
                    var strPtr = arena.allocateFrom(url);
                    var driver = (MemorySegment) gdal.identifyDriver.invokeExact(strPtr, MemorySegment.NULL);
                    if (!GDAL.isNull(driver)) {
                        strPtr = arena.allocateFrom("DMD_MIMETYPE");
                        var mimeType = (MemorySegment) gdal.getMetadataItem.invokeExact(driver, strPtr, MemorySegment.NULL);
                        return new ProbeResult(true, GDAL.toString(mimeType), null);
                    }
                } catch (Throwable e) {
                    throw GDAL.propagate(e);
                } finally {
                    ErrorHandler.throwOnFailure(null, "probeContent");
                }
            }
        }
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

    /**
     * Invoked by {@link java.lang.ref.Cleaner} when the native resource is ready to be closed.
     * This method shall not be invoked explicitly. The {@code Cleaner} <abbr>API</abbr> ensures
     * that this method will be invoked exactly once.
     */
    @Override
    public void run() {
        RuntimeException error = null;
        try {
            /*
             * Closes the `GDALDatasetH`. This operation may be skipped if the GDAL library has been unloaded.
             * It may be because `GDALStoreProvider` has been garbage-collected, or because a GDAL fatal error
             * occurred.
             */
            owner.tryGDAL(GDALStore.class, "close").ifPresent((gdal) -> {
                final int err;
                try {
                    err = (int) gdal.close.invokeExact(handle);
                } catch (Throwable e) {
                    throw GDAL.propagate(e);
                }
                if (err != 0) {
                    ErrorHandler.errorOccurred(err);
                }
            });
        } catch (RuntimeException e) {
            error = e;
        } finally {
            /*
             * Closes temporary arenas that may have been created by `FeatureIterator`.
             * This is a safety in case the user forgot to invoke `Stream.close()`.
             */
            for (Arena arena : arenasToClose) {
                if (arena != null) try {
                    arena.close();
                } catch (RuntimeException e) {
                    if (error == null) error = e;
                    else error.addSuppressed(e);
                }
            }
            Arrays.fill(arenasToClose, null);
        }
        if (error != null) {
            throw error;
        }
    }
}
