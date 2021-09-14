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
 * Enumeration values associated to some {@link GeoKeys}. In this class, field names are close to GeoTIFF code values.
 * For that reason, many of those field names do not follow usual Java convention for constants.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
final class GeoCodes {
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
    static final short missing = -1;

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
}
