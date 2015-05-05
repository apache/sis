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
import org.apache.sis.setup.About;
import org.apache.sis.util.Version;
import org.apache.sis.util.resources.Vocabulary;


/**
 * The "about" subcommand.
 * By default this sub-command prints all information except the {@link About#LIBRARIES} section,
 * because the later is considered too verbose. Available options are:
 *
 * <ul>
 *   <li>{@code --brief}:   prints only Apache SIS version number.</li>
 *   <li>{@code --verbose}: prints all information including the libraries.</li>
 * </ul>
 *
 * <div class="section">About SIS installation on a remote machine</div>
 * This sub-command can provide information about SIS installation on a remote machine,
 * provided that remote access has been enabled at the Java Virtual Machine startup time.
 * See {@link org.apache.sis.console} package javadoc for more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
final class AboutSC extends SubCommand {
    /**
     * Creates the {@code "about"} sub-command.
     */
    AboutSC(final int commandIndex, final String... args) throws InvalidOptionException {
        super(commandIndex, args, EnumSet.of(Option.LOCALE, Option.TIMEZONE, Option.ENCODING,
                Option.BRIEF, Option.VERBOSE, Option.HELP));
    }

    /**
     * Prints the information to the output stream.
     */
    @Override
    public int run() throws Exception {
        /*
         * Check the number of arguments, which can be 0 or 1. If present,
         * the argument is the name and port number of a remote machine.
         *
         * In the current implementation, the --brief option is supported only on the local machine.
         */
        final boolean brief = options.containsKey(Option.BRIEF);
        if (hasUnexpectedFileCount(0, brief ? 0 : 1)) {
            return Command.INVALID_ARGUMENT_EXIT_CODE;
        }
        final String configuration;
        if (brief) {
            configuration = Vocabulary.getResources(locale).getString(
                    Vocabulary.Keys.Version_2, "Apache SIS", Version.SIS);
        } else {
            /*
             * Provide information about the local SIS installation.
             */
            final EnumSet<About> sections = EnumSet.allOf(About.class);
            if (!options.containsKey(Option.VERBOSE)) {
                sections.remove(About.LIBRARIES);
            }
            configuration = About.configuration(sections, locale, timezone).toString();
        }
        out.println(configuration);
        out.flush();
        return 0;
    }
}
