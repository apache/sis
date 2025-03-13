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
package org.apache.sis.storage.image;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.io.Closeable;
import java.io.IOException;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardOpenOption;
import java.net.URISyntaxException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.maintenance.ScopeCode;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreClosedException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.ReadOnlyStorageException;
import org.apache.sis.storage.UnsupportedStorageException;
import org.apache.sis.storage.base.PRJDataStore;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.base.AuxiliaryContent;
import org.apache.sis.referencing.privy.AffineTransform2D;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.privy.ListOfUnknownSize;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.setup.OptionKey;


/**
 * A data store which creates grid coverages from Image I/O readers using <i>World File</i> convention.
 * Georeferencing is defined by two auxiliary files having the same name as the image file but different suffixes:
 *
 * <ul class="verbose">
 *   <li>A text file containing the coefficients of the affine transform mapping pixel coordinates to geodesic coordinates.
 *     The reader expects one coefficient per line, in the same order as the order expected by the
 *     {@link java.awt.geom.AffineTransform#AffineTransform(double[]) AffineTransform(double[])} constructor, which is
 *     <var>scaleX</var>, <var>shearY</var>, <var>shearX</var>, <var>scaleY</var>, <var>translateX</var>, <var>translateY</var>.
 *     The reader looks for a file having the following suffixes, in preference order:
 *     <ol>
 *       <li>The first letter of the image file extension, followed by the last letter of
 *         the image file extension, followed by {@code 'w'}. Example: {@code "tfw"} for
 *         {@code "tiff"} images, and {@code "jgw"} for {@code "jpeg"} images.</li>
 *       <li>The extension of the image file with a {@code 'w'} appended.</li>
 *       <li>The {@code "wld"} extension.</li>
 *     </ol>
 *   </li>
 *   <li>A text file containing the <i>Coordinate Reference System</i> (CRS) definition
 *     in <i>Well Known Text</i> (WKT) syntax.
 *     The reader looks for a file having the {@code ".prj"} extension.</li>
 * </ul>
 *
 * Every auxiliary text file are expected to be encoded in UTF-8
 * and every numbers are expected to be formatted in US locale.
 *
 * <h2>Type of input objects</h2>
 * The {@link StorageConnector} input should be an instance of the following types:
 * {@link java.nio.file.Path}, {@link java.io.File}, {@link java.net.URL} or {@link java.net.URI}.
 * Other types such as {@link ImageInputStream} are also accepted but in those cases the auxiliary files cannot be read.
 * For any input of unknown type, this data store first checks if an {@link ImageReader} accepts the input type directly.
 * If none is found, this data store tries to {@linkplain ImageIO#createImageInputStream(Object) create an input stream}
 * from the input object.
 *
 * <p>The storage input object may also be an {@link ImageReader} instance ready for use
 * (i.e. with its {@linkplain ImageReader#setInput(Object) input set} to a non-null value).
 * In that case, this data store will use the given image reader as-is.
 * The image reader will be {@linkplain ImageReader#dispose() disposed}
 * and its input closed (if {@link AutoCloseable}) when this data store is {@linkplain #close() closed}.</p>
 *
 * <h2>Handling of multi-image files</h2>
 * Because some image formats can store an arbitrary number of images,
 * this data store is considered as an aggregate with one resource per image.
 * All image should have the same size and all resources will share the same {@link GridGeometry}.
 * However, this base class does not implement the {@link Aggregate} interface directly in order to
 * give a chance to subclasses to implement {@link GridCoverageResource} directly when the format
 * is known to support only one image per file.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class WorldFileStore extends PRJDataStore {
    /**
     * Image I/O format names (ignoring case) for which we have an entry in the {@code SpatialMetadata} database.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-300">SIS-300 — Complete the information provided in Citations constants</a>
     */
    private static final String[] KNOWN_FORMATS = {
        "PNG"
    };

    /**
     * Index of the main image. This is relevant only with formats capable to store an arbitrary number of images.
     * Current implementation assumes that the main image is always the first one, but it may become configurable
     * in a future version if useful.
     *
     * @see #width
     * @see #height
     */
    static final int MAIN_IMAGE = 0;

    /**
     * The default World File suffix when it cannot be determined from {@link #location}.
     * This is a GDAL convention.
     */
    private static final String DEFAULT_SUFFIX = "wld";

    /**
     * The "cell center" versus "cell corner" interpretation of translation coefficients.
     * The ESRI specification said that the coefficients map to pixel center.
     */
    static final PixelInCell CELL_ANCHOR = PixelInCell.CELL_CENTER;

    /**
     * The filename extension (may be an empty string), or {@code null} if unknown.
     * It does not include the leading dot.
     */
    final String suffix;

    /**
     * The filename extension for the auxiliary "world file".
     * For the TIFF format, this is typically {@code "tfw"}.
     * This is computed as a side-effect of {@link #readWorldFile()}.
     */
    private String suffixWLD;

    /**
     * The image reader, set by the constructor and cleared when the store is closed.
     * May also be null if the store is initially write-only, in which case a reader
     * may be created the first time than an image is read.
     *
     * @see #reader()
     */
    private volatile ImageReader reader;

    /**
     * The object to close when {@code WorldFileStore} is closed. It may be a different object than
     * reader input or writer output, because some {@link ImageInputStream#close()} implementations
     * in the standard Java {@link javax.imageio.stream} package do not close the underlying stream.
     *
     * <p>The type is {@link Closeable} instead of {@link AutoCloseable} because the former is idempotent:
     * invoking {@link Closeable#close()} many times has no effect. By contrast {@link AutoCloseable} does
     * not offer this guarantee. Because it is hard to know what {@link ImageInputStream#close()} will do,
     * we need idempotent {@code toClose} for safety. Note that {@link ImageInputStream#close()} violates
     * the idempotent contract of {@link Closeable#close()}, so an additional check will be necessary in
     * our {@link #close()} implementation.</p>
     *
     * @see javax.imageio.stream.FileCacheImageInputStream#close()
     * @see javax.imageio.stream.FileCacheImageOutputStream#close()
     * @see javax.imageio.stream.MemoryCacheImageInputStream#close()
     * @see javax.imageio.stream.MemoryCacheImageOutputStream#close()
     */
    private Closeable toClose;

    /**
     * Width and height of the main image.
     * The {@link #gridGeometry} is assumed valid only for images having this size.
     *
     * @see #MAIN_IMAGE
     * @see #gridGeometry
     */
    private int width, height;

    /**
     * The conversion from pixel center to CRS, or {@code null} if none or not yet computed.
     * The grid extent has the size given by {@link #width} and {@link #height}.
     *
     * @see #crs
     * @see #width
     * @see #height
     * @see #getGridGeometry(int)
     */
    private GridGeometry gridGeometry;

    /**
     * All images in this resource, created when first needed.
     * Elements in this list will also be created when first needed.
     *
     * @see #components()
     */
    private Components components;

    /**
     * The metadata object, or {@code null} if not yet created.
     *
     * @see #getMetadata()
     */
    private Metadata metadata;

    /**
     * Identifiers used by a resource. Identifiers must be unique in the data store,
     * so after an identifier has been used it cannot be reused anymore even if the
     * resource having that identifier has been removed.
     * Values associated to identifiers tell whether the resource still exist.
     *
     * @see WorldFileResource#getIdentifier()
     */
    final Map<String,Boolean> identifiers;

    /**
     * Creates a new store from the given file, URL or stream.
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if an error occurred while opening the stream.
     * @throws IOException if an error occurred while creating the image reader instance.
     */
    public WorldFileStore(final WorldFileStoreProvider provider, final StorageConnector connector)
            throws DataStoreException, IOException
    {
        this(new FormatFinder(provider, connector), true);
    }

    /**
     * Creates a new store from the given file, URL or stream.
     *
     * @param  format    information about the storage (URL, stream, <i>etc</i>) and the reader/writer to use.
     * @param  readOnly  {@code true} if the store should be open in read-only mode, ignoring {@code format}.
     *                   This is a workaround while waiting for JEP 447: Statements before super(…).
     * @throws DataStoreException if an error occurred while opening the stream.
     * @throws IOException if an error occurred while creating the image reader instance.
     */
    WorldFileStore(final FormatFinder format, final boolean readOnly) throws DataStoreException, IOException {
        super(format.provider, format.connector);
        listeners.useReadOnlyEvents();
        identifiers = new HashMap<>();
        suffix = format.suffix;
        if (format.storage instanceof Closeable) {
            toClose = (Closeable) format.storage;
        }
        if (readOnly || !format.openAsWriter) {
            reader = format.getOrCreateReader();
            if (reader == null) {
                throw new UnsupportedStorageException(super.getLocale(), WorldFileStoreProvider.NAME,
                            format.storage, format.connector.getOption(OptionKey.OPEN_OPTIONS));
            }
            configureReader();
            if (readOnly) {
                format.close();
            }
            /*
             * Do not invoke any method that may cause the image reader to start reading the stream,
             * because the `WritableStore` subclass will want to save the initial stream position.
             */
        }
    }

    /**
     * Sets the locale to use for warning messages, if supported. If the reader
     * does not support the locale, the reader's default locale will be used.
     */
    private void configureReader() {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final ImageReader reader = this.reader;
        try {
            reader.setLocale(listeners.getLocale());
        } catch (IllegalArgumentException e) {
            // Ignore
        }
        reader.addIIOReadWarningListener(new WarningListener(listeners));
    }

    /**
     * Returns the preferred suffix for the auxiliary world file. For TIFF images, this is {@code "tfw"}.
     * This method tries to use the same case (lower-case or upper-case) than the suffix of the main file.
     */
    private String getWorldFileSuffix() {
        if (suffix != null) {
            final int length = suffix.length();
            if (suffix.codePointCount(0, length) >= 2) {
                boolean lower = true;
                for (int i = length; i > 0;) {
                    final int c = suffix.codePointBefore(i);
                    lower =  Character.isLowerCase(c); if ( lower) break;
                    lower = !Character.isUpperCase(c); if (!lower) break;
                    i -= Character.charCount(c);
                }
                // If the case cannot be determined, `lower` will default to `true`.
                return new StringBuilder(3)
                        .appendCodePoint(suffix.codePointAt(0))
                        .appendCodePoint(suffix.codePointBefore(length))
                        .append(lower ? 'w' : 'W').toString();
            }
        }
        return DEFAULT_SUFFIX;
    }

    /**
     * Reads the "World file" by searching for an auxiliary file with a suffix inferred from
     * the suffix of the main file. This method tries suffixes with the following conventions,
     * in preference order.
     *
     * <ol>
     *   <li>First letter of main file suffix, followed by last letter, followed by {@code 'w'}.</li>
     *   <li>Full suffix of the main file followed by {@code 'w'}.</li>
     *   <li>{@value #DEFAULT_SUFFIX}.</li>
     * </ol>
     *
     * @return the "World file" content as an affine transform, or {@code null} if none was found.
     * @throws URISyntaxException if an error occurred while normalizing the URI.
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the auxiliary file content cannot be parsed.
     */
    private AffineTransform2D readWorldFile() throws URISyntaxException, IOException, DataStoreException {
        IOException warning = null;
        final String preferred = getWorldFileSuffix();
loop:   for (int convention=0;; convention++) {
            final String wld;
            switch (convention) {
                default: break loop;
                case 0:  wld = preferred;      break;       // First file suffix to search.
                case 2:  wld = DEFAULT_SUFFIX; break;       // File suffix to search in last resort.
                case 1: {
                    if (preferred.equals(DEFAULT_SUFFIX)) break loop;
                    wld = suffix + preferred.charAt(preferred.length() - 1);
                    break;
                }
            }
            try {
                return readWorldFile(wld);
            } catch (NoSuchFileException | FileNotFoundException e) {
                if (warning == null) {
                    warning = e;
                } else {
                    warning.addSuppressed(e);
                }
            }
        }
        if (warning != null) {
            cannotReadAuxiliaryFile(WorldFileStore.class, "getGridGeometry", preferred, warning, true);
        }
        return null;
    }

    /**
     * Reads the "World file" by parsing an auxiliary file with the given suffix.
     *
     * @param  wld  suffix of the auxiliary file.
     * @return the "World file" content as an affine transform, or {@code null} if none was found.
     * @throws URISyntaxException if an error occurred while normalizing the URI.
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the file content cannot be parsed.
     */
    private AffineTransform2D readWorldFile(final String wld)
            throws URISyntaxException, IOException, DataStoreException
    {
        final AuxiliaryContent content = readAuxiliaryFile(wld, false);
        if (content == null) {
            cannotReadAuxiliaryFile(WorldFileStore.class, "getGridGeometry", wld, null, true);
            return null;
        }
        final String         filename = content.getFilename();
        final CharSequence[] lines    = CharSequences.splitOnEOL(content);
        final int            expected = 6;        // Expected number of elements.
        int                  count    = 0;        // Actual number of elements.
        final double[]       elements = new double[expected];
        for (int i=0; i<expected; i++) {
            final String line = lines[i].toString().trim();
            if (!line.isEmpty() && line.charAt(0) != '#') {
                if (count >= expected) {
                    throw new DataStoreContentException(errors().getString(Errors.Keys.TooManyOccurrences_2, expected, "coefficient"));
                }
                try {
                    elements[count++] = Double.parseDouble(line);
                } catch (NumberFormatException e) {
                    throw new DataStoreContentException(errors().getString(Errors.Keys.ErrorInFileAtLine_2, filename, i), e);
                }
            }
        }
        if (count != expected) {
            throw new EOFException(errors().getString(Errors.Keys.UnexpectedEndOfFile_1, filename));
        }
        if (filename != null) {
            final int s = filename.lastIndexOf(IOUtilities.EXTENSION_SEPARATOR);
            if (s >= 0) {
                suffixWLD = filename.substring(s+1);
            }
        }
        return new AffineTransform2D(elements);
    }

    /**
     * Returns the localized resources for producing error messages.
     */
    private Errors errors() {
        return Errors.forLocale(getLocale());
    }

    /**
     * Returns the Image I/O format names or MIME types of the image read by this data store.
     * More than one names may be returned if the format has aliases or if the MIME type
     * has legacy types (e.g. official {@code "image/png"} and legacy {@code "image/x-png"}).
     *
     * @param  asMimeType  {@code true} for MIME types, or {@code false} for format names.
     * @return the requested names, or an empty array if none or unknown.
     */
    public String[] getImageFormat(final boolean asMimeType) {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final ImageReader reader = this.reader;
        if (reader != null) {
            final ImageReaderSpi p = reader.getOriginatingProvider();
            if (p != null) {
                final String[] names = asMimeType ? p.getMIMETypes() : p.getFormatNames();
                if (names != null) {
                    return names;
                }
            }
        }
        return CharSequences.EMPTY_ARRAY;
    }

    /**
     * Returns paths to the main file together with auxiliary files.
     *
     * @return paths to the main file and auxiliary files, or an empty value if unknown.
     * @throws DataStoreException if the URI cannot be converted to a {@link Path}.
     */
    @Override
    public synchronized Optional<FileSet> getFileSet() throws DataStoreException {
        if (suffixWLD == null) try {
            getGridGeometry(MAIN_IMAGE);                // Will compute `suffixWLD` as a side effect.
        } catch (URISyntaxException | IOException e) {
            throw new DataStoreException(e);
        }
        return listComponentFiles(suffixWLD, PRJ);      // `suffixWLD` still null if file was not found.
    }

    /**
     * Gets the grid geometry for image at the given index.
     * This method should be invoked only once per image, and the result cached.
     *
     * @param  index  index of the image for which to read the grid geometry.
     * @return grid geometry of the image at the given index.
     * @throws IndexOutOfBoundsException if the image index is out of bounds.
     * @throws URISyntaxException if an error occurred while normalizing the URI.
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the {@code *.prj} or {@code *.tfw} auxiliary file content cannot be parsed.
     */
    final GridGeometry getGridGeometry(final int index) throws URISyntaxException, IOException, DataStoreException {
        assert Thread.holdsLock(this);
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final ImageReader reader = reader();
        if (gridGeometry == null) {
            final AffineTransform2D gridToCRS;
            width     = reader.getWidth (MAIN_IMAGE);
            height    = reader.getHeight(MAIN_IMAGE);
            gridToCRS = readWorldFile();
            readPRJ(WorldFileStore.class, "getGridGeometry");
            gridGeometry = new GridGeometry(new GridExtent(width, height), CELL_ANCHOR, gridToCRS, crs);
        }
        if (index != MAIN_IMAGE) {
            final int w = reader.getWidth (index);
            final int h = reader.getHeight(index);
            if (w != width || h != height) {
                // Cannot use `gridToCRS` and `crs` because they may not apply.
                return new GridGeometry(new GridExtent(w, h), CELL_ANCHOR, null, null);
            }
        }
        return gridGeometry;
    }

    /**
     * Sets the store-wide grid geometry when a new coverage is written. The {@link WritableStore} implementation
     * is responsible for making sure that the new grid geometry is compatible with preexisting grid geometry.
     *
     * @param  index  index of the image for which to set the grid geometry.
     * @param  gg     the new grid geometry.
     * @return suffix of the "world file", or {@code null} if the image cannot be written.
     */
    String setGridGeometry(final int index, final GridGeometry gg)
            throws URISyntaxException, IOException, DataStoreException
    {
        if (index != MAIN_IMAGE) {
            return null;
        }
        final GridExtent extent = gg.getExtent();
        final int w = Math.toIntExact(extent.getSize(WorldFileResource.X_DIMENSION));
        final int h = Math.toIntExact(extent.getSize(WorldFileResource.Y_DIMENSION));
        final String s = (suffixWLD != null) ? suffixWLD : getWorldFileSuffix();
        crs = gg.isDefined(GridGeometry.CRS) ? gg.getCoordinateReferenceSystem() : null;
        gridGeometry = gg;                  // Set only after success of all the above.
        width        = w;
        height       = h;
        suffixWLD    = s;
        return s;
    }

    /**
     * Returns information about the data store as a whole.
     */
    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) try {
            final var builder = new MetadataBuilder();
            String format = reader().getFormatName();
            for (final String key : KNOWN_FORMATS) {
                if (key.equalsIgnoreCase(format)) {
                    if (builder.setPredefinedFormat(key, listeners, false)) {
                        format = null;
                    }
                    break;
                }
            }
            builder.addFormatName(format);      // Does nothing if `format` is null.
            builder.addFormatReaderSIS(WorldFileStoreProvider.NAME);
            builder.addResourceScope(ScopeCode.COVERAGE, null);
            builder.addSpatialRepresentation(null, getGridGeometry(MAIN_IMAGE), true);
            if (gridGeometry.isDefined(GridGeometry.ENVELOPE)) {
                builder.addExtent(gridGeometry.getEnvelope(), listeners);
            }
            mergeAuxiliaryMetadata(WorldFileStore.class, builder);
            builder.addTitleOrIdentifier(getFilename(), MetadataBuilder.Scope.ALL);
            builder.setISOStandards(false);
            metadata = builder.buildAndFreeze();
        } catch (URISyntaxException | IOException e) {
            throw new DataStoreException(e);
        }
        return metadata;
    }

    /**
     * Returns all images in this store. Note that fetching the size of the list is a potentially costly operation.
     *
     * @return list of images in this store.
     * @throws DataStoreException if an error occurred while fetching components.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public synchronized Collection<? extends GridCoverageResource> components() throws DataStoreException {
        if (components == null) try {
            components = new Components(reader().getNumImages(false));
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
        return components;
    }

    /**
     * Returns all images in this store, or {@code null} if none and {@code create} is false.
     *
     * @param  create     whether to create the component list if it was not already created.
     * @param  numImages  number of images, or any negative value if unknown.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final Components components(final boolean create, final int numImages) {
        if (components == null && create) {
            components = new Components(numImages);
        }
        return components;
    }

    /**
     * A list of images where each {@link WorldFileResource} instance is initialized when first needed.
     * Fetching the list size may be a costly operation and will be done only if requested.
     */
    final class Components extends ListOfUnknownSize<WorldFileResource> {
        /**
         * Size of this list, or any negative value if unknown.
         */
        private int size;

        /**
         * All elements in this list. Some array elements may be {@code null} if the image
         * has never been requested.
         */
        private WorldFileResource[] images;

        /**
         * Creates a new list of images.
         *
         * @param  numImages  number of images, or any negative value if unknown.
         */
        private Components(final int numImages) {
            size = numImages;
            images = new WorldFileResource[Math.max(numImages, 1)];
        }

        /**
         * Returns the number of images in this list.
         * This method may be costly when invoked for the first time.
         */
        @Override
        public int size() {
            synchronized (WorldFileStore.this) {
                if (size < 0) try {
                    size   = reader().getNumImages(true);
                    images = ArraysExt.resize(images, size);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } catch (DataStoreException e) {
                    throw new BackingStoreException(e);
                }
                return size;
            }
        }

        /**
         * Returns the number of images if this information is known, or any negative value otherwise.
         * This is used by {@link ListOfUnknownSize} for optimizing some operations.
         */
        @Override
        protected int sizeIfKnown() {
            synchronized (WorldFileStore.this) {
                return size;
            }
        }

        /**
         * Returns {@code true} if an element exists at the given index.
         * Current implementations is not more efficient than {@link #get(int)}.
         */
        @Override
        protected boolean exists(final int index) {
            synchronized (WorldFileStore.this) {
                if (size >= 0) {
                    return index >= 0 && index < size;
                }
                try {
                    return get(index) != null;
                } catch (IndexOutOfBoundsException e) {
                    return false;
                }
            }
        }

        /**
         * Returns the image at the given index. New instances are created when first requested.
         *
         * @param  index  index of the image for which to get a resource.
         * @return resource for the image identified by the given index.
         * @throws IndexOutOfBoundsException if the image index is out of bounds.
         */
        @Override
        public WorldFileResource get(final int index) {
            synchronized (WorldFileStore.this) {
                WorldFileResource image = null;
                if (index < images.length) {
                    image = images[index];
                }
                if (image == null) try {
                    image = createImageResource(index);
                    if (index >= images.length) {
                        images = Arrays.copyOf(images, Math.max(images.length * 2, index + 1));
                    }
                    images[index] = image;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } catch (URISyntaxException | DataStoreException e) {
                    throw new BackingStoreException(e);
                }
                return image;
            }
        }

        /**
         * Invoked <em>after</em> an image has been added to the image file.
         * This method adds in this list a reference to the newly added file.
         *
         * @param  image  the image to add to this list.
         */
        final void added(final WorldFileResource image) {
            size = image.getImageIndex();
            if (size >= images.length) {
                images = Arrays.copyOf(images, size * 2);
            }
            images[size++] = image;
        }

        /**
         * Invoked <em>after</em> an image has been removed from the image file.
         * This method performs no bounds check (it must be done by the caller).
         *
         * @param  index  index of the image that has been removed.
         */
        final void removed(int index) throws DataStoreException {
            final int last = images.length - 1;
            System.arraycopy(images, index+1, images, index, last - index);
            images[last] = null;
            size--;
            while (index < last) {
                final WorldFileResource image = images[index++];
                if (image != null) image.decrementImageIndex();
            }
        }

        /**
         * Removes the element at the specified position in this list.
         */
        @Override
        public WorldFileResource remove(final int index) {
            final WorldFileResource image = get(index);
            try {
                WorldFileStore.this.remove(image);
            } catch (DataStoreException e) {
                throw new UnsupportedOperationException(e);
            }
            return image;
        }
    }

    /**
     * Invoked by {@link Components} when the caller want to remove a resource.
     * The actual implementation is provided by {@link WritableStore}.
     */
    void remove(final Resource resource) throws DataStoreException {
        throw new ReadOnlyStorageException();
    }

    /**
     * Creates a {@link GridCoverageResource} for the specified image.
     * This method is invoked by {@link Components} when first needed
     * and the result is cached by the caller.
     *
     * @param  index  index of the image for which to create a resource.
     * @return resource for the image identified by the given index.
     * @throws IndexOutOfBoundsException if the image index is out of bounds.
     * @throws URISyntaxException if an error occurred while normalizing the URI.
     */
    WorldFileResource createImageResource(final int index)
            throws DataStoreException, URISyntaxException, IOException
    {
        return new WorldFileResource(this, listeners, index, getGridGeometry(index));
    }

    /**
     * Whether the component of this data store is used only as a delegate.
     * This is {@code false} when the components will be given to the user,
     * or {@code true} if the singleton component will be used only for internal purposes.
     */
    boolean isComponentHidden() {
        return false;
    }

    /**
     * Prepares an image reader compatible with the writer and sets its input.
     * This method is invoked for switching from write mode to read mode.
     * Its actual implementation is provided by {@link WritableResource}.
     *
     * @param  current  the current image reader, or {@code null} if none.
     * @return the image reader to use, or {@code null} if none.
     * @throws IOException if an error occurred while preparing the reader.
     */
    ImageReader prepareReader(ImageReader current) throws IOException {
        return null;
    }

    /**
     * Returns the reader without doing any validation. The reader may be {@code null} either
     * because the store is closed or because the store is initially opened in write-only mode.
     * The reader may have a {@code null} input.
     */
    final ImageReader getCurrentReader() {
        return reader;
    }

    /**
     * Returns the reader if it has not been closed.
     *
     * @throws DataStoreClosedException if this data store is closed.
     * @throws IOException if an error occurred while preparing the reader.
     */
    final ImageReader reader() throws DataStoreException, IOException {
        assert Thread.holdsLock(this);
        ImageReader current = reader;
        if (current == null || current.getInput() == null) {
            reader = current = prepareReader(current);
            if (current == null) {
                throw new DataStoreClosedException(getLocale(), WorldFileStoreProvider.NAME, StandardOpenOption.READ);
            }
            configureReader();
        }
        return current;
    }

    /**
     * Closes this data store and releases any underlying resources.
     * If a read operation is in progress, it will be aborted.
     *
     * @throws DataStoreException if an error occurred while closing this data store.
     */
    @Override
    public void close() throws DataStoreException {
        listeners.close();                  // Should never fail.
        final ImageReader codec = reader;
        if (codec != null) codec.abort();
        synchronized (this) {
            final Closeable  stream = toClose;
            reader       = null;
            toClose      = null;
            metadata     = null;
            components   = null;
            gridGeometry = null;
            try {
                Object input = null;
                if (codec != null) {
                    input = codec.getInput();
                    codec.reset();
                    codec.dispose();
                    if (input instanceof AutoCloseable) {
                        ((AutoCloseable) input).close();
                    }
                }
                if (stream != null && stream != input) {
                    stream.close();
                }
            } catch (Exception e) {
                throw new DataStoreException(e);
            }
        }
    }
}
