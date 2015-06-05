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
import java.text.ParsePosition;
import java.text.ParseException;
import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import javax.measure.quantity.Quantity;
import javax.measure.quantity.Duration;

import org.opengis.util.Factory;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ObjectFactory;
import org.opengis.util.FactoryException;

// While start import is usually a deprecated practice, we use such a large amount
// of interfaces in those packages that it we choose to exceptionnaly use * here.
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;

import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.internal.metadata.VerticalDatumTypes;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.util.LocalizedParseException;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.iso.Types;

import static java.util.Collections.singletonMap;


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
     * The names of the 7 parameters in a {@code TOWGS84[…]} element.
     * Those names are derived from the <cite>Well Known Text</cite> (WKT) version 1 specification.
     * They are not the same than the {@link org.apache.sis.referencing.datum.BursaWolfParameters}
     * field names, which are derived from the EPSG database.
     */
    private static final String[] ToWGS84 = {"dx", "dy", "dz", "ex", "ey", "ez", "ppm"};

    /**
     * The factory to use for creating {@link CoordinateReferenceSystem} instances.
     */
    private final CRSFactory crsFactory;

    /**
     * The factory to use for creating {@link CoordinateSystem} instances.
     */
    private final CSFactory csFactory;

    /**
     * The factory to use for creating {@link Datum} instances.
     */
    private final DatumFactory datumFactory;

    /**
     * The factory to use for creating defining conversions.
     * Used only for map projections and derived CRS.
     */
    private final CoordinateOperationFactory opFactory;

    /**
     * Other services from "sis-referencing" module which can not be provided by the standard factories.
     */
    private final ReferencingServices referencing;

    /**
     * The WKT convention to assume for parsing.
     *
     * <ul>
     *   <li>{@link Convention#WKT1_COMMON_UNITS} means that {@code PRIMEM} and {@code PARAMETER} angular units
     *       need to be forced to {@code NonSI.DEGREE_ANGLE} instead than inferred from the context.
     *       Note that this rule does not apply to {@code AXIS} elements.</li>
     * </ul>
     */
    private final Convention convention;

    /**
     * {@code true} if {@code AXIS[...]} elements should be ignored.
     * This is sometime used for simulating a "force longitude first axis order" behavior.
     * This is also used for compatibility with softwares that ignore axis elements.
     *
     * <p>Note that {@code AXIS} elements still need to be well formed even when this flag is set to {@code true}.
     * Malformed axis elements will continue to cause a {@link ParseException} despite their content being ignored.</p>
     */
    private final boolean isAxisIgnored;

    /**
     * A map of properties to be given the factory constructor methods.
     * This map will be recycled for each object to be parsed.
     */
    private final Map<String,Object> properties = new HashMap<String,Object>(4);

    /**
     * Creates a parser using the default set of symbols and factories.
     */
    public GeodeticObjectParser() {
        this(Symbols.getDefault(), Convention.DEFAULT, false, null, null);
    }

    /**
     * Constructs a parser for the specified set of symbols using the specified set of factories.
     *
     * This constructor is for internal usage by Apache SIS only — <b>do not use!</b>
     *
     * <p><b>Maintenance note:</b> this constructor is invoked through reflection by
     * {@link org.apache.sis.referencing.factory.GeodeticObjectFactory#createFromWKT(String)}.
     * Do not change the method signature even if it doesn't break the compilation, unless the
     * reflection code is also updated.</p>
     *
     * @param defaultProperties Default properties to give to the object to create.
     * @param factories An object implementing {@link DatumFactory}, {@link CSFactory} and {@link CRSFactory}.
     * @param mtFactory The factory to use to create {@link MathTransform} objects.
     */
    public GeodeticObjectParser(final Map<String,?> defaultProperties,
            final ObjectFactory factories, final MathTransformFactory mtFactory)
    {
        super(Symbols.getDefault(), mtFactory, (Locale) defaultProperties.get(Errors.LOCALE_KEY));
        crsFactory    = (CRSFactory)   factories;
        csFactory     = (CSFactory)    factories;
        datumFactory  = (DatumFactory) factories;
        referencing   = ReferencingServices.getInstance();
        opFactory     = referencing.getCoordinateOperationFactory(defaultProperties, mtFactory);
        convention    = Convention.DEFAULT;
        isAxisIgnored = false;
    }

    /**
     * Constructs a parser for the specified set of symbols using the specified set of factories.
     *
     * @param symbols       The set of symbols to use.
     * @param convention    The WKT convention to use.
     * @param isAxisIgnored {@code true} if {@code AXIS} elements should be ignored.
     * @param errorLocale   The locale for error messages (not for parsing), or {@code null} for the system default.
     * @param factories     On input, the factories to use. On output, the factories used. Can be null.
     */
    GeodeticObjectParser(final Symbols symbols, final Convention convention, final boolean isAxisIgnored,
            final Locale errorLocale, final Map<Class<?>,Factory> factories)
    {
        super(symbols, getFactory(MathTransformFactory.class, factories), errorLocale);
        crsFactory   = getFactory(CRSFactory.class,   factories);
        csFactory    = getFactory(CSFactory.class,    factories);
        datumFactory = getFactory(DatumFactory.class, factories);
        referencing  = ReferencingServices.getInstance();
        opFactory    = referencing.getCoordinateOperationFactory(null, mtFactory);
        this.convention = convention;
        this.isAxisIgnored = isAxisIgnored;
    }

    /**
     * Returns the factory of the given type. This method will fetch the factory from the given
     * map if non-null. The factory actually used is stored in the map, unless the map is null.
     */
    private static <T extends Factory> T getFactory(final Class<T> type, final Map<Class<?>,Factory> factories) {
        if (factories == null) {
            return DefaultFactories.forBuildin(type);
        }
        T factory = type.cast(factories.get(type));
        if (factory == null) {
            factory = DefaultFactories.forBuildin(type);
            factories.put(type, factory);
        }
        return factory;
    }

    /**
     * Parses a <cite>Well Know Text</cite> (WKT).
     *
     * @param  text The text to be parsed.
     * @param  position The position to start parsing from.
     * @return The parsed object.
     * @throws ParseException if the string can not be parsed.
     */
    @Override
    public Object parseObject(final String text, final ParsePosition position) throws ParseException {
        try {
            return super.parseObject(text, position);
        } finally {
            properties.clear();     // for letting the garbage collector do its work.
        }
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
        final Object child = element.peek();
        if (child instanceof Element) {
            keyword = ((Element) child).keyword;
            if (keyword != null) {
                if (keyword.equalsIgnoreCase(WKTKeywords.Axis))        return parseAxis      (element, SI.METRE, true);
                if (keyword.equalsIgnoreCase(WKTKeywords.PrimeM))      return parsePrimem    (element, NonSI.DEGREE_ANGLE);
                if (keyword.equalsIgnoreCase(WKTKeywords.ToWGS84))     return parseToWGS84   (element);
                if (keyword.equalsIgnoreCase(WKTKeywords.Spheroid))    return parseSpheroid  (element);
                if (keyword.equalsIgnoreCase(WKTKeywords.Vert_Datum))  return parseVertDatum (element);
                if (keyword.equalsIgnoreCase(WKTKeywords.Local_Datum)) return parseLocalDatum(element);
                if (keyword.equalsIgnoreCase(WKTKeywords.Datum))       return parseDatum     (element, referencing.getGreenwich());
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
        final Object child = element.peek();
        if (child instanceof Element) {
            keyword = ((Element) child).keyword;
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
    private Map<String,Object> parseAuthorityAndClose(final Element parent, Object name) throws ParseException {
        assert (name instanceof String) || (name instanceof Identifier);
        properties.clear();
        properties.put(IdentifiedObject.NAME_KEY, name);
        final Element element = parent.pullOptionalElement(WKTKeywords.Authority);
        if (element != null) {
            final String auth = element.pullString("name");
            final String code = element.pullObject("code").toString();  // Accepts Integer as well as String.
            element.close(ignoredElements);
            final Citation authority = Citations.fromName(auth);
            properties.put(IdentifiedObject.IDENTIFIERS_KEY, new ImmutableIdentifier(authority, auth, code));
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
             * (for example "WGS84" for the datum instead than "World Geodetic System 1984"),
             * so the name in WKT is often not compliant with the name actually defined by the authority.
             */
        }
        parent.close(ignoredElements);
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
        final String  name    = element.pullString("name");
        final double  factor  = element.pullDouble("factor");
        parseAuthorityAndClose(element, name);
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
        final AxisDirection direction = Types.forCodeName(AxisDirection.class, orientation.keyword, mandatory);
        try {
            return csFactory.createCoordinateSystemAxis(parseAuthorityAndClose(element, name), name, direction, unit);
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
        try {
            return datumFactory.createPrimeMeridian(parseAuthorityAndClose(element, name), longitude, angularUnit);
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
     * @return The {@code "TOWGS84"} element as a {@link org.apache.sis.referencing.datum.BursaWolfParameters} object,
     *         or {@code null} if no {@code "TOWGS84"} has been found.
     * @throws ParseException if the {@code "TOWGS84"} can not be parsed.
     */
    private Object parseToWGS84(final Element parent) throws ParseException {
        final Element element = parent.pullOptionalElement(WKTKeywords.ToWGS84);
        if (element == null) {
            return null;
        }
        final double[] values = new double[ToWGS84.length];
        for (int i=0; i<values.length;) {
            values[i] = element.pullDouble(ToWGS84[i]);
            if ((++i % 3) == 0 && element.peek() == null) {
                break;  // It is legal to have only 3 or 6 elements.
            }
        }
        element.close(ignoredElements);
        return referencing.createToWGS84(values);
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
        final Element element      = parent.pullElement(WKTKeywords.Spheroid);
        final String name          = element.pullString("name");
        final double semiMajorAxis = element.pullDouble("semiMajorAxis");
        double inverseFlattening   = element.pullDouble("inverseFlattening");
        if (inverseFlattening == 0) {   // OGC convention for a sphere.
            inverseFlattening = Double.POSITIVE_INFINITY;
        }
        try {
            return datumFactory.createFlattenedSphere(parseAuthorityAndClose(element, name),
                    semiMajorAxis, inverseFlattening, SI.METRE);
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
        /*
         * Set the list of parameters. NOTE: Parameters are defined in the parent
         * Element (usually a "PROJCS" element), not in this "PROJECTION" element.
         */
        try {
            final OperationMethod method = opFactory.getOperationMethod(classification);
            final ParameterValueGroup parameters = method.getParameters().createValue();
            parseParameters(parent, parameters, linearUnit, angularUnit);
            return opFactory.createDefiningConversion(parseAuthorityAndClose(element, classification), method, parameters);
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
        final Element             element    = parent.pullElement(WKTKeywords.Datum);
        final String              name       = element.pullString("name");
        final Ellipsoid           ellipsoid  = parseSpheroid(element);
        final Object              toWGS84    = parseToWGS84(element);     // Optional; may be null.
        final Map<String,Object>  properties = parseAuthorityAndClose(element, name);
        if (toWGS84 != null) {
            properties.put(ReferencingServices.BURSA_WOLF_KEY, toWGS84);
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
        final VerticalDatumType type = VerticalDatumTypes.fromLegacy(datum);
        // TODO: need to find a default value if null.
        try {
            return datumFactory.createVerticalDatum(parseAuthorityAndClose(element, name), type);
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
        try {
            return datumFactory.createTemporalDatum(parseAuthorityAndClose(element, name), epoch);
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
        try {
            return datumFactory.createEngineeringDatum(parseAuthorityAndClose(element, name));
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
        final Element          element    = parent.pullElement(WKTKeywords.Local_CS);
        final String           name       = element.pullString("name");
        final EngineeringDatum datum      = parseLocalDatum(element);
        final Unit<Length>     linearUnit = parseUnit(element, SI.METRE);
        CoordinateSystemAxis   axis       = parseAxis(element, linearUnit, true);
        final List<CoordinateSystemAxis> list = new ArrayList<CoordinateSystemAxis>();
        do {
            list.add(axis);
            axis = parseAxis(element, linearUnit, false);
        } while (axis != null);
        final CoordinateSystem cs = referencing.createAbstractCS(list.toArray(new CoordinateSystemAxis[list.size()]));
        try {
            return crsFactory.createEngineeringCRS(parseAuthorityAndClose(element, name), datum, cs);
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
        final PrimeMeridian meridian   = parsePrimem(element, NonSI.DEGREE_ANGLE);
        final GeodeticDatum datum      = parseDatum (element, meridian);
        final Unit<Length>  linearUnit = parseUnit  (element, SI.METRE);
        CartesianCS cs;
        CoordinateSystemAxis axis0, axis1 = null, axis2 = null;
        axis0 = parseAxis(element, linearUnit, false);
        try {
            if (axis0 != null) {
                axis1 = parseAxis(element, linearUnit, true);
                axis2 = parseAxis(element, linearUnit, true);
            }
            final Map<String,?> properties = parseAuthorityAndClose(element, name);
            if (axis0 != null && !isAxisIgnored) {
                cs = csFactory.createCartesianCS(properties, axis0, axis1, axis2);
                cs = referencing.upgradeGeocentricCS(cs);
            } else {
                cs = referencing.getGeocentricCS(linearUnit);
            }
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
        final Element        element    = parent.pullElement(WKTKeywords.Vert_CS);
        final String         name       = element.pullString("name");
        final VerticalDatum  datum      = parseVertDatum(element);
        final Unit<Length>   linearUnit = parseUnit(element, SI.METRE);
        CoordinateSystemAxis axis       = parseAxis(element, linearUnit, false);
        try {
            if (axis == null || isAxisIgnored) {
                axis = createAxis("h", AxisDirection.UP, linearUnit);
            }
            return crsFactory.createVerticalCRS(parseAuthorityAndClose(element, name), datum,
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
        final Element        element  = parent.pullElement(WKTKeywords.TimeCRS);
        final String         name     = element.pullString("name");
        final TemporalDatum  datum    = parseTimeDatum(element);
        final Unit<Duration> timeUnit = parseUnit(element, SI.SECOND);
        CoordinateSystemAxis axis     = parseAxis(element, timeUnit, false);
        try {
            if (axis == null || isAxisIgnored) {
                axis = createAxis("t", AxisDirection.UP, timeUnit);
            }
            return crsFactory.createTemporalCRS(parseAuthorityAndClose(element, name), datum,
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
        final Element       element     = parent.pullElement(WKTKeywords.GeogCS);
              Object        name        = element.pullString("name");
        final Unit<Angle>   angularUnit = parseUnit  (element, SI.RADIAN);
        final PrimeMeridian meridian    = parsePrimem(element, angularUnit);
        final GeodeticDatum datum       = parseDatum (element, meridian);
        if (((String) name).isEmpty()) {
            /*
             * GeographicCRS name is a mandatory property, but some invalid WKT with an empty string exist.
             * In such case, we will use the name of the enclosed datum. Indeed, it is not uncommon to have
             * the same name for a geographic CRS and its geodetic datum.
             */
            name = datum.getName();
        }
        CoordinateSystemAxis axis0 = parseAxis(element, angularUnit, false);
        CoordinateSystemAxis axis1 = null;
        try {
            if (axis0 != null) {
                axis1 = parseAxis(element, angularUnit, true);
            }
            if (axis0 == null || isAxisIgnored) {
                axis0 = createAxis("λ", AxisDirection.EAST,  angularUnit);
                axis1 = createAxis("φ", AxisDirection.NORTH, angularUnit);
            }
            final Map<String,?> properties = parseAuthorityAndClose(element, name);
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
        final Element       element    = parent.pullElement(WKTKeywords.ProjCS);
        final String        name       = element.pullString("name");
        final GeographicCRS geoCRS     = parseGeoGCS(element);
        final Unit<Length>  linearUnit = parseUnit(element, SI.METRE);
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
            final Map<String,?> properties = parseAuthorityAndClose(element, name);
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
        final List<CoordinateReferenceSystem> components = new ArrayList<CoordinateReferenceSystem>(4);
        final Element element = parent.pullElement(WKTKeywords.Compd_CS);
        final String  name    = element.pullString("name");
        CoordinateReferenceSystem crs;
        while ((crs = parseCoordinateReferenceSystem(element, components.size() < 2)) != null) {
            components.add(crs);
        }
        try {
            return crsFactory.createCompoundCRS(parseAuthorityAndClose(element, name),
                    components.toArray(new CoordinateReferenceSystem[components.size()]));
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
        final Element                   element = parent.pullElement(WKTKeywords.Fitted_CS);
        final String                    name    = element.pullString("name");
        final MathTransform             toBase  = parseMathTransform(element, true);
        final OperationMethod           method  = getOperationMethod();
        final CoordinateReferenceSystem baseCRS = parseCoordinateReferenceSystem(element, true);
        if (!(baseCRS instanceof SingleCRS)) {
            throw new LocalizedParseException(errorLocale, Errors.Keys.UnexpectedValueInElement_2,
                    new Object[] {WKTKeywords.Fitted_CS, baseCRS.getClass()}, element.offset);
        }
        /*
         * WKT 1 provides no information about the underlying CS of a derived CRS.
         * We have to guess some reasonable one with arbitrary units. We try to construct the one which
         * contains as few information as possible, in order to avoid providing wrong informations.
         */
        final CoordinateSystemAxis[] axes = new CoordinateSystemAxis[toBase.getSourceDimensions()];
        final StringBuilder buffer = new StringBuilder(name).append(" axis ");
        final int start = buffer.length();
        try {
            for (int i=0; i<axes.length; i++) {
                final String number = String.valueOf(i);
                buffer.setLength(start);
                buffer.append(number);
                axes[i] = csFactory.createCoordinateSystemAxis(
                        singletonMap(IdentifiedObject.NAME_KEY, buffer.toString()),
                        number, AxisDirection.OTHER, Unit.ONE);
            }
            final Map<String,Object> properties = parseAuthorityAndClose(element, name);
            final CoordinateSystem derivedCS = referencing.createAbstractCS(axes);
            /*
             * We do not know which name to give to the conversion method.
             * For now, use the CRS name.
             */
            properties.put("conversion.name", name);
            return referencing.createDerivedCRS(properties, (SingleCRS) baseCRS, method, toBase.inverse(), derivedCS);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        } catch (NoninvertibleTransformException exception) {
            throw element.parseFailed(exception);
        }
    }
}
