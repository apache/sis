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
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;


/**
 * A Molodensky transforms for two-dimensional input and output coordinates.
 *
 * @author  Rueben Schulz (UBC)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class MolodenskyTransform2D extends MolodenskyTransform implements MathTransform2D {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -7205339479409468581L;

    /**
     * Constructs a 2D transform.
     */
    MolodenskyTransform2D(final Ellipsoid source, final Ellipsoid target,
                          final double tX, final double tY, final double tZ,
                          final boolean isAbridged)
    {
        super(source, false, target, false, tX, tY, tZ, isAbridged);
    }

    /**
     * Constructs the inverse of a 2D transform.
     *
     * @param inverse The transform for which to create the inverse.
     * @param source  The source ellipsoid of the given {@code inverse} transform.
     * @param target  The target ellipsoid of the given {@code inverse} transform.
     */
    MolodenskyTransform2D(final MolodenskyTransform inverse, final Ellipsoid source, final Ellipsoid target) {
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
