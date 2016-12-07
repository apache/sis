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
package org.apache.sis.feature.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.lang.reflect.Array;
import org.opengis.util.GenericName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.feature.FeatureOperations;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.util.SetOfUnknownSize;
import org.apache.sis.internal.util.AbstractIterator;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.Resources;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;

// Branch-dependent imports
import org.opengis.feature.AttributeType;


/**
 * Describes one {@code AttributeType} which will be part of the feature type to be built by
 * a {@code FeatureTypeBuilder}. An attribute can be for example a city name, a temperature
 * (together with its units of measurement and uncertainty if desired) or a geometric shape.
 * Attribute types contain the following information:
 *
 * <ul>
 *   <li>the name        — a unique name which can be defined within a scope (or namespace).</li>
 *   <li>the definition  — a concise definition of the element.</li>
 *   <li>the designation — a natural language designator for the element for user interfaces.</li>
 *   <li>the description — information beyond that required for concise definition of the element.</li>
 *   <li>the value class — often {@link String}, {@link Float} or {@link com.esri.core.geometry.Geometry}.
 *       Must be specified at {@linkplain FeatureTypeBuilder#addAttribute(Class) construction time}.</li>
 *   <li>a default value — to be used when an attribute instance does not provide an explicit value.</li>
 *   <li>characteristics — for example the units of measurement for all attributes of the same type.</li>
 *   <li>cardinality     — the minimum and maximum occurrences of attribute values.</li>
 * </ul>
 *
 * @param <V> the class of attribute values.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 *
 * @see FeatureTypeBuilder#addAttribute(Class)
 * @see org.apache.sis.feature.DefaultAttributeType
 */
public final class AttributeTypeBuilder<V> extends PropertyTypeBuilder {
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
     * Whether this attribute will be used in a {@linkplain FeatureOperations#compound compound key} named
     * {@code "@identifier"}. If only one attribute has this flag and {@link FeatureTypeBuilder#idPrefix} and
     * {@code isSuffix} are null, then {@code "@identifier"} will be a {@linkplain FeatureOperations#link link}
     * to {@code idAttributes[0]}.
     *
     * @see #addRole(AttributeRole)
     */
    private boolean isIdentifier;

    /**
     * Builders for the characteristics associated to the attribute.
     */
    final List<CharacteristicTypeBuilder<?>> characteristics;

    /**
     * The attribute type created by this builder, or {@code null} if not yet created.
     * This field must be cleared every time that a setter method is invoked on this builder.
     */
    private transient AttributeType<V> property;

    /**
     * Creates a new builder initialized to the values of the given builder.
     * This constructor is for {@link #setValueClass(Class)} implementation only.
     *
     * @throws UnconvertibleObjectException if the default value can not be converted to the given class.
     */
    private AttributeTypeBuilder(final AttributeTypeBuilder<?> builder, final Class<V> valueClass)
            throws UnconvertibleObjectException
    {
        super(builder);
        this.valueClass = valueClass;
        defaultValue = ObjectConverters.convert(builder.defaultValue, valueClass);
        isIdentifier = builder.isIdentifier;
        characteristics = builder.characteristics;
    }

    /**
     * Creates a new {@code AttributeType} builder for values of the given class.
     *
     * @param owner      the builder of the {@code FeatureType} for which to add the attribute.
     * @param valueClass the class of attribute values.
     */
    AttributeTypeBuilder(final FeatureTypeBuilder owner, final Class<V> valueClass) {
        super(owner, null);
        this.valueClass = valueClass;
        characteristics = new ArrayList<>();
    }

    /**
     * Creates a new {@code AttributeType} builder initialized to the values of an existing attribute.
     *
     * @param owner  the builder of the {@code FeatureType} for which to add the attribute.
     */
    AttributeTypeBuilder(final FeatureTypeBuilder owner, final AttributeType<V> template) {
        super(owner, template);
        property      = template;
        minimumOccurs = template.getMinimumOccurs();
        maximumOccurs = template.getMaximumOccurs();
        valueClass    = template.getValueClass();
        defaultValue  = template.getDefaultValue();
        final Map<String, AttributeType<?>> tc = template.characteristics();
        characteristics = new ArrayList<>(tc.size());
        for (final AttributeType<?> c : tc.values()) {
            characteristics.add(new CharacteristicTypeBuilder<>(this, c));
        }
    }

    /**
     * If the {@code AttributeType} created by the last call to {@link #build()} has been cached,
     * clears that cache. This method must be invoked every time that a setter method is invoked.
     */
    @Override
    final void clearCache() {
        property = null;
        super.clearCache();
    }

    /**
     * Returns a default name to use if the user did not specified a name. The first letter will be changed to
     * lower case (unless the name looks like an acronym) for compliance with Java convention on attribute names.
     */
    @Override
    final String getDefaultName() {
        return Classes.getShortName(valueClass);
    }

    /**
     * Sets the {@code AttributeType} name as a generic name.
     * If another name was defined before this method call, that previous value will be discarded.
     *
     * @return {@code this} for allowing method calls chaining.
     */
    @Override
    public AttributeTypeBuilder<V> setName(final GenericName name) {
        super.setName(name);
        return this;
    }

    /**
     * Sets the {@code AttributeType} name as a simple string with the default scope.
     * The default scope is the value specified by the last call to
     * {@link FeatureTypeBuilder#setDefaultScope(String)}.
     * The name will be a {@linkplain org.apache.sis.util.iso.DefaultLocalName local name} if no default scope
     * has been specified, or a {@linkplain org.apache.sis.util.iso.DefaultScopedName scoped name} otherwise.
     *
     * @return {@code this} for allowing method calls chaining.
     */
    @Override
    public AttributeTypeBuilder<V> setName(final String localPart) {
        super.setName(localPart);
        return this;
    }

    /**
     * Sets the {@code AttributeType} name as a string in the given scope.
     * The name will be a {@linkplain org.apache.sis.util.iso.DefaultLocalName local name} if the given scope is
     * {@code null} or empty, or a {@linkplain org.apache.sis.util.iso.DefaultScopedName scoped name} otherwise.
     * If a {@linkplain FeatureTypeBuilder#setDefaultScope(String) default scope} has been specified, then the
     * {@code scope} argument overrides it.
     *
     * @return {@code this} for allowing method calls chaining.
     */
    @Override
    public AttributeTypeBuilder<V> setName(final String scope, final String localPart) {
        super.setName(scope, localPart);
        return this;
    }

    /**
     * Sets the minimum number of attribute values. If the given number is greater than the
     * {@linkplain #getMaximumOccurs() maximal number} of attribute values, than the maximum
     * is also set to that value.
     *
     * @param  occurs the new minimum number of attribute values.
     * @return {@code this} for allowing method calls chaining.
     */
    @Override
    public AttributeTypeBuilder<V> setMinimumOccurs(final int occurs) {
        super.setMinimumOccurs(occurs);
        return this;
    }

    /**
     * Sets the maximum number of attribute values. If the given number is less than the
     * {@linkplain #getMinimumOccurs() minimal number} of attribute values, than the minimum
     * is also set to that value.
     *
     * @param  occurs the new maximum number of attribute values.
     * @return {@code this} for allowing method calls chaining.
     */
    @Override
    public AttributeTypeBuilder<V> setMaximumOccurs(final int occurs) {
        super.setMaximumOccurs(occurs);
        return this;
    }

    /**
     * Returns the class of attribute values.
     *
     * @return the class of attribute values.
     *
     * @see #setValueClass(Class)
     */
    public Class<V> getValueClass() {
        return valueClass;
    }

    /**
     * Sets the class of attribute values. Callers <strong>must</strong> use the builder returned by this method
     * instead of {@code this} builder after this method call, since the returned builder may be a new instance.
     *
     * @param  <N>   the compile-time value of the {@code type} argument.
     * @param  type  the new class of attribute values.
     * @return the attribute builder — <em>not necessarily this instance.</em>
     * @throws UnconvertibleObjectException if the {@linkplain #getDefaultValue() default value}
     *         can not be converted to the given {@code <N>} class.
     *
     * @see #getValueClass()
     */
    @SuppressWarnings("unchecked")
    public <N> AttributeTypeBuilder<N> setValueClass(final Class<N> type) throws UnconvertibleObjectException {
        final FeatureTypeBuilder owner = owner();
        ensureNonNull("type", type);
        if (type == valueClass) {
            return (AttributeTypeBuilder<N>) this;
        }
        final AttributeTypeBuilder<N> newb = new AttributeTypeBuilder<>(this, type);
        for (final CharacteristicTypeBuilder<?> c : characteristics) {
            c.owner(newb);
        }
        owner.replace(this, newb);
        dispose();
        return newb;
    }

    /**
     * Returns the default value for the attribute, or {@code null} if none.
     *
     * @return the default attribute value, or {@code null} if none.
     *
     * @see #setDefaultValue(Object)
     */
    public V getDefaultValue() {
        return defaultValue;
    }

    /**
     * Sets the default value for the attribute.
     *
     * @param  value  default attribute value, or {@code null} if none.
     * @return {@code this} for allowing method calls chaining.
     *
     * @see #getDefaultValue()
     */
    public AttributeTypeBuilder<V> setDefaultValue(final V value) {
        if (!Objects.equals(defaultValue, value)) {
            defaultValue = value;
            clearCache();
        }
        return this;
    }

    /**
     * Returns an enumeration of valid values for the attribute, or an empty array if none.
     * This convenience method returns the value of the characteristic set by {@link #setValidValues(Object...)}.
     *
     * @return valid values for the attribute, or an empty array if none.
     */
    @SuppressWarnings("unchecked")
    public V[] getValidValues() {
        final Collection<?> c = CollectionsExt.nonNull((Collection<?>)
                getCharacteristic(AttributeConvention.VALID_VALUES_CHARACTERISTIC));
        final V[] values = (V[]) Array.newInstance(valueClass, c.size());
        int index = 0;
        for (final Object value : c) {
            values[index++] = (V) value;        // ArrayStoreException if 'value' is not the expected type.
        }
        return values;
    }

    /**
     * Sets an enumeration of valid values for the attribute.
     *
     * <p>This is a convenience method for {@link #addCharacteristic(Class)} with a value
     * of type {@link Set} and a conventional name.</p>
     *
     * @param  values valid values.
     * @return {@code this} for allowing method calls chaining.
     *
     * @see #characteristics()
     * @see AttributeConvention#VALID_VALUES_CHARACTERISTIC
     */
    @SafeVarargs
    public final AttributeTypeBuilder<V> setValidValues(final V... values) {
        return setCharacteristic(AttributeConvention.VALID_VALUES_CHARACTERISTIC,
                Set.class, CollectionsExt.immutableSet(false, values));
    }

    /**
     * Returns the maximal length that characterizes the {@link CharSequence} values of this attribute.
     * This convenience method returns the value of the characteristic set by {@link #setMaximalLength(Integer)}.
     *
     * @return the maximal length of {@link CharSequence} attribute values, or {@code null}.
     */
    public Integer getMaximalLength() {
        return (Integer) getCharacteristic(AttributeConvention.MAXIMAL_LENGTH_CHARACTERISTIC);
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
     *
     * @see #characteristics()
     * @see AttributeConvention#MAXIMAL_LENGTH_CHARACTERISTIC
     */
    public AttributeTypeBuilder<V> setMaximalLength(final Integer length) {
        return setCharacteristic(AttributeConvention.MAXIMAL_LENGTH_CHARACTERISTIC, Integer.class, length);
    }

    /**
     * Returns the coordinate reference system associated to attribute values.
     * This convenience method returns the value of the characteristic set by {@link #setCRS(CoordinateReferenceSystem)}.
     *
     * @return the coordinate reference system associated to attribute values, or {@code null}.
     */
    public CoordinateReferenceSystem getCRS() {
        return (CoordinateReferenceSystem) getCharacteristic(AttributeConvention.CRS_CHARACTERISTIC);
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
     *
     * @see #characteristics()
     * @see AttributeConvention#CRS_CHARACTERISTIC
     */
    public AttributeTypeBuilder<V> setCRS(final CoordinateReferenceSystem crs) {
        return setCharacteristic(AttributeConvention.CRS_CHARACTERISTIC, CoordinateReferenceSystem.class, crs);
    }

    /**
     * Implementation of all getter methods for characteristics.
     */
    private Object getCharacteristic(final GenericName name) {
        for (final CharacteristicTypeBuilder<?> characteristic : characteristics) {
            if (name.equals(characteristic.getName())) {
                return characteristic.getDefaultValue();
            }
        }
        return null;
    }

    /**
     * Implementation of all setter methods for characteristics.
     */
    private <C> AttributeTypeBuilder<V> setCharacteristic(final GenericName name, final Class<C> type, final C value) {
        for (final CharacteristicTypeBuilder<?> characteristic : characteristics) {
            if (name.equals(characteristic.getName())) {
                characteristic.set(value);
                clearCache();
                return this;
            }
        }
        addCharacteristic(type).setDefaultValue(value).setName(name);
        return this;
    }

    /**
     * Returns the builder for the characteristic of the given name. The given name does not need to contains
     * all elements of a {@link org.opengis.util.ScopedName}; it is okay to specify only the tip (for example
     * {@code "myName"} instead of {@code "myScope:myName"}) provided that ignoring the name head does not
     * create ambiguity.
     *
     * @param  name   name of the characteristic to search.
     * @return characteristic of the given name, or {@code null} if none.
     * @throws IllegalArgumentException if the given name is ambiguous.
     *
     * @see #characteristics()
     */
    public CharacteristicTypeBuilder<?> getCharacteristic(final String name) {
        return forName(characteristics, name);
    }

    /**
     * Adds another attribute type that describes this attribute type.
     * See <cite>"Attribute characterization"</cite> in {@link DefaultAttributeType} Javadoc for more information.
     *
     * <p>Usage example:</p>
     * {@preformat java
     *     attribute.addCharacteristic(Unit.class).setName("Unit of measurement").setDefaultValue(Units.CELSIUS);
     * }
     *
     * The default characteristic name is the name of the given type, but callers should invoke one
     * of the {@code CharacteristicTypeBuilder.setName(…)} methods on the returned instance with a better name.
     *
     * @param  <C>   the compile-time type of {@code type} argument.
     * @param  type  the class of characteristic values.
     * @return a builder for a characteristic of this attribute.
     *
     * @see #characteristics()
     */
    public <C> CharacteristicTypeBuilder<C> addCharacteristic(final Class<C> type) {
        ensureNonNull("type", type);
        final CharacteristicTypeBuilder<C> characteristic = new CharacteristicTypeBuilder<>(this, type);
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
     *
     * @see #characteristics()
     */
    public <C> CharacteristicTypeBuilder<C> addCharacteristic(final AttributeType<C> template) {
        ensureNonNull("template", template);
        final CharacteristicTypeBuilder<C> characteristic = new CharacteristicTypeBuilder<>(this, template);
        characteristics.add(characteristic);
        clearCache();
        return characteristic;
    }

    /**
     * Returns a view of all characteristics added to the {@code AttributeType} to build.
     * The returned list is <cite>live</cite>: changes in this builder are reflected in that list and conversely.
     * However the returned list allows only {@linkplain List#remove(Object) remove} operations;
     * new characteristics can be added only by calls to one of the {@code set/addCharacteristic(…)} methods.
     *
     * @return a live list over the characteristics declared to this builder.
     *
     * @see #getCharacteristic(String)
     * @see #addCharacteristic(Class)
     * @see #addCharacteristic(AttributeType)
     * @see #setValidValues(Object...)
     * @see #setCRS(CoordinateReferenceSystem)
     */
    public List<CharacteristicTypeBuilder<?>> characteristics() {
        return new RemoveOnlyList<>(characteristics);
    }

    /**
     * Returns the roles that the attribute play in the pre-defined operations managed by {@code AttributeTypeBuilder}.
     * The set returned by this method is <cite>live</cite>: additions or removal on that set are reflected back on
     * this builder, and conversely.
     *
     * @return the roles that the attribute play in the pre-defined operations managed by {@code AttributeTypeBuilder}.
     */
    public Set<AttributeRole> roles() {
        return new SetOfUnknownSize<AttributeRole>() {
            @Override public Iterator<AttributeRole> iterator() {return new RoleIter();}
            @Override public boolean add(AttributeRole role)    {return addRole(role);}
        };
    }

    /**
     * The iterator returned by the {@link AttributeTypeBuilder#roles()} set.
     */
    private final class RoleIter extends AbstractIterator<AttributeRole> {
        /**
         * Index of the next {@code AttributeRole} to return.
         */
        private int index;

        /**
         * Prepares the next {@code AttributeRole} on which to iterate and returns
         * {@code true} if such {@code AttributeRole} has been found.
         */
        @Override
        @SuppressWarnings("fallthrough")
        public boolean hasNext() {
            if (next == null) {
                switch (index) {
                    case 0: {
                        if (isIdentifier) {
                            next = AttributeRole.IDENTIFIER_COMPONENT;
                            break;
                        }
                        index++;        // Fall through for testing the case for next 'index' value.
                    }
                    case 1: {
                        if (owner().defaultGeometry == AttributeTypeBuilder.this) {
                            next = AttributeRole.DEFAULT_GEOMETRY;
                            break;
                        }
                        index++;        // Fall through for testing the case for next 'index' value.
                    }
                    default: {
                        return false;
                    }
                }
                index++;
            }
            return true;
        }

        /**
         * Removes the element returned by the last {@link #next()} method.
         */
        @Override
        public void remove() {
            switch (index) {
                case 1: isIdentifier = false; break;
                case 2: owner().defaultGeometry = null; break;
                default: throw new IllegalStateException();
            }
        }
    }

    /**
     * Flags this attribute as an input of one of the pre-defined operations managed by {@code AttributeTypeBuilder}.
     * Invoking this method is equivalent to invoking <code>{@linkplain #roles()}.add(role)</code>.
     *
     * @param  role the role to add to the attribute (shall not be null).
     * @return {@code true} if the given role has been added to the attribute.
     */
    public boolean addRole(final AttributeRole role) {
        final FeatureTypeBuilder owner = owner();
        ensureNonNull("role", role);
        switch (role) {
            case IDENTIFIER_COMPONENT: {
                if (!isIdentifier) {
                    isIdentifier = true;
                    owner.identifierCount++;
                    owner.clearCache();         // The change does not impact this attribute itself.
                    return true;
                }
                break;
            }
            case DEFAULT_GEOMETRY: {
                if (owner.defaultGeometry != this) {
                    if (!Geometries.isKnownType(valueClass)) {
                        throw new IllegalStateException(errors().getString(Errors.Keys.UnsupportedImplementation_1, valueClass));
                    }
                    if (owner.defaultGeometry != null) {
                        throw new IllegalStateException(resources().getString(Resources.Keys.PropertyAlreadyExists_2,
                                owner.getDisplayName(), AttributeConvention.GEOMETRY_PROPERTY));
                    }
                    owner.defaultGeometry = this;
                    owner.clearCache();         // The change does not impact this attribute itself.
                    return true;
                }
                break;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if {@link AttributeRole#IDENTIFIER_COMPONENT} has been associated to this attribute.
     */
    @Override
    boolean isIdentifier() {
        return isIdentifier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AttributeTypeBuilder<V> setDefinition(final CharSequence definition) {
        super.setDefinition(definition);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AttributeTypeBuilder<V> setDesignation(final CharSequence designation) {
        super.setDesignation(designation);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AttributeTypeBuilder<V> setDescription(final CharSequence description) {
        super.setDescription(description);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AttributeTypeBuilder<V> setDeprecated(final boolean deprecated) {
        super.setDeprecated(deprecated);
        return this;
    }

    /**
     * Appends a text inside the value returned by {@link #toString()}, before the closing bracket.
     */
    @Override
    final void toStringInternal(final StringBuilder buffer) {
        buffer.append(" : ").append(Classes.getShortName(valueClass));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() {
        if (isIdentifier) {
            isIdentifier = false;
            owner().identifierCount--;      // Owner should never be null since we set 'isIdentifier' to false.
        }
        super.remove();
    }

    /**
     * Builds the attribute type from the information specified to this builder.
     * If a type has already been built and this builder state has not changed since the type creation,
     * then the previously created {@code AttributeType} instance is returned.
     *
     * <div class="note"><b>Example:</b>
     * the following lines of code add a "name" attribute to a "City" feature, then get the corresponding
     * {@code AttributeType<String>} instance. If no setter method is invoked on the builder of the "name"
     * attribute after those lines, then the {@code name} variable below will reference the same instance
     * than the "name" attribute in the {@code city} type.
     *
     * {@preformat java
     *   FeatureTypeBuilder builder = new FeatureTypeBuilder().setName("City");
     *   AttributeType<String> name = builder.addAttribute(String.class).setName("name").build();
     *   FeatureType city = builder.build();
     *
     *   assert city.getProperty("name") == name : "AttributeType instance should be the same.";
     * }
     *
     * Note that {@code city.getProperty("name")} returns {@code AttributeType<?>},
     * i.e. the {@linkplain #getValueClass() value class} is lost at compile-time.
     * By comparison, this {@code build()} method has a more accurate return type.
     * </div>
     *
     * @return the attribute type.
     */
    @Override
    public AttributeType<V> build() {
        if (property == null) {
            final AttributeType<?>[] chrts = new AttributeType<?>[characteristics.size()];
            for (int i=0; i<chrts.length; i++) {
                chrts[i] = characteristics.get(i).build();
            }
            property = new DefaultAttributeType<>(identification(), valueClass, minimumOccurs, maximumOccurs, defaultValue, chrts);
        }
        return property;
    }
}
