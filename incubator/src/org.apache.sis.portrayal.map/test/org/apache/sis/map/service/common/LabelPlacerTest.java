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

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link LabelBox} and {@link LabelPlacer}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class LabelPlacerTest {
    /**
     * The bounds of the labels relative to their anchor: 20×10 pixels centered on the anchor.
     */
    private static final Rectangle2D BOUNDS = new Rectangle2D.Double(-10, -5, 20, 10);

    /**
     * Names of the labels in the order they were painted.
     */
    private final List<String> painted;

    /**
     * Creates a new test case.
     */
    public LabelPlacerTest() {
        painted = new ArrayList<>();
    }

    /**
     * A drawing which records when it is painted.
     */
    private final class Recorder implements Drawing {
        /**
         * The name of the label.
         */
        private final String name;

        /**
         * Creates a drawing of the given name.
         */
        Recorder(final String name) {
            this.name = name;
        }

        @Override
        public void paint(final Graphics2D graphics) {
            painted.add(name);
        }

        @Override
        public boolean intersects(final Shape mask) {
            return false;
        }
    }

    /**
     * Creates a label of the given name with its anchor at (x, 0).
     */
    private LabelCandidate label(final String name, final double x, final double priority) {
        return new LabelCandidate(new Recorder(name), List.of(new LabelBox(BOUNDS, x, 0, 0)), priority);
    }

    /**
     * Tests the intersections of boxes, with padding and rotation.
     */
    @Test
    public void testBoxes() {
        final LabelBox a = new LabelBox(BOUNDS, 0, 0, 0);
        final LabelBox b = new LabelBox(BOUNDS, 25, 0, 0);
        assertFalse(a.intersects(b));
        assertFalse(a.intersects(b.expand(3)));
        assertTrue (a.intersects(b.expand(5)), "Boxes which touch intersect.");
        assertTrue (a.intersects(new LabelBox(BOUNDS, 15, 8, 0)));
        /*
         * A thin box rotated by -45°, whose envelope overlaps the envelope
         * of the first box but which is separated by its diagonal edge.
         */
        final LabelBox diagonal = new LabelBox(new Rectangle2D.Double(-6, -1, 12, 2), 14, 9, -Math.PI / 4);
        assertFalse(a.intersects(diagonal));
        assertFalse(diagonal.intersects(a));
        assertTrue (a.intersects(new LabelBox(new Rectangle2D.Double(-6, -1, 12, 2), 10, 5, -Math.PI / 4)));
    }

    /**
     * Tests the placement by priority, the padding and the groups.
     */
    @Test
    public void testPlacement() {
        final LabelPlacer placer = new LabelPlacer(2);
        placer.add(label("low",    0, 0));
        placer.add(label("high",  15, 1));
        placer.add(label("far",  100, 0));
        placer.add(label("near", 121, 0));          // 1 pixel away from "far", less than the padding.
        placer.add(List.of(label("first", 200, 0), label("second", 210, 0)));
        placer.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics());
        assertEquals(List.of("high", "far", "first", "second"), painted,
                "Labels by priority, without overlaps except in a same group.");

        painted.clear();
        placer.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics());
        assertTrue(painted.isEmpty(), "Labels shall be discarded after painting.");
    }
}
