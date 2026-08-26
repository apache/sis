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
package org.apache.sis.geometries.operation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.measure.quantity.Length;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.LineString;
import org.apache.sis.geometries.LinearRing;
import org.apache.sis.geometries.MultiLineString;
import org.apache.sis.geometries.MultiPoint;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.Polygon;
import org.apache.sis.geometries.Triangle;
import org.apache.sis.geometries.math.Array;
import org.apache.sis.geometries.math.DataType;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.mesh.MeshPrimitiveVisitor;
import org.apache.sis.geometries.mesh.MultiMeshPrimitive;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 * Geometry operation computation processor.
 *
 * NOTE/TODO :
 * There is the case of 2D versus 3D operations in ISO-19107.
 * ISO separate operations in 2d and 3d, and when dealing with 3D geometries with a 2D operation
 * then the geometry must be projected.
 * Currently the code stays in the dimension the geometry is in.
 * We will wait and see how this goes and maybe separate the two dimension or provide 2d wrapper if needed.
 *
 * @see OGC Simple Feature Access 1.2.1 - 6.1.2.4 Methods that support spatial analysis
 * @see OGC Simple Feature Access 1.2.1 - 6.1.2.3 Methods for testing spatial relations between geometric objects
 * @see ISO_19107 Query2D section 6.4.8
 * @see ISO_19107 Query3D section 6.4.9
 * @author Johann Sorel (Geomatys)
 */
public final class GeometryProcessor {

    public GeometryProcessor(){}

    /**
     * Returns a geometric object that represents all Points whose distance from this geometric object is less than
     * or equal to distance. Calculations are in the spatial reference system of this geometric object. Because of the
     * limitations of linear interpolation, there will often be some relatively small error in this distance,
     * but it should be near the resolution of the coordinates used.
     */
    public Geometry buffer(Geometry geom, double distance) throws OperationException {

        //TODO : fallback on JTS until implemented, this loss the attributes !
        return Geometries.fromJTS(jts(geom).buffer(distance), true);
    }

    @UML(identifier="buffer", specification=ISO_19107) // section 6.4.4.24 and 6.4.8.3
    //@UML(identifier="3Dbuffer", specification=ISO_19107) // section 6.4.9
    public Geometry buffer(Geometry geom, Length radius) throws OperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a geometric object that represents the convex hull of this geometric object.
     * Convex hulls, being dependent on straight lines, can be accurately represented in linear interpolations
     * for any geometry restricted to linear interpolations.
     */
    //@UML(identifier="3DconvexHull", specification=ISO_19107) // section 6.4.9
    public Geometry convexHull(Geometry geom) throws OperationException {

        //TODO : fallback on JTS until implemented, this loss the attributes !
        return Geometries.fromJTS(jts(geom).convexHull(), true);
    }

    /**
     * Returns a geometric object that represents the Point set difference of this geometric object with anotherGeometry.
     */
    @UML(identifier="difference", specification=ISO_19107) // section 6.4.4.30 and 6.4.8.5
    //@UML(identifier="3Ddifference", specification=ISO_19107) // section 6.4.9
    public Geometry difference(Geometry geom1, Geometry geom2) throws OperationException {

        //TODO : fallback on JTS until implemented, this loss the attributes !
        return Geometries.fromJTS(jts(geom1).difference(jts(geom2)), true);
    }

    /**
     * Returns the shortest distance between any two Points in the two geometric objects as calculated in the
     * spatial reference system of this geometric object.
     * Because the geometries are closed, it is possible to find a point on each geometric object involved, such that
     * the distance between these 2 points is the returned distance between their geometric objects.
     */
    public double distance(Geometry geom1, Geometry geom2) throws OperationException {
        if (geom1 instanceof Point pt1) {
            if (geom2 instanceof Point pt2) {
                return Distance.distance(pt1, pt2);
            }
        }

        throw new UnsupportedOperationException();
    }

    @UML(identifier="distance", specification=ISO_19107) // section 6.4.4.26 and 6.4.8.2
    //@UML(identifier="3Ddistance", specification=ISO_19107) // section 6.4.9
    public Length distance2(Geometry geom1, Geometry geom2) throws OperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a geometric object that represents the Point set intersection of this geometric object with anotherGeometry.
     */
    @UML(identifier="intersection", specification=ISO_19107) // section 6.4.4.30 and 6.4.8.4
    //@UML(identifier="3Dintersection", specification=ISO_19107) // section 6.4.9
    public Geometry intersection(Geometry geom1, Geometry geom2) throws OperationException {

        if (geom1 instanceof MeshPrimitive.Triangles g1) {
            if (geom2 instanceof MeshPrimitive.Points g2) {
                return Intersection.intersection(g1, g2);
            } else if (geom2 instanceof MeshPrimitive.Lines g2) {
                return Intersection.intersection(g1, g2);
            }
        }

        //TODO : fallback on JTS until implemented, this loss the attributes !
        return Geometries.fromJTS(jts(geom1).intersection(jts(geom2)), true);
    }

    /**
     * Returns a geometric object that represents the Point set symmetric difference of this geometric
     * object with anotherGeometry.
     */
    @UML(identifier="symDifference", specification=ISO_19107) // section 6.4.4.30 and 6.4.8.6
    //@UML(identifier="3DsymDifference", specification=ISO_19107) // section 6.4.9
    public Geometry symDifference(Geometry geom1, Geometry geom2) throws OperationException {

        //TODO : fallback on JTS until implemented, this loss the attributes !
        return Geometries.fromJTS(jts(geom1).symDifference(jts(geom2)), true);
    }

    /**
     * Returns a geometric object that represents the Point set union of this geometric object with anotherGeometry.
     */
    @UML(identifier="union", specification=ISO_19107) // section 6.4.4.30 and 6.4.8.7
    //@UML(identifier="3Dunion", specification=ISO_19107) // section 6.4.9
    public Geometry union(Geometry geom1, Geometry geom2) throws OperationException {

        //TODO : fallback on JTS until implemented, this loss the attributes !
        return Geometries.fromJTS(jts(geom1).union(jts(geom2)), true);
    }

    @UML(identifier="contains", specification=ISO_19107) // section 6.4.4.30 ?
    //@UML(identifier="3Dcontains", specification=ISO_19107) // section 6.4.9
    public boolean contains(Geometry geom1, DirectPosition element) throws OperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns TRUE if this geometric object “spatially contains” anotherGeometry.
     */
    @UML(identifier="contains", specification=ISO_19107) // section 6.4.8.8, 6.4.4.2
    public boolean contains(Geometry geom1, Geometry geom2) throws OperationException {
        if (geom1 instanceof Polygon polygon) {
            if (geom2 instanceof Point pt) {
                return Contains.contains(polygon, pt);
            }
        }

        //TODO : fallback on JTS until implemented
        return jts(geom1).contains(jts(geom2));
    }

    /**
     * Returns TRUE if this geometric object “spatially crosses” anotherGeometry.
     */
    @UML(identifier="crosses", specification=ISO_19107) // section 6.4.8.8
    //@UML(identifier="3Dcrosses", specification=ISO_19107) // section 6.4.9
    public boolean crosses(Geometry geom1, Geometry geom2) throws OperationException {
        //TODO : fallback on JTS until implemented
        return jts(geom1).crosses(jts(geom2));
    }

    /**
     * Returns TRUE if this geometric object “spatially disjoint” anotherGeometry.
     */
    @UML(identifier="disjoint", specification=ISO_19107) // section 6.4.8.8
    //@UML(identifier="3Ddisjoint", specification=ISO_19107) // section 6.4.9
    public boolean disjoint(Geometry geom1, Geometry geom2) throws OperationException {
        //TODO : fallback on JTS until implemented
        return jts(geom1).disjoint(jts(geom2));
    }

    /**
     * Returns TRUE if this geometric object “spatially equal” anotherGeometry.
     */
    @UML(identifier="equals", specification=ISO_19107) // section 6.4.8.8, 6.4.4.30
    //@UML(identifier="3Dequals", specification=ISO_19107) // section 6.4.9
    public boolean equal(Geometry geom1, Geometry geom2) throws OperationException {
        //TODO : fallback on JTS until implemented
        return jts(geom1).equals(jts(geom2));
    }

    /**
     * Returns TRUE if this geometric object “spatially intersects” anotherGeometry.
     */
    @UML(identifier="intersects", specification=ISO_19107) // section 6.4.8.8, 6.4.4.30
    //@UML(identifier="3Dintersects", specification=ISO_19107) // section 6.4.9
    public boolean intersects(Geometry geom1, Geometry geom2) throws OperationException {
        //TODO : fallback on JTS until implemented
        return jts(geom1).intersects(jts(geom2));
    }

    /**
     * Returns a derived geometry collection value that matches the specified m coordinate value.
     * See Subclause 6.1.2.6 “Measures on Geometry” for more details.
     *
     * TODO : M has been replaced by attributes, change this method
     */
    public Geometry locateAlong(Geometry geom1, double mValue) throws OperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a derived geometry collection value that matches the specified range of m coordinate values inclusively.
     * See Subclause 6.1.2.6 “Measures on Geometry” for more details.
     *
     * TODO : M has been replaced by attributes, change this method
     */
    public Geometry locateBetween(Geometry geom1, double mStart, double mEnd) throws OperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns TRUE if this geometric object “spatially overlaps” anotherGeometry.
     */
    @UML(identifier="overlaps", specification=ISO_19107) // section 6.4.8.8
    //@UML(identifier="3Doverlaps", specification=ISO_19107) // section 6.4.9
    public boolean overlaps(Geometry geom1, Geometry geom2) throws OperationException {
        //TODO : fallback on JTS until implemented
        return jts(geom1).overlaps(jts(geom2));
    }

    /**
     * Returns TRUE if this geometric object is spatially related to anotherGeometry by testing for intersections between
     * the interior, boundary and exterior of the two geometric objects as specified by the values in the
     * intersectionPatternMatrix.
     * This returns FALSE if all the tested intersections are empty except exterior (this) intersect exterior (another).
     *
     * @todo merge with ISO Relate beneath
     */
    public boolean relate(Geometry geom1, Geometry geom2, int matrix) throws OperationException {
        throw new UnsupportedOperationException();
    }

    @UML(identifier="relate", specification=ISO_19107) // section 6.4.8.8
    //@UML(identifier="3Drelate", specification=ISO_19107) // section 6.4.9
    public boolean relate(Geometry geom1, Geometry geom2, String matrix) throws OperationException {
        //TODO : fallback on JTS until implemented
        return jts(geom1).relate(jts(geom2), matrix);
    }

    /**
     * Returns TRUE if this geometric object “spatially touches” anotherGeometry.
     */
    @UML(identifier="touches", specification=ISO_19107) // section 6.4.8.8
    //@UML(identifier="3Dtouches", specification=ISO_19107) // section 6.4.9
    public boolean touches(Geometry geom1, Geometry geom2) throws OperationException {
        //TODO : fallback on JTS until implemented
        return jts(geom1).touches(jts(geom2));
    }

    /**
     * Returns TRUE if this geometric object “spatially within” anotherGeometry.
     */
    @UML(identifier="within", specification=ISO_19107) // section 6.4.8.8
    //@UML(identifier="3Dwithin", specification=ISO_19107) // section 6.4.9
    public boolean within(Geometry geom1, Geometry geom2) throws OperationException {

        //TODO : fallback on JTS until implemented
        return jts(geom1).within(jts(geom2));
    }

    @UML(identifier="withinDistance", specification=ISO_19107) // section 6.4.8.8
    //@UML(identifier="3DwithinDistance", specification=ISO_19107) // section 6.4.9
    public boolean withinDistance(Geometry geom1, Geometry geom2, Length distance) throws OperationException {
        throw new UnsupportedOperationException();
    }

    // ////////////////////////////////////////////////////////////////////////
    // additional operations
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Add a Z axis on the geometry and configure it's ordinates.
     *
     * @param geom geometry to transform, if it already has a 3D crs and given crs is null, geometry crs will be preserved.
     * @param crs3d the result crs in 3d, if null an ellipsoid height is assumed
     * @param zeditor called to configure the Z value on each position, if null, value 0.0 will be used
     */
    public Geometry to3D(Geometry geom, CoordinateReferenceSystem crs3d, Consumer<Tuple> zeditor) {
        if (geom instanceof Point base) {
            return To3D.to3D(base, crs3d, zeditor);
        } else if (geom instanceof LineString base) {
            return To3D.to3D(base, crs3d, zeditor);
        } else if (geom instanceof MeshPrimitive base) {
            return To3D.to3D(base, crs3d, zeditor);
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Create a new attribute or update an existing one.
     *
     * @param geom geometry to modify
     * @param attributeName new attribute name
     * @param attributeSystem new attribute system
     * @param attributeType new attribute type
     * @param valueGenerator function to generate attribute value
     * @return new or modified geometry
     */
    public Geometry compute(Geometry geom, String attributeName, SampleSystem attributeSystem, DataType attributeType, Function<Point,Tuple> valueGenerator) {
        if (geom instanceof MeshPrimitive mp) {
            return ComputeAttribute.compute(mp, attributeName, attributeSystem, attributeType, valueGenerator);
        } else if (geom instanceof MultiMeshPrimitive<?> mp) {
            return ComputeAttribute.compute(mp, attributeName, attributeSystem, attributeType, valueGenerator);
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Convert this geometry to a Primitive geometry type.
     * This method is provided as a conversion to GPU geometric model.
     *
     * @return equivalent primitive, all attributes are copied.
     *  can be a Primitive or MultiPrimitive
     */
    public Geometry toPrimitive(Geometry geom) {
        if (geom instanceof MeshPrimitive || geom instanceof MultiMeshPrimitive) {
            //Does nothing, geometry is already a Primitive.
            return geom;
        } else if (geom instanceof Point cdt) {
            return ToPrimitive.toPrimitive(cdt);
        } else if (geom instanceof LineString cdt) {
            return ToPrimitive.toPrimitive(cdt);
        } else if (geom instanceof Polygon cdt) {
            return ToPrimitive.toPrimitive(cdt);
        } else if (geom instanceof MultiLineString cdt) {
            return ToPrimitive.toPrimitive(cdt);
        } else if (geom instanceof MultiPoint<?> cdt) {
            return ToPrimitive.toPrimitive(cdt);
        } else if (geom instanceof GeometryCollection<?> cdt) {
            return ToPrimitive.toPrimitive(cdt);
        }

        throw new UnsupportedOperationException();
    }

    /**
     * Returns a geometric object that represents a transformed version of the geometry.
     *
     * @param geom geometry to transform
     * @param crs target CRS, if null geometry crs is unchanged but transform will still be applied
     * @param transform transform to apply, if null, geometry crs to target crs will be used
     * @return geometry of same type when possible
     */
    @UML(identifier="transform", specification=ISO_19107) // section 6.4.4.28
    public Geometry transform(Geometry geom, CoordinateReferenceSystem crs, MathTransform transform) {
        if (crs == null) {
            crs = geom.getCoordinateReferenceSystem();
        }

        if (geom instanceof LinearRing cdt) {
            return Transform.transform(cdt, crs, transform);
        } else if (geom instanceof Triangle cdt) {
            return Transform.transform(cdt, crs, transform);
        } else if (geom instanceof Polygon cdt) {
            return Transform.transform(cdt, crs, transform);
        } else if (geom instanceof MultiMeshPrimitive<?> cdt) {
            return Transform.transform(cdt, crs, transform);
        } else if (geom instanceof MeshPrimitive cdt) {
            return Transform.transform(cdt, crs, transform);
        }

        throw new UnsupportedOperationException();
    }

    /**
     * Separate the points/lines/triangles in the given primitive.
     * This ensure each point is used only once.
     *
     * @return equivalent primitive, all attributes are copied.
     *  can be a Primitive or MultiPrimitive
     */
    public Geometry separateFaces(MeshPrimitive p) {

        final AttributesType attributesType = p.getAttributesType();
        final Map<String,List<Tuple<?>>> atts = new HashMap<>();

        for (String name : attributesType.getAttributeNames()) {
            atts.put(name, new ArrayList<>());
        }

        MeshPrimitiveVisitor pv = new MeshPrimitiveVisitor(p) {
            @Override
            protected void visit(Point candidate) {
                for (Entry<String,List<Tuple<?>>> entry : atts.entrySet()) {
                    entry.getValue().add(candidate.getAttribute(entry.getKey()));
                }
            }

            @Override
            protected void visit(LineString candidate) {
                final Point p0 = candidate.getPointN(0);
                final Point p1 = candidate.getPointN(1);
                for (Entry<String,List<Tuple<?>>> entry : atts.entrySet()) {
                    entry.getValue().add(p0.getAttribute(entry.getKey()));
                    entry.getValue().add(p1.getAttribute(entry.getKey()));
                }
            }

            @Override
            protected void visit(Triangle candidate) {
                final LinearRing ring = candidate.getExteriorRing();
                final Point p0 = ring.getPointN(0);
                final Point p1 = ring.getPointN(1);
                final Point p2 = ring.getPointN(2);
                for (Entry<String,List<Tuple<?>>> entry : atts.entrySet()) {
                    entry.getValue().add(p0.getAttribute(entry.getKey()));
                    entry.getValue().add(p1.getAttribute(entry.getKey()));
                    entry.getValue().add(p2.getAttribute(entry.getKey()));
                }
            }

            @Override
            protected void visit(MeshPrimitive.Vertex vertex) {}
        };
        pv.visit();

        //do not create an index, result elements
        final MeshPrimitive.Type type;
        switch (p.getType()) {
            case POINTS : type = MeshPrimitive.Type.POINTS; break;
            case LINES :
            case LINE_LOOP :
            case LINE_STRIP : type = MeshPrimitive.Type.LINES; break;
            case TRIANGLES :
            case TRIANGLE_FAN :
            case TRIANGLE_STRIP :
            default : type = MeshPrimitive.Type.TRIANGLES; break;
        }
        final MeshPrimitive sep = MeshPrimitive.create(type);
        for (Entry<String,List<Tuple<?>>> entry : atts.entrySet()) {
            final String name = entry.getKey();
            final Array array = NDArrays.of(entry.getValue(), attributesType.getAttributeSystem(name), attributesType.getAttributeType(name));
            sep.setAttribute(name, array);
        }
        return sep;
    }

    /**
     * TODO fallback on JTS until we implemetend all methods.
     */
    private static org.locationtech.jts.geom.Geometry jts(Geometry geom) {
        return Geometries.asJTS(geom, false, null);
    }
}
