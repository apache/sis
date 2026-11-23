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
package org.apache.sis.referencing.dggs.internal.shared;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import com.google.common.geometry.S2Loop;
import com.google.common.geometry.S2Point;
import com.google.common.geometry.S2Polygon;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.dggs.DiscreteGlobalGrid;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.storage.dggs.DiscreteGlobalGridSystems;


/**
 * Logo of a discrete global grid system: the zones of one of its grids on an orthographic view of the globe.
 *
 * <ul>
 *   <li>The globe is seen from above a point of the Earth, with north up.</li>
 *   <li>Lands are drawn faintly below the zones.</li>
 *   <li>Only the zone edges of the visible hemisphere are drawn, densified along great circles.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GlobeLogo {

    /**
     * Width and height of the logo, in pixels.
     */
    public static final int SIZE = 256;

    /**
     * Minimal number of zones of the drawn grid.
     * The coarsest grid with at least this number of zones is drawn.
     */
    static final long MIN_ZONES = 60;

    /**
     * Maximal number of zones drawn, for grids with more zones.
     */
    private static final long MAX_ZONES = 20000;

    /**
     * Latitude of the view center, in degrees.
     */
    private static final double CENTER_LATITUDE = 30;

    /**
     * Longitude of the view center, in degrees.
     */
    private static final double CENTER_LONGITUDE = 15;

    /**
     * Empty space between the globe and the image border, in pixels.
     */
    private static final double MARGIN = 6;

    /**
     * Maximal angle between two consecutive points of a densified edge, in radians.
     */
    private static final double MAX_SEGMENT_ANGLE = Math.toRadians(2);

    /**
     * Name of the map of the lands, copied from the {@code org.apache.sis.gui} module.
     *
     * <ul>
     *   <li>Equirectangular image covering 180°W to 180°E and 90°N to 90°S, one pixel per degree.</li>
     *   <li>Lands are opaque and seas are transparent.</li>
     * </ul>
     */
    private static final String LAND_MAP = "WorldMap360x180.png";

    /**
     * Colors of the globe, from the lit side to the shadowed side.
     */
    private static final Color OCEAN_LIGHT = new Color(0xB4BCC4),
                               OCEAN       = new Color(0x6E7881),
                               OCEAN_DARK  = new Color(0x2B3137);

    /**
     * Color of the lands, with the opacity of a pixel fully on land.
     */
    private static final Color LAND = new Color(255, 255, 255, 55);

    /**
     * Color of the zone edges.
     */
    private static final Color EDGES = new Color(255, 255, 255, 120);

    /**
     * Colors of the light reflection, from its center to its border.
     */
    private static final Color GLOSS     = new Color(255, 255, 255, 70),
                               GLOSS_END = new Color(255, 255, 255, 0);

    /**
     * Color of the globe outline.
     */
    private static final Color RIM = new Color(0x22272C);

    /**
     * The map of the lands, read when first needed.
     *
     * @see #readLands()
     */
    private static boolean[][] landMap;

    /**
     * Sine and cosine of the latitude and longitude of the view center.
     */
    private final double sinφ0, cosφ0, sinλ0, cosλ0;

    /**
     * Center and radius of the globe in the logo, in pixels.
     */
    private final double centerX, centerY, radius;

    /**
     * The map of the lands, as returned by {@link #readLands()}.
     */
    private final boolean[][] lands;

    /**
     * Creates a logo seen from above the given point.
     *
     * @param latitude   latitude of the view center, in degrees.
     * @param longitude  longitude of the view center, in degrees.
     * @param lands      the map of the lands, as returned by {@link #readLands()}.
     */
    GlobeLogo(final double latitude, final double longitude, final boolean[][] lands) {
        this.lands = lands;
        final double φ0 = Math.toRadians(latitude);
        final double λ0 = Math.toRadians(longitude);
        sinφ0   = Math.sin(φ0);
        cosφ0   = Math.cos(φ0);
        sinλ0   = Math.sin(λ0);
        cosλ0   = Math.cos(λ0);
        centerX = SIZE / 2.0;
        centerY = SIZE / 2.0;
        radius  = SIZE / 2.0 - MARGIN;
    }

    /**
     * Creates the logo of the given system.
     *
     * @param  dggs  the system to draw.
     * @return logo of {@value #SIZE}×{@value #SIZE} pixels with a transparent background.
     * @throws TransformException if the zones of the drawn grid cannot be computed.
     * @throws IOException if the map of the lands cannot be read.
     */
    public static RenderedImage create(final DiscreteGlobalGridSystem dggs) throws TransformException, IOException {
        final GlobeLogo logo = new GlobeLogo(CENTER_LATITUDE, CENTER_LONGITUDE, readLands());
        final BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        try {
            logo.paint(g, selectGrid(dggs.getHierarchy().getGrids()));
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * Returns the map of the lands, read when first needed.
     *
     * @return for each row from north to south and each column from west to east, whether the pixel is on land.
     * @throws IOException if the map cannot be read.
     */
    static synchronized boolean[][] readLands() throws IOException {
        if (landMap == null) {
            final BufferedImage image;
            try (InputStream in = GlobeLogo.class.getResourceAsStream(LAND_MAP)) {
                if (in == null) {
                    throw new IOException("Missing resource " + LAND_MAP);
                }
                image = ImageIO.read(in);
            }
            if (image == null) {
                throw new IOException("Unreadable resource " + LAND_MAP);
            }
            final boolean[][] map = new boolean[image.getHeight()][image.getWidth()];
            for (int row = 0; row < map.length; row++) {
                for (int column = 0; column < map[row].length; column++) {
                    map[row][column] = (image.getRGB(column, row) >>> 24) != 0;
                }
            }
            landMap = map;
        }
        return landMap;
    }

    /**
     * Returns the fraction of land around the given point, interpolated between the centers of the map pixels.
     *
     * @param  latitude   latitude in degrees.
     * @param  longitude  longitude in degrees, wrapped around if outside the [−180 … 180]° range.
     * @return value from 0 for sea to 1 for land.
     */
    double landCoverage(final double latitude, final double longitude) {
        final double x = (longitude + 180) * lands[0].length / 360 - 0.5;
        final double y = (90 - latitude)   * lands.length    / 180 - 0.5;
        final int x0 = (int) Math.floor(x);
        final int y0 = (int) Math.floor(y);
        final double fx = x - x0;
        final double fy = y - y0;
        final double top    = (1 - fx) * land(x0, y0)     + fx * land(x0 + 1, y0);
        final double bottom = (1 - fx) * land(x0, y0 + 1) + fx * land(x0 + 1, y0 + 1);
        return (1 - fy) * top + fy * bottom;
    }

    /**
     * Returns 1 if the given pixel of the map is on land, or 0 otherwise.
     * Columns wrap around the globe and rows are clamped to the poles.
     */
    private double land(final int column, final int row) {
        final boolean[] line = lands[Math.max(0, Math.min(lands.length - 1, row))];
        return line[Math.floorMod(column, line.length)] ? 1 : 0;
    }

    /**
     * Returns the coarsest grid with at least {@value #MIN_ZONES} zones, or the finest grid if none has that many.
     *
     * @param  grids  grids ordered by refinement level.
     * @return grid to draw, or {@code null} if the list is empty.
     */
    static DiscreteGlobalGrid selectGrid(final List<DiscreteGlobalGrid> grids) {
        for (final DiscreteGlobalGrid grid : grids) {
            if (grid.getZoneCount() >= MIN_ZONES) {
                return grid;
            }
        }
        return grids.isEmpty() ? null : grids.get(grids.size() - 1);
    }

    /**
     * Returns the position in the logo of the given geographic coordinates.
     *
     * @param  φ  latitude in degrees.
     * @param  λ  longitude in degrees.
     * @return position in pixels, or {@code null} if the point is on the hidden hemisphere.
     */
    Point2D project(final double φ, final double λ) {
        final double cosφ = Math.cos(Math.toRadians(φ));
        final double[] point = rotate(cosφ * Math.cos(Math.toRadians(λ)),
                                      cosφ * Math.sin(Math.toRadians(λ)),
                                      Math.sin(Math.toRadians(φ)));
        return (point[0] < 0) ? null : new Point2D.Double(x(point), y(point));
    }

    /**
     * Rotates a point of the unit sphere for bringing the view center on the first axis.
     *
     * @return the depth (positive on the visible hemisphere), the east and the north coordinates.
     */
    private double[] rotate(final double x, final double y, final double z) {
        final double x1 =  x * cosλ0 + y * sinλ0;
        final double y1 = -x * sinλ0 + y * cosλ0;
        return new double[] {x1 * cosφ0 + z * sinφ0, y1, z * cosφ0 - x1 * sinφ0};
    }

    /**
     * Returns the column in pixels of a point returned by {@link #rotate}.
     */
    private double x(final double[] point) {
        return centerX + radius * point[1];
    }

    /**
     * Returns the row in pixels of a point returned by {@link #rotate}.
     */
    private double y(final double[] point) {
        return centerY - radius * point[2];
    }

    /**
     * Paints the globe, the lands and the zones of the given grid.
     *
     * @param  grid  the grid to draw, or {@code null} for the globe and the lands only.
     */
    private void paint(final Graphics2D g, final DiscreteGlobalGrid grid) throws TransformException {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        final Ellipse2D globe = new Ellipse2D.Double(centerX - radius, centerY - radius, 2 * radius, 2 * radius);

        // Shading with light coming from the upper left.
        g.setPaint(new RadialGradientPaint(new Point2D.Double(centerX - 0.35 * radius, centerY - 0.4 * radius),
                (float) (1.45 * radius), new float[] {0, 0.55f, 1}, new Color[] {OCEAN_LIGHT, OCEAN, OCEAN_DARK}));
        g.fill(globe);
        paintLands(g);
        if (grid != null) {
            g.clip(globe);
            paintZones(g, grid);
            g.setClip(null);
        }
        g.setPaint(new RadialGradientPaint(new Point2D.Double(centerX - 0.4 * radius, centerY - 0.45 * radius),
                (float) (0.75 * radius), new float[] {0, 1}, new Color[] {GLOSS, GLOSS_END}));
        g.fill(globe);
        g.setColor(RIM);
        g.setStroke(new BasicStroke(2));
        g.draw(globe);
    }

    /**
     * Paints the lands of the visible hemisphere.
     * Each pixel of the globe is converted to geographic coordinates for reading the map of the lands.
     */
    private void paintLands(final Graphics2D g) {
        final BufferedImage layer = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        final int rgb = LAND.getRGB() & 0xFFFFFF;
        for (int row = 0; row < SIZE; row++) {
            final double north = (centerY - (row + 0.5)) / radius;
            for (int column = 0; column < SIZE; column++) {
                final double east = (column + 0.5 - centerX) / radius;
                final double r2 = east * east + north * north;
                if (r2 >= 1) continue;
                // Inverse of rotate(…), with the depth computed for a point on the unit sphere.
                final double depth = Math.sqrt(1 - r2);
                final double x1 = depth * cosφ0 - north * sinφ0;
                final double z  = depth * sinφ0 + north * cosφ0;
                final double x  = x1 * cosλ0 - east * sinλ0;
                final double y  = x1 * sinλ0 + east * cosλ0;
                final double coverage = landCoverage(Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, z)))),
                                                     Math.toDegrees(Math.atan2(y, x)));
                if (coverage > 0) {
                    final int alpha = (int) Math.round(coverage * LAND.getAlpha());
                    layer.setRGB(column, row, (alpha << 24) | rgb);
                }
            }
        }
        g.drawImage(layer, 0, 0, null);
    }

    /**
     * Paints the edges of the zones of the given grid.
     */
    private void paintZones(final Graphics2D g, final DiscreteGlobalGrid grid) throws TransformException {
        final Path2D edges = new Path2D.Double();
        try (Stream<Zone> zones = grid.getZones()) {
            final Iterator<Zone> it = zones.limit(MAX_ZONES).iterator();
            while (it.hasNext()) {
                final S2Polygon polygon = DiscreteGlobalGridSystems.toS2Polygon(it.next().getGeographicExtent());
                if (polygon != null) {
                    for (int i = 0; i < polygon.numLoops(); i++) {
                        appendVisibleEdges(edges, polygon.loop(i));
                    }
                }
            }
        }
        g.setColor(EDGES);
        g.setStroke(new BasicStroke(0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(edges);
    }

    /**
     * Appends to the path the parts of the loop edges which are on the visible hemisphere.
     * Edges crossing the horizon are cut on the globe outline.
     */
    private void appendVisibleEdges(final Path2D path, final S2Loop loop) {
        double[] previous = null;
        boolean drawing = false;
        for (final double[] point : ring(loop)) {
            final boolean visible = point[0] >= 0;
            if (visible) {
                if (!drawing) {
                    final double[] start = (previous != null) ? horizon(previous, point) : point;
                    path.moveTo(x(start), y(start));
                }
                path.lineTo(x(point), y(point));
            } else if (drawing) {
                final double[] end = horizon(previous, point);
                path.lineTo(x(end), y(end));
            }
            drawing = visible;
            previous = point;
        }
    }

    /**
     * Returns the points of the closed ring of the given loop, rotated by {@link #rotate}.
     * Edges are densified along great circles, and the first point is repeated at the end.
     */
    private List<double[]> ring(final S2Loop loop) {
        final List<double[]> points = new ArrayList<>();
        final int n = loop.numVertices();
        if (n == 0) {
            return points;
        }
        S2Point a = loop.vertex(0);
        points.add(rotate(a.getX(), a.getY(), a.getZ()));
        for (int i = 1; i <= n; i++) {
            final S2Point b = loop.vertex(i % n);
            final double cx = a.getY() * b.getZ() - a.getZ() * b.getY();
            final double cy = a.getZ() * b.getX() - a.getX() * b.getZ();
            final double cz = a.getX() * b.getY() - a.getY() * b.getX();
            final double angle = Math.atan2(Math.sqrt(cx*cx + cy*cy + cz*cz),
                                            a.getX() * b.getX() + a.getY() * b.getY() + a.getZ() * b.getZ());
            final int steps = Math.max(1, (int) Math.ceil(angle / MAX_SEGMENT_ANGLE));
            final double sin = Math.sin(angle);
            for (int k = 1; k <= steps; k++) {
                final double t = (double) k / steps;
                final double wa, wb;
                if (sin < 1E-12) {
                    wa = 1 - t;
                    wb = t;
                } else {
                    wa = Math.sin((1 - t) * angle) / sin;
                    wb = Math.sin(t * angle) / sin;
                }
                points.add(rotate(wa * a.getX() + wb * b.getX(),
                                  wa * a.getY() + wb * b.getY(),
                                  wa * a.getZ() + wb * b.getZ()));
            }
            a = b;
        }
        return points;
    }

    /**
     * Returns the point on the globe outline between a visible point and a hidden point.
     */
    private static double[] horizon(final double[] p, final double[] q) {
        final double t = p[0] / (p[0] - q[0]);
        final double east  = p[1] + t * (q[1] - p[1]);
        final double north = p[2] + t * (q[2] - p[2]);
        final double norm  = Math.hypot(east, north);
        return new double[] {0, east / norm, north / norm};
    }
}
