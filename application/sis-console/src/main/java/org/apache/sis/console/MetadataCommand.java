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

import java.util.Collections;
import java.util.EnumSet;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.ReferenceSystem;
import org.apache.sis.metadata.iso.DefaultMetadata;


/**
 * The "metadata" sub-command.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.8
 * @module
 */
final class MetadataCommand extends FormattedOutputCommand {
    /**
     * Returns valid options for the {@code "metadata"} command.
     */
    static EnumSet<Option> options() {
        return EnumSet.of(Option.FORMAT, Option.LOCALE, Option.TIMEZONE,
                Option.ENCODING, Option.COLORS, Option.HELP, Option.DEBUG);
    }

    /**
     * Creates the {@code "metadata"} sub-command.
     *
     * @param  commandIndex  index of the {@code arguments} element containing the {@code "metadata"} command name, or -1 if none.
     * @param  arguments     the command-line arguments provided by the user.
     * @throws InvalidOptionException if an illegal option has been provided, or the option has an illegal value.
     */
    MetadataCommand(final int commandIndex, final String... arguments) throws InvalidOptionException {
        super(commandIndex, arguments, options(), OutputFormat.TEXT, OutputFormat.XML, OutputFormat.GPX);
    }

    /**
     * Prints metadata or CRS information.
     *
     * @return 0 on success, or an exit code if the command failed for a reason other than an uncaught Java exception.
     */
    @Override
    public int run() throws Exception {
        /*
         * Read metadata from the data storage only after we verified that the arguments are valid.
         * The input can be a file given on the command line, or the standard input stream.
         */
        Object metadata = readMetadataOrCRS();
        if (hasUnexpectedFileCount) {
            return Command.INVALID_ARGUMENT_EXIT_CODE;
        }
        if (metadata != null) {
            if (!(metadata instanceof Metadata)) {
                final DefaultMetadata md = new DefaultMetadata();
                md.setReferenceSystemInfo(Collections.singleton((ReferenceSystem) metadata));
                metadata = md;
            }
            format(metadata);
        }
        return 0;
    }
}
