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

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.opengis.test.util.NameTest;
import org.apache.sis.test.DependsOn;


/**
 * Runs the suite of tests provided in the GeoAPI project. The test suite is run using
 * a {@link DefaultNameFactory} instance shared for all tests in this class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from goetk-3.00)
 * @version 0.3
 * @module
 */
@RunWith(JUnit4.class)
@DependsOn(AbstractNameTest.class)
public final strictfp class DefaultNameFactoryTest extends NameTest {
    /**
     * The factory to test.
     */
    private static DefaultNameFactory factory;

    /**
     * Creates a new test suite using the singleton factory instance.
     */
    public DefaultNameFactoryTest() {
        super(factory);
    }

    /**
     * Creates the singleton factory instance to be reused for all tests in this class.
     */
    @BeforeClass
    public static void createFactory() {
        factory = new DefaultNameFactory();
    }

    /**
     * Disposes the singleton factory instance after all tests have been executed.
     */
    @AfterClass
    public static void disposeFactory() {
        factory = null;
    }
}
