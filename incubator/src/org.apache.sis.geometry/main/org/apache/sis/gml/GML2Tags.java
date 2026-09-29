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


/**
 * GML 2.0 XML tags.
 *
 * @author  Johann Sorel (Geomatys)
 */
final class GML2Tags {
    /**
     * GML 2.0 XML namespace.
     */
    public static final String NAMESPACE_2 = "http://www.opengis.net/gml";

    // geometry types
    public static final String POINT               = "Point";
    public static final String LINE_STRING         = "LineString";
    public static final String LINEAR_RING         = "LinearRing";
    public static final String POLYGON             = "Polygon";
    public static final String BOX                 = "Box";
    public static final String MULTI_POINT         = "MultiPoint";
    public static final String MULTI_LINE_STRING   = "MultiLineString";
    public static final String MULTI_POLYGON       = "MultiPolygon";
    public static final String MULTI_GEOMETRY      = "MultiGeometry";

    // boundary used in GML 2, replaced by exterior and interior in GML 3
    public static final String OUTER_BOUNDARY_IS = "outerBoundaryIs";
    public static final String INNER_BOUNDARY_IS = "innerBoundaryIs";

    // collection members
    public static final String POINT_MEMBER       = "pointMember";
    public static final String LINE_STRING_MEMBER = "lineStringMember";
    public static final String POLYGON_MEMBER     = "polygonMember";
    public static final String GEOMETRY_MEMBER    = "geometryMember";

    // coordinates in GML 2
    public static final String COORD       = "coord";
    public static final String COORDINATES = "coordinates";
    public static final String X = "X";
    public static final String Y = "Y";
    public static final String Z = "Z";

    // geometry attribute
    public static final String SRS_NAME = "srsName";

    // coordinates attributes
    public static final String DECIMAL = "decimal";
    public static final String CS      = "cs";
    public static final String TS      = "ts";

    public static final char DEFAULT_DECIMAL = '.';
    public static final char DEFAULT_CS      = ',';
    public static final char DEFAULT_TS      = ' ';

    private GML2Tags() {
    }
}
