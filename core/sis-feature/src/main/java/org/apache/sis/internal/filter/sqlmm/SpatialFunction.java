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
package org.apache.sis.internal.filter.sqlmm;

import java.util.EnumMap;
import java.util.Collection;
import org.opengis.util.LocalName;
import org.opengis.util.ScopedName;
import org.apache.sis.filter.Optimization;
import org.apache.sis.internal.filter.Node;
import org.apache.sis.internal.feature.Resources;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.FeatureExpression;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.iso.Names;

// Branch-dependent imports
import org.opengis.feature.FeatureType;
import org.opengis.filter.Expression;
import org.opengis.filter.InvalidFilterValueException;


/**
 * Base class of SQLMM spatial functions.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @param  <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 *
 * @since 1.1
 * @module
 */
abstract class SpatialFunction<R> extends Node implements FeatureExpression<R,Object>, Optimization.OnExpression<R,Object> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6933519274722660893L;

    /**
     * Scope of all names defined by SQLMM standard.
     *
     * @see #createName(SQLMM)
     */
    private static final LocalName SCOPE = Names.createLocalName("ISO", null, "sqlmm");;

    /**
     * Identification of the SQLMM operation.
     */
    final SQLMM operation;

    /**
     * All operation names as {@link ScopedName} instances.
     * Values are {@linkplain #createName(SQLMM) created} when first needed.
     */
    private static final EnumMap<SQLMM, ScopedName> NAMES = new EnumMap<>(SQLMM.class);

    /**
     * Creates a new function. This constructor verifies that the number of parameters
     * is between {@link SQLMM#minParamCount} and {@link SQLMM#maxParamCount} inclusive,
     * but does not store the parameters. Parameters shall be stored by subclasses.
     *
     * @param  operation   identification of the SQLMM operation.
     * @param  parameters  sub-expressions that will be evaluated to provide the parameters to the function.
     * @throws IllegalArgumentException if the number of parameters is not in the expected range.
     */
    SpatialFunction(final SQLMM operation, final Expression<? super R, ?>[] parameters) {
        this.operation = operation;
        final int n = parameters.length;
        final int minParamCount = operation.minParamCount;
        final String message;
        if (n < minParamCount) {
            if (n == 0) {
                message = Errors.format(Errors.Keys.EmptyArgument_1, "parameters");
            } else {
                message = Errors.format(Errors.Keys.TooFewArguments_2, minParamCount, n);
            }
        } else {
            final int maxParamCount = operation.maxParamCount;
            if (n > maxParamCount) {
                message = Errors.format(Errors.Keys.TooManyArguments_2, maxParamCount, n);
            } else {
                return;
            }
        }
        throw new IllegalArgumentException(message);
    }

    /**
     * Returns a handler for the library of geometric objects used by this expression.
     * This is typically implemented by a call to {@code getGeometryLibrary(geometry)}
     * where {@code geometry} as the first expression returning a geometry object.
     *
     * @return the geometry library (never {@code null}).
     *
     * @see #getGeometryLibrary(Expression)
     */
    abstract Geometries<?> getGeometryLibrary();

    /**
     * Returns the name of the function to be called. This method returns
     * a scoped name with the {@link SQLMM} function name in the local part.
     */
    @Override
    public final ScopedName getFunctionName() {
        synchronized (NAMES) {
            return NAMES.computeIfAbsent(operation, SpatialFunction::createName);
        }
    }

    /**
     * Invoked by {@link #getFunctionName()} when a name needs to be created.
     */
    private static ScopedName createName(final SQLMM operation) {
        return Names.createScopedName(SCOPE, null, operation.name());
    }

    /**
     * Returns the children of this node, which are the {@linkplain #getParameters() parameters list}.
     * This is used for information purpose only, for example in order to build a string representation.
     *
     * @return the children of this node.
     */
    @Override
    protected final Collection<?> getChildren() {
        return getParameters();
    }

    /**
     * Returns a Backus-Naur Form (BNF) of this function.
     *
     * @todo Fetch parameter names from {@code FilterCapabilities}.
     *       Or maybe use annotations, which would also be used for capabilities implementation.
     */
    public String getSyntax() {
        final int minParamCount = operation.minParamCount;
        final int maxParamCount = operation.maxParamCount;
        final StringBuilder sb = new StringBuilder();
        sb.append(getFunctionName().tip()).append('(');
        for (int i = 0; i < maxParamCount; i++) {
            if (i == minParamCount) sb.append('[');
            if (i != 0) sb.append(", ");
            sb.append("param").append(i + 1);
        }
        if (maxParamCount > minParamCount) {
            sb.append(']');
        }
        return sb.append(')').toString();
    }

    /**
     * Returns the kind of objects evaluated by this expression.
     */
    @Override
    public final Class<?> getValueClass() {
        return operation.getReturnType(getGeometryLibrary());
    }

    /**
     * Returns {@code this} if this expression provides values of the specified type,
     * or throws an exception otherwise.
     */
    @Override
    @SuppressWarnings("unchecked")
    public final <N> Expression<R,N> toValueType(final Class<N> target) {
        if (target.isAssignableFrom(getValueClass())) {
            return (Expression<R,N>) this;
        } else {
            throw new ClassCastException(Errors.format(Errors.Keys.CanNotConvertValue_2, getFunctionName(), target));
        }
    }

    /**
     * Provides the type of values produced by this expression when a feature of the given type is evaluated.
     * There are two cases:
     *
     * <ul class="verbose">
     *   <li>If the operation expects at least one geometric parameter and returns a geometry,
     *       then the characteristics of the first parameter (in particular the CRS) are copied.
     *       The first parameter is used as a template for compliance with SQLMM specification.</li>
     *   <li>Otherwise an attribute is created with the return value specified by the operation.</li>
     * </ul>
     *
     * @param  valueType  the type of features on which to apply this expression.
     * @param  addTo      where to add the type of properties evaluated by this expression.
     * @return builder of type resulting from expression evaluation (never null).
     * @throws InvalidFilterValueException if the given feature type does not contain the expected properties,
     *         or if this method can not determine the result type of the expression.
     *         It may be because that expression is backed by an unsupported implementation.
     */
    @Override
    public PropertyTypeBuilder expectedType(final FeatureType valueType, final FeatureTypeBuilder addTo) {
        AttributeTypeBuilder<?> att;
cases:  if (operation.isGeometryInOut()) {
            final FeatureExpression<?,?> fex = FeatureExpression.castOrCopy(getParameters().get(0));
            if (fex != null) {
                final PropertyTypeBuilder type = fex.expectedType(valueType, addTo);
                if (type instanceof AttributeTypeBuilder<?>) {
                    att = (AttributeTypeBuilder<?>) type;
                    final Geometries<?> library = Geometries.implementation(att.getValueClass());
                    if (library != null) {
                        att = att.setValueClass(operation.getReturnType(library));
                        break cases;
                    }
                }
            }
            throw new InvalidFilterValueException(Resources.format(Resources.Keys.NotAGeometryAtFirstExpression));
        } else {
            att = addTo.addAttribute(getValueClass());
        }
        return att.setName(getFunctionName());
    }
}
