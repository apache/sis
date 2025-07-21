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
package org.apache.sis.geometries.triangulate.delaunay;

import java.awt.geom.Point2D;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.LinearRing;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.Triangle;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.privy.AbstractGeometry;
import org.apache.sis.geometries.mesh.MeshPrimitive.Vertex;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.referencing.privy.ShapeUtilities;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
final class OrientedTriangle extends AbstractGeometry implements Triangle, PointSequence {

    private final LinearRing ring = GeometryFactory.createLinearRing(this);

    private boolean obsolete = false;
    public final OrientedEdge ab;
    public final OrientedEdge bc;
    public final OrientedEdge ca;
    /**
     * triangle points in counter clockwise order
     */
    public final Vertex a;
    public final Vertex b;
    public final Vertex c;
    //cache delaunay triangle informations
    private double circleCenterX = Double.POSITIVE_INFINITY;
    private double circleCenterY;
    private double circleRadius2;

    /**
     * Create triangle with vertices in counter clockwise order.
     * This constructor makes no verificiations.
     *
     * @param a first vertex, not null
     * @param b second vertex, not null
     * @param c thord vertex, not null
     */
    public OrientedTriangle(Vertex a, Vertex b, Vertex c) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.ab = new OrientedEdge(a, b, this);
        this.bc = new OrientedEdge(b, c, this);
        this.ca = new OrientedEdge(c, a, this);
    }

    /**
     * Create triangle with edges in counter clockwise order.
     * This constructor makes no verificiations.
     *
     * @param ab first edge, not null
     * @param bc second edge, not null
     * @param ca thord edge, not null
     */
    public OrientedTriangle(OrientedEdge ab, OrientedEdge bc, OrientedEdge ca) {
        this.ab = ab;
        this.bc = bc;
        this.ca = ca;
        this.ab.changeTriangle(this);
        this.bc.changeTriangle(this);
        this.ca.changeTriangle(this);
        this.a = ab.getStart();
        this.b = bc.getStart();
        this.c = ca.getStart();
    }

    public boolean isObsolete() {
        return obsolete;
    }

    public void obsolete(){
        this.obsolete = true;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public LinearRing getExteriorRing() {
        return ring;
    }

    public OrientedEdge getEdge(Vertex start) {
        assert (!obsolete);
        if (a == start) {
            return ab;
        } else if (b == start) {
            return bc;
        } else  if (c == start) {
            return ca;
        } else {
            throw new OperationException("Vertex is not from this triangle");
        }
    }

    /**
     * Returns the opposite vertex of given edge.
     */
    MeshPrimitive.Vertex oppositePoint(OrientedEdge edge) {
        assert (!obsolete);
        if (edge == ab) {
            return c;
        } else if (edge == bc) {
            return a;
        } else if (edge == ca) {
            return b;
        } else {
            throw new OperationException("Edge is not from this triangle");
        }
    }

    /**
     * Returns the adjacent edge, sharing the starting vertex.
     */
    OrientedEdge adjacentEdge(OrientedEdge edge) {
        assert (!obsolete);
        if (edge == ab) {
            return ca;
        } else if (edge == bc) {
            return ab;
        } else if (edge == ca) {
            return bc;
        } else {
            throw new OperationException("Edge is not from this triangle");
        }
    }

    public boolean validate() throws OperationException {
        assert (!obsolete);

        if (ab.getStart() == ab.getEnd() || bc.getStart() == bc.getEnd() || ca.getStart() == ca.getEnd()) throw new OperationException("Unvalid edges",null);

        if (ab.getStart() != a) throw new OperationException("Unvalid edge " + ab);
        if (ab.getEnd()   != b) throw new OperationException("Unvalid edge " + ab);

        if (bc.getStart() != b) throw new OperationException("Unvalid edge " + bc);
        if (bc.getEnd()   != c) throw new OperationException("Unvalid edge " + bc);

        if (ca.getStart() != c) throw new OperationException("Unvalid edge " + ca);
        if (ca.getEnd()   != a) throw new OperationException("Unvalid edge " + ca);

        if (ab.getTriangle() != this) throw new OperationException("Unvalid edge " + ab);
        if (bc.getTriangle() != this) throw new OperationException("Unvalid edge " + bc);
        if (ca.getTriangle() != this) throw new OperationException("Unvalid edge " + ca);

        if (ab.isObsolete()) throw new OperationException("Obsolete edge " + ab);
        if (bc.isObsolete()) throw new OperationException("Obsolete edge " + bc);
        if (ca.isObsolete()) throw new OperationException("Obsolete edge " + ca);
        return true;
    }

    public boolean isInDelaunayCircle(double x, double y) {
        return distanceToDelaunayCircleCenterSquare(x,y) < circleRadius2;
    }

    public double distanceToDelaunayCircleCenterSquare(double x, double y) {
        getDelaunayCircleRadiusSquare();
        final double rx = x - circleCenterX;
        final double ry = y - circleCenterY;
        return rx*rx + ry*ry;
    }

    public double getDelaunayCircleRadiusSquare() {
        if (circleCenterX == Double.POSITIVE_INFINITY) {
            //sort vertex starting by lowest index, this ensure more consistancy in the tests
            Vertex A = a;
            Vertex B = b;
            Vertex C = c;
            if (A.getIndex() > B.getIndex()) {
                Vertex T = A;
                A = B;
                B = C;
                C = T;
            }
            if (A.getIndex() > B.getIndex()) {
                Vertex T = A;
                A = B;
                B = C;
                C = T;
            }
            final Tuple posA = A.getPosition();
            final Tuple posB = B.getPosition();
            final Tuple posC = C.getPosition();
            final double ax = posA.get(0);
            final double ay = posA.get(1);
            final Point2D.Double circleCentre = ShapeUtilities.circleCentre(ax, ay, posB.get(0), posB.get(1), posC.get(0), posC.get(1));
            final double rx = ax - circleCentre.x;
            final double ry = ay - circleCentre.y;
            this.circleCenterX = circleCentre.x;
            this.circleCenterY = circleCentre.y;
            this.circleRadius2 = rx*rx + ry*ry;
        }
        return circleRadius2;
    }

    ////////////////////////////////////////////////////////////////////////////
    // PointSequence ///////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return a.getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public int getDimension() {
        return a.getCoordinateReferenceSystem().getCoordinateSystem().getDimension();
    }

    /**
     * Get the geometry bounding envelope.
     *
     * @return Envelope in geometry coordinate reference system.
     */
    @Override
    public Envelope getEnvelope() {
        return PointSequence.super.getEnvelope();
    }

    @Override
    public AttributesType getAttributesType() {
        return AttributesType.EMPTY;
    }

    @Override
    public int size() {
        return 4;
    }

    @Override
    public Point getPoint(final int index) {
        return switch (index) {
            case 0, 3 -> a;
            case 1 -> b;
            case 2 -> c;
            default -> throw new ArrayIndexOutOfBoundsException();
        };
    }

    @Override
    public Tuple getPosition(int index) {
        return switch (index) {
            case 0, 3 -> a.getPosition().copy();
            case 1 -> b.getPosition().copy();
            case 2 -> c.getPosition().copy();
            default -> throw new ArrayIndexOutOfBoundsException();
        };
    }

    @Override
    public void setPosition(int index, Tuple value) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Tuple getAttribute(int index, String name) {
        if (AttributesType.ATT_POSITION.equals(name)) {
            return getPosition(index);
        }
        return null;
    }

    @Override
    public void setAttribute(int index, String name, Tuple value) {
        if (AttributesType.ATT_POSITION.equals(name)) {
            setPosition(index, value);
            return;
        }
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public int hashCode() {
        int result = 31 + a.getIndex();
        result = 31 * result + b.getIndex();
        result = 31 * result + c.getIndex();
        return result;
    }

}
