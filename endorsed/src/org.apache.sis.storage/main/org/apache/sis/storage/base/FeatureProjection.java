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
package org.apache.sis.storage.base;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ConcurrentModificationException;
import java.util.function.UnaryOperator;
import org.opengis.util.GenericName;
import org.apache.sis.feature.FeatureOperations;
import org.apache.sis.feature.AbstractOperation;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.privy.FeatureExpression;
import org.apache.sis.filter.privy.XPath;
import org.apache.sis.storage.FeatureQuery;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.pending.jdk.JDK19;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.privy.UnmodifiableArrayList;
import org.apache.sis.util.resources.Vocabulary;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.filter.Expression;
import org.apache.sis.pending.geoapi.filter.ValueReference;


/**
 * A function applying projections (with "projected" in the <abbr>SQL</abbr> database sense) of features.
 * Given full feature instances in input, this method returns feature instances containing only a subset
 * of the properties. The property values may also be different if they are computed by the expressions.
 *
 * @author Guilhem Legal (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public class FeatureProjection implements UnaryOperator<AbstractFeature> {
    /**
     * The type of features created by this mapper.
     */
    public final DefaultFeatureType featureType;

    /**
     * Names of the properties to be stored in the target features. This is inferred from the {@code properties}
     * given at construction time, retaining only the elements of type {@link FeatureQuery.ProjectionType#STORED}.
     * Properties that are computed on-the-fly from other properties are not included in this list.
     * This array is arbitrarily set to {@code null} if this projection is an identity operation.
     *
     * <p>When a new {@code Feature} instance needs to be created with a subset of the properties
     * of the original feature instance, usually only the stored properties need to be copied.
     * Other properties computed on-the-fly from stored properties may not need copy.</p>
     *
     * @see #isIdentity()
     */
    private final String[] storedProperties;

    /**
     * Expressions to apply on the source feature for fetching the property values of the projected feature.
     * Shall have the same length as {@link #storedProperties} and the properties shall be in the same order.
     * This array is arbitrarily set to {@code null} if this projection is an identity operation.
     *
     * @see #isIdentity()
     */
    private final Expression<? super AbstractFeature, ?>[] expressions;

    /**
     * Infers the type of values evaluated by a query when executed on features of the given type.
     * If some expressions have no name, default names are computed as below:
     *
     * <ul>
     *   <li>If the expression is an instance of {@link ValueReference}, the name of the
     *       property referenced by the {@linkplain ValueReference#getXPath() XPath}.</li>
     *   <li>Otherwise the localized string "Unnamed #1" with increasing numbers.</li>
     * </ul>
     *
     * <h4>Identity operation</h4>
     * If the result is a feature type with all the properties of the source feature,
     * with the same property names in the same order, and if the expressions are only
     * fetching the values (no computation), then this method returns {@code null} for
     * meaning that this projection does nothing.
     *
     * @param  sourceType  the type of features to be converted to projected features.
     * @param  projection  descriptions of the properties to keep in the projected features, or {@code null} if none.
     * @return a function for projecting the feature instances, or {@code null} if none.
     * @throws IllegalArgumentException if this method can operate only on some feature types
     *         and the given type is not one of them.
     * @throws IllegalArgumentException if this method cannot determine the result type of an expression.
     *         It may be because that expression is backed by an unsupported implementation.
     */
    public static FeatureProjection create(final DefaultFeatureType sourceType, final FeatureQuery.NamedExpression[] projection) {
        if (projection != null) {
            var fp = new FeatureProjection(sourceType, projection);
            if (!fp.isIdentity()) {
                return fp;
            }
        }
        return null;
    }

    /**
     * Creates a new feature projection. If some expressions have no name,
     * default names are computed as described in {@link #create create(…)}.
     *
     * <p>Callers shall invoke {@link #isIdentity()} after construction.
     * This instance shall not be used if it is the identity operation.</p>
     *
     * @param  sourceType  the type of features to be converted to projected features.
     * @param  projection  descriptions of the properties to keep in the projected features, or {@code null} if none.
     * @throws IllegalArgumentException if this method can operate only on some feature types
     *         and the given type is not one of them.
     * @throws IllegalArgumentException if this method cannot determine the result type of an expression.
     *         It may be because that expression is backed by an unsupported implementation.
     */
    protected FeatureProjection(final DefaultFeatureType sourceType, final FeatureQuery.NamedExpression[] projection) {
        int storedCount   = 0;
        int unnamedNumber = 0;          // Sequential number for unnamed expressions.
        Set<String> names = null;       // Names already used, for avoiding collisions.

        // For detecting if the projection would be an identity operation.
        var propertiesOfIdentity = sourceType.getProperties(true).iterator();

        @SuppressWarnings({"LocalVariableHidesMemberVariable", "unchecked", "rawtypes"})
        final Expression<? super AbstractFeature,?>[] expressions = new Expression[projection.length];

        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final String[] storedProperties = new String[projection.length];

        final FeatureTypeBuilder ftb = new FeatureTypeBuilder().setName(sourceType.getName());
        for (int column = 0; column < projection.length; column++) {
            final FeatureQuery.NamedExpression item = projection[column];
            /*
             * For each property, get the expected type (mandatory) and its name (optional).
             * A default name will be computed if no alias was explicitly given by the user.
             */
            final var expression = item.expression;
            final var fex = FeatureExpression.castOrCopy(expression);
            final PropertyTypeBuilder resultType;
            if (fex == null || (resultType = fex.expectedType(sourceType, ftb)) == null) {
                throw new IllegalArgumentException(Resources.format(Resources.Keys.InvalidExpression_2,
                            expression.getFunctionName().toInternationalString(), column));
            }
            GenericName name = item.alias;
            if (name == null) {
                /*
                 * Build a list of aliases declared by the user, for making sure that we do not collide with them.
                 * No check for `GenericName` collision here because it was already verified by `setProjection(…)`.
                 * We may have collision of their `String` representations however, which is okay.
                 */
                if (names == null) {
                    names = JDK19.newHashSet(projection.length);
                    for (final FeatureQuery.NamedExpression p : projection) {
                        if (p.alias != null) {
                            names.add(p.alias.toString());
                        }
                    }
                }
                /*
                 * If the expression is a `ValueReference`, the `PropertyType` instance can be taken directly
                 * from the source feature (the Apache SIS implementation does just that). If the name is set,
                 * then we assume that it is correct. Otherwise we take the tip of the XPath.
                 */
                String tip = reference(expression, true);
                if (tip != null) {
                    /*
                     * Take the existing `GenericName` instance from the property. It should be equivalent to
                     * creating a name from the tip, except that it may be a `ScopedName` instead of `LocalName`.
                     * We do not take `resultType.getName()` because the latter is different if the property
                     * is itself a link to another property (in which case `resultType` is the final target).
                     */
                    name = sourceType.getProperty(tip).getName();
                    if (name == null || !names.add(name.toString())) {
                        name = null;
                        if (tip.isBlank() || names.contains(tip)) {
                            tip = null;
                        }
                    }
                }
                /*
                 * If we still have no name at this point, create a name like "Unnamed #1".
                 * Note that despite the use of `Vocabulary` resources, the name will be unlocalized
                 * (for easier programmatic use) because `GenericName` implementation is designed for
                 * providing localized names only if explicitly requested.
                 */
                if (name == null) {
                    CharSequence text = tip;
                    if (text == null) do {
                        text = Vocabulary.formatInternational(Vocabulary.Keys.Unnamed_1, ++unnamedNumber);
                    } while (!names.add(text.toString()));
                    name = Names.createLocalName(null, null, text);
                }
            }
            /*
             * If the attribute that we just added should be virtual, replace the attribute by an operation.
             * When `FeatureProjection` contains at least one such virtual attributes, the result cannot be
             * an identity operation.
             */
            if (item.type != FeatureQuery.ProjectionType.STORED) {
                propertiesOfIdentity = null;        // No longer an identity operation.
                if (resultType instanceof AttributeTypeBuilder<?>) {
                    final var ab = (AttributeTypeBuilder<?>) resultType;
                    final var storedType = ab.build();
                    if (ftb.properties().remove(resultType)) {
                        final var identification = Map.of(AbstractOperation.NAME_KEY, name);
                        ftb.addProperty(FeatureOperations.expression(identification, expression, storedType));
                    }
                }
                continue;
            }
            /*
             * This is the usual case where the property value is copied in the "projected" feature.
             * If the target property name is equal to the source property name, in the same order,
             * and the expression is just fetching the value from a property of the same name, then
             * we consider that the projection is an identity operation.
             */
            resultType.setName(name);
            storedProperties[storedCount] = name.toString();
            expressions[storedCount++] = expression;
isIdentity: if (propertiesOfIdentity != null) {
                if (propertiesOfIdentity.hasNext()) {
                    final var property = propertiesOfIdentity.next();
                    if (name.equals(property.getName())) {
                        final String tip = name.tip().toString();
                        if (tip.equals(reference(expression, true))) try {
                            if (property.equals(sourceType.getProperty(tip))) {
                                break isIdentity;   // Continue to consider that we may have an identity projection.
                            }
                        } catch (IllegalArgumentException e) {
                            // It may be because the property name is ambiguous.
                            Logging.ignorableException(StoreUtilities.LOGGER, FeatureProjection.class, "create", e);
                        }
                    }
                }
                propertiesOfIdentity = null;
            }
        }
        /*
         * End of the construction of properties from the given expressions. If all properties
         * are just copying values of properties of the same name from the source, returns `null`.
         */
        if (propertiesOfIdentity == null || propertiesOfIdentity.hasNext()) {
            this.featureType      = ftb.build();
            this.storedProperties = ArraysExt.resize(storedProperties, storedCount);
            this.expressions      = ArraysExt.resize(expressions, storedCount);
        } else {
            this.featureType      = sourceType;
            this.storedProperties = null;
            this.expressions      = null;    // Means "identity operation".
        }
    }

    /**
     * Creates a new projection with the given properties.
     *
     * @param type        the type of feature instances created by this projection.
     * @param projection  all properties by name, associated to expressions for getting values from source features.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public FeatureProjection(final DefaultFeatureType type, final Map<String, Expression<? super AbstractFeature, ?>> projection) {
        featureType = type;
        expressions = new Expression[projection.size()];
        storedProperties = new String[expressions.length];
        int i = 0;
        for (var entry : projection.entrySet()) {
            storedProperties[i] = entry.getKey();
            expressions[i++] = entry.getValue();
        }
        if (i != expressions.length) {
            // Should never happen, unless the `Map` has been modified concurrently.
            throw new ConcurrentModificationException();
        }
    }

    /**
     * If the given expression is a value reference, returns the tip of the referenced property.
     * Otherwise, returns {@code null}.
     *
     * @param  expression the expression from which to get the referenced property name.
     * @param  decode  whether to decode the XPath syntax (e.g. {@code {@code "Q{namespace}"}} syntax).
     * @return the referenced property name, or {@code null} if none.
     */
    private static String reference(final Expression<? super AbstractFeature, ?> expression, final boolean decode) {
        if (expression instanceof ValueReference<?,?>) {
            String xpath = ((ValueReference<?,?>) expression).getXPath();
            return decode ? XPath.toPropertyName(xpath) : xpath;
        }
        return null;
    }

    /**
     * Returns whether this projection performs no operation.
     * If this method returns {@code true}, then this instance shall <strong>not</strong> be used.
     *
     * @return whether this projection performs no operation.
     */
    public final boolean isIdentity() {
        return expressions == null;
    }

    /**
     * Returns the names of all stored properties. This list may be shorter than the list of properties
     * of the {@linkplain #featureType feature type} if some feature properties are computed on-the-fly.
     *
     * @return the name of all stored properties.
     */
    public final List<String> getStoredPropertyNames() {
        return UnmodifiableArrayList.wrap(storedProperties);
    }

    /**
     * Returns the name of a property in the source features.
     * The given index corresponds to an index in the list returned by {@link #getStoredPropertyNames()}.
     * The return value is often the same name as {@code getStoredPropertyNames().get(index)}.
     *
     * @param  index   index of the stored property for which to get the name in the source feature.
     * @param  decode  whether to decode the XPath syntax (e.g. {@code {@code "Q{namespace}"}} syntax).
     * @return name in the source features, or {@code null} if the property is not a {@link ValueReference}.
     * @throws NullPointerException if {@link #isIdentity()} is {@code true}.
     */
    public final String getSourcePropertyName(final int index, final boolean decode) {
        return reference(expressions[index], decode);
    }

    /**
     * Returns the expression at the given index.
     *
     * @param  index  index of the stored property for which to get the expression.
     * @return expression at the given index.
     * @throws NullPointerException if {@link #isIdentity()} is {@code true}.
     */
    public final Expression<? super AbstractFeature, ?> getExpression(final int index) {
        return expressions[index];
    }

    /**
     * Derives a new projected feature instance from the given source.
     *
     * @param  source the source feature instance.
     * @return the "projected" (<abbr>SQL</abbr> database sense) feature instance.
     * @throws NullPointerException if {@link #isIdentity()} is {@code true}.
     */
    @Override
    public final AbstractFeature apply(final AbstractFeature source) {
        final var feature = featureType.newInstance();
        for (int i=0; i < expressions.length; i++) {
            feature.setPropertyValue(storedProperties[i], expressions[i].apply(source));
        }
        return feature;
    }

    /**
     * Applies the expressions with the given feature as both the source and the target.
     * IT can be useful when all expressions are computing values derived from other attributes.
     *
     * @param  feature  the feature on which to apply the expressions.
     * @throws NullPointerException if {@link #isIdentity()} is {@code true}.
     */
    public final void applySelf(final AbstractFeature feature) {
        for (int i=0; i < expressions.length; i++) {
            feature.setPropertyValue(storedProperties[i], expressions[i].apply(feature));
        }
    }
}
