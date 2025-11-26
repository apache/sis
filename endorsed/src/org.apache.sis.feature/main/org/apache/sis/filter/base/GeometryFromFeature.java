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
package org.apache.sis.filter.base;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.feature.internal.shared.AttributeConvention;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.ValueReference;
import org.opengis.filter.InvalidFilterValueException;


/**
 * Expression whose input is a feature instance and output is a geometry wrapper.
 * This converter evaluates another expression, which is given at construction time,
 * then wraps the result in a {@link GeometryWrapper}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <G>  the geometry implementation type.
 */
final class GeometryFromFeature<G> extends GeometryConverter<Feature, G> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -550060050444279386L;

    /**
     * Name of the property from which the geometry object is read, or {@code null} if none.
     * This is used for fetching the default <abbr>CRS</abbr> when the geometry has none.
     */
    private final String propertyName;

    /**
     * Creates a new converter for the given expression producing library-specific objects.
     *
     * @param  library     the geometry library to use.
     * @param  expression  the expression providing geometric objects of the given library.
     */
    private GeometryFromFeature(Geometries<G> library, Expression<Feature,?> expression, String propertyName) {
        super(library, expression);
        this.propertyName = propertyName;
    }

    /**
     * Tries to create a new converter for the given expression.
     *
     * @param  library     the geometry library to use.
     * @param  expression  the expression providing geometric objects of the given library.
     * @return the geometry converter, or {@code null} if the given expression cannot be used.
     */
    static <G> GeometryFromFeature<G> tryCreate(final Geometries<G> library, final Expression<?,?> expression) {
        if (Feature.class.isAssignableFrom(expression.getResourceClass()) && expression instanceof ValueReference<?,?>) {
            final var xpath = new XPath(((ValueReference<?,?>) expression).getXPath());
            if (xpath.path == null) {
                /*
                 * The expression type is actually <? extends R>, so it is not really correct to cast to <R>.
                 * However, we are going to use <R> as input only, not as output. In such case, it is okay to
                 * ignore the fact that <R> may be a subtype of `Feature`.
                 */
                @SuppressWarnings("unchecked")
                final var ve = (Expression<Feature,?>) expression;
                return new GeometryFromFeature<>(library, ve, xpath.tip);
            }
        }
        return null;
    }

    /**
     * Evaluates the expression and converts the value to a geometry wrapper.
     * If the geometry library does not store the <abbr>CRS</abbr>,
     * the coordinate reference system is taken from the feature.
     *
     * @param  input  the geometry to evaluate with this expression.
     * @return the geometry wrapper, or {@code null} if the evaluated value is null.
     * @throws InvalidFilterValueException if the expression result is not an instance of a supported type.
     */
    @Override
    public GeometryWrapper apply(final Feature input) {
        final GeometryWrapper wrapper = super.apply(input);
        if (wrapper != null && wrapper.getCoordinateReferenceSystem() == null) {
            final CoordinateReferenceSystem crs = AttributeConvention.getCRSCharacteristic(input, propertyName);
            if (crs != null) {
                wrapper.setCoordinateReferenceSystem(crs);
            }
        }
        return wrapper;
    }
}
