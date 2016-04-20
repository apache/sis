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
package org.apache.sis.internal.metadata;

import java.util.Map;
import java.util.Collections;
import java.util.Locale;
import javax.measure.unit.Unit;
import javax.measure.quantity.Length;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultVerticalExtent;
import org.apache.sis.metadata.iso.extent.DefaultTemporalExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultSpatialTemporalExtent;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.system.OptionalDependency;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.iso.DefaultNameSpace;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.resources.Errors;

// Branch-specific imports
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.DatumFactory;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.util.NoSuchIdentifierException;


/**
 * Provides access to services defined in the {@code "sis-referencing"} module.
 * This class searches for the {@link org.apache.sis.internal.referencing.ServicesForMetadata}
 * implementation using Java reflection.
 *
 * <p>This class also opportunistically defines some methods and constants related to
 * <cite>"referencing by coordinates"</cite> but needed by metadata.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
public class ReferencingServices extends OptionalDependency {
    /**
     * The length of one nautical mile, which is {@value} metres.
     */
    public static final double NAUTICAL_MILE = 1852;

    /**
     * The GRS80 {@linkplain org.apache.sis.referencing.datum.DefaultEllipsoid#getAuthalicRadius() authalic radius},
     * which is {@value} metres.
     */
    public static final double AUTHALIC_RADIUS = 6371007;

    /**
     * The {@link org.apache.sis.referencing.datum.DefaultGeodeticDatum#BURSA_WOLF_KEY} value.
     */
    public static final String BURSA_WOLF_KEY = "bursaWolf";

    /**
     * The key for specifying explicitely the value to be returned by
     * {@link org.apache.sis.referencing.operation.DefaultConversion#getParameterValues()}.
     * It is usually not necessary to specify those parameters because they are inferred either from
     * the {@link MathTransform}, or specified explicitely in a {@code DefiningConversion}. However
     * there is a few cases, for example the Molodenski transform, where none of the above can apply,
     * because SIS implements those operations as a concatenation of math transforms, and such
     * concatenations do not have {@link org.opengis.parameter.ParameterValueGroup}.
     */
    public static final String PARAMETERS_KEY = "parameters";

    /**
     * The key for specifying the base type of the coordinate operation to create. This optional entry
     * is used by {@code DefaultCoordinateOperationFactory.createSingleOperation(…)}. Apache SIS tries
     * to infer this value automatically, but this entry may help SIS to perform a better choice in
     * some cases. For example an "Affine" operation can be both a conversion or a transformation
     * (the later is used in datum shift in geocentric coordinates).
     */
    public static final String OPERATION_TYPE_KEY = "operationType";

    /**
     * The key for specifying a {@link MathTransformFactory} instance to use for geodetic object constructions.
     * This is usually not needed for CRS construction, except in the special case of a derived CRS created
     * from a defining conversion.
     */
    public static final String MT_FACTORY = "mtFactory";

    /**
     * The key for specifying a {@link CRSFactory} instance to use for geodetic object constructions.
     */
    public static final String CRS_FACTORY = "crsFactory";

    /**
     * The key for specifying a {@link CSFactory} instance to use for geodetic object constructions.
     */
    public static final String CS_FACTORY = "csFactory";

    /**
     * The separator character between an identifier and its namespace in the argument given to
     * {@link #getOperationMethod(String)}. For example this is the separator in {@code "EPSG:9807"}.
     *
     * This is defined as a constant for now, but we may make it configurable in a future version.
     */
    private static final char IDENTIFIER_SEPARATOR = DefaultNameSpace.DEFAULT_SEPARATOR;

    /**
     * The services, fetched when first needed.
     */
    private static volatile ReferencingServices instance;

    /**
     * For subclass only. This constructor registers this instance as a {@link SystemListener}
     * in order to force a new {@code ReferencingServices} lookup if the classpath changes.
     */
    protected ReferencingServices() {
        super(Modules.METADATA, "sis-referencing");
    }

    /**
     * Invoked when the classpath changed. Resets the {@link #instance} to {@code null}
     * in order to force the next call to {@link #getInstance()} to fetch a new one,
     * which may be different.
     */
    @Override
    protected final void classpathChanged() {
        synchronized (ReferencingServices.class) {
            super.classpathChanged();
            instance = null;
        }
    }

    /**
     * Returns the singleton instance.
     *
     * @return The singleton instance.
     */
    @SuppressWarnings("DoubleCheckedLocking")
    public static ReferencingServices getInstance() {
        ReferencingServices c = instance;
        if (c == null) {
            synchronized (ReferencingServices.class) {
                c = instance;
                if (c == null) {
                    /*
                     * Double-checked locking: okay since Java 5 provided that the 'instance' field is volatile.
                     * In the particular case of this class, the intend is to ensure that SystemListener.add(…)
                     * is invoked only once.
                     */
                    c = getInstance(ReferencingServices.class, Modules.METADATA, "sis-referencing",
                            "org.apache.sis.internal.referencing.ServicesForMetadata");
                    if (c == null) {
                        c = new ReferencingServices();
                    }
                    instance = c;
                }
            }
        }
        return c;
    }




    ///////////////////////////////////////////////////////////////////////////////////////
    ////                                                                               ////
    ////                        SERVICES FOR ISO 19115 METADATA                        ////
    ////                                                                               ////
    ///////////////////////////////////////////////////////////////////////////////////////

    /**
     * Sets a geographic bounding box from the specified envelope.
     * If the envelope contains a CRS which is not geographic, then the bounding box will be transformed
     * to a geographic CRS (without datum shift if possible). Otherwise, the envelope is assumed already
     * in a geographic CRS using (<var>longitude</var>, <var>latitude</var>) axis order.
     *
     * @param  envelope The source envelope.
     * @param  target The target bounding box.
     * @throws TransformException if the given envelope can't be transformed.
     * @throws UnsupportedOperationException if the {@code "sis-referencing"} module has not been found on the classpath.
     */
    public void setBounds(Envelope envelope, DefaultGeographicBoundingBox target) throws TransformException {
        throw moduleNotFound();
    }

    /**
     * Sets a vertical extent with the value inferred from the given envelope.
     * Only the vertical ordinates are extracted; all other ordinates are ignored.
     *
     * @param  envelope The source envelope.
     * @param  target The target vertical extent.
     * @throws TransformException if no vertical component can be extracted from the given envelope.
     * @throws UnsupportedOperationException if the {@code "sis-referencing"} module has not been found on the classpath.
     */
    public void setBounds(Envelope envelope, DefaultVerticalExtent target) throws TransformException {
        throw moduleNotFound();
    }

    /**
     * Sets a temporal extent with the value inferred from the given envelope.
     * Only the temporal ordinates are extracted; all other ordinates are ignored.
     *
     * @param  envelope The source envelope.
     * @param  target The target temporal extent.
     * @throws TransformException if no temporal component can be extracted from the given envelope.
     * @throws UnsupportedOperationException if the {@code "sis-referencing"} module has not been found on the classpath.
     */
    public void setBounds(Envelope envelope, DefaultTemporalExtent target) throws TransformException {
        throw moduleNotFound();
    }

    /**
     * Sets the geographic, vertical and temporal extents with the values inferred from the given envelope.
     * If the given {@code target} has more geographic or vertical extents than needed (0 or 1), then the
     * extraneous extents are removed.
     *
     * <p>Behavior regarding missing dimensions:</p>
     * <ul>
     *   <li>If the given envelope has no horizontal component, then all geographic extents are removed
     *       from the given {@code target}. Non-geographic extents (e.g. descriptions and polygons) are
     *       left unchanged.</li>
     *   <li>If the given envelope has no vertical component, then the vertical extent is set to {@code null}.</li>
     *   <li>If the given envelope has no temporal component, then the temporal extent is set to {@code null}.</li>
     * </ul>
     *
     * @param  envelope The source envelope.
     * @param  target The target spatio-temporal extent.
     * @throws TransformException if no temporal component can be extracted from the given envelope.
     * @throws UnsupportedOperationException if the {@code "sis-referencing"} module has not been found on the classpath.
     */
    public void setBounds(Envelope envelope, DefaultSpatialTemporalExtent target) throws TransformException {
        throw moduleNotFound();
    }

    /**
     * Initializes a horizontal, vertical and temporal extent with the values inferred from the given envelope.
     *
     * @param  envelope The source envelope.
     * @param  target The target extent.
     * @throws TransformException if a coordinate transformation was required and failed.
     * @throws UnsupportedOperationException if the {@code "sis-referencing"} module has not been found on the classpath.
     */
    public void addElements(Envelope envelope, DefaultExtent target) throws TransformException {
        throw moduleNotFound();
    }




    ///////////////////////////////////////////////////////////////////////////////////////
    ////                                                                               ////
    ////                          SERVICES FOR WKT FORMATTING                          ////
    ////                                                                               ////
    ///////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns a fully implemented parameter descriptor.
     *
     * @param  parameter A partially implemented parameter descriptor, or {@code null}.
     * @return A fully implemented parameter descriptor, or {@code null} if the given argument was null.
     * @throws UnsupportedOperationException if the {@code "sis-referencing"} module has not been found on the classpath.
     *
     * @since 0.5
     */
    public ParameterDescriptor<?> toImplementation(ParameterDescriptor<?> parameter) {
        throw moduleNotFound();
    }

    /**
     * Converts the given object in a {@code FormattableObject} instance.
     *
     * @param  object The object to wrap.
     * @return The given object converted to a {@code FormattableObject} instance.
     * @throws UnsupportedOperationException if the {@code "sis-referencing"} module has not been found on the classpath.
     *
     * @see org.apache.sis.referencing.AbstractIdentifiedObject#castOrCopy(IdentifiedObject)
     *
     * @since 0.4
     */
    public FormattableObject toFormattableObject(IdentifiedObject object) {
        throw moduleNotFound();
    }

    /**
     * Converts the given object in a {@code FormattableObject} instance. Callers should verify that the given
     * object is not already an instance of {@code FormattableObject} before to invoke this method. This method
     * returns {@code null} if it can not convert the object.
     *
     * @param  object The object to wrap.
     * @param  internal {@code true} if the formatting convention is {@code Convention.INTERNAL}.
     * @return The given object converted to a {@code FormattableObject} instance, or {@code null}.
     * @throws UnsupportedOperationException if the {@code "sis-referencing"} module has not been found on the classpath.
     *
     * @since 0.6
     */
    public FormattableObject toFormattableObject(MathTransform object, boolean internal) {
        throw moduleNotFound();
    }




    ///////////////////////////////////////////////////////////////////////////////////////
    ////                                                                               ////
    ////                           SERVICES FOR WKT PARSING                            ////
    ////                                                                               ////
    ///////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns a coordinate reference system for heights above the mean seal level.
     *
     * @return The "Mean Seal Level (MSL) height" coordinate reference system, or {@code null}.
     *
     * @since 0.6
     */
    public VerticalCRS getMSLH() {
        throw moduleNotFound();
    }

    /**
     * Returns the Greenwich prime meridian.
     *
     * @return The Greenwich prime meridian.
     *
     * @since 0.6
     */
    public PrimeMeridian getGreenwich() {
        throw moduleNotFound();
    }

    /**
     * Returns the coordinate system of a geocentric CRS using axes in the given unit of measurement.
     *
     * @param  unit The unit of measurement for the geocentric CRS axes.
     * @return The coordinate system for a geocentric CRS with axes using the given unit of measurement.
     *
     * @since 0.6
     */
    public CartesianCS getGeocentricCS(final Unit<Length> unit) {
        throw moduleNotFound();
    }

    /**
     * Converts a geocentric coordinate system from the legacy WKT 1 to the current ISO 19111 standard.
     * This method replaces the (Other, East, North) directions by (Geocentric X, Geocentric Y, Geocentric Z).
     *
     * @param  cs The geocentric coordinate system to upgrade.
     * @return The upgraded coordinate system, or {@code cs} if this method can not upgrade the given CS.
     *
     * @since 0.6
     */
    public CartesianCS upgradeGeocentricCS(final CartesianCS cs) {
        return cs;
    }

    /**
     * Creates a coordinate system of unknown type. This method is used during parsing of WKT version 1,
     * since that legacy format did not specified any information about the coordinate system in use.
     * This method should not need to be invoked for parsing WKT version 2.
     *
     * @param  properties The coordinate system name, and optionally other properties.
     * @param  axes The axes of the unknown coordinate system.
     * @return An "abstract" coordinate system using the given axes.
     *
     * @since 0.6
     */
    public CoordinateSystem createAbstractCS(final Map<String,?> properties, final CoordinateSystemAxis[] axes) {
        throw moduleNotFound();
    }

    /**
     * Creates a parametric CS. This method requires the SIS factory
     * since parametric CRS were not available in GeoAPI 3.0.
     *
     * @param  properties  the coordinate system name, and optionally other properties.
     * @param  axis        the axis of the parametric coordinate system.
     * @param  factory     the factory to use for creating the coordinate system.
     * @return a parametric coordinate system using the given axes.
     * @throws FactoryException if the parametric object creation failed.
     *
     * @since 0.7
     */
    public CoordinateSystem createParametricCS(final Map<String,?> properties, final CoordinateSystemAxis axis,
            final CSFactory factory) throws FactoryException
    {
        throw moduleNotFound();
    }

    /**
     * Creates a parametric datum. This method requires the SIS factory
     * since parametric CRS were not available in GeoAPI 3.0.
     *
     * @param  properties  the datum name, and optionally other properties.
     * @param  factory     the factory to use for creating the datum.
     * @return a parametric datum using the given name.
     * @throws FactoryException if the parametric object creation failed.
     *
     * @since 0.7
     */
    public Datum createParametricDatum(final Map<String,?> properties, final DatumFactory factory)
            throws FactoryException
    {
        throw moduleNotFound();
    }

    /**
     * Creates a parametric CRS. This method requires the SIS factory
     * since parametric CRS were not available in GeoAPI 3.0.
     *
     * @param  properties  the coordinate reference system name, and optionally other properties.
     * @param  datum       the parametric datum.
     * @param  cs          the parametric coordinate system.
     * @param  factory     the factory to use for creating the coordinate reference system.
     * @return a parametric coordinate system using the given axes.
     * @throws FactoryException if the parametric object creation failed.
     *
     * @since 0.7
     */
    public SingleCRS createParametricCRS(final Map<String,?> properties, final Datum datum,
            final CoordinateSystem cs, final CRSFactory factory) throws FactoryException
    {
        throw moduleNotFound();
    }

    /**
     * Creates a derived CRS from the information found in a WKT 1 {@code FITTED_CS} element.
     * This coordinate system can not be easily constructed from the information provided by the WKT 1 format.
     * Note that this method is needed only for WKT 1 parsing, since WKT provides enough information for using
     * the standard factories.
     *
     * @param  properties    The properties to be given to the {@code DerivedCRS} and {@code Conversion} objects.
     * @param  baseCRS       Coordinate reference system to base the derived CRS on.
     * @param  method        The coordinate operation method (mandatory in all cases).
     * @param  baseToDerived Transform from positions in the base CRS to positions in this target CRS.
     * @param  derivedCS     The coordinate system for the derived CRS.
     * @return The newly created derived CRS, potentially implementing an additional CRS interface.
     *
     * @since 0.6
     */
    public DerivedCRS createDerivedCRS(final Map<String,?>    properties,
                                       final SingleCRS        baseCRS,
                                       final OperationMethod  method,
                                       final MathTransform    baseToDerived,
                                       final CoordinateSystem derivedCS)
    {
        throw moduleNotFound();
    }

    /**
     * Creates a compound CRS, but we special processing for (two-dimensional Geographic + ellipsoidal heights) tupples.
     * If any such tupple is found, a three-dimensional geographic CRS is created instead than the compound CRS.
     *
     * @param  crsFactory The factory to use for creating compound or three-dimensional geographic CRS.
     * @param  csFactory  The factory to use for creating three-dimensional ellipsoidal CS, if needed.
     * @param  properties Name and other properties to give to the new object.
     * @param  components ordered array of {@code CoordinateReferenceSystem} objects.
     * @return The coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     *
     * @since 0.7
     */
    public final CoordinateReferenceSystem createCompoundCRS(final CRSFactory crsFactory, final CSFactory csFactory,
            final Map<String,?> properties, CoordinateReferenceSystem... components) throws FactoryException
    {
        for (int i=0; i<components.length; i++) {
            final CoordinateReferenceSystem vertical = components[i];
            if (vertical instanceof VerticalCRS) {
                final VerticalDatum datum = ((VerticalCRS) vertical).getDatum();
                if (datum != null && datum.getVerticalDatumType() == VerticalDatumTypes.ELLIPSOIDAL) {
                    int axisPosition = 0;
                    EllipsoidalCS cs = null;
                    CoordinateReferenceSystem crs = null;
                    if (i == 0 || (cs = getCsIfGeographic2D(crs = components[i - 1])) == null) {
                        /*
                         * GeographicCRS are normally before VerticalCRS. But Apache SIS is tolerant to the
                         * opposite order (note however that such ordering is illegal according ISO 19162).
                         */
                        if (i+1 >= components.length || (cs = getCsIfGeographic2D(crs = components[i + 1])) == null) {
                            continue;
                        }
                        axisPosition = 1;
                    }
                    /*
                     * At this point we have the horizontal and vertical components. The horizontal component
                     * begins at 'axisPosition', which is almost always zero. Create the three-dimensional CRS.
                     * If the result is the CRS to be returned directly by this method (components.length == 2),
                     * use the properties given in argument. Otherwise we need to use other properties; current
                     * implementation recycles the properties of the existing two-dimensional CRS.
                     */
                    final CoordinateSystemAxis[] axes = new CoordinateSystemAxis[3];
                    axes[axisPosition++   ] = cs.getAxis(0);
                    axes[axisPosition++   ] = cs.getAxis(1);
                    axes[axisPosition %= 3] = vertical.getCoordinateSystem().getAxis(0);
                    cs = csFactory.createEllipsoidalCS(getProperties(cs), axes[0], axes[1], axes[2]);
                    crs = crsFactory.createGeographicCRS((components.length == 2) ? properties : getProperties(crs),
                            ((GeodeticCRS) crs).getDatum(), cs);
                    /*
                     * Remove the VerticalCRS and store the three-dimensional GeographicCRS in place of the previous
                     * two-dimensional GeographicCRS. Then let the loop continues in case there is other CRS to merge
                     * (should never happen, but we are paranoiac).
                     */
                    components = ArraysExt.remove(components, i, 1);
                    if (axisPosition != 0) i--;             // GeographicCRS before VerticalCRS (usual case).
                    components[i] = crs;
                }
            }
        }
        switch (components.length) {
            case 0:  return null;
            case 1:  return components[0];
            default: return crsFactory.createCompoundCRS(properties, components);
        }
    }

    /**
     * Returns the coordinate system if the given CRS is a two-dimensional geographic CRS, or {@code null} otherwise.
     */
    private static EllipsoidalCS getCsIfGeographic2D(final CoordinateReferenceSystem crs) {
        if (crs instanceof GeodeticCRS) {
            final CoordinateSystem cs = crs.getCoordinateSystem();
            if (cs instanceof EllipsoidalCS && cs.getDimension() == 2) {
                return (EllipsoidalCS) cs;
            }
        }
        return null;
    }

    /**
     * Returns an axis direction from a pole along a meridian.
     * The given meridian is usually, but not necessarily, relative to the Greenwich meridian.
     *
     * @param  baseDirection The base direction, which must be {@link AxisDirection#NORTH} or {@link AxisDirection#SOUTH}.
     * @param  meridian The meridian in degrees, relative to a unspecified (usually Greenwich) prime meridian.
     *         Meridians in the East hemisphere are positive and meridians in the West hemisphere are negative.
     * @return The axis direction along the given meridian.
     *
     * @since 0.6
     */
    public AxisDirection directionAlongMeridian(final AxisDirection baseDirection, final double meridian) {
        throw moduleNotFound();
    }

    /**
     * Creates the {@code TOWGS84} element during parsing of a WKT version 1. This is an optional operation:
     * this method is allowed to return {@code null} if the "sis-referencing" module is not in the classpath.
     *
     * @param  values The 7 Bursa-Wolf parameter values.
     * @return The {@link org.apache.sis.referencing.datum.BursaWolfParameters}, or {@code null}.
     *
     * @since 0.6
     */
    public Object createToWGS84(final double[] values) {
        return null;
    }

    /**
     * Creates a single operation from the given properties.
     * This method is provided here because not yet available in GeoAPI interfaces.
     *
     * @param  properties The properties to be given to the identified object.
     * @param  sourceCRS  The source CRS.
     * @param  targetCRS  The target CRS.
     * @param  interpolationCRS The CRS of additional coordinates needed for the operation, or {@code null} if none.
     * @param  method     The coordinate operation method (mandatory in all cases).
     * @param  factory    The factory to use.
     * @return The coordinate operation created from the given arguments.
     * @throws FactoryException if the object creation failed.
     *
     * @since 0.6
     */
    public SingleOperation createSingleOperation(
            final Map<String,?>              properties,
            final CoordinateReferenceSystem  sourceCRS,
            final CoordinateReferenceSystem  targetCRS,
            final CoordinateReferenceSystem  interpolationCRS,
            final OperationMethod            method,
            final CoordinateOperationFactory factory) throws FactoryException
    {
        throw moduleNotFound();
    }

    /**
     * Returns the coordinate operation factory to use for the given properties and math transform factory.
     * If the given properties are empty and the {@code mtFactory} is the system default, then this method
     * returns the system default {@code CoordinateOperationFactory} instead of creating a new one.
     *
     * @param  properties The default properties.
     * @param  mtFactory  The math transform factory to use.
     * @param  crsFactory The factory to use if the operation factory needs to create CRS for intermediate steps.
     * @param  csFactory  The factory to use if the operation factory needs to create CS for intermediate steps.
     * @return The coordinate operation factory to use.
     *
     * @since 0.7
     */
    public CoordinateOperationFactory getCoordinateOperationFactory(Map<String,?> properties,
            final MathTransformFactory mtFactory, final CRSFactory crsFactory, final CSFactory csFactory)
    {
        /*
         * The check for 'properties' and 'mtFactory' is performed by the ServicesForMetadata subclass. If this code is
         * executed, this means that the "sis-referencing" module is not on the classpath, in which case we do not know
         * how to pass the 'properties' and 'mtFactory' arguments to the foreigner CoordinateOperationFactory anyway.
         */
        final CoordinateOperationFactory factory = DefaultFactories.forClass(CoordinateOperationFactory.class);
        if (factory != null) {
            return factory;
        } else {
            throw moduleNotFound();
        }
    }

    /**
     * Returns the properties of the given object.
     *
     * @param  object The object from which to get the properties.
     * @return The properties of the given object.
     *
     * @since 0.6
     */
    public Map<String,?> getProperties(final IdentifiedObject object) {
        return Collections.singletonMap(IdentifiedObject.NAME_KEY, object.getName());
    }

    /**
     * Returns {@code true} if the {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#getName()
     * primary name} or an aliases of the given object matches the given name. The comparison ignores case,
     * some Latin diacritical signs and any characters that are not letters or digits.
     *
     * @param  object The object for which to check the name or alias.
     * @param  name The name to compare with the object name or aliases.
     * @return {@code true} if the primary name of at least one alias matches the specified {@code name}.
     *
     * @since 0.6
     */
    public boolean isHeuristicMatchForName(final IdentifiedObject object, final String name) {
        return NameToIdentifier.isHeuristicMatchForName(object.getName(), object.getAlias(), name,
                NameToIdentifier.Simplifier.DEFAULT);
    }

    /**
     * Returns {@code true} if the name or an identifier of the given method matches the given {@code identifier}.
     *
     * @param  method     The method to test for a match.
     * @param  identifier The name or identifier of the operation method to search.
     * @return {@code true} if the given method is a match for the given identifier.
     *
     * @since 0.6
     */
    private boolean matches(final OperationMethod method, final String identifier) {
        if (isHeuristicMatchForName(method, identifier)) {
            return true;
        }
        for (int s = identifier.indexOf(IDENTIFIER_SEPARATOR); s >= 0;
                 s = identifier.indexOf(IDENTIFIER_SEPARATOR, s))
        {
            final String codespace = identifier.substring(0, s).trim();
            final String code = identifier.substring(++s).trim();
            for (final ReferenceIdentifier id : method.getIdentifiers()) {
                if (codespace.equalsIgnoreCase(id.getCodeSpace()) && code.equalsIgnoreCase(id.getCode())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the operation method for the specified name or identifier. The given argument shall be either a
     * method name (e.g. <cite>"Transverse Mercator"</cite>) or one of its identifiers (e.g. {@code "EPSG:9807"}).
     *
     * @param  methods The method candidates.
     * @param  identifier The name or identifier of the operation method to search.
     * @return The coordinate operation method for the given name or identifier, or {@code null} if none.
     *
     * @see org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory#getOperationMethod(String)
     * @see org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory#getOperationMethod(String)
     *
     * @since 0.6
     */
    public final OperationMethod getOperationMethod(final Iterable<? extends OperationMethod> methods, final String identifier) {
        OperationMethod fallback = null;
        for (final OperationMethod method : methods) {
            if (matches(method, identifier)) {
                /*
                 * Stop the iteration at the first non-deprecated method.
                 * If we find only deprecated methods, take the first one.
                 */
                if (!(method instanceof Deprecable) || !((Deprecable) method).isDeprecated()) {
                    return method;
                }
                if (fallback == null) {
                    fallback = method;
                }
            }
        }
        return fallback;
    }

    /**
     * Returns the coordinate operation method for the given classification.
     * This method checks if the given {@code opFactory} is a SIS implementation
     * before to fallback on a slower fallback.
     *
     * @param  opFactory  The coordinate operation factory to use if it is a SIS implementation.
     * @param  mtFactory  The math transform factory to use as a fallback.
     * @param  identifier The name or identifier of the operation method to search.
     * @return The coordinate operation method for the given name or identifier.
     * @throws FactoryException if an error occurred which searching for the given method.
     *
     * @since 0.6
     */
    public OperationMethod getOperationMethod(final CoordinateOperationFactory opFactory,
            final MathTransformFactory mtFactory, final String identifier) throws FactoryException
    {
        final OperationMethod method = getOperationMethod(mtFactory.getAvailableMethods(SingleOperation.class), identifier);
        if (method != null) {
            return method;
        }
        throw new NoSuchIdentifierException(Errors.format(Errors.Keys.NoSuchOperationMethod_1, identifier), identifier);
    }

    /**
     * Returns information about the Apache SIS configuration to be reported in {@link org.apache.sis.setup.About}.
     * This method is invoked only for aspects that depends on other modules than {@code sis-utility}.
     *
     * <p>Current keys are:</p>
     * <ul>
     *   <li>{@code "EPSG"}: version of EPSG database.</li>
     * </ul>
     *
     * @param  key A key identifying the information to return.
     * @param  locale Language to use if possible.
     * @return The information, or {@code null} if none.
     *
     * @see org.apache.sis.internal.util.MetadataServices#getInformation(String)
     *
     * @since 0.7
     */
    public String getInformation(String key, Locale locale) {
        return null;
    }
}
