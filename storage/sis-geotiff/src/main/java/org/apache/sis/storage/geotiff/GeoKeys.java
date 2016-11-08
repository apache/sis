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
    static class Configuration {

        //6.2.1 GeoTIFF Configuration Keys
        static final int GTModelTypeGeoKey           = 1024; /* Section 6.3.1.1 Codes */
        static final int GTRasterTypeGeoKey          = 1025; /* Section 6.3.1.2 Codes */
        static final int GTCitationGeoKey            = 1026; /* documentation */

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
        static final int ModelTypeProjected   = 1;   /* Projection Coordinate System         */
        static final int ModelTypeGeographic  = 2;   /* Geographic latitude-longitude System */
        static final int ModelTypeGeocentric  = 3;   /* Geocentric (X,Y,Z) Coordinate System */
        ////////////////////////////////////////////////////////////////////////////
        // Codes
        ////////////////////////////////////////////////////////////////////////////

        static final short GTUserDefinedGeoKey         = 32767;
        static final String GTUserDefinedGeoKey_String = "32767";

        /**
         * Return tag Name from {@link GeoTiffConstants} class.
         *
         * @param tag
         * @return tag Name from {@link GeoTiffConstants} class.
         */
        static String getName(final int tag) {
            try {
                for (final Field field : CRS.class.getDeclaredFields()) {
                    if (field.getType() == Integer.TYPE) {
                        if (field.getInt(null) == tag) {
                            return field.getName();
                        }
                    }
                }
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError(ex); // Should never happen.
            }
            return Integer.toHexString(tag);
        }

        static boolean contain(int key) {
            try {
                for (final Field field : CRS.class.getDeclaredFields()) {
                    if (field.getType() == Integer.TYPE) {
                        if (field.getInt(null) == key) {
                            return true;
                        }
                    }
                }
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError(ex); // Should never happen.
            }
            return false;
        }
    }

    static class Operation_Method {
        /*
         * 6.3.3.3 Coordinate Transformation Codes
         * Ranges:
         * 0 = undefined
         * [    1, 16383] = GeoTIFF Coordinate Transformation codes
         * [16384, 32766] = Reserved by GeoTIFF
         * 32767          = user-defined
         * [32768, 65535] = Private User Implementations
         */
        static final int CT_TransverseMercator =             1;
        static final int CT_TransvMercator_Modified_Alaska = 2;
        static final int CT_ObliqueMercator =                3;
        static final int CT_ObliqueMercator_Laborde =        4;
        static final int CT_ObliqueMercator_Rosenmund =      5;
        static final int CT_ObliqueMercator_Spherical =      6;
        static final int CT_Mercator =                       7;
        static final int CT_LambertConfConic_2SP =           8;
        static final int CT_LambertConfConic_1SP =           9;
        static final int CT_LambertAzimEqualArea =           10;
        static final int CT_AlbersEqualArea =                11;
        static final int CT_AzimuthalEquidistant =           12;
        static final int CT_EquidistantConic =               13;
        static final int CT_Stereographic =                  14;
        static final int CT_PolarStereographic =             15;
        static final int CT_ObliqueStereographic =           16;
        static final int CT_Equirectangular =                17;
        static final int CT_CassiniSoldner =                 18;
        static final int CT_Gnomonic =                       19;
        static final int CT_MillerCylindrical =              20;
        static final int CT_Orthographic =                   21;
        static final int CT_Polyconic =                      22;
        static final int CT_Robinson =                       23;
        static final int CT_Sinusoidal =                     24;
        static final int CT_VanDerGrinten =                  25;
        static final int CT_NewZealandMapGrid =              26;
        static final int CT_TransvMercator_SouthOriented=    27;
        //Aliases:
        static final int CT_AlaskaConformal =                CT_TransvMercator_Modified_Alaska;
        static final int CT_TransvEquidistCylindrical =      CT_CassiniSoldner;
        static final int CT_ObliqueMercator_Hotine =         CT_ObliqueMercator;
        static final int CT_SwissObliqueCylindrical =        CT_ObliqueMercator_Rosenmund;
        static final int CT_GaussBoaga =                     CT_TransverseMercator;
        static final int CT_GaussKruger =                    CT_TransverseMercator;
        static final int CT_LambertConfConic =               CT_LambertConfConic_2SP ;
        static final int CT_LambertConfConic_Helmert =       CT_LambertConfConic_1SP;
        static final int CT_SouthOrientedGaussConformal =    CT_TransvMercator_SouthOriented;

        /**
         * Return tag Name from {@link GeoTiffConstants} class.
         *
         * @param tag
         * @return tag Name from {@link GeoTiffConstants} class.
         */
        static String getName(final int tag) {
            try {
                for (final Field field : CRS.class.getDeclaredFields()) {
                    if (field.getType() == Integer.TYPE) {
                        if (field.getInt(null) == tag) {
                            return field.getName();
                        }
                    }
                }
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError(ex); // Should never happen.
            }
            return Integer.toHexString(tag);
        }

        static boolean contain(int key) {
            try {
                for (final Field field : CRS.class.getDeclaredFields()) {
                    if (field.getType() == Integer.TYPE) {
                        if (field.getInt(null) == key) {
                            return true;
                        }
                    }
                }
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError(ex); // Should never happen.
            }
            return false;
        }
    }

    static class Units {
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
        static final int Linear_Meter                       = 9001;
        static final int Linear_Foot                        = 9002;
        static final int Linear_Foot_US_Survey              = 9003;
        static final int Linear_Foot_Modified_American      = 9004;
        static final int Linear_Foot_Clarke                 = 9005;
        static final int Linear_Foot_Indian                 = 9006;
        static final int Linear_Link                        = 9007;
        static final int Linear_Link_Benoit                 = 9008;
        static final int Linear_Link_Sears                  = 9009;
        static final int Linear_Chain_Benoit                = 9010;
        static final int Linear_Chain_Sears                 = 9011;
        static final int Linear_Yard_Sears                  = 9012;
        static final int Linear_Yard_Indian                 = 9013;
        static final int Linear_Fathom                      = 9014;
        static final int Linear_Mile_International_Nautical = 9015;

        /*
         * 6.3.1.4 Angular Units Codes
         * These codes shall be used for any key that requires specification of an angular unit of measurement.
         */
        static final int Angular_Radian         = 9101;
        static final int Angular_Degree         = 9102;
        static final int Angular_Arc_Minute     = 9103;
        static final int Angular_Arc_Second     = 9104;
        static final int Angular_Grad           = 9105;
        static final int Angular_Gon            = 9106;
        static final int Angular_DMS            = 9107;
        static final int Angular_DMS_Hemisphere = 9108;

        /**
         * Return tag Name from {@link GeoTiffConstants} class.
         *
         * @param tag
         * @return tag Name from {@link GeoTiffConstants} class.
         */
        static String getName(final int tag) {
            try {
                for (final Field field : CRS.class.getDeclaredFields()) {
                    if (field.getType() == Integer.TYPE) {
                        if (field.getInt(null) == tag) {
                            return field.getName();
                        }
                    }
                }
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError(ex); // Should never happen.
            }
            return Integer.toHexString(tag);
        }

        static boolean contain(int key) {
            try {
                for (final Field field : CRS.class.getDeclaredFields()) {
                    if (field.getType() == Integer.TYPE) {
                        if (field.getInt(null) == key) {
                            return true;
                        }
                    }
                }
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError(ex); // Should never happen.
            }
            return false;
        }
    }


    static class CRS {

        //6.2.2 Geographic CS Parameter Keys
        static final int GeographicTypeGeoKey        = 2048; /* Section 6.3.2.1 Codes */
        static final int GeogCitationGeoKey          = 2049; /* documentation */
        static final int GeogGeodeticDatumGeoKey     = 2050; /* Section 6.3.2.2 Codes */
        static final int GeogPrimeMeridianGeoKey     = 2051; /* Section 6.3.2.4 codes */
        static final int GeogLinearUnitsGeoKey       = 2052; /* Section 6.3.1.3 Codes */
        static final int GeogLinearUnitSizeGeoKey    = 2053; /* meters */
        static final int GeogAngularUnitsGeoKey      = 2054; /* Section 6.3.1.4 Codes */
        static final int GeogAngularUnitSizeGeoKey   = 2055; /* radians */
        static final int GeogEllipsoidGeoKey         = 2056; /* Section 6.3.2.3 Codes */
        static final int GeogSemiMajorAxisGeoKey     = 2057; /* GeogLinearUnits */
        static final int GeogSemiMinorAxisGeoKey     = 2058; /* GeogLinearUnits */
        static final int GeogInvFlatteningGeoKey     = 2059; /* ratio */
        static final int GeogAzimuthUnitsGeoKey      = 2060; /* Section 6.3.1.4 Codes */
        static final int GeogPrimeMeridianLongGeoKey = 2061; /* GeogAngularUnit */

        //6.2.3 Projected CS Parameter Keys
        static final int ProjectedCSTypeGeoKey          = 3072;  /* Section 6.3.3.1 codes */
        static final int PCSCitationGeoKey              = 3073;  /* documentation */
        static final int ProjectionGeoKey               = 3074;  /* Section 6.3.3.2 codes */
        static final int ProjCoordTransGeoKey           = 3075;  /* Section 6.3.3.3 codes */
        static final int ProjLinearUnitsGeoKey          = 3076;  /* Section 6.3.1.3 codes */
        static final int ProjLinearUnitSizeGeoKey       = 3077;  /* meters */
        static final int ProjStdParallel1GeoKey         = 3078;  /* GeogAngularUnit */
        static final int ProjStdParallel2GeoKey         = 3079;  /* GeogAngularUnit */
        static final int ProjNatOriginLongGeoKey        = 3080;  /* GeogAngularUnit */
        static final int ProjNatOriginLatGeoKey         = 3081;  /* GeogAngularUnit */
        static final int ProjFalseEastingGeoKey         = 3082;  /* ProjLinearUnits */
        static final int ProjFalseNorthingGeoKey        = 3083;  /* ProjLinearUnits */
        static final int ProjFalseOriginLongGeoKey      = 3084;  /* GeogAngularUnit */
        static final int ProjFalseOriginLatGeoKey       = 3085;  /* GeogAngularUnit */
        static final int ProjFalseOriginEastingGeoKey   = 3086;  /* ProjLinearUnits */
        static final int ProjFalseOriginNorthingGeoKey  = 3087;  /* ProjLinearUnits */
        static final int ProjCenterLongGeoKey           = 3088;  /* GeogAngularUnit */
        static final int ProjCenterLatGeoKey            = 3089;  /* GeogAngularUnit */
        static final int ProjCenterEastingGeoKey        = 3090;  /* ProjLinearUnits */
        static final int ProjCenterNorthingGeoKey       = 3091;  /* ProjLinearUnits */
        static final int ProjScaleAtNatOriginGeoKey     = 3092;  /* ratio */
        static final int ProjScaleAtCenterGeoKey        = 3093;  /* ratio */
        static final int ProjAzimuthAngleGeoKey         = 3094;  /* GeogAzimuthUnit */
        static final int ProjStraightVertPoleLongGeoKey = 3095;  /* GeogAngularUnit */
        //Aliases:
        static final int ProjStdParallelGeoKey       = ProjStdParallel1GeoKey;
        static final int ProjOriginLongGeoKey        = ProjNatOriginLongGeoKey;
        static final int ProjOriginLatGeoKey         = ProjNatOriginLatGeoKey;
        static final int ProjScaleAtOriginGeoKey     = ProjScaleAtNatOriginGeoKey;

        //6.2.4 Vertical CS Keys
        static final int VerticalCSTypeGeoKey    = 4096;   /* Section 6.3.4.1 codes */
        static final int VerticalCitationGeoKey  = 4097;   /* documentation */
        static final int VerticalDatumGeoKey     = 4098;   /* Section 6.3.4.2 codes */
        static final int VerticalUnitsGeoKey     = 4099;   /* Section 6.3.1.3 codes */

        /**
         * Return tag Name from {@link GeoTiffConstants} class.
         *
         * @param tag
         * @return tag Name from {@link GeoTiffConstants} class.
         */
        static String getName(final int tag) {
            try {
                for (final Field field : CRS.class.getDeclaredFields()) {
                    if (field.getType() == Integer.TYPE) {
                        if (field.getInt(null) == tag) {
                            return field.getName();
                        }
                    }
                }
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError(ex); // Should never happen.
            }
            return Integer.toHexString(tag);
        }

        static boolean contain(int key) {
            try {
                for (final Field field : CRS.class.getDeclaredFields()) {
                    if (field.getType() == Integer.TYPE) {
                        if (field.getInt(null) == key) {
                            return true;
                        }
                    }
                }
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError(ex); // Should never happen.
            }
            return false;
        }
    }

    static class GridToCrs {

    }

}
