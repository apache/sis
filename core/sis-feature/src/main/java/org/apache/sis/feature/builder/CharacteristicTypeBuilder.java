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

// Branch-dependent imports
import java.util.Objects;


/**
 * Describes one characteristic of an {@code AttributeType} to be built by the enclosing {@code FeatureTypeBuilder}.
 * A different instance of {@code CharacteristicTypeBuilder} exists for each characteristic to describe.
 * Those instances are created by:
 *
 * <ul>
 *   <li>{@link AttributeTypeBuilder#addCharacteristic(Class)}</li>
 * </ul>
 *
 * @param <V> the class of characteristic values.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public final class CharacteristicTypeBuilder<V> extends TypeBuilder {
    /**
     * The attribute type builder instance that created this {@code CharacteristicTypeBuilder} builder.
     */
    private final AttributeTypeBuilder<?> owner;

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
    private transient DefaultAttributeType<V> characteristic;

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
    CharacteristicTypeBuilder(final AttributeTypeBuilder<?> owner, final DefaultAttributeType<V> template) {
        super(template, owner.getLocale());
        this.owner     = owner;
        valueClass     = template.getValueClass();
        defaultValue   = template.getDefaultValue();
        characteristic = template;
    }

    /**
     * If the {@code AttributeType<V>} created by the last call to {@link #build()} has been cached,
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
    public CharacteristicTypeBuilder<V> setDefaultValue(final V value) {
        if (!Objects.equals(defaultValue, value)) {
            defaultValue = value;
            clearCache();
        }
        return this;
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
     * Creates a new characteristic from the current setting.
     */
    final DefaultAttributeType<V> build() {
        if (characteristic == null) {
            characteristic = new DefaultAttributeType<V>(identification(), valueClass, 0, 1, defaultValue);
        }
        return characteristic;
    }
}
