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
package org.apache.sis.measure;

import tec.units.tck.TCKRunner;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;


/**
 * Runs all tests provided by the JSR-363 compatibility test suite.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final strictfp class ConformanceTest {
    /**
     * Runs the TCK tests.
     */
    @Test
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void runTCK() {
        final TCKRunner tests = new TCKRunner();
        assertEquals("TCK exit code reports test failure. ", 0, tests.run(System.in, System.out, System.err));
    }
}
