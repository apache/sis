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

import java.util.EnumSet;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.OptionKey;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.IllegalOpenParameterException;
import org.apache.sis.io.stream.ChannelDataOutput;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.util.ArraysExt;


/**
 * Provider for {@link URIDataStore} instances.
 * All implementations <em>shall</em> supports the {@value #LOCATION} parameter.
 * All other parameters are optional.
 *
 * <p>This class contains also a few static methods for data stores which read from an <abbr>URI</abbr>,
 * not necessarily data stores extending {@link URIDataStore}.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class URIDataStoreProvider extends DataStoreProvider {
    /**
     * Name of the parameter for the name of the {@code DataStoreProvider} on which to delegate the work.
     * This is used by data stores that are container for other data stores.
     * It may be, for example, the data store for a whole directory content.
     */
    protected static final String FORMAT = "format";

    /**
     * Options that are supported. Shall contain at least {@link URIDataStoreOption#LOCATION}.
     * For thread-safety reason, this set shall not be modified after construction.
     */
    protected final EnumSet<URIDataStoreOption> supportedOptions;

    /**
     * The parameter descriptor to be returned by {@link #getOpenParameters()}.
     * Created when first needed.
     *
     * @see #getOpenParameters()
     */
    private volatile ParameterDescriptorGroup openDescriptor;

    /**
     * Creates a new provider which initially supports only the {@value #LOCATION} parameter.
     * The {@link #supportedOptions} set is initialized to {@link URIDataStoreOption#LOCATION}.
     * Subclass shall add all other supported options (if any) in their constructor,
     * and shall not modify that set after construction.
     */
    protected URIDataStoreProvider() {
        supportedOptions = EnumSet.of(URIDataStoreOption.LOCATION);
    }

    /**
     * Returns a description of all parameters accepted by this provider for opening a data store.
     * This method creates the descriptor only when first needed. Subclasses should initialize the
     * content of the {@link #supportedOptions} set if they need to modify the descriptor.
     *
     * @return description of the parameters required or accepted for opening a {@link DataStore}.
     */
    @Override
    public final ParameterDescriptorGroup getOpenParameters() {
        ParameterDescriptorGroup desc = openDescriptor;
        if (desc == null) {
            // No synchronization because not a big issue if created twice.
            openDescriptor = desc = createOpenParameters(new ParameterBuilder().addName(getShortName()));
        }
        return desc;
    }

    /**
     * Invoked by {@link #getOpenParameters()} the first time that a parameter descriptor needs to be created.
     * When invoked, the parameter group name is set to a name derived from the {@link #getShortName()} value.
     * The default implementation creates a group containing {@link #supportedOptions}.
     * Subclasses can override if they need to create a group with different parameters.
     *
     * @param  builder  the builder to use for creating parameter descriptor. The group name is already set.
     * @return the parameters descriptor created from the given builder.
     *
     * @see URIDataStoreOption#createForLocationOnly(String)
     */
    protected ParameterDescriptorGroup createOpenParameters(final ParameterBuilder builder) {
        return builder.createGroup(supportedOptions.stream()
                .map(URIDataStoreOption::getParameterDescriptor)
                .toArray(ParameterDescriptor[]::new));
    }

    /**
     * Creates a data store from the given parameters by delegating to {@link #open(StorageConnector)}.
     * The {@value #LOCATION} parameter is unconditionally requested. The other parameters to be requested
     * are specified in the {@link #supportedOptions} set. Missing optional parameters are ignored.
     *
     * @param  parameters  opening parameters as defined by {@link #getOpenParameters()}.
     * @return a data store implementation associated with this provider for the given parameters.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final ParameterValueGroup parameters) throws DataStoreException {
        return open(connector(parameters, null));
    }

    /**
     * Creates a storage connector initialized to the location declared in the given parameters.
     * The {@value #LOCATION} parameter is unconditionally requested. The other parameters to be
     * requested are specified in {@link #supportedOptions}.
     * Missing optional parameters are ignored.
     *
     * @param  parameters   the parameters to use for creating a storage connector.
     * @param  openOptions  where to store open options, or {@code null} for storing them in the storage connector.
     * @return the storage connector initialized to the location specified in the parameters.
     * @throws IllegalOpenParameterException if no {@value #LOCATION} parameter has been found.
     */
    protected final StorageConnector connector(final ParameterValueGroup parameters,
                                               final EnumSet<StandardOpenOption> openOptions)
            throws IllegalOpenParameterException
    {
        return URIDataStoreOption.connector(this, parameters, supportedOptions, openOptions);
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
     *
     * @see StoreUtilities#canWrite(Class)
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
     * Creates a new output stream and set the order to native order if is was not explicitly specified by the user.
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

    /**
     * Returns {@code true} if a sequence of bytes in the given encoding can be decoded as if they were ASCII,
     * ignoring values greater than 127. In case of doubt, this method conservatively returns {@code false}.
     *
     * @param  encoding  the encoding.
     * @return whether bytes less than 128 can be interpreted as ASCII.
     */
    public static boolean basedOnASCII(final Charset encoding) {
        return ArraysExt.containsIgnoreCase(basedOnASCII, encoding.name());
    }

    /** Names of encoding where bytes less than 128 can be interpreted as <abbr>ASCII</abbr>. */
    private static final String[] basedOnASCII = {"US-ASCII", "ISO-8859-1", "UTF-8"};
}
