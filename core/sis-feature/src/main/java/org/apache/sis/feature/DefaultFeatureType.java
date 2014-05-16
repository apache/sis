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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.IdentityHashMap;
import java.util.Collection;
import java.util.Collections;
import java.io.IOException;
import java.io.ObjectInputStream;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.util.UnmodifiableArrayList;


/**
 * Abstraction of a real-world phenomena. A {@code FeatureType} instance describes the class of all
 * {@link DefaultFeature} instances of that type.
 *
 * <div class="note"><b>Note:</b>
 * Compared to the Java language, {@code FeatureType} is equivalent to {@link Class} while
 * {@code Feature} instances are equivalent to {@link Object} instances of that class.</div>
 *
 * <div class="warning"><b>Warning:</b>
 * This class is expected to implement a GeoAPI {@code FeatureType} interface in a future version.
 * When such interface will be available, most references to {@code DefaultFeatureType} in the API
 * will be replaced by references to the {@code FeatureType} interface.</div>
 *
 * {@section Naming}
 * The feature type {@linkplain #getName() name} is mandatory and should be unique. Those names are the main
 * criterion used for deciding if a feature type {@linkplain #isAssignableFrom is assignable from} another type.
 * Names can be {@linkplain org.apache.sis.util.iso.DefaultScopedName scoped} for avoiding name collision.
 *
 * {@section Properties and inheritance}
 * Each feature type can provide descriptions for the following {@link #getProperties(boolean) properties}:
 *
 * <ul>
 *   <li>{@linkplain DefaultAttributeType    Attributes}</li>
 *   <li>{@linkplain DefaultAssociationRole  Associations to other feature types}</li>
 *   <li>{@linkplain DefaultOperation        Operations}</li>
 * </ul>
 *
 * In addition, a feature type can inherit the properties of one or more other feature types.
 * Properties defined in the sub-type can override properties of the same name defined in the
 * {@link #getSuperTypes() super-types}, provided that values of the sub-type property are
 * assignable to the super-type property.
 *
 * <div class="note"><b>Analogy:</b> compared to the Java language, the above rule is similar to overriding a method
 * with a more specific return type (a.k.a. <cite>covariant return type</cite>). This is also similar to Java arrays,
 * which are implicitly <cite>covariant</cite> (i.e. {@code String[]} can be casted to {@code CharSequence[]}, which
 * is safe for read operations but not for write operations — the later may throw {@link ArrayStoreException}).</div>
 *
 * {@section Immutability and thread safety}
 * Instances of this class are immutable if all properties ({@link GenericName} and {@link InternationalString}
 * instances) and all arguments ({@link AttributeType} instances) given to the constructor are also immutable.
 * Such immutable instances can be shared by many objects and passed between threads without synchronization.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see DefaultFeature
 */
public class DefaultFeatureType extends AbstractIdentifiedType {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4357370600723922312L;

    /**
     * If {@code true}, the feature type acts as an abstract super-type.
     *
     * @see #isAbstract()
     */
    private final boolean isAbstract;

    /**
     * {@code true} if this feature type contains only attributes constrained to the [1 … 1] cardinality,
     * or operations.
     *
     * @see #isSimple()
     */
    private transient boolean isSimple;

    /**
     * The direct parents of this feature type, or an empty set if none.
     *
     * @see #getSuperTypes()
     */
    private final Set<DefaultFeatureType> superTypes;

    /**
     * The names of all parents of this feature type, including parents of parents. This is used
     * for a more efficient implementation of {@link #isAssignableFrom(DefaultFeatureType)}.
     *
     * @see #isAssignableFrom(DefaultFeatureType)
     */
    private transient Set<GenericName> assignableTo;

    /**
     * Any feature operation, any feature attribute type and any feature association role
     * that carries characteristics of a feature type.
     *
     * @see #getProperties(boolean)
     */
    private final List<PropertyType> properties;

    /**
     * A lookup table for fetching properties by name, including the properties from super-types.
     * This map shall not be modified after construction.
     *
     * @see #getProperty(String)
     */
    private transient Map<String, PropertyType> byName;

    /**
     * Indices of properties in an array of properties similar to {@link #properties},
     * but excluding operations. This map includes the properties from the super-types.
     *
     * The size of this map may be smaller than the {@link #byName} size.
     * This map shall not be modified after construction.
     */
    private transient Map<String, Integer> indices;

    /**
     * Constructs a feature type from the given properties. The identification map is given unchanged to
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
     * <div class="warning"><b>Warning:</b> In a future SIS version, the type of array elements may be
     * changed to {@code org.opengis.feature.FeatureType} and {@code org.opengis.feature.PropertyType}.
     * This change is pending GeoAPI revision. In the meantime, make sure that the {@code properties}
     * array contains only attribute types, association roles or operations, <strong>not</strong> other
     * feature types since the later are not properties in the ISO sense.</div>
     *
     * @param identification The name and other information to be given to this feature type.
     * @param isAbstract     If {@code true}, the feature type acts as an abstract super-type.
     * @param superTypes     The parents of this feature type, or {@code null} or empty if none.
     * @param properties     Any feature operation, any feature attribute type and any feature
     *                       association role that carries characteristics of a feature type.
     */
    public DefaultFeatureType(final Map<String,?> identification, final boolean isAbstract,
            final DefaultFeatureType[] superTypes, final AbstractIdentifiedType... properties)
    {
        super(identification);
        ArgumentChecks.ensureNonNull("properties", properties);
        this.isAbstract = isAbstract;
        this.superTypes = (superTypes == null) ? Collections.<DefaultFeatureType>emptySet() :
                          CollectionsExt.<DefaultFeatureType>immutableSet(true, superTypes);
        switch (properties.length) {
            case 0:  this.properties = Collections.emptyList(); break;
            case 1:  this.properties = Collections.singletonList((PropertyType) properties[0]); break;
            default: this.properties = UnmodifiableArrayList.wrap(Arrays.copyOf(properties, properties.length, PropertyType[].class)); break;
        }
        computeTransientFields();
    }

    /**
     * Invoked on deserialization for restoring the {@link #byName} and other transient fields.
     *
     * @param  in The input stream from which to deserialize a feature type.
     * @throws IOException If an I/O error occurred while reading or if the stream contains invalid data.
     * @throws ClassNotFoundException If the class serialized on the stream is not on the classpath.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        computeTransientFields();
    }

    /**
     * Computes all transient fields ({@link #assignableTo}, {@link #byName}, {@link #indices}, {@link #isSimple}).
     *
     * <p>As a side effect, this method checks for missing or duplicated names.</p>
     *
     * @throws IllegalArgumentException if two properties have the same name.
     */
    private void computeTransientFields() {
        final int capacity = Containers.hashMapCapacity(properties.size());
        isSimple     = true;
        byName       = new LinkedHashMap<>(capacity);
        indices      = new HashMap<>(capacity);
        assignableTo = new HashSet<>(4);
        assignableTo.add(getName());
        scanPropertiesFrom(this);
        byName       = CollectionsExt.unmodifiableOrCopy(byName);
        indices      = CollectionsExt.unmodifiableOrCopy(indices);
        assignableTo = CollectionsExt.unmodifiableOrCopy(assignableTo);
    }

    /**
     * Computes the transient fields using the non-transient information in the given {@code source}.
     * This method invokes itself recursively in order to use the information provided in super-types.
     */
    private void scanPropertiesFrom(final DefaultFeatureType source) {
        /*
         * Process all super-types before to process the given type. The intend is to have the
         * super-types properties indexed before the sub-types ones in the 'indices' map.
         */
        for (final DefaultFeatureType parent : source.getSuperTypes()) {
            if (assignableTo.add(parent.getName())) {
                scanPropertiesFrom(parent);
            }
        }
        int index = -1;
        Map<DefaultFeatureType,Boolean> done = null;
        for (final PropertyType property : source.properties) {
            ArgumentChecks.ensureNonNullElement("properties", ++index, property);
            /*
             * Fill the (name, property) map with opportunist verification of argument validity.
             */
            final String name = toString(property.getName(), source, index);
            final PropertyType previous = byName.put(name, property);
            if (previous != null) {
                if (done == null) {
                    done = new IdentityHashMap<>(4); // Guard against infinite recursivity.
                }
                if (!isAssignableIgnoreName(previous, property, done)) {
                    final GenericName owner = ownerOf(previous);
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.PropertyAlreadyExists_2,
                            (owner != null) ? owner : "?", name));
                }
                done.clear();
            }
            /*
             * Fill the (name, indice) map. Values are indices that the property elements would have
             * in a flat array. This block also opportunistically check if the FeatureType is "simple".
             */
            final int maximumOccurs;
            if (property instanceof DefaultAttributeType<?>) { // TODO: check for AttributeType instead (after GeoAPI upgrade).
                maximumOccurs = ((DefaultAttributeType<?>) property).getMaximumOccurs();
                if (isSimple && ((DefaultAttributeType<?>) property).getMinimumOccurs() != maximumOccurs) {
                    isSimple = false;
                }
            } else if (property instanceof FieldType) { // TODO: check for AssociationRole instead (after GeoAPI upgrade).
                maximumOccurs = ((FieldType) property).getMaximumOccurs();
                isSimple = false;
            } else {
                continue; // For feature operations, maximumOccurs is implicitly 0.
            }
            if (maximumOccurs != 0) {
                isSimple &= (maximumOccurs == 1);
                indices.put(name, indices.size());
            }
        }
    }

    /**
     * Returns the name of the feature which defines the given property, or {@code null} if not found.
     * This method is for information purpose when producing an error message - its implementation does
     * not need to be efficient.
     */
    private GenericName ownerOf(final PropertyType property) {
        if (properties.contains(property)) {
            return getName();
        }
        for (final DefaultFeatureType type : superTypes) {
            final GenericName owner = type.ownerOf(property);
            if (owner != null) {
                return owner;
            }
        }
        return null;
    }

    /**
     * Returns the string representation of the given name, making sure that the name is non-null
     * and the string non-empty. This method is used for checking argument validity.
     *
     * @param name   The name for which to get the string representation.
     * @param source The feature which contains the property (typically {@code this}).
     * @param index  Index of the property having the given name.
     */
    private String toString(final GenericName name, final DefaultFeatureType source, final int index) {
        short key = Errors.Keys.MissingValueForProperty_1;
        if (name != null) {
            final String s = name.toString();
            if (!s.isEmpty()) {
                return s;
            }
            key = Errors.Keys.EmptyProperty_1;
        }
        final StringBuilder b = new StringBuilder(30);
        if (source != this) {
            b.append(source.getName()).append('.');
        }
        throw new IllegalArgumentException(Errors.format(key,
                b.append("properties[").append(index).append("].name").toString()));
    }


    // -------- END OF CONSTRUCTORS ------------------------------------------------------------------------------


    /**
     * Returns {@code true} if the feature type acts as an abstract super-type.
     *
     * @return {@code true} if the feature type acts as an abstract super-type.
     */
    public boolean isAbstract() {
        return isAbstract;
    }

    /**
     * Returns {@code true} if this feature type contains only attributes constrained to the [1 … 1] cardinality,
     * or operations. Such feature types can be handled as a {@link org.opengis.util.Record}s.
     *
     * @return {@code true} if this feature type contains only simple attributes or operations.
     */
    public boolean isSimple() {
        return isSimple;
    }

    /**
     * Returns {@code true} if this type may be the same or a super-type of the given type, using only
     * the name as a criterion. This is a faster check than {@link #isAssignableFrom(DefaultFeatureType)}
     */
    final boolean maybeAssignableFrom(final DefaultFeatureType type) {
        return type.assignableTo.contains(getName());
    }

    /**
     * Returns {@code true} if this type is same or a super-type of the given type.
     * The check is based mainly on the feature type {@linkplain #getName() name}, which should be unique.
     * However as a safety, this method also checks that all properties in this feature type is assignable
     * from a property of the same name in the given type.
     *
     * @param  type The type to be checked.
     * @return {@code true} if instances of the given type can be assigned to association of this type.
     */
    public boolean isAssignableFrom(final DefaultFeatureType type) {
        if (type == this) {
            return true; // Optimization for a common case.
        }
        ArgumentChecks.ensureNonNull("type", type);
        return maybeAssignableFrom(type) && isAssignableIgnoreName(type, new IdentityHashMap<>(4));
    }

    /**
     * Return {@code true} if all properties in this type are also properties in the given type.
     * This method does not compare the names — this verification is presumed already done by the caller.
     *
     * @param type The type to check.
     * @param done An initially empty map to be used for avoiding infinite recursivity.
     */
    private boolean isAssignableIgnoreName(final DefaultFeatureType type, final Map<DefaultFeatureType,Boolean> done) {
        if (done.put(this, Boolean.TRUE) == null) {
            /*
             * Ensures that all properties defined in this feature type is also defined
             * in the given property, and that the former is assignable from the later.
             */
            for (final Map.Entry<String, PropertyType> entry : byName.entrySet()) {
                final PropertyType other = type.getProperty(entry.getKey());
                if (other == null || !isAssignableIgnoreName(entry.getValue(), other, done)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if instances of the {@code other} type are assignable to the given {@code base} type.
     * This method does not compare the names — this verification is presumed already done by the caller.
     */
    private static boolean isAssignableIgnoreName(final PropertyType base, final PropertyType other,
            final Map<DefaultFeatureType,Boolean> done)
    {
        if (base != other) {
            /*
             * TODO: DefaultAttributeType and DefaultAssociationRole to be replaced by GeoAPI interfaces
             *       (pending GeoAPI review).
             */
            if (base instanceof DefaultAttributeType<?>) {
                if (!(other instanceof DefaultAttributeType<?>)) {
                    return false;
                }
                final DefaultAttributeType<?> p0 = (DefaultAttributeType<?>) base;
                final DefaultAttributeType<?> p1 = (DefaultAttributeType<?>) other;
                if (!p0.getValueClass().isAssignableFrom(p1.getValueClass()) ||
                     p0.getMinimumOccurs() > p1.getMinimumOccurs() ||
                     p0.getMaximumOccurs() < p1.getMaximumOccurs())
                {
                    return false;
                }
            }
            if (base instanceof DefaultAssociationRole) {
                if (!(other instanceof DefaultAssociationRole)) {
                    return false;
                }
                final DefaultAssociationRole p0 = (DefaultAssociationRole) base;
                final DefaultAssociationRole p1 = (DefaultAssociationRole) other;
                if (p0.getMinimumOccurs() > p1.getMinimumOccurs() ||
                    p0.getMaximumOccurs() < p1.getMaximumOccurs())
                {
                    return false;
                }
                final DefaultFeatureType f0 = p0.getValueType();
                final DefaultFeatureType f1 = p1.getValueType();
                if (!f0.maybeAssignableFrom(f1) || !f0.isAssignableIgnoreName(f1, done)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns the direct parents of this feature type.
     *
     * <div class="warning"><b>Warning:</b>
     * The type of list elements will be changed to {@code FeatureType} if and when such interface
     * will be defined in GeoAPI.</div>
     *
     * @return The parents of this feature type, or an empty set if none.
     */
    public Set<DefaultFeatureType> getSuperTypes() {
        return superTypes;
    }

    /**
     * Returns any feature operation, any feature attribute type and any feature association role that
     * carries characteristics of a feature type. The returned collection will include the properties
     * inherited from the {@link #getSuperTypes() super-types} only if {@code includeSuperTypes} is
     * {@code true}.
     *
     * <div class="warning"><b>Warning:</b>
     * The type of list elements will be changed to {@code PropertyType} if and when such interface
     * will be defined in GeoAPI.</div>
     *
     * @param  includeSuperTypes {@code true} for including the properties inherited from the super-types,
     *         or {@code false} for returning only the properties defined explicitely in this type.
     * @return Feature operation, attribute type and association role that carries characteristics of this
     *         feature type (not including parent types).
     */
    public Collection<AbstractIdentifiedType> getProperties(final boolean includeSuperTypes) {
        // TODO: temporary cast to be removed after we upgraded GeoAPI.
        return (Collection) (includeSuperTypes ? byName.values() : properties);
    }

    /**
     * Returns the attribute, operation or association role for the given name.
     *
     * @param  name The name of the property to search.
     * @return The property for the given name, or {@code null} if none.
     */
    final PropertyType getProperty(final String name) {
        return byName.get(name);
    }

    /**
     * Returns the number of attributes and features (not operations) that instance will have
     * if all attributes are handled as simple attributes (maximum occurrences of 1).
     */
    final int getInstanceSize() {
        return indices.size();
    }

    /**
     * Returns a hash code value for this feature type.
     *
     * @return {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return super.hashCode() + superTypes.hashCode() + 37*properties.hashCode();
    }

    /**
     * Compares this feature type with the given object for equality.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (super.equals(obj)) {
            final DefaultFeatureType that = (DefaultFeatureType) obj;
            return isAbstract == that.isAbstract &&
                   superTypes.equals(that.superTypes) &&
                   properties.equals(that.properties);
        }
        return false;
    }

    /**
     * Formats this feature in a tabular format.
     *
     * @return A string representation of this feature in a tabular format.
     *
     * @see FeatureFormat
     */
    @Override
    public String toString() {
        return FeatureFormat.sharedFormat(this);
    }
}
