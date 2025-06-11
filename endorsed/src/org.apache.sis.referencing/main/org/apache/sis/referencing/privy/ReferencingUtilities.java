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
package org.apache.sis.referencing.privy;

import java.util.Map;
import java.util.HashMap;
import java.util.function.Function;
import javax.measure.Unit;
import javax.measure.quantity.Angle;
import org.opengis.annotation.UML;
import org.opengis.annotation.Specification;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.util.Static;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.datum.DefaultPrimeMeridian;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.DefaultEllipsoidalCS;
import org.apache.sis.referencing.internal.VerticalDatumTypes;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.datum.DatumEnsemble;


/**
 * A set of static methods working on GeoAPI referencing objects.
 * Some of those methods may be useful, but not really rigorous.
 * This is why they do not appear in the public packages.
 *
 * <p><strong>Do not rely on this API!</strong> It may change in incompatible way in any future release.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public final class ReferencingUtilities extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private ReferencingUtilities() {
    }

    /**
     * Returns the longitude value relative to the Greenwich Meridian, expressed in the specified units.
     * This method provides the same functionality as {@link DefaultPrimeMeridian#getGreenwichLongitude(Unit)},
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
     * @see org.apache.sis.referencing.privy.AxisDirections#getAngularUnit(CoordinateSystem, Unit)
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
     * Returns {@code true} if the type of the given datum is ellipsoidal. A vertical datum is not allowed
     * to be ellipsoidal according ISO 19111, but Apache SIS relaxes this restriction in some limited cases,
     * for example when parsing a string in the legacy WKT 1 format. Apache SIS should not expose those
     * vertical heights as much as possible, and instead try to combine them with three-dimensional
     * geographic or projected CRS as soon as it can.
     *
     * @param  datum  the datum to test, or {@code null} if none.
     * @return {@code true} if the given datum is non null and of ellipsoidal type.
     *
     * @see org.apache.sis.referencing.internal.VerticalDatumTypes#ELLIPSOIDAL
     */
    public static boolean isEllipsoidalHeight(final VerticalDatum datum) {
        if (datum != null) {
            return datum.getRealizationMethod().map(VerticalDatumTypes::ellipsoidal).orElse(false);
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
     *   <li>Otherwise, if the given CRS is associated to a {@link DatumEnsemble} and all members of the
     *       ensemble have equal (ignoring metadata) ellipsoid, then returns that ellipsoid.</li>
     *   <li>Otherwise, if the given CRS is an instance of {@link CompoundCRS}, then this method
     *       invokes itself recursively for each component until a geodetic reference frame is found.</li>
     *   <li>Otherwise, this method returns {@code null}.</li>
     * </ul>
     *
     * Note that this method does not check if a compound <abbr>CRS</abbr> contains more than one ellipsoid
     * (it should never be the case). Note also that this method may return {@code null} even if the CRS is
     * geodetic.
     *
     * @param  crs  the coordinate reference system for which to get the ellipsoid.
     * @return the ellipsoid, or {@code null} if none.
     */
    public static Ellipsoid getEllipsoid(final CoordinateReferenceSystem crs) {
        return getGeodeticProperty(crs, GeodeticDatum::getEllipsoid);
    }

    /**
     * Returns the prime meridian used by the given coordinate reference system, or {@code null} if none.
     * This method applies the same rules as {@link #getEllipsoid(CoordinateReferenceSystem)}.
     *
     * @param  crs  the coordinate reference system for which to get the prime meridian.
     * @return the prime meridian, or {@code null} if none.
     */
    public static PrimeMeridian getPrimeMeridian(final CoordinateReferenceSystem crs) {
        return getGeodeticProperty(crs, GeodeticDatum::getPrimeMeridian);
    }

    /**
     * Implementation of {@code getEllipsoid(CRS)} and {@code getPrimeMeridian(CRS)}.
     * The difference between this method and {@link org.apache.sis.referencing.datum.PseudoDatum}
     * is that this method ignore null values and does not throw an exception in case of mismatch.
     *
     * @param  <P>     the type of object to get.
     * @param  crs     the coordinate reference system for which to get the ellipsoid or prime meridian.
     * @param  getter  the method to invoke on {@link GeodeticDatum} instances.
     * @return the ellipsoid or prime meridian, or {@code null} if none.
     */
    private static <P> P getGeodeticProperty(final CoordinateReferenceSystem crs, final Function<GeodeticDatum, P> getter) {
single: if (crs instanceof SingleCRS) {
            final SingleCRS scrs = (SingleCRS) crs;
            final Datum datum = scrs.getDatum();
            if (datum instanceof GeodeticDatum) {
                P property = getter.apply((GeodeticDatum) datum);
                if (property != null) {
                    return property;
                }
            }
            final DatumEnsemble<?> ensemble = scrs.getDatumEnsemble();
            if (ensemble != null) {
                P common = null;
                for (Datum member : ensemble.getMembers()) {
                    if (member instanceof GeodeticDatum) {
                        final P property = getter.apply((GeodeticDatum) member);
                        if (property != null) {
                            if (common == null) {
                                common = property;
                            } else if (!Utilities.equalsIgnoreMetadata(property, common)) {
                                break single;
                            }
                        }
                    }
                }
                return common;
            }
        }
        if (crs instanceof CompoundCRS) {
            for (final CoordinateReferenceSystem c : ((CompoundCRS) crs).getComponents()) {
                final P property = getGeodeticProperty(c, getter);
                if (property != null) {
                    return property;
                }
            }
        }
        return null;
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
    @SuppressWarnings("deprecation")
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
             * At this point we usually have a GeographicCRS, but it could also be a geocentric CRS.
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
            final var source = (GeodeticCRS) crs;
            return new DefaultGeographicCRS(
                    Map.of(DefaultGeographicCRS.NAME_KEY, NilReferencingObject.UNNAMED),
                    source.getDatum(), source.getDatumEnsemble(), normalizedCS);
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
                && cs.getAxis(0).getDirection() == AxisDirection.NORTH
                && cs.getAxis(1).getDirection() == AxisDirection.EAST;
    }

    /**
     * Returns the properties (scope, domain of validity) except the identifiers and the EPSG namespace.
     * The identifiers are removed because a modified CRS is no longer conform to the authoritative definition.
     * If the name contains a namespace (e.g. "EPSG"), this method removes that namespace for the same reason.
     * For example, "EPSG:WGS 84" will become simply "WGS 84".
     *
     * @param  object     the identified object for which to get properties map.
     * @param  overwrite  properties overwriting the inherited ones, or {@code null} if none.
     * @return the identified object properties.
     */
    public static Map<String,?> getPropertiesWithoutIdentifiers(final IdentifiedObject object, final Map<String,?> overwrite) {
        final Map<String,?> properties = IdentifiedObjects.getProperties(object, IdentifiedObject.IDENTIFIERS_KEY);
        final Identifier name = object.getName();
        final boolean keepName = name.getCodeSpace() == null && name.getAuthority() == null;
        if (keepName && overwrite == null) {
            return properties;
        }
        final var copy = new HashMap<String,Object>(properties);
        if (!keepName) {
            copy.put(IdentifiedObject.NAME_KEY, new NamedIdentifier(null, name.getCode()));
        }
        if (overwrite != null) {
            copy.putAll(overwrite);
        }
        return copy;
    }

    /**
     * Returns the properties of the given object but potentially with a modified name.
     * Current implement truncates the name at the first non-white character which is not
     * a valid Unicode identifier part, with the following exception:
     *
     * <ul>
     *   <li>If the character is {@code '('} and the content until the closing {@code ')'} is a valid
     *       Unicode identifier, then that part is included. The intent is to keep the prime meridian
     *       name in names like <q>NTF (Paris)</q>.</li>
     * </ul>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li><q>NTF (Paris)</q> is left unchanged.</li>
     *   <li><q>WGS 84 (3D)</q> is truncated as <q>WGS 84</q>.</li>
     *   <li><q>Ellipsoidal 2D CS. Axes: latitude, longitude. Orientations: north, east. UoM: degree</q>
     *       is truncated as <q>Ellipsoidal 2D CS</q>.</li>
     * </ul>
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
            if (spec == Specification.ISO_19111) {
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
     * Returns the given factory instance if non-null, or a default instance otherwise.
     *
     * @param  factory  the factory, which may be {@code null}.
     * @return the instance to use.
     */
    public static MathTransformFactory nonNull(MathTransformFactory factory) {
        if (factory == null) {
            factory = DefaultMathTransformFactory.provider().caching(false);
        }
        return factory;
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
     * Returns a parameter descriptor group with the same parameters as the given group, but a different name.
     * If the given code is equal to the current group code, then the {@code parameters} instance is returned.
     * Otherwise, a new group is created with the same name {@linkplain Identifier#getAuthority() authority}
     * and {@linkplain Identifier#getCodeSpace() code space} as the given parameter group, but with the
     * {@linkplain Identifier#getCode() code} replaced by the given value.
     *
     * <p><b>Examples:</b> this method can be used for creating the parameters of an inverse operation
     * in the common case where the inverse has the same parameters than the forward operation.</p>
     *
     * @param  parameters  the parameter group to rename, or {@code null}.
     * @param  code        the new name of the group, in the same code space as the given parameters.
     * @return a group with the same parameters but with a different name, or {@code null} if the given group is null.
     */
    public static ParameterDescriptorGroup rename(final ParameterDescriptorGroup parameters, final String code) {
        if (parameters != null) {
            Identifier name = parameters.getName();
            if (!code.equals(name.getCode())) {
                name = new ImmutableIdentifier(name.getAuthority(), name.getCodeSpace(), code);
                return new DefaultParameterDescriptorGroup(Map.of(ParameterDescriptorGroup.NAME_KEY, name), parameters);
            }
        }
        return parameters;
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
                final boolean isMeridional = (direction == AxisDirection.NORTH) || (direction == AxisDirection.SOUTH);
                if (isGeographic) {
                    key = isMeridional ? Vocabulary.Keys.Latitude : Vocabulary.Keys.Longitude;
                } else if (isProjected) {
                    // We could add "Easting" / "Northing" here for ProjectedCRS in a future version.
                }
            } else if (direction == AxisDirection.UP) {
                if (isGeographic | isProjected) {
                    key = Vocabulary.Keys.Height;
                }
            }
            names[i] = (key != 0) ? resources.getString(key) : axis.getName().getCode();
        }
        return names;
    }
}
