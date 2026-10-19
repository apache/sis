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
package org.apache.sis.geometries.internal.shared.topology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.internal.shared.ArrayDataPoints;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.solid.Polyhedron;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometries.surface.PolyhedralSurface;
import org.apache.sis.maths.Array;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Utilities;
import static org.apache.sis.geometries.internal.shared.topology.IndexedComplex.index;


/**
 * Builds an {@link IndexedComplex} from geometries. This is the construction of a topological complex
 * from a geometric collection defined by ISO 19107:2019 section 10.7.2.2.
 *
 * <p>The supported geometries are points, line strings, polygons (including triangles), polyhedral surfaces,
 * polyhedra, meshes of points, lines or triangles, and collections of them. Their primitives are merged
 * as follows:</p>
 * <ul>
 *   <li>Positions having exactly the same coordinates are the same node. There is no tolerance.
 *       The node keeps the other attributes of the positions, such as normals or colors, taking each attribute
 *       from the first position having it. See {@link NodePoints}.</li>
 *   <li>Segments between the same two nodes are the same edge, whether they come from lines or polygons.</li>
 *   <li>Polygons having the same edges are the same face, possibly with opposite orientations.
 *       This is how the face between two adjacent polyhedra is shared.</li>
 * </ul>
 *
 * <p>The orientations are normalized: the holes of a face are wound opposite to its exterior ring,
 * so that the face is on the left of all its edges, and the shells of a polyhedron are oriented
 * by the sign of the volume they enclose, so that the solid is below all its faces.</p>
 *
 * <p>{@link #build()} verifies that the interiors of the primitives are mutually disjoint, as required by
 * ISO 19107, except for the primitives isolated in a primitive of higher dimension: a node inside a face
 * or a solid, and an edge inside a solid. A point inside an edge, or a line crossing a face, is rejected
 * rather than split: splitting is done afterward with {@link ComplexEditor}. A builder which has thrown
 * an exception should be discarded.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ComplexBuilder {

    /**
     * The reference system of all geometries, or {@code null} if no geometry has been added yet.
     */
    private CoordinateReferenceSystem crs;

    /**
     * Number of coordinates of each position: 2 or 3, or 0 if no geometry has been added yet.
     */
    private int dimension;

    /**
     * Index of the node at each position.
     */
    private final Map<Position, Integer> nodes = new HashMap<>();

    /**
     * Positions and other attributes of the nodes, or {@code null} if no geometry has been added yet.
     */
    private NodePoints points;

    /**
     * Index of the edge between two nodes, keyed by the smallest node index in the high bits.
     */
    private final Map<Long, Integer> edges = new HashMap<>();

    /**
     * Index of the face having a given set of edges.
     */
    private final Map<EdgeSet, Integer> faces = new HashMap<>();

    /**
     * The arrays of the complex being built. See {@link IndexedComplex} for their meaning.
     */
    private final Ints edgeNodes   = new Ints(),
                       faceRings   = new Ints(0),
                       ringEdges   = new Ints(0),
                       faceEdges   = new Ints(),
                       solidShells = new Ints(0),
                       shellFaces  = new Ints(0),
                       solidFaces  = new Ints();

    /**
     * The geometries given to {@link #add(Geometry)}.
     */
    private final List<Geometry> sources = new ArrayList<>();

    /**
     * Creates an empty builder.
     */
    public ComplexBuilder() {
    }

    /**
     * Adds the primitives of the given geometry. All geometries shall have the same coordinate reference system,
     * of 2 or 3 dimensions.
     *
     * @param  geometry  a point, line string, polygon, polyhedral surface, polyhedron, mesh, or collection of them.
     * @return this builder.
     * @throws IllegalArgumentException if the geometry is not supported, has another reference system,
     *         or has a degenerate line or ring.
     */
    public ComplexBuilder add(final Geometry geometry) {
        ArgumentChecks.ensureNonNull("geometry", geometry);
        final CoordinateReferenceSystem other = geometry.getCoordinateReferenceSystem();
        if (other == null) {
            throw new IllegalArgumentException("The geometry has no coordinate reference system.");
        }
        if (crs == null) {
            final int n = other.getCoordinateSystem().getDimension();
            if (n != 2 && n != 3) {
                throw new IllegalArgumentException("Only 2 and 3 dimensional geometries are supported, found " + n + '.');
            }
            crs = other;
            dimension = n;
            points = new NodePoints(crs);
        } else if (!Utilities.equalsIgnoreMetadata(crs, other)) {
            throw new IllegalArgumentException("All geometries shall have the same coordinate reference system.");
        }
        addComponent(geometry);
        sources.add(geometry);
        return this;
    }

    /**
     * Adds the primitives of the given geometry, which may be a component of a geometry given to {@link #add(Geometry)}.
     */
    private void addComponent(final Geometry geometry) {
        // Meshes first, since some of them are also line strings, surfaces or collections.
        if (geometry instanceof MeshPrimitive mesh) {
            addMesh(mesh);
        } else if (geometry instanceof Polyhedron solid) {
            addPolyhedron(solid);
        } else if (geometry instanceof Polygon polygon) {
            addPolygon(polygon);
        } else if (geometry instanceof PolyhedralSurface<?> surface) {
            for (int i = 0, n = surface.getNumPatches(); i < n; i++) {
                addPolygon(surface.getPatchN(i));
            }
        } else if (geometry instanceof LineString line) {
            addLine(line);
        } else if (geometry instanceof Point point) {
            addPoint(point);
        } else if (geometry instanceof GeometryCollection<?> collection) {
            for (int i = 0, n = collection.getNumGeometries(); i < n; i++) {
                addComponent(collection.getGeometryN(i));
            }
        } else {
            throw new IllegalArgumentException("Unsupported geometry: " + geometry.getClass().getSimpleName() + '.');
        }
    }

    /**
     * Returns the complex of all primitives added so far.
     *
     * @return the complex of the added geometries.
     * @throws IllegalArgumentException if the interiors of the primitives are not mutually disjoint,
     *         if a face has no area, or if a shell is not closed.
     */
    public IndexedComplex build() {
        return new IndexedComplex(crs, dimension, (points != null) ? points : new NodePoints(null), edgeNodes.toArray(),
                faceRings.toArray(), ringEdges.toArray(), faceEdges.toArray(),
                solidShells.toArray(), shellFaces.toArray(), solidFaces.toArray(), sources);
    }

    /**
     * Adds a point as a node. An empty point is ignored.
     */
    private void addPoint(final Point point) {
        if (!point.isEmpty()) {
            final double[] position = point.getPosition().toArrayDouble();
            if (position.length != dimension) {
                throw new IllegalArgumentException("A point has " + position.length + " coordinates, expected " + dimension + '.');
            }
            node(point.getDataPoints(), 0, position, 0);
        }
    }

    /**
     * Adds a line string as edges between its consecutive positions. A closed line string gives a cycle of edges.
     * An empty line string is ignored.
     */
    private void addLine(final LineString line) {
        if (line.isEmpty()) {
            return;
        }
        final DataPoints source = line.getDataPoints();
        final Array positions = source.getAttributeArray(DataPointsType.ATT_POSITION);
        ensureDimension(positions);
        final double[] c = positions.toArrayDouble();
        final int[] nodes = new int[c.length / dimension];
        int n = 0;
        for (int i = 0; i < c.length; i += dimension) {
            final int node = node(source, i / dimension, c, i);
            if (n == 0 || nodes[n - 1] != node) {
                nodes[n++] = node;
            }
        }
        if (n < 2) {
            throw new IllegalArgumentException("A line has less than 2 distinct positions.");
        }
        for (int i = 1; i < n; i++) {
            edge(nodes[i - 1], nodes[i]);
        }
    }

    /**
     * Adds a polyhedron as a solid. Each shell is reversed if needed, so that the exterior shell encloses
     * a positive volume and the interior shells a negative one, which puts the solid below all faces.
     */
    private void addPolyhedron(final Polyhedron solid) {
        if (dimension != 3) {
            throw new IllegalArgumentException("A polyhedron requires 3-dimensional coordinates.");
        }
        final List<GeometryCollection<Polygon>> interiors = solid.getInteriorShells();
        final int[][] shells = new int[1 + interiors.size()][];
        shells[0] = addShell(solid.getExteriorShell());
        for (int i = 0; i < interiors.size(); i++) {
            shells[i + 1] = addShell(interiors.get(i));
        }
        for (int s = 0; s < shells.length; s++) {
            final int[] shell = shells[s];
            final double volume = signedVolume(shell);
            if ((s == 0) ? volume < 0 : volume > 0) {
                for (int i = 0; i < shell.length; i++) {
                    shell[i] = ~shell[i];
                }
            }
            for (final int ref : shell) {
                solidFaces.add(ref);
            }
            shellFaces.add(solidFaces.size());
        }
        solidShells.add(shellFaces.size() - 1);
    }

    /**
     * Adds the faces of the given shell.
     *
     * @return signed references to the faces, positive for the orientation of the polygons.
     */
    private int[] addShell(final GeometryCollection<Polygon> shell) {
        final int[] refs = new int[shell.getNumGeometries()];
        for (int i = 0; i < refs.length; i++) {
            refs[i] = addPolygon(shell.getGeometryN(i));
        }
        return refs;
    }

    /**
     * Adds a polygon as a face.
     *
     * @return signed reference to the face, positive for the orientation of the polygon.
     */
    private int addPolygon(final Polygon polygon) {
        final List<LinearRing> interiors = polygon.getInteriorRings();
        final int[][] rings = new int[1 + interiors.size()][];
        rings[0] = ringNodes(polygon.getExteriorRing());
        for (int i = 0; i < interiors.size(); i++) {
            rings[i + 1] = ringNodes(interiors.get(i));
        }
        return addFace(rings);
    }

    /**
     * Adds the points, segments or triangles of the given mesh as nodes, edges or faces.
     * Segments and triangles having twice the same node are ignored.
     */
    private void addMesh(final MeshPrimitive mesh) {
        final Array positions = mesh.getPositions();
        ensureDimension(positions);
        final Map<String, Array> attributes = new LinkedHashMap<>();
        for (final String name : mesh.getDataPointsType().getAttributeNames()) {
            final Array values = mesh.getAttribute(name);
            if (values != null) {
                attributes.put(name, values);
            }
        }
        final DataPoints source = new ArrayDataPoints(attributes);      // the vertices, in the order of the index.
        final double[] c = positions.toArrayDouble();
        final int count = c.length / dimension;
        final Array index = mesh.getIndex();
        final int[] indices;
        if (index != null) {
            indices = index.toArrayInt();
        } else {
            indices = new int[count];
            for (int i = 0; i < count; i++) {
                indices[i] = i;
            }
        }
        final int[] vertexNodes = new int[count];
        Arrays.fill(vertexNodes, -1);
        final IntUnaryOperator nodeOf = (vertex) -> {
            if (vertexNodes[vertex] < 0) {
                vertexNodes[vertex] = node(source, vertex, c, vertex * dimension);
            }
            return vertexNodes[vertex];
        };
        final int n = indices.length;
        switch (mesh.getType()) {
            case POINTS: {
                for (final int vertex : indices) {
                    nodeOf.applyAsInt(vertex);
                }
                break;
            }
            case LINES: {
                for (int i = 0; i + 1 < n; i += 2) {
                    addSegment(nodeOf.applyAsInt(indices[i]), nodeOf.applyAsInt(indices[i + 1]));
                }
                break;
            }
            case LINE_STRIP:
            case LINE_LOOP: {
                for (int i = 0; i + 1 < n; i++) {
                    addSegment(nodeOf.applyAsInt(indices[i]), nodeOf.applyAsInt(indices[i + 1]));
                }
                if (mesh.getType() == MeshPrimitive.Type.LINE_LOOP && n > 2) {
                    addSegment(nodeOf.applyAsInt(indices[n - 1]), nodeOf.applyAsInt(indices[0]));
                }
                break;
            }
            case TRIANGLES: {
                for (int i = 0; i + 2 < n; i += 3) {
                    addTriangle(nodeOf.applyAsInt(indices[i]), nodeOf.applyAsInt(indices[i + 1]), nodeOf.applyAsInt(indices[i + 2]));
                }
                break;
            }
            case TRIANGLE_STRIP: {
                // Every other triangle of a strip is swapped for keeping the orientation.
                for (int i = 0; i + 2 < n; i++) {
                    final boolean odd = (i & 1) != 0;
                    addTriangle(nodeOf.applyAsInt(indices[odd ? i + 1 : i]),
                                nodeOf.applyAsInt(indices[odd ? i : i + 1]),
                                nodeOf.applyAsInt(indices[i + 2]));
                }
                break;
            }
            case TRIANGLE_FAN: {
                for (int i = 1; i + 1 < n; i++) {
                    addTriangle(nodeOf.applyAsInt(indices[0]), nodeOf.applyAsInt(indices[i]), nodeOf.applyAsInt(indices[i + 1]));
                }
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported mesh type: " + mesh.getType() + '.');
            }
        }
    }

    /**
     * Adds an edge between the given nodes, unless they are the same node.
     */
    private void addSegment(final int a, final int b) {
        if (a != b) {
            edge(a, b);
        }
    }

    /**
     * Adds a triangular face of the given nodes, unless two of them are the same node.
     */
    private void addTriangle(final int a, final int b, final int c) {
        if (a != b && b != c && c != a) {
            addFace(new int[][] {{a, b, c}});
        }
    }

    /**
     * Verifies that the given positions have the dimension of the reference system.
     */
    private void ensureDimension(final Array positions) {
        final int n = positions.getDimension();
        if (n != dimension) {
            throw new IllegalArgumentException("Positions have " + n + " coordinates, expected " + dimension + '.');
        }
    }

    /**
     * Returns the nodes of the given ring, without consecutive duplicates and without repeating the first node.
     */
    private int[] ringNodes(final LinearRing ring) {
        final DataPoints source = ring.getDataPoints();
        final Array positions = source.getAttributeArray(DataPointsType.ATT_POSITION);
        ensureDimension(positions);
        final double[] c = positions.toArrayDouble();
        final int[] nodes = new int[c.length / dimension];
        int n = 0;
        for (int i = 0; i < c.length; i += dimension) {
            final int node = node(source, i / dimension, c, i);
            if (n == 0 || nodes[n - 1] != node) {
                nodes[n++] = node;
            }
        }
        while (n > 1 && nodes[n - 1] == nodes[0]) {
            n--;
        }
        if (n < 3) {
            throw new IllegalArgumentException("A ring has less than 3 distinct positions.");
        }
        return Arrays.copyOf(nodes, n);
    }

    /**
     * Returns the node at the position starting at the given offset, creating it if needed.
     * The other attributes of the source point are copied to the node, unless the node already has them.
     *
     * @param  source  the data points from which the coordinates have been read.
     * @param  index   index of the point in the source.
     * @param  c       coordinates read from the source.
     * @param  offset  index of the first coordinate of the point in {@code c}.
     */
    private int node(final DataPoints source, final int index, final double[] c, final int offset) {
        final double[] position = new double[dimension];
        for (int k = 0; k < dimension; k++) {
            final double value = c[offset + k];
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Coordinates shall be finite.");
            }
            position[k] = value + 0.0;          // replaces −0 by +0, so that both are the same node.
        }
        final Position key = new Position(position);
        Integer node = nodes.get(key);
        if (node == null) {
            node = points.add(position);
            nodes.put(key, node);
        }
        points.merge(node, source, index);
        return node;
    }

    /**
     * Returns the edge from node {@code a} to node {@code b}, creating it if needed.
     *
     * @return signed reference to the edge, negative if the edge goes from {@code b} to {@code a}.
     */
    private int edge(final int a, final int b) {
        final long key = (a < b) ? ((long) a << 32) | b : ((long) b << 32) | a;
        Integer edge = edges.get(key);
        if (edge == null) {
            edge = edgeNodes.size() / 2;
            edges.put(key, edge);
            edgeNodes.add(a);
            edgeNodes.add(b);
            return edge;
        }
        return (edgeNodes.get(2 * edge) == a) ? edge : ~edge;
    }

    /**
     * Adds a face of the given rings of nodes, the exterior ring first, or returns the existing face
     * having the same edges. The holes are reversed if needed for being wound opposite to the exterior ring.
     *
     * @return signed reference to the face, positive for the orientation of the exterior ring.
     */
    private int addFace(final int[][] rings) {
        final double[] normal = normal(rings[0]);
        for (int r = 1; r < rings.length; r++) {
            final double[] n = normal(rings[r]);
            if (n[0]*normal[0] + n[1]*normal[1] + n[2]*normal[2] > 0) {
                final int[] ring = rings[r];
                for (int i = 0, j = ring.length - 1; i < j; i++, j--) {
                    final int t = ring[i]; ring[i] = ring[j]; ring[j] = t;
                }
            }
        }
        final int[][] refs = new int[rings.length][];
        int count = 0;
        for (int r = 0; r < rings.length; r++) {
            final int[] ring = rings[r];
            refs[r] = new int[ring.length];
            for (int i = 0; i < ring.length; i++) {
                refs[r][i] = edge(ring[i], ring[(i + 1) % ring.length]);
            }
            count += ring.length;
        }
        final int[] sorted = new int[count];
        count = 0;
        for (final int[] ring : refs) {
            for (final int ref : ring) {
                sorted[count++] = index(ref);
            }
        }
        Arrays.sort(sorted);
        final EdgeSet key = new EdgeSet(sorted);
        final Integer existing = faces.get(key);
        if (existing != null) {
            return sameOrientation(existing, refs[0][0]) ? existing : ~existing;
        }
        final int face = faceRings.size() - 1;
        for (final int[] ring : refs) {
            for (final int ref : ring) {
                faceEdges.add(ref);
            }
            ringEdges.add(faceEdges.size());
        }
        faceRings.add(ringEdges.size() - 1);
        faces.put(key, face);
        return face;
    }

    /**
     * Returns whether the given face uses the edge of the given signed reference in the same direction.
     */
    private boolean sameOrientation(final int face, final int edgeRef) {
        final int edge = index(edgeRef);
        for (int i = ringEdges.get(faceRings.get(face)), end = ringEdges.get(faceRings.get(face + 1)); i < end; i++) {
            final int ref = faceEdges.get(i);
            if (index(ref) == edge) {
                return (ref < 0) == (edgeRef < 0);
            }
        }
        return true;
    }

    /**
     * Returns the coordinate of the given node on the given axis, 0 for the third axis in two dimensions.
     */
    private double coordinate(final int node, final int axis) {
        return (axis < dimension) ? points.coordinates[node * dimension + axis] : 0;
    }

    /**
     * Returns the normal of the given ring of nodes with Newell's method,
     * pointing toward an observer who sees the ring counterclockwise.
     */
    private double[] normal(final int[] ring) {
        final double[] normal = new double[3];
        for (int i = 0; i < ring.length; i++) {
            final int a = ring[i], b = ring[(i + 1) % ring.length];
            final double ax = coordinate(a, 0), ay = coordinate(a, 1), az = coordinate(a, 2);
            final double bx = coordinate(b, 0), by = coordinate(b, 1), bz = coordinate(b, 2);
            normal[0] += (ay - by) * (az + bz);
            normal[1] += (az - bz) * (ax + bx);
            normal[2] += (ax - bx) * (ay + by);
        }
        return normal;
    }

    /**
     * Returns the volume enclosed by the given faces, positive if they are oriented outward.
     * The volume is the sum of the tetrahedra joining a reference node to the triangles
     * of a fan over each ring, which is exact for planar rings, convex or not.
     */
    private double signedVolume(final int[] shell) {
        final int origin = startNode(faceEdges.get(ringEdges.get(faceRings.get(index(shell[0])))));
        double volume = 0;
        for (final int ref : shell) {
            final int face = index(ref);
            double sum = 0;
            for (int r = faceRings.get(face); r < faceRings.get(face + 1); r++) {
                final int lo = ringEdges.get(r), hi = ringEdges.get(r + 1);
                final int first = startNode(faceEdges.get(lo));
                for (int i = lo + 1; i + 1 < hi; i++) {
                    sum += determinant(origin, first, startNode(faceEdges.get(i)), startNode(faceEdges.get(i + 1)));
                }
            }
            volume += (ref < 0) ? -sum : sum;
        }
        return volume / 6;
    }

    /**
     * Returns the node where the given oriented edge starts.
     */
    private int startNode(final int edgeRef) {
        return edgeNodes.get(2 * index(edgeRef) + (edgeRef < 0 ? 1 : 0));
    }

    /**
     * Returns the determinant of the vectors from node {@code o} to nodes {@code a}, {@code b} and {@code c},
     * which is six times the signed volume of the tetrahedron.
     */
    private double determinant(final int o, final int a, final int b, final int c) {
        final double ox = coordinate(o, 0), oy = coordinate(o, 1), oz = coordinate(o, 2);
        final double ax = coordinate(a, 0) - ox, ay = coordinate(a, 1) - oy, az = coordinate(a, 2) - oz;
        final double bx = coordinate(b, 0) - ox, by = coordinate(b, 1) - oy, bz = coordinate(b, 2) - oz;
        final double cx = coordinate(c, 0) - ox, cy = coordinate(c, 1) - oy, cz = coordinate(c, 2) - oz;
        return ax * (by*cz - bz*cy) + ay * (bz*cx - bx*cz) + az * (bx*cy - by*cx);
    }

    /**
     * Coordinates of a node, compared by value.
     */
    private record Position(double[] coordinates) {
        @Override
        public boolean equals(final Object obj) {
            return (obj instanceof Position other) && Arrays.equals(coordinates, other.coordinates);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(coordinates);
        }
    }

    /**
     * Sorted indices of the edges of a face, compared by value.
     */
    private record EdgeSet(int[] edges) {
        @Override
        public boolean equals(final Object obj) {
            return (obj instanceof EdgeSet other) && Arrays.equals(edges, other.edges);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(edges);
        }
    }

    /**
     * A growable array of integers.
     */
    private static final class Ints {
        private int[] values = new int[16];
        private int size;

        Ints(final int... initial) {
            for (final int value : initial) {
                add(value);
            }
        }

        void add(final int value) {
            if (size == values.length) {
                values = Arrays.copyOf(values, size * 2);
            }
            values[size++] = value;
        }

        int get(final int i) {
            return values[i];
        }

        int size() {
            return size;
        }

        int[] toArray() {
            return Arrays.copyOf(values, size);
        }
    }
}
