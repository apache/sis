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
package org.apache.sis.storage.geotiff.base;


/**
 * Enumeration values associated to some {@link GeoKeys}. In this class, field names are close to GeoTIFF code values.
 * For that reason, many of those field names do not follow usual Java convention for constants.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class GeoCodes {
    /**
     * Do not allow instantiation of this class.
     */
    private GeoCodes() {
    }

    /**
     * The code value for undefined property.
     */
    public static final short undefined = 0;

    /**
     * The code value for a property defined by the user instead of by an EPSG code.
     */
    public static final short userDefined = 32767;

    /**
     * An alternative code for {@link #undefined} found in some GeoTIFF file.
     * This is not a standard value. This is used only in some methods implemented defensively.
     */
    public static final short missing = -1;

    /*
     * 6.3.1.1 Model Type Codes
     *
     * Ranges:
     *   [    0        ] = undefined
     *   [    1 … 32766] = GeoTIFF Reserved Codes
     *   [        32767] = user-defined
     *   [32768 … 65535] = Private User Implementations
     *
     * Notes:
     *   ModelTypeGeographic and ModelTypeProjected
     *   correspond to the FGDC metadata Geographic and
     *   Planar-Projected coordinate system types.
     */
    /** Projection Coordinate System         */ public static final short ModelTypeProjected  = 1;
    /** Geographic latitude-longitude System */ public static final short ModelTypeGeographic = 2;
    /** Geocentric (X,Y,Z) Coordinate System */ public static final short ModelTypeGeocentric = 3;

    /*
     * 6.3.1.2 Raster Type Codes
     *
     * Ranges:
     *   [            0]  =  undefined
     *   [    1 …  1023]  =  Raster Type Codes (GeoTIFF Defined)
     *   [ 1024 … 32766]  =  Reserved
     *   [        32767]  =  user-defined
     *   [32768 … 65535]  =  Private User Implementations
     *
     * Note: Use of "user-defined" or "undefined" raster codes is not recommended.
     */
    public static final short RasterPixelIsArea  = 1;
    public static final short RasterPixelIsPoint = 2;

    /**
     * The code for polar stereographic map projection.
     * This is handled as a special case for distinguishing between variants.
     */
    public static final short PolarStereographic = 15;

    /**
     * Number of GeoTIFF keys.
     * This value is verified by the {@code GeoKeysTest.verifyNumKeys()} test.
     *
     * <p>This field should be part of {@link GeoKeys}, but is declared here because we
     * need to avoid public constants that are not GeoKey names in {@code GeoKeys}.</p>
     */
    public static final int NUM_GEOKEYS = 46;

    /**
     * Number of GeoTIFF key associated to values of type {@code double}.
     *
     * <p>This field should be part of {@link GeoKeys}, but is declared here because we
     * need to avoid public constants that are not GeoKey names in {@code GeoKeys}.</p>
     */
    public static final int NUM_DOUBLE_GEOKEYS = 25;

    /**
     * Number of {@code short} values in each GeoKey entry.
     */
    public static final int ENTRY_LENGTH = 4;

    /**
     * The character used as a separator in {@link String} multi-values.
     */
    public static final char STRING_SEPARATOR = '|';
}
