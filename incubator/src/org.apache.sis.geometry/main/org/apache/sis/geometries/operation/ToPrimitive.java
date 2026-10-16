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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.measure.Quantity;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.internal.shared.ArrayDataPoints;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.mesh.MultiMeshPrimitive;
import org.apache.sis.geometries.operation.triangulate.EarClipping;
import org.apache.sis.geometries.solid.Polyhedron;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometries.surface.PolyhedralSurface;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.NDArrays;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ToPrimitive {

    private ToPrimitive(){}

    private static ArrayDataPoints toArraySequence(DataPoints points) {
        final Map<String,Array> attributes = new HashMap<>();
        for (String name : points.getType().getAttributeNames()) {
            attributes.put(name, points.getAttributeArray(name));
        }
        return new ArrayDataPoints(attributes);
    }

    /**
     * Transform Point to Primitive.
     */
    public static MeshPrimitive.Points toPrimitive(Point geometry) throws OperationException {
        final MeshPrimitive.Points primitive = new MeshPrimitive.Points();
        final DataPointsType attributesType = geometry.getDataPointsType();
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
     * Transform PolyhedralSurface, including TIN, to Primitive.
     * Each patch is triangulated and all triangles are merged in a single non-indexed mesh.
     */
    public static MeshPrimitive.Triangles toPrimitive(PolyhedralSurface<?> geometry) throws OperationException {
        final List<Polygon> faces = new ArrayList<>();
        for (int i = 0, n = geometry.getNumPatches(); i < n; i++) {
            faces.add(geometry.getPatchN(i));
        }
        return triangles(geometry.getCoordinateReferenceSystem(), faces);
    }

    /**
     * Transform a Polyhedron to a Triangles primitive.
     * The faces of the exterior shell and of all the interior shells are triangulated together
     * in a single mesh, each face keeping its orientation.
     */
    public static MeshPrimitive.Triangles toPrimitive(Polyhedron geometry) throws OperationException {
        final List<Polygon> faces = new ArrayList<>();
        final List<GeometryCollection<Polygon>> shells = new ArrayList<>();
        shells.add(geometry.getExteriorShell());
        shells.addAll(geometry.getInteriorShells());
        for (final GeometryCollection<Polygon> shell : shells) {
            for (int i = 0, n = shell.getNumGeometries(); i < n; i++) {
                faces.add(shell.getGeometryN(i));
            }
        }
        return triangles(geometry.getCoordinateReferenceSystem(), faces);
    }

    /**
     * Triangulates the given polygons into a single indexed mesh of triangles.
     * The mesh holds the attributes which are common to all the polygons.
     */
    private static MeshPrimitive.Triangles triangles(final CoordinateReferenceSystem crs, final List<Polygon> faces) {
        final EarClipping triangulator = new EarClipping();
        final List<EarClipping.Triangulation> triangulations = new ArrayList<>(faces.size());
        final Set<String> names = new LinkedHashSet<>();
        names.add(DataPointsType.ATT_POSITION);
        int vertexCount = 0, indexCount = 0;
        for (final Polygon face : faces) {
            final EarClipping.Triangulation triangulation = triangulator.triangulateIndexed(face);
            final List<String> faceNames = triangulation.vertices().getType().getAttributeNames();
            if (triangulations.isEmpty()) {
                names.addAll(faceNames);
            } else {
                names.retainAll(faceNames);
            }
            triangulations.add(triangulation);
            vertexCount += triangulation.vertices().size();
            indexCount  += triangulation.triangles().length;
        }
        /*
         * The vertices of all faces are appended in a single array for each attribute,
         * and the indices of each face are shifted by the number of vertices before it.
         */
        final MeshPrimitive.Triangles primitive = new MeshPrimitive.Triangles();
        if (triangulations.isEmpty()) {
            primitive.setPositions(NDArrays.of(crs, new double[0]));
            return primitive;
        }
        final DataPointsType type = triangulations.get(0).vertices().getType();
        final int[] index = new int[indexCount];
        for (final String name : names) {
            final Array array = NDArrays.of(type.getAttributeSystem(name), type.getAttributeType(name), vertexCount);
            int offset = 0;
            for (final EarClipping.Triangulation triangulation : triangulations) {
                final DataPoints vertices = triangulation.vertices();
                array.set(offset, vertices.getAttributeArray(name), 0, vertices.size());
                offset += vertices.size();
            }
            primitive.setAttribute(name, array);
        }
        int vertexOffset = 0, indexOffset = 0;
        for (final EarClipping.Triangulation triangulation : triangulations) {
            for (final int i : triangulation.triangles()) {
                index[indexOffset++] = vertexOffset + i;
            }
            vertexOffset += triangulation.vertices().size();
        }
        primitive.setIndex(NDArrays.packIntegerDataType(NDArrays.of(1, index)));
        return primitive;
    }

    /**
     * Transform MultiPoint to Primitive.
     */
    public static MeshPrimitive.Points toPrimitivePoint(GeometryCollection<Point> geometry) throws OperationException {
        final ArrayDataPoints array = toArraySequence(geometry.getDataPoints());
        final MeshPrimitive.Points primitive = new MeshPrimitive.Points();
        for (String name : array.getAttributeNames()) {
            primitive.setAttribute(name, array.getAttribute(name));
        }
        return primitive;
    }

    /**
     * Transform MultiLineString to Primitive.
     * If all LineString are Lines and no spacing is given, the return a single Primitive.lines
     * otherwise return a MultiPrimitive.
     *
     * @param spacing maximal distance between two points of a line strip, or zero or {@code null} for no limit.
     * @param offset  ignored for line strings, which are their own linear approximation.
     */
    public static Geometry toPrimitiveLine(GeometryCollection<LineString> geometry, Quantity<?> spacing, Quantity<?> offset)
            throws OperationException
    {
        boolean allLines = (spacing == null || spacing.getValue().doubleValue() == 0);
        final int numGeometries = geometry.getNumGeometries();
        for (int i = 0; i < numGeometries && allLines; i++) {
            allLines &= geometry.getGeometryN(i).isLine();
        }

        if (allLines) {
            final MeshPrimitive.Lines primitive = new MeshPrimitive.Lines();
            final DataPointsType attributesType = geometry.getDataPointsType();
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
                final Geometry p = toPrimitive(geometry.getGeometryN(i).asLine(spacing, offset));
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
     *
     * @param spacing maximal distance along a curve between two points of its line strip, or zero or {@code null}.
     * @param offset  maximal distance between a curve and its line strip, or zero or {@code null}.
     */
    public static MultiMeshPrimitive toPrimitive(GeometryCollection geometry, Quantity<?> spacing, Quantity<?> offset)
            throws OperationException
    {
        final MultiMeshPrimitive<MeshPrimitive> mp = new MultiMeshPrimitive(geometry.getCoordinateReferenceSystem());
        for (int i = 0, n = geometry.getNumGeometries(); i < n; i++) {
            final Geometry p = new GeometryProcessor().toPrimitive(geometry.getGeometryN(i), spacing, offset);
            if (p instanceof org.apache.sis.geometries.mesh.MultiMeshPrimitive<?> subm) {
                mp.append(subm.getComponents());
            } else if (p instanceof org.apache.sis.geometries.mesh.MeshPrimitive cd){
                mp.append(Collections.singletonList(cd));
            }
        }
        return mp;
    }

}
