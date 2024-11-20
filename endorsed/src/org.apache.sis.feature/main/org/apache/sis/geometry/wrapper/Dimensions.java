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
package org.apache.sis.geometry.wrapper;

import org.apache.sis.feature.internal.Resources;


/**
 * Identification of which dimensions are contained in a sequence of coordinate tuples.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public enum Dimensions {
    /**
     * Two-dimensional tuples with <var>x</var>, <var>y</var> coordinates.
     */
    XY(false, false, (byte) Geometries.BIDIMENSIONAL),

    /**
     * Three-dimensional tuples with <var>x</var>, <var>y</var>, <var>z</var> coordinates.
     */
    XYZ(true, false, (byte) Geometries.TRIDIMENSIONAL),

    /**
     * Three-dimensional tuples with <var>x</var>, <var>y</var>, <var>m</var> coordinates.
     */
    XYM(false, true, (byte) Geometries.TRIDIMENSIONAL),

    /**
     * Four-dimensional tuples with <var>x</var>, <var>y</var>, <var>z</var>, <var>m</var> coordinates.
     */
    XYZM(true, true, (byte) 4);

    /**
     * Whether this set of dimensions include the <var>z</var> coordinate.
     */
    public final boolean hasZ;

    /**
     * Whether this set of dimensions includes the <var>m</var> coordinate.
     */
    public final boolean hasM;

    /**
     * Number of dimensions: 2, 3 or 4.
     */
    public final byte count;

    /**
     * Creates a new enumeration value for the given number of dimensions.
     */
    private Dimensions(final boolean hasZ, final boolean hasM, final byte count) {
        this.hasZ  = hasZ;
        this.hasM  = hasM;
        this.count = count;
    }

    /**
     * Returns the enumeration value for the given support of <var>z</var> and <var>m</var> coordinates.
     *
     * @param  hasZ  whether the <var>z</var> coordinate is included.
     * @param  hasM  whether the <var>m</var> coordinate is included.
     * @return enumeration value for the given included coordinates.
     */
    public static Dimensions forZorM(final boolean hasZ, final boolean hasM) {
        return hasZ ? (hasM ? XYZM : XYZ) : (hasM ? XYM : XY);
    }

    /**
     * Returns an enumeration value for the given number of dimensions.
     *
     * @param  count    the number of dimensions.
     * @param  preferM  the coordinate to use if the number of dimensions is 3:
     *         {@code false} for <var>z</var> or {@code true} for <var>m</var>.
     * @return enumeration for the given number of dimensions.
     * @throws IllegalArgumentException if the given number if less than 2 or more than 3.
     */
    public static Dimensions forCount(final int count, final boolean preferM) {
        switch (count) {
            case Geometries.BIDIMENSIONAL:  return XY;
            case Geometries.TRIDIMENSIONAL: return preferM ? XYM : XYZ;
            case 4:  return XYZM;
            default: throw new IllegalArgumentException(Resources.format(Resources.Keys.UnsupportedGeometryObject_1, count));
        }
    }
}
