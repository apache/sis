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
package org.apache.sis.geometries.internal.shared.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.sis.geometries.Orientable;
import org.apache.sis.geometries.topology.DirectedTopo;
import org.apache.sis.geometries.topology.Expression;
import org.apache.sis.geometries.topology.ExpressionTerm;


/**
 * An expression stored as a map from primitives to integer coefficients.
 *
 * <p>The map is in canonical form: each key is the positive orientation of its primitive, the sign of the
 * orientation being carried by the coefficient, and no coefficient is zero. For example −<var>e</var> is
 * stored as the coefficient −1 of <var>e</var>. Two expressions are therefore equal if their maps are equal,
 * whatever the order of their terms. Instances are immutable.</p>
 *
 * <p>Any {@link DirectedTopo} can be a variable, provided that its {@code equals} and {@code hashCode}
 * methods identify the oriented primitive, as the views of {@link IndexedComplex} do.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DefaultExpression implements Expression {

    /**
     * The expression without term.
     */
    public static final DefaultExpression ZERO = new DefaultExpression(new LinkedHashMap<>());

    /**
     * Coefficient of each primitive, in canonical form.
     */
    private final Map<DirectedTopo, Integer> coefficients;

    /**
     * Creates an expression from the given coefficients in canonical form, stored without copy.
     */
    private DefaultExpression(final LinkedHashMap<DirectedTopo, Integer> coefficients) {
        this.coefficients = Collections.unmodifiableMap(coefficients);
    }

    /**
     * Returns the expression of a single oriented primitive, of coefficient 1.
     *
     * @param  variable  the oriented primitive.
     * @return the expression of the given oriented primitive.
     */
    public static DefaultExpression of(final DirectedTopo variable) {
        final var map = new LinkedHashMap<DirectedTopo, Integer>();
        add(map, variable, 1);
        return new DefaultExpression(map);
    }

    /**
     * Returns the given expression as an instance of this class.
     *
     * @param  expression  the expression to convert.
     * @return the given expression if it is already an instance of this class, or a copy of its terms otherwise.
     */
    public static DefaultExpression of(final Expression expression) {
        if (expression instanceof DefaultExpression e) {
            return e;
        }
        final var map = new LinkedHashMap<DirectedTopo, Integer>();
        addAll(map, expression, 1);
        return new DefaultExpression(map);
    }

    /**
     * Returns the sum of the given oriented primitives, each of coefficient 1.
     *
     * @param  variables  the oriented primitives to add.
     * @return the sum of the given oriented primitives.
     */
    public static DefaultExpression sum(final Iterable<? extends DirectedTopo> variables) {
        final var map = new LinkedHashMap<DirectedTopo, Integer>();
        for (final DirectedTopo variable : variables) {
            add(map, variable, 1);
        }
        return new DefaultExpression(map);
    }

    /**
     * Adds a term to the given map, keeping the canonical form.
     */
    private static void add(final Map<DirectedTopo, Integer> map, DirectedTopo variable, int coefficient) {
        if (coefficient != 0) {
            if (variable.getOrientationSign() == Orientable.Sign.NEGATIVE) {
                variable = variable.getNegate();
                coefficient = Math.negateExact(coefficient);
            }
            map.merge(variable, coefficient, (a, b) -> {
                final int sum = Math.addExact(a, b);
                return (sum != 0) ? sum : null;             // null removes the term.
            });
        }
    }

    /**
     * Adds the terms of the given expression multiplied by the given factor to the given map.
     */
    private static void addAll(final Map<DirectedTopo, Integer> map, final Expression expression, final int factor) {
        for (final ExpressionTerm term : expression.getTerms()) {
            add(map, term.getVariable(), Math.multiplyExact(term.getCoefficient(), factor));
        }
    }

    /**
     * Returns the terms of this expression. The variable of each term is the positive orientation
     * of its primitive, and the sign of the coefficient gives the orientation.
     */
    @Override
    public Collection<ExpressionTerm> getTerms() {
        final List<ExpressionTerm> terms = new ArrayList<>(coefficients.size());
        coefficients.forEach((variable, coefficient) -> terms.add(new Term(coefficient, variable)));
        return Collections.unmodifiableList(terms);
    }

    /**
     * Returns the oriented primitives of this expression, each one with the sign of its coefficient.
     * A coefficient greater than 1 in absolute value gives a single element.
     */
    final List<DirectedTopo> toDirectedList() {
        final List<DirectedTopo> list = new ArrayList<>(coefficients.size());
        coefficients.forEach((variable, coefficient) -> list.add((coefficient > 0) ? variable : variable.getNegate()));
        return list;
    }

    @Override
    public boolean isZero() {
        return coefficients.isEmpty();
    }

    @Override
    public DefaultExpression getNegate() {
        final var map = new LinkedHashMap<DirectedTopo, Integer>();
        coefficients.forEach((variable, coefficient) -> map.put(variable, Math.negateExact(coefficient)));
        return new DefaultExpression(map);
    }

    /**
     * Returns the sum of the boundaries of the primitives, each multiplied by its coefficient.
     */
    @Override
    public DefaultExpression getBoundary() {
        final var map = new LinkedHashMap<DirectedTopo, Integer>();
        coefficients.forEach((variable, coefficient) -> {
            for (final DirectedTopo element : variable.getBoundary()) {
                add(map, element, coefficient);
            }
        });
        return new DefaultExpression(map);
    }

    /**
     * Returns the sum of the co-boundaries of the primitives, each multiplied by its coefficient.
     */
    @Override
    public DefaultExpression getCoBoundary() {
        final var map = new LinkedHashMap<DirectedTopo, Integer>();
        coefficients.forEach((variable, coefficient) -> {
            for (final DirectedTopo element : variable.getCoBoundary()) {
                add(map, element, coefficient);
            }
        });
        return new DefaultExpression(map);
    }

    @Override
    public DefaultExpression plus(final Expression other) {
        final var map = new LinkedHashMap<>(coefficients);
        addAll(map, other, 1);
        return new DefaultExpression(map);
    }

    @Override
    public DefaultExpression minus(final Expression other) {
        final var map = new LinkedHashMap<>(coefficients);
        addAll(map, other, -1);
        return new DefaultExpression(map);
    }

    /**
     * Returns {@code true} if the given object is an expression with the same terms, in any order.
     */
    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof Expression other) && coefficients.equals(of(other).coefficients);
    }

    @Override
    public int hashCode() {
        return coefficients.hashCode();
    }

    /**
     * Returns the terms of this expression, such as {@code "+2*Edge[0] -Edge[3]"}, or {@code "0"} if none.
     */
    @Override
    public String toString() {
        if (coefficients.isEmpty()) {
            return "0";
        }
        final var buffer = new StringBuilder();
        coefficients.forEach((variable, coefficient) -> {
            if (buffer.length() != 0) {
                buffer.append(' ');
            }
            buffer.append(coefficient < 0 ? '-' : '+');
            final int magnitude = Math.abs(coefficient);
            if (magnitude != 1) {
                buffer.append(magnitude).append('*');
            }
            buffer.append(variable);
        });
        return buffer.toString();
    }

    /**
     * A term of a {@link DefaultExpression}, which is also an expression of that single term.
     */
    private static final class Term implements ExpressionTerm {
        private final int coefficient;
        private final DirectedTopo variable;

        Term(final int coefficient, final DirectedTopo variable) {
            this.coefficient = coefficient;
            this.variable    = variable;
        }

        @Override
        public int getCoefficient() {
            return coefficient;
        }

        @Override
        public DirectedTopo getVariable() {
            return variable;
        }

        /**
         * Returns this term as an expression, on which all operations are delegated.
         */
        private DefaultExpression expression() {
            return of(this);
        }

        @Override public Expression getNegate()               {return expression().getNegate();}
        @Override public Expression getBoundary()             {return expression().getBoundary();}
        @Override public Expression getCoBoundary()           {return expression().getCoBoundary();}
        @Override public Expression plus(Expression other)    {return expression().plus(other);}
        @Override public Expression minus(Expression other)   {return expression().minus(other);}
        @Override public boolean    equals(Object obj)        {return expression().equals(obj);}
        @Override public int        hashCode()                {return expression().hashCode();}
        @Override public String     toString()                {return expression().toString();}
    }
}
