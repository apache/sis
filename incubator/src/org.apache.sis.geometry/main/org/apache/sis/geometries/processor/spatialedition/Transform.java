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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.Vector;
import org.apache.sis.geometries.math.Vectors;
import org.apache.sis.geometries.operation.GeometryOperations;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.geometries.internal.shared.ArraySequence;
import org.apache.sis.geometries.processor.Processor;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.geometries.math.Array;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Transform {

    private Transform(){}

    private static Array transform(Array reference, org.apache.sis.geometries.operation.spatialedition.Transform operation) {

        final int sourceDimension = reference.getDimension();
        final int targetDimension = operation.crs.getCoordinateSystem().getDimension();
        final SampleSystem ss = SampleSystem.of(operation.crs);

        if (sourceDimension == targetDimension) {
            //use in place transform
            final Array positions = reference.copy();
            try {
                positions.transform(operation.transform);
            } catch (TransformException ex) {
                throw new OperationException(ex.getMessage(), ex);
            }
            positions.setSampleSystem(ss);
            return positions;
        } else {
            final int nb = Math.toIntExact(reference.getLength());
            final double[] values = reference.toArrayDouble();
            final double[] result = new double[targetDimension * nb];
            try {
                operation.transform.transform(values, 0, result, 0, nb);
            } catch (TransformException ex) {
                throw new OperationException(ex.getMessage(), ex);
            }
            return NDArrays.of(ss, result);
        }
    }

    public static class LinearRing implements Processor<org.apache.sis.geometries.operation.spatialedition.Transform, org.apache.sis.geometries.LinearRing> {

        @Override
        public Class<org.apache.sis.geometries.operation.spatialedition.Transform> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialedition.Transform.class;
        }

        @Override
        public Class<org.apache.sis.geometries.LinearRing> getGeometryClass() {
            return org.apache.sis.geometries.LinearRing.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialedition.Transform operation) throws OperationException {
            final org.apache.sis.geometries.LinearRing r = (org.apache.sis.geometries.LinearRing) operation.geometry;

            PointSequence ps = r.getPoints();
            final Array reference = ps.getAttributeArray(AttributesType.ATT_POSITION);
            final Array positions = transform(reference, operation);
            final ArraySequence cp = new ArraySequence(positions);
            for (String name : ps.getAttributesType().getAttributeNames()) {
                if (!AttributesType.ATT_POSITION.equals(name)) {
                    cp.setAttribute(name, ps.getAttributeArray(name).copy());
                }
            }
            operation.result = GeometryFactory.createLinearRing(cp);
        }
    }

    public static class Polygon implements Processor<org.apache.sis.geometries.operation.spatialedition.Transform, org.apache.sis.geometries.Polygon> {

        @Override
        public Class<org.apache.sis.geometries.operation.spatialedition.Transform> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialedition.Transform.class;
        }

        @Override
        public Class<org.apache.sis.geometries.Polygon> getGeometryClass() {
            return org.apache.sis.geometries.Polygon.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialedition.Transform operation) throws OperationException {
            final org.apache.sis.geometries.Polygon p = (org.apache.sis.geometries.Polygon) operation.geometry;

            org.apache.sis.geometries.LinearRing exterior = (org.apache.sis.geometries.LinearRing) GeometryOperations.SpatialEdition.transform(p.getExteriorRing(), operation.crs, operation.transform);

            final List<org.apache.sis.geometries.LinearRing> interiors = new ArrayList<>(p.getInteriorRings());
            for (int i = 0, n = interiors.size(); i < n; i++) {
                interiors.set(i, (org.apache.sis.geometries.LinearRing) GeometryOperations.SpatialEdition.transform(interiors.get(i), operation.crs, operation.transform));
            }

            operation.result = GeometryFactory.createPolygon(exterior, interiors);
        }
    }

    public static class MultiPrimitive implements Processor<org.apache.sis.geometries.operation.spatialedition.Transform, org.apache.sis.geometries.mesh.MultiMeshPrimitive>{

        @Override
        public Class<org.apache.sis.geometries.operation.spatialedition.Transform> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialedition.Transform.class;
        }

        @Override
        public Class<org.apache.sis.geometries.mesh.MultiMeshPrimitive> getGeometryClass() {
            return org.apache.sis.geometries.mesh.MultiMeshPrimitive.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialedition.Transform operation) throws OperationException {
            final org.apache.sis.geometries.mesh.MultiMeshPrimitive<?> mp = (org.apache.sis.geometries.mesh.MultiMeshPrimitive) operation.geometry;
            final org.apache.sis.geometries.mesh.MultiMeshPrimitive<?> copy = new org.apache.sis.geometries.mesh.MultiMeshPrimitive(operation.crs);
            final List<org.apache.sis.geometries.mesh.MeshPrimitive> primitives = new ArrayList<>();
            for (org.apache.sis.geometries.mesh.MeshPrimitive p : mp.getComponents()) {
                primitives.add((org.apache.sis.geometries.mesh.MeshPrimitive) GeometryOperations.SpatialEdition.transform(p, operation.crs, operation.transform));
            }
            copy.append(primitives);
            operation.result = copy;
        }
    }

    /**
     * Transform primitive to a new CoordinateReferenceSystem.
     * Note : this method will clone all other attributes untransformed.
     */
    public static class Primitive implements Processor<org.apache.sis.geometries.operation.spatialedition.Transform, org.apache.sis.geometries.mesh.MeshPrimitive>{

        @Override
        public Class<org.apache.sis.geometries.operation.spatialedition.Transform> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialedition.Transform.class;
        }

        @Override
        public Class<org.apache.sis.geometries.mesh.MeshPrimitive> getGeometryClass() {
            return org.apache.sis.geometries.mesh.MeshPrimitive.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialedition.Transform operation) throws OperationException {
            final org.apache.sis.geometries.mesh.MeshPrimitive p = (org.apache.sis.geometries.mesh.MeshPrimitive) operation.geometry;
            final org.apache.sis.geometries.mesh.MeshPrimitive copy = org.apache.sis.geometries.mesh.MeshPrimitive.create(p.getType());

            final Set<String> toSkip = new HashSet<>();
            final Array positions = p.getAttribute(AttributesType.ATT_POSITION);
            final Array normals = p.getAttribute(AttributesType.ATT_NORMAL);
            final Array tangents = p.getAttribute(AttributesType.ATT_TANGENT);
            if (positions != null) {
                toSkip.add(AttributesType.ATT_POSITION);
                toSkip.add(AttributesType.ATT_NORMAL);
                toSkip.add(AttributesType.ATT_TANGENT);

                try {
                    //transform positions
                    final Array cpp = positions.copy();
                    cpp.setSampleSystem(SampleSystem.of(operation.crs));
                    cpp.transform(operation.transform);
                    copy.setAttribute(AttributesType.ATT_POSITION, cpp);

                    final Array cpn = (normals == null) ? null : normals.copy();
                    final Array cpt = (tangents == null) ? null : tangents.copy();

                    //transform normal and tangent with local matrix at each point
                    if (normals != null || tangents != null) {
                        final Vector pos = Vectors.create(positions.getSampleSystem(), positions.getDataType());

                        final Vector nor = (normals == null) ? null : Vectors.create(normals.getSampleSystem(), normals.getDataType());
                        final Vector tag = (tangents == null) ? null : Vectors.create(tangents.getSampleSystem(), tangents.getDataType());
                        for (long i = 0, n = positions.getLength(); i < n; i++) {
                            positions.get(i, pos);
                            final MatrixSIS matrix = MatrixSIS.castOrCopy(operation.transform.derivative(Vectors.asDirectPostion(pos)));
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
            final Array index = p.getIndex();
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

            final Array reference = ps.getAttributeArray(AttributesType.ATT_POSITION);
            final Array positions = transform(reference, operation);
            final ArraySequence cp = new ArraySequence(positions);
            for (String name : ps.getAttributesType().getAttributeNames()) {
                if (!AttributesType.ATT_POSITION.equals(name)) {
                    cp.setAttribute(name, ps.getAttributeArray(name).copy());
                }
            }
            operation.result = GeometryFactory.createTriangle(GeometryFactory.createLinearRing(cp));
        }

    }
}
