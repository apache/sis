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
import java.util.NoSuchElementException;
import javax.xml.bind.annotation.XmlTransient;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.util.GenericName;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.Projection;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.operation.projection.NormalizedProjection;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.Debug;

import static org.opengis.metadata.Identifier.AUTHORITY_KEY;


/**
 * Base class for all map projection providers defined in this package. This base class defines some descriptors
 * for the most commonly used parameters. Subclasses will declare additional parameters and group them in a
 * {@linkplain ParameterDescriptorGroup descriptor group} named {@code PARAMETERS}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
@XmlTransient
public abstract class MapProjection extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6280666068007678702L;

    /**
     * All names known to Apache SIS for the <cite>semi-major</cite> parameter.
     * This parameter is mandatory and has no default value. The range of valid values is (0 … ∞).
     *
     * <p>Some names for this parameter are {@code "semi_major"}, {@code "SemiMajor"} and {@code "a"}.</p>
     */
    public static final DefaultParameterDescriptor<Double> SEMI_MAJOR;

    /**
     * All names known to Apache SIS for the <cite>semi-minor</cite> parameter.
     * This parameter is mandatory and has no default value. The range of valid values is (0 … ∞).
     *
     * <p>Some names for this parameter are {@code "semi_minor"}, {@code "SemiMinor"} and {@code "b"}.</p>
     */
    public static final DefaultParameterDescriptor<Double> SEMI_MINOR;

    /**
     * The ellipsoid eccentricity, computed from the semi-major and semi-minor axis lengths.
     * This a SIS-specific parameter used mostly for debugging purpose.
     */
    @Debug
    public static final DefaultParameterDescriptor<Double> ECCENTRICITY;
    static {
        final MeasurementRange<Double> valueDomain = MeasurementRange.createGreaterThan(0, SI.METRE);
        final GenericName[] aliases = {
            new NamedIdentifier(Citations.ESRI,    "Semi_Major"),
            new NamedIdentifier(Citations.NETCDF,  "semi_major_axis"),
            new NamedIdentifier(Citations.GEOTIFF, "SemiMajor"),
            new NamedIdentifier(Citations.PROJ4,   "a")
        };
        final Map<String,Object> properties = new HashMap<String,Object>(4);
        properties.put(AUTHORITY_KEY, Citations.OGC);
        properties.put(NAME_KEY,      Constants.SEMI_MAJOR);
        properties.put(ALIAS_KEY,     aliases);
        SEMI_MAJOR = new DefaultParameterDescriptor<Double>(properties, 1, 1, Double.class, valueDomain, null, null);
        /*
         * Change in-place the name and aliases (we do not need to create new objects)
         * before to create the SEMI_MINOR descriptor.
         */
        properties.put(NAME_KEY, Constants.SEMI_MINOR);
        aliases[0] = new NamedIdentifier(Citations.ESRI,    "Semi_Minor");
        aliases[1] = new NamedIdentifier(Citations.NETCDF,  "semi_minor_axis");
        aliases[2] = new NamedIdentifier(Citations.GEOTIFF, "SemiMinor");
        aliases[3] = new NamedIdentifier(Citations.PROJ4,   "b");
        SEMI_MINOR = new DefaultParameterDescriptor<Double>(properties, 1, 1, Double.class, valueDomain, null, null);
        /*
         * SIS-specific parameter for debugging purpose only.
         */
        properties.clear();
        properties.put(AUTHORITY_KEY, Citations.SIS);
        properties.put(NAME_KEY, "eccentricity");
        ECCENTRICITY = new DefaultParameterDescriptor<Double>(properties, 1, 1, Double.class,
                MeasurementRange.create(0d, true, 1d, true, null), null, null);
    }

    /**
     * Constructs a math transform provider from a set of parameters. The provider
     * {@linkplain #getIdentifiers() identifiers} will be the same than the parameter ones.
     *
     * @param parameters The set of parameters (never {@code null}).
     */
    protected MapProjection(final ParameterDescriptorGroup parameters) {
        super(2, 2, parameters);
    }

    /**
     * Returns the operation type for this map projection.
     *
     * @return {@code Projection.class} or a sub-type.
     */
    @Override
    public Class<? extends Projection> getOperationType() {
        return Projection.class;
    }

    /**
     * Validates the given parameter value. This method duplicates the verification already
     * done by {@link org.apache.sis.parameter.DefaultParameterValue#setValue(Object, Unit)}.
     * But we check again because we have no guarantee that the parameters given by the user
     * were instances of {@code DefaultParameterValue}, or that the descriptor associated to
     * the user-specified {@code ParameterValue} has sufficient information.
     *
     * @param  descriptor The descriptor that specify the parameter to validate.
     * @param  value The parameter value in the units given by the descriptor.
     * @throws IllegalArgumentException if the given value is out of bounds.
     *
     * @see #createZeroConstant(ParameterBuilder)
     */
    public static void validate(final ParameterDescriptor<? extends Number> descriptor, final double value)
            throws IllegalArgumentException
    {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalParameterValue_2,
                    descriptor.getName(), value));
        }
        final Comparable<? extends Number> min = descriptor.getMinimumValue();
        final Comparable<? extends Number> max = descriptor.getMaximumValue();
        final double minValue = (min instanceof Number) ? ((Number) min).doubleValue() : Double.NaN;
        final double maxValue = (max instanceof Number) ? ((Number) max).doubleValue() : Double.NaN;
        if (value < minValue || value > maxValue) {
            /*
             * RATIONAL: why we do not check the bounds if (min == max):
             * The only case when our descriptor have (min == max) is when a parameter can only be zero,
             * because of the way the map projection is defined (see e.g. Mercator1SP.LATITUDE_OF_ORIGIN).
             * But in some cases, it would be possible to deal with non-zero values, even if in principle
             * we should not. In such case we let the caller decides.
             *
             * Above check should be revisited if createZeroConstant(ParameterBuilder) is modified.
             */
            if (minValue != maxValue) {   // Compare as 'double' because we want (-0 == +0) to be true.
                throw new IllegalArgumentException(Errors.format(Errors.Keys.ValueOutOfRange_4,
                        descriptor.getName(), min, max, value));
            }
        }
    }

    /**
     * Creates a map projection from the specified group of parameter values.
     *
     * @param  factory The factory to use for creating and concatenating the (de)normalization transforms.
     * @param  parameters The group of parameter values.
     * @return The map projection created from the given parameter values.
     * @throws ParameterNotFoundException if a required parameter was not found.
     * @throws FactoryException if the map projection can not be created.
     */
    @Override
    public final MathTransform createMathTransform(final MathTransformFactory factory, final ParameterValueGroup parameters)
            throws ParameterNotFoundException, FactoryException
    {
        return createProjection(Parameters.castOrWrap(parameters)).createMapProjection(factory);
    }

    /**
     * Creates a map projection on an ellipsoid having a semi-major axis length of 1.
     *
     * @param  parameters The group of parameter values.
     * @return The map projection created from the given parameter values.
     * @throws ParameterNotFoundException if a required parameter was not found.
     */
    protected abstract NormalizedProjection createProjection(final Parameters parameters) throws ParameterNotFoundException;

    /**
     * Notifies {@code DefaultMathTransformFactory} that map projections require
     * values for the {@code "semi_major"} and {@code "semi_minor"} parameters.
     *
     * @return 1, meaning that the operation requires a source ellipsoid.
     */
    @Override
    public final int getEllipsoidsMask() {
        return 1;
    }




    //////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                          ////////
    ////////                       HELPER METHODS FOR SUBCLASSES                      ////////
    ////////                                                                          ////////
    ////////    Following methods are defined for sharing the same GenericName or     ////////
    ////////    Identifier instances when possible.                                   ////////
    //////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the name of the given authority declared in the given parameter descriptor.
     * This method is used only as a way to avoid creating many instances of the same name.
     */
    static GenericName sameNameAs(final Citation authority, final GeneralParameterDescriptor parameters) {
        for (final GenericName candidate : parameters.getAlias()) {
            if (candidate instanceof Identifier && ((Identifier) candidate).getAuthority() == authority) {
                return candidate;
            }
        }
        throw new NoSuchElementException();
    }

    /**
     * Copies all aliases and identifiers except the ones for the given authority.
     * If the given replacement is non-null, then it will be used instead of the
     * first occurrence of the omitted name.
     *
     * <p>This method does not copy the primary name. It is caller's responsibility to add it first.</p>
     *
     * @param  source      The parameter from which to copy the names.
     * @param  except      The authority of the name to omit. Can not be EPSG.
     * @param  replacement The name to use instead of the omitted one, or {@code null} if none.
     * @param  builder     Where to add the names.
     * @return The given {@code builder}, for method call chaining.
     *
     * @since 0.7
     */
    static ParameterBuilder except(final ParameterDescriptor<Double> source, final Citation except,
            GenericName replacement, final ParameterBuilder builder)
    {
        for (GenericName alias : source.getAlias()) {
            if (((Identifier) alias).getAuthority() == except) {
                if (replacement == null) continue;
                alias = replacement;
                replacement = null;
            }
            builder.addName(alias);
        }
        for (final ReferenceIdentifier id : source.getIdentifiers()) {
            builder.addIdentifier(id);
        }
        return builder;
    }

    /**
     * Copies all names except the EPSG one from the given parameter into the builder.
     * The EPSG name is presumed the first name and identifier (this is not verified).
     *
     * @param  source  The parameter from which to copy the names.
     * @param  builder Where to add the names.
     * @return The given {@code builder}, for method call chaining.
     */
    static ParameterBuilder exceptEPSG(final ParameterDescriptor<?> source, final ParameterBuilder builder) {
        for (final GenericName alias : source.getAlias()) {
            builder.addName(alias);
        }
        return builder;
    }

    /**
     * Creates a remarks for parameters that are not formally EPSG parameter.
     *
     * @param  origin The name of the projection for where the parameter is formally used.
     * @return A remarks saying that the parameter is actually defined in {@code origin}.
     */
    static InternationalString notFormalParameter(final String origin) {
        return Messages.formatInternational(Messages.Keys.NotFormalProjectionParameter_1, origin);
    }
}
