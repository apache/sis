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
import java.util.HashMap;
import java.util.Collection;
import java.util.logging.Logger;
import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.util.GenericName;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.SingleOperation;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.internal.Constants;
import org.apache.sis.measure.Units;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.system.Loggers;
import org.apache.sis.referencing.internal.Resources;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.ReferenceIdentifier;


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
    private static final long serialVersionUID = 1165868434518724597L;

    /**
     * The logger for coordinate operations.
     */
    public static final Logger LOGGER = Logger.getLogger(Loggers.COORDINATE_OPERATION);

    /**
     * The base interface of the {@code CoordinateOperation} instances that use this method.
     *
     * @see #getOperationType()
     */
    private final Class<? extends SingleOperation> operationType;

    /**
     * The base interface of the coordinate system of source/target coordinates.
     * This is used for resolving some ambiguities at WKT parsing time.
     */
    public final Class<? extends CoordinateSystem> sourceCSType, targetCSType;

    /**
     * Flags whether the source and/or target ellipsoid are concerned by this operation. Those flags are read by
     * {@link org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory} for determining if this
     * operation has {@code "semi_major"}, {@code "semi_minor"}, {@code "src_semi_major"}, {@code "src_semi_minor"}
     * parameters that may need to be filled with values inferred from the source or target
     * {@link org.apache.sis.referencing.datum.DefaultGeodeticDatum}.
     * Meaning of return values:
     *
     * <ul>
     *   <li>({@code false},{@code false}) if neither the source coordinate system or the destination
     *       coordinate system is ellipsoidal. There are no parameters that need to be completed.</li>
     *   <li>({@code true},{@code false}) if this operation has {@code "semi_major"} and {@code "semi_minor"}
     *       parameters that need to be set to the axis lengths of the source ellipsoid.</li>
     *   <li>({@code false},{@code true}) if this operation has {@code "semi_major"} and {@code "semi_minor"}
     *       parameters that need to be set to the axis lengths of the target ellipsoid.</li>
     *   <li>({@code true},{@code true}) if this operation has {@code "src_semi_major"}, {@code "src_semi_minor"},
     *       {@code "tgt_semi_major"} and {@code "tgt_semi_minor"} parameters that need to be set to the axis lengths
     *       of the source and target ellipsoids.</li>
     * </ul>
     *
     * Those flags are only hints. If the information is not provided, {@code DefaultMathTransformFactory}
     * will try to infer it from the type of user-specified source and target CRS.
     */
    public final boolean sourceOnEllipsoid, targetOnEllipsoid;

    /**
     * Constructs a math transform provider from the given properties and a set of parameters.
     *
     * @param properties  set of properties. Shall contain at least {@code "name"}.
     * @param parameters  the set of parameters (never {@code null}).
     */
    protected AbstractProvider(final Map<String,?> properties,
                               final ParameterDescriptorGroup parameters)
    {
        super(properties, parameters);
        operationType = SingleOperation.class;
        sourceCSType  = CoordinateSystem.class;
        targetCSType  = CoordinateSystem.class;
        sourceOnEllipsoid = false;
        targetOnEllipsoid = false;
    }

    /**
     * Constructs a math transform provider from a set of parameters. The provider name and
     * {@linkplain #getIdentifiers() identifiers} will be the same as the parameter ones.
     *
     * @param operationType      base interface of the {@code CoordinateOperation} instances that use this method.
     * @param parameters         description of parameters expected by this operation.
     * @param sourceCSType       base interface of the coordinate system of source coordinates.
     * @param sourceOnEllipsoid  whether the operation needs source ellipsoid axis lengths.
     * @param targetCSType       base interface of the coordinate system of target coordinates.
     * @param targetOnEllipsoid  whether the operation needs target ellipsoid axis lengths.
     */
    AbstractProvider(final Class<? extends SingleOperation> operationType, final ParameterDescriptorGroup parameters,
                     final Class<? extends CoordinateSystem> sourceCSType, final boolean sourceOnEllipsoid,
                     final Class<? extends CoordinateSystem> targetCSType, final boolean targetOnEllipsoid)
    {
        super(toMap(parameters), parameters);
        this.operationType     = operationType;
        this.sourceCSType      = sourceCSType;
        this.targetCSType      = targetCSType;
        this.sourceOnEllipsoid = sourceOnEllipsoid;
        this.targetOnEllipsoid = targetOnEllipsoid;
    }

    /**
     * Creates a copy of this provider.
     *
     * @deprecated This is a temporary constructor before replacement by a {@code provider()} method with JDK9.
     */
    @Deprecated
    AbstractProvider(final AbstractProvider copy) {
        super(copy);
        operationType     = copy.operationType;
        sourceCSType      = copy.sourceCSType;
        targetCSType      = copy.targetCSType;
        sourceOnEllipsoid = copy.sourceOnEllipsoid;
        targetOnEllipsoid = copy.targetOnEllipsoid;
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private static Map<String,Object> toMap(final IdentifiedObject parameters) {
        // Implicit null value check below.
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(NAME_KEY, parameters.getName());
        final Collection<ReferenceIdentifier> identifiers = parameters.getIdentifiers();
        int size = identifiers.size();
        if (size != 0) {
            properties.put(IDENTIFIERS_KEY, identifiers.toArray(new ReferenceIdentifier[size]));
        }
        final Collection<GenericName> aliases = parameters.getAlias();
        size = aliases.size();
        if (size != 0) {
            properties.put(ALIAS_KEY, aliases.toArray(new GenericName[size]));
        }
        return properties;
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
    public String resolveAmbiguity(final DefaultMathTransformFactory.Context context) {
        return null;
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
     * If this provider has a variant for the specified transform, returns that variant. Otherwise returns {@code this}.
     * For example the operation method may have different names depending on the number of dimensions.
     *
     * @param  transform  the transform for which to get a variant of the operation method.
     * @return the operation method for the given transform, or {@code this} if there is no specialized variant.
     */
    public AbstractProvider variantFor(final MathTransform transform) {
        return redimension(transform.getSourceDimensions(), transform.getTargetDimensions());
    }

    /**
     * Returns an operation method equivalent to this one but with the specified number dimensions.
     * The default implementation verifies that the arguments are valid, then returns {@code this}.
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
     * @param  sourceDimensions  the desired number of input dimensions.
     * @param  targetDimensions  the desired number of output dimensions.
     * @return the redimensioned operation method, or {@code this} if no change is needed.
     * @throws IllegalArgumentException if the given dimensions are illegal for this operation method.
     */
    public AbstractProvider redimension(final int sourceDimensions, final int targetDimensions) {
        ArgumentChecks.ensureStrictlyPositive("sourceDimensions", sourceDimensions);
        ArgumentChecks.ensureStrictlyPositive("targetDimensions", targetDimensions);
        @SuppressWarnings("deprecation") final Integer src = getSourceDimensions();
        @SuppressWarnings("deprecation") final Integer tgt = getTargetDimensions();
        if ((src == null || src == sourceDimensions) &&
            (tgt == null || tgt == targetDimensions))
        {
            return this;
        }
        throw new IllegalArgumentException(Resources.format(Resources.Keys.IllegalOperationDimension_3,
                    getName().getCode(), sourceDimensions, targetDimensions));
    }

    /**
     * Returns the operation method which is the inverse of this method.
     * <ul>
     *   <li>If {@code null}, no inverse method is easily available. This is the default.</li>
     *   <li>If {@code this}, the inverse of this operation method is the same operation method
     *       with some parameter values changed (typically with sign inverted).</li>
     *   <li>If another method, it should take the same parameter values.</li>
     * </ul>
     *
     * <p>This is a SIS-specific information which may be changed in any future SIS version.
     * Current implementation provides this information in a "all or nothing" way: either all parameter values
     * can have their sign reversed, or either the operation is considered not revertible at all.
     * This is different than the EPSG dataset in two way:</p>
     *
     * <ul class="verbose">
     *   <li>EPSG provides an equivalent information in the {@code PARAM_SIGN_REVERSAL} column of the
     *       {@code [Coordinate_Operation Parameter Usage]} table, but on a parameter-by-parameter basis
     *       instead of for the whole operation (which is probably better).</li>
     *
     *   <li>EPSG provides another information in the {@code REVERSE_OP} column of the
     *       {@code [Coordinate_Operation Method]} table, but this is not equivalent to this method because it
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
     * Convenience method for reporting a non-fatal error at transform construction time.
     * This method assumes that the error occurred (indirectly) during execution of
     * {@link #createMathTransform(MathTransformFactory, ParameterValueGroup)}.
     *
     * @param  caller  the provider class in which the error occurred.
     * @param  e       the error that occurred.
     */
    static void recoverableException(final Class<? extends AbstractProvider> caller, Exception e) {
        Logging.recoverableException(LOGGER, caller, "createMathTransform", e);
    }
}
