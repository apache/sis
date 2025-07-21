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

import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.geometries.processor.Processor;
import org.apache.sis.geometries.processor.ProcessorUtils;
import org.apache.sis.geometries.math.Maths;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.Vectors;


/**
 * Distance 2D processors.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Distance {

    private Distance(){}

    /**
     * Calculate distance between two points.
     *
     * @param p1 first point
     * @param p2 second point
     * @return double, distance
     */
    public static double distance(Tuple p1, Tuple p2){
        return Math.sqrt(distanceSquare(p1, p2));
    }

    /**
     * Calculate square distance between two points.
     *
     * @param p1 first point
     * @param p2 second point
     * @return double, distance
     */
    public static double distanceSquare(Tuple p1, Tuple p2){
        final double t0 = p1.get(0) - p2.get(0);
        final double t1 = p1.get(1) - p2.get(1);
        return t0*t0 + t1*t1;
    }

    /**
     * Calculate square distance between two line segments.
     *
     * Adapted from book : Real-TimeCollision Detection by Christer Ericson
     * (ClosestPtSegmentSegment p.149)
     *
     * @param line1Start line 1 start point
     * @param line1End line 1 start point
     * @param buffer1 closest point on line 1
     * @param line2Start line 1 start point
     * @param line2End line 1 start point
     * @param buffer2 closest point on line 2
     * @param ratio size 2 , for each line,
     *  ratio [0..1] of the closest point position between start and end points.
     * @param epsilon tolerance
     * @return distance
     */
    public static double distanceSquare(double[] line1Start, double[] line1End, double[] buffer1,
                                   double[] line2Start, double[] line2End, double[] buffer2,
                                   double[] ratio, double epsilon) {
        final double[] d1 = Vectors.subtract(line1End,line1Start); // Direction vector of segment S1
        final double[] d2 = Vectors.subtract(line2End,line2Start); // Direction vector of segment S2
        final double[] r = Vectors.subtract(line1Start, line2Start);
        final double a = Vectors.dot(d1, d1); // Squared length of segment S1, always nonnegative
        final double e = Vectors.dot(d2, d2); // Squared length of segment S2, always nonnegative
        final double f = Vectors.dot(d2, r);
        // Check if either or both segments degenerate into points
        if (a <= epsilon && e <= epsilon) {
            // Both segments degenerate into points
            ratio[0] = 0;
            ratio[1] = 0;
            System.arraycopy(line1Start, 0, buffer1, 0, line1Start.length);
            System.arraycopy(line2Start, 0, buffer2, 0, line2Start.length);
            final double[] t = Vectors.subtract(buffer1,buffer2);
            return Vectors.dot(t,t);
        }
        if (a <= epsilon) {
            // First segment degenerates into a point
            ratio[0] = 0;
            ratio[1] = Maths.clamp( f/e, 0, 1);
        } else {
            final double c = Vectors.dot(d1, r);
            if (e <= epsilon) {
                // Second segment degenerates into a point
                ratio[0] = Maths.clamp( -c/a, 0, 1);
                ratio[1] = 0;
            } else {
                // The general nondegenerate case starts here
                final double b = Vectors.dot(d1, d2);
                final double denom = a*e-b*b; // Always nonnegative
                // If segments not parallel, compute closest point on L1 to L2 and
                // clamp to segment S1. Else pick arbitrary s (here 0)
                if (denom != 0d) {
                    ratio[0] = Maths.clamp((b*f - c*e) / denom, 0.0f, 1.0f);
                } else {
                    ratio[0] = 0;
                }
                // Compute point on L2 closest to S1(s) using
                // t = Dot((P1 + D1*s) - P2,D2) / Dot(D2,D2) = (b*s + f) / e
                ratio[1] = (b*ratio[0] + f) / e;

                //If t in [0,1] done. Else clamp t, recompute s for the new value
                //of t using s = Dot((P2 + D2*t) - P1,D1) / Dot(D1,D1)= (t*b - c) / a
                //and clamp s to [0, 1]
                if (ratio[1] < 0){
                    ratio[1] = 0;
                    ratio[0] = Maths.clamp(-c / a, 0.0f, 1.0f);
                } else if (ratio[1] > 1){
                    ratio[1] = 1;
                    ratio[0] = Maths.clamp((b - c) / a, 0.0f, 1.0f);
                }

            }
        }

        Vectors.add(line1Start, Vectors.scale(d1, ratio[0]), buffer1);
        Vectors.add(line2Start, Vectors.scale(d2, ratio[1]), buffer2);
        final double[] t = Vectors.subtract(buffer1,buffer2);
        return Vectors.dot(t,t);
    }

    /**
     * Point to point distance.
     */
    public static class PointPoint implements Processor.Binary<org.apache.sis.geometries.operation.spatialanalysis2d.Distance, Point, Point>{

        @Override
        public Class<org.apache.sis.geometries.operation.spatialanalysis2d.Distance> getOperationClass() {
            return org.apache.sis.geometries.operation.spatialanalysis2d.Distance.class;
        }

        @Override
        public Class<Point> getRelatedClass() {
            return Point.class;
        }

        @Override
        public Class<Point> getGeometryClass() {
            return Point.class;
        }

        @Override
        public void process(org.apache.sis.geometries.operation.spatialanalysis2d.Distance operation) throws OperationException {
            ProcessorUtils.ensureSameCRS(operation.geometry, operation.other);
            final Point p1 = (Point) operation.geometry;
            final Point p2 = (Point) operation.other;
            operation.result = distance(p1.getPosition(), p2.getPosition());
        }
    }

}
