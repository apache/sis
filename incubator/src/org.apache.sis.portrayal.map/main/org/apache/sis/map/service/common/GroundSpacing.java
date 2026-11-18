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

import java.util.Arrays;
import java.util.logging.Level;
import javax.measure.IncommensurableException;
import javax.measure.Unit;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.map.service.Scene2D;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.GeodeticCalculator;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;


/**
 * Ground distances in metres between the centers of neighbouring cells of a two-dimensional grid, for each row.
 * The distances are measured at the middle column of each row:
 *
 * <ul>
 *   <li>with the axis unit if the axes are linear (projection distortions are ignored),</li>
 *   <li>as geodesic distances on the ellipsoid of the CRS otherwise.</li>
 * </ul>
 *
 * If a distance cannot be computed, it is 1 (the values are then measured in cells).
 *
 * @author Johann Sorel (Geomatys)
 */
final class GroundSpacing {
    /**
     * Distance between the centers of horizontally adjacent cells, for each row.
     */
    private final double[] columns;

    /**
     * Distance between the centers of vertically adjacent cells, for each row.
     */
    private final double[] rows;

    /**
     * Computes the distances of the given grid.
     *
     * @param  grid  the two-dimensional grid.
     */
    GroundSpacing(final GridGeometry grid) {
        final GridExtent extent = grid.getExtent();
        final int height = Math.toIntExact(extent.getSize(1));
        columns = new double[height];
        rows    = new double[height];
        try {
            final CoordinateReferenceSystem crs = grid.getCoordinateReferenceSystem();
            final MathTransform gridToCRS = grid.getGridToCRS(PixelInCell.CELL_CENTER);
            final Unit<?> unit = crs.getCoordinateSystem().getAxis(0).getUnit();
            final GeodeticCalculator calculator = Units.isLinear(unit) ? null : GeodeticCalculator.create(crs);
            final double x = extent.getMedian(0);
            for (int y=0; y<height; y++) {
                final double gy = extent.getLow(1) + y;
                final double[] points = {x, gy, x + 1, gy, x, gy + 1};
                gridToCRS.transform(points, 0, points, 0, 3);
                if (calculator != null) {
                    columns[y] = geodesic(calculator, crs, points, 2);
                    rows[y]    = geodesic(calculator, crs, points, 4);
                } else {
                    final double toMetres = unit.getConverterToAny(Units.METRE).convert(1);
                    columns[y] = toMetres * Math.hypot(points[2] - points[0], points[3] - points[1]);
                    rows[y]    = toMetres * Math.hypot(points[4] - points[0], points[5] - points[1]);
                }
            }
        } catch (TransformException | IncommensurableException | RuntimeException e) {
            Scene2D.LOGGER.log(Level.FINE, "Cannot compute the ground size of the cells.", e);
            Arrays.fill(columns, 1);
            Arrays.fill(rows, 1);
        }
    }

    /**
     * Returns the geodesic distance in metres from the first point to the point at the given offset.
     */
    private double geodesic(final GeodeticCalculator calculator, final CoordinateReferenceSystem crs,
                            final double[] points, final int offset) throws IncommensurableException
    {
        final GeneralDirectPosition start = new GeneralDirectPosition(crs);
        final GeneralDirectPosition end   = new GeneralDirectPosition(crs);
        start.setCoordinate(0, points[0]);
        start.setCoordinate(1, points[1]);
        end.setCoordinate(0, points[offset]);
        end.setCoordinate(1, points[offset + 1]);
        calculator.setStartPoint(start);
        calculator.setEndPoint(end);
        return calculator.getDistanceUnit().getConverterToAny(Units.METRE).convert(calculator.getGeodesicDistance());
    }

    /**
     * Returns the distance between the centers of horizontally adjacent cells.
     *
     * @param  row  the row index.
     * @return the distance in metres.
     */
    double column(final int row) {
        return columns[row];
    }

    /**
     * Returns the distance between the centers of vertically adjacent cells.
     *
     * @param  row  the row index.
     * @return the distance in metres.
     */
    double row(final int row) {
        return rows[row];
    }
}
