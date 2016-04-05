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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.sis.feature.DefaultAttributeType;
import static org.apache.sis.feature.AbstractIdentifiedType.*;
import static org.apache.sis.internal.feature.AttributeConvention.*;
import org.apache.sis.internal.system.DefaultFactories;
import org.opengis.feature.AttributeType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;

/**
 * Helper class for the creation of {@link AttributeType} instances.
 * This builder can create the parameters to be given to {@linkplain DefaultAttributeType#DefaultAttributeType(
 * java.util.Map, java.lang.Class, int, int, java.lang.Object, org.opengis.feature.AttributeType...)  attribute type constructor}
 * from simpler parameters given to this builder.
 * 
 * @author Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public class AttributeTypeBuilder {

    private final Map parameters = new HashMap();
    private final List<AttributeType> atts = new ArrayList<>();
    private Class valueClass = Object.class;
    private int minimumOccurs = 1;
    private int maximumOccurs = 1;
    private Object defaultValue = null;

    /**
     * Reset builder parameters to there original values.
     */
    public void reset(){
        parameters.clear();
        atts.clear();
        valueClass = Object.class;
        minimumOccurs = 1;
        maximumOccurs = 1;
        defaultValue = null;
    }

    /**
     * Copy parameters from the given attribute type.
     *
     * @param type attribute type to copy parameters from.
     */
    public void copy(AttributeType type){
        setName(type.getName());
        setDefinition(type.getDefinition());
        setDescription(type.getDescription());
        setDesignation(type.getDesignation());
        atts.addAll(type.characteristics().values());
        valueClass = type.getValueClass();
        minimumOccurs = type.getMinimumOccurs();
        maximumOccurs = type.getMaximumOccurs();
        defaultValue = type.getDefaultValue();
    }

    /**
     * Set attribute type name.
     *
     * @param localPart generic name tip part, not null
     */
    public void setName(String localPart){
        this.setName(null,localPart);
    }

    /**
     * Set attribute type name.
     *
     * @param scope generic name scope part, can be null
     * @param localPart generic name tip part, not null
     */
    public void setName(String scope, String localPart){
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
        if(scope==null){
            setName(factory.createGenericName(null, localPart));
        }else{
            setName(factory.createGenericName(null, scope,  localPart));
        }
    }

    /**
     * Set attribute type name.
     *
     * See {@link #NAME_KEY}
     *
     * @param name generic name, not null
     */
    public void setName(GenericName name) {
        parameters.put(NAME_KEY, name);
    }

    /**
     * Set attribute description.
     *
     * See {@link #DESCRIPTION_KEY}
     *
     * @param description
     */
    public void setDescription(CharSequence description){
        parameters.put(DESCRIPTION_KEY, description);
    }

    /**
     * Set attribute designation.
     *
     * See {@link #DESIGNATION_KEY}
     *
     * @param designation
     */
    public void setDesignation(CharSequence designation){
        parameters.put(DESIGNATION_KEY, designation);
    }

    /**
     * Set attribute definition.
     *
     * See {@link #DEFINITION_KEY}
     *
     * @param definition
     */
    public void setDefinition(CharSequence definition){
        parameters.put(DEFINITION_KEY, definition);
    }

    /**
     * Set attribute value class.
     *
     * @param valueClass not null
     */
    public void setValueClass(Class valueClass) {
        this.valueClass = valueClass;
    }

    /**
     * Set default attribute value.
     *
     * @param defaultValue
     */
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Set minimum occurrences of the attribute values.
     *
     * @param minimumOccurs
     */
    public void setMinimumOccurs(int minimumOccurs) {
        this.minimumOccurs = minimumOccurs;
    }

    /**
     * Set maximum occurrences of the attribute values.
     *
     * @param maximumOccurs
     */
    public void setMaximumOccurs(int maximumOccurs) {
        this.maximumOccurs = maximumOccurs;
    }

    /**
     * Set maximum attribute length.
     * This characteristic only have a meaning with CharSequence type attributes.
     *
     * @param length 
     * @return created characteristic
     */
    public AttributeType setLengthCharacteristic(int length){
        return addCharacteristic(CHARACTERISTIC_LENGTH, Integer.class, 1, 1, length);
    }

    /**
     * Set attribute {@code CoordinateReferenceSystem}.
     * This characteristic only have a meaning with georeferenced type attributes.
     * 
     * @param crs
     * @return created characteristic
     */
    public AttributeType setCRSCharacteristic(CoordinateReferenceSystem crs){
        return addCharacteristic(CHARACTERISTIC_CRS, CoordinateReferenceSystem.class, 1, 1, crs);
    }

    /**
     * Set attribute restricted values.
     * This characteristic defines a list of possible values for the attribute.
     *
     * @param values
     * @return created characteristic
     */
    public AttributeType setPossibleValues(Collection values){
        return addCharacteristic(CHARACTERISTIC_ENUM, Object.class, 1, 1, values);
    }

    /**
     * Add a user defined characteristic.
     *
     * @param localPart generic name tip part, not null
     * @param valueClass characteristic value class
     * @param minimumOccurs characteristic minimum number of occurrences
     * @param maximumOccurs characteristic maximum number of occurrences
     * @param defaultValue characteristic default value
     * @return created characteristic
     */
    public AttributeType addCharacteristic(String localPart, Class valueClass, int minimumOccurs, int maximumOccurs, Object defaultValue){
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
        final GenericName name = factory.createGenericName(null, localPart);
        return addCharacteristic(name,valueClass,minimumOccurs,maximumOccurs,defaultValue);
    }

    /**
     * Add a user defined characteristic.
     *
     * @param name characteristic name
     * @param valueClass characteristic value class
     * @param minimumOccurs characteristic minimum number of occurrences
     * @param maximumOccurs characteristic maximum number of occurrences
     * @param defaultValue characteristic default value
     * @return created characteristic
     */
    public AttributeType addCharacteristic(GenericName name, Class valueClass, int minimumOccurs, int maximumOccurs, Object defaultValue){
        return addCharacteristic(new DefaultAttributeType(
                    Collections.singletonMap(NAME_KEY, name),
                    valueClass,minimumOccurs,maximumOccurs,defaultValue));
    }

    /**
     * Add a user defined characteristic.
     *
     * @param characteristic
     * @return added characteristic
     */
    public AttributeType addCharacteristic(AttributeType characteristic){
        //search and remove previous characteristic with the same id if it exist
        for(AttributeType at : atts){
            if(at.getName().equals(characteristic.getName())){
                atts.remove(at);
                break;
            }
        }
        atts.add(characteristic);
        return characteristic;
    }

    /**
     * Create the attribute type.
     *
     * @return AtributeType, never null
     */
    public AttributeType build(){
        return new DefaultAttributeType(parameters, valueClass, 
                minimumOccurs, maximumOccurs,
                defaultValue, atts.toArray(new AttributeType[atts.size()]));
    }

}
