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
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.parameter.TensorParameters;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.operation.transform.MathTransforms;


/**
 * The provider for "<cite>Affine general parametric transformation</cite>" (EPSG:9624).
 * The set of available parameters depends on the matrix size, which is 3×3 by default.
 *
 * <table class="sis">
 *   <caption>{@code Affine} parameters</caption>
 *   <tr><th>EPSG code</th><th>EPSG name</th><th>OGC name</th><th>Default value</th></tr>
 *   <tr><td>    </td> <td>          </td> <td>{@code num_row}</td> <td>3</td></tr>
 *   <tr><td>    </td> <td>          </td> <td>{@code num_col}</td> <td>3</td></tr>
 *   <tr><td>8623</td> <td>{@code A0}</td> <td>{@code elt_0_0}</td> <td>1</td></tr>
 *   <tr><td>8624</td> <td>{@code A1}</td> <td>{@code elt_0_1}</td> <td>0</td></tr>
 *   <tr><td>8625</td> <td>{@code A2}</td> <td>{@code elt_0_2}</td> <td>0</td></tr>
 *   <tr><td>8639</td> <td>{@code B0}</td> <td>{@code elt_1_0}</td> <td>0</td></tr>
 *   <tr><td>8640</td> <td>{@code B1}</td> <td>{@code elt_1_1}</td> <td>1</td></tr>
 *   <tr><td>8641</td> <td>{@code B2}</td> <td>{@code elt_1_2}</td> <td>0</td></tr>
 *   <tr><td>    </td> <td>          </td> <td>{@code elt_2_0}</td> <td>0</td></tr>
 *   <tr><td>    </td> <td>          </td> <td>{@code elt_2_1}</td> <td>0</td></tr>
 *   <tr><td>    </td> <td>          </td> <td>{@code elt_2_2}</td> <td>1</td></tr>
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
     * The operation method name as defined in the EPSG database.
     * Must matches exactly the EPSG name (this will be verified by JUnit tests).
     *
     * <p>Note: in contrast, the name used by OGC is just "Affine".</p>
     *
     * @see org.apache.sis.internal.util.Constants#AFFINE
     */
    public static final String NAME = "Affine general parametric transformation";

    /**
     * The number of dimensions used by the EPSG:9624 definition. This will be used as the
     * default number of dimensions. Operation methods of other dimensions, where we have
     * no EPSG definition, shall use the Well Known Text (WKT) parameter names.
     */
    public static final int EPSG_DIMENSION = 2;

    /**
     * The maximal number of dimensions to be cached. Descriptors having more than
     * this amount of dimensions will be recreated every time they are requested.
     */
    private static final int MAX_CACHED_DIMENSION = 6;

    /**
     * Cached providers for methods of dimension 1×1 to {@link #MAX_CACHED_DIMENSION}.
     * The index of each element is computed by {@link #cacheIndex(int, int)}.
     * All usages of this array shall be synchronized on {@code cached}.
     */
    private static final Affine[] cached = new Affine[MAX_CACHED_DIMENSION * MAX_CACHED_DIMENSION];

    /**
     * Returns the index where to store a method of the given dimensions in the {@link #cached} array,
     * or -1 if it should not be cached.
     */
    static int cacheIndex(int sourceDimensions, int targetDimensions) {
        if (--sourceDimensions >= 0 && sourceDimensions < MAX_CACHED_DIMENSION &&
            --targetDimensions >= 0 && targetDimensions < MAX_CACHED_DIMENSION)
        {
            return sourceDimensions * MAX_CACHED_DIMENSION + targetDimensions;
        }
        return -1;
    }

    /**
     * Index equivalent to {@link cacheIndex(EPSG_DIMENSION, EPSG_DIMENSION)}.
     * We expand the computation inline for allowing the compiler to replace the whole
     * expression by a single constant.
     */
    static final int EPSG_INDEX = (EPSG_DIMENSION - 1) * MAX_CACHED_DIMENSION + (EPSG_DIMENSION - 1);

    /**
     * The name, aliases and identifiers of the "Affine" method.
     */
    private static final Map<String,?> IDENTIFICATION =
            Collections.singletonMap(NAME_KEY, new NamedIdentifier(Citations.OGC, "Affine"));

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final int[] indices = new int[2]; // The length of this array is the number of tensor dimensions.
        @SuppressWarnings("rawtypes")
        final ParameterDescriptor<?>[] parameters =
                new ParameterDescriptor[indices.length + (EPSG_DIMENSION +1) * (EPSG_DIMENSION + 1)];
        int k = 0;
        do {
            parameters[k] = TensorParameters.WKT1.getDimensionDescriptor(k);
        } while (++k < indices.length);
        for (int j=0; j <= EPSG_DIMENSION; j++) {
            for (int i=0; i <= EPSG_DIMENSION; i++) {
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
        super(EPSG_DIMENSION, EPSG_DIMENSION, PARAMETERS);
        synchronized (cached) {
            cached[EPSG_INDEX] = this;
        }
    }

    /**
     * Creates a provider for affine transform with the specified dimensions.
     */
    private Affine(final int sourceDimensions, final int targetDimensions) {
        super(sourceDimensions, targetDimensions, PARAMETERS);
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
        /*
         * The TensorParameters constant used below (WKT1 or EPSG) does not matter,
         * since both of them understand the names of the other TensorParameters.
         */
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
     * This method provides different {@code Affine} instances for different dimensions.
     *
     * @param sourceDimensions The number of source dimensions.
     * @param targetDimensions The number of target dimensions.
     * @return The provider for transforms of the given source and target dimensions.
     */
    private static Affine getProvider(final int sourceDimensions, final int targetDimensions) {
        final int i = cacheIndex(sourceDimensions, targetDimensions);
        if (i >= 0) {
            final Affine method;
            synchronized (Affine.class) {
                method = cached[i];
            }
            if (method != null) {
                return method;
            }
        }
        /*
         * At this point, no existing instance has been found in the cache.
         * Create a new instance and cache it if its dimension is not too large.
         */
        final Affine method = new Affine(sourceDimensions, targetDimensions);
        if (i >= 0) {
            synchronized (Affine.class) {
                final Affine other = cached[i];     // May have been created in another thread.
                if (other != null) {
                    return other;
                }
                cached[i] = method;
            }
        }
        return method;
    }

    /**
     * Returns the parameter descriptor for the given dimensions.
     *
     * @param sourceDimensions The number of source dimensions.
     * @param targetDimensions The number of target dimensions.
     * @return The parameters descriptor for the given dimensions.
     */
    public static ParameterDescriptorGroup descriptor(final int sourceDimensions, final int targetDimensions) {
        return getProvider(sourceDimensions, targetDimensions).getParameters();
    }

    /**
     * Returns the parameter values for the given matrix.
     *
     * @param  matrix The matrix for which to get parameter values.
     * @return The parameters of the given matrix.
     */
    public static ParameterValueGroup parameters(final Matrix matrix) {
        return TensorParameters.WKT1.createValueGroup(IdentifiedObjects.getProperties(
                descriptor(matrix.getNumRow() - 1, matrix.getNumCol() - 1)), matrix);
    }
}
