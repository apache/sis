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
package org.apache.sis.referencing.geoapi;

import org.opengis.util.FactoryException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.MultiRegisterOperations;

// Test dependencies
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.sis.test.FailureDetailsReporter;


/**
 * Runs the suite of transformation tests provided in the GeoAPI project.
 * The test suite uses the authority factory instance registered in {@link CRS}.
 * Some (not all) of those tests require the EPSG geodetic database to be installed.
 * If that database is not available, tests that cannot be executed will be automatically skipped.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.1
 */
@ExtendWith(FailureDetailsReporter.class)
public final class AuthorityFactoryTest extends org.opengis.test.referencing.AuthorityFactoryTest {
    /**
     * Creates a new test suite using the singleton factory instance.
     *
     * @throws FactoryException if no factory can be returned for the given authority.
     */
    public AuthorityFactoryTest() throws FactoryException {
        super(MultiRegisterOperations.provider());
    }

    /**
     * Skips for now the <cite>Krovak</cite> projection.
     */
    @Override
    @Disabled("Projection not yet implemented")
    public void testEPSG_2065() {
    }

    /**
     * Skips for now the <cite>Lambert Azimuthal Equal Area</cite> projection.
     */
    @Override
    @Disabled("Projection not yet implemented")
    public void testEPSG_3035() {
    }

    /**
     * Skips for now the <cite>Hyperbolic Cassini-Soldner</cite> projection
     * because projection derivative (Jacobian matrix) is not yet implemented.
     */
    @Override
    @Disabled("Derivative (Jacobian) not yet implemented")
    public void testEPSG_3139() {
    }
}
