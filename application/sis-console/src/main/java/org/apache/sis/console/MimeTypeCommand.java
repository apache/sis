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

import java.net.URI;
import java.util.EnumSet;
import java.io.IOException;
import java.net.URISyntaxException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.CharSequences;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Files;


/**
 * The "mime-type" subcommand.
 * This sub-command reproduces the functionality of the following Unix command,
 * except that {@code MimeTypeCommand} uses the SIS detection mechanism instead than the OS one.
 *
 * {@preformat shell
 *   file --mime-type <files>
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 *
 * @see Files#probeContentType(Path)
 * @see DataStores#probeContentType(Object)
 */
final class MimeTypeCommand extends CommandRunner {
    /**
     * Creates the {@code "mime-type"} sub-command.
     */
    MimeTypeCommand(final int commandIndex, final String... args) throws InvalidOptionException {
        super(commandIndex, args, EnumSet.of(Option.ENCODING, Option.HELP, Option.DEBUG));
    }

    /**
     * Prints mime-type information.
     *
     * @throws IOException If an error occurred while reading the file.
     */
    @Override
    public int run() throws InvalidOptionException, IOException, DataStoreException, URISyntaxException {
        if (hasUnexpectedFileCount(1, Integer.MAX_VALUE)) {
            return Command.INVALID_ARGUMENT_EXIT_CODE;
        }
        /*
         * Computes the width of the first column, which will contain file names.
         */
        int width = 0;
        for (final String file : files) {
            final int length = file.length() + 1;
            if (length > width) {
                width = length;
            }
        }
        /*
         * Now detect and print MIME type.
         */
        for (final String file : files) {
            final URI uri;
            try {
                uri = new URI(file);
            } catch (URISyntaxException e) {
                canNotOpen(0, e);
                return Command.IO_EXCEPTION_EXIT_CODE;
            }
            String type;
            if (!uri.isAbsolute()) {
                // If the URI is not absolute, we will not be able to convert to Path.
                // Open as a String, leaving the conversion to DataStore implementations.
                type = DataStores.probeContentType(file);
            } else {
                type = DataStores.probeContentType(uri);
            }
            /*
             * Output of Unix "file --mime-type" Unix command is of the form:
             *
             *   file: type
             */
            if (type != null) {
                out.print(file);
                out.print(':');
                out.print(CharSequences.spaces(width - file.length()));
                out.println(type);
                out.flush();
            }
        }
        return 0;
    }
}
