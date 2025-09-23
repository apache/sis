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
package org.apache.sis.storage.modifier;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import org.opengis.metadata.Metadata;
import org.opengis.util.GenericName;
import org.apache.sis.image.DataType;
import org.apache.sis.storage.DataOptionKey;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.util.internal.shared.Strings;


/**
 * Modifies the metadata, grid geometry or sample dimensions inferred by a data store for a (grid) coverage.
 * The modifications are applied by callback methods which are invoked at reading time when first needed.
 * The caller is usually a {@link org.apache.sis.storage.GridCoverageResource}, but not necessarily.
 * It may also be a more generic coverage.
 *
 * <h2>Usage</h2>
 * For modifying the coverages provided by a data store, register an instance of {@code CoverageModifier}
 * at the store opening time as below:
 *
 * {@snippet lang="java" :
 * StorageConnector storage = ...;
 * CoverageModifier modifier = ...;
 * storage.setOption(DataOptionKey.COVERAGE_MODIFIER, modifier);
 * try (DataStore store = DataStores.open(connector)) {
 *     // Modified resources will be returned.
 * }
 * }
 *
 * Not all {@link DataStore} implementations recognize this options.
 * Data stores that do not support modifications will ignore the above option.
 * A {@link DataStore} may also support modifications only partially,
 * by invoking only a subset of the methods defined in this interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see DataOptionKey#COVERAGE_MODIFIER
 *
 * @since 1.5
 */
public interface CoverageModifier {
    /**
     * Returns modifier specified in the options of the given storage connector.
     * This convenience method fetches the value associated to {@link DataOptionKey#COVERAGE_MODIFIER}.
     * If there is no such value, then this method returns the {@link #DEFAULT} instance.
     *
     * @param  connector  the storage connector from which to get the modifier.
     * @return the modifier to use, never {@code null}.
     */
    public static CoverageModifier getOrDefault(StorageConnector connector) {
        final CoverageModifier customizer = connector.getOption(DataOptionKey.COVERAGE_MODIFIER);
        return (customizer != null) ? customizer : Source.DEFAULT;
    }

    /**
     * Information about which file and coverage (image) is subject to modification.
     * Coverages are identified by their index, starting at 0 and incremented sequentially.
     *
     * @version 1.5
     * @since   1.5
     */
    public static class Source {
        /**
         * The default instance using the default implementation documented in each method.
         * Defined in this class because we cannot have private static field in an interface.
         */
        static final CoverageModifier DEFAULT = new CoverageModifier() {
        };

        /** The data store for which to modify a file or coverage description. */
        private final DataStore store;

        /** Index of the coverage for which to compute information, or -1 for the whole file. */
        private final int coverageIndex;

        /** The type of raster data, or {@code null} if unknown. */
        private final DataType dataType;

        /**
         * Creates a new source for the file as a whole.
         * The coverage index and data type are unspecified.
         *
         * @param store  the data store for which to modify some coverages or sample dimensions.
         */
        public Source(final DataStore store) {
            this.store = Objects.requireNonNull(store);
            this.coverageIndex = -1;
            this.dataType = null;
        }

        /**
         * Creates a new source for a coverage at the specified index.
         *
         * @param store          the data store for which to modify some coverages or sample dimensions.
         * @param coverageIndex  index of the coverage (image) for which to compute information.
         * @param dataType       the type of raster data, or {@code null} if unknown.
         */
        public Source(final DataStore store, final int coverageIndex, final DataType dataType) {
            this.store = Objects.requireNonNull(store);
            this.coverageIndex = coverageIndex;
            this.dataType = dataType;
        }

        /**
         * Return the data store for which to modify a file, coverage (image) or sample dimension (band) description.
         *
         * @return the data store for which to modify a description.
         */
        public DataStore getDataStore() {
            return store;
        }

        /**
         * Returns the index of the coverage for which to modify the description.
         * If absent, then the modifications apply to the whole file.
         *
         * <h4>Interpretation in GeoTIFF files</h4>
         * The index starts with 0 for the first (potentially pyramided) coverage and is incremented
         * by 1 after each <em>pyramid</em>, as defined by the cloud Optimized GeoTIFF specification.
         * Therefore, this index may differ from the <abbr>TIFF</abbr> <i>Image File Directory</i>
         * (<abbr>IFD</abbr>) index.
         *
         * @return the index of the coverage to eventually modify.
         */
        public OptionalInt getCoverageIndex() {
            return (coverageIndex >= 0) ? OptionalInt.of(coverageIndex) : OptionalInt.empty();
        }

        /**
         * Returns the type in which the coverage (raster) data are stored.
         * The enumeration values are restricted to the types compatible with Java2D.
         *
         * @return the type of raster data.
         */
        public Optional<DataType> getDataType() {
            return Optional.ofNullable(dataType);
        }

        /**
         * Returns the index of the band for which to create sample dimension, or -1 if none.
         * Defined in this base class only for {@link #toString()} implementation convenience.
         */
        int getBandIndex() {
            return -1;
        }

        /**
         * Returns the number of bands, or -1 if none.
         * Defined in this base class only for {@link #toString()} implementation convenience.
         */
        int getNumBands() {
            return -1;
        }

        /**
         * Returns the minimum and maximum values declared in the coverage metadata, if known.
         * Defined in this base class only for {@link #toString()} implementation convenience.
         */
        Optional<NumberRange<?>> getSampleRange() {
            return Optional.empty();
        }

        /**
         * Returns a string representation for debugging purposes.
         * The format or the returned string may change in any future version.
         *
         * @return a string representation for debugging purposes.
         */
        @Override
        public String toString() {
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final int coverageIndex = getCoverageIndex().orElse(-1);
            final int bandIndex     = getBandIndex();
            final int numBands      = getNumBands();
            return Strings.toString(getClass(),
                    "store",         getDataStore().getDisplayName(),
                    "coverageIndex", (coverageIndex >= 0) ? coverageIndex  : null,
                    "bandIndex",     (bandIndex     >= 0) ? bandIndex : null,
                    "numBands",      (numBands      >= 0) ? numBands  : null,
                    "dataType",      getDataType(),
                    "sampleRange", getSampleRange().orElse(null));
        }
    }

    /**
     * Information about which sample dimension (band) is subject to modification.
     * Bands are identified by their index, starting at 0 and incremented sequentially.
     *
     * @version 1.5
     * @since   1.5
     */
    public static class BandSource extends Source {
        /** Index of the band for which to create sample dimension. */
        private final int bandIndex;

        /** Number of bands. */
        private final int numBands;

        /**
         * Creates a new source for the specified band.
         *
         * @param store          the data store which contains the band to modify.
         * @param coverageIndex  index of the coverage for which to create a sample dimension.
         * @param bandIndex      index of the band for which to create a sample dimension.
         * @param numBands       number of bands.
         * @param dataType       type of raster data, or {@code null} if unknown.
         */
        public BandSource(final DataStore store, final int coverageIndex, final int bandIndex,
                          final int numBands, final DataType dataType)
        {
            super(store, coverageIndex, dataType);
            this.bandIndex = bandIndex;
            this.numBands  = numBands;
        }

        /**
         * Returns the index of the band for which to create sample dimension.
         * The numbers start at 0.
         *
         * @return the index of the band for which to create sample dimension.
         */
        @Override
        public int getBandIndex() {
            return bandIndex;
        }

        /**
         * Returns the number of sample dimensions (bands) in the coverage.
         *
         * @return the number of bands.
         */
        @Override
        public int getNumBands() {
            return numBands;
        }

        /**
         * Return the minimum and maximum values declared in the coverage metadata, if known.
         * This range may contain the {@linkplain SampleDimension#getBackground() background value}.
         *
         * @return the minimum and maximum values declared in the coverage.
         */
        @Override
        public Optional<NumberRange<?>> getSampleRange() {
            return Optional.empty();
        }
    }

    /**
     * Invoked when an identifier is created for a single coverage or for the whole file.
     * Implementations can override this method for replacing the given identifier by their own.
     *
     * <h4>Default implementation</h4>
     * The default implementation returns the given {@code identifier} unchanged.
     * It may be null.
     *
     * @param  source      contains the index of the coverage for which to compute an identifier.
     *                     If the coverage index is absent, then the identifier applies to the whole file.
     * @param  identifier  the default identifier computed by {@code DataStore}. May be {@code null} if
     *                     the {@code DataStore} has been unable to determine an identifier by itself.
     * @return the identifier to use, or {@code null} if none.
     * @throws DataStoreException if an exception occurred while computing an identifier.
     */
    default GenericName customize(Source source, GenericName identifier) throws DataStoreException {
        return identifier;
    }

    /**
     * Invoked when a metadata is created for a single coverage or for the whole file.
     * Implementations can override this method for modifying or replacing the given metadata.
     * The given {@link DefaultMetadata} instance is still in modifiable state when this method is invoked.
     *
     * <h4>Default implementation</h4>
     * The default implementation declares the given metadata as {@linkplain DefaultMetadata.State#FINAL final}
     * (unmodifiable), then returns the metadata instance.
     *
     * @param  source    contains the index of the coverage for which to compute metadata.
     *                   If the coverage index is absent, then the metadata applies to the whole file.
     * @param  metadata  metadata pre-filled by the {@code DataStore} (never null). Can be modified in-place.
     * @return the metadata to return to user. This is often the same instance as the given {@code metadata}.
     * @throws DataStoreException if an exception occurred while updating metadata.
     */
    default Metadata customize(Source source, DefaultMetadata metadata) throws DataStoreException {
        metadata.transitionTo(DefaultMetadata.State.FINAL);
        return metadata;
    }

    /**
     * Invoked when a grid geometry is created for a coverage.
     * Implementations can override this method for replacing the given grid geometry by a derived instance.
     * A typical use case is to check if the Coordinate Reference System (<abbr>CRS</abbr>) is present and,
     * if not, provide a default <abbr>CRS</abbr>.
     *
     * <h4>Default implementation</h4>
     * The default implementation returns the given {@code domain} unchanged.
     *
     * @param  source  contains the index of the coverage for which to compute metadata.
     * @param  domain  the domain computed by the data store.
     * @return the domain to return to user.
     * @throws DataStoreException if an exception occurred while computing the domain.
     */
    default GridGeometry customize(Source source, GridGeometry domain) throws DataStoreException {
        return domain;
    }

    /**
     * Invoked when a sample dimension is created in a coverage.
     * The data store invokes this method with a {@link SampleDimension} builder initialized to a default name,
     * which may be the {@linkplain SampleDimension.Builder#setName(int) band number}.
     * The builder may also contain a {@linkplain SampleDimension.Builder#setBackground(Number) background value}
     * and {@linkplain SampleDimension.Builder#categories() categories}.
     * Implementations can override this method for setting a better name
     * or for declaring the meaning of sample values (by replacing categories).
     *
     * <h4>Default implementation</h4>
     * The default implementation returns {@code dimensions.build()} with no modification on the given builder.
     *
     * <h4>Example: measurement data</h4>
     * The following example declares that the values 0 means "no data".
     * The presence of such "no data" category will cause the raster to be converted to floating point
     * values before operations such as {@code resample}, in order to replace those "no data" by NaN values.
     * When a "no data" category is declared, it is strongly recommended to also declare the range of real data.
     * The following example declares the range 1 to 255 inclusive.
     *
     * {@snippet lang="java" :
     * @Override
     * public SampleDimension customize(BandSource source, SampleDimension.Builder dimension) {
     *     dimension.categories().clear();      // Discard the categories created by the store.
     *     dimension.addQualitative(null, 0);   // Declare value 0 as "no data".
     *     dimension.addQuantitative("Some name for my data", 1, 255, null);
     *     return dimension.build();
     * }
     * }
     *
     * See the various {@code addQuantitative(…)} methods for information about how to declare a transfer function
     * (a conversion from pixel values to the unit of measurement).
     *
     * <h4>Example: visualization only</h4>
     * If the pixel values have no meaning other than visualization, this method can be overridden
     * as below for making sure that they raster is not interpreted as measurement data:
     *
     * {@snippet lang="java" :
     * @Override
     * public SampleDimension customize(BandSource source, SampleDimension.Builder dimension) {
     *     dimension.categories().clear();      // Discard the categories created by the store.
     *     return dimension.build();
     * }
     * }
     *
     * @param  source     contains index of the coverage and band for which to create sample dimension.
     * @param  dimension  a sample dimension builder initialized with band number as the dimension name.
     *                    This builder can be modified in-place.
     * @return the sample dimension to use.
     * @throws DataStoreException if an exception occurred while fetching sample dimension information.
     */
    default SampleDimension customize(final BandSource source, final SampleDimension.Builder dimension)
            throws DataStoreException
    {
        return dimension.build();
    }

    /**
     * Returns {@code true} if the converted values are measurement in the electromagnetic spectrum.
     * This flag controls the kind of metadata objects ({@linkplain org.opengis.metadata.content.ImageDescription}
     * versus {@linkplain org.opengis.metadata.content.CoverageDescription}) to be created for describing a coverage
     * with these sample dimensions. Those metadata have properties specific to electromagnetic spectrum, such as
     * {@linkplain org.opengis.metadata.content.Band#getPeakResponse() wavelength of peak response}.
     *
     * @param  source  contains the index of the coverage for which to compute metadata.
     * @return {@code true} if the coverage contains measurements in the electromagnetic spectrum.
     * @throws DataStoreException if an exception occurred while fetching metadata.
     */
    default boolean isElectromagneticMeasurement(Source source) throws DataStoreException {
        return false;
    }
}
