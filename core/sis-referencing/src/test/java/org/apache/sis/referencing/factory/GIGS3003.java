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
package org.apache.sis.referencing.factory;

import org.opengis.referencing.datum.DatumFactory;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.test.DependsOn;
import org.junit.FixMethodOrder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;


/**
 * Runs the <cite>Geospatial Integrity of Geoscience Software</cite> tests on
 * {@link org.apache.sis.referencing.datum.DefaultPrimeMeridian} objects creation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.6
 * @since   0.6
 * @module
 */
@DependsOn({
    org.apache.sis.referencing.datum.DefaultPrimeMeridianTest.class
})
@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.JVM)      // Intentionally want some randomness
public final strictfp class GIGS3003 extends org.opengis.test.referencing.gigs.GIGS3003 {
    /**
     * Creates a new test suite using the singleton factory instance.
     */
    public GIGS3003() {
        super(DefaultFactories.forBuildin(DatumFactory.class));
    }
}
