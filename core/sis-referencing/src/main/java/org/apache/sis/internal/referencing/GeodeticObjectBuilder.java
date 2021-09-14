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
import java.util.Date;
import java.util.Collections;
import java.util.Locale;
import javax.measure.Unit;
import javax.measure.quantity.Time;
import javax.measure.quantity.Length;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.TimeCS;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.DatumFactory;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Conversion;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.referencing.provider.TransverseMercator;
import org.apache.sis.internal.referencing.provider.PolarStereographicA;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.measure.Latitude;
import org.apache.sis.referencing.Builder;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;


/**
 * Helper methods for building Coordinate Reference Systems and related objects.
 *
 * <p>For now, this class is defined in the internal package because this API needs more experimentation.
 * However this class may move in a public package later if we feel confident that its API is mature enough.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.6
 * @module
 */
public final class GeodeticObjectBuilder extends Builder<GeodeticObjectBuilder> {
    /**
     * The geodetic datum, or {@code null} if none.
     */
    private GeodeticDatum datum;

    /**
     * The name of the conversion to use for creating a {@code ProjectedCRS} or {@code DerivedCRS}.
     * This name is for information purpose; its value does not impact the numerical results of coordinate operations.
     *
     * @see #setConversionName(String)
     */
    private String conversionName;

    /**
     * The conversion method used by {@code ProjectedCRS} or {@code DerivedCRS}, or {@code null} if unspecified.
     *
     * @see #setConversionMethod(String)
     */
    private OperationMethod method;

    /**
     * The projection parameters, or {@code null} if not applicable.
     */
    private ParameterValueGroup parameters;

    /**
     * Group of factories used by this builder.
     */
    private final ReferencingFactoryContainer factories;

    /**
     * The locale for error messages, or {@code null} for default locale.
     */
    private final Locale locale;

    /**
     * Creates a new builder.
     */
    public GeodeticObjectBuilder() {
        this(null);
    }

    /**
     * Creates a new builder using the given locale for message in exceptions.
     *
     * @param  locale  the locale for error message in exceptions.
     */
    public GeodeticObjectBuilder(final Locale locale) {
        factories = new ReferencingFactoryContainer();
        this.locale = locale;
    }

    /**
     * Creates a new builder using the given factories and locale.
     *
     * @param  factories  the factories to use for geodetic objects creation.
     * @param  locale     the locale for error message in exceptions.
     */
    public GeodeticObjectBuilder(final ReferencingFactoryContainer factories, final Locale locale) {
        this.factories = factories;
        this.locale = locale;
    }

    /**
     * Creates a map of properties containing only the name of the given object.
     */
    private static Map<String,Object> name(final IdentifiedObject template) {
        return Collections.singletonMap(IdentifiedObject.NAME_KEY, template.getName());
    }

    /**
     * Sets the domain of validity as a geographic bounding box set to the specified values.
     * The bounding box crosses the anti-meridian if {@code eastBoundLongitude} &lt; {@code westBoundLongitude}.
     * If this method has already been invoked previously, the new value overwrites the previous one.
     *
     * @param  description         a textual description of the domain of validity, or {@code null} if none.
     * @param  westBoundLongitude  the minimal λ value.
     * @param  eastBoundLongitude  the maximal λ value.
     * @param  southBoundLatitude  the minimal φ value.
     * @param  northBoundLatitude  the maximal φ value.
     * @return {@code this}, for method call chaining.
     * @throws IllegalArgumentException if (<var>south bound</var> &gt; <var>north bound</var>).
     *         Note that {@linkplain Double#NaN NaN} values are allowed.
     */
    public GeodeticObjectBuilder setDomainOfValidity(final CharSequence description,
                    final double westBoundLongitude,
                    final double eastBoundLongitude,
                    final double southBoundLatitude,
                    final double northBoundLatitude)
    {
        DefaultGeographicBoundingBox bbox = new DefaultGeographicBoundingBox(
                westBoundLongitude, eastBoundLongitude, southBoundLatitude, northBoundLatitude);
        if (bbox.isEmpty()) {
            bbox = null;
        }
        if (description != null || bbox != null) {
            final DefaultExtent extent = new DefaultExtent(description, bbox, null, null);
            properties.put(CoordinateReferenceSystem.DOMAIN_OF_VALIDITY_KEY, extent);
        }
        return this;
    }

    /**
     * Creates a geodetic datum with an ellipsoid of the given shape.
     *
     * @param  name               ellipsoid and datum name.
     * @param  semiMajorAxis      equatorial radius in supplied linear units.
     * @param  inverseFlattening  eccentricity of ellipsoid. An infinite value creates a sphere.
     * @param  units              linear units of major axis.
     * @return {@code this}, for method call chaining.
     * @throws FactoryException if the datum can not be created.
     */
    public GeodeticObjectBuilder setFlattenedSphere(final String name, final double semiMajorAxis,
            final double inverseFlattening, final Unit<Length> units) throws FactoryException
    {
        final DatumFactory factory = factories.getDatumFactory();
        final Ellipsoid ellipsoid = factory.createFlattenedSphere(
                Collections.singletonMap(Ellipsoid.NAME_KEY, name), semiMajorAxis, inverseFlattening, units);
        datum = factory.createGeodeticDatum(name(ellipsoid), ellipsoid, CommonCRS.WGS84.primeMeridian());
        return this;
    }

    /**
     * Sets the conversion method to use for creating a {@code ProjectedCRS} or {@code DerivedCRS}.
     * The method is typically a map projection method. Examples:
     *
     * <ul>
     *   <li>Lambert Conic Conformal (1SP)</li>
     *   <li>Lambert Conic Conformal (2SP)</li>
     *   <li>Mercator (variant A)</li>
     *   <li>Mercator (variant B)</li>
     *   <li>Mercator (variant C)</li>
     *   <li>Popular Visualisation Pseudo Mercator</li>
     * </ul>
     *
     * This method can be invoked only once.
     *
     * @param  name  name of the conversion method.
     * @return {@code this}, for method call chaining.
     * @throws FactoryException if the operation method of the given name can not be obtained.
     */
    public GeodeticObjectBuilder setConversionMethod(final String name) throws FactoryException {
        if (method != null) {
            throw new IllegalStateException(Errors.getResources(locale).getString(Errors.Keys.ElementAlreadyPresent_1, "OperationMethod"));
        }
        method = factories.getCoordinateOperationFactory().getOperationMethod(name);
        parameters = method.getParameters().createValue();
        return this;
    }

    /**
     * Sets the name of the conversion to use for creating a {@code ProjectedCRS} or {@code DerivedCRS}.
     * This name is for information purpose; its value does not impact the numerical results of coordinate operations.
     *
     * @param  name  the name to give to the conversion.
     * @return {@code this}, for method calls chaining.
     */
    public GeodeticObjectBuilder setConversionName(final String name) {
        conversionName = name;
        return this;
    }

    /**
     * Sets the conversion method together with all parameters. This method does not set the conversion name.
     * If a name different than the default is desired, {@link #setConversionName(String)} should be invoked.
     *
     * @param  parameters  the map projection parameter values.
     * @return {@code this}, for method calls chaining.
     * @throws FactoryException if the operation method can not be obtained.
     */
    public GeodeticObjectBuilder setConversion(final ParameterValueGroup parameters) throws FactoryException {
        ArgumentChecks.ensureNonNull("parameters", parameters);
        method = factories.getCoordinateOperationFactory().getOperationMethod(parameters.getDescriptor().getName().getCode());
        this.parameters = parameters;           // Set only if above line succeed.
        return this;
    }

    /**
     * Ensures that {@link #setConversionMethod(String)} has been invoked.
     */
    private void ensureConversionMethodSet() {
        if (parameters == null) {
            throw new IllegalStateException(Resources.forLocale(locale).getString(Resources.Keys.UnspecifiedParameterValues));
        }
    }

    /**
     * Sets the value of a numeric parameter. The {@link #setConversionMethod(String)} method must have been invoked
     * exactly once before this method. Calls to this {@code setParameter(…)} can be repeated as many times as needed.
     *
     * @param  name   the parameter name.
     * @param  value  the value to give to the parameter.
     * @param  unit   unit of measurement for the given value.
     * @return {@code this}, for method calls chaining.
     * @throws IllegalStateException if {@link #setConversionMethod(String)} has not been invoked before this method.
     * @throws ParameterNotFoundException if there is no parameter of the given name.
     * @throws InvalidParameterValueException if the parameter does not accept the given value.
     */
    public GeodeticObjectBuilder setParameter(final String name, final double value, final Unit<?> unit)
            throws IllegalStateException, ParameterNotFoundException, InvalidParameterValueException
    {
        ensureConversionMethodSet();
        parameters.parameter(name).setValue(value, unit);
        return this;
    }

    /**
     * Sets the operation method, parameters and conversion name for a Transverse Mercator projection.
     * This convenience method delegates to the following methods:
     *
     * <ul>
     *   <li>{@link #setConversionName(String)} with a name like <cite>"Transverse Mercator"</cite>
     *       or <cite>"UTM zone 10N"</cite>, depending on the arguments given to this method.</li>
     *   <li>{@link #setConversionMethod(String)} with the name of the Transverse Mercator projection method.</li>
     *   <li>{@link #setParameter(String, double, Unit)} for each of the parameters enumerated below:</li>
     * </ul>
     *
     * <blockquote><table class="sis">
     *   <caption>Transverse Mercator parameters</caption>
     *   <tr><th>Parameter name</th>                 <th>Parameter value</th></tr>
     *   <tr><td>Latitude of natural origin</td>     <td>Given latitude, snapped to 0° in the UTM case</td></tr>
     *   <tr><td>Longitude of natural origin</td>    <td>Given longitude, optionally snapped to a UTM zone</td></tr>
     *   <tr><td>Scale factor at natural origin</td> <td>0.9996 in UTM case</td></tr>
     *   <tr><td>False easting</td>                  <td>500000 metres in UTM case</td></tr>
     *   <tr><td>False northing</td>                 <td>0 (North hemisphere) or 10000000 (South hemisphere) metres</td></tr>
     * </table></blockquote>
     *
     * Note that calculation of UTM zone contains special cases for Norway and Svalbard.
     * If not desired, those exceptions can be avoided by making sure that the given latitude is below 56°N.
     *
     * <p>If the given {@code zoner} is {@link TransverseMercator.Zoner#ANY ANY}, then this method will use the given
     * latitude and longitude verbatim (without snapping them to a zone) but will still use the UTM scale factor,
     * false easting and false northing.
     *
     * @param  zoner      whether to use UTM or MTM zones, or {@code ANY} for using arbitrary central meridian.
     * @param  latitude   the latitude in the center of the desired projection.
     * @param  longitude  the longitude in the center of the desired projection.
     * @return {@code this}, for method calls chaining.
     * @throws FactoryException if the operation method for the Transverse Mercator projection can not be obtained.
     *
     * @see CommonCRS#universal(double, double)
     */
    public GeodeticObjectBuilder setTransverseMercator(TransverseMercator.Zoner zoner, double latitude, double longitude)
            throws FactoryException
    {
        ArgumentChecks.ensureBetween("latitude",   Latitude.MIN_VALUE,     Latitude.MAX_VALUE,     latitude);
        ArgumentChecks.ensureBetween("longitude", -Formulas.LONGITUDE_MAX, Formulas.LONGITUDE_MAX, longitude);
        setConversionMethod(TransverseMercator.NAME);
        setConversionName(zoner.setParameters(parameters, latitude, longitude));
        return this;
    }

    /**
     * Sets the operation method, parameters and conversion name for a Polar Stereographic projection.
     * This convenience method delegates to the following methods:
     *
     * <ul>
     *   <li>{@link #setConversionName(String)} with a name like <cite>"Universal Polar Stereographic North"</cite>,
     *       depending on the argument given to this method.</li>
     *   <li>{@link #setConversionMethod(String)} with the name of the Polar Stereographic (variant A) projection method.</li>
     *   <li>{@link #setParameter(String, double, Unit)} for each of the parameters enumerated below:</li>
     * </ul>
     *
     * <blockquote><table class="sis">
     *   <caption>Universal Polar Stereographic parameters</caption>
     *   <tr><th>Parameter name</th>                 <th>Parameter value</th></tr>
     *   <tr><td>Latitude of natural origin</td>     <td>90°N or 90°S</td></tr>
     *   <tr><td>Longitude of natural origin</td>    <td>0°</td></tr>
     *   <tr><td>Scale factor at natural origin</td> <td>0.994</td></tr>
     *   <tr><td>False easting</td>                  <td>2000000 metres</td></tr>
     *   <tr><td>False northing</td>                 <td>2000000 metres</td></tr>
     * </table></blockquote>
     *
     * @param  north  {@code true} for North pole, or {@code false} for South pole.
     * @return {@code this}, for method calls chaining.
     * @throws FactoryException if the operation method for the Polar Stereographic (variant A)
     *         projection can not be obtained.
     */
    public GeodeticObjectBuilder setPolarStereographic(final boolean north) throws FactoryException {
        setConversionMethod(PolarStereographicA.NAME);
        setConversionName(PolarStereographicA.setParameters(parameters, north));
        return this;
    }

    /**
     * Creates a projected CRS using a conversion built from the values given by the {@code setParameter(…)} calls.
     *
     * <div class="note"><b>Example:</b>
     * The following example creates a projected CRS for the <cite>"NTF (Paris) / Lambert zone II"</cite> projection,
     * from a base CRS which is presumed to already exists in this example.
     *
     * {@preformat java
     *   GeodeticObjectBuilder builder = new GeodeticObjectBuilder();
     *   GeographicCRS baseCRS = ...;
     *   CartesianCS derivedCS = ...;
     *   ProjectedCRS crs = builder
     *           .setConversionMethod("Lambert Conic Conformal (1SP)")
     *           .setConversionName("Lambert zone II")
     *           .setParameter("Latitude of natural origin",             52, Units.GRAD)
     *           .setParameter("Scale factor at natural origin", 0.99987742, Units.UNITY)
     *           .setParameter("False easting",                      600000, Units.METRE)
     *           .setParameter("False northing",                    2200000, Units.METRE)
     *           .addName("NTF (Paris) / Lambert zone II")
     *           .createProjectedCRS(baseCRS, derivedCS);
     * }
     * </div>
     *
     * @param  baseCRS    coordinate reference system to base the derived CRS on.
     * @param  derivedCS  the coordinate system for the derived CRS, or {@code null} for the default.
     * @return the projected CRS.
     * @throws FactoryException if an error occurred while building the projected CRS.
     */
    public ProjectedCRS createProjectedCRS(final GeographicCRS baseCRS, CartesianCS derivedCS) throws FactoryException {
        ensureConversionMethodSet();
        onCreate(false);
        try {
            /*
             * Create a conversion with the same properties than the ProjectedCRS properties,
             * except the aliases and identifiers. The name defaults to the ProjectedCRS name,
             * but can optionally be different.
             */
            final Object name = (conversionName != null) ? properties.put(Conversion.NAME_KEY, conversionName) : null;
            final Object alias = properties.put(Conversion.ALIAS_KEY, null);
            final Object identifier = properties.put(Conversion.IDENTIFIERS_KEY, null);
            final Conversion conversion = factories.getCoordinateOperationFactory().createDefiningConversion(properties, method, parameters);
            /*
             * Restore the original properties and create the final ProjectedCRS.
             */
            properties.put(Conversion.IDENTIFIERS_KEY, identifier);
            properties.put(Conversion.ALIAS_KEY, alias);
            if (name != null) {
                properties.put(Conversion.NAME_KEY, name);
            }
            if (derivedCS == null) {
                derivedCS = factories.getStandardProjectedCS();
            }
            return factories.getCRSFactory().createProjectedCRS(properties, baseCRS, conversion, derivedCS);
        } finally {
            onCreate(true);
        }
    }

    /**
     * Creates a projected CRS with base CRS on the specified datum and with default axes.
     * The base CRS uses the ellipsoid specified by {@link #setFlattenedSphere(String, double, double, Unit)}.
     *
     * @return the projected CRS.
     * @throws FactoryException if an error occurred while building the projected CRS.
     */
    public ProjectedCRS createProjectedCRS() throws FactoryException {
        GeographicCRS crs = CommonCRS.WGS84.geographic();
        if (datum != null) {
            crs = factories.getCRSFactory().createGeographicCRS(name(datum), datum, crs.getCoordinateSystem());
        }
        return createProjectedCRS(crs, factories.getStandardProjectedCS());
    }

    /**
     * Creates a geographic CRS.
     *
     * @param  normalized  whether axes should be in (longitude, latitude) order instead of (latitude, longitude).
     * @return the geographic coordinate reference system.
     * @throws FactoryException if an error occurred while building the geographic CRS.
     */
    public GeographicCRS createGeographicCRS(final boolean normalized) throws FactoryException {
        final GeographicCRS crs = normalized ? CommonCRS.WGS84.geographic() : CommonCRS.defaultGeographic();
        if (datum != null) properties.putIfAbsent(GeographicCRS.NAME_KEY, datum.getName());
        return factories.getCRSFactory().createGeographicCRS(properties, datum, crs.getCoordinateSystem());
    }

    /**
     * Creates a temporal CRS from the given origin and temporal unit. For this method, the CRS name is optional:
     * if no {@code addName(…)} method has been invoked, then a default name will be used.
     *
     * @param  origin  the epoch in milliseconds since January 1st, 1970 at midnight UTC.
     * @param  unit    the unit of measurement.
     * @return a temporal CRS using the given origin and units.
     * @throws FactoryException if an error occurred while building the temporal CRS.
     */
    public TemporalCRS createTemporalCRS(final Date origin, final Unit<Time> unit) throws FactoryException {
        /*
         * Try to use one of the pre-defined datum and coordinate system if possible.
         * This not only saves a little bit of memory, but also provides better names.
         */
        TimeCS cs = null;
        TemporalDatum datum = null;
        for (final CommonCRS.Temporal c : CommonCRS.Temporal.values()) {
            if (datum == null) {
                final TemporalDatum candidate = c.datum();
                if (origin.equals(candidate.getOrigin())) {
                    datum = candidate;
                }
            }
            if (cs == null) {
                final TemporalCRS crs = c.crs();
                final TimeCS candidate = crs.getCoordinateSystem();
                if (unit.equals(candidate.getAxis(0).getUnit())) {
                    if (datum == candidate && properties.isEmpty()) {
                        return crs;
                    }
                    cs = candidate;
                }
            }
        }
        /*
         * Create the datum and coordinate system before the CRS if we were not able to use a pre-defined object.
         * In the datum case, we will use the same metadata than the CRS (domain of validity, scope, etc.) except
         * the identifier and the remark.
         */
        onCreate(false);
        try {
            if (cs == null) {
                final CSFactory csFactory = factories.getCSFactory();
                cs = CommonCRS.Temporal.JAVA.crs().getCoordinateSystem();   // To be used as a template, except for units.
                cs = csFactory.createTimeCS(name(cs),
                     csFactory.createCoordinateSystemAxis(name(cs.getAxis(0)), "t", AxisDirection.FUTURE, unit));
            }
            if (properties.get(TemporalCRS.NAME_KEY) == null) {
                properties.putAll(name(cs));
            }
            if (datum == null) {
                final Object remarks    = properties.remove(TemporalCRS.REMARKS_KEY);
                final Object identifier = properties.remove(TemporalCRS.IDENTIFIERS_KEY);
                datum = factories.getDatumFactory().createTemporalDatum(properties, origin);
                properties.put(TemporalCRS.IDENTIFIERS_KEY, identifier);
                properties.put(TemporalCRS.REMARKS_KEY,     remarks);
                properties.put(TemporalCRS.NAME_KEY, datum.getName());      // Share the Identifier instance.
            }
            return factories.getCRSFactory().createTemporalCRS(properties, datum, cs);
        } finally {
            onCreate(true);
        }
    }

    /**
     * Creates a compound CRS, but we special processing for (two-dimensional Geographic + ellipsoidal heights) tuples.
     * If any such tuple is found, a three-dimensional geographic CRS is created instead of the compound CRS.
     *
     * @param  components  ordered array of {@code CoordinateReferenceSystem} objects.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    public CoordinateReferenceSystem createCompoundCRS(final CoordinateReferenceSystem... components) throws FactoryException {
        return new EllipsoidalHeightCombiner(factories).createCompoundCRS(properties, components);
    }

    /**
     * Replaces the component starting at given index by the given component. This method can be used for replacing
     * e.g. the horizontal component of a CRS, or the vertical component, <i>etc.</i>. If a new compound CRS needs
     * to be created and a {@linkplain #addName(org.opengis.util.GenericName) name has been specified}, that name
     * will be used.
     *
     * <h4>Limitations</h4>
     * Current implementation can replace exactly one component of {@link CompoundCRS}.
     * If the given replacement spans more than one component, then this method will fail.
     *
     * @param  source          the coordinate reference system in which to replace a component.
     * @param  firstDimension  index of the first dimension to replace.
     * @param  replacement     the component to insert in place of the CRS component at given index.
     * @return a CRS with the component replaced.
     * @throws FactoryException if the object creation failed.
     *
     * @see org.apache.sis.referencing.CRS#getComponentAt(CoordinateReferenceSystem, int, int)
     */
    public CoordinateReferenceSystem replaceComponent(final CoordinateReferenceSystem source,
            final int firstDimension, final CoordinateReferenceSystem replacement) throws FactoryException
    {
        final int srcDim = ReferencingUtilities.getDimension(source);
        final int repDim = ReferencingUtilities.getDimension(replacement);
        if (firstDimension == 0 && srcDim == repDim) {
            return replacement;
        }
        ArgumentChecks.ensureValidIndex(srcDim - repDim, firstDimension);
        if (source instanceof CompoundCRS) {
            final CoordinateReferenceSystem[] components = CollectionsExt.toArray(((CompoundCRS) source).getComponents(), CoordinateReferenceSystem.class);
            int lower = 0;
            for (int i=0; i<components.length; i++) {
                final CoordinateReferenceSystem c = components[i];
                if (firstDimension >= lower) {
                    /*
                     * Reached the index of the CRS component to replace. Invoke this method recursively in case we have nested
                     * components, but without using the names and identifiers that may have been specified for the final CRS.
                     */
                    Object name  = properties.remove(IdentifiedObject.NAME_KEY);
                    Object alias = properties.remove(IdentifiedObject.ALIAS_KEY);
                    Object ids   = properties.remove(IdentifiedObject.IDENTIFIERS_KEY);
                    final CoordinateReferenceSystem nc = replaceComponent(c, firstDimension - lower, replacement);
                    /*
                     * Restore the names and identifiers before to create the final CompoundCRS. If no name was specified,
                     * reuse the primary name of existing CRS but not the identifiers.
                     */
                    if (name == null) {
                        name = source.getName();
                    }
                    properties.put(IdentifiedObject.NAME_KEY, name);
                    properties.put(IdentifiedObject.ALIAS_KEY, alias);
                    properties.put(IdentifiedObject.IDENTIFIERS_KEY, ids);
                    if (nc == c) {
                        return source;                      // No change.
                    }
                    components[i] = nc;
                    return createCompoundCRS(components);
                }
                lower += ReferencingUtilities.getDimension(c);
            }
        }
        throw new IllegalArgumentException(Resources.forLocale(locale).getString(
                Resources.Keys.CanNotSeparateCRS_1, IdentifiedObjects.getDisplayName(source, locale)));
    }
}
