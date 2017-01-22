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

import java.awt.Shape;
import java.awt.geom.Point2D;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.datum.DatumShiftGrid;


/**
 * An interpolated Molodensky transform for two-dimensional input and output coordinates.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class InterpolatedMolodenskyTransform2D extends InterpolatedMolodenskyTransform implements MathTransform2D {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 3050077982181110135L;

    /**
     * Constructs a 2D transform.
     */
    InterpolatedMolodenskyTransform2D(final Ellipsoid source, final Ellipsoid target, final DatumShiftGrid<Angle,Length> grid) {
        super(source, false, target, false, grid);
    }

    /**
     * Computes the derivative at the given position point.
     */
    @Override
    public Matrix derivative(Point2D point) throws TransformException {
        return AbstractMathTransform2D.derivative(this, point);
    }

    /**
     * Transforms a single point.
     */
    @Override
    public Point2D transform(Point2D ptSrc, Point2D ptDst) throws TransformException {
        return AbstractMathTransform2D.transform(this, ptSrc, ptDst);
    }

    /**
     * Transforms the given shape.
     */
    @Override
    public Shape createTransformedShape(Shape shape) throws TransformException {
        return AbstractMathTransform2D.createTransformedShape(this, shape, null, null, false);
    }

    /**
     * Returns the inverse transform of this transform.
     */
    @Override
    public MathTransform2D inverse() {
        return (MathTransform2D) super.inverse();
    }

    /**
     * The inverse of the enclosing {@link InterpolatedMolodenskyTransform2D}.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.7
     * @version 0.7
     * @module
     */
    static final class Inverse extends InterpolatedMolodenskyTransform.Inverse implements MathTransform2D {
        /**
         * Serial number for inter-operability with different versions.
         */
        private static final long serialVersionUID = 3175846640786132902L;

        /**
         * Constructs the inverse of an interpolated Molodensky transform.
         *
         * @param inverse  the transform for which to create the inverse.
         * @param source   the source ellipsoid of the given {@code inverse} transform.
         * @param target   the target ellipsoid of the given {@code inverse} transform.
         */
        Inverse(final InterpolatedMolodenskyTransform inverse, final Ellipsoid source, final Ellipsoid target) {
           super(inverse, source, target);
        }

        /**
         * Computes the derivative at the given position point.
         */
        @Override
        public Matrix derivative(Point2D point) throws TransformException {
            return AbstractMathTransform2D.derivative(this, point);
        }

        /**
         * Transforms a single point.
         */
        @Override
        public Point2D transform(Point2D ptSrc, Point2D ptDst) throws TransformException {
            return AbstractMathTransform2D.transform(this, ptSrc, ptDst);
        }

        /**
         * Transforms the given shape.
         */
        @Override
        public Shape createTransformedShape(Shape shape) throws TransformException {
            return AbstractMathTransform2D.createTransformedShape(this, shape, null, null, false);
        }

        /**
         * Returns the inverse transform of this transform.
         */
        @Override
        public MathTransform2D inverse() {
            return (MathTransform2D) super.inverse();
        }
    }
}
