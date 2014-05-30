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

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.FeatureAssociationRole;


/**
 * Abstraction of a real-world phenomena. A {@code FeatureType} instance describes the class of all
 * {@linkplain AbstractFeature feature} instances of that type.
 *
 * <div class="note"><b>Analogy:</b>
 * compared to the Java language, {@code FeatureType} is equivalent to {@link Class} while
 * {@code Feature} instances are equivalent to {@link Object} instances of that class.</div>
 *
 * {@section Naming}
 * The feature type {@linkplain #getName() name} is mandatory and should be unique. Those names are the main
 * criterion used for deciding if a feature type {@linkplain #isAssignableFrom is assignable from} another type.
 * Names can be {@linkplain org.apache.sis.util.iso.DefaultScopedName scoped} for avoiding name collision.
 *
 * {@section Properties and inheritance}
 * Each feature type can provide descriptions for the following {@linkplain #getProperties(boolean) properties}:
 *
 * <ul>
 *   <li>{@linkplain DefaultAttributeType    Attributes}</li>
 *   <li>{@linkplain DefaultAssociationRole  Associations to other features}</li>
 *   <li>{@linkplain DefaultOperation        Operations}</li>
 * </ul>
 *
 * In addition, a feature type can inherit the properties of one or more other feature types.
 * Properties defined in the sub-type can override properties of the same name defined in the
 * {@linkplain #getSuperTypes() super-types}, provided that values of the sub-type property are
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
 * @see AbstractFeature
 */
public class DefaultFeatureType extends AbstractIdentifiedType implements FeatureType {
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
     * {@code true} if the feature instances are expected to have lot of unset properties, or
     * {@code false} if we expect most properties to be specified.
     */
    private transient boolean isSparse;

    /**
     * The direct parents of this feature type, or an empty set if none.
     *
     * @see #getSuperTypes()
     */
    private final Set<FeatureType> superTypes;

    /**
     * The names of all parents of this feature type, including parents of parents.
     * This is used for a more efficient implementation of {@link #isAssignableFrom(FeatureType)}.
     *
     * @see #isAssignableFrom(FeatureType)
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
     * All properties, including the ones declared in the super-types.
     * This is an unmodifiable view of the {@link #byName} values.
     *
     * @see #getProperties(boolean)
     */
    private transient Collection<PropertyType> allProperties;

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
     * @param identification The name and other information to be given to this feature type.
     * @param isAbstract     If {@code true}, the feature type acts as an abstract super-type.
     * @param superTypes     The parents of this feature type, or {@code null} or empty if none.
     * @param properties     Any feature operation, any feature attribute type and any feature
     *                       association role that carries characteristics of a feature type.
     */
    public DefaultFeatureType(final Map<String,?> identification, final boolean isAbstract,
            final FeatureType[] superTypes, final PropertyType... properties)
    {
        super(identification);
        ArgumentChecks.ensureNonNull("properties", properties);
        this.isAbstract = isAbstract;
        this.superTypes = (superTypes == null) ? Collections.<FeatureType>emptySet() :
                          CollectionsExt.<FeatureType>immutableSet(true, superTypes);
        switch (properties.length) {
            case 0:  this.properties = Collections.emptyList(); break;
            case 1:  this.properties = Collections.singletonList(properties[0]); break;
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
        byName       = new LinkedHashMap<String,PropertyType>(capacity);
        indices      = new LinkedHashMap<String,Integer>(capacity);
        assignableTo = new HashSet<GenericName>(4);
        assignableTo.add(getName());
        scanPropertiesFrom(this);
        byName        = compact(byName);
        assignableTo  = CollectionsExt.unmodifiableOrCopy(assignableTo);
        allProperties = byName.values();
        if (byName instanceof HashMap<?,?>) {
            allProperties = Collections.unmodifiableCollection(allProperties);
        }
        /*
         * Now check if the feature is simple/complex or dense/sparse. We perform this check after we finished
         * to create the list of all properties, because some properties may be overridden and we want to take
         * in account only the most specific ones.
         */
        isSimple = true;
        int mandatory = 0; // Count of mandatory properties.
        for (final Map.Entry<String,PropertyType> entry : byName.entrySet()) {
            final int minimumOccurs, maximumOccurs;
            final PropertyType property = entry.getValue();
            if (property instanceof AttributeType<?>) {
                minimumOccurs = ((AttributeType<?>) property).getMinimumOccurs();
                maximumOccurs = ((AttributeType<?>) property).getMaximumOccurs();
                isSimple &= (minimumOccurs == maximumOccurs);
            } else if (property instanceof FieldType) { // TODO: check for AssociationRole instead (after GeoAPI upgrade).
                minimumOccurs = ((FieldType) property).getMinimumOccurs();
                maximumOccurs = ((FieldType) property).getMaximumOccurs();
                isSimple = false;
            } else {
                continue; // For feature operations, maximumOccurs is implicitly 0.
            }
            if (maximumOccurs != 0) {
                isSimple &= (maximumOccurs == 1);
                indices.put(entry.getKey(), indices.size());
                if (minimumOccurs != 0) {
                    mandatory++;
                }
            }
        }
        indices = compact(indices);
        /*
         * Rational for choosing whether the feature is sparse: By default, java.util.HashMap implementation creates
         * an internal array of length 16 (see HashMap.DEFAULT_INITIAL_CAPACITY).  In addition, the HashMap instance
         * itself consumes approximatively 8 "words" in memory.  Consequently there is no advantage in using HashMap
         * unless the number of properties is greater than 16 + 8 (note: we could specify a smaller initial capacity,
         * but the memory consumed by each internal Map.Entry quickly exceed the few saved words). Next, the default
         * HashMap threshold is 0.75, so there is again no advantage in using HashMap if we do not expect at least 25%
         * of unused properties. Our current implementation arbitrarily sets the threshold to 50%.
         */
        final int n = indices.size();
        isSparse = (n > 24) && (mandatory <= n/2);
    }

    /**
     * Returns a more compact representation of the given map. This method is similar to
     * {@link CollectionsExt#unmodifiableOrCopy(Map)}, except that it does not wrap the
     * map in an unmodifiable view. The intend is to avoid one level of indirection for
     * performance and memory reasons (keeping in mind that we will have lot of features).
     * This is okay if we guaranteed that the map does not escape outside this class.
     */
    private static <K,V> Map<K,V> compact(final Map<K,V> map) {
        switch (map.size()) {
            case 0:  return Collections.emptyMap();
            case 1:  final Map.Entry<K,V> entry = map.entrySet().iterator().next();
                     return Collections.singletonMap(entry.getKey(), entry.getValue());
            default: return map;
        }
    }

    /**
     * Fills the {@link #byName} map using the non-transient information in the given {@code source}.
     * This method invokes itself recursively in order to use the information provided in super-types.
     * This method also performs an opportunist verification of argument validity.
     */
    private void scanPropertiesFrom(final FeatureType source) {
        for (final FeatureType parent : source.getSuperTypes()) {
            if (assignableTo.add(parent.getName())) {
                scanPropertiesFrom(parent);
            }
        }
        int index = -1;
        for (final PropertyType property : source.getProperties(false)) {
            ArgumentChecks.ensureNonNullElement("properties", ++index, property);
            final String name = toString(property.getName(), source, index);
            final PropertyType previous = byName.put(name, property);
            if (previous != null) {
                if (!isAssignableIgnoreName(previous, property)) {
                    final GenericName owner = ownerOf(this, previous);
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.PropertyAlreadyExists_2,
                            (owner != null) ? owner : "?", name));
                }
            }
        }
    }

    /**
     * Returns the name of the feature which defines the given property, or {@code null} if not found.
     * This method is for information purpose when producing an error message - its implementation does
     * not need to be efficient.
     */
    private static GenericName ownerOf(final FeatureType type, final PropertyType property) {
        if (type.getProperties(false).contains(property)) {
            return type.getName();
        }
        for (final FeatureType superType : type.getSuperTypes()) {
            final GenericName owner = ownerOf(superType, property);
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
    private String toString(final GenericName name, final FeatureType source, final int index) {
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
     * Abstract types can not be {@linkplain #newInstance() instantiated}.
     *
     * @return {@code true} if the feature type acts as an abstract super-type.
     */
    @Override
    public final boolean isAbstract() {
        return isAbstract;
    }

    /**
     * Returns {@code true} if the feature instances are expected to have lot of unset properties,
     * or {@code false} if we expect most properties to be specified.
     */
    final boolean isSparse() {
        return isSparse;
    }

    /**
     * Returns {@code true} if this feature type contains only attributes constrained to the [1 … 1] cardinality,
     * or operations. Such feature types can be handled as a {@link org.opengis.util.Record}s.
     *
     * @return {@code true} if this feature type contains only simple attributes or operations.
     */
    @Override
    public boolean isSimple() {
        return isSimple;
    }

    /**
     * Returns {@code true} if the given base type may be the same or a super-type of the given type, using only
     * the name as a criterion. This is a faster check than {@link #isAssignableFrom(FeatureType)}.
     *
     * <p>Performance note: callers should verify that {@code base != type} before to invoke this method.</p>
     */
    static boolean maybeAssignableFrom(final FeatureType base, final FeatureType type) {
        if (type instanceof DefaultFeatureType) {
            return ((DefaultFeatureType) type).assignableTo.contains(base.getName());
        }
        // Slower path for non-SIS implementations.
        if (Objects.equals(base.getName(), type.getName())) {
            return true;
        }
        for (final FeatureType superType : type.getSuperTypes()) {
            if (base == superType || maybeAssignableFrom(base, superType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if this type is same or a super-type of the given type.
     * The check is based mainly on the feature type {@linkplain #getName() name}, which should be unique.
     * However as a safety, this method also checks that all properties in this feature type is assignable
     * from a property of the same name in the given type.
     *
     * <div class="note"><b>Analogy:</b>
     * if we compare {@code FeatureType} to {@link Class} in the Java language, then this method is equivalent
     * to {@link Class#isAssignableFrom(Class)}.</div>
     *
     * @param  type The type to be checked.
     * @return {@code true} if instances of the given type can be assigned to association of this type.
     */
    @Override
    public boolean isAssignableFrom(final FeatureType type) {
        if (type == this) {
            return true; // Optimization for a common case.
        }
        ArgumentChecks.ensureNonNull("type", type);
        if (!maybeAssignableFrom(this, type)) {
            return false;
        }
        /*
         * Ensures that all properties defined in this feature type is also defined
         * in the given property, and that the former is assignable from the later.
         */
        for (final Map.Entry<String, PropertyType> entry : byName.entrySet()) {
            final PropertyType other;
            try {
                other = type.getProperty(entry.getKey());
            } catch (IllegalArgumentException e) {
                /*
                 * A property in this FeatureType does not exist in the given FeatureType.
                 * Catching exceptions is not an efficient way to perform this check, but
                 * actually this case should be rare because we verified before this loop
                 * that the names match. If the names are unique (as recommended), then
                 * this exception should never happen.
                 */
                return false;
            }
            if (!isAssignableIgnoreName(entry.getValue(), other)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if instances of the {@code other} type are assignable to the given {@code base} type.
     * This method does not compare the names — this verification is presumed already done by the caller.
     */
    private static boolean isAssignableIgnoreName(final PropertyType base, final PropertyType other) {
        if (base != other) {
            if (base instanceof AttributeType<?>) {
                if (!(other instanceof AttributeType<?>)) {
                    return false;
                }
                final AttributeType<?> p0 = (AttributeType<?>) base;
                final AttributeType<?> p1 = (AttributeType<?>) other;
                if (!p0.getValueClass().isAssignableFrom(p1.getValueClass()) ||
                     p0.getMinimumOccurs() > p1.getMinimumOccurs() ||
                     p0.getMaximumOccurs() < p1.getMaximumOccurs())
                {
                    return false;
                }
            }
            if (base instanceof FeatureAssociationRole) {
                if (!(other instanceof FeatureAssociationRole)) {
                    return false;
                }
                final FeatureAssociationRole p0 = (FeatureAssociationRole) base;
                final FeatureAssociationRole p1 = (FeatureAssociationRole) other;
                if (p0.getMinimumOccurs() > p1.getMinimumOccurs() ||
                    p0.getMaximumOccurs() < p1.getMaximumOccurs())
                {
                    return false;
                }
                final FeatureType f0 = p0.getValueType();
                final FeatureType f1 = p1.getValueType();
                if (f0 != f1) {
                    if (!f0.isAssignableFrom(f1)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Returns the direct parents of this feature type.
     *
     * <div class="note"><b>Analogy:</b>
     * if we compare {@code FeatureType} to {@link Class} in the Java language, then this method is equivalent
     * to {@link Class#getSuperclass()} except that feature types allow multi-inheritance.</div>
     *
     * @return The parents of this feature type, or an empty set if none.
     */
    @Override
    public Set<FeatureType> getSuperTypes() {
        return superTypes;
    }

    /**
     * Returns any feature operation, any feature attribute type and any feature association role that
     * carries characteristics of a feature type. The returned collection will include the properties
     * inherited from the {@linkplain #getSuperTypes() super-types} only if {@code includeSuperTypes}
     * is {@code true}.
     *
     * @param  includeSuperTypes {@code true} for including the properties inherited from the super-types,
     *         or {@code false} for returning only the properties defined explicitely in this type.
     * @return Feature operation, attribute type and association role that carries characteristics of this
     *         feature type (not including parent types).
     */
    @Override
    public Collection<PropertyType> getProperties(final boolean includeSuperTypes) {
        return includeSuperTypes ? allProperties : properties;
    }

    /**
     * Returns the attribute, operation or association role for the given name.
     *
     * @param  name The name of the property to search.
     * @return The property for the given name, or {@code null} if none.
     * @throws IllegalArgumentException If the given argument is not a property name of this feature.
     */
    @Override
    public PropertyType getProperty(final String name) throws IllegalArgumentException {
        final PropertyType pt = byName.get(name);
        if (pt != null) {
            return pt;
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.PropertyNotFound_2, getName(), name));
    }

    /**
     * Returns the map from names to indices in an array of properties.
     * This is used for {@link DenseFeature} implementation.
     */
    final Map<String,Integer> indices() {
        return indices;
    }

    /**
     * Creates a new feature instance of this type.
     *
     * <div class="note"><b>Analogy:</b>
     * if we compare {@code FeatureType} to {@link Class} and {@code Feature} to {@link Object} in the Java language,
     * then this method is equivalent to {@link Class#newInstance()}.</div>
     *
     * @return A new feature instance.
     * @throws IllegalStateException if this feature type {@linkplain #isAbstract() is abstract}.
     */
    public Feature newInstance() throws IllegalStateException {
        if (isAbstract) {
            throw new IllegalStateException(Errors.format(Errors.Keys.AbstractType_1, getName()));
        }
        return isSparse ? new SparseFeature(this) : new DenseFeature(this);
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
