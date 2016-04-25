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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.io.Console;
import java.io.IOException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBException;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.Colors;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.ValueExistencePolicy;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.referencing.CRS;
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
 * The "metadata" sub-command. This class is also used as the base class of other sub-commands
 * that perform most of their work on the basis of metadata information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
class MetadataCommand extends CommandRunner {
    /**
     * The protocol part of the filename to be recognized as a CRS authority.
     * In such case, this class will delegate to {@link CRS#forCode(String)}
     * instead of opening the file.
     */
    static final Set<String> AUTHORITIES = new HashSet<String>(Arrays.asList("URN", "EPSG", "CRS", "AUTO", "AUTO2"));

    /**
     * Length of the longest authority name declared in {@link #AUTHORITIES}.
     */
    static final int MAX_AUTHORITY_LENGTH = 5;

    /**
     * The output format.
     */
    static enum Format {
        TEXT, WKT, XML
    }

    /**
     * The output format. Default value can be overridden by {@link #parseArguments()}.
     */
    Format outputFormat = Format.TEXT;

    /**
     * The WKT convention, or {@code null} if it does not apply.
     */
    Convention convention;

    /**
     * Sets to {@code true} by {@link #readMetadata()} if the users provided an unexpected number of file arguments.
     * In such case, the {@link #run()} should terminate with exit code {@link Command#INVALID_ARGUMENT_EXIT_CODE}.
     */
    boolean hasUnexpectedFileCount;

    /**
     * Creates the {@code "metadata"} sub-command.
     */
    MetadataCommand(final int commandIndex, final String... args) throws InvalidOptionException {
        super(commandIndex, args, EnumSet.of(Option.FORMAT, Option.LOCALE, Option.TIMEZONE, Option.ENCODING,
                Option.COLORS, Option.HELP, Option.DEBUG));
    }

    /**
     * Creates a new sub-command with the given command-line arguments.
     * This constructor is for {@code MetadataCommand} subclasses only.
     *
     * @param  commandIndex  index of the {@code args} element containing the sub-command name.
     * @param  arguments     the command-line arguments provided by the user.
     * @param  validOptions  the command-line options allowed by this sub-command.
     * @throws InvalidOptionException if an illegal option has been provided, or the option has an illegal value.
     */
    MetadataCommand(final int commandIndex, final String[] args, final EnumSet<Option> validOptions)
            throws InvalidOptionException
    {
        super(commandIndex, args, validOptions);
    }

    /**
     * Parses the command-line arguments and initializes the {@link #outputFormat} and {@link #convention} fields
     * accordingly.
     */
    final void parseArguments() throws InvalidOptionException {
        /*
         * Output format can be either "text" (the default) or "xml".
         * In the case of "crs" sub-command, we accept also WKT variants.
         */
        final String format = options.get(Option.FORMAT);
        if (format != null && !format.equalsIgnoreCase("text")) {
            if (format.equalsIgnoreCase("wkt") || format.equalsIgnoreCase("wkt2")) {
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
        }
    }

    /**
     * Returns {@code true} if the given argument begins with one of the known authorities
     * ("URN", "EPSG", "CRS", "AUTO", <i>etc.</i>).
     */
    static boolean isAuthorityCode(final String code) {
        final char[] authority = new char[MAX_AUTHORITY_LENGTH];
        final int length = code.length();
        int p = 0, i = 0;
        while (i < length) {
            final int c = code.codePointAt(i);
            if (c == ':') {
                if (!AUTHORITIES.contains(new String(authority, 0, p))) {
                    break;
                }
                return true;
            }
            if (!Character.isWhitespace(c)) {
                if (p >= MAX_AUTHORITY_LENGTH || !Character.isLetterOrDigit(c)) {
                    break;
                }
                /*
                 * Casting to char is okay because AUTHORITIES contains only ASCII values.
                 * If 'c' was a supplemental Unicode value, then the result of the cast
                 * would not match any AUTHORITIES value anyway.
                 */
                authority[p++] = (char) Character.toUpperCase(c);
            }
            i += Character.charCount(c);
        }
        return false;
    }

    /**
     * If the given argument begins with one of the known authorities ("URN", "EPSG", "CRS", "AUTO", <i>etc.</i>),
     * delegates to {@link CRS#forCode(String)}. Otherwise reads the metadata using a datastore.
     *
     * @return A {@link Metadata} or {@link CoordinateReferenceSystem} instance, or {@code null} if none.
     */
    final Object readMetadataOrCRS() throws DataStoreException, FactoryException {
        if (useStandardInput()) {
            final DataStore store = DataStores.open(System.in);
            try {
                return store.getMetadata();
            } finally {
                store.close();
            }
        } else if (hasUnexpectedFileCount(1, 1)) {
            hasUnexpectedFileCount = true;
            return null;
        } else {
            final String file = files.get(0);
            if (isAuthorityCode(file)) {
                return CRS.forCode(file);
            } else {
                final DataStore store = DataStores.open(file);
                try {
                    return store.getMetadata();
                } finally {
                    store.close();
                }
            }
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
    public int run() throws Exception {
        parseArguments();
        if (convention != null) {
            final String format = outputFormat.name();
            throw new InvalidOptionException(Errors.format(Errors.Keys.IncompatibleFormat_2, "metadata", format), format);
        }
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

    /**
     * Format the given metadata or CRS object to the standard output stream.
     * The format is determined by {@link #outputFormat} and (in WKT case only) {@link #convention}.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    final void format(final Object object) throws IOException, JAXBException {
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
