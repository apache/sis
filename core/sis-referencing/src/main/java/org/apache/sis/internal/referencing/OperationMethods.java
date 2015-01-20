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
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.PassThroughTransform;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Static;


/**
 * Utility methods related to {@link OperationMethod} and {@link MathTransform} instances.
 * Not in public API because the contract of those methods is not clear or is disputable.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5 (derived from geotk-3.20)
 * @version 0.5
 * @module
 */
public final class OperationMethods extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private OperationMethods() {
    }

    /**
     * Checks if an operation method and a math transform have a compatible number of source and target dimensions.
     * In the particular case of a {@linkplain PassThroughTransform pass through transform} with more dimensions
     * than what we would expect from the given method, the check will rather be performed against the
     * {@linkplain PassThroughTransform#getSubTransform() sub transform}.
     *
     * <p>The intend is to allow creation of a three-dimensional {@code ProjectedCRS} with a two-dimensional
     * {@code OperationMethod}, where the third-dimension just pass through. This is not a recommended approach
     * and we do not document that as a supported feature, but we do not prevent it neither.</p>
     *
     * <p>This method tries to locates what seems to be the "core" of the given math transform. The definition
     * of "core" is imprecise and may be adjusted in future SIS versions. The current algorithm is as below:</p>
     *
     * <ul>
     *   <li>If the given transform can be decomposed in {@linkplain MathTransforms#getSteps(MathTransform) steps},
     *       then the steps for {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes axis
     *       swapping and scaling} are ignored.</li>
     *   <li>If the given transform or its non-ignorable step is a {@link PassThroughTransform}, then its sub-transform
     *       is taken. Only one non-ignorable step may exist, otherwise we do not try to select any of them.</li>
     * </ul>
     *
     * @param  method    The operation method to compare to the math transform.
     * @param  transform The math transform to compare to the operation method.
     * @throws IllegalArgumentException if the number of dimensions are incompatible.
     */
    public static void checkDimensions(final OperationMethod method, MathTransform transform)
            throws IllegalArgumentException
    {
        int actual = transform.getSourceDimensions();
        Integer expected = method.getSourceDimensions();
        if (expected != null && actual > expected) {
            /*
             * The given MathTransform use more dimensions than the OperationMethod.
             * Try to locate one and only one sub-transform, ignoring axis swapping and scaling.
             */
            MathTransform subTransform = null;
            for (final MathTransform step : MathTransforms.getSteps(transform)) {
                if (!isIgnorable(step)) {
                    if (subTransform == null && step instanceof PassThroughTransform) {
                        subTransform = ((PassThroughTransform) step).getSubTransform();
                    } else {
                        subTransform = null;
                        break;
                    }
                }
            }
            if (subTransform != null) {
                transform = subTransform;
                actual = transform.getSourceDimensions();
            }
        }
        /*
         * Now verify if the MathTransform dimensions are equal to the OperationMethod ones,
         * ignoring null java.lang.Integer instances.
         */
        final String name;
        if (expected == null || actual == expected) {
            actual = transform.getTargetDimensions();
            expected = method.getTargetDimensions();
            if (expected == null || actual == expected) {
                return;
            }
            name = "transform.target";
        } else {
            name = "transform.source";
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedDimension_3, name, expected, actual));
    }

    /**
     * Returns {@code true} if the specified transform is likely to exists only for axis swapping
     * and/or unit conversions. The heuristic rule checks if the transform is backed by a square
     * matrix with exactly one non-null value in each row and each column.
     */
    private static boolean isIgnorable(final MathTransform transform) {
        final Matrix matrix = MathTransforms.getMatrix(transform);
        if (matrix != null) {
            final int size = matrix.getNumRow();
            if (matrix.getNumCol() == size) {
                for (int j=0; j<size; j++) {
                    int n1=0, n2=0;
                    for (int i=0; i<size; i++) {
                        if (matrix.getElement(j,i) != 0) n1++;
                        if (matrix.getElement(i,j) != 0) n2++;
                    }
                    if (n1 != 1 || n2 != 1) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
}
