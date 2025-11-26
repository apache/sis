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
package org.apache.sis.feature.internal.shared;

import java.util.Set;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.filter.Optimization;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.filter.base.ConvertFunction;
import org.apache.sis.filter.base.Node;
import org.apache.sis.util.resources.Errors;

// Specific to the main branch:
import org.apache.sis.filter.Expression;
import org.apache.sis.pending.geoapi.filter.Literal;
import org.apache.sis.pending.geoapi.filter.ValueReference;


/**
 * <abbr>OGC</abbr> expressions or other functions operating on feature instances.
 * This interface adds an additional method, {@link #expectedType expectedType(…)},
 * for fetching in advance the expected type of expression results.
 *
 * <p>This is an experimental interface which may be removed in any future version.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <R>  the type of resources (e.g. {@code Feature}) used as inputs.
 * @param  <V>  the type of values computed by the expression.
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
     * Returns the type of values computed by this expression, or {@code null} if unknown.
     *
     * @return the type of values computed by this expression.
     */
    Class<? extends V> getResultClass();

    /**
     * Provides the expected type of values produced by this expression when a feature of a given type is evaluated.
     * Except for the special case of links (described below), the resulting type shall describe a "static" property,
     * <i>i.e.</i> the type should be an {@code AttributeType} or a {@code FeatureAssociationRole}
     * but not an {@code Operation}. The value of the static property will be set to the result of
     * evaluating the expression when instances of the {@code FeatureType} will be created.
     * This evaluation will be performed by {@link FeatureProjection#apply(AbstractFeature)}.
     *
     * <h4>Implementation guideline</h4>
     * Implementations should declare the property by invoking some of the following methods:
     *
     * <ul>
     *   <li>{@link FeatureProjectionBuilder#source()} for the source of the {@link PropertyType} in next point.</li>
     *   <li>{@link FeatureProjectionBuilder#addSourceProperty(AbstractIdentifiedType, boolean)}</li>
     *   <li>{@link FeatureProjectionBuilder#addComputedProperty(PropertyTypeBuilder, boolean)}</li>
     * </ul>
     *
     * Inherited methods such as {@link FeatureProjectionBuilder#addAttribute(Class)} can also be invoked,
     * but callers will be responsible for providing the value of the properties added by those methods.
     * These values will not be provided by {@link FeatureProjection#apply(AbstractFeature)}.
     *
     * <h4>Operations</h4>
     * If the property is a link to another property, such as {@code "sis:identifier"} or {@code "sis:geometry"},
     * then adding this property may require the addition of dependencies. These dependencies will be detected by
     * {@link FeatureProjectionBuilder}, which may generate an intermediate {@code FeatureType}.
     *
     * @param  addTo  where to add the type of the property evaluated by this expression.
     * @return handler of the added property, or {@code null} if the property cannot be added.
     * @throws IllegalArgumentException if this expression is invalid for the requested operation.
     * @throws IllegalArgumentException if the property was not found in {@code addTo.source()}.
     * @throws UnconvertibleObjectException if the property default value cannot be converted to the expected type.
     */
    FeatureProjectionBuilder.Item expectedType(FeatureProjectionBuilder addTo);

    /**
     * Returns an expression doing the same evaluation as this method, but returning results
     * as values of the specified type. This method can return {@code this} if this expression
     * is already guaranteed to provide results of the specified type.
     *
     * <h4>Default implementation</h4>
     * The default implementation returns {@code this} if this expression already provides values
     * of the specified type, or otherwise returns an expression doing conversions on-the-fly.
     *
     * @param  <N>     compile-time value of {@code target} type.
     * @param  target  desired type of expression results.
     * @return expression doing the same operation this this expression but with results of the specified type.
     * @throws ClassCastException if the specified type is not a supported target type.
     */
    @Override
    @SuppressWarnings("unchecked")
    default <N> Expression<R,N> toValueType(final Class<N> target) {
        UnconvertibleObjectException error = null;
        final Class<? extends V> current = getResultClass();
        if (current != null) {
            if (target.isAssignableFrom(current)) {
                return (Expression<R,N>) this;
            } else try {
                return new ConvertFunction<>(this, current, target);
            } catch (UnconvertibleObjectException e) {
                error = e;
            }
        }
        var e = new ClassCastException(Errors.format(Errors.Keys.CanNotConvertValue_2, getFunctionName(), target));
        e.initCause(error);
        throw e;
    }

    /**
     * Tries to cast or convert the given expression to a {@link FeatureExpression}.
     * If the given expression cannot be cast, then this method creates a copy
     * if the expression is one of the following types:
     *
     * <ol>
     *   <li>{@link Literal}.</li>
     *   <li>{@link ValueReference} if the given expression accepts feature instances.</li>
     * </ol>
     *
     * Otherwise, this method returns {@code null}.
     * It is caller's responsibility to verify if this method returns {@code null} and to throw an exception in such case.
     * We leave that responsibility to the callers because they may be able to provide better error messages.
     *
     * @param  <R>        the type of resources used as inputs.
     * @param  candidate  the expression to cast or copy. Can be null.
     * @return the given expression as a feature expression, or {@code null} if it cannot be cast or converted.
     */
    public static <R> FeatureExpression<? super R, ?> castOrCopy(final Expression<R,?> candidate) {
        if (candidate instanceof FeatureExpression<?,?>) {
            return (FeatureExpression<R,?>) candidate;
        }
        final Expression<? super R, ?> copy;
        if (candidate instanceof Literal<?,?>) {
            copy = Optimization.literal(((Literal<R,?>) candidate).getValue());
        } else if (candidate instanceof ValueReference<?,?>) {
            final String xpath = ((ValueReference<R,?>) candidate).getXPath();
            copy = DefaultFilterFactory.forResources(candidate.getResourceClass())
                    .map((factory) -> factory.property(xpath)).orElse(null);
        } else {
            return null;
        }
        // We do not expect a `ClassCastException` here because `copy` should be a SIS implementation.
        return (FeatureExpression<? super R, ?>) copy;
    }
}
