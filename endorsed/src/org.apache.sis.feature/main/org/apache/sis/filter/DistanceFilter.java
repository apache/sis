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
import java.util.Objects;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import org.opengis.geometry.Geometry;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.geometry.wrapper.SpatialOperationContext;
import org.apache.sis.util.Exceptions;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Literal;
import org.opengis.filter.Expression;
import org.opengis.filter.DistanceOperator;
import org.opengis.filter.DistanceOperatorName;


/**
 * Spatial operations between two geometries and using a distance.
 * The nature of the operation depends on the subclass.
 *
 * <h2>API design note</h2>
 * This class has 3 parameters, but the third one is not an expression.
 * It still a "binary" operator if we count only the expressions.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 */
final class DistanceFilter<R> extends BinaryGeometryFilter<R> implements DistanceOperator<R> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5304631042699647889L;

    /**
     * Nature of the operation applied by this {@code DistanceOperator}.
     */
    private final DistanceOperatorName operatorType;

    /**
     * The buffer distance around the geometry of the second expression.
     */
    @SuppressWarnings("serial")                         // Most SIS implementations are serializable.
    private final Quantity<Length> distance;

    /**
     * Creates a new spatial function.
     *
     * @param  operatorType  nature of the operation applied by this {@code DistanceOperator}.
     * @param  library       the geometry library to use.
     * @param  geometry1     expression fetching the first geometry of the binary operator.
     * @param  geometry2     expression fetching the second geometry of the binary operator.
     * @param  distance      the buffer distance around the geometry of the second expression.
     */
    DistanceFilter(final DistanceOperatorName operatorType,
                   final Geometries<?>    library,
                   final Expression<R,?>  geometry1,
                   final Expression<R,?>  geometry2,
                   final Quantity<Length> distance)
    {
        super(library, geometry1, geometry2, distance.getUnit().getSystemUnit());
        this.operatorType = Objects.requireNonNull(operatorType);
        this.distance     = distance;
    }

    /**
     * Recreates a new filter of the same type and with the same parameters, but using the given expressions.
     * This method is invoked when it is possible to simplify or optimize at least one of the expressions that
     * were given in the original call to the constructor.
     */
    @Override
    protected BinaryGeometryFilter<R> recreate(final Expression<R,?> geometry1,
                                               final Expression<R,?> geometry2)
    {
        return new DistanceFilter<>(operatorType, getGeometryLibrary(expression1), geometry1, geometry2, distance);
    }

    /**
     * Identification of this operation.
     */
    @Override
    public DistanceOperatorName getOperatorType() {
        return operatorType;
    }

    /**
     * Returns the two expressions used as parameters by this filter.
     */
    @Override
    public List<Expression<R,?>> getExpressions() {
        return List.of(original(expression1), original(expression2),
                       new LeafExpression.Literal<>(distance));
    }

    /**
     * Returns the two expressions together with the distance parameter.
     * This is used for information purpose only, for example in order to build a string representation.
     */
    @Override
    protected Collection<?> getChildren() {
        return List.of(original(expression1), original(expression2), distance);
    }

    /**
     * Returns the buffer distance around the geometry that will be used when comparing features geometries.
     */
    @Override
    public Quantity<Length> getDistance() {
        return distance;
    }

    /**
     * Returns the literal geometry from which distances are measured.
     *
     * @throws IllegalStateException if the geometry is not a literal.
     */
    @Override
    public Geometry getGeometry() {
        final Literal<R, ? extends GeometryWrapper> literal;
        if (expression2 instanceof Literal<?,?>) {
            literal = (Literal<R, ? extends GeometryWrapper>) expression2;
        } else if (expression1 instanceof Literal<?,?>) {
            literal = (Literal<R, ? extends GeometryWrapper>) expression1;
        } else {
            throw new IllegalStateException();
        }
        return literal.getValue();
    }

    /**
     * Given an object, determines if the test(s) represented by this filter are passed.
     *
     * @param  object  the object (often a {@link Feature} instance) to evaluate.
     * @return {@code true} if the test(s) are passed for the provided object.
     */
    @Override
    public boolean test(final R object) {
        final GeometryWrapper left = expression1.apply(object);
        if (left != null) {
            final GeometryWrapper right = expression2.apply(object);
            if (right != null) try {
                return left.predicate(operatorType, right, distance, context);
            } catch (Exception e) {
                warning(Exceptions.unwrap(e), true);
            }
        }
        return emptyResult();
    }

    /**
     * Returns the value to return when a test cannot be applied.
     */
    @Override
    protected boolean emptyResult() {
        return SpatialOperationContext.emptyResult(operatorType);
    }
}
