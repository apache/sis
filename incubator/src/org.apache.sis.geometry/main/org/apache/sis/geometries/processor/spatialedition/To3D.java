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
package org.apache.sis.geometries.processor.spatialedition;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.Cursor;
import org.apache.sis.geometries.math.Array;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.geometries.internal.shared.ArraySequence;
import org.apache.sis.geometries.processor.Processor;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class To3D {

    private To3D(){}

    private static void zedit(Array array, Consumer<Tuple> Zeditor) {
        final Cursor cursor = array.cursor();
        while (cursor.next()) {
            Zeditor.accept(cursor.samples());
        }
    }

    private static ArraySequence copy(PointSequence ps) {
        final Map<String,Array> attributes = new HashMap<>();
        for (String name : ps.getAttributesType().getAttributeNames()) {
            attributes.put(name, ps.getAttributeArray(name).copy());
        }
        return new ArraySequence(attributes);
    }

    private static ArraySequence to3d(PointSequence base, CoordinateReferenceSystem crs3d, Consumer<Tuple> Zeditor) {

        final ArraySequence ps = copy(base);

        if (Zeditor == null) {
            Zeditor = (Tuple t) -> t.set(2, 0.0);
        }

        Array positions = ps.getAttributeArray(AttributesType.ATT_POSITION);
        positions = to3d(positions, crs3d, Zeditor);
        ps.setAttribute(AttributesType.ATT_POSITION, positions);
        return ps;
    }

    private static Array to3d(Array positions, CoordinateReferenceSystem crs3d, Consumer<Tuple> Zeditor) {

        if (Zeditor == null) {
            Zeditor = (Tuple t) -> t.set(2, 0.0);
        }

        final CoordinateReferenceSystem geomCrs = positions.getCoordinateReferenceSystem();
        final int geomCrsDim = geomCrs.getCoordinateSystem().getDimension();
        if (geomCrsDim < 2) throw new OperationException("Geometry crs must have at least two dimensions");

        if (crs3d == null && geomCrsDim > 2) {
            //just edit the Z values
            zedit(positions, Zeditor);
        } else if (crs3d != null && geomCrsDim == 3) {
            //change the crs and edit values
            positions.setSampleSystem(SampleSystem.of(crs3d));
            zedit(positions, Zeditor);
        } else {
            if (crs3d == null) {
                try {
                    crs3d = CRS.compound(geomCrs, CommonCRS.Vertical.ELLIPSOIDAL.crs());
                } catch (FactoryException ex) {
                    throw new OperationException(ex.getMessage(), ex);
                }
            }
            //create a new one
            final Array array = NDArrays.of(SampleSystem.of(crs3d), positions.getDataType(), positions.getLength());
            final Cursor target = array.cursor();
            final Cursor source = positions.cursor();
            while (source.next() && target.next()) {
                Tuple t = target.samples();
                Tuple s = source.samples();
                t.set(0, s.get(0));
                t.set(1, s.get(1));
                Zeditor.accept(t);
            }
            positions = array;
        }
        return positions;
    }

    /**
     * Add Z axis to Point.
     */
    public static class Point implements Processor<org.apache.sis.geometries.operation.spatialedition.To3D, org.apache.sis.geometries.Point>{

        @Override
        public Class<org.apache.sis.geometries.operation.spatialedition.To3D> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialedition.To3D.class;
        }

        @Override
        public Class<org.apache.sis.geometries.Point> getGeometryClass() {
            return org.apache.sis.geometries.Point.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialedition.To3D operation) throws OperationException {
            final org.apache.sis.geometries.Point base = (org.apache.sis.geometries.Point) operation.geometry;
            final PointSequence copy3d = to3d(base.asPointSequence(), operation.crs3d, operation.Zeditor);
            operation.result = GeometryFactory.createPoint(copy3d);
        }
    }

    /**
     * Add Z axis to LineString.
     */
    public static class LineString implements Processor<org.apache.sis.geometries.operation.spatialedition.To3D, org.apache.sis.geometries.LineString>{

        @Override
        public Class<org.apache.sis.geometries.operation.spatialedition.To3D> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialedition.To3D.class;
        }

        @Override
        public Class<org.apache.sis.geometries.LineString> getGeometryClass() {
            return org.apache.sis.geometries.LineString.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialedition.To3D operation) throws OperationException {
            final org.apache.sis.geometries.LineString base = (org.apache.sis.geometries.LineString) operation.geometry;
            final PointSequence copy3d = to3d(base.getPoints(), operation.crs3d, operation.Zeditor);
            operation.result = GeometryFactory.createLineString(copy3d);
        }
    }

    /**
     * Add Z axis to Primitive.
     * Also works for ModelPrimitive.
     */
    public static class Primitive implements Processor<org.apache.sis.geometries.operation.spatialedition.To3D, org.apache.sis.geometries.mesh.MeshPrimitive>{

        @Override
        public Class<org.apache.sis.geometries.operation.spatialedition.To3D> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialedition.To3D.class;
        }

        @Override
        public Class<org.apache.sis.geometries.mesh.MeshPrimitive> getGeometryClass() {
            return org.apache.sis.geometries.mesh.MeshPrimitive.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialedition.To3D operation) throws OperationException {
            final org.apache.sis.geometries.mesh.MeshPrimitive base = (org.apache.sis.geometries.mesh.MeshPrimitive) operation.geometry;
            final org.apache.sis.geometries.mesh.MeshPrimitive copy3d = base.deepCopy();
            operation.result = copy3d;

            Array positions = copy3d.getPositions();
            positions = to3d(positions, operation.crs3d, operation.Zeditor);
            copy3d.setPositions(positions);
        }
    }

}
