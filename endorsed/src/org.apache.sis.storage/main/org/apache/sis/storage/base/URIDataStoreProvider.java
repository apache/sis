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
package org.apache.sis.storage.base;

import java.util.Optional;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.Buffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.charset.Charset;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.IllegalOpenParameterException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.io.stream.ChannelDataOutput;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.logging.Logging;


/**
 * Provider for {@link URIDataStore} instances.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class URIDataStoreProvider extends DataStoreProvider {
    /**
     * Description of the {@value #LOCATION} parameter.
     */
    public static final ParameterDescriptor<URI> LOCATION_PARAM;

    /**
     * Description of the "metadata" parameter.
     */
    public static final ParameterDescriptor<Path> METADATA_PARAM;

    /**
     * Description of the optional {@value #CREATE} parameter, which may be present in writable data store.
     * This parameter is not included in the descriptor created by {@link #build(ParameterBuilder)} default
     * implementation. It is subclass responsibility to add it if desired, only if supported.
     */
    public static final ParameterDescriptor<Boolean> CREATE_PARAM;

    /**
     * Description of the optional parameter for character encoding used by the data store.
     * This parameter is not included in the descriptor created by {@link #build(ParameterBuilder)}
     * default implementation. It is subclass responsibility to add it if desired.
     */
    public static final ParameterDescriptor<Charset> ENCODING;
    static {
        final ParameterBuilder builder = new ParameterBuilder();
        ENCODING       = builder.addName("encoding").setDescription(Resources.formatInternational(Resources.Keys.DataStoreEncoding)).create(Charset.class, null);
        CREATE_PARAM   = builder.addName( CREATE   ).setDescription(Resources.formatInternational(Resources.Keys.DataStoreCreate  )).create(Boolean.class, null);
        METADATA_PARAM = builder.addName("metadata").setDescription(Resources.formatInternational(Resources.Keys.MetadataLocation )).create(Path.class, null);
        LOCATION_PARAM = builder.addName( LOCATION ).setDescription(Resources.formatInternational(Resources.Keys.DataStoreLocation)).setRequired(true).create(URI.class, null);
    }

    /**
     * The parameter descriptor to be returned by {@link #getOpenParameters()}.
     * Created when first needed.
     */
    private volatile ParameterDescriptorGroup openDescriptor;

    /**
     * Creates a new provider.
     */
    protected URIDataStoreProvider() {
    }

    /**
     * Returns a description of all parameters accepted by this provider for opening a data store.
     * This method creates the descriptor only when first needed. Subclasses can override the
     * {@link #build(ParameterBuilder)} method if they need to modify the descriptor to create.
     *
     * @return description of the parameters required or accepted for opening a {@link DataStore}.
     */
    @Override
    public final ParameterDescriptorGroup getOpenParameters() {
        ParameterDescriptorGroup desc = openDescriptor;
        if (desc == null) {
            openDescriptor = desc = build(new ParameterBuilder().addName(getShortName()));
        }
        return desc;
    }

    /**
     * Invoked by {@link #getOpenParameters()} the first time that a parameter descriptor needs to be created.
     * When invoked, the parameter group name is set to a name derived from the {@link #getShortName()} value.
     * The default implementation creates a group containing {@link #LOCATION_PARAM} and {@link #METADATA_PARAM}.
     * Subclasses can override if they need to create a group with more parameters.
     *
     * @param  builder  the builder to use for creating parameter descriptor. The group name is already set.
     * @return the parameters descriptor created from the given builder.
     */
    protected ParameterDescriptorGroup build(final ParameterBuilder builder) {
        return builder.createGroup(LOCATION_PARAM, METADATA_PARAM);
    }

    /**
     * Convenience method creating a parameter descriptor containing only {@link #LOCATION_PARAM}.
     * This convenience method is used for public providers that cannot extend this
     * {@code URIDataStoreProvider} class because it is internal.
     *
     * @param  name  short name of the data store format.
     * @return the descriptor for open parameters.
     *
     * @todo Verify if non-exported classes in JDK9 are hidden from Javadoc, like package-private classes.
     *       If true, we could remove this hack and extend {@code URIDataStore} even in public classes.
     */
    public static ParameterDescriptorGroup descriptor(final String name) {
        return new ParameterBuilder().addName(name).createGroup(LOCATION_PARAM);
    }

    /**
     * Returns the location (path, URL, URI, <i>etc.</i>) of the given resource.
     * The type of the returned object can be any of the types documented in {@link DataStoreProvider#LOCATION}.
     *
     * @param  resource  the resource for which to get the location, or {@code null}.
     * @return location of the given resource, or {@code null} if none.
     * @throws DataStoreException if an error on the file system prevent the creation of the path.
     */
    public static Object location(final Resource resource) throws DataStoreException {
        if (resource instanceof DataStore) {
            final Optional<ParameterValueGroup> p = ((DataStore) resource).getOpenParameters();
            if (p.isPresent()) try {
                return p.get().parameter(DataStoreProvider.LOCATION).getValue();
            } catch (ParameterNotFoundException e) {
                /*
                 * This exception should not happen often since the "location" parameter is recommended.
                 * Note that it does not mean the same thing as "parameter provided but value is null".
                 * In that later case we want to return the null value as specified in the parameters.
                 */
                Logging.recoverableException(StoreUtilities.LOGGER, URIDataStore.class, "location", e);
            }
        }
        /*
         * This fallback should not happen with `URIDataStore` implementation because the "location" parameter
         * is always present even if null. This fallback is for resources implementated by different classes.
         * The first path is presumed the main file.
         */
        return resource.getFileSet().flatMap((files) -> files.getPaths().stream().findFirst()).orElse(null);
    }

    /**
     * Creates a storage connector initialized to the location declared in given parameters.
     * This convenience method does not set any other parameters.
     * In particular, reading (or ignoring) the {@value #CREATE} parameter is left to callers,
     * because not all implementations may create data stores with {@link java.nio.file.StandardOpenOption}.
     *
     * @param  provider    the provider for which to create a storage connector (for error messages).
     * @param  parameters  the parameters to use for creating a storage connector.
     * @return the storage connector initialized to the location specified in the parameters.
     * @throws IllegalOpenParameterException if no {@value #LOCATION} parameter has been found.
     */
    public static StorageConnector connector(final DataStoreProvider provider, final ParameterValueGroup parameters)
            throws IllegalOpenParameterException
    {
        ParameterNotFoundException cause = null;
        if (parameters != null) try {
            final Object location = parameters.parameter(LOCATION).getValue();
            if (location != null) {
                return new StorageConnector(location);
            }
        } catch (ParameterNotFoundException e) {
            cause = e;
        }
        throw new IllegalOpenParameterException(Resources.format(Resources.Keys.UndefinedParameter_2,
                    provider.getShortName(), LOCATION), cause);
    }

    /**
     * Returns {@code true} if the open options contains {@link StandardOpenOption#WRITE}
     * or if the storage type is some kind of output stream. An ambiguity may exist between
     * the case when a new file would be created and when an existing file would be updated.
     * This ambiguity is resolved by the {@code ifNew} argument:
     * if {@code false}, then the two cases are not distinguished.
     * If {@code true}, then this method returns {@code true} only if a new file would be created.
     *
     * @param  connector  the connector to use for opening a file.
     * @param  ifNew  whether to return {@code true} only if a new file would be created.
     * @return whether the specified connector should open a writable data store.
     * @throws DataStoreException if the storage object has already been used and cannot be reused.
     */
    public static boolean isWritable(final StorageConnector connector, final boolean ifNew) throws DataStoreException {
        final Object storage = connector.getStorage();
        if (storage instanceof OutputStream || storage instanceof DataOutput) return true;    // Must be tested first.
        if (storage instanceof InputStream  || storage instanceof DataInput)  return false;   // Ignore options.
        final OpenOption[] options = connector.getOption(OptionKey.OPEN_OPTIONS);
        if (ArraysExt.contains(options, StandardOpenOption.WRITE)) {
            if (!ifNew || ArraysExt.contains(options, StandardOpenOption.TRUNCATE_EXISTING)) {
                return true;
            }
            if (ArraysExt.contains(options, StandardOpenOption.CREATE_NEW)) {
                return IOUtilities.isKindOfPath(storage);
            }
            if (ArraysExt.contains(options, StandardOpenOption.CREATE)) try {
                final Path path = connector.getStorageAs(Path.class);
                return (path != null) && IOUtilities.isAbsentOrEmpty(path);
            } catch (IOException e) {
                throw new DataStoreException(e);
            }
        }
        return false;
    }

    /**
     * Creates a new output stream and set by the order to native order if is was not explicitly specified by the user.
     * The byte order is considered explicitly specified if the storage type is one of the types were the user could
     * have specified that order.
     *
     * @param  connector  the connector to use for opening a file.
     * @param  format     short name or abbreviation of the data format (e.g. "CSV", "GML", "WKT", <i>etc</i>).
     *                    Used for information purpose in error messages if needed.
     * @return the output stream.
     * @throws DataStoreException if an error occurred while creating the output stream.
     */
    public static ChannelDataOutput openAndSetNativeByteOrder(final StorageConnector connector, final String format)
            throws DataStoreException
    {
        final Object storage = connector.getStorage();
        final ChannelDataOutput output = connector.commit(ChannelDataOutput.class, format);
        if (output != storage && !(storage instanceof Buffer)) {
            output.buffer.order(ByteOrder.nativeOrder());
        }
        return output;
    }
}
