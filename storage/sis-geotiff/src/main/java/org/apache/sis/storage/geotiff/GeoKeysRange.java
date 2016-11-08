/*
 * Copyright 2016 rmarechal.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage.geotiff;

/**
 * Enum which represente range value validity for EPSG codes,
 * of Geographical key from geotiff specification.
 *
 * @author Remi Maréchal(Geomatys).
 */
enum GeoKeysRange {

    /**
     * Operation method means (Coordinate Transformation Codes in geotiff).
     * Example: CT_TransverseMercator = 1, CT_Polyconic = 22 etc ...
     *
     * Ranges:
     * 0 = undefined
     * [    1, 16383] = GeoTIFF Coordinate Transformation codes
     * [16384, 32766] = Reserved by GeoTIFF
     * 32767          = user-defined
     * [32768, 65535] = Private User Implementations
     *
     * note : from documentation, given range is [1; 16383]
     * but value are within into [1, 27]
     * OperationMethod range is initialized by [1; 27] to avoid overlaps with ProjectedCRS range [10000; 32760]
     */
    OPERATION_METHOD(1, 27),

    /**
     * Ranges:

     * 0 = undefined
     * [    1,  1000] = Obsolete EPSG/POSC Geographic Codes
     * [ 1001,  3999] = Reserved by GeoTIFF
     * [ 4000, 4199]  = EPSG GCS Based on Ellipsoid only
     * [ 4200, 4999]  = EPSG GCS Based on EPSG Datum
     * [ 5000, 32766] = Reserved by GeoTIFF
     * 32767          = user-defined GCS
     * [32768, 65535] = Private User Implementations
     */
    GEOGRAPHIC_CRS(4200, 4999),

    /**
     * Projected CRS means (Projected CS Type Codes in geotiff).
     * Example: PCS_WGS84_UTM_zone_7S = 32707, PCS_NAD27_UTM_zone_21N = 26721 etc ...
     *
     * Ranges:
     * [    1,   1000]  = Obsolete EPSG/POSC Projection System Codes
     * [20000,  32760]  = EPSG Projection System codes
     * 32767            = user-defined
     * [32768,  65535]  = Private User Implementations
     *
     * and also (Projection Codes in geotiff)
     * Note: Projections do not include GCS or PCS definitions. If possible, use the PCS code for standard projected coordinate systems, and use this code only if nonstandard datums are required.
     * Ranges:
     * 0 = undefined
     * [    1,  9999] = Obsolete EPSG/POSC Projection codes
     * [10000, 19999] = EPSG/POSC Projection codes
     * 32767          = user-defined
     * [32768, 65535] = Private User Implementations
     */
    PROJECTED_CRS(10000, 32760),

    /**
     * Prime Meridian Codes.
     *
     * Ranges:
     * 0 = undefined
     * [    1,   100] = Obsolete EPSG/POSC Prime Meridian codes
     * [  101,  7999] = Reserved by GeoTIFF
     * [ 8000,  8999] = EPSG Prime Meridian Codes
     * [ 9000, 32766] = Reserved by GeoTIFF
     * 32767          = user-defined
     * [32768, 65535] = Private User Implementations
     */
    PRIME_MERIDIAN(8000, 8999),

    /**
     * Geodetic Datum Codes.
     *
     * Ranges:
     * 0 = undefined
     * [    1,  1000] = Obsolete EPSG/POSC Datum Codes
     * [ 1001,  5999] = Reserved by GeoTIFF
     * [ 6000, 6199]  = EPSG Datum Based on Ellipsoid only
     * [ 6200, 6999]  = EPSG Datum Based on EPSG Datum
     * [ 6322, 6327]  = WGS Datum
     * [ 6900, 6999]  = Archaic Datum
     * [ 7000, 32766] = Reserved by GeoTIFF
     * 32767          = user-defined GCS
     * [32768, 65535] = Private User Implementations
     *
     */
    DATUM(6000, 6999),

    /**
     * Ellipsoid Codes
     *
     * Ranges:
     * 0 = undefined
     * [    1, 1000]  = Obsolete EPSG/POSC Ellipsoid codes
     * [1001,  6999]  = Reserved by GeoTIFF
     * [7000,  7999]  = EPSG Ellipsoid codes
     * [8000, 32766]  = Reserved by GeoTIFF
     * 32767          = user-defined
     * [32768, 65535] = Private User Implementations
     *
     */
    ELLIPSOID(7000, 7999),

    /**
     * Units.
     * Linear  [9000 9099]
     * Angular [9100, 9199]
     *
     * Ranges:
     * 0             = undefined
     * [   1,  2000] = Obsolete GeoTIFF codes
     * [2001,  8999] = Reserved by GeoTIFF
     * [9000,  9099] = EPSG Linear Units.
     * [9100,  9199] = EPSG Angular Units.
     * 32767         = user-defined unit
     * [32768, 65535]= Private User Implementations
     */
    UNITS(9000, 9199);//-- maybe separate in two different enum.

//------------------------------------------------------------------------------
    /**
     * Returns the {@link GeoKeysRange} enum which contain the given key.
     *
     * @param key
     * @return
     */
    static GeoKeysRange getRange(final int key) {
        for (GeoKeysRange value : values()) {
            if (value.contain(key)) return value;
        }
        return null;//-- TODO exception unexpected key
    }

//------------------------------------------------------------------------------
    /**
     * Minimum inclusive range value and
     * Maximum exclusive range value.
     */
    private final int minRange, maxRange;

    /**
     * Create a Range enum with range borders.
     *
     * @param minimumRange <strong>inclusive</strong> minimum range value.
     * @param maximumRange <strong>inclusive</strong> maximum range value.
     */
    private GeoKeysRange(final int minimumRange, final int maximumRange) {
        minRange = minimumRange;
        maxRange = maximumRange;
    }

    /**
     * Returns {@code true} if the key is within this range enum, else {@code false}.
     *
     * @param key
     * @return true if key is within else false.
     */
    private boolean contain(final int key) {
        return key >= minRange && key <= maxRange;
    }
}
