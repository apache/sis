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

import java.util.Set;
import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.logging.Filter;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.charset.Charset;
import org.opengis.util.GenericName;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.identification.DataIdentification;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.WritableFeatureSet;
import org.apache.sis.storage.UnsupportedStorageException;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.metadata.internal.shared.Identifiers;
import org.apache.sis.system.Configuration;
import org.apache.sis.system.Modules;
import org.apache.sis.util.resources.Errors;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.metadata.iso.identification.AbstractIdentification;


/**
 * Utility methods related to {@link DataStore}s, {@link DataStoreProvider}s and {@link Resource}s.
 * This is not a committed API; any method in this class may change in any future Apache SIS version.
 * Some methods may also move in public API if we feel confident enough.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class StoreUtilities {
    /**
     * Whether to allow computation of statistics when no minimum/maximum values can be determined.
     * This is a costly operation because it requires loading all data, so any code enabled by this
     * flag should be executed in last resort only.
     *
     * <p>This flag can be set to {@code true} for exploring data that we cannot visualize otherwise.
     * But it should generally stay to {@code false}, because otherwise browsing resource metadata can
     * become as costly (slow and high memory usage) as visualizing the full raster.</p>
     *
     * <p>In addition of possible performance degradations, setting this flag to {@code true} can also prevent
     * {@link org.apache.sis.storage.aggregate.CoverageAggregator} to group coverages that should be together.
     * This is because using statistics may cause {@link org.apache.sis.coverage.SampleDimension} instances to
     * have different sample value ranges for each coverage, which cause {@code CoverageAggregator} to consider
     * that that cannot be aggregated together.</p>
     */
    @Configuration
    public static final boolean ALLOW_LAST_RESORT_STATISTICS = false;

    /**
     * Logger for the {@value Modules#STORAGE} module. This is used when no more specific logger is available,
     * or if the more specific logger is not appropriate (e.g. because the log message come from base class).
     */
    public static final Logger LOGGER = Logger.getLogger(Modules.STORAGE);

    /**
     * Names of encoding where bytes less than 128 can be interpreted as ASCII.
     *
     * @see #basedOnASCII(Charset)
     */
    private static final Set<String> basedOnASCII = Set.of("US-ASCII", "ISO-8859-1", "UTF-8");

    /**
     * Do not allow instantiation of this class.
     */
    private StoreUtilities() {
    }

    /**
     * Returns an identifier for the given data store provider, or {@code null} if none.
     * The data store identifier should be the format name, but this is not guaranteed.
     * It current version, it is not even guaranteed to be unique.
     *
     * <p>This method will need to be revisited since {@link DataStoreProvider#getShortName()} said that
     * the short name is not to be used as an identifier. In the meantime, we use this method as a way
     * to keep trace of the location in the code where an identifier is desired.</p>
     *
     * @param  provider  the provider for which to get an identifier, or {@code null}.
     * @return an identifier for the given data store, or {@code null}.
     */
    public static String getFormatName(final DataStoreProvider provider) {
        if (provider != null) {
            final StoreMetadata md = provider.getClass().getAnnotation(StoreMetadata.class);
            if (md != null) {
                return md.formatName();
            }
            return provider.getShortName();
        }
        return null;
    }

    /**
     * Returns an identifier for a resource having the given metadata, or {@code null} if none.
     * This method checks the information returned by {@link Metadata#getIdentificationInfo()},
     * with precedence to {@link DataIdentification} over other kinds of {@link Identification}.
     * This method does not check for ambiguity (if there is more than one identification info).
     *
     * @param  metadata  the metadata from which to get a data identifier, or {@code null}.
     * @param  unicode   whether to restrict to valid Unicode identifiers.
     * @return a data identifier, or {@code null} if none.
     *
     * @see DataStore#getIdentifier()
     * @see org.apache.sis.metadata.iso.citation.Citations#removeIgnorableCharacters(String)
     */
    private static String getAnyIdentifier(final Metadata metadata, final boolean unicode) {
        String fallback = null;
        if (metadata != null) {
            for (final Identification md : metadata.getIdentificationInfo()) {
                String id = Identifiers.getIdentifier(md.getCitation(), unicode);
                if (id != null) {
                    if (md instanceof DataIdentification) {
                        return id;
                    } else if (fallback == null) {
                        fallback = id;
                    }
                }
            }
        }
        return fallback;
    }

    /**
     * Returns a short label for the given resource. This method returns the display name if possible,
     * or the identifier otherwise. If neither a display name, identifier or title can be found, then
     * this method returns the kind of resource implemented by the given object.
     *
     * @param  resource  the resource for which to get a label.
     * @return a human-readable label for the given resource (not to be used as an identifier).
     * @throws DataStoreException if an error occurred while fetching metadata.
     */
    public static String getLabel(final Resource resource) throws DataStoreException {
        String title = null;
        if (resource instanceof DataStore) {
            title = ((DataStore) resource).getDisplayName();
        }
        if (title == null) {
            final Optional<GenericName> identifier = resource.getIdentifier();
            if (identifier.isPresent()) {
                title = identifier.get().toString();
            } else {
                title = getAnyIdentifier(resource.getMetadata(), false);
                if (title == null) {
                    title = Classes.getShortName(getInterface(resource.getClass()));
                }
            }
        }
        return title;
    }

    /**
     * Returns the spatiotemporal envelope of the given metadata.
     * This method computes the union of all {@link GeographicBoundingBox} in the metadata, assuming the
     * {@linkplain org.apache.sis.referencing.CommonCRS#defaultGeographic() default geographic CRS}
     * (usually WGS 84).
     *
     * @param  metadata  the metadata from which to compute the envelope, or {@code null}.
     * @return the spatiotemporal extent, or {@code null} if none.
     */
    public static Envelope getEnvelope(final Metadata metadata) {
        GeneralEnvelope bounds = null;
        if (metadata != null) {
            for (final Identification identification : metadata.getIdentificationInfo()) {
                if (!(identification instanceof AbstractIdentification)) {
                    continue;       // Following cast is specific to GeoAPI 3.0 branch.
                }
                for (final Extent extent : ((AbstractIdentification) identification).getExtents()) {
                    for (final GeographicExtent ge : extent.getGeographicElements()) {
                        if (ge instanceof GeographicBoundingBox) {
                            final GeneralEnvelope env = new GeneralEnvelope((GeographicBoundingBox) ge);
                            if (bounds == null) {
                                bounds = env;
                            } else {
                                bounds.add(env);
                            }
                        }
                    }
                }
            }
        }
        return bounds;
    }

    /**
     * Returns the most specific interface implemented by the given class.
     * For indicative purpose only, as this method has arbitrary behavior if more than one leaf is found.
     *
     * @param  implementation  the implementation class.
     * @return the most specific resource interface.
     */
    public static Class<? extends Resource> getInterface(final Class<? extends Resource> implementation) {
        final Class<? extends Resource>[] types = Classes.getLeafInterfaces(implementation, Resource.class);
        Class<? extends Resource> type = null;
        for (int i=types.length; --i >= 0;) {
            type = types[i];
            if (FeatureSet.class.isAssignableFrom(type)) break;              // Arbitrary precedence rule.
        }
        return type;                // Should never be null since the 'types' array should never be empty.
    }

    /**
     * Returns the possible suffixes of the files written by the data store created by the given provider.
     * If the file suffixes are unknown, returns an empty array.
     *
     * @param  provider  class of the provider for which to determine if it has write capability, or {@code null}.
     * @return the file suffixes, or an empty array if none or if the suffixes cannot be determined.
     *
     * @see StoreMetadata#fileSuffixes()
     */
    public static String[] getFileSuffixes(final Class<? extends DataStoreProvider> provider) {
        if (provider != null) {
            final StoreMetadata md = provider.getAnnotation(StoreMetadata.class);
            if (md != null) return md.fileSuffixes();
        }
        return CharSequences.EMPTY_ARRAY;
    }

    /**
     * Returns whether the given store has write capability.
     * In case of doubt, this method returns {@code null}.
     *
     * @param  provider  class of the provider for which to determine if it has write capability, or {@code null}.
     * @return whether the data store has write capability, or {@code null} if it cannot be determined.
     *
     * @see StoreMetadata#capabilities()
     */
    public static Boolean canWrite(final Class<? extends DataStoreProvider> provider) {
        if (provider != null) {
            StoreMetadata md = provider.getAnnotation(StoreMetadata.class);
            if (md != null) {
                return ArraysExt.contains(md.capabilities(), Capability.WRITE);
            }
        }
        return null;
    }

    /**
     * Converts the given sequence of options into a simplified set of standard options.
     * The returned set can contain combinations of
     * {@link StandardOpenOption#WRITE},
     * {@link StandardOpenOption#CREATE CREATE},
     * {@link StandardOpenOption#CREATE_NEW CREATE_NEW} and
     * {@link StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING}.
     * If the set is empty, then the data store should be read-only.
     * If both {@code TRUNCATE_EXISTING} and {@code CREATE_NEW} are specified,
     * then {@code CREATE_NEW} has precedence.
     * More specifically:
     *
     * <p>{@link StandardOpenOption#WRITE}<br>
     * means that the {@link DataStore} should be opened as writable resource.</p>
     *
     * <p>{@link StandardOpenOption#CREATE}<br>
     * means that the {@link DataStore} is allowed to create new files.
     * If this option is present, then {@code WRITE} is also present.
     * If this option is absent, then writable data stores should not create any new file.
     * This flag can be tested as below (this cover both the read-only case and the writable
     * case where the files must exist):</p>
     *
     * {@snippet lang="java" :
     *     if (!options.contains(StandardOpenOption.CREATE)) {
     *         // Throw an exception if the file does not exist.
     *     }
     *     }
     *
     * <p>{@link StandardOpenOption#CREATE_NEW}<br>
     * means that the {@link DataStore} should fail to open if the file already exists.
     * This mode is used when creating new writable resources, for making sure that we
     * do not modify existing resources.
     * If this option is present, then {@code WRITE} and {@code CREATE} are also present.</p>
     *
     * <p>{@link StandardOpenOption#TRUNCATE_EXISTING}<br>
     * means that the {@link DataStore} should overwrite the content of any pre-existing resources.
     * If this option is present, then {@code WRITE} and {@code CREATE} are also present.</p>
     *
     * @param  options  the open options, or {@code null}.
     * @return the open options as a bitmask.
     */
    @SuppressWarnings("fallthrough")
    public static EnumSet<StandardOpenOption> toStandardOptions(final OpenOption[] options) {
        final EnumSet<StandardOpenOption> set = EnumSet.noneOf(StandardOpenOption.class);
        if (options != null) {
            for (final OpenOption op : options) {
                if (op instanceof StandardOpenOption) {
                    switch ((StandardOpenOption) op) {  // Fallthrough in every cases.
                        case CREATE_NEW:         set.add(StandardOpenOption.CREATE_NEW);
                        case TRUNCATE_EXISTING:  set.add(StandardOpenOption.TRUNCATE_EXISTING);
                        case CREATE:             set.add(StandardOpenOption.CREATE);
                        case APPEND: case WRITE: set.add(StandardOpenOption.WRITE);
                    }
                }
            }
            if (set.contains(StandardOpenOption.CREATE_NEW)) {
                set.remove(StandardOpenOption.TRUNCATE_EXISTING);
            }
        }
        return set;
    }

    /**
     * Returns {@code true} if a sequence of bytes in the given encoding can be decoded as if they were ASCII,
     * ignoring values greater than 127. In case of doubt, this method conservatively returns {@code false}.
     *
     * @param  encoding  the encoding.
     * @return whether bytes less than 128 can be interpreted as ASCII.
     */
    public static boolean basedOnASCII(final Charset encoding) {
        return basedOnASCII.contains(encoding.name());
    }

    /**
     * Returns a provider for the given format name.
     *
     * @param  format  name of the format for which to get a provider.
     * @return first provider found for the given format name.
     * @throws UnsupportedStorageException if no provider is found for the specified format.
     *
     * @see StoreMetadata#formatName()
     */
    public static DataStoreProvider providerByFormatName(final String format) throws UnsupportedStorageException {
        for (DataStoreProvider provider : DataStores.providers()) {
            if (format.equalsIgnoreCase(getFormatName(provider))) {
                return provider;
            }
        }
        throw new UnsupportedStorageException(Errors.format(Errors.Keys.UnsupportedFormat_1, format));
    }

    /**
     * Copies all feature from the given source to the given target.
     * We use this method as central point where such copy occur, in case we want to implement
     * a more efficient algorithm in some future Apache SIS version. For example, we could copy
     * the files using {@link java.nio.file.Files} if we determine that it is possible.
     *
     * @param  source  the source set of features.
     * @param  target  where to copy the features.
     * @throws DataStoreException if an error occurred during the copy operation.
     *
     * @see #canWrite(Class)
     */
    public static void copy(final FeatureSet source, final WritableFeatureSet target) throws DataStoreException {
        target.updateType(source.getType());
        try (Stream<AbstractFeature> stream = source.features(false)) {
            target.add(stream.iterator());
        }
    }

    /**
     * Returns an error message for a resource not found. This is used for exception to be thrown
     * as {@link org.apache.sis.storage.IllegalNameException}.
     *
     * @param  store       the store for which a resource has not been found.
     * @param  identifier  the requested identifier.
     * @return error message for the exception to be thrown.
     */
    public static String resourceNotFound(final DataStore store, final String identifier) {
        return Resources.forLocale(store.getLocale()).getString(Resources.Keys.ResourceNotFound_2, store.getDisplayName(), identifier);
    }

    /**
     * Returns a log filter that removes the stack trace of filtered given log.
     * It can be used as argument in a call to {@link StoreListeners#warning(LogRecord, Filter)}
     * if the caller wants to trim the stack trace in log files or console outputs.
     *
     * <p>This filter should be used only for filtering {@link LogRecord} created by the caller, because
     * it modifies the record. Users would not expect this side effect on records created by them.</p>
     *
     * @return a filter for trimming stack trace.
     */
    public static Filter removeStackTraceInLogs() {
        return (record) -> {
            record.setThrown(null);
            return true;
        };
    }
}
