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
package org.apache.sis.geometries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import javax.measure.Unit;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.IdentifiedObject;
import static org.opengis.referencing.IdentifiedObject.ALIAS_KEY;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.datum.EngineeringDatum;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.apache.sis.geometries.math.DataType;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.TupleArray;
import org.apache.sis.geometries.math.TupleArrayCursor;
import org.apache.sis.geometries.math.TupleArrays;
import org.apache.sis.geometries.math.Vector;
import org.apache.sis.geometries.math.Vector3D;
import org.apache.sis.geometries.math.Vectors;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.mesh.MultiMeshPrimitive;
import org.apache.sis.geometries.internal.shared.ArraySequence;
import org.apache.sis.geometry.wrapper.jts.JTS;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.DefaultEngineeringCRS;
import org.apache.sis.referencing.cs.DefaultCartesianCS;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;
import org.apache.sis.referencing.cs.DefaultLinearCS;
import org.apache.sis.referencing.datum.DefaultEngineeringDatum;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.SimpleInternationalString;


/**
 * Mesh geometry utilities.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Geometries {

    private static final CoordinateReferenceSystem UNDEFINED_CRS_1D = createUndefined(1);
    private static final CoordinateReferenceSystem UNDEFINED_CRS_2D = createUndefined(2);
    private static final CoordinateReferenceSystem UNDEFINED_CRS_3D = createUndefined(3);
    private static final CoordinateReferenceSystem UNDEFINED_CRS_4D = createUndefined(4);
    private static CoordinateReferenceSystem[] UNDEFINED = new CoordinateReferenceSystem[0];

    private static final DefaultEngineeringDatum DATUM;
    static {
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(NAME_KEY,  "Display");
        properties.put(ALIAS_KEY, "Display");
        DATUM = new DefaultEngineeringDatum(properties);
    }

    public static final AxisDirection UP = AxisDirection.valueOf("UP");
    public static final AxisDirection DOWN = AxisDirection.valueOf("DOWN");
    public static final AxisDirection LEFT = AxisDirections.valueOf("WEST"); //TODO fix me when SIS has the appropriate axis
    public static final AxisDirection RIGHT = AxisDirection.valueOf("EAST"); //TODO fix me when SIS has the appropriate axis
    public static final AxisDirection FORWARD = AxisDirection.valueOf("NORTH"); //TODO fix me when SIS has the appropriate axis
    public static final AxisDirection BACKWARD = AxisDirection.valueOf("SOUTH"); //TODO fix me when SIS has the appropriate axis
    public static final DefaultCoordinateSystemAxis X_RIGHT = create("Right", "x", RIGHT, Units.METRE);
    public static final DefaultCoordinateSystemAxis X_LEFT = create("Left", "x", LEFT, Units.METRE);
    public static final DefaultCoordinateSystemAxis Y_UP = create("Up", "y", UP, Units.METRE);
    public static final DefaultCoordinateSystemAxis Y_DOWN = create("Down", "y", DOWN, Units.METRE);
    public static final DefaultCoordinateSystemAxis Y_FORWARD = create("Unknown", "y", FORWARD, Units.METRE);
    public static final DefaultCoordinateSystemAxis Y_BACKWARD = create("Unknown", "y", BACKWARD, Units.METRE);
    public static final DefaultCoordinateSystemAxis Z_FORWARD = create("Unknown", "z", FORWARD, Units.METRE);
    public static final DefaultCoordinateSystemAxis Z_BACKWARD = create("Unknown", "z", BACKWARD, Units.METRE);
    public static final DefaultCoordinateSystemAxis Z_UP = create("Up", "z", UP, Units.METRE);
    public static final DefaultCoordinateSystemAxis Z_DOWN = create("Down", "z", DOWN, Units.METRE);
    /**
     * Right hand CRS, X(right), Y(up)
     */
    public static final CoordinateReferenceSystem RIGHT_HAND_2D = createCartesianCRS2D(X_RIGHT, Y_UP);
    /**
     * Right hand CRS, X(right), Y(up), Z(forward)
     */
    public static final CoordinateReferenceSystem RIGHT_HAND_3D = createCartesianCRS3D(X_LEFT, Y_UP, Z_FORWARD);
    /**
     * Left hand CRS, X(right), Y(up), Z(backward)
     */
    public static final CoordinateReferenceSystem LEFT_HAND_3D = createCartesianCRS3D(X_RIGHT, Y_UP, Z_BACKWARD);
    /**
     * Planar geographic crs with an aditional elevation axis.
     * X(right), Y(forward), Z(up)
     */
    public static final CoordinateReferenceSystem PSEUDOGEO_3D = createCartesianCRS3D(X_RIGHT, Y_FORWARD, Z_UP);

    /**
     * Get an undefined CRS whic can not be converted to any other CRS.
     * @param nbDim CRS dimension
     * @return created CRS.
     */
    public static CoordinateReferenceSystem getUndefinedCRS(int nbDim) {
        switch (nbDim) {
            case 1 : return UNDEFINED_CRS_1D;
            case 2 : return UNDEFINED_CRS_2D;
            case 3 : return UNDEFINED_CRS_3D;
            case 4 : return UNDEFINED_CRS_4D;
            default: {
                ArgumentChecks.ensureStrictlyPositive("nbDim", nbDim);
                final int idx = nbDim - 4;
                synchronized (DATUM) {
                    if (idx >= UNDEFINED.length) {
                        UNDEFINED = Arrays.copyOf(UNDEFINED, idx+1);
                    }
                    if (UNDEFINED[idx] == null) {
                        UNDEFINED[idx] = createUndefined(nbDim);
                    }
                    return UNDEFINED[idx];
                }
            }
        }
    }

    private static CoordinateReferenceSystem createUndefined(int nbDim) {
        try {
            final String name = "Undefined";
            final List<CoordinateReferenceSystem> crss = new ArrayList<>();
            for (int i=0;i<nbDim;i++) {
                final EngineeringDatum datum = new DefaultEngineeringDatum(Collections.singletonMap("name", name + i + "D Datum"));
                final CoordinateSystemAxis axis = new DefaultCoordinateSystemAxis(Collections.singletonMap("name", name + i + "Axis"),
                        name, AxisDirection.UNSPECIFIED, Units.UNITY);
                final CoordinateSystem cs = new DefaultLinearCS(Collections.singletonMap("name", name + i + "CS"), axis);
                DefaultEngineeringCRS crs = new DefaultEngineeringCRS(Collections.singletonMap("name", name), datum, cs);
                crss.add(crs);
            }
            return CRS.compound(crss.toArray(new CoordinateReferenceSystem[crss.size()]));
        } catch (FactoryException ex) {
            //should not happen
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    /**
     * Constructs an axis with a name and an abbreviation as a resource bundle key.
     * To be used for construction of pre-defined constants only.
     *
     * @param name         The name.
     * @param abbreviation The {@linkplain #getAbbreviation abbreviation} used for this
     *                     coordinate system axes.
     * @param direction    The {@linkplain #getDirection direction} of this coordinate system axis.
     * @param unit         The {@linkplain #getUnit unit of measure} used for this coordinate
     *                     system axis.
     */
    private static DefaultCoordinateSystemAxis create(final String        name,
                                                      final String        abbreviation,
                                                      final AxisDirection direction,
                                                      final Unit<?>       unit) {
        final Map<String,Object> properties = new HashMap<>(4);
        if (name.length() > 0) {
            final InternationalString n = new SimpleInternationalString(name);
            properties.put(IdentifiedObject.NAME_KEY, n.toString(null));
            properties.put(IdentifiedObject.ALIAS_KEY, n);
        } else {
            properties.put(IdentifiedObject.NAME_KEY, abbreviation);
        }
        final DefaultCoordinateSystemAxis axis = new DefaultCoordinateSystemAxis(properties, abbreviation, direction, unit);
        return axis;
    }
    /**
     * Use the unlocalized name (usually in English locale), because the name is part of the elements
     * compared by the {@link #equals} method.
     */
    private static Map<String,Object> name(final String name) {
        final Map<String,Object> properties = new HashMap<>(4);
        final InternationalString sname = new SimpleInternationalString(name);
        properties.put(NAME_KEY,  sname.toString(Locale.ROOT));
        properties.put(ALIAS_KEY, sname);
        return properties;
    }

    public static CoordinateReferenceSystem createCartesianCRS2D(CoordinateSystemAxis axis1, CoordinateSystemAxis axis2) {
        final DefaultCartesianCS cs = new DefaultCartesianCS(
                    name("Cartesian2d"),
                    axis1, axis2);

        return new DefaultEngineeringCRS(name("Cartesian2d"), DATUM, cs);
    }

    public static CoordinateReferenceSystem createCartesianCRS3D(CoordinateSystemAxis axis1, CoordinateSystemAxis axis2, CoordinateSystemAxis axis3) {
        final DefaultCartesianCS cs = new DefaultCartesianCS(
                    name("Cartesian3d"),
                    axis1, axis2, axis3);

        return new DefaultEngineeringCRS(name("Cartesian3d"), DATUM, cs);
    }

    /**
     * Get rotation matrix from Geometry crs to another.
     */
    public static Matrix3 createRotation(CoordinateReferenceSystem crs1, CoordinateReferenceSystem crs2) throws FactoryException {
        final MathTransform trs = CRS.findOperation(crs1, crs2, null).getMathTransform();
        final LinearTransform lt = (LinearTransform) trs;
        final Matrix m = lt.getMatrix();
        final Matrix3 rotation = new Matrix3();
        rotation.m00 = m.getElement(0, 0);
        rotation.m01 = m.getElement(0, 1);
        rotation.m02 = m.getElement(0, 2);
        rotation.m10 = m.getElement(1, 0);
        rotation.m11 = m.getElement(1, 1);
        rotation.m12 = m.getElement(1, 2);
        rotation.m20 = m.getElement(2, 0);
        rotation.m21 = m.getElement(2, 1);
        rotation.m22 = m.getElement(2, 2);
        return rotation;
    }

    /**
     *
     * @param crs to evaluate, not null
     * @return true if given crs is an undefined crs
     */
    public static boolean isUndefined(CoordinateReferenceSystem crs) {
        return crs.getName().toString().contains("Undefined");
    }

    public static MeshPrimitive createBox(Envelope geom) {

        final Vector lower = Vectors.castOrCopy(geom.getLowerCorner());
        final Vector upper = Vectors.castOrCopy(geom.getUpperCorner());

        final int dim = lower.getDimension();

        if (dim == 2) {
            /*
            2  3
            +--+
            |\ |
            | \|
            +--+
            0  1
            */
            double minx = lower.get(0);
            double miny = lower.get(1);
            double maxx = upper.get(0);
            double maxy = upper.get(1);
            final double[] values = new double[]{
                minx,miny,
                maxx,miny,
                minx,maxy,
                maxx,maxy
            };

            final TupleArray positions = TupleArrays.of(SampleSystem.of(lower.getCoordinateReferenceSystem()), values);
            final TupleArray index = TupleArrays.ofUnsigned(1, new int[]{0,1,2,3});

            final MeshPrimitive primitive = new MeshPrimitive.TriangleStrip();
            primitive.setPositions(positions);
            primitive.setIndex(index);

            return primitive;
        } else if (dim == 3) {
            double minx = lower.get(0);
            double miny = lower.get(1);
            double minz = lower.get(2);
            double maxx = upper.get(0);
            double maxy = upper.get(1);
            double maxz = upper.get(2);

            final Vector3D.Double lll = new Vector3D.Double(minx, miny, minz);
            final Vector3D.Double ull = new Vector3D.Double(maxx, miny, minz);
            final Vector3D.Double uul = new Vector3D.Double(maxx, maxy, minz);
            final Vector3D.Double lul = new Vector3D.Double(minx, maxy, minz);
            final Vector3D.Double ulu = new Vector3D.Double(maxx, miny, maxz);
            final Vector3D.Double llu = new Vector3D.Double(minx, miny, maxz);
            final Vector3D.Double uuu = new Vector3D.Double(maxx, maxy, maxz);
            final Vector3D.Double luu = new Vector3D.Double(minx, maxy, maxz);

            final TupleArray positions = TupleArrays.of(SampleSystem.of(lower.getCoordinateReferenceSystem()), new double[24*3]);
            //back
            positions.set(0, lll);
            positions.set(1, ull);
            positions.set(2, uul);
            positions.set(3, lul);

            //front
            positions.set(4, ulu);
            positions.set(5, llu);
            positions.set(6, luu);
            positions.set(7, uuu);

            //right
            positions.set(8, ull);
            positions.set(9, ulu);
            positions.set(10, uuu);
            positions.set(11, uul);

            //left
            positions.set(12, llu);
            positions.set(13, lll);
            positions.set(14, lul);
            positions.set(15, luu);

            //up
            positions.set(16, uul);
            positions.set(17, uuu);
            positions.set(18, luu);
            positions.set(19, lul);

            //down
            positions.set(20, lll);
            positions.set(21, llu);
            positions.set(22, ulu);
            positions.set(23, ull);

            final int[] indices = {
                2, 1, 0, //back
                3, 2, 0,

                6, 5, 4, //front
                7, 6, 4,

                10, 9, 8, //sides
                11, 10, 8,

                14, 13, 12,
                15, 14, 12,

                18, 17, 16,
                19, 18, 16,

                22, 21, 20,
                23, 22, 20 };

            final TupleArray index = TupleArrays.ofUnsigned(1, indices);

            final MeshPrimitive primitive = new MeshPrimitive.Triangles();
            primitive.setPositions(positions);
            primitive.setIndex(index);
            return primitive;
        } else {
            throw new IllegalArgumentException("Only 2D and 3D BBOX supported.");
        }
    }

    /**
     * Removes any duplicated vertice coordinate.
     * Limitation :
     * - This method can be used only when positions and index are defined.
     * - Single index range of type triangles
     *
     * @param mesh not null
     */
    public static void smoothVertices(MultiMeshPrimitive<?> mesh) {
        ArgumentChecks.ensureNonNull("mesh", mesh);
        for (MeshPrimitive p : mesh.getComponents()) {
            p.removeDuplicatesByPosition();
        }
    }

    /**
     * Regroup all primitives as one.
     * Preserves all attributes.
     *
     * Types which can not be concatenated :
     * - LINE_LOOP
     * - LINE_STRIP
     * - TRIANGLE_FAN
     *
     * @param primitives to concatenate, they must all be of the same type.
     * @return concatenanted primitives
     * throws IllegalArgumentException if type is LINE_LOOP, LINE_STRIP, TRIANGLE_FAN
     */
    public static MeshPrimitive concatenate(Collection<? extends MeshPrimitive> primitives) {

        final Iterator<? extends MeshPrimitive> iterator = primitives.iterator();
        MeshPrimitive primitive = iterator.next();
        final MeshPrimitive.Type type = primitive.getType();
        final CoordinateReferenceSystem crs = primitive.getCoordinateReferenceSystem();

        int maxIndexSize = 0;
        int attSize = 0;
        final Map<String,TupleArray> resultAttributes = new HashMap<>();
        for (MeshPrimitive p : primitives) {
            TupleArray index = p.getIndex();
            maxIndexSize += index.getLength() + 3; //+1 for winding reset, +2 for degenerated triangle
            attSize += p.getPositions().getLength();
        }
        for (String name : primitive.getAttributesType().getAttributeNames()) {
            final TupleArray model = primitive.getAttribute(name);
            resultAttributes.put(name, TupleArrays.of(model.getSampleSystem(), model.getDataType(), attSize));
        }
        int[] resultIndex = new int[maxIndexSize -3]; //no degenerate triangles for the first strip

        //add the first primitive
        int[] primIndex = primitive.getIndex().toArrayInt();
        System.arraycopy(primIndex, 0, resultIndex, 0, primIndex.length);
        for (Entry<String,TupleArray> entry : resultAttributes.entrySet()) {
            final TupleArray resultArray = entry.getValue();
            final TupleArray primAtt = primitive.getAttribute(entry.getKey());
            resultArray.set(0, primAtt, 0, primAtt.getLength());
        }

        int attOffset = primitive.getPositions().getLength();
        int idOffset = primIndex.length;


        while (iterator.hasNext()) {
            primitive = iterator.next();
            if (!primitive.getType().equals(type)) {
                throw new IllegalArgumentException("All primitives must have the same type");
            }
            if (!CRS.equivalent(crs, primitive.getCoordinateReferenceSystem())) {
                throw new IllegalArgumentException("All primitives must have the same CRS, found \n" + crs +"\n and \n" + primitive.getCoordinateReferenceSystem());
            }
            if (resultAttributes.size() != primitive.getAttributesType().getAttributeNames().size()) {
                throw new IllegalArgumentException("All primitives must have the same attributes."
                        + "\n Found " + Arrays.toString(resultAttributes.keySet().toArray())
                        + "\n Found " + Arrays.toString(primitive.getAttributesType().getAttributeNames().toArray()));
            }

            primIndex = primitive.getIndex().toArrayInt();

            switch (type) {
                case LINE_LOOP :
                case LINE_STRIP :
                    throw new IllegalArgumentException("Line Strip and Loop can not be concatenate, unlike triangles which can have a degenerated triangle to link them");
                case TRIANGLE_FAN :
                    throw new IllegalArgumentException("Triangle fan can not be concatenate, unlike triangles which can have a degenerated triangle to link them");
                case TRIANGLE_STRIP : {
                    //we must have even size trips otherwise next concatenated strip will have reversed winding
                    if (idOffset % 2 != 0) {
                        //repeat the last index to reset winding
                        resultIndex[idOffset] = resultIndex[idOffset-1]; //duplicate last index
                        idOffset++;
                    }

                    for (int i = 0; i < primIndex.length; i++) {
                        primIndex[i] += attOffset;
                    }
                    System.arraycopy(primIndex, 0, resultIndex, idOffset + 2, primIndex.length);

                    //add a degenerated triangle between strips
                    resultIndex[idOffset] = resultIndex[idOffset-1];
                    resultIndex[idOffset+1] = resultIndex[idOffset+2];
                    idOffset +=  primIndex.length + 2;

                    for (Entry<String,TupleArray> entry : resultAttributes.entrySet()) {
                        final TupleArray resultArray = entry.getValue();
                        final TupleArray primAtt = primitive.getAttribute(entry.getKey());
                        resultArray.set(attOffset, primAtt, 0, primAtt.getLength());
                    }
                    attOffset += primitive.getPositions().getLength();
                    } break;
                case POINTS :
                case LINES :
                case TRIANGLES : {
                    System.arraycopy(primIndex, 0, resultIndex, idOffset, primIndex.length);
                    for (int i = idOffset, end = idOffset + primIndex.length; i < end; i++) {
                        resultIndex[i] += attOffset;
                    }
                    idOffset += primIndex.length;
                    for (Entry<String,TupleArray> entry : resultAttributes.entrySet()) {
                        final TupleArray resultArray = entry.getValue();
                        final TupleArray primAtt = primitive.getAttribute(entry.getKey());
                        resultArray.set(attOffset, primAtt, 0, primAtt.getLength());
                    }
                    attOffset += primitive.getPositions().getLength();
                    }break;
                default : throw new IllegalArgumentException("Unexpected type " + type);
            }
        }

        //build result primitive
        final MeshPrimitive result = MeshPrimitive.create(type);
        if (idOffset != resultIndex.length) resultIndex = Arrays.copyOf(resultIndex, idOffset);
        final TupleArray allIndex = TupleArrays.ofUnsigned(1, resultIndex);
        result.setIndex(allIndex);
        for (Entry<String,TupleArray> entry : resultAttributes.entrySet()) {
            result.setAttribute(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Search similar primitives in the multi-primitive and regroup them when possible.
     * To be regrouped primitives must have the same type and same attributes.
     *
     * @param primitives not null
     * @return collection of concatenated promitives
     */
    public static Collection<? extends MeshPrimitive> tryConcatenate(Collection<? extends MeshPrimitive> primitives) {
        ArgumentChecks.ensureNonNull("primitives", primitives);

        if (primitives.size() <= 1) {
            //nothing to concatenate
            return primitives;
        }

        final Map<MeshPrimitive.Type, List<MeshPrimitive>> groups = new HashMap<>();

        for (MeshPrimitive p : primitives) {
            final MeshPrimitive.Type type = p.getType();
            List<MeshPrimitive> group = groups.get(type);
            if (group == null) {
                group = new ArrayList<>();
                groups.put(type, group);
            }
            group.add(p);
        }

        final List<MeshPrimitive> newPrimitives = new ArrayList<>(groups.size());
        for (List<MeshPrimitive> group : groups.values()) {
            if (group.size() > 1) {
                try {
                    MeshPrimitive p = concatenate(group);
                    newPrimitives.add(p);
                } catch (IllegalArgumentException ex) {
                    //we have try
                    newPrimitives.addAll(group);
                }
            } else {
                newPrimitives.add(group.get(0));
            }
        }

        return newPrimitives;
    }

    /**
     * Cut the primitive in smaller primitives of at most MaxSize positions.
     *
     * If primitive has less positions then given size it is returned unchanged
     * as single element in the list.
     *
     * @param primitive Primitive to cut
     * @param maxSize maximum number of elements by primitive.
     * @return splitted primitive.
     */
    public static List<MeshPrimitive> split(MeshPrimitive primitive, int maxSize) {
        if ( primitive.getPositions().getLength() <= maxSize
          && primitive.getIndex().getLength() <= maxSize) {
            return Arrays.asList(primitive);
        }

        final MeshPrimitive.Type type = primitive.getType();
        final TupleArray index = primitive.getIndex();

        final List<MeshPrimitive> primitives = new ArrayList<>(2);
        switch (type) {
            case TRIANGLES : {
                //split by packs of 3
                final int batchSize = (maxSize / 3) * 3;
                final int[] array = index.toArrayInt();
                for (int i = 0; i < array.length; i+= batchSize) {
                    int end = i + batchSize;
                    if (end > array.length) end = array.length;
                    final int[] subRange = Arrays.copyOfRange(array, i, end);
                    final MeshPrimitive copy = primitive.deepCopy();
                    copy.setIndex(TupleArrays.ofUnsigned(1, subRange));
                    if (copy.getIndex().getLength() > maxSize) {
                        throw new UnsupportedOperationException("ARG 1");
                    }
                    compact(copy);
                    if (copy.getIndex().getLength() > maxSize || copy.getPositions().getLength() > maxSize) {
                        throw new UnsupportedOperationException("ARG 2");
                    }
                    primitives.add(copy);
                }
            } break;
            default : throw new UnsupportedOperationException("Type " + type + "not supported yet.");
        }
        return primitives;
    }

    /**
     * Remove all points which are not used in the index.
     */
    public static void compact(MeshPrimitive primitive) {

        final TupleArray indexArray = primitive.getIndex();
        if (indexArray == null) {
            //all attributes are used
            return;
        }

        int inc = -1;
        final Map<Integer, Integer> mapping = new HashMap<>();
        final Map<String,List<Tuple>> rebuild = new IdentityHashMap<>();
        final int[] index = indexArray.toArrayInt();

        for (String name : primitive.getAttributesType().getAttributeNames()) {
            rebuild.put(name, new ArrayList<>());
        }

        for (int i = 0; i < index.length; i++) {
            final int oldIndex = index[i];
            Integer newIndex = mapping.get(oldIndex);
            if (newIndex == null) {
                newIndex = ++inc;
                mapping.put(oldIndex, newIndex);

                for (String name : rebuild.keySet()) {
                    final TupleArray oldTa = primitive.getAttribute(name);
                    final List<Tuple> newTa = rebuild.get(name);
                    newTa.add(newIndex, oldTa.get(oldIndex));
                }
            }
            index[i] = newIndex;
        }

        //rebuild attributes arrays
        for (String name : rebuild.keySet()) {
            final TupleArray oldTa = primitive.getAttribute(name);
            final List<Tuple> newTa = rebuild.get(name);
            final TupleArray ta = TupleArrays.of(newTa, oldTa.getSampleSystem(), oldTa.getDataType());
            primitive.setAttribute(name, ta);
        }
        //new index
        primitive.setIndex(TupleArrays.ofUnsigned(primitive.getIndex().getSampleSystem(), index));
    }

    public static MeshPrimitive.Triangles toPrimitive(List<org.locationtech.jts.geom.Polygon> triangles, CoordinateReferenceSystem crs) {
        final int dimension = crs.getCoordinateSystem().getDimension();
        final float[] values = new float[Math.multiplyExact(triangles.size(), 3 * dimension)];
        final int[] index = new int[triangles.size()*3];
        int k = -1;
        final Coordinate coord = new Coordinate();
        for (int i = 0, n = triangles.size(); i < n; i++) {
            org.locationtech.jts.geom.Polygon p = triangles.get(i);
            //counter clockwise to ensure up direction
            p = JTS.ensureWinding(p, false);
            final CoordinateSequence ring = p.getExteriorRing().getCoordinateSequence();
            int idx = i*3*dimension;
            switch (dimension) {
                case 2 :
                    values[idx+0] = (float) ring.getX(0);
                    values[idx+1] = (float) ring.getY(0);
                    ring.getCoordinate(1, coord);
                    values[idx+2] = (float) ring.getX(1);
                    values[idx+3] = (float) ring.getY(1);
                    ring.getCoordinate(2, coord);
                    values[idx+4] = (float) ring.getX(2);
                    values[idx+5] = (float) ring.getY(2);
                    break;
                case 3 :
                    values[idx+0] = (float) ring.getX(0);
                    values[idx+1] = (float) ring.getY(0);
                    values[idx+2] = (float) ring.getZ(0);
                    ring.getCoordinate(1, coord);
                    values[idx+3] = (float) ring.getX(1);
                    values[idx+4] = (float) ring.getY(1);
                    values[idx+5] = (float) ring.getZ(1);
                    ring.getCoordinate(2, coord);
                    values[idx+6] = (float) ring.getX(2);
                    values[idx+7] = (float) ring.getY(2);
                    values[idx+8] = (float) ring.getZ(2);
                    break;
                default: throw new UnsupportedOperationException();
            }
            index[i*3+0] = ++k;
            index[i*3+1] = ++k;
            index[i*3+2] = ++k;
        }

        final TupleArray positions = TupleArrays.of(SampleSystem.of(crs), values);
        TupleArray idx = TupleArrays.of(1, index);
        idx = TupleArrays.packIntegerDataType(idx);

        final MeshPrimitive.Triangles primitive = new MeshPrimitive.Triangles();
        primitive.setPositions(positions);
        primitive.setIndex(idx);
        return primitive;
    }

    /**
     * Ensure two geometries declare the same attributes.
     */
    public static void ensureSameAttributes(AttributesType att1, AttributesType att2) {
        final List<String> names1 = att1.getAttributeNames();
        final List<String> names2 = att2.getAttributeNames();
        if (names1.size() != names2.size() || !names1.containsAll(names2)) {
            throw new IllegalArgumentException("Attributes are different. found " + Arrays.toString(names1.toArray()) +" and " + Arrays.toString(names2.toArray()));
        }
        for (String name : names1) {
            if (!att1.getAttributeSystem(name).equals(att2.getAttributeSystem(name))) {
                throw new IllegalArgumentException("Attribute systems are different for name " + name);
            }
            if (!att1.getAttributeType(name).equals(att2.getAttributeType(name))) {
                throw new IllegalArgumentException("Attribute types are different for name " + name);
            }
        }
    }

    /**
     * Convert given JTS geometry to SIS Geometry.
     */
    public static Geometry fromJTS(org.locationtech.jts.geom.Geometry jts) {
        if (jts == null) {
            return null;
        }
        CoordinateReferenceSystem crs = org.apache.sis.geometry.wrapper.Geometries.wrap(jts).get().getCoordinateReferenceSystem();
        if (crs == null) crs = Geometries.getUndefinedCRS(2);
        return fromJTS(jts, crs);
    }

    /**
     * Convert given JTS geometry to SIS Geometry.
     */
    private static Geometry fromJTS(org.locationtech.jts.geom.Geometry jts, CoordinateReferenceSystem crs) {
        if (jts == null) {
            return null;
        } else if (jts instanceof org.locationtech.jts.geom.Point cdt) {
            return GeometryFactory.createPoint(toPointSequence(cdt.getCoordinateSequence(), crs));

        } else if (jts instanceof org.locationtech.jts.geom.MultiPoint cdt) {
            return GeometryFactory.createMultiPoint(toPointSequence(jts.getFactory().getCoordinateSequenceFactory().create(cdt.getCoordinates()), crs));

        } else if (jts instanceof org.locationtech.jts.geom.LinearRing cdt) {
            return GeometryFactory.createLinearRing(toPointSequence(cdt.getCoordinateSequence(), crs));

        } else if (jts instanceof org.locationtech.jts.geom.LineString cdt) {
            return GeometryFactory.createLineString(toPointSequence(cdt.getCoordinateSequence(), crs));

        } else if (jts instanceof org.locationtech.jts.geom.MultiLineString cdt) {
            final LineString[] strings = new LineString[cdt.getNumGeometries()];
            for (int i = 0; i < strings.length; i++) {
                strings[i] = (LineString) fromJTS(cdt.getGeometryN(i), crs);
            }
            return GeometryFactory.createMultiLineString(strings);
        } else if (jts instanceof org.locationtech.jts.geom.Polygon cdt) {
            final LinearRing exterior = (LinearRing) fromJTS(cdt.getExteriorRing(), crs);
            final List<LinearRing> interiors = new ArrayList<>(cdt.getNumInteriorRing());
            for (int i = 0, n = cdt.getNumInteriorRing(); i < n; i++) {
                interiors.add((LinearRing) fromJTS(cdt.getInteriorRingN(i), crs));
            }
            return GeometryFactory.createPolygon(exterior, interiors);

        } else if (jts instanceof org.locationtech.jts.geom.MultiPolygon cdt) {
            final Surface[] geoms = new Surface[cdt.getNumGeometries()];
            for (int i = 0; i < geoms.length; i++) {
                geoms[i] = (Surface) fromJTS(cdt.getGeometryN(i), crs);
            }
            return GeometryFactory.createMultiSurface(geoms);

        } else if (jts instanceof org.locationtech.jts.geom.GeometryCollection cdt) {
            final Geometry[] geoms = new Geometry[cdt.getNumGeometries()];
            for (int i = 0; i < geoms.length; i++) {
                geoms[i] = fromJTS(cdt.getGeometryN(i), crs);
            }
            return GeometryFactory.createGeometryCollection(geoms);

        } else {
            throw new IllegalArgumentException("Unknown JTS geometry type");
        }
    }

    /**
     * Convert JTS coordinate sequence to SIS PointSequence.
     */
    private static PointSequence toPointSequence(CoordinateSequence cs, CoordinateReferenceSystem crs) {
        final int size = cs.size();
        final int dimension = crs.getCoordinateSystem().getDimension();
        final TupleArray positions = TupleArrays.of(SampleSystem.of(crs), DataType.DOUBLE, size);
        final TupleArrayCursor cursor = positions.cursor();
        int i = 0;
        while (cursor.next()) {
            final Tuple samples = cursor.samples();
            samples.set(0, cs.getOrdinate(i, 0));
            samples.set(1, cs.getOrdinate(i, 1));
            if (dimension > 2) {
                //JTS only goes up to 3 dimensions
                samples.set(2, cs.getOrdinate(i, 2));
            }
            i++;
        }
        return new ArraySequence(positions);
    }

}
