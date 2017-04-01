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

import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link Molodensky} provider.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.7
 * @since   0.7
 * @module
 */
public final strictfp class MolodenskyTest extends TestCase {
    /**
     * Tests {@link Molodensky#redimension(int, int)}.
     */
    @Test
    public void testRedimension() {
        testRedimension(new Molodensky());
    }

    /**
     * Implementation of {@link #testRedimension()} to be shared with other provider having similar capability.
     */
    static void testRedimension(final AbstractProvider provider) {
        for (int sourceDimensions = 2; sourceDimensions <= 3; sourceDimensions++) {
            for (int targetDimensions = 2; targetDimensions <= 3; targetDimensions++) {
                final OperationMethod redim = provider.redimension(sourceDimensions, targetDimensions);
                assertEquals("sourceDimensions", sourceDimensions, redim.getSourceDimensions().intValue());
                assertEquals("targetDimensions", targetDimensions, redim.getTargetDimensions().intValue());
            }
        }
    }
}
