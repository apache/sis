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
package org.apache.sis.internal.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageProcessor;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.AbstractGridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.RasterLoadingStrategy;
import org.apache.sis.storage.UnsupportedQueryException;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.util.GenericName;

/**
 * Create a coverage resource with sample values converted by the given functions.
 * The number of sample dimensions in the returned coverage is the length of the {@code converters} array,
 * which must be greater than 0 and not greater than the number of sample dimensions in the source coverage.
 * If the {@code converters} array length is less than the number of source sample dimensions,
 * then all sample dimensions at index ≥ {@code converters.length} will be ignored.
 *
 * <h2>Sample dimensions customization</h2>
 * By default, this method creates new sample dimensions with the same names and categories than in the
 * previous coverage, but with {@linkplain org.apache.sis.coverage.Category#getSampleRange() sample ranges}
 * converted using the given converters and with {@linkplain SampleDimension#getUnits() units of measurement}
 * omitted. This behavior can be modified by specifying a non-null {@code sampleDimensionModifier} function.
 * If non-null, that function will be invoked with, as input, a pre-configured sample dimension builder.
 * The {@code sampleDimensionModifier} function can {@linkplain SampleDimension.Builder#setName(CharSequence)
 * change the sample dimension name} or {@linkplain SampleDimension.Builder#categories() rebuild the categories}.
 *
 * <h2>Result relationship with source</h2>
 * If the source coverage is backed by a {@link java.awt.image.WritableRenderedImage},
 * then changes in the source coverage are reflected in the returned coverage and conversely.
 *
 * @see GridCoverageProcessor#convert(org.apache.sis.coverage.grid.GridCoverage, org.opengis.referencing.operation.MathTransform1D[], java.util.function.Function)
 * @see ImageProcessor#convert(RenderedImage, NumberRange<?>[], MathTransform1D[], DataType, ColorModel)
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ConvertedCoverageResource extends AbstractGridCoverageResource {

    private final GridCoverageResource source;
    private final MathTransform1D[] converters;
    private final Function<SampleDimension.Builder, SampleDimension> sampleDimensionModifier;
    private List<SampleDimension> sampleDimensions;

    /**
     *
     * @param source the coverage resource for which to convert sample values.
     * @param converters the transfer functions to apply on each sample dimension of the source coverage.
     * @param sampleDimensionModifier a callback for modifying the {@link SampleDimension.Builder} default
     *         configuration for each sample dimension of the target coverage, or {@code null} if none.
     */
    public ConvertedCoverageResource(GridCoverageResource source, MathTransform1D[] converters,
            Function<SampleDimension.Builder, SampleDimension> sampleDimensionModifier) {
        super(null, false);
        ArgumentChecks.ensureNonNull("base", source);
        ArgumentChecks.ensureNonNull("converters", converters);
        this.source = source;
        this.converters = converters;
        this.sampleDimensionModifier = sampleDimensionModifier;
    }

    @Override
    public GridGeometry getGridGeometry() throws DataStoreException {
        return source.getGridGeometry();
    }

    @Override
    public synchronized List<SampleDimension> getSampleDimensions() throws DataStoreException {
        if (sampleDimensions == null) {
            sampleDimensions = new ArrayList<>(source.getSampleDimensions());
            if (this.sampleDimensionModifier != null) {
                for (int i = 0; i < converters.length; i++) {
                    final SampleDimension.Builder builder = new SampleDimension.Builder();
                    final SampleDimension band = sampleDimensions.get(i);
                    final MathTransform1D converter = converters[i];
                    band.getBackground().ifPresent(builder::setBackground);
                    band.getCategories().forEach((category) -> {
                        if (category.isQuantitative()) {
                            // Unit is assumed different as a result of conversion.
                            builder.addQuantitative(category.getName(), category.getSampleRange(), converter, null);
                        } else {
                            builder.addQualitative(category.getName(), category.getSampleRange());
                        }
                    });
                    builder.setName(band.getName());
                    sampleDimensions.set(i, sampleDimensionModifier.apply(builder));
                }
            }
            sampleDimensions = Collections.unmodifiableList(sampleDimensions);
        }
        return sampleDimensions;
    }

    @Override
    public List<double[]> getResolutions() throws DataStoreException {
        return source.getResolutions();
    }

    @Override
    public GridCoverageResource subset(Query query) throws UnsupportedQueryException, DataStoreException {
        final GridCoverageResource subset = source.subset(query);
        return new ConvertedCoverageResource(subset, converters, sampleDimensionModifier);
    }

    @Override
    public GridCoverage read(GridGeometry domain, int... range) throws DataStoreException {
        final GridCoverage coverage = source.read(domain, range);
        final MathTransform1D[] trs;
        if (range != null) {
            trs = new MathTransform1D[range.length];
            for (int i = 0; i < trs.length; i++) {
                trs[i] = converters[range[i]];
            }
        } else {
            trs = converters.clone();
        }
        return new GridCoverageProcessor().convert(coverage, trs, sampleDimensionModifier);
    }

    @Override
    public RasterLoadingStrategy getLoadingStrategy() throws DataStoreException {
        return source.getLoadingStrategy();
    }

    @Override
    public boolean setLoadingStrategy(RasterLoadingStrategy strategy) throws DataStoreException {
        return source.setLoadingStrategy(strategy);
    }

    @Override
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        return source.getEnvelope();
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return source.getIdentifier();
    }

}
