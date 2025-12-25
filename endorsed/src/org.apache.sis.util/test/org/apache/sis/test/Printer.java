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
package org.apache.sis.test;

import java.io.Console;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.internal.shared.X364;


/**
 * Output writer where to print debugging information.
 * Texts sent to this printer will be shown only if a test fails, or if the tests are in verbose mode.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class Printer extends PrintWriter {
    /**
     * The system property for setting the output encoding.
     * If this property is not set, then the system encoding will be used.
     */
    static final String ENCODING_KEY = "org.apache.sis.test.encoding";

    /**
     * Width of the separator to print to {@link #out}, in number of characters.
     */
    private static final int SEPARATOR_WIDTH = 80;

    /**
     * The buffer where content as sent.
     */
    private final StringWriter buffer;

    /**
     * Creates a new printer.
     */
    Printer() {
        this(new StringWriter());
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="7", fixed="25")
    private Printer(final StringWriter buffer) {
        super(buffer);
        this.buffer = buffer;
    }

    /**
     * Prints the given title in a box.
     * This is used for writing a clear visual separator between the verbose output of different test cases.
     * Like everything printed with this printer, the box will be visible only if the test fails or if tests
     * are run in {@linkplain TestCase#verbose() verbose} mode.
     *
     * @param  title  the title to write.
     */
    public void printSeparator(final String title) {
        final boolean isAnsiSupported = X364.isAnsiSupported();
        if (isAnsiSupported) {
            print(X364.FOREGROUND_CYAN.sequence());
        }
        print('╒');
        for (int i = 0; i < SEPARATOR_WIDTH-2; i++) {
            print('═');
        }
        println('╕');
        print("│ ");
        print(title);
        for (int i = title.codePointCount(0, title.length()); i < SEPARATOR_WIDTH-3; i++) {
            print(' ');
        }
        println('│');
        print('└');
        for (int i=0; i < SEPARATOR_WIDTH-2; i++) {
            print('─');
        }
        println('┘');
        if (isAnsiSupported) {
            print(X364.FOREGROUND_DEFAULT.sequence());
        }
    }

    /**
     * Unconditionally prints the content sent to this printer, regardless if the tests are verbose.
     * The content will be sent to the {@linkplain System#console() console} if available,
     * or to the {@linkplain System#out standard output stream} otherwise.
     * Then, this method clears the stream buffer.
     *
     * <p>This method is invoked automatically by {@code TestRunner} and usually does not
     * need to be invoked explicitly. An exception to this rule is in methods annotated by
     * {@link org.junit.jupiter.api.AfterAll}.</p>
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void flushUnconditionally() {
        System.out.flush();
        System.err.flush();
        synchronized (buffer) {             // This is the lock used by the `PrintWriter`.
            flush();
            final String content = buffer.toString().stripTrailing();
            PrintWriter writer = null;
            final String encoding = System.getProperty(ENCODING_KEY);
            if (encoding == null) {
                final Console console = System.console();
                if (console != null) {
                    writer = console.writer();
                }
            }
            if (writer == null) {
                if (encoding != null) try {
                    writer = new PrintWriter(new OutputStreamWriter(System.out, encoding));
                } catch (UnsupportedEncodingException e) {
                    // Ignore. We will use the default encoding.
                }
                if (writer == null) {
                    writer = new PrintWriter(System.out);
                }
            }
            writer.println(content);
            writer.flush();
            buffer.getBuffer().setLength(0);
        }
    }

    /**
     * Invoked by {@code TestRunner} in order to clear the buffer before a new test begin.
     * This is necessary when the previous test succeeded and the {@link TestCase#VERBOSE}
     * flag is {@code false}, since the {@link #flushUnconditionally()} method has not been invoked
     * in such case.
     */
    void clearBuffer() {
        synchronized (buffer) {             // This is the lock used by the `PrintWriter`.
            flush();
            buffer.getBuffer().setLength(0);
        }
    }
}
