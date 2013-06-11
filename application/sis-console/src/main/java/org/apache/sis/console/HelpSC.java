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
import java.util.Locale;
import java.util.ResourceBundle;
import java.io.IOException;
import org.apache.sis.io.TableAppender;
import org.apache.sis.util.resources.Vocabulary;


/**
 * The "help" subcommand.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class HelpSC extends SubCommand {
    /**
     * The commands, in the order to be shown.
     */
    private static final String[] COMMANDS = {
        "help",
        "about"
    };

    /**
     * Creates the {@code "help"} sub-command.
     */
    HelpSC(final int commandIndex, final String... args) throws InvalidOptionException {
        super(commandIndex, args, EnumSet.of(Option.LOCALE, Option.ENCODING));
    }

    /**
     * Prints the help instructions.
     */
    @Override
    public void run() {
        final ResourceBundle commands = ResourceBundle.getBundle("org.apache.sis.console.Commands", locale);
        final ResourceBundle options  = ResourceBundle.getBundle("org.apache.sis.console.Options", locale);
        final Vocabulary vocabulary = Vocabulary.getResources(locale);
        out.print("Apache SIS, ");
        out.println(commands.getString("SIS"));
        out.println(commands.getString("Usage"));
        out.println();
        out.print(vocabulary.getString(Vocabulary.Keys.Commands));
        out.println(':');
        try {
            final TableAppender table = new TableAppender(out, "  ");
            for (final String command : COMMANDS) {
                table.append("  ").append(command);
                table.nextColumn();
                table.append(commands.getString(command));
                table.nextLine();
            }
            table.flush();
            out.println();
            out.print(vocabulary.getString(Vocabulary.Keys.Options));
            out.println(':');
            for (final Option option : Option.values()) {
                final String name = option.name().toLowerCase(Locale.US);
                table.append("  ").append(Option.PREFIX).append(name);
                table.nextColumn();
                table.append(options.getString(name));
                table.nextLine();
            }
            table.flush();
        } catch (IOException e) {
            throw new AssertionError(e); // Should never happen, because we are writing to a PrintWriter.
        }
    }
}
