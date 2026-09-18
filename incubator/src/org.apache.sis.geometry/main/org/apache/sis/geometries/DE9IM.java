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
package org.apache.sis.geometries;

import java.util.Arrays;
import org.apache.sis.util.ArgumentChecks;


/**
 * A dimensionally extended nine-intersection matrix, the pattern against which
 * {@link Geometry#relate(Geometry, DE9IM)} tests a pair of geometries.
 *
 * <p>Each cell constrains the intersection between a part of the first geometry (the row)
 * and a part of the second geometry (the column). Two orders are defined:</p>
 * <ul>
 *   <li>An <dfn>order 4</dfn> matrix has the {@linkplain Element#CLOSURE closure} and the
 *       {@linkplain Element#EXTERIOR exterior} as rows and columns.</li>
 *   <li>An <dfn>order 9</dfn> matrix has the {@linkplain Element#INTERIOR interior},
 *       the {@linkplain Element#BOUNDARY boundary} and the {@linkplain Element#EXTERIOR exterior}
 *       as rows and columns.</li>
 * </ul>
 *
 * <p>The matrix can be read from and written to a string of 4 or 9 characters in row-major order,
 * such as {@code "TNNNNNFFN"}, or to an integer mask for compact storage. It can also be built
 * cell by cell:</p>
 *
 * {@snippet lang="java" :
 *     DE9IM contains = DE9IM.order9().set(Element.INTERIOR, Element.INTERIOR, Constraint.NON_EMPTY)
 *                                    .set(Element.EXTERIOR, Element.INTERIOR, Constraint.EMPTY)
 *                                    .set(Element.EXTERIOR, Element.BOUNDARY, Constraint.EMPTY);
 *     }
 *
 * <p>Instances of this class are mutable and therefore not thread-safe.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see Geometry#relate(Geometry, DE9IM)
 * @see ISO 19107:2019 - relate & 3Drelate - 6.4.8.8, 6.4.9, 10.8.4, 10.8.5, 10.8.6
 */
public final class DE9IM implements Cloneable {
    /**
     * The part of a geometry designated by a row or a column of the matrix.
     * {@link #INTERIOR}, {@link #BOUNDARY} and {@link #EXTERIOR} apply to matrices of order 9,
     * while {@link #CLOSURE} and {@link #EXTERIOR} apply to matrices of order 4.
     */
    public enum Element {
        /**
         * The geometry without its boundary.
         * Only in matrices of {@linkplain DE9IM#getOrder() order} 9.
         */
        INTERIOR,

        /**
         * The boundary of the geometry.
         * Only in matrices of {@linkplain DE9IM#getOrder() order} 9.
         */
        BOUNDARY,

        /**
         * All the positions which do not belong to the geometry.
         * In matrices of both orders.
         */
        EXTERIOR,

        /**
         * The geometry with its boundary, that is the union of the interior and the boundary.
         * Only in matrices of {@linkplain DE9IM#getOrder() order} 4.
         */
        CLOSURE
    }

    /**
     * The condition that the intersection of a row element with a column element shall meet.
     *
     * <p>Difference with ISO 19107, where a digit requires the intersection to be of that topological
     * dimension <em>at most</em>: the digits are interpreted as requiring <em>exactly</em> that dimension,
     * which is the interpretation of OGC Simple Feature Access and of the usual implementations such as
     * JTS. Consequently {@link #CURVE} for example is not met by an intersection made of points.</p>
     */
    public enum Constraint {
        /**
         * The cell is not tested. Written {@code 'N'}, also accepted as {@code '*'}.
         */
        ANY('N'),

        /**
         * The intersection shall be empty. Written {@code 'F'}.
         */
        EMPTY('F'),

        /**
         * The intersection shall not be empty, whatever its dimension. Written {@code 'T'}.
         */
        NON_EMPTY('T'),

        /**
         * The intersection shall be a set of points, of topological dimension 0.
         * Written {@code '0'}.
         */
        POINT('0'),

        /**
         * The intersection shall be a set of curves, of topological dimension 1.
         * Written {@code '1'}.
         */
        CURVE('1'),

        /**
         * The intersection shall be a set of surfaces, of topological dimension 2.
         * Written {@code '2'}.
         */
        SURFACE('2'),

        /**
         * The intersection shall be a set of solids, of topological dimension 3.
         * Written {@code '3'}.
         */
        SOLID('3');

        /**
         * The character representing this constraint in the string form of a matrix.
         */
        private final char symbol;

        /**
         * Creates a new constraint represented by the given character.
         */
        private Constraint(final char symbol) {
            this.symbol = symbol;
        }

        /**
         * Returns the character representing this constraint in the string form of a matrix.
         *
         * @return the symbol of this constraint.
         */
        public char symbol() {
            return symbol;
        }

        /**
         * Returns the constraint represented by the given character.
         * Lower cases are accepted, and {@code '*'} is synonymous of {@code 'N'}.
         *
         * @param  symbol  the character to decode.
         * @return the constraint represented by the given character.
         * @throws IllegalArgumentException if the given character is not a valid symbol.
         */
        public static Constraint forSymbol(final char symbol) {
            if (symbol == '*') {
                return ANY;
            }
            final char c = Character.toUpperCase(symbol);
            for (final Constraint candidate : VALUES) {
                if (candidate.symbol == c) {
                    return candidate;
                }
            }
            throw new IllegalArgumentException("Unknown DE-9IM symbol: " + symbol);
        }

        /**
         * Returns whether an intersection of the given topological dimension meets this constraint.
         *
         * @param  dimension  topological dimension of the intersection, or -1 if the intersection is empty.
         * @return whether the given dimension meets this constraint.
         */
        public boolean accept(final int dimension) {
            switch (this) {
                case ANY:       return true;
                case EMPTY:     return dimension < 0;
                case NON_EMPTY: return dimension >= 0;
                default:        return dimension == (symbol - '0');
            }
        }

        /**
         * All constraints, fetched only once because this array is not cloned.
         */
        private static final Constraint[] VALUES = values();
    }

    /**
     * Number of bits used by the encoding of a single cell in the {@linkplain #toInt() integer form}.
     * The 7 {@linkplain Constraint constraints} are encoded by their ordinal value.
     */
    private static final int BITS_PER_CELL = 3;

    /**
     * Mask of the bits used by the encoding of a single cell in the {@linkplain #toInt() integer form}.
     */
    private static final int CELL_MASK = (1 << BITS_PER_CELL) - 1;

    /**
     * Bit set in the {@linkplain #toInt() integer form} of a matrix of order 9.
     * This is the bit after the 9 cells of {@value #BITS_PER_CELL} bits each.
     */
    private static final int ORDER_9_FLAG = 1 << (9 * BITS_PER_CELL);

    /**
     * Number of rows and columns: 2 for a matrix of order 4, or 3 for a matrix of order 9.
     */
    private final int side;

    /**
     * Constraint on each cell, in row-major order. The length of this array is {@link #side} squared.
     * Never {@code null} and never contains null elements.
     */
    private final Constraint[] cells;

    /**
     * Creates a new matrix with the given number of rows and columns, with all cells untested.
     */
    private DE9IM(final int side) {
        this.side  = side;
        this.cells = new Constraint[side * side];
        Arrays.fill(cells, Constraint.ANY);
    }

    /**
     * Creates a copy of the given matrix.
     */
    private DE9IM(final DE9IM other) {
        side  = other.side;
        cells = other.cells.clone();
    }

    /**
     * Creates a matrix of order 4 with all cells untested.
     * The rows and columns are the {@linkplain Element#CLOSURE closure}
     * and the {@linkplain Element#EXTERIOR exterior} of the geometries.
     *
     * @return a new matrix of order 4 accepting all geometry pairs.
     */
    public static DE9IM order4() {
        return new DE9IM(2);
    }

    /**
     * Creates a matrix of order 9 with all cells untested.
     * The rows and columns are the {@linkplain Element#INTERIOR interior},
     * the {@linkplain Element#BOUNDARY boundary} and the {@linkplain Element#EXTERIOR exterior}
     * of the geometries.
     *
     * @return a new matrix of order 9 accepting all geometry pairs.
     */
    public static DE9IM order9() {
        return new DE9IM(3);
    }

    /**
     * Creates a matrix from its string form: 4 or 9 characters in row-major order.
     * Each character is a {@linkplain Constraint#symbol() constraint symbol}.
     *
     * @param  pattern  the pattern of 4 or 9 characters to decode.
     * @return the matrix represented by the given pattern.
     * @throws IllegalArgumentException if the given pattern has an invalid length or contains an invalid symbol.
     *
     * @see #toString()
     */
    public static DE9IM valueOf(final String pattern) {
        ArgumentChecks.ensureNonNull("pattern", pattern);
        final int side;
        switch (pattern.length()) {
            case 4:  side = 2; break;
            case 9:  side = 3; break;
            default: throw new IllegalArgumentException("A DE-9IM pattern shall have 4 or 9 characters, "
                            + "but the given pattern has " + pattern.length() + " of them.");
        }
        final DE9IM matrix = new DE9IM(side);
        for (int i=0; i<matrix.cells.length; i++) {
            matrix.cells[i] = Constraint.forSymbol(pattern.charAt(i));
        }
        return matrix;
    }

    /**
     * Creates a matrix from its integer form.
     *
     * @param  code  the integer mask to decode.
     * @return the matrix represented by the given mask.
     * @throws IllegalArgumentException if the given mask is not a valid encoding.
     *
     * @see #toInt()
     */
    public static DE9IM valueOf(final int code) {
        final DE9IM matrix = new DE9IM((code & ORDER_9_FLAG) != 0 ? 3 : 2);
        final int n = matrix.cells.length;
        int remaining = code & ~ORDER_9_FLAG;
        for (int i=0; i<n; i++) {
            final int ordinal = remaining & CELL_MASK;
            if (ordinal >= Constraint.VALUES.length) {
                throw new IllegalArgumentException("Invalid constraint code " + ordinal + " at cell " + i + '.');
            }
            matrix.cells[i] = Constraint.VALUES[ordinal];
            remaining >>>= BITS_PER_CELL;
        }
        if (remaining != 0) {
            throw new IllegalArgumentException("The given code has bits set outside the "
                    + n + " cells of the matrix: " + code);
        }
        return matrix;
    }

    /**
     * Returns the number of cells in this matrix.
     *
     * @return the order of this matrix, either 4 or 9.
     */
    public int getOrder() {
        return cells.length;
    }

    /**
     * Returns the index of the given element in a row or a column of this matrix.
     *
     * @param  element  the element for which to get the index.
     * @param  name     the argument name, for the error message if the element is invalid.
     * @return index of the given element, from 0 inclusive to {@link #side} exclusive.
     * @throws IllegalArgumentException if the given element does not apply to the order of this matrix.
     */
    private int indexOf(final Element element, final String name) {
        ArgumentChecks.ensureNonNull(name, element);
        if (side == 3) {
            switch (element) {
                case INTERIOR: return 0;
                case BOUNDARY: return 1;
                case EXTERIOR: return 2;
            }
        } else {
            switch (element) {
                case CLOSURE:  return 0;
                case EXTERIOR: return 1;
            }
        }
        throw new IllegalArgumentException("Element " + element + " given as \"" + name
                + "\" does not apply to a matrix of order " + cells.length + '.');
    }

    /**
     * Returns the constraint applied on the intersection of the given parts of the two geometries.
     *
     * @param  row     the part of the first geometry.
     * @param  column  the part of the second geometry.
     * @return the constraint applied on that intersection.
     * @throws IllegalArgumentException if an element does not apply to the order of this matrix.
     */
    public Constraint get(final Element row, final Element column) {
        return cells[indexOf(row, "row") * side + indexOf(column, "column")];
    }

    /**
     * Sets the constraint applied on the intersection of the given parts of the two geometries.
     *
     * @param  row         the part of the first geometry.
     * @param  column      the part of the second geometry.
     * @param  constraint  the constraint to apply on that intersection.
     * @return {@code this} for method call chaining.
     * @throws IllegalArgumentException if an element does not apply to the order of this matrix.
     */
    public DE9IM set(final Element row, final Element column, final Constraint constraint) {
        ArgumentChecks.ensureNonNull("constraint", constraint);
        cells[indexOf(row, "row") * side + indexOf(column, "column")] = constraint;
        return this;
    }

    /**
     * Sets all cells of this matrix to the given constraint.
     *
     * @param  constraint  the constraint to apply on all intersections.
     * @return {@code this} for method call chaining.
     */
    public DE9IM setAll(final Constraint constraint) {
        ArgumentChecks.ensureNonNull("constraint", constraint);
        Arrays.fill(cells, constraint);
        return this;
    }

    /**
     * Returns whether the given intersection dimensions meet all the constraints of this matrix.
     * The given array holds the topological dimension of the intersection of each part of the first
     * geometry (the rows) with each part of the second geometry (the columns), in the interior,
     * boundary and exterior order used by matrices of order 9. A negative value means that the
     * intersection is empty.
     *
     * <p>If this matrix is of order 4, the dimensions of the closures are derived from the given
     * dimensions: the closure is the union of the interior and the boundary, therefore the dimension
     * of its intersection with another part is the greatest dimension of the parts of that union.</p>
     *
     * @param  dimensions  topological dimension of the 9 intersections in row-major order,
     *         with a negative value for an empty intersection.
     * @return whether the given dimensions meet all the constraints of this matrix.
     * @throws IllegalArgumentException if the given array does not have a length of 9.
     */
    public boolean matches(final int... dimensions) {
        ArgumentChecks.ensureNonNull("dimensions", dimensions);
        if (dimensions.length != 9) {
            throw new IllegalArgumentException("Expected the dimensions of 9 intersections, but got "
                    + dimensions.length + " of them.");
        }
        if (side == 3) {
            for (int i=0; i<cells.length; i++) {
                if (!cells[i].accept(dimensions[i])) {
                    return false;
                }
            }
        } else {
            /*
             * A row or a column of index 0 is the closure, which merges the interior (index 0) and
             * the boundary (index 1) of the order 9 matrix. A row or a column of index 1 is the
             * exterior, which is at index 2 in the order 9 matrix. Since the closure is the union
             * of the interior and the boundary, the dimension of an intersection with the closure
             * is the greatest dimension of the intersections with those two parts.
             */
            for (int i=0; i<cells.length; i++) {
                final boolean rowIsClosure    = (i / side) == 0;
                final boolean columnIsClosure = (i % side) == 0;
                int dimension = -1;
                for (int r = rowIsClosure ? 0 : 2; r <= (rowIsClosure ? 1 : 2); r++) {
                    for (int c = columnIsClosure ? 0 : 2; c <= (columnIsClosure ? 1 : 2); c++) {
                        dimension = Math.max(dimension, dimensions[r*3 + c]);
                    }
                }
                if (!cells[i].accept(dimension)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns the integer form of this matrix. Each cell is encoded on {@value #BITS_PER_CELL} bits
     * in row-major order, the first cell being in the lowest bits, and an additional bit tells whether
     * the matrix is of order 9. This form is more compact than the string form but is specific to
     * Apache SIS; it is not defined by any standard.
     *
     * @return the integer form of this matrix.
     *
     * @see #valueOf(int)
     */
    public int toInt() {
        int code = (side == 3) ? ORDER_9_FLAG : 0;
        for (int i=0; i<cells.length; i++) {
            code |= cells[i].ordinal() << (i * BITS_PER_CELL);
        }
        return code;
    }

    /**
     * Returns the string form of this matrix: the {@linkplain Constraint#symbol() symbol} of each cell
     * in row-major order. Untested cells are written {@code 'N'}, which is the symbol used by ISO 19107,
     * but {@code '*'} is also accepted when {@linkplain #valueOf(String) reading} a pattern.
     *
     * @return the string form of this matrix, of 4 or 9 characters.
     *
     * @see #valueOf(String)
     */
    @Override
    public String toString() {
        final char[] symbols = new char[cells.length];
        for (int i=0; i<symbols.length; i++) {
            symbols[i] = cells[i].symbol;
        }
        return new String(symbols);
    }

    /**
     * Returns a copy of this matrix which can be modified without impacting this instance.
     *
     * @return a copy of this matrix.
     */
    @Override
    public DE9IM clone() {
        return new DE9IM(this);
    }

    /**
     * Compares this matrix with the given object for equality.
     *
     * @param  other  the object to compare with this matrix, or {@code null}.
     * @return whether the given object is a matrix of the same order with the same constraints.
     */
    @Override
    public boolean equals(final Object other) {
        return (other instanceof DE9IM) && Arrays.equals(cells, ((DE9IM) other).cells);
    }

    /**
     * Returns a hash code value for this matrix.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(cells) ^ side;
    }
}
