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

import java.util.Locale;
import java.util.logging.LogManager;
import java.util.logging.ConsoleHandler;
import java.io.Console;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.sql.SQLException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.MonolineFormatter;


/**
 * Command line interface for Apache SIS. The {@link #main(String[])} method accepts the following actions:
 *
 * <blockquote><table class="compact" summary="Supported command-line actions.">
 * <tr><td>{@code help}       </td><td>Show a help overview.</td></tr>
 * <tr><td>{@code about}      </td><td>Show information about Apache SIS and system configuration.</td></tr>
 * <tr><td>{@code mime-type}  </td><td>Show MIME type for the given file.</td></tr>
 * <tr><td>{@code metadata}   </td><td>Show metadata information for the given file.</td></tr>
 * <tr><td>{@code crs}        </td><td>Show Coordinate Reference System information for the given file or code.</td></tr>
 * <tr><td>{@code identifier} </td><td>Show identifiers for metadata and referencing systems in the given file.</td></tr>
 * <tr><td>{@code transform}  </td><td>Convert or transform coordinates from given source CRS to target CRS.</td></tr>
 * </table></blockquote>
 *
 * Each command can accepts some of the following options:
 *
 * <blockquote><table class="compact" summary="Supported command-line options.">
 * <tr><td>{@code --sourceCRS} </td><td>The Coordinate Reference System of input data.</td></tr>
 * <tr><td>{@code --targetCRS} </td><td>The Coordinate Reference System of output data.</td></tr>
 * <tr><td>{@code --format}    </td><td>The output format: {@code xml}, {@code wkt}, {@code wkt1} or {@code text}.</td></tr>
 * <tr><td>{@code --locale}    </td><td>The locale to use for the command output.</td></tr>
 * <tr><td>{@code --timezone}  </td><td>The timezone for the dates to be formatted.</td></tr>
 * <tr><td>{@code --encoding}  </td><td>The encoding to use for the command outputs and some inputs.</td></tr>
 * <tr><td>{@code --colors}    </td><td>Whether colorized output shall be enabled.</td></tr>
 * <tr><td>{@code --brief}     </td><td>Whether the output should contains only brief information.</td></tr>
 * <tr><td>{@code --verbose}   </td><td>Whether the output should contains more detailed information.</td></tr>
 * <tr><td>{@code --debug}     </td><td>Prints full stack trace in case of failure.</td></tr>
 * <tr><td>{@code --help}      </td><td>Lists the options available for a specific command.</td></tr>
 * </table></blockquote>
 *
 * The {@code --locale}, {@code --timezone} and {@code --encoding} options apply to the command output sent
 * to the {@linkplain System#out standard output stream}, but usually do not apply to the error messages sent
 * to the {@linkplain System#err standard error stream}. The reason is that command output may be targeted to
 * a client, while the error messages are usually for the operator.
 *
 * <div class="section">SIS installation on remote machines</div>
 * Some sub-commands can operate on SIS installation on remote machines, provided that remote access has been enabled
 * at the Java Virtual Machine startup time. See {@linkplain org.apache.sis.console package javadoc} for more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
public final class Command {
    /**
     * The code given to {@link System#exit(int)} when the program failed because of a unknown sub-command.
     */
    public static final int INVALID_COMMAND_EXIT_CODE = 1;

    /**
     * The code given to {@link System#exit(int)} when the program failed because of a unknown option.
     * The set of valid options depend on the sub-command to execute.
     */
    public static final int INVALID_OPTION_EXIT_CODE = 2;

    /**
     * The code given to {@link System#exit(int)} when the program failed because of an illegal user argument.
     * The user arguments are everything which is not a command name or an option. They are typically file names,
     * but can occasionally be other types like URL.
     */
    public static final int INVALID_ARGUMENT_EXIT_CODE = 3;

    /**
     * The code given to {@link System#exit(int)} when a file given in argument uses an unknown file format.
     */
    public static final int UNKNOWN_STORAGE_EXIT_CODE = 4;

    /**
     * The code given to {@link System#exit(int)} when the program failed because of an {@link java.io.IOException}.
     */
    public static final int IO_EXCEPTION_EXIT_CODE = 100;

    /**
     * The code given to {@link System#exit(int)} when the program failed because of an {@link java.sql.SQLException}.
     */
    public static final int SQL_EXCEPTION_EXIT_CODE = 101;

    /**
     * The code given to {@link System#exit(int)} when the program failed for a reason
     * other than the ones enumerated in the above constants.
     */
    public static final int OTHER_ERROR_EXIT_CODE = 199;

    /**
     * The sub-command name.
     */
    private final String commandName;

    /**
     * The sub-command to execute.
     */
    private final CommandRunner command;

    /**
     * Creates a new command for the given arguments. The first value in the given array which is
     * not an option is taken as the command name. All other values are options or filenames.
     *
     * @param  args The command-line arguments.
     * @throws InvalidCommandException If an invalid command has been given.
     * @throws InvalidOptionException If the given arguments contain an invalid option.
     */
    protected Command(final String[] args) throws InvalidCommandException, InvalidOptionException {
        int commandIndex = -1;
        String commandName = null;
        for (int i=0; i<args.length; i++) {
            final String arg = args[i];
            if (arg.startsWith(Option.PREFIX)) {
                final String name = arg.substring(Option.PREFIX.length());
                final Option option = Option.forLabel(name);
                if (option.hasValue) {
                    i++;                        // Skip the next argument.
                }
            } else {
                // Takes the first non-argument option as the command name.
                commandName = arg;
                commandIndex = i;
                break;
            }
        }
        if (commandName == null) {
            command = new HelpCommand(-1, args);
        } else {
            commandName = commandName.toLowerCase(Locale.US);
                 if (commandName.equals("help"))       command = new HelpCommand      (commandIndex, args);
            else if (commandName.equals("about"))      command = new AboutCommand     (commandIndex, args);
            else if (commandName.equals("mime-type"))  command = new MimeTypeCommand  (commandIndex, args);
            else if (commandName.equals("metadata"))   command = new MetadataCommand  (commandIndex, args);
            else if (commandName.equals("crs"))        command = new CRSCommand       (commandIndex, args);
            else if (commandName.equals("identifier")) command = new IdentifierCommand(commandIndex, args);
            else if (commandName.equals("transform"))  command = new TransformCommand (commandIndex, args);
            else throw new InvalidCommandException(Errors.format(
                        Errors.Keys.UnknownCommand_1, commandName), commandName);
        }
        this.commandName = commandName;
        CommandRunner.instance = command;       // For ResourcesDownloader only.
    }

    /**
     * Runs the command. If an exception occurs, then the exception message is sent to the error output stream
     * before to be thrown. Callers can map the exception to a {@linkplain System#exit(int) system exit code}
     * by the {@link #exitCodeFor(Throwable)} method.
     *
     * @return 0 on success, or an exit code if the command failed for a reason other than a Java exception.
     * @throws Exception If an error occurred during the command execution. This is typically, but not limited, to
     *         {@link IOException}, {@link SQLException}, {@link DataStoreException} or {@link TransformException}.
     */
    public int run() throws Exception {
        if (command.hasContradictoryOptions(Option.BRIEF, Option.VERBOSE)) {
            return INVALID_OPTION_EXIT_CODE;
        }
        if (command.options.containsKey(Option.HELP)) {
            command.help(commandName);
        } else try {
            return command.run();
        } catch (Exception e) {
            command.error(null, e);
            throw e;
        }
        return 0;
    }

    /**
     * Returns the exit code for the given exception, or 0 if unknown. This method iterates through the
     * {@linkplain Throwable#getCause() causes} until an exception matching a {@code *_EXIT_CODE}
     * constant is found.
     *
     * @param  cause The exception for which to get the exit code.
     * @return The exit code as one of the {@code *_EXIT_CODE} constant, or {@link #OTHER_ERROR_EXIT_CODE} if unknown.
     */
    public static int exitCodeFor(Throwable cause) {
        while (cause != null) {
            if (cause instanceof InvalidCommandException) return INVALID_COMMAND_EXIT_CODE;
            if (cause instanceof InvalidOptionException)  return INVALID_OPTION_EXIT_CODE;
            if (cause instanceof IOException)             return IO_EXCEPTION_EXIT_CODE;
            if (cause instanceof SQLException)            return SQL_EXCEPTION_EXIT_CODE;
            cause = cause.getCause();
        }
        return OTHER_ERROR_EXIT_CODE;
    }

    /**
     * Prints the message of the given exception. This method is invoked only when the error occurred before
     * the {@link CommandRunner} has been built, otherwise the {@link CommandRunner#err} printer shall be used.
     *
     * @param args The command line arguments, used only for detecting if the {@code --debug} option was present.
     */
    private static void error(final String[] args, final Exception e) {
        final boolean debug = ArraysExt.containsIgnoreCase(args, Option.PREFIX + "debug");
        final Console console = System.console();
        if (console != null) {
            final PrintWriter err = console.writer();
            if (debug) {
                e.printStackTrace(err);
            } else {
                err.println(e.getLocalizedMessage());
            }
            err.flush();
        } else {
            @SuppressWarnings("UseOfSystemOutOrSystemErr")
            final PrintStream err = System.err;
            if (debug) {
                e.printStackTrace(err);
            } else {
                err.println(e.getLocalizedMessage());
            }
            err.flush();
        }
    }

    /**
     * Prints the information to the standard output stream.
     *
     * @param args Command-line options.
     */
    public static void main(final String[] args) {
        /*
         * The logging configuration is given by the "conf/logging.properties" file in the Apache SIS
         * installation directory. By default, that configuration file contains the following line:
         *
         *     java.util.logging.ConsoleHandler.formatter = org.apache.sis.util.logging.MonolineFormatter
         *
         * However this configuration is silently ignored by LogManager at JVM startup time, probably
         * because the Apache SIS class is not on the system classpath. So we check again for this
         * configuration line here, and manually install our log formatter only if the above-cited
         * line is present.
         */
        final LogManager manager = LogManager.getLogManager();
        if (MonolineFormatter.class.getName().equals(manager.getProperty(ConsoleHandler.class.getName() + ".formatter"))) {
            MonolineFormatter.install();
        }
        /*
         * Now run the command.
         */
        final Command c;
        try {
            c = new Command(args);
        } catch (InvalidCommandException e) {
            error(args, e);
            System.exit(INVALID_COMMAND_EXIT_CODE);
            return;
        } catch (InvalidOptionException e) {
            error(args, e);
            System.exit(INVALID_OPTION_EXIT_CODE);
            return;
        }
        int status;
        try {
            status = c.run();
        } catch (Exception e) {
            status = exitCodeFor(e);
        }
        if (status != 0) {
            System.exit(status);
        }
    }
}
