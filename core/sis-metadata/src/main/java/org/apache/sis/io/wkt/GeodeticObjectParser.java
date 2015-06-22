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
import java.util.IdentityHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.ParseException;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import javax.measure.quantity.Quantity;
import javax.measure.quantity.Duration;

import org.opengis.util.Factory;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceSystem;
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
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultGeographicDescription;
import org.apache.sis.metadata.iso.extent.DefaultVerticalExtent;
import org.apache.sis.metadata.iso.extent.DefaultTemporalExtent;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.internal.metadata.AxisNames;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.internal.metadata.VerticalDatumTypes;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.util.LocalizedParseException;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.iso.Types;

import static java.util.Collections.singletonMap;


/**
 * Well Known Text (WKT) parser for referencing objects. This include, but is not limited too,
 * {@linkplain org.apache.sis.referencing.crs.AbstractCRS Coordinate Reference System} and
 * {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform Math Transform} objects.
 * Note that math transforms are part of the WKT 1 {@code "FITTED_CS"} element.
 *
 * @author  Rémi Eve (IRD)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
final class GeodeticObjectParser extends MathTransformParser implements Comparator<CoordinateSystemAxis> {
    /**
     * The keywords of unit elements. Most frequently used keywords should be first.
     */
    private static final String[] UNIT_KEYWORDS = {
        WKTKeywords.Unit,   // Ignored since it does not allow us to know the quantity dimension.
        WKTKeywords.LengthUnit, WKTKeywords.AngleUnit, WKTKeywords.ScaleUnit, WKTKeywords.TimeUnit,
        WKTKeywords.ParametricUnit  // Ignored for the same reason than "Unit".
    };

    /**
     * The base unit associated to the {@link #UNIT_KEYWORDS}, ignoring {@link WKTKeywords#Unit}.
     * For each {@code UNIT_KEYWORDS[i]} element, the associated base unit is {@code BASE_UNIT[i]}.
     */
    private static final Unit<?>[] BASE_UNITS = {
        SI.METRE, SI.RADIAN, Unit.ONE, SI.SECOND
    };

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
     *
     *   <li>{@link Convention#WKT1_IGNORE_AXES} means that axes should be parsed only for verifying the syntax,
     *       but otherwise parsing should behave as if axes were not declared.</li>
     * </ul>
     */
    private final Convention convention;

    /**
     * The object to use for replacing WKT axis names and abbreviations by ISO 19111 names and abbreviations.
     */
    private final Transliterator transliterator;

    /**
     * A map of properties to be given to the factory constructor methods.
     * This map will be recycled for each object to be parsed.
     */
    private final Map<String,Object> properties = new HashMap<>(4);

    /**
     * Order of coordinate system axes. Used only if {@code AXIS[…]} elements contain {@code ORDER[…]} sub-element.
     */
    private final Map<CoordinateSystemAxis,Integer> axisOrder = new IdentityHashMap<>(4);

    /**
     * The last vertical CRS found during the parsing, or {@code null} if none.
     * This information is needed for creating {@link DefaultVerticalExtent} instances.
     *
     * <p>ISO 19162 said that we should have at most one vertical CRS per WKT. Apache SIS does
     * not enforce this constraint, but if a WKT contains more than one vertical CRS then the
     * instance used for completing the {@link DefaultVerticalExtent} instances is unspecified.</p>
     */
    private transient VerticalCRS verticalCRS;

    /**
     * A chained list of temporary information needed for completing the construction of {@link DefaultVerticalExtent}
     * instances. In particular, stores the unit of measurement until the {@link VerticalCRS} instance to associate to
     * the extents become known.
     */
    private transient VerticalInfo verticalElements;

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
        super(Symbols.getDefault(), null, null, null, mtFactory, (Locale) defaultProperties.get(Errors.LOCALE_KEY));
        crsFactory     = (CRSFactory)   factories;
        csFactory      = (CSFactory)    factories;
        datumFactory   = (DatumFactory) factories;
        referencing    = ReferencingServices.getInstance();
        opFactory      = referencing.getCoordinateOperationFactory(defaultProperties, mtFactory);
        convention     = Convention.DEFAULT;
        transliterator = Transliterator.DEFAULT;
    }

    /**
     * Constructs a parser for the specified set of symbols using the specified set of factories.
     * This constructor is for {@link WKTFormat} usage only.
     *
     * @param symbols       The set of symbols to use.
     * @param numberFormat  The number format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param dateFormat    The date format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param unitFormat    The unit format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param convention    The WKT convention to use.
     * @param errorLocale   The locale for error messages (not for parsing), or {@code null} for the system default.
     * @param factories     On input, the factories to use. On output, the factories used. Can be null.
     */
    GeodeticObjectParser(final Symbols symbols, final NumberFormat numberFormat, final DateFormat dateFormat,
            final UnitFormat unitFormat, final Convention convention, final Transliterator transliterator,
            final Locale errorLocale, final Map<Class<?>,Factory> factories)
    {
        super(symbols, numberFormat, dateFormat, unitFormat, getFactory(MathTransformFactory.class, factories), errorLocale);
        this.convention = convention;
        this.transliterator = transliterator;
        crsFactory   = getFactory(CRSFactory.class,   factories);
        csFactory    = getFactory(CSFactory.class,    factories);
        datumFactory = getFactory(DatumFactory.class, factories);
        referencing  = ReferencingServices.getInstance();
        opFactory    = referencing.getCoordinateOperationFactory(null, mtFactory);
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
        final Object object;
        try {
            object = super.parseObject(text, position);
            /*
             * After parsing the object, we may have been unable to set the VerticalCRS of VerticalExtent instances.
             * First, try to set a default VerticalCRS for Mean Sea Level Height in metres. In the majority of cases
             * that should be enough. If not (typically because the vertical extent uses other unit than metre), try
             * to create a new CRS using the unit declared in the WKT.
             */
            if (verticalElements != null) {
                Exception ex = null;
                try {
                    verticalElements = verticalElements.resolve(referencing.getMSLH());     // Optional operation.
                } catch (UnsupportedOperationException e) {
                    ex = e;
                }
                if (verticalElements != null) try {
                    verticalElements = verticalElements.complete(crsFactory, csFactory);
                } catch (FactoryException e) {
                    if (ex == null) ex = e;
                    else ex.addSuppressed(e);
                }
                if (verticalElements != null) {
                    warning(Errors.formatInternational(Errors.Keys.CanNotAssignUnitToDimension_2,
                            WKTKeywords.VerticalExtent, verticalElements.unit), ex);
                }
            }
        } finally {
            verticalElements = null;
            verticalCRS = null;
            axisOrder.clear();
            properties.clear();     // for letting the garbage collector do its work.
        }
        return object;
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
        Object object;
        if ((object = parseAxis             (FIRST, element, null, SI.METRE)) == null &&
            (object = parsePrimeMeridian    (FIRST, element, NonSI.DEGREE_ANGLE)) == null &&
            (object = parseDatum            (FIRST, element, null)) == null &&
            (object = parseEllipsoid        (FIRST, element)) == null &&
            (object = parseToWGS84          (FIRST, element)) == null &&
            (object = parseVerticalDatum    (FIRST, element)) == null &&
            (object = parseTimeDatum        (FIRST, element)) == null &&
            (object = parseEngineeringDatum (FIRST, element)) == null &&
            (object = parseProjection       (FIRST, element, SI.METRE, NonSI.DEGREE_ANGLE)) == null)
        {
            throw element.missingOrUnknownComponent(WKTKeywords.GeodeticCRS);
        }
        return object;
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
        CoordinateReferenceSystem crs;
        if ((crs = parseGeographicCRS  (FIRST, element)) == null &&
            (crs = parseProjectedCRS   (FIRST, element)) == null &&
            (crs = parseGeocentricCRS  (FIRST, element)) == null &&
            (crs = parseVerticalCRS    (FIRST, element)) == null &&
            (crs = parseTimeCRS        (FIRST, element)) == null &&
            (crs = parseEngineeringCRS (FIRST, element)) == null &&
            (crs = parseCompoundCRS    (FIRST, element)) == null &&
            (crs = parseFittedCS       (FIRST, element)) == null)
        {
            if (mandatory) {
                throw element.missingOrUnknownComponent(WKTKeywords.GeodeticCRS);
            }
        }
        return crs;
    }

    /**
     * Parses an <strong>optional</strong> metadata elements and close.
     * This include elements like {@code "SCOPE"}, {@code "ID"} (WKT 2) or {@code "AUTHORITY"} (WKT 1).
     * This WKT 1 element has the following pattern:
     *
     * {@preformat text
     *     AUTHORITY["<name>", "<code>"]
     * }
     *
     * @param  parent The parent element.
     * @param  name The name of the parent object being parsed, either a {@link String} or {@link Identifier} instance.
     * @return A properties map with the parent name and the optional authority code.
     * @throws ParseException if an element can not be parsed.
     */
    private Map<String,Object> parseMetadataAndClose(final Element parent, Object name) throws ParseException {
        assert (name instanceof String) || (name instanceof Identifier);
        properties.clear();
        properties.put(IdentifiedObject.NAME_KEY, name);
        Element element;
        while ((element = parent.pullElement(OPTIONAL, WKTKeywords.Id, WKTKeywords.Authority)) != null) {
            final String   codeSpace = element.pullString("name");
            final String   code      = element.pullObject("code").toString();   // Accepts Integer as well as String.
            final Object   version   = element.pullOptional(Object.class);      // Accepts Number as well as String.
            final Element  citation  = element.pullElement(OPTIONAL, WKTKeywords.Citation);
            final String   authority;
            if (citation != null) {
                authority = citation.pullString("authority");
                citation.close(ignoredElements);
            } else {
                authority = codeSpace;
            }
            final Element uri = element.pullElement(OPTIONAL, WKTKeywords.URI);
            if (uri != null) {
                uri.pullString("URI");      // TODO: not yet stored, since often redundant with other informations.
                uri.close(ignoredElements);
            }
            element.close(ignoredElements);
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
            final ImmutableIdentifier id = new ImmutableIdentifier(Citations.fromName(authority),
                    codeSpace, code, (version != null) ? version.toString() : null, null);
            final Object previous = properties.put(IdentifiedObject.IDENTIFIERS_KEY, id);
            if (previous != null) {
                Identifier[] identifiers;
                if (previous instanceof Identifier) {
                    identifiers = new Identifier[] {(Identifier) previous, id};
                } else {
                    identifiers = (Identifier[]) previous;
                    final int n = identifiers.length;
                    identifiers = Arrays.copyOf(identifiers, n + 1);
                    identifiers[n] = id;
                }
                properties.put(IdentifiedObject.IDENTIFIERS_KEY, identifiers);
            }
        }
        /*
         * Other metadata (SCOPE, AREA, etc.).  ISO 19162 said that at most one of each type shall be present,
         * but our parser accepts an arbitrary amount of some kinds of metadata. They can be recognized by the
         * 'while' loop.
         *
         * Most WKT do not contain any of those metadata, so we perform an 'isEmpty()' check as an optimization
         * for those common cases.
         */
        if (!parent.isEmpty()) {
            /*
             * Example: SCOPE["Large scale topographic mapping and cadastre."]
             */
            element = parent.pullElement(OPTIONAL, WKTKeywords.Scope);
            if (element != null) {
                properties.put(ReferenceSystem.SCOPE_KEY, element.pullString("scope"));  // Other types like Datum use the same key.
                element.close(ignoredElements);
            }
            /*
             * Example: AREA["Netherlands offshore."]
             */
            DefaultExtent extent = null;
            while ((element = parent.pullElement(OPTIONAL, WKTKeywords.Area)) != null) {
                final String area = element.pullString("area");
                element.close(ignoredElements);
                if (extent == null) extent = new DefaultExtent();
                extent.getGeographicElements().add(new DefaultGeographicDescription(area));
            }
            /*
             * Example: BBOX[51.43, 2.54, 55.77, 6.40]
             */
            while ((element = parent.pullElement(OPTIONAL, WKTKeywords.BBox)) != null) {
                final double southBoundLatitude = element.pullDouble("southBoundLatitude");
                final double westBoundLongitude = element.pullDouble("westBoundLongitude");
                final double northBoundLatitude = element.pullDouble("northBoundLatitude");
                final double eastBoundLongitude = element.pullDouble("eastBoundLongitude");
                element.close(ignoredElements);
                if (extent == null) extent = new DefaultExtent();
                extent.getGeographicElements().add(new DefaultGeographicBoundingBox(
                        westBoundLongitude, eastBoundLongitude, southBoundLatitude, northBoundLatitude));
            }
            /*
             * Example: VERTICALEXTENT[-1000, 0, LENGTHUNIT[“metre”, 1]]
             *
             * Units are optional, default to metres (no "contextual units" here).
             */
            while ((element = parent.pullElement(OPTIONAL, WKTKeywords.VerticalExtent)) != null) {
                final double minimum = element.pullDouble("minimum");
                final double maximum = element.pullDouble("maximum");
                Unit<Length> unit = parseDerivedUnit(element, WKTKeywords.LengthUnit, SI.METRE);
                element.close(ignoredElements);
                if (unit   == null) unit   = SI.METRE;
                if (extent == null) extent = new DefaultExtent();
                verticalElements = new VerticalInfo(verticalElements, extent, minimum, maximum, unit).resolve(verticalCRS);
            }
            /*
             * Example: TIMEEXTENT[2013-01-01, 2013-12-31]
             *
             * TODO: syntax like TIMEEXTENT[“Jurassic”, “Quaternary”] is not yet supported.
             * See https://issues.apache.org/jira/browse/SIS-163
             *
             * This operation requires the the sis-temporal module. If not available,
             * we will report a warning and leave the temporal extent missing.
             */
            while ((element = parent.pullElement(OPTIONAL, WKTKeywords.TimeExtent)) != null) {
                final Date startTime = element.pullDate("startTime");
                final Date endTime   = element.pullDate("endTime");
                element.close(ignoredElements);
                try {
                    final DefaultTemporalExtent t = new DefaultTemporalExtent();
                    t.setBounds(startTime, endTime);
                    if (extent == null) extent = new DefaultExtent();
                    extent.getTemporalElements().add(t);
                } catch (UnsupportedOperationException e) {
                    warning(parent, element, e);
                }
            }
            /*
             * Example: REMARK["Замечание на русском языке"]
             */
            element = parent.pullElement(OPTIONAL, WKTKeywords.Remark);
            if (element != null) {
                properties.put(IdentifiedObject.REMARKS_KEY, element.pullString("remarks"));
                element.close(ignoredElements);
            }
        }
        parent.close(ignoredElements);
        return properties;
    }

    /**
     * Parses an optional {@code "UNIT"} element of a known dimension.
     * This element has the following pattern:
     *
     * {@preformat text
     *     UNIT["<name>", <conversion factor> {,<authority>}]
     * }
     *
     * Unit was a mandatory element in WKT 1, but became optional in WKT 2 because the unit may be specified
     * in each {@code AXIS[…]} element instead than for the whole coordinate system.
     *
     * @param  parent   The parent element.
     * @param  keyword  The unit keyword (e.g. {@code "LengthUnit"} or {@code "AngleUnit"}).
     * @param  baseUnit The base unit, usually {@code SI.METRE} or {@code SI.RADIAN}.
     * @return The {@code "UNIT"} element as an {@link Unit} object, or {@code null} if none.
     * @throws ParseException if the {@code "UNIT"} can not be parsed.
     *
     * @todo Authority code is currently ignored. We may consider to create a subclass of
     *       {@link Unit} which implements {@link IdentifiedObject} in a future version.
     */
    private <Q extends Quantity> Unit<Q> parseDerivedUnit(final Element parent,
            final String keyword, final Unit<Q> baseUnit) throws ParseException
    {
        final Element element = parent.pullElement(OPTIONAL, keyword, WKTKeywords.Unit);
        if (element == null) {
            return null;
        }
        final String name   = element.pullString("name");
        final double factor = element.pullDouble("factor");
        parseMetadataAndClose(element, name);
        return Units.multiply(baseUnit, factor);
    }

    /**
     * Parses an optional {@code "UNIT"} element of unknown dimension.
     * This method tries to infer the quantity dimension from the unit keyword.
     *
     * @param  parent The parent element.
     * @return The {@code "UNIT"} element, or {@code null} if none.
     * @throws ParseException if the {@code "UNIT"} can not be parsed.
     */
    private Unit<?> parseUnit(final Element parent) throws ParseException {
        final Element element = parent.pullElement(OPTIONAL, UNIT_KEYWORDS);
        if (element == null) {
            return null;
        }
        final String name   = element.pullString("name");
        final double factor = element.pullDouble("factor");
        final int    index  = element.getKeywordIndex() - 1;
        parseMetadataAndClose(element, name);
        if (index >= 0 && index < BASE_UNITS.length) {
            return Units.multiply(BASE_UNITS[index - 1], factor);
        }
        // If we can not infer the base type, we have to rely on the name.
        return parseUnit(name);
    }

    /**
     * Parses a {@code "CS"} element followed by all {@code "AXIS"} elements.
     * This element has the following pattern (simplified):
     *
     * {@preformat text
     *     CS["<type>", dimension],
     *     AXIS["<name>", NORTH | SOUTH | EAST | WEST | UP | DOWN | OTHER],
     *     UNIT["<name>", <conversion factor>],
     *     etc.
     * }
     *
     * This element is different from all other elements parsed by {@code GeodeticObjectParser}
     * in that its components are sibling elements rather than child elements of the CS element.
     *
     * <p>The optional {@code "UNIT[…]"} element shall be parsed by the caller. That element may appear after the
     * {@code "CS[…]"} element (not inside). The unit may be forced to some dimension (e.g. {@code "LengthUnit"})
     * or be any kind of unit, depending on the context in which this {@code parseCoordinateSystem(…)} method is
     * invoked.</p>
     *
     * <div class="section">Variants of Cartesian type</div>
     * The {@link WKTKeywords#Cartesian} type may be used for projected, geocentric or other kinds of CRS.
     * However while all those variants are of the same CS type, their axis names and directions differ.
     * Current implementation uses the following rules:
     *
     * <ul>
     *   <li>If the datum is not geodetic, then the axes of the Cartesian CS are unknown.</li>
     *   <li>Otherwise if {@code dimension is 2}, then the CS is assumed to be for a projected CRS.</li>
     *   <li>Otherwise if {@code dimension is 3}, then the CS is assumed to be for a geocentric CRS.</li>
     * </ul>
     *
     * @param  parent      The parent element.
     * @param  type        The expected type (Cartesian | ellipsoidal | vertical | etc…), or null if unknown.
     * @param  dimension   The minimal number of dimensions. Can be 1 if unknown.
     * @param  defaultUnit The contextual unit (usually {@code SI.METRE} or {@code SI.RADIAN}), or {@code null} if unknown.
     * @param  datum       The datum of the enclosing CRS.
     * @return The {@code "CS"}, {@code "UNIT"} and/or {@code "AXIS"} elements as a Coordinate System, or {@code null}.
     * @throws ParseException if an element can not be parsed.
     * @throws FactoryException if the factory can not create the coordinate system.
     */
    private CoordinateSystem parseCoordinateSystem(final Element parent, String type, int dimension,
            final Unit<?> defaultUnit, final Datum datum) throws ParseException, FactoryException
    {
        axisOrder.clear();
        final boolean is3D = (dimension >= 3);
        Map<String,Object> csProperties = null;
        { // For keeping the 'element' variable local to this block, for reducing the risk of accidental reuse.
            final Element element = parent.pullElement(OPTIONAL, WKTKeywords.CS);
            if (element != null) {
                final String expected = type;
                type         = CharSequences.trimWhitespaces(element.pullString("type"));
                dimension    = element.pullInteger("dimension");
                csProperties = new HashMap<>(parseMetadataAndClose(element, "CS"));
                if (expected != null) {
                    if (!expected.equalsIgnoreCase(type)) {
                        throw new LocalizedParseException(errorLocale, Errors.Keys.UnexpectedValueInElement_2,
                                new String[] {WKTKeywords.CS, type}, element.offset);
                    }
                }
                if (dimension <= 0 || dimension > 1000) {  // Arbitrary upper limit against badly formed CS.
                    final short key;
                    final Object[] args;
                    if (dimension <= 0) {
                        key = Errors.Keys.ValueNotGreaterThanZero_2;
                        args = new Object[] {"dimension", dimension};
                    } else {
                        key = Errors.Keys.ExcessiveNumberOfDimensions_1;
                        args = new Object[] {dimension};
                    }
                    throw new LocalizedParseException(errorLocale, key, args, element.offset);
                }
                type = type.equalsIgnoreCase(WKTKeywords.Cartesian) ?
                       WKTKeywords.Cartesian : type.toLowerCase(symbols.getLocale());
            }
        }
        /*
         * AXIS[…] elements are optional, but if we find one we will request that there is as many axes
         * as the number of dimensions. If there is more axes than expected, we may emit an error later
         * depending on the CS type.
         *
         * AXIS[…] elements will be parsed for verifying the syntax, but otherwise ignored if the parsing
         * convention is WKT1_IGNORE_AXES. This is for compatibility with the way some other libraries
         * parse WKT 1.
         */
        CoordinateSystemAxis[] axes = null;
        CoordinateSystemAxis axis = parseAxis(type == null ? MANDATORY : OPTIONAL, parent, type, defaultUnit);
        if (axis != null) {
            final List<CoordinateSystemAxis> list = new ArrayList<>(dimension + 2);
            do {
                list.add(axis);
                axis = parseAxis(list.size() < dimension ? MANDATORY : OPTIONAL, parent, type, defaultUnit);
            } while (axis != null);
            if (convention != Convention.WKT1_IGNORE_AXES) {
                axes = list.toArray(new CoordinateSystemAxis[list.size()]);
                Arrays.sort(axes, this);  // Take ORDER[n] elements in account.
            }
        }
        /*
         * If there is no explicit AXIS[…] elements, or if the user asked to ignore them, then we need to
         * create default axes. This is possible only if we know the type of the CS to create, and only
         * for some of those CS types.
         */
        if (axes == null) {
            if (type == null) {
                throw parent.missingComponent(WKTKeywords.Axis);
            }
            String nx = null, x = null;     // Easting or Longitude axis name and abbreviation.
            String ny = null, y = null;     // Northing or latitude axis name and abbreviation.
            String nz = null, z = null;     // Depth, height or time axis name and abbreviation.
            AxisDirection direction = null; // Depth, height or time axis direction.
            Unit<?> unit = defaultUnit;     // Depth, height or time axis unit.
            switch (type) {
                /*
                 * Unknown CS type — we can not guess which axes to create.
                 */
                default: {
                    throw parent.missingComponent(WKTKeywords.Axis);
                }
                /*
                 * Cartesian — we can create axes only for geodetic datum, in which case the axes are for
                 * two-dimensional Projected or three-dimensional Geocentric CRS.
                 */
                case WKTKeywords.Cartesian: {
                    if (!(datum instanceof GeodeticDatum)) {
                        throw parent.missingComponent(WKTKeywords.Axis);
                    }
                    if (defaultUnit == null) {
                        throw parent.missingComponent(WKTKeywords.LengthUnit);
                    }
                    if (is3D) {  // If dimension can not be 2, then CRS can not be Projected.
                        return referencing.getGeocentricCS(defaultUnit.asType(Length.class));
                    }
                    nx = AxisNames.EASTING;  x = "E";
                    ny = AxisNames.NORTHING; y = "N";
                    if (dimension >= 3) {   // Non-standard but SIS is tolerant to this case.
                        z    = "h";
                        nz   = AxisNames.ELLIPSOIDAL_HEIGHT;
                        unit = SI.METRE;
                    }
                    break;
                }
                /*
                 * Ellipsoidal — can be two- or three- dimensional, in which case the height can
                 * only be ellipsoidal height.
                 */
                case WKTKeywords.ellipsoidal: {
                    if (defaultUnit == null) {
                        throw parent.missingComponent(WKTKeywords.AngleUnit);
                    }
                    nx = AxisNames.GEODETIC_LONGITUDE; x = "λ";
                    ny = AxisNames.GEODETIC_LATITUDE;  y = "φ";
                    if (dimension >= 3) {
                        z    = "h";
                        nz   = AxisNames.ELLIPSOIDAL_HEIGHT;
                        unit = SI.METRE;
                    }
                    break;
                }
                /*
                 * Vertical — the default name and symbol depends on whether this is depth,
                 * geoidal height, ellipsoidal height (non-standard) or other kind of heights.
                 */
                case WKTKeywords.vertical: {
                    if (defaultUnit == null) {
                        throw parent.missingComponent(WKTKeywords.Unit);
                    }
                    direction = AxisDirection.UP;
                    final VerticalDatumType vt = ((VerticalDatum) datum).getVerticalDatumType();
                    if (VerticalDatumType.GEOIDAL.equals(vt)) {
                        nz = AxisNames.GRAVITY_RELATED_HEIGHT;
                        z  = "H";
                    } else if (VerticalDatumType.DEPTH.equals(vt)) {
                        direction = AxisDirection.DOWN;
                        nz = AxisNames.DEPTH;
                        z  = "D";
                    } else if (VerticalDatumTypes.ELLIPSOIDAL.equals(vt)) {
                        nz = AxisNames.ELLIPSOIDAL_HEIGHT;  // Not allowed by ISO 19111 as a standalone axis, but SIS is
                        z  = "h";                           // tolerant to this case since it is sometime hard to avoid.
                    } else {
                        nz = "Height";
                        z  = "h";
                    }
                    break;
                }
                /*
                 * Temporal — axis name and abbreviation not yet specified by ISO 19111.
                 */
                case WKTKeywords.temporal: {
                    if (defaultUnit == null) {
                        throw parent.missingComponent(WKTKeywords.TimeUnit);
                    }
                    direction = AxisDirection.FUTURE;
                    nz = "Time";
                    z = "t";
                    break;
                }
            }
            int i = 0;
            axes = new CoordinateSystemAxis[dimension];
            if (x != null && i < dimension) axes[i++] = csFactory.createCoordinateSystemAxis(singletonMap(CoordinateSystemAxis.NAME_KEY, nx), x, AxisDirection.EAST,  defaultUnit);
            if (y != null && i < dimension) axes[i++] = csFactory.createCoordinateSystemAxis(singletonMap(CoordinateSystemAxis.NAME_KEY, ny), y, AxisDirection.NORTH, defaultUnit);
            if (z != null && i < dimension) axes[i++] = csFactory.createCoordinateSystemAxis(singletonMap(CoordinateSystemAxis.NAME_KEY, nz), z, direction, unit);
            // Not a problem if the array does not have the expected length for the CS type. This will be verified below in this method.
        }
        /*
         * Infer a CS name will be inferred from the axes if possible.
         * Example: "Compound CS: East (km), North (km), Up (m)."
         */
        final String name;
        { // For keeping the 'buffer' variable local to this block.
            final StringBuilder buffer = new StringBuilder();
            if (type != null && !type.isEmpty()) {
                final int c = type.codePointAt(0);
                buffer.appendCodePoint(Character.toUpperCase(c))
                        .append(type, Character.charCount(c), type.length()).append(' ');
            }
            name = AxisDirections.appendTo(buffer.append("CS"), axes);
        }
        if (csProperties == null) {
            csProperties = singletonMap(CoordinateSystem.NAME_KEY, name);
        } else {
            csProperties.put(CoordinateSystem.NAME_KEY, name);
        }
        if (type == null) {
            return referencing.createAbstractCS(csProperties, axes);
        }
        /*
         * Finally, delegate to the factory method corresponding to the CS type and the number of axes.
         */
        switch (type) {
            case WKTKeywords.ellipsoidal: {
                switch (axes.length) {
                    case 2: return csFactory.createEllipsoidalCS(csProperties, axes[0], axes[1]);
                    case 3: return csFactory.createEllipsoidalCS(csProperties, axes[0], axes[1], axes[2]);
                }
                dimension = (axes.length < 2) ? 2 : 3;  // For error message.
                break;
            }
            case WKTKeywords.Cartesian: {
                switch (axes.length) {
                    case 2: return csFactory.createCartesianCS(csProperties, axes[0], axes[1]);
                    case 3: return csFactory.createCartesianCS(csProperties, axes[0], axes[1], axes[2]);
                }
                dimension = (axes.length < 2) ? 2 : 3;  // For error message.
                break;
            }
            case WKTKeywords.affine: {
                switch (axes.length) {
                    case 2: return csFactory.createAffineCS(csProperties, axes[0], axes[1]);
                    case 3: return csFactory.createAffineCS(csProperties, axes[0], axes[1], axes[2]);
                }
                dimension = (axes.length < 2) ? 2 : 3;  // For error message.
                break;
            }
            case WKTKeywords.vertical: {
                if (axes.length != (dimension = 1)) break;
                return csFactory.createVerticalCS(csProperties, axes[0]);
            }
            case WKTKeywords.temporal: {
                if (axes.length != (dimension = 1)) break;
                return csFactory.createTimeCS(csProperties, axes[0]);
            }
            case WKTKeywords.linear: {
                if (axes.length != (dimension = 1)) break;
                return csFactory.createLinearCS(csProperties, axes[0]);
            }
            case WKTKeywords.polar: {
                if (axes.length != (dimension = 2)) break;
                return csFactory.createPolarCS(csProperties, axes[0], axes[1]);
            }
            case WKTKeywords.cylindrical: {
                if (axes.length != (dimension = 3)) break;
                return csFactory.createCylindricalCS(csProperties, axes[0], axes[1], axes[2]);
            }
            case WKTKeywords.spherical: {
                if (axes.length != (dimension = 3)) break;
                return csFactory.createSphericalCS(csProperties, axes[0], axes[1], axes[2]);
            }
            case WKTKeywords.parametric: {  // TODO: not yet supported.
                return referencing.createAbstractCS(csProperties, axes);
            }
            default: {
                warning(Errors.formatInternational(Errors.Keys.UnknownType_1, type), null);
                return referencing.createAbstractCS(csProperties, axes);
            }
        }
        throw new LocalizedParseException(errorLocale, (axes.length > dimension)
                ? Errors.Keys.TooManyOccurrences_2 : Errors.Keys.TooFewOccurrences_2,
                new Object[] {dimension, WKTKeywords.Axis}, parent.offset);
    }

    /**
     * Parses an {@code "AXIS"} element.
     * This element has the following pattern (simplified):
     *
     * {@preformat text
     *     AXIS["<name (abbr.)>", NORTH | SOUTH | EAST | WEST | UP | DOWN | OTHER, ORDER[n], UNIT[…], ID[…]]
     * }
     *
     * @param  mode        {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent      The parent element.
     * @param  csType      The coordinate system type (Cartesian | ellipsoidal | vertical | etc…), or null if unknown.
     * @param  defaultUnit The contextual unit (usually {@code SI.METRE} or {@code SI.RADIAN}), or {@code null} if unknown.
     * @return The {@code "AXIS"} element as a {@link CoordinateSystemAxis} object, or {@code null}
     *         if the axis was not required and there is no axis object.
     * @throws ParseException if the {@code "AXIS"} element can not be parsed.
     */
    private CoordinateSystemAxis parseAxis(final int mode, final Element parent, final String csType,
            final Unit<?> defaultUnit) throws ParseException
    {
        final Element element = parent.pullElement(mode, WKTKeywords.Axis);
        if (element == null) {
            return null;
        }
        /*
         * Name, orientation (usually NORTH, SOUTH, EAST or WEST) and units are the main components of AXIS[…].
         * The name may contain an abbreviation, which will be handle later in this method. In the special case
         * of coordinate system over a pole, the orientation may be of the form “South along 90°W”, which is
         * expressed by a syntax like AXIS[“South along 90°W”, SOUTH, MERIDIAN[-90, UNIT["deg"]]]. Note that
         * the meridian is relative to the prime meridian of the enclosing geodetic CRS.
         */
        String name = CharSequences.trimWhitespaces(element.pullString("name"));
        final Element orientation = element.pullVoidElement("orientation");
        Unit<?> unit = parseUnit(element);
        if (unit == null) {
            if (defaultUnit == null) {
                throw element.missingComponent(WKTKeywords.Unit);
            }
            unit = defaultUnit;
        }
        AxisDirection direction = Types.forCodeName(AxisDirection.class, orientation.keyword, true);
        final Element meridian = element.pullElement(OPTIONAL, WKTKeywords.Meridian);
        if (meridian != null) {
            double angle = meridian.pullDouble("meridian");
            final Unit<Angle> m = parseDerivedUnit(meridian, WKTKeywords.AngleUnit, SI.RADIAN);
            meridian.close(ignoredElements);
            if (m != null) {
                angle = m.getConverterTo(NonSI.DEGREE_ANGLE).convert(angle);
            }
            direction = referencing.directionAlongMeridian(direction, angle);
        }
        /*
         * According ISO 19162, the abbreviation should be inserted between parenthesis in the name.
         * Example: "Easting (E)", "Longitude (L)". If we do not find an abbreviation, then we will
         * have to guess one since abbreviation is a mandatory part of axis.
         */
        String abbreviation;
        final int start, end = name.length() - 1;
        if (end > 1 && name.charAt(end) == ')' && (start = name.lastIndexOf('(', end-1)) >= 0) {
            abbreviation = CharSequences.trimWhitespaces(name.substring(start + 1, end));
            name = CharSequences.trimWhitespaces(name.substring(0, start));
            if (name.isEmpty()) {
                name = abbreviation;
            }
        } else {
            abbreviation = AxisDirections.suggestAbbreviation(name, direction, unit);
        }
        /*
         * The longitude and latitude axis names are explicitly fixed by ISO 19111:2007 to "Geodetic longitude"
         * and "Geodetic latitude". But ISO 19162:2015 §7.5.3(ii) said that the "Geodetic" part in those names
         * shall be omitted at WKT formatting time. SIS's DefaultCoordinateSystemAxis.formatTo(Formatter)
         * method performs this removal, so we apply the reverse operation here.
         */
        name         = transliterator.toLongAxisName       (csType, direction, name);
        abbreviation = transliterator.toUnicodeAbbreviation(csType, direction, abbreviation);
        /*
         * At this point we are done and ready to create the CoordinateSystemAxis. But there is one last element
         * specified by ISO 19162 but not in Apache SIS representation of axis: ORDER[n], which specify the axis
         * ordering. If present we will store that value for processing by the 'parseCoordinateSystem(…)' method.
         */
        final Element order = element.pullElement(OPTIONAL, WKTKeywords.Order);
        Integer n = null;
        if (order != null) {
            n = order.pullInteger("order");
            order.close(ignoredElements);
        }
        final CoordinateSystemAxis axis;
        try {
            axis = csFactory.createCoordinateSystemAxis(parseMetadataAndClose(element, name), abbreviation, direction, unit);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
        if (axisOrder.put(axis, n) != null) {   // Opportunist check, effective for instances created by SIS factory.
            throw new LocalizedParseException(errorLocale, Errors.Keys.DuplicatedElement_1,
                    new Object[] {WKTKeywords.Axis + "[“" + name + "”]"}, element.offset);
        }
        return axis;
    }

    /**
     * Compares axes for order. This method is used for ordering axes according their {@code ORDER} element,
     * if present. If no {@code ORDER} element were present, then the axis order is left unchanged. If only
     * some axes have an {@code ORDER} element (which is illegal according ISO 19162), then those axes will
     * be sorted before the axes without {@code ORDER} element.
     *
     * @param  o1 The first axis to compare.
     * @param  o2 The second axis to compare.
     * @return -1 if {@code o1} should be before {@code o2},
     *         +1 if {@code o2} should be before {@code o1}, or
     *          0 if undetermined (no axis order change).
     */
    @Override
    public int compare(final CoordinateSystemAxis o1, final CoordinateSystemAxis o2) {
        final Integer n1 = axisOrder.get(o1);
        final Integer n2 = axisOrder.get(o2);
        if (n1 != null) {
            if (n2 != null) {
                return n1 - n2;
            }
            return -1;  // Axis 1 before Axis 2 since the later has no 'ORDER' element.
        } else if (n2 != null) {
            return +1;  // Axis 2 before Axis 1 since the later has no 'ORDER' element.
        }
        return 0;
    }

    /**
     * Parses a {@code "PRIMEM"} element. This element has the following pattern:
     *
     * {@preformat text
     *     PRIMEM["<name>", <longitude> {,<authority>}]
     * }
     *
     * @param  mode {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @param  angularUnit The contextual unit.
     * @return The {@code "PRIMEM"} element as a {@link PrimeMeridian} object.
     * @throws ParseException if the {@code "PRIMEM"} element can not be parsed.
     */
    private PrimeMeridian parsePrimeMeridian(final int mode, final Element parent, Unit<Angle> angularUnit)
            throws ParseException
    {
        if (convention.usesCommonUnits) {
            angularUnit = NonSI.DEGREE_ANGLE;
        }
        final Element element = parent.pullElement(mode, WKTKeywords.PrimeMeridian, WKTKeywords.PrimeM);
        if (element == null) {
            return null;
        }
        final String name      = element.pullString("name");
        final double longitude = element.pullDouble("longitude");
        if (angularUnit == null) {
            throw element.missingComponent(WKTKeywords.AngleUnit);
        }
        try {
            return datumFactory.createPrimeMeridian(parseMetadataAndClose(element, name), longitude, angularUnit);
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
     * @param  mode {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @return The {@code "TOWGS84"} element as a {@link org.apache.sis.referencing.datum.BursaWolfParameters} object,
     *         or {@code null} if no {@code "TOWGS84"} has been found.
     * @throws ParseException if the {@code "TOWGS84"} can not be parsed.
     */
    private Object parseToWGS84(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.ToWGS84);
        if (element == null) {
            return null;
        }
        final double[] values = new double[ToWGS84.length];
        for (int i=0; i<values.length;) {
            values[i] = element.pullDouble(ToWGS84[i]);
            if ((++i % 3) == 0 && element.isEmpty()) {
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
     * @param  mode {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @return The {@code "SPHEROID"} element as an {@link Ellipsoid} object.
     * @throws ParseException if the {@code "SPHEROID"} element can not be parsed.
     */
    private Ellipsoid parseEllipsoid(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.Ellipsoid, WKTKeywords.Spheroid);
        if (element == null) {
            return null;
        }
        final String name          = element.pullString("name");
        final double semiMajorAxis = element.pullDouble("semiMajorAxis");
        double inverseFlattening   = element.pullDouble("inverseFlattening");
        if (inverseFlattening == 0) {   // OGC convention for a sphere.
            inverseFlattening = Double.POSITIVE_INFINITY;
        }
        try {
            return datumFactory.createFlattenedSphere(parseMetadataAndClose(element, name),
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
     * @param  mode        {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent      The parent element.
     * @param  linearUnit  The linear unit of the parent {@code PROJCS} element, or {@code null}.
     * @param  angularUnit The angular unit of the parent {@code GEOCS} element, or {@code null}.
     * @return The {@code "PROJECTION"} element as a defining conversion.
     * @throws ParseException if the {@code "PROJECTION"} element can not be parsed.
     */
    private Conversion parseProjection(final int mode, final Element parent,
            final Unit<Length> linearUnit, final Unit<Angle> angularUnit) throws ParseException
    {
        final Element element = parent.pullElement(mode, WKTKeywords.Projection);
        if (element == null) {
            return null;
        }
        final String classification = element.pullString("name");
        /*
         * Set the list of parameters. NOTE: Parameters are defined in the parent
         * Element (usually a "PROJCS" element), not in this "PROJECTION" element.
         */
        try {
            final OperationMethod method = opFactory.getOperationMethod(classification);
            final ParameterValueGroup parameters = method.getParameters().createValue();
            parseParameters(parent, parameters, linearUnit, angularUnit);
            return opFactory.createDefiningConversion(parseMetadataAndClose(element, classification), method, parameters);
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
     * @param  mode {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @param  meridian the prime meridian, or {@code null} for Greenwich.
     * @return The {@code "DATUM"} element as a {@link GeodeticDatum} object.
     * @throws ParseException if the {@code "DATUM"} element can not be parsed.
     */
    private GeodeticDatum parseDatum(final int mode, final Element parent, PrimeMeridian meridian) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.Datum);
        if (element == null) {
            return null;
        }
        final String             name       = element.pullString("name");
        final Ellipsoid          ellipsoid  = parseEllipsoid(MANDATORY, element);
        final Object             toWGS84    = parseToWGS84(OPTIONAL, element);
        final Map<String,Object> properties = parseMetadataAndClose(element, name);
        if (meridian == null) {
            meridian = referencing.getGreenwich();
        }
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
     * @param  mode {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @return The {@code "VERT_DATUM"} element as a {@link VerticalDatum} object.
     * @throws ParseException if the {@code "VERT_DATUM"} element can not be parsed.
     */
    private VerticalDatum parseVerticalDatum(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.VerticalDatum, WKTKeywords.Vert_Datum);
        if (element == null) {
            return null;
        }
        final String  name  = element.pullString ("name");
        final int     datum = element.pullInteger("datum");
        final VerticalDatumType type = VerticalDatumTypes.fromLegacy(datum);
        // TODO: need to find a default value if null.
        try {
            return datumFactory.createVerticalDatum(parseMetadataAndClose(element, name), type);
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
     * @param  mode {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @return The {@code "TimeDatum"} element as a {@link TemporalDatum} object.
     * @throws ParseException if the {@code "TimeDatum"} element can not be parsed.
     */
    private TemporalDatum parseTimeDatum(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.TimeDatum);
        if (element == null) {
            return null;
        }
        final String  name   = element.pullString ("name");
        final Element origin = element.pullElement(MANDATORY, WKTKeywords.TimeOrigin);
        final Date    epoch  = origin .pullDate("origin");
        origin.close(ignoredElements);
        try {
            return datumFactory.createTemporalDatum(parseMetadataAndClose(element, name), epoch);
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
     * @param  mode {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @return The {@code "LOCAL_DATUM"} element as an {@link EngineeringDatum} object.
     * @throws ParseException if the {@code "LOCAL_DATUM"} element can not be parsed.
     *
     * @todo The vertical datum type is currently ignored.
     */
    private EngineeringDatum parseEngineeringDatum(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.EngineeringDatum, WKTKeywords.Local_Datum);
        if (element == null) {
            return null;
        }
        final String name  = element.pullString ("name");
        final int    datum = element.pullInteger("datum");   // Ignored for now.
        try {
            return datumFactory.createEngineeringDatum(parseMetadataAndClose(element, name));
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
     * @param  mode {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @return The {@code "LOCAL_CS"} element as an {@link EngineeringCRS} object.
     * @throws ParseException if the {@code "LOCAL_CS"} element can not be parsed.
     */
    private EngineeringCRS parseEngineeringCRS(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.EngineeringCRS, WKTKeywords.Local_CS);
        if (element == null) {
            return null;
        }
        final String           name  = element.pullString("name");
        final EngineeringDatum datum = parseEngineeringDatum(MANDATORY, element);
        final Unit<?>          unit  = parseUnit(element);
        try {
            final CoordinateSystem cs = parseCoordinateSystem(element, null, 1, unit, datum);
            return crsFactory.createEngineeringCRS(parseMetadataAndClose(element, name), datum, cs);
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
     * @param  mode {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @return The {@code "GEOCCS"} element as a {@link GeocentricCRS} object.
     * @throws ParseException if the {@code "GEOCCS"} element can not be parsed.
     */
    private GeocentricCRS parseGeocentricCRS(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.GeocCS);
        if (element == null) {
            return null;
        }
        final String        name       = element.pullString("name");
        final PrimeMeridian meridian   = parsePrimeMeridian(MANDATORY, element, NonSI.DEGREE_ANGLE);
        final GeodeticDatum datum      = parseDatum(MANDATORY, element, meridian);
        final Unit<Length>  linearUnit = parseDerivedUnit(element, WKTKeywords.LengthUnit, SI.METRE);
        try {
            final CoordinateSystem cs = parseCoordinateSystem(element, WKTKeywords.Cartesian, 3, linearUnit, datum);
            return crsFactory.createGeocentricCRS(parseMetadataAndClose(element, name), datum,
                    referencing.upgradeGeocentricCS((CartesianCS) cs));
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
     * @param  mode {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @return The {@code "VERT_CS"} element as a {@link VerticalCRS} object.
     * @throws ParseException if the {@code "VERT_CS"} element can not be parsed.
     */
    private VerticalCRS parseVerticalCRS(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.VerticalCRS, WKTKeywords.Vert_CS);
        if (element == null) {
            return null;
        }
        final String         name       = element.pullString("name");
        final VerticalDatum  datum      = parseVerticalDatum(MANDATORY, element);
        final Unit<Length>   linearUnit = parseDerivedUnit(element, WKTKeywords.LengthUnit, SI.METRE);
        try {
            final CoordinateSystem cs = parseCoordinateSystem(element, WKTKeywords.vertical, 1, linearUnit, datum);
            verticalCRS = crsFactory.createVerticalCRS(parseMetadataAndClose(element, name), datum, (VerticalCS) cs);
            /*
             * Some DefaultVerticalExtent objects may be waiting for the VerticalCRS before to complete
             * their construction. If this is the case, try to complete them now.
             */
            if (verticalElements != null) {
                verticalElements = verticalElements.resolve(verticalCRS);
            }
            return verticalCRS;
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses an <strong>optional</strong> {@code "TIMECRS"} element.
     *
     * @param  mode {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @return The {@code "TIMECRS"} element as a {@link TemporalCRS} object.
     * @throws ParseException if the {@code "TIMECRS"} element can not be parsed.
     */
    private TemporalCRS parseTimeCRS(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.TimeCRS);
        if (element == null) {
            return null;
        }
        final String         name     = element.pullString("name");
        final TemporalDatum  datum    = parseTimeDatum(MANDATORY, element);
        final Unit<Duration> timeUnit = parseDerivedUnit(element, WKTKeywords.TimeUnit, SI.SECOND);
        try {
            final CoordinateSystem cs = parseCoordinateSystem(element, WKTKeywords.temporal, 1, timeUnit, datum);
            return crsFactory.createTemporalCRS(parseMetadataAndClose(element, name), datum, (TimeCS) cs);
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
     * @param  mode {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @return The {@code "GEOGCS"} element as a {@link GeographicCRS} object.
     * @throws ParseException if the {@code "GEOGCS"} element can not be parsed.
     */
    private GeographicCRS parseGeographicCRS(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.GeodeticCRS, WKTKeywords.GeodCRS, WKTKeywords.GeogCS);
        if (element == null) {
            return null;
        }
        Object              name        = element.pullString("name");
        final Unit<Angle>   angularUnit = parseDerivedUnit(element, WKTKeywords.AngleUnit, SI.RADIAN);
        final PrimeMeridian meridian    = parsePrimeMeridian(MANDATORY, element, angularUnit);
        final GeodeticDatum datum       = parseDatum(MANDATORY, element, meridian);
        if (((String) name).isEmpty()) {
            /*
             * GeographicCRS name is a mandatory property, but some invalid WKT with an empty string exist.
             * In such case, we will use the name of the enclosed datum. Indeed, it is not uncommon to have
             * the same name for a geographic CRS and its geodetic datum.
             */
            name = datum.getName();
        }
        try {
            final CoordinateSystem cs = parseCoordinateSystem(element, WKTKeywords.ellipsoidal, 2, angularUnit, datum);
            return crsFactory.createGeographicCRS(parseMetadataAndClose(element, name), datum, (EllipsoidalCS) cs);
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
     * @param  mode {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @return The {@code "PROJCS"} element as a {@link ProjectedCRS} object.
     * @throws ParseException if the {@code "GEOGCS"} element can not be parsed.
     */
    private ProjectedCRS parseProjectedCRS(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.ProjCS);
        if (element == null) {
            return null;
        }
        final String        name       = element.pullString("name");
        final GeographicCRS geoCRS     = parseGeographicCRS(MANDATORY, element);
        final Unit<Length>  linearUnit = parseDerivedUnit(element, WKTKeywords.LengthUnit, SI.METRE);
        final boolean  usesCommonUnits = convention.usesCommonUnits;
        final Conversion    conversion = parseProjection(MANDATORY, element, usesCommonUnits ? SI.METRE : linearUnit,
                usesCommonUnits ? NonSI.DEGREE_ANGLE : geoCRS.getCoordinateSystem().getAxis(0).getUnit().asType(Angle.class));
        try {
            final CoordinateSystem cs = parseCoordinateSystem(element, WKTKeywords.Cartesian, 2, linearUnit, geoCRS.getDatum());
            return crsFactory.createProjectedCRS(parseMetadataAndClose(element, name), geoCRS, conversion, (CartesianCS) cs);
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
     * @param  mode {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @return The {@code "COMPD_CS"} element as a {@link CompoundCRS} object.
     * @throws ParseException if the {@code "COMPD_CS"} element can not be parsed.
     */
    private CompoundCRS parseCompoundCRS(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.CompoundCRS, WKTKeywords.Compd_CS);
        if (element == null) {
            return null;
        }
        final String  name = element.pullString("name");
        CoordinateReferenceSystem crs;
        final List<CoordinateReferenceSystem> components = new ArrayList<>(4);
        while ((crs = parseCoordinateReferenceSystem(element, components.size() < 2)) != null) {
            components.add(crs);
        }
        try {
            return crsFactory.createCompoundCRS(parseMetadataAndClose(element, name),
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
     * @param  mode {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @return The {@code "FITTED_CS"} element as a {@link CompoundCRS} object.
     * @throws ParseException if the {@code "COMPD_CS"} element can not be parsed.
     */
    private DerivedCRS parseFittedCS(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.Fitted_CS);
        if (element == null) {
            return null;
        }
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
                        singletonMap(CoordinateSystemAxis.NAME_KEY, buffer.toString()),
                        number, AxisDirection.OTHER, Unit.ONE);
            }
            final Map<String,Object> properties = parseMetadataAndClose(element, name);
            final CoordinateSystem derivedCS = referencing.createAbstractCS(
                    singletonMap(CoordinateSystem.NAME_KEY, AxisDirections.appendTo(new StringBuilder("CS"), axes)), axes);
            /*
             * We do not know which name to give to the conversion method.
             * For now, use the CRS name.
             */
            properties.put("conversion.name", name);
            return referencing.createDerivedCRS(properties, (SingleCRS) baseCRS, method, toBase.inverse(), derivedCS);
        } catch (FactoryException | NoninvertibleTransformException exception) {
            throw element.parseFailed(exception);
        }
    }
}
