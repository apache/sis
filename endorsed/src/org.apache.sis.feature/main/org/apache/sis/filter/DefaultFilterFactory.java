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
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.iso.AbstractFactory;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.internal.shared.LazyCandidate;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.pending.geoapi.filter.MatchAction;
import org.apache.sis.pending.geoapi.filter.SpatialOperatorName;
import org.apache.sis.pending.geoapi.filter.DistanceOperatorName;


/**
 * A factory of default {@link Filter} and {@link Expression} implementations.
 * This base class operates on resources of arbitrary type {@code <R>}.
 * Concrete subclass operates on resources of specific type such as {@link AbstractFeature}.
 *
 * <div class="warning"><b>Upcoming API change</b><br>
 * In a future version, all {@link Filter} and {@link Expression} parameters may be replaced by parameters
 * of the same names but from the {@code org.opengis.filter} package instead of {@code org.apache.sis.filter}.
 * This change is pending next GeoAPI release.
 * In addition, return types may become more specialized types.
 * </div>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 *
 * @param  <R>  the type of resources (e.g. {@link AbstractFeature}) to use as inputs.
 * @param  <G>  base class of geometry objects. The implementation-neutral type is GeoAPI {@link Geometry},
 *              but this factory allows the use of other implementations such as JTS
 *              {@link org.locationtech.jts.geom.Geometry} or ESRI {@link com.esri.core.geometry.Geometry}.
 * @param  <T>  base class of temporal objects.
 *
 * @since 1.1
 */
public abstract class DefaultFilterFactory<R,G,T> extends AbstractFactory {
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
     * The factories are fetched by {@link #function(String, Expression...)} when first needed.
     *
     * @see #function(String, Expression...)
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
     * Returns a factory operating on {@link AbstractFeature} instances.
     * The {@linkplain GeometryLibrary geometry library} will be the system default.
     * The temporal objects can be {@link java.util.Date} or implementations of {@link java.time.temporal.Temporal}.
     *
     * @return factory operating on {@link AbstractFeature} instances.
     */
    public static DefaultFilterFactory<AbstractFeature, Object, Object> forFeatures() {
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
    public static <R> Optional<DefaultFilterFactory<R, Object, Object>> forResources(final Class<R> type) {
        if (type.equals(AbstractFeature.class)) {
            return Optional.of((DefaultFilterFactory<R, Object, Object>) forFeatures());
        } else {
            return Optional.empty();
        }
    }

    /**
     * A filter factory operating on {@link Feature} instances.
     *
     * @param  <G>  base class of geometry objects. The implementation-neutral type is GeoAPI {@link Geometry},
     *              but this factory allows the use of other implementations such as JTS
     *              {@link org.locationtech.jts.geom.Geometry} or ESRI {@link com.esri.core.geometry.Geometry}.
     * @param  <T>  base class of temporal objects.
     */
    public static class Features<G,T> extends DefaultFilterFactory<AbstractFeature, G, T> {
        /**
         * The instance using system default.
         *
         * @see #forFeatures()
         */
        static final Features<Object,Object> DEFAULT = new Features<>(Object.class, Object.class, WraparoundMethod.SPLIT);

        /**
         * Creates a new factory operating on {@link AbstractFeature} instances.
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
         *
         * @param  identifier  identifier of the resource that shall be selected by the predicate.
         * @return the predicate.
         */
        @Override
        public Filter<AbstractFeature> resourceId(final String identifier) {
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
        public <V> Expression<AbstractFeature,V> property(final String xpath, final Class<V> type) {
            ArgumentChecks.ensureNonEmpty("xpath", xpath);
            ArgumentChecks.ensureNonNull ("type",  type);
            return PropertyValue.create(xpath, type);
        }
    }

    /**
     * Creates a predicate to identify an identifiable resource within a filter expression.
     *
     * @param  rid  identifier of the resource that shall be selected by the predicate.
     * @return the predicate.
     */
    public abstract Filter<R> resourceId(String rid);

    /**
     * Creates an expression whose value is computed by retrieving the value indicated by a path in a resource.
     * If all characters in the path are {@linkplain Character#isUnicodeIdentifierPart(int) Unicode identifier parts},
     * then the XPath expression is simply a property name.
     *
     * @param  xpath  the path to the property whose value will be returned by the {@code apply(R)} method.
     * @return an expression evaluating the referenced property value.
     */
    public Expression<R,?> property(String xpath) {
        return property(xpath, Object.class);
    }

    /**
     * Creates an expression retrieving the value as an instance of the specified class.
     * The {@code xpath} argument follows the rule described in {@link #property(String)}.
     *
     * <p>The desired type of property values can be specified. For example if the property values should be numbers,
     * then {@code type} can be <code>{@linkplain Number}.class</code>. If property values can be of any type with no
     * conversion desired, then {@code type} should be {@code Object.class}.</p>
     *
     * @param  <V>    the type of the values to be fetched (compile-time value of {@code type}).
     * @param  xpath  the path to the property whose value will be returned by the {@code apply(R)} method.
     * @param  type   the type of the values to be fetched (run-time value of {@code <V>}).
     * @return an expression evaluating the referenced property value.
     */
    public abstract <V> Expression<R,V> property(String xpath, Class<V> type);

    /**
     * Creates a constant, literal value that can be used in expressions.
     * The given value should be data objects such as strings, numbers, dates or geometries.
     *
     * @param  <V>    the type of the value of the literal.
     * @param  value  the literal value. May be {@code null}.
     * @return a literal for the given value.
     */
    public <V> Expression<R,V> literal(final V value) {
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
     * @return a filter evaluating {@code expression1} = {@code expression2}.
     */
    public Filter<R> equal(final Expression<R,?> expression1,
                           final Expression<R,?> expression2)
    {
        return new ComparisonFilter.EqualTo<>(expression1, expression2, true, MatchAction.ANY);
    }

    /**
     * Filter operator that compares that its two sub-expressions are not equal to each other.
     *
     * @param  expression1     the first of the two expressions to be used by this comparator.
     * @param  expression2     the second of the two expressions to be used by this comparator.
     * @return a filter evaluating {@code expression1} ≠ {@code expression2}.
     */
    public Filter<R> notEqual(final Expression<R,?> expression1,
                              final Expression<R,?> expression2)
    {
        return new ComparisonFilter.NotEqualTo<>(expression1, expression2, true, MatchAction.ANY);
    }

    /**
     * Filter operator that checks that its first sub-expression is less than its second sub-expression.
     *
     * @param  expression1     the first of the two expressions to be used by this comparator.
     * @param  expression2     the second of the two expressions to be used by this comparator.
     * @return a filter evaluating {@code expression1} &lt; {@code expression2}.
     */
    public Filter<R> less(final Expression<R,?> expression1,
                          final Expression<R,?> expression2)
    {
        return new ComparisonFilter.LessThan<>(expression1, expression2, true, MatchAction.ANY);
    }

    /**
     * Filter operator that checks that its first sub-expression is greater than its second sub-expression.
     *
     * @param  expression1     the first of the two expressions to be used by this comparator.
     * @param  expression2     the second of the two expressions to be used by this comparator.
     * @return a filter evaluating {@code expression1} &gt; {@code expression2}.
     */
    public Filter<R> greater(final Expression<R,?> expression1,
                             final Expression<R,?> expression2)
    {
        return new ComparisonFilter.GreaterThan<>(expression1, expression2, true, MatchAction.ANY);
    }

    /**
     * Filter operator that checks that its first sub-expression is less than or equal to its second sub-expression.
     *
     * @param  expression1     the first of the two expressions to be used by this comparator.
     * @param  expression2     the second of the two expressions to be used by this comparator.
     * @return a filter evaluating {@code expression1} ≤ {@code expression2}.
     */
    public Filter<R> lessOrEqual(final Expression<R,?> expression1,
                                 final Expression<R,?> expression2)
    {
        return new ComparisonFilter.LessThanOrEqualTo<>(expression1, expression2, true, MatchAction.ANY);
    }

    /**
     * Filter operator that checks that its first sub-expression is greater than its second sub-expression.
     *
     * @param  expression1     the first of the two expressions to be used by this comparator.
     * @param  expression2     the second of the two expressions to be used by this comparator.
     * @return a filter evaluating {@code expression1} ≥ {@code expression2}.
     */
    public Filter<R> greaterOrEqual(final Expression<R,?> expression1,
                                    final Expression<R,?> expression2)
    {
        return new ComparisonFilter.GreaterThanOrEqualTo<>(expression1, expression2, true, MatchAction.ANY);
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
    public Filter<R> between(final Expression<R,?> expression,
                             final Expression<R,?> lowerBoundary,
                             final Expression<R,?> upperBoundary)
    {
        return new ComparisonFilter.Between<>(expression, lowerBoundary, upperBoundary);
    }

    /**
     * Character string comparison operator with pattern matching and default wildcards.
     * The wildcard character is {@code '%'}, the single character is {@code '_'} and
     * the escape character is {@code '\\'}. The comparison is case-sensitive.
     *
     * @param  expression  source of values to compare against the pattern.
     * @param  pattern     pattern to match against expression values.
     * @return a character string comparison operator with pattern matching.
     */
    public Filter<R> like(Expression<R,?> expression, String pattern) {
        return like(expression, pattern, '%', '_', '\\', true);
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
    public Filter<R> like(final Expression<R,?> expression, final String pattern,
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
    public Filter<R> isNull(final Expression<R,?> expression) {
        return new UnaryFunction.IsNull<>(expression);
    }

    /**
     * An operator that tests if an expression's value is nil.
     * The difference with {@code NullOperator} is that a value should exist
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
    public Filter<R> isNil(final Expression<R,?> expression, final String nilReason) {
        return new UnaryFunction.IsNil<>(expression, nilReason);
    }

    /**
     * Creates a {@code AND} filter between two filters.
     *
     * @param  operand1  the first operand of the AND operation.
     * @param  operand2  the second operand of the AND operation.
     * @return a filter evaluating {@code operand1 AND operand2}.
     */
    public Filter<R> and(final Filter<R> operand1, final Filter<R> operand2) {
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
     */
    public Filter<R> and(final Collection<? extends Filter<R>> operands) {
        return new LogicalFilter.And<>(operands);
    }

    /**
     * Creates a {@code OR} filter between two filters.
     *
     * @param  operand1  the first operand of the OR operation.
     * @param  operand2  the second operand of the OR operation.
     * @return a filter evaluating {@code operand1 OR operand2}.
     */
    public Filter<R> or(final Filter<R> operand1, final Filter<R> operand2) {
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
     */
    public Filter<R> or(final Collection<? extends Filter<R>> operands) {
        return new LogicalFilter.Or<>(operands);
    }

    /**
     * Creates a {@code NOT} filter for the given filter.
     *
     * @param  operand  the operand of the NOT operation.
     * @return a filter evaluating {@code NOT operand}.
     */
    public Filter<R> not(final Filter<R> operand) {
        return new LogicalFilter.Not<>(operand);
    }

    /**
     * Creates an operator that checks if the bounding box of the feature's geometry interacts
     * with the bounding box provided in the filter properties.
     *
     * @param  geometry  expression fetching the geometry to check for interaction with bounds.
     * @param  bounds    the bounds to check geometry against.
     * @return a filter checking for any interactions between the bounding boxes.
     */
    public Filter<R> bbox(final Expression<R, ? extends G> geometry, final Envelope bounds) {
        return new BinarySpatialFilter<>(library, geometry, bounds, wraparound);
    }

    /**
     * Creates an operator that checks if the geometry of the two operands are equal.
     *
     * @param  geometry1  expression fetching the first geometry of the binary operator.
     * @param  geometry2  expression fetching the second geometry of the binary operator.
     * @return a filter for the "Equals" operation between the two geometries.
     */
    public Filter<R> equals(final Expression<R, ? extends G> geometry1,
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
     */
    public Filter<R> disjoint(final Expression<R, ? extends G> geometry1,
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
     */
    public Filter<R> intersects(final Expression<R, ? extends G> geometry1,
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
     */
    public Filter<R> touches(final Expression<R, ? extends G> geometry1,
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
     */
    public Filter<R> crosses(final Expression<R, ? extends G> geometry1,
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
     */
    public Filter<R> within(final Expression<R, ? extends G> geometry1,
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
     */
    public Filter<R> contains(final Expression<R, ? extends G> geometry1,
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
     */
    public Filter<R> overlaps(final Expression<R, ? extends G> geometry1,
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
     */
    public Filter<R> beyond(final Expression<R, ? extends G> geometry1,
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
     */
    public Filter<R> within(final Expression<R, ? extends G> geometry1,
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
     */
    public Filter<R> after(final Expression<R, ? extends T> time1,
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
     */
    public Filter<R> before(final Expression<R, ? extends T> time1,
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
     */
    public Filter<R> begins(final Expression<R, ? extends T> time1,
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
     */
    public Filter<R> begunBy(final Expression<R, ? extends T> time1,
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
     */
    public Filter<R> tcontains(final Expression<R, ? extends T> time1,
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
     */
    public Filter<R> during(final Expression<R, ? extends T> time1,
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
     */
    public Filter<R> tequals(final Expression<R, ? extends T> time1,
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
     */
    public Filter<R> toverlaps(final Expression<R, ? extends T> time1,
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
     */
    public Filter<R> meets(final Expression<R, ? extends T> time1,
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
     */
    public Filter<R> ends(final Expression<R, ? extends T> time1,
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
     */
    public Filter<R> overlappedBy(final Expression<R, ? extends T> time1,
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
     */
    public Filter<R> metBy(final Expression<R, ? extends T> time1,
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
     */
    public Filter<R> endedBy(final Expression<R, ? extends T> time1,
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
     */
    public Filter<R> anyInteracts(final Expression<R, ? extends T> time1,
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
     */
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
     */
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
     */
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
     */
    public Expression<R,Number> divide(final Expression<R, ? extends Number> operand1,
                                       final Expression<R, ? extends Number> operand2)
    {
        return new ArithmeticFunction.Divide<>(operand1, operand2);
    }

    /**
     * Creates an implementation-specific function with a single parameter.
     *
     * @param  name       name of the function to call.
     * @param  parameter  expression providing values for the function argument.
     * @return an expression which will call the specified function.
     * @throws IllegalArgumentException if the given name is not recognized,
     *         or if the argument is illegal for the specified function.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Expression<R,?> function(String name, Expression<? super R, ?> parameter) {
        return function(name, new Expression[] {parameter});
    }

    /**
     * Creates an implementation-specific function with two parameters.
     *
     * @param  name    name of the function to call.
     * @param  param1  expression providing values for the first function argument.
     * @param  param2  expression providing values for the second function argument.
     * @return an expression which will call the specified function.
     * @throws IllegalArgumentException if the given name is not recognized,
     *         or if the arguments are illegal for the specified function.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Expression<R,?> function(String name, Expression<? super R, ?> param1, Expression<? super R, ?> param2) {
        return function(name, new Expression[] {param1, param2});
    }

    /**
     * Creates an implementation-specific function.
     *
     * @param  name        name of the function to call.
     * @param  parameters  expressions providing values for the function arguments.
     * @return an expression which will call the specified function.
     * @throws IllegalArgumentException if the given name is not recognized,
     *         or if the arguments are illegal for the specified function.
     */
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
