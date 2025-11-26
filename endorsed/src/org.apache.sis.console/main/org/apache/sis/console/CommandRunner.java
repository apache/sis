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

import java.time.ZoneId;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.EnumSet;
import java.util.EnumMap;
import java.util.TimeZone;
import java.io.Console;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import org.apache.sis.system.Environment;
import org.apache.sis.util.Locales;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.X364;
import org.apache.sis.pending.jdk.JDK22;
import org.apache.sis.storage.DataOptionKey;
import org.apache.sis.storage.StorageConnector;


/**
 * Base class of all sub-commands.
 * A subclasses is initialized by the {@link Command} constructor,
 * then the {@link #run()} method is invoked by {@link Command#run()}.
 *
 * @author  Martin Desruisseaux (Geomatys)
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
     * instantiated by us, so we cannot pass the {@code CommandRunner} instance to its constructor.
     */
    static final ThreadLocal<CommandRunner> instance = new ThreadLocal<>();

    /**
     * The name of this command, as specified by the user on the command-line.
     * May contain a mix of lower-case and upper-case letters if the user specified the command that way.
     */
    protected final String commandName;

    /**
     * The set of legal options for this command.
     *
     * @see #help(String)
     */
    private final EnumSet<Option> validOptions;

    /**
     * The command-line options allowed by this sub-command, together with their values.
     * Values are usually instances of {@link String}, but other types are allowed when
     * the values is expected to be a file.
     */
    protected final EnumMap<Option,Object> options;

    /**
     * The locale specified by the {@code "--locale"} option. If no such option was provided,
     * then this field is set to the {@linkplain Locale#getDefault() default locale}.
     */
    protected final Locale locale;

    /**
     * The locale specified by the {@code "--timezone"} option, or null if no timezone was specified.
     * The null value may be interpreted as the {{@linkplain ZoneId#systemDefault() default timezone}
     * or as UTC, depending on the context. For example, WKT parsing and formatting use UTC unless
     * specified otherwise.
     *
     * @see #getTimeZone()
     */
    protected final ZoneId timezone;

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
     * Values are always instances of {@link String} when SIS is executed from bash,
     * but may be other kinds of object when {@link SIS} is executed from JShell.
     */
    protected final List<Object> files;

    /**
     * Copies the configuration of the given sub-command. This constructor is used
     * only when a command needs to delegates part of its work to another command.
     */
    CommandRunner(final CommandRunner parent) {
        this.commandName  = parent.commandName;
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
     * The {@code arguments} array is the same array as the one given to the {@code main(String[])} method.
     * The argument at index {@code commandIndex} is the name of this command, and will be ignored except for
     * the special {@value #TEST} value which is used only at JUnit testing time.
     *
     * @param  commandIndex  index of the {@code arguments} element containing the sub-command name, or -1 if none.
     * @param  arguments     the command-line arguments provided by the user.
     * @param  validOptions  the command-line options allowed by this sub-command.
     * @throws InvalidOptionException if an illegal option has been provided, or the option has an illegal value.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    protected CommandRunner(final int commandIndex, final Object[] arguments, final EnumSet<Option> validOptions)
            throws InvalidOptionException
    {
        commandName = (commandIndex >= 0) ? arguments[commandIndex].toString() : null;
        this.validOptions = validOptions;
        options = new EnumMap<>(Option.class);
        files = new ArrayList<>(arguments.length);
        for (int i=0; i < arguments.length; i++) {
            if (i == commandIndex) {
                continue;
            }
            final Object arg = arguments[i];
            final String s;
            if (arg instanceof CharSequence && (s = arg.toString()).startsWith(Option.PREFIX)) {
                final String name = s.substring(Option.PREFIX.length());
                final Option option = Option.forLabel(name);
                if (!validOptions.contains(option)) {
                    throw new InvalidOptionException(Errors.format(Errors.Keys.UnknownOption_1, name), name);
                }
                Object value = null;
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
        Object value  = null;
        final Console console;
        final boolean explicitEncoding;
        try {
            debug = options.containsKey(option = Option.DEBUG);

            String s;
            value = s = getOptionAsString(option = Option.LOCALE);
            locale = (s != null) ? Locales.parse(s) : Locale.getDefault(Locale.Category.DISPLAY);

            value = s = getOptionAsString(option = Option.TIMEZONE);
            timezone = (s != null) ? ZoneId.of(s) : null;

            value = s = getOptionAsString(option = Option.ENCODING);
            explicitEncoding = (s != null);
            encoding = explicitEncoding ? Charset.forName(s) : Charset.defaultCharset();

            value = options.get(option = Option.COLORS);
            console = System.console();
            colors = (value != null) ? Option.COLORS.parseBoolean(value)
                    : (console != null) && JDK22.isTerminal(console) && X364.isAnsiSupported();
        } catch (RuntimeException e) {
            @SuppressWarnings("null")                   // `option` has been assigned in `get` argument.
            final String name = option.label();
            throw new InvalidOptionException(Errors.format(Errors.Keys.IllegalOptionValue_2, name, value), name);
        }
        /*
         * Creates the writers. If this sub-command is created for JUnit test purpose, then we will send the
         * output to a StringBuffer. Otherwise the output will be sent to the java.io.Console if possible,
         * or to the standard output stream otherwise.
         */
        if (TEST.equals(commandName)) {
            final var s = new StringWriter();
            outputBuffer = s.getBuffer();
            out = new PrintWriter(s);
            err = out;
        } else {
            outputBuffer = null;
            err = Environment.writer(console, System.err);
            if (explicitEncoding) {
                out = new PrintWriter(new OutputStreamWriter(System.out, encoding), true);
            } else {
                out = Environment.writer(console, System.out);
            }
        }
    }

    /**
     * Returns a non-null timezone, either the specified timezone or the default one.
     * This method is invoked when a null {@link #timezone} would be interpreted as UTC,
     * but the {@linkplain TimeZone#getDefault() default timezone} is preferred instead.
     *
     * @return the timezone to use.
     */
    protected final TimeZone getTimeZone() {
        return (timezone != null) ? TimeZone.getTimeZone(timezone) : TimeZone.getDefault();
    }

    /**
     * Returns the value of the specified option as a character string.
     *
     * @param  key  the option for which to get a value.
     * @return the requested option, or {@code null} if not present.
     * @throws InvalidOptionException if the value is not a character string.
     */
    final String getOptionAsString(final Option key) throws InvalidOptionException {
        final Object value = options.get(key);
        if (value == null) return null;
        if (value instanceof CharSequence) {
            return value.toString();
        }
        throw invalidOption(key, value, null);
    }

    /**
     * Returns the value of the specified option as a path.
     *
     * @param  key  the option for which to get a value.
     * @return the requested option, or {@code null} if not present.
     * @throws InvalidOptionException if the value is not convertible to a path.
     */
    final Path getOptionAsPath(final Option key) throws InvalidOptionException {
        final Object value = options.get(key);
        if (value == null) return null;
        if (value instanceof Path) {
            return (Path) value;
        }
        Throwable cause = null;
        if (value instanceof CharSequence) try {
            return Path.of(value.toString());
        } catch (InvalidPathException e) {
            cause = e;
        }
        throw invalidOption(key, value, cause);
    }

    /**
     * Return the value of a mandatory option.
     *
     * @param  option  the option to fetch.
     * @return the option value, never {@code null}.
     * @throws InvalidOptionException if the option is missing.
     */
    final Object getMandatoryOption(final Option option) throws InvalidOptionException {
        final Object value = options.get(option);
        if (value == null) {
            final String name = option.label();
            throw new InvalidOptionException(Errors.format(Errors.Keys.MissingValueForOption_1, name), name);
        }
        return value;
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
     * Prints the given color to the standard output stream if ANSI X3.64
     * escape sequences are enabled, or does nothing otherwise.
     *
     * @param  code  the ANSI X3.64 color to print.
     */
    final void color(final X364 code) {
        color(colors, out, code);
    }

    /**
     * Prints the given color to the given stream if ANSI X3.64
     * escape sequences are enabled, or does nothing otherwise.
     *
     * @param  colors  the condition for printing the color. Usually {@link #colors}.
     * @param  out     where to print the ANSI sequence. Usually {@link #out} or {@link #err}.
     * @param  code    the ANSI X3.64 color to print.
     */
    static void color(final boolean colors, final PrintWriter out, final X364 code) {
        if (colors) {
            out.print(code.sequence());
        }
    }

    /**
     * Returns {@code true} if the command should use the standard input.
     */
    final boolean useStandardInput() {
        return files.isEmpty() && System.console() == null;
    }

    /**
     * Returns a storage connector for the specified input.
     * This method should be invoked only for the input, not for the output, because it uses input-specific options.
     * Conversely, this storage connector does <strong>not</strong> include the options intended for console output
     * such as {@linkplain #locale}, {@link #timezone} and {@linkplain #encoding}.
     *
     * @param  input  the storage input.
     * @return storage connector for the specified input.
     * @throws InvalidOptionException if an option has an invalid value.
     */
    final StorageConnector inputConnector(final Object input) throws InvalidOptionException {
        final var connector = new StorageConnector(input);
        final Path p = getOptionAsPath(Option.METADATA);
        if (p != null) {
            connector.setOption(DataOptionKey.METADATA_PATH, p);
        }
        return connector;
    }

    /**
     * Returns the exception to throw for an invalid option.
     *
     * @param  key    the requested option.
     * @param  value  the value associated to the specified option.
     * @param  cause  cause of the invalidity, or {@code null} if none.
     * @return the exception to throw.
     */
    private static InvalidOptionException invalidOption(Option key, Object value, Throwable cause) {
        final String name = key.label();
        return new InvalidOptionException(Errors.format(Errors.Keys.IllegalOptionValue_2, name, value), cause, name);
    }

    /**
     * Prints the <q>Cannot open …</q> error message followed by the message in the given exception.
     *
     * @param fileIndex  index in the {@link #files} list of the file that cannot be opened.
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
        err.flush();
    }

    /**
     * Shows the help instructions for a specific command. This method is invoked instead of {@link #run()}
     * if the user provided the {@code --help} option. The default implementation builds a description
     * from the texts associated to the given {@code resourceKey} in various resource bundles provided in
     * this {@code org.apache.sis.console} module. Subclasses can override if needed.
     *
     * @param  resourceKey  the key for the resource to print. This is usually {@link #commandName} in lower-cases.
     * @throws IOException should never happen, because we are writing to a {@code PrintWriter}.
     */
    protected void help(final String resourceKey) throws IOException {
        new HelpCommand(this).help(false, new String[] {resourceKey}, validOptions);
    }

    /**
     * Executes the sub-command.
     *
     * @return 0 on success, or an exit code if the command failed for a reason other than an uncaught Java exception.
     * @throws Exception if an error occurred while executing the sub-command.
     */
    public abstract int run() throws Exception;

    /**
     * Flushes any pending information to the output streams.
     * The default information flushes {@link #out} and {@link #err} in that order.
     * Subclasses may override if there is more things to flush.
     */
    protected void flush() {
        out.flush();
        err.flush();
    }
}
