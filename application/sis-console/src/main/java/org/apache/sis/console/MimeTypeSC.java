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
import org.apache.sis.util.resources.Errors;

// Related to JDK7.
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.NoSuchFileException;
import java.nio.file.FileSystemNotFoundException;


/**
 * The "mime-type" subcommand.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 *
 * @see Files#probeContentType(Path)
 * @see DataStores#probeContentType(Object)
 */
final class MimeTypeSC extends SubCommand {
    /**
     * Creates the {@code "mime-type"} sub-command.
     */
    MimeTypeSC(final int commandIndex, final String... args) throws InvalidOptionException {
        super(commandIndex, args, EnumSet.of(Option.ENCODING, Option.HELP));
    }

    /**
     * Prints mime-type information.
     *
     * @throws IOException If an error occurred while reading the file.
     */
    @Override
    public int run() throws InvalidOptionException, IOException, DataStoreException, URISyntaxException {
        if (hasUnexpectedFileCount(1, 1)) {
            return Command.INVALID_ARGUMENT_EXIT_CODE;
        }
        final String file = files.get(0);
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
        } else try {
            type = Files.probeContentType(Paths.get(uri));
        } catch (IllegalArgumentException | FileSystemNotFoundException e) {
            type = DataStores.probeContentType(uri);
        } catch (NoSuchFileException e) {
            error(Errors.format(Errors.Keys.CanNotOpen_1, uri), e);
            return Command.IO_EXCEPTION_EXIT_CODE;
        }
        if (type != null) {
            out.println(type);
            out.flush();
        }
        return 0;
    }
}
