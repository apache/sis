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
package org.apache.sis.console;

import java.io.Console;
import java.io.PrintStream;
import java.io.PrintWriter;
import org.apache.sis.util.resources.Errors;


/**
 * Command line interface for Apache SIS.
 * The main method can be invoked from the command-line as below
 * (the filename needs to be completed with the actual version number):
 *
 * {@preformat java
 *     java -jar target/binaries/sis-app.jar
 * }
 *
 * "{@code target/binaries}" is the default location where SIS JAR files are grouped together
 * with their dependencies after a Maven build. This directory can be replaced by any path to
 * a directory providing the required dependencies.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public class Command implements Runnable {
    /**
     * The code given to {@link System#exit(int)} when the program failed because of a unknown sub-command.
     */
    public static final int INVALID_COMMAND_EXIT_CODE = 1;

    /**
     * The code given to {@link System#exit(int)} when the program failed because of an illegal user argument.
     */
    public static final int INVALID_OPTION_EXIT_CODE = 2;

    /**
     * The code given to {@link System#exit(int)} when a file given in argument uses an unknown file format.
     */
    public static final int UNKNOWN_STORAGE_EXIT_CODE = 10;

    /**
     * The code given to {@link System#exit(int)} when the program failed because of an {@link java.io.IOException}.
     */
    public static final int IO_EXCEPTION_EXIT_CODE = 100;

    /**
     * The code given to {@link System#exit(int)} when the program failed because of an {@link java.sql.SQLException}.
     */
    public static final int SQL_EXCEPTION_EXIT_CODE = 101;

    /**
     * The sub-command to execute.
     */
    private final SubCommand command;

    /**
     * Creates a new command for the given arguments. The first value in the given array shall be the
     * command name. All other values are options.
     *
     * @param  args The command-line arguments.
     * @throws InvalidCommandException If an invalid command has been given.
     * @throws InvalidOptionException If the given arguments contain an invalid option.
     */
    protected Command(final String[] args) throws InvalidCommandException, InvalidOptionException {
        if (args.length == 0) {
            command = new HelpCS(args);
        } else {
            final String name = args[0];
            switch (name.toLowerCase()) {
                case "about": {
                    command = new AboutSC(args);
                    break;
                }
                case "help": {
                    command = new HelpCS(args);
                    break;
                }
                default: {
                    throw new InvalidCommandException(Errors.format(Errors.Keys.UnknownCommand_1, name), name);
                }
            }
        }
    }

    /**
     * Runs the command.
     */
    @Override
    public void run() {
        command.run();
    }

    /**
     * Prints the message of the given exception. This method is invoked only when the error occurred before
     * the {@link SubCommand} has been built, otherwise the {@link SubCommand#err} printer shall be used.
     */
    private static void error(final Exception e) {
        final Console console = System.console();
        if (console != null) {
            final PrintWriter err = console.writer();
            err.println(e.getLocalizedMessage());
            err.flush();
        } else {
            final PrintStream err = System.err;
            err.println(e.getLocalizedMessage());
            err.flush();
        }
    }

    /**
     * Prints the information to the standard output stream.
     *
     * @param args Command-line options.
     */
    public static void main(final String[] args) {
        final Command c;
        try {
            c = new Command(args);
        } catch (InvalidCommandException e) {
            error(e);
            System.exit(INVALID_COMMAND_EXIT_CODE);
            return;
        } catch (InvalidOptionException e) {
            error(e);
            System.exit(INVALID_OPTION_EXIT_CODE);
            return;
        }
        c.run();
    }
}
