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

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.Map;
import java.util.TreeMap;
import java.util.Optional;
import java.util.Collections;
import java.io.IOException;
import org.opengis.metadata.Identifier;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.datum.RealizationMethod;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.internal.DeprecatedCode;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Version;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.factory.CommonAuthorityFactory;
import org.apache.sis.referencing.factory.MultiAuthoritiesFactory;
import org.apache.sis.referencing.factory.sql.EPSGFactory;
import org.apache.sis.util.internal.shared.URLs;
import org.apache.sis.util.iso.DefaultNameSpace;
import org.apache.sis.util.logging.Logging;


/**
 * Generates a list of supported Coordinate Reference Systems in the current directory.
 * This class is for manual execution after the <abbr>EPSG</abbr> database has been updated,
 * or after some implementations of operation methods changed.
 *
 * <h2>WARNING:</h2>
 * this class implements heuristic rules for nicer sorting (e.g. of CRS having numbers as Roman letters).
 * Those heuristic rules were determined specifically for the EPSG dataset expanded with WMS codes.
 * This class is not likely to produce good results for any other authorities, and many need to be updated
 * after any upgrade of the <abbr>EPSG</abbr> dataset.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class CoordinateReferenceSystems extends HTMLGenerator {
    /**
     * Generates the <abbr>HTML</abbr> report.
     *
     * @param  args  ignored.
     * @throws FactoryException if an error occurred while fetching the CRS.
     * @throws IOException if an error occurred while writing the HTML file.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(final String[] args) throws FactoryException, IOException {
        Locale.setDefault(LOCALE);   // We have to use this hack for now because exceptions are formatted in the current locale.
        try (var writer = new CoordinateReferenceSystems()) {
            writer.write();
        }
    }

    /**
     * Words to ignore in a datum name in order to detect if a CRS name is the acronym of the datum name.
     */
    private static final Set<String> DATUM_WORDS_TO_IGNORE = Set.of(
            "of",           // VIVD:   Virgin Islands Vertical Datum of 2009
            "de",           // RRAF:   Reseau de Reference des Antilles Francaises
            "des",          // RGAF:   Reseau Geodesique des Antilles Francaises
            "la",           // RGR:    Reseau Geodesique de la Reunion
            "et",           // RGSPM:  Reseau Geodesique de Saint Pierre et Miquelon
            "para",         // SIRGAS: Sistema de Referencia Geocentrico para America del Sur 1995
            "del",          // SIRGAS: Sistema de Referencia Geocentrico para America del Sur 1995
            "las",          // SIRGAS: Sistema de Referencia Geocentrico para las AmericaS 2000
            "Tides");       // MLWS:   Mean Low Water Spring Tides

    /**
     * The keywords before which to cut the CRS names when sorting by alphabetical order.
     * The intent is to preserve the "far west", "west", "central west", "central",
     * "central east", "east", "far east" order.
     */
    private static final String[] CUT_BEFORE = {
        " far west",        // "MAGNA-SIRGAS / Colombia Far West zone"
        " far east",
        " west",            // "Bogota 1975 / Colombia West zone"
        " east",            // "Bogota 1975 / Colombia East Central zone"
        " central",         // "Korean 1985 / Central Belt" (between "East Belt" and "West Belt")
        " old central",     // "NAD Michigan / Michigan Old Central"
        " bogota zone",     // "Bogota 1975 / Colombia Bogota zone"
        // Do not declare "North" and "South" as it causes confusion with "WGS 84 / North Pole" and other cases.
    };

    /**
     * The keywords after which to cut the CRS names when sorting by alphabetical order.
     *
     * Note: alphabetical sorting of Roman numbers work for zones from I to VIII inclusive.
     * If there is more zones (for example with "JGD2000 / Japan Plane Rectangular"), then
     * we need to cut before those numbers in order to use sorting by EPSG codes instead.
     *
     * Note 2: if alphabetical sorting is okay for Roman numbers, it is actually preferable
     * because it give better position of names with height like "zone II + NGF-IGN69 height".
     */
    private static final String[] CUT_AFTER = {
        " cs ",                     // "JGD2000 / Japan Plane Rectangular CS IX"
        " tm",                      // "ETRS89 / TM35FIN(E,N)" — we want to not interleave them between "TM35" and "TM36".
        " dktm",                    // "ETRS89 / DKTM1 + DVR90 height"
        "-gk",                      // "ETRS89 / ETRS-GK19FIN"
        " philippines zone ",       // "Luzon 1911 / Philippines zone IV"
        " california zone ",        // "NAD27 / California zone V"
        " ngo zone ",               // "NGO 1948 (Oslo) / NGO zone I"
        " lambert zone ",           // "NTF (Paris) / Lambert zone II + NGF-IGN69 height"
        "fiji 1956 / utm zone "     // Two zones: 60S and 1S with 60 before 1.
    };

    /**
     * The symbol to write in front of <abbr>EPSG</abbr> code of <abbr>CRS</abbr>
     * having an axis order different than the (longitude, latitude) order.
     */
    private static final char YX_ORDER = '\u21B7';

    /**
     * The factory which create CRS instances.
     */
    private final CRSAuthorityFactory factory;

    /**
     * Version of the <abbr>EPSG</abbr> geodetic dataset.
     */
    private final String versionEPSG;

    /**
     * Creates a new instance.
     */
    private CoordinateReferenceSystems() throws FactoryException, IOException {
        super("CoordinateReferenceSystems.html",
              "Coordinate Reference Systems recognized by Apache SIS™",
              "crs-report.css");

        final var ogc   = new CommonAuthorityFactory();
        final var epsg  = new EPSGFactory(Map.of("showDeprecated", Boolean.TRUE));
        final var asSet = Set.of(epsg);
        factory = new MultiAuthoritiesFactory(List.of(ogc, epsg), asSet, asSet, asSet);
        final GeographicCRS anyCRS = factory.createGeographicCRS("EPSG:4326");
        versionEPSG = IdentifiedObjects.getIdentifier(anyCRS, Citations.EPSG).getVersion();
    }

    /**
     * Writes the report after all rows have been collected.
     */
    private void write() throws IOException, FactoryException {
        int numSupportedCRS  = 0;
        int numDeprecatedCRS = 0;
        int numAnnotatedCRS  = 0;
        final var rows = new ArrayList<Row>(10000);
        for (final String code : factory.getAuthorityCodes(CoordinateReferenceSystem.class)) {
            final var row = new Row();
            row.code = escape(code).toString();
            try {
                row.setValues(factory, factory.createCoordinateReferenceSystem(code));
            } catch (FactoryException exception) {
                if (row.setValues(factory, exception, code)) {
                    continue;
                }
            }
            rows.add(row);
            if (!row.hasError)       numSupportedCRS++;
            if (row.annotation != 0) numAnnotatedCRS++;
            if (row.isDeprecated)    numDeprecatedCRS++;
        }
        final int numCRS = rows.size();
        sortRows(rows);     // May add new rows as section separators.
        println("h1", "Coordinate Reference Systems recognized by Apache <abbr title=\"Spatial Information System\">SIS</abbr>™");
        int item = openTag("p");
        println("This list is generated from the <abbr>EPSG</abbr> geodetic dataset version " + versionEPSG + ", together with other sources.");
        println("Those Coordinate Reference Systems (<abbr>CRS</abbr>) are supported by the Apache <abbr>SIS</abbr>™ library version " + Version.SIS);
        println("(provided that a <a href=\"" + URLs.EPSG_INSTALL + "\">connection to an <abbr>EPSG</abbr> database</a> exists),");
        println("except those with a red text in the last column.");
        println("There are " + numCRS + " codes, " + (100 * numSupportedCRS / numCRS) + "% of them being supported.");
        closeTags(item);
        println("p", "<b>Notation:</b>");
        item = openTag("ul");
        openTag("li");
        println("The " + YX_ORDER + " symbol in front of authority codes (" + Math.round(100f * numAnnotatedCRS / numCRS) + "% of them) "
                + "identifies left-handed coordinate systems (for example with <var>latitude</var> axis before <var>longitude</var>).");
        reopenTag("li");
        println("The <del>codes with a strike</del> (" + Math.round(100f * numDeprecatedCRS / numCRS) + "% of them) "
                + "identify deprecated definitions. In some cases, the remarks column indicates the replacement.");
        reopenTag("li");
        println("Coordinate Reference Systems are grouped by their reference frame or datum.");
        closeTags(item);
        item = openTag("table");
        printlnWithoutIndentation("<tr><th class=\"narrow\"></th><th class=\"left-align\">Code</th><th class=\"left-align\">Name</th><th class=\"left-align\">Remarks</th></tr>");
        final var buffer = new StringBuilder();
        int counterForHighlight = 0;
        for (final Row row : rows) {
            row.write(buffer, (counterForHighlight & 2) != 0);
            printlnWithoutIndentation(buffer);
            buffer.setLength(0);
            counterForHighlight++;
            if (row.isSectionHeader) {
                counterForHighlight = 0;
            }
        }
        closeTags(item);
    }

    /**
     * Creates the text to show in the "Remarks" column for the given CRS.
     */
    private static String getRemark(final CoordinateReferenceSystem crs) {
        if (crs instanceof GeographicCRS) {
            return (crs.getCoordinateSystem().getDimension() == 3) ? "Geographic 3D" : "Geographic";
        }
        if (crs instanceof DerivedCRS derived) {
            final OperationMethod method = derived.getConversionFromBase().getMethod();
            final Identifier identifier = IdentifiedObjects.getIdentifier(method, Citations.EPSG);
            if (identifier != null) {
                return "<a href=\"CoordinateOperationMethods.html#" + identifier.getCode()
                       + "\">" + method.getName().getCode().replace('_', ' ') + "</a>";
            }
        }
        if (crs instanceof GeodeticCRS) {
            final CoordinateSystem cs = crs.getCoordinateSystem();
            if (cs instanceof CartesianCS) {
                return "Geocentric (Cartesian coordinate system)";
            } else if (cs instanceof SphericalCS) {
                return "Geocentric (spherical coordinate system)";
            }
            return "Geodetic";
        }
        if (crs instanceof VerticalCRS vertical) {
            VerticalDatum datum = vertical.getDatum();
            if (datum != null) {
                Optional<RealizationMethod> method = datum.getRealizationMethod();
                if (method.isPresent()) {
                    String name = method.get().name().toLowerCase(LOCALE);
                    return CharSequences.camelCaseToSentence(name) + " realization method";
                }
            }
        }
        if (crs instanceof CompoundCRS compound) {
            final var buffer = new StringBuilder();
            for (final CoordinateReferenceSystem component : compound.getComponents()) {
                if (buffer.length() != 0) {
                    buffer.append(" + ");
                }
                buffer.append(getRemark(component));
            }
            return buffer.toString();
        }
        if (crs instanceof EngineeringCRS) {
            return "Engineering (" + crs.getCoordinateSystem().getName().getCode() + ')';
        }
        return "";
    }

    /**
     * Omits the trailing number, if any.
     * For example if the given name is "Abidjan 1987", then this method returns "Abidjan".
     */
    private static String omitTrailingNumber(String name) {
        int i = CharSequences.skipTrailingWhitespaces(name, 0, name.length());
        while (i != 0) {
            final char c = name.charAt(--i);
            if (c < '0' || c > '9') {
                name = name.substring(0, CharSequences.skipTrailingWhitespaces(name, 0, i+1));
                break;
            }
        }
        return name;
    }

    /**
     * If the first word of the CRS name seems to be an acronym of the datum name,
     * puts that acronym in a {@code <abbr title="datum name">...</abbr>} element.
     */
    private static String insertAbbreviationTitle(final String crsName, final String datumName) {
        int s = crsName.indexOf(' ');
        if (s < 0) s = crsName.length();
        int p = crsName.indexOf('(');
        if (p >= 0 && p < s) s = p;
        p = datumName.indexOf('(');
        if (p < 0) p = datumName.length();
        final String acronym = crsName.substring(0, s);
        final String ar = omitTrailingNumber(acronym);
        final String dr = omitTrailingNumber(datumName.substring(0, p));
        if (dr.startsWith(ar)) {
            return crsName;                                 // Avoid redudancy between CRS name and datum name.
        }
        /*
         * If the first CRS word does not seem to be an acronym of the datum name, verify
         * if there is some words that we should ignore in the datum name and try again.
         */
        if (!CharSequences.isAcronymForWords(ar, dr)) {
            final String[] words = (String[]) CharSequences.split(dr, ' ');
            int n = 0;
            for (final String word : words) {
                if (!DATUM_WORDS_TO_IGNORE.contains(word)) {
                    words[n++] = word;
                }
            }
            if (n == words.length || n < 2) {
                return crsName;
            }
            final StringBuilder b = new StringBuilder();
            for (int i=0; i<n; i++) {
                if (i != 0) b.append(' ');
                b.append(words[i]);
            }
            if (!CharSequences.isAcronymForWords(ar, b)) {
                return crsName;
            }
        }
        return "<abbr title=\"" + datumName + "\">" + acronym + "</abbr>" + crsName.substring(s);
    }




    /**
     * A row with a natural ordering that use the first part of the name before to use the authority code.
     * Every {@link String} fields in this class must be valid HTML. If some text is expected to print
     * {@code <} or {@code >} characters, then those characters need to be escaped to their HTML entities.
     *
     * <p>Content of each {@code Row} instance is written in the following order:</p>
     * <ol>
     *   <li>{@link #annotation} if explicitly set (the default is none).</li>
     *   <li>{@link #code}</li>
     *   <li>{@link #name}</li>
     *   <li>{@link #remark}</li>
     * </ol>
     *
     * <p>Other attributes ({@link #isSectionHeader}, {@link #isDeprecated} and {@link #hasError})
     * are not directly written in the table, but affect their styling.</p>
     *
     * <h2>Rules for sorting the rows</h2>
     * We use only the part of the name prior some keywords (e.g. {@code "zone"}).
     * For example in the following codes:
     *
     * <pre class="text">
     *    EPSG:32609    WGS 84 / UTM zone 9N
     *    EPSG:32610    WGS 84 / UTM zone 10N</pre>
     *
     * We compare only the "WGS 84 / UTM" string, then the code. This is a reasonably easy way to keep a more
     * natural ordering ("9" sorted before "10", "UTM North" projections kept together and same for South).
     */
    private static final class Row implements Comparable<Row> {
        /**
         * {@code true} if this row should actually be used as a section header.
         * We insert rows with this flag set to {@code true} for splitting the large table is smaller sections.
         */
        boolean isSectionHeader;

        /**
         * The datum name, or {@code null} if unknown.
         * If non-null, this is used for grouping CRS names by sections.
         */
        String section;

        /**
         * The authority code in HTML.
         */
        String code;

        /**
         * The object name in HTML, or {@code null} if none. By default, this field is set to the value of
         * <code>{@linkplain IdentifiedObject#getName()}.{@linkplain Identifier#getCode() getCode()}</code>.
         */
        String name;

        /**
         * A remark in HTML to display after the name, or {@code null} if none.
         */
        private String remark;

        /**
         * A string derived from the {@link #name} to use for sorting.
         */
        private String reducedName;

        /**
         * A small symbol to put before the {@linkplain #code} and {@linkplain #name}, or 0 (the default) if none.
         * For example, it can indicate a <abbr>CRS</abbr> having unusual axes order.
         */
        char annotation;

        /**
         * {@code true} if this authority code is deprecated, or {@code false} otherwise.
         */
        boolean isDeprecated;

        /**
         * {@code true} if an exception occurred while creating the identified object.
         * If {@code true}, then the {@link #remark} field will contains the exception localized message.
         */
        boolean hasError;

        /**
         * Creates a new row.
         */
        Row() {
        }

        /**
         * Invoked when the <abbr>CRS</abbr> cannot be constructed because of the given error.
         *
         * @param  factory  the factory which created the <abbr>CRS</abbr>.
         * @param  cause    the reason why the <abbr>CRS</abbr> cannot be constructed.
         * @param  code     the authority code without <abbr>HTML</abbr> escapes.
         * @return whether to ignore this row.
         */
        final boolean setValues(final CRSAuthorityFactory factory, final FactoryException cause, final String code) {
            if (code.startsWith(Constants.PROJ4 + DefaultNameSpace.DEFAULT_SEPARATOR)) {
                return true;
            }
            String message = cause.getMessage();
            if (message == null) {
                message = cause.toString();
            }
            remark = escape(message).toString();
            hasError = true;
            try {
                name = toLocalizedString(factory.getDescriptionText(CoordinateReferenceSystem.class, code).get());
            } catch (FactoryException e) {
                Logging.unexpectedException(null, CoordinateReferenceSystems.class, "createRow", e);
            }
            if (code.startsWith("AUTO2:")) {
                // It is normal to be unable to instantiate an "AUTO" CRS,
                // because those authority codes need parameters.
                hasError = false;
                remark = "Projected";
                setSection(CommonCRS.WGS84.datum(true));
            } else {
                if (cause instanceof NoSuchIdentifierException e) {
                    remark = '“' + e.getIdentifierCode() + "” operation method is not yet supported.";
                } else {
                    remark = cause.getLocalizedMessage();
                }
                setSection(null);
            }
            return false;
        }

        /**
         * Invoked when a <abbr>CRS</abbr> has been successfully created.
         *
         * @param  factory  the factory which created the <abbr>CRS</abbr>.
         * @param  crs      the object created from the authority code.
         * @return the created row, or {@code null} if the row should be ignored.
         */
        final void setValues(final CRSAuthorityFactory factory, CoordinateReferenceSystem crs) {
            name = escape(crs.getName().getCode()).toString();
            final CoordinateReferenceSystem crsXY = AbstractCRS.castOrCopy(crs).forConvention(AxesConvention.RIGHT_HANDED);
            if (!Utilities.deepEquals(crs.getCoordinateSystem(), crsXY.getCoordinateSystem(), ComparisonMode.IGNORE_METADATA)) {
                annotation = YX_ORDER;
            }
            remark = getRemark(crs);
            /*
             * If the object is deprecated, find the replacement.
             * We do not take the whole comment because it may be pretty long.
             */
            if (crs instanceof Deprecable dep) {
                isDeprecated = dep.isDeprecated();
                if (isDeprecated) {
                    String replacedBy = null;
                    InternationalString i18n = crs.getRemarks().orElse(null);
                    for (final Identifier id : crs.getIdentifiers()) {
                        if (id instanceof Deprecable did && did.isDeprecated()) {
                            i18n = did.getRemarks().orElse(null);
                            if (id instanceof DeprecatedCode dc) {
                                replacedBy = dc.replacedBy;
                            }
                            break;
                        }
                    }
                    remark = toLocalizedString(i18n);
                    /*
                     * If a replacement exists for a deprecated CRS, use the datum of the replacement instead of
                     * the datum of the deprecated CRS for determining in which section to put the CRS. The reason
                     * is that some CRS are deprecated because they were associated to the wrong datum, in which
                     * case the deprecated CRS would appear in the wrong section if we do not apply this correction.
                     */
                    if (replacedBy != null) try {
                        crs = factory.createCoordinateReferenceSystem("EPSG:" + replacedBy);
                    } catch (FactoryException e) {
                        // Ignore - keep the datum of the deprecated object.
                    }
                }
            }
            setSection(CRS.getSingleComponents(crs).get(0).getDatum());
        }

        /**
         * Computes the {@link #reducedName} field value.
         * It determines the section where the <abbr>CRS</abbr> will be placed.
         */
        private void setSection(final Datum datum) {
            /*
             * Get a copy of the name in all lower case.
             */
            final StringBuilder b = new StringBuilder(name);
            for (int i=0; i<b.length(); i++) {
                b.setCharAt(i, Character.toLowerCase(b.charAt(i)));
            }
            /*
             * Cut the string to a shorter length if we find a keyword.
             * This will result in many string equals, which will then be sorted by EPSG codes.
             * This is useful when the EPSG codes give a better ordering than the alphabetic one
             * (for example with Roman numbers).
             */
            int s = 0;
            for (final String keyword : CUT_BEFORE) {
                int i = b.lastIndexOf(keyword);
                if (i > 0 && (s == 0 || i < s)) s = i;
            }
            for (final String keyword : CUT_AFTER) {
                int i = b.lastIndexOf(keyword);
                if (i >= 0) {
                    i += keyword.length();
                    if (i > s) s = i;
                }
            }
            if (s != 0) b.setLength(s);
            uniformizeZoneNumber(b);
            reducedName = b.toString();
            if (datum != null) {
                section = datum.getName().getCode().replace('_', ' ');
                name = insertAbbreviationTitle(name, section);
            }
        }

        /**
         * If the string ends with a number optionally followed by "N" or "S", replaces the hemisphere
         * symbol by a sign and makes sure that the number uses at least 3 digits (e.g. "2N" → "+002").
         * This string will be used for better sorting order.
         */
        private static void uniformizeZoneNumber(final StringBuilder b) {
            if (b.indexOf("/") < 0) {
                /*
                 * Do not process names like "WGS 84". We want to process only names like "WGS 84 / UTM zone 2N",
                 * otherwise the replacement of "WGS 84" by "WGS 084" causes unexpected sorting.
                 */
                return;
            }
            int  i = b.length();
            char c = b.charAt(i - 1);
            if (c == ')') {
                // Ignore suffix like " (ftUS)".
                i = b.lastIndexOf(" (");
                if (i < 0) return;
                c = b.charAt(i - 1);
            }
            char sign;
            switch (c) {
                default:            sign =  0;       break;
                case 'e': case 'n': sign = '+'; i--; break;
                case 'w': case 's': sign = '-'; i--; break;
            }
            int upper = i;
            do {
                if (i == 0) return;
                c = b.charAt(--i);
            } while (c >= '0' && c <= '9');
            switch (upper - ++i) {
                case 2: b.insert(i,  '0'); upper++;  break;     // Found 2 digits.
                case 1: b.insert(i, "00"); upper+=2; break;     // Only one digit found.
                case 0: return;                                 // No digit.
            }
            if (sign != 0) {
                b.insert(i, sign);
                upper++;
            }
            b.setLength(upper);
        }

        /**
         * Writes this row to the given stream.
         *
         * @param  out        where to write this row.
         * @param  highlight  whether to highlight this row.
         * @throws IOException if an error occurred while writing this row.
         */
        final void write(final StringBuilder out, final boolean highlight) {
            if (isSectionHeader) {
                out.append("<tr class=\"separator\"><td colspan=\"4\">").append(name).append("</td></tr>");
                return;
            }
            out.append("<tr");
            if (highlight) out.append(" class=\"HL\"");
            out.append("><td class=\"narrow\">");
            if (annotation != 0) out.append(annotation);
            out.append("</td><td>");
            if (code != null) {
                out.append("<code>");
                if (isDeprecated) out.append("<del>");
                out.append(code);
                if (isDeprecated) out.append("</del>");
                out.append("</code>");
            }
            out.append("</td><td>");
            if (name != null) out.append(name);
            out.append("</td><td");
            if (hasError) out.append(" class=\"error\"");
            else if (isDeprecated) out.append(" class=\"warning\"");
            out.append('>');
            if (remark != null) out.append(remark);
            out.append("</td></tr>");
        }

        /**
         * Compares this row with the given row for ordering by name and authority code.
         */
        @Override
        public int compareTo(final Row o) {
            int n = reducedName.compareTo(o.reducedName);
            if (n == 0) {
                try {
                    n = Integer.compare(Integer.parseInt(code), Integer.parseInt(o.code));
                } catch (NumberFormatException e) {
                    n = code.compareTo(o.code);
                }
            }
            return n;
        }
    }

    /**
     * Sorts the rows, then inserts sections between CRS instances that use different datums.
     */
    private static void sortRows(final List<Row> rows) {
        Collections.sort(rows);
        @SuppressWarnings("SuspiciousToArrayCall")
        final Row[] data = rows.toArray(Row[]::new);
        final var sections = new TreeMap<String,String>();
        for (final Row row : data) {
            final String section = row.section;
            if (section != null) {
                sections.put(CharSequences.toASCII(section).toString().toLowerCase(), section);
            }
        }
        rows.clear();
        /*
         * Recopy the rows, but section-by-section. We do this sorting here instead of in the Row.compareTo(Row)
         * method in order to preserve the alphabetical order of rows with unknown datum.
         * Algorithm below is inefficient, but this class should be rarely used anyway and only by site maintainer.
         */
        for (final String section : sections.values()) {
            final Row separator = new Row();
            separator.isSectionHeader = true;
            separator.name = section;
            rows.add(separator);
            boolean found = false;
            for (int i=0; i<data.length; i++) {
                final Row row = data[i];
                if (row != null) {
                    if (row.section != null) {
                        found = section.equals(row.section);
                    }
                    if (found) {
                        rows.add(row);
                        data[i] = null;
                        found = true;
                    }
                }
            }
        }
        boolean found = false;
        for (final Row row : data) {
            if (row != null) {
                if (!found) {
                    final Row separator = new Row();
                    separator.isSectionHeader = true;
                    separator.name = "Unknown";
                    rows.add(separator);
                }
                rows.add(row);
                found = true;
            }
        }
    }
}
