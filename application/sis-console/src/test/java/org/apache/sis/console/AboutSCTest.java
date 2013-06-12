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
package org.apache.sis.console;

import org.apache.sis.util.Version;
import org.apache.sis.util.CharSequences;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests the {@link AboutSC} sub-command.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn(SubCommandTest.class)
public final strictfp class AboutSCTest extends TestCase {
    /**
     * Tests the sub-command without option.
     *
     * @throws InvalidOptionException Should never happen.
     */
    @Test
    public void testDefault() throws InvalidOptionException {
        final AboutSC test = new AboutSC(0, SubCommand.TEST);
        test.run();
        verify(test.outputBuffer.toString());
    }

    /**
     * Verify the given output of an {@code about} command.
     */
    private static void verify(final String result) {
        String expected = Version.SIS.toString();
        assertTrue(expected, result.contains(expected));

        expected = System.getProperty("java.version");
        assertTrue(expected, result.contains(expected));

        expected = System.getProperty("os.name");
        assertTrue(expected, result.contains(expected));

        expected = System.getProperty("user.home");
        assertTrue(expected, result.contains(expected));
    }

    /**
     * Tests the sub-command with the {@code --brief} option.
     *
     * @throws InvalidOptionException Should never happen.
     */
    @Test
    public void testBrief() throws InvalidOptionException {
        final AboutSC test = new AboutSC(0, SubCommand.TEST, "--brief");
        test.run();
        final String result = getSingleton(CharSequences.splitOnEOL(test.outputBuffer.toString().trim())).toString();
        assertTrue(result, result.contains(Version.SIS.toString()));
    }

    /**
     * Tests the sub-command with the {@code --verbose} option.
     *
     * @throws InvalidOptionException Should never happen.
     */
    @Test
    public void testVerbose() throws InvalidOptionException {
        final AboutSC test = new AboutSC(0, SubCommand.TEST, "--verbose");
        test.run();
        final String result = test.outputBuffer.toString();
        verify(result);

        // Check for a dependency which should be present.
        assertTrue("geoapi", result.contains("geoapi"));
    }
}
