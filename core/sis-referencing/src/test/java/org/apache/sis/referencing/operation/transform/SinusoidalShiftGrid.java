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

import java.util.Collections;
import java.awt.geom.AffineTransform;
import javax.measure.quantity.Dimensionless;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.referencing.datum.DatumShiftGrid;
import org.apache.sis.measure.Units;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.parameter.Parameters;

import static org.junit.Assert.*;


/**
 * Dummy implementation of {@link DatumShiftGrid} containing translation vectors that are computed by a sinusoidal function.
 * This class has no computational interest compared to a direct implementation of sinusoidal formulas, but is convenient
 * for debugging {@link InterpolatedTransform} because of its predictable behavior (easier to see with rotation of 0°).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
@SuppressWarnings("serial")                             // Not intended to be serialized.
final strictfp class SinusoidalShiftGrid extends DatumShiftGrid<Dimensionless,Dimensionless> {
    /**
     * Number of source and target dimensions of the grid.
     */
    static final int DIMENSION = 2;

    /**
     * The precision used for inverse transforms, in units of cells.
     * A smaller numbers will cause more iterations to occur.
     */
    static final double PRECISION = 1E-7;

    /**
     * Index of the first point in {@link #samplePoints()} which contains a fractional part.
     * Those points need a different tolerance threshold because the interpolations performed
     * by {@link DatumShiftGrid} is not the same than the formula used in this test class.
     *
     * @see #samplePoints()
     */
    static final int FIRST_FRACTIONAL_COORDINATE = 3 * DIMENSION;

    /**
     * Grid size in number of pixels. The translation vectors in the middle of this grid will be (0,0).
     * We simulate a grid spanning from 80°S to 80°N.
     */
    private static final int WIDTH = 100, HEIGHT = 160;

    /**
     * An internal step performed during computation of translation vectors at a given point.
     */
    private final AffineTransform offsets;

    /**
     * A temporary buffer for calculations with {@link #offsets}.
     */
    private final double[] buffer;

    /**
     * Creates a new grid mock of size {@value #WIDTH} × {@value #HEIGHT} pixels.
     * The translation vectors will be (0,0) in the middle of that grid and increase
     * as we get further from the center.
     *
     * @param  rotation  the rotation angle, in degrees.
     */
    SinusoidalShiftGrid(final double rotation) {
        super(Units.UNITY, MathTransforms.identity(DIMENSION), new int[] {WIDTH, HEIGHT}, true, Units.UNITY);
        offsets = AffineTransform.getTranslateInstance(0.5*WIDTH, 0.5*HEIGHT);
        offsets.rotate(StrictMath.toRadians(rotation));
        offsets.scale(-0.75, 0.95);
        buffer = new double[DIMENSION];
    }

    /**
     * Returns the number of dimensions of the translation vectors interpolated by this shift grid.
     */
    @Override
    public int getTranslationDimensions() {
        return DIMENSION;
    }

    /**
     * Returns suggested points to use for testing purposes. This method returns an array of length 2,
     * with source coordinates in the first array and target coordinates in the second array.
     *
     * @see #FIRST_FRACTIONAL_COORDINATE
     */
    final double[][] samplePoints() {
        final double[] sources = {
            /*[0]*/ WIDTH/2, HEIGHT/2,
            /*[1]*/      30,      120,
            /*[2]*/      75,       40,
            /*[3]*/      10.356,   30.642               // FIRST_FRACTIONAL_COORDINATE must point here.
        };
        final double[] targets = new double[sources.length];
        transform(sources, targets);
        return new double[][] {sources, targets};
    }

    /**
     * Applies an arbitrary non-linear transform on the given source coordinates
     * and stores the results in the given target array.
     */
    private void transform(final double[] sources, final double[] targets) {
        for (int i=0; i<sources.length;) {
            double x = sources[i  ] - WIDTH  / 2;
            double y = sources[i+1] - HEIGHT / 2;
            x /= StrictMath.max(0.1, StrictMath.cos(StrictMath.toRadians(y)));
            targets[i++] = x;
            targets[i++] = y;
        }
        offsets.transform(targets, 0, targets, 0, targets.length / DIMENSION);
    }

    /**
     * Returns the cell value at the given dimension and grid index.
     * Those values are components of <em>translation</em> vectors.
     */
    @Override
    public double getCellValue(int dim, int gridX, int gridY) {
        assertTrue(dim   >= 0 && dim < DIMENSION);
        assertTrue(gridX >= 0 && gridX < WIDTH);
        assertTrue(gridY >= 0 && gridY < HEIGHT);
        buffer[0] = gridX;
        buffer[1] = gridY;
        transform(buffer, buffer);
        buffer[0] -= gridX;
        buffer[1] -= gridY;
        return buffer[dim];
    }

    /**
     * Returns an arbitrary cell precision. This determines when the iteration algorithm stops.
     */
    @Override
    public double getCellPrecision() {
        return PRECISION;
    }

    /**
     * Returns a dummy parameter descriptor for this test.
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return new DefaultParameterDescriptorGroup(
                Collections.singletonMap(DefaultParameterDescriptorGroup.NAME_KEY, "Test grid"), 0, 1);
    }

    /**
     * No parameter to set for this test.
     */
    @Override
    public void getParameterValues(Parameters parameters) {
    }
}
