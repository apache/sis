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

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryWrapper;
import org.apache.sis.util.resources.Errors;

// Branch-dependent imports
import org.opengis.filter.Expression;
import org.opengis.filter.InvalidFilterValueException;


/**
 * SQLMM spatial functions taking non-geometry operands and creating a geometry.
 * Geometries can be created from Well-Known Text (WKT), Well-Known Binary (WKB)
 * Geographic Markup Language (GML), or a list of points or coordinates.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 * @param  <G>  the implementation type of geometry objects.
 *
 * @since 1.1
 * @module
 */
class GeometryConstructor<R,G> extends FunctionWithSRID<R> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4644842662358995787L;

    /**
     * The expression giving the geometry.
     */
    final Expression<? super R, ?> geometry;

    /**
     * The library to use for creating geometry objects.
     */
    final Geometries<G> library;

    /**
     * Creates a new function for the given parameters.
     */
    GeometryConstructor(final SQLMM operation, final Expression<? super R, ?>[] parameters, final Geometries<G> library) {
        super(operation, parameters, parameters.length >= operation.maxParamCount ? PRESENT : ABSENT);
        this.geometry = parameters[0];
        this.library  = library;
    }

    /**
     * Creates a new expression of the same type than this expression, but with an optimized geometry.
     * The optimization may be a geometry computed immediately if all operator parameters are literals.
     */
    @Override
    public Expression<R,Object> recreate(final Expression<? super R, ?>[] effective) {
        return new GeometryConstructor<>(operation, effective, getGeometryLibrary());
    }

    /**
     * Returns the sub-expressions that will be evaluated to provide the parameters to the function.
     */
    @Override
    public final List<Expression<? super R, ?>> getParameters() {
        if (srid == null) {
            return Collections.singletonList(geometry);
        }
        return Arrays.asList(geometry, srid);                   // TODO: use List.of(…) with JDK9.
    }

    /**
     * Returns a handler for the library of geometric objects used by this expression.
     */
    @Override
    final Geometries<?> getGeometryLibrary() {
        return library;
    }

    /**
     * Creates a geometry from the content of the given object.
     *
     * @param  input  the object to be evaluated by the expression.
     * @return geometry created by the expression, or {@code null} if the operation failed..
     */
    @Override
    public Object apply(final R input) {
        final Object value = geometry.apply(input);
        try {
            final GeometryWrapper<G> result;
            if (value == null) {
                return null;
            } else if (value instanceof byte[]) {
                result = library.parseWKB(ByteBuffer.wrap((byte[]) value));
            } else if (value instanceof ByteBuffer) {
                result = library.parseWKB((ByteBuffer) value);
            } else if (value instanceof String) {
                result = library.parseWKT((String) value);
            } else {
                result = library.createFromComponents(operation.getGeometryType().get(), value);
            }
            final Object   geometry = result.implementation();
            final Class<?> expected = operation.getReturnType(library);
            if (!expected.isInstance(geometry)) {
                throw new InvalidFilterValueException(Errors.format(
                        Errors.Keys.IllegalArgumentClass_3, "geom", expected, geometry.getClass()));
            }
            if (srid != null) {
                final CoordinateReferenceSystem crs = getTargetCRS(input);
                if (crs != null) {
                    result.setCoordinateReferenceSystem(crs);
                }
            }
            return geometry;
        } catch (Exception e) {
            warning(e, false);
        }
        return null;
    }
}
