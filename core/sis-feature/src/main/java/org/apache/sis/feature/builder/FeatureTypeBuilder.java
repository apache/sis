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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.FactoryException;
import org.apache.sis.feature.AbstractOperation;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.feature.FeatureOperations;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.feature.Resources;
import org.apache.sis.util.CorruptedObjectException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArraysExt;

// Branch-dependent imports
import java.util.Objects;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.Operation;


/**
 * Helper class for the creation of {@link FeatureType} instances.
 * This builder can create the arguments to be given to the
 * {@linkplain DefaultFeatureType#DefaultFeatureType feature type constructor}
 * from simpler parameters given to this builder.
 *
 * <p>{@code FeatureTypeBuilder} should be short lived.
 * After the {@code FeatureType} has been created, the builder should be discarded.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 *
 * @see org.apache.sis.parameter.ParameterBuilder
 */
public class FeatureTypeBuilder extends TypeBuilder {
    /**
     * The factory to use for creating names.
     */
    private final NameFactory nameFactory;

    /**
     * Builders for the properties (attributes, associations or operations) of this feature.
     */
    private final List<PropertyTypeBuilder> properties;

    /**
     * The parent of the feature to create. By default, new features have no parent.
     */
    private final List<FeatureType> superTypes;

    /**
     * Whether the feature type is abstract. The default value is {@code false}.
     *
     * @see #isAbstract()
     * @see #setAbstract(boolean)
     */
    private boolean isAbstract;

    /**
     * The default scope to use when {@link #name(String, String)} is invoked with a null scope.
     *
     * @see #getDefaultScope()
     * @see #setDefaultScope(String)
     */
    private String defaultScope;

    /**
     * The default minimum number of property values.
     *
     * @see #setDefaultCardinality(int, int)
     */
    int defaultMinimumOccurs;

    /**
     * The default maximum number of property values.
     *
     * @see #setDefaultCardinality(int, int)
     */
    int defaultMaximumOccurs;

    /**
     * An optional prefix or suffix to insert before or after the {@linkplain FeatureOperations#compound compound key}
     * named {@code "@identifier"}.
     */
    private String idPrefix, idSuffix;

    /**
     * The separator to insert between each single component in a {@linkplain FeatureOperations#compound compound key}
     * named {@code "@identifier"}. This is ignored if {@link #identifierCount} is zero.
     */
    private String idDelimiter;

    /**
     * Number of attribute that have been flagged as an identifier component.
     *
     * @see AttributeRole#IDENTIFIER_COMPONENT
     * @see AttributeConvention#IDENTIFIER_PROPERTY
     */
    int identifierCount;

    /**
     * The default geometry attribute, or {@code null} if none.
     *
     * @see AttributeRole#DEFAULT_GEOMETRY
     * @see AttributeConvention#GEOMETRY_PROPERTY
     */
    AttributeTypeBuilder<?> defaultGeometry;

    /**
     * The object created by this builder, or {@code null} if not yet created.
     * This field must be cleared every time that a setter method is invoked on this builder.
     */
    private transient FeatureType feature;

    /**
     * Creates a new builder instance using the default name factory.
     */
    public FeatureTypeBuilder() {
        this(null, null, null);
    }

    /**
     * Creates a new builder instance using the given feature type as a template.
     *
     * @param template  an existing feature type to use as a template, or {@code null} if none.
     */
    public FeatureTypeBuilder(final FeatureType template) {
        this(template, null, null);
    }

    /**
     * Creates a new builder instance using the given name factory, template and locale for formatting error messages.
     *
     * @param template  an existing feature type to use as a template, or {@code null} if none.
     * @param factory   the factory to use for creating names, or {@code null} for the default factory.
     * @param locale    the locale to use for formatting error messages, or {@code null} for the default locale.
     */
    public FeatureTypeBuilder(final FeatureType template, NameFactory factory, final Locale locale) {
        super(template, locale);
        if (factory == null) {
            factory = DefaultFactories.forBuildin(NameFactory.class);
        }
        nameFactory = factory;
        properties  = new ArrayList<>();
        superTypes  = new ArrayList<>();
        idDelimiter = ":";
        defaultMinimumOccurs = 1;
        defaultMaximumOccurs = 1;
        if (template != null) {
            feature    = template;
            isAbstract = template.isAbstract();
            superTypes.addAll(template.getSuperTypes());
            /*
             * For each attribute and association, wrap those properties in a builder.
             * For each operation, wrap them in pseudo-builder only if the operation
             * is not one of the operations automatically generated by this builder.
             */
            final Map<String,Set<AttributeRole>> propertyRoles = new HashMap<>();
            for (final PropertyType property : template.getProperties(false)) {
                PropertyTypeBuilder builder;
                if (property instanceof AttributeType<?>) {
                    builder = new AttributeTypeBuilder<>(this, (AttributeType<?>) property);
                } else if (property instanceof FeatureAssociationRole) {
                    builder = new AssociationRoleBuilder(this, (FeatureAssociationRole) property);
                } else {
                    builder = null;     // Do not create OperationWrapper now - see below.
                }
                /*
                 * If the property name is one of our (Apache SIS specific) conventional names, try to reconstitute
                 * the attribute roles that caused FeatureTypeBuilder to produce such property. Those roles usually
                 * need to be applied on the source properties used for calculating the current property. There is
                 * usually at most one role for each source property, but we nevertheless allow an arbitrary amount.
                 */
                final AttributeRole role;
                final GenericName name = property.getName();
                if (AttributeConvention.IDENTIFIER_PROPERTY.equals(name)) {
                    role = AttributeRole.IDENTIFIER_COMPONENT;
                } else if (AttributeConvention.GEOMETRY_PROPERTY.equals(name)) {
                    role = AttributeRole.DEFAULT_GEOMETRY;
                } else if (AttributeConvention.ENVELOPE_PROPERTY.equals(name)) {
                    // If "@envelope" is an operation, skip it completely.
                    // It will be recreated if a default geometry exists.
                    role = null;
                } else {
                    if (builder == null) {
                        // For all unknown operation, wrap as-is.
                        builder = new OperationWrapper(this, property);
                    }
                    role = null;
                }
                if (role != null) {
                    final Set<AttributeRole> rc = Collections.singleton(role);
                    if (property instanceof AbstractOperation) {
                        for (final String dependency : ((AbstractOperation) property).getDependencies()) {
                            propertyRoles.merge(dependency, rc, AttributeRole::merge);
                        }
                    } else {
                        propertyRoles.merge(name.toString(), rc, AttributeRole::merge);
                    }
                }
                if (builder != null) {
                    properties.add(builder);
                }
            }
            /*
             * At this point we finished to collect information about the attribute roles.
             * Now assign those roles to the attribute builders. Note that some roles may
             * be ignored if we didn't found a suitable builder. The roles inference done
             * in this constructor is only a "best effort".
             */
            if (!propertyRoles.isEmpty()) {
                for (final Map.Entry<String,Set<AttributeRole>> entry : propertyRoles.entrySet()) {
                    final PropertyTypeBuilder property = forName(properties, entry.getKey());
                    if (property instanceof AttributeTypeBuilder<?>) {
                        ((AttributeTypeBuilder<?>) property).roles().addAll(entry.getValue());
                    }
                }
            }
        }
    }

    /**
     * If the {@code FeatureType} created by the last call to {@link #build()} has been cached,
     * clears that cache. This method must be invoked every time that a setter method is invoked.
     */
    @Override
    final void clearCache() {
        feature = null;
    }

    /**
     * Returns {@code true} if the feature type to create will act as an abstract super-type.
     * Abstract types can not be {@linkplain DefaultFeatureType#newInstance() instantiated}.
     *
     * @return {@code true} if the feature type to create will act as an abstract super-type.
     *
     * @see DefaultFeatureType#isAbstract()
     */
    public boolean isAbstract() {
        return isAbstract;
    }

    /**
     * Sets whether the feature type to create will be abstract.
     * If this method is not invoked, then the default value is {@code false}.
     *
     * @param  isAbstract whether the feature type will be abstract.
     * @return {@code this} for allowing method calls chaining.
     */
    public FeatureTypeBuilder setAbstract(final boolean isAbstract) {
        if (this.isAbstract != isAbstract) {
            this.isAbstract  = isAbstract;
            clearCache();
        }
        return this;
    }

    /**
     * Returns the direct parents of the feature type to create.
     *
     * @return the parents of the feature type to create, or an empty array if none.
     *
     * @see DefaultFeatureType#getSuperTypes()
     */
    public FeatureType[] getSuperTypes() {
        return superTypes.toArray(new FeatureType[superTypes.size()]);
    }

    /**
     * Sets the parent types (or super-type) from which to inherit properties.
     * If this method is not invoked, then the default value is to have no parent.
     *
     * @param  parents  the parent types from which to inherit properties, or an empty array if none.
     * @return {@code this} for allowing method calls chaining.
     */
    public FeatureTypeBuilder setSuperTypes(final FeatureType... parents) {
        ensureNonNull("parents", parents);
        final List<FeatureType> asList = Arrays.asList(parents);
        if (!superTypes.equals(asList)) {
            superTypes.clear();
            superTypes.addAll(asList);
            clearCache();
        }
        return this;
    }

    /**
     * Sets the {@code FeatureType} name as a generic name.
     * If another name was defined before this method call, that previous value will be discarded.
     *
     * <div class="note"><b>Note for subclasses:</b>
     * all {@code setName(…)} convenience methods in this builder delegate to this method.
     * Consequently this method can be used as a central place where to control the creation of all names.</div>
     *
     * @return {@code this} for allowing method calls chaining.
     */
    @Override
    public FeatureTypeBuilder setName(final GenericName name) {
        super.setName(name);
        return this;
    }

    /**
     * Sets the {@code FeatureType} name as a simple string with the default scope.
     * The default scope is the value specified by the last call to {@link #setDefaultScope(String)}.
     * The name will be a {@linkplain org.apache.sis.util.iso.DefaultLocalName local name} if no default scope
     * has been specified, or a {@linkplain org.apache.sis.util.iso.DefaultScopedName scoped name} otherwise.
     *
     * <p>This convenience method creates a {@link GenericName} instance,
     * then delegates to {@link #setName(GenericName)}.</p>
     *
     * @return {@code this} for allowing method calls chaining.
     */
    @Override
    public FeatureTypeBuilder setName(final String localPart) {
        super.setName(localPart);
        return this;
    }

    /**
     * Sets the {@code FeatureType} name as a string in the given scope.
     * The name will be a {@linkplain org.apache.sis.util.iso.DefaultLocalName local name} if the given scope is
     * {@code null} or empty, or a {@linkplain org.apache.sis.util.iso.DefaultScopedName scoped name} otherwise.
     * If a {@linkplain #setDefaultScope(String) default scope} has been specified, then the
     * {@code scope} argument overrides it.
     *
     * <p>This convenience method creates a {@link GenericName} instance,
     * then delegates to {@link #setName(GenericName)}.</p>
     *
     * @return {@code this} for allowing method calls chaining.
     */
    @Override
    public FeatureTypeBuilder setName(final String scope, final String localPart) {
        super.setName(scope, localPart);
        return this;
    }

    /**
     * Invoked by {@link TypeBuilder} for creating new {@code LocalName} or {@code GenericName} instances.
     */
    @Override
    final GenericName name(String scope, final String localPart) {
        if (scope == null) {
            scope = getDefaultScope();
        }
        if (scope == null || scope.isEmpty()) {
            return nameFactory.createLocalName(null, localPart);
        } else {
            return nameFactory.createGenericName(null, scope, localPart);
        }
    }

    /**
     * Returns the scope of the names created by {@code setName(String)} method calls.
     *
     * @return the scope to use by default when {@link #setName(String)} is invoked.
     */
    public String getDefaultScope() {
        return defaultScope;
    }

    /**
     * Sets the scope of the next names created by {@code setName(String)} method calls.
     * This method applies only to the next calls to {@code setName(String)};
     * the result of all previous calls stay unmodified.
     *
     * <p>There is different conventions about the use of name scopes. ISO 19109 suggests that the scope of all
     * {@code AttributeType} names is the name of the enclosing {@code FeatureType}, but this is not mandatory.
     * Users who want to apply this convention can invoke {@code setDefaultScope(featureName)} after
     * <code>{@linkplain #setName(String) FeatureTypeBuilder.setName}(featureName)</code> but before
     * <code>{@linkplain AttributeTypeBuilder#setName(String) AttributeTypeBuilder.setName}(attributeName)</code>.</p>
     *
     * @param  scope  the new default scope, or {@code null} if none.
     * @return {@code this} for allowing method calls chaining.
     */
    public FeatureTypeBuilder setDefaultScope(final String scope) {
        defaultScope = scope;
        // No need to clear the cache because this change affects
        // only the next names to be created, not the existing ones.
        return this;
    }

    /**
     * Sets the default minimum and maximum number of next attributes and associations to add.
     * Those defaults will applied to newly created attributes or associations,
     * for example in next calls to {@link #addAttribute(Class)}.
     * Attributes and associations added before this method call are not modified.
     *
     * <p>If this method is not invoked, then the default cardinality is [1 … 1].</p>
     *
     * @param  minimumOccurs  new default minimum number of property values.
     * @param  maximumOccurs  new default maximum number of property values.
     * @return {@code this} for allowing method calls chaining.
     *
     * @see PropertyTypeBuilder#setMinimumOccurs(int)
     * @see PropertyTypeBuilder#setMaximumOccurs(int)
     */
    public FeatureTypeBuilder setDefaultCardinality(final int minimumOccurs, final int maximumOccurs) {
        if (minimumOccurs < 0 || maximumOccurs < minimumOccurs) {
            throw new IllegalArgumentException(errors().getString(Errors.Keys.IllegalRange_2, minimumOccurs, maximumOccurs));
        }
        defaultMinimumOccurs = minimumOccurs;
        defaultMaximumOccurs = maximumOccurs;
        // No need to clear the cache because this change affects only
        // the next properties to be created, not the existing ones.
        return this;
    }

    /**
     * Sets the prefix, suffix and delimiter to use when formatting a compound identifier made of two or more attributes.
     * The delimiter will be used only if at least two attributes have the {@linkplain AttributeRole#IDENTIFIER_COMPONENT
     * identifier component role}.
     *
     * <p>If this method is not invoked, then the default values are the {@code ":"} delimiter and no prefix or suffix.</p>
     *
     * @param  delimiter  the characters to use as delimiter between each single property value.
     * @param  prefix     characters to use at the beginning of the concatenated string, or {@code null} if none.
     * @param  suffix     characters to use at the end of the concatenated string, or {@code null} if none.
     * @return {@code this} for allowing method calls chaining.
     *
     * @see AttributeRole#IDENTIFIER_COMPONENT
     * @see FeatureOperations#compound(Map, String, String, String, PropertyType...)
     */
    public FeatureTypeBuilder setIdentifierDelimiters(final String delimiter, final String prefix, final String suffix) {
        ensureNonEmpty("delimiter", delimiter);
        if (!delimiter.equals(idDelimiter) || !Objects.equals(prefix, idPrefix) || !Objects.equals(suffix, idSuffix)) {
            idDelimiter = delimiter;
            idPrefix    = prefix;
            idSuffix    = suffix;
            clearCache();
        }
        return this;
    }

    /**
     * Returns a view of all attributes and associations added to the {@code FeatureType} to build.
     * The returned list is <cite>live</cite>: changes in this builder are reflected in that list and conversely.
     * However the returned list allows only {@linkplain List#remove(Object) remove} operations;
     * new attributes or associations can be added only by calls to one of the {@code addAttribute(…)}
     * or {@code addAssociation(…)} methods.
     *
     * @return a live list over the properties declared to this builder.
     *
     * @see #getProperty(String)
     * @see #addAttribute(Class)
     * @see #addAttribute(AttributeType)
     * @see #addAssociation(FeatureType)
     * @see #addAssociation(GenericName)
     * @see #addAssociation(FeatureAssociationRole)
     */
    public List<PropertyTypeBuilder> properties() {
        return new RemoveOnlyList<>(properties);
    }

    /**
     * Returns the builder for the property of the given name. The given name does not need to contains all elements
     * of a {@link org.opengis.util.ScopedName}; it is okay to specify only the tip (for example {@code "myName"}
     * instead of {@code "myScope:myName"}) provided that ignoring the name head does not create ambiguity.
     *
     * @param  name   name of the property to search.
     * @return property of the given name, or {@code null} if none.
     * @throws IllegalArgumentException if the given name is ambiguous.
     *
     * @see #addProperty(PropertyType)
     */
    public PropertyTypeBuilder getProperty(final String name) {
        return forName(properties, name);
    }

    /**
     * Creates a new {@code AttributeType} builder for values of the given class.
     * The default attribute name is the name of the given type, but callers should invoke one
     * of the {@code AttributeTypeBuilder.setName(…)} methods on the returned instance with a better name.
     *
     * <p>Usage example:</p>
     * {@preformat java
     *     builder.addAttribute(String.class).setName("City").setDefaultValue("Metropolis");
     * }
     *
     * The value class can not be {@code Feature.class} since features shall be handled
     * as {@linkplain #addAssociation(FeatureType) associations} instead than attributes.
     *
     * @param  <V>  the compile-time value of {@code valueClass} argument.
     * @param  valueClass  the class of attribute values (can not be {@code Feature.class}).
     * @return a builder for an {@code AttributeType}.
     *
     * @see #properties()
     */
    public <V> AttributeTypeBuilder<V> addAttribute(final Class<V> valueClass) {
        ensureNonNull("valueClass", valueClass);
        if (Feature.class.isAssignableFrom(valueClass)) {
            // We disallow Feature.class because that type shall be handled as association instead than attribute.
            throw new IllegalArgumentException(errors().getString(Errors.Keys.IllegalArgumentValue_2, "valueClass", valueClass));
        }
        final AttributeTypeBuilder<V> property = new AttributeTypeBuilder<>(this, valueClass);
        properties.add(property);
        clearCache();
        return property;
    }

    /**
     * Creates a new {@code AttributeType} builder initialized to the same characteristics than the given template.
     *
     * @param  <V>       the compile-time type of values in the {@code template} argument.
     * @param  template  an existing attribute type to use as a template.
     * @return a builder for an {@code AttributeType}, initialized with the values of the given template.
     *
     * @see #properties()
     */
    public <V> AttributeTypeBuilder<V> addAttribute(final AttributeType<V> template) {
        ensureNonNull("template", template);
        final AttributeTypeBuilder<V> property = new AttributeTypeBuilder<>(this, template);
        properties.add(property);
        clearCache();
        return property;
    }

    /**
     * Creates a new {@code FeatureAssociationRole} builder for features of the given type.
     * The default association name is the name of the given type, but callers should invoke one
     * of the {@code AssociationRoleBuilder.setName(…)} methods on the returned instance with a better name.
     *
     * @param  type  the type of feature values.
     * @return a builder for a {@code FeatureAssociationRole}.
     *
     * @see #properties()
     */
    public AssociationRoleBuilder addAssociation(final FeatureType type) {
        ensureNonNull("type", type);
        final AssociationRoleBuilder property = new AssociationRoleBuilder(this, type, type.getName());
        properties.add(property);
        clearCache();
        return property;
    }

    /**
     * Creates a new {@code FeatureAssociationRole} builder for features of a type of the given name.
     * This method can be invoked as an alternative to {@link #addAssociation(FeatureType)} when the
     * {@code FeatureType} instance is not yet available because of cyclic dependency.
     *
     * @param  type  the name of the type of feature values.
     * @return a builder for a {@code FeatureAssociationRole}.
     *
     * @see #properties()
     */
    public AssociationRoleBuilder addAssociation(final GenericName type) {
        ensureNonNull("type", type);
        final AssociationRoleBuilder property = new AssociationRoleBuilder(this, null, type);
        properties.add(property);
        clearCache();
        return property;
    }

    /**
     * Creates a new {@code FeatureAssociationRole} builder initialized to the same characteristics
     * than the given template.
     *
     * @param  template  an existing feature association to use as a template.
     * @return a builder for an {@code FeatureAssociationRole}, initialized with the values of the given template.
     *
     * @see #properties()
     */
    public AssociationRoleBuilder addAssociation(final FeatureAssociationRole template) {
        ensureNonNull("template", template);
        final AssociationRoleBuilder property = new AssociationRoleBuilder(this, template);
        properties.add(property);
        clearCache();
        return property;
    }

    /**
     * Adds the given property in the feature type properties.
     * The given property shall be an instance of one of the following types:
     * <ul>
     *   <li>{@link AttributeType}, in which case this method delegate to {@link #addAttribute(AttributeType)}.</li>
     *   <li>{@link FeatureAssociationRole}, in which case this method delegate to {@link #addAssociation(FeatureAssociationRole)}.</li>
     *   <li>{@link Operation}, in which case the given operation object will be added verbatim in the {@code FeatureType};
     *       this builder does not create new operations.</li>
     * </ul>
     *
     * @param  template  the property to add to the feature type.
     * @return a builder initialized to the given builder.
     *         In the {@code Operation} case, the builder is a read-only accessor on the operation properties.
     *
     * @see #properties()
     * @see #getProperty(String)
     */
    public PropertyTypeBuilder addProperty(final PropertyType template) {
        ensureNonNull("template", template);
        if (template instanceof AttributeType<?>) {
            return addAttribute((AttributeType<?>) template);
        } else if (template instanceof FeatureAssociationRole) {
            return addAssociation((FeatureAssociationRole) template);
        } else {
            final PropertyTypeBuilder property = new OperationWrapper(this, template);
            properties.add(property);
            clearCache();
            return property;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FeatureTypeBuilder setDefinition(final CharSequence definition) {
        super.setDefinition(definition);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FeatureTypeBuilder setDesignation(final CharSequence designation) {
        super.setDesignation(designation);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FeatureTypeBuilder setDescription(final CharSequence description) {
        super.setDescription(description);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FeatureTypeBuilder setDeprecated(final boolean deprecated) {
        super.setDeprecated(deprecated);
        return this;
    }

    /**
     * Builds the feature type from the information and properties specified to this builder.
     * One of the {@code setName(…)} methods must have been invoked before this {@code build()} method (mandatory).
     * All other methods are optional, but some calls to a {@code add} method are usually needed.
     *
     * <p>If a feature type has already been built and this builder state has not changed since the
     * feature type creation, then the previously created {@code FeatureType} instance is returned.</p>
     *
     * @return the feature type.
     * @throws IllegalStateException if the builder contains inconsistent information.
     */
    @Override
    public FeatureType build() throws IllegalStateException {
        if (feature == null) {
            /*
             * Creates an initial array of property types with up to 3 slots reserved for @identifier, @geometry
             * and @envelope operations. At first we presume that there is always an identifier.  The identifier
             * slot will be removed later if there is none.
             */
            final int numSpecified = properties.size();     // Number of explicitely specified properties.
            int numSynthetic;                               // Number of synthetic properties that may be generated.
            int envelopeIndex = -1;
            int geometryIndex = -1;
            final PropertyType[] identifierTypes;
            if (identifierCount == 0) {
                numSynthetic    = 0;
                identifierTypes = null;
            } else {
                numSynthetic    = 1;
                identifierTypes = new PropertyType[identifierCount];
            }
            if (defaultGeometry != null) {
                envelopeIndex = numSynthetic++;
                if (!AttributeConvention.GEOMETRY_PROPERTY.equals(defaultGeometry.getName())) {
                    geometryIndex = numSynthetic++;
                }
            }
            final PropertyType[] propertyTypes = new PropertyType[numSynthetic + numSpecified];
            int propertyCursor = numSynthetic;
            int identifierCursor = 0;
            for (int i=0; i<numSpecified; i++) {
                final PropertyTypeBuilder builder = properties.get(i);
                final PropertyType instance = builder.build();
                propertyTypes[propertyCursor] = instance;
                /*
                 * Collect the attributes to use as identifier components while we loop over all properties.
                 * A NullPointerException or an ArrayIndexOutOfBoundsException in this block would mean that
                 * identifierCount field has not been updated correctly by an addRole(AttributeRole) method.
                 */
                if (builder.isIdentifier()) {
                    identifierTypes[identifierCursor++] = instance;
                }
                /*
                 * If there is a default geometry, add a link named "@geometry" to that geometry.
                 * It may happen that the property created by the user is already named "@geometry",
                 * in which case we will avoid to duplicate the property.
                 */
                if (builder == defaultGeometry && geometryIndex >= 0) {
                    if (propertyTypes[geometryIndex] != null) {
                        // Assuming that there is no bug in our implementation, this error could happen if the user
                        // has modified this FeatureTypeBuilder in another thread during this build() execution.
                        throw new CorruptedObjectException();
                    }
                    propertyTypes[geometryIndex] = FeatureOperations.link(name(AttributeConvention.GEOMETRY_PROPERTY), instance);
                }
                propertyCursor++;
            }
            /*
             * Create the "envelope" operation only after we created all other properties.
             * Actually it is okay if the 'propertyTypes' array still contains null elements not needed for envelope calculation
             * like "@identifier", since FeatureOperations.envelope(…) constructor ignores any property which is not for a value.
             */
            if (envelopeIndex >= 0) try {
                propertyTypes[envelopeIndex] = FeatureOperations.envelope(name(AttributeConvention.ENVELOPE_PROPERTY), null, propertyTypes);
            } catch (FactoryException e) {
                throw new IllegalStateException(e);
            }
            /*
             * If a synthetic identifier need to be created, create it now as the first property.
             * It may happen that the user provided a single identifier component already named
             * "@identifier", in which case we avoid to duplicate the property.
             */
            if (identifierTypes != null) {
                if (identifierCursor != identifierTypes.length) {
                    // Assuming that there is no bug in our implementation, this error could happen if the user
                    // has modified this FeatureTypeBuilder in another thread during this build() execution.
                    throw new CorruptedObjectException();
                }
                if (AttributeConvention.IDENTIFIER_PROPERTY.equals(identifierTypes[0].getName())) {
                    if (identifierCursor > 1) {
                        throw new IllegalStateException(Resources.format(Resources.Keys.PropertyAlreadyExists_2,
                                getDisplayName(), AttributeConvention.IDENTIFIER_PROPERTY));
                    }
                    System.arraycopy(propertyTypes, 1, propertyTypes, 0, --propertyCursor);
                } else {
                    propertyTypes[0] = FeatureOperations.compound(name(AttributeConvention.IDENTIFIER_PROPERTY),
                            idDelimiter, idPrefix, idSuffix, identifierTypes);
                }
            }
            feature = new DefaultFeatureType(identification(), isAbstract(),
                    superTypes.toArray(new FeatureType[superTypes.size()]),
                    ArraysExt.resize(propertyTypes, propertyCursor));
        }
        return feature;
    }

    /**
     * Helper method for creating identification info of synthetic attributes.
     */
    private static Map<String,?> name(final GenericName name) {
        return Collections.singletonMap(AbstractOperation.NAME_KEY, name);
    }

    /**
     * Replaces the given builder instance by a new instance, or delete the old instance.
     * This builder should contain exactly one instance of the given {@code old} builder.
     *
     * @param old  the instance to replace.
     * @param replacement  the replacement, or {@code null} for deleting the old instance.
     */
    final void replace(final PropertyTypeBuilder old, final PropertyTypeBuilder replacement) {
        final int index = properties.lastIndexOf(old);
        if (index < 0 || (replacement != null ? properties.set(index, replacement) : properties.remove(index)) != old) {
            // Assuming that there is no bug in our algorithm, this exception should never happen
            // unless builder state has been changed in another thread before this method completed.
            throw new CorruptedObjectException();
        }
        if (old == defaultGeometry) {
            defaultGeometry = (AttributeTypeBuilder<?>) replacement;
        }
        clearCache();
    }

    /**
     * Formats a string representation of this builder for debugging purpose.
     */
    @Override
    final void toStringInternal(final StringBuilder buffer) {
        if (isAbstract()) {
            buffer.insert(buffer.indexOf("[") + 1, "abstract ");
        }
        String separator = " : ";
        for (final FeatureType parent : superTypes) {
            buffer.append(separator).append('“').append(parent.getName()).append('”');
            separator = ", ";
        }
        buffer.append(" {");
        separator = System.lineSeparator();
        for (final PropertyTypeBuilder p : properties) {
            p.toString(buffer.append(separator).append("    ").append(p.getClass().getSimpleName()));
        }
        buffer.append(separator).append('}');
    }
}
