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

import javax.measure.Unit;
import org.apache.sis.measure.Units;
import org.apache.sis.util.Workaround;


/**
 * GeoTIFF keys for storing units of measurement.
 * The enumeration values are ordered in increasing value of GeoTIFF key codes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public enum UnitKey {
    /**
     * Linear unit in geodetic CRS. Used for
     * the axes in user-defined geocentric Cartesian CRSs,
     * the height axis of a user-defined geographic 3D CRS, and
     * for user-defined ellipsoid axes.
     *
     * @see #PROJECTED
     */
    LINEAR(GeoKeys.GeogLinearUnits, GeoKeys.GeogLinearUnitSize, true, true, false, false),

    /**
     * Angular unit in geodetic CRS. Used for
     * the axes in user-defined geographic 2D CRSs,
     * the horizontal axes in user-defined geographic 3D CRSs,
     * the longitude from the reference meridian in user-defined prime meridians, and
     * user-defined map projection parameters that are angles.
     */
    ANGULAR(GeoKeys.GeogAngularUnits, GeoKeys.GeogAngularUnitSize, true, false, true, false),

    /**
     * Angular unit is <em>some</em> map projection parameters.
     * Used for angular units for user-defined map projection parameters
     * when these differ from the angular unit described through {@link #ANGULAR}.
     */
    AZIMUTH(GeoKeys.GeogAzimuthUnits, (short) 0, false, false, true, false),

    /**
     * Linear unit in map projections. Used for
     * the axes of a user-defined projected CRS, and
     * map projection parameters that are lengths.
     *
     * @see #LINEAR
     */
    PROJECTED(GeoKeys.ProjLinearUnits, GeoKeys.ProjLinearUnitSize, true, true, false, false),

    /**
     * Unit of axis of a user-defined vertical CRS.
     */
    VERTICAL(GeoKeys.VerticalUnits, (short) 0, true, true, false, false),

    /**
     * Unit of measurement of ratios. There is no GeoTIFF keys associated to this unit.
     */
    RATIO((short) 0, (short) 0, false, false, false, true),

    /**
     * Workaround for null return value. To be removed after upgrade to JDK 21.
     *
     * @see <a hre="https://openjdk.org/jeps/441">JEP 441</a>
     */
    @Workaround(library="JDK", version="17", fixed="21")
    NULL((short) 0, (short) 0, false, false, false, false);

    /**
     * The {@link GeoKeys} for a unit of measurement defined by an EPSG code, or 0 if none.
     */
    public final short codeKey;

    /**
     * The {@link GeoKeys} for a unit of measurement defined by a scale applied on a base unit, or 0 if none.
     */
    public final short scaleKey;

    /**
     * Whether the unit may be associated to coordinate system axes.
     */
    public final boolean isAxis;

    /**
     * Whether the key accepts linear, angular or scalar units.
     */
    private final boolean linear, angular, scalar;

    /**
     * Creates a enumeration value.
     *
     * @param  codeKey   {@link GeoKeys} for a unit defined by an EPSG code, or 0 if none.
     * @param  scaleKey  {@link GeoKeys} for a unit defined by a scale applied on a base unit, or 0 if none.
     * @param  isAxis    whether the unit may be associated to coordinate system axes.
     */
    private UnitKey(short codeKey, short scaleKey, boolean isAxis, boolean linear, boolean angular, boolean scalar) {
        this.codeKey  = codeKey;
        this.scaleKey = scaleKey;
        this.isAxis   = isAxis;
        this.linear   = linear;
        this.angular  = angular;
        this.scalar   = scalar;
    }

    /**
     * Returns the unit of measurement for the given map projection parameter.
     * The returned value should be one of {@link #RATIO}, {@link #PROJECTED},
     * {@link #ANGULAR} or {@link #AZIMUTH} enumeration values.
     *
     * If the given parameter is not a map projection parameter, then this method returns
     * {@link #LINEAR} if the parameter is a semi-axis length, or {@link #NULL} otherwise.
     *
     * @param  key  GeoTIFF key for which to get the unit of associated map projection parameter value.
     * @return the unit of measurement of the map projection parameter, or {@link #LINEAR} or {@link #NULL}
     *         if the given parameter is not a map projection parameter.
     */
    public static UnitKey ofProjectionParameter(final short key) {
        switch (key) {
            case GeoKeys.SemiMajorAxis:
            case GeoKeys.SemiMinorAxis:      return LINEAR;
            case GeoKeys.FalseEasting:
            case GeoKeys.FalseNorthing:
            case GeoKeys.FalseOriginEasting:
            case GeoKeys.FalseOriginNorthing:
            case GeoKeys.CenterEasting:
            case GeoKeys.CenterNorthing:     return PROJECTED;
            case GeoKeys.ScaleAtNatOrigin:
            case GeoKeys.ScaleAtCenter:      return RATIO;
            case GeoKeys.RectifiedGridAngle: // Note: GDAL seems to use angular unit here.
            case GeoKeys.AzimuthAngle:       return AZIMUTH;
            default: {
                if (key >= GeoKeys.StdParallel1 && key <= GeoKeys.LAST_MAP_PROJECTION_PARAMETER) {
                    return ANGULAR;
                } else {
                    return NULL;
                }
            }
        }
    }

    /**
     * Verifies that the given unit is compatible with the quantity expected by this key.
     * This method may propose an alternative key for the given unit.
     *
     * @param  unit  unit of measurement of an axis of a geodetic CRS.
     * @return the key to use for the specified unit, or {@code null} if none.
     */
    public UnitKey validate(final Unit<?> unit) {
        if ((linear  && Units.isLinear (unit)) ||
            (angular && Units.isAngular(unit)) ||
            (scalar  && Units.isScale  (unit)))
        {
            return this;
        }
        switch (this) {
            case LINEAR:  if (Units.isAngular(unit)) return ANGULAR; else break;
            case ANGULAR: if (Units.isLinear (unit)) return LINEAR;  else break;
        }
        return null;
    }

    /**
     * Returns the default unit of measurement, or {@code null} if none.
     *
     * @return default unit of measurement (if any) for this key.
     */
    public Unit<?> defaultUnit() {
        if (linear)  return Units.METRE;
        if (angular) return Units.DEGREE;
        if (scalar)  return Units.UNITY;
        return null;
    }
}
