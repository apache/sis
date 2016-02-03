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

import java.util.Locale;
import java.io.File;
import java.io.IOException;

import org.opengis.metadata.Identifier;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.GeocentricCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.crs.GeneralDerivedCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.datum.VerticalDatumType;
import org.opengis.test.report.AuthorityCodesReport;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Version;


/**
 * Generates a list of supported Coordinate Reference Systems in the current directory.
 * This class is for manual execution after the EPSG database has been updated,
 * or the projection implementations changed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public strictfp class CoordinateReferenceSystems extends AuthorityCodesReport {
    /**
     * The symbol to write in from of EPSG code of CRS having an axis order different
     * then the (longitude, latitude) one.
     */
    private static final char YX_ORDER = '\u21B7';

    /**
     * The factory which create CRS instances.
     */
    private final CRSAuthorityFactory factory;

    /**
     * Creates a new instance.
     */
    private CoordinateReferenceSystems() throws FactoryException {
        super(null);
        properties.setProperty("TITLE",           "Apache SIS™ Coordinate Reference System (CRS) codes");
        properties.setProperty("PRODUCT.NAME",    "Apache SIS™");
        properties.setProperty("PRODUCT.VERSION", getVersion());
        properties.setProperty("PRODUCT.URL",     "http://sis.apache.org");
        properties.setProperty("JAVADOC.GEOAPI",  "http://www.geoapi.org/snapshot/javadoc");
        properties.setProperty("FACTORY.NAME",    "EPSG");
        properties.setProperty("FACTORY.VERSION", "7.9");
        properties.setProperty("FACTORY.VERSION.SUFFIX", ", together with other sources");
        properties.setProperty("DESCRIPTION", "<p><b>Notation:</b></p>\n" +
                "<ul>\n" +
                "  <li>The " + YX_ORDER + " symbol in front of authority codes (${PERCENT.ANNOTATED} of them)" +
                " identifies the CRS having an axis order different than (<var>easting</var>, <var>northing</var>).</li>\n" +
                "  <li>The <del>codes with a strike</del> (${PERCENT.DEPRECATED} of them) identify deprecated CRS." +
                " In some cases, the remarks column indicates the replacement.</li>\n" +
                "</ul>");
        factory = org.apache.sis.referencing.CRS.getAuthorityFactory(null);
        add(factory);
    }

    /**
     * Returns the current Apache SIS version, with the {@code -SNAPSHOT} trailing part omitted.
     *
     * @return The current Apache SIS version.
     */
    private static String getVersion() {
        String version = Version.SIS.toString();
        final int snapshot = version.lastIndexOf('-');
        if (snapshot >= 2) {
            version = version.substring(0, snapshot);
        }
        return version;
    }

    /**
     * Generates the HTML report.
     *
     * @param  args Ignored.
     * @throws FactoryException If an error occurred while fetching the CRS.
     * @throws IOException If an error occurred while writing the HTML file.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(final String[] args) throws FactoryException, IOException {
        Locale.setDefault(Locale.US);   // We have to use this hack for now because exceptions are formatted in the current locale.
        final CoordinateReferenceSystems writer = new CoordinateReferenceSystems();
        final File file = writer.write(new File("CoordinateReferenceSystems.html"));
        System.out.println("Created " + file.getAbsolutePath());
    }

    /**
     * Creates the remarks for the given CRS.
     */
    private String getRemark(final CoordinateReferenceSystem crs) {
        if (crs instanceof GeographicCRS) {
            return (crs.getCoordinateSystem().getDimension() == 3) ? "Geographic 3D" : "Geographic";
        }
        if (crs instanceof GeneralDerivedCRS) {
            return ((GeneralDerivedCRS) crs).getConversionFromBase().getMethod().getName().getCode().replace('_', ' ');
        }
        if (crs instanceof GeocentricCRS) {
            final CoordinateSystem cs = crs.getCoordinateSystem();
            if (cs instanceof CartesianCS) {
                return "Geocentric (Cartesian coordinate system)";
            } else if (cs instanceof SphericalCS) {
                return "Geocentric (spherical coordinate system)";
            }
            return "Geocentric";
        }
        if (crs instanceof VerticalCRS) {
            final VerticalDatumType type = ((VerticalCRS) crs).getDatum().getVerticalDatumType();
            return CharSequences.camelCaseToSentence(type.name().toLowerCase(getLocale())) + " height";
        }
        if (crs instanceof CompoundCRS) {
            final StringBuilder buffer = new StringBuilder();
            for (final CoordinateReferenceSystem component : ((CompoundCRS) crs).getComponents()) {
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
     * Invoked when a CRS has been successfully created. This method modifies the default
     * {@link org.opengis.test.report.AuthorityCodesReport.Row} attribute values created
     * by GeoAPI.
     *
     * @param  code    The authority code of the created object.
     * @param  object  The object created from the given authority code.
     * @return The created row, or {@code null} if the row should be ignored.
     */
    @Override
    protected Row createRow(final String code, final IdentifiedObject object) {
        final Row row = super.createRow(code, object);
        final CoordinateReferenceSystem crs = (CoordinateReferenceSystem) object;
        final CoordinateReferenceSystem crsXY = AbstractCRS.castOrCopy(crs).forConvention(AxesConvention.RIGHT_HANDED);
        if (!Utilities.deepEquals(crs.getCoordinateSystem(), crsXY.getCoordinateSystem(), ComparisonMode.IGNORE_METADATA)) {
            row.annotation = YX_ORDER;
        }
        row.remark = getRemark(crs);
        if (object instanceof Deprecable) {
            row.isDeprecated = ((Deprecable) object).isDeprecated();
        }
        /*
         * If the object is deprecated, try to find the reason.
         * Don't take the whole comment, because it may be pretty long.
         */
        if (row.isDeprecated) {
            InternationalString i18n = object.getRemarks();
            for (final Identifier id : object.getIdentifiers()) {
                if (id instanceof Deprecable && ((Deprecable) id).isDeprecated()) {
                    i18n = ((Deprecable) id).getRemarks();
                    break;
                }
            }
            if (i18n != null) {
                row.remark = i18n.toString(getLocale());
            }
        }
        return new ByName(row);
    }

    /**
     * Invoked when a CRS creation failed. This method modifies the default
     * {@link org.opengis.test.report.AuthorityCodesReport.Row} attribute values
     * created by GeoAPI.
     *
     * @param  code      The authority code of the object to create.
     * @param  exception The exception that occurred while creating the identified object.
     * @return The created row, or {@code null} if the row should be ignored.
     */
    @Override
    protected Row createRow(final String code, final FactoryException exception) {
        final Row row = super.createRow(code, exception);
        try {
            row.name = factory.getDescriptionText(code).toString(getLocale());
        } catch (FactoryException e) {
            Logging.unexpectedException(null, CoordinateReferenceSystems.class, "createRow", e);
        }
        String message;
        if (code.startsWith("AUTO2:")) {
            // It is normal to be unable to instantiate an "AUTO" CRS,
            // because those authority codes need parameters.
            message = "Projected";
            row.hasError = false;
        } else {
            message = exception.getMessage();
            if (message.contains("Unable to format units in UCUM")) {
                // Simplify a very long and badly formatted message.
                message = "Unable to format units in UCUM";
            }
        }
        row.remark = message;
        return new ByName(row);
    }

    /**
     * A row with an natural ordering that use the first part of the name before to use the authority code.
     * We use only the part of the name prior some keywords (e.g. {@code "zone"}).
     * For example if the following codes:
     *
     * {@preformat text
     *    EPSG:32609    WGS 84 / UTM zone 9N
     *    EPSG:32610    WGS 84 / UTM zone 10N
     * }
     *
     * We compare only the "WGS 84 / UTM" string, then the code. This is a reasonably easy way to keep a more
     * natural ordering ("9" sorted before "10", "UTM North" projections kept together and same for South).
     */
    private static final class ByName extends Row {
        /**
         * The keywords before which to cut the name. The main intend here is to preserve the
         * "far west", "west", "central west", "central", "central east", "east", "far east" order.
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
         * The keywords after which to cut the name.
         *
         * Note: alphabetical sorting of Roman numbers work for zones from I to VIII inclusive.
         * If there is more zones (for example with "JGD2000 / Japan Plane Rectangular"), then
         * we need to cut before those numbers in order to use sorting by EPSG codes instead.
         *
         * Note 2: if alphabetical sorting is okay for Roman numbers, it is actually preferable
         * because it give better position of names with height like "zone II + NGF IGN69 height".
         */
        private static final String[] CUT_AFTER = {
            " cs ",                     // "JGD2000 / Japan Plane Rectangular CS IX"
            " tm",                      // "ETRS89 / TM35FIN(E,N)" — we want to not interleave them between "TM35" and "TM36".
            " dktm",                    // "ETRS89 / DKTM1 + DVR90 height"
            "-gk",                      // "ETRS89 / ETRS-GK19FIN"
//          " philippines zone ",       // "Luzon 1911 / Philippines zone IV"
//          " california zone ",        // "NAD27 / California zone V"
//          " ngo zone ",               // "NGO 1948 (Oslo) / NGO zone I"
//          " lambert zone ",           // "NTF (Paris) / Lambert zone II + NGF IGN69 height"
            "fiji 1956 / utm zone "     // Two zones: 60S and 1S with 60 before 1.
        };

        /**
         * A string derived from the {@link #name} to use for sorting.
         */
        private final String reducedName;

        /**
         * Creates a new row as a copy of the given row, but with a different natural ordering.
         */
        ByName(final Row row) {
            super(row);
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
        }

        /**
         * If the string ends with a number optionally followed by "N" or "S", replaces the hemisphere
         * symbol by a sign and makes sure that the number uses at least 3 digits (e.g. "2N" → "+002").
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
         * Compares this row wit the given row for ordering by name.
         */
        @Override public int compareTo(final Row o) {
            int n = reducedName.compareTo(((ByName) o).reducedName);
            if (n == 0) {
                n = super.compareTo(o);
            }
            return n;
        }
    }
}
