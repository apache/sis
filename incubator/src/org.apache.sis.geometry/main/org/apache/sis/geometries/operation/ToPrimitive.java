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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.MultiLineString;
import org.apache.sis.geometries.MultiPoint;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.Polygon;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.Array;
import org.apache.sis.geometries.internal.shared.ArrayDataPoints;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.mesh.MultiMeshPrimitive;
import org.apache.sis.geometries.operation.triangulate.EarClipping;
import org.apache.sis.geometries.DataPoints;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ToPrimitive {

    private ToPrimitive(){}

    private static ArrayDataPoints toArraySequence(DataPoints points) {
        final Map<String,Array> attributes = new HashMap<>();
        for (String name : points.getAttributesType().getAttributeNames()) {
            attributes.put(name, points.getAttributeArray(name));
        }
        return new ArrayDataPoints(attributes);
    }

    /**
     * Transform Point to Primitive.
     */
    public static MeshPrimitive.Points toPrimitive(Point geometry) throws OperationException {
        final MeshPrimitive.Points primitive = new MeshPrimitive.Points();
        final AttributesType attributesType = geometry.getAttributesType();
        for (String name : attributesType.getAttributeNames()) {
            final Array array = NDArrays.of(attributesType.getAttributeSystem(name), attributesType.getAttributeType(name), 1);
            array.set(0, geometry.getAttribute(name));
            primitive.setAttribute(name, array);
        }
        return primitive;
    }

    /**
     * Transform LineString to Primitive.
     */
    public static MeshPrimitive.LineStrip toPrimitive(LineString geometry) throws OperationException {
        final MeshPrimitive.LineStrip primitive = new MeshPrimitive.LineStrip();
        final ArrayDataPoints array = toArraySequence(geometry.getDataPoints());
        for (String name : array.getAttributeNames()) {
            primitive.setAttribute(name, array.getAttribute(name));
        }
        return primitive;
    }

    /**
     * Transform Polygon to Primitive.
     */
    public static MeshPrimitive.Triangles toPrimitive(Polygon geometry) throws OperationException {
        //we must triangulate it
        return new EarClipping().toMesh(geometry);
    }

    /**
     * Transform MultiPoint to Primitive.
     */
    public static MeshPrimitive.Points toPrimitive(MultiPoint geometry) throws OperationException {
        final ArrayDataPoints array = toArraySequence(geometry.asDataPoints());
        final MeshPrimitive.Points primitive = new MeshPrimitive.Points();
        for (String name : array.getAttributeNames()) {
            primitive.setAttribute(name, array.getAttribute(name));
        }
        return primitive;
    }

    /**
     * Transform MultiLineString to Primitive.
     * If all LineString are Lines, the return a single Primitive.lines otherwise return a MultiPrimitive.
     */
    public static Geometry toPrimitive(MultiLineString geometry) throws OperationException {
        boolean allLines = true;
        final int numGeometries = geometry.getNumGeometries();
        for (int i = 0; i < numGeometries && allLines; i++) {
            allLines &= geometry.getGeometryN(i).isLine();
        }

        if (allLines) {
            final MeshPrimitive.Lines primitive = new MeshPrimitive.Lines();
            final AttributesType attributesType = geometry.getAttributesType();
            for (String name : attributesType.getAttributeNames()) {
                primitive.setAttribute(name, NDArrays.of(attributesType.getAttributeSystem(name), attributesType.getAttributeType(name), numGeometries*2));
            }

            for (int i = 0, k = 0; i < numGeometries; i++, k += 2) {
                final org.apache.sis.geometries.curve.LineString line = geometry.getGeometryN(i);
                final DataPoints points = line.getDataPoints();
                for (String name : attributesType.getAttributeNames()) {
                    Array att = primitive.getAttribute(name);
                    att.set(k, points.getAttribute(0, name));
                    att.set(k+1, points.getAttribute(1, name));
                }
            }
            return primitive;
        } else {
            final MultiMeshPrimitive<MeshPrimitive> mp = new MultiMeshPrimitive(geometry.getCoordinateReferenceSystem());
            for (int i = 0; i < numGeometries; i++) {
                final Geometry p = toPrimitive(geometry.getGeometryN(i));
                if (p instanceof org.apache.sis.geometries.mesh.MultiMeshPrimitive<?> subm) {
                    mp.append(subm.getComponents());
                } else if (p instanceof org.apache.sis.geometries.mesh.MeshPrimitive cd){
                    mp.append(Collections.singletonList(cd));
                }
            }
            return mp;
        }
    }

    /**
     * Transform GeometryCollection to Primitive.
     */
    public static MultiMeshPrimitive toPrimitive(GeometryCollection geometry) throws OperationException {
        final MultiMeshPrimitive<MeshPrimitive> mp = new MultiMeshPrimitive(geometry.getCoordinateReferenceSystem());
        for (int i = 0, n = geometry.getNumGeometries(); i < n; i++) {
            final Geometry p = new GeometryProcessor().toPrimitive(geometry.getGeometryN(i));
            if (p instanceof org.apache.sis.geometries.mesh.MultiMeshPrimitive<?> subm) {
                mp.append(subm.getComponents());
            } else if (p instanceof org.apache.sis.geometries.mesh.MeshPrimitive cd){
                mp.append(Collections.singletonList(cd));
            }
        }
        return mp;
    }

}
