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
import java.nio.ByteBuffer;
import static java.lang.Double.isNaN;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;
import org.apache.sis.util.resources.Errors;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;
import org.opengis.filter.InvalidFilterValueException;


/**
 * An expression which creates a point geometry from coordinate values.
 * In current implementation, the parameters can be:
 * <ul>
 *   <li>WKT|WKB</li>
 *   <li>WKT|WKB, CoordinateReferenceSystem</li>
 *   <li>X, Y</li>
 *   <li>X, Y, CoordinateReferenceSystem</li>
 *   <li>X, Y, Z</li>
 *   <li>X, Y, Z, CoordinateReferenceSystem</li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 */
final class ST_Point<R> extends FunctionWithSRID<R> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -6280773709322350835L;

    /**
     * The expression giving the coordinate values. May include the SRID as last parameter.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private final Expression<R,?>[] parameters;

    /**
     * The library to use for creating geometry objects.
     */
    private final Geometries<?> library;

    /**
     * Creates a new function with the given parameters. It is caller's responsibility to ensure
     * that the given array is non-null, has been cloned and does not contain null elements.
     *
     * @throws IllegalArgumentException if the number of arguments is less then two.
     */
    ST_Point(final Expression<R,?>[] parameters, final Geometries<?> library) {
        super(SQLMM.ST_Point, parameters, MAYBE);
        this.parameters = parameters;
        this.library = library;
    }

    /**
     * Creates a new expression of the same type as this expression, but with an optimized geometry.
     * The optimization may be a geometry computed immediately if all operator parameters are literals.
     */
    @Override
    public Expression<R,Object> recreate(final Expression<R,?>[] effective) {
        return new ST_Point<>(effective, getGeometryLibrary());
    }

    /**
     * Returns a handler for the library of geometric objects used by this expression.
     */
    @Override
    final Geometries<?> getGeometryLibrary() {
        return library;
    }

    /**
     * Returns the class of resources expected by this expression.
     */
    @Override
    public Class<? super R> getResourceClass() {
        Class<? super R> type = super.getResourceClass();
        for (final Expression<R,?> p : parameters) {
            type = specializedClass(type, p.getResourceClass());
        }
        return type;
    }

    /**
     * Returns the sub-expressions that will be evaluated to provide the parameters to the function.
     */
    @Override
    public List<Expression<R,?>> getParameters() {
        return UnmodifiableArrayList.wrap(parameters);
    }

    /**
     * Returns the numerical value evaluated by the expression at the given index.
     * If the value is {@code null}, then {@link Double#NaN} is returned.
     * If the value is not a number, then an {@link InvalidFilterValueException} is thrown.
     *
     * @param  input  the object to be evaluated by the expression. Can be {@code null}.
     * @param  index  the parameter index.
     * @param  name   parameter name to report in exception message if the value is not a number.
     * @return the numerical value, or {@link Double#NaN} if the value was null.
     * @throws InvalidFilterValueException if the value is not a number.
     */
    private double value(final R input, final int index, final String name) {
        final Object value = parameters[index].apply(input);
        if (value == null) {
            return Double.NaN;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else {
            throw new InvalidFilterValueException(Errors.format(
                    Errors.Keys.IllegalArgumentClass_3, name, Number.class, value.getClass()));
        }
    }

    /**
     * Parses a WKT or WKB as a point. The result should be a point.
     * While this class could accept to return any kind of geometry,
     * we nevertheless throw an exception if the result is not a point.
     *
     * @param  value  the WKB or WKT value to parse. Can be {@code null}.
     * @return the parsed point, or {@code null} if the given value is null.
     * @throws InvalidFilterValueException if the value is not a string or byte array.
     * @throws Exception if parsing failed for another reason.
     */
    private GeometryWrapper parse(final Object value) throws Exception {
        final GeometryWrapper point;
        if (value == null) {
            return null;
        } else if (value instanceof byte[]) {
            point = library.parseWKB(ByteBuffer.wrap((byte[]) value));
        } else if (value instanceof ByteBuffer) {
            point = library.parseWKB((ByteBuffer) value);
        } else if (value instanceof String) {
            point = library.parseWKT((String) value);
        } else {
            throw new InvalidFilterValueException(Errors.format(
                    Errors.Keys.IllegalArgumentClass_3, "wkt|wkb", String.class, value.getClass()));
        }
        final Object geometry = library.getGeometry(point);
        if (library.pointClass.isInstance(geometry)) {
            return point;
        } else {
            final String type = (value instanceof String) ? "wkt" : "wkb";
            throw new InvalidFilterValueException(Errors.format(
                    Errors.Keys.IllegalArgumentClass_3, type, library.pointClass, point.getClass()));
        }
    }

    /**
     * Creates a point based on the content of the given object.
     * This method returns {@code null} if all coordinates are {@link Double#NaN}.
     *
     * @param  input  the object to be evaluated by the expression.
     * @return point created by the expression, or {@code null} if all coordinates are NaN.
     */
    @Override
    public Object apply(R input) {
        final GeometryWrapper point;
        final CoordinateReferenceSystem crs;
        try {
            switch (parameters.length) {
                /*
                 * Well-Known Text (WKT) or Well-Known Binary (WKB).
                 */
                case 1: {
                    point = parse(parameters[0].apply(input));
                    return library.getGeometry(point);
                }
                /*
                 * One of the following:
                 *
                 *   - (WKB or WKT) with SRID.
                 *   - (x,y) coordinates without SRID.
                 *
                 * We distinguish between those two cases by checking whether the first argument is a number.
                 * We do not check the second argument because it can be a `Number` in all cases, since SRID
                 * can be an integer.
                 */
                case 2: {
                    final Object value = parameters[0].apply(input);
                    if (value instanceof Number) {
                        final double x = ((Number) value).doubleValue();
                        final double y = value(input, 1, "y");
                        if (isNaN(x) && isNaN(y)) return null;
                        return library.createPoint(x, y);
                    } else {
                        point = parse(value);
                        if (point == null) return null;
                        crs = getTargetCRS(input);
                    }
                    break;
                }
                /*
                 * One of the following:
                 *
                 *   - (x,y) with SRID.
                 *   - (x,y,z) coordinates without SRID.
                 *
                 * We do not have a reliable way to distinguish between those two cases. Current implementation
                 * uses the `literalCRS` flag, but it works only if the SRID has been specified by a literal.
                 */
                case 3: {
                    final double x = value(input, 0, "x");
                    final double y = value(input, 1, "y");
                    if (literalCRS) {
                        if (isNaN(x) && isNaN(y)) return null;
                        point = library.castOrWrap(library.createPoint(x, y));
                        crs = getTargetCRS(input);
                    } else {
                        final double z = value(input, 2, "z");
                        if (isNaN(x) && isNaN(y) && isNaN(z)) return null;
                        return library.createPoint(x, y, z);
                    }
                    break;
                }
                /*
                 * (x,y,z) coordinates with SRID.
                 */
                case 4: {
                    final double x = value(input, 0, "x");
                    final double y = value(input, 1, "y");
                    final double z = value(input, 2, "z");
                    if (isNaN(x) && isNaN(y) && isNaN(z)) return null;
                    point = library.castOrWrap(library.createPoint(x, y, z));
                    crs = getTargetCRS(input);
                    break;
                }
                default: {
                    return null;
                }
            }
        } catch (Exception e) {
            warning(e, false);
            return null;
        }
        if (crs != null) {
            point.setCoordinateReferenceSystem(crs);
        }
        return library.getGeometry(point);
    }
}
