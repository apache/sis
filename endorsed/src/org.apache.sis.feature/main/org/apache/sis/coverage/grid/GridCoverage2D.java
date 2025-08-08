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
package org.apache.sis.coverage.grid;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import static java.lang.Math.min;
import static java.lang.Math.addExact;
import static java.lang.Math.subtractExact;
import static java.lang.Math.toIntExact;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.util.NameFactory;
import org.opengis.util.InternationalString;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.MathTransform1D;
import org.apache.sis.image.DataType;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.image.privy.ImageUtilities;
import org.apache.sis.image.privy.ReshapedImage;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Debug;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coordinate.MismatchedDimensionException;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.coverage.PointOutsideCoverageException;


/**
 * Basic access to grid data values backed by a two-dimensional {@link RenderedImage}.
 * While images are two-dimensional, the coverage <em>envelope</em> may have more dimensions.
 * In other words the rendered image can be a two-dimensional slice in a <var>n</var>-dimensional space.
 * The only restriction is that the {@linkplain GridGeometry#getExtent() grid extent} has a
 * {@linkplain GridExtent#getSize(int) size} equals to 1 in all dimensions except two of them.
 *
 * <h2>Example</h2>
 * A remote sensing image may be valid only over some time range
 * (the temporal period of the satellite passing over observed area).
 * Envelopes for such grid coverage can have three dimensions:
 * the two usual ones (horizontal extent along <var>x</var> and <var>y</var>),
 * and a third dimension for start time and end time (temporal extent along <var>t</var>).
 * This "two-dimensional" grid coverage can have any number of columns along <var>x</var> axis
 * and any number of rows along <var>y</var> axis, but only one plan along <var>t</var> axis.
 * This single plan can have a lower bound (the start time) and an upper bound (the end time).
 *
 * <h2>Image size and location</h2>
 * The {@linkplain RenderedImage#getWidth() image width} and {@linkplain RenderedImage#getHeight() height}
 * must be equal to the {@linkplain GridExtent#getSize(int) grid extent size} in the two dimensions of the slice.
 * However, the image origin ({@linkplain RenderedImage#getMinX() minimal x} and {@linkplain RenderedImage#getMinY() y}
 * values) does not need to be equal to the {@linkplain GridExtent#getLow(int) grid extent low values};
 * a translation will be applied as needed.
 *
 * <h2>Image bands</h2>
 * Each band in an image is represented as a {@link SampleDimension}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.5
 * @since   1.1
 */
public class GridCoverage2D extends GridCoverage {
    /**
     * A constant for identifying code that relying on having 2 dimensions.
     * This is the minimal number of dimension required for this coverage.
     */
    static final int BIDIMENSIONAL = 2;

    /**
     * The sample values stored as a {@code RenderedImage}.
     */
    private final RenderedImage data;

    /**
     * Offsets to apply for converting grid coverage coordinates to image pixel coordinates.
     * This is {@link RenderedImage#getMinX()} − <code>{@linkplain GridExtent#getLow(int)
     * GridExtent.getLow}({@linkplain #xDimension})</code> for the <var>x</var> offset
     * and a similar formula for the <var>y</var> offset.
     */
    private final long gridToImageX, gridToImageY;

    /**
     * Indices of extent dimensions corresponding to image <var>x</var> and <var>y</var> coordinates.
     * Typical values are 0 for {@code xDimension} and 1 for {@code yDimension}, but different values
     * are allowed.
     */
    private final int xDimension, yDimension;

    /**
     * The two-dimensional components of the coordinate reference system and "grid to CRS" transform.
     * This is derived from {@link #gridGeometry} when first needed, retaining only the components at
     * dimension indices {@link #xDimension} and {@link #yDimension}. The same {@link AtomicReference}
     * instance may be shared with {@link #convertedView} and {@link #packedView}.
     *
     * @see #getGridGeometry2D()
     */
    private final AtomicReference<GridGeometry> gridGeometry2D;

    /**
     * Creates a new grid coverage for the conversion of specified source coverage.
     *
     * @param  source       the coverage containing source values.
     * @param  range        the sample dimensions to assign to the converted grid coverage.
     * @param  converters   conversion from source to converted coverage, one transform per band.
     * @param  isConverted  whether this grid coverage is for converted or packed values.
     */
    private GridCoverage2D(final GridCoverage2D source, final List<SampleDimension> range,
                           final MathTransform1D[] converters, final boolean isConverted)
    {
        super(source.gridGeometry, range);
        final DataType bandType = ConvertedGridCoverage.getBandType(range, isConverted, source);
        data           = convert(source.data, bandType, converters, Lazy.PROCESSOR);
        gridToImageX   = source.gridToImageX;
        gridToImageY   = source.gridToImageY;
        xDimension     = source.xDimension;
        yDimension     = source.yDimension;
        gridGeometry2D = source.gridGeometry2D;
    }

    /**
     * Creates a new grid coverage for the resampling of specified source coverage.
     *
     * @param  source  the coverage containing source values.
     * @param  domain  the grid extent, CRS and conversion from cell indices to CRS.
     * @param  extent  the {@code domain.getExtent()} value.
     * @param  data    the sample values as a {@link RenderedImage}, with one band for each sample dimension.
     */
    GridCoverage2D(final GridCoverage source, final GridGeometry domain, final GridExtent extent, RenderedImage data) {
        super(source, domain);
        final int[] imageAxes = extent.getSubspaceDimensions(BIDIMENSIONAL);
        xDimension     = imageAxes[0];
        yDimension     = imageAxes[1];
        this.data      = data = unwrapIfSameSize(data);
        gridToImageX   = subtractExact(data.getMinX(), extent.getLow(xDimension));
        gridToImageY   = subtractExact(data.getMinY(), extent.getLow(yDimension));
        gridGeometry2D = new AtomicReference<>();
    }

    /**
     * Constructs a grid coverage using the same domain and range than the given coverage, but different data.
     * This constructor can be used when new data have been computed by an image processing operation,
     * but each pixel of the result have the same coordinates and the same units of measurement
     * than in the source coverage.
     *
     * @param  source  the coverage from which to copy grid geometry and sample dimensions.
     * @param  data    the sample values as a {@link RenderedImage}, with one band for each sample dimension.
     * @throws IllegalGridGeometryException if the image size is not consistent with the grid geometry.
     * @throws IllegalArgumentException if the image number of bands is not the same as the number of sample dimensions.
     *
     * @since 1.2
     */
    @SuppressWarnings("this-escape")    // The invoked method does not store `this` and is not overrideable.
    public GridCoverage2D(final GridCoverage source, RenderedImage data) {
        super(source, source.getGridGeometry());
        this.data = data = unwrapIfSameSize(Objects.requireNonNull(data));
        final GridExtent extent = gridGeometry.getExtent();
        final int[] imageAxes;
        if (source instanceof GridCoverage2D) {
            final var gs = (GridCoverage2D) source;
            xDimension     = gs.xDimension;
            yDimension     = gs.yDimension;
            gridToImageX   = gs.gridToImageX;
            gridToImageY   = gs.gridToImageY;
            gridGeometry2D = gs.gridGeometry2D;
            imageAxes      = new int[] {xDimension, yDimension};
        } else {
            imageAxes      = extent.getSubspaceDimensions(BIDIMENSIONAL);
            xDimension     = imageAxes[0];
            yDimension     = imageAxes[1];
            gridToImageX   = subtractExact(data.getMinX(), extent.getLow(xDimension));
            gridToImageY   = subtractExact(data.getMinY(), extent.getLow(yDimension));
            gridGeometry2D = new AtomicReference<>();
        }
        verifyImageSize(extent, data, imageAxes);
        verifyBandCount(super.getSampleDimensions(), data);
    }

    /**
     * Constructs a grid coverage using the specified domain, range and data. If the given domain does not
     * have an extent, then a default {@link GridExtent} will be computed from given image. Otherwise the
     * {@linkplain RenderedImage#getWidth() image width} and {@linkplain RenderedImage#getHeight() height}
     * must be equal to the {@linkplain GridExtent#getSize(int) grid extent size} in the two dimensions of
     * the slice.
     *
     * <p>The image origin ({@linkplain RenderedImage#getMinX() minimal x} and {@linkplain RenderedImage#getMinY() y}
     * values) can be anywhere; it does not need to be the same as the {@linkplain GridExtent#getLow(int) grid extent
     * low values}. Translations will be applied automatically when needed.</p>
     *
     * <p>This constructor throws an {@link IllegalGridGeometryException} if one
     * of the following errors is detected in the {@code domain} argument:</p>
     * <ul>
     *   <li>The given domain has less than two dimensions.</li>
     *   <li>The given domain has more than two dimensions having an
     *       {@linkplain GridExtent#getSize(int) extent size} greater than 1.</li>
     *   <li>The extent size along <var>x</var> and <var>y</var> axes is not equal to the image width and height.</li>
     * </ul>
     *
     * @param  domain  the grid extent (may be absent), CRS and conversion from cell indices.
     *                 If {@code null} a default grid geometry will be created with no CRS and identity conversion.
     * @param  range   sample dimensions for each image band. The size of this list must be equal to the number of bands.
     *                 If {@code null}, default sample dimensions will be created with no transfer function.
     * @param  data    the sample values as a {@link RenderedImage}, with one band for each sample dimension.
     * @throws IllegalGridGeometryException if the {@code domain} does not met the above-documented conditions.
     * @throws IllegalArgumentException if the image number of bands is not the same as the number of sample dimensions.
     * @throws ArithmeticException if the distance between grid location and image location exceeds the {@code long} capacity.
     *
     * @see GridCoverageBuilder
     */
    public GridCoverage2D(GridGeometry domain, final List<? extends SampleDimension> range, RenderedImage data) {
        /*
         * The complex nesting of method calls below is a workaround
         * while waiting for JEP 447: Statements before super(…).
         */
        super(domain = addExtentIfAbsent(domain, data = unwrapIfSameSize(data)),
                defaultIfAbsent(range, data, ImageUtilities.getNumBands(data)));

        this.data = Objects.requireNonNull(data);
        /*
         * Find indices of the two dimensions of the slice. Those dimensions are usually 0 for x and 1 for y,
         * but not necessarily. A two dimensional CRS will be extracted for those dimensions later if needed.
         */
        final GridExtent extent = domain.getExtent();
        final int[] imageAxes;
        try {
            imageAxes = extent.getSubspaceDimensions(BIDIMENSIONAL);
        } catch (CannotEvaluateException e) {
            throw new IllegalGridGeometryException(e.getMessage(), e);
        }
        xDimension   = imageAxes[0];
        yDimension   = imageAxes[1];
        gridToImageX = subtractExact(data.getMinX(), extent.getLow(xDimension));
        gridToImageY = subtractExact(data.getMinY(), extent.getLow(yDimension));
        verifyImageSize(extent, data, imageAxes);
        verifyBandCount(range, data);
        gridGeometry2D = new AtomicReference<>();
    }

    /**
     * Returns the wrapped image if the only difference is a translation, or {@code data} otherwise.
     */
    private static RenderedImage unwrapIfSameSize(RenderedImage data) {
        if (data instanceof ReshapedImage) {
            final var source = ((ReshapedImage) data).source;
            if (source.getWidth() == data.getWidth() && source.getHeight() == data.getHeight()) {
                data = source;
            }
        }
        return data;
    }

    /**
     * If the given domain does not have a {@link GridExtent}, creates a new grid geometry
     * with an extent computed from the given image. The new grid will start at the same
     * location than the image and will have the same size.
     *
     * @param  domain  the domain to complete. May be {@code null}.
     * @param  data    user supplied image, or {@code null} if missing.
     * @return the potentially completed domain (may be {@code null}).
     */
    static GridGeometry addExtentIfAbsent(GridGeometry domain, final RenderedImage data) {
        if (data != null) {
            domain = addExtentIfAbsent(domain, ImageUtilities.getBounds(data));
        }
        return domain;
    }

    /**
     * If the given domain does not have a {@link GridExtent}, creates a new grid geometry
     * with an extent computed from the given image bounds. The new grid will start at the
     * same location as the image and will have the same size.
     *
     * <p>This method does nothing if the given domain already has an extent;
     * it does not verify that the extent is consistent with image size.
     * This verification should be done by the caller.</p>
     *
     * @param  domain  the domain to complete. May be {@code null}.
     * @param  bounds  image or raster bounds (cannot be {@code null}).
     * @return the potentially completed domain (may be {@code null}).
     */
    static GridGeometry addExtentIfAbsent(GridGeometry domain, final Rectangle bounds) {
        if (domain == null) {
            GridExtent extent = new GridExtent(bounds);
            domain = new GridGeometry(extent, PixelInCell.CELL_CENTER, null, null);
        } else if (!domain.isDefined(GridGeometry.EXTENT)) {
            final int dimension = domain.getDimension();
            if (dimension >= BIDIMENSIONAL) {
                CoordinateReferenceSystem crs = null;
                if (domain.isDefined(GridGeometry.CRS)) {
                    crs = domain.getCoordinateReferenceSystem();
                }
                final GridExtent extent = createExtent(dimension, bounds, crs);
                if (domain.isDefined(GridGeometry.GRID_TO_CRS)) try {
                    domain = new GridGeometry(domain, extent, null);
                } catch (TransformException e) {
                    throw new IllegalGridGeometryException(e);                  // Should never happen.
                } else {
                    domain = new GridGeometry(extent, domain.envelope, GridOrientation.HOMOTHETY);
                }
            }
        }
        return domain;
    }

    /**
     * Creates a grid extent with the low and high coordinates of the given image bounds.
     * The coordinate reference system is used for extracting grid axis names, in particular
     * the {@link DimensionNameType#VERTICAL} and {@link DimensionNameType#TIME} dimensions.
     * The {@link DimensionNameType#COLUMN} and {@link DimensionNameType#ROW} dimensions can
     * not be inferred from CRS analysis; they are added from knowledge that we have an image.
     *
     * @param  dimension  number of dimensions.
     * @param  bounds     bounds of the image for which to create a grid extent.
     * @param  crs        coordinate reference system, or {@code null} if none.
     */
    private static GridExtent createExtent(final int dimension, final Rectangle bounds, final CoordinateReferenceSystem crs) {
        final var low  = new long[dimension];
        final var high = new long[dimension];
        low [0] = bounds.x;
        low [1] = bounds.y;
        high[0] = bounds.width  + low[0] - 1;        // Inclusive.
        high[1] = bounds.height + low[1] - 1;
        DimensionNameType[] axisTypes = GridExtent.typeFromAxes(crs, dimension);
        if (axisTypes == null) {
            axisTypes = new DimensionNameType[dimension];
        }
        if (!ArraysExt.contains(axisTypes, DimensionNameType.COLUMN)) axisTypes[0] = DimensionNameType.COLUMN;
        if (!ArraysExt.contains(axisTypes, DimensionNameType.ROW))    axisTypes[1] = DimensionNameType.ROW;
        return new GridExtent(axisTypes, low, high, true);
    }

    /**
     * Verifies that the domain is consistent with image size.
     * We do not verify image location; it can be anywhere.
     */
    private static void verifyImageSize(final GridExtent extent, final RenderedImage data, final int[] imageAxes) {
        for (int i=0; i<BIDIMENSIONAL; i++) {
            final int imageSize = (i == 0) ? data.getWidth() : data.getHeight();
            final long gridSize = extent.getSize(imageAxes[i]);
            if (imageSize != gridSize) {
                throw new IllegalGridGeometryException(Resources.format(Resources.Keys.MismatchedImageSize_3, i, imageSize, gridSize));
            }
        }
    }

    /**
     * If the sample dimensions are null, creates default sample dimensions with default names.
     * The default names are "gray", "red, green, blue" or "cyan, magenta, yellow" if the color
     * model is identified as such, or numbers if the color model is not recognized.
     *
     * @param  range     the list of sample dimensions, potentially null.
     * @param  data      the image for which to build sample dimensions, or {@code null}.
     * @param  numBands  the number of bands in the given image, or 0 if none.
     * @return the given list of sample dimensions if it was non-null, or a default list otherwise.
     */
    static List<? extends SampleDimension> defaultIfAbsent(List<? extends SampleDimension> range,
                                                           final RenderedImage data, final int numBands)
    {
        if (range == null) {
            final short[] names;
            if (data != null) {
                names = ImageUtilities.bandNames(data.getColorModel(), data.getSampleModel());
            } else {
                names = ArraysExt.EMPTY_SHORT;
            }
            final SampleDimension[] sd = new SampleDimension[numBands];
            final NameFactory factory = DefaultNameFactory.provider();
            for (int i=0; i<numBands; i++) {
                final InternationalString name;
                final short k;
                if (i < names.length && (k = names[i]) != 0) {
                    name = Vocabulary.formatInternational(k);
                } else {
                    name = Vocabulary.formatInternational(Vocabulary.Keys.Band_1, i+1);
                }
                sd[i] = new SampleDimension(factory.createLocalName(null, name), null, List.of());
            }
            range = Arrays.asList(sd);
        }
        return range;
    }

    /**
     * Verifies that the number of bands in the image is equal to the number of sample dimensions.
     * The number of bands is fetched from the sample model, which in theory shall never be null.
     * However, this class has a little bit of tolerance to missing sample model.
     * It may happen when the image is used only as a matrix storage.
     */
    private static void verifyBandCount(final List<? extends SampleDimension> range, final RenderedImage data) {
        if (range != null) {
            final SampleModel sm = data.getSampleModel();
            if (sm != null) {
                final int nb = sm.getNumBands();
                final int ns = range.size();
                if (nb != ns) {
                    throw new IllegalArgumentException(Resources.format(Resources.Keys.MismatchedBandCount_2, nb, ns));
                }
            }
        }
    }

    /**
     * Returns the constant identifying the primitive type used for storing sample values.
     */
    @Override
    final DataType getBandType() {
        return DataType.forBands(data);
    }

    /**
     * Returns the two-dimensional part of this grid geometry.
     * If the {@linkplain #getGridGeometry() complete geometry} is already two-dimensional,
     * then this method returns the same geometry. Otherwise it returns a geometry for the two first
     * axes having a {@linkplain GridExtent#getSize(int) size} greater than 1 in the grid envelope.
     * Note that those axes are guaranteed to appear in the same order as in the complete geometry.
     *
     * @return the two-dimensional part of the grid geometry.
     *
     * @see #getGridGeometry()
     * @see GridGeometry#selectDimensions(int[])
     */
    public GridGeometry getGridGeometry2D() {
        GridGeometry g = gridGeometry2D.get();
        if (g == null) {
            g = gridGeometry.selectDimensions(xDimension, yDimension);
            if (!gridGeometry2D.compareAndSet(null, g)) {
                GridGeometry other = gridGeometry2D.get();
                if (other != null) return other;
            }
        }
        return g;
    }

    /**
     * Creates a grid coverage that contains real values or sample values,
     * depending if {@code converted} is {@code true} or {@code false} respectively.
     * This method is invoked by the default implementation of {@link #forConvertedValues(boolean)}
     * when first needed.
     *
     * @param  converted  {@code true} for a coverage containing converted values,
     *                    or {@code false} for a coverage containing packed values.
     * @return a coverage containing converted or packed values, depending on {@code converted} argument value.
     */
    @Override
    protected GridCoverage createConvertedValues(final boolean converted) {
        try {
            final List<SampleDimension> sources = getSampleDimensions();
            final List<SampleDimension> targets = new ArrayList<>(sources.size());
            final MathTransform1D[]  converters = ConvertedGridCoverage.converters(sources, targets, converted);
            return (converters == null) ? this : new GridCoverage2D(this, targets, converters, converted);
        } catch (NoninvertibleTransformException e) {
            throw new CannotEvaluateException(e.getMessage(), e);
        }
    }

    /**
     * Creates a new function for computing or interpolating sample values at given locations.
     *
     * <h4>Multi-threading</h4>
     * {@code Evaluator}s are not thread-safe. For computing sample values concurrently,
     * a new {@code Evaluator} instance should be created for each thread.
     *
     * @since 1.1
     */
    @Override
    public Evaluator evaluator() {
        return new PixelAccessor();
    }

    /**
     * Implementation of evaluator returned by {@link #evaluator()}.
     */
    private final class PixelAccessor extends DefaultEvaluator {
        /**
         * Creates a new evaluator for the enclosing coverage.
         */
        PixelAccessor() {
            super(GridCoverage2D.this);
        }

        /**
         * Returns a sequence of double values for a given point in the coverage.
         * The CRS of the given point may be any coordinate reference system,
         * or {@code null} for the same CRS as the coverage.
         */
        @Override
        public double[] apply(final DirectPosition point) throws CannotEvaluateException {
            try {
                final FractionalGridCoordinates gc = toGridPosition(point);
                try {
                    final int x = toIntExact(addExact(gc.getCoordinateValue(xDimension), gridToImageX));
                    final int y = toIntExact(addExact(gc.getCoordinateValue(yDimension), gridToImageY));
                    return evaluate(data, x, y);
                } catch (ArithmeticException | IndexOutOfBoundsException | DisjointExtentException ex) {
                    if (isNullIfOutside()) {
                        return null;
                    }
                    throw (PointOutsideCoverageException) new PointOutsideCoverageException(
                            gc.pointOutsideCoverage(gridGeometry.extent), point).initCause(ex);
                }
            } catch (PointOutsideCoverageException ex) {
                ex.setOffendingLocation(point);
                throw ex;
            } catch (RuntimeException | FactoryException | TransformException ex) {
                throw new CannotEvaluateException(ex.getMessage(), ex);
            }
        }
    }

    /**
     * Returns a grid data region as a rendered image. The {@code sliceExtent} argument
     * specifies the area of interest and may be {@code null} for requesting the whole image.
     * The coordinates given by {@link RenderedImage#getMinX()} and {@link RenderedImage#getMinY() getMinY()}
     * will be the image location <em>relative to</em> the location specified in {@code sliceExtent}
     * {@linkplain GridExtent#getLow(int) low coordinates} (see super-class javadoc for more discussion).
     * The {@linkplain RenderedImage#getWidth() image width} and {@linkplain RenderedImage#getHeight() height} will be
     * the {@code sliceExtent} {@linkplain GridExtent#getSize(int) sizes} if this method can honor exactly the request,
     * but this method is free to return a smaller or larger image if doing so reduce the number of data to create or copy.
     * This implementation returns a view as much as possible, without copying sample values.
     *
     * @param  sliceExtent  area of interest, or {@code null} for the whole image.
     * @return the grid slice as a rendered image. Image location is relative to {@code sliceExtent}.
     * @throws MismatchedDimensionException if the given extent does not have the same number of dimensions as this coverage.
     * @throws DisjointExtentException if the given extent does not intersect this grid coverage.
     * @throws CannotEvaluateException if this method cannot produce the rendered image for another reason.
     *
     * @see BufferedImage#getSubimage(int, int, int, int)
     */
    @Override
    @SuppressWarnings("AssertWithSideEffects")
    public RenderedImage render(GridExtent sliceExtent) throws CannotEvaluateException {
        final GridExtent extent = gridGeometry.extent;
        if (sliceExtent == null) {
            if (extent == null || (data.getMinX() == 0 && data.getMinY() == 0)) {
                return data;
            }
            sliceExtent = extent;
        } else {
            final int expected = gridGeometry.getDimension();
            final int dimension = sliceExtent.getDimension();
            if (expected != dimension) {
                throw new MismatchedDimensionException(Errors.format(
                        Errors.Keys.MismatchedDimension_3, "sliceExtent", expected, dimension));
            }
        }
        if (extent != null) {
            final int n = min(sliceExtent.getDimension(), extent.getDimension());
            for (int i=0; i<n; i++) {
                if (sliceExtent.getHigh(i) < extent.getLow(i) || sliceExtent.getLow(i) > extent.getHigh(i)) {
                    throw new DisjointExtentException(extent, sliceExtent, i);
                }
            }
        }
        try {
            /*
             * Convert the coordinates from this grid coverage coordinate system to the image coordinate system.
             * The coverage coordinates may require 64 bits integers, but after translation the (x,y) coordinates
             * should be in 32 bits integers range. Do not cast to 32 bits now however, this will be done later.
             */
            final long xmin = addExact(sliceExtent.getLow (xDimension), gridToImageX);
            final long ymin = addExact(sliceExtent.getLow (yDimension), gridToImageY);
            final long xmax = addExact(sliceExtent.getHigh(xDimension), gridToImageX);    // Inclusive
            final long ymax = addExact(sliceExtent.getHigh(yDimension), gridToImageY);
            /*
             * BufferedImage.getSubimage() returns a new image with upper-left coordinate at (0,0),
             * which is exactly what this method contract is requesting provided that the requested
             * upper-left point is inside the image.
             */
            if (data instanceof BufferedImage) {
                var result = (BufferedImage) data;
                /*
                 * BufferedImage origin should be (0, 0). But for consistency with image API,
                 * we consider it as variable.
                 */
                final long ix = result.getMinX();
                final long iy = result.getMinY();
                if (xmin >= ix && ymin >= iy) {
                    final int width  = result.getWidth();
                    final int height = result.getHeight();
                    /*
                     * Result of `ix + width` requires at most 33 bits for any `ix` value (same for y axis).
                     * Subtractions by `xmin` and `ymin` never overflow if `ix` and `iy` are zero or positive,
                     * which should always be the case with BufferedImage. The +1 is applied after subtraction
                     * instead of on `xmax` and `ymax` for avoiding overflow, since the result of `min(…)`
                     * uses at most 33 bits.
                     */
                    final int nx = toIntExact(min(xmax, ix + width  - 1) - xmin + 1);
                    final int ny = toIntExact(min(ymax, iy + height - 1) - ymin + 1);
                    if ((xmin | ymin) != 0 || nx != width || ny != height) {
                        result = result.getSubimage(toIntExact(xmin), toIntExact(ymin), nx, ny);
                    }
                    /*
                     * Workaround for https://bugs.openjdk.java.net/browse/JDK-8166038
                     * If BufferedImage cannot be used, fallback on ReshapedImage
                     * at the cost of returning an image larger than necessary.
                     * This workaround can be removed on JDK17.
                     */
                    if (org.apache.sis.image.privy.TilePlaceholder.PENDING_JDK_FIX) {
                        if (result.getTileGridXOffset() == ix && result.getTileGridYOffset() == iy) {
                            return result;
                        }
                    }
                }
            }
            /*
             * Return the backing image almost as-is (with potentially just a wrapper) for avoiding to copy data.
             * As per method contract, we shall set the (x,y) location to the difference between requested region
             * and actual region of the returned image. For example if the user requested an image starting at
             * (5,5) but the image to return starts at (1,1), then we need to set its location to (-4,-4).
             *
             * Note: we could do a special case when the result has only one tile and create a BufferedImage
             * with Raster.createChild(…), but that would force us to invoke RenderedImage.getTile(…) which
             * may force data loading earlier than desired.
             */
            final var result = new ReshapedImage(data, xmin, ymin, xmax, ymax);
            return result.isIdentity() ? result.source : result;
        } catch (ArithmeticException e) {
            throw new CannotEvaluateException(e.getMessage(), e);
        }
    }

    /**
     * Appends a "data layout" branch (if it exists) to the tree representation of this coverage.
     * That branch will be inserted between "coverage domain" and "sample dimensions" branches.
     *
     * @param  root        root of the tree where to add a branch.
     * @param  vocabulary  localized resources for vocabulary.
     * @param  column      the single column where to write texts.
     */
    @Debug
    @Override
    void appendDataLayout(final TreeTable.Node root, final Vocabulary vocabulary, final TableColumn<CharSequence> column) {
        final TreeTable.Node branch = root.newChild();
        branch.setValue(column, vocabulary.getString(Vocabulary.Keys.ImageLayout));
        final var nf = NumberFormat.getIntegerInstance(vocabulary.getLocale());
        final var pos = new FieldPosition(0);
        final var buffer = new StringBuffer();
write:  for (int item=0; ; item++) try {
            switch (item) {
                case 0: {
                    vocabulary.appendLabel(Vocabulary.Keys.Origin, buffer);
                    nf.format(data.getMinX(), buffer.append(' '),  pos);
                    nf.format(data.getMinY(), buffer.append(", "), pos);
                    break;
                }
                case 1: {
                    final int tx = data.getTileWidth();
                    final int ty = data.getTileHeight();
                    if (tx == data.getWidth() && ty == data.getHeight()) continue;
                    vocabulary.appendLabel(Vocabulary.Keys.TileSize, buffer);
                    nf.format(tx, buffer.append( ' ' ), pos);
                    nf.format(ty, buffer.append(" × "), pos);
                    break;
                }
                case 2: {
                    final String type = ImageUtilities.getDataTypeName(data.getSampleModel());
                    if (type == null) continue;
                    vocabulary.appendLabel(Vocabulary.Keys.DataType, buffer);
                    buffer.append(' ').append(type);
                    break;
                }
                case 3: {
                    final short t = ImageUtilities.getTransparencyDescription(data.getColorModel());
                    if (t != 0) {
                        final String desc = Resources.forLocale(vocabulary.getLocale()).getString(t);
                        branch.newChild().setValue(column, desc);
                    }
                    continue;
                }
                default: break write;
            }
            branch.newChild().setValue(column, buffer.toString());
            buffer.setLength(0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);      // Should never happen since we are writing to StringBuilder.
        }
    }
}
