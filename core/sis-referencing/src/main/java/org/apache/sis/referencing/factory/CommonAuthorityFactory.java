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
package org.apache.sis.referencing.factory;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.Arrays;
import java.util.Locale;
import javax.measure.unit.NonSI;
import org.opengis.util.NameFactory;
import org.opengis.util.FactoryException;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.internal.simple.SimpleIdentifier;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.datum.DefaultEngineeringDatum;
import org.apache.sis.referencing.crs.DefaultEngineeringCRS;
import org.apache.sis.referencing.cs.DefaultCartesianCS;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;


/**
 * The factory for coordinate reference systems in the "{@code OGC}" or "{@code CRS}" namespace.
 * All namespaces recognized by this factory are defined by the Open Geospatial Consortium (OGC).
 * Most codes map to one of the constants in the {@link CommonCRS} enumeration.
 *
 * <table class="sis">
 *   <caption>Codes recognized by <code>CommonAuthorityFactory</code></caption>
 *   <tr><th>Code</th> <th>Name</th>             <th>Datum type</th>  <th>CS type</th>     <th>Axis direction</th></tr>
 *   <tr><td>   1</td> <td>Computer display</td> <td>Engineering</td> <td>Cartesian</td>   <td>East, South</td></tr>
 *   <tr><td>  27</td> <td>NAD27</td>            <td>Geodetic</td>    <td>Ellipsoidal</td> <td>East, South</td></tr>
 *   <tr><td>  83</td> <td>NAD83</td>            <td>Geodetic</td>    <td>Ellipsoidal</td> <td>East, South</td></tr>
 *   <tr><td>  84</td> <td>WGS84</td>            <td>Geodetic</td>    <td>Ellipsoidal</td> <td>East, South</td></tr>
 *   <tr><td>  88</td> <td>NAVD88</td>           <td>Vertical</td>    <td>Vertical</td>    <td>Up</td></tr>
 * </table>
 *
 * <div class="section">Note on codes in CRS namespace</div>
 * The format is usually <code>"CRS:</code><var>n</var><code>"</code> where <var>n</var> is a number like 27, 83 or 84.
 * However this factory is lenient and allows the {@code CRS} part to be repeated as in {@code "CRS:CRS84"}.
 * It also accepts {@code "OGC"} as a synonymous of the {@code "CRS"} namespace.
 *
 * <div class="note"><b>Examples:</b>
 * {@code "CRS:27"}, {@code "CRS:83"}, {@code "CRS:84"}, {@code "CRS:CRS84"}, {@code "OGC:CRS84"}.</div>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 *
 * @see CommonCRS
 * @see Citations#OGC
 */
public class CommonAuthorityFactory extends GeodeticAuthorityFactory implements CRSAuthorityFactory {
    /**
     * An optional prefix put in front of code. For example a code may be {@code "CRS84"} instead of a plain {@code "84"}.
     * This is useful in order to understand URN syntax like {@code "urn:ogc:def:crs:OGC:1.3:CRS84"}.
     * Must be uppercase for this implementation, but parsing will be case-insensitive.
     */
    private static final String PREFIX = "CRS";

    /**
     * Authority codes known to this factory.
     * We are better to declare first the codes that are most likely to be requested.
     */
    private static final String[] CODES = {"84", "83", "27", "88", "1"};

    /**
     * The set of codes known to this factory. Created when first needed.
     */
    private Set<String> codes;

    /**
     * The authority for this factory.
     */
    private final Citation authority;

    /**
     * The "Computer display" reference system (CRS:1). Created when first needed.
     *
     * @see #displayCRS()
     */
    private CoordinateReferenceSystem displayCRS;

    /**
     * Constructs a default factory for the {@code CRS} authority.
     *
     * @param nameFactory The factory to use for parsing authority code as {@link org.opengis.util.GenericName} instances.
     */
    public CommonAuthorityFactory(final NameFactory nameFactory) {
        super(nameFactory);
        final DefaultCitation c = new DefaultCitation(Citations.OGC);
        c.setIdentifiers(Arrays.asList(
                new SimpleIdentifier(null, "OGC", false),
                new SimpleIdentifier(null, "CRS", false)
        ));
        c.freeze();
        authority = c;
    }

    /**
     * Returns the organization responsible for definition of the CRS codes recognized by this factory.
     * The authority for this factory is the <cite>Open Geospatial Consortium</cite>.
     *
     * @return The OGC authority.
     */
    @Override
    public Citation getAuthority() {
        return authority;
    }

    /**
     * Provides a complete set of the known codes provided by this authority.
     * The returned set contains only numeric identifiers like {@code "84"}, {@code "27"}, <i>etc</i>.
     * The authority name ({@code "CRS"}) is not included in the character strings.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")     // Because the returned set is unmodifiable.
    public synchronized Set<String> getAuthorityCodes(final Class<? extends IdentifiedObject> type) {
        if (codes == null) {
            codes = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(CODES)));
        }
        return codes;
    }

    /**
     * Creates an object from the specified code.
     * The default implementation delegates to {@link #createCoordinateReferenceSystem(String)}.
     *
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public IdentifiedObject createObject(final String code) throws FactoryException {
        return createCoordinateReferenceSystem(code);
    }

    /**
     * Creates a coordinate reference system from the specified code.
     *
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CoordinateReferenceSystem createCoordinateReferenceSystem(final String code) throws FactoryException {
        String c = trimAuthority(code).toUpperCase(Locale.US);
        if (c.startsWith(PREFIX)) {
            /*
             * "trimAuthority" removed "CRS" when it was separated from the code, as in "CRS:84".
             * This block removes "CRS" when it is concatenated with the code, as in "CRS84".
             */
            c = c.substring(PREFIX.length());
        }
        final int n;
        try {
            n = Integer.parseInt(c);
        } catch (NumberFormatException exception) {
            // If a number can not be parsed, then this is an invalid authority code.
            NoSuchAuthorityCodeException e = noSuchAuthorityCode(CoordinateReferenceSystem.class, code);
            e.initCause(exception);
            throw e;
        }
        final CommonCRS crs;
        switch (n) {
            case  1: return displayCRS();
            case 84: crs = CommonCRS.WGS84; break;
            case 83: crs = CommonCRS.NAD83; break;
            case 27: crs = CommonCRS.NAD27; break;
            case 88: return CommonCRS.Vertical.NAVD88.crs();
            default: throw noSuchAuthorityCode(CoordinateReferenceSystem.class, code);
        }
        return crs.normalizedGeographic();
    }

    /**
     * Returns the "Computer display" reference system (CRS:1). This is rarely used.
     */
    private synchronized CoordinateReferenceSystem displayCRS() {
        if (displayCRS == null) {
            final DefaultCartesianCS cs = new DefaultCartesianCS(
                    Collections.singletonMap(DefaultCartesianCS.NAME_KEY, "Computer display"),
                    createDisplayAxis("i", AxisDirection.EAST),
                    createDisplayAxis("j", AxisDirection.SOUTH));

            final Map<String,Object> properties = new HashMap<>(4);
            properties.put(DefaultEngineeringDatum.NAME_KEY, cs.getName());
            properties.put(DefaultEngineeringDatum.ANCHOR_POINT_KEY, "Origin is in upper left.");
            displayCRS = new DefaultEngineeringCRS(properties, new DefaultEngineeringDatum(properties), cs);
        }
        return displayCRS;
    }

    /**
     * Creates a coordinate axis for "Computer display" (CRS:1).
     */
    private static CoordinateSystemAxis createDisplayAxis(final String abbreviation, final AxisDirection direction) {
        return new DefaultCoordinateSystemAxis(Collections.singletonMap(DefaultCoordinateSystemAxis.NAME_KEY, abbreviation),
                abbreviation, direction, NonSI.PIXEL);
    }
}
