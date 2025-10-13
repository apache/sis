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

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.BufferedReader;
import java.text.NumberFormat;
import javax.measure.Unit;
import javax.measure.IncommensurableException;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
import org.opengis.referencing.operation.ConcatenatedOperation;
import org.opengis.referencing.operation.PassThroughOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.referencing.internal.shared.DirectPositionView;
import org.apache.sis.referencing.internal.shared.ReferencingUtilities;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.base.CodeType;
import org.apache.sis.system.Modules;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.internal.shared.X364;
import org.apache.sis.io.LineAppender;
import org.apache.sis.io.TableAppender;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.io.wkt.Colors;
import org.apache.sis.io.wkt.Transliterator;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.wkt.Warnings;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.ObjectDomain;

// Specific to the geoapi-4.0 branch:
import org.opengis.coordinate.MismatchedDimensionException;


/**
 * The "transform" subcommand.
 * The output is a comma separated values (CSV) file, with {@code '#'} as the first character of comment lines.
 * The source and target CRS are mandatory and can be specified as EPSG codes, WKT strings, or read from data files.
 * Those information are passed as options:
 *
 * <ul>
 *   <li>{@code --sourceCRS}: the coordinate reference system of input points.</li>
 *   <li>{@code --targetCRS}: the coordinate reference system of output points.</li>
 *   <li>{@code --operation}: the coordinate operation from source CRS to target CRS.</li>
 * </ul>
 *
 * The {@code --operation} parameter is optional.
 * If provided, then the {@code --sourceCRS} and {@code --targetCRS} parameters become optional.
 * If the operation is specified together with the source and/or target CRS, then the operation
 * is used in the middle and conversions from/to the specified CRS are concatenated before/after
 * the specified operation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class TransformCommand extends FormattedOutputCommand {
    /**
     * The logger for command-line tools.
     * Currently defined in this class because it is the only one to use directly this logger.
     */
    private static final Logger LOGGER = Logger.getLogger(Modules.CONSOLE);

    /**
     * The coordinate operation from the given source CRS to target CRS.
     * This is always the forward operation. The inverse needs to be used
     * if the {@link #inverse} flag is {@code true}.
     */
    private CoordinateOperation operation;

    /**
     * The user supplied or inferred source and target CRS. They are not necessarily the source and target CRS of
     * the transform to apply on coordinate tuples, because they need to be swapped if {@link #inverse} is true.
     *
     * @see #getEffectiveCRS(boolean)
     */
    private CoordinateReferenceSystem sourceCRS, targetCRS;

    /**
     * Whether to use the inverse of the transform given by {@link #operation}.
     * The transform will be inverted after all other options have been applied.
     */
    private final boolean inverse;

    /**
     * The transformation from source CRS to the domain of validity CRS, or {@code null} if none.
     */
    private MathTransform toDomainOfValidity;

    /**
     * Resources for {@link #printHeader(short)}.
     */
    private final Vocabulary resources;

    /**
     * Where to write the header before the data.
     */
    private TableAppender outHeader;

    /**
     * The format to use for writing coordinate values.
     */
    private NumberFormat coordinateFormat;

    /**
     * Width of coordinate values, in number of characters in coordinate {@link String} representations.
     */
    private int coordinateWidth;

    /**
     * Suggested number of fraction digits for each coordinate values.
     */
    private int[] numFractionDigits;

    /**
     * We will switch to scientific notation if the coordinate value to format is greater than this value.
     */
    private double[] thresholdForScientificNotation;

    /**
     * The error message to report after we transformed all coordinates, or {@code null}.
     */
    private String errorMessage;

    /**
     * The cause of {@link #errorMessage}, or {@code null} if none.
     */
    private NumberFormatException errorCause;

    /**
     * Returns valid options for the {@code "transform"} commands.
     */
    private static EnumSet<Option> options() {
        return EnumSet.of(Option.INVERSE, Option.SOURCE_CRS, Option.TARGET_CRS, Option.OPERATION, Option.VERBOSE,
                Option.LOCALE, Option.TIMEZONE, Option.ENCODING, Option.COLORS, Option.HELP, Option.DEBUG);
    }

    /**
     * Creates the {@code "transform"} sub-command.
     */
    TransformCommand(final int commandIndex, final Object[] args) throws InvalidOptionException {
        super(commandIndex, args, options(), OutputFormat.WKT, OutputFormat.TEXT);
        resources = Vocabulary.forLocale(locale);
        inverse = options.containsKey(Option.INVERSE);
    }

    /**
     * Creates the coordinate operation from the given authority code or file path.
     * The result is assigned to the {@link #operation} field.
     *
     * @param  identifier  the authority code or file path.
     * @throws InvalidOptionException if the coordinate operation cannot be read from the given identifier.
     * @throws FactoryException if the operation failed for another reason.
     */
    private void fetchOperation(Object identifier) throws Exception {
        if (identifier instanceof CharSequence) {
            final String c = identifier.toString();
            if (CodeType.guess(c).isAuthorityCode) try {
                var factory = (CoordinateOperationAuthorityFactory) CRS.getAuthorityFactory(null);
                operation = factory.createCoordinateOperation(c);
                return;
            } catch (NoSuchAuthorityCodeException e) {
                throw illegalOptionValue(Option.OPERATION, identifier, e);
            }
            identifier = IOUtilities.toFileOrURL(c, "UTF-8");
        }
        final Object path = identifier;     // Because lambda function require effectively final variable.
        if (path != null) {
            operation = new OperationParser(path).read()
                    .orElseThrow(() -> illegalOptionValue(Option.OPERATION, path, null));
        }
    }

    /**
     * Fetches the source or target coordinate reference system from the value given to the specified option.
     *
     * @param  option     either {@link Option#SOURCE_CRS} or {@link Option#TARGET_CRS}.
     * @param  mandatory  whether the option is mandatory.
     * @return the coordinate reference system for the given option.
     * @throws InvalidOptionException if the given option is missing or have an invalid value.
     * @throws FactoryException if the operation failed for another reason.
     */
    private CoordinateReferenceSystem fetchCRS(final Option option, final boolean mandatory) throws Exception {
        final Object identifier = mandatory ? getMandatoryOption(option) : options.get(option);
        if (identifier == null) {
            return null;
        }
        if (identifier instanceof CharSequence) {
            final String c = identifier.toString();
            if (CodeType.guess(c).isAuthorityCode) try {
                return CRS.forCode(c);
            } catch (NoSuchAuthorityCodeException e) {
                throw illegalOptionValue(option, identifier, e);
            }
        }
        final Metadata metadata;
        try (DataStore store = DataStores.open(identifier)) {
            metadata = store.getMetadata();
        }
        if (metadata != null) {
            for (final ReferenceSystem rs : metadata.getReferenceSystemInfo()) {
                if (rs instanceof CoordinateReferenceSystem) {
                    return (CoordinateReferenceSystem) rs;
                }
            }
        }
        throw new InvalidOptionException(Errors.format(Errors.Keys.UnspecifiedCRS), option.label());
    }

    /**
     * Creates the exception to throw for an illegal option value.
     */
    private static InvalidOptionException illegalOptionValue(Option option, Object identifier, Exception cause) {
        final String name = option.label();
        return new InvalidOptionException(Errors.format(Errors.Keys.IllegalOptionValue_2, name, identifier), cause, name);
    }

    /**
     * Transforms coordinates from the files given in argument or from the standard input stream.
     *
     * @return 0 on success, or an exit code if the command failed for a reason other than an uncaught Java exception.
     */
    @Override
    public int run() throws Exception {
        fetchOperation(options.get(Option.OPERATION));
        sourceCRS = fetchCRS(Option.SOURCE_CRS, operation == null);
        targetCRS = fetchCRS(Option.TARGET_CRS, operation == null);
        if (sourceCRS == null) sourceCRS = operation.getSourceCRS();    // May still be null.
        if (targetCRS == null) targetCRS = operation.getTargetCRS();
        /*
         * Read all coordinates, so we can compute the area of interest.
         * This will be used when searching for a coordinate operation.
         */
        GeographicBoundingBox areaOfInterest = null;
        List<double[]> points = List.of();
        final boolean useStandardInput = useStandardInput();
        if (useStandardInput || !files.isEmpty()) {
            if (useStandardInput) {
                try (LineNumberReader in = new LineNumberReader(new InputStreamReader(System.in, encoding))) {
                    points = readCoordinates(in, "stdin");
                }
            } else {
                for (final Object file : files) {
                    final StorageConnector c = inputConnector(file);
                    try (BufferedReader in = IOUtilities.toBuffered(c.commit(Reader.class, "transform"))) {
                        points = readCoordinates(in, file);
                    }
                }
            }
            try {
                final CoordinateReferenceSystem crs = getEffectiveCRS(false);
                final GeographicCRS domainOfValidityCRS = ReferencingUtilities.toNormalizedGeographicCRS(crs, false, false);
                if (domainOfValidityCRS != null) {
                    toDomainOfValidity = CRS.findOperation(crs, domainOfValidityCRS, null).getMathTransform();
                    areaOfInterest = computeAreaOfInterest(points);
                }
            } catch (FactoryException e) {
                warning(e);
            }
        }
        /*
         * Now find or complete the coordinate operation. Note that the source and target CRS may be null
         * if and only if a coordinate operation was explicitly specified. In that case, CRS are optional.
         * If both a coordinate operation and CRS are present, this code ensures that the operation inputs
         * and outputs match the specified CRS.
         */
        if (operation == null) {
            operation = CRS.findOperation(sourceCRS, targetCRS, areaOfInterest);
        } else {
            final var steps = new ArrayList<CoordinateOperation>(3);
            if (sourceCRS != null) {
                CoordinateReferenceSystem step = operation.getSourceCRS();
                if (step != null && step != sourceCRS) {
                    steps.add(CRS.findOperation(sourceCRS, step, areaOfInterest));
                }
            }
            steps.add(operation);
            if (targetCRS != null) {
                CoordinateReferenceSystem step = operation.getTargetCRS();
                if (step != null && step != targetCRS) {
                    steps.add(CRS.findOperation(step, targetCRS, areaOfInterest));
                }
            }
            if (steps.size() > 1) {
                var factory = DefaultCoordinateOperationFactory.provider();
                var properties = IdentifiedObjects.getProperties(operation, CoordinateOperation.IDENTIFIERS_KEY);
                operation = factory.createConcatenatedOperation(
                        properties,
                        null, null,     // Infer source and target CRS from the first and last steps.
                        steps.toArray(CoordinateOperation[]::new));
            }
        }
        /*
         * At this point, the coordinate operation has been fully constructed. Replace the source and target CRS
         * by the ones specified in the operation because they may have more accurate metadata (name, identifier)
         * if they were fetched from the EPSG database.
         */
        CoordinateReferenceSystem crs;
        if ((crs = operation.getSourceCRS()) != null) sourceCRS = crs;
        if ((crs = operation.getTargetCRS()) != null) targetCRS = crs;
        /*
         * Prints the header: source CRS, target CRS, operation steps and positional accuracy.
         */
        outHeader = new TableAppender(new LineAppender(out), " ");
        outHeader.setMultiLinesCells(true);
        printHeader(Vocabulary.Keys.Source);      printNameAndIdentifier(getEffectiveCRS(false), false);
        printHeader(Vocabulary.Keys.Destination); printNameAndIdentifier(getEffectiveCRS(true),  false);
        printHeader(Vocabulary.Keys.Operations);  printOperations(operation, false);
        outHeader.nextLine();
        printDomainOfValidity(operation.getDomains());
        printAccuracy(CRS.getLinearAccuracy(operation));
        if (options.containsKey(Option.VERBOSE)) {
            printDetails();
        }
        outHeader.flush();
        outHeader = null;
        /*
         * At this point we finished to write the header. If there is at least one input file,
         * compute the number of digits to format and perform the actual coordinate operations.
         */
        if (!points.isEmpty()) {
            coordinateWidth  = 15;          // Must be set before computeNumFractionDigits(…).
            coordinateFormat = NumberFormat.getInstance(Locale.US);
            coordinateFormat.setGroupingUsed(false);
            final CoordinateSystem cs = getEffectiveCRS(true).getCoordinateSystem();
            computeNumFractionDigits(cs);
            out.println();
            printAxes(cs);
            out.println();
            transform(points);
            if (errorMessage != null) {
                error(errorMessage, errorCause);
            }
        }
        return 0;
    }

    /**
     * Appends the ANSI X3.64 sequence if colors are enabled, otherwise does nothing.
     */
    private void outHeader(final X364 code) {
        if (colors) {
            outHeader.append(code.sequence());
        }
    }

    /**
     * Prints the character for commented lines.
     */
    private void printCommentLinePrefix() {
        outHeader(X364.FOREGROUND_GRAY);
        outHeader.append("# ");
        outHeader(X364.FOREGROUND_DEFAULT);
    }

    /**
     * Prints the given string after the prefix comment. This is used for formatting
     * some metadata in the header before to print transformed coordinates.
     *
     * @param  key  a {@code Vocabulary.Keys} constant for the header to print.
     */
    private void printHeader(final short key) throws IOException {
        printCommentLinePrefix();
        resources.appendLabel(key, outHeader);
        outHeader.nextColumn();
    }

    /**
     * Prints the name and authority code (if any) of the given object.
     *
     * @param  object      the object for which to print name and identifier.
     * @param  idRequired  {@code true} for printing the name only if an identifier is present.
     * @return whether this method has printed something.
     */
    private boolean printNameAndIdentifier(final IdentifiedObject object, final boolean idRequired) {
        final String identifier = IdentifiedObjects.toString(IdentifiedObjects.getIdentifier(object, null));
        if (idRequired && identifier == null) {
            return false;
        }
        outHeader.append(object.getName().getCode());
        if (identifier != null) {
            outHeader.append(' ');
            outHeader(X364.FOREGROUND_CYAN);
            outHeader.append('(');
            outHeader.append(identifier);
            outHeader.append(')');
            outHeader(X364.FOREGROUND_DEFAULT);
        }
        if (!idRequired) {
            outHeader.nextLine();
        }
        return true;
    }

    /**
     * Prints a summary of the given coordinate operation as a sequence of steps.
     * If the operations is specified by EPSG, prints the EPSG name and code.
     * Otherwise prints only the operation method names, since the coordinate operation names
     * generated by SIS are not very meaningful.
     *
     * @param  step  the coordinate operation to print as a sequence of steps.
     */
    private void printOperations(final CoordinateOperation step, boolean isNext) {
        if (isNext) {
            isNext = false;
            outHeader(X364.FOREGROUND_GREEN);
            outHeader.append(" → ");
            outHeader(X364.FOREGROUND_DEFAULT);
        }
        if (!printNameAndIdentifier(step, true)) {
            if (step instanceof ConcatenatedOperation) {
                for (final CoordinateOperation op : ((ConcatenatedOperation) step).getOperations()) {
                    printOperations(op, isNext);
                    isNext = true;
                }
            } else if (step instanceof PassThroughOperation) {
                printOperations(((PassThroughOperation) step).getOperation(), false);
            } else if (step instanceof SingleOperation) {
                outHeader.append(((SingleOperation) step).getMethod().getName().getCode());
            }
        }
    }

    /**
     * Prints the accuracy.
     */
    private void printAccuracy(double accuracy) throws IOException {
        if (accuracy >= 0) {
            if (accuracy == 0) {
                accuracy = Formulas.LINEAR_TOLERANCE;
            }
            printHeader(Vocabulary.Keys.Accuracy);
            outHeader(X364.FOREGROUND_YELLOW);              // Same as Colors.DEFAULT for ElementKind.NUMBER
            outHeader.append(Double.toString(accuracy));
            outHeader(X364.FOREGROUND_DEFAULT);
            outHeader.append(" metres");
            outHeader.nextLine();
        }
    }

    /**
     * Prints a textual description of the domain of validity. This method tries to reduce the string length by
     * the use of some heuristic rules based on the syntax used in EPSG dataset. For example the following string:
     *
     * <blockquote>Canada - onshore and offshore - Alberta; British Columbia (BC); Manitoba; New Brunswick (NB);
     * Newfoundland and Labrador; Northwest Territories (NWT); Nova Scotia (NS); Nunavut; Ontario; Prince Edward
     * Island (PEI); Quebec; Saskatchewan; Yukon.</blockquote>
     *
     * is replaced by:
     *
     * <blockquote>Canada - onshore and offshore</blockquote>
     */
    private void printDomainOfValidity(final Collection<ObjectDomain> domains) throws IOException {
        for (final ObjectDomain domain : domains) {
            final Extent extent = domain.getDomainOfValidity();
            final InternationalString description = extent.getDescription();
            if (description != null) {
                String text = description.toString(locale);
                if (text.length() >= 80) {
                    int end = text.indexOf(';');
                    if (end >= 0) {
                        int s = text.lastIndexOf('-', end);
                        if (s >= 0) {
                            end = s;
                        }
                        text = text.substring(0, end).trim();
                    }
                }
                printHeader(Vocabulary.Keys.Domain);
                outHeader.append(text);
                outHeader.nextLine();
                return;
            }
        }
    }

    /**
     * Prints the coordinate operation or math transform in Well Known Text format.
     * This information is printed only if the {@code --verbose} option was specified.
     */
    private void printDetails() throws IOException, NoninvertibleTransformException {
        final WKTFormat f = new WKTFormat(locale, timezone);
        if (colors) f.setColors(Colors.DEFAULT);
        f.setConvention(convention);
        CharSequence[] lines = CharSequences.splitOnEOL(f.format(debug ? getMathTransform() : operation));
        for (int i=0; i<lines.length; i++) {
            if (i == 0) {
                printHeader(Vocabulary.Keys.Details);
            } else {
                printCommentLinePrefix();
                outHeader.nextColumn();
            }
            outHeader.append(lines[i]);
            outHeader.nextLine();
        }
        final Warnings warnings = f.getWarnings();
        if (warnings != null) {
            lines = CharSequences.splitOnEOL(warnings.toString());
            if (lines.length != 0) {                                            // Paranoiac check.
                printHeader(Vocabulary.Keys.Note);
                outHeader.append(lines[0]);
                outHeader.nextLine();
            }
        }
    }

    /**
     * Prints a quoted text in the given color.
     * If the given text contains the quote character, it will be escaped.
     */
    private void printQuotedText(String text, int fieldWidth, final X364 color) {
        final boolean quoted;
        if (text.indexOf('"') >= 0) {
            text = text.replace("\"", "\"\"");
            quoted = true;
        } else {
            quoted = (text.indexOf(',') >= 0);
        }
        if (quoted) fieldWidth -= 2;
        out.print(CharSequences.spaces(fieldWidth - text.length()));
        color(color);
        if (quoted) out.print('"');
        out.print(text);
        if (quoted) out.print('"');
        color(X364.FOREGROUND_DEFAULT);
    }

    /*
     * Prints the names of all coordinate system axes on the first row.
     * This method does not add EOL character.
     * This method opportunistically computes the suggested precision for formatting values.
     *
     * @throws ConversionException should never happen.
     */
    private void printAxes(final CoordinateSystem cs) {
        final int targetDim = cs.getDimension();
        for (int i=0; i<targetDim; i++) {
            if (i != 0) {
                out.print(',');
            }
            final CoordinateSystemAxis axis = cs.getAxis(i);
            String name =  axis.getName().getCode();
            name = Transliterator.DEFAULT.toShortAxisName(cs, axis.getDirection(), name);
            final String unit = axis.getUnit().toString();
            if (!unit.isEmpty()) {
                name = name + " (" + unit + ')';
            }
            printQuotedText(name, coordinateWidth, X364.FOREGROUND_CYAN);
        }
    }

    /**
     * Computes the suggested precision for printing values in the given units.
     *
     * @throws IncommensurableException should never happen.
     */
    private void computeNumFractionDigits(final CoordinateSystem cs) throws IncommensurableException {
        final int dimension = cs.getDimension();
        numFractionDigits = new int[dimension];
        thresholdForScientificNotation = new double[dimension];
        for (int i=0; i<dimension; i++) {
            final Unit<?> unit = cs.getAxis(0).getUnit();
            final Unit<?> source;
            double precision;
            if (Units.isLinear(unit)) {
                precision = Formulas.LINEAR_TOLERANCE;
                source = Units.METRE;
            } else if (Units.isAngular(unit)) {
                precision = Formulas.ANGULAR_TOLERANCE;
                source = Units.DEGREE;
            } else {
                precision = 0.001;
                source = unit;
            }
            precision = source.getConverterToAny(unit).convert(precision);
            if (precision > 0) {
                numFractionDigits[i] = Math.max(DecimalFunctions.fractionDigitsForDelta(precision, false) + 1, 0);
            }
            thresholdForScientificNotation[i] = MathFunctions.pow10(coordinateWidth - 1 - numFractionDigits[i]);
        }
    }

    /**
     * Reads all coordinates.
     * This method ignores empty and comment lines.
     *
     * @param  in        the stream from where to read coordinates.
     * @param  filename  the filename, for error reporting only.
     * @return the coordinate values.
     */
    private List<double[]> readCoordinates(final BufferedReader in, Object filename) throws IOException {
        final List<double[]> points = new ArrayList<>();
        try {
            String line;
            while ((line = in.readLine()) != null) {
                final int start = CharSequences.skipLeadingWhitespaces(line, 0, line.length());
                if (start < line.length() && line.charAt(start) != '#') {
                    points.add(CharSequences.parseDoubles(line, ','));
                }
            }
        } catch (NumberFormatException e) {
            if ((filename = IOUtilities.toString(filename)) == null) filename = "?";
            Object line = (in instanceof LineNumberReader) ? ((LineNumberReader) in).getLineNumber() : "?";
            errorMessage = Errors.format(Errors.Keys.ErrorInFileAtLine_2, filename, line);
            errorCause = e;
        }
        return points;
    }

    /**
     * Computes the geographic area of interest from the given points.
     * This method ignores the points having an unexpected number of dimensions since it is not this
     * method's job to report those issues (they will be reported by {@link #transform(List)} instead.
     */
    private GeographicBoundingBox computeAreaOfInterest(final List<double[]> points) {
        final int dimension = toDomainOfValidity.getSourceDimensions();
        final double[] domainCoordinate = new double[toDomainOfValidity.getTargetDimensions()];
        if (domainCoordinate.length >= 2) {
            double xmin = Double.POSITIVE_INFINITY;
            double ymin = Double.POSITIVE_INFINITY;
            double xmax = Double.NEGATIVE_INFINITY;
            double ymax = Double.NEGATIVE_INFINITY;
            for (final double[] coordinates : points) {
                if (coordinates.length == dimension) {
                    try {
                        toDomainOfValidity.transform(coordinates, 0, domainCoordinate, 0, 1);
                    } catch (TransformException e) {
                        warning(e);
                        continue;
                    }
                    final double x = domainCoordinate[0];
                    final double y = domainCoordinate[1];
                    if (x < xmin) xmin = x;
                    if (x > xmax) xmax = x;
                    if (y < ymin) ymin = y;
                    if (y > ymax) ymax = y;
                }
            }
            if (xmin < xmax && ymin < ymax) {
                return new DefaultGeographicBoundingBox(xmin, xmax, ymin, ymax);
            }
        }
        return null;
    }

    /**
     * Returns the source or target CRS, taking in account whether the inverse transform is requested.
     *
     * @param  target  {@code false} for the source CRS, or {@code true} for the target CRS.
     * @return the source or target CRS.
     */
    private CoordinateReferenceSystem getEffectiveCRS(final boolean target) {
        return (target ^ inverse) ? targetCRS : sourceCRS;
    }

    /**
     * Returns the math transform of the operation, inverted if requested by user.
     */
    private MathTransform getMathTransform() throws NoninvertibleTransformException {
        MathTransform mt = operation.getMathTransform();
        if (inverse) mt = mt.inverse();
        return mt;
    }

    /**
     * Transforms the given coordinates.
     */
    private void transform(final List<double[]> points) throws TransformException {
        final MathTransform mt = getMathTransform();
        final int dimension    = mt.getSourceDimensions();
        final double[] result  = new double[mt.getTargetDimensions()];
        final double[] domainCoordinate;
        final DirectPositionView positionInDomain;
        final ImmutableEnvelope domainOfValidity;
        final GeographicBoundingBox bbox;
        if (toDomainOfValidity != null && (bbox = CRS.getGeographicBoundingBox(operation)) != null) {
            domainOfValidity = new ImmutableEnvelope(bbox);
            domainCoordinate = new double[toDomainOfValidity.getTargetDimensions()];
            positionInDomain = new DirectPositionView.Double(domainCoordinate);
        } else {
            domainOfValidity = null;
            domainCoordinate = null;
            positionInDomain = null;
        }
        for (final double[] coordinates : points) {
            if (coordinates.length != dimension) {
                throw new MismatchedDimensionException(Errors.format(Errors.Keys.MismatchedDimensionForCRS_3,
                            getEffectiveCRS(false).getName().getCode(), dimension, coordinates.length));
            }
            /*
             * At this point we got the coordinates and they have the expected number of dimensions.
             * Now perform the coordinate operation and print each coordinate values. We will switch
             * to scientific notation if the coordinate is much larger than expected.
             */
            mt.transform(coordinates, 0, result, 0, 1);
            for (int i=0; i<result.length; i++) {
                if (i != 0) {
                    out.print(',');
                }
                final double value = result[i];
                final String s;
                if (Math.abs(value) >= thresholdForScientificNotation[i]) {
                    s = Double.toString(value);
                } else {
                    coordinateFormat.setMinimumFractionDigits(numFractionDigits[i]);
                    coordinateFormat.setMaximumFractionDigits(numFractionDigits[i]);
                    s = coordinateFormat.format(value);
                }
                out.print(CharSequences.spaces(coordinateWidth - s.length()));
                out.print(s);
            }
            /*
             * Append a warning after the transformed coordinate values if the source coordinate was outside
             * the domain of validity. A failure to perform a coordinate transformation is also considered as
             * being out of the domain of valididty.
             */
            if (domainOfValidity != null) {
                boolean inside;
                try {
                    toDomainOfValidity.transform(coordinates, 0, domainCoordinate, 0, 1);
                    inside = domainOfValidity.contains(positionInDomain);
                } catch (TransformException e) {
                    inside = false;
                    warning(e);
                }
                if (!inside) {
                    out.print(",    ");
                    printQuotedText(Errors.forLocale(locale).getString(Errors.Keys.OutsideDomainOfValidity), 0, X364.FOREGROUND_RED);
                }
            }
            out.println();
        }
    }

    /**
     * Reports the given exception as an ignorable one. We consider {@link FactoryException} or
     * {@link TransformException} as ignorable exceptions only if they occurred while computing
     * whether a point is inside the domain of validity. Failure to answer that question is
     * considered as an indication that the point is outside the domain of validity.
     */
    private static void warning(final Exception e) {
        Logging.recoverableException(LOGGER, TransformCommand.class, "run", e);
    }
}
