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

import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.Transformation;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.measure.Units;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.datum.BursaWolfParameters;
import org.apache.sis.referencing.operation.transform.MathTransforms;


/**
 * The base class of operation methods performing a translation, rotation and/or scale in geocentric coordinates.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@XmlTransient
public abstract class GeocentricAffine extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8291967302538661639L;

    /**
     * The operation parameter descriptor for the <cite>X-axis translation</cite>
     * ({@linkplain BursaWolfParameters#tX tX}) parameter value. Valid values range
     * from negative to positive infinity. Units are {@linkplain SI#METRE metres}.
     */
    static final ParameterDescriptor<Double> TX;

    /**
     * The operation parameter descriptor for the <cite>Y-axis translation</cite>
     * ({@linkplain BursaWolfParameters#tY tY}) parameter value. Valid values range
     * from negative to positive infinity. Units are {@linkplain SI#METRE metres}.
     */
    static final ParameterDescriptor<Double> TY;

    /**
     * The operation parameter descriptor for the <cite>Z-axis translation</cite>
     * ({@linkplain BursaWolfParameters#tZ tZ}) parameter value. Valid values range
     * from negative to positive infinity. Units are {@linkplain SI#METRE metres}.
     */
    static final ParameterDescriptor<Double> TZ;

    /**
     * The operation parameter descriptor for the <cite>X-axis rotation</cite>
     * ({@linkplain BursaWolfParameters#rX rX}) parameter value.
     * Units are {@linkplain NonSI#SECOND_ANGLE arc-seconds}.
     */
    static final ParameterDescriptor<Double> RX;

    /**
     * The operation parameter descriptor for the <cite>Y-axis rotation</cite>
     * ({@linkplain BursaWolfParameters#rY rY}) parameter value.
     * Units are {@linkplain NonSI#SECOND_ANGLE arc-seconds}.
     */
    static final ParameterDescriptor<Double> RY;

    /**
     * The operation parameter descriptor for the <cite>Z-axis rotation</cite>
     * ({@linkplain BursaWolfParameters#rZ rZ}) parameter value.
     * Units are {@linkplain NonSI#SECOND_ANGLE arc-seconds}.
     */
    static final ParameterDescriptor<Double> RZ;

    /**
     * The operation parameter descriptor for the <cite>Scale difference</cite>
     * ({@linkplain BursaWolfParameters#dS dS}) parameter value.
     * Valid values range from negative to positive infinity.
     * Units are {@linkplain Units#PPM parts per million}.
     */
    static final ParameterDescriptor<Double> DS;
    static {
        final ParameterBuilder builder = builder();
        TX = createShift(builder.addName("X-axis translation").addName(Citations.OGC, "dx"));
        TY = createShift(builder.addName("Y-axis translation").addName(Citations.OGC, "dy"));
        TZ = createShift(builder.addName("Z-axis translation").addName(Citations.OGC, "dz"));
        RX = createRotation(builder, "X-axis rotation", "ex");
        RY = createRotation(builder, "Y-axis rotation", "ey");
        RZ = createRotation(builder, "Z-axis rotation", "ez");
        DS = builder.addName("Scale difference").addName(Citations.OGC, "ppm").create(1, Units.PPM);
    }

    /**
     * Convenience method for building the rotation parameters.
     */
    private static ParameterDescriptor<Double> createRotation(final ParameterBuilder builder, final String name, final String alias) {
        return builder.addName(name).addName(Citations.OGC, alias).createBounded(-180*60*60, 180*60*60, 0, NonSI.SECOND_ANGLE);
    }

    /**
     * Return value for {@link #getType()}.
     */
    static final int TRANSLATION=1, SEVEN_PARAM=2, FRAME_ROTATION=3;

    /**
     * Constructs a provider with the specified parameters.
     * This constructor is for subclass constructors only.
     */
    GeocentricAffine(final int dimension, final ParameterDescriptorGroup parameters) {
        super(dimension, dimension, parameters);
    }

    /**
     * Returns the operation type.
     *
     * @return Interface implemented by all coordinate operations that use this method.
     */
    @Override
    public final Class<Transformation> getOperationType() {
        return Transformation.class;
    }

    /**
     * Returns the operation type as one of {@link #TRANSLATION}, {@link #SEVEN_PARAM} or
     * {@link #FRAME_ROTATION} constants.
     */
    abstract int getType();

    /**
     * Creates a math transform from the specified group of parameter values.
     *
     * @param  factory Ignored (can be null).
     * @param  values The group of parameter values.
     * @return The created math transform.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public final MathTransform createMathTransform(final MathTransformFactory factory, final ParameterValueGroup values) {
        final BursaWolfParameters parameters = new BursaWolfParameters(null, null);
        final Parameters pv = Parameters.castOrWrap(values);
        switch (getType()) {
            default:             throw new AssertionError();
            case FRAME_ROTATION: parameters.reverseRotation();         // Fall through
            case SEVEN_PARAM:    parameters.rX = pv.doubleValue(RX);
                                 parameters.rY = pv.doubleValue(RY);
                                 parameters.rZ = pv.doubleValue(RZ);
                                 parameters.dS = pv.doubleValue(DS);
            case TRANSLATION:    parameters.tX = pv.doubleValue(TX);   // Fall through
                                 parameters.tY = pv.doubleValue(TY);
                                 parameters.tZ = pv.doubleValue(TZ);
        }
        return MathTransforms.linear(parameters.getPositionVectorTransformation(null));
    }

    /**
     * Given a matrix for an affine transform operation, finds the equivalent Bursa-Wolf parameters.
     * It is user-responsibility to ensure that the operation is performed in the geocentric space.
     *
     * @param  matrix The matrix for the affine transform in the geocentric domain.
     * @return The Bursa-Wolf parameter values for the given matrix.
     * @throws IllegalArgumentException if the given matrix can not be decomposed in Bursa-Wolf parameters.
     */
    public static ParameterValueGroup getParameters(final Matrix matrix) throws IllegalArgumentException {
        final BursaWolfParameters parameters = new BursaWolfParameters(null, null);
        /*
         * We use a 0.01 metre tolerance (Formulas.LINEAR_TOLERANCE) based on the knowledge that the
         * translation terms are in metres and the rotation terms have the some order of magnitude.
         */
        parameters.setPositionVectorTransformation(matrix, Formulas.LINEAR_TOLERANCE);
        final boolean isTranslation = parameters.isTranslation();
        final Parameters values = Parameters.castOrWrap(
                (isTranslation ? GeocentricTranslation.PARAMETERS : PositionVector7Param.PARAMETERS).createValue());
        values.getOrCreate(TX).setValue(parameters.tX);
        values.getOrCreate(TY).setValue(parameters.tY);
        values.getOrCreate(TZ).setValue(parameters.tZ);
        if (!isTranslation) {
            values.getOrCreate(RX).setValue(parameters.rX);
            values.getOrCreate(RY).setValue(parameters.rY);
            values.getOrCreate(RZ).setValue(parameters.rZ);
            values.getOrCreate(DS).setValue(parameters.dS);
        }
        return values;
    }
}
