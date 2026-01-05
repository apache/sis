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
package org.apache.sis.filter.base;

import java.util.Set;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.io.Serializable;
import org.opengis.util.CodeList;
import org.opengis.util.LocalName;
import org.opengis.util.ScopedName;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.feature.internal.shared.FeatureExpression;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.internal.shared.ReferencingUtilities;
import org.apache.sis.util.Classes;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.collection.DefaultTreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.system.Loggers;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Filter;
import org.opengis.filter.Expression;
import org.opengis.filter.InvalidFilterValueException;
import org.opengis.feature.AttributeType;


/**
 * Base class of Apache <abbr>SIS</abbr> implementations of <abbr>OGC</abbr> expressions, comparators and filters.
 * {@code Node} instances are organized in a tree which can be formatted by {@link #toString()}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class Node implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -749201100175374658L;

    /**
     * The logger for all operations relative to filters.
     */
    public static final Logger LOGGER = Logger.getLogger(Loggers.FILTER);

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
    public static <T> AttributeType<T> createType(final Class<T> type, final Object name) {
        // We do not use `Map.of(…)` for better exception message in case of null name.
        return new DefaultAttributeType<>(Collections.singletonMap(DefaultAttributeType.NAME_KEY, name),
                                          type, 1, 1, null, (AttributeType<?>[]) null);
    }

    /**
     * Returns the most specialized class of the given pair of classes. A specialized class is guaranteed to exist
     * if parametrized type safety has not been bypassed with unchecked casts, because {@code <R>} is always valid.
     * However, this method is not guaranteed to be able to find that specialized type, because it could be none of
     * the given arguments if {@code t1}, {@code t2} and {@code <R>} are interfaces with {@code <R>} extending both
     * {@code t1} and {@code t2}.
     *
     * @param  <R>  the compile-time type of resources expected by filters or expressions.
     * @param  t1   the runtime type of resources expected by the first filter or expression. May be null.
     * @param  t2   the runtime type of resources expected by the second filter or expression. May be null.
     * @return the most specialized type of resources, or {@code null} if it cannot be determined.
     */
    protected static <R> Class<? super R> specializedClass(final Class<? super R> t1, final Class<? super R> t2) {
        if (t1 != null && t2 != null) {
            if (t1.isAssignableFrom(t2)) return t2;
            if (t2.isAssignableFrom(t1)) return t1;
        }
        return null;
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
     * <h4>Note for implementers</h4>
     * Implementations typically return a hard-coded value. If the returned value may vary for the same class,
     * then implementers should override also the {@link #equals(Object)} and {@link #hashCode()} methods.
     *
     * @return the name of this function.
     */
    public final Object getDisplayName() {
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
     * This is a helper method for {@link Expression#getFunctionName()} implementations.
     *
     * @param  tip  the expression name in SIS namespace.
     * @return an expression name in the SIS namespace.
     */
    public static ScopedName createName(final String tip) {
        return Names.createScopedName(SCOPE, null, tip);
    }

    /**
     * Returns an expression whose results is a geometry wrapper.
     * Note that the {@code apply(R)} method of the returned expression may throw {@link BackingStoreException}.
     *
     * @param  <R>         the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
     * @param  <G>         the geometry implementation type.
     * @param  library     the geometry library to use.
     * @param  expression  the expression providing geometry instances of the given library.
     * @return an expression whose results is a geometry wrapper.
     * @throws InvalidFilterValueException if the given expression is already a wrapper
     *         but for another geometry implementation.
     */
    protected static <R,G> Expression<R, GeometryWrapper> toGeometryWrapper(
            final Geometries<G> library, final Expression<R,?> expression)
    {
        return GeometryConverter.create(library, expression);
    }

    /**
     * If the given exception was wrapped by {@link #toGeometryWrapper(Geometries, Expression)},
     * returns the original expression. Otherwise, returns the given expression as-is.
     *
     * @param  <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
     * @param  expression  the expression to unwrap.
     * @return the unwrapped expression.
     */
    protected static <R> Expression<R,?> unwrap(final Expression<R, GeometryWrapper> expression) {
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
     * @param  expression   the expression for which to get the geometry library.
     * @return the geometry library (never {@code null}).
     */
    protected static Geometries<?> getGeometryLibrary(final Expression<?, GeometryWrapper> expression) {
        if (expression instanceof GeometryConverter<?,?>) {
            return ((GeometryConverter<?,?>) expression).library;
        }
        throw new InvalidFilterValueException(Resources.format(Resources.Keys.NotAGeometryAtFirstExpression));
    }

    /**
     * The set of all properties that may be present in {@link FeatureExpression#properties()}
     * when the only available information is the list of parameters. When we do not know how
     * an expression is using the parameters, the function properties should be the empty set.
     * When combining a function properties with the properties inherited from the parameters,
     * the only properties that can be added to an initially empty set are the properties that
     * are {@linkplain FunctionProperty#concatenate(Set, Set) concatenated} with the logical
     * {@code OR} operation. In the current {@link FunctionProperty} enumeration, the only
     * property handled that way is {@code VOLATILE}.
     */
    private static final Set<FunctionProperty> TRANSITIVE_PROPERTIES = Set.of(FunctionProperty.VOLATILE);

    /**
     * Whether the given set of properties contains the {@link #TRANSITIVE_PROPERTIES} singleton value.
     * If a future version recognizes more properties, the return type will no longer be a boolean.
     */
    private static boolean isVolatile(final Set<FunctionProperty> properties) {
        return properties.contains(FunctionProperty.VOLATILE);
    }

    /**
     * Whether the combination of the function properties of all given expression is {@link #TRANSITIVE_PROPERTIES}.
     * This method assumes that {@code TRANSITIVE_PROPERTIES} is a singleton and that the property can be combined
     * by a logical {@code OR} operation.
     *
     * @param  operands  the expressions from which to get the function properties.
     * @return whether is present the single function property that may appear.
     */
    private static <R> boolean isVolatile(final Iterable<Expression<R,?>> operands) {
        for (final Expression<R,?> operand : operands) {
            if (operand instanceof FeatureExpression<?,?>) {
                if (isVolatile(((FeatureExpression<?,?>) operand).properties())) {
                    return true;    // Short-circuit for `OR` logical operation.
                }
            } else {
                if (isVolatile(operand.getParameters())) {
                    return true;    // Short-circuit for `OR` logical operation.
                }
            }
        }
        return false;
    }

    /**
     * Returns the manner in which values are computed from resources.
     * This method delegates to {@link FeatureExpression#properties()} if possible.
     * Otherwise this method assumes that the intrinsic properties of the given expression are unknown,
     * and inherits from the parameters only the properties that can be added to an initially empty set.
     *
     * @param  function  the expression for which to query function properties, or {@code null}.
     * @return the manners in which values are computed from resources.
     */
    public static Set<FunctionProperty> properties(final Expression<?,?> function) {
        if (function instanceof FeatureExpression<?,?>) {
            return ((FeatureExpression<?,?>) function).properties();
        } else if (function != null) {
            return transitiveProperties(function.getParameters());
        } else {
            return Set.of();
        }
    }

    /**
     * Returns the manner in which values are computed from resources in an expression having the given operands.
     * This method assumes that the intrinsic properties of the parent expression or parent filter are unknown,
     * and inherits from the operands only the properties that can be added to an initially empty set.
     *
     * <p>Note that {@code transitiveProperties(function.getParameters())} is <strong>not</strong> equivalent to
     * {@code properties(function)}. It is rather equivalent to the following code, where the parent expression
     * is not the final step of a chain of operations, and the next step has no known properties:</p>
     *
     * {@snippet lang="java" :
     *     FunctionProperty.concatenate(transitiveProperties(operands), Set.of());
     *     }
     *
     * @param  <R>       the type of resources.
     * @param  operands  the operands from which to inherit function properties.
     * @return the manners in which values are computed from resources.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")             // Because immutable.
    public static <R> Set<FunctionProperty> transitiveProperties(final Iterable<Expression<R,?>> operands) {
        return isVolatile(operands) ? TRANSITIVE_PROPERTIES : Set.of();
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
            } else if (child instanceof IdentifiedObject) {
                final var object = (IdentifiedObject) child;
                value = Classes.getShortName(ReferencingUtilities.getInterface(object))
                        + "[“" + IdentifiedObjects.getDisplayName(object, null) + "”]";
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
        final var table = new DefaultTreeTable(TableColumn.VALUE);
        toTree(table.getRoot(), new IdentityHashMap<>());
        return table.toString();
    }

    /**
     * Returns a hash code value computed from the class and the children.
     *
     * @return a hash code value.
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
     * Reports that an operation failed because of the given exception, resulting in a null value.
     * This method assumes that the warning occurred in a {@code test(…)} or {@code apply(…)} method.
     *
     * @param  exception  the exception that occurred.
     */
    protected final void warning(final Exception exception) {
        warning(exception, false);
    }

    /**
     * Reports that an operation failed because of the given exception.
     * This method assumes that the warning occurred in a {@code test(…)} or {@code apply(…)} method.
     *
     * @param  exception    the exception that occurred.
     * @param  recoverable  {@code true} if the caller has been able to fallback on a default value,
     *                      or {@code false} if the caller has to return {@code null} or {@code false}.
     *
     * @todo Consider defining a {@code Context} class providing, among other information, listeners where to report warnings.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-460">SIS-460</a>
     */
    protected final void warning(final Exception exception, final boolean recoverable) {
        final Consumer<WarningEvent> listener = WarningEvent.LISTENER.get();
        if (listener != null) {
            listener.accept(new WarningEvent(this, exception, recoverable));
        } else {
            final String method = (this instanceof Predicate) ? "test" : "apply";
            if (recoverable) {
                Logging.recoverableException(LOGGER, getClass(), method, exception);
            } else {
                Logging.unexpectedException(LOGGER, getClass(), method, exception);
            }
        }
    }
}
