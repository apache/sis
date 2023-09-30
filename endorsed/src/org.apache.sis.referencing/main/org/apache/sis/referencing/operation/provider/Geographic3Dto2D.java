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
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.referencing.util.WKTKeywords;
import org.apache.sis.referencing.util.WKTUtilities;


/**
 * The provider for <cite>"Geographic 3D to 2D conversion"</cite> (EPSG:9659).
 * This is a trivial operation that just drop the height in a geographic coordinate.
 * The inverse operation arbitrarily sets the ellipsoidal height to zero.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see Geographic2Dto3D
 */
@XmlTransient
public final class Geographic3Dto2D extends GeographicRedimension {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -9103595336196565505L;

    /**
     * The group of all parameters expected by this coordinate operation (in this case, none).
     */
    public static final ParameterDescriptorGroup PARAMETERS = builder()
            .addIdentifier("9659").addName("Geographic3D to 2D conversion").createGroup();

    /**
     * The providers for all combinations between 2D and 3D cases.
     * Conceptually a field of {@link GeographicRedimension} parent class,
     * but needs to be defined here because of class initialization order.
     */
    static final GeographicRedimension[] REDIMENSIONED = new GeographicRedimension[4];
    static {
        REDIMENSIONED[0] = new GeographicRedimension(0, "Identity 2D");
        REDIMENSIONED[1] = new Geographic2Dto3D(1);
        REDIMENSIONED[2] = new Geographic3Dto2D(2);
        REDIMENSIONED[3] = new GeographicRedimension(3, "Identity 3D");
    }

    /**
     * The unique transform instance, created when first needed.
     */
    private transient MathTransform transform;

    /**
     * Constructs a provider that can be resized.
     */
    private Geographic3Dto2D(final int indexOfDim) {
        super(PARAMETERS, indexOfDim);
    }

    /**
     * Constructs a provider with default parameters.
     *
     * @deprecated This is a temporary constructor before replacement by a {@code provider()} method with JDK9.
     */
    @Deprecated
    public Geographic3Dto2D() {
        super(REDIMENSIONED[2]);
    }

    /**
     * Returns the inverse of this operation.
     */
    @Override
    public AbstractProvider inverse() {
        return REDIMENSIONED[1];
    }

    /**
     * Returns the transform.
     *
     * <h4>Implementation note</h4>
     * Creating a transform that drop a dimension is trivial. We even have a helper method for that:
     * {@link Matrices#createDimensionSelect}  The difficulty is that the inverse of that transform
     * will set the height to NaN, while we want zero. The trick is to first create the transform for
     * the inverse transform with the zero that we want, then get the inverse of that inverse transform.
     * The transform that we get will remember where it come from (its inverse).
     *
     * <p>This work with SIS implementation, but is not guaranteed to work with other implementations.
     * For that reason, this method does not use the given {@code factory}.</p>
     *
     * @param  factory  ignored (can be null).
     * @param  values   ignored.
     * @return the math transform.
     * @throws FactoryException should never happen.
     */
    @Override
    public synchronized MathTransform createMathTransform(MathTransformFactory factory, ParameterValueGroup values)
            throws FactoryException
    {
        if (transform == null) try {
            final MatrixSIS m = Matrices.createDiagonal(4, 3);
            m.setElement(2, 2, 0);                                  // Here is the height value that we want.
            m.setElement(3, 2, 1);
            transform = MathTransforms.linear(m).inverse();
        } catch (NoninvertibleTransformException e) {
            throw new FactoryException(e);                          // Should never happen.
        }
        return transform;
    }

    /**
     * A temporary placeholder used for formatting a {@code PARAM_MT["Geographic 3D to 2D conversion"]} element in
     * Well-Known Text format. This placeholder is needed because there is no {@link MathTransform} implementation
     * for the Geographic 3D to 2D conversion, since we use affine transform instead.
     */
    public static final class WKT extends FormattableObject implements Parameterized {
        /**
         * {@code true} if this placeholder is for the inverse transform instead of the direct one.
         */
        private final boolean inverse;

        /**
         * Creates a new object to be formatted.
         *
         * @param inverse {@code false} for the "Geographic3D to 2D" operation, or {@code true} for its inverse.
         */
        public WKT(final boolean inverse) {
            this.inverse = inverse;
        }

        /**
         * Returns the parameters descriptor.
         */
        @Override
        public ParameterDescriptorGroup getParameterDescriptors() {
            return PARAMETERS;
        }

        /**
         * Returns the parameter values.
         */
        @Override
        public ParameterValueGroup getParameterValues() {
            return PARAMETERS.createValue();
        }

        /**
         * Formats a <cite>Well Known Text</cite> version 1 (WKT 1) element for a transform using this group of parameters.
         *
         * <h4>Compatibility note</h4>
         * {@code Param_MT} is defined in the WKT 1 specification only.
         * If the {@linkplain Formatter#getConvention() formatter convention} is set to WKT 2,
         * then this method silently uses the WKT 1 convention without raising an error.
         *
         * @return {@code "Param_MT"} or {@code "Inverse_MT"}.
         */
        @Override
        protected String formatTo(final Formatter formatter) {
            if (inverse) {
                formatter.append(new WKT(false));
                return WKTKeywords.Inverse_MT;
            } else {
                WKTUtilities.appendParamMT(getParameterValues(), formatter);
                return WKTKeywords.Param_MT;
            }
        }
    }
}
