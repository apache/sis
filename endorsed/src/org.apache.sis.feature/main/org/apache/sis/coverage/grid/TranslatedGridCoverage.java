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


/**
 * A grid coverage with the same data as the source coverage,
 * with only a translation applied on grid coordinates.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class TranslatedGridCoverage extends DerivedGridCoverage {
    /**
     * The translation to apply on the argument given to {@link #render(GridExtent)}
     * before to delegate to the source. This is the conversion from this coverage to
     * the source coverage.
     */
    private final long[] translation;

    /**
     * Constructs a new grid coverage which will delegate the rendering operation to the given source.
     * This coverage will take the same sample dimensions as the source.
     * The {@code domain} size must be the same as the source grid geometry size.
     *
     * @param  source       the source to which to delegate rendering operations.
     * @param  domain       the grid extent, CRS and conversion from cell indices to CRS.
     * @param  translation  translation to apply on the argument given to {@link #render(GridExtent)}.
     */
    private TranslatedGridCoverage(final GridCoverage source, final GridGeometry domain, final long[] translation) {
        super(source, domain);
        this.translation = translation;
    }

    /**
     * Returns a grid coverage which will use the {@code domain} grid geometry.
     * This coverage will take the same sample dimensions as the source.
     *
     * <p>If {@code domain} is non-null, then it should have the same size as the source grid geometry size.
     * If this is not the case, then this method returns {@code null}.</p>
     *
     * @param  source       the source to which to delegate rendering operations.
     * @param  domain       the geometry of the grid coverage to return, or {@code null} for automatic.
     * @param  translation  translation to apply on the argument given to {@link #render(GridExtent)}.
     * @return the coverage, or {@code null} if {@code domain} is non-null but does not have the expected size.
     *         May be the {@code source} returned as-is.
     */
    static GridCoverage create(GridCoverage source, GridGeometry domain, long[] translation,
                               final boolean allowSourceReplacement)
    {
        if (allowSourceReplacement) {
            while (source instanceof TranslatedGridCoverage) {
                final var tc = (TranslatedGridCoverage) source;
                final long[] shifted = tc.translation.clone();
                long tm = 0;
                for (int i = Math.min(shifted.length, translation.length); --i >= 0;) {
                    shifted[i] = Math.addExact(shifted[i], translation[i]);
                    tm |= translation[i];
                }
                if (tm == 0) return tc;         // All translation terms are zero.
                translation = shifted;
                source = tc.source;
            }
        }
        final GridGeometry gridGeometry = source.getGridGeometry();
        if (domain == null) {
            domain = gridGeometry.shiftGrid(translation);
        } else if (!domain.getExtent().isSameSize(gridGeometry.extent)) {
            return null;
        }
        if (domain.equals(gridGeometry)) {
            return source;                  // All (potentially updated) translation terms are zero.
        }
        return new TranslatedGridCoverage(source, domain, translation);
    }

    /**
     * Returns a grid coverage that contains real values or sample values, depending if {@code converted}
     * is {@code true} or {@code false} respectively. This method delegates to the source and wraps the
     * result in a {@link TranslatedGridCoverage} with the same {@linkplain #translation}.
     */
    @Override
    protected final GridCoverage createConvertedValues(final boolean converted) {
        final GridCoverage cs = source.forConvertedValues(converted);
        return (cs == source) ? this : new TranslatedGridCoverage(cs, gridGeometry, translation);
    }

    /**
     * Returns a two-dimensional slice of grid data as a rendered image.
     * This method translates the {@code sliceExtent} argument, then delegates to the {@linkplain #source source}.
     * It is okay to use the source result as-is because image coordinates are relative to the request;
     * the rendered image shall not be translated.
     */
    @Override
    public RenderedImage render(GridExtent sliceExtent) {
        if (sliceExtent == null) {
            sliceExtent = gridGeometry.extent;
        }
        if (sliceExtent != null) {
            sliceExtent = sliceExtent.translate(translation);
        }
        return source.render(sliceExtent);
    }
}
