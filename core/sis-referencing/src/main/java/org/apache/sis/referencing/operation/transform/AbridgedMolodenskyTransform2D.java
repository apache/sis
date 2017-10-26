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
package org.apache.sis.referencing.operation.transform;

import java.io.IOException;
import java.io.ObjectInputStream;
import org.opengis.referencing.datum.Ellipsoid;

import static java.lang.Math.*;


/**
 * Two-dimensional abridged Molodensky transform with all translation terms fixed to zero.
 * This implementation performs only a change of ellipsoid. It provides nothing new compared
 * to {@link MolodenskyFormula}, except performance.
 *
 * <p><b>Note:</b> this transform is yet more abridged than standard "abridged Molondensky" transform since
 * it sets all translation terms to zero. A better class name could be "Very abridged Molodensky transform".
 * For the usual abridged Molondensky with non-zero translation terms, use the parent class.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class AbridgedMolodenskyTransform2D extends MolodenskyTransform2D {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7732503837408555590L;

    /**
     * A combination of {@link #ANGULAR_SCALE} with other fields in an expression that become constants
     * because of the simplification applied in this {@code AbridgedMolodenskyTransform2D}.
     */
    private transient double scale;

    /**
     * Constructs a 2D transform.
     */
    AbridgedMolodenskyTransform2D(final Ellipsoid source, final Ellipsoid target) {
        super(source, target, 0, 0, 0, true);
        computeTransientFields();
    }

    /**
     * Constructs the inverse of a 2D transform.
     *
     * @param inverse  the transform for which to create the inverse.
     * @param source   the source ellipsoid of the given {@code inverse} transform.
     * @param target   the target ellipsoid of the given {@code inverse} transform.
     */
    AbridgedMolodenskyTransform2D(final MolodenskyTransform inverse, final Ellipsoid source, final Ellipsoid target) {
        super(inverse, source, target);
        computeTransientFields();
    }

    /**
     * Invoked on deserialization for restoring the {@link #scale} field.
     *
     * @param  in  the input stream from which to deserialize a math transform.
     * @throws IOException if an I/O error occurred while reading or if the stream contains invalid data.
     * @throws ClassNotFoundException if the class serialized on the stream is not on the classpath.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        computeTransientFields();
    }

    /**
     * Computes the {@link #scale} field from existing fields.
     */
    private void computeTransientFields() {
        scale = 2 * ANGULAR_SCALE * Δfmod / (semiMajor * (1 - eccentricitySquared));
    }

    /**
     * Transforms the (λ,φ) coordinates between two geographic CRS. This method performs the same transformation
     * than {@link MolodenskyTransform}, but the formulas are repeated and simplified here for performance reasons.
     * In addition of using abridged Molodensky formulas, this method assumes that {@link #tX}, {@link #tY} and
     * {@link #tZ} fields are zero.
     */
    @Override
    public void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) {
        System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
        while (--numPts >= 0) {
            final double φ    = dstPts[++dstOff];
            final double sinφ = sin(φ);
            final double cosφ = cos(φ);
            double ρ = 1 - eccentricitySquared * (sinφ * sinφ);
            ρ *= sqrt(ρ);
            dstPts[dstOff++] = φ + scale * (cosφ*sinφ) * ρ;
        }
    }
}
