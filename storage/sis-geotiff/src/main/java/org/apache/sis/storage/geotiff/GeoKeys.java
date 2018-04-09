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
 * GeoTIFF keys associated to values needed for building {@link CoordinateReferenceSystem} instances
 * and {@link MathTransform} "grid to CRS". In this class, field names are close to GeoTIFF key names
 * with the {@code "GeoKey"} suffix omitted. For that reason, many of those field names do not follow
 * usual Java convention for constants.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class GeoKeys {
    /**
     * Do not allow instantiation of this class.
     */
    private GeoKeys() {
    }

    // 6.2.1 GeoTIFF Configuration Keys
    /** Section 6.3.1.1 Codes. */ public static final short ModelType            = 1024;
    /** Section 6.3.1.2 Codes. */ public static final short RasterType           = 1025;
    /** Documentation.         */ public static final short Citation             = 1026;

    // 6.2.2 Geographic CS Parameter Keys
    /** Section 6.3.2.1 Codes. */ public static final short GeographicType       = 2048;
    /** Documentation.         */ public static final short GeogCitation         = 2049;
    /** Section 6.3.2.2 Codes. */ public static final short GeodeticDatum        = 2050;
    /** Section 6.3.2.4 codes. */ public static final short PrimeMeridian        = 2051;
    /** Section 6.3.1.3 Codes. */ public static final short GeogLinearUnits      = 2052;
    /** Relative to meters.    */ public static final short GeogLinearUnitSize   = 2053;
    /** Section 6.3.1.4 Codes. */ public static final short AngularUnits         = 2054;
    /** Relative to radians.   */ public static final short AngularUnitSize      = 2055;
    /** Section 6.3.2.3 Codes. */ public static final short Ellipsoid            = 2056;
    /** In GeogLinearUnits.    */ public static final short SemiMajorAxis        = 2057;
    /** In GeogLinearUnits.    */ public static final short SemiMinorAxis        = 2058;
    /** A ratio.               */ public static final short InvFlattening        = 2059;
    /** Section 6.3.1.4 Codes. */ public static final short AzimuthUnits         = 2060;
    /** In AngularUnit.        */ public static final short PrimeMeridianLong    = 2061;

    // 6.2.3 Projected CS Parameter Keys
    /** Section 6.3.3.1 codes. */ public static final short ProjectedCSType      = 3072;
    /** Documentation.         */ public static final short PCSCitation          = 3073;
    /** Section 6.3.3.2 codes. */ public static final short Projection           = 3074;
    /** Section 6.3.3.3 codes. */ public static final short CoordTrans           = 3075;
    /** Section 6.3.1.3 codes. */ public static final short LinearUnits          = 3076;
    /** Relative to meters.    */ public static final short LinearUnitSize       = 3077;
    /** In AngularUnit.        */ public static final short StdParallel1         = 3078;    // First projection parameter
    /** In AngularUnit.        */ public static final short StdParallel2         = 3079;
    /** In AngularUnit.        */ public static final short NatOriginLong        = 3080;
    /** In AngularUnit.        */ public static final short NatOriginLat         = 3081;
    /** In LinearUnits.        */ public static final short FalseEasting         = 3082;
    /** In LinearUnits.        */ public static final short FalseNorthing        = 3083;
    /** In AngularUnit.        */ public static final short FalseOriginLong      = 3084;
    /** In AngularUnit.        */ public static final short FalseOriginLat       = 3085;
    /** In LinearUnits.        */ public static final short FalseOriginEasting   = 3086;
    /** In LinearUnits.        */ public static final short FalseOriginNorthing  = 3087;
    /** In AngularUnit.        */ public static final short CenterLong           = 3088;
    /** In AngularUnit.        */ public static final short CenterLat            = 3089;
    /** In LinearUnits.        */ public static final short CenterEasting        = 3090;
    /** In LinearUnits.        */ public static final short CenterNorthing       = 3091;
    /** A ratio.               */ public static final short ScaleAtNatOrigin     = 3092;
    /** A ratio.               */ public static final short ScaleAtCenter        = 3093;
    /** In AzimuthUnit.        */ public static final short AzimuthAngle         = 3094;
    /** In AngularUnit.        */ public static final short StraightVertPoleLong = 3095;
    /** In AzimuthUnit.        */ public static final short RectifiedGridAngle   = 3096;    // Last projection parameter (for now)

    // 6.2.4 Vertical CS Keys
    /** Section 6.3.4.1 codes. */ public static final short VerticalCSType       = 4096;
    /** Documentation.         */ public static final short VerticalCitation     = 4097;
    /** Section 6.3.4.2 codes. */ public static final short VerticalDatum        = 4098;
    /** Section 6.3.1.3 codes. */ public static final short VerticalUnits        = 4099;

    /**
     * Enumeration of return values for the {@link #unitOf(short)} method.
     */
    static final int RATIO = 0, LINEAR = 1, ANGULAR = 2, AZIMUTH = 3;

    /**
     * Returns the unit of measurement for the given map projection parameter.
     *
     * @param  key  GeoTIFF key for which to get the unit of associated map projection parameter value.
     * @return one of {@link #RATIO}, {@link #LINEAR}, {@link #ANGULAR}, {@link #AZIMUTH} codes,
     *         or -1 if the given key is not for a map projection parameter.
     */
    static int unitOf(final short key) {
        if (key < StdParallel1 || key > RectifiedGridAngle) {
            return -1;
        }
        switch (key) {
            case FalseEasting:
            case FalseNorthing:
            case FalseOriginEasting:
            case FalseOriginNorthing:
            case CenterEasting:
            case CenterNorthing:     return LINEAR;
            case ScaleAtNatOrigin:
            case ScaleAtCenter:      return RATIO;
            case RectifiedGridAngle: // Note: GDAL seems to use angular unit here.
            case AzimuthAngle:       return AZIMUTH;
            default:                 return ANGULAR;
        }
    }

    /**
     * Returns the name of the given key. Implementation of this method is inefficient,
     * but it should rarely be invoked (mostly for formatting error messages).
     */
    static String name(final short key) {
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
