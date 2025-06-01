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
package org.apache.sis.referencing.operation;

import org.opengis.util.FactoryException;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.referencing.privy.ReferencingUtilities;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.ContextualParameters.MatrixRole;
import org.apache.sis.referencing.internal.ParameterizedTransformBuilder;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.measure.Units;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.util.UnimplementedServiceException;


/**
 * Information about the context in which a {@code MathTransform} is created.
 * This class performs the same normalization as the super-class (namely axis swapping and unit conversions),
 * with the addition of longitude rotation for supporting change of prime meridian.
 * This latter change is not applied by the super-class because prime meridian is part of geodetic reference frame,
 * and the public math transform factory knows nothing about datum (on design, for separation of concerns).
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class MathTransformContext extends ParameterizedTransformBuilder {
    /**
     * The longitude of the source and target prime meridian, in number of degrees East of Greenwich.
     */
    private double sourceMeridian, targetMeridian;

    /**
     * Creates a new context which add some datum-related information in addition
     * to the information provided by the super-class.
     */
    MathTransformContext(final MathTransformFactory factory, final GeodeticDatum source, final GeodeticDatum target) {
        super(factory, null);
        final double rs = ReferencingUtilities.getGreenwichLongitude(source.getPrimeMeridian(), Units.DEGREE);
        final double rt = ReferencingUtilities.getGreenwichLongitude(target.getPrimeMeridian(), Units.DEGREE);
        if (rs != rt) {
            sourceMeridian = rs;
            targetMeridian = rt;
        }
    }

    /**
     * Creates a math transform that represent a change of coordinate system.
     * If one argument is an ellipsoidal coordinate systems, then the {@code ellipsoid} argument is mandatory.
     * In other cases (including the case where both coordinate systems are ellipsoidal),
     * the ellipsoid argument is ignored and can be {@code null}.
     *
     * <p>This method does not change the state of this {@code MathTransformContext}.
     * This method is defined here for {@link CoordinateOperationFinder} convenience,
     * because this method is invoked together with {@code setSource/TargetAxes(…)}.</p>
     *
     * <h4>Design note</h4>
     * This method does not accept separated ellipsoid arguments for {@code source} and {@code target} because
     * this method should not be used for datum shifts. If the two given coordinate systems are ellipsoidal,
     * then they are assumed to use the same ellipsoid. If different ellipsoids are desired, then a
     * parameterized transform like <q>Molodensky</q>, <q>Geocentric translations</q>, <q>Coordinate Frame Rotation</q>
     * or <q>Position Vector transformation</q> should be used instead.
     *
     * @param  source     the source coordinate system.
     * @param  target     the target coordinate system.
     * @param  ellipsoid  the ellipsoid of {@code EllipsoidalCS}, or {@code null} if none.
     * @return a conversion from the given source to the given target coordinate system.
     * @throws FactoryException if the conversion cannot be created.
     */
    final MathTransform createCoordinateSystemChange(final CoordinateSystem source,
                                                     final CoordinateSystem target,
                                                     final Ellipsoid ellipsoid)
            throws FactoryException
    {
        final var builder = getFactory().builder(Constants.COORDINATE_SYSTEM_CONVERSION);
        builder.setSourceAxes(source, ellipsoid);
        builder.setTargetAxes(target, ellipsoid);
        return builder.create();
    }

    /**
     * Returns the normalization or denormalization matrix.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public Matrix getMatrix(final MatrixRole role) throws FactoryException {
        final Class<? extends CoordinateSystem> userCS;
        boolean inverse = false;
        double rotation;
        switch (role) {
            default: throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "role", role));
            case INVERSE_NORMALIZATION:   inverse  = true;              // Fall through
            case NORMALIZATION:           rotation = sourceMeridian;
                                          userCS   = getSourceCSType();
                                          break;
            case INVERSE_DENORMALIZATION: inverse  = true;              // Fall through
            case DENORMALIZATION:         inverse  = !inverse;
                                          rotation = targetMeridian;
                                          userCS   = getTargetCSType();
                                          break;
        }
        Matrix matrix = super.getMatrix(role);
        if (rotation != 0) {
            if (inverse) rotation = -rotation;
            MatrixSIS cm = MatrixSIS.castOrCopy(matrix);
            if (CartesianCS.class.isAssignableFrom(userCS)) {
                rotation = Math.toRadians(rotation);
                final var rot = new Matrix4();
                rot.m00 =   rot.m11 = Math.cos(rotation);
                rot.m01 = -(rot.m10 = Math.sin(rotation));
                if (inverse) {
                    matrix = Matrices.multiply(rot, cm);        // Apply the rotation after denormalization.
                } else {
                    matrix = cm.multiply(rot);                  // Apply the rotation before normalization.
                }
            } else if (userCS == CoordinateSystem.class
                    || EllipsoidalCS.class.isAssignableFrom(userCS)
                    ||   SphericalCS.class.isAssignableFrom(userCS))
            {
                final Double value = rotation;
                if (inverse) {
                    cm.convertBefore(0, null, value);           // Longitude is the first axis in normalized CS.
                } else {
                    cm.convertAfter(0, null, value);
                }
                matrix = cm;
            } else {
                throw new UnimplementedServiceException(Errors.format(Errors.Keys.UnsupportedCoordinateSystem_1, userCS.getName()));
            }
        }
        return matrix;
    }
}
