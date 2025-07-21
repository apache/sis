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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.math.TupleArray;
import org.apache.sis.geometries.math.TupleArrays;
import org.apache.sis.geometries.operation.GeometryOperations;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.geometries.privy.ArraySequence;
import org.apache.sis.geometries.processor.Processor;
import org.apache.sis.geometries.triangulate.EarClipping;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ToPrimitive {

    private ToPrimitive(){}

    private static ArraySequence toArraySequence(PointSequence points) {
        final Map<String,TupleArray> attributes = new HashMap<>();
        for (String name : points.getAttributesType().getAttributeNames()) {
            attributes.put(name, points.getAttributeArray(name));
        }
        return new ArraySequence(attributes);
    }

    /**
     * Transform Point to Primitive.
     */
    public static class Point implements Processor<org.apache.sis.geometries.operation.spatialedition.ToPrimitive, org.apache.sis.geometries.Point>{

        @Override
        public Class<org.apache.sis.geometries.operation.spatialedition.ToPrimitive> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialedition.ToPrimitive.class;
        }

        @Override
        public Class<org.apache.sis.geometries.Point> getGeometryClass() {
            return org.apache.sis.geometries.Point.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialedition.ToPrimitive operation) throws OperationException {
            final org.apache.sis.geometries.mesh.MeshPrimitive primitive = new org.apache.sis.geometries.mesh.MeshPrimitive.Points();
            final AttributesType attributesType = operation.geometry.getAttributesType();
            for (String name : attributesType.getAttributeNames()) {
                final TupleArray array = TupleArrays.of(attributesType.getAttributeSystem(name), attributesType.getAttributeType(name), 1);
                array.set(0, ((org.apache.sis.geometries.Point) operation.geometry).getAttribute(name));
                primitive.setAttribute(name, array);
            }
            operation.result = primitive;
        }
    }

    /**
     * Transform LineString to Primitive.
     */
    public static class LineString implements Processor<org.apache.sis.geometries.operation.spatialedition.ToPrimitive, org.apache.sis.geometries.LineString>{

        @Override
        public Class<org.apache.sis.geometries.operation.spatialedition.ToPrimitive> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialedition.ToPrimitive.class;
        }

        @Override
        public Class<org.apache.sis.geometries.LineString> getGeometryClass() {
            return org.apache.sis.geometries.LineString.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialedition.ToPrimitive operation) throws OperationException {
            final org.apache.sis.geometries.mesh.MeshPrimitive primitive = new org.apache.sis.geometries.mesh.MeshPrimitive.LineStrip();
            final ArraySequence array = toArraySequence(((org.apache.sis.geometries.LineString) operation.geometry).getPoints());
            for (String name : array.getAttributeNames()) {
                primitive.setAttribute(name, array.getAttribute(name));
            }
            operation.result = primitive;
        }
    }

    /**
     * Transform Polygon to Primitive.
     */
    public static class Polygon implements Processor<org.apache.sis.geometries.operation.spatialedition.ToPrimitive, org.apache.sis.geometries.Polygon>{

        @Override
        public Class<org.apache.sis.geometries.operation.spatialedition.ToPrimitive> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialedition.ToPrimitive.class;
        }

        @Override
        public Class<org.apache.sis.geometries.Polygon> getGeometryClass() {
            return org.apache.sis.geometries.Polygon.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialedition.ToPrimitive operation) throws OperationException {
            //we must triangulate it
            operation.result = new EarClipping().toMesh((org.apache.sis.geometries.Polygon) operation.geometry);
        }
    }

    /**
     * Transform MultiPoint to Primitive.
     */
    public static class MultiPoint implements Processor<org.apache.sis.geometries.operation.spatialedition.ToPrimitive, org.apache.sis.geometries.MultiPoint>{

        @Override
        public Class<org.apache.sis.geometries.operation.spatialedition.ToPrimitive> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialedition.ToPrimitive.class;
        }

        @Override
        public Class<org.apache.sis.geometries.MultiPoint> getGeometryClass() {
            return org.apache.sis.geometries.MultiPoint.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialedition.ToPrimitive operation) throws OperationException {
            final org.apache.sis.geometries.MultiPoint cdt = (org.apache.sis.geometries.MultiPoint) operation.geometry;
            final ArraySequence array = toArraySequence(cdt.asPointSequence());
            final org.apache.sis.geometries.mesh.MeshPrimitive primitive = new org.apache.sis.geometries.mesh.MeshPrimitive.Points();
            for (String name : array.getAttributeNames()) {
                primitive.setAttribute(name, array.getAttribute(name));
            }
            operation.result = primitive;
        }
    }

    /**
     * Transform MultiLineString to Primitive.
     * If all LineString are Lines, the return a single Primitive.lines otherwise return a MultiPrimitive.
     */
    public static class MultiLineString implements Processor<org.apache.sis.geometries.operation.spatialedition.ToPrimitive, org.apache.sis.geometries.MultiLineString>{

        @Override
        public Class<org.apache.sis.geometries.operation.spatialedition.ToPrimitive> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialedition.ToPrimitive.class;
        }

        @Override
        public Class<org.apache.sis.geometries.MultiLineString> getGeometryClass() {
            return org.apache.sis.geometries.MultiLineString.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialedition.ToPrimitive operation) throws OperationException {
            final org.apache.sis.geometries.MultiLineString cdt = (org.apache.sis.geometries.MultiLineString) operation.geometry;
            boolean allLines = true;
            final int numGeometries = cdt.getNumGeometries();
            for (int i = 0; i < numGeometries && allLines; i++) {
                allLines &= cdt.getGeometryN(i).isLine();
            }

            if (allLines) {
                final org.apache.sis.geometries.mesh.MeshPrimitive.Lines primitive = new org.apache.sis.geometries.mesh.MeshPrimitive.Lines();
                final AttributesType attributesType = cdt.getAttributesType();
                for (String name : attributesType.getAttributeNames()) {
                    primitive.setAttribute(name, TupleArrays.of(attributesType.getAttributeSystem(name), attributesType.getAttributeType(name), numGeometries*2));
                }

                for (int i = 0, k = 0; i < numGeometries; i++, k += 2) {
                    final org.apache.sis.geometries.LineString line = cdt.getGeometryN(i);
                    final PointSequence points = line.getPoints();
                    for (String name : attributesType.getAttributeNames()) {
                        TupleArray att = primitive.getAttribute(name);
                        att.set(k, points.getAttribute(0, name));
                        att.set(k+1, points.getAttribute(1, name));
                    }
                }
                operation.result = primitive;
            } else {
                final org.apache.sis.geometries.mesh.MultiMeshPrimitive<org.apache.sis.geometries.mesh.MeshPrimitive> mp =
                        new org.apache.sis.geometries.mesh.MultiMeshPrimitive(operation.geometry.getCoordinateReferenceSystem());
                for (int i = 0; i < numGeometries; i++) {
                    final Geometry p = GeometryOperations.SpatialEdition.toPrimitive(cdt.getGeometryN(i));
                    if (p instanceof org.apache.sis.geometries.mesh.MultiMeshPrimitive<?> subm) {
                        mp.append(subm.getComponents());
                    } else if (p instanceof org.apache.sis.geometries.mesh.MeshPrimitive cd){
                        mp.append(Collections.singletonList(cd));
                    }
                }
                operation.result = mp;
            }
        }
    }

    /**
     * Transform GeometryCollection to Primitive.
     */
    public static class GeometryCollection implements Processor<org.apache.sis.geometries.operation.spatialedition.ToPrimitive, org.apache.sis.geometries.GeometryCollection>{

        @Override
        public Class<org.apache.sis.geometries.operation.spatialedition.ToPrimitive> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialedition.ToPrimitive.class;
        }

        @Override
        public Class<org.apache.sis.geometries.GeometryCollection> getGeometryClass() {
            return org.apache.sis.geometries.GeometryCollection.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialedition.ToPrimitive operation) throws OperationException {
            final org.apache.sis.geometries.mesh.MultiMeshPrimitive<org.apache.sis.geometries.mesh.MeshPrimitive> mp = new org.apache.sis.geometries.mesh.MultiMeshPrimitive(operation.geometry.getCoordinateReferenceSystem());
            final org.apache.sis.geometries.GeometryCollection cdt = (org.apache.sis.geometries.GeometryCollection) operation.geometry;
            for (int i = 0, n = cdt.getNumGeometries(); i < n; i++) {
                final Geometry p = GeometryOperations.SpatialEdition.toPrimitive(cdt.getGeometryN(i));
                if (p instanceof org.apache.sis.geometries.mesh.MultiMeshPrimitive<?> subm) {
                    mp.append(subm.getComponents());
                } else if (p instanceof org.apache.sis.geometries.mesh.MeshPrimitive cd){
                    mp.append(Collections.singletonList(cd));
                }
            }
            operation.result = mp;
        }
    }

    /**
     * Does nothing, geometry is already a Primitive.
     */
    public static class Primitive implements Processor<org.apache.sis.geometries.operation.spatialedition.ToPrimitive, org.apache.sis.geometries.mesh.MeshPrimitive>{

        @Override
        public Class<org.apache.sis.geometries.operation.spatialedition.ToPrimitive> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialedition.ToPrimitive.class;
        }

        @Override
        public Class<org.apache.sis.geometries.mesh.MeshPrimitive> getGeometryClass() {
            return org.apache.sis.geometries.mesh.MeshPrimitive.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialedition.ToPrimitive operation) throws OperationException {
            operation.result = (org.apache.sis.geometries.mesh.MeshPrimitive) operation.geometry;
        }
    }

    /**
     * Does nothing, geometry is already a MultiPrimitive.
     */
    public static class MultiPrimitive implements Processor<org.apache.sis.geometries.operation.spatialedition.ToPrimitive, org.apache.sis.geometries.mesh.MultiMeshPrimitive>{

        @Override
        public Class<org.apache.sis.geometries.operation.spatialedition.ToPrimitive> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialedition.ToPrimitive.class;
        }

        @Override
        public Class<org.apache.sis.geometries.mesh.MultiMeshPrimitive> getGeometryClass() {
            return org.apache.sis.geometries.mesh.MultiMeshPrimitive.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialedition.ToPrimitive operation) throws OperationException {
            operation.result = (org.apache.sis.geometries.mesh.MultiMeshPrimitive) operation.geometry;
        }
    }

}
