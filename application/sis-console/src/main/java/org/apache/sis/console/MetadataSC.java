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
import java.io.Console;
import java.io.IOException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBException;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.Colors;
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
 * The "metadata" and "crs" subcommands.
 * CRS are considered as a kind of metadata here.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
final class MetadataSC extends SubCommand {
    /**
     * The output format.
     */
    private static enum Format {
        TEXT, WKT, XML
    }

    /**
     * {@code true} for the {@code "crs"} sub-command,
     * or {@code false} for the {@code "metadata"} sub-command.
     */
    private final boolean isCRS;

    /**
     * Creates the {@code "metadata"} or {@code "crs"} sub-command.
     *
     * @param isCRS {@code true} for the {@code "crs"} sub-command,
     *        or {@code false} for the {@code "metadata"} sub-command.
     */
    MetadataSC(final boolean isCRS, final int commandIndex, final String... args) throws InvalidOptionException {
        super(commandIndex, args, EnumSet.of(Option.FORMAT, Option.LOCALE, Option.TIMEZONE, Option.ENCODING,
                Option.COLORS, Option.HELP));
        this.isCRS = isCRS;
    }

    /**
     * Prints metadata or CRS information.
     *
     * @throws DataStoreException If an error occurred while reading the file.
     * @throws JAXBException If an error occurred while producing the XML output.
     * @throws IOException Should never happen, since we are appending to a print writer.
     */
    @Override
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public int run() throws InvalidOptionException, DataStoreException, JAXBException, IOException {
        /*
         * Output format can be either "text" (the default) or "xml".
         * In the case of "crs" sub-command, we accept also WKT variants.
         */
        Convention convention = null;
        final Format outputFormat;
        final String format = options.get(Option.FORMAT);
        if (format == null || format.equalsIgnoreCase("text")) {
            if (isCRS) {
                outputFormat = Format.WKT;
                convention = Convention.WKT2_SIMPLIFIED;
            } else {
                outputFormat = Format.TEXT;
            }
        } else if (isCRS && (format.equalsIgnoreCase("wkt") || format.equalsIgnoreCase("wkt2"))) {
            outputFormat = Format.WKT;
            convention = Convention.WKT2;
        } else if (isCRS && format.equalsIgnoreCase("wkt1")) {
            outputFormat = Format.WKT;
            convention = Convention.WKT1;
        } else if (format.equalsIgnoreCase("xml")) {
            outputFormat = Format.XML;
        } else {
            throw new InvalidOptionException(Errors.format(
                    Errors.Keys.IllegalOptionValue_2, "format", format), format);
        }
        /*
         * Read metadata from the data storage.
         */
        Object metadata;
        if (useStandardInput()) {
            try (DataStore store = DataStores.open(System.in)) {
                metadata = store.getMetadata();
            }
        } else {
            if (hasUnexpectedFileCount(1, 1)) {
                return Command.INVALID_ARGUMENT_EXIT_CODE;
            }
            try (DataStore store = DataStores.open(files.get(0))) {
                metadata = store.getMetadata();
            }
        }
        if (metadata == null) {
            return 0;
        }
        /*
         * If we are executing the "crs" sub-command, extract the first CRS.
         */
        if (isCRS && (metadata instanceof Metadata)) {
            boolean found = false;
            for (final ReferenceSystem rs : ((Metadata) metadata).getReferenceSystemInfo()) {
                if (rs instanceof CoordinateReferenceSystem) {
                    metadata = rs;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return 0;
            }
        }
        /*
         * Format metadata to the standard output stream.
         */
        switch (outputFormat) {
            case TEXT: {
                final TreeTable tree = MetadataStandard.ISO_19115.asTreeTable(metadata, ValueExistencePolicy.NON_EMPTY);
                final TreeTableFormat tf = new TreeTableFormat(locale, timezone);
                tf.setColumns(TableColumn.NAME, TableColumn.VALUE);
                tf.format(tree, out);
                break;
            }

            case WKT: {
                final WKTFormat f = new WKTFormat(locale, timezone);
                if (convention != null) {
                    f.setConvention(convention);
                }
                if (colors) {
                    f.setColors(Colors.DEFAULT);
                }
                f.format(metadata, out);
                out.println();
                break;
            }

            case XML: {
                final MarshallerPool pool = new MarshallerPool(null);
                final Marshaller marshaller = pool.acquireMarshaller();
                marshaller.setProperty(XML.LOCALE,   locale);
                marshaller.setProperty(XML.TIMEZONE, timezone);
                if (isConsole()) {
                    marshaller.marshal(metadata, out);
                } else {
                    out.flush();
                    marshaller.setProperty(Marshaller.JAXB_ENCODING, encoding.name());
                    marshaller.marshal(metadata, System.out);   // Intentionally use OutputStream instead than Writer.
                    System.out.flush();
                }
                break;
            }
        }
        out.flush();
        return 0;
    }

    /**
     * Returns {@code true} if {@link #out} is sending its output to the console.
     * If not, then we are probably writing to a file or the user specified his own encoding.
     * In such case, we will send the XML output to an {@code OutputStream} instead than to a
     * {@code Writer} and let the marshaller apply the encoding itself.
     */
    private boolean isConsole() {
        if (outputBuffer != null) return true;                      // Special case for JUnit tests only.
        final Console console = System.console();
        return (console != null) && console.writer() == out;
    }
}
