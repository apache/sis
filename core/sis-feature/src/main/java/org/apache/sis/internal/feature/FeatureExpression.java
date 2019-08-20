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
package org.apache.sis.internal.feature;

import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;
import org.opengis.filter.expression.Expression;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;


/**
 * OGC expressions or other functions operating on feature instances.
 * This interface adds an additional method, {@link #expectedType(FeatureType, FeatureTypeBuilder)},
 * for fetching in advance the expected type of expression results.
 *
 * <p>This is an experimental interface which may be removed in any future version.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public interface FeatureExpression {
    /**
     * Provides the expected type of values produced by this expression when a feature of the given
     * type is evaluated. The resulting type shall describe a "static" property, i.e. it can be an
     * {@link AttributeType} or a {@link org.opengis.feature.FeatureAssociationRole}
     * but not an {@link org.opengis.feature.Operation}.
     *
     * @param  valueType  the type of features to be evaluated by the given expression.
     * @param  addTo      where to add the type of properties evaluated by this expression.
     * @return builder of the added property, or {@code null} if this method can not add a property.
     * @throws IllegalArgumentException if this method can operate only on some feature types
     *         and the given type is not one of them.
     */
    PropertyTypeBuilder expectedType(FeatureType valueType, FeatureTypeBuilder addTo);

    /**
     * Provides the type of results computed by the given expression.
     * This method executes the first of the following choices that apply:
     *
     * <ol>
     *   <li>If the expression implements {@link FeatureExpression}, delegate to {@link #expectedType(FeatureType,
     *       FeatureTypeBuilder)}. Note that the invoked method may throw an {@link IllegalArgumentException}.</li>
     *   <li>Otherwise if {@link Expression#evaluate(Object, Class)} with a {@code PropertyType.class} argument
     *       returns a non-null property, adds that property to the given builder.</li>
     *   <li>Otherwise if the given feature type contains exactly one property (including inherited properties),
     *       adds that property to the given builder.</li>
     *   <li>Otherwise returns {@code null}.</li>
     * </ol>
     *
     * It is caller's responsibility to verify if this method returns {@code null} and to throw an exception in such case.
     * We leave that responsibility to the caller because (s)he may be able to provide better error messages.
     *
     * @param  expression  the expression for which to get the result type, or {@code null}.
     * @param  valueType   the type of features to be evaluated by the given expression.
     * @param  addTo       where to add the type of properties evaluated by the given expression.
     * @return builder of the added property, or {@code null} if this method can not add a property.
     * @throws IllegalArgumentException if this method can operate only on some feature types
     *         and the given type is not one of them.
     */
    public static PropertyTypeBuilder expectedType(final Expression expression, final FeatureType valueType, final FeatureTypeBuilder addTo) {
        if (expression instanceof FeatureExpression) {
            return ((FeatureExpression) expression).expectedType(valueType, addTo);
        }
        PropertyType pt = null;
        if (expression != null) {
            // TODO: remove this hack if we can get more type-safe Expression.
            pt = expression.evaluate(valueType, PropertyType.class);
        }
        if (pt == null) {
            pt = CollectionsExt.singletonOrNull(valueType.getProperties(true));
            if (pt == null) {
                return null;
            }
        }
        return addTo.addProperty(pt);
    }
}
