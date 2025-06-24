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
package org.apache.sis.coverage.internal;

import java.util.Arrays;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.TransformJoiner;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ArraysExt;


/**
 * A transform composed of an arbitrary number of juxtaposed transforms.
 *
 * This implementation is sufficient for {@code org.apache.sis.feature} purposes,
 * but incomplete for {@code org.apache.sis.referencing} purposes.
 * See <a href="https://issues.apache.org/jira/browse/SIS-498">SIS-498</a>.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class CompoundTransform extends AbstractMathTransform {
    /**
     * The inverse, created when first needed.
     */
    private transient MathTransform inverse;

    /**
     * Creates a new compound transforms.
     */
    CompoundTransform() {
    }

    /**
     * Returns the component transforms that are juxtaposed in this compound transform.
     * This method may return a direct reference to an internal array; callers shall not modify that array.
     */
    abstract MathTransform[] components();

    /**
     * Creates a new transform made of the given components.
     *
     * @todo Current implementation requires that given transforms are {@link MathTransform1D} instances.
     *
     * @param  components  transforms to juxtapose for defining a new transform.
     * @return compound transforms with the given components.
     */
    public static MathTransform create(final MathTransform[] components) {
        final int n = components.length;
        if (n == 0) {
            return MathTransforms.identity(0);
        }
        final MathTransform first = components[0];
        ArgumentChecks.ensureNonNullElement("components", 0, first);
        if (n == 1) {
            return first;
        }
        /*
         * TODO: we should check here if there is consecutive linear transforms that we can combine in a single matrix.
         *       Code for doing that may be found in PassthroughTransform. Doing this optimization requires a general
         *       (non 1D) implementation of CompoundTransform.
         */
        if (ArraysExt.allEquals(components, first)) {
            return new RepeatedTransform(first, n);
        }
        final MathTransform1D[] as1D = new MathTransform1D[n];
        for (int i=0; i<n; i++) {
            final MathTransform c = components[i];
            ArgumentChecks.ensureNonNullElement("components", i, c);
            if (c instanceof MathTransform1D) {
                as1D[i] = (MathTransform1D) c;
            } else {
                /*
                 * TODO: if we have a nested CompoundTransform, we need to unwrap its components.
                 *       For all other types, we need a general (non 1D) implementation.
                 */
                throw new UnsupportedOperationException("Non 1D-case not yet implemented.");
            }
        }
        return new CompoundTransformOf1D(as1D);
    }

    /**
     * Returns the number of source dimensions of this compound transform.
     * This is the sum of the number of source dimensions of all components.
     */
    @Override
    public int getSourceDimensions() {
        int dim = 0;
        for (final MathTransform c : components()) {
            dim += c.getSourceDimensions();
        }
        return dim;
    }

    /**
     * Returns the number of target dimensions of this compound transform.
     * This is the sum of the number of target dimensions of all components.
     */
    @Override
    public int getTargetDimensions() {
        int dim = 0;
        for (final MathTransform c : components()) {
            dim += c.getTargetDimensions();
        }
        return dim;
    }

    /**
     * Tests whether this transform does not move any points.
     *
     * @return {@code true} if all transform components are identity.
     */
    @Override
    public boolean isIdentity() {
        for (final MathTransform c : components()) {
            if (!c.isIdentity()) return false;
        }
        return true;
    }

    /**
     * Returns the inverse transform of this transform.
     *
     * @return the inverse of this transform.
     * @throws NoninvertibleTransformException if at least one component transform cannot be inverted.
     */
    @Override
    public final synchronized MathTransform inverse() throws NoninvertibleTransformException {
        if (inverse == null) {
            final MathTransform[] components = components();
            final MathTransform[] inverses = new MathTransform1D[components.length];
            for (int i=0; i<components.length; i++) {
                inverses[i] = components[i].inverse();
            }
            inverse = create(inverses);
        }
        return inverse;
    }

    /**
     * Concatenates or pre-concatenates in an optimized way this math transform with the given one, if possible.
     */
    @Override
    protected final void tryConcatenate(final TransformJoiner context) throws FactoryException {
        int relativeIndex = +1;
search: do {
            final MathTransform other = context.getTransform(relativeIndex).orElse(null);
            if (other instanceof CompoundTransform) {
                final MathTransform[] components = components();
                final MathTransform[] toConcatenate = ((CompoundTransform) other).components();
                final int n = components.length;
                if (toConcatenate.length == n) {
                    final MathTransform[] concatenated = new MathTransform1D[n];
                    for (int i=0; i<n; i++) {
                        MathTransform c1 = components[i];
                        MathTransform c2 = toConcatenate[i];
                        if (relativeIndex < 0) {
                            c1 = c2;
                            c2 = components[i];
                        }
                        if (c1.getTargetDimensions() != c2.getSourceDimensions()) {
                            /*
                             * TODO: if c1 or c2 are linear transforms, we could take sub-matrices.
                             */
                            continue search;
                        }
                        concatenated[i] = context.factory.createConcatenatedTransform(c1, c2);
                    }
                    if (context.replace(relativeIndex, create(concatenated))) {
                        return;
                    }
                }
            }
        } while ((relativeIndex = -relativeIndex) < 0);
        super.tryConcatenate(context);
    }

    /**
     * Computes a hash value for this transform. This method is invoked by {@link #hashCode()} when first needed.
     */
    @Override
    protected final int computeHashCode() {
        return super.hashCode() + Arrays.hashCode(components());
    }

    /**
     * Compares the specified object with this math transform for equality.
     */
    @Override
    public final boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;
        }
        return super.equals(object, mode) &&
                Utilities.deepEquals(components(), ((CompoundTransform) object).components(), mode);
    }
}
