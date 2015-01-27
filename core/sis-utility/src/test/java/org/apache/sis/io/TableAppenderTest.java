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
package org.apache.sis.io;

import java.io.IOException;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.junit.Assert.*;

// Related to JK7
import org.apache.sis.internal.jdk7.JDK7;


/**
 * Tests {@link TableAppender} implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn({
  org.apache.sis.util.CharSequencesTest.class,
  org.apache.sis.internal.util.X364Test.class,
  LineAppenderTest.class})
public final strictfp class TableAppenderTest extends AppenderTestCase {
    /**
     * The table appender to test. May not be same instance than {@link #appender},
     * because the super-class will wraps it in a {@link SingleCharAppendable} in
     * some occasions.
     */
    private final TableAppender table;

    /**
     * Creates a new test case.
     */
    public TableAppenderTest() {
        appender = table = new TableAppender(appender);
    }

    /**
     * A simple test using the given line separator.
     */
    @Override
    void run(String lineSeparator) throws IOException {
        final Appendable out = appender;
        table.nextLine('═');

        // r.e.d. = Equatorial diameter Measured relative to the Earth.
        // Source: "Planet" on wikipedia on July 25, 2008.
        assertSame(out, out.append("English\tFrench\tr.e.d." + lineSeparator));
        table.appendHorizontalSeparator();
        assertSame(out, out.append("Mercury\tMercure\t0.382" + lineSeparator));
        assertSame(out, out.append("Venus\tVénus\t0.949"     + lineSeparator));
        assertSame(out, out.append("Earth\tTerre"));
        assertSame(out, out.append("\t1.00"                  + lineSeparator));
        assertSame(out, out.append("Mars\tMa"));
        assertSame(out, out.append("rs\t0.532"               + lineSeparator));
        assertSame(out, out.append("Jupiter\tJupiter\t11.209"));
        assertSame(out, out.append(lineSeparator));
        assertSame(out, out.append("Saturn"));
        assertSame(out, out.append("\tSaturne\t"));
        assertSame(out, out.append("9.449"                   + lineSeparator));
        assertSame(out, out.append("Uranus\tUranus\t4.007"   + lineSeparator
                                 + "Neptune\tNeptune\t3.883" + lineSeparator));
        table.nextLine('═');
        /*
         * If our test case is using the wrapper which will send the data once character at time,
         * our TableAppender implementation will not be able to detect the line separator and
         * will fallback on the default one. So we set the line separator to the one actually used
         * not because this is the class contract (quite the opposite, this is a limitation in our
         * implementation), but simply in order to allow the test to pass.
         */
        if (out instanceof SingleCharAppendable) {
            lineSeparator = JDK7.lineSeparator();
        }
        assertOutputEquals(
                  "╔═════════╤═════════╤════════╗" + lineSeparator
                + "║ English │ French  │ r.e.d. ║" + lineSeparator
                + "╟─────────┼─────────┼────────╢" + lineSeparator
                + "║ Mercury │ Mercure │ 0.382  ║" + lineSeparator
                + "║ Venus   │ Vénus   │ 0.949  ║" + lineSeparator
                + "║ Earth   │ Terre   │ 1.00   ║" + lineSeparator
                + "║ Mars    │ Mars    │ 0.532  ║" + lineSeparator
                + "║ Jupiter │ Jupiter │ 11.209 ║" + lineSeparator
                + "║ Saturn  │ Saturne │ 9.449  ║" + lineSeparator
                + "║ Uranus  │ Uranus  │ 4.007  ║" + lineSeparator
                + "║ Neptune │ Neptune │ 3.883  ║" + lineSeparator
                + "╚═════════╧═════════╧════════╝" + lineSeparator);
    }

    /**
     * Tests the {@link TableAppender#toString()} method.
     * The intend of this test is also to ensure that we can use the API
     * more easily, without having to deal with {@link IOException}.
     */
    @Test
    public void testToString() { // NO throws IOException
        /*
         * First, ensure that TableAppender.toString() does not
         * mess with the content of user-supplied Appendable.
         */
        testToString(table, "");
        /*
         * When TableAppender is created with its own internal buffer,
         * then TableAppender.toString() is allowed to format the table.
         */
        testToString(new TableAppender(),
                "╔═════════╤═════════╤════════╗\n"
              + "║ English │ French  │ r.e.d. ║\n"
              + "╟─────────┼─────────┼────────╢\n"
              + "║ Mercury │ Mercure │ 0.382  ║\n"
              + "║ Venus   │ Vénus   │ 0.949  ║\n"
              + "║ Earth   │ Terre   │ 1.00   ║\n"
              + "╚═════════╧═════════╧════════╝\n");
    }

    /**
     * Helper method for {@link #testToString()}.
     *
     * @param table    Where to format the table.
     * @param expected The expected string representation of the formatted table.
     */
    private static void testToString(final TableAppender table, final String expected) {
        table.nextLine('═');
        table.append("English\tFrench\tr.e.d.\n").appendHorizontalSeparator();
        table.append("Mercury\tMercure\t0.382\n")
             .append("Venus\tVénus\t0.949\n")
             .append("Earth\tTerre\t1.00\n")
             .nextLine('═');
        assertEquals(expected, table.toString());
    }
}
