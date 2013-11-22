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
package org.apache.sis.referencing.cs;

import java.util.Arrays;
import javax.measure.unit.Unit;
import javax.measure.converter.UnitConverter;
import javax.measure.converter.LinearConverter;
import javax.measure.converter.ConversionException;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.measure.Units;
import org.apache.sis.util.Static;
import org.apache.sis.util.Classes;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;

// Related to JDK7
import java.util.Objects;


/**
 * Utility methods working on {@link CoordinateSystem} objects.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 */
public final class CoordinateSystems extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private CoordinateSystems() {
    }

    /**
     * Returns the axis direction for the specified coordinate system.
     *
     * @param  cs The coordinate system.
     * @return The axis directions for the specified coordinate system.
     */
    private static AxisDirection[] getAxisDirections(final CoordinateSystem cs) {
        final AxisDirection[] directions = new AxisDirection[cs.getDimension()];
        for (int i=0; i<directions.length; i++) {
            directions[i] = cs.getAxis(i).getDirection();
        }
        return directions;
    }

    /**
     * Returns an affine transform between two coordinate systems.
     * The two coordinate systems must implement the same GeoAPI coordinate system interface
     * (for example both of them shall implement {@link org.opengis.referencing.cs.CartesianCS},
     * or both of them shall implement {@link org.opengis.referencing.cs.EllipsoidalCS}).
     * Only units and axes order (e.g. transforming from
     * ({@linkplain AxisDirection#NORTH NORTH},{@linkplain AxisDirection#WEST WEST}) to
     * ({@linkplain AxisDirection#EAST EAST},{@linkplain AxisDirection#NORTH NORTH})
     * are taken in account by this method.
     *
     * <blockquote><font size="-1"><b>Example:</b>
     * If coordinates in {@code sourceCS} are (<var>x</var>,<var>y</var>) tuples in metres
     * and coordinates in {@code targetCS} are (<var>-y</var>,<var>x</var>) tuples in centimetres,
     * then the transformation can be performed as below:
     *
     * {@preformat math
     *     ┌      ┐   ┌                ┐ ┌     ┐
     *     │-y(cm)│   │   0  -100    0 │ │ x(m)│
     *     │ x(cm)│ = │ 100     0    0 │ │ y(m)│
     *     │ 1    │   │   0     0    1 │ │ 1   │
     *     └      ┘   └                ┘ └     ┘
     * }
     * </font></blockquote>
     *
     * @param  sourceCS The source coordinate system.
     * @param  targetCS The target coordinate system.
     * @return The conversion from {@code sourceCS} to {@code targetCS} as an affine transform.
     *         Only axis direction and units are taken in account.
     * @throws IllegalArgumentException if the CS are not of the same type, or axes do not match.
     * @throws ConversionException if the units are not compatible, or the conversion is non-linear.
     *
     * @see Matrices#createTransform(AxisDirection[], AxisDirection[])
     */
    public static Matrix swapAndScaleAxes(final CoordinateSystem sourceCS,
                                          final CoordinateSystem targetCS)
            throws IllegalArgumentException, ConversionException
    {
        if (!Classes.implementSameInterfaces(sourceCS.getClass(), targetCS.getClass(), CoordinateSystem.class)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IncompatibleCoordinateSystemTypes));
        }
        final AxisDirection[] sourceAxis = getAxisDirections(sourceCS);
        final AxisDirection[] targetAxis = getAxisDirections(targetCS);
        final MatrixSIS matrix = Matrices.createTransform(sourceAxis, targetAxis);
        assert Arrays.equals(sourceAxis, targetAxis) == matrix.isIdentity() : matrix;
        /*
         * The previous code computed a matrix for swapping axes. Usually, this
         * matrix contains only 0 and 1 values with only one "1" value by row.
         * For example, the matrix operation for swapping x and y axes is:
         *          ┌ ┐   ┌         ┐ ┌ ┐
         *          │y│   │ 0  1  0 │ │x│
         *          │x│ = │ 1  0  0 │ │y│
         *          │1│   │ 0  0  1 │ │1│
         *          └ ┘   └         ┘ └ ┘
         * Now, take in account units conversions. Each matrix's element (j,i)
         * is multiplied by the conversion factor from sourceCS.getUnit(i) to
         * targetCS.getUnit(j). This is an element-by-element multiplication,
         * not a matrix multiplication. The last column is processed in a special
         * way, since it contains the offset values.
         */
        final int sourceDim = matrix.getNumCol() - 1;  // == sourceCS.getDimension()
        final int targetDim = matrix.getNumRow() - 1;  // == targetCS.getDimension()
        for (int j=0; j<targetDim; j++) {
            final Unit<?> targetUnit = targetCS.getAxis(j).getUnit();
            for (int i=0; i<sourceDim; i++) {
                final double element = matrix.getElement(j,i);
                if (element == 0) {
                    // There is no dependency between source[i] and target[j]
                    // (i.e. axes are orthogonal).
                    continue;
                }
                final Unit<?> sourceUnit = sourceCS.getAxis(i).getUnit();
                if (Objects.equals(sourceUnit, targetUnit)) {
                    // There is no units conversion to apply
                    // between source[i] and target[j].
                    continue;
                }
                final UnitConverter converter = sourceUnit.getConverterToAny(targetUnit);
                if (!(converter instanceof LinearConverter)) {
                    throw new ConversionException(Errors.format(
                              Errors.Keys.NonLinearUnitConversion_2, sourceUnit, targetUnit));
                }
                final double offset = converter.convert(0);
                final double scale  = Units.derivative(converter, 0);
                matrix.setElement(j, i, element*scale);
                matrix.setElement(j, sourceDim, matrix.getElement(j, sourceDim) + element*offset);
            }
        }
        return matrix;
    }
}
