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
package org.apache.sis.geometries.polyhedron;

import java.util.List;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.LinearRing;
import org.apache.sis.geometries.MultiPolygon;
import org.apache.sis.geometries.Polygon;
import org.apache.sis.geometries.Polyhedron;
import org.apache.sis.geometries.Sphere;
import org.apache.sis.geometries.internal.shared.AbstractGeometry;
import org.apache.sis.geometries.math.Array;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.ReadOnly;
import org.apache.sis.geometries.math.Vector3D;
import org.apache.sis.geometries.spherical.SphericalConvexPolygon;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class AbstractPolyhedron extends AbstractGeometry implements Polyhedron{

    /**
     * Golden ratio, used to build the latitude constants of the icosahedral-
     * symmetry solids (Icosahedron, Dodecahedron, RhombicTriacontahedron,
     * TruncatedIcosahedron).
     */
    protected static final double PHI = (1 + Math.sqrt(5)) / 2;

    /**
     * Latitude of a cube corner (atan(1/sqrt(2)), equivalently asin(1/sqrt(3))),
     * used by Tetrahedron, Hexahedron and Dodecahedron.
     */
    protected static final double CUBE_LAT = Math.atan(1 / Math.sqrt(2));

    /**
     * atan(phi), used by Icosahedron and RhombicTriacontahedron.
     */
    protected static final double ATAN_PHI = Math.atan(PHI);

    /**
     * atan(1/phi) = PI/2 - atan(phi), used by Icosahedron and RhombicTriacontahedron.
     */
    protected static final double ATAN_INV_PHI = Math.PI/2 - ATAN_PHI;

    /**
     * atan(phi^2), used by Dodecahedron and RhombicTriacontahedron.
     */
    protected static final double ATAN_PHI2 = Math.atan(PHI*PHI);

    /**
     * atan(1/phi^2) = PI/2 - atan(phi^2), used by Dodecahedron and RhombicTriacontahedron.
     */
    protected static final double ATAN_INV_PHI2 = Math.PI/2 - ATAN_PHI2;

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Envelope getEnvelope() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public MultiPolygon getExteriorShell() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public List<MultiPolygon> getInteriorShells() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    /**
     * @return number of faces on the polyhedron
     */
    public abstract int getFaceCount();

    /**
     * @return face polygon
     */
    public abstract Polygon getFace(int index);

    /**
     * Get the face index intersecting the given unit vector.
     *
     * @param unitVector a unit vector from the center of the polyhedron
     * @return the face index intersecting the vector
     */
    public abstract int getFace(ReadOnly.Vector<?> unitVector);


    /**
     * Build a unit direction vector from a latitude/longitude, in radians.
     *
     * @param latRad latitude in radians, in range [-PI/2 .. PI/2]
     * @param lonRad longitude in radians, in range [-PI .. PI]
     * @return unit direction vector
     */
    protected static ReadOnly.Vector<?> fromLatLon(double latRad, double lonRad) {
        return new Vector3D.Double().setFromLatLon(latRad, lonRad);
    }

    /**
     * Build a direction vector from a latitude/longitude, in radians, scaled
     * by the given radius.
     *
     * @param latRad latitude in radians, in range [-PI/2 .. PI/2]
     * @param lonRad longitude in radians, in range [-PI .. PI]
     * @param radius scale applied to the unit direction vector
     * @return direction vector
     */
    protected static ReadOnly.Vector<?> fromLatLon(double latRad, double lonRad, double radius) {
        return new Vector3D.Double().setFromLatLon(latRad, lonRad).scale(radius);
    }

    /**
     * Find which face's solid-angle cone (as seen from the polyhedron center)
     * contains the given unit vector.
     * <p>
     * Faces must be convex, with vertices in CCW order viewed from outside
     * the polyhedron. For each face, the vector lies in its cone if it is on
     * the inner side of every edge plane, following the same triple-product
     * test as {@link org.apache.sis.geometries.spherical.SphericalTriangle#contains }
     * generalized to polygons with any number of vertices. This holds for any
     * convex polyhedron centered on the origin : each ray from the center
     * crosses exactly one face.
     *
     * @param vertices polyhedron vertices
     * @param faces polyhedron faces, as CCW vertex indices into {@code vertices}
     * @param unitVector vector to test
     * @return matching face index, or -1 if none matched
     */
    protected static int nearestFace(Array vertices, int[][] faces, ReadOnly.Vector<?> unitVector) {
        final Sphere sphere = new Sphere(3);
        for (int f = 0; f < faces.length; f++) {
            final Array polyArray = NDArrays.subset(vertices, faces[f]);
            final SphericalConvexPolygon face = new SphericalConvexPolygon(sphere, polyArray);
            if (face.contains(unitVector)) {
                return f;
            }
        }
        return -1;
    }

    /**
     * Build the polygon of one face.
     *
     * @param vertices polyhedron vertices
     * @param face face vertex indices into {@code vertices}, in CCW order viewed from outside
     * @return face polygon
     */
    protected static Polygon toPolygon(Array vertices, int[] face) {
        final Array positions = NDArrays.subset(vertices, face);
        final LinearRing ring = GeometryFactory.createLinearRing(GeometryFactory.createSequence(positions));
        return GeometryFactory.createPolygon(ring, null);
    }

}
