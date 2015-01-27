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

import static org.junit.Assert.*;


/**
 * Tests {@link LineAppender} implementation when used for inserting a margin before every line.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see LineAppender#onLineBegin(boolean)
 */
@DependsOn(LineAppenderTest.class)
public final strictfp class LeftMarginTest extends LineAppenderTest {
    /**
     * Creates and configure the {@link LineAppender} to test.
     */
    @Before
    @Override
    public void createLineAppender() {
        appender = new LineAppender(appender) {
            @Override
            protected void onLineBegin(boolean isContinuation) throws IOException {
                out.append("    ");
            }
        };
    }

    /**
     * Runs the test using an extract from Arthur RIMBAUD (1854-1891), "Le bateau ivre".
     *
     * @param  lineSeparator The line separator to use in the test strings.
     * @throws IOException Should never happen, since we are writing in a {@link StringBuilder}.
     */
    @Override
    void run(final String lineSeparator) throws IOException {
        final Appendable f = appender;
        assertSame(f, f.append("Comme je descendais des Fleuves impassibles,\r"
                             + "Je ne me sentis plus guidé par les haleurs :\n"));
        assertSame(f, f.append("Des Peaux-Rouges criards les avaient pris pour cibles,\r\n"));
        assertSame(f, f.append("Les ayant cloués nus "));
        assertSame(f, f.append("aux poteaux de couleurs." + lineSeparator));

        assertOutputEquals("    Comme je descendais des Fleuves impassibles,"           + '\r'
                         + "    Je ne me sentis plus guidé par les haleurs :"           + '\n'
                         + "    Des Peaux-Rouges criards les avaient pris pour cibles," + "\r\n"
                         + "    Les ayant cloués nus aux poteaux de couleurs."          + lineSeparator);
    }
}
