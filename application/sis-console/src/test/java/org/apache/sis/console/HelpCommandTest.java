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

import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link HelpCommand} sub-command.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn(CommandRunnerTest.class)
public final strictfp class HelpCommandTest extends TestCase {
    /**
     * Tests the sub-command without option.
     *
     * @throws InvalidOptionException Should never happen.
     */
    @Test
    public void testDefault() throws InvalidOptionException {
        final HelpCommand test = new HelpCommand(0, CommandRunner.TEST);
        test.run();
        final String result = test.outputBuffer.toString();
        assertTrue("Apache SIS", result.startsWith("Apache SIS"));
        assertTrue("--locale",   result.contains("--locale"));
        assertTrue("--encoding", result.contains("--encoding"));
        assertTrue("--timezone", result.contains("--timezone"));
        assertTrue("--brief",    result.contains("--brief"));
        assertTrue("--verbose",  result.contains("--verbose"));
        assertTrue("--help",     result.contains("--help"));
    }

    /**
     * Tests the sub-command with the {@code --help} option.
     * Shall contain only a subset of {@link #testDefault()}.
     *
     * @throws InvalidOptionException Should never happen.
     */
    @Test
    public void testHelp() throws InvalidOptionException {
        final HelpCommand test = new HelpCommand(0, CommandRunner.TEST, "--help");
        test.help("help");
        final String result = test.outputBuffer.toString();
        assertTrue ("help",       result.startsWith("help"));
        assertTrue ("--locale",   result.contains("--locale"));
        assertTrue ("--encoding", result.contains("--encoding"));
        assertFalse("--timezone", result.contains("--timezone"));
        assertFalse("--brief",    result.contains("--brief"));
        assertFalse("--verbose",  result.contains("--verbose"));
        assertTrue ("--help",     result.contains("--help"));
    }

    /**
     * Tests the sub-command with the {@code --locale en} option.
     *
     * @throws InvalidOptionException Should never happen.
     */
    @Test
    public void testEnglishLocale() throws InvalidOptionException {
        final HelpCommand test = new HelpCommand(0, CommandRunner.TEST, "--help", "--locale", "en");
        test.help("help");
        final String result = test.outputBuffer.toString();
        assertTrue(result, result.contains("Show a help overview."));
        assertTrue(result, result.contains("The locale to use"));
    }

    /**
     * Tests the sub-command with the {@code --locale fr} option.
     *
     * @throws InvalidOptionException Should never happen.
     */
    @Test
    public void testFrenchLocale() throws InvalidOptionException {
        final HelpCommand test = new HelpCommand(0, CommandRunner.TEST, "--help", "--locale", "fr");
        test.help("help");
        final String result = test.outputBuffer.toString();
        assertTrue(result, result.contains("Affiche un écran d’aide."));
        assertTrue(result, result.contains("Les paramètres régionaux"));
    }
}
