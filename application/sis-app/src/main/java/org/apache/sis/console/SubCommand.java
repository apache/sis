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
import java.util.EnumSet;
import java.util.EnumMap;
import java.io.Console;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import org.apache.sis.util.Locales;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.X364;


/**
 * Base class of sub-commands.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
abstract class SubCommand implements Runnable {
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
     * Creates a new sub-command.
     *
     * @param  args The command-line arguments provided by the user.
     *         The first element in this array will be ignored, because it is assumed to be the sub-command name.
     * @param  validOptions The command-line options allowed by this sub-command.
     * @throws InvalidOptionException If an illegal option has been provided, or the option has an illegal value.
     */
    protected SubCommand(final String[] args, final EnumSet<Option> validOptions) throws InvalidOptionException {
        options = new EnumMap<Option,String>(Option.class);
        for (int i=1; i<args.length; i++) {
            final String arg = args[i];
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
                    if (++i >= args.length) {
                        throw new InvalidOptionException(Errors.format(Errors.Keys.MissingValueForOption_1, name), name);
                    }
                    value = args[i];
                }
                if (options.containsKey(option)) {
                    throw new InvalidOptionException(Errors.format(Errors.Keys.DuplicatedOption_1, name), name);
                }
                options.put(option, value);
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
            locale = (value != null) ? Locales.parse(value) : Locale.getDefault();

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
         * Creates the writers.
         */
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

    /**
     * Executes the sub-command.
     */
    @Override
    public abstract void run();
}
