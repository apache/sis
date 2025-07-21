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

import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.awt.image.RenderedImage;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;
import org.apache.sis.util.privy.UnmodifiableArrayList;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.coverage.BandedCoverage;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.SubspaceNotSpecifiedException;
import org.apache.sis.image.DataType;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.coverage.privy.SampleDimensions;
import org.apache.sis.util.collection.DefaultTreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.resources.Vocabulary;

// Specific to the main branch:
import org.opengis.geometry.MismatchedDimensionException;
import org.apache.sis.coverage.CannotEvaluateException;


/**
 * Base class of coverages with domains defined as a set of grid points.
 * The essential property of coverage is to be able to generate a value for any point within its domain.
 * Since a grid coverage is represented by a grid of values, the value returned by the coverage for a point
 * is that of the grid value whose location is nearest the point.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.4
 * @since   1.0
 */
public abstract class GridCoverage extends BandedCoverage {
    /**
     * The processor to use in calls to {@link #convert(RenderedImage, DataType, MathTransform1D[], ImageProcessor)}.
     * Wrapped in a class for lazy instantiation.
     */
    static final class Lazy {
        private Lazy() {}
        static final ImageProcessor PROCESSOR = new ImageProcessor();
    }

    /**
     * The grid extent, coordinate reference system (CRS) and conversion from cell indices to CRS.
     *
     * @see #getGridGeometry()
     */
    protected final GridGeometry gridGeometry;

    /**
     * List of sample dimension (band) information for the grid coverage. Information include such things
     * as description, the no data values, minimum and maximum values, <i>etc</i>. A coverage must have
     * at least one sample dimension. The content of this array shall never be modified.
     *
     * @see #getSampleDimensions()
     */
    private final SampleDimension[] sampleDimensions;

    /**
     * View over this grid coverage after conversion of sample values, or {@code null} if not yet created.
     * May be {@code this} if we determined that there is no conversion or the conversion is identity.
     *
     * @see #forConvertedValues(boolean)
     */
    private transient GridCoverage packedView, convertedView;

    /**
     * Constructs a grid coverage using the specified grid geometry and sample dimensions.
     * The grid geometry defines the "domain" (inputs) of the coverage function,
     * and the sample dimensions define the "range" (output) of that function.
     *
     * @param  domain  the grid extent, CRS and conversion from cell indices to CRS.
     * @param  ranges  sample dimensions for each image band.
     * @throws NullPointerException if an argument is {@code null} or if the list contains a null element.
     * @throws IllegalArgumentException if the {@code range} list is empty.
     */
    protected GridCoverage(final GridGeometry domain, final List<? extends SampleDimension> ranges) {
        gridGeometry     = Objects.requireNonNull(domain);
        sampleDimensions = ranges.toArray(SampleDimension[]::new);
        for (int i=0; i<sampleDimensions.length; i++) {
            ArgumentChecks.ensureNonNullElement("ranges", i, sampleDimensions[i]);
        }
    }

    /**
     * Constructs a new grid coverage with the same sample dimensions as the given source.
     *
     * @param  source  the source from which to copy the sample dimensions.
     * @param  domain  the grid extent, CRS and conversion from cell indices to CRS.
     */
    GridCoverage(final GridCoverage source, final GridGeometry domain) {
        gridGeometry = domain;
        sampleDimensions = source.sampleDimensions;
    }

    /**
     * Returns the coordinate reference system to which the values in grid domain are referenced.
     * This is the target coordinate reference system of the {@link GridGeometry#getGridToCRS gridToCRS}
     * math transform.
     *
     * <p>The default implementation delegates to {@link GridGeometry#getCoordinateReferenceSystem()}.</p>
     *
     * @return the "real world" CRS of this coverage.
     * @throws IncompleteGridGeometryException if the grid geometry has no CRS.
     */
    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return gridGeometry.getCoordinateReferenceSystem();
    }

    /**
     * Returns the bounding box for the coverage domain in CRS coordinates.
     * The envelope encompasses all cell surfaces, from the left border of leftmost cell
     * to the right border of the rightmost cell and similarly along other axes.
     *
     * <p>The default implementation delegates to {@link GridGeometry#getEnvelope()}.</p>
     *
     * @return the bounding box for the coverage domain in CRS coordinates.
     *
     * @since 1.2
     */
    @Override
    public Optional<Envelope> getEnvelope() {
        if (gridGeometry.isDefined(GridGeometry.ENVELOPE)) {
            return Optional.of(gridGeometry.getEnvelope());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns information about the <i>domain</i> of this grid coverage.
     * Information includes the grid extent, CRS and conversion from cell indices to CRS.
     * {@code GridGeometry} can also provide derived information like bounding box and resolution.
     *
     * @return grid extent, CRS and conversion from cell indices to CRS.
     *
     * @see org.apache.sis.storage.GridCoverageResource#getGridGeometry()
     */
    public GridGeometry getGridGeometry() {
        return gridGeometry;
    }

    /**
     * Returns information about the <i>range</i> of this grid coverage.
     * Information include names, sample value ranges, fill values and transfer functions for all bands in this grid coverage.
     * The length of the returned list should be equal to the {@linkplain java.awt.image.SampleModel#getNumBands() number of
     * bands} in the rendered image.
     *
     * @return names, value ranges, fill values and transfer functions for all bands in this grid coverage.
     *
     * @see org.apache.sis.storage.GridCoverageResource#getSampleDimensions()
     */
    @Override
    public List<SampleDimension> getSampleDimensions() {
        return UnmodifiableArrayList.wrap(sampleDimensions);
    }

    /**
     * Returns the range of values in each sample dimension, or {@code null} if none.
     */
    private NumberRange<?>[] getRanges() {
        NumberRange<?>[] ranges = null;
        for (int i=0; i<sampleDimensions.length; i++) {
            final Optional<NumberRange<?>> r = sampleDimensions[i].getSampleRange();
            if (r.isPresent()) {
                if (ranges == null) {
                    ranges = new NumberRange<?>[sampleDimensions.length];
                }
                ranges[i] = r.get();
            }
        }
        return ranges;
    }

    /**
     * Returns the background value of each sample dimension.
     * The array length is the number of sample dimensions (bands).
     * Some array element may be {@code null} if the corresponding band has no background value.
     *
     * @return background value of each sample dimension.
     *
     * @see SampleDimension#getBackground()
     */
    final Number[] getBackground() {
        return SampleDimensions.backgrounds(getSampleDimensions());
    }

    /**
     * Returns the data type identifying the primitive type used for storing sample values in each band.
     * We assume no packed sample model (e.g. no packing of 4 byte ARGB values in a single 32-bits integer).
     * If the sample model is packed, the value returned by this method should be as if the image has been
     * converted to a banded sample model.
     */
    DataType getBandType() {
        return DataType.DOUBLE;     // Most conservative value, should be overridden by subclasses.
    }

    /**
     * Returns a grid coverage that contains real values or sample values, depending if {@code converted} is {@code true}
     * or {@code false} respectively. If there is no {@linkplain SampleDimension#getTransferFunction() transfer function}
     * defined by the {@linkplain #getSampleDimensions() sample dimensions}, then this method returns {@code this}.
     * In all cases, the returned grid coverage <var>r</var> has the following properties:
     *
     * <ul>
     *   <li>The list returned by {@code r.getSampleDimensions()} is equal to the list returned by
     *       <code>this.{@linkplain #getSampleDimensions()}</code> with each element <var>e</var> replaced by
     *       <code>e.{@linkplain SampleDimension#forConvertedValues(boolean) forConvertedValues}(converted)</code>.</li>
     *   <li>The {@link RenderedImage} produced by {@code r.render(extent)} is equivalent to the image returned by
     *       <code>this.{@linkplain #render(GridExtent) render}(extent)</code> with all sample values converted
     *       using the transfer function if {@code converted} is {@code true}, or the inverse of transfer function
     *       if {@code converted} is {@code false}.</li>
     * </ul>
     *
     * The default implementation delegates to {@link #createConvertedValues(boolean)} when first needed,
     * then caches the result for future invocations.
     *
     * @param  converted  {@code true} for a coverage containing converted values,
     *                    or {@code false} for a coverage containing packed values.
     * @return a coverage containing converted or packed values, depending on {@code converted} argument value.
     *         May be {@code this} but never {@code null}.
     * @throws CannotEvaluateException if an error occurred while conversion the values.
     *
     * @see SampleDimension#forConvertedValues(boolean)
     */
    public synchronized GridCoverage forConvertedValues(final boolean converted) {
        GridCoverage view = converted ? convertedView : packedView;
        if (view == null) {
            view = createConvertedValues(converted);
            if (converted) {
                convertedView = view;
                if (view != this) {
                    view.packedView = this;
                }
            } else {
                packedView = view;
                if (view != this) {
                    view.convertedView = this;
                }
            }
        }
        return view;
    }

    /**
     * Creates the grid coverage instance for the converted or packed values.
     * This method is invoked by {@link #forConvertedValues(boolean)} when first needed.
     * Then the result returned by this method is cached for future invocations
     * of {@code forConvertedValues(converted)}.
     *
     * <p>Subclasses can override this method for customizing the converted coverages
     * while leverage the caching done by {@link #forConvertedValues(boolean)}.</p>
     *
     * @param  converted  {@code true} for a coverage containing converted values,
     *                    or {@code false} for a coverage containing packed values.
     * @return a new coverage containing converted or packed values, depending on {@code converted} argument value.
     *         May be {@code this} but never {@code null}.
     * @throws CannotEvaluateException if an error occurred while conversion the values.
     *
     * @since 1.3
     */
    protected GridCoverage createConvertedValues(final boolean converted) {
        try {
            return ConvertedGridCoverage.create(this, converted);
        } catch (NoninvertibleTransformException e) {
            throw new CannotEvaluateException(e.getMessage(), e);
        }
    }

    /**
     * Creates a new image of the given data type which will compute values using the given converters.
     * The {@link #sampleDimensions} declared in this {@code GridCoverage} instances shall be applicable
     * to the returned image, as it will be assigned to the image property
     * {@value org.apache.sis.image.PlanarImage#SAMPLE_DIMENSIONS_KEY}.
     *
     * @param  source      the image for which to convert sample values.
     * @param  bandType    the type of data in the bands resulting from conversion of given image.
     * @param  converters  the transfer functions to apply on each band of the source image.
     * @param  processor   the processor to use for creating the tiles of converted values.
     * @return the image which compute converted values from the given source.
     */
    final RenderedImage convert(final RenderedImage source, final DataType bandType,
            final MathTransform1D[] converters, final ImageProcessor processor)
    {
        final List<SampleDimension> ranges = getSampleDimensions();
        try {
            SampleDimensions.IMAGE_PROCESSOR_ARGUMENT.set(ranges);
            return processor.convert(source, getRanges(), converters, bandType);
        } finally {
            SampleDimensions.IMAGE_PROCESSOR_ARGUMENT.remove();
        }
    }

    /**
     * Creates a new function for computing or interpolating sample values at given locations.
     * That function accepts {@link DirectPosition} in arbitrary Coordinate Reference System;
     * conversions to grid indices are applied as needed.
     *
     * <h4>Multi-threading</h4>
     * {@code Evaluator}s are not thread-safe. For computing sample values concurrently,
     * a new {@code Evaluator} instance should be created for each thread by invoking this
     * method multiply times.
     *
     * @return a new function for computing or interpolating sample values.
     *
     * @since 1.1
     */
    @Override
    public Evaluator evaluator() {
        return new DefaultEvaluator(this);
    }

    /**
     * Interpolates values of sample dimensions at given positions.
     * Values are computed by calls to {@link #apply(DirectPosition)} and are returned as {@code double[]}.
     * This method extends {@link BandedCoverage.Evaluator} with the addition of some methods specific to
     * gridded data.
     *
     * <h2>Multi-threading</h2>
     * Evaluators are not thread-safe. An instance of {@code Evaluator} should be created
     * for each thread that need to interpolate sample values.
     *
     * @author  Johann Sorel (Geomatys)
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.3
     *
     * @see GridCoverage#evaluator()
     *
     * @since 1.3
     */
    public interface Evaluator extends BandedCoverage.Evaluator {
        /**
         * Returns the grid coverage from which this evaluator is computing sample values.
         * This is <em>usually</em> the instance on which the {@link GridCoverage#evaluator()}
         * method has been invoked, but not necessarily. Evaluators are allowed to fetch values
         * from a different source for better performances or accuracies.
         *
         * <h4>Example</h4>
         * If the values of the enclosing coverage are interpolated from the values of another coverage,
         * then this evaluator may use directly the values of the latter coverage. Doing so avoid to add
         * more interpolations on values that are already interpolated.
         *
         * @return the source of sample values for this evaluator.
         *
         * @see #toGridCoordinates(DirectPosition)
         */
        @Override
        GridCoverage getCoverage();

        /**
         * Returns the default slice where to perform evaluation, or an empty map if unspecified.
         * Keys are dimensions from 0 inclusive to {@link GridGeometry#getDimension()} exclusive,
         * and values are the grid coordinates of the slice in the dimension specified by the key.
         *
         * <p>This information allows to invoke {@link #apply(DirectPosition)} with for example
         * two-dimensional points even if the underlying coverage is three-dimensional.
         * The missing coordinate values are replaced by the values provided in the map.</p>
         *
         * @return the default slice where to perform evaluation, or an empty map if unspecified.
         */
        Map<Integer,Long> getDefaultSlice();

        /**
         * Sets the default slice where to perform evaluation when the points do not have enough dimensions.
         * A {@code null} argument restores the default value, which is to infer the slice from the coverage
         * grid geometry.
         *
         * @param  slice  the default slice where to perform evaluation, or an empty map if none.
         * @throws IllegalArgumentException if the map contains an illegal dimension or grid coordinate value.
         *
         * @see GridExtent#getSliceCoordinates()
         */
        void setDefaultSlice(Map<Integer,Long> slice);

        /**
         * Converts the specified geospatial position to grid coordinates. If the given position is associated to
         * a non-null coordinate reference system (CRS) different than the {@linkplain #getCoverage() coverage} CRS,
         * then this method automatically transforms that position to the {@linkplain #getCoordinateReferenceSystem()
         * coverage CRS} before to compute grid coordinates.
         *
         * <p>The returned value are coordinates in the grid of the coverage returned by {@link #getCoverage()}.
         * This is <em>usually</em> the coverage instance on which {@link GridCoverage#evaluator()} has been invoked,
         * but not necessarily. Evaluators are allowed to fetch values from a different source for better performances
         * or accuracies.</p>
         *
         * <p>This method does not put any restriction on the grid coordinates result.
         * The result may be outside the {@linkplain GridGeometry#getExtent() grid extent}
         * if the {@linkplain GridGeometry#getGridToCRS(PixelInCell) grid to CRS} transform allows it.</p>
         *
         * @param  point  geospatial coordinates (in arbitrary CRS) to transform to grid coordinates.
         * @return the grid coordinates for the given geospatial coordinates.
         * @throws IncompleteGridGeometryException if the {@linkplain GridCoverage#getGridGeometry() grid geometry}
         *         does not define a "grid to CRS" transform, or if the given point has a non-null CRS but the
         *         coverage does not {@linkplain GridCoverage#getCoordinateReferenceSystem() have a CRS}.
         * @throws TransformException if the given coordinates cannot be transformed.
         *
         * @see FractionalGridCoordinates#toPosition(MathTransform)
         */
        FractionalGridCoordinates toGridCoordinates(final DirectPosition point) throws TransformException;
    }

    /**
     * Returns a two-dimensional slice of grid data as a rendered image. The given {@code sliceExtent} argument specifies
     * the coordinates of the slice in all dimensions that are not in the two-dimensional image. For example if this grid
     * coverage has (<var>x</var>,<var>y</var>,<var>z</var>,<var>t</var>) dimensions and we want to render an image
     * of data in the (<var>x</var>,<var>y</var>) dimensions, then the given {@code sliceExtent} shall contain the
     * (<var>z</var>,<var>t</var>) coordinates of the desired slice. Those coordinates are specified in a grid extent
     * where {@linkplain GridExtent#getLow(int) low coordinate} = {@linkplain GridExtent#getHigh(int) high coordinate} in the
     * <var>z</var> and <var>t</var> dimensions. The two dimensions of the data to be shown (<var>x</var> and <var>y</var>
     * in our example) shall be the only dimensions having a {@linkplain GridExtent#getSize(int) size} greater than 1 cell.
     *
     * <p>If the {@code sliceExtent} argument is {@code null}, then the default value is
     * <code>{@linkplain #getGridGeometry()}.{@linkplain GridGeometry#getExtent() getExtent()}</code>.
     * This means that {@code gridExtent} is optional for two-dimensional grid coverages or grid coverages where all dimensions
     * except two have a size of 1 cell. If the grid extent contains more than 2 dimensions with a size greater than one cell,
     * then a {@link SubspaceNotSpecifiedException} is thrown.</p>
     *
     * <h4>How to compute a slice extent from a slice point in "real world" coordinates</h4>
     * The {@code sliceExtent} is specified to this method as grid indices. If the <var>z</var> and <var>t</var> values
     * are not grid indices but are relative to some Coordinate Reference System (CRS) instead, then the slice extent
     * can be computed as below. First, a <i>slice point</i> containing the <var>z</var> and <var>t</var> coordinates
     * should be constructed as a {@link DirectPosition} in one of the following ways:
     *
     * <ul>
     *   <li>The {@code slicePoint} has a CRS with two dimensions less than this grid coverage CRS.</li>
     *   <li>The {@code slicePoint} has the same CRS as this grid coverage, but the two coordinates to
     *       exclude are set to {@link Double#NaN}.</li>
     * </ul>
     *
     * Then:
     *
     * {@snippet lang="java" :
     *     sliceExtent = getGridGeometry().derive().slice(slicePoint).getIntersection();
     *     }
     *
     * If the {@code slicePoint} CRS is different than this grid coverage CRS (except for the number of dimensions),
     * a coordinate transformation will be applied as needed.
     *
     * <h4>Characteristics of the returned image</h4>
     * Image dimensions <var>x</var> and <var>y</var> map to the first and second dimension respectively of
     * the two-dimensional {@code sliceExtent} {@linkplain GridExtent#getSubspaceDimensions(int) subspace}.
     * The coordinates given by {@link RenderedImage#getMinX()} and {@link RenderedImage#getMinY() getMinY()}
     * will be the image location <em>relative to</em> the location specified in {@code sliceExtent}
     * {@linkplain GridExtent#getLow(int) low coordinates}.
     * For example, in the case of image {@linkplain RenderedImage#getMinX() minimum X coordinate}:
     *
     * <ul class="verbose">
     *   <li>A value of 0 means that the image left border is exactly where requested by {@code sliceExtent.getLow(xDimension)}.</li>
     *   <li>A positive value means that the returned image is shifted to the right compared to specified extent.
     *       This implies that the image has less data than requested on left side.
     *       It may happen if the specified extent is partially outside grid coverage extent.</li>
     *   <li>A negative value means that the returned image is shifted to the left compared to specified extent.
     *       This implies that the image has more data than requested on left side. It may happen if the image is tiled,
     *       the specified {@code sliceExtent} covers many tiles, and expanding the specified extent is necessary
     *       for returning an integer number of tiles.</li>
     * </ul>
     *
     * Similar discussion applies to the {@linkplain RenderedImage#getMinY() minimum Y coordinate}.
     * The {@linkplain RenderedImage#getWidth() image width} and {@linkplain RenderedImage#getHeight() height} will be
     * the {@code sliceExtent} {@linkplain GridExtent#getSize(int) sizes} if this method can honor exactly the request,
     * or otherwise may be adjusted for the same reasons as <var>x</var> and <var>y</var> location discussed above.
     *
     * <p>Implementations should return a view as much as possible, without copying sample values.
     * {@code GridCoverage} subclasses can use the {@link ImageRenderer} class as a helper tool for that purpose.
     * This method does not mandate any behavior regarding tiling (size of tiles, their numbering system, <i>etc.</i>).
     * Some implementations may defer data loading until {@linkplain RenderedImage#getTile(int, int) a tile is requested}.</p>
     *
     * @param  sliceExtent  a subspace of this grid coverage where all dimensions except two have a size of 1 cell.
     *         May be {@code null} if this grid coverage has only two dimensions with a size greater than 1 cell.
     * @return the grid slice as a rendered image. Image location is relative to {@code sliceExtent}.
     * @throws MismatchedDimensionException if the given extent does not have the same number of dimensions as this coverage.
     * @throws SubspaceNotSpecifiedException if the given argument is not sufficient for reducing the grid to a two-dimensional slice.
     * @throws DisjointExtentException if the given extent does not intersect this grid coverage.
     * @throws CannotEvaluateException if this method cannot produce the rendered image for another reason.
     */
    public abstract RenderedImage render(GridExtent sliceExtent);

    /**
     * Returns a string representation of this grid coverage for debugging purpose.
     * The returned string is implementation dependent and may change in any future version.
     * Current implementation is equivalent to the following, where {@code <default flags>}
     * is the same set of flags than {@link GridGeometry#toString()}.
     *
     * {@snippet lang="java" :
     *     return toTree(Locale.getDefault(), <default flags>).toString();
     *     }
     *
     * @return a string representation of this grid coverage for debugging purpose.
     */
    @Override
    public String toString() {
        return toTree(Locale.getDefault(), gridGeometry.defaultFlags()).toString();
    }

    /**
     * Returns a tree representation of some elements of this grid coverage.
     * The tree representation is for debugging purpose only and may change
     * in any future SIS version.
     *
     * @param  locale   the locale to use for textual labels.
     * @param  bitmask  combination of {@link GridGeometry} flags.
     * @return a tree representation of the specified elements.
     *
     * @see GridGeometry#toTree(Locale, int)
     */
    @Debug
    public TreeTable toTree(final Locale locale, final int bitmask) {
        final Vocabulary vocabulary = Vocabulary.forLocale(Objects.requireNonNull(locale));
        final TableColumn<CharSequence> column = TableColumn.VALUE_AS_TEXT;
        final var tree = new DefaultTreeTable(column);
        final TreeTable.Node root = tree.getRoot();
        root.setValue(column, Classes.getShortClassName(this));
        TreeTable.Node branch = root.newChild();
        branch.setValue(column, vocabulary.getString(Vocabulary.Keys.CoverageDomain));
        gridGeometry.formatTo(locale, vocabulary, bitmask, branch);
        appendDataLayout(root, vocabulary, column);
        branch = root.newChild();
        branch.setValue(column, vocabulary.getString(Vocabulary.Keys.SampleDimensions));
        branch.newChild().setValue(column, SampleDimension.toString(locale, sampleDimensions));
        return tree;
    }

    /**
     * Appends a "data layout" branch (if it exists) to the tree representation of this coverage.
     * That branch will be inserted between "coverage domain" and "sample dimensions" branches.
     * The default implementation does nothing.
     *
     * @param  root        root of the tree where to add a branch.
     * @param  vocabulary  localized resources for vocabulary.
     * @param  column      the single column where to write texts.
     */
    @Debug
    void appendDataLayout(TreeTable.Node root, Vocabulary vocabulary, TableColumn<CharSequence> column) {
    }
}
