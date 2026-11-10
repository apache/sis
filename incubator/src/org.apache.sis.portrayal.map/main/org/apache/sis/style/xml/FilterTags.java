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
package org.apache.sis.style.xml;

import org.apache.sis.xml.Namespaces;


/**
 * XML tags of OGC Filter Encoding 1.0 and 1.1, in the {@value #NAMESPACE} namespace.
 * Also contains the GML and XLink names used in filters and styles.
 *
 * @author  Johann Sorel (Geomatys)
 */
final class FilterTags {
    /**
     * Namespace of Filter Encoding 1.0 and 1.1.
     */
    static final String NAMESPACE = "http://www.opengis.net/ogc";

    /**
     * Usual prefix for the {@link #NAMESPACE}.
     */
    static final String PREFIX = "ogc";

    /**
     * Unversioned GML namespace, used by GML 2, 3.0 and 3.1.
     */
    static final String GML_NAMESPACE = "http://www.opengis.net/gml";

    /**
     * Namespace of XLink, used for online resources.
     */
    static final String XLINK_NAMESPACE = Namespaces.XLINK;

    /**
     * Usual prefix for the {@link #XLINK_NAMESPACE}.
     */
    static final String XLINK_PREFIX = "xlink";

    // XLink attributes
    static final String HREF = "href";
    static final String TYPE = "type";
    static final String SIMPLE = "simple";

    // Root element
    static final String FILTER = "Filter";

    // Comparison operators
    static final String PROPERTY_IS_EQUAL_TO                 = "PropertyIsEqualTo";
    static final String PROPERTY_IS_NOT_EQUAL_TO             = "PropertyIsNotEqualTo";
    static final String PROPERTY_IS_LESS_THAN                = "PropertyIsLessThan";
    static final String PROPERTY_IS_GREATER_THAN             = "PropertyIsGreaterThan";
    static final String PROPERTY_IS_LESS_THAN_OR_EQUAL_TO    = "PropertyIsLessThanOrEqualTo";
    static final String PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO = "PropertyIsGreaterThanOrEqualTo";
    static final String PROPERTY_IS_LIKE                     = "PropertyIsLike";
    static final String PROPERTY_IS_NULL                     = "PropertyIsNull";
    static final String PROPERTY_IS_BETWEEN                  = "PropertyIsBetween";
    static final String LOWER_BOUNDARY                       = "LowerBoundary";
    static final String UPPER_BOUNDARY                       = "UpperBoundary";

    // Attributes of comparison operators
    static final String MATCH_CASE  = "matchCase";
    static final String WILD_CARD   = "wildCard";
    static final String SINGLE_CHAR = "singleChar";
    static final String ESCAPE      = "escape";         // Filter Encoding 1.0
    static final String ESCAPE_CHAR = "escapeChar";     // Filter Encoding 1.1

    // Logical operators
    static final String AND = "And";
    static final String OR  = "Or";
    static final String NOT = "Not";

    // Spatial operators
    static final String BBOX       = "BBOX";
    static final String EQUALS     = "Equals";
    static final String DISJOINT   = "Disjoint";
    static final String TOUCHES    = "Touches";
    static final String WITHIN     = "Within";
    static final String OVERLAPS   = "Overlaps";
    static final String CROSSES    = "Crosses";
    static final String INTERSECTS = "Intersects";
    static final String CONTAINS   = "Contains";
    static final String DWITHIN    = "DWithin";
    static final String BEYOND     = "Beyond";
    static final String DISTANCE   = "Distance";
    static final String UNITS      = "units";

    // Identifiers
    static final String FEATURE_ID    = "FeatureId";
    static final String GML_OBJECT_ID = "GmlObjectId";
    static final String FID           = "fid";
    static final String ID            = "id";

    // Expressions
    static final String PROPERTY_NAME = "PropertyName";
    static final String LITERAL       = "Literal";
    static final String FUNCTION      = "Function";
    static final String NAME          = "name";
    static final String ADD           = "Add";
    static final String SUB           = "Sub";
    static final String MUL           = "Mul";
    static final String DIV           = "Div";

    /**
     * Do not allow instantiation of this class.
     */
    private FilterTags() {
    }
}
