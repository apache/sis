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

import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import org.apache.sis.referencing.operation.transform.EllipsoidToCentricTransform;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.measure.Units;


/**
 * The base class of operation methods performing an affine operation in geocentric coordinates
 * concatenated with conversion from/to geographic coordinates. This base class is also used for
 * operation methods performing <em>approximation</em> of above, even if they do not really pass
 * through geocentric coordinates.
 *
 * <h2>Default values to verify</h2>
 * This class assumes the following default values.
 * Subclasses should verify if those default values are suitable for them:
 *
 * <ul>
 *   <li>{@link #getOperationType()} defaults to {@link org.opengis.referencing.operation.Transformation}.</li>
 *   <li>{@link #sourceCSType} and {@link #targetCSType} default to {@link EllipsoidalCS}.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
@XmlTransient
public abstract class GeocentricAffineBetweenGeographic extends GeocentricAffine {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -6202315859507526222L;

    /**
     * The operation parameter descriptor for the number of source and target geographic dimensions (2 or 3).
     * This is an OGC-specific parameter for the {@link Molodensky} and {@link AbridgedMolodensky} operations.
     *
     * <p>A default value is needed for avoiding the need to check for null values. This is set to 2 dimensions.
     * Note that the value of this parameter is ignored if the math transform is built with a {@link Context}
     * that specifies the number of source and/or target dimensions.</p>
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> OGC:     </td><td> dim </td></tr>
     * </table>
     * <b>Notes:</b>
     * <ul>
     *   <li>Value domain: [2…3]</li>
     *   <li>Default value: 2</li>
     *   <li>Optional</li>
     * </ul>
     */
    public static final ParameterDescriptor<Integer> DIMENSION;

    /**
     * The operation parameter descriptor for the {@code "src_semi_major"} optional parameter value.
     * Valid values range from 0 to infinity. Units are {@linkplain Units#METRE metres}.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> OGC:     </td><td> src_semi_major </td></tr>
     * </table>
     * <b>Notes:</b>
     * <ul>
     *   <li>Value domain: (0.0 … ∞) m</li>
     *   <li>No default value</li>
     * </ul>
     */
    public static final ParameterDescriptor<Double> SRC_SEMI_MAJOR;

    /**
     * The operation parameter descriptor for the {@code "src_semi_minor"} optional parameter value.
     * Valid values range from 0 to infinity. Units are {@linkplain Units#METRE metres}.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> OGC:     </td><td> src_semi_minor </td></tr>
     * </table>
     * <b>Notes:</b>
     * <ul>
     *   <li>Value domain: (0.0 … ∞) m</li>
     *   <li>No default value</li>
     * </ul>
     */
    public static final ParameterDescriptor<Double> SRC_SEMI_MINOR;

    /**
     * The operation parameter descriptor for the {@code "src_semi_major"} optional parameter value.
     * Valid values range from 0 to infinity. Units are {@linkplain Units#METRE metres}.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> OGC:     </td><td> tgt_semi_major </td></tr>
     * </table>
     * <b>Notes:</b>
     * <ul>
     *   <li>Value domain: (0.0 … ∞) m</li>
     *   <li>No default value</li>
     * </ul>
     */
    public static final ParameterDescriptor<Double> TGT_SEMI_MAJOR;

    /**
     * The operation parameter descriptor for the {@code "src_semi_minor"} optional parameter value.
     * Valid values range from 0 to infinity. Units are {@linkplain Units#METRE metres}.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> OGC:     </td><td> tgt_semi_minor </td></tr>
     * </table>
     * <b>Notes:</b>
     * <ul>
     *   <li>Value domain: (0.0 … ∞) m</li>
     *   <li>No default value</li>
     * </ul>
     */
    public static final ParameterDescriptor<Double> TGT_SEMI_MINOR;

    /*
     * Initializes all static fields.
     */
    static {
        final ParameterBuilder builder = builder().setCodeSpace(Citations.OGC, Constants.OGC);
        SRC_SEMI_MAJOR = builder.addName("src_semi_major").createStrictlyPositive(Double.NaN, Units.METRE);
        SRC_SEMI_MINOR = builder.addName("src_semi_minor").createStrictlyPositive(Double.NaN, Units.METRE);
        TGT_SEMI_MAJOR = builder.addName("tgt_semi_major").createStrictlyPositive(Double.NaN, Units.METRE);
        TGT_SEMI_MINOR = builder.addName("tgt_semi_minor").createStrictlyPositive(Double.NaN, Units.METRE);
        DIMENSION      = builder.addName(Constants.DIM).setRequired(false).createBounded(Integer.class, 2, 3, 2);
    }

    /**
     * Constructs a provider with the specified parameters.
     *
     * @param operationType  the operation type as an enumeration value.
     * @param parameters     description of parameters expected by this operation.
     * @param dimension      number of geographic dimensions: 2 or 3.
     */
    GeocentricAffineBetweenGeographic(Type operationType, ParameterDescriptorGroup parameters, byte dimension) {
        super(operationType, parameters,
              EllipsoidalCS.class, true,
              EllipsoidalCS.class, true,
              dimension);
    }

    /**
     * Creates a source ellipsoid from the given parameter values.
     * The axis lengths are read from the parameters identified by {@link #SRC_SEMI_MAJOR} and {@link #SRC_SEMI_MINOR}.
     * An arbitrary ellipsoid name is used. The returned ellipsoid should be used only for the time needed for building
     * the math transform (because the returned ellipsoid lacks metadata).
     *
     * @param  values   the parameters from which to get the axis lengths and unit.
     * @param  context  the context of parameter values, or {@code null} if none.
     * @return a temporary source ellipsoid to use for creating the math transform.
     * @throws ClassCastException if the unit of measurement of an axis length parameter is not linear.
     */
    public static Ellipsoid getSourceEllipsoid(final Parameters values, final Context context) {
        if (context instanceof DefaultMathTransformFactory.Context) {
            // TODO: move getSourceEllipsoid() in `Context` interface with `Optional` return value.
            Ellipsoid c = ((DefaultMathTransformFactory.Context) context).getSourceEllipsoid();
            if (c != null) return c;
        }
        return getEllipsoid("source", values, SRC_SEMI_MAJOR, SRC_SEMI_MINOR);
    }

    /**
     * Creates a target ellipsoid from the given parameter values.
     * The axis lengths are read from the parameters identified by {@link #TGT_SEMI_MAJOR} and {@link #TGT_SEMI_MINOR}.
     * An arbitrary ellipsoid name is used. The returned ellipsoid should be used only for the time needed for building
     * the math transform (because the returned ellipsoid lacks metadata).
     *
     * @param  values   the parameters from which to get the axis lengths and unit.
     * @param  context  the context of parameter values, or {@code null} if none.
     * @return a temporary target ellipsoid to use for creating the math transform.
     * @throws ClassCastException if the unit of measurement of an axis length parameter is not linear.
     */
    public static Ellipsoid getTargetEllipsoid(final Parameters values, final Context context) {
        if (context instanceof DefaultMathTransformFactory.Context) {
            // TODO: move getTargetEllipsoid() in `Context` interface with `Optional` return value.
            Ellipsoid c = ((DefaultMathTransformFactory.Context) context).getTargetEllipsoid();
            if (c != null) return c;
        }
        return getEllipsoid("target", values, TGT_SEMI_MAJOR, TGT_SEMI_MINOR);
    }

    /**
     * Creates a math transform from the specified group of parameter values.
     * This method wraps the affine operation into Geographic/Geocentric conversions.
     *
     * @param  context  the parameter values together with its context.
     * @return the created math transform.
     * @throws FactoryException if a transform cannot be created.
     */
    @Override
    public MathTransform createMathTransform(final Context context) throws FactoryException {
        final Parameters pv = Parameters.castOrWrap(context.getCompletedParameters());
        final MathTransform affine = super.createMathTransform(context);
        final MathTransformFactory factory = context.getFactory();
        /*
         * Create a "Geographic to Geocentric" conversion with ellipsoid axis length units converted to metres
         * (the unit implied by `SRC_SEMI_MAJOR` and `SRC_SEMI_MINOR`) because metre is the unit of Bursa-Wolf
         * parameters created by above method call.
         */
        MathTransform toGeocentric = EllipsoidToCentricTransform.createGeodeticConversion(factory,
                DefaultEllipsoid.castOrCopy(getSourceEllipsoid(pv, context)).convertTo(Units.METRE),
                context.getSourceDimensions().orElse(minSourceDimension) >= 3,
                EllipsoidToCentricTransform.TargetType.CARTESIAN);
        /*
         * Create a "Geocentric to Geographic" conversion with ellipsoid axis length units converted to metres
         * because this is the unit of the Geocentric CRS used above.
         */
        MathTransform toGeographic = EllipsoidToCentricTransform.createGeodeticConversion(factory,
                DefaultEllipsoid.castOrCopy(getTargetEllipsoid(pv, context)).convertTo(Units.METRE),
                context.getTargetDimensions().orElse(minSourceDimension) >= 3,
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
