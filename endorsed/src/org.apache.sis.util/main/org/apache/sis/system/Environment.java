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
package org.apache.sis.system;

import java.io.Console;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import org.apache.sis.util.Workaround;
import org.apache.sis.pending.jdk.JDK17;


/**
 * Method related to the environment where Apache SIS is executed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class Environment {
    /**
     * Whether the use of the console writer should be avoided.
     *
     * @see #avoidConsoleWriter()
     */
    private static boolean avoidConsoleWriter;

    /**
     * Do not allow instantiation of this class.
     */
    private Environment() {
    }

    /**
     * Notifies the command runners that the use of console writer should be avoided.
     * In our tests with Java 21, sending non ASCII characters to the console writer in a Linux system
     * from a JShell session resulted in wrong characters being printed, sometime followed by JShell errors.
     * This flag is set only if the commands are run from JShell.
     */
    @Configuration(writeAccess = Configuration.Access.INTERNAL)
    @Workaround(library="jshell", version="21")
    public static void avoidConsoleWriter() {
        avoidConsoleWriter = true;
    }

    /**
     * Returns the console print writer, or the standard output stream if there is no console.
     * Caller should flush the writer after use, because there is no guarantee that is will not
     * be disposed by the garbage collector.
     *
     * @return the writer to use.
     */
    public static PrintWriter writer() {
        return writer(System.console(), System.out);
    }

    /**
     * Returns the console print writer, or the given alternative if the console cannot be used.
     * Caller should flush the writer after use, because there is no guarantee that is will not
     * be disposed by the garbage collector.
     *
     * @param  console   the value of {@link System#console()}, potentially null.
     * @param  fallback  the fallback to use if the console cannot be used.
     * @return the writer to use.
     */
    public static PrintWriter writer(final Console console, final PrintStream fallback) {
        if (console == null) {
            return new PrintWriter(fallback, true);
        } else if (avoidConsoleWriter) {
            return new PrintWriter(new OutputStreamWriter(fallback, JDK17.charset(console)), true);
        } else {
            return console.writer();
        }
    }
}
