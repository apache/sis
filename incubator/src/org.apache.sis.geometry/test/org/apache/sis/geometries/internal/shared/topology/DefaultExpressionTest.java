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

import java.util.List;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.topology.DirectedTopo;
import org.apache.sis.geometries.topology.Edge;
import org.apache.sis.geometries.topology.Expression;
import org.apache.sis.geometries.topology.ExpressionTerm;
import org.apache.sis.geometries.topology.Face;
import org.apache.sis.geometries.topology.Node;
import org.apache.sis.maths.NDArrays;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link DefaultExpression} on the complex of a unit square, whose nodes 0 to 3 are its corners
 * in counterclockwise order from the origin, and whose edge <var>i</var> goes from node <var>i</var>
 * to the next node.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultExpressionTest {

    /**
     * Returns the complex of the unit square.
     */
    private static IndexedComplex square() {
        final GeometryFactory f = GeometryFactory.DEFAULT;
        final double[] ring = {0,0, 1,0, 1,1, 0,1, 0,0};
        return new ComplexBuilder().add(f.createPolygon(f.createLinearRing(f.createDataPoints(
                NDArrays.of(Geometries.getUndefinedCRS(2), ring))), List.of())).build();
    }

    /**
     * Returns the coefficient of the given primitive in the given expression.
     */
    private static int coefficient(final Expression expression, final DirectedTopo variable) {
        for (final ExpressionTerm term : expression.getTerms()) {
            if (term.getVariable().equals(variable)) {
                return term.getCoefficient();
            }
        }
        return 0;
    }

    /**
     * Terms of the same primitive are combined, and the orientation is carried by the coefficient.
     */
    @Test
    public void testTerms() {
        final IndexedComplex complex = square();
        final Edge e0 = complex.getEdge(0), e1 = complex.getEdge(1);
        final Expression x = e0.getExpression().plus(e0.getExpression()).minus(e1.getNegate().getExpression());
        assertEquals(2, x.getTerms().size());
        assertEquals(2, coefficient(x, e0));
        assertEquals(1, coefficient(x, e1));
        assertEquals("+2*Edge[0] +Edge[1]", x.toString());
        assertEquals(-1, coefficient(e0.getNegate().getExpression(), e0), "Stored on the positive edge.");
        assertTrue(e0.getExpression().plus(e0.getNegate().getExpression()).isZero());
        assertEquals("0", DefaultExpression.ZERO.toString());
    }

    /**
     * Equality is polynomial equality: the order of the terms does not matter,
     * and a term is equal to the expression of that single term.
     */
    @Test
    public void testEquals() {
        final IndexedComplex complex = square();
        final Expression a = complex.getEdge(0).getExpression();
        final Expression b = complex.getEdge(1).getNegate().getExpression();
        assertEquals(a.plus(b), b.plus(a));
        assertEquals(a.plus(b).hashCode(), b.plus(a).hashCode());
        assertNotEquals(a.plus(b), a.minus(b));
        assertEquals(DefaultExpression.ZERO, a.minus(a));
        assertEquals(a.getNegate(), complex.getEdge(0).getNegate().getExpression());

        final ExpressionTerm term = a.getTerms().iterator().next();
        assertEquals(a, term);
        assertEquals(term, a);
        assertEquals(a.hashCode(), term.hashCode());
    }

    /**
     * The boundary of an edge is its end minus its start, the boundary of a node is zero,
     * and the boundary of the boundary of a face is zero.
     */
    @Test
    public void testBoundary() {
        final IndexedComplex complex = square();
        final Edge edge = complex.getEdge(0);
        final Node start = edge.getBoundary().get(0).getTopo();
        final Node end   = edge.getBoundary().get(1).getTopo();
        assertEquals(complex.getNode(0), start);
        assertEquals(complex.getNode(1), end);
        assertEquals(end.getExpression().minus(start.getExpression()), edge.getExpression().getBoundary());
        assertTrue(start.getExpression().getBoundary().isZero());

        final Face face = complex.getFace(0);
        final Expression boundary = face.getExpression().getBoundary();
        assertEquals(4, boundary.getTerms().size());
        for (int i = 0; i < 4; i++) {
            assertEquals(1, coefficient(boundary, complex.getEdge(i)), "The face is on the left of each edge.");
        }
        assertTrue(boundary.getBoundary().isZero());
        assertEquals(boundary.getNegate(), face.getNegate().getExpression().getBoundary());
    }

    /**
     * The co-boundary is the transpose of the boundary.
     */
    @Test
    public void testCoBoundary() {
        final IndexedComplex complex = square();
        final Face face = complex.getFace(0);
        for (int i = 0; i < 4; i++) {
            assertEquals(face.getExpression(), complex.getEdge(i).getExpression().getCoBoundary());
        }
        // Edge 3 ends at node 0 and edge 0 starts at it.
        assertEquals(complex.getEdge(3).getExpression().minus(complex.getEdge(0).getExpression()),
                     complex.getNode(0).getExpression().getCoBoundary());
        assertTrue(face.getExpression().getCoBoundary().isZero(), "No solid above or below the face.");
    }

    /**
     * The support of a face is the face, its edges and its nodes. As a set of oriented primitives,
     * each node appears in both orientations, as the start of an edge and the end of another.
     */
    @Test
    public void testSupport() {
        final IndexedComplex complex = square();
        final Expression face = complex.getFace(0).getExpression();
        assertEquals(1 + 4 + 4, face.getSupport().size());
        assertEquals(1 + 4 + 8, face.asSet().size());
        assertTrue(face.getSupport().contains(complex.getNode(2)));
    }
}
