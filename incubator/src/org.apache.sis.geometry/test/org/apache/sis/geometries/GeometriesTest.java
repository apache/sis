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
package org.apache.sis.geometries;

import java.util.Arrays;
import java.util.List;
import org.apache.sis.geometries.math.TupleArrays;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.FactoryException;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class GeometriesTest {

    /**
     * Test creation of undefined CRS.
     */
    @Test
    public void testUndefinedCrs() {

        for (int i = 1; i < 10; i++) {
            CoordinateReferenceSystem crs = Geometries.getUndefinedCRS(i);

            assertTrue(Geometries.isUndefined(crs));

            try {
                CRS.findOperation(crs, CommonCRS.WGS84.geographic(), null);
                fail("Conversion to geographic crs should have fail");
            } catch (FactoryException ex) {
                //ok
            }
        }
    }

    /**
     * Test transform between cartesian crs
     */
    @Test
    @Disabled
    public void testCartesianCrsTransform() throws FactoryException {

        MathTransform trs = CRS.findOperation(Geometries.RIGHT_HAND_3D, Geometries.PSEUDOGEO_3D, null).getMathTransform();
        System.out.println(trs);

    }

    /**
     * Test concatenating triangles.
     */
    @Test
    public void testConcatenateTriangles() {

        final MeshPrimitive ts0 = new MeshPrimitive.Triangles();
        ts0.setIndex(TupleArrays.ofUnsigned(1, new int[]{0,1,2}));
        ts0.setPositions(TupleArrays.of(Geometries.getUndefinedCRS(3), new float[]{
            0,1,0,
            2,3,0,
            4,5,0,
            -1,-1,-1, //an unused position, must be added anyway, not the algo problem at this point
            -1,-1,-1 //an unused position, must be added anyway, not the algo problem at this point
        }));

        final MeshPrimitive ts1 = new MeshPrimitive.Triangles();
        ts1.setIndex(TupleArrays.ofUnsigned(1, new int[]{0,1,2}));
        ts1.setPositions(TupleArrays.of(Geometries.getUndefinedCRS(3), new float[]{
            0,1,0,
            2,3,0,
            4,5,0,
            -1,-1,-1 //an unused position, must be added anyway, not the algo problem at this point
        }));

        final MeshPrimitive ts2 = new MeshPrimitive.Triangles();
        ts2.setIndex(TupleArrays.ofUnsigned(1, new int[]{0,1,2}));
        ts2.setPositions(TupleArrays.of(Geometries.getUndefinedCRS(3), new float[]{
            0,1,0,
            2,3,0,
            4,5,0,
            -1,-1,-1 //an unused position, must be added anyway, not the algo problem at this point
        }));

        final MeshPrimitive ts = Geometries.concatenate(Arrays.asList(ts0, ts1, ts2));

        assertArrayEquals(new int[]{
            0,1,2, // +2 unused positions
            5,6,7, // +1 unused position
            9,10,11
        }, ts.getIndex().toArrayInt());

        ts.validate();
    }

    /**
     * Test concatenating triangle strips.
     * Strips must be joined by a degenerated triangle.
     */
    @Test
    public void testConcatenateTriangleStrip() {

        final MeshPrimitive ts0 = new MeshPrimitive.TriangleStrip();
        ts0.setIndex(TupleArrays.ofUnsigned(1, new int[]{0,1,2,3}));
        ts0.setPositions(TupleArrays.of(Geometries.getUndefinedCRS(3), new float[]{
            0,1,0,
            2,3,0,
            4,5,0,
            5,6,0
        }));

        final MeshPrimitive ts1 = new MeshPrimitive.TriangleStrip();
        ts1.setIndex(TupleArrays.ofUnsigned(1, new int[]{0,1,2,3}));
        ts1.setPositions(TupleArrays.of(Geometries.getUndefinedCRS(3), new float[]{
            0,1,0,
            2,3,0,
            4,5,0,
            5,6,0
        }));

        final MeshPrimitive ts2 = new MeshPrimitive.TriangleStrip();
        ts2.setIndex(TupleArrays.ofUnsigned(1, new int[]{0,1,2,3}));
        ts2.setPositions(TupleArrays.of(Geometries.getUndefinedCRS(3), new float[]{
            0,1,0,
            2,3,0,
            4,5,0,
            5,6,0
        }));

        final MeshPrimitive ts = Geometries.concatenate(Arrays.asList(ts0, ts1, ts2));

        assertArrayEquals(new int[]{
            0,1,2,3,
            3,4, //linking degenerated triangle
            4,5,6,7,
            7,8, //linking degenerated triangle
            8,9,10,11
        }, ts.getIndex().toArrayInt());

        ts.validate();
    }

    /**
     * Test concatenating triangle strips.
     * strips reuse the same vertices in this test
     * Strips must be joined by a degenerated triangle.
     */
    @Test
    public void testConcatenateTriangleStrip2() {

        final MeshPrimitive ts0 = new MeshPrimitive.TriangleStrip();
        ts0.setIndex(TupleArrays.ofUnsigned(1, new int[]{0,1,2,1}));
        ts0.setPositions(TupleArrays.of(Geometries.getUndefinedCRS(3), new float[]{
            0,1,0,
            2,3,0,
            4,5,0
        }));

        final MeshPrimitive ts1 = new MeshPrimitive.TriangleStrip();
        ts1.setIndex(TupleArrays.ofUnsigned(1, new int[]{0,1,0,0}));
        ts1.setPositions(TupleArrays.of(Geometries.getUndefinedCRS(3), new float[]{
            0,1,0,
            2,3,0
        }));

        final MeshPrimitive ts2 = new MeshPrimitive.TriangleStrip();
        ts2.setIndex(TupleArrays.ofUnsigned(1, new int[]{0,1,1,2}));
        ts2.setPositions(TupleArrays.of(Geometries.getUndefinedCRS(3), new float[]{
            0,1,0,
            2,3,0,
            4,5,0
        }));

        final MeshPrimitive ts = Geometries.concatenate(Arrays.asList(ts0, ts1, ts2));

        assertArrayEquals(new int[]{
            0,1,2,1,
            1,3, //linking degenerated triangle
            3,4,3,3,
            3,5, //linking degenerated triangle
            5,6,6,7
        }, ts.getIndex().toArrayInt());

        ts.validate();
    }

    /**
     * Test splitting triangles.
     */
    @Test
    public void testSplitTriangles() {

        final MeshPrimitive ts0 = new MeshPrimitive.Triangles();
        ts0.setIndex(TupleArrays.ofUnsigned(1, new int[]{0,2,3, 3,0,2}));
        ts0.setPositions(TupleArrays.of(Geometries.getUndefinedCRS(3), new float[]{
            0,1,0,
            -1,-1,-1, //an unused position, must be added anyway, not the algo problem at this point
            2,3,0,
            4,5,0,
            -1,-1,-1 //an unused position, must be added anyway, not the algo problem at this point
        }));

        final List<MeshPrimitive> primitives = Geometries.split(ts0, 3);
        assertEquals(2, primitives.size());
        final MeshPrimitive p0 = primitives.get(0);
        final MeshPrimitive p1 = primitives.get(1);

        assertEquals(1, p0.getAttributesType().getAttributeNames().size());
        assertEquals(1, p1.getAttributesType().getAttributeNames().size());
        assertEquals(MeshPrimitive.Type.TRIANGLES, p0.getType());
        assertEquals(MeshPrimitive.Type.TRIANGLES, p1.getType());

        assertArrayEquals(new int[]{0,1,2}, p0.getIndex().toArrayInt());
        assertArrayEquals(new int[]{0,1,2}, p1.getIndex().toArrayInt());
        assertArrayEquals(new int[]{0,1,0, 2,3,0, 4,5,0}, p0.getPositions().toArrayInt());
        assertArrayEquals(new int[]{4,5,0, 0,1,0, 2,3,0}, p1.getPositions().toArrayInt());
    }
}
