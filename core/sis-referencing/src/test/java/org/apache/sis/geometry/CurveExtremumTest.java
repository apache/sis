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
package org.apache.sis.geometry;

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link CurveExtremum} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final strictfp class CurveExtremumTest extends TestCase {
    /**
     * Tolerance factor for the tests in this class.
     */
    private static final double EPS = 1E-8;

    /**
     * Tests {@link CurveExtremum#resolve(double, double, double, double, double, double)}.
     */
    @Test
    public void testResolve() {
        final CurveExtremum extremum = new CurveExtremum();
        double x1, y1, dy1;
        double x2, y2, dy2;

        x1 =  0; y1 =  0; dy1 =   7;
        x2 = -4; y2 =  0; dy2 = -12;
        extremum.resolve(x1, y1, dy1, x2, y2, dy2);
        assertEquals("X1",   3.31741507, extremum.ex1, EPS);
        assertEquals("Y1",  17.31547745, extremum.ey1, EPS);
        assertEquals("X2",  -2.25074840, extremum.ex2, EPS);
        assertEquals("Y2",  -9.65918115, extremum.ey2, EPS);

        x1 = 0; y1 =  0; dy1 = 5;
        x2 = 5; y2 = 20; dy2 = 1;
        extremum.resolve(x1, y1, dy1, x2, y2, dy2);
        assertEquals("X1",   5.47313697, extremum.ex1, EPS);
        assertEquals("Y1",  20.24080512, extremum.ey1, EPS);
        assertEquals("X2",  -3.80647030, extremum.ex2, EPS);
        assertEquals("Y2", -11.72228660, extremum.ey2, EPS);
    }
}
