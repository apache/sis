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

import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometries.ArraySequence;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.DefaultLinearRing;
import org.apache.sis.geometries.DefaultTriangle;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.operation.GeometryOperations;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.geometries.processor.Processor;
import org.apache.sis.geometries.math.TupleArray;
import org.apache.sis.geometries.math.TupleArrays;
import org.apache.sis.geometries.math.Vector;
import org.apache.sis.geometries.math.Vectors;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Transform {

    private Transform(){}

    private static TupleArray transform(TupleArray reference, org.apache.sis.geometries.operation.spatialedition.Transform operation) {

        final int sourceDimension = reference.getDimension();
        final int targetDimension = operation.crs.getCoordinateSystem().getDimension();

        if (sourceDimension == targetDimension) {
            //use in place transform
            final TupleArray positions = reference.copy();
            try {
                positions.transform(operation.transform);
            } catch (TransformException ex) {
                throw new OperationException(ex.getMessage(), ex);
            }
            return positions;
        } else {
            final int nb = reference.getLength();
            final double[] values = reference.toArrayDouble();
            final double[] result = new double[targetDimension * nb];
            try {
                operation.transform.transform(values, 0, result, 0, nb);
            } catch (TransformException ex) {
                throw new OperationException(ex.getMessage(), ex);
            }
            return TupleArrays.of(SampleSystem.of(operation.crs), result);
        }
    }

    public static class MultiPrimitive implements Processor<org.apache.sis.geometries.operation.spatialedition.Transform, org.apache.sis.geometries.MultiMeshPrimitive>{

        @Override
        public Class<org.apache.sis.geometries.operation.spatialedition.Transform> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialedition.Transform.class;
        }

        @Override
        public Class<org.apache.sis.geometries.MultiMeshPrimitive> getGeometryClass() {
            return org.apache.sis.geometries.MultiMeshPrimitive.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialedition.Transform operation) throws OperationException {
            final org.apache.sis.geometries.MultiMeshPrimitive<?> mp = (org.apache.sis.geometries.MultiMeshPrimitive) operation.geometry;
            final org.apache.sis.geometries.MultiMeshPrimitive<?> copy = new org.apache.sis.geometries.MultiMeshPrimitive(operation.crs);
            final List<org.apache.sis.geometries.MeshPrimitive> primitives = new ArrayList<>();
            for (org.apache.sis.geometries.MeshPrimitive p : mp.getComponents()) {
                primitives.add((org.apache.sis.geometries.MeshPrimitive) GeometryOperations.SpatialEdition.transform(p, operation.crs, operation.transform));
            }
            copy.append(primitives);
            operation.result = copy;
        }
    }

    /**
     * Transform primitive to a new CoordinateReferenceSystem.
     * Note : this method will clone all other attributes untransformed.
     */
    public static class Primitive implements Processor<org.apache.sis.geometries.operation.spatialedition.Transform, org.apache.sis.geometries.MeshPrimitive>{

        @Override
        public Class<org.apache.sis.geometries.operation.spatialedition.Transform> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialedition.Transform.class;
        }

        @Override
        public Class<org.apache.sis.geometries.MeshPrimitive> getGeometryClass() {
            return org.apache.sis.geometries.MeshPrimitive.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialedition.Transform operation) throws OperationException {
            final org.apache.sis.geometries.MeshPrimitive p = (org.apache.sis.geometries.MeshPrimitive) operation.geometry;
            final org.apache.sis.geometries.MeshPrimitive copy = org.apache.sis.geometries.MeshPrimitive.create(p.getType());

            final Set<String> toSkip = new HashSet<>();
            final TupleArray positions = p.getAttribute(AttributesType.ATT_POSITION);
            final TupleArray normals = p.getAttribute(AttributesType.ATT_NORMAL);
            final TupleArray tangents = p.getAttribute(AttributesType.ATT_TANGENT);
            if (positions != null) {
                toSkip.add(AttributesType.ATT_POSITION);
                toSkip.add(AttributesType.ATT_NORMAL);
                toSkip.add(AttributesType.ATT_TANGENT);

                try {
                    //transform positions
                    final TupleArray cpp = positions.copy();
                    cpp.setSampleSystem(SampleSystem.of(operation.crs));
                    cpp.transform(operation.transform);
                    copy.setAttribute(AttributesType.ATT_POSITION, cpp);

                    final TupleArray cpn = (normals == null) ? null : normals.copy();
                    final TupleArray cpt = (tangents == null) ? null : tangents.copy();

                    //transform normal and tangent with local matrix at each point
                    if (normals != null || tangents != null) {
                        final Vector pos = Vectors.create(positions.getSampleSystem(), positions.getDataType());

                        final Vector nor = (normals == null) ? null : Vectors.create(normals.getSampleSystem(), normals.getDataType());
                        final Vector tag = (tangents == null) ? null : Vectors.create(tangents.getSampleSystem(), tangents.getDataType());
                        for (int i = 0, n = positions.getLength(); i < n; i++) {
                            positions.get(i, pos);
                            final MatrixSIS matrix = MatrixSIS.castOrCopy(operation.transform.derivative(pos));
                            if (nor != null) {
                                cpn.get(i, nor);
                                nor.set(matrix.multiply(nor.toArrayDouble()));
                                nor.normalize();
                                cpn.set(i, nor);
                            }
                            if (tag != null) {
                                cpt.get(i, tag);
                                tag.set(matrix.multiply(tag.toArrayDouble()));
                                tag.normalize();
                                cpt.set(i, tag);
                            }
                        }
                        if (cpn != null) copy.setAttribute(AttributesType.ATT_NORMAL, cpn);
                        if (cpt != null) copy.setAttribute(AttributesType.ATT_TANGENT, cpt);
                    }
                } catch (TransformException ex) {
                    throw new OperationException(ex.getMessage(), ex);
                }
            }

            //copy all other attributes
            for (String name : p.getAttributesType().getAttributeNames()) {
                if (!toSkip.contains(name)) {
                    copy.setAttribute(name, p.getAttribute(name).copy());
                }
            }

            //copy index and ranges
            final TupleArray index = p.getIndex();
            if (index != null) {
                copy.setIndex(index.copy());
            }

            operation.result = copy;
        }
    }

    public static class Triangle implements Processor<org.apache.sis.geometries.operation.spatialedition.Transform, org.apache.sis.geometries.Triangle> {

        @Override
        public Class<org.apache.sis.geometries.operation.spatialedition.Transform> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialedition.Transform.class;
        }

        @Override
        public Class<org.apache.sis.geometries.Triangle> getGeometryClass() {
            return org.apache.sis.geometries.Triangle.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialedition.Transform operation) throws OperationException {
            final org.apache.sis.geometries.Triangle p = (org.apache.sis.geometries.Triangle) operation.geometry;
            final PointSequence ps = p.getExteriorRing().getPoints();

            final TupleArray reference = ps.getAttributeArray(AttributesType.ATT_POSITION);
            final TupleArray positions = transform(reference, operation);
            final ArraySequence cp = new ArraySequence(positions);
            for (String name : ps.getAttributesType().getAttributeNames()) {
                if (!AttributesType.ATT_POSITION.equals(name)) {
                    cp.setAttribute(name, ps.getAttributeArray(name).copy());
                }
            }
            operation.result = new DefaultTriangle(new DefaultLinearRing(cp));
        }

    }
}
