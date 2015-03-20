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
package org.apache.sis.referencing.operation.transform;

import java.util.List;
import java.util.Objects;
import java.io.Serializable;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.internal.referencing.WKTUtilities;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.NoninvertibleMatrixException;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * The parameters of a math transform as a tuple of
 * (<cite>normalize</cite> – <cite>non-linear kernel</cite> – <cite>denormalize</cite>) transforms.
 * The normalize and denormalize parts must be affine transforms.
 *
 * <div class="section">Usage in map projections</div>
 * This object is used mostly for Apache SIS implementation of map projections, where the kernel is a
 * {@linkplain org.apache.sis.referencing.operation.projection.UnitaryProjection unitary projection}.
 * This object is typically created and used as below:
 *
 * <ol class="verbose">
 *   <li>A {@link MathTransformProvider#createMathTransform(ParameterValueGroup)} method instantiates a
 *     class from the {@linkplain org.apache.sis.referencing.operation.projection map projection package}.
 *     Note that different {@code MathTransformProvider}s may instantiate the same map projection class.
 *     For example both <cite>"Mercator (variant A)"</cite> and <cite>"Mercator (variant B)"</cite> operation methods
 *     instantiate the same {@link org.apache.sis.referencing.operation.Mercator} class, but with different descriptors.</li>
 *
 *   <li>The map projection constructor fetches all parameters that he needs from the user-supplied
 *     {@link ParameterValueGroup}, initializes the projection, then saves the parameter values that
 *     it actually used in a new {@code NonLinearParameters} instance.</li>
 *
 *   <li>The constructor should invoke {@link #normalizeGeographic(double)}
 *     and {@link #denormalizeCartesian(double, double, double, double)}.
 *     The constructor is free to apply additional operations on the two affine transforms
 *     ({@linkplain #normalization(boolean) normalize / denormalize}) after the above-cited methods have been invoked.</li>
 * </ol>
 *
 * <div class="section">Serialization</div>
 * Serialized instances of this class are not guaranteed to be compatible with future SIS versions.
 * Serialization should be used only for short term storage or RMI between applications running the same SIS version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see AbstractMathTransform#getNonLinearParameters()
 */
public abstract class NonLinearParameters extends FormattableObject implements Parameterized, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4899134192407586472L;

    /**
     * The descriptor that represents this tuple as a whole. The parameter values may take effect in either the
     * {@linkplain #normalization(boolean) normalize/denormalize} transforms or in the kernel.
     *
     * @see #getParameterDescriptors()
     */
    private final ParameterDescriptorGroup descriptor;

    /**
     * The affine transform to be applied before (<cite>normalize</cite>) and after (<cite>denormalize</cite>)
     * the kernel operation. On {@code NonLinearParameters} construction, those affines are initially identity
     * transforms, to be modified in-place by callers of {@link #normalization(boolean)}.
     * After {@link #createConcatenatedTransform(MathTransformFactory, MathTransform)} has been invoked,
     * they are typically (but not necessarily) replaced by the {@link LinearTransform} instance itself.
     *
     * @see #normalization(boolean)
     */
    private Matrix normalize, denormalize;

    /**
     * Creates a new {@code NonLinearParameters} for the given coordinate operation method.
     * The {@linkplain org.apache.sis.referencing.operation.DefaultOperationMethod#getParameters() method parameters}
     * shall describe the parameters of this tuple as a whole, including the affine transforms applied before and after
     * the non-linear kernel. Subclasses shall initialize those {@linkplain #normalization(boolean) normalize/denormalize}
     * affine transforms when they have enough information for doing so.
     *
     * @param method The operation method for which to describe the non-linear parameters.
     */
    protected NonLinearParameters(final OperationMethod method) {
        ensureNonNull("method", method);
        descriptor  = method.getParameters();
        normalize   = linear("sourceDimensions", method.getSourceDimensions());
        denormalize = linear("targetDimensions", method.getTargetDimensions());
    }

    /**
     * Creates a matrix for a linear part of the tupple.
     * It is important that the matrices created here are instances of {@link MatrixSIS}, in order
     * to allow {@link #normalization(boolean)} to return the reference to the (de)normalize matrices.
     */
    private static MatrixSIS linear(final String name, final Integer size) {
        if (size == null) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.MissingValueForProperty_1, name));
        }
        return Matrices.createIdentity(size);
    }

    /**
     * The affine transforms to be applied before or after the kernel operation. Those affines are initially
     * identity transforms. Subclasses should invoke this method at construction time (or at some time close
     * to construction) in order to set the affine coefficients.
     *
     * @param  norm {@code true} for fetching the <cite>normalize</cite> transform to apply before the kernel,
     *         or {@code false} for the <cite>denormalize</cite> transform to apply after the kernel.
     * @return The requested normalize ({@code true}) or denormalize ({@code false}) affine transform.
     */
    public final MatrixSIS normalization(final boolean norm) {
        return MatrixSIS.castOrCopy(norm ? normalize : denormalize);
    }

    /**
     * Creates a chain of {@linkplain ConcatenatedTransform concatenated transforms} from the
     * <cite>normalize</cite> transform, the given kernel and the <cite>denormalize</cite> transform.
     *
     * @param  kernel The (usually non-linear) kernel.
     * @return The concatenation of (<cite>normalize</cite> – the given kernel – <cite>denormalize</cite>) transforms.
     */
    final MathTransform createConcatenatedTransform(final MathTransformFactory factory, MathTransform kernel)
            throws FactoryException
    {
        final MathTransform n = factory.createAffineTransform(normalize);
        final MathTransform d = factory.createAffineTransform(denormalize);
        Matrix m;
        if ((m = MathTransforms.getMatrix(n)) != null)   normalize = m;
        if ((m = MathTransforms.getMatrix(d)) != null) denormalize = m;
        if (factory instanceof DefaultMathTransformFactory) {
            kernel = ((DefaultMathTransformFactory) factory).unique(kernel);
        }
        return factory.createConcatenatedTransform(factory.createConcatenatedTransform(n, kernel), d);
    }

    /**
     * Returns the descriptor that represents this tuple as a whole. The parameter values may take effect
     * in either the {@linkplain #normalization(boolean) normalize/denormalize} transforms or in the kernel.
     *
     * <div class="note"><b>Note:</b>
     * The definition of "kernel" is left to implementors. In the particular case of Apache SIS implementation of map
     * projections, kernels are subclasses of {@link org.apache.sis.referencing.operation.projection.UnitaryProjection}.
     * </div>
     *
     * @return The description of the parameters.
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return descriptor;
    }

    /**
     * Returns the parameters that describe this tuple as a whole.
     * Changes to the returned parameters will not affect this object.
     *
     * @return A copy of the parameter values.
     */
    @Override
    public abstract ParameterValueGroup getParameterValues();

    /**
     * Returns a hash code value for this object. This value is
     * implementation-dependent and may change in any future version.
     */
    @Override
    public int hashCode() {
        return (normalize.hashCode() + 31*denormalize.hashCode()) ^ (int) serialVersionUID;
    }

    /**
     * Compares the given object with the parameters for equality.
     *
     * @param  object The object to compare with the parameters.
     * @return {@code true} if the given object is equal to this one.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            final NonLinearParameters that = (NonLinearParameters) object;
            return Objects.equals(descriptor,  that.descriptor) &&
                   Objects.equals(normalize,   that.normalize)  &&
                   Objects.equals(denormalize, that.denormalize);
        }
        return false;
    }

    /**
     * Formats a <cite>Well Known Text</cite> version 1 (WKT 1) element for a transform using those parameters.
     * The content is inferred from the parameter values returned by the {@link #getParameterValues()} method.
     *
     * <div class="note"><b>Compatibility note:</b>
     * {@code Param_MT} is defined in the WKT 1 specification only.
     * If the {@linkplain Formatter#getConvention() formatter convention} is set to WKT 2,
     * then this method silently uses the WKT 1 convention without raising an error.</div>
     *
     * @return {@code "Param_MT"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        final ParameterValueGroup parameters = getParameterValues();
        WKTUtilities.appendName(parameters.getDescriptor(), formatter, null);
        WKTUtilities.append(parameters, formatter);
        return "Param_MT";
    }

    /**
     * Formats the <cite>Well Known Text</cite> for the inverse of the transform that would be built
     * from the enclosing {@code NonLinearParameters}.
     */
    private final class InverseWKT extends FormattableObject implements Parameterized {
        /**
         * Creates a new object to be formatted instead than the enclosing transform.
         */
        InverseWKT() {
        }

        /**
         * Returns the parameters descriptor.
         */
        @Override
        public ParameterDescriptorGroup getParameterDescriptors() {
            return NonLinearParameters.this.getParameterDescriptors();
        }

        /**
         * Returns the parameter values.
         */
        @Override
        public ParameterValueGroup getParameterValues() {
            return NonLinearParameters.this.getParameterValues();
        }

        /**
         * Process to the WKT formatting of the inverse transform.
         */
        @Override
        protected String formatTo(final Formatter formatter) {
            formatter.append(NonLinearParameters.this);
            return "Inverse_MT";
        }
    }

    /**
     * Given a transformation chain, replaces the elements around {@code transforms.get(index)} transform by
     * alternative objects to use when formatting WKT. The replacement is performed in-place in the given list.
     *
     * <p>This method shall replace only the previous element and the few next elements that need
     * to be changed as a result of the previous change. This method is not expected to continue
     * the iteration after the changes that are of direct concern to this object.</p>
     *
     * <p>This method is invoked (indirectly) only by {@link ConcatenatedTransform#getPseudoSteps()} in order
     * to get the {@link ParameterValueGroup} of a map projection, or to format a {@code ProjectedCRS} WKT.</p>
     *
     * @param  transforms The full chain of concatenated transforms.
     * @param  index      The index of this transform in the {@code transforms} chain.
     * @param  inverse    Always {@code false}, except if we are formatting the inverse transform.
     * @return Index of the last transform processed. Iteration should continue at that index + 1.
     *
     * @see ConcatenatedTransform#getPseudoSteps()
     * @see AbstractMathTransform#beforeFormat(List, int, boolean)
     */
    final int beforeFormat(final List<Object> transforms, int index, final boolean inverse) {
        /*
         * We expect affine transforms before and after the unitary projection. Extracts those
         * affine transforms now. If one or both are missing, we will treat null as an identity
         * transform. We will not replace the elements in the list before new values for those
         * affine transforms have been fully calculated.
         */
        Matrix before = null;
        Matrix after  = null;
        if (index != 0) {
            final Object candidate = transforms.get(index - 1);
            if (candidate instanceof MathTransform) {
                before = MathTransforms.getMatrix((MathTransform) candidate);
            }
        }
        if (index+1 < transforms.size()) {
            final Object candidate = transforms.get(index + 1);
            if (candidate instanceof MathTransform) {
                after = MathTransforms.getMatrix((MathTransform) candidate);
            }
        }
        final boolean hasBefore = (before != null);
        final boolean hasAfter  = (after  != null);
        /*
         * We assume that the "before" affine contains the normalize operation to be applied
         * before the projection. However it may contains more than just this normalization,
         * because it may have been concatenated with any user-defined transform (for example
         * in order to apply a change of axis order). We need to separate the "user-defined"
         * part from the "normalize" part.
         */
        Matrix userDefined = inverse ? denormalize : normalize;
        if (!inverse) try {
            userDefined = Matrices.inverse(userDefined);
        } catch (NoninvertibleMatrixException e) {
            // Should never happen. But if it does, we abandon the attempt to change
            // the list elements and will format the objects in their "raw" format.
            unexpectedException(e);
            return index;
        }
        if (hasBefore) {
            userDefined = Matrices.multiply(userDefined, before);
        }
        /*
         * At this point "userDefined" is the affine transform to show to user instead of the
         * "before" affine transform. Replaces "before" by "userDefined" locally (but not yet
         * in the list), or set it to null (meaning that it will be removed from the list) if
         * it is identity, which happen quite often. Note that in the former (non-null) case,
         * the coefficients are often either 0 or 1 since the transform is often for changing
         * axis order, so it is worth to attempt rounding coefficents.
         */
        before = userDefined.isIdentity() ? null : userDefined;
        /*
         * Compute the "after" affine transform in a way similar than the "before" affine.
         * Note that if this operation fails, we will cancel everything we would have done
         * in this method (i.e. we do not touch the transforms list at all).
         */
        userDefined = inverse ? normalize : denormalize;
        if (!inverse) try {
            userDefined = Matrices.inverse(userDefined);
        } catch (NoninvertibleMatrixException e) {
            unexpectedException(e);
            return index;
        }
        if (hasAfter) {
            userDefined = Matrices.multiply(after, userDefined);
        }
        after = userDefined.isIdentity() ? null : userDefined;
        /*
         * At this point we have computed all the affine transforms to show to the user.
         * We can replace the elements in the list. The transform referenced by transforms.get(index)
         * is usually a UnitaryProjection, to be replaced by a NonLinearParameters instance in order
         * to format real parameter values (semi-major axis, scale factor, etc.)
         * instead than a semi-major axis length of 1.
         */
        if (before == null) {
            if (hasBefore) {
                final Object old = transforms.remove(--index);
                assert (old instanceof LinearTransform);
            }
        } else {
            if (hasBefore) {
                final Object old = transforms.set(index-1, before);
                assert (old instanceof LinearTransform);
            } else {
                transforms.add(index++, before);
            }
        }
        transforms.set(index, inverse ? new InverseWKT() : this);
        if (after == null) {
            if (hasAfter) {
                final Object old = transforms.remove(index + 1);
                assert (old instanceof LinearTransform);
            }
        } else {
            index++;
            if (hasAfter) {
                final Object old = transforms.set(index, after);
                assert (old instanceof LinearTransform);
            } else {
                transforms.add(index, after);
            }
        }
        return index;
    }

    /**
     * Logs a warning about a non-invertible transform. This method may be invoked during WKT
     * formatting. This error should never occur, but it still possible to recover from this
     * error and let WKT formatting to continue, which can be useful for debugging.
     *
     * <p>We pretend that the error come from {@link ConcatenatedTransform#formatTo(Formatter)}
     * because this error should occurs only in the context of WKT formatting of a concatenated
     * transform.</p>
     */
    private static void unexpectedException(final NoninvertibleMatrixException e) {
        Logging.unexpectedException(ConcatenatedTransform.class, "formatTo", e);
    }
}
