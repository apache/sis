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
import java.util.LinkedHashMap;
import java.util.IdentityHashMap;
import java.util.Collection;
import java.util.Collections;
import java.io.IOException;
import java.io.ObjectInputStream;
import org.opengis.util.NameFactory;
import org.opengis.util.LocalName;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.util.UnmodifiableArrayList;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.JDK8;


/**
 * Abstraction of a real-world phenomena. A {@code FeatureType} instance describes the class of all
 * {@linkplain AbstractFeature feature} instances of that type.
 *
 * <div class="note"><b>Analogy:</b>
 * compared to the Java language, {@code FeatureType} is equivalent to {@link Class} while
 * {@code Feature} instances are equivalent to {@link Object} instances of that class.</div>
 *
 * <div class="warning"><b>Warning:</b>
 * This class is expected to implement a GeoAPI {@code FeatureType} interface in a future version.
 * When such interface will be available, most references to {@code DefaultFeatureType} in the API
 * will be replaced by references to the {@code FeatureType} interface.</div>
 *
 * <div class="section">Naming</div>
 * The feature type {@linkplain #getName() name} is mandatory and should be unique. Those names are the main
 * criterion used for deciding if a feature type {@linkplain #isAssignableFrom is assignable from} another type.
 * Names can be {@linkplain org.apache.sis.util.iso.DefaultScopedName scoped} for avoiding name collision.
 *
 * <div class="section">Properties and inheritance</div>
 * Each feature type can provide descriptions for the following {@linkplain #getProperties(boolean) properties}:
 *
 * <ul>
 *   <li>{@linkplain DefaultAttributeType    Attributes}</li>
 *   <li>{@linkplain DefaultAssociationRole  Associations to other features}</li>
 *   <li>{@linkplain AbstractOperation       Operations}</li>
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
 * <div class="section">Immutability and thread safety</div>
 * Instances of this class are immutable if all properties ({@link GenericName} and {@link InternationalString}
 * instances) and all arguments ({@link AttributeType} instances) given to the constructor are also immutable.
 * Such immutable instances can be shared by many objects and passed between threads without synchronization.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
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
     * or operations. The feature type shall not contains associations.
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
     * {@code true} if we determined that this feature type does not have, directly or indirectly,
     * any unresolved name (i.e. a {@link DefaultAssociationRole#valueType} specified only be the
     * feature type name instead than its actual instance). A value of {@code true} means that all
     * names have been resolved. However a value of {@code false} only means that we are not sure,
     * and that {@link #resolve(FeatureType)} should check again.
     *
     * <div class="note"><b>Note:</b>
     * Strictly speaking, this field should be declared {@code volatile} since the names could
     * be resolved late after construction, after the {@code DefaultFeatureType} instance became
     * used by different threads. However this is not the intended usage of deferred associations.
     * Furthermore a wrong value ({@code false} when it should be {@code true}) should only cause
     * more computation than needed, without changing the result.
     * </div>
     */
    private transient boolean isResolved;

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
    private final List<AbstractIdentifiedType> properties;

    /**
     * All properties, including the ones declared in the super-types.
     * This is an unmodifiable view of the {@link #byName} values.
     *
     * @see #getProperties(boolean)
     */
    private transient Collection<AbstractIdentifiedType> allProperties;

    /**
     * A lookup table for fetching properties by name, including the properties from super-types.
     * This map shall not be modified after construction.
     *
     * @see #getProperty(String)
     */
    private transient Map<String, AbstractIdentifiedType> byName;

    /**
     * Indices of properties in an array of properties similar to {@link #properties},
     * but excluding operations. This map includes the properties from the super-types.
     * Parameterless operations (to be handled in a special way) are identified by index -1.
     *
     * The size of this map may be smaller than the {@link #byName} size.
     * This map shall not be modified after construction.
     */
    private transient Map<String, Integer> indices;

    /**
     * Value in {@link #indices} map for parameterless operations. Those operations are not stored
     * in feature instances, but can be handled as virtual attributes computed on-the-fly.
     */
    static final Integer OPERATION_INDEX = -1;

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
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public DefaultFeatureType(final Map<String,?> identification, final boolean isAbstract,
            final DefaultFeatureType[] superTypes, final AbstractIdentifiedType... properties)
    {
        super(identification);
        ArgumentChecks.ensureNonNull("properties", properties);
        this.isAbstract = isAbstract;
        if (superTypes == null) {
            this.superTypes = Collections.emptySet();
        } else {
            this.superTypes = CollectionsExt.immutableSet(true, superTypes);
            for (final FeatureType type : this.superTypes) {
                if (type instanceof NamedFeatureType) {
                    // Hierarchy of feature types can not be cyclic.
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.UnresolvedFeatureName_1, type.getName()));
                }
            }
        }
        switch (properties.length) {
            case 0:  this.properties = Collections.emptyList(); break;
            case 1:  this.properties = Collections.singletonList(properties[0]); break;
            default: this.properties = UnmodifiableArrayList.wrap(Arrays.copyOf(properties, properties.length, AbstractIdentifiedType[].class)); break;
        }
        computeTransientFields();
        isResolved = resolve(this, null, isSimple);
    }

    /**
     * Creates a name from the given string. This method is invoked at construction time,
     * so it should not use any field in this {@code AbtractIdentifiedObject} instance.
     */
    @Override
    GenericName createName(final NameFactory factory, final String value) {
        return factory.createTypeName(null, value);
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
        isResolved = isSimple; // Conservative value. The 'resolve' method will compute a more accurate value if needed.
    }

    /**
     * Computes transient fields ({@link #assignableTo}, {@link #byName}, {@link #indices}, {@link #isSimple}).
     *
     * <p>As a side effect, this method checks for missing or duplicated names.</p>
     *
     * @throws IllegalArgumentException if two properties have the same name.
     */
    private void computeTransientFields() {
        final int capacity = Containers.hashMapCapacity(properties.size());
        byName       = new LinkedHashMap<String,AbstractIdentifiedType>(capacity);
        indices      = new LinkedHashMap<String,Integer>(capacity);
        assignableTo = new HashSet<GenericName>(4);
        assignableTo.add(super.getName());
        scanPropertiesFrom(this);
        allProperties = UnmodifiableArrayList.wrap(byName.values().toArray(new AbstractIdentifiedType[byName.size()]));
        /*
         * Now check if the feature is simple/complex or dense/sparse. We perform this check after we finished
         * to create the list of all properties, because some properties may be overridden and we want to take
         * in account only the most specific ones.
         */
        isSimple = true;
        int index = 0;
        int mandatory = 0; // Count of mandatory properties.
        for (final Map.Entry<String,AbstractIdentifiedType> entry : byName.entrySet()) {
            final int minimumOccurs, maximumOccurs;
            final AbstractIdentifiedType property = entry.getValue();
            if (property instanceof DefaultAttributeType<?>) { // Other SIS branches check for AttributeType instead.
                minimumOccurs = ((DefaultAttributeType<?>) property).getMinimumOccurs();
                maximumOccurs = ((DefaultAttributeType<?>) property).getMaximumOccurs();
                isSimple &= (minimumOccurs == maximumOccurs);
            } else if (property instanceof FieldType) { // TODO: check for AssociationRole instead (after GeoAPI upgrade).
                minimumOccurs = ((FieldType) property).getMinimumOccurs();
                maximumOccurs = ((FieldType) property).getMaximumOccurs();
                isSimple = false;
            } else {
                if (isParameterlessOperation(property)) {
                    indices.put(entry.getKey(), OPERATION_INDEX);
                }
                continue; // For feature operations, maximumOccurs is implicitly 0.
            }
            if (maximumOccurs != 0) {
                isSimple &= (maximumOccurs == 1);
                indices.put(entry.getKey(), index++);
                if (minimumOccurs != 0) {
                    mandatory++;
                }
            }
        }
        /*
         * If some properties use long name of the form "head:tip", creates short aliases containing only the "tip"
         * name for convenience, provided that it does not create ambiguity. If an short alias could map to two or
         * more properties, then this alias is not added.
         *
         * In the 'aliases' map below, null values will be assigned to ambiguous short names.
         */
        final Map<String, AbstractIdentifiedType> aliases = new LinkedHashMap<String, AbstractIdentifiedType>();
        for (final AbstractIdentifiedType property : allProperties) {
            final GenericName name = property.getName();
            final LocalName tip = name.tip();
            if (tip != name) {  // Slight optimization for a common case.
                final String key = tip.toString();
                if (key != null && !key.isEmpty() && !key.equals(name.toString())) {
                    aliases.put(key, aliases.containsKey(key) ? null : property);
                }
            }
        }
        for (final Map.Entry<String,AbstractIdentifiedType> entry : aliases.entrySet()) {
            final AbstractIdentifiedType property = entry.getValue();
            if (property != null) {
                final String tip = entry.getKey();
                if (JDK8.putIfAbsent(byName, tip, property) == null) {
                    // This block is skipped if there is properties named "tip" and "head:tip".
                    // The 'indices' value may be null if the property is an operation.
                    final Integer value = indices.get(property.getName().toString());
                    if (value != null && indices.put(tip, value) != null) {
                        throw new AssertionError(tip);  // Should never happen.
                    }
                }
            }
        }
        /*
         * Trim the collections. Especially useful when the collections have less that 2 elements.
         */
        byName       = CollectionsExt.compact(byName);
        indices      = CollectionsExt.compact(indices);
        assignableTo = CollectionsExt.unmodifiableOrCopy(assignableTo);
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
     * Fills the {@link #byName} map using the non-transient information in the given {@code source}.
     * This method invokes itself recursively in order to use the information provided in super-types.
     * This method also performs an opportunist verification of argument validity.
     *
     * <p>{@code this} shall be the instance in process of being created, not any other instance
     * (i.e. recursive method invocations are performed on the same {@code this} instance).</p>
     *
     * @param  source The feature from which to get properties.
     * @throws IllegalArgumentException if two properties have the same name.
     */
    private void scanPropertiesFrom(final DefaultFeatureType source) {
        for (final DefaultFeatureType parent : source.getSuperTypes()) {
            if (assignableTo.add(parent.getName())) {
                scanPropertiesFrom(parent);
            }
        }
        int index = -1;
        for (final AbstractIdentifiedType property : source.getProperties(false)) {
            ArgumentChecks.ensureNonNullElement("properties", ++index, property);
            final String name = toString(property.getName(), source, "properties", index);
            final AbstractIdentifiedType previous = byName.put(name, property);
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
     *
     * <p><b>API note:</b> a non-static method would be more elegant in this "SIS for GeoAPI 3.0" branch.
     * However this method needs to be static in other SIS branches, because they work with interfaces
     * rather than SIS implementation. We keep the method static in this branch too for easier merges.</p>
     */
    private static GenericName ownerOf(final DefaultFeatureType type, final AbstractIdentifiedType property) {
        if (type.getProperties(false).contains(property)) {
            return type.getName();
        }
        for (final DefaultFeatureType superType : type.getSuperTypes()) {
            final GenericName owner = ownerOf(superType, property);
            if (owner != null) {
                return owner;
            }
        }
        return null;
    }

    /**
     * If an associated feature type is a placeholder for a {@code FeatureType} to be defined later,
     * replaces the placeholder by the actual instance if available. Otherwise do nothing.
     *
     * <p>This method is needed only in case of cyclic graph, e.g. feature <var>A</var> has an association
     * to feature <var>B</var> which has an association back to <var>A</var>. It may also be <var>A</var>
     * having an association to itself, <i>etc.</i></p>
     *
     * <p>{@code this} shall be the instance in process of being created, not other instance
     * (i.e. recursive method invocations are performed on the same {@code this} instance).</p>
     *
     * @param  feature  The feature type for which to resolve the properties.
     * @param  previous Previous results, for avoiding never ending loop.
     * @return {@code true} if all names have been resolved.
     */
    private boolean resolve(final DefaultFeatureType feature, final Map<FeatureType,Boolean> previous) {
        /*
         * The isResolved field is used only as a cache for skipping completely the DefaultFeatureType instance if
         * we have determined that there is no unresolved name.
         */
        return feature.isResolved = resolve(feature, previous, feature.isResolved);
    }

    /**
     * Implementation of {@link #resolve(FeatureType, Map)}, also to be invoked from the constructor.
     *
     * @param  feature  The feature type for which to resolve the properties.
     * @param  previous Previous results, for avoiding never ending loop. Initially {@code null}.
     * @param  resolved {@code true} if we already know that all names are resolved.
     * @return {@code true} if all names have been resolved.
     */
    private boolean resolve(final DefaultFeatureType feature, Map<FeatureType,Boolean> previous, boolean resolved) {
        if (!resolved) {
            resolved = true;
            for (final DefaultFeatureType type : feature.getSuperTypes()) {
                resolved &= resolve(type, previous);
            }
            for (final AbstractIdentifiedType property : feature.getProperties(false)) {
                if (property instanceof DefaultAssociationRole) {
                    if (!((DefaultAssociationRole) property).resolve(this)) {
                        resolved = false;
                        continue;
                    }
                    /*
                     * Resolve recursively the associated features, with a check against infinite recursivity.
                     * If we fall in a loop (for example A → B → C → A), conservatively returns 'false'. This
                     * may not be the most accurate answer, but will not cause any more hurt than checking more
                     * often than necessary.
                     */
                    final DefaultFeatureType valueType = ((DefaultAssociationRole) property).getValueType();
                    if (valueType != this) {
                        if (previous == null) {
                            previous = new IdentityHashMap<FeatureType,Boolean>(8);
                        }
                        Boolean r = previous.put(valueType, Boolean.FALSE);
                        if (r == null) {
                            r = resolve(valueType, previous);
                            previous.put(valueType, r);
                        }
                        resolved &= r;
                    }
                }
            }
        }
        return resolved;
    }

    /**
     * Returns {@code true} if the given property type stands for a parameterless operation which return a result.
     *
     * @see #OPERATION_INDEX
     */
    private static boolean isParameterlessOperation(final AbstractIdentifiedType type) {
        if (type instanceof AbstractOperation) {
            final ParameterDescriptorGroup parameters = ((AbstractOperation) type).getParameters();
            return ((parameters == null) || parameters.descriptors().isEmpty())
                   && ((AbstractOperation) type).getResult() != null;
        }
        return false;
    }


    // -------- END OF CONSTRUCTORS ------------------------------------------------------------------------------


    /**
     * Returns {@code true} if the feature type acts as an abstract super-type.
     * Abstract types can not be {@linkplain #newInstance() instantiated}.
     *
     * @return {@code true} if the feature type acts as an abstract super-type.
     */
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
     * or operations (no feature association).
     * Such feature types can be handled as a {@linkplain org.apache.sis.util.iso.DefaultRecord records}.
     *
     * @return {@code true} if this feature type contains only simple attributes or operations.
     */
    public boolean isSimple() {
        return isSimple;
    }

    /**
     * Returns {@code true} if the given base type may be the same or a super-type of the given type, using only
     * the name as a criterion. This is a faster check than {@link #isAssignableFrom(DefaultFeatureType)}.
     *
     * <p>Performance note: callers should verify that {@code base != type} before to invoke this method.</p>
     *
     * <p><b>API note:</b> a non-static method would be more elegant in this "SIS for GeoAPI 3.0" branch.
     * However this method needs to be static in other SIS branches, because they work with interfaces
     * rather than SIS implementation. We keep the method static in this branch too for easier merges.</p>
     */
    static boolean maybeAssignableFrom(final DefaultFeatureType base, final DefaultFeatureType type) {
        return type.assignableTo.contains(base.getName());
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
    public boolean isAssignableFrom(final DefaultFeatureType type) {
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
        for (final Map.Entry<String, AbstractIdentifiedType> entry : byName.entrySet()) {
            final AbstractIdentifiedType other;
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
    private static boolean isAssignableIgnoreName(final AbstractIdentifiedType base, final AbstractIdentifiedType other) {
        if (base != other) {
            /*
             * Note: other SIS branches use AttributeType and FeatureAssociationRole
             *       instead than DefaultAttributeType and DefaultAssociationRole.
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
     * <div class="warning"><b>Warning:</b>
     * The type of list elements will be changed to {@code FeatureType} if and when such interface
     * will be defined in GeoAPI.</div>
     *
     * <div class="note"><b>Note for subclasses:</b>
     * this method is final because it is invoked (indirectly) by constructors, and invoking a user-overrideable
     * method at construction time is not recommended. Furthermore, many Apache SIS methods need guarantees about
     * the stability of this collection.
     * </div>
     *
     * @return The parents of this feature type, or an empty set if none.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public final Set<DefaultFeatureType> getSuperTypes() {
        return superTypes;      // Immutable
    }

    /**
     * Returns any feature operation, any feature attribute type and any feature association role that
     * carries characteristics of a feature type. The returned collection will include the properties
     * inherited from the {@linkplain #getSuperTypes() super-types} only if {@code includeSuperTypes}
     * is {@code true}.
     *
     * <div class="warning"><b>Warning:</b>
     * The type of list elements will be changed to {@code PropertyType} if and when such interface
     * will be defined in GeoAPI.</div>
     *
     * <div class="note"><b>Note for subclasses:</b>
     * this method is final because it is invoked (indirectly) by constructors, and invoking a user-overrideable
     * method at construction time is not recommended. Furthermore, many Apache SIS methods need guarantees about
     * the stability of this collection.
     * </div>
     *
     * @param  includeSuperTypes {@code true} for including the properties inherited from the super-types,
     *         or {@code false} for returning only the properties defined explicitely in this type.
     * @return Feature operation, attribute type and association role that carries characteristics of this
     *         feature type (not including parent types).
     */
    @Override
    public final Collection<AbstractIdentifiedType> getProperties(final boolean includeSuperTypes) {
        return includeSuperTypes ? allProperties : properties;
    }

    /**
     * Returns the attribute, operation or association role for the given name.
     *
     * <div class="warning"><b>Warning:</b>
     * The type of returned element will be changed to {@code PropertyType} if and when such interface
     * will be defined in GeoAPI.</div>
     *
     * @param  name The name of the property to search.
     * @return The property for the given name, or {@code null} if none.
     * @throws IllegalArgumentException If the given argument is not a property name of this feature.
     *
     * @see AbstractFeature#getProperty(String)
     */
    public AbstractIdentifiedType getProperty(final String name) throws IllegalArgumentException {
        final AbstractIdentifiedType pt = byName.get(name);
        if (pt != null) {
            return pt;
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.PropertyNotFound_2, getName(), name));
    }

    /**
     * Returns the map from names to indices in an array of properties.
     * This is used for {@link DenseFeature} implementation.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
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
    public AbstractFeature newInstance() throws IllegalStateException {
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
