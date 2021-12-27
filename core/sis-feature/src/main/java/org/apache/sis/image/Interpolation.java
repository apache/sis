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

import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.awt.image.IndexColorModel;
import java.nio.DoubleBuffer;


/**
 * Algorithm for image interpolation (resampling). Interpolations are performed by sampling on a regular grid
 * of pixels using a local neighborhood. The sampling is performed by the {@link ResampledImage} class, which
 * gives the sample values to the {@code interpolate(…)} method of this interpolation.
 *
 * <p>All methods in this class shall be safe for concurrent use in multi-threading context.
 * For example interpolations may be executed in a different thread for each tile in an image.</p>
 *
 * <p>This class is designed for interpolations in a two-dimensional space only.</p>
 *
 * @author  Rémi Marechal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
public abstract class Interpolation {
    /**
     * Creates a new interpolation.
     */
    protected Interpolation() {
    }

    /**
     * Returns the size of the area over which the resampling function needs to provide values.
     * Common values are:
     *
     * <table class="sis">
     *   <caption>Common support sizes</caption>
     *   <tr><th>Interpolation</th>    <th>Width</th> <th>Height</th></tr>
     *   <tr><td>Nearest-neighbor</td> <td>1</td>     <td>1</td></tr>
     *   <tr><td>Bilinear</td>         <td>2</td>     <td>2</td></tr>
     *   <tr><td>Bicubic</td>          <td>4</td>     <td>4</td></tr>
     *   <tr><td>Lanczos</td>          <td>4</td>     <td>4</td></tr>
     * </table>
     *
     * @return number of sample values required for interpolations.
     */
    public abstract Dimension getSupportSize();

    /**
     * Interpolates sample values for all bands using the given pixel values in local neighborhood.
     * The given {@code source} is a buffer with the number of elements shown below, where
     * <var>support width</var> and <var>support height</var> are given by {@link #getSupportSize()}:
     *
     * <blockquote>
     * <var>(number of bands)</var> × <var>(support width)</var> × <var>(support height)</var>
     * </blockquote>
     *
     * Values in {@code source} buffer are always given with band index varying fastest, then column index,
     * then row index. Columns are traversed from left to right and rows are traversed from top to bottom
     * ({@link org.opengis.coverage.grid.SequenceType#LINEAR} iteration order).
     *
     * <p>The interpolation point is in the middle. For example if the {@linkplain #getSupportSize() support size}
     * is 4×4 pixels, then the interpolation point is the dot below and the fractional coordinates are relative to
     * the horizontal and vertical lines drawn below. This figure is for an image with only one band, otherwise all
     * indices between brackets would need to be multiplied by {@code numBands}.</p>
     *
     * {@preformat text
     *   s[0]   s[1]   s[2]   s[3]
     *
     *   s[4]   s[5]───s[6]   s[7]  ← yfrac = 0
     *           │   ●              ← yfrac given
     *   s[8]   s[9]   s[10]  s[11] ← yfrac = 1
     *
     *   s[12]  s[13]  s[14]  s[15]
     *               ↑
     *             xfrac
     * }
     *
     * On output, this method shall write the interpolation results as {@code numBands} consecutive
     * values in the supplied {@code writeTo} array, starting at {@code writeToOffset} index.
     * This method should not modify the buffer position (use {@link DoubleBuffer#mark()} and
     * {@link DoubleBuffer#reset() reset()} if needed).
     *
     * @param  source         pixel values from the source image to use for interpolation.
     * @param  numBands       number of bands. This is the number of values to put in the {@code writeTo} array.
     * @param  xfrac          the X subsample position, usually (but not always) in the range [0 … 1).
     * @param  yfrac          the Y subsample position, usually (but not always) in the range [0 … 1).
     * @param  writeTo        the array where this method shall write interpolated values.
     * @param  writeToOffset  index of the first value to put in the {@code writeTo} array.
     */
    public abstract void interpolate(DoubleBuffer source, int numBands, double xfrac, double yfrac, double[] writeTo, int writeToOffset);

    /**
     * Returns {@link #NEAREST} if interpolations on the given image should be restricted to nearest-neighbor.
     * If the given image uses an index color model, interpolating the indexed values does not produce the
     * expected colors. Safest approach is to disable completely interpolations in that case.
     *
     * <div class="note"><b>Note:</b>
     * we could interpolate if we knew that all index values, without exception (i.e. no index for missing values),
     * are related to measurements by a linear function. In practice it rarely happens, because there is usually
     * at least one index value reserved for missing values. Scientific data in SIS are usually stored as floating
     * point type (with missing values mapped to NaN), which can not be associated to {@link IndexColorModel}.
     * For now we do not try to perform a more sophisticated detection of which interpolations are allowed,
     * but a future SIS version may revisit this policy if needed.</div>
     *
     * @return {@link #NEAREST} if interpolations should be restricted to nearest-neighbor, or {@code this} otherwise.
     */
    Interpolation toCompatible(final RenderedImage source) {
        return (source.getColorModel() instanceof IndexColorModel) ? NEAREST : this;
    }

    /**
     * A nearest-neighbor interpolation using 1×1 pixel.
     */
    public static final Interpolation NEAREST = new Interpolation() {
        /** Interpolation name for debugging purpose. */
        @Override public String toString() {
            return "NEAREST";
        }

        /** Size of the area over which to provide values. */
        @Override public Dimension getSupportSize() {
            return new Dimension(1,1);
        }

        /** Applies nearest-neighbor interpolation on 1×1 window. */
        @Override public void interpolate(final DoubleBuffer source, final int numBands,
                final double xfrac, final double yfrac, final double[] writeTo, int writeToOffset)
        {
            // TODO: use `get(position(), …)` with JDK13 for avoiding mark/reset.
            source.mark();
            source.get(writeTo, writeToOffset, numBands);
            source.reset();
        }

        /** This interpolation never need to be disabled. */
        @Override Interpolation toCompatible(final RenderedImage source) {
            return this;
        }
    };

    /**
     * A bilinear interpolation using 2×2 pixels.
     * If the interpolation result is NaN, this method fallbacks on nearest-neighbor.
     */
    public static final Interpolation BILINEAR = new Interpolation() {
        /** Interpolation name for debugging purpose. */
        @Override public String toString() {
            return "BILINEAR";
        }

        /** Size of the area over which to provide values. */
        @Override public Dimension getSupportSize() {
            return new Dimension(2,2);
        }

        /** Applies bilinear interpolation on a 2×2 window. */
        @Override public void interpolate(final DoubleBuffer source, final int numBands,
                final double xfrac, final double yfrac, final double[] writeTo, int writeToOffset)
        {
            final double mx = (1 - xfrac);
            final double my = (1 - yfrac);
            for (int b=0; b<numBands; b++) {
                int p = source.position() + b;
                double y = (source.get(p            )*mx + source.get(p += numBands)*xfrac) * my
                         + (source.get(p += numBands)*mx + source.get(p +  numBands)*xfrac) * yfrac;
                if (Double.isNaN(y)) {
                    // Fallback on nearest-neighbor.
                    p = source.position() + b;
                    if (xfrac >= 0.5) p += numBands;
                    if (yfrac >= 0.5) p += numBands*2;
                    y = source.get(p);
                }
                writeTo[writeToOffset++] = y;
            }
        }
    };

    /**
     * Lanczos interpolation for photographic images.
     * This interpolation is not recommended for images that may contain NaN values.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Lanczos_resampling">Lanczos resampling on Wikipedia</a>
     */
    public static final Interpolation LANCZOS = new LanczosInterpolation(3);
}
