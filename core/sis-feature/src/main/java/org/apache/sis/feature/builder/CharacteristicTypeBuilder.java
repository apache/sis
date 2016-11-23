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

import org.opengis.util.GenericName;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;

// Branch-dependent imports
import java.util.Objects;
import org.opengis.feature.AttributeType;


/**
 * Describes one characteristic of the {@code AttributeType} will will be built by a {@code FeatureTypeBuilder}.
 * Characteristics can describe additional information useful for interpreting an attribute value, like
 * the units of measurement and uncertainties of a numerical value, or the coordinate reference system
 * (CRS) of a geometry.
 *
 * <p>In many cases, all instances of the same {@code AttributeType} have the same characteristics.
 * For example all values of the "temperature" attribute typically have the same units of measurement.
 * Such common value can be specified as the characteristic {@linkplain #setDefaultValue(Object) default value}.</p>
 *
 * @param <V> the class of characteristic values.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 *
 * @see AttributeTypeBuilder#addCharacteristic(Class)
 */
public final class CharacteristicTypeBuilder<V> extends TypeBuilder {
    /**
     * The attribute type builder instance that created this {@code CharacteristicTypeBuilder} builder.
     * This is set at construction time and considered as immutable until it is set to {@code null}.
     */
    private AttributeTypeBuilder<?> owner;

    /**
     * The class of characteristic values. Can not be changed after construction
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
     * Creates a new builder initialized to the values of the given builder but a different type.
     * This constructor is for {@link #setValueClass(Class)} implementation only.
     *
     * @throws UnconvertibleObjectException if the default value can not be converted to the given class.
     */
    private CharacteristicTypeBuilder(final CharacteristicTypeBuilder<?> builder, final Class<V> valueClass)
            throws UnconvertibleObjectException
    {
        super(builder);
        this.owner        = builder.owner;
        this.valueClass   = valueClass;
        this.defaultValue = ObjectConverters.convert(builder.defaultValue, valueClass);
        // Do not copy the 'characteristic' reference since the 'valueClass' is different.
    }

    /**
     * Creates a new characteristic builder for values of the given class.
     *
     * @param owner      the builder of the {@code AttributeType} for which to add this property.
     * @param valueClass the class of characteristic values.
     */
    CharacteristicTypeBuilder(final AttributeTypeBuilder<?> owner, final Class<V> valueClass) {
        super(null, owner.getLocale());
        this.owner = owner;
        this.valueClass = valueClass;
    }

    /**
     * Creates a new characteristic builder initialized to the values of an existing attribute.
     *
     * @param owner  the builder of the {@code AttributeType} for which to add this property.
     */
    CharacteristicTypeBuilder(final AttributeTypeBuilder<?> owner, final AttributeType<V> template) {
        super(template, owner.getLocale());
        this.owner     = owner;
        characteristic = template;
        valueClass     = template.getValueClass();
        defaultValue   = template.getDefaultValue();
    }

    /**
     * If the {@code AttributeType<V>} created by the last call to {@link #build()} has been cached,
     * clears that cache. This method must be invoked every time that a setter method is invoked.
     */
    @Override
    final void clearCache() {
        characteristic = null;
        ensureAlive(owner);
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
     * Sets the characteristic name as a generic name.
     * If another name was defined before this method call, that previous value will be discarded.
     *
     * @return {@code this} for allowing method calls chaining.
     */
    @Override
    public CharacteristicTypeBuilder<V> setName(final GenericName name) {
        super.setName(name);
        return this;
    }

    /**
     * Sets the characteristic name as a simple string with the default scope.
     * The default scope is the value specified by the last call to
     * {@link FeatureTypeBuilder#setDefaultScope(String)}.
     * The name will be a {@linkplain org.apache.sis.util.iso.DefaultLocalName local name} if no default scope
     * has been specified, or a {@linkplain org.apache.sis.util.iso.DefaultScopedName scoped name} otherwise.
     *
     * @return {@code this} for allowing method calls chaining.
     */
    @Override
    public CharacteristicTypeBuilder<V> setName(final String localPart) {
        super.setName(localPart);
        return this;
    }

    /**
     * Sets the characteristic name as a string in the given scope.
     * The name will be a {@linkplain org.apache.sis.util.iso.DefaultLocalName local name} if the given scope is
     * {@code null} or empty, or a {@linkplain org.apache.sis.util.iso.DefaultScopedName scoped name} otherwise.
     * If a {@linkplain FeatureTypeBuilder#setDefaultScope(String) default scope} has been specified, then the
     * {@code scope} argument overrides it.
     *
     * @return {@code this} for allowing method calls chaining.
     */
    @Override
    public CharacteristicTypeBuilder<V> setName(final String scope, final String localPart) {
        super.setName(scope, localPart);
        return this;
    }

    /**
     * Delegates the creation of a new name to the enclosing builder.
     */
    @Override
    final GenericName name(final String scope, final String localPart) {
        ensureAlive(owner);
        return owner.name(scope, localPart);
    }

    /**
     * Returns the class of characteristic values.
     *
     * @return the class of characteristic values.
     *
     * @see #setValueClass(Class)
     */
    public Class<V> getValueClass() {
        return valueClass;
    }

    /**
     * Sets the class of characteristic values. Callers <strong>must</strong> use the builder returned by this method
     * instead of {@code this} builder after this method call, since the returned builder may be a new instance.
     *
     * @param  <N>   the compile-time value of the {@code type} argument.
     * @param  type  the new class of characteristic values.
     * @return the characteristic builder — <em>not necessarily this instance.</em>
     * @throws UnconvertibleObjectException if the {@linkplain #getDefaultValue() default value}
     *         can not be converted to the given {@code <N>} class.
     *
     * @see #getValueClass()
     */
    @SuppressWarnings("unchecked")
    public <N> CharacteristicTypeBuilder<N> setValueClass(final Class<N> type) throws UnconvertibleObjectException {
        ensureAlive(owner);
        ensureNonNull("type", type);
        if (type == valueClass) {
            return (CharacteristicTypeBuilder<N>) this;
        }
        final CharacteristicTypeBuilder<N> newb = new CharacteristicTypeBuilder<>(this, type);
        owner.characteristics.set(owner.characteristics.lastIndexOf(this), newb);
        // Note: a negative lastIndexOf(old) would be a bug in our algorithm.
        owner = null;
        return newb;
    }

    /**
     * Returns the default value for the characteristic, or {@code null} if none.
     *
     * @return the default characteristic value, or {@code null} if none.
     *
     * @see #setDefaultValue(Object)
     */
    public V getDefaultValue() {
        return defaultValue;
    }

    /**
     * Sets the default value for the characteristic.
     *
     * @param  value  characteristic default value, or {@code null} if none.
     * @return {@code this} for allowing method calls chaining.
     *
     * @see #getDefaultValue()
     */
    public CharacteristicTypeBuilder<V> setDefaultValue(final V value) {
        if (!Objects.equals(defaultValue, value)) {
            defaultValue = value;
            clearCache();
        }
        return this;
    }

    /**
     * Sets the default value with check of the value class.
     */
    final void set(final Object value) {
        setDefaultValue(valueClass.cast(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharacteristicTypeBuilder<V> setDefinition(final CharSequence definition) {
        super.setDefinition(definition);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharacteristicTypeBuilder<V> setDesignation(final CharSequence designation) {
        super.setDesignation(designation);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharacteristicTypeBuilder<V> setDescription(final CharSequence description) {
        super.setDescription(description);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharacteristicTypeBuilder<V> setDeprecated(final boolean deprecated) {
        super.setDeprecated(deprecated);
        return this;
    }

    /**
     * Builds the characteristic type from the information specified to this builder.
     * If a type has already been built and this builder state has not changed since the type creation,
     * then the previously created {@code AttributeType} instance is returned.
     *
     * @return the characteristic type.
     */
    @Override
    public AttributeType<V> build() {
        if (characteristic == null) {
            characteristic = new DefaultAttributeType<>(identification(), valueClass, 0, 1, defaultValue);
        }
        return characteristic;
    }

    /**
     * Sets a new owner.
     */
    final void owner(final AttributeTypeBuilder<?> newb) {
        owner = newb;
    }

    /**
     * Removes this characteristics from the {@code AttributeTypeBuilder}.
     * After this method has been invoked, this {@code CharacteristicTypeBuilder} instance
     * is no longer in the list returned by {@link AttributeTypeBuilder#characteristics()}
     * and attempts to invoke any setter method on {@code this} will cause an
     * {@link IllegalStateException} to be thrown.
     */
    @Override
    public void remove() {
        if (owner != null) {
            owner.characteristics.remove(owner.characteristics.lastIndexOf(this));
            // Note: a negative lastIndexOf(old) would be a bug in our algorithm.
            owner.clearCache();
            owner = null;
        }
    }
}
