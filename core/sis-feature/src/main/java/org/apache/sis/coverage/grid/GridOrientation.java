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

import org.opengis.geometry.Envelope;
import org.opengis.referencing.datum.PixelInCell;


/**
 * High-level description about how a grid is orientated relative to the CRS axes.
 * This is determined by the {@linkplain GridGeometry#getGridToCRS(PixelInCell) grid to CRS} transform.
 * For example conversion from grid coordinates to CRS coordinates may flip the <var>y</var> axis
 * (grid coordinates increasing toward down on screen), or may swap <var>x</var> and <var>y</var> axes, <i>etc.</i>
 * The possibilities are infinite; this enumeration covers only a few common types.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public enum GridOrientation {
    /**
     * The {@code gridToCRS} transform applies only scales and translations (no axis flip or swap).
     * Moving along the grid axis in dimension <var>i</var> causes a displacement along the CRS axis
     * in the same dimension <var>i</var>. In matrix terms all coefficients on the diagonal are positives
     * (restricted to 1 on the last row), the translation terms can be anything, and all other terms are zero.
     * For example in the three-dimensional case:
     *
     * {@preformat math
     *   ┌                ┐
     *   │ Sx  0   0   Tx │
     *   │ 0   Sy  0   Ty │
     *   │ 0   0   Sz  Tz │
     *   │ 0   0   0   1  │
     *   └                ┘
     * }
     *
     * with
     * <var>S<sub>x</sub></var> &gt; 0,
     * <var>S<sub>y</sub></var> &gt; 0 and
     * <var>S<sub>z</sub></var> &gt; 0.
     */
    HOMOTHETY(0),

    /**
     * The {@code gridToCRS} transform applies scales and translations with a flip of the second axis (<var>y</var>).
     * This is the same kind of conversion than {@link #HOMOTHETY} except that the <var>S<sub>y</sub></var> term in
     * the matrix is replaced by −<var>S<sub>y</sub></var>.
     *
     * <p>{@code REFLECTION_Y} is commonly used when the grid is a {@link java.awt.image.RenderedImage}.
     * By contrast, an {@link #HOMOTHETY} transform often results in <var>y</var> axis oriented toward up,
     * instead of down as commonly expected with rendered images.
     * This {@code REFLECTION_Y} value matches the common usage for grids backed by images.</p>
     */
    REFLECTION_Y(2);

    /*
     * TODO: add DISPLAY. The difference compared to REFLECTION_Y is that it may change CRS axes.
     */

    /**
     * Bitmask of axes to flip.
     * This is the argument to give in calls to {@link GridExtent#cornerToCRS(Envelope, long, int[])}.
     */
    final int flip;

    /**
     * Creates a new enumeration.
     */
    private GridOrientation(final int flip) {
        this.flip = flip;
    }
}
