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
package org.apache.sis.internal.filter;

import java.util.Map;
import java.util.IdentityHashMap;
import java.util.Collection;
import java.io.Serializable;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.logging.Logger;
import org.opengis.util.CodeList;
import org.opengis.util.LocalName;
import org.opengis.util.ScopedName;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.internal.feature.Resources;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryWrapper;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.collection.DefaultTreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.internal.system.Loggers;

// Branch-dependent imports
import org.opengis.filter.Filter;
import org.opengis.filter.Expression;
import org.opengis.filter.InvalidFilterValueException;
import org.opengis.feature.AttributeType;


/**
 * Base class of Apache SIS implementation of OGC expressions, comparators or filters.
 * {@code Node} instances are associated together in a tree, which can be formatted
 * by {@link #toString()}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class Node implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -749201100175374658L;

    /**
     * Scope of all names defined by SIS convention.
     *
     * @see #createName(String)
     */
    private static final LocalName SCOPE = Names.createLocalName("Apache", null, "sis");

    /**
     * Creates a new expression, operator or filter.
     */
    protected Node() {
    }

    /**
     * Creates an attribute type for values of the given type and name.
     * The attribute is mandatory, unbounded and has no default value.
     *
     * @param  <T>   compile-time value of {@code type}.
     * @param  type  type of values in the attribute.
     * @param  name  name of the attribute to create.
     * @return an attribute of the given type and name.
     *
     * @see Expression#getFunctionName()
     */
    protected static <T> AttributeType<T> createType(final Class<T> type, final Object name) {
        return new DefaultAttributeType<>(Collections.singletonMap(DefaultAttributeType.NAME_KEY, name),
                                          type, 1, 1, null, (AttributeType<?>[]) null);
    }

    /**
     * Returns the mathematical symbol for this binary function.
     * For comparison operators, the symbol should be one of {@literal < > ≤ ≥ = ≠}.
     * For arithmetic operators, the symbol should be one of {@literal + − × ÷}.
     *
     * @return the mathematical symbol, or 0 if none.
     */
    protected char symbol() {
        return (char) 0;
    }

    /**
     * Returns the name of the function or filter to be called.
     * For example, this might be {@code "sis:cos"} or {@code "sis:atan2"}.
     * The type depend on the implemented interface:
     *
     * <ul>
     *   <li>{@link ScopedName} if this node implements {@link Expression}.</li>
     *   <li>{@link CodeList} if this node implements {@link Filter}.</li>
     * </ul>
     *
     * <div class="note"><b>Note for implementers:</b>
     * implementations typically return a hard-coded value. If the returned value may vary for the same class,
     * then implementers should override also the {@link #equals(Object)} and {@link #hashCode()} methods.</div>
     *
     * @return the name of this function.
     */
    private Object getDisplayName() {
        if (this instanceof Expression<?,?>) {
            return ((Expression<?,?>) this).getFunctionName();
        } else if (this instanceof Filter<?>) {
            return ((Filter<?>) this).getOperatorType();
        } else {
            return getClass().getSimpleName();
        }
    }

    /**
     * Creates a name in the "SIS" scope.
     * This is a helper method for {@link #getFunctionName()} implementations.
     *
     * @param  tip  the expression name in SIS namespace.
     * @return an expression name in the SIS namespace.
     */
    protected static ScopedName createName(final String tip) {
        return Names.createScopedName(SCOPE, null, tip);
    }

    /**
     * Returns an expression whose results is a geometry wrapper.
     *
     * @param  <R>         the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
     * @param  <G>         the geometry implementation type.
     * @param  library     the geometry library to use.
     * @param  expression  the expression providing source values.
     * @return an expression whose results is a geometry wrapper.
     * @throws InvalidFilterValueException if the given expression is already a wrapper
     *         but for another geometry implementation.
     */
    @SuppressWarnings("unchecked")
    protected static <R,G> Expression<R, GeometryWrapper<G>> toGeometryWrapper(
            final Geometries<G> library, final Expression<R,?> expression)
    {
        if (expression instanceof GeometryConverter<?,?>) {
            if (library.equals(((GeometryConverter<?,?>) expression).library)) {
                return (GeometryConverter<R,G>) expression;
            } else {
                throw new InvalidFilterValueException();        // TODO: provide a message.
            }
        }
        return new GeometryConverter<>(library, expression);
    }

    /**
     * If the given exception was wrapped by {@link #toGeometryWrapper(Geometries, Expression)},
     * returns the original expression. Otherwise returns the given expression.
     *
     * @param  <R>         the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
     * @param  <G>         the geometry implementation type.
     * @param  expression  the expression to unwrap.
     * @return the unwrapped expression.
     */
    protected static <R,G> Expression<? super R, ?> unwrap(final Expression<R, GeometryWrapper<G>> expression) {
        if (expression instanceof GeometryConverter<?,?>) {
            return ((GeometryConverter<R,?>) expression).expression;
        } else {
            return expression;
        }
    }

    /**
     * Returns a handler for the library of geometric objects used by the given expression.
     * The given expression should be the first parameter (as requested by SQLMM specification),
     * otherwise the error message will not be accurate.
     *
     * @param  <G>          the type of geometry created by the expression.
     * @param  expression   the expression for which to get the geometry library.
     * @return the geometry library (never {@code null}).
     */
    protected static <G> Geometries<G> getGeometryLibrary(final Expression<?, GeometryWrapper<G>> expression) {
        if (expression instanceof GeometryConverter<?,?>) {
            return ((GeometryConverter<?,G>) expression).library;
        }
        throw new InvalidFilterValueException(Resources.format(Resources.Keys.NotAGeometryAtFirstExpression));
    }

    /**
     * Returns the children of this node, or an empty collection if none. This is used
     * for information purpose, for example in order to build a string representation.
     *
     * @return the children of this node, or an empty collection if none.
     */
    protected abstract Collection<?> getChildren();

    /**
     * Builds a tree representation of this node, including all children. This method expects an
     * initially empty node, which will be set to the {@linkplain #getFunctionName() name} of this node.
     * Then all children will be appended recursively, with a check against cyclic graph.
     *
     * @param  root     where to create a tree representation of this node.
     * @param  visited  nodes already visited. This method will write in this map.
     */
    private void toTree(final TreeTable.Node root, final Map<Object,Boolean> visited) {
        root.setValue(TableColumn.VALUE, getDisplayName());
        for (final Object child : getChildren()) {
            final TreeTable.Node node = root.newChild();
            final String value;
            if (child instanceof Node) {
                if (visited.putIfAbsent(child, Boolean.TRUE) == null) {
                    ((Node) child).toTree(node, visited);
                    continue;
                } else {
                    value = Vocabulary.format(Vocabulary.Keys.CycleOmitted);
                }
            } else {
                value = String.valueOf(child);
            }
            node.setValue(TableColumn.VALUE, value);
        }
    }

    /**
     * Returns a string representation of this node. This representation can be printed
     * to the {@linkplain System#out standard output stream} (for example) if it uses a
     * monospaced font and supports Unicode.
     *
     * @return a string representation of this filter.
     */
    @Override
    public final String toString() {
        final DefaultTreeTable table = new DefaultTreeTable(TableColumn.VALUE);
        toTree(table.getRoot(), new IdentityHashMap<>());
        return table.toString();
    }

    /**
     * Returns a hash code value computed from the class and the children.
     */
    @Override
    public int hashCode() {
        return getClass().hashCode() + 37 * getChildren().hashCode();
    }

    /**
     * Returns {@code true} if the given object is an instance of the same class with the equal children.
     *
     * @param  other  the other object to compare with this node.
     * @return whether the two object are equal.
     */
    @Override
    public boolean equals(final Object other) {
        if (other != null && other.getClass() == getClass()) {
            return getChildren().equals(((Node) other).getChildren());
        }
        return false;
    }

    /**
     * Reports that an operation failed because of the given exception.
     * This method assumes that the warning occurred in a {@code test(…)} or {@code apply(…)} method.
     *
     * @param  e            the exception that occurred.
     * @param  recoverable  {@code true} if the caller has been able to fallback on a default value,
     *                      or {@code false} if the caller has to return {@code null}.
     *
     * @todo Consider defining a {@code Context} class providing, among other information, listeners where to report warnings.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-460">SIS-460</a>
     */
    protected final void warning(final Exception e, final boolean recoverable) {
        final Logger logger = Logger.getLogger(Loggers.FILTER);
        final String method = (this instanceof Predicate) ? "test" : "apply";
        if (recoverable) {
            Logging.recoverableException(logger, getClass(), method, e);
        } else {
            Logging.unexpectedException(logger, getClass(), method, e);
        }
    }
}
