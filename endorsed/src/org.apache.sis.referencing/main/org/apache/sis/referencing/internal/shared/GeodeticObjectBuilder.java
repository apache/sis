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
package org.apache.sis.referencing.internal.shared;

import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.time.temporal.Temporal;
import javax.measure.Unit;
import javax.measure.quantity.Time;
import javax.measure.quantity.Length;
import org.opengis.util.GenericName;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.GeneralParameterValue;
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
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.Builder;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.provider.TransverseMercator;
import org.apache.sis.referencing.operation.provider.PolarStereographicA;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.measure.Latitude;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.parameter.Parameters;

// Specific to the main branch:
import org.apache.sis.temporal.TemporalDate;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.operation.transform.MathTransformBuilder;


/**
 * Helper methods for building Coordinate Reference Systems and related objects.
 *
 * In current version, each builder instance should be used for creating only one CRS.
 * Reusing the same builder for creating many CRS has unspecified behavior.
 *
 * <p>For now, this class is defined in the internal package because this API needs more experimentation.
 * However, this class may move in a public package later if we feel confident that its API is mature enough.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class GeodeticObjectBuilder extends Builder<GeodeticObjectBuilder> {
    /**
     * The geodetic reference frame, or {@code null} if none.
     *
     * @see #getDatumOrEnsemble()
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
     * Whether to use the axis order defined by {@link AxesConvention#NORMALIZED}.
     *
     * @see CommonCRS#normalizedGeographic()
     */
    private boolean normalizedAxisOrder;

    /**
     * Creates a new builder with default locale and set of factories.
     */
    public GeodeticObjectBuilder() {
        this(null, null);
    }

    /**
     * Creates a new builder using the given factories and locale.
     *
     * @param  factories  the factories to use for geodetic objects creation, or {@code null} for default.
     * @param  locale     the locale for error message in exceptions, or {@code null} for default.
     */
    public GeodeticObjectBuilder(final ReferencingFactoryContainer factories, final Locale locale) {
        this.factories = (factories != null) ? factories : new ReferencingFactoryContainer();
        this.locale = locale;
    }

    /**
     * Creates a map of properties containing only the name of the given object.
     */
    private static Map<String,Object> name(final IdentifiedObject template) {
        return Map.of(IdentifiedObject.NAME_KEY, template.getName());
    }

    /**
     * Sets whether axes should be in (longitude, latitude) order instead of (latitude, longitude).
     * This flag applies to geographic CRS created by this builder.
     *
     * @param  normalized  whether axes should be in (longitude, latitude) order instead of (latitude, longitude).
     * @return {@code this}, for method call chaining.
     *
     * @see AxesConvention#NORMALIZED
     * @see CommonCRS#normalizedGeographic()
     */
    public GeodeticObjectBuilder setNormalizedAxisOrder(final boolean normalized) {
        normalizedAxisOrder = normalized;
        return this;
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
     * Creates a geodetic reference frame with an ellipsoid of the given shape.
     *
     * @param  name               ellipsoid and datum name.
     * @param  semiMajorAxis      equatorial radius in supplied linear units.
     * @param  inverseFlattening  eccentricity of ellipsoid. An infinite value creates a sphere.
     * @param  units              linear units of major axis.
     * @return {@code this}, for method call chaining.
     * @throws FactoryException if the datum cannot be created.
     */
    public GeodeticObjectBuilder setFlattenedSphere(final String name, final double semiMajorAxis,
            final double inverseFlattening, final Unit<Length> units) throws FactoryException
    {
        final DatumFactory factory = factories.getDatumFactory();
        final Ellipsoid ellipsoid = factory.createFlattenedSphere(
                Map.of(Ellipsoid.NAME_KEY, name), semiMajorAxis, inverseFlattening, units);
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
     * @throws FactoryException if the operation method of the given name cannot be obtained.
     */
    public GeodeticObjectBuilder setConversionMethod(final String name) throws FactoryException {
        if (method != null) {
            throw new IllegalStateException(Errors.forLocale(locale).getString(Errors.Keys.ElementAlreadyPresent_1, "OperationMethod"));
        }
        method = factories.findOperationMethod(name);
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
     * @param  builder  the map projection parameter values.
     * @return {@code this}, for method calls chaining.
     * @throws FactoryException if the operation method cannot be obtained.
     */
    public GeodeticObjectBuilder setConversion(final MathTransformBuilder builder) throws FactoryException {
        method = builder.getMethod().orElseThrow(() -> new FactoryException());
        parameters = builder.parameters();    // Set only if above line succeed.
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
     * Replaces the current operation method by a new one with parameter values derived form the old method.
     * This method can be invoked for replacing a projection by another one with a similar set of parameters.
     *
     * <p>If non-null, the given {@code mapper} is used for copying parameter values from the old projection.
     * The {@code accept(ParameterValue<?> source, Parameters target)} method is invoked where {@code source}
     * is a parameter value of the old projection and {@code target} is the group of parameters where to set
     * the values for new projection. If {@code mapper} is null, then the default implementation is as below:</p>
     *
     * {@snippet lang="java" :
     *     target.getOrCreate(source.getDescriptor()).setValue(source.getValue());
     *     }
     *
     * @param  newMethod  name of the new operation method, or {@code null} if no change.
     * @param  mapper     mapper from old parameters to new parameters, or {@code null} for verbatim copy.
     * @return {@code this}, for method calls chaining.
     * @throws IllegalStateException if {@link #setConversionMethod(String)} has not been invoked before this method.
     * @throws FactoryException if the operation method of the given name cannot be obtained.
     * @throws ClassCastException if a parameter value of the old projection is not an instance of {@link ParameterValue}
     *         (this restriction may change in a future version).
     */
    public GeodeticObjectBuilder changeConversion(final String newMethod,
            BiConsumer<ParameterValue<?>, Parameters> mapper) throws FactoryException
    {
        ensureConversionMethodSet();
        if (mapper == null) {
            mapper = GeodeticObjectBuilder::copyParameterValue;
        }
        final ParameterValueGroup source = parameters;
        if (newMethod != null) {
            method = null;
            setConversionMethod(newMethod);
        }
        final Parameters target = Parameters.castOrWrap(parameters);
        for (final GeneralParameterValue param : source.values()) {
            mapper.accept((ParameterValue<?>) param, target);       // ClassCastException is part of current method contract.
        }
        return this;
    }

    /**
     * The default {@code mapper} of {@code changeConversion(String, BiConsumer)}.
     *
     * @param  source  parameter value of the old projection.
     * @param  target  group of parameters of the new projection where to copy source parameter value.
     */
    private static void copyParameterValue(final ParameterValue<?> source, final Parameters target) {
        target.getOrCreate(source.getDescriptor()).setValue(source.getValue());
    }

    /**
     * Sets the operation method, parameters, conversion name and datum for the same projection as the given CRS.
     * Metadata such as domain of validity are inherited, except identifiers.
     *
     * @param  crs  the projected CRS from which to inherit the properties.
     * @return {@code this}, for method call chaining.
     */
    public GeodeticObjectBuilder apply(final ProjectedCRS crs) {
        final Conversion c = crs.getConversionFromBase();
        conversionName = c.getName().getCode();
        method         = c.getMethod();
        parameters     = c.getParameterValues();
        datum          = DatumOrEnsemble.asDatum(crs.getBaseCRS());
        properties.putAll(IdentifiedObjects.getProperties(crs, ProjectedCRS.IDENTIFIERS_KEY));
        return this;
    }

    /**
     * Sets the operation method, parameters and conversion name for a Transverse Mercator projection.
     * This convenience method delegates to the following methods:
     *
     * <ul>
     *   <li>{@link #setConversionName(String)} with a name like <q>Transverse Mercator</q>
     *       or <q>UTM zone 10N</q>, depending on the arguments given to this method.</li>
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
     * @throws FactoryException if the operation method for the Transverse Mercator projection cannot be obtained.
     *
     * @see CommonCRS#universal(double, double)
     */
    public GeodeticObjectBuilder applyTransverseMercator(TransverseMercator.Zoner zoner,
            double latitude, double longitude) throws FactoryException
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
     *   <li>{@link #setConversionName(String)} with a name like <q>Universal Polar Stereographic North</q>,
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
     *         projection cannot be obtained.
     */
    public GeodeticObjectBuilder applyPolarStereographic(final boolean north) throws FactoryException {
        setConversionMethod(PolarStereographicA.NAME);
        setConversionName(PolarStereographicA.setParameters(parameters, north));
        return this;
    }

    /**
     * Creates a projected CRS using a conversion built from the values given by the {@code setParameter(…)} calls.
     *
     * <h4>Example</h4>
     * The following example creates a projected CRS for the <q>NTF (Paris) / Lambert zone II</q> projection,
     * from a base CRS which is presumed to already exists in this example.
     *
     * {@snippet lang="java" :
     *     var builder = new GeodeticObjectBuilder();
     *     GeographicCRS baseCRS = ...;
     *     CartesianCS derivedCS = ...;
     *     ProjectedCRS crs = builder
     *             .setConversionMethod("Lambert Conic Conformal (1SP)")
     *             .setConversionName("Lambert zone II")
     *             .setParameter("Latitude of natural origin",             52, Units.GRAD)
     *             .setParameter("Scale factor at natural origin", 0.99987742, Units.UNITY)
     *             .setParameter("False easting",                      600000, Units.METRE)
     *             .setParameter("False northing",                    2200000, Units.METRE)
     *             .addName("NTF (Paris) / Lambert zone II")
     *             .createProjectedCRS(baseCRS, derivedCS);
     *     }
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
             * Create a conversion with the same properties as the ProjectedCRS properties,
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
     * Creates a projected CRS with base CRS on the datum previously specified to this builder and with default axes.
     * The base CRS uses the ellipsoid specified by {@link #setFlattenedSphere(String, double, double, Unit)}.
     *
     * @return the projected CRS.
     * @throws FactoryException if an error occurred while building the projected CRS.
     */
    public ProjectedCRS createProjectedCRS() throws FactoryException {
        GeographicCRS crs = getBaseCRS();
        final IdentifiedObject id = getDatumOrEnsemble();
        if (id != null) {
            crs = factories.getCRSFactory().createGeographicCRS(name(id), datum, crs.getCoordinateSystem());
        }
        return createProjectedCRS(crs, factories.getStandardProjectedCS());
    }

    /**
     * Returns the datum if defined, or the datum ensemble otherwise.
     * Both of them may be {@code null}.
     */
    private IdentifiedObject getDatumOrEnsemble() {
        return datum;   // There is more code in the GeoAPI 3.1/4.0 branches.
    }

    /**
     * Returns the CRS to use as the base of a projected CRS.
     *
     * @todo {@code CommonCRS.WGS84} should be {@code CommonCRS.DEFAULT}, but the latter is not public.
     */
    private GeographicCRS getBaseCRS() {
        return normalizedAxisOrder ? CommonCRS.defaultGeographic() : CommonCRS.WGS84.geographic();
    }

    /**
     * Creates a geographic CRS.
     *
     * @return the geographic coordinate reference system.
     * @throws FactoryException if an error occurred while building the geographic CRS.
     */
    public GeographicCRS createGeographicCRS() throws FactoryException {
        final GeographicCRS crs = getBaseCRS();
        final IdentifiedObject id = getDatumOrEnsemble();
        if (id != null) {
            properties.putIfAbsent(GeographicCRS.NAME_KEY, id.getName());
        }
        return factories.getCRSFactory().createGeographicCRS(properties, datum, crs.getCoordinateSystem());
    }

    /**
     * Creates a temporal CRS from the given origin and temporal unit. For this method, the CRS name is optional:
     * if no {@code addName(…)} method has been invoked, then a default name will be used.
     *
     * @param  origin  the origin of the temporal datum.
     * @param  unit    the unit of measurement.
     * @return a temporal CRS using the given origin and units.
     * @throws FactoryException if an error occurred while building the temporal CRS.
     */
    public TemporalCRS createTemporalCRS(final Temporal origin, final Unit<Time> unit) throws FactoryException {
        /*
         * Try to use one of the predefined datum and coordinate system if possible.
         * This not only saves a little bit of memory, but also provides better names.
         */
        TimeCS cs = null;
        TemporalDatum td = null;
        for (final CommonCRS.Temporal c : CommonCRS.Temporal.values()) {
            if (td == null) {
                final TemporalDatum candidate = c.datum();
                if (origin.equals(candidate.getOrigin())) {
                    td = candidate;
                }
            }
            if (cs == null) {
                final TemporalCRS crs = c.crs();
                final TimeCS candidate = crs.getCoordinateSystem();
                if (unit.equals(candidate.getAxis(0).getUnit())) {
                    if (td == candidate && properties.isEmpty()) {
                        return crs;
                    }
                    cs = candidate;
                }
            }
        }
        /*
         * Create the datum and coordinate system before the CRS if we were not able to use a predefined object.
         * In the datum case, we will use the same metadata as the CRS (domain of validity, scope, etc.) except
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
            if (td == null) {
                final Object remarks    = properties.remove(TemporalCRS.REMARKS_KEY);
                final Object identifier = properties.remove(TemporalCRS.IDENTIFIERS_KEY);
                td = factories.getDatumFactory().createTemporalDatum(properties, TemporalDate.toDate(origin));
                properties.put(TemporalCRS.IDENTIFIERS_KEY, identifier);
                properties.put(TemporalCRS.REMARKS_KEY,     remarks);
                properties.put(TemporalCRS.NAME_KEY, td.getName());     // Share the Identifier instance.
            }
            return factories.getCRSFactory().createTemporalCRS(properties, td, cs);
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
     * to be created and a {@linkplain #addName(GenericName) name has been specified}, that name will be used.
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
        final int srcDim = CRS.getDimensionOrZero(source);
        final int repDim = CRS.getDimensionOrZero(replacement);
        if (firstDimension == 0 && srcDim == repDim) {
            /*
             * conceptually return the replacement. But returning the original instance if applicable
             * allows the caller to detect that a compound CRS does not need to be replaced.
             */
            return source.equals(replacement) ? source : replacement;
        }
        Objects.checkIndex(firstDimension, srcDim - repDim);
        if (source instanceof CompoundCRS) {
            final var components = ((CompoundCRS) source).getComponents().toArray(CoordinateReferenceSystem[]::new);
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
                     * Restore the names and identifiers before to create the final CompoundCRS.
                     * If no name was specified, reuse the primary name of existing CRS but not the identifiers.
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
                lower += CRS.getDimensionOrZero(c);
            }
        }
        throw new IllegalArgumentException(Resources.forLocale(locale).getString(
                Resources.Keys.CanNotSeparateCRS_1, IdentifiedObjects.getDisplayName(source, locale)));
    }
}
