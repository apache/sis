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
package org.apache.sis.util.iso;

import org.opengis.util.GenericName;
import org.opengis.test.util.NameTest;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestRunner;
import org.junit.runner.RunWith;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Runs the suite of tests provided in the GeoAPI project. The test suite is run using
 * a {@link DefaultNameFactory} instance shared for all tests in this class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.3
 * @module
 */
@RunWith(TestRunner.class)
@DependsOn({DefaultLocalNameTest.class, DefaultScopedNameTest.class})
public final strictfp class DefaultNameFactoryTest extends NameTest {
    /**
     * The factory to test.
     */
    private static DefaultNameFactory factorySIS;

    /**
     * Creates a new test suite using the singleton factory instance.
     */
    public DefaultNameFactoryTest() {
        super(factorySIS);
    }

    /**
     * Creates the singleton factory instance to be reused for all tests in this class.
     */
    @BeforeClass
    public static void createFactory() {
        factorySIS = new DefaultNameFactory();
    }

    /**
     * Disposes the singleton factory instance after all tests have been executed.
     */
    @AfterClass
    public static void disposeFactory() {
        factorySIS = null;
    }

    /**
     * Tests navigation in a name parsed from a string.
     */
    @Test
    public void testNavigation() {
        final GenericName name = factory.parseGenericName(null, "codespace:subspace:name");
        assertEquals("codespace:subspace:name", name.toString());
        assertEquals("codespace:subspace",      name.tip().scope().name().toString());
        assertEquals("codespace",               name.tip().scope().name().tip().scope().name().toString());
        assertSame(name, name.toFullyQualifiedName());
        assertSame(name, name.tip().toFullyQualifiedName());
    }

    /**
     * Tests the creation of scoped names where different parts of the name are {@link SimpleInternationalString}
     * instances. The implementation should be able to detect that the names and their hash codes are equal.
     *
     * @see DefaultScopedNameTest#testSimpleInternationalString()
     */
    @Test
    public void testSimpleInternationalString() {
        GenericName n1 = factory.createGenericName(null, "ns1", "Route");
        GenericName n2 = factory.createGenericName(null, new SimpleInternationalString("ns1"), "Route");
        GenericName n3 = factory.createGenericName(null, "ns1", new SimpleInternationalString("Route"));
        assertSame(n1, n2);
        assertSame(n1, n3);
        assertSame(n2, n3);
    }
}
