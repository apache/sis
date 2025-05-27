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
package org.apache.sis.geometries.triangulate.delaunay;

import java.util.Iterator;
import org.apache.sis.geometries.MeshPrimitive;
import org.apache.sis.geometries.MeshPrimitive.Vertex;
import org.apache.sis.geometries.math.TupleArray;
import org.apache.sis.geometries.math.TupleArrays;
import org.apache.sis.referencing.CommonCRS;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class OrientedEdgeTest {

    private static final CoordinateReferenceSystem CRS = CommonCRS.WGS84.normalizedGeographic();

    @Test
    public void testRotatingIterator() {
        /*
              C
              +
             /|\
            / | \
         D +--A--+ B
            \ | /
             \|/
              +
              E
        */
        final TupleArray positions = TupleArrays.of(CRS,
                0,0,
                2,0,
                1,2,
                1,-2,
                1,0
                );

        final MeshPrimitive.Points points = new MeshPrimitive.Points();
        points.setPositions(positions);

        Vertex A = points.getVertex(4);
        Vertex B = points.getVertex(1);
        Vertex C = points.getVertex(2);
        Vertex D = points.getVertex(0);
        Vertex E = points.getVertex(3);

        { //test full loop
            final OrientedTriangle ABC = new OrientedTriangle(A, B, C);
            final OrientedTriangle ACD = new OrientedTriangle(ABC.ca.reverse(true), new OrientedEdge(C, D), new OrientedEdge(D, A));
            final OrientedTriangle ADE = new OrientedTriangle(ACD.ca.reverse(true), new OrientedEdge(D, E), new OrientedEdge(E, A));
            final OrientedTriangle AEB = new OrientedTriangle(ADE.ca.reverse(true), new OrientedEdge(E, B), ABC.ab.reverse(true));

            final Iterator<OrientedEdge> ite = ABC.ab.rotatingIterator();
            assertEquals(ABC.ab, ite.next());
            assertEquals(ACD.ab, ite.next());
            assertEquals(ADE.ab, ite.next());
            assertEquals(AEB.ab, ite.next());
            assertNull(ite.next());
        }

        { //test partial counterclockwise loop
            final OrientedTriangle ABC = new OrientedTriangle(A, B, C);
            final OrientedTriangle ACD = new OrientedTriangle(ABC.ca.reverse(true), new OrientedEdge(C, D), new OrientedEdge(D, A));
            final OrientedTriangle ADE = new OrientedTriangle(ACD.ca.reverse(true), new OrientedEdge(D, E), new OrientedEdge(E, A));

            final Iterator<OrientedEdge> ite1 = ABC.ab.rotatingIterator();
            assertEquals(ABC.ab, ite1.next());
            assertEquals(ACD.ab, ite1.next());
            assertEquals(ADE.ab, ite1.next());
            assertNull(ite1.next());

            final Iterator<OrientedEdge> ite2 = ACD.ab.rotatingIterator();
            assertEquals(ACD.ab, ite2.next());
            assertEquals(ADE.ab, ite2.next());
            assertEquals(ABC.ab, ite2.next());
            assertNull(ite2.next());

            final Iterator<OrientedEdge> ite3 = ADE.ab.rotatingIterator();
            assertEquals(ADE.ab, ite3.next());
            assertEquals(ACD.ab, ite3.next());
            assertEquals(ABC.ab, ite3.next());
            assertNull(ite3.next());
        }

    }
}
