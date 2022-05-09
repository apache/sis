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

import java.awt.Shape;
import java.util.Objects;
import java.awt.image.RenderedImage;
import javax.measure.Quantity;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.image.Interpolation;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.util.FinalFieldSetter;
import org.apache.sis.coverage.RegionOfInterest;


/**
 * A predefined set of operations on grid coverages as convenience methods.
 *
 * <h2>Thread-safety</h2>
 * {@code GridCoverageProcessor} is safe for concurrent use in multi-threading environment.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see org.apache.sis.image.ImageProcessor
 *
 * @since 1.1
 * @module
 */
public class GridCoverageProcessor implements Cloneable {
    /**
     * Configured {@link ImageProcessor} instances used by {@link GridCoverage}s created by processors.
     * We use this set for sharing common instances in {@link GridCoverage} instances, which is okay
     * provided that we do not modify the {@link ImageProcessor} configuration.
     */
    private static final WeakHashSet<ImageProcessor> PROCESSORS = new WeakHashSet<>(ImageProcessor.class);

    /**
     * Returns an unique instance of the given processor. Both the given and the returned processors
     * shall be unmodified, because they may be shared by many {@link GridCoverage} instances.
     */
    static ImageProcessor unique(final ImageProcessor image) {
        return PROCESSORS.unique(image);
    }

    /**
     * The processor to use for operations on two-dimensional slices.
     */
    protected final ImageProcessor imageProcessor;

    /**
     * Creates a new processor with default configuration.
     */
    public GridCoverageProcessor() {
        imageProcessor = new ImageProcessor();
    }

    /**
     * Creates a new processor initialized to the given configuration.
     *
     * @param  processor  the processor to use for operations on two-dimensional slices.
     */
    public GridCoverageProcessor(final ImageProcessor processor) {
        ArgumentChecks.ensureNonNull("processor", processor);
        imageProcessor = processor.clone();
    }

    /**
     * Returns the interpolation method to use for resampling operations.
     * The default implementation delegates to the image processor.
     *
     * @return interpolation method to use in resampling operations.
     *
     * @see ImageProcessor#getInterpolation()
     */
    public Interpolation getInterpolation() {
        return imageProcessor.getInterpolation();
    }

    /**
     * Sets the interpolation method to use for resampling operations.
     * The default implementation delegates to the image processor.
     *
     * @param  method  interpolation method to use in resampling operations.
     *
     * @see ImageProcessor#setInterpolation(Interpolation)
     */
    public void setInterpolation(final Interpolation method) {
        imageProcessor.setInterpolation(method);
    }

    /**
     * Returns hints about the desired positional accuracy, in "real world" units or in pixel units.
     * The default implementation delegates to the image processor.
     *
     * @return desired accuracy in no particular order, or an empty array if none.
     *
     * @see ImageProcessor#getPositionalAccuracyHints()
     */
    public Quantity<?>[] getPositionalAccuracyHints() {
        return imageProcessor.getPositionalAccuracyHints();
    }

    /**
     * Sets hints about desired positional accuracy, in "real world" units or in pixel units.
     * The default implementation delegates to the image processor.
     *
     * @param  hints  desired accuracy in no particular order, or a {@code null} array if none.
     *                Null elements in the array are ignored.
     *
     * @see ImageProcessor#setPositionalAccuracyHints(Quantity...)
     */
    public void setPositionalAccuracyHints(final Quantity<?>... hints) {
        imageProcessor.setPositionalAccuracyHints(hints);
    }

    /**
     * Returns the values to use for pixels that can not be computed.
     * The default implementation delegates to the image processor.
     *
     * @return fill values to use for pixels that can not be computed, or {@code null} for the defaults.
     *
     * @see ImageProcessor#getFillValues()
     *
     * @since 1.2
     */
    public Number[] getFillValues() {
        return imageProcessor.getFillValues();
    }

    /**
     * Sets the values to use for pixels that can not be computed.
     * The default implementation delegates to the image processor.
     *
     * @param  values  fill values to use for pixels that can not be computed, or {@code null} for the defaults.
     *
     * @see ImageProcessor#setFillValues(Number...)
     *
     * @since 1.2
     */
    public void setFillValues(final Number... values) {
        imageProcessor.setFillValues(values);
    }

    /**
     * Applies a mask defined by a region of interest (ROI). If {@code maskInside} is {@code true},
     * then all pixels inside the given ROI are set to the {@linkplain #getFillValues() fill values}.
     * If {@code maskInside} is {@code false}, then the mask is reversed:
     * the pixels set to fill values are the ones outside the ROI.
     *
     * @param  source      the coverage on which to apply a mask.
     * @param  mask        region (in arbitrary CRS) of the mask.
     * @param  maskInside  {@code true} for masking pixels inside the shape, or {@code false} for masking outside.
     * @return a coverage with mask applied.
     * @throws TransformException if ROI coordinates can not be transformed to grid coordinates.
     *
     * @since 1.2
     */
    public GridCoverage mask(final GridCoverage source, final RegionOfInterest mask, final boolean maskInside)
            throws TransformException
    {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("mask", mask);
        final Shape roi = mask.toShape2D(source.getGridGeometry());
        RenderedImage data = source.render(null);
        data = imageProcessor.mask(data, roi, maskInside);
        return new GridCoverage2D(source, data);
    }

    /**
     * Creates a new coverage with a different grid extent, resolution or coordinate reference system.
     * The desired properties are specified by the {@link GridGeometry} argument, which may be incomplete.
     * The missing grid geometry components are completed as below:
     *
     * <table class="sis">
     *   <caption>Default values for undefined grid geometry components</caption>
     *   <tr>
     *     <th>Component</th>
     *     <th>Default value</th>
     *   </tr><tr>
     *     <td>{@linkplain GridGeometry#getExtent() Grid extent}</td>
     *     <td>A default size preserving resolution at source
     *       {@linkplain GridExtent#getPointOfInterest() point of interest}.</td>
     *   </tr><tr>
     *     <td>{@linkplain GridGeometry#getGridToCRS Grid to CRS transform}</td>
     *     <td>Whatever it takes for fitting data inside the supplied extent.</td>
     *   </tr><tr>
     *     <td>{@linkplain GridGeometry#getCoordinateReferenceSystem() Coordinate reference system}</td>
     *     <td>Same as source coverage.</td>
     *   </tr>
     * </table>
     *
     * The interpolation method can be specified by {@link #setInterpolation(Interpolation)}.
     * If the grid coverage values are themselves interpolated, this method tries to use the
     * original data. The intent is to avoid adding interpolations on top of other interpolations.
     *
     * @param  source  the grid coverage to resample.
     * @param  target  the desired geometry of returned grid coverage. May be incomplete.
     * @return a grid coverage with the characteristics specified in the given grid geometry.
     * @throws IncompleteGridGeometryException if the source grid geometry is missing an information.
     *         It may be the source CRS, the source extent, <i>etc.</i> depending on context.
     * @throws TransformException if some coordinates can not be transformed to the specified target.
     */
    public GridCoverage resample(GridCoverage source, final GridGeometry target) throws TransformException {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("target", target);
        final boolean isConverted = source == source.forConvertedValues(true);
        /*
         * If the source coverage is already the result of a previous "resample" operation,
         * use the original data in order to avoid interpolating values that are already interpolated.
         */
        for (;;) {
            if (ResampledGridCoverage.equivalent(source.getGridGeometry(), target)) {
                return source;
            } else if (source instanceof ResampledGridCoverage) {
                source = ((ResampledGridCoverage) source).source;
            } else if (source instanceof ConvertedGridCoverage) {
                source = ((ConvertedGridCoverage) source).source;
            } else {
                break;
            }
        }
        /*
         * The projection are usually applied on floating-point values, in order
         * to gets maximal precision and to handle correctly the special case of
         * NaN values. However, we can apply the projection on integer values if
         * the interpolation type is "nearest neighbor" since this is not really
         * an interpolation.
         */
        if (!Interpolation.NEAREST.equals(imageProcessor.getInterpolation())) {
            source = source.forConvertedValues(true);
        }
        final GridCoverage resampled;
        try {
            resampled = ResampledGridCoverage.create(source, target, imageProcessor);
        } catch (IllegalGridGeometryException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof TransformException) {
                throw (TransformException) cause;
            } else {
                throw e;
            }
        } catch (FactoryException e) {
            throw new TransformException(e.getMessage(), e);
        }
        return resampled.forConvertedValues(isConverted);
    }

    /**
     * Invoked when an ignorable exception occurred.
     *
     * @param  caller  the method where the exception occurred.
     * @param  ex      the ignorable exception.
     */
    static void recoverableException(final String caller, final Exception ex) {
        Logging.recoverableException(Logging.getLogger(Modules.RASTER), GridCoverageProcessor.class, caller, ex);
    }

    /**
     * Returns {@code true} if the given object is a coverage processor
     * of the same class with the same configuration.
     *
     * @param  object  the other object to compare with this processor.
     * @return whether the other object is a coverage processor of the same class with the same configuration.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            final GridCoverageProcessor other = (GridCoverageProcessor) object;
            return imageProcessor.equals(other.imageProcessor);
        }
        return false;
    }

    /**
     * Returns a hash code value for this coverage processor based on its current configuration.
     *
     * @return a hash code value for this processor.
     */
    @Override
    public int hashCode() {
        return Objects.hash(getClass(), imageProcessor);
    }

    /**
     * Returns a coverage processor with the same configuration than this processor.
     *
     * @return a clone of this coverage processor.
     */
    @Override
    public GridCoverageProcessor clone() {
        try {
            final GridCoverageProcessor clone = (GridCoverageProcessor) super.clone();
            FinalFieldSetter.set(GridCoverageProcessor.class, "imageProcessor", clone, imageProcessor.clone());
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        } catch (ReflectiveOperationException e) {
            throw FinalFieldSetter.cloneFailure(e);
        }
    }
}
