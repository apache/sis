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

import java.util.Map;
import java.util.HashMap;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.geometry.Envelope2D;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link SpecializableTransform}. This test use a simple affine transform that multiply
 * coordinate values by 10, except in specialized sub-areas in which case a small translation is added.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class SpecializableTransformTest extends MathTransformTestCase {
    /**
     * Creates a transform scaling the coordinate values by 10, then applying the given translation.
     */
    private static MathTransform translation(final double t) {
        return new AffineTransform2D(10, 0, 0, 10, t, t);
    }

    /**
     * Creates a transform to test.
     *
     * @throws InvalidGeodeticParameterException if {@link SpecializableTransform} constructor reject a parameter.
     */
    private static SpecializableTransform create() throws InvalidGeodeticParameterException {
        final Map<Envelope,MathTransform> specializations = new HashMap<>(4);
        assertNull(specializations.put(new Envelope2D(null, -5, -4, 10, 7), translation(0.1)));
        assertNull(specializations.put(new Envelope2D(null, -3, -1,  5, 2), translation(0.2)));
        return new SpecializableTransform(translation(0), specializations);
    }

    /**
     * Tests consistency between different {@code transform(…)} methods.
     *
     * @throws InvalidGeodeticParameterException if {@link SpecializableTransform} constructor reject a parameter.
     * @throws TransformException if a transformation failed.
     */
    @Test
    public void testConsistency() throws InvalidGeodeticParameterException, TransformException {
        transform = create();
        isDerivativeSupported = false;          // Actually supported, but our test transform has discontinuities.
        isInverseTransformSupported = false;
        verifyInDomain(CoordinateDomain.RANGE_10, -672445632505596619L);
    }
}
