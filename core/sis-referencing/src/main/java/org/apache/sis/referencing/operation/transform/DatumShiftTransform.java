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
import java.util.Objects;


/**
 * Base class of transforms performing datum shifts.
 * Some implementations are backed by a {@link DatumShiftGrid}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public abstract class DatumShiftTransform extends AbstractMathTransform implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4492222496475405226L;

    /**
     * The parameters used for creating this transformation.
     * They are used for formatting <cite>Well Known Text</cite> (WKT) and error messages.
     *
     * @see #getContextualParameters()
     */
    final ContextualParameters context;

    /**
     * The grid of datum shifts from source datum to target datum, or {@code null} if none.
     */
    final DatumShiftGrid<?,?> grid;

    /**
     * Creates a datum shift transform. It is caller responsibility to initialize the {@link #context} parameters.
     *
     * @param descriptor  The contextual parameter descriptor.
     * @param grid        Interpolation grid in geocentric coordinates, or {@code null} if none.
     */
    DatumShiftTransform(ParameterDescriptorGroup descriptor, int srcSize, int tgtSize, final DatumShiftGrid<?,?> grid) {
        context = new ContextualParameters(descriptor, srcSize, tgtSize);
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
     * Returns the internal parameter values of this transform.
     * This method is mostly for {@linkplain org.apache.sis.io.wkt.Convention#INTERNAL debugging purposes}
     * since the isolation of non-linear parameters in this class is highly implementation dependent.
     * Most GIS applications will instead be interested in the {@linkplain #getContextualParameters()
     * contextual parameters}.
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
