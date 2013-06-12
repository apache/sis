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
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
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
    public int run() {
        if (hasUnexpectedFileCount(0, 0)) {
            return Command.INVALID_ARGUMENT_EXIT_CODE;
        }
        final String configuration;
        if (options.containsKey(Option.BRIEF)) {
            configuration = Vocabulary.getResources(locale).getString(
                    Vocabulary.Keys.Version_2, "Apache SIS", Version.SIS);
        } else {
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
