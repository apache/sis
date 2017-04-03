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

import java.util.EnumSet;
import java.util.ResourceBundle;
import java.io.IOException;
import org.apache.sis.io.TableAppender;
import org.apache.sis.util.resources.Vocabulary;


/**
 * The "help" subcommand.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.3
 * @module
 */
final class HelpCommand extends CommandRunner {
    /**
     * The commands, in the order to be shown.
     */
    private static final String[] COMMANDS = {
        "help",
        "about",
        "mime-type",
        "metadata",
        "crs",
        "identifier",
        "transform"
    };

    /**
     * Copies the configuration of the given sub-command.
     * This constructor is used for printing help about another command.
     */
    HelpCommand(final CommandRunner parent) {
        super(parent);
    }

    /**
     * Creates the {@code "help"} sub-command.
     *
     * @param  commandIndex  index of the {@code arguments} element containing the {@code "help"} command name, or -1 if none.
     * @param  arguments     the command-line arguments provided by the user.
     * @throws InvalidOptionException if an illegal option has been provided, or the option has an illegal value.
     */
    HelpCommand(final int commandIndex, final String... arguments) throws InvalidOptionException {
        super(commandIndex, arguments, EnumSet.of(Option.LOCALE, Option.ENCODING, Option.HELP, Option.DEBUG));
    }

    /**
     * Prints the help instructions.
     *
     * @return 0 on success, or an exit code if the command failed for a reason other than a Java exception.
     * @throws IOException should never happen, because we are writing to a {@code PrintWriter}.
     */
    @Override
    public int run() throws IOException {
        if (hasUnexpectedFileCount(0, 0)) {
            return Command.INVALID_ARGUMENT_EXIT_CODE;
        }
        help(true, COMMANDS, EnumSet.allOf(Option.class));
        return 0;
    }

    /**
     * Implementation of {@link #run()}, also shared by {@link CommandRunner#help(String)}.
     *
     * @param  showHeader    {@code true} for printing the "Apache SIS" header.
     * @param  commandNames  the names of the commands to list.
     * @param  validOptions  the options to list.
     * @throws IOException should never happen, because we are writing to a {@code PrintWriter}.
     */
    void help(final boolean showHeader, final String[] commandNames, final EnumSet<Option> validOptions) throws IOException {
        final ResourceBundle commands = ResourceBundle.getBundle("org.apache.sis.console.Commands", locale);
        final ResourceBundle options  = ResourceBundle.getBundle("org.apache.sis.console.Options",  locale);
        final Vocabulary vocabulary = Vocabulary.getResources(locale);
        if (showHeader) {
            out.print("Apache SIS, ");
            out.println(commands.getString("SIS"));
            out.println(commands.getString("Usage"));
            out.println();
            out.print(vocabulary.getString(Vocabulary.Keys.Commands));
            out.println(':');
        }
        final TableAppender table = new TableAppender(out, "  ");
        for (final String command : commandNames) {
            if (showHeader) {
                table.append("  ");
            }
            table.append(command);
            if (!showHeader) {
                table.append(':');
            }
            table.nextColumn();
            table.append(commands.getString(command));
            table.nextLine();
        }
        table.flush();
        out.println();
        out.print(vocabulary.getString(Vocabulary.Keys.Options));
        out.println(':');
        for (final Option option : validOptions) {
            final String name = option.label();
            table.append("  ").append(Option.PREFIX).append(name);
            table.nextColumn();
            table.append(options.getString(name));
            table.nextLine();
        }
        table.flush();
    }
}
