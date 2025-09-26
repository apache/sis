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
package org.apache.sis.referencing.operation.provider;

import java.util.Map;
import java.util.logging.Logger;
import jakarta.xml.bind.annotation.XmlTransient;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.SingleOperation;
import org.apache.sis.measure.Units;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.system.Loggers;


/**
 * Base class for all providers defined in this package.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@XmlTransient
public abstract class AbstractProvider extends DefaultOperationMethod implements MathTransformProvider {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5087102598171128284L;

    /**
     * The logger for the creation of coordinate operation. The logger name is {@value Loggers#CRS_FACTORY}
     * because the logs should be emitted in the context of some {@code FooFactory.createBar(…)} methods.
     */
    public static final Logger LOGGER = Logger.getLogger(Loggers.CRS_FACTORY);

    /**
     * The base interface of the {@code CoordinateOperation} instances that use this method.
     * Value can be {@link org.opengis.referencing.operation.SingleOperation},
     * {@link org.opengis.referencing.operation.Conversion},
     * {@link org.opengis.referencing.operation.Transformation} or
     * {@link org.opengis.referencing.operation.PointMotionOperation}.
     *
     * @see #getOperationType()
     */
    private final Class<? extends SingleOperation> operationType;

    /**
     * The base interface of the coordinate system of source coordinates.
     * This is used for resolving some ambiguities at <abbr>WKT</abbr> parsing time.
     *
     * <h4>Deprecation</h4>
     * This field is not formally annotated as deprecated, but should be considered as such.
     * The coordinate system type can be specified by the {@code context} argument given in
     * calls to the {@code create(…)} method. In particular, {@link GeographicToGeocentric}
     * accepts Cartesian and spherical coordinate systems. In such cases, this field should
     * be set to the type expected in Well-Known Text (<abbr>WKT</abbr>) definitions.
     * This is not necessarily the only type accepted by this operation method.
     */
    public final Class<? extends CoordinateSystem> sourceCSType, targetCSType;

    /**
     * Flags whether the operation needs source and/or target ellipsoid axis lengths.
     * Those flags are read by {@link DefaultMathTransformFactory} for determining if
     * this operation has parameters that may need to be filled with values inferred
     * from the source or target {@link org.opengis.referencing.datum.GeodeticDatum}.
     * Meaning of flag combinations are:
     *
     * <ul>
     *   <li>({@code false},{@code false}) if neither the source coordinate system or the destination
     *       coordinate system is ellipsoidal. There is no parameter that needs to be completed.</li>
     *   <li>({@code true},{@code false}) if this operation has {@code "semi_major"} and {@code "semi_minor"}
     *       parameters that need to be set to the axis lengths of the source ellipsoid.</li>
     *   <li>({@code false},{@code true}) if this operation has {@code "semi_major"} and {@code "semi_minor"}
     *       parameters that need to be set to the axis lengths of the target ellipsoid.</li>
     *   <li>({@code true},{@code true}) if this operation has {@code "src_semi_major"}, {@code "src_semi_minor"},
     *       {@code "tgt_semi_major"} and {@code "tgt_semi_minor"} parameters that need to be set to the axis lengths
     *       of the source and target ellipsoids.</li>
     * </ul>
     *
     * Those flags are not necessarily redundant with above {@code CSType} fields. For example, a source or target
     * coordinate system set to {@code EllipsoidCS.class} does not necessarily imply that this flag must be set to
     * {@code true}, because longitude rotation (for example) does not need to know the ellipsoid axis lengths.
     * Conversely, an operation working on Cartesian coordinate system may need to know ellipsoid axis lengths.
     */
    public final boolean sourceOnEllipsoid, targetOnEllipsoid;

    /**
     * Minimum number of source dimensions (typically 1, 2 or 3).
     */
    public final byte minSourceDimension;

    /**
     * Constructs a math transform provider from a set of parameters. The provider name and
     * {@linkplain #getIdentifiers() identifiers} will be the same as the parameter ones.
     *
     * @param operationType       base interface of the {@code CoordinateOperation} instances that use this method.
     * @param parameters          description of parameters expected by this operation.
     * @param sourceCSType        base interface of the coordinate system of source coordinates.
     * @param sourceOnEllipsoid   whether the operation needs source ellipsoid axis lengths.
     * @param targetCSType        base interface of the coordinate system of target coordinates.
     * @param targetOnEllipsoid   whether the operation needs target ellipsoid axis lengths.
     * @param minSourceDimension  minimum number of source dimensions (typically 1, 2 or 3).
     */
    AbstractProvider(final Class<? extends SingleOperation> operationType, final ParameterDescriptorGroup parameters,
                     final Class<? extends CoordinateSystem> sourceCSType, final boolean sourceOnEllipsoid,
                     final Class<? extends CoordinateSystem> targetCSType, final boolean targetOnEllipsoid,
                     final byte minSourceDimension)
    {
        super(IdentifiedObjects.getProperties(parameters), parameters);
        this.operationType      = operationType;
        this.sourceCSType       = sourceCSType;
        this.targetCSType       = targetCSType;
        this.sourceOnEllipsoid  = sourceOnEllipsoid;
        this.targetOnEllipsoid  = targetOnEllipsoid;
        this.minSourceDimension = minSourceDimension;
    }

    /**
     * Creates the parameter builder with the default namespace set to EPSG.
     */
    static ParameterBuilder builder() {
        return new ParameterBuilder().setCodeSpace(Citations.EPSG, Constants.EPSG).setRequired(true);
    }

    /**
     * Adds a name together with its previous (legacy) name.
     * The legacy name will be added as a deprecated alias.
     */
    static ParameterBuilder addNameAndLegacy(final ParameterBuilder builder, final String name, final String legacy) {
        return builder.addName(name).setDeprecated(true)
                .setRemarks(Vocabulary.formatInternational(Vocabulary.Keys.SupersededBy_1, name))
                .addName(legacy).setDeprecated(false).setRemarks(null);
    }

    /**
     * Adds an identifier code together with its previous (legacy) code.
     * The legacy code will be added as a deprecated identifier.
     */
    static ParameterBuilder addIdentifierAndLegacy(final ParameterBuilder builder, final String code, final String legacy) {
        return builder.addIdentifier(code).setDeprecated(true)
                .setRemarks(Vocabulary.formatInternational(Vocabulary.Keys.SupersededBy_1, code))
                .addIdentifier(legacy).setDeprecated(false).setRemarks(null);
    }

    /**
     * Creates a descriptor for a 0 constant value in degrees.
     *
     * @see MapProjection#validate(ParameterDescriptor, double)
     */
    static ParameterDescriptor<Double> createZeroConstant(final ParameterBuilder builder) {
        final Double zero = +0.0;
        return builder.createBounded(MeasurementRange.create(-0.0, true, zero, true, Units.DEGREE), zero);
    }

    /**
     * Creates a descriptor for a latitude parameter in degrees without default value.
     * This method is used for latitude of origin that cannot be zero, of for standard parallels
     * where the default value should be the value of another parameter instead of 0°.
     */
    static ParameterDescriptor<Double> createMandatoryLatitude(final ParameterBuilder builder) {
        return builder.createBounded(Latitude.MIN_VALUE, Latitude.MAX_VALUE, Double.NaN, Units.DEGREE);
    }

    /**
     * Creates a descriptor for a latitude parameter in degrees with a default value of 0°.
     */
    static ParameterDescriptor<Double> createLatitude(final ParameterBuilder builder, final boolean includePoles) {
        return builder.createBounded(MeasurementRange.create(
                Latitude.MIN_VALUE, includePoles,
                Latitude.MAX_VALUE, includePoles,
                Units.DEGREE), 0.0);
    }

    /**
     * Creates a descriptor for a longitude parameter in degrees with a default value of 0°.
     */
    static ParameterDescriptor<Double> createLongitude(final ParameterBuilder builder) {
        return builder.createBounded(Longitude.MIN_VALUE, Longitude.MAX_VALUE, 0.0, Units.DEGREE);
    }

    /**
     * Creates a descriptor for a scale parameter with a default value of 1.
     */
    static ParameterDescriptor<Double> createScale(final ParameterBuilder builder) {
        return builder.createStrictlyPositive(1.0, Units.UNITY);
    }

    /**
     * Creates a false easting, false northing or height parameter in metre with a default value of 0.
     */
    static ParameterDescriptor<Double> createShift(final ParameterBuilder builder) {
        return builder.create(0.0, Units.METRE);
    }

    /**
     * Creates an ellipsoid from the given parameter values.
     * The axis lengths are read from the parameter values identified by {@code semiMajor} and {@code semiMinor}.
     * An arbitrary ellipsoid name such as "source" or "target" is used. The returned ellipsoid should be used
     * only for the time needed for building the math transform (because the returned ellipsoid lacks metadata).
     *
     * <p>Callers should try to get the ellipsoid from the {@link Context} before to invoke this method,
     * because the original object contains more accurate information (e.g., whether inverse flattening
     * is the defining parameter instead of semi-major axis length).</p>
     *
     * @param  name       the arbitrary name to use for the ellipsoid.
     * @param  values     the parameters from which to get the axis lengths and unit.
     * @param  semiMajor  the descriptor for fetching the semi-major axis length.
     * @param  semiMinor  the descriptor for fetching the semi-minor axis length.
     * @return a temporary ellipsoid to use for creating the math transform.
     * @throws ClassCastException if the unit of measurement of an axis length parameter is not linear.
     *
     * @todo Get the ellipsoid from the {@link Context}.
     */
    static Ellipsoid getEllipsoid(final String name,
                                  final Parameters values,
                                  final ParameterDescriptor<Double> semiMajor,
                                  final ParameterDescriptor<Double> semiMinor)
    {
        final ParameterValue<?> p = values.getOrCreate(semiMajor);
        final Unit<Length> unit = p.getUnit().asType(Length.class);
        final double a = p.doubleValue();
        final double b = values.doubleValue(semiMinor, unit);
        return DefaultEllipsoid.createEllipsoid(Map.of(NAME_KEY, name), a, b, unit);
    }

    /**
     * Returns the interface implemented by the coordinate operation.
     * This method returns the type specified at construction time.
     *
     * @return interface implemented by all coordinate operations that use this method.
     */
    @Override
    public final Class<? extends SingleOperation> getOperationType() {
        return operationType;
    }

    /**
     * If an operation method is ambiguous according Apache SIS, returns the name of the method that SIS should use.
     * Otherwise returns {@code null}. The ambiguities that need to be resolved are:
     *
     * <ul>
     *   <li>Method <q>Geographic/geocentric conversions</q> (EPSG:9602) can be either:
     *     <ul>
     *       <li>{@code "Ellipsoid_To_Geocentric"} (implemented by {@link GeographicToGeocentric}</li>
     *       <li>{@code "Geocentric_To_Ellipsoid"} (implemented by {@link GeocentricToGeographic}</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param  context   the potentially ambiguous context.
     * @return name of the provider to use, or {@code null} if there is nothing to change.
     */
    public String resolveAmbiguity(Context context) {
        return null;
    }

    /**
     * If this provider has a variant for the specified transform, returns that variant.
     * The default implementation unconditionally returns {@code this}, ignoring the transform.
     * Subclasses may override this method when the name or the parameters of the operation method
     * depend on the number of dimensions.
     *
     * <h4>Example</h4>
     * The EPSG database defines the following as two distinct operations.
     * However they can be understood as two variants of the same operation.
     *
     * <ul>
     *   <li>EPSG:9603 — Geocentric translations (geog2D domain)</li>
     *   <li>EPSG:1035 — Geocentric translations (geog3D domain)</li>
     * </ul>
     *
     * Implementations do not need to ensure that the returned value is a perfect match for the given transform.
     * They only need to return the closest match.
     *
     * @param  transform  the transform for which to get a variant of this operation method.
     * @return the closest match for the given transform, or {@code this} in case of doubt.
     */
    public AbstractProvider variantFor(final MathTransform transform) {
        return this;
    }

    /**
     * Whether this provider expects source and target <abbr>CRS</abbr> in normalized units and axis order.
     * The <abbr>EPSG</abbr> guidance note identifies two categories of formulas regarding their relationship
     * with axis order and units of measurement:
     *
     * <ul>
     *   <li>Formulas where an intrinsic unambiguous relationship exists.</li>
     *   <li>Formulas where no intrinsic relationship exists, in particular affine and polynomial transformations.</li>
     * </ul>
     *
     * The default implementation returns {@link FormulaCategory#ASSUME_NORMALIZED_CRS}
     * as most coordinate operation methods fall into this category.
     *
     * @return whether axes should be swapped and scaled to normalized order and units.
     *
     * @todo consider moving this method to {@link MathTransformProvider}.
     */
    public FormulaCategory getFormulaCategory() {
        return FormulaCategory.ASSUME_NORMALIZED_CRS;
    }

    /**
     * Returns the operation method which is the inverse of this method.
     * The returns value may be {@code null}, {@code this} or other:
     *
     * <ul>
     *   <li>If {@code null}, no inverse method is easily available. This is the default.</li>
     *   <li>If {@code this}, the inverse of this operation method is the same operation method
     *       with some parameter values changed (typically with sign inverted).</li>
     *   <li>If another method, it should take the same parameter values.</li>
     * </ul>
     *
     * This is a SIS-specific information which may be changed in any future SIS version.
     * Current implementation provides this information in a "all or nothing" way: either all parameter values
     * can have their sign reversed, or either the operation is considered not revertible at all.
     * This is different than the EPSG dataset in two ways:
     *
     * <ul class="verbose">
     *   <li><abbr>EPSG</abbr> provides an equivalent information in the {@code PARAM_SIGN_REVERSAL} column
     *       of the {@code "Coordinate_Operation Parameter Usage"} table, but on a parameter-by-parameter basis
     *       instead of for the whole operation (which is probably better).</li>
     *
     *   <li><abbr>EPSG</abbr> provides another information in the {@code REVERSE_OP} column of the
     *       {@code "Coordinate_Operation Method"} table, but this is not equivalent to this method because it
     *       does not differentiate the map projection methods from <em>inverse</em> map projection methods.</li>
     * </ul>
     *
     * @return the inverse of this operation method (possibly {@code this}), or {@code null} if none.
     *
     * @see org.apache.sis.referencing.internal.SignReversalComment
     */
    public AbstractProvider inverse() {
        return null;
    }

    /**
     * Returns the maximum number of dimensions in the given transform.
     *
     * @param  transform  the transform for which to get the maximal number of dimensions.
     * @return the maximum between the source and target number of dimensions.
     */
    static int maxDimension(final MathTransform transform) {
        return Math.max(transform.getSourceDimensions(), transform.getTargetDimensions());
    }

    /**
     * Convenience method for reporting a non-fatal error at transform construction time.
     * This method assumes that the error occurred (indirectly) during execution of
     * {@link #createMathTransform(Context)}.
     *
     * @param  caller  the provider class in which the error occurred.
     * @param  e       the error that occurred.
     */
    static void recoverableException(final Class<? extends AbstractProvider> caller, Exception e) {
        Logging.recoverableException(LOGGER, caller, "createMathTransform", e);
    }
}
