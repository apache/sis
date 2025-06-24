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
package org.apache.sis.geometries.simplify.greedyinsert;

import java.util.function.BiFunction;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.Triangle;
import org.apache.sis.geometries.math.Maths;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.Vector;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
final class TinDistance implements BiFunction<Tuple,Triangle,Double> {

    @Override
    public Double apply(Tuple pt, Triangle triangle) {
        final PointSequence points = triangle.getExteriorRing().getPoints();
        final Tuple p0 = points.getPosition(0);
        final Tuple p1 = points.getPosition(1);
        final Tuple p2 = points.getPosition(2);
        Vector normal = Maths.calculateNormal(p0, p1, p2);
        double planD = normal.dot(p0);

        return Math.abs(Maths.distance(pt, normal, planD));
    }

}
