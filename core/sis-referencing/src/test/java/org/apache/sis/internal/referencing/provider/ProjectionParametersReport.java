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
package org.apache.sis.internal.referencing.provider;

import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Collections;
import java.io.File;
import java.io.IOException;
import org.opengis.util.GenericName;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.operation.Projection;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.Transformation;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.test.report.OperationParametersReport;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.util.Version;

import static org.apache.sis.util.Characters.NO_BREAK_SPACE;
import static org.apache.sis.util.collection.Containers.hashMapCapacity;


/**
 * Generates a list of projection parameters in a HTML page. This class is used for updating the
 * <a href="http://sis.apache.org/CoordinateOperationMethods.html">CoordinateOperationMethods.html</a> page.
 * The {@linkplain #main(String[])} method creates the "{@code CoordinateOperationMethods.html}" file in the
 * current default directory if it does not already exists. Users is responsible for moving the generated
 * file to the Apache SIS site directory.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final class ProjectionParametersReport extends OperationParametersReport {
    /**
     * Generates the HTML report.
     *
     * @param  args No argument expected.
     * @throws IOException If an error occurred while writing the HTML file.
     */
    public static void main(final String[] args) throws IOException {
        if (args.length != 0) {
            System.err.println("This command does not expect any argument.");
            return;
        }
        final File file = new File("operation-parameters.html");
        if (file.exists()) {
            System.err.println("File " + file + " already exists.");
            return;
        }
        System.out.println("Write " + file.getAbsolutePath());
        /*
         * Note: ESRI needs to be right after OGC in the above list because the createRow(…) method in this class
         * contains an empirical hack for allowing the GeoAPI report to merge long ESRI projection names with the
         * OGC name when the two names are identical (ignoring case).
         */
        final ProjectionParametersReport writer = new ProjectionParametersReport(
                Citations.EPSG,   Citations.OGC,     Citations.ESRI,
                Citations.NETCDF, Citations.GEOTIFF, Citations.PROJ4);
        writer.add(DefaultFactories.forClass(MathTransformFactory.class));
        writer.write(file);
    }

    /**
     * All authority names as {@link String} instances. Those names will be used as column headers
     * in the table of coordinate operation methods. Those headers will typically be "EPSG", "OGC",
     * "ESRI", "NetCDF", "GeoTIFF" and "PROJ4".
     */
    private final Set<String> columnHeaders;

    /**
     * The type of coordinate operation methods, in the order to be shown in the HTML report.
     * We will typically show map projections first, followed by coordinate conversions,
     * followed by coordinate transformations.
     */
    private final Class<? extends SingleOperation>[] categories;

    /**
     * Creates a new instance which will use the parameter names and aliases of the given authorities.
     *
     * @param authorities The authorities for which to show parameter names and aliases.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private ProjectionParametersReport(final Citation... authorities) {
        super(null);
        /*
         * For a list of legal property names, see:
         * http://www.geoapi.org/geoapi-conformance/apidocs/org/opengis/test/report/OperationParametersReport.html
         */
        properties.setProperty("TITLE",           "Coordinate Operation parameters");
        properties.setProperty("PRODUCT.NAME",    "Apache SIS");
        properties.setProperty("PRODUCT.VERSION",  Version.SIS.toString());
        properties.setProperty("PRODUCT.URL",     "http://sis.apache.org");
        final Set<String> columns = new LinkedHashSet<>(hashMapCapacity(authorities.length));
        for (final Citation authority : authorities) {
            columns.add(org.apache.sis.internal.util.Citations.getCodeSpace(authority));
        }
        columnHeaders = Collections.unmodifiableSet(columns);
        categories = new Class[] {
            Projection.class,
            Conversion.class,
            Transformation.class
        };
    }

    /**
     * Creates a new row for the given operation and parameters. The given code spaces will be ignored;
     * we will use our own code spaces derived from the citations given at construction time instead.
     *
     * @param  operation  The operation.
     * @param  parameters The operation parameters, or {@code null} if none.
     * @param  codeSpaces The code spaces for which to get the name and aliases.
     * @return The new row, or {@code null} if none.
     */
    @Override
    protected Row createRow(final IdentifiedObject operation, final ParameterDescriptorGroup parameters, final Set<String> codeSpaces) {
        final Row row = super.createRow(operation, parameters, columnHeaders);
        /*
         * Find a user category for the given object. If a category is found, it will be formatted as a single row in
         * the HTML table before all subsequent objects of the same category. Note that in order to get good results,
         * the Row.compare(...) method needs to be defined in such a way that objects of the same category are grouped
         * together.
         */
        int categoryIndex = categories.length;
        if (operation instanceof DefaultOperationMethod) {
            final Class<? extends SingleOperation> c = ((DefaultOperationMethod) operation).getOperationType();
            if (c != null) {
                for (int i=0; i<categoryIndex; i++) {
                    final Class<?> category = categories[i];
                    if (category.isAssignableFrom(c)) {
                        if (category == Projection.class) {
                            row.category = "Map projections";
                        } else {
                            row.category = category.getSimpleName() + 's';
                        }
                        categoryIndex = i;
                        break;
                    }
                }
            }
        }
        /*
         * Empirical adjustment in the table layout:  for a few very long ESRI names, just declare
         * that the name is the same than the OGC name. This allow the GeoAPI report to generate a
         * more compact HTML table, by avoiding the column space required when repeating the same
         * information twice.
         */
        String names[] = row.names.get("ESRI");
        if (names != null && names.length == 1) {
            final String name = names[0];
            switch (name) {
                case "Lambert_Azimuthal_Equal_Area":
                case "Lambert_Conformal_Conic_2SP_Belgium": {
                    names = row.names.get(Constants.OGC);
                    assert names.length == 1 && names[0].contains(name) : name;
                    names[0] += " " + NO_BREAK_SPACE + "<font size=\"-1\" color=\"MediumSlateBlue\">(ESRI: same name)</font>";
                    row.names.remove("ESRI");
                    break;
                }
            }
        }
        /*
         * Search for deprecated names. We will render them as deleted name.
         */
        for (final Map.Entry<String,String[]> entry : row.names.entrySet()) {
            final String authority = entry.getKey();
            for (final GenericName candidate : operation.getAlias()) {
                if (candidate instanceof ImmutableIdentifier) {
                    final ImmutableIdentifier identifier = (ImmutableIdentifier) candidate;
                    if (identifier.isDeprecated() && authority.equalsIgnoreCase(identifier.getCodeSpace())) {
                        final String[] codes = entry.getValue();
                        final String deprecated = identifier.getCode();
                        for (int i=0; i<codes.length; i++) {
                            final String code = codes[i];
                            if (code.equalsIgnoreCase(deprecated)) {
                                codes[i] = "<del>" + code + "</del>";
                                break; // Continue the outer loop.
                            }
                        }
                    }
                }
            }
        }
        return new OrderedRow(row, categoryIndex);
    }

    /**
     * A row implementation sorted by category before to be sorted by name. This implementation
     * is used for sorting the operation methods in the order to be show on the HTML output page.
     * First, the operations are sorted by categories according the order of elements in the
     * {@link #categories} array. For each operation of the same category, methods are
     * sorted by alphabetical order.
     */
    private static final class OrderedRow extends Row {
        /** The category index to use for sorting rows. */
        private final int categoryIndex;

        /** Creates a new row as a copy of the given row.*/
        OrderedRow(final Row toCopy, final int categoryIndex) {
            super(toCopy);
            this.categoryIndex = categoryIndex;
        }

        /** Compares by category, then compares by name. */
        @Override public int compareTo(final Row o) {
            final int c = categoryIndex - ((OrderedRow) o).categoryIndex;
            return (c != 0) ? c : super.compareTo(o);
        }
    }
}
