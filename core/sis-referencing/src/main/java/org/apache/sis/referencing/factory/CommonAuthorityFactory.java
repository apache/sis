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
import java.util.LinkedHashMap;
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
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.datum.DatumFactory;
import org.opengis.referencing.datum.EngineeringDatum;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.internal.simple.SimpleIdentifier;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;


/**
 * Creates coordinate reference systems in the "{@code OGC}" or "{@code CRS}" namespace.
 * All namespaces recognized by this factory are defined by the Open Geospatial Consortium (OGC).
 * Most codes map to one of the constants in the {@link CommonCRS} enumeration.
 *
 * <table class="sis">
 *   <caption>Recognized Coordinate Reference System codes</caption>
 *   <tr>
 *     <th>Code</th>
 *     <th>Name</th>
 *     <th>Datum type</th>
 *     <th>CS type</th>
 *     <th>Axis direction</th>
 *     <th>Units</th>
 *   </tr><tr>
 *     <td>1</td>
 *     <td>Computer display</td>
 *     <td>{@linkplain org.apache.sis.referencing.crs.DefaultEngineeringCRS Engineering}</td>
 *     <td>{@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian}</td>
 *     <td>(east, south)</td>
 *     <td>pixels</td>
 *   </tr><tr>
 *     <td>27</td>
 *     <td>NAD27</td>
 *     <td>{@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS Geographic}</td>
 *     <td>{@linkplain org.apache.sis.referencing.cs.DefaultEllipsoidalCS Ellipsoidal}</td>
 *     <td>(east, north)</td>
 *     <td>degrees</td>
 *   </tr><tr>
 *     <td>83</td>
 *     <td>NAD83</td>
 *     <td>{@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS Geographic}</td>
 *     <td>{@linkplain org.apache.sis.referencing.cs.DefaultEllipsoidalCS Ellipsoidal}</td>
 *     <td>(east, north)</td>
 *     <td>degrees</td>
 *   </tr><tr>
 *     <td>84</td>
 *     <td>WGS84</td>
 *     <td>{@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS Geographic}</td>
 *     <td>{@linkplain org.apache.sis.referencing.cs.DefaultEllipsoidalCS Ellipsoidal}</td>
 *     <td>(east, north)</td>
 *     <td>degrees</td>
 *   </tr><tr>
 *     <td>88</td>
 *     <td>NAVD88</td>
 *     <td>{@linkplain org.apache.sis.referencing.crs.DefaultVerticalCRS Vertical}</td>
 *     <td>{@linkplain org.apache.sis.referencing.cs.DefaultVerticalCS Vertical}</td>
 *     <td>up</td>
 *     <td>metres</td>
 *   </tr>
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
     * The codes known to this factory, associated with their CRS type. This is set to an empty map
     * at {@code CommonAuthorityFactory} construction time, but filled only when first needed.
     */
    private final Map<String,Class<?>> codes;

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
                new SimpleIdentifier(null, Constants.OGC, false),
                new SimpleIdentifier(null, Constants.CRS, false)
        ));
        c.freeze();
        authority = c;
        codes = new LinkedHashMap<>();
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
     *
     * @param  type The spatial reference objects type.
     * @return The set of authority codes for spatial reference objects of the given type.
     * @throws FactoryException if this method failed to provide the set of codes.
     */
    @Override
    public Set<String> getAuthorityCodes(final Class<? extends IdentifiedObject> type) throws FactoryException {
        ArgumentChecks.ensureNonNull("type", type);
        final boolean all = type.isAssignableFrom(SingleCRS.class);
        if (!all && !SingleCRS.class.isAssignableFrom(type)) {
            return Collections.emptySet();
        }
        synchronized (codes) {
            if (codes.isEmpty()) {
                add(Constants.CRS84, GeographicCRS.class);   // Put first the codes that are most likely to be requested.
                add(Constants.CRS83, GeographicCRS.class);
                add(Constants.CRS27, GeographicCRS.class);
                add(Constants.CRS88, VerticalCRS.class);
                add(Constants.CRS1,  EngineeringCRS.class);
            }
        }
        return all ? Collections.unmodifiableSet(codes.keySet()) : new FilteredCodes(codes, type).keySet();
    }

    /**
     * Adds an element in the {@link #codes} map, witch check against duplicated values.
     */
    private void add(final byte code, final Class<? extends SingleCRS> type) throws FactoryException {
        if (codes.put(String.valueOf(code).intern(), type) != null) {
            throw new FactoryException();    // Should never happen, but we are paranoiac.
        }
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
        if (c.startsWith(Constants.CRS)) {
            /*
             * "trimAuthority" removed "CRS" when it was separated from the code, as in "CRS:84".
             * This block removes "CRS" when it is concatenated with the code, as in "CRS84".
             */
            c = CharSequences.trimWhitespaces(c.substring(Constants.CRS.length()));
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
            case Constants.CRS1:  return displayCRS();
            case Constants.CRS84: crs = CommonCRS.WGS84; break;
            case Constants.CRS83: crs = CommonCRS.NAD83; break;
            case Constants.CRS27: crs = CommonCRS.NAD27; break;
            case Constants.CRS88: return CommonCRS.Vertical.NAVD88.crs();
            default: throw noSuchAuthorityCode(CoordinateReferenceSystem.class, code);
        }
        return crs.normalizedGeographic();
    }

    /**
     * Returns the "Computer display" reference system (CRS:1). This is rarely used.
     */
    private synchronized CoordinateReferenceSystem displayCRS() throws FactoryException {
        if (displayCRS == null) {
            final CSFactory csFactory = DefaultFactories.forBuildin(CSFactory.class);
            final CartesianCS cs = csFactory.createCartesianCS(
                    Collections.singletonMap(CartesianCS.NAME_KEY, "Computer display"),
                    csFactory.createCoordinateSystemAxis(Collections.singletonMap(CartesianCS.NAME_KEY, "i"), "i", AxisDirection.EAST, NonSI.PIXEL),
                    csFactory.createCoordinateSystemAxis(Collections.singletonMap(CartesianCS.NAME_KEY, "j"), "j", AxisDirection.SOUTH, NonSI.PIXEL));

            final Map<String,Object> properties = new HashMap<>(4);
            properties.put(EngineeringDatum.NAME_KEY, cs.getName());
            properties.put(EngineeringDatum.ANCHOR_POINT_KEY, "Origin is in upper left.");
            displayCRS = DefaultFactories.forBuildin(CRSFactory.class).createEngineeringCRS(properties,
                         DefaultFactories.forBuildin(DatumFactory.class).createEngineeringDatum(properties), cs);
        }
        return displayCRS;
    }
}
