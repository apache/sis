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

import java.util.function.BiConsumer;
import java.io.IOException;
import java.io.BufferedWriter;
import java.nio.file.StandardOpenOption;
import java.net.URISyntaxException;
import java.awt.geom.AffineTransform;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ImageReaderWriterSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.WritableAggregate;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreClosedException;
import org.apache.sis.storage.UnsupportedStorageException;
import org.apache.sis.storage.IncompatibleResourceException;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.setup.OptionKey;


/**
 * A data store which writes grid coverages using Image I/O writers completed by the <i>World File</i> convention.
 * Georeferencing is defined by two auxiliary files described in the {@link WorldFileStore} parent class.
 *
 * <h2>Type of output objects</h2>
 * The {@link StorageConnector} output should be an instance of the following types:
 * {@link java.nio.file.Path}, {@link java.io.File}, {@link java.net.URL} or {@link java.net.URI}.
 * Other types such as {@link ImageOutputStream} are also accepted but in those cases the auxiliary files cannot be written.
 * For any output of unknown type, this data store first checks if an {@link ImageWriter} accepts the output type directly.
 * If none is found, this data store tries to {@linkplain ImageIO#createImageOutputStream(Object) create an output stream}
 * from the output object.
 *
 * <p>The storage output object may also be an {@link ImageWriter} instance ready for use
 * (i.e. with its {@linkplain ImageWriter#setOutput(Object) output set} to a non-null value).
 * In that case, this data store will use the given image writer as-is.
 * The image writer will be {@linkplain ImageWriter#dispose() disposed}
 * and its output closed (if {@link AutoCloseable}) when this data store is {@linkplain #close() closed}.</p>
 *
 * <h2>Handling of multi-image files</h2>
 * Because some image formats can store an arbitrary number of images,
 * this data store is considered as an aggregate with one resource per image.
 * All image should have the same size and all resources will share the same {@link GridGeometry}.
 * However, this base class does not implement the {@link WritableAggregate} interface directly in order
 * to give a chance to subclasses to implement {@link GridCoverageResource} directly when the format is
 * known to support only one image per file.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
class WritableStore extends WorldFileStore {
    /**
     * Position of the input/output stream beginning. This is usually 0.
     */
    private final long streamBeginning;

    /**
     * The image writer, created when first needed and cleared when the store is closed.
     * Only one of {@link #reader} and {@link #writer} should have its input or output set
     * at a given time.
     *
     * @see #writer()
     */
    private ImageWriter writer;

    /**
     * Number of images in this store, or any negative value if unknown. This information is redundant
     * with {@link ImageReader#getNumImages(boolean)} but is stored here because {@link #reader} may be
     * null and {@link ImageWriter} does not have a {@code getNumImages(…)} method.
     *
     * @see #isMultiImages()
     */
    private int numImages;

    /**
     * Creates a new store from the given file, URL or stream.
     *
     * @param  format  information about the storage (URL, stream, <i>etc</i>) and the reader/writer to use.
     * @throws DataStoreException if an error occurred while opening the stream.
     * @throws IOException if an error occurred while creating the image reader instance.
     */
    WritableStore(final FormatFinder format) throws DataStoreException, IOException {
        super(format, false);
        if (getCurrentReader() != null) {
            numImages = -1;
        } else {
            writer = format.getOrCreateWriter(null);
            if (writer == null) {
                throw new UnsupportedStorageException(super.getLocale(), WorldFileStoreProvider.NAME,
                            format.storage, format.connector.getOption(OptionKey.OPEN_OPTIONS));
            }
            configureWriter();
            if (!format.fileIsEmpty) {
                numImages = -1;
            } else {
                // Leave `numImages` to 0.
            }
        }
        streamBeginning = (format.storage instanceof ImageInputStream) ? ((ImageInputStream) format.storage).getStreamPosition() : 0;
    }

    /**
     * Sets the locale to use for warning messages, if supported. If the writer
     * does not support the locale, the writer's default locale will be used.
     */
    private void configureWriter() {
        try {
            writer.setLocale(listeners.getLocale());
        } catch (IllegalArgumentException e) {
            // Ignore
        }
        writer.addIIOWriteWarningListener(new WarningListener(listeners));
    }

    /**
     * Returns the Image I/O format names or MIME types of the image read or written by this data store.
     * More than one names may be returned if the format has aliases or if the MIME type
     * has legacy types (e.g. official {@code "image/png"} and legacy {@code "image/x-png"}).
     *
     * @param  asMimeType  {@code true} for MIME types, or {@code false} for format names.
     * @return the requested names, or an empty array if none or unknown.
     */
    @Override
    public String[] getImageFormat(final boolean asMimeType) {
        if (writer != null) {
            final ImageWriterSpi codec = writer.getOriginatingProvider();
            if (codec != null) {
                final String[] names = asMimeType ? codec.getMIMETypes() : codec.getFormatNames();
                if (names != null) {
                    return names;
                }
            }
        }
        return super.getImageFormat(asMimeType);
    }

    /**
     * Returns whether this data store contains more than one image.
     * This is used for deciding if {@link WritableStore} can overwrite a grid geometry.
     *
     * @return 0 if this store is empty, 1 if it contains exactly one image,
     *         or a value greater than 1 if it contains more than one image.
     *         The returned value is not necessarily the number of images.
     * @see #setGridGeometry(int, GridGeometry)
     */
    final int isMultiImages() throws IOException, DataStoreException {
        if (numImages < 0) {
            // This case happens only when we opened an existing file.
            final Components components = components(true, numImages);
            if (components.isEmpty()) {
                numImages = 0;
            } else if (components.exists(1)) {
                return 2;
            } else {
                numImages = 1;
            }
        }
        return numImages;
    }

    /**
     * Sets the store-wide grid geometry. Only one grid geometry can be set for a data store.
     * If a grid geometry already exists and the specified grid geometry is incompatible,
     * then an {@link IncompatibleResourceException} is thrown.
     *
     * <p>This method may use the {@link ImageReader} for checking the number of images,
     * so it is better to invoke this method before {@link #writer()}.</p>
     *
     * @param  index  index of the image for which to read the grid geometry.
     * @param  gg     the new grid geometry.
     * @return suffix of the "world file", or {@code null} if this method wrote nothing.
     * @throws IncompatibleResourceException if the "grid to CRS" is not affine,
     *         or if a different grid geometry already exists.
     *
     * @see #getGridGeometry(int)
     */
    @Override
    String setGridGeometry(final int index, GridGeometry gg)
            throws URISyntaxException, IOException, DataStoreException
    {
        assert Thread.holdsLock(this);
        /*
         * Make sure that the grid geometry starts at (0,0).
         * Must be done before to compare with existing grid.
         */
        final GridExtent extent = gg.getExtent();
        gg = gg.shiftGrid(extent.getLow().getCoordinateValues(), true);
        /*
         * If the data store already contains a coverage, then the given grid geometry
         * must be identical to the existing one, in which case there is nothing to do.
         */
        if (index != MAIN_IMAGE || isMultiImages() > 1) {
            if (!getGridGeometry(MAIN_IMAGE).equals(gg, ComparisonMode.IGNORE_METADATA)) {
                String message = resources().getString(Resources.Keys.IncompatibleGridGeometry);
                throw new IncompatibleResourceException(message).addAspect("gridGeometry");
            }
        }
        /*
         * Get the two-dimensional affine transform (it provides the "World file" content).
         * Only after we successfully got all the information, assign the grid geometry to
         * this store.
         */
        AffineTransform gridToCRS = null;
        if (gg.isDefined(GridGeometry.GRID_TO_CRS)) try {
            gridToCRS = AffineTransforms2D.castOrCopy(gg.getGridToCRS(CELL_ANCHOR));
        } catch (IllegalArgumentException e) {
            throw new IncompatibleResourceException(e.getLocalizedMessage(), e).addAspect("gridToCRS");
        }
        final String suffixWLD = super.setGridGeometry(index, gg);      // May throw `ArithmeticException`.
        /*
         * If the image is the main one, overwrite (possibly with same content) the previous auxiliary files.
         * Otherwise above checks should have ensured that the existing auxiliary files are applicable.
         */
        if (suffixWLD != null) {
            if (gridToCRS == null) {
                deleteAuxiliaryFile(suffixWLD);
            } else try (BufferedWriter out = writeAuxiliaryFile(suffixWLD)) {
writeCoeffs:    for (int i=0;; i++) {
                    final double c;
                    switch (i) {
                        case 0: c = gridToCRS.getScaleX(); break;
                        case 1: c = gridToCRS.getShearY(); break;
                        case 2: c = gridToCRS.getShearX(); break;
                        case 3: c = gridToCRS.getScaleY(); break;
                        case 4: c = gridToCRS.getTranslateX(); break;
                        case 5: c = gridToCRS.getTranslateY(); break;
                        default: break writeCoeffs;
                    }
                    out.write(Double.toString(c));
                    out.newLine();
                }
            }
            writePRJ();
        }
        return suffixWLD;
    }

    /**
     * Creates a {@link GridCoverageResource} for the specified image.
     * This method is invoked by {@link Components} when first needed
     * and the result is cached by the caller.
     *
     * @param  index  index of the image for which to create a resource.
     * @return resource for the image identified by the given index.
     * @throws IndexOutOfBoundsException if the image index is out of bounds.
     */
    @Override
    WorldFileResource createImageResource(final int index) throws DataStoreException, URISyntaxException, IOException {
        return new WritableResource(this, listeners, index, getGridGeometry(index));
    }

    /**
     * Adds a new {@code Resource} in this {@code Aggregate}.
     * The given {@link Resource} will be copied, and the <i>effectively added</i> resource returned.
     *
     * @param  resource  the resource to copy in this {@code Aggregate}.
     * @return the effectively added resource.
     * @throws DataStoreException if the given resource cannot be stored in this {@code Aggregate}.
     *
     * @see WritableAggregate#add(Resource)
     */
    public synchronized Resource add(final Resource resource) throws DataStoreException {
        Exception cause = null;
        if (resource instanceof GridCoverageResource) try {
            final Components components = components(true, numImages);
            if (numImages < 0) {
                numImages = components.size();      // For this method, we need an accurate count.
            }
            /*
             * If we are adding the first image, the grid geometry of the coverage will determine
             * the new grid geometry of the data store. Otherwise (if we are adding more images)
             * the coverage grid geometry must be the same as the current data store grid geometry.
             */
            GridGeometry domain = null;
            if (numImages != 0) {
                domain = getGridGeometry(MAIN_IMAGE);
            }
            final GridCoverage coverage = ((GridCoverageResource) resource).read(domain, null);
            if (domain == null) {
                domain = coverage.getGridGeometry();        // We are adding the first image.
            }
            final var image = new WritableResource(this, listeners, numImages, domain);
            image.write(coverage);
            components.added(image);        // Must be invoked only after above succeeded.
            numImages++;
            return image;
        } catch (URISyntaxException | IOException | RuntimeException e) {
            cause = e;
        }
        throw new DataStoreException(resources().getString(Resources.Keys.CanNotWriteResource_1, label(resource)), cause);
    }

    /**
     * Removes a {@code Resource} from this {@code Aggregate}.
     * The given resource should be one of the instances returned by {@link #components()}.
     *
     * @param  resource  child resource to remove from this {@code Aggregate}.
     * @throws DataStoreException if the given resource could not be removed.
     *
     * @see WritableAggregate#remove(Resource)
     */
    @Override
    public synchronized void remove(final Resource resource) throws DataStoreException {
        Exception cause = null;
        if (resource instanceof WritableResource) {
            final var image = (WritableResource) resource;
            if (image.store() == this) try {
                final int imageIndex = image.getImageIndex();
                writer().removeImage(imageIndex);
                final Components components = components(false, numImages);
                if (components != null) {
                    components.removed(imageIndex);
                    image.dispose();
                    numImages--;            // Okay if negative.
                }
            } catch (IOException | RuntimeException e) {
                cause = e;
            }
        }
        throw new DataStoreException(resources().getString(
                Resources.Keys.CanNotRemoveResource_2, getDisplayName(), label(resource)), cause);
    }

    /**
     * Returns the localized resources for producing warnings or error messages.
     */
    final Resources resources() {
        return Resources.forLocale(getLocale());
    }

    /**
     * Returns a label for the given resource in error messages.
     */
    private static String label(final Resource resource) throws DataStoreException {
        return resource.getIdentifier().map(Object::toString).orElse("?");
    }

    /**
     * Prepares an image reader compatible with the writer and sets its input.
     * This method is invoked for switching from write mode to read mode.
     *
     * @param  current  the current image reader, or {@code null} if none.
     * @return the image reader to use, or {@code null} if none.
     */
    @Override
    ImageReader prepareReader(ImageReader current) throws IOException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final ImageWriter writer = this.writer;
        if (writer != null) {
            final Object output = writer.getOutput();
            if (output != null) {
                if (current == null) {
                    final ImageWriterSpi wp = writer.getOriginatingProvider();
                    if (wp != null) {
                        final ImageReaderSpi rp = getProviderByClass(ImageReaderSpi.class, wp.getImageReaderSpiNames(), wp);
                        if (rp != null) {
                            current = rp.createReaderInstance();
                        }
                    }
                }
                if (current != null) {
                    writer.setOutput(null);
                    setStream(current, output, ImageReader::setInput);
                    return current;
                }
            }
        }
        return null;
    }

    /**
     * Returns the writer if it has not been closed.
     * If the data store was in read mode, invoking this method switch to write mode.
     *
     * @throws DataStoreClosedException if this data store is closed.
     * @throws IOException if an error occurred while preparing the writer.
     */
    final ImageWriter writer() throws DataStoreException, IOException {
        assert Thread.holdsLock(this);
        ImageWriter current = writer;
        if (current != null && current.getOutput() != null) {
            return current;
        }
        final ImageReader reader = getCurrentReader();
        if (reader != null) {
            final Object input = reader.getInput();
            if (input != null) {
                if (current == null) {
                    final ImageReaderSpi rp = reader.getOriginatingProvider();
                    if (rp != null) {
                        final ImageWriterSpi wp = getProviderByClass(ImageWriterSpi.class, rp.getImageWriterSpiNames(), rp);
                        if (wp != null) {
                            current = wp.createWriterInstance();
                        }
                    }
                }
                if (current != null) {
                    reader.setInput(null);
                    setStream(current, input, ImageWriter::setOutput);
                    writer = current;
                    configureWriter();
                    return current;
                }
            }
        }
        throw new DataStoreClosedException(getLocale(), WorldFileStoreProvider.NAME, StandardOpenOption.WRITE);
    }

    /**
     * Sets the input or output stream on the given image reader or writer.
     * If the operation fails, the stream is closed.
     *
     * @param  <T>     class of the {@code codec} argument.
     * @param  codec   the {@link ImageReader} or {@link ImageWriter} on which to set the stream.
     * @param  stream  the input or output to set on the specified {@code codec}.
     * @param  setter  for calling the {@code setInput(Object)} or {@code setOutput(Object)} method.
     */
    private <T> void setStream(final T codec, final Object stream, final BiConsumer<T,Object> setter) throws IOException {
        try {
            /*
             * `ImageOutputStream` extends `ImageInputStream`,
             * so there is no need to check the output stream case.
             */
            if (stream instanceof ImageInputStream) {
                ((ImageInputStream) stream).seek(streamBeginning);
            }
            setter.accept(codec, stream);
        } catch (Throwable exception) {
            if (stream instanceof AutoCloseable) try {
                ((AutoCloseable) stream).close();
            } catch (Throwable s) {
                exception.addSuppressed(s);
            }
            throw exception;
        }
    }

    /**
     * Returns the first service provider that we can get from the given list of class names.
     *
     * @param  <T>          compile-time value of {@code type} argument.
     * @param  type         type of the provider to get.
     * @param  classNames   class names of provider implementations, or {@code null} if none.
     * @param  originating  the originating provider, used for fetching the class loader.
     * @return first provider found, or {@code null} if none.
     */
    private <T extends ImageReaderWriterSpi> T getProviderByClass(final Class<T> type,
                    final String[] classNames, final ImageReaderWriterSpi originating)
    {
        if (classNames != null) {
            final IIORegistry registry = IIORegistry.getDefaultInstance();
            final ClassLoader loader = originating.getClass().getClassLoader();
            for (final String name : classNames) {
                final Class<? extends T> impl;
                try {
                    impl = Class.forName(name, true, loader).asSubclass(type);
                } catch (ClassNotFoundException | ClassCastException e) {
                    listeners.warning(e);
                    continue;
                }
                final T candidate = registry.getServiceProviderByClass(impl);
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Closes this data store and releases any underlying resources.
     * If a read or write operation is in progress in another thread,
     * then this method blocks until that operation completed.
     * This restriction is for avoiding data lost.
     *
     * @throws DataStoreException if an error occurred while closing this data store.
     */
    @Override
    public synchronized void close() throws DataStoreException {
        listeners.close();                  // Should never fail.
        try {
            final ImageWriter codec = writer;
            writer = null;
            if (codec != null) try {
                final Object output = codec.getOutput();
                codec.setOutput(null);
                codec.dispose();
                if (output instanceof AutoCloseable) {
                    ((AutoCloseable) output).close();
                }
            } catch (Exception e) {
                throw new DataStoreException(e);
            }
        } catch (Throwable e) {
            try {
                super.close();
            } catch (Throwable s) {
                e.addSuppressed(s);
            }
            throw e;
        }
        super.close();
    }
}
