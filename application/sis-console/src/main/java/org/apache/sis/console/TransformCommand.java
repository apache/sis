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
import org.opengis.util.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.CRS;
import org.apache.sis.internal.util.X364;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.resources.Errors;


/**
 * The "transform" subcommand.
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
     * Creates the {@code "transform"} sub-command.
     */
    TransformCommand(final int commandIndex, final String... args) throws InvalidOptionException {
        super(commandIndex, args, EnumSet.of(Option.SOURCE_CRS, Option.TARGET_CRS,
                Option.LOCALE, Option.TIMEZONE, Option.ENCODING, Option.COLORS, Option.HELP, Option.DEBUG));
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
    }

    /**
     * Transforms coordinates from the files given in argument or from the standard input stream.
     */
    @Override
    public int run() throws InvalidOptionException, DataStoreException, FactoryException {
        parseArguments();
        operation = CRS.findOperation(fetchCRS(Option.SOURCE_CRS), fetchCRS(Option.TARGET_CRS), null);
        if (!operation.getIdentifiers().isEmpty()) {
            print("Coordinate operation", operation);
        } else {
            print("Source CRS", operation.getSourceCRS());
            print("Target CRS", operation.getTargetCRS());
        }
        double accuracy = CRS.getLinearAccuracy(operation);
        if (accuracy >= 0) {
            if (accuracy == 0) {
                accuracy = Formulas.LINEAR_TOLERANCE;
            }
            printHeader("Accuracy");
            out.print(accuracy);
            out.println(" metres");
        }
        return 0;
    }

    /**
     * Prints the given string in bold characters. This is used for formatting
     * some metadata in the header before to print transformed coordinates.
     */
    private void printHeader(final String header) {
        if (colors) {
            out.print(X364.BOLD.sequence());
        }
        out.print(header);
        out.print(':');
        if (colors) {
            out.print(X364.NORMAL.sequence());
        }
        out.print(' ');
    }

    /**
     * Prints the name and authority code (if any) of the given object.
     */
    private void print(final String header, final IdentifiedObject object) {
        printHeader(header);
        out.print(object.getName().getCode());
        final String identifier = IdentifiedObjects.toString(IdentifiedObjects.getIdentifier(object, null));
        if (identifier != null) {
            out.print(' ');
            if (colors) {
                out.print(X364.FOREGROUND_YELLOW.sequence());
            }
            out.print('(');
            out.print(identifier);
            out.print(')');
            if (colors) {
                out.print(X364.FOREGROUND_DEFAULT.sequence());
            }
        }
        out.println();
    }
}
