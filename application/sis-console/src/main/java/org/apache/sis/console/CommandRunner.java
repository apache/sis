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
import java.io.Console;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import org.apache.sis.util.Locales;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.X364;


/**
 * Base class of sub-commands.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
abstract class CommandRunner {
    /**
     * Special value for {@code arguments[commandIndex]} meaning that this sub-command is created
     * for JUnit test purpose.
     *
     * @see #outputBuffer
     */
    static final String TEST = "TEST";

    /**
     * The instance, used by {@link ResourcesDownloader} only.
     * We use this static field as a workaround for the fact that {@code ResourcesDownloader} is not
     * instantiated by us, so we can not pass the {@code CommandRunner} instance to its constructor.
     */
    static CommandRunner instance;

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
     * then this field is set to the {@linkplain TimeZone#getDefault() default timezone}.
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
    protected final boolean colors;

    /**
     * {@code true} for printing the full stack trace in case of failure.
     */
    protected final boolean debug;

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
     * They are typically file names, but can occasionally be other types like URL.
     */
    protected final List<String> files;

    /**
     * Copies the configuration of the given sub-command. This constructor is used
     * only when a command needs to delegates part of its work to an other command.
     */
    CommandRunner(final CommandRunner parent) {
        this.validOptions = parent.validOptions;
        this.options      = parent.options;
        this.locale       = parent.locale;
        this.timezone     = parent.timezone;
        this.encoding     = parent.encoding;
        this.colors       = parent.colors;
        this.debug        = parent.debug;
        this.out          = parent.out;
        this.err          = parent.err;
        this.outputBuffer = parent.outputBuffer;
        this.files        = parent.files;
    }

    /**
     * Creates a new sub-command with the given command-line arguments.
     * The {@code arguments} array is the same array than the one given to the {@code main(String[])} method.
     * The argument at index {@code commandIndex} is the name of this command, and will be ignored except for
     * the special {@value #TEST} value which is used only at JUnit testing time.
     *
     * @param  commandIndex  index of the {@code args} element containing the sub-command name.
     * @param  arguments     the command-line arguments provided by the user.
     * @param  validOptions  the command-line options allowed by this sub-command.
     * @throws InvalidOptionException if an illegal option has been provided, or the option has an illegal value.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    protected CommandRunner(final int commandIndex, final String[] arguments, final EnumSet<Option> validOptions)
            throws InvalidOptionException
    {
        boolean isTest = false;
        this.validOptions = validOptions;
        options = new EnumMap<Option,String>(Option.class);
        files = new ArrayList<String>(arguments.length);
        for (int i=0; i<arguments.length; i++) {
            final String arg = arguments[i];
            if (i == commandIndex) {
                isTest = arg.equals(TEST);
                continue;
            }
            if (arg.startsWith(Option.PREFIX)) {
                final String name = arg.substring(Option.PREFIX.length());
                final Option option = Option.forLabel(name);
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
        Option option = null;                                           // In case of IllegalArgumentException.
        String value  = null;
        final Console console;
        final boolean explicitEncoding;
        try {
            debug = options.containsKey(option = Option.DEBUG);

            value = options.get(option = Option.LOCALE);
            locale = (value != null) ? Locales.parse(value) : Locale.getDefault();

            value = options.get(option = Option.TIMEZONE);
            timezone = (value != null) ? TimeZone.getTimeZone(value) : TimeZone.getDefault();

            value = options.get(option = Option.ENCODING);
            explicitEncoding = (value != null);
            encoding = explicitEncoding ? Charset.forName(value) : Charset.defaultCharset();

            value = options.get(option = Option.COLORS);
            console = System.console();
            colors = (value != null) ? Option.COLORS.parseBoolean(value) : (console != null) && X364.isAnsiSupported();
        } catch (RuntimeException e) {
            @SuppressWarnings("null")                                   // 'option' has been assigned in 'get' argument.
            final String name = option.label();
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
     * Checks if the user-provided {@linkplain #options} contains mutually exclusive options.
     * If an inconsistency is found, then this method prints an error message to {@link #err}
     * and returns {@code true}.
     *
     * <p>An example of a pair of mutually exclusive options is {@code --brief} and {@code --verbose}.</p>
     *
     * @param  exclusive  pairs of mutually exclusive options.
     * @return {@code true} if two mutually exclusive options exist.
     */
    final boolean hasContradictoryOptions(final Option... exclusive) {
        for (int i=0; i<exclusive.length;) {
            final Option o1 = exclusive[i++];
            final Option o2 = exclusive[i++];
            if (options.containsKey(o1) && options.containsKey(o2)) {
                err.println(Errors.format(Errors.Keys.MutuallyExclusiveOptions_2, o1.label(), o2.label()));
                return true;
            }
        }
        return false;
    }

    /**
     * Checks the size of the {@link #files} list. If the list has an unexpected size,
     * then this method prints an error message to {@link #err} and returns {@code true}.
     *
     * @param  min  minimal number of files.
     * @param  max  maximum number of files.
     * @return {@code true} if the list size is not in the expected bounds.
     */
    final boolean hasUnexpectedFileCount(final int min, final int max) {
        final int size = files.size();
        final int expected;
        final short key;
        if (size < min) {
            expected = min;
            key = Errors.Keys.TooFewArguments_2;
        } else if (size > max) {
            expected = max;
            key = Errors.Keys.TooManyArguments_2;
        } else {
            return false;
        }
        err.println(Errors.format(key, expected, size));
        return true;
    }

    /**
     * Returns {@code true} if the command should use the standard input.
     */
    final boolean useStandardInput() {
        return files.isEmpty() && System.console() == null;
    }

    /**
     * Prints the <cite>"Can not open …"</cite> error message followed by the message in the given exception.
     *
     * @param fileIndex  index in the {@link #files} list of the file that can not be opened.
     * @param e          the exception which occurred.
     */
    final void canNotOpen(final int fileIndex, final Exception e) {
        error(Errors.format(Errors.Keys.CanNotOpen_1, files.get(fileIndex)), e);
    }

    /**
     * Prints the given error message followed by the message in the given exception.
     *
     * @param message  the message to print before the exception, or {@code null}.
     * @param e        the exception which occurred.
     */
    final void error(final String message, final Exception e) {
        out.flush();
        if (debug) {
            e.printStackTrace(err);
        } else {
            err.println(Exceptions.formatChainedMessages(locale, message, e));
        }
    }

    /**
     * Shows the help instructions for a specific command. This method is invoked
     * instead of {@link #run()} if the the user provided the {@code --help} option.
     *
     * @param commandName  the command name converted to lower cases.
     */
    protected void help(final String commandName) {
        new HelpCommand(this).help(false, new String[] {commandName}, validOptions);
    }

    /**
     * Executes the sub-command.
     *
     * @return 0 on success, or an exit code if the command failed for a reason other than a Java exception.
     * @throws Exception if an error occurred while executing the sub-command.
     */
    public abstract int run() throws Exception;
}
