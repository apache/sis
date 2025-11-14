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

import java.util.Iterator;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.LineString;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.internal.shared.AbstractGeometry;
import org.apache.sis.geometries.mesh.MeshPrimitive.Vertex;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.math.Maths;
import org.apache.sis.geometries.math.Tuple;


/**
 * A oriented edge is a straight line between from a start point to an end point.
 * The opposite oriented edge shares the same start and end points.
 *
 * @author Johann Sorel (Geomatys)
 */
final class OrientedEdge extends AbstractGeometry implements LineString, PointSequence {

    /**
     * Structure is shared by the two opposite directional edges.
     */
    private final Structure structure;

    /**
     * Triangle on the left side of the edge.
     * Edges form a counter clockwise triangle.
     */
    private OrientedTriangle triangle;

    /**
     * Reverse directional edge.
     */
    private OrientedEdge reverse;

    private boolean reversed = false;

    /**
     * Set to true if this edge is a constraint.
     * If true this edge should not be removed in the TIN creation process.
     * Still this edge can be split, each resulting edge will inherit the constraint.
     */
    private boolean constraint = false;

    /**
     * @param start starting vertex
     * @param end ending vertex
     */
    public OrientedEdge(MeshPrimitive.Vertex start, MeshPrimitive.Vertex end) {
        this.structure = new Structure(start, end);
    }

    /**
     * Start and end vertices are part of given triangle, created edge is in
     * counter-clockwise direction in the triangle.
     * This constructor makes no verificiations.
     *
     * @param start starting vertex
     * @param end ending vertex
     * @param triangle parent triangle
     */
    public OrientedEdge(MeshPrimitive.Vertex start, MeshPrimitive.Vertex end, OrientedTriangle triangle) {
        this.structure = new Structure(start, end);
        this.triangle = triangle;
    }

    private OrientedEdge(OrientedEdge reverse) {
        this.structure = reverse.structure;
        this.reverse = reverse;
        this.reversed = true;
    }

    public OrientedTriangle getTriangle() {
        return triangle;
    }

    public Vertex getStart() {
        return getPoint(0);
    }

    public Vertex getEnd() {
        return getPoint(1);
    }

    public OrientedEdge reverse() {
        return reverse;
    }

    public OrientedEdge reverse(boolean create) {
        if (create) {
            if (reverse != null) {
                throw new RuntimeException("Opposite edge already exist");
            }
            this.reverse = new OrientedEdge(this);
        }
        return reverse;
    }

    void makeObsolete() {
        this.structure.obsolete = true;
    }

    boolean isObsolete() {
        return structure.obsolete;
    }

    /**
     * @return true if point is on the edge
     */
    public boolean isOnEdge(MeshPrimitive.Vertex pt) {
        assert (!structure.obsolete);
        return Maths.isOnLine(structure.start.getPosition(), structure.end.getPosition(), pt.getPosition());
    }

    /**
     * @return greater than 0 if point is on the left side
     *          equal 0 if point is on the line
     *          inferior than 0 if point is on the right side
     */
    public double getSide(MeshPrimitive.Vertex pt) {
        return Maths.lineSide(
                getStart().getPosition(),
                getEnd().getPosition(),
                pt.getPosition());
    }

    /**
     * Set to true if this edge is a constraint.
     * If true this edge should not be removed in the TIN creation process.
     * Still this edge can be split, each resulting edge will inherit the constraint.
     *
     * @param constraint true for a constraint edge
     */
    public void setConstraint(boolean constraint) {
        this.constraint = constraint;
    }

    /**
     * @return true if edge is a constraint
     */
    public boolean isConstraint() {
        return constraint;
    }

    void changeTriangle(OrientedTriangle after) {
        assert (!structure.obsolete);
        this.triangle = after;
    }

    /**
     * @return true if one of the two edge vertices is the same as given vertex.
     */
    public boolean hasPoint(MeshPrimitive.Vertex pt) {
        assert (!structure.obsolete);
        return structure.start.getIndex() == pt.getIndex() || structure.end.getIndex() == pt.getIndex();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public PointSequence getPoints() {
        return this;
    }

    /**
     * Get an iterator which rotates around the edge starting point.
     * Returned edges starts by this edge followed by edges in counter-clockwise
     * direction until a loop is done. If not a loop, edges in clockwise direction
     * are iterated afterward.
     * All edges start on this edge starting vertex.
     */
    public Iterator<OrientedEdge> rotatingIterator() {
        return new RotatingIterator();
    }

    ////////////////////////////////////////////////////////////////////////////
    // PointSequence ///////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return structure.start.getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public int getDimension() {
        return structure.start.getCoordinateReferenceSystem().getCoordinateSystem().getDimension();
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
        return 2;
    }

    @Override
    public Vertex getPoint(int index) {
        switch (index) {
            case 0 : return reversed ? structure.end : structure.start;
            case 1 : return reversed ? structure.start : structure.end;
            default : throw new ArrayIndexOutOfBoundsException();
        }
    }

    @Override
    public Tuple getPosition(int index) {
        switch (index) {
            case 0 : return (reversed ? structure.end : structure.start).getPosition().copy();
            case 1 : return (reversed ? structure.start : structure.end).getPosition().copy();
            default : throw new ArrayIndexOutOfBoundsException();
        }
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
        long result = 31 + structure.start.getIndex();
        result = 31 * result + structure.end.getIndex();
        return (int) result;
    }

    private static class Structure {

        private boolean obsolete = false;
        final MeshPrimitive.Vertex start;
        final MeshPrimitive.Vertex end;

        public Structure(MeshPrimitive.Vertex start, MeshPrimitive.Vertex end) {
            this.start = start;
            this.end = end;
        }
    }

    private class RotatingIterator implements Iterator<OrientedEdge> {

        private OrientedEdge search = OrientedEdge.this;
        private OrientedEdge candidate = search;
        private boolean goingCounterClockwise = true;

        @Override
        public boolean hasNext() {
            findNext();
            return candidate != null;
        }

        private void findNext() {
            if (candidate != null) return;
            if (search == null) return;

            //search the triangles turning clockwise until we make a loop
            if (goingCounterClockwise) {
                final OrientedTriangle t = search.getTriangle();
                //get next triangle edge
                search = t.adjacentEdge(search).reverse();
                candidate = search;
                if (search == OrientedEdge.this) {
                    //loop finished
                    search = null;
                    candidate = null;
                    return;
                }
            }

            if (goingCounterClockwise && search == null) {
                goingCounterClockwise = false;
                search = OrientedEdge.this;
            }

            //search the triangles turning counter-clockwise
            //this case can happen only when we didn't make a full loop previously
            //it can happen for the area border triangles.
            if (!goingCounterClockwise) {
                search = search.reverse();
                if (search != null) {
                    final OrientedTriangle t = search.getTriangle();
                    search = t.getEdge(search.getEnd());
                }
            }

            candidate = search;
        }

        @Override
        public OrientedEdge next() {
            findNext();
            OrientedEdge edge = candidate;
            candidate = null;
            return edge;
        }

    }
}
