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
package org.apache.sis.internal.geotiff;

import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.internal.storage.io.InternalOptionKey;
import org.apache.sis.storage.DataStoreException;
import org.opengis.metadata.Metadata;
import org.opengis.util.GenericName;


/**
 * Modifies the metadata and bands inferred from GeoTIFF tags.
 *
 * <h2>Image indices</h2>
 * All image {@code index} arguments in this interfaces starts with 0 for the first (potentially pyramided) image
 * and are incremented by 1 after each <em>pyramid</em>, as defined by the cloud Optimized GeoTIFF specification.
 * Consequently those indices may differ from TIFF <cite>Image File Directory</cite> (IFD) indices.
 *
 * @todo May move to public API (in revised form) in a future version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
public interface SchemaModifier {
    /**
     * Invoked when an identifier is created for a single image or for the whole data store.
     * Implementations can override this method for replacing the given identifier by their own.
     *
     * @param  image       index of the image for which to compute identifier, or -1 for the whole store.
     * @param  identifier  the default identifier computed by {@code GeoTiffStore}. May be {@code null}
     *                     if {@code GeoTiffStore} has been unable to determine an identifier by itself.
     * @return the identifier to use, or {@code null} if none.
     */
    default GenericName customize(final int image, final GenericName identifier) {
        return identifier;
    }

    /**
     * Invoked when a metadata is created for a single image or for the whole data store.
     * Implementations can override this method for modifying or replacing the given metadata.
     * The given {@link DefaultMetadata} instance is still in modifiable state when this
     * method is invoked.
     *
     * @param  image     index of the image for which to compute metadata, or -1 for the whole store.
     * @param  metadata  metadata pre-filled by {@code GeoTiffStore} (never null). Can be modified in-place.
     * @return the metadata to return to user. This is often the same instance than the given {@code metadata}.
     *         Should never be null.
     * @throws DataStoreException if an exception occurred while updating metadata.
     */
    default Metadata customize(final int image, final DefaultMetadata metadata) throws DataStoreException {
        return metadata;
    }

    /**
     * Invoked when a sample dimension is created for a band in an image.
     * {@code GeoTiffStore} invokes this method with a builder initialized to band number as
     * {@linkplain SampleDimension.Builder#setName(int) dimension name}, with the fill value
     * declared as {@linkplain SampleDimension.Builder#setBackground(Number) background} and
     * with no category. Implementations can override this method for setting a better name
     * or for declaring the meaning of sample values (by adding "categories").
     *
     * @param  image        index of the image for which to create sample dimension.
     * @param  band         index of the band for which to create sample dimension.
     * @param  sampleRange  minimum and maximum values declared in the TIFF tags, or {@code null} if unknown.
     *                      This range may contain the background value.
     * @param  dimension    a sample dimension builder initialized with band number as the dimension name.
     *                      This builder can be modified in-place.
     * @return the sample dimension to use.
     */
    default SampleDimension customize(final int image, final int band, NumberRange<?> sampleRange,
                                      final SampleDimension.Builder dimension)
    {
        return dimension.build();
    }

    /**
     * Returns {@code true} if the converted values are measurement in the electromagnetic spectrum.
     * This flag controls the kind of metadata objects ({@linkplain org.opengis.metadata.content.ImageDescription}
     * versus {@linkplain org.opengis.metadata.content.CoverageDescription}) to be created for describing an image
     * with these sample dimensions. Those metadata have properties specific to electromagnetic spectrum, such as
     * {@linkplain org.opengis.metadata.content.Band#getPeakResponse() wavelength of peak response}.
     *
     * @param  image  index of the image for which to compute metadata.
     * @return {@code true} if the image contains measurements in the electromagnetic spectrum.
     */
    default boolean isElectromagneticMeasurement(final int image) {
        return false;
    }

    /**
     * The option for declaring a schema modifier at {@link org.apache.sis.storage.geotiff.GeoTiffStore} creation time.
     *
     * @todo if we move this key in public API in the future, then it would be a
     *       value in existing {@link org.apache.sis.storage.DataOptionKey} class.
     */
    OptionKey<SchemaModifier> OPTION = new InternalOptionKey<SchemaModifier>("SCHEMA_MODIFIER", SchemaModifier.class);

    /**
     * The default instance which performs no modification.
     */
    SchemaModifier DEFAULT = new SchemaModifier() {
    };
}
