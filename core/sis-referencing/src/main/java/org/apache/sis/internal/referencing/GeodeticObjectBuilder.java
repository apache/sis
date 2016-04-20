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
import javax.measure.unit.Unit;
import javax.measure.quantity.Duration;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.TimeCS;
import org.opengis.referencing.datum.DatumFactory;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Conversion;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.referencing.provider.TransverseMercator;
import org.apache.sis.measure.Latitude;
import org.apache.sis.referencing.Builder;
import org.apache.sis.referencing.CommonCRS;

// Branch-dependent import
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;


/**
 * Helper methods for building Coordinate Reference Systems and related objects.
 *
 * <p>For now, this class is defined in the internal package because this API needs more experimentation.
 * However this class may move in a public package later if we feel confident that its API is mature enough.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
public class GeodeticObjectBuilder extends Builder<GeodeticObjectBuilder> {
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
     * The factory for Coordinate Reference System objects, fetched when first needed.
     */
    private CRSFactory crsFactory;

    /**
     * The factory for Coordinate System objects, fetched when first needed.
     */
    private CSFactory csFactory;

    /**
     * The factory for Datum objects, fetched when first needed.
     */
    private DatumFactory datumFactory;

    /**
     * The factory for Coordinate Operation objects, fetched when first needed.
     */
    private DefaultCoordinateOperationFactory copFactory;

    /**
     * Creates a new builder.
     */
    public GeodeticObjectBuilder() {
    }

    /**
     * Returns the factory for Coordinate Reference System objects. This method fetches the factory when first needed.
     */
    private CRSFactory getCRSFactory() {
        if (crsFactory == null) {
            crsFactory = DefaultFactories.forBuildin(CRSFactory.class);
        }
        return crsFactory;
    }

    /**
     * Returns the factory for Coordinate System objects. This method fetches the factory when first needed.
     */
    private CSFactory getCSFactory() {
        if (csFactory == null) {
            csFactory = DefaultFactories.forBuildin(CSFactory.class);
        }
        return csFactory;
    }

    /**
     * Returns the factory for Datum objects. This method fetches the factory when first needed.
     */
    private DatumFactory getDatumFactory() {
        if (datumFactory == null) {
            datumFactory = DefaultFactories.forBuildin(DatumFactory.class);
        }
        return datumFactory;
    }

    /**
     * Returns the factory for Coordinate Operation objects. This method fetches the factory when first needed.
     */
    private DefaultCoordinateOperationFactory getCoordinateOperationFactory() {
        if (copFactory == null) {
            copFactory = CoordinateOperations.factory();
        }
        return copFactory;
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
     * @param  name Name of the conversion method.
     * @return {@code this}, for method call chaining.
     * @throws FactoryException if the operation method of the given name can not be obtained.
     */
    public GeodeticObjectBuilder setConversionMethod(final String name) throws FactoryException {
        if (method != null) {
            throw new IllegalStateException(Errors.format(Errors.Keys.ElementAlreadyPresent_1, "OperationMethod"));
        }
        method = getCoordinateOperationFactory().getOperationMethod(name);
        parameters = method.getParameters().createValue();
        return this;
    }

    /**
     * Sets the name of the conversion to use for creating a {@code ProjectedCRS} or {@code DerivedCRS}.
     * This name is for information purpose; its value does not impact the numerical results of coordinate operations.
     *
     * @param  name The name to give to the conversion.
     * @return {@code this}, for method call chaining.
     */
    public GeodeticObjectBuilder setConversionName(final String name) {
        conversionName = name;
        return this;
    }

    /**
     * Ensures that {@link #setConversionMethod(String)} has been invoked.
     */
    private void ensureConversionMethodSet() {
        if (parameters == null) {
            throw new IllegalStateException();  // TODO: provide an error message.
        }
    }

    /**
     * Sets the value of a numeric parameter. The {@link #setConversionMethod(String)} method must have been invoked
     * exactly once before this method. Calls to this {@code setParameter(…)} can be repeated as many times as needed.
     *
     * @param  name  The parameter name.
     * @param  value The value to give to the parameter.
     * @param  unit  Unit of measurement for the given value.
     * @return {@code this}, for method call chaining.
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
     *   <tr><th>Parameter name</th>                 <th>Value</th></tr>
     *   <tr><td>Latitude of natural origin</td>     <td>Given latitude, snapped to 0° in the UTM case</td></tr>
     *   <tr><td>Longitude of natural origin</td>    <td>Given longitude, optionally snapped to a UTM zone</td></tr>
     *   <tr><td>Scale factor at natural origin</td> <td>0.9996</td></tr>
     *   <tr><td>False easting</td>                  <td>500000 metres</td></tr>
     *   <tr><td>False northing</td>                 <td>0 (North hemisphere) or 10000000 (South hemisphere) metres</td></tr>
     * </table></blockquote>
     *
     * @param  isUTM      If {@code true}, the given central meridian will be snapped to the central meridian of a UTM zone.
     * @param  latitude   The latitude in the center of the desired projection.
     * @param  longitude  The longitude in the center of the desired projection.
     * @return {@code this}, for method call chaining.
     * @throws FactoryException if the operation method for the Transverse Mercator projection can not be obtained.
     */
    public GeodeticObjectBuilder setTransverseMercator(boolean isUTM, double latitude, double longitude)
            throws FactoryException
    {
        ArgumentChecks.ensureBetween("latitude",   Latitude.MIN_VALUE,     Latitude.MAX_VALUE,     latitude);
        ArgumentChecks.ensureBetween("longitude", -Formulas.LONGITUDE_MAX, Formulas.LONGITUDE_MAX, longitude);
        setConversionMethod(TransverseMercator.NAME);
        setConversionName(TransverseMercator.setParameters(parameters, isUTM, latitude, longitude));
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
     *           .setParameter("Latitude of natural origin",             52, NonSI.GRADE)
     *           .setParameter("Scale factor at natural origin", 0.99987742, Unit.ONE)
     *           .setParameter("False easting",                      600000, SI.METRE)
     *           .setParameter("False northing",                    2200000, SI.METRE)
     *           .addName("NTF (Paris) / Lambert zone II")
     *           .createProjectedCRS(baseCRS, derivedCS);
     * }
     * </div>
     *
     * @param  baseCRS Coordinate reference system to base the derived CRS on.
     * @param  derivedCS The coordinate system for the derived CRS.
     * @return The projected CRS.
     * @throws FactoryException if an error occurred while building the projected CRS.
     */
    public ProjectedCRS createProjectedCRS(final GeographicCRS baseCRS, final CartesianCS derivedCS) throws FactoryException {
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
            final Conversion conversion = getCoordinateOperationFactory().createDefiningConversion(properties, method, parameters);
            /*
             * Restore the original properties and create the final ProjectedCRS.
             */
            properties.put(Conversion.IDENTIFIERS_KEY, identifier);
            properties.put(Conversion.ALIAS_KEY, alias);
            if (name != null) {
                properties.put(Conversion.NAME_KEY, name);
            }
            return getCRSFactory().createProjectedCRS(properties, baseCRS, conversion, derivedCS);
        } finally {
            onCreate(true);
        }
    }

    /**
     * Creates a temporal CRS from the given origin and temporal unit. For this method, the CRS name is optional:
     * if no {@code addName(…)} method has been invoked, then a default name will be used.
     *
     * @param  origin The epoch in milliseconds since January 1st, 1970 at midnight UTC.
     * @param  unit The unit of measurement.
     * @return A temporal CRS using the given origin and units.
     * @throws FactoryException if an error occurred while building the temporal CRS.
     */
    public TemporalCRS createTemporalCRS(final Date origin, final Unit<Duration> unit) throws FactoryException {
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
                final CSFactory csFactory = getCSFactory();
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
                datum = getDatumFactory().createTemporalDatum(properties, origin);
                properties.put(TemporalCRS.IDENTIFIERS_KEY, identifier);
                properties.put(TemporalCRS.REMARKS_KEY,     remarks);
                properties.put(TemporalCRS.NAME_KEY, datum.getName());      // Share the Identifier instance.
            }
            return getCRSFactory().createTemporalCRS(properties, datum, cs);
        } finally {
            onCreate(true);
        }
    }

    /**
     * Creates a compound CRS, but we special processing for (two-dimensional Geographic + ellipsoidal heights) tupples.
     * If any such tupple is found, a three-dimensional geographic CRS is created instead than the compound CRS.
     *
     * @param  components ordered array of {@code CoordinateReferenceSystem} objects.
     * @return The coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    public CoordinateReferenceSystem createCompoundCRS(final CoordinateReferenceSystem... components) throws FactoryException {
        return ReferencingServices.getInstance().createCompoundCRS(getCRSFactory(), getCSFactory(), properties, components);
    }

    /**
     * Creates a map of properties containing only the name of the given object.
     */
    private static Map<String,Object> name(final IdentifiedObject template) {
        return Collections.<String,Object>singletonMap(IdentifiedObject.NAME_KEY, template.getName());
    }
}
