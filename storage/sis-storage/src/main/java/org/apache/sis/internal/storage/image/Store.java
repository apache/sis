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
package org.apache.sis.internal.storage.image;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.io.IOException;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardOpenOption;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreClosedException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.storage.UnsupportedStorageException;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.internal.storage.PRJDataStore;
import org.apache.sis.internal.storage.io.IOUtilities;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.internal.util.ListOfUnknownSize;
import org.apache.sis.metadata.sql.MetadataStoreException;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.setup.OptionKey;


/**
 * A data store which creates grid coverages from Image I/O.
 * The store is considered as an aggregate, with one resource per image.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class Store extends PRJDataStore implements Aggregate {
    /**
     * Image I/O format names (ignoring case) for which we have an entry in the {@code SpatialMetadata} database.
     */
    private static final String[] KNOWN_FORMATS = {
        "PNG"
    };

    /**
     * Index of the main image. This is relevant only with formats capable to store an arbitrary amount of images.
     * Current implementation assumes that the main image is always the first one, but it may become configurable
     * in a future version if useful.
     *
     * @see #width
     * @see #height
     */
    private static final int MAIN_IMAGE = 0;

    /**
     * The default World File suffix when it can not be determined from {@link #location}.
     * This is a GDAL convention.
     */
    private static final String DEFAULT_SUFFIX = "wld";

    /**
     * The filename extension (may be an empty string), or {@code null} if unknown.
     * It does not include the leading dot.
     */
    private final String suffix;

    /**
     * The image reader, set by the constructor and cleared when no longer needed.
     */
    private ImageReader reader;

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
    private List<Image> components;

    /**
     * The metadata object, or {@code null} if not yet created.
     *
     * @see #getMetadata()
     */
    private Metadata metadata;

    /**
     * Creates a new store from the given file, URL or stream.
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if an error occurred while opening the stream.
     * @throws IOException if an error occurred while creating the image reader instance.
     */
    public Store(final StoreProvider provider, final StorageConnector connector)
            throws DataStoreException, IOException
    {
        super(provider, connector);
        final Map<ImageReaderSpi,Boolean> deferred = new LinkedHashMap<>();
        final Object storage = connector.getStorage();
        suffix = IOUtilities.extension(storage);
        /*
         * Search for a reader that claim to be able to read the storage input.
         * First we try readers associated to the file suffix. If no reader is
         * found, we try all other readers.
         */
        if (suffix != null) {
            reader = FormatFilter.SUFFIX.createReader(suffix, connector, deferred);
        }
        if (reader == null) {
            reader = FormatFilter.SUFFIX.createReader(null, connector, deferred);
fallback:   if (reader == null) {
                /*
                 * If no reader has been found, maybe `StorageConnector` has not been able to create
                 * an `ImageInputStream`. It may happen if the storage object is of unknown type.
                 * Check if it is the case, then try all providers that we couldn't try because of that.
                 */
                ImageInputStream stream = null;
                for (final Map.Entry<ImageReaderSpi,Boolean> entry : deferred.entrySet()) {
                    if (entry.getValue()) {
                        if (stream == null) {
                            stream = ImageIO.createImageInputStream(storage);
                            if (stream == null) break;
                        }
                        final ImageReaderSpi p = entry.getKey();
                        if (p.canDecodeInput(stream)) {
                            connector.closeAllExcept(storage);
                            reader = p.createReaderInstance();
                            reader.setInput(stream, false, true);
                            break fallback;
                        }
                    }
                }
                throw new UnsupportedStorageException(super.getLocale(), StoreProvider.NAME,
                            storage, connector.getOption(OptionKey.OPEN_OPTIONS));
            }
        }
        /*
         * Sets the locale to use for warning messages, if supported. If the reader
         * does not support the locale, the reader's default locale will be used.
         */
        try {
            reader.setLocale(listeners.getLocale());
        } catch (IllegalArgumentException e) {
            // Ignore
        }
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
                // If the case can not be determined, `lower` will default to `true`.
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
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the auxiliary file content can not be parsed.
     */
    private AffineTransform2D readWorldFile() throws IOException, DataStoreException {
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
            listeners.warning(Resources.format(Resources.Keys.CanNotReadAuxiliaryFile_1, preferred), warning);
        }
        return null;
    }

    /**
     * Reads the "World file" by parsing an auxiliary file with the given suffix.
     *
     * @param  wld  suffix of the auxiliary file.
     * @return the "World file" content as an affine transform.
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the file content can not be parsed.
     */
    private AffineTransform2D readWorldFile(final String wld) throws IOException, DataStoreException {
        final AuxiliaryContent content = readAuxiliaryFile(wld, encoding);
        final CharSequence[] lines = CharSequences.splitOnEOL(readAuxiliaryFile(wld, encoding));
        int count = 0;
        final int expected = 6;                     // Expected number of elements.
        final double[] elements = new double[expected];
        for (int i=0; i<expected; i++) {
            final String line = lines[i].toString().trim();
            if (!line.isEmpty() && line.charAt(0) != '#') {
                if (count >= expected) {
                    throw new DataStoreContentException(errors().getString(Errors.Keys.TooManyOccurrences_2, expected, "coefficient"));
                }
                try {
                    elements[count++] = Double.parseDouble(line);
                } catch (NumberFormatException e) {
                    throw new DataStoreContentException(errors().getString(Errors.Keys.ErrorInFileAtLine_2, content.getFilename(), i), e);
                }
            }
        }
        if (count != expected) {
            throw new EOFException(errors().getString(Errors.Keys.UnexpectedEndOfFile_1, content.getFilename()));
        }
        return new AffineTransform2D(elements);
    }

    /**
     * Returns the localized resources for producing error messages.
     */
    private Errors errors() {
        return Errors.getResources(listeners.getLocale());
    }

    /**
     * Gets the grid geometry for image at the given index.
     * This method should be invoked only once per image, and the result cached.
     *
     * @param  index  index of the image for which to read the grid geometry.
     * @return grid geometry of the image at the given index.
     * @throws IndexOutOfBoundsException if the image index is out of bounds.
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the {@code *.prj} or {@code *.tfw} auxiliary file content can not be parsed.
     */
    private GridGeometry getGridGeometry(final int index) throws IOException, DataStoreException {
        assert Thread.holdsLock(this);
        final ImageReader reader = reader();
        if (gridGeometry == null) {
            final AffineTransform2D gridToCRS;
            width     = reader.getWidth (MAIN_IMAGE);
            height    = reader.getHeight(MAIN_IMAGE);
            gridToCRS = readWorldFile();
            readPRJ();
            gridGeometry = new GridGeometry(new GridExtent(width, height), PixelInCell.CELL_CENTER, gridToCRS, crs);
        }
        if (index != MAIN_IMAGE) {
            final int w = reader.getWidth (index);
            final int h = reader.getHeight(index);
            if (w != width || h != height) {
                return new GridGeometry(new GridExtent(w, h), PixelInCell.CELL_CENTER, null, null);
            }
        }
        return gridGeometry;
    }

    /**
     * Returns information about the data store as a whole.
     */
    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) try {
            final MetadataBuilder builder = new MetadataBuilder();
            String format = reader().getFormatName();
            for (final String key : KNOWN_FORMATS) {
                if (key.equalsIgnoreCase(format)) {
                    try {
                        builder.setPredefinedFormat(key);
                        format = null;
                    } catch (MetadataStoreException e) {
                        listeners.warning(Level.FINE, null, e);
                    }
                    break;
                }
            }
            builder.addFormatName(format);                          // Does nothing if `format` is null.
            builder.addResourceScope(ScopeCode.COVERAGE, null);
            builder.addSpatialRepresentation(null, getGridGeometry(MAIN_IMAGE), true);
            if (gridGeometry.isDefined(GridGeometry.ENVELOPE)) {
                builder.addExtent(gridGeometry.getEnvelope());
            }
            addTitleOrIdentifier(builder);
            builder.setISOStandards(false);
            metadata = builder.buildAndFreeze();
        } catch (IOException e) {
            throw new DataStoreException(e);
        } catch (TransformException e) {
            throw new DataStoreReferencingException(e);
        }
        return metadata;
    }

    /**
     * Returns all images in this store. Note that fetching the size of the list is a potentially costly operation.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public final synchronized Collection<? extends GridCoverageResource> components() throws DataStoreException {
        if (components == null) try {
            components = new Components();
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
        return components;
    }

    /**
     * A list of images where each {@link Image} instance is initialized when first needed.
     * Fetching the list size may be a costly operation and will be done only if requested.
     */
    private final class Components extends ListOfUnknownSize<Image> {
        /**
         * Size of this list, or -1 if unknown.
         */
        private int size;

        /**
         * All elements in this list. Some array element may be {@code null} if the image
         * as never been requested.
         */
        private Image[] images;

        /**
         * Creates a new list of images.
         */
        private Components() throws DataStoreException, IOException {
            size = reader().getNumImages(false);
            images = new Image[size >= 0 ? size : 1];
        }

        /**
         * Returns the number of images in this list.
         * This method may be costly when invoked for the first time.
         */
        @Override
        public int size() {
            synchronized (Store.this) {
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
         * Returns the number of images if this information is known, or -1 otherwise.
         * This is used by {@link ListOfUnknownSize} for optimizing some operations.
         */
        @Override
        protected int sizeIfKnown() {
            synchronized (Store.this) {
                return size;
            }
        }

        /**
         * Returns {@code true} if an element exists at the given index.
         * Current implementations is not more efficient than {@link #get(int)}.
         */
        @Override
        protected boolean exists(final int index) {
            synchronized (Store.this) {
                if (size >= 0) {
                    return index >= 0 && index < size;
                }
                return get(index) != null;
            }
        }

        /**
         * Returns the image at the given index. New instances are created when first requested.
         */
        @Override
        public Image get(final int index) {
            synchronized (Store.this) {
                Image image = null;
                if (index < images.length) {
                    image = images[index];
                }
                if (image == null) try {
                    image = new Image(Store.this, listeners, index, getGridGeometry(index));
                    if (index >= images.length) {
                        images = Arrays.copyOf(images, Math.max(images.length * 2, index + 1));
                    }
                    images[index] = image;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } catch (DataStoreException e) {
                    throw new BackingStoreException(e);
                }
                return image;
            }
        }
    }

    /**
     * Returns the reader if it has not been closed.
     */
    final ImageReader reader() throws DataStoreException {
        final ImageReader in = reader;
        if (in == null) {
            throw new DataStoreClosedException(getLocale(), StoreProvider.NAME, StandardOpenOption.READ);
        }
        return in;
    }

    /**
     * Closes this data store and releases any underlying resources.
     *
     * @throws DataStoreException if an error occurred while closing this data store.
     */
    @Override
    public synchronized void close() throws DataStoreException {
        final ImageReader r = reader;
        reader = null;
        if (r != null) try {
            final Object input = r.getInput();
            r.setInput(null);
            r.dispose();
            if (input instanceof AutoCloseable) {
                ((AutoCloseable) input).close();
            }
        } catch (Exception e) {
            throw new DataStoreException(e);
        }
    }
}
