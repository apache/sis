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

import java.awt.Graphics2D;
import java.awt.image.RenderedImage;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import javax.swing.JFrame;
import org.opengis.referencing.datum.PixelInCell;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;


/**
 * Shows a {@link RenderedImage}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
@SuppressWarnings("serial")
public class ImagePane extends ZoomPane {
    /**
     * The data to show.
     */
    private final RenderedImage image;

    /**
     * The transform from pixel coordinates to "real world" coordinates.
     */
    private final AffineTransform gridToCRS;

    /**
     * Show the given coverage in a panel. This method assumes that the coverage is two-dimensional
     * and uses an affine "grid to CRS" transform. This is not well verified because this method is
     * only for quick testing purpose.
     *
     * @param  coverage  the coverage to show.
     */
    public static void show(final GridCoverage coverage) {
        final RenderedImage image = coverage.render(null);
        final GridGeometry gg = (GridGeometry) image.getProperty(PlanarImage.GRID_GEOMETRY_KEY);
        show(image, (AffineTransform) gg.getGridToCRS(PixelInCell.CELL_CORNER));
    }

    /**
     * Show the given image in a panel.
     *
     * @param  image      the image to show.
     * @param  gridToCRS  the transform from pixel coordinates to "real world" coordinates.
     */
    public static void show(final RenderedImage image, final AffineTransform gridToCRS) {
        final JFrame frame = new JFrame("RenderedImage");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ImagePane(image, gridToCRS).createScrollPane());
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Creates a new viewer for the given image.
     *
     * @param  image      the image to show.
     * @param  gridToCRS  the transform from pixel coordinates to "real world" coordinates.
     */
    public ImagePane(final RenderedImage image, final AffineTransform gridToCRS) {
        this.image = image;
        this.gridToCRS = gridToCRS;
    }

    /**
     * Requests a window of the size of the image to show.
     */
    @Override
    public Rectangle2D getArea() {
        final Rectangle2D.Double bounds = new Rectangle2D.Double();
        bounds.width  = image.getWidth();
        bounds.height = image.getHeight();
        return AffineTransforms2D.transform(gridToCRS, bounds, bounds);
    }

    /**
     * Paints the image.
     */
    @Override
    protected void paintComponent(final Graphics2D graphics) {
        graphics.transform(zoom);
        graphics.drawRenderedImage(image, gridToCRS);
    }
}
