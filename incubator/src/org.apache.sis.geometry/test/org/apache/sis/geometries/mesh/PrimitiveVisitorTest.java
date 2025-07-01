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
package org.apache.sis.geometries.mesh;

import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.mesh.MeshPrimitiveVisitor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.LineString;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.TIN;
import org.apache.sis.geometries.Triangle;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometries.math.TupleArrays;
import org.apache.sis.geometries.math.Vector3D;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Tests for MeshGeometry visitor.
 *
 * @author Johann Sorel (Geomatys)
 */
public class PrimitiveVisitorTest {

    private static final CoordinateReferenceSystem CRS3D = Geometries.getUndefinedCRS(3);
    private static final SampleSystem SS3D = SampleSystem.of(CRS3D);


    /**
     * Test visiting Points.
     */
    @Test
    public void testPoints() {

        final MeshPrimitive.Points geometry = new MeshPrimitive.Points();
        geometry.setPositions(TupleArrays.of(CRS3D,
                0,1,2,
                3,4,5));

        final List<String> expected = Arrays.asList(
                "V:0 POSITION[0.0, 1.0, 2.0]",
                "V:0 POSITION[0.0, 1.0, 2.0]",
                "V:1 POSITION[3.0, 4.0, 5.0]",
                "V:1 POSITION[3.0, 4.0, 5.0]"
        );

        visit(geometry, expected);

        //test geometry collection
        assertEquals(2, geometry.getNumGeometries());
        assertEquals(new Vector3D.Double(SS3D, 0, 1, 2), geometry.getGeometryN(0).getPosition());
        assertEquals(new Vector3D.Double(SS3D, 3, 4, 5), geometry.getGeometryN(1).getPosition());
    }

    /**
     * Test visiting indexed Points.
     */
    @Test
    public void testIndexedPoints() {

        final MeshPrimitive.Points geometry = new MeshPrimitive.Points();
        geometry.setPositions(TupleArrays.of(CRS3D,
                0,1,2,
                3,4,5));
        geometry.setIndex(TupleArrays.ofUnsigned(1, 0, 1, 0));

        final List<String> expected = Arrays.asList(
                "V:0 POSITION[0.0, 1.0, 2.0]",
                "V:0 POSITION[0.0, 1.0, 2.0]",
                "V:1 POSITION[3.0, 4.0, 5.0]",
                "V:1 POSITION[3.0, 4.0, 5.0]",
                "V:0 POSITION[0.0, 1.0, 2.0]",
                "V:0 POSITION[0.0, 1.0, 2.0]"
        );

        visit(geometry, expected);

        //test geometry collection
        assertEquals(3, geometry.getNumGeometries());
        assertEquals(new Vector3D.Double(SS3D, 0, 1, 2), geometry.getGeometryN(0).getPosition());
        assertEquals(new Vector3D.Double(SS3D, 3, 4, 5), geometry.getGeometryN(1).getPosition());
        assertEquals(new Vector3D.Double(SS3D, 0, 1, 2), geometry.getGeometryN(2).getPosition());
    }

    /**
     * Test visiting Lines.
     */
    @Test
    public void testLines() {

        final MeshPrimitive.Lines geometry = new MeshPrimitive.Lines();
        geometry.setPositions(TupleArrays.of(CRS3D,
                0,1,2,
                3,4,5,
                6,7,8,
                9,10,11));

        final List<String> expected = Arrays.asList(
                "LINESTRING (0.0 1.0 2.0, 3.0 4.0 5.0)",
                "V:0 POSITION[0.0, 1.0, 2.0]",
                "V:1 POSITION[3.0, 4.0, 5.0]",
                "LINESTRING (6.0 7.0 8.0, 9.0 10.0 11.0)",
                "V:2 POSITION[6.0, 7.0, 8.0]",
                "V:3 POSITION[9.0, 10.0, 11.0]"
        );

        visit(geometry, expected);

        //test geometry collection
        assertEquals(2, geometry.getNumGeometries());
        assertEquals("LINESTRING (0.0 1.0 2.0, 3.0 4.0 5.0)", geometry.getGeometryN(0).asText());
        assertEquals("LINESTRING (6.0 7.0 8.0, 9.0 10.0 11.0)", geometry.getGeometryN(1).asText());
    }

    /**
     * Test visiting indexed Lines.
     */
    @Test
    public void testIndexedLines() {

        final MeshPrimitive.Lines geometry = new MeshPrimitive.Lines();
        geometry.setPositions(TupleArrays.of(CRS3D,
                0,1,2,
                3,4,5,
                6,7,8,
                9,10,11));
        geometry.setIndex(TupleArrays.ofUnsigned(1, 0, 1, 2, 3, 1, 3));

        final List<String> expected = Arrays.asList(
                "LINESTRING (0.0 1.0 2.0, 3.0 4.0 5.0)",
                "V:0 POSITION[0.0, 1.0, 2.0]",
                "V:1 POSITION[3.0, 4.0, 5.0]",
                "LINESTRING (6.0 7.0 8.0, 9.0 10.0 11.0)",
                "V:2 POSITION[6.0, 7.0, 8.0]",
                "V:3 POSITION[9.0, 10.0, 11.0]",
                "LINESTRING (3.0 4.0 5.0, 9.0 10.0 11.0)",
                "V:1 POSITION[3.0, 4.0, 5.0]",
                "V:3 POSITION[9.0, 10.0, 11.0]"
        );

        visit(geometry, expected);

        //test geometry collection
        assertEquals(3, geometry.getNumGeometries());
        assertEquals("LINESTRING (0.0 1.0 2.0, 3.0 4.0 5.0)", geometry.getGeometryN(0).asText());
        assertEquals("LINESTRING (6.0 7.0 8.0, 9.0 10.0 11.0)", geometry.getGeometryN(1).asText());
        assertEquals("LINESTRING (3.0 4.0 5.0, 9.0 10.0 11.0)", geometry.getGeometryN(2).asText());
    }

    /**
     * Test visiting Line strip.
     */
    @Test
    public void testLineStrip() {

        final MeshPrimitive.LineStrip geometry = new MeshPrimitive.LineStrip();
        geometry.setPositions(TupleArrays.of(CRS3D,
                0,1,2,
                3,4,5,
                6,7,8,
                9,10,11));

        final List<String> expected = Arrays.asList(
                "LINESTRING (0.0 1.0 2.0, 3.0 4.0 5.0)",
                "V:0 POSITION[0.0, 1.0, 2.0]",
                "V:1 POSITION[3.0, 4.0, 5.0]",
                "LINESTRING (3.0 4.0 5.0, 6.0 7.0 8.0)",
                "V:1 POSITION[3.0, 4.0, 5.0]",
                "V:2 POSITION[6.0, 7.0, 8.0]",
                "LINESTRING (6.0 7.0 8.0, 9.0 10.0 11.0)",
                "V:2 POSITION[6.0, 7.0, 8.0]",
                "V:3 POSITION[9.0, 10.0, 11.0]"
        );
        visit(geometry, expected);

        //test geometry as LineString
        assertTrue(geometry instanceof LineString);
        assertEquals("LINESTRING (0.0 1.0 2.0, 3.0 4.0 5.0, 6.0 7.0 8.0, 9.0 10.0 11.0)", geometry.asText());
    }

    /**
     * Test visiting indexed Line strip.
     */
    @Test
    public void testIndexedLineStrip() {

        final MeshPrimitive.LineStrip geometry = new MeshPrimitive.LineStrip();
        geometry.setPositions(TupleArrays.of(CRS3D,
                0,1,2,
                3,4,5,
                6,7,8,
                9,10,11));
        geometry.setIndex(TupleArrays.ofUnsigned(1, 0, 1, 2, 3, 0, 2));

        final List<String> expected = Arrays.asList(
                "LINESTRING (0.0 1.0 2.0, 3.0 4.0 5.0)",
                "V:0 POSITION[0.0, 1.0, 2.0]",
                "V:1 POSITION[3.0, 4.0, 5.0]",
                "LINESTRING (3.0 4.0 5.0, 6.0 7.0 8.0)",
                "V:1 POSITION[3.0, 4.0, 5.0]",
                "V:2 POSITION[6.0, 7.0, 8.0]",
                "LINESTRING (6.0 7.0 8.0, 9.0 10.0 11.0)",
                "V:2 POSITION[6.0, 7.0, 8.0]",
                "V:3 POSITION[9.0, 10.0, 11.0]",
                "LINESTRING (9.0 10.0 11.0, 0.0 1.0 2.0)",
                "V:3 POSITION[9.0, 10.0, 11.0]",
                "V:0 POSITION[0.0, 1.0, 2.0]",
                "LINESTRING (0.0 1.0 2.0, 6.0 7.0 8.0)",
                "V:0 POSITION[0.0, 1.0, 2.0]",
                "V:2 POSITION[6.0, 7.0, 8.0]"
        );
        visit(geometry, expected);

        //test geometry as LineString
        assertTrue(geometry instanceof LineString);
        assertEquals("LINESTRING (0.0 1.0 2.0, 3.0 4.0 5.0, 6.0 7.0 8.0, 9.0 10.0 11.0, 0.0 1.0 2.0, 6.0 7.0 8.0)", geometry.asText());
    }

    /**
     * Test visiting Triangles.
     */
    @Test
    public void testTriangles() {

        final MeshPrimitive.Triangles geometry = new MeshPrimitive.Triangles();
        geometry.setPositions(TupleArrays.of(CRS3D,
                0,1,2,
                3,4,5,
                6,7,8,
                9,10,11,
                12,13,14,
                15,16,17));

        final List<String> expected = Arrays.asList(
                "TRIANGLE ((0.0 1.0 2.0,3.0 4.0 5.0,6.0 7.0 8.0))",
                "V:0 POSITION[0.0, 1.0, 2.0]",
                "V:1 POSITION[3.0, 4.0, 5.0]",
                "V:2 POSITION[6.0, 7.0, 8.0]",
                "TRIANGLE ((9.0 10.0 11.0,12.0 13.0 14.0,15.0 16.0 17.0))",
                "V:3 POSITION[9.0, 10.0, 11.0]",
                "V:4 POSITION[12.0, 13.0, 14.0]",
                "V:5 POSITION[15.0, 16.0, 17.0]"
        );

        visit(geometry, expected);

        //test geometry as TIN
        assertTrue(geometry instanceof TIN);
        assertEquals(2, geometry.getNumPatches());
        assertEquals("TRIANGLE ((0.0 1.0 2.0,3.0 4.0 5.0,6.0 7.0 8.0))", geometry.getPatchN(0).asText());
        assertEquals("TRIANGLE ((9.0 10.0 11.0,12.0 13.0 14.0,15.0 16.0 17.0))", geometry.getPatchN(1).asText());
    }

    /**
     * Test visiting indexed Triangles.
     */
    @Test
    public void testIndexedTriangles() {

        final MeshPrimitive.Triangles geometry = new MeshPrimitive.Triangles();
        geometry.setPositions(TupleArrays.of(CRS3D,
                0,1,2,
                3,4,5,
                6,7,8,
                9,10,11));
        geometry.setIndex(TupleArrays.ofUnsigned(1, 0, 1, 2, 2, 3, 1));

        final List<String> expected = Arrays.asList(
                "TRIANGLE ((0.0 1.0 2.0,3.0 4.0 5.0,6.0 7.0 8.0))",
                "V:0 POSITION[0.0, 1.0, 2.0]",
                "V:1 POSITION[3.0, 4.0, 5.0]",
                "V:2 POSITION[6.0, 7.0, 8.0]",
                "TRIANGLE ((6.0 7.0 8.0,9.0 10.0 11.0,3.0 4.0 5.0))",
                "V:2 POSITION[6.0, 7.0, 8.0]",
                "V:3 POSITION[9.0, 10.0, 11.0]",
                "V:1 POSITION[3.0, 4.0, 5.0]"
        );

        visit(geometry, expected);

        //test geometry as TIN
        assertTrue(geometry instanceof TIN);
        assertEquals(2, geometry.getNumPatches());
        assertEquals("TRIANGLE ((0.0 1.0 2.0,3.0 4.0 5.0,6.0 7.0 8.0))", geometry.getPatchN(0).asText());
        assertEquals("TRIANGLE ((6.0 7.0 8.0,9.0 10.0 11.0,3.0 4.0 5.0))", geometry.getPatchN(1).asText());
    }

    /**
     * Test visiting Triangle Fan.
     * Triangles index is clockwise.
     *
     *   1  2
     *   +--+
     *   | /|
     *   |/ |
     * 0 +--+ 3
     *   |\ |
     *   | \|
     *   +--+
     *   5  4
     */
    @Test
    public void testTriangleFan() {

        final MeshPrimitive.TriangleFan geometry = new MeshPrimitive.TriangleFan();
        geometry.setPositions(TupleArrays.of(CRS3D,
                0, 0,0,
                0, 1,0,
                1, 1,0,
                1, 0,0,
                1,-1,0,
                0,-1,0));

        final List<String> expected = Arrays.asList(
                "TRIANGLE ((0.0 0.0 0.0,0.0 1.0 0.0,1.0 1.0 0.0))",
                "V:0 POSITION[0.0, 0.0, 0.0]",
                "V:1 POSITION[0.0, 1.0, 0.0]",
                "V:2 POSITION[1.0, 1.0, 0.0]",
                "TRIANGLE ((0.0 0.0 0.0,1.0 1.0 0.0,1.0 0.0 0.0))",
                "V:0 POSITION[0.0, 0.0, 0.0]",
                "V:2 POSITION[1.0, 1.0, 0.0]",
                "V:3 POSITION[1.0, 0.0, 0.0]",
                "TRIANGLE ((0.0 0.0 0.0,1.0 0.0 0.0,1.0 -1.0 0.0))",
                "V:0 POSITION[0.0, 0.0, 0.0]",
                "V:3 POSITION[1.0, 0.0, 0.0]",
                "V:4 POSITION[1.0, -1.0, 0.0]",
                "TRIANGLE ((0.0 0.0 0.0,1.0 -1.0 0.0,0.0 -1.0 0.0))",
                "V:0 POSITION[0.0, 0.0, 0.0]",
                "V:4 POSITION[1.0, -1.0, 0.0]",
                "V:5 POSITION[0.0, -1.0, 0.0]"
        );

        visit(geometry, expected);

        //test geometry as TIN
        assertTrue(geometry instanceof TIN);
        assertEquals(4, geometry.getNumPatches());
        assertEquals("TRIANGLE ((0.0 0.0 0.0,0.0 1.0 0.0,1.0 1.0 0.0))", geometry.getPatchN(0).asText());
        assertEquals("TRIANGLE ((0.0 0.0 0.0,1.0 1.0 0.0,1.0 0.0 0.0))", geometry.getPatchN(1).asText());
        assertEquals("TRIANGLE ((0.0 0.0 0.0,1.0 0.0 0.0,1.0 -1.0 0.0))", geometry.getPatchN(2).asText());
        assertEquals("TRIANGLE ((0.0 0.0 0.0,1.0 -1.0 0.0,0.0 -1.0 0.0))", geometry.getPatchN(3).asText());
    }

    /**
     * Test visiting Triangle Fan.
     * Triangles index is clockwise.
     *
     *   1  2
     *   +--+
     *   | /|
     *   |/ |
     * 0 +--+ 3
     *   |\ |
     *   | \|
     *   +--+
     *   5  4
     */
    @Test
    public void testIndexedTriangleFan() {

        final MeshPrimitive.TriangleFan geometry = new MeshPrimitive.TriangleFan();
        geometry.setPositions(TupleArrays.of(CRS3D,
                0, 0,0,
                0, 1,0,
                1, 1,0,
                1, 0,0,
                1,-1,0,
                0,-1,0));
        geometry.setIndex(TupleArrays.ofUnsigned(1,
                0, 1, 2, 3, 4, 5, 2));

        final List<String> expected = Arrays.asList(
                "TRIANGLE ((0.0 0.0 0.0,0.0 1.0 0.0,1.0 1.0 0.0))",
                "V:0 POSITION[0.0, 0.0, 0.0]",
                "V:1 POSITION[0.0, 1.0, 0.0]",
                "V:2 POSITION[1.0, 1.0, 0.0]",
                "TRIANGLE ((0.0 0.0 0.0,1.0 1.0 0.0,1.0 0.0 0.0))",
                "V:0 POSITION[0.0, 0.0, 0.0]",
                "V:2 POSITION[1.0, 1.0, 0.0]",
                "V:3 POSITION[1.0, 0.0, 0.0]",
                "TRIANGLE ((0.0 0.0 0.0,1.0 0.0 0.0,1.0 -1.0 0.0))",
                "V:0 POSITION[0.0, 0.0, 0.0]",
                "V:3 POSITION[1.0, 0.0, 0.0]",
                "V:4 POSITION[1.0, -1.0, 0.0]",
                "TRIANGLE ((0.0 0.0 0.0,1.0 -1.0 0.0,0.0 -1.0 0.0))",
                "V:0 POSITION[0.0, 0.0, 0.0]",
                "V:4 POSITION[1.0, -1.0, 0.0]",
                "V:5 POSITION[0.0, -1.0, 0.0]",
                "TRIANGLE ((0.0 0.0 0.0,0.0 -1.0 0.0,1.0 1.0 0.0))",
                "V:0 POSITION[0.0, 0.0, 0.0]",
                "V:5 POSITION[0.0, -1.0, 0.0]",
                "V:2 POSITION[1.0, 1.0, 0.0]"
        );

        visit(geometry, expected);

        //test geometry as TIN
        assertTrue(geometry instanceof TIN);
        assertEquals(5, geometry.getNumPatches());
        assertEquals("TRIANGLE ((0.0 0.0 0.0,0.0 1.0 0.0,1.0 1.0 0.0))", geometry.getPatchN(0).asText());
        assertEquals("TRIANGLE ((0.0 0.0 0.0,1.0 1.0 0.0,1.0 0.0 0.0))", geometry.getPatchN(1).asText());
        assertEquals("TRIANGLE ((0.0 0.0 0.0,1.0 0.0 0.0,1.0 -1.0 0.0))", geometry.getPatchN(2).asText());
        assertEquals("TRIANGLE ((0.0 0.0 0.0,1.0 -1.0 0.0,0.0 -1.0 0.0))", geometry.getPatchN(3).asText());
        assertEquals("TRIANGLE ((0.0 0.0 0.0,0.0 -1.0 0.0,1.0 1.0 0.0))", geometry.getPatchN(4).asText());
    }

    /**
     * Test visiting Triangle Strip.
     * Triangles index is clockwise.
     *
     *   1  3  5  7
     *   +--+--+--+
     *   |\ |\ |\ |
     *   | \| \| \|
     *   +--+--+--+
     *   0  2  4  6
     */
    @Test
    public void testTriangleStrip() {

        final MeshPrimitive.TriangleStrip geometry = new MeshPrimitive.TriangleStrip();
        geometry.setPositions(TupleArrays.of(CRS3D,
                0,0,0,
                0,1,0,
                1,0,0,
                1,1,0,
                2,0,0,
                2,1,0,
                3,0,0,
                3,1,0));

        final List<String> expected = Arrays.asList(
                "TRIANGLE ((0.0 0.0 0.0,0.0 1.0 0.0,1.0 0.0 0.0))",
                "V:0 POSITION[0.0, 0.0, 0.0]",
                "V:1 POSITION[0.0, 1.0, 0.0]",
                "V:2 POSITION[1.0, 0.0, 0.0]",
                "TRIANGLE ((1.0 0.0 0.0,0.0 1.0 0.0,1.0 1.0 0.0))",
                "V:2 POSITION[1.0, 0.0, 0.0]",
                "V:1 POSITION[0.0, 1.0, 0.0]",
                "V:3 POSITION[1.0, 1.0, 0.0]",
                "TRIANGLE ((1.0 0.0 0.0,1.0 1.0 0.0,2.0 0.0 0.0))",
                "V:2 POSITION[1.0, 0.0, 0.0]",
                "V:3 POSITION[1.0, 1.0, 0.0]",
                "V:4 POSITION[2.0, 0.0, 0.0]",
                "TRIANGLE ((2.0 0.0 0.0,1.0 1.0 0.0,2.0 1.0 0.0))",
                "V:4 POSITION[2.0, 0.0, 0.0]",
                "V:3 POSITION[1.0, 1.0, 0.0]",
                "V:5 POSITION[2.0, 1.0, 0.0]",
                "TRIANGLE ((2.0 0.0 0.0,2.0 1.0 0.0,3.0 0.0 0.0))",
                "V:4 POSITION[2.0, 0.0, 0.0]",
                "V:5 POSITION[2.0, 1.0, 0.0]",
                "V:6 POSITION[3.0, 0.0, 0.0]",
                "TRIANGLE ((3.0 0.0 0.0,2.0 1.0 0.0,3.0 1.0 0.0))",
                "V:6 POSITION[3.0, 0.0, 0.0]",
                "V:5 POSITION[2.0, 1.0, 0.0]",
                "V:7 POSITION[3.0, 1.0, 0.0]"
        );

        visit(geometry, expected);

        //test geometry as TIN
        assertTrue(geometry instanceof TIN);
        assertEquals(6, geometry.getNumPatches());
        assertEquals("TRIANGLE ((0.0 0.0 0.0,0.0 1.0 0.0,1.0 0.0 0.0))", geometry.getPatchN(0).asText());
        assertEquals("TRIANGLE ((1.0 0.0 0.0,0.0 1.0 0.0,1.0 1.0 0.0))", geometry.getPatchN(1).asText());
        assertEquals("TRIANGLE ((1.0 0.0 0.0,1.0 1.0 0.0,2.0 0.0 0.0))", geometry.getPatchN(2).asText());
        assertEquals("TRIANGLE ((2.0 0.0 0.0,1.0 1.0 0.0,2.0 1.0 0.0))", geometry.getPatchN(3).asText());
        assertEquals("TRIANGLE ((2.0 0.0 0.0,2.0 1.0 0.0,3.0 0.0 0.0))", geometry.getPatchN(4).asText());
        assertEquals("TRIANGLE ((3.0 0.0 0.0,2.0 1.0 0.0,3.0 1.0 0.0))", geometry.getPatchN(5).asText());
    }

    /**
     * Test visiting Triangle Strip.
     * Triangles index is clockwise.
     *
     *   1  3  5
     *   +--+--+
     *   |\ |\ |\
     *   | \| \| \
     *   +--+--+--+
     *   0  2  4  6
     */
    @Test
    public void testIndexedTriangleStrip() {

        final MeshPrimitive.TriangleStrip geometry = new MeshPrimitive.TriangleStrip();
        geometry.setPositions(TupleArrays.of(CRS3D,
                0,0,0,
                0,1,0,
                1,0,0,
                1,1,0,
                2,0,0,
                2,1,0,
                3,0,0,
                3,1,0));
        geometry.setIndex(TupleArrays.ofUnsigned(1,
                0, 1, 2, 3, 4, 5, 6, 3));

        final List<String> expected = Arrays.asList(
                "TRIANGLE ((0.0 0.0 0.0,0.0 1.0 0.0,1.0 0.0 0.0))",
                "V:0 POSITION[0.0, 0.0, 0.0]",
                "V:1 POSITION[0.0, 1.0, 0.0]",
                "V:2 POSITION[1.0, 0.0, 0.0]",
                "TRIANGLE ((1.0 0.0 0.0,0.0 1.0 0.0,1.0 1.0 0.0))",
                "V:2 POSITION[1.0, 0.0, 0.0]",
                "V:1 POSITION[0.0, 1.0, 0.0]",
                "V:3 POSITION[1.0, 1.0, 0.0]",
                "TRIANGLE ((1.0 0.0 0.0,1.0 1.0 0.0,2.0 0.0 0.0))",
                "V:2 POSITION[1.0, 0.0, 0.0]",
                "V:3 POSITION[1.0, 1.0, 0.0]",
                "V:4 POSITION[2.0, 0.0, 0.0]",
                "TRIANGLE ((2.0 0.0 0.0,1.0 1.0 0.0,2.0 1.0 0.0))",
                "V:4 POSITION[2.0, 0.0, 0.0]",
                "V:3 POSITION[1.0, 1.0, 0.0]",
                "V:5 POSITION[2.0, 1.0, 0.0]",
                "TRIANGLE ((2.0 0.0 0.0,2.0 1.0 0.0,3.0 0.0 0.0))",
                "V:4 POSITION[2.0, 0.0, 0.0]",
                "V:5 POSITION[2.0, 1.0, 0.0]",
                "V:6 POSITION[3.0, 0.0, 0.0]",
                "TRIANGLE ((3.0 0.0 0.0,2.0 1.0 0.0,1.0 1.0 0.0))",
                "V:6 POSITION[3.0, 0.0, 0.0]",
                "V:5 POSITION[2.0, 1.0, 0.0]",
                "V:3 POSITION[1.0, 1.0, 0.0]"
        );

        visit(geometry, expected);

        //test geometry as TIN
        assertTrue(geometry instanceof TIN);
        assertEquals(6, geometry.getNumPatches());
        assertEquals("TRIANGLE ((0.0 0.0 0.0,0.0 1.0 0.0,1.0 0.0 0.0))", geometry.getPatchN(0).asText());
        assertEquals("TRIANGLE ((1.0 0.0 0.0,0.0 1.0 0.0,1.0 1.0 0.0))", geometry.getPatchN(1).asText());
        assertEquals("TRIANGLE ((1.0 0.0 0.0,1.0 1.0 0.0,2.0 0.0 0.0))", geometry.getPatchN(2).asText());
        assertEquals("TRIANGLE ((2.0 0.0 0.0,1.0 1.0 0.0,2.0 1.0 0.0))", geometry.getPatchN(3).asText());
        assertEquals("TRIANGLE ((2.0 0.0 0.0,2.0 1.0 0.0,3.0 0.0 0.0))", geometry.getPatchN(4).asText());
        assertEquals("TRIANGLE ((3.0 0.0 0.0,2.0 1.0 0.0,1.0 1.0 0.0))", geometry.getPatchN(5).asText());
    }

    private void visit(MeshPrimitive geometry, List<String> exp) {
        final List<String> expected = new ArrayList<>(exp);

        final MeshPrimitiveVisitor visitor = new MeshPrimitiveVisitor(geometry) {
            @Override
            protected void visit(Triangle candidate) {
                assertEquals(expected.remove(0), candidate.toString());
                super.visit(candidate);
            }

            @Override
            protected void visit(LineString candidate) {
                assertEquals(expected.remove(0), candidate.toString());
                super.visit(candidate);
            }

            @Override
            protected void visit(Point candidate) {
                assertEquals(expected.remove(0), candidate.toString());
                super.visit(candidate);
            }

            @Override
            protected void visit(MeshPrimitive.Vertex vertex) {
                assertEquals(expected.remove(0), vertex.toString());
            }
        };
        visitor.visit();

        assertEquals(0, expected.size());
    }

}
