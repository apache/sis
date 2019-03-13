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
package org.apache.sis.internal.referencing;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import javax.measure.Unit;
import javax.measure.quantity.Angle;
import org.opengis.annotation.UML;
import org.opengis.annotation.Specification;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.datum.VerticalDatumType;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.Units;
import org.apache.sis.util.Static;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.datum.DefaultPrimeMeridian;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory.Context;

import static java.util.Collections.singletonMap;


/**
 * A set of static methods working on GeoAPI referencing objects.
 * Some of those methods may be useful, but not really rigorous.
 * This is why they do not appear in the public packages.
 *
 * <p><strong>Do not rely on this API!</strong> It may change in incompatible way in any future release.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   0.5
 * @module
 */
public final class ReferencingUtilities extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private ReferencingUtilities() {
    }

    /**
     * Returns the longitude value relative to the Greenwich Meridian, expressed in the specified units.
     * This method provides the same functionality than {@link DefaultPrimeMeridian#getGreenwichLongitude(Unit)},
     * but on arbitrary implementation.
     *
     * @param  primeMeridian  the prime meridian from which to get the Greenwich longitude, or {@code null}.
     * @param  unit           the unit for the prime meridian to return.
     * @return the prime meridian in the given units, or {@code 0} if the given prime meridian was null.
     *
     * @see DefaultPrimeMeridian#getGreenwichLongitude(Unit)
     * @see org.apache.sis.referencing.CRS#getGreenwichLongitude(GeodeticCRS)
     */
    public static double getGreenwichLongitude(final PrimeMeridian primeMeridian, final Unit<Angle> unit) {
        if (primeMeridian == null) {
            return 0;
        } else if (primeMeridian instanceof DefaultPrimeMeridian) {         // Maybe the user overrode some methods.
            return ((DefaultPrimeMeridian) primeMeridian).getGreenwichLongitude(unit);
        } else {
            return primeMeridian.getAngularUnit().getConverterTo(unit).convert(primeMeridian.getGreenwichLongitude());
        }
    }

    /**
     * Returns the range (maximum - minimum) of the given axis if it has wraparound meaning,
     * or {@link Double#NaN} otherwise. This method implements a fallback for the longitude
     * axis if it does not declare the minimum and maximum values as expected.
     *
     * @param  cs         the coordinate system for which to get wraparound range, or {@code null}.
     * @param  dimension  dimension of the axis to test.
     * @return the wraparound range, or {@link Double#NaN} if none.
     *
     * @since 1.0
     */
    public static double getWraparoundRange(final CoordinateSystem cs, final int dimension) {
        if (cs != null) {
            final CoordinateSystemAxis axis = cs.getAxis(dimension);
            if (axis != null && RangeMeaning.WRAPAROUND.equals(axis.getRangeMeaning())) {
                double period = axis.getMaximumValue() - axis.getMinimumValue();
                if (period > 0 && period != Double.POSITIVE_INFINITY) {
                    return period;
                }
                final AxisDirection dir = AxisDirections.absolute(axis.getDirection());
                if (AxisDirection.EAST.equals(dir) && cs instanceof EllipsoidalCS) {
                    period = Longitude.MAX_VALUE - Longitude.MIN_VALUE;
                    final Unit<?> unit = axis.getUnit();
                    if (unit != null) {
                        period = Units.DEGREE.getConverterTo(Units.ensureAngular(unit)).convert(period);
                    }
                    return period;
                }
            }
        }
        return Double.NaN;
    }

    /**
     * Returns the unit used for all axes in the given coordinate system.
     * If not all axes use the same unit, then this method returns {@code null}.
     *
     * <p>This method is used either when the coordinate system is expected to contain exactly one axis,
     * or for operations that support only one units for all axes, for example Well Know Text version 1
     * (WKT 1) formatting.</p>
     *
     * @param  cs  the coordinate system for which to get the unit, or {@code null}.
     * @return the unit for all axis in the given coordinate system, or {@code null}.
     *
     * @see org.apache.sis.internal.metadata.AxisDirections#getAngularUnit(CoordinateSystem, Unit)
     */
    public static Unit<?> getUnit(final CoordinateSystem cs) {
        Unit<?> unit = null;
        if (cs != null) {
            for (int i=cs.getDimension(); --i>=0;) {
                final CoordinateSystemAxis axis = cs.getAxis(i);
                if (axis != null) {                                         // Paranoiac check.
                    final Unit<?> candidate = axis.getUnit();
                    if (candidate != null) {
                        if (unit == null) {
                            unit = candidate;
                        } else if (!unit.equals(candidate)) {
                            return null;
                        }
                    }
                }
            }
        }
        return unit;
    }

    /**
     * Returns the number of dimensions of the given CRS, or 0 if {@code null}.
     *
     * @param  crs  the CRS from which to get the number of dimensions, or {@code null}.
     * @return the number of dimensions, or 0 if the given CRS or its coordinate system is null.
     *
     * @since 0.6
     */
    public static int getDimension(final CoordinateReferenceSystem crs) {
        if (crs != null) {
            final CoordinateSystem cs = crs.getCoordinateSystem();
            if (cs != null) {                                               // Paranoiac check.
                return cs.getDimension();
            }
        }
        return 0;
    }

    /**
     * Copies all {@link SingleCRS} components from the given source to the given collection.
     * For each {@link CompoundCRS} element found in the iteration, this method replaces the
     * {@code CompoundCRS} by its {@linkplain CompoundCRS#getComponents() components}, which
     * may themselves have other {@code CompoundCRS}. Those replacements are performed recursively
     * until we obtain a flat view of CRS components.
     *
     * @param  source  the collection of single or compound CRS.
     * @param  addTo   where to add the single CRS in order to obtain a flat view of {@code source}.
     * @return {@code true} if this method found only single CRS in {@code source}, in which case {@code addTo}
     *         got the same content (assuming that {@code addTo} was empty prior this method call).
     * @throws ClassCastException if a CRS is neither a {@link SingleCRS} or a {@link CompoundCRS}.
     *
     * @see org.apache.sis.referencing.CRS#getSingleComponents(CoordinateReferenceSystem)
     */
    public static boolean getSingleComponents(final Iterable<? extends CoordinateReferenceSystem> source,
            final Collection<? super SingleCRS> addTo) throws ClassCastException
    {
        boolean sameContent = true;
        for (final CoordinateReferenceSystem candidate : source) {
            if (candidate instanceof CompoundCRS) {
                getSingleComponents(((CompoundCRS) candidate).getComponents(), addTo);
                sameContent = false;
            } else {
                // Intentional CassCastException here if the candidate is not a SingleCRS.
                addTo.add((SingleCRS) candidate);
            }
        }
        return sameContent;
    }

    /**
     * Returns {@code true} if the type of the given datum is ellipsoidal. A vertical datum is not allowed
     * to be ellipsoidal according ISO 19111, but Apache SIS relaxes this restriction in some limited cases,
     * for example when parsing a string in the legacy WKT 1 format. Apache SIS should not expose those
     * vertical heights as much as possible, and instead try to combine them with three-dimensional
     * geographic or projected CRS as soon as it can.
     *
     * @param  datum  the datum to test, or {@code null} if none.
     * @return {@code true} if the given datum is non null and of ellipsoidal type.
     *
     * @see org.apache.sis.internal.metadata.VerticalDatumTypes#ELLIPSOIDAL
     *
     * @since 0.8
     */
    public static boolean isEllipsoidalHeight(final VerticalDatum datum) {
        if (datum != null) {
            final VerticalDatumType type = datum.getVerticalDatumType();
            if (type != null) {
                return "ELLIPSOIDAL".equalsIgnoreCase(type.name());
            }
        }
        return false;
    }

    /**
     * Returns the ellipsoid used by the given coordinate reference system, or {@code null} if none.
     *
     * @param  crs  the coordinate reference system for which to get the ellipsoid.
     * @return the ellipsoid, or {@code null} if none.
     */
    public static Ellipsoid getEllipsoid(final CoordinateReferenceSystem crs) {
        if (crs != null) {
            if (crs instanceof SingleCRS) {
                final Datum datum = ((SingleCRS) crs).getDatum();
                if (datum instanceof GeodeticDatum) {
                    final Ellipsoid e = ((GeodeticDatum) datum).getEllipsoid();
                    if (e != null) return e;
                }
            }
            if (crs instanceof CompoundCRS) {
                for (final CoordinateReferenceSystem c : ((CompoundCRS) crs).getComponents()) {
                    final Ellipsoid e = getEllipsoid(c);
                    if (e != null) return e;
                }
            }
        }
        return null;
    }

    /**
     * Returns the ellipsoid used by the specified coordinate reference system, provided that the two first dimensions
     * use an instance of {@link GeographicCRS}. Otherwise (i.e. if the two first dimensions are not geographic),
     * returns {@code null}.
     *
     * <p>This method excludes geocentric CRS on intent. Some callers needs this exclusion as a way to identify
     * which CRS in a Geographic/Geocentric conversion is the geographic one. An other point of view is to said
     * that if this method returns a non-null value, then the coordinates are expected to be either two-dimensional
     * or three-dimensional with an ellipsoidal height.</p>
     *
     * @param  crs  the coordinate reference system for which to get the ellipsoid.
     * @return the ellipsoid in the given CRS, or {@code null} if none.
     *
     * @since 0.6
     */
    public static Ellipsoid getEllipsoidOfGeographicCRS(CoordinateReferenceSystem crs) {
        while (!(crs instanceof GeodeticCRS)) {
            if (crs instanceof CompoundCRS) {
                crs = ((CompoundCRS) crs).getComponents().get(0);
            } else {
                return null;
            }
        }
        /*
         * In order to determine if the CRS is geographic, checking the CoordinateSystem type is more reliable
         * then checking if the CRS implements the GeographicCRS interface.  This is because the GeographicCRS
         * interface is GeoAPI-specific, so a CRS may be OGC-compliant without implementing that interface.
         */
        if (crs.getCoordinateSystem() instanceof EllipsoidalCS) {
            return ((GeodeticCRS) crs).getDatum().getEllipsoid();
        } else {
            return null;    // Geocentric CRS.
        }
    }

    /**
     * Derives a geographic CRS with (<var>longitude</var>, <var>latitude</var>) axis order in decimal degrees.
     * If no such CRS can be obtained or created, returns {@code null}.
     *
     * <p>This method does not set the prime meridian to Greenwich.
     * Meridian rotation, if needed, shall be performed by the caller.</p>
     *
     * @param  crs  a source CRS, or {@code null}.
     * @return a two-dimensional geographic CRS with standard axes, or {@code null} if none.
     */
    public static GeographicCRS toNormalizedGeographicCRS(CoordinateReferenceSystem crs) {
        /*
         * ProjectedCRS instances always have a GeographicCRS as their base.
         * More generally, derived CRS are always derived from a base, which
         * is often (but not necessarily) geographic.
         */
        while (crs instanceof GeneralDerivedCRS) {
            crs = ((GeneralDerivedCRS) crs).getBaseCRS();
        }
        if (crs instanceof GeodeticCRS) {
            /*
             * At this point we usually have a GeographicCRS, but it could also be a GeocentricCRS.
             */
            if (crs instanceof DefaultGeographicCRS && crs.getCoordinateSystem().getDimension() == 2) {
                return ((DefaultGeographicCRS) crs).forConvention(AxesConvention.NORMALIZED);
            }
            final CoordinateSystem cs = CommonCRS.defaultGeographic().getCoordinateSystem();
            if (crs instanceof GeographicCRS && Utilities.equalsIgnoreMetadata(cs, crs.getCoordinateSystem())) {
                return (GeographicCRS) crs;
            }
            return new DefaultGeographicCRS(
                    singletonMap(DefaultGeographicCRS.NAME_KEY, NilReferencingObject.UNNAMED),
                    ((GeodeticCRS) crs).getDatum(), (EllipsoidalCS) cs);
        }
        if (crs instanceof CompoundCRS) {
            for (final CoordinateReferenceSystem e : ((CompoundCRS) crs).getComponents()) {
                final GeographicCRS candidate = toNormalizedGeographicCRS(e);
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Returns the properties of the given object but potentially with a modified name.
     * Current implement truncates the name at the first non-white character which is not
     * a valid Unicode identifier part, with the following exception:
     *
     * <ul>
     *   <li>If the character is {@code '('} and the content until the closing {@code ')'} is a valid
     *       Unicode identifier, then that part is included. The intent is to keep the prime meridian
     *       name in names like <cite>"NTF (Paris)"</cite>.</li>
     * </ul>
     *
     * <div class="note"><b>Example:</b><ul>
     *   <li><cite>"NTF (Paris)"</cite> is left unchanged.</li>
     *   <li><cite>"WGS 84 (3D)"</cite> is truncated as <cite>"WGS 84"</cite>.</li>
     *   <li><cite>"Ellipsoidal 2D CS. Axes: latitude, longitude. Orientations: north, east. UoM: degree"</cite>
     *       is truncated as <cite>"Ellipsoidal 2D CS"</cite>.</li>
     * </ul></div>
     *
     * @param  object    the identified object to view as a properties map.
     * @return a view of the identified object properties.
     *
     * @see IdentifiedObjects#getProperties(IdentifiedObject, String...)
     *
     * @since 0.7
     */
    public static Map<String,?> getPropertiesForModifiedCRS(final IdentifiedObject object) {
        final Map<String,?> properties = IdentifiedObjects.getProperties(object, IdentifiedObject.IDENTIFIERS_KEY);
        final Identifier id = (Identifier) properties.get(IdentifiedObject.NAME_KEY);
        if (id != null) {
            String name = id.getCode();
            if (name != null) {
                for (int i=0; i < name.length();) {
                    final int c = name.codePointAt(i);
                    if (!Character.isUnicodeIdentifierPart(c) && !Character.isSpaceChar(c)) {
                        if (c == '(') {
                            final int endAt = name.indexOf(')', i);
                            if (endAt >= 0) {
                                final String extra = name.substring(i+1, endAt);
                                if (CharSequences.isUnicodeIdentifier(extra)) {
                                    i += extra.length() + 2;
                                }
                            }
                        }
                        name = CharSequences.trimWhitespaces(name, 0, i).toString();
                        if (!name.isEmpty()) {
                            final Map<String,Object> copy = new HashMap<>(properties);
                            copy.put(IdentifiedObject.NAME_KEY, name);
                            return copy;
                        }
                    }
                    i += Character.charCount(c);
                }
            }
        }
        return properties;
    }

    /**
     * Returns the XML property name of the given interface.
     *
     * For {@link CoordinateSystem} base type, the returned value shall be one of
     * {@code affineCS}, {@code cartesianCS}, {@code cylindricalCS}, {@code ellipsoidalCS}, {@code linearCS},
     * {@code parametricCS}, {@code polarCS}, {@code sphericalCS}, {@code timeCS} or {@code verticalCS}.
     *
     * @param  base  the abstract base interface.
     * @param  type  the interface or classes for which to get the XML property name.
     * @return the XML property name for the given class or interface, or {@code null} if none.
     *
     * @see WKTUtilities#toType(Class, Class)
     *
     * @since 0.6
     */
    public static StringBuilder toPropertyName(final Class<?> base, final Class<?> type) {
        final UML uml = type.getAnnotation(UML.class);
        if (uml != null) {
            final Specification spec = uml.specification();
            if (spec == Specification.ISO_19111 || spec == Specification.ISO_19111_2) {
                final String name = uml.identifier();
                final int length = name.length();
                final StringBuilder buffer = new StringBuilder(length).append(name, name.indexOf('_') + 1, length);
                if (buffer.length() != 0) {
                    buffer.setCharAt(0, Character.toLowerCase(buffer.charAt(0)));
                    return buffer;
                }
            }
        }
        for (final Class<?> c : type.getInterfaces()) {
            if (base.isAssignableFrom(c)) {
                final StringBuilder name = toPropertyName(base, c);
                if (name != null) {
                    return name;
                }
            }
        }
        return null;
    }

    /**
     * Sets the source and target ellipsoids and coordinate systems to values inferred from the given CRS.
     * The ellipsoids will be non-null only if the given CRS is geographic (not geocentric).
     *
     * @param  sourceCRS  the CRS from which to get the source coordinate system and ellipsoid.
     * @param  targetCRS  the CRS from which to get the target coordinate system and ellipsoid.
     * @param  context    a pre-allocated context, or {@code null} for creating a new one.
     * @return the given context if it was non-null, or a new context otherwise.
     *
     * @since 0.7
     */
    public static Context createTransformContext(final CoordinateReferenceSystem sourceCRS,
            final CoordinateReferenceSystem targetCRS, Context context)
    {
        if (context == null) {
            context = new Context();
        }
        final CoordinateSystem sourceCS = (sourceCRS != null) ? sourceCRS.getCoordinateSystem() : null;
        final CoordinateSystem targetCS = (targetCRS != null) ? targetCRS.getCoordinateSystem() : null;
        if (sourceCRS instanceof GeodeticCRS && sourceCS instanceof EllipsoidalCS) {
            context.setSource((EllipsoidalCS) sourceCS, ((GeodeticCRS) sourceCRS).getDatum().getEllipsoid());
        } else {
            context.setSource(sourceCS);
        }
        if (targetCRS instanceof GeodeticCRS && targetCS instanceof EllipsoidalCS) {
            context.setTarget((EllipsoidalCS) targetCS, ((GeodeticCRS) targetCRS).getDatum().getEllipsoid());
        } else {
            context.setTarget(targetCS);
        }
        return context;
    }

    /**
     * Creates a projected CRS from the given parameters using the default factories.
     *
     * @param  properties   the name and other properties, to be given both to the conversion and the CRS.
     * @param  baseCRS      the base geographic CRS.
     * @param  parameters   the map projection parameters.
     * @param  cs           the projected coordinate system.
     * @return the projected coordinate reference system.
     * @throws FactoryException if an error occurred while creating the CRS.
     *
     * @since 0.8
     */
    public static ProjectedCRS createProjectedCRS(final Map<String,String> properties,
            final GeographicCRS baseCRS, final ParameterValueGroup parameters, final CartesianCS cs)
            throws FactoryException
    {
        final CoordinateOperationFactory factory = DefaultFactories.forBuildin(CoordinateOperationFactory.class);
        return DefaultFactories.forBuildin(CRSFactory.class).createProjectedCRS(properties, baseCRS,
                factory.createDefiningConversion(properties,
                        factory.getOperationMethod(parameters.getDescriptor().getName().getCode()), parameters), cs);
    }

    /**
     * Returns the mapping between parameter identifiers and parameter names as defined by the given authority.
     * This method assumes that the identifiers of all parameters defined by that authority are numeric.
     * Examples of authorities defining numeric parameters are EPSG and GeoTIFF.
     *
     * <p>The map returned by this method is modifiable. Callers are free to add or remove entries.</p>
     *
     * @param  parameters  the parameters for which to get a mapping from identifiers to names.
     * @param  authority   the authority defining the parameters.
     * @return mapping from parameter identifiers to parameter names defined by the given authority.
     * @throws NumberFormatException if a parameter identifier of the given authority is not numeric.
     * @throws IllegalArgumentException if the same identifier is used for two or more parameters.
     *
     * @since 0.8
     */
    public static Map<Integer,String> identifierToName(final ParameterDescriptorGroup parameters, final Citation authority) {
        final Map<Integer,String> mapping = new HashMap<>();
        for (final GeneralParameterDescriptor descriptor : parameters.descriptors()) {
            final Identifier id = IdentifiedObjects.getIdentifier(descriptor, authority);
            if (id != null) {
                String name = IdentifiedObjects.getName(descriptor, authority);
                if (name == null) {
                    name = IdentifiedObjects.getName(descriptor, null);
                }
                if (mapping.put(Integer.valueOf(id.getCode()), name) != null) {
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.DuplicatedIdentifier_1, id));
                }
            }
        }
        return mapping;
    }

    /**
     * Returns an envelope with coordinates equivalent to the given coordinates,
     * but potentially shifted for intersecting the given domain of validity.
     * The dimensions that may be shifted are the ones having an axis with wraparound meaning.
     *
     * <p>The coordinate reference system must be specified in the given {@code areaOfInterest},
     * or (as a fallback) in the given {@code domainOfValidity}. If none of those envelope have
     * a CRS, then this method does nothing. If any envelope is null, then this method returns
     * {@code areaOfInterest} unchanged.</p>
     *
     * <p>This method does not intersect the area of interest with the domain of validity.
     * It is up to the caller to compute that intersection after this method call, if desired.</p>
     *
     * @param  areaOfInterest    the envelope to potentially shift toward the domain of validity, or {@code null} if none.
     * @param  domainOfValidity  the domain of validity, or {@code null} if none.
     * @param  validToAOI        if the envelopes do not use the same CRS, the transformation from {@code domainOfValidity}
     *                           to {@code areaOfInterest}. Otherwise {@code null}. This method does not check by itself if
     *                           a coordinate operation is needed; it must be supplied.
     * @return the given area of interest, possibly shifted toward the domain of validity. May also be expanded.
     * @throws TransformException if an envelope transformation was required but failed.
     *
     * @see GeneralEnvelope#simplify()
     *
     * @since 1.0
     */
    public static Envelope adjustWraparoundAxes(final Envelope areaOfInterest, Envelope domainOfValidity,
            CoordinateOperation validToAOI) throws TransformException
    {
        CoordinateReferenceSystem crs;
        if (areaOfInterest != null && domainOfValidity != null &&
                ((crs =   areaOfInterest.getCoordinateReferenceSystem()) != null ||
                 (crs = domainOfValidity.getCoordinateReferenceSystem()) != null))
        {
            GeneralEnvelope shifted = null;
            final DirectPosition lowerCorner = areaOfInterest.getLowerCorner();
            final DirectPosition upperCorner = areaOfInterest.getUpperCorner();
            final CoordinateSystem cs = crs.getCoordinateSystem();
            for (int i=cs.getDimension(); --i >= 0;) {
                final double period = getWraparoundRange(cs, i);
                if (period > 0) {
                    /*
                     * Found an axis (typically the longitude axis) with wraparound range meaning.
                     * We are going to need the domain of validity in the same CRS than the AOI.
                     * Transform that envelope when first needed.
                     */
                    if (validToAOI != null) {
                        final MathTransform mt = validToAOI.getMathTransform();
                        validToAOI = null;
                        if (!mt.isIdentity()) {
                            domainOfValidity = Envelopes.transform(mt, domainOfValidity);
                        }
                    }
                    /*
                     * "Unroll" the range. For example if we have [+160 … -170]° of longitude, we can replace by [160 … 190]°.
                     * We do not change the 'lower' or 'upper' value now in order to avoid rounding error. Instead we compute
                     * how many periods we need to add to those values. We adjust the side which results in the value closest
                     * to zero, in order to reduce rounding error if no more adjustment is done in the next block.
                     */
                    final double lower = lowerCorner.getOrdinate(i);
                    final double upper = upperCorner.getOrdinate(i);
                    double lowerCycles = 0;                             // In number of periods.
                    double upperCycles = 0;
                    double delta = upper - lower;
                    if (MathFunctions.isNegative(delta)) {              // Use 'isNegative' for catching [+0 … -0] range.
                        final double cycles = (delta == 0) ? -1 : Math.floor(delta / period);         // Always negative.
                        delta = cycles * period;
                        if (Math.abs(lower + delta) < Math.abs(upper - delta)) {
                            lowerCycles = cycles;                                    // Will subtract periods to 'lower'.
                        } else {
                            upperCycles = -cycles;                                   // Will add periods to 'upper'.
                        }
                    }
                    /*
                     * The range may be before or after the domain of validity. Compute the distance from current
                     * lower/upper coordinate to the coordinate of validity domain  (the sign tells us whether we
                     * are before or after). The cases can be:
                     *
                     *   ┌─────────────┬────────────┬────────────────────────────┬───────────────────────────────┐
                     *   │lowerIsBefore│upperIsAfter│ Meaning                    │ Action                        │
                     *   ├─────────────┼────────────┼────────────────────────────┼───────────────────────────────┤
                     *   │    false    │    false   │ AOI is inside valid area   │ Nothing to do                 │
                     *   │    true     │    true    │ AOI encompasses valid area │ Nothing to do                 │
                     *   │    true     │    false   │ AOI on left of valid area  │ Add positive amount of period │
                     *   │    false    │    true    │ AOI on right of valid area │ Add negative amount of period │
                     *   └─────────────┴────────────┴────────────────────────────┴───────────────────────────────┘
                     *
                     * We try to compute multiples of 'periods' instead than just adding or subtracting 'periods' once in
                     * order to support images that cover more than one period, for example images over 720° of longitude.
                     * It may happen for example if an image shows data under the trajectory of a satellite.
                     */
                    final double  validStart        = domainOfValidity.getMinimum(i);
                    final double  validEnd          = domainOfValidity.getMaximum(i);
                    final double  lowerToValidStart = ((validStart - lower) / period) - lowerCycles;    // In number of periods.
                    final double  upperToValidEnd   = ((validEnd   - upper) / period) - upperCycles;
                    final boolean lowerIsBefore     = (lowerToValidStart > 0);
                    final boolean upperIsAfter      = (upperToValidEnd < 0);
                    if (lowerIsBefore != upperIsAfter) {
                        final double upperToValidStart = ((validStart - upper) / period) - upperCycles;
                        final double lowerToValidEnd   = ((validEnd   - lower) / period) - lowerCycles;
                        if (lowerIsBefore) {
                            /*
                             * We need to add an integer amount of 'period' to both sides in order to move the range
                             * inside the valid area. We need  ⎣lowerToValidStart⎦  for reaching the point where:
                             *
                             *     (validStart - period) < (new lower) ≦ validStart
                             *
                             * But we may add more because there will be no intersection without following condition:
                             *
                             *     (new upper) ≧ validStart
                             *
                             * That second condition is met by  ⎡upperToValidStart⎤. Note: ⎣x⎦=floor(x) and ⎡x⎤=ceil(x).
                             */
                            final double cycles = Math.max(Math.floor(lowerToValidStart), Math.ceil(upperToValidStart));
                            /*
                             * If after the shift we see that the following condition hold:
                             *
                             *     (new lower) + period < validEnd
                             *
                             * Then we may have a situation like below:
                             *                  ┌────────────────────────────────────────────┐
                             *                  │             Domain of validity             │
                             *                  └────────────────────────────────────────────┘
                             *   ┌────────────────────┐                                ┌─────
                             *   │  Area of interest  │                                │  AOI
                             *   └────────────────────┘                                └─────
                             *    ↖……………………………………………………………period……………………………………………………………↗︎
                             *
                             * The user may be requesting two extremums of the domain of validity. We can not express
                             * that with a single envelope. Instead, we will expand the Area Of Interest to encompass
                             * the full domain of validity.
                             */
                            if (cycles + 1 < lowerToValidEnd) {
                                upperCycles += Math.ceil(upperToValidEnd);
                            } else {
                                upperCycles += cycles;
                            }
                            lowerCycles += cycles;
                        } else {
                            /*
                             * Same reasoning than above with sign reverted and lower/upper variables interchanged.
                             * In this block, 'upperToValidEnd' and 'lowerToValidEnd' are negative, contrarily to
                             * above block where they were positive.
                             */
                            final double cycles = Math.min(Math.ceil(upperToValidEnd), Math.floor(lowerToValidEnd));
                            if (cycles - 1 > upperToValidStart) {
                                lowerCycles += Math.floor(lowerToValidStart);
                            } else {
                                lowerCycles += cycles;
                            }
                            upperCycles += cycles;
                        }
                    }
                    /*
                     * If there is change to apply, copy the envelope when first needed.
                     */
                    if (lowerCycles != 0 || upperCycles != 0) {
                        if (shifted == null) {
                            shifted = new GeneralEnvelope(areaOfInterest);
                        }
                        shifted.setRange(i, lower + lowerCycles * period,       // TODO: use Math.fma in JDK9.
                                            upper + upperCycles * period);
                    }
                }
            }
            if (shifted != null) {
                return shifted;
            }
        }
        return areaOfInterest;
    }
}
