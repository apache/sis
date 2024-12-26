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

import java.util.Map;
import java.awt.Shape;
import java.awt.geom.Point2D;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;


/**
 * A specializable transform in the two-dimensional case.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class SpecializableTransform2D extends SpecializableTransform implements MathTransform2D {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 2924682501896480813L;

    /**
     * Creates a new transform.
     */
    SpecializableTransform2D(MathTransform global, Map<Envelope,MathTransform> specializations) {
        super(global, specializations);
    }

    /**
     * Transforms the specified {@code ptSrc} and stores the result in {@code ptDst}.
     */
    @Override
    public Point2D transform(final Point2D ptSrc, final Point2D ptDst) throws TransformException {
        return AbstractMathTransform2D.transform(this, ptSrc, ptDst);
    }

    /**
     * Transforms the specified shape.
     */
    @Override
    public Shape createTransformedShape(final Shape shape) throws TransformException {
        return AbstractMathTransform2D.createTransformedShape(this, shape, null, null);
    }

    /**
     * Gets the derivative of this transform at a point.
     */
    @Override
    public Matrix derivative(final Point2D point) throws TransformException {
        return AbstractMathTransform2D.derivative(this, point);
    }

    /**
     * Returns the inverse transform of this object.
     */
    @Override
    public MathTransform2D inverse() throws NoninvertibleTransformException {
        return (MathTransform2D) super.inverse();
    }

    /**
     * Invoked at construction time for creating the two-dimensional inverse transform.
     */
    @Override
    Inverse createInverse() throws NoninvertibleTransformException {
        return new Inverse(this);
    }

    /**
     * The inverse of {@link SpecializableTransform2D}.
     */
    static final class Inverse extends SpecializableTransform.Inverse implements MathTransform2D {
        /**
         * Serial number for inter-operability with different versions.
         */
        private static final long serialVersionUID = 366420739633368389L;

        /**
         * Creates a new inverse transform.
         */
        Inverse(SpecializableTransform2D forward) throws NoninvertibleTransformException {
            super(forward);
        }

        /**
         * Transforms the specified {@code ptSrc} and stores the result in {@code ptDst}.
         */
        @Override
        public Point2D transform(final Point2D ptSrc, final Point2D ptDst) throws TransformException {
            return AbstractMathTransform2D.transform(this, ptSrc, ptDst);
        }

        /**
         * Transforms the specified shape.
         */
        @Override
        public Shape createTransformedShape(final Shape shape) throws TransformException {
            return AbstractMathTransform2D.createTransformedShape(this, shape, null, null);
        }

        /**
         * Gets the derivative of this transform at a point.
         */
        @Override
        public Matrix derivative(final Point2D point) throws TransformException {
            return AbstractMathTransform2D.derivative(this, point);
        }

        /**
         * Returns the inverse transform of this object.
         */
        @Override
        public MathTransform2D inverse() {
            return (MathTransform2D) super.inverse();
        }
    }
}
