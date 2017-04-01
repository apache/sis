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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.NoSuchFileException;
import java.nio.file.FileSystemNotFoundException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.CharSequences;


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
 * @version 0.8
 * @since   0.4
 * @module
 *
 * @see Files#probeContentType(Path)
 * @see DataStores#probeContentType(Object)
 */
final class MimeTypeCommand extends CommandRunner {
    /**
     * Creates the {@code "mime-type"} sub-command.
     *
     * @param  commandIndex  index of the {@code arguments} element containing the {@code "mime-type"} command name, or -1 if none.
     * @param  arguments     the command-line arguments provided by the user.
     * @throws InvalidOptionException if an illegal option has been provided, or the option has an illegal value.
     */
    MimeTypeCommand(final int commandIndex, final String... arguments) throws InvalidOptionException {
        super(commandIndex, arguments, EnumSet.of(Option.ENCODING, Option.HELP, Option.DEBUG));
    }

    /**
     * Prints mime-type information.
     *
     * @return 0 on success, or an exit code if the command failed for a reason other than an uncaught Java exception.
     * @throws Exception if an error occurred while executing the sub-command.
     */
    @Override
    public int run() throws Exception {
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
                /*
                 * If the URI is not absolute, we will not be able to convert to Path.
                 * Open as a String, leaving the conversion to DataStore implementations.
                 */
                type = DataStores.probeContentType(file);
            } else try {
                type = Files.probeContentType(Paths.get(uri));
            } catch (IllegalArgumentException | FileSystemNotFoundException e) {
                type = DataStores.probeContentType(uri);
            } catch (NoSuchFileException e) {
                error(Errors.format(Errors.Keys.CanNotOpen_1, uri), e);
                return Command.IO_EXCEPTION_EXIT_CODE;
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
