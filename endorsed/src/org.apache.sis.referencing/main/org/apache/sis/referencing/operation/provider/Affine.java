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

import java.util.Map;
import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.parameter.MatrixParameters;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.LinearTransform;


/**
 * The provider for <q>Affine parametric transformation</q> (EPSG:9624).
 * The set of available parameters depends on the matrix size, which is 3×3 by default.
 *
 * <table class="sis">
 *   <caption>{@code Affine} parameters</caption>
 *   <tr><th>EPSG code</th><th>EPSG name</th><th>OGC name</th><th>Default value</th></tr>
 *   <tr><td>    </td> <td>          </td> <td>{@code num_row}</td> <td>3</td></tr>
 *   <tr><td>    </td> <td>          </td> <td>{@code num_col}</td> <td>3</td></tr>
 *   <tr><td>8623</td> <td>{@code A0}</td> <td>{@code elt_0_2}</td> <td>1</td></tr>
 *   <tr><td>8624</td> <td>{@code A1}</td> <td>{@code elt_0_0}</td> <td>0</td></tr>
 *   <tr><td>8625</td> <td>{@code A2}</td> <td>{@code elt_0_1}</td> <td>0</td></tr>
 *   <tr><td>8639</td> <td>{@code B0}</td> <td>{@code elt_1_2}</td> <td>0</td></tr>
 *   <tr><td>8640</td> <td>{@code B1}</td> <td>{@code elt_1_0}</td> <td>1</td></tr>
 *   <tr><td>8641</td> <td>{@code B2}</td> <td>{@code elt_1_1}</td> <td>0</td></tr>
 *   <tr><td>    </td> <td>          </td> <td>{@code elt_2_0}</td> <td>0</td></tr>
 *   <tr><td>    </td> <td>          </td> <td>{@code elt_2_1}</td> <td>0</td></tr>
 *   <tr><td>    </td> <td>          </td> <td>{@code elt_2_2}</td> <td>1</td></tr>
 * </table>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
@XmlTransient
public final class Affine extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6001828063655967608L;

    /**
     * The <abbr>EPSG</abbr> code of this operation.
     */
    private static final int CODE = 9624;

    /**
     * The operation method name as defined in the <abbr>EPSG</abbr> database.
     * Must matches exactly the EPSG name (this will be verified by JUnit tests).
     * Note: in contrast, the name used by <abbr>OGC</abbr> is just "Affine".
     *
     * @see #IDENTIFICATION_OGC
     */
    public static final String NAME = "Affine parametric transformation";

    /**
     * The number of dimensions used by the <abbr>EPSG</abbr>:9624 definition.
     * Operation methods with a different number of dimensions should use the
     * Well Known Text (<abbr>WKT</abbr>) parameter names instead.
     */
    public static final int EPSG_DIMENSION = 2;

    /**
     * The maximal number of dimensions to be cached. Descriptors having more than
     * this number of dimensions will be recreated every time that they are requested.
     */
    private static final int MAX_CACHED_DIMENSION = 6;

    /**
     * Cached providers for methods using matrices of dimension 1×1 inclusive to
     * {@value #MAX_CACHED_DIMENSION}×{@value #MAX_CACHED_DIMENSION} inclusive.
     * The index of each element is computed by {@link #cacheIndex(int, int)}.
     * All usages of this array shall be synchronized on {@code CACHED}.
     */
    private static final Affine[] CACHED = new Affine[MAX_CACHED_DIMENSION * MAX_CACHED_DIMENSION];

    /**
     * The <abbr>OGC</abbr> name of this operation method.
     */
    private static final NamedIdentifier IDENTIFICATION_OGC =
            new NamedIdentifier(Citations.OGC, Constants.OGC, Constants.AFFINE, null, null);

    /**
     * The EPSG:9624 compliant instance.
     * The number of dimensions is {@value #EPSG_DIMENSION}.
     *
     * @see #provider()
     */
    private static final Affine EPSG_METHOD = new Affine();

    /**
     * Returns a provider for affine transforms defined by EPSG:9624 parameters.
     * This method is invoked by {@link java.util.ServiceLoader} using reflection.
     *
     * @return the EPSG case of affine transform.
     */
    public static Affine provider() {
        return EPSG_METHOD;
    }

    /**
     * Creates a provider for affine transforms defined by EPSG:9624 parameters.
     *
     * @see org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory
     *
     * @todo Delete this constructor after we stop class-path support.
     *       Implementation will be moved to {@link #EPSG_METHOD}.
     */
    public Affine() {
        this(EPSGName.properties(CODE, NAME, IDENTIFICATION_OGC), descriptors());
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private static ParameterDescriptor<?>[] descriptors() {
        final var descriptors = MatrixParameters.EPSG.getAllDescriptors(EPSG_DIMENSION, EPSG_DIMENSION + 1);
        return new ParameterDescriptor<?>[] {
            descriptors[4],     // A0
            descriptors[2],     // A1
            descriptors[3],     // A2
            descriptors[7],     // B0
            descriptors[5],     // B1
            descriptors[6]      // B2
        };
    }

    /**
     * Creates a provider for affine transforms defined by the given parameters.
     * This is created when first needed by {@link #provider(int, int, boolean)}.
     *
     * @see #provider(int, int, boolean)
     */
    private Affine(final Map<String,?> properties, final ParameterDescriptor<?>[] parameters) {
        super(SingleOperation.class,
              new Descriptor(properties, parameters),
              CoordinateSystem.class, false,
              CoordinateSystem.class, false,
              (byte) 1);
    }

    /**
     * The parameter descriptor to be returned by {@link Affine#getParameters()}.
     * The only purpose of this class is to override the {@link #createValue()} method.
     */
    private static final class Descriptor extends DefaultParameterDescriptorGroup {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 8320799650519834830L;

        /** Creates a new descriptor for the given parameters. */
        Descriptor(Map<String,?> properties, ParameterDescriptor<?>[] parameters) {
            super(properties, 1, 1, parameters);
        }

        /**
         * Returns default parameter values for the "Affine" or "Affine parametric transformation" operation.
         * Note that in the latter case, the matrix should not be resizable but this implementation does not
         * block the caller to nevertheless change the matrix size. In such case, the EPSG:9624 identifiers
         * are not okay anymore.
         */
        @Override
        public ParameterValueGroup createValue() {
            return convention(this).createValueGroup(Map.of(NAME_KEY, getName()));
        }
    }

    /**
     * Returns the parameter names convention for an operation of the given name.
     * The heuristics applied in this method may change in any future version.
     */
    private static MatrixParameters<Double> convention(final IdentifiedObject object) {
        return EPSGName.isCodeEquals(object, CODE) || IdentifiedObjects.isHeuristicMatchForName(object, NAME)
                ? MatrixParameters.EPSG : MatrixParameters.WKT1;
    }

    /*
     * Do not override the `getOperationType()` method. We want to inherit the super-type value, which is
     * SingleOperation.class, because we do not know if this operation method will be used for a Conversion
     * or a Transformation. When applied on geocentric coordinates, this method applies a transformation
     * (indeeded, the EPSG method name is "Affine parametric transformation"). But this method can also
     * be applied for unit conversions or axis swapping for examples, which are conversions.
     */

    /**
     * Returns an affine conversion with the specified number of dimensions.
     */
    @Override
    public AbstractProvider variantFor(final MathTransform transform) {
        final boolean isAffine = (transform instanceof LinearTransform) && ((LinearTransform) transform).isAffine();
        return provider(transform.getSourceDimensions(), transform.getTargetDimensions(), isAffine);
    }

    /**
     * Specifies that this operation shall be applied verbatim,
     * without normalization of source and target <abbr>CRS</abbr>.
     */
    @Override
    public FormulaCategory getFormulaCategory() {
        return FormulaCategory.APPLIED_VERBATIM;
    }

    /**
     * The inverse of this operation can be described by the same operation with different parameter values.
     *
     * @return {@code this} for all {@code Affine}.
     */
    @Override
    public AbstractProvider inverse() {
        return this;
    }

    /**
     * Creates a projective transform from the specified group of parameter values.
     *
     * @param  context  the parameter values together with its context.
     * @return the created math transform.
     * @throws ParameterNotFoundException if a required parameter was not found.
     */
    @Override
    public MathTransform createMathTransform(final Context context) {
        final ParameterValueGroup parameters = context.getCompletedParameters();
        return MathTransforms.linear(convention(parameters.getDescriptor()).toMatrix(parameters));
    }

    /**
     * Returns the index where to store a method of the given dimensions in the {@link #CACHED} array,
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
    public static Affine provider(final int sourceDimensions, final int targetDimensions, final boolean isAffine) {
        Affine method;
        if (isAffine && sourceDimensions == EPSG_DIMENSION && targetDimensions == EPSG_DIMENSION) {
            // Matrix complies with EPSG:9624 definition. This is the most common case.
            method = EPSG_METHOD;
        } else {
            // All other cases. We will use the WKT1 parameter names instead of the EPSG ones.
            final int index = cacheIndex(sourceDimensions, targetDimensions);
            if (index >= 0) {
                synchronized (CACHED) {
                    method = CACHED[index];
                }
                if (method != null) {
                    return method;
                }
            }
            /*
             * At this point, no existing instance has been found in the cache.
             * Create a new instance and cache it if its dimension is not too large.
             */
            var parameters = MatrixParameters.WKT1.getAllDescriptors(targetDimensions + 1, sourceDimensions + 1);
            method = new Affine(Map.of(NAME_KEY, IDENTIFICATION_OGC), parameters);
            if (index >= 0) {
                synchronized (CACHED) {
                    final Affine other = CACHED[index];     // May have been created in another thread.
                    if (other != null) {
                        return other;
                    }
                    CACHED[index] = method;
                }
            }
        }
        return method;
    }

    /**
     * Returns parameter values for an identity transform of the given input and output dimensions.
     * Callers can modify the returned parameters if desired.
     *
     * @param  dimension  the number of source and target dimensions.
     * @return parameters for an identity transform of the given dimensions.
     */
    public static ParameterValueGroup identity(int dimension) {
        final var values = MatrixParameters.WKT1.createValueGroup(Map.of(NAME_KEY, Constants.AFFINE));
        values.parameter(Constants.NUM_COL).setValue(++dimension);
        values.parameter(Constants.NUM_ROW).setValue(  dimension);
        return values;
    }

    /**
     * Returns the parameter values for the given matrix. This method is invoked by implementations of
     * {@link org.apache.sis.referencing.operation.transform.AbstractMathTransform#getParameterValues()}.
     *
     * <h4>Historical note</h4>
     * Before Apache <abbr>SIS</abbr> 1.5, this method sometime used the <abbr>EPSG</abbr>:9624 parameters.
     * However, the wrong values were assigned to the wrong parameters. Since Apache <abbr>SIS</abbr> 1.5,
     * the <abbr>OGC</abbr> names are always used for avoiding confusion.
     *
     * @param  matrix  the matrix for which to get parameter values.
     * @return the parameters of the given matrix.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-619">SIS-619</a>
     */
    public static ParameterValueGroup parameters(final Matrix matrix) {
        return MatrixParameters.WKT1.createValueGroup(Map.of(NAME_KEY, IDENTIFICATION_OGC), matrix);
    }
}
