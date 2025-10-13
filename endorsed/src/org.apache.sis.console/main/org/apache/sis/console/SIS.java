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

import java.util.EnumMap;
import java.io.PrintWriter;
import org.apache.sis.util.Version;
import org.apache.sis.util.Printable;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.system.Environment;


/**
 * Entry point for {@code SIS} commands from JShell.
 * This class provides the same commands as the {@code SIS} shell script, but from Java code.
 * Each method accepts an arbitrary number of arguments of type {@link Object}.
 * The actual argument values should be instances of {@link String},
 * but the arguments that are input or output files can also be instances of
 * {@link java.io.File}, {@link java.nio.file.Path}, {@link java.net.URL}, {@link java.net.URI}
 * or any other type recognized by {@link org.apache.sis.storage.StorageConnector}.
 *
 * <p>This class should not be used in Java applications.
 * This class is provided for usage in JShell environment.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public final class SIS {
    /*
     * Usages of `Console#writer()` within JShell seems incompatible with JShell own writer.
     * Problems observed with Java 21 on Linux when printing non-ASCII characters.
     */
    static {
        Environment.avoidConsoleWriter();
    }

    /**
     * Builder for calls to the SIS command-line. Each sub-command is represented by a subclasses of {@code Builder}.
     * A unique instance for each command is provided as fields in {@link SIS}, and that instance is cloned when the
     * user wants to add options.
     *
     * @param  <C>  the sub-class for the command builder.
     */
    private static abstract class Builder<C extends Builder<C>> implements Cloneable {
        /**
         * Name of the sub-command. Shall be one of the names recognized by {@link Command}.
         */
        private final String command;

        /**
         * The options added by the user, or {@code null} if this instance is a field of the {@link SIS} class.
         * In the latter case, this builder shall be considered immutable and options shall be added in a clone.
         */
        private EnumMap<Option,Object> options;

        /**
         * Creates a new builder for an immutable instance to be assigned to a {@code SIS} field.
         *
         * @param  command  name of the sub-command. Shall be one of the names recognized by {@link Command}.
         */
        Builder(final String command) {
            this.command = command;
        }

        /**
         * Sets the value of an option.
         *
         * @param  key    the option.
         * @param  value  value for the specified option.
         * @return the builder on which to perform chained method calls.
         */
        @SuppressWarnings("unchecked")
        final C set(final Option key, Object value) {
            if (key.hasValue && (value = trim(value)) == null) {
                final String option = key.label();
                throw new IllegalArgumentException(Errors.format(Errors.Keys.MissingValueForOption_1, option));
            }
            Builder<C> target = this;
            if (options == null) try {
                target = (Builder<C>) clone();
                target.options = new EnumMap<>(Option.class);
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
            target.options.put(key, value);
            return (C) target;
        }

        /**
         * If the given value is a character string, trims it and returns either a non-empty string or null.
         * Otherwise returns the value unchanged.
         */
        private static Object trim(final Object value) {
            if (value instanceof CharSequence) {
                final String s = value.toString().trim();
                return s.isBlank() ? null : s;
            }
            return value;
        }

        /**
         * Prints full stack trace in case of failure.
         *
         * @return a new builder or {@code this}, for method call chaining.
         */
        public C debug() {
            return set(Option.DEBUG, null);
        }

        /**
         * Lists the options available for the sub-command.
         *
         * @return a new builder or {@code this}, for method call chaining.
         */
        public C help() {
            return set(Option.HELP, null);
        }

        /**
         * Executes the command with the given arguments. The arguments are usually {@link String} instances,
         * but may also be instances of {@link java.io.File}, {@link java.nio.file.Path}, {@link java.net.URL},
         * {@link java.net.URI} or other types accepted by {@link org.apache.sis.storage.StorageConnector} if
         * the corresponding argument specifies an input or output.
         *
         * @param  name  name of the sub-command to execute.
         * @param  args  the arguments to pass to the sub-command.
         * @throws Exception if an error occurred while executing the command.
         */
        @SuppressWarnings("UseOfSystemOutOrSystemErr")
        public void run(final Object... args) throws Exception {
            /*
             * Count the number of arguments to insert at the beginning,
             * including the argument for the sub-command name.
             */
            int i = 1;
            if (options != null) {
                for (Option key : options.keySet()) {
                    i += (key.hasValue ? 2 : 1);
                }
            }
            /*
             * Copy the sub-command name, the options, then the arguments specified to this method.
             */
            final var allArgs = new Object[args.length + i];
            i = 0;
            allArgs[i++] = command;
            if (options != null) {
                for (final EnumMap.Entry<Option,Object> entry : options.entrySet()) {
                    final Option key = entry.getKey();
                    allArgs[i++] = Option.PREFIX.concat(key.label());
                    if (key.hasValue) {
                        allArgs[i++] = entry.getValue();
                    }
                }
            }
            System.arraycopy(args, 0, allArgs, i, args.length);
            /*
             * Prints an echo of the command to execute, then execute.
             * The operation may fail without throwing an exception.
             */
            var c = new Command(allArgs);
            final PrintWriter out = c.writer(false);
            c.setFaintOutput(true);
            out.print("command> sis");
            for (i=0; i < allArgs.length; i++) {
                final String arg = allArgs[i].toString();
                final int start = arg.startsWith(Option.PREFIX) ? Option.PREFIX.length() : 0;
                final boolean quote = !CharSequences.isUnicodeIdentifier(arg.substring(start));
                out.print(' ');
                if (quote) out.print('"');
                out.print(arg.replace("\"", "\\\""));
                if (quote) out.print('"');
            }
            c.setFaintOutput(false);
            out.println();
            int status = c.run();
            if (status != 0) {
                c.writer(true).println("Error code " + status);
            }
        }

        /**
         * Returns the command with all options that have been set.
         */
        @Override
        public String toString() {
            if (options == null) return command;
            final var sb = new StringBuilder(command);
            options.forEach((key, value) -> sb.append(' ').append(Option.PREFIX).append(key.label()).append(' ').append(value));
            return sb.toString();
        }
    }

    /**
     * Do not allow instantiation of this class.
     */
    private SIS() {
    }

    /**
     * Returns a string representation of the Apache SIS version.
     *
     * @see Version#SIS
     */
    public static String version() {
        return Version.SIS.toString();
    }




    /**
     * Shows a help overview.
     * This sub-command prints the same text as when {@code SIS} is invoked on the command-line without arguments.
     * Usage example:
     *
     * {@snippet lang="java" :
     *     SIS.HELP.run();
     *     }
     */
    public static final Help HELP = new Help();

    /**
     * Builder for the "help" sub-command. This builder provides convenience methods
     * for setting options before to execute the command by a call to {@link #run(Object...)}.
     *
     * @see #HELP
     */
    public static final class Help extends Builder<Help> {
        /** Creates the unique instance. */
        Help() {super("help");}

        /**
         * Sets the locale to use for the command output.
         *
         * @param  value  the language and country code.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Help locale(String value) {
            return set(Option.LOCALE, value);
        }

        /**
         * Sets encoding to use for the command outputs.
         * This option rarely needs to be specified.
         *
         * @param  value  the character set name.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Help encoding(String value) {
            return set(Option.ENCODING, value);
        }
    }




    /**
     * Shows information about Apache SIS and system configuration.
     * By default this sub-command prints all information except the section about dependencies.
     * Some available options are:
     *
     * <ul>
     *   <li>{@code --brief}:   prints only Apache SIS version number.</li>
     *   <li>{@code --verbose}: prints all information including the libraries.</li>
     * </ul>
     *
     * Usage example:
     *
     * {@snippet lang="java" :
     *     SIS.ABOUT.verbose().run();
     *     }
     */
    public static final About ABOUT = new About();

    /**
     * Builder for the "about" sub-command. This builder provides convenience methods
     * for setting options before to execute the command by a call to {@link #run(Object...)}.
     *
     * @see #ABOUT
     */
    public static final class About extends Builder<About> {
        /** Creates the unique instance. */
        About() {super("about");}

        /**
         * Sets the locale to use for the command output.
         *
         * @param  value  the language and country code.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public About locale(String value) {
            return set(Option.LOCALE, value);
        }

        /**
         * Sets the timezone for the dates to be formatted.
         *
         * @param  value  the time zone identifier.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public About timezone(String value) {
            return set(Option.TIMEZONE, value);
        }

        /**
         * Sets encoding to use for the command outputs.
         * This option rarely needs to be specified.
         *
         * @param  value  the character set name.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public About encoding(String value) {
            return set(Option.ENCODING, value);
        }

        /**
         * Reduces the output to only brief information.
         *
         * @return a new builder or {@code this}, for method call chaining.
         */
        public About brief() {
            return set(Option.BRIEF, null);
        }

        /**
         * Requests the output to contain more detailed information.
         *
         * @return a new builder or {@code this}, for method call chaining.
         */
        public About verbose() {
            return set(Option.VERBOSE, null);
        }
    }




    /**
     * Shows MIME type for the given file.
     * This sub-command reproduces the functionality of the following Unix command,
     * except that {@code MimeTypeCommand} uses the SIS detection mechanism instead of the OS one.
     *
     * {@snippet lang="shell" :
     *   file --mime-type <files>
     *   }
     *
     * Arguments other than options are files, usually as character strings but can also be
     * {@link java.io.File}, {@link java.nio.file.Path} or {@link java.net.URL} for example.
     * Usage example:
     *
     * {@snippet lang="java" :
     *     SIS.MIME_TYPE.run("data.xml");
     *     }
     */
    public static final MimeType MIME_TYPE = new MimeType();

    /**
     * Builder for the "mime-type" sub-command. This builder provides convenience methods
     * for setting options before to execute the command by a call to {@link #run(Object...)}.
     *
     * @see #MIME_TYPE
     */
    public static final class MimeType extends Builder<MimeType> {
        /** Creates the unique instance. */
        MimeType() {super("mime-type");}

        /**
         * Sets encoding to use for the command outputs.
         * This option rarely needs to be specified.
         *
         * @param  value  the character set name.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public MimeType encoding(String value) {
            return set(Option.ENCODING, value);
        }
    }

    /**
     * Shows ISO 19115 metadata information for the given file.
     * Some available options are:
     *
     * <ul>
     *   <li>{@code --format}: the output format (text, XML or GPX).</li>
     * </ul>
     *
     * Arguments other than options are files, usually as character strings but can also be
     * {@link java.io.File}, {@link java.nio.file.Path} or {@link java.net.URL} for example.
     * Usage example:
     *
     * {@snippet lang="java" :
     *     SIS.METADATA.format("xml").run("data.xml");
     *     }
     */
    public static final Metadata METADATA = new Metadata("metadata");

    /**
     * Shows Coordinate Reference System (CRS) information for the given file.
     * CRS are considered as a kind of metadata.
     * Some available options are:
     *
     * <ul>
     *   <li>{@code --format}: the output format (WKT or XML).</li>
     * </ul>
     *
     * Arguments other than options are files, usually as character strings but can also be
     * {@link java.io.File}, {@link java.nio.file.Path} or {@link java.net.URL} for example.
     * Usage example:
     *
     * {@snippet lang="java" :
     *     SIS.CRS.format("wkt").run("data.xml");
     *     }
     */
    public static final Metadata CRS = new Metadata("crs");

    /**
     * Builder for the "metadata" and "crs" sub-commands. This builder provides convenience methods
     * for setting options before to execute the command by a call to {@link #run(Object...)}.
     *
     * @see #METADATA
     * @see #CRS
     */
    public static final class Metadata extends Builder<Metadata> {
        /** Creates the instance for metadata or CRS. */
        Metadata(String command) {super(command);}

        /**
         * Sets the path to auxiliary metadata, relative to the main file.
         * The {@code '*'} character stands for the name of the main file.
         * For example if the main file is {@code "city-center.tiff"},
         * then {@code "*.xml"} stands for {@code "city-center.xml"}.
         *
         * @param  value  relative path to auxiliary metadata.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Metadata metadata(String value) {
            return set(Option.METADATA, value);
        }

        /**
         * Sets the output format.
         *
         * @param  value  the format. Examples: xml, wkt, wkt1 or text.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Metadata format(String value) {
            return set(Option.FORMAT, value);
        }

        /**
         * Sets the locale to use for the command output.
         *
         * @param  value  the language and country code.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Metadata locale(String value) {
            return set(Option.LOCALE, value);
        }

        /**
         * Sets the timezone for the dates to be formatted.
         *
         * @param  value  the time zone identifier.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Metadata timezone(String value) {
            return set(Option.TIMEZONE, value);
        }

        /**
         * Sets encoding to use for the command outputs.
         * This option rarely needs to be specified.
         *
         * @param  value  the character set name.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Metadata encoding(String value) {
            return set(Option.ENCODING, value);
        }

        /**
         * Sets whether colorized output shall be enabled.
         *
         * @param  enabled  whether colors are enabled.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Metadata colors(boolean enabled) {
            return set(Option.COLORS, enabled);
        }

        /**
         * Requests the output to contain more detailed information.
         *
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Metadata verbose() {
            return set(Option.VERBOSE, null);
        }
    }




    /**
     * Shows identifiers for metadata and referencing systems in the given file.
     * Arguments other than options are files, usually as character strings but can also be
     * {@link java.io.File}, {@link java.nio.file.Path} or {@link java.net.URL} for example.
     * Usage example:
     *
     * {@snippet lang="java" :
     *     SIS.IDENTIFIER.run("data.xml");
     *     }
     */
    public static final Identifier IDENTIFIER = new Identifier();

    /**
     * Builder for the "identifier" sub-command. This builder provides convenience methods
     * for setting options before to execute the command by a call to {@link #run(Object...)}.
     *
     * @see #IDENTIFIER
     */
    public static final class Identifier extends Builder<Identifier> {
        /** Creates the unique instance. */
        Identifier() {super("identifier");}

        /**
         * Sets the locale to use for the command output.
         *
         * @param  value  the language and country code.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Identifier locale(String value) {
            return set(Option.LOCALE, value);
        }

        /**
         * Sets encoding to use for the command outputs.
         * This option rarely needs to be specified.
         *
         * @param  value  the character set name.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Identifier encoding(String value) {
            return set(Option.ENCODING, value);
        }

        /**
         * Sets whether colorized output shall be enabled.
         *
         * @param  enabled  whether colors are enabled.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Identifier colors(boolean enabled) {
            return set(Option.COLORS, enabled);
        }

        /**
         * Requests the output to contain more detailed information.
         *
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Identifier verbose() {
            return set(Option.VERBOSE, null);
        }
    }




    /**
     * Converts or transform coordinates from given source CRS to target CRS.
     * The output uses comma separated values (CSV) format,
     * with {@code '#'} as the first character of comment lines.
     * The source and target CRS are mandatory and can be specified
     * as EPSG codes, WKT strings, or metadata read from data files.
     * Those information are passed as options:
     *
     * <ul>
     *   <li>{@code --sourceCRS}: the coordinate reference system of input points.</li>
     *   <li>{@code --targetCRS}: the coordinate reference system of output points.</li>
     *   <li>{@code --operation}: the coordinate operation from source CRS to target CRS.</li>
     * </ul>
     *
     * The {@code --operation} parameter is optional.
     * If provided, then the {@code --sourceCRS} and {@code --targetCRS} parameters become optional.
     * If the operation is specified together with the source and/or target CRS, then the operation
     * is used in the middle and conversions from/to the specified CRS are concatenated before/after
     * the specified operation.
     *
     * <p>Arguments other than options are files, usually as character strings, but can also be
     * {@link java.io.File}, {@link java.nio.file.Path} or {@link java.net.URL} for example.
     * Usage example:</p>
     *
     * {@snippet lang="java" :
     *     SIS.TRANSFORM.sourceCRS("EPSG:3395").targetCRS("EPSG:4326").run("data.txt");
     *     }
     */
    public static final Transform TRANSFORM = new Transform();

    /**
     * Builder for the "transform" sub-command. This builder provides convenience methods
     * for setting options before to execute the command by a call to {@link #run(Object...)}.
     *
     * @see #TRANSFORM
     */
    public static final class Transform extends Builder<Transform> {
        /** Creates the unique instance. */
        Transform() {super("transform");}

        /**
         * Sets the Coordinate Reference System of input data.
         *
         * @param  value  the EPSG code, WKT or file from which to get the CRS.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Transform sourceCRS(Object value) {
            return set(Option.SOURCE_CRS, value);
        }

        /**
         * Sets the Coordinate Reference System of output data.
         *
         * @param  value  the EPSG code, WKT or file from which to get the CRS.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Transform targetCRS(Object value) {
            return set(Option.TARGET_CRS, value);
        }

        /**
         * Sets the Coordinate Operation to use.
         *
         * @param  value  the EPSG code, WKT or file from which to get the coordinate operation.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Transform operation(Object value) {
            return set(Option.OPERATION, value);
        }

        /**
         * Use the inverse of the coordinate operation. The transform will be inverted <em>after</em> all
         * other options ({@code operation}, {@code sourceCRS} and {@code targetCRS}) have been applied.
         *
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Transform inverse() {
            return set(Option.INVERSE, null);
        }

        /**
         * Sets the locale to use for the command output.
         *
         * @param  value  the language and country code.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Transform locale(String value) {
            return set(Option.LOCALE, value);
        }

        /**
         * Sets the timezone for the dates to be formatted.
         *
         * @param  value  the time zone identifier.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Transform timezone(String value) {
            return set(Option.TIMEZONE, value);
        }

        /**
         * Sets encoding to use for the command outputs.
         * This option rarely needs to be specified.
         *
         * @param  value  the character set name.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Transform encoding(String value) {
            return set(Option.ENCODING, value);
        }

        /**
         * Sets whether colorized output shall be enabled.
         *
         * @param  enabled  whether colors are enabled.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Transform colors(boolean enabled) {
            return set(Option.COLORS, enabled);
        }

        /**
         * Requests the output to contain more detailed information.
         *
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Transform verbose() {
            return set(Option.VERBOSE, null);
        }
    }




    /**
     * Rewrites a data file in another format.
     * If more than one source file is specified, then all those files are aggregated in the output file.
     * This is possible only if the output format supports the storage of an arbitrary number of resources.
     * Some options are:
     *
     * <ul>
     *   <li>{@code --output}: the file where to write the image.</li>
     * </ul>
     *
     * Arguments are usually character strings but can also be
     * {@link java.io.File}, {@link java.nio.file.Path} or {@link java.net.URL} for example.
     * Usage example:
     *
     * {@snippet lang="java" :
     *     SIS.TRANSLATE.output("data.tiff").run("data.png");
     *     }
     */
    public static final Translate TRANSLATE = new Translate();

    /**
     * Builder for the "translate" sub-command. This builder provides convenience methods
     * for setting options before to execute the command by a call to {@link #run(Object...)}.
     *
     * @see #TRANSLATE
     */
    public static final class Translate extends Builder<Translate> {
        /** Creates the unique instance. */
        Translate() {super("translate");}

        /**
         * Sets the path to auxiliary metadata, relative to the main file.
         * The {@code '*'} character stands for the name of the main file.
         * For example if the main file is {@code "city-center.tiff"},
         * then {@code "*.xml"} stands for {@code "city-center.xml"}.
         *
         * @param  value  relative path to auxiliary metadata.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Translate metadata(String value) {
            return set(Option.METADATA, value);
        }

        /**
         * Sets the destination file.
         *
         * @param  value  the output file.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Translate output(Object value) {
            return set(Option.OUTPUT, value);
        }

        /**
         * Sets the output format.
         *
         * @param  value  the format. Examples: xml, wkt, wkt1 or text.
         * @return a new builder or {@code this}, for method call chaining.
         */
        public Translate format(String value) {
            return set(Option.FORMAT, value);
        }
    }




    /**
     * Prints the given object to the standard output stream.
     *
     * @param  value  the object to print.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void print(final Object value) {
        if (value instanceof Printable) {
            ((Printable) value).print();
        } else {
            final PrintWriter out = Environment.writer(System.console(), System.out);
            out.println(value);
            out.flush();
        }
    }
}
