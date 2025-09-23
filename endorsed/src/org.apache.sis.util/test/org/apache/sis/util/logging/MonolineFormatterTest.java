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
package org.apache.sis.util.logging;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.internal.shared.Strings;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.api.parallel.ResourceLock;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertMultilinesEquals;


/**
 * Tests the {@link MonolineFormatter} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class MonolineFormatterTest extends TestCase {
    /**
     * The formatter to be tested.
     */
    private final MonolineFormatter formatter = new MonolineFormatter(null);

    /**
     * Creates a new test case.
     */
    public MonolineFormatterTest() {
    }

    /**
     * Tests {@link MonolineFormatter#levelWidth(Level)}.
     */
    @Test
    public void testlevelWidth() {
        final String severe = Level.SEVERE.getLocalizedName();
        assertEquals(severe.length(), MonolineFormatter.levelWidth(Level.SEVERE), severe);

        final String warning = Level.WARNING.getLocalizedName();
        assertEquals(StrictMath.max(severe.length(), warning.length()),
                MonolineFormatter.levelWidth(Level.WARNING), warning);
    }

    /**
     * Formats the given expected string to a format matching the current locale setting.
     * The given string shall use tabulation before each line of the message.
     */
    private static String localize(final Level level, final String expected) {
        final String levelToReplace = level.getName();
        final String levelLocalized = level.getLocalizedName();
        assertTrue(expected.startsWith(levelToReplace), expected);
        final int margin = MonolineFormatter.levelWidth(null);
        final StringBuilder buffer = new StringBuilder(expected.length() + 40)
                .append(levelLocalized)
                .append(CharSequences.spaces(margin - levelLocalized.length()))
                .append(expected, levelToReplace.length() + 1, expected.length());      // +1 is for skipping '\t'.
        final String spaces = Strings.CONTINUATION_MARK
                            + CharSequences.spaces(margin - 1).toString();
        int positionOfLast = -1;
        for (int i=margin; (i=buffer.indexOf("\n\t", i)) >= 0; i += margin) {
            buffer.replace(positionOfLast = ++i, i+1, spaces);                          // Replace only tabulation, leave new line.
        }
        if (positionOfLast >= 0) {
            buffer.setCharAt(positionOfLast, Strings.CONTINUATION_END);
        }
        return buffer.toString();
    }

    /**
     * Tests formatting of a multi-line message.
     */
    @Test
    @ResourceLock(Resources.LOCALE)
    public void testMultilines() {
        final LogRecord record = new LogRecord(Level.INFO, "First line\n  Indented line\nLast line\n");
        final String formatted = formatter.format(record);
        assertMultilinesEquals(localize(Level.INFO,
                "INFO\t First line\n" +
                    "\t   Indented line\n" +
                    "\t Last line\n"), formatted);
    }

    /**
     * Tests formatting a log record which contains an exception.
     */
    @Test
    @ResourceLock(Resources.LOCALE)
    public void testException() {
        final LogRecord record = new LogRecord(Level.WARNING, "An exception occured.");
        final Exception exception = new Exception();
        exception.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("org.apache.sis.NonExistent", "foo",  "NonExistent.java", 10),
            new StackTraceElement("org.junit.WhoKnows",         "main", "WhoKnows.java",    20)
        });
        record.setThrown(exception);
        String formatted = formatter.format(record);
        assertMultilinesEquals(localize(Level.WARNING,
                "WARNING\t An exception occured.\n" +
                       "\t Caused by: java.lang.Exception\n" +
                       "\t     at org.apache.sis.NonExistent.foo(NonExistent.java:10)\n" +
                       "\t     at org.junit.WhoKnows.main(WhoKnows.java:20)\n"), formatted);
        /*
         * Remove the message and try again.
         */
        record.setMessage(null);
        formatted = formatter.format(record);
        assertMultilinesEquals(localize(Level.WARNING,
                "WARNING\t java.lang.Exception\n" +
                       "\t     at org.apache.sis.NonExistent.foo(NonExistent.java:10)\n" +
                       "\t     at org.junit.WhoKnows.main(WhoKnows.java:20)\n"), formatted);
    }
}
