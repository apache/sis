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
     * enumeration is not generalizable).
     */
    static final int NE = 0, NW = 1, SE = 2, SW = 3;

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
     * Creates a new instance of the same class than this node.
     */
    @Override
    final KDTreeNode newInstance() {
        return new QuadTreeNode();
    }

    /**
     * Returns the child of this node that resides in the specified quadrant.
     *
     * @param  quadrant  quadrant of child to get.
     * @return child in the specified quadrant.
     */
    @Override
    final Object getChild(final int quadrant) {
        final Object child;
        switch (quadrant) {
            case NW: child = nw; break;
            case NE: child = ne; break;
            case SW: child = sw; break;
            case SE: child = se; break;
            default: throw new IndexOutOfBoundsException(/*quadrant*/);     // TODO: uncomment with JDK9.
        }
        return child;
    }

    /**
     * Sets the node's quadrant to the specified child.
     *
     * @param quadrant  quadrant where the child resides.
     * @param child     child of this node in the specified quadrant.
     */
    @Override
    final void setChild(final int quadrant, final Object child) {
        switch (quadrant) {
            case NW: nw = child; break;
            case NE: ne = child; break;
            case SW: sw = child; break;
            case SE: se = child; break;
            default: throw new IndexOutOfBoundsException(/*quadrant*/);     // TODO: uncomment with JDK9.
        }
    }
}
