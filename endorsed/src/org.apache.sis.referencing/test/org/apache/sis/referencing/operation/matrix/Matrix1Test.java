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

import static org.apache.sis.referencing.operation.matrix.Matrix1.SIZE;

// Test dependencies
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.DependsOn;


/**
 * Tests the {@link Matrix1} implementation.
 * This class inherits all tests defined in {@link MatrixTestCase}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@DependsOn(SolverTest.class)
public final class Matrix1Test extends MatrixTestCase {
    /**
     * Creates a new test case.
     */
    public Matrix1Test() {
    }

    /**
     * Returns the size of the matrix of interest for this test class.
     */
    @Override int getNumRow() {return SIZE;}
    @Override int getNumCol() {return SIZE;}

    /**
     * Ensures that the given matrix is an instance of the expected type.
     */
    @Override
    void validateImplementation(final MatrixSIS matrix) {
        super.validateImplementation(matrix);
        assertEquals(Matrix1.class, matrix.getClass());
    }

    /**
     * Tests the {@link Matrix1#Matrix1(double)} constructor.
     * This constructor is specific to the implementation class.
     */
    @Test
    public void testConstructor() {
        initialize(415870088589607716L);
        final double[] elements = createRandomPositiveValues(SIZE * SIZE);
        final Matrix1 matrix = new Matrix1(
                elements[0]);
        validateImplementation(matrix);
        assertArrayEquals(elements, matrix.getElements());
    }
}
