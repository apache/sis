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

import java.awt.image.RenderedImage;
import org.opengis.util.FactoryException;
import org.apache.sis.image.DataType;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.internal.Numerics;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.coverage.SubspaceNotSpecifiedException;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coverage.CannotEvaluateException;


/**
 * A grid coverage with extra dimensions appended.
 * All extra dimensions have a grid size of one cell.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class DimensionAppender extends GridCoverage {
    /**
     * The source grid coverage for which to append extra dimensions.
     */
    private final GridCoverage source;

    /**
     * The dimensions added to the source grid coverage.
     * Should have a grid size of one cell in all dimensions.
     */
    private final GridGeometry dimToAdd;

    /**
     * Creates a new dimension appender for the given grid coverage.
     * The grid extent of {@code dimToAdd} shall have a grid size of one cell in all dimensions.
     *
     * @param  source    the source grid coverage for which to append extra dimensions.
     * @param  dimToAdd  the dimensions to add to the source grid coverage.
     * @throws FactoryException if the compound CRS cannot be created.
     * @throws IllegalGridGeometryException if a dimension has more than one grid cell.
     * @throws IllegalArgumentException if the concatenation results in duplicated
     *         {@linkplain GridExtent#getAxisType(int) grid axis types}.
     */
    private DimensionAppender(final GridCoverage source, final GridGeometry dimToAdd) throws FactoryException {
        super(source, new GridGeometry(source.getGridGeometry(), dimToAdd));
        this.source   = source;
        this.dimToAdd = dimToAdd;
        final GridExtent extent = dimToAdd.getExtent();
        for (int i = extent.getDimension(); --i >= 0;) {
            final long size = extent.getSize(i);
            if (size != 1) {
                throw new IllegalGridGeometryException(Resources.format(Resources.Keys.NotASlice_2,
                        extent.getAxisIdentification(i,i), size));
            }
        }
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    static DimensionAppender create(GridCoverage source, GridGeometry dimToAdd) throws FactoryException {
        if (source instanceof DimensionAppender) {
            final var a = (DimensionAppender) source;
            dimToAdd = new GridGeometry(a.dimToAdd, dimToAdd);
            source = a.source;
        }
        return new DimensionAppender(source, dimToAdd);
    }

    /**
     * Returns the data type identifying the primitive type used for storing sample values in each band.
     */
    @Override
    final DataType getBandType() {
        return source.getBandType();
    }

    /**
     * Creates the grid coverage instance for the converted or packed values.
     * This method is invoked only when first needed, and the result is cached by the caller.
     */
    @Override
    protected GridCoverage createConvertedValues(final boolean converted) {
        try {
            return new DimensionAppender(source.forConvertedValues(converted), dimToAdd);
        } catch (FactoryException e) {
            throw new CannotEvaluateException(e.getMessage(), e);
        }
    }

    /**
     * Creates a new function for computing or interpolating sample values at given locations.
     * This implementation returns the evaluator of the source coverage on the assumption that
     * it should be able to do dimensionality reduction of the coordinates given to it.
     */
    @Override
    public Evaluator evaluator() {
        return source.evaluator();
    }

    /**
     * Returns a two-dimensional slice of grid data as a rendered image.
     *
     * @param  sliceExtent  a subspace of this grid coverage where all dimensions except two have a size of 1 cell.
     *         May be {@code null} if this grid coverage has only two dimensions with a size greater than 1 cell.
     * @return the grid slice as a rendered image. Image location is relative to {@code sliceExtent}.
     */
    @Override
    public RenderedImage render(GridExtent sliceExtent) {
        if (sliceExtent != null) {
            final int sourceDim = source.getGridGeometry().getDimension();
            final int dimension = dimToAdd.getDimension() + sourceDim;
            ArgumentChecks.ensureDimensionMatches("sliceExtent", dimension, sliceExtent);
            for (int i=sourceDim; i<dimension; i++) {
                final long size = sliceExtent.getSize(i);
                if (size != 1) {
                    throw new SubspaceNotSpecifiedException(Resources.format(Resources.Keys.NoNDimensionalSlice_3,
                                sourceDim, sliceExtent.getAxisIdentification(i,i), Numerics.toUnsignedDouble(size)));
                }
                if (dimToAdd.extent != null) {
                    final long actual = sliceExtent.getLow(i);
                    final long expected = dimToAdd.extent.getLow(i - sourceDim);
                    if (actual != expected) {
                        throw new DisjointExtentException(sliceExtent.getAxisIdentification(i,i), expected, expected, actual, actual);
                    }
                }
            }
            sliceExtent = sliceExtent.selectDimensions(ArraysExt.range(0, sourceDim));
        }
        return source.render(sliceExtent);
    }
}
