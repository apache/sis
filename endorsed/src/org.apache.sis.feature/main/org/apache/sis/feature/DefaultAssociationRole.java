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
package org.apache.sis.feature;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Objects;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.feature.internal.shared.AttributeConvention;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureType;
import org.opengis.feature.FeatureAssociation;
import org.opengis.feature.FeatureAssociationRole;


/**
 * Indicates the role played by the association between two features.
 * In the area of geographic information, there exist multiple kinds of associations:
 *
 * <ul>
 *   <li><b>Aggregation</b> represents associations between features which can exist even if the aggregate is destroyed.</li>
 *   <li><b>Composition</b> represents relationships where the owned features are destroyed together with the composite.</li>
 *   <li><b>Spatial</b> association represents spatial or topological relationships that may exist between features (e.g. <q>east of</q>).</li>
 *   <li><b>Temporal</b> association may represent for example a sequence of changes over time involving the replacement of some
 *       feature instances by other feature instances.</li>
 * </ul>
 *
 * <h2>Immutability and thread safety</h2>
 * Instances of this class are immutable if all properties ({@link GenericName} and {@link InternationalString}
 * instances) and all arguments (e.g. {@code valueType}) given to the constructor are also immutable.
 * Such immutable instances can be shared by many objects and passed between threads without synchronization.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see DefaultFeatureType
 * @see AbstractAssociation
 *
 * @since 0.5
 */
public class DefaultAssociationRole extends FieldType implements FeatureAssociationRole {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1592712639262027124L;

    /**
     * The type of feature instances to be associated.
     *
     * @see #getValueType()
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private volatile FeatureType valueType;

    /**
     * The name of the property to use as a title for the associated feature, or an empty string if none.
     * This field is initially null, then computed when first needed.
     * This information is used only by {@link AbstractAssociation#toString()} implementation.
     *
     * @see #getTitleProperty(FeatureAssociationRole)
     */
    private transient volatile String titleProperty;

    /**
     * Constructs an association to the given feature type. The properties map is given unchanged to
     * the {@linkplain AbstractIdentifiedType#AbstractIdentifiedType(Map) super-class constructor}.
     * The following table is a reminder of main (not all) recognized map entries:
     *
     * <table class="sis">
     *   <caption>Recognized map entries (non exhaustive list)</caption>
     *   <tr>
     *     <th>Map key</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr><tr>
     *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#NAME_KEY}</td>
     *     <td>{@link GenericName} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#DEFINITION_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getDefinition()}</td>
     *   </tr><tr>
     *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#DESIGNATION_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getDesignation()}</td>
     *   </tr><tr>
     *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#DESCRIPTION_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getDescription()}</td>
     *   </tr><tr>
     *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#DEPRECATED_KEY}</td>
     *     <td>{@link Boolean}</td>
     *     <td>{@link #isDeprecated()}</td>
     *   </tr>
     * </table>
     *
     * @param identification  the name and other information to be given to this association role.
     * @param valueType       the type of feature values.
     * @param minimumOccurs   the minimum number of occurrences of the association within its containing entity.
     * @param maximumOccurs   the maximum number of occurrences of the association within its containing entity,
     *                        or {@link Integer#MAX_VALUE} if there is no restriction.
     *
     * @see org.apache.sis.feature.builder.AssociationRoleBuilder
     */
    public DefaultAssociationRole(final Map<String,?> identification, final FeatureType valueType,
            final int minimumOccurs, final int maximumOccurs)
    {
        super(identification, minimumOccurs, maximumOccurs);
        this.valueType = Objects.requireNonNull(valueType);
    }

    /**
     * Constructs an association to a feature type of the given name.
     * This constructor can be used when creating a cyclic graph of {@link DefaultFeatureType} instances.
     * In such cases, at least one association needs to be created while its {@code FeatureType} is not yet available.
     *
     * <h4>Example</h4>
     * The following establishes a bidirectional association between feature types <var>A</var> and <var>B</var>:
     *
     * {@snippet lang="java" :
     *     String    namespace = "My model";
     *     GenericName nameOfA = Names.createTypeName(namespace, ":", "Feature type A");
     *     GenericName nameOfB = Names.createTypeName(namespace, ":", "Feature type B");
     *     FeatureType typeA = new DefaultFeatureType(Map.of(NAME_KEY, nameOfA), false, null,
     *         new DefaultAssociationRole(Map.of(NAME_KEY, "Association to B"), nameOfB, 1, 1),
     *         // More properties if desired.
     *     );
     *     FeatureType typeB = new DefaultFeatureType(Map.of(NAME_KEY, nameOfB), false, null,
     *         new DefaultAssociationRole(Map.of(NAME_KEY, "Association to A"), featureA, 1, 1),
     *         // More properties if desired.
     *     );
     *     }
     *
     * After the above code completed, the {@linkplain #getValueType() value type} of <q>association to B</q>
     * has been automatically set to the {@code typeB} instance.
     *
     * Callers shall make sure that the feature types graph will not contain more than one feature of the given name.
     * If more than one {@code FeatureType} instance of the given name is found at resolution time, the selected one
     * is undetermined.
     *
     * @param identification  the name and other information to be given to this association role.
     * @param valueType       the name of the type of feature values.
     * @param minimumOccurs   the minimum number of occurrences of the association within its containing entity.
     * @param maximumOccurs   the maximum number of occurrences of the association within its containing entity,
     *                        or {@link Integer#MAX_VALUE} if there is no restriction.
     */
    public DefaultAssociationRole(final Map<String,?> identification, final GenericName valueType,
            final int minimumOccurs, final int maximumOccurs)
    {
        super(identification, minimumOccurs, maximumOccurs);
        this.valueType = new NamedFeatureType(Objects.requireNonNull(valueType));
    }

    /**
     * Returns {@code true} if the associated {@code FeatureType} is complete (not just a name).
     * This method returns {@code false} if this {@code FeatureAssociationRole} has been
     * {@linkplain #DefaultAssociationRole(Map, GenericName, int, int) constructed with only a feature name}
     * and that named feature has not yet been resolved.
     *
     * @return {@code true} if the associated feature is complete, or {@code false} if only its name is known.
     *
     * @see #getValueType()
     *
     * @since 0.8
     */
    public final boolean isResolved() {
        final FeatureType type = valueType;
        if (type instanceof NamedFeatureType) {
            final FeatureType resolved = ((NamedFeatureType) type).resolved;
            if (resolved == null) {
                return false;
            }
            valueType = resolved;
        }
        return true;
    }

    /**
     * If the associated feature type is a placeholder for a {@code FeatureType} to be defined later,
     * replaces the placeholder by the actual instance if available. Otherwise do nothing.
     *
     * This method is needed only in case of cyclic graph, e.g. feature <var>A</var> has an association
     * to feature <var>B</var> which has an association back to <var>A</var>. It may also be <var>A</var>
     * having an association to itself, <i>etc.</i>
     *
     * @param  creating    the feature type in process of being constructed.
     * @param  properties  {@code creating.getProperties(false)} given as a direct reference to the internal field,
     *         without invoking {@code getProperties(…)}. We do that because {@code resolve(…)} is invoked while the
     *         given {@code DefaultFeatureType} is under creation. Since {@code getProperties(…)} can be overridden,
     *         invoking that method on {@code creating} may cause a failure with user code.
     * @return {@code true} if this association references a resolved feature type after this method call.
     */
    final boolean resolve(final DefaultFeatureType creating, final Collection<PropertyType> properties) {
        final FeatureType type = valueType;
        if (type instanceof NamedFeatureType) {
            FeatureType resolved = ((NamedFeatureType) type).resolved;
            if (resolved == null) {
                final GenericName name = type.getName();
                if (name.equals(creating.getName())) {
                    resolved = creating;                                    // This is the most common case.
                } else {
                    /*
                     * The feature that we need to resolve is not the one we just created. Maybe we can find
                     * this desired feature in an association of the `creating` feature, instead of beeing
                     * the `creating` feature itself. This is a little bit unusual, but not illegal.
                     */
                    final List<FeatureType> deferred = new ArrayList<>();
                    resolved = search(creating, properties, name, deferred);
                    if (resolved == null) {
                        /*
                         * Did not found the desired FeatureType in the `creating` instance.
                         * Try harder, by searching recursively in associations of associations.
                         */
                        if (deferred.isEmpty() || (resolved = deepSearch(deferred, name)) == null) {
                            return false;
                        }
                    }
                }
                ((NamedFeatureType) type).resolved = resolved;
            }
            valueType = resolved;
        }
        return true;
    }

    /**
     * Searches in the given {@code feature} for an associated feature type of the given name.
     * This method does not search recursively in the associations of the associated features.
     * Such recursive search will be performed by {@link #deepSearch(List, GenericName)} only
     * if we do not find the desired feature in the most direct way.
     *
     * <p>Current implementation does not check that there are no duplicated names.
     * See {@link #deepSearch(List, GenericName)} for a rational.</p>
     *
     * @param  feature     the feature in which to search.
     * @param  properties  {@code feature.getProperties(false)}, or {@code null} for letting this method performing the call.
     * @param  name        the name of the feature to search.
     * @param  deferred    where to store {@code FeatureType}s to be eventually used for a deep search.
     * @return the feature of the given name, or {@code null} if none.
     */
    private static FeatureType search(final FeatureType feature, Collection<? extends PropertyType> properties,
            final GenericName name, final List<FeatureType> deferred)
    {
        /*
         * Search only in associations declared in the given feature, not in inherited associations.
         * The inherited associations will be checked in a separated loop below if we did not found
         * the request feature type in explicitly declared associations.
         */
        if (properties == null) {
            properties = feature.getProperties(false);
        }
        for (final PropertyType property : properties) {
            if (property instanceof FeatureAssociationRole) {
                final FeatureType valueType;
                if (property instanceof DefaultAssociationRole) {
                    valueType = ((DefaultAssociationRole) property).valueType;
                    if (valueType instanceof NamedFeatureType) {
                        continue;                                   // Skip unresolved feature types.
                    }
                } else {
                    valueType = ((FeatureAssociationRole) property).getValueType();
                }
                if (name.equals(valueType.getName())) {
                    return valueType;
                }
                deferred.add(valueType);
            }
        }
        /*
         * Search in inherited associations as a separated step, in order to include the overridden
         * associations in the search. Overridden associations have the same association role name,
         * but not necessarily the same feature type (may be a subtype). This is equivalent to
         * "covariant return type" in the Java language.
         */
        for (FeatureType type : feature.getSuperTypes()) {
            if (name.equals(type.getName())) {
                return type;
            }
            type = search(type, null, name, deferred);
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    /**
     * Potentially invoked after {@link #search(FeatureType, Collection, GenericName, List)} for searching
     * in associations of associations.
     *
     * <p>Current implementation does not check that there are no duplicated names. Even if we did so,
     * a graph of feature types may have no duplicated names at this time but some duplicated names
     * later. We rather put a warning in {@link #DefaultAssociationRole(Map, GenericName, int, int)}
     * javadoc.</p>
     *
     * @param  deferred  the feature types collected by {@link #search(FeatureType, Collection, GenericName, List)}.
     * @param  name      the name of the feature to search.
     * @return the feature of the given name, or {@code null} if none.
     */
    private static FeatureType deepSearch(final List<FeatureType> deferred, final GenericName name) {
        final Map<FeatureType,Boolean> done = new IdentityHashMap<>(8);
        for (int i=0; i<deferred.size();) {
            FeatureType valueType = deferred.get(i++);
            if (done.put(valueType, Boolean.TRUE) == null) {
                deferred.subList(0, i).clear();                 // Discard previous value for making more room.
                valueType = search(valueType, null, name, deferred);
                if (valueType != null) {
                    return valueType;
                }
                i = 0;
            }
        }
        return null;
    }

    /**
     * Returns the type of feature values.
     *
     * <p>This method cannot be invoked if {@link #isResolved()} returns {@code false}.
     * However, it is still possible to {@linkplain Features#getValueTypeName(PropertyType)
     * get the associated feature type name}.</p>
     *
     * @return the type of feature values.
     * @throws IllegalStateException if the feature type has been specified
     *         {@linkplain #DefaultAssociationRole(Map, GenericName, int, int) only by its name}
     *         and not yet resolved.
     *
     * @see #isResolved()
     * @see Features#getValueTypeName(PropertyType)
     */
    @Override
    public final FeatureType getValueType() {
        /*
         * This method shall be final for consistency with other methods in this classes
         * which use the `valueType` field directly. Furthermore, this method is invoked
         * (indirectly) by DefaultFeatureType constructors.
         */
        FeatureType type = valueType;
        if (type instanceof NamedFeatureType) {
            type = ((NamedFeatureType) type).resolved;
            if (type == null) {
                throw new IllegalStateException(Resources.format(Resources.Keys.UnresolvedFeatureName_1, getName()));
            }
            valueType = type;
        }
        return type;
    }

    /**
     * Returns the name of the feature type. This information is always available
     * even when the name has not yet been {@linkplain #resolve resolved}.
     */
    static GenericName getValueTypeName(final FeatureAssociationRole role) {
        return (role instanceof DefaultAssociationRole ? ((DefaultAssociationRole) role).valueType : role.getValueType()).getName();
    }

    /**
     * Returns the name of the property to use as a title for the associated feature, or {@code null} if none.
     * This method applies the following heuristic rules:
     *
     * <ul>
     *   <li>If associated feature has a property named {@code "sis:identifier"}, then this method returns that name.</li>
     *   <li>Otherwise if the associated feature has a mandatory property of type {@link CharSequence}, {@link GenericName}
     *       or {@link Identifier}, then this method returns the name of that property.</li>
     *   <li>Otherwise if the associated feature has an optional property of type {@link CharSequence}, {@link GenericName}
     *       or {@link Identifier}, then this method returns the name of that property.</li>
     *   <li>Otherwise this method returns {@code null}.</li>
     * </ul>
     *
     * This method should be used only for display purpose, not as a reliable or stable way to get the identifier.
     * The heuristic rules implemented in this method may change in any future Apache SIS version.
     */
    static String getTitleProperty(final FeatureAssociationRole role) {
        if (role instanceof DefaultAssociationRole) {
            String p = ((DefaultAssociationRole) role).titleProperty;       // No synchronization - not a big deal if computed twice.
            if (p != null) {
                return p.isEmpty() ? null : p;
            }
            p = searchTitleProperty(role.getValueType());
            ((DefaultAssociationRole) role).titleProperty = (p != null) ? p : "";
            return p;
        }
        return searchTitleProperty(role.getValueType());
    }

    /**
     * Implementation of {@link #getTitleProperty(FeatureAssociationRole)} for first search,
     * or for non-SIS {@code FeatureAssociationRole} implementations.
     */
    private static String searchTitleProperty(final FeatureType ft) {
        String fallback = null;
        if (ft.hasProperty(AttributeConvention.IDENTIFIER)) {
            return ft.getProperty(AttributeConvention.IDENTIFIER).getName().toString();
        }
        for (final PropertyType type : ft.getProperties(true)) {
            if (type instanceof AttributeType<?>) {
                final AttributeType<?> pt = (AttributeType<?>) type;
                final Class<?> valueClass = pt.getValueClass();
                if (CharSequence.class.isAssignableFrom(valueClass) ||
                    GenericName .class.isAssignableFrom(valueClass) ||
                    Identifier  .class.isAssignableFrom(valueClass))
                {
                    final String name = pt.getName().toString();
                    if (pt.getMaximumOccurs() != 0) {
                        return name;
                    } else if (fallback == null) {
                        fallback = name;
                    }
                }
            }
        }
        return fallback;
    }

    /**
     * Returns the minimum number of occurrences of the association within its containing entity.
     * The returned value is greater than or equal to zero.
     *
     * @return the minimum number of occurrences of the association within its containing entity.
     */
    @Override
    public final int getMinimumOccurs() {
        return super.getMinimumOccurs();
    }

    /**
     * Returns the maximum number of occurrences of the association within its containing entity.
     * The returned value is greater than or equal to the {@link #getMinimumOccurs()} value.
     * If there is no maximum, then this method returns {@link Integer#MAX_VALUE}.
     *
     * @return the maximum number of occurrences of the association within its containing entity,
     *         or {@link Integer#MAX_VALUE} if none.
     */
    @Override
    public final int getMaximumOccurs() {
        return super.getMaximumOccurs();
    }

    /**
     * Creates a new association instance of this role.
     *
     * @return a new association instance.
     *
     * @see AbstractAssociation#create(FeatureAssociationRole)
     */
    @Override
    public FeatureAssociation newInstance() {
        return AbstractAssociation.create(this);
    }

    /**
     * Returns a hash code value for this association role.
     *
     * @return {@inheritDoc}
     */
    @Override
    public int hashCode() {
        /*
         * Do not use the full `valueType` object for computing hash code,
         * because it may change before and after `resolve` is invoked. In
         * addition, this avoid infinite recursion in case of cyclic graph.
         */
        return super.hashCode() + valueType.getName().hashCode();
    }

    /**
     * Compares this association role with the given object for equality.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (super.equals(obj)) {
            final DefaultAssociationRole that = (DefaultAssociationRole) obj;
            return valueType.equals(that.valueType);
        }
        return false;
    }

    /**
     * Returns a string representation of this association role.
     * The returned string is for debugging purpose and may change in any future SIS version.
     *
     * @return a string representation of this association role for debugging purpose.
     */
    @Override
    public String toString() {
        return toString(deprecated, "FeatureAssociationRole", getName(), valueType.getName()).toString();
    }
}
