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

import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertSingleton;


/**
 * Tests the {@link CommandRunner} base class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class CommandRunnerTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public CommandRunnerTest() {
    }

    /**
     * A dummy sub-command for testing purpose.
     */
    private static final class Dummy extends CommandRunner {
        /**
         * Creates a new sub-command for the given arguments and option values.
         *
         * @param  validOptions  the valid options.
         * @param  arguments     the test arguments.
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
     * @throws InvalidOptionException should never happen.
     */
    @Test
    public void testLocale() throws InvalidOptionException {
        final CommandRunner c = new Dummy(EnumSet.allOf(Option.class), CommandRunner.TEST, "--locale", "ja");
        assertEquals(Option.LOCALE, assertSingleton(c.options.keySet()));
        assertSame(Locale.JAPANESE, c.locale, "locale");
        assertTrue(c.files.isEmpty(), "files.isEmpty()");
    }

    /**
     * Tests the {@code --timezone} option.
     *
     * @throws InvalidOptionException should never happen.
     */
    @Test
    public void testTimeZone() throws InvalidOptionException {
        final CommandRunner c = new Dummy(EnumSet.allOf(Option.class), CommandRunner.TEST, "--timezone", "Asia/Tokyo");
        assertEquals(Option.TIMEZONE, assertSingleton(c.options.keySet()));
        assertEquals(TimeZone.getTimeZone("Asia/Tokyo"), c.getTimeZone(), "timezone");
        assertEquals(TimeUnit.HOURS.toMillis(9), c.getTimeZone().getRawOffset(), "rawoffset");
        assertTrue(c.files.isEmpty(), "files.isEmpty()");
    }

    /**
     * Tests the {@code --encoding} option.
     *
     * @throws InvalidOptionException should never happen.
     */
    @Test
    public void testEncoding() throws InvalidOptionException {
        final CommandRunner c = new Dummy(EnumSet.allOf(Option.class), CommandRunner.TEST, "--encoding", "UTF-16");
        assertEquals(Option.ENCODING, assertSingleton(c.options.keySet()));
        assertEquals(StandardCharsets.UTF_16, c.encoding, "encoding");
        assertTrue(c.files.isEmpty(), "files.isEmpty()");
    }

    /**
     * Tests passing a mix of different legal options.
     *
     * @throws InvalidOptionException should never happen.
     */
    @Test
    public void testOptionMix() throws InvalidOptionException {
        final CommandRunner c = new Dummy(EnumSet.allOf(Option.class), CommandRunner.TEST,
                "--brief", "--locale", "ja", "--verbose", "--timezone", "Asia/Tokyo");
        assertEquals(EnumSet.of(Option.BRIEF, Option.LOCALE, Option.VERBOSE, Option.TIMEZONE), c.options.keySet(), "options");

        // Test specific values.
        assertSame(Locale.JAPANESE, c.locale, "locale");
        assertEquals(ZoneId.of("Asia/Tokyo"), c.timezone, "timezone");
        assertTrue(c.files.isEmpty(), "files.isEmpty()");
    }

    /**
     * Tests passing an option with a missing value.
     *
     * @throws InvalidOptionException should never happen.
     */
    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testMissingOptionValue() throws InvalidOptionException {
        final CommandRunner c = new Dummy(EnumSet.allOf(Option.class), CommandRunner.TEST, "--brief"); // Should not comply.
        assertEquals(Option.BRIEF, assertSingleton(c.options.keySet()));
        var exception = assertThrows(InvalidOptionException.class,
                () -> new Dummy(EnumSet.allOf(Option.class), CommandRunner.TEST, "--brief", "--locale"));
        assertMessageContains(exception, "locale");
    }

    /**
     * Tests passing an unexpected option.
     *
     * @throws InvalidOptionException Should never happen.
     */
    @Test
    public void testUnexpectedOption() throws InvalidOptionException {
        var exception = assertThrows(InvalidOptionException.class,
                () -> new Dummy(EnumSet.of(Option.HELP, Option.BRIEF), CommandRunner.TEST, "--brief", "--verbose", "--help"));
        assertMessageContains(exception, "verbose");
    }

    /**
     * Tests {@link CommandRunner#hasContradictoryOptions(Option[])}.
     *
     * @throws InvalidOptionException Should never happen.
     */
    @Test
    public void testHasContradictoryOptions() throws InvalidOptionException {
        final var c = new Dummy(EnumSet.allOf(Option.class), CommandRunner.TEST, "--brief", "--verbose");
        assertTrue(c.hasContradictoryOptions(Option.BRIEF, Option.VERBOSE));
        String message = c.outputBuffer.toString();
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
        final var c = new Dummy(EnumSet.allOf(Option.class), CommandRunner.TEST, "MyFile.txt");
        assertFalse(c.hasUnexpectedFileCount(0, 1));
        assertEquals("", c.outputBuffer.toString());
        assertFalse(c.hasUnexpectedFileCount(1, 2));
        assertEquals("", c.outputBuffer.toString());
        assertTrue(c.hasUnexpectedFileCount(2, 3));
        String message = c.outputBuffer.toString();
        assertNotEquals(0, message.length());
    }
}
