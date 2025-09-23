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
package org.apache.sis.referencing.report;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;
import javax.measure.Unit;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.parameter.*;
import org.opengis.referencing.operation.*;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Characters;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.Version;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.internal.shared.URLs;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.provider.Affine;
import org.apache.sis.referencing.operation.provider.AlbersEqualArea;
import org.apache.sis.referencing.operation.provider.LambertConformal2SP;
import org.apache.sis.referencing.operation.provider.ObliqueMercator;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.RangeFormat;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.Identifier;
import org.opengis.referencing.crs.DerivedCRS;


/**
 * Generates a list of projection parameters in a HTML page. This class is used for updating the
 * <a href="https://sis.apache.org/tables/CoordinateOperationMethods.html">CoordinateOperationMethods.html</a> page.
 * The {@linkplain #main(String[])} method creates the "{@code CoordinateOperationMethods.html}" file in the current
 * default directory if it does not already exists. Users is responsible for moving the generated file to the Apache
 * SIS {@code "content/"} site directory.
 *
 * <p><b>This class is designed for Apache SIS operation methods only</b> - this is not a general purpose generator
 * for arbitrary operation methods. The reason is that we make some assumptions in various place (e.g. EPSG name is
 * first, no HTML characters to escape in non-EPSG identifiers, etc.).</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class CoordinateOperationMethods extends HTMLGenerator {
    /**
     * Generates the HTML report.
     *
     * @param  args  no argument expected.
     * @throws IOException if an error occurred while writing the HTML file.
     */
    public static void main(final String[] args) throws IOException {
        final MathTransformFactory factory = DefaultMathTransformFactory.provider();
        final List<OperationMethod> methods = new ArrayList<>(factory.getAvailableMethods(SingleOperation.class));
        methods.removeIf((method) -> method.getClass().getName().endsWith("Mock"));
        Collections.sort(methods, (final OperationMethod o1, final OperationMethod o2) -> {
            int c = category(o1) - category(o2);
            if (c == 0) {  // If the two methods are in the same category, sort by name.
                final String n1 = o1.getName().getCode().replace('(',' ').replace(')',' ').replace('_',' ');
                final String n2 = o2.getName().getCode().replace('(',' ').replace(')',' ').replace('_',' ');
                c = n1.compareTo(n2);
            }
            return c;
        });
        try (CoordinateOperationMethods writer = new CoordinateOperationMethods()) {
            writer.writeIndex(methods);
            for (final OperationMethod method : methods) {
                writer.write(method);
            }
        }
    }

    /**
     * Values returned by {@link #category(OperationMethod)}.
     */
    private static final int CONVERSION = 1, TRANSFORMATION = 3;

    /**
     * Parameters to default to the latitude of origin. We can hardly detect those cases
     * automatically, since the behavior for the default value is hard-coded in Java.
     */
    private final GeneralParameterDescriptor defaultToLatitudeOfOrigin[] = {
        AlbersEqualArea    .STANDARD_PARALLEL_1,
        LambertConformal2SP.STANDARD_PARALLEL_1
    };

    /**
     * Parameters to default to the first standard parallel. We can hardly detect those
     * cases automatically, since the behavior for the default value is hard-coded in Java.
     */
    private final GeneralParameterDescriptor defaultToStandardParallel1[] = {
        AlbersEqualArea    .STANDARD_PARALLEL_2,
        LambertConformal2SP.STANDARD_PARALLEL_2
    };

    /**
     * Parameters to default to the azimuth. We can hardly detect those cases automatically,
     * since the behavior for the default value is hard-coded in Java.
     */
    private final GeneralParameterDescriptor defaultToAzimuth[] = {
        ObliqueMercator      .RECTIFIED_GRID_ANGLE,
//      HotineObliqueMercator.PARAMETERS.descriptor("Angle from Rectified to Skew Grid")
    };

    /**
     * The union of domain of validity of all map projections using a method of the given name.
     * Keys are {@link OperationMethod} names, and values are the union of the domain of validity
     * of all CRS using that {@code OperationMethod}.
     *
     * @see #computeUnionOfAllDomainOfValidity(CRSAuthorityFactory)
     */
    private final Map<String, DefaultGeographicBoundingBox> domainOfValidity;

    /**
     * The object to use for formatting ranges.
     */
    private final RangeFormat rangeFormat;

    /**
     * Creates a new HTML generator for parameters.
     *
     * @throws IOException if an error occurred while writing to the file.
     */
    public CoordinateOperationMethods() throws IOException {
        super("CoordinateOperationMethods.html", "Apache SIS Coordinate Operation Methods", "authority-codes.css");
        domainOfValidity = Map.of();                // TODO: not yet available.
        rangeFormat = new RangeFormat(LOCALE);
        final int header = openTag("header");
        println("h1", "Apache <abbr title=\"Spatial Information System\">SIS</abbr>™ Coordinate Operation Methods");
        int item = openTag("p");
        println("The following tables summarize the coordinate operation methods known to Apache <abbr title=\"Spatial Information System\">SIS</abbr> " + Version.SIS);
        println("together with the recognized parameters. There are three kinds of parameters:");
        closeTags(item);
        openTag("ul", "verbose");
        openTag("li");
        println("The <code>semi-major</code> and <code>semi-minor</code> parameters are needed for all map projections,");
        println("but usually do not need to be specified explicitly since they are inferred from the ellipsoid");
        println("(unless <a href=\"https://sis.apache.org/apidocs/org/apache/sis/referencing/operation/transform/DefaultMathTransformFactory.html\">creating parameterized transforms directly</a>).");
        println("For this reason, those parameters are usually not shown in <a href=\"" + URLs.EPSG + "\"><abbr>EPSG</abbr> repository</a>");
        println("or <a href=\"https://www.ogc.org/standards/wkt-crs/\">Well Known Text</a> (<abbr>WKT</abbr>) definitions.");
        reopenTag("li");
        println("The <code>earth_radius</code> and <code>inverse_flattening</code> parameters (not shown below) are implicitly supported by all map projections.");
        println("They are other ways to specify the ellipsoid (actually rarely used).");
        println("Read and write operations on those implicit parameters are converted into equivalent operations on <code>semi-major</code> and <code>semi-minor</code> parameters.");
        reopenTag("li");
        println("Unless otherwise noticed, all other parameters are mandatory");
        println("(in the sense that they should always be shown in forms, regardless of whether they have default value).");
        closeTags(header);
    }

    /**
     * Writes a table of content.
     *
     * @param  methods  the methods to write to the HTML file.
     * @throws IOException if an error occurred while writing to the file.
     */
    public void writeIndex(final Iterable<? extends OperationMethod> methods) throws IOException {
        final int nav = openTag("nav");
        println("h2", "Table of content:");
        int innerUL  = openTag("ul") + 1;
        int category = 0;
        for (final OperationMethod method : methods) {
            final int nc = category(method);
            if (nc != category) {
                closeTags(innerUL);
                reopenTag("li");
                switch (nc) {
                    case CONVERSION:     println("Conversions");    break;
                    case TRANSFORMATION: println("Tranformations"); break;
                    default: throw new AssertionError(category);
                }
                innerUL = openTag("ul");
                category = nc;
            }
            println("li", "<a href=\"#" + getAnchor(method) + "\">" + escape(method.getName().getCode()) + "</a>");
        }
        closeTags(nav);
    }

    /**
     * Writes identification info and parameters for the given method.
     *
     * @param  method  the method to write to the HTML file.
     * @throws IOException if an error occurred while writing to the file.
     */
    public void write(final OperationMethod method) throws IOException {
        final int article = openTag("article");
        final int header = openTag("header");
        println("h2 id=\"" + getAnchor(method) + '"', escape(method.getName().getCode()));
        closeTags(header);
        final int blockquote = openTag("blockquote");
        writeIdentification(method);
        writeParameters(method.getParameters());
        closeTags(blockquote);
        closeTags(article);
    }

    /**
     * Writes identification info about the given method.
     * This method writes the following information:
     *
     * <ul>
     *   <li>EPSG codes</li>
     *   <li>Aliases</li>
     *   <li>Domain of validity</li>
     * </ul>
     */
    private void writeIdentification(final OperationMethod method) throws IOException {
        final int table = openTag("table class=\"info\"");
        /*
         * ────────────────    EPSG IDENTIFIERS    ────────────────────────────────────
         */
        final StringBuilder buffer = new StringBuilder();
        for (final Identifier id : method.getIdentifiers()) {
            if (Constants.EPSG.equalsIgnoreCase(id.getCodeSpace())) {
                if (buffer.length() != 0) {
                    buffer.append(", ");
                }
                final boolean isDeprecated = isDeprecated(id);
                if (isDeprecated) {
                    buffer.append("<del>");
                }
                buffer.append(id.getCode());
                if (isDeprecated) {
                    buffer.append("</del>");
                }
            }
        }
        if (buffer.length() != 0) {
            final int tr = openTag("tr");
            println("th", "EPSG code:");
            println("td", buffer);
            closeTags(tr);
        }
        /*
         * ────────────────    ALIASES    ─────────────────────────────────────────────
         */
        buffer.setLength(0);
        for (final GenericName alias : method.getAlias()) {
            if (buffer.length() != 0) {
                buffer.append(", ");
            }
            final GenericName head = alias.head();
            if (head == alias || Constants.EPSG.equalsIgnoreCase(head.toString())) {
                buffer.append(alias.tip());
            } else {
                buffer.append("<span class=\"non-epsg\">").append(head).append(":</span>")
                      .append("<code>").append(alias.tip()).append("</code>");
            }
        }
        if (buffer.length() != 0) {
            final int tr = openTag("tr");
            println("th", "Aliases:");
            println("td", buffer);
            closeTags(tr);
        }
        /*
         * ────────────────    DOMAIN OF VALIDITY    ──────────────────────────────────
         */
        buffer.setLength(0);
        final DefaultGeographicBoundingBox domain = getDomainOfValidity(method);
        if (domain != null) {
            openTag("tr");
            println("th", "Domain of validity:");
            println("td", buffer.append(new Latitude (domain.getSouthBoundLatitude())).append(" to ")
                                .append(new Latitude (domain.getNorthBoundLatitude())).append(" and ")
                                .append(new Longitude(domain.getWestBoundLongitude())).append(" to ")
                                .append(new Longitude(domain.getEastBoundLongitude())));
        }
        closeTags(table);
    }

    /**
     * Writes the table of parameters.
     * Table columns will be:
     *
     * <ul>
     *   <li>First EPSG code</li>
     *   <li>Primary name</li>
     *   <li>Reference to remarks, if any</li>
     *   <li>Domain of values</li>
     *   <li>Default values</li>
     * </ul>
     */
    private void writeParameters(final ParameterDescriptorGroup group) throws IOException {
        int table = openTag("table class=\"param\"");
        println("caption", "Operation parameters:");
        openTag("tr");
        if (group.descriptors().isEmpty()) {
            println("td", "None");
            closeTags(table);
            return;
        }
        println("th", "EPSG");
        println("th class=\"sep\"", "Name");
        println("th class=\"sep\"", "Remarks");
        println("th class=\"sep\" colspan=\"3\"", "Value domain");
        println("th class=\"sep\"", "Default");
        final Map<String, Integer> footnotes = new LinkedHashMap<>();
        for (final GeneralParameterDescriptor gp : group.descriptors()) {
            if (isDeprecated(gp)) {
                continue;                                                       // Hide deprecated parameters.
            }
            final ParameterDescriptor<?> param = (ParameterDescriptor<?>) gp;
            reopenTag("tr");
            println("td", escape(getFirstEpsgCode(param.getIdentifiers())));
            writeName(param);
            String remarks = toLocalizedString(param.getRemarks());
            if (remarks != null) {
                Integer index = footnotes.putIfAbsent(remarks, footnotes.size() + 1);
                if (index == null) {
                    index = footnotes.size();
                }
                if (param.getMinimumOccurs() == 0) {
                    remarks = "Optional ";
                } else {
                    final Comparable<?> min = param.getMinimumValue();
                    if ((min instanceof Number n) && n.doubleValue() == ((Number) param.getMaximumValue()).doubleValue()) {
                        remarks = "Unmodifiable ";
                    } else {
                        remarks = "See note ";
                    }
                }
                remarks += toSuperScript(index);
            }
            println("td class=\"sep\"", escape(remarks));
            final String domain = toLocalizedString(Parameters.getValueDomain(param));
            final int s;
            if (domain != null && ((s = domain.indexOf('…')) >= 0)) {
                println("td class=\"sep right\"", domain.substring(0, s).trim());
                println("td class=\"center\"", "…");
                println("td class=\"left\"", domain.substring(s + 1).trim());
            } else {
                println("td class=\"sep center\" colspan=\"3\"", domain);
            }
            println("td class=\"sep\"", escape(getDefaultValue(param, getUnit(param))));
        }
        closeTags(table);
        if (!footnotes.isEmpty()) {
            table = openTag("table class=\"footnotes\"");
            for (final Map.Entry<String,Integer> entry : footnotes.entrySet()) {
                reopenTag("tr");
                println("td", String.valueOf(toSuperScript(entry.getValue())));
                println("td", escape(entry.getKey()));
            }
            closeTags(table);
        }
    }

    /**
     * Writes the primary name and aliases.
     */
    private void writeName(final ParameterDescriptor<?> param) throws IOException {
        final int td = openTag("td class=\"sep\"");
        openTag("details");
        final Identifier name = param.getName();
        final String codeSpace = name.getCodeSpace();
        if (Constants.EPSG.equalsIgnoreCase(codeSpace)) {
            println("summary", escape(name.getCode()));
        } else {
            println("summary", "<span class=\"non-epsg\">" + codeSpace + ":</span>" +
                               "<code>" + name.getCode() + "</code>");
        }
        openTag("table class=\"aliases\"");
        for (final GenericName alias : param.getAlias()) {
            reopenTag("tr");
            println("th", escape(alias.head().toString() + ':'));
            println("td", escape(alias.tip().toString()));
        }
        closeTags(td);
    }

    /**
     * For each {@link OperationMethod} (identified by their name), computes the union of the domain of validity
     * of all CRS using that operation method. The result is a map where keys are {@link OperationMethod} names,
     * and values are the union of the domain of validity of all CRS using that {@code OperationMethod}.
     *
     * <p>This is a costly operation.</p>
     *
     * @todo This method is not yet used. This is pending the implementation of {@code CRSAuthorityFactory} is SIS.
     *
     * @param  factory  the factory to use for getting CRS.
     * @return the union of domain of validity of all map projections using a method of the given name.
     * @throws FactoryException if an error occurred while fetching the list of CRS.
     */
    public static Map<String, DefaultGeographicBoundingBox> computeUnionOfAllDomainOfValidity(
            final CRSAuthorityFactory factory) throws FactoryException
    {
        final Map<String, DefaultGeographicBoundingBox> domainOfValidity = new HashMap<>();
        for (final String code : factory.getAuthorityCodes(DerivedCRS.class)) {
            final CoordinateReferenceSystem crs;
            try {
                crs = factory.createCoordinateReferenceSystem(code);
            } catch (FactoryException e) {
                continue;                                                   // Ignore and inspect the next element.
            }
            if (crs instanceof DerivedCRS derived) {
                final GeographicBoundingBox candidate = CRS.getGeographicBoundingBox(derived);
                if (candidate != null) {
                    final String name = derived.getConversionFromBase().getMethod().getName().getCode();
                    DefaultGeographicBoundingBox validity = domainOfValidity.get(name);
                    if (validity == null) {
                        validity = new DefaultGeographicBoundingBox(candidate);
                        domainOfValidity.put(name, validity);
                    } else {
                        validity.add(candidate);
                    }
                }
            }
        }
        return domainOfValidity;
    }

    /**
     * Returns the domain of validity for the given operation method.
     * If no domain of validity is found, returns {@code null}.
     */
    private DefaultGeographicBoundingBox getDomainOfValidity(final OperationMethod method) {
        DefaultGeographicBoundingBox validity = null;
        for (final GenericName name : method.getAlias()) {
            final String tip = name.tip().toString();
            final DefaultGeographicBoundingBox candidate = domainOfValidity.get(tip);
            if (candidate != null) {
                if (validity == null) {
                    validity = new DefaultGeographicBoundingBox(candidate);
                } else {
                    validity.add(candidate);
                }
            }
        }
        return validity;
    }

    /**
     * Returns the string representation of the given parameter default value,
     * or an empty string (never {@code null}) if none.
     */
    private String getDefaultValue(final ParameterDescriptor<?> param, final String unit) {
        Object defaultValue = param.getDefaultValue();
        if (defaultValue != null) {
            if (defaultValue instanceof Number n) {
                // Trim the fractional part if unnecessary (e.g. "0.0" to "0").
                defaultValue = Numbers.narrowestNumber(n);
            } else if (defaultValue instanceof String s) {
                return s;
            }
        } else {
            if (ArraysExt.contains(defaultToLatitudeOfOrigin, param)) {
                return "Latitude of origin";
            } else if (ArraysExt.contains(defaultToStandardParallel1, param)) {
                return "Standard parallel 1";
            } else if (ArraysExt.contains(defaultToAzimuth, param)) {
                return "Azimuth at projection centre";
            } else if (param.getValueClass() == Boolean.class) {
                defaultValue = Boolean.FALSE;
            }
        }
        return (defaultValue != null) ? defaultValue + unit : "";
    }

    /**
     * Returns the string representation of the given parameter unit,
     * or an empty string (never {@code null}) if none.
     */
    private static String getUnit(final ParameterDescriptor<?> param) {
        final Unit<?> unit = param.getUnit();
        if (unit != null) {
            final String symbol = unit.toString();
            if (!symbol.isEmpty()) {
                if (symbol.equals("°")) {
                    return symbol;
                }
                return " " + symbol;
            }
        }
        return "";
    }

    /**
     * Returns the operation type of the given method.
     */
    private static Class<?> getOperationType(final DefaultOperationMethod method) {
        Class<?> type = method.getOperationType();
        if (type == SingleOperation.class) {
            if (method instanceof Affine) {     // EPSG:9624 - Affine parametric transformation
                type = Transformation.class;
            }
        }
        return type;
    }

    /**
     * Returns a code for sorting methods in categories.
     */
    private static int category(final OperationMethod method) {
        final Class<?> c = getOperationType((DefaultOperationMethod) method);
        if (Conversion    .class.isAssignableFrom(c)) return CONVERSION;
        if (Transformation.class.isAssignableFrom(c)) return TRANSFORMATION;
        return 0;
    }

    /**
     * Returns the first EPSG code found in the given collection, or {@code null} if none.
     */
    private static String getFirstEpsgCode(final Iterable<? extends Identifier> identifiers) {
        for (final Identifier id : identifiers) {
            if (Constants.EPSG.equalsIgnoreCase(id.getCodeSpace())) {
                return id.getCode();
            }
        }
        return null;
    }

    /**
     * Returns an identifier to use for HREF.
     */
    private static String getAnchor(final OperationMethod method) {
        String id = getFirstEpsgCode(method.getIdentifiers());
        if (id == null) {
            id = method.getName().getCode();
        }
        return id.replace(" ", "_").replace("(", "").replace(")", "");
    }

    /**
     * Returns a string representation of the given range, or {@code null} if none.
     */
    private String toLocalizedString(final Range<?> range) {
        return (range != null) ? rangeFormat.format(range) : null;
    }

    /**
     * Returns the superscript character for the given number.
     * This is used for footnotes.
     */
    private static char toSuperScript(final int index) {
        if (index >= 10) {
            throw new IllegalArgumentException("Too many footnotes.");
        }
        return Characters.toSuperScript((char) (index + '0'));
    }
}
