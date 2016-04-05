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

import com.esri.core.geometry.Geometry;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.util.Static;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Operation;
import org.opengis.feature.PropertyType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;

/**
 * Features are basically improved java Maps, this convention helps keeping
 * a coherency for the most common use cases.
 *
 *
 * @author Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class AttributeConvention extends Static {

    /**
     * Scope used by SIS for convention properties.
     */
    private static final String SIS_SCOPE = "http://sis.apache.org/feature";

    /**
     * Convention name of the feature identifier attribute if there is one in the feature type.
     * <br>
     * This attribute should be an Operation which aggregate other attributes.
     */
    public static final GenericName ATTRIBUTE_ID;
    /**
     * Convention name of the default geometry attribute if there is one in the feature type.
     * <br>
     * This attribute should be an Operation acting as a redirection to another attribute.
     */
    public static final GenericName ATTRIBUTE_DEFAULT_GEOMETRY;
    /**
     * Convention name of the feature bounds.
     * <br>
     * This attribute should be an Operation.
     * <br>
     * Most feature types have a single geometry but in case several geometries
     * exist the value returned is the concatenation of all first depth geometries.
     */
    public static final GenericName ATTRIBUTE_BOUNDS;
    /**
     * Attribute types which store geometries or coverages can have a CoordinateReferenceSystem defined.
     * <br>
     * The crs information is stored as default value in the AttributeTypes with this name
     * of the geometry attribute type.
     */
    public static final GenericName CHARACTERISTIC_CRS;
    /**
     * Attribute type which store the string maximum length.
     */
    public static final GenericName CHARACTERISTIC_LENGTH;
    /**
     * Attribute type which store the enumeration of possible values.
     */
    public static final GenericName CHARACTERISTIC_ENUM;

    static {
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
        ATTRIBUTE_ID = factory.createGenericName(null, SIS_SCOPE,  "@id");
        ATTRIBUTE_DEFAULT_GEOMETRY = factory.createGenericName(null, SIS_SCOPE,  "@defaultgeom");
        ATTRIBUTE_BOUNDS = factory.createGenericName(null, SIS_SCOPE,  "@bounds");
        CHARACTERISTIC_CRS = factory.createGenericName(null, SIS_SCOPE,  "@crs");
        CHARACTERISTIC_LENGTH = factory.createGenericName(null, SIS_SCOPE,  "@length");
        CHARACTERISTIC_ENUM = factory.createGenericName(null, SIS_SCOPE,  "@enum");
    }

    /**
     * Convention properties are properties added to the real feature type
     * to offer access to common informations.
     * <br>
     * Such properties should not be stored when writing, those are calculate
     * most of the time and have only a meaning within SIS.
     *
     * @param property tested property, not null
     * @return true if property is a convention property.
     */
    public static boolean isConventionProperty(PropertyType property){
        return property.getName().toString().startsWith(SIS_SCOPE);
    }
    
    /**
     * Indicate of given PropertyType is a geometry type.
     * <br>
     * This implies property is an attribute and the value class is a type of Geometry.
     *
     * @param propertyType tested property type, not null
     * @return true if property type is for geometries
     */
    public static boolean isGeometryAttribute(PropertyType propertyType){
        if(propertyType instanceof AttributeType){
            return Geometry.class.isAssignableFrom(((AttributeType)propertyType).getValueClass());
        }else{
            return false;
        }
    }

    /**
     * Extract CRS characteristic if it exist.
     * 
     * @param type tested property type, not null
     * @return CoordinateReferenceSystem or null
     */
    public static CoordinateReferenceSystem getCRSCharacteristic(PropertyType type){
        while(type instanceof Operation){
            type = (PropertyType) ((Operation)type).getResult();
        }
        if(type instanceof AttributeType){
            final AttributeType at = (AttributeType) ((AttributeType)type).characteristics().get(CHARACTERISTIC_CRS.toString());
            if(at!=null){
                return (CoordinateReferenceSystem) at.getDefaultValue();
            }
        }
        return null;
    }

    /**
     * Extract field length characteristic if it exist.
     *
     * @param type tested property type, not null
     * @return Length or null
     */
    public static Integer getLengthCharacteristic(AttributeType type){
        final AttributeType at = (AttributeType) type.characteristics().get(CHARACTERISTIC_LENGTH.toString());
        if(at!=null){
            return (Integer) at.getDefaultValue();
        }
        return null;
    }

}
