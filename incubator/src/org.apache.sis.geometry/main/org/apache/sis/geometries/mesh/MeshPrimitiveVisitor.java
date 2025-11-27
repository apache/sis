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
package org.apache.sis.geometries.mesh;

import java.util.HashSet;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.LineString;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.Triangle;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.Cursor;
import org.apache.sis.geometries.math.Array;
import org.apache.sis.util.ArgumentChecks;


/**
 * Loop on all geometry primitive and vertex informations.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class MeshPrimitiveVisitor {

    private final HashSet visited = new HashSet();
    private MultiMeshPrimitive<?> geometry;
    private MeshPrimitive primitive;


    public MeshPrimitiveVisitor() {}

    /**
     *
     * @param geometry to visit, not null.
     */
    public MeshPrimitiveVisitor(Geometry geometry) {
        if (geometry instanceof MeshPrimitive p) {
            reset(new MultiMeshPrimitive(p));
        } else if (geometry instanceof MultiMeshPrimitive mp) {
            reset(mp);
        } else {
            throw new IllegalArgumentException("Geometry is not a primitive");
        }
    }

    /**
     * Change visited geometry and reset visit states.
     *
     * @param geometry not null
     */
    public void reset(MultiMeshPrimitive geometry) {
        ArgumentChecks.ensureNonNull("geometry", geometry);
        this.geometry = geometry;
        visited.clear();
    }

    /**
     * Loop on all primitives in the geometry.
     */
    public void visit() {
        for (MeshPrimitive p : geometry.getComponents()) {
            visit(p);
        }
    }

    protected void visit(MeshPrimitive primitive) {
        visited.clear();
        this.primitive = primitive;

        Array index = primitive.getIndex();
        if (index == null) {
            //create a virtual index with all points
            int[] indices = new int[Math.toIntExact(primitive.getPositions().getLength())];
            for (int i = 0; i < indices.length; i++) indices[i] = i;
            index = NDArrays.ofUnsigned(1, indices);
        }
        if (index.isEmpty()) return;

        final Cursor cursor = index.cursor();

        int idx0;
        int idx1;
        int idx2;
        long cnt = index.getLength();
        MeshPrimitive.Type mode = primitive.getType();
        long offset = 0;
        long i;
        long n;

        switch (mode) {
            case POINTS:
                for (i = 0; i < cnt; i++) {
                    cursor.moveTo(i + offset + 0);
                    idx0 = (int) cursor.samples().get(0);
                    visit(readPoint(idx0));
                    visited.add(idx0);
                }   break;
            case LINES:
                for (i = 0; i < cnt; i+=2) {
                    cursor.moveTo(i + offset + 0);
                    idx0 = (int) cursor.samples().get(0);
                    cursor.moveTo(i + offset + 1);
                    idx1 = (int) cursor.samples().get(0);
                    visit(readLine(idx0, idx1));
                    visited.add(idx0);
                    visited.add(idx1);
                }   break;
            case LINE_STRIP:
                for (i = 1; i < cnt; i++) {
                    cursor.moveTo(i-1);
                    idx0 = (int) cursor.samples().get(0);
                    cursor.moveTo(i);
                    idx1 = (int) cursor.samples().get(0);
                    visit(readLine(idx0, idx1));
                    visited.add(idx0);
                    visited.add(idx1);
                }   break;
            case TRIANGLES:
                for (n = offset + cnt; offset < n;) {
                    cursor.moveTo(offset);
                    idx0 = (int) cursor.samples().get(0);
                    offset++;
                    cursor.moveTo(offset);
                    idx1 = (int) cursor.samples().get(0);
                    offset++;
                    cursor.moveTo(offset);
                    idx2 = (int) cursor.samples().get(0);
                    offset++;
                    visit(readTriangle(idx0, idx1, idx2));
                    visited.add(idx0);
                    visited.add(idx1);
                    visited.add(idx2);
                }   break;
            case TRIANGLE_FAN:
                cursor.moveTo(offset+0);
                idx0 = (int) cursor.samples().get(0);
                for (i = 1, n = cnt-1; i < n; i++) {
                    cursor.moveTo(offset + i);
                    idx1 = (int) cursor.samples().get(0);
                    cursor.moveTo(offset + i + 1);
                    idx2 = (int) cursor.samples().get(0);
                    visit(readTriangle(idx0, idx1, idx2));
                    visited.add(idx0);
                    visited.add(idx1);
                    visited.add(idx2);
                }   break;
            case TRIANGLE_STRIP:
                cursor.moveTo(0);
                idx0 = (int) cursor.samples().get(0);
                cursor.moveTo(1);
                idx1 = (int) cursor.samples().get(0);
                for (i = 2, n = cnt; i < n; i++) {
                    cursor.moveTo(i);
                    idx2 = (int) cursor.samples().get(0);
                    //in triangle strip we must take care to reverse index
                    //at every new point, otherwise we would have reverse winding
                    //for each triangle
                    Triangle t;
                    if (i % 2 == 0) {
                        t = readTriangle(idx0, idx1, idx2);
                    } else {
                        t = readTriangle(idx1, idx0, idx2);
                    }
                    visit(t);
                    visited.add(idx0);
                    visited.add(idx1);
                    visited.add(idx2);
                    idx0 = idx1;
                    idx1 = idx2;
                }   break;
            default:
                throw new UnsupportedOperationException("TODO");
        }

    }

    private Point readPoint(int idx0) {
        return new MeshPrimitive.Vertex(primitive, idx0);
    }

    private Triangle readTriangle(int idx0, int idx1, int idx2) {
        return GeometryFactory.createTriangle(GeometryFactory.createLinearRing(new MeshPrimitive.Sequence(primitive, new int[]{idx0, idx1, idx2, idx0})));
    }

    private LineString readLine(int idx0, int idx1) {
        return GeometryFactory.createLineString(new MeshPrimitive.Sequence(primitive, new int[]{idx0, idx1}));
    }

    /**
     * Override this method to process a triangle.
     */
    protected void visit(Triangle candidate) {
        final PointSequence points = candidate.getExteriorRing().getPoints();
        visit((MeshPrimitive.Vertex) points.getPoint(0));
        visit((MeshPrimitive.Vertex) points.getPoint(1));
        visit((MeshPrimitive.Vertex) points.getPoint(2));
    }

    /**
     * Override this method to process a line.
     */
    protected void visit(LineString candidate) {
        final PointSequence points = candidate.getPoints();
        visit((MeshPrimitive.Vertex) points.getPoint(0));
        visit((MeshPrimitive.Vertex) points.getPoint(1));
    }

    /**
     * Override this method to process a point.
     */
    protected void visit(Point candidate) {
        visit((MeshPrimitive.Vertex) candidate);
    }

    /**
     * Called when visiting a mesh vertex.
     * @param vertex visited vertex.
     */
    protected abstract void visit(MeshPrimitive.Vertex vertex);

    public boolean isVisited(MeshPrimitive.Vertex vertex) {
        return visited.contains(vertex.getIndex());
    }

}
