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
package org.apache.sis.storage.folder;

import java.util.EnumSet;
import java.util.Locale;
import java.util.logging.Logger;
import java.time.ZoneId;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardOpenOption;
import org.opengis.util.InternationalString;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.storage.base.URIDataStoreProvider;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.StoreMetadata;
import org.apache.sis.storage.base.StoreUtilities;
import org.apache.sis.setup.OptionKey;


/**
 * The provider of {@link Store} instances. This provider is intentionally registered with lowest priority
 * because it will open any directory, which may conflict with other providers opening only directory with
 * some specific content.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
@StoreMetadata(formatName    = StoreProvider.NAME,
               capabilities  = {Capability.READ, Capability.WRITE},
               resourceTypes = {Aggregate.class, FeatureSet.class, GridCoverageResource.class},
               yieldPriority = true)
public final class StoreProvider extends DataStoreProvider {
    /**
     * A short name or abbreviation for the data format.
     */
    static final String NAME = "folder";

    /**
     * The logger used by folder stores.
     *
     * @see #getLogger()
     */
    private static final Logger LOGGER = Logger.getLogger("org.apache.sis.storage.folder");

    /**
     * Description of the parameter for formatting conventions of dates and numbers.
     */
    private static final ParameterDescriptor<Locale> LOCALE;

    /**
     * Description of the parameter for timezone of dates in the data store.
     */
    private static final ParameterDescriptor<ZoneId> TIMEZONE;

    /**
     * Description of the parameter for character encoding used by the data store.
     */
    private static final ParameterDescriptor<Charset> ENCODING;

    /**
     * Description of the parameter for name of format or {@code DataStoreProvider}
     * to use for reading or writing the directory content.
     */
    private static final ParameterDescriptor<String> FORMAT;

    /**
     * The group of parameter descriptors to be returned by {@link #getOpenParameters()}.
     */
    static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterDescriptor<Path> location;
        final ParameterBuilder builder = new ParameterBuilder();
        final InternationalString remark = Resources.formatInternational(Resources.Keys.UsedOnlyIfNotEncoded);
        ENCODING   = annotate(builder, URIDataStoreProvider.ENCODING, remark);
        LOCALE     = builder.addName("locale"  ).setDescription(Resources.formatInternational(Resources.Keys.DataStoreLocale  )).setRemarks(remark).create(Locale.class,   null);
        TIMEZONE   = builder.addName("timezone").setDescription(Resources.formatInternational(Resources.Keys.DataStoreTimeZone)).setRemarks(remark).create(ZoneId.class, null);
        FORMAT     = builder.addName("format"  ).setDescription(Resources.formatInternational(Resources.Keys.DirectoryContentFormatName)).create(String.class, null);
        location   = new ParameterBuilder(URIDataStoreProvider.LOCATION_PARAM).create(Path.class, null);
        PARAMETERS = builder.addName(NAME).createGroup(location, LOCALE, TIMEZONE, ENCODING, FORMAT, URIDataStoreProvider.CREATE_PARAM);
    }

    /**
     * Creates a parameter descriptor equals to the given one except for the remarks which are set to the given value.
     */
    private static <T> ParameterDescriptor<T> annotate(ParameterBuilder builder, ParameterDescriptor<T> e, InternationalString remark) {
        return builder.addName(e.getName()).setDescription(e.getDescription().orElse(null)).setRemarks(remark).create(e.getValueClass(), null);
    }

    /**
     * Creates a new provider.
     */
    public StoreProvider() {
    }

    /**
     * Returns a short name or abbreviation for the data format.
     *
     * @return a short name or abbreviation for the data format.
     */
    @Override
    public String getShortName() {
        return NAME;
    }

    /**
     * Returns a description of all parameters accepted by this provider for opening a data store.
     *
     * @return description of the parameters for opening a {@link DataStore}.
     */
    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS;
    }

    /**
     * Returns {@link ProbeResult#SUPPORTED} if the given storage appears to be a folder.
     * Returning {@code SUPPORTED} from this method does not guarantee that reading or writing will succeed,
     * only that there appears to be a reasonable chance of success based on a brief inspection of the storage
     * characteristics.
     *
     * @return {@link ProbeResult#SUPPORTED} if the given storage seems to be readable as a folder.
     * @throws DataStoreException if an I/O error occurred.
     */
    @Override
    public ProbeResult probeContent(StorageConnector connector) throws DataStoreException {
        try {
            final Path path = connector.getStorageAs(Path.class);
            if (path != null && Files.isDirectory(path)) {
                return ProbeResult.SUPPORTED;
            }
        } catch (FileSystemNotFoundException e) {
            Logging.recoverableException(getLogger(), StoreProvider.class, "probeContent", e);
            // Nothing we can do, may happen often.
        }
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

    /**
     * Returns a data store implementation associated with this provider.
     * The data store created by this method will try to auto-detect the format of every files in the directory.
     * For exploring only the file of a known format, use {@link #open(ParameterValueGroup)} instead.
     *
     * @param  connector  information about the storage (URL, path, <i>etc</i>).
     * @return a data store implementation associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final StorageConnector connector) throws DataStoreException {
        return open(connector, null, EnumSet.noneOf(StandardOpenOption.class));
    }

    /**
     * Shared implementation of public {@code open(…)} methods.
     * Note that this method may modify the given {@code options} set for its own purpose.
     *
     * @param  connector  information about the storage (URL, path, <i>etc</i>).
     * @param  format     format name for directory content, or {@code null} if unspecified.
     * @param  options    whether to create a new directory, overwrite existing content, <i>etc</i>.
     */
    private DataStore open(final StorageConnector connector, final String format, final EnumSet<StandardOpenOption> options)
            throws DataStoreException
    {
        /*
         * Determine now the provider to use for directory content. We do that for determining if the component
         * has write capability. If not, then the WRITE, CREATE and related options will be ignored.  If we can
         * not determine whether the component store has write capabilities (i.e. if canWrite(…) returns null),
         * assume that the answer is "yes".
         */
        final DataStoreProvider componentProvider;
        if (format != null) {
            componentProvider = StoreUtilities.providerByFormatName(format.trim());
            if (Boolean.FALSE.equals(StoreUtilities.canWrite(componentProvider.getClass()))) {
                options.clear();            // No write capability.
            }
        } else {
            componentProvider = null;
            options.clear();                // Cannot write if we don't know the components format.
        }
        final Path path = connector.getStorageAs(Path.class);
        final Store store;
        try {
            /*
             * If the user asked to create a new directory, we need to perform this task before
             * to create the Store (otherwise constructor will fail with NoSuchFileException).
             * In the particular case of CREATE_NEW, we unconditionally attempt to create the
             * directory in order to rely on the atomic check performed by Files.createDirectory(…).
             */
            boolean isNew = false;
            if (options.contains(StandardOpenOption.CREATE)) {
                if (options.contains(StandardOpenOption.CREATE_NEW) || Files.notExists(path)) {
                    Files.createDirectory(path);                        // IOException if the directory already exists.
                    isNew = true;
                }
            }
            if (isNew || (options.contains(StandardOpenOption.WRITE) && Files.isWritable(path))) {
                store = new WritableStore(this, connector, path, componentProvider);  // May throw NoSuchFileException.
            } else {
                store = new Store(this, connector, path, componentProvider);          // May throw NoSuchFileException.
            }
            /*
             * If there is a destructive operation to perform (TRUNCATE_EXISTING), do it last only
             * after we have successfully created the data store. The check for directory existence
             * is also done after creation to be sure to check the path used by the store.
             */
            if (!Files.isDirectory(path)) {
                throw new DataStoreException(Resources.format(Resources.Keys.FileIsNotAResourceDirectory_1, path));
            }
            if (options.contains(StandardOpenOption.TRUNCATE_EXISTING)) {
                WritableStore.deleteRecursively(path, false);
            }
        } catch (IOException e) {
            /*
             * In case of error, Java FileSystem implementation tries to throw a specific exception
             * (NoSuchFileException or FileAlreadyExistsException), but this is not guaranteed.
             */
            int isDirectory = 0;
            final short errorKey;
            if (e instanceof FileAlreadyExistsException) {
                if (path != null && Files.isDirectory(path)) {
                    isDirectory = 1;
                }
                errorKey = Resources.Keys.FileAlreadyExists_2;
            } else if (e instanceof NoSuchFileException) {
                errorKey = Resources.Keys.NoSuchResourceDirectory_1;
            } else {
                errorKey = Resources.Keys.CanNotCreateFolderStore_1;
            }
            throw new DataStoreException(Resources.format(errorKey,
                    (path != null) ? path : connector.getStorageName(), isDirectory), e);
        }
        connector.closeAllExcept(path);
        return store;
    }

    /**
     * Returns a data store implementation associated with this provider for the given parameters.
     *
     * @return a folder data store implementation for the given parameters.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final ParameterValueGroup parameters) throws DataStoreException {
        final StorageConnector connector = URIDataStoreProvider.connector(this, parameters);
        final Parameters pg = Parameters.castOrWrap(parameters);
        connector.setOption(OptionKey.LOCALE,   pg.getValue(LOCALE));
        connector.setOption(OptionKey.TIMEZONE, pg.getValue(TIMEZONE));
        connector.setOption(OptionKey.ENCODING, pg.getValue(ENCODING));
        final EnumSet<StandardOpenOption> options = EnumSet.of(StandardOpenOption.WRITE);
        if (Boolean.TRUE.equals(pg.getValue(URIDataStoreProvider.CREATE_PARAM))) {
            options.add(StandardOpenOption.CREATE);
        }
        return open(connector, pg.getValue(FORMAT), options);
    }

    /**
     * {@return the logger used by folder stores}.
     */
    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
