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
package org.apache.sis.feature.internal.shared;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import org.opengis.util.GenericName;
import org.apache.sis.util.Debug;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CorruptedObjectException;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.pending.jdk.Record;
import org.apache.sis.pending.jdk.JDK19;
import org.apache.sis.feature.Features;
import org.apache.sis.feature.FeatureOperations;
import org.apache.sis.feature.AbstractOperation;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.apache.sis.filter.visitor.ListingPropertyVisitor;
import org.apache.sis.io.TableAppender;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.feature.Operation;
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;
import org.opengis.filter.ValueReference;


/**
 * A function applying projections (with "projection" in the <abbr>SQL</abbr> database sense) of features.
 * Given full feature instances in input, the function returns feature instances containing only a subset
 * of the properties. The property values may also be different if they are computed by the expressions.
 *
 * @author Guilhem Legal (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class FeatureProjection extends Record implements UnaryOperator<Feature> {
    /**
     * The type of features with the properties explicitly requested by the user.
     * The property names may differ from the properties of the {@link FeatureProjectionBuilder#source() source}
     * features if aliases were specified by {@link FeatureProjectionBuilder.Item#setPreferredName(GenericName)}.
     *
     * <h4>Relationship with {@code typeWithDependencies}</h4>
     * The property <em>names</em> of {@code typeRequested} shall be a subset of the property names of
     * {@link #typeWithDependencies}. However, a property of the same name may be an {@link Operation}
     * in {@code typeWithDependencies} and replaced by {@link OperationView} in {@code typeRequested}.
     * This replacement is done by {@link FeatureProjectionBuilder.Item#replaceIfMissingDependency()}.
     * It is necessary because the {@link org.apache.sis.feature.DefaultFeatureType} constructor
     * verifies that all dependencies of all operations exist.
     */
    public final FeatureType typeRequested;

    /**
     * The requested type augmented with dependencies required for the execution of operations such as links.
     * If there is no need for additional properties, then this value is the same as {@link #typeRequested}.
     * The property names are the same as {@link #typeRequested} (i.e., may be aliases).
     * However, some operations may be wrapped in {@link OperationView}.
     *
     * <p>This is <em>not</em> a container listing the properties of the source feature that are required.
     * This type still assume that the {@link #apply(Feature)} method will receive complete source features.
     * This type may differ from {@link #typeRequested} only when the latter contains operation that have not
     * been converted to stored attributes.</p>
     *
     * @see #requiredSourceProperties()
     */
    public final FeatureType typeWithDependencies;

    /**
     * Names of the properties to be stored in the feature instances created by this {@code FeatureProjection}.
     * Properties that are computed on-the-fly from other properties are not included in this array. Properties
     * that are present in {@link #typeRequested} but not in the source features are not in this array neither.
     */
    private final String[] propertiesToCopy;

    /**
     * Expressions to apply on the source feature for fetching the property values of the projected feature.
     * This array has the same length as {@link #propertiesToCopy} and each expression is associated to the
     * property at the same index.
     */
    private final Expression<? super Feature, ?>[] expressions;

    /**
     * Whether the {@link #apply(Feature)} method shall create instances of {@link #typeWithDependencies}.
     * If {@code false}, then the instances given to the {@link #apply(Feature)} method will be assumed
     * to be already instances of {@link #typeWithDependencies} and will be modified in-place.
     */
    private final boolean createInstance;

    /**
     * Creates a new projection with the given properties.
     *
     * @param typeRequested         type of features with the properties explicitly requested by the user.
     * @param typeWithDependencies  requested type augmented with dependencies required for the execution of operations.
     * @param propertiesToCopy      names of the properties to be stored in the feature instances created by this object.
     * @param expressions           expressions to apply on the source feature for fetching the property values.
     * @param createInstance        whether the {@link #apply(Feature)} method shall create the feature instances.
     */
    private FeatureProjection(final FeatureType typeRequested,
                              final FeatureType typeWithDependencies,
                              final String[]    propertiesToCopy,
                              final Expression<? super Feature, ?>[] expressions,
                              final boolean createInstance)
    {
        this.typeRequested        = typeRequested;
        this.typeWithDependencies = typeWithDependencies;
        this.propertiesToCopy     = propertiesToCopy;
        this.expressions          = expressions;
        this.createInstance       = createInstance;
    }

    /**
     * Creates a new projection with the given properties specified by a builder.
     * The {@link #apply(Feature)} method will copy the properties of the given
     * features into new instances of {@link #typeWithDependencies}.
     *
     * @param  typeRequested  the type of projected features.
     * @param  projection     descriptions of the properties to keep in the projected features.
     */
    FeatureProjection(final FeatureType typeRequested, final FeatureType typeWithDependencies,
                      final List<FeatureProjectionBuilder.Item> projection)
    {
        this.createInstance       = true;
        this.typeRequested        = typeRequested;
        this.typeWithDependencies = typeWithDependencies;

        // Expressions to apply on the source feature for fetching the property values of the projected feature.
        @SuppressWarnings({"LocalVariableHidesMemberVariable", "unchecked", "rawtypes"})
        final Expression<? super Feature, ?>[] expressions = new Expression[projection.size()];

        // Names of the properties to be stored in the attributes of the target features.
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final String[] propertiesToCopy = new String[expressions.length];

        int storedCount = 0;
        for (final FeatureProjectionBuilder.Item item : projection) {
            final var expression = item.attributeValueGetter();
            if (expression != null) {
                expressions[storedCount] = expression;
                propertiesToCopy[storedCount++] = item.getPreferredName();
            }
        }
        this.propertiesToCopy = ArraysExt.resize(propertiesToCopy, storedCount);
        this.expressions      = ArraysExt.resize(expressions, storedCount);
    }

    /**
     * Returns a variant of this projection where the caller has created the target feature instance itself.
     * The callers may have set some property values itself, and the {@code remaining} argument gives the
     * indexes of the properties that still need to be copied after caller's processing.
     *
     * <h4>Recommendation</h4>
     * Caller should ensure that the {@code remaining} array does not contain indexes <var>i</var>
     * where {@code expressions[i]} is equivalent to {@code FilterFactory.property(propertiesToCopy[i])}
     * because it would be a useless operation. This method does not perform that verification by itself
     * on the assumption that it would duplicate work already done by the caller.
     *
     * @param  remaining  index of the properties that still need to be copied after the caller did its processing.
     * @return a variant of this projection which only completes the projection done by the caller,
     *         or {@code null} if there is nothing left to complete.
     */
    public FeatureProjection forPreexistingFeatureInstances(final int[] remaining) {
        if (remaining.length == 0 && typeRequested == typeWithDependencies) {
            return null;
        }
        @SuppressWarnings({"unchecked", "rawtypes"})
        final Expression<? super Feature, ?>[] filteredExpressions = new Expression[remaining.length];
        final String[] filteredProperties = new String[remaining.length];
        for (int i=0; i<remaining.length; i++) {
            final int index = remaining[i];
            filteredProperties [i] = propertiesToCopy[index];
            filteredExpressions[i] = expressions[index];
        }
        return new FeatureProjection(typeRequested, typeWithDependencies, filteredProperties, filteredExpressions, false);
    }

    /**
     * Creates a new projection with the same properties as the source projection, but modified expressions.
     * The modifications are specified by the given {@code mapper}, which should return values of the same
     * type as the previous expressions. The new expressions shall not introduce new dependencies.
     *
     * <h4>Purpose</h4>
     * This method is used when the caller can replace some expressions by <abbr>SQL</abbr> statements.
     *
     * @param mapper  a function receiving in arguments the property name and the expression fetching the property value,
     *                and returning the expression to use in replacement of the function given in argument.
     * @return the feature projection with modified expressions. May be {@code this} if there is no change.
     */
    public FeatureProjection replaceExpressions(
            final BiFunction<String, Expression<? super Feature, ?>, Expression<? super Feature, ?>> mapper)
    {
        final Map<String, Expression<? super Feature, ?>> filtered = JDK19.newLinkedHashMap(expressions.length);
        for (int i = 0; i < expressions.length; i++) {
            final String property = propertiesToCopy[i];
            if (filtered.put(property, mapper.apply(property, expressions[i])) != null) {
                throw new CorruptedObjectException(Errors.format(Errors.Keys.DuplicatedElement_1, property));
            }
        }
        /*
         * The above loop replaced the expressions used for fetching values from the source feature instances
         * and storing them as attributes. But expressions may also be used in an indirect way, as operations.
         * Note that operations have no corresponding entries in the `propertiesToCopy` array.
         */
        final var builder = new FeatureTypeBuilder(typeWithDependencies);
        for (final PropertyTypeBuilder property : builder.properties().toArray(PropertyTypeBuilder[]::new)) {
            filtered.computeIfAbsent(property.getName().toString(), (name) -> {
                final PropertyType type = property.build();
                Expression<? super Feature, ?> expression = FeatureOperations.expressionOf(type);
                if (!expression.equals(expression = mapper.apply(name, expression))) {
                    property.replaceBy(builder.addProperty(FeatureOperations.replace(type, expression)));
                    return expression;
                }
                return null;    // No change, keep the current operation.
            });
        }
        /*
         * Some expressions may become unnecessary if the new expression only fetches a property value, then stores
         * that value unchanged in the same feature instance. Note that we will need to remove only the expression,
         * not the property in the `FeatureType`.
         */
        if (!createInstance) {
            for (final var it = filtered.entrySet().iterator(); it.hasNext();) {
                final var entry = it.next();
                final var expression = entry.getValue();
                if (expression instanceof ValueReference<?,?>) {
                    final String name = ((ValueReference<?,?>) expression).getXPath();
                    if (name.equals(entry.getKey())) {
                        // The expression reads and stores a property of the same name in the same feature instance.
                        final PropertyTypeBuilder property = builder.getProperty(name);
                        if (property != null) {     // A null value would probably be a bug, but check anyway.
                            property.remove();
                            it.remove();
                        }
                    }
                }
            }
        }
        /*
         * Maybe the new expressions have less dependencies than the old expressions. It happens if `mapper` replaced
         * a pure Java filter by a SQL expression such as `SQRT(x*x + y*y)`, in which case the `x` and `y` properties
         * are used by the database and do not need anymore to be forwarded to the Java code.
         */
        if (typeWithDependencies != typeRequested) {
            final var unnecessary = new HashSet<>(Features.getPropertyNames(typeWithDependencies));
            unnecessary.removeAll(Features.getPropertyNames(typeRequested));
            for (var property : builder.properties()) {
                final PropertyType type = property.build();
                if (type instanceof AbstractOperation) {
                    unnecessary.removeAll(((AbstractOperation) type).getDependencies());
                }
            }
            for (final String name : unnecessary) {
                final PropertyTypeBuilder property = builder.getProperty(name);
                // A `false` value below would be a bug, but check anyway.
                if (property != null && filtered.remove(name) != null) {
                    property.remove();
                }
            }
        }
        /*
         * Remove any remaining operations. The remaining entries shall be only expressions
         * for fetching the values to store in attributes, not values computed on-the-fly.
         */
        filtered.keySet().removeIf((name) -> {
            final PropertyTypeBuilder property = builder.getProperty(name);
            return (property != null) && (property.build() instanceof Operation);
        });
        /*
         * Create the new feature types with the modified expressions.
         * Check if we can reuse the existing feature types.
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        final Expression<? super Feature, ?>[] filteredExpressions = filtered.values().toArray(Expression[]::new);
        String[] filteredProperties = filtered.keySet().toArray(String[]::new);
        if (Arrays.equals(filteredProperties, propertiesToCopy)) {
            filteredProperties = propertiesToCopy;  // Share existing instances (common case).
        }
        FeatureType withDeps = builder.build();
        boolean modified = builder.setName(typeRequested.getName()).properties().removeIf(
                (property) -> !typeRequested.hasProperty(property.getName().toString()));
        FeatureType filteredType = builder.build();
        if (filteredType.equals(typeRequested)) {
            filteredType = typeRequested;
        }
        if (!modified) {
            withDeps = filteredType;
        } else if (withDeps.equals(typeWithDependencies)) {
            withDeps = typeWithDependencies;
        }
        var p = new FeatureProjection(filteredType, withDeps, filteredProperties, filteredExpressions, createInstance);
        if (equals(p)) p = this;
        return p;
    }

    /**
     * Returns {@code true} if this projection contains at least one expression
     * which is not a value reference or a literal.
     *
     * @return whether this projection is presumed to perform some operations.
     */
    public final boolean hasOperations() {
        for (final var expression : expressions) {
            if (!(expression instanceof ValueReference<?,?> || expression instanceof Literal<?,?>)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the names of all stored properties. This list may be shorter than the list of properties of the
     * {@linkplain #typeRequested requested feature type} if some feature properties are computed on-the-fly,
     * or if the target feature contains some new properties that are not in the source.
     *
     * @return the name of all stored properties.
     */
    public final List<String> propertiesToCopy() {
        return Containers.viewAsUnmodifiableList(propertiesToCopy);
    }

    /**
     * Returns the expression which is executed for fetching the property value at the given index.
     *
     * @param  index  index of the stored property for which to get the expression for fetching the value.
     * @return the expression which is executed for fetching the property value at the given index.
     */
    public final Expression<? super Feature, ?> expression(final int index) {
        return expressions[index];
    }

    /**
     * Returns the path to the value (in source features) of the property at the given index.
     * The argument corresponds to an index in the list returned by {@link #propertiesToCopy()}.
     * The return value is often the same name as {@code propertiesToCopy().get(index)} but may
     * differ if the user has specified aliases or if two properties have the same default name.
     *
     * @param  index  index of the stored property for which to get the name in the source feature.
     * @return path in the source features, or empty if the property is not a {@link ValueReference}.
     */
    public Optional<String> xpath(final int index) {
        final Expression<? super Feature, ?> expression = expressions[index];
        if (expression instanceof ValueReference<?,?>) {
            return Optional.of(((ValueReference<?,?>) expression).getXPath());
        }
        return Optional.empty();
    }

    /**
     * Returns all dependencies used, directly or indirectly, by all expressions used in this projection.
     * The set includes transitive dependencies (expressions with operands that are other expressions).
     * The elements are XPaths to properties in the <em>source</em> features.
     *
     * <p>This method does not search for operations in {@link #typeWithDependencies} because the operation
     * dependencies are references to {@link #typeWithDependencies} properties instead of properties of the
     * source features. The property names may differ.</p>
     *
     * @return all dependencies (including transitive dependencies) from source features, as XPaths.
     */
    public Set<String> requiredSourceProperties() {
        Set<String> references = null;
        for (var expression : expressions) {
            references = ListingPropertyVisitor.xpaths(expression, references);
        }
        return (references != null) ? references : Set.of();
    }

    /**
     * Derives a new projected feature instance from the given source.
     * The given source feature should be of type {@link #typeWithDependencies}.
     * The type of the returned feature instance will be {@link #typeRequested}.
     * This method performs the following steps:
     *
     * <ol class="verbose">
     *   <li>If this projection was created by {@link #forPreexistingFeatureInstances(int[])},
     *     then the given feature shall be an instance of {@link #typeWithDependencies} and may be modified.
     *     Otherwise, this method creates a new instance of {@link #typeWithDependencies}.</li>
     *   <li>This method executes all expressions for fetching values from {@code source}
     *     and stores the results in the feature instance of above step.</li>
     *   <li>If {@link #typeWithDependencies} is different than {@link #typeRequested}, then the feature
     *     of above step is wrapped in a view which hides the undesired properties.</li>
     * </ol>
     *
     * @param  source the source feature instance.
     * @return the "projected" (<abbr>SQL</abbr> database sense) feature instance.
     */
    @Override
    public Feature apply(final Feature source) {
        var feature = createInstance ? typeWithDependencies.newInstance() : source;
        for (int i=0; i < expressions.length; i++) {
            feature.setPropertyValue(propertiesToCopy[i], expressions[i].apply(source));
        }
        if (typeRequested != typeWithDependencies) {
            feature = new FeatureView(typeRequested, feature);
        }
        return feature;
    }

    /**
     * Returns a string representation of this projection for debugging purposes.
     * The current implementation formats a table with all properties, including
     * dependencies, and a column saying whether the property is an operation,
     * is stored or whether there is an error.
     *
     * @return a string representation.
     */
    @Override
    public String toString() {
        return Row.toString(this, propertiesToCopy);
    }

    /**
     * Helper class for the implementation of {@link FeatureProjection#toString()}.
     * Each instance represents a row in the table to be formatted..
     * This is used for debugging purposes only.
     */
    @Debug
    private static final class Row {
        /**
         * Returns a string representation of the {@link FeatureProjection} having the given values.
         * Having this method in a separated class reduces the amount of classes loading, since this
         * {@code Row} class is rarely needed in production environment.
         *
         * @param  projection        the projection for which to format a string representation.
         * @param  propertiesToCopy  value of {@link FeatureProjection#propertiesToCopy}.
         * @return the string representation of the given projection.
         */
        static String toString(final FeatureProjection projection, final String[] propertiesToCopy) {
            final var rowByName = new LinkedHashMap<String,Row>();
            if (projection.typeWithDependencies != projection.typeRequested) {
                addAll(rowByName, projection.typeWithDependencies, "dependency");
            }
            addAll(rowByName, projection.typeRequested, "operation");   // Overwrite above dependencies.
            for (int i=0; i < propertiesToCopy.length; i++) {
                String name = propertiesToCopy[i];
                String value;
                try {
                    name = projection.typeWithDependencies.getProperty(name).getName().toString();
                    value = "stored";
                } catch (RuntimeException e) {
                    value = e.toString();
                }
                Row row   = rowByName.computeIfAbsent(name, Row::new);
                row.type  = value;
                row.xpath = projection.xpath(i).orElse("");
            }
            final var words = Vocabulary.forLocale(null);
            final var table = new TableAppender(" │ ");
            table.setMultiLinesCells(true);
            table.appendHorizontalSeparator();
            table.append(words.getString(Vocabulary.Keys.Property)).nextColumn();
            table.append(words.getString(Vocabulary.Keys.Type)).nextColumn();
            table.append("XPath").nextLine();
            table.appendHorizontalSeparator();
            for (final Row row : rowByName.values()) {
                table.append(row.property).nextColumn();
                table.append(row.type).nextColumn();
                table.append(row.xpath).nextLine();
            }
            table.appendHorizontalSeparator();
            return table.toString();
        }

        /**
         * Adds all properties of the given feature type into the specified map.
         * For each property, {@link #type} is overwritten with the {@code type} argument value.
         *
         * @param rowByName    where to add the properties.
         * @param featureType  the type from which to get the list of properties.
         * @param type         the "stored", "operation" or "dependency" value to assign to {@link #type}.
         */
        private static void addAll(final Map<String,Row> rowByName, final FeatureType featureType, final String type) {
            for (final var property : featureType.getProperties(true)) {
                Row row = rowByName.computeIfAbsent(property.getName().toString(), Row::new);
                row.type = type;
            }
        }

        /**
         * Creates a row with no value.
         *
         * @param  name  number under which the property is stored.
         */
        private Row(final String name) {
            property = name;
            xpath = "";
        }

        /**
         * Name of the property in the projected feature type.
         */
        private final String property;

        /**
         * Path to the property value in the source feature.
         */
        private String xpath;

        /**
         * Type: stored, operation or dependency.
         */
        private String type;
    }

    /**
     * Computes a hash code value for the projection.
     */
    @Override
    public int hashCode() {
        return Objects.hash(typeRequested, typeWithDependencies, createInstance)
                + 97 * (Arrays.hashCode(propertiesToCopy) + 97 * Arrays.hashCode(expressions));
    }

    /**
     * Compares this projection with the given object for equality.
     *
     * @param  obj  the object to compare with this projection.
     * @return whether the two objects are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof FeatureProjection) {
            final var other = (FeatureProjection) obj;
            return (createInstance == other.createInstance)
                    && typeRequested.equals(other.typeRequested)
                    && typeWithDependencies.equals(other.typeWithDependencies)
                    && Arrays.equals(propertiesToCopy, other.propertiesToCopy)
                    && Arrays.equals(expressions, other.expressions);
        }
        return false;
    }
}
