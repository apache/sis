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

import java.util.Optional;
import java.util.Collection;
import java.util.function.Predicate;
import org.opengis.geometry.Envelope;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.image.DataStoreFilter;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.DisjointExtentException;


/**
 * Static convenience methods creating {@link DataStore} instances from a given storage object.
 * Storage objects are typically {@link java.io.File} or {@link javax.sql.DataSource} instances,
 * but can also be any other objects documented in the {@link StorageConnector} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.5
 *
 * <a href="https://sis.apache.org/formats.html">Data formats supported by Apache SIS</a>
 *
 * @since 0.4
 */
public final class DataStores {
    /**
     * Do not allow instantiation of this class.
     */
    private DataStores() {
    }

    /**
     * Returns the set of available data store providers.
     * The returned collection is live: its content may change
     * if new modules are added on the module path at run-time.
     *
     * @return descriptions of available data stores.
     *
     * @since 0.8
     */
    public static Collection<DataStoreProvider> providers() {
        return DataStoreRegistry.INSTANCE;
    }

    /**
     * Returns the MIME type of the storage file format, or {@code null} if unknown or not applicable.
     *
     * @param  storage  the input/output object as a URL, file, image input stream, <i>etc.</i>.
     * @return the storage MIME type, or {@code null} if unknown or not applicable.
     * @throws DataStoreException if an error occurred while opening the storage.
     */
    public static String probeContentType(final Object storage) throws DataStoreException {
        return DataStoreRegistry.INSTANCE.probeContentType(storage);
    }

    /**
     * Creates a {@link DataStore} capable to read the given storage.
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
     * The file format is detected automatically by inspection of the file header.
     * The file suffix may also be used in case of ambiguity.
     *
     * @param  storage  the input object as a URL, file, image input stream, <i>etc.</i>.
     * @return the object to use for reading geospatial data from the given storage.
     * @throws UnsupportedStorageException if no {@link DataStoreProvider} is found for the given storage object.
     * @throws DataStoreException if an error occurred while opening the storage in read mode.
     */
    public static DataStore open(final Object storage) throws UnsupportedStorageException, DataStoreException {
        return DataStoreRegistry.INSTANCE.open(storage, Capability.READ, null);
    }

    /**
     * Creates a {@link DataStore} capable to read the given storage, with a preference for the specified reader.
     * The {@code storage} argument can be of the same types as documented in {@link #open(Object)}.
     * The {@code preferredFormat} argument can be one of the following (non-exhaustive list).
     * Note that which formats are available depend on which modules are on the module-path.
     *
     * <table class="sis">
     *   <caption>Common formats</caption>
     *   <tr><th>Format</th> <th>Description</th></tr>
     *   <tr><td>{@code "ASCII Grid"}</td>  <td>ESRI ASCII Grid raster format</td></tr>
     *   <tr><td>{@code "BIL/BIP/BSQ"}</td> <td>ESRI RAW binary encoding</td></tr>
     *   <tr><td>{@code "CSV"}</td>         <td>Comma-Separated Values, optionally with Moving Features</td></tr>
     *   <tr><td>{@code "folder"}</td>      <td>Directory of more files</td></tr>
     *   <tr><td>{@code "GDAL"}</td>        <td>Binding to the <abbr>GDAL</abbr> C/C++ library</td></tr>
     *   <tr><td>{@code "GeoTIFF"}</td>     <td>GeoTIFF, including big and <abbr>COG</abbr> variants</td></tr>
     *   <tr><td>{@code "GPX"}</td>         <td><abbr>GPS</abbr> Exchange Format</td></tr>
     *   <tr><td>{@code "Landsat"}</td>     <td>Landsat 8 level 1-2 data</td></tr>
     *   <tr><td>{@code "NetCDF"}</td>      <td>NetCDF 3 (or 4 if UCAR dependency is included)</td></tr>
     *   <tr><td>{@code "SQL"}</td>         <td>Connection to a <abbr>SQL</abbr> database</td></tr>
     *   <tr><td>{@code "WKT"}</td>         <td>CRS definition in Well-Known Text format</td></tr>
     *   <tr><td>{@code "World file"}</td>  <td>World File image read through Java Image I/O</td></tr>
     *   <tr><td>{@code "XML"}</td>         <td>Metadata in <abbr>GML</abbr> format</td></tr>
     * </table>
     *
     * The preferred format is only a hint. If the {@link DataStore} identified by {@code preferredFormat}
     * cannot open the given storage, another data store will be searched as with {@link #open(Object)}.
     * The actual format which has been selected is given by {@code DataStore.getProvider().getShortName()}.
     *
     * <h4>Example</h4>
     * If both the {@code org.apache.sis.storage.geotiff} and {@code org.apache.sis.storage.gdal} modules
     * are present on the module-path, then the Apache <abbr>SIS</abbr> implementation is used by default
     * for opening GeoTIFF files. For using <abbr>GDAL</abbr> instead, use this method with {@code "GDAL"}
     * argument value.
     *
     * @param  storage          the input object as a URL, file, image input stream, <i>etc.</i>.
     * @param  preferredFormat  identification of the preferred {@code DataStore} implementation, or {@code null}.
     * @return the object to use for reading geospatial data from the given storage.
     * @throws UnsupportedStorageException if no {@link DataStoreProvider} is found for the given storage object.
     * @throws DataStoreException if an error occurred while opening the storage in read mode.
     *
     * @see DataStore#getProvider()
     * @see DataStoreProvider#getShortName()
     *
     * @since 1.5
     */
    public static DataStore open(final Object storage, final String preferredFormat)
            throws UnsupportedStorageException, DataStoreException
    {
        Predicate<DataStoreProvider> preferred = null;
        if (preferredFormat != null) {
            preferred = new DataStoreFilter(preferredFormat, false);
        }
        return DataStoreRegistry.INSTANCE.open(storage, Capability.READ, preferred);
    }

    /**
     * Creates a {@link DataStore} capable to write or update the given storage.
     * The {@code storage} argument can be any of the types documented in {@link #open(Object)}.
     * If the storage is a file and that file does not exist, then a new file will be created.
     * If the storage exists, then it will be opened in read/write mode for updates.
     * The returned data store should implement the {@link WritableGridCoverageResource},
     * {@link WritableFeatureSet} or {@link WritableAggregate} interface.
     *
     * <h4>Format selection</h4>
     * The {@code preferredFormat} argument can be a {@linkplain DataStoreProvider#getShortName() data store name}
     * (examples: {@code "CSV"}, {@code "GPX"}) or an {@linkplain javax.imageio.ImageIO Image I/O} name
     * (examples: {@code "TIFF"}, {@code "PNG"}). In the latter case, the WorldFile convention is used.
     *
     * <p>If the given storage exists (for example, an existing file), then the {@link DataStoreProvider} is determined
     * by probing the existing content and the {@code preferredFormat} argument may be ignored (it can be {@code null}).
     * Otherwise the {@link DataStoreProvider} is selected by a combination of {@code preferredFormat} (if non-null) and
     * file suffix (if the storage is a file path or URI).</p>
     *
     * @param  storage         the input/output object as a URL, file, image input stream, <i>etc.</i>.
     * @param  preferredFormat the format to use if not determined by the existing content, or {@code null}.
     * @return the object to use for writing geospatial data in the given storage.
     * @throws UnsupportedStorageException if no {@link DataStoreProvider} is found for the given storage object.
     * @throws DataStoreException if an error occurred while opening the storage in write mode.
     *
     * @since 1.4
     */
    public static DataStore openWritable(final Object storage, final String preferredFormat)
            throws UnsupportedStorageException, DataStoreException
    {
        Predicate<DataStoreProvider> preferred = null;
        if (preferredFormat != null) {
            preferred = new DataStoreFilter(preferredFormat, true);
        }
        return DataStoreRegistry.INSTANCE.open(storage, Capability.WRITE, preferred);
    }

    /**
     * Reads immediately the first grid coverage found in the given storage.
     * This is a convenience method searching for the first instance of {@link GridCoverageResource}.
     * If the given storage contains two or more such instances, all resources after the first one are ignored.
     *
     * <p>The Area Of Interest (AOI) is an optional argument for reducing the amount of data to load.
     * It can be expressed using an arbitrary Coordinate Reference System (CRS), as transformations will be applied as needed.
     * If the AOI does not specify a CRS, then the AOI is assumed to be in the CRS of the grid coverage to read.
     * The returned grid coverage will not necessarily cover exactly the specified AOI.
     * It may be smaller if the coverage does not cover the full AOI,
     * or it may be larger for loading an integer number of tiles.</p>
     *
     * <p>On return, the grid coverage (possibly intersected with the AOI) has been fully loaded in memory.
     * If lower memory consumption is desired, for example because the coverage is very large, then deferred
     * tile loading should be used. The latter approach requires that the caller use {@link DataStore} directly,
     * because the store needs to be open as long as the {@link GridCoverage} is used.
     * See {@link RasterLoadingStrategy} for more details.</p>
     *
     * @param  storage  the input object as a URL, file, image input stream, <i>etc.</i>.
     * @param  aoi      spatiotemporal region of interest, or {@code null} for reading the whole coverage.
     * @return the first grid coverage found in the given storage, or an empty value if none was found.
     * @throws UnsupportedStorageException if no {@link DataStoreProvider} is found for the given storage object.
     * @throws DataStoreException if an error occurred while opening the storage or reading the grid coverage.
     * @throws DisjointExtentException if the given envelope does not intersect any resource extent.
     *
     * @since 1.5
     */
    public static Optional<GridCoverage> readGridCoverage(final Object storage, final Envelope aoi)
            throws UnsupportedStorageException, DataStoreException
    {
        final GridGeometry domain = (aoi == null) ? null : new GridGeometry(aoi);
        try (DataStore ds = open(storage)) {
            final GridCoverageResource gc;
search:     if (ds instanceof GridCoverageResource) {
                gc = (GridCoverageResource) ds;
            } else {
                if (ds instanceof Aggregate) {
                    for (final Resource r : ((Aggregate) ds).components()) {
                        if (r instanceof GridCoverageResource) {
                            gc = (GridCoverageResource) r;
                            break search;
                        }
                    }
                }
                return Optional.empty();
            }
            return Optional.of(gc.read(domain, null));
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(DataStoreException.class);
        }
    }
}
