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
package org.apache.sis.geometries.processor.spatialrelations2d;

import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.LineString;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.Polygon;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.geometries.processor.Processor;
import org.apache.sis.geometries.processor.ProcessorUtils;
import org.apache.sis.geometries.math.Maths;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.TupleArray;
import org.apache.sis.geometries.math.TupleArrayCursor;
import org.apache.sis.geometries.math.Vectors;

/**
 * Constains 2D processors.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Contains {

    private Contains(){}

    /**
     * Test if point is within polygon using Winding Number algorithm.
     * http://geomalgorithms.com/a03-_inclusion.html
     * http://en.wikipedia.org/wiki/Point_in_polygon
     *
     * @param ring not null
     * @param point not null
     */
    private static boolean contains(TupleArray ring, Tuple point) {
        final TupleArrayCursor cursor = ring.cursor();

        int windingNumber = 0;
        Tuple current;
        Tuple previous;
        cursor.moveTo(0);
        current = cursor.samples();
        previous = Vectors.create(current.getSampleSystem(), current.getDataType());
        final double pointY = point.get(1);
        for (int i = 1, n = ring.getLength(); i < n; i++){
            previous.set(current);
            cursor.moveTo(i);
            current = cursor.samples();

            if (previous.get(1) <= pointY){
                if (current.get(1) > pointY){
                    if (Maths.lineSide(previous, current, point) > 0){
                        windingNumber++;
                    }
                }
            } else {
                if (current.get(1) <= pointY){
                    if (Maths.lineSide(previous, current, point) < 0){
                        windingNumber--;
                    }
                }
            }
        }

        //if 0 point is outside
        return windingNumber != 0;
    }

    /**
     * Polygon contains Point test.
     */
    public static class PolygonPoint implements Processor.Binary<org.apache.sis.geometries.operation.spatialrelations2d.Contains, Polygon, Point>{

        @Override
        public Class<org.apache.sis.geometries.operation.spatialrelations2d.Contains> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialrelations2d.Contains.class;
        }

        @Override
        public Class<Polygon> getGeometryClass() {
            return Polygon.class;
        }

        @Override
        public Class<Point> getRelatedClass() {
            return Point.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialrelations2d.Contains operation) throws OperationException {
            ProcessorUtils.ensureSameCRS2D(operation.geometry, operation.other);
            final Polygon polygon = (Polygon) operation.geometry;
            final Point candidate = (Point) operation.other;


            { //check exterior
                final TupleArray coords = asLineString(polygon.getExteriorRing()).getPoints().getAttributeArray(AttributesType.ATT_POSITION);
                if (!contains(coords, candidate.getPosition())) {
                    //point is outside the exterior ring
                    operation.result = false;
                    return;
                }
            }

            { //check holes
                for (int i = 0, n = polygon.getNumInteriorRing(); i < n; i++) {
                    final LineString hole = asLineString(polygon.getInteriorRingN(i));
                    final TupleArray coords = hole.getPoints().getAttributeArray(AttributesType.ATT_POSITION);
                    if (contains(coords, candidate.getPosition())) {
                        //point is within a hole
                        operation.result = false;
                        return;
                    }
                }
            }

            //point is inside polygon
            operation.result = true;
        }
    }

    private static LineString asLineString(Curve curve) {
        if (curve instanceof LineString ls) {
            return ls;
        }
        throw new OperationException("Curve type not supported");
    }

}
