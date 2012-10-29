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
import org.apache.sis.util.CharSequencesTest;

import static org.junit.Assert.*;


/**
 * Tests the {@link LineWrapFormatter} implementation
 * when used for expanding tabulations to spaces.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
@DependsOn(CharSequencesTest.class)
public final strictfp class ExpandedTabFormatterTest extends FormatterTestCase {
    /**
     * Creates a new test case.
     */
    public ExpandedTabFormatterTest() {
        formatter = new LineWrapFormatter(formatter, null, true);
    }

    /**
     * A simple test using the given line separator.
     */
    @Override
    void run(final String lineSeparator) throws IOException {
        final Appendable out = formatter;
        if (out instanceof LineWrapFormatter) {
            assertEquals("getTabWidth", 8, ((LineWrapFormatter) out).getTabulationWidth());
        }
        assertSame(out, out.append("12\t8"   + lineSeparator));
        assertSame(out, out.append("1234\t8" + lineSeparator));
        assertSame(out, out.append("A tabulation\tin the middle."));
        assertOutputEquals("12      8" + lineSeparator
                         + "1234    8" + lineSeparator
                         + "A tabulation    in the middle.");
    }
}
