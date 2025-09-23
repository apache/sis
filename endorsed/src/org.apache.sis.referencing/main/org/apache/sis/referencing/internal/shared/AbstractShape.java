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
package org.apache.sis.referencing.internal.shared;

import java.awt.Shape;


/**
 * Base class for some (not all) shape implementations in Apache SIS.
 * This base class provides a mechanism for determining if a shape stores
 * coordinate values as simple-precision or double-precision floating point numbers.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 */
public abstract class AbstractShape implements Shape {
    /**
     * Base classes of Java2D implementations known to store coordinates
     * as single-precision floating point numbers.
     */
    private static final Class<?>[] FLOAT_SHAPES = {
        java.awt.geom.Point2D.Float.class,
        java.awt.geom.Line2D.Float.class,
        java.awt.geom.QuadCurve2D.Float.class,
        java.awt.geom.CubicCurve2D.Float.class,
        java.awt.geom.Rectangle2D.Float.class,
        java.awt.geom.RoundRectangle2D.Float.class,
        java.awt.geom.Arc2D.Float.class,
        java.awt.geom.Ellipse2D.Float.class,
        java.awt.geom.Path2D.Float.class
    };

    /**
     * Creates a new shape.
     */
    protected AbstractShape() {
    }

    /**
     * Returns {@code true} if this shape backed by primitive {@code float} values.
     *
     * @return {@code true} if this shape is backed by {@code float} coordinate values.
     */
    protected boolean isFloat() {
        return false;
    }

    /**
     * Returns {@code true} if the given shape is presumed backed by primitive {@code float} values.
     * The given object should be an instance of {@link Shape} or {@link java.awt.geom.Point2D}.
     *
     * @param  shape  the shape for which to determine the backing primitive type.
     * @return {@code true} if the given shape is presumed backed by {@code float} coordinate values.
     */
    public static boolean isFloat(final Object shape) {
        if (shape instanceof AbstractShape) {
            return ((AbstractShape) shape).isFloat();
        }
        for (final Class<?> c : FLOAT_SHAPES) {
            if (c.isInstance(shape)) {
                return true;
            }
        }
        return false;
    }
}
