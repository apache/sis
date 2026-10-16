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

import org.apache.sis.geometries.DataPoints;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.DataPointsTest;


/**
 * Tests {@link ArrayDataPoints}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class ArrayDataPointsTest extends DataPointsTest {
    /**
     * Tests the copies of points made from the arrays, which do not use the default methods.
     */
    @Test
    public void testSubsetOfArrays() {
        final DataPoints points = createArraySample();
        assertInstanceOf(ArrayDataPoints.class, points);
        assertSubsets(points);
        assertInstanceOf(ArrayDataPoints.class, points.subset(new int[] {1}));
        assertInstanceOf(ArrayDataPoints.class, points.subset(0, 1));
    }
}
