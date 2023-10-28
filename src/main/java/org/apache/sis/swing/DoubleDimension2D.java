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
package org.apache.sis.swing;

import java.io.Serializable;
import java.awt.geom.Dimension2D;
import static java.lang.Double.doubleToLongBits;
import org.apache.sis.util.internal.Numerics;


/**
 * Implements {@link Dimension2D} using double-precision floating point values.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @version 1.1
 * @since   1.1
 */
final class DoubleDimension2D extends Dimension2D implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3603763914115376884L;

    /**
     * The width.
     */
    public double width;

    /**
     * The height.
     */
    public double height;

    /**
     * Constructs a new dimension initialized to (0,0).
     */
    public DoubleDimension2D() {
    }

    /**
     * Constructs a new dimension initialized to the given dimension.
     *
     * @param  dimension  the dimension to copy.
     */
    public DoubleDimension2D(final Dimension2D dimension) {
        width  = dimension.getWidth();
        height = dimension.getHeight();
    }

    /**
     * Constructs a new dimension with the specified values.
     *
     * @param  w  the width.
     * @param  h  the height.
     */
    public DoubleDimension2D(final double w, final double h) {
        width  = w;
        height = h;
    }

    /**
     * Sets width and height for this dimension.
     *
     * @param  w  the width.
     * @param  h  the height.
     */
    @Override
    public void setSize(final double w, final double h) {
        width  = w;
        height = h;
    }

    /**
     * Returns the width.
     */
    @Override
    public double getWidth() {
        return width;
    }

    /**
     * Returns the height.
     */
    @Override
    public double getHeight() {
        return height;
    }

    /**
     * Returns a hash code value for this dimension.
     */
    @Override
    public int hashCode() {
        final long code = doubleToLongBits(width) + 31*doubleToLongBits(height);
        return (int) code ^ (int) (code >>> 32) ^ (int) serialVersionUID;
    }

    /**
     * Compares this dimension with the given object for equality.
     *
     * @param  object  the object to compare with.
     * @return {@code true} if this dimension is equal to the given object.
     */
    @Override
    public boolean equals(final Object object) {
        if (object instanceof DoubleDimension2D) {
            final DoubleDimension2D that = (DoubleDimension2D) object;
            return Numerics.equals(width,  that.width) &&
                   Numerics.equals(height, that.height);
        }
        return false;
    }

    /**
     * Returns a string representation of this dimension.
     */
    @Override
    public String toString() {
        return "Dimension2D[" + width + ", " + height + ']';
    }
}
