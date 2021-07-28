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
package org.apache.sis.internal.filter;

import java.util.List;
import java.util.Collection;
import java.util.Collections;
import org.apache.sis.geometry.WraparoundMethod;
import org.opengis.util.ScopedName;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryWrapper;
import org.apache.sis.filter.Optimization;

// Branch-dependent imports
import org.opengis.filter.Expression;


/**
 * Expression whose results is a geometry wrapper.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 * @param  <G>  the geometry implementation type.
 *
 * @see org.apache.sis.filter.ConvertFunction
 *
 * @since 1.1
 * @module
 */
final class GeometryConverter<R,G> extends Node implements Optimization.OnExpression<R, GeometryWrapper<G>> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1164523020740782044L;

    /**
     * Name of this expression.
     */
    private static final ScopedName NAME = createName("GeometryConverter");

    /**
     * The geometry library to use.
     */
    final Geometries<G> library;

    /**
     * The expression to be used by this operator.
     *
     * @see #getParameters()
     */
    final Expression<? super R, ?> expression;

    /**
     * Creates a new converter expression.
     *
     * @param  library     the geometry library to use.
     * @param  expression  the expression providing source values.
     */
    public GeometryConverter(final Geometries<G> library, final Expression<? super R, ?> expression) {
        ArgumentChecks.ensureNonNull("expression", expression);
        ArgumentChecks.ensureNonNull("library",    library);
        this.expression = expression;
        this.library    = library;
    }

    /**
     * Creates a new expression of the same type than this expression, but with an optimized geometry.
     * The optimization may be a geometry computed immediately if all operator parameters are literals.
     */
    @Override
    public Expression<R, GeometryWrapper<G>> recreate(final Expression<? super R, ?>[] effective) {
        return new GeometryConverter<>(library, effective[0]);
    }

    /**
     * Returns an identification of this operation.
     */
    @Override
    public ScopedName getFunctionName() {
        return NAME;
    }

    /**
     * Returns the expression used as parameters for this function.
     * This is the value specified at construction time.
     */
    @Override
    public List<Expression<? super R, ?>> getParameters() {
        return Collections.singletonList(expression);
    }

    /**
     * Returns the singleton expression tested by this operator.
     */
    @Override
    protected Collection<?> getChildren() {
        return getParameters();
    }

    /**
     * Evaluates the expression for producing a result of the given type.
     *
     * @param  input  the geometry to evaluate with this expression.
     * @return the geometry boundary.
     */
    @Override
    public GeometryWrapper<G> apply(final R input) {
        // TODO: review if this component should only cast geometries. If conversion is kept, check how to propagate
        // wrap-around method from build context.
        return library.toGeometry(expression.apply(input), WraparoundMethod.NONE);
    }

    /**
     * Returns {@code this} if the given type is assignable from the geometry root type,
     * or throws an exception otherwise.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <N> Expression<R,N> toValueType(final Class<N> type) {
        if (type.isAssignableFrom(library.rootClass)) {
            return (Expression<R,N>) expression;
        } else if (type.isAssignableFrom(GeometryWrapper.class)) {
            return (Expression<R,N>) this;
        } else {
            throw new ClassCastException(Errors.format(Errors.Keys.UnsupportedType_1, type));
        }
    }
}
