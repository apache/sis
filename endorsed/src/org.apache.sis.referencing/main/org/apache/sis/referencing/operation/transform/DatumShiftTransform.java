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

import java.util.Objects;
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.quantity.Length;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.Matrix;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.referencing.datum.DatumShiftGrid;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.provider.Molodensky;
import org.apache.sis.measure.Units;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Debug;


/**
 * Transforms between two CRS (usually geographic) based on different datum. A datum shift may be needed when two CRS
 * use different {@linkplain org.apache.sis.referencing.datum.DefaultEllipsoid ellipsoids} as approximation of the
 * shape of the Earth. Sometimes two CRS use the same ellipsoid but with different anchor point (i.e. their coordinate
 * systems have their origin in different locations).
 *
 * <p>There is many different datum shift methods, ranging from transformations as simple as adding a constant offset
 * to geographic coordinates, to more complex transformations involving conversions to geocentric coordinates and/or
 * interpolations in a {@linkplain DatumShiftGrid datum shift grid}. The simple cases like adding a constant offset
 * are handled by other {@code MathTransform} implementations like {@link LinearTransform}.
 * More complex methods are subclasses of this {@code DatumShiftTransform} base class, but users should not assume
 * that this is the case of every transforms performing a datum shift.</p>
 *
 * <h2>Datum shift methods overview</h2>
 * The two CRS's ellipsoids have slightly different scale and rotation in space, and their center are located in
 * a slightly different position. Consequently, geodetic datum shifts are often approximated by a constant scale,
 * rotation and translation applied on geocentric coordinates. Those approximations are handled in SIS
 * by concatenations of {@link EllipsoidToCentricTransform} with {@link LinearTransform} instead of a specific
 * {@code DatumShiftTransform} subclass.
 *
 * <p>If the geodetic datum shifts is approximated only by a geocentric translation without any scale or rotation,
 * and if an error of a few centimetres it acceptable, then the {@link MolodenskyTransform} subclass can be used
 * as an approximation of the above method. The Molodensky method requires less floating point operations since
 * it applies directly on geographic coordinates, without conversions to geocentric coordinates.</p>
 *
 * <p>Some countries go one step further and allow the above geocentric translations to be non-constant.
 * Instead, a different geocentric translation is interpolated for each geographic input coordinates.
 * This case is handled by the {@link InterpolatedGeocentricTransform} subclass.</p>
 *
 * <p>A simpler alternative to the above is to interpolate translations to apply directly on geographic coordinates.
 * This is the approach taken by NADCON and NTv2 grids.
 * SIS handles those datum shifts with the {@link InterpolatedTransform} subclass.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see DatumShiftGrid
 *
 * @since 0.7
 */
public abstract class DatumShiftTransform extends AbstractMathTransform implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4492222496475405226L;

    /**
     * The parameters used for creating this datum shift. They are used for formatting <i>Well Known Text</i> (WKT)
     * and error messages. Subclasses shall not use the values defined in this object for computation purpose,
     * except at construction time.
     *
     * @see #getContextualParameters()
     */
    final ContextualParameters context;

    /**
     * The grid of datum shifts from source datum to target datum, or {@code null} if none.
     *
     * @see InterpolatedTransform#getShiftGrid()
     */
    final DatumShiftGrid<?,?> grid;

    /**
     * Conversion from (λ,φ) coordinates in radians to grid indices (x,y).
     *
     * <ul>
     *   <li>x  =  (λ - λ₀) ⋅ {@code scaleX}  =  λ ⋅ {@code scaleX} + x₀</li>
     *   <li>y  =  (φ - φ₀) ⋅ {@code scaleY}  =  φ ⋅ {@code scaleY} + y₀</li>
     * </ul>
     *
     * Those factors are extracted from the {@link DatumShiftGrid#getCoordinateToGrid()}
     * transform for performance reasons.
     *
     * @see #computeConversionFactors()
     */
    private transient double scaleX, scaleY, x0, y0;

    /**
     * Creates a datum shift transform for direct interpolations in a grid.
     * It is caller responsibility to initialize the {@link #context} parameters.
     *
     * @param descriptor  the contextual parameter descriptor.
     * @param grid        interpolation grid.
     */
    DatumShiftTransform(ParameterDescriptorGroup descriptor, final DatumShiftGrid<?,?> grid) {
        final int dim = grid.getTranslationDimensions();
        context = new ContextualParameters(descriptor, dim, dim);
        this.grid = grid;
        computeConversionFactors();
    }

    /**
     * Creates a datum shift transform for interpolations in geocentric domain.
     * It is caller responsibility to initialize the {@link #context} parameters.
     *
     * @param descriptor  the contextual parameter descriptor.
     * @param grid        interpolation grid in geocentric coordinates, or {@code null} if none.
     * @param isSource3D  {@code true} if the source coordinates have a height.
     * @param isTarget3D  {@code true} if the target coordinates have a height.
     */
    DatumShiftTransform(final ParameterDescriptorGroup descriptor,
            final boolean isSource3D, final boolean isTarget3D, final DatumShiftGrid<?,?> grid)
    {
        context = new ContextualParameters(descriptor, isSource3D ? 3 : 2, isTarget3D ? 3 : 2);
        this.grid = grid;
        computeConversionFactors();
    }

    /**
     * Creates a datum shift transform with the same data as the given transform,
     * except for the number of dimensions.
     *
     * @param  other   the transform to copy.
     * @param  srcDim  new number of source dimensions.
     * @param  tgtDim  new number of target dimensions.
     * @throws IllegalArgumentException if a dimension is zero or negative.
     */
    DatumShiftTransform(final DatumShiftTransform other, final int srcDim, final int tgtDim) {
        context = other.context.redimension(srcDim, tgtDim);
        grid    = other.grid;
        scaleX  = other.scaleX;
        scaleY  = other.scaleY;
        x0      = other.x0;
        y0      = other.y0;
    }

    /**
     * Invoked after deserialization. This method computes the transient fields.
     *
     * @param  in  the input stream from which to deserialize the datum shift grid.
     * @throws IOException if an I/O error occurred while reading or if the stream contains invalid data.
     * @throws ClassNotFoundException if the class serialized on the stream is not on the module path.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        computeConversionFactors();
    }

    /**
     * Computes the conversion factors needed for calls to {@link DatumShiftGrid#interpolateInCell(double, double, double[])}.
     * This method takes only the {@value DatumShiftGrid#INTERPOLATED_DIMENSIONS} first dimensions. If a conversion factor can
     * not be computed, then it is set to NaN.
     */
    @SuppressWarnings("fallthrough")
    private void computeConversionFactors() {
        scaleX = Double.NaN;
        scaleY = Double.NaN;
        x0     = Double.NaN;
        y0     = Double.NaN;
        if (grid != null) {
            final LinearTransform coordinateToGrid = grid.getCoordinateToGrid();
            final double toStandardUnit = Units.toStandardUnit(grid.getCoordinateUnit());
            if (!Double.isNaN(toStandardUnit)) {
                final Matrix m = coordinateToGrid.getMatrix();
                if (Matrices.isAffine(m)) {
                    final int n = m.getNumCol() - 1;
                    switch (m.getNumRow()) {
                        default: y0 = m.getElement(1,n); scaleY = diagonal(m, 1, n) / toStandardUnit;   // Fall through
                        case 1:  x0 = m.getElement(0,n); scaleX = diagonal(m, 0, n) / toStandardUnit;
                        case 0:  break;
                    }
                }
            }
        }
    }

    /**
     * Returns the value on the diagonal of the given matrix, provided that all other non-translation terms are 0.
     *
     * @param  m  the matrix from which to get the scale factor on a row.
     * @param  j  the row for which to get the scale factor.
     * @param  n  index of the last column.
     * @return the scale factor on the diagonal, or NaN.
     */
    private static double diagonal(final Matrix m, final int j, int n) {
        while (--n >= 0) {
            if (j != n && m.getElement(j, n) != 0) {
                return Double.NaN;
            }
        }
        return m.getElement(j, j);
    }

    /**
     * Sets the semi-axis length in the {@link #context} parameters.
     * This is a helper method for constructors in some (not all) subclasses.
     *
     * @param  semiMajor  the semi-major axis length of the source ellipsoid.
     * @param  semiMinor  the semi-minor axis length of the source ellipsoid.
     * @param  unit       the unit of measurement of source ellipsoid axes.
     * @param  target     the target ellipsoid.
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
     * <h4>Example</h4>
     * The chain of transforms of an {@link InterpolatedGeocentricTransform} is:
     * <div class="horizontal-flow" style="align-items:center">
     *   <div>{@include formulas.html#NormalizeGeographic}</div>
     *   <div>→</div>
     *   <div><ol style="padding-left: 15px">
     *     <li>Geographic to geocentric conversion</li>
     *     <li>Geocentric interpolation</li>
     *     <li>Geocentric to geographic conversion</li>
     *   </ol></div>
     *   <div>→</div>
     *   <div>{@include formulas.html#DenormalizeGeographic}</div>
     * </div>
     *
     * This method returns the parameters for the part in the middle of above example.
     * The content of this part is highly implementation-dependent and used mostly for
     * {@linkplain org.apache.sis.io.wkt.Convention#INTERNAL debugging purposes}.
     * The parameters that describe the process as a whole are rather given by {@link #getContextualParameters()}.
     *
     * @return the internal parameter values for this transform.
     */
    @Debug
    @Override
    public ParameterValueGroup getParameterValues() {
        return context;     // Overridden by some subclasses.
    }

    /**
     * Returns the parameters used for creating the complete transformation. Those parameters describe a sequence
     * of <i>normalize</i> → {@code this} → <i>denormalize</i> transforms, <strong>not</strong>
     * including {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes axis swapping}.
     * Those parameters are used for formatting <i>Well Known Text</i> (WKT) and error messages.
     *
     * @return the parameter values for the <i>normalize</i> → {@code this} → <i>denormalize</i> chain of transforms.
     */
    @Override
    protected ContextualParameters getContextualParameters() {
        return context;
    }

    /**
     * Converts the given normalized <var>x</var> coordinate to grid index.
     * "Normalized coordinates" are coordinates in the unit of measurement given by {@link Unit#getSystemUnit()}.
     * For angular coordinates, this is radians. For linear coordinates, this is metres.
     *
     * @param  x  the "real world" coordinate (often longitude in radians) of the point for which to get the translation.
     * @return the grid index for the given coordinate. May be out of bounds.
     */
    final double normalizedToGridX(final double x) {
        return x * scaleX + x0;
    }

    /**
     * Converts the given normalized <var>x</var> coordinate to grid index.
     * "Normalized coordinates" are coordinates in the unit of measurement given by {@link Unit#getSystemUnit()}.
     * For angular coordinates, this is radians. For linear coordinates, this is metres.
     *
     * @param  y  the "real world" coordinate (often latitude in radians) of the point for which to get the translation.
     * @return the grid index for the given coordinate. May be out of bounds.
     */
    final double normalizedToGridY(final double y) {
        return y * scaleY + y0;
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    protected int computeHashCode() {
        return super.computeHashCode() + Objects.hashCode(grid);
    }

    /**
     * Compares the specified object with this math transform for equality.
     *
     * @hidden because nothing new to said.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        return super.equals(object, mode) && Objects.equals(grid, ((DatumShiftTransform) object).grid);
    }
}
