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
import org.apache.sis.util.Characters;
import org.apache.sis.util.internal.shared.X364;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link LineAppender} implementation when used for wrapping lines to 80 characters.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see LineAppender#setMaximalLineLength(int)
 */
public class WordWrapTest extends LineAppenderTest {
    /**
     * The line length used in the tests.
     */
    static final int LINE_LENGTH = 11;

    /**
     * Creates a new test case.
     */
    public WordWrapTest() {
    }

    /**
     * Creates and configure the {@link LineAppender} to test.
     */
    @Override
    @BeforeEach
    public void createLineAppender() {
        appender = new LineAppender(appender, 11, false);
    }

    /**
     * Returns the line separator expected by this test. The default implementation returns
     * the given line separator unchanged. The {@link WordWrapWithLineSeparatorTest} subclass
     * override this method with a hard-coded line separator.
     */
    String expectedLineSeparator(final String lineSeparator) {
        return lineSeparator;
    }

    /**
     * Runs the test using an extract from Émile Nelligan (1879-1941)
     * with soft hyphen and X3.64 sequences added.
     *
     * @param  lineSeparator  the line separator to use in the test strings.
     * @throws IOException should never happen, since we are writing in a {@link StringBuilder}.
     */
    @Override
    void run(final String lineSeparator) throws IOException {
        final Appendable f = appender;
        if (f instanceof LineAppender la) {
            assertEquals(LINE_LENGTH, la.getMaximalLineLength());
        }
        final String BLUE    = X364.FOREGROUND_BLUE   .sequence();
        final String DEFAULT = X364.FOREGROUND_DEFAULT.sequence();
        assertSame(f, f.append("Ah! comme la " + BLUE + "neige" + DEFAULT + " a neigé!" + lineSeparator));
        assertSame(f, f.append("Ma vitre est un jar" + Characters.SOFT_HYPHEN + "din de givre." + lineSeparator));
        /*
         * If our test case is using the wrapper which will send the data once character at time,
         * our LineAppender implementation will not be able to detect the line separator and
         * will fallback on the default one. So we set the line separator to the one actually used
         * not because this is the class contract (quite the opposite, this is a limitation in our
         * implementation), but simply in order to allow the test to pass.
         */
        String insertedLineSeparator = lineSeparator;
        if (f instanceof SingleCharAppendable) {
            insertedLineSeparator = System.lineSeparator();
        }
        insertedLineSeparator = expectedLineSeparator(insertedLineSeparator);
        final String expectedLineSeparator = expectedLineSeparator(lineSeparator);
        assertOutputEquals("Ah! comme"                             + insertedLineSeparator
                         + "la " + BLUE + "neige" + DEFAULT + " a" + insertedLineSeparator
                         + "neigé!"                                + expectedLineSeparator
                         + "Ma vitre"                              + insertedLineSeparator
                         + "est un jar" + Characters.HYPHEN        + insertedLineSeparator
                         + "din de"                                + insertedLineSeparator
                         + "givre."                                + expectedLineSeparator);
    }

    /**
     * Test splitting a long lines into shorter lines.
     *
     * @throws IOException should never happen, since we are writing in a {@link StringBuilder}.
     */
    @Test
    public void testLineSplit() throws IOException {
        final LineAppender f = (LineAppender) appender;
        final String ls = expectedLineSeparator("\n");
        f.setLineSeparator("\n");
        f.setMaximalLineLength(LINE_LENGTH);
        f.append("bar foo-biz bla\nThisLineIsTooLong");
        assertOutputEquals("bar foo-" + ls + "biz bla" + ls + "ThisLineIsT" + ls + "ooLong");
    }
}
