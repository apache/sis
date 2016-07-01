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
package org.apache.sis.geometry;

import java.awt.geom.Rectangle2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.test.DependsOn;

import static org.apache.sis.test.ReferencingAssert.*;


/**
 * Tests the {@link Shapes2D} class.
 * This class inherits the test methods defined in {@link TransformTestCase}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
@DependsOn(CurveExtremumTest.class)
public final strictfp class Shapes2DTest extends TransformTestCase<Rectangle2D> {
    /**
     * Creates a rectangle for the given CRS and coordinate values.
     */
    @Override
    Rectangle2D createFromExtremums(CoordinateReferenceSystem crs, double xmin, double ymin, double xmax, double ymax) {
        return new Rectangle2D.Double(xmin, ymin, xmax - xmin, ymax - ymin);
    }

    /**
     * Transforms a rectangle using the given math transform.
     * This transformation can not handle poles.
     */
    @Override
    Rectangle2D transform(CoordinateReferenceSystem targetCRS, MathTransform2D transform, Rectangle2D envelope) throws TransformException {
        return Shapes2D.transform(transform, envelope, null);
    }

    /**
     * Transforms a rectangle using the given operation.
     * This transformation can handle poles.
     */
    @Override
    Rectangle2D transform(CoordinateOperation operation, Rectangle2D envelope) throws TransformException {
        return Shapes2D.transform(operation, envelope, null);
    }

    /**
     * Returns {@code true} if the outer rectangle contains the inner one.
     */
    @Override
    boolean contains(Rectangle2D outer, Rectangle2D inner) {
        return outer.contains(inner);
    }

    /**
     * Asserts that the given rectangle is equals to the expected value.
     */
    @Override
    void assertGeometryEquals(Rectangle2D expected, Rectangle2D actual, double tolx, double toly) {
        assertRectangleEquals(expected, actual, tolx, toly);
    }
}
