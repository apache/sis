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

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.LineString;
import org.apache.sis.geometries.LinearRing;
import org.apache.sis.geometries.MultiLineString;
import org.apache.sis.geometries.MultiPoint;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.Polygon;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.mesh.MultiMeshPrimitive;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.operation.GeometryOperations;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.math.Array;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class ToPrimitiveTest {

    private static final CoordinateReferenceSystem CRS2D = CommonCRS.WGS84.normalizedGeographic();

    @Test
    public void testPoint() {
        final Point point = GeometryFactory.createPoint(CRS2D, 1, 2);
        final Geometry result = GeometryOperations.SpatialEdition.toPrimitive(point);
        assertTrue(result instanceof MeshPrimitive);
        final MeshPrimitive primitive = (MeshPrimitive) result;
        assertEquals(MeshPrimitive.Type.POINTS, primitive.getType());
    }

    @Test
    public void testLineString() {
        final Array positions = NDArrays.of(CRS2D, new double[]{0,1,2,3});
        final PointSequence points = GeometryFactory.createSequence(positions);
        final LineString line = GeometryFactory.createLineString(points);
        final Geometry result = GeometryOperations.SpatialEdition.toPrimitive(line);
        assertTrue(result instanceof MeshPrimitive);
        final MeshPrimitive primitive = (MeshPrimitive) result;
        assertEquals(MeshPrimitive.Type.LINE_STRIP, primitive.getType());
    }

    @Test
    public void testPolygon() {
        final Array positions = NDArrays.of(CRS2D, new double[]{0,1,2,3});
        final PointSequence points = GeometryFactory.createSequence(positions);
        final LinearRing exterior = GeometryFactory.createLinearRing(points);
        final Polygon point = GeometryFactory.createPolygon(exterior, null);
        final Geometry result = GeometryOperations.SpatialEdition.toPrimitive(point);
        assertTrue(result instanceof MeshPrimitive);
        final MeshPrimitive primitive = (MeshPrimitive) result;
        assertEquals(MeshPrimitive.Type.TRIANGLES, primitive.getType());
    }

    @Test
    public void testMultiLineString() {
        { //one line has 3 points, so we must obtain a MultiPrimitive
            final Array positions1 = NDArrays.of(CRS2D, new double[]{0,1,2,3});
            final PointSequence points1 = GeometryFactory.createSequence(positions1);
            final LineString line1 = GeometryFactory.createLineString(points1);

            final Array positions2 = NDArrays.of(CRS2D, new double[]{3,4,5,6,7,8});
            final PointSequence points2 = GeometryFactory.createSequence(positions2);
            final LineString line2 = GeometryFactory.createLineString(points2);

            final MultiLineString mlines = GeometryFactory.createMultiLineString(line1, line2);

            final Geometry result = GeometryOperations.SpatialEdition.toPrimitive(mlines);
            assertTrue(result instanceof MultiMeshPrimitive);
            final MultiMeshPrimitive mp = (MultiMeshPrimitive) result;
            assertEquals(2, mp.getNumGeometries());
            assertTrue(mp.getGeometryN(0) instanceof MeshPrimitive);
            assertTrue(mp.getGeometryN(1) instanceof MeshPrimitive);

            final MeshPrimitive primitive1 = (MeshPrimitive) mp.getGeometryN(0);
            assertEquals(MeshPrimitive.Type.LINE_STRIP, primitive1.getType());

            final MeshPrimitive primitive2 = (MeshPrimitive) mp.getGeometryN(1);
            assertEquals(MeshPrimitive.Type.LINE_STRIP, primitive2.getType());
        }
        { //all linestrings are lines, we must obtain a Primitive.Lines
            final Array positions1 = NDArrays.of(CRS2D, new double[]{0,1,2,3});
            final PointSequence points1 = GeometryFactory.createSequence(positions1);
            final LineString line1 = GeometryFactory.createLineString(points1);

            final Array positions2 = NDArrays.of(CRS2D, new double[]{3,4,5,6});
            final PointSequence points2 = GeometryFactory.createSequence(positions2);
            final LineString line2 = GeometryFactory.createLineString(points2);

            final MultiLineString mlines = GeometryFactory.createMultiLineString(line1, line2);

            final Geometry result = GeometryOperations.SpatialEdition.toPrimitive(mlines);
            assertTrue(result instanceof MeshPrimitive.Lines);
            final MeshPrimitive.Lines mp = (MeshPrimitive.Lines) result;
            assertEquals(2, mp.getNumGeometries());
            LineString l1 = mp.getGeometryN(0);
            LineString l2 = mp.getGeometryN(1);
            assertArrayEquals(new double[]{0,1,2,3}, l1.getPoints().getAttributeArray(AttributesType.ATT_POSITION).toArrayDouble(), 0.0);
            assertArrayEquals(new double[]{3,4,5,6}, l2.getPoints().getAttributeArray(AttributesType.ATT_POSITION).toArrayDouble(), 0.0);
        }
    }

    @Test
    public void testMultiPoint() {
        final Array positions1 = NDArrays.of(CRS2D, new double[]{0,1,2,3});
        final PointSequence points = GeometryFactory.createSequence(positions1);
        final MultiPoint mpoints = GeometryFactory.createMultiPoint(points);

        final Geometry result = GeometryOperations.SpatialEdition.toPrimitive(mpoints);
        assertTrue(result instanceof MeshPrimitive);
        final MeshPrimitive primitive1 = (MeshPrimitive) result;
        assertEquals(MeshPrimitive.Type.POINTS, primitive1.getType());
    }
}
