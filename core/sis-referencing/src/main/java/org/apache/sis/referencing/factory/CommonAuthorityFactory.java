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
import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.Arrays;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;
import javax.measure.quantity.Length;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.datum.DatumFactory;
import org.opengis.referencing.datum.EngineeringDatum;
import org.apache.sis.internal.referencing.GeodeticObjectBuilder;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.iso.DefaultNameSpace;
import org.apache.sis.util.iso.SimpleInternationalString;

import static org.apache.sis.internal.referencing.provider.TransverseMercator.zone;
import static org.apache.sis.internal.referencing.provider.TransverseMercator.centralMeridian;


/**
 * Creates coordinate reference systems in the "{@code OGC}", "{@code CRS}" or {@code "AUTO(2)"} namespaces.
 * All namespaces recognized by this factory are defined by the Open Geospatial Consortium (OGC).
 * Most codes map to one of the constants in the {@link CommonCRS} enumeration.
 *
 * <table class="sis">
 *   <caption>Recognized Coordinate Reference System codes</caption>
 *   <tr>
 *     <th>Namespace</th>
 *     <th>Code</th>
 *     <th>Name</th>
 *     <th>Datum type</th>
 *     <th>CS type</th>
 *     <th>Axis direction</th>
 *     <th>Units</th>
 *   </tr><tr>
 *     <td>CRS</td>
 *     <td>1</td>
 *     <td>Computer display</td>
 *     <td>{@linkplain org.apache.sis.referencing.crs.DefaultEngineeringCRS Engineering}</td>
 *     <td>{@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian}</td>
 *     <td>(east, south)</td>
 *     <td>pixels</td>
 *   </tr><tr>
 *     <td>CRS</td>
 *     <td>27</td>
 *     <td>NAD27</td>
 *     <td>{@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS Geographic}</td>
 *     <td>{@linkplain org.apache.sis.referencing.cs.DefaultEllipsoidalCS Ellipsoidal}</td>
 *     <td>(east, north)</td>
 *     <td>degrees</td>
 *   </tr><tr>
 *     <td>CRS</td>
 *     <td>83</td>
 *     <td>NAD83</td>
 *     <td>{@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS Geographic}</td>
 *     <td>{@linkplain org.apache.sis.referencing.cs.DefaultEllipsoidalCS Ellipsoidal}</td>
 *     <td>(east, north)</td>
 *     <td>degrees</td>
 *   </tr><tr>
 *     <td>CRS</td>
 *     <td>84</td>
 *     <td>WGS84</td>
 *     <td>{@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS Geographic}</td>
 *     <td>{@linkplain org.apache.sis.referencing.cs.DefaultEllipsoidalCS Ellipsoidal}</td>
 *     <td>(east, north)</td>
 *     <td>degrees</td>
 *   </tr><tr>
 *     <td>CRS</td>
 *     <td>88</td>
 *     <td>NAVD88</td>
 *     <td>{@linkplain org.apache.sis.referencing.crs.DefaultVerticalCRS Vertical}</td>
 *     <td>{@linkplain org.apache.sis.referencing.cs.DefaultVerticalCS Vertical}</td>
 *     <td>up</td>
 *     <td>metres</td>
 *   </tr><tr>
 *     <td>AUTO2</td>
 *     <td>42001</td>
 *     <td>WGS 84 / Auto UTM</td>
 *     <td>{@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS Projected}</td>
 *     <td>{@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian}</td>
 *     <td>(east, north)</td>
 *     <td>user-specified</td>
 *   </tr><tr>
 *     <td>AUTO2</td>
 *     <td>42002</td>
 *     <td>WGS 84 / Auto Tr Mercator</td>
 *     <td>{@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS Projected}</td>
 *     <td>{@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian}</td>
 *     <td>(east, north)</td>
 *     <td>user-specified</td>
 *   </tr><tr>
 *     <td>AUTO2</td>
 *     <td>42003</td>
 *     <td>WGS 84 / Auto Orthographic</td>
 *     <td>{@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS Projected}</td>
 *     <td>{@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian}</td>
 *     <td>(east, north)</td>
 *     <td>user-specified</td>
 *   </tr><tr>
 *     <td>AUTO2</td>
 *     <td>42004</td>
 *     <td>WGS 84 / Auto Equirectangular</td>
 *     <td>{@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS Projected}</td>
 *     <td>{@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian}</td>
 *     <td>(east, north)</td>
 *     <td>user-specified</td>
 *   </tr><tr>
 *     <td>AUTO2</td>
 *     <td>42005</td>
 *     <td>WGS 84 / Auto Mollweide</td>
 *     <td>{@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS Projected}</td>
 *     <td>{@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian}</td>
 *     <td>(east, north)</td>
 *     <td>user-specified</td>
 *   </tr>
 * </table>
 *
 * <div class="section">Note on codes in CRS namespace</div>
 * The format is usually "{@code CRS:}<var>n</var>" where <var>n</var> is a number like 27, 83 or 84.
 * However this factory is lenient and allows the {@code CRS} part to be repeated as in {@code "CRS:CRS84"}.
 * It also accepts {@code "OGC"} as a synonymous of the {@code "CRS"} namespace.
 *
 * <div class="note"><b>Examples:</b>
 * {@code "CRS:27"}, {@code "CRS:83"}, {@code "CRS:84"}, {@code "CRS:CRS84"}, {@code "OGC:CRS84"}.</div>
 *
 * <div class="section">Note on codes in AUTO(2) namespace</div>
 * The format is usually "{@code AUTO2:}<var>n</var>,<var>factor</var>,<var>λ₀</var>,<var>φ₀</var>"
 * where <var>n</var> is a number between 42001 and 42005 inclusive, <var>factor</var> is a conversion
 * factor from the CRS units to metres (e.g. 0.3048 for a CRS with axes in feet) and (<var>λ₀</var>,<var>φ₀</var>)
 * is the longitude and latitude of a point in the projection centre.
 *
 * <div class="note"><b>Examples:</b>
 * {@code "AUTO2:42001,1,-100,45"} identifies a Universal Transverse Mercator (UTM) projection
 * for a zone containing the point at (45°N, 100°W).</div>
 *
 * Codes in the {@code "AUTO"} namespace are the same than codes in the {@code "AUTO2"} namespace, except that
 * the {@linkplain org.apache.sis.measure.Units#valueOfEPSG(int) EPSG code} of the desired unit of measurement
 * was used instead than the unit factor.
 * The {@code "AUTO"} namespace was defined in the <cite>Web Map Service</cite> (WMS) 1.1.1 specification
 * while the {@code "AUTO2"} namespace is defined in WMS 1.3.0.
 * In Apache SIS implementation, the unit parameter (either as factor or as EPSG code) is optional and default to metres.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 *
 * @see CommonCRS
 */
public class CommonAuthorityFactory extends GeodeticAuthorityFactory implements CRSAuthorityFactory {
    /**
     * The {@value} prefix for a code identified by parameters.
     * This is defined in annexes B.7 to B.11 of WMS 1.3 specification.
     * The {@code "AUTO(2)"} namespaces are not considered by SIS as real authorities.
     */
    private static final String AUTO2 = "AUTO2";

    /**
     * The namespaces of codes defined by OGC.
     *
     * @see #getCodeSpaces()
     */
    private static final Set<String> CODESPACES = Collections.unmodifiableSet(
            new LinkedHashSet<String>(Arrays.asList(Constants.OGC, Constants.CRS, "AUTO", AUTO2)));

    /**
     * The bit for saying that a namespace is the legacy {@code "AUTO"} namespace.
     */
    private static final int LEGACY_MASK = 0x80000000;

    /**
     * First code in the AUTO(2) namespace.
     */
    private static final int FIRST_PROJECTION_CODE = 42001;

    /**
     * Names of objects in the AUTO(2) namespace for codes from 42001 to 42005 inclusive.
     * Those names are defined in annexes B.7 to B.11 of WMS 1.3 specification.
     *
     * @see #getDescriptionText(String)
     */
    private static final String[] PROJECTION_NAMES = {
        "WGS 84 / Auto UTM",
        "WGS 84 / Auto Tr. Mercator",
        "WGS 84 / Auto Orthographic",
        "WGS 84 / Auto Equirectangular",
        "WGS 84 / Auto Mollweide"
    };

    /**
     * The parameter separator for codes in the {@code "AUTO(2)"} namespace.
     */
    static final char SEPARATOR = ',';

    /**
     * The codes known to this factory, associated with their CRS type. This is set to an empty map
     * at {@code CommonAuthorityFactory} construction time and filled only when first needed.
     */
    private final Map<String,Class<?>> codes;

    /**
     * The "Computer display" reference system (CRS:1). Created when first needed.
     *
     * @see #displayCRS()
     */
    private CoordinateReferenceSystem displayCRS;

    /**
     * The coordinate system for map projection in metres, created when first needed.
     */
    private volatile CartesianCS projectedCS;

    /**
     * Constructs a default factory for the {@code CRS} authority.
     */
    public CommonAuthorityFactory() {
        codes = new LinkedHashMap<String,Class<?>>();
    }

    /**
     * Returns the specification that defines the codes recognized by this factory. The definitive source for this
     * factory is OGC <a href="http://www.opengeospatial.org/standards/wms">Web Map Service</a> (WMS) specification,
     * also available as the ISO 19128 <cite>Geographic Information — Web map server interface</cite> standard.
     *
     * <p>While the authority is WMS, the {@linkplain org.apache.sis.xml.IdentifierSpace#getName() namespace}
     * of that authority is set to {@code "OGC"}. Apache SIS does that for consistency with the namespace used
     * in URNs (for example {@code "urn:ogc:def:crs:OGC:1.3:CRS84"}).</p>
     *
     * @return The <cite>"Web Map Service"</cite> authority.
     *
     * @see #getCodeSpaces()
     * @see Citations#WMS
     */
    @Override
    public Citation getAuthority() {
        return Citations.WMS;
    }

    /**
     * Rewrites the given code in a canonical format.
     * If the code can not be reformatted, then this method returns {@code null}.
     */
    static String reformat(final String code) {
        try {
            return format(Integer.parseInt(code.substring(skipNamespace(code) & ~LEGACY_MASK)));
        } catch (Exception e) {  // (NoSuchAuthorityCodeException | NumberFormatException) on the JDK7 branch.
            Logging.recoverableException(Logging.getLogger(Loggers.CRS_FACTORY), CommonAuthorityFactory.class, "reformat", e);
            return null;
        }
    }

    /**
     * Returns the index where the code begins, ignoring spaces and the {@code "OGC"}, {@code "CRS"}, {@code "AUTO"},
     * {@code "AUTO1"} or {@code "AUTO2"} namespaces if present. If a namespace is found and is a legacy one, then
     * this {@link #LEGACY_MASK} bit will be set.
     *
     * @return Index where the code begin, possibly with the {@link #LEGACY_MASK} bit set.
     * @throws NoSuchAuthorityCodeException if an authority is present but is not one of the recognized authorities.
     */
    private static int skipNamespace(final String code) throws NoSuchAuthorityCodeException {
        int isLegacy = 0;
        int s = code.indexOf(DefaultNameSpace.DEFAULT_SEPARATOR);
        if (s >= 0) {
            final int end   = CharSequences.skipTrailingWhitespaces(code, 0, s);
            final int start = CharSequences.skipLeadingWhitespaces (code, 0, end);
            if (!regionMatches(Constants.CRS, code, start, end) &&
                !regionMatches(Constants.OGC, code, start, end))
            {
                boolean isRecognized = false;
                final int length = AUTO2.length() - 1;
                if (code.regionMatches(true, start, AUTO2, 0, length)) {
                    switch (end - start - length) {         // Number of extra characters after "AUTO".
                        case 0: {                           // Namespace is exactly "AUTO" (ignoring case).
                            isRecognized = true;
                            isLegacy = LEGACY_MASK;
                            break;
                        }
                        case 1: {                           // Namespace has one more character than "AUTO".
                            final char c = code.charAt(end - 1);
                            isRecognized = (c >= '1' && c <= '2');
                            if (c == '1') {
                                isLegacy = LEGACY_MASK;
                            }
                        }
                    }
                }
                if (!isRecognized) {
                    throw new NoSuchAuthorityCodeException(Errors.format(Errors.Keys.UnknownAuthority_1,
                            CharSequences.trimWhitespaces(code, 0, s)), Constants.OGC, code);
                }
            }
        }
        s = CharSequences.skipLeadingWhitespaces(code, s+1, code.length());
        /*
         * Above code removed the "CRS" part when it is used as a namespace, as in "CRS:84".
         * The code below removes the "CRS" prefix when it is concatenated within the code,
         * as in "CRS84". Together, those two checks handle redundant codes like "CRS:CRS84"
         * (malformed code, but seen in practice).
         */
        if (code.regionMatches(true, s, Constants.CRS, 0, Constants.CRS.length())) {
            s = CharSequences.skipLeadingWhitespaces(code, s + Constants.CRS.length(), code.length());
        }
        if (s >= code.length()) {
            throw new NoSuchAuthorityCodeException(Errors.format(Errors.Keys.EmptyArgument_1, "code"), Constants.OGC, code);
        }
        return s | isLegacy;
    }

    /**
     * Provides a complete set of the known codes provided by this factory.
     * The returned set contains a namespace followed by numeric identifiers
     * like {@code "CRS:84"}, {@code "CRS:27"}, {@code "AUTO2:42001"}, <i>etc</i>.
     *
     * @param  type The spatial reference objects type.
     * @return The set of authority codes for spatial reference objects of the given type.
     * @throws FactoryException if this method failed to provide the set of codes.
     */
    @Override
    public Set<String> getAuthorityCodes(final Class<? extends IdentifiedObject> type) throws FactoryException {
        ArgumentChecks.ensureNonNull("type", type);
        if (!type.isAssignableFrom(SingleCRS.class) && !SingleCRS.class.isAssignableFrom(type)) {
            return Collections.emptySet();
        }
        synchronized (codes) {
            if (codes.isEmpty()) {
                add(Constants.CRS1,  EngineeringCRS.class);
                add(Constants.CRS27, GeographicCRS.class);
                add(Constants.CRS83, GeographicCRS.class);
                add(Constants.CRS84, GeographicCRS.class);
                add(Constants.CRS88, VerticalCRS.class);
                for (int code = FIRST_PROJECTION_CODE; code < FIRST_PROJECTION_CODE + PROJECTION_NAMES.length; code++) {
                    add(code, ProjectedCRS.class);
                }
            }
        }
        return new FilteredCodes(codes, type).keySet();
    }

    /**
     * Formats the given code with a {@code "CRS:"} or {@code "AUTO2:"} prefix.
     */
    private static String format(final int code) {
        return ((code >= FIRST_PROJECTION_CODE) ? AUTO2 : Constants.CRS) + DefaultNameSpace.DEFAULT_SEPARATOR + code;
    }

    /**
     * Adds an element in the {@link #codes} map, witch check against duplicated values.
     */
    private void add(final int code, final Class<? extends SingleCRS> type) throws FactoryException {
        assert (code >= FIRST_PROJECTION_CODE) == (ProjectedCRS.class.isAssignableFrom(type)) : code;
        if (codes.put(format(code), type) != null) {
            throw new FactoryException();    // Should never happen, but we are paranoiac.
        }
    }

    /**
     * Returns the namespaces defined by the OGC specifications implemented by this factory.
     * At the difference of other factories, the namespaces of {@code CommonAuthorityFactory}
     * are quite different than the {@linkplain #getAuthority() authority} title or identifier:
     *
     * <ul>
     *   <li><b>Authority:</b> {@code "WMS"} (for <cite>"Web Map Services"</cite>)</li>
     *   <li><b>Namespaces:</b> {@code "CRS"}, {@code "AUTO"}, {@code "AUTO2"}.
     *       The {@code "OGC"} namespace is also accepted for compatibility reason,
     *       but its scope is wider than the above-cited namespaces.</li>
     * </ul>
     *
     * @return A set containing at least the {@code "CRS"}, {@code "AUTO"} and {@code "AUTO2"} strings.
     *
     * @see #getAuthority()
     * @see Citations#WMS
     */
    @Override
    public Set<String> getCodeSpaces() {
        return CODESPACES;
    }

    /**
     * Returns a description of the object corresponding to a code.
     * The description can be used for example in a combo box in a graphical user interface.
     *
     * <p>Codes in the {@code "AUTO(2)"} namespace do not need parameters for this method.
     * But if parameters are nevertheless specified, then they will be taken in account.</p>
     *
     * <table class="sis">
     *   <caption>Examples</caption>
     *   <tr><th>Argument value</th>                <th>Return value</th></tr>
     *   <tr><td>{@code CRS:84}</td>                <td>WGS 84</td></tr>
     *   <tr><td>{@code AUTO2:42001}</td>           <td>WGS 84 / Auto UTM</td></tr>
     *   <tr><td>{@code AUTO2:42001,1,-100,45}</td> <td>WGS 84 / UTM zone 47N</td></tr>
     * </table>
     *
     * @param  code Value in the CRS or AUTO(2) code space.
     * @return A description of the object.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if an error occurred while fetching the description.
     */
    @Override
    public InternationalString getDescriptionText(final String code) throws FactoryException {
        final int s = skipNamespace(code) & ~LEGACY_MASK;
        final String localCode = code.substring(s, CharSequences.skipTrailingWhitespaces(code, s, code.length()));
        if (localCode.indexOf(SEPARATOR) < 0) {
            /*
             * For codes in the "AUTO(2)" namespace without parameters, we can not rely on the default implementation
             * since it would fail to create the ProjectedCRS instance. Instead we return a generic description.
             * Note that we do not execute this block if parametes were specified. If there is parameters,
             * then we instead rely on the default implementation for a more accurate description text.
             */
            final int codeValue;
            try {
                codeValue = Integer.parseInt(localCode);
            } catch (NumberFormatException exception) {
                throw noSuchAuthorityCode(localCode, code, exception);
            }
            final int i = codeValue - FIRST_PROJECTION_CODE;
            if (i >= 0 && i < PROJECTION_NAMES.length) {
                return new SimpleInternationalString(PROJECTION_NAMES[i]);
            }
        }
        return new SimpleInternationalString(createCoordinateReferenceSystem(localCode).getName().getCode());
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
     * This method performs the following steps:
     *
     * <ol>
     *   <li>Skip the {@code "OGC"}, {@code "CRS"}, {@code "AUTO"}, {@code "AUTO1"} or {@code "AUTO2"} namespace
     *       if present (ignoring case). All other namespaces will cause an exception to be thrown.</li>
     *   <li>Skip the {@code "CRS"} prefix if present. This additional check is for accepting codes like
     *       {@code "OGC:CRS84"} (not a valid CRS code, but seen in practice).</li>
     *   <li>In the remaining text, interpret the integer value as documented in this class javadoc.
     *       Note that some codes require coma-separated parameters after the integer value.</li>
     * </ol>
     *
     * @param  code Value allocated by OGC.
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CoordinateReferenceSystem createCoordinateReferenceSystem(final String code) throws FactoryException {
        ArgumentChecks.ensureNonNull("code", code);
        final String localCode;
        final boolean isLegacy;
        String complement = null;
        { // Block for keeping 'start' and 'end' variables locale.
            int start = skipNamespace(code);
            isLegacy = (start & LEGACY_MASK) != 0;
            start &= ~LEGACY_MASK;
            final int startOfParameters = code.indexOf(SEPARATOR, start);
            int end = CharSequences.skipTrailingWhitespaces(code, start, code.length());
            if (startOfParameters >= 0) {
                complement = code.substring(startOfParameters + 1);
                end = CharSequences.skipTrailingWhitespaces(code, start, startOfParameters);
            }
            localCode = code.substring(start, end);
        }
        int codeValue = 0;
        double[] parameters = ArraysExt.EMPTY_DOUBLE;
        try {
            codeValue = Integer.parseInt(localCode);
            if (complement != null) {
                parameters = CharSequences.parseDoubles(complement, SEPARATOR);
            }
        } catch (NumberFormatException exception) {
            throw noSuchAuthorityCode(localCode, code, exception);
        }
        /*
         * At this point we have isolated the code value from the parameters (if any). Verify the number of arguments.
         * Then codes in the AUTO(2) namespace are delegated to a separated method while codes in the CRS namespaces
         * are handled below.
         */
        final int count = parameters.length;
        if (codeValue >= FIRST_PROJECTION_CODE) {
            int expected;
            short errorKey = 0;
            if (count < (expected = 2)) {
                errorKey = Errors.Keys.TooFewArguments_2;
            } else if (count > (expected = 3)) {
                errorKey = Errors.Keys.TooManyArguments_2;
            }
            if (errorKey == 0) {
                return createAuto(code, codeValue, isLegacy,
                        (count > 2) ? parameters[0] : isLegacy ? Constants.EPSG_METRE : 1,
                                      parameters[count - 2],
                                      parameters[count - 1]);
            }
            throw new NoSuchAuthorityCodeException(Errors.format(errorKey, expected, count), AUTO2, localCode, code);
        }
        if (count != 0) {
            throw new NoSuchAuthorityCodeException(Errors.format(Errors.Keys.UnexpectedCharactersAfter_2,
                    localCode, complement), Constants.CRS, localCode, code);
        }
        final CommonCRS crs;
        switch (codeValue) {
            case Constants.CRS1:  return displayCRS();
            case Constants.CRS84: crs = CommonCRS.WGS84; break;
            case Constants.CRS83: crs = CommonCRS.NAD83; break;
            case Constants.CRS27: crs = CommonCRS.NAD27; break;
            case Constants.CRS88: return CommonCRS.Vertical.NAVD88.crs();
            default: throw noSuchAuthorityCode(localCode, code, null);
        }
        return crs.normalizedGeographic();
    }

    /**
     * Creates a projected CRS from parameters in the {@code AUTO(2)} namespace.
     *
     * @param  code        The user-specified code, used only for error reporting.
     * @param  projection  The projection code (e.g. 42001).
     * @param  isLegacy    {@code true} if the code was found in {@code "AUTO"} or {@code "AUTO1"} namespace.
     * @param  factor      The multiplication factor for the unit of measurement.
     * @param  longitude   A longitude in the desired projection zone.
     * @param  latitude    A latitude in the desired projection zone.
     * @return The projected CRS for the given projection and parameters.
     */
    @SuppressWarnings("null")
    private ProjectedCRS createAuto(final String code, final int projection, final boolean isLegacy,
            final double factor, final double longitude, final double latitude) throws FactoryException
    {
        Boolean isUTM = null;
        String method = null;
        String param  = null;
        switch (projection) {
            /*
             * 42001: Universal Transverse Mercator   —   central meridian must be in the center of a UTM zone.
             * 42002: Transverse Mercator             —   like 42001 except that central meridian can be anywhere.
             * 42003: WGS 84 / Auto Orthographic      —   defined by "Central_Meridian" and "Latitude_of_Origin".
             * 42004: WGS 84 / Auto Equirectangular   —   defined by "Central_Meridian" and "Standard_Parallel_1".
             * 42005: WGS 84 / Auto Mollweide         —   defined by "Central_Meridian" only.
             */
            case 42001: isUTM  = true; break;
            case 42002: isUTM  = (latitude == 0) && (centralMeridian(zone(longitude)) == longitude); break;
            case 42003: method = "Orthographic";       param = Constants.LATITUDE_OF_ORIGIN;         break;
            case 42004: method = "Equirectangular";    param = Constants.STANDARD_PARALLEL_1;        break;
            case 42005: method = "Mollweide";                                                        break;
            default: throw noSuchAuthorityCode(String.valueOf(projection), code, null);
        }
        /*
         * For the (Universal) Transverse Mercator case (AUTO:42001 and 42002), we delegate to the CommonCRS
         * enumeration if possible because CommonCRS will itself delegate to the EPSG factory if possible.
         */
        final CommonCRS datum = CommonCRS.WGS84;
        final GeographicCRS baseCRS;
        final ProjectedCRS crs;
        CartesianCS cs;
        try {
            if (isUTM != null && isUTM) {
                crs = datum.UTM(latitude, longitude);
                if (factor == (isLegacy ? Constants.EPSG_METRE : 1)) {
                    return crs;
                }
                baseCRS = crs.getBaseCRS();
                cs = crs.getCoordinateSystem();
            } else {
                cs = projectedCS;
                if (cs == null) {
                    crs = datum.UTM(latitude, longitude);
                    projectedCS = cs = crs.getCoordinateSystem();
                    baseCRS = crs.getBaseCRS();
                } else {
                    crs = null;
                    baseCRS = datum.geographic();
                }
            }
            /*
             * At this point we got a coordinate system with axes in metres.
             * If the user asked for another unit of measurement, change the axes now.
             */
            final Unit<Length> unit;
            if (isLegacy) {
                unit = createUnitFromEPSG(factor).asType(Length.class);
            } else {
                unit = (factor != 1) ? Units.multiply(SI.METRE, factor) : SI.METRE;
            }
            if (!SI.METRE.equals(unit)) {
                cs = (CartesianCS) CoordinateSystems.replaceLinearUnit(cs, unit);
            }
            /*
             * Set the projection name, operation method and parameters. The parameters for the Transverse Mercator
             * projection are a little bit more tedious to set, so we use a convenience method for that.
             */
            final GeodeticObjectBuilder builder = new GeodeticObjectBuilder();
            if (isUTM != null) {
                if (isUTM) {
                    builder.addName(crs.getName());
                } // else default to the conversion name, which is "Transverse Mercator".
                builder.setTransverseMercator(isUTM, latitude, longitude);
            } else {
                builder.setConversionMethod(method)
                       .addName(PROJECTION_NAMES[projection - FIRST_PROJECTION_CODE])
                       .setParameter(Constants.CENTRAL_MERIDIAN, longitude, NonSI.DEGREE_ANGLE);
                if (param != null) {
                    builder.setParameter(param, latitude, NonSI.DEGREE_ANGLE);
                }
            }
            return builder.createProjectedCRS(baseCRS, cs);
        } catch (IllegalArgumentException e) {
            throw noSuchAuthorityCode(String.valueOf(projection), code, e);
        }
    }

    /**
     * Returns the unit of measurement for the given EPSG code.
     * This is used only for codes in the legacy {@code "AUTO"} namespace.
     */
    private static Unit<?> createUnitFromEPSG(final double code) throws NoSuchAuthorityCodeException {
        String message = null;      // Error message to be used only in case of failure.
        final String s;             // The string representation of the code, to be used only in case of failure.
        final int c = (int) code;
        if (c == code) {
            final Unit<?> unit = Units.valueOfEPSG(c);
            if (Units.isLinear(unit)) {
                return unit;
            } else if (unit != null) {
                message = Errors.format(Errors.Keys.NonLinearUnit_1, unit);
            }
            s = String.valueOf(c);
        } else {
            s = String.valueOf(code);
        }
        if (message == null) {
            message = Errors.format(Errors.Keys.NoSuchAuthorityCode_3, Constants.EPSG, Unit.class, s);
        }
        throw new NoSuchAuthorityCodeException(message, Constants.EPSG, s);
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

            final Map<String,Object> properties = new HashMap<String,Object>(4);
            properties.put(EngineeringDatum.NAME_KEY, cs.getName());
            properties.put(EngineeringDatum.ANCHOR_POINT_KEY, "Origin is in upper left.");
            displayCRS = DefaultFactories.forBuildin(CRSFactory.class).createEngineeringCRS(properties,
                         DefaultFactories.forBuildin(DatumFactory.class).createEngineeringDatum(properties), cs);
        }
        return displayCRS;
    }

    /**
     * Creates an exception for an unknown authority code.
     *
     * @param  localCode  The unknown authority code, without namespace.
     * @param  code       The unknown authority code as specified by the user (may include namespace).
     * @param  cause      The failure cause, or {@code null} if none.
     * @return An exception initialized with an error message built from the specified informations.
     */
    private static NoSuchAuthorityCodeException noSuchAuthorityCode(String localCode, String code, Exception cause) {
        return (NoSuchAuthorityCodeException) new NoSuchAuthorityCodeException(Errors.format(Errors.Keys.NoSuchAuthorityCode_3,
                Constants.OGC, CoordinateReferenceSystem.class, localCode),
                Constants.OGC, localCode, code).initCause(cause);
    }
}
