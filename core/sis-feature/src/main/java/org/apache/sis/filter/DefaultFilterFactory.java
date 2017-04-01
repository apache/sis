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
import java.util.Set;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.MatchAction;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNil;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.capability.ArithmeticOperators;
import org.opengis.filter.capability.ComparisonOperators;
import org.opengis.filter.capability.FilterCapabilities;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.capability.Functions;
import org.opengis.filter.capability.GeometryOperand;
import org.opengis.filter.capability.IdCapabilities;
import org.opengis.filter.capability.Operator;
import org.opengis.filter.capability.ScalarCapabilities;
import org.opengis.filter.capability.SpatialCapabilities;
import org.opengis.filter.capability.SpatialOperator;
import org.opengis.filter.capability.SpatialOperators;
import org.opengis.filter.capability.TemporalCapabilities;
import org.opengis.filter.capability.TemporalOperand;
import org.opengis.filter.capability.TemporalOperators;
import org.opengis.filter.expression.Add;
import org.opengis.filter.expression.Divide;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.Multiply;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.expression.Subtract;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.identity.GmlObjectId;
import org.opengis.filter.identity.Identifier;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.AnyInteracts;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.Begins;
import org.opengis.filter.temporal.BegunBy;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.EndedBy;
import org.opengis.filter.temporal.Ends;
import org.opengis.filter.temporal.Meets;
import org.opengis.filter.temporal.MetBy;
import org.opengis.filter.temporal.OverlappedBy;
import org.opengis.filter.temporal.TContains;
import org.opengis.filter.temporal.TEquals;
import org.opengis.filter.temporal.TOverlaps;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.Geometry;
import org.opengis.util.GenericName;

/**
 * Default implementation of GeoAPI filter factory.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public class DefaultFilterFactory implements FilterFactory2 {

    // SPATIAL FILTERS /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc }
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
    public BBOX bbox(final Expression e, final Envelope bounds)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
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
            final double distance, final String units) {
        final PropertyName name = property(propertyName);
        final Literal geom = literal(geometry);
        return dwithin(name, geom, distance, units);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public DWithin dwithin(final Expression left, final Expression right,
            final double distance, final String units) {
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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public GmlObjectId gmlObjectId(final String id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // FILTERS /////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc }
     */
    @Override
    public And and(final Filter filter1, final Filter filter2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public And and(final List<Filter> filters) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Or or(final Filter filter1, final Filter filter2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Or or(final List<Filter> filters) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Not not(final Filter filter) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Id id(final Set<? extends Identifier> ids) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyName property(final GenericName name) {
        return property(name.toString());
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyName property(final String name) {
        return new DefaultPropertyName(name);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsBetween between(final Expression expr,
            final Expression lower, final Expression upper) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsEqualTo equals(final Expression expr1, final Expression expr2) {
        return equal(expr1, expr2, true, MatchAction.ANY);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsEqualTo equal(final Expression expr1,
            final Expression expr2, final boolean matchCase, MatchAction matchAction) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsNotEqualTo notEqual(final Expression expr1, final Expression expr2) {
        return notEqual(expr1, expr2,false, MatchAction.ANY);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsNotEqualTo notEqual(final Expression expr1,
            final Expression expr2, final boolean matchCase, final MatchAction matchAction) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsGreaterThan greater(final Expression expr1,
            final Expression expr2) {
        return greater(expr1,expr2,false, MatchAction.ANY);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsGreaterThan greater(final Expression expr1,
            final Expression expr2, final boolean matchCase, final MatchAction matchAction) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsGreaterThanOrEqualTo greaterOrEqual(
            final Expression expr1, final Expression expr2) {
        return greaterOrEqual(expr1, expr2,false, MatchAction.ANY);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsGreaterThanOrEqualTo greaterOrEqual(
            final Expression expr1, final Expression expr2, final boolean matchCase, final MatchAction matchAction) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsLessThan less(final Expression expr1, final Expression expr2) {
        return less(expr1, expr2, false, MatchAction.ANY);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsLessThan less(final Expression expr1,
            final Expression expr2, final boolean matchCase, MatchAction matchAction) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsLessThanOrEqualTo lessOrEqual(
            final Expression expr1, final Expression expr2) {
        return lessOrEqual(expr1, expr2, false, MatchAction.ANY);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsLessThanOrEqualTo lessOrEqual(final Expression expr1,
            final Expression expr2, final boolean matchCase, final MatchAction matchAction) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsLike like(final Expression expr, final String pattern) {
        return like(expr, pattern, "*", "?", "\\");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsLike like(final Expression expr, final String pattern,
            final String wildcard, final String singleChar, final String escape) {
        return like(expr,pattern,wildcard,singleChar,escape,false);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsLike like(final Expression expr, final String pattern,
            final String wildcard, final String singleChar,
            final String escape, final boolean matchCase) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsNull isNull(final Expression expr) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsNil isNil(Expression expr) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // TEMPORAL FILTER /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc }
     */
    @Override
    public After after(Expression expr1, Expression expr2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public AnyInteracts anyInteracts(Expression expr1, Expression expr2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Before before(Expression expr1, Expression expr2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Begins begins(Expression expr1, Expression expr2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public BegunBy begunBy(Expression expr1, Expression expr2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public During during(Expression expr1, Expression expr2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Ends ends(Expression expr1, Expression expr2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public EndedBy endedBy(Expression expr1, Expression expr2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Meets meets(Expression expr1, Expression expr2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public MetBy metBy(Expression expr1, Expression expr2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public OverlappedBy overlappedBy(Expression expr1, Expression expr2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TContains tcontains(Expression expr1, Expression expr2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TEquals tequals(Expression expr1, Expression expr2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TOverlaps toverlaps(Expression expr1, Expression expr2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // EXPRESSIONS /////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc }
     */
    @Override
    public Add add(final Expression expr1, final Expression expr2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Divide divide(final Expression expr1, final Expression expr2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Multiply multiply(final Expression expr1, final Expression expr2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Subtract subtract(final Expression expr1, final Expression expr2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Function function(final String name, final Expression... parameters) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Literal literal(final Object value) {
        return new DefaultLiteral<>(value);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Literal literal(final byte value) {
        return new DefaultLiteral<>(value);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Literal literal(final short value) {
        return new DefaultLiteral<>(value);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Literal literal(final int value) {
        return new DefaultLiteral<>(value);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Literal literal(final long value) {
        return new DefaultLiteral<>(value);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Literal literal(final float value) {
        return new DefaultLiteral<>(value);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Literal literal(final double value) {
        return new DefaultLiteral<>(value);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Literal literal(final char value) {
        return new DefaultLiteral<>(value);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Literal literal(final boolean value) {
        return new DefaultLiteral<>(value);
    }

    // SORT BY /////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc }
     */
    @Override
    public SortBy sort(final String propertyName, final SortOrder order) {
        throw new UnsupportedOperationException("Not supported yet.");
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
    public SpatialOperator spatialOperator(final String name,
            final GeometryOperand[] geometryOperands) {
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
