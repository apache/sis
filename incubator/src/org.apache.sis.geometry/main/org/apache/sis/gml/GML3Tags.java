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
package org.apache.sis.gml;

import org.apache.sis.xml.Namespaces;


/**
 * GML 3 XML tags, does not duplicated tags from GML 2.
 *
 * @author  Johann Sorel (Geomatys)
 */
final class GML3Tags {
    /**
     * GML 3.2 XML namespace. GML 3.0 and 3.1 documents typically use the unversioned
     * {@link GML2Tags#NAMESPACE} instead;
     */
    public static final String NAMESPACE_3 = Namespaces.GML;

    // coordinates in GML 3
    public static final String POS           = "pos";
    public static final String POS_LIST      = "posList";
    public static final String SRS_DIMENSION = "srsDimension";

    // envelope, which replaces the GML 2.0 Box
    public static final String ENVELOPE     = "Envelope";
    public static final String LOWER_CORNER = "lowerCorner";
    public static final String UPPER_CORNER = "upperCorner";

    // boundary used in GML 3
    public static final String EXTERIOR = "exterior";
    public static final String INTERIOR = "interior";

    // geometry types
    public static final String CURVE              = "Curve";
    public static final String SURFACE            = "Surface";
    public static final String RING               = "Ring";
    public static final String COMPOSITE_CURVE    = "CompositeCurve";
    public static final String COMPOSITE_SURFACE  = "CompositeSurface";
    public static final String ORIENTABLE_CURVE   = "OrientableCurve";
    public static final String ORIENTABLE_SURFACE = "OrientableSurface";
    public static final String SOLID              = "Solid";
    public static final String COMPOSITE_SOLID    = "CompositeSolid";
    public static final String MULTI_SOLID        = "MultiSolid";
    public static final String MULTI_CURVE        = "MultiCurve";
    public static final String MULTI_SURFACE      = "MultiSurface";
    public static final String TRIANGULATED_SURFACE = "TriangulatedSurface";
    public static final String TIN                = "Tin";

    // curve decomposition
    public static final String SEGMENTS            = "segments";
    public static final String LINE_STRING_SEGMENT = "LineStringSegment";
    public static final String ARC                 = "Arc";
    public static final String ARC_STRING          = "ArcString";
    public static final String CIRCLE              = "Circle";
    public static final String GEODESIC_STRING     = "GeodesicString";
    public static final String GEODESIC            = "Geodesic";
    public static final String BASE_CURVE          = "baseCurve";

    // surface decomposition
    public static final String PATCHES       = "patches";
    public static final String POLYGON_PATCH = "PolygonPatch";
    public static final String TRIANGLE      = "Triangle";
    public static final String RECTANGLE     = "Rectangle";
    public static final String BASE_SURFACE  = "baseSurface";

    // solid decomposition
    public static final String SHELL         = "Shell";
    public static final String SOLID_MEMBER  = "solidMember";
    public static final String SOLID_MEMBERS = "solidMembers";

    // orientation of an OrientableCurve or OrientableSurface
    public static final String ORIENTATION          = "orientation";
    public static final String ORIENTATION_REVERSED = "-";

    public static final String CIRCLE_BY_CENTER_POINT = "CircleByCenterPoint";
    public static final String ARC_BY_CENTER_POINT    = "ArcByCenterPoint";
    public static final String ARC_BY_BULGE           = "ArcByBulge";

    // parameters of the ArcByCenterPoint and ArcByBulge curve segments
    public static final String POINT_PROPERTY = "pointProperty";
    public static final String RADIUS         = "radius";
    public static final String START_ANGLE    = "startAngle";
    public static final String END_ANGLE      = "endAngle";
    public static final String BULGE          = "bulge";
    public static final String NORMAL         = "normal";

    /**
     * Attribute naming the unit of measurement of a {@code gml:radius}, {@code gml:startAngle}
     * or {@code gml:endAngle} value.
     */
    public static final String UOM = "uom";

    /**
     * Value of the {@code uom} attribute written for the angles of a {@code gml:ArcByCenterPoint}.
     * Those are always kept in decimal degrees by
     * {@link org.apache.sis.geometries.conics.ArcByCenterPoint}.
     */
    public static final String UOM_DEGREE = "deg";

    public static final String CUBIC_SPLINE           = "CubicSpline";
    public static final String BSPLINE                = "BSpline";
    public static final String BEZIER                 = "Bezier";
    public static final String CLOTHOID               = "Clothoid";
    public static final String OFFSET_CURVE           = "OffsetCurve";

    // collection members
    public static final String CURVE_MEMBER     = "curveMember";
    public static final String CURVE_MEMBERS    = "curveMembers";
    public static final String SURFACE_MEMBER   = "surfaceMember";
    public static final String SURFACE_MEMBERS  = "surfaceMembers";
    public static final String POINT_MEMBERS    = "pointMembers";
    public static final String GEOMETRY_MEMBERS = "geometryMembers";

    private GML3Tags() {
    }
}
