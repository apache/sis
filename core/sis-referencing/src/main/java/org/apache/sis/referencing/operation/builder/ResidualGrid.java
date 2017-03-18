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
package org.apache.sis.referencing.operation.builder;

import javax.measure.quantity.Dimensionless;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.internal.referencing.provider.DatumShiftGridFile;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.measure.Units;
import org.opengis.referencing.operation.Matrix;


/**
 * The residuals after an affine approximation has been created for a set of matching control point pairs.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class ResidualGrid extends DatumShiftGridFile<Dimensionless,Dimensionless> {
    /**
     * The parameter descriptors for the "Localization grid" operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = new ParameterBuilder();
        PARAMETERS = builder.addName("Localization grid").createGroup();
    }

    /**
     * The residual data, as translations to apply on the result of affine transform.
     * In this flat array, index of target dimension varies fastest, then column index, then row index.
     */
    private final double[] offsets;

    /**
     * Number of dimension of target coordinates.
     */
    private final int numDim;

    /**
     * Conversion from grid coordinates to the final "real world" coordinates.
     *
     * @see #gridToTarget()
     */
    private final LinearTransform gridToTarget;

    /**
     * Creates a new residual grid.
     *
     * @param sourceToGrid  conversion from the "real world" source coordinates to grid indices including fractional parts.
     * @param gridToTarget  conversion from grid coordinates to the final "real world" coordinates.
     * @param numDim        number of dimension of target coordinates.
     * @param residuals     the residual data, as translations to apply on the result of affine transform.
     */
    ResidualGrid(final LinearTransform sourceToGrid, final LinearTransform gridToTarget,
            final int nx, final int ny, final int numDim, final double[] residuals)
    {
        super(Units.UNITY, Units.UNITY, true, sourceToGrid, nx, ny, PARAMETERS);
        this.gridToTarget = gridToTarget;
        this.numDim       = numDim;
        this.offsets      = residuals;
        this.accuracy     = 0.01;           // TODO
    }

    /**
     * Creates a new datum shift grid with the same grid geometry than the given grid
     * but a reference to a different data array.
     */
    private ResidualGrid(final ResidualGrid other, final double[] data) {
        super(other);
        gridToTarget = other.gridToTarget;
        numDim       = other.numDim;
        accuracy     = other.accuracy;
        offsets      = data;
    }

    /**
     * Returns a new grid with the same geometry than this grid but different data array.
     */
    @Override
    protected DatumShiftGridFile<Dimensionless, Dimensionless> setData(final Object[] other) {
        return new ResidualGrid(this, (double[]) other[0]);
    }

    /**
     * Returns reference to the data array. This method is for cache management, {@link #equals(Object)}
     * and {@link #hashCode()} implementations only and should not be invoked in other context.
     */
    @Override
    protected Object[] getData() {
        return new Object[] {offsets};
    }

    /**
     * Returns the transform from grid coordinates to "real world" coordinates after the datum shift has been applied.
     */
    @Override
    public Matrix gridToTarget() {
        return gridToTarget.getMatrix();
    }

    /**
     * Returns the number of dimensions of the translation vectors interpolated by this shift grid.
     */
    @Override
    public int getTranslationDimensions() {
        return numDim;
    }

    /**
     * Returns the cell value at the given dimension and grid index.
     */
    @Override
    public double getCellValue(int dim, int gridX, int gridY) {
        return offsets[(gridX + gridY*nx) * numDim + dim];
    }

    /**
     * Returns {@code true} if the given object is a grid containing the same data than this grid.
     */
    @Override
    public boolean equals(final Object other) {
        if (super.equals(other)) {
            // Offset array has been compared by the parent class.
            return numDim == ((ResidualGrid) other).numDim;
        }
        return false;
    }
}
