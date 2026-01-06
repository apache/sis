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

import java.util.Collection;
import java.util.Optional;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.Geometry;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.geometry.WraparoundMethod;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.filter.base.UnaryFunction;
import org.apache.sis.filter.math.ArithmeticFunction;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.iso.AbstractFactory;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.internal.shared.LazyCandidate;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import java.time.Instant;
import org.opengis.filter.*;
import org.opengis.feature.Feature;
import org.opengis.filter.capability.FilterCapabilities;


/**
 * A factory of default {@link Filter} and {@link Expression} implementations.
 * This base class operates on resources of arbitrary type {@code <R>}.
 * Concrete subclass operates on resources of specific type such as {@link org.opengis.feature.Feature}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 *
 * @param  <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) to use as inputs.
 * @param  <G>  base class of geometry objects. The implementation-neutral type is GeoAPI {@link Geometry},
 *              but this factory allows the use of other implementations such as JTS
 *              {@link org.locationtech.jts.geom.Geometry} or ESRI {@link com.esri.core.geometry.Geometry}.
 * @param  <T>  base class of temporal objects.
 *
 * @since 1.1
 */
public abstract class DefaultFilterFactory<R,G,T> extends AbstractFactory implements FilterFactory<R,G,T> {
    /**
     * The geometry library used by this factory.
     */
    private final Geometries<G> library;

    /**
     * The base class of temporal objects.
     */
    private final Class<T> temporal;

    /**
     * The strategy to use for representing a region crossing the anti-meridian.
     */
    private final WraparoundMethod wraparound;

    /**
     * All functions identified by a name like {@code "cos"}, {@code "hypot"}, <i>etc</i>.
     * The actual function creations is delegated to an external factory such as SQLMM registry.
     * The factories are fetched by {@link #function(String, Expression[])} when first needed.
     *
     * @see #function(String, Expression[])
     */
    @LazyCandidate
    private final Capabilities availableFunctions;

    /**
     * Creates a new factory for geometries and temporal objects of the given types.
     * The {@code spatial} argument can be one of the following classes:
     *
     * <table class="sis">
     *   <caption>Authorized spatial class argument values</caption>
     *   <tr><th>Library</th> <th>Spatial class</th></tr>
     *   <tr><td>ESRI</td>    <td>{@code com.esri.core.geometry.Geometry}</td></tr>
     *   <tr><td>JTS</td>     <td>{@code org.locationtech.jts.geom.Geometry}</td></tr>
     *   <tr><td>Java2D</td>  <td>{@code java.awt.Shape}</td></tr>
     *   <tr><td>Default</td> <td>{@code java.lang.Object}</td></tr>
     * </table>
     *
     * The {@code temporal} argument should be one of the {@link java.time.temporal.Temporal}
     * implementation classes. The {@code Temporal.class} or {@code Object.class} arguments
     * are also accepted if the temporal class is not known at compile-time, in which case
     * it will be determined on a case-by-case basis at runtime. Note the latter is lightly
     * more expensive that specifying an implementation class in advance.
     *
     * @param  spatial     type of spatial objects,  or {@code Object.class} for default.
     * @param  temporal    type of temporal objects, or {@code Object.class} for any supported type.
     * @param  wraparound  the strategy to use for representing a region crossing the anti-meridian.
     */
    @SuppressWarnings("unchecked")
    protected DefaultFilterFactory(final Class<G> spatial, final Class<T> temporal, final WraparoundMethod wraparound) {
        ArgumentChecks.ensureNonNull("spatial",    spatial);
        ArgumentChecks.ensureNonNull("temporal",   temporal);
        ArgumentChecks.ensureNonNull("wraparound", wraparound);
        if (spatial == Object.class) {
            library = (Geometries<G>) Geometries.factory((GeometryLibrary) null);
        } else {
            library = (Geometries<G>) Geometries.factory(spatial);
            if (library == null || library.rootClass != spatial) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "spatial", spatial));
            }
        }
        this.temporal = temporal;
        this.wraparound = wraparound;
        availableFunctions = new Capabilities(library);
    }

    /**
     * Returns a factory operating on {@link Feature} instances.
     * The {@linkplain GeometryLibrary geometry library} will be the system default.
     * The temporal objects can be {@link java.util.Date} or implementations of {@link java.time.temporal.Temporal}.
     *
     * @return factory operating on {@link Feature} instances.
     */
    public static FilterFactory<Feature, Object, Object> forFeatures() {
        return Features.DEFAULT;
    }

    /**
     * Returns a factory operating on resource instances of the given class.
     * The current implementation recognizes the following classes:
     *
     * <ul>
     *   <li>{@link Feature}: delegate to {@link #forFeatures()}.</li>
     * </ul>
     *
     * More classes may be added in future versions.
     *
     * @param  <R>   compile-time value of the {@code type} argument.
     * @param  type  type of resources that the factory shall accept.
     * @return factory operating on resource instances of the given class.
     * @since 1.5
     */
    @SuppressWarnings("unchecked")
    public static <R> Optional<FilterFactory<R, Object, Object>> forResources(final Class<R> type) {
        if (type.equals(Feature.class)) {
            return Optional.of((FilterFactory<R, Object, Object>) forFeatures());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Describes the abilities of this factory. The description includes restrictions on
     * the available spatial operations, scalar operations, lists of supported functions,
     * and description of which geometry literals are understood.
     *
     * @return description of the abilities of this factory.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public FilterCapabilities getCapabilities() {
        return availableFunctions;
    }

    /**
     * A filter factory operating on {@link Feature} instances.
     *
     * @param  <G>  base class of geometry objects. The implementation-neutral type is GeoAPI {@link Geometry},
     *              but this factory allows the use of other implementations such as JTS
     *              {@link org.locationtech.jts.geom.Geometry} or ESRI {@link com.esri.core.geometry.Geometry}.
     * @param  <T>  base class of temporal objects.
     */
    public static class Features<G,T> extends DefaultFilterFactory<Feature, G, T> {
        /**
         * The instance using system default.
         *
         * @see #forFeatures()
         */
        static final Features<Object, Object> DEFAULT = new Features<>(Object.class, Object.class, WraparoundMethod.SPLIT);

        /**
         * Creates a new factory operating on {@link Feature} instances.
         * See the {@linkplain DefaultFilterFactory#DefaultFilterFactory(Class, Class, WraparoundMethod)}
         * super-class constructor} for a list of valid class arguments.
         *
         * @param  spatial     type of spatial objects,  or {@code Object.class} for default.
         * @param  temporal    type of temporal objects, or {@code Object.class} for any supported type.
         * @param  wraparound  the strategy to use for representing a region crossing the anti-meridian.
         *
         * @see DefaultFilterFactory#forFeatures()
         */
        public Features(final Class<G> spatial, final Class<T> temporal, final WraparoundMethod wraparound) {
            super(spatial, temporal, wraparound);
        }

        /**
         * Creates a new predicate to identify an identifiable resource within a filter expression.
         * The predicate uses no versioning and no time range.
         *
         * @param  identifier  identifier of the resource that shall be selected by the predicate.
         * @return the predicate.
         */
        @Override
        public ResourceId<Feature> resourceId(final String identifier) {
            return new IdentifierFilter(identifier);
        }

        /**
         * Creates a new predicate to identify an identifiable resource within a filter expression.
         * If {@code startTime} and {@code endTime} are non-null, the filter will select all versions
         * of a resource between the specified dates.
         *
         * @param  identifier  identifier of the resource that shall be selected by the predicate.
         * @param  version     version of the resource to select, or {@code null} for any version.
         * @param  startTime   start time of the resource to select, or {@code null} if none.
         * @param  endTime     end time of the resource to select, or {@code null} if none.
         * @return the predicate.
         *
         * @todo Current implementation ignores the version, start time and end time.
         *       This limitation may be resolved in a future version.
         */
        @Override
        public ResourceId<Feature> resourceId(final String identifier, final Version version,
                                              final Instant startTime, final Instant endTime)
        {
            return new IdentifierFilter(identifier);
        }

        /**
         * Creates an expression whose value is computed by retrieving the value indicated by a path in a resource.
         * If all characters in the path are {@linkplain Character#isUnicodeIdentifierPart(int) Unicode identifier parts},
         * then the XPath expression is simply a property name.
         *
         * <p>The desired type of property values can be specified. For example if the property values should be numbers,
         * then {@code type} can be <code>{@linkplain Number}.class</code>. If property values can be of any type with no
         * conversion desired, then {@code type} should be {@code Object.class}.</p>
         *
         * <h4>Supported XPath syntax</h4>
         * If the given {@code xpath} contains the "/" character, then all path components before the last one
         * are interpreted as associations to follow. For example if the XPath is {@code "client/name"}, then
         * the {@code ValueReference} applied on feature <var>F</var> will first search for an association
         * named {@code "client"} to feature <var>C</var>, then search for a property named {@code "name"}
         * in feature <var>C</var>.
         *
         * <p>The given {@code xpath} may contain scoped names.
         * For example {@code "foo:client"} is the name {@code "client"} in scope {@code "foo"}.
         * If the scope is an URL, then it needs to be enclosed inside {@code "Q{…}"}.
         * Example: {@code "Q{http://www.foo.com/bar}client"}.</p>
         *
         * @param  <V>    the type of the values to be fetched (compile-time value of {@code type}).
         * @param  xpath  the path to the property whose value will be returned by the {@code apply(R)} method.
         * @param  type   the type of the values to be fetched (run-time value of {@code <V>}).
         * @return an expression evaluating the referenced property value.
         */
        @Override
        public <V> ValueReference<Feature,V> property(final String xpath, final Class<V> type) {
            ArgumentChecks.ensureNonEmpty("xpath", xpath);
            ArgumentChecks.ensureNonNull ("type",  type);
            return PropertyValue.create(xpath, type);
        }
    }

    /**
     * Creates a constant, literal value that can be used in expressions.
     * The given value should be data objects such as strings, numbers, dates or geometries.
     *
     * @param  <V>    the type of the value of the literal.
     * @param  value  the literal value. May be {@code null}.
     * @return a literal for the given value.
     */
    @Override
    public <V> Literal<R,V> literal(final V value) {
        if (value == null) {
            return LeafExpression.NULL();
        }
        return new LeafExpression.Literal<>(value);
    }

    /**
     * Filter operator that compares that two sub-expressions are equal to each other.
     *
     * @param  expression1     the first of the two expressions to be used by this comparator.
     * @param  expression2     the second of the two expressions to be used by this comparator.
     * @param  isMatchingCase  specifies whether comparisons are case sensitive.
     * @param  matchAction     specifies how the comparisons shall be evaluated for a collection of values.
     * @return a filter evaluating {@code expression1} = {@code expression2}.
     *
     * @see ComparisonOperatorName#PROPERTY_IS_EQUAL_TO
     * @todo Revisit if we can be more specific on the second parameterized type in expressions.
     */
    @Override
    public BinaryComparisonOperator<R> equal(final Expression<R,?> expression1,
                                             final Expression<R,?> expression2,
                                             boolean isMatchingCase, MatchAction matchAction)
    {
        return new ComparisonFilter.EqualTo<>(expression1, expression2, isMatchingCase, matchAction);
    }

    /**
     * Filter operator that compares that its two sub-expressions are not equal to each other.
     *
     * @param  expression1     the first of the two expressions to be used by this comparator.
     * @param  expression2     the second of the two expressions to be used by this comparator.
     * @param  isMatchingCase  specifies whether comparisons are case sensitive.
     * @param  matchAction     specifies how the comparisons shall be evaluated for a collection of values.
     * @return a filter evaluating {@code expression1} ≠ {@code expression2}.
     *
     * @see ComparisonOperatorName#PROPERTY_IS_NOT_EQUAL_TO
     * @todo Revisit if we can be more specific on the second parameterized type in expressions.
     */
    @Override
    public BinaryComparisonOperator<R> notEqual(final Expression<R,?> expression1,
                                                final Expression<R,?> expression2,
                                                boolean isMatchingCase, MatchAction matchAction)
    {
        return new ComparisonFilter.NotEqualTo<>(expression1, expression2, isMatchingCase, matchAction);
    }

    /**
     * Filter operator that checks that its first sub-expression is less than its second sub-expression.
     *
     * @param  expression1     the first of the two expressions to be used by this comparator.
     * @param  expression2     the second of the two expressions to be used by this comparator.
     * @param  isMatchingCase  specifies whether comparisons are case sensitive.
     * @param  matchAction     specifies how the comparisons shall be evaluated for a collection of values.
     * @return a filter evaluating {@code expression1} &lt; {@code expression2}.
     *
     * @see ComparisonOperatorName#PROPERTY_IS_LESS_THAN
     * @todo Revisit if we can be more specific on the second parameterized type in expressions.
     */
    @Override
    public BinaryComparisonOperator<R> less(final Expression<R,?> expression1,
                                            final Expression<R,?> expression2,
                                            boolean isMatchingCase, MatchAction matchAction)
    {
        return new ComparisonFilter.LessThan<>(expression1, expression2, isMatchingCase, matchAction);
    }

    /**
     * Filter operator that checks that its first sub-expression is greater than its second sub-expression.
     *
     * @param  expression1     the first of the two expressions to be used by this comparator.
     * @param  expression2     the second of the two expressions to be used by this comparator.
     * @param  isMatchingCase  specifies whether comparisons are case sensitive.
     * @param  matchAction     specifies how the comparisons shall be evaluated for a collection of values.
     * @return a filter evaluating {@code expression1} &gt; {@code expression2}.
     *
     * @see ComparisonOperatorName#PROPERTY_IS_GREATER_THAN
     * @todo Revisit if we can be more specific on the second parameterized type in expressions.
     */
    @Override
    public BinaryComparisonOperator<R> greater(final Expression<R,?> expression1,
                                               final Expression<R,?> expression2,
                                               boolean isMatchingCase, MatchAction matchAction)
    {
        return new ComparisonFilter.GreaterThan<>(expression1, expression2, isMatchingCase, matchAction);
    }

    /**
     * Filter operator that checks that its first sub-expression is less than or equal to its second sub-expression.
     *
     * @param  expression1     the first of the two expressions to be used by this comparator.
     * @param  expression2     the second of the two expressions to be used by this comparator.
     * @param  isMatchingCase  specifies whether comparisons are case sensitive.
     * @param  matchAction     specifies how the comparisons shall be evaluated for a collection of values.
     * @return a filter evaluating {@code expression1} ≤ {@code expression2}.
     *
     * @see ComparisonOperatorName#PROPERTY_IS_LESS_THAN_OR_EQUAL_TO
     * @todo Revisit if we can be more specific on the second parameterized type in expressions.
     */
    @Override
    public BinaryComparisonOperator<R> lessOrEqual(final Expression<R,?> expression1,
                                                   final Expression<R,?> expression2,
                                                   boolean isMatchingCase, MatchAction matchAction)
    {
        return new ComparisonFilter.LessThanOrEqualTo<>(expression1, expression2, isMatchingCase, matchAction);
    }

    /**
     * Filter operator that checks that its first sub-expression is greater than its second sub-expression.
     *
     * @param  expression1     the first of the two expressions to be used by this comparator.
     * @param  expression2     the second of the two expressions to be used by this comparator.
     * @param  isMatchingCase  specifies whether comparisons are case sensitive.
     * @param  matchAction     specifies how the comparisons shall be evaluated for a collection of values.
     * @return a filter evaluating {@code expression1} ≥ {@code expression2}.
     *
     * @see ComparisonOperatorName#PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO
     * @todo Revisit if we can be more specific on the second parameterized type in expressions.
     */
    @Override
    public BinaryComparisonOperator<R> greaterOrEqual(final Expression<R,?> expression1,
                                                      final Expression<R,?> expression2,
                                                      boolean isMatchingCase, MatchAction matchAction)
    {
        return new ComparisonFilter.GreaterThanOrEqualTo<>(expression1, expression2, isMatchingCase, matchAction);
    }

    /**
     * Filter operation for a range check.
     * The lower and upper boundary values are inclusive.
     *
     * @param  expression     the expression to be compared by this comparator.
     * @param  lowerBoundary  the lower bound (inclusive) as an expression.
     * @param  upperBoundary  the upper bound (inclusive) as an expression.
     * @return a filter evaluating ({@code expression} ≥ {@code lowerBoundary})
     *                       &amp; ({@code expression} ≤ {@code upperBoundary}).
     */
    @Override
    public BetweenComparisonOperator<R> between(final Expression<R,?> expression,
                                                final Expression<R,?> lowerBoundary,
                                                final Expression<R,?> upperBoundary)
    {
        return new ComparisonFilter.Between<>(expression, lowerBoundary, upperBoundary);
    }

    /**
     * Character string comparison operator with pattern matching and specified wildcards.
     *
     * @param  expression      source of values to compare against the pattern.
     * @param  pattern         pattern to match against expression values.
     * @param  wildcard        pattern character for matching any sequence of characters.
     * @param  singleChar      pattern character for matching exactly one character.
     * @param  escape          pattern character for indicating that the next character should be matched literally.
     * @param  isMatchingCase  specifies how a filter expression processor should perform string comparisons.
     * @return a character string comparison operator with pattern matching.
     */
    @Override
    public LikeOperator<R> like(final Expression<R,?> expression, final String pattern,
            final char wildcard, final char singleChar, final char escape, final boolean isMatchingCase)
    {
        return new LikeFilter<>(expression, pattern, wildcard, singleChar, escape, isMatchingCase);
    }

    /**
     * An operator that tests if an expression's value is {@code null}.
     * This corresponds to checking whether the property exists in the real-world.
     *
     * @param  expression  source of values to compare against {@code null}.
     * @return a filter that checks if an expression's value is {@code null}.
     */
    @Override
    public NullOperator<R> isNull(final Expression<R,?> expression) {
        return new UnaryFunction.IsNull<>(expression);
    }

    /**
     * An operator that tests if an expression's value is nil.
     * The difference with {@link NullOperator} is that a value should exist
     * but cannot be provided for the reason given by {@code nilReason}.
     * Possible reasons are:
     *
     * <ul>
     *   <li><b>inapplicable</b> — there is no value.</li>
     *   <li><b>template</b>     — the value will be available later.</li>
     *   <li><b>missing</b>      — the correct value is not readily available to the sender of this data.
     *                             Furthermore, a correct value may not exist.</li>
     *   <li><b>unknown</b>      — the correct value is not known to, and not computable by, the sender of this data.
     *                             However, a correct value probably exists..</li>
     *   <li><b>withheld</b>     — the value is not divulged.</li>
     *   <li>Other strings at implementation choice.</li>
     * </ul>
     *
     * @param  expression  source of values to compare against nil values.
     * @param  nilReason   the reason why the value is nil, or {@code null} for accepting any reason.
     * @return a filter that checks if an expression's value is nil for the specified reason.
     *
     * @see org.apache.sis.xml.NilObject
     * @see org.apache.sis.xml.NilReason
     */
    @Override
    public NilOperator<R> isNil(final Expression<R,?> expression, final String nilReason) {
        return new UnaryFunction.IsNil<>(expression, nilReason);
    }

    /**
     * Creates a {@code AND} filter between two filters.
     *
     * @param  operand1  the first operand of the AND operation.
     * @param  operand2  the second operand of the AND operation.
     * @return a filter evaluating {@code operand1 AND operand2}.
     *
     * @see LogicalOperatorName#AND
     */
    @Override
    public LogicalOperator<R> and(final Filter<R> operand1, final Filter<R> operand2) {
        ArgumentChecks.ensureNonNull("operand1", operand1);
        ArgumentChecks.ensureNonNull("operand2", operand2);
        return new LogicalFilter.And<>(operand1, operand2);
    }

    /**
     * Creates a {@code AND} filter between two or more filters.
     *
     * @param  operands  a collection of at least 2 operands.
     * @return a filter evaluating {@code operand1 AND operand2 AND operand3}…
     * @throws IllegalArgumentException if the given collection contains less than 2 elements.
     *
     * @see LogicalOperatorName#AND
     */
    @Override
    public LogicalOperator<R> and(final Collection<? extends Filter<R>> operands) {
        return new LogicalFilter.And<>(operands);
    }

    /**
     * Creates a {@code OR} filter between two filters.
     *
     * @param  operand1  the first operand of the OR operation.
     * @param  operand2  the second operand of the OR operation.
     * @return a filter evaluating {@code operand1 OR operand2}.
     *
     * @see LogicalOperatorName#OR
     */
    @Override
    public LogicalOperator<R> or(final Filter<R> operand1, final Filter<R> operand2) {
        ArgumentChecks.ensureNonNull("operand1", operand1);
        ArgumentChecks.ensureNonNull("operand2", operand2);
        return new LogicalFilter.Or<>(operand1, operand2);
    }

    /**
     * Creates a {@code OR} filter between two or more filters.
     *
     * @param  operands  a collection of at least 2 operands.
     * @return a filter evaluating {@code operand1 OR operand2 OR operand3}…
     * @throws IllegalArgumentException if the given collection contains less than 2 elements.
     *
     * @see LogicalOperatorName#OR
     */
    @Override
    public LogicalOperator<R> or(final Collection<? extends Filter<R>> operands) {
        return new LogicalFilter.Or<>(operands);
    }

    /**
     * Creates a {@code NOT} filter for the given filter.
     *
     * @param  operand  the operand of the NOT operation.
     * @return a filter evaluating {@code NOT operand}.
     *
     * @see LogicalOperatorName#NOT
     */
    @Override
    public LogicalOperator<R> not(final Filter<R> operand) {
        return new LogicalFilter.Not<>(operand);
    }

    /**
     * Creates an operator that checks if the bounding box of the feature's geometry interacts
     * with the bounding box provided in the filter properties.
     *
     * @param  geometry  expression fetching the geometry to check for interaction with bounds.
     * @param  bounds    the bounds to check geometry against.
     * @return a filter checking for any interactions between the bounding boxes.
     *
     * @see SpatialOperatorName#BBOX
     *
     * @todo Maybe the expression parameterized type should extend {@link Geometry}.
     */
    @Override
    public BinarySpatialOperator<R> bbox(final Expression<R, ? extends G> geometry, final Envelope bounds) {
        return new BinarySpatialFilter<>(library, geometry, bounds, wraparound);
    }

    /**
     * Creates an operator that checks if the geometry of the two operands are equal.
     *
     * @param  geometry1  expression fetching the first geometry of the binary operator.
     * @param  geometry2  expression fetching the second geometry of the binary operator.
     * @return a filter for the "Equals" operation between the two geometries.
     *
     * @see SpatialOperatorName#EQUALS
     */
    @Override
    public BinarySpatialOperator<R> equals(final Expression<R, ? extends G> geometry1,
                                           final Expression<R, ? extends G> geometry2)
    {
        return new BinarySpatialFilter<>(SpatialOperatorName.EQUALS, library, geometry1, geometry2);
    }

    /**
     * Creates an operator that checks if the first operand is disjoint from the second.
     *
     * @param  geometry1  expression fetching the first geometry of the binary operator.
     * @param  geometry2  expression fetching the second geometry of the binary operator.
     * @return a filter for the "Disjoint" operation between the two geometries.
     *
     * @see SpatialOperatorName#DISJOINT
     */
    @Override
    public BinarySpatialOperator<R> disjoint(final Expression<R, ? extends G> geometry1,
                                             final Expression<R, ? extends G> geometry2)
    {
        return new BinarySpatialFilter<>(SpatialOperatorName.DISJOINT, library, geometry1, geometry2);
    }

    /**
     * Creates an operator that checks if the two geometric operands intersect.
     *
     * @param  geometry1  expression fetching the first geometry of the binary operator.
     * @param  geometry2  expression fetching the second geometry of the binary operator.
     * @return a filter for the "Intersects" operation between the two geometries.
     *
     * @see SpatialOperatorName#INTERSECTS
     */
    @Override
    public BinarySpatialOperator<R> intersects(final Expression<R, ? extends G> geometry1,
                                               final Expression<R, ? extends G> geometry2)
    {
        return new BinarySpatialFilter<>(SpatialOperatorName.INTERSECTS, library, geometry1, geometry2);
    }

    /**
     * Creates an operator that checks if the two geometric operands touch each other, but do not overlap.
     *
     * @param  geometry1  expression fetching the first geometry of the binary operator.
     * @param  geometry2  expression fetching the second geometry of the binary operator.
     * @return a filter for the "Touches" operation between the two geometries.
     *
     * @see SpatialOperatorName#TOUCHES
     */
    @Override
    public BinarySpatialOperator<R> touches(final Expression<R, ? extends G> geometry1,
                                            final Expression<R, ? extends G> geometry2)
    {
        return new BinarySpatialFilter<>(SpatialOperatorName.TOUCHES, library, geometry1, geometry2);
    }

    /**
     * Creates an operator that checks if the first geometric operand crosses the second.
     *
     * @param  geometry1  expression fetching the first geometry of the binary operator.
     * @param  geometry2  expression fetching the second geometry of the binary operator.
     * @return a filter for the "Crosses" operation between the two geometries.
     *
     * @see SpatialOperatorName#CROSSES
     */
    @Override
    public BinarySpatialOperator<R> crosses(final Expression<R, ? extends G> geometry1,
                                            final Expression<R, ? extends G> geometry2)
    {
        return new BinarySpatialFilter<>(SpatialOperatorName.CROSSES, library, geometry1, geometry2);
    }

    /**
     * Creates an operator that checks if the first geometric operand is completely
     * contained by the constant geometric operand.
     *
     * @param  geometry1  expression fetching the first geometry of the binary operator.
     * @param  geometry2  expression fetching the second geometry of the binary operator.
     * @return a filter for the "Within" operation between the two geometries.
     *
     * @see SpatialOperatorName#WITHIN
     */
    @Override
    public BinarySpatialOperator<R> within(final Expression<R, ? extends G> geometry1,
                                           final Expression<R, ? extends G> geometry2)
    {
        return new BinarySpatialFilter<>(SpatialOperatorName.WITHIN, library, geometry1, geometry2);
    }

    /**
     * Creates an operator that checks if the first geometric operand contains the second.
     *
     * @param  geometry1  expression fetching the first geometry of the binary operator.
     * @param  geometry2  expression fetching the second geometry of the binary operator.
     * @return a filter for the "Contains" operation between the two geometries.
     *
     * @see SpatialOperatorName#CONTAINS
     */
    @Override
    public BinarySpatialOperator<R> contains(final Expression<R, ? extends G> geometry1,
                                             final Expression<R, ? extends G> geometry2)
    {
        return new BinarySpatialFilter<>(SpatialOperatorName.CONTAINS, library, geometry1, geometry2);
    }

    /**
     * Creates an operator that checks if the interior of the first geometric operand
     * somewhere overlaps the interior of the second geometric operand.
     *
     * @param  geometry1  expression fetching the first geometry of the binary operator.
     * @param  geometry2  expression fetching the second geometry of the binary operator.
     * @return a filter for the "Overlaps" operation between the two geometries.
     *
     * @see SpatialOperatorName#OVERLAPS
     */
    @Override
    public BinarySpatialOperator<R> overlaps(final Expression<R, ? extends G> geometry1,
                                             final Expression<R, ? extends G> geometry2)
    {
        return new BinarySpatialFilter<>(SpatialOperatorName.OVERLAPS, library, geometry1, geometry2);
    }

    /**
     * Creates an operator that checks if all of a feature's geometry is more distant
     * than the given distance from the given geometry.
     *
     * @param  geometry1  expression fetching the first geometry of the binary operator.
     * @param  geometry2  expression fetching the second geometry of the binary operator.
     * @param  distance   minimal distance for evaluating the expression as {@code true}.
     * @return operator that evaluates to {@code true} when all of a feature's geometry
     *         is more distant than the given distance from the second geometry.
     *
     * @see DistanceOperatorName#BEYOND
     */
    @Override
    public DistanceOperator<R> beyond(final Expression<R, ? extends G> geometry1,
                                      final Expression<R, ? extends G> geometry2,
                                      final Quantity<Length> distance)
    {
        return new DistanceFilter<>(DistanceOperatorName.BEYOND, library, geometry1, geometry2, distance);
    }

    /**
     * Creates an operator that checks if any part of the first geometry lies within
     * the given distance of the second geometry.
     *
     * @param  geometry1  expression fetching the first geometry of the binary operator.
     * @param  geometry2  expression fetching the second geometry of the binary operator.
     * @param  distance   maximal distance for evaluating the expression as {@code true}.
     * @return operator that evaluates to {@code true} when any part of the feature's geometry
     *         lies within the given distance of the second geometry.
     *
     * @see DistanceOperatorName#WITHIN
     */
    @Override
    public DistanceOperator<R> within(final Expression<R, ? extends G> geometry1,
                                      final Expression<R, ? extends G> geometry2,
                                      final Quantity<Length> distance)
    {
        return new DistanceFilter<>(DistanceOperatorName.WITHIN, library, geometry1, geometry2, distance);
    }

    /**
     * Creates an operator that checks if first temporal operand is after the second.
     *
     * @param  time1  expression fetching the first temporal value.
     * @param  time2  expression fetching the second temporal value.
     * @return a filter for the "After" operator between the two temporal values.
     *
     * @see TemporalOperatorName#AFTER
     */
    @Override
    public TemporalOperator<R> after(final Expression<R, ? extends T> time1,
                                     final Expression<R, ? extends T> time2)
    {
        return TemporalFilter.create(temporal, TemporalOperation.After::new, time1, time2);
    }

    /**
     * Creates an operator that checks if first temporal operand is before the second.
     *
     * @param  time1  expression fetching the first temporal value.
     * @param  time2  expression fetching the second temporal value.
     * @return a filter for the "Before" operator between the two temporal values.
     *
     * @see TemporalOperatorName#BEFORE
     */
    @Override
    public TemporalOperator<R> before(final Expression<R, ? extends T> time1,
                                      final Expression<R, ? extends T> time2)
    {
        return TemporalFilter.create(temporal, TemporalOperation.Before::new, time1, time2);
    }

    /**
     * Creates an operator that checks if first temporal operand begins at the second.
     *
     * @param  time1  expression fetching the first temporal value.
     * @param  time2  expression fetching the second temporal value.
     * @return a filter for the "Begins" operator between the two temporal values.
     *
     * @see TemporalOperatorName#BEGINS
     */
    @Override
    public TemporalOperator<R> begins(final Expression<R, ? extends T> time1,
                                      final Expression<R, ? extends T> time2)
    {
        return TemporalFilter.create(temporal, TemporalOperation.Begins::new, time1, time2);
    }

    /**
     * Creates an operator that checks if first temporal operand begun by the second.
     *
     * @param  time1  expression fetching the first temporal value.
     * @param  time2  expression fetching the second temporal value.
     * @return a filter for the "BegunBy" operator between the two temporal values.
     *
     * @see TemporalOperatorName#BEGUN_BY
     */
    @Override
    public TemporalOperator<R> begunBy(final Expression<R, ? extends T> time1,
                                       final Expression<R, ? extends T> time2)
    {
        return TemporalFilter.create(temporal, TemporalOperation.BegunBy::new, time1, time2);
    }

    /**
     * Creates an operator that checks if first temporal operand is contained by the second.
     *
     * @param  time1  expression fetching the first temporal value.
     * @param  time2  expression fetching the second temporal value.
     * @return a filter for the "TContains" operator between the two temporal values.
     *
     * @see TemporalOperatorName#CONTAINS
     */
    @Override
    public TemporalOperator<R> tcontains(final Expression<R, ? extends T> time1,
                                         final Expression<R, ? extends T> time2)
    {
        return TemporalFilter.create(temporal, TemporalOperation.Contains::new, time1, time2);
    }

    /**
     * Creates an operator that checks if first temporal operand is during the second.
     *
     * @param  time1  expression fetching the first temporal value.
     * @param  time2  expression fetching the second temporal value.
     * @return a filter for the "During" operator between the two temporal values.
     *
     * @see TemporalOperatorName#DURING
     */
    @Override
    public TemporalOperator<R> during(final Expression<R, ? extends T> time1,
                                      final Expression<R, ? extends T> time2)
    {
        return TemporalFilter.create(temporal, TemporalOperation.During::new, time1, time2);
    }

    /**
     * Creates an operator that checks if first temporal operand is equal to the second.
     *
     * @param  time1  expression fetching the first temporal value.
     * @param  time2  expression fetching the second temporal value.
     * @return a filter for the "TEquals" operator between the two temporal values.
     *
     * @see TemporalOperatorName#EQUALS
     */
    @Override
    public TemporalOperator<R> tequals(final Expression<R, ? extends T> time1,
                                       final Expression<R, ? extends T> time2)
    {
        return TemporalFilter.create(temporal, TemporalOperation.Equals::new, time1, time2);
    }

    /**
     * Creates an operator that checks if first temporal operand overlaps the second.
     *
     * @param  time1  expression fetching the first temporal value.
     * @param  time2  expression fetching the second temporal value.
     * @return a filter for the "TOverlaps" operator between the two temporal values.
     *
     * @see TemporalOperatorName#OVERLAPS
     */
    @Override
    public TemporalOperator<R> toverlaps(final Expression<R, ? extends T> time1,
                                         final Expression<R, ? extends T> time2)
    {
        return TemporalFilter.create(temporal, TemporalOperation.Overlaps::new, time1, time2);
    }

    /**
     * Creates an operator that checks if first temporal operand meets the second.
     *
     * @param  time1  expression fetching the first temporal value.
     * @param  time2  expression fetching the second temporal value.
     * @return a filter for the "Meets" operator between the two temporal values.
     *
     * @see TemporalOperatorName#MEETS
     */
    @Override
    public TemporalOperator<R> meets(final Expression<R, ? extends T> time1,
                                     final Expression<R, ? extends T> time2)
    {
        return TemporalFilter.create(temporal, TemporalOperation.Meets::new, time1, time2);
    }

    /**
     * Creates an operator that checks if first temporal operand ends at the second.
     *
     * @param  time1  expression fetching the first temporal value.
     * @param  time2  expression fetching the second temporal value.
     * @return a filter for the "Ends" operator between the two temporal values.
     *
     * @see TemporalOperatorName#ENDS
     */
    @Override
    public TemporalOperator<R> ends(final Expression<R, ? extends T> time1,
                                    final Expression<R, ? extends T> time2)
    {
        return TemporalFilter.create(temporal, TemporalOperation.Ends::new, time1, time2);
    }

    /**
     * Creates an operator that checks if first temporal operand is overlapped by the second.
     *
     * @param  time1  expression fetching the first temporal value.
     * @param  time2  expression fetching the second temporal value.
     * @return a filter for the "OverlappedBy" operator between the two temporal values.
     *
     * @see TemporalOperatorName#OVERLAPPED_BY
     */
    @Override
    public TemporalOperator<R> overlappedBy(final Expression<R, ? extends T> time1,
                                            final Expression<R, ? extends T> time2)
    {
        return TemporalFilter.create(temporal, TemporalOperation.OverlappedBy::new, time1, time2);
    }

    /**
     * Creates an operator that checks if first temporal operand is met by the second.
     *
     * @param  time1  expression fetching the first temporal value.
     * @param  time2  expression fetching the second temporal value.
     * @return a filter for the "MetBy" operator between the two temporal values.
     *
     * @see TemporalOperatorName#MET_BY
     */
    @Override
    public TemporalOperator<R> metBy(final Expression<R, ? extends T> time1,
                                     final Expression<R, ? extends T> time2)
    {
        return TemporalFilter.create(temporal, TemporalOperation.MetBy::new, time1, time2);
    }

    /**
     * Creates an operator that checks if first temporal operand is ended by the second.
     *
     * @param  time1  expression fetching the first temporal value.
     * @param  time2  expression fetching the second temporal value.
     * @return a filter for the "EndedBy" operator between the two temporal values.
     *
     * @see TemporalOperatorName#ENDED_BY
     */
    @Override
    public TemporalOperator<R> endedBy(final Expression<R, ? extends T> time1,
                                       final Expression<R, ? extends T> time2)
    {
        return TemporalFilter.create(temporal, TemporalOperation.EndedBy::new, time1, time2);
    }

    /**
     * Creates a shortcut operator semantically equivalent to NOT (Before OR Meets OR MetBy OR After).
     * This is applicable to periods only.
     *
     * @param  time1  expression fetching the first temporal value.
     * @param  time2  expression fetching the second temporal value.
     * @return a filter for the "AnyInteracts" operator between the two temporal values.
     *
     * @see TemporalOperatorName#ANY_INTERACTS
     */
    @Override
    public TemporalOperator<R> anyInteracts(final Expression<R, ? extends T> time1,
                                            final Expression<R, ? extends T> time2)
    {
        return TemporalFilter.create(temporal, TemporalOperation.AnyInteracts::new, time1, time2);
    }

    /**
     * Creates a function computing the numeric addition of the first and second operand.
     *
     * @param  operand1  expression fetching the first number.
     * @param  operand2  expression fetching the second number.
     * @return an expression for the "Add" function between the two numerical values.
     *
     * @todo Should we really restrict the type to {@link Number}?
     */
    @Override
    public Expression<R,Number> add(final Expression<R, ? extends Number> operand1,
                                    final Expression<R, ? extends Number> operand2)
    {
        return new ArithmeticFunction.Add<>(operand1, operand2);
    }

    /**
     * Creates a function computing the numeric difference between the first and second operand.
     *
     * @param  operand1  expression fetching the first number.
     * @param  operand2  expression fetching the second number.
     * @return an expression for the "Subtract" function between the two numerical values.
     *
     * @todo Should we really restrict the type to {@link Number}?
     */
    @Override
    public Expression<R,Number> subtract(final Expression<R, ? extends Number> operand1,
                                         final Expression<R, ? extends Number> operand2)
    {
        return new ArithmeticFunction.Subtract<>(operand1, operand2);
    }

    /**
     * Creates a function computing the numeric product of their first and second operand.
     *
     * @param  operand1  expression fetching the first number.
     * @param  operand2  expression fetching the second number.
     * @return an expression for the "Multiply" function between the two numerical values.
     *
     * @todo Should we really restrict the type to {@link Number}?
     */
    @Override
    public Expression<R,Number> multiply(final Expression<R, ? extends Number> operand1,
                                         final Expression<R, ? extends Number> operand2)
    {
        return new ArithmeticFunction.Multiply<>(operand1, operand2);
    }

    /**
     * Creates a function computing the numeric quotient resulting from dividing the first operand by the second.
     *
     * @param  operand1  expression fetching the first number.
     * @param  operand2  expression fetching the second number.
     * @return an expression for the "Divide" function between the two numerical values.
     *
     * @todo Should we really restrict the type to {@link Number}?
     */
    @Override
    public Expression<R,Number> divide(final Expression<R, ? extends Number> operand1,
                                       final Expression<R, ? extends Number> operand2)
    {
        return new ArithmeticFunction.Divide<>(operand1, operand2);
    }

    /**
     * Creates an implementation-specific function.
     * The names of available functions is given by {@link #getCapabilities()}.
     *
     * @param  name        name of the function to call.
     * @param  parameters  expressions providing values for the function arguments.
     * @return an expression which will call the specified function.
     * @throws IllegalArgumentException if the given name is not recognized,
     *         or if the arguments are illegal for the specified function.
     */
    @Override
    public Expression<R,?> function(final String name, Expression<R,?>[] parameters) {
        ArgumentChecks.ensureNonNull("name", name);
        ArgumentChecks.ensureNonNull("parameters", parameters);
        parameters = parameters.clone();
        for (int i=0; i<parameters.length; i++) {
            ArgumentChecks.ensureNonNullElement("parameters", i, parameters[i]);
        }
        final Expression<R,?> expression = availableFunctions.createFunction(name, parameters);
        if (expression != null) {
            return expression;
        }
        throw new IllegalArgumentException(Resources.format(Resources.Keys.UnknownFunction_1, name));
    }

    /**
     * Indicates a property by which contents should be sorted, along with intended order.
     * The given expression should evaluate to {@link Comparable} objects,
     * but {@link Iterable} objects are accepted as well.
     *
     * @param  property  the property to sort by.
     * @param  order     the sorting order, ascending or descending.
     * @return definition of sort order of a property.
     */
    @Override
    public SortProperty<R> sort(final ValueReference<R,?> property, final SortOrder order) {
        return new DefaultSortProperty<>(property, order);
    }

    /**
     * Returns a string representation of this factory for debugging purposes.
     * The string returned by this method may change in any future version.
     *
     * @return a string representation for debugging purposes.
     *
     * @since 1.5
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(), "spatial", library.library, "temporal", temporal.getSimpleName());
    }
}
