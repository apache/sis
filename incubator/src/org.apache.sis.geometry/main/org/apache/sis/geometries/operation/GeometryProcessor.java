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

import javax.measure.quantity.Length;
import org.apache.sis.geometries.Geometry;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.DirectPosition;

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
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a geometric object that represents the Point set difference of this geometric object with anotherGeometry.
     */
    @UML(identifier="difference", specification=ISO_19107) // section 6.4.4.30 and 6.4.8.5
    //@UML(identifier="3Ddifference", specification=ISO_19107) // section 6.4.9
    public Geometry difference(Geometry geom1, Geometry geom2) throws OperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the shortest distance between any two Points in the two geometric objects as calculated in the
     * spatial reference system of this geometric object.
     * Because the geometries are closed, it is possible to find a point on each geometric object involved, such that
     * the distance between these 2 points is the returned distance between their geometric objects.
     */
    public double distance(Geometry geom1, Geometry geom2) throws OperationException {
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
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a geometric object that represents the Point set symmetric difference of this geometric
     * object with anotherGeometry.
     */
    @UML(identifier="symDifference", specification=ISO_19107) // section 6.4.4.30 and 6.4.8.6
    //@UML(identifier="3DsymDifference", specification=ISO_19107) // section 6.4.9
    public Geometry symDifference(Geometry geom1, Geometry geom2) throws OperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a geometric object that represents the Point set union of this geometric object with anotherGeometry.
     */
    @UML(identifier="union", specification=ISO_19107) // section 6.4.4.30 and 6.4.8.7
    //@UML(identifier="3Dunion", specification=ISO_19107) // section 6.4.9
    public Geometry union(Geometry geom1, Geometry geom2) throws OperationException {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    /**
     * Returns TRUE if this geometric object “spatially crosses” anotherGeometry.
     */
    @UML(identifier="crosses", specification=ISO_19107) // section 6.4.8.8
    //@UML(identifier="3Dcrosses", specification=ISO_19107) // section 6.4.9
    public boolean crosses(Geometry geom1, Geometry geom2) throws OperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns TRUE if this geometric object “spatially disjoint” anotherGeometry.
     */
    @UML(identifier="disjoint", specification=ISO_19107) // section 6.4.8.8
    //@UML(identifier="3Ddisjoint", specification=ISO_19107) // section 6.4.9
    public boolean disjoint(Geometry geom1, Geometry geom2) throws OperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns TRUE if this geometric object “spatially equal” anotherGeometry.
     */
    @UML(identifier="equals", specification=ISO_19107) // section 6.4.8.8, 6.4.4.30
    //@UML(identifier="3Dequals", specification=ISO_19107) // section 6.4.9
    public boolean equal(Geometry geom1, Geometry geom2) throws OperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns TRUE if this geometric object “spatially intersects” anotherGeometry.
     */
    @UML(identifier="intersects", specification=ISO_19107) // section 6.4.8.8, 6.4.4.30
    //@UML(identifier="3Dintersects", specification=ISO_19107) // section 6.4.9
    public boolean intersects(Geometry geom1, Geometry geom2) throws OperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a derived geometry collection value that matches the specified m coordinate value.
     * See Subclause 6.1.2.6 “Measures on Geometry” for more details.
     */
    public Geometry locateAlong(Geometry geom1, double mValue) throws OperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a derived geometry collection value that matches the specified range of m coordinate values inclusively.
     * See Subclause 6.1.2.6 “Measures on Geometry” for more details.
     */
    public Geometry contains(Geometry geom1, double mStart, double mEnd) throws OperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns TRUE if this geometric object “spatially overlaps” anotherGeometry.
     */
    @UML(identifier="overlaps", specification=ISO_19107) // section 6.4.8.8
    //@UML(identifier="3Doverlaps", specification=ISO_19107) // section 6.4.9
    public boolean overlaps(Geometry geom1, Geometry geom2) throws OperationException {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    /**
     * Returns TRUE if this geometric object “spatially touches” anotherGeometry.
     */
    @UML(identifier="touches", specification=ISO_19107) // section 6.4.8.8
    //@UML(identifier="3Dtouches", specification=ISO_19107) // section 6.4.9
    public boolean touches(Geometry geom1, Geometry geom2) throws OperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns TRUE if this geometric object “spatially within” anotherGeometry.
     */
    @UML(identifier="within", specification=ISO_19107) // section 6.4.8.8
    //@UML(identifier="3Dwithin", specification=ISO_19107) // section 6.4.9
    public boolean within(Geometry geom1, Geometry geom2) throws OperationException {
        throw new UnsupportedOperationException();
    }

    @UML(identifier="withinDistance", specification=ISO_19107) // section 6.4.8.8
    //@UML(identifier="3DwithinDistance", specification=ISO_19107) // section 6.4.9
    public boolean withinDistance(Geometry geom1, Geometry geom2, Length distance) throws OperationException {
        throw new UnsupportedOperationException();
    }

}
