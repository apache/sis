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

import org.opengis.util.NoSuchIdentifierException;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the registration of operation methods in {@link DefaultMathTransformFactory}. This test uses the
 * providers registered in all {@code META-INF/services/org.opengis.referencing.operation.OperationMethod}
 * files found on the classpath.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn({
    org.apache.sis.referencing.operation.DefaultOperationMethodTest.class,
    OperationMethodSetTest.class
})
public final strictfp class DefaultMathTransformFactoryTest extends TestCase {
    /**
     * The factory being tested.
     */
    private static DefaultMathTransformFactory factory;

    /**
     * Creates the factory to be tested.
     */
    @BeforeClass
    public static void createFactory() {
        factory = new DefaultMathTransformFactory();
    }

    /**
     * Releases the factory used for the tests.
     */
    @AfterClass
    public static void releaseFactory() {
        factory = null;
    }

    /**
     * Tests non-existent operation method.
     */
    @Test
    public void testNonExistentCode() {
        try {
            factory.getOperationMethod("EPXX:9624");
            fail("Expected NoSuchIdentifierException");
        } catch (NoSuchIdentifierException e) {
            final String message = e.getLocalizedMessage();
            assertTrue(message, message.contains("EPXX:9624"));
        }
    }
}
