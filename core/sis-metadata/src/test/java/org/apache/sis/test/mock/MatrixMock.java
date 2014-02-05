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
package org.apache.sis.test.mock;

import java.io.Serializable;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.util.ArgumentChecks;

import static org.junit.Assert.assertEquals;


/**
 * A dummy implementation of {@link Matrix}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@SuppressWarnings("serial")
public final strictfp class MatrixMock implements Matrix, Cloneable, Serializable {
    /**
     * Matrix size.
     */
    private final int numRow, numCol;

    /**
     * Matrix elements.
     */
    private double[] elements;

    /**
     * Creates a new matrix of the given size with the given elements.
     *
     * @param numRow   Number of matrix rows.
     * @param numCol   Number of matrix columns.
     * @param elements Matrix element values.
     */
    public MatrixMock(final int numRow, final int numCol, final double... elements) {
        assertEquals(numRow * numCol, elements.length);
        this.numRow = numRow;
        this.numCol = numCol;
        this.elements = elements;
    }

    /**
     * Returns the number of rows.
     *
     * @return Number of matrix rows.
     */
    @Override
    public int getNumRow() {
        return numRow;
    }

    /**
     * Returns the number of columns.
     *
     * @return Number of matrix columns.
     */
    @Override
    public int getNumCol() {
        return numCol;
    }

    /**
     * Return the index of the matrix element.
     */
    private int index(final int row, final int column) {
        ArgumentChecks.ensureBetween("row",    0, numRow - 1, row);
        ArgumentChecks.ensureBetween("column", 0, numCol - 1, column);
        return row * numCol + column;
    }

    /**
     * Returns the element at the given row and column.
     *
     * @param  row    Row index.
     * @param  column Colum index.
     * @return Matrix element at the given indices.
     */
    @Override
    public double getElement(final int row, final int column) {
        return elements[index(row, column)];
    }

    /**
     * Sets the element at the given row and column.
     *
     * @param row    Row index.
     * @param column Colum index.
     * @param value  Matrix element at the given indices.
     */
    @Override
    public void setElement(final int row, final int column, final double value) {
        elements[index(row, column)] = value;
    }

    /**
     * Returns {@code true} if this matrix is the identity matrix.
     *
     * @return {@code true} for the identity matrix.
     */
    @Override
    public boolean isIdentity() {
        if (numRow != numCol) {
            return false;
        }
        for (int i=0; i<elements.length; i++) {
            if (elements[i] != ((i % numCol) == 0 ? 1 : 0)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a clone of this matrix.
     *
     * @return A clone of this matrix.
     */
    @Override
    public Matrix clone() {
        final MatrixMock copy;
        try {
            copy = (MatrixMock) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
        copy.elements = copy.elements.clone();
        return copy;
    }
}
