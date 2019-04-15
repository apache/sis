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
package org.apache.sis.internal.netcdf;

import java.awt.geom.AffineTransform;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.referencing.operation.builder.LocalizationGridBuilder;
import org.apache.sis.referencing.operation.transform.MathTransformTestCase;
import org.apache.sis.referencing.operation.transform.CoordinateDomain;
import org.apache.sis.internal.system.DefaultFactories;
import org.junit.Test;


/**
 * Tests {@link SatelliteGroundTrack}. There is no external data that we can use as a reference.
 * Consequently this test merely verifies that {@link SatelliteGroundTrack} is self-consistent.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class SatelliteGroundTrackTest extends MathTransformTestCase {
    /**
     * Size of the grid created for testing purpose.
     */
    private static final int WIDTH = 20, HEIGHT = 20;

    /**
     * Creates a transform for a grid of fixed size in a geographic domain.
     */
    private void createTransform() throws TransformException, FactoryException {
        final LocalizationGridBuilder grid = new LocalizationGridBuilder(WIDTH, HEIGHT);
        final AffineTransform tr = AffineTransform.getRotateInstance(StrictMath.random()/2 + 0.25);     // Between 14 and 43°.
        tr.translate(-WIDTH / 2, -HEIGHT / 2);
        final double[] point = new double[2];
        for (int y=0; y<HEIGHT; y++) {
            for (int x=0; x<WIDTH; x++) {
                point[0] = x;
                point[1] = y;
                tr.transform(point, 0, point, 0, 1);
                grid.setControlPoint(x, y, point);
            }
        }
        transform = SatelliteGroundTrack.create(DefaultFactories.forBuildin(MathTransformFactory.class), grid, 1, 1);
        tolerance = 1E-12;
        derivativeDeltas = new double[] {0.1, 0.1};
    }

    /**
     * Tests self-consistency at random points.
     *
     * @throws FactoryException if an error occurred while creating the transform.
     * @throws TransformException if an error occurred while transforming a point.
     */
    @Test
    public void testConsistency() throws TransformException, FactoryException {
        createTransform();
        validate();
        verifyInDomain(CoordinateDomain.RANGE_10, -979924465940961910L);
        transform = transform.inverse();
        validate();
        verifyInDomain(CoordinateDomain.RANGE_10, -2122465178330330413L);
    }
}
