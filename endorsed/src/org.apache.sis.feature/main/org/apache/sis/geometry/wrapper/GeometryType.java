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
package org.apache.sis.geometry.wrapper;

import java.util.Locale;
import java.util.EnumMap;
import org.opengis.util.TypeName;
import org.opengis.util.NameSpace;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.util.internal.shared.Constants;


/**
 * Implementation-neutral description of the type of geometry.
 * The name of each enumeration value is the name in WKT format.
 * The ordinal value is the binary code, ignoring thousands.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see Geometries#getGeometryClass(GeometryType)
 */
public enum GeometryType {
    /**
     * Base class of all geometries, with the possible exception of point in some implementation.
     *
     * @see Geometries#rootClass
     */
    GEOMETRY("Geometry", false),

    /**
     * Zero-dimensional geometry containing a single point.
     * Note that this is not necessarily a subtype of {@link #GEOMETRY}.
     * The notable exception is Java2D.
     */
    POINT("Point", false),

    /**
     * Sequence of points connected by straight, non-self intersecting line pieces.
     * This is a one-dimensional geometry.
     */
    LINESTRING("LineString", false),

    /**
     * Geometry with a positive area (two-dimensional).
     * The sequence of points form a closed, non-self intersecting ring.
     */
    POLYGON("Polygon", false),

    /**
     * Set of points.
     */
    MULTIPOINT("MultiPoint", true),

    /**
     * Set of line strings.
     */
    MULTILINESTRING("MultiLineString", true),

    /**
     * Set of polygons.
     */
    MULTIPOLYGON("MultiPolygon", true),

    /**
     * Set of geometries of any type except other geometry collection.
     */
    GEOMETRYCOLLECTION("GeometryCollection", true),

    /**
     * Curve with circular interpolation between points.
     */
    CIRCULARSTRING("CircularString", false),

    /**
     * Contiguous curves such that adjacent curves are joined at their end points.
     */
    COMPOUNDCURVE("CompoundCurve", true),

    /**
     * Planar surface consisting of a single patch.
     */
    CURVEPOLYGON("CurvePolygon", false),

    /**
     * Geometry collection made of curves.
     */
    MULTICURVE("MultiCurve", true),

    /**
     * Geometry collection made of surfaces.
     */
    MULTISURFACE("MultiSurface", true),

    /**
     * 1-dimensional geometry usually stored as a sequence of points.
     * The subtype specifies the form of the interpolation between points.
     */
    CURVE("Curve", false),

    /**
     * 2-dimensional geometry associated with one exterior ring and zero or more interior rings.
     */
    SURFACE("Surface", false),

    /**
     * Surface composed of contiguous surfaces connected along their common boundary.
     */
    POLYHEDRALSURFACE("PolyhedralSurface", true),

    /**
     * Polyhedral surface composed only of triangles.
     */
    TIN("Tin", true),

    /**
     * Polygon with exactly four points (the last point being the same as the first point).
     */
    TRIANGLE("Triangle", false),

    /**
     * A circular curve in which the last point is the same as the first point.
     */
    CIRCLE("Circle", false),

    /**
     * Curve for the shortest distance on a sphere or ellipsoid.
     */
    GEODESICSTRING("GeodesicString", false),

    /**
     * Elliptical curve.
     */
    ELLIPTICALCURVE("EllipticalCurve", false),

    /**
     * Nurbs curve.
     */
    NURBSCURVE("NurbsCurve", false),

    /**
     * Clothoid.
     */
    CLOTHOID("Clothoid", false),

    /**
     * Curve describing a spiral.
     */
    SPIRALCURVE("SpiralCurve", false),

    /**
     * Surface made of other surfaces.
     */
    COMPOUNDSURFACE("CompoundSurface", true),

    /**
     * Brep solid.
     */
    BREPSOLID("BrepSolid", false);

    /*
     * Set the `related` fields after initialization of all enumeration values.
     * It needs to be done that way because there is some forward references.
     */
    static {
        MULTIPOINT        .contains(POINT);
        MULTILINESTRING   .contains(LINESTRING);
        MULTIPOLYGON      .contains(POLYGON);
        GEOMETRYCOLLECTION.contains(GEOMETRY);
        COMPOUNDCURVE     .contains(CURVE);
        MULTICURVE        .contains(CURVE);     // Must be the last `CURVE` collection.
        POLYHEDRALSURFACE .contains(SURFACE);
        COMPOUNDSURFACE   .contains(SURFACE);
        MULTISURFACE      .contains(SURFACE);   // Must be the last `SURFACE` collection.
        TIN               .contains(TRIANGLE);
    }

    /**
     * Declares that this enumeration value identifies a collection containing the components of the given class.
     * Invocation order matter: the last call for a given {@code component} will determine the collection class
     * associated to that component.
     */
    private void contains(final GeometryType component) {
        related = component;
        component.related = this;
    }

    /**
     * All enumeration values, fetched at construction time for avoiding array copies.
     */
    private static final GeometryType[] VALUES = values();

    /**
     * Camel-case name of this geometry type.
     * The upper-case variant of this name is equal to {@link #name()}.
     */
    public final String name;

    /**
     * Whether this geometry type is some sort of collection.
     * Some of those types are {@link #MULTIPOINT}, {@link #MULTILINESTRING},
     * {@link #MULTIPOLYGON} or {@link #GEOMETRYCOLLECTION}.
     */
    public final boolean isCollection;

    /**
     * The component type or collection type related to this type.
     * More specifically:
     *
     * <ul>
     *   <li>If {@link #isCollection} is {@code false}, then this is the collection type.</li>
     *   <li>If {@link #isCollection} is {@code true}, then this is the component type.</li>
     * </ul>
     *
     * This field is shall be considered final after class initialization.
     * This field may be null if there is no related type.
     *
     * @see #component()
     * @see #collection()
     */
    private GeometryType related;

    /**
     * The geometry types as ISO 19103 type names, created when first needed.
     * For a given enumeration value, all {@code typeNames} values are identical
     * except for the associated Java class, which depends on the geometry library.
     *
     * @see #getTypeName(Geometries)
     */
    private final EnumMap<GeometryLibrary, TypeName> typeNames;

    /**
     * The "OGC" namespace for geometry names. Fetched when first needed.
     */
    private static volatile NameSpace namespace;

    /**
     * Creates a new enumeration value.
     *
     * @param  name  camel-case name of the geometry.
     * @param  isCollection  whether this geometry type is some sort of collection.
     */
    private GeometryType(final String name, final boolean isCollection) {
        this.name = name;
        this.isCollection = isCollection;
        typeNames = new EnumMap<>(GeometryLibrary.class);
    }

    /**
     * {@return the name of this geometry type as an ISO 19103 object}.
     * The namespace is "OGC". The Java type depends on the geometry library.
     *
     * @param  library  the geometry library that determine geometry classes.
     */
    public final TypeName getTypeName(final Geometries<?> library) {
        TypeName value;
        synchronized (typeNames) {
            value = typeNames.get(library.library);
        }
        if (value == null) {
            NameSpace scope = namespace;
            if (scope == null) {
                /*
                 * The `Names.createTypeName(…)` method creates a `TypeName` associated to the
                 * `org.opengis.geometry.Geometry` type, which is not necessarily what we want.
                 * So we keep only the namespace.
                 */
                namespace = scope = Names.createTypeName(Constants.OGC, null, "Geometry").scope();
            }
            value = DefaultNameFactory.provider().createTypeName(scope, name, library.getGeometryClass(this));
            synchronized (typeNames) {
                final TypeName existing = typeNames.put(library.library, value);
                if (existing != null) {
                    typeNames.put(library.library, value = existing);
                }
            }
        }
        return value;
    }

    /**
     * Returns the type of geometry collection if this type is for a component.
     * If this enumeration value is not a component type, returns {@code null}.
     */
    public final GeometryType collection() {
        return isCollection ? null : related;
    }

    /**
     * Returns the type of geometry components if this type is some kind of collection.
     * Example of collection types are {@link #MULTIPOINT}, {@link #MULTILINESTRING},
     * {@link #MULTIPOLYGON} or {@link #GEOMETRYCOLLECTION}.
     * If this enumeration value is not a collection type, returns {@code null}.
     */
    public final GeometryType component() {
        return isCollection ? related : null;
    }

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
     * Returns whether the given <abbr>WKB</abbr> type has a <var>z</var> coordinates.
     *
     * @param  type  the <abbr>WKB</abbr> type to test.
     * @return whether the given type has a <var>z</var> coordinates.
     */
    public static boolean hasZ(final int type) {
        return (type < 0) || (type >= 1000 && type < 2000) || (type >= 3000 && type < 4000);
    }

    /**
     * Returns whether the given <abbr>WKB</abbr> type has a <var>m</var> coordinates.
     *
     * @param  type  the <abbr>WKB</abbr> type to test.
     * @return whether the given type has a <var>m</var> coordinates.
     */
    public static boolean hasM(int type) {
        type &= Integer.MAX_VALUE;      // Clear a 2.5D bit mask which was used in some software.
        return (type >= 2000 && type < 4000);
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
        type &= Integer.MAX_VALUE;      // Clear a 2.5D bit mask which was used in some software.
        if (type < 4000) {
            type %= 1000;
            if (type < VALUES.length) {
                return VALUES[type];
            }
        }
        return null;
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
                name = name.substring(0, length).replace("_", "");
                if (name.equals("GEOMCOLLECTION")) {    // Alternative name also accepted.
                    return GEOMETRYCOLLECTION;
                }
                return valueOf(name);
            }
        }
        return null;
    }

    /**
     * Returns the enumeration value for the given ISO type.
     *
     * @param  type  the ISO 19115 geometry type, or {@code null}.
     * @return the geometry type from this enumeration, or {@code null} if none.
     */
    public static GeometryType forISO(final org.opengis.metadata.acquisition.GeometryType type) {
        if (org.opengis.metadata.acquisition.GeometryType.POINT.equals(type)) {
            return POINT;
        } else if (org.opengis.metadata.acquisition.GeometryType.LINEAR.equals(type)) {
            return LINESTRING;
        } else if (org.opengis.metadata.acquisition.GeometryType.AREAL.equals(type)) {
            return POLYGON;
        } else {
            return null;
        }
    }

    /**
     * Returns {@code true} if the given name is one of the enumerated geometry types, ignoring case.
     *
     * @param  name  the name to test.
     * @return whether the given name is one of the enumerated geometry types, ignoring case.
     */
    public static boolean isKnown(final String name) {
        for (GeometryType value : VALUES) {
            if (value.name().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}
