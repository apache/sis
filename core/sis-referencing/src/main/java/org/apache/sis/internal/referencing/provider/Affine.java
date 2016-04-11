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
import java.util.Arrays;
import java.util.Collections;
import javax.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.parameter.TensorParameters;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.MathTransforms;


/**
 * The provider for <cite>"Affine parametric transformation"</cite> (EPSG:9624).
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
@XmlTransient
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
    public static final String NAME = "Affine parametric transformation";

    /**
     * The EPSG:9624 compliant instance, created when first needed.
     */
    private static volatile Affine EPSG_METHOD;

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
     * A map containing identification properties for creating {@code OperationMethod} or
     * {@code ParameterDescriptorGroup} instances.
     */
    private static final Map<String,?> IDENTIFICATION_EPSG, IDENTIFICATION_OGC;
    static {
        final NamedIdentifier nameOGC = new NamedIdentifier(Citations.OGC, Constants.OGC, Constants.AFFINE, null, null);
        IDENTIFICATION_OGC = Collections.singletonMap(NAME_KEY, nameOGC);
        IDENTIFICATION_EPSG = EPSGName.properties(9624, NAME, nameOGC);
    }

    /**
     * Creates a provider for affine transform with a default matrix size (standard EPSG:9624 instance).
     * This constructor is public for the needs of {@link java.util.ServiceLoader} — do not invoke explicitely.
     * If an instance of {@code Affine()} is desired, invoke {@code getProvider(EPSG_DIMENSION, EPSG_DIMENSION)}
     * instead.
     *
     * @see org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public Affine() {
        super(IDENTIFICATION_EPSG, EPSG_DIMENSION, EPSG_DIMENSION, new Descriptor(IDENTIFICATION_EPSG,
                Arrays.copyOfRange( // Discards param 0 and 1, take only the ones in index range [2…7].
                        TensorParameters.ALPHANUM.getAllDescriptors(EPSG_DIMENSION, EPSG_DIMENSION + 1), 2, 8)));
        /*
         * Do caching ourselves because this constructor is usually not invoked by getProvider(int, int).
         * It is usually invoked when DefaultMathTransformFactory scans the classpath with a ServiceLoader.
         * This normally happen only once, so this instance is probably the unique instance to keep in the JVM.
         */
        EPSG_METHOD = this;
    }

    /**
     * Creates a provider for affine transform with the specified dimensions.
     * This is created when first needed by {@link #getProvider(int, int)}.
     *
     * @see #getProvider(int, int)
     */
    private Affine(final int sourceDimensions, final int targetDimensions) {
        super(IDENTIFICATION_OGC, sourceDimensions, targetDimensions, new Descriptor(IDENTIFICATION_OGC,
                TensorParameters.WKT1.getAllDescriptors(targetDimensions + 1, sourceDimensions + 1)));
    }

    /**
     * The parameter descriptor to be returned by {@link Affine#getParameters()}.
     * The only purpose of this class is to override the {@link #createValue()} method.
     */
    private static final class Descriptor extends DefaultParameterDescriptorGroup {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 8320799650519834830L;

        /** Creates a new descriptor for the given parameters. */
        Descriptor(final Map<String,?> properties, final ParameterDescriptor<?>[] parameters) {
            super(properties, 1, 1, parameters);
        }

        /**
         * Returns default parameter values for the "Affine" operation. Unconditionally use the WKT1 parameter names,
         * regardless of whether this descriptor is for the EPSG:9624 case, because the caller is free to change the
         * matrix size, in which case (strictly speaking) the parameters would no longer be for EPSG:9624 operation.
         */
        @Override
        public ParameterValueGroup createValue() {
            return TensorParameters.WKT1.createValueGroup(IDENTIFICATION_OGC);
        }
    }

    /*
     * Do not override the 'getOperationType()' method. We want to inherit the super-type value, which is
     * SingleOperation.class, because we do not know if this operation method will be used for a Conversion
     * or a Transformation. When applied on geocentric coordinates, this method applies a transformation
     * (indeeded, the EPSG method name is "Affine parametric transformation"). But this method can also
     * be applied for unit conversions or axis swapping for examples, which are conversions.
     */

    /**
     * The inverse of this operation can be described by the same operation with different parameter values.
     *
     * @return {@code true} for all {@code Affine}.
     */
    @Override
    public final boolean isInvertible() {
        return true;
    }

    /**
     * Creates a projective transform from the specified group of parameter values.
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
        return getProvider(sourceDimensions, targetDimensions, false);
    }

    /**
     * Returns the index where to store a method of the given dimensions in the {@link #cached} array,
     * or -1 if it should not be cached.
     */
    private static int cacheIndex(int sourceDimensions, int targetDimensions) {
        if (--sourceDimensions >= 0 && sourceDimensions < MAX_CACHED_DIMENSION &&
            --targetDimensions >= 0 && targetDimensions < MAX_CACHED_DIMENSION)
        {
            return sourceDimensions * MAX_CACHED_DIMENSION + targetDimensions;
        }
        return -1;
    }

    /**
     * Returns the operation method for the specified source and target dimensions.
     * This method provides different {@code Affine} instances for different dimensions.
     *
     * @param  sourceDimensions  the number of source dimensions.
     * @param  targetDimensions  the number of target dimensions.
     * @param  isAffine          {@code true} if the transform is affine.
     * @return the provider for transforms of the given source and target dimensions.
     */
    public static Affine getProvider(final int sourceDimensions, final int targetDimensions, final boolean isAffine) {
        Affine method;
        if (isAffine && sourceDimensions == EPSG_DIMENSION && targetDimensions == EPSG_DIMENSION) {
            /*
             * Matrix complies with EPSG:9624 definition. This is the most common case. We do perform synchronization
             * for this field since it is okay if the same object is created twice (they should be identical).
             */
            method = EPSG_METHOD;
            if (method == null) {
                method = new Affine();
            }
        } else {
            /*
             * All other cases. We will use the WKT1 parameter names instead than the EPSG ones.
             */
            final int index = cacheIndex(sourceDimensions, targetDimensions);
            if (index >= 0) {
                synchronized (cached) {
                    method = cached[index];
                }
                if (method != null) {
                    return method;
                }
            }
            /*
             * At this point, no existing instance has been found in the cache.
             * Create a new instance and cache it if its dimension is not too large.
             */
            method = new Affine(sourceDimensions, targetDimensions);
            if (index >= 0) {
                synchronized (cached) {
                    final Affine other = cached[index];     // May have been created in another thread.
                    if (other != null) {
                        return other;
                    }
                    cached[index] = method;
                }
            }
        }
        return method;
    }

    /**
     * Returns the parameter values for the given matrix. This method is invoked by implementations of
     * {@link org.apache.sis.referencing.operation.transform.AbstractMathTransform#getParameterValues()}.
     *
     * @param  matrix The matrix for which to get parameter values.
     * @return The parameters of the given matrix.
     */
    public static ParameterValueGroup parameters(final Matrix matrix) {
        final int sourceDimensions = matrix.getNumCol() - 1;
        final int targetDimensions = matrix.getNumRow() - 1;
        final TensorParameters<Double> parameters;
        final Map<String,?> properties;
        if (sourceDimensions == EPSG_DIMENSION && targetDimensions == EPSG_DIMENSION && Matrices.isAffine(matrix)) {
            parameters = TensorParameters.ALPHANUM;
            properties = IDENTIFICATION_EPSG;
        } else {
            parameters = TensorParameters.WKT1;
            properties = IDENTIFICATION_OGC;
        }
        return parameters.createValueGroup(properties, matrix);
    }
}
