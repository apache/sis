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
import org.opengis.metadata.Metadata;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.netcdf.NetcdfStore;


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
     */
    @Override
    public void run() throws DataStoreException {
        if (files.size() != 1) {
            err.println("Needs a single file"); // TODO: localize, and need to return an error code.
            return;
        }
        final Metadata metadata;
        try (NetcdfStore store = new NetcdfStore(new StorageConnector(files.get(0)))) {
            metadata = store.getMetadata();
        }
        if (metadata == null) {
            out.println("none"); // TODO: localize
        } else {
            out.println(metadata);
        }
    }
}
