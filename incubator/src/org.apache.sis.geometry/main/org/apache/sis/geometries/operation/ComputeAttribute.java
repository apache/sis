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

import java.util.function.Function;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.internal.shared.ArrayDataPoints;
import org.apache.sis.geometries.internal.shared.Vertex;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.mesh.MeshPrimitiveVisitor;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Vector;


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
    public static MeshPrimitive compute(MeshPrimitive base, String attributeName, SampleSystem attributeSystem, DataType attributeType, Function<Point,Vector> valueGenerator) {
        final org.apache.sis.geometries.mesh.MeshPrimitive copy3d = base.deepCopy();

        Array ta = copy3d.getAttribute(attributeName);
        if (ta == null) {
            ta = NDArrays.of(attributeSystem, attributeType, copy3d.getPositions().getLength());
            copy3d.setAttribute(attributeName, ta);
        }

        if (valueGenerator != null) {
            new MeshPrimitiveVisitor(copy3d) {
                @Override
                protected void visit(Vertex vertex) {
                    vertex.setAttribute(attributeName, valueGenerator.apply(vertex));
                }
            }.visit();
        }

        return copy3d;
    }


    /**
     * Compute attribute on each primitive of a collection.
     *
     * @throws UnsupportedOperationException if an element of the collection is not a mesh primitive.
     */
    public static GeometryCollection<MeshPrimitive> compute(GeometryCollection<?> base, String attributeName, SampleSystem attributeSystem, DataType attributeType, Function<Point,Vector> valueGenerator) {
        final MeshPrimitive[] news = new MeshPrimitive[base.getNumGeometries()];
        for (int i = 0; i < news.length; i++) {
            final Geometry p = base.getGeometryN(i);
            if (!(p instanceof MeshPrimitive mp)) {
                throw new UnsupportedOperationException("Collection element is not a mesh primitive : " + p.getGeometryType());
            }
            news[i] = compute(mp, attributeName, attributeSystem, attributeType, valueGenerator);
        }
        return GeometryFactory.DEFAULT.createGeometryCollection(base.getCoordinateReferenceSystem(), news);
    }

}
