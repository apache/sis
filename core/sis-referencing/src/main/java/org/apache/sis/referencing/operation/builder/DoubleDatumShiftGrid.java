/*
 * Copyright 2016 rmarechal.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.referencing.operation.builder;

import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;
import org.apache.sis.referencing.datum.DatumShiftGrid;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.util.ArgumentChecks;

/**
     * An implementation of {@link DatumShiftGridFile} which stores the offset values in {@code double[]} arrays.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @author  Remi Marechal (Geomatys)
     * @since   0.7
     * @version 0.7
     * @module
     */
strictfp class DoubleDatumShiftGrid<C extends Quantity, T extends Quantity> extends DatumShiftGrid<C, T> {

    /**
     *
     */
    private final double[][] targets;

    /**
     *
     */
    private final double accuracy;

    /**
     *
     * @param coordinateUnit
     * @param sourcePointCoordinateToGrid
     * @param gridSize
     * @param translationUnit
     * @param targets
     * @param cellPrecision
     */
    public DoubleDatumShiftGrid(final Unit<C> coordinateUnit, final LinearTransform sourcePointCoordinateToGrid,
            int[] gridSize, /*final boolean isCellValueRatio,*/ final Unit<T> translationUnit,
            final double[][] targets, double cellPrecision) {
        super(coordinateUnit, sourcePointCoordinateToGrid, gridSize, false, translationUnit);
        ArgumentChecks.ensureNonNull("Targets points", targets);
        ArgumentChecks.ensurePositive("cellPrecision", cellPrecision);
        this.targets  = targets;
        this.accuracy = cellPrecision;
    }

    @Override
    public int getTranslationDimensions() {
        return targets.length;
    }

    @Override
    public double getCellValue(int dim, int gridX, int gridY) {
        return targets[dim][gridY * getGridSize()[0] + gridX];
    }

    @Override
    public double getCellPrecision() {
        return accuracy;
    }

}
