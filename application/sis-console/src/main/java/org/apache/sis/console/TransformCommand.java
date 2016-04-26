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
import java.util.Locale;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.LineNumberReader;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import javax.measure.unit.Unit;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.converter.ConversionException;
import org.opengis.metadata.Metadata;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.util.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.ConcatenatedOperation;
import org.opengis.referencing.operation.PassThroughOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.referencing.DirectPositionView;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.util.PatchedUnitFormat;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.CRS;
import org.apache.sis.internal.util.X364;
import org.apache.sis.io.LineAppender;
import org.apache.sis.io.TableAppender;
import org.apache.sis.io.wkt.Colors;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.Transliterator;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.measure.Units;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.CharSequences;


/**
 * The "transform" subcommand.
 * The output is a comma separated values (CSV) file, with {@code '#'} as the first character of comment lines.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class TransformCommand extends MetadataCommand {
    /**
     * The coordinate operation from the given source CRS to target CRS.
     */
    private CoordinateOperation operation;

    /**
     * The coordinate operation domain of validity.
     * We use the {@code ImmutableEnvelope} type in order to handle envelope crossing the anti-meridian.
     *
     * @see #computeDomainOfValidity()
     */
    private ImmutableEnvelope domainOfValidity;

    /**
     * The transformation from source CRS to the domain of validity CRS, or {@code null} if none.
     *
     * @see #computeDomainOfValidity()
     */
    private MathTransform toDomainOfValidityCRS;

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
     * Width of ordinate values, in number of characters in ordinate {@link String} representations.
     */
    private int ordinateWidth;

    /**
     * Suggested number of fraction digits for each ordinate values.
     */
    private int[] numFractionDigits;

    /**
     * We will switch to scientific notation if the ordinate value to format is greater than this value.
     */
    private double[] thresholdForScientificNotation;

    /**
     * Creates the {@code "transform"} sub-command.
     */
    TransformCommand(final int commandIndex, final String... args) throws InvalidOptionException {
        super(commandIndex, args, EnumSet.of(Option.SOURCE_CRS, Option.TARGET_CRS, Option.VERBOSE,
                Option.LOCALE, Option.TIMEZONE, Option.ENCODING, Option.COLORS, Option.HELP, Option.DEBUG));

        // Default output format for CRS.
        outputFormat = Format.WKT;
        convention   = Convention.WKT2_SIMPLIFIED;
        resources    = Vocabulary.getResources(locale);
    }

    /**
     * Fetches the source or target coordinate reference system from the value given to the specified option.
     *
     * @param  option  either {@link Option#SOURCE_CRS} or {@link Option#TARGET_CRS}.
     * @return the coordinate reference system for the given option.
     * @throws InvalidOptionException if the given option is missing or have an invalid value.
     * @throws FactoryException if the operation failed for another reason.
     */
    private CoordinateReferenceSystem fetchCRS(final Option option) throws InvalidOptionException, FactoryException, DataStoreException {
        final String identifier = options.get(option);
        if (identifier == null) {
            final String name = option.label();
            throw new InvalidOptionException(Errors.format(Errors.Keys.MissingValueForOption_1, name), name);
        }
        if (isAuthorityCode(identifier)) try {
            return CRS.forCode(identifier);
        } catch (NoSuchAuthorityCodeException e) {
            final String name = option.label();
            throw new InvalidOptionException(Errors.format(Errors.Keys.IllegalOptionValue_2, name, identifier), e, name);
        } else {
            final Metadata metadata;
            final DataStore store = DataStores.open(identifier);
            try {
                metadata = store.getMetadata();
            } finally {
                store.close();
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
    }

    /**
     * Transforms coordinates from the files given in argument or from the standard input stream.
     */
    @Override
    public int run() throws Exception {
        parseArguments();
        if (outputFormat == Format.XML) {
            final String format = outputFormat.name();
            throw new InvalidOptionException(Errors.format(Errors.Keys.IncompatibleFormat_2, "transform", format), format);
        }
        operation = CRS.findOperation(fetchCRS(Option.SOURCE_CRS), fetchCRS(Option.TARGET_CRS), null);
        /*
         * Prints the header: source CRS, target CRS, operation steps and positional accuracy.
         */
        outHeader = new TableAppender(new LineAppender(out), " ");
        outHeader.setMultiLinesCells(true);
        printHeader(Vocabulary.Keys.Source);      printNameAndIdentifier(operation.getSourceCRS());
        printHeader(Vocabulary.Keys.Destination); printNameAndIdentifier(operation.getTargetCRS());
        printHeader(Vocabulary.Keys.Methods);     printOperationMethods (operation, false);
        outHeader.nextLine();
        if (options.containsKey(Option.VERBOSE)) {
            final WKTFormat f = new WKTFormat(locale, timezone);
            f.setConvention(options.containsKey(Option.DEBUG) ? Convention.INTERNAL : convention);
            if (colors) {
                f.setColors(Colors.DEFAULT);
            }
            final CharSequence[] lines = CharSequences.splitOnEOL(f.format(operation.getMathTransform()));
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
        }
        double accuracy = CRS.getLinearAccuracy(operation);
        if (accuracy >= 0) {
            if (accuracy == 0) {
                accuracy = Formulas.LINEAR_TOLERANCE;
            }
            printHeader(Vocabulary.Keys.Accuracy);
            if (colors) {
                outHeader.append(X364.FOREGROUND_YELLOW.sequence());    // Same as Colors.DEFAULT for ElementKind.NUMBER
            }
            outHeader.append(Double.toString(accuracy));
            if (colors) {
                outHeader.append(X364.FOREGROUND_DEFAULT.sequence());
            }
            outHeader.append(" metres");
            outHeader.nextLine();
        }
        outHeader.flush();
        outHeader = null;
        /*
         * At this point we finished to write the header. If there is at least one input file,
         * compute the transformation needed for verifying if the input points are inside the
         * domain of validity. Next we can perform the actual coordinate operations.
         */
        final boolean useStandardInput = useStandardInput();
        if (useStandardInput || !files.isEmpty()) {
            computeDomainOfValidity();
            ordinateWidth    = 15;                                      // Must be set before computeNumFractionDigits(…).
            coordinateFormat = NumberFormat.getInstance(Locale.US);
            coordinateFormat.setGroupingUsed(false);
            computeNumFractionDigits(operation.getTargetCRS().getCoordinateSystem());
            out.println();
            printAxes(operation.getTargetCRS().getCoordinateSystem());
            out.println();
            if (useStandardInput) {
                final LineNumberReader in = new LineNumberReader(new InputStreamReader(System.in, encoding));
                try {
                    transform(in, "stdin");
                } finally {
                    in.close();
                }
            } else {
                for (final String file : files) {
                    final LineNumberReader in = new LineNumberReader(new InputStreamReader(new FileInputStream(file), encoding));
                    try {
                        transform(in, file);
                    } finally {
                        in.close();
                    }
                }
            }
        }
        return 0;
    }

    /**
     * Prints the character for commented lines.
     *
     * @param after  the color to apply after the comment character, if colors are enabled.
     */
    private void printCommentLinePrefix() {
        if (colors) {
            outHeader.append(X364.FOREGROUND_GRAY.sequence());
        }
        outHeader.append("# ");
        if (colors) {
            outHeader.append(X364.FOREGROUND_DEFAULT.sequence());
        }
    }

    /**
     * Prints the given string after the prefix comment. This is used for formatting
     * some metadata in the header before to print transformed coordinates.
     *
     * @param key  a {@code Vocabulary.Keys} constant for the header to print.
     */
    private void printHeader(final short key) {
        printCommentLinePrefix();
        outHeader.append(resources.getLabel(key));
        outHeader.nextColumn();
    }

    /**
     * Prints the name and authority code (if any) of the given object.
     */
    private void printNameAndIdentifier(final IdentifiedObject object) {
        outHeader.append(object.getName().getCode());
        final String identifier = IdentifiedObjects.toString(IdentifiedObjects.getIdentifier(object, null));
        if (identifier != null) {
            outHeader.append(' ');
            if (colors) {
                outHeader.append(X364.FOREGROUND_CYAN.sequence());
            }
            outHeader.append('(');
            outHeader.append(identifier);
            outHeader.append(')');
            if (colors) {
                outHeader.append(X364.FOREGROUND_DEFAULT.sequence());
            }
        }
        outHeader.nextLine();
    }

    /**
     * Prints a summary (currently only the operation method) of the given coordinate operation as a list item.
     * The list will contain many items if the given operation is a concatenated operation.
     *
     * @param step  the coordinate operation to print as a list of steps.
     */
    private void printOperationMethods(final CoordinateOperation step, boolean isNext) {
        if (step instanceof ConcatenatedOperation) {
            for (final CoordinateOperation op : ((ConcatenatedOperation) step).getOperations()) {
                printOperationMethods(op, isNext);
                isNext = true;
            }
        } else if (step instanceof PassThroughOperation) {
            printOperationMethods(((PassThroughOperation) step).getOperation(), isNext);
        } else if (step instanceof SingleOperation) {
            if (isNext) {
                if (colors) {
                    outHeader.append(X364.FOREGROUND_GREEN.sequence());
                }
                outHeader.append(" → ");
                if (colors) {
                    outHeader.append(X364.FOREGROUND_DEFAULT.sequence());
                }
            }
            outHeader.append(((SingleOperation) step).getMethod().getName().getCode());
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
        if (colors) out.print(color.sequence());
        if (quoted) out.print('"');
        out.print(text);
        if (quoted) out.print('"');
        if (colors) out.print(X364.FOREGROUND_DEFAULT.sequence());
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
            final String unit = PatchedUnitFormat.toString(axis.getUnit());
            if (unit != null && !unit.isEmpty()) {
                name = name + " (" + unit + ')';
            }
            printQuotedText(name, ordinateWidth, X364.FOREGROUND_CYAN);
        }
    }

    /**
     * Computes the suggested precision for printing values in the given units.
     *
     * @throws ConversionException should never happen.
     */
    private void computeNumFractionDigits(final CoordinateSystem cs) throws ConversionException {
        final int dimension = cs.getDimension();
        numFractionDigits = new int[dimension];
        thresholdForScientificNotation = new double[dimension];
        for (int i=0; i<dimension; i++) {
            final Unit<?> unit = cs.getAxis(0).getUnit();
            final Unit<?> source;
            double precision;
            if (Units.isLinear(unit)) {
                precision = Formulas.LINEAR_TOLERANCE;
                source = SI.METRE;
            } else if (Units.isAngular(unit)) {
                precision = Formulas.ANGULAR_TOLERANCE;
                source = NonSI.DEGREE_ANGLE;
            } else {
                precision = 0.001;
                source = unit;
            }
            precision = source.getConverterToAny(unit).convert(precision);
            if (precision > 0) {
                numFractionDigits[i] = Math.max(DecimalFunctions.fractionDigitsForDelta(precision, false) + 1, 0);
            }
            thresholdForScientificNotation[i] = MathFunctions.pow10(ordinateWidth - 1 - numFractionDigits[i]);
        }
    }

    /**
     * Computes the domain validity. This method is a "all or nothing" operation; if the domain of validity
     * can not be computed, then {@link #toDomainOfValidityCRS} and {@link #domainOfValidity} stay {@code null}.
     */
    private void computeDomainOfValidity() {
        final GeographicBoundingBox bbox = CRS.getGeographicBoundingBox(operation);
        if (bbox != null) {
            final GeographicCRS domainOfValidityCRS = ReferencingUtilities.toNormalizedGeographicCRS(operation.getSourceCRS());
            if (domainOfValidityCRS != null) try {
                toDomainOfValidityCRS = CRS.findOperation(operation.getSourceCRS(), domainOfValidityCRS, null).getMathTransform();
                domainOfValidity = new ImmutableEnvelope(bbox);
            } catch (FactoryException e) {
                warning(e);
            }
        }
    }

    /**
     * Transforms the coordinates read from the given stream.
     * This method ignores empty and comment lines.
     *
     * @param  in        the stream from where to read coordinates.
     * @param  filename  the filename, for error reporting only.
     * @return the errors that occurred during transformation.
     */
    private void transform(final LineNumberReader in, final String filename) throws IOException {
        final int dimension    = operation.getSourceCRS().getCoordinateSystem().getDimension();
        final MathTransform mt = operation.getMathTransform();
        final double[] result  = new double[mt.getTargetDimensions()];
        final double[] domainCoordinate;
        final DirectPositionView positionInDomain;
        if (toDomainOfValidityCRS != null) {
            domainCoordinate = new double[toDomainOfValidityCRS.getTargetDimensions()];
            positionInDomain = new DirectPositionView(domainCoordinate, 0, domainCoordinate.length);
        } else {
            domainCoordinate = null;
            positionInDomain = null;
        }
        try {
            String line;
            while ((line = in.readLine()) != null) {
                final int start = CharSequences.skipLeadingWhitespaces(line, 0, line.length());
                if (start < line.length() && line.charAt(start) != '#') {
                    final double[] coordinates = CharSequences.parseDoubles(line, ',');
                    if (coordinates.length != dimension) {
                        throw new MismatchedDimensionException(Errors.format(Errors.Keys.MismatchedDimensionForCRS_3,
                                    operation.getSourceCRS().getName().getCode(), dimension, coordinates.length));
                    }
                    /*
                     * At this point we got the coordinates and they have the expected number of dimensions.
                     * Now perform the coordinate operation and prints each ordinate values. We will switch
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
                        out.print(CharSequences.spaces(ordinateWidth - s.length()));
                        out.print(s);
                    }
                    /*
                     * Append a warning after the transformed coordinate values if the source coordinate was outside
                     * the domain of validity. A failure to perform a coordinate transformation is also considered as
                     * being out of the domain of valididty.
                     */
                    if (domainCoordinate != null) {
                        boolean inside;
                        try {
                            toDomainOfValidityCRS.transform(coordinates, 0, domainCoordinate, 0, 1);
                            inside = domainOfValidity.contains(positionInDomain);
                        } catch (TransformException e) {
                            inside = false;
                            warning(e);
                        }
                        if (!inside) {
                            out.print(",    ");
                            printQuotedText(Errors.getResources(locale).getString(Errors.Keys.OutsideDomainOfValidity), 0, X364.FOREGROUND_RED);
                        }
                    }
                    out.println();
                }
            }
        } catch (Exception e) {     // This is a multi-catch exception on the JDK7 branch.
            error(Errors.format(Errors.Keys.ErrorInFileAtLine_2, filename, in.getLineNumber()), e);
        }
    }

    /**
     * Reports the given exception as an ignorable one. We consider {@link FactoryException} or
     * {@link TransformException} as ignorable exceptions only if they occurred while computing
     * whether a point is inside the domain of validity. Failure to answer that question is
     * considered as an indication that the point is outside the domain of validity.
     */
    private static void warning(final Exception e) {
        Logging.recoverableException(Logging.getLogger("org.apache.sis.console"), TransformCommand.class, "run", e);
    }
}
