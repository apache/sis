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
package org.apache.sis.geometries.internal.shared;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.point.MultiPoint;

// Test dependencies
import org.apache.sis.geometries.point.MultiPointTest;


/**
 * Tests {@link DefaultRawMultiPoint}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultRawMultiPointTest extends MultiPointTest {

    /**
     * Creates the set from an array of positions rather than from a sequence of positions,
     * which is the difference between this implementation and {@link DefaultMultiPoint}.
     */
    @Override
    protected MultiPoint<?> createMultiPoint(final CoordinateReferenceSystem crs, final double... coordinates) {
        final Point[] points = new Point[coordinates.length / 2];
        for (int i = 0; i < points.length; i++) {
            points[i] = new DefaultPoint(crs, coordinates[i*2], coordinates[i*2 + 1]);
        }
        return new DefaultRawMultiPoint(crs, points);
    }
}
