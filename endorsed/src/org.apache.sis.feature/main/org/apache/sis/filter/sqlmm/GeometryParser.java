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

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.resources.Errors;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;
import org.opengis.filter.InvalidFilterValueException;


/**
 * SQLMM spatial functions taking non-geometry operands and parsing a geometry.
 * Geometries can be created from Well-Known Text (WKT), Well-Known Binary (WKB)
 * or Geographic Markup Language (GML).
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 * @param  <G>  the implementation type of geometry objects.
 */
abstract class GeometryParser<R,G> extends GeometryConstructor<R,G> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4636578226555118315L;

    /**
     * Creates a new function for the given parameters.
     */
    GeometryParser(final SQLMM operation, final Expression<R,?>[] parameters, final Geometries<G> library) {
        super(operation, parameters, library);
    }

    /**
     * Creates a new expression of the same type as this expression, but with an optimized geometry.
     * The optimization may be a geometry computed immediately if all operator parameters are literals.
     */
    @Override
    public abstract Expression<R,Object> recreate(final Expression<R,?>[] effective);

    /**
     * Returns a Backus-Naur Form (BNF) of this function.
     */
    @Override
    public final String getSyntax() {
        return getFunctionName().tip() + " ( <" + inputName() + "> [, <srid>] )";
    }

    /**
     * Returns the name of the kind of input expected by this expression.
     *
     * @return {@code "text"} or {@code "wkb"}.
     */
    abstract String inputName();

    /**
     * Creates a geometry from the content of the given object.
     *
     * @param  input  the object to be evaluated by the expression.
     * @return geometry created by the expression, or {@code null} if the operation failed..
     */
    @Override
    public final Object apply(final R input) {
        final Object value = geometry.apply(input);
        try {
            final GeometryWrapper parsed = parse(value);
            if (parsed == null) {
                return null;
            }
            final GeometryWrapper result = parsed.toGeometryType(operation.getGeometryType().get());
            if (result != parsed) {
                /*
                 * Conversions are expected for operations of the kind "Boundary polygon".
                 * For other kind of operations, we accept the value but report a warning.
                 */
                switch (operation) {
                    case ST_BdPolyFromWKB:
                    case ST_BdPolyFromText:
                    case ST_BdMPolyFromWKB:
                    case ST_BdMPolyFromText: break;
                    default: warning(new InvalidFilterValueException(Errors.format(
                                            Errors.Keys.IllegalArgumentClass_3, inputName(),
                                            getResultClass(),
                                            Classes.getClass(library.getGeometry(result)))), true);
                }
            }
            if (srid != null) {
                final CoordinateReferenceSystem crs = getTargetCRS(input);
                if (crs != null) {
                    result.setCoordinateReferenceSystem(crs);
                }
            }
            return library.getGeometry(result);
        } catch (Exception e) {
            warning(Exceptions.unwrap(e));
        }
        return null;
    }

    /**
     * Parses the given value.
     *
     * @param  value  the WKT, WKB or GML value.
     * @return the geometry parsed from the given value.
     * @throws ClassCastException if the given value is not an instance of the type expected by this operation
     * @throws Exception if parsing failed for another reason. This is an implementation-specific exception.
     */
    protected abstract GeometryWrapper parse(Object value) throws Exception;
}
