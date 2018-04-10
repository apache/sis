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
import static org.apache.sis.feature.AbstractIdentifiedType.*;
import org.opengis.feature.AttributeType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.GenericName;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.system.DefaultFactories;
import org.opengis.util.NameFactory;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class SingleAttributeTypeBuilder {

    private final Map<String,Object> parameters = new HashMap<>();
    private final List<AttributeType<?>> atts = new ArrayList<>();
    private Class<?> valueClass;
    private int minimumOccurs;
    private int maximumOccurs;
    private Object defaultValue;

    public SingleAttributeTypeBuilder() {
        minimumOccurs = 1;
        maximumOccurs = 1;
    }

    public SingleAttributeTypeBuilder reset(){
        parameters.clear();
        atts.clear();
        valueClass = null;
        minimumOccurs = 1;
        maximumOccurs = 1;
        defaultValue = null;
        return this;
    }

    public SingleAttributeTypeBuilder copy(AttributeType<?> base){
        setName(base.getName());
        setDefinition(base.getDefinition());
        setDescription(base.getDescription());
        setDesignation(base.getDesignation());
        atts.addAll(base.characteristics().values());
        valueClass = base.getValueClass();
        minimumOccurs = base.getMinimumOccurs();
        maximumOccurs = base.getMaximumOccurs();
        defaultValue = base.getDefaultValue();
        return this;
    }

    public SingleAttributeTypeBuilder setName(String localPart){
        this.setName(null,localPart);
        return this;
    }

    public SingleAttributeTypeBuilder setName(String namespace, String localPart){
        final GenericName name;
        if (namespace == null || namespace.isEmpty()) {
            name = DefaultFactories.forBuildin(NameFactory.class).createGenericName(null, localPart);
        } else {
            name = DefaultFactories.forBuildin(NameFactory.class).createGenericName(null, namespace, localPart);
        }
        setName(name);
        return this;
    }

    public SingleAttributeTypeBuilder setName(GenericName name) {
        parameters.put(DefaultAttributeType.NAME_KEY, name);
        return this;
    }

    public GenericName getName(){
        return GenericName.class.cast(parameters.get(DefaultAttributeType.NAME_KEY));
    }

    public SingleAttributeTypeBuilder setDescription(CharSequence description){
        parameters.put(DESCRIPTION_KEY, description);
        return this;
    }

    public CharSequence getDescription(){
        return CharSequence.class.cast(parameters.get(DESCRIPTION_KEY));
    }

    public SingleAttributeTypeBuilder setDesignation(CharSequence designation){
        parameters.put(DESIGNATION_KEY, designation);
        return this;
    }

    public CharSequence getDesignation(){
        return CharSequence.class.cast(parameters.get(DESIGNATION_KEY));
    }

    public SingleAttributeTypeBuilder setDefinition(CharSequence definition){
        parameters.put(DEFINITION_KEY, definition);
        return this;
    }

    public CharSequence getDefinition(){
        return CharSequence.class.cast(parameters.get(DEFINITION_KEY));
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
     * Set maximum string length
     * @param length
     * @return created attribute
     */
    public SingleAttributeTypeBuilder setLength(int length){
        return addCharacteristic(AttributeConvention.MAXIMAL_LENGTH_CHARACTERISTIC, Integer.class, 0, 1, length);
    }

    public SingleAttributeTypeBuilder setCRS(CoordinateReferenceSystem crs){
        return addCharacteristic(AttributeConvention.CRS_CHARACTERISTIC, CoordinateReferenceSystem.class, 0, 1, crs);
    }

    public SingleAttributeTypeBuilder setPossibleValues(Collection values){
        return addCharacteristic(AttributeConvention.VALID_VALUES_CHARACTERISTIC, Object.class, 0, 1, values);
    }

    public SingleAttributeTypeBuilder addCharacteristic(String localPart, Class valueClass, int minimumOccurs, int maximumOccurs, Object defaultValue) {
        final GenericName name = DefaultFactories.forBuildin(NameFactory.class).createGenericName(null, localPart);
        return addCharacteristic(name,valueClass,minimumOccurs,maximumOccurs,defaultValue);
    }

    public SingleAttributeTypeBuilder addCharacteristic(GenericName name, Class valueClass, int minimumOccurs, int maximumOccurs, Object defaultValue) {
        return addCharacteristic(new DefaultAttributeType(
                    Collections.singletonMap(NAME_KEY, name),
                    valueClass,minimumOccurs,maximumOccurs,defaultValue));
    }

    public SingleAttributeTypeBuilder addCharacteristic(AttributeType characteristic){
        //search and remove previous characteristic with the same id if it exist
        for (AttributeType at : atts) {
            if (at.getName().equals(characteristic.getName())) {
                atts.remove(at);
                break;
            }
        }
        atts.add(characteristic);
        return this;
    }

    public AttributeType build(){
        return new DefaultAttributeType(parameters, valueClass,
                minimumOccurs, maximumOccurs,
                defaultValue, atts.toArray(new AttributeType[atts.size()]));
    }

    public static AttributeType create(GenericName name, Class valueClass) {
        return new DefaultAttributeType(Collections.singletonMap("name", name), valueClass, 1, 1, null);
    }

}
