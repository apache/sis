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
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.internal.referencing.WKTUtilities;


/**
 * The provider for <cite>"Geographic 3D to 2D conversion"</cite> (EPSG:9659).
 * This is a trivial operation that just drop the height in a geographic coordinate.
 * The inverse operation arbitrarily sets the ellipsoidal height to zero.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@XmlTransient
public final class Geographic3Dto2D extends AbstractProvider {
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
     * The unique instance, created when first needed.
     */
    private transient MathTransform transform;

    /**
     * Constructs a provider with default parameters.
     */
    public Geographic3Dto2D() {
        super(3, 2, PARAMETERS);
    }

    /**
     * Returns the operation type.
     *
     * @return Interface implemented by all coordinate operations that use this method.
     */
    @Override
    public Class<Conversion> getOperationType() {
        return Conversion.class;
    }

    /**
     * Returns the transform.
     *
     * <div class="note"><b>Implementation note:</b>
     * creating a transform that drop a dimension is trivial. We even have a helper method for that:
     * {@link Matrices#createDimensionSelect}  The difficulty is that the inverse of that transform
     * will set the height to NaN, while we want zero. The trick is to first create the transform for
     * the inverse transform with the zero that we want, then get the inverse of that inverse transform.
     * The transform that we get will remember where it come from (its inverse).
     *
     * <p>This work with SIS implementation, but is not guaranteed to work with other implementations.
     * For that reason, this method does not use the given {@code factory}.</p></div>
     *
     * @param  factory Ignored (can be null).
     * @param  values Ignored.
     * @return The math transform.
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
         * {@code true} if this placeholder is for the inverse transform instead than the direct one.
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
         * <div class="note"><b>Compatibility note:</b>
         * {@code Param_MT} is defined in the WKT 1 specification only.
         * If the {@linkplain Formatter#getConvention() formatter convention} is set to WKT 2,
         * then this method silently uses the WKT 1 convention without raising an error.</div>
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
