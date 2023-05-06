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

import java.util.Set;
import org.opengis.feature.FeatureType;
import org.opengis.feature.AttributeType;
import org.opengis.filter.Literal;
import org.opengis.filter.Expression;
import org.opengis.filter.ValueReference;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.filter.Optimization;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.internal.filter.Node;


/**
 * OGC expressions or other functions operating on feature instances.
 * This interface adds an additional method, {@link #expectedType(FeatureType, FeatureTypeBuilder)},
 * for fetching in advance the expected type of expression results.
 *
 * <p>This is an experimental interface which may be removed in any future version.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @param  <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 * @param  <V>  the type of values computed by the expression.
 *
 * @since 1.0
 */
public interface FeatureExpression<R,V> extends Expression<R,V> {
    /**
     * Returns the manner in which values are computed from given resources.
     * The default implementation combines the properties of all parameters.
     *
     * @return the manners in which values are computed from resources.
     */
    default Set<FunctionProperty> properties() {
        return Node.transitiveProperties(getParameters());
    }

    /**
     * Returns the type of values computed by this expression, or {@code Object.class} if unknown.
     *
     * <h4>Note on type safety</h4>
     * The parameterized type should be {@code <? extends V>} because some implementations get this
     * information by a call to {@code value.getClass()}. But it should also be {@code <? super V>}
     * for supporting the {@code Object.class} return value. Those contradictory requirements force
     * us to use {@code <?>}.
     *
     * @return the type of values computed by this expression.
     */
    default Class<?> getValueClass() {
        return Object.class;
    }

    /**
     * Provides the expected type of values produced by this expression when a feature of the given
     * type is evaluated. The resulting type shall describe a "static" property, i.e. it can be an
     * {@link AttributeType} or a {@link org.opengis.feature.FeatureAssociationRole}
     * but not an {@link org.opengis.feature.Operation}.
     *
     * <p>If this method returns an instance of {@link AttributeTypeBuilder}, then its parameterized
     * type should be the same {@code <V>} than this {@code FeatureExpression}.</p>
     *
     * @param  valueType  the type of features to be evaluated by the given expression.
     * @param  addTo      where to add the type of properties evaluated by this expression.
     * @return builder of the added property, or {@code null} if this method cannot add a property.
     * @throws IllegalArgumentException if this method can operate only on some feature types
     *         and the given type is not one of them.
     */
    PropertyTypeBuilder expectedType(FeatureType valueType, FeatureTypeBuilder addTo);

    /**
     * Tries to cast or convert the given expression to a {@link FeatureExpression}.
     * If the given expression cannot be casted, then this method creates a copy
     * provided that the expression is one of the following type:
     *
     * <ol>
     *   <li>{@link Literal}.</li>
     *   <li>{@link ValueReference}, assuming that the expression expects feature instances.</li>
     * </ol>
     *
     * Otherwise this method returns {@code null}.
     * It is caller's responsibility to verify if this method returns {@code null} and to throw an exception in such case.
     * We leave that responsibility to the caller because (s)he may be able to provide better error messages.
     *
     * <h4>Note on type safety</h4>
     * This method does not use parameterized types because of the assumption on {@link ValueReference}.
     * As of Apache SIS 1.3, we have no way to check if {@code <R>} is for feature instances.
     *
     * @param  candidate  the expression to cast or copy. Can be null.
     * @return the given expression as a feature expression, or {@code null} if it cannot be casted or converted.
     */
    public static FeatureExpression<?,?> castOrCopy(final Expression<?,?> candidate) {
        if (candidate instanceof FeatureExpression<?,?>) {
            return (FeatureExpression<?,?>) candidate;
        }
        final Expression<?,?> copy;
        if (candidate instanceof Literal<?,?>) {
            copy = Optimization.literal(((Literal<?,?>) candidate).getValue());
        } else if (candidate instanceof ValueReference<?,?>) {
            final String xpath = ((ValueReference<?,?>) candidate).getXPath();
            copy = DefaultFilterFactory.forFeatures().property(xpath);
        } else {
            return null;
        }
        return (FeatureExpression<?,?>) copy;
    }
}
