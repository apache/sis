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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;
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
import javax.measure.converter.ConversionException;

import org.opengis.util.Factory;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
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
import org.apache.sis.internal.metadata.TransformationAccuracy;
import org.apache.sis.internal.util.LocalizedParseException;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.iso.DefaultNameSpace;
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
 * @author  Johann Sorel (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
class GeodeticObjectParser extends MathTransformParser implements Comparator<CoordinateSystemAxis> {
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
     * During WKT 1 parsing, {@code true} means that {@code PRIMEM} and {@code PARAMETER} angular units
     * need to be forced to {@code NonSI.DEGREE_ANGLE} instead than inferred from the context.
     * Note that this rule does not apply to {@code AXIS} elements
     *
     * <p>This flag is ignored during WKT 2 parsing.</p>
     *
     * @see Convention#WKT1_COMMON_UNITS
     */
    private final boolean usesCommonUnits;

    /**
     * During WKT 1 parsing, {@code true} means that axes should be parsed only for verifying the syntax,
     * but otherwise parsing should behave as if axes were not declared.
     *
     * <p>This flag is ignored during WKT 2 parsing.</p>
     *
     * @see Convention#WKT1_IGNORE_AXES
     */
    private final boolean ignoreAxes;

    /**
     * The object to use for replacing WKT axis names and abbreviations by ISO 19111 names and abbreviations.
     */
    private final Transliterator transliterator;

    /**
     * A map of properties to be given to the factory constructor methods.
     * This map will be recycled for each object to be parsed.
     */
    private final Map<String,Object> properties = new HashMap<String,Object>(4);

    /**
     * Order of coordinate system axes. Used only if {@code AXIS[…]} elements contain {@code ORDER[…]} sub-element.
     */
    private final Map<CoordinateSystemAxis,Integer> axisOrder = new IdentityHashMap<CoordinateSystemAxis,Integer>(4);

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
        super(Symbols.getDefault(), Collections.<String,Element>emptyMap(), null, null, null,
                mtFactory, (Locale) defaultProperties.get(Errors.LOCALE_KEY));
        crsFactory      = (CRSFactory)   factories;
        csFactory       = (CSFactory)    factories;
        datumFactory    = (DatumFactory) factories;
        referencing     = ReferencingServices.getInstance();
        opFactory       = referencing.getCoordinateOperationFactory(defaultProperties, mtFactory, crsFactory, csFactory);
        transliterator  = Transliterator.DEFAULT;
        usesCommonUnits = false;
        ignoreAxes      = false;
    }

    /**
     * Constructs a parser for the specified set of symbols using the specified set of factories.
     * This constructor is for {@link WKTFormat} usage only.
     *
     * @param symbols       The set of symbols to use.
     * @param fragments     Reference to the {@link WKTFormat#fragments} map, or an empty map if none.
     * @param numberFormat  The number format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param dateFormat    The date format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param unitFormat    The unit format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param convention    The WKT convention to use.
     * @param errorLocale   The locale for error messages (not for parsing), or {@code null} for the system default.
     * @param factories     On input, the factories to use. On output, the factories used. Can be null.
     */
    GeodeticObjectParser(final Symbols symbols, final Map<String,Element> fragments,
            final NumberFormat numberFormat, final DateFormat dateFormat, final UnitFormat unitFormat,
            final Convention convention, final Transliterator transliterator, final Locale errorLocale,
            final Map<Class<?>,Factory> factories)
    {
        super(symbols, fragments, numberFormat, dateFormat, unitFormat,
                getFactory(MathTransformFactory.class, factories), errorLocale);
        this.transliterator = transliterator;
        crsFactory      = getFactory(CRSFactory.class,   factories);
        csFactory       = getFactory(CSFactory.class,    factories);
        datumFactory    = getFactory(DatumFactory.class, factories);
        referencing     = ReferencingServices.getInstance();
        usesCommonUnits = convention.usesCommonUnits;
        ignoreAxes      = convention == Convention.WKT1_IGNORE_AXES;
        final Factory f = factories.get(CoordinateOperationFactory.class);
        if (f != null) {
            opFactory = (CoordinateOperationFactory) f;
        } else {
            opFactory = referencing.getCoordinateOperationFactory(null, mtFactory, crsFactory, csFactory);
            factories.put(CoordinateOperationFactory.class, opFactory);
        }
    }

    /**
     * Returns the factory of the given type. This method fetches the factory from the given map.
     * The factory actually used is stored in the map.
     */
    static <T extends Factory> T getFactory(final Class<T> type, final Map<Class<?>,Factory> factories) {
        T factory = type.cast(factories.get(type));
        if (factory == null) {
            factory = DefaultFactories.forBuildin(type);
            factories.put(type, factory);
        }
        return factory;
    }

    /**
     * Returns the name of the class providing the publicly-accessible {@code createFromWKT(String)} method.
     * This information is used for logging purpose only.
     */
    @Override
    String getPublicFacade() {
        return "org.apache.sis.referencing.factory.GeodeticObjectFactory";
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
    public final Object parseObject(final String text, final ParsePosition position) throws ParseException {
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
                }
                if (verticalElements != null) {
                    warning(null, (String) null, Errors.formatInternational(Errors.Keys.CanNotAssignUnitToDimension_2,
                            WKTKeywords.VerticalExtent, verticalElements.unit), ex);
                }
            }
        } finally {
            verticalElements = null;
            verticalCRS = null;
            axisOrder.clear();
            properties.clear();                             // for letting the garbage collector do its work.
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
    final Object parseObject(final Element element) throws ParseException {
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
            (object = parsePrimeMeridian    (FIRST, element, false, NonSI.DEGREE_ANGLE)) == null &&
            (object = parseDatum            (FIRST, element, null )) == null &&
            (object = parseEllipsoid        (FIRST, element       )) == null &&
            (object = parseToWGS84          (FIRST, element       )) == null &&
            (object = parseVerticalDatum    (FIRST, element, false)) == null &&
            (object = parseTimeDatum        (FIRST, element       )) == null &&
            (object = parseParametricDatum  (FIRST, element       )) == null &&
            (object = parseEngineeringDatum (FIRST, element, false)) == null &&
            (object = parseImageDatum       (FIRST, element       )) == null &&
            (object = parseOperation        (FIRST, element))        == null)
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
        if ((crs = parseGeodeticCRS    (FIRST, element, 2, null)) == null &&
            (crs = parseProjectedCRS   (FIRST, element, false))   == null &&
            (crs = parseVerticalCRS    (FIRST, element, false))   == null &&
            (crs = parseTimeCRS        (FIRST, element, false))   == null &&
            (crs = parseParametricCRS  (FIRST, element, false))   == null &&
            (crs = parseEngineeringCRS (FIRST, element, false))   == null &&
            (crs = parseImageCRS       (FIRST, element))          == null &&
            (crs = parseCompoundCRS    (FIRST, element))          == null &&
            (crs = parseFittedCS       (FIRST, element))          == null)
        {
            if (mandatory) {
                throw element.missingOrUnknownComponent(WKTKeywords.GeodeticCRS);
            }
        }
        return crs;
    }

    /**
     * Parses a coordinate reference system wrapped in an element of the given name.
     *
     * @param  parent   The parent element containing the CRS to parse.
     * @param  mode     {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  keyword  "SourceCRS", "TargetCRS" or "InterpolationCRS".
     * @return The coordinate reference system, or {@code null} if none.
     * @throws ParseException if the CRS can not be parsed.
     */
    private CoordinateReferenceSystem parseCoordinateReferenceSystem(final Element parent, final int mode,
            final String keyword) throws ParseException
    {
        final Element element = parent.pullElement(mode, keyword);
        if (element == null) {
            return null;
        }
        final CoordinateReferenceSystem crs = parseCoordinateReferenceSystem(element, true);
        element.close(ignoredElements);
        return crs;
    }

    /**
     * Returns the value associated to {@link IdentifiedObject#IDENTIFIERS_KEY} as an {@code Identifier} object.
     * This method shall accept all value types that {@link #parseMetadataAndClose(Element, Object)} may store.
     *
     * @param  identifier The {@link #properties} value, or {@code null}.
     * @return The identifier, or {@code null} if the given value was null.
     */
    private static Identifier toIdentifier(final Object identifier) {
        return (identifier instanceof Identifier[]) ? ((Identifier[]) identifier)[0] : (Identifier) identifier;
    }

    /**
     * Parses an <strong>optional</strong> metadata elements and close.
     * This include elements like {@code "SCOPE"}, {@code "ID"} (WKT 2) or {@code "AUTHORITY"} (WKT 1).
     * This WKT 1 element has the following pattern:
     *
     * {@preformat wkt
     *     AUTHORITY["<name>", "<code>"]
     * }
     *
     * <div class="section">Fallback</div>
     * The name is a mandatory property, but some invalid WKT with an empty string exist. In such case,
     * we will use the name of the enclosed datum. Indeed, it is not uncommon to have the same name for
     * a geographic CRS and its geodetic datum.
     *
     * @param  parent   The parent element.
     * @param  name     The name of the parent object being parsed.
     * @param  fallback The fallback to use if {@code name} is empty.
     * @return A properties map with the parent name and the optional authority code.
     * @throws ParseException if an element can not be parsed.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private Map<String,Object> parseMetadataAndClose(final Element parent, final String name,
            final IdentifiedObject fallback) throws ParseException
    {
        properties.clear();
        properties.put(IdentifiedObject.NAME_KEY, (name.isEmpty() && fallback != null) ? fallback.getName() : name);
        Element element;
        while ((element = parent.pullElement(OPTIONAL, ID_KEYWORDS)) != null) {
            final String   codeSpace = element.pullString("codeSpace");
            final String   code      = element.pullObject("code").toString();       // Accepts Integer as well as String.
            final Object   version   = element.pullOptional(Object.class);          // Accepts Number as well as String.
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
                // REMINDER: values associated to IDENTIFIERS_KEY shall be recognized by 'toIdentifier(Object)'.
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
                if (extent == null) {
                    extent = new DefaultExtent(area, null, null, null);
                } else {
                    extent.getGeographicElements().add(new DefaultGeographicDescription(area));
                }
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
                Unit<Length> unit = parseScaledUnit(element, WKTKeywords.LengthUnit, SI.METRE);
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
                if (element.peekValue() instanceof String) {
                    element.pullString("startTime");
                    element.pullString("endTime");
                    element.close(ignoredElements);
                    warning(parent, element, Errors.formatInternational(Errors.Keys.UnsupportedType_1, "TimeExtent[String,String]"), null);
                } else {
                    final Date startTime = element.pullDate("startTime");
                    final Date endTime   = element.pullDate("endTime");
                    element.close(ignoredElements);
                    try {
                        final DefaultTemporalExtent t = new DefaultTemporalExtent();
                        t.setBounds(startTime, endTime);
                        if (extent == null) extent = new DefaultExtent();
                        extent.getTemporalElements().add(t);
                    } catch (UnsupportedOperationException e) {
                        warning(parent, element, null, e);
                    }
                }
            }
            if (extent != null) {
                properties.put(ReferenceSystem.DOMAIN_OF_VALIDITY_KEY, extent);
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
     * Parses the datum {@code ANCHOR[]} element and invoke {@link #parseMetadataAndClose(Element, Object)}.
     * If an anchor has been found, its value is stored in the returned map.
     */
    private Map<String,Object> parseAnchorAndClose(final Element element, final String name) throws ParseException {
        final Element anchor = element.pullElement(OPTIONAL, WKTKeywords.Anchor);
        final Map<String,Object> properties = parseMetadataAndClose(element, name, null);
        if (anchor != null) {
            properties.put(Datum.ANCHOR_POINT_KEY, anchor.pullString("anchorDefinition"));
            anchor.close(ignoredElements);
        }
        return properties;
    }

    /**
     * Parses an optional {@code "UNIT"} element of a known dimension.
     * This element has the following pattern:
     *
     * {@preformat wkt
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
     * @todo Authority code is currently discarded after parsing. We may consider to create a subclass of
     *       {@link Unit} which implements {@link IdentifiedObject} in a future version.
     */
    @SuppressWarnings("unchecked")
    private <Q extends Quantity> Unit<Q> parseScaledUnit(final Element parent,
            final String keyword, final Unit<Q> baseUnit) throws ParseException
    {
        final Element element = parent.pullElement(OPTIONAL, keyword, WKTKeywords.Unit);
        if (element == null) {
            return null;
        }
        final String name   = element.pullString("name");
        final double factor = element.pullDouble("factor");
        Unit<Q> unit   = Units.multiply(baseUnit, factor);
        Unit<?> verify = parseUnitID(element);
        element.close(ignoredElements);
        /*
         * Consider the following element: UNIT[“km”, 1000, ID[“EPSG”, “9036”]]
         *
         *  - if the authority code (“9036”) refers to a unit incompatible with 'baseUnit' (“metre”), log a warning.
         *  - otherwise: 1) unconditionally replace the parsed unit (“km”) by the unit referenced by the authority code.
         *               2) if the new unit is not equivalent to the old one (i.e. different scale factor), log a warning.
         */
        if (verify != null) {
            if (!baseUnit.toSI().equals(verify.toSI())) {
                warning(parent, element, Errors.formatInternational(Errors.Keys.InconsistentUnitsForCS_1, verify), null);
            } else if (Math.abs(unit.getConverterTo(unit = (Unit<Q>) verify).convert(1) - 1) > Numerics.COMPARISON_THRESHOLD) {
                warning(parent, element, Errors.formatInternational(Errors.Keys.UnexpectedScaleFactorForUnit_2, verify, factor), null);
            } else {
                verify = null;                                          // Means to perform additional verifications.
            }
        }
        /*
         * Above block verified the ID[“EPSG”, “9036”] authority code. Now verify the unit parsed from the “km” symbol.
         * This is only a verification; we will not replace the unit by the parsed one (i.e. authority code or scale
         * factor have precedence over the unit symbol).
         */
        if (verify == null) {
            try {
                verify = parseUnit(name);
            } catch (IllegalArgumentException e) {
                log(new LogRecord(Level.FINE, e.toString()));
            } catch (ParseException e) {
                log(new LogRecord(Level.FINE, e.toString()));
            }
            if (verify != null) try {
                if (Math.abs(verify.getConverterToAny(unit).convert(1) - 1) > Numerics.COMPARISON_THRESHOLD) {
                    warning(parent, element, Errors.formatInternational(Errors.Keys.UnexpectedScaleFactorForUnit_2, verify, factor), null);
                }
            } catch (ConversionException e) {
                throw (ParseException) new LocalizedParseException(errorLocale,
                        Errors.Keys.InconsistentUnitsForCS_1, new Object[] {verify}, element.offset).initCause(e);
            }
        }
        return unit;
    }

    /**
     * Parses a {@code "CS"} element followed by all {@code "AXIS"} elements.
     * This element has the following pattern (simplified):
     *
     * {@preformat wkt
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
     * @param  isWKT1      {@code true} if the parent element is an element from the WKT 1 standard.
     * @param  defaultUnit The contextual unit (usually {@code SI.METRE} or {@code SI.RADIAN}), or {@code null} if unknown.
     * @param  datum       The datum of the enclosing CRS, or {@code null} if unknown.
     * @return The {@code "CS"}, {@code "UNIT"} and/or {@code "AXIS"} elements as a Coordinate System, or {@code null}.
     * @throws ParseException if an element can not be parsed.
     * @throws FactoryException if the factory can not create the coordinate system.
     */
    private CoordinateSystem parseCoordinateSystem(final Element parent, String type, int dimension,
            final boolean isWKT1, final Unit<?> defaultUnit, final Datum datum) throws ParseException, FactoryException
    {
        axisOrder.clear();
        final boolean is3D = (dimension >= 3);
        Map<String,Object> csProperties = null;
        /*
         * Parse the CS[<type>, <dimension>] element.  This is specific to the WKT 2 format.
         * In principle the CS element is mandatory, but the Apache SIS parser is lenient on
         * this aspect:  if the CS element is not present, we will compute the same defaults
         * than what we do for WKT 1.
         */
        if (!isWKT1) {
            final Element element = parent.pullElement(OPTIONAL, WKTKeywords.CS);
            if (element != null) {
                final String expected = type;
                type         = element.pullVoidElement("type").keyword;
                dimension    = element.pullInteger("dimension");
                csProperties = new HashMap<String,Object>(parseMetadataAndClose(element, "CS", null));
                if (expected != null) {
                    if (!expected.equalsIgnoreCase(type)) {
                        throw new LocalizedParseException(errorLocale, Errors.Keys.UnexpectedValueInElement_2,
                                new String[] {WKTKeywords.CS, type}, element.offset);
                    }
                }
                if (dimension <= 0 || dimension > 1000) {       // Arbitrary upper limit against badly formed CS.
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
            final List<CoordinateSystemAxis> list = new ArrayList<CoordinateSystemAxis>(dimension + 2);
            do {
                list.add(axis);
                axis = parseAxis(list.size() < dimension ? MANDATORY : OPTIONAL, parent, type, defaultUnit);
            } while (axis != null);
            if (!isWKT1 || !ignoreAxes) {
                axes = list.toArray(new CoordinateSystemAxis[list.size()]);
                Arrays.sort(axes, this);                    // Take ORDER[n] elements in account.
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
            String nx = null, x = null;                     // Easting or Longitude axis name and abbreviation.
            String ny = null, y = null;                     // Northing or latitude axis name and abbreviation.
            String nz = null, z = null;                     // Depth, height or time axis name and abbreviation.
            AxisDirection dx = AxisDirection.EAST;
            AxisDirection dy = AxisDirection.NORTH;
            AxisDirection direction = null;                 // Depth, height or time axis direction.
            Unit<?> unit = defaultUnit;                     // Depth, height or time axis unit.
            /*switch (type)*/ {
                /*
                 * Cartesian — we can create axes only for geodetic datum, in which case the axes are for
                 * two-dimensional Projected or three-dimensional Geocentric CRS.
                 */
                if (type.equals(WKTKeywords.Cartesian)) {
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
                }
                /*
                 * Ellipsoidal — can be two- or three- dimensional, in which case the height can
                 * only be ellipsoidal height. The default axis order depends on the WKT version:
                 *
                 *   - WKT 1 said explicitely that the default order is (longitude, latitude).
                 *   - WKT 2 has no default, and allows only (latitude, longitude) order.
                 */
                else if (type.equals(WKTKeywords.ellipsoidal)) {
                    if (defaultUnit == null) {
                        throw parent.missingComponent(WKTKeywords.AngleUnit);
                    }
                    if (isWKT1) {
                        nx = AxisNames.GEODETIC_LONGITUDE; x = "λ";
                        ny = AxisNames.GEODETIC_LATITUDE;  y = "φ";
                    } else {
                        nx = AxisNames.GEODETIC_LATITUDE;  x = "φ"; dx = AxisDirection.NORTH;
                        ny = AxisNames.GEODETIC_LONGITUDE; y = "λ"; dy = AxisDirection.EAST;
                    }
                    if (dimension >= 3) {
                        direction = AxisDirection.UP;
                        z    = "h";
                        nz   = AxisNames.ELLIPSOIDAL_HEIGHT;
                        unit = SI.METRE;
                    }
                }
                /*
                 * Vertical — the default name and symbol depends on whether this is depth,
                 * geoidal height, ellipsoidal height (non-standard) or other kind of heights.
                 */
                else if (type.equals(WKTKeywords.vertical)) {
                    if (defaultUnit == null) {
                        throw parent.missingComponent(WKTKeywords.Unit);
                    }
                    z         = "h";
                    nz        = "Height";
                    direction = AxisDirection.UP;
                    if (datum instanceof VerticalDatum) {
                        final VerticalDatumType vt = ((VerticalDatum) datum).getVerticalDatumType();
                        if (vt == VerticalDatumType.GEOIDAL) {
                            nz = AxisNames.GRAVITY_RELATED_HEIGHT;
                            z  = "H";
                        } else if (vt == VerticalDatumType.DEPTH) {
                            direction = AxisDirection.DOWN;
                            nz = AxisNames.DEPTH;
                            z  = "D";
                        } else if (vt == VerticalDatumTypes.ELLIPSOIDAL) {
                            // Not allowed by ISO 19111 as a standalone axis, but SIS is
                            // tolerant to this case since it is sometime hard to avoid.
                            nz = AxisNames.ELLIPSOIDAL_HEIGHT;
                        }
                    }
                }
                /*
                 * Temporal — axis name and abbreviation not yet specified by ISO 19111.
                 */
                else if (type.equals(WKTKeywords.temporal)) {
                    if (defaultUnit == null) {
                        throw parent.missingComponent(WKTKeywords.TimeUnit);
                    }
                    direction = AxisDirection.FUTURE;
                    nz = "Time";
                    z = "t";
                }
                /*
                 * Parametric — axis name and abbreviation not yet specified by ISO 19111_2.
                 */
                else if (type.equals(WKTKeywords.parametric)) {
                    if (defaultUnit == null) {
                        throw parent.missingComponent(WKTKeywords.ParametricUnit);
                    }
                    direction = AxisDirection.OTHER;
                    nz = "Parametric";
                    z = "p";
                }
                /*
                 * Unknown CS type — we can not guess which axes to create.
                 */
                else {
                    throw parent.missingComponent(WKTKeywords.Axis);
                }
            }
            int i = 0;
            axes = new CoordinateSystemAxis[dimension];
            if (x != null && i < dimension) axes[i++] = csFactory.createCoordinateSystemAxis(singletonMap(CoordinateSystemAxis.NAME_KEY, nx), x, dx,  defaultUnit);
            if (y != null && i < dimension) axes[i++] = csFactory.createCoordinateSystemAxis(singletonMap(CoordinateSystemAxis.NAME_KEY, ny), y, dy, defaultUnit);
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
            csProperties = java.util.Collections.<String,Object>singletonMap(CoordinateSystem.NAME_KEY, name);
        } else {
            csProperties.put(CoordinateSystem.NAME_KEY, name);
        }
        if (type == null) {
            return referencing.createAbstractCS(csProperties, axes);
        }
        /*
         * Finally, delegate to the factory method corresponding to the CS type and the number of axes.
         */
        /*switch (type)*/ {
            if (type.equals(WKTKeywords.ellipsoidal)) {
                switch (axes.length) {
                    case 2: return csFactory.createEllipsoidalCS(csProperties, axes[0], axes[1]);
                    case 3: return csFactory.createEllipsoidalCS(csProperties, axes[0], axes[1], axes[2]);
                }
                dimension = (axes.length < 2) ? 2 : 3;                      // For error message.
            }
            else if (type.equals(WKTKeywords.Cartesian)) {
                switch (axes.length) {
                    case 2: return csFactory.createCartesianCS(csProperties, axes[0], axes[1]);
                    case 3: return csFactory.createCartesianCS(csProperties, axes[0], axes[1], axes[2]);
                }
                dimension = (axes.length < 2) ? 2 : 3;                      // For error message.
            }
            else if (type.equals(WKTKeywords.affine)) {
                switch (axes.length) {
                    case 2: return csFactory.createAffineCS(csProperties, axes[0], axes[1]);
                    case 3: return csFactory.createAffineCS(csProperties, axes[0], axes[1], axes[2]);
                }
                dimension = (axes.length < 2) ? 2 : 3;                      // For error message.
            }
            else if (type.equals(WKTKeywords.vertical)) {
                if (axes.length == (dimension = 1)) {
                    return csFactory.createVerticalCS(csProperties, axes[0]);
                }
            }
            else if (type.equals(WKTKeywords.temporal)) {
                if (axes.length == (dimension = 1)) {
                    return csFactory.createTimeCS(csProperties, axes[0]);
                }
            }
            else if (type.equals(WKTKeywords.linear)) {
                if (axes.length == (dimension = 1)) {
                    return csFactory.createLinearCS(csProperties, axes[0]);
                }
            }
            else if (type.equals(WKTKeywords.polar)) {
                if (axes.length == (dimension = 2)) {
                    return csFactory.createPolarCS(csProperties, axes[0], axes[1]);
                }
            }
            else if (type.equals(WKTKeywords.cylindrical)) {
                if (axes.length == (dimension = 3)) {
                    return csFactory.createCylindricalCS(csProperties, axes[0], axes[1], axes[2]);
                }
            }
            else if (type.equals(WKTKeywords.spherical)) {
                if (axes.length == (dimension = 3)) {
                    return csFactory.createSphericalCS(csProperties, axes[0], axes[1], axes[2]);
                }
            }
            else if (type.equals(WKTKeywords.parametric)) {
                if (axes.length == (dimension = 1)) {
                    return referencing.createParametricCS(csProperties, axes[0], csFactory);
                }
            }
            else {
                warning(parent, WKTKeywords.CS, Errors.formatInternational(Errors.Keys.UnknownType_1, type), null);
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
     * {@preformat wkt
     *     AXIS["<name (abbr.)>", NORTH | SOUTH | EAST | WEST | UP | DOWN | OTHER, ORDER[n], UNIT[…], ID[…]]
     * }
     *
     * Abbreviation may be specified between parenthesis. Nested parenthesis are possible, as for example:
     *
     * {@preformat wkt
     *     AXIS["Easting (E(X))", EAST]
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
        String name = element.pullString("name");
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
            final Unit<Angle> m = parseScaledUnit(meridian, WKTKeywords.AngleUnit, SI.RADIAN);
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
        int start, end = name.length() - 1;
        if (end > 1 && name.charAt(end) == ')' && (start = name.lastIndexOf('(', end-1)) >= 0) {
            // Abbreviation may have nested parenthesis (e.g. "Easting (E(X))").
            for (int np = end; (--np >= 0) && name.charAt(np) == ')';) {
                final int c = name.lastIndexOf('(', start - 1);
                if (c < 0) {
                    warning(parent, element, Errors.formatInternational(
                            Errors.Keys.NonEquilibratedParenthesis_2, '(', name), null);
                    break;
                }
                start = c;
            }
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
            axis = csFactory.createCoordinateSystemAxis(parseMetadataAndClose(element, name, null), abbreviation, direction, unit);
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
    public final int compare(final CoordinateSystemAxis o1, final CoordinateSystemAxis o2) {
        final Integer n1 = axisOrder.get(o1);
        final Integer n2 = axisOrder.get(o2);
        if (n1 != null) {
            if (n2 != null) {
                return n1 - n2;
            }
            return -1;                      // Axis 1 before Axis 2 since the later has no 'ORDER' element.
        } else if (n2 != null) {
            return +1;                      // Axis 2 before Axis 1 since the later has no 'ORDER' element.
        }
        return 0;
    }

    /**
     * Parses a {@code "PrimeMeridian"} element. The syntax is given by
     * <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#53">WKT 2 specification §8.2.2</a>.
     *
     * The legacy WKT 1 pattern was:
     *
     * {@preformat wkt
     *     PRIMEM["<name>", <longitude> {,<authority>}]
     * }
     *
     * @param  mode        {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent      The parent element.
     * @param  isWKT1      {@code true} if this method is invoked while parsing a WKT 1 element.
     * @param  angularUnit The contextual unit.
     * @return The {@code "PrimeMeridian"} element as a {@link PrimeMeridian} object.
     * @throws ParseException if the {@code "PrimeMeridian"} element can not be parsed.
     *
     * @see org.apache.sis.referencing.datum.DefaultPrimeMeridian#formatTo(Formatter)
     */
    private PrimeMeridian parsePrimeMeridian(final int mode, final Element parent, final boolean isWKT1, Unit<Angle> angularUnit)
            throws ParseException
    {
        if (isWKT1 && usesCommonUnits) {
            angularUnit = NonSI.DEGREE_ANGLE;
        }
        final Element element = parent.pullElement(mode, WKTKeywords.PrimeMeridian, WKTKeywords.PrimeM);
        if (element == null) {
            return null;
        }
        final String name      = element.pullString("name");
        final double longitude = element.pullDouble("longitude");
        final Unit<Angle> unit = parseScaledUnit(element, WKTKeywords.AngleUnit, SI.RADIAN);
        if (unit != null) {
            angularUnit = unit;
        } else if (angularUnit == null) {
            throw parent.missingComponent(WKTKeywords.AngleUnit);
        }
        try {
            return datumFactory.createPrimeMeridian(parseMetadataAndClose(element, name, null), longitude, angularUnit);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses an <strong>optional</strong> {@code "TOWGS84"} element.
     * This element is specific to WKT 1 and has the following pattern:
     *
     * {@preformat wkt
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
                break;                                              // It is legal to have only 3 or 6 elements.
            }
        }
        element.close(ignoredElements);
        return referencing.createToWGS84(values);
    }

    /**
     * Parses an {@code "Ellipsoid"} element. The syntax is given by
     * <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#52">WKT 2 specification §8.2.1</a>.
     *
     * The legacy WKT 1 pattern was:
     *
     * {@preformat wkt
     *     SPHEROID["<name>", <semi-major axis>, <inverse flattening> {,<authority>}]
     * }
     *
     * @param  mode {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @return The {@code "Ellipsoid"} element as an {@link Ellipsoid} object.
     * @throws ParseException if the {@code "Ellipsoid"} element can not be parsed.
     *
     * @see org.apache.sis.referencing.datum.DefaultEllipsoid#formatTo(Formatter)
     */
    private Ellipsoid parseEllipsoid(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.Ellipsoid, WKTKeywords.Spheroid);
        if (element == null) {
            return null;
        }
        final String name          = element.pullString("name");
        final double semiMajorAxis = element.pullDouble("semiMajorAxis");
        double inverseFlattening   = element.pullDouble("inverseFlattening");
        Unit<Length> unit = parseScaledUnit(element, WKTKeywords.LengthUnit, SI.METRE);
        if (unit == null) {
            unit = SI.METRE;
        }
        final Map<String,?> properties = parseMetadataAndClose(element, name, null);
        try {
            if (inverseFlattening == 0) {                           // OGC convention for a sphere.
                return datumFactory.createEllipsoid(properties, semiMajorAxis, semiMajorAxis, unit);
            } else {
                return datumFactory.createFlattenedSphere(properties, semiMajorAxis, inverseFlattening, unit);
            }
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Returns the number of source dimensions of the given operation method, or 2 if unspecified.
     */
    private static int getSourceDimensions(final OperationMethod method) {
        if (method != null) {
            final Integer dimension = method.getSourceDimensions();
            if (dimension != null) {
                return dimension;
            }
        }
        return 2;
    }

    /**
     * Parses a {@code "Method"} (WKT 2) element, without the parameters.
     *
     * @param  parent   The parent element.
     * @param  keywords The element keywords.
     * @return The operation method.
     * @throws ParseException if the {@code "Method"} element can not be parsed.
     */
    private OperationMethod parseMethod(final Element parent, final String... keywords) throws ParseException {
        final Element element    = parent.pullElement(MANDATORY, keywords);
        final String  name       = element.pullString("method");
        Map<String,?> properties = parseMetadataAndClose(element, name, null);
        final Identifier id      = toIdentifier(properties.remove(IdentifiedObject.IDENTIFIERS_KEY));  // See NOTE 2 in parseDerivingConversion.
        /*
         * The map projection method may be specified by an EPSG identifier (or any other authority),
         * which is preferred to the method name since the later is potentially ambiguous. However not
         * all CoordinateOperationFactory may accept identifier as an argument to 'getOperationMethod'.
         * So if an identifier is present, we will try to use it but fallback on the name if we can
         * not use the identifier.
         */
        FactoryException suppressed = null;
        if (id instanceof ReferenceIdentifier) try {
            // CodeSpace is a mandatory attribute in ID[…] elements, so we do not test for null values.
            return referencing.getOperationMethod(opFactory, mtFactory,
                    ((ReferenceIdentifier) id).getCodeSpace() + DefaultNameSpace.DEFAULT_SEPARATOR + id.getCode());
        } catch (FactoryException e) {
            suppressed = e;
        }
        try {
            return referencing.getOperationMethod(opFactory, mtFactory, name);
        } catch (FactoryException e) {
            if (suppressed != null) {
//              e.addSuppressed(suppressed);    // Not available on JDK6.
            }
            throw element.parseFailed(e);
        }
    }

    /**
     * Parses a {@code "Method"} (WKT 2) element, followed by parameter values. The syntax is given by
     * <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#62">WKT 2 specification §9.3</a>.
     *
     * The legacy WKT 1 specification was:
     *
     * {@preformat wkt
     *     PROJECTION["<name>" {,<authority>}]
     * }
     *
     * Note that in WKT 2, this element is wrapped inside a {@code Conversion} or {@code DerivingConversion}
     * element which is itself inside the {@code ProjectedCRS} element. This is different than WKT 1, which
     * puts this element right into the the {@code ProjectedCRS} element without {@code Conversion} wrapper.
     *
     * @param  mode               {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent             The parent element.
     * @param  wrapper            "Conversion" or "DerivingConversion" wrapper name, or null if parsing a WKT 1.
     * @param  defaultUnit        The unit (usually linear) of the parent element, or {@code null}.
     * @param  defaultAngularUnit The angular unit of the sibling {@code GeographicCRS} element, or {@code null}.
     * @return The {@code "Method"} element and its parameters as a defining conversion.
     * @throws ParseException if the {@code "Method"} element can not be parsed.
     */
    private Conversion parseDerivingConversion(final int mode, Element parent, final String wrapper,
            final Unit<?> defaultUnit, final Unit<Angle> defaultAngularUnit) throws ParseException
    {
        final String name;
        if (wrapper == null) {
            name = null;  // Will actually be ignored. WKT 1 does not provide name for Conversion objects.
        } else {
            /*
             * If we are parsing WKT 2, then there is an additional "Conversion" element between
             * the parent (usually a ProjectedCRS) and the other elements parsed by this method.
             */
            parent = parent.pullElement(mode, wrapper);
            if (parent == null) {
                return null;
            }
            name = parent.pullString("name");
        }
        final OperationMethod method = parseMethod(parent, WKTKeywords.Method, WKTKeywords.Projection);
        Map<String,?> properties = this.properties;  // Same properties then OperationMethod, with ID removed.
        /*
         * Set the list of parameters.
         *
         * NOTE 1: Parameters are defined in the parent element (usually a "ProjectedCRS" element
         *         in WKT 1 or a "Conversion" element in WKT 2), not in this "Method" element.
         *
         * NOTE 2: We may inherit the OperationMethod name if there is no Conversion wrapper with its own name,
         *         but we shall not inherit the OperationMethod identifier. This is the reason why we invoked
         *         properties.remove(IdentifiedObject.IDENTIFIERS_KEY)) above.
         */
        final ParameterValueGroup parameters = method.getParameters().createValue();
        parseParameters(parent, parameters, defaultUnit, defaultAngularUnit);
        if (wrapper != null) {
            properties = parseMetadataAndClose(parent, name, method);
            /*
             * DEPARTURE FROM ISO 19162: the specification in §9.3.2 said:
             *
             *     "If an identifier is provided as an attribute within the <map projection conversion> object,
             *     because it is expected to describe a complete collection of zone name, method, parameters and
             *     parameter values, it shall override any identifiers given within the map projection method and
             *     map projection parameter objects."
             *
             * However this would require this GeodeticObjectParser to hold a CoordinateOperationAuthorityFactory,
             * which we do not yet implement. See https://issues.apache.org/jira/browse/SIS-210
             */
        }
        try {
            return opFactory.createDefiningConversion(properties, method, parameters);
        } catch (FactoryException exception) {
            throw parent.parseFailed(exception);
        }
    }

    /**
     * Parses a {@code "Datum"} (WKT 2) element. The syntax is given by
     * <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#54">WKT 2 specification §8.2.4</a>.
     *
     * The legacy WKT 1 pattern was:
     *
     * {@preformat wkt
     *     DATUM["<name>", <spheroid> {,<to wgs84>} {,<authority>}]
     * }
     *
     * @param  mode {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @param  meridian the prime meridian, or {@code null} for Greenwich.
     * @return The {@code "Datum"} element as a {@link GeodeticDatum} object.
     * @throws ParseException if the {@code "Datum"} element can not be parsed.
     *
     * @see org.apache.sis.referencing.datum.DefaultGeodeticDatum#formatTo(Formatter)
     */
    private GeodeticDatum parseDatum(final int mode, final Element parent, PrimeMeridian meridian) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.Datum, WKTKeywords.GeodeticDatum);
        if (element == null) {
            return null;
        }
        final String             name       = element.pullString("name");
        final Ellipsoid          ellipsoid  = parseEllipsoid(MANDATORY, element);
        final Object             toWGS84    = parseToWGS84(OPTIONAL, element);
        final Map<String,Object> properties = parseAnchorAndClose(element, name);
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
     * Parses a {@code "VerticalDatum"} (WKT 2) element. The syntax is given by
     * <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#71">WKT 2 specification §10.2</a>.
     *
     * The legacy WKT 1 pattern was:
     *
     * {@preformat wkt
     *     VERT_DATUM["<name>", <datum type> {,<authority>}]
     * }
     *
     * @param  mode   {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @param  isWKT1 {@code true} if the parent is a WKT 1 element.
     * @return The {@code "VerticalDatum"} element as a {@link VerticalDatum} object.
     * @throws ParseException if the {@code "VerticalDatum"} element can not be parsed.
     */
    private VerticalDatum parseVerticalDatum(final int mode, final Element parent, final boolean isWKT1)
            throws ParseException
    {
        final Element element = parent.pullElement(mode,
                WKTKeywords.VerticalDatum,
                WKTKeywords.VDatum,
                WKTKeywords.Vert_Datum);
        if (element == null) {
            return null;
        }
        final String name = element.pullString("name");
        VerticalDatumType type = null;
        if (isWKT1) {
            type = VerticalDatumTypes.fromLegacy(element.pullInteger("datum"));
        }
        if (type == null) {
            type = VerticalDatumTypes.guess(name, null, null);
        }
        try {
            return datumFactory.createVerticalDatum(parseAnchorAndClose(element, name), type);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses a {@code "TimeDatum"} element. This element has the following pattern:
     *
     * {@preformat wkt
     *     TimeDatum["<name>", TimeOrigin[<time origin>] {,<authority>}]
     * }
     *
     * @param  mode {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @return The {@code "TimeDatum"} element as a {@link TemporalDatum} object.
     * @throws ParseException if the {@code "TimeDatum"} element can not be parsed.
     */
    private TemporalDatum parseTimeDatum(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.TimeDatum, WKTKeywords.TDatum);
        if (element == null) {
            return null;
        }
        final String  name   = element.pullString ("name");
        final Element origin = element.pullElement(MANDATORY, WKTKeywords.TimeOrigin);
        final Date    epoch  = origin .pullDate("origin");
        origin.close(ignoredElements);
        try {
            return datumFactory.createTemporalDatum(parseAnchorAndClose(element, name), epoch);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses a {@code "ParametricDatum"} element. This element has the following pattern:
     *
     * {@preformat wkt
     *     ParametricDatum["<name>", Anchor[...] {,<authority>}]
     * }
     *
     * @param  mode {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @return The {@code "ParametricDatum"} element as a {@link ParametricDatum} object.
     * @throws ParseException if the {@code "ParametricDatum"} element can not be parsed.
     */
    private Datum parseParametricDatum(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.ParametricDatum, WKTKeywords.PDatum);
        if (element == null) {
            return null;
        }
        final String name = element.pullString("name");
        try {
            return referencing.createParametricDatum(parseAnchorAndClose(element, name), datumFactory);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses a {@code "EngineeringDatum"} (WKT 2) element. The syntax is given by
     * <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#76">WKT 2 specification §11.2</a>.
     *
     * The legacy WKT 1 pattern was:
     *
     * {@preformat wkt
     *     LOCAL_DATUM["<name>", <datum type> {,<authority>}]
     * }
     *
     * The datum type (WKT 1 only) is currently ignored.
     *
     * @param  mode   {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @param  isWKT1 {@code true} if the parent is a WKT 1 element.
     * @return The {@code "EngineeringDatum"} element as an {@link EngineeringDatum} object.
     * @throws ParseException if the {@code "EngineeringDatum"} element can not be parsed.
     */
    private EngineeringDatum parseEngineeringDatum(final int mode, final Element parent, final boolean isWKT1) throws ParseException {
        final Element element = parent.pullElement(mode,
                WKTKeywords.EngineeringDatum,
                WKTKeywords.EDatum,
                WKTKeywords.Local_Datum);
        if (element == null) {
            return null;
        }
        final String name = element.pullString("name");
        if (isWKT1) {
            element.pullInteger("datum");                                       // Ignored for now.
        }
        try {
            return datumFactory.createEngineeringDatum(parseAnchorAndClose(element, name));
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses an {@code "ImageDatum"} (WKT 2) element. The syntax is given by
     * <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#81">WKT 2 specification §12.2</a>.
     *
     * @param  mode   {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @return The {@code "ImageDatum"} element as an {@link ImageDatum} object.
     * @throws ParseException if the {@code "ImageDatum"} element can not be parsed.
     */
    private ImageDatum parseImageDatum(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.ImageDatum, WKTKeywords.IDatum);
        if (element == null) {
            return null;
        }
        final String name = element.pullString("name");
        final PixelInCell pixelInCell = Types.forCodeName(PixelInCell.class,
                element.pullVoidElement("pixelInCell").keyword, true);
        try {
            return datumFactory.createImageDatum(parseAnchorAndClose(element, name), pixelInCell);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses a {@code "EngineeringCRS"} (WKT 2) element. The syntax is given by
     * <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#74">WKT 2 specification §11</a>.
     *
     * The legacy WKT 1 pattern was:
     *
     * {@preformat wkt
     *     LOCAL_CS["<name>", <local datum>, <unit>, <axis>, {,<axis>}* {,<authority>}]
     * }
     *
     * @param  mode      {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent    The parent element.
     * @param  isBaseCRS {@code true} if parsing the CRS inside a {@code DerivedCRS}.
     * @return The {@code "EngineeringCRS"} element as an {@link EngineeringCRS} object.
     * @throws ParseException if the {@code "EngineeringCRS"} element can not be parsed.
     */
    private SingleCRS parseEngineeringCRS(final int mode, final Element parent, final boolean isBaseCRS)
            throws ParseException
    {
        final Element element = parent.pullElement(mode,
                isBaseCRS ? new String[] {WKTKeywords.BaseEngCRS}               // WKT 2 in DerivedCRS
                          : new String[] {WKTKeywords.EngineeringCRS,           // [0]  WKT 2
                                          WKTKeywords.EngCRS,                   // [1]  WKT 2
                                          WKTKeywords.Local_CS});               // [2]  WKT 1
        if (element == null) {
            return null;
        }
        final boolean isWKT1 = element.getKeywordIndex() == 2;                  // Index of "Local_CS" above.
        final String  name   = element.pullString("name");
        final Unit<?> unit   = parseUnit(element);
        /*
         * An EngineeringCRS can be either a "normal" one (with a non-null datum), or a DerivedCRS.
         * In the later case, the datum is null and we have instead DerivingConversion element from a base CRS.
         */
        EngineeringDatum datum    = null;
        SingleCRS        baseCRS  = null;
        Conversion       fromBase = null;
        if (!isWKT1 && !isBaseCRS) {
            /*
             * UNIT[…] in DerivedCRS parameters are mandatory according ISO 19162 and the specification does not said
             * what to do if they are missing.  In this code, we default to the contextual units in the same way than
             * what we do for ProjectedCRS parameters, in the hope to be consistent.
             *
             * An alternative would be to specify null units, in which case MathTransformParser.parseParameters(…)
             * defaults to the units specified in the parameter descriptor. But this would make the CRS parser more
             * implementation-dependent, because the parameter descriptors are provided by the MathTransformFactory
             * instead than inferred from the WKT.
             */
            fromBase = parseDerivingConversion(OPTIONAL, element, WKTKeywords.DerivingConversion, unit, null);
            if (fromBase != null) {
                /*
                 * The order of base types below is arbitrary. But no matter their type,
                 * they must be optional except the last one which should be mandatory.
                 * The last one determines the error message to be reported if we find none.
                 */
                baseCRS = parseEngineeringCRS(OPTIONAL, element, true);
                if (baseCRS == null) {
                    baseCRS = parseGeodeticCRS(OPTIONAL, element, getSourceDimensions(fromBase.getMethod()), WKTKeywords.ellipsoidal);
                    if (baseCRS == null) {
                        baseCRS = parseProjectedCRS(MANDATORY, element, true);
                    }
                }
            }
        }
        if (baseCRS == null) {                                                  // The most usual case.
            datum = parseEngineeringDatum(MANDATORY, element, isWKT1);
        }
        try {
            final CoordinateSystem cs = parseCoordinateSystem(element, null, 1, isWKT1, unit, datum);
            final Map<String,?> properties = parseMetadataAndClose(element, name, datum);
            if (baseCRS != null) {
                return crsFactory.createDerivedCRS(properties, baseCRS, fromBase, cs);
            }
            return crsFactory.createEngineeringCRS(properties, datum, cs);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses an {@code "ImageCRS"} (WKT 2) element. The syntax is given by
     * <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#79">WKT 2 specification §12</a>.
     *
     * @param  mode {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @return The {@code "ImageCRS"} element as an {@link ImageCRS} object.
     * @throws ParseException if the {@code "ImageCRS"} element can not be parsed.
     */
    private ImageCRS parseImageCRS(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.ImageCRS);
        if (element == null) {
            return null;
        }
        final String     name  = element.pullString("name");
        final ImageDatum datum = parseImageDatum(MANDATORY, element);
        final Unit<?>    unit  = parseUnit(element);
        final CoordinateSystem cs;
        try {
            cs = parseCoordinateSystem(element, WKTKeywords.Cartesian, 2, false, unit, datum);
            final Map<String,?> properties = parseMetadataAndClose(element, name, datum);
            if (cs instanceof AffineCS) {
                return crsFactory.createImageCRS(properties, datum, (AffineCS) cs);
            }
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
        throw element.illegalCS(cs);
    }

    /**
     * Parses a {@code "GeodeticCRS"} (WKT 2) element. The syntax is given by
     * <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#49">WKT 2 specification §8</a>.
     *
     * The legacy WKT 1 specification had two elements for this:
     *
     * {@preformat wkt
     *     GEOGCS["<name>", <datum>, <prime meridian>, <angular unit>  {,<twin axes>} {,<authority>}]
     * }
     *
     * and
     *
     * {@preformat wkt
     *     GEOCCS["<name>", <datum>, <prime meridian>, <linear unit> {,<axis> ,<axis> ,<axis>} {,<authority>}]
     * }
     *
     * @param  mode      {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent    The parent element.
     * @param  dimension The minimal number of dimensions (usually 2).
     * @param  csType    The default coordinate system type, or {@code null} if unknown.
     *                   Should be non-null only when parsing a {@link GeneralDerivedCRS#getBaseCRS()} component.
     * @return The {@code "GeodeticCRS"} element as a {@link GeographicCRS} or {@link GeocentricCRS} object.
     * @throws ParseException if the {@code "GeodeticCRS"} element can not be parsed.
     *
     * @see org.apache.sis.referencing.crs.DefaultGeographicCRS#formatTo(Formatter)
     * @see org.apache.sis.referencing.crs.DefaultGeocentricCRS#formatTo(Formatter)
     */
    private SingleCRS parseGeodeticCRS(final int mode, final Element parent, int dimension, String csType)
            throws ParseException
    {
        final Element element = parent.pullElement(mode,
                (csType != null) ? new String[] {WKTKeywords.BaseGeodCRS,       // [0]  WKT 2 in ProjectedCRS or DerivedCRS
                                                 WKTKeywords.GeogCS}            // [1]  WKT 1 in ProjectedCRS
                                 : new String[] {WKTKeywords.GeodeticCRS,       // [0]  WKT 2
                                                 WKTKeywords.GeogCS,            // [1]  WKT 1
                                                 WKTKeywords.GeodCRS,           // [2]  WKT 2
                                                 WKTKeywords.GeocCS});          // [3]  WKT 1
        if (element == null) {
            return null;
        }
        final boolean isWKT1;
        Unit<?> csUnit;
        final Unit<Angle> angularUnit;
        switch (element.getKeywordIndex()) {
            default: {
                /*
                 * WKT 2 "GeodeticCRS" element.
                 * The specification in §8.2.2 (ii) said:
                 *
                 *     "If the subtype of the geodetic CRS to which the prime meridian is an attribute
                 *     is geographic, the prime meridian’s <irm longitude> value shall be given in the
                 *     same angular units as those for the horizontal axes of the geographic CRS;
                 *     if the geodetic CRS subtype is geocentric the prime meridian’s <irm longitude>
                 *     value shall be given in degrees."
                 *
                 * An apparent ambiguity exists for Geocentric CRS using a Spherical CS instead than the more
                 * usual Cartesian CS: despite using angular units, we should not use the result of parseUnit
                 * for those CRS. However this ambiguity should not happen in practice because such Spherical
                 * CS have a third axis in metre.  Since the unit is not the same for all axes, csUnit should
                 * be null if the WKT is well-formed.
                 */
                isWKT1 = false;
                csUnit = parseUnit(element);
                if (Units.isAngular(csUnit)) {
                    angularUnit = csUnit.asType(Angle.class);
                } else {
                    angularUnit = NonSI.DEGREE_ANGLE;
                    if (csUnit == null) {
                        /*
                         * A UNIT[…] is mandatory either in the CoordinateSystem as a whole (csUnit != null),
                         * or inside each AXIS[…] component (csUnit == null). An exception to this rule is when
                         * parsing a BaseGeodCRS inside a ProjectedCRS or DerivedCRS, in which case axes are omitted.
                         * We recognize those cases by a non-null 'csType' given in argument to this method.
                         */
                        if (WKTKeywords.ellipsoidal.equals(csType)) {
                            csUnit = NonSI.DEGREE_ANGLE;                        // For BaseGeodCRS in ProjectedCRS.
                        }
                    }
                }
                break;
            }
            case 1: {
                /*
                 * WKT 1 "GeogCS" (Geographic) element.
                 */
                isWKT1      = true;
                csType      = WKTKeywords.ellipsoidal;
                angularUnit = parseScaledUnit(element, WKTKeywords.AngleUnit, SI.RADIAN);
                csUnit      = angularUnit;
                dimension   = 2;
                break;
            }
            case 3: {
                /*
                 * WKT 1 "GeocCS" (Geocentric) element.
                 */
                isWKT1      = true;
                csType      = WKTKeywords.Cartesian;
                angularUnit = NonSI.DEGREE_ANGLE;
                csUnit      = parseScaledUnit(element, WKTKeywords.LengthUnit, SI.METRE);
                dimension   = 3;
                break;
            }
        }
        final String name = element.pullString("name");
        /*
         * A GeodeticCRS can be either a "normal" one (with a non-null datum), or a DerivedCRS of kind GeodeticCRS.
         * In the later case, the datum is null and we have instead DerivingConversion element from a BaseGeodCRS.
         */
        SingleCRS  baseCRS  = null;
        Conversion fromBase = null;
        if (!isWKT1 && csType == null) {
            /*
             * UNIT[…] in DerivedCRS parameters are mandatory according ISO 19162 and the specification does not said
             * what to do if they are missing.  In this code, we default to the contextual units in the same way than
             * what we do for ProjectedCRS parameters, in the hope to be consistent.
             *
             * An alternative would be to specify null units, in which case MathTransformParser.parseParameters(…)
             * defaults to the units specified in the parameter descriptor. But this would make the CRS parser more
             * implementation-dependent, because the parameter descriptors are provided by the MathTransformFactory
             * instead than inferred from the WKT.
             */
            fromBase = parseDerivingConversion(OPTIONAL, element, WKTKeywords.DerivingConversion, csUnit, angularUnit);
            if (fromBase != null) {
                baseCRS = parseGeodeticCRS(MANDATORY, element, getSourceDimensions(fromBase.getMethod()), WKTKeywords.ellipsoidal);
            }
        }
        /*
         * At this point, we have either a non-null 'datum' or non-null 'baseCRS' + 'fromBase'.
         * The coordinate system is parsed in the same way for both cases, but the CRS is created differently.
         */
        final CoordinateSystem cs;
        try {
            cs = parseCoordinateSystem(element, csType, dimension, isWKT1, csUnit, null);
            if (baseCRS != null) {
                final Map<String,?> properties = parseMetadataAndClose(element, name, null);
                return crsFactory.createDerivedCRS(properties, baseCRS, fromBase, cs);
            }
            /*
             * The specification in §8.2.2 (ii) said:
             *
             *     "(snip) the prime meridian’s <irm longitude> value shall be given in the
             *     same angular units as those for the horizontal axes of the geographic CRS."
             *
             * This is a little bit different than using the 'angularUnit' variable directly,
             * since the WKT could have overwritten the unit directly in the AXIS[…] element.
             * So we re-fetch the angular unit. Normally, we will get the same value (unless
             * the previous value was null).
             */
            final Unit<Angle> longitudeUnit = AxisDirections.getAngularUnit(cs, angularUnit);
            if (angularUnit != null && !angularUnit.equals(longitudeUnit)) {
                warning(element, WKTKeywords.AngleUnit, Errors.formatInternational(
                        Errors.Keys.InconsistentUnitsForCS_1, angularUnit), null);
            }
            final PrimeMeridian meridian = parsePrimeMeridian(OPTIONAL, element, isWKT1, longitudeUnit);
            final GeodeticDatum datum = parseDatum(MANDATORY, element, meridian);
            final Map<String,?> properties = parseMetadataAndClose(element, name, datum);
            if (cs instanceof EllipsoidalCS) {  // By far the most frequent case.
                return crsFactory.createGeographicCRS(properties, datum, (EllipsoidalCS) cs);
            }
            if (cs instanceof CartesianCS) {                                    // The second most frequent case.
                return crsFactory.createGeocentricCRS(properties, datum,
                        referencing.upgradeGeocentricCS((CartesianCS) cs));
            }
            if (cs instanceof SphericalCS) {                                    // Not very common case.
                return crsFactory.createGeocentricCRS(properties, datum, (SphericalCS) cs);
            }
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
        throw element.illegalCS(cs);
    }

    /**
     * Parses a {@code "VerticalCRS"} (WKT 2) element. The syntax is given by
     * <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#69">WKT 2 specification §10</a>.
     *
     * The legacy WKT 1 pattern was:
     *
     * {@preformat wkt
     *     VERT_CS["<name>", <vert datum>, <linear unit>, {<axis>,} {,<authority>}]
     * }
     *
     * @param  mode      {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent    The parent element.
     * @param  isBaseCRS {@code true} if parsing the CRS inside a {@code DerivedCRS}.
     * @return The {@code "VerticalCRS"} element as a {@link VerticalCRS} object.
     * @throws ParseException if the {@code "VerticalCRS"} element can not be parsed.
     */
    @SuppressWarnings("null")
    private SingleCRS parseVerticalCRS(final int mode, final Element parent, final boolean isBaseCRS)
            throws ParseException
    {
        final Element element = parent.pullElement(mode,
                isBaseCRS ? new String[] {WKTKeywords.BaseVertCRS}              // WKT 2 in DerivedCRS
                          : new String[] {WKTKeywords.VerticalCRS,              // [0]  WKT 2
                                          WKTKeywords.VertCRS,                  // [1]  WKT 2
                                          WKTKeywords.Vert_CS});                // [2]  WKT 1
        if (element == null) {
            return null;
        }
        final boolean isWKT1 = element.getKeywordIndex() == 2;                  // Index of "Vert_CS" above.
        final String  name   = element.pullString("name");
        final Unit<?> unit   = parseUnit(element);
        /*
         * A VerticalCRS can be either a "normal" one (with a non-null datum), or a DerivedCRS of kind VerticalCRS.
         * In the later case, the datum is null and we have instead DerivingConversion element from a BaseVertCRS.
         */
        VerticalDatum datum    = null;
        SingleCRS     baseCRS  = null;
        Conversion    fromBase = null;
        if (!isWKT1 && !isBaseCRS) {
            /*
             * UNIT[…] in DerivedCRS parameters are mandatory according ISO 19162 and the specification does not said
             * what to do if they are missing.  In this code, we default to the contextual units in the same way than
             * what we do for ProjectedCRS parameters, in the hope to be consistent.
             *
             * An alternative would be to specify null units, in which case MathTransformParser.parseParameters(…)
             * defaults to the units specified in the parameter descriptor. But this would make the CRS parser more
             * implementation-dependent, because the parameter descriptors are provided by the MathTransformFactory
             * instead than inferred from the WKT.
             */
            fromBase = parseDerivingConversion(OPTIONAL, element, WKTKeywords.DerivingConversion, unit, null);
            if (fromBase != null) {
                baseCRS = parseVerticalCRS(MANDATORY, element, true);
            }
        }
        if (baseCRS == null) {                                                  // The most usual case.
            datum = parseVerticalDatum(MANDATORY, element, isWKT1);
        }
        final CoordinateSystem cs;
        try {
            cs = parseCoordinateSystem(element, WKTKeywords.vertical, 1, isWKT1, unit, datum);
            final Map<String,?> properties = parseMetadataAndClose(element, name, datum);
            if (cs instanceof VerticalCS) {
                if (baseCRS != null) {
                    return crsFactory.createDerivedCRS(properties, baseCRS, fromBase, cs);
                }
                /*
                 * The 'parseVerticalDatum(…)' method may have been unable to resolve the datum type.
                 * But sometime the axis (which was not available when we created the datum) provides
                 * more information. Verify if we can have a better type now, and if so rebuild the datum.
                 */
                if (VerticalDatumType.OTHER_SURFACE.equals(datum.getVerticalDatumType())) {
                    final VerticalDatumType type = VerticalDatumTypes.guess(datum.getName().getCode(), datum.getAlias(), cs.getAxis(0));
                    if (!VerticalDatumType.OTHER_SURFACE.equals(type)) {
                        datum = datumFactory.createVerticalDatum(referencing.getProperties(datum), type);
                    }
                }
                verticalCRS = crsFactory.createVerticalCRS(properties, datum, (VerticalCS) cs);
                /*
                 * Some DefaultVerticalExtent objects may be waiting for the VerticalCRS before to complete
                 * their construction. If this is the case, try to complete them now.
                 */
                if (verticalElements != null) {
                    verticalElements = verticalElements.resolve(verticalCRS);
                }
                return verticalCRS;
            }
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
        throw element.illegalCS(cs);
    }

    /**
     * Parses {@code "TimeCRS"} element.
     *
     * @param  mode      {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent    The parent element.
     * @param  isBaseCRS {@code true} if parsing the CRS inside a {@code DerivedCRS}.
     * @return The {@code "TimeCRS"} element as a {@link TemporalCRS} object.
     * @throws ParseException if the {@code "TimeCRS"} element can not be parsed.
     */
    private SingleCRS parseTimeCRS(final int mode, final Element parent, final boolean isBaseCRS)
            throws ParseException
    {
        final Element element = parent.pullElement(mode, isBaseCRS ? WKTKeywords.BaseTimeCRS : WKTKeywords.TimeCRS);
        if (element == null) {
            return null;
        }
        final String         name = element.pullString("name");
        final Unit<Duration> unit = parseScaledUnit(element, WKTKeywords.TimeUnit, SI.SECOND);
        /*
         * A TemporalCRS can be either a "normal" one (with a non-null datum), or a DerivedCRS of kind TemporalCRS.
         * In the later case, the datum is null and we have instead DerivingConversion element from a BaseTimeCRS.
         */
        TemporalDatum datum    = null;
        SingleCRS     baseCRS  = null;
        Conversion    fromBase = null;
        if (!isBaseCRS) {
            /*
             * UNIT[…] in DerivedCRS parameters are mandatory according ISO 19162 and the specification does not said
             * what to do if they are missing.  In this code, we default to the contextual units in the same way than
             * what we do for ProjectedCRS parameters, in the hope to be consistent.
             *
             * An alternative would be to specify null units, in which case MathTransformParser.parseParameters(…)
             * defaults to the units specified in the parameter descriptor. But this would make the CRS parser more
             * implementation-dependent, because the parameter descriptors are provided by the MathTransformFactory
             * instead than inferred from the WKT.
             */
            fromBase = parseDerivingConversion(OPTIONAL, element, WKTKeywords.DerivingConversion, unit, null);
            if (fromBase != null) {
                baseCRS = parseTimeCRS(MANDATORY, element, true);
            }
        }
        if (baseCRS == null) {                                                  // The most usual case.
            datum = parseTimeDatum(MANDATORY, element);
        }
        final CoordinateSystem cs;
        try {
            cs = parseCoordinateSystem(element, WKTKeywords.temporal, 1, false, unit, datum);
            final Map<String,?> properties = parseMetadataAndClose(element, name, datum);
            if (cs instanceof TimeCS) {
                if (baseCRS != null) {
                    return crsFactory.createDerivedCRS(properties, baseCRS, fromBase, cs);
                }
                return crsFactory.createTemporalCRS(properties, datum, (TimeCS) cs);
            }
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
        throw element.illegalCS(cs);
    }

    /**
     * Parses {@code "ParametricCRS"} element.
     *
     * @param  mode      {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent    The parent element.
     * @param  isBaseCRS {@code true} if parsing the CRS inside a {@code DerivedCRS}.
     * @return The {@code "ParametricCRS"} object.
     * @throws ParseException if the {@code "ParametricCRS"} element can not be parsed.
     */
    private SingleCRS parseParametricCRS(final int mode, final Element parent, final boolean isBaseCRS)
            throws ParseException
    {
        final Element element = parent.pullElement(mode, isBaseCRS ? WKTKeywords.BaseParamCRS : WKTKeywords.ParametricCRS);
        if (element == null) {
            return null;
        }
        final String  name = element.pullString("name");
        final Unit<?> unit = parseUnit(element);
        /*
         * A ParametricCRS can be either a "normal" one (with a non-null datum), or a DerivedCRS of kind ParametricCRS.
         * In the later case, the datum is null and we have instead DerivingConversion element from a BaseParametricCRS.
         */
        Datum           datum    = null;
        SingleCRS       baseCRS  = null;
        Conversion      fromBase = null;
        if (!isBaseCRS) {
            /*
             * UNIT[…] in DerivedCRS parameters are mandatory according ISO 19162 and the specification does not said
             * what to do if they are missing.  In this code, we default to the contextual units in the same way than
             * what we do for ProjectedCRS parameters, in the hope to be consistent.
             *
             * An alternative would be to specify null units, in which case MathTransformParser.parseParameters(…)
             * defaults to the units specified in the parameter descriptor. But this would make the CRS parser more
             * implementation-dependent, because the parameter descriptors are provided by the MathTransformFactory
             * instead than inferred from the WKT.
             */
            fromBase = parseDerivingConversion(OPTIONAL, element, WKTKeywords.DerivingConversion, unit, null);
            if (fromBase != null) {
                baseCRS = parseParametricCRS(MANDATORY, element, true);
            }
        }
        if (baseCRS == null) {                                                  // The most usual case.
            datum = parseParametricDatum(MANDATORY, element);
        }
        final CoordinateSystem cs;
        try {
            cs = parseCoordinateSystem(element, WKTKeywords.parametric, 1, false, unit, datum);
            final Map<String,?> properties = parseMetadataAndClose(element, name, datum);
            if (cs != null) {
                if (baseCRS != null) {
                    return crsFactory.createDerivedCRS(properties, baseCRS, fromBase, cs);
                }
                return referencing.createParametricCRS(properties, datum, cs, crsFactory);
            }
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
        throw element.illegalCS(cs);
    }

    /**
     * Parses a {@code "ProjectedCRS"} (WKT 2) element. The syntax is given by
     * <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#57">WKT 2 specification §9</a>.
     *
     * The legacy WKT 1 specification was:
     *
     * {@preformat wkt
     *     PROJCS["<name>", <geographic cs>, <projection>, {<parameter>,}*,
     *            <linear unit> {,<twin axes>}{,<authority>}]
     * }
     *
     * @param  mode      {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent    The parent element.
     * @param  isBaseCRS {@code true} if parsing the CRS inside a {@code DerivedCRS}.
     * @return The {@code "ProjectedCRS"} element as a {@link ProjectedCRS} object.
     * @throws ParseException if the {@code "ProjectedCRS"} element can not be parsed.
     */
    private ProjectedCRS parseProjectedCRS(final int mode, final Element parent, final boolean isBaseCRS)
            throws ParseException
    {
        final Element element = parent.pullElement(mode,
                isBaseCRS ? new String[] {WKTKeywords.BaseProjCRS}              // WKT 2 in DerivedCRS
                          : new String[] {WKTKeywords.ProjectedCRS,             // [0]  WKT 2
                                          WKTKeywords.ProjCRS,                  // [1]  WKT 2
                                          WKTKeywords.ProjCS});                 // [2]  WKT 1

        if (element == null) {
            return null;
        }
        final boolean   isWKT1 = element.getKeywordIndex() == 2;                // Index of "ProjCS" above.
        final String    name   = element.pullString("name");
        final SingleCRS geoCRS = parseGeodeticCRS(MANDATORY, element, 2, WKTKeywords.ellipsoidal);
        if (!(geoCRS instanceof GeographicCRS)) {
            throw new LocalizedParseException(errorLocale, Errors.Keys.IllegalCRSType_1,
                    new Object[] {geoCRS.getClass()}, element.offset);
        }
        /*
         * Parse the projection parameters. If a default linear unit is specified, it will apply to
         * all parameters that do not specify explicitely a LengthUnit. If no such crs-wide unit was
         * specified, then the default will be degrees.
         *
         * More specifically §9.3.4 in the specification said about the default units:
         *
         *    - lengths shall be given in the unit for the projected CRS axes.
         *    - angles shall be given in the unit for the base geographic CRS of the projected CRS.
         */
        Unit<Length> csUnit = parseScaledUnit(element, WKTKeywords.LengthUnit, SI.METRE);
        final Unit<Length> linearUnit;
        final Unit<Angle>  angularUnit;
        if (isWKT1 && usesCommonUnits) {
            linearUnit  = SI.METRE;
            angularUnit = NonSI.DEGREE_ANGLE;
        } else {
            linearUnit  = csUnit;
            angularUnit = AxisDirections.getAngularUnit(geoCRS.getCoordinateSystem(), NonSI.DEGREE_ANGLE);
        }
        final Conversion conversion = parseDerivingConversion(MANDATORY, element,
                isWKT1 ? null : WKTKeywords.Conversion, linearUnit, angularUnit);
        /*
         * Parse the coordinate system. The linear unit must be specified somewhere, either explicitely in each axis
         * or for the whole CRS with the above 'csUnit' value. If 'csUnit' is null, then an exception will be thrown
         * with a message like "A LengthUnit component is missing in ProjectedCRS".
         *
         * However we make an exception if we are parsing a BaseProjCRS, since the coordinate system is unspecified
         * in the WKT of base CRS. In this case only, we will default to metre.
         */
        if (csUnit == null && isBaseCRS) {
            csUnit = SI.METRE;
        }
        final CoordinateSystem cs;
        try {
            cs = parseCoordinateSystem(element, WKTKeywords.Cartesian, 2, isWKT1, csUnit, geoCRS.getDatum());
            final Map<String,?> properties = parseMetadataAndClose(element, name, conversion);
            if (cs instanceof CartesianCS) {
                return crsFactory.createProjectedCRS(properties, (GeographicCRS) geoCRS, conversion, (CartesianCS) cs);
            }
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
        throw element.illegalCS(cs);
    }

    /**
     * Parses a {@code "CompoundCRS"} element. The syntax is given by
     * <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#110">WKT 2 specification §16</a>.
     *
     * The legacy WKT 1 specification was:
     *
     * {@preformat wkt
     *     COMPD_CS["<name>", <head cs>, <tail cs> {,<authority>}]
     * }
     *
     * In the particular case where there is a geographic CRS and an ellipsoidal height,
     * this method rather build a three-dimensional geographic CRS.
     *
     * @param  mode {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @return The {@code "CompoundCRS"} element as a {@link CompoundCRS} object.
     * @throws ParseException if the {@code "CompoundCRS"} element can not be parsed.
     */
    private CoordinateReferenceSystem parseCompoundCRS(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.CompoundCRS, WKTKeywords.Compd_CS);
        if (element == null) {
            return null;
        }
        final String  name = element.pullString("name");
        CoordinateReferenceSystem crs;
        final List<CoordinateReferenceSystem> components = new ArrayList<CoordinateReferenceSystem>(4);
        while ((crs = parseCoordinateReferenceSystem(element, components.size() < 2)) != null) {
            components.add(crs);
        }
        try {
            return referencing.createCompoundCRS(crsFactory, csFactory, parseMetadataAndClose(element, name, null),
                    components.toArray(new CoordinateReferenceSystem[components.size()]));
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses a {@code "FITTED_CS"} element.
     * This element has the following pattern:
     *
     * {@preformat wkt
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
            final Map<String,Object> properties = parseMetadataAndClose(element, name, baseCRS);
            final CoordinateSystem derivedCS = referencing.createAbstractCS(
                    singletonMap(CoordinateSystem.NAME_KEY, AxisDirections.appendTo(new StringBuilder("CS"), axes)), axes);
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

    /**
     * Parses a {@code "CoordinateOperation"} element. The syntax is given by
     * <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#113">WKT 2 specification §17</a>.
     *
     * @param  mode {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @return The {@code "CoordinateOperation"} element as a {@link CoordinateOperation} object.
     * @throws ParseException if the {@code "CoordinateOperation"} element can not be parsed.
     */
    private CoordinateOperation parseOperation(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.CoordinateOperation);
        if (element == null) {
            return null;
        }
        final String name = element.pullString("name");
        final CoordinateReferenceSystem sourceCRS        = parseCoordinateReferenceSystem(element, MANDATORY, WKTKeywords.SourceCRS);
        final CoordinateReferenceSystem targetCRS        = parseCoordinateReferenceSystem(element, MANDATORY, WKTKeywords.TargetCRS);
        final CoordinateReferenceSystem interpolationCRS = parseCoordinateReferenceSystem(element, OPTIONAL,  WKTKeywords.InterpolationCRS);
        final OperationMethod           method           = parseMethod(element, WKTKeywords.Method);
        final Element                   accuracy         = element.pullElement(OPTIONAL, WKTKeywords.OperationAccuracy);
        final Map<String,Object>        properties       = parseMetadataAndClose(element, name, method);
        final ParameterValueGroup       parameters       = method.getParameters().createValue();
        parseParameters(element, parameters, null, null);
        properties.put(ReferencingServices.PARAMETERS_KEY, parameters);
        if (accuracy != null) {
            properties.put(CoordinateOperation.COORDINATE_OPERATION_ACCURACY_KEY,
                    TransformationAccuracy.create(accuracy.pullDouble("accuracy")));
            accuracy.close(ignoredElements);
        }
        try {
            return referencing.createSingleOperation(properties, sourceCRS, targetCRS, interpolationCRS, method, opFactory);
        } catch (FactoryException e) {
            throw element.parseFailed(e);
        }
    }
}
