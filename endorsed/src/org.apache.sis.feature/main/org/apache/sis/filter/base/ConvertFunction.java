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

import java.util.Set;
import java.util.List;
import java.util.Collection;
import org.opengis.util.ScopedName;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.feature.internal.shared.FeatureExpression;
import org.apache.sis.feature.internal.shared.FeatureProjectionBuilder;
import org.apache.sis.filter.Optimization;
import org.apache.sis.math.FunctionProperty;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;


/**
 * Expression whose results are converted to a different type.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 * @param  <S>  the type of value computed by the wrapped exception. This is the type to convert.
 * @param  <V>  the type of value computed by this expression. This is the type after conversion.
 *
 * @see GeometryConverter
 */
public final class ConvertFunction<R,S,V> extends UnaryFunction<R,S>
        implements FeatureExpression<R,V>, Optimization.OnExpression<R,V>
{
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4686604324414717316L;

    /**
     * Name of this expression.
     */
    private static final ScopedName NAME = createName("Convert");

    /**
     * The converter to use.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private final ObjectConverter<? super S, ? extends V> converter;

    /**
     * Creates a new converted expression.
     *
     * @param  expression  the expression providing source values.
     * @param  source      the type of value produced by given expression
     * @param  target      the desired type for the expression result.
     * @throws UnconvertibleObjectException if no converter is found.
     */
    public ConvertFunction(final Expression<R, ? extends S> expression,
                           final Class<? extends S> source,
                           final Class<V> target)
    {
        super(expression);
        converter = ObjectConverters.find(source, target);
    }

    /**
     * Creates a new converted expression after optimization.
     *
     * @param  expression  the expression providing source values.
     * @throws UnconvertibleObjectException if no converter is found.
     */
    private ConvertFunction(final ConvertFunction<R,S,V> original, final Expression<R, ? extends S> expression) {
        super(expression);
        converter = original.converter;
    }

    /**
     * Creates a new expression of the same type as this expression, but with optimized parameters.
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Expression<R,V> recreate(Expression<R,?>[] effective) {
        final Expression<R,?> e = effective[0];
        if (e instanceof FeatureExpression<?,?>) {
            final Class<? extends V> target = getResultClass();                         // This is <V>.
            final Class<?> source = ((FeatureExpression<?,?>) e).getResultClass();      // May become <S>.
            if (target.isAssignableFrom(source)) {
                return (Expression<R,V>) e;
            }
            if (source != Object.class) {
                return new ConvertFunction(e, source, target);
            }
        }
        final Class<? super S> source = converter.getSourceClass();
        return new ConvertFunction(this, e.toValueType(source));
    }

    /**
     * Returns an identification of this operation.
     */
    @Override
    public ScopedName getFunctionName() {
        return NAME;
    }

    /**
     * Returns the manner in which values are computed from given resources.
     * This expression can be represented as the concatenation of the user supplied expression with the converter.
     * Because this {@code ConvertFunction} does nothing on its own, it does not have its own set of properties.
     */
    @Override
    public Set<FunctionProperty> properties() {
        return FunctionProperty.concatenate(properties(expression), converter.properties());
    }

    /**
     * Returns the singleton expression tested by this operator
     * together with the source and target classes.
     */
    @Override
    protected Collection<?> getChildren() {
        return List.of(expression, converter.getSourceClass(), getResultClass());
    }

    /**
     * Evaluates the expression for producing a result of the given type.
     * If this method cannot produce a value of the given type, then it returns {@code null}.
     * This implementation evaluates the expression {@linkplain Expression#apply(Object) in the default way},
     * then tries to convert the result to the target type.
     *
     * @param  feature  the value or feature to evaluate with this expression.
     * @return the result, or {@code null} if it cannot be of the specified type.
     */
    @Override
    public V apply(final R feature) {
        final S value = expression.apply(feature);
        try {
            return converter.apply(value);
        } catch (UnconvertibleObjectException e) {
            warning(e, false);
            return null;
        }
    }

    /**
     * Returns the type of values computed by this expression.
     */
    @Override
    public final Class<? extends V> getResultClass() {
        return converter.getTargetClass();
    }

    /**
     * Provides the type of values produced by this expression.
     * May return {@code null} if the type cannot be determined.
     *
     * @param  addTo  where to add the type of the property evaluated by this expression.
     * @return handler of the added property, or {@code null} if the property cannot be added.
     * @throws UnconvertibleObjectException if the property default value cannot be converted to the expected type.
     */
    @Override
    public FeatureProjectionBuilder.Item expectedType(final FeatureProjectionBuilder addTo) {
        final FeatureExpression<?,?> fex = FeatureExpression.castOrCopy(expression);
        if (fex == null) {
            return null;
        }
        final FeatureProjectionBuilder.Item item = addTo.addTemplateProperty(fex);
        if (item != null) item.replaceValueClass((c) -> getResultClass());
        return item;
    }

    /**
     * Returns an expression doing the same evaluation as this method, but returning results as values
     * of the specified type. The result may be {@code this}.
     *
     * @param  <N>     compile-time value of {@code type}.
     * @param  target  desired type of expression results.
     * @return expression doing the same operation this this expression but with results of the specified type.
     * @throws ClassCastException if the specified type is not a target type supported by implementation.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <N> Expression<R,N> toValueType(final Class<N> target) {
        if (target.isAssignableFrom(getResultClass())) {
            return (Expression<R,N>) this;
        }
        final Class<? super S> source = converter.getSourceClass();
        if (target.isAssignableFrom(source)) {
            return (Expression<R,N>) expression;
        } else try {
            return new ConvertFunction<>(expression, source, target);
        } catch (UnconvertibleObjectException e) {
            throw (ClassCastException) new ClassCastException(Errors.format(
                    Errors.Keys.CanNotConvertValue_2, expression.getFunctionName(), target)).initCause(e);
        }
    }
}
