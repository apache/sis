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
package org.apache.sis.geometries.operation;

import org.apache.sis.geometries.processor.spatialanalysis2d.Distance;
import org.apache.sis.geometries.math.Maths;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.Vector;
import org.apache.sis.geometries.math.Vectors;
import java.util.ArrayList;
import java.util.List;

/**
 * The Sutherland–Hodgman algorithm is an algorithm used for clipping polygons.
 *
 * reference :
 * https://en.wikipedia.org/wiki/Sutherland–Hodgman_algorithm
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SutherlandHodgman {

    private SutherlandHodgman(){}

    /**
     *
     * @param subject : sequence of Tuples
     * @param clip : sequence of Segment, must be counter-clockwise direction, first point must equals last
     * @return Sequence of tuple for the result polygon
     */
    public static List<Tuple> clip(List<Tuple> subject, List<Tuple> clip){
        final List<Tuple> outputList = new ArrayList(subject);

        for (int i = 0, n = clip.size() - 1; i < n; i++){
            final Tuple clipEdgeStart = clip.get(i);
            final Tuple clipEdgeEnd = clip.get(i + 1);

            final List<Tuple> inputList = new ArrayList<>(outputList);
            if (inputList.isEmpty()) break;
            outputList.clear();

            Tuple start = inputList.get(inputList.size() - 1);
            for (int k = 0, kn = inputList.size(); k < kn; k++){
                final Tuple end = (Tuple) inputList.get(k);

                if (isInside(clipEdgeStart, clipEdgeEnd, end)){
                    if (!isInside(clipEdgeStart, clipEdgeEnd, start)){
                        outputList.add(computeIntersection(clipEdgeStart, clipEdgeEnd, start, end));
                    }
                    outputList.add(end.copy());
                } else if (isInside(clipEdgeStart, clipEdgeEnd, start)){
                    outputList.add(computeIntersection(clipEdgeStart, clipEdgeEnd, start, end));
                }
                start = end;
            }
        }

        return outputList;
    }

    private static boolean isInside(Tuple edgeStart, Tuple edgeEnd, Tuple point){
        return Maths.lineSide(edgeStart, edgeEnd, point) > 0;
    }

    private static Tuple computeIntersection(Tuple start1, Tuple end1, Tuple start2, Tuple end2){
        final double[] buffer1 = new double[start2.getDimension()];
        final double[] buffer2 = new double[start2.getDimension()];
        final double[] ratio = new double[2];
        Distance.distanceSquare(
                start1.toArrayDouble(),
                end1.toArrayDouble(),
                buffer1,
                start2.toArrayDouble(),
                end2.toArrayDouble(),
                buffer2,
                ratio,
                0.000000000000001);
        final Vector v = Vectors.createDouble(start1.getDimension());
        v.set(buffer1);
        return v;
    }
}
