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
package org.apache.sis.geometries.operation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.mesh.MeshPrimitiveVisitor;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.Array;
import org.apache.sis.geometries.math.DataType;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.mesh.MultiMeshPrimitive;


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
    public static MeshPrimitive compute(MeshPrimitive base, String attributeName, SampleSystem attributeSystem, DataType attributeType, Function<Point,Tuple> valueGenerator) {
        final org.apache.sis.geometries.mesh.MeshPrimitive copy3d = base.deepCopy();

        Array ta = copy3d.getAttribute(attributeName);
        if (ta == null) {
            ta = NDArrays.of(attributeSystem, attributeType, copy3d.getPositions().getLength());
            copy3d.setAttribute(attributeName, ta);
        }

        if (valueGenerator != null) {
            new MeshPrimitiveVisitor(copy3d) {
                @Override
                protected void visit(org.apache.sis.geometries.mesh.MeshPrimitive.Vertex vertex) {
                    vertex.setAttribute(attributeName, valueGenerator.apply(vertex));
                }
            }.visit();
        }

        return copy3d;
    }


    /**
     * Compute attribute on MultiPrimitive.
     * Also works for ModelPrimitive.
     */
    public static MultiMeshPrimitive compute(MultiMeshPrimitive base, String attributeName, SampleSystem attributeSystem, DataType attributeType, Function<Point,Tuple> valueGenerator) {
        final List<org.apache.sis.geometries.mesh.MeshPrimitive> news = new ArrayList<>();
        for (int i = 0, n = base.getNumGeometries(); i < n; i++) {
            Geometry p = base.getGeometryN(i);
            p = compute((MeshPrimitive)p, attributeName, attributeSystem, attributeType, valueGenerator);
            news.add((org.apache.sis.geometries.mesh.MeshPrimitive) p);
        }

        final org.apache.sis.geometries.mesh.MultiMeshPrimitive copy3d = new org.apache.sis.geometries.mesh.MultiMeshPrimitive(base.getCoordinateReferenceSystem());
        copy3d.append(news);

        return copy3d;
    }

}
