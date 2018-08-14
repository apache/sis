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
import org.opengis.test.referencing.TransformTestCase;
import org.apache.sis.test.DependsOn;
import org.junit.Test;


/**
 * Tests {@link LocalizationGridBuilder}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
@DependsOn(LinearTransformBuilderTest.class)
public final strictfp class LocalizationGridBuilderTest extends TransformTestCase {
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
        transform = builder.create(null);

        tolerance = 1E-13;
        isInverseTransformSupported = false;
        verifyQuadratic();
        /*
         * The tolerance value specified here should be approximately equal to ResidualGrid.accuracy.
         */
        tolerance = LocalizationGridBuilder.DEFAULT_PRECISION;
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
}
