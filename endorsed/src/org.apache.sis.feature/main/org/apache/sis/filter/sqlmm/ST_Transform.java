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
package org.apache.sis.filter.sqlmm;

import java.util.List;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.util.collection.BackingStoreException;

// Specific to the main branch:
import org.apache.sis.filter.Expression;
import org.apache.sis.pending.geoapi.filter.Literal;


/**
 * Return an geometry value transformed to the specified spatial reference system, considering
 * z and m coordinate values in the calculations and including them in the resultant geometry.
 * This expression expects two arguments:
 *
 * <ol class="verbose">
 *   <li>An expression returning a geometry object. The evaluated value shall be an instance of
 *       one of the implementations enumerated in {@link org.apache.sis.setup.GeometryLibrary}.</li>
 *   <li>An expression returning the target CRS. This is typically a {@link Literal}, i.e. a constant for all geometries,
 *       but this implementation allows the expression to return different CRS for different geometries
 *       for example depending on the number of dimensions. This CRS can be specified in different ways:
 *     <ul>
 *       <li>As a {@link CoordinateReferenceSystem} instance.</li>
 *       <li>As a {@link String} instance of the form {@code "EPSG:xxxx"}, a URL or a URN.</li>
 *       <li>As an {@link Integer} instance specifying an EPSG code.</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Current implementation ignores the <var>z</var> and <var>m</var> values.</li>
 *   <li>If the SRID is an integer, it is interpreted as an EPSG code.
 *       It should be a primary key in the {@code "spatial_ref_sys"} table instead.</li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <R>  the type of resources (e.g. {@code Feature}) used as inputs.
 */
final class ST_Transform<R> extends FunctionWithSRID<R> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5769818355081378907L;

    /**
     * The expression giving the geometry.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private final Expression<R, GeometryWrapper> geometry;

    /**
     * Creates a new function with the given parameters. It is caller's responsibility to ensure
     * that the given array is non-null and does not contain null elements.
     *
     * @throws IllegalArgumentException if CRS cannot be constructed from the second expression.
     */
    ST_Transform(final Expression<R,?>[] parameters, final Geometries<?> library) {
        super(SQLMM.ST_Transform, parameters, PRESENT);
        geometry = toGeometryWrapper(library, parameters[0]);
    }

    /**
     * Creates a new expression of the same type as this expression, but with an optimized geometry.
     * The optimization may be a geometry computed immediately if all operator parameters are literals.
     */
    @Override
    public Expression<R,Object> recreate(final Expression<R,?>[] effective) {
        return new ST_Transform<>(effective, getGeometryLibrary());
    }

    /**
     * Returns the class of resources expected by this expression.
     */
    @Override
    public Class<? super R> getResourceClass() {
        return specializedClass(geometry.getResourceClass(), super.getResourceClass());
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
    public List<Expression<R,?>> getParameters() {
        return List.of(unwrap(geometry), srid);
    }

    /**
     * Evaluates the first expression as a geometry object, transforms that geometry to the CRS given
     * by the second expression and returns the result.
     *
     * @param  input  the object from which to get a geometry.
     * @return the transformed geometry, or {@code null} if the given object is not an instance
     *         of a supported geometry library (<abbr>JTS</abbr>, <abbr>ERSI</abbr>, Java2D…).
     */
    @Override
    public Object apply(final R input) {
        final GeometryWrapper value = geometry.apply(input);
        if (value != null) try {
            // Note: `transform(…)` does nothing if the CRS is null.
            return getGeometryLibrary().getGeometry(value.transform(getTargetCRS(input)));
        } catch (BackingStoreException e) {
            final Throwable cause = e.getCause();
            warning((cause instanceof Exception) ? (Exception) cause : e, false);
        } catch (UnsupportedOperationException | FactoryException | TransformException e) {
            warning(e, false);
        }
        return null;
    }
}
