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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.Objects;
import org.opengis.util.NameSpace;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.FactoryException;
import org.opengis.metadata.acquisition.GeometryType;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.feature.AbstractOperation;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.feature.FeatureOperations;
import org.apache.sis.feature.privy.AttributeConvention;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.util.CorruptedObjectException;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.util.resources.Errors;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.AbstractIdentifiedType;
import org.apache.sis.feature.DefaultAssociationRole;
import org.apache.sis.feature.DefaultAttributeType;


/**
 * Helper class for the creation of {@code FeatureType} instances.
 * This builder can create the arguments to be given to the
 * {@linkplain DefaultFeatureType#DefaultFeatureType feature type constructor}
 * from simpler parameters given to this builder.
 * The main methods provided in this class are:
 *
 * <ul>
 *   <li>Various {@link #setName(CharSequence) setName(...)} methods for specifying the feature type name (mandatory).</li>
 *   <li>Methods for optionally setting {@linkplain #setDesignation designation}, {@linkplain #setDefinition definition} or
 *       {@linkplain #setDescription description} texts, or the {@linkplain #setDeprecated deprecation status}.</li>
 *   <li>Methods for optionally specifying the feature type hierarchy: its {@linkplain #setSuperTypes super types}
 *       and whether the feature type is {@linkplain #setAbstract abstract}.</li>
 *   <li>Convenience methods for setting the {@linkplain #setNameSpace name space} and the
 *       {@linkplain #setDefaultMultiplicity default multiplicity} of properties to be added to the feature type.</li>
 *   <li>Methods for {@linkplain #addAttribute(Class) adding an attribute}, {@linkplain #addAssociation(DefaultFeatureType)
 *       an association} or {@linkplain #addProperty an operation}.</li>
 *   <li>Method for listing the previously added {@linkplain #properties() properties}.</li>
 *   <li>A {@link #build()} method for creating the {@code FeatureType} instance from all previous information.</li>
 * </ul>
 *
 * The following example creates a city named "Utopia" by default:
 *
 * {@snippet lang="java" :
 *     FeatureTypeBuilder builder;

 *     // Create a feature type for a city, which contains a name and a population.
 *     builder = new FeatureTypeBuilder() .setName("City");
 *     builder.addAttribute(String.class) .setName("name").setDefaultValue("Utopia");
 *     builder.addAttribute(Integer.class).setName("population");
 *     FeatureType city = builder.build();
 *     }
 *
 * A call to {@code System.out.println(city)} prints the following table:
 *
 * <pre class="text">
 *   City
 *   ┌────────────┬─────────┬──────────────┬───────────────┐
 *   │ Name       │ Type    │ Multiplicity │ Default value │
 *   ├────────────┼─────────┼──────────────┼───────────────┤
 *   │ name       │ String  │   [1 … 1]    │ Utopia        │
 *   │ population │ Integer │   [1 … 1]    │               │
 *   └────────────┴─────────┴──────────────┴───────────────┘</pre>
 *
 * {@code FeatureTypeBuilder} instances should be short lived.
 * After the {@code FeatureType} has been created, the builder should be discarded.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.5
 *
 * @see org.apache.sis.parameter.ParameterBuilder
 *
 * @since 0.8
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
    private final List<DefaultFeatureType> superTypes;

    /**
     * Whether the feature type is abstract. The default value is {@code false}.
     *
     * @see #isAbstract()
     * @see #setAbstract(boolean)
     */
    private boolean isAbstract;

    /**
     * The namespace to use when a {@link #setName(CharSequence)} method is invoked.
     *
     * @see #getNameSpace()
     * @see #setNameSpace(CharSequence)
     */
    private NameSpace namespace;

    /**
     * The default minimum number of property values.
     *
     * @see #setDefaultMultiplicity(int, int)
     */
    int defaultMinimumOccurs;

    /**
     * The default maximum number of property values.
     *
     * @see #setDefaultMultiplicity(int, int)
     */
    int defaultMaximumOccurs;

    /**
     * An optional prefix or suffix to insert before or after the {@linkplain FeatureOperations#compound compound key}
     * named {@code "sis:identifier"}.
     */
    private String idPrefix, idSuffix;

    /**
     * The separator to insert between each single component in a {@linkplain FeatureOperations#compound compound key}
     * named {@code "sis:identifier"}. This is ignored if {@link #identifierCount} is zero.
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
     * Provides method for creating geometric objects using the library specified by the user.
     */
    private final Geometries<?> geometries;

    /**
     * The object created by this builder, or {@code null} if not yet created.
     * This field must be cleared every time that a setter method is invoked on this builder.
     */
    private transient DefaultFeatureType feature;

    /**
     * Creates a new builder instance using the default name factory.
     */
    public FeatureTypeBuilder() {
        this(null, null, null);
    }

    /**
     * Creates a new builder instance using the given feature type as a template.
     * This constructor initializes the list of {@linkplain #properties() properties}, the
     * {@linkplain #getSuperTypes() super types} and {@link #isAbstract() isAbstract} flag
     * to values inferred from the given template. The properties list will contain properties
     * declared explicitly in the given template, not including properties inherited from super types.
     *
     * <div class="warning"><b>Warning:</b>
     * The {@code template} argument type will be changed to {@code FeatureType} if and when such interface
     * will be defined in GeoAPI.</div>
     *
     * @param template  an existing feature type to use as a template, or {@code null} if none.
     */
    @SuppressWarnings("this-escape")    // The invoked method does not store `this` and is not overrideable.
    public FeatureTypeBuilder(final DefaultFeatureType template) {
        this(null, null, null);
        if (template != null) {
            initialize(template);
        }
    }

    /**
     * Creates a new builder instance using the given name factory, geometry library
     * and locale for formatting error messages.
     *
     * @param factory  the factory to use for creating names, or {@code null} for the default factory.
     * @param library  the library to use for creating geometric objects, or {@code null} for the default.
     * @param locale   the locale to use for formatting error messages, or {@code null} for the default locale.
     */
    public FeatureTypeBuilder(NameFactory factory, final GeometryLibrary library, final Locale locale) {
        super(locale);
        if (factory == null) {
            factory = DefaultNameFactory.provider();
        }
        nameFactory          = factory;
        geometries           = Geometries.factory(library);
        properties           = new ArrayList<>();
        superTypes           = new ArrayList<>();
        idDelimiter          = ":";
        defaultMinimumOccurs = 1;
        defaultMaximumOccurs = 1;
    }

    /**
     * Clears all setting in this builder. After invoking this method, this {@code FeatureTypeBuilder}
     * is in same state that after it has been constructed. This method can be invoked for reusing the
     * same builder for creating other {@code FeatureType} instances after {@link #build()} invocation.
     *
     * @return {@code this} for allowing method calls chaining.
     */
    public FeatureTypeBuilder clear() {
        reset();
        properties.clear();
        superTypes.clear();
        isAbstract           = false;
        namespace            = null;
        defaultMinimumOccurs = 1;
        defaultMaximumOccurs = 1;
        idPrefix             = null;
        idSuffix             = null;
        idDelimiter          = ":";
        identifierCount      = 0;
        defaultGeometry      = null;
        clearCache();
        return this;
    }

    /**
     * Sets all properties of this builder to the values of the given feature type.
     * This builder is {@linkplain #clear() cleared} before the properties of the given type are copied.
     * The copy is performed as documented in the {@linkplain #FeatureTypeBuilder(DefaultFeatureType) constructor}.
     *
     * <div class="warning"><b>Warning:</b>
     * The {@code template} argument type will be changed to {@code FeatureType} if and when such interface
     * will be defined in GeoAPI.</div>
     *
     * @param  template  an existing feature type to use as a template, or {@code null} if none.
     * @return {@code this} for allowing method calls chaining.
     */
    public FeatureTypeBuilder setAll(final DefaultFeatureType template) {
        clear();
        if (template != null) {
            initialize(template);
        }
        return this;
    }

    /**
     * Initializes this builder to the value of the given type.
     * The caller is responsible to invoke {@link #clear()} (if needed) before this method.
     */
    private void initialize(final DefaultFeatureType template) {
        super.initialize(template);
        feature    = template;
        isAbstract = template.isAbstract();
        superTypes.addAll(template.getSuperTypes());
        /*
         * For each attribute and association, wrap those properties in a builder.
         * For each operation, wrap them in pseudo-builder only if the operation
         * is not one of the operations automatically generated by this builder.
         */
        final var propertyRoles = new HashMap<String,Set<AttributeRole>>();
        for (final AbstractIdentifiedType property : template.getProperties(false)) {
            PropertyTypeBuilder builder;
            if (property instanceof DefaultAttributeType<?>) {
                builder = new AttributeTypeBuilder<>(this, (DefaultAttributeType<?>) property);
            } else if (property instanceof DefaultAssociationRole) {
                builder = new AssociationRoleBuilder(this, (DefaultAssociationRole) property);
            } else {
                builder = null;                             // Do not create OperationWrapper now - see below.
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
                // If "sis:envelope" is an operation, skip it completely.
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
                final Set<AttributeRole> rc = Set.of(role);
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
                final PropertyTypeBuilder property = forName(properties, entry.getKey(), true);
                if (property instanceof AttributeTypeBuilder<?>) {
                    ((AttributeTypeBuilder<?>) property).roles().addAll(entry.getValue());
                }
            }
        }
    }

    /**
     * If the {@code FeatureType} created by the last call to {@link #build()} has been cached,
     * clears that cache. This method must be invoked every time that a setter method is invoked.
     *
     * @see #clear()
     */
    @Override
    final void clearCache() {
        feature = null;
    }

    /**
     * Returns {@code true} if the feature type to create will act as an abstract super-type.
     * Abstract types cannot be {@linkplain DefaultFeatureType#newInstance() instantiated}.
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
     * @param  isAbstract  whether the feature type will be abstract.
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
     * <div class="warning"><b>Warning:</b>
     * The return type will be changed to {@code FeatureType[]} if and when such interface
     * will be defined in GeoAPI.</div>
     *
     * @return the parents of the feature type to create, or an empty array if none.
     *
     * @see DefaultFeatureType#getSuperTypes()
     */
    public DefaultFeatureType[] getSuperTypes() {
        return superTypes.toArray(DefaultFeatureType[]::new);
    }

    /**
     * Sets the parent types (or super-type) from which to inherit properties.
     * If this method is not invoked, then the default value is no parent.
     *
     * <div class="warning"><b>Warning:</b>
     * The {@code parents} argument type will be changed to {@code FeatureType...} if and when such interface
     * will be defined in GeoAPI.</div>
     *
     * @param  parents  the parent types from which to inherit properties, or an empty array if none.
     *                  Null elements are ignored.
     * @return {@code this} for allowing method calls chaining.
     */
    public FeatureTypeBuilder setSuperTypes(final DefaultFeatureType... parents) {
        ensureNonNull("parents", parents);
        final List<DefaultFeatureType> asList = Arrays.asList(parents);
        if (!superTypes.equals(asList)) {
            superTypes.clear();
            superTypes.addAll(asList);
            for (int i=superTypes.size(); --i >= 0;) {
                if (superTypes.get(i) == null) {
                    superTypes.remove(i);
                }
            }
            clearCache();
        }
        return this;
    }

    /**
     * Returns the namespace of the names created by {@code setName(CharSequence...)} method calls.
     * A {@code null} value means that the names are in the
     * {@linkplain org.apache.sis.util.iso.DefaultNameSpace#isGlobal() global namespace}.
     *
     * @return the namespace to use when {@link #setName(CharSequence)} is invoked, or {@code null} if none.
     */
    public CharSequence getNameSpace() {
        return (namespace != null) ? namespace.name().toString() : null;
    }

    /**
     * Sets the namespace of the next names to be created by {@code setName(CharSequence...)} method calls.
     * This method applies only to the next calls to {@link #setName(CharSequence)} or
     * {@link #setName(CharSequence...)} methods; the result of all previous calls stay unmodified.
     * Example:
     *
     * {@snippet lang="java" :
     *     FeatureTypeBuilder builder = new FeatureTypeBuilder().setNameSpace("MyNameSpace").setName("City");
     *     FeatureType city = builder.build();

     *     System.out.println(city.getName());                              // Prints "City"
     *     System.out.println(city.getName().toFullyQualifiedName());       // Prints "MyNameSpace:City"
     *     }
     *
     * There is different conventions about the use of name spaces. ISO 19109 suggests that the namespace of all
     * {@code AttributeType} names is the name of the enclosing {@code FeatureType}, but this is not mandatory.
     * Users who want to apply this convention can invoke {@code setNameSpace(featureName)} after
     * <code>{@linkplain #setName(CharSequence) FeatureTypeBuilder.setName}(featureName)</code> but before
     * <code>{@linkplain AttributeTypeBuilder#setName(CharSequence) AttributeTypeBuilder.setName}(attributeName)</code>.
     *
     * @param  ns  the new namespace, or {@code null} if none.
     * @return {@code this} for allowing method calls chaining.
     */
    public FeatureTypeBuilder setNameSpace(final CharSequence ns) {
        if (ns != null && ns.length() != 0) {
            namespace = nameFactory.createNameSpace(nameFactory.createLocalName(null, ns), null);
        } else {
            namespace = null;
        }
        // No need to clear the cache because this change affects
        // only the next names to be created, not the existing ones.
        return this;
    }

    /**
     * Sets the {@code FeatureType} name as a generic name.
     * If another name was defined before this method call, that previous value will be discarded.
     *
     * <h4>Note for subclasses</h4>
     * All {@code setName(…)} convenience methods in this builder delegate to this method.
     * Consequently, this method can be used as a central place where to control the creation of all names.
     *
     * @return {@code this} for allowing method calls chaining.
     */
    @Override
    public FeatureTypeBuilder setName(final GenericName name) {
        super.setName(name);
        return this;
    }

    /**
     * Sets the {@code FeatureType} name as a simple string.
     * The namespace will be the value specified by the last call to {@link #setNameSpace(CharSequence)},
     * but that namespace will not be visible in the {@linkplain org.apache.sis.util.iso.DefaultLocalName#toString()
     * string representation} unless the {@linkplain org.apache.sis.util.iso.DefaultLocalName#toFullyQualifiedName()
     * fully qualified name} is requested.
     *
     * <p>This convenience method creates a {@link org.opengis.util.LocalName} instance from
     * the given {@code CharSequence}, then delegates to {@link #setName(GenericName)}.</p>
     *
     * @return {@code this} for allowing method calls chaining.
     */
    @Override
    public FeatureTypeBuilder setName(final CharSequence localPart) {
        super.setName(localPart);
        return this;
    }

    /**
     * Sets the {@code FeatureType} name as a string in the given scope.
     * The {@code components} array must contain at least one element.
     * In addition to the path specified by the {@code components} array, the name may also contain
     * a namespace specified by the last call to {@link #setNameSpace(CharSequence)}.
     * But contrarily to the specified components, the namespace will not be visible in the name
     * {@linkplain org.apache.sis.util.iso.DefaultScopedName#toString() string representation} unless the
     * {@linkplain org.apache.sis.util.iso.DefaultScopedName#toFullyQualifiedName() fully qualified name} is requested.
     *
     * <p>This convenience method creates a {@link org.opengis.util.LocalName} or {@link org.opengis.util.ScopedName}
     * instance depending on whether the {@code names} array contains exactly 1 element or more than 1 element, then
     * delegates to {@link #setName(GenericName)}.</p>
     *
     * @return {@code this} for allowing method calls chaining.
     */
    @Override
    public FeatureTypeBuilder setName(final CharSequence... components) {
        super.setName(components);
        return this;
    }

    /**
     * Creates a local name in the {@linkplain #setNameSpace feature namespace}.
     */
    @Override
    final GenericName createLocalName(final CharSequence name) {
        return nameFactory.createLocalName(namespace, name);
    }

    /**
     * Creates a generic name in the {@linkplain #setNameSpace feature namespace}.
     */
    @Override
    final GenericName createGenericName(final CharSequence... names) {
        return nameFactory.createGenericName(namespace, names);
    }

    /**
     * Sets the default minimum and maximum number of next attributes and associations to add.
     * Those defaults will applied to newly created attributes or associations,
     * for example in next calls to {@link #addAttribute(Class)}.
     * Attributes and associations added before this method call are not modified.
     *
     * <p>If this method is not invoked, then the default multiplicity is [1 … 1].</p>
     *
     * @param  minimumOccurs  new default minimum number of property values.
     * @param  maximumOccurs  new default maximum number of property values.
     * @return {@code this} for allowing method calls chaining.
     *
     * @see PropertyTypeBuilder#setMinimumOccurs(int)
     * @see PropertyTypeBuilder#setMaximumOccurs(int)
     *
     * @since 1.0
     */
    public FeatureTypeBuilder setDefaultMultiplicity(final int minimumOccurs, final int maximumOccurs) {
        if (minimumOccurs < 0 || maximumOccurs < minimumOccurs) {
            throw new IllegalArgumentException(errors().getString(Errors.Keys.IllegalRange_2, minimumOccurs, maximumOccurs));
        }
        defaultMinimumOccurs = minimumOccurs;
        defaultMaximumOccurs = maximumOccurs;
        /*
         * No need to clear the cache because this change affects only
         * the next properties to be created, not the existing ones.
         */
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
     * This list contains only properties declared explicitly to this builder;
     * it does not include properties inherited from {@linkplain #getSuperTypes() super-types}.
     * The returned list is <em>live</em>: changes in this builder are reflected in that list and conversely.
     * However, the returned list allows only {@linkplain List#remove(Object) remove} operations;
     * new attributes or associations can be added only by calls to one of the {@code addAttribute(…)}
     * or {@code addAssociation(…)} methods. Removal operations never affect the super-types.
     *
     * @return a live list over the properties declared to this builder.
     *
     * @see #getProperty(String)
     * @see #addAttribute(Class)
     * @see #addAttribute(DefaultAttributeType)
     * @see #addAssociation(DefaultFeatureType)
     * @see #addAssociation(GenericName)
     * @see #addAssociation(DefaultAssociationRole)
     */
    public List<PropertyTypeBuilder> properties() {
        return new RemoveOnlyList<>(properties);
    }

    /**
     * Returns {@code true} if a property of the given name is defined or if the given name is ambiguous.
     * Invoking this method is equivalent to testing if {@code getProperty(name) != null} except that this
     * method does not throw exception if the given name is ambiguous.
     *
     * @param  name  the name to test.
     * @return {@code true} if the given name is used by another property or is ambiguous.
     *
     * @since 1.0
     */
    public boolean isNameUsed(final String name) {
        return forName(properties, name, false) != null;
    }

    /**
     * Returns the builder for the property of the given name. The given name does not need to contains all elements
     * of a {@link org.opengis.util.ScopedName}. It is okay to specify only the tip (for example {@code "myName"}
     * instead of {@code "myScope:myName"}) provided that ignoring the name head does not create ambiguity.
     *
     * @param  name   name of the property to search.
     * @return property of the given name, or {@code null} if none.
     * @throws IllegalArgumentException if the given name is ambiguous.
     */
    public PropertyTypeBuilder getProperty(final String name) {
        return forName(properties, name, true);
    }

    /**
     * Creates a new {@code AttributeType} builder for values of the given class.
     * The default attribute name is the name of the given type, but callers should invoke one
     * of the {@code AttributeTypeBuilder.setName(…)} methods on the returned instance with a better name.
     *
     * <h4>Usage example</h4>
     * {@snippet lang="java" :
     *     builder.addAttribute(String.class).setName("City").setDefaultValue("Metropolis");
     *     }
     *
     * The value class cannot be {@code Feature.class} since features shall be handled
     * as {@linkplain #addAssociation(DefaultFeatureType) associations} instead of attributes.
     *
     * @param  <V>         the compile-time value of {@code valueClass} argument.
     * @param  valueClass  the class of attribute values (cannot be {@code Feature.class}).
     * @return a builder for an {@code AttributeType}.
     *
     * @see #properties()
     */
    public <V> AttributeTypeBuilder<V> addAttribute(final Class<V> valueClass) {
        ensureNonNull("valueClass", valueClass);
        if (AbstractFeature.class.isAssignableFrom(valueClass)) {
            // We disallow Feature.class because that type shall be handled as association instead of attribute.
            throw new IllegalArgumentException(errors().getString(Errors.Keys.IllegalArgumentValue_2, "valueClass", valueClass));
        }
        final var property = new AttributeTypeBuilder<V>(this, Numbers.primitiveToWrapper(valueClass));
        properties.add(property);
        clearCache();
        return property;
    }

    /**
     * Creates a new {@code AttributeType} builder initialized to the same characteristics as the given template.
     * If the new attribute duplicates an existing one (for example if the same template is used many times),
     * caller should use the returned builder for modifying some attributes.
     *
     * <div class="warning"><b>Warning:</b>
     * The {@code template} argument type will be changed to {@code AttributeType} if and when such interface
     * will be defined in GeoAPI.</div>
     *
     * @param  <V>       the compile-time type of values in the {@code template} argument.
     * @param  template  an existing attribute type to use as a template.
     * @return a builder for an {@code AttributeType}, initialized with the values of the given template.
     *
     * @see #properties()
     */
    public <V> AttributeTypeBuilder<V> addAttribute(final DefaultAttributeType<V> template) {
        ensureNonNull("template", template);
        final var property = new AttributeTypeBuilder<V>(this, template);
        properties.add(property);
        clearCache();
        return property;
    }

    /**
     * Creates a new attribute for geometries of the given type. This method delegates to {@link #addAttribute(Class)}
     * with a {@code valueClass} argument inferred from the combination of the {@link GeometryType} argument given to
     * this method with the {@link GeometryLibrary} argument given at {@linkplain #FeatureTypeBuilder(NameFactory,
     * GeometryLibrary, Locale) builder creation time}.
     * The geometry type can be:
     *
     * <ul>
     *   <li>{@link GeometryType#POINT}  for {@code Point} or {@code Point2D} type.</li>
     *   <li>{@link GeometryType#LINEAR} for {@code Polyline} or {@code LineString} type.</li>
     *   <li>{@link GeometryType#AREAL}  for {@code Polygon} type.</li>
     * </ul>
     *
     * Geometric objects outside the above list can still be used by declaring their type explicitly.
     * However, in this case there is no isolation level between the geometry types and the library that implement them.
     *
     * <h4>Example</h4>
     * The following code creates an attribute named "MyPoint" with values of class
     * {@link java.awt.geom.Point2D} if the library in use is {@linkplain GeometryLibrary#JAVA2D Java2D}.
     * The Coordinate Reference System (CRS) uses (<var>longitude</var>, <var>latitude</var>) axes on the WGS 84 datum.
     * Finally that new attribute is declared the feature <em>default</em> geometry:
     *
     * {@snippet lang="java" :
     *     builder.addAttribute(GeometryType.POINT).setName("MyPoint")
     *            .setCRS(CommonCRS.WGS84.normalizedGeographic())
     *            .addRole(AttributeRole.DEFAULT_GEOMETRY);
     *     }
     *
     * If the library in use is JTS or ESRI instead of Java2D,
     * then the {@code Point} class of those libraries will be used instead of {@code Point2D}.
     * The fully-qualified class names are given in the {@link GeometryLibrary} javadoc.
     *
     * @param  type  kind of geometric object (point, polyline or polygon).
     * @return a builder for an {@code AttributeType}.
     */
    public AttributeTypeBuilder<?> addAttribute(final GeometryType type) {
        ensureNonNull("type", type);
        var t = org.apache.sis.geometry.wrapper.GeometryType.forISO(type);
        if (t != null) {
            return addAttribute(geometries.getGeometryClass(t));
        } else {
            throw new IllegalArgumentException(errors().getString(Errors.Keys.UnsupportedArgumentValue_1, type));
        }
    }

    /**
     * Creates a new {@code FeatureAssociationRole} builder for features of the given type.
     * The default association name is the name of the given type, but callers should invoke one
     * of the {@code AssociationRoleBuilder.setName(…)} methods on the returned instance with a better name.
     *
     * <div class="warning"><b>Warning:</b>
     * The {@code type} argument type will be changed to {@code FeatureType} if and when such interface
     * will be defined in GeoAPI.</div>
     *
     * @param  type  the type of feature values.
     * @return a builder for a {@code FeatureAssociationRole}.
     *
     * @see #properties()
     */
    public AssociationRoleBuilder addAssociation(final DefaultFeatureType type) {
        ensureNonNull("type", type);
        final var property = new AssociationRoleBuilder(this, type, type.getName());
        properties.add(property);
        clearCache();
        return property;
    }

    /**
     * Creates a new {@code FeatureAssociationRole} builder for features of a type of the given name.
     * This method can be invoked as an alternative to {@code addAssociation(FeatureType)} when the
     * {@code FeatureType} instance is not yet available because of cyclic dependency.
     *
     * @param  type  the name of the type of feature values.
     * @return a builder for a {@code FeatureAssociationRole}.
     *
     * @see #properties()
     */
    public AssociationRoleBuilder addAssociation(final GenericName type) {
        ensureNonNull("type", type);
        final var property = new AssociationRoleBuilder(this, null, type);
        properties.add(property);
        clearCache();
        return property;
    }

    /**
     * Creates a new {@code FeatureAssociationRole} builder initialized to the same characteristics
     * than the given template. If the new association duplicates an existing one (for example if the
     * same template is used many times), caller should use the returned builder for modifying some
     * associations.
     *
     * <div class="warning"><b>Warning:</b>
     * The {@code template} argument type will be changed to {@code FeatureAssociationRole} if and when such interface
     * will be defined in GeoAPI.</div>
     *
     * @param  template  an existing feature association to use as a template.
     * @return a builder for an {@code FeatureAssociationRole}, initialized with the values of the given template.
     *
     * @see #properties()
     */
    public AssociationRoleBuilder addAssociation(final DefaultAssociationRole template) {
        ensureNonNull("template", template);
        final var property = new AssociationRoleBuilder(this, template);
        properties.add(property);
        clearCache();
        return property;
    }

    /**
     * Adds the given property in the feature type properties.
     * The given property shall be an instance of one of the following types:
     *
     * <ul>
     *   <li>{@code AttributeType}, in which case this method delegate to {@code addAttribute(AttributeType)}.</li>
     *   <li>{@code FeatureAssociationRole}, in which case this method delegate to {@code addAssociation(FeatureAssociationRole)}.</li>
     *   <li>{@code Operation}, in which case the given operation object will be added verbatim in the {@code FeatureType};
     *       this builder does not create new operations.</li>
     * </ul>
     *
     * This method does not verify if the given property duplicates an existing property.
     * If the same template is used many times, then the caller should use the returned builder
     * for modifying some properties.
     *
     * <div class="warning"><b>Warning:</b> In a future SIS version, the argument type may be changed to the
     * {@code org.opengis.feature.PropertyType} interface. This change is pending GeoAPI revision.</div>
     *
     * @param  template  the property to add to the feature type.
     * @return a builder initialized to the given template.
     *         In the {@code Operation} case, the builder is a read-only accessor on the operation properties.
     *
     * @see #properties()
     * @see #getProperty(String)
     */
    public PropertyTypeBuilder addProperty(final AbstractIdentifiedType template) {
        ensureNonNull("template", template);
        if (template instanceof DefaultAttributeType<?>) {
            return addAttribute((DefaultAttributeType<?>) template);
        } else if (template instanceof DefaultAssociationRole) {
            return addAssociation((DefaultAssociationRole) template);
        } else {
            final var property = new OperationWrapper(this, template);
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
     * <div class="warning"><b>Warning:</b> In a future SIS version, the return type may be changed to the
     * {@code org.opengis.feature.FeatureType} interface. This change is pending GeoAPI revision.</div>
     *
     * <p>If a feature type has already been built and this builder state has not changed since the
     * feature type creation, then the previously created {@code FeatureType} instance is returned.</p>
     *
     * @return the feature type.
     * @throws IllegalStateException if the builder contains inconsistent information.
     *
     * @see #clear()
     */
    @Override
    public DefaultFeatureType build() throws IllegalStateException {
        if (feature == null) {
            /*
             * Creates an initial array of property types with up to 3 slots reserved for sis:identifier, sis:geometry
             * and sis:envelope operations. At first we presume that there is always an identifier. The identifier slot
             * will be removed later if there is none.
             */
            final int numSpecified = properties.size();     // Number of explicitly specified properties.
            int numSynthetic;                               // Number of synthetic properties that may be generated.
            int envelopeIndex = -1;
            int geometryIndex = -1;
            final AbstractIdentifiedType[] identifierTypes;
            if (identifierCount == 0) {
                numSynthetic    = 0;
                identifierTypes = null;
            } else {
                numSynthetic    = 1;
                identifierTypes = new AbstractIdentifiedType[identifierCount];
            }
            if (defaultGeometry != null) {
                envelopeIndex = numSynthetic++;
                if (!AttributeConvention.GEOMETRY_PROPERTY.equals(defaultGeometry.getName())) {
                    geometryIndex = numSynthetic++;
                }
            }
            final var propertyTypes = new AbstractIdentifiedType[numSynthetic + numSpecified];
            int propertyCursor = numSynthetic;
            int identifierCursor = 0;
            for (int i=0; i<numSpecified; i++) {
                final PropertyTypeBuilder builder = properties.get(i);
                final AbstractIdentifiedType instance = builder.buildForFeature();
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
                 * If there is a default geometry, add a link named "sis:geometry" to that geometry.
                 * It may happen that the property created by the user is already named "sis:geometry",
                 * in which case we will avoid to duplicate the property.
                 */
                if (builder == defaultGeometry && geometryIndex >= 0) {
                    if (propertyTypes[geometryIndex] != null) {
                        /*
                         * Assuming that there is no bug in our implementation, this error could happen if the user
                         * has modified this FeatureTypeBuilder in another thread during this build() execution.
                         */
                        throw new CorruptedObjectException();
                    }
                    propertyTypes[geometryIndex] = FeatureOperations.link(name(AttributeConvention.GEOMETRY_PROPERTY), instance);
                }
                propertyCursor++;
            }
            /*
             * Create the "envelope" operation only after we created all other properties.
             * Actually it is okay if the 'propertyTypes' array still contains null elements not needed for envelope calculation
             * like "sis:identifier", since FeatureOperations.envelope(…) constructor ignores any property which is not for a value.
             */
            if (envelopeIndex >= 0) try {
                propertyTypes[envelopeIndex] = FeatureOperations.envelope(name(AttributeConvention.ENVELOPE_PROPERTY), null, propertyTypes);
            } catch (FactoryException e) {
                throw new IllegalStateException(e);
            }
            /*
             * If a synthetic identifier need to be created, create it now as the first property.
             * It may happen that the user provided a single identifier component already named
             * "sis:identifier", in which case we avoid to duplicate the property.
             */
            if (identifierTypes != null) {
                if (identifierCursor != identifierTypes.length) {
                    /*
                     * Assuming that there is no bug in our implementation, this error could happen if the user
                     * has modified this FeatureTypeBuilder in another thread during this build() execution.
                     */
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
                    superTypes.toArray(DefaultFeatureType[]::new),
                    ArraysExt.resize(propertyTypes, propertyCursor));
        }
        return feature;
    }

    /**
     * Helper method for creating identification info of synthetic attributes.
     */
    private static Map<String,?> name(final GenericName name) {
        return Map.of(AbstractOperation.NAME_KEY, name);
    }

    /**
     * Replaces the given builder instance by a new instance, or delete the old instance.
     * This builder should contain exactly one instance of the given {@code old} builder.
     * The {@code metadata} argument should generally be {@code true}, except when this
     * method is invoked for removing a property only temporarily, e.g. before to move
     * it to another location.
     *
     * @param old          the instance to replace.
     * @param replacement  the replacement, or {@code null} for deleting the old instance.
     * @param metadata     whether to update metadata such as {@link #defaultGeometry}.
     */
    final void replace(final PropertyTypeBuilder old, final PropertyTypeBuilder replacement, final boolean metadata) {
        final int index = properties.lastIndexOf(old);
        if (index < 0 || (replacement != null ? properties.set(index, replacement) : properties.remove(index)) != old) {
            /*
             * Assuming that there is no bug in our algorithm, this exception should never happen
             * unless builder state has been changed in another thread before this method completed.
             */
            throw new CorruptedObjectException();
        }
        if (metadata && old == defaultGeometry) {
            defaultGeometry = (replacement instanceof AttributeTypeBuilder<?>) ? (AttributeTypeBuilder<?>) replacement : null;
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
        for (final DefaultFeatureType parent : superTypes) {
            buffer.append(separator).append('“').append(parent.getName()).append('”');
            separator = ", ";
        }
        buffer.append(" {");
        separator = System.lineSeparator();
        for (final PropertyTypeBuilder p : properties) {
            p.appendStringTo(buffer.append(separator).append("    ").append(p.getClass().getSimpleName()));
        }
        buffer.append(separator).append('}');
    }
}
