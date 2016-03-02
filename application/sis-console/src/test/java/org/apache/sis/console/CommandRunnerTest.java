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

import java.util.EnumSet;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.StandardCharsets;


/**
 * Tests the {@link CommandRunner} base class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class CommandRunnerTest extends TestCase {
    /**
     * A dummy sub-command for testing purpose.
     */
    private static final class Dummy extends CommandRunner {
        /**
         * Creates a new sub-command for the given arguments and option values.
         *
         * @param  validOptions The valid options.
         * @param  arguments The test arguments.
         */
        Dummy(final EnumSet<Option> validOptions, final String... arguments) throws InvalidOptionException {
            super(0, arguments, validOptions);
        }

        /**
         * Do nothing, since this method is not of interest for the enclosing JUnit test.
         */
        @Override
        public int run() {
            return 0;
        }
    }

    /**
     * Tests the {@code --locale} option.
     *
     * @throws InvalidOptionException Should never happen.
     */
    @Test
    public void testLocale() throws InvalidOptionException {
        final CommandRunner c = new Dummy(EnumSet.allOf(Option.class), CommandRunner.TEST, "--locale", "ja");
        assertEquals(Option.LOCALE, getSingleton(c.options.keySet()));
        assertSame("locale", Locale.JAPANESE, c.locale);
        assertTrue("files.isEmpty()", c.files.isEmpty());
    }

    /**
     * Tests the {@code --timezone} option.
     *
     * @throws InvalidOptionException Should never happen.
     */
    @Test
    public void testTimeZone() throws InvalidOptionException {
        final CommandRunner c = new Dummy(EnumSet.allOf(Option.class), CommandRunner.TEST, "--timezone", "JST");
        assertEquals(Option.TIMEZONE, getSingleton(c.options.keySet()));
        assertEquals("timezone", TimeZone.getTimeZone("JST"), c.timezone);
        assertEquals("rawoffset", TimeUnit.HOURS.toMillis(9), c.timezone.getRawOffset());
        assertTrue("files.isEmpty()", c.files.isEmpty());
    }

    /**
     * Tests the {@code --encoding} option.
     *
     * @throws InvalidOptionException Should never happen.
     */
    @Test
    public void testEncoding() throws InvalidOptionException {
        final CommandRunner c = new Dummy(EnumSet.allOf(Option.class), CommandRunner.TEST, "--encoding", "UTF-16");
        assertEquals(Option.ENCODING, getSingleton(c.options.keySet()));
        assertEquals("encoding", StandardCharsets.UTF_16, c.encoding);
        assertTrue("files.isEmpty()", c.files.isEmpty());
    }

    /**
     * Tests passing a mix of different legal options.
     *
     * @throws InvalidOptionException Should never happen.
     */
    @Test
    @DependsOnMethod({"testLocale", "testTimeZone", "testEncoding"})
    public void testOptionMix() throws InvalidOptionException {
        final CommandRunner c = new Dummy(EnumSet.allOf(Option.class), CommandRunner.TEST,
                "--brief", "--locale", "ja", "--verbose", "--timezone", "JST");
        assertEquals("options", EnumSet.of(
                Option.BRIEF, Option.LOCALE, Option.VERBOSE, Option.TIMEZONE), c.options.keySet());

        // Test specific values.
        assertSame  ("locale",   Locale.JAPANESE,             c.locale);
        assertEquals("timezone", TimeZone.getTimeZone("JST"), c.timezone);
        assertTrue("files.isEmpty()", c.files.isEmpty());
    }

    /**
     * Tests passing an option with a missing value.
     *
     * @throws InvalidOptionException Should never happen.
     */
    @Test
    @DependsOnMethod("testLocale")
    public void testMissingOptionValue() throws InvalidOptionException {
        final CommandRunner c = new Dummy(EnumSet.allOf(Option.class), CommandRunner.TEST, "--brief"); // Should not comply.
        assertEquals(Option.BRIEF, getSingleton(c.options.keySet()));
        try {
            new Dummy(EnumSet.allOf(Option.class), CommandRunner.TEST, "--brief", "--locale");
            fail("Expected InvalidOptionException");
        } catch (InvalidOptionException e) {
            final String message = e.getMessage();
            assertTrue(message.contains("locale"));
        }
    }

    /**
     * Tests passing an unexpected option.
     *
     * @throws InvalidOptionException Should never happen.
     */
    @Test
    public void testUnexpectedOption() throws InvalidOptionException {
        try {
            new Dummy(EnumSet.of(Option.HELP, Option.BRIEF), CommandRunner.TEST, "--brief", "--verbose", "--help");
            fail("Expected InvalidOptionException");
        } catch (InvalidOptionException e) {
            final String message = e.getMessage();
            assertTrue(message.contains("verbose"));
        }
    }

    /**
     * Tests {@link CommandRunner#hasContradictoryOptions(Option[])}.
     *
     * @throws InvalidOptionException Should never happen.
     */
    @Test
    public void testHasContradictoryOptions() throws InvalidOptionException {
        final CommandRunner c = new Dummy(EnumSet.allOf(Option.class), CommandRunner.TEST, "--brief", "--verbose");
        assertTrue(c.hasContradictoryOptions(Option.BRIEF, Option.VERBOSE));
        final String message = c.outputBuffer.toString();
        assertTrue(message.contains("brief"));
        assertTrue(message.contains("verbose"));
    }

    /**
     * Tests {@link CommandRunner#hasUnexpectedFileCount(int, int)}.
     *
     * @throws InvalidOptionException Should never happen.
     */
    @Test
    public void testHasUnexpectedFileCount() throws InvalidOptionException {
        final CommandRunner c = new Dummy(EnumSet.allOf(Option.class), CommandRunner.TEST, "MyFile.txt");
        assertFalse(c.hasUnexpectedFileCount(0, 1));
        assertEquals("", c.outputBuffer.toString());
        assertFalse(c.hasUnexpectedFileCount(1, 2));
        assertEquals("", c.outputBuffer.toString());
        assertTrue(c.hasUnexpectedFileCount(2, 3));
        final String message = c.outputBuffer.toString();
        assertTrue(message.length() != 0);
    }
}
