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
package org.apache.sis.internal.referencing;

import org.opengis.referencing.operation.Matrix;


/**
 * A matrix capable to store extended precision elements. Apache SIS uses double-double arithmetic
 * for extended precision, but we want to hide that implementation details from public API.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
 * @module
 */
public interface ExtendedPrecisionMatrix extends Matrix {
    /**
     * A sentinel value for {@link org.apache.sis.referencing.operation.matrix.Matrices#create(int, int, Number[])}
     * meaning that we request an extended precision matrix initialized to the identity (or diagonal) matrix.
     * This is a non-public feature because we try to hide our extended-precision mechanism from the users.
     */
    Number[] IDENTITY = new Number[0];

    /**
     * Returns a copy of all matrix elements, potentially followed by the error terms for extended-precision arithmetic.
     * Matrix elements are returned in a flat, row-major (column indices vary fastest) array.
     *
     * <p>In <cite>extended precision mode</cite>, the length of this array is actually twice the normal length.
     * The first half contains {@link org.apache.sis.internal.util.DoubleDouble#value}, and the second half contains
     * the {@link org.apache.sis.internal.util.DoubleDouble#error} for each value in the first half.</p>
     *
     * @return A copy of matrix elements, potentially followed by error terms.
     */
    double[] getExtendedElements();
}
