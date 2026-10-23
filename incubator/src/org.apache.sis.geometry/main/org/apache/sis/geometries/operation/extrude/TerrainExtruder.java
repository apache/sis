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
package org.apache.sis.geometries.operation.extrude;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.internal.shared.Planar;
import org.apache.sis.geometries.internal.shared.Planar.DoubleList;
import org.apache.sis.geometries.internal.shared.Planar.IntList;
import org.apache.sis.geometries.internal.shared.Planar.XY;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.mesh.MeshPrimitiveIndexes;
import org.apache.sis.geometries.mesh.MultiMeshPrimitive;
import org.apache.sis.geometries.operation.Intersection;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.geometries.operation.ProcessorUtils;
import org.apache.sis.geometries.operation.spatialanalysis2d.Centerline;
import org.apache.sis.geometries.operation.spatialanalysis2d.StraightSkeleton;
import org.apache.sis.geometries.operation.triangulate.delaunay.Delaunay;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.Maths;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.util.ArgumentChecks;


/**
 * Embeds flat areas, such as building footprints or roads, in a terrain mesh.
 *
 * <p>The terrain is a height field: a triangle mesh in which each (x,y) location has a single elevation.
 * Each {@linkplain #appendFlatConstraint(Polygon, double, double, HeightReference, boolean) appended polygon}
 * cuts the terrain along its rings and replaces the part inside by a flat top, depending on the
 * {@linkplain HeightReference height reference}:</p>
 * <ul>
 *   <li>with an absolute height, the top is horizontal at the given elevation;</li>
 *   <li>relative to the ground, the top has no sideways tilt but follows the terrain along the
 *       {@linkplain Centerline centerline} of the polygon, like a road climbing a mountain.
 *       Each point of the top is at the elevation of the ground at its nearest point on the centerline,
 *       plus the given height;</li>
 *   <li>at the centroid, the top is horizontal at the elevation of the ground at the polygon centroid,
 *       plus the given height, like a building on a slope.</li>
 * </ul>
 * <p>The top is joined to the terrain either by vertical walls, or by moving the terrain vertices
 * on the polygon rings to the elevation of the top, in which case the neighboring terrain triangles slope
 * to the top. When polygons overlap, the last appended one wins: it cuts into the tops of the previous ones.</p>
 *
 * <p>The top can be replaced by a hipped roof, computed from the {@linkplain StraightSkeleton straight skeleton}
 * of the polygon: every face is planar and slopes up from one edge of the polygon with the same pitch,
 * from the top elevation at the eaves to the roof elevation at the highest ridge.</p>
 *
 * <p>Only the positions of the terrain are used, other attributes such as normals or texture coordinates
 * are not in the {@linkplain #getResult() result}. The polygons must have the same horizontal CRS than
 * the terrain. Parts of a polygon outside the terrain get a top but no wall.</p>
 *
 * <p>Limitation: where the rings of a polygon cross a wall of a previous polygon, the new walls on each side
 * of the crossing stand on different vertices (the previous top and the ground). The surface is closed but
 * has a vertical T-junction there.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class TerrainExtruder {

    /**
     * How the height of an appended polygon is measured.
     */
    public enum HeightReference {
        /**
         * The height is an elevation, the top is horizontal.
         */
        ABSOLUTE,

        /**
         * The height is above the ground along the polygon centerline. The top has no sideways tilt
         * but follows the terrain, like a road climbing a mountain.
         */
        RELATIVE_TO_GROUND,

        /**
         * The height is above the ground at the polygon centroid, the top is horizontal.
         * If the centroid is outside the terrain, the mean ground elevation on the polygon vertices is used.
         */
        POINT_AT_CENTROID
    }

    /**
     * The axes of the (x,y) plane.
     */
    private static final int[] XY_AXES = {0, 1};

    /**
     * Ratio on an edge under which a point is considered at the edge end.
     */
    private static final double SNAP = 1e-9;

    /**
     * The terrain given at construction, for its CRS.
     */
    private final MeshPrimitive.Triangles terrain;

    /**
     * Vertex positions, three values per vertex. Vertices are never removed,
     * unused vertices are dropped when building the result.
     */
    private double[] xyz;
    private int nbVertex;

    /**
     * Surface triangles, counterclockwise when seen from above, three vertex indices per triangle.
     * A removed triangle has -1 as first index. Walls are not stored, they are created in the result
     * between the edges facing each other on both sides of a cut.
     */
    private int[] triangles;
    private int nbTriangle;

    /**
     * Identifiers of the surface triangles which are tops or roofs of the appended areas.
     * Pieces of a cut triangle keep its kind.
     */
    private final BitSet roofs = new BitSet();

    /**
     * Whether the terrain, the roofs and the walls are returned in separated mesh primitives.
     */
    private boolean separateParts;

    /**
     * Index of the surface triangles by their (x,y) envelope.
     */
    private final Quadtree index = new Quadtree();

    /**
     * Maximum distance between points of the polygon rings and of the centerline,
     * used for polygons relative to the ground.
     */
    private double densification;

    /**
     * Prepares the extrusion of polygons in the given terrain.
     * The terrain is copied, it is not modified by this class.
     *
     * @param terrain the terrain, a 3D height field, not null
     * @throws IllegalArgumentException if the terrain positions are not 3D
     */
    public TerrainExtruder(final MeshPrimitive.Triangles terrain) {
        ArgumentChecks.ensureNonNull("terrain", terrain);
        final Array positions = terrain.getPositions();
        if (positions == null || positions.getDimension() != 3) {
            throw new IllegalArgumentException("Terrain positions must be 3D");
        }
        this.terrain = terrain;
        this.xyz = positions.toArrayDouble();
        this.nbVertex = xyz.length / 3;
        final Array idx = terrain.getIndex();
        final int[] indices = (idx != null) ? MeshPrimitiveIndexes.toTriangles(idx, terrain.getType()).toArrayInt()
                                            : IntStream.range(0, nbVertex / 3 * 3).toArray();
        this.triangles = new int[Math.max(indices.length, 30)];
        double length = 0;
        for (int i = 0; i + 2 < indices.length; i += 3) {
            final int a = indices[i], b = indices[i+1], c = indices[i+2];
            addTriangle(a, b, c, false);
            length += distance2D(a, b) + distance2D(b, c) + distance2D(c, a);
        }
        densification = (length > 0) ? length / (indices.length / 3 * 3) : 1;
    }

    /**
     * Returns the maximum distance between the points of the rings and of the centerline,
     * used for polygons relative to the ground.
     *
     * @return the densification distance, the mean terrain edge length by default
     */
    public double getDensification() {
        return densification;
    }

    /**
     * Sets the maximum distance between the points of the rings and of the centerline,
     * used for polygons relative to the ground. Smaller values follow the terrain more closely
     * but create more triangles.
     *
     * @param densification the densification distance, strictly positive
     */
    public void setDensification(final double densification) {
        ArgumentChecks.ensureStrictlyPositive("densification", densification);
        this.densification = densification;
    }

    /**
     * Returns whether the terrain, the roofs and the walls are returned in separated mesh primitives.
     *
     * @return whether the result parts are separated, {@code false} by default
     */
    public boolean getSeparateParts() {
        return separateParts;
    }

    /**
     * Sets whether the terrain, the roofs and the walls are returned in separated mesh primitives.
     * Separated primitives do not share vertices, so normals computed on each primitive are not averaged
     * across the sharp edges between the terrain, the walls and the roofs.
     *
     * @param separateParts whether the result parts are separated
     */
    public void setSeparateParts(final boolean separateParts) {
        this.separateParts = separateParts;
    }

    /**
     * Appends a flat area, possibly covered by a hipped roof. The terrain is cut along the polygon rings
     * and the part inside the polygon is replaced by the top. Tops of previously appended polygons
     * are cut the same way.
     *
     * @param polygon the area footprint, only the (x,y) coordinates are used
     * @param height the elevation of the top if absolute, or its height above the ground;
     *        with a roof, this is the elevation of the eaves
     * @param roofHeight elevation of the highest roof ridge, in the same way than {@code height}.
     *        If equal to {@code height} the top is flat, otherwise it is a hipped roof
     * @param reference how the heights are measured
     * @param walls {@code true} to join the top to the terrain with vertical walls,
     *        {@code false} to move the terrain vertices on the rings to the top elevation
     * @throws OperationException if the polygon CRS differs from the terrain one, or a triangulation
     *         or the straight skeleton computation failed
     */
    public void appendFlatConstraint(final Polygon polygon, final double height, final double roofHeight,
            final HeightReference reference, final boolean walls) throws OperationException
    {
        ArgumentChecks.ensureNonNull("polygon", polygon);
        ArgumentChecks.ensureNonNull("reference", reference);
        ProcessorUtils.ensureSameCRS2D(terrain, polygon);
        final List<double[]> rings = Planar.rings(polygon);
        if (!rings.isEmpty()) {
            new Append(polygon, rings, height, roofHeight, reference, walls).run();
        }
    }

    /**
     * Returns the terrain with the appended flat areas.
     * By default the result contains a single mesh primitive. If {@linkplain #setSeparateParts(boolean)
     * parts are separated}, the result contains the terrain, the roofs and the walls in that order,
     * without the empty ones, each with its own vertices.
     *
     * @return new meshes, with positions only
     */
    public MultiMeshPrimitive<MeshPrimitive.Triangles> getResult() {
        final IntList terrainFaces = new IntList(), roofFaces = new IntList(), wallFaces = new IntList();
        for (int i = 0; i < nbTriangle; i++) {
            if (triangles[i*3] >= 0) {
                (roofs.get(i) ? roofFaces : terrainFaces).add(triangles[i*3], triangles[i*3+1], triangles[i*3+2]);
            }
        }
        /*
         * A surface edge without neighbor is on the terrain border or on a cut made by a flat area.
         * On a cut, another edge without neighbor goes in the reverse direction at the same location
         * on the other side. Those two edges are joined by a vertical wall, with the same orientation.
         */
        final long[] edges = new long[terrainFaces.size + roofFaces.size];
        int n = 0;
        for (IntList faces : List.of(terrainFaces, roofFaces)) {
            for (int i = 0; i < faces.size; i += 3) {
                for (int k = 0; k < 3; k++) {
                    edges[n++] = ((long) faces.values[i+k] << 32) | faces.values[i + (k+1) % 3];
                }
            }
        }
        Arrays.sort(edges);
        final Map<List<XY>, int[]> open = new HashMap<>();
        for (long edge : edges) {
            final int a = (int) (edge >>> 32), b = (int) edge;
            if (Arrays.binarySearch(edges, ((long) b << 32) | a) < 0) {
                final int[] other = open.remove(List.of(key(b), key(a)));
                if (other != null) {
                    wallFaces.add(b, a, other[1], b, other[1], other[0]);
                } else {
                    open.put(List.of(key(a), key(b)), new int[] {a, b});
                }
            }
        }
        final MultiMeshPrimitive<MeshPrimitive.Triangles> result = new MultiMeshPrimitive<>(terrain.getCoordinateReferenceSystem());
        if (separateParts) {
            for (IntList faces : List.of(terrainFaces, roofFaces, wallFaces)) {
                final MeshPrimitive.Triangles part = toPrimitive(faces);
                if (part != null) result.append(List.of(part));
            }
        } else {
            terrainFaces.add(roofFaces.toArray());
            terrainFaces.add(wallFaces.toArray());
            final MeshPrimitive.Triangles part = toPrimitive(terrainFaces);
            if (part != null) result.append(List.of(part));
        }
        return result;
    }

    /**
     * Creates a mesh primitive with the given triangles not degenerated in 3D, and only their vertices.
     *
     * @return the primitive, or null if there is no triangle
     */
    private MeshPrimitive.Triangles toPrimitive(final IntList faces) {
        final int[] remap = new int[nbVertex];
        Arrays.fill(remap, -1);
        final DoubleList positions = new DoubleList();
        final IntList indices = new IntList();
        for (int i = 0; i < faces.size; i += 3) {
            final int a = faces.values[i]*3, b = faces.values[i+1]*3, c = faces.values[i+2]*3;
            final double ux = xyz[b] - xyz[a], uy = xyz[b+1] - xyz[a+1], uz = xyz[b+2] - xyz[a+2];
            final double vx = xyz[c] - xyz[a], vy = xyz[c+1] - xyz[a+1], vz = xyz[c+2] - xyz[a+2];
            if (uy*vz - uz*vy == 0 && uz*vx - ux*vz == 0 && ux*vy - uy*vx == 0) continue;
            for (int k = 0; k < 3; k++) {
                final int v = faces.values[i+k];
                if (remap[v] < 0) {
                    remap[v] = positions.size / 3;
                    positions.add(xyz[v*3], xyz[v*3+1], xyz[v*3+2]);
                }
                indices.add(remap[v]);
            }
        }
        if (indices.size == 0) {
            return null;
        }
        final MeshPrimitive.Triangles primitive = new MeshPrimitive.Triangles();
        primitive.setPositions(NDArrays.of(terrain.getCoordinateReferenceSystem(), positions.toArray()));
        primitive.setIndex(NDArrays.ofUnsigned(1, indices.toArray()));
        return primitive;
    }

    // ---------------------------------------------------------------------------------------------
    // Working mesh
    // ---------------------------------------------------------------------------------------------

    private int addVertex(final double x, final double y, final double z) {
        if (nbVertex * 3 == xyz.length) {
            xyz = Arrays.copyOf(xyz, Math.max(30, xyz.length * 2));
        }
        xyz[nbVertex*3]   = x;
        xyz[nbVertex*3+1] = y;
        xyz[nbVertex*3+2] = z;
        return nbVertex++;
    }

    /**
     * Adds a surface triangle, made counterclockwise. Flat triangles are ignored.
     *
     * @param roof whether the triangle is part of a top or a roof
     */
    private void addTriangle(final int a, int b, int c, final boolean roof) {
        final double side = side(a, b, xyz[c*3], xyz[c*3+1]);
        if (side == 0) {
            return;
        }
        if (side < 0) {
            final int t = b; b = c; c = t;
        }
        if (nbTriangle * 3 == triangles.length) {
            triangles = Arrays.copyOf(triangles, triangles.length * 2);
        }
        final int id = nbTriangle++;
        triangles[id*3]   = a;
        triangles[id*3+1] = b;
        triangles[id*3+2] = c;
        roofs.set(id, roof);
        index.insert(envelope(id), id);
    }

    private void removeTriangle(final int id) {
        index.remove(envelope(id), id);
        triangles[id*3] = -1;
    }

    private Envelope envelope(final int triangle) {
        final int a = triangles[triangle*3], b = triangles[triangle*3+1], c = triangles[triangle*3+2];
        final Envelope env = new Envelope(xyz[a*3], xyz[b*3], xyz[a*3+1], xyz[b*3+1]);
        env.expandToInclude(xyz[c*3], xyz[c*3+1]);
        return env;
    }

    /**
     * Returns the identifiers of the alive surface triangles whose envelope intersects the given one.
     */
    private int[] query(final Envelope env) {
        final IntList result = new IntList();
        for (Object item : index.query(env)) {
            final int id = (Integer) item;
            if (triangles[id*3] >= 0 && env.intersects(envelope(id))) {
                result.add(id);
            }
        }
        return result.toArray();
    }

    /**
     * Returns the robust side of a point relative to the line from vertex a to vertex b, positive on the left.
     */
    private double side(final int a, final int b, final double x, final double y) {
        return Maths.lineSide(xyz[a*3], xyz[a*3+1], xyz[b*3], xyz[b*3+1], x, y);
    }

    /**
     * Returns whether vertex u is before vertex v in (x,y) order. Edges are always processed from their first
     * vertex in that order, so that edges at the same location on both sides of a cut give the same points.
     */
    private boolean before(final int u, final int v) {
        return xyz[u*3] < xyz[v*3] || (xyz[u*3] == xyz[v*3] && xyz[u*3+1] < xyz[v*3+1]);
    }

    private double distance2D(final int a, final int b) {
        return Math.hypot(xyz[b*3] - xyz[a*3], xyz[b*3+1] - xyz[a*3+1]);
    }

    private double[] xy(final int v) {
        return new double[] {xyz[v*3], xyz[v*3+1]};
    }

    private XY key(final int v) {
        return XY.of(xyz[v*3], xyz[v*3+1]);
    }

    /**
     * Returns the elevation of the surface at the given location, or NaN if outside the terrain.
     */
    private double groundZ(final double x, final double y) {
        for (int id : query(new Envelope(x, x, y, y))) {
            final int a = triangles[id*3], b = triangles[id*3+1], c = triangles[id*3+2];
            if (side(a, b, x, y) >= 0 && side(b, c, x, y) >= 0 && side(c, a, x, y) >= 0) {
                return interpolate(a, b, c, x, y);
            }
        }
        return Double.NaN;
    }

    private double interpolate(final int a, final int b, final int c, final double x, final double y) {
        final double[] w = Maths.getBarycentricValue2D(xyz[a*3], xyz[a*3+1], xyz[b*3], xyz[b*3+1], xyz[c*3], xyz[c*3+1], x, y);
        return w[0] * xyz[a*3+2] + w[1] * xyz[b*3+2] + w[2] * xyz[c*3+2];
    }

    /**
     * Receives the points where a segment meets a surface edge.
     */
    @FunctionalInterface
    private interface Crossing {
        /**
         * @param t  ratio on the segment.
         * @param s  ratio on the edge from p to q.
         * @param p  first edge vertex in (x,y) order.
         * @param q  second edge vertex in (x,y) order.
         */
        void at(double t, double s, int p, int q);
    }

    /**
     * Finds where a segment meets the surface edges. An edge overlapping the segment gives its two ends.
     */
    private void crossings(final double x0, final double y0, final double x1, final double y1, final Crossing action) {
        final double[] a0 = {x0, y0}, a1 = {x1, y1};
        final Set<Long> seen = new HashSet<>();
        for (int id : query(new Envelope(x0, x1, y0, y1))) {
            for (int k = 0; k < 3; k++) {
                int p = triangles[id*3+k], q = triangles[id*3+(k+1)%3];
                if (!seen.add(Planar.edgeKey(p, q))) continue;
                if (!before(p, q)) {
                    final int t = p; p = q; q = t;
                }
                final Intersection.SegmentSegment r = Intersection.segmentSegment2D(a0, a1, xy(p), xy(q), XY_AXES);
                if (r.type() != Intersection.SegmentSegment.Type.NONE) {
                    action.at(r.startOnFirst(), r.startOnSecond(), p, q);
                }
                if (r.type() == Intersection.SegmentSegment.Type.OVERLAP) {
                    action.at(r.endOnFirst(), r.endOnSecond(), p, q);
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Append operation
    // ---------------------------------------------------------------------------------------------

    /**
     * Vertex inserted on an edge of the terrain.
     *
     * @param s       ratio on the edge, from its first vertex in (x,y) order.
     * @param vertex  the inserted vertex.
     */
    private record Split(double s, int vertex) {}

    /**
     * State of the insertion of one polygon.
     */
    private final class Append {

        private final Polygon polygon;
        private final List<double[]> rings;
        private final double height, roofHeight;
        private final HeightReference reference;
        private final boolean relative, withWalls;
        private final Envelope extent = new Envelope();

        /** Vertices inserted on the surface edges, by edge key. */
        private final Map<Long, List<Split>> splits = new HashMap<>();

        /** Vertices inserted inside surface triangles, by triangle identifier. */
        private final Map<Integer, IntList> interiors = new HashMap<>();

        /** The rings split where they meet the terrain edges, two values per point. */
        private double[] chain;
        /** Index of the next point in the same ring, for each chain point. */
        private int[] next;
        /** Chain point indices by location. */
        private final Map<XY, IntList> chainAt = new HashMap<>();
        /** Locations of the ring vertices moved on a terrain vertex or edge. */
        private final Map<XY, XY> snapped = new HashMap<>();

        /** Centerline sampled on the ground, (x,y,z) for each polyline, for relative tops. */
        private final List<double[]> profiles = new ArrayList<>();
        /** Elevation of the ground below a top at the centroid, or used for relative tops without centerline. */
        private double ground;

        Append(final Polygon polygon, final List<double[]> rings, final double height, final double roofHeight,
               final HeightReference reference, final boolean withWalls)
        {
            this.polygon = polygon;
            this.rings = rings;
            this.height = height;
            this.roofHeight = roofHeight;
            this.reference = reference;
            this.relative = (reference == HeightReference.RELATIVE_TO_GROUND);
            this.withWalls = withWalls;
            final double[] exterior = rings.get(0);
            for (int i = 0; i < exterior.length; i += 2) {
                extent.expandToInclude(exterior[i], exterior[i+1]);
            }
        }

        void run() throws OperationException {
            //ground sampled before the surface is modified
            switch (reference) {
                case RELATIVE_TO_GROUND: buildProfiles(); break;
                case POINT_AT_CENTROID: {
                    final double[] c = centroid();
                    ground = groundZ(c[0], c[1]);
                    if (Double.isNaN(ground)) ground = ringGround();
                    break;
                }
                default: break;
            }
            buildChain();
            cutSurface();
            buildTop();
        }

        // -----------------------------------------------------------------------------------------
        // Relative elevation
        // -----------------------------------------------------------------------------------------

        /**
         * Computes the centerline of the polygon and samples the ground elevation along it,
         * at each crossing with a surface edge and at the densification distance.
         */
        private void buildProfiles() throws OperationException {
            final GeometryCollection<LineString> lines = new Centerline(densification).compute(polygon);
            for (int l = 0; l < lines.getNumGeometries(); l++) {
                final Array positions = lines.getGeometryN(l).getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION);
                final int dim = positions.getDimension();
                final double[] c = positions.toArrayDouble();
                final int n = c.length / dim;
                final DoubleList profile = new DoubleList();
                for (int i = 0; i < n; i++) {
                    final double x0 = c[i*dim], y0 = c[i*dim+1];
                    final double x1 = c[Math.min(i+1, n-1)*dim], y1 = c[Math.min(i+1, n-1)*dim+1];
                    final DoubleList ratios = new DoubleList();
                    ratios.add(0);
                    final int steps = Planar.steps(Math.hypot(x1 - x0, y1 - y0), densification);
                    for (int k = 1; k < steps; k++) ratios.add((double) k / steps);
                    if (i < n-1) crossings(x0, y0, x1, y1, (t, s, p, q) -> {if (t > 0 && t < 1) ratios.add(t);});
                    final double[] sorted = ratios.toArray();
                    Arrays.sort(sorted);
                    for (double t : sorted) {
                        final double x = x0 + (x1 - x0) * t, y = y0 + (y1 - y0) * t;
                        profile.add(x, y, groundZ(x, y));
                    }
                }
                final double[] p = profile.toArray();
                if (fillMissing(p)) {
                    profiles.add(p);
                }
            }
            //used if there is no centerline or the centerline is outside the terrain
            ground = ringGround();
        }

        /**
         * Returns the mean elevation of the ground on the ring vertices inside the terrain, or 0 if none.
         */
        private double ringGround() {
            double sum = 0;
            int count = 0;
            for (double[] ring : rings) {
                for (int i = 0; i < ring.length; i += 2) {
                    final double z = groundZ(ring[i], ring[i+1]);
                    if (!Double.isNaN(z)) {
                        sum += z;
                        count++;
                    }
                }
            }
            return (count > 0) ? sum / count : 0;
        }

        /**
         * Returns the area centroid of the polygon, holes excluded, or the center of its extent if the area is zero.
         * Coordinates are relative to the first vertex for accuracy.
         */
        private double[] centroid() {
            final double x0 = rings.get(0)[0], y0 = rings.get(0)[1];
            double area = 0, cx = 0, cy = 0;
            for (double[] ring : rings) {
                for (int i = 0; i < ring.length; i += 2) {
                    final double xa = ring[i] - x0, ya = ring[i+1] - y0;
                    final double xb = ring[(i+2) % ring.length] - x0, yb = ring[(i+3) % ring.length] - y0;
                    final double cross = xa*yb - xb*ya;
                    area += cross;
                    cx += (xa + xb) * cross;
                    cy += (ya + yb) * cross;
                }
            }
            if (area == 0) {
                return new double[] {extent.centre().x, extent.centre().y};
            }
            return new double[] {x0 + cx / (3 * area), y0 + cy / (3 * area)};
        }

        /**
         * Replaces the NaN elevations of a profile by the nearest known elevation along it.
         *
         * @return false if all elevations are NaN
         */
        private boolean fillMissing(final double[] p) {
            int known = 2;
            while (known < p.length && Double.isNaN(p[known])) known += 3;
            if (known >= p.length) {
                return false;
            }
            for (int i = 2; i < p.length; i += 3) {
                if (Double.isNaN(p[i])) p[i] = (i < known) ? p[known] : p[i-3];
            }
            return true;
        }

        /**
         * Returns the elevation of the top at the given location.
         */
        private double topZ(final double x, final double y) {
            if (!relative) {
                return ground + height;
            }
            double best = Double.POSITIVE_INFINITY;
            double z = ground;
            for (double[] p : profiles) {
                final int n = p.length / 3;
                for (int i = 0; i < Math.max(1, n - 1); i++) {
                    final int j = Math.min(i + 1, n - 1);
                    final double x0 = p[i*3], y0 = p[i*3+1], dx = p[j*3] - x0, dy = p[j*3+1] - y0;
                    final double len2 = dx*dx + dy*dy;
                    final double t = (len2 == 0) ? 0 : Maths.clamp(((x - x0) * dx + (y - y0) * dy) / len2, 0, 1);
                    final double ex = x - x0 - dx * t, ey = y - y0 - dy * t, d = ex*ex + ey*ey;
                    if (d < best) {
                        best = d;
                        z = p[i*3+2] + (p[j*3+2] - p[i*3+2]) * t;
                    }
                }
            }
            return z + height;
        }

        // -----------------------------------------------------------------------------------------
        // Rings split on the terrain
        // -----------------------------------------------------------------------------------------

        /**
         * Splits the rings where they cross the surface edges, inserting the crossing points in the edges,
         * and inserts the other ring points in the surface.
         */
        private void buildChain() {
            final DoubleList points = new DoubleList();
            final IntList links = new IntList();
            for (double[] ring : rings) {
                final DoubleList ringPoints = new DoubleList();
                for (int i = 0; i < ring.length; i += 2) {
                    final double x0 = ring[i], y0 = ring[i+1];
                    final double x1 = ring[(i+2) % ring.length], y1 = ring[(i+3) % ring.length];
                    final List<double[]> events = new ArrayList<>();
                    final double[] start = locate(x0, y0);
                    snapped.put(XY.of(x0, y0), XY.of(start[0], start[1]));
                    events.add(new double[] {0, start[0], start[1]});
                    final int steps = relative ? Planar.steps(Math.hypot(x1 - x0, y1 - y0), densification) : 1;
                    for (int k = 1; k < steps; k++) {
                        final double t = (double) k / steps;
                        final double[] p = locate(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t);
                        events.add(new double[] {t, p[0], p[1]});
                    }
                    //the ends of the segment are inserted by locate(…)
                    crossings(x0, y0, x1, y1, (t, s, p, q) -> {
                        if (t > 0 && t < 1) {
                            final int v = vertexOnEdge(p, q, s);
                            events.add(new double[] {t, xyz[v*3], xyz[v*3+1]});
                        }
                    });
                    events.sort(Comparator.comparingDouble((e) -> e[0]));
                    for (double[] e : events) ringPoints.add(e[1], e[2]);
                }
                ringPoints.size = Planar.removeDuplicates(ringPoints.values, ringPoints.size);
                final int offset = points.size / 2, n = ringPoints.size / 2;
                points.add(ringPoints.toArray());
                for (int i = 0; i < n; i++) links.add(offset + (i + 1) % n);
            }
            chain = points.toArray();
            next  = links.toArray();
            for (int k = 0; k < next.length; k++) {
                chainAt.computeIfAbsent(location(k), (key) -> new IntList()).add(k);
            }
        }

        private XY location(final int chainPoint) {
            return XY.of(chain[chainPoint*2], chain[chainPoint*2+1]);
        }

        /**
         * Returns the vertex at the given ratio on the edge from p to q, with p before q in (x,y) order,
         * inserting it in the edge if needed.
         */
        private int vertexOnEdge(final int p, final int q, final double s) {
            if (s <= SNAP) return p;
            if (s >= 1 - SNAP) return q;
            final List<Split> list = splits.computeIfAbsent(Planar.edgeKey(p, q), (key) -> new ArrayList<>());
            for (Split split : list) {
                if (Math.abs(split.s() - s) <= SNAP) {
                    return split.vertex();
                }
            }
            final int vertex = addVertex(xyz[p*3]   + (xyz[q*3]   - xyz[p*3])   * s,
                                         xyz[p*3+1] + (xyz[q*3+1] - xyz[p*3+1]) * s,
                                         xyz[p*3+2] + (xyz[q*3+2] - xyz[p*3+2]) * s);
            list.add(new Split(s, vertex));
            return vertex;
        }

        /**
         * Inserts a ring point in the triangles containing it.
         *
         * @return the point location, moved on the vertex at that location if the point is on an edge
         */
        private double[] locate(final double x, final double y) {
            final double[] result = {x, y};
            for (int id : query(new Envelope(x, x, y, y))) {
                final int a = triangles[id*3], b = triangles[id*3+1], c = triangles[id*3+2];
                final double sab = side(a, b, x, y), sbc = side(b, c, x, y), sca = side(c, a, x, y);
                if (sab < 0 || sbc < 0 || sca < 0) {
                    continue;
                }
                if (sab > 0 && sbc > 0 && sca > 0) {
                    interiors.computeIfAbsent(id, (key) -> new IntList()).add(addVertex(x, y, interpolate(a, b, c, x, y)));
                    continue;
                }
                final int u = (sab == 0) ? a : (sbc == 0) ? b : c;
                final int v = (sab == 0) ? b : (sbc == 0) ? c : a;
                final int p = before(u, v) ? u : v, q = (p == u) ? v : u;
                final double s = Maths.projectionRatio(xyz[p*3], xyz[p*3+1], xyz[q*3], xyz[q*3+1], x, y);
                final int vertex = vertexOnEdge(p, q, Maths.clamp(s, 0, 1));
                result[0] = xyz[vertex*3];
                result[1] = xyz[vertex*3+1];
            }
            return result;
        }

        /**
         * Returns the vertices inserted in the edge from p to q, ordered from p to q.
         */
        private IntList along(final int p, final int q) {
            final IntList result = new IntList();
            final List<Split> list = splits.get(Planar.edgeKey(p, q));
            if (list != null) {
                final Comparator<Split> order = Comparator.comparingDouble(Split::s);
                list.sort(before(p, q) ? order : order.reversed());
                list.forEach((split) -> result.add(split.vertex()));
            }
            return result;
        }

        // -----------------------------------------------------------------------------------------
        // Surface cut
        // -----------------------------------------------------------------------------------------

        /**
         * Removes the surface inside the polygon. Triangles crossed by the rings are triangulated again
         * with the ring segments as constraints, keeping the parts outside the polygon.
         */
        private void cutSurface() throws OperationException {
            for (int id : query(extent)) {
                final int a = triangles[id*3], b = triangles[id*3+1], c = triangles[id*3+2];
                /*
                 * A ring segment entering the triangle interior leaves it on an edge, where a vertex was
                 * inserted, or at a vertex, in which case it entered on an edge. Triangles without inserted
                 * vertices are therefore entirely on one side of the rings.
                 */
                if (interiors.containsKey(id) || splits.containsKey(Planar.edgeKey(a, b))
                        || splits.containsKey(Planar.edgeKey(b, c)) || splits.containsKey(Planar.edgeKey(c, a))) {
                    cutTriangle(id);
                } else if (Planar.inside(rings, (xyz[a*3]   + xyz[b*3]   + xyz[c*3])   / 3,
                                                (xyz[a*3+1] + xyz[b*3+1] + xyz[c*3+1]) / 3)) {
                    removeTriangle(id);
                }
            }
        }

        private void cutTriangle(final int id) throws OperationException {
            final int a = triangles[id*3], b = triangles[id*3+1], c = triangles[id*3+2];
            final IntList vertices = new IntList();
            final Map<XY,Integer> local = new HashMap<>();
            final IntList constraints = new IntList();

            //triangle edges, split at the inserted vertices
            final int[] corners = {a, b, c};
            for (int k = 0; k < 3; k++) {
                final int p = corners[k], q = corners[(k+1) % 3];
                int previous = addLocal(p, vertices, local);
                final IntList inner = along(p, q);
                inner.add(q);
                for (int i = 0; i < inner.size; i++) {
                    final int current = addLocal(inner.values[i], vertices, local);
                    constraints.add(previous, current);
                    previous = current;
                }
            }
            final IntList inside = interiors.get(id);
            for (int i = 0; inside != null && i < inside.size; i++) {
                addLocal(inside.values[i], vertices, local);
            }

            //ring segments in the triangle, as constraints if inside it, all of them as boundary
            final IntList boundary = new IntList();
            for (int i = 0, n = vertices.size; i < n; i++) {
                final IntList points = chainAt.get(key(vertices.values[i]));
                for (int j = 0; points != null && j < points.size; j++) {
                    final int k = points.values[j];
                    final Integer other = local.get(location(next[k]));
                    if (other == null) continue;
                    boundary.add(i, other);
                    final double mx = (chain[k*2]   + chain[next[k]*2])   / 2;
                    final double my = (chain[k*2+1] + chain[next[k]*2+1]) / 2;
                    if (side(a, b, mx, my) > 0 && side(b, c, mx, my) > 0 && side(c, a, mx, my) > 0) {
                        constraints.add(i, other);
                    }
                }
            }

            final double[] coords = new double[vertices.size * 2];
            for (int i = 0; i < vertices.size; i++) {
                coords[i*2]   = xyz[vertices.values[i]*3];
                coords[i*2+1] = xyz[vertices.values[i]*3+1];
            }
            final int[] parts = Planar.keep(coords, Delaunay.triangulate(coords, constraints.toArray()),
                                            boundary.toArray(), rings, false);
            removeTriangle(id);
            final boolean roof = roofs.get(id);
            for (int i = 0; i < parts.length; i += 3) {
                addTriangle(vertices.values[parts[i]], vertices.values[parts[i+1]], vertices.values[parts[i+2]], roof);
            }
        }

        private int addLocal(final int vertex, final IntList vertices, final Map<XY,Integer> local) {
            return local.computeIfAbsent(key(vertex), (k) -> {
                vertices.add(vertex);
                return vertices.size - 1;
            });
        }

        // -----------------------------------------------------------------------------------------
        // Top
        // -----------------------------------------------------------------------------------------

        /**
         * Triangulates the top on the chain points. For a hipped roof, the straight skeleton nodes are added
         * with their time and its arcs as constraints, so that each triangle is inside a single planar face.
         * The ring vertices are the skeleton nodes of time 0, they are already chain points.
         */
        private void buildTop() throws OperationException {
            final Map<XY,Integer> local = new HashMap<>();
            final DoubleList points = new DoubleList();         // x, y, time
            final IntList constraints = new IntList();
            for (int k = 0; k < next.length; k++) {
                constraints.add(addTop(location(k), 0, local, points), addTop(location(next[k]), 0, local, points));
            }
            final int[] boundary = constraints.toArray();
            double roofScale = 0;
            if (roofHeight != height) {
                final StraightSkeleton skeleton = new StraightSkeleton(polygon);
                final double[] nodes = skeleton.getNodes();
                final int[] arcs = skeleton.getArcs();
                for (int i = 0; i < arcs.length; i++) {
                    final int n = arcs[i] * 3;
                    final XY p = XY.of(nodes[n], nodes[n+1]);
                    arcs[i] = addTop(snapped.getOrDefault(p, p), nodes[n+2], local, points);
                }
                for (int i = 0; i < arcs.length; i += 2) {
                    if (arcs[i] != arcs[i+1]) constraints.add(arcs[i], arcs[i+1]);
                }
                if (skeleton.getMaximumTime() > 0) {
                    roofScale = (roofHeight - height) / skeleton.getMaximumTime();
                }
            }
            final int nbLocal = points.size / 3;
            final double[] xy = new double[nbLocal * 2];
            final double[] z = new double[nbLocal];
            final int[] topVertex = new int[nbLocal];
            for (int l = 0; l < nbLocal; l++) {
                xy[l*2]   = points.values[l*3];
                xy[l*2+1] = points.values[l*3+1];
                z[l] = topZ(xy[l*2], xy[l*2+1]) + roofScale * points.values[l*3+2];
                topVertex[l] = -1;
            }
            if (!withWalls) {
                //reuse the surface vertices on the rings, moved to the top elevation
                for (int id : query(extent)) {
                    for (int k = 0; k < 3; k++) {
                        final int v = triangles[id*3+k];
                        final Integer l = local.get(key(v));
                        if (l != null) {
                            xyz[v*3+2] = z[l];
                            if (topVertex[l] < 0) topVertex[l] = v;
                        }
                    }
                }
            }
            for (int l = 0; l < nbLocal; l++) {
                if (topVertex[l] < 0) topVertex[l] = addVertex(xy[l*2], xy[l*2+1], z[l]);
            }
            final int[] tops = Planar.keep(xy, Delaunay.triangulate(xy, constraints.toArray()), boundary, rings, true);
            for (int i = 0; i < tops.length; i += 3) {
                addTriangle(topVertex[tops[i]], topVertex[tops[i+1]], topVertex[tops[i+2]], true);
            }
        }

        private int addTop(final XY p, final double time, final Map<XY,Integer> local, final DoubleList points) {
            return local.computeIfAbsent(p, (k) -> {
                points.add(k.x(), k.y(), time);
                return points.size / 3 - 1;
            });
        }
    }
}
