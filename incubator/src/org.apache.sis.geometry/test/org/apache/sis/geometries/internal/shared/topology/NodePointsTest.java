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
package org.apache.sis.geometries.internal.shared.topology;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.NDArrays;
import static org.apache.sis.geometries.internal.shared.topology.ComplexBuilderTest.build;
import static org.apache.sis.geometries.internal.shared.topology.ComplexBuilderTest.polygon;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link NodePoints}: the attributes of the geometries are kept by the nodes of a complex,
 * by the geometric realizations of the primitives, and by the edits.
 *
 * @author Johann Sorel (Geomatys)
 */
public class NodePointsTest {

    private static final double EPS = 1E-12;

    private static final CoordinateReferenceSystem CRS2D = Geometries.getUndefinedCRS(2);

    /**
     * Name of an attribute of one component, interpolated linearly.
     */
    private static final String TEMPERATURE = "TEMPERATURE";

    /**
     * Returns a triangle mesh of the given positions, with one temperature per vertex.
     */
    private static MeshPrimitive mesh(final double[] positions, final double[] temperatures, final int... index) {
        final MeshPrimitive mesh = MeshPrimitive.create(MeshPrimitive.Type.TRIANGLES);
        mesh.setPositions(NDArrays.of(CRS2D, positions));
        mesh.setAttribute(TEMPERATURE, NDArrays.of(1, temperatures));
        mesh.setIndex(NDArrays.ofUnsigned(1, index));
        return mesh;
    }

    /**
     * Returns a point with a temperature.
     */
    private static Point point(final double x, final double y, final double temperature) {
        final Map<String, Array> attributes = new LinkedHashMap<>();
        attributes.put(DataPointsType.ATT_POSITION, NDArrays.of(CRS2D, new double[] {x, y}));
        attributes.put(TEMPERATURE, NDArrays.of(1, new double[] {temperature}));
        final GeometryFactory f = GeometryFactory.DEFAULT;
        return f.createPoint(f.createDataPoints(attributes));
    }

    /**
     * Returns the temperature of the given node.
     */
    private static double temperature(final IndexedComplex complex, final int node) {
        return complex.getNodePoints().getAttribute(node, TEMPERATURE).get(0);
    }

    /**
     * The attributes of a mesh are kept by the nodes and by the realizations of the faces,
     * and interpolated when an edge is split.
     */
    @Test
    public void testMeshAttributes() {
        // The unit square as two triangles, the temperature being 10x + y.
        final IndexedComplex complex = build(mesh(new double[] {0,0, 1,0, 1,1, 0,1}, new double[] {0, 10, 11, 1}, 0,1,2, 0,2,3));
        final NodePoints points = complex.getNodePoints();
        assertEquals(List.of(DataPointsType.ATT_POSITION, TEMPERATURE), points.getAttributeNames());
        assertEquals(4, points.size());
        for (int i = 0; i < 4; i++) {
            final double[] c = complex.getCoordinates(i);
            assertEquals(10*c[0] + c[1], temperature(complex, i), EPS);
        }
        final DataPoints ring = ((Polygon) complex.getFace(0).getGeometry()).getExteriorRing().getDataPoints();
        assertTrue(ring.getType().getAttributeNames().contains(TEMPERATURE), "The realization keeps the attributes.");
        for (int i = 0; i < ring.size(); i++) {
            final double[] c = ring.getPosition(i).toArrayDouble();
            assertEquals(10*c[0] + c[1], ring.getAttribute(i, TEMPERATURE).get(0), EPS);
        }
        final int node = new ComplexEditor(complex).splitEdge(0, 0.25, 0);
        assertEquals(2.5, temperature(complex, node), EPS, "Interpolated along the edge.");
    }

    /**
     * Positions shared by two meshes take the attributes of the first mesh.
     */
    @Test
    public void testFirstValueKept() {
        final MeshPrimitive first  = mesh(new double[] {0,0, 1,0, 1,1}, new double[] {1, 2, 3}, 0,1,2);
        final MeshPrimitive second = mesh(new double[] {0,0, 1,1, 0,1}, new double[] {7, 8, 9}, 0,1,2);
        final IndexedComplex complex = build(first, second);
        assertEquals(4, complex.getNodeCount());
        assertEquals(1, temperature(complex, 0), EPS);
        assertEquals(3, temperature(complex, 2), EPS);
        assertEquals(9, temperature(complex, 3), EPS);
    }

    /**
     * A node without value for an attribute takes it from a later point at the same position,
     * or has zero values. Points added by the editor keep their attributes.
     */
    @Test
    public void testMissingAttribute() {
        final Polygon square = polygon(CRS2D, 0,0, 2,0, 2,2, 0,2, 0,0);
        final IndexedComplex complex = build(square, point(2, 2, 5));
        final NodePoints points = complex.getNodePoints();
        assertEquals(4, complex.getNodeCount());
        assertTrue(points.hasAttribute(2, TEMPERATURE));
        assertEquals(5, temperature(complex, 2), EPS);
        assertFalse(points.hasAttribute(0, TEMPERATURE));
        assertEquals(0, temperature(complex, 0), EPS, "Missing values are zero.");

        final ComplexEditor editor = new ComplexEditor(complex);
        final int node = editor.addIsolatedNode(point(1, 1, 7));
        assertEquals(7, temperature(complex, node), EPS);
        assertEquals(complex.getFace(0), complex.getNode(node).getContainer());
        final int split = editor.splitEdge(1, 2, 1);
        assertFalse(points.hasAttribute(split, TEMPERATURE), "Not interpolated when an end has no value.");

        assertThrows(UnsupportedOperationException.class, () -> points.setPosition(0, points.getPosition(1)));
        points.setAttribute(0, TEMPERATURE, points.getAttribute(2, TEMPERATURE));
        assertTrue(points.hasAttribute(0, TEMPERATURE));
        assertEquals(5, temperature(complex, 0), EPS);
    }
}
