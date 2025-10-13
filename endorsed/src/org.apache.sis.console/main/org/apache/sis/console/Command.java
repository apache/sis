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
import java.io.PrintWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.internal.shared.X364;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.logging.Initializer;
import org.apache.sis.util.logging.MonolineFormatter;
import org.apache.sis.system.Environment;


/**
 * Command line interface for Apache SIS. The {@link #main(String[])} method accepts the following actions:
 *
 * <blockquote><table class="compact">
 * <caption>Supported command-line actions</caption>
 * <tr><td>{@code help}       </td><td>Show a help overview.</td></tr>
 * <tr><td>{@code about}      </td><td>Show information about Apache SIS and system configuration.</td></tr>
 * <tr><td>{@code mime-type}  </td><td>Show MIME type for the given file.</td></tr>
 * <tr><td>{@code identifier} </td><td>Show identifiers for metadata and referencing systems in the given file.</td></tr>
 * <tr><td>{@code metadata}   </td><td>Show metadata information for the given file.</td></tr>
 * <tr><td>{@code crs}        </td><td>Show Coordinate Reference System information for the given file or code.</td></tr>
 * <tr><td>{@code info}       </td><td>Show resource-specific information (e.g., grid geometry).</td></tr>
 * <tr><td>{@code transform}  </td><td>Convert or transform coordinates from given source CRS to target CRS.</td></tr>
 * <tr><td>{@code translate}  </td><td>Rewrite a data file in another format.</td></tr>
 * </table></blockquote>
 *
 * Each command can accepts some of the following options:
 *
 * <blockquote><table class="compact">
 * <caption>Supported command-line options</caption>
 * <tr><td>{@code --sourceCRS} </td><td>The Coordinate Reference System of input data.</td></tr>
 * <tr><td>{@code --targetCRS} </td><td>The Coordinate Reference System of output data.</td></tr>
 * <tr><td>{@code --metadata}  </td><td>Relative path to an auxiliary metadata file.</td></tr>
 * <tr><td>{@code --format}    </td><td>The output format: {@code xml}, {@code wkt}, {@code wkt1} or {@code text}.</td></tr>
 * <tr><td>{@code --locale}    </td><td>The locale to use for the console output.</td></tr>
 * <tr><td>{@code --timezone}  </td><td>The timezone for the dates printed to the console output.</td></tr>
 * <tr><td>{@code --encoding}  </td><td>The encoding to use for some text inputs and for console output.</td></tr>
 * <tr><td>{@code --colors}    </td><td>Whether colorized output shall be enabled.</td></tr>
 * <tr><td>{@code --brief}     </td><td>Whether the output should contain only brief information.</td></tr>
 * <tr><td>{@code --verbose}   </td><td>Whether the output should contain more detailed information.</td></tr>
 * <tr><td>{@code --debug}     </td><td>Prints full stack trace in case of failure.</td></tr>
 * <tr><td>{@code --help}      </td><td>Lists the options available for a specific command.</td></tr>
 * </table></blockquote>
 *
 * The {@code --locale}, {@code --timezone} and {@code --encoding} options apply to the command output sent
 * to the {@linkplain System#out standard output stream}, but usually do not apply to the error messages sent
 * to the {@linkplain System#err standard error stream}. The reason is that command output may be targeted to
 * a client, while the error messages are usually for the operator.
 *
 * <h2>SIS installation on remote machines</h2>
 * Some sub-commands can operate on SIS installation on remote machines, provided that remote access has been enabled
 * at the Java Virtual Machine startup time. See {@linkplain org.apache.sis.console package javadoc} for more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   0.3
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
     * The sub-command to execute.
     */
    private final CommandRunner command;

    /**
     * Creates a new command for the given arguments. The first value in the given array which is
     * not an option is taken as the command name. All other values are options or filenames.
     *
     * <p>Arguments should be instances of {@link String}, except the arguments for input or output files
     * which can be any types accepted by {@link org.apache.sis.storage.StorageConnector}. This includes,
     * for example, {@link String}, {@link java.io.File}, {@link java.nio.file.Path}, {@link java.net.URL},
     * <i>etc.</i></p>
     *
     * @param  args  the command-line arguments.
     * @throws InvalidCommandException if an invalid command has been given.
     * @throws InvalidOptionException if the given arguments contain an invalid option.
     */
    public Command(final Object[] args) throws InvalidCommandException, InvalidOptionException {
        int commandIndex = -1;
        String commandName = null;
        for (int i=0; i<args.length; i++) {
            final Object arg = args[i];
            if (arg instanceof CharSequence) {
                final String s = arg.toString();
                if (s.startsWith(Option.PREFIX)) {
                    final String name = s.substring(Option.PREFIX.length());
                    final Option option = Option.forLabel(name);
                    if (option.hasValue) {
                        i++;                        // Skip the next argument.
                    }
                } else {
                    // Takes the first non-argument option as the command name.
                    commandName  = s;
                    commandIndex = i;
                    break;
                }
            }
        }
        if (commandName == null) {
            command = new HelpCommand(-1, args);
        } else {
            switch (commandName.toLowerCase(Locale.US)) {
                case "help":       command = new HelpCommand      (commandIndex, args); break;
                case "about":      command = new AboutCommand     (commandIndex, args); break;
                case "mime-type":  command = new MimeTypeCommand  (commandIndex, args); break;
                case "metadata":   command = new MetadataCommand  (commandIndex, args); break;
                case "crs":        command = new CRSCommand       (commandIndex, args); break;
                case "info":       command = new InfoCommand      (commandIndex, args); break;
                case "identifier": command = new IdentifierCommand(commandIndex, args); break;
                case "transform":  command = new TransformCommand (commandIndex, args); break;
                case "translate":  command = new TranslateCommand (commandIndex, args); break;
                default: throw new InvalidCommandException(Errors.format(
                            Errors.Keys.UnknownCommand_1, commandName), commandName);
            }
        }
    }

    /**
     * Loads the logging configuration file if not already done, then configures the monoline formatter.
     * This method performs two main tasks:
     *
     * <ol>
     *   <li>If the {@value Initializer#CONFIG_FILE_PROPERTY} is <em>not</em> set, then try
     *       to set it to {@code $SIS_HOME/conf/logging.properties} and load that file.</li>
     *   <li>If the {@code "derby.stream.error.file"} system property is not defined,
     *       then try to set it to {@code $SIS_HOME/log/derby.log}.</li>
     *   <li>If the configuration file declares {@link MonolineFormatter} as the console formatter,
     *       ensures that the formatter is loaded and resets its colors depending on whether X364
     *       seems to be supported.</li>
     * </ol>
     *
     * This method can be invoked at initialization time,
     * such as the beginning of {@code main(…)} static method.
     *
     * @see MonolineFormatter#install()
     *
     * @since 1.5
     */
    public static void configureLogging() {
        final String value = System.getenv("SIS_HOME");
        if (value != null) {
            final Path home = Path.of(value).normalize();
            Path file = home.resolve("log");
            if (Files.isDirectory(file)) {
                setPropertyIfAbsent("derby.stream.error.file", file.resolve("derby.log"));
            }
            file = home.resolve("conf").resolve("logging.properties");
            if (Files.isRegularFile(file)) {
                if (setPropertyIfAbsent(Initializer.CONFIG_FILE_PROPERTY, file)) try {
                    Initializer.reload(file);
                } catch (IOException e) {
                    Logging.unexpectedException(null, Command.class, "configureLogging", e);
                }
            }
        }
        /*
         * The logging configuration is given by the "conf/logging.properties" file in the Apache SIS
         * installation directory. By default, that configuration file contains the following line:
         *
         *     java.util.logging.ConsoleHandler.formatter = org.apache.sis.util.logging.MonolineFormatter
         *
         * However, this configuration is sometime silently ignored by the LogManager at JVM startup time,
         * maybe because the Apache SIS classes were not yet loaded. So we check again if the configuration
         * contained that line, and manually re-install the log formatter if that line is present.
         */
        final String handler = LogManager.getLogManager().getProperty("java.util.logging.ConsoleHandler.formatter");
        if (MonolineFormatter.class.getName().equals(handler)) {
            MonolineFormatter f = MonolineFormatter.install();
            f.setMaximalLineLength(Command::terminalSize);
            f.resetLevelColors(X364.isAnsiSupported());
        }
    }

    /**
     * Returns the terminal size if known, of {@link Integer#MAX_VALUE} otherwise.
     * The returned value may be obsolete, because the {@code COLUMNS} environment
     * variable is not updated when the user resizes the terminal window.
     *
     * @return the assumed terminal size.
     */
    private static int terminalSize() {
        final String n = System.getenv("COLUMNS");
        if (n != null && !n.isEmpty()) try {
            return Integer.parseInt(n);
        } catch (NumberFormatException e) {
            // Ignore.
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Sets the specified system property if that property is not already set.
     * This method returns whether the property has been set.
     */
    private static boolean setPropertyIfAbsent(final String property, final Path value) {
        if (System.getProperty(property) == null) {
            System.setProperty(property, value.toString());
            return true;
        }
        return false;
    }

    /**
     * Returns the writer where this command sends its output.
     *
     * @param  error  {@code true} for the error stream, or {@code false} for the standard output stream.
     * @return the stream where this command sends it output.
     *
     * @since 1.5
     */
    public PrintWriter writer(final boolean error) {
        return error ? command.err : command.out;
    }

    /**
     * Turns on or off the faint output, if supported.
     * This method does nothing if the terminal does not seem to support X364 sequences.
     *
     * @param  faint  whether to turn on the faint output.
     */
    final void setFaintOutput(final boolean faint) {
        command.color(faint ? X364.FAINT : X364.NORMAL);
    }

    /**
     * Runs the command. If an exception occurs, then the exception message is sent to the error output stream
     * before to be thrown. Callers can map the exception to a {@linkplain System#exit(int) system exit code}
     * by the {@link #exitCodeFor(Throwable)} method.
     *
     * @return 0 on success, or an exit code if the command failed for a reason other than a Java exception.
     * @throws Exception if an error occurred during the command execution. This is typically, but not limited, to
     *         {@link IOException}, {@link SQLException}, {@link DataStoreException} or {@link TransformException}.
     */
    public int run() throws Exception {
        if (command.hasContradictoryOptions(Option.BRIEF, Option.VERBOSE)) {
            return INVALID_OPTION_EXIT_CODE;
        }
        if (command.options.containsKey(Option.HELP)) {
            command.help(command.commandName.toLowerCase(Locale.US));
        } else try {
            CommandRunner.instance.set(command);        // For ResourcesDownloader only.
            int status = command.run();
            command.flush();
            return status;
        } catch (Exception e) {
            command.error(null, e);
            throw e;
        } finally {
            CommandRunner.instance.remove();
        }
        return 0;
    }

    /**
     * Returns the exit code for the given exception, or 0 if unknown. This method iterates through the
     * {@linkplain Throwable#getCause() causes} until an exception matching a {@code *_EXIT_CODE}
     * constant is found.
     *
     * @param  cause  the exception for which to get the exit code.
     * @return the exit code as one of the {@code *_EXIT_CODE} constant, or {@link #OTHER_ERROR_EXIT_CODE} if unknown.
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
     * @param  args  the command line arguments, used only for detecting if the {@code --debug} option was present.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void error(final String[] args, final Exception e) {
        final boolean debug = ArraysExt.containsIgnoreCase(args, Option.PREFIX + "debug");
        final PrintWriter err = Environment.writer(System.console(), System.err);
        if (debug) {
            e.printStackTrace(err);
        } else {
            err.println(e.getLocalizedMessage());
        }
        err.flush();
    }

    /**
     * Prints the information to the standard output stream.
     *
     * @param  args  command-line options.
     */
    public static void main(final String[] args) {
        configureLogging();
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
