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
package org.apache.sis.io.wkt;

import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Date;
import java.text.ParseException;
import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import javax.measure.quantity.Quantity;
import javax.measure.quantity.Duration;

import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.util.FactoryException;

// While start import is usually a deprecated practice, we use such a large amount
// of interfaces in those packages that it we choose to exceptionnaly use * here.
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;

import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.referencing.datum.BursaWolfParameters;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.internal.referencing.Legacy;
import org.apache.sis.internal.referencing.VerticalDatumTypes;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.ArgumentChecks;

import static java.util.Collections.singletonMap;
import static org.apache.sis.referencing.datum.DefaultGeodeticDatum.BURSA_WOLF_KEY;


/**
 * Well Known Text (WKT) parser for referencing objects. This include, but is not limited too,
 * {@linkplain org.apache.sis.referencing.crs.AbstractCRS Coordinate Reference System} and
 * {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform Math Transform} objects.
 * Note that math transforms are part of the WKT 1 {@code "FITTED_CS"} element.
 *
 * <div class="section">Default axis names</div>
 * The WKT 1 specification defined axis names different than the ISO 19111 ones.
 * This parser replaces the WKT 1 names by the ISO names and abbreviations when possible.
 *
 * <table class="sis">
 *   <tr><th>CRS type</th>   <th>WKT1 names</th> <th>ISO abbreviations</th></tr>
 *   <tr><td>Geographic</td> <td>Lon, Lat</td>   <td>λ, φ</td></tr>
 *   <tr><td>Vertical</td>   <td>H</td>          <td>h</td></tr>
 *   <tr><td>Projected</td>  <td>X, Y</td>       <td>x, y</td></tr>
 *   <tr><td>Geocentric</td> <td>X, Y, Z</td>    <td>X, Y, Z</td></tr>
 * </table>
 *
 * The default behavior is to use the ISO identifiers.
 * This behavior can be changed by setting the parsing conventions to {@link Convention#WKT1}.
 *
 * @author  Rémi Eve (IRD)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
final class GeodeticObjectParser extends MathTransformParser {
    /**
     * The factory to use for creating {@link Datum} instances.
     */
    private final DatumFactory datumFactory;

    /**
     * The factory to use for creating {@link CoordinateSystem} instances.
     */
    private final CSFactory csFactory;

    /**
     * The factory to use for creating {@link CoordinateReferenceSystem} instances.
     */
    private final CRSFactory crsFactory;

    /**
     * The factory to use for creating defining conversions.
     */
    private final CoordinateOperationFactory opFactory;

    /**
     * The WKT convention to assume for parsing.
     *
     * <ul>
     *   <li>{@link Convention#WKT1_COMMON_UNITS} means that {@code PRIMEM} and {@code PARAMETER} angular units
     *       need to be forced to {@code NonSI.DEGREE_ANGLE} instead than inferred from the context.
     *       Note that this rule does not apply to {@code AXIS} elements.</li>
     * </ul>
     */
    Convention convention;

    /**
     * {@code true} if {@code AXIS[...]} elements should be ignored.
     * This is sometime used for simulating a "force longitude first axis order" behavior.
     * This is also used for compatibility with softwares that ignore axis elements.
     *
     * <p>Note that {@code AXIS} elements still need to be well formed even when this flag is set to {@code true}.
     * Malformed axis elements will continue to cause a {@link ParseException} despite their content being ignored.</p>
     */
    boolean isAxisIgnored;

    /**
     * The list of {@linkplain AxisDirection axis directions} from their name.
     * Instantiated at construction time and never modified after that point.
     */
    private final Map<String,AxisDirection> directions;

    /**
     * The value of the last {@linkplain #directions} map created.
     * We keep this reference only on the assumption that the same map will often be reused.
     */
    private static Map<String,AxisDirection> lastDirections;

    /**
     * Creates a parser using the default set of symbols and factories.
     */
    public GeodeticObjectParser() {
        this(Symbols.getDefault(),
             DefaultFactories.forBuildin(DatumFactory.class),
             DefaultFactories.forBuildin(CSFactory.class),
             DefaultFactories.forBuildin(CRSFactory.class),
             new DefaultCoordinateOperationFactory(), // TODO
             DefaultFactories.forBuildin(MathTransformFactory.class),
             null);
    }

    /**
     * Constructs a parser for the specified set of symbols using the specified set of factories.
     *
     * @param symbols      The symbols for parsing and formatting numbers.
     * @param datumFactory The factory to use for creating {@linkplain Datum datum}.
     * @param csFactory    The factory to use for creating {@linkplain CoordinateSystem coordinate systems}.
     * @param crsFactory   The factory to use for creating {@linkplain CoordinateReferenceSystem coordinate reference systems}.
     * @param mtFactory    The factory to use for creating {@linkplain MathTransform math transform} objects.
     * @param errorLocale  The locale for error messages (not for parsing), or {@code null} for the system default.
     */
    public GeodeticObjectParser(final Symbols symbols,
                                final DatumFactory datumFactory,
                                final CSFactory csFactory,
                                final CRSFactory crsFactory,
                                final CoordinateOperationFactory opFactory,
                                final MathTransformFactory mtFactory,
                                final Locale errorLocale)
    {
        super(symbols, mtFactory, errorLocale);
        ArgumentChecks.ensureNonNull("datumFactory", datumFactory);
        ArgumentChecks.ensureNonNull("csFactory",    csFactory);
        ArgumentChecks.ensureNonNull("crsFactory",   crsFactory);
        this.datumFactory = datumFactory;
        this.csFactory    = csFactory;
        this.crsFactory   = crsFactory;
        this.opFactory    = opFactory;
        /*
         * Gets the map of axis directions.
         */
        final AxisDirection[] values = AxisDirection.values();
        Map<String,AxisDirection> directions = new HashMap<>(Containers.hashMapCapacity(values.length));
        final Locale locale = symbols.getLocale();
        for (final AxisDirection value : values) {
            directions.put(value.name().trim().toUpperCase(locale), value);
        }
        /*
         * Replace by the last generated map if it is the same.
         */
        synchronized (GeodeticObjectParser.class) {
            final Map<String,AxisDirection> existing = lastDirections;
            if (directions.equals(existing)) {
                directions = existing;
            } else {
                lastDirections = directions;
            }
        }
        this.directions = directions;
    }

    /**
     * Parses the next element in the specified <cite>Well Know Text</cite> (WKT) tree.
     *
     * @param  element The element to be parsed.
     * @return The object.
     * @throws ParseException if the element can not be parsed.
     */
    @Override
    Object parseObject(final Element element) throws ParseException {
        Object value = parseCoordinateReferenceSystem(element, false);
        if (value != null) {
            return value;
        }
        value = parseMathTransform(element, false);
        if (value != null) {
            return value;
        }
        String keyword = WKTKeywords.GeogCS;
        final Object key = element.peek();
        if (key instanceof Element) {
            keyword = ((Element) key).keyword;
            if (keyword != null) {
                if (keyword.equalsIgnoreCase(WKTKeywords.Axis))        return parseAxis      (element, SI.METRE, true);
                if (keyword.equalsIgnoreCase(WKTKeywords.PrimeM))      return parsePrimem    (element, NonSI.DEGREE_ANGLE);
                if (keyword.equalsIgnoreCase(WKTKeywords.ToWGS84))     return parseToWGS84   (element);
                if (keyword.equalsIgnoreCase(WKTKeywords.Spheroid))    return parseSpheroid  (element);
                if (keyword.equalsIgnoreCase(WKTKeywords.Vert_Datum))  return parseVertDatum (element);
                if (keyword.equalsIgnoreCase(WKTKeywords.Local_Datum)) return parseLocalDatum(element);
                if (keyword.equalsIgnoreCase(WKTKeywords.Datum))       return parseDatum     (element, CommonCRS.WGS84.primeMeridian());
            }
        }
        throw element.keywordNotFound(keyword, keyword == WKTKeywords.GeogCS);
    }

    /**
     * Parses a coordinate reference system element.
     *
     * @param  element The parent element.
     * @param  mandatory {@code true} if a CRS must be present, or {@code false} if optional.
     * @return The next element as a {@code CoordinateReferenceSystem} object.
     * @throws ParseException if the next element can not be parsed.
     */
    private CoordinateReferenceSystem parseCoordinateReferenceSystem(final Element element, final boolean mandatory)
            throws ParseException
    {
        String keyword = WKTKeywords.GeogCS;
        final Object key = element.peek();
        if (key instanceof Element) {
            keyword = ((Element) key).keyword;
            if (keyword != null) {
                if (keyword.equalsIgnoreCase(WKTKeywords.GeogCS))    return parseGeoGCS  (element);
                if (keyword.equalsIgnoreCase(WKTKeywords.ProjCS))    return parseProjCS  (element);
                if (keyword.equalsIgnoreCase(WKTKeywords.GeocCS))    return parseGeoCCS  (element);
                if (keyword.equalsIgnoreCase(WKTKeywords.Vert_CS))   return parseVertCS  (element);
                if (keyword.equalsIgnoreCase(WKTKeywords.TimeCRS))   return parseTimeCRS (element);
                if (keyword.equalsIgnoreCase(WKTKeywords.Local_CS))  return parseLocalCS (element);
                if (keyword.equalsIgnoreCase(WKTKeywords.Compd_CS))  return parseCompdCS (element);
                if (keyword.equalsIgnoreCase(WKTKeywords.Fitted_CS)) return parseFittedCS(element);
            }
        }
        if (mandatory) {
            throw element.keywordNotFound(keyword, keyword == WKTKeywords.GeogCS);
        }
        return null;
    }

    /**
     * Parses an <strong>optional</strong> {@code "AUTHORITY"} element.
     * This element has the following pattern:
     *
     * {@preformat text
     *     AUTHORITY["<name>", "<code>"]
     * }
     *
     * @param  parent The parent element.
     * @param  name The name of the parent object being parsed, either a {@link String} or {@link Identifier} instance.
     * @return A properties map with the parent name and the optional authority code.
     * @throws ParseException if the {@code "AUTHORITY"} can not be parsed.
     */
    private Map<String,Object> parseAuthority(final Element parent, Object name) throws ParseException {
        assert (name instanceof String) || (name instanceof Identifier);
        final boolean isRoot  = parent.isRoot();
        final Element element = parent.pullOptionalElement(WKTKeywords.Authority);
        if (element == null && !isRoot) {
            return singletonMap(IdentifiedObject.NAME_KEY, name);
        }
        Map<String,Object> properties = new HashMap<>(4);
        properties.put(IdentifiedObject.NAME_KEY, name);
        if (element != null) {
            final String auth = element.pullString("name");
            final String code = element.pullObject("code").toString();  // Accepts Integer as well as String.
            element.close(ignoredElements);
            final Citation authority = Citations.fromName(auth);
            properties.put(IdentifiedObject.IDENTIFIERS_KEY, new NamedIdentifier(authority, code));
            /*
             * Note: we could be tempted to assign the authority to the name as well, like below:
             *
             *     if (name instanceof String) {
             *         name = new NamedIdentifier(authority, (String) name);
             *     }
             *     properties.put(IdentifiedObject.NAME_KEY, name);
             *
             * However experience shows that it is often wrong in practice, because peoples often
             * declare EPSG codes but still use WKT names much shorter than the EPSG names
             * (for example "WGS84" instead than "World Geodetic System 1984").
             */
        }
        return properties;
    }

    /**
     * Parses a {@code "UNIT"} element.
     * This element has the following pattern:
     *
     * {@preformat text
     *     UNIT["<name>", <conversion factor> {,<authority>}]
     * }
     *
     * @param  parent The parent element.
     * @param  unit The contextual unit, usually {@code SI.METRE} or {@code SI.RADIAN}.
     * @return The {@code "UNIT"} element as an {@link Unit} object.
     * @throws ParseException if the {@code "UNIT"} can not be parsed.
     *
     * @todo Authority code is currently ignored. We may consider to create a subclass of
     *       {@link Unit} which implements {@link IdentifiedObject} in a future version.
     */
    private <Q extends Quantity> Unit<Q> parseUnit(final Element parent, final Unit<Q> unit)
            throws ParseException
    {
        final Element element = parent.pullElement("Unit");
        final String     name = element.pullString("name");
        final double   factor = element.pullDouble("factor");
        final Map<String,?> properties = parseAuthority(element, name); // Ignored for now.
        element.close(ignoredElements);
        return Units.multiply(unit, factor);
    }

    /**
     * Parses an {@code "AXIS"} element.
     * This element has the following pattern:
     *
     * {@preformat text
     *     AXIS["<name>", NORTH | SOUTH | EAST | WEST | UP | DOWN | OTHER]
     * }
     *
     * <div class="note"><b>Note:</b>
     * There is no AUTHORITY element for AXIS element in WKT 1 specification. However, we accept it anyway in order
     * to make the parser more tolerant to non-100% compliant WKT. Note that AXIS is really the only element without
     * such AUTHORITY clause and the EPSG database provides authority code for all axis.</div>
     *
     * @param  parent The parent element.
     * @param  unit The contextual unit, usually {@code SI.METRE} or {@code SI.RADIAN}.
     * @param  mandatory {@code true} if the axis is mandatory, or {@code false} if it is optional.
     * @return The {@code "AXIS"} element as a {@link CoordinateSystemAxis} object, or {@code null}
     *         if the axis was not required and there is no axis object.
     * @throws ParseException if the {@code "AXIS"} element can not be parsed.
     */
    private CoordinateSystemAxis parseAxis(final Element parent, final Unit<?> unit, final boolean mandatory)
            throws ParseException
    {
        final Element element;
        if (mandatory) {
            element = parent.pullElement(WKTKeywords.Axis);
        } else {
            element = parent.pullOptionalElement(WKTKeywords.Axis);
            if (element == null) {
                return null;
            }
        }
        final String name = element.pullString("name");
        final Element orientation = element.pullVoidElement("orientation");
        final Map<String,?> properties = parseAuthority(element, name);
        element.close(ignoredElements);
        final AxisDirection direction = directions.get(orientation.keyword);
        if (direction == null) {
            throw element.keywordNotFound("orientation", true);
        }
        try {
            return csFactory.createCoordinateSystemAxis(properties, name, direction, unit);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Creates an axis with the same name than the abbreviation.
     */
    private CoordinateSystemAxis createAxis(final String abbreviation,
            final AxisDirection direction, final Unit<?> unit) throws FactoryException
    {
        return csFactory.createCoordinateSystemAxis(singletonMap(IdentifiedObject.NAME_KEY, abbreviation),
                abbreviation, direction, unit);
    }

    /**
     * Parses a {@code "PRIMEM"} element. This element has the following pattern:
     *
     * {@preformat text
     *     PRIMEM["<name>", <longitude> {,<authority>}]
     * }
     *
     * @param  parent The parent element.
     * @param  angularUnit The contextual unit.
     * @return The {@code "PRIMEM"} element as a {@link PrimeMeridian} object.
     * @throws ParseException if the {@code "PRIMEM"} element can not be parsed.
     */
    private PrimeMeridian parsePrimem(final Element parent, Unit<Angle> angularUnit) throws ParseException {
        if (convention == Convention.WKT1_COMMON_UNITS) {
            angularUnit = NonSI.DEGREE_ANGLE;
        }
        final Element element   = parent.pullElement(WKTKeywords.PrimeM);
        final String  name      = element.pullString("name");
        final double  longitude = element.pullDouble("longitude");
        final Map<String,?> properties = parseAuthority(element, name);
        element.close(ignoredElements);
        try {
            return datumFactory.createPrimeMeridian(properties, longitude, angularUnit);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses an <strong>optional</strong> {@code "TOWGS84"} element.
     * This element has the following pattern:
     *
     * {@preformat text
     *     TOWGS84[<dx>, <dy>, <dz>, <ex>, <ey>, <ez>, <ppm>]
     * }
     *
     * @param  parent The parent element.
     * @return The {@code "TOWGS84"} element as a {@link BursaWolfParameters} object,
     *         or {@code null} if no {@code "TOWGS84"} has been found.
     * @throws ParseException if the {@code "TOWGS84"} can not be parsed.
     */
    private BursaWolfParameters parseToWGS84(final Element parent) throws ParseException {
        final Element element = parent.pullOptionalElement(WKTKeywords.ToWGS84);
        if (element == null) {
            return null;
        }
        final BursaWolfParameters info = new BursaWolfParameters(CommonCRS.WGS84.datum(), null);
        info.tX = element.pullDouble("dx");
        info.tY = element.pullDouble("dy");
        info.tZ = element.pullDouble("dz");
        if (element.peek() != null) {
            info.rX = element.pullDouble("ex");
            info.rY = element.pullDouble("ey");
            info.rZ = element.pullDouble("ez");
            info.dS = element.pullDouble("ppm");
        }
        element.close(ignoredElements);
        return info;
    }

    /**
     * Parses a {@code "SPHEROID"} element. This element has the following pattern:
     *
     * {@preformat text
     *     SPHEROID["<name>", <semi-major axis>, <inverse flattening> {,<authority>}]
     * }
     *
     * @param  parent The parent element.
     * @return The {@code "SPHEROID"} element as an {@link Ellipsoid} object.
     * @throws ParseException if the {@code "SPHEROID"} element can not be parsed.
     */
    private Ellipsoid parseSpheroid(final Element parent) throws ParseException {
        Element element           = parent.pullElement(WKTKeywords.Spheroid);
        String  name              = element.pullString("name");
        double  semiMajorAxis     = element.pullDouble("semiMajorAxis");
        double  inverseFlattening = element.pullDouble("inverseFlattening");
        Map<String,?> properties = parseAuthority(element, name);
        element.close(ignoredElements);
        if (inverseFlattening == 0) {   // OGC convention for a sphere.
            inverseFlattening = Double.POSITIVE_INFINITY;
        }
        try {
            return datumFactory.createFlattenedSphere(properties, semiMajorAxis, inverseFlattening, SI.METRE);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses a {@code "PROJECTION"} element. This element has the following pattern:
     *
     * {@preformat text
     *     PROJECTION["<name>" {,<authority>}]
     * }
     *
     * @param  parent      The parent element.
     * @param  linearUnit  The linear unit of the parent {@code PROJCS} element, or {@code null}.
     * @param  angularUnit The angular unit of the parent {@code GEOCS} element, or {@code null}.
     * @return The {@code "PROJECTION"} element as a defining conversion.
     * @throws ParseException if the {@code "PROJECTION"} element can not be parsed.
     */
    private Conversion parseProjection(final Element      parent,
                                       final Unit<Length> linearUnit,
                                       final Unit<Angle>  angularUnit)
            throws ParseException
    {
        final Element element = parent.pullElement(WKTKeywords.Projection);
        final String classification = element.pullString("name");
        final Map<String,?> properties = parseAuthority(element, classification);
        element.close(ignoredElements);
        /*
         * Set the list of parameters. NOTE: Parameters are defined in the parent
         * Element (usually a "PROJCS" element), not in this "PROJECTION" element.
         */
        try {
            final OperationMethod method = opFactory.getOperationMethod(classification);
            final ParameterValueGroup parameters = method.getParameters().createValue();
            parseParameters(parent, parameters, linearUnit, angularUnit);
            return opFactory.createDefiningConversion(properties, method, parameters);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses a {@code "DATUM"} element. This element has the following pattern:
     *
     * {@preformat text
     *     DATUM["<name>", <spheroid> {,<to wgs84>} {,<authority>}]
     * }
     *
     * @param  parent The parent element.
     * @param  meridian the prime meridian.
     * @return The {@code "DATUM"} element as a {@link GeodeticDatum} object.
     * @throws ParseException if the {@code "DATUM"} element can not be parsed.
     */
    private GeodeticDatum parseDatum(final Element parent, final PrimeMeridian meridian) throws ParseException {
        Element             element    = parent.pullElement(WKTKeywords.Datum);
        String              name       = element.pullString("name");
        Ellipsoid           ellipsoid  = parseSpheroid(element);
        BursaWolfParameters toWGS84    = parseToWGS84(element);     // Optional; may be null.
        Map<String,Object>  properties = parseAuthority(element, name);
        element.close(ignoredElements);
        if (toWGS84 != null) {
            if (!(properties instanceof HashMap<?,?>)) {
                properties = new HashMap<>(properties);
            }
            properties.put(BURSA_WOLF_KEY, toWGS84);
        }
        try {
            return datumFactory.createGeodeticDatum(properties, ellipsoid, meridian);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses a {@code "VERT_DATUM"} element. This element has the following pattern:
     *
     * {@preformat text
     *     VERT_DATUM["<name>", <datum type> {,<authority>}]
     * }
     *
     * @param  parent The parent element.
     * @return The {@code "VERT_DATUM"} element as a {@link VerticalDatum} object.
     * @throws ParseException if the {@code "VERT_DATUM"} element can not be parsed.
     */
    private VerticalDatum parseVertDatum(final Element parent) throws ParseException {
        final Element element = parent.pullElement(WKTKeywords.Vert_Datum);
        final String  name    = element.pullString ("name");
        final int     datum   = element.pullInteger("datum");
        final Map<String,?> properties = parseAuthority(element, name);
        element.close(ignoredElements);
        final VerticalDatumType type = VerticalDatumTypes.fromLegacy(datum);
        // TODO: need to find a default value if null.
        try {
            return datumFactory.createVerticalDatum(properties, type);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses a {@code "TimeDatum"} element. This element has the following pattern:
     *
     * {@preformat text
     *     TimeDatum["<name>", TimeOrigin[<time origin>] {,<authority>}]
     * }
     *
     * @param  parent The parent element.
     * @return The {@code "TimeDatum"} element as a {@link TemporalDatum} object.
     * @throws ParseException if the {@code "TimeDatum"} element can not be parsed.
     */
    private TemporalDatum parseTimeDatum(final Element parent) throws ParseException {
        final Element element = parent.pullElement(WKTKeywords.TimeDatum);
        final String  name    = element.pullString ("name");
        final Element origin  = element.pullElement(WKTKeywords.TimeOrigin);
        final Date    epoch   = origin .pullDate("origin");
        origin.close(ignoredElements);
        final Map<String,?> properties = parseAuthority(element, name);
        element.close(ignoredElements);
        try {
            return datumFactory.createTemporalDatum(properties, epoch);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses a {@code "LOCAL_DATUM"} element. This element has the following pattern:
     *
     * {@preformat text
     *     LOCAL_DATUM["<name>", <datum type> {,<authority>}]
     * }
     *
     * @param  parent The parent element.
     * @return The {@code "LOCAL_DATUM"} element as an {@link EngineeringDatum} object.
     * @throws ParseException if the {@code "LOCAL_DATUM"} element can not be parsed.
     *
     * @todo The vertical datum type is currently ignored.
     */
    private EngineeringDatum parseLocalDatum(final Element parent) throws ParseException {
        final Element element = parent.pullElement(WKTKeywords.Local_Datum);
        final String  name    = element.pullString ("name");
        final int     datum   = element.pullInteger("datum");   // Ignored for now.
        final Map<String,?> properties = parseAuthority(element, name);
        element.close(ignoredElements);
        try {
            return datumFactory.createEngineeringDatum(properties);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses a {@code "LOCAL_CS"} element.
     * This element has the following pattern:
     *
     * {@preformat text
     *     LOCAL_CS["<name>", <local datum>, <unit>, <axis>, {,<axis>}* {,<authority>}]
     * }
     *
     * @param  parent The parent element.
     * @return The {@code "LOCAL_CS"} element as an {@link EngineeringCRS} object.
     * @throws ParseException if the {@code "LOCAL_CS"} element can not be parsed.
     *
     * @todo The coordinate system used is always a SIS implementation, since we don't
     *       know which method to invokes in the {@link CSFactory} (is it a Cartesian
     *       coordinate system? a spherical one? etc.).
     */
    private EngineeringCRS parseLocalCS(final Element parent) throws ParseException {
        Element           element = parent.pullElement(WKTKeywords.Local_CS);
        String               name = element.pullString("name");
        EngineeringDatum    datum = parseLocalDatum(element);
        Unit<Length>   linearUnit = parseUnit(element, SI.METRE);
        CoordinateSystemAxis axis = parseAxis(element, linearUnit, true);
        List<CoordinateSystemAxis> list = new ArrayList<>();
        do {
            list.add(axis);
            axis = parseAxis(element, linearUnit, false);
        } while (axis != null);
        final Map<String,?> properties = parseAuthority(element, name);
        element.close(ignoredElements);
        final CoordinateSystem cs;
        cs = new AbstractCS(singletonMap("name", name), list.toArray(new CoordinateSystemAxis[list.size()]));
        try {
            return crsFactory.createEngineeringCRS(properties, datum, cs);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses a {@code "GEOCCS"} element.
     * This element has the following pattern:
     *
     * {@preformat text
     *     GEOCCS["<name>", <datum>, <prime meridian>, <linear unit>
     *            {,<axis> ,<axis> ,<axis>} {,<authority>}]
     * }
     *
     * @param  parent The parent element.
     * @return The {@code "GEOCCS"} element as a {@link GeocentricCRS} object.
     * @throws ParseException if the {@code "GEOCCS"} element can not be parsed.
     */
    private GeocentricCRS parseGeoCCS(final Element parent) throws ParseException {
        final Element       element    = parent.pullElement(WKTKeywords.GeocCS);
        final String        name       = element.pullString("name");
        final Map<String,?> properties = parseAuthority(element, name);
        final PrimeMeridian meridian   = parsePrimem   (element, NonSI.DEGREE_ANGLE);
        final GeodeticDatum datum      = parseDatum    (element, meridian);
        final Unit<Length>  linearUnit = parseUnit     (element, SI.METRE);
        CoordinateSystemAxis axis0, axis1 = null, axis2 = null;
        axis0 = parseAxis(element, linearUnit, false);
        try {
            if (axis0 != null) {
                axis1 = parseAxis(element, linearUnit, true);
                axis2 = parseAxis(element, linearUnit, true);
            }
            if (axis0 == null || isAxisIgnored) {
                // Those default values are part of WKT specification.
                // TODO: use CommonCRS.
                axis0 = createAxis("X", AxisDirection.OTHER, linearUnit);
                axis1 = createAxis("Y", AxisDirection.EAST,  linearUnit);
                axis2 = createAxis("Z", AxisDirection.NORTH, linearUnit);
            }
            element.close(ignoredElements);
            CartesianCS cs = csFactory.createCartesianCS(properties, axis0, axis1, axis2);
            cs = Legacy.forGeocentricCRS(cs, false);
            return crsFactory.createGeocentricCRS(properties, datum, cs);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses an <strong>optional</strong> {@code "VERT_CS"} element.
     * This element has the following pattern:
     *
     * {@preformat text
     *     VERT_CS["<name>", <vert datum>, <linear unit>, {<axis>,} {,<authority>}]
     * }
     *
     * @param  parent The parent element.
     * @return The {@code "VERT_CS"} element as a {@link VerticalCRS} object.
     * @throws ParseException if the {@code "VERT_CS"} element can not be parsed.
     */
    private VerticalCRS parseVertCS(final Element parent) throws ParseException {
        final Element element = parent.pullElement(WKTKeywords.Vert_CS);
        if (element == null) {
            return null;
        }
        String               name       = element.pullString("name");
        VerticalDatum        datum      = parseVertDatum(element);
        Unit<Length>         linearUnit = parseUnit(element, SI.METRE);
        CoordinateSystemAxis axis       = parseAxis(element, linearUnit, false);
        Map<String,?>        properties = parseAuthority(element, name);
        element.close(ignoredElements);
        try {
            if (axis == null || isAxisIgnored) {
                axis = createAxis("h", AxisDirection.UP, linearUnit);
            }
            return crsFactory.createVerticalCRS(properties, datum,
                    csFactory.createVerticalCS(singletonMap("name", name), axis));
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses an <strong>optional</strong> {@code "TIMECRS"} element.
     *
     * @param  parent The parent element.
     * @return The {@code "TIMECRS"} element as a {@link TemporalCRS} object.
     * @throws ParseException if the {@code "TIMECRS"} element can not be parsed.
     */
    private TemporalCRS parseTimeCRS(final Element parent) throws ParseException {
        final Element element = parent.pullElement(WKTKeywords.TimeCRS);
        if (element == null) {
            return null;
        }
        String               name       = element.pullString("name");
        TemporalDatum        datum      = parseTimeDatum(element);
        Unit<Duration>       timeUnit   = parseUnit(element, SI.SECOND);
        CoordinateSystemAxis axis       = parseAxis(element, timeUnit, false);
        Map<String,?>        properties = parseAuthority(element, name);
        element.close(ignoredElements);
        try {
            if (axis == null || isAxisIgnored) {
                axis = createAxis("t", AxisDirection.UP, timeUnit);
            }
            return crsFactory.createTemporalCRS(properties, datum,
                    csFactory.createTimeCS(singletonMap("name", name), axis));
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses a {@code "GEOGCS"} element. This element has the following pattern:
     *
     * {@preformat text
     *     GEOGCS["<name>", <datum>, <prime meridian>, <angular unit>  {,<twin axes>} {,<authority>}]
     * }
     *
     * @param  parent The parent element.
     * @return The {@code "GEOGCS"} element as a {@link GeographicCRS} object.
     * @throws ParseException if the {@code "GEOGCS"} element can not be parsed.
     */
    private GeographicCRS parseGeoGCS(final Element parent) throws ParseException {
        Element       element     = parent.pullElement(WKTKeywords.GeogCS);
        Object        name        = element.pullString("name");
        Unit<Angle>   angularUnit = parseUnit  (element, SI.RADIAN);
        PrimeMeridian meridian    = parsePrimem(element, angularUnit);
        GeodeticDatum datum       = parseDatum (element, meridian);
        if (((String) name).isEmpty()) {
            /*
             * GeographicCRS name is a mandatory property, but some invalid WKT with an empty string exist.
             * In such case, we will use the name of the enclosed datum. Indeed, it is not uncommon to have
             * the same name for a geographic CRS and its geodetic datum.
             */
            name = datum.getName();
        }
        Map<String,?>   properties = parseAuthority(element, name);
        CoordinateSystemAxis axis0 = parseAxis     (element, angularUnit, false);
        CoordinateSystemAxis axis1 = null;
        try {
            if (axis0 != null) {
                axis1 = parseAxis(element, angularUnit, true);
            }
            if (axis0 == null || isAxisIgnored) {
                axis0 = createAxis("λ", AxisDirection.EAST,  angularUnit);
                axis1 = createAxis("φ", AxisDirection.NORTH, angularUnit);
            }
            element.close(ignoredElements);
            return crsFactory.createGeographicCRS(properties, datum,
                    csFactory.createEllipsoidalCS(properties, axis0, axis1));
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses a {@code "PROJCS"} element.
     * This element has the following pattern:
     *
     * {@preformat text
     *     PROJCS["<name>", <geographic cs>, <projection>, {<parameter>,}*,
     *            <linear unit> {,<twin axes>}{,<authority>}]
     * }
     *
     * @param  parent The parent element.
     * @return The {@code "PROJCS"} element as a {@link ProjectedCRS} object.
     * @throws ParseException if the {@code "GEOGCS"} element can not be parsed.
     */
    private ProjectedCRS parseProjCS(final Element parent) throws ParseException {
        Element             element    = parent.pullElement(WKTKeywords.ProjCS);
        String              name       = element.pullString("name");
        Map<String,?>       properties = parseAuthority(element, name);
        GeographicCRS       geoCRS     = parseGeoGCS(element);
        Unit<Length>        linearUnit = parseUnit(element, SI.METRE);
        final Conversion    conversion = parseProjection(element, linearUnit,
                (convention == Convention.WKT1_COMMON_UNITS) ? NonSI.DEGREE_ANGLE :
                geoCRS.getCoordinateSystem().getAxis(0).getUnit().asType(Angle.class));
        CoordinateSystemAxis axis0 = parseAxis(element, linearUnit, false);
        CoordinateSystemAxis axis1 = null;
        try {
            if (axis0 != null) {
                axis1 = parseAxis(element, linearUnit, true);
            }
            if (axis0 == null || isAxisIgnored) {
                axis0 = createAxis("x", AxisDirection.EAST,  linearUnit);
                axis1 = createAxis("y", AxisDirection.NORTH, linearUnit);
            }
            element.close(ignoredElements);
            return crsFactory.createProjectedCRS(properties, geoCRS, conversion,
                    csFactory.createCartesianCS(properties, axis0, axis1));
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses a {@code "COMPD_CS"} element.
     * This element has the following pattern:
     *
     * {@preformat text
     *     COMPD_CS["<name>", <head cs>, <tail cs> {,<authority>}]
     * }
     *
     * @param  parent The parent element.
     * @return The {@code "COMPD_CS"} element as a {@link CompoundCRS} object.
     * @throws ParseException if the {@code "COMPD_CS"} element can not be parsed.
     */
    private CompoundCRS parseCompdCS(final Element parent) throws ParseException {
        final List<CoordinateReferenceSystem> components = new ArrayList<>(4);
        Element       element    = parent.pullElement(WKTKeywords.Compd_CS);
        String        name       = element.pullString("name");
        Map<String,?> properties = parseAuthority(element, name);
        CoordinateReferenceSystem crs;
        while ((crs = parseCoordinateReferenceSystem(element, components.size() < 2)) != null) {
            components.add(crs);
        }
        element.close(ignoredElements);
        try {
            return crsFactory.createCompoundCRS(properties, components.toArray(new CoordinateReferenceSystem[components.size()]));
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses a {@code "FITTED_CS"} element.
     * This element has the following pattern:
     *
     * {@preformat text
     *     FITTED_CS["<name>", <to base>, <base cs>]
     * }
     *
     * @param  parent The parent element.
     * @return The {@code "FITTED_CS"} element as a {@link CompoundCRS} object.
     * @throws ParseException if the {@code "COMPD_CS"} element can not be parsed.
     */
    private DerivedCRS parseFittedCS(final Element parent) throws ParseException {
        Element       element    = parent.pullElement(WKTKeywords.Fitted_CS);
        String        name       = element.pullString("name");
        Map<String,?> properties = parseAuthority(element, name);
        final MathTransform toBase = parseMathTransform(element, true);
        final CoordinateReferenceSystem base = parseCoordinateReferenceSystem(element, true);
        final OperationMethod method = getOperationMethod();
        element.close(ignoredElements);
        /*
         * WKT 1 provides no informations about the underlying CS of a derived CRS.
         * We have to guess some reasonable one with arbitrary units. We try to construct the one which
         * contains as few information as possible, in order to avoid providing wrong informations.
         */
        final CoordinateSystemAxis[] axis = new CoordinateSystemAxis[toBase.getSourceDimensions()];
        final StringBuilder buffer = new StringBuilder(name);
        buffer.append(" axis ");
        final int start = buffer.length();
        try {
            for (int i=0; i<axis.length; i++) {
                final String number = String.valueOf(i);
                buffer.setLength(start);
                buffer.append(number);
                axis[i] = csFactory.createCoordinateSystemAxis(
                        singletonMap(IdentifiedObject.NAME_KEY, buffer.toString()),
                        number, AxisDirection.OTHER, Unit.ONE);
            }
            final Conversion conversion = new DefaultConversion(    // TODO: use opFactory
                    singletonMap(IdentifiedObject.NAME_KEY, method.getName().getCode()),
                    method, toBase.inverse(), null);
            final CoordinateSystem cs = new AbstractCS(properties, axis);
            return crsFactory.createDerivedCRS(properties, base, conversion, cs);
        } catch (FactoryException | NoninvertibleTransformException exception) {
            throw element.parseFailed(exception);
        }
    }
}
