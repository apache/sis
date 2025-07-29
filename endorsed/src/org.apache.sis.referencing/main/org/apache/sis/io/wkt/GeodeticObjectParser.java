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

import java.net.URI;
import java.util.Map;
import java.util.Locale;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.ParseException;
import java.time.Instant;
import static java.util.Collections.singletonMap;
import javax.measure.Unit;
import javax.measure.Quantity;
import javax.measure.IncommensurableException;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import javax.measure.quantity.Time;
import javax.measure.format.MeasurementParseException;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ObjectFactory;
import org.opengis.util.FactoryException;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.apache.sis.measure.Units;
import org.apache.sis.measure.UnitFormat;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.crs.DefaultDerivedCRS;
import org.apache.sis.referencing.datum.BursaWolfParameters;
import org.apache.sis.referencing.datum.DefaultGeodeticDatum;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.referencing.privy.CoordinateOperations;
import org.apache.sis.referencing.privy.ReferencingFactoryContainer;
import org.apache.sis.referencing.privy.EllipsoidalHeightCombiner;
import org.apache.sis.referencing.privy.AxisDirections;
import org.apache.sis.referencing.privy.WKTUtilities;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.referencing.internal.Legacy;
import org.apache.sis.referencing.internal.VerticalDatumTypes;
import org.apache.sis.referencing.internal.PositionalAccuracyConstant;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultGeographicDescription;
import org.apache.sis.metadata.iso.extent.DefaultVerticalExtent;
import org.apache.sis.metadata.iso.extent.DefaultTemporalExtent;
import org.apache.sis.metadata.privy.AxisNames;
import org.apache.sis.referencing.operation.provider.AbstractProvider;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.iso.Types;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.ObjectDomain;

// Specific to the geoapi-4.0 branch:
import org.apache.sis.referencing.crs.DefaultImageCRS;
import org.apache.sis.referencing.datum.DefaultImageDatum;


/**
 * Well Known Text (WKT) parser for referencing objects. This include, but is not limited too,
 * {@linkplain org.apache.sis.referencing.crs.AbstractCRS Coordinate Reference System} and
 * {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform Math Transform} objects.
 * Note that math transforms are part of the WKT 1 {@code "FITTED_CS"} element.
 *
 * @author  Rémi Eve (IRD)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 */
@SuppressWarnings("LocalVariableHidesMemberVariable")       // We hide with the same value made final.
class GeodeticObjectParser extends MathTransformParser implements Comparator<CoordinateSystemAxis> {
    /**
     * The names of the 7 parameters in a {@code TOWGS84[…]} element.
     * Those names are derived from the <cite>Well Known Text</cite> (WKT) version 1 specification.
     * They are not the same as the {@link org.apache.sis.referencing.datum.BursaWolfParameters}
     * field names, which are derived from the EPSG database.
     */
    private static final String[] ToWGS84 = {"dx", "dy", "dz", "ex", "ey", "ez", "ppm"};

    /**
     * During WKT 1 parsing, {@code true} means that {@code PRIMEM} and {@code PARAMETER} angular units
     * need to be forced to {@code Units.DEGREE} instead of inferred from the context.
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
     * @param  defaultProperties  default properties to give to the objects to create.
     * @param  factories  an object implementing {@link DatumFactory}, {@link CSFactory} and {@link CRSFactory}.
     * @param  mtFactory  the factory to use to create {@link MathTransform} objects.
     */
    public GeodeticObjectParser(final Map<String,?> defaultProperties,
            final ObjectFactory factories, final MathTransformFactory mtFactory)
    {
        super(null, Collections.emptyMap(), Symbols.getDefault(), null, null, null,
                new ReferencingFactoryContainer(defaultProperties,
                        (CRSFactory)   factories,
                        (CSFactory)    factories,
                        (DatumFactory) factories,
                        null, mtFactory),
                (Locale) defaultProperties.get(Errors.LOCALE_KEY));
        transliterator  = Transliterator.DEFAULT;
        usesCommonUnits = false;
        ignoreAxes      = false;
    }

    /**
     * Constructs a parser for the specified set of symbols using the specified set of factories.
     * This constructor is for {@link WKTFormat} usage only.
     *
     * @param  sourceFile    URI to declare as the source of the WKT definitions, or {@code null} if unknown.
     * @param  fragments     reference to the {@link WKTFormat#fragments} map, or an empty map if none.
     * @param  symbols       the set of symbols to use. Cannot be null.
     * @param  numberFormat  the number format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param  dateFormat    the date format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param  unitFormat    the unit format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param  convention    the WKT convention to use.
     * @param  errorLocale   the locale for error messages (not for parsing), or {@code null} for the system default.
     * @param  factories     on input, the factories to use. On output, the factories used. Can be null.
     */
    GeodeticObjectParser(final URI sourceFile, final Map<String,StoredTree> fragments, final Symbols symbols,
            final NumberFormat numberFormat, final DateFormat dateFormat, final UnitFormat unitFormat,
            final Convention convention, final Transliterator transliterator, final Locale errorLocale,
            final ReferencingFactoryContainer factories)
    {
        super(sourceFile, fragments, symbols, numberFormat, dateFormat, unitFormat, factories, errorLocale);
        this.transliterator = transliterator;
        usesCommonUnits = convention.usesCommonUnits;
        ignoreAxes      = convention == Convention.WKT1_IGNORE_AXES;
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
     * Completes or edits properties of the root {@link IdentifiedObject}. This method is invoked
     * before a {@code Factory.createFoo(Map, …)} method is invoked for creating the root object.
     * The {@code properties} map is filled with all information that this parser found in the WKT elements.
     * Subclasses can override this method for adding additional information if desired.
     *
     * <p>The most typical use case is to add a default {@link Identifier} when the WKT does not contain
     * an explicit {@code ID[…]} or {@code AUTHORITY[…]} element.</p>
     *
     * @param  properties  the properties to be given in a call to a {@code createFoo(Map, …)} method.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#complete(Map)
     */
    void completeRoot(Map<String,Object> properties) {
    }

    /**
     * Parses a <i>Well-Know Text</i> from specified position as a geodetic object.
     * Caller should invoke {@link #getAndClearWarnings(Object)} in a {@code finally} block
     * after this method.
     *
     * @param  text       the Well-Known Text (WKT) to parse.
     * @param  position   index of the first character to parse (on input) or after last parsed character (on output).
     * @return the parsed object.
     * @throws ParseException if the string cannot be parsed.
     */
    @Override
    final Object createFromWKT(final String text, final ParsePosition position) throws ParseException {
        final Object object;
        try {
            object = super.createFromWKT(text, position);
            /*
             * After parsing the object, we may have been unable to set the VerticalCRS of VerticalExtent instances.
             * First, try to set a default VerticalCRS for Mean Sea Level Height in metres. In the majority of cases
             * that should be enough. If not (typically because the vertical extent uses other unit than metre), try
             * to create a new CRS using the unit declared in the WKT.
             */
            if (verticalElements != null) {
                Exception ex = null;
                try {
                    verticalElements = verticalElements.resolve(CommonCRS.Vertical.MEAN_SEA_LEVEL.crs());     // Optional operation.
                } catch (UnsupportedOperationException e) {
                    ex = e;
                }
                if (verticalElements != null) try {
                    verticalElements = verticalElements.complete(factories.getCRSFactory(), factories.getCSFactory());
                } catch (FactoryException e) {
                    if (ex == null) ex = e;
                    else ex.addSuppressed(e);
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
     * Parses the next element in the specified <i>Well Know Text</i> (WKT) tree.
     *
     * @param  element  the element to be parsed.
     * @return the parsed object.
     * @throws ParseException if the element cannot be parsed.
     */
    @Override
    final Object buildFromTree(final Element element) throws ParseException {
        Object value = parseCoordinateReferenceSystem(element, false);
        if (value != null) {
            return value;
        }
        value = parseMathTransform(element, false);
        if (value != null) {
            return value;
        }
        Object object;
        if ((object = parseAxis             (FIRST, element, null,  Units.METRE )) == null &&
            (object = parsePrimeMeridian    (FIRST, element, false, Units.DEGREE)) == null &&
            (object = parseDatum            (FIRST, element, null )) == null &&
            (object = parseEllipsoid        (FIRST, element       )) == null &&
            (object = parseToWGS84          (FIRST, element       )) == null &&
            (object = parseVerticalDatum    (FIRST, element, false)) == null &&
            (object = parseTimeDatum        (FIRST, element       )) == null &&
            (object = parseParametricDatum  (FIRST, element       )) == null &&
            (object = parseEngineeringDatum (FIRST, element, false)) == null &&
            (object = parseImageDatum       (FIRST, element       )) == null &&
            (object = parseOperation        (FIRST, element))        == null &&
            (object = parseGeogTranslation  (FIRST, element))        == null)
        {
            throw element.missingOrUnknownComponent(WKTKeywords.GeodeticCRS);
        }
        return object;
    }

    /**
     * Parses a coordinate reference system element.
     *
     * @param  element    the parent element.
     * @param  mandatory  {@code true} if a CRS must be present, or {@code false} if optional.
     * @return the next element as a {@code CoordinateReferenceSystem} object.
     * @throws ParseException if the next element cannot be parsed.
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
     * @param  parent   the parent element containing the CRS to parse.
     * @param  mode     {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  keyword  "SourceCRS", "TargetCRS" or "InterpolationCRS".
     * @return the coordinate reference system, or {@code null} if none.
     * @throws ParseException if the CRS cannot be parsed.
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
     * This method shall accept all value types that {@link #parseMetadataAndClose(Element, String, IdentifiedObject)}
     * may store.
     *
     * @param  identifier  the {@link #properties} value, or {@code null}.
     * @return the identifier, or {@code null} if the given value was null.
     */
    private static Identifier toIdentifier(final Object identifier) {
        return (identifier instanceof Identifier[]) ? ((Identifier[]) identifier)[0] : (Identifier) identifier;
    }

    /**
     * Parses an <strong>optional</strong> metadata elements and close.
     * This includes elements like {@code "SCOPE"}, {@code "ID"} (WKT 2) or {@code "AUTHORITY"} (WKT 1).
     * This WKT 1 element has the following pattern:
     *
     * {@snippet lang="wkt" :
     *     AUTHORITY["<name>", "<code>"]
     *     }
     *
     * <h4>Fallback</h4>
     * The name is a mandatory property, but some invalid WKT with an empty string exist. In such case,
     * we will use the name of the enclosed datum. Indeed, it is not uncommon to have the same name for
     * a geographic CRS and its geodetic reference frame.
     *
     * @param  parent    the parent element.
     * @param  name      the name of the parent object being parsed.
     * @param  fallback  the fallback to use if {@code name} is empty.
     * @return a properties map with the parent name and the optional authority code.
     * @throws ParseException if an element cannot be parsed.
     *
     * @see #parseParametersAndClose(Element, String, OperationMethod)
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private Map<String,Object> parseMetadataAndClose(final Element parent, final String name,
            final IdentifiedObject fallback) throws ParseException
    {
        properties.clear();
        properties.put(IdentifiedObject.NAME_KEY, (name.isEmpty() && fallback != null) ? fallback.getName() : name);
        Element element;
        while ((element = parent.pullElement(OPTIONAL, ID_KEYWORDS)) != null) {
            final String  codeSpace = element.pullString("codeSpace");
            final String  code      = element.pullObject("code").toString();        // Accepts Integer as well as String.
            final Object  version   = element.pullOptional(Object.class);           // Accepts Number as well as String.
            final Element citation  = element.pullElement(OPTIONAL, WKTKeywords.Citation);
            final String  authority;
            if (citation != null) {
                authority = citation.pullString("authority");
                citation.close(ignoredElements);
            } else {
                authority = codeSpace;
            }
            final Element uri = element.pullElement(OPTIONAL, WKTKeywords.URI);
            if (uri != null) {
                uri.pullString("URI");      // TODO: not yet stored, since often redundant with other information.
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
             * However, experience shows that it is often wrong in practice, because peoples often
             * declare EPSG codes but still use WKT names much shorter than the EPSG names
             * (for example "WGS84" for the datum instead of "World Geodetic System 1984"),
             * so the name in WKT is often not compliant with the name actually defined by the authority.
             */
            final var id = new ImmutableIdentifier(Citations.fromName(authority),
                    codeSpace, code, (version != null) ? version.toString() : null, null);
            properties.merge(IdentifiedObject.IDENTIFIERS_KEY, id, (previous, toAdd) -> {
                final var more = (Identifier) toAdd;
                if (previous instanceof Identifier) {
                    return new Identifier[] {(Identifier) previous, more};
                } else {
                    return ArraysExt.append((Identifier[]) previous, more);
                }
            });
            // REMINDER: values associated to IDENTIFIERS_KEY shall be recognized by `toIdentifier(Object)`.
        }
        /*
         * Other metadata (SCOPE, AREA, etc.).  ISO 19162 said that at most one of each type shall be present,
         * but our parser accepts an arbitrary number of some kinds of metadata. They can be recognized by the
         * `while` loop.
         *
         * Most WKT do not contain any of those metadata, so we perform an `isEmpty()` check as an optimization
         * for those common cases.
         */
        if (!parent.isEmpty()) {
            /*
             * Example: SCOPE["Large scale topographic mapping and cadastre."]
             */
            element = parent.pullElement(OPTIONAL, WKTKeywords.Scope);
            if (element != null) {
                properties.put(ObjectDomain.SCOPE_KEY, element.pullString("scope"));
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
                Unit<Length> unit = parseScaledUnit(element, WKTKeywords.LengthUnit, Units.METRE);
                element.close(ignoredElements);
                if (unit   == null) unit   = Units.METRE;
                if (extent == null) extent = new DefaultExtent();
                verticalElements = new VerticalInfo(verticalElements, extent, minimum, maximum, unit).resolve(verticalCRS);
            }
            /*
             * Example: TIMEEXTENT[2013-01-01, 2013-12-31]
             *
             * TODO: syntax like TIMEEXTENT[“Jurassic”, “Quaternary”] is not yet supported.
             * See https://issues.apache.org/jira/browse/SIS-163
             */
            while ((element = parent.pullElement(OPTIONAL, WKTKeywords.TimeExtent)) != null) {
                if (element.peekValue() instanceof String) {
                    element.pullString("startTime");
                    element.pullString("endTime");
                    element.close(ignoredElements);
                    warning(parent, element, Errors.formatInternational(Errors.Keys.UnsupportedType_1, "TimeExtent[String,String]"), null);
                } else {
                    final Instant startTime = element.pullDate("startTime");
                    final Instant endTime   = element.pullDate("endTime");
                    element.close(ignoredElements);
                    final var t = new DefaultTemporalExtent(startTime, endTime);
                    if (extent == null) extent = new DefaultExtent();
                    extent.getTemporalElements().add(t);
                }
            }
            if (extent != null) {
                properties.put(ObjectDomain.DOMAIN_OF_VALIDITY_KEY, extent);
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
        if (parent.isRoot) {
            completeRoot(properties);
        }
        return properties;
    }

    /**
     * Parses the datum {@code ANCHOR[]} element and pass the values to the {@link #parseMetadataAndClose(Element,
     * String, IdentifiedObject)} method. If an anchor has been found, its value is stored in the returned map.
     */
    private Map<String,Object> parseAnchorAndClose(final Element element, final String name) throws ParseException {
        final Element anchor = element.pullElement(OPTIONAL, WKTKeywords.Anchor);
        final Map<String,Object> properties = parseMetadataAndClose(element, name, null);
        if (anchor != null) {
            properties.put(Datum.ANCHOR_DEFINITION_KEY, anchor.pullString("anchorDefinition"));
            anchor.close(ignoredElements);
        }
        return properties;
    }

    /**
     * Parses an optional {@code "UNIT"} element of a known dimension.
     * This element has the following pattern:
     *
     * {@snippet lang="wkt" :
     *     UNIT["<name>", <conversion factor> {,<authority>}]
     *     }
     *
     * Unit was a mandatory element in WKT 1, but became optional in WKT 2 because the unit may be specified
     * in each {@code AXIS[…]} element instead of for the whole coordinate system.
     *
     * @param  parent    the parent element.
     * @param  keyword   the unit keyword (e.g. {@code "LengthUnit"} or {@code "AngleUnit"}).
     * @param  baseUnit  the base unit, usually {@code Units.METRE} or {@code Units.RADIAN}.
     * @return the {@code "UNIT"} element as an {@link Unit} object, or {@code null} if none.
     * @throws ParseException if the {@code "UNIT"} cannot be parsed.
     *
     * @see #parseUnit(Element)
     *
     * @todo Authority code is currently discarded after parsing. We may consider to create a subclass of
     *       {@link Unit} which implements {@link IdentifiedObject} in a future version.
     */
    @SuppressWarnings("unchecked")
    private <Q extends Quantity<Q>> Unit<Q> parseScaledUnit(final Element parent,
            final String keyword, final Unit<Q> baseUnit) throws ParseException
    {
        final Element element = parent.pullElement(OPTIONAL, keyword, WKTKeywords.Unit);
        if (element == null) {
            return null;
        }
        final String name   = element.pullString("name");
        final double factor = element.pullDouble("factor");
        Unit<Q> unit   = baseUnit.multiply(completeUnitFactor(baseUnit, factor));
        Unit<?> verify = parseUnitID(element);
        element.close(ignoredElements);
        /*
         * Consider the following element: UNIT[“kilometre”, 1000, ID[“EPSG”, “9036”]]
         *
         *  - if the authority code (“9036”) refers to a unit incompatible with `baseUnit` (“metre”), log a warning.
         *  - otherwise: 1) unconditionally replace the parsed unit (“km”) by the unit referenced by the authority code.
         *               2) if the new unit is not equivalent to the old one (i.e. different scale factor), log a warning.
         */
        if (verify != null) {
            if (!baseUnit.getSystemUnit().equals(verify.getSystemUnit())) {
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
            } catch (MeasurementParseException e) {
                log(new LogRecord(Level.FINE, e.toString()));
            }
            if (verify != null) try {
                if (Math.abs(verify.getConverterToAny(unit).convert(1) - 1) > Numerics.COMPARISON_THRESHOLD) {
                    warning(parent, element, Errors.formatInternational(Errors.Keys.UnexpectedScaleFactorForUnit_2, verify, factor), null);
                }
            } catch (IncommensurableException e) {
                throw new UnparsableObjectException(errorLocale, Errors.Keys.InconsistentUnitsForCS_1,
                        new Object[] {verify}, element.offset).initCause(e);
            }
        }
        return unit;
    }

    /**
     * Parses a {@code "CS"} element followed by all {@code "AXIS"} elements.
     * This element has the following pattern (simplified):
     *
     * {@snippet lang="wkt" :
     *     CS["<type>", dimension],
     *     AXIS["<name>", NORTH | SOUTH | EAST | WEST | UP | DOWN | OTHER],
     *     UNIT["<name>", <conversion factor>],
     *     etc.
     *     }
     *
     * This element is different from all other elements parsed by {@code GeodeticObjectParser}
     * in that its components are sibling elements rather than child elements of the CS element.
     *
     * <p>The optional {@code "UNIT[…]"} element shall be parsed by the caller. That element may appear after the
     * {@code "CS[…]"} element (not inside). The unit may be forced to some dimension (e.g. {@code "LengthUnit"})
     * or be any kind of unit, depending on the context in which this {@code parseCoordinateSystem(…)} method is
     * invoked.</p>
     *
     * <h4>Variants of Cartesian type</h4>
     * The {@link WKTKeywords#Cartesian} type may be used for projected, geocentric or other kinds of CRS.
     * However, while all those variants are of the same CS type, their axis names and directions differ.
     * Current implementation uses the following rules:
     *
     * <ul>
     *   <li>If the datum is not geodetic, then the axes of the Cartesian CS are unknown.</li>
     *   <li>Otherwise if {@code dimension is 2}, then the CS is assumed to be for a projected CRS.</li>
     *   <li>Otherwise if {@code dimension is 3}, then the CS is assumed to be for a geocentric CRS.</li>
     * </ul>
     *
     * @param  parent       the parent element.
     * @param  type         the expected type (Cartesian | ellipsoidal | vertical | etc…), or null if unknown.
     * @param  dimension    the minimal number of dimensions. Can be 1 if unknown.
     * @param  isWKT1       {@code true} if the parent element is an element from the WKT 1 standard.
     * @param  defaultUnit  the contextual unit (usually {@code Units.METRE} or {@code Units.RADIAN}), or {@code null} if unknown.
     * @param  datum        the datum of the enclosing CRS, or {@code null} if unknown.
     * @return the {@code "CS"}, {@code "UNIT"} and/or {@code "AXIS"} elements as a Coordinate System, or {@code null}.
     * @throws ParseException if an element cannot be parsed.
     * @throws FactoryException if the factory cannot create the coordinate system.
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
                csProperties = new HashMap<>(parseMetadataAndClose(element, "CS", null));
                if (expected != null) {
                    if (!expected.equalsIgnoreCase(type)) {
                        throw new UnparsableObjectException(errorLocale, Errors.Keys.UnexpectedValueInElement_2,
                                new String[] {WKTKeywords.CS, type}, element.offset);
                    }
                }
                if (dimension <= 0 || dimension >= Numerics.MAXIMUM_MATRIX_SIZE) {
                    final short key;
                    final Object[] args;
                    if (dimension <= 0) {
                        key = Errors.Keys.ValueNotGreaterThanZero_2;
                        args = new Object[] {"dimension", dimension};
                    } else {
                        key = Errors.Keys.ExcessiveNumberOfDimensions_1;
                        args = new Object[] {dimension};
                    }
                    throw new UnparsableObjectException(errorLocale, key, args, element.offset);
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
            final var list = new ArrayList<CoordinateSystemAxis>(dimension + 2);
            do {
                list.add(axis);
                axis = parseAxis(list.size() < dimension ? MANDATORY : OPTIONAL, parent, type, defaultUnit);
            } while (axis != null);
            if (!isWKT1 || !ignoreAxes) {
                axes = list.toArray(CoordinateSystemAxis[]::new);
                Arrays.sort(axes, this);                    // Take ORDER[n] elements in account.
            }
        }
        /*
         * If there are no explicit AXIS[…] elements, or if the user asked to ignore them,
         * then we need to create default axes. This is possible only if we know the type
         * of the CS to create, and only for some of those CS types.
         */
        final CSFactory csFactory = factories.getCSFactory();
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
            switch (type) {
                /*
                 * Cartesian — we can create axes only if the datum is geodetic, in which case the axes
                 * are for two- or three-dimensional Projected or three-dimensional Geocentric CRS.
                 */
                case WKTKeywords.Cartesian: {
                    if (datum != null && !(datum instanceof GeodeticDatum)) {
                        throw parent.missingComponent(WKTKeywords.Axis);
                    }
                    if (defaultUnit == null) {
                        throw parent.missingComponent(WKTKeywords.LengthUnit);
                    }
                    if (is3D) {  // If dimension cannot be 2, then CRS cannot be Projected.
                        return Legacy.standard(defaultUnit);
                    }
                    nx = AxisNames.EASTING;  x = "E";
                    ny = AxisNames.NORTHING; y = "N";
                    if (dimension >= 3) {   // Non-standard but SIS is tolerant to this case.
                        z    = "h";
                        nz   = AxisNames.ELLIPSOIDAL_HEIGHT;
                        unit = Units.METRE;
                    }
                    break;
                }
                /*
                 * Ellipsoidal — can be two- or three- dimensional, in which case the height can
                 * only be ellipsoidal height. The default axis order depends on the WKT version:
                 *
                 *   - WKT 1 said explicitly that the default order is (longitude, latitude).
                 *   - WKT 2 has no default, and allows only (latitude, longitude) order.
                 */
                case WKTKeywords.ellipsoidal: {
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
                        unit = Units.METRE;
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
                    z         = "h";
                    nz        = "Height";
                    direction = AxisDirection.UP;
                    if (datum instanceof VerticalDatum) {
                        final RealizationMethod vt = ((VerticalDatum) datum).getRealizationMethod().orElse(null);
                        if (vt == RealizationMethod.GEOID) {
                            nz = AxisNames.GRAVITY_RELATED_HEIGHT;
                            z  = "H";
                        } else if (vt == RealizationMethod.TIDAL) {
                            direction = AxisDirection.DOWN;
                            nz = AxisNames.DEPTH;
                            z  = "D";
                        } else if (VerticalDatumTypes.ellipsoidal(vt)) {
                            // Not allowed by ISO 19111 as a standalone axis, but SIS is
                            // tolerant to this case since it is sometimes hard to avoid.
                            nz = AxisNames.ELLIPSOIDAL_HEIGHT;
                        }
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
                /*
                 * Parametric — axis name and abbreviation not yet specified by ISO 19111_2.
                 */
                case WKTKeywords.parametric: {
                    if (defaultUnit == null) {
                        throw parent.missingComponent(WKTKeywords.ParametricUnit);
                    }
                    direction = AxisDirection.UNSPECIFIED;
                    nz = "Parametric";
                    z = "p";
                    break;
                }
                /*
                 * Unknown CS type — we cannot guess which axes to create.
                 */
                default: {
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
        {   // For keeping the `buffer` variable local to this block.
            final var buffer = new StringBuilder();
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
            /*
             * Creates a coordinate system of unknown type. This block is executed during parsing of WKT version 1,
             * since that legacy format did not specify any information about the coordinate system in use.
             * This block should not be executed during parsing of WKT version 2.
             */
            return new AbstractCS(csProperties, axes);
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
                dimension = (axes.length < 2) ? 2 : 3;                      // For error message.
                break;
            }
            case WKTKeywords.spherical: {
                switch (axes.length) {
                    case 2: return csFactory.createSphericalCS(csProperties, axes[0], axes[1]);
                    case 3: return csFactory.createSphericalCS(csProperties, axes[0], axes[1], axes[2]);
                }
                dimension = (axes.length < 2) ? 2 : 3;                      // For error message.
                break;
            }
            case WKTKeywords.Cartesian: {
                switch (axes.length) {
                    case 2: return csFactory.createCartesianCS(csProperties, axes[0], axes[1]);
                    case 3: return csFactory.createCartesianCS(csProperties, axes[0], axes[1], axes[2]);
                }
                dimension = (axes.length < 2) ? 2 : 3;                      // For error message.
                break;
            }
            case WKTKeywords.affine: {
                switch (axes.length) {
                    case 2: return csFactory.createAffineCS(csProperties, axes[0], axes[1]);
                    case 3: return csFactory.createAffineCS(csProperties, axes[0], axes[1], axes[2]);
                }
                dimension = (axes.length < 2) ? 2 : 3;                      // For error message.
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
            case WKTKeywords.parametric: {
                if (axes.length != (dimension = 1)) break;
                return csFactory.createParametricCS(csProperties, axes[0]);
            }
            default: {
                warning(parent, WKTKeywords.CS, Errors.formatInternational(Errors.Keys.UnknownType_1, type), null);
                return new AbstractCS(csProperties, axes);
            }
        }
        throw new UnparsableObjectException(errorLocale, (axes.length > dimension)
                ? Errors.Keys.TooManyOccurrences_2 : Errors.Keys.TooFewOccurrences_2,
                new Object[] {dimension, WKTKeywords.Axis}, parent.offset);
    }

    /**
     * Parses an {@code "AXIS"} element.
     * This element has the following pattern (simplified):
     *
     * {@snippet lang="wkt" :
     *     AXIS["<name (abbr.)>", NORTH | SOUTH | EAST | WEST | UP | DOWN | OTHER, ORDER[n], UNIT[…], ID[…]]
     *     }
     *
     * Abbreviation may be specified between parenthesis. Nested parenthesis are possible, as for example:
     *
     * {@snippet lang="wkt" :
     *     AXIS["Easting (E(X))", EAST]
     *     }
     *
     * @param  mode         {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent       the parent element.
     * @param  csType       the coordinate system type (Cartesian | ellipsoidal | vertical | etc…), or null if unknown.
     * @param  defaultUnit  the contextual unit (usually {@code Units.METRE} or {@code Units.RADIAN}), or {@code null} if unknown.
     * @return the {@code "AXIS"} element as a {@link CoordinateSystemAxis} object,
     *         or {@code null} if the axis was not required and there are no axis objects.
     * @throws ParseException if the {@code "AXIS"} element cannot be parsed.
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
        AxisDirection direction = Types.forCodeName(AxisDirection.class, orientation.keyword, AxisDirection::valueOf);
        final Element meridian = element.pullElement(OPTIONAL, WKTKeywords.Meridian);
        if (meridian != null) {
            double angle = meridian.pullDouble("meridian");
            final Unit<Angle> m = parseScaledUnit(meridian, WKTKeywords.AngleUnit, Units.RADIAN);
            meridian.close(ignoredElements);
            if (m != null) {
                angle = m.getConverterTo(Units.DEGREE).convert(angle);
            }
            direction = CoordinateSystems.directionAlongMeridian(direction, angle);
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
            abbreviation = name.substring(start + 1, end).strip();
            name = name.substring(0, start).strip();
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
         * ordering. If present we will store that value for processing by the `parseCoordinateSystem(…)` method.
         */
        final Element order = element.pullElement(OPTIONAL, WKTKeywords.Order);
        Integer n = null;
        if (order != null) {
            n = order.pullInteger("order");
            order.close(ignoredElements);
        }
        final CoordinateSystemAxis axis;
        final CSFactory csFactory = factories.getCSFactory();
        try {
            axis = csFactory.createCoordinateSystemAxis(parseMetadataAndClose(element, name, null), abbreviation, direction, unit);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
        if (axisOrder.put(axis, n) != null) {   // Opportunist check, effective for instances created by SIS factory.
            throw new UnparsableObjectException(errorLocale, Errors.Keys.DuplicatedElement_1,
                    new Object[] {Strings.bracket(WKTKeywords.Axis, name)}, element.offset);
        }
        return axis;
    }

    /**
     * Compares axes for order. This method is used for ordering axes according their {@code ORDER} element,
     * if present. If no {@code ORDER} element were present, then the axis order is left unchanged. If only
     * some axes have an {@code ORDER} element (which is illegal according ISO 19162), then those axes will
     * be sorted before the axes without {@code ORDER} element.
     *
     * @param  o1  the first axis to compare.
     * @param  o2  the second axis to compare.
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
            return -1;                      // Axis 1 before Axis 2 since the latter has no `ORDER` element.
        } else if (n2 != null) {
            return +1;                      // Axis 2 before Axis 1 since the latter has no `ORDER` element.
        }
        return 0;
    }

    /**
     * Parses a {@code "PrimeMeridian"} element. The syntax is given by
     * <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#53">WKT 2 specification §8.2.2</a>.
     *
     * The legacy WKT 1 pattern was:
     *
     * {@snippet lang="wkt" :
     *     PRIMEM["<name>", <longitude> {,<authority>}]
     *     }
     *
     * @param  mode         {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent       the parent element.
     * @param  isWKT1       {@code true} if this method is invoked while parsing a WKT 1 element.
     * @param  angularUnit  the contextual unit.
     * @return the {@code "PrimeMeridian"} element as a {@link PrimeMeridian} object.
     * @throws ParseException if the {@code "PrimeMeridian"} element cannot be parsed.
     *
     * @see org.apache.sis.referencing.datum.DefaultPrimeMeridian#formatTo(Formatter)
     */
    private PrimeMeridian parsePrimeMeridian(final int mode, final Element parent, final boolean isWKT1, Unit<Angle> angularUnit)
            throws ParseException
    {
        if (isWKT1 && usesCommonUnits) {
            angularUnit = Units.DEGREE;
        }
        final Element element = parent.pullElement(mode, WKTKeywords.PrimeMeridian, WKTKeywords.PrimeM);
        if (element == null) {
            return null;
        }
        final String name      = element.pullString("name");
        final double longitude = element.pullDouble("longitude");
        final Unit<Angle> unit = parseScaledUnit(element, WKTKeywords.AngleUnit, Units.RADIAN);
        if (unit != null) {
            angularUnit = unit;
        } else if (angularUnit == null) {
            throw parent.missingComponent(WKTKeywords.AngleUnit);
        }
        final DatumFactory datumFactory = factories.getDatumFactory();
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
     * {@snippet lang="wkt" :
     *     TOWGS84[<dx>, <dy>, <dz>, <ex>, <ey>, <ez>, <ppm>]
     *     }
     *
     * @param  mode    {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent  the parent element.
     * @return the {@code "TOWGS84"} element as a {@link org.apache.sis.referencing.datum.BursaWolfParameters} object,
     *         or {@code null} if no {@code "TOWGS84"} has been found.
     * @throws ParseException if the {@code "TOWGS84"} cannot be parsed.
     */
    private Object parseToWGS84(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.ToWGS84);
        if (element == null) {
            return null;
        }
        final var values = new double[ToWGS84.length];
        for (int i=0; i<values.length;) {
            values[i] = element.pullDouble(ToWGS84[i]);
            if ((++i % 3) == 0 && element.isEmpty()) {
                break;                                              // It is legal to have only 3 or 6 elements.
            }
        }
        element.close(ignoredElements);
        final var info = new BursaWolfParameters(CommonCRS.WGS84.datum(), null);
        info.setValues(values);
        return info;
    }

    /**
     * Parses an {@code "Ellipsoid"} element. The syntax is given by
     * <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#52">WKT 2 specification §8.2.1</a>.
     *
     * The legacy WKT 1 pattern was:
     *
     * {@snippet lang="wkt" :
     *     SPHEROID["<name>", <semi-major axis>, <inverse flattening> {,<authority>}]
     *     }
     *
     * @param  mode    {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent  the parent element.
     * @return the {@code "Ellipsoid"} element as an {@link Ellipsoid} object.
     * @throws ParseException if the {@code "Ellipsoid"} element cannot be parsed.
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
        Unit<Length> unit = parseScaledUnit(element, WKTKeywords.LengthUnit, Units.METRE);
        if (unit == null) {
            unit = Units.METRE;
        }
        final Map<String,?> properties = parseMetadataAndClose(element, name, null);
        final DatumFactory datumFactory = factories.getDatumFactory();
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
     * Parses a {@code "GeodeticCRS"} (WKT 2) element where the number of dimensions and coordinate system type
     * are derived from the operation method. This is used for parsing the base CRS component of derived CRS.
     *
     * @param  mode       {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent     the parent element.
     * @param  method     the operation method, or {@code null} if unknown.
     * @throws ParseException if the {@code "GeodeticCRS"} element cannot be parsed.
     */
    private SingleCRS parseBaseCRS(final int mode, final Element parent, final OperationMethod method)
            throws ParseException
    {
        int dimension = 2;
        String csType = WKTKeywords.ellipsoidal;
        if (method instanceof AbstractProvider) {
            var p = (AbstractProvider) method;
            dimension = p.minSourceDimension;
            csType = WKTUtilities.toType(CoordinateSystem.class, p.sourceCSType);
            if (csType == null) csType = WKTKeywords.ellipsoidal;
        }
        return parseGeodeticCRS(mode, parent, dimension, csType);
    }

    /**
     * Parses a {@code "Method"} (WKT 2) element, without the parameters.
     *
     * @param  parent    the parent element.
     * @param  keywords  the element keywords.
     * @return the operation method.
     * @throws ParseException if the {@code "Method"} element cannot be parsed.
     */
    private OperationMethod parseMethod(final Element parent, final String... keywords) throws ParseException {
        final Element element    = parent.pullElement(MANDATORY, keywords);
        final String  name       = element.pullString("method");
        Map<String,?> properties = parseMetadataAndClose(element, name, null);
        final Identifier id      = toIdentifier(properties.remove(IdentifiedObject.IDENTIFIERS_KEY));  // See NOTE 2 in parseDerivingConversion.
        /*
         * The map projection method may be specified by an EPSG identifier (or any other authority),
         * which is preferred to the method name since the latter is potentially ambiguous. However, not
         * all CoordinateOperationFactory may accept identifier as an argument to `getOperationMethod(…)`.
         * So if an identifier is present, we will try to use it but fallback on the name if we can
         * not use the identifier.
         */
        FactoryException suppressed = null;
        if (id != null) try {
            // CodeSpace is a mandatory attribute in ID[…] elements, so we do not test for null values.
            return factories.findOperationMethod(id.getCodeSpace() + Constants.DEFAULT_SEPARATOR + id.getCode());
        } catch (FactoryException e) {
            suppressed = e;
        }
        try {
            return factories.findOperationMethod(name);
        } catch (FactoryException e) {
            if (suppressed != null) {
                e.addSuppressed(suppressed);
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
     * {@snippet lang="wkt" :
     *     PROJECTION["<name>" {,<authority>}]
     *     }
     *
     * Note that in WKT 2, this element is wrapped inside a {@code Conversion} or {@code DerivingConversion}
     * element which is itself inside the {@code ProjectedCRS} element. This is different than WKT 1, which
     * puts this element right into the {@code ProjectedCRS} element without {@code Conversion} wrapper.
     *
     * @param  mode                {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent              the parent element.
     * @param  wrapper             "Conversion" or "DerivingConversion" wrapper name, or null if parsing a WKT 1.
     * @param  defaultUnit         the unit (usually linear) of the parent element, or {@code null}.
     * @param  defaultAngularUnit  the angular unit of the sibling {@code GeographicCRS} element, or {@code null}.
     * @return the {@code "Method"} element and its parameters as a defining conversion.
     * @throws ParseException if the {@code "Method"} element cannot be parsed.
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
             * However, this would require this GeodeticObjectParser to hold a CoordinateOperationAuthorityFactory,
             * which we do not yet implement. See https://issues.apache.org/jira/browse/SIS-210
             */
        }
        final CoordinateOperationFactory opFactory = factories.getCoordinateOperationFactory();
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
     * {@snippet lang="wkt" :
     *     DATUM["<name>", <spheroid> {,<to wgs84>} {,<authority>}]
     *     }
     *
     * @param  mode      {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent    the parent element.
     * @param  meridian  the prime meridian, or {@code null} for Greenwich.
     * @return the {@code "Datum"} element as a {@link GeodeticDatum} object.
     * @throws ParseException if the {@code "Datum"} element cannot be parsed.
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
            meridian = CommonCRS.WGS84.primeMeridian();
        }
        if (toWGS84 != null) {
            properties.put(DefaultGeodeticDatum.BURSA_WOLF_KEY, toWGS84);
        }
        final DatumFactory datumFactory = factories.getDatumFactory();
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
     * {@snippet lang="wkt" :
     *     VERT_DATUM["<name>", <datum type> {,<authority>}]
     *     }
     *
     * @param  mode    {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent  the parent element.
     * @param  isWKT1  {@code true} if the parent is a WKT 1 element.
     * @return the {@code "VerticalDatum"} element as a {@link VerticalDatum} object.
     * @throws ParseException if the {@code "VerticalDatum"} element cannot be parsed.
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
        @SuppressWarnings("deprecation")
        RealizationMethod method = null;
        if (isWKT1) {
            method = VerticalDatumTypes.fromLegacyCode(element.pullInteger("datum"));
        }
        if (method == null) {
            method = VerticalDatumTypes.fromDatum(name, null, null);
        }
        final DatumFactory datumFactory = factories.getDatumFactory();
        try {
            return datumFactory.createVerticalDatum(parseAnchorAndClose(element, name), method);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses a {@code "TimeDatum"} element. This element has the following pattern:
     *
     * {@snippet lang="wkt" :
     *     TimeDatum["<name>", TimeOrigin[<time origin>] {,<authority>}]
     *     }
     *
     * @param  mode    {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent  the parent element.
     * @return the {@code "TimeDatum"} element as a {@link TemporalDatum} object.
     * @throws ParseException if the {@code "TimeDatum"} element cannot be parsed.
     */
    private TemporalDatum parseTimeDatum(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.TimeDatum, WKTKeywords.TDatum);
        if (element == null) {
            return null;
        }
        final String  name   = element.pullString ("name");
        final Element origin = element.pullElement(MANDATORY, WKTKeywords.TimeOrigin);
        final Instant epoch  = origin .pullDate("origin");
        origin.close(ignoredElements);
        final DatumFactory datumFactory = factories.getDatumFactory();
        try {
            return datumFactory.createTemporalDatum(parseAnchorAndClose(element, name), epoch);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses a {@code "ParametricDatum"} element. This element has the following pattern:
     *
     * {@snippet lang="wkt" :
     *     ParametricDatum["<name>", Anchor[...] {,<authority>}]
     *     }
     *
     * @param  mode    {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent  the parent element.
     * @return the {@code "ParametricDatum"} element as a {@link ParametricDatum} object.
     * @throws ParseException if the {@code "ParametricDatum"} element cannot be parsed.
     */
    private ParametricDatum parseParametricDatum(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.ParametricDatum, WKTKeywords.PDatum);
        if (element == null) {
            return null;
        }
        final String name = element.pullString("name");
        final DatumFactory datumFactory = factories.getDatumFactory();
        try {
            return datumFactory.createParametricDatum(parseAnchorAndClose(element, name));
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
     * {@snippet lang="wkt" :
     *     LOCAL_DATUM["<name>", <datum type> {,<authority>}]
     *     }
     *
     * The datum type (WKT 1 only) is currently ignored.
     *
     * @param  mode    {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent  the parent element.
     * @param  isWKT1  {@code true} if the parent is a WKT 1 element.
     * @return the {@code "EngineeringDatum"} element as an {@link EngineeringDatum} object.
     * @throws ParseException if the {@code "EngineeringDatum"} element cannot be parsed.
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
        final DatumFactory datumFactory = factories.getDatumFactory();
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
     * @param  mode    {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent  the parent element.
     * @return the {@code "ImageDatum"} element.
     * @throws ParseException if the {@code "ImageDatum"} element cannot be parsed.
     */
    @SuppressWarnings("removal")
    private DefaultImageDatum parseImageDatum(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.ImageDatum, WKTKeywords.IDatum);
        if (element == null) {
            return null;
        }
        final String name = element.pullString("name");
        final String pixelInCell = element.pullVoidElement("pixelInCell").keyword;
        return new DefaultImageDatum(parseAnchorAndClose(element, name), pixelInCell);
    }

    /**
     * Parses a {@code "EngineeringCRS"} (WKT 2) element. The syntax is given by
     * <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#74">WKT 2 specification §11</a>.
     *
     * The legacy WKT 1 pattern was:
     *
     * {@snippet lang="wkt" :
     *     LOCAL_CS["<name>", <local datum>, <unit>, <axis>, {,<axis>}* {,<authority>}]
     *     }
     *
     * @param  mode       {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent     the parent element.
     * @param  isBaseCRS  {@code true} if parsing the CRS inside a {@code DerivedCRS}.
     * @return the {@code "EngineeringCRS"} element as an {@link EngineeringCRS} object.
     * @throws ParseException if the {@code "EngineeringCRS"} element cannot be parsed.
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
         * In the latter case, the datum is null and we have instead DerivingConversion element from a base CRS.
         */
        EngineeringDatum datum    = null;
        SingleCRS        baseCRS  = null;
        Conversion       fromBase = null;
        if (!isWKT1 && !isBaseCRS) {
            /*
             * UNIT[…] in DerivedCRS parameters are mandatory according ISO 19162 and the specification does not said
             * what to do if they are missing.  In this code, we default to the contextual units in the same way as
             * what we do for ProjectedCRS parameters, in the hope to be consistent.
             *
             * An alternative would be to specify null units, in which case MathTransformParser.parseParameters(…)
             * defaults to the units specified in the parameter descriptor. But this would make the CRS parser more
             * implementation-dependent, because the parameter descriptors are provided by the MathTransformFactory
             * instead of inferred from the WKT.
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
                    baseCRS = parseBaseCRS(OPTIONAL, element, fromBase.getMethod());
                    if (baseCRS == null) {
                        baseCRS = parseProjectedCRS(MANDATORY, element, true);
                    }
                }
            }
        }
        if (baseCRS == null) {                                                  // The most usual case.
            datum = parseEngineeringDatum(MANDATORY, element, isWKT1);
        }
        final CRSFactory crsFactory = factories.getCRSFactory();
        try {
            final CoordinateSystem cs = parseCoordinateSystem(element, null, 1, isWKT1, unit, datum);
            final Map<String,Object> properties = parseMetadataAndClose(element, name, datum);
            if (baseCRS != null) {
                properties.put(Legacy.DERIVED_TYPE_KEY, EngineeringCRS.class);
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
     * @param  mode    {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent  the parent element.
     * @return the {@code "ImageCRS"} element as an {@link ImageCRS} object.
     * @throws ParseException if the {@code "ImageCRS"} element cannot be parsed.
     */
    @SuppressWarnings("removal")
    private DefaultImageCRS parseImageCRS(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.ImageCRS);
        if (element == null) {
            return null;
        }
        final String  name  = element.pullString("name");
        final var     datum = parseImageDatum(MANDATORY, element);
        final Unit<?> unit  = parseUnit(element);
        final CoordinateSystem cs;
        try {
            cs = parseCoordinateSystem(element, WKTKeywords.Cartesian, 2, false, unit, datum);
            final Map<String,?> properties = parseMetadataAndClose(element, name, datum);
            if (cs instanceof AffineCS) {
                return new DefaultImageCRS(properties, datum, (AffineCS) cs);
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
     * {@snippet lang="wkt" :
     *     GEOGCS["<name>", <datum>, <prime meridian>, <angular unit>  {,<twin axes>} {,<authority>}]
     *     }
     *
     * and
     *
     * {@snippet lang="wkt" :
     *     GEOCCS["<name>", <datum>, <prime meridian>, <linear unit> {,<axis> ,<axis> ,<axis>} {,<authority>}]
     *     }
     *
     * @param  mode       {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent     the parent element.
     * @param  dimension  the minimal number of dimensions (usually 2).
     * @param  csType     the default coordinate system type, or {@code null} if unknown.
     *                    Should be non-null only when parsing a {@link DerivedCRS#getBaseCRS()} component.
     * @return the {@code "GeodeticCRS"} element as a {@link GeographicCRS} or {@link GeodeticCRS} object.
     * @throws ParseException if the {@code "GeodeticCRS"} element cannot be parsed.
     *
     * @see org.apache.sis.referencing.crs.DefaultGeographicCRS#formatTo(Formatter)
     * @see org.apache.sis.referencing.crs.DefaultGeocentricCRS#formatTo(Formatter)
     */
    private SingleCRS parseGeodeticCRS(final int mode, final Element parent, int dimension, String csType)
            throws ParseException
    {
        final Element element = parent.pullElement(mode,
                (csType != null) ? new String[] {WKTKeywords.BaseGeodCRS,       // [0]  WKT 2 in ProjectedCRS or DerivedCRS
                                                 WKTKeywords.BaseGeogCRS,       // [1]  WKT 2 as a specialization of above
                                                 WKTKeywords.GeogCS}            // [2]  WKT 1 in ProjectedCRS
                                 : new String[] {WKTKeywords.GeodeticCRS,       // [0]  WKT 2
                                                 WKTKeywords.GeographicCRS,     // [1]  WKT 2 as a specialization of above
                                                 WKTKeywords.GeogCS,            // [2]  WKT 1
                                                 WKTKeywords.GeodCRS,           // [3]  WKT 2
                                                 WKTKeywords.GeogCRS,           // [4]  WKT 2 as a specialization of above
                                                 WKTKeywords.GeocCS});          // [5]  WKT 1
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
                 * An apparent ambiguity exists for Geocentric CRS using a Spherical CS instead of the more
                 * usual Cartesian CS: despite using angular units, we should not use the result of parseUnit
                 * for those CRS. However, this ambiguity should not happen in practice because such Spherical
                 * CS have a third axis in metre.  Since the unit is not the same for all axes, csUnit should
                 * be null if the WKT is well-formed.
                 */
                isWKT1 = false;
                csUnit = parseUnit(element);
                if (Units.isAngular(csUnit)) {
                    angularUnit = csUnit.asType(Angle.class);
                } else {
                    angularUnit = Units.DEGREE;
                    if (csUnit == null && csType != null) {
                        /*
                         * A UNIT[…] is mandatory either in the CoordinateSystem as a whole (csUnit != null),
                         * or inside each AXIS[…] component (csUnit == null). An exception to this rule is when
                         * parsing a BaseGeodCRS inside a ProjectedCRS or DerivedCRS, in which case axes are omitted.
                         * We recognize those cases by a non-null `csType` given in argument to this method.
                         */
                        switch (csType) {
                            case WKTKeywords.ellipsoidal: csUnit = Units.DEGREE; break;     // For BaseGeodCRS in ProjectedCRS.
                            case WKTKeywords.Cartesian:   csUnit = Units.METRE;  break;
                        }
                    }
                }
                break;
            }
            case 2: {
                /*
                 * WKT 1 "GeogCS" (Geographic) element.
                 */
                isWKT1      = true;
                csType      = WKTKeywords.ellipsoidal;
                angularUnit = parseScaledUnit(element, WKTKeywords.AngleUnit, Units.RADIAN);
                csUnit      = angularUnit;
                dimension   = 2;
                break;
            }
            case 5: {
                /*
                 * WKT 1 "GeocCS" (Geocentric) element.
                 */
                isWKT1      = true;
                csType      = WKTKeywords.Cartesian;
                angularUnit = Units.DEGREE;
                csUnit      = parseScaledUnit(element, WKTKeywords.LengthUnit, Units.METRE);
                dimension   = 3;
                break;
            }
        }
        final String name = element.pullString("name");
        /*
         * A GeodeticCRS can be either a "normal" one (with a non-null datum), or a DerivedCRS of kind GeodeticCRS.
         * In the latter case, the datum is null and we have instead DerivingConversion element from a BaseGeodCRS.
         */
        SingleCRS  baseCRS  = null;
        Conversion fromBase = null;
        if (!isWKT1 && csType == null) {
            /*
             * UNIT[…] in DerivedCRS parameters are mandatory according ISO 19162 and the specification does not said
             * what to do if they are missing.  In this code, we default to the contextual units in the same way as
             * what we do for ProjectedCRS parameters, in the hope to be consistent.
             *
             * An alternative would be to specify null units, in which case MathTransformParser.parseParameters(…)
             * defaults to the units specified in the parameter descriptor. But this would make the CRS parser more
             * implementation-dependent, because the parameter descriptors are provided by the MathTransformFactory
             * instead of inferred from the WKT.
             */
            fromBase = parseDerivingConversion(OPTIONAL, element, WKTKeywords.DerivingConversion, csUnit, angularUnit);
            if (fromBase != null) {
                baseCRS = parseBaseCRS(MANDATORY, element, fromBase.getMethod());
            }
        }
        /*
         * At this point, we have either a non-null `datum` or non-null `baseCRS` + `fromBase`.
         * The coordinate system is parsed in the same way for both cases, but the CRS is created differently.
         */
        final CRSFactory crsFactory = factories.getCRSFactory();
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
             * This is a little bit different than using the `angularUnit` variable directly,
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
            final DatumEnsemble<GeodeticDatum> datumEnsemble = null;    // TODO
            final Map<String,?> properties = parseMetadataAndClose(element, name, datum);
            if (cs instanceof EllipsoidalCS) {                                  // By far the most frequent case.
                return crsFactory.createGeographicCRS(properties, datum, datumEnsemble, (EllipsoidalCS) cs);
            }
            if (cs instanceof CartesianCS) {                                    // The second most frequent case.
                return crsFactory.createGeodeticCRS(properties, datum, datumEnsemble,
                        Legacy.forGeocentricCRS((CartesianCS) cs, false));
            }
            if (cs instanceof SphericalCS) {                                    // Not very common case.
                return crsFactory.createGeodeticCRS(properties, datum, datumEnsemble, (SphericalCS) cs);
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
     * {@snippet lang="wkt" :
     *     VERT_CS["<name>", <vert datum>, <linear unit>, {<axis>,} {,<authority>}]
     *     }
     *
     * @param  mode       {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent     the parent element.
     * @param  isBaseCRS  {@code true} if parsing the CRS inside a {@code DerivedCRS}.
     * @return the {@code "VerticalCRS"} element as a {@link VerticalCRS} object.
     * @throws ParseException if the {@code "VerticalCRS"} element cannot be parsed.
     */
    @SuppressWarnings("deprecation")
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
         * In the latter case, the datum is null and we have instead DerivingConversion element from a BaseVertCRS.
         */
        VerticalDatum datum    = null;
        SingleCRS     baseCRS  = null;
        Conversion    fromBase = null;
        if (!isWKT1 && !isBaseCRS) {
            /*
             * UNIT[…] in DerivedCRS parameters are mandatory according ISO 19162 and the specification does not said
             * what to do if they are missing.  In this code, we default to the contextual units in the same way as
             * what we do for ProjectedCRS parameters, in the hope to be consistent.
             *
             * An alternative would be to specify null units, in which case MathTransformParser.parseParameters(…)
             * defaults to the units specified in the parameter descriptor. But this would make the CRS parser more
             * implementation-dependent, because the parameter descriptors are provided by the MathTransformFactory
             * instead of inferred from the WKT.
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
                final CRSFactory crsFactory = factories.getCRSFactory();
                if (baseCRS != null) {
                    return crsFactory.createDerivedCRS(properties, baseCRS, fromBase, cs);
                }
                /*
                 * The `parseVerticalDatum(…)` method may have been unable to resolve the realization method.
                 * But sometimes the axis (which was not available when we created the datum) provides
                 * more information. Verify if we can have a better type now, and if so rebuild the datum.
                 */
                if (datum.getRealizationMethod().isEmpty()) {
                    var type = VerticalDatumTypes.fromDatum(datum.getName().getCode(), datum.getAlias(), cs.getAxis(0));
                    if (type != null) {
                        final DatumFactory datumFactory = factories.getDatumFactory();
                        datum = datumFactory.createVerticalDatum(IdentifiedObjects.getProperties(datum), type);
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
     * @param  mode       {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent     the parent element.
     * @param  isBaseCRS  {@code true} if parsing the CRS inside a {@code DerivedCRS}.
     * @return the {@code "TimeCRS"} element as a {@link TemporalCRS} object.
     * @throws ParseException if the {@code "TimeCRS"} element cannot be parsed.
     */
    private SingleCRS parseTimeCRS(final int mode, final Element parent, final boolean isBaseCRS)
            throws ParseException
    {
        final Element element = parent.pullElement(mode, isBaseCRS ? WKTKeywords.BaseTimeCRS : WKTKeywords.TimeCRS);
        if (element == null) {
            return null;
        }
        final String     name = element.pullString("name");
        final Unit<Time> unit = parseScaledUnit(element, WKTKeywords.TimeUnit, Units.SECOND);
        /*
         * A TemporalCRS can be either a "normal" one (with a non-null datum), or a DerivedCRS of kind TemporalCRS.
         * In the latter case, the datum is null and we have instead DerivingConversion element from a BaseTimeCRS.
         */
        TemporalDatum datum    = null;
        SingleCRS     baseCRS  = null;
        Conversion    fromBase = null;
        if (!isBaseCRS) {
            /*
             * UNIT[…] in DerivedCRS parameters are mandatory according ISO 19162 and the specification does not said
             * what to do if they are missing.  In this code, we default to the contextual units in the same way as
             * what we do for ProjectedCRS parameters, in the hope to be consistent.
             *
             * An alternative would be to specify null units, in which case MathTransformParser.parseParameters(…)
             * defaults to the units specified in the parameter descriptor. But this would make the CRS parser more
             * implementation-dependent, because the parameter descriptors are provided by the MathTransformFactory
             * instead of inferred from the WKT.
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
                final CRSFactory crsFactory = factories.getCRSFactory();
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
     * @param  mode       {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent     the parent element.
     * @param  isBaseCRS  {@code true} if parsing the CRS inside a {@code DerivedCRS}.
     * @return the {@code "ParametricCRS"} object.
     * @throws ParseException if the {@code "ParametricCRS"} element cannot be parsed.
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
         * In the latter case, the datum is null and we have instead DerivingConversion element from a BaseParametricCRS.
         */
        ParametricDatum datum = null;
        DatumEnsemble<ParametricDatum> datumEnsemble = null;    // TODO
        SingleCRS baseCRS = null;
        Conversion fromBase = null;
        if (!isBaseCRS) {
            /*
             * UNIT[…] in DerivedCRS parameters are mandatory according ISO 19162 and the specification does not said
             * what to do if they are missing.  In this code, we default to the contextual units in the same way as
             * what we do for ProjectedCRS parameters, in the hope to be consistent.
             *
             * An alternative would be to specify null units, in which case MathTransformParser.parseParameters(…)
             * defaults to the units specified in the parameter descriptor. But this would make the CRS parser more
             * implementation-dependent, because the parameter descriptors are provided by the MathTransformFactory
             * instead of inferred from the WKT.
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
            if (cs instanceof ParametricCS) {
                final CRSFactory crsFactory = factories.getCRSFactory();
                if (baseCRS != null) {
                    return crsFactory.createDerivedCRS(properties, baseCRS, fromBase, cs);
                }
                return crsFactory.createParametricCRS(properties, datum, datumEnsemble, (ParametricCS) cs);
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
     * {@snippet lang="wkt" :
     *     PROJCS["<name>", <geographic cs>, <projection>, {<parameter>,}*,
     *            <linear unit> {,<twin axes>}{,<authority>}]
     *     }
     *
     * @param  mode       {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent     the parent element.
     * @param  isBaseCRS  {@code true} if parsing the CRS inside a {@code DerivedCRS}.
     * @return the {@code "ProjectedCRS"} element as a {@link ProjectedCRS} object.
     * @throws ParseException if the {@code "ProjectedCRS"} element cannot be parsed.
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
        if (!(geoCRS instanceof GeodeticCRS)) {
            throw new UnparsableObjectException(errorLocale, Errors.Keys.IllegalCRSType_1,
                    new Object[] {geoCRS.getClass()}, element.offset);
        }
        /*
         * Parse the projection parameters. If a default linear unit is specified, it will apply to
         * all parameters that do not specify explicitly a LengthUnit. If no such crs-wide unit was
         * specified, then the default will be degrees.
         *
         * More specifically §9.3.4 in the specification said about the default units:
         *
         *    - lengths shall be given in the unit for the projected CRS axes.
         *    - angles shall be given in the unit for the base geographic CRS of the projected CRS.
         */
        Unit<Length> csUnit = parseScaledUnit(element, WKTKeywords.LengthUnit, Units.METRE);
        final Unit<Length> linearUnit;
        final Unit<Angle>  angularUnit;
        if (isWKT1 && usesCommonUnits) {
            linearUnit  = Units.METRE;
            angularUnit = Units.DEGREE;
        } else {
            linearUnit  = csUnit;
            angularUnit = AxisDirections.getAngularUnit(geoCRS.getCoordinateSystem(), Units.DEGREE);
        }
        final Conversion conversion = parseDerivingConversion(MANDATORY, element,
                isWKT1 ? null : WKTKeywords.Conversion, linearUnit, angularUnit);
        /*
         * Parse the coordinate system. The linear unit must be specified somewhere, either explicitly in each axis
         * or for the whole CRS with the above `csUnit` value. If `csUnit` is null, then an exception will be thrown
         * with a message like "A LengthUnit component is missing in ProjectedCRS".
         *
         * However, we make an exception if we are parsing a BaseProjCRS, since the coordinate system is unspecified
         * in the WKT of base CRS. In this case only, we will default to metre.
         */
        if (csUnit == null && isBaseCRS) {
            csUnit = Units.METRE;
        }
        final CoordinateSystem cs;
        try {
            cs = parseCoordinateSystem(element, WKTKeywords.Cartesian, 2, isWKT1, csUnit, geoCRS.getDatum());
            final Map<String,?> properties = parseMetadataAndClose(element, name, conversion);
            if (cs instanceof CartesianCS) {
                /*
                 * TODO: if the CartesianCS is three-dimensional, we need to ensure that the base CRS is also
                 * three-dimensional. We could do that by parsing the CS before to invoke `parseGeodeticCRS`.
                 */
                final CRSFactory crsFactory = factories.getCRSFactory();
                return crsFactory.createProjectedCRS(properties, (GeodeticCRS) geoCRS, conversion, (CartesianCS) cs);
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
     * {@snippet lang="wkt" :
     *     COMPD_CS["<name>", <head cs>, <tail cs> {,<authority>}]
     *     }
     *
     * In the particular case where there is a geographic CRS and an ellipsoidal height,
     * this method rather build a three-dimensional geographic CRS.
     *
     * @param  mode    {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent  the parent element.
     * @return the {@code "CompoundCRS"} element as a {@link CompoundCRS} object.
     * @throws ParseException if the {@code "CompoundCRS"} element cannot be parsed.
     */
    private CoordinateReferenceSystem parseCompoundCRS(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.CompoundCRS, WKTKeywords.Compd_CS);
        if (element == null) {
            return null;
        }
        final String  name = element.pullString("name");
        CoordinateReferenceSystem crs;
        final var components = new ArrayList<CoordinateReferenceSystem>(4);
        while ((crs = parseCoordinateReferenceSystem(element, components.size() < 2)) != null) {
            components.add(crs);
        }
        try {
            return new EllipsoidalHeightCombiner(factories).createCompoundCRS(
                            parseMetadataAndClose(element, name, null),
                            components.toArray(CoordinateReferenceSystem[]::new));
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses a {@code "FITTED_CS"} element.
     * This element has the following pattern:
     *
     * {@snippet lang="wkt" :
     *     FITTED_CS["<name>", <to base>, <base cs>]
     *     }
     *
     * @param  mode    {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent  the parent element.
     * @return the {@code "FITTED_CS"} element as a {@link CompoundCRS} object.
     * @throws ParseException if the {@code "COMPD_CS"} element cannot be parsed.
     */
    private DerivedCRS parseFittedCS(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.Fitted_CS);
        if (element == null) {
            return null;
        }
        final String                    name    = element.pullString("name");
        final MathTransform             toBase  = parseMathTransform(element, true);
        final OperationMethod           method  = getOperationMethod(element);
        final CoordinateReferenceSystem baseCRS = parseCoordinateReferenceSystem(element, true);
        if (!(baseCRS instanceof SingleCRS)) {
            throw new UnparsableObjectException(errorLocale, Errors.Keys.UnexpectedValueInElement_2,
                    new Object[] {WKTKeywords.Fitted_CS, baseCRS.getClass()}, element.offset);
        }
        /*
         * WKT 1 provides no information about the underlying CS of a derived CRS.
         * We have to guess some reasonable one with arbitrary units. We try to construct the one which
         * contains as few information as possible, in order to avoid providing wrong information.
         */
        final var axes = new CoordinateSystemAxis[toBase.getSourceDimensions()];
        final var buffer = new StringBuilder(name).append(" axis ");
        final int start = buffer.length();
        final CSFactory csFactory = factories.getCSFactory();
        try {
            for (int i=0; i<axes.length; i++) {
                final String number = String.valueOf(i);
                buffer.setLength(start);
                buffer.append(number);
                axes[i] = csFactory.createCoordinateSystemAxis(
                        singletonMap(CoordinateSystemAxis.NAME_KEY, buffer.toString()),
                        number, AxisDirection.UNSPECIFIED, Units.UNITY);
            }
            final Map<String,Object> properties = parseMetadataAndClose(element, name, baseCRS);
            final Map<String,Object> axisName = singletonMap(CoordinateSystem.NAME_KEY, AxisDirections.appendTo(new StringBuilder("CS"), axes));
            final var derivedCS = new AbstractCS(axisName, axes);
            /*
             * Creates a derived CRS from the information found in a WKT 1 {@code FITTED_CS} element.
             * This coordinate system cannot be easily constructed from the information provided by
             * the WKT 1 format, which block us from using the standard Coordinate System factory.
             * Note that we do not know which name to give to the conversion method; for now we use the CRS name.
             */
            properties.put("conversion.name", name);
            return DefaultDerivedCRS.create(properties, (SingleCRS) baseCRS, null, method, toBase.inverse(), derivedCS);
        } catch (FactoryException | NoninvertibleTransformException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses a {@code "GeogTran"} element. This is specific to ESRI.
     *
     * @param  mode    {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent  the parent element.
     * @return the {@code "GeogTran"} element as a {@link CoordinateOperation} object.
     * @throws ParseException if the {@code "GeogTran"} element cannot be parsed.
     */
    private CoordinateOperation parseGeogTranslation(final int mode, final Element parent) throws ParseException {
        final Element element = parent.pullElement(mode, WKTKeywords.GeogTran);
        if (element == null) {
            return null;
        }
        final String name = element.pullString("name");
        final CoordinateReferenceSystem sourceCRS  = parseGeodeticCRS(MANDATORY, element, 2, null);
        final CoordinateReferenceSystem targetCRS  = parseGeodeticCRS(MANDATORY, element, 2, null);
        final OperationMethod           method     = parseMethod(element, WKTKeywords.Method);
        final Map<String,Object>        properties = parseParametersAndClose(element, name, method);
        try {
            final DefaultCoordinateOperationFactory df = getOperationFactory();
            return df.createSingleOperation(properties, sourceCRS, targetCRS, null, method, null);
        } catch (FactoryException e) {
            throw element.parseFailed(e);
        }
    }

    /**
     * Parses a {@code "CoordinateOperation"} element. The syntax is given by
     * <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#113">WKT 2 specification §17</a>.
     *
     * @param  mode    {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent  the parent element.
     * @return the {@code "CoordinateOperation"} element as a {@link CoordinateOperation} object.
     * @throws ParseException if the {@code "CoordinateOperation"} element cannot be parsed.
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
        final Map<String,Object>        properties       = parseParametersAndClose(element, name, method);
        if (accuracy != null) {
            properties.put(CoordinateOperation.COORDINATE_OPERATION_ACCURACY_KEY,
                    PositionalAccuracyConstant.transformation(accuracy.pullDouble("accuracy")));
            accuracy.close(ignoredElements);
        }
        try {
            final DefaultCoordinateOperationFactory df = getOperationFactory();
            return df.createSingleOperation(properties, sourceCRS, targetCRS, interpolationCRS, method, null);
        } catch (FactoryException e) {
            throw element.parseFailed(e);
        }
    }

    /**
     * Parses a sequence of {@code "PARAMETER"} elements, then parses optional metadata elements and close.
     *
     * @param  parent  the parent element.
     * @param  name    the name of the parent object being parsed.
     * @param  method  the operation method, also the fallback to use if {@code name} is empty.
     * @return a properties map with the parent name, the optional authority code and the parameters.
     * @throws ParseException if an element cannot be parsed.
     *
     * @see #parseMetadataAndClose(Element, String, IdentifiedObject)
     */
    private Map<String,Object> parseParametersAndClose(final Element parent, final String name,
            final OperationMethod method) throws ParseException
    {
        final ParameterValueGroup parameters = method.getParameters().createValue();
        parseParameters(parent, parameters, null, null);
        final Map<String,Object> properties = parseMetadataAndClose(parent, name, method);
        properties.put(CoordinateOperations.PARAMETERS_KEY, parameters);
        return properties;
    }

    /**
     * Returns the factory to use for creating coordinate operation.
     */
    private DefaultCoordinateOperationFactory getOperationFactory() {
        final CoordinateOperationFactory opFactory = factories.getCoordinateOperationFactory();
        if (opFactory instanceof DefaultCoordinateOperationFactory) {
            return (DefaultCoordinateOperationFactory) opFactory;
        } else {
            return DefaultCoordinateOperationFactory.provider();
        }
    }
}
