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
import java.io.IOException;
import org.opengis.metadata.Metadata;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.ValueExistencePolicy;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.netcdf.NetcdfStore;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TreeTableFormat;


/**
 * The "metadata" subcommand.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class MetadataSC extends SubCommand {
    /**
     * Creates the {@code "metadata"} sub-command.
     */
    MetadataSC(final int commandIndex, final String... args) throws InvalidOptionException {
        super(commandIndex, args, EnumSet.of(Option.LOCALE, Option.TIMEZONE, Option.ENCODING, Option.HELP));
    }

    /**
     * Prints metadata information.
     *
     * @todo NetCDF data store is hard-coded for now. Will need a dynamic mechanism in the future.
     *
     * @throws DataStoreException If an error occurred while reading the NetCDF file.
     * @throws IOException Should never happen, since we are appending to a print writer.
     */
    @Override
    public int run() throws DataStoreException, IOException {
        if (hasUnexpectedFileCount(1, 1)) {
            return Command.INVALID_ARGUMENT_EXIT_CODE;
        }
        final Metadata metadata;
        final NetcdfStore store = new NetcdfStore(new StorageConnector(files.get(0)));
        try {
            metadata = store.getMetadata();
        } finally {
            store.close();
        }
        if (metadata != null) {
            final TreeTable tree = MetadataStandard.ISO_19115.asTreeTable(metadata, ValueExistencePolicy.NON_EMPTY);
            final TreeTableFormat format = new TreeTableFormat(locale, timezone);
            format.setColumns(TableColumn.NAME, TableColumn.VALUE);
            format.format(tree, out);
            out.flush();
        }
        return 0;
    }
}
