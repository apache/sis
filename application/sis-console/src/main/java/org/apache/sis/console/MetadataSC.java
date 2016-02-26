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
import org.opengis.metadata.Identifier;
import org.opengis.util.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.io.TableAppender;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.Colors;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.ValueExistencePolicy;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TreeTableFormat;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.xml.XML;


/**
 * The "metadata", "crs" and "identifier" subcommands.
 * CRS are considered as a kind of metadata here.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
final class MetadataSC extends SubCommand {
    /**
     * The desired information.
     */
    static enum Info {
        METADATA, CRS, IDENTIFIER
    }

    /**
     * The output format.
     */
    private static enum Format {
        TEXT, WKT, XML
    }

    /**
     * The sub-command: {@code "metadata"}, {@code "crs"} or {@code "identifier"}.
     */
    private final Info command;

    /**
     * The output format.
     */
    private Format outputFormat;

    /**
     * The WKT convention, or {@code null} if it does not apply.
     */
    private Convention convention;

    /**
     * Creates the {@code "metadata"}, {@code "crs"} or {@code "identifier"} sub-command.
     */
    MetadataSC(final Info command, final int commandIndex, final String... args) throws InvalidOptionException {
        super(commandIndex, args, EnumSet.of(Option.FORMAT, Option.LOCALE, Option.TIMEZONE, Option.ENCODING,
                Option.COLORS, Option.HELP, Option.DEBUG));
        this.command = command;
    }

    /**
     * Parses the command-line arguments and initializes the {@link #outputFormat} and {@link #convention} fields
     * accordingly. This method verifies the parameter validity.
     */
    private void parseArguments() throws InvalidOptionException {
        /*
         * Output format can be either "text" (the default) or "xml".
         * In the case of "crs" sub-command, we accept also WKT variants.
         */
        final String format = options.get(Option.FORMAT);
        if (format == null || format.equalsIgnoreCase("text")) {
            if (command == Info.CRS) {
                outputFormat = Format.WKT;
                convention = Convention.WKT2_SIMPLIFIED;
            } else {
                outputFormat = Format.TEXT;
            }
        } else if (format.equalsIgnoreCase("wkt") || format.equalsIgnoreCase("wkt2")) {
            outputFormat = Format.WKT;
            convention = Convention.WKT2;
        } else if (format.equalsIgnoreCase("wkt1")) {
            outputFormat = Format.WKT;
            convention = Convention.WKT1;
        } else if (format.equalsIgnoreCase("xml")) {
            outputFormat = Format.XML;
        } else {
            throw new InvalidOptionException(Errors.format(
                    Errors.Keys.IllegalOptionValue_2, "format", format), format);
        }
        final boolean isFormatCompatible;
        switch (command) {
            case CRS: {
                isFormatCompatible = true;
                break;
            }
            case IDENTIFIER: {
                isFormatCompatible = (outputFormat == Format.TEXT);
                break;
            }
            default: {
                isFormatCompatible = (convention == null);
                break;
            }
        }
        if (!isFormatCompatible) {
            throw new InvalidOptionException(Errors.format(Errors.Keys.IncompatibleFormat_2,
                    command.name().toLowerCase(locale), format), format);
        }
    }

    /**
     * Prints metadata or CRS information.
     *
     * @throws DataStoreException if an error occurred while reading the file.
     * @throws JAXBException if an error occurred while producing the XML output.
     * @throws FactoryException if an error occurred while looking for a CRS identifier.
     * @throws IOException should never happen, since we are appending to a print writer.
     */
    @Override
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public int run() throws InvalidOptionException, DataStoreException, JAXBException, FactoryException, IOException {
        parseArguments();
        /*
         * Read metadata from the data storage only aftr we verified that the arguments are valid.
         * The input can be a file given on the command line, or the standard input stream.
         */
        final Metadata metadata;
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
         * If we are executing the "identifier" sub-command, then show the metadata identifier (if any)
         * and the identifier of all referencing systems found. Otherwise if we are executing the "crs"
         * sub-command, extract only the first CRS. That CRS will be displayed after the switch statement.
         */
        Object object = metadata;
choice: switch (command) {
            case IDENTIFIER: {
                final TableAppender table = new TableAppender(out, "    ");
                appendIdentifier(table, metadata);
                for (final ReferenceSystem rs : metadata.getReferenceSystemInfo()) {
                    appendIdentifier(table, rs);
                }
                table.flush();
                return 0;
            }
            case CRS: {
                for (final ReferenceSystem rs : metadata.getReferenceSystemInfo()) {
                    if (rs instanceof CoordinateReferenceSystem) {
                        object = rs;
                        break choice;
                    }
                }
                return 0;
            }
        }
        /*
         * Format metadata to the standard output stream.
         */
        switch (outputFormat) {
            case TEXT: {
                final TreeTable tree = MetadataStandard.ISO_19115.asTreeTable(object, ValueExistencePolicy.NON_EMPTY);
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
                f.format(object, out);
                out.println();
                break;
            }

            case XML: {
                final MarshallerPool pool = new MarshallerPool(null);
                final Marshaller marshaller = pool.acquireMarshaller();
                marshaller.setProperty(XML.LOCALE,   locale);
                marshaller.setProperty(XML.TIMEZONE, timezone);
                if (isConsole()) {
                    marshaller.marshal(object, out);
                } else {
                    out.flush();
                    marshaller.setProperty(Marshaller.JAXB_ENCODING, encoding.name());
                    marshaller.marshal(object, System.out);     // Intentionally use OutputStream instead than Writer.
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

    /**
     * Append a row to the given table provided that the identifier is non-null.
     * If the given identifier is {@code null}, then this method does nothing.
     */
    private static void appendRow(final TableAppender table, final CharSequence identifier,
            final char separator, final CharSequence legend)
    {
        if (identifier != null) {
            table.append(identifier).nextColumn();
            if (legend != null) {
                table.append(separator).append(' ').append(legend);
            }
            table.nextLine();
        }
    }

    /**
     * Appends the metadata identifier to the given table.
     */
    private void appendIdentifier(final TableAppender table, final Metadata metadata) {
        final Identifier id = metadata.getMetadataIdentifier();
        if (id != null) {
            CharSequence desc = id.getDescription();
            if (desc != null && !files.isEmpty()) desc = files.get(0);
            appendRow(table, IdentifiedObjects.toString(id), '-', desc);
        }
    }

    /**
     * Append the CRS identifier to the given table.
     * This method gives precedence to {@code "urn:ogc:def:"} identifiers if possible.
     */
    private static void appendIdentifier(final TableAppender table, final ReferenceSystem rs) throws FactoryException {
        char separator;
        String legend;
        String identifier = IdentifiedObjects.lookupURN(rs, null);
        if (identifier != null) {
            separator = '-';
            legend = rs.getName().getCode();
        } else {
            for (final Identifier id : rs.getIdentifiers()) {
                final String c = IdentifiedObjects.toURN(rs.getClass(), id);
                if (c != null) {
                    identifier = c;
                    break;                                          // Stop at the first "urn:ogc:def:…".
                }
                if (identifier == null) {
                    identifier = IdentifiedObjects.toString(id);    // "AUTHORITY:CODE" as a fallback if no URN.
                }
            }
            if (identifier == null) {
                return;                                             // No identifier found.
            }
            /*
             * The CRS provided by the user contains identifier, but the 'lookupURN' operation above failed to
             * find it. The most likely cause is that the user-provided CRS does not use the same axis order.
             */
            try {
                separator = '!';
                final ReferenceSystem def = CRS.forCode(identifier);
                if (Utilities.deepEquals(def, rs, ComparisonMode.ALLOW_VARIANT)) {
                    legend = "Axis order does not match the definition.";
                } else {
                    legend = "Apparently wrong identifier!";
                }
            } catch (NoSuchAuthorityCodeException e) {
                separator = '?';
                legend = "Exactness of this identifier has not been verified.";
            }
        }
        appendRow(table, identifier, separator, legend);
    }
}
