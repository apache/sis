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
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests various aspects of {@link LineAppender}.
 * This base class tests {@code LineAppender} when used for changing the line separator,
 * which is a problematic involved in every tests. Subclasses will test other aspects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
@DependsOn({
  org.apache.sis.util.CharSequencesTest.class,
  org.apache.sis.internal.util.X364Test.class
})
public strictfp class LineAppenderTest extends AppenderTestCase {
    /**
     * Creates a new test. Subclasses shall override the {@link #createLineAppender()} method
     * in order to create the instance to test.
     */
    public LineAppenderTest() {
    }

    /**
     * Creates and configure the {@link LineAppender} to test.
     */
    @Before
    public void createLineAppender() {
        appender = new LineAppender(appender, " ", false);
    }

    /**
     * Runs the test.
     *
     * @param  lineSeparator The line separator to use in the test strings.
     * @throws IOException Should never happen, since we are writing in a {@link StringBuilder}.
     */
    @Override
    void run(final String lineSeparator) throws IOException {
        final Appendable f = appender;
        if (f instanceof LineAppender) {
            assertEquals("getLineSeparator", " ", ((LineAppender) f).getLineSeparator());
        }
        assertSame(f, f.append("Le vrai" + lineSeparator + "policitien, "));
        assertSame(f, f.append("c'est celui\r\nqui\r")); // Line separator broken on two method calls.
        assertSame(f, f.append("\narrive à garder " + lineSeparator));
        assertSame(f, f.append("son\ridéal   " + lineSeparator + 't'));
        assertSame(f, f.append("out en perdant"));
        assertSame(f, f.append(lineSeparator + "ses illusions."));
        assertOutputEquals("Le vrai policitien, c'est celui qui arrive à garder son idéal tout en perdant ses illusions.");
    }

    /**
     * Tests a call to {@link LineAppender#flush()} interleaved between two lines,
     * where the second line begin with a tabulation.
     *
     * @throws IOException Should never happen, since we are writing in a {@link StringBuilder}.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-140">SIS-140</a>
     */
    @Test
    public void testInterleavedFlush() throws IOException {
        final LineAppender f = (LineAppender) appender;
        f.setTabulationWidth(4);
        f.append("S1");
        f.flush();
        f.append("\tS2");
        assertOutputEquals(f.isTabulationExpanded() ? "S1  S2" : "S1\tS2");
    }
}
