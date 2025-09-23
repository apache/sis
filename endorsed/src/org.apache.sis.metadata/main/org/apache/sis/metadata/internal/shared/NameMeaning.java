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
package org.apache.sis.metadata.internal.shared;

import java.util.Map;
import java.util.Locale;
import javax.measure.Unit;
import org.opengis.parameter.*;
import org.opengis.referencing.*;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.internal.shared.DefinitionURI;
import org.apache.sis.metadata.iso.citation.Citations;


/**
 * The meaning of some parts of URN in the {@code "ogc"} namespace.
 * The meaning are defined by <cite>OGC Naming Authority</cite> (OGCNA) or other OGC sources.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 *
 * @see DefinitionURI
 * @see <a href="https://www.ogc.org/ogcna">https://www.ogc.org/ogcna</a>
 * @see <a href="https://portal.ogc.org/files/?artifact_id=24045">Definition identifier URNs in OGC namespace</a>
 */
public final class NameMeaning {
    /**
     * Subtypes of {@link IdentifiedObject} for which an object type is defined.
     * For each interface at index <var>i</var>, the type is {@code TYPES[i]}.
     *
     * <p>For performance reasons, most frequently used types should be first.</p>
     */
    private static final Class<?>[] CLASSES = {
        CoordinateReferenceSystem.class,
        Datum.class,
        Ellipsoid.class,
        PrimeMeridian.class,
        CoordinateSystem.class,
        CoordinateSystemAxis.class,
        CoordinateOperation.class,
        OperationMethod.class,
        ParameterDescriptor.class,
        ReferenceSystem.class,
        Unit.class
    };

    /**
     * The object types for instances of {@link #CLASSES}.
     * See {@link DefinitionURI} javadoc for a list of object types in URN.
     *
     * <p>Types not yet listed (waiting to see if there is a use for them):</p>
     *
     * "group"              for  ParameterValueGroup.class;
     * "verticalDatumType"  for  VerticalDatumType.class;
     * "pixelInCell"        for  PixelInCell.class;
     * "rangeMeaning"       for  RangeMeaning.class;
     * "axisDirection"      for  AxisDirection.class;
     */
    private static final String[] TYPES = {
        "crs",
        "datum",
        "ellipsoid",
        "meridian",
        "cs",
        "axis",
        "coordinateOperation",
        "method",
        "parameter",
        "referenceSystem",
        "uom"
    };

    /**
     * Naming authorities allowed to appear in {@code "urn:ogc:def:"}.
     * This map serves two purposes:
     *
     * <ul>
     *   <li>Tell if a given authority is one of the authorities allowed by the OGC namespace.</li>
     *   <li>Opportunistically fix the letter case.</li>
     * </ul>
     *
     * <b>Note on the case:</b>
     * <q>Name type specification — definitions</q> (OGC 09-048) writes authorities in upper cases,
     * while <a href="http://www.opengis.net/def/auth/">http://www.opengis.net/def/auth/</a> uses lower cases.
     * Apache SIS uses upper cases for now. The lower/upper case policy should be kept consistent with the policy
     * used by {@link org.apache.sis.referencing.factory.MultiAuthoritiesFactory} for its keys.
     *
     * @see org.apache.sis.referencing.factory.MultiAuthoritiesFactory
     * @see <a href="http://www.opengis.net/def/auth/">http://www.opengis.net/def/auth/</a>
     */
    private static final Map<String,String> AUTHORITIES = Map.ofEntries(
            entry(Constants.EPSG),      // IOGP
            entry(Constants.OGC),       // Open Geospatial Consortium
            entry("OGC-WFS"),           // OGC Web Feature Service
            entry("SI"),                // Système International d'Unités
            entry(Constants.UCUM),      // Unified Code for Units of Measure
            entry("UNSD"),              // United Nations Statistics Division
            entry("USNO"));             // United States Naval Observatory

    /**
     * Returns an {@link #AUTHORITIES} entry for the common case
     * where the code space is the same as the key.
     */
    private static Map.Entry<String,String> entry(final String authority) {
        return Map.entry(authority, authority);
    }

    /**
     * Do not allow instantiation of this class.
     */
    private NameMeaning() {
    }

    /**
     * Formats the given identifier using the {@code "ogc:urn:def:"} syntax with possible heuristic changes to
     * the given values. The identifier code space, version and code are appended omitting any characters that
     * are not valid for a Unicode identifier. If some information are missing in the given identifier, then
     * this method returns {@code null}. This method tries to "fix" the given values using some heuristic
     * knowledge about the meaning of URN.
     *
     * @param  type       the object type.
     * @param  authority  the authority as one of the values documented in {@link DefinitionURI} javadoc.
     * @param  version    the code version, or {@code null}. This is the only optional information.
     * @param  code       the code.
     * @return an identifier using the URN syntax, or {@code null} if a mandatory information is missing.
     */
    public static String toURN(final Class<?> type, final String authority, String version, String code) {
        if (type == null || authority == null || code == null) {
            return null;
        }
        final String key = authority.toUpperCase(Locale.US);
        String codeSpace = AUTHORITIES.get(key);
        if (codeSpace == null) {
            /*
             * If the given authority is not one of the authorities that we expected for the OGC namespace,
             * verify if we can related it to one of the specifications enumerated in the Citations class.
             * For example if the user gave us "OGP" as the authority, we will replace that by "IOGP" (the
             * new name for that organization).
             */
            final Citation c = Citations.fromName(key);
            codeSpace = Citations.toCodeSpace(c);
            if (codeSpace == null || AUTHORITIES.get(codeSpace) == null) {
                return null;            // Not an authority that we recognize for the OGC namespace.
            }
            version = getVersion(c);    // Unconditionally overwrite the user-specified version.
            /*
             * If the above lines resulted in a change of codespace, we may need to concatenate the authority
             * with the code for preserving information. The main use case is WMS codes like "CRS:84":
             *
             *   1) Citations.fromName("CRS") gave us Citations.WMS (version 1.3) as the authority.
             *   2) getCodeSpace(Citations.WMS) gave us "OGC", which is indeed the codespace used in URN.
             *   3) OGC Naming Authority – Procedures (OGC-09-046r2) said that "CRS:84" should be formatted
             *      as "urn:ogc:def:crs:OGC:1.3:CRS84". We already got the "OGC" and "1.3" parts with above
             *      steps, the last part is to replace "84" by "CRS84".
             */
            if (!authority.equals(codeSpace) && !code.startsWith(authority)) {
                code = authority + code;    // Intentionally no ':' separator.
            }
        }
        final StringBuilder buffer = new StringBuilder(DefinitionURI.PREFIX);
loop:   for (int p=0; ; p++) {
            final String part;
            switch (p) {
                case 0:  part = toObjectType(type); break;
                case 1:  part = codeSpace;          break;
                case 2:  part = version;            break;
                case 3:  part = code;               break;
                default: break loop;
            }
            if (!Strings.appendUnicodeIdentifier(buffer.append(DefinitionURI.SEPARATOR), '\u0000', part, ".-", false)) {
                /*
                 * Only the version (p = 2) is optional. All other fields are mandatory.
                 * If no character has been added for a mandatory field, we cannot build a URN.
                 */
                if (p != 2) {
                    return null;
                }
            }
        }
        return buffer.toString();
    }

    /**
     * Returns the "object type" part of an OGC URN for the given class, or {@code null} if unknown.
     * See {@link DefinitionURI} javadoc for a list of object types in URN.
     *
     * @param  type  the class for which to get the URN type.
     * @return the URN type, or {@code null} if unknown.
     */
    public static String toObjectType(final Class<?> type) {
        for (int i=0; i<CLASSES.length; i++) {
            if (CLASSES[i].isAssignableFrom(type)) {
                return TYPES[i];
            }
        }
        return null;
    }

    /**
     * Returns the version of the namespace managed by the given authority.
     * Current Apache SIS implementation searches this information in the {@link Citation#getEdition()} property.
     * This approach is based on the assumption that the authority is some specification document or reference to
     * a database, not an organization. However, this policy may be revisited in any future SIS version.
     *
     * @param  authority  the authority from which to get a version, or {@code null}.
     * @return the version, or {@code null} if none.
     */
    public static String getVersion(final Citation authority) {
        if (authority != null) {
            final InternationalString i18n = authority.getEdition();
            if (i18n != null) {
                return i18n.toString(Locale.US);
            }
        }
        return null;
    }
}
