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
package org.apache.sis.internal.feature;

import java.util.Locale;


/**
 * Implementation-neutral description of the type of geometry.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see Geometries#getGeometryClass(GeometryType)
 *
 * @since 1.1
 * @module
 */
public enum GeometryType {
    /**
     * Base class of all geometries, with the possible exception of point in some implementation.
     *
     * @see Geometries#rootClass
     */
    GEOMETRY,

    /**
     * Zero-dimensional geometry containing a single point.
     * Note that this is not necessarily a subtype of {@link #GEOMETRY}.
     * The notable exception is Java2D.
     *
     * @see Geometries#pointClass
     */
    POINT,

    /**
     * Sequence of points connected by straight, non-self intersecting line pieces.
     * This is a one-dimensional geometry.
     *
     * @see Geometries#polylineClass
     */
    LINESTRING,

    /**
     * Geometry with a positive area (two-dimensional).
     * The sequence of points form a closed, non-self intersecting ring.
     *
     * @see Geometries#polygonClass
     */
    POLYGON,

    /**
     * Set of points.
     */
    MULTI_POINT,

    /**
     * Set of linestrings.
     */
    MULTI_LINESTRING,

    /**
     * Set of polygons.
     */
    MULTI_POLYGON,

    /**
     * Set of geometries of any type except other geometry collection.
     */
    GEOMETRY_COLLECTION;

    /**
     * The type of this geometry as specified in Well-Known Binary (WKB) specification.
     * This is also the integer value declared in the {@code "GEOMETRY_TYPE"} column of
     * the {@code "GEOMETRY_COLUMNS} table of a spatial database.
     *
     * <p>The WKB specification defines values in the [0 … 15] range for 2D geometries
     * and adds 1000 for geometries having <var>Z</var> values.
     * Then 2000 is added again for geometries having <var>M</var> values.</p>
     *
     * @return the geometry type specified in WKB specification.
     *
     * @see #forBinaryType(int)
     */
    public final int binaryType() {
        return ordinal();
    }

    /**
     * Returns {@code true} if this geometry type is some sort of collection.
     * Those types are {@link #MULTI_POINT}, {@link #MULTI_LINESTRING},
     * {@link #MULTI_POLYGON} or {@link #GEOMETRY_COLLECTION}.
     *
     * @return whether this geometry type is some kind of collections.
     */
    public final boolean isCollection() {
        return ordinal() >= MULTI_POINT.ordinal();
    }

    /**
     * Returns the enumeration value for the given name.
     * This method is case-insensitive.
     *
     * @param  name  the geometry type name, or {@code null}.
     * @return enumeration value for the given name, or {@code null} if the name was null.
     * @throws IllegalArgumentException if the name is not recognized.
     */
    public static GeometryType forName(String name) {
        if (name != null) {
            name = name.trim().toUpperCase(Locale.US);
            int length = name.length();
            if (length > 0) {
                // Remove Z, M or ZM suffix.
                if (/*non-empty*/ name.charAt(length - 1) == 'M') length--;
                if (length > 0 && name.charAt(length - 1) == 'Z') length--;
                name = name.substring(0, length);
                switch (name) {
                    case "MULTIPOINT":      return MULTI_POINT;
                    case "MULTILINESTRING": return MULTI_LINESTRING;
                    case "MULTIPOLYGON":    return MULTI_POLYGON;
                    case "GEOMCOLLECTION":  return GEOMETRY_COLLECTION;
                    default: return valueOf(name);
                }
            }
        }
        return null;
    }

    /**
     * Returns the enumeration value for the given WKB type, or {@code null} if unknown.
     * Types for geometries having <var>Z</var> and <var>M</var> are replaced by 2D types.
     *
     * @param  type  WKB geometry type.
     * @return enumeration value for the given type, or {@code null} if the given type is not recognized.
     *
     * @see #binaryType()
     */
    public static GeometryType forBinaryType(int type) {
        if (type >= 1000 && type < 4000) {
            type %= 1000;
        }
        switch (type) {
            default: return null;
            case 0:  return GEOMETRY;
            case 1:  return POINT;
            case 2:  return LINESTRING;
            case 3:  return POLYGON;
            case 4:  return MULTI_POINT;
            case 5:  return MULTI_LINESTRING;
            case 6:  return MULTI_POLYGON;
            case 7:  return GEOMETRY_COLLECTION;
        //  case 13: return CURVE;
        //  case 14: return SURFACE;
        //  case 15: return POLYHEDRALSURFACE;
        }
    }
}
