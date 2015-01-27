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

import java.util.Map;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.PassThroughTransform;
import org.apache.sis.internal.util.Citations;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Characters;
import org.apache.sis.util.Static;


/**
 * Utility methods related to {@link OperationMethod} and {@link MathTransform} instances.
 * Not in public API because the contract of those methods is not clear or is disputable.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
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
     * Determines whether a match or mismatch is found between the two given collections of identifiers.
     * If any of the given collections is {@code null} or empty, this method returns {@code null}.
     *
     * <p>According ISO 19162 (<cite>Well known text representation of coordinate reference systems</cite>),
     * {@linkplain AbstractIdentifiedObject#getIdentifier() identifiers} should have precedence over
     * {@linkplain AbstractIdentifiedObject#getName() name} for identifying {@code IdentifiedObject}s,
     * at least in the case of {@linkplain org.apache.sis.referencing.operation.DefaultOperationMethod
     * operation methods} and {@linkplain org.apache.sis.parameter.AbstractParameterDescriptor parameters}.</p>
     *
     * @param  id1 The first collection of identifiers, or {@code null}.
     * @param  id2 The second collection of identifiers, or {@code null}.
     * @return {@code TRUE} or {@code FALSE} on match or mismatch respectively, or {@code null} if this method
     *         can not determine if there is a match or mismatch.
     */
    public static Boolean hasCommonIdentifier(final Iterable<? extends Identifier> id1,
                                              final Iterable<? extends Identifier> id2)
    {
        if (id1 != null && id2 != null) {
            boolean hasFound = false;
            for (final Identifier identifier : id1) {
                final Citation authority = identifier.getAuthority();
                final String   codeSpace = identifier.getCodeSpace();
                for (final Identifier other : id2) {
                    if (authorityMatches(identifier, authority, codeSpace)) {
                        if (CharSequences.equalsFiltered(identifier.getCode(), other.getCode(), Characters.Filter.UNICODE_IDENTIFIER, true)) {
                            return Boolean.TRUE;
                        }
                        hasFound = true;
                    }
                }
            }
            if (hasFound) {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if the given identifier authority matches the given {@code authority}.
     * If one of the authority is null, then the comparison fallback on the given {@code codeSpace}.
     * If the code spaces are also null, then this method conservatively returns {@code false}.
     *
     * @param  identifier The identifier to compare.
     * @param  authority  The desired authority, or {@code null}.
     * @param  codeSpace  The desired code space or {@code null}, used as a fallback if an authority is null.
     * @return {@code true} if the authority or code space (as a fallback only) matches.
     */
    private static boolean authorityMatches(final Identifier identifier, final Citation authority, final String codeSpace) {
        if (authority != null) {
            final Citation other = identifier.getAuthority();
            if (other != null) {
                return Citations.identifierMatches(authority, other);
            }
        }
        if (codeSpace != null) {
            final String other = identifier.getCodeSpace();
            if (other != null) {
                return CharSequences.equalsFiltered(codeSpace, other, Characters.Filter.UNICODE_IDENTIFIER, true);
            }
        }
        return false;
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
     * @param  method     The operation method to compare to the math transform.
     * @param  transform  The math transform to compare to the operation method.
     * @param  properties Properties of the caller object being constructed, used only for formatting error message.
     * @throws IllegalArgumentException if the number of dimensions are incompatible.
     */
    public static void checkDimensions(final OperationMethod method, MathTransform transform,
            final Map<String,?> properties) throws IllegalArgumentException
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
        byte isTarget = 0; // false: wrong dimension is the source one.
        if (expected == null || actual == expected) {
            actual = transform.getTargetDimensions();
            expected = method.getTargetDimensions();
            if (expected == null || actual == expected) {
                return;
            }
            isTarget = 1; // true: wrong dimension is the target one.
        }
        throw new IllegalArgumentException(Errors.getResources(properties).getString(
                Errors.Keys.MismatchedTransformDimension_3, isTarget, expected, actual));
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
