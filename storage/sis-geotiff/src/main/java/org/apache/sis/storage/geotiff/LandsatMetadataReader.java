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

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.storage.DataStoreException;


/**
 * Parses Landsat metadata as {@linkplain DefaultMetadata ISO-19115 Metadata} object.
 *
 * @author  Remi Marechal (Geomatys)
 * @author  Thi Phuong Hao NGUYEN
 * @author  Minh Chinh VU
 * @since   0.8
 * @version 0.8
 * @module
 */
class LandsatMetadataReader {
    /**
     * All properties found in the Landsat metadata file, except {@code GROUP} and {@code END_GROUP}.
     * Example:
     *
     * {@preformat text
     *   DATE_ACQUIRED = 2014-03-12
     *   SCENE_CENTER_TIME = 03:02:01.5339408Z
     *   CORNER_UL_LAT_PRODUCT = 12.61111
     *   CORNER_UL_LON_PRODUCT = 108.33624
     *   CORNER_UR_LAT_PRODUCT = 12.62381
     *   CORNER_UR_LON_PRODUCT = 110.44017
     * }
     */
    private final Map<String,String> properties;

    /**
     * Stores all properties found in the Landsat file read from the the given reader,
     * except {@code GROUP} and {@code END_GROUP}.
     *
     * @param  reader a reader opened on the Landsat file. It is caller's responsibility to close this reader.
     * @throws IOException if an I/O error occurred while reading the given stream.
     * @throws DataStoreException if the content is not a Landsat file.
     */
    LandsatMetadataReader(final BufferedReader reader) throws IOException, DataStoreException {
        properties = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) {
                /*
                 * Landsat metadata ends with the END keyword. If we find that keyword, stop reading.
                 * All remaining lines (if any) will be ignored.
                 */
                if (line.equals("END")) {
                    break;
                }
                /*
                 * Separate the line into its key and value. For example in CORNER_UL_LAT_PRODUCT = 12.61111,
                 * the key will be CORNER_UL_LAT_PRODUCT and the value will be 12.61111.
                 */
                int separator = line.indexOf('=');
                if (separator < 0) {
                    throw new DataStoreException("Not a key-value pair.");
                }
                String key = line.substring(0, separator).trim().toUpperCase(Locale.US);
                if (!key.equals("GROUP") && !key.equals("END_GROUP")) {
                    String value = line.substring(separator + 1).trim();
                    if (key.isEmpty()) {
                        throw new DataStoreException("Key shall not be empty.");
                    }
                    /*
                     * In a Landsat file, String values are between quotes. Example: STATION_ID = "LGN"
                     * If such quotes are found, remove them.
                     */
                    int length = value.length();
                    if (length >= 2 && value.charAt(0) == '"' && value.charAt(length - 1) == '"') {
                        value = value.substring(1, length - 1).trim();
                        length = value.length();
                    }
                    /*
                     * Store only non-empty values. If a different value was already specified for the same key,
                     * this is considered as an error.
                     */
                    if (length != 0) {
                        String previous = properties.put(key, value);
                        if (previous != null && !value.equals(previous)) {
                            throw new DataStoreException("Duplicated values for \"" + key + "\".");
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the property value associated to the given key, or {@code null} if none.
     *
     * @param  key  the key for which to get the property value.
     * @return the property value associated to the given key, {@code null} if none.
     */
    private String getValue(String key) {
        return properties.get(key);
    }

    /**
     * Returns the floating-point value associated to the given key, or {@code NaN} if none.
     *
     * @param  key  the key for which to get the floating-point value.
     * @return the floating-point value associated to the given key, {@link Double#NaN} if none.
     * @throws NumberFormatException if the property associated to the given key can not be parsed
     *         as a floating-point number.
     */
    private double getNumericValue(String key) throws NumberFormatException {
        String value = getValue(key);
        return (value != null) ? Double.parseDouble(value) : Double.NaN;
    }

    /**
     * Returns the minimal or maximal value associated to the given two keys.
     *
     * @param  key1  the key for which to get the first floating-point value.
     * @param  key2  the key for which to get the second floating-point value.
     * @param  max   {@code true} for the maximal value, or {@code false} for the minimal value.
     * @return the minimal (if {@code max} is false) or maximal (if {@code max} is true) floating-point value
     *         associated to the given keys.
     * @throws NumberFormatException if the property associated to one of the given keys can not be parsed
     *         as a floating-point number.
     */
    private double getExtremumValue(String key1, String key2, boolean max) throws NumberFormatException {
        double value1 = getNumericValue(key1);
        double value2 = getNumericValue(key2);
        if (max ? (value2 > value1) : (value2 < value1)) {
            return value2;
        } else {
            return value1;
        }
    }

    /**
     * Gets the data bounding box in degrees of longitude and latitude, or {@code null} if none.
     *
     * @return the data domain in degrees of longitude and latitude, or {@code null} if none.
     * @throws DataStoreException if a longitude or a latitude can not be read.
     */
    private GeographicBoundingBox getGeographicBoundingBox() throws DataStoreException {
        final DefaultGeographicBoundingBox bbox;
        try {
            bbox = new DefaultGeographicBoundingBox(
                getExtremumValue("CORNER_UL_LON_PRODUCT", "CORNER_LL_LON_PRODUCT", false),      // westBoundLongitude
                getExtremumValue("CORNER_UR_LON_PRODUCT", "CORNER_LR_LON_PRODUCT", true),       // eastBoundLongitude
                getExtremumValue("CORNER_LL_LAT_PRODUCT", "CORNER_LR_LAT_PRODUCT", false),      // southBoundLatitude
                getExtremumValue("CORNER_UL_LAT_PRODUCT", "CORNER_UR_LAT_PRODUCT", true));      // northBoundLatitude
        } catch (NumberFormatException e) {
            throw new DataStoreException("Can not read the geographic bounding box.", e);
        }
        return bbox.isEmpty() ? null : bbox;
    }

    /**
     * Temporary method for testing purpose only - will be removed in the final version.
     */
    public static void main(String[] args) throws Exception {
        LandsatMetadataReader reader;
        try (BufferedReader in = new BufferedReader(new FileReader("/home/haonguyen/data/LC81230522014071LGN00_MTL.txt"))) {
            reader = new LandsatMetadataReader(in);
        }
        System.out.println("The geographic bounding box of LC81230522014071LGN00_MTL.txt is:");
        System.out.println(reader.getGeographicBoundingBox());
    }
}
