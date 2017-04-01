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

import java.lang.reflect.Field;


/**
 * Identifiers (usually EPSG codes) associated to {@link GeoKeys}.
 * Those identifiers do not need to be declared in the main {@code sis-geotiff} module
 * because the GeoTIFF reader uses the EPSG database instead or declare those identifiers
 * in the {@link org.apache.sis.internal.referencing.provider} package instead.
 * However those identifiers are useful for verification purposes during tests.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class GeoIdentifiers {
    /**
     * Do not allow instantiation of this class.
     */
    private GeoIdentifiers() {
    }

    /*
     * 6.3.3.3 Coordinate Transformation Codes
     *
     * Ranges:
     *   [    0        ]  =  undefined
     *   [    1 … 16383]  =  GeoTIFF Coordinate Transformation codes
     *   [16384 … 32766]  =  Reserved by GeoTIFF
     *   [        32767]  =  user-defined
     *   [32768 … 65535]  =  Private User Implementations
     */
    public static final short CT_TransverseMercator             =  1;
    public static final short CT_TransvMercator_Modified_Alaska =  2;
    public static final short CT_ObliqueMercator                =  3;
    public static final short CT_ObliqueMercator_Laborde        =  4;
    public static final short CT_ObliqueMercator_Rosenmund      =  5;
    public static final short CT_ObliqueMercator_Spherical      =  6;
    public static final short CT_Mercator                       =  7;
    public static final short CT_LambertConfConic_2SP           =  8;
    public static final short CT_LambertConfConic_1SP           =  9;
    public static final short CT_LambertAzimEqualArea           = 10;
    public static final short CT_AlbersEqualArea                = 11;
    public static final short CT_AzimuthalEquidistant           = 12;
    public static final short CT_EquidistantConic               = 13;
    public static final short CT_Stereographic                  = 14;
    public static final short CT_PolarStereographic             = 15;
    public static final short CT_ObliqueStereographic           = 16;
    public static final short CT_Equirectangular                = 17;
    public static final short CT_CassiniSoldner                 = 18;
    public static final short CT_Gnomonic                       = 19;
    public static final short CT_MillerCylindrical              = 20;
    public static final short CT_Orthographic                   = 21;
    public static final short CT_Polyconic                      = 22;
    public static final short CT_Robinson                       = 23;
    public static final short CT_Sinusoidal                     = 24;
    public static final short CT_VanDerGrinten                  = 25;
    public static final short CT_NewZealandMapGrid              = 26;
    public static final short CT_TransvMercator_SouthOriented   = 27;
    public static final short CT_CylindricalEqualArea           = 28;

    // Aliases:
    public static final short CT_AlaskaConformal              =  CT_TransvMercator_Modified_Alaska;
    public static final short CT_TransvEquidistCylindrical    =  CT_CassiniSoldner;
    public static final short CT_ObliqueMercator_Hotine       =  CT_ObliqueMercator;
    public static final short CT_SwissObliqueCylindrical      =  CT_ObliqueMercator_Rosenmund;
    public static final short CT_GaussBoaga                   =  CT_TransverseMercator;
    public static final short CT_GaussKruger                  =  CT_TransverseMercator;
    public static final short CT_LambertConfConic             =  CT_LambertConfConic_2SP ;
    public static final short CT_LambertConfConic_Helmert     =  CT_LambertConfConic_1SP;
    public static final short CT_SouthOrientedGaussConformal  =  CT_TransvMercator_SouthOriented;

    /*
     * 6.3.1.3 Linear Units Codes
     *
     *  There are several different kinds of units that may be used in geographically related raster data:
     *  linear units, angular units, units of time (e.g. for radar-return), CCD-voltages, etc.
     *  For this reason there will be a single, unique range for each kind of unit,
     *  broken down into the following currently defined ranges:
     *
     *  Ranges:
     *     [    0        ]  =  undefined
     *     [    1 …  2000]  =  Obsolete GeoTIFF codes
     *     [ 2001 …  8999]  =  Reserved by GeoTIFF
     *     [ 9000 …  9099]  =  EPSG Linear Units.
     *     [ 9100 …  9199]  =  EPSG Angular Units.
     *     [32767        ]  =  user-defined unit
     *     [32768 … 65535]  =  Private User Implementations
     *
     *  Linear Unit Values (See the ESPG/POSC tables for definition):
     */
    public static final short Linear_Meter                       = 9001;
    public static final short Linear_Foot                        = 9002;
    public static final short Linear_Foot_US_Survey              = 9003;
    public static final short Linear_Foot_Modified_American      = 9004;
    public static final short Linear_Foot_Clarke                 = 9005;
    public static final short Linear_Foot_Indian                 = 9006;
    public static final short Linear_Link                        = 9007;
    public static final short Linear_Link_Benoit                 = 9008;
    public static final short Linear_Link_Sears                  = 9009;
    public static final short Linear_Chain_Benoit                = 9010;
    public static final short Linear_Chain_Sears                 = 9011;
    public static final short Linear_Yard_Sears                  = 9012;
    public static final short Linear_Yard_Indian                 = 9013;
    public static final short Linear_Fathom                      = 9014;
    public static final short Linear_Mile_International_Nautical = 9015;

    /*
     * 6.3.1.4 Angular Units Codes
     * These codes shall be used for any key that requires specification of an angular unit of measurement.
     */
    public static final short Angular_Radian         = 9101;
    public static final short Angular_Degree         = 9102;
    public static final short Angular_Arc_Minute     = 9103;
    public static final short Angular_Arc_Second     = 9104;
    public static final short Angular_Grad           = 9105;
    public static final short Angular_Gon            = 9106;
    public static final short Angular_DMS            = 9107;
    public static final short Angular_DMS_Hemisphere = 9108;

    /**
     * Returns the name of the given code. Implementation of this method is inefficient,
     * but it should rarely be invoked (mostly for formatting error messages).
     */
    static String name(final short tag) {
        try {
            for (final Field field : GeoIdentifiers.class.getFields()) {
                if (field.getType() == Short.TYPE) {
                    if (field.getShort(null) == tag) {
                        return field.getName();
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);        // Should never happen because we asked only for public fields.
        }
        return Integer.toHexString(Short.toUnsignedInt(tag));
    }

    /**
     * Returns the numerical value of the given GeoTIFF key name.
     * This method is the converse of {@link #name(short)}.
     */
    static short code(final String name) {
        try {
            return GeoIdentifiers.class.getField(name).getShort(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
