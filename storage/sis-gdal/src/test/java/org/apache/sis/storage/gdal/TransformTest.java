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
package org.apache.sis.storage.gdal;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.test.referencing.ParameterizedTransformTest;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.BeforeClass;
import org.junit.AfterClass;

import static org.apache.sis.test.Assert.*;


/**
 * Tests various map projections using {@literal Proj.4} wrappers.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
@RunWith(JUnit4.class)
public class TransformTest extends ParameterizedTransformTest {
    /**
     * The operation methods that we failed to instantiate.
     * We use this set for verifying that there is no more failures than the expected ones.
     */
    private static final Set<String> FAILURES = new HashSet<>();

    /**
     * Creates a new test suite.
     */
    public TransformTest() {
        super(new MTFactory(null) {
            @Override
            public ParameterValueGroup getDefaultParameters(final String method) throws NoSuchIdentifierException {
                try {
                    return super.getDefaultParameters(method);
                } catch (NoSuchIdentifierException e) {
                    FAILURES.add(method);
                    throw e;                            // Instructs ParameterizedTransformTest to skip the test.
                }
            }
        });
        isDerivativeSupported = false;
    }

    /**
     * Verifies if the {@literal Proj.4} library is available.
     */
    @BeforeClass
    public static void verifyNativeLibraryAvailability() {
        PJTest.verifyNativeLibraryAvailability();
    }

    /**
     * Invoked after all the tests have been run for comparing the list of failures with the expected list.
     * This method checks for the exact same content, so this method detects both unexpected failures and
     * "unexpected" successes. Note that a failure is not necessarily because Proj.4 does not support the
     * map projection; it may also be because Apache SIS does not yet declare the corresponding operation
     * method.
     */
    @AfterClass
    public static void verifyFailureList() {
        /*
         * The list of failures is empty if verifyNativeLibraryAvailability() failed,
         * in which case no test have been run.
         */
        if (!FAILURES.isEmpty()) {
            assertSetEquals(Arrays.asList(
                    "Abridged Molodensky",
                    "Cassini-Soldner",                          // No OperationMethod in SIS yet.
                    "Hotine Oblique Mercator (variant B)",
                    "Krovak",                                   // No OperationMethod in SIS yet.
                    "Lambert Azimuthal Equal Area",             // No OperationMethod in SIS yet.
                    "Lambert Conic Conformal (1SP)",
                    "Lambert Conic Conformal (2SP Belgium)",
                    "Lambert Conic Conformal (2SP Michigan)",
                    "Mercator (Spherical)",
                    "Mercator (variant C)",
                    "Polar Stereographic (variant B)",
                    "Polar Stereographic (variant C)",
                    "Popular Visualisation Pseudo Mercator",
                    "Polyconic",                                // No OperationMethod in SIS yet.
                    "Transverse Mercator (South Orientated)"), FAILURES);
            FAILURES.clear();
        }
    }
}
