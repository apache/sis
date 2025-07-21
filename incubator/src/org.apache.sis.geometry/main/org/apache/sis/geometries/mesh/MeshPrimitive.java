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
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.BBox;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.LineString;
import org.apache.sis.geometries.MultiLineString;
import org.apache.sis.geometries.MultiPoint;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.TIN;
import org.apache.sis.geometries.Triangle;
import org.apache.sis.geometries.math.AbstractTupleArray;
import org.apache.sis.geometries.math.DataType;
import org.apache.sis.geometries.math.Maths;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.TupleArray;
import org.apache.sis.geometries.math.TupleArrayCursor;
import org.apache.sis.geometries.math.TupleArrays;
import org.apache.sis.geometries.math.Vector;
import org.apache.sis.geometries.math.Vector1D;
import org.apache.sis.geometries.math.Vector3D;
import org.apache.sis.geometries.math.Vectors;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.privy.UnmodifiableArrayList;


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
public interface MeshPrimitive extends Geometry {

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

    public static MeshPrimitive createEmpty(Type type, AttributesType attDef) {
        Abs p = (Abs) create(type);
        for (String name : attDef.getAttributeNames()) {
            p.attributes.put(name, TupleArrays.of(attDef.getAttributeSystem(name), attDef.getAttributeType(name), 0));
        }
        p.positions = p.attributes.get(AttributesType.ATT_POSITION);
        ArgumentChecks.ensureNonNull(AttributesType.ATT_POSITION, p.positions);
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
    TupleArray getAttribute(String name);

    /**
     * @param name attribute name
     * @param array if null, will remove the attribute
     */
    void setAttribute(String name, TupleArray array);

    /**
     * Get geometry index.
     * The index combined with the index range define the different primitives.
     * If index is null all points in the geometry are used.
     *
     * @return vertex indices, may be null
     */
    TupleArray getIndex();

    /**
     * Set geometry index.
     * If index is null all points in the geometry are used.
     *
     * @param index can be null.
     */
    void setIndex(TupleArray index);

    /**
     * @return standard positions attribute
     */
    TupleArray getPositions();

    /**
     * Change positions.
     * Caution : changing the positions will change the primitive CRS,
     *           the positions crs is used as primitive CRS.
     * @param array not null
     */
    void setPositions(TupleArray array);

    /**
     * @return standard normals attribute
     */
    TupleArray getNormals();

    void setNormals(TupleArray array);

    /**
     * @return standard tangents attribute
     */
    TupleArray getTangents();

    void setTangents(TupleArray array);

    /**
     * @return standard indexed color attribute
     */
    TupleArray getColors(int index);

    void setColors(int index, TupleArray array);

    /**
     * @return standard indexed texture coordinate attribute
     */
    TupleArray getTexCoords(int index);

    void setTexCoords(int index, TupleArray array);

    /**
     * @return standard indexed joints attribute
     */
    TupleArray getJoints(int index);

    void setJoints(int index, TupleArray array);

    /**
     * @return standard indexed weights attribute
     */
    TupleArray getWeights(int index);

    void setWeights(int index, TupleArray array);

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

    public static abstract class Abs implements MeshPrimitive, AttributesType {

        /**
         * Checks tuplearray change for position is in the same crs as the geometry.
         */
        protected final LinkedHashMap<String,TupleArray> attributes = new LinkedHashMap<>();
        protected TupleArray index;
        protected final Type type;
        //keep it as variable for fast access, used a lot
        protected TupleArray positions;

        private Map<String, Object> userProperties;

        protected Abs(Type type) {
            positions = TupleArrays.of(SampleSystem.of(Geometries.RIGHT_HAND_3D), new double[0]);
            attributes.put(AttributesType.ATT_POSITION, positions);
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
        public AttributesType getAttributesType() {
            return this;
        }

        @Override
        public List<String> getAttributeNames() {
            return UnmodifiableArrayList.wrap(attributes.keySet().toArray(new String[0]));
        }

        @Override
        public SampleSystem getAttributeSystem(String name) {
            final TupleArray ta = attributes.get(name);
            if (ta == null) return null;
            return ta.getSampleSystem();
        }

        @Override
        public DataType getAttributeType(String name) {
            final TupleArray ta = attributes.get(name);
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
            final TupleArray positions = getAttribute(ATT_POSITION);
            if (positions.isEmpty()) {
                final GeneralEnvelope env = new GeneralEnvelope(getCoordinateReferenceSystem());
                env.setToNaN();
                return env;
            }
            return TupleArrays.computeRange(positions);
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

            final TupleArray positions = getPositions();
            return positions == null || positions.isEmpty();
        }

        /**
         * Returns tuple array for given name.
         *
         * @param name seached attribute name
         * @return attribute array or null.
         */
        @Override
        public TupleArray getAttribute(String name) {
            return attributes.get(name);
        }

        /**
         * @param name attribute name
         * @param array if null, will remove the attribute
         */
        @Override
        public void setAttribute(String name, TupleArray array) {
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
        public TupleArray getIndex() {
            return index;
        }

        /**
         * Set geometry index.
         *
         * @param index can be null.
         */
        @Override
        public void setIndex(TupleArray index) {
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
        public TupleArray getPositions() {
            return positions;
        }

        /**
         * Change positions.
         * Caution : changing the positions will change the primitive CRS,
         *           the positions crs is used as primitive CRS.
         * @param array not null
         */
        @Override
        public void setPositions(TupleArray array) {
            ArgumentChecks.ensureNonNull(ATT_POSITION, array);
            ArgumentChecks.ensureNonNull("positions crs", array.getCoordinateReferenceSystem());
            this.positions = array;
            attributes.put(ATT_POSITION, array);
        }

        /**
         * @return standard normals attribute
         */
        @Override
        public TupleArray getNormals() {
            return getAttribute(ATT_NORMAL);
        }

        @Override
        public void setNormals(TupleArray array) {
            attributes.put(ATT_NORMAL, array);
        }

        /**
         * @return standard tangents attribute
         */
        @Override
        public TupleArray getTangents() {
            return getAttribute(ATT_TANGENT);
        }

        @Override
        public void setTangents(TupleArray array) {
            attributes.put(ATT_TANGENT, array);
        }

        /**
         * @return standard indexed color attribute
         */
        @Override
        public TupleArray getColors(int index) {
            return getAttribute(ATT_COLOR + "_" + index);
        }

        @Override
        public void setColors(int index, TupleArray array) {
            attributes.put(ATT_COLOR + "_" + index, array);
        }

        /**
         * @return standard indexed texture coordinate attribute
         */
        @Override
        public TupleArray getTexCoords(int index) {
            return getAttribute(ATT_TEXCOORD + "_" + index);
        }

        @Override
        public void setTexCoords(int index, TupleArray array) {
            attributes.put(ATT_TEXCOORD + "_" + index, array);
        }

        /**
         * @return standard indexed joints attribute
         */
        @Override
        public TupleArray getJoints(int index) {
            return getAttribute(ATT_JOINTS + "_" + index);
        }

        @Override
        public void setJoints(int index, TupleArray array) {
            attributes.put(ATT_JOINTS + "_" + index, array);
        }

        /**
         * @return standard indexed weights attribute
         */
        @Override
        public TupleArray getWeights(int index) {
            return getAttribute(ATT_WEIGHTS + "_" + index);
        }

        @Override
        public void setWeights(int index, TupleArray array) {
            attributes.put(ATT_WEIGHTS + "_" + index, array);
        }

        /**
         * Check geometry definition.
         */
        @Override
        public void validate() {
            TupleArray positions = getPositions();
            if (positions == null) {
                throw new IllegalArgumentException("Positions attribute is undefined");
            }

            int size = -1;
            for (Map.Entry<String,TupleArray> entry : attributes.entrySet()) {
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
                final Set<Integer> usedIndexes = new HashSet<>();
                final TupleArrayCursor cursor = index.cursor();
                while (cursor.next()) {
                    int i = (int) cursor.samples().get(0);
                    if (i < 0 || i >= size) throw new IllegalArgumentException("Index " + i + " is not in range 0:"+size);
                    usedIndexes.add(i);
                }

                final List<Integer> unusedIndex = new ArrayList<>();
                for (int i = 0, n = positions.getLength(); i < n; i++) {
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
            final int nbEntries;
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
            final TupleArray normals = getNormals();
            if (normals != null) {
                for (int i = 0, n = normals.getLength(); i < n; i++) {
                    Tuple normal = normals.get(i);
                    if (!normal.isFinite()) {
                        throw new IllegalArgumentException("Normal " + i + " is not finite. " + normal);
                    } else if ( Math.abs(Vectors.castOrWrap(normal).length() - 1.0) > 1e-6) {
                        throw new IllegalArgumentException("Normal " + i + " is not unitary. " + normal);
                    }
                }
            }

        }

        private void validate(String name, TupleArray ta) {

            final int dimension = ta.getDimension();
            final int length = ta.getLength();
            final Tuple tuple = Vectors.createDouble(dimension);

            for (int i = 0; i <length; i++) {
                ta.get(i, tuple);
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

            for (Map.Entry<String,TupleArray> entry : attributes.entrySet()) {
                copy.attributes.put(entry.getKey(), entry.getValue().copy());
            }

            //copy index and ranges
            if (index != null) {
                copy.setIndex(index.copy());
            }
            copy.positions = copy.attributes.get(AttributesType.ATT_POSITION);
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
                final TupleArrayCursor icursor = getIndex().cursor();
                final TupleArray vertices = getPositions();
                final TupleArrayCursor vcursor = vertices.cursor();

                final long size = vertices.getLength();
                final TupleArray normals = TupleArrays.of(positions.getSampleSystem(), new float[(int)size * 3]);
                final TupleArrayCursor ncursor = normals.cursor();
                final Vector nv = Vectors.castOrWrap(ncursor.samples());
                final Tuple v0 = Vectors.createFloat(3);
                final Tuple v1 = Vectors.createFloat(3);
                final Tuple v2 = Vectors.createFloat(3);
                Tuple normal = null;

                // accumulate normal vectors
                final int offset = 0;
                for (int i=0,n=index.getLength();i<n;i+=3) {
                    icursor.moveTo(i+offset);
                    final int idx0 = (int) icursor.samples().get(0);
                    icursor.next();
                    final int idx1 = (int) icursor.samples().get(0);
                    icursor.next();
                    final int idx2 = (int) icursor.samples().get(0);
                    vcursor.moveTo(idx0); v0.set(vcursor.samples());
                    vcursor.moveTo(idx1); v1.set(vcursor.samples());
                    vcursor.moveTo(idx2); v2.set(vcursor.samples());
                    normal = Maths.calculateNormal(v0, v1, v2);

                    //a normal may already be set at given index, we accumulate
                    //values and we will normalize them in a second loop
                    ncursor.moveTo(idx0); nv.add(normal);
                    ncursor.moveTo(idx1); nv.add(normal);
                    ncursor.moveTo(idx2); nv.add(normal);
                }

                //normalize normals
                for (int i=0,n=index.getLength();i<n;i+=3) {
                    icursor.moveTo(i+offset);
                    final int idx0 = (int) icursor.samples().get(0);
                    icursor.next();
                    final int idx1 = (int) icursor.samples().get(0);
                    icursor.next();
                    final int idx2 = (int) icursor.samples().get(0);
                    ncursor.moveTo(idx0); nv.normalize();
                    if (!nv.isFinite()) nv.set(new double[]{1,0,0});
                    ncursor.moveTo(idx1); nv.normalize();
                    if (!nv.isFinite()) nv.set(new double[]{1,0,0});
                    ncursor.moveTo(idx2); nv.normalize();
                    if (!nv.isFinite()) nv.set(new double[]{1,0,0});
                }

                setNormals(normals);
            }
        }

        /**
         * Set each vertex normal by computing the normal of the triangle
         * where it is used.
         */
        @Override
        public void computeSmoothNormals() {

            final TupleArray positions = getAttribute(ATT_POSITION);
            final TupleArray normals = TupleArrays.of(positions.getSampleSystem(), new float[positions.getLength()*3]);

            //compute smooth normals
            final Vector pos0 = Vectors.createDouble(3);
            final Vector pos1 = Vectors.createDouble(3);
            final Vector pos2 = Vectors.createDouble(3);
            final Vector nor0 = Vectors.createDouble(3);
            final Vector nor1 = Vectors.createDouble(3);
            final Vector nor2 = Vectors.createDouble(3);

            new MeshPrimitiveVisitor(this) {
                @Override
                protected void visit(MeshPrimitive.Vertex vertex) {
                }

                @Override
                protected void visit(Triangle candidate) {
                    final PointSequence points = candidate.getExteriorRing().getPoints();
                    int idx0 = ((MeshPrimitive.Vertex)points.getPoint(0)).getIndex();
                    int idx1 = ((MeshPrimitive.Vertex)points.getPoint(1)).getIndex();
                    int idx2 = ((MeshPrimitive.Vertex)points.getPoint(2)).getIndex();
                    if (idx0 == idx1 || idx0 == idx2 || idx1 == idx2) {
                        //flat triangle ignore it
                        return;
                    }
                    positions.get(idx0, pos0);
                    positions.get(idx1, pos1);
                    positions.get(idx2, pos2);

                    if (fastEquals(pos0, pos1) || fastEquals(pos0, pos2) || fastEquals(pos1, pos2)) {
                        //flat triangle ignore it
                        return;
                    }

                    final Vector normal = Maths.calculateNormal(pos0, pos1, pos2);
                    if (normal.isFinite()) {
                        //we do this check to ignore bad or degenerated triangles
                        normals.get(idx0, nor0);
                        normals.get(idx1, nor1);
                        normals.get(idx2, nor2);
                        nor0.add(normal);
                        nor1.add(normal);
                        nor2.add(normal);
                        normals.set(idx0, nor0);
                        normals.set(idx1, nor1);
                        normals.set(idx2, nor2);
                    }
                }

                @Override
                protected void visit(LineString candidate) {
                    final PointSequence points = candidate.getPoints();
                    int idx0 = ((MeshPrimitive.Vertex)points.getPoint(0)).getIndex();
                    int idx1 = ((MeshPrimitive.Vertex)points.getPoint(1)).getIndex();
                    positions.get(idx0, pos0);
                    positions.get(idx1, pos1);
                    final Vector normal = new Vector3D.Float(0,0,1);

                    normals.get(idx0, nor0);
                    normals.get(idx1, nor1);
                    nor0.add(normal);
                    nor1.add(normal);
                    normals.set(idx0, nor0);
                    normals.set(idx1, nor1);
                }

                @Override
                protected void visit(Point candidate) {
                    int idx0 = ((Vertex)candidate).getIndex();
                    final Vector normal = new Vector3D.Float(0,0,1);
                    normals.set(idx0, normal);
                }

            }.visit();

            //normalize
            for (int i = 0, n = normals.getLength(); i < n; i++) {
                normals.get(i, nor0);
                nor0.normalize();
                if (nor0.isFinite()) {
                    //we do this check to ignore bad or degenerated triangles
                    normals.set(i, nor0);
                }
            }

            setNormals(normals);
        }

        private static boolean fastEquals(Tuple t1, Tuple t2) {
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
            final Map<String,List<Tuple>> atts = new HashMap();
            final List<Entry<List<Tuple>,TupleArray>> mapping = new ArrayList<>();
            for (String attName : attNames) {
                final List<Tuple> lst = new ArrayList<>();
                atts.put(attName, lst);
                mapping.add(new AbstractMap.SimpleImmutableEntry<>(lst, attributes.get(attName)));
            }
            final List<Tuple> aNewAttribute = mapping.get(0).getKey();

            final Map<Tuple, Integer> reindex = new HashMap<>();
            final TupleArrayCursor cursorIdx = index.cursor();
            final TupleArrayCursor cursorPos = getPositions().cursor();
            final List<Vector1D.Int> newIndex = new ArrayList<>();

            while (cursorIdx.next()) {
                int idx = (int) cursorIdx.samples().get(0);
                cursorPos.moveTo(idx);
                final Tuple position = cursorPos.samples().copy();

                int newIdx = aNewAttribute.size();
                Integer previous = reindex.putIfAbsent(position, newIdx);
                if (previous == null) {
                    //copy attributes
                    for (Entry<List<Tuple>,TupleArray> entry : mapping) {
                        entry.getKey().add(entry.getValue().get(idx).copy());
                    }
                    newIndex.add(new Vector1D.Int(newIdx));
                } else {
                    newIndex.add(new Vector1D.Int(previous));
                }
            }

            setIndex(TupleArrays.of(newIndex, 1, index.getDataType()));

            final Map<String, TupleArray> newAttributes = new HashMap();
            for (String attName : attNames) {
                final TupleArray model = attributes.get(attName);
                final TupleArray array = TupleArrays.of(atts.get(attName), model.getSampleSystem(), model.getDataType());
                newAttributes.put(attName, array);
            }
            attributes.clear();
            attributes.putAll(newAttributes);
            positions = attributes.get(AttributesType.ATT_POSITION);
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
    public static class Vertex implements Point {

        private final Abs parent;
        private int index;

        public Vertex(MeshPrimitive geometry, int index) {
            this.parent = (Abs) geometry;
            this.index = index;
        }

        @Override
        public Tuple getPosition() {
            final TupleArrayCursor cursor = parent.getPositions().cursor();
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
        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        @Override
        public Tuple getAttribute(String key) {
            final TupleArray tupleGrid = parent.attributes.get(key);
            if (tupleGrid == null) return null;
            final TupleArrayCursor cursor = tupleGrid.cursor();
            cursor.moveTo(index);
            return cursor.samples();
        }

        @Override
        public void setAttribute(String name, Tuple tuple) {
            final TupleArray tupleGrid = parent.attributes.get(name);
            if (tupleGrid == null) throw new IllegalArgumentException("Attribute " + name + " do not exist");
            final TupleArrayCursor cursor = tupleGrid.cursor();
            cursor.moveTo(index);
            cursor.samples().set(tuple);
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
        public AttributesType getAttributesType() {
            return parent.getAttributesType();
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("V:");
            sb.append(getIndex());
            final TreeSet<String> properties = new TreeSet<>(getAttributesType().getAttributeNames());
            for (String name : properties) {
                sb.append(" ");
                sb.append(name);
                final Tuple tuple = getAttribute(name);
                sb.append(Arrays.toString(tuple.toArrayDouble()));
            }
            return sb.toString();
        }
    }

    public static final class Sequence implements PointSequence {

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
        public Tuple getPosition(int index) {
            return primitive.getPositions().get(this.index[index]);
        }

        @Override
        public Tuple getAttribute(int index, String name) {
            return primitive.getAttribute(name).get(this.index[index]);
        }

        @Override
        public AttributesType getAttributesType() {
            return primitive.getAttributesType();
        }

        @Override
        public void setPosition(int index, Tuple value) {
            primitive.getPositions().set(this.index[index], value);
        }

        @Override
        public void setAttribute(int index, String name, Tuple value) {
            primitive.getAttribute(name).set(this.index[index], value);
        }

        @Override
        public BBox getAttributeRange(String name) {
            return TupleArrays.computeRange(TupleArrays.subset(primitive.getAttribute(name),index));
        }
    }

    public static class Points extends Abs implements MultiPoint<Point>{
        public Points() {
            super(Type.POINTS);
        }

        @Override
        public String getGeometryType() {
            return "MULTIPOINT";
        }

        @Override
        public int getNumGeometries() {
            if (index == null) return getPositions().getLength();
            return this.index.getLength();
        }

        @Override
        public Point getGeometryN(int n) {
            final int[] indices = (index == null) ? new int[]{n} : index.toArrayInt(n, 1);
            return new Sequence(this, indices).getPoint(0);
        }
    }

    public static class Lines extends Abs implements MultiLineString {
        public Lines() {
            super(Type.LINES);
        }

        @Override
        public int getNumGeometries() {
            if (index == null) return getPositions().getLength() / 2;
            return index.getLength() / 2;
        }

        @Override
        public LineString getGeometryN(int n) {
            final int[] indices = (index == null) ? new int[]{n*2, n*2+1} : index.toArrayInt(n*2, 2);
            return GeometryFactory.createLineString(new Sequence(this, indices));
        }
    }

    public static class LineLoop extends Abs implements LineString {
        public LineLoop() {
            super(Type.LINE_LOOP);
        }

        @Override
        public PointSequence getPoints() {
            //select all points, duplicate first point as last
            int[] indices;
            if (index == null) {
                indices = new int[getPositions().getLength() + 1];
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

    public static class LineStrip extends Abs implements LineString {
        public LineStrip() {
            super(Type.LINE_STRIP);
        }

        @Override
        public PointSequence getPoints() {
            final int[] indices;
            if (index == null) {
                indices = new int[getPositions().getLength()];
                for (int i = 0; i < indices.length; i++) indices[i] = i;
            } else {
                indices = index.toArrayInt();
            }
            return new Sequence(this, indices);
        }
    }

    public static class Triangles extends Abs implements TIN {
        public Triangles() {
            super(Type.TRIANGLES);
        }

        @Override
        public int getNumPatches() {
            if (index == null) return getPositions().getLength() / 3;
            return index.getLength() / 3;
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
            return GeometryFactory.createTriangle(GeometryFactory.createLinearRing(new Sequence(this, indices)));
        }
    }

    public static class TriangleFan extends Abs implements TIN {
        public TriangleFan() {
            super(Type.TRIANGLE_FAN);
        }

        @Override
        public int getNumPatches() {
            if (index == null) return getPositions().getLength() - 2;
            return index.getLength() - 2;
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
            return GeometryFactory.createTriangle(GeometryFactory.createLinearRing(new Sequence(this, indices)));
        }
    }

    public static class TriangleStrip extends Abs implements TIN {
        public TriangleStrip() {
            super(Type.TRIANGLE_STRIP);
        }

        @Override
        public int getNumPatches() {
            if (index == null) return getPositions().getLength() - 2;
            return index.getLength() - 2;
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

            return GeometryFactory.createTriangle(GeometryFactory.createLinearRing(new Sequence(this, indices)));
        }
    }
}
