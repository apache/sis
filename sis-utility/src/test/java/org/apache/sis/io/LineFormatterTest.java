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

import java.util.List;
import java.util.Arrays;
import java.io.IOException;
import org.apache.sis.test.DependsOn;
import org.apache.sis.internal.util.X364;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;
import static org.apache.sis.util.Characters.SOFT_HYPHEN;


/**
 * Tests {@link LineFormatter} implementation. This class will run the {@link FormatterTestCase}
 * test methods many time, testing each time a different {@code LineFormatter} aspect identified
 * by the {@link Aspect} enumeration.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
@RunWith(Parameterized.class)
@DependsOn({
  org.apache.sis.util.CharSequencesTest.class,
  org.apache.sis.internal.util.X364Test.class
})
public final strictfp class LineFormatterTest extends FormatterTestCase {
    /**
     * Returns all {@link LineFormatter} aspects to test. Each element of the returned list is
     * the list of arguments given by JUnit to the {@linkplain #LineFormatterTest constructor}.
     *
     * @return All aspects to test.
     */
    @Parameterized.Parameters
    public static List<Aspect[]> aspects() {
        final Aspect[] aspects = Aspect.values();
        final Aspect[][] args = new Aspect[aspects.length][];
        for (int i=0; i<aspects.length; i++) {
            args[i] = new Aspect[] {aspects[i]};
        }
        return Arrays.asList(args);
    }

    /**
     * The {@link LineFormatter} aspect being tested.
     */
    private final Aspect aspect;

    /**
     * Creates a new test which will test the given aspect of {@link LineFormatter}.
     *
     * @param aspect The aspect to test.
     */
    public LineFormatterTest(final Aspect aspect) {
        this.aspect = aspect;
        formatter = aspect.create(formatter);
    }

    /**
     * Runs the test.
     *
     * @param  lineSeparator The line separator to use in the test strings.
     * @throws IOException Should never happen, since we are writing in a {@link StringBuilder}.
     */
    @Override
    void run(final String lineSeparator) throws IOException {
        final Appendable out = formatter;
        if (out instanceof LineFormatter) {
            aspect.preCondition((LineFormatter) out);
        }
        assertOutputEquals(aspect.run(out, lineSeparator));
    }

    /**
     * The {@link LineFormatter} aspects to test.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3 (derived from geotk-3.00)
     * @version 0.3
     * @module
     */
    public static enum Aspect {
        /**
         * Tests the {@link LineFormatter} implementation
         * when used for changing the line separator.
         *
         * @see LineFormatter#setLineSeparator(String)
         */
        LINE_SEPARATOR {
            @Override
            LineFormatter create(final Appendable out) {
                return new LineFormatter(out, " ", false);
            }

            @Override
            void preCondition(LineFormatter out) {
                assertEquals("getLineSeparator", " ", out.getLineSeparator());
            };

            @Override
            String run(final Appendable out, final String lineSeparator) throws IOException {
                assertSame(out, out.append("Le vrai" + lineSeparator + "policitien, "));
                assertSame(out, out.append("c'est celui\r\nqui\r")); // Line separator broken on two method calls.
                assertSame(out, out.append("\narrive à garder " + lineSeparator));
                assertSame(out, out.append("son\ridéal   " + lineSeparator + 't'));
                assertSame(out, out.append("out en perdant"));
                assertSame(out, out.append(lineSeparator + "ses illusions."));
                return "Le vrai policitien, c'est celui qui arrive à garder son idéal tout en perdant ses illusions.";
            }
        },

        /**
         * Tests the {@link LineFormatter} implementation
         * when used for expanding tabulations to spaces.
         *
         * @see LineFormatter#setTabulationExpanded(boolean)
         */
        TABULATION {
            @Override
            LineFormatter create(final Appendable out) {
                return new LineFormatter(out, null, true);
            }

            @Override
            void preCondition(LineFormatter out) {
                assertEquals("getTabWidth", 8, out.getTabulationWidth());
            };

            @Override
            String run(final Appendable out, final String lineSeparator) throws IOException {
                assertSame(out, out.append("12\t8"   + lineSeparator));
                assertSame(out, out.append("1234\t8" + lineSeparator));
                assertSame(out, out.append("A tabulation\tin the middle."));
                return "12      8" + lineSeparator
                     + "1234    8" + lineSeparator
                     + "A tabulation    in the middle.";
            }
        },

        /**
         * Tests the {@link LineFormatter} implementation
         * when used for wrapping lines to 80 characters.
         *
         * @see LineFormatter#setMaximalLineLength(int)
         */
        WORD_WRAP {
            @Override
            LineFormatter create(final Appendable out) {
                return new LineFormatter(out, 10, false);
            }

            @Override
            void preCondition(LineFormatter out) {
                assertEquals("getMaximalLineLength", 10, out.getMaximalLineLength());
            };

            /*
             * Extract from Émile Nelligan (1879-1941) with soft hyphen and X3.64 sequences added.
             */
            @Override
            String run(final Appendable out, final String lineSeparator) throws IOException {
                final String BLUE    = X364.FOREGROUND_BLUE   .sequence();
                final String DEFAULT = X364.FOREGROUND_DEFAULT.sequence();
                assertSame(out, out.append("Ah! comme la " + BLUE + "neige" + DEFAULT + " a neigé!" + lineSeparator));
                assertSame(out, out.append("Ma vitre est un jar" + SOFT_HYPHEN + "din de givre."    + lineSeparator));
                /*
                 * If our test case is using the wrapper which will send the data once character at time,
                 * our LineFormatter implementation will not be able to detect the line separator and
                 * will fallback on the default one. So we set the line separator to the one actually used
                 * not because this is the class contract (quite the opposite, this is a limitation in our
                 * implementation), but simply in order to allow the test to pass.
                 */
                String insertedLineSeparator = lineSeparator;
                if (out instanceof SingleCharAppendable) {
                    insertedLineSeparator = System.lineSeparator();
                }
                return "Ah! comme"                             + insertedLineSeparator
                     + "la " + BLUE + "neige" + DEFAULT + " a" + insertedLineSeparator
                     + "neigé!"                                +         lineSeparator
                     + "Ma vitre"                              + insertedLineSeparator
                     + "est un jar" + SOFT_HYPHEN              + insertedLineSeparator
                     + "din de"                                + insertedLineSeparator
                     + "givre."                                +         lineSeparator;
            }
        },

        /**
         * Tests the {@link LineFormatter} implementation
         * when used for inserting a margin before every line.
         *
         * @see LineFormatter#onLineBegin(boolean)
         */
        ON_LINE_BEGIN {
            @Override
            LineFormatter create(final Appendable out) {
                return new LineFormatter(out) {
                    @Override
                    protected void onLineBegin(boolean isContinuation) throws IOException {
                        out.append("    ");
                    }
                };
            }

            /*
             * Extract from Arthur RIMBAUD (1854-1891), "Le bateau ivre"
             */
            @Override
            String run(final Appendable out, final String lineSeparator) throws IOException {
                assertSame(out, out.append("Comme je descendais des Fleuves impassibles,\r"
                                         + "Je ne me sentis plus guidé par les haleurs :\n"));
                assertSame(out, out.append("Des Peaux-Rouges criards les avaient pris pour cibles,\r\n"));
                assertSame(out, out.append("Les ayant cloués nus "));
                assertSame(out, out.append("aux poteaux de couleurs." + lineSeparator));

                return "    Comme je descendais des Fleuves impassibles,"           + '\r'
                     + "    Je ne me sentis plus guidé par les haleurs :"           + '\n'
                     + "    Des Peaux-Rouges criards les avaient pris pour cibles," + "\r\n"
                     + "    Les ayant cloués nus aux poteaux de couleurs."          + lineSeparator;
            }
        };

        /**
         * Creates and configure the {@link LineFormatter} for the aspect to test.
         */
        abstract LineFormatter create(Appendable out);

        /**
         * Optionally test pre-conditions before to run the test.
         */
        void preCondition(LineFormatter formatter) {};

        /**
         * Appends string in the given buffer and returns the expected result.
         *
         * @param  out Where to append the string.
         * @param  lineSeparator The line separator to insert in the test string.
         * @return The expected result.
         */
        abstract String run(Appendable out, String lineSeparator) throws IOException;
    };
}
