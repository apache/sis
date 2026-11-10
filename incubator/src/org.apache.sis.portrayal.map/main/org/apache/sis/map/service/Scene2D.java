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
package org.apache.sis.map.service;

import java.awt.Graphics2D;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.measure.IncommensurableException;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.GeodeticCalculator;
import org.apache.sis.referencing.operation.transform.MathTransforms;


/**
 * Holds the rendering properties.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Scene2D {

    public static final Logger LOGGER = Logger.getLogger("org.apache.sis.internal.renderer");

    /**
     * The rendering grid geometry.
     */
    public final GridGeometry grid;
    /**
     * Graphics to render into.
     * When modified by renderers, it must be reset accordingly.
     */
    public final Graphics2D graphics;

    /**
     * Definition from OGC SLD/SE :
     * The portrayal unit “pixel” is the default unit of measure.
     * If available, the pixel size depends on the viewer client resolution, otherwise it is equal to 0.28mm * 0.28mm (~ 90 DPI).
     *
     * In facts, all displays have there own DPI, but the common is around 96dpi (old 72dpi x 4/3).
     * This dpi is the default on windows and replicated on different tools such as Apache Batik user agents.
     *
     * TODO : should we use a transform as in GraphicsConfiguration.getNormalizingTransform() ?
     */
    private double dpi = 96;

    /**
     * Cache of transforms from data CRS to display, computed when first needed.
     */
    private final Map<CoordinateReferenceSystem, MathTransform> dataToDisplay = new ConcurrentHashMap<>();

    /**
     * Size of a pixel on the ground in metres, at the center of the grid.
     * Computed when first needed, {@code NaN} if not yet computed.
     */
    private volatile double metresPerPixel = Double.NaN;

    /**
     * @param grid scene domain
     * @param graphics to paint with
     */
    public Scene2D(GridGeometry grid, Graphics2D graphics) {
        this.grid = Objects.requireNonNull(grid);
        this.graphics = Objects.requireNonNull(graphics);
    }

    /**
     * Graphics2D to use for painting data.
     * This instance should be left untouched.
     *
     * @return Graphics2D
     */
    public Graphics2D getGraphics() {
        return graphics;
    }

    /**
     * Set current rendering DPI.
     * Default is 96.
     *
     * @param dpi new DPI
     */
    public void setDpi(double dpi) {
        this.dpi = dpi;
    }

    /**
     * @return current DPI.
     */
    public double getDpi() {
        return dpi;
    }

    /**
     * Convert given distance to pixels.
     *
     * @param distance to convert
     * @param unit distance unit, not null
     * @return distance in pixels
     */
    public double toPixels(double distance, Unit<Length> unit) {
        return unit.getConverterTo(Units.INCH).convert(distance) * dpi;
    }

    /**
     * Returns the transform from the given data CRS to the display (Java2D user space).
     * The display space follows the Java2D convention where the pixel <var>i</var>
     * covers the [<var>i</var> … <var>i</var>+1] range, with 0 at the upper-left corner.
     *
     * @param  dataCrs  CRS of the data to render, or {@code null} for the grid CRS.
     * @return transform from data coordinates to display coordinates.
     * @throws RenderingException if the transform cannot be computed.
     */
    public MathTransform getDataToDisplay(CoordinateReferenceSystem dataCrs) throws RenderingException {
        final CoordinateReferenceSystem gridCrs = grid.getCoordinateReferenceSystem();
        if (dataCrs == null) {
            dataCrs = gridCrs;
        }
        MathTransform mt = dataToDisplay.get(dataCrs);
        if (mt == null) {
            try {
                final MathTransform crsToGrid = grid.getGridToCRS(PixelInCell.CELL_CORNER).inverse();
                mt = MathTransforms.concatenate(CRS.findOperation(dataCrs, gridCrs, null).getMathTransform(), crsToGrid);
            } catch (FactoryException | TransformException ex) {
                throw new RenderingException(ex);
            }
            dataToDisplay.put(dataCrs, mt);
        }
        return mt;
    }

    /**
     * Returns the number of pixels for one unit of the given symbology unit of measure.
     * The {@linkplain Units#PIXEL pixel} unit, or a null unit, returns 1.
     * Linear units (metre, foot, …) are interpreted as ground distances, as specified by OGC Symbology Encoding.
     * The ground size of a pixel is measured at the center of the grid along the first grid axis.
     *
     * @param  uom  symbology unit of measure, may be null.
     * @return number of pixels for one unit of measure.
     * @throws RenderingException if the unit is not linear or the ground resolution cannot be computed.
     */
    public double getPixelsPerUnit(Unit<?> uom) throws RenderingException {
        if (uom == null || Units.PIXEL.equals(uom)) {
            return 1.0;
        }
        if (!Units.isLinear(uom)) {
            throw new RenderingException("Unsupported symbology unit of measure : " + uom);
        }
        double mpp = metresPerPixel;
        if (Double.isNaN(mpp)) {
            metresPerPixel = mpp = computeMetresPerPixel();
        }
        return uom.asType(Length.class).getConverterTo(Units.METRE).convert(1.0) / mpp;
    }

    /**
     * Computes the ground size of a pixel in metres, at the center of the grid.
     * If the CRS axes are linear, the distance is measured in map units.
     * Otherwise (geographic CRS), the geodesic distance is used.
     */
    private double computeMetresPerPixel() throws RenderingException {
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
        } catch (TransformException | IncommensurableException | IllegalArgumentException ex) {
            throw new RenderingException(ex);
        }
    }

}
