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

import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.Envelope2D;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opengis.test.referencing.TransformTestCase;
import org.apache.sis.test.FailureDetailsReporter;
import static org.apache.sis.referencing.Assertions.assertEnvelopeEquals;


/**
 * Tests {@link LocalizationGridBuilder}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
@ExtendWith(FailureDetailsReporter.class)
public final class LocalizationGridBuilderTest extends TransformTestCase {
    /**
     * Creates a new test case.
     */
    public LocalizationGridBuilderTest() {
    }

    /**
     * Creates a builder initialized with control points computed from the given affine transform.
     * Some non-linear terms will be added to the coordinates computed by the given transform.
     *
     * @param  reference  the affine transform to use as a basis for generating a localization grid.
     * @param  width      number of columns in the localization grid to create.
     * @param  height     number of rows in the localization grid to create.
     * @return the builder to test.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static LocalizationGridBuilder builder(final AffineTransform reference, final int width, final int height) {
        final LocalizationGridBuilder builder = new LocalizationGridBuilder(width, height);
        Point2D pt = new Point2D.Double();
        for (int gridY=0; gridY < height; gridY++) {
            for (int gridX=0; gridX < width; gridX++) {
                pt.setLocation(gridX, gridY);
                pt = reference.transform(pt, pt);
                final double gx2 = gridX * gridX;
                final double gy2 = gridY * gridY;
                final double x = pt.getX() + 0.4*gx2 + 0.7*gy2;
                final double y = pt.getY() + 0.3*gx2 - 0.5*gy2;
                builder.setControlPoint(gridX, gridY, x, y);
                if (false) {
                    // For generating verification code.
                    System.out.printf("verifyTransform(new double[] {%d, %d}, new double[] {%f, %f});%n", gridX, gridY, x, y);
                }
            }
        }
        return builder;
    }

    /**
     * Tests a small grid built from an arbitrary affine transform with small quadratic terms added to the control points.
     *
     * @throws FactoryException if an error occurred while computing the localization grid.
     * @throws TransformException if an error occurred while testing a transformation.
     */
    @Test
    public void testQuadratic() throws FactoryException, TransformException {
        final AffineTransform reference = new AffineTransform(20, -30, 5, -4, -20, 8);
        final LocalizationGridBuilder builder = builder(reference, 5, 4);
        builder.setDesiredPrecision(1E-6);
        transform = builder.create(null);

        tolerance = 2E-7;
        isInverseTransformSupported = false;
        verifyQuadratic();
        /*
         * The tolerance value specified here should be approximately equal to ResidualGrid.accuracy.
         * That value was specified in the call to builder.setDesiredPrecision(1E-6).
         */
        tolerance = 1E-6;
        isInverseTransformSupported = true;
        verifyQuadratic();
    }

    /**
     * Hard-coded verifications of some values for the transform built by {@link #testQuadratic()}.
     * This verification is run twice: once without check for inverse transform, and a second time
     * with inverse transform enabled.
     */
    private void verifyQuadratic() throws TransformException {
        verifyTransform(new double[] {0, 0}, new double[] {-20.0,    8.0});         // Translation terms
        verifyTransform(new double[] {1, 0}, new double[] {  0.4,  -21.7});         // Not yet non-linear
        verifyTransform(new double[] {0, 1}, new double[] {-14.3,    3.5});         // Not yet non-linear
        verifyTransform(new double[] {1, 1}, new double[] {  6.1,  -26.2});         // Not yet non-linear
        verifyTransform(new double[] {0, 3}, new double[] {  1.3,   -8.5});
        verifyTransform(new double[] {4, 3}, new double[] { 87.7, -123.7});
    }

    /**
     * Tests {@link LocalizationGridBuilder#LocalizationGridBuilder(LinearTransformBuilder)}.
     *
     * @throws TransformException if an error occurred while computing the envelope.
     */
    @Test
    public void testCreateFromLocalizations() throws TransformException {
        final LinearTransformBuilder localizations = new LinearTransformBuilder();
        localizations.setControlPoint(new int[] {0, 0}, new double[] {-20.0,    8.0});
        localizations.setControlPoint(new int[] {1, 0}, new double[] {  0.4,  -21.7});
        localizations.setControlPoint(new int[] {0, 1}, new double[] {-14.3,    3.5});
        localizations.setControlPoint(new int[] {1, 1}, new double[] {  6.1,  -26.2});
        localizations.setControlPoint(new int[] {0, 2}, new double[] {  1.3,   -8.5});
        localizations.setControlPoint(new int[] {1, 2}, new double[] { 87.7, -123.7});
        LocalizationGridBuilder builder = new LocalizationGridBuilder(localizations);
        /*
         * Verifies the grid size by checking the source envelope.
         * Minimum and maximum values are inclusive.
         */
        assertEnvelopeEquals(new Envelope2D(null, 0, 0, 1, 2), builder.getSourceEnvelope(false));
        /*
         * Verify a few random positions.
         */
        assertArrayEquals(new double[] {-20.0,    8.0}, builder.getControlPoint(0, 0));
        assertArrayEquals(new double[] {  0.4,  -21.7}, builder.getControlPoint(1, 0));
        assertArrayEquals(new double[] {  1.3,   -8.5}, builder.getControlPoint(0, 2));
        assertArrayEquals(new double[] { 87.7, -123.7}, builder.getControlPoint(1, 2));
        /*
         * Verify getting a row and a column.
         */
        assertArrayEquals(new double[] {-8.5, -123.7}, builder.getRow(1, 2).doubleValues());
        assertArrayEquals(new double[] {-21.7, -26.2, -123.7}, builder.getColumn(1, 1).doubleValues());
    }
}
