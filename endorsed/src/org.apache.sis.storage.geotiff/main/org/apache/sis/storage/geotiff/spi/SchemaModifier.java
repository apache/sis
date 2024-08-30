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
package org.apache.sis.storage.geotiff.spi;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import org.opengis.metadata.Metadata;
import org.opengis.util.GenericName;
import org.apache.sis.image.DataType;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.io.stream.InternalOptionKey;
import org.apache.sis.util.privy.Strings;


/**
 * Modifies the name, metadata or bands inferred by the data store.
 * The modifications are applied at reading time, for example just before to create a sample dimension.
 * {@code SchemaModifier} allows to change image names, metadata or sample dimension (band) descriptions.
 *
 * @todo May move to public API (in revised form) in a future version.
 *       Most of this interface is not specific to GeoTIFF and could be placed in a generic package.
 *       An exception is {@link #customize(BandSource, SampleDimension.Builder)} which is specific
 *       at least in its contract. It may need to stay in a specialized interface at least for that contract.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public interface SchemaModifier {
    /**
     * Information about which file, image or band is subject to modification.
     * Images are identified by their index, starting at 0 and incremented sequentially.
     * Band information are provided in the {@link BandSource} subclass.
     */
    public static class Source {
        /** The data store for which to modify a file, image or band description. */
        private final DataStore store;

        /** Index of the image for which to compute information, or -1 for the whole file. */
        private final int imageIndex;

        /** The type of raster data, or {@code null} if unknown. */
        private final DataType dataType;

        /**
         * Creates a new source for the file as a whole.
         *
         * @param store  the data store for which to modify a file, image or band description.
         */
        public Source(final DataStore store) {
            this.store = Objects.requireNonNull(store);
            imageIndex = -1;
            dataType = null;
        }

        /**
         * Creates a new source for the specified image.
         *
         * @param store       the data store for which to modify a file, image or band description.
         * @param imageIndex  index of the image for which to compute information.
         * @param dataType    the type of raster data, or {@code null} if unknown.
         */
        public Source(final DataStore store, final int imageIndex, final DataType dataType) {
            this.store      = Objects.requireNonNull(store);
            this.imageIndex = imageIndex;
            this.dataType   = dataType;
        }

        /**
         * {@return the data store for which to modify a file, image or band description}.
         */
        public DataStore getDataStore() {
            return store;
        }

        /**
         * {@return the index of the image for which to compute information}.
         * If absent, then the value to compute applies to the whole file.
         *
         * <h4>Interpretation in GeoTIFF files</h4>
         * The index starts with 0 for the first (potentially pyramided) image and is incremented
         * by 1 after each <em>pyramid</em>, as defined by the cloud Optimized GeoTIFF specification.
         * Consequently, this index may differ from the TIFF <i>Image File Directory</i> (IFD) index.
         */
        public OptionalInt getImageIndex() {
            return (imageIndex >= 0) ? OptionalInt.of(imageIndex) : OptionalInt.empty();
        }

        /**
         * {@return the type of raster data}.
         * The enumeration values are restricted to types compatible with Java2D.
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
         * Returns the minimum and maximum values declared in the TIFF tags, if known.
         * Defined in this base class only for {@link #toString()} implementation convenience.
         */
        Optional<NumberRange<?>> getSampleRange() {
            return Optional.empty();
        }

        /**
         * {@return a string representation for debugging purposes}.
         */
        @Override
        public String toString() {
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final int imageIndex = getImageIndex().orElse(-1);
            final int bandIndex  = getBandIndex();
            final int numBands   = getNumBands();
            return Strings.toString(getClass(),
                    "store",       getDataStore().getDisplayName(),
                    "imageIndex",  (imageIndex >= 0) ? imageIndex : null,
                    "bandIndex",   (bandIndex  >= 0) ? bandIndex  : null,
                    "numBands",    (numBands   >= 0) ? numBands   : null,
                    "dataType",    getDataType(),
                    "sampleRange", getSampleRange().orElse(null));
        }
    }

    /**
     * Information about which band is subject to modification.
     * Images and bands are identified by their index, starting at 0 and incremented sequentially.
     */
    public static abstract class BandSource extends Source {
        /** Index of the band for which to create sample dimension. */
        private final int bandIndex;

        /** Number of bands. */
        private final int numBands;

        /**
         * Creates a new source for the specified band.
         *
         * @param store       the data store which contains the band to modify.
         * @param imageIndex  index of the image for which to create a sample dimension.
         * @param bandIndex   index of the band for which to create a sample dimension.
         * @param numBands    number of bands.
         * @param dataType    type of raster data, or {@code null} if unknown.
         */
        protected BandSource(final DataStore store, final int imageIndex, final int bandIndex,
                             final int numBands, final DataType dataType)
        {
            super(store, imageIndex, dataType);
            this.bandIndex = bandIndex;
            this.numBands  = numBands;
        }

        /**
         * {@return the index of the band for which to create sample dimension}.
         */
        @Override
        public int getBandIndex() {
            return bandIndex;
        }

        /**
         * {@return the number of bands}.
         */
        @Override
        public int getNumBands() {
            return numBands;
        }

        /**
         * {@return the minimum and maximum values declared in the TIFF tags, if known}.
         * This range may contain the {@linkplain SampleDimension#getBackground() background value}.
         */
        @Override
        public Optional<NumberRange<?>> getSampleRange() {
            return Optional.empty();
        }
    }

    /**
     * Invoked when an identifier is created for a single image or for the whole file.
     * Implementations can override this method for replacing the given identifier by their own.
     *
     * @param  source      contains the index of the image for which to compute an identifier.
     *                     If the image index is absent, then the identifier applies to the whole file.
     * @param  identifier  the default identifier computed by {@code DataStore}. May be {@code null} if
     *                     the {@code DataStore} has been unable to determine an identifier by itself.
     * @return the identifier to use, or {@code null} if none.
     */
    default GenericName customize(final Source source, final GenericName identifier) {
        return identifier;
    }

    /**
     * Invoked when a metadata is created for a single image or for the whole file.
     * Implementations can override this method for modifying or replacing the given metadata.
     * The given {@link DefaultMetadata} instance is still in modifiable state when this method is invoked.
     *
     * @param  source    contains the index of the image for which to compute metadata.
     *                   If the image index is absent, then the metadata applies to the whole file.
     * @param  metadata  metadata pre-filled by the {@code DataStore} (never null). Can be modified in-place.
     * @return the metadata to return to user. This is often the same instance as the given {@code metadata}.
     *         Should never be null.
     * @throws DataStoreException if an exception occurred while updating metadata.
     */
    default Metadata customize(final Source source, final DefaultMetadata metadata) throws DataStoreException {
        return metadata;
    }

    /**
     * Invoked when a sample dimension is created for a band in an image.
     * {@code GeoTiffStore} invokes this method with a builder initialized to the band number as
     * {@linkplain SampleDimension.Builder#setName(int) dimension name}, with the fill value
     * declared as {@linkplain SampleDimension.Builder#setBackground(Number) background} and
     * with no category. Implementations can override this method for setting a better name
     * or for declaring the meaning of sample values (by adding "categories").
     *
     * <h4>Default implementation</h4>
     * The default implementation creates a "no data" category for the
     * {@linkplain SampleDimension.Builder#getBackground() background value} if such value exists.
     * The presence of such "no data" category will cause the raster to be converted to floating point
     * values before operations such as {@code resample}, in order to replace those "no data" by NaN values.
     * If this replacement is not desired, then subclass should override this method for example like below:
     *
     * {@snippet lang="java" :
     * @Override
     * public SampleDimension customize(BandSource source, SampleDimension.Builder dimension) {
     *     return dimension.build();
     * }
     * }
     *
     * @param  source     contains indices of the image and band for which to create sample dimension.
     * @param  dimension  a sample dimension builder initialized with band number as the dimension name.
     *                    This builder can be modified in-place.
     * @return the sample dimension to use.
     */
    default SampleDimension customize(final BandSource source, final SampleDimension.Builder dimension) {
        final Number fill = dimension.getBackground();
        if (fill != null) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            NumberRange<?> samples = new NumberRange(fill.getClass(), fill, true, fill, true);
            dimension.addQualitative(null, samples);
        }
        return dimension.build();
    }

    /**
     * Returns {@code true} if the converted values are measurement in the electromagnetic spectrum.
     * This flag controls the kind of metadata objects ({@linkplain org.opengis.metadata.content.ImageDescription}
     * versus {@linkplain org.opengis.metadata.content.CoverageDescription}) to be created for describing an image
     * with these sample dimensions. Those metadata have properties specific to electromagnetic spectrum, such as
     * {@linkplain org.opengis.metadata.content.Band#getPeakResponse() wavelength of peak response}.
     *
     * @param  source  contains the index of the image for which to compute metadata.
     * @return {@code true} if the image contains measurements in the electromagnetic spectrum.
     */
    default boolean isElectromagneticMeasurement(final Source source) {
        return false;
    }

    /**
     * The option for declaring a schema modifier at {@link DataStore} creation time.
     *
     * @todo if we move this key in public API in the future, then it would be a
     *       value in existing {@link org.apache.sis.storage.DataOptionKey} class.
     */
    OptionKey<SchemaModifier> OPTION_KEY = new InternalOptionKey<>("SCHEMA_MODIFIER", SchemaModifier.class);

    /**
     * The default instance which performs no modification.
     */
    SchemaModifier DEFAULT = new SchemaModifier() {
    };
}
