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
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.referencing.privy.WKTUtilities;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;


/**
 * The provider for <q>Geographic 3D to 2D conversion</q> (EPSG:9659).
 * This is a trivial operation that just drop the height in a geographic coordinate.
 * The inverse operation arbitrarily sets the ellipsoidal height to zero.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see Geographic2Dto3D
 * @see Spherical3Dto2D
 */
@XmlTransient
public final class Geographic3Dto2D extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -9103595336196565505L;

    /**
     * The <abbr>EPSG</abbr> name used for this operation method.
     */
    public static final String NAME = "Geographic3D to 2D conversion";

    /**
     * The group of all parameters expected by this coordinate operation (in this case, none).
     */
    public static final ParameterDescriptorGroup PARAMETERS = builder()
            .addIdentifier("9659").addName(NAME).createGroup();

    /**
     * The canonical instance of this operation method.
     *
     * @see #provider()
     */
    private static final Geographic3Dto2D INSTANCE = new Geographic3Dto2D();

    /**
     * Returns the canonical instance of this operation method.
     * This method is invoked by {@link java.util.ServiceLoader} using reflection.
     *
     * @return the canonical instance of this operation method.
     */
    public static Geographic3Dto2D provider() {
        return INSTANCE;
    }

    /**
     * Creates a new provider.
     *
     * @todo Make this constructor private after we stop class-path support.
     */
    public Geographic3Dto2D() {
        super(Conversion.class, PARAMETERS,
              CoordinateSystem.class, false,
              CoordinateSystem.class, false,
              (byte) 3);
    }

    /**
     * Returns the inverse of this operation.
     */
    @Override
    public AbstractProvider inverse() {
        return Geographic2Dto3D.provider();
    }

    /**
     * Returns the operation method which is the closest match for the given transform.
     * This is an adjustment based on the number of dimensions only, on the assumption
     * that the given transform has been created by this provider or a compatible one.
     */
    @Override
    public AbstractProvider variantFor(final MathTransform transform) {
        return transform.getSourceDimensions() < transform.getTargetDimensions() ? Geographic2Dto3D.provider() : this;
    }

    /**
     * Returns the transform.
     *
     * <h4>Implementation note</h4>
     * Creating a transform that drop a dimension is trivial. We even have a helper method for that:
     * {@link Matrices#createDimensionSelect}. The difficulty is that the inverse of that transform
     * will set the height to NaN, while we want zero. The trick is to first create the transform for
     * the inverse transform with the zero that we want, then get the inverse of that inverse transform.
     * The transform that we get will remember where it come from (its inverse).
     *
     * <p>This work with SIS implementation, but is not guaranteed to work with other implementations.</p>
     *
     * @param  context  the parameter values together with its context.
     * @return the math transform.
     * @throws FactoryException should never happen.
     */
    @Override
    public MathTransform createMathTransform(final Context context) throws FactoryException {
        return createMathTransform(context,
                context.getSourceDimensions().orElse(3),
                context.getTargetDimensions().orElse(2),
                Geographic2Dto3D.DEFAULT_HEIGHT);
    }

    /**
     * Implementation of {@link #createMathTransform(Context)} shared by {@link Geographic2Dto3D}.
     */
    static MathTransform createMathTransform(final Context context, int sourceDimensions, int targetDimensions, final double height)
            throws FactoryException
    {
        final boolean inverse = (sourceDimensions > targetDimensions);
        if (inverse) {
            final int swap = sourceDimensions;
            sourceDimensions = targetDimensions;
            targetDimensions = swap;
        }
        final MatrixSIS m = Matrices.createDiagonal(targetDimensions + 1, sourceDimensions + 1);
        m.setElement(sourceDimensions, sourceDimensions, height);
        m.setElement(targetDimensions, sourceDimensions, 1);    // Must be last in case the matrix is square.
        MathTransform tr = context.getFactory().createAffineTransform(m);
        if (inverse) try {
            tr = tr.inverse();
        } catch (NoninvertibleTransformException e) {
            throw new FactoryException(e);                      // Should never happen.
        }
        return tr;
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
         * Formats a <i>Well Known Text</i> version 1 (WKT 1) element for a transform using this group of parameters.
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
