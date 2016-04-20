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
import javax.measure.unit.Unit;
import javax.measure.quantity.Angle;
import org.opengis.annotation.UML;
import org.opengis.annotation.Specification;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.apache.sis.util.Static;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.CharSequences;
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
 * @since   0.5
 * @version 0.7
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
     * @param  primeMeridian The prime meridian from which to get the Greenwich longitude, or {@code null}.
     * @param  unit The unit for the prime meridian to return.
     * @return The prime meridian in the given units, or {@code 0} if the given prime meridian was null.
     *
     * @see DefaultPrimeMeridian#getGreenwichLongitude(Unit)
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
     * @param cs The coordinate system for which to get the unit, or {@code null}.
     * @return The unit for all axis in the given coordinate system, or {@code null}.
     *
     * @see org.apache.sis.internal.metadata.AxisDirections#getAngularUnit(CoordinateSystem)
     */
    public static Unit<?> getUnit(final CoordinateSystem cs) {
        Unit<?> unit = null;
        if (cs != null) {
            for (int i=cs.getDimension(); --i>=0;) {
                final CoordinateSystemAxis axis = cs.getAxis(i);
                if (axis != null) {  // Paranoiac check.
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
     * @param  crs The CRS from which to get the number of dimensions, or {@code null}.
     * @return The number of dimensions, or 0 if the given CRS or its coordinate system is null.
     *
     * @since 0.6
     */
    public static int getDimension(final CoordinateReferenceSystem crs) {
        if (crs != null) {
            final CoordinateSystem cs = crs.getCoordinateSystem();
            if (cs != null) {  // Paranoiac check.
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
     * @param  source The collection of single or compound CRS.
     * @param  addTo  Where to add the single CRS in order to obtain a flat view of {@code source}.
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
     * Returns the ellipsoid used by the specified coordinate reference system, provided that the two first dimensions
     * use an instance of {@link GeographicCRS}. Otherwise (i.e. if the two first dimensions are not geographic),
     * returns {@code null}.
     *
     * <p>This method excludes geocentric CRS on intend. Some callers needs this exclusion as a way to identify
     * which CRS in a Geographic/Geocentric conversion is the geographic one. An other point of view is to said
     * that if this method returns a non-null value, then the coordinates are expected to be either two-dimensional
     * or three-dimensional with an ellipsoidal height.</p>
     *
     * @param  crs The coordinate reference system for which to get the ellipsoid.
     * @return The ellipsoid in the given CRS, or {@code null} if none.
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
     * @param  crs A source CRS, or {@code null}.
     * @return A two-dimensional geographic CRS with standard axes, or {@code null} if none.
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
     *       Unicode identifier, then that part is included. The intend is to keep the prime meridian
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
     * @param  object The identified object to view as a properties map.
     * @param  excludes The keys of properties to exclude from the map.
     * @return A view of the identified object properties.
     *
     * @see IdentifiedObjects#getProperties(IdentifiedObject, String...)
     *
     * @since 0.7
     */
    public static Map<String,?> getPropertiesForModifiedCRS(final IdentifiedObject object, final String... excludes) {
        final Map<String,?> properties = IdentifiedObjects.getProperties(object, excludes);
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
                            final Map<String,Object> copy = new HashMap<String,Object>(properties);
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
     * @param  base The abstract base interface.
     * @param  type The interface or classes for which to get the XML property name.
     * @return The XML property name for the given class or interface, or {@code null} if none.
     *
     * @see WKTUtilities#toType(Class, Class)
     *
     * @since 0.6
     */
    public static StringBuilder toPropertyName(final Class<?> base, final Class<?> type) {
        final UML uml = type.getAnnotation(UML.class);
        if (uml != null && uml.specification() == Specification.ISO_19111) {
            final String name = uml.identifier();
            final int length = name.length();
            final StringBuilder buffer = new StringBuilder(length).append(name, name.indexOf('_') + 1, length);
            if (buffer.length() != 0) {
                buffer.setCharAt(0, Character.toLowerCase(buffer.charAt(0)));
                return buffer;
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
     * @param sourceCRS The CRS from which to get the source coordinate system and ellipsoid.
     * @param targetCRS The CRS from which to get the target coordinate system and ellipsoid.
     * @param context   A pre-allocated context, or {@code null} for creating a new one.
     * @return The given context if it was non-null, or a new context otherwise.
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
}
