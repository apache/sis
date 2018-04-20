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
package org.apache.sis.internal.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.sis.feature.DefaultAttributeType;
import org.opengis.feature.AttributeType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.GenericName;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.system.DefaultFactories;
import org.opengis.util.NameFactory;

import static org.apache.sis.feature.AbstractIdentifiedType.*;


/**
 * Builder for a single {@link AttributeType}.
 * This is an alternative to {@link org.apache.sis.feature.builder.PropertyTypeBuilder}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 *
 * @todo Is this class really needed?
 */
public class SingleAttributeTypeBuilder {
    /**
     * Properties (name, description, …) to give to the attribute type constructor.
     */
    private final Map<String,Object> properties;

    /**
     * Optional characteristics of the attribute.
     */
    private final List<AttributeType<?>> characteristics;

    /**
     * The class of the attribute.
     */
    private Class<?> valueClass;

    /**
     * Minimum number of occurrences.
     */
    private int minimumOccurs;

    /**
     * Maximum number of occurrences.
     */
    private int maximumOccurs;

    /**
     * The default value, or {@code null} if none.
     */
    private Object defaultValue;

    /**
     * Creates a new attribute type builder.
     */
    public SingleAttributeTypeBuilder() {
        properties      = new HashMap<>();
        characteristics = new ArrayList<>();
        minimumOccurs   = 1;
        maximumOccurs   = 1;
    }

    /**
     * Restores this builder to its initial state.
     * This method can be invoked before to build another attribute.
     *
     * @return {@code this} for method call chaining.
     */
    public SingleAttributeTypeBuilder reset(){
        properties.clear();
        characteristics.clear();
        valueClass    = null;
        minimumOccurs = 1;
        maximumOccurs = 1;
        defaultValue  = null;
        return this;
    }

    /**
     * Sets this builder to the same properties then the given attribute type.
     *
     * @param  template  the attribute type to use as a template.
     * @return {@code this} for method call chaining.
     */
    public SingleAttributeTypeBuilder copy(AttributeType<?> template) {
        reset();
        setName       (template.getName());
        setDefinition (template.getDefinition());
        setDescription(template.getDescription());
        setDesignation(template.getDesignation());
        characteristics.addAll(template.characteristics().values());
        valueClass    = template.getValueClass();
        minimumOccurs = template.getMinimumOccurs();
        maximumOccurs = template.getMaximumOccurs();
        defaultValue  = template.getDefaultValue();
        return this;
    }

    public SingleAttributeTypeBuilder setName(String localPart) {
        return setName(null, localPart);
    }

    public SingleAttributeTypeBuilder setName(String namespace, String localPart) {
        final GenericName name;
        if (namespace == null || namespace.isEmpty()) {
            name = DefaultFactories.forBuildin(NameFactory.class).createGenericName(null, localPart);
        } else {
            name = DefaultFactories.forBuildin(NameFactory.class).createGenericName(null, namespace, localPart);
        }
        return setName(name);
    }

    public SingleAttributeTypeBuilder setName(GenericName name) {
        properties.put(DefaultAttributeType.NAME_KEY, name);
        return this;
    }

    public GenericName getName() {
        return (GenericName) properties.get(DefaultAttributeType.NAME_KEY);
    }

    public SingleAttributeTypeBuilder setDescription(CharSequence description) {
        properties.put(DESCRIPTION_KEY, description);
        return this;
    }

    public CharSequence getDescription() {
        return (CharSequence) properties.get(DESCRIPTION_KEY);
    }

    public SingleAttributeTypeBuilder setDesignation(CharSequence designation){
        properties.put(DESIGNATION_KEY, designation);
        return this;
    }

    public CharSequence getDesignation(){
        return (CharSequence) properties.get(DESIGNATION_KEY);
    }

    public SingleAttributeTypeBuilder setDefinition(CharSequence definition){
        properties.put(DEFINITION_KEY, definition);
        return this;
    }

    public CharSequence getDefinition(){
        return (CharSequence) properties.get(DEFINITION_KEY);
    }

    public SingleAttributeTypeBuilder setValueClass(Class<?> valueClass) {
        this.valueClass = valueClass;
        return this;
    }

    public Class<?> getValueClass(){
        return valueClass;
    }

    public SingleAttributeTypeBuilder setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public SingleAttributeTypeBuilder setMinimumOccurs(int minimumOccurs) {
        this.minimumOccurs = minimumOccurs;
        return this;
    }

    public int getMinimumOccurs() {
        return minimumOccurs;
    }

    public SingleAttributeTypeBuilder setMaximumOccurs(int maximumOccurs) {
        this.maximumOccurs = maximumOccurs;
        return this;
    }

    public int getMaximumOccurs() {
        return maximumOccurs;
    }

    /**
     * Set maximum string length.
     *
     * @param  length maximal number of characters.
     * @return {@code this} for method call chaining.
     */
    public SingleAttributeTypeBuilder setLength(int length) {
        return addCharacteristic(AttributeConvention.MAXIMAL_LENGTH_CHARACTERISTIC, Integer.class, 0, 1, length);
    }

    public SingleAttributeTypeBuilder setCRS(CoordinateReferenceSystem crs) {
        return addCharacteristic(AttributeConvention.CRS_CHARACTERISTIC, CoordinateReferenceSystem.class, 0, 1, crs);
    }

    public SingleAttributeTypeBuilder setPossibleValues(Collection<?> values) {
        return addCharacteristic(AttributeConvention.VALID_VALUES_CHARACTERISTIC, Object.class, 0, 1, values);
    }

    public SingleAttributeTypeBuilder addCharacteristic(String localPart, Class<?> valueClass, int minimumOccurs, int maximumOccurs, Object defaultValue) {
        final GenericName name = DefaultFactories.forBuildin(NameFactory.class).createGenericName(null, localPart);
        return addCharacteristic(name,valueClass,minimumOccurs,maximumOccurs,defaultValue);
    }

    public SingleAttributeTypeBuilder addCharacteristic(GenericName name, Class<?> valueClass, int minimumOccurs, int maximumOccurs, Object defaultValue) {
        return addCharacteristic(new DefaultAttributeType(
                    Collections.singletonMap(NAME_KEY, name),
                    valueClass,minimumOccurs,maximumOccurs,defaultValue));
    }

    public SingleAttributeTypeBuilder addCharacteristic(AttributeType<?> characteristic) {
        //search and remove previous characteristic with the same id if it exist
        for (AttributeType<?> at : characteristics) {
            if (at.getName().equals(characteristic.getName())) {
                characteristics.remove(at);
                break;
            }
        }
        characteristics.add(characteristic);
        return this;
    }

    public AttributeType<?> build(){
        return new DefaultAttributeType(properties, valueClass,
                minimumOccurs, maximumOccurs,
                defaultValue, characteristics.toArray(new AttributeType[characteristics.size()]));
    }

    public static AttributeType<?> create(GenericName name, Class<?> valueClass) {
        return new DefaultAttributeType(Collections.singletonMap("name", name), valueClass, 1, 1, null);
    }
}
