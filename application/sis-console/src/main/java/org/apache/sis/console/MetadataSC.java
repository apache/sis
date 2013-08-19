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
import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBException;
import org.opengis.metadata.Metadata;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.ValueExistencePolicy;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TreeTableFormat;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.xml.XML;


/**
 * The "metadata" subcommand.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
final class MetadataSC extends SubCommand {
    /**
     * Creates the {@code "metadata"} sub-command.
     */
    MetadataSC(final int commandIndex, final String... args) throws InvalidOptionException {
        super(commandIndex, args, EnumSet.of(Option.FORMAT, Option.LOCALE, Option.TIMEZONE, Option.ENCODING, Option.HELP));
    }

    /**
     * Prints metadata information.
     *
     * @throws DataStoreException If an error occurred while reading the file.
     * @throws JAXBException If an error occurred while producing the XML output.
     * @throws IOException Should never happen, since we are appending to a print writer.
     */
    @Override
    public int run() throws InvalidOptionException, DataStoreException, JAXBException, IOException {
        /*
         * Output format can be either "text" (the default) or "xml".
         */
        boolean toXML = false;
        final String format = options.get(Option.FORMAT);
        if (format != null && !format.equalsIgnoreCase("text")) {
            if (!format.equalsIgnoreCase("xml")) {
                throw new InvalidOptionException(Errors.format(
                        Errors.Keys.IllegalOptionValue_2, "format", format), format);
            }
            toXML = true;
        }
        /*
         * Read metadata from the data storage.
         */
        if (hasUnexpectedFileCount(1, 1)) {
            return Command.INVALID_ARGUMENT_EXIT_CODE;
        }
        final Metadata metadata;
        try (DataStore store = DataStores.open(files.get(0))) {
            metadata = store.getMetadata();
        }
        /*
         * Format metadata to the standard output stream.
         */
        if (metadata != null) {
            if (toXML) {
                final MarshallerPool pool = new MarshallerPool(null);
                final Marshaller marshaller = pool.acquireMarshaller();
                marshaller.setProperty(XML.LOCALE,   locale);
                marshaller.setProperty(XML.TIMEZONE, timezone);
                marshaller.marshal(metadata, out);
            } else {
                final TreeTable tree = MetadataStandard.ISO_19115.asTreeTable(metadata, ValueExistencePolicy.NON_EMPTY);
                final TreeTableFormat tf = new TreeTableFormat(locale, timezone);
                tf.setColumns(TableColumn.NAME, TableColumn.VALUE);
                tf.format(tree, out);
            }
            out.flush();
        }
        return 0;
    }
}
