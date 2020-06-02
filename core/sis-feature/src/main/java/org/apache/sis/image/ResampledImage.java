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
package org.apache.sis.image;

import java.util.Set;
import java.util.Arrays;
import java.util.Objects;
import java.util.Collections;
import java.nio.DoubleBuffer;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.ImagingOpException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.internal.coverage.j2d.ImageLayout;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.internal.feature.Resources;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.geometry.Shapes2D;
import org.apache.sis.measure.NumberRange;


/**
 * An image which is the result of resampling the pixel values of another image.
 * Resampling is the action of computing pixel values at possibly non-integral positions of a source image.
 * It can be used for projecting an image to another coordinate reference system,
 * for example from (<var>latitude</var>, <var>longitude</var>) to World Mercator.
 * The resampling is defined by a non-linear {@link MathTransform} (for example a map projection)
 * which converts pixel center coordinates from <em>this</em> image to pixel center coordinates
 * in the <em>source</em> image.
 * The converted coordinates usually contain fraction digits, in which case an interpolation is applied.
 *
 * <h2>Usage note</h2>
 * This class should be used with non-linear transforms such as map projections. It is technically
 * possible to use this class with linear transforms such as {@link java.awt.geom.AffineTransform},
 * but there is more efficient alternatives for linear cases (for example
 * {@linkplain java.awt.Graphics2D#drawRenderedImage(RenderedImage, java.awt.geom.AffineTransform)
 * specifying the affine transform at rendering time}).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 *
 * @see Interpolation
 * @see java.awt.image.AffineTransformOp
 *
 * @since 1.1
 * @module
 */
public class ResampledImage extends ComputedImage {
    /**
     * The properties to forwards to source image in calls to {@link #getProperty(String)}.
     * This list may be augmented in any future Apache SIS version.
     *
     * @see #getProperty(String)
     */
    private static final Set<String> FILTERED_PROPERTIES = Collections.singleton(SAMPLE_RESOLUTIONS_KEY);

    /**
     * The {@value} value for identifying code expecting exactly 2 dimensions.
     */
    private static final int BIDIMENSIONAL = 2;

    /**
     * Domain of pixel coordinates in this image.
     */
    private final int minX, minY, width, height;

    /**
     * Conversion from pixel center coordinates of <em>this</em> image to pixel center coordinates of <em>source</em>
     * image. This transform should be an instance of {@link MathTransform2D}, but this is not required by this class
     * (a future version may allow interpolations in a <var>n</var>-dimensional cube).
     *
     * @see org.opengis.referencing.datum.PixelInCell#CELL_CENTER
     */
    protected final MathTransform toSource;

    /**
     * Same as {@link #toSource} but with the addition of a shift for taking in account the number of pixels required
     * for interpolations. For example if a bicubic interpolation needs 4×4 pixels, then the source coordinates that
     * we need are not the coordinates of the pixel we want to interpolate, but 1 or 2 pixels before for making room
     * for interpolation support.
     *
     * <p>This transform may be an instance of {@link ResamplingGrid} if the usage of such grid has been authorized.
     * That transform may be non-invertible. Consequently this transform should not be used for inverse operations
     * and should not be made accessible to the user.</p>
     *
     * @see #interpolationSupportOffset(int)
     */
    private final MathTransform toSourceSupport;

    /**
     * The object to use for performing interpolations.
     */
    protected final Interpolation interpolation;

    /**
     * The values to use if a pixel in this image can not be mapped to a pixel in the source image.
     * Must be an {@code int[]} or {@code double[]} array (no other type allowed). The array length
     * must be equal to the number of bands. Can not be null.
     */
    private final Object fillValues;

    /**
     * Creates a new image which will resample the given image. The resampling operation is defined
     * by a non-linear transform from <em>this</em> image to the specified <em>source</em> image.
     * That transform should map {@linkplain org.opengis.referencing.datum.PixelInCell#CELL_CENTER pixel centers}.
     *
     * <p>If a pixel in this image can not be mapped to a pixel in the source image, then the sample values are set
     * to {@code fillValues}. If the given array is {@code null}, or if any element in the given array is {@code null},
     * then the default fill value is NaN for floating point data types or zero for integer data types.</p>
     *
     * @param  bounds         domain of pixel coordinates of this resampled image.
     * @param  toSource       conversion of pixel coordinates of this image to pixel coordinates of {@code source} image.
     * @param  source         the image to be resampled.
     * @param  interpolation  the object to use for performing interpolations.
     * @param  accuracy       desired positional accuracy in pixel units, or 0 for the best accuracy available.
     *                        A value such as 0.125 pixel may enable the use of a slightly faster algorithm
     *                        at the expense of accuracy. This is only a hint honored on a <em>best-effort</em> basis.
     * @param  fillValues     the values to use for pixels in this image that can not be mapped to pixels in source image.
     *                        May be {@code null} or contain {@code null} elements. If shorter than the number of bands,
     *                        missing values are assumed {@code null}. If longer than the number of bands, extraneous
     *                        values are ignored.
     *
     * @see ImageProcessor#resample(Rectangle, MathTransform, RenderedImage)
     */
    protected ResampledImage(final Rectangle bounds, final MathTransform toSource, final RenderedImage source,
                             final Interpolation interpolation, final float accuracy, final Number[] fillValues)
    {
        super(ImageLayout.DEFAULT.createCompatibleSampleModel(source, bounds), source);
        if (source.getWidth() <= 0 || source.getHeight() <= 0) {
            throw new IllegalArgumentException(Resources.format(Resources.Keys.EmptyImage));
        }
        ArgumentChecks.ensureNonNull("interpolation", interpolation);
        ArgumentChecks.ensureStrictlyPositive("width",  width  = bounds.width);
        ArgumentChecks.ensureStrictlyPositive("height", height = bounds.height);
        minX = bounds.x;
        minY = bounds.y;
        /*
         * The transform from this image to source image must have exactly two coordinates in input
         * (otherwise we would not know what to put in extra coordinates), but may have more values
         * in output. The two first output coordinate values will be used for interpolation between
         * pixels of source image. Supplemental coordinates can be used for selecting an image in a
         * n-dimensional data cube.
         */
        this.toSource = toSource;
        int numDim = toSource.getSourceDimensions();
        if (numDim != BIDIMENSIONAL || (numDim = toSource.getTargetDimensions()) < BIDIMENSIONAL) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.MismatchedDimension_3, "toSource", BIDIMENSIONAL, numDim));
        }
        /*
         * If the interpolation requires more than 2×2 pixels, we will need to shift the transform
         * to source image. For example if the interpolation requires 4×4 pixels, the interpolation
         * point will be between the second and third pixel, so there is one more pixel on the left
         * to grab. We shift to the left because we need the coordinates of the first pixel.
         */
        this.interpolation = interpolation;
        final Dimension s = interpolation.getSupportSize();
        final double[] offset = new double[numDim];
        offset[0] = interpolationSupportOffset(s.width);
        offset[1] = interpolationSupportOffset(s.height);
        MathTransform toSourceSupport = MathTransforms.concatenate(toSource, MathTransforms.translation(offset));
        /*
         * If the desired accuracy is large enough, try using a grid of precomputed values for faster operations.
         * This is optional; it is okay to abandon the grid if we can not compute it.
         */
        if (accuracy >= ResamplingGrid.TOLERANCE) try {
            toSourceSupport = ResamplingGrid.getOrCreate(MathTransforms.bidimensional(toSourceSupport), bounds);
        } catch (TransformException | ImagingOpException e) {
            recoverableException("<init>", e);
        }
        this.toSourceSupport = toSourceSupport;
        /*
         * Copy the `fillValues` either as an `int[]` or `double[]` array, depending on
         * whether the data type is an integer type or not. Null elements default to zero.
         */
        final int numBands = ImageUtilities.getNumBands(source);
        final int dataType = ImageUtilities.getDataType(source);
        if (ImageUtilities.isIntegerType(dataType)) {
            final int[] fill = new int[numBands];
            if (fillValues != null) {
                for (int i=Math.min(fillValues.length, numBands); --i >= 0;) {
                    final Number f = fillValues[i];
                    if (f != null) fill[i] = f.intValue();
                }
            }
            this.fillValues = fill;
        } else {
            final double[] fill = new double[numBands];
            Arrays.fill(fill, Double.NaN);
            if (fillValues != null) {
                for (int i=Math.min(fillValues.length, numBands); --i >= 0;) {
                    final Number f = fillValues[i];
                    if (f != null) fill[i] = f.doubleValue();
                }
            }
            this.fillValues = fill;
        }
    }

    /**
     * The relative index of the first pixel needed on the left or top side of the region to use for interpolation.
     * The index is relative to the two central pixels needed for a bilinear interpolation. This value is negative
     * or zero. A value of 0 means that we need no pixels in addition of the two central pixels:
     *
     * <blockquote>
     * sample[0] … (position where to interpolate) … sample[1]
     * </blockquote>
     *
     * A value of -1 means that we need one more pixel on the left or top side.
     * It often means that we also need one more pixel on the right or bottom
     * sides, but not necessarily; those sides are not this method business.
     *
     * <blockquote>
     * sample[-1] … sample[0] … (position where to interpolate) … sample[1] … sample[2]
     * </blockquote>
     *
     * @param  span  the width or height of the support region for interpolations.
     * @return relative index of the first pixel needed on the left or top sides, as a value ≤ 0.
     *
     * @see #toSourceSupport
     */
    static double interpolationSupportOffset(final int span) {
        return -Math.max(0, (span - 1) / 2);        // Round toward 0.
    }

    /**
     * Verifies whether image layout information are consistent. This method performs all verifications
     * {@linkplain ComputedImage#verify() documented in parent class}, then verifies that source coordinates
     * required by this image (computed by converting {@linkplain #getBounds() this image bounds} using the
     * {@link #toSource} transform) intersects the bounds of the source image. If this is not the case, then
     * this method returns {@code "toSource"} for signaling that the transform may have a problem.
     *
     * @return {@code null} if image layout information are consistent,
     *         or the name of inconsistent attribute if a problem is found.
     */
    @Override
    public String verify() {
        String error = super.verify();
        if (error == null && toSource instanceof MathTransform2D) try {
            final Rectangle bounds = getBounds();
            final Rectangle2D tb = Shapes2D.transform((MathTransform2D) toSource, bounds, bounds);
            if (!ImageUtilities.getBounds(getSource()).intersects(tb)) {
                return "toSource";
            }
        } catch (TransformException e) {
            recoverableException("verify", e);
            return "toSource";
        }
        return error;
    }

    /**
     * Invoked when a non-fatal error occurred.
     *
     * @param  method  the method where the ignorable error occurred.
     * @param  error   the ignore which can be ignored.
     */
    private static void recoverableException(final String method, final Exception error) {
        Logging.recoverableException(Logging.getLogger(Modules.RASTER), ResampledImage.class, method, error);
    }

    /**
     * Returns the unique source of this resampled image.
     */
    private RenderedImage getSource() {
        return getSource(0);
    }

    /**
     * Returns the same color model than the source image.
     *
     * @return the color model, or {@code null} if unspecified.
     */
    @Override
    public ColorModel getColorModel() {
        return getSource().getColorModel();
    }

    /**
     * Gets a property from this image. Current default implementation forwards the following property requests
     * to the source image (more properties may be added to this list in future Apache SIS versions):
     *
     * <ul>
     *   <li>{@value #SAMPLE_RESOLUTIONS_KEY}</li>
     * </ul>
     *
     * Above listed properties are selected because they should have approximately the same values before and after
     * resampling. {@linkplain #STATISTICS_KEY Statistics} are not in this list because, while minimum and maximum
     * values should stay approximately the same, the average value and standard deviation may be quite different.
     */
    @Override
    public Object getProperty(final String key) {
        if (FILTERED_PROPERTIES.contains(key)) {
            return getSource().getProperty(key);
        } else {
            return super.getProperty(key);
        }
    }

    /**
     * Returns the names of all recognized properties, or {@code null} if this image has no properties.
     * The returned array contains the properties listed in {@link #getProperty(String)} if the source
     * image has those properties.
     *
     * @return names of all recognized properties, or {@code null} if none.
     */
    @Override
    public String[] getPropertyNames() {
        final String[] names = getSource().getPropertyNames();      // Array should be a copy, so we don't copy again.
        if (names != null) {
            int n = 0;
            for (final String name : names) {
                if (FILTERED_PROPERTIES.contains(name)) {
                    names[n++] = name;
                }
            }
            if (n != 0) {
                return ArraysExt.resize(names, n);
            }
        }
        return null;
    }

    /**
     * Returns the minimum <var>x</var> coordinate (inclusive) of this image.
     * This is the {@link Rectangle#x} value of the {@code bounds} specified at construction time.
     *
     * @return the minimum <var>x</var> coordinate (column) of this image.
     */
    @Override
    public final int getMinX() {
        return minX;
    }

    /**
     * Returns the minimum <var>y</var> coordinate (inclusive) of this image.
     * This is the {@link Rectangle#y} value of the {@code bounds} specified at construction time.
     *
     * @return the minimum <var>y</var> coordinate (row) of this image.
     */
    @Override
    public final int getMinY() {
        return minY;
    }

    /**
     * Returns the number of columns in this image.
     * This is the {@link Rectangle#width} value of the {@code bounds} specified at construction time.
     *
     * @return number of columns in this image.
     */
    @Override
    public final int getWidth() {
        return width;
    }

    /**
     * Returns the number of rows in this image.
     * This is the {@link Rectangle#height} value of the {@code bounds} specified at construction time.
     *
     * @return number of rows in this image.
     */
    @Override
    public final int getHeight() {
        return height;
    }

    /**
     * Invoked when a tile need to be computed or updated. This method fills all pixel values of the tile
     * with values interpolated from the source image. It may be invoked concurrently in different threads.
     *
     * @param  tileX  the column index of the tile to compute.
     * @param  tileY  the row index of the tile to compute.
     * @param  tile   if the tile already exists but needs to be updated, the tile to update. Otherwise {@code null}.
     * @return computed tile for the given indices.
     * @throws TransformException if an error occurred while computing pixel coordinates.
     */
    @Override
    @SuppressWarnings("SuspiciousSystemArraycopy")
    protected Raster computeTile(final int tileX, final int tileY, WritableRaster tile) throws TransformException {
        if (tile == null) {
            tile = createTile(tileX, tileY);
        }
        final int numBands = tile.getNumBands();
        final int scanline = tile.getWidth();
        final int tileMinX = tile.getMinX();
        final int tileMinY = tile.getMinY();
        final int tileMaxX = Math.addExact(tileMinX, scanline);
        final int tileMaxY = Math.addExact(tileMinY, tile.getHeight());
        final int tgtDim   = toSourceSupport.getTargetDimensions();
        final double[] coordinates = new double[scanline * Math.max(BIDIMENSIONAL, tgtDim)];
        /*
         * Compute the bounds of pixel coordinates that we can use for setting iterator positions in the source image.
         * The iterator bounds are slightly smaller than the image bounds because it needs to keep a margin for giving
         * enough pixels to interpolators (for example bilinear interpolations require 2×2 pixels).
         *
         * The (xmin, ymin) and (xmax, ymax) coordinates are integers and inclusive. Because integer pixel coordinates
         * are located at pixel centers, the image area is actually wider by 0.5 pixel (or 1.5, 2.5, …) on image sides.
         * This expansion is taken in account in (xlim, ylim), which are the limit values than we can interpolate.
         */
        final double xmin, ymin, xmax, ymax, xlim, ylim, xoff, yoff;
        final PixelIterator it;
        {   // For keeping temporary variables locale.
            final Dimension support = interpolation.getSupportSize();
            it = new PixelIterator.Builder().setWindowSize(support).create(getSource());
            final Rectangle domain = it.getDomain();    // Source image bounds.
            xmin = domain.getMinX();                    // We will tolerate 0.5 pixels before (from center to border).
            ymin = domain.getMinY();
            xmax = domain.getMaxX() - 1;                // Iterator limit (inclusive) because of interpolation support.
            ymax = domain.getMaxY() - 1;
            xlim = xmax + support.width  - 0.5;         // Limit of coordinates where we can interpolate.
            ylim = ymax + support.height - 0.5;
            xoff = interpolationSupportOffset(support.width)  - 0.5;    // Always negative.
            yoff = interpolationSupportOffset(support.height) - 0.5;
        }
        /*
         * Prepare a buffer where to store a line of interpolated values. We use this buffer for transferring
         * many pixels in a single `WritableRaster.setPixels(…)` call, which is faster than invoking `setPixel(…)`
         * for each pixel. We use integer values if possible because `WritableRaster.setPixels(…)` implementations
         * have optimizations for this case. If data are not integers, then we fallback on non-optimized `double[]`.
         */
        double[] transfer = null;
        int[] intTransfer = null;
        final double[] values;
        final int[] intValues;
        final Object valuesArray;
        final long[] minValues, maxValues;
        final boolean isInteger = (fillValues instanceof int[]);
        if (isInteger) {
            values      = new double[numBands];
            intValues   = new int[scanline * numBands];
            valuesArray = intValues;
            final NumberRange<?>[] ranges = it.getSampleRanges();   // Assumes source.sampleModel == this.sampleModel.
            minValues = new long[ranges.length];
            maxValues = new long[ranges.length];
            for (int i=0; i<ranges.length; i++) {
                final NumberRange<?> r = ranges[i];
                minValues[i] = r.getMinValue().longValue();
                maxValues[i] = r.getMaxValue().longValue();
            }
        } else {
            intValues   = null;
            values      = new double[scanline * numBands];
            valuesArray = values;
            minValues   = null;                         // Not used for floating point types.
            maxValues   = null;
        }
        /*
         * The (sx,sy) values are iterator position, remembered for detecting if the window buffer
         * needs to be updated. The `Integer.MAX_VALUE` initial value is safe because the iterator
         * can not have that position (its construction would have failed with ArithmeticException
         * if the image position was so high).
         */
        int sx = Integer.MAX_VALUE;
        int sy = Integer.MAX_VALUE;
        final PixelIterator.Window<DoubleBuffer> buffer = it.createWindow(TransferType.DOUBLE);
        for (int ty = tileMinY; ty < tileMaxY; ty++) {
            /*
             * Transform a block of coordinates in one `transform(…)` method call.
             * This is faster than doing a method call for each coordinates tuple.
             */
            for (int ci=0, tx=tileMinX; tx<tileMaxX; tx++) {
                coordinates[ci++] = tx;
                coordinates[ci++] = ty;
            }
            toSourceSupport.transform(coordinates, 0, coordinates, 0, scanline);
            /*
             * Special case for nearest-neighbor.
             */
            if (interpolation == Interpolation.NEAREST) {
                int ci = 0;     // Index in `coordinates` array.
                int vi = 0;     // Index in `values` or `intValues` array.
                for (int tx=tileMinX; tx<tileMaxX; tx++, ci+=tgtDim, vi+=numBands) {
                    final long x = Math.round(coordinates[ci]);
                    if (x >= it.lowerX && x < it.upperX) {
                        final long y = Math.round(coordinates[ci+1]);
                        if (y >= it.lowerY && y < it.upperY) {
                            it.moveTo((int) x, (int) y);
                            if (isInteger) {
                                intTransfer = it.getPixel(intTransfer);
                                System.arraycopy(intTransfer, 0, intValues, vi, numBands);
                            } else {
                                transfer = it.getPixel(transfer);
                                System.arraycopy(transfer, 0, values, vi, numBands);
                            }
                            continue;       // Values have been set, move to next pixel.
                        }
                    }
                    System.arraycopy(fillValues, 0, valuesArray, vi, numBands);
                }
            } else {
                /*
                 * Interpolate values for all bands in current scanline. The (x,y) values are coordinates
                 * in the source image and (xf,yf) are their fractional parts. Those fractional parts are
                 * between 0 inclusive and 1 exclusive except on the image borders: on the left and upper
                 * sides the fractional parts can go down to -0.5, because 0 is for pixel center and -0.5
                 * is at image border. On the right and bottom sides the fractional parts are constrained
                 * to +0.5 in nearest-neighbor interpolation case, for the same reason than other borders.
                 * However if the interpolation is bilinear, then the fractional parts on the bottom and
                 * right borders can go up to 1.5 because `PixelIterator` has reduced the (xmax, ymax)
                 * values by 1 (for taking in account the padding needed for interpolation support).
                 * This tolerance can be generalized (2.5, 3.5, etc.) depending on interpolation method.
                 */
                int ci = 0;     // Index in `coordinates` array.
                int vi = 0;     // Index in `values` or `intValues` array.
                for (int tx=tileMinX; tx<tileMaxX; tx++, ci+=tgtDim, vi+=numBands) {
                    double x = coordinates[ci];
                    if (x <= xlim) {
                        // Separate integer and fractional parts with 0 ≤ xf < 1 except on borders.
                        final double xf = x - (x = Math.max(xmin, Math.min(xmax, Math.floor(x))));
                        if (xf >= xoff) {                   // Negative only on left image border.
                            double y = coordinates[ci+1];
                            if (y <= ylim) {
                                // Separate integer and fractional parts with 0 ≤ yf < 1 except on borders.
                                final double yf = y - (y = Math.max(ymin, Math.min(ymax, Math.floor(y))));
                                if (yf >= yoff) {                   // Negative only on upper image border.
                                    /*
                                     * At this point we determined that (x,y) coordinates are inside source image domain.
                                     * Those coordinates may have been slightly shifted for interpolation support if they
                                     * were close to an image border. If the MathTransform produced 3 or more coordinates,
                                     * current implementation does not yet use those coordinates. But if we want to use
                                     * them in a future version (e.g. for interpolation in 3D cube), it would be there.
                                     */
                                    if (sx != (sx = (int) x)  |     // Really |, not ||.
                                        sy != (sy = (int) y))
                                    {
                                        it.moveTo(sx, sy);
                                        buffer.update();
                                    }
                                    /*
                                     * Interpolate the values at current position. We don't do any special processing
                                     * for NaN values because we want to keep them if output type is floating point,
                                     * and NaN values should not occur if data type (input and output) is integer.
                                     */
                                    if (interpolation.interpolate(buffer.values, numBands, xf, yf, values, isInteger ? 0 : vi)) {
                                        if (isInteger) {
                                            for (int b=0; b<numBands; b++) {
                                                intValues[vi+b] = (int) Math.max(minValues[b],
                                                                        Math.min(maxValues[b], Math.round(values[b])));
                                            }
                                        }
                                        continue;       // Values have been set, move to next pixel.
                                    }
                                }
                            }
                        }
                    }
                    /*
                     * If we reach this point then any of the "if" conditions above failed
                     * (i.e. the point to interpolate are outside the source image bounds)
                     * and no values have been set in the `values` or `intValues` array.
                     */
                    System.arraycopy(fillValues, 0, valuesArray, vi, numBands);
                }
            }
            /*
             * At this point we finished to compute the value of a scanline.
             * Copy to its final destination then move to next line.
             */
            if (isInteger) {
                tile.setPixels(tileMinX, ty, scanline, 1, intValues);
            } else {
                tile.setPixels(tileMinX, ty, scanline, 1, values);
            }
        }
        return tile;
    }

    /**
     * Compares the given object with this image for equality. This method returns {@code true}
     * if the given object is non-null, is an instance of the exact same class than this image,
     * has equal sources and do the same resampling operation (same interpolation method,
     * same fill values, same coordinates).
     *
     * @param  object  the object to compare with this image.
     * @return {@code true} if the given object is an image performing the same resampling than this image.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass().equals(getClass())) {
            final ResampledImage other = (ResampledImage) object;
            return minX   == other.minX &&
                   minY   == other.minY &&
                   width  == other.width &&
                   height == other.height &&
                   interpolation.equals(other.interpolation) &&
                   Objects.deepEquals(fillValues, other.fillValues) &&
                   toSource.equals(other.toSource) &&
                   getSources().equals(other.getSources());
        }
        return false;
    }

    /**
     * Returns a hash code value for this image.
     *
     * @return a hash code value based on a description of the operation performed by this image.
     */
    @Override
    public int hashCode() {
        return minX + 31*(minY + 31*(width + 31*height)) + interpolation.hashCode()
                + toSource.hashCode() + getSources().hashCode();
    }
}
