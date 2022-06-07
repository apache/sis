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

import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryWrapper;

// Branch-dependent imports
import org.opengis.filter.Expression;


/**
 * SQLMM spatial functions taking a single geometry operand.
 * This base class assume that the geometry is the only parameter.
 * Subclasses may add other kind of parameters.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 *
 * @param  <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 * @param  <G>  the implementation type of geometry objects.
 *
 * @since 1.1
 * @module
 */
class OneGeometry<R,G> extends SpatialFunction<R> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3895562608419096524L;

    /**
     * The expression giving the geometry.
     */
    final Expression<? super R, GeometryWrapper<G>> geometry;

    /**
     * Creates a new function for a geometry represented by the given parameter.
     */
    OneGeometry(final SQLMM operation, final Expression<? super R, ?>[] parameters, final Geometries<G> library) {
        super(operation, parameters);
        geometry = toGeometryWrapper(library, parameters[0]);
    }

    /**
     * Creates a new expression of the same type than this expression, but with an optimized geometry.
     * The optimization may be a geometry computed immediately if all operator parameters are literals.
     */
    @Override
    public Expression<R,Object> recreate(final Expression<? super R, ?>[] effective) {
        return new OneGeometry<>(operation, effective, getGeometryLibrary());
    }

    /**
     * Returns a handler for the library of geometric objects used by this expression.
     */
    @Override
    final Geometries<?> getGeometryLibrary() {
        return getGeometryLibrary(geometry);
    }

    /**
     * Returns the sub-expressions that will be evaluated to provide the parameters to the function.
     */
    @Override
    public List<Expression<? super R, ?>> getParameters() {
        return Collections.singletonList(unwrap(geometry));
    }

    /**
     * Evaluates the first expression as a geometry object, applies the operation and returns the result.
     */
    @Override
    public Object apply(R input) {
        final GeometryWrapper<G> value = geometry.apply(input);
        if (value != null) try {
            return value.operation(operation);
        } catch (RuntimeException e) {
            warning(e, false);
        }
        return null;
    }

    /**
     * SQLMM spatial functions taking a single geometry operand with one argument.
     */
    static final class WithArgument<R,G> extends OneGeometry<R,G> {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 2422322830405666146L;

        /**
         * The first argument after the geometry.
         */
        final Expression<? super R, ?> argument;

        /**
         * Creates a new function for a geometry represented by the given parameter.
         */
        WithArgument(final SQLMM operation, final Expression<? super R, ?>[] parameters, final Geometries<G> library) {
            super(operation, parameters, library);
            argument = parameters[1];
        }

        /**
         * Creates a new expression of the same type than this expression, but with an optimized geometry.
         * The optimization may be a geometry computed immediately if all operator parameters are literals.
         */
        @Override
        public Expression<R,Object> recreate(final Expression<? super R, ?>[] effective) {
            return new WithArgument<>(operation, effective, getGeometryLibrary());
        }

        /**
         * Returns the sub-expressions that will be evaluated to provide the parameters to the function.
         */
        @Override
        public List<Expression<? super R, ?>> getParameters() {
            return Arrays.asList(unwrap(geometry), argument);       // TODO: use List.of(…) with JDK9.
        }

        /**
         * Evaluates the first expression as a geometry object,
         * applies the operation and returns the result.
         */
        @Override
        public Object apply(R input) {
            final GeometryWrapper<G> value = geometry.apply(input);
            if (value != null) try {
                return value.operationWithArgument(operation, argument.apply(input));
            } catch (RuntimeException e) {
                warning(e, false);
            }
            return null;
        }
    }
}
