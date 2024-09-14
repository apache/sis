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

import java.util.Set;
import java.util.Locale;
import java.net.URI;
import java.nio.file.Path;
import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.MemorySegment;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.storage.DataStoreException;


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
     * Pointer to the <abbr>GDAL</abbr> object in native memory.
     * This is a {@code GDALDatasetH} in the C/C++ <abbr>API</abbr>.
     */
    final MemorySegment handle;

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
     * @param  url             <abbr>URL</abbr> for <var>GDAL</var> of the data store to open.
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
        final GDAL gdal = owner.GDAL();
        try (var arena = Arena.ofConfined()) {
            handle = (MemorySegment) gdal.open.invokeExact(
                    arena.allocateFrom(url),
                    OpenFlag.mask(openFlags),
                    allocateStringArray(arena, allowedDrivers),
                    allocateStringArray(arena, driverOptions),
                    allocateStringArray(arena, siblingFiles));
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        if (GDAL.isNull(handle)) {
            throw new DataStoreException();     // TODO: get message from GDAL.
        }
    }

    /**
     * Creates a new instance for read operations on the given file or <abbr>URL</abbr>.
     *
     * @param  owner           owner of the set of handles for invoking <abbr>GDAL</abbr> functions.
     * @param  url             <abbr>URL</abbr> for <var>GDAL</var> of the data store to open.
     * @param  allowedDrivers  short names (identifiers) of drivers that may be used, or {@code null} for any driver.
     * @throws DataStoreException if <var>GDAL</var> cannot open the data set.
     */
    static Opener read(final GDALStoreProvider owner, final String url, final String... allowedDrivers)
            throws DataStoreException
    {
        return new Opener(owner, url, new OpenFlag[] {OpenFlag.RASTER, OpenFlag.SHARED}, allowedDrivers, null, null);
    }

    /**
     * Infers a <var>GDAL</var> <abbr>URL</abbr> from the given location and/or path.
     *
     * @param  location  URL to the file to open (mandatory).
     * @param  path      URL as a path on the file system, or {@code null} if none.
     * @return <abbr>URL</abbr> for <var>GDAL</var>.
     */
    static String toURL(final URI location, final Path path) {
        String url;
        final String scheme = location.getScheme();
        if (path != null && "file".equalsIgnoreCase(scheme)) {
            url = path.toString();
        } else {
            url = location.toString();
            if (scheme != null && VSICURL.contains(scheme.toLowerCase(Locale.US))) {
                url = "/vsicurl/".concat(url);
            }
        }
        return url;
    }

    /**
     * Allocates memory for an array of strings.
     *
     * @param  arena  the arena to allocate memory from.
     * @param  values the array of strings to allocate memory for.
     * @return the array of pointer, or {@link MemorySegment#NULL} if the array is {@code null} or empty.
     */
    private static MemorySegment allocateStringArray(final Arena arena, final String[] values) {
        if (values == null || values.length == 0) {
            return MemorySegment.NULL;
        }
        final MemorySegment array = arena.allocate(ValueLayout.ADDRESS, values.length);
        for (int i=0; i < values.length; i++) {
            array.setAtIndex(ValueLayout.ADDRESS, i, arena.allocateFrom(values[i]));
        }
        return array;
    }

    /**
     * Invoked by {@link java.lang.ref.Cleaner} when the native resource is ready to be closed.
     * This method shall not be invoked explicitly. The {@code Cleaner} <abbr>API</abbr> ensures
     * that this method will be invoked exactly once.
     */
    @Override
    public void run() {
        owner.tryGDAL("close").ifPresent((gdal) -> {
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
    }
}
