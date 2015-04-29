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
package org.apache.sis.referencing;

import javax.measure.unit.Unit;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.util.FactoryException;
import org.apache.sis.internal.referencing.OperationMethods;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.referencing.crs.DefaultProjectedCRS;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.util.resources.Errors;


/**
 * Helper methods for building Coordinate Reference Systems and related objects.
 *
 * <p>For now, this class is defined in the test directory because this API needs more experimentation.
 * However this class may move in the main source directory later if we feel confident that its API is
 * mature enough.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public class GeodeticObjectBuilder extends Builder<GeodeticObjectBuilder> {
    /**
     * The name of the conversion used by {@code ProjectedCRS} or {@code DerivedCRS},
     * or {@code null} if unspecified.
     */
    private String conversionName;

    /**
     * The projection parameters, or {@code null} if not applicable.
     */
    private ParameterValueGroup parameters;

    /**
     * The math transform factory, fetched when first needed.
     */
    private MathTransformFactory mtFactory;

    /**
     * Creates a new builder.
     */
    public GeodeticObjectBuilder() {
    }

    /**
     * Returns the math transform factory. This method fetch the factory when first needed.
     */
    private MathTransformFactory getMathTransformFactory() {
        if (mtFactory == null) {
            mtFactory = DefaultFactories.forBuildin(MathTransformFactory.class);
        }
        return mtFactory;
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
     * @param  method Name of the conversion method.
     * @return {@code this}, for method call chaining.
     * @throws NoSuchIdentifierException if the given name is unknown to the math transform factory.
     */
    public GeodeticObjectBuilder setConversionMethod(final String method) throws NoSuchIdentifierException {
        if (parameters != null) {
            throw new IllegalStateException(Errors.format(Errors.Keys.ElementAlreadyPresent_1, "OperationMethod"));
        }
        parameters = getMathTransformFactory().getDefaultParameters(method);
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
        final MathTransformFactory mtFactory = getMathTransformFactory();
        final MathTransform mt = mtFactory.createBaseToDerived(baseCRS, parameters, derivedCS);
        onCreate(false);
        try {
            /*
             * Create a conversion with the same properties than the ProjectedCRS properties,
             * except the aliases and identifiers. The name defaults to the ProjectedCRS name,
             * but can optionally be different.
             */
            properties.put(OperationMethods.PARAMETERS_KEY, parameters);
            final Object name = (conversionName != null) ? properties.put(Conversion.NAME_KEY, conversionName) : null;
            final Object alias = properties.put(Conversion.ALIAS_KEY, null);
            final Object identifier = properties.put(Conversion.IDENTIFIERS_KEY, null);
            final Conversion conversion = new DefaultConversion(properties, mtFactory.getLastMethodUsed(), mt);
            /*
             * Restore the original properties and create the final ProjectedCRS.
             */
            properties.put(Conversion.IDENTIFIERS_KEY, identifier);
            properties.put(Conversion.ALIAS_KEY, alias);
            if (name != null) {
                properties.put(Conversion.NAME_KEY, name);
            }
            return new DefaultProjectedCRS(properties, conversion, baseCRS, derivedCS);
        } finally {
            onCreate(true);
            properties.remove(OperationMethods.PARAMETERS_KEY);
        }
    }
}
