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


/**
 * Keys of Landsat metadata recognized by {@link LandsatReader}.
 * This class should not contain the keys of entries ignored by {@code LandsatReader}.
 *
 * @author  Remi Marechal (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class LandsatKeys {
    /**
     * Do not allow instantiation of this class.
     */
    private LandsatKeys() {
    }

    ////
    //// GROUP = METADATA_FILE_INFO
    ////

    /**
     * Origin of the product.
     * Value is “Image courtesy of the U.S. Geological Survey”.
     */
    static final String ORIGIN = "ORIGIN";

    /**
     * The unique Landsat scene identifier.
     * Format is {@code Ls8ppprrrYYYYDDDGGGVV}.
     */
    static final String LANDSAT_SCENE_ID = "LANDSAT_SCENE_ID";

    /**
     * The date when the metadata file for the L1G product set was created.
     * The date is based on Universal Time Coordinated (UTC).
     * Date format is {@code YYYY-MM-DDTHH:MM:SSZ}.
     */
    static final String FILE_DATE = "FILE_DATE";


    ////
    //// GROUP = PRODUCT_METADATA
    ////

    /**
     * The output format of the image.
     * Value is {@code “GEOTIFF”}.
     */
    static final String OUTPUT_FORMAT = "OUTPUT_FORMAT";

    /**
     * Spacecraft from which the data were captured.
     * Value is {@code “LANDSAT_8”}.
     */
    static final String SPACECRAFT_ID = "SPACECRAFT_ID";

    /**
     * Sensor(s) used to capture this scene.
     * Value can be {@code “OLI”}, {@code “TIRS”} or {@code “OLI_TIRS”}.
     */
    static final String SENSOR_ID = "SENSOR_ID";

    /**
     * The date the image was acquired.
     * Date format is {@code YYYY-MM-DD}.
     */
    static final String DATE_ACQUIRED = "DATE_ACQUIRED";

    /**
     * Scene center time of the date the image was acquired.
     * Time format is {@code HH:MI:SS.SSSSSSSZ}.
     */
    static final String SCENE_CENTER_TIME = "SCENE_CENTER_TIME";

    /**
     * The latitude value for the upper-left corner of the product, measured at the center of the pixel.
     * Positive value indicates north latitude; negative value indicates south latitude. Units are in degrees.
     */
    static final String CORNER_UL_LAT_PRODUCT = "CORNER_UL_LAT_PRODUCT";

    /**
     * The longitude value for the upper-left corner of the product, measured at the center of the pixel.
     * Positive value indicates east longitude; negative value indicates west longitude. Units are in degrees.
     */
    static final String CORNER_UL_LON_PRODUCT = "CORNER_UL_LON_PRODUCT";

    /**
     * The latitude value for the upper-right corner of the product. Measured at the center of the pixel.
     * Units are in degrees.
     */
    static final String CORNER_UR_LAT_PRODUCT = "CORNER_UR_LAT_PRODUCT";

    /**
     * The longitude value for the upper-right corner of the product, measured at the center of the pixel.
     * Units are in degrees.
     */
    static final String CORNER_UR_LON_PRODUCT = "CORNER_UR_LON_PRODUCT";

    /**
     * The latitude value for the lower-left corner of the product, measured at the center of the pixel.
     * Units are in degrees.
     */
    static final String CORNER_LL_LAT_PRODUCT = "CORNER_LL_LAT_PRODUCT";

    /**
     * The longitude value for the lower-left corner of the product, measured at the center of the pixel.
     * Units are in degrees.
     */
    static final String CORNER_LL_LON_PRODUCT = "CORNER_LL_LON_PRODUCT";

    /**
     * The latitude value for the lower-right corner of the product, measured at the center of the pixel.
     * Units are in degrees.
     */
    static final String CORNER_LR_LAT_PRODUCT = "CORNER_LR_LAT_PRODUCT";

    /**
     * The longitude value for the lower-right corner of the product, measured at the center of the pixel.
     * Units are in degrees.
     */
    static final String CORNER_LR_LON_PRODUCT = "CORNER_LR_LON_PRODUCT";


    ////
    //// GROUP = IMAGE_ATTRIBUTES
    ////

    /**
     * The overall cloud coverage (percent) of the WRS-2 scene as a value between 0 and 100 inclusive.
     * -1 indicates that the score was not calculated.
     */
    static final String CLOUD_COVER = "CLOUD_COVER";

    /**
     * The Sun azimuth angle in degrees for the image center location at the image center acquisition time.
     * Values are from -180 to 180 degrees inclusive.
     * A positive value indicates angles to the east or clockwise from the north.
     * A negative value indicates angles to the west or counterclockwise from the north.
     */
    static final String SUN_AZIMUTH = "SUN_AZIMUTH";

    /**
     * The Sun elevation angle in degrees for the image center location at the image center acquisition time.
     * Values are from -90 to 90 degrees inclusive.
     * A positive value indicates a daytime scene. A negative value indicates a nighttime scene.
     * Note: for reflectance calculation, the sun zenith angle is needed, which is 90 - sun elevation angle.
     */
    static final String SUN_ELEVATION = "SUN_ELEVATION";
    static final String ELEVATION_SOURCE="ELEVATION_SOURCE";
}
