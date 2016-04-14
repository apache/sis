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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.ScopedName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.feature.AbstractOperation;
import org.apache.sis.feature.DefaultAssociationRole;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.feature.FeatureOperations;
import org.apache.sis.internal.system.DefaultFactories;

import static org.apache.sis.internal.feature.AttributeConvention.*;
import static org.apache.sis.feature.DefaultFeatureType.*;

// Branch-dependent imports
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.FeatureType;
import org.opengis.feature.Operation;
import org.opengis.feature.PropertyType;


/**
 * Helper class for the creation of {@link FeatureType} instances.
 * This builder can create the parameters to be given to {@linkplain DefaultFeatureType feature type constructor}
 * from simpler parameters given to this builder.
 *
 * @author  Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public class FeatureTypeBuilder {

    private final Map<GenericName,PropertyType> properties = new LinkedHashMap<>();
    private final List<FeatureType> superTypes = new ArrayList<>();
    private boolean isAbstract = false;
    private final Map<String,Object> parameters = new HashMap<>();

    //convention ID
    private String idPrefix = null;
    private String idSeparator = null;
    private GenericName[] idAttributes = null;
    //convention default geometry
    private GenericName defGeomAttribute = null;

    /**
     * Reset builder parameters to there original values.
     */
    public void reset() {
        properties.clear();
        superTypes.clear();
        isAbstract = false;
        parameters.clear();
        idPrefix = null;
        idSeparator = null;
        idAttributes = null;
        defGeomAttribute = null;
    }

    /**
     * Copy parameters from the given feature type.
     *
     * @param type feature type to copy parameters from.
     */
    public void copy(final FeatureType type) {
        reset();
        setName(type.getName());
        setDescription(type.getDescription());
        setDefinition(type.getDefinition());
        setDesignation(type.getDesignation());
        setAbstract(type.isAbstract());
        setSuperTypes(type.getSuperTypes());

        for (PropertyType pt : type.getProperties(false)) {
            addProperty(pt);
        }
    }

    /**
     * Set feature type name.
     *
     * @param localPart generic name tip part, not null
     */
    public void setName(String localPart) {
        setName(create(localPart));
    }

    /**
     * Set feature type name.
     *
     * @param scope generic name scope part, can be null
     * @param localPart generic name tip part, not null
     */
    public void setName(String scope, String localPart) {
        setName(create(scope,localPart));
    }

    /**
     * Set feature type name.
     *
     * See {@link #NAME_KEY}
     *
     * @param name generic name, not null
     */
    public void setName(GenericName name) {
        parameters.put(NAME_KEY, name);
    }

    /**
     * Return the current feature type name.
     *
     * @return GenericName, can be null
     */
    public GenericName getName() {
        Object val = parameters.get(DefaultFeatureType.NAME_KEY);
        if (val instanceof GenericName) {
            return (GenericName) val;
        } else if (val instanceof String) {
            return valueOf((String) val);
        }
        return null;
    }

    /**
     * Set attribute description.
     *
     * See {@link #DESCRIPTION_KEY}
     *
     * @param description feature type description
     */
    public void setDescription(CharSequence description) {
        parameters.put(DESCRIPTION_KEY, description);
    }

    /**
     * Set attribute designation.
     *
     * See {@link #DESIGNATION_KEY}
     *
     * @param designation feature type designation
     */
    public void setDesignation(CharSequence designation) {
        parameters.put(DESIGNATION_KEY, designation);
    }

    /**
     * Set attribute definition.
     *
     * See {@link #DEFINITION_KEY}
     *
     * @param definition feature type definition
     */
    public void setDefinition(CharSequence definition) {
        parameters.put(DEFINITION_KEY, definition);
    }

    /**
     * Set feature type abstract.
     *
     * @param isAbstract whether the feature is abstract.
     */
    public void setAbstract(final boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    /**
     * Set parent types.
     * Feature type will inherit all parent properties.
     *
     * @param types not null
     */
    public void setSuperTypes(final Collection<? extends FeatureType> types) {
        superTypes.clear();
        superTypes.addAll(types);
    }

    /**
     * Set parent types.
     * Feature type will inherit all parent properties.
     *
     * @param types not null
     */
    public void setSuperTypes(final FeatureType... types) {
        superTypes.clear();
        superTypes.addAll(Arrays.asList(types));
    }

    /**
     * Define an id operation composed of the given attribute.
     * Generated id prefix will be the name of the featuretype+'.'
     *
     * @param attributeName attribute id used in the id operation
     */
    public void setIdOperation(final String attributeName) {
        setIdOperation(null, "-", create(attributeName));
    }

    /**
     * Define an id operation composed of the given attributes.
     * Generated id prefix will be the name of the featuretype+'.'
     *
     * @param attributes attributes used in the id operation
     */
    public void setIdOperation(final GenericName ... attributes) {
        setIdOperation(null, "-", attributes);
    }

    /**
     * Define an id operation composed of the given attribute.
     *
     * @param prefix generated id prefix
     * @param attributeName attribute id used in the id operation
     */
    public void setIdOperation(final String prefix, final String attributeName) {
        setIdOperation(prefix, "-", create(attributeName));
    }

    /**
     * Define an id operation composed of the given attributes.
     *
     * @param prefix generated id prefix
     * @param attributes attributes used in the id operation
     */
    public void setIdOperation(final String prefix, final GenericName ... attributes) {
        setIdOperation(prefix, "-", attributes);
    }

    /**
     * Define an id operation composed of the given attributes.
     *
     * @param prefix generated id prefix
     * @param separator generated id separator between attribute values
     * @param attributes attributes used in the id operation
     */
    public void setIdOperation(final String prefix, final String separator, final GenericName ... attributes) {
        idPrefix = prefix;
        idSeparator = separator;
        idAttributes = attributes;
        //add placeholder
        properties.put(ATTRIBUTE_ID, null);
    }

    /**
     * Define a default geometry link operation.
     *
     * @param attribute referenced attribute
     */
    public void setDefaultGeometryOperation(final String attribute) {
        setDefaultGeometryOperation(create(attribute));
    }

    /**
     * Define a default geometry link operation.
     *
     * @param scope referenced attribute name scope
     * @param localPart referenced name tip
     */
    public void setDefaultGeometryOperation(final String scope, final String localPart) {
        setDefaultGeometryOperation(create(scope,localPart));
    }

    /**
     * Define a default geometry link operation.
     *
     * @param attribute referenced attribute
     */
    public void setDefaultGeometryOperation(final GenericName attribute) {
        defGeomAttribute = attribute;
        //add placeholder
        properties.put(ATTRIBUTE_DEFAULT_GEOMETRY, null);
        properties.put(ATTRIBUTE_BOUNDS, null);
    }

    /**
     * Add a new property to the feature type.
     * Property will have a minimum and maximum occurrence of one, no characteristics
     * and no default value.
     *
     * @param localPart property generic name tip part
     * @param valueClass property value class
     * @return created property type
     */
    public <V> AttributeType<V> addProperty(final String localPart, final Class<V> valueClass) {
        return addProperty(create(localPart), valueClass);
    }

    /**
     * Add a new property to the feature type.
     * Property will have a minimum and maximum occurrence of one, no characteristics
     * and no default value.
     *
     * @param scope property generic name scope part
     * @param localPart property generic name tip part
     * @param valueClass property value class
     * @return created property type
     */
    public <V> AttributeType<V> addProperty(final String scope, final String localPart, final Class<V> valueClass) {
        return addProperty(create(scope, localPart), valueClass);
    }

    /**
     * Add a new property to the feature type.
     * Property will have a minimum and maximum occurrence of one, no characteristics
     * and no default value.
     *
     * @param name property name
     * @param valueClass property value class
     * @return created property type
     */
    public <V> AttributeType<V> addProperty(final GenericName name, final Class<V> valueClass) {
        return addProperty(name, valueClass, (V) null);
    }

    /**
     * Add a new property to the feature type.
     * Property will have a minimum and maximum occurrence of one, no default value
     * and the given {@code CoordinateReferenceSystem} characteristic.
     *
     * @param localPart property generic name tip part
     * @param valueClass property value class
     * @param crs property {@code CoordinateReferenceSystem} characteristic
     * @return created property type
     */
    public <V> AttributeType<V> addProperty(final String localPart, final Class<V> valueClass, final CoordinateReferenceSystem crs) {
        return addProperty(null, localPart, valueClass, crs);
    }

    /**
     * Add a new property to the feature type.
     * Property will have a minimum and maximum occurrence of one, no default value
     * and the given {@code CoordinateReferenceSystem} characteristic.
     *
     * @param scope property generic name scope part
     * @param localPart property generic name tip part
     * @param valueClass property value class
     * @param crs property {@code CoordinateReferenceSystem} characteristic
     * @return created property type
     */
    public <V> AttributeType<V> addProperty(final String scope, final String localPart,
            final Class<V> valueClass, final CoordinateReferenceSystem crs)
    {
        return addProperty(create(scope, localPart), valueClass, crs);
    }

    /**
     * Add a new property to the feature type.
     * Property will have a minimum and maximum occurrence of one, no default value
     * and the given {@code CoordinateReferenceSystem} characteristic.
     *
     * @param name property name
     * @param valueClass property value class
     * @param crs property {@code CoordinateReferenceSystem} characteristic
     * @return created property type
     */
    public <V> AttributeType<V> addProperty(final GenericName name, final Class<V> valueClass, final CoordinateReferenceSystem crs) {
        return addProperty(name, valueClass, null, crs);
    }

    /**
     * Add a new property to the feature type.
     * Property will have a minimum and maximum occurrence of one and no characteristics.
     *
     * @param scope property generic name scope part
     * @param localPart property generic name tip part
     * @param valueClass property value class
     * @param defaultValue property default value
     * @return created property type
     */
    public <V> AttributeType<V> addProperty(final String scope, final String localPart,
            final Class<V> valueClass, final V defaultValue)
    {
        return addProperty(create(scope, localPart), valueClass, defaultValue, null);
    }

    /**
     * Add a new property to the feature type.
     * Property will have a minimum and maximum occurrence of one and the given
     * {@code CoordinateReferenceSystem} characteristic.
     *
     * @param scope property generic name scope part
     * @param localPart property generic name tip part
     * @param valueClass property value class
     * @param defaultValue property default value
     * @param crs property {@code CoordinateReferenceSystem} characteristic
     * @return created property type
     */
    public <V> AttributeType<V> addProperty(final String scope, final String localPart,
            final Class<V> valueClass, final V defaultValue, final CoordinateReferenceSystem crs)
    {
        return addProperty(create(scope, localPart), valueClass, 1, 1, defaultValue, crs);
    }

    /**
     * Add a new property to the feature type.
     * Property will have a minimum and maximum occurrence of one and no characteristics.
     *
     * @param name property name
     * @param valueClass property value class
     * @param defaultValue property default value
     * @return created property type
     */
    public <V> AttributeType<V> addProperty(final GenericName name, final Class<V> valueClass, final V defaultValue) {
        return addProperty(name, valueClass, 1, 1, defaultValue);
    }

    /**
     * Add a new property to the feature type.
     * Property will have a minimum and maximum occurrence of one and the given
     * {@code CoordinateReferenceSystem} characteristic.
     *
     * @param name property name
     * @param valueClass property value class
     * @param defaultValue property default value
     * @param crs property {@code CoordinateReferenceSystem} characteristic
     * @return created property type
     */
    public <V> AttributeType<V> addProperty(final GenericName name, final Class<V> valueClass,
            final V defaultValue, final CoordinateReferenceSystem crs)
    {
        return addProperty(name, valueClass, 1, 1, defaultValue, crs);
    }

    /**
     * Add a new property to the feature type.
     *
     *
     * @param localPart property generic name tip part
     * @param valueClass property value class
     * @param minimumOccurs property minimum number of occurrences
     * @param maximumOccurs property maximum number of occurrences
     * @param defaultValue property default value
     * @return created property type
     */
    public <V> AttributeType<V> addProperty(final String localPart, final Class<V> valueClass,
            final int minimumOccurs, final int maximumOccurs, final V defaultValue)
    {
        return addProperty(create(localPart), valueClass, minimumOccurs, maximumOccurs, defaultValue);
    }

    /**
     * Add a new property to the feature type.
     *
     * @param scope property generic name scope part
     * @param localPart property generic name tip part
     * @param valueClass property value class
     * @param minimumOccurs property minimum number of occurrences
     * @param maximumOccurs property maximum number of occurrences
     * @param defaultValue property default value
     * @return created property type
     */
    public <V> AttributeType<V> addProperty(final String scope, final String localPart, final Class<V> valueClass,
            final int minimumOccurs, final int maximumOccurs, final V defaultValue)
    {
        return addProperty(create(scope, localPart), valueClass, minimumOccurs, maximumOccurs, defaultValue);
    }

    /**
     * Add a new property to the feature type.
     *
     * @param scope property generic name scope part
     * @param localPart property generic name tip part
     * @param valueClass property value class
     * @param minimumOccurs property minimum number of occurrences
     * @param maximumOccurs property maximum number of occurrences
     * @param defaultValue property default value
     * @param crs property {@code CoordinateReferenceSystem} characteristic
     * @return created property type
     */
    public <V> AttributeType<V> addProperty(final String scope, final String localPart, final Class<V> valueClass,
            final int minimumOccurs, final int maximumOccurs, final V defaultValue, final CoordinateReferenceSystem crs)
    {
        return addProperty(create(scope, localPart), valueClass, minimumOccurs, maximumOccurs, defaultValue, crs);
    }

    /**
     * Add a new property to the feature type.
     *
     * @param name property name
     * @param valueClass property value class
     * @param minimumOccurs property minimum number of occurrences
     * @param maximumOccurs property maximum number of occurrences
     * @param defaultValue property default value
     * @return created property type
     */
    public <V> AttributeType<V> addProperty(final GenericName name, final Class<V> valueClass,
            final int minimumOccurs, final int maximumOccurs, final V defaultValue)
    {
        return addProperty(name, valueClass, minimumOccurs, maximumOccurs, defaultValue,null);
    }

    /**
     * Add a new property to the feature type.
     *
     * @param name property name
     * @param valueClass property value class
     * @param minimumOccurs property minimum number of occurrences
     * @param maximumOccurs property maximum number of occurrences
     * @param defaultValue property default value
     * @param crs property {@code CoordinateReferenceSystem} characteristic
     * @return created property type
     */
    public <V> AttributeType<V> addProperty(final GenericName name, final Class<V> valueClass,
            final int minimumOccurs, final int maximumOccurs, final V defaultValue, final CoordinateReferenceSystem crs)
    {
        final AttributeType<V> att;
        if (crs != null) {
            final AttributeType<CoordinateReferenceSystem> qualifier = new DefaultAttributeType<>(
                    Collections.singletonMap(NAME_KEY, CHARACTERISTIC_CRS),
                    CoordinateReferenceSystem.class, 1, 1, crs);
            att = new DefaultAttributeType<>(
                Collections.singletonMap(NAME_KEY, name),
                valueClass, minimumOccurs, maximumOccurs, defaultValue, qualifier);
        } else {
            att = new DefaultAttributeType<>(
                Collections.singletonMap(NAME_KEY, name),
                valueClass, minimumOccurs, maximumOccurs, defaultValue);
        }
        addProperty(att);
        return att;
    }

    /**
     * Add a custom property to the feature type.
     *
     * @param property user defined property type
     * @return created property type
     */
    public PropertyType addProperty(final PropertyType property) {
        properties.put(property.getName(), property);
        return property;
    }

    /**
     * Add a new association to the feature type.
     * Association will have a minimum occurrence of zero and maximum occurrence of one.
     *
     * @param scope property generic name scope part
     * @param localPart property generic name tip part
     * @param type associated feature type
     * @return created association
     */
    public FeatureAssociationRole addAssociation(final String scope, final String localPart, final FeatureType type) {
        return addAssociation(scope, localPart, type,0,1);
    }

    /**
     * Add a new association to the feature type.
     * Association will have a minimum occurrence of zero and maximum occurrence of one.
     *
     * @param name property name
     * @param type associated feature type
     * @return created association
     */
    public FeatureAssociationRole addAssociation(final GenericName name, final FeatureType type) {
        return addAssociation(name, type,0,1);
    }

    /**
     * Add a new association to the feature type.
     *
     * @param scope property generic name scope part
     * @param localPart property generic name tip part
     * @param type associated feature type
     * @param minimumOccurs property minimum number of occurrences
     * @param maximumOccurs property maximum number of occurrences
     * @return created association
     */
    public FeatureAssociationRole addAssociation(final String scope, final String localPart, final FeatureType type,
            final int minimumOccurs, final int maximumOccurs)
    {
        return addAssociation(create(scope, localPart), type,minimumOccurs, maximumOccurs);
    }

    /**
     * Add a new association to the feature type.
     *
     * @param name property name
     * @param type associated feature type
     * @param minimumOccurs property minimum number of occurrences
     * @param maximumOccurs property maximum number of occurrences
     * @return created association
     */
    public FeatureAssociationRole addAssociation(final GenericName name, final FeatureType type,
            final int minimumOccurs, final int maximumOccurs)
    {
        final FeatureAssociationRole role = new DefaultAssociationRole(
                Collections.singletonMap(NAME_KEY, name), type, minimumOccurs, maximumOccurs);
        addProperty(role);
        return role;
    }

    /**
     * Add a custom properties to the feature type.
     *
     * @param properties user defined property types
     */
    public void addProperties(final PropertyType ... properties) {
        for (PropertyType pt : properties) {
            this.properties.put(pt.getName(), pt);
        }
    }

    /**
     * Add a custom properties to the feature type.
     *
     * @param properties user defined property types
     */
    public void addProperties(final Collection<? extends PropertyType> properties) {
        for (PropertyType pt : properties) {
            this.properties.put(pt.getName(), pt);
        }
    }

    /**
     * Remove a property from the feature type.
     *
     * @param name property name
     * @return removed property, can be null if property was not found
     */
    public PropertyType removeProperty(final String name) {
        return properties.remove(valueOf(name));
    }

    /**
     * Remove a property from the feature type.
     *
     * @param name property name
     * @return removed property, can be null if property was not found
     */
    public PropertyType removeProperty(final GenericName name) {
        return properties.remove(name);
    }

    /**
     * Build feature type
     *
     * @return FeatureType
     */
    public DefaultFeatureType build() throws IllegalArgumentException {
        //build id property
        if (idAttributes != null) {
            //check id properties exist
            for (GenericName n : idAttributes) {
                if (!properties.containsKey(n)) {
                    throw new IllegalArgumentException("Property "+n+" used in id does not exist");
                }
            }

            String prefix = idPrefix;
            if (idPrefix==null) {
                prefix = getName().tip().toString();
            }

            final Operation att = FeatureOperations.compound(
                    Collections.singletonMap(AbstractOperation.NAME_KEY, ATTRIBUTE_ID),
                    idSeparator, prefix, null, properties.get(idAttributes));
            properties.put(ATTRIBUTE_ID, att);
        }
        //build default geometry property
        if (defGeomAttribute != null) {
            if (!properties.containsKey(defGeomAttribute)) {
                throw new IllegalArgumentException("Property "+defGeomAttribute+" used in default geometry does not exist");
            }
            final PropertyType geomAtt = properties.get(defGeomAttribute);
            final CoordinateReferenceSystem crs = AttributeConvention.getCRSCharacteristic(geomAtt);
            final Operation att = FeatureOperations.link(
                    Collections.singletonMap(AbstractOperation.NAME_KEY, ATTRIBUTE_DEFAULT_GEOMETRY), geomAtt);
            properties.put(ATTRIBUTE_DEFAULT_GEOMETRY, att);

            final Operation boundAtt = FeatureOperations.bounds(Collections.singletonMap(AbstractOperation.NAME_KEY, ATTRIBUTE_BOUNDS), crs);
            properties.put(ATTRIBUTE_BOUNDS, boundAtt);

        }

        verifyOperations();

        return new DefaultFeatureType(
                parameters,
                isAbstract,
                superTypes.toArray(new FeatureType[superTypes.size()]),
                properties.values().toArray(new PropertyType[properties.size()]));
    }

    /**
     * Check operations have the required properties.
     *
     * @throws IllegalArgumentException if some properties are missing for an operation.
     */
    private void verifyOperations() throws IllegalArgumentException {
        for (PropertyType pt : properties.values()) {
            if (pt instanceof AbstractOperation) {
                final Set<String> dependencies = ((AbstractOperation)pt).getDependencies();
                depLoop:
                for (String dep : dependencies) {
                    for (GenericName gn : properties.keySet()) {
                        if (match(gn, dep)) continue depLoop;
                    }
                    throw new IllegalArgumentException("Operation "+pt.getName().toString()+" requiere property "+dep+" but this property is missing.");
                }

            }
        }

    }

    ////////////////////////////////////////////////////////////////////////////
    // Name utils //////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////


    private static GenericName create(final String local) {
        return create(null,local);
    }

    /**
     *
     * @param parsedNames mandatory
     * @return GenericName
     */
    private static GenericName create(final String scope, final String localPart) {
        if (scope == null) {
            return DefaultFactories.forBuildin(NameFactory.class).createGenericName(null, localPart);
        } else {
            return DefaultFactories.forBuildin(NameFactory.class).createGenericName(null, scope, localPart);
        }
    }

    /**
     * Parse a string value that can be expressed in 2 different forms :
     * JSR-283 extended form : {uri}localpart
     * Separator form : uri:localpart
     *
     * if the given string do not match any, then a Name with no namespace will be
     * created and the localpart will be the given string.
     *
     * @param candidate String to convert to a geoneric name
     * @return Name
     */
    private static GenericName valueOf(final String candidate) {

        if (candidate.startsWith("{")) {
            //name is in extended form
            return toSessionNamespaceFromExtended(candidate);
        }

        int index = candidate.lastIndexOf(':');

        if (index <= 0) {
            return create(null, candidate);
        } else {
            final String uri = candidate.substring(0,index);
            final String name = candidate.substring(index+1,candidate.length());
            return create(uri, name);
        }

    }

    private static GenericName toSessionNamespaceFromExtended(final String candidate) {
        final int index = candidate.indexOf('}');

        if (index < 0) throw new IllegalArgumentException("Invalide extended form : "+ candidate);

        final String uri = candidate.substring(1, index);
        final String name = candidate.substring(index+1, candidate.length());

        return create(uri, name);
    }

    private static String toExpandedString(final GenericName name) {
        String ns = getNamespace(name);
        if (ns == null) {
            return name.tip().toString();
        } else {
            return '{' + ns + '}' + name.tip();
        }
    }

    /**
     * Tests that the given string representation matches the given name.
     * String can be written with only the local part or in extendedform or JCR
     * extended form.
     *
     * @param name expected generic name
     * @param candidate name to test
     * @return true if the string match the name
     */
    private static boolean match(final GenericName name, final String candidate) {
        if (candidate.startsWith("{")) {
            //candidate is in extended form
            return candidate.equals(toExpandedString(name));
        }

        final int index = candidate.lastIndexOf(':');

        if (index <= 0) {
            return candidate.equals(name.tip().toString());
        } else {
            final String uri = candidate.substring(0,index);
            final String local = candidate.substring(index+1,candidate.length());
            return uri.equals(getNamespace(name)) && local.equals(name.tip().toString());
        }
    }

    private static String getNamespace(final GenericName name) {
        return (name instanceof ScopedName) ? ((ScopedName)name).path().toString() : null;
    }
}
