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

import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.apache.sis.internal.geotiff.Resources;
import org.apache.sis.internal.geotiff.Compression;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.CharSequences;
import org.apache.sis.measure.Units;


/**
 * A temporary object for building the metadata for a single GeoTIFF image.
 * Fields in the class are used only for information purposes; they do not
 * have incidence on the way Apache SIS will handle the GeoTIFF image.
 *
 * <div class="note"><b>Note:</b>
 * if those fields become useful to {@link ImageFileDirectory} in a future version,
 * we can move them into that class. Otherwise keeping those fields here allow to
 * discard them (which save a little bit of space) when no longer needed.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.2
 * @module
 */
final class ImageMetadataBuilder extends MetadataBuilder {
    /**
     * The number of pixels per {@link #resolutionUnit} in the image width and Height directions,
     * or {@link Double#NaN} is unspecified. Since ISO 19115 does not have separated resolution
     * fields for image width and height, Apache SIS stores only the maximal value.
     */
    private double resolution = Double.NaN;

    /**
     * The unit of measurement for the {@linkplain #resolution} value, or {@code null} if none.
     * A null value is used for images that may have a non-square aspect ratio, but no meaningful
     * absolute dimensions. Default value for TIFF files is inch.
     */
    private Unit<Length> resolutionUnit = Units.INCH;

    /**
     * The size of the dithering or halftoning matrix used to create a dithered or halftoned bilevel file.
     * This field should be present only if {@code Threshholding} tag is 2 (an ordered dither or halftone
     * technique has been applied to the image data). Special values:
     *
     * <ul>
     *   <li>-1 means that {@code Threshholding} is 1 or unspecified.</li>
     *   <li>-2 means that {@code Threshholding} is 2 but the matrix size has not yet been specified.</li>
     *   <li>-3 means that {@code Threshholding} is 3 (randomized process such as error diffusion).</li>
     * </ul>
     */
    private short cellWidth = -1, cellHeight = -1;

    /**
     * Metadata specified in {@code GEO_METADATA} or {@code GDAL_METADATA} tags, or {@code null} if none.
     */
    private XMLMetadata complement;

    /**
     * Creates an initially empty metadata builder.
     */
    ImageMetadataBuilder() {
    }

    /**
     * Encodes the value of Threshholding TIFF tag into the {@link #cellWidth} and {@link #cellHeight} fields.
     * Recognized values are:
     *
     * <ul>
     *   <li>1 = No dithering or halftoning has been applied to the image data.</li>
     *   <li>2 = An ordered dither or halftone technique has been applied to the image data.</li>
     *   <li>3 = A randomized process such as error diffusion has been applied to the image data.</li>
     * </ul>
     *
     * @param  value  the threshholding value.
     * @return {@code null} on success, or the given value if not recognized.
     */
    @SuppressWarnings("fallthrough")
    Integer setThreshholding(final int value) {
        switch (value) {
            default: return value;                              // Cause a warning to be reported by the caller.
            case 2:  if ((cellWidth & cellHeight) >= 0) break;  // Exit if at least one value is positive, else fallthrough.
            case 1:  // Fall through
            case 3:  cellWidth = cellHeight = (short) -value; break;
        }
        return null;
    }

    /**
     * Sets the width or height of the dithering or halftoning matrix used to create
     * a dithered or halftoned bilevel file. Meaningful only if Threshholding = 2.
     *
     * @param  size  the new size.
     * @param  w     {@code true} for setting cell width, or {@code false} for setting cell height.
     */
    void setCellSize(final short size, final boolean w) {
        if (w) cellWidth = size;
        else  cellHeight = size;
    }

    /**
     * Sets the resolution to the maximal of current value and given value.
     */
    void setResolution(final double r) {
        if (Double.isNaN(resolution) || r > resolution) {
            resolution = r;
        }
    }

    /**
     * Sets the unit of measurement for {@code XResolution} and {@code YResolution}.
     * Recognized values are:
     *
     * <ul>
     *   <li>1 = None. Used for images that may have a non-square aspect ratio.</li>
     *   <li>2 = Inch (default).</li>
     *   <li>3 = Centimeter.</li>
     * </ul>
     *
     * @param  value  the threshholding value.
     * @return {@code null} on success, or the given value if not recognized.
     */
    Integer setResolutionUnit(final int unit) {
        switch (unit) {
            case 1:  resolutionUnit = null;             break;
            case 2:  resolutionUnit = Units.INCH;       break;
            case 3:  resolutionUnit = Units.CENTIMETRE; break;
            default: return unit;     // Cause a warning to be reported by the caller.
        }
        return null;
    }

    /**
     * Adds metadata in XML format. Those metadata are defined
     * in {@code GEO_METADATA} or {@code GDAL_METADATA} tags.
     */
    void addXML(final XMLMetadata xml) {
        if (complement == null) {
            complement = xml;
        } else {
            xml.appendTo(complement);
        }
    }

    /**
     * Completes the metadata with the information stored in the fields of the IFD.
     * This method is invoked only if the user requested the ISO 19115 metadata.
     * It should be invoked last, after all other metadata have been set.
     *
     * @throws DataStoreException if an error occurred while reading metadata from the data store.
     */
    void finish(final ImageFileDirectory image, final StoreListeners listeners) throws DataStoreException {
        image.getIdentifier().ifPresent((id) -> {
            if (!image.getImageIndex().equals(id.tip().toString())) {
                addTitle(id.toString());
            }
        });
        /*
         * Add information about the file format.
         *
         * Destination: metadata/identificationInfo/resourceFormat
         */
        final GeoTiffStore store = image.reader.store;
        if (store.hidden) {
            store.setFormatInfo(this);       // Should be before `addCompression(…)`.
        }
        final Compression compression = image.getCompression();
        if (compression != null) {
            addCompression(CharSequences.upperCaseToSentence(compression.name()));
        }
        /*
         * Add the resolution into the metadata. Our current ISO 19115 implementation restricts
         * the resolution unit to metres, but it may be relaxed in a future SIS version.
         *
         * Destination: metadata/identificationInfo/spatialResolution/distance
         */
        if (!Double.isNaN(resolution) && resolutionUnit != null) {
            addResolution(resolutionUnit.getConverterTo(Units.METRE).convert(resolution));
        }
        /*
         * Cell size is relevant only if the Threshholding TIFF tag value is 2. By convention in
         * this implementation class, other Threshholding values are stored as negative cell sizes:
         *
         *   -1 means that Threshholding is 1 or unspecified.
         *   -2 means that Threshholding is 2 but the matrix size has not yet been specified.
         *   -3 means that Threshholding is 3 (randomized process such as error diffusion).
         *
         * Destination: metadata/resourceLineage/processStep/description
         */
        final int cellWidth  = this.cellWidth;
        final int cellHeight = this.cellHeight;
        switch (Math.min(cellWidth, cellHeight)) {
            case -1: {
                // Nothing to report.
                break;
            }
            case -3: {
                addProcessDescription(Resources.formatInternational(Resources.Keys.RandomizedProcessApplied));
                break;
            }
            default: {
                addProcessDescription(Resources.formatInternational(
                            Resources.Keys.DitheringOrHalftoningApplied_2,
                            (cellWidth  >= 0) ? cellWidth  : '?',
                            (cellHeight >= 0) ? cellHeight : '?'));
                break;
            }
        }
        /*
         * If there is XML metadata, append them last in order
         * to allow them to be merged with existing metadata.
         */
        while (complement != null) try {
            complement = complement.appendTo(this);
        } catch (Exception ex) {
            listeners.warning(image.reader.errors().getString(Errors.Keys.CanNotSetPropertyValue_1, complement.tag()), ex);
        }
    }
}
