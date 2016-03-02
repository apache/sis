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
 * Tests the {@link AboutCommand} sub-command.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
@DependsOn(CommandRunnerTest.class)
public final strictfp class AboutCommandTest extends TestCase {
    /**
     * Tests the sub-command without option.
     *
     * @throws Exception Should never happen.
     */
    @Test
    public void testDefault() throws Exception {
        final AboutCommand test = new AboutCommand(0, CommandRunner.TEST);
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
     * @throws Exception Should never happen.
     */
    @Test
    public void testBrief() throws Exception {
        final AboutCommand test = new AboutCommand(0, CommandRunner.TEST, "--brief");
        test.run();
        final String result = getSingleton(CharSequences.splitOnEOL(test.outputBuffer.toString().trim())).toString();
        assertTrue(result, result.contains(Version.SIS.toString()));
    }

    /**
     * Tests the sub-command with the {@code --verbose} option.
     *
     * @throws Exception Should never happen.
     */
    @Test
    public void testVerbose() throws Exception {
        final AboutCommand test = new AboutCommand(0, CommandRunner.TEST, "--verbose");
        test.run();
        final String result = test.outputBuffer.toString();
        verify(result);

        // Check for a dependency which should be present.
        assertTrue("geoapi", result.contains("geoapi"));
    }

    /**
     * Tests the {@link AboutCommand#toRemoteURL(String)} method.
     */
    @Test
    public void testToRemoteURL() {
        assertEquals("service:jmx:rmi:///jndi/rmi://myhost:9999/jmxrmi",    AboutCommand.toRemoteURL("myhost:9999"));
        assertEquals("service:jmx:rmi:///jndi/rmi://myhost:1099/jmxrmi",    AboutCommand.toRemoteURL("myhost"));
        assertEquals("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",          AboutCommand.toRemoteURL("localhost:9999"));
        assertEquals("service:jmx:rmi:///jndi/rmi://:1099/jmxrmi",          AboutCommand.toRemoteURL("localhost"));
        assertEquals("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",          AboutCommand.toRemoteURL(":9999"));
        assertEquals("service:jmx:rmi:///jndi/rmi://localhosx:1099/jmxrmi", AboutCommand.toRemoteURL("localhosx"));
    }
}
