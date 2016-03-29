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

import javax.measure.unit.NonSI;
import org.opengis.util.FactoryException;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.ContextualParameters.MatrixRole;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory.Context;
import org.apache.sis.util.resources.Errors;


/**
 * Information about the context in which a {@code MathTransform} is created.
 * This class performs the same normalization than the super-class (namely axis swapping and unit conversions),
 * with the addition of longitude rotation for supporting change of prime meridian.  This later change is not
 * applied by the super-class because prime meridian is part of geodetic datum, and the public math transform
 * factory know nothing about datum (on design, for separation of concerns).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class MathTransformContext extends Context {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8765209303733056283L;

    /**
     * The longitude of the source and target prime meridian, in number of degrees East of Greenwich.
     */
    private double sourceMeridian, targetMeridian;

    /**
     * Creates a new context which add some datum-related information in addition
     * to the information provided by the super-class.
     */
    MathTransformContext(final GeodeticDatum source, final GeodeticDatum target) {
        final double rs = ReferencingUtilities.getGreenwichLongitude(source.getPrimeMeridian(), NonSI.DEGREE_ANGLE);
        final double rt = ReferencingUtilities.getGreenwichLongitude(target.getPrimeMeridian(), NonSI.DEGREE_ANGLE);
        if (rs != rt) {
            sourceMeridian = rs;
            targetMeridian = rt;
        }
    }

    /**
     * Returns the normalization or denormalization matrix.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public Matrix getMatrix(final MatrixRole role) throws FactoryException {
        final CoordinateSystem cs;
        boolean inverse = false;
        double rotation;
        switch (role) {
            default: throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "role", role));
            case INVERSE_NORMALIZATION:   inverse  = true;              // Fall through
            case NORMALIZATION:           rotation = sourceMeridian;
                                          cs       = getSourceCS();
                                          break;
            case INVERSE_DENORMALIZATION: inverse  = true;              // Fall through
            case DENORMALIZATION:         inverse  = !inverse;
                                          rotation = targetMeridian;
                                          cs       = getTargetCS();
                                          break;
        }
        Matrix matrix = super.getMatrix(role);
        if (rotation != 0) {
            if (inverse) rotation = -rotation;
            MatrixSIS cm = MatrixSIS.castOrCopy(matrix);
            if (cs instanceof CartesianCS) {
                rotation = Math.toRadians(rotation);
                final Matrix4 rot = new Matrix4();
                rot.m00 =   rot.m11 = Math.cos(rotation);
                rot.m01 = -(rot.m10 = Math.sin(rotation));
                if (inverse) {
                    matrix = Matrices.multiply(rot, cm);        // Apply the rotation after denormalization.
                } else {
                    matrix = cm.multiply(rot);                  // Apply the rotation before normalization.
                }
            } else {
                final Double value = rotation;
                if (inverse) {
                    cm.convertBefore(0, null, value);           // Longitude is the first axis in normalized CS.
                } else {
                    cm.convertAfter(0, null, value);
                }
                matrix = cm;
            }
        }
        return matrix;
    }
}
