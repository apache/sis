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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.opengis.util.GenericName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.feature.Features;
import org.apache.sis.feature.AbstractOperation;
import org.apache.sis.feature.FeatureOperations;
import org.apache.sis.feature.builder.AssociationRoleBuilder;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.filter.Optimization;
import org.apache.sis.util.ArgumentCheckByAssertion;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.AttributeType;
import org.opengis.feature.PropertyType;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.Operation;
import org.opengis.filter.Expression;
import org.opengis.filter.ValueReference;


/**
 * A builder for deriving a feature type containing a subset of the properties of another type.
 * The other type is called the {@linkplain #source() source} feature type. The properties that
 * are retained may have different names than the property names of the source feature type.
 * If a property is a link such as {@code sis:identifier} or {@code sis:geometry},
 * this class keeps trace of the dependencies required for recreating the link.
 *
 * <p>Properties that are copied from the source feature type are declared by calls to
 * {@link #addSourceProperty(PropertyType, boolean)} and related methods defined in this class.
 * The methods inherited from the parent class can also be invoked,
 * but they will receive no special treatment.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see #project()
 */
public final class FeatureProjectionBuilder extends FeatureTypeBuilder {
    /**
     * The type of features that provide the values to store in the projected features.
     * The value of this field does not change, except when following a XPath such as {@code "a/b/c"}.
     *
     * @see #source()
     */
    private FeatureType source;

    /**
     * Whether the source is a dependency of the feature type given to the constructor.
     * This flag become {@code true} when following a XPath of the form {@code "a/b/c"}.
     * In such case, {@link #source} may be temporarily set to the tip {@code "c"} type.
     *
     * @see #using(FeatureType, FeatureExpression)
     */
    private boolean sourceIsDependency;

    /**
     * The properties to inherit from the {@linkplain #source} feature type by explicit user's request.
     * The property types and the property names are not necessarily the same as in the source feature.
     * For example, an operation may be replaced by an attribute which will store the operation result.
     *
     * <h4>Implementation note</h4>
     * This collection cannot be a {@link java.util.Map} with names of the source properties as keys,
     * because the list may contain more than one item with the same {@link Item#sourceName} value.
     * This collision happens if some items are the results of XPath evaluations such as {@code "a/b/c"}.
     * This is the reason why properties can be renamed before to be stored in the projected features.
     */
    private final List<Item> requested;

    /**
     * Names that are actually used, or may be used, in the projected feature type.
     * For each name, the associated value is the item that is explicitly using that name.
     * A {@code null} value means that the name is not used, but is nevertheless reserved
     * because potentially ambiguous. This information is used for avoiding name collisions
     * in automatically generated names.
     *
     * <p>Note that the keys are not necessarily the values of {@link Item#sourceName}.
     * Keys are rather the values of {@code Item.builder.getPreferredName()},
     * except that the latter may not be valid before {@link Item#finish(Optimization)}
     * has been invoked.</p>
     *
     * @see #reserve(GenericName, Item)
     */
    private final Map<GenericName, Item> reservedNames;

    /**
     * Whether at least one item is modified compared to the original property in the source feature type.
     * A modified item may be an item with a name different than the property in {@linkplain #source}.
     * Result of operations such as links may also need to be fetched in advance,
     * because operations cannot be executed anymore after the name of a dependency changed.
     *
     * @see Item#setPreferredName(GenericName)
     */
    private boolean hasModifiedProperties;

    /**
     * Sequential number for generating default names of unnamed properties. This is used when the
     * name inherited from the {@linkplain #source} feature is unknown or collides with another name.
     */
    private int unnamedNumber;

    /**
     * Names of {@linkplain #source} properties that are dependencies (arguments) of feature operations.
     * The most common cases are the targets of {@code "sis:identifier"} and {@code "sis:geometry"} links.
     * Values are the items having this dependency. Many items may share the same dependency.
     *
     * <p>At first, this map is populated without checking if the properties requested by the user contains
     * those dependencies. After all user's requested properties have been declared to this builder, values
     * are filtered for identifying which dependencies need to be added as implicit properties.</p>
     */
    private final Map<String, List<Item>> dependencies;

    /**
     * Whether to store operations as attributes. By default, when {@linkplain #addSourceProperty source
     * properties are added in the projection}, operations are forwarded as given (with their dependencies).
     * But if this flag is set to {@code true}, then operations are replaced by attributes.
     * In such case, it will be caller's responsibility to store the value.
     */
    private boolean operationResultAsAttribute;

    /**
     * Creates a new builder instance using the default factories.
     * This constructor does <em>not</em> inherits the name of the given feature type.
     * Callers need to invoke a {@code setName(…)} method after construction.
     *
     * @todo provides a way to specify the factories used by the data store.
     *
     * @param source  the type from which to take the properties to keep in the projected feature.
     * @param locale  the locale to use for formatting error messages, or {@code null} for the default locale.
     */
    public FeatureProjectionBuilder(final FeatureType source, final Locale locale) {
        super(null, null, locale);
        this.source   = Objects.requireNonNull(source);
        requested     = new ArrayList<>();
        dependencies  = new HashMap<>();
        reservedNames = new HashMap<>();
    }

    /**
     * Returns the type of features that provide the values to store in the projected features.
     * This is the type given at construction time, except when following a XPath such as
     * {@code "a/b/c"} in which case it may temporarily be the leaf {@code "c"} type.
     *
     * @return the current source of properties (never {@code null}).
     */
    public FeatureType source() {
        return source;
    }

    /**
     * Returns the expected type of the given expression using the given feature type as the source.
     * This method temporarily sets the {@linkplain #source() source} to the given {@code childType},
     * then returns the value of {@code expression.expectedType(this)}.
     * This is used for the last expression in a XPath such as {@code "a/b/c"}.
     *
     * @param  childType   the feature type to use.
     * @param  expression  the expression from which to get the expected type.
     * @return handler for the property, or {@code null} if it cannot be resolved.
     *
     * @see FeatureExpression#expectedType(FeatureProjectionBuilder)
     */
    public Item using(final FeatureType childType, final FeatureExpression<?,?> expression) {
        final FeatureType previous = source;
        final boolean status = sourceIsDependency;
        try {
            sourceIsDependency = true;
            source = Objects.requireNonNull(childType);
            return expression.expectedType(this);
        } finally {
            source = previous;
            sourceIsDependency = status;
        }
    }

    /**
     * Adds a property from the source feature type, but replacing operation results by attributes.
     * This method is invoked when an operation uses a source property as a template, usually because
     * the result will be of the same type as one of the operation argument (usually the first argument).
     * In such case, the caller does not want the operation to be executed, since the property will rather
     * be used as a slot for receiving the result.
     *
     * @param  expression  the expression from which to get the expected type.
     * @return handler for the property, or {@code null} if it cannot be resolved.
     */
    public Item addTemplateProperty(final FeatureExpression<?,?> expression) {
        final boolean status = operationResultAsAttribute;
        try {
            operationResultAsAttribute = true;
            return expression.expectedType(this);
        } finally {
            operationResultAsAttribute = status;
        }
    }

    /**
     * Adds the given property, replacing operation by an attribute storing the operation result.
     * This method may return {@code null} if it cannot resolve the property type, in which case
     * the caller should throw an exception (throwing an exception is left to the caller because
     * it can produce a better error message). Operation's dependencies, if any, are added into
     * the given {@code deferred} collection.
     *
     * @param  property  the {@linkplain #source} property to add.
     * @param  deferred  where to add operation's dependencies, or {@code null} for not collecting dependencies.
     * @return builder for the projected property, or {@code null} if it cannot be resolved.
     */
    private PropertyTypeBuilder addPropertyResult(PropertyType property, final Collection<String> deferred) {
        if (property instanceof Operation) {
            final GenericName name = property.getName();
            do {
                if (deferred != null) {
                    if (property instanceof AbstractOperation) {
                        deferred.addAll(((AbstractOperation) property).getDependencies());
                    } else {
                        /*
                         * Cannot resolve dependencies. Current implementation assumes that there is no dependency.
                         * Note: we could conservatively add all properties as dependencies, but this is difficult
                         * to implement efficiently.
                         */
                    }
                }
                final IdentifiedType result = ((Operation) property).getResult();
                if (result != property && result instanceof PropertyType) {
                    property = (PropertyType) result;
                } else if (result instanceof FeatureType) {
                    return addAssociation((FeatureType) result).setName(name);
                } else {
                    return null;
                }
            } while (property instanceof Operation);
            return addProperty(property).setName(name);
        }
        return addProperty(property);
    }

    /**
     * Adds a property from the source feature type. The given property should be the result of a call to
     * {@code source().getProperty(sourceName)}. The call to {@code getProperty(…)} is left to the caller
     * because some callers need to wrap that call in a {@code try} block.
     *
     * @param  property  the property type, usually as one of the properties of {@link #source()}.
     * @param  named     whether the {@code property} name can be used as a default name.
     * @return handler for the given item (never {@code null}).
     */
    public Item addSourceProperty(final PropertyType property, final boolean named) {
        final PropertyTypeBuilder builder;
        final Collection<String> deferred;
        if (sourceIsDependency) {
            /*
             * Adding a property which is not defined in the feature type specified at construction time,
             * but which is defined at the tip of some XPath such as "a/b/c". This is not the same thing
             * as adding an association. This is rather adding a subset of an association. We do not add
             * dependency information because the dependencies are not directly in the source feature.
             */
            reserve(property.getName(), null);
            deferred = new ArrayList<>();
            builder = addPropertyResult(property, deferred);
        } else if (!operationResultAsAttribute && property instanceof AbstractOperation) {
            /*
             * For operations, remember the dependencies in order to determine (after we added all properties)
             * if we can keep the property as an operation or if we will need to copy the value in an attribute.
             * If the operation is not an `AbstractOperation`, unconditionally replace operation by its result.
             */
            deferred = ((AbstractOperation) property).getDependencies();
            builder = addProperty(property);
        } else {
            deferred = new ArrayList<>();
            builder = addPropertyResult(property, deferred);
        }
        final var item = new Item(named ? property.getName() : null, builder);
        declareDependencies(item, deferred);
        requested.add(item);
        return item;
    }

    /**
     * Declares that the {@code deferred} elements are dependencies of the given item.
     * This information will be used by {@link #resolveDependencies(List)}.
     *
     * @param item      the item having dependencies.
     * @param deferred  the item dependencies.
     */
    private void declareDependencies(final Item item, final Collection<String> deferred) {
        for (String dependency : deferred) {
            dependencies.computeIfAbsent(dependency, (key) -> new ArrayList<>(2)).add(item);
        }
    }

    /**
     * Adds a property created by the caller rather than extracted from the source feature.
     * The given builder should have been created by a method of the {@link FeatureTypeBuilder} parent class.
     * The name of the builder is usually not the name of a property in the {@linkplain #source() source} feature.
     *
     * <h4>Assertions</h4>
     * This method verifies that the given builder is a member of the {@linkplain #properties() properties} collection.
     * It also verifies that no {@link Item} have been created for that builder yet.
     * For performance reasons, those verifications are performed only if assertions are enabled.
     *
     * @param  builder  builder for the computed property.
     * @param  named    whether the {@code builder} name can be used as a default name.
     * @return handler for the given item (should never be {@code null}).
     */
    @ArgumentCheckByAssertion
    public Item addComputedProperty(final PropertyTypeBuilder builder, final boolean named) {
        assert properties().contains(builder) : builder;
        assert requested.stream().noneMatch((item) -> item.builder == builder) : builder;
        final var item = new Item(named ? builder.getName() : null, builder);
        requested.add(item);
        return item;
    }

    /**
     * Adds dependencies. This method adds into the {@code deferred} list any transitive
     * dependencies which may need to be added in a second pass after this method call.
     * The elements added into {@code deferred} are {@linkplain #source} properties.
     *
     * @param  deferred  where to add missing transitive dependencies (source properties).
     * @return whether there is no dependency to resolve (i.e., all dependencies are already resolved).
     * @throws UnsupportedOperationException if there is an attempt to rename a property which is used by an operation.
     */
    private boolean resolveDependencies(final List<PropertyType> deferred) {
        final Iterator<Map.Entry<String, List<Item>>> it = dependencies.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<String, List<Item>> entry = it.next();
            final PropertyType property = source.getProperty(entry.getKey());
            final GenericName sourceName = property.getName();
            final Item item = reservedNames.get(sourceName);
            if (item != null) {
                if (!sourceName.equals(item.sourceName)) {
                    throw new UnsupportedOperationException(Resources.forLocale(getLocale())
                            .getString(Resources.Keys.CannotRenameDependency_2, item.sourceName, sourceName));
                }
            } else {
                for (Item dependent : entry.getValue()) {
                    dependent.hasMissingDependency = true;
                }
                deferred.add(property);
            }
            it.remove();
        }
        return deferred.isEmpty();
    }

    /**
     * Declares that the given name is reserved. If this class needs to generate a default name,
     * it will ensure that any automatically generated name do not conflict with reserved names.
     *
     * @param  name   name to reserve for a projected property type, or {@code null} if none.
     * @param  owner  the builder using that name, or {@code null} if none.
     */
    private void reserve(GenericName name, final Item owner) {
        if (name != null) {
            // By `putIfAbsent` method contract, non-null values have precedence over null values.
            reservedNames.putIfAbsent(name, owner);
            if (name != (name = name.tip())) {              // Shortcut for a majority of cases.
                reservedNames.putIfAbsent(name, owner);
            }
        }
    }

    /**
     * Handler for a property inherited from the source feature type. The property is initially unnamed.
     * A name can be specified explicitly after construction by {@link #setPreferredName(GenericName)}.
     * If no name is specified, the default name will be the same as in the source feature type if that
     * name is available, or a default name otherwise.
     */
    public final class Item {
        /**
         * The name that the property had in the {@linkplain #source() source} feature, or {@code null}.
         * The property built by the {@linkplain #builder} will often have the same name, but not always.
         */
        final GenericName sourceName;

        /**
         * The builder for configuring the property.
         */
        private PropertyTypeBuilder builder;

        /**
         * Whether this item got an explicit name. The specified name may be
         * identical to the name in the {@linkplain #source() source} feature.
         */
        private boolean isNamed;

        /**
         * Whether to keep the current name if it is available. This is set to {@code true} when user did not
         * specified explicitly a name, but keeping the name of the source property would be a natural choice.
         * However, before to use that name, we need to wait and see if that name will be explicitly used for
         * another property.
         */
        private boolean preferCurrentName;

        /**
         * Whether this property is an operation having at least one dependency which is not included
         * in the list of properties requested by the user. In such case, we cannot keep the operation
         * and need to replace it by a stored attribute.
         *
         * @see #replaceIfMissingDependency()
         */
        private boolean hasMissingDependency;

        /**
         * Expression for evaluating the attribute value from a source feature instance, or {@code null} if none.
         * This field should be non-null only if the value will be stored in an attribute. If the property is an
         * operation, then this field should be null (this is not the expression of the operation).
         *
         * @see #attributeValueGetter()
         */
        private Expression<? super Feature, ?> attributeValueGetter;

        /**
         * Whether the {@link #attributeValueGetter} should be executed only when the property value is requested.
         * If {@code false} (the default), the value is computed from the source feature exactly once, when the
         * {@link FeatureProjection#apply(Feature)} method is invoked, and the result is stored as an attribute.
         * If {@code true}, then the expression is wrapped in an {@link AbstractOperation} and evaluated when
         * the property value is requested.
         */
        private boolean isDeferredValueGetter;

        /**
         * Creates a new handle for the property created by the given builder.
         *
         * @param  sourceName  the property name in the {@linkplain #source() source} feature, or {@code null}.
         * @param  builder     the builder for configuring the property.
         */
        private Item(final GenericName sourceName, final PropertyTypeBuilder builder) {
            this.sourceName = sourceName;
            this.builder = builder;
        }

        /**
         * Returns a string representation for debugging purposes.
         *
         * @return a string representation for debugging purposes.
         */
        @Override
        public String toString() {
            return Strings.toString(getClass(),
                    "sourceName", (sourceName != null) ? sourceName.toString() : null,
                    "targetName", isNamed ? getPreferredName() : null,
                    "valueClass", getValueClass(),
                    null, hasMissingDependency  ? "hasMissingDependency"  : null,
                    null, isDeferredValueGetter ? "isDeferredValueGetter" : null);
        }

        /**
         * Returns the property type builder wrapped by this item.
         * The following operations are allowed on the returned builder:
         *
         * <ul>
         *   <li>Set the cardinality (minimum and maximum occurrences).</li>
         *   <li>Build the {@code PropertyType}.</li>
         * </ul>
         *
         * The following operations should <em>not</em> be executed on the returned builder.
         * Use the dedicated methods in this class instead:
         *
         * <ul>
         *   <li>Set the name: use {@link #setPreferredName(GenericName)}.</li>
         *   <li>Set the value class: use {@link #replaceValueClass(UnaryOperator)}.</li>
         * </ul>
         *
         * @return the property type builder wrapped by this item.
         */
        public PropertyTypeBuilder builder() {
            hasModifiedProperties = true;       // Conservative because the caller may do anything on the builder.
            return builder;
        }

        /**
         * Replaces this property by a view that will not block the creation of {@code DefaultFeatureType}
         * when a dependency is missing. This method should be invoked only for creating the feature type
         * to be shown to users through {@link FeatureView}.
         */
        private void replaceIfMissingDependency() {
            if (hasMissingDependency) {
                hasMissingDependency = false;
                hasModifiedProperties = true;
                final PropertyTypeBuilder old = builder;
                final PropertyType property = old.build();  // `old.build()` returns the existing operation.
                if (property instanceof AbstractOperation) {
                    builder = addProperty(new OperationView((AbstractOperation) property, getName()));
                    old.replaceBy(builder);
                }
            }
        }

        /**
         * Sets the class of attribute values. If the builder is an instance of {@link AttributeTypeBuilder}
         * and if {@code type.apply(valueClass)} returns a non-null value ({@code valueClass} is the current
         * class of attribute values), then this method sets the new attribute value class to the specified
         * type and returns {@code true}. Otherwise, this method returns {@code false}.
         *
         * <p>Note that when {@code type.apply(valueClass)} is invoked, returning {@code null} is not the same
         * as returning the {@code valueClass} unchanged, even if this {@code Item} is unchanged in both cases.
         * This method returns {@code false} in the former case while it returns {@code true} if the latter case.</p>
         *
         * @param  type  a converter from current class to the new class of attribute values.
         * @return whether the value returned by {@code type} has been accepted (not necessarily causing a change of state).
         * @throws UnconvertibleObjectException if the default value cannot be converted to the given type.
         */
        public boolean replaceValueClass(final UnaryOperator<Class<?>> type) {
            if (builder instanceof AttributeTypeBuilder<?>) {
                final var ab = (AttributeTypeBuilder<?>) builder;
                final Class<?> oldType = ab.getValueClass();
                final Class<?> newType = type.apply(oldType);
                if (newType != null) {
                    if (builder != (builder = ab.setValueClass(newType))) {
                        hasModifiedProperties = true;
                    }
                    return true;
                }
            } else if (builder instanceof AssociationRoleBuilder) {
                // We do not yet have a special case for this one.
            } else {
                final var property = builder.build();
                if (property instanceof Operation) {
                    /*
                     * Less common case where the caller wants to change the type of an operation.
                     * We cannot change the type of an operation (unless we replace the operation
                     * by a stored attribute). Therefore, we only check type compatibility.
                     */
                    final var result = ((Operation) property).getResult();
                    if (result instanceof AttributeType<?>) {
                        final Class<?> oldType = ((AttributeType<?>) result).getValueClass();
                        final Class<?> newType = type.apply(oldType);
                        if (newType != null) {
                            /*
                             * We can be lenient for link operation, but must be strict for other operations.
                             * Example: a link to a geometry, but relaxing the `Polygon` type to `Geometry`.
                             */
                            if (Features.getLinkTarget(property).isPresent() ? newType.isAssignableFrom(oldType) : newType.equals(oldType)) {
                                return true;
                            }
                            throw new UnconvertibleObjectException(Errors.forLocale(getLocale())
                                        .getString(Errors.Keys.CanNotConvertFromType_2, oldType, newType));
                        }
                    }
                }
            }
            return false;
        }

        /**
         * Returns the class of attribute values currently configured in the builder, or {@code null} if none.
         */
        private Class<?> getValueClass() {
            return (builder instanceof AttributeTypeBuilder<?>) ? ((AttributeTypeBuilder<?>) builder).getValueClass() : null;
        }

        /**
         * Sets the expression to use for evaluating the property value.
         * If {@code stored} is {@code true} (the usual case), then the expression will be evaluated early
         * and its result will be stored as an attribute value, unless this property is not an attribute.
         * If {@code stored} is {@code false}, this expression will be wrapped in an operation which will
         * be executed when the property value is requested. Note that in the latter case, this builder
         * may have to retain more dependencies than what the user specified.
         *
         * @param  expression  the expression to be evaluated by the operation.
         * @param  stored      {@code true} for evaluating the expression immediately after feature instances
         *                     are known, or {@code false} for wrapping the expression in a feature operation.
         */
        public void setValueGetter(final Expression<? super Feature, ?> expression, final boolean stored) {
            // Modify only direct value references, not link (or other operations).
            if (builder instanceof AttributeTypeBuilder<?>) {
                attributeValueGetter = expression;
                isDeferredValueGetter = !stored;
            }
        }

        /**
         * Returns the expression for evaluating the value to store in the attribute built by this item.
         * The expression may be {@code null} if the value is computed on-the-fly (i.e. the property is
         * an operation), or if the expression has not been specified.
         */
        final Expression<? super Feature, ?> attributeValueGetter() {
            return attributeValueGetter;
        }

        /**
         * Sets the coordinate reference system that characterizes the values of this attribute.
         *
         * @param  crs  coordinate reference system associated to attribute values, or {@code null}.
         * @return {@code this} for method calls chaining.
         */
        public Item setCRS(final CoordinateReferenceSystem crs) {
            if (builder instanceof AttributeTypeBuilder<?>) {
                builder = ((AttributeTypeBuilder<?>) builder).setCRS(crs);
                hasModifiedProperties = true;
            }
            return this;
        }

        /**
         * Returns whether the property built by this item is equivalent to the given property.
         * The caller should have verified that {@link #hasModifiedProperties} is {@code false}
         * before to invoke this method, because the implementation performs a filtering based
         * on the property name only. This is that way for accepting differences in metadata.
         *
         * @param  property  the property to compare.
         * @return whether this item builds a property equivalent to the given one.
         */
        private boolean equivalent(final PropertyType property) {
            return builder.getName().equals(property.getName());
        }

        /**
         * Returns the name of the projected property.
         * This is initially the name of the property given at construction time,
         * but can be changed later by a call to {@link #setPreferredName(GenericName)}.
         *
         * @return the name of the projected property.
         */
        public String getPreferredName() {
            return builder.getName().toString();
        }

        /**
         * Sets the name of the projected property. A {@code null} argument means that the name is unspecified,
         * in which case a different name may be generated later if the current name collides with other names.
         *
         * <p>This method should be invoked exactly once for each item, even if the argument is {@code null}.
         * The reason is because this method uses this information for recording which names to reserve.</p>
         *
         * @param  targetName  the desired name in the projected feature, or {@code null} if unspecified.
         */
        public void setPreferredName(final GenericName targetName) {
            if (targetName == null) {
                reserve(sourceName, null);      // Will use that name only if not owned by another item.
                preferCurrentName = true;
            } else if (targetName.equals(sourceName)) {
                reserve(sourceName, this);      // Take possession of that name.
                isNamed = true;
            } else {
                builder.setName(targetName);
                reserve(targetName, this);
                hasModifiedProperties = true;   // Because the name is different.
                isNamed = true;
            }
        }

        /**
         * Completes and optimizes this item before construction of the final {@link FeatureProjection}.
         *
         * <h4>Naming</h4>
         * If this item has not received an explicit name, infers a default name.
         * This method should be invoked only after {@link #setPreferredName(GenericName)}
         * has been invoked for all items, for allowing this class to know which names are reserved.
         *
         * <h4>Optimization</h4>
         * This method ensures that the {@link #attributeValueGetter} expression produces a value of the type
         * expected by {@link FeatureProjection#typeWithDependencies}, adding a conversion step if needed.
         * Then this method simplifies the expression, which may remove the conversion added just before.
         */
        final void finish(final Optimization optimizer) {
            if (!isNamed) {
                final Item owner = reservedNames.get(sourceName);
                if (owner != this) {
                    GenericName name = sourceName;
                    if (owner != null || name == null || (!preferCurrentName && reservedNames.containsKey(name))) {
                        do {
                            var text = Vocabulary.formatInternational(Vocabulary.Keys.Unnamed_1, ++unnamedNumber);
                            name = builder.setName(text).getName();     // Local name with the appropriate name space.
                        } while (reservedNames.containsKey(name));      // Reminder: the associated value may be null.
                    }
                    reserve(name, this);
                }
                isNamed = true;
            }
            if (attributeValueGetter != null) {
                final Class<?> valueClass = getValueClass();
                if (valueClass != null) {
                    attributeValueGetter = attributeValueGetter.toValueType(valueClass);
                }
                attributeValueGetter = optimizer.apply(attributeValueGetter);
                if (isDeferredValueGetter) {
                    // Following cast should never fail because it was verified in `setValueGetter(…)`.
                    final var atb = (AttributeTypeBuilder<?>) builder;
                    /*
                     * Optimization: we could compute `storedType = atb.build()` unconditionally,
                     * which creates an attribute with the final name in the target feature type.
                     * However, in the particular case of links, we are better to use the name of
                     * the property in the source feature type, because it allows an optimization
                     * (a replacement by a `LinkOperation`) in `ExpressionOperation.create(…)`.
                     */
                    AttributeType<?> storedType = null;
                    if (attributeValueGetter instanceof ValueReference<?,?>) {
                        var candidate = source.getProperty(((ValueReference<?,?>) attributeValueGetter).getXPath());
                        if (candidate instanceof AttributeType<?>) {
                            storedType = (AttributeType<?>) candidate;
                        }
                    }
                    if (storedType == null) {
                        storedType = atb.build();   // Same name as in the `identification` map below.
                    }
                    final Map<String, ?> identification = Map.of(AbstractOperation.NAME_KEY, atb.getName());
                    final var operation = FeatureOperations.expression(identification, attributeValueGetter, storedType);
                    if (operation instanceof AbstractOperation) {   // Should always be the case, but be safe.
                        declareDependencies(this, ((AbstractOperation) operation).getDependencies());
                        builder = addProperty(operation);
                        atb.replaceBy(builder);
                        hasModifiedProperties = true;
                        attributeValueGetter = null;
                    }
                }
            }
        }
    }

    /**
     * Sets the default name of all anonymous properties, then builds the feature types.
     * Two feature types are built: one with only the requested properties, and another
     * type augmented with dependencies of operations such as links.
     *
     * <p>This method should be invoked exactly once.</p>
     *
     * <h4>Identity operation</h4>
     * If the result is a feature type with all the properties of the source feature,
     * with the same property names in the same order, and if the expressions are only
     * fetching the values (no computation), then this method returns an empty value
     * for meaning that this projection does nothing.
     *
     * @return the feature types with and without dependencies, or empty if there is no projection.
     * @throws UnsupportedOperationException if there is an attempt to rename a property which is used by an operation.
     */
    public Optional<FeatureProjection> project() {
        final var optimizer = new Optimization();
        optimizer.setFinalFeatureType(source);
        requested.forEach((item) -> item.finish(optimizer));
        /*
         * Add properties for all dependencies that are required by operations but are not already present.
         * If there is no need to add anything, `typeWithDependencies` will be directly `typeRequested`.
         */
        final FeatureType typeRequested, typeWithDependencies;
        final List<PropertyTypeBuilder> properties = properties();
        final int count = properties.size();
        final var deferred = new ArrayList<PropertyType>();
        while (!resolveDependencies(deferred)) {
            for (PropertyType property : deferred) {
                final Item item = addSourceProperty(property, true);
                if (item != null) {
                    item.setValueGetter(FeatureOperations.expressionOf(property), true);
                    item.finish(optimizer);
                }
            }
            deferred.clear();
        }
        final List<PropertyTypeBuilder> added = properties.subList(count, properties.size());
        if (added.isEmpty()) {
            typeRequested = typeWithDependencies = build();
        } else {
            final GenericName name = getName();
            typeWithDependencies = setName(
                    Vocabulary.formatInternational(
                            Vocabulary.Keys.PlusDependencies_1,
                            name.tip().toInternationalString()))
                    .build();

            added.clear();     // Keep only the properties requested by user.
            requested.forEach(Item::replaceIfMissingDependency);
            typeRequested = setName(name).build();
        }
        if (typeRequested.equals(source)) {
            return Optional.empty();
        }
        return Optional.of(new FeatureProjection(typeRequested, typeWithDependencies, requested));
    }

    /**
     * Returns the feature type described by this builder. This method may return the
     * {@linkplain #source() source} directly if this projection performs no operation.
     *
     * <p>Users should not invoke this method directly. Use {@link #project()} instead.</p>
     */
    @Override
    public FeatureType build() {
        if (hasModifiedProperties) {
            return super.build();
        }
        final Iterator<Item> it = requested.iterator();
        for (PropertyType property : source.getProperties(true)) {
            if (!(it.hasNext() && it.next().equivalent(property))) {
                return super.build();
            }
        }
        return it.hasNext() ? super.build() : source;
    }
}
