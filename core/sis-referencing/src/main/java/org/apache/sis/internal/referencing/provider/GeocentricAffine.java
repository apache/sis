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
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.Transformation;
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
public class GeocentricAffine extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8291967302538661639L;

    /**
     * The operation parameter descriptor for the <cite>X-axis translation</cite>
     * ({@linkplain BursaWolfParameters#tX tX}) parameter value. Valid values range
     * from negative to positive infinity. Units are {@linkplain SI#METRE metres}.
     */
    public static final ParameterDescriptor<Double> TX;

    /**
     * The operation parameter descriptor for the <cite>Y-axis translation</cite>
     * ({@linkplain BursaWolfParameters#tY tY}) parameter value. Valid values range
     * from negative to positive infinity. Units are {@linkplain SI#METRE metres}.
     */
    public static final ParameterDescriptor<Double> TY;

    /**
     * The operation parameter descriptor for the <cite>Z-axis translation</cite>
     * ({@linkplain BursaWolfParameters#tZ tZ}) parameter value. Valid values range
     * from negative to positive infinity. Units are {@linkplain SI#METRE metres}.
     */
    public static final ParameterDescriptor<Double> TZ;

    /**
     * The operation parameter descriptor for the <cite>X-axis rotation</cite>
     * ({@linkplain BursaWolfParameters#rX rX}) parameter value.
     * Units are {@linkplain NonSI#SECOND_ANGLE arc-seconds}.
     */
    public static final ParameterDescriptor<Double> RX;

    /**
     * The operation parameter descriptor for the <cite>Y-axis rotation</cite>
     * ({@linkplain BursaWolfParameters#rY rY}) parameter value.
     * Units are {@linkplain NonSI#SECOND_ANGLE arc-seconds}.
     */
    public static final ParameterDescriptor<Double> RY;

    /**
     * The operation parameter descriptor for the <cite>Z-axis rotation</cite>
     * ({@linkplain BursaWolfParameters#rZ rZ}) parameter value.
     * Units are {@linkplain NonSI#SECOND_ANGLE arc-seconds}.
     */
    public static final ParameterDescriptor<Double> RZ;

    /**
     * The operation parameter descriptor for the <cite>Scale difference</cite>
     * ({@linkplain BursaWolfParameters#dS dS}) parameter value.
     * Valid values range from negative to positive infinity.
     * Units are {@linkplain Units#PPM parts per million}.
     */
    public static final ParameterDescriptor<Double> DS;

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
     * Creates a math transform from the specified group of parameter values.
     *
     * @param  factory Ignored (can be null).
     * @param  values The group of parameter values.
     * @return The created math transform.
     */
    @Override
    public final MathTransform createMathTransform(final MathTransformFactory factory, final ParameterValueGroup values) {
        final BursaWolfParameters parameters = new BursaWolfParameters(null, null);
        fill(parameters, Parameters.castOrWrap(values));
        return MathTransforms.linear(parameters.getPositionVectorTransformation(null));
    }

    /**
     * Fills the given Bursa-Wolf parameters with the specified values.
     * This method is invoked automatically by {@link #createMathTransform}.
     *
     * @param parameters The Bursa-Wold parameters to set.
     * @param values The parameter values to read. Those parameters will not be modified.
     */
    void fill(final BursaWolfParameters parameters, final Parameters values) {
        parameters.tX = values.doubleValue(TX);
        parameters.tY = values.doubleValue(TY);
        parameters.tZ = values.doubleValue(TZ);
        parameters.rX = values.doubleValue(RX);
        parameters.rY = values.doubleValue(RY);
        parameters.rZ = values.doubleValue(RZ);
        parameters.dS = values.doubleValue(DS);
    }
}
