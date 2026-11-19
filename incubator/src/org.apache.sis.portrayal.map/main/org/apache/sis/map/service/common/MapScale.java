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

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.Objects;
import javax.measure.IncommensurableException;
import javax.measure.Unit;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.map.service.RenderingException;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.GeodeticCalculator;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;


/**
 * The scale of a rendering grid: the ground size of a pixel, and the scale denominator by two formulas.
 *
 * <ul>
 *   <li>The {@linkplain org.apache.sis.map.service.Scene2D#getScaleDenominator() ground scale denominator}
 *       is the {@linkplain #metresPerPixel() ground size of the pixel} at the center of the grid,
 *       divided by the {@linkplain #STANDARD_PIXEL_SIZE standardized rendering pixel size} of 0.28 mm.
 *       In a geographic CRS, the ground size is the geodesic distance, so the denominator depends on the latitude.</li>
 *   <li>The {@linkplain #nominalDenominator() nominal scale denominator} is the formula of Symbology Encoding renderers:
 *       the span of the grid converted to metres, with 6378137×2π/360 metres per degree at all latitudes,
 *       divided by the grid width at 90 pixels per inch.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class MapScale {
    /**
     * The standardized rendering pixel size in metres, defined by OGC as 0.28 mm.
     */
    public static final double STANDARD_PIXEL_SIZE = 0.00028;

    /**
     * Number of metres per degree in the nominal formula: one degree on the equator of the WGS84 ellipsoid.
     */
    private static final double NOMINAL_METRES_PER_DEGREE = 6378137.0 * 2.0 * Math.PI / 360.0;

    /**
     * Number of pixels per inch in the nominal formula.
     */
    private static final double NOMINAL_DPI = 90;

    /**
     * Length of an inch in metres.
     */
    private static final double INCH = 0.0254;

    /**
     * The rendering grid.
     */
    private final GridGeometry grid;

    /**
     * Creates the scale of the given grid.
     *
     * @param  grid  the rendering grid, with a two-dimensional CRS.
     */
    public MapScale(final GridGeometry grid) {
        this.grid = Objects.requireNonNull(grid, "grid");
    }

    /**
     * Returns the ground size of a pixel in metres, at the center of the grid along the first grid axis.
     * If the CRS axes are linear, the distance is measured in map units.
     * Otherwise (geographic CRS), the geodesic distance is used.
     *
     * @return the ground size of a pixel in metres.
     * @throws RenderingException if the size cannot be computed.
     */
    public double metresPerPixel() throws RenderingException {
        final CoordinateReferenceSystem crs = grid.getCoordinateReferenceSystem();
        try {
            final MathTransform gridToCRS = grid.getGridToCRS(PixelInCell.CELL_CENTER);
            final double[] p0 = grid.getExtent().getPointOfInterest(PixelInCell.CELL_CENTER);
            final double[] p1 = p0.clone();
            p1[0] += 1;
            final GeneralDirectPosition c0 = new GeneralDirectPosition(crs);
            final GeneralDirectPosition c1 = new GeneralDirectPosition(crs);
            gridToCRS.transform(p0, 0, c0.coordinates, 0, 1);
            gridToCRS.transform(p1, 0, c1.coordinates, 0, 1);
            final Unit<?> axisUnit = crs.getCoordinateSystem().getAxis(0).getUnit();
            if (Units.isLinear(axisUnit)) {
                final double dx = c1.coordinates[0] - c0.coordinates[0];
                final double dy = c1.coordinates[1] - c0.coordinates[1];
                return axisUnit.getConverterToAny(Units.METRE).convert(Math.hypot(dx, dy));
            }
            final GeodeticCalculator calculator = GeodeticCalculator.create(crs);
            calculator.setStartPoint(c0);
            calculator.setEndPoint(c1);
            return calculator.getDistanceUnit().getConverterToAny(Units.METRE).convert(calculator.getGeodesicDistance());
        } catch (TransformException | IncommensurableException | IllegalArgumentException e) {
            throw new RenderingException(e);
        }
    }

    /**
     * Returns the scale denominator by the nominal formula of Symbology Encoding renderers.
     * This is not an accurate geographic scale, but a value independent of the latitude for selecting rules.
     * The CRS units other than degrees are taken as metres.
     *
     * @return the nominal scale denominator.
     * @throws RenderingException if the grid to CRS transform is not affine or not invertible.
     */
    public double nominalDenominator() throws RenderingException {
        final Envelope envelope = grid.getEnvelope();
        final boolean degrees = envelope.getCoordinateReferenceSystem() instanceof GeographicCRS;
        final double metresPerUnit = degrees ? NOMINAL_METRES_PER_DEGREE : 1;
        final AffineTransform objToDisp;
        try {
            objToDisp = AffineTransforms2D.castOrCopy(grid.getGridToCRS(PixelInCell.CELL_CENTER)).createInverse();
        } catch (IllegalArgumentException | NoninvertibleTransformException e) {
            throw new RenderingException(e);
        }
        if (AffineTransforms2D.getRotation(objToDisp) != 0) {
            return (metresPerUnit * NOMINAL_DPI) / (AffineTransforms2D.getScale(objToDisp) * INCH);
        }
        final long width = grid.getExtent().getSize(0);
        return (envelope.getSpan(0) * metresPerUnit) / (width / NOMINAL_DPI * INCH);
    }
}
