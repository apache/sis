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
import java.util.List;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.mesh.MeshPrimitiveVisitor;
import org.apache.sis.geometries.operation.GeometryOperations;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.geometries.processor.Processor;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.Array;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ComputeAttribute {

    private ComputeAttribute(){}


    /**
     * Compute attribute on Primitive.
     * Also works for ModelPrimitive.
     */
    public static class Primitive implements Processor<org.apache.sis.geometries.operation.spatialedition.ComputeAttribute, org.apache.sis.geometries.mesh.MeshPrimitive>{

        @Override
        public Class<org.apache.sis.geometries.operation.spatialedition.ComputeAttribute> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialedition.ComputeAttribute.class;
        }

        @Override
        public Class<org.apache.sis.geometries.mesh.MeshPrimitive> getGeometryClass() {
            return org.apache.sis.geometries.mesh.MeshPrimitive.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialedition.ComputeAttribute operation) throws OperationException {
            final org.apache.sis.geometries.mesh.MeshPrimitive base = (org.apache.sis.geometries.mesh.MeshPrimitive) operation.geometry;
            final org.apache.sis.geometries.mesh.MeshPrimitive copy3d = base.deepCopy();
            operation.result = copy3d;

            Array ta = copy3d.getAttribute(operation.attributeName);
            if (ta == null) {
                ta = NDArrays.of(operation.attributeSystem, operation.attributeType, copy3d.getPositions().getLength());
                copy3d.setAttribute(operation.attributeName, ta);
            }

            if (operation.valueGenerator != null) {
                new MeshPrimitiveVisitor(copy3d) {
                    @Override
                    protected void visit(org.apache.sis.geometries.mesh.MeshPrimitive.Vertex vertex) {
                        vertex.setAttribute(operation.attributeName, operation.valueGenerator.apply(vertex));
                    }
                }.visit();
            }
        }
    }


    /**
     * Compute attribute on MultiPrimitive.
     * Also works for ModelPrimitive.
     */
    public static class MultiPrimitive implements Processor<org.apache.sis.geometries.operation.spatialedition.ComputeAttribute, org.apache.sis.geometries.mesh.MultiMeshPrimitive>{

        @Override
        public Class<org.apache.sis.geometries.operation.spatialedition.ComputeAttribute> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialedition.ComputeAttribute.class;
        }

        @Override
        public Class<org.apache.sis.geometries.mesh.MultiMeshPrimitive> getGeometryClass() {
            return org.apache.sis.geometries.mesh.MultiMeshPrimitive.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialedition.ComputeAttribute operation) throws OperationException {
            final org.apache.sis.geometries.mesh.MultiMeshPrimitive base = (org.apache.sis.geometries.mesh.MultiMeshPrimitive) operation.geometry;

            final List<org.apache.sis.geometries.mesh.MeshPrimitive> news = new ArrayList<>();
            for (int i = 0, n = base.getNumGeometries(); i < n; i++) {
                Geometry p = base.getGeometryN(i);
                p = GeometryOperations.SpatialEdition.computeAttribute(p, operation.attributeName, operation.attributeSystem, operation.attributeType, operation.valueGenerator);
                news.add((org.apache.sis.geometries.mesh.MeshPrimitive) p);
            }

            final org.apache.sis.geometries.mesh.MultiMeshPrimitive copy3d = new org.apache.sis.geometries.mesh.MultiMeshPrimitive(base.getCoordinateReferenceSystem());
            copy3d.append(news);
            operation.result = copy3d;
        }
    }
}
