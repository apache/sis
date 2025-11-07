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
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import org.apache.sis.util.Debug;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;
import org.apache.sis.filter.visitor.ListingPropertyVisitor;
import org.apache.sis.io.TableAppender;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
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
public final class FeatureProjection implements UnaryOperator<Feature> {
    /**
     * The type of features with the properties explicitly requested by the user.
     * The property names may differ from the properties of the {@link FeatureProjectionBuilder#source() source}
     * features if aliases were specified by calls to {@link FeatureProjectionBuilder.Item#setName(GenericName)}.
     */
    public final FeatureType typeRequested;

    /**
     * The requested type augmented with dependencies required for the execution of operations such as links.
     * If there is no need for additional properties, then this value is the same as {@link #typeRequested}.
     * The property names are the same as {@link #typeRequested} (i.e., may be aliases).
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
        int storedCount = 0;

        // Expressions to apply on the source feature for fetching the property values of the projected feature.
        @SuppressWarnings({"LocalVariableHidesMemberVariable", "unchecked", "rawtypes"})
        final Expression<? super Feature,?>[] expressions = new Expression[projection.size()];

        // Names of the properties to be stored in the attributes of the target features.
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final String[] propertiesToCopy = new String[expressions.length];

        for (final FeatureProjectionBuilder.Item item : projection) {
            final var expression = item.attributeValueGetter();
            if (expression != null) {
                expressions[storedCount] = expression;
                propertiesToCopy[storedCount++] = item.getName();
            }
        }
        this.propertiesToCopy = ArraysExt.resize(propertiesToCopy, storedCount);
        this.expressions      = ArraysExt.resize(expressions, storedCount);
    }

    /**
     * Creates a new projection with a subset of the properties of another projection.
     * This constructor is invoked when the caller handles itself some of the properties.
     *
     * <h4>Behavioral change</h4>
     * Projections created by this constructor assumes that the feature instances given to the
     * {@link #apply(Feature)} method are already instances of {@link #typeWithDependencies}
     * and can be modified (if needed) in place. This constructor is designed for cases where
     * the caller does itself a part of the {@code FeatureProjection} work.
     *
     * @param  parent     the projection from which to inherit the types and expressions.
     * @param  remaining  index of the properties that still need to be copied after the caller did its processing.
     *
     * @see #afterPreprocessing(int[])
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private FeatureProjection(final FeatureProjection parent, final int[] remaining) {
        createInstance       = false;
        typeRequested        = parent.typeRequested;
        typeWithDependencies = parent.typeWithDependencies;
        expressions          = new Expression[remaining.length];
        propertiesToCopy     = new String[remaining.length];
        for (int i=0; i<remaining.length; i++) {
            final int index = remaining[i];
            propertiesToCopy[i] = parent.propertiesToCopy[index];
            expressions[i] = parent.expressions[index];
        }
    }

    /**
     * Returns a variant of this projection where the caller has created the target feature instance itself.
     * The callers may have set some property values itself, and the {@code remaining} argument gives the
     * indexes of the properties that still need to be copied after caller's processing.
     *
     * @param  remaining  index of the properties that still need to be copied after the caller did its processing.
     * @return a variant of this projection which only completes the projection done by the caller,
     *         or {@code null} if there is nothing left to complete.
     */
    public FeatureProjection afterPreprocessing(final int[] remaining) {
        if (remaining.length == 0 && typeRequested == typeWithDependencies) {
            return null;
        }
        return new FeatureProjection(this, remaining);
    }

    /**
     * Creates a new projection with the same properties as the source projection, but modified expressions.
     *
     * @param source  the projection to copy.
     * @param mapper  a function receiving in arguments the property name and the expression fetching the property value,
     *                and returning the expression to use in replacement of the function given in argument.
     */
    public FeatureProjection(final FeatureProjection source,
            final BiFunction<String, Expression<? super Feature, ?>, Expression<? super Feature, ?>> mapper)
    {
        typeRequested    = source.typeRequested;
        createInstance   = source.createInstance;
        propertiesToCopy = source.propertiesToCopy;
        expressions      = source.expressions.clone();
        for (int i = 0; i < expressions.length; i++) {
            expressions[i] = mapper.apply(propertiesToCopy[i], expressions[i]);
        }
        // TODO: check if we can remove some dependencies.
        typeWithDependencies = source.typeWithDependencies;
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
        return UnmodifiableArrayList.wrap(propertiesToCopy);
    }

    /**
     * Returns the expression which is executed for fetching the property value at the given index.
     *
     * @param  index  index of the stored property for which to get the expression for fething the value.
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
     * The elements are XPaths.
     *
     * @return all dependencies (including transitive dependencies) as XPaths.
     */
    public Set<String> dependencies() {
        Set<String> references = null;
        for (var expression : expressions) {
            references = ListingPropertyVisitor.xpaths(expression, references);
        }
        return (references != null) ? references : Set.of();
    }

    /**
     * Derives a new projected feature instance from the given source.
     * The feature type of the returned feature instance will be be {@link #typeRequested}.
     * This method performs the following steps:
     *
     * <ol class="verbose">
     *   <li>If this projection was created by {@link #afterPreprocessing(int[])}, then the given feature
     *     shall be an instances of {@link #typeWithDependencies} and may be modified in-place. Otherwise,
     *     this method creates a new instance of {@link #typeWithDependencies}.</li>
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
