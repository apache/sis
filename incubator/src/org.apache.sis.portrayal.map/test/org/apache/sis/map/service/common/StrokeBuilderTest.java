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
package org.apache.sis.map.service.common;

import java.awt.BasicStroke;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link StrokeBuilder}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class StrokeBuilderTest {
    /**
     * Creates a new test case.
     */
    public StrokeBuilderTest() {
    }

    /**
     * Tests the default stroke and the width, cap and join.
     */
    @Test
    public void testSolid() {
        assertEquals(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10), new StrokeBuilder().build());
        final StrokeBuilder builder = new StrokeBuilder().setWidth(3).setCap(BasicStroke.CAP_ROUND).setJoin(BasicStroke.JOIN_BEVEL);
        assertEquals(3, builder.getWidth());
        assertEquals(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 10), builder.build());
        assertEquals(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 10), builder.buildSolid(7));
        assertEquals(0, new StrokeBuilder().setWidth(Double.NaN).getWidth());
        assertEquals(0, builder.buildSolid(-2).getLineWidth());
    }

    /**
     * Tests the dash patterns and offsets.
     */
    @Test
    public void testDashes() {
        BasicStroke stroke = new StrokeBuilder().setDashes(new double[] {10, 5}, 18).build();
        assertArrayEquals(new float[] {10, 5}, stroke.getDashArray());
        assertEquals(3, stroke.getDashPhase(), "Offset reduced to the period.");

        stroke = new StrokeBuilder().setDashes(new double[] {10, 5}, -3).build();
        assertEquals(12, stroke.getDashPhase(), "Negative offset.");

        stroke = new StrokeBuilder().setDashes(new double[] {4}, 9).build();
        assertEquals(1, stroke.getDashPhase(), "An odd number of lengths gives a period of twice the total.");

        stroke = new StrokeBuilder().setDashes(new double[] {6, -2, Double.NaN, 3}, 0).build();
        assertArrayEquals(new float[] {6, 0, 0, 3}, stroke.getDashArray(), "Invalid lengths replaced by 0.");

        assertNull(new StrokeBuilder().setDashes(new double[] {0, 0}, 0).build().getDashArray(), "All lengths are 0.");
        assertNull(new StrokeBuilder().setDashes(new double[] {1, Double.POSITIVE_INFINITY}, 0).build().getDashArray());
        assertNull(new StrokeBuilder().setDashes(new double[] {10, 5}, 0).buildSolid(2).getDashArray(), "Solid line.");
    }
}
