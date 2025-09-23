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

import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.referencing.internal.shared.AffineTransform2D;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.apache.sis.test.TestCase;
import static org.apache.sis.referencing.Assertions.assertEnvelopeEquals;


/**
 * Tests the {@link DomainDefinition} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DomainDefinitionTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DomainDefinitionTest() {
    }

    /**
     * Tests domain transformation when the domain is provided by a step in a chain of transforms.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testTransformChain() throws TransformException {
        final AbstractMathTransform transform = new ConcatenatedTransform(new ConcatenatedTransform(
                new AffineTransform2D(2, 0, 0, 4, 0, 0),  new PseudoTransform(2, 2)),
                new AffineTransform2D(9, 0, 0, 9, 0, 0)); // This one should have no effect.

        final Envelope domain = MathTransforms.getDomain(transform).get();
        assertEnvelopeEquals(new Envelope2D(null, 0, 0, 1/2d, 1/4d), domain, STRICT);
    }
}
