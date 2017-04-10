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
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;


/**
 * The provider for <cite>"axis order reversal (2D)"</cite> (EPSG:9843).
 * This is a trivial operation that just swap the two axes.
 * The inverse operation is this operation itself.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
@XmlTransient
public class AxisOrderReversal extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -663548119085488844L;

    /**
     * The group of all parameters expected by this coordinate operation (in this case, none).
     */
    private static final ParameterDescriptorGroup PARAMETERS = builder()
            .addIdentifier("9843").addName("Axis order reversal (2D)").createGroup();

    /**
     * The unique instance, created when first needed.
     */
    private transient MathTransform transform;

    /**
     * Constructs a provider with default parameters.
     */
    public AxisOrderReversal() {
        super(2, 2, PARAMETERS);
    }

    /**
     * For {@link AxisOrderReversal3D} subclass only.
     *
     * @param dimensions  number of dimensions in the source and target CRS of this operation method.
     * @param parameters  description of parameters expected by this operation.
     */
    AxisOrderReversal(final int dimensions, final ParameterDescriptorGroup parameters) {
        super(dimensions, dimensions, parameters);
    }

    /**
     * Returns the operation type.
     *
     * @return interface implemented by all coordinate operations that use this method.
     */
    @Override
    public final Class<Conversion> getOperationType() {
        return Conversion.class;
    }

    /**
     * Returns the transform.
     *
     * @param  factory  ignored (can be null).
     * @param  values   ignored.
     * @return the math transform.
     */
    @Override
    public synchronized MathTransform createMathTransform(MathTransformFactory factory, ParameterValueGroup values) {
        if (transform == null) {
            final MatrixSIS m = Matrices.createZero(getTargetDimensions() + 1, getSourceDimensions() + 1);
            m.setElement(0, 1, 1);
            m.setElement(1, 0, 1);
            transform = MathTransforms.linear(m);
        }
        return transform;
    }
}
