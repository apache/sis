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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Hashtable;
import static java.util.Objects.requireNonNull;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.image.privy.ColorScaleBuilder;
import org.apache.sis.image.privy.ObservableImage;
import org.apache.sis.image.privy.TiledImage;
import org.apache.sis.image.privy.WritableTiledImage;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * Helper class for the creation of {@link GridCoverage} instances.
 * A grid coverage is a function described by three parts:
 *
 * <ul>
 *   <li>A <dfn>domain</dfn>, which describes the input values (e.g. geographic coordinates).</li>
 *   <li>One or more <dfn>ranges</dfn>, which describe the output values that the coverage can produce.</li>
 *   <li>The actual values, distributed on a regular grid.</li>
 * </ul>
 *
 * Each of those parts can be set by a {@code setDomain(…)}, {@code setRanges(…)} or {@code setValues(…)} method.
 * Those methods are overloaded with many variants accepting different kind of arguments. For example, values can
 * be specified as a {@link RenderedImage}, a {@link Raster} or some other types.
 *
 * <h2>Example</h2>
 * The easiest way to create a {@link GridCoverage} from a matrix of values is to set the values in a
 * {@link WritableRaster} and to specify the domain as an {@link Envelope}:
 *
 * {@snippet lang="java" :
 *     public GridCoverage createCoverage() {
 *         WritableRaster data = Raster.createBandedRaster(DataBuffer.TYPE_USHORT, width, height, numBands, null);
 *         for (int y=0; y<height; y++) {
 *             for (int x=0; x<width; x++) {
 *                 int value = ...;                     // Compute a value here.
 *                 data.setSample(x, y, 0, value);      // Set value in the first band.
 *             }
 *         }
 *         var builder = new GridCoverageBuilder();
 *         builder.setValues(data).flixAxis(1);
 *
 *         Envelope domain = ...;                       // Specify here the "real world" coordinates.
 *         return builder.setDomain(domain).build();
 *     }
 * }
 *
 * <h2>Limitations</h2>
 * Current implementation creates only two-dimensional coverages.
 * A future version may extend this builder API for creating <var>n</var>-dimensional coverages.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @see GridCoverage2D
 * @see SampleDimension.Builder
 *
 * @since 1.1
 */
public class GridCoverageBuilder {
    /**
     * The domain (input) of the coverage function, or {@code null} if unspecified.
     * If {@code null}, an identify "grid to CRS" transform will be assumed.
     *
     * @see #setDomain(GridGeometry)
     * @see #setDomain(Envelope)
     */
    private GridGeometry domain;

    /**
     * The range (output) of the coverage function, or {@code null} if unspecified.
     * If non-null, then the size of this list must be equal to the number of bands.
     *
     * @see #setRanges(Collection)
     * @see #setRanges(SampleDimension...)
     * @see #addRange(SampleDimension)
     */
    private List<SampleDimension> ranges;

    /**
     * The band to be made visible (usually 0). All other bands, if any will be hidden.
     * This is used only for data without color model as {@link #raster} and {@link #buffer}.
     *
     * @todo There is not yet a setter method for this property.
     */
    private int visibleBand;

    /**
     * The raster containing the coverage values.
     * Exactly one of {@code image}, {@link #raster} and {@link #buffer} shall be non-null.
     *
     * @see #setValues(RenderedImage)
     */
    private RenderedImage image;

    /**
     * The raster containing the coverage values.
     * May be a {@link WritableRaster}, in which case a {@link BufferedImage} may be created.
     * Exactly one of {@link #image}, {@code raster} and {@link #buffer} shall be non-null.
     *
     * @see #setValues(Raster)
     */
    private Raster raster;

    /**
     * The data buffer containing the coverage values.
     * Exactly one of {@link #image}, {@link #raster} and {@code buffer} shall be non-null.
     *
     * @see #setValues(DataBuffer, Dimension)
     */
    private DataBuffer buffer;

    /**
     * The image size, or {@code null} if unspecified. It needs to be specified only
     * if values were specified as a buffer without information about the grid size.
     *
     * @see #setValues(DataBuffer, Dimension)
     */
    private Dimension size;

    /**
     * Set of grid axes to reverse, as a bit mask. For any dimension <var>i</var>, the bit
     * at {@code 1L << i} is set to 1 if the grid axis at that dimension should be flipped.
     *
     * @see #flipGridAxis(int)
     */
    private long flippedAxes;

    /**
     * The properties to give to the image, or {@code null} if none.
     *
     * @see #addImageProperty(String, Object)
     */
    @SuppressWarnings("UseOfObsoleteCollectionType")
    private final Hashtable<String,Object> properties;

    /**
     * Creates an initially empty builder.
     */
    public GridCoverageBuilder() {
        properties = new Hashtable<>();
    }

    /**
     * Sets the domain envelope (including its CRS) and/or the transform from grid indices to domain coordinates.
     * The given {@code GridGeometry} does not need to contain a {@link GridExtent} because that extent will be
     * computed automatically if needed. However if an extent is present, then it must be consistent with the
     * size of data given to {@code setValues(…)} method (will be verified at {@link #build()} time).
     *
     * @param  grid  the new grid geometry, or {@code null} for removing previous domain setting.
     * @return {@code this} for method invocation chaining.
     */
    public GridCoverageBuilder setDomain(final GridGeometry grid) {
        domain = (grid != GridGeometry.UNDEFINED) ? grid : null;
        return this;
    }

    /**
     * Sets the domain as an enclosing envelope (including its CRS).
     * The given envelope should contain all pixel area. For example, the
     * {@linkplain Envelope#getLowerCorner() envelope lower corner} should locate the lower-left
     * (or upper-left, depending on <var>y</var> axis orientation) pixel corner, not pixel center.
     * If the given envelope contains a CRS, then that CRS will be the coverage CRS.
     * A transform from grid indices to domain coordinates will be created automatically.
     * That transform will map grid dimensions to envelope dimensions in the same order
     * (i.e. the matrix representation of the affine transform will be diagonal,
     * ignoring the translation column).
     *
     * <h4>Axis directions</h4>
     * By default grid indices increase in the same direction as domain coordinates.
     * When applied to images with pixels located by (<var>column</var>, <var>row</var>) indices,
     * it means that by default row indices in the image are increasing toward up if the <var>y</var>
     * coordinates in the coverage domain (e.g. latitude values) are also increasing toward up.
     * It often results in images flipped vertically, because popular image formats such as PNG
     * use row indices increasing in the opposite direction (toward down).
     * This effect can be compensated by invoking <code>{@linkplain #flipGridAxis(int) flipGridAxis}(1)</code>.
     *
     * <p>{@code GridCoverageBuilder} provides method only for flipping axes.
     * If more sophisticated operations is desired (for example a rotation),
     * then {@link #setDomain(GridGeometry)} should be used instead of this method.</p>
     *
     * <h5>Design note</h5>
     * {@code GridCoverageBuilder} does not flip the <var>y</var> axis by default because not all
     * file formats have row indices increasing toward down. A counter-example is the netCDF format.
     * Even if we consider that the majority of images have <var>y</var> axis flipped, things become
     * less obvious when considering data in more than two dimensions. Having the same default policy
     * (no flipping) for all dimensions make problem analysis easier.
     *
     * <h4>Default implementation</h4>
     * The default implementation creates a new {@link GridGeometry} from the given envelope
     * then invokes {@link #setDomain(GridGeometry)}. Subclasses can override that later method
     * as a single overriding point for all domain settings.
     *
     * @param  domain  envelope of the coverage domain together with its CRS,
     *                 or {@code null} for removing previous domain setting.
     * @return {@code this} for method invocation chaining.
     *
     * @see #flipGridAxis(int)
     * @see GridGeometry#GridGeometry(GridExtent, Envelope, GridOrientation)
     */
    public GridCoverageBuilder setDomain(final Envelope domain) {
        return setDomain(domain == null ? null : new GridGeometry(domain));
    }

    /**
     * Sets the sample dimensions for all bands.
     * The list size must be equal to the number of bands in the data specified to
     * {@code setValues(…)} method (it will be verified at {@link #build()} time).
     *
     * @param  bands  the new sample dimensions, or {@code null} for removing previous range setting.
     * @return {@code this} for method invocation chaining.
     * @throws IllegalArgumentException if the given list is empty.
     *
     * @see SampleDimension.Builder
     */
    public GridCoverageBuilder setRanges(final Collection<? extends SampleDimension> bands) {
        if (bands == null) {
            ranges = null;
        } else {
            ArgumentChecks.ensureNonEmpty("bands", bands);
            if (ranges instanceof ArrayList<?>) {
                ranges.clear();
                ranges.addAll(bands);
            } else {
                ranges = new ArrayList<>(bands);
            }
        }
        return this;
    }

    /**
     * Sets the sample dimensions for all bands.
     * The array length must be equal to the number of bands in the data specified to
     * {@code setValues(…)} method (it will be verified at {@link #build()} time).
     *
     * @param  bands  the new sample dimensions, or {@code null} for removing previous range setting.
     * @return {@code this} for method invocation chaining.
     * @throws IllegalArgumentException if the given array is empty.
     *
     * @see SampleDimension.Builder
     */
    public GridCoverageBuilder setRanges(final SampleDimension... bands) {
        if (bands == null) {
            ranges = null;
        } else {
            ArgumentChecks.ensureNonEmpty("bands", bands);
            ranges = Arrays.asList(bands);
        }
        return this;
    }

    /**
     * Adds a sample dimension for one band. This method can be invoked repeatedly until the number of
     * sample dimensions is equal to the number of bands in the data specified to {@code setValues(…)}.
     *
     * @param  band  the sample dimension to add.
     * @return {@code this} for method invocation chaining.
     *
     * @see SampleDimension.Builder
     */
    public GridCoverageBuilder addRange(final SampleDimension band) {
        requireNonNull(band);
        if (!(ranges instanceof ArrayList<?>)) {
            ranges = (ranges != null) ? new ArrayList<>(ranges) : new ArrayList<>();
        }
        ranges.add(band);
        return this;
    }

    /**
     * Sets a two-dimensional slice of sample values as a rendered image.
     * If {@linkplain #setRanges(SampleDimension...) sample dimensions are specified},
     * then the {@linkplain java.awt.image.SampleModel#getNumBands() number of bands}
     * must be equal to the number of sample dimensions.
     *
     * <p><b>Note:</b> row indices in an image are usually increasing down, while geographic coordinates
     * are usually increasing up. Consequently, the <code>{@linkplain #flipGridAxis(int) flipGridAxis}(1)</code>
     * method may need to be invoked after this method.</p>
     *
     * @param  data  the rendered image to be wrapped in a {@code GridCoverage}. Cannot be {@code null}.
     * @return {@code this} for method invocation chaining.
     *
     * @see BufferedImage
     */
    public GridCoverageBuilder setValues(final RenderedImage data) {
        image  = requireNonNull(data);
        raster = null;
        buffer = null;
        size   = null;
        return this;
    }

    /**
     * Sets a two-dimensional slice of sample values as a raster.
     * If {@linkplain #setRanges(SampleDimension...) sample dimensions are specified},
     * then the {@linkplain Raster#getNumBands() number of bands} must be equal to the
     * number of sample dimensions.
     *
     * <p><b>Note:</b> row indices in a raster are usually increasing down, while geographic coordinates
     * are usually increasing up. Consequently, the <code>{@linkplain #flipGridAxis(int) flipGridAxis}(1)</code>
     * method may need to be invoked after this method.</p>
     *
     * @param  data  the raster to be wrapped in a {@code GridCoverage}. Cannot be {@code null}.
     * @return {@code this} for method invocation chaining.
     *
     * @see Raster#createBandedRaster(int, int, int, int, Point)
     */
    public GridCoverageBuilder setValues(final Raster data) {
        raster = requireNonNull(data);
        image  = null;
        buffer = null;
        size   = null;
        return this;
    }

    /**
     * Sets a two-dimensional slice of sample values as a Java2D data buffer.
     * The {@linkplain DataBuffer#getNumBanks() number of banks} will be the number of bands in the image.
     * If {@linkplain #setRanges(SampleDimension...) sample dimensions are specified}, then the number of
     * bands must be equal to the number of sample dimensions.
     *
     * @param  data  the data buffer to be wrapped in a {@code GridCoverage}. Cannot be {@code null}.
     * @param  size  the image size in pixels, or {@code null} if unspecified. If null, then the image
     *               size will be taken from the {@linkplain GridGeometry#getExtent() grid extent}.
     * @return {@code this} for method invocation chaining.
     * @throws IllegalArgumentException if {@code width} or {@code height} is negative or equals to zero.
     */
    public GridCoverageBuilder setValues(final DataBuffer data, Dimension size) {
        ArgumentChecks.ensureNonNull("data", data);
        if (size != null) {
            size = new Dimension(size);
            ArgumentChecks.ensureStrictlyPositive("width",  size.width);
            ArgumentChecks.ensureStrictlyPositive("height", size.height);
            final int length = Math.multiplyExact(size.width, size.height);
            final int capacity = data.getSize();
            if (length > capacity) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.UnexpectedArrayLength_2, length, capacity));
            }
        }
        this.size = size;
        buffer = data;
        image  = null;
        raster = null;
        return this;
    }

    /**
     * Reverses axis direction in the specified grid dimension.
     * For example if grid indices are (<var>column</var>, <var>row</var>),
     * then {@code flipGridAxis(1)} will reverse the direction of rows axis.
     * Invoking this method a second time for the same dimension will cancel the flipping.
     *
     * <p>When building coverage with a {@linkplain #setDomain(Envelope) domain specified by an envelope}
     * (i.e. with no explicit <i>grid to CRS</i> transform), the default {@code GridCoverageBuilder}
     * behavior is to create a {@link GridGeometry} with grid indices increasing in the same direction as
     * domain coordinates. This method allows to reverse direction for an axis.
     * The most typical usage is to reverse the direction of the <var>y</var> axis in images.</p>
     *
     * @param  dimension  index of the dimension in the grid to reverse direction.
     * @return {@code this} for method invocation chaining.
     *
     * @see #setDomain(Envelope)
     * @see GridOrientation#flipGridAxis(int)
     */
    public GridCoverageBuilder flipGridAxis(final int dimension) {
        ArgumentChecks.ensurePositive("dimension", dimension);
        if (dimension >= Long.SIZE) {
            throw new ArithmeticException(Errors.format(Errors.Keys.ExcessiveNumberOfDimensions_1, dimension + 1));
        }
        flippedAxes ^= (1L << dimension);
        return this;
    }

    /**
     * Adds a value associated to an image property. This method can be invoked only once for each {@code key}.
     * Those properties will be given to the {@link RenderedImage} created by the {@link #build()} method.
     *
     * @param  key    key of the property to set.
     * @param  value  value to associate to the given key.
     * @return {@code this} for method invocation chaining.
     * @throws IllegalArgumentException if a value is already associated to the given key.
     *
     * @since 1.1
     */
    @SuppressWarnings("UseOfObsoleteCollectionType")
    public GridCoverageBuilder addImageProperty(final String key, final Object value) {
        if (properties.putIfAbsent(requireNonNull(key, "key"), requireNonNull(value, "value")) != null) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.ElementAlreadyPresent_1, key));
        }
        return this;
    }

    /**
     * Creates the grid coverage from the domain, ranges and values given to setter methods.
     * The returned coverage is often a {@link GridCoverage2D} instance, but not necessarily.
     *
     * @return grid coverage created from specified domain, ranges and sample values.
     * @throws IllegalStateException if some properties are inconsistent, for example
     *         {@linkplain GridGeometry#getExtent() grid extent} not matching image size or
     *         {@linkplain #setRanges(SampleDimension...) number of sample dimensions} not matching
     *         the number of bands. This exception often wraps an {@link IllegalGridGeometryException},
     *         {@link IllegalArgumentException} or {@link NullPointerException}.
     */
    public GridCoverage build() throws IllegalStateException {
        GridGeometry grid = domain;                                 // May be replaced by an instance with extent.
        List<? extends SampleDimension> bands = ranges;             // May be replaced by a non-null value.
        /*
         * If not already done, create the image from the raster. We try to create the most standard objects
         * when possible: a BufferedImage (from Java2D), then later a GridCoverage2D (from SIS public API).
         * An exception to this rule is the DataBuffer case: we use a dedicated BufferedGridCoverage class
         * instead.
         */
        try {
            if (image == null) {
                if (raster == null) {
                    if (buffer == null) {
                        throw new IllegalStateException(missingProperty("values"));
                    }
                    if (size != null) {
                        grid = GridCoverage2D.addExtentIfAbsent(grid, new Rectangle(size));
                        verifyGridExtent(grid.getExtent(), size.width, size.height);
                    } else if (grid == null) {
                        throw new IncompleteGridGeometryException(missingProperty("size"));
                    }
                    bands = GridCoverage2D.defaultIfAbsent(bands, null, buffer.getNumBanks());
                    return new BufferedGridCoverage(domainWithAxisFlips(grid), bands, buffer);
                }
                /*
                 * If the band list is null, create a default list of bands because we need
                 * them for creating the color model. Note that we shall not do that when a
                 * RenderedImage has been specified because GridCoverage2D constructor will
                 * will infer better names.
                 */
                bands = GridCoverage2D.defaultIfAbsent(bands, null, raster.getNumBands());
                final SampleModel sm = raster.getSampleModel();
                final var colorizer = new ColorScaleBuilder(ColorScaleBuilder.GRAYSCALE, null, false);
                final ColorModel colors;
                if (colorizer.initialize(sm, bands.get(visibleBand)) || colorizer.initialize(sm, visibleBand)) {
                    colors = colorizer.createColorModel(sm, bands.size(), visibleBand);
                } else {
                    colors = ColorScaleBuilder.NULL_COLOR_MODEL;
                }
                /*
                 * Create an image from the raster. We favor BufferedImage instance when possible,
                 * and fallback on TiledImage only if the BufferedImage cannot be created.
                 */
                if (bands != null) {
                    properties.put(PlanarImage.SAMPLE_DIMENSIONS_KEY, bands.toArray(SampleDimension[]::new));
                } else {
                    properties.remove(PlanarImage.SAMPLE_DIMENSIONS_KEY);
                }
                if (raster instanceof WritableRaster) {
                    final WritableRaster wr = (WritableRaster) raster;
                    if (colors != null && (wr.getMinX() | wr.getMinY()) == 0) {
                        image = new ObservableImage(colors, wr, false, properties);
                    } else {
                        image = new WritableTiledImage(properties, colors, wr.getWidth(), wr.getHeight(), 0, 0, wr);
                    }
                } else {
                    image = new TiledImage(properties, colors, raster.getWidth(), raster.getHeight(), 0, 0, raster);
                }
                properties.remove(PlanarImage.SAMPLE_DIMENSIONS_KEY);
            }
            /*
             * At this point `image` shall be non-null but `bands` may still be null (it is okay).
             */
            return new GridCoverage2D(domainWithAxisFlips(grid), bands, image);
        } catch (TransformException | NullPointerException | IllegalArgumentException | ArithmeticException e) {
            throw new IllegalStateException(Resources.format(Resources.Keys.CanNotBuildGridCoverage), e);
        }
    }

    /**
     * Returns the {@linkplain #domain} with axis flips applied. If there is no axis to flip,
     * {@link #domain} is returned unchanged (without completion for missing extent; we leave
     * that to {@link GridCoverage2D} constructor).
     *
     * @see GridCoverage2D#addExtentIfAbsent(GridGeometry, Rectangle)
     */
    private GridGeometry domainWithAxisFlips(GridGeometry grid) throws TransformException {
        long f = flippedAxes;
        if (f != 0) {
            grid = GridCoverage2D.addExtentIfAbsent(grid, image);
            if (grid != null && grid.isDefined(GridGeometry.EXTENT)) {
                final GridExtent extent = grid.getExtent();
                final int srcDim = extent.getDimension();
                final MatrixSIS flip = Matrices.createDiagonal(grid.getTargetDimension() + 1, srcDim + 1);
                do {
                    final int j = Long.numberOfTrailingZeros(f);
                    flip.setElement(j, j, -1);
                    flip.setElement(j, srcDim, extent.getSize(j, false));
                    f &= ~(1L << j);
                } while (f != 0);
                grid = new GridGeometry(grid, extent, MathTransforms.linear(flip));
            }
        }
        return grid;
    }

    /**
     * Verifies that the grid extent has the expected size. This method does not verify grid location
     * (low coordinates) because it is okay to have it anywhere. The {@code expectedSize} array can be
     * shorter than the number of dimensions (i.e. it may be a slice in a data cube); this method uses
     * {@link GridExtent#getSubspaceDimensions(int)} for determining which dimensions to check.
     *
     * <p>This verification can be useful because {@link DataBuffer} does not contain any information
     * about image size, so {@link BufferedGridCoverage#render(GridExtent)} will rely on the size
     * provided by the grid extent. If those information do not reflect accurately the image size,
     * the image will not be rendered properly.</p>
     *
     * @param  extent        the extent to verify.
     * @param  expectedSize  the expected image size.
     * @throws IllegalGridGeometryException if the extent does not have the expected size.
     */
    private static void verifyGridExtent(final GridExtent extent, final int... expectedSize) {
        final int[] imageAxes = extent.getSubspaceDimensions(expectedSize.length);
        for (int i=0; i<expectedSize.length; i++) {
            final int imageSize = expectedSize[i];
            final long gridSize = extent.getSize(imageAxes[i]);
            if (imageSize != gridSize) {
                throw new IllegalGridGeometryException(Resources.format(
                        Resources.Keys.MismatchedImageSize_3, i, imageSize, gridSize));
            }
        }
    }

    /**
     * Returns an error message for the exception to throw when a mandatory property is missing.
     *
     * @param  name  name of the missing property.
     * @return message for the exception to throw.
     */
    private static String missingProperty(final String name) {
        return Errors.format(Errors.Keys.MissingValueForProperty_1, name);
    }
}
