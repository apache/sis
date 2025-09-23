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

import java.util.Objects;
import java.lang.ref.Reference;
import java.nio.DoubleBuffer;
import java.awt.Shape;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.ImagingOpException;
import java.awt.image.SampleModel;
import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.image.internal.shared.ImageUtilities;
import org.apache.sis.image.internal.shared.FillValues;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Disposable;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.geometry.Shapes2D;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import static org.apache.sis.image.internal.shared.ImageUtilities.LOGGER;


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
 * @version 1.5
 *
 * @see Interpolation
 * @see java.awt.image.AffineTransformOp
 *
 * @since 1.1
 */
public class ResampledImage extends ComputedImage {
    /**
     * Key of a property providing an estimation of positional error for each pixel.
     * Values shall be instances of {@link RenderedImage} with same size and origin than this image.
     * The image should contain a single band where all sample values are error estimations in pixel units
     * (relative to pixels of this image). The value should be small, for example between 0 and 0.2.
     *
     * <p>The default implementation transforms all pixel coordinates {@linkplain #toSource to source},
     * then convert them back to pixel coordinates in this image. The result is compared with expected
     * coordinates and the distance is stored in the image.</p>
     *
     * @see #POSITIONAL_ACCURACY_KEY
     */
    public static final String POSITIONAL_CONSISTENCY_KEY = "org.apache.sis.PositionalConsistency";

    /**
     * The {@value} value for identifying code expecting exactly 2 dimensions.
     */
    static final int BIDIMENSIONAL = 2;

    /**
     * Domain of pixel coordinates in this image.
     */
    private final int minX, minY, width, height;

    /**
     * Index of the first tile.
     */
    private final int minTileX, minTileY;

    /**
     * Conversion from pixel center coordinates of <em>this</em> image to pixel center coordinates of <em>source</em>
     * image. This transform should be an instance of {@link MathTransform2D}, but this is not required by this class
     * (a future version may allow interpolations in a <var>n</var>-dimensional cube).
     *
     * @see org.apache.sis.coverage.grid.PixelInCell#CELL_CENTER
     */
    protected final MathTransform toSource;

    /**
     * Same as {@link #toSource} but with the addition of a shift for taking in account the number of pixels required
     * for interpolations. For example if a bicubic interpolation needs 4×4 pixels, then the source coordinates that
     * we need are not the coordinates of the pixel we want to interpolate, but 1 or 2 pixels before for making room
     * for interpolation support.
     *
     * <p>This transform may be an instance of {@link ResamplingGrid} if the usage of such grid has been authorized.
     * That transform may be non-invertible. Consequently, this transform should not be used for inverse operations
     * and should not be made accessible to the user.</p>
     *
     * <p>This transform maps pixel centers of both images, except in the case of nearest-neighbor interpolation.
     * In that special case only, the transform maps target pixel <em>center</em> to source pixel <em>corner</em>.
     * We have to map corners in source images because source pixel coordinates are computed by taking the integer
     * parts of {@code toSourceSupport} results, without rounding.</p>
     *
     * @see #interpolationSupportOffset(int)
     * @see Interpolation#interpolate(DoubleBuffer, int, double, double, double[], int)
     */
    private final MathTransform toSourceSupport;

    /**
     * The object to use for performing interpolations.
     */
    protected final Interpolation interpolation;

    /**
     * The values to use if a pixel in this image cannot be mapped to a pixel in the source image.
     * Must be an {@code int[]} or {@code double[]} array (no other type allowed). The array length
     * must be equal to the number of bands. Cannot be null.
     */
    private final Object fillValues;

    /**
     * The largest accuracy declared in the {@code accuracy} argument given to constructor,
     * or {@code null} if none. This is for information purpose only.
     *
     * @see #getPositionalAccuracy()
     */
    private final Quantity<Length> linearAccuracy;

    /**
     * {@link #POSITIONAL_CONSISTENCY_KEY} value, computed when first requested.
     *
     * @see #getPositionalConsistency()
     * @see #getProperty(String)
     */
    private Reference<ComputedImage> positionalConsistency;

    /**
     * {@link #MASK_KEY} value, computed when first requested.
     *
     * @see #getMask()
     * @see #getProperty(String)
     */
    private Reference<ComputedImage> mask;

    /**
     * The valid area, computed when first requested.
     *
     * @see #getValidArea()
     */
    private Shape validArea;

    /**
     * Creates a new image which will resample the given image. The resampling operation is defined
     * by a potentially non-linear transform from <em>this</em> image to the specified <em>source</em> image.
     * That transform should map {@linkplain org.apache.sis.coverage.grid.PixelInCell#CELL_CENTER pixel centers}.
     *
     * <p>The {@code sampleModel} determines the tile size and the target data type. This is often the same sample
     * model than the one used by the {@code source} image, but may also be different for forcing a different tile
     * size or a different data type (e.g. {@code byte} versus {@code float}) for storing resampled values.
     * If the specified sample model is not the same as the one used by the source image,
     * then subclass should override {@link #getColorModel()} for returning a color model which is
     * {@linkplain ColorModel#isCompatibleSampleModel(SampleModel) compatible with the sample model}.</p>
     *
     * <p>If a pixel in this image cannot be mapped to a pixel in the source image, then the sample values are set
     * to {@code fillValues}. If the given array is {@code null}, or if any element in the given array is {@code null},
     * then the default fill value is NaN for floating point data types or zero for integer data types.
     * If the array is shorter than the number of bands, then above-cited default values are used for missing values.
     * If longer than the number of bands, extraneous values are ignored.</p>
     *
     * @param  source         the image to be resampled.
     * @param  sampleModel    the sample model shared by all tiles in this resampled image.
     * @param  minTile        indices of the first tile ({@code minTileX}, {@code minTileY}), or {@code null} for (0,0).
     * @param  bounds         domain of pixel coordinates of this resampled image.
     * @param  toSource       conversion of pixel coordinates of this image to pixel coordinates of {@code source} image.
     * @param  interpolation  the object to use for performing interpolations.
     * @param  fillValues     the values to use for pixels in this image that cannot be mapped to pixels in source image.
     *                        May be {@code null} or contain {@code null} elements, and may have any length
     *                        (see above for more details).
     * @param  accuracy       values of {@value #POSITIONAL_ACCURACY_KEY} property, or {@code null} if none.
     *                        This constructor may retain only a subset of specified values or replace some of them.
     *                        If an accuracy is specified in {@linkplain Units#PIXEL pixel units}, then a value such as
     *                        0.125 pixel may enable the use of a slightly faster algorithm at the expense of accuracy.
     *                        This is only a hint honored on a <em>best-effort</em> basis.
     *
     * @see ImageProcessor#resample(RenderedImage, Rectangle, MathTransform)
     */
    protected ResampledImage(final RenderedImage source, final SampleModel sampleModel, final Point minTile,
            final Rectangle bounds, final MathTransform toSource, Interpolation interpolation,
            final Number[] fillValues, final Quantity<?>[] accuracy)
    {
        super(sampleModel, source);
        if ((source.getWidth() | source.getHeight()) <= 0) {
            throw new IllegalArgumentException(Resources.format(Resources.Keys.EmptyImage));
        }
        ArgumentChecks.ensureNonNull("interpolation", interpolation);
        ArgumentChecks.ensureStrictlyPositive("width",  width  = bounds.width);
        ArgumentChecks.ensureStrictlyPositive("height", height = bounds.height);
        minX = bounds.x;
        minY = bounds.y;
        if (minTile != null) {
            minTileX = minTile.x;
            minTileY = minTile.y;
        } else {
            minTileX = 0;
            minTileY = 0;
        }
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
         * If the image uses an index color model, interpolating the indexed values does not produce
         * the expected colors. Safest approach is to disable completely interpolations in that case.
         */
        interpolation = interpolation.toCompatible(source);
        /*
         * If the interpolation requires more than 2×2 pixels, we will need to shift the transform
         * to source image. For example if the interpolation requires 4×4 pixels, the interpolation
         * point will be between the second and third pixel, so there is one more pixel on the left
         * to grab. We shift to the left because we need the coordinates of the first pixel.
         */
        Dimension s = interpolation.getSupportSize();
        if (s.width > source.getWidth() || s.height > source.getHeight()) {
            interpolation = Interpolation.NEAREST;
            s = interpolation.getSupportSize();
        }
        this.interpolation = interpolation;
        final double[] offset = new double[numDim];
        offset[0] = interpolationSupportOffset(s.width);
        offset[1] = interpolationSupportOffset(s.height);

        @SuppressWarnings("LocalVariableHidesMemberVariable")
        MathTransform toSourceSupport = MathTransforms.concatenate(toSource, MathTransforms.translation(offset));
        /*
         * If the desired accuracy is large enough, try using a grid of precomputed values for faster operations.
         * This is optional; it is okay to abandon the grid if we cannot compute it.
         */
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        Quantity<Length> linearAccuracy = null;
        Boolean          canUseGrid     = null;
        if (accuracy != null) {
            for (final Quantity<?> hint : accuracy) {
                if (hint != null) {
                    final Unit<?> unit = hint.getUnit();
                    if (Units.PIXEL.equals(unit)) {
                        final boolean c = Math.abs(hint.getValue().doubleValue()) >= ResamplingGrid.TOLERANCE;
                        if (canUseGrid == null) canUseGrid = c;
                        else canUseGrid &= c;
                    } else if (Units.isLinear(unit)) {
                        linearAccuracy = Quantities.max(linearAccuracy, hint.asType(Length.class));
                    }
                }
            }
        }
        if (canUseGrid != null && canUseGrid) try {
            toSourceSupport = ResamplingGrid.getOrCreate(MathTransforms.bidimensional(toSourceSupport), bounds);
        } catch (TransformException | ImagingOpException e) {
            recoverableException("<init>", e);
        }
        this.toSourceSupport = toSourceSupport;
        this.linearAccuracy  = linearAccuracy;
        this.fillValues      = new FillValues(sampleModel, fillValues, false).asPrimitiveArray;
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
     * <h4>Nearest-neighbor special case</h4>
     * The nearest-neighbor interpolation (identified by {@code span == 1}) is handled in a special way.
     * The return value should be 0 according above contract, but this method returns 0.5 instead.
     * This addition of a 0.5 offset allows the following substitution:
     *
     * {@snippet lang="java" :
     *     Math.round(x) ≈ (long) Math.floor(x + 0.5)
     *     }
     *
     * {@link Math#round(double)} is the desired behavior for nearest-neighbor interpolation, but the buffer given
     * to {@link Interpolation#interpolate(DoubleBuffer, int, double, double, double[], int)} is filled with values
     * at coordinates determined by {@link Math#floor(double)} semantic. Because the buffer has only one value,
     * {@code interpolate(…)} has no way to look at neighbor values for the best match (contrarily to what other
     * interpolation implicitly do, through mathematics). The 0.5 offset is necessary for compensating.
     *
     * @param  span  the width or height of the support region for interpolations.
     * @return relative index of the first pixel needed on the left or top sides,
     *         as a value ≤ 0 (except in nearest-neighbor special case).
     *
     * @see #toSourceSupport
     * @see Interpolation#interpolate(DoubleBuffer, int, double, double, double[], int)
     */
    static double interpolationSupportOffset(final int span) {
        if (span <= 1) return 0.5;                  // Nearest-neighbor (special case).
        return -((span - 1) / 2);                   // Round toward 0.
    }

    /**
     * Returns the upper limit (inclusive) where an interpolation is possible. The given {@code max} value is
     * the maximal coordinate value (inclusive) traversed by {@link PixelIterator}. Note that this is not the
     * image size because of margin required by interpolation methods.
     *
     * <p>Since interpolator will receive data at coordinates {@code max} to {@code max + span - 1} inclusive
     * and since those coordinates are pixel centers, the points to interpolate are on the surface of a valid
     * pixel until {@code (max + span - 1) + 0.5}. Consequently, this method computes {@code max + span - 0.5}.
     * An additional 0.5 offset is added in the special case of nearest-neighbor interpolation for consistency
     * with {@link #interpolationSupportOffset(int)}.</p>
     *
     * @param  max   the maximal coordinate value, inclusive.
     * @param  span  the width or height of the support region for interpolations.
     * @return {@code max + span - 0.5} (except in nearest-neighbor special case).
     *
     * @see PixelIterator#getDomain()
     */
    private static double interpolationLimit(double max, final int span) {
        max += span;
        if (span > 1) max -= 0.5;           // Must be consistent with `interpolationSupportOffset(int)`.
        return max;
    }

    /**
     * Returns the number of quantities in the array returned by {@link #getPositionalAccuracy()}.
     */
    private int getPositionalAccuracyCount() {
        int n = 0;
        if (linearAccuracy != null) n++;
        if (toSourceSupport instanceof ResamplingGrid) n++;
        return n;
    }

    /**
     * Computes the {@value #POSITIONAL_ACCURACY_KEY} value. This method is invoked by {@link #getProperty(String)}
     * when the {@link #POSITIONAL_ACCURACY_KEY} property value is requested.
     */
    @SuppressWarnings("rawtypes")
    private Quantity<?>[] getPositionalAccuracy() {
        final Quantity<?>[] accuracy = new Quantity[getPositionalAccuracyCount()];
        int n = 0;
        if (linearAccuracy != null) {
            accuracy[n++] = linearAccuracy;
        }
        if (toSourceSupport instanceof ResamplingGrid) {
            accuracy[n++] = Quantities.create(ResamplingGrid.TOLERANCE, Units.PIXEL);
        }
        return accuracy;
    }

    /**
     * Computes the {@value #POSITIONAL_CONSISTENCY_KEY} value. This method is invoked by {@link #getProperty(String)}
     * when the {@link #POSITIONAL_CONSISTENCY_KEY} property value is requested. The result is saved by weak reference
     * since recomputing this image is rarely requested, and if needed can be recomputed easily.
     */
    private synchronized RenderedImage getPositionalConsistency() throws TransformException {
        ComputedImage image = (positionalConsistency != null) ? positionalConsistency.get() : null;
        if (image == null) {
            positionalConsistency = null;
            final Dimension s = interpolation.getSupportSize();
            final double[] offset = new double[toSourceSupport.getSourceDimensions()];
            offset[0] = -interpolationSupportOffset(s.width);
            offset[1] = -interpolationSupportOffset(s.height);
            final MathTransform tr = MathTransforms.concatenate(toSourceSupport, MathTransforms.translation(offset));
            image = new PositionalConsistencyImage(this, tr);
            positionalConsistency = image.reference();
        }
        return image;
    }

    /**
     * Computes the {@value #MASK_KEY} value. This method is invoked by {@link #getProperty(String)} when the
     * {@link #MASK_KEY} property value is requested. The result is saved by weak reference since recomputing
     * this image is rarely requested, and if needed can be recomputed easily.
     */
    private synchronized RenderedImage getMask() {
        ComputedImage image = (mask != null) ? mask.get() : null;
        if (image == null) {
            mask = null;                    // Cleared first in case an error occurs below.
            image = new MaskImage(this);
            mask = image.reference();
        }
        return image;
    }

    /**
     * Returns {@code true} if this image cannot have mask.
     */
    boolean hasNoMask() {
        return fillValues instanceof int[];
    }

    /**
     * Verifies whether image layout information are consistent. This method verifies that source coordinates
     * required by this image (computed by converting {@linkplain #getBounds() this image bounds} using the
     * {@link #toSource} transform) intersects the bounds of the source image. If this is not the case, then
     * this method returns {@code "toSource"} for signaling that the transform may have a problem.
     * Otherwise this method completes the check with all verifications
     * {@linkplain ComputedImage#verify() documented in parent class}
     *
     * @return {@code null} if image layout information are consistent,
     *         or the name of inconsistent attribute if a problem is found.
     */
    @Override
    public String verify() {
        if (toSource instanceof MathTransform2D) try {
            final Rectangle bounds = getBounds();
            final Rectangle2D tb = Shapes2D.transform((MathTransform2D) toSource, bounds, bounds);
            if (!ImageUtilities.getBounds(getSource()).intersects(tb)) {
                return "toSource";
            }
        } catch (TransformException e) {
            recoverableException("verify", e);
            return "toSource";
        }
        return super.verify();      // "width" and "height" properties should be checked last.
    }

    /**
     * Invoked when a non-fatal error occurred.
     *
     * @param  method  the method where the ignorable error occurred.
     * @param  error   the ignore which can be ignored.
     */
    private static void recoverableException(final String method, final Exception error) {
        Logging.recoverableException(LOGGER, ResampledImage.class, method, error);
    }

    /**
     * Returns the color model of this resampled image.
     * Default implementation assumes that this image has the same color model as the source image.
     *
     * @return the color model, or {@code null} if unspecified.
     */
    @Override
    public ColorModel getColorModel() {
        RenderedImage image = getDestination();
        if (image == null) image = getSource();
        return image.getColorModel();
    }

    /**
     * Gets a property from this image. Current default implementation supports the following keys
     * (more properties may be added to this list in any future Apache SIS versions):
     *
     * <ul>
     *   <li>{@value #POSITIONAL_ACCURACY_KEY}</li>
     *   <li>{@value #POSITIONAL_CONSISTENCY_KEY}</li>
     *   <li>{@value #SAMPLE_DIMENSIONS_KEY}  (forwarded to the source image)</li>
     *   <li>{@value #SAMPLE_RESOLUTIONS_KEY} (forwarded to the source image)</li>
     *   <li>{@value #MASK_KEY} if the image uses floating point numbers.</li>
     * </ul>
     *
     * <h4>Note on sample values</h4>
     * The sample resolutions are retained because they should have approximately the same values before and after
     * resampling. {@linkplain #STATISTICS_KEY Statistics} are not in this list because, while minimum and maximum
     * values should stay approximately the same, the average value and standard deviation may be quite different.
     */
    @Override
    public Object getProperty(final String key) {
        switch (key) {
            case SAMPLE_DIMENSIONS_KEY:
            case SAMPLE_RESOLUTIONS_KEY: {
                return getSource().getProperty(key);
            }
            case POSITIONAL_ACCURACY_KEY: {
                return getPositionalAccuracy();
            }
            case POSITIONAL_CONSISTENCY_KEY: try {
                return getPositionalConsistency();
            } catch (TransformException | IllegalArgumentException e) {
                throw (ImagingOpException) new ImagingOpException(e.getMessage()).initCause(e);
            }
            case MASK_KEY: {
                if (hasNoMask()) break;
                return getMask();
            }
        }
        return super.getProperty(key);
    }

    /**
     * Returns the names of all recognized properties, or {@code null} if this image has no properties.
     * The returned array contains the properties listed in {@link #getProperty(String)} if the source
     * image has those properties.
     *
     * @return names of all recognized properties, or {@code null} if none.
     */
    @Override
    @SuppressWarnings("StringEquality")
    public String[] getPropertyNames() {
        final String[] inherited = getSource().getPropertyNames();
        final String[] names = {
            SAMPLE_DIMENSIONS_KEY,
            SAMPLE_RESOLUTIONS_KEY,
            POSITIONAL_ACCURACY_KEY,
            POSITIONAL_CONSISTENCY_KEY,
            MASK_KEY
        };
        int n = 0;
        for (final String name : names) {
            if (name != POSITIONAL_CONSISTENCY_KEY) {           // Identity comparisons are okay for this method.
                if (name == POSITIONAL_ACCURACY_KEY) {
                    if (getPositionalAccuracyCount() == 0) {
                        continue;                               // Exclude PositionalAccuracy change.
                    }
                } else if (name == MASK_KEY) {
                    if (hasNoMask()) {
                        continue;
                    }
                } else if (!ArraysExt.contains(inherited, name)) {
                    continue;                       // Exclude inherited property not defined by source.
                }
            }
            names[n++] = name;
        }
        return ArraysExt.resize(names, n);
    }

    /**
     * Returns a shape containing all pixels that are valid in this image.
     * This method returns the valid area of the source image transformed
     * by the inverse of {@link #toSource}, mapping pixel corners.
     *
     * @return the valid area of the source converted to the coordinate system of this resampled image.
     *
     * @since 1.5
     */
    @Override
    public synchronized Shape getValidArea() {
        Shape domain = validArea;
        if (domain == null) try {
            final var ts = new TransformSeparator(toSource);
            ts.addSourceDimensionRange(0, BIDIMENSIONAL);
            ts.addTargetDimensionRange(0, BIDIMENSIONAL);
            MathTransform mt = ts.separate();
            MathTransform centerToCorner = MathTransforms.uniformTranslation(BIDIMENSIONAL, -0.5);
            mt = MathTransforms.concatenate(centerToCorner, mt);
            mt = MathTransforms.concatenate(centerToCorner, mt.inverse());
            domain = ImageUtilities.getValidArea(getSource());
            domain = MathTransforms.bidimensional(mt).createTransformedShape(domain);
            final Area area = new Area(domain);
            area.intersect(new Area(getBounds()));
            validArea = domain = area.isRectangular() ? area.getBounds2D() : area;
        } catch (FactoryException | TransformException e) {
            recoverableException("getValidArea", e);
            validArea = domain = getBounds();
        }
        if (domain instanceof Area) {
            domain = (Area) ((Area) domain).clone();    // Cloning an Area is cheap.
        } else if (domain instanceof Rectangle2D) {
            domain = (Rectangle2D) ((Rectangle2D) domain).clone();
        }
        return domain;
    }

    /**
     * Returns the minimum tile index in the <var>x</var> direction.
     * This is often 0.
     *
     * @return the minimum tile index in the <var>x</var> direction.
     */
    @Override
    public final int getMinTileX() {
        return minTileX;
    }

    /**
     * Returns the minimum tile index in the <var>y</var> direction.
     * This is often 0.
     *
     * @return the minimum tile index in the <var>y</var> direction.
     */
    @Override
    public final int getMinTileY() {
        return minTileY;
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
            xlim = interpolationLimit(xmax, support.width);     // Upper limit of coordinates where we can interpolate.
            ylim = interpolationLimit(ymax, support.height);
            xoff = interpolationSupportOffset(support.width)  - 0.5;    // Always negative (or 0 for nearest-neighbor).
            yoff = interpolationSupportOffset(support.height) - 0.5;
        }
        /*
         * In the special case of nearest-neighbor interpolation with no precision lost, the code inside the loop
         * can take a shorter path were data are just copied. The lossless criterion allows us to omit the checks
         * for minimal and maximal values. Shortcut may apply to both integer values and floating point values.
         */
        final boolean useFillValues = (getDestination() == null);
        final boolean shortcut = useFillValues && Interpolation.NEAREST.equals(interpolation) &&
                    ImageUtilities.isLosslessConversion(sampleModel, tile.getSampleModel());
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
            valuesArray = intValues = new int[scanline * numBands];
            if (shortcut) {
                values    = null;                                   // No floating point values to transfer.
                minValues = null;                                   // Min/max checks are not needed in shortcut case.
                maxValues = null;
            } else {
                values    = new double[numBands];
                minValues = new long  [numBands];
                maxValues = new long  [numBands];
                final SampleModel sm = tile.getSampleModel();
                for (int b=0; b<numBands; b++) {
                    maxValues[b] = Numerics.bitmask(sm.getSampleSize(b)) - 1;
                }
                if (!DataType.isUnsigned(sm)) {
                    for (int b=0; b<numBands; b++) {
                        minValues[b] = ~(maxValues[b] >>>= 1);      // Convert unsigned type to signed type range.
                    }
                }
            }
        } else {
            intValues   = null;
            values      = new double[scanline * numBands];
            valuesArray = values;
            minValues   = null;                                     // Not used for floating point types.
            maxValues   = null;
        }
        /*
         * The (sx,sy) values are iterator position, remembered for detecting if the window buffer
         * needs to be updated. The `Integer.MAX_VALUE` initial value is safe because the iterator
         * cannot have that position (its construction would have failed with ArithmeticException
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
             * Pixel coordinate along X axis where to start writing the `values` or `intValues` array.
             * This is usually the first column of the tile, and the number of pixels to write is the
             * tile width (i.e. we write a full tile row). However, those values may be modified below
             * if we avoid writing pixels that are outside the source image.
             */
            int posX = tileMinX;
            if (shortcut) {
                /*
                 * Special case for nearest-neighbor interpolation without the need to check for min/max values.
                 * In this case values will be copied as `int` or `double` type without further processing.
                 */
                int ci = 0;     // Index in `coordinates` array.
                int vi = 0;     // Index in `values` or `intValues` array.
                for (int tx=tileMinX; tx<tileMaxX; tx++, ci+=tgtDim, vi+=numBands) {
                    final long x = (long) Math.floor(coordinates[ci]);
                    if (x >= it.lowerX && x < it.upperX) {
                        final long y = (long) Math.floor(coordinates[ci+1]);
                        if (y >= it.lowerY && y < it.upperY) {
                            if (sx != (sx = (int) x)  |                 // Really |, not ||.
                                sy != (sy = (int) y))
                            {
                                it.moveTo(sx, sy);
                            }
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
                 * to +0.5 in nearest-neighbor interpolation case, for the same reason as other borders.
                 * However if the interpolation is bilinear, then the fractional parts on the bottom and
                 * right borders can go up to 1.5 because `PixelIterator` has reduced the (xmax, ymax)
                 * values by 1 (for taking in account the padding needed for interpolation support).
                 * This tolerance can be generalized (2.5, 3.5, etc.) depending on interpolation method.
                 */
                int ci = 0;     // Index in `coordinates` array.
                int vi = 0;     // Index in `values` or `intValues` array.
                for (int tx=tileMinX; tx<tileMaxX; tx++, ci+=tgtDim) {
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
                                    interpolation.interpolate(buffer.values, numBands, xf, yf, values, isInteger ? 0 : vi);
                                    if (isInteger) {
                                        for (int b=0; b<numBands; b++) {
                                            intValues[vi+b] = (int) Math.max(minValues[b],
                                                                    Math.min(maxValues[b], Math.round(values[b])));
                                        }
                                    }
                                    vi += numBands;
                                    continue;       // Values have been set, move to next pixel.
                                }
                            }
                        }
                    }
                    /*
                     * If we reach this point then any of the "if" conditions above failed
                     * (i.e. the point to interpolate is outside the source image bounds)
                     * and no values have been set in the `values` or `intValues` array.
                     * If we are writing in an existing image, do not write anything
                     * (i.e. keep the existing value). Otherwise write the fill values.
                     */
                    if (useFillValues) {
                        System.arraycopy(fillValues, 0, valuesArray, vi, numBands);
                        vi += numBands;
                    } else {
                        if (vi != 0) {
                            final int numX = vi / numBands;
                            if (isInteger) {
                                tile.setPixels(posX, ty, numX, 1, intValues);
                            } else {
                                tile.setPixels(posX, ty, numX, 1, values);
                            }
                            posX += numX;
                            vi = 0;
                        }
                        posX++;
                    }
                }
            }
            /*
             * At this point we finished to compute the value of a scanline.
             * Copy to its final destination then move to next line.
             */
            final int numX = scanline - (posX - tileMinX);
            if (numX != 0) {
                if (isInteger) {
                    tile.setPixels(posX, ty, numX, 1, intValues);
                } else {
                    tile.setPixels(posX, ty, numX, 1, values);
                }
            }
        }
        return tile;
    }

    /**
     * Notifies the source image that tiles will be computed soon in the given region.
     * If the source image is an instance of {@link ComputedImage}, then this method
     * forwards the notification to it. Otherwise default implementation does nothing.
     *
     * @since 1.2
     */
    @Override
    protected Disposable prefetch(final Rectangle tiles) {
        final RenderedImage source = getSource();
        if (source instanceof PlanarImage) try {
            final Dimension s = interpolation.getSupportSize();
            Rectangle pixels = ImageUtilities.tilesToPixels(this, tiles);
            final var bounds = new Rectangle2D.Double(
                    pixels.x      -  0.5  *  s.width,
                    pixels.y      -  0.5  *  s.height,
                    pixels.width  + (double) s.width,
                    pixels.height + (double) s.height);
            pixels = (Rectangle) Shapes2D.transform(MathTransforms.bidimensional(toSource), bounds, pixels);
            ImageUtilities.clipBounds(source, pixels);
            return ((PlanarImage) source).prefetch(ImageUtilities.pixelsToTiles(source, pixels));
        } catch (TransformException e) {
            recoverableException("prefetch", e);
        }
        return super.prefetch(tiles);
    }

    /**
     * Compares the given object with this image for equality. This method returns {@code true}
     * if the given object is non-null, is an instance of the exact same class as this image,
     * has equal sources and do the same resampling operation (same interpolation method,
     * same fill values, same coordinates).
     *
     * @param  object  the object to compare with this image.
     * @return {@code true} if the given object is an image performing the same resampling as this image.
     */
    @Override
    public boolean equals(final Object object) {
        if (equalsBase(object)) {
            final var other = (ResampledImage) object;
            return minX     == other.minX &&
                   minY     == other.minY &&
                   width    == other.width &&
                   height   == other.height &&
                   minTileX == other.minTileX &&
                   minTileY == other.minTileY &&
                   interpolation.equals(other.interpolation) &&
                   Objects.deepEquals(fillValues, other.fillValues) &&
                   toSource.equals(other.toSource);
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
        return hashCodeBase() + minX + 31*(minY + 31*(width + 31*height))
                + interpolation.hashCode() + toSource.hashCode();
    }
}
