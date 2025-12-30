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
import java.util.Optional;
import java.util.Collection;
import java.util.stream.Stream;
import java.awt.image.RenderedImage;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.image.DataType;
import org.apache.sis.image.ImageProcessor;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coverage.CannotEvaluateException;


/**
 * Decorates a {@link GridCoverage} in order to convert sample values on the fly.
 * There are two strategies about when to convert sample values:
 *
 * <ul>
 *   <li>In calls to {@link #render(GridExtent)}, sample values are converted when first needed
 *       on a tile-by-tile basis then cached for future reuse. Note however that discarding the
 *       returned image may result in the lost of cached tiles.</li>
 *   <li>In calls to {@link GridCoverage.Evaluator#apply(DirectPosition)}, the conversion is applied
 *       on-the-fly each time in order to avoid the potentially costly tile computations.</li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ConvertedGridCoverage extends DerivedGridCoverage {
    /**
     * Conversions from {@linkplain #source source} values to converted values.
     * The length of this array shall be equal to the number of bands.
     */
    private final MathTransform1D[] converters;

    /**
     * Whether this grid coverage is for converted values.
     * If {@code false}, then this coverage is for packed values.
     */
    private final boolean isConverted;

    /**
     * One of enumeration value that describe the sample values type in each band
     * of images produced by {@link #render(GridExtent)}. Shall not be {@code null}.
     *
     * @see #getBandType()
     */
    private final DataType bandType;

    /**
     * The image processor to use for creating the tiles of converted values.
     */
    private final ImageProcessor processor;

    /**
     * {@code true} if the conversion was defined by user, or {@code false} if this instance
     * has been created by {@link #forConvertedValues(boolean)} implementation.
     */
    private final boolean isUSerDefined;

    /**
     * Creates a new coverage with the same grid geometry as the given coverage but converted sample dimensions.
     *
     * @param  source       the coverage containing source values.
     * @param  range        the sample dimensions to assign to the converted grid coverage.
     * @param  converters   conversion from source to converted coverage, one transform per band.
     * @param  isConverted  whether this grid coverage is for converted or packed values.
     * @param  processor    the image processor to use for creating the tiles of converted values.
     */
    ConvertedGridCoverage(final GridCoverage source, final List<SampleDimension> range,
                          final MathTransform1D[] converters, final boolean isConverted,
                          final ImageProcessor processor, final boolean isUSerDefined)
    {
        super(source, range);
        this.converters    = converters;
        this.isConverted   = isConverted;
        this.bandType      = getBandType(range, isConverted, source);
        this.processor     = processor;
        this.isUSerDefined = isUSerDefined;
    }

    /**
     * Returns a coverage of converted values computed from a coverage of packed values, or conversely.
     * If the given coverage is already converted, then this method returns {@code coverage} unchanged.
     * This method is used for {@link GridCoverage#forConvertedValues(boolean)} default implementation.
     *
     * @param  source     the coverage containing values to convert.
     * @param  converted  {@code true} for a coverage containing converted values,
     *                    or {@code false} for a coverage containing packed values.
     * @return the converted coverage. May be {@code source}.
     * @throws NoninvertibleTransformException if this constructor cannot build a full conversion chain to target.
     */
    static GridCoverage create(final GridCoverage source, final boolean converted) throws NoninvertibleTransformException {
        final List<SampleDimension> sources = source.getSampleDimensions();
        final List<SampleDimension> targets = new ArrayList<>(sources.size());
        final MathTransform1D[]  converters = converters(sources, targets, converted);
        if (converters == null) {
            return source;
        }
        return new ConvertedGridCoverage(source, targets, converters, converted, Lazy.PROCESSOR, false);
    }

    /**
     * Returns the transforms for converting sample values from given sources to the {@code converted} status
     * of those sources. This method opportunistically adds the target sample dimensions in {@code target} list.
     *
     * @param  sources    {@link GridCoverage#getSampleDimensions()} of {@code source} coverage.
     * @param  targets    where to add {@link SampleDimension#forConvertedValues(boolean)} results.
     * @param  converted  {@code true} for transforms to converted values, or {@code false} for transforms to packed values.
     * @return the transforms, or {@code null} if all transforms are identity transform.
     * @throws NoninvertibleTransformException if this method cannot build a full conversion chain.
     */
    static MathTransform1D[] converters(final List<SampleDimension> sources,
                                        final List<SampleDimension> targets,
                                        final boolean converted)
            throws NoninvertibleTransformException
    {
        final int               numBands   = sources.size();
        final MathTransform1D   identity   = (MathTransform1D) MathTransforms.identity(1);
        final MathTransform1D[] converters = new MathTransform1D[numBands];
        Arrays.fill(converters, identity);
        for (int i = 0; i < numBands; i++) {
            final SampleDimension src = sources.get(i);
            final SampleDimension tgt = src.forConvertedValues(converted);
            targets.add(tgt);
            if (src != tgt) {
                MathTransform1D tr = src.getTransferFunction().orElse(identity);
                Optional<MathTransform1D> complete = tgt.getTransferFunction();
                if (complete.isPresent()) {
                    tr = MathTransforms.concatenate(tr, complete.get().inverse());
                }
                converters[i] = tr;
            }
        }
        for (final MathTransform1D converter : converters) {
            if (!converter.isIdentity()) return converters;
        }
        return null;
    }

    /**
     * Returns the data type for range of values of given sample dimensions.
     * This data type applies to each band, not to a packed sample model
     * (e.g. we assume no packing of 4 byte values in a single 32-bits integer).
     *
     * @param  targets    the sample dimensions for which to get the data type.
     * @param  converted  whether the image will hold converted or packed values.
     * @param  source     if the type cannot be determined, coverage from which to inherit the type as a fallback.
     * @return the data type (never null).
     *
     * @see GridCoverage#getBandType()
     */
    static DataType getBandType(final List<SampleDimension> targets, final boolean converted, final GridCoverage source) {
        NumberRange<?> union = null;
        boolean allowsNaN = false;
        for (final SampleDimension dimension : targets) {
            final Optional<NumberRange<?>> c = dimension.getSampleRange();
            if (c.isPresent()) {
                final NumberRange<?> range = c.get();
                if (union == null) {
                    union = range;
                } else {
                    /*
                     * We do not want unit conversions for this union, because the union is used
                     * only for determining a data type having the capacity to store the values.
                     * The physical meaning of those values is not relevant here.
                     */
                    if (union instanceof MeasurementRange<?>) {
                        union = new NumberRange<>(union);
                    }
                    union = union.unionAny(range);
                }
            }
            if (!allowsNaN) allowsNaN = dimension.allowsNaN();
        }
        if (union == null) {
            return source.getBandType();
        }
        DataType type = DataType.forRange(union, !converted);
        if (allowsNaN) {
            type = type.toFloat();
        }
        return type;
    }

    /**
     * Returns the constant identifying the primitive type used for storing sample values.
     */
    @Override
    final DataType getBandType() {
        return bandType;
    }

    /**
     * Returns {@code true} if this coverage should not be replaced by its source.
     *
     * @see GridCoverageProcessor.Optimization#REPLACE_SOURCE
     */
    @Override
    final boolean isNotRepleacable() {
        return isUSerDefined;
    }

    /**
     * Creates a new function for computing or interpolating sample values at given locations.
     *
     * <h4>Multi-threading</h4>
     * {@code Evaluator}s are not thread-safe. For computing sample values concurrently,
     * a new {@link Evaluator} instance should be created for each thread.
     */
    @Override
    public Evaluator evaluator() {
        return new SampleConverter();
    }

    /**
     * Implementation of evaluator returned by {@link ConvertedGridCoverage#evaluator()}.
     * This evaluator delegates all operations to the {@link #source} coverage and converts
     * the returned sample values.
     */
    private final class SampleConverter extends EvaluatorWrapper {
        /**
         * Creates a new evaluator for the enclosing coverage.
         */
        SampleConverter() {
            super(source.evaluator());
        }

        /**
         * Returns the enclosing coverage.
         */
        @Override
        public GridCoverage getCoverage() {
            return ConvertedGridCoverage.this;
        }

        /**
         * Returns a sequence of double values for a given point in the coverage.
         * This method delegates to the source coverage, then converts the values.
         *
         * @param  point  the position where to evaluate.
         * @throws CannotEvaluateException if the values cannot be computed.
         */
        @Override
        public double[] apply(final DirectPosition point) throws CannotEvaluateException {
            return convert(super.apply(point));
        }

        /**
         * Returns a stream of double values for given points in the coverage.
         * This method delegates to the source coverage, then converts the values.
         *
         * @param  point    the position where to evaluate.
         * @param  parallel {@code true} for a parallel stream, or {@code false} for a sequential stream.
         */
        @Override
        public Stream<double[]> stream(Collection<? extends DirectPosition> points, boolean parallel) {
            return super.stream(points, parallel).map(this::convert);
        }

        /**
         * Converts the given sample values.
         *
         * @param  values  the sample values to convert. May be null.
         * @return the converted sample values, or {@code null} if the given values were null.
         */
        private double[] convert(final double[] values) {
            if (values != null) try {
                final MathTransform1D[] converters = ConvertedGridCoverage.this.converters;
                for (int i=0; i<converters.length; i++) {
                    values[i] = converters[i].transform(values[i]);
                }
            } catch (TransformException ex) {
                throw new CannotEvaluateException(ex.getMessage(), ex);
            }
            return values;
        }
    }

    /**
     * Creates a converted view over {@linkplain #source source} data for the given extent.
     * Values will be converted when first requested on a tile-by-tile basis.
     * Note that if the returned image is discarded, then the cache of converted
     * tiles will be discarded too.
     *
     * @return the grid slice as a rendered image with converted view.
     */
    @Override
    public RenderedImage render(final GridExtent sliceExtent) {
        RenderedImage image = source.render(sliceExtent);
        /*
         * That image should never be null. But if an implementation wants to do so, respect that.
         * We do not cache the image because caching is already handled by `ImageProcessor`,
         * assuming that `source` returned an image from its own cache.
         */
        if (image != null) {
            image = convert(image, bandType, converters, processor);
        }
        return image;
    }

    /**
     * Returns this coverage or the source coverage depending on whether {@code converted} matches
     * the kind of content of this coverage.
     */
    @Override
    public GridCoverage forConvertedValues(final boolean converted) {
        return (converted == isConverted) ? this : source;
    }

    /**
     * Overridden as a matter of principle, but should not been invoked.
     */
    @Override
    protected final GridCoverage createConvertedValues(final boolean converted) {
        return forConvertedValues(converted);
    }
}
