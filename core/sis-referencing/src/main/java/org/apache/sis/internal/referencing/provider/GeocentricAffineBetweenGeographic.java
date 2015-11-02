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

import javax.xml.bind.annotation.XmlTransient;
import javax.measure.unit.SI;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.resources.Errors;


/**
 * The base class of operation methods performing an affine operation in geocentric coordinates
 * concatenated with conversion from/to geographic coordinates. This base class is also used for
 * operation methods performing <em>approximation</em> of above, even if they do not really pass
 * through geocentric coordinates.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@XmlTransient
public abstract class GeocentricAffineBetweenGeographic extends GeocentricAffine {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -6202315859507526222L;

    /**
     * The operation parameter descriptor for the number of source and target geographic dimensions (2 or 3).
     * This is an OGC-specific parameter for the {@link Molodensky} and {@link AbridgedMolodensky} operations,
     * but Apache SIS uses it also for Geographic/Geocentric conversions.
     *
     * <p>We do not provide default value for this parameter (neither we do for other OGC-specific parameters
     * in this class) because this parameter is used with both two- and three-dimensional operation methods.
     * If we want to provide a default value, we could but it would complicate a little bit the code since we
     * could no longer reuse the same {@code PARAMETERS} constant for operation methods of any number of dimensions.
     * Furthermore it would not solve the case where the number of input dimensions is different than the number of
     * output dimensions. We can not afford to have wrong default values since it would confuse our interpretation
     * of user's parameters in {@link #createMathTransform(MathTransformFactory, ParameterValueGroup)}.</p>
     */
    public static final ParameterDescriptor<Integer> DIMENSION;

    /**
     * The operation parameter descriptor for the {@code "src_semi_major"} optional parameter value.
     * Valid values range from 0 to infinity. Units are {@linkplain SI#METRE metres}.
     */
    static final ParameterDescriptor<Double> SRC_SEMI_MAJOR;

    /**
     * The operation parameter descriptor for the {@code "src_semi_minor"} optional parameter value.
     * Valid values range from 0 to infinity. Units are {@linkplain SI#METRE metres}.
     */
    static final ParameterDescriptor<Double> SRC_SEMI_MINOR;

    /**
     * The operation parameter descriptor for the {@code "src_semi_major"} optional parameter value.
     * Valid values range from 0 to infinity. Units are {@linkplain SI#METRE metres}.
     */
    static final ParameterDescriptor<Double> TGT_SEMI_MAJOR;

    /**
     * The operation parameter descriptor for the {@code "src_semi_minor"} optional parameter value.
     * Valid values range from 0 to infinity. Units are {@linkplain SI#METRE metres}.
     */
    static final ParameterDescriptor<Double> TGT_SEMI_MINOR;

    static {
        final ParameterBuilder builder = builder().setCodeSpace(Citations.OGC, Constants.OGC).setRequired(false);
        DIMENSION      = builder.addName("dim")   .createBounded(Integer.class, 2, 3, null);
        SRC_SEMI_MAJOR = builder.addName("src_semi_major").createStrictlyPositive(Double.NaN, SI.METRE);
        SRC_SEMI_MINOR = builder.addName("src_semi_minor").createStrictlyPositive(Double.NaN, SI.METRE);
        TGT_SEMI_MAJOR = builder.addName("tgt_semi_major").createStrictlyPositive(Double.NaN, SI.METRE);
        TGT_SEMI_MINOR = builder.addName("tgt_semi_minor").createStrictlyPositive(Double.NaN, SI.METRE);
    }

    /**
     * Constructs a provider with the specified parameters.
     *
     * @param sourceDimensions Number of dimensions in the source CRS of this operation method.
     * @param targetDimensions Number of dimensions in the target CRS of this operation method.
     */
    GeocentricAffineBetweenGeographic(int sourceDimensions, int targetDimensions, ParameterDescriptorGroup parameters) {
        super(sourceDimensions, targetDimensions, parameters);
    }

    /**
     * Returns the number of dimensions declared in the given parameter group, or 0 if none.
     * If this method returns a non-zero value, then it is guaranteed to be either 2 or 3.
     *
     * @param  values The values from which to get the dimension.
     * @return The dimension, or 0 if none.
     * @throws InvalidParameterValueException if the dimension parameter has an invalid value.
     */
    static int getDimension(final Parameters values) throws InvalidParameterValueException {
        final Integer value = values.getValue(DIMENSION);
        if (value == null) {
            return 0;
        }
        final int dimension = value;  // Unboxing.
        if (dimension != 2 && dimension != 3) {
            throw new InvalidParameterValueException(Errors.format(
                    Errors.Keys.IllegalArgumentValue_2, "dim", value), "dim", value);
        }
        return dimension;
    }

    /**
     * Creates a math transform from the specified group of parameter values.
     * This method wraps the affine operation into Geographic/Geocentric conversions.
     *
     * @param  factory The factory to use for creating concatenated transforms.
     * @param  values The group of parameter values.
     * @return The created math transform.
     * @throws FactoryException if a transform can not be created.
     */
    @Override
    public MathTransform createMathTransform(final MathTransformFactory factory, final ParameterValueGroup values)
            throws FactoryException
    {
        final Parameters pv = Parameters.castOrWrap(values);
        final MathTransform affine = super.createMathTransform(factory, pv);
        final int dimension = getDimension(pv);
        /*
         * Create a "Geographic to Geocentric" conversion with ellipsoid axis length units converted to metres
         * (the unit implied by SRC_SEMI_MAJOR) because it is the unit of Bursa-Wolf param. that we created above.
         */
        Parameters step = Parameters.castOrWrap(factory.getDefaultParameters(GeographicToGeocentric.NAME));
        step.getOrCreate(MapProjection.SEMI_MAJOR).setValue(pv.doubleValue(SRC_SEMI_MAJOR));
        step.getOrCreate(MapProjection.SEMI_MINOR).setValue(pv.doubleValue(SRC_SEMI_MINOR));
        step.getOrCreate(DIMENSION).setValue(dimension != 0 ? dimension : getSourceDimensions());
        final MathTransform toGeocentric = factory.createParameterizedTransform(step);
        /*
         * Create a "Geocentric to Geographic" conversion with ellipsoid axis length units converted to metres
         * because this is the unit of the Geocentric CRS used above.
         */
        step = Parameters.castOrWrap(factory.getDefaultParameters(GeocentricToGeographic.NAME));
        step.getOrCreate(MapProjection.SEMI_MAJOR).setValue(pv.doubleValue(TGT_SEMI_MAJOR));
        step.getOrCreate(MapProjection.SEMI_MINOR).setValue(pv.doubleValue(TGT_SEMI_MINOR));
        step.getOrCreate(DIMENSION).setValue(dimension != 0 ? dimension : getTargetDimensions());
        final MathTransform toGeographic = factory.createParameterizedTransform(step);
        /*
         * The  Geocentric → Affine → Geographic  chain.
         */
        return factory.createConcatenatedTransform(toGeocentric,
               factory.createConcatenatedTransform(affine, toGeographic));
    }
}
