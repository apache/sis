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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.surface.TIN;
import org.apache.sis.geometries.surface.Triangle;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.Cursor;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.Maths;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.NDRange;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vector1D;
import org.apache.sis.maths.Vector3D;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.Containers;


/**
 * A mesh geometry is a structure which mimics the natural primitives used
 * by GPUs.
 *
 * Inspired by GLTF 2.0 :
 * https://github.com/KhronosGroup/glTF/blob/master/specification/2.0/README.md#geometry
 *
 * Relation to OGC Simple Feature Access :
 * A primitive is a efficient batch of simple geometries, we view it as a GeometryCollection.
 *
 * @author Johann Sorel (Geomatys)
 */
public sealed interface MeshPrimitive extends Geometry
        permits MeshPrimitive.Abs
{

    public static final Logger LOGGER = Logger.getLogger("org.apache.sis.geometries");

    /**
     * Enumeration of GPU supported primitive types.
     */
    public static enum Type {
        POINTS,
        LINES,
        LINE_LOOP,
        LINE_STRIP,
        TRIANGLES,
        TRIANGLE_FAN,
        TRIANGLE_STRIP;
    }

    public static MeshPrimitive create(Type type) {
        switch (type) {
            case POINTS: return new Points();
            case LINES: return new Lines();
            case LINE_LOOP: return new LineLoop();
            case LINE_STRIP: return new LineStrip();
            case TRIANGLES: return new Triangles();
            case TRIANGLE_FAN: return new TriangleFan();
            case TRIANGLE_STRIP: return new TriangleStrip();
            default: throw new IllegalArgumentException("Unknown type " + type);
        }
    }

    public static MeshPrimitive createEmpty(Type type, DataPointsType attDef) {
        Abs p = (Abs) create(type);
        for (String name : attDef.getAttributeNames()) {
            p.attributes.put(name, NDArrays.of(attDef.getAttributeSystem(name), attDef.getAttributeType(name), 0));
        }
        p.positions = p.attributes.get(DataPointsType.ATT_POSITION);
        ArgumentChecks.ensureNonNull(DataPointsType.ATT_POSITION, p.positions);
        return p;
    }


    /**
     * @return primitive (and positions) CRS.
     */
    @Override
    CoordinateReferenceSystem getCoordinateReferenceSystem();

    /**
     * Change primitive CRS, this will modify the position Record type.
     */
    @Override
    void setCoordinateReferenceSystem(CoordinateReferenceSystem crs);

    /**
     * Calculate envelope of this primitive.
     *
     * @return Envelope of the mesh
     */
    @Override
    Envelope getEnvelope();

    /**
     * @return OpenGL primtive type
     */
    Type getType();

    /**
     * Primitive is empty if :
     * - index is null and position array is empty
     * - index is not null and is empty
     */
    @Override
    boolean isEmpty();

    /**
     * Returns tuple array for given name.
     *
     * @param name seached attribute name
     * @return attribute array or null.
     */
    Array getAttribute(String name);

    /**
     * @param name attribute name
     * @param array if null, will remove the attribute
     */
    void setAttribute(String name, Array array);

    /**
     * Get geometry index.
     * The index combined with the index range define the different primitives.
     * If index is null all points in the geometry are used.
     *
     * @return vertex indices, may be null
     */
    Array getIndex();

    /**
     * Set geometry index.
     * If index is null all points in the geometry are used.
     *
     * @param index can be null.
     */
    void setIndex(Array index);

    /**
     * @return standard positions attribute
     */
    Array getPositions();

    /**
     * Change positions.
     * Caution : changing the positions will change the primitive CRS,
     *           the positions crs is used as primitive CRS.
     * @param array not null
     */
    void setPositions(Array array);

    /**
     * @return standard normals attribute
     */
    Array getNormals();

    void setNormals(Array array);

    /**
     * @return standard tangents attribute
     */
    Array getTangents();

    void setTangents(Array array);

    /**
     * @return standard indexed color attribute
     */
    Array getColors(int index);

    void setColors(int index, Array array);

    /**
     * @return standard indexed texture coordinate attribute
     */
    Array getTexCoords(int index);

    void setTexCoords(int index, Array array);

    /**
     * @return standard indexed joints attribute
     */
    Array getJoints(int index);

    void setJoints(int index, Array array);

    /**
     * @return standard indexed weights attribute
     */
    Array getWeights(int index);

    void setWeights(int index, Array array);

    /**
     * Check geometry definition.
     */
    void validate();

    /**
     * Get vertex at index.
     *
     * @param index vertex index.
     * @return Vertex
     */
    Vertex getVertex(int index);

    /**
     * Create a deep copy of this primitive.
     *
     * Note : this method will clone all attributes.
     *
     * @return copied primitive
     */
    MeshPrimitive deepCopy();

    /**
     * Set each vertex normal by computing the normal of the triangle
     * where it is used.
     * This method should not be used if vertices are shared by multiple
     * triangles or it will cause visual glitches.
     */
    void computeFaceNormals();

    /**
     * Set each vertex normal by computing the normal of the triangle
     * where it is used.
     */
    void computeSmoothNormals();


    /**
     * Removes any duplicated vertice.
     * Compare operation is based on vertice position.
     *
     * Only the first duplicated vertex attributes are preserved.
     */
    void removeDuplicatesByPosition();

    public static abstract sealed class Abs implements MeshPrimitive, DataPointsType
            permits Points,
                    Lines,
                    LineLoop,
                    LineStrip,
                    Triangles,
                    TriangleFan,
                    TriangleStrip
    {

        /**
         * Checks Array change for position is in the same crs as the geometry.
         */
        protected final LinkedHashMap<String,Array> attributes = new LinkedHashMap<>();
        protected Array index;
        protected final Type type;
        //keep it as variable for fast access, used a lot
        protected Array positions;

        private Map<String, Object> userProperties;

        protected Abs(Type type) {
            positions = NDArrays.of(SampleSystem.of(Geometries.RIGHT_HAND_3D), new double[0]);
            attributes.put(DataPointsType.ATT_POSITION, positions);
            this.type = type;
        }

        /**
         * @return primitive (and positions) CRS.
         */
        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return getPositions().getCoordinateReferenceSystem();
        }

        /**
         * Change primitive CRS, this will modify the position Record type.
         */
        @Override
        public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) {
            getPositions().setSampleSystem(SampleSystem.of(crs));
        }

        @Override
        public DataPointsType getDataPointsType() {
            return this;
        }

        @Override
        public List<String> getAttributeNames() {
            return Containers.copyToImmutableList(attributes.keySet(), String.class);
        }

        @Override
        public SampleSystem getAttributeSystem(String name) {
            final Array ta = attributes.get(name);
            if (ta == null) return null;
            return ta.getSampleSystem();
        }

        @Override
        public DataType getAttributeType(String name) {
            final Array ta = attributes.get(name);
            if (ta == null) return null;
            return ta.getDataType();
        }

        /**
         * Calculate envelope of this primitive.
         *
         * @return Envelope of the mesh
         */
        @Override
        public Envelope getEnvelope() {
            final Array positions = getAttribute(ATT_POSITION);
            if (positions.isEmpty()) {
                final GeneralEnvelope env = new GeneralEnvelope(getCoordinateReferenceSystem());
                env.setToNaN();
                return env;
            }
            return NDArrays.computeRange(positions);
        }

        @Override
        public Geometry getBoundary() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public List<Geometry> getComponents() {
            final List<Geometry> elements = new ArrayList<>();
            MeshPrimitiveVisitor visitor = new MeshPrimitiveVisitor(this) {
                @Override
                protected void visit(MeshPrimitive.Vertex vertex) {
                }

                @Override
                protected void visit(LineString candidate) {
                    elements.add(candidate);
                }

                @Override
                protected void visit(Point candidate) {
                    elements.add(candidate);
                }

                @Override
                protected void visit(Triangle candidate) {
                    elements.add(candidate);
                }
            };
            visitor.visit();
            return elements;
        }

        /**
         * @return OpenGL primtive type
         */
        @Override
        public Type getType() {
            return type;
        }

        /**
         * Primitive is empty if :
         * - index is null and position array is empty
         * - index is not null and is empty
         */
        @Override
        public boolean isEmpty() {
            if (index != null) {
                return index.isEmpty();
            }

            final Array positions = getPositions();
            return positions == null || positions.isEmpty();
        }

        /**
         * Returns tuple array for given name.
         *
         * @param name seached attribute name
         * @return attribute array or null.
         */
        @Override
        public Array getAttribute(String name) {
            return attributes.get(name);
        }

        /**
         * @param name attribute name
         * @param array if null, will remove the attribute
         */
        @Override
        public void setAttribute(String name, Array array) {
            if (ATT_POSITION.equals(name)) {
                //makes extract verifications
                setPositions(array);
            } else if (array == null) {
                attributes.remove(name);
            } else {
                attributes.put(name, array);
            }
        }

        /**
         * Get geometry index.
         * The index combined with the index range define the different primitives.
         *
         * @return vertex indices, may be null
         */
        @Override
        public Array getIndex() {
            return index;
        }

        /**
         * Set geometry index.
         *
         * @param index can be null.
         */
        @Override
        public void setIndex(Array index) {
            if (index != null) {
                //verify type
                DataType dataType = index.getDataType();
                switch (dataType) {
                    case BYTE :
                    case SHORT :
                    case INT :
                    case LONG :
                    case FLOAT :
                    case DOUBLE :
                        throw new IllegalArgumentException("Index must be of positive integer types.");
                }
                if (index.getDimension() != 1) {
                    throw new IllegalArgumentException("Index dimensions must be 1.");
                }
                if (index.getCoordinateReferenceSystem() != null) {
                    throw new IllegalArgumentException("Index array must no have a CRS");
                }
            }
            this.index = index;
        }

        /**
         * @return standard positions attribute
         */
        @Override
        public Array getPositions() {
            return positions;
        }

        /**
         * Returns the positions this primitive draws, in the order it draws them. When the
         * primitive carries an index, the positions appear in the order that index names them,
         * and a position the index names several times appears as many times.
         */
        @Override
        public DataPoints getDataPoints() {
            final int[] indices;
            if (index == null) {
                indices = new int[Math.toIntExact(getPositions().getLength())];
                for (int i = 0; i < indices.length; i++) indices[i] = i;
            } else {
                indices = index.toArrayInt();
            }
            return new Sequence(this, indices);
        }

        /**
         * Change positions.
         * Caution : changing the positions will change the primitive CRS,
         *           the positions crs is used as primitive CRS.
         * @param array not null
         */
        @Override
        public void setPositions(Array array) {
            ArgumentChecks.ensureNonNull(ATT_POSITION, array);
            ArgumentChecks.ensureNonNull("positions crs", array.getCoordinateReferenceSystem());
            this.positions = array;
            attributes.put(ATT_POSITION, array);
        }

        /**
         * @return standard normals attribute
         */
        @Override
        public Array getNormals() {
            return getAttribute(ATT_NORMAL);
        }

        @Override
        public void setNormals(Array array) {
            attributes.put(ATT_NORMAL, array);
        }

        /**
         * @return standard tangents attribute
         */
        @Override
        public Array getTangents() {
            return getAttribute(ATT_TANGENT);
        }

        @Override
        public void setTangents(Array array) {
            attributes.put(ATT_TANGENT, array);
        }

        /**
         * @return standard indexed color attribute
         */
        @Override
        public Array getColors(int index) {
            return getAttribute(ATT_COLOR + "_" + index);
        }

        @Override
        public void setColors(int index, Array array) {
            attributes.put(ATT_COLOR + "_" + index, array);
        }

        /**
         * @return standard indexed texture coordinate attribute
         */
        @Override
        public Array getTexCoords(int index) {
            return getAttribute(ATT_TEXCOORD + "_" + index);
        }

        @Override
        public void setTexCoords(int index, Array array) {
            attributes.put(ATT_TEXCOORD + "_" + index, array);
        }

        /**
         * @return standard indexed joints attribute
         */
        @Override
        public Array getJoints(int index) {
            return getAttribute(ATT_JOINTS + "_" + index);
        }

        @Override
        public void setJoints(int index, Array array) {
            attributes.put(ATT_JOINTS + "_" + index, array);
        }

        /**
         * @return standard indexed weights attribute
         */
        @Override
        public Array getWeights(int index) {
            return getAttribute(ATT_WEIGHTS + "_" + index);
        }

        @Override
        public void setWeights(int index, Array array) {
            attributes.put(ATT_WEIGHTS + "_" + index, array);
        }

        /**
         * Check geometry definition.
         */
        @Override
        public void validate() {
            Array positions = getPositions();
            if (positions == null) {
                throw new IllegalArgumentException("Positions attribute is undefined");
            }

            long size = -1;
            for (Map.Entry<String,Array> entry : attributes.entrySet()) {
                validate(entry.getKey(), entry.getValue());
                if (size == -1) {
                    size = entry.getValue().getLength();
                } else if (size != entry.getValue().getLength()) {
                    throw new IllegalArgumentException("Attributes have different tuple array lengths.");
                }
            }
            if (index != null) {
                validate("index", index);

                //check no index value is greater the array size
                final Set<Long> usedIndexes = new HashSet<>();
                final Cursor cursor = index.cursor();
                while (cursor.next()) {
                    long i = (long) cursor.samples().get(0);
                    if (i < 0 || i >= size) throw new IllegalArgumentException("Index " + i + " is not in range 0:"+size);
                    usedIndexes.add(i);
                }

                final List<Long> unusedIndex = new ArrayList<>();
                for (long i = 0, n = positions.getLength(); i < n; i++) {
                    if (!usedIndexes.contains(i)) {
                        unusedIndex.add(i);
                    }
                }
                if (!unusedIndex.isEmpty()) {
                    //may not be a bug, tuple arrays may be shared by multiple primitives
                    LOGGER.log(Level.INFO, "Unused indexes {0}", Arrays.toString(unusedIndex.toArray()));
                }
            }

            //check index size type
            final long nbEntries;
            if (index != null) {
                nbEntries = index.getLength();
            } else {
                nbEntries = positions.getLength();
            }
            switch (type) {
                case POINTS :
                    if (nbEntries < 1) throw new IllegalArgumentException("Points index range should have a length of at least one");
                    break;
                case LINES :
                    if (nbEntries < 2) throw new IllegalArgumentException("Lines index range should have a length of at least two");
                    if ((nbEntries % 2) != 0) throw new IllegalArgumentException("Lines index range must be a factor of 2");
                    break;
                case LINE_STRIP :
                    if (nbEntries < 2) throw new IllegalArgumentException("Line strip index range should have a length of at least two");
                    break;
                case TRIANGLES :
                    if (nbEntries < 3) throw new IllegalArgumentException("Triangles index range should have a length of at least three");
                    if ((nbEntries % 3) != 0) throw new IllegalArgumentException("Triangles index range must be a factor of 3");
                    break;
                case TRIANGLE_FAN :
                    if (nbEntries < 3) throw new IllegalArgumentException("Triangle fan index range should have a length of at least three");
                    break;
                case TRIANGLE_STRIP :
                    if (nbEntries < 3) throw new IllegalArgumentException("Triangle strip index range should have a length of at least three");
                    break;
                default : //do nothing
                    break;
            }

            //check normals
            final Array normals = getNormals();
            if (normals != null) {
                for (long i = 0, n = normals.getLength(); i < n; i++) {
                    Vector normal = normals.get(i);
                    if (!normal.isFinite()) {
                        throw new IllegalArgumentException("Normal " + i + " is not finite. " + normal);
                    } else if ( Math.abs(normal.length() - 1.0) > 1e-6) {
                        throw new IllegalArgumentException("Normal " + i + " is not unitary. " + normal);
                    }
                }
            }

        }

        private void validate(String name, Array ta) {

            final int dimension = ta.getDimension();
            final long length = ta.getLength();
            final Cursor cursor = ta.cursor();
            final Vector tuple = cursor.samples();

            for (int i = 0; i <length; i++) {
                cursor.moveTo(i);
                if (!tuple.isFinite()) {
                    throw new IllegalArgumentException("Found a NaN on attribute " + name +" at index "+ i);
                }
            }
        }

        /**
         * Get vertex at index.
         *
         * @param index vertex index.
         * @return Vertex
         */
        @Override
        public Vertex getVertex(int index) {
            return new Vertex(this, index);
        }

        /**
         * Create a deep copy of this primitive.
         *
         * Note : this method will clone all attributes.
         *
         * @return copied primitive
         */
        @Override
        public MeshPrimitive deepCopy() {
            final Abs copy = (Abs) create(type);

            for (Map.Entry<String,Array> entry : attributes.entrySet()) {
                copy.attributes.put(entry.getKey(), entry.getValue().copy());
            }

            //copy index and ranges
            if (index != null) {
                copy.setIndex(index.copy());
            }
            copy.positions = copy.attributes.get(DataPointsType.ATT_POSITION);
            return copy;
        }

        /**
         * Set each vertex normal by computing the normal of the triangle
         * where it is used.
         * This method should not be used if vertices are shared by multiple
         * triangles or it will cause visual glitches.
         */
        @Override
        public void computeFaceNormals() {
            if (Type.TRIANGLES.equals(type)) {
                /*
                 * Positions are read in double precision: geocentric coordinates are millions of meters,
                 * in float precision the edges of small triangles would be mostly rounding noise.
                 * The not normalized cross products are accumulated, so each triangle contributes
                 * in proportion of its area, and degenerated triangles contribute nothing.
                 */
                final int[] idx = MeshPrimitiveIndexes.toTriangles(getIndex(), type).toArrayInt();
                final double[] xyz = getPositions().toArrayDouble();
                final int dim = getPositions().getDimension();
                final int size = xyz.length / dim;
                final double[] sum = new double[size * 3];
                final boolean[] used = new boolean[size];
                for (int i = 0; i + 2 < idx.length; i += 3) {
                    final int a = idx[i] * dim, b = idx[i+1] * dim, c = idx[i+2] * dim;
                    final double ux = xyz[b] - xyz[a], uy = xyz[b+1] - xyz[a+1], uz = xyz[b+2] - xyz[a+2];
                    final double vx = xyz[c] - xyz[a], vy = xyz[c+1] - xyz[a+1], vz = xyz[c+2] - xyz[a+2];
                    final double nx = uy*vz - uz*vy, ny = uz*vx - ux*vz, nz = ux*vy - uy*vx;
                    for (int k = 0; k < 3; k++) used[idx[i+k]] = true;
                    if (!Double.isFinite(nx + ny + nz) || (nx == 0 && ny == 0 && nz == 0)) {
                        //degenerated triangle, it has no normal
                        continue;
                    }
                    for (int k = 0; k < 3; k++) {
                        final int v = idx[i+k] * 3;
                        sum[v] += nx; sum[v+1] += ny; sum[v+2] += nz;
                    }
                }

                //normalize normals
                final float[] normals = new float[size * 3];
                for (int v = 0; v < size; v++) {
                    final double x = sum[v*3], y = sum[v*3+1], z = sum[v*3+2];
                    final double length = Math.sqrt(x*x + y*y + z*z);
                    if (length > 0 && Double.isFinite(length)) {
                        normals[v*3]   = (float) (x / length);
                        normals[v*3+1] = (float) (y / length);
                        normals[v*3+2] = (float) (z / length);
                    } else if (used[v]) {
                        //only used by degenerated triangles, any direction is as good
                        normals[v*3] = 1;
                    }
                }
                setNormals(NDArrays.of(getPositions().getSampleSystem(), normals));
            }
        }

        /**
         * Set each vertex normal by computing the normal of the triangle
         * where it is used.
         */
        @Override
        public void computeSmoothNormals() {

            final Array positions = getAttribute(ATT_POSITION);
            final Array normals = NDArrays.of(positions.getSampleSystem(), DataType.FLOAT, positions.getLength());

            //compute smooth normals
            final Cursor posCursor = positions.cursor();

            new MeshPrimitiveVisitor(this) {
                @Override
                protected void visit(MeshPrimitive.Vertex vertex) {
                }

                @Override
                protected void visit(Triangle candidate) {
                    final DataPoints points = candidate.getExteriorRing().getDataPoints();
                    long idx0 = ((MeshPrimitive.Vertex)points.getPoint(0)).getIndex();
                    long idx1 = ((MeshPrimitive.Vertex)points.getPoint(1)).getIndex();
                    long idx2 = ((MeshPrimitive.Vertex)points.getPoint(2)).getIndex();
                    if (idx0 == idx1 || idx0 == idx2 || idx1 == idx2) {
                        //flat triangle ignore it
                        return;
                    }
                    posCursor.moveTo(idx0);
                    final Vector pos0 = new Vector3D.Double(posCursor.samples());
                    posCursor.moveTo(idx1);
                    final Vector pos1 = new Vector3D.Double(posCursor.samples());
                    posCursor.moveTo(idx2);
                    final Vector pos2 = new Vector3D.Double(posCursor.samples());

                    if (fastEquals(pos0, pos1) || fastEquals(pos0, pos2) || fastEquals(pos1, pos2)) {
                        //flat triangle ignore it
                        return;
                    }

                    final Vector normal = Maths.calculateNormal(pos0, pos1, pos2);
                    if (normal.isFinite()) {
                        //we do this check to ignore bad or degenerated triangles
                        normals.set(idx0, normals.get(idx0).add(normal));
                        normals.set(idx1, normals.get(idx1).add(normal));
                        normals.set(idx2, normals.get(idx2).add(normal));
                    }
                }

                @Override
                protected void visit(LineString candidate) {
                    final DataPoints points = candidate.getDataPoints();
                    long idx0 = ((MeshPrimitive.Vertex)points.getPoint(0)).getIndex();
                    long idx1 = ((MeshPrimitive.Vertex)points.getPoint(1)).getIndex();
                    final Vector normal = new Vector3D.Float(0,0,1);

                    normals.set(idx0, normals.get(idx0).add(normal));
                    normals.set(idx1, normals.get(idx1).add(normal));
                }

                @Override
                protected void visit(Point candidate) {
                    long idx0 = ((Vertex)candidate).getIndex();
                    final Vector normal = new Vector3D.Float(0,0,1);
                    normals.set(idx0, normal);
                }

            }.visit();

            //normalize
            for (long i = 0, n = normals.getLength(); i < n; i++) {
                final Vector nor0 = normals.get(i).normalize();
                if (nor0.isFinite()) {
                    //we do this check to ignore bad or degenerated triangles
                    normals.set(i, nor0);
                }
            }

            setNormals(normals);
        }

        private static boolean fastEquals(Vector t1, Vector t2) {
            return t1.get(0) == t2.get(0) && t1.get(1) == t2.get(1) && t1.get(2) == t2.get(2);
        }

        /**
         * Removes any duplicated vertice.
         * Compare operation is based on vertice position.
         *
         * Only the first duplicated vertex attributes are preserved.
         */
        @Override
        public void removeDuplicatesByPosition() {

            final Set<String> attNames = attributes.keySet();
            final Map<String,List<Vector<?>>> atts = new HashMap<>();
            final List<Entry<List<Vector<?>>,Array>> mapping = new ArrayList<>();
            for (String attName : attNames) {
                final List<Vector<?>> lst = new ArrayList<>();
                atts.put(attName, lst);
                mapping.add(new AbstractMap.SimpleImmutableEntry<>(lst, attributes.get(attName)));
            }
            final List<Vector<?>> aNewAttribute = mapping.get(0).getKey();

            final Map<Vector, Integer> reindex = new HashMap<>();
            final Cursor cursorIdx = index.cursor();
            final Cursor cursorPos = getPositions().cursor();
            final List<Vector1D.Int> newIndex = new ArrayList<>();

            while (cursorIdx.next()) {
                int idx = (int) cursorIdx.samples().get(0);
                cursorPos.moveTo(idx);
                final Vector position = cursorPos.samples().copy();

                int newIdx = aNewAttribute.size();
                Integer previous = reindex.putIfAbsent(position, newIdx);
                if (previous == null) {
                    //copy attributes
                    for (Entry<List<Vector<?>>,Array> entry : mapping) {
                        entry.getKey().add(entry.getValue().get(idx).copy());
                    }
                    newIndex.add(new Vector1D.Int(newIdx));
                } else {
                    newIndex.add(new Vector1D.Int(previous));
                }
            }

            setIndex(NDArrays.of(newIndex, 1, index.getDataType()));

            final Map<String, Array> newAttributes = new HashMap<>();
            for (String attName : attNames) {
                final Array model = attributes.get(attName);
                final Array array = NDArrays.of(atts.get(attName), model.getSampleSystem(), model.getDataType());
                newAttributes.put(attName, array);
            }
            attributes.clear();
            attributes.putAll(newAttributes);
            positions = attributes.get(DataPointsType.ATT_POSITION);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + Objects.hashCode(this.attributes);
            hash = 23 * hash + Objects.hashCode(this.index);
            hash = 23 * hash + Objects.hashCode(this.type);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Abs other = (Abs) obj;
            if (!Objects.equals(this.attributes, other.attributes)) {
                return false;
            }
            if (!Objects.equals(this.index, other.index)) {
                return false;
            }
            if (this.type != other.type) {
                return false;
            }
            return true;
        }

        @Override
        public synchronized Map<String, Object> userProperties() {
            if (userProperties == null) {
                userProperties = new HashMap<>();
            }
            return userProperties;
        }
    }


    /**
     * A vertex is a indexed point in a geometry.
     */
    public static final class Vertex implements Point {

        private final Abs parent;
        private long index;

        public Vertex(MeshPrimitive geometry, long index) {
            this.parent = (Abs) geometry;
            this.index = index;
        }

        @Override
        public Vector getPosition() {
            final Cursor cursor = parent.getPositions().cursor();
            cursor.moveTo(index);
            return cursor.samples();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        /**
         * @return index in the parent mesh.
         */
        public long getIndex() {
            return index;
        }

        public void setIndex(long index) {
            this.index = index;
        }

        @Override
        public Vector getAttribute(String key) {
            final Array tupleGrid = parent.attributes.get(key);
            if (tupleGrid == null) return null;
            final Cursor cursor = tupleGrid.cursor();
            cursor.moveTo(index);
            return cursor.samples();
        }

        @Override
        public void setAttribute(String name, Vector tuple) {
            final Array tupleGrid = parent.attributes.get(name);
            if (tupleGrid == null) throw new IllegalArgumentException("Attribute " + name + " do not exist");
            final Cursor cursor = tupleGrid.cursor();
            cursor.moveTo(index);
            cursor.set(tuple);
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return parent.getCoordinateReferenceSystem();
        }

        @Override
        public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public DataPointsType getDataPointsType() {
            return parent.getDataPointsType();
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("V:");
            sb.append(getIndex());
            final TreeSet<String> properties = new TreeSet<>(getDataPointsType().getAttributeNames());
            for (String name : properties) {
                sb.append(" ");
                sb.append(name);
                final Vector tuple = getAttribute(name);
                sb.append(Arrays.toString(tuple.toArrayDouble()));
            }
            return sb.toString();
        }
    }

    public static final class Sequence implements DataPoints {

        private final Abs primitive;
        public final int[] index;

        public Sequence(MeshPrimitive primitive, int[] index) {
            this.primitive = (Abs) primitive;
            this.index = index;
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return primitive.getCoordinateReferenceSystem();
        }

        @Override
        public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public int size() {
            return index.length;
        }

        @Override
        public Point getPoint(int index) {
            return new Vertex(primitive, this.index[index]);
        }

        @Override
        public Vector getPosition(int index) {
            return primitive.getPositions().get(this.index[index]);
        }

        @Override
        public Vector getAttribute(int index, String name) {
            return primitive.getAttribute(name).get(this.index[index]);
        }

        @Override
        public DataPointsType getType() {
            return primitive.getDataPointsType();
        }

        @Override
        public void setPosition(int index, Vector value) {
            primitive.getPositions().set(this.index[index], value);
        }

        @Override
        public void setAttribute(int index, String name, Vector value) {
            primitive.getAttribute(name).set(this.index[index], value);
        }

        @Override
        public NDRange getAttributeRange(String name) {
            return NDArrays.computeRange(NDArrays.subset(primitive.getAttribute(name),index));
        }
    }

    public static final class Points extends Abs implements GeometryCollection<Point>{
        public Points() {
            super(Type.POINTS);
        }

        @Override
        public GeometryType getGeometryType() {
            return GeometryType.COLLECTION;
        }

        @Override
        public GeometryType getElementType() {
            return GeometryType.POINT;
        }

        @Override
        public int getNumGeometries() {
            long nb = (index == null) ? getPositions().getLength() : this.index.getLength();
            return Math.toIntExact(nb);
        }

        @Override
        public Point getGeometryN(int n) {
            final int[] indices = (index == null) ? new int[]{n} : index.toArrayInt(n, 1);
            return new Sequence(this, indices).getPoint(0);
        }

    }

    public static final class Lines extends Abs implements GeometryCollection<LineString> {
        public Lines() {
            super(Type.LINES);
        }

        @Override
        public GeometryType getElementType() {
            return GeometryType.LINESTRING;
        }

        @Override
        public int getNumGeometries() {
            long nb = (index == null) ? getPositions().getLength() : this.index.getLength();
            return Math.toIntExact(nb / 2);
        }

        @Override
        public LineString getGeometryN(int n) {
            final int[] indices = (index == null) ? new int[]{n*2, n*2+1} : index.toArrayInt(n*2, 2);
            return GeometryFactory.DEFAULT.createLineString(new Sequence(this, indices));
        }

    }

    public static final class LineLoop extends Abs implements LineString {
        public LineLoop() {
            super(Type.LINE_LOOP);
        }

        @Override
        public DataPoints getDataPoints() {
            //select all points, duplicate first point as last
            int[] indices;
            if (index == null) {
                indices = new int[Math.toIntExact(getPositions().getLength() + 1)];
                for (int i = 0; i < indices.length; i++) indices[i] = i;
                indices[indices.length - 1] = 0;
            } else {
                indices = index.toArrayInt();
                indices = Arrays.copyOf(indices, indices.length + 1);
                indices[indices.length - 1] = indices[0];
            }
            return new Sequence(this, indices);
        }
    }

    public static final class LineStrip extends Abs implements LineString {
        public LineStrip() {
            super(Type.LINE_STRIP);
        }

    }

    public static final class Triangles extends Abs implements TIN {
        public Triangles() {
            super(Type.TRIANGLES);
        }

        @Override
        public int getNumPatches() {
            long nb = (index == null) ? getPositions().getLength() : this.index.getLength();
            return Math.toIntExact(nb / 3);
        }

        @Override
        public Triangle getPatchN(int n) {
            int[] indices;
            if (index == null) {
                int o = n*3;
                indices = new int[]{o, o+1, o+2, o};
            } else {
                indices = index.toArrayInt(n*3, 3);
                indices = Arrays.copyOf(indices, 4);
                indices[3] = indices[0];
            }
            return GeometryFactory.DEFAULT.createTriangle(GeometryFactory.DEFAULT.createLinearRing(new Sequence(this, indices)));
        }
    }

    public static final class TriangleFan extends Abs implements TIN {
        public TriangleFan() {
            super(Type.TRIANGLE_FAN);
        }

        @Override
        public int getNumPatches() {
            long nb = (index == null) ? getPositions().getLength() : this.index.getLength();
            return Math.toIntExact(nb - 2);
        }

        @Override
        public Triangle getPatchN(int n) {
            final int[] indices = new int[4];
            if (index == null) {
                indices[0] = 0;
                indices[1] = 1 + n;
                indices[2] = 2 + n;
            } else {
                indices[0] = (int) index.get(0    ).get(0);
                indices[1] = (int) index.get(1 + n).get(0);
                indices[2] = (int) index.get(2 + n).get(0);
            }
                indices[3] = indices[0];
            return GeometryFactory.DEFAULT.createTriangle(GeometryFactory.DEFAULT.createLinearRing(new Sequence(this, indices)));
        }
    }

    public static final class TriangleStrip extends Abs implements TIN {
        public TriangleStrip() {
            super(Type.TRIANGLE_STRIP);
        }

        @Override
        public int getNumPatches() {
            long nb = (index == null) ? getPositions().getLength() : this.index.getLength();
            return Math.toIntExact(nb - 2);
        }

        @Override
        public Triangle getPatchN(int n) {
            final int[] indices = new int[4];
            if (index == null) {
                indices[0] = n    ;
                indices[1] = n + 1;
                indices[2] = n + 2;
            } else {
                indices[0] = (int) index.get(n    ).get(0);
                indices[1] = (int) index.get(n + 1).get(0);
                indices[2] = (int) index.get(n + 2).get(0);
            }

            //in triangle strip we must take care to reverse index
            //at every new point, otherwise we would have reverse winding
            //for each triangle
            if (n % 2 != 0) {
                int t = indices[0];
                indices[0] = indices[1];
                indices[1] = t;
            }
            indices[3] = indices[0];

            return GeometryFactory.DEFAULT.createTriangle(GeometryFactory.DEFAULT.createLinearRing(new Sequence(this, indices)));
        }
    }
}
