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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.sis.geometries.math.DataType;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.Vector1D;
import org.apache.sis.geometries.math.Cursor;
import org.apache.sis.geometries.math.Array;


/**
 * Various algorithms to transform primitive indexes.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class MeshPrimitiveIndexes {

    private MeshPrimitiveIndexes(){
        //for futur parameters
    }

    /**
     * Create an opposite winding triangle strip index.
     *
     * Current algorithm build a triangle index which are converted to a strip again.
     * TODO : we should search for a more efficient solution.
     * A solution is to add a degenerated triangle, but this might not create the best solution.
     *
     * @param index triangle strip index
     * @param type index type, only triangle type index supported
     * @return reverse winding triangle index, returned type is the same as input
     */
    public static Array reverseTriangles(Array index, MeshPrimitive.Type type) {
        switch (type) {
            case TRIANGLE_FAN :
                throw new UnsupportedOperationException("Triangle fan type not supported yet");
            case TRIANGLE_STRIP :
                return toTriangleStrip(
                        reverseTriangles(
                                toTrianglesNoPack(index),
                                MeshPrimitive.Type.TRIANGLES),
                        MeshPrimitive.Type.TRIANGLES);
            case TRIANGLES :
                break;
            default :
                throw new IllegalArgumentException("Only triangle index type supported, but was " + type.name());
        }

        int[] array = index.toArrayInt();
        for(int i = 0, t = 0; i < array.length; i+=3) {
            t = array[i+1];
            array[i+1] = array[i+2];
            array[i+2] = t;
        }
        return NDArrays.packIntegerDataType(NDArrays.of(index.getSampleSystem(), array));
    }

    /**
     * Build triangles from primitive triangles.
     * This method generate a new index of type TRIANGLES.
     *
     * @param index triangle strip index.
     * @param type index type, only triangle type index supported
     * @return triangles index, can be the original index if it is already triangle.
     */
    public static Array toTriangles(Array index, MeshPrimitive.Type type) {
        switch (type) {
            case TRIANGLE_FAN :
                throw new UnsupportedOperationException("Triangle fan type not supported yet");
            case TRIANGLE_STRIP :
                //continues below
                break;
            case TRIANGLES :
                //return unchanged
                return index;
            default :
                throw new IllegalArgumentException("Only triangle index type supported, but was " + type.name());
        }
        final Array tidx = toTrianglesNoPack(index);
        return NDArrays.packIntegerDataType(tidx);
    }

    /**
     * Build triangles from primitive triangle strip.
     * This method generate a new index of type TRIANGLES.
     * Empty triangles are removed (two identical index in the same triangle).
     *
     * @param triangleStripIndex triangle strip index.
     * @return triangles index.
     */
    private static Array toTrianglesNoPack(Array triangleStripIndex) {

        final Cursor cursor = triangleStripIndex.cursor();

        final int length = Math.toIntExact(triangleStripIndex.getLength());
        int[] triangles = new int[(length-2)*3];
        cursor.moveTo(0);
        int idx0 = (int) cursor.samples().get(0);
        cursor.moveTo(1);
        int idx1 = (int) cursor.samples().get(0);

        int realsize = 0;
        int maxIdx = Math.max(idx0, idx1);
        for (int i = 2, n = length, k = 0; i < n; i++) {
            cursor.moveTo(i);
            int idx2 = (int) cursor.samples().get(0);
            maxIdx = Math.max(maxIdx, idx2);

            //check for empty triangle and remove them
            if (idx0 == idx1 || idx0 == idx2 || idx1 == idx2) {
                //ignore it, empty
            } else {
                //in triangle strip we must take care to reverse index
                //at every new point, otherwise we would have reverse winding
                //for each triangle winding is reversed
                if (i % 2 == 0) {
                    triangles[k  ] = idx0;
                    triangles[k+1] = idx1;
                    triangles[k+2] = idx2;
                } else {
                    triangles[k  ] = idx0;
                    triangles[k+1] = idx2;
                    triangles[k+2] = idx1;
                }

                k += 3;
                realsize += 3;
            }
            idx0 = idx1;
            idx1 = idx2;
        }

        if (triangles.length != realsize) {
            triangles = Arrays.copyOf(triangles, realsize);
        }

        return NDArrays.of(1, triangles);
    }

    /**
     * Build a triangle strip index from triangles index.
     * This method generate a new index of type TRIANGLE_STRIP.
     *
     * @param index triangles index.
     * @param type index type, only triangle type index supported
     * @return triangle strip index, can be the original index if it is already a strip
     */
    public static Array toTriangleStrip(Array index, MeshPrimitive.Type type) {

        switch (type) {
            case TRIANGLE_FAN :
                throw new UnsupportedOperationException("Triangle fan type not supported yet");
            case TRIANGLE_STRIP :
                //return unchanged
                return index;
            case TRIANGLES :
                //continues below
                break;
            default :
                throw new IllegalArgumentException("Only triangle index type supported, but was " + type.name());
        }

        final Map<Long,Edge> map = new HashMap<>();
        final Set<Edge> edges = new HashSet<>();

        //build edges map
        final Cursor cursor = index.cursor();
        while (cursor.next()) {
            final int i0 = (int) cursor.samples().get(0);
            cursor.next();
            final int i1 = (int) cursor.samples().get(0);
            cursor.next();
            final int i2 = (int) cursor.samples().get(0);
            final Triangle t = new Triangle(i0,i1,i2);
            t.buildEdges(map);
            edges.add(t.edge0);
            edges.add(t.edge1);
            edges.add(t.edge2);
        }

        //pick the first edge which is a mesh border
        //if none left pick the first edge if any remaining
        final List<LinkedList<Vector1D.Int>> strips = new ArrayList<>();
        while (!edges.isEmpty()) {

            //find a good starting edge
            Edge edge = null;
            int priority = -100;
            for (Edge e : edges) {
                final int p = e.priority();
                if (p == 20) {
                    //best case
                    edge = e;
                    break;
                } else if (p > priority) {
                    priority = p;
                    edge = e;
                }
            }

            //build strip starting with this edge
            strips.add(buildStrip(edge, edges));
        }

        //connect each strip with a degenerated triangle
        final List<Vector1D.Int> stripIndex = connect(strips);

        Array idx = NDArrays.of(stripIndex, 1, DataType.UINT);
        return NDArrays.packIntegerDataType(idx);
    }

    /**
     * Connect each strip with a degenerated triangle.
     */
    private static List<Vector1D.Int> connect(List<? extends List<Vector1D.Int>> strips) {
        final List<Vector1D.Int> index = new ArrayList<>();
        for (List<Vector1D.Int> strip : strips) {
            if (!index.isEmpty()) {
                //we must have even size trips otherwise next concatenated strip will have reversed winding
                if (index.size() % 2 != 0) {
                    //repeat the last index to reset winding
                    index.add(new Vector1D.Int(index.get(index.size()-1)));
                }
                //add a degenerated triangle as link between strips
                index.add(new Vector1D.Int(index.get(index.size()-1)));
                index.add(new Vector1D.Int(strip.get(0)));
            }
            index.addAll(strip);
        }
        return index;
    }

    private static LinkedList<Vector1D.Int> buildStrip(final Edge root, Collection<Edge> edges) {

        //Build list of edges we will use in the strip
        final LinkedList<Vector1D.Int> index = new LinkedList<>();

        Triangle triangle;
        {//loop one way
            Edge edge = root;
            triangle = edge.triangle;
            //add the 3 first indices
            index.add(new Vector1D.Int(edge.start));
            index.add(new Vector1D.Int(edge.end));
            index.add(new Vector1D.Int(edge.next.end));

            //add each next triangle new vertex
            edge = edge.next;
            edge = edge.neighbor;

            //remove from list triangle edges
            triangle.unregister(edges);

            for (boolean odd = false; edge != null; odd = !odd) {
                index.add(new Vector1D.Int(edge.next.end));
                edge = odd ? edge.next : edge.next.next;
                triangle = edge.triangle;
                edge = edge.neighbor;
                triangle.unregister(edges);
            }
        }
        {//loop the other way
            //we need at least 2 triangles to preserve winding order
            Edge edge = root;
            boolean odd = true;
            while (edge.neighbor != null && edge.neighbor.opposite(odd) != null) {
                edge = edge.neighbor;
                index.add(new Vector1D.Int(edge.next.end));
                triangle = edge.triangle;
                edge = edge.opposite(odd).neighbor;
                triangle.unregister(edges);
            }
        }
        return index;
    }

    private static long edgeId(int start, int end) {
        return (((long) start) << 32) | ((long) end);
    }

    private static final class Triangle {
        final int i0;
        final int i1;
        final int i2;
        final Edge edge0;
        final Edge edge1;
        final Edge edge2;

        public Triangle(int i0, int i1, int i2) {
            this.i0 = i0;
            this.i1 = i1;
            this.i2 = i2;
            this.edge0 = new Edge(i0, i1, this);
            this.edge1 = new Edge(i1, i2, this);
            this.edge2 = new Edge(i2, i0, this);
            this.edge0.next = this.edge1;
            this.edge1.next = this.edge2;
            this.edge2.next = this.edge0;
        }

        private void buildEdges(Map<Long,Edge> edges) {
            edges.put(edgeId(i0, i1), edge0);
            edges.put(edgeId(i1, i2), edge1);
            edges.put(edgeId(i2, i0), edge2);
            edge0.updateNeighbor(edges.get(edgeId(i1, i0)));
            edge1.updateNeighbor(edges.get(edgeId(i2, i1)));
            edge2.updateNeighbor(edges.get(edgeId(i0, i2)));
        }

        private void unregister(Collection<Edge> edges) {
            edges.remove(edge0);
            edges.remove(edge1);
            edges.remove(edge2);
            edge0.updateNeighbor(null);
            edge1.updateNeighbor(null);
            edge2.updateNeighbor(null);
        }
    }

    private static final class Edge {
        private final int start;
        private final int end;
        private final Triangle triangle;
        private Edge next;
        private Edge neighbor;

        public Edge(int start, int end, Triangle triangle) {
            this.start = start;
            this.end = end;
            this.triangle = triangle;
        }

        /**
         * Get the edge where the strip exist if it started by this edge.
         */
        private Edge opposite(boolean odd) {
            return odd ? next : next.next;
        }

        private void updateNeighbor(Edge edge) {
            if (neighbor != null) {
                //remove revious neighbor relation both ways
                neighbor.neighbor = null;
            }
            neighbor = edge;
            if (neighbor != null) {
                neighbor.neighbor = this;
            }
        }

        private int priority() {
            final boolean border = this.neighbor != null;
            final boolean nextborder = this.next.neighbor != null;
            final boolean previousborder = this.next.next.neighbor != null;

            // NO NEIGHBOR TRIANGLES EDGES /////////////////////////////////////
            if (!border && !nextborder && !previousborder) {
                /*
                single triangle edge, highest priority
                +
                |\
                | \
                +==+
                */
                return 20;
            }
            // SINGLE NEIGHBOR TRIANGLES EDGES /////////////////////////////////
            else if (!border &&  nextborder && !previousborder) {
                /*
                Strip starting edge.
                corner triangle starting edge,
                highest priority
                +--+
                |\ |
                | \|
                +==+
                */
                return 20;
            } else if ( border && !nextborder && !previousborder) {
                /*
                not possible strip starting edge.
                A better edge in the triangle exist !
                never start with such edge.
                +
                |\
                | \
                +==+
                 \ |
                  \|
                   +
                */
                return -10;
            } else if (!border && !nextborder &&  previousborder) {
                /*
                not possible strip starting edge.
                A better edge in the triangle exist !
                never start with such edge.
                +--+
                 \ |\
                  \| \
                   +==+
                */
                return -10;
            }

            // TWO NEIGHBOR TRIANGLES EDGES ////////////////////////////////////
            else if ( border &&  nextborder && !previousborder) {
                /*
                possible strip starting edge.
                low priority
                +--+
                |\ |
                | \|
                +==+
                 \ |
                  \|
                   +
                */
                return 4;
            } else if ( border && !nextborder &&  previousborder) {
                /*
                not possible strip starting edge.
                edge in a strip,
                lower priority
                +--+
                 \ |\
                  \| \
                   +==+
                    \ |
                     \|
                      +
                */
                return 3;
            } else if (!border &&  nextborder &&  previousborder) {
                /*
                possible strip starting edge.
                edge in a strip,
                low priority
                +--+--+
                 \ |\ |
                  \| \|
                   +==+
                */
                return 4;
            }

            // THREE NEIGHBOR TRIANGLES EDGES //////////////////////////////////
            else if ( border &&  nextborder &&  previousborder) {
                /*
                possible strip starting edge.
                edge in the middle of multiple triangles.
                lowest priority
                +--+--+
                 \ |\ |
                  \| \|
                   +==+
                    \ |
                     \|
                      +
                */
                return 0;
            } else {
                throw new IllegalArgumentException("Should never happen");
            }
        }

        @Override
        public String toString() {
            return "E " + start + " " + end + " P" + priority();
        }
    }

}
