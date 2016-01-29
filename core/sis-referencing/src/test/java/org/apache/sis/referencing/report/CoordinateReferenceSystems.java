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
        /*
         * We have to use this hack for now because exceptions are formatted in the current locale.
         */
        Locale.setDefault(getLocale());
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
            final InternationalString i18n = object.getRemarks();
            if (i18n != null) {
                String remark = i18n.toString(getLocale());
                final int s = Math.max(remark.lastIndexOf("Superseded"),
                              Math.max(remark.lastIndexOf("superseded"),
                              Math.max(remark.lastIndexOf("Replaced"),
                              Math.max(remark.lastIndexOf("replaced"),
                              Math.max(remark.lastIndexOf("See"),
                                       remark.lastIndexOf("see"))))));
                if (s >= 0) {
                    final int start = remark.lastIndexOf('.', s) + 1;
                    final int end = remark.indexOf('.', s);
                    remark = (end >= 0) ? remark.substring(start, end) : remark.substring(start);
                    remark = CharSequences.trimWhitespaces(remark.replace('¶', '\n').trim());
                    if (!remark.isEmpty()) {
                        row.remark = remark;
                    }
                }
            }
        }
        return row;
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
        return row;
    }
}
