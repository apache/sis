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
import javax.measure.Quantity;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.datum.DatumShiftGrid;
import org.apache.sis.referencing.operation.matrix.NoninvertibleMatrixException;


/**
 * An interpolated transform for two-dimensional input and output coordinates.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class InterpolatedTransform2D extends InterpolatedTransform implements MathTransform2D {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -4273636001174024827L;

    /**
     * Constructs a 2D transform.
     */
    <T extends Quantity<T>> InterpolatedTransform2D(final DatumShiftGrid<T,T> grid) throws NoninvertibleMatrixException {
        super(grid);
    }

    /**
     * Computes the derivative at the given position.
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
        return AbstractMathTransform2D.createTransformedShape(this, shape, null, null);
    }

    /**
     * Returns the inverse transform of this transform.
     */
    @Override
    public MathTransform2D inverse() {
        return (MathTransform2D) super.inverse();
    }

    /**
     * Invoked at construction time for creating the two-dimensional inverse transform.
     */
    @Override
    InterpolatedTransform.Inverse createInverse() {
        return new Inverse(this);
    }

    /**
     * The inverse of the enclosing {@link InterpolatedTransform2D}.
     *
     * @author  Martin Desruisseaux (Geomatys)
     */
    final class Inverse extends InterpolatedTransform.Inverse implements MathTransform2D {
        /**
         * Serial number for inter-operability with different versions.
         */
        private static final long serialVersionUID = 4802773675799229357L;

        /**
         * Constructs the inverse of an interpolated transform.
         */
        Inverse(final InterpolatedTransform2D forward) {
            super(forward);
        }

        /**
         * Computes the derivative at the given position.
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
            return AbstractMathTransform2D.createTransformedShape(this, shape, null, null);
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
