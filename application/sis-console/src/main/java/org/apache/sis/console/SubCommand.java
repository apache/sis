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

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.EnumSet;
import java.util.EnumMap;
import java.util.TimeZone;
import java.util.ResourceBundle;
import java.io.Console;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import org.apache.sis.util.Locales;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.io.TableAppender;
import org.apache.sis.internal.util.X364;


/**
 * Base class of sub-commands.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
abstract class SubCommand {
    /**
     * Special value for {@code arguments[commandIndex]} meaning that this sub-command is created
     * for JUnit test purpose.
     *
     * @see #outputBuffer
     */
    static final String TEST = "TEST";

    /**
     * The set of legal options for this command.
     *
     * @see #help(String)
     */
    private final EnumSet<Option> validOptions;

    /**
     * The command-line options allowed by this sub-command, together with their values.
     */
    protected final EnumMap<Option,String> options;

    /**
     * The locale specified by the {@code "--locale"} option. If no such option was
     * provided, then this field is set to the {@linkplain Locale#getDefault() default locale}.
     */
    protected final Locale locale;

    /**
     * The locale specified by the {@code "--timezone"} option. If no such option was provided,
     * then this field is left to {@code null}.
     */
    protected final TimeZone timezone;

    /**
     * The encoding specified by the {@code "--encoding"} option. If no such option was provided,
     * then this field is set to the {@linkplain Charset#defaultCharset() default charset}.
     */
    protected final Charset encoding;

    /**
     * {@code true} if colors can be applied for ANSI X3.64 compliant terminal.
     * This is the value specified by the {@code --colors} arguments if present,
     * or a value inferred from the system otherwise.
     */
    protected final Boolean colors;

    /**
     * Output stream to the console. This output stream uses the encoding
     * specified by the {@code "--encoding"} argument, if presents.
     */
    protected final PrintWriter out;

    /**
     * Error stream to the console. This stream always uses the locale encoding, since its output will
     * typically be sent to the console even if the user redirected the standard output to a file.
     */
    protected final PrintWriter err;

    /**
     * The buffer where {@link #out} and {@link #err} output are sent, or {@code null} if none.
     * This is non-null only during JUnit tests.
     *
     * @see #TEST
     */
    final StringBuffer outputBuffer;

    /**
     * Any remaining parameters that are not command name or option.
     * They are typically file names, but can occasionally be other type like URL.
     */
    protected final List<String> files;

    /**
     * Creates a new sub-command with the given command-line arguments.
     * The {@code arguments} array is the same array than the one given to the {@code main(String[])} method.
     * The argument at index {@code commandIndex} is the name of this command, and will be ignored except for
     * the special {@value #TEST} value which is used only at JUnit testing time.
     *
     * @param  commandIndex Index of the {@code args} element containing the sub-command name.
     * @param  arguments    The command-line arguments provided by the user.
     * @param  validOptions The command-line options allowed by this sub-command.
     * @throws InvalidOptionException If an illegal option has been provided, or the option has an illegal value.
     */
    protected SubCommand(final int commandIndex, final String[] arguments, final EnumSet<Option> validOptions)
            throws InvalidOptionException
    {
        boolean isTest = false;
        this.validOptions = validOptions;
        options = new EnumMap<>(Option.class);
        files = new ArrayList<>(arguments.length);
        for (int i=0; i<arguments.length; i++) {
            final String arg = arguments[i];
            if (i == commandIndex) {
                isTest = arg.equals(TEST);
                continue;
            }
            if (arg.startsWith(Option.PREFIX)) {
                final String name = arg.substring(Option.PREFIX.length());
                final Option option;
                try {
                    option = Option.valueOf(name.toUpperCase(Locale.US));
                } catch (IllegalArgumentException e) {
                    throw new InvalidOptionException(Errors.format(Errors.Keys.UnknownOption_1, name), e, name);
                }
                if (!validOptions.contains(option)) {
                    throw new InvalidOptionException(Errors.format(Errors.Keys.UnknownOption_1, name), name);
                }
                String value = null;
                if (option.hasValue) {
                    if (++i >= arguments.length) {
                        throw new InvalidOptionException(Errors.format(Errors.Keys.MissingValueForOption_1, name), name);
                    }
                    value = arguments[i];
                }
                if (options.containsKey(option)) {
                    throw new InvalidOptionException(Errors.format(Errors.Keys.DuplicatedOption_1, name), name);
                }
                options.put(option, value);
            } else {
                files.add(arg);
            }
        }
        /*
         * Process the --locale, --encoding and --colors options.
         */
        Option option = null; // In case of IllegalArgumentException.
        String value  = null;
        final Console console;
        final boolean explicitEncoding;
        try {
            value = options.get(option = Option.LOCALE);
            locale = (value != null) ? Locales.parse(value) : Locale.getDefault(Locale.Category.DISPLAY);

            value = options.get(option = Option.TIMEZONE);
            timezone = (value != null) ? TimeZone.getTimeZone(value) : null;

            value = options.get(option = Option.ENCODING);
            explicitEncoding = (value != null);
            encoding = explicitEncoding ? Charset.forName(value) : Charset.defaultCharset();

            value = options.get(option = Option.COLORS);
            console = System.console();
            colors = (value != null) ? Option.COLORS.parseBoolean(value) : (console != null) && X364.isAnsiSupported();
        } catch (IllegalArgumentException e) {
            final String name = option.name().toLowerCase(Locale.US);
            throw new InvalidOptionException(Errors.format(Errors.Keys.IllegalOptionValue_2, name, value), name);
        }
        /*
         * Creates the writers. If this sub-command is created for JUnit test purpose, then we will send the
         * output to a StringBuffer. Otherwise the output will be sent to the java.io.Console if possible,
         * or to the standard output stream otherwise.
         */
        if (isTest) {
            final StringWriter s = new StringWriter();
            outputBuffer = s.getBuffer();
            out = new PrintWriter(s);
            err = out;
        } else {
            outputBuffer = null;
            err = (console != null) ? console.writer() : new PrintWriter(System.err, true);
            if (!explicitEncoding && console != null) {
                out = console.writer();
            } else {
                if (explicitEncoding) {
                    out = new PrintWriter(new OutputStreamWriter(System.out, encoding), true);
                } else {
                    out = new PrintWriter(System.out, true);
                }
            }
        }
    }

    /**
     * Shows the help instructions for a specific command. This method is invoked
     * instead of {@link #run()} if the the user provided the {@code --help} option.
     *
     * @param commandName The command name converted to lower cases.
     */
    protected void help(final String commandName) {
        final ResourceBundle commands = ResourceBundle.getBundle("org.apache.sis.console.Commands", locale);
        final ResourceBundle options  = ResourceBundle.getBundle("org.apache.sis.console.Options", locale);
        final Vocabulary vocabulary = Vocabulary.getResources(locale);
        out.print(commandName);
        out.print(": ");
        out.println(commands.getString(commandName));
        out.println();
        out.print(vocabulary.getString(Vocabulary.Keys.Options));
        out.println(':');
        final TableAppender table = new TableAppender(out, "  ");
        for (final Option option : validOptions) {
            final String name = option.name().toLowerCase(Locale.US);
            table.append("  ").append(Option.PREFIX).append(name);
            table.nextColumn();
            table.append(options.getString(name));
            table.nextLine();
        }
        try {
            table.flush();
        } catch (IOException e) {
            throw new AssertionError(e); // Should never happen, because we are writing to a PrintWriter.
        }
    }

    /**
     * Executes the sub-command.
     *
     * @throws Exception If an error occurred while executing the sub-command.
     */
    public abstract void run() throws Exception;
}
