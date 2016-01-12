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
import javax.measure.unit.NonSI;
import org.opengis.util.NameFactory;
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
import org.opengis.util.GenericName;
import org.opengis.util.ScopedName;
import org.apache.sis.internal.referencing.GeodeticObjectBuilder;
import org.apache.sis.internal.referencing.provider.TransverseMercator;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.internal.simple.SimpleIdentifier;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.iso.DefaultNameSpace;
import org.apache.sis.util.iso.SimpleInternationalString;


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
 * {@code "AUTO2:42003,1,-100,45"}.</div>
 *
 * Codes in the {@code "AUTO"} namespace are the same than codes in the {@code "AUTO2"} namespace,
 * but with the unit factor omitted.
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
     * The {@value} prefix for a code identified by parameters.
     * This is defined in annexes B.7 to B.11 of WMS 1.3 specification.
     * The {@code "AUTO(2)"} namespaces are not considered by SIS as real authorities.
     */
    private static final String AUTO2 = "AUTO2";

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
    private static final char SEPARATOR = ',';

    /**
     * The codes known to this factory, associated with their CRS type. This is set to an empty map
     * at {@code CommonAuthorityFactory} construction time and filled only when first needed.
     */
    private final Map<String,Class<?>> codes;

    /**
     * The authority for this factory, created when first needed.
     */
    private Citation authority;

    /**
     * The "Computer display" reference system (CRS:1). Created when first needed.
     *
     * @see #displayCRS()
     */
    private CoordinateReferenceSystem displayCRS;

    /**
     * Constructs a default factory for the {@code CRS} authority.
     *
     * @param nameFactory The factory to use for {@linkplain NameFactory#parseGenericName parsing} authority codes.
     */
    public CommonAuthorityFactory(final NameFactory nameFactory) {
        super(nameFactory);
        codes = new LinkedHashMap<>();
    }

    /**
     * Returns the organization responsible for definition of the CRS codes recognized by this factory.
     * The authority for this factory is the <cite>Open Geospatial Consortium</cite>.
     *
     * @return The OGC authority.
     */
    @Override
    public synchronized Citation getAuthority() {
        if (authority == null) {
            final DefaultCitation c = new DefaultCitation(Citations.OGC);
            c.setIdentifiers(Arrays.asList(
                    new SimpleIdentifier(null, Constants.OGC, false),
                    new SimpleIdentifier(null, Constants.CRS, false)
            ));
            c.freeze();
            authority = c;
        }
        return authority;
    }

    /**
     * Returns {@code true} if the given string if one of the namespaces recognized by this factory.
     */
    private static boolean isAuthority(final String namespace) {
        if (!namespace.equalsIgnoreCase(Constants.CRS) && !namespace.equalsIgnoreCase(Constants.OGC)) {
            final int s = AUTO2.length() - 1;
            if (!namespace.regionMatches(true, 0, AUTO2, 0, s)) {
                return false;
            }
            switch (namespace.length() - s) {
                case 0:  break;                                 // Namespace is exactly "AUTO" (ignoring case).
                case 1:  final char c = namespace.charAt(s);    // Namespace has one more character than "AUTO".
                         return (c >= '1' && c <= '2');         // Namespace has more than one more character.
                default: return false;
            }
        }
        return true;
    }

    /**
     * If the given code begins with {@code "OGC"}, {@code "CRS"}, {@code "AUTO"}, {@code "AUTO1"} or {@code "AUTO2"}
     * authority (ignoring spaces), returns the code without the authority part. Otherwise if the code starts with any
     * other authority, throw an exception. Otherwise if the code has no authority, returns the code as-is.
     */
    private String trimAuthority(String code) throws NoSuchAuthorityCodeException {
        final GenericName name = nameFactory.parseGenericName(null, code);
        if (name instanceof ScopedName) {
            final GenericName scope = ((ScopedName) name).path();
            if (!isAuthority(CharSequences.trimWhitespaces(scope.toString()))) {
                throw new NoSuchAuthorityCodeException(Errors.format(Errors.Keys.UnknownAuthority_1, scope), Constants.OGC, code);
            }
            code = name.tip().toString();
        }
        /*
         * Above code removed the "CRS" part when it is used as a namespace, as in "CRS:84".
         * The code below removes the "CRS" prefix when it is concatenated within the code,
         * as in "CRS84". Together, those two checks handle redundant codes like "CRS:CRS84"
         * (malformed code, but seen in practice).
         */
        int start = CharSequences.skipLeadingWhitespaces(code, 0, code.length());
        if (code.regionMatches(true, start, Constants.CRS, 0, Constants.CRS.length())) {
            start = CharSequences.skipLeadingWhitespaces(code, start + Constants.CRS.length(), code.length());
        }
        code = code.substring(start, CharSequences.skipTrailingWhitespaces(code, start, code.length()));
        if (code.isEmpty()) {
            throw new NoSuchAuthorityCodeException(Errors.format(Errors.Keys.EmptyArgument_1, "code"), Constants.OGC, code);
        }
        return code;
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
        final boolean all = type.isAssignableFrom(SingleCRS.class);
        if (!all && !SingleCRS.class.isAssignableFrom(type)) {
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
        return all ? Collections.unmodifiableSet(codes.keySet()) : new FilteredCodes(codes, type).keySet();
    }

    /**
     * Adds an element in the {@link #codes} map, witch check against duplicated values.
     */
    private void add(final int code, final Class<? extends SingleCRS> type) throws FactoryException {
        final String namespace = (code >= FIRST_PROJECTION_CODE) ? AUTO2 : Constants.CRS;
        if (codes.put(namespace + DefaultNameSpace.DEFAULT_SEPARATOR + code, type) != null) {
            throw new FactoryException();    // Should never happen, but we are paranoiac.
        }
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
        final String localCode = trimAuthority(code);
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
                throw noSuchAuthorityCode(false, localCode, code, exception);
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
        String localCode  = trimAuthority(code);
        String complement = null;
        final int startOfParameters = localCode.indexOf(SEPARATOR);
        if (startOfParameters >= 0) {
            localCode = code.substring(0, CharSequences.skipTrailingWhitespaces(code, 0, startOfParameters));
            complement = code.substring(startOfParameters + 1);
        }
        int codeValue = 0;
        double[] parameters = ArraysExt.EMPTY_DOUBLE;
        try {
            codeValue = Integer.parseInt(localCode);
            if (complement != null) {
                parameters = CharSequences.parseDoubles(complement, SEPARATOR);
            }
        } catch (NumberFormatException exception) {
            throw noSuchAuthorityCode(codeValue >= FIRST_PROJECTION_CODE, localCode, code, exception);
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
                return createAuto(code, codeValue, (count > 2) ? parameters[0] : 1,
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
            default: throw noSuchAuthorityCode(false, localCode, code, null);
        }
        return crs.normalizedGeographic();
    }

    /**
     * Creates a projected CRS from parameters in the {@code AUTO(2)} namespace.
     *
     * @param  code        The user-specified code, used only for error reporting.
     * @param  projection  The projection code (e.g. 42001).
     * @param  factor      The multiplication factor for the unit of measurement.
     * @param  longitude   A longitude in the desired projection zone.
     * @param  latitude    A latitude in the desired projection zone.
     * @return The projected CRS for the given projection and parameters.
     */
    @SuppressWarnings("fallthrough")
    private static ProjectedCRS createAuto(final String code, final int projection,
            final double factor, final double longitude, final double latitude) throws FactoryException
    {
        IllegalArgumentException failure = null;
        try {
            boolean isUTM = false;
            switch (projection) {
                /*
                 * 42001: Universal Transverse Mercator   —   central meridian must be in the center of a UTM zone.
                 * 42002: Transverse Mercator             —   like 42001 except that central meridian can be anywhere.
                 */
                case 42001: isUTM = true;   // Fall through
                case 42002: {
                    ProjectedCRS crs = CommonCRS.WGS84.UTM(latitude, longitude);
                    if (factor != 1 || (!isUTM && TransverseMercator.centralMeridian(TransverseMercator.zone(longitude)) != longitude)) {
                        CartesianCS cs = crs.getCoordinateSystem();       // In metres
                        if (factor != 1) {
                            // TODO
                        }
                        crs = new GeodeticObjectBuilder()
                                .setTransverseMercator(longitude, isUTM, MathFunctions.isNegative(latitude))
                                .createProjectedCRS(crs.getBaseCRS(), cs);
                    }
                    return crs;
                }
            }
        } catch (IllegalArgumentException e) {
            failure = e;
        }
        throw noSuchAuthorityCode(true, String.valueOf(projection), code, failure);
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

    /**
     * Creates an exception for an unknown authority code.
     *
     * @param  localCode  The unknown authority code, without namespace.
     * @param  code       The unknown authority code as specified by the user (may include namespace).
     * @param  cause      The failure cause, or {@code null} if none.
     * @return An exception initialized with an error message built from the specified informations.
     */
    private static NoSuchAuthorityCodeException noSuchAuthorityCode(final boolean isAuto,
            final String localCode, final String code, final Exception cause)
    {
        NoSuchAuthorityCodeException e = new NoSuchAuthorityCodeException(Errors.format(Errors.Keys.NoSuchAuthorityCode_3,
                Constants.OGC, isAuto ? ProjectedCRS.class : CoordinateReferenceSystem.class, localCode),
                Constants.OGC, localCode, code);
        e.initCause(cause);
        return e;
    }
}
