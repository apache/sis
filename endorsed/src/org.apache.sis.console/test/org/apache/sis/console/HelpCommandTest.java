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

import java.io.IOException;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link HelpCommand} sub-command.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class HelpCommandTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public HelpCommandTest() {
    }

    /**
     * Tests the sub-command without option.
     *
     * @throws InvalidOptionException should never happen.
     * @throws IOException should never happen, because we are writing to a {@code PrintWriter}.
     */
    @Test
    public void testDefault() throws InvalidOptionException, IOException {
        var test = new HelpCommand(0, new String[] {CommandRunner.TEST});
        test.run();
        String result = test.outputBuffer.toString();
        assertTrue(result.startsWith("Apache SIS"));
        assertTrue(result.contains("--locale"));
        assertTrue(result.contains("--encoding"));
        assertTrue(result.contains("--timezone"));
        assertTrue(result.contains("--brief"));
        assertTrue(result.contains("--verbose"));
        assertTrue(result.contains("--help"));
    }

    /**
     * Tests the sub-command with the {@code --help} option.
     * Shall contain only a subset of {@link #testDefault()}.
     *
     * @throws InvalidOptionException should never happen.
     * @throws IOException should never happen, because we are writing to a {@code PrintWriter}.
     */
    @Test
    public void testHelp() throws InvalidOptionException, IOException {
        var test = new HelpCommand(0, new String[] {CommandRunner.TEST, "--help"});
        test.help("help");
        String result = test.outputBuffer.toString();
        assertTrue (result.startsWith("help"));
        assertTrue (result.contains("--locale"));
        assertTrue (result.contains("--encoding"));
        assertFalse(result.contains("--timezone"));
        assertFalse(result.contains("--brief"));
        assertFalse(result.contains("--verbose"));
        assertTrue (result.contains("--help"));
    }

    /**
     * Tests the sub-command with the {@code --locale en} option.
     *
     * @throws InvalidOptionException should never happen.
     * @throws IOException should never happen, because we are writing to a {@code PrintWriter}.
     */
    @Test
    public void testEnglishLocale() throws InvalidOptionException, IOException {
        var test = new HelpCommand(0, new String[] {CommandRunner.TEST, "--help", "--locale", "en"});
        test.help("help");
        String result = test.outputBuffer.toString();
        assertTrue(result.contains("Show a help overview."));
        assertTrue(result.contains("The locale to use"));
    }

    /**
     * Tests the sub-command with the {@code --locale fr} option.
     *
     * @throws InvalidOptionException should never happen.
     * @throws IOException should never happen, because we are writing to a {@code PrintWriter}.
     */
    @Test
    public void testFrenchLocale() throws InvalidOptionException, IOException {
        var test = new HelpCommand(0, new String[] {CommandRunner.TEST, "--help", "--locale", "fr"});
        test.help("help");
        String result = test.outputBuffer.toString();
        assertTrue(result.contains("Affiche un écran d’aide."));
        assertTrue(result.contains("Les paramètres régionaux"));
    }
}
