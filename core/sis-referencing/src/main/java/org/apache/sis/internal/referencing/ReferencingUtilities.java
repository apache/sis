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
import org.opengis.util.FactoryException;
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
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.util.Static;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.datum.DefaultPrimeMeridian;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.DefaultEllipsoidalCS;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
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
 * @version 1.1
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
     * @see org.apache.sis.internal.referencing.AxisDirections#getAngularUnit(CoordinateSystem, Unit)
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
     * Returns the unit used for all axes in the given coordinate reference system.
     * If not all axes use the same unit, then this method returns {@code null}.
     *
     * <p>This method is used either when the CRS is expected to contain exactly one axis,
     * or for operations that support only one units for all axes.</p>
     *
     * @param  crs  the coordinate reference system for which to get the unit, or {@code null}.
     * @return the unit for all axis in the given coordinate system, or {@code null}.
     */
    public static Unit<?> getUnit(final CoordinateReferenceSystem crs) {
        return (crs != null) ? getUnit(crs.getCoordinateSystem()) : null;
    }

    /**
     * Returns the number of dimensions of the given CRS, or 0 if {@code null}.
     *
     * @param  crs  the CRS from which to get the number of dimensions, or {@code null}.
     * @return the number of dimensions, or 0 if the given CRS or its coordinate system is null.
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
     * Returns the GeoAPI interface implemented by the given object, or the implementation class
     * if the interface is unknown. This method can be used when the base type (CRS, CS, Datum…)
     * is unknown, for example when preparing an error message. If the base type is known, then
     * the method expecting a {@code baseType} argument should be preferred.
     *
     * @param  object    the object for which to get the GeoAPI interface, or {@code null}.
     * @return GeoAPI interface or implementation class of the given object, or {@code null} if the given object is null.
     */
    @SuppressWarnings("unchecked")
    public static Class<?> getInterface(final Object object) {
        if (object instanceof AbstractIdentifiedObject) {
            return ((AbstractIdentifiedObject) object).getInterface();
        } else {
            return getInterface(IdentifiedObject.class, (Class) Classes.getClass(object));
        }
    }

    /**
     * Returns the GeoAPI interface implemented by the given object, or the implementation class
     * if the interface is unknown.
     *
     * @param  <T>       compile-time value of {@code baseType}.
     * @param  baseType  parent interface of the desired type.
     * @param  object    the object for which to get the GeoAPI interface, or {@code null}.
     * @return GeoAPI interface or implementation class of the given object, or {@code null} if the given object is null.
     */
    public static <T extends IdentifiedObject> Class<? extends T> getInterface(final Class<T> baseType, final T object) {
        if (object instanceof AbstractIdentifiedObject) {
            return ((AbstractIdentifiedObject) object).getInterface().asSubclass(baseType);
        } else {
            return getInterface(baseType, Classes.getClass(object));
        }
    }

    /**
     * Returns the GeoAPI interface implemented by the given class, or the class itself if the interface is unknown.
     *
     * @param  <T>       compile-time value of {@code baseType}.
     * @param  baseType  parent interface of the desired type.
     * @param  type      type of object for which to get the GeoAPI interface, or {@code null}.
     * @return GeoAPI interface or implementation class, or {@code null} if the given type is null.
     */
    public static <T extends IdentifiedObject> Class<? extends T> getInterface(final Class<T> baseType, final Class<? extends T> type) {
        final Class<? extends T>[] types = Classes.getLeafInterfaces(type, baseType);
        return (types.length != 0) ? types[0] : type;
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
     * @see org.apache.sis.internal.referencing.VerticalDatumTypes#ELLIPSOIDAL
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
     * More specifically:
     *
     * <ul>
     *   <li>If the given CRS is an instance of {@link SingleCRS} and its datum is a {@link GeodeticDatum},
     *       then this method returns the datum ellipsoid.</li>
     *   <li>Otherwise if the given CRS is an instance of {@link CompoundCRS}, then this method
     *       invokes itself recursively for each component until a geodetic datum is found.</li>
     *   <li>Otherwise this method returns {@code null}.</li>
     * </ul>
     *
     * Note that this method does not check if there is more than one ellipsoid (it should never be the case).
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
     * Derives a geographic CRS with (<var>longitude</var>, <var>latitude</var>) axis in the specified
     * order and in decimal degrees. If no such CRS can be obtained or created, returns {@code null}.
     *
     * <p>This method does not set the prime meridian to Greenwich.
     * Meridian rotation, if needed, shall be performed by the caller.</p>
     *
     * @param  crs      a source CRS, or {@code null}.
     * @param  latlon   {@code true} for (latitude, longitude) axis order, or {@code false} for (longitude, latitude).
     * @param  allow3D  whether this method is allowed to return three-dimensional CRS (with ellipsoidal height).
     * @return a two-dimensional geographic CRS with standard axes, or {@code null} if none.
     */
    public static GeographicCRS toNormalizedGeographicCRS(CoordinateReferenceSystem crs, final boolean latlon, final boolean allow3D) {
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
             * If we can let `forConvention` do its job, do that first since it may return a cached
             * instance. If the CRS is a `GeographicCRS` but not a `DefaultGeographicCRS`, create a
             * CRS in this code instead of invoking `DefaultGeographicCRS.castOrCopy(…)` in order
             * to create only one CRS instead of two.
             */
            final CoordinateSystem cs = crs.getCoordinateSystem();
            if (!latlon && crs instanceof DefaultGeographicCRS && (allow3D || cs.getDimension() == 2)) {
                return ((DefaultGeographicCRS) crs).forConvention(AxesConvention.NORMALIZED);
            }
            /*
             * Get a normalized coordinate system with the number of dimensions authorized by the
             * `allow3D` argument. We do not check if we can invoke `cs.forConvention(…)` because
             * it is unlikely that `cs` will be an instance of `DefaultEllipsoidalCS` is `crs` is
             * not a `DefaultGeographicCRS`. The code below has more chances to use cached instance.
             */
            EllipsoidalCS normalizedCS;
            if (allow3D && cs.getDimension() >= 3) {
                normalizedCS = CommonCRS.WGS84.geographic3D().getCoordinateSystem();
                if (!latlon) {
                    normalizedCS = DefaultEllipsoidalCS.castOrCopy(normalizedCS).forConvention(AxesConvention.NORMALIZED);
                }
            } else if (latlon) {
                normalizedCS = CommonCRS.WGS84.geographic().getCoordinateSystem();
            } else {
                normalizedCS = CommonCRS.defaultGeographic().getCoordinateSystem();
            }
            if (crs instanceof GeographicCRS && Utilities.equalsIgnoreMetadata(normalizedCS, cs)) {
                return (GeographicCRS) crs;
            }
            return new DefaultGeographicCRS(
                    singletonMap(DefaultGeographicCRS.NAME_KEY, NilReferencingObject.UNNAMED),
                    ((GeodeticCRS) crs).getDatum(), normalizedCS);
        }
        if (crs instanceof CompoundCRS) {
            for (final CoordinateReferenceSystem e : ((CompoundCRS) crs).getComponents()) {
                final GeographicCRS candidate = toNormalizedGeographicCRS(e, latlon, allow3D);
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if the given coordinate system has at least 2 dimensions and
     * the 2 first axis have (North, East) directions. This method is used for assertions.
     *
     * @param  cs  the coordinate system to verify.
     * @return whether the coordinate system starts with (North, East) direction.
     */
    public static boolean startsWithNorthEast(final CoordinateSystem cs) {
        final int dimension = cs.getDimension();
        return (dimension >= 2)
                && AxisDirection.NORTH.equals(cs.getAxis(0).getDirection())
                && AxisDirection.EAST .equals(cs.getAxis(1).getDirection());
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
     * Substitute for the deprecated {@link MathTransformFactory#createBaseToDerived createBaseToDerived(…)} method.
     * This substitute use the full {@code targetCRS} instead of only the coordinate system of the target.
     * This is needed for setting the {@code "tgt_semi_minor"} and {@code "tgt_semi_major"} parameters of
     * Molodensky transformation for example.
     *
     * @param  factory     the factory to use for creating the transform.
     * @param  sourceCRS   the source (base) coordinate reference system.
     * @param  parameters  the parameter values for the transform.
     * @param  targetCRS   the target (derived) coordinate system.
     * @return the parameterized transform from {@code sourceCRS} to {@code targetCRS},
     *         including unit conversions and axis swapping.
     * @throws FactoryException if the object creation failed. This exception is thrown
     *         if some required parameter has not been supplied, or has illegal value.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-512">SIS-512 on issues tracker</a>
     */
    public static MathTransform createBaseToDerived(final MathTransformFactory factory,
            final CoordinateReferenceSystem sourceCRS, final ParameterValueGroup parameters,
            final CoordinateReferenceSystem targetCRS) throws FactoryException
    {
        if (factory instanceof DefaultMathTransformFactory) {
            return ((DefaultMathTransformFactory) factory).createParameterizedTransform(
                    parameters, createTransformContext(sourceCRS, targetCRS, null));
        } else {
            // Fallback for non-SIS implementations. Work for map projections but not for Molodensky.
            return factory.createBaseToDerived(sourceCRS, parameters, targetCRS.getCoordinateSystem());
        }
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
     * Returns short names for all axes of the given CRS. This method uses short names like "Latitude" or "Height",
     * even if the full ISO 19111 names are "Geodetic latitude" or "Ellipsoidal height". This is suitable as header
     * for columns in a table. This method does not include abbreviation or units in the returned names.
     *
     * @param  resources  the resources from which to get "latitude" and "longitude" localized labels.
     * @param  crs        the coordinate reference system from which to get axis names.
     * @return axis names, localized if possible.
     */
    public static String[] getShortAxisNames(final Vocabulary resources, final CoordinateReferenceSystem crs) {
        final boolean isGeographic = (crs instanceof GeographicCRS);
        final boolean isProjected  = (crs instanceof ProjectedCRS);
        final CoordinateSystem cs = crs.getCoordinateSystem();
        final String[] names = new String[cs.getDimension()];
        for (int i=0; i<names.length; i++) {
            short key = 0;
            final CoordinateSystemAxis axis = cs.getAxis(i);
            final AxisDirection direction = axis.getDirection();
            if (AxisDirections.isCardinal(direction)) {
                final boolean isMeridional = AxisDirection.NORTH.equals(direction) || AxisDirection.SOUTH.equals(direction);
                if (isGeographic) {
                    key = isMeridional ? Vocabulary.Keys.Latitude : Vocabulary.Keys.Longitude;
                } else if (isProjected) {
                    // We could add "Easting" / "Northing" here for ProjectedCRS in a future version.
                }
            } else if (AxisDirection.UP.equals(direction)) {
                if (isGeographic | isProjected) {
                    key = Vocabulary.Keys.Height;
                }
            }
            names[i] = (key != 0) ? resources.getString(key) : axis.getName().getCode();
        }
        return names;
    }
}
