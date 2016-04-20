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
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.transform.MathTransforms;


/**
 * The provider for <cite>"Vertical Offset"</cite> (EPSG:9616).
 * The Apache SIS implementation of this operation method always perform the vertical offset in metres.
 * The vertical axis of source and target CRS shall be converted to metres before this operation is applied.
 *
 * <div class="note">Axis direction</div>
 * The EPSG guidance note defines this operation as (ignoring unit conversions):
 *
 * <blockquote>X₂ = m⋅X₁ + offset</blockquote>
 *
 * where <var>m</var> is +1 if source and target axes have the same direction, or -1 if they have opposite direction.
 * Consequently the <var>offset</var> value is always applied in the direction of the target axis. This is different
 * than the Apache SIS design, which always interpret the parameter in the direction of a normalized coordinate axis
 * (up in this case, regardless the source and target coordinate systems). Consequently the sign of the
 * <cite>"Vertical Offset"</cite> parameter value needs to be reversed if the target coordinate system axis is down.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@XmlTransient
public final class VerticalOffset extends GeographicOffsets {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -8309224700931038020L;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        PARAMETERS = builder().addIdentifier("9616").addName("Vertical Offset").createGroup(TZ);
    }

    /**
     * Constructs a provider with default parameters.
     */
    public VerticalOffset() {
        super(1, 1, PARAMETERS, null);
    }

    /**
     * Creates a transform from the specified group of parameter values.
     * The parameter value is unconditionally converted to metres.
     *
     * @param  factory Ignored (can be null).
     * @param  values The group of parameter values.
     * @return The created math transform.
     * @throws ParameterNotFoundException if a required parameter was not found.
     */
    @Override
    public MathTransform createMathTransform(final MathTransformFactory factory, final ParameterValueGroup values)
            throws ParameterNotFoundException
    {
        final Parameters pv = Parameters.castOrWrap(values);
        final Matrix2 t = new Matrix2();
        t.m01 = pv.doubleValue(TZ);
        return MathTransforms.linear(t);
    }

    /**
     * Invoked by {@link org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory} after
     * the transform has been created but before it is concatenated with operations performing axis changes.
     * This method performs the parameter sign adjustment as documented in the class javadoc if and only if
     * this method detects that the target axis is oriented toward down. This orientation is detected by a
     * negative sign for the <var>m₀₀</var> coefficient in the given 2×2 affine transform matrix.
     *
     * <div class="note"><b>Implementation note:</b>
     * for now we define this method as a static one because it is the only special case handled by
     * {@code DefaultMathTransformFactory}. But if there is more special cases in a future SIS version,
     * then we should make this method non-static and declare an overrideable {@code postCreate} method
     * in {@link AbstractProvider} instead.</div>
     *
     * @param  parameterized The transform created by {@code createMathTransform(…)}.
     * @param  after The matrix for the operation to be concatenated after {@code parameterized}.
     * @return The transform to use instead of {@code parameterized}.
     * @throws FactoryException if an error occurred while creating the new transform.
     */
    public static MathTransform postCreate(MathTransform parameterized, final Matrix after) throws FactoryException {
        if (after.getElement(0,0) < 0) try {
            parameterized = parameterized.inverse();
        } catch (NoninvertibleTransformException e) {
            throw new FactoryException(e);                  // Should never happe since matrix element is not zero.
        }
        return parameterized;
    }
}
