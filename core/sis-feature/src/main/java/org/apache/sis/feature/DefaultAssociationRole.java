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
import java.util.IdentityHashMap;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Debug;

import static org.apache.sis.util.ArgumentChecks.*;


/**
 * Indicates the role played by the association between two features.
 * In the area of geographic information, there exist multiple kinds of associations:
 *
 * <ul>
 *   <li><b>Aggregation</b> represents associations between features which can exist even if the aggregate is destroyed.</li>
 *   <li><b>Composition</b> represents relationships where the owned features are destroyed together with the composite.</li>
 *   <li><b>Spatial</b> association represents spatial or topological relationships that may exist between features (e.g. “<cite>east of</cite>”).</li>
 *   <li><b>Temporal</b> association may represent for example a sequence of changes over time involving the replacement of some
 *       feature instances by other feature instances.</li>
 * </ul>
 *
 * <div class="section">Immutability and thread safety</div>
 * Instances of this class are immutable if all properties ({@link GenericName} and {@link InternationalString}
 * instances) and all arguments (e.g. {@code valueType}) given to the constructor are also immutable.
 * Such immutable instances can be shared by many objects and passed between threads without synchronization.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see AbstractAssociation
 */
public class DefaultAssociationRole extends FieldType {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1592712639262027124L;

    /**
     * The type of feature instances to be associated.
     *
     * @see #getValueType()
     */
    private volatile FeatureType valueType;

    /**
     * The name of the property to use as a title for the associated feature, or an empty string if none.
     * This field is initially null, then computed when first needed.
     * This information is used only by {@link AbstractAssociation#toString()} implementation.
     *
     * @see #getTitleProperty()
     */
    private volatile transient String titleProperty;

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
     *   </tr>
     *   <tr>
     *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#NAME_KEY}</td>
     *     <td>{@link GenericName} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#DEFINITION_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getDefinition()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#DESIGNATION_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getDesignation()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#DESCRIPTION_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getDescription()}</td>
     *   </tr>
     * </table>
     *
     * @param identification The name and other information to be given to this association role.
     * @param valueType      The type of feature values.
     * @param minimumOccurs  The minimum number of occurrences of the association within its containing entity.
     * @param maximumOccurs  The maximum number of occurrences of the association within its containing entity,
     *                       or {@link Integer#MAX_VALUE} if there is no restriction.
     */
    public DefaultAssociationRole(final Map<String,?> identification, final DefaultFeatureType valueType,
            final int minimumOccurs, final int maximumOccurs)
    {
        super(identification, minimumOccurs, maximumOccurs);
        ensureNonNull("valueType", valueType);
        this.valueType = valueType;
    }

    /**
     * Constructs an association to a feature type of the given name.
     * This constructor can be used when creating a cyclic graph of {@link DefaultFeatureType} instances.
     * In such cases, at least one association needs to be created while its {@code FeatureType} is not yet available.
     *
     * <div class="note"><b>Example:</b>
     * The following establishes a bidirectional association between feature types <var>A</var> and <var>B</var>:
     *
     * {@preformat java
     *   String    namespace = "My model";
     *   GenericName nameOfA = Names.createTypeName(namespace, ":", "Feature type A");
     *   GenericName nameOfB = Names.createTypeName(namespace, ":", "Feature type B");
     *   FeatureType typeA = new DefaultFeatureType(nameOfA, false, null,
     *       new DefaultAssociationRole(Names.createLocalName("Association to B"), nameOfB),
     *       // More properties if desired.
     *   );
     *   FeatureType typeB = new DefaultFeatureType(nameOfB, false, null,
     *       new DefaultAssociationRole(Names.createLocalName("Association to A"), featureA),
     *       // More properties if desired.
     *   );
     * }
     *
     * After the above code completed, the {@linkplain #getValueType() value type} of <cite>"association to B"</cite>
     * has been automatically set to the {@code typeB} instance.
     * </div>
     *
     * Callers shall make sure that the feature types graph will not contain more than one feature of the given name.
     * If more than one {@code FeatureType} instance of the given name is found at resolution time, the selected one
     * is undetermined.
     *
     * @param identification The name and other information to be given to this association role.
     * @param valueType      The name of the type of feature values.
     * @param minimumOccurs  The minimum number of occurrences of the association within its containing entity.
     * @param maximumOccurs  The maximum number of occurrences of the association within its containing entity,
     *                       or {@link Integer#MAX_VALUE} if there is no restriction.
     */
    public DefaultAssociationRole(final Map<String,?> identification, final GenericName valueType,
            final int minimumOccurs, final int maximumOccurs)
    {
        super(identification, minimumOccurs, maximumOccurs);
        ensureNonNull("valueType", valueType);
        this.valueType = new NamedFeatureType(valueType);
    }

    /**
     * If the associated feature type is a placeholder for a {@code FeatureType} to be defined later,
     * replaces the placeholder by the actual instance if available. Otherwise do nothing.
     *
     * This method is needed only in case of cyclic graph, e.g. feature <var>A</var> has an association
     * to feature <var>B</var> which has an association back to <var>A</var>. It may also be <var>A</var>
     * having an association to itself, <i>etc.</i>
     *
     * @param  creating The feature type in process of being constructed.
     * @return {@code true} if this association references a resolved feature type after this method call.
     */
    final boolean resolve(final DefaultFeatureType creating) {
        FeatureType type = valueType;
        if (type instanceof NamedFeatureType) {
            final GenericName name = type.getName();
            if (name.equals(creating.getName())) {
                type = creating; // This is the most common case.
            } else {
                /*
                 * The feature that we need to resolve is not the one we just created. Maybe we can find
                 * this desired feature in an association of the 'creating' feature, instead than beeing
                 * the 'creating' feature itself. This is a little bit unusual, but not illegal.
                 */
                final List<DefaultFeatureType> deferred = new ArrayList<DefaultFeatureType>();
                type = search(creating, name, deferred);
                if (type == null) {
                    /*
                     * Did not found the desired FeatureType in the 'creating' instance.
                     * Try harder, by searching recursively in associations of associations.
                     */
                    if (deferred.isEmpty() || (type = deepSearch(deferred, name)) == null) {
                        return false;
                    }
                }
            }
            valueType = type;
        }
        return true;
    }

    /**
     * Searches in the given {@code feature} for an associated feature type of the given name.
     * This method does not search recursively in the associations of the associated features.
     * Such recursive search will be performed by {@link #deepSearch(List, GenericName)} only
     * if we do not find the desired feature in the most direct way.
     *
     * <p>Current implementation does not check that there is no duplicated names.
     * See {@link #deepSearch(List, GenericName)} for a rational.</p>
     *
     * @param  feature The feature in which to search.
     * @param  name The name of the feature to search.
     * @param  deferred Where to store {@code FeatureType}s to be eventually used for a deep search.
     * @return The feature of the given name, or {@code null} if none.
     */
    @SuppressWarnings("null")
    private static DefaultFeatureType search(final DefaultFeatureType feature, final GenericName name,
            final List<DefaultFeatureType> deferred)
    {
        /*
         * Search only in associations declared in the given feature, not in inherited associations.
         * The inherited associations will be checked in a separated loop below if we did not found
         * the request feature type in explicitly declared associations.
         */
        for (final AbstractIdentifiedType property : feature.getProperties(false)) {
            if (property instanceof DefaultAssociationRole) {
                final FeatureType valueType;
                valueType = ((DefaultAssociationRole) property).valueType;
                if (valueType instanceof NamedFeatureType) {
                    continue; // Skip unresolved feature types.
                }
                if (name.equals(valueType.getName())) {
                    return (DefaultFeatureType) valueType;
                }
                deferred.add((DefaultFeatureType) valueType);
            }
        }
        /*
         * Search in inherited associations as a separated step, in order to include the overridden
         * associations in the search. Overridden associations have the same association role name,
         * but not necessarily the same feature type (may be a subtype). This is equivalent to
         * "covariant return type" in the Java language.
         */
        for (DefaultFeatureType type : feature.getSuperTypes()) {
            if (name.equals(type.getName())) {
                return type;
            }
            type = search(type, name, deferred);
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    /**
     * Potentially invoked after {@link #search(FeatureType, GenericName, List)} for searching
     * in associations of associations.
     *
     * <p>Current implementation does not check that there is no duplicated names. Even if we did so,
     * a graph of feature types may have no duplicated names at this time but some duplicated names
     * later. We rather put a warning in {@link #DefaultAssociationRole(Map, GenericName, int, int)}
     * javadoc.</p>
     *
     * @param  feature The feature in which to search.
     * @param  name The name of the feature to search.
     * @param  done The feature types collected by {@link #search(FeatureType, GenericName, List)}.
     * @return The feature of the given name, or {@code null} if none.
     */
    private static DefaultFeatureType deepSearch(final List<DefaultFeatureType> deferred, final GenericName name) {
        final Map<FeatureType,Boolean> done = new IdentityHashMap<FeatureType,Boolean>(8);
        for (int i=0; i<deferred.size();) {
            DefaultFeatureType valueType = deferred.get(i++);
            if (done.put(valueType, Boolean.TRUE) == null) {
                deferred.subList(0, i).clear(); // Discard previous value for making more room.
                valueType = search(valueType, name, deferred);
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
     * <div class="warning"><b>Warning:</b> In a future SIS version, the return type may be changed
     * to {@code org.opengis.feature.FeatureType}. This change is pending GeoAPI revision.</div>
     *
     * @return The type of feature values.
     * @throws IllegalStateException if the feature type has been specified
     *         {@linkplain #DefaultAssociationRole(Map, GenericName, int, int) only by its name}
     *         and not yet resolved.
     */
    public final DefaultFeatureType getValueType() {
        /*
         * This method shall be final for consistency with other methods in this classes
         * which use the 'valueType' field directly. Furthermore, this method is invoked
         * (indirectly) by DefaultFeatureType constructors.
         */
        final FeatureType type = valueType;
        if (type instanceof NamedFeatureType) {
            throw new IllegalStateException(Errors.format(Errors.Keys.UnresolvedFeatureName_1, getName()));
        }
        return (DefaultFeatureType) type;
    }

    /**
     * Returns the name of the feature type. This information is always available
     * even when the name has not yet been {@linkplain #resolve resolved}.
     */
    static GenericName getValueTypeName(final DefaultAssociationRole role) {
        // Method is static for compatibility with branches on GeoAPI snapshots.
        return role.valueType.getName();
    }

    /**
     * Returns the name of the property to use as a title for the associated feature, or {@code null} if none.
     * This method searches for the first attribute having a value class assignable to {@link CharSequence}.
     *
     * <p><b>API note:</b> a non-static method would be more elegant in this "SIS for GeoAPI 3.0" branch.
     * However this method needs to be static in other SIS branches, because they work with interfaces
     * rather than SIS implementation. We keep the method static in this branch too for easier merges.</p>
     */
    static String getTitleProperty(final DefaultAssociationRole role) {
        String p = role.titleProperty; // No synchronization - not a big deal if computed twice.
        if (p != null) {
            return p.isEmpty() ? null : p;
        }
        p = searchTitleProperty(role);
        role.titleProperty = (p != null) ? p : "";
        return p;
    }

    /**
     * Implementation of {@link #getTitleProperty(FeatureAssociationRole)} for first search,
     * or for non-SIS {@code FeatureAssociationRole} implementations.
     */
    private static String searchTitleProperty(final DefaultAssociationRole role) {
        for (final AbstractIdentifiedType type : role.getValueType().getProperties(true)) {
            if (type instanceof DefaultAttributeType<?>) {
                final DefaultAttributeType<?> pt = (DefaultAttributeType<?>) type;
                if (pt.getMaximumOccurs() != 0 && CharSequence.class.isAssignableFrom(pt.getValueClass())) {
                    return pt.getName().toString();
                }
            }
        }
        return null;
    }

    /**
     * Returns the minimum number of occurrences of the association within its containing entity.
     * The returned value is greater than or equal to zero.
     *
     * @return The minimum number of occurrences of the association within its containing entity.
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
     * @return The maximum number of occurrences of the association within its containing entity,
     *         or {@link Integer#MAX_VALUE} if none.
     */
    @Override
    public final int getMaximumOccurs() {
        return super.getMaximumOccurs();
    }

    /**
     * Creates a new association instance of this role.
     *
     * @return A new association instance.
     *
     * @see AbstractAssociation#create(FeatureAssociationRole)
     */
    public AbstractAssociation newInstance() {
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
         * Do not use the full 'valueType' object for computing hash code,
         * because it may change before and after 'resolve' is invoked. In
         * addition, this avoid infinite recursivity in case of cyclic graph.
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
     * @return A string representation of this association role for debugging purpose.
     */
    @Debug
    @Override
    public String toString() {
        return toString("FeatureAssociationRole", this, valueType.getName()).toString();
    }
}
