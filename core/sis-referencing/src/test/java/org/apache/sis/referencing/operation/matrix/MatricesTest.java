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
package org.apache.sis.referencing.operation.matrix;

import org.opengis.referencing.operation.Matrix;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link Matrices} implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class MatricesTest extends TestCase {
    /**
     * Tests {@link Matrices#createPassThrough(int, Matrix, int)}.
     */
    @Test
    public void testCreatePassThrough() {
        MatrixSIS matrix = Matrices.create(3, 4, new double[] {
            2, 0, 3, 8,
            0, 4, 7, 5,
            0, 0, 0, 1
        });
        matrix = Matrices.createPassThrough(2, matrix, 1);
        assertTrue(matrix.equals(Matrices.create(6, 7, new double[] {
            1, 0, 0, 0, 0, 0, 0,  // Dimension added
            0, 1, 0, 0, 0, 0, 0,  // Dimension added
            0, 0, 2, 0, 3, 0, 8,  // Sub-matrix, row 0
            0, 0, 0, 4, 7, 0, 5,  // Sub-matrix, row 1
            0, 0, 0, 0, 0, 1, 0,  // Dimension added
            0, 0, 0, 0, 0, 0, 1   // Last sub-matrix row
        })));
    }
}
