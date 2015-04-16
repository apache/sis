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
import java.util.Arrays;
import java.util.Objects;
import java.io.Serializable;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.internal.referencing.ExtendedPrecisionMatrix;
import org.apache.sis.internal.referencing.WKTUtilities;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.parameter.DefaultParameterValue;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.NoninvertibleMatrixException;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * The parameters that describe a sequence of
 * <cite>normalize</cite> → <cite>non-linear kernel</cite> → <cite>denormalize</cite> transforms as a whole.
 * The normalize and denormalize steps must be affine transforms, while the non-linear kernel is arbitrary.
 *
 * <div class="note"><b>Note:</b> actually there is nothing in this class which force the kernel to be non-linear.
 * But this class is useless if the kernel is linear, because 3 linear steps can be efficiently
 * {@linkplain java.awt.geom.AffineTransform#concatenate concatenated} in a single affine transform.</div>
 *
 * <p>Contextual parameters can be {@linkplain AbstractMathTransform#getContextualParameters() associated}
 * to the <cite>non-linear kernel</cite> step of the above-cited sequence.
 * Since the {@linkplain AbstractMathTransform#getParameterValues() parameter values} of the non-linear kernel contains
 * only normalized parameters (e.g. a map projection on an ellipsoid having a <cite>semi-major</cite> axis length of 1),
 * Apache SIS needs contextual information for reconstructing the parameters of the complete transforms chain.</p>
 *
 * <div class="section">Usage in map projections</div>
 * This object is used mostly for Apache SIS implementation of map projections, where the non-linear kernel is a
 * {@linkplain org.apache.sis.referencing.operation.projection.NormalizedProjection normalized projection}.
 * The {@linkplain #completeTransform(MathTransformFactory, MathTransform) complete map projection}
 * (ignoring {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes changes of axis order})
 * is a chain of 3 transforms as shown below:
 *
 * <center>
 *   <table class="compact" style="td {vertical-align: middle}" summary="Decomposition of a map projection">
 *     <tr>
 *       <td>{@include formulas.html#NormalizeGeographic}</td>
 *       <td>→</td>
 *       <td>Map projection on a normalized ellipsoid</td>
 *       <td>→</td>
 *       <td>{@include formulas.html#DenormalizeCartesian}</td>
 *     </tr>
 *   </table>
 * </center>
 *
 * {@code ContextualParameters} is typically created and used as below:
 *
 * <ol class="verbose">
 *   <li>A {@link MathTransformProvider} instantiates a class from the
 *     {@linkplain org.apache.sis.referencing.operation.projection map projection package}.
 *     Note that different providers may instantiate the same map projection class.
 *     For example both <cite>"Mercator (variant A)"</cite> and <cite>"Mercator (variant B)"</cite> methods
 *     instantiate the same {@link org.apache.sis.referencing.operation.projection.Mercator} class,
 *     but with different ways to represent the parameters.</li>
 *
 *   <li>The map projection constructor fetches all parameters that it needs from the user-supplied
 *     {@link org.apache.sis.parameter.Parameters}, initializes the projection, then saves the parameter values that
 *     it actually used in a new {@code ContextualParameters} instance.</li>
 *
 *   <li>The map projection constructor may keep only the non-linear parameters for itself,
 *     and gives the linear parameters to the {@link #normalizeGeographicInputs normalizeGeographicInputs(…)} and
 *     {@link MatrixSIS#convertAfter MatrixSIS.convertAfter(…)} methods, which will create the matrices show above.
 *     The projection constructor is free to apply additional operations on the two affine transforms
 *     ({@linkplain #getMatrix(boolean) normalize / denormalize}) before or after the above-cited
 *     methods have been invoked.</li>
 *
 *   <li>After all parameter values have been set and the normalize / denormalize matrices defined,
 *     the {@link #completeTransform(MathTransformFactory, MathTransform) completeTransform(…)} method
 *     will mark this {@code ContextualParameters} object as unmodifiable and create the chain of transforms
 *     from (λ,φ) in angular degrees to (x,y) in metres. Note that conversions to other units and
 *     {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes changes in axis order}
 *     are not the purpose of this transforms chain – they are separated steps.
 *   </li>
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
 * @see org.apache.sis.referencing.operation.projection.NormalizedProjection
 * @see AbstractMathTransform#getContextualParameters()
 */
public class ContextualParameters extends FormattableObject implements ParameterValueGroup, Cloneable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4899134192407586472L;

    /**
     * The parameters that represents the sequence of transforms as a whole. The parameter values may be used
     * in the {@linkplain #getMatrix(boolean) (de)normalization} transforms, in the kernel, or both.
     *
     * @see #getDescriptor()
     */
    private final ParameterDescriptorGroup descriptor;

    /**
     * The affine transform to be applied before (<cite>normalize</cite>) and after (<cite>denormalize</cite>)
     * the kernel operation. On {@code ContextualParameters} construction, those affines are initially identity
     * transforms, to be modified in-place by callers of {@link #getMatrix(boolean)} or related methods.
     * After the {@link #completeTransform(MathTransformFactory, MathTransform)} method has been invoked,
     * those matrices are typically (but not necessarily) replaced by the {@link LinearTransform} instances itself.
     *
     * @see #getMatrix(boolean)
     */
    private Matrix normalize, denormalize;

    /**
     * The parameter values. Null elements in this array are empty slots available for adding new parameter values.
     * The array length is the maximum number of parameters allowed, which is determined by the {@link #descriptor}.
     *
     * <p>This array is modifiable after construction, but is considered unmodifiable after
     * {@link #completeTransform(MathTransformFactory, MathTransform)} has been invoked.</p>
     */
    private ParameterValue<?>[] values;

    /**
     * {@code false} if this parameter group is modifiable, or {@code true} if it has been made unmodifiable
     * (frozen) by a call to {@link #completeTransform(MathTransformFactory, MathTransform)}.
     */
    private boolean isFrozen;

    /**
     * Creates a new group of parameters for the given non-linear coordinate operation method.
     * The {@linkplain org.apache.sis.referencing.operation.DefaultOperationMethod#getParameters() method parameters}
     * shall describe the <cite>normalize</cite> → <cite>non-linear kernel</cite> → <cite>denormalize</cite> sequence
     * as a whole. After construction, callers shall:
     *
     * <ul>
     *   <li>Set the relevant parameter values by calls to
     *     <code>{@linkplain #parameter(ParameterDescriptor) parameter(…)}.setValue(…)</code>.</li>
     *   <li>Modify the element values in {@linkplain #getMatrix(boolean) normalization / denormalization}
     *     affine transforms, optionally by calls to the convenience methods in this class.</li>
     *   <li>Get the complete transforms chain with a call
     *     {@link #completeTransform(MathTransformFactory, MathTransform) completeTransform(…)}</li>
     * </ul>
     *
     * See class javadoc for more information.
     *
     * @param method The non-linear operation method for which to define the parameter values.
     */
    public ContextualParameters(final OperationMethod method) {
        ensureNonNull("method", method);
        descriptor  = method.getParameters();
        normalize   = linear("sourceDimensions", method.getSourceDimensions());
        denormalize = linear("targetDimensions", method.getTargetDimensions());
        values      = new ParameterValue<?>[descriptor.descriptors().size()];
    }

    /**
     * Creates a matrix for a linear step of the transforms chain.
     * It is important that the matrices created here are instances of {@link MatrixSIS}, in order
     * to allow {@link #getMatrix(boolean)} to return the reference to the (de)normalize matrices.
     */
    private static MatrixSIS linear(final String name, final Integer size) {
        if (size == null) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.MissingValueForProperty_1, name));
        }
        final int n = size + 1;
        return Matrices.create(n, n, ExtendedPrecisionMatrix.IDENTITY);
    }

    /**
     * Returns the parameters for the <cite>normalize</cite> → <cite>non-linear kernel</cite> →
     * <cite>denormalize</cite> sequence as a whole. This is the parameter descriptor of the
     * {@linkplain org.apache.sis.referencing.operation.DefaultOperationMethod operation method}
     * given to the constructor.
     *
     * <p>The values for those parameters is given by the {@link #values()} method. Those values may be used in
     * the {@linkplain #getMatrix(boolean) normalization / denormalization} transforms, in the kernel, or both.</p>
     *
     * <div class="note"><b>Note:</b>
     * The definition of "kernel" is left to implementors.
     * In the particular case of Apache SIS implementation of map projections,
     * kernels are subclasses of {@link org.apache.sis.referencing.operation.projection.NormalizedProjection}.
     * </div>
     *
     * @return The description of the parameters.
     */
    @Override
    public final ParameterDescriptorGroup getDescriptor() {
        return descriptor;
    }

    /**
     * Ensures that this instance is modifiable.
     *
     * @throws IllegalStateException if this {@code ContextualParameter} has been made unmodifiable.
     */
    private void ensureModifiable() throws IllegalStateException {
        if (isFrozen) {
            throw new IllegalStateException(Errors.format(Errors.Keys.UnmodifiableObject_1, getClass()));
        }
    }

    /**
     * Returns the affine transforms to be applied before or after the non-linear kernel operation.
     * Immediately after {@linkplain #ContextualParameters(OperationMethod) construction}, those matrices
     * are modifiable identity matrices. Callers can modify the matrix element values, typically by calls to
     * the {@link MatrixSIS#convertBefore(int, Number, Number) MatrixSIS.convertBefore(…)} method.
     * Alternatively, the following methods can be invoked for applying some frequently used configurations:
     *
     * <ul>
     *   <li>{@link #normalizeGeographicInputs(double)}</li>
     *   <li>{@link #denormalizeGeographicOutputs(double)}</li>
     * </ul>
     *
     * After the {@link #completeTransform(MathTransformFactory, MathTransform) completeTransform(…)} method has been
     * invoked, the matrices returned by this method are {@linkplain Matrices#unmodifiable(Matrix) unmodifiable}.
     *
     *
     * <div class="section">Application to map projections</div>
     * After {@link org.apache.sis.referencing.operation.projection.NormalizedProjection} construction, the matrices
     * returned by {@code projection.getContextualParameters().getMatrix(…)} are initialized to the values shown below.
     * Note that some {@code NormalizedProjection} subclasses apply further modifications to those matrices.
     *
     * <table class="sis">
     *   <caption>Initial matrix coefficients after construction</caption>
     *   <tr>
     *     <th>{@code getMatrix(true)}</th>
     *     <th class="sep">{@code getMatrix(false)}</th>
     *   </tr><tr>
     *     <td>{@include formulas.html#NormalizeGeographic}</td>
     *     <td class="sep">{@include formulas.html#DenormalizeCartesian}</td>
     *   </tr>
     * </table>
     *
     * @param  norm {@code true} for fetching the <cite>normalization</cite> transform to apply before the kernel,
     *         or {@code false} for the <cite>denormalization</cite> transform to apply after the kernel.
     * @return The matrix for the requested normalization ({@code true}) or denormalization ({@code false}) affine transform.
     */
    public final MatrixSIS getMatrix(final boolean norm) {
        final Matrix m = norm ? normalize : denormalize;
        if (!isFrozen) {
            return (MatrixSIS) m;       // Must be the same instance, not a copy.
        } else {
            return Matrices.unmodifiable(m);
        }
    }

    /**
     * Prepends a normalization step converting input ordinates in the two first dimensions from degrees to radians.
     * The normalization can optionally subtract the given λ₀ value (in degrees) from the longitude.
     *
     * <p>Invoking this method is equivalent to {@linkplain java.awt.geom.AffineTransform#concatenate concatenating}
     * the normalization matrix with the following matrix. This will have the effect of applying the conversion
     * described above before any other operation:</p>
     *
     * <center>{@include formulas.html#NormalizeGeographic}</center>
     *
     * @param  λ0 Longitude of the central meridian, in degrees.
     * @return The normalization affine transform as a matrix.
     *         Callers can change that matrix directly if they want to apply additional normalization operations.
     * @throws IllegalStateException if this {@code ContextualParameter} has been made unmodifiable.
     */
    public MatrixSIS normalizeGeographicInputs(final double λ0) {
        ensureModifiable();
        /*
         * In theory the check for (λ0 != 0) is useless. However Java has a notion of negative zero, and we want
         * to avoid negative zeros because we do not want them to appear in WKT formatting of matrix elements.
         */
        final DoubleDouble toRadians = DoubleDouble.createDegreesToRadians();
        DoubleDouble offset = null;
        if (λ0 != 0) {
            offset = new DoubleDouble(-λ0);
            offset.multiply(toRadians);
        }
        final MatrixSIS normalize = (MatrixSIS) this.normalize;  // Must be the same instance, not a copy.
        normalize.convertBefore(0, toRadians, offset);
        normalize.convertBefore(1, toRadians, null);
        return normalize;
    }

    /**
     * Appends a denormalization step after the non-linear kernel, which will convert input ordinates
     * in the two first dimensions from radians to degrees. After this conversion, the denormalization
     * can optionally add the given λ₀ value (in degrees) to the longitude.
     *
     * <p>Invoking this method is equivalent to {@linkplain java.awt.geom.AffineTransform#concatenate concatenating}
     * the denormalization matrix with the following matrix. This will have the effect of applying the conversion
     * described above after the non-linear kernel operation:</p>
     *
     * <center>{@include formulas.html#DeormalizeGeographic}</center>
     *
     * @param  λ0 Longitude of the central meridian, in degrees.
     * @return The denormalization affine transform as a matrix.
     *         Callers can change that matrix directly if they want to apply additional denormalization operations.
     * @throws IllegalStateException if this {@code ContextualParameter} has been made unmodifiable.
     */
    public MatrixSIS denormalizeGeographicOutputs(final double λ0) {
        ensureModifiable();
        final DoubleDouble toDegrees = DoubleDouble.createRadiansToDegrees();
        final MatrixSIS denormalize = (MatrixSIS) this.denormalize;  // Must be the same instance, not a copy.
        denormalize.convertAfter(0, toDegrees, (λ0 != 0) ? λ0 : null);
        denormalize.convertAfter(1, toDegrees, null);
        return denormalize;
    }

    /**
     * Marks this {@code ContextualParameter} as unmodifiable and creates the
     * <cite>normalize</cite> → {@code kernel} → <cite>denormalize</cite> transforms chain.
     * This method shall be invoked only after the {@linkplain #getMatrix(boolean) (de)normalization}
     * matrices have been set to their final values.
     *
     * <p>The transforms chain created by this method does not include any step for
     * {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes changing axis order}
     * or for converting to units other than degrees or metres. Such steps, if desired, should be defined
     * outside {@code ContextualParameters}. Efficient concatenation of those steps will happen "under
     * the hood".</p>
     *
     * @param  factory The factory to use for creating math transform instances.
     * @param  kernel The (usually non-linear) kernel.
     *         This is often a {@link org.apache.sis.referencing.operation.projection.NormalizedProjection}.
     * @return The concatenation of <cite>normalize</cite> → <cite>the given kernel</cite> → <cite>denormalize</cite>
     *         transforms.
     * @throws FactoryException if an error occurred while creating a math transform instance.
     *
     * @see org.apache.sis.referencing.operation.projection.NormalizedProjection#createMapProjection(MathTransformFactory)
     */
    public MathTransform completeTransform(final MathTransformFactory factory, final MathTransform kernel)
            throws FactoryException
    {
        if (!isFrozen) {
            isFrozen = true;
            for (int i=0; i < values.length; i++) {
                final ParameterValue<?> p = values[i];
                if (p == null) {
                    values = Arrays.copyOf(values, i);  // Trim extra values.
                    break;
                }
                values[i] = DefaultParameterValue.unmodifiable(p);
            }
        }
        /*
         * Creates the ConcatenatedTransform, letting the factory returns the cached instance
         * if the caller already invoked this method previously (which usually do not happen).
         */
        final MathTransform n = factory.createAffineTransform(normalize);
        final MathTransform d = factory.createAffineTransform(denormalize);
        Matrix m;
        if ((m = MathTransforms.getMatrix(n)) != null)   normalize = m;
        if ((m = MathTransforms.getMatrix(d)) != null) denormalize = m;
        return factory.createConcatenatedTransform(factory.createConcatenatedTransform(n, kernel), d);
    }

    /**
     * Returns the parameter value of the given name.
     * Before the call to {@link #completeTransform completeTransform(…)},
     * this method can be used for setting parameter values like below:
     *
     * {@preformat java
     *   parameter("Scale factor").setValue(0.9996);   // Scale factor of Universal Transverse Mercator (UTM) projections.
     * }
     *
     * After the call to {@code completeTransform(…)}, the returned parameters are read-only.
     *
     * @param  name The name of the parameter to search.
     * @return The parameter value for the given name.
     * @throws ParameterNotFoundException if there is no parameter of the given name.
     */
    @Override
    public ParameterValue<?> parameter(final String name) throws ParameterNotFoundException {
        final GeneralParameterDescriptor desc = descriptor.descriptor(name);
        if (!(desc instanceof ParameterDescriptor<?>)) {
            throw parameterNotFound(name);
        }
        /*
         * Search for existing parameter instance. This implementation does not scale,
         * but should be okay since the amount of parameters is typically very small
         * (rarely more than 6 parameters in map projections).
         */
        for (int i=0; i < values.length; i++) {
            ParameterValue<?> p = values[i];
            if (p == null) {
                /*
                 * No existing parameter instance. Create a new one if this ContextualParameter
                 * is still modifiable.
                 */
                ensureModifiable();
                p = ((ParameterDescriptor<?>) desc).createValue();
                values[i] = p;
            } else if (p.getDescriptor() != desc) {  // Identity comparison should be okay here.
                continue;
            }
            return p;   // Found or created a parameter.
        }
        ensureModifiable();
        /*
         * Should never reach this point. If it happen anyway, this means that the descriptor now accepts
         * more parameters than what it declared at ContextualParameteres construction time, or that some
         * ParameterDescriptor instances changed.
         */
        throw new IllegalStateException(Errors.format(Errors.Keys.UnexpectedChange_1, descriptor.getName()));
    }

    /**
     * Returns an unmodifiable list containing all parameters in this group.
     * Callers should not attempt to modify the parameter values in this list.
     *
     * @return All parameter values.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<GeneralParameterValue> values() {
        int upper = values.length;
        while (upper != 0 && values[upper - 1] == null) {
            upper--;
        }
        return UnmodifiableArrayList.wrap(values, 0, upper);
    }

    /**
     * Unsupported operation, since {@code ContextualParameters} groups do not contain sub-groups.
     *
     * @param name Ignored.
     * @return Never returned.
     */
    @Override
    public List<ParameterValueGroup> groups(final String name) {
        throw parameterNotFound(name);
    }

    /**
     * Unsupported operation, since {@code ContextualParameters} groups do not contain sub-groups.
     *
     * @param name Ignored.
     * @return Never returned.
     */
    @Override
    public ParameterValueGroup addGroup(final String name) {
        throw parameterNotFound(name);
    }

    /**
     * Returns the exception to thrown when the parameter of the given name has not been found.
     */
    private ParameterNotFoundException parameterNotFound(final String name) {
        return new ParameterNotFoundException(Errors.format(
                Errors.Keys.ParameterNotFound_2, descriptor.getName(), name), name);
    }

    /**
     * Returns a modifiable clone of this parameter value group.
     *
     * @return A clone of this parameter value group.
     */
    @Override
    public ContextualParameters clone() {
        /*
         * Creates a new parameter array with enough room for adding new parameters.
         * Then replace each element by a modifiable clone.
         */
        final ParameterValue<?>[] param = Arrays.copyOf(values, descriptor.descriptors().size());
        for (int i=0; i<param.length; i++) {
            final ParameterValue<?> p = param[i];
            if (p == null) {
                break;
            }
            param[i] = param[i].clone();
        }
        /*
         * Now proceed to the clone of this ContextualParameters instance.
         */
        final ContextualParameters clone;
        try {
            clone = (ContextualParameters) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);    // Should never happen since we are cloneable.
        }
        clone.values      = param;
        clone.normalize   = normalize.clone();
        clone.denormalize = denormalize.clone();
        return clone;
    }

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
            final ContextualParameters that = (ContextualParameters) object;
            return Objects.equals(descriptor,  that.descriptor) &&
                   Objects.equals(normalize,   that.normalize)  &&
                   Objects.equals(denormalize, that.denormalize);
        }
        return false;
    }

    /**
     * Formats a <cite>Well Known Text</cite> version 1 (WKT 1) element for a transform using this group of parameters.
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
        WKTUtilities.appendParamMT(this, formatter);
        return "Param_MT";
    }

    /**
     * Formats the <cite>Well Known Text</cite> for the inverse of the transform that would be built
     * from the enclosing {@code ContextualParameters}.
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
            return getDescriptor();
        }

        /**
         * Returns the parameter values.
         */
        @Override
        public ParameterValueGroup getParameterValues() {
            return ContextualParameters.this;
        }

        /**
         * Process to the WKT formatting of the inverse transform.
         */
        @Override
        protected String formatTo(final Formatter formatter) {
            formatter.append(ContextualParameters.this);
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
         * We expect affine transforms before and after the normalized projection. Extracts those
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
         * step from the "normalize" step.
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
         * is usually a NormalizedProjection, to be replaced by a ContextualParameters instance in order
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
