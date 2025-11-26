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

import java.util.Objects;
import java.io.Serializable;
import org.opengis.geometry.Envelope;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * High-level description about how a grid is orientated relative to the CRS axes. The orientation of a grid
 * is closely related to the {@linkplain GridGeometry#getGridToCRS(PixelInCell) grid to CRS} transform.
 * For example, the conversion from grid coordinates to CRS coordinates may flip the <var>y</var> axis
 * (grid coordinates increasing toward down on screen), or may swap <var>x</var> and <var>y</var> axes, <i>etc.</i>
 * The constants enumerated in this class cover only a few common cases where the grid is
 * <a href="https://en.wikipedia.org/wiki/Axis-aligned_object">axis-aligned</a> with the <abbr>CRS</abbr>.
 *
 * <h4>Custom orientations</h4>
 * For creating a custom orientations, one of the constants defined in this class can be used as a starting point.
 * Then, the {@link #flipGridAxis(int)}, {@link #useVariantOfCRS(AxesConvention)} or {@link #canReorderGridAxis(boolean)}
 * methods can be invoked.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 *
 * @see GridGeometry#GridGeometry(GridExtent, Envelope, GridOrientation)
 *
 * @since 1.1
 */
public final class GridOrientation implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1354776950822418237L;

    /**
     * The {@code gridToCRS} transform applies only scales and translations (no axis flip or swap).
     * Moving along the grid axis in dimension <var>i</var> causes a displacement along the CRS axis
     * in the same dimension <var>i</var>.
     * In matrix terms all non-zero coefficients are on the diagonal or in the translation column.
     * For example, in the three-dimensional case:
     *
     * <pre class="math">
     *   ┌                ┐
     *   │ Sx  0   0   Tx │
     *   │ 0   Sy  0   Ty │
     *   │ 0   0   Sz  Tz │
     *   │ 0   0   0   1  │
     *   └                ┘</pre>
     *
     * with
     * <var>S<sub>x</sub></var> &gt; 0,
     * <var>S<sub>y</sub></var> &gt; 0 and
     * <var>S<sub>z</sub></var> &gt; 0.
     */
    public static final GridOrientation HOMOTHETY = new GridOrientation(0, null, false);

    /**
     * The {@code gridToCRS} transform applies scales and translations with a flip of the second axis (<var>y</var>).
     * This is equivalent to {@code HOMOTHETY.flipGridAxis(1)}; i.e.
     * this is the same kind of conversion than {@link #HOMOTHETY} except that the <var>S<sub>y</sub></var> term in
     * the matrix is replaced by −<var>S<sub>y</sub></var> and the <var>T<sub>y</sub></var> term has a different value.
     * For example in the three-dimensional case, the {@code gridToCRS} transform is:
     *
     * <pre class="math">
     *   ┌                 ┐
     *   │ Sx  0   0   Tx  │
     *   │ 0  −Sy  0   Ty′ │
     *   │ 0   0   Sz  Tz  │
     *   │ 0   0   0   1   │
     *   └                 ┘</pre>
     *
     * <h4>When to use</h4>
     * {@code REFLECTION_Y} is commonly used when the grid is a {@link java.awt.image.RenderedImage}.
     * By contrast, an {@link #HOMOTHETY} transform often results in <var>y</var> axis oriented toward up,
     * instead of down as commonly expected with rendered images.
     * This {@code REFLECTION_Y} value matches the common usage for grids backed by images.
     *
     * @see #flipGridAxis(int)
     */
    public static final GridOrientation REFLECTION_Y = new GridOrientation(2, null, false);

    /**
     * CRS axes are reordered and oriented toward directions commonly used for displaying purpose.
     * This is equivalent to {@code REFLECTION_Y.useVariantOfCRS(AxesConvention.DISPLAY_ORIENTED)}.
     * {@link GridGeometry}s created with this orientation have properties computed as below:
     *
     * <ul>
     *   <li>The {@link GridExtent} specified by user (never modified).</li>
     *   <li>An envelope initialized to user-specified envelope (potentially modified as described below).</li>
     *   <li>A {@code gridToCRS} initialized to {@link #REFLECTION_Y} (potentially modified as described below).</li>
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
     * <pre class="math">
     *   ┌                  ┐
     *   │  0   Sx  0   Tx  │
     *   │ −Sy  0   0   Ty′ │
     *   │  0   0   Sz  Tz  │
     *   │  0   0   0   1   │
     *   └                  ┘</pre>
     *
     * <h4>When to use</h4>
     * This orientation can be used for deriving a coordinate reference system with the
     * (<var>longitude</var>, <var>latitude</var>) or (<var>x</var>,<var>y</var>) axis order,
     * but without altering grid axes order.
     *
     * <h4>Alternative</h4>
     * {@code DISPLAY.canReorderGridAxis(true)} is an alternative where grid axes get the same reordering as CRS axes.
     * Consequently, the {@link GridExtent} may be different then the specified extent
     * but the {@code gridToCRS} transform always has the form shown in {@link #REFLECTION_Y}.
     * This alternative can be used for deriving a coordinate reference system with the
     * (<var>longitude</var>, <var>latitude</var>) or (<var>x</var>,<var>y</var>) axis order,
     * and modify grid cell layout (i.e. replace the {@link GridExtent} instance)
     * in way that allows {@link java.awt.image.RenderedImage} to appear with expected orientation.
     *
     * @see #useVariantOfCRS(AxesConvention)
     * @see AxesConvention#DISPLAY_ORIENTED
     */
    public static final GridOrientation DISPLAY = new GridOrientation(2, AxesConvention.DISPLAY_ORIENTED, false);

    /**
     * Unknown image orientation. The {@linkplain GridGeometry#getGridToCRS(PixelInCell) grid to CRS}
     * transforms inferred from this orientation will be null.
     * All methods in this class invoked on the {@code UNKNOWN} instance will return {@code UNKNOWN}.
     *
     * @since 1.6
     */
    public static final GridOrientation UNKNOWN = new GridOrientation(0, null, true);

    /**
     * Set of grid axes to reverse, as a bit mask. For any dimension <var>i</var>, the bit
     * at {@code 1L << i} is set to 1 if the grid axis at that dimension should be flipped.
     * This is the argument to give in calls to {@link GridExtent#cornerToCRS(Envelope, long, int[])}.
     *
     * @see #flipGridAxis(int)
     */
    final long flippedAxes;

    /**
     * If the user-specified CRS should be substituted by a variant of that CRS, the variant to use.
     * Otherwise {@code null}. If non-null, either the {@code gridToCRS} matrix may be non-diagonal
     * or the {@link GridExtent} axes may be ordered, depending on {@link #canReorderGridAxis} value.
     *
     * @see #useVariantOfCRS(AxesConvention)
     */
    final AxesConvention crsVariant;

    /**
     * Whether {@link GridExtent} can be rewritten with a different axis order
     * for matching the <abbr>CRS</abbr> axis order specified by {@link #crsVariant}.
     * If {@code false}, then axis order changes will be handled in the {@code gridToCRS} transform instead.
     *
     * @see #canReorderGridAxis(boolean)
     */
    final boolean canReorderGridAxis;

    /**
     * Creates a new enumeration value.
     */
    private GridOrientation(final long flippedAxes, final AxesConvention crsVariant, final boolean canReorderGridAxis) {
        this.flippedAxes = flippedAxes;
        this.crsVariant  = crsVariant;
        this.canReorderGridAxis = canReorderGridAxis;
    }

    /**
     * Reverses axis direction in the specified grid dimension.
     * For example if grid indices are (<var>column</var>, <var>row</var>),
     * then {@code flipGridAxis(1)} will reverse the direction of rows axis.
     * Invoking this method a second time for the same dimension will cancel the flipping.
     *
     * @param  dimension  index of the dimension in the grid on which to apply direction reversal.
     * @return a grid orientation equals to this one except for the axis flip in specified dimension.
     *
     * @see #REFLECTION_Y
     * @see GridCoverageBuilder#flipGridAxis(int)
     */
    public GridOrientation flipGridAxis(final int dimension) {
        ArgumentChecks.ensurePositive("dimension", dimension);
        if (dimension >= Long.SIZE) {
            throw new ArithmeticException(Errors.format(Errors.Keys.ExcessiveNumberOfDimensions_1, dimension + 1));
        }
        if (this == UNKNOWN) {
            return this;
        }
        return new GridOrientation(flippedAxes ^ (1L << dimension), crsVariant, canReorderGridAxis);
    }

    /**
     * Substitutes the user-specified CRS by a variant of it, for example with different axis order.
     * If the CRS axis order changed as a result of this substitution, then:
     *
     * <ul>
     *   <li>Order of envelope coordinates are changed accordingly.</li>
     *   <li>CRS axis order change is mapped to the grid in one of the following ways:
     *     <ul>
     *       <li>If {@code canReorderGridAxis(true)} has been invoked, then the same change is applied
     *           on grid axis order. Consequently, grid axes and CRS axes stay in the same order,
     *           but the resulting {@link GridExtent} may be different than the specified one.</li>
     *       <li>Otherwise {@link GridExtent} stay unchanged and axis order change is handled in the
     *           {@code gridToCRS} transform instead.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <h4>Limitations</h4>
     * Current implementation accepts only axis order changes and direction changes.
     * The units of measurement changes are not yet supported.
     * Consequently, {@link AxesConvention#NORMALIZED} is not accepted.
     *
     * @param  variant  the kind of substitution to apply on CRS, or {@code null} if none.
     * @return a grid orientation equals to this one except that it uses the specified CRS variant.
     *
     * @see #DISPLAY
     */
    public GridOrientation useVariantOfCRS(final AxesConvention variant) {
        if (variant == crsVariant || this == UNKNOWN) {
            return this;
        }
        if (variant == AxesConvention.NORMALIZED || variant == AxesConvention.ORIGINAL) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedArgumentValue_1, variant));
        }
        return new GridOrientation(flippedAxes, variant, canReorderGridAxis);
    }

    /**
     * Specifies whether a change of CRS axis order should be accompanied by an equivalent change of grid axis order.
     * A value of {@code true} implies that user-specified {@link GridExtent} may be replaced by a different extent.
     * If {@code false} (the default), then axis order changes will be handled in the {@code gridToCRS} transform
     * instead.
     *
     * @param  enabled  whether changes of CRS axis order should be reflected by changes of grid axis order.
     * @return a grid orientation equals to this one except that it has the specified flag.
     */
    public GridOrientation canReorderGridAxis(final boolean enabled) {
        if (enabled == canReorderGridAxis || this == UNKNOWN) {
            return this;
        }
        return new GridOrientation(flippedAxes, crsVariant, enabled);
    }

    /**
     * Returns whether this object is equal to the given object.
     *
     * @param  other  the other object to compare with this object.
     * @return whether the two objects are equal.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof GridOrientation) {
            final var that = (GridOrientation) other;
            return flippedAxes == that.flippedAxes &&
                   crsVariant  == that.crsVariant  &&
                   canReorderGridAxis == that.canReorderGridAxis;
        }
        return false;
    }

    /**
     * Returns a hash code value for this grid orientation.
     */
    @Override
    public int hashCode() {
        return Long.hashCode(flippedAxes) + Objects.hashCode(crsVariant) + Boolean.hashCode(canReorderGridAxis);
    }

    /**
     * Returns a string representation of this grid orientation.
     * This is for debugging purpose and may change in any future version.
     */
    @Override
    public String toString() {
        if (this == UNKNOWN) {
            return "UNKNOWN";
        }
        final var buffer = new StringBuilder(getClass().getSimpleName()).append('[');
        String separator = "";
        if (flippedAxes != 0) {
            buffer.append("flip={");
            long f = flippedAxes;
            do {
                final long i = Long.numberOfTrailingZeros(f);
                buffer.append(separator).append(i);
                f &= ~(1L << i);
                separator = ", ";
            } while (f != 0);
            buffer.append('}');
        }
        if (crsVariant != null) {
            buffer.append(separator).append("crs=").append(crsVariant);
            if (canReorderGridAxis) {
                buffer.append(" with grid axis reordering");
            }
        }
        return buffer.append(']').toString();
    }
}
