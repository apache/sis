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
import org.opengis.util.ScopedName;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryWrapper;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.geometry.WraparoundMethod;
import org.apache.sis.filter.Optimization;

// Branch-dependent imports
import org.opengis.filter.Expression;
import org.opengis.filter.InvalidFilterValueException;


/**
 * Expression whose results is a geometry wrapper. This converter evaluates another expression,
 * which is given at construction time, potentially converts the result then wraps it.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
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
     * Evaluates the expression and converts the value to a geometry wrapper.
     * For now, only "native" geometry objects, envelope and bounding boxes are supported.
     * No Wrap-around resolution is applied.
     *
     * <p>This method is a workaround for attempting conversion of an arbitrary value to a geometry.
     * When more context is available, the chain of {@code if (x instanceof y)} statements should
     * be replaced by subclasses invoking directly the appropriate method. For example if we know
     * that values are {@link Envelope}, we should use {@link Geometries#toGeometry2D(Envelope,
     * WraparoundMethod)} directly.</p>
     *
     * @todo Try to change the class parameterized type for restricting to geometries {@code <G>}.
     *       If we can do that, remove all {@code if} statements for doing only geometry wrapping.
     *       If we can not do that, check how to propagate the wrap-around policy from some context.
     *
     * @param  input  the geometry to evaluate with this expression.
     * @return the geometry wrapper, or {@code null} if the evaluated value is null.
     * @throws InvalidFilterValueException if the expression result is not an instance of a supported type.
     */
    @Override
    public GeometryWrapper<G> apply(final R input) {
        final Object value = expression.apply(input);
        final Envelope envelope;
        if (value instanceof GeographicBoundingBox) {
            envelope = new ImmutableEnvelope((GeographicBoundingBox) value);
        } else if (value instanceof Envelope) {
            envelope = (Envelope) value;
        } else try {
            return library.castOrWrap(value);
        } catch (ClassCastException e) {
            throw new InvalidFilterValueException(Errors.format(
                    Errors.Keys.IllegalClass_2, library.rootClass, Classes.getClass(value)), e);
        }
        return library.toGeometry2D(envelope, WraparoundMethod.NONE);
    }

    /**
     * Returns {@code this} if the given type is assignable from the geometry root type,
     * or throws an exception otherwise.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <N> Expression<R,N> toValueType(final Class<N> target) {
        if (target.isAssignableFrom(library.rootClass)) {
            return (Expression<R,N>) expression;
        } else if (target.isAssignableFrom(GeometryWrapper.class)) {
            return (Expression<R,N>) this;
        } else {
            throw new ClassCastException(Errors.format(Errors.Keys.UnsupportedType_1, target));
        }
    }
}
