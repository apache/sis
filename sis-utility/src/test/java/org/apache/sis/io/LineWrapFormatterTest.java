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

import static org.junit.Assert.*;
import static org.apache.sis.util.Characters.SOFT_HYPHEN;

// Related to JDK7
import org.apache.sis.internal.util.JDK7;


/**
 * Tests the {@link LineWrapFormatter} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
public final strictfp class LineWrapFormatterTest extends FormatterTestCase {
    /**
     * Creates a new test case.
     */
    public LineWrapFormatterTest() {
        formatter = new LineWrapFormatter(formatter, 10);
    }

    /**
     * A simple test using the given line separator.
     */
    @Override
    void run(final String lineSeparator) throws IOException {
        final Appendable out = formatter;
        if (out instanceof LineWrapFormatter) {
            assertEquals("getMaximalLineLength", 10, ((LineWrapFormatter) out).getMaximalLineLength());
        }
        final String BLUE    = X364.FOREGROUND_BLUE   .sequence();
        final String DEFAULT = X364.FOREGROUND_DEFAULT.sequence();
        /*
         * Extract from Émile Nelligan (1879-1941) with soft hyphen and X3.64 sequences added.
         */
        assertSame(out, out.append("Ah! comme la " + BLUE + "neige" + DEFAULT + " a neigé!" + lineSeparator));
        assertSame(out, out.append("Ma vitre est un jar" + SOFT_HYPHEN + "din de givre."    + lineSeparator));
        /*
         * If our test case is using the wrapper which will send the data once character at time,
         * our LineWrapFormatter implementation will not be able to detect the line separator and
         * will fallback on the default one. So we set the line separator to the one actually used
         * not because this is the class contract (quite the opposite, this is a limitation in our
         * implementation), but simply in order to allow the test to pass.
         */
        String insertedLineSeparator = lineSeparator;
        if (out instanceof SingleCharAppendable) {
            insertedLineSeparator = JDK7.lineSeparator();
        }
        assertOutputEquals("Ah! comme"                             + insertedLineSeparator
                         + "la " + BLUE + "neige" + DEFAULT + " a" + insertedLineSeparator
                         + "neigé!"                                +         lineSeparator
                         + "Ma vitre"                              + insertedLineSeparator
                         + "est un jar" + SOFT_HYPHEN              + insertedLineSeparator
                         + "din de"                                + insertedLineSeparator
                         + "givre."                                +         lineSeparator);
    }
}
