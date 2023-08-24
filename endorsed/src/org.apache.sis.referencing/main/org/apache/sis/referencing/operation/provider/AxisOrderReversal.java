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
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;


/**
 * The provider for <cite>"axis order reversal (2D)"</cite> (EPSG:9843).
 * This is a trivial operation that just swap the two first axes.
 * The inverse operation is this operation itself.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.8
 */
@XmlTransient
public class AxisOrderReversal extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7027181359241386097L;

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
     * The matrix size, which is the number of dimensions plus one.
     */
    private final int size;

    /**
     * Constructs a provider with default parameters.
     */
    public AxisOrderReversal() {
        this(PARAMETERS, 3);
    }

    /**
     * For {@link AxisOrderReversal3D} subclass only.
     *
     * @param parameters  description of parameters expected by this operation.
     * @param size  the matrix size, which is the number of dimensions plus one.
     */
    AxisOrderReversal(final ParameterDescriptorGroup parameters, final int size) {
        super(Conversion.class, parameters,
              CoordinateSystem.class, false,
              CoordinateSystem.class, false);
        this.size = size;
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
            final MatrixSIS m = Matrices.createZero(size, size);
            m.setElement(0, 1, 1);
            m.setElement(1, 0, 1);
            for (int i=2; i<size; i++) {
                m.setElement(i, i, 1);
            }
            transform = MathTransforms.linear(m);
        }
        return transform;
    }
}
