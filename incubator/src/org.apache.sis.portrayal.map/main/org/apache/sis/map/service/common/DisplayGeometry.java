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

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import org.apache.sis.map.service.Scene2D;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;


/**
 * A feature geometry projected to display coordinates, as Java2D shapes.
 * Instances are filled by a {@link FeatureGeometry} with points, lines and polygons in data coordinates.
 *
 * <ul>
 *   <li>Polygons use the even-odd rule, so that holes are not filled.</li>
 *   <li>Points which cannot be projected are skipped, and the line is continued from the next valid point.</li>
 *   <li>The markers are at each point, at each vertex of the lines, and at the area centroid
 *       of each polygon computed in display coordinates.</li>
 * </ul>
 *
 * @see "OGC 18-067r4 section 9.2.1.4"
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DisplayGeometry {
    /**
     * Conversion from data coordinates to display coordinates.
     */
    private final MathTransform dataToDisplay;

    /**
     * The polygons, with their holes.
     */
    private final Path2D.Double polygons;

    /**
     * The lines, without the polygon rings.
     */
    private final Path2D.Double lines;

    /**
     * The rings of the polygons and the lines, for stroking.
     */
    private final Path2D.Double outline;

    /**
     * The positions of the points.
     */
    private final List<GraphicPlacement> points;

    /**
     * The area centroid of each polygon.
     */
    private final List<GraphicPlacement> centroids;

    /**
     * Where to draw the markers.
     */
    private final List<GraphicPlacement> markers;

    /**
     * Largest dimension of the projected components: 0 for points, 1 for lines, 2 for polygons, -1 if none.
     */
    private int dimension;

    /**
     * Bounds of all projected points.
     */
    private double xmin, ymin, xmax, ymax;

    /**
     * Creates an empty geometry.
     *
     * @param  dataToDisplay  conversion from data coordinates to display coordinates.
     */
    DisplayGeometry(final MathTransform dataToDisplay) {
        this.dataToDisplay = dataToDisplay;
        polygons  = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        lines     = new Path2D.Double();
        outline   = new Path2D.Double();
        points    = new ArrayList<>();
        centroids = new ArrayList<>();
        markers   = new ArrayList<>();
        dimension = -1;
        xmin = ymin = Double.POSITIVE_INFINITY;
        xmax = ymax = Double.NEGATIVE_INFINITY;
    }

    /**
     * Returns the number of coordinates of each point given to the {@code add} methods.
     *
     * @return the source dimension of the conversion to display coordinates.
     */
    int getSourceDimension() {
        return dataToDisplay.getSourceDimensions();
    }

    /**
     * Adds points.
     *
     * @param  coordinates  coordinates of the points, in data coordinates.
     */
    void addPoints(final double[] coordinates) {
        final double[] xy = project(coordinates);
        for (int i=0; i<xy.length; i += 2) {
            final double x = xy[i], y = xy[i+1];
            if (Double.isFinite(x) && Double.isFinite(y)) {
                final GraphicPlacement point = new GraphicPlacement(x, y, 0);
                points.add(point);
                markers.add(point);
                include(x, y);
                dimension = Math.max(dimension, 0);
            }
        }
    }

    /**
     * Adds a line.
     *
     * @param  coordinates  coordinates of the line vertices, in data coordinates.
     */
    void addLine(final double[] coordinates) {
        final double[] xy = project(coordinates);
        if (append(lines, xy, false)) {
            append(outline, xy, false);
            for (int i=0; i<xy.length; i += 2) {
                if (Double.isFinite(xy[i]) && Double.isFinite(xy[i+1])) {
                    markers.add(new GraphicPlacement(xy[i], xy[i+1], 0));
                }
            }
            dimension = Math.max(dimension, 1);
        }
    }

    /**
     * Adds a polygon, with a marker at its area centroid.
     *
     * <ul>
     *   <li>The holes are subtracted from the area whatever the orientation of the rings.</li>
     *   <li>The rings do not need to be explicitly closed.</li>
     * </ul>
     *
     * @param  rings  coordinates of the exterior ring followed by the holes, in data coordinates.
     */
    void addPolygon(final List<double[]> rings) {
        double area = 0, momentX = 0, momentY = 0;      // Twice the area and six times the first moments of area.
        double[] exteriorRing = null;
        boolean exterior = true;
        for (final double[] ring : rings) {
            final double[] xy = project(ring);
            if (append(polygons, xy, true)) {
                append(outline, xy, true);
                final int n = xy.length;
                double a = 0, mx = 0, my = 0;
                for (int i=0; i<n; i += 2) {
                    final double x0 = xy[i],           y0 = xy[i+1];
                    final double x1 = xy[(i+2) % n],   y1 = xy[(i+3) % n];
                    final double cross = x0 * y1 - x1 * y0;
                    a  += cross;
                    mx += (x0 + x1) * cross;
                    my += (y0 + y1) * cross;
                }
                final double sign = ((a >= 0) == exterior) ? 1 : -1;
                area    += sign * a;
                momentX += sign * mx;
                momentY += sign * my;
                if (exterior) {
                    exteriorRing = xy;
                }
                dimension = 2;
            } else if (exterior) {
                return;                         // Ignore the holes of an invalid exterior ring.
            }
            exterior = false;
        }
        final GraphicPlacement centroid = centroid(area, momentX, momentY, exteriorRing);
        if (centroid != null) {
            centroids.add(centroid);
            markers.add(centroid);
        }
    }

    /**
     * Returns the area centroid of a polygon, or the center of the bounds of its exterior ring if the area is zero.
     *
     * @param  area      twice the area of the polygon, holes subtracted.
     * @param  momentX   six times the first moment of area along <var>x</var>.
     * @param  momentY   six times the first moment of area along <var>y</var>.
     * @param  exterior  the (<var>x</var>,<var>y</var>) coordinates of the exterior ring, or {@code null} if none.
     * @return the centroid, or {@code null} if the coordinates are not finite.
     */
    private GraphicPlacement centroid(final double area, final double momentX, final double momentY, final double[] exterior) {
        double x = momentX / (3 * area);
        double y = momentY / (3 * area);
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            if (exterior == null) {
                return null;
            }
            double xmin = Double.POSITIVE_INFINITY, ymin = Double.POSITIVE_INFINITY;
            double xmax = Double.NEGATIVE_INFINITY, ymax = Double.NEGATIVE_INFINITY;
            for (int i=0; i<exterior.length; i += 2) {
                xmin = Math.min(xmin, exterior[i]);
                ymin = Math.min(ymin, exterior[i+1]);
                xmax = Math.max(xmax, exterior[i]);
                ymax = Math.max(ymax, exterior[i+1]);
            }
            x = (xmin + xmax) / 2;
            y = (ymin + ymax) / 2;
            if (!Double.isFinite(x) || !Double.isFinite(y)) {
                return null;
            }
        }
        return new GraphicPlacement(x, y, 0);
    }

    /**
     * Converts data coordinates to (<var>x</var>,<var>y</var>) display coordinates.
     * The points which cannot be converted are NaN.
     */
    private double[] project(final double[] coordinates) {
        final int count = coordinates.length / dataToDisplay.getSourceDimensions();
        final int target = dataToDisplay.getTargetDimensions();
        final double[] result = new double[count * target];
        Arrays.fill(result, Double.NaN);
        try {
            dataToDisplay.transform(coordinates, 0, result, 0, count);
        } catch (TransformException e) {
            Scene2D.LOGGER.log(Level.FINE, "Cannot project some points of a geometry.", e);
        }
        if (target == 2) {
            return result;
        }
        final double[] xy = new double[count * 2];
        for (int i=0; i<count; i++) {
            xy[i*2]   = result[i*target];
            xy[i*2+1] = result[i*target + 1];
        }
        return xy;
    }

    /**
     * Appends a line to the given path, skipping the points which are not finite.
     *
     * @return whether at least two points have been appended.
     */
    private boolean append(final Path2D path, final double[] xy, final boolean close) {
        int count = 0;
        boolean started = false;
        for (int i=0; i<xy.length; i += 2) {
            final double x = xy[i], y = xy[i+1];
            if (Double.isFinite(x) && Double.isFinite(y)) {
                if (started) {
                    path.lineTo(x, y);
                } else {
                    path.moveTo(x, y);
                    started = true;
                }
                include(x, y);
                count++;
            } else {
                started = false;
            }
        }
        if (close && started) {
            path.closePath();
        }
        return count >= 2;
    }

    /**
     * Expands the bounds for including the given point.
     */
    private void include(final double x, final double y) {
        xmin = Math.min(xmin, x);
        ymin = Math.min(ymin, y);
        xmax = Math.max(xmax, x);
        ymax = Math.max(ymax, y);
    }

    /**
     * Returns the largest dimension of the projected components.
     *
     * @return 0 for points, 1 for lines, 2 for polygons, or -1 if empty.
     */
    public int getDimension() {
        return dimension;
    }

    /**
     * Returns whether this geometry intersects the given area, comparing the bounds.
     *
     * @param  area  the area in display coordinates.
     * @return whether the bounds of this geometry intersect the area.
     */
    boolean intersects(final Rectangle2D area) {
        return xmin <= area.getMaxX() && xmax >= area.getMinX() && ymin <= area.getMaxY() && ymax >= area.getMinY();
    }

    /**
     * Returns the polygons to fill.
     *
     * @return the polygons with the even-odd rule, or {@code null} if none.
     */
    public Shape getPolygons() {
        return (dimension == 2) ? polygons : null;
    }

    /**
     * Returns the rings of the polygons and the lines, to stroke.
     *
     * @return the lines to stroke, or {@code null} if none.
     */
    public Shape getOutline() {
        return (dimension >= 1) ? outline : null;
    }

    /**
     * Returns the lines, without the polygon rings. This is used for placing labels along the lines.
     *
     * @return the lines, empty if none.
     */
    public Shape getLines() {
        return lines;
    }

    /**
     * Returns the positions of the points. This is used for placing labels on the points.
     *
     * @return the points, empty if none.
     */
    public List<GraphicPlacement> getPoints() {
        return points;
    }

    /**
     * Returns the area centroid of each polygon, or the center of the bounds of its exterior ring if its area is zero.
     * This is used for placing labels on the polygons.
     *
     * @return the centroids of the polygons, empty if none.
     */
    public List<GraphicPlacement> getCentroids() {
        return centroids;
    }

    /**
     * Returns where to draw the markers.
     *
     * @return the points, the vertices of the lines and the centroids of the polygons.
     */
    public List<GraphicPlacement> getMarkerPositions() {
        return markers;
    }
}
