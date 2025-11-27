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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.LineString;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.Triangle;
import org.apache.sis.geometries.mesh.MeshPrimitive.Vertex;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.Array;
import org.apache.sis.referencing.CRS;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class MeshPrimitiveComparator {

    private boolean compareCrs = true;
    private boolean compareByElement = false;
    private boolean skipDegenerated = false;
    private List<String> comparedAttributes = new ArrayList<>();

    /**
     * Set to false to ignore CRS.
     */
    public MeshPrimitiveComparator compareCrs(boolean compareCrs) {
        this.compareCrs = compareCrs;
        return this;
    }

    /**
     * Set to true to compare point,line,triangles as unique element
     * and ignore variable indexing.
     */
    public MeshPrimitiveComparator compareByElement(boolean compareByElement) {
        this.compareByElement = compareByElement;
        return this;
    }

    /**
     * Set to true to skip degenerated lines and triangles.
     * This parameter only works when comparison by elements is true.
     */
    public MeshPrimitiveComparator skipDegenerated(boolean skipDegenerated) {
        this.skipDegenerated = skipDegenerated;
        return this;
    }

    /**
     * Finite list of attributes to compare.
     * This parameter only works when comparison by elements is true.
     */
    public MeshPrimitiveComparator comparedAttributes(String ... names) {
        comparedAttributes = Arrays.asList(names);
        return this;
    }

    /**
     * Compare two primitive.
     * @throws IllegalArgumentException if they are not equal
     */
    public void compare(MeshPrimitive expected, MeshPrimitive candidate) throws IllegalArgumentException {

        if (compareCrs) {
            if (!CRS.equivalent(expected.getCoordinateReferenceSystem(), candidate.getCoordinateReferenceSystem())) {
                throw new IllegalArgumentException("CRS are different");
            }
        }

        if (!compareByElement) {
            if (!expected.getType().equals(candidate.getType())) {
                throw new IllegalArgumentException("Primitive type differ, expected " + expected.getType() + " but was " + candidate.getType());
            }

            final List<String> expectedAtts = expected.getAttributesType().getAttributeNames();
            final List<String> candidateAtts = candidate.getAttributesType().getAttributeNames();
            final Array expectedIdx = expected.getIndex();
            final Array candidateIdx = candidate.getIndex();

            if (!Objects.equals(expectedAtts, candidateAtts)) {
                throw new IllegalArgumentException("Primitive attributes differ");
            }
            for (String attName : expectedAtts) {
                if (!Objects.equals(expected.getAttribute(attName), candidate.getAttribute(attName))) {
                    throw new IllegalArgumentException("Primitive attributes " + attName + "differ");
                }
            }

            if (!Objects.equals(expectedIdx, candidateIdx)) {
                throw new IllegalArgumentException("Primitive index differ");
            }
        } else {
            final List<Geometry> expectedElements = elements(expected);
            final List<Geometry> candidateElements = elements(candidate);

            if (skipDegenerated) {
                //remove all degenerated elements
                for (int i = expectedElements.size() - 1; i >= 0; i--) {
                    if (isDegenerated(expectedElements.get(i))) {
                        expectedElements.remove(i);
                    }
                }
                for (int i = candidateElements.size() - 1; i >= 0; i--) {
                    if (isDegenerated(candidateElements.get(i))) {
                        candidateElements.remove(i);
                    }
                }
            }

            if (expectedElements.size() != candidateElements.size()) {
                throw new IllegalArgumentException("Number of elements do not match. expected " + expectedElements.size() + " but was " + candidateElements.size());
            }

            search:
            for (int t=0,n=expectedElements.size();t<n;t++) {
                Object exp = expectedElements.get(t);
                if (skipDegenerated && isDegenerated(exp)) {
                    continue;
                }

                for (Object cdt : candidateElements) {
                    if (equals(exp, cdt)) {
                        //found equivalent
                        candidateElements.remove(cdt);
                        continue search;
                    }
                }
                throw new IllegalArgumentException("Primitive element " + t + " not found " + exp);
            }

        }

    }

    /**
     * Compare two multi-primitive.
     * @throws IllegalArgumentException if they are not equal
     */
    public void compare(MultiMeshPrimitive expected, MultiMeshPrimitive candidate) throws IllegalArgumentException {

        if (compareCrs) {
            if (!CRS.equivalent(expected.getCoordinateReferenceSystem(), candidate.getCoordinateReferenceSystem())) {
                throw new IllegalArgumentException("CRS are different");
            }
        }

        final List<? extends MeshPrimitive> expectedPrimitives = expected.getComponents();
        final List<? extends MeshPrimitive> candidatePrimitives = candidate.getComponents();

        int size = expectedPrimitives.size();
        if (expectedPrimitives.size() != candidatePrimitives.size()) {
            throw new IllegalArgumentException("Number of primitives differ, expected " + expectedPrimitives.size() + " but was " + candidatePrimitives.size());
        }

        for (int i = 0; i < size; i++) {
            final MeshPrimitive pe = expectedPrimitives.get(i);
            final MeshPrimitive pc = candidatePrimitives.get(i);
            compare(pe, pc);
        }
    }

    /**
     * Compare two mesh element.
     * @return true if elements are equal.
     */
    private boolean equals(Object expected, Object candidate) {
        if (expected instanceof Point && candidate instanceof Point) {
            final Point e1 = (Point) expected;
            final Point e2 = (Point) candidate;
            return compareVertex((MeshPrimitive.Vertex) e1, (MeshPrimitive.Vertex) e2);

        } else if (expected instanceof LineString && candidate instanceof LineString) {
            final LineString e1 = (LineString) expected;
            final LineString e2 = (LineString) candidate;

            final Vertex e1v0 = (Vertex) e1.getPoints().getPoint(0);
            final Vertex e1v1 = (Vertex) e1.getPoints().getPoint(1);
            final Vertex e2v0 = (Vertex) e2.getPoints().getPoint(0);
            final Vertex e2v1 = (Vertex) e2.getPoints().getPoint(1);
            return (compareVertex(e1v0, e2v0) && compareVertex(e1v1, e2v1))
                || (compareVertex(e1v0, e2v1) && compareVertex(e1v1, e2v0));


        } else if (expected instanceof Triangle && candidate instanceof Triangle) {
            final LineString e1 = ((Triangle) expected).getExteriorRing();
            final LineString e2 = ((Triangle) candidate).getExteriorRing();

            final Vertex e1v0 = (Vertex) e1.getPoints().getPoint(0);
            final Vertex e1v1 = (Vertex) e1.getPoints().getPoint(1);
            final Vertex e1v2 = (Vertex) e1.getPoints().getPoint(2);
            final Vertex e2v0 = (Vertex) e2.getPoints().getPoint(0);
            final Vertex e2v1 = (Vertex) e2.getPoints().getPoint(1);
            final Vertex e2v2 = (Vertex) e2.getPoints().getPoint(2);
            return (compareVertex(e1v0, e2v0) && compareVertex(e1v1, e2v1) && compareVertex(e1v2, e2v2))
                || (compareVertex(e1v0, e2v1) && compareVertex(e1v1, e2v2) && compareVertex(e1v2, e2v0))
                || (compareVertex(e1v0, e2v2) && compareVertex(e1v1, e2v0) && compareVertex(e1v2, e2v1));
        }
        return false;
    }

    /**
     * Compare two mesh vertex attributes.
     * @return true if vertex are equal.
     */
    private boolean compareVertex(MeshPrimitive.Vertex expected, MeshPrimitive.Vertex candidate) {

        Collection<String> toTest = comparedAttributes;
        if (toTest.isEmpty()) {
            toTest = expected.getAttributesType().getAttributeNames();
            if (toTest.size() != candidate.getAttributesType().getAttributeNames().size()) {
                throw new IllegalArgumentException("Attributes do not match, expected "
                        + Arrays.toString(toTest.toArray()) + " but was "
                        + Arrays.toString(candidate.getAttributesType().getAttributeNames().toArray()));
            }
        }

        for (String attName : toTest) {
            final Object att1 = expected.getAttribute(attName);
            final Object att2 = candidate.getAttribute(attName);
            if (!Objects.equals(att1, att2)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Test if given element is a degenerated line or triangle.
     */
    private static boolean isDegenerated(Object candidate) {
        if (candidate instanceof LineString) {
            final LineString cdt = (LineString) candidate;
            final PointSequence points = cdt.getPoints();
            return points.getPosition(0).equals(points.getPosition(1));

        } else if (candidate instanceof Triangle) {
            final Triangle cdt = (Triangle) candidate;
            final PointSequence points = cdt.getExteriorRing().getPoints();
            final Tuple c0 = points.getPosition(0);
            final Tuple c1 = points.getPosition(1);
            final Tuple c2 = points.getPosition(2);
            return c0.equals(c1)
                || c0.equals(c2)
                || c1.equals(c2);

        }
        return false;
    }

    /**
     * List all elements in given primitive.
     */
    private static List<Geometry> elements(MeshPrimitive p) {

        final List<Geometry> elements = new ArrayList<>();
        final MeshPrimitiveVisitor visitor = new MeshPrimitiveVisitor(p) {
            @Override
            protected void visit(MeshPrimitive.Vertex vertex) {
            }

            @Override
            protected void visit(LineString candidate) {
                elements.add(candidate);
            }

            @Override
            protected void visit(Point candidate) {
                elements.add(candidate);
            }

            @Override
            protected void visit(Triangle candidate) {
                elements.add(candidate);
            }
        };
        visitor.visit();
        return elements;
    }
}
