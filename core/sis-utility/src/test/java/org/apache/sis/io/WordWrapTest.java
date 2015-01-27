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
import org.apache.sis.internal.util.X364;
import org.junit.Before;

import static org.junit.Assert.*;
import static org.apache.sis.util.Characters.SOFT_HYPHEN;

// Related to JK7
import org.apache.sis.internal.jdk7.JDK7;


/**
 * Tests {@link LineAppender} implementation when used for wrapping lines to 80 characters.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see LineAppender#setMaximalLineLength(int)
 */
@DependsOn(LineAppenderTest.class)
public strictfp class WordWrapTest extends LineAppenderTest {
    /**
     * Creates and configure the {@link LineAppender} to test.
     */
    @Before
    @Override
    public void createLineAppender() {
        appender = new LineAppender(appender, 10, false);
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
     * @param  lineSeparator The line separator to use in the test strings.
     * @throws IOException Should never happen, since we are writing in a {@link StringBuilder}.
     */
    @Override
    void run(final String lineSeparator) throws IOException {
        final Appendable f = appender;
        if (f instanceof LineAppender) {
            assertEquals("getMaximalLineLength", 10, ((LineAppender) f).getMaximalLineLength());
        }
        final String BLUE    = X364.FOREGROUND_BLUE   .sequence();
        final String DEFAULT = X364.FOREGROUND_DEFAULT.sequence();
        assertSame(f, f.append("Ah! comme la " + BLUE + "neige" + DEFAULT + " a neigé!" + lineSeparator));
        assertSame(f, f.append("Ma vitre est un jar" + SOFT_HYPHEN + "din de givre."    + lineSeparator));
        /*
         * If our test case is using the wrapper which will send the data once character at time,
         * our LineAppender implementation will not be able to detect the line separator and
         * will fallback on the default one. So we set the line separator to the one actually used
         * not because this is the class contract (quite the opposite, this is a limitation in our
         * implementation), but simply in order to allow the test to pass.
         */
        String insertedLineSeparator = lineSeparator;
        if (f instanceof SingleCharAppendable) {
            insertedLineSeparator = JDK7.lineSeparator();
        }
        insertedLineSeparator = expectedLineSeparator(insertedLineSeparator);
        final String expectedLineSeparator = expectedLineSeparator(lineSeparator);
        assertOutputEquals("Ah! comme"                             + insertedLineSeparator
                         + "la " + BLUE + "neige" + DEFAULT + " a" + insertedLineSeparator
                         + "neigé!"                                + expectedLineSeparator
                         + "Ma vitre"                              + insertedLineSeparator
                         + "est un jar" + SOFT_HYPHEN              + insertedLineSeparator
                         + "din de"                                + insertedLineSeparator
                         + "givre."                                + expectedLineSeparator);
    }
}
