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
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.image.WritableRaster;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import javax.swing.JComponent;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.internal.coverage.j2d.RasterFactory;
import org.apache.sis.internal.processing.image.Isolines;
import org.apache.sis.swing.ZoomPane;


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
     * The image with data as as integer numbers. Created together with floating-point version of same image,
     * and restored to {@code null} after {@code dataAsIntegers} has been assigned to a {@link ZoomPane}.
     */
    private BufferedImage dataAsIntegers;

    /**
     * The zoom pane of the image using floating-point values. This is a temporary value and is discarded
     * after the two zoom panes (on floating-point values and on integer values) have been created.
     */
    private ZoomPane zoomOnFloats;

    /**
     * Creates a new viewer for {@link Isolines}.
     */
    public IsolinesView() {
        super(Isolines.class, 2);
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
     * @param  index  a sequence number for the isoline window. Shall be 0 or 1.
     * @return a widget showing isolines.
     * @throws TransformException if an error occurred while computing isolines.
     */
    @Override
    protected JComponent create(final int index) throws TransformException {
        final BufferedImage image;
        switch (index) {
            case 0: image = createImages(); break;
            case 1: image = dataAsIntegers; dataAsIntegers = null; break;
            default: throw new AssertionError(index);
        }
        final List<Shape> shapes = new ArrayList<>();
        for (final Isolines isolines : Isolines.generate(image, new double[][] {{0x20, 0x40, 0x60, 0x80, 0xA0, 0xC0, 0xE0}}, null)) {
            shapes.addAll(isolines.polylines().values());
        }
        final ZoomPane pane = new ZoomPane() {
            /**
             * Requests a window of the size of the image to show.
             */
            @Override public Rectangle2D getArea() {
                return new Rectangle(width, height);
            }

            /**
             * Paints isolines on top of the image. Isolines interior are filled with transparent colors
             * for making easier to see if the shapes are closed polygons. If zoomed, paint pixel positions.
             */
            @Override protected void paintComponent(final Graphics2D graphics) {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                final AffineTransform otr = graphics.getTransform();
                graphics.transform(zoom);
                graphics.drawRenderedImage(image, new AffineTransform());
                graphics.setStroke(new BasicStroke(0));
                int count = 0;
                for (final Shape shape : shapes) {
                    final Color color = colors[count++ % colors.length];
                    graphics.setColor(new Color(color.getRGB() & 0x20FFFFFF, true));
                    graphics.fill(shape);
                    graphics.setColor(color);
                    graphics.draw(shape);
                }
                graphics.setTransform(otr);
                /*
                 * If the zoom allows us to have at least 10 pixels between cells, draw a grid.
                 */
                final double scale = AffineTransforms2D.getScale(zoom);
                if (scale >= 10) {
                    final Rectangle2D bounds = getVisibleArea();
                    final int sx = (int) bounds.getX();     // Rounding toward zero is what we want.
                    final int sy = (int) bounds.getY();
                    final int mx = (int) bounds.getMaxX();
                    final int my = (int) bounds.getMaxY();
                    final Point       srcPt = new Point();
                    final Point.Float tgtPt = new Point.Float();
                    final Dimension   size  = new Dimension(3,3);
                    final StringBuilder buffer = (scale >= 100) ? new StringBuilder("(") : null;
                    graphics.setColor(Color.MAGENTA);
                    for (srcPt.y = sy; srcPt.y <= my; srcPt.y++) {
                        for (srcPt.x = sx; srcPt.x <= mx; srcPt.x++) {
                            bounds.setFrame(zoom.transform(srcPt, tgtPt), size);
                            graphics.fill(bounds);
                            if (buffer != null) {
                                buffer.append(srcPt.x).append(", ").append(srcPt.y).append(')');
                                final int x = Math.round(srcPt.x);
                                final int y = Math.round(srcPt.y);
                                if (x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight()) {
                                    buffer.append(": ").append(image.getRaster().getSampleFloat(x, y, 0));
                                }
                                graphics.setColor(Color.BLACK);
                                graphics.drawString(buffer.toString(), tgtPt.x, tgtPt.y);
                                graphics.setColor(Color.MAGENTA);
                                buffer.setLength(1);
                            }
                        }
                    }
                }
            }
        };
        pane.setPreferredSize(new Dimension(width, height));
        pane.reset();
        /*
         * When user moves on the pane showing floating point values, apply the same move on the
         * pane showing integer values. We do not synchronize in the reverse direction for now.
         */
        switch (index) {
            case 0: zoomOnFloats = pane; break;
            case 1: {
                zoomOnFloats.addZoomChangeListener((event) -> pane.transform(event.getChange()));
                zoomOnFloats = null;
                break;
            }
        }
        return pane.createScrollPane();
    }

    /**
     * Creates grayscale images (floating and integer versions) with random mounts.
     * This method returns the floating point version and stores the integer version
     * in {@link #dataAsIntegers} for future use.
     */
    private BufferedImage createImages() {
        final BufferedImage dataAsFloats;
        dataAsFloats   = RasterFactory.createGrayScaleImage(DataBuffer.TYPE_FLOAT, width, height, 1, 0, 0, 255);
        dataAsIntegers = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        final WritableRaster rasterAsFloats   = dataAsFloats.getRaster();
        final WritableRaster rasterAsIntegers = dataAsIntegers.getRaster();
        final Random random = new Random();
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
                    int intValue = (int) Math.round(value);
                    value += rasterAsFloats.getSampleDouble(x, y, 0);
                    intValue +=  rasterAsIntegers.getSample(x, y, 0);
                    rasterAsIntegers.setSample(x, y, 0, Math.min(255, intValue));
                    rasterAsFloats.setSample(x, y, 0, value);
                }
            }
        }
        return dataAsFloats;
    }
}
