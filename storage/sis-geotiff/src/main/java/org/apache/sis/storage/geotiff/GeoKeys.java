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
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;


/**
 * All Geographic Keys needed for building {@link CoordinateReferenceSystem} instances
 * and {@link MathTransform} "grid to CRS" from TIFF tags values.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class GeoKeys {
    /**
     * Do not allow instantiation of this class.
     */
    private GeoKeys() {
    }

    ////////////////////////////////////////////////////////////////////////////
    // KEYS, values are taken from :
    // http://www.remotesensing.org/geotiff/spec/geotiff6.html#6
    ////////////////////////////////////////////////////////////////////////////

    //6.2.1 GeoTIFF Configuration Keys
    public static final short GTModelTypeGeoKey           = 1024; /* Section 6.3.1.1 Codes */
    public static final short GTRasterTypeGeoKey          = 1025; /* Section 6.3.1.2 Codes */
    public static final short GTCitationGeoKey            = 1026; /* documentation */

    /*
     * 6.3.1.1 Model Type Codes
     *
     * Ranges:
     *   0              = undefined
     *   [   1,  32766] = GeoTIFF Reserved Codes
     *   32767          = user-defined
     *   [32768, 65535] = Private User Implementations
     *
     * Notes:
     *   1. ModelTypeGeographic and ModelTypeProjected
     *   correspond to the FGDC metadata Geographic and
     *   Planar-Projected coordinate system types.
     */
    //GeoTIFF defined CS Model Type Codes:
    public static final short ModelTypeProjected   = 1;   /* Projection Coordinate System         */
    public static final short ModelTypeGeographic  = 2;   /* Geographic latitude-longitude System */
    public static final short ModelTypeGeocentric  = 3;   /* Geocentric (X,Y,Z) Coordinate System */
    ////////////////////////////////////////////////////////////////////////////
    // Codes
    ////////////////////////////////////////////////////////////////////////////

    public static final short GTUserDefinedGeoKey         = 32767;
    static final String GTUserDefinedGeoKey_String = "32767";

    /*
     * 6.3.3.3 Coordinate Transformation Codes
     * Ranges:
     * 0 = undefined
     * [    1, 16383] = GeoTIFF Coordinate Transformation codes
     * [16384, 32766] = Reserved by GeoTIFF
     * 32767          = user-defined
     * [32768, 65535] = Private User Implementations
     */
    public static final short CT_TransverseMercator =             1;
    public static final short CT_TransvMercator_Modified_Alaska = 2;
    public static final short CT_ObliqueMercator =                3;
    public static final short CT_ObliqueMercator_Laborde =        4;
    public static final short CT_ObliqueMercator_Rosenmund =      5;
    public static final short CT_ObliqueMercator_Spherical =      6;
    public static final short CT_Mercator =                       7;
    public static final short CT_LambertConfConic_2SP =           8;
    public static final short CT_LambertConfConic_1SP =           9;
    public static final short CT_LambertAzimEqualArea =           10;
    public static final short CT_AlbersEqualArea =                11;
    public static final short CT_AzimuthalEquidistant =           12;
    public static final short CT_EquidistantConic =               13;
    public static final short CT_Stereographic =                  14;
    public static final short CT_PolarStereographic =             15;
    public static final short CT_ObliqueStereographic =           16;
    public static final short CT_Equirectangular =                17;
    public static final short CT_CassiniSoldner =                 18;
    public static final short CT_Gnomonic =                       19;
    public static final short CT_MillerCylindrical =              20;
    public static final short CT_Orthographic =                   21;
    public static final short CT_Polyconic =                      22;
    public static final short CT_Robinson =                       23;
    public static final short CT_Sinusoidal =                     24;
    public static final short CT_VanDerGrinten =                  25;
    public static final short CT_NewZealandMapGrid =              26;
    public static final short CT_TransvMercator_SouthOriented=    27;
    //Aliases:
    public static final short CT_AlaskaConformal =                CT_TransvMercator_Modified_Alaska;
    public static final short CT_TransvEquidistCylindrical =      CT_CassiniSoldner;
    public static final short CT_ObliqueMercator_Hotine =         CT_ObliqueMercator;
    public static final short CT_SwissObliqueCylindrical =        CT_ObliqueMercator_Rosenmund;
    public static final short CT_GaussBoaga =                     CT_TransverseMercator;
    public static final short CT_GaussKruger =                    CT_TransverseMercator;
    public static final short CT_LambertConfConic =               CT_LambertConfConic_2SP ;
    public static final short CT_LambertConfConic_Helmert =       CT_LambertConfConic_1SP;
    public static final short CT_SouthOrientedGaussConformal =    CT_TransvMercator_SouthOriented;

    /*
     * 6.3.1.3 Linear Units Codes
     *
     *  There are several different kinds of units that may be used in geographically related raster data: linear units, angular units, units of time (e.g. for radar-return), CCD-voltages, etc. For this reason there will be a single, unique range for each kind of unit, broken down into the following currently defined ranges:
     *  Ranges:
     *     0             = undefined
     *     [   1,  2000] = Obsolete GeoTIFF codes
     *     [2001,  8999] = Reserved by GeoTIFF
     *     [9000,  9099] = EPSG Linear Units.
     *     [9100,  9199] = EPSG Angular Units.
     *     32767         = user-defined unit
     *     [32768, 65535]= Private User Implementations
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


    //6.2.2 Geographic CS Parameter Keys
    public static final short GeographicTypeGeoKey        = 2048; /* Section 6.3.2.1 Codes */
    public static final short GeogCitationGeoKey          = 2049; /* documentation */
    public static final short GeogGeodeticDatumGeoKey     = 2050; /* Section 6.3.2.2 Codes */
    public static final short GeogPrimeMeridianGeoKey     = 2051; /* Section 6.3.2.4 codes */
    public static final short GeogLinearUnitsGeoKey       = 2052; /* Section 6.3.1.3 Codes */
    public static final short GeogLinearUnitSizeGeoKey    = 2053; /* meters */
    public static final short GeogAngularUnitsGeoKey      = 2054; /* Section 6.3.1.4 Codes */
    public static final short GeogAngularUnitSizeGeoKey   = 2055; /* radians */
    public static final short GeogEllipsoidGeoKey         = 2056; /* Section 6.3.2.3 Codes */
    public static final short GeogSemiMajorAxisGeoKey     = 2057; /* GeogLinearUnits */
    public static final short GeogSemiMinorAxisGeoKey     = 2058; /* GeogLinearUnits */
    public static final short GeogInvFlatteningGeoKey     = 2059; /* ratio */
    public static final short GeogAzimuthUnitsGeoKey      = 2060; /* Section 6.3.1.4 Codes */
    public static final short GeogPrimeMeridianLongGeoKey = 2061; /* GeogAngularUnit */

    //6.2.3 Projected CS Parameter Keys
    public static final short ProjectedCSTypeGeoKey          = 3072;  /* Section 6.3.3.1 codes */
    public static final short PCSCitationGeoKey              = 3073;  /* documentation */
    public static final short ProjectionGeoKey               = 3074;  /* Section 6.3.3.2 codes */
    public static final short ProjCoordTransGeoKey           = 3075;  /* Section 6.3.3.3 codes */
    public static final short ProjLinearUnitsGeoKey          = 3076;  /* Section 6.3.1.3 codes */
    public static final short ProjLinearUnitSizeGeoKey       = 3077;  /* meters */
    public static final short ProjStdParallel1GeoKey         = 3078;  /* GeogAngularUnit */
    public static final short ProjStdParallel2GeoKey         = 3079;  /* GeogAngularUnit */
    public static final short ProjNatOriginLongGeoKey        = 3080;  /* GeogAngularUnit */
    public static final short ProjNatOriginLatGeoKey         = 3081;  /* GeogAngularUnit */
    public static final short ProjFalseEastingGeoKey         = 3082;  /* ProjLinearUnits */
    public static final short ProjFalseNorthingGeoKey        = 3083;  /* ProjLinearUnits */
    public static final short ProjFalseOriginLongGeoKey      = 3084;  /* GeogAngularUnit */
    public static final short ProjFalseOriginLatGeoKey       = 3085;  /* GeogAngularUnit */
    public static final short ProjFalseOriginEastingGeoKey   = 3086;  /* ProjLinearUnits */
    public static final short ProjFalseOriginNorthingGeoKey  = 3087;  /* ProjLinearUnits */
    public static final short ProjCenterLongGeoKey           = 3088;  /* GeogAngularUnit */
    public static final short ProjCenterLatGeoKey            = 3089;  /* GeogAngularUnit */
    public static final short ProjCenterEastingGeoKey        = 3090;  /* ProjLinearUnits */
    public static final short ProjCenterNorthingGeoKey       = 3091;  /* ProjLinearUnits */
    public static final short ProjScaleAtNatOriginGeoKey     = 3092;  /* ratio */
    public static final short ProjScaleAtCenterGeoKey        = 3093;  /* ratio */
    public static final short ProjAzimuthAngleGeoKey         = 3094;  /* GeogAzimuthUnit */
    public static final short ProjStraightVertPoleLongGeoKey = 3095;  /* GeogAngularUnit */
    //Aliases:
    public static final short ProjStdParallelGeoKey       = ProjStdParallel1GeoKey;
    public static final short ProjOriginLongGeoKey        = ProjNatOriginLongGeoKey;
    public static final short ProjOriginLatGeoKey         = ProjNatOriginLatGeoKey;
    public static final short ProjScaleAtOriginGeoKey     = ProjScaleAtNatOriginGeoKey;

    //6.2.4 Vertical CS Keys
    public static final short VerticalCSTypeGeoKey    = 4096;   /* Section 6.3.4.1 codes */
    public static final short VerticalCitationGeoKey  = 4097;   /* documentation */
    public static final short VerticalDatumGeoKey     = 4098;   /* Section 6.3.4.2 codes */
    public static final short VerticalUnitsGeoKey     = 4099;   /* Section 6.3.1.3 codes */

    /**
     * Returns key name if exist.
     *
     * @return tag  name from one of the constants.
     */
    static String getName(final short tag) {
        try {
            for (final Field field : GeoKeys.class.getDeclaredFields()) {
                if (field.getType() == Short.TYPE) {
                    if (field.getShort(null) == tag) {
                        return field.getName();
                    }
                }
            }
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex); // Should never happen.
        }
        return Integer.toHexString(Short.toUnsignedInt(tag));
    }
}
