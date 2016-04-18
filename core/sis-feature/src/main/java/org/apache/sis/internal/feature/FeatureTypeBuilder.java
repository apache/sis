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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.ScopedName;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.feature.AbstractOperation;
import org.apache.sis.feature.DefaultAssociationRole;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.feature.FeatureOperations;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.util.ArgumentChecks;

import static org.apache.sis.internal.feature.NameConvention.*;

// Branch-dependent imports
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.FeatureType;
import org.opengis.feature.Operation;
import org.opengis.feature.PropertyType;


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
 */
public class FeatureTypeBuilder extends Builder {
    /**
     * The factory to use for creating names.
     */
    private final NameFactory nameFactory;

    /**
     * The feature properties. Entries in this map are added by invoking one of the
     * {@code addProperty(…)} methods defined in this class.
     */
    private final Map<GenericName,PropertyType> properties = new LinkedHashMap<>();

    /**
     * The parent of the feature to create. By default, new features have no parent.
     */
    private final List<FeatureType> superTypes = new ArrayList<>();

    /**
     * Whether the feature type is abstract. The default value is {@code false}.
     */
    private boolean isAbstract;

    /**
     * If {@link #idAttributes} is non-null, an optional prefix to insert before the
     * {@linkplain FeatureOperations#compound compound key} named {@code "@id"}.
     */
    private String idPrefix;

    /**
     * If {@link #idAttributes} is non-null and contains more than one value, the separator to insert between
     * each single component in a {@linkplain FeatureOperations#compound compound key} named {@code "@id"}.
     */
    private String idDelimiter;

    /**
     * The names of attributes to use in a {@linkplain FeatureOperations#compound compound key} named {@code "@id"},
     * or {@code null} if none. If this array contains only one name and {@link #idPrefix} is null, then {@code "@id"}
     * will be a {@linkplain FeatureOperations#link link} to the attribute named {@code idAttributes[0]}.
     */
    private GenericName[] idAttributes;

    /**
     * The name of the default geometry attribute, or {@code null} if none.
     */
    private GenericName defGeomAttribute;

    /**
     * Creates a new builder instance.
     */
    public FeatureTypeBuilder() {
        nameFactory = DefaultFactories.forBuildin(NameFactory.class);
    }

    /**
     * Resets this builder to its initial state. After invocation of this method,
     * this builder is in the same state than after construction.
     */
    @Override
    public void clear() {
        super.clear();
        properties.clear();
        superTypes.clear();
        isAbstract       = false;
        idPrefix         = null;
        idDelimiter      = null;
        idAttributes     = null;
        defGeomAttribute = null;
    }

    /**
     * Sets this builder state to properties inferred from the given feature type.
     * This method fetches the {@code FeatureType} name, definition, designation, description, super-types,
     * abstract flag, and list of properties from the given instance.
     *
     * <p>This method is useful when an existing instance is to be used as a template.</p>
     *
     * @param template  feature type to copy parameters from.
     */
    public void copy(final FeatureType template) {
        super.copy(template);
        setAbstract(template.isAbstract());
        Set<? extends FeatureType> parents = template.getSuperTypes();
        setSuperTypes(parents.toArray(new FeatureType[parents.size()]));
        for (PropertyType pt : template.getProperties(false)) {
            addProperty(pt);
        }
    }

    /**
     * Sets whether the feature type is abstract.
     * If this method is not invoked, then the default value is {@code false}.
     *
     * @param isAbstract whether the feature type is abstract.
     */
    public void setAbstract(final boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    /**
     * Sets the parent types (or super-type) from which to inherit properties.
     * If this method is not invoked, then the default value is to have no parent.
     *
     * @param parents  the parent types from which to inherit properties, or an empty array if none.
     */
    public void setSuperTypes(final FeatureType... parents) {
        ArgumentChecks.ensureNonNull("parents", parents);
        superTypes.clear();
        superTypes.addAll(Arrays.asList(parents));
    }

    /**
     * Define an id operation composed of the given attributes.
     *
     * @param prefix generated id prefix
     * @param separator generated id separator between attribute values
     * @param attributes attributes used in the id operation
     */
    public void setIdOperation(final String prefix, final String separator, final GenericName... attributes) {
        idPrefix     = prefix;
        idDelimiter  = separator;
        idAttributes = attributes.clone();
        properties.put(ID_PROPERTY, null);                                  // Add placeholder
    }

    /**
     * Define a default geometry link operation.
     *
     * @param attribute referenced attribute
     */
    public void setDefaultGeometryOperation(final GenericName attribute) {
        defGeomAttribute = attribute;
        properties.put(DEFAULT_GEOMETRY_PROPERTY, null);                    // Add placeholder
        properties.put(ENVELOPE_PROPERTY, null);
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
        return addProperty(name(null, localPart), valueClass);
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
        return addProperty(name(scope, localPart), valueClass);
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
        return addProperty(name(scope, localPart), valueClass, crs);
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
        return addProperty(name(scope, localPart), valueClass, defaultValue, null);
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
        return addProperty(name(scope, localPart), valueClass, 1, 1, defaultValue, crs);
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
        return addProperty(name(null, localPart), valueClass, minimumOccurs, maximumOccurs, defaultValue);
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
        return addProperty(name(scope, localPart), valueClass, minimumOccurs, maximumOccurs, defaultValue);
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
        return addProperty(name(scope, localPart), valueClass, minimumOccurs, maximumOccurs, defaultValue, crs);
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
                    Collections.singletonMap(DefaultFeatureType.NAME_KEY, CRS_CHARACTERISTIC),
                    CoordinateReferenceSystem.class, 1, 1, crs);
            att = new DefaultAttributeType<>(
                Collections.singletonMap(DefaultFeatureType.NAME_KEY, name),
                valueClass, minimumOccurs, maximumOccurs, defaultValue, qualifier);
        } else {
            att = new DefaultAttributeType<>(
                Collections.singletonMap(DefaultFeatureType.NAME_KEY, name),
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
        return addAssociation(name(scope, localPart), type,minimumOccurs, maximumOccurs);
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
                Collections.singletonMap(DefaultFeatureType.NAME_KEY, name), type, minimumOccurs, maximumOccurs);
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
                    Collections.singletonMap(AbstractOperation.NAME_KEY, ID_PROPERTY),
                    idDelimiter, prefix, null, properties.get(idAttributes));
            properties.put(ID_PROPERTY, att);
        }
        //build default geometry property
        if (defGeomAttribute != null) {
            if (!properties.containsKey(defGeomAttribute)) {
                throw new IllegalArgumentException("Property "+defGeomAttribute+" used in default geometry does not exist");
            }
            final PropertyType geomAtt = properties.get(defGeomAttribute);
            final CoordinateReferenceSystem crs = null; // TODO NameConvention.getCoordinateReferenceSystem(geomAtt);
            final Operation att = FeatureOperations.link(
                    Collections.singletonMap(AbstractOperation.NAME_KEY, DEFAULT_GEOMETRY_PROPERTY), geomAtt);
            properties.put(DEFAULT_GEOMETRY_PROPERTY, att);

            final Operation boundAtt;
            try {
                boundAtt = FeatureOperations.envelope(Collections.singletonMap(AbstractOperation.NAME_KEY, ENVELOPE_PROPERTY),
                        crs, properties.values().toArray(new PropertyType[properties.size()]));
            } catch (FactoryException e) {
                throw new IllegalStateException(e);
            }
            properties.put(ENVELOPE_PROPERTY, boundAtt);

        }

        verifyOperations();

        return new DefaultFeatureType(
                identification,
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

    /**
     *
     * @param parsedNames mandatory
     * @return GenericName
     */
    @Override
    final GenericName name(final String scope, final String localPart) {
        if (scope == null) {
            return nameFactory.createLocalName(null, localPart);
        } else {
            return nameFactory.createGenericName(null, scope, localPart);
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
    private GenericName valueOf(final String candidate) {

        if (candidate.startsWith("{")) {
            //name is in extended form
            return toSessionNamespaceFromExtended(candidate);
        }

        int index = candidate.lastIndexOf(':');

        if (index <= 0) {
            return name(null, candidate);
        } else {
            final String uri = candidate.substring(0,index);
            final String name = candidate.substring(index+1,candidate.length());
            return name(uri, name);
        }

    }

    private GenericName toSessionNamespaceFromExtended(final String candidate) {
        final int index = candidate.indexOf('}');

        if (index < 0) throw new IllegalArgumentException("Invalide extended form : "+ candidate);

        final String uri = candidate.substring(1, index);
        final String name = candidate.substring(index+1, candidate.length());

        return name(uri, name);
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
