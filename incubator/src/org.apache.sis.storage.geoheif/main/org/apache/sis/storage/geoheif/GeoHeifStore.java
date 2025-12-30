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
package org.apache.sis.storage.geoheif;

import java.net.URI;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.opengis.util.LocalName;
import org.opengis.util.NameSpace;
import org.opengis.util.NameFactory;
import org.opengis.util.GenericName;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.ChannelImageInputStream;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreClosedException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.base.URIDataStore;
import org.apache.sis.storage.modifier.CoverageModifier;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.isobmff.Root;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.util.resources.Errors;


/**
 * A data store backed by GeoHEIF files.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public class GeoHeifStore extends DataStore implements Aggregate {
    /**
     * The {@link GeoHeifStoreProvider#LOCATION} parameter value, or {@code null} if none.
     * This is used for information purpose only, not for actual reading operations.
     *
     * @see #getOpenParameters()
     */
    private final URI location;

    /**
     * Same value as {@link #location} but as a path, or {@code null} if none.
     * Stored separately because conversion from path to URI then back to path
     * is not looseness (relative paths become absolutes).
     *
     * @see #getFileSet()
     */
    private final Path path;

    /**
     * The data store identifier created from the filename, or {@code null} if none.
     * Defined as a namespace for use as the scope of children resources (the images).
     * May be {@code null}.
     *
     * @see #createComponentName(String)
     */
    private final NameSpace namespace;

    /**
     * The factory to use for creating resource names in the namespace of this store.
     *
     * @see #createComponentName(String)
     */
    private final NameFactory nameFactory;

    /**
     * The stream from which to read the data, or {@code null} if this store has been closed.
     * {@code GeoHeifStore} needs only a {@link ChannelDataInput} for its own operations.
     * But the {@code ChannelImageInputStream} sub-type is used because we need Image I/O
     * when reading tiles encoded as a <abbr>JPEG</abbr> images.
     *
     * @see #ensureOpen()
     * @see #close()
     */
    private volatile ChannelImageInputStream input;

    /**
     * The <abbr>ISO</abbr> <abbr>BMFF</abbr> boxes parsed from the file header.
     * {@code Root} is not an existing type of box, but only a container.
     */
    private Root root;

    /**
     * The metadata, or {@code null} if not yet created.
     *
     * @see #getMetadata()
     */
    private Metadata metadata;

    /**
     * Description of images in this GeoHEIF files.
     * This array is created only when first needed.
     *
     * @see #components()
     */
    private Resource[] content;

    /**
     * The user-specified method for customizing the grid geometry and band definitions. Never {@code null}.
     */
    final CoverageModifier customizer;

    /**
     * Creates a new GeoHEIF store from the given file, URL or stream object.
     * This constructor invokes {@link StorageConnector#closeAllExcept(Object)},
     * keeping open only the needed resource.
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if an error occurred while opening the GeoHEIF file.
     */
    public GeoHeifStore(final DataStoreProvider provider, final StorageConnector connector) throws DataStoreException {
        super(provider, connector);
        location    = connector.getStorageAs(URI.class);
        path        = connector.getStorageAs(Path.class);
        input       = connector.commit(ChannelImageInputStream.class, GeoHeifStoreProvider.NAME);
        customizer  = CoverageModifier.getOrDefault(connector);
        nameFactory = DefaultNameFactory.provider();
        if (location != null) {
            String filename = IOUtilities.filenameWithoutExtension(input.filename);
            namespace = nameFactory.createNameSpace(nameFactory.createLocalName(null, filename), null);
        } else {
            namespace = null;
        }
    }

    /**
     * Returns the parameters used to open this GeoHEIF data store.
     * The parameters are described by {@link GeoHeifStoreProvider#getOpenParameters()} and contains at least
     * a parameter named {@value org.apache.sis.storage.DataStoreProvider#LOCATION} with a {@link URI} value.
     * The return value may be empty if the storage input cannot be described by a URI
     * (for example a GeoHEIF file reading directly from a {@link java.nio.channels.ReadableByteChannel}).
     *
     * @return parameters used for opening this data store.
     */
    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        return Optional.ofNullable(URIDataStore.parameters(provider, location));
    }

    /**
     * Returns the paths to the files used by this GeoHEIF store.
     *
     * @return files used by this GeoHEIF store.
     * @throws DataStoreException if an error occurred while preparing the set of files.
     */
    @Override
    public Optional<FileSet> getFileSet() throws DataStoreException {
        if (path != null) {
            return Optional.of(new FileSet(path));
        }
        return Optional.empty();
    }

    /**
     * Returns an identifier constructed from the name of the <abbr>HEIF</abbr> file.
     * An identifier is available only if the storage input specified at construction time was something convertible to
     * {@link java.net.URI}, for example an {@link java.net.URL}, {@link java.io.File} or {@link java.nio.file.Path}.
     *
     * @return the identifier derived from the filename.
     * @throws DataStoreException if an error occurred while fetching the identifier.
     */
    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return (namespace != null) ? Optional.of(namespace.name()) : Optional.empty();
    }

    /**
     * Creates a name for a component of this data store.
     *
     * @param  tip  component name as the tip of the qualified name.
     * @return a name in the namespace of this store.
     */
    final LocalName createComponentName(final String tip) {
        return nameFactory.createLocalName(namespace, tip);
    }

    /**
     * Ensures that the data store is still open.
     *
     * @return the non-null input channel.
     * @throws DataStoreClosedException if the data store is closed.
     */
    final ChannelDataInput ensureOpen() throws DataStoreClosedException {
        assert Thread.holdsLock(this);
        ChannelDataInput in = input;
        if (in != null) {
            return in;
        }
        throw new DataStoreClosedException(getLocale(), GeoHeifStoreProvider.NAME, StandardOpenOption.READ);
    }

    /**
     * Returns the root node which contains all boxes of the file.
     *
     * @return all boxes of the file.
     * @throws DataStoreException if an error occurred while parsing the root.
     */
    private Root root() throws DataStoreException {
        final ChannelDataInput in = ensureOpen();   // Unconditional for making sure that the file is open.
        if (root == null) {
            try {
                root = new Root(new Reader(in, listeners));
            } catch (IOException e) {
                throw new DataStoreException(Errors.format(Errors.Keys.CanNotOpen_1, in.filename), e);
            }
        }
        return root;
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
            final var builder = new MetadataBuilder();
            root().metadata(builder);
            builder.setISOStandards(true);
            builder.setPredefinedFormat(GeoHeifStoreProvider.NAME, listeners, true);
            builder.addFormatReaderSIS(GeoHeifStoreProvider.NAME);
            builder.addResourceScope(ScopeCode.COVERAGE, null);
            getIdentifier().ifPresent((id) -> builder.addIdentifier(id, MetadataBuilder.Scope.ALL));
            metadata = customizer.customize(new CoverageModifier.Source(this), builder.build());
        }
        return metadata;
    }

    /**
     * Returns <abbr>HEIF</abbr> boxes and their fields as a tree for debugging purpose.
     * The boxes appear in the order they are declared in the file.
     * Only the boxes and fields recognized by this <abbr>HEIF</abbr> store implementation are shown.
     * The table contains the following columns:
     *
     * <ul>
     *   <li>{@linkplain TableColumn#NAME}</li>
     *   <li>{@linkplain TableColumn#VALUE}</li>
     *   <li>{@linkplain TableColumn#VALUE_AS_TEXT}</li>
     * </ul>
     *
     * <h4>Usage and performance note</h4>
     * This method should not be invoked during normal operations. The {@linkplain #getMetadata() standard metadata}
     * are preferred because they allow abstraction of data format details. Native metadata should be used only when
     * an information does not appear in standard metadata, or for debugging purposes.
     *
     * <p>Since this method should not be invoked in normal operations, it has not been tuned for performance.
     * Invoking this method may cause a lot of {@linkplain java.nio.channels.SeekableByteChannel#position(long)
     * seek operations}.</p>
     *
     * @return resources information structured in an implementation-specific way.
     * @throws DataStoreException if an error occurred while reading the metadata.
     *
     * @todo It is sometime possible, through the tree cell values, to modify the content of internal arrays.
     *       This is unsafe, we should makes this implementation safer before this module is released.
     */
    @Override
    public synchronized Optional<TreeTable> getNativeMetadata() throws DataStoreException {
        return Optional.of(root().toTree(getDisplayName(), true));
    }

    /**
     * Returns the children resources of this aggregate.
     *
     * @return all children resources that are components of this aggregate.
     * @throws DataStoreException if an error occurred while fetching the components.
     */
    @Override
    public synchronized Collection<? extends Resource> components() throws DataStoreException {
        if (content == null) try {
            final var builder = new ResourceBuilder(this, root());
            content = builder.build();
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
        return Containers.viewAsUnmodifiableList(content);
    }

    /**
     * Logs a warning with a message built from localized resources. This method pretends that the
     * warning has been emitted by {@link #components()}. It should be the case, but often indirectly.
     *
     * @param  errorKey  one of {@link Errors.Keys} values.
     * @param  args   the parameter for the log message, which may be an array.
     */
    final void warning(final short errorKey, final Object args) {
        warning(Errors.forLocale(getLocale()).createLogRecord(Level.WARNING, errorKey, args));
    }

    /**
     * Logs a warning emitted (usually indirectly) by {@link #components()}.
     */
    final void warning(final LogRecord record) {
        record.setSourceClassName(GeoHeifStore.class.getCanonicalName());
        record.setSourceMethodName("components");
        listeners.warning(record);
    }

    /**
     * The listeners where to send warnings.
     */
    final StoreListeners listeners() {
        return listeners;
    }

    /**
     * Closes this GeoHEIF store and releases any underlying resources.
     * This method can be invoked asynchronously for interrupting a long reading process.
     *
     * @throws DataStoreException if an error occurred while closing the GeoHEIF file.
     */
    @Override
    @SuppressWarnings("ConvertToTryWithResources")
    public void close() throws DataStoreException {
        try {
            listeners.close();                  // Should never fail.
            ChannelDataInput in = input;
            if (in != null) {
                input = null;
                in.channel.close();
            }
        } catch (IOException e) {
            throw new DataStoreException(e);
        } finally {
            synchronized (this) {
                root     = null;
                content  = null;
                metadata = null;
            }
        }
    }
}
