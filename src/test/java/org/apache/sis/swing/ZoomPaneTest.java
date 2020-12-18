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
package org.apache.sis.swing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import javax.swing.JComponent;


/**
 * Tests the {@link ZoomPane}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.1
 * @since   1.1
 */
public final strictfp class ZoomPaneTest extends TestCase {
    /**
     * Creates the test case.
     */
    public ZoomPaneTest() {
        super(ZoomPane.class);
    }

    /**
     * Shows a {@link ZoomPane}.
     *
     * @param  args  ignored.
     */
    public static void main(String[] args) {
        new ZoomPaneTest().show();
    }

    /**
     * Creates the widget.
     */
    @Override
    @SuppressWarnings("serial")
    protected JComponent create(final int index) {
        final Rectangle rect = new Rectangle(100,200,100,93);
        final Polygon   poly = new Polygon(new int[] {125,175,150}, new int[] {225,225,268}, 3);
        final ZoomPane  pane = new ZoomPane(
                ZoomPane.UNIFORM_SCALE | ZoomPane.ROTATE      |
                ZoomPane.TRANSLATE_X   | ZoomPane.TRANSLATE_Y |
                ZoomPane.RESET         | ZoomPane.DEFAULT_ZOOM)
        {
            @Override public Rectangle2D getArea() {
                return rect;
            }

            @Override protected void paintComponent(final Graphics2D graphics) {
                graphics.transform(zoom);
                graphics.setColor(Color.RED);
                graphics.fill(poly);
                graphics.setColor(Color.BLUE);
                graphics.draw(poly);
                graphics.draw(rect);
            }
        };
        pane.setPaintingWhileAdjusting(true);
        return pane.createScrollPane();
    }
}
