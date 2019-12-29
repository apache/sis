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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.util.NameFactory;
import org.opengis.util.InternationalString;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.internal.coverage.j2d.ConvertedGridCoverage;
import org.apache.sis.internal.feature.Resources;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.Debug;

import static java.lang.Math.min;
import static java.lang.Math.addExact;
import static java.lang.Math.subtractExact;
import static java.lang.Math.toIntExact;

// Branch-specific imports
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.coverage.PointOutsideCoverageException;


/**
 * Basic access to grid data values backed by a two-dimensional {@link RenderedImage}.
 * While images are two-dimensional, the coverage <em>envelope</em> may have more dimensions.
 * In other words the rendered image can be a two-dimensional slice in a <var>n</var>-dimensional space.
 * The only restriction is that the {@linkplain GridGeometry#getExtent() grid extent} has a
 * {@linkplain GridExtent#getSize(int) size} equals to 1 in all dimensions except two of them.
 *
 * <div class="note"><b>Example:</b>
 * a remote sensing image may be valid only over some time range
 * (the temporal period of the satellite passing over observed area).
 * Envelopes for such grid coverage can have three dimensions:
 * the two usual ones (horizontal extent along <var>x</var> and <var>y</var>),
 * and a third dimension for start time and end time (temporal extent along <var>t</var>).
 * This "two-dimensional" grid coverage can have any number of columns along <var>x</var> axis
 * and any number of rows along <var>y</var> axis, but only one plan along <var>t</var> axis.
 * This single plan can have a lower bound (the start time) and an upper bound (the end time).
 * </div>
 *
 * <h2>Image size and location</h2>
 * The {@linkplain RenderedImage#getWidth() image width} and {@linkplain RenderedImage#getHeight() height}
 * must be equal to the {@linkplain GridExtent#getSize(int) grid extent size} in the two dimensions of the slice.
 * However the image origin ({@linkplain RenderedImage#getMinX() minimal x} and {@linkplain RenderedImage#getMinY() y}
 * values) does not need to be equal to the {@linkplain GridExtent#getLow(int) grid extent low values};
 * a translation will be applied as needed.
 *
 * <h2>Image bands</h2>
 * Each band in an image is represented as a {@link SampleDimension}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class GridCoverage2D extends GridCoverage {
    /**
     * Minimal number of dimension for this coverage.
     */
    private static final int MIN_DIMENSION = 2;

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
     * dimension indices {@link #xDimension} and {@link #yDimension}.
     *
     * @see #getGridGeometry2D()
     */
    private transient GridGeometry gridGeometry2D;

    /**
     * Result of the call to {@link #forConvertedValues(boolean)} with a boolean value opposite to
     * {@link #isConverted}. This coverage is determined when first needed and may be {@code this}.
     *
     * @see #forConvertedValues(boolean)
     */
    private transient GridCoverage converse;

    /**
     * Whether all sample dimensions are already representing converted values.
     * This field has no meaning if {@link #converse} is null.
     *
     * @see #forConvertedValues(boolean)
     */
    private transient boolean isConverted;

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
     * @throws IllegalArgumentException if the image number of bands is not the same than the number of sample dimensions.
     * @throws ArithmeticException if the distance between grid location and image location exceeds the {@code long} capacity.
     */
    public GridCoverage2D(GridGeometry domain, final Collection<? extends SampleDimension> range, RenderedImage data) {
        super(domain = addExtentIfAbsent(domain, data = unwrapIfSameSize(data)), defaultIfAbsent(range, data));
        this.data = data;           // Non-null verified by addExtentIfAbsent(…, data).
        /*
         * Find indices of the two dimensions of the slice. Those dimensions are usually 0 for x and 1 for y,
         * but not necessarily. A two dimensional CRS will be extracted for those dimensions later if needed.
         */
        final GridExtent extent = domain.getExtent();
        final int[] imageAxes;
        try {
            imageAxes = extent.getSubspaceDimensions(MIN_DIMENSION);
        } catch (CannotEvaluateException e) {
            throw new IllegalGridGeometryException(e.getMessage(), e);
        }
        xDimension   = imageAxes[0];
        yDimension   = imageAxes[1];
        gridToImageX = subtractExact(data.getMinX(), extent.getLow(xDimension));
        gridToImageY = subtractExact(data.getMinY(), extent.getLow(yDimension));
        /*
         * Verifiy that the domain is consistent with image size.
         * We do not verify image location; it can be anywhere.
         */
        for (int i=0; i<MIN_DIMENSION; i++) {
            final int imageSize = (i == 0) ? data.getWidth() : data.getHeight();
            final long gridSize = extent.getSize(imageAxes[i]);
            if (imageSize != gridSize) {
                throw new IllegalGridGeometryException(Resources.format(Resources.Keys.MismatchedImageSize_3, i, imageSize, gridSize));
            }
        }
        verifyBandCount(range, data);
    }

    /**
     * Returns the wrapped image if the only difference is a translation, or {@code data} otherwise.
     * Workaround for RFE #4093999 ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.8")
    private static RenderedImage unwrapIfSameSize(RenderedImage data) {
        if (data instanceof ReshapedImage) {
            final RenderedImage image = ((ReshapedImage) data).image;
            if (image.getWidth() == data.getWidth() && image.getHeight() == data.getHeight()) {
                data = image;
            }
        }
        return data;
    }

    /**
     * If the given domain does not have a {@link GridExtent}, creates a new grid geometry
     * with an extent computed from the given image. The new grid will start at the same
     * location than the image and will have the same size.
     *
     * <p>This static method is a workaround for RFE #4093999
     * ("Relax constraint on placement of this()/super() call in constructors").</p>
     */
    @Workaround(library="JDK", version="1.8")
    private static GridGeometry addExtentIfAbsent(GridGeometry domain, final RenderedImage data) {
        ArgumentChecks.ensureNonNull("data", data);
        if (domain == null) {
            GridExtent extent = new GridExtent(data.getMinX(), data.getMinY(), data.getWidth(), data.getHeight());
            domain = new GridGeometry(extent, PixelInCell.CELL_CENTER, null, null);
        } else if (!domain.isDefined(GridGeometry.EXTENT)) {
            final int dimension = domain.getDimension();
            if (dimension >= MIN_DIMENSION) {
                CoordinateReferenceSystem crs = null;
                if (domain.isDefined(GridGeometry.CRS)) {
                    crs = domain.getCoordinateReferenceSystem();
                }
                final GridExtent extent = createExtent(dimension, data, crs);
                try {
                    domain = new GridGeometry(domain, extent, null);
                } catch (TransformException e) {
                    throw new IllegalGridGeometryException(e);                  // Should never happen.
                }
            }
        }
        return domain;
    }

    /**
     * Constructs a grid coverage using the specified envelope, range and data.
     * This convenience constructor computes a {@link GridGeometry} from the given envelope and image size.
     * This constructor assumes that all grid axes are in the same order than CRS axes and no axis is flipped.
     * This straightforward approach often results in the <var>y</var> axis to be oriented toward up,
     * not down as often expected in rendered images.
     *
     * <p>This constructor is generally not recommended because of the assumptions on axis order and directions.
     * For better control, use the constructor expecting a {@link GridGeometry} argument instead.
     * This constructor is provided mostly as a convenience for testing purposes.</p>
     *
     * @todo Not yet public. We should provide an argument controlling whether to flip Y axis.
     *
     * @param  domain  the envelope encompassing all images, from upper-left corner to lower-right corner.
     *                 If {@code null} a default grid geometry will be created with no CRS and identity conversion.
     * @param  range   sample dimensions for each image band. The size of this list must be equal to the number of bands.
     *                 If {@code null}, default sample dimensions will be created with no transfer function.
     * @param  data    the sample values as a {@link RenderedImage}, with one band for each sample dimension.
     * @throws IllegalArgumentException if the image number of bands is not the same than the number of sample dimensions.
     *
     * @see GridGeometry#GridGeometry(GridExtent, Envelope)
     */
    GridCoverage2D(final Envelope domain, final Collection<? extends SampleDimension> range, final RenderedImage data) {
        super(createGridGeometry(data, domain), defaultIfAbsent(range, data));
        this.data = data;   // Non-null verified by createGridGeometry(…, data).
        xDimension   = 0;
        yDimension   = 1;
        gridToImageX = 0;
        gridToImageY = 0;
        verifyBandCount(range, data);
    }

    /**
     * Creates a grid geometry from an envelope. The grid extent is computed from the image size.
     * This static method is a workaround for RFE #4093999
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.8")
    private static GridGeometry createGridGeometry(final RenderedImage data, final Envelope envelope) {
        ArgumentChecks.ensureNonNull("data", data);
        CoordinateReferenceSystem crs = null;
        int dimension = MIN_DIMENSION;
        if (envelope != null) {
            dimension = envelope.getDimension();
            if (dimension < MIN_DIMENSION) {
                throw new IllegalGridGeometryException(Resources.format(
                        Resources.Keys.GridEnvelopeMustBeNDimensional_1, MIN_DIMENSION));
            }
            crs = envelope.getCoordinateReferenceSystem();
        }
        return new GridGeometry(createExtent(dimension, data, crs), envelope);
    }

    /**
     * Creates a grid extent with the low and high coordinates of the given image.
     * The coordinate reference system is used for extracting grid axis names, in particular
     * the {@link DimensionNameType#VERTICAL} and {@link DimensionNameType#TIME} dimensions.
     * The {@link DimensionNameType#COLUMN} and {@link DimensionNameType#ROW} dimensions can
     * not be inferred from CRS analysis; they are added from knowledge that we have an image.
     *
     * @param  dimension  number of dimensions.
     * @param  data       the image for which to create a grid extent.
     * @param  crs        coordinate reference system, or {@code null} if none.
     */
    private static GridExtent createExtent(final int dimension, final RenderedImage data, final CoordinateReferenceSystem crs) {
        final long[] low  = new long[dimension];
        final long[] high = new long[dimension];
        low [0] = data.getMinX();
        low [1] = data.getMinY();
        high[0] = data.getWidth()  + low[0] - 1;        // Inclusive.
        high[1] = data.getHeight() + low[1] - 1;
        DimensionNameType[] axisTypes = GridExtent.typeFromAxes(crs, dimension);
        if (axisTypes == null) {
            axisTypes = new DimensionNameType[dimension];
        }
        if (!ArraysExt.contains(axisTypes, DimensionNameType.COLUMN)) axisTypes[0] = DimensionNameType.COLUMN;
        if (!ArraysExt.contains(axisTypes, DimensionNameType.ROW))    axisTypes[1] = DimensionNameType.ROW;
        return new GridExtent(axisTypes, low, high, true);
    }

    /**
     * If the sample dimensions are null, creates default sample dimensions
     * with "gray", "red, green, blue" or "cyan, magenta, yellow" names.
     */
    private static Collection<? extends SampleDimension> defaultIfAbsent(
            Collection<? extends SampleDimension> range, final RenderedImage data)
    {
        if (range == null) {
            final short[] names = ImageUtilities.bandNames(data);
            final SampleDimension[] sd = new SampleDimension[names.length];
            final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
            for (int i=0; i<names.length; i++) {
                final InternationalString name;
                final short k = names[0];
                if (k != 0) {
                    name = Vocabulary.formatInternational(k);
                } else {
                    name = Vocabulary.formatInternational(Vocabulary.Keys.Band_1, i+1);
                }
                sd[i] = new SampleDimension(factory.createLocalName(null, name), null, Collections.emptyList());
            }
            range = Arrays.asList(sd);
        }
        return range;
    }

    /**
     * Verifies that the number of bands in the image is equal to the number of sample dimensions.
     * The number of bands is fetched from the sample model, which in theory shall never be null.
     * However this class has a little bit of tolerance to missing sample model; it may happen
     * when the image is used only as a matrix storage.
     */
    private static void verifyBandCount(final Collection<? extends SampleDimension> range, final RenderedImage data) {
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
     * Returns the two-dimensional part of this grid geometry.
     * If the {@linkplain #getGridGeometry() complete geometry} is already two-dimensional,
     * then this method returns the same geometry. Otherwise it returns a geometry for the two first
     * axes having a {@linkplain GridExtent#getSize(int) size} greater than 1 in the grid envelope.
     * Note that those axes are guaranteed to appear in the same order than in the complete geometry.
     *
     * @return the two-dimensional part of the grid geometry.
     *
     * @see #getGridGeometry()
     * @see GridGeometry#reduce(int...)
     */
    public synchronized GridGeometry getGridGeometry2D() {
        if (gridGeometry2D == null) {
            gridGeometry2D = gridGeometry.reduce(xDimension, yDimension);
        }
        return gridGeometry2D;
    }

    /**
     * Returns a grid coverage that contains real values or sample values,
     * depending if {@code converted} is {@code true} or {@code false} respectively.
     *
     * If the given value is {@code true}, then the default implementation returns a grid coverage which produces
     * {@link RenderedImage} views. Those views convert each sample value on the fly. This is known to be very slow
     * if an entire raster needs to be processed, but this is temporary until another implementation is provided in
     * a future SIS release.
     *
     * @param  converted  {@code true} for a coverage containing converted values,
     *                    or {@code false} for a coverage containing packed values.
     * @return a coverage containing converted or packed values, depending on {@code converted} argument value.
     */
    @Override
    public synchronized GridCoverage forConvertedValues(final boolean converted) {
        if (converse == null) {
            isConverted = allConvertedFlagEqual(true);
            if (isConverted) {
                if (allConvertedFlagEqual(false)) {
                    // No conversion in any direction.
                    converse = this;
                } else {
                    // Data are converted and user may want a packed format.
                    converse = ConvertedGridCoverage.createFromConverted(this);
                }
            } else {
                // Anything that need conversion, even if "is packed" test is also false.
                converse = ConvertedGridCoverage.createFromPacked(this);
            }
        }
        return (converted == isConverted) ? this : converse;
    }

    /**
     * Determines whether an "is converted" or "is packed" test on all sample dimensions returns {@code true}.
     *
     * @param  converted  {@coce true} for an "is converted" test, or {@code false} for an "is packed" test.
     * @return whether all sample dimensions in this coverage pass the specified test.
     */
    private boolean allConvertedFlagEqual(final boolean converted) {
        for (final SampleDimension sd : getSampleDimensions()) {
            if (sd != sd.forConvertedValues(converted)) {
                return false;
            }
        }
        return true;
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
     *         for another reason.
     */
    @Override
    public double[] evaluate(final DirectPosition point, final double[] buffer) throws CannotEvaluateException {
        try {
            final FractionalGridCoordinates gc = toGridCoordinates(point);
            try {
                final int x = toIntExact(addExact(gc.getCoordinateValue(xDimension), gridToImageX));
                final int y = toIntExact(addExact(gc.getCoordinateValue(yDimension), gridToImageY));
                return evaluate(data, x, y, buffer);
            } catch (ArithmeticException | IndexOutOfBoundsException | DisjointExtentException ex) {
                throw (PointOutsideCoverageException) new PointOutsideCoverageException(
                        gc.pointOutsideCoverage(gridGeometry.extent), point).initCause(ex);
            }
        } catch (PointOutsideCoverageException ex) {
            ex.setOffendingLocation(point);
            throw ex;
        } catch (RuntimeException | TransformException ex) {
            throw new CannotEvaluateException(ex.getMessage(), ex);
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
     * but this method is free to return a smaller or larger image if doing so reduce the amount of data to create or copy.
     * This implementation returns a view as much as possible, without copying sample values.
     *
     * @param  sliceExtent  area of interest, or {@code null} for the whole image.
     * @return the grid slice as a rendered image. Image location is relative to {@code sliceExtent}.
     * @throws DisjointExtentException if the given extent does not intersect this grid coverage.
     * @throws CannotEvaluateException if this method can not produce the rendered image for another reason.
     *
     * @see BufferedImage#getSubimage(int, int, int, int)
     */
    @Override
    @SuppressWarnings("AssertWithSideEffects")
    public RenderedImage render(final GridExtent sliceExtent) throws CannotEvaluateException {
        if (sliceExtent == null) {
            return data;
        }
        final GridExtent extent = gridGeometry.extent;
        if (extent != null) {
            for (int i = min(sliceExtent.getDimension(), extent.getDimension()); --i >= 0;) {
                if (i != xDimension && i != yDimension) {
                    if (sliceExtent.getHigh(i) < extent.getLow(i) || sliceExtent.getLow(i) > extent.getHigh(i)) {
                        throw new DisjointExtentException(extent, sliceExtent, i);
                    }
                }
            }
        }
        try {
            /*
             * Convert the coordinates from this grid coverage coordinate system to the image coordinate system.
             * The coverage coordinates may require 64 bits integers, but after translation the (x,y) coordinates
             * should be in 32 bits integers range. Do not cast to 32 bits now however; this will be done later.
             */
            final long xmin = addExact(sliceExtent.getLow (xDimension), gridToImageX);
            final long ymin = addExact(sliceExtent.getLow (yDimension), gridToImageY);
            final long xmax = addExact(sliceExtent.getHigh(xDimension), gridToImageX);
            final long ymax = addExact(sliceExtent.getHigh(yDimension), gridToImageY);
            /*
             * BufferedImage.getSubimage() returns a new image with upper-left coordinate at (0,0),
             * which is exactly what this method contract is requesting provided that the requested
             * upper-left point is inside the image.
             */
            if (data instanceof BufferedImage) {
                final long ix = data.getMinX();
                final long iy = data.getMinY();
                if (xmin >= ix && ymin >= iy) {
                    return ((BufferedImage) data).getSubimage(toIntExact(xmin), toIntExact(ymin),
                            toIntExact(min(xmax + 1, ix + data.getWidth()  - 1) - xmin),
                            toIntExact(min(ymax + 1, iy + data.getHeight() - 1) - ymin));
                }
            }
            /*
             * Return the backing image almost as-is (with potentially just a wrapper) for avoiding to copy data.
             * As per method contract, we shall set the (x,y) location to the difference between requested region
             * and actual region of the returned image. For example if the user requested an image starting at
             * (5,5) but the image to return starts at (1,1), then we need to set its location to (-4,-4).
             */
            final ReshapedImage r = new ReshapedImage(data, xmin, ymin, xmax, ymax);
            String error; assert (error = r.verify()) != null : error;
            return r.isIdentity() ? data : r;
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
        final NumberFormat nf = NumberFormat.getIntegerInstance(vocabulary.getLocale());
        final FieldPosition pos = new FieldPosition(0);
        final StringBuffer buffer = new StringBuffer();
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
