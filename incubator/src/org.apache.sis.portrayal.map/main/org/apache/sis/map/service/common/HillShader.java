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

import java.awt.image.BufferedImage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.util.ArgumentChecks;


/**
 * Hill shading (or relief shading) of an elevation grid, for a light coming from the sun.
 * The shader is configured with builder methods, then computes the shade of each cell or an overlay image:
 *
 * {@snippet lang="java" :
 *     BufferedImage overlay = new HillShader(elevations, width, height)
 *             .setGrid(grid)
 *             .setFactor(55)
 *             .setSun(315, 45)
 *             .createOverlay();
 *     }
 *
 * The terrain slopes are computed with the Horn algorithm (weighted differences over the 3×3 neighbourhood)
 * divided by the ground distances between the cell centers. The shade is the cosine of the angle between the
 * terrain normal and the direction of the sun.
 *
 * <ul>
 *   <li>The grid is assumed oriented with north up: rows go southward and columns eastward.</li>
 *   <li>The elevations and the ground distances are in metres. Without grid, the ground distances are 1.</li>
 *   <li>A neighbour without elevation, or outside the grid, is extrapolated from the other neighbours,
 *       so that the slopes of a plane are exact up to the borders of the data.</li>
 *   <li>A flat cell has the shade sin(sun elevation).</li>
 * </ul>
 *
 * @see "OGC 18-067r4 section 10.2.2"
 * @see org.apache.sis.style.se1.ShadedRelief
 *
 * @author Johann Sorel (Geomatys)
 */
public final class HillShader {
    /**
     * Default azimuth of the sun, in degrees clockwise from north.
     * This is the north-west, the usual cartographic convention.
     */
    public static final double DEFAULT_AZIMUTH = 315;

    /**
     * Default elevation of the sun, in degrees above the ground.
     */
    public static final double DEFAULT_ELEVATION = 45;

    /**
     * Maximal alpha of the white pixels lightening the slopes facing the sun in the overlay.
     * Lower than 1 for avoiding to wash out the colors.
     */
    private static final double HIGHLIGHT = 0.5;

    /**
     * Elevation of each cell in row-major order, NaN if none.
     */
    private final double[] elevations;

    /**
     * Number of cells in a row and number of rows.
     */
    private final int width, height;

    /**
     * Ground distances between the cell centers, or {@code null} for distances of 1.
     */
    private GroundSpacing spacing;

    /**
     * Exaggeration of the relief, multiplying the slopes.
     */
    private double factor;

    /**
     * Components of the unit vector toward the sun, in the grid space (x eastward, y southward, z up).
     */
    private double sunX, sunY, sunZ;

    /**
     * Creates a shader of the given elevations, with a factor of 1 and the default sun position.
     *
     * @param  elevations  elevation of each cell in metres, in row-major order, NaN if none.
     * @param  width       number of cells in a row.
     * @param  height      number of rows.
     * @throws IllegalArgumentException if the length of the array is not {@code width} × {@code height}.
     */
    public HillShader(final double[] elevations, final int width, final int height) {
        ArgumentChecks.ensureNonNull("elevations", elevations);
        ArgumentChecks.ensureStrictlyPositive("width",  width);
        ArgumentChecks.ensureStrictlyPositive("height", height);
        if (elevations.length != width * height) {
            throw new IllegalArgumentException("Expected " + width * height + " elevations but got " + elevations.length + '.');
        }
        this.elevations = elevations;
        this.width      = width;
        this.height     = height;
        factor          = 1;
        setSun(DEFAULT_AZIMUTH, DEFAULT_ELEVATION);
    }

    /**
     * Sets the grid of the elevations, for computing the ground distances between the cell centers.
     * The distances are geodesic if the axes are not linear.
     *
     * @param  grid  the two-dimensional grid of the elevations.
     * @return this shader.
     */
    public HillShader setGrid(final GridGeometry grid) {
        spacing = new GroundSpacing(grid);
        return this;
    }

    /**
     * Sets the exaggeration of the relief.
     *
     * @param  factor  the factor multiplying the slopes. The default is 1.
     * @return this shader.
     *
     * @see "OGC 18-067r4 section 10.2.2.1"
     */
    public HillShader setFactor(final double factor) {
        ArgumentChecks.ensureFinite("factor", factor);
        this.factor = factor;
        return this;
    }

    /**
     * Sets the position of the sun.
     *
     * @param  azimuth    direction of the sun in degrees clockwise from north. The default is 315°.
     * @param  elevation  angle of the sun in degrees above the ground. The default is 45°.
     * @return this shader.
     *
     * @see "OGC 18-067r4 section 10.2.2.2"
     * @see "OGC 18-067r4 section 10.2.2.3"
     */
    public HillShader setSun(final double azimuth, final double elevation) {
        ArgumentChecks.ensureFinite("azimuth",   azimuth);
        ArgumentChecks.ensureFinite("elevation", elevation);
        final double a = Math.toRadians(azimuth);
        final double e = Math.toRadians(elevation);
        sunX =  Math.sin(a) * Math.cos(e);
        sunY = -Math.cos(a) * Math.cos(e);
        sunZ =  Math.sin(e);
        return this;
    }

    /**
     * Returns the shade of a cell.
     *
     * @param  cell  index of the cell in row-major order.
     * @return the shade between 0 (full shade) and 1 (facing the sun), or NaN if the cell has no elevation.
     */
    public double shade(final int cell) {
        final double z = elevations[cell];
        if (Double.isNaN(z)) {
            return Double.NaN;
        }
        final int x = cell % width;
        final int y = cell / width;
        final double a = neighbour(x, y, -1, -1, z), b = neighbour(x, y, 0, -1, z), c = neighbour(x, y, +1, -1, z);
        final double d = neighbour(x, y, -1,  0, z),                                f = neighbour(x, y, +1,  0, z);
        final double g = neighbour(x, y, -1, +1, z), h = neighbour(x, y, 0, +1, z), i = neighbour(x, y, +1, +1, z);
        final double p = factor * ((c + 2*f + i) - (a + 2*d + g)) / (8 * distance(spacing != null ? spacing.column(y) : 1));
        final double q = factor * ((g + 2*h + i) - (a + 2*b + c)) / (8 * distance(spacing != null ? spacing.row(y)    : 1));
        /*
         * The terrain normal is (−p, −q, 1) before normalization,
         * where p and q are the slopes along the x and y axes of the grid.
         */
        final double shade = (sunZ - p*sunX - q*sunY) / Math.sqrt(p*p + q*q + 1);
        return Math.max(0, Math.min(1, shade));
    }

    /**
     * Computes an overlay to paint over the colors to shade.
     * Flat cells and cells without elevation are fully transparent.
     * Cells less lit than a flat cell are darkened by black pixels with an alpha proportional to the shadow,
     * and cells more lit than a flat cell are lightened by white pixels with an alpha proportional to the extra light.
     * Painting this overlay with the {@code SRC_OVER} rule multiplies the brightness of darkened colors
     * by the ratio between the light received by the cell and the light received by a flat cell.
     *
     * @return the overlay of {@code width} × {@code height} pixels.
     */
    public BufferedImage createOverlay() {
        final double flat = Math.max(0, sunZ);
        final int[] argb = new int[elevations.length];
        for (int cell = 0; cell < argb.length; cell++) {
            final double shade = shade(cell);
            if (Double.isNaN(shade)) {
                continue;
            }
            if (shade < flat) {
                argb[cell] = alpha(1 - shade / flat);                                     // Black.
            } else if (flat < 1) {
                argb[cell] = alpha(HIGHLIGHT * (shade - flat) / (1 - flat)) | 0xFFFFFF;   // White.
            }
        }
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, argb, 0, width);
        return image;
    }

    /**
     * Returns the elevation of a neighbour of a cell. If the neighbour has no elevation or is outside the grid,
     * the elevation is extrapolated linearly from the opposite neighbour. If the opposite neighbour is also missing,
     * a diagonal neighbour is extrapolated from its two adjacent neighbours, and other neighbours take the center
     * elevation. Those rules give the exact elevations on a plane.
     *
     * @param  x       column of the center cell.
     * @param  y       row of the center cell.
     * @param  dx      column offset of the neighbour.
     * @param  dy      row offset of the neighbour.
     * @param  center  elevation of the center cell.
     * @return the elevation of the neighbour.
     */
    private double neighbour(final int x, final int y, final int dx, final int dy, final double center) {
        final double value = elevation(x + dx, y + dy);
        if (!Double.isNaN(value)) {
            return value;
        }
        final double opposite = elevation(x - dx, y - dy);
        if (!Double.isNaN(opposite)) {
            return 2*center - opposite;
        }
        if (dx != 0 && dy != 0) {
            return neighbour(x, y, dx, 0, center) + neighbour(x, y, 0, dy, center) - center;
        }
        return center;
    }

    /**
     * Returns the elevation of the cell at the given position, or NaN if none or outside the grid.
     */
    private double elevation(final int x, final int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return Double.NaN;
        }
        return elevations[y * width + x];
    }

    /**
     * Returns the given ground distance, or 1 if the distance is not positive.
     */
    private double distance(final double value) {
        return (value > 0) ? value : 1;
    }

    /**
     * Returns the given alpha value in [0 … 1] as the alpha bits of an ARGB value.
     */
    private int alpha(final double alpha) {
        return (int) Math.round(Math.max(0, Math.min(1, alpha)) * 255) << 24;
    }
}
