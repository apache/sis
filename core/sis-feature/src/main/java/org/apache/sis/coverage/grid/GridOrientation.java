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
import org.apache.sis.referencing.cs.AxesConvention;


/**
 * High-level description about how a grid is orientated relative to the CRS axes.
 * This is determined by the {@linkplain GridGeometry#getGridToCRS(PixelInCell) grid to CRS} transform.
 * For example conversion from grid coordinates to CRS coordinates may flip the <var>y</var> axis
 * (grid coordinates increasing toward down on screen), or may swap <var>x</var> and <var>y</var> axes, <i>etc.</i>
 * The possibilities are infinite; this enumeration covers only a few common types where the grid is
 * <a href="https://en.wikipedia.org/wiki/Axis-aligned_object">axis-aligned</a> with the CRS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see GridGeometry#GridGeometry(GridExtent, Envelope, GridOrientation)
 *
 * @since 1.1
 * @module
 */
public enum GridOrientation {
    /**
     * The {@code gridToCRS} transform applies only scales and translations (no axis flip or swap).
     * Moving along the grid axis in dimension <var>i</var> causes a displacement along the CRS axis
     * in the same dimension <var>i</var>.
     * In matrix terms all non-zero coefficients are on the diagonal or in the translation column.
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
     * the matrix is replaced by −<var>S<sub>y</sub></var> and the <var>T<sub>y</sub></var> term has a different value.
     * For example in the three-dimensional case, the {@code gridToCRS} transform is:
     *
     * {@preformat math
     *   ┌                 ┐
     *   │ Sx  0   0   Tx  │
     *   │ 0  −Sy  0   Ty′ │
     *   │ 0   0   Sz  Tz  │
     *   │ 0   0   0   1   │
     *   └                 ┘
     * }
     *
     * {@code REFLECTION_Y} is commonly used when the grid is a {@link java.awt.image.RenderedImage}.
     * By contrast, an {@link #HOMOTHETY} transform often results in <var>y</var> axis oriented toward up,
     * instead of down as commonly expected with rendered images.
     * This {@code REFLECTION_Y} value matches the common usage for grids backed by images.
     */
    REFLECTION_Y(2),

    /**
     * CRS axes are reordered and oriented toward directions commonly used for displaying purpose.
     * {@link GridGeometry}s created with this orientation have properties computed as below:
     *
     * <ul>
     *   <li>The {@link GridExtent} specified by user (never modified).</li>
     *   <li>An envelope initialized to user-specified envelope (potentially modified below).</li>
     *   <li>A {@code gridToCRS} initialized to {@link #REFLECTION_Y} (potentially modified below).</li>
     *   <li>The {@linkplain AxesConvention#DISPLAY_ORIENTED display oriented} variant of the CRS specified by user.</li>
     *   <li>If above CRS variant is same as user-specified CRS, we are done. Otherwise:
     *     <ul>
     *       <li>Envelope dimensions are reordered to match axis order in above CRS variant
     *           Those changes are applied on a copy of user-specified envelope.</li>
     *       <li>The {@code gridToCRS} transform is amended with the same reordering (applied on columns)
     *           as for envelope.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * Below is an example of {@code gridToCRS} transform obtained when the display-oriented CRS variant
     * is different than the user-specified CRS (if those CRSs are equal, then the transform is rather
     * like the one shown in {@link #REFLECTION_Y}):
     *
     * {@preformat math
     *   ┌                  ┐
     *   │  0   Sx  0   Tx  │
     *   │ −Sy  0   0   Ty′ │
     *   │  0   0   Sz  Tz  │
     *   │  0   0   0   1   │
     *   └                  ┘
     * }
     *
     * This orientation can be used for deriving a coordinate reference system with the
     * <i>(<var>longitude</var>, <var>latitude</var>)</i> or <i>(<var>x</var>,<var>y</var>)</i> axis order,
     * but without altering grid axes order.
     *
     * @see AxesConvention#DISPLAY_ORIENTED
     */
    DISPLAY(2),

    /**
     * CRS and grid axes are reordered and oriented toward directions commonly used for displaying purpose.
     * This is similar to {@link #DISPLAY} except that {@link GridExtent} axes get the same reordering than CRS axes.
     * Consequently the {@code gridToCRS} transform always has the form shown in {@link #REFLECTION_Y}.
     *
     * <p>This orientation can be used for deriving a coordinate reference system with the
     * <i>(<var>longitude</var>, <var>latitude</var>)</i> or <i>(<var>x</var>,<var>y</var>)</i> axis order,
     * and modify grid cell layout (i.e. replace the {@link GridExtent} instance)
     * in way that allows {@link java.awt.image.RenderedImage} to appear with expected orientation.</p>
     */
    DISPLAY_GRID(2);

    /**
     * Bitmask of axes to flip.
     * This is the argument to give in calls to {@link GridExtent#cornerToCRS(Envelope, long, int[])}.
     */
    final int flip;

    /**
     * Creates a new enumeration value.
     */
    private GridOrientation(final int flip) {
        this.flip = flip;
    }
}
