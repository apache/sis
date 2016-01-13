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

import javax.measure.unit.Unit;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Conversion;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.referencing.provider.TransverseMercator;
import org.apache.sis.measure.Latitude;
import org.apache.sis.referencing.Builder;

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
     * The factor for Coordinate Reference System objects, fetched when first needed.
     */
    private CRSFactory crsFactory;

    /**
     * The factor for Coordinate Operation objects, fetched when first needed.
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
     * Returns the factory for Coordinate Operation objects. This method fetches the factory when first needed.
     */
    private DefaultCoordinateOperationFactory getCoordinateOperationFactory() {
        if (copFactory == null) {
            copFactory = DefaultFactories.forBuildin(CoordinateOperationFactory.class,
                                              DefaultCoordinateOperationFactory.class);
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
}
