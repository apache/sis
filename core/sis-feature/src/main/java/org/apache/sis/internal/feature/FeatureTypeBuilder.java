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
import org.apache.sis.util.NullArgumentException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;

// Branch-dependent imports
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.AbstractIdentifiedType;


/**
 * Helper class for the creation of {@link FeatureType} instances.
 * This builder can create the parameters to be given to the {@linkplain DefaultFeatureType#DefaultFeatureType
 * feature type constructor} from simpler parameters given to this builder.
 *
 * <p>{@code FeatureTypeBuilder} should be short lived.
 * After the {@code FeatureType} has been created, the builder should be discarded.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
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
     * Builders for the properties of this feature.
     */
    private final List<Property<?>> properties;

    /**
     * The parent of the feature to create. By default, new features have no parent.
     */
    private final List<DefaultFeatureType> superTypes;

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
     * If {@link #idAttributes} is non-null, an optional prefix or suffix to insert before
     * or after the {@linkplain FeatureOperations#compound compound key} named {@code "@id"}.
     */
    private String idPrefix, idSuffix;

    /**
     * If {@link #idAttributes} is non-null and contains more than one value, the separator to insert between
     * each single component in a {@linkplain FeatureOperations#compound compound key} named {@code "@id"}.
     */
    private String idDelimiter;

    /**
     * The attributes to use in a {@linkplain FeatureOperations#compound compound key} named {@code "@id"},
     * or {@code null} if none. If this array contains only one property and {@link #idPrefix} is null,
     * then {@code "@id"} will be a {@linkplain FeatureOperations#link link} to {@code idAttributes[0]}.
     */
    private final List<Property<?>> idAttributes;

    /**
     * The default geometry attribute, or {@code null} if none.
     *
     * @see AttributeConvention#DEFAULT_GEOMETRY_PROPERTY
     */
    private Property<?> defaultGeometry;

    /**
     * Creates a new builder instance using the default name factory.
     */
    public FeatureTypeBuilder() {
        this(DefaultFactories.forBuildin(NameFactory.class));
    }

    /**
     * Creates a new builder instance using the given name factory.
     *
     * @param factory  the factory to use for creating names.
     */
    public FeatureTypeBuilder(final NameFactory factory) {
        nameFactory  = factory;
        properties   = new ArrayList<Property<?>>();
        superTypes   = new ArrayList<DefaultFeatureType>();
        idAttributes = new ArrayList<Property<?>>();
        idDelimiter  = ":";
        defaultMinimumOccurs = 1;
        defaultMaximumOccurs = 1;
    }

    /**
     * Resets this builder to its initial state. After invocation of this method,
     * this builder is in the same state than after construction.
     *
     * @return {@code this} for allowing method calls chaining.
     */
    @Override
    public FeatureTypeBuilder clear() {
        super.clear();
        properties  .clear();
        superTypes  .clear();
        idAttributes.clear();
        idDelimiter     = ":";
        idPrefix        = null;
        idSuffix        = null;
        isAbstract      = false;
        defaultGeometry = null;
        defaultMinimumOccurs = 1;
        defaultMaximumOccurs = 1;
        return this;
    }

    /**
     * Sets whether the feature type is abstract.
     * If this method is not invoked, then the default value is {@code false}.
     *
     * @param  isAbstract whether the feature type is abstract.
     * @return {@code this} for allowing method calls chaining.
     */
    public FeatureTypeBuilder setAbstract(final boolean isAbstract) {
        this.isAbstract = isAbstract;
        return this;
    }

    /**
     * Sets the parent types (or super-type) from which to inherit properties.
     * If this method is not invoked, then the default value is to have no parent.
     *
     * @param  parents  the parent types from which to inherit properties, or an empty array if none.
     * @return {@code this} for allowing method calls chaining.
     */
    public FeatureTypeBuilder setSuperTypes(final DefaultFeatureType... parents) {
        ArgumentChecks.ensureNonNull("parents", parents);
        superTypes.clear();
        superTypes.addAll(Arrays.asList(parents));
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
     * @see Property#setCardinality(int, int)
     */
    public FeatureTypeBuilder setDefaultCardinality(final int minimumOccurs, final int maximumOccurs) {
        if (minimumOccurs < 0 || maximumOccurs < minimumOccurs) {
            throw new IllegalArgumentException(errors().getString(Errors.Keys.IllegalRange_2, minimumOccurs, maximumOccurs));
        }
        defaultMinimumOccurs = minimumOccurs;
        defaultMaximumOccurs = maximumOccurs;
        return this;
    }

    /**
     * Sets the prefix, suffix and delimiter to use when formatting a compound identifier made of two or more attributes.
     * The strings specified to this method will be used only if {@link #addIdentifier(Class)} is invoked more than once.
     *
     * <p>If this method is not invoked, then the default values are the {@code ":"} delimiter and no prefix or suffix.</p>
     *
     * @param  delimiter  the characters to use as delimiter between each single property value.
     * @param  prefix     characters to use at the beginning of the concatenated string, or {@code null} if none.
     * @param  suffix     characters to use at the end of the concatenated string, or {@code null} if none.
     * @return {@code this} for allowing method calls chaining.
     *
     * @see java.util.StringJoiner
     * @see FeatureOperations#compound(Map, String, String, String, PropertyType...)
     */
    public FeatureTypeBuilder setIdentifierDelimiters(final String delimiter, final String prefix, final String suffix) {
        ArgumentChecks.ensureNonEmpty("delimiter", delimiter);
        idDelimiter = delimiter;
        idPrefix    = prefix;
        idSuffix    = suffix;
        return this;
    }

    /**
     * Creates a new {@code AttributeType} builder for values of the given class which will be used as identifiers.
     * An arbitrary amount of attributes can be specified as identifiers:
     *
     * <ul>
     *   <li>If this method is never invoked, no attribute is marked as feature identifier.</li>
     *   <li>If this method is invoked exactly once, then a new attribute is created in the same way than
     *       {@link #addAttribute(Class)} and a synthetic attribute named {@code "@id"} will be created as
     *       a {@linkplain FeatureOperations#link link} to the new attribute.</li>
     *   <li>If this method is invoked more than once, then new attributes are created in the same way than
     *       {@link #addAttribute(Class)} and a synthetic attribute named {@code "@id"} will be created as
     *       a {@linkplain FeatureOperations#compound compound key} made of all identifiers.</li>
     * </ul>
     *
     * Callers shall invoke at least one of the {@code Property.setName(…)} methods on the returned instance.
     * All other methods are optional.
     *
     * @param  <V>  the compile-time value of {@code valueClass} argument.
     * @param  valueClass  the class of attribute values.
     * @return a builder for an {@code AttributeType}.
     */
    public <V> Property<V> addIdentifier(final Class<V> valueClass) {
        ensureAttributeType(valueClass);
        final Property<V> property = new Property<V>(valueClass);
        idAttributes.add(property);
        properties.add(property);
        return property;
    }

    /**
     * Creates a new {@code AttributeType} builder for a geometry which will be flagged as the default geometry.
     * Callers shall invoke at least one of the {@code Property.setName(…)} methods on the returned instance.
     * All other methods are optional.
     *
     * @param  <V>  the compile-time value of {@code valueClass} argument.
     * @param  valueClass  the geometry class of attribute values.
     * @return a builder for an {@code AttributeType} which will contain the default geometry.
     * @throws IllegalArgumentException if the given type is not a supported geometry type.
     * @throws IllegalStateException if a default geometry has already been specified to this builder.
     */
    public <V> Property<V> addDefaultGeometry(final Class<V> valueClass) {
        ensureAttributeType(valueClass);
        if (!Geometries.isKnownType(valueClass)) {
            throw new IllegalArgumentException(errors().getString(Errors.Keys.UnsupportedImplementation_1, valueClass));
        }
        if (defaultGeometry != null) {
            throw new IllegalStateException(errors().getString(Errors.Keys.PropertyAlreadyExists_2,
                    getDisplayName(), AttributeConvention.DEFAULT_GEOMETRY_PROPERTY));
        }
        final Property<V> property = new Property<V>(valueClass);
        defaultGeometry = property;
        properties.add(property);
        return property;
    }

    /**
     * Creates a new {@code AttributeType} builder for values of the given class.
     * Callers shall invoke at least one of the {@code Property.setName(…)} methods on the returned instance.
     * All other methods are optional.
     *
     * <p>Usage example:</p>
     * {@preformat java
     *     builder.addAttribute(String.class).setName("City");
     * }
     *
     * @param  <V>  the compile-time value of {@code valueClass} argument.
     * @param  valueClass  the class of attribute values.
     * @return a builder for an {@code AttributeType}.
     */
    public <V> Property<V> addAttribute(final Class<V> valueClass) {
        ensureAttributeType(valueClass);
        final Property<V> property = new Property<V>(valueClass);
        properties.add(property);
        return property;
    }

    /**
     * Ensures that the given value class is not null and not assignable to {@code Feature}.
     * We disallow {@code Feature.class} because those type shall be handled as associations
     * instead than attributes.
     */
    private void ensureAttributeType(final Class<?> valueClass) {
        if (valueClass == null) {
            throw new NullArgumentException(errors().getString(Errors.Keys.NullArgument_1, "valueClass"));
        }
        if (AbstractFeature.class.isAssignableFrom(valueClass)) {
            throw new IllegalArgumentException(errors().getString(Errors.Keys.IllegalArgumentValue_2, "valueClass", valueClass));
        }
    }

    /**
     * Creates a new {@code FeatureAssociationRole} builder for features of the given type.
     * Callers shall invoke at least one of the {@code Property.setName(…)} methods on the returned instance.
     * All other methods are optional.
     *
     * @param  type  the type of feature values.
     * @return a builder for a {@code FeatureAssociationRole}.
     */
    public Property<AbstractFeature> addAssociation(final DefaultFeatureType type) {
        ArgumentChecks.ensureNonNull("type", type);
        final Property<AbstractFeature> property = new Property<AbstractFeature>(AbstractFeature.class, DefaultFeatureType.class, type);
        properties.add(property);
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
    public Property<AbstractFeature> addAssociation(final GenericName type) {
        ArgumentChecks.ensureNonNull("type", type);
        final Property<AbstractFeature> property = new Property<AbstractFeature>(AbstractFeature.class, GenericName.class, type);
        properties.add(property);
        return property;
    }

    /**
     * Describes one property of the {@code FeatureType} to be built by the enclosing {@code FeatureTypeBuilder}.
     * A different instance of {@code Property} exists for each property to describe. Those instances are created by:
     *
     * <ul>
     *   <li>{@link FeatureTypeBuilder#addIdentifier(Class)}</li>
     *   <li>{@link FeatureTypeBuilder#addDefaultGeometry(Class)}</li>
     *   <li>{@link FeatureTypeBuilder#addAttribute(Class)}</li>
     *   <li>{@link FeatureTypeBuilder#addAssociation(FeatureType)}</li>
     *   <li>{@link FeatureTypeBuilder#addAssociation(GenericName)}</li>
     * </ul>
     *
     * @param <V> the class of property values.
     */
    public final class Property<V> extends Builder<Property<V>> {
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
         * The minimum number of property values.
         * The default value is 1, unless otherwise specified by {@link #setDefaultCardinality(int, int)}.
         *
         * @see #setCardinality(int, int)
         */
        private int minimumOccurs;

        /**
         * The maximum number of property values. The default value is 1.
         * The default value is 1, unless otherwise specified by {@link #setDefaultCardinality(int, int)}.
         *
         * @see #setCardinality(int, int)
         */
        private int maximumOccurs;

        /**
         * Builders for the characteristics associated to the attribute.
         */
        private final List<Characteristic<?>> characteristics;

        /**
         * Creates a new {@code AttributeType} or {@code Operation} builder for values of the given class.
         *
         * @param valueClass the class of property values.
         */
        Property(final Class<V> valueClass) {
            this.valueClass = valueClass;
            minimumOccurs   = defaultMinimumOccurs;
            maximumOccurs   = defaultMaximumOccurs;
            characteristics = new ArrayList<Characteristic<?>>();
        }

        /**
         * Creates a new {@code AssociationRole} builder for values of the given type.
         * This constructor arbitrarily stores the feature type as an unnamed characteristic of this property.
         *
         * @param valueClass shall be {@code Feature.class}.
         * @param typeClass  shall be either {@code FeatureType.class} or {@code GenericName.class}.
         * @param type       the type of associated features.
         */
        <C> Property(final Class<V> valueClass, final Class<C> typeClass, final C type) {
            this.valueClass = valueClass;
            minimumOccurs   = defaultMinimumOccurs;
            maximumOccurs   = defaultMaximumOccurs;
            characteristics = Collections.<Characteristic<?>>singletonList(new Characteristic<C>(typeClass).setDefaultValue(type));
        }

        /**
         * Delegates the creation of a new name to the enclosing builder.
         */
        @Override
        final GenericName name(final String scope, final String localPart) {
            return FeatureTypeBuilder.this.name(scope, localPart);
        }

        /**
         * Sets the minimum and maximum number of property values. Those numbers must be equal or greater than zero.
         *
         * <p>If this method is not invoked, then the default values are the cardinality specified by the last call
         * to {@link #setDefaultCardinality(int, int)} at the time this {@code Property} instance has been created.
         * If the later method has not invoked neither, then the default cardinality is [1 … 1].</p>
         *
         * @param  minimumOccurs  new minimum number of property values.
         * @param  maximumOccurs  new maximum number of property values.
         * @return {@code this} for allowing method calls chaining.
         */
        public Property<V> setCardinality(final int minimumOccurs, final int maximumOccurs) {
            if (minimumOccurs < 0 || maximumOccurs < minimumOccurs) {
                throw new IllegalArgumentException(errors().getString(Errors.Keys.IllegalRange_2, minimumOccurs, maximumOccurs));
            }
            this.minimumOccurs = minimumOccurs;
            this.maximumOccurs = maximumOccurs;
            return this;
        }

        /**
         * Sets the default value for the property.
         *
         * @param  defaultValue  default property value, or {@code null} if none.
         * @return {@code this} for allowing method calls chaining.
         */
        public Property<V> setDefaultValue(final V defaultValue) {
            this.defaultValue = defaultValue;
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
        public final Property<V> setValidValues(final V... values) {
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
        public Property<V> setMaximalLengthCharacteristic(final Integer length) {
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
        public Property<V> setCRSCharacteristic(final CoordinateReferenceSystem crs) {
            return setCharacteristic(AttributeConvention.CRS_CHARACTERISTIC, CoordinateReferenceSystem.class, crs);
        }

        /**
         * Implementation of all setter methods for characteristics.
         *
         * @throws UnsupportedOperationException if this property does not support characteristics.
         */
        private <C> Property<V> setCharacteristic(final GenericName name, final Class<C> type, final C value) {
            for (final Characteristic<?> characteristic : characteristics) {
                if (name.equals(characteristic.identification.get(DefaultAttributeType.NAME_KEY))) {
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
         * Callers shall invoke at least one of the {@code Characteristic.setName(…)} methods on the returned instance.
         * All other methods are optional.
         *
         * @param  <C>   the compile-time type of {@code type} argument.
         * @param  type  the class of characteristic values.
         * @return a builder for a characteristic of this attribute.
         * @throws UnsupportedOperationException if this property does not support characteristics.
         */
        public <C> Characteristic<C> addCharacteristic(final Class<C> type) {
            if (valueClass == AbstractFeature.class) {
                throw new UnsupportedOperationException(errors().getString(Errors.Keys.IllegalOperationForValueClass_1, valueClass));
            }
            ArgumentChecks.ensureNonNull("type", type);
            final Characteristic<C> characteristic = new Characteristic<C>(type);
            characteristics.add(characteristic);
            return characteristic;
        }

        /**
         * Creates a new property type from the current setting.
         */
        final AbstractIdentifiedType build() {
            final AbstractIdentifiedType property;
            if (valueClass == AbstractFeature.class) {
                final Object type = CollectionsExt.first(characteristics).defaultValue;
                if (type instanceof DefaultFeatureType) {
                    property = new DefaultAssociationRole(identification, (DefaultFeatureType) type, minimumOccurs, maximumOccurs);
                } else {
                    property = new DefaultAssociationRole(identification, (GenericName) type, minimumOccurs, maximumOccurs);
                }
            } else {
                final DefaultAttributeType<?>[] chrts = new DefaultAttributeType<?>[characteristics.size()];
                for (int i=0; i<chrts.length; i++) {
                    chrts[i] = characteristics.get(i).build();
                }
                property = new DefaultAttributeType<V>(identification, valueClass, minimumOccurs, maximumOccurs, defaultValue, chrts);
            }
            return property;
        }
    }

    /**
     * Describes one characteristic of the {@code AttributeType} to be built by the enclosing {@code FeatureTypeBuilder}.
     * A different instance of {@code Characteristic} exists for each characteristic to describe.
     * Those instances are created by:
     *
     * <ul>
     *   <li>{@link Property#addCharacteristic(Class)}</li>
     * </ul>
     *
     * @param <V> the class of characteristic values.
     */
    public final class Characteristic<V> extends Builder<Characteristic<V>> {
        /**
         * The class of attribute values. Can not be changed after construction
         * because this value determines the parameterized type {@code <V>}.
         */
        private final Class<V> valueClass;

        /**
         * The default value for the attribute, or {@code null} if none.
         */
        V defaultValue;

        /**
         * Creates a new characteristic builder for values of the given class.
         *
         * @param valueClass the class of characteristic values.
         */
        Characteristic(final Class<V> valueClass) {
            this.valueClass = valueClass;
        }

        /**
         * Delegates the creation of a new name to the enclosing builder.
         */
        @Override
        final GenericName name(final String scope, final String localPart) {
            return FeatureTypeBuilder.this.name(scope, localPart);
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
            defaultValue = value;
            return this;
        }

        /**
         * Creates a new characteristic from the current setting.
         */
        final DefaultAttributeType<V> build() {
            return new DefaultAttributeType<V>(identification, valueClass, 0, 1, defaultValue);
        }
    }

    /**
     * Builds the feature type from the information and properties specified to this builder.
     * One of the {@code setName(…)} methods must have been invoked before this {@code build()} method (mandatory).
     * All other methods are optional, but some calls to a {@code add} method are usually needed.
     *
     * @return the new feature type.
     * @throws IllegalStateException if the feature type contains incompatible
     *         {@linkplain Property#setCRSCharacteristic CRS characteristics}.
     */
    public DefaultFeatureType build() throws IllegalStateException {
        int numSynthetic;                                   // Number of synthetic properties to be generated.
        int numSpecified = properties.size();               // Number of explicitely specified properties.
        final AbstractIdentifiedType[] identifierTypes;
        if (idAttributes.isEmpty()) {
            identifierTypes = null;
            numSynthetic = 0;
        } else {
            identifierTypes = new AbstractIdentifiedType[idAttributes.size()];
            numSynthetic = 1;                               // Reserve one slot for the synthetic property "@id".
        }
        if (defaultGeometry != null) {
            numSynthetic += 2;                              // One slot for "@defaultGeometry" and one for "@envelope".
        }
        AbstractIdentifiedType[] propertyTypes = new AbstractIdentifiedType[numSynthetic + numSpecified];
        for (int i=0,j=numSynthetic; i<numSpecified; i++, j++) {
            final Property<?>  builder  = properties.get(i);
            final AbstractIdentifiedType instance = builder.build();
            propertyTypes[j] = instance;
            final int id = idAttributes.indexOf(builder);
            if (id >= 0) {
                identifierTypes[id] = instance;
            }
            /*
             * If there is a default geometry, add a link named "@geometry" to that geometry. It may happen
             * that the property created by the user is already named "@geometry", in which case will will
             * avoid to duplicate the property by removing the second occurrence.
             */
            if (builder == defaultGeometry) {
                final AbstractIdentifiedType geom;
                if (AttributeConvention.DEFAULT_GEOMETRY_PROPERTY.equals(instance.getName())) {
                    propertyTypes = ArraysExt.remove(propertyTypes, j--, 1);
                    geom = instance;
                } else {
                    geom = FeatureOperations.link(name(AttributeConvention.DEFAULT_GEOMETRY_PROPERTY), instance);
                }
                propertyTypes[numSynthetic - 1] = geom;
            }
        }
        /*
         * Create the "envelope" operation only after we created all other properties, except "@id" which is not needed
         * for envelope. It is okay if the 'propertyTypes' array still contains null elements like the "@id" one, since
         * FeatureOperations.envelope(…) constructor will ignore any property which is not for a value.
         */
        if (defaultGeometry != null) try {
            propertyTypes[numSynthetic - 2] = FeatureOperations.envelope(name(AttributeConvention.ENVELOPE_PROPERTY), null, propertyTypes);
        } catch (FactoryException e) {
            throw new IllegalStateException(e);
        }
        if (identifierTypes != null) {
            propertyTypes[0] = FeatureOperations.compound(name(AttributeConvention.ID_PROPERTY), idDelimiter, idPrefix, idSuffix, identifierTypes);
        }
        return new DefaultFeatureType(identification, isAbstract, superTypes.toArray(new DefaultFeatureType[superTypes.size()]), propertyTypes);
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
     * Returns the resources for error messages.
     */
    final Errors errors() {
        return Errors.getResources(identification);
    }
}
