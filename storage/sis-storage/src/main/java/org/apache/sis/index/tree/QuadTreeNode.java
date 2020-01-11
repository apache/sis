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
package org.apache.sis.index.tree;


/**
 * A node in a {@link QuadTree} which is the parent of other nodes.
 * This class is a specialization of {@link KDTree} for the two-dimensional case.
 * This specialization is provided for reducing the number of objects to create,
 * by storing the 4 quadrants as fields instead than in an array.
 *
 * @author  Chris Mattmann
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.1
 * @module
 */
final class QuadTreeNode extends KDTreeNode {
    /**
     * The 4 quadrants of a {@link QuadTreeNode}: North-West (NW), North-East (NE),
     * South-West (SW) and South-East (SE). Numerical values follow this bit pattern:
     *
     * <ul>
     *   <li>Bit 0 is the sign of <var>x</var> coordinates: 0 for East  and 1 for West.</li>
     *   <li>Bit 1 is the sign of <var>y</var> coordinates: 0 for North and 1 for South.</li>
     * </ul>
     *
     * This pattern is generalizable to <var>n</var> dimensions (by contrast, the use of a {@code Quadrant}
     * enumeration is not generalizable). Current implementation uses only 2 dimensions, but a future version
     * may generalize.
     */
    static final int NE = 0, NW = 1, SE = 2, SW = 3;

    /**
     * The mask to apply on quadrant values for getting the sign of <var>x</var> and <var>y</var>
     * values relative to the center of a node.
     *
     * Also used as bit position + 1 (this is possible only for mask values 1 and 2).
     * If we generalize to 3 or more dimensions, we will need to differentiate those
     * "bit positions" from "mask values".
     */
    private static final int X_MASK = 1, Y_MASK = 2;

    /**
     * Returns the quadrant relative to the given point.
     *
     * @param  x   data <var>x</var> coordinate.
     * @param  y   data <var>y</var> coordinate.
     * @param  cx  center of current node along <var>x</var> axis.
     * @param  cy  center of current node along <var>y</var> axis.
     * @return one of {@link #NE}, {@link #NW}, {@link #SE} or {@link #SW} constants.
     */
    static int quadrant(final double x, final double y, final double cx, final double cy) {
        int q = 0;
        if (x < cx) q  = X_MASK;
        if (y < cy) q |= Y_MASK;
        // Same for z and all other dimensions.
        return q;
    }

    /**
     * Returns 0.5 if the given quadrant is in the East side, or -0.5 if in the West side.
     */
    static double factorX(final int quadrant) {
        /*
         * The 3FE0000000000000 long value is the bit pattern of 0.5. The leftmost bit at (Long.SIZE - 1)
         * is the sign, which we set to the sign encoded in the quadrant value. This approach allow us to
         * get the value efficiently, without jump instructions.
         */
        return Double.longBitsToDouble(0x3FE0000000000000L | (((long) quadrant) << (Long.SIZE - X_MASK)));
    }

    /**
     * Returns 0.5 if the given quadrant is in the North side, or -0.5 if in the South side.
     */
    static double factorY(final int quadrant) {
        /*
         * Same approach than for factorY, except that we need to clear the bits on the right side of
         * the Y mask (it was not necessary for X because they were no bit on the right side of X mask).
         */
        return Double.longBitsToDouble(0x3FE0000000000000L | (((long) (quadrant & Y_MASK)) << (Long.SIZE - Y_MASK)));
    }

    /**
     * The child nodes in 4 quadrants of equal size.
     * Quadrants are North-West, North-East, South-East and South-West.
     */
    private Object nw, ne, se, sw;

    /**
     * Constructs an initially empty parent node.
     */
    QuadTreeNode() {
    }

    /**
     * Returns the child of this node that resides in the specified quadrant.
     *
     * @param  q  quadrant of child to get.
     * @return child in the specified quadrant.
     */
    final Object getChild(final int q) {
        final Object child;
        switch (q) {
            case NW: child = nw; break;
            case NE: child = ne; break;
            case SW: child = sw; break;
            case SE: child = se; break;
            default: throw new AssertionError(q);
        }
        return child;
    }

    /**
     * Sets the node's quadrant to the specified child.
     *
     * @param q      quadrant where the child resides.
     * @param child  child of this node in the specified quadrant.
     */
    final void setChild(final int q, final Object child) {
        switch (q) {
            case NW: nw = child; break;
            case NE: ne = child; break;
            case SW: sw = child; break;
            case SE: se = child; break;
            default: throw new AssertionError(q);
        }
    }
}
