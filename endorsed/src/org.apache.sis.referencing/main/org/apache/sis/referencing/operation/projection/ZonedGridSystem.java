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
package org.apache.sis.referencing.operation.projection;

import java.util.EnumMap;
import java.util.Optional;
import java.io.Serializable;
import static java.lang.Math.PI;
import static java.lang.Math.floor;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform2D;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.referencing.operation.transform.DomainDefinition;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.measure.Longitude;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.internal.shared.Numerics;
import static org.apache.sis.referencing.operation.provider.TransverseMercator.*;
import static org.apache.sis.referencing.operation.provider.ZonedTransverseMercator.*;


/**
 * <cite>Transverse Mercator Zoned Grid System</cite> projection (EPSG codes 9824).
 * This projection is valid for all the world in a given hemisphere, except the poles.
 * The Earth is divided into zones, usually 6° width. The zone number is determined
 * automatically from the longitude and is prefixed to the Easting value.
 *
 * <p>This map projection is not suitable for geometric calculations like distances and angles,
 * since there is discontinuities (gaps) between zones. Actually this operation is not handled
 * as a map projection by Apache SIS, as can be seen from the different class hierarchy.</p>
 *
 * <div class="note"><b>Note:</b>
 * current implementation can only be backed by the Transverse Mercator projection,
 * but future versions could apply to some other projections if needed.</div>
 *
 * <p>Examples of CRS using this projection are <cite>WGS 84 / UTM grid system</cite>
 * EPSG:32600 (northern hemisphere) and EPSG:32700 (southern hemisphere).</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class ZonedGridSystem extends AbstractMathTransform2D implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7219325241026170925L;

    /**
     * The 360° range of longitude values.
     */
    private static final double RANGE = Longitude.MAX_VALUE - Longitude.MIN_VALUE;

    /**
     * The zone multiplication factor for encoding the zone in the easting.
     * This scale factor assumes that the projection results are in metres.
     */
    private static final double ZONE_SCALE = 1E6;

    /**
     * Westernmost longitude of the first zone.
     */
    final double initialLongitude;

    /**
     * Width of each Transverse Mercator zone, in the same units as longitude values.
     * This is usually 6°.
     */
    final double zoneWidth;

    /**
     * The projection that performs the actual work before we add the zone number.
     */
    @SuppressWarnings("serial")             // Most SIS implementations are serializable.
    final AbstractMathTransform projection;

    /**
     * The inverse of this map projection.
     */
    @SuppressWarnings("serial")             // Most SIS implementations are serializable.
    private final MathTransform2D inverse;

    /**
     * Creates a Zoned Grid System from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><q>Transverse Mercator Zoned Grid System</q>.</li>
     * </ul>
     *
     * Contrarily to other map projections in this package, there is no {@code createMapProjection(MathTransformFactory)}
     * method in this class. Instead, the factory must be specified at this {@code ZonedGridSystem} construction time.
     *
     * @param  method      description of the projection parameters.
     * @param  parameters  the parameter values of the projection to create.
     * @param  factory     the factory to use for creating the transform.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    public ZonedGridSystem(final OperationMethod method, final Parameters parameters, final MathTransformFactory factory)
            throws FactoryException
    {
        final EnumMap<NormalizedProjection.ParameterRole, ParameterDescriptor<Double>> roles =
                new EnumMap<>(NormalizedProjection.ParameterRole.class);
        roles.put(NormalizedProjection.ParameterRole.SCALE_FACTOR,   SCALE_FACTOR);
        roles.put(NormalizedProjection.ParameterRole.FALSE_EASTING,  FALSE_EASTING);
        roles.put(NormalizedProjection.ParameterRole.FALSE_NORTHING, FALSE_NORTHING);
        final Initializer initializer = new Initializer(method, parameters, roles, null);
        initialLongitude = initializer.getAndStore(INITIAL_LONGITUDE);
        zoneWidth        = initializer.getAndStore(ZONE_WIDTH);
        final MatrixSIS normalize = initializer.context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
        normalize.convertBefore(0, null, zoneWidth / -2);
        projection = (AbstractMathTransform) new TransverseMercator(initializer).createMapProjection(factory);
        inverse    = new Inverse(this);
    }


    /**
     * Returns the parameter values of this zoned grid system projection.
     *
     * @return the internal parameter values for this zoned grid system projection.
     */
    @Override
    public ParameterValueGroup getParameterValues() {
        return projection.getParameterValues();
    }

    /**
     * Returns the domain of input coordinates.
     * The limits defined by this method are arbitrary and may change in any future implementation.
     * Current implementation sets a longitude range of ±180° (i.e. the world) and a latitude range
     * from 84°S to 84°N.
     */
    @Override
    public Optional<Envelope> getDomain(final DomainDefinition criteria) {
        final double y = -NormalizedProjection.POLAR_AREA_LIMIT;
        return Optional.of(new Envelope2D(null, -PI, y, 2*PI, -2*y));
    }

    /**
     * Projects the specified (λ,φ) coordinates and stores the result in {@code dstPts}.
     * In addition, opportunistically computes the projection derivative if {@code derivate} is {@code true}.
     * Note that the derivative does not contain zone prefix.
     *
     * @return the matrix of the projection derivative at the given source position,
     *         or {@code null} if the {@code derivate} argument is {@code false}.
     * @throws TransformException if the coordinates cannot be converted.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws TransformException
    {
        double λ = srcPts[srcOff] - initialLongitude;
        double φ = srcPts[srcOff+1];
        λ -= RANGE * floor(λ / RANGE);
        final double zone = floor(λ / zoneWidth);
        λ -= (zone * zoneWidth);
        dstPts[dstOff  ] = λ;
        dstPts[dstOff+1] = φ;
        final Matrix derivative = projection.transform(dstPts, dstOff, dstPts, dstOff, derivate);
        dstPts[dstOff] += (zone + 1) * ZONE_SCALE;
        return derivative;
    }

    /**
     * Returns the inverse of this map projection.
     *
     * @return the inverse of this map projection.
     */
    @Override
    public MathTransform2D inverse() {
        return inverse;
    }

    /**
     * Inverse of a zoned grid system.
     *
     * @author  Martin Desruisseaux (Geomatys)
     */
    private static final class Inverse extends AbstractMathTransform2D.Inverse implements Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -4417726238412154175L;

        /**
         * The enclosing transform.
         */
        private final ZonedGridSystem forward;

        /**
         * The projection that performs the actual work after we removed the zone number.
         */
        @SuppressWarnings("serial")         // Most SIS implementations are serializable.
        private final AbstractMathTransform inverseProjection;

        /**
         * Default constructor.
         */
        Inverse(final ZonedGridSystem forward) throws FactoryException {
            this.forward = forward;
            try {
                inverseProjection = (AbstractMathTransform) forward.projection.inverse();
            } catch (NoninvertibleTransformException e) {
                throw new FactoryException(e);                  // Should not happen.
            }
        }

        /**
         * Returns the inverse of this math transform.
         */
        @Override
        public MathTransform2D inverse() {
            return forward;
        }

        /**
         * Inverse transforms the specified {@code srcPts} and stores the result in {@code dstPts}.
         * If the derivative has been requested, then this method will delegate the derivative
         * calculation to the enclosing class and inverts the resulting matrix.
         */
        @Override
        public Matrix transform(final double[] srcPts, final int srcOff,
                                final double[] dstPts, final int dstOff,
                                final boolean derivate) throws TransformException
        {
            double x = srcPts[srcOff  ];
            double y = srcPts[srcOff+1];
            double zone = floor(x / ZONE_SCALE) - 1;
            x -= (zone + 1) * ZONE_SCALE;
            dstPts[dstOff  ] = x;
            dstPts[dstOff+1] = y;
            final Matrix derivative = inverseProjection.transform(dstPts, dstOff, dstPts, dstOff, derivate);
            dstPts[dstOff] += zone * forward.zoneWidth + forward.initialLongitude;
            return derivative;
        }
    }

    /**
     * Computes a hash code value for this {@code ZonedGridSystem}.
     *
     * @return the hash code value.
     */
    @Override
    protected int computeHashCode() {
        final long c = Double.doubleToLongBits(initialLongitude) + 31*Double.doubleToLongBits(zoneWidth);
        return (super.computeHashCode() ^ Long.hashCode(c)) + 37 * projection.hashCode();
    }

    /**
     * Compares the given object with this transform for equivalence.
     * If this method returns {@code true}, then for any given identical source position,
     * the two compared map projections shall compute the same target position.
     *
     * @param  object  the object to compare with this map projection for equivalence.
     * @param  mode    the strictness level of the comparison. Default to {@link ComparisonMode#STRICT}.
     * @return {@code true} if the given object is equivalent to this map projection.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;
        }
        if (super.equals(object, mode)) {
            final ZonedGridSystem that = (ZonedGridSystem) object;
            return Numerics.equals(initialLongitude, that.initialLongitude) &&
                   Numerics.equals(zoneWidth, that.zoneWidth) &&
                   projection.equals(that.projection, mode);
        }
        return false;
    }
}
