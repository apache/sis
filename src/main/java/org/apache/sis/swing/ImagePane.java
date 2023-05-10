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

import java.awt.BasicStroke;
import java.awt.Paint;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.ColorModel;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import javax.swing.JFrame;
import org.opengis.referencing.datum.PixelInCell;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.IncompleteGridGeometryException;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;


/**
 * Shows a {@link RenderedImage}, optionally with marks such as pixel grid or tile grid.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.1
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
     * Whether to show the pixel grid.
     */
    private boolean paintPixelGrid;

    /**
     * The paint to use for showing the pixel grid.
     */
    private static final Paint PIXEL_GRID_PAINT = Color.DARK_GRAY;

    /**
     * Whether to show the tile grid.
     */
    private boolean paintTileGrid;

    /**
     * The paint to use for showing the tile grid.
     */
    private static final Paint TILE_GRID_PAINT = Color.RED;

    /**
     * Minimal zoom factor for showing the pixel grid and the tile grid.
     */
    private static final double MIN_ZOOM_FACTOR_FOR_GRID = 10;

    /**
     * Show the given coverage in a panel. This method assumes that the coverage is two-dimensional
     * and uses an affine "grid to CRS" transform. This is not well verified because this method is
     * only for quick testing purpose.
     *
     * @param  coverage  the coverage to show.
     * @param  title     window title.
     * @return the image pane which has been shown.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static ImagePane show(final GridCoverage coverage, final String title) {
        final RenderedImage image = coverage.render(null);
        final GridGeometry gg = (GridGeometry) image.getProperty(PlanarImage.GRID_GEOMETRY_KEY);
        AffineTransform gridToCRS;
        try {
            gridToCRS = (AffineTransform) gg.getGridToCRS(PixelInCell.CELL_CORNER);
        } catch (IncompleteGridGeometryException e) {
            gridToCRS = new AffineTransform();
            System.err.println(e);
        }
        return show(image, gridToCRS, title);
    }

    /**
     * Show the given image in a panel.
     *
     * @param  image      the image to show.
     * @param  gridToCRS  the transform from pixel coordinates to "real world" coordinates.
     * @param  title      window title.
     * @return the image pane which has been shown.
     */
    public static ImagePane show(final RenderedImage image, final AffineTransform gridToCRS, final String title) {
        final ImagePane pane = new ImagePane(image, gridToCRS);
        final JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(pane.createScrollPane());
        frame.pack();
        frame.setVisible(true);
        return pane;
    }

    /**
     * Show the given raster in a panel.
     * The "grid to CRS" transform will be modified in-place.
     *
     * @param  raster     the raster to show.
     * @param  colors     the color model to use.
     * @param  gridToCRS  the transform from pixel coordinates to "real world" coordinates (modified by this method).
     * @param  title      window title.
     * @return the image pane which has been shown.
     */
    public static ImagePane show(final Raster raster, final ColorModel colors, final AffineTransform gridToCRS, final String title) {
        final Rectangle b = raster.getBounds();
        final WritableRaster wr;
        if (raster instanceof WritableRaster) {
            wr = ((WritableRaster) raster).createWritableChild(b.x, b.y, b.width, b.height, 0, 0, null);
        } else {
            wr = Raster.createWritableRaster(raster.getSampleModel(), raster.getDataBuffer(), null);
        }
        gridToCRS.translate(b.x, b.y);
        return show(new BufferedImage(colors, wr, false, null), gridToCRS, title);
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
        setBackground(Color.GRAY);
    }

    /**
     * Sets whether a grid for pixels should be shown.
     *
     * @param  visible  whether the pixel grid should be shown.
     */
    public void setPaintPixelGrid(final boolean visible) {
        firePropertyChange("paintPixelGrid", paintPixelGrid, paintPixelGrid = visible);
        repaint();
    }

    /**
     * Sets whether a grid for tiles should be shown.
     *
     * @param  visible  whether the tile grid should be shown.
     */
    public void setPaintTileGrid(final boolean visible) {
        firePropertyChange("paintTileGrid", paintTileGrid, paintTileGrid = visible);
        repaint();
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
     * Paints the image and optionally the grids.
     */
    @Override
    protected void paintComponent(final Graphics2D graphics) {
        graphics.transform(zoom);
        graphics.drawRenderedImage(image, gridToCRS);
        if ((paintPixelGrid | paintTileGrid) && AffineTransforms2D.getScale(zoom) >= MIN_ZOOM_FACTOR_FOR_GRID) {
            final int xmin = image.getMinX();
            final int ymin = image.getMinY();
            final int xmax = image.getWidth()  + xmin;
            final int ymax = image.getHeight() + ymin;
            if (paintPixelGrid) {
                graphics.setPaint(PIXEL_GRID_PAINT);
                graphics.setStroke(new BasicStroke(0));
                for (int y = ymin; y <= ymax; y++) {
                    graphics.drawLine(xmin, y, xmax, y);
                }
                for (int x = xmin; x <= xmax; x++) {
                    graphics.drawLine(x, ymin, x, ymax);
                }
            }
            if (paintTileGrid) {
                graphics.setPaint(TILE_GRID_PAINT);
                graphics.setStroke(new BasicStroke(0.1f));
                final int tileWidth  = image.getTileWidth();
                final int tileHeight = image.getTileHeight();
                final int xoff = image.getTileGridXOffset() + image.getMinTileX() * tileWidth;
                final int yoff = image.getTileGridYOffset() + image.getMinTileY() * tileHeight;
                for (int y = yoff; y <= ymax; y += tileHeight) {
                    graphics.drawLine(xmin, y, xmax, y);
                }
                for (int x = xoff; x <= xmax; x += tileWidth) {
                    graphics.drawLine(x, ymin, x, ymax);
                }
            }
        }
    }
}
