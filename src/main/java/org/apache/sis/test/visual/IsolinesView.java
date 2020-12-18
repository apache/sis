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
package org.apache.sis.test.visual;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.image.WritableRaster;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.swing.ZoomPane;
import org.apache.sis.internal.processing.image.Isolines;


/**
 * Generate an image with synthetic mounts and draw isolines on that image.
 * This allows a visual check of {@link Isolines} results.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class IsolinesView extends Visualization {
    /**
     * Shows a random image with isolines on it.
     *
     * @param  args  ignored.
     */
    public static void main(final String[] args) {
        new IsolinesView().show();
    }

    /**
     * Desired size of the image to test.
     */
    private final int width, height;

    /**
     * Colors to use for drawing isolines.
     */
    private final Color[] colors;

    /**
     * Creates a new viewer for {@link Isolines}.
     */
    public IsolinesView() {
        super(Isolines.class, 4);
        width  = 800;
        height = 600;
        colors = new Color[] {
            Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW, Color.ORANGE, Color.RED
        };
    }

    /**
     * Creates a widget showing a random image with isolines on it.
     * The widget uses {@link ZoomPane}.
     *
     * @param  index  a sequence number for the isoline window.
     * @return a widget showing isolines.
     * @throws TransformException if an error occurred while computing isolines.
     */
    @Override
    protected JComponent create(final int index) throws TransformException {
        final BufferedImage image = createImage(index * 1000);
        final List<Shape> shapes = new ArrayList<>();
        for (final Isolines isolines : Isolines.generate(image, new double[][] {{0x20, 0x40, 0x60, 0x80, 0xA0, 0xC0, 0xE0}}, 0, null)) {
            shapes.addAll(isolines.polylines().values());
        }
        final ZoomPane pane = new ZoomPane() {
            /** Requests a window of the size of the image to show. */
            @Override public Rectangle2D getArea() {
                return new Rectangle(width, height);
            }

            /** Paints isolines on top of the image. */
            @Override protected void paintComponent(final Graphics2D graphics) {
                graphics.transform(zoom);
                graphics.drawRenderedImage(image, new AffineTransform());
                graphics.setStroke(new BasicStroke(0));
                int count = 0;
                for (final Shape shape : shapes) {
                    graphics.setColor(colors[count++ % colors.length]);
                    graphics.draw(shape);
                }
            }
        };
        pane.setPaintingWhileAdjusting(true);
        return pane.createScrollPane();
    }

    /**
     * Creates a grayscale image of given size with random mounts.
     */
    private BufferedImage createImage(final int seed) {
        final Random         random = new Random(seed);
        final BufferedImage  image  = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        final WritableRaster raster = image.getRaster();
        for (int i=0; i<10; i++) {
            final int centerX = random.nextInt(width);
            final int centerY = random.nextInt(height);
            final double magnitude = 255d / (i+1);
            final double fx = (i+1) / (width  * -40 * (random.nextDouble() + 0.5));
            final double fy = (i+1) / (height * -40 * (random.nextDouble() + 0.5));
            for (int y=0; y<height; y++) {
                for (int x=0; x<width; x++) {
                    final int dx = x - centerX;
                    final int dy = y - centerY;
                    double value = magnitude * Math.exp(dx*dx*fx + dy*dy*fy);
                    value += raster.getSample(x, y, 0);
                    raster.setSample(x, y, 0, (int) Math.min(255, Math.round(value)));
                }
            }
        }
        return image;
    }
}
