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


/**
 * Tests the {@link EndOfLineFormatter} implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
public final strictfp class EndOfLineFormatterTest extends FormatterTestCase {
    /**
     * Creates a new test case.
     */
    public EndOfLineFormatterTest() {
        formatter = new EndOfLineFormatter(formatter, " ");
    }

    /**
     * A simple test using the given line separator.
     */
    @Override
    void run(final String lineSeparator) throws IOException {
        final Appendable out = formatter;
        if (out instanceof EndOfLineFormatter) {
            assertEquals("getLineSeparator", " ", ((EndOfLineFormatter) out).getLineSeparator());
        }
        assertSame(out, out.append("Le vrai" + lineSeparator + "policitien, "));
        assertSame(out, out.append("c'est celui\r\nqui\r")); // Line separator broken on two method calls.
        assertSame(out, out.append("\narrive à garder " + lineSeparator));
        assertSame(out, out.append("son\ridéal   " + lineSeparator + 't'));
        assertSame(out, out.append("out en perdant"));
        assertSame(out, out.append(lineSeparator + "ses illusions."));

        assertOutputEquals("Le vrai policitien, c'est celui qui arrive à garder son idéal "
                         + "tout en perdant ses illusions.");
    }
}
