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
package org.apache.sis.filter;

import java.util.Set;
import org.opengis.util.LocalName;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;

import static org.junit.Assert.*;

// Branch-dependent imports
import org.opengis.filter.ComparisonOperatorName;
import org.opengis.filter.capability.IdCapabilities;
import org.opengis.filter.capability.ScalarCapabilities;


/**
 * Tests {@link Capabilities} implementations.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class CapabilitiesTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public CapabilitiesTest() {
    }

    /**
     * Tests {@link Capabilities#getResourceIdentifiers()}.
     */
    @Test
    public void testResourceIdentifiers() {
        assertTrue(Capabilities.INSTANCE.getConformance().implementsResourceld());
        final IdCapabilities idc = Capabilities.INSTANCE.getIdCapabilities().get();
        final LocalName id = TestUtilities.getSingleton(idc.getResourceIdentifiers());
        assertEquals("identifier", id.toString());
    }

    /**
     * Tests {@link Capabilities#getComparisonOperators()}.
     */
    @Test
    public void testComparisonOperators() {
        final ScalarCapabilities c = Capabilities.INSTANCE.getScalarCapabilities().get();
        final Set<ComparisonOperatorName> op = c.getComparisonOperators();
        assertTrue(op.contains(ComparisonOperatorName.PROPERTY_IS_EQUAL_TO));
        assertTrue(op.contains(ComparisonOperatorName.PROPERTY_IS_LESS_THAN));
        assertTrue(op.contains(ComparisonOperatorName.PROPERTY_IS_GREATER_THAN));
    }
}
