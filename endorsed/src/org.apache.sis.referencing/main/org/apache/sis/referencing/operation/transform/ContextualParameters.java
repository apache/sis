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
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.io.Serializable;
import org.opengis.util.FactoryException;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.referencing.privy.ExtendedPrecisionMatrix;
import org.apache.sis.referencing.privy.WKTUtilities;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.referencing.privy.Formulas;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.privy.DoubleDouble;
import org.apache.sis.util.privy.UnmodifiableArrayList;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.parameter.DefaultParameterValue;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.NoninvertibleMatrixException;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import static org.apache.sis.referencing.privy.WKTUtilities.LOGGER;


/**
 * The parameters that describe a sequence of
 * <i>normalize</i> → <i>non-linear kernel</i> → <i>denormalize</i> transforms as a whole.
 * The normalize and denormalize steps must be affine transforms, while the non-linear kernel is arbitrary.
 * This separation is useful for doing in the linear steps any unit conversions and axis swapping necessary
 * for allowing the non-linear step to work in some predefined (implementation specific) coordinate system.
 *
 * <p>Contextual parameters are {@linkplain AbstractMathTransform#getContextualParameters() associated}
 * to the <em>non-linear kernel</em> step of the above-cited sequence.
 * Since the {@linkplain AbstractMathTransform#getParameterValues() parameter values} of the non-linear kernel contains
 * only normalized parameters (e.g. a map projection on an ellipsoid having a <i>semi-major</i> axis length of 1),
 * Apache SIS needs contextual information for reconstructing the parameters of the complete transforms chain.</p>
 *
 * <h2>Usage in map projections</h2>
 * This object is used mostly for Apache SIS implementation of map projections, where the non-linear kernel
 * is a normalized projection working on (<var>longitude</var>, <var>latitude</var>) coordinates in radians.
 * The {@linkplain #completeTransform(MathTransformFactory, MathTransform) complete map projection}
 * (ignoring {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes changes of axis order})
 * is a chain of 3 transforms as shown below:
 *
 * <div class="horizontal-flow" style="align-items:center">
 *   <div>{@include formulas.html#NormalizeGeographic}</div>
 *   <div>→</div>
 *   <div>Map projection on a normalized ellipsoid</div>
 *   <div>→</div>
 *   <div>{@include formulas.html#DenormalizeCartesian}</div>
 * </div>
 *
 * {@code ContextualParameters} is typically created and used as below:
 *
 * <ol class="verbose">
 *   <li>A {@link MathTransformProvider} instantiates a non-linear transform working in a predefined coordinate system.
 *     Note that different providers may instantiate the same transform class but with different parameters.
 *     For example, both <q>Mercator (variant A)</q> and <q>Mercator (variant B)</q> operation methods
 *     instantiate the same Mercator projection class, but with different ways to represent the parameters.</li>
 *
 *   <li>The map projection constructor fetches all parameters that it needs from the user supplied
 *     {@link org.apache.sis.parameter.Parameters}, initializes the projection, then saves the parameter values that
 *     it actually used in a new {@code ContextualParameters} instance.</li>
 *
 *   <li>The map projection constructor may keep only the non-linear parameters for itself,
 *     and gives the linear parameters to the {@link #normalizeGeographicInputs normalizeGeographicInputs(…)} and
 *     {@link MatrixSIS#convertAfter MatrixSIS.convertAfter(…)} methods, which will create the matrices show above.
 *     The projection constructor is free to apply additional operations on the two affine transforms
 *     ({@linkplain #getMatrix(MatrixRole) normalize / denormalize}) before or after the above-cited
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
 * <h2>Serialization</h2>
 * Serialized instances of this class are not guaranteed to be compatible with future SIS versions.
 * Serialization should be used only for short term storage or RMI between applications running the same SIS version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see AbstractMathTransform#getContextualParameters()
 *
 * @since 0.6
 */
public class ContextualParameters extends Parameters implements Serializable {
    /**
     * Whether a matrix is used for <i>normalization</i> or <i>denormalization</i>
     * before or after a non-linear operation.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 0.7
     *
     * @see ContextualParameters#getMatrix(MatrixRole)
     *
     * @since 0.7
     */
    public enum MatrixRole {
        /**
         * Matrix for converting angular degrees to radians, or any other linear operations needed
         * before to apply a non-linear operation. For example, in a map projection, this matrix is
         * typically (but not necessarily) as below:
         *
         * <div style="text-align:center">{@include formulas.html#NormalizeGeographic}</div>
         */
        NORMALIZATION,

        /**
         * Inverse of the {@link #NORMALIZATION} matrix.
         * For example, in a map projection, this matrix is typically (but not necessarily) as below:
         *
         * <div style="text-align:center">{@include formulas.html#DenormalizeGeographic}</div>
         */
        INVERSE_NORMALIZATION,

        /**
         * Matrix for scaling Cartesian coordinates to the size of the planet, or any other linear operations needed
         * after execution of a non-linear operation. For example, in a map projection, this matrix is typically
         * (but not necessarily) as below:
         *
         * <div style="text-align:center">{@include formulas.html#DenormalizeCartesian}</div>
         */
        DENORMALIZATION,

        /**
         * Inverse of the {@link #DENORMALIZATION} matrix.
         */
        INVERSE_DENORMALIZATION
    }

    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6769546741493459341L;

    /**
     * The parameters that represents the sequence of transforms as a whole. The parameter values may be used
     * in the {@linkplain #getMatrix(MatrixRole) (de)normalization} transforms, in the kernel, or both.
     *
     * @see #getDescriptor()
     */
    @SuppressWarnings("serial")                         // Most SIS implementations are serializable.
    private final ParameterDescriptorGroup descriptor;

    /**
     * The affine transforms to be applied before (<i>normalize</i>) and after (<i>denormalize</i>) the non-linear
     * transform described by this group of parameters. Those matrices are initialized to identity at construction
     * time, then modified in-place by callers of {@link #getMatrix(MatrixRole)} or related methods.
     * After the {@link #completeTransform(MathTransformFactory, MathTransform)} method has been invoked,
     * those matrices are typically (but not necessarily) replaced by the {@link LinearTransform} instances itself.
     *
     * @see #getMatrix(MatrixRole)
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private Matrix normalize, denormalize;

    /**
     * The parameter values. Null elements in this array are empty slots available for adding new parameter values.
     * The array length is the maximum number of parameters allowed, which is determined by the {@link #descriptor}.
     *
     * <p>This array is modifiable after construction, but is considered unmodifiable after
     * {@link #completeTransform(MathTransformFactory, MathTransform)} has been invoked.</p>
     *
     * @see #parameter(String)
     * @see #freeze()
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private ParameterValue<?>[] values;

    /**
     * If the inverse coordinate operation can be described by another {@code ContextualParameters} instance,
     * a reference to that instance. Otherwise {@code null}.
     *
     * @see #inverse(ParameterDescriptorGroup, BiPredicate)
     */
    private ContextualParameters inverse;

    /**
     * {@code false} if this parameter group is modifiable, or {@code true} if it has been made unmodifiable
     * (frozen) by a call to {@link #completeTransform(MathTransformFactory, MathTransform)}.
     *
     * @see #freeze()
     */
    private boolean isFrozen;

    /**
     * Creates a new group of parameters with the given descriptor.
     * The {@linkplain org.apache.sis.referencing.operation.DefaultOperationMethod#getParameters() method parameters}
     * shall describe the <i>normalize</i> → <i>non-linear kernel</i> → <i>denormalize</i> sequence
     * as a whole. After construction, callers shall:
     *
     * <ul>
     *   <li>Set the relevant parameter values by calls to
     *     <code>{@linkplain #parameter(String) parameter(…)}.setValue(…)</code>.</li>
     *   <li>Modify the element values in {@linkplain #getMatrix(MatrixRole) normalization / denormalization}
     *     affine transforms, optionally by calls to the convenience methods in this class.</li>
     *   <li>Get the complete transforms chain with a call
     *     {@link #completeTransform(MathTransformFactory, MathTransform) completeTransform(…)}</li>
     * </ul>
     *
     * See class javadoc for more information.
     *
     * @param  descriptor  the parameter descriptor.
     * @param  srcDim      number of source dimensions.
     * @param  tgtDim      number of target dimensions.
     *
     * @since 1.0
     */
    public ContextualParameters(final ParameterDescriptorGroup descriptor, int srcDim, int tgtDim) {
        ArgumentChecks.ensureStrictlyPositive("srcDim", srcDim);
        ArgumentChecks.ensureStrictlyPositive("tgtDim", tgtDim);
        this.descriptor  = Objects.requireNonNull(descriptor);
        this.normalize   = Matrices.create(++srcDim, srcDim, ExtendedPrecisionMatrix.CREATE_IDENTITY);
        this.denormalize = Matrices.create(++tgtDim, tgtDim, ExtendedPrecisionMatrix.CREATE_IDENTITY);
        this.values      = new ParameterValue<?>[descriptor.descriptors().size()];
    }

    /**
     * Creates a new group with the same parameters as the given source but with a different number of dimensions.
     * The new group will be frozen if the given group is frozen.
     *
     * @param  source  the parameters to copy.
     * @param  srcDim  number of source dimensions plus 1.
     * @param  tgtDim  number of target dimensions plus 1.
     *
     * @see #redimension(int, int)
     */
    private ContextualParameters(final ContextualParameters source, final int srcDim, final int tgtDim) {
        descriptor  = source.descriptor;
        normalize   = Matrices.resizeAffine(source.normalize,   srcDim, srcDim);
        denormalize = Matrices.resizeAffine(source.denormalize, tgtDim, tgtDim);
        values      = source.values;
        isFrozen    = source.isFrozen;
        if (!isFrozen) {
            values = values.clone();
        }
    }

    /**
     * Creates a new and frozen {@code ContextualParameters} for the inverse operation.
     * An optional {@code mapper} can be specified for setting the parameter values.
     * If the mapper is-non null, then it will be invoked for each new parameters.
     * The first argument given to the mapper is the {@code forward} operation,
     * and the second argument is the parameter to configure.
     * The return value is whether the parameter should be kept in the new operation.
     *
     * @param  desc     descriptor of the inverse operation.
     * @param  forward  the parameters created for the forward operation.
     * @param  mapper   the function to invoke for setting parameter values,
     *                  or {@code null} if the inverse operation uses the same parameter values.
     *
     * @see #inverse(ParameterDescriptorGroup, BiPredicate)
     */
    private ContextualParameters(final ParameterDescriptorGroup desc, final ContextualParameters forward,
                                 final BiPredicate<Parameters, ParameterValue<?>> mapper)
    {
        descriptor  = desc;
        inverse     = forward;
        normalize   = forward.getMatrix(MatrixRole.INVERSE_DENORMALIZATION);
        denormalize = forward.getMatrix(MatrixRole.INVERSE_NORMALIZATION);
        if (mapper == null) {
            values = forward.values;
        } else {
            final List<GeneralParameterDescriptor> descriptors = desc.descriptors();
            final var copy = new ParameterValue<?>[descriptors.size()];
            int count = 0;
            for (int i=0; i < copy.length; i++) {
                final var p = new ContextualParameter<>((ParameterDescriptor<?>) descriptors.get(i));
                if (mapper.test(forward, p)) {
                    copy[count++] = p;
                }
            }
            values = ArraysExt.resize(copy, count);
        }
        isFrozen = true;
    }

    /**
     * Creates a new and frozen {@code ContextualParameters} for the inverse of this operation.
     * An optional {@code mapper} can be specified for setting the parameter values.
     * If the mapper is-non null, then it will be invoked for each new parameters.
     * The first argument given to the mapper will be {@code this} operation,
     * and the second argument is the parameter to configure.
     * The return value is whether the parameter should be kept in the new operation.
     *
     * <p>This method caches the inverse operation. It is caller responsibility to ensure that all
     * arguments given to this method are constants for a given {@link MathTransform} instance.</p>
     *
     * @param  desc    descriptor of the inverse operation.
     * @param  mapper  the function to invoke for setting parameter values,
     *                 or {@code null} if the inverse operation uses the same parameter values.
     * @return parameters for the inverse operation.
     */
    final synchronized ContextualParameters inverse(final ParameterDescriptorGroup desc,
            final BiPredicate<Parameters, ParameterValue<?>> mapper)
    {
        if (inverse == null) {
            if (!isFrozen) {
                freeze();
            }
            inverse = new ContextualParameters(desc, this, mapper);
        }
        assert inverse.descriptor == desc;
        return inverse;
    }

    /**
     * Returns the parameters for the <i>normalize</i> → <i>non-linear kernel</i> →
     * <i>denormalize</i> sequence as a whole. This is the parameter descriptor of the
     * {@linkplain org.apache.sis.referencing.operation.DefaultOperationMethod operation method}
     * given to the constructor.
     *
     * <p>The values for those parameters are given by the {@link #values()} method.
     * Those values may be used by the {@linkplain #getMatrix(MatrixRole) normalization / denormalization}
     * linear steps, by the non-linear kernel, or both.</p>
     *
     * @return the description of the parameters.
     */
    @Override
    public final ParameterDescriptorGroup getDescriptor() {
        return descriptor;
    }

    /**
     * Ensures that this {@code ContextualParameters} instance is modifiable.
     *
     * @throws IllegalStateException if this {@code ContextualParameter} has been made unmodifiable.
     */
    private void ensureModifiable() throws IllegalStateException {
        assert Thread.holdsLock(this);
        if (isFrozen) {
            throw new IllegalStateException(Errors.format(Errors.Keys.UnmodifiableObject_1, getClass()));
        }
    }

    /**
     * Returns the given matrix as an unmodifiable one if this {@code ContextualParameters} instance is unmodifiable.
     * Note that if this instance is modifiable, then we <strong>must</strong> return a direct reference to the matrix,
     * not a wrapper, because the caller may need to modify it.
     */
    private MatrixSIS toMatrixSIS(final Matrix m) {
        return isFrozen ? Matrices.unmodifiable(m) : (MatrixSIS) m;
    }

    /**
     * Returns the affine transforms to be applied before or after the non-linear kernel operation.
     * Immediately after {@linkplain #ContextualParameters(ParameterDescriptorGroup, int, int) construction},
     * those matrices are modifiable identity matrices. Callers can modify the matrix element values, typically
     * by calls to the {@link MatrixSIS#convertBefore(int, Number, Number) MatrixSIS.convertBefore(…)} method.
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
     * <h4>Application to map projections</h4>
     * After the construction of a non-linear kernel working on (<var>longitude</var>, <var>latitude</var>)
     * coordinates in radians, the matrices returned by {@code projection.getContextualParameters().getMatrix(…)}
     * are initialized to the values shown below.
     *
     * <table class="sis">
     *   <caption>Initial matrix coefficients after {@code NormalizedProjection} construction</caption>
     *   <tr>
     *     <th>{@code getMatrix(NORMALIZATION)}</th>
     *     <th class="sep">{@code getMatrix(DENORMALIZATION)}</th>
     *   </tr><tr>
     *     <td>{@include formulas.html#NormalizeGeographic}</td>
     *     <td class="sep">{@include formulas.html#DenormalizeCartesian}</td>
     *   </tr>
     * </table>
     *
     * @param  role  {@code NORMALIZATION} for fetching the <i>normalization</i> transform to apply before the kernel,
     *               {@code DENORMALIZATION} for the <i>denormalization</i> transform to apply after the kernel, or
     *               {@code INVERSE_*} for the inverse of the above-cited matrices.
     * @return the matrix for the requested normalization or denormalization affine transform.
     *
     * @since 0.7
     */
    public final MatrixSIS getMatrix(MatrixRole role) {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final ContextualParameters inverse;
        final Matrix fallback;
        synchronized (this) {
            switch (role) {
                default:                      throw new AssertionError(role);
                case NORMALIZATION:           return toMatrixSIS(normalize);
                case DENORMALIZATION:         return toMatrixSIS(denormalize);
                case INVERSE_NORMALIZATION:   role = MatrixRole.DENORMALIZATION; fallback = normalize; break;
                case INVERSE_DENORMALIZATION: role = MatrixRole.NORMALIZATION; fallback = denormalize; break;
            }
            inverse = this.inverse;     // Copy the reference while we are inside the synchronized block.
        }
        /*
         * Following must be outside the synchronized block in order to avoid potential deadlock while invoking
         * inverse.getMatrix(role). We do not cache the matrix here, but `inverse` is likely to have cached it.
         */
        final Matrix m;
        if (inverse != null) {
            m = inverse.getMatrix(role);
        } else try {
            m = Matrices.inverse(fallback);
        } catch (NoninvertibleMatrixException e) {
            throw new IllegalStateException(Errors.format(Errors.Keys.CanNotCompute_1, role), e);
        }
        return Matrices.unmodifiable(m);
    }

    /**
     * Prepends a normalization step converting input coordinates in the two first dimensions from degrees to radians.
     * The normalization can optionally subtract the given λ₀ value (in degrees) from the longitude.
     *
     * <p>Invoking this method is equivalent to {@linkplain java.awt.geom.AffineTransform#concatenate concatenating}
     * the normalization matrix with the following matrix. This will have the effect of applying the conversion
     * described above before any other operation:</p>
     *
     * <div style="text-align:center">{@include formulas.html#NormalizeGeographic}</div>
     *
     * @param  λ0  longitude of the central meridian, in degrees.
     * @return the normalization affine transform as a matrix.
     *         Callers can change that matrix directly if they want to apply additional normalization operations.
     * @throws IllegalStateException if this {@code ContextualParameter} has been made unmodifiable.
     */
    public synchronized MatrixSIS normalizeGeographicInputs(final double λ0) {
        ensureModifiable();
        /*
         * In theory the check for (λ0 != 0) is useless. However, Java has a notion of negative zero, and we want
         * to avoid negative zeros because we do not want them to appear in WKT formatting of matrix elements.
         */
        final DoubleDouble toRadians = DoubleDouble.DEGREES_TO_RADIANS;
        DoubleDouble offset = null;
        if (λ0 != 0) {
            offset = DoubleDouble.of(-λ0, true).multiply(toRadians);
        }
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final var normalize = (MatrixSIS) this.normalize;       // Must be the same instance, not a copy.
        normalize.convertBefore(0, toRadians, offset);
        normalize.convertBefore(1, toRadians, null);
        return normalize;
    }

    /**
     * Appends a denormalization step after the non-linear kernel, which will convert input coordinates
     * in the two first dimensions from radians to degrees. After this conversion, the denormalization
     * can optionally add the given λ₀ value (in degrees) to the longitude.
     *
     * <p>Invoking this method is equivalent to {@linkplain java.awt.geom.AffineTransform#concatenate concatenating}
     * the denormalization matrix with the following matrix. This will have the effect of applying the conversion
     * described above after the non-linear kernel operation:</p>
     *
     * <div style="text-align:center">{@include formulas.html#DenormalizeGeographic}</div>
     *
     * @param  λ0  longitude of the central meridian, in degrees.
     * @return the denormalization affine transform as a matrix.
     *         Callers can change that matrix directly if they want to apply additional denormalization operations.
     * @throws IllegalStateException if this {@code ContextualParameter} has been made unmodifiable.
     */
    public synchronized MatrixSIS denormalizeGeographicOutputs(final double λ0) {
        ensureModifiable();
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final var denormalize = (MatrixSIS) this.denormalize;           // Must be the same instance, not a copy.
        final DoubleDouble toDegrees = DoubleDouble.RADIANS_TO_DEGREES;
        denormalize.convertAfter(0, toDegrees, (λ0 != 0) ? λ0 : null);
        denormalize.convertAfter(1, toDegrees, null);
        return denormalize;
    }

    /**
     * Marks this {@code ContextualParameter} as unmodifiable and creates the
     * <i>normalize</i> → {@code kernel} → <i>denormalize</i> transforms chain.
     * This method shall be invoked only after the {@linkplain #getMatrix(MatrixRole) (de)normalization}
     * matrices have been set to their final values.
     *
     * <p>The transforms chain created by this method does not include any step for
     * {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes changing axis order}
     * or for converting to units other than degrees or metres. Such steps, if desired, should be defined
     * outside {@code ContextualParameters}. Efficient concatenation of those steps will happen "under
     * the hood".</p>
     *
     * @param  factory  the factory to use for creating math transform instances.
     * @param  kernel   the non-linear kernel which expects "normalized" coordinates in a predefined coordinate system.
     * @return the concatenation of <i>normalize</i> → <i>the given kernel</i> → <i>denormalize</i>
     *         transforms.
     * @throws FactoryException if an error occurred while creating a math transform instance.
     */
    public MathTransform completeTransform(final MathTransformFactory factory, final MathTransform kernel)
            throws FactoryException
    {
        final MathTransform n, d;
        synchronized (this) {
            if (!isFrozen) {
                freeze();
            }
            /*
             * Creates the ConcatenatedTransform, letting the factory returns the cached instance
             * if the caller already invoked this method previously (which usually do not happen).
             */
            n = factory.createAffineTransform(normalize);
            d = factory.createAffineTransform(denormalize);
            Matrix m;
            if ((m = MathTransforms.getMatrix(n)) != null)   normalize = m;
            if ((m = MathTransforms.getMatrix(d)) != null) denormalize = m;
        }
        if (kernel == null) {   // Undocumented feature useful for MolodenskyTransform constructor.
            return null;
        }
        /*
         * Following call must be outside the synchronized block for avoiding dead-lock. This is because the
         * factory typically contains a WeakHashSet, which invoke in turn the `equals` methods in this class
         * (indirectly, through `kernel` comparison). We need to be outside the synchronized block for having
         * the locks taken in same order (WeakHashSet lock before the ContextualParameters lock).
         */
        return factory.createConcatenatedTransform(factory.createConcatenatedTransform(n, kernel), d);
    }

    /**
     * Marks this contextual parameter as unmodifiable.
     *
     * @see #ensureModifiable()
     */
    private void freeze() {
        isFrozen = true;
        /*
         * Sort the parameter values in the same order as the parameter descriptor. This is not essential,
         * but makes easier to read `toString()` output by ensuring a consistent order for most projections.
         * Some WKT parsers other than SIS may also require the parameter values to be listed in that specific
         * order. We proceed by first copying all parameters in a temporary HashMap:
         */
        final var parameters = new IdentityHashMap<ParameterDescriptor<?>, ParameterValue<?>>(values.length);
        for (ParameterValue<?> p : values) {
            if (p == null) {
                break;                      // The first null value in the array indicates the end of sequence.
            }
            p = DefaultParameterValue.unmodifiable(p);
            final ParameterDescriptor<?> desc = p.getDescriptor();
            if (parameters.put(desc, p) != null) {
                // Should never happen unless ParameterValue.descriptor changed (contract violation).
                throw new IllegalStateException(Errors.format(Errors.Keys.ElementAlreadyPresent_1, desc.getName()));
            }
        }
        /*
         * Then, copy all HashMap values back to the `values` array in the order they are declared in the
         * descriptor. Implementation note: the iteration termination condition uses the values array, not
         * the descriptors list, because the former is often shorter than the latter. We should never reach
         * the end of descriptors list before the end of values array because `descriptors` contains all
         * `parameters` keys. This is verified by the `assert` below.
         */
        values = new ParameterValue<?>[parameters.size()];
        assert descriptor.descriptors().containsAll(parameters.keySet());
        final Iterator<GeneralParameterDescriptor> it = descriptor.descriptors().iterator();
        for (int i=0; i < values.length;) {
            /*
             * No need to check for it.hasNext(), since a NoSuchElementException below would be a bug in
             * our algorithm (or a concurrent change in the `descriptor.descriptors()` list, which would
             * be a contract violation). See above `assert`.
             */
            @SuppressWarnings("element-type-mismatch")
            final ParameterValue<?> p = parameters.get(it.next());
            if (p != null) {
                values[i++] = p;
            }
        }
    }

    /**
     * Returns the parameter value of the given name.
     * Before the call to {@link #completeTransform completeTransform(…)},
     * this method can be used for setting parameter values like below:
     *
     * {@snippet lang="java" :
     *     parameter("Scale factor").setValue(0.9996);   // Scale factor of Universal Transverse Mercator (UTM) projections.
     *     }
     *
     * After the call to {@code completeTransform(…)}, the returned parameters are read-only.
     *
     * @param  name  the name of the parameter to search.
     * @return the parameter value for the given name.
     * @throws ParameterNotFoundException if there is no parameter of the given name.
     */
    @Override
    public synchronized ParameterValue<?> parameter(final String name) throws ParameterNotFoundException {
        final GeneralParameterDescriptor desc = descriptor.descriptor(name);
        if (!(desc instanceof ParameterDescriptor<?>)) {
            throw parameterNotFound(name);
        }
        /*
         * Search for existing parameter instance. This implementation does not scale, but should be okay since
         * the number of parameters is typically very small (rarely more than 6 parameters in map projections).
         */
        for (int i=0; i < values.length; i++) {
            ParameterValue<?> p = values[i];
            if (p == null) {
                // Null values are always last, so it is okay to stop the search here.
                values[i] = p = new ContextualParameter<>((ParameterDescriptor<?>) desc);
                return p;
            }
            if (p.getDescriptor() == desc) {                    // Identity comparison should be okay here.
                return p;
            }
        }
        /*
         * We may reach this point if map projection construction is completed (i.e. `completeTransform(…)` has
         * been invoked) and the user asks for a parameter which is not one of the parameters that we retained.
         * Returns a parameter initialized to the default value, which is the actual value (otherwise we would
         * have stored that parameter).  Note: we do not bother making the parameter immutable for performance
         * reason. If the user invokes a setter method on the returned parameter, he may get a false impression
         * that this ContextualParameters is still modifiable. We presume that such scenario would be rare.
         */
        if (isFrozen) {
            return ((ParameterDescriptor<?>) desc).createValue();
        }
        /*
         * Should never reach this point. If it happen anyway, this means that the descriptor now accepts
         * more parameters than what it declared at ContextualParameteres construction time, or that some
         * ParameterDescriptor instances changed.
         */
        throw new ParameterNotFoundException(Errors.format(Errors.Keys.UnexpectedChange_1, descriptor.getName()), name);
    }

    /**
     * Returns an unmodifiable list containing all parameters in this group.
     * Callers should not attempt to modify the parameter values in this list.
     *
     * @return all parameter values.
     */
    @Override
    @SuppressWarnings("unchecked")
    public synchronized List<GeneralParameterValue> values() {
        int upper = values.length;
        while (upper != 0 && values[upper - 1] == null) {
            upper--;
        }
        return UnmodifiableArrayList.wrap(values, 0, upper);
    }

    /**
     * Unsupported operation, since {@code ContextualParameters} groups do not contain sub-groups.
     *
     * @param  name  ignored.
     * @return never returned.
     */
    @Override
    public List<ParameterValueGroup> groups(final String name) {
        throw parameterNotFound(name);
    }

    /**
     * Unsupported operation, since {@code ContextualParameters} groups do not contain sub-groups.
     *
     * @param  name  ignored.
     * @return never returned.
     */
    @Override
    public ParameterValueGroup addGroup(final String name) {
        throw parameterNotFound(name);
    }

    /**
     * Returns the exception to throw when the parameter of the given name has not been found.
     */
    private ParameterNotFoundException parameterNotFound(final String name) {
        return new ParameterNotFoundException(Resources.format(
                Resources.Keys.ParameterNotFound_2, descriptor.getName(), name), name);
    }

    /**
     * Returns the same parameters but for an operation having the specified number of dimensions.
     * The returned instance has a normalization matrix set to a square matrix of size {@code srcDim}+1,
     * and a denormalization matrix set to a square matrix of size {@code tgtDim}+1.
     * See {@link Matrices#resizeAffine(Matrix, int, int) Matrices.resizeAffine(…)}
     * for details about which matrix values are copied.
     *
     * <p>If this group of parameters is unmodifiable, then the returned group is also unmodifiable.
     * If the specified dimensions result in no change, then this method returns {@code this}.</p>
     *
     * <h4>Purpose</h4>
     * This method is mostly for deriving a two-dimensional variant of a three-dimensional operation
     * by dropping the last dimensions. The {@link #completeTransform completeTransform(…)} method of
     * the returned instance should be invoked with a kernel transform having compatible dimensions.
     *
     * @param  srcDim  new number of source dimensions.
     * @param  tgtDim  new number of target dimensions.
     * @return parameters for an operation having the given number of dimensions, or {@code this} if there is no change.
     * @throws IllegalArgumentException if a dimension is zero or negative.
     *
     * @see Matrices#resizeAffine(Matrix, int, int)
     *
     * @since 1.5
     */
    public synchronized ContextualParameters redimension(int srcDim, int tgtDim) {
        ArgumentChecks.ensureStrictlyPositive("srcDim", srcDim++);
        ArgumentChecks.ensureStrictlyPositive("tgtDim", tgtDim++);
        if (srcDim == normalize.getNumCol() && tgtDim == denormalize.getNumRow()) {
            return this;
        }
        final var r = new ContextualParameters(this, srcDim, tgtDim);
        if (inverse != null) {
            r.inverse = new ContextualParameters(inverse, tgtDim, srcDim);
        }
        return r;
    }

    /**
     * Returns a modifiable clone of this parameter value group.
     *
     * @return a clone of this parameter value group.
     */
    @Override
    public synchronized ContextualParameters clone() {
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
         * We do not clone inverseFoo fields since they shall be null for modifiable instances.
         */
        final var clone = (ContextualParameters) super.clone();
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
    public synchronized int hashCode() {
        return (normalize.hashCode() + 31*denormalize.hashCode()) ^ Arrays.hashCode(values) ^ (int) serialVersionUID;
    }

    /**
     * Compares the given object with the parameters for equality.
     *
     * @param  object  the object to compare with the parameters.
     * @return {@code true} if the given object is equal to this one.
     */
    @Override
    public synchronized boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            final var that = (ContextualParameters) object;
            return Objects.equals(descriptor,  that.descriptor)  &&
                   Objects.equals(normalize,   that.normalize)   &&
                   Objects.equals(denormalize, that.denormalize) &&
                    Arrays.equals(values,      that.values);
        }
        return false;
    }

    /**
     * Formats the <i>Well Known Text</i> for the transform or the inverse of the transform
     * that would be built from the enclosing {@code ContextualParameters}.
     */
    private final class WKT extends FormattableObject implements Parameterized {
        /**
         * {@code true} if this proxy is for the inverse transform instead of the direct one.
         */
        private final boolean inverse;

        /**
         * Creates a new object to be formatted instead of the enclosing transform.
         */
        WKT(final boolean inverse) {
            this.inverse = inverse;
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
         * Formats a <i>Well Known Text</i> version 1 (WKT 1) element for a transform using this group of parameters.
         *
         * <h4>Compatibility note</h4>
         * {@code Param_MT} is defined in the WKT 1 specification only.
         * If the {@linkplain Formatter#getConvention() formatter convention} is set to WKT 2,
         * then this method silently uses the WKT 1 convention without raising an error.
         *
         * @return {@code "Param_MT"}.
         */
        @Override
        protected String formatTo(final Formatter formatter) {
            if (inverse) {
                formatter.newLine();
                formatter.append(new WKT(false));
                return WKTKeywords.Inverse_MT;
            } else {
                WKTUtilities.appendParamMT(ContextualParameters.this, formatter);
                return WKTKeywords.Param_MT;
            }
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
     * @param  transforms  the full chain of concatenated transforms.
     * @param  index       the index of this transform in the {@code transforms} chain.
     * @param  inverse     always {@code false}, except if we are formatting the inverse transform.
     * @return index of this transform in the {@code transforms} chain after processing.
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
         * before the projection. However, it may contain more than just this normalization,
         * because it may have been concatenated with any user-defined transform (for example,
         * in order to apply a change of axis order). We need to separate the "user-defined"
         * step from the "normalize" step.
         */
        MatrixSIS userDefined;
        try {
            userDefined = getMatrix(inverse ? MatrixRole.DENORMALIZATION : MatrixRole.INVERSE_NORMALIZATION);
        } catch (IllegalStateException e) {
            /*
             * Should never happen. But if it does, we abandon the attempt to change
             * the list elements and will format the objects in their "raw" format.
             */
            unexpectedException(e);
            return index;
        }
        if (hasBefore) {
            userDefined = userDefined.multiply(before);
        }
        /*
         * At this point "userDefined" is the affine transform to show to user instead of the
         * "before" affine transform. Replaces "before" by "userDefined" locally (but not yet
         * in the list), or set it to null (meaning that it will be removed from the list) if
         * it is identity, which happen quite often.
         *
         * Note on rounding error: the coefficients are often either 0 or 1 since the transform
         * is often for changing axis order. Thanks to double-double arithmetic in SIS matrices,
         * the non-zero values are usually accurate. But the values that should be zero are much
         * harder to get right. Sometimes we see small values (around 1E-12) in the last column of
         * the `before` matrix below. Since this column contains translation terms, those numbers
         * are in the unit of measurement of input values of the MathTransform after the matrix.
         *
         *   - For forward map projections, those values are conceptually in decimal degrees
         *     (in fact the values are converted to radians but not by this `before` matrix).
         *
         *   - For inverse map projections, those values are conceptually in metres (in fact
         *     converted to distances on a unitary ellipsoid but not by this `before` matrix).
         *
         *   - Geographic/Geocentric transformations behave like map projections in regard to units.
         *     Molodensky transformations conceptually use always decimal degrees. There is not much
         *     other cases since this mechanism is internal to SIS (not in public API).
         *
         * Consequently, we set the tolerance threshold to ANGULAR_TOLERANCE. We do not bother (at least
         * for now) to identify the cases where we could use LINEAR_TOLERANCE because just checking the
         * `inverse` flag is not sufficient (e.g. the Molodensky case). Since the angular tolerance is
         * smaller than the linear one, unconditional usage of ANGULAR_TOLERANCE is more conservative.
         */
        before = Matrices.isIdentity(userDefined, Formulas.ANGULAR_TOLERANCE) ? null : userDefined;
        /*
         * Compute the "after" affine transform in a way similar than the "before" affine.
         * Note that if this operation fails, we will cancel everything we would have done
         * in this method (i.e. we do not touch the transforms list at all).
         */
        try {
            userDefined = getMatrix(inverse ? MatrixRole.NORMALIZATION : MatrixRole.INVERSE_DENORMALIZATION);
        } catch (IllegalStateException e) {
            unexpectedException(e);
            return index;
        }
        if (hasAfter) {
            userDefined = Matrices.multiply(after, userDefined);
        }
        /*
         * Note on rounding error: same discussion as the "note on rounding error" of the `before` matrix,
         * with the following differences:
         *
         *   - For forward map projections, unit of measurements of translation terms are conceptually
         *     metres (instead of degrees) multiplied by the scale factors in the `after` matrix.
         *
         *   - For inverse map projections, unit of measurements of translation terms are conceptually
         *     degrees (instead of metres) multiplied by the scale factors in the `after` matrix.
         *
         *   - And so on for all cases: swap the units of the forward and inverse cases, then multiply
         *     by the scale factor. Note that the multiplication step does not exist in the `before` case.
         *
         * Since we are seeking for the identity matrix, the scale factor is 1. We do not bother to distinguish
         * the ANGULAR_TOLERANCE and LINEAR_TOLERANCE cases for the same reasons as for the `before` matrix.
         */
        after = Matrices.isIdentity(userDefined, Formulas.ANGULAR_TOLERANCE) ? null : userDefined;
        /*
         * At this point we have computed all the affine transforms to show to the user.
         * We can replace the elements in the list. The transform referenced by transforms.get(index)
         * is usually a NormalizedProjection, to be replaced by a ContextualParameters instance in order
         * to format real parameter values (semi-major axis, scale factor, etc.)
         * instead of a semi-major axis length of 1.
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
        transforms.set(index, new WKT(inverse));
        if (after == null) {
            if (hasAfter) {
                final Object old = transforms.remove(index + 1);
                assert (old instanceof LinearTransform);
            }
        } else {
            if (hasAfter) {
                final Object old = transforms.set(index + 1, after);
                assert (old instanceof LinearTransform);
            } else {
                transforms.add(index + 1, after);
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
    private static void unexpectedException(final IllegalStateException e) {
        Logging.unexpectedException(LOGGER, ConcatenatedTransform.class, "formatTo", e.getCause());
    }
}
