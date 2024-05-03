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

import java.lang.reflect.Field;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;


/**
 * GeoTIFF keys associated to values needed for building {@link CoordinateReferenceSystem} instances
 * and {@link MathTransform} "grid to CRS". In this class, field names are GeoTIFF key names, except
 * for the following departures:
 *
 * <ul>
 *   <li>The {@code "GeoKey"} suffix is omitted for all keys<.</li>
 *   <li>The {@code "Proj"} prefix is omitted for all map projection parameters.
 *       The resulting map projection parameter names are the same as published on
 *       <a href="https://gdal.org/proj_list/">Map Tools projection list</a>.</li>
 *   <li>The ellipsoid axis lengths and inverse flattening factor have the {@code Ellipsoid} prefix omitted.</li>
 * </ul>
 *
 * Because of this convention, the field names do not follow usual Java convention for constants.
 *
 * <p>The current version of this class uses GeoTIFF 1.1 names.
 * When they differ from GeoTIFF 1.0, the old name is written in comment.
 * Key names may be changed again in any future version if the GeoTIFF specification changes.</p>
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class GeoKeys {
    /**
     * Do not allow instantiation of this class.
     */
    private GeoKeys() {
    }

    // GeoTIFF Configuration Keys
    /** CRS type.              */ public static final short ModelType              = 1024;
    /** Pixel area or point.   */ public static final short RasterType             = 1025;
    /** Documentation.         */ public static final short Citation               = 1026;

    // Geographic CRS Parameter Keys
    /** EPSG code.             */ public static final short GeodeticCRS            = 2048;    // Was `GeographicType`
    /** Documentation.         */ public static final short GeodeticCitation       = 2049;    // Was `GeogCitation`
    /** For user-defined CRS.  */ public static final short GeodeticDatum          = 2050;    // Was `GeogGeodeticDatum`
    /** For user-defined CRS.  */ public static final short PrimeMeridian          = 2051;    // Was `GeogPrimeMeridian`
    /** For geodetic axes.     */ public static final short GeogLinearUnits        = 2052;    // Actually geodetic, not necessarily geographic.
    /** Relative to meters.    */ public static final short GeogLinearUnitSize     = 2053;
    /** For geodetic axes.     */ public static final short GeogAngularUnits       = 2054;
    /** Relative to radians.   */ public static final short GeogAngularUnitSize    = 2055;
    /** For user-defined CRS.  */ public static final short Ellipsoid              = 2056;    // Was `GeogEllipsoid`
    /** In GeogLinearUnits.    */ public static final short SemiMajorAxis          = 2057;    // Was `GeogSemiMajorAxis`
    /** In GeogLinearUnits.    */ public static final short SemiMinorAxis          = 2058;    // Was `GeogSemiMinorAxis`
    /** A ratio.               */ public static final short InvFlattening          = 2059;    // Was `GeogInvFlattening`
    /** For some parameters.   */ public static final short GeogAzimuthUnits       = 2060;
    /** In GeogAngularUnits.   */ public static final short PrimeMeridianLongitude = 2061;    // Was `GeogPrimeMeridianLong`

    // Projected CRS Parameter Keys, omitting "Proj" prefix in map projection parameters
    /** EPSG code.             */ public static final short ProjectedCRS           = 3072;    // Was `ProjectedCSType`
    /** Documentation.         */ public static final short ProjectedCitation      = 3073;    // Was `PCSCitation`
    /** For user-defined CRS.  */ public static final short Projection             = 3074;
    /** For user-defined CRS.  */ public static final short ProjMethod             = 3075;    // Was `ProjCoordTrans`
    /** For projected axes.    */ public static final short ProjLinearUnits        = 3076;
    /** Relative to meters.    */ public static final short ProjLinearUnitSize     = 3077;
    /** In GeogAngularUnits.   */ public static final short StdParallel1           = 3078;    // First projection parameter
    /** In GeogAngularUnits.   */ public static final short StdParallel2           = 3079;
    /** In GeogAngularUnits.   */ public static final short NatOriginLong          = 3080;
    /** In GeogAngularUnits.   */ public static final short NatOriginLat           = 3081;
    /** In ProjLinearUnits.    */ public static final short FalseEasting           = 3082;
    /** In ProjLinearUnits.    */ public static final short FalseNorthing          = 3083;
    /** In GeogAngularUnits.   */ public static final short FalseOriginLong        = 3084;
    /** In GeogAngularUnits.   */ public static final short FalseOriginLat         = 3085;
    /** In ProjLinearUnits.    */ public static final short FalseOriginEasting     = 3086;
    /** In ProjLinearUnits.    */ public static final short FalseOriginNorthing    = 3087;
    /** In GeogAngularUnits.   */ public static final short CenterLong             = 3088;
    /** In GeogAngularUnits.   */ public static final short CenterLat              = 3089;
    /** In ProjLinearUnits.    */ public static final short CenterEasting          = 3090;
    /** In ProjLinearUnits.    */ public static final short CenterNorthing         = 3091;
    /** A ratio.               */ public static final short ScaleAtNatOrigin       = 3092;
    /** A ratio.               */ public static final short ScaleAtCenter          = 3093;
    /** In GeogAzimuthUnits.   */ public static final short AzimuthAngle           = 3094;
    /** In GeogAngularUnits.   */ public static final short StraightVertPoleLong   = 3095;
    /** In GeogAzimuthUnits.   */ public static final short RectifiedGridAngle     = 3096;
    /** For unit inference.    */ static final short LAST_MAP_PROJECTION_PARAMETER = RectifiedGridAngle;

    // Vertical CRS Keys
    /** EPSG code.             */ public static final short Vertical               = 4096;    // Was `VerticalCSType`
    /** Documentation.         */ public static final short VerticalCitation       = 4097;
    /** For user-defined CRS.  */ public static final short VerticalDatum          = 4098;
    /** For vertical axis.     */ public static final short VerticalUnits          = 4099;

    /**
     * Returns the name of the given key. Implementation of this method is inefficient,
     * but it should rarely be invoked (mostly for formatting error messages).
     */
    public static String name(final short key) {
        try {
            for (final Field field : GeoKeys.class.getFields()) {
                if (field.getType() == Short.TYPE) {
                    if (field.getShort(null) == key) {
                        return field.getName();
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);        // Should never happen because we asked only for public fields.
        }
        return Integer.toHexString(Short.toUnsignedInt(key));
    }
}
