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
package org.apache.sis.referencing.operation.transform;

import java.io.Serializable;
import javax.measure.unit.Unit;
import javax.measure.quantity.Length;
import javax.measure.converter.UnitConverter;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.geometry.MismatchedDimensionException;
import org.apache.sis.referencing.datum.DatumShiftGrid;
import org.apache.sis.internal.referencing.provider.Molodensky;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Debug;

// Branch-specific imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Transforms between two CRS (usually geographic) based on different datum. A datum shift may be needed when two CRS
 * use different {@linkplain org.apache.sis.referencing.datum.DefaultEllipsoid ellipsoids} as approximation of the
 * shape of the Earth. Sometime two CRS use the same ellipsoid but with different anchor point (i.e. their coordinate
 * systems have their origin in different locations).
 *
 * <p>There is many different datum shift methods, ranging from transformations as simple as adding a constant offset
 * to geographic coordinates, to more complex transformations involving conversions to geocentric coordinates and/or
 * interpolations in a {@linkplain DatumShiftGrid datum shift grid}. The simple cases like adding a constant offset
 * are handled by other {@code MathTransform} implementations like {@link LinearTransform}.
 * More complex methods are subclasses of this {@code DatumShiftTransform} base class, but users should not assume
 * that this is the case of every transforms performing a datum shift.</p>
 *
 * <div class="section">Datum shift methods overview</div>
 * The two CRS's ellipsoids have slightly different scale and rotation in space, and their center are located in
 * a slightly different position. Consequently geodetic datum shifts are often approximated by a constant scale,
 * rotation and translation applied on geocentric coordinates. Those approximations are handled in SIS
 * by concatenations of {@link EllipsoidToCentricTransform} with {@link LinearTransform} instead than a specific
 * {@code DatumShiftTransform} subclass.
 *
 * <p>If the geodetic datum shifts is approximated only by a geocentric translation without any scale or rotation,
 * and if an error of a few centimetres it acceptable, then the {@link MolodenskyTransform} subclass can be used
 * as an approximation of the above method. The Molodensky method requires less floating point operations since
 * it applies directly on geographic coordinates, without conversions to geocentric coordinates.</p>
 *
 * <p>Some countries go one step further and allow the above geocentric translations to be non-constant.
 * Instead, a different geocentric translation is interpolated for each geographic input coordinates.
 * This case is handled by the {@link InterpolatedGeocentricTransform} subclass, or its
 * {@link InterpolatedMolodenskyTransform} variant if a few centimetres accuracy lost can be afforded.</p>
 *
 * <p>A simpler alternative to the above is to interpolate translations to apply directly on geographic coordinates.
 * This is the approach taken by NADCON and NTv2 grids.
 * SIS handles those datum shifts with the {@link InterpolatedTransform} subclass.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 *
 * @see DatumShiftGrid
 */
public abstract class DatumShiftTransform extends AbstractMathTransform implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4492222496475405226L;

    /**
     * The parameters used for creating this datum shift. They are used for formatting <cite>Well Known Text</cite> (WKT)
     * and error messages. Subclasses shall not use the values defined in this object for computation purpose, except at
     * construction time.
     *
     * @see #getContextualParameters()
     */
    final ContextualParameters context;

    /**
     * The grid of datum shifts from source datum to target datum, or {@code null} if none.
     */
    final DatumShiftGrid<?,?> grid;

    /**
     * Creates a datum shift transform for direct interpolations in a grid.
     * It is caller responsibility to initialize the {@link #context} parameters.
     *
     * @param descriptor  The contextual parameter descriptor.
     * @param grid        Interpolation grid.
     */
    DatumShiftTransform(ParameterDescriptorGroup descriptor, final DatumShiftGrid<?,?> grid) {
        final int size = grid.getTranslationDimensions() + 1;
        context = new ContextualParameters(descriptor, size, size);
        this.grid = grid;
    }

    /**
     * Creates a datum shift transform for interpolations in geocentric domain.
     * It is caller responsibility to initialize the {@link #context} parameters.
     *
     * @param descriptor  The contextual parameter descriptor.
     * @param grid        Interpolation grid in geocentric coordinates, or {@code null} if none.
     * @param isSource3D  {@code true} if the source coordinates have a height.
     * @param isTarget3D  {@code true} if the target coordinates have a height.
     */
    DatumShiftTransform(final ParameterDescriptorGroup descriptor,
            final boolean isSource3D, final boolean isTarget3D, final DatumShiftGrid<?,?> grid)
    {
        context = new ContextualParameters(descriptor, isSource3D ? 4 : 3, isTarget3D ? 4 : 3);
        this.grid = grid;
    }

    /**
     * Ensures that the {@link #grid} performs geocentric translations in the given units.
     * This method is invoked by constructor for validation of given arguments.
     *
     * <p>This method is defined here in order to ensure a consistent behavior of
     * {@link InterpolatedGeocentricTransform} with {@link InterpolatedMolodenskyTransform}.</p>
     *
     * @param  grid  The grid to validate.
     * @param  unit  The unit of semi-axis length of the <strong>source</strong> ellipsoid.
     * @throws IllegalArgumentException if the given grid is not valid.
     */
    static void ensureGeocentricTranslation(final DatumShiftGrid<?,?> grid, final Unit<Length> unit)
            throws IllegalArgumentException
    {
        final int dim = grid.getTranslationDimensions();
        if (dim != 3) {
            throw new MismatchedDimensionException(Errors.format(Errors.Keys.MismatchedDimension_3, "grid", 3, dim));
        }
        Object unitLabel = "ratio";
        if (grid.isCellValueRatio() || (unitLabel = grid.getTranslationUnit()) != unit) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalUnitFor_2, "translation", unitLabel));
        }
    }

    /**
     * Sets the semi-axis length in the {@link #context} parameters.
     * This is a helper method for constructors in some (not all) subclasses.
     *
     * @param semiMajor The semi-major axis length of the source ellipsoid.
     * @param semiMinor The semi-minor axis length of the source ellipsoid.
     * @param unit      The unit of measurement of source ellipsoid axes.
     * @param target    The target ellipsoid.
     */
    final void setContextParameters(final double semiMajor, final double semiMinor, final Unit<Length> unit, final Ellipsoid target) {
        final UnitConverter c = target.getAxisUnit().getConverterTo(unit);
        context.getOrCreate(Molodensky.SRC_SEMI_MAJOR).setValue(semiMajor, unit);
        context.getOrCreate(Molodensky.SRC_SEMI_MINOR).setValue(semiMinor, unit);
        context.getOrCreate(Molodensky.TGT_SEMI_MAJOR).setValue(c.convert(target.getSemiMajorAxis()), unit);
        context.getOrCreate(Molodensky.TGT_SEMI_MINOR).setValue(c.convert(target.getSemiMinorAxis()), unit);
    }

    /**
     * Returns the internal parameter values of this {@code DatumShiftTransform} instance (ignoring context).
     * The parameters returned by this method do not necessarily describe the whole datum shift process,
     * because {@code DatumShiftTransform} instances are often preceeded and followed by linear conversions.
     * It may be conversions between degrees and radians units, or conversions from geodetic coordinates to grid indices.
     *
     * <div class="note"><b>Example:</b>
     * The chain of transforms of an {@link InterpolatedGeocentricTransform} is:
     * <center>
     *   <table class="compact" style="td {vertical-align: middle}" summary="Decomposition of a datum shift">
     *     <tr style="text-align: center">
     *       <th>Degrees to radians</th><th></th>
     *       <th>{@code DatumShiftTransform} work</th><th></th>
     *       <th>Radians to degrees</th>
     *     </tr><tr>
     *       <td>{@include formulas.html#NormalizeGeographic}</td>
     *       <td>→</td>
     *       <td style="vertical-align: top"><ol style="padding-left: 15px">
     *         <li>Geographic to geocentric conversion</li>
     *         <li>Geocentric interpolation</li>
     *         <li>Geocentric to geographic conversion</li>
     *       </ol></td>
     *       <td>→</td>
     *       <td>{@include formulas.html#DenormalizeGeographic}</td>
     *     </tr>
     *   </table>
     * </center></div>
     *
     * This method returns the parameters for the part in the middle of above example.
     * The content of this part is highly implementation-dependent and used mostly for
     * {@linkplain org.apache.sis.io.wkt.Convention#INTERNAL debugging purposes}.
     * The parameters that describe the process as a whole are rather given by {@link #getContextualParameters()}.
     *
     * @return The internal parameter values for this transform.
     */
    @Debug
    @Override
    public ParameterValueGroup getParameterValues() {
        return context;     // Overridden by some subclasses.
    }

    /**
     * Returns the parameters used for creating the complete transformation. Those parameters describe a sequence
     * of <cite>normalize</cite> → {@code this} → <cite>denormalize</cite> transforms, <strong>not</strong>
     * including {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes axis swapping}.
     * Those parameters are used for formatting <cite>Well Known Text</cite> (WKT) and error messages.
     *
     * @return The parameters values for the sequence of
     *         <cite>normalize</cite> → {@code this} → <cite>denormalize</cite> transforms.
     */
    @Override
    protected ContextualParameters getContextualParameters() {
        return context;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    protected int computeHashCode() {
        return super.computeHashCode() + Objects.hashCode(grid);
    }

    /**
     * Compares the specified object with this math transform for equality.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        return super.equals(object, mode) && Objects.equals(grid, ((DatumShiftTransform) object).grid);
    }
}
