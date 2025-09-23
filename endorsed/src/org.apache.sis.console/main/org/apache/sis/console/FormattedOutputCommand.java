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

import java.util.Locale;
import java.util.EnumSet;
import java.util.function.Predicate;
import java.io.Console;
import java.io.IOException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.JAXBException;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.Colors;
import org.apache.sis.io.wkt.Warnings;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.ValueExistencePolicy;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.base.CodeType;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Version;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TreeTableFormat;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.X364;
import org.apache.sis.measure.Range;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.xml.XML;


/**
 * Base class of commands that provided formatted output.
 * The output format is controlled by {@link OutputFormat} enumeration.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class FormattedOutputCommand extends CommandRunner {
    /**
     * The output format.
     */
    private final OutputFormat outputFormat;

    /**
     * The WKT convention, or {@code null} if it does not apply.
     * This is slightly redundant to {@link #version}, but specific to the WKT format.
     */
    Convention convention;

    /**
     * Desired version of output format, or {@code null} if unspecified.
     * The format can be specified after the format name, for example {@code "gpx-1.1"}.
     */
    private Version version;

    /**
     * The provider of {@link DataStore} instances capable to write data in the {@link #outputFormat},
     * or {@code null} if none.
     */
    private final DataStoreProvider provider;

    /**
     * Sets to {@code true} by {@link #readMetadataOrCRS()} if the users provided an unexpected number of file arguments.
     * In such case, the {@link #run()} should terminate with exit code {@link Command#INVALID_ARGUMENT_EXIT_CODE}.
     */
    boolean hasUnexpectedFileCount;

    /**
     * Creates a new sub-command with the given command-line arguments.
     *
     * @param  commandIndex      index of the {@code arguments} element containing the sub-command name, or -1 if none.
     * @param  arguments         the command-line arguments provided by the user.
     * @param  validOptions      the command-line options allowed by this sub-command.
     * @param  supportedFormats  the output formats to accept. The first format is the default one.
     * @throws InvalidOptionException if an illegal option has been provided, or the option has an illegal value.
     */
    FormattedOutputCommand(final int commandIndex, final Object[] arguments, final EnumSet<Option> validOptions,
            final OutputFormat... supportedFormats) throws InvalidOptionException
    {
        super(commandIndex, arguments, validOptions);
        boolean isVersionSupported = true;
        /*
         * Output format can be either "text" (the default) or "xml".
         * In the case of "crs" sub-command, we accept also WKT variants.
         */
        final String format = getOptionAsString(Option.FORMAT);
        if (format == null) {
            outputFormat = supportedFormats[0];
            convention   = Convention.WKT2_SIMPLIFIED;
        } else if (format.equalsIgnoreCase("WKT1")) {
            outputFormat = OutputFormat.WKT;
            convention   = Convention.WKT1;
        } else if (format.equalsIgnoreCase("WKT2")) {
            outputFormat = OutputFormat.WKT;
            convention   = Convention.WKT2;
        } else if (format.equalsIgnoreCase("WKT2:2019")) {  // Same name as PROJ.
            outputFormat = OutputFormat.WKT;
            convention   = Convention.WKT2_2019;
        } else if (format.equalsIgnoreCase("WKT2:2015")) {  // Same name as PROJ.
            outputFormat = OutputFormat.WKT;
            convention   = Convention.WKT2_2015;
        } else {
            /*
             * Separate the format name from its version. We verify right after this block that the format
             * enumerated value that we get is one of the formats supported by this command. Note that the
             * error messages in case of unknown and unsupported format are slightly different.
             */
            final int s = format.indexOf('-');
            String fmtEnum = format;
            if (s > 0) {
                fmtEnum = format.substring(0, s);
                version = new Version(format.substring(s+1).trim());
            }
            fmtEnum = fmtEnum.toUpperCase(Locale.US);
            try {
                outputFormat = OutputFormat.valueOf(fmtEnum);
            } catch (IllegalArgumentException e) {
                throw new InvalidOptionException(Errors.format(
                        Errors.Keys.IllegalOptionValue_2, "format", format), e, "format");
            }
        }
        if (!ArraysExt.contains(supportedFormats, outputFormat)) {
            throw new InvalidOptionException(Errors.format(
                    Errors.Keys.IncompatibleFormat_2, commandName, outputFormat), "format");
        }
        /*
         * At this point the output format is considered valid. Now verify its version number.
         * For some special cases (e.g. WKT format), this block sets properties that depend on
         * the version number (e.g. WKT conventions).
         */
        provider = outputFormat.provider();
        switch (outputFormat) {
            case WKT: {
                if (convention == null) {
                    if (version == null || version.equals(Version.valueOf(2))) {
                        convention = Convention.WKT2;
                    } else {
                        convention = Convention.WKT1;
                        isVersionSupported = version.equals(Version.valueOf(1));
                    }
                }
                break;
            }
            default: {
                if (version != null) {
                    if (isVersionSupported = (provider != null)) {
                        final Range<Version> supportedVersions = provider.getSupportedVersions();
                        isVersionSupported = (supportedVersions != null) && supportedVersions.contains(version);
                    }
                }
                break;
            }
        }
        if (!isVersionSupported) {
            throw new InvalidOptionException(Errors.format(
                    Errors.Keys.UnsupportedFormatVersion_2, outputFormat.name(), version), "format");
        }
    }

    /**
     * If the given argument seems to be an authority code ("URN", "EPSG", "CRS", "AUTO", <i>etc.</i>),
     * delegates to {@link CRS#forCode(String)}. Otherwise reads the metadata using a datastore.
     * The input format is detected automatically (this is <strong>not</strong> {@link #outputFormat}).
     *
     * @return a {@link Metadata} or {@link CoordinateReferenceSystem} instance, or {@code null} if none.
     * @throws InvalidOptionException if an option has an invalid value.
     * @throws DataStoreException if an error occurred while reading the file.
     * @throws FactoryException if an error occurred while looking for a CRS identifier.
     */
    final Object readMetadataOrCRS() throws InvalidOptionException, DataStoreException, FactoryException {
        final Object input;
        if (useStandardInput()) {
            input = System.in;
        } else if (hasUnexpectedFileCount(1, 1)) {
            hasUnexpectedFileCount = true;
            return null;
        } else {
            final Object file = files.get(0);
            if (file instanceof CharSequence) {
                final String c = file.toString();
                if (CodeType.guess(c).isAuthorityCode) {
                    return CRS.forCode(c);
                }
            }
            input = file;
        }
        try (DataStore store = DataStores.open(inputConnector(input))) {
            return store.getMetadata();
        }
    }

    /**
     * Formats the given metadata or CRS object to the standard output stream.
     * The format is determined by {@link #outputFormat} and (in WKT case only) {@link #convention}.
     *
     * @throws DataStoreException if an error occurred while producing output using a data store.
     * @throws JAXBException if an error occurred while producing the XML output using JAXB.
     * @throws IOException should never happen since we are appending to a print writer.
     */
    @SuppressWarnings({"UseOfSystemOutOrSystemErr", "deprecation"})
    final void format(final Object object) throws DataStoreException, JAXBException, IOException {
        switch (outputFormat) {
            case TEXT: {
                final TreeTable tree = MetadataStandard.ISO_19115.asTreeTable(object,
                        (object instanceof Metadata) ? Metadata.class : null,
                        ValueExistencePolicy.COMPACT);
                final var tf = new TreeTableFormat(locale, getTimeZone());
                tf.setColumns(TableColumn.NAME, TableColumn.VALUE);
                tf.setNodeFilter(getNodeFilter());
                tf.format(tree, out);
                break;
            }

            case WKT: {
                final var f = new WKTFormat(locale, timezone);
                if (convention != null) {
                    f.setConvention(convention);
                }
                if (colors) {
                    f.setColors(Colors.DEFAULT);
                }
                f.format(object, out);
                out.println();
                final Warnings warnings = f.getWarnings();
                if (warnings != null) {
                    out.flush();
                    err.println();
                    color(colors, err, X364.FOREGROUND_YELLOW);
                    err.println(warnings.toString(locale));
                    color(colors, err, X364.RESET);
                }
                break;
            }

            case XML: {
                final var pool = new MarshallerPool(null);
                final Marshaller marshaller = pool.acquireMarshaller();
                marshaller.setProperty(XML.LOCALE,   locale);
                marshaller.setProperty(XML.TIMEZONE, timezone);
                if (isConsole()) {
                    marshaller.marshal(object, out);
                } else {
                    out.flush();
                    marshaller.setProperty(Marshaller.JAXB_ENCODING, encoding.name());
                    marshaller.marshal(object, System.out);     // Intentionally use OutputStream instead of Writer.
                    System.out.flush();
                }
                break;
            }

            default: {
                final var connector = new StorageConnector(out);
                connector.setOption(OptionKey.TIMEZONE, timezone);
                connector.setOption(OptionKey.LOCALE,   locale);
                connector.setOption(OptionKey.ENCODING, encoding);
                try (DataStore store = provider.open(connector)) {
                    /*
                     * HACK: API used in this block is currently available only for GPX format,
                     * but we will generalize to more formats in a future Apache SIS version.
                     *
                     * Note: after such generalization is done, revert the xml-store dependency
                     *       scope in pom.xml from "compile" to "runtime".
                     */
                    final var fs = (org.apache.sis.storage.gpx.WritableStore) store;
                    if (version != null) {
                        fs.setVersion(version);
                    }
                    fs.write((object instanceof Metadata) ? (Metadata) object : null, null);
                }
                break;
            }
        }
        out.flush();
    }

    /**
     * Returns the filter for simplifying the tree table to be formatted, or {@code null} if none.
     * This is used only for the tree in text format (not for XML output).
     */
    Predicate<TreeTable.Node> getNodeFilter() {
        return null;
    }

    /**
     * Returns {@code true} if {@link #out} is sending its output to the console.
     * If not, then we are probably either writing to a file, or the user specified his own encoding.
     * In such case, we will send the XML output to an {@code OutputStream} instead of {@code Writer},
     * and let the marshaller applies the encoding itself.
     */
    private boolean isConsole() {
        if (outputBuffer != null) return true;                      // Special case for JUnit tests only.
        final Console console = System.console();
        return (console != null) && console.writer() == out;
    }
}
