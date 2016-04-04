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
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.referencing.operation.transform.EllipsoidToCentricTransform;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.ArgumentChecks;


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
     * but Apache SIS uses it also for internal parameters of Geographic/Geocentric.
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
    public static final ParameterDescriptor<Double> SRC_SEMI_MAJOR;

    /**
     * The operation parameter descriptor for the {@code "src_semi_minor"} optional parameter value.
     * Valid values range from 0 to infinity. Units are {@linkplain SI#METRE metres}.
     */
    public static final ParameterDescriptor<Double> SRC_SEMI_MINOR;

    /**
     * The operation parameter descriptor for the {@code "src_semi_major"} optional parameter value.
     * Valid values range from 0 to infinity. Units are {@linkplain SI#METRE metres}.
     */
    public static final ParameterDescriptor<Double> TGT_SEMI_MAJOR;

    /**
     * The operation parameter descriptor for the {@code "src_semi_minor"} optional parameter value.
     * Valid values range from 0 to infinity. Units are {@linkplain SI#METRE metres}.
     */
    public static final ParameterDescriptor<Double> TGT_SEMI_MINOR;

    static {
        final ParameterBuilder builder = builder().setCodeSpace(Citations.OGC, Constants.OGC);
        SRC_SEMI_MAJOR = builder.addName("src_semi_major").createStrictlyPositive(Double.NaN, SI.METRE);
        SRC_SEMI_MINOR = builder.addName("src_semi_minor").createStrictlyPositive(Double.NaN, SI.METRE);
        TGT_SEMI_MAJOR = builder.addName("tgt_semi_major").createStrictlyPositive(Double.NaN, SI.METRE);
        TGT_SEMI_MINOR = builder.addName("tgt_semi_minor").createStrictlyPositive(Double.NaN, SI.METRE);
        DIMENSION      = builder.addName("dim").setRequired(false).createBounded(Integer.class, 2, 3, null);
    }

    /**
     * The providers for all combinations between 2D and 3D cases, or {@code null} if none.
     * If non-null, then array length shall be 4. Indices are built with following rules:
     *
     * <ul>
     *   <li>Bit 1: dimension of source coordinates (0 for 2D, 1 for 3D).</li>
     *   <li>Bit 0: dimension of target coordinates (0 for 2D, 1 for 3D).</li>
     * </ul>
     *
     * <strong>Do not modify this array after construction</strong>, since the same array is shared by many
     * objects and there is no synchronization.
     */
    final GeocentricAffineBetweenGeographic[] redimensioned;

    /**
     * Constructs a provider with the specified parameters.
     *
     * @param sourceDimensions Number of dimensions in the source CRS of this operation method.
     * @param targetDimensions Number of dimensions in the target CRS of this operation method.
     * @param parameters       Description of parameters expected by this operation.
     * @param redimensioned    The providers for all combinations between 2D and 3D cases, or {@code null}.
     */
    GeocentricAffineBetweenGeographic(int sourceDimensions, int targetDimensions, ParameterDescriptorGroup parameters,
            final GeocentricAffineBetweenGeographic[] redimensioned)
    {
        super(sourceDimensions, targetDimensions, parameters);
        this.redimensioned = redimensioned;
    }

    /**
     * Returns the same operation method, but for different number of dimensions.
     *
     * @param  sourceDimensions The desired number of input dimensions.
     * @param  targetDimensions The desired number of output dimensions.
     * @return The redimensioned operation method, or {@code this} if no change is needed.
     */
    @Override
    public final OperationMethod redimension(final int sourceDimensions, final int targetDimensions) {
        if (redimensioned != null) {
            ArgumentChecks.ensureBetween("sourceDimensions", 2, 3, sourceDimensions);
            ArgumentChecks.ensureBetween("targetDimensions", 2, 3, targetDimensions);
            final OperationMethod m = redimensioned[((sourceDimensions & 1) << 1) | (targetDimensions & 1)];
            if (m != null) {
                return m;
            }
        }
        return super.redimension(sourceDimensions, targetDimensions);
    }

    /**
     * Notifies {@code DefaultMathTransformFactory} that map projections require values for the
     * {@code "src_semi_major"}, {@code "src_semi_minor"} , {@code "tgt_semi_major"} and
     * {@code "tgt_semi_minor"} parameters.
     *
     * @return 3, meaning that the operation requires source and target ellipsoids.
     */
    @Override
    public final int getEllipsoidsMask() {
        return 3;
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
        /*
         * Create a "Geographic to Geocentric" conversion with ellipsoid axis length units converted to metres
         * (the unit implied by SRC_SEMI_MAJOR) because it is the unit of Bursa-Wolf parameters that we created above.
         */
        MathTransform toGeocentric = EllipsoidToCentricTransform.createGeodeticConversion(factory,
                pv.doubleValue(SRC_SEMI_MAJOR),
                pv.doubleValue(SRC_SEMI_MINOR),
                SI.METRE, getSourceDimensions() >= 3,
                EllipsoidToCentricTransform.TargetType.CARTESIAN);
        /*
         * Create a "Geocentric to Geographic" conversion with ellipsoid axis length units converted to metres
         * because this is the unit of the Geocentric CRS used above.
         */
        MathTransform toGeographic = EllipsoidToCentricTransform.createGeodeticConversion(factory,
                pv.doubleValue(TGT_SEMI_MAJOR),
                pv.doubleValue(TGT_SEMI_MINOR),
                SI.METRE, getTargetDimensions() >= 3,
                EllipsoidToCentricTransform.TargetType.CARTESIAN);
        try {
            toGeographic = toGeographic.inverse();
        } catch (NoninvertibleTransformException e) {
            throw new FactoryException(e);                  // Should never happen with SIS implementation.
        }
        /*
         * The  Geocentric → Affine → Geographic  chain.
         */
        return factory.createConcatenatedTransform(toGeocentric,
               factory.createConcatenatedTransform(affine, toGeographic));
    }
}
