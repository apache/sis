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
package org.apache.sis.internal.coverage;

import java.util.Collection;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.coverage.PointOutsideCoverageException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.FractionalGridCoordinates;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.DisjointExtentException;
import org.apache.sis.coverage.grid.IllegalGridGeometryException;
import org.apache.sis.coverage.grid.IncompleteGridGeometryException;
import org.apache.sis.internal.image.TranslatedRenderedImage;
import org.apache.sis.internal.referencing.AxisDirections;
import org.apache.sis.internal.feature.Resources;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.util.ArgumentChecks;


/**
 * Basic access to grid data values backed by a two-dimensional {@link RenderedImage}.
 * Each band in an image is represented as a {@link SampleDimension}.
 * The rendered image can be a two-dimensional slice in a <var>n</var>-dimensional space
 * (i.e. the {@linkplain GridGeometry#getEnvelope() grid geometry envelope} may have more
 * than two dimensions) provided that the {@linkplain GridExtent grid extent} have a
 * {@linkplain GridExtent#getSize size} equals to 1 in all dimensions except 2.
 *
 * <div class="note"><b>Example:</b>
 * a remote sensing image may be valid only over some time range
 * (the time of satellite pass over the observed area).
 * Envelopes for such grid coverage can have three dimensions:
 * the two usual ones (horizontal extent along <var>x</var> and <var>y</var>),
 * and a third one for start time and end time (time extent along <var>t</var>).
 * The "two-dimensional" grid coverage can have any number of columns along <var>x</var> axis
 * and any number of rows along <var>y</var> axis, but only one plan along <var>t</var> axis.
 * This single plan can have a lower bound (the start time) and an upper bound (the end time).
 * </div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class GridCoverage2D extends GridCoverage {
    /**
     * The sample values stored as a {@code RenderedImage}.
     */
    private final RenderedImage data;

    /**
     * Index of extent dimensions corresponding to image <var>x</var> and <var>y</var> coordinates.
     * Typical values are 0 for {@code xDimension} and 1 for {@code yDimension}, but different values
     * are allowed.
     */
    private final int xDimension, yDimension;

    /**
     * The two-dimensional component of the coordinate reference system, or {@code null} if unspecified.
     */
    private final CoordinateReferenceSystem crs2D;

    /**
     * Result of the call to {@link #forConvertedValues(boolean)}, created when first needed.
     */
    private transient GridCoverage converted;

    /**
     * Constructs a grid coverage using the specified domain, range and data.
     * The given RenderedImage may not start at 0,0, so does the gridExtent of the grid geometry.
     * Image 0/0 coordinate is expected to match grid extent lower corner.
     *
     * @param  domain  the grid extent, CRS and conversion from cell indices to CRS.
     * @param  range   sample dimensions for each image band.
     * @param  data    the sample values as a RenderedImage, potentially multi-banded in packed view.
     */
    public GridCoverage2D(final GridGeometry domain, final Collection<? extends SampleDimension> range, final RenderedImage data) {
        super(domain, range);
        this.data = data;
        ArgumentChecks.ensureNonNull("image", data);
        /*
         * Extract the 2D components of the coordinate reference system.
         */
        final GridExtent extent = domain.getExtent();
        final int[] imageAxes = extent.getSubspaceDimensions(2);
        xDimension = imageAxes[0];
        yDimension = imageAxes[1];
        if (domain.isDefined(GridGeometry.CRS)) {
            final CoordinateReferenceSystem crs = domain.getCoordinateReferenceSystem();
            try {
                crs2D = CRS.reduce(crs, imageAxes);
            } catch (IllegalArgumentException | FactoryException e) {
                throw new IllegalGridGeometryException("Can not create a two-dimensional CRS from " + crs.getName(), e);
            }
        } else {
            crs2D = null;
        }
        /*
         * Check that image is coherent with grid geometry.
         */
        int  actual;
        long expected;
        if ((actual = data.getWidth()) != (expected = extent.getSize(xDimension))) {
            throw new IllegalArgumentException("Image width " + actual + " does not match grid extent width " + expected);
        }
        if ((actual = data.getHeight()) != (expected = extent.getSize(yDimension))) {
            throw new IllegalArgumentException("Image height " + actual + " does not match grid extent height " + expected);
        }
        int n;
        if ((actual = data.getSampleModel().getNumBands()) != (n = range.size())) {
            throw new IllegalArgumentException("Image sample model number of bands " + actual + " does not match number of sample dimensions " + n);
        }
    }

    /**
     * Returns the two-dimensional part of this grid coverage CRS.
     * If the {@linkplain #getCoordinateReferenceSystem complete CRS} is two-dimensional,
     * then this method returns the same CRS. Otherwise it returns a CRS for the two first axis
     * having a {@linkplain GridExtent#getSize(int) size} greater than 1 in the grid envelope.
     * Note that those axis are guaranteed to appear in the same order than in the complete CRS.
     *
     * @return the two-dimensional part of the grid coverage CRS.
     * @throws IncompleteGridGeometryException if the grid geometry does not contain a CRS.
     *
     * @see #getCoordinateReferenceSystem()
     */
    public CoordinateReferenceSystem getCoordinateReferenceSystem2D() {
        if (crs2D != null) {
            return crs2D;
        }
        throw new IncompleteGridGeometryException(Resources.format(Resources.Keys.UnspecifiedCRS));
    }

    /**
     * Returns the grid to CRS 2D transform in pixel center.
     *
     * @return MathTransform grid to CRS 2D transform
     * @throws FactoryException if separating 2d transform fails.
     */
    public MathTransform getGridToCrs2D() throws FactoryException {
        TransformSeparator sep = new TransformSeparator(getGridGeometry().getGridToCRS(PixelInCell.CELL_CENTER));
        int idx = AxisDirections.indexOfColinear(getCoordinateReferenceSystem().getCoordinateSystem(), crs2D.getCoordinateSystem());
        sep.addSourceDimensionRange(idx, idx+2);
        return sep.separate();
    }

    /**
     * Returns a grid coverage that contains real values or sample values, depending if {@code converted} is {@code true}
     * or {@code false} respectively.
     *
     * If the given value is {@code false}, then the default implementation returns a grid coverage which produces
     * {@link RenderedImage} views. Those views convert each sample value on the fly. This is known to be very slow
     * if an entire raster needs to be processed, but this is temporary until another implementation is provided in
     * a future SIS release.
     *
     * @return a coverage containing converted or packed values, depending on {@code converted} argument value.
     */
    @Override
    public GridCoverage forConvertedValues(final boolean converted) {
        if (converted) {
            synchronized (this) {
                if (this.converted == null) {
                    this.converted = BufferedGridCoverage.convert(this);
                }
                return this.converted;
            }
        }
        return this;
    }

    /**
     * Returns a two-dimensional slice of grid data as a rendered image.
     * This method may return a view or a copy.
     *
     * @return the grid slice as a rendered image.
     */
    @Override
    public RenderedImage render(final GridExtent sliceExtent) throws CannotEvaluateException {
        if (sliceExtent == null || sliceExtent.equals(getGridGeometry().getExtent())) {
            return data;
        } else {
            final int subX = Math.toIntExact(sliceExtent.getLow(xDimension));
            final int subY = Math.toIntExact(sliceExtent.getLow(yDimension));
            final int subWidth = Math.toIntExact(Math.round(sliceExtent.getSize(xDimension)));
            final int subHeight = Math.toIntExact(Math.round(sliceExtent.getSize(yDimension)));

            if (data instanceof BufferedImage) {
                final BufferedImage bi = (BufferedImage) data;
                return bi.getSubimage(subX, subY, subWidth, subHeight);
            } else {
                return new TranslatedRenderedImage(data, subX, subY);
            }
        }
    }

    /**
     * Returns a sequence of double values for a given point in the coverage.
     * The CRS of the given point may be any coordinate reference system,
     * or {@code null} for the same CRS than this coverage.
     * The returned sequence contains a value for each {@linkplain SampleDimension sample dimension}.
     *
     * @param  point   the coordinate point where to evaluate.
     * @param  buffer  an array in which to store values, or {@code null} to create a new array.
     * @return the {@code buffer} array, or a newly created array if {@code buffer} was null.
     * @throws PointOutsideCoverageException if the evaluation failed because the input point
     *         has invalid coordinates.
     * @throws CannotEvaluateException if the values can not be computed at the specified coordinate
     *         for an other reason.
     */
    @Override
    public double[] evaluate(final DirectPosition point, double[] buffer) throws CannotEvaluateException {
        try {
            final FractionalGridCoordinates gc = toGridCoordinates(point);
            final int x = Math.toIntExact(gc.getCoordinateValue(xDimension));
            final int y = Math.toIntExact(gc.getCoordinateValue(yDimension));
            final int xmin = data.getMinX();
            final int ymin = data.getMinY();
            if (x >= xmin && x < xmin + (long) data.getWidth() &&
                y >= ymin && y < ymin + (long) data.getHeight())
            {
                final int tx = Math.floorDiv(x - data.getTileGridXOffset(), data.getTileWidth());
                final int ty = Math.floorDiv(y - data.getTileGridYOffset(), data.getTileHeight());
                return data.getTile(tx, ty).getPixel(x, y, buffer);
            }
        } catch (ArithmeticException | DisjointExtentException ex) {
            throw (PointOutsideCoverageException) new PointOutsideCoverageException(ex.getMessage(), point).initCause(ex);
        } catch (IllegalArgumentException | TransformException ex) {
            throw new CannotEvaluateException(ex.getMessage(), ex);
        }
        throw new PointOutsideCoverageException(null, point);
    }
}
