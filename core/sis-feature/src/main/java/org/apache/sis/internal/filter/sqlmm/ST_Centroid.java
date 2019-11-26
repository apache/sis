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
package org.apache.sis.internal.filter.sqlmm;

import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.apache.sis.internal.filter.NamedFunction;
import org.apache.sis.internal.feature.FeatureExpression;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.feature.FeatureType;
import org.opengis.filter.expression.Expression;


/**
 * SQL/MM, ISO/IEC 13249-3:2011, ST_Centroid. <br>
 * <p>
 * Return the 2D ST_Point value that is the mathematical centroid of the ST_Surface value, ignoring z and m
 * coordinate values in the calculations and not including them in the resultant geometry.
 * </p>
 *
 * <p>
 * This class should be for all surface types, but JTS only has Polygon and MultiPolygon.
 * </p>
 *
 * <p>
 * An expression which computes the centroid of a geometry.
 * This expression expects one argument:
 * </p>
 *
 * <ol class="verbose">
 *   <li>An expression returning a geometry object. The evaluated value shall be an instance of
 *       one of the implementations enumerated in {@link org.apache.sis.setup.GeometryLibrary}.</li>
 * </ol>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class ST_Centroid extends NamedFunction implements FeatureExpression {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -3993074123019061170L;

    /**
     * Name of this function as defined by SQL/MM standard.
     */
    static final String NAME = "ST_Centroid";

    /**
     * Creates a new function with the given parameters. It is caller's responsibility to ensure
     * that the given array is non-null, has been cloned and does not contain null elements.
     *
     * @throws IllegalArgumentException if the number of arguments is not equal to 1.
     */
    ST_Centroid(final Expression... parameters) {
        super(parameters);
        ArgumentChecks.ensureExpectedCount("parameters", 1, parameters.length);
    }

    /**
     * Returns the name of this function, which is {@value #NAME}.
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Evaluates the first expression as a geometry object, gets the centroid of that geometry and
     * returns the result. If the geometry is not a supported implementation, returns {@code null}.
     */
    @Override
    public Object evaluate(final Object value) {
        return Geometries.getCentroid(parameters.get(0).evaluate(value));
    }

    /**
     * Provides the type of values produced by this expression when a feature of the given type is evaluated.
     *
     * @param  valueType  the type of features on which to apply this expression.
     * @param  addTo      where to add the type of properties evaluated by this expression.
     * @return builder of type resulting from expression evaluation (never null).
     * @throws IllegalArgumentException if the given feature type does not contain the expected properties.
     */
    @Override
    public PropertyTypeBuilder expectedType(final FeatureType valueType, final FeatureTypeBuilder addTo) {
        final AttributeTypeBuilder<?> pt = copyGeometryType(valueType, addTo);
        return pt.setValueClass(Geometries.implementation(pt.getValueClass()).pointClass);
    }
}
