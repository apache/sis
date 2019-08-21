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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import org.apache.sis.internal.feature.FunctionRegister;
import org.opengis.filter.*;
import org.opengis.filter.capability.*;
import org.opengis.filter.capability.SpatialOperator;
import org.opengis.filter.expression.*;
import org.opengis.filter.identity.*;
import org.opengis.filter.sort.*;
import org.opengis.filter.spatial.*;
import org.opengis.filter.temporal.*;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.Geometry;
import org.opengis.util.GenericName;


/**
 * Default implementation of GeoAPI filter factory for creation of {@link Filter} and {@link Expression} instances.
 *
 * <div class="warning"><b>Warning:</b> most methods in this class are still unimplemented.
 * This is a very early draft subject to changes.
 * <b>TODO: the API of this class needs severe revision! DO NOT RELEASE.</b>
 * See <a href="https://github.com/opengeospatial/geoapi/issues/32">GeoAPI issue #32</a>.</div>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class DefaultFilterFactory implements FilterFactory2 {

    //TODO we should use a class like FeatureNaming to store and find registers.
    //but it is not accessible here, should we move it ?
    private static Map<String,FunctionRegister> FUNCTION_REGISTERS = new HashMap<>();
    static {
        final Iterator<FunctionRegister> iterator = ServiceLoader.load(FunctionRegister.class).iterator();
        while (iterator.hasNext()) {
            FunctionRegister register = iterator.next();
            for (String name : register.getNames()) {
                FUNCTION_REGISTERS.put(name, register);
            }
        }
    }


    /**
     * Creates a new factory.
     */
    public DefaultFilterFactory() {
    }

    // SPATIAL FILTERS /////////////////////////////////////////////////////////

    /**
     * Creates an operator that evaluates to {@code true} when the bounding box of the feature's geometry overlaps
     * the given bounding box.
     *
     * @param  propertyName  name of geometry property (for a {@link PropertyName} to access a feature's Geometry)
     * @param  minx          minimum "x" value (for a literal envelope).
     * @param  miny          minimum "y" value (for a literal envelope).
     * @param  maxx          maximum "x" value (for a literal envelope).
     * @param  maxy          maximum "y" value (for a literal envelope).
     * @param  srs           identifier of the Coordinate Reference System to use for a literal envelope.
     * @return operator that evaluates to {@code true} when the bounding box of the feature's geometry overlaps
     *         the bounding box provided in arguments to this method.
     *
     * @see #bbox(Expression, Envelope)
     */
    @Override
    public BBOX bbox(final String propertyName, final double minx,
            final double miny, final double maxx, final double maxy, final String srs)
    {
        return bbox(property(propertyName), minx, miny, maxx, maxy, srs);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public BBOX bbox(final Expression e, final double minx, final double miny,
            final double maxx, final double maxy, final String srs)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public BBOX bbox(final Expression e, final Envelope bounds) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Creates an operator that checks if all of a feature's geometry is more distant than the given distance
     * from the given geometry.
     *
     * @param  propertyName  name of geometry property (for a {@link PropertyName} to access a feature's Geometry).
     * @param  geometry      the geometry from which to evaluate the distance.
     * @param  distance      minimal distance for evaluating the expression as {@code true}.
     * @param  units         units of the given {@code distance}.
     * @return operator that evaluates to {@code true} when all of a feature's geometry is more distant than
     *         the given distance from the given geometry.
     */
    @Override
    public Beyond beyond(final String propertyName, final Geometry geometry,
            final double distance, final String units)
    {
        final PropertyName name = property(propertyName);
        final Literal geom = literal(geometry);
        return beyond(name, geom, distance, units);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Beyond beyond(final Expression left, final Expression right,
            final double distance, final String units)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Contains contains(final String propertyName, final Geometry geometry) {
        final PropertyName name = property(propertyName);
        final Literal geom = literal(geometry);
        return contains(name, geom);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Contains contains(final Expression left, final Expression right) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Crosses crosses(final String propertyName, final Geometry geometry) {
        final PropertyName name = property(propertyName);
        final Literal geom = literal(geometry);
        return crosses(name, geom);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Crosses crosses(final Expression left, final Expression right) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Disjoint disjoint(final String propertyName, final Geometry geometry) {
        final PropertyName name = property(propertyName);
        final Literal geom = literal(geometry);
        return disjoint(name, geom);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Disjoint disjoint(final Expression left, final Expression right) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public DWithin dwithin(final String propertyName, final Geometry geometry,
            final double distance, final String units)
    {
        final PropertyName name = property(propertyName);
        final Literal geom = literal(geometry);
        return dwithin(name, geom, distance, units);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public DWithin dwithin(final Expression left, final Expression right,
            final double distance, final String units)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Equals equals(final String propertyName, final Geometry geometry) {
        final PropertyName name = property(propertyName);
        final Literal geom = literal(geometry);
        return equal(name, geom);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Equals equal(final Expression left, final Expression right) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Intersects intersects(final String propertyName, final Geometry geometry) {
        final PropertyName name = property(propertyName);
        final Literal geom = literal(geometry);
        return intersects(name, geom);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Intersects intersects(final Expression left, final Expression right) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Overlaps overlaps(final String propertyName, final Geometry geometry) {
        final PropertyName name = property(propertyName);
        final Literal geom = literal(geometry);
        return overlaps(name, geom);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Overlaps overlaps(final Expression left, final Expression right) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Touches touches(final String propertyName, final Geometry geometry) {
        final PropertyName name = property(propertyName);
        final Literal geom = literal(geometry);
        return touches(name, geom);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Touches touches(final Expression left, final Expression right) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Within within(final String propertyName, final Geometry geometry) {
        final PropertyName name = property(propertyName);
        final Literal geom = literal(geometry);
        return within(name, geom);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Within within(final Expression left, final Expression right) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // IDENTIFIERS /////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc }
     */
    @Override
    public FeatureId featureId(final String id) {
        return new DefaultObjectId(id);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public GmlObjectId gmlObjectId(final String id) {
        return new DefaultObjectId(id);
    }

    // FILTERS /////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc }
     */
    @Override
    public And and(final Filter filter1, final Filter filter2) {
        return and(Arrays.asList(filter1, filter2));
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public And and(final List<Filter> filters) {
        return new LogicalFunction.And(filters);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Or or(final Filter filter1, final Filter filter2) {
        return or(Arrays.asList(filter1, filter2));
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Or or(final List<Filter> filters) {
        return new LogicalFunction.Or(filters);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Not not(final Filter filter) {
        return new UnaryFunction.Not(filter);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Id id(final Set<? extends Identifier> ids) {
        return new FilterByIdentifier(ids);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyName property(final GenericName name) {
        return property(name.toString());
    }

    /**
     * Creates a new expression retrieving values from a property of the given name.
     *
     * @param  name  name of the property (usually a feature attribute).
     */
    @Override
    public PropertyName property(final String name) {
        return new LeafExpression.Property(name);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsBetween between(final Expression expression, final Expression lower, final Expression upper) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsEqualTo equals(final Expression expression1, final Expression expression2) {
        return equal(expression1, expression2, true, MatchAction.ANY);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsEqualTo equal(final Expression expression1, final Expression expression2,
                                   final boolean isMatchingCase, final MatchAction matchAction)
    {
        return new ComparisonFunction.EqualTo(expression1, expression2, isMatchingCase, matchAction);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsNotEqualTo notEqual(final Expression expression1, final Expression expression2) {
        return notEqual(expression1, expression2, true, MatchAction.ANY);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsNotEqualTo notEqual(final Expression expression1, final Expression expression2,
                                         final boolean isMatchingCase, final MatchAction matchAction)
    {
        return new ComparisonFunction.NotEqualTo(expression1, expression2, isMatchingCase, matchAction);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsGreaterThan greater(final Expression expression1, final Expression expression2) {
        return greater(expression1, expression2, true, MatchAction.ANY);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsGreaterThan greater(final Expression expression1, final Expression expression2,
                                         final boolean isMatchingCase, final MatchAction matchAction)
    {
        return new ComparisonFunction.GreaterThan(expression1, expression2, isMatchingCase, matchAction);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsGreaterThanOrEqualTo greaterOrEqual(final Expression expression1, final Expression expression2) {
        return greaterOrEqual(expression1, expression2, true, MatchAction.ANY);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsGreaterThanOrEqualTo greaterOrEqual(final Expression expression1, final Expression expression2,
                                                         final boolean isMatchingCase, final MatchAction matchAction)
    {
        return new ComparisonFunction.GreaterThanOrEqualTo(expression1, expression2, isMatchingCase, matchAction);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsLessThan less(final Expression expression1, final Expression expression2) {
        return less(expression1, expression2, true, MatchAction.ANY);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsLessThan less(final Expression expression1, final Expression expression2,
                                   final boolean isMatchingCase, MatchAction matchAction)
    {
        return new ComparisonFunction.LessThan(expression1, expression2, isMatchingCase, matchAction);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsLessThanOrEqualTo lessOrEqual(final Expression expression1, final Expression expression2) {
        return lessOrEqual(expression1, expression2, true, MatchAction.ANY);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsLessThanOrEqualTo lessOrEqual(final Expression expression1, final Expression expression2,
                                                   final boolean isMatchingCase, final MatchAction matchAction)
    {
        return new ComparisonFunction.LessThanOrEqualTo(expression1, expression2, isMatchingCase, matchAction);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsLike like(final Expression expression, final String pattern) {
        return like(expression, pattern, "*", "?", "\\");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsLike like(final Expression expression, final String pattern,
            final String wildcard, final String singleChar, final String escape)
    {
        return like(expression, pattern, wildcard, singleChar, escape, true);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsLike like(final Expression expression, final String pattern,
            final String wildcard, final String singleChar,
            final String escape, final boolean isMatchingCase)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsNull isNull(final Expression expression) {
        return new UnaryFunction.IsNull(expression);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsNil isNil(Expression expression) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // TEMPORAL FILTER /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc }
     */
    @Override
    public After after(Expression expression1, Expression expression2) {
        return new TemporalFunction.After(expression1, expression2);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public AnyInteracts anyInteracts(Expression expression1, Expression expression2) {
        return new TemporalFunction.AnyInteracts(expression1, expression2);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Before before(Expression expression1, Expression expression2) {
        return new TemporalFunction.Before(expression1, expression2);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Begins begins(Expression expression1, Expression expression2) {
        return new TemporalFunction.Begins(expression1, expression2);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public BegunBy begunBy(Expression expression1, Expression expression2) {
        return new TemporalFunction.BegunBy(expression1, expression2);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public During during(Expression expression1, Expression expression2) {
        return new TemporalFunction.During(expression1, expression2);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Ends ends(Expression expression1, Expression expression2) {
        return new TemporalFunction.Ends(expression1, expression2);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public EndedBy endedBy(Expression expression1, Expression expression2) {
        return new TemporalFunction.EndedBy(expression1, expression2);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Meets meets(Expression expression1, Expression expression2) {
        return new TemporalFunction.Meets(expression1, expression2);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public MetBy metBy(Expression expression1, Expression expression2) {
        return new TemporalFunction.MetBy(expression1, expression2);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public OverlappedBy overlappedBy(Expression expression1, Expression expression2) {
        return new TemporalFunction.OverlappedBy(expression1, expression2);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TContains tcontains(Expression expression1, Expression expression2) {
        return new TemporalFunction.TContains(expression1, expression2);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TEquals tequals(Expression expression1, Expression expression2) {
        return new TemporalFunction.TEquals(expression1, expression2);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TOverlaps toverlaps(Expression expression1, Expression expression2) {
        return new TemporalFunction.TOverlaps(expression1, expression2);
    }

    // EXPRESSIONS /////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc }
     */
    @Override
    public Add add(final Expression expression1, final Expression expression2) {
        return new ArithmeticFunction.Add(expression1, expression2);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Divide divide(final Expression expression1, final Expression expression2) {
        return new ArithmeticFunction.Divide(expression1, expression2);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Multiply multiply(final Expression expression1, final Expression expression2) {
        return new ArithmeticFunction.Multiply(expression1, expression2);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Subtract subtract(final Expression expression1, final Expression expression2) {
        return new ArithmeticFunction.Subtract(expression1, expression2);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Function function(final String name, final Expression... parameters) {
        final FunctionRegister register = FUNCTION_REGISTERS.get(name);
        if (register == null) {
            throw new IllegalArgumentException("Unknown function "+name);
        }
        return register.create(name, parameters);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Literal literal(final Object value) {
        return new LeafExpression.Literal(value);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Literal literal(final byte value) {
        return new LeafExpression.Literal(value);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Literal literal(final short value) {
        return new LeafExpression.Literal(value);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Literal literal(final int value) {
        return new LeafExpression.Literal(value);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Literal literal(final long value) {
        return new LeafExpression.Literal(value);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Literal literal(final float value) {
        return new LeafExpression.Literal(value);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Literal literal(final double value) {
        return new LeafExpression.Literal(value);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Literal literal(final char value) {
        return new LeafExpression.Literal(value);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Literal literal(final boolean value) {
        return new LeafExpression.Literal(value);
    }

    // SORT BY /////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc }
     */
    @Override
    public SortBy sort(final String propertyName, final SortOrder order) {
        return new DefaultSortBy(property(propertyName), order);
    }

    // CAPABILITIES ////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc }
     */
    @Override
    public Operator operator(final String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public SpatialOperator spatialOperator(final String name, final GeometryOperand[] geometryOperands) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public FunctionName functionName(final String name, final int nargs) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Functions functions(final FunctionName[] functionNames) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public SpatialOperators spatialOperators(final SpatialOperator[] spatialOperators) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ComparisonOperators comparisonOperators(final Operator[] comparisonOperators) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ArithmeticOperators arithmeticOperators(final boolean simple, final Functions functions) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ScalarCapabilities scalarCapabilities(final ComparisonOperators comparison,
            final ArithmeticOperators arithmetic, final boolean logical)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public SpatialCapabilities spatialCapabilities(
            final GeometryOperand[] geometryOperands, final SpatialOperators spatial)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public IdCapabilities idCapabilities(final boolean eid, final boolean fid) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public FilterCapabilities capabilities(final String version,
            final ScalarCapabilities scalar, final SpatialCapabilities spatial,
            final TemporalCapabilities temporal, final IdCapabilities id)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TemporalCapabilities temporalCapabilities(TemporalOperand[] temporalOperands, TemporalOperators temporal) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
