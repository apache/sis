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

import java.io.IOException;
import java.io.ObjectInputStream;


/**
 * Base class of map projections based on distance along the meridian from equator to latitude φ.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @version 1.0
 *
 * @see <a href="https://en.wikipedia.org/wiki/Meridian_arc">Meridian arc on Wikipedia</a>
 *
 * @since 1.0
 * @module
 */
abstract class MeridionalDistanceBased extends NormalizedProjection {
    /**
     * {@code false} for using the original formulas as published by EPSG, or {@code true} for using formulas
     * modified using trigonometric identities. Use of trigonometric identities reduces the amount of calls to
     * {@link Math#sin(double)} and similar methods. Snyder 3-34 to 3-39 give the following identities:
     *
     * <pre>
     *     If:     f(φ) = A⋅sin(2φ) + B⋅sin(4φ) + C⋅sin(6φ) + D⋅sin(8φ)
     *     Then:   f(φ) = sin(2φ)⋅(A′ + cos(2φ)⋅(B′ + cos(2φ)⋅(C′ + D′⋅cos(2φ))))
     *     Where:  A′ = A - C
     *             B′ = 2B - 4D
     *             C′ = 4C
     *             D′ = 8D
     * </pre>
     *
     * Similar, but with cosine instead than sin and the addition of a constant:
     *
     * <pre>
     *     If:     f(φ) = A + B⋅cos(2φ) + C⋅cos(4φ) + D⋅cos(6φ) + E⋅cos(8φ)
     *     Then:   f(φ) = A′ + cos(2φ)⋅(B′ + cos(2φ)⋅(C′ + cos(2φ)⋅(D′ + E′⋅cos(2φ))))
     *     Where:  A′ = A - C + E
     *             B′ = B - 3D
     *             C′ = 2C - 8E
     *             D′ = 4D
     *             E′ = 8E
     * </pre>
     */
    private static final boolean ALLOW_TRIGONOMETRIC_IDENTITIES = true;

    /**
     * Creates a new normalized projection from the parameters computed by the given initializer.
     */
    MeridionalDistanceBased(final Initializer initializer) {
        super(initializer);
        computeCoefficients();
    }

    /**
     * Creates a new projection initialized to the same parameters than the given one.
     */
    MeridionalDistanceBased(final MeridionalDistanceBased other) {
        super(other);
    }

    /**
     * Restores transient fields after deserialization.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        computeCoefficients();
    }

    /**
     * Computes the coefficients in the series expansions from the {@link #eccentricitySquared} value.
     * This method shall be invoked after {@code MeridionalDistanceBased} construction or deserialization.
     */
    private void computeCoefficients() {
        final double e2 = eccentricitySquared;
        // TODO
    }
}
