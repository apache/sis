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
package org.apache.sis.internal.referencing.provider;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.xml.bind.annotation.XmlTransient;
import org.opengis.util.GenericName;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Workaround;


/**
 * Base class for all providers defined in this package.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
@XmlTransient
public abstract class AbstractProvider extends DefaultOperationMethod implements MathTransformProvider {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2239172887926695217L;

    /**
     * Constructs a math transform provider from the given properties and a set of parameters.
     *
     * @param properties      Set of properties. Shall contain at least {@code "name"}.
     * @param sourceDimension Number of dimensions in the source CRS of this operation method.
     * @param targetDimension Number of dimensions in the target CRS of this operation method.
     * @param parameters      The set of parameters (never {@code null}).
     */
    AbstractProvider(final Map<String,?> properties,
                     final int sourceDimension,
                     final int targetDimension,
                     final ParameterDescriptorGroup parameters)
    {
        super(properties, sourceDimension, targetDimension, parameters);
    }

    /**
     * Constructs a math transform provider from a set of parameters. The provider name and
     * {@linkplain #getIdentifiers() identifiers} will be the same than the parameter ones.
     *
     * @param sourceDimensions Number of dimensions in the source CRS of this operation method.
     * @param targetDimensions Number of dimensions in the target CRS of this operation method.
     * @param parameters       Description of parameters expected by this operation.
     */
    AbstractProvider(final int sourceDimensions,
                     final int targetDimensions,
                     final ParameterDescriptorGroup parameters)
    {
        super(toMap(parameters), sourceDimensions, targetDimensions, parameters);
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private static Map<String,Object> toMap(final IdentifiedObject parameters) {
        ArgumentChecks.ensureNonNull("parameters", parameters);
        final Map<String,Object> properties = new HashMap<String,Object>(4);
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
        return builder.createBounded(MeasurementRange.create(-0.0, true, zero, true, NonSI.DEGREE_ANGLE), zero);
    }

    /**
     * Creates a descriptor for a latitude parameter in degrees without default value.
     * This method is used for latitude of origin that can not be zero, of for standard parallels
     * where the default value should be the value of another parameter instead than 0°.
     */
    static ParameterDescriptor<Double> createMandatoryLatitude(final ParameterBuilder builder) {
        return builder.createBounded(Latitude.MIN_VALUE, Latitude.MAX_VALUE, Double.NaN, NonSI.DEGREE_ANGLE);
    }

    /**
     * Creates a descriptor for a latitude parameter in degrees with a default value of 0°.
     */
    static ParameterDescriptor<Double> createLatitude(final ParameterBuilder builder, final boolean includePoles) {
        return builder.createBounded(MeasurementRange.create(
                Latitude.MIN_VALUE, includePoles,
                Latitude.MAX_VALUE, includePoles,
                NonSI.DEGREE_ANGLE), 0.0);
    }

    /**
     * Creates a descriptor for a longitude parameter in degrees with a default value of 0°.
     */
    static ParameterDescriptor<Double> createLongitude(final ParameterBuilder builder) {
        return builder.createBounded(Longitude.MIN_VALUE, Longitude.MAX_VALUE, 0.0, NonSI.DEGREE_ANGLE);
    }

    /**
     * Creates a descriptor for a scale parameter with a default value of 1.
     */
    static ParameterDescriptor<Double> createScale(final ParameterBuilder builder) {
        return builder.createStrictlyPositive(1.0, Unit.ONE);
    }

    /**
     * Creates a false easting, false northing or height parameter in metre with a default value of 0.
     */
    static ParameterDescriptor<Double> createShift(final ParameterBuilder builder) {
        return builder.create(0.0, SI.METRE);
    }

    /**
     * Flags whether the source and/or target ellipsoid are concerned by this operation. This method is invoked by
     * {@link org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory} for determining if this
     * operation has {@code "semi_major"}, {@code "semi_minor"}, {@code "src_semi_major"}, {@code "src_semi_minor"}
     * parameters that may need to be filled with values inferred from the source or target
     * {@link org.apache.sis.referencing.datum.DefaultGeodeticDatum}.
     * Meaning of return values:
     *
     * <ul>
     *   <li>0 if neither the source coordinate system or the destination coordinate system is ellipsoidal.
     *       There is no parameter that need to be completed.</li>
     *   <li>1 if this operation has {@code "semi_major"} and {@code "semi_minor"} parameters that need
     *       to be set to the axis lengths of the source ellipsoid.</li>
     *   <li>2 if this operation has {@code "semi_major"} and {@code "semi_minor"} parameters that need
     *       to be set to the axis lengths of the target ellipsoid.</li>
     *   <li>3 if this operation has {@code "src_semi_major"}, {@code "src_semi_minor"}, {@code "tgt_semi_major"}
     *       and {@code "tgt_semi_minor"} parameters that need to be set to the axis lengths of the source and
     *       target ellipsoids.</li>
     * </ul>
     *
     * This method is just a hint. If the information is not provided, {@code DefaultMathTransformFactory}
     * will try to infer it from the type of user-specified source and target CRS.
     *
     * @return 0, 1, 2 or 3.
     */
    public int getEllipsoidsMask() {
        return 0;
    }

    /**
     * Returns {@code true} if the inverse of this operation method is the same operation method with some parameter
     * values changed (typically with sign inverted). The default implementation returns {@code false}.
     *
     * <p>This is a SIS-specific information which may be changed in any future SIS version.
     * Current implementation provides this information in a "all or nothing" way: either all parameter values
     * can have their sign reversed, or either the operation is considered not revertible at all.
     * This is different than the EPSG dataset in two way:</p>
     *
     * <ul class="verbose">
     *   <li>EPSG provides an equivalent information in the {@code PARAM_SIGN_REVERSAL} column of the
     *       {@code [Coordinate_Operation Parameter Usage]} table, but on a parameter-by-parameter basis
     *       instead than for the whole operation (which is probably better).</li>
     *
     *   <li>EPSG provides another information in the {@code REVERSE_OP} column of the
     *       {@code [Coordinate_Operation Method]} table, but this is not equivalent to this method because it
     *       does not differentiate the map projection methods from <em>inverse</em> map projection methods.</li>
     * </ul>
     *
     * @return {@code true} if the inverse of this operation method can be described by the same operation method.
     *
     * @see org.apache.sis.internal.referencing.SignReversalComment
     */
    public boolean isInvertible() {
        return false;
    }
}
