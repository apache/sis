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
package org.apache.sis.test.widget;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import javax.swing.JPanel;


/**
 * A simple viewer of {@link Shape} objects. The shapes are resized to fill most of the window,
 * with <var>y</var> axis oriented toward up. The bounding box is drawn in gray color behind.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
@SuppressWarnings("serial")
final strictfp class ShapeViewer extends JPanel {
    /**
     * Margin to keep on each side of the window.
     */
    private static final int MARGIN = 20;

    /**
     * The shape to visualize.
     */
    private final Shape[] shapes;

    /**
     * Colors to use for drawing shapes.
     */
    private final Color[] colors = {
        Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA
    };

    /**
     * Creates a new panel for rendering the given shape.
     */
    ShapeViewer(final Shape[] shapes) {
        setBackground(Color.BLACK);
        this.shapes = shapes;
    }

    /**
     * Paints the shape.
     */
    @Override
    protected void paintComponent(final Graphics graphics) {
        super.paintComponent(graphics);
        final Graphics2D g = (Graphics2D) graphics;
        Rectangle2D bounds = null;
        for (final Shape shape : shapes) {
            final Rectangle2D b = shape.getBounds2D();
            if (bounds == null) bounds = b;
            else bounds.add(b);
        }
        if (bounds != null) {
            g.translate(MARGIN, MARGIN);
            g.scale((getWidth() - 2*MARGIN) / bounds.getWidth(), (2*MARGIN - getHeight()) / bounds.getHeight());
            g.translate(-bounds.getMinX(), -bounds.getMaxY());
            g.setStroke(new BasicStroke(0));
            g.setColor(Color.GRAY);
            g.draw(bounds);
            for (int i=0; i<shapes.length; i++) {
                g.setColor(colors[i % colors.length]);
                g.draw(shapes[i]);
            }
        }
    }
}
