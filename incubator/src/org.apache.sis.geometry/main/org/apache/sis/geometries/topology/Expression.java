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
package org.apache.sis.geometries.topology;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * A multivariate polynomial of degree 1 whose variables are oriented primitives, such as
 * 2<var>e₁</var> − <var>e₂</var>. It is the basis of computational topology: geometric problems
 * are translated into algebraic ones, solved there, and the solutions are translated back.
 *
 * <p>The order of the terms does not change the value of a polynomial, so an expression is
 * a set of {@linkplain ExpressionTerm terms}, each one an integer coefficient and an oriented primitive.
 * Expressions behave as vectors having the oriented primitives as basis vectors.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 10.1.8
 */
@UML(identifier="Expression", specification=ISO_19107)
public interface Expression {

    /**
     * The terms of this expression, in no particular order. Terms having the same underlying primitive
     * are combined, and terms of coefficient zero are removed, so each primitive appears at most once.
     *
     * @return terms of this expression, empty for the zero expression.
     *
     * @see ISO 19107:2019 - 10.1.8.1
     */
    Collection<ExpressionTerm> getTerms();

    /**
     * Returns {@code true} if this expression has no term.
     * This method is not in ISO 19107, which refers to the zero expression in several places.
     *
     * @return whether this expression is zero.
     */
    default boolean isZero() {
        return getTerms().isEmpty();
    }

    /**
     * This expression with every term reversed: the unary minus of polynomials.
     *
     * @return the opposite of this expression.
     *
     * @see ISO 19107:2019 - 10.1.8.2
     */
    @UML(identifier="negate", specification=ISO_19107)
    Expression getNegate();

    /**
     * This expression with each primitive replaced by its boundary, then simplified.
     * The boundary is made of primitives of the next lower dimension, and is zero
     * if all primitives of this expression are nodes.
     *
     * @return boundary of this expression.
     *
     * @see ISO 19107:2019 - 10.1.8.3
     */
    @UML(identifier="boundary", specification=ISO_19107)
    Expression getBoundary();

    /**
     * This expression with each primitive replaced by its co-boundary, then simplified.
     * The co-boundary is made of primitives of the next higher dimension, and is zero if the primitives
     * of this expression have the dimension of their maximal complex.
     *
     * @return co-boundary of this expression.
     *
     * @see ISO 19107:2019 - 10.1.8.4
     */
    @UML(identifier="coBoundary", specification=ISO_19107)
    Expression getCoBoundary();

    /**
     * This expression as a set of primitives, for computing geometric operators: the primitives
     * of the {@linkplain #asSet() oriented primitives set}. The support is a valid {@link Complex},
     * and is empty for the zero expression.
     *
     * <p>The default implementation returns the {@linkplain DirectedTopo#getTopo() primitive}
     * of each element of {@link #asSet()}.</p>
     *
     * @return primitives of this expression and of their boundaries.
     *
     * @see ISO 19107:2019 - 10.1.8.5
     */
    @UML(identifier="support", specification=ISO_19107)
    default Set<Primitive> getSupport() {
        final Set<Primitive> support = new LinkedHashSet<>();
        for (final DirectedTopo element : asSet()) {
            support.add(element.getTopo());
        }
        return support;
    }

    /**
     * This expression as a set of oriented primitives, for computing geometric operators. The set contains
     * the variable of each term, and every element of their boundaries recursively down to the nodes.
     * It is empty for the zero expression.
     *
     * <p>The default implementation takes each variable with the sign of its coefficient,
     * then adds the boundaries recursively.</p>
     *
     * @return oriented primitives of this expression and of their boundaries.
     *
     * @see ISO 19107:2019 - 10.1.8.6
     */
    @UML(identifier="asSet", specification=ISO_19107)
    default Set<DirectedTopo> asSet() {
        final Set<DirectedTopo> set = new LinkedHashSet<>();
        final Deque<DirectedTopo> pending = new ArrayDeque<>();
        for (final ExpressionTerm term : getTerms()) {
            final DirectedTopo variable = term.getVariable();
            pending.add(term.getCoefficient() < 0 ? variable.getNegate() : variable);
        }
        DirectedTopo next;
        while ((next = pending.poll()) != null) {
            if (set.add(next)) {
                pending.addAll(next.getBoundary());
            }
        }
        return set;
    }

    /**
     * The vector sum of this expression and the given one. Terms having the same underlying primitive
     * are combined by adding their coefficients, and terms of coefficient zero are removed.
     *
     * @param  other  the expression to add to this expression.
     * @return sum of the two expressions.
     *
     * @see ISO 19107:2019 - 10.1.8.7
     */
    @UML(identifier="plus", specification=ISO_19107)
    Expression plus(Expression other);

    /**
     * The vector difference of this expression and the given one. Terms having the same underlying primitive
     * are combined by subtracting their coefficients, and terms of coefficient zero are removed.
     *
     * @param  other  the expression to subtract from this expression.
     * @return difference of the two expressions.
     *
     * @see ISO 19107:2019 - 10.1.8.8
     */
    @UML(identifier="minus", specification=ISO_19107)
    Expression minus(Expression other);

    /**
     * Returns {@code true} if the given object is an expression equal to this one as a polynomial.
     * The order of the terms does not matter.
     *
     * @param  other  the object to compare with this expression.
     * @return whether the given object is an equal polynomial.
     *
     * @see ISO 19107:2019 - 10.1.8.9
     */
    @UML(identifier="equals", specification=ISO_19107)
    @Override
    boolean equals(Object other);

    /**
     * Returns a hash code value consistent with {@link #equals(Object)},
     * therefore independent of the order of the terms.
     *
     * @return hash code value of this expression.
     */
    @Override
    int hashCode();

}
