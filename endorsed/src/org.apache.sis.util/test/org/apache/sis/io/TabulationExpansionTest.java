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

// Test dependencies
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link LineAppender} implementation when used for expanding tabulations to spaces.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see LineAppender#setTabulationExpanded(boolean)
 */
public final class TabulationExpansionTest extends LineAppenderTest {
    /**
     * Creates a new test case.
     */
    public TabulationExpansionTest() {
    }

    /**
     * Creates and configure the {@link LineAppender} to test.
     */
    @Override
    @BeforeEach
    public void createLineAppender() {
        appender = new LineAppender(appender, null, true);
    }

    /**
     * Runs the test.
     *
     * @param  lineSeparator  the line separator to use in the test strings.
     * @throws IOException should never happen, since we are writing in a {@link StringBuilder}.
     */
    @Override
    void run(final String lineSeparator) throws IOException {
        final Appendable f = appender;
        if (f instanceof LineAppender la) {
            assertEquals(8, la.getTabulationWidth());
        }
        assertSame(f, f.append("12\t8"   + lineSeparator));
        assertSame(f, f.append("1234\t8" + lineSeparator));
        assertSame(f, f.append("A tabulation\tin the middle."));
        assertOutputEquals("12      8" + lineSeparator
                         + "1234    8" + lineSeparator
                         + "A tabulation    in the middle.");
    }
}
