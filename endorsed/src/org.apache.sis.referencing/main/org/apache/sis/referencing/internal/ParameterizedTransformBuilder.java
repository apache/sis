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
package org.apache.sis.referencing.internal;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.OptionalInt;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.measure.Unit;
import javax.measure.IncommensurableException;
import javax.measure.quantity.Length;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.util.FactoryException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Classes;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.provider.AbstractProvider;
import org.apache.sis.referencing.operation.provider.Geographic2Dto3D;
import org.apache.sis.referencing.operation.provider.VerticalOffset;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.MathTransformBuilder;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.referencing.operation.transform.EllipsoidToRadiusTransform;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.referencing.privy.CoordinateOperations;
import org.apache.sis.referencing.privy.Formulas;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.measure.Units;


/**
 * Builder of a parameterized math transform identified by a name or code.
 * A builder can optionally contain the source and target coordinate systems
 * for which a new parameterized transform is going to be used.
 * {@link DefaultMathTransformFactory} uses this information for:
 *
 * <ul>
 *   <li>Completing some parameters if they were not provided. In particular, the source ellipsoid can be used for
 *       providing values for the {@code "semi_major"} and {@code "semi_minor"} parameters in map projections.</li>
 *   <li>{@linkplain #swapAndScaleAxes Swapping and scaling axes} if the source or the target
 *       coordinate systems are not {@linkplain AxesConvention#NORMALIZED normalized}.</li>
 * </ul>
 *
 * Each instance should be used only once. This class is <em>not</em> thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class ParameterizedTransformBuilder extends MathTransformBuilder implements MathTransformProvider.Context {
    /**
     * Minimal precision of ellipsoid semi-major and semi-minor axis lengths, in metres.
     * If the length difference between the axis of two ellipsoids is greater than this threshold,
     * we will report a mismatch. This is used for logging purpose only and do not have any impact
     * on the {@code MathTransform} objects to be created by the factory.
     */
    private static final double ELLIPSOID_PRECISION = Formulas.LINEAR_TOLERANCE;

    /**
     * Coordinate system of the source or target points.
     */
    protected CoordinateSystem sourceCS, targetCS;

    /**
     * The ellipsoid of the source or target ellipsoidal coordinate system, or {@code null} if it does not apply.
     */
    protected Ellipsoid sourceEllipsoid, targetEllipsoid;

    /**
     * The parameters of the transform to create. This is initialized to default values.
     * The instance is returned directly by {@link #parameters()} for allowing users to
     * modify the values in-place. Then, contextual parameters are added the first time
     * that {@link #getCompletedParameters()} is invoked.
     *
     * <p>This reference is {@code null} if this builder has been constructed without
     * specifying a method or parameters. In such case, calls to {@link #parameters()}
     * or {@link #getCompletedParameters()} will throw {@link IllegalStateException},
     * unless {@link #setParameters(ParameterValueGroup, boolean)} is invoked.</p>
     */
    protected ParameterValueGroup parameters;

    /**
     * Names of parameters which have been inferred from the context.
     *
     * @see #getContextualParameters()
     */
    private final Map<String,Boolean> contextualParameters;

    /**
     * Whether the user-specified parameters have been completed with the contextual parameters.
     * This is set to {@code true} the first time that {@link #getCompletedParameters()} is invoked.
     * After this flag become {@code true}, this builder should not be modified anymore.
     *
     * @see #completeParameters()
     */
    private boolean completedParameters;

    /**
     * The warning that occurred during parameters completion, or {@code null} if none.
     * This warning is not always fatal, but will be appended to the suppressed exceptions
     * of {@link FactoryException} if the {@link MathTransform} creation nevertheless fail.
     */
    private RuntimeException warning;

    /**
     * Creates a new builder for the given operation method.
     *
     * @param  factory  factory to use for building the transform.
     * @param  method   a method known to the given factory, or {@code null} if none.
     */
    public ParameterizedTransformBuilder(final MathTransformFactory factory, final OperationMethod method) {
        super(factory);
        if (method != null) {
            provider   = method;
            parameters = method.getParameters().createValue();
        }
        contextualParameters = new LinkedHashMap<>();
    }

    /**
     * Replaces the parameters by the given values. If {@code copy} is {@code false}, the given parameters
     * will be used directly and may be modified. If {@code true}, the parameters will be copied in a group
     * created by the provider. The latter group may contain more parameters than the given {@code values}.
     * In particular, the copy may contain parameters such as {@code "semi_major"} that may not be present
     * in the given values.
     *
     * @param  values  the parameter values.
     * @param  copy    whether to copy the given parameter values.
     * @throws NoSuchIdentifierException if no method has been found for the given parameters.
     * @throws InvalidGeodeticParameterException if the parameters cannot be set to the given values.
     */
    public void setParameters(final ParameterValueGroup values, final boolean copy)
            throws NoSuchIdentifierException, InvalidGeodeticParameterException
    {
        provider = CoordinateOperations.findMethod(factory, values.getDescriptor());
        if (copy) try {
            parameters = provider.getParameters().createValue();
            Parameters.copy(values, parameters);
        } catch (IllegalArgumentException e) {
            throw new InvalidGeodeticParameterException(e.getMessage(), e);
        } else {
            parameters = values;
        }
    }

    /**
     * Returns the factory given at construction time.
     *
     * @return the factory to use for creating the transform.
     */
    @Override
    public final MathTransformFactory getFactory() {
        return factory;
    }

    /**
     * Gives input coordinates hints derived from the given <abbr>CRS</abbr>. The hints are used for axis order and
     * units conversions, and for completing parameters with axis lengths. No change of <abbr>CRS</abbr> other than
     * axis order and units are performed. This method is not public for that reason.
     *
     * @param  crs  the <abbr>CRS</abbr> from which to fetch the hints, or {@code null}.
     */
    public final void setSourceAxes(final CoordinateReferenceSystem crs) {
        setSourceAxes(crs != null ? crs.getCoordinateSystem() : null, DatumOrEnsemble.getEllipsoid(crs).orElse(null));
    }

    /**
     * Gives output coordinates hints derived from the given <abbr>CRS</abbr>. The hints are used for axis order and
     * units conversions, and for completing parameters with axis lengths. No change of <abbr>CRS</abbr> other than
     * axis order and units are performed. This method is not public for that reason.
     *
     * @param  crs  the <abbr>CRS</abbr> from which to fetch the hints, or {@code null}.
     */
    public final void setTargetAxes(final CoordinateReferenceSystem crs) {
        setTargetAxes(crs != null ? crs.getCoordinateSystem() : null, DatumOrEnsemble.getEllipsoid(crs).orElse(null));
    }

    /**
     * Gives hints about axis lengths and their orientations in input coordinates.
     * The {@code ellipsoid} argument is often provided together with an {@link EllipsoidalCS}, but not only.
     * For example, a two-dimensional {@link SphericalCS} may also require information about the ellipsoid.
     *
     * <p>Each call to this method replaces the values of the previous call.
     * However, this method cannot be invoked anymore after {@link #getCompletedParameters()} has been invoked.</p>
     *
     * @param  cs  the coordinate system defining source axis order and units, or {@code null} if none.
     * @param  ellipsoid  the ellipsoid providing source semi-axis lengths, or {@code null} if none.
     * @throws IllegalStateException if {@link #getCompletedParameters()} has already been invoked.
     */
    @Override
    public void setSourceAxes(final CoordinateSystem cs, final Ellipsoid ellipsoid) {
        if (completedParameters) {
            throw new IllegalStateException(Errors.format(Errors.Keys.AlreadyInitialized_1, "completedParameters"));
        }
        sourceCS = cs;
        sourceEllipsoid = ellipsoid;
    }

    /**
     * Gives hints about axis lengths and their orientations in output coordinates.
     * The {@code ellipsoid} argument is often provided together with an {@link EllipsoidalCS}, but not only.
     * For example, a two-dimensional {@link SphericalCS} may also require information about the ellipsoid.
     *
     * <p>Each call to this method replaces the values of the previous call.
     * However, this method cannot be invoked anymore after {@link #getCompletedParameters()} has been invoked.</p>
     *
     * @param  cs  the coordinate system defining target axis order and units, or {@code null} if none.
     * @param  ellipsoid  the ellipsoid providing target semi-axis lengths, or {@code null} if none.
     * @throws IllegalStateException if {@link #getCompletedParameters()} has already been invoked.
     */
    @Override
    public void setTargetAxes(final CoordinateSystem cs, final Ellipsoid ellipsoid) {
        if (completedParameters) {
            throw new IllegalStateException(Errors.format(Errors.Keys.AlreadyInitialized_1, "completedParameters"));
        }
        targetCS = cs;
        targetEllipsoid = ellipsoid;
    }

    /**
     * Returns the desired number of source dimensions of the transform to create.
     * This value is inferred from the source coordinate system if present.
     */
    @Override
    public final OptionalInt getSourceDimensions() {
        return (sourceCS != null) ? OptionalInt.of(sourceCS.getDimension()) : OptionalInt.empty();
    }

    /**
     * Returns the type of the source coordinate system.
     * The returned value may be an interface or an implementation class.
     *
     * @return the type of the source coordinate system, or {@code CoordinateSystem.class} if unknown.
     */
    @Override
    public final Class<? extends CoordinateSystem> getSourceCSType() {
        return (sourceCS != null) ? sourceCS.getClass() : CoordinateSystem.class;
    }

    /**
     * Returns the desired number of target dimensions of the transform to create.
     * This value is inferred from the target coordinate system if present.
     */
    @Override
    public final OptionalInt getTargetDimensions() {
        return (targetCS != null) ? OptionalInt.of(targetCS.getDimension()) : OptionalInt.empty();
    }

    /**
     * Returns the type of the target coordinate system.
     * The returned value may be an interface or an implementation class.
     *
     * @return the type of the target coordinate system, or {@code CoordinateSystem.class} if unknown.
     */
    @Override
    public final Class<? extends CoordinateSystem> getTargetCSType() {
        return (targetCS != null) ? targetCS.getClass() : CoordinateSystem.class;
    }

    /**
     * Returns the matrix that represent the affine transform to concatenate before or after
     * the parameterized transform. The {@code role} argument specifies which matrix is desired:
     *
     * <ul class="verbose">
     *   <li>{@link org.apache.sis.referencing.operation.transform.ContextualParameters.MatrixRole#NORMALIZATION
     *       NORMALIZATION} for the conversion from the {@linkplain #getSourceCS() source coordinate system} to
     *       a {@linkplain AxesConvention#NORMALIZED normalized} coordinate system, usually with
     *       (<var>longitude</var>, <var>latitude</var>) axis order in degrees or
     *       (<var>easting</var>, <var>northing</var>) in metres.
     *       This normalization needs to be applied <em>before</em> the parameterized transform.</li>
     *
     *   <li>{@link org.apache.sis.referencing.operation.transform.ContextualParameters.MatrixRole#DENORMALIZATION
     *       DENORMALIZATION} for the conversion from a normalized coordinate system to the
     *       {@linkplain #getTargetCS() target coordinate system}, for example with
     *       (<var>latitude</var>, <var>longitude</var>) axis order.
     *       This denormalization needs to be applied <em>after</em> the parameterized transform.</li>
     *
     *   <li>{@link org.apache.sis.referencing.operation.transform.ContextualParameters.MatrixRole#INVERSE_NORMALIZATION INVERSE_NORMALIZATION} and
     *       {@link org.apache.sis.referencing.operation.transform.ContextualParameters.MatrixRole#INVERSE_DENORMALIZATION INVERSE_DENORMALIZATION}
     *       are also supported but rarely used.</li>
     * </ul>
     *
     * @param  role  whether the normalization or denormalization matrix is desired.
     * @return the requested matrix, or {@code null} if this builder has no information about the coordinate system.
     * @throws FactoryException if an error occurred while computing the matrix.
     */
    @SuppressWarnings("fallthrough")
    public Matrix getMatrix(final ContextualParameters.MatrixRole role) throws FactoryException {
        final CoordinateSystem userCS;
        boolean inverse = false;
        switch (role) {
            default: throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "role", role));
            case INVERSE_NORMALIZATION:   inverse = true;     // Fall through
            case NORMALIZATION:           userCS  = sourceCS; break;
            case INVERSE_DENORMALIZATION: inverse = true;     // Fall through
            case DENORMALIZATION:         inverse = !inverse;
                                          userCS  = targetCS; break;
        }
        if (userCS == null) {
            return null;
        }
        final CoordinateSystem normalized = CoordinateSystems.replaceAxes(userCS, AxesConvention.NORMALIZED);
        try {
            if (inverse) {
                return CoordinateSystems.swapAndScaleAxes(normalized, userCS);
            } else {
                return CoordinateSystems.swapAndScaleAxes(userCS, normalized);
            }
        } catch (IllegalArgumentException | IncommensurableException cause) {
            throw new InvalidGeodeticParameterException(cause.getLocalizedMessage(), cause);
        }
    }

    /**
     * Returns the parameter values to modify for defining the transform to create.
     * Those parameters are initialized to default values, which are {@linkplain #getMethod() method} dependent.
     * User-supplied values should be set directly in the returned instance with codes like
     * <code>parameter(</code><var>name</var><code>).setValue(</code><var>value</var><code>)</code>.
     *
     * @return the parameter values to modify for defining the transform to create.
     * @throws IllegalStateException if no operation method has been specified at construction time.
     */
    @Override
    public final ParameterValueGroup parameters() {
        if (parameters != null) {
            return parameters;
        }
        throw new IllegalStateException(Errors.format(Errors.Keys.MissingValueForProperty_1, "method"));
    }

    /**
     * Returns the names of parameters that have been inferred from the context.
     * See the {@linkplain MathTransformProvider.Context#getContextualParameters interface} for more information.
     *
     * @return names of parameters inferred from context.
     */
    @Override
    public Map<String,Boolean> getContextualParameters() {
        return Collections.unmodifiableMap(contextualParameters);
    }

    /**
     * Returns the parameter values used for the math transform creation,
     * including the parameters completed by the factory.
     * This is the union of {@link #parameters()} with {@link #getContextualParameters()}.
     * The completed parameters may only have additional parameters compared to the user-supplied parameters.
     * {@linkplain #parameters() Parameter} values that were explicitly set by the user are not overwritten.
     *
     * <p>After this method has been invoked, the {@link #setSourceAxes setSourceAxes(…)}
     * and {@link #setTargetAxes setTargetAxes(…)} methods can no longer be invoked.</p>
     *
     * @return the parameter values used by the factory.
     * @throws IllegalStateException if no operation method has been specified at construction time.
     */
    @Override
    public ParameterValueGroup getCompletedParameters() {
        if (parameters != null) {
            /*
             * If the user's parameters do not contain semi-major and semi-minor axis lengths, infer
             * them from the ellipsoid. We have to do that because those parameters are often omitted,
             * since the standard place where to provide this information is in the ellipsoid object.
             */
            if (!completedParameters) {
                warning = completeParameters();
            }
            return parameters;
        }
        throw new IllegalStateException(Resources.format(Resources.Keys.UnspecifiedParameterValues));
    }

    /**
     * Gets a parameter for which to infer a value from the context.
     * The consistency flag is initially set to {@link Boolean#TRUE}.
     *
     * @param  name  name of the contextual parameter.
     * @return the parameter.
     * @throws ParameterNotFoundException if the parameter was not found.
     */
    private ParameterValue<?> getContextualParameter(final String name) throws ParameterNotFoundException {
        ParameterValue<?> parameter = parameters.parameter(name);
        contextualParameters.put(name, Boolean.TRUE);               // Add only if above line succeeded.
        return parameter;
    }

    /**
     * Returns the value of the given parameter in the given unit, or {@code NaN} if the parameter is not set.
     *
     * <p><b>NOTE:</b> Do not merge this function with {@code ensureSet(…)}. We keep those two methods
     * separated in order to give to {@code completeParameters()} an "all or nothing" behavior.</p>
     */
    private static double getValue(final ParameterValue<?> parameter, final Unit<?> unit) {
        return (parameter.getValue() != null) ? parameter.doubleValue(unit) : Double.NaN;
    }

    /**
     * Ensures that a value is set in the given parameter.
     *
     * <ul>
     *   <li>If the parameter has no value, then it is set to the given value.<li>
     *   <li>If the parameter already has a value, then the parameter is left unchanged
     *       but its value is compared to the given one for consistency.</li>
     * </ul>
     *
     * @param  parameter  the parameter which must have a value.
     * @param  actual     the current parameter value, or {@code NaN} if none.
     * @param  expected   the expected parameter value, derived from the ellipsoid.
     * @param  unit       the unit of {@code value}.
     * @param  tolerance  maximal difference (in unit of {@code unit}) for considering the two values as equivalent.
     * @return {@code true} if there is a mismatch between the actual value and the expected one.
     */
    private static boolean ensureSet(final ParameterValue<?> parameter, final double actual,
            final double expected, final Unit<?> unit, final double tolerance)
    {
        if (Math.abs(actual - expected) <= tolerance) {
            return false;
        }
        if (Double.isNaN(actual)) {
            parameter.setValue(expected, unit);
            return false;
        }
        return true;
    }

    /**
     * Completes the parameter group with information about source or target ellipsoid axis lengths,
     * if available. This method writes semi-major and semi-minor parameter values only if they do not
     * already exists in the given parameters.
     *
     * @param  ellipsoid          the ellipsoid from which to get axis lengths of flattening factor, or {@code null}.
     * @param  semiMajor          {@code "semi_major}, {@code "src_semi_major} or {@code "tgt_semi_major} parameter name.
     * @param  semiMinor          {@code "semi_minor}, {@code "src_semi_minor} or {@code "tgt_semi_minor} parameter name.
     * @param  inverseFlattening  {@code true} if this method can try to set the {@code "inverse_flattening"} parameter.
     * @return the exception if the operation failed, or {@code null} if none. This exception is not thrown now
     *         because the caller may succeed in creating the transform anyway, or otherwise may produce a more
     *         informative exception.
     */
    private RuntimeException setEllipsoid(final Ellipsoid ellipsoid, final String semiMajor, final String semiMinor,
            final boolean inverseFlattening, RuntimeException failure)
    {
        /*
         * Note: we could also consider to set the "dim" parameter here based on the number of dimensions
         * of the coordinate system. But except for the Molodensky operation, this would be SIS-specific.
         * A more portable way is to concatenate a "Geographic 3D to 2D" operation after the transform if
         * we see that the dimensions do not match. It also avoid attempt to set a "dim" parameter on map
         * projections, which is not allowed.
         */
        if (ellipsoid != null) {
            ParameterValue<?> mismatchedParam = null;
            double mismatchedValue = 0;
            try {
                final ParameterValue<?> ap = getContextualParameter(semiMajor);
                final ParameterValue<?> bp = getContextualParameter(semiMinor);
                final Unit<Length> unit = ellipsoid.getAxisUnit();
                /*
                 * The two calls to getValue(…) shall succeed before we write anything, in order to have a
                 * "all or nothing" behavior as much as possible. Note that Ellipsoid.getSemi**Axis() have
                 * no reason to fail, so we do not take precaution for them.
                 */
                final double a   = getValue(ap, unit);
                final double b   = getValue(bp, unit);
                final double tol = Units.METRE.getConverterTo(unit).convert(ELLIPSOID_PRECISION);
                if (ensureSet(ap, a, ellipsoid.getSemiMajorAxis(), unit, tol)) {
                    contextualParameters.put(semiMajor, Boolean.FALSE);
                    mismatchedParam = ap;
                    mismatchedValue = a;
                }
                if (ensureSet(bp, b, ellipsoid.getSemiMinorAxis(), unit, tol)) {
                    contextualParameters.put(semiMinor, Boolean.FALSE);
                    mismatchedParam = bp;
                    mismatchedValue = b;
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                /*
                 * Parameter not found, or is not numeric, or unit of measurement is not linear.
                 * Do not touch to the parameters. We will see if `create(…)` can do something
                 * about that. If not, `create(…)` is the right place to throw the exception.
                 */
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
            /*
             * Following is specific to Apache SIS. We use this non-standard API for allowing the
             * NormalizedProjection class (our base class for all map projection implementations)
             * to known that the ellipsoid definitive parameter is the inverse flattening factor
             * instead of the semi-major axis length. It makes a small difference in the accuracy
             * of the eccentricity parameter.
             */
            if (mismatchedParam == null && inverseFlattening && ellipsoid.isIvfDefinitive()) try {
                final ParameterValue<?> ep = getContextualParameter(Constants.INVERSE_FLATTENING);
                final double e = getValue(ep, Units.UNITY);
                if (ensureSet(ep, e, ellipsoid.getInverseFlattening(), Units.UNITY, 1E-10)) {
                    contextualParameters.put(Constants.INVERSE_FLATTENING, Boolean.FALSE);
                    mismatchedParam = ep;
                    mismatchedValue = e;
                }
            } catch (ParameterNotFoundException e) {
                /*
                 * Should never happen with Apache SIS implementation, but may happen if the given parameters come
                 * from another implementation. We can safely abandon our attempt to set the inverse flattening value,
                 * since it was redundant with semi-minor axis length.
                 */
                Logging.recoverableException(CoordinateOperations.LOGGER, getClass(), "create", e);
            }
            /*
             * If a parameter was explicitly specified by user but has a value inconsistent with the context,
             * log a warning. In addition, the associated boolean value in `contextualParameters` map should
             * have been set to `Boolean.FALSE`.
             */
            if (mismatchedParam != null) {
                final LogRecord record = Resources.forLocale(null).createLogRecord(
                        Level.WARNING,
                        Resources.Keys.MismatchedEllipsoidAxisLength_3,
                        ellipsoid.getName().getCode(),
                        mismatchedParam.getDescriptor().getName().getCode(),
                        mismatchedValue);
                Logging.completeAndLog(CoordinateOperations.LOGGER, getClass(), "create", record);
            }
        }
        return failure;
    }

    /**
     * Completes the parameter group with information about source and target ellipsoid axis lengths,
     * if available. This method writes semi-major and semi-minor parameter values only if they do not
     * already exists in the current parameters.
     *
     * @return the exception if the operation failed, or {@code null} if none. This exception is not thrown now
     *         because the caller may succeed in creating the transform anyway, or otherwise may produce a more
     *         informative exception.
     *
     * @see #getCompletedParameters()
     */
    private RuntimeException completeParameters() throws IllegalArgumentException {
        completedParameters = true;     // Need to be first.
        /*
         * Get a mask telling us if we need to set parameters for the source and/or target ellipsoid.
         * This information should preferably be given by the provider. But if the given provider is
         * not a SIS implementation, use as a fallback whether ellipsoids are provided. This fallback
         * may be less reliable.
         */
        final boolean sourceOnEllipsoid, targetOnEllipsoid;
        if (provider instanceof AbstractProvider) {
            final var p = (AbstractProvider) provider;
            sourceOnEllipsoid = p.sourceOnEllipsoid;
            targetOnEllipsoid = p.targetOnEllipsoid;
        } else {
            sourceOnEllipsoid = sourceEllipsoid != null;
            targetOnEllipsoid = targetEllipsoid != null;
        }
        /*
         * Set the ellipsoid axis-length parameter values. Those parameters may appear in the source ellipsoid,
         * in the target ellipsoid or in both ellipsoids. Only in the latter case, we also try to set the "dim"
         * parameter, because OGC 01-009 defines this parameter only in operation between two geographic CRSs.
         */
        if (!(sourceOnEllipsoid | targetOnEllipsoid)) return null;
        if (!targetOnEllipsoid) return setEllipsoid(sourceEllipsoid, Constants.SEMI_MAJOR, Constants.SEMI_MINOR, true, null);
        if (!sourceOnEllipsoid) return setEllipsoid(targetEllipsoid, Constants.SEMI_MAJOR, Constants.SEMI_MINOR, true, null);

        RuntimeException failure = null;
        if (sourceCS != null) try {
            final ParameterValue<?> p = getContextualParameter(Constants.DIM);
            if (p.getValue() == null) {
                p.setValue(sourceCS.getDimension());
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            failure = e;
        }
        failure = setEllipsoid(sourceEllipsoid, "src_semi_major", "src_semi_minor", false, failure);
        failure = setEllipsoid(targetEllipsoid, "tgt_semi_major", "tgt_semi_minor", false, failure);
        return failure;
    }

    /**
     * Creates the parameterized transform. The operation method is given by {@link #getMethod()}
     * and the parameter values should have been set on the group returned by {@link #parameters()}
     * before to invoke this constructor.
     *
     * @return the parameterized transform.
     * @throws FactoryException if the transform creation failed.
     * This exception is thrown if some required parameters have not been supplied, or have illegal values.
     */
    @Override
    public MathTransform create() throws FactoryException {
        try {
            if (provider instanceof AbstractProvider) {
                /*
                 * The "Geographic/geocentric conversions" conversion (EPSG:9602) can be either:
                 *
                 *    - "Ellipsoid_To_Geocentric"
                 *    - "Geocentric_To_Ellipsoid"
                 *
                 * EPSG defines both by a single operation, but Apache SIS needs to distinguish them.
                 */
                final String method = ((AbstractProvider) provider).resolveAmbiguity(this);
                if (method != null) {
                    provider = (factory instanceof DefaultMathTransformFactory
                            ? (DefaultMathTransformFactory) factory
                            :  DefaultMathTransformFactory.provider()).getOperationMethod(method);
                }
            }
            /*
             * Will catch only exceptions that may be the result of improper parameter usage (e.g. a value out
             * of range). Do not catch exceptions caused by programming errors (e.g. null pointer exception).
             */
            final MathTransform transform;
            if (provider instanceof MathTransformProvider) try {
                transform = ((MathTransformProvider) provider).createMathTransform(this);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                throw new InvalidGeodeticParameterException(exception.getLocalizedMessage(), exception);
            } else {
                throw new FactoryException(Errors.format(
                        Errors.Keys.UnsupportedImplementation_1, Classes.getClass(provider)));
            }
            if (provider instanceof AbstractProvider) {
                provider = ((AbstractProvider) provider).variantFor(transform);
            }
            // A call to `unique` needs to be last because it sets `factory.lastMethod` as a side-effect.
            return unique(swapAndScaleAxes(unique(transform)));
        } catch (FactoryException exception) {
            if (warning != null) {
                exception.addSuppressed(warning);
            }
            throw exception;
        }
    }

    /**
     * Given a transform between normalized spaces,
     * creates a transform taking in account axis directions and units of measurement.
     * This method {@linkplain #createConcatenatedTransform concatenates} the given normalized transform
     * with any other transform required for performing units changes and coordinates swapping.
     *
     * <h4>Design note</h4>
     * The {@code normalized} transform is a black box receiving inputs in any <abbr>CS</abbr> and producing
     * outputs in any <abbr>CS</abbr>, not necessarily of the same kind. For that reason, this method cannot
     * use {@link CoordinateSystems#swapAndScaleAxes(CoordinateSystem, CoordinateSystem)} between the normalized CS.
     * This method have to trust that the callers know that the coordinate systems that they provided are correct
     * for the work done by the transform. The given {@code normalized} transform shall expect
     * {@linkplain org.apache.sis.referencing.cs.AxesConvention#NORMALIZED normalized} input coordinates
     * and produce normalized output coordinates. See {@link org.apache.sis.referencing.cs.AxesConvention}
     * for more information about what Apache SIS means by "normalized".
     *
     * <h4>Example</h4>
     * The most typical examples of transforms with normalized inputs/outputs are normalized
     * map projections expecting (<var>longitude</var>, <var>latitude</var>) inputs in degrees
     * and calculating (<var>x</var>, <var>y</var>) coordinates in metres,
     * both of them with ({@linkplain org.opengis.referencing.cs.AxisDirection#EAST East},
     * {@linkplain org.opengis.referencing.cs.AxisDirection#NORTH North}) axis orientations.
     *
     * <h4>When to use</h4>
     * This method is invoked automatically by {@link #create()} and therefore usually does not need
     * to be invoked explicitly. Explicit calls may be useful when the normalized transform has been
     * constructed by the caller instead of by {@link #create()}.
     *
     * @param  normalized  a transform for normalized input and output coordinates.
     * @return a transform taking in account unit conversions and axis swapping.
     * @throws FactoryException if the object creation failed.
     *
     * @see org.apache.sis.referencing.cs.AxesConvention#NORMALIZED
     * @see org.apache.sis.referencing.operation.DefaultConversion#DefaultConversion(Map, OperationMethod, MathTransform, ParameterValueGroup)
     */
    public MathTransform swapAndScaleAxes(final MathTransform normalized) throws FactoryException {
        ArgumentChecks.ensureNonNull("normalized", normalized);
        /*
         * Compute matrices for swapping axes and performing units conversion.
         * There is one matrix to apply before projection from (λ,φ) coordinates,
         * and one matrix to apply after projection on (easting,northing) coordinates.
         */
        final Matrix swap1 = getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
        final Matrix swap3 = getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        /*
         * Prepare the concatenation of above `swap` matrices with the normalized transform.
         * The chain of transforms built by this method will be:
         *
         *     step1  ⟶  step2 (normalized)  ⟶  step3
         *
         * Note that at this stage, the dimensions between each step may not be compatible.
         * For example, the projection (step2) is usually two-dimensional while the source
         * coordinate system (step1) may be three-dimensional if it has a height.
         */
        MathTransform step1 = swap1 != null ? factory.createAffineTransform(swap1) : MathTransforms.identity(normalized.getSourceDimensions());
        MathTransform step3 = swap3 != null ? factory.createAffineTransform(swap3) : MathTransforms.identity(normalized.getTargetDimensions());
        MathTransform step2 = normalized;
        /*
         * Special case for the way that EPSG handles reversal of axis direction.
         * For now the "Vertical Offset" (EPSG:9616) method is the only known case.
         * But if more special cases are added in a future SIS version, then we should
         * replace the static method by a non-static one defined in `AbstractProvider`.
         */
        if (provider instanceof VerticalOffset) {
            step2 = VerticalOffset.postCreate(step2, swap3);
        }
        /*
         * Add or remove the vertical coordinate of an ellipsoidal or spherical coordinate system.
         * For an ellipsoidal CS, the vertical coordinate is the height and its default value is 0.
         * For a spherical CS, the vertical coordinate is the radius and its value depends on the latitude.
         * If there is more than one dimension to add or remove, the extra dimensions will be handled later.
         *
         * Note that the vertical coordinate is added only if needed by `step2`. If `step2` does not expect
         * a vertical coordinate, then that coordinate is not added here. It will be added later with a NaN
         * value for avoiding a false sense of information availability.
         */
        int sourceDim = step1.getTargetDimensions();      // Number of available dimensions.
        int neededDim = step2.getSourceDimensions();      // Number of required dimensions.
        int kernelDim = step2.getTargetDimensions();      // Result of the core part of transform.
        int resultDim = step3.getSourceDimensions();      // Expected result after the kernel part.
        if (sourceDim == 2 && neededDim > 2) {
            MathTransform change = addOrRemoveVertical(sourceCS, sourceEllipsoid, targetEllipsoid, false);
            if (change != null) {
                step1 = factory.createConcatenatedTransform(step1, change);
                sourceDim = step1.getTargetDimensions();
            }
        }
        if (kernelDim == 3 && resultDim < 3) {
            MathTransform change = addOrRemoveVertical(targetCS, targetEllipsoid, sourceEllipsoid, true);
            if (change != null) {
                step3 = factory.createConcatenatedTransform(change, step3);
                resultDim = step3.getSourceDimensions();
            }
        }
        /*
         * Make the number of target dimensions of `step2` compatible with the number of source dimensions of `step3`.
         * For example, SIS provides only 2D map projections, therefore 3D projections must be generated on the fly
         * by adding a "pass-through" transform.
         */
        final int numTrailingCoordinates = resultDim - kernelDim;
        if (numTrailingCoordinates != 0) {
            ensureDimensionChangeAllowed(numTrailingCoordinates, resultDim, normalized);
            if (numTrailingCoordinates > 0) {
                step2 = factory.createPassThroughTransform(0, step2, numTrailingCoordinates);
            } else {
                step2 = factory.createConcatenatedTransform(step2, addOrRemoveDimensions(kernelDim, resultDim));
            }
            neededDim = step2.getSourceDimensions();
            kernelDim = step2.getTargetDimensions();
        }
        /*
         * Make the number of target dimensions of `step1` compatible with the number of source dimensions of `step2`.
         * If dimensions must be added, their values will be NaN. Note that the vertical dimension (height or radius)
         * has already been added with a non-NaN value before to reach this point if that value was required by `step2`.
         */
        if (sourceDim != neededDim) {
            ensureDimensionChangeAllowed(neededDim - sourceDim, neededDim, normalized);
            step1 = factory.createConcatenatedTransform(step1, addOrRemoveDimensions(sourceDim, neededDim));
        }
        if (kernelDim != resultDim) {
            ensureDimensionChangeAllowed(resultDim - kernelDim, resultDim, normalized);
            step3 = factory.createConcatenatedTransform(addOrRemoveDimensions(kernelDim, resultDim), step3);
        }
        /*
         * Create the transform.
         *
         * Special case: if the parameterized transform was a map projection but the result, after simplification,
         * is an affine transform (it can happen with the Equirectangular projection), wraps the affine transform
         * for continuing to show "semi_major", "semi_minor", etc. parameters instead of "elt_0_0", "elt_0_1", etc.
         */
        MathTransform mt = factory.createConcatenatedTransform(factory.createConcatenatedTransform(step1, step2), step3);
        if (normalized instanceof ParameterizedAffine && !(mt instanceof ParameterizedAffine)) {
            return ((ParameterizedAffine) normalized).newTransform(mt);
        }
        return mt;
    }

    /**
     * Adds or removes the ellipsoidal height or spherical radius dimension.
     *
     * @param  cs         the coordinate system for which to add a dimension, or {@code null} if unknown.
     * @param  ellipsoid  the ellipsoid to use, or {@code null} if unknown.
     * @param  fallback   another ellipsoid that may be used if {@code ellipsoid} is null.
     * @param  remove     {@code false} for adding the dimension, or {@code true} for removing the dimension.
     * @return the transform to concatenate, or {@code null} if the coordinate system is not recognized.
     * @throws FactoryException if an error occurred while creating the transform.
     *
     * @see org.apache.sis.referencing.operation.transform.CoordinateSystemTransformBuilder#addOrRemoveVertical
     */
    private MathTransform addOrRemoveVertical(final CoordinateSystem cs, Ellipsoid ellipsoid,
            final Ellipsoid fallback, final boolean remove) throws FactoryException
    {
        if (ellipsoid == null) {
            ellipsoid = fallback;
        }
        MathTransform tr;
        if (cs instanceof EllipsoidalCS) {
            Matrix resize = Matrices.createDiagonal(4, 3);  // Set the height to zero (not NaN).
            resize.setElement(3, 2, 1);                     // Element in the lower-right corner.
            resize.setElement(2, 2, Geographic2Dto3D.DEFAULT_HEIGHT);
            tr = factory.createAffineTransform(resize);
        } else if (ellipsoid != null && cs instanceof SphericalCS) {
            tr = EllipsoidToRadiusTransform.createGeodeticConversion(factory, ellipsoid);
        } else {
            return null;
        }
        if (remove) try {
            tr = tr.inverse();
        } catch (NoninvertibleTransformException cause) {
            throw new FactoryException(cause.getMessage(), cause);
        }
        return tr;
    }

    /**
     * Adds or removes an arbitrary number of dimensions.
     * Coordinate values of added dimensions will be NaN.
     *
     * @param  sourceDim  the current number of dimensions.
     * @param  targetDim  the desired number of dimensions.
     * @return the transform to concatenate.
     * @throws FactoryException if the transform cannot be created.
     */
    private MathTransform addOrRemoveDimensions(final int sourceDim, final int targetDim) throws FactoryException {
        final Matrix resize = Matrices.createDiagonal(targetDim+1, sourceDim+1);
        if (sourceDim < targetDim) {
            for (int j=sourceDim; j<targetDim; j++) {
                resize.setElement(j, sourceDim, Double.NaN);
            }
        } else {
            resize.setElement(targetDim, targetDim, 0);
        }
        resize.setElement(targetDim, sourceDim, 1);     // Element in the lower-right corner.
        return factory.createAffineTransform(resize);
    }

    /**
     * Checks whether {@link #swapAndScaleAxes(MathTransform)} should accept to adjust the number of dimensions.
     * This method is for catching errors caused by wrong coordinate systems associated to a parameterized transform,
     * keeping in mind that it is not {@link DefaultMathTransformFactory} job to handle changes between arbitrary CRS
     * (those changes are handled by {@link org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory}).
     *
     * <h4>Current rules</h4>
     * The current implementation accepts only addition or removal of ellipsoidal height.
     * Future Apache SIS versions may expand the list of accepted cases.
     *
     * @param  change      number of dimensions to add (if positive) or remove (if negative).
     * @param  resultDim   number of dimensions after the change.
     * @param  normalized  the parameterized transform, for producing an error message if needed.
     */
    private void ensureDimensionChangeAllowed(final int change, final int resultDim, final MathTransform normalized)
            throws FactoryException
    {
        if (Math.abs(change) == 1 && resultDim >= 2 && resultDim <= 3) {
            if (sourceCS instanceof EllipsoidalCS || targetCS instanceof EllipsoidalCS) {
                return;
            }
        }
        /*
         * Creates the error message for a transform that cannot be associated with given coordinate systems.
         */
        String name = null;
        if (normalized instanceof Parameterized) {
            name = IdentifiedObjects.getDisplayName(((Parameterized) normalized).getParameterDescriptors(), null);
        }
        if (name == null) {
            name = Classes.getShortClassName(normalized);
        }
        final var b = new StringBuilder();
        getSourceDimensions().ifPresent((dim) -> b.append(dim).append("D → "));
        b.append("tr(").append(normalized.getSourceDimensions()).append("D → ")
                       .append(normalized.getTargetDimensions()).append("D)");
        getTargetDimensions().ifPresent((dim) -> b.append(" → ").append(dim).append('D'));
        throw new InvalidGeodeticParameterException(Resources.format(Resources.Keys.CanNotAssociateToCS_2, name, b));
    }

    /**
     * Returns a string representation of this builder/context for debugging purposes.
     * The current implementation writes the name of source/target coordinate systems and ellipsoids.
     * If the {@linkplain #getContextualParameters() contextual parameters} have already been inferred,
     * then their names are appended with inconsistent parameters (if any) written on a separated line.
     *
     * @return a string representation of this builder/context.
     */
    @Override
    public String toString() {
        final Object[] properties = {
            "sourceCS", sourceCS, "sourceEllipsoid", sourceEllipsoid,
            "targetCS", targetCS, "targetEllipsoid", targetEllipsoid
        };
        for (int i=1; i<properties.length; i += 2) {
            final var value = (IdentifiedObject) properties[i];
            if (value != null) properties[i] = value.getName();
        }
        String text = Strings.toString(getClass(), properties);
        if (!contextualParameters.isEmpty()) {
            final var b = new StringBuilder(text);
            boolean isContextual = true;
            do {
                boolean first = true;
                for (final Map.Entry<String,Boolean> entry : contextualParameters.entrySet()) {
                    if (entry.getValue() == isContextual) {
                        if (first) {
                            first = false;
                            b.append(System.lineSeparator())
                             .append(isContextual ? "Contextual parameters" : "Inconsistencies").append(": ");
                        } else {
                            b.append(", ");
                        }
                        b.append(entry.getKey());
                    }
                }
            } while ((isContextual = !isContextual) == false);
            text = b.toString();
        }
        return text;
    }
}
