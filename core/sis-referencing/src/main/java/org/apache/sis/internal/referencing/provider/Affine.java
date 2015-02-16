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
import java.util.Collections;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.parameter.TensorParameters;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.operation.transform.MathTransforms;


/**
 * The provider for "<cite>Affine general parametric transformation</cite>" (EPSG:9624).
 * The set of available parameters depends on the matrix size, which is
 * {@value org.geotoolkit.parameter.MatrixParameterDescriptors#DEFAULT_MATRIX_SIZE}&times;{@value
 * org.geotoolkit.parameter.MatrixParameterDescriptors#DEFAULT_MATRIX_SIZE} by default.
 *
 * <table class="sis">
 *   <caption>{@code Affine} parameters</caption>
 *   <tr><th>Parameter name</th><th>Default value</th></tr>
 *   <tr><td>{@code num_row}</td><td>3</td></tr>
 *   <tr><td>{@code num_col}</td><td>3</td></tr>
 *   <tr><td>{@code elt_0_0}</td><td>1</td></tr>
 *   <tr><td>{@code elt_0_1}</td><td>0</td></tr>
 *   <tr><td>{@code elt_0_2}</td><td>0</td></tr>
 *   <tr><td>{@code elt_1_0}</td><td>0</td></tr>
 *   <tr><td>{@code elt_1_1}</td><td>1</td></tr>
 *   <tr><td>{@code elt_1_2}</td><td>0</td></tr>
 *   <tr><td>{@code elt_2_0}</td><td>0</td></tr>
 *   <tr><td>{@code elt_2_1}</td><td>0</td></tr>
 *   <tr><td>{@code elt_2_2}</td><td>1</td></tr>
 * </table>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.5
 * @version 0.6
 * @module
 */
public final class Affine extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 649555815622129472L;

    /**
     * The default matrix size.
     */
    private static final int DEFAULT_MATRIX_SIZE = 3;

    /**
     * The set of predefined providers.
     */
    private static final Affine[] methods = new Affine[8];

    /**
     * The name, aliases and identifiers of the "Affine" method.
     */
    public static final Map<String,?> IDENTIFICATION =
            Collections.singletonMap(NAME_KEY, new NamedIdentifier(Citations.OGC, "Affine"));

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    public static final ParameterDescriptorGroup PARAMETERS;
    static {
        final int[] indices = new int[2]; // The length of this array is the number of tensor dimensions.
        @SuppressWarnings("rawtypes")
        final ParameterDescriptor<?>[] parameters =
                new ParameterDescriptor[indices.length + (DEFAULT_MATRIX_SIZE * DEFAULT_MATRIX_SIZE)];
        int k = 0;
        do {
            parameters[k] = TensorParameters.WKT1.getDimensionDescriptor(k);
        } while (++k < indices.length);
        for (int j=0; j<DEFAULT_MATRIX_SIZE; j++) {
            for (int i=0; i<DEFAULT_MATRIX_SIZE; i++) {
                indices[0] = j;
                indices[1] = i;
                parameters[k++] = TensorParameters.WKT1.getElementDescriptor(indices);
            }
        }
        PARAMETERS = new DefaultParameterDescriptorGroup(IDENTIFICATION, 1, 1, parameters);
    }

    /**
     * Creates a provider for affine transform with a default matrix size.
     */
    public Affine() {
        this(DEFAULT_MATRIX_SIZE - 1, DEFAULT_MATRIX_SIZE - 1);
        methods[DEFAULT_MATRIX_SIZE - 2] = this;
    }

    /**
     * Creates a provider for affine transform with the specified dimensions.
     */
    private Affine(final int sourceDimension, final int targetDimension) {
        super(sourceDimension, targetDimension, PARAMETERS);
    }

    /**
     * Returns the type of operations created by this provider.
     *
     * @return Always {@code Conversion.class} for this provider.
     */
    @Override
    public Class<Conversion> getOperationType() {
        return Conversion.class;
    }

    /**
     * Creates a projective transform from the specified group of parameter values.
     *
     * @param  values The group of parameter values.
     * @return The created math transform.
     * @throws ParameterNotFoundException if a required parameter was not found.
     */
    @Override
    public MathTransform createMathTransform(final ParameterValueGroup values) throws ParameterNotFoundException {
        return MathTransforms.linear(TensorParameters.WKT1.toMatrix(values));
    }

    /**
     * Returns the same operation method, but for different dimensions.
     *
     * @param  sourceDimensions The desired number of input dimensions.
     * @param  targetDimensions The desired number of output dimensions.
     * @return The redimensioned operation method, or {@code this} if no change is needed.
     */
    @Override
    public OperationMethod redimension(final int sourceDimensions, final int targetDimensions) {
        return getProvider(sourceDimensions, targetDimensions);
    }

    /**
     * Returns the operation method for the specified source and target dimensions.
     * This method provides different sourceDimensions for different matrix sizes.
     *
     * @param sourceDimensions The number of source dimensions.
     * @param targetDimensions The number of target dimensions.
     * @return The provider for transforms of the given source and target dimensions.
     */
    public static Affine getProvider(final int sourceDimensions, final int targetDimensions) {
        if (sourceDimensions == targetDimensions) {
            final int i = sourceDimensions - 1;
            if (i >= 0 && i < methods.length) {
                synchronized (Affine.class) {
                    Affine method = methods[i];
                    if (method == null) {
                        methods[i] = method = new Affine(sourceDimensions, targetDimensions);
                    }
                    return method;
                }
            }
        }
        return new Affine(sourceDimensions, targetDimensions);
    }
}
