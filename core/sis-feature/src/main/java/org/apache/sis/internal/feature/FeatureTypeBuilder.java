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
package org.apache.sis.internal.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.feature.AbstractOperation;
import org.apache.sis.feature.DefaultAssociationRole;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.feature.FeatureOperations;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.util.CorruptedObjectException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Classes;

// Branch-dependent imports
import java.util.Objects;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.feature.FeatureAssociationRole;


/**
 * Helper class for the creation of {@link FeatureType} instances.
 * This builder can create the arguments to be given to the
 * {@linkplain DefaultFeatureType#DefaultFeatureType feature type constructor}
 * from simpler parameters given to this builder.
 *
 * <p>{@code FeatureTypeBuilder} should be short lived.
 * After the {@code FeatureType} has been created, the builder should be discarded.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.8
 * @module
 *
 * @see org.apache.sis.parameter.ParameterBuilder
 */
public class FeatureTypeBuilder extends Builder<FeatureTypeBuilder> {
    /**
     * The factory to use for creating names.
     */
    private final NameFactory nameFactory;

    /**
     * Builders for the properties (attributes, associations or operations) of this feature.
     */
    private final List<Property<?>> properties;

    /**
     * The parent of the feature to create. By default, new features have no parent.
     */
    private final List<FeatureType> superTypes;

    /**
     * Whether the feature type is abstract. The default value is {@code false}.
     */
    private boolean isAbstract;

    /**
     * The default scope to use when {@link #name(String, String)} is invoked with a null scope.
     *
     * @see #setDefaultScope(String)
     */
    private String defaultScope;

    /**
     * The default minimum number of property values.
     *
     * @see #setDefaultCardinality(int, int)
     */
    private int defaultMinimumOccurs;

    /**
     * The default maximum number of property values.
     *
     * @see #setDefaultCardinality(int, int)
     */
    private int defaultMaximumOccurs;

    /**
     * An optional prefix or suffix to insert before or after the {@linkplain FeatureOperations#compound compound key}
     * named {@code "@identifier"}.
     */
    private String idPrefix, idSuffix;

    /**
     * The separator to insert between each single component in a {@linkplain FeatureOperations#compound compound key}
     * named {@code "@identifier"}. This is ignored if {@link #identifierCount} is zero.
     */
    private String idDelimiter;

    /**
     * Number of attribute that have been flagged as an identifier component.
     *
     * @see Attribute.Role#IDENTIFIER_COMPONENT
     * @see AttributeConvention#IDENTIFIER_PROPERTY
     */
    private int identifierCount;

    /**
     * The default geometry attribute, or {@code null} if none.
     *
     * @see Attribute.Role#DEFAULT_GEOMETRY
     * @see AttributeConvention#GEOMETRY_PROPERTY
     */
    private Attribute<?> defaultGeometry;

    /**
     * The object created by this builder, or {@code null} if not yet created.
     * This field must be cleared every time that a setter method is invoked on this builder.
     */
    private transient FeatureType feature;

    /**
     * Creates a new builder instance using the default name factory.
     */
    public FeatureTypeBuilder() {
        this(null, null, null);
    }

    /**
     * Creates a new builder instance using the given feature type as a template.
     *
     * @param template  an existing feature type to use as a template, or {@code null} if none.
     */
    public FeatureTypeBuilder(final FeatureType template) {
        this(template, null, null);
    }

    /**
     * Creates a new builder instance using the given name factory, template and locale for formatting error messages.
     *
     * @param template  an existing feature type to use as a template, or {@code null} if none.
     * @param factory   the factory to use for creating names, or {@code null} for the default factory.
     * @param locale    the locale to use for formatting error messages, or {@code null} for the default locale.
     */
    public FeatureTypeBuilder(final FeatureType template, NameFactory factory, final Locale locale) {
        super(template, locale);
        if (factory == null) {
            factory = DefaultFactories.forBuildin(NameFactory.class);
        }
        nameFactory = factory;
        properties  = new ArrayList<>();
        superTypes  = new ArrayList<>();
        idDelimiter = ":";
        defaultMinimumOccurs = 1;
        defaultMaximumOccurs = 1;
        if (template != null) {
            feature    = template;
            isAbstract = template.isAbstract();
            superTypes.addAll(template.getSuperTypes());
            for (final PropertyType p : template.getProperties(false)) {
                final Property<?> builder;
                if (p instanceof AttributeType<?>) {
                    builder = new Attribute<>(this, (AttributeType<?>) p);
                } else if (p instanceof FeatureAssociationRole) {
                    builder = new Association(this, (FeatureAssociationRole) p);
                } else {
                    continue;           // Skip unknown types.
                }
                properties.add(builder);
            }
        }
    }

    /**
     * If a {@code FeatureType} has been created by the last call to {@link #build()} has been cached,
     * clears that cache. This method must be invoked every time that a setter method is invoked.
     */
    @Override
    final void clearCache() {
        feature = null;
    }

    /**
     * Sets whether the feature type is abstract.
     * If this method is not invoked, then the default value is {@code false}.
     *
     * @param  isAbstract whether the feature type is abstract.
     * @return {@code this} for allowing method calls chaining.
     */
    public FeatureTypeBuilder setAbstract(final boolean isAbstract) {
        if (this.isAbstract != isAbstract) {
            this.isAbstract  = isAbstract;
            clearCache();
        }
        return this;
    }

    /**
     * Sets the parent types (or super-type) from which to inherit properties.
     * If this method is not invoked, then the default value is to have no parent.
     *
     * @param  parents  the parent types from which to inherit properties, or an empty array if none.
     * @return {@code this} for allowing method calls chaining.
     */
    public FeatureTypeBuilder setSuperTypes(final FeatureType... parents) {
        ensureNonNull("parents", parents);
        final List<FeatureType> asList = Arrays.asList(parents);
        if (!superTypes.equals(asList)) {
            superTypes.clear();
            superTypes.addAll(asList);
            clearCache();
        }
        return this;
    }

    /**
     * Sets the scope to use by default when {@link #setName(String)} is invoked.
     *
     * @param  scope  the new default scope, or {@code null} if none.
     * @return {@code this} for allowing method calls chaining.
     */
    public FeatureTypeBuilder setDefaultScope(final String scope) {
        defaultScope = scope;
        // No need to clear the cache because this change affects
        // only the next names to be created, not the existing ones.
        return this;
    }

    /**
     * Sets the default minimum and maximum number of property values.
     * Those defaults will applied to newly created attributes or associations,
     * for example in next calls to {@link #addAttribute(Class)}.
     *
     * <p>If this method is not invoked, then the default cardinality is [1 … 1].</p>
     *
     * @param  minimumOccurs  new default minimum number of property values.
     * @param  maximumOccurs  new default maximum number of property values.
     * @return {@code this} for allowing method calls chaining.
     *
     * @see Attribute#setCardinality(int, int)
     */
    public FeatureTypeBuilder setDefaultCardinality(final int minimumOccurs, final int maximumOccurs) {
        if (minimumOccurs < 0 || maximumOccurs < minimumOccurs) {
            throw new IllegalArgumentException(errors().getString(Errors.Keys.IllegalRange_2, minimumOccurs, maximumOccurs));
        }
        defaultMinimumOccurs = minimumOccurs;
        defaultMaximumOccurs = maximumOccurs;
        // No need to clear the cache because this change affects only
        // the next properties to be created, not the existing ones.
        return this;
    }

    /**
     * Sets the prefix, suffix and delimiter to use when formatting a compound identifier made of two or more attributes.
     * The delimiter will be used only if at least two attributes have the {@linkplain Attribute.Role#IDENTIFIER_COMPONENT
     * identifier component role}.
     *
     * <p>If this method is not invoked, then the default values are the {@code ":"} delimiter and no prefix or suffix.</p>
     *
     * @param  delimiter  the characters to use as delimiter between each single property value.
     * @param  prefix     characters to use at the beginning of the concatenated string, or {@code null} if none.
     * @param  suffix     characters to use at the end of the concatenated string, or {@code null} if none.
     * @return {@code this} for allowing method calls chaining.
     *
     * @see Attribute.Role#IDENTIFIER_COMPONENT
     * @see FeatureOperations#compound(Map, String, String, String, PropertyType...)
     */
    public FeatureTypeBuilder setIdentifierDelimiters(final String delimiter, final String prefix, final String suffix) {
        ensureNonEmpty("delimiter", delimiter);
        if (!delimiter.equals(idDelimiter) || !Objects.equals(prefix, idPrefix) || !Objects.equals(suffix, idSuffix)) {
            idDelimiter = delimiter;
            idPrefix    = prefix;
            idSuffix    = suffix;
            clearCache();
        }
        return this;
    }

    /**
     * Creates a new {@code AttributeType} builder for values of the given class.
     * The default attribute name is the name of the given type, but callers should invoke one
     * of the {@code Attribute.setName(…)} methods on the returned instance with a better name.
     *
     * <p>Usage example:</p>
     * {@preformat java
     *     builder.addAttribute(String.class).setName("City").setDefaultValue("Metropolis");
     * }
     *
     * The value class can not be {@code Feature.class} since features shall be handled
     * as {@linkplain #addAssociation(FeatureType) associations} instead than attributes.
     *
     * @param  <V>  the compile-time value of {@code valueClass} argument.
     * @param  valueClass  the class of attribute values (can not be {@code Feature.class}).
     * @return a builder for an {@code AttributeType}.
     */
    public <V> Attribute<V> addAttribute(final Class<V> valueClass) {
        ensureNonNull("valueClass", valueClass);
        if (Feature.class.isAssignableFrom(valueClass)) {
            // We disallow Feature.class because that type shall be handled as association instead than attribute.
            throw new IllegalArgumentException(errors().getString(Errors.Keys.IllegalArgumentValue_2, "valueClass", valueClass));
        }
        final Attribute<V> property = new Attribute<>(this, valueClass);
        properties.add(property);
        clearCache();
        return property;
    }

    /**
     * Creates a new {@code AttributeType} builder initialized to the same characteristics than the given template.
     *
     * @param  <V>       the compile-time type of values in the {@code template} argument.
     * @param  template  an existing attribute type to use as a template.
     * @return a builder for an {@code AttributeType}, initialized with the values of the given template.
     */
    public <V> Attribute<V> addAttribute(final AttributeType<V> template) {
        ensureNonNull("template", template);
        final Attribute<V> property = new Attribute<>(this, template);
        properties.add(property);
        clearCache();
        return property;
    }

    /**
     * Creates a new {@code FeatureAssociationRole} builder for features of the given type.
     * The default association name is the name of the given type, but callers should invoke one
     * of the {@code Association.setName(…)} methods on the returned instance with a better name.
     *
     * @param  type  the type of feature values.
     * @return a builder for a {@code FeatureAssociationRole}.
     */
    public Association addAssociation(final FeatureType type) {
        ensureNonNull("type", type);
        final Association property = new Association(this, type, type.getName());
        properties.add(property);
        clearCache();
        return property;
    }

    /**
     * Creates a new {@code FeatureAssociationRole} builder for features of a type of the given name.
     * This method can be invoked as an alternative to {@link #addAssociation(FeatureType)} when the
     * {@code FeatureType} instance is not yet available because of cyclic dependency.
     *
     * @param  type  the name of the type of feature values.
     * @return a builder for a {@code FeatureAssociationRole}.
     */
    public Association addAssociation(final GenericName type) {
        ensureNonNull("type", type);
        final Association property = new Association(this, null, type);
        properties.add(property);
        clearCache();
        return property;
    }

    /**
     * Creates a new {@code FeatureAssociationRole} builder initialized to the same characteristics
     * than the given template.
     *
     * @param  template  an existing feature association to use as a template.
     * @return a builder for an {@code FeatureAssociationRole}, initialized with the values of the given template.
     */
    public Association addAssociation(final FeatureAssociationRole template) {
        ensureNonNull("template", template);
        final Association property = new Association(this, template);
        properties.add(property);
        clearCache();
        return property;
    }




    /**
     * Describes one property of the {@code FeatureType} to be built by the enclosing {@code FeatureTypeBuilder}.
     * A different instance of {@code Property} exists for each property to describe. Those instances are created by:
     *
     * <ul>
     *   <li>{@link FeatureTypeBuilder#addAttribute(Class)}</li>
     *   <li>{@link FeatureTypeBuilder#addAssociation(FeatureType)}</li>
     *   <li>{@link FeatureTypeBuilder#addAssociation(GenericName)}</li>
     * </ul>
     *
     * @param <B> the property subclass. It is subclass responsibility to ensure that {@code this}
     *            is assignable to {@code <B>}; this {@code Property} class can not verify that.
     */
    static abstract class Property<B extends Property<B>> extends Builder<B> {
        /**
         * The feature type builder instance that created this {@code Property} builder.
         *
         * <div class="note">We could replace this reference by a non-static {@code Property} class.
         * But we do not for consistency with {@link Characteristic} and for allowing the inner
         * {@code Attribute.Role} enumeration.</div>
         */
        final FeatureTypeBuilder owner;

        /**
         * The minimum number of property values.
         * The default value is 1, unless otherwise specified by {@link #setDefaultCardinality(int, int)}.
         *
         * @see #setCardinality(int, int)
         */
        int minimumOccurs;

        /**
         * The maximum number of property values.
         * The default value is 1, unless otherwise specified by {@link #setDefaultCardinality(int, int)}.
         *
         * @see #setCardinality(int, int)
         */
        int maximumOccurs;

        /**
         * The attribute or association created by this builder, or {@code null} if not yet created.
         * This field must be cleared every time that a setter method is invoked on this builder.
         */
        private transient PropertyType property;

        /**
         * Creates a new {@code PropertyType} builder initialized to the values of an existing property.
         *
         * @param owner     the builder of the {@code FeatureType} for which to add this property.
         * @param template  an existing property to use as a template, or {@code null} if none.
         */
        Property(final FeatureTypeBuilder owner, final PropertyType template) {
            super(template, owner.getLocale());
            this.owner    = owner;
            minimumOccurs = owner.defaultMinimumOccurs;
            maximumOccurs = owner.defaultMaximumOccurs;
            property      = template;
        }

        /**
         * Sets the minimum and maximum number of property values. Those numbers must be equal or greater than zero.
         *
         * <p>If this method is not invoked, then the default values are the cardinality specified by the last call
         * to {@link FeatureTypeBuilder#setDefaultCardinality(int, int)} at the time this instance has been created.
         * If the later method has not been invoked, then the default cardinality is [1 … 1].</p>
         *
         * @param  minimumOccurs  new minimum number of property values.
         * @param  maximumOccurs  new maximum number of property values.
         * @return {@code this} for allowing method calls chaining.
         */
        @SuppressWarnings("unchecked")
        public B setCardinality(final int minimumOccurs, final int maximumOccurs) {
            if (this.minimumOccurs != minimumOccurs || this.maximumOccurs != maximumOccurs) {
                if (minimumOccurs < 0 || maximumOccurs < minimumOccurs) {
                    throw new IllegalArgumentException(errors().getString(Errors.Keys.IllegalRange_2, minimumOccurs, maximumOccurs));
                }
                this.minimumOccurs = minimumOccurs;
                this.maximumOccurs = maximumOccurs;
                clearCache();
            }
            return (B) this;
        }

        /**
         * Returns {@code true} if {@link Attribute.Role#IDENTIFIER_COMPONENT} has been associated to this property.
         */
        boolean isIdentifier() {
            return false;
        }

        /**
         * Delegates the creation of a new name to the enclosing builder.
         */
        @Override
        final GenericName name(final String scope, final String localPart) {
            return owner.name(scope, localPart);
        }

        /**
         * If a {@code PropertyType} has been created by the last call to {@link #build()} has been cached,
         * clears that cache. This method must be invoked every time that a setter method is invoked.
         */
        @Override
        final void clearCache() {
            property = null;
            owner.clearCache();
        }

        /**
         * Returns the property type from the current setting.
         * This method may return an existing property if it was already created.
         */
        final PropertyType build() {
            if (property == null) {
                property = create();
            }
            return property;
        }

        /**
         * Creates a new property type from the current setting.
         */
        abstract PropertyType create();
    }




    /**
     * Describes one association from the {@code FeatureType} to be built by the enclosing {@code FeatureTypeBuilder}
     * to another {@code FeatureType}. A different instance of {@code Association} exists for each feature association
     * to describe. Those instances are created preferably by {@link FeatureTypeBuilder#addAssociation(FeatureType)},
     * or in case of cyclic reference by {@link FeatureTypeBuilder#addAssociation(GenericName)}.
     *
     * @see FeatureTypeBuilder#addAssociation(FeatureType)
     * @see FeatureTypeBuilder#addAssociation(GenericName)
     */
    public static final class Association extends Property<Association> {
        /**
         * The target feature type, or {@code null} if unknown.
         */
        private final FeatureType type;

        /**
         * Name of the target feature type (never null).
         */
        private final GenericName typeName;

        /**
         * Creates a new {@code AssociationRole} builder for values of the given type.
         * The {@code type} argument can be null if unknown, but {@code typeName} is mandatory.
         *
         * @param owner  the builder of the {@code FeatureType} for which to add this property.
         */
        Association(final FeatureTypeBuilder owner, final FeatureType type, final GenericName typeName) {
            super(owner, null);
            this.type     = type;
            this.typeName = typeName;
        }

        /**
         * Creates a new {@code FeatureAssociationRole} builder initialized to the values of an existing association.
         *
         * @param owner  the builder of the {@code FeatureType} for which to add this property.
         */
        Association(final FeatureTypeBuilder owner, final FeatureAssociationRole template) {
            super(owner, template);
            minimumOccurs = template.getMinimumOccurs();
            maximumOccurs = template.getMaximumOccurs();
            type          = template.getValueType();
            typeName      = type.getName();
        }

        /**
         * Returns a default name to use if the user did not specified a name. The first letter will be changed to
         * lower case (unless the name looks like an acronym) for compliance with Java convention on property names.
         */
        @Override
        final String getDefaultName() {
            return typeName.tip().toString();
        }

        /**
         * Appends a text inside the value returned by {@link #toString()}, before the closing bracket.
         */
        @Override
        final void toStringInternal(final StringBuilder buffer) {
            buffer.append(" → ").append(typeName);
        }

        /**
         * Creates a new property type from the current setting.
         */
        @Override
        final PropertyType create() {
            if (type != null) {
                return new DefaultAssociationRole(identification(), type, minimumOccurs, maximumOccurs);
            } else {
                return new DefaultAssociationRole(identification(), typeName, minimumOccurs, maximumOccurs);
            }
        }
    }




    /**
     * Describes one attribute of the {@code FeatureType} to be built by the enclosing {@code FeatureTypeBuilder}.
     * A different instance of {@code Attribute} exists for each feature attribute to describe.
     * Those instances are created by {@link FeatureTypeBuilder#addAttribute(Class)}.
     *
     * @param <V> the class of property values.
     *
     * @see FeatureTypeBuilder#addAttribute(Class)
     */
    public static final class Attribute<V> extends Property<Attribute<V>> {
        /**
         * The class of property values. Can not be changed after construction
         * because this value determines the parameterized type {@code <V>}.
         */
        private final Class<V> valueClass;

        /**
         * The default value for the property, or {@code null} if none.
         */
        private V defaultValue;

        /**
         * Whether this attribute will be used in a {@linkplain FeatureOperations#compound compound key} named
         * {@code "@identifier"}. If only one attribute has this flag and {@link FeatureTypeBuilder#idPrefix} and
         * {@code isSuffix} are null, then {@code "@identifier"} will be a {@linkplain FeatureOperations#link link}
         * to {@code idAttributes[0]}.
         *
         * @see #addRole(Role)
         */
        private boolean isIdentifier;

        /**
         * Builders for the characteristics associated to the attribute.
         */
        private final List<Characteristic<?>> characteristics = new ArrayList<>();

        /**
         * Creates a new {@code AttributeType} builder for values of the given class.
         *
         * @param owner      the builder of the {@code FeatureType} for which to add this property.
         * @param valueClass the class of property values.
         */
        Attribute(final FeatureTypeBuilder owner, final Class<V> valueClass) {
            super(owner, null);
            this.valueClass = valueClass;
        }

        /**
         * Creates a new {@code AttributeType} builder initialized to the values of an existing attribute.
         *
         * @param owner  the builder of the {@code FeatureType} for which to add this property.
         */
        Attribute(final FeatureTypeBuilder owner, final AttributeType<V> template) {
            super(owner, template);
            minimumOccurs = template.getMinimumOccurs();
            maximumOccurs = template.getMaximumOccurs();
            valueClass    = template.getValueClass();
            defaultValue  = template.getDefaultValue();
            for (final AttributeType<?> c : template.characteristics().values()) {
                characteristics.add(new Characteristic<>(this, c));
            }
        }

        /**
         * Returns a default name to use if the user did not specified a name. The first letter will be changed to
         * lower case (unless the name looks like an acronym) for compliance with Java convention on property names.
         */
        @Override
        final String getDefaultName() {
            return Classes.getShortName(valueClass);
        }

        /**
         * Sets the default value for the property.
         *
         * @param  value  default property value, or {@code null} if none.
         * @return {@code this} for allowing method calls chaining.
         */
        public Attribute<V> setDefaultValue(final V value) {
            if (!Objects.equals(defaultValue, value)) {
                defaultValue = value;
                clearCache();
            }
            return this;
        }

        /**
         * Sets an enumeration of valid values for this attribute.
         *
         * <p>This is a convenience method for {@link #addCharacteristic(Class)} with a value
         * of type {@link Set} and a conventional name.</p>
         *
         * @param  values valid values.
         * @return {@code this} for allowing method calls chaining.
         * @throws UnsupportedOperationException if this property does not support characteristics.
         *
         * @see AttributeConvention#VALID_VALUES_CHARACTERISTIC
         */
        @SafeVarargs
        public final Attribute<V> setValidValues(final V... values) {
            return setCharacteristic(AttributeConvention.VALID_VALUES_CHARACTERISTIC,
                    Set.class, CollectionsExt.immutableSet(false, values));
        }

        /**
         * Sets the maximal length that characterizes the {@link CharSequence} values of this attribute.
         * While this characteristic can be applied to any kind of attribute, it is meaningful only with
         * character sequences.
         *
         * <p>This is a convenience method for {@link #addCharacteristic(Class)} with a value
         * of type {@link Integer} and a conventional name.</p>
         *
         * @param  length  maximal length of {@link CharSequence} attribute values, or {@code null}.
         * @return {@code this} for allowing method calls chaining.
         * @throws UnsupportedOperationException if this property does not support length characteristics.
         *
         * @see AttributeConvention#MAXIMAL_LENGTH_CHARACTERISTIC
         */
        public Attribute<V> setMaximalLengthCharacteristic(final Integer length) {
            return setCharacteristic(AttributeConvention.MAXIMAL_LENGTH_CHARACTERISTIC, Integer.class, length);
        }

        /**
         * Sets the coordinate reference system that characterizes the values of this attribute.
         * While this characteristic can be applied to any kind of attribute, it is meaningful
         * only with georeferenced values like geometries or coverages.
         *
         * <p>This is a convenience method for {@link #addCharacteristic(Class)} with a value
         * of type {@link CoordinateReferenceSystem} and a conventional name.</p>
         *
         * @param  crs  coordinate reference system associated to attribute values, or {@code null}.
         * @return {@code this} for allowing method calls chaining.
         * @throws UnsupportedOperationException if this property does not support CRS characteristics.
         *
         * @see AttributeConvention#CRS_CHARACTERISTIC
         */
        public Attribute<V> setCRSCharacteristic(final CoordinateReferenceSystem crs) {
            return setCharacteristic(AttributeConvention.CRS_CHARACTERISTIC, CoordinateReferenceSystem.class, crs);
        }

        /**
         * Implementation of all setter methods for characteristics.
         *
         * @throws UnsupportedOperationException if this property does not support characteristics.
         */
        private <C> Attribute<V> setCharacteristic(final GenericName name, final Class<C> type, final C value) {
            for (final Characteristic<?> characteristic : characteristics) {
                if (name.equals(characteristic.getName())) {
                    characteristic.set(value);
                    return this;
                }
            }
            addCharacteristic(type).setDefaultValue(value).setName(name);
            return this;
        }

        /**
         * Adds another attribute type that describes this attribute type.
         * See <cite>"Attribute characterization"</cite> in {@link DefaultAttributeType} Javadoc for more information.
         *
         * <p>Usage example:</p>
         * {@preformat java
         *     attribute.addCharacteristic(Unit.class).setName("Unit of measurement").setDefaultValue(SI.CELSIUS);
         * }
         *
         * The default characteristic name is the name of the given type, but callers should invoke one
         * of the {@code Characteristic.setName(…)} methods on the returned instance with a better name.
         *
         * @param  <C>   the compile-time type of {@code type} argument.
         * @param  type  the class of characteristic values.
         * @return a builder for a characteristic of this attribute.
         */
        public <C> Characteristic<C> addCharacteristic(final Class<C> type) {
            if (valueClass == Feature.class) {
                throw new UnsupportedOperationException(errors().getString(Errors.Keys.IllegalOperationForValueClass_1, valueClass));
            }
            ensureNonNull("type", type);
            final Characteristic<C> characteristic = new Characteristic<>(this, type);
            characteristics.add(characteristic);
            clearCache();
            return characteristic;
        }

        /**
         * Adds another attribute type that describes this attribute type, using an existing one as a template.
         * See <cite>"Attribute characterization"</cite> in {@link DefaultAttributeType} Javadoc for more information.
         *
         * @param  <C>       the compile-time type of values in the {@code template} argument.
         * @param  template  an existing attribute type to use as a template.
         * @return a builder for a characteristic of this attribute, initialized with the values of the given template.
         */
        public <C> Characteristic<C> addCharacteristic(final AttributeType<C> template) {
            ensureNonNull("template", template);
            final Characteristic<C> characteristic = new Characteristic<>(this, template);
            characteristics.add(characteristic);
            clearCache();
            return characteristic;
        }

        /**
         * Roles that can be associated to some attributes for instructing {@code FeatureTypeBuilder}
         * how to generate pre-defined operations. Those pre-defined operations are:
         *
         * <ul>
         *   <li>A {@linkplain FeatureOperations#compound compound operation} for generating a unique identifier
         *       from an arbitrary amount of attribute values.</li>
         *   <li>A {@linkplain FeatureOperations#link link operation} for referencing a geometry to be used as the
         *       <em>default</em> geometry.</li>
         *   <li>An {@linkplain FeatureOperations#envelope operation} for computing the bounding box of all geometries
         *       found in the feature. This operation is automatically added if the feature contains a default geometry.</li>
         * </ul>
         *
         * This enumeration allows user code to specify which feature attribute to use for creating those operations.
         *
         * @see Attribute#addRole(Role)
         */
        public static enum Role {
            /**
             * Attribute value will be part of a unique identifier for the feature instance.
             * An arbitrary amount of attributes can be flagged as identifier components:
             *
             * <ul>
             *   <li>If no attribute has this role, then no attribute is marked as feature identifier.</li>
             *   <li>If exactly one attribute has this role, then a synthetic attribute named {@code "@identifier"}
             *       will be created as a {@linkplain FeatureOperations#link link} to the flagged attribute.</li>
             *   <li>If more than one attribute have this role, then a synthetic attribute named {@code "@identifier"}
             *       will be created as a {@linkplain FeatureOperations#compound compound key} made of all flagged
             *       attributes. The separator character can be modified by a call to
             *       {@link FeatureTypeBuilder#setIdentifierDelimiters(String, String, String)}</li>
             * </ul>
             *
             * @see FeatureTypeBuilder#setIdentifierDelimiters(String, String, String)
             */
            IDENTIFIER_COMPONENT,

            /**
             * Attribute value will be flagged as the <em>default</em> geometry.
             * Feature can have an arbitrary amount of geometry attributes,
             * but only one can be flagged as the default geometry.
             */
            DEFAULT_GEOMETRY
        }

        /**
         * Flags this attribute as an input of one of the pre-defined operations managed by {@code FeatureTypeBuilder}.
         *
         * @param role the role to add to this attribute (shall not be null).
         */
        public void addRole(final Role role) {
            ensureNonNull("role", role);
            switch (role) {
                case IDENTIFIER_COMPONENT: {
                    if (!isIdentifier) {
                        isIdentifier = true;
                        owner.identifierCount++;
                        owner.clearCache();         // The change does not impact this attribute itself.
                    }
                    break;
                }
                case DEFAULT_GEOMETRY: {
                    if (owner.defaultGeometry != this) {
                        if (!Geometries.isKnownType(valueClass)) {
                            throw new IllegalStateException(errors().getString(Errors.Keys.UnsupportedImplementation_1, valueClass));
                        }
                        if (owner.defaultGeometry != null) {
                            throw new IllegalStateException(errors().getString(Errors.Keys.PropertyAlreadyExists_2,
                                    owner.getDisplayName(), AttributeConvention.GEOMETRY_PROPERTY));
                        }
                        owner.defaultGeometry = this;
                        owner.clearCache();         // The change does not impact this attribute itself.
                    }
                    break;
                }
            }
        }

        /**
         * Returns {@code true} if {@link Role#IDENTIFIER_COMPONENT} has been associated to this attribute.
         */
        @Override
        boolean isIdentifier() {
            return isIdentifier;
        }

        /**
         * Appends a text inside the value returned by {@link #toString()}, before the closing bracket.
         */
        @Override
        final void toStringInternal(final StringBuilder buffer) {
            buffer.append(" : ").append(Classes.getShortName(valueClass));
        }

        /**
         * Creates a new property type from the current setting.
         */
        @Override
        final PropertyType create() {
            final AttributeType<?>[] chrts = new AttributeType<?>[characteristics.size()];
            for (int i=0; i<chrts.length; i++) {
                chrts[i] = characteristics.get(i).build();
            }
            return new DefaultAttributeType<>(identification(), valueClass, minimumOccurs, maximumOccurs, defaultValue, chrts);
        }
    }




    /**
     * Describes one characteristic of an {@code AttributeType} to be built by the enclosing {@code FeatureTypeBuilder}.
     * A different instance of {@code Characteristic} exists for each characteristic to describe.
     * Those instances are created by:
     *
     * <ul>
     *   <li>{@link Attribute#addCharacteristic(Class)}</li>
     * </ul>
     *
     * @param <V> the class of characteristic values.
     */
    public static final class Characteristic<V> extends Builder<Characteristic<V>> {
        /**
         * The attribute type builder instance that created this {@code Characteristic} builder.
         */
        private final Attribute<?> owner;

        /**
         * The class of attribute values. Can not be changed after construction
         * because this value determines the parameterized type {@code <V>}.
         */
        private final Class<V> valueClass;

        /**
         * The default value for the attribute, or {@code null} if none.
         */
        private V defaultValue;

        /**
         * The characteristic created by this builder, or {@code null} if not yet created.
         * This field must be cleared every time that a setter method is invoked on this builder.
         */
        private transient AttributeType<V> characteristic;

        /**
         * Creates a new characteristic builder for values of the given class.
         *
         * @param owner      the builder of the {@code AttributeType} for which to add this property.
         * @param valueClass the class of characteristic values.
         */
        Characteristic(final Attribute<?> owner, final Class<V> valueClass) {
            super(null, owner.getLocale());
            this.owner = owner;
            this.valueClass = valueClass;
        }

        /**
         * Creates a new characteristic builder initialized to the values of an existing attribute.
         *
         * @param owner  the builder of the {@code AttributeType} for which to add this property.
         */
        Characteristic(final Attribute<?> owner, final AttributeType<V> template) {
            super(template, owner.getLocale());
            this.owner     = owner;
            valueClass     = template.getValueClass();
            defaultValue   = template.getDefaultValue();
            characteristic = template;
        }

        /**
         * If an {@code AttributeType<V>} has been created by the last call to {@link #build()} has been cached,
         * clears that cache. This method must be invoked every time that a setter method is invoked.
         */
        @Override
        final void clearCache() {
            characteristic = null;
            owner.clearCache();
        }

        /**
         * Returns a default name to use if the user did not specified a name. The first letter will be changed to
         * lower case (unless the name looks like an acronym) for compliance with Java convention on property names.
         */
        @Override
        final String getDefaultName() {
            return Classes.getShortName(valueClass);
        }

        /**
         * Delegates the creation of a new name to the enclosing builder.
         */
        @Override
        final GenericName name(final String scope, final String localPart) {
            return owner.name(scope, localPart);
        }

        /**
         * Sets the default value with check of the value class.
         */
        final void set(final Object value) {
            setDefaultValue(valueClass.cast(value));
        }

        /**
         * Sets the default value for the characteristic.
         *
         * @param  value  characteristic default value, or {@code null} if none.
         * @return {@code this} for allowing method calls chaining.
         */
        public Characteristic<V> setDefaultValue(final V value) {
            if (!Objects.equals(defaultValue, value)) {
                defaultValue = value;
                clearCache();
            }
            return this;
        }

        /**
         * Creates a new characteristic from the current setting.
         */
        final AttributeType<V> build() {
            if (characteristic == null) {
                characteristic = new DefaultAttributeType<>(identification(), valueClass, 0, 1, defaultValue);
            }
            return characteristic;
        }
    }




    /**
     * Builds the feature type from the information and properties specified to this builder.
     * One of the {@code setName(…)} methods must have been invoked before this {@code build()} method (mandatory).
     * All other methods are optional, but some calls to a {@code add} method are usually needed.
     *
     * @return the new feature type.
     * @throws IllegalStateException if the feature type contains incompatible
     *         {@linkplain Attribute#setCRSCharacteristic CRS characteristics}.
     */
    public FeatureType build() throws IllegalStateException {
        if (feature == null) {
            /*
             * Creates an initial array of property types with up to 3 slots reserved for @identifier, @geometry
             * and @envelope operations. At first we presume that there is always an identifier.  The identifier
             * slot will be removed later if there is none.
             */
            final int numSpecified = properties.size();     // Number of explicitely specified properties.
            int numSynthetic;                               // Number of synthetic properties that may be generated.
            int envelopeIndex = -1;
            int geometryIndex = -1;
            final PropertyType[] identifierTypes;
            if (identifierCount == 0) {
                numSynthetic    = 0;
                identifierTypes = null;
            } else {
                numSynthetic    = 1;
                identifierTypes = new PropertyType[identifierCount];
            }
            if (defaultGeometry != null) {
                envelopeIndex = numSynthetic;
                geometryIndex = numSynthetic + 1;
                numSynthetic += 2;
            }
            final PropertyType[] propertyTypes = new PropertyType[numSynthetic + numSpecified];
            int propertyCursor = numSynthetic;
            int identifierCursor = 0;
            for (int i=0; i<numSpecified; i++) {
                final Property<?>  builder = properties.get(i);
                final PropertyType instance = builder.build();
                propertyTypes[propertyCursor] = instance;
                /*
                 * Collect the attributes to use as identifier components while we loop over all properties.
                 * A NullPointerException or an ArrayIndexOutOfBoundsException in this block would mean that
                 * identifierCount field has not been updated correctly by an Attribute.addRole(Role) method.
                 */
                if (builder.isIdentifier()) {
                    identifierTypes[identifierCursor++] = instance;
                }
                /*
                 * If there is a default geometry, add a link named "@geometry" to that geometry.
                 * It may happen that the property created by the user is already named "@geometry",
                 * in which case we will avoid to duplicate the property.
                 */
                if (builder == defaultGeometry) {
                    if (propertyTypes[geometryIndex] != null) {
                        // Assuming that there is no bug in our implementation, this error could happen if the user
                        // has modified this FeatureTypeBuilder in another thread during this build() execution.
                        throw new CorruptedObjectException();
                    }
                    if (AttributeConvention.GEOMETRY_PROPERTY.equals(instance.getName())) {
                        System.arraycopy(propertyTypes, geometryIndex, propertyTypes, geometryIndex-1, (numSynthetic - geometryIndex) + i);
                        geometryIndex = -1;
                        numSynthetic--;
                        continue;           // Skip the increment of propertyCursor.
                    }
                    propertyTypes[geometryIndex] = FeatureOperations.link(name(AttributeConvention.GEOMETRY_PROPERTY), instance);
                }
                propertyCursor++;
            }
            /*
             * Create the "envelope" operation only after we created all other properties.
             * Actually it is okay if the 'propertyTypes' array still contains null elements not needed for envelope calculation
             * like "@identifier", since FeatureOperations.envelope(…) constructor ignores any property which is not for a value.
             */
            if (envelopeIndex >= 0) try {
                propertyTypes[envelopeIndex] = FeatureOperations.envelope(name(AttributeConvention.ENVELOPE_PROPERTY), null, propertyTypes);
            } catch (FactoryException e) {
                throw new IllegalStateException(e);
            }
            /*
             * If a synthetic identifier need to be created, create it now as the first property.
             * It may happen that the user provided a single identifier component already named
             * "@identifier", in which case we avoid to duplicate the property.
             */
            if (identifierTypes != null) {
                if (identifierCursor != identifierTypes.length) {
                    // Assuming that there is no bug in our implementation, this error could happen if the user
                    // has modified this FeatureTypeBuilder in another thread during this build() execution.
                    throw new CorruptedObjectException();
                }
                if (identifierCursor == 1 && AttributeConvention.IDENTIFIER_PROPERTY.equals(identifierTypes[0].getName())) {
                    System.arraycopy(propertyTypes, 1, propertyTypes, 0, --propertyCursor);
                } else {
                    propertyTypes[0] = FeatureOperations.compound(name(AttributeConvention.IDENTIFIER_PROPERTY),
                            idDelimiter, idPrefix, idSuffix, identifierTypes);
                }
            }
            feature = new DefaultFeatureType(identification(), isAbstract,
                    superTypes.toArray(new FeatureType[superTypes.size()]),
                    ArraysExt.resize(propertyTypes, propertyCursor));
        }
        return feature;
    }

    /**
     * Helper method for creating identification info of synthetic attributes.
     */
    private static Map<String,?> name(final GenericName name) {
        return Collections.singletonMap(AbstractOperation.NAME_KEY, name);
    }

    /**
     * Invoked by {@link Builder} for creating new {@code LocalName} or {@code GenericName} instances.
     */
    @Override
    final GenericName name(String scope, final String localPart) {
        if (scope == null) {
            scope = defaultScope;
        }
        if (scope == null || scope.isEmpty()) {
            return nameFactory.createLocalName(null, localPart);
        } else {
            return nameFactory.createGenericName(null, scope, localPart);
        }
    }

    /**
     * Formats a string representation of this builder for debugging purpose.
     */
    @Override
    final void toStringInternal(final StringBuilder buffer) {
        if (isAbstract) {
            buffer.insert(buffer.indexOf("[") + 1, "abstract ");
        }
        String separator = " : ";
        for (final FeatureType parent : superTypes) {
            buffer.append(separator).append('“').append(parent.getName()).append('”');
            separator = ", ";
        }
        buffer.append(" {");
        separator = System.lineSeparator();
        for (final Property<?> p : properties) {
            p.toString(buffer.append(separator).append("    ").append(p.getClass().getSimpleName()));
        }
        buffer.append(separator).append('}');
    }
}
