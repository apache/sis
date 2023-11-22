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
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.WritableGridCoverageResource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.WritableAggregate;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Classes;


/**
 * The "translate" sub-command.
 * This command reads resources and rewrites them in another format.
 * If more than one source file is specified, then all those files are aggregated in the output file.
 * This is possible only if the output format supports the storage of an arbitrary number of resources.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class TranslateCommand extends CommandRunner {
    /**
     * Creates the {@code "translate"} sub-command.
     *
     * @param  commandIndex  index of the {@code arguments} element containing the {@code "translate"} command name, or -1 if none.
     * @param  arguments     the command-line arguments provided by the user.
     * @throws InvalidOptionException if an illegal option has been provided, or the option has an illegal value.
     */
    TranslateCommand(final int commandIndex, final String... arguments) throws InvalidOptionException {
        super(commandIndex, arguments, EnumSet.of(Option.OUTPUT, Option.FORMAT, Option.HELP, Option.DEBUG));
    }

    /**
     * Translates a file.
     *
     * @return 0 on success, or an exit code if the command failed for a reason other than an uncaught Java exception.
     * @throws Exception if an error occurred while executing the sub-command.
     */
    @Override
    public int run() throws Exception {
        if (hasUnexpectedFileCount(1, Integer.MAX_VALUE)) {
            return Command.INVALID_ARGUMENT_EXIT_CODE;
        }
        final String output = getMandatoryOption(Option.OUTPUT);
        final String format = options.get(Option.FORMAT);
        final var connector = new StorageConnector(Path.of(output));
        connector.setOption(OptionKey.OPEN_OPTIONS, new StandardOpenOption[] {
            StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE
        });
        try (DataStore target = DataStores.openWritable(connector, format)) {
            for (final String file : files) {
                try (DataStore source = DataStores.open(file)) {
                    if (target instanceof WritableAggregate) {
                        ((WritableAggregate) target).add(source);
                    } else if (target instanceof WritableGridCoverageResource) {
                        write(source, (WritableGridCoverageResource) target);
                    }
                }
            }
        }
        return 0;
    }

    /**
     * Writes the given source to the given target.
     * This method invokes itself recursively if the source is an aggregate.
     */
    private void write(final Resource source, final WritableGridCoverageResource target) throws DataStoreException {
        if (source instanceof GridCoverageResource) {
            target.write(((GridCoverageResource) source).read(null, null));
        } else if (source instanceof Aggregate) {
            for (final Resource component : ((Aggregate) source).components()) {
                write(component, target);
            }
        } else {
            Object id = source.getIdentifier().orElse(null);
            if (id == null) id = Classes.getShortClassName(source);
            err.println(Errors.format(Errors.Keys.CanNotCopy_1, id));
        }
    }
}
