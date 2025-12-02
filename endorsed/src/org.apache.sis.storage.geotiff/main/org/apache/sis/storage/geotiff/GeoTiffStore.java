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
package org.apache.sis.storage.geotiff;

import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Spliterator;
import java.net.URI;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.awt.image.RenderedImage;
import java.awt.image.RasterFormatException;
import org.opengis.util.NameSpace;
import org.opengis.util.NameFactory;
import org.opengis.util.GenericName;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreClosedException;
import org.apache.sis.storage.ReadOnlyStorageException;
import org.apache.sis.storage.WriteOnlyStorageException;
import org.apache.sis.storage.IncompatibleResourceException;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.base.StoreUtilities;
import org.apache.sis.storage.base.URIDataStore;
import org.apache.sis.storage.base.URIDataStoreProvider;
import org.apache.sis.storage.base.GridResourceWrapper;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.event.WarningEvent;
import org.apache.sis.storage.modifier.CoverageModifier;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.ChannelDataOutput;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.coverage.SubspaceNotSpecifiedException;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.collection.ListOfUnknownSize;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.util.iso.DefaultNameSpace;
import org.apache.sis.util.resources.Errors;


/**
 * A data store backed by GeoTIFF files.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @author  Alexis Manin (Geomatys)
 * @version 1.5
 * @since   0.8
 */
public class GeoTiffStore extends DataStore implements Aggregate {
    /**
     * The encoding of strings in the metadata. The TIFF specification said that is shall be US-ASCII,
     * but Apache SIS nevertheless let the user specifies an alternative encoding if needed.
     */
    final Charset encoding;

    /**
     * The GeoTIFF reader implementation, or {@code null} if the store has been closed.
     *
     * @see #reader()
     */
    private volatile Reader reader;

    /**
     * The GeoTIFF writer implementation, or {@code null} if the store has been closed.
     *
     * @see #writer()
     */
    private volatile Writer writer;

    /**
     * The compression to apply when writing tiles, or {@code null} if unspecified.
     *
     * @see #getCompression()
     */
    private final Compression compression;

    /**
     * The locale to use for formatting metadata. This is not necessarily the same as {@link #getLocale()},
     * which is about formatting error messages. A null value means "unlocalized", which is usually English.
     */
    final Locale dataLocale;

    /**
     * The {@link GeoTiffStoreProvider#LOCATION} parameter value, or {@code null} if none.
     * This is used for information purpose only, not for actual reading operations.
     *
     * @see #getOpenParameters()
     */
    private final URI location;

    /**
     * Same value as {@link #location} but as a path, or {@code null} if none.
     * Stored separately because conversion from path to URI back to path is not
     * looseness (relative paths become absolutes).
     *
     * @todo May become an array later if we want to handle TFW and PRJ file here.
     */
    final Path path;

    /**
     * The factory to use for creating image identifiers.
     */
    final NameFactory nameFactory;

    /**
     * The data store identifier created from the filename, or {@code null} if none.
     * Defined as a namespace for use as the scope of children resources (the images).
     * This is created when first needed.
     *
     * <h4>Design note</h4>
     * We do not create this field in the constructor because this value can be provided by
     * the user-specified {@link #customizer}, which would receive a reference to {@code this}
     * before its construction is completed.
     *
     * @see #namespace()
     */
    private NameSpace namespace;

    /**
     * Whether {@link #namespace} has been determined.
     * Note that the resulting namespace may still be null.
     *
     * @see #namespace()
     */
    private boolean isNamespaceSet;

    /**
     * The metadata, or {@code null} if not yet created.
     *
     * @see #getMetadata()
     */
    private Metadata metadata;

    /**
     * The native metadata, or {@code null} if not yet created.
     *
     * @see #getNativeMetadata()
     */
    private TreeTable nativeMetadata;

    /**
     * Description of images in this GeoTIFF files. This collection is created only when first needed.
     *
     * @see #components()
     */
    private Components components;

    /**
     * Whether this {@code GeotiffStore} will be hidden. If {@code true}, then some metadata that would
     * normally be provided in this {@code GeoTiffStore} will be provided by individual components instead.
     */
    final boolean hidden;

    /**
     * The user-specified method for customizing the band definitions. Never {@code null}.
     */
    final CoverageModifier customizer;

    /**
     * Creates a new GeoTIFF store from the given file, URL or stream object.
     * This constructor invokes {@link StorageConnector#closeAllExcept(Object)},
     * keeping open only the needed resource.
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if an error occurred while opening the GeoTIFF file.
     *
     * @since 1.5
     */
    public GeoTiffStore(final DataStoreProvider provider, final StorageConnector connector) throws DataStoreException {
        this(null, provider, connector, false);
    }

    /**
     * Creates a new GeoTIFF store as a component of a larger data store.
     * If the {@code hidden} parameter is {@code true}, some metadata that would normally be
     * provided in this {@code GeoTiffStore} will be provided by individual components instead.
     *
     * <h4>Example</h4>
     * A Landsat data set is a collection of files in a directory or ZIP file,
     * which includes more than 10 GeoTIFF files (one image per band or product for a scene).
     * {@link org.apache.sis.storage.landsat.LandsatStore} is a data store opening the Landsat
     * metadata file as the main file, then opening each band/product using a GeoTIFF data store.
     * Those bands/products are components of the Landsat data store.
     *
     * @param  parent     the parent that contains this new GeoTIFF store component, or {@code null} if none.
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @param  hidden     {@code true} if this GeoTIFF store will not be directly accessible from the parent.
     *                    It is the case if the parent store will expose only some {@linkplain #components()
     *                    components} instead of the GeoTIFF store itself.
     * @throws DataStoreException if an error occurred while opening the GeoTIFF file.
     *
     * @since 1.1
     */
    @SuppressWarnings("this-escape")        // `this` appears in a cyclic graph.
    public GeoTiffStore(final DataStore parent, final DataStoreProvider provider, final StorageConnector connector,
                        final boolean hidden) throws DataStoreException
    {
        super(parent, provider, connector, hidden);
        this.hidden = hidden;
        nameFactory = DefaultNameFactory.provider();
        customizer  = CoverageModifier.getOrDefault(connector);

        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final Charset encoding = connector.getOption(OptionKey.ENCODING);
        this.encoding = (encoding != null) ? encoding : StandardCharsets.US_ASCII;

        compression = connector.getOption(Compression.OPTION_KEY);
        dataLocale  = connector.getOption(OptionKey.LOCALE);
        location    = connector.getStorageAs(URI.class);
        path        = connector.getStorageAs(Path.class);
        try {
            if (URIDataStoreProvider.isWritable(connector, true)) {
                ChannelDataOutput output = URIDataStoreProvider.openAndSetNativeByteOrder(connector, Constants.GEOTIFF);
                writer = new Writer(this, output, connector.getOption(FormatModifier.OPTION_KEY));
            } else {
                ChannelDataInput input = connector.commit(ChannelDataInput.class, Constants.GEOTIFF);
                reader = new Reader(this, input);
            }
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
    }

    /**
     * Returns the namespace to use in component identifiers, or {@code null} if none.
     * This method must be invoked inside a block synchronized on {@code this}.
     *
     * @throws DataStoreException if an error occurred while computing an identifier.
     */
    private NameSpace namespace() throws DataStoreException {
        assert Thread.holdsLock(this);
        if (!isNamespaceSet && (reader != null || writer != null)) {
            GenericName name = null;
            /*
             * We test `location != null` because if the location was not convertible to URI,
             * then the string representation is probably a class name, which is not useful.
             */
            if (location != null) {
                String filename = (reader != null ? reader.input : writer.output).filename;
                filename = IOUtilities.filenameWithoutExtension(filename);
                name = nameFactory.createLocalName(null, filename);
            }
            name = customizer.customize(new CoverageModifier.Source(this), name);
            if (name != null) {
                namespace = nameFactory.createNameSpace(name, null);
            }
            isNamespaceSet = true;
        }
        return namespace;
    }

    /**
     * Creates a name in the namespace of this store.
     * This method must be invoked inside a block synchronized on {@code this}.
     *
     * @param  tip  the tip of the name to create.
     * @return a name in the scope of this store.
     * @throws DataStoreException if an error occurred while computing an identifier.
     */
    final GenericName createLocalName(final String tip) throws DataStoreException {
        return nameFactory.createLocalName(namespace(), tip);
    }

    /**
     * Opens access to listeners for {@link ImageFileDirectory}.
     */
    final StoreListeners listeners() {
        return listeners;
    }

    /**
     * Returns the parameters used to open this GeoTIFF data store.
     * The parameters are described by {@link GeoTiffStoreProvider#getOpenParameters()} and contains at least
     * a parameter named {@value org.apache.sis.storage.DataStoreProvider#LOCATION} with a {@link URI} value.
     * The return value may be empty if the storage input cannot be described by a URI
     * (for example a GeoTIFF file reading directly from a {@link java.nio.channels.ReadableByteChannel}).
     *
     * @return parameters used for opening this data store.
     */
    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        final ParameterValueGroup param = URIDataStore.parameters(provider, location);
        if (param != null) {
            final Writer w = writer;
            if (w != null) {
                final Set<FormatModifier> modifiers = w.getModifiers();
                if (!modifiers.isEmpty()) {
                    param.parameter(GeoTiffStoreProvider.MODIFIERS).setValue(modifiers.toArray(FormatModifier[]::new));
                }
                if (compression != null) {
                    param.parameter(GeoTiffStoreProvider.COMPRESSION).setValue(compression);
                }
            }
        }
        return Optional.ofNullable(param);
    }

    /**
     * Returns the modifiers (BigTIFF, COG…) of this data store.
     *
     * @return format modifiers of this data store.
     *
     * @since 1.5
     */
    public Set<FormatModifier> getModifiers() {
        final Writer w = writer; if (w != null) return w.getModifiers();
        final Reader r = reader; if (r != null) return r.getModifiers();
        return Set.of();
    }

    /**
     * Returns the compression used when writing tiles.
     * This is not necessarily the compression of images to be read.
     * For the compression of existing images, see {@linkplain #getMetadata() the metadata}.
     *
     * @return the compression to use for writing new images, or empty if unspecified.
     *
     * @since 1.5
     */
    public Optional<Compression> getCompression() {
        return Optional.ofNullable(compression);
    }

    /**
     * Returns an identifier constructed from the name of the TIFF file.
     * An identifier is available only if the storage input specified at construction time was something convertible to
     * {@link java.net.URI}, for example an {@link java.net.URL}, {@link java.io.File} or {@link java.nio.file.Path}.
     *
     * @return the identifier derived from the filename.
     * @throws DataStoreException if an error occurred while fetching the identifier.
     *
     * @since 1.0
     */
    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final NameSpace namespace;
        synchronized (this) {
            namespace = namespace();
        }
        return (namespace != null) ? Optional.of(namespace.name()) : Optional.empty();
    }

    /**
     * Sets the {@code metadata/identificationInfo/resourceFormat} node to "GeoTIFF" format.
     */
    final void setFormatInfo(final MetadataBuilder builder) {
        builder.setPredefinedFormat(Constants.GEOTIFF, listeners, true);
        builder.addFormatReaderSIS(Constants.GEOTIFF);
        builder.addLanguage(Locale.ENGLISH, encoding, MetadataBuilder.Scope.METADATA);
        builder.addResourceScope(ScopeCode.valueOf("COVERAGE"), null);
    }

    /**
     * Returns information about the dataset as a whole. The returned metadata object can contain information
     * such as the spatiotemporal extent of the dataset, contact information about the creator or distributor,
     * data quality, usage constraints and more.
     *
     * @return information about the dataset.
     * @throws DataStoreException if an error occurred while reading the data.
     */
    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) {
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final Reader reader = reader();
            final var builder = new MetadataBuilder();
            setFormatInfo(builder);
            int n = 0;
            try {
                GridCoverageResource dir;
                while ((dir = reader.getImage(n++)) != null) {
                    builder.addFromComponent(dir.getMetadata());
                }
            } catch (IOException e) {
                throw errorIO(e);
            } catch (ArithmeticException e) {
                listeners.warning(e);
            }
            /*
             * Add the filename as an identifier only if the input was something convertible to URI (URL, File or Path),
             * otherwise reader.input.filename may not be useful; it may be just the InputStream classname. If the TIFF
             * file did not specify any ImageDescription tag, then we will add the filename as a title instead of an
             * identifier because the title is mandatory in ISO 19115 metadata.
             */
            getIdentifier().ifPresent((id) -> {
                builder.addIdentifier(id, MetadataBuilder.Scope.ALL);
                // Replace the `ResourceInternationalString` for "Image 1".
                if (!(builder.getTitle() instanceof SimpleInternationalString)) {
                    builder.setTitle(id.toString());
                }
            });
            builder.setISOStandards(true);
            metadata = customizer.customize(new CoverageModifier.Source(this), builder.build());
        }
        return metadata;
    }

    /**
     * Returns TIFF tags and GeoTIFF keys as a tree for debugging purpose.
     * The tags and keys appear in the order they are declared in the file.
     * The columns are tag numerical code as an {@link Integer},
     * tag name as a {@link String} and value as an {@link Object}.
     *
     * <p>This method should not be invoked during normal operations;
     * the {@linkplain #getMetadata() standard metadata} are preferred
     * because they allow abstraction of data format details.
     * Native metadata should be used only when an information does not appear in standard metadata,
     * or for debugging purposes.</p>
     *
     * <h4>Performance note</h4>
     * Since this method should not be invoked in normal operations, it has not been tuned for performance.
     * Invoking this method may cause a lot of {@linkplain java.nio.channels.SeekableByteChannel#position(long)
     * seek operations}.
     *
     * @return resources information structured in an implementation-specific way.
     * @throws DataStoreException if an error occurred while reading the metadata.
     *
     * @since 1.2
     */
    @Override
    public synchronized Optional<TreeTable> getNativeMetadata() throws DataStoreException {
        if (nativeMetadata == null) try {
            nativeMetadata = new NativeMetadata(getLocale()).read(reader());
        } catch (IOException e) {
            throw errorIO(e);
        }
        return Optional.of(nativeMetadata);
    }

    /**
     * Returns the paths to the files used by this GeoTIFF store.
     * The fileset contains the path of the file given at construction time.
     *
     * @return files used by this resource, or an empty value if unknown.
     * @throws DataStoreException if an error occurred while preparing the set of files.
     *
     * @since 1.5
     */
    @Override
    public Optional<FileSet> getFileSet() throws DataStoreException {
        return (path != null) ? Optional.of(new FileSet(path)) : Optional.empty();
    }

    /**
     * Returns the reader if it is not closed, or throws an exception otherwise.
     *
     * @return the reader, potentially created when first needed.
     * @throws WriteOnlyStorageException if the channel is write-only.
     *
     * @see #close()
     */
    private Reader reader() throws DataStoreException {
        assert Thread.holdsLock(this);
        final Reader r = reader;
        if (r == null) {
            if (writer != null) {
                throw new WriteOnlyStorageException(readOrWriteOnly(1));
            }
            throw new DataStoreClosedException(getLocale(), Constants.GEOTIFF, StandardOpenOption.READ);
        }
        return r;
    }

    /**
     * Returns the writer if it can be created and is not closed, or throws an exception otherwise.
     * If there is no writer but a reader exists, then a writer is created for writing past the last image.
     * After the write operation has been completed, it is caller responsibility to invoke the following code:
     *
     * {@snippet lang="java":
     *     writer.synchronize(reader, false);
     *     // Write the image
     *     writer.flush();
     *     writer.synchronize(reader, true);
     * }
     *
     * @return the writer, potentially created when first needed.
     * @throws ReadOnlyStorageException if this data store is read-only.
     *
     * @see #close()
     * @see Writer#synchronize(Reader, boolean)
     */
    private Writer writer() throws DataStoreException, IOException {
        assert Thread.holdsLock(this);
        final Reader r = reader;
        Writer w = writer;
        if (w == null) {
            if (r == null) {
                throw new DataStoreClosedException(getLocale(), Constants.GEOTIFF, StandardOpenOption.WRITE);
            }
            writer = w = new Writer(r);
        } else if (r != null) {
            w.moveAfterExisting(r);
        }
        return w;
    }

    /**
     * Returns descriptions of all images in this GeoTIFF file.
     * Images are not immediately loaded.
     *
     * <p>If an error occurs during iteration in the returned collection,
     * an unchecked {@link BackingStoreException} will be thrown with a {@link DataStoreException} as its cause.</p>
     *
     * @return descriptions of all images in this GeoTIFF file.
     * @throws DataStoreException if an error occurred while fetching the image descriptions.
     *
     * @since 1.0
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public synchronized List<GridCoverageResource> components() throws DataStoreException {
        if (components == null) {
            components = new Components();
        }
        return components;
    }

    /**
     * The components returned by {@link #components}. Defined as a named class instead of an anonymous
     * class for more readable stack trace. This is especially useful since {@link BackingStoreException}
     * may happen in any method.
     */
    private final class Components extends ListOfUnknownSize<GridCoverageResource> {
        /** The collection size, cached when first computed. */
        private int size = -1;

        /** Creates a new list of components. */
        Components() {
        }

        /** Declares that this list has no duplicated elements and excludes null. */
        @Override protected int characteristics() {
            return Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.DISTINCT;
        }

        /** Returns the size or empty if not yet known. */
        @Override protected OptionalInt sizeIfKnown() {
            synchronized (GeoTiffStore.this) {
                return (size >= 0) ? OptionalInt.of(size) : OptionalInt.empty();
            }
        }

        /** Returns the size, computing and caching it if needed. */
        @Override public int size() {
            synchronized (GeoTiffStore.this) {
                if (size < 0) {
                    size = super.size();
                }
                return size;
            }
        }

        /** Increments the size by the given number of images. */
        final void incrementSize(final int n) {
            synchronized (GeoTiffStore.this) {
                if (size >= 0) {
                    size += n;
                }
            }
        }

        /** Returns whether the given index is valid. */
        @Override protected boolean isValidIndex(final int index) {
            return (index >= 0) && (size >= 0 ? index < size : getImageFileDirectory(index) != null);
        }

        /** Returns element at the given index or throw {@link IndexOutOfBoundsException}. */
        @Override public GridCoverageResource get(final int index) {
            if (index >= 0) {
                GridCoverageResource image = getImageFileDirectory(index);
                if (image != null) return image;
            }
            throw new IndexOutOfBoundsException(errors().getString(Errors.Keys.IndexOutOfBounds_1, index));
        }

        /** Returns element at the given index or returns {@code null} if the index is invalid. */
        private GridCoverageResource getImageFileDirectory(final int index) {
            try {
                synchronized (GeoTiffStore.this) {
                    return reader().getImage(index);
                }
            } catch (IOException e) {
                throw new BackingStoreException(errorIO(e));
            } catch (DataStoreException e) {
                throw new BackingStoreException(e);
            }
        }
    }

    /**
     * Returns the image at the given index. Images numbering starts at 1.
     * If the given string has a scope (e.g. "filename:1"), then the scope
     *
     * @param  sequence  string representation of the image index, starting at 1.
     * @return image at the given index.
     * @throws DataStoreException if the requested image cannot be obtained.
     */
    @Override
    public synchronized GridCoverageResource findResource(final String sequence) throws DataStoreException {
        ArgumentChecks.ensureNonEmpty("sequence", sequence);
        final int index = parseImageIndex(sequence);
        if (index >= 0) try {
            final GridCoverageResource image = reader().getImage(index - 1);
            if (image != null) return image;
        } catch (IOException e) {
            throw errorIO(e);
        }
        throw new IllegalNameException(StoreUtilities.resourceNotFound(this, sequence));
    }

    /**
     * Validates input resource name and extracts the image index it should contain.
     * The resource name may be of the form "1" or "filename:1". We verify that:
     *
     * <ul>
     *   <li>Input tip (last name part) is a parsable integer.</li>
     *   <li>If input provides more than a tip, all test before the tip matches this datastore namespace
     *       (should be the name of the Geotiff file without its extension).</li>
     * </ul>
     *
     * @param  sequence  a string representing the name of a resource present in this datastore.
     * @return the index of the Geotiff image matching the requested resource.
     *         There is no verification that the returned index is valid.
     * @throws IllegalNameException if the argument use an invalid namespace or if the tip is not an integer.
     * @throws DataStoreException if an exception occurred while computing an identifier.
     */
    private int parseImageIndex(String sequence) throws DataStoreException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final NameSpace namespace = namespace();
        final String separator = DefaultNameSpace.getSeparator(namespace, false);
        final int s = sequence.lastIndexOf(separator);
        if (s >= 0) {
            if (namespace != null) {
                final String expected = namespace.name().toString();
                if (!sequence.substring(0, s).equals(expected)) {
                    throw new IllegalNameException(errors().getString(Errors.Keys.UnexpectedNamespace_2, expected, sequence));
                }
            }
            sequence = sequence.substring(s + separator.length());
        }
        try {
            return Integer.parseInt(sequence);
        } catch (NumberFormatException e) {
            throw new IllegalNameException(StoreUtilities.resourceNotFound(this, sequence), e);
        }
    }

    /**
     * Encodes the given image in the GeoTIFF file.
     * The image is appended after any existing images in the GeoTIFF file.
     * This method does not handle pyramids such as Cloud Optimized GeoTIFF (COG).
     *
     * @param  image     the image to encode.
     * @param  grid      mapping from pixel coordinates to "real world" coordinates, or {@code null} if none.
     * @param  metadata  title, author and other information, or {@code null} if none.
     * @return the effectively added resource. Using this resource may cause data to be reloaded.
     * @throws ReadOnlyStorageException if this data store is read-only.
     * @throws IncompatibleResourceException if the given {@code image} has a property which is not supported by this writer.
     * @throws DataStoreException if an error occurred while writing to the output stream.
     *
     * @since 1.5
     */
    @SuppressWarnings("LocalVariableHidesMemberVariable")
    public synchronized GridCoverageResource append(final RenderedImage image, final GridGeometry grid, final Metadata metadata)
            throws DataStoreException
    {
        final int index;
        try {
            final Reader reader = this.reader;
            final Writer writer = writer();
            writer.synchronize(reader, false);
            final long offsetIFD;
            try {
                offsetIFD = writer.append(image, grid, metadata);
            } finally {
                writer.synchronize(reader, true);
            }
            if (reader != null) {
                reader.offsetOfWrittenIFD(offsetIFD);
            }
            index = writer.imageIndex++;
        } catch (RasterFormatException | ArithmeticException | IllegalArgumentException e) {
            throw new IncompatibleResourceException(cannotWrite(), e).addAspect("raster");
        } catch (RuntimeException | IOException e) {
            throw new DataStoreException(cannotWrite(), e);
        }
        if (components != null) {
            components.incrementSize(1);
        }
        /*
         * Returns a thin wrapper with only a reference to this store and the image index.
         * The actual loading of the effectively added resource will be done only if requested.
         */
        return new GridResourceWrapper() {
            /** The lock to use for synchronization purposes. */
            @Override protected Object getSynchronizationLock() {
                return GeoTiffStore.this;
            }

            /** Loads the effectively added resource when first requested. */
            @Override protected GridCoverageResource createSource() throws DataStoreException {
                try {
                    synchronized (GeoTiffStore.this) {
                        return reader().getImage(index);
                    }
                } catch (IOException e) {
                    throw new DataStoreException(errorIO(e));
                }
            }
        };
    }

    /**
     * Adds a new grid coverage in the GeoTIFF file.
     * The coverage is appended after any existing images in the GeoTIFF file.
     * This method does not handle pyramids such as Cloud Optimized GeoTIFF (COG).
     *
     * @param  coverage  the grid coverage to encode.
     * @param  metadata  title, author and other information, or {@code null} if none.
     * @return the effectively added resource. Using this resource may cause data to be reloaded.
     * @throws SubspaceNotSpecifiedException if the given grid coverage is not a two-dimensional slice.
     * @throws ReadOnlyStorageException if this data store is read-only.
     * @throws DataStoreException if the given {@code image} has a property which is not supported by this writer,
     *         or if an error occurred while writing to the output stream.
     *
     * @since 1.5
     */
    public GridCoverageResource append(final GridCoverage coverage, final Metadata metadata) throws DataStoreException {
        return append(coverage.render(null), coverage.getGridGeometry(), metadata);
    }

    /**
     * Registers a listener to notify when the specified kind of event occurs in this data store.
     * The current implementation of this data store can emit only {@link WarningEvent}s;
     * any listener specified for another kind of events will be ignored.
     */
    @Override
    public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
        // If an argument is null, we let the parent class throws (indirectly) NullPointerException.
        if (listener == null || eventType == null || eventType.isAssignableFrom(WarningEvent.class)) {
            super.addListener(eventType, listener);
        }
    }

    /**
     * Returns the error resources in the current locale.
     */
    private Errors errors() {
        return Errors.forLocale(getLocale());
    }

    /**
     * Returns the exception to throw when an I/O error occurred.
     * This method wraps the exception with a {@literal "Cannot read <filename>"} message.
     */
    final DataStoreException errorIO(final IOException e) {
        return new DataStoreException(errors().getString(Errors.Keys.CanNotRead_1, getDisplayName()), e);
    }

    /**
     * Returns the error message for a file that cannot be written.
     */
    private String cannotWrite() {
        return errors().getString(Errors.Keys.CanNotWriteFile_2, Constants.GEOTIFF, getDisplayName());
    }

    /**
     * Returns a localized error message saying that this data store has been opened in read-only or write-only mode.
     *
     * @param  mode  0 for read-only, or 1 for write-only.
     * @return localized error message.
     */
    final String readOrWriteOnly(final int mode) {
        return errors().getString(Errors.Keys.OpenedReadOrWriteOnly_2, mode, getDisplayName());
    }

    /**
     * Closes this GeoTIFF store and releases any underlying resources.
     * This method can be invoked asynchronously for interrupting a long reading process.
     *
     * @throws DataStoreException if an error occurred while closing the GeoTIFF file.
     */
    @Override
    public void close() throws DataStoreException {
        try {
            listeners.close();                  // Should never fail.
            final Reader r = reader;
            final Writer w = writer;
            if (w != null) w.close();
            if (r != null) r.close();
        } catch (IOException e) {
            throw new DataStoreException(e);
        } finally {
            synchronized (this) {
                components     = null;
                namespace      = null;
                metadata       = null;
                nativeMetadata = null;
                reader         = null;
                writer         = null;
            }
        }
    }
}
