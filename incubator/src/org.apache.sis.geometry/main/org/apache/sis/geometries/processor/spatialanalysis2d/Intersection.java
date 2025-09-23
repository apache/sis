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
package org.apache.sis.geometries.processor.spatialanalysis2d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.LineString;
import org.apache.sis.geometries.MultiLineString;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.PreparedTIN;
import org.apache.sis.geometries.Triangle;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.internal.shared.DefaultPointSequence;
import org.apache.sis.geometries.mesh.MultiMeshPrimitive;
import org.apache.sis.geometries.mesh.MeshPrimitiveVisitor;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.operation.GeometryOperations;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.geometries.operation.SutherlandHodgman;
import org.apache.sis.geometries.processor.Processor;
import org.apache.sis.geometries.processor.ProcessorUtils;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.TupleArray;
import org.apache.sis.geometries.math.TupleArrayCursor;
import org.apache.sis.geometries.math.TupleArrays;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Intersection {

    private Intersection(){}


    /**
     * Inherit attributes from TIN.
     * The POSITION attribute is ignored.
     */
    private static void copyAttributes(PreparedTIN tin, MultiMeshPrimitive<?> primitives) {
        for (MeshPrimitive primitive : primitives.getComponents()) {
            copyAttributes(tin, primitive);
        }
    }

    /**
     * Inherit attributes from TIN.
     * The POSITION attribute is ignored.
     */
    private static void copyAttributes(PreparedTIN tin, MeshPrimitive primitive) {

        final AttributesType attributesType = tin.getAttributesType();
        final int nbVertex = primitive.getPositions().getLength();

        final List<String> attributeNames = new ArrayList<>(attributesType.getAttributeNames());
        if (attributeNames.size() == 1) {
            //no attributes to copy
            return;
        }
        attributeNames.remove(AttributesType.ATT_POSITION);
        final String[] templateNames = new String[attributeNames.size()];
        final TupleArray[] attributes = new TupleArray[attributeNames.size()];
        for (int i = 0; i < templateNames.length; i++) {
            templateNames[i] = attributeNames.get(i);
            final TupleArray ta = TupleArrays.of(
                    attributesType.getAttributeSystem(templateNames[i]),
                    attributesType.getAttributeType(templateNames[i]),
                    nbVertex);
            attributes[i] = ta;
            primitive.setAttribute(templateNames[i], ta);
        }

        //interpolate attributes from TIN
        final PreparedTIN.Evaluator evaluator = tin.evaluator();
        new MeshPrimitiveVisitor(primitive) {
            @Override
            protected void visit(MeshPrimitive.Vertex vertex) {
                if (isVisited(vertex)) return;
                final Optional<Point> opt = evaluator.evaluate(vertex.getPosition());
                if (opt.isEmpty()) return;
                final Point point = opt.get();
                final int index = vertex.getIndex();
                for (int i = 0; i < templateNames.length; i++) {
                    attributes[i].set(index, point.getAttribute(templateNames[i]));
                }
            }
        }.visit();
    }

    /**
     * Triangles with points.
     */
    public static class PrimitiveTrianglesPrimitivePoints implements Processor.Binary<org.apache.sis.geometries.operation.spatialanalysis2d.Intersection, MeshPrimitive.Triangles, MeshPrimitive.Points>{

        @Override
        public Class<org.apache.sis.geometries.operation.spatialanalysis2d.Intersection> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialanalysis2d.Intersection.class;
        }

        @Override
        public Class<MeshPrimitive.Points> getRelatedClass() {
            return MeshPrimitive.Points.class;
        }

        @Override
        public Class<MeshPrimitive.Triangles> getGeometryClass() {
            return MeshPrimitive.Triangles.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialanalysis2d.Intersection operation) throws OperationException {
            ProcessorUtils.ensureSameCRS(operation.geometry, operation.other);
            final MeshPrimitive.Triangles p1 = (MeshPrimitive.Triangles) operation.geometry;
            final MeshPrimitive.Points p2 = (MeshPrimitive.Points) operation.other;

            final PreparedTIN pt = PreparedTIN.create(p1);

            final TupleArray positions = p2.getPositions().copy();

            final MeshPrimitive.Points intersection = new MeshPrimitive.Points();
            operation.result = intersection;

            //create a copy of the points positions
            intersection.setPositions(positions);

            //create an index only for points which intersect
            final PreparedTIN.Evaluator evaluator = pt.evaluator();
            final TupleArrayCursor cursor = positions.cursor();
            final List<Integer> values = new ArrayList<>();
            while (cursor.next()) {
                Tuple position = cursor.samples();
                if (evaluator.evaluate(position).isPresent()) {
                    values.add(cursor.coordinate());
                }
            }
            if (!values.isEmpty()) {
                final TupleArray index = TupleArrays.ofUnsigned(1, values);
                intersection.setIndex(index);
            }

            //remove unused indices
            Geometries.compact(intersection);

            //copy attributes
            copyAttributes(pt, intersection);
        }
    }

    /**
     * Triangles with lines.
     */
    public static class PrimitiveTrianglesPrimitiveLines implements Processor.Binary<org.apache.sis.geometries.operation.spatialanalysis2d.Intersection, MeshPrimitive.Triangles, MeshPrimitive.Lines>{

        @Override
        public Class<org.apache.sis.geometries.operation.spatialanalysis2d.Intersection> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialanalysis2d.Intersection.class;
        }

        @Override
        public Class<MeshPrimitive.Lines> getRelatedClass() {
            return MeshPrimitive.Lines.class;
        }

        @Override
        public Class<MeshPrimitive.Triangles> getGeometryClass() {
            return MeshPrimitive.Triangles.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialanalysis2d.Intersection operation) throws OperationException {
            ProcessorUtils.ensureSameCRS(operation.geometry, operation.other);
            final MeshPrimitive.Triangles triangles = (MeshPrimitive.Triangles) operation.geometry;
            final MeshPrimitive.Lines lines = (MeshPrimitive.Lines) operation.other;

            final PreparedTIN tin = PreparedTIN.create(triangles);

            final List<LineString> segments = new ArrayList<>();
            for (int i = 0, n = lines.getNumGeometries(); i < n; i++) {
                final LineString line = lines.getGeometryN(i);
                final TupleArray segment = line.getPoints().getAttributeArray(AttributesType.ATT_POSITION);
                final Tuple s1 = segment.get(0);
                final Tuple s2 = segment.get(1);

                try (Stream<Triangle> stream = tin.getPatches(line.getEnvelope())) {
                    final Iterator<Triangle> iterator = stream.iterator();

                    while (iterator.hasNext()) {
                        final Triangle triangle = iterator.next();
                        final TupleArray corners = triangle.getExteriorRing().getPoints().getAttributeArray(AttributesType.ATT_POSITION);
                        final Tuple c0 = corners.get(0);
                        final Tuple c1 = corners.get(1);
                        final Tuple c2 = corners.get(2);
                        final List<Tuple> clip = SutherlandHodgman.clip(Arrays.asList(s1,s2,s1), Arrays.asList(c0,c1,c2,c0));
                        if (clip.size() >= 2) {
                            //inherit attributes
                            final double x1 = c0.get(0);
                            final double y1 = c0.get(1);
                            final double x2 = c1.get(0);
                            final double y2 = c1.get(1);
                            final double x3 = c2.get(0);
                            final double y3 = c2.get(1);
                            final Tuple p1 = clip.get(0);
                            final Tuple p2 = clip.get(1);
                            final double[] bary1 = Triangle.getBarycentricValue2D(x1, y1, x2, y2, x3, y3, p1.get(0), p1.get(1), 0.0, false);
                            final double[] bary2 = Triangle.getBarycentricValue2D(x1, y1, x2, y2, x3, y3, p2.get(0), p2.get(1), 0.0, false);
                            final Point point1 = triangle.interpolate(bary1);
                            final Point point2 = triangle.interpolate(bary2);
                            segments.add(GeometryFactory.createLineString(new DefaultPointSequence(point1, point2)));
                        }
                    }
                } catch (TransformException ex) {
                    throw new OperationException(ex.getMessage(), ex);
                }
            }

            final MultiLineString mline = GeometryFactory.createMultiLineString(segments.toArray(LineString[]::new));
            final MeshPrimitive intersection = (MeshPrimitive) GeometryOperations.SpatialEdition.toPrimitive(mline);
            operation.result = intersection;
        }
    }
}
