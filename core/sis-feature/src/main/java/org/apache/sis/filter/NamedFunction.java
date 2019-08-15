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
package org.apache.sis.filter;

import java.util.List;
import java.util.Collection;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.internal.feature.FeatureExpression;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.internal.util.CollectionsExt;


/**
 * Base class of functions having a name and receiving an arbitrary amount of parameters.
 * This class differs from {@link UnaryFunction} and {@link BinaryFunction} in two ways:
 *
 * <ul>
 *   <li>It has a name used for invoking this function. By contrast, the unary and binary functions
 *       are represented by a symbol such as +, and their names are used only for debugging purposes.</li>
 *   <li>The number of parameters is not fixed to 1 (unary functions) or 2 (binary functions).</li>
 * </ul>
 *
 * Subclasses shall override at least the {@link #getName()} method, typically by returning a hard-coded name
 * that depends only on the class. If the name may vary for the same class, then the {@link #equals(Object)}
 * method should also be overridden.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
abstract class NamedFunction extends Node implements Function {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6933519274722660893L;

    /**
     * The sub-expressions that will be evaluated to provide the parameters to the function.
     * This list is unmodifiable.
     *
     * @see #getParameters()
     */
    protected final List<Expression> parameters;

    /**
     * Creates a new function with the given parameters. This constructor wraps the given array in a list directly
     * (without defensive copy). it is caller's responsibility to ensure that the given array is non-null, has been
     * cloned and does not contain null elements. Those steps are done by {@link SQLMM#create(String, Expression...)}.
     *
     * @param  parameters  the sub-expressions that will be evaluated to provide the parameters to the function.
     */
    NamedFunction(final Expression[] parameters) {
        this.parameters = UnmodifiableArrayList.wrap(parameters);
    }

    /**
     * Returns the name of the function to be called.
     * For example, this might be "{@code cos}" or "{@code atan2}".
     *
     * <div class="note"><b>Note for implementers:</b>
     * implementations typically return a hard-coded value. If the returned value may vary for the same class,
     * then implementers should override also the {@link #equals(Object)} and {@link #hashCode()} methods.</div>
     *
     * @return the name of this function.
     */
    @Override
    public abstract String getName();

    /**
     * Returns the sub-expressions that will be evaluated to provide the parameters to the function.
     *
     * @return the sub-expressions providing parameter values.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")         // Safe because list is unmodifiable.
    public final List<Expression> getParameters() {
        return parameters;
    }

    /**
     * Returns the children of this node, which are the {@linkplain #getParameters() parameters list}.
     * This is used for information purpose only, for example in order to build a string representation.
     *
     * @return the children of this node.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")         // Safe because list is unmodifiable.
    protected final Collection<?> getChildren() {
        return parameters;
    }

    /**
     * Returns the default value to use if an implementation for this function is not available.
     * The default implementation returns {@code null}.
     *
     * <div class="note"><b>Note for implementers:</b>
     * implementations typically return a hard-coded value. If the returned value may vary for the same class,
     * then implementers should override also the {@link #equals(Object)} and {@link #hashCode()} methods.</div>
     *
     * @return literal to use if an implementation for this function is not available, or {@code null} if none.
     */
    @Override
    public Literal getFallbackValue() {
        return null;
    }

    /**
     * Evaluates the function for producing a result of the given type.
     * If this method can not produce a value of the given type, then it returns {@code null}.
     * The default implementation evaluates the expression {@linkplain #evaluate(Object) in the default way},
     * then tries to convert the result to the target type.
     *
     * @param  object  to object to evaluate with this expression.
     * @param  target  the desired type for the expression result.
     * @return the result, or {@code null} if it can not be of the specified type.
     */
    @Override
    public <T> T evaluate(final Object object, final Class<T> target) {
        ArgumentChecks.ensureNonNull("target", target);
        final Object value = evaluate(object);
        try {
            return ObjectConverters.convert(value, target);
        } catch (UnconvertibleObjectException e) {
            warning(e);
            return null;                    // As per method contract.
        }
    }

    /**
     * Returns the type of results computed by the parameters at given index, or {@code null} if unknown.
     * If the expression implements {@link FeatureExpression}, its {@code expectedType(valueType)} method
     * will be invoked. Otherwise this method returns the single property of the given feature type if it
     * contains exactly one property, or returns {@code null} otherwise.
     *
     * @param  parameter  index of the expression for which to get the result type.
     * @param  valueType  the type of features on which to apply the expression at given index.
     * @return expected expression result type, or {@code null} if unknown.
     */
    final PropertyType expectedType(final int parameter, final FeatureType valueType) {
        final Expression exp = parameters.get(parameter);
        if (exp instanceof FeatureExpression) {
            return ((FeatureExpression) exp).expectedType(valueType);
        }
        return CollectionsExt.singletonOrNull(valueType.getProperties(true));
    }

    /**
     * Implementation of the visitor pattern.
     */
    @Override
    public Object accept(final ExpressionVisitor visitor, final Object extraData) {
        return visitor.visit(this, extraData);
    }
}
