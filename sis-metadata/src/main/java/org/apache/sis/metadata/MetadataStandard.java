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
package org.apache.sis.metadata;

import java.util.Set;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Iterator;
import net.jcip.annotations.ThreadSafe;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.ExtendedElementInformation;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.internal.util.SystemListener;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * Enumeration of some metadata standards. A standard is defined by a set of Java interfaces
 * in a specific package or sub-packages. For example the {@linkplain #ISO_19115 ISO 19115}
 * standard is defined by <a href="http://www.geoapi.org">GeoAPI</a> interfaces in the
 * {@link org.opengis.metadata} package and sub-packages.
 *
 * <p>This class provides some methods operating on metadata instances through
 * {@linkplain java.lang.reflect Java reflection}. The following rules are assumed:</p>
 *
 * <ul>
 *   <li>Metadata properties are defined by the collection of following getter methods found
 *       <strong>in the interface</strong>, ignoring implementation methods:
 *       <ul>
 *         <li>{@code get*()} methods with arbitrary return type;</li>
 *         <li>or {@code is*()} methods with boolean return type.</li>
 *       </ul></li>
 *   <li>All properties are <cite>readable</cite>.</li>
 *   <li>A property is also <cite>writable</cite> if a {@code set*(…)} method is defined
 *       <strong>in the implementation class</strong> for the corresponding getter method.
 *       The setter method doesn't need to be defined in the interface.</li>
 * </ul>
 *
 * An instance of {@code MetadataStandard} is associated to every {@link AbstractMetadata} objects.
 * The {@code AbstractMetadata} base class usually form the basis of ISO 19115 implementations but
 * can also be used for other standards.
 *
 * {@section Defining new <code>MetadataStandard</code> instances}
 * Users should use the pre-defined constants when applicable.
 * However if new instances need to be defined, then there is a choice:
 *
 * <ul>
 *   <li>For <em>read-only</em> metadata, {@code MetadataStandard} can be instantiated directly.
 *       Only getter methods will be used and all operations that modify the metadata properties
 *       will throw an {@link UnmodifiableMetadataException}.</li>
 *   <li>For <em>read/write</em> metadata, the {@link #getImplementation(Class)}
 *       method must be overridden in a {@code MetadataStandard} subclass.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
@ThreadSafe
public class MetadataStandard {
    /**
     * Metadata instances defined in this class. The current implementation does not yet
     * contains the user-defined instances. However this may be something we will need to
     * do in the future.
     */
    private static final MetadataStandard[] INSTANCES;

    /**
     * An instance working on ISO 19111 standard as defined by GeoAPI interfaces
     * in the {@link org.opengis.referencing} package and sub-packages.
     */
    public static final MetadataStandard ISO_19111;

    /**
     * An instance working on ISO 19115 standard as defined by GeoAPI interfaces
     * in the {@link org.opengis.metadata} package and sub-packages.
     */
    public static final MetadataStandard ISO_19115;

    /**
     * An instance working on ISO 19119 standard as defined by GeoAPI interfaces
     * in the {@link org.opengis.service} package and sub-packages.
     */
    public static final MetadataStandard ISO_19119;

    /**
     * An instance working on ISO 19123 standard as defined by GeoAPI interfaces
     * in the {@link org.opengis.coverage} package and sub-packages.
     */
    public static final MetadataStandard ISO_19123;
    static {
        final String[] prefix = {"Default", "Abstract"};
        final String[] acronyms = {"CoordinateSystem", "CS", "CoordinateReferenceSystem", "CRS"};
        ISO_19111 = new StandardImplementation("ISO 19111", "org.opengis.referencing.", "org.apache.sis.referencing.", prefix, acronyms);
        ISO_19115 = new StandardImplementation("ISO 19115", "org.opengis.metadata.", "org.apache.sis.metadata.iso.", prefix, null);
        ISO_19119 = new StandardImplementation("ISO 19119", "org.opengis.service.",  null, null, null);
        ISO_19123 = new StandardImplementation("ISO 19123", "org.opengis.coverage.", null, null, null);
        INSTANCES = new MetadataStandard[] {
            ISO_19111,
            ISO_19115,
            ISO_19119,
            ISO_19123
        };
        SystemListener.add(new SystemListener() {
            @Override protected void classpathChanged() {
                clearCache();
            }
        });
    }

    /**
     * Bibliographical reference to the international standard.
     *
     * @see #getCitation()
     */
    private final Citation citation;

    /**
     * The root packages for metadata interfaces. Must have a trailing {@code '.'}.
     */
    final String interfacePackage;

    /**
     * Accessors for the specified implementations.
     * The only legal value types are:
     *
     * <ul>
     *   <li>{@link Class} if we have determined the standard interface for a given type
     *       but did not yet created the {@link PropertyAccessor} for it.</li>
     *   <li>{@link PropertyAccessor} otherwise.</li>
     * </ul>
     */
    private final Map<Class<?>, Object> accessors;

    /**
     * Creates a new instance working on implementation of interfaces defined in the specified package.
     *
     * <p><b>Example:</b>: For the ISO 19115 standard reflected by GeoAPI interfaces,
     * {@code interfacePackage} shall be the {@link org.opengis.metadata} package.</p>
     *
     * @param citation         Bibliographical reference to the international standard.
     * @param interfacePackage The root package for metadata interfaces.
     */
    public MetadataStandard(final Citation citation, final Package interfacePackage) {
        ensureNonNull("citation", citation);
        ensureNonNull("interfacePackage", interfacePackage);
        this.citation         = citation;
        this.interfacePackage = interfacePackage.getName() + '.';
        this.accessors        = new IdentityHashMap<Class<?>,Object>();
    }

    /**
     * Creates a new instance working on implementation of interfaces defined in the
     * specified package. This constructor is used only for the pre-defined constants.
     *
     * @param citation         Bibliographical reference to the international standard.
     * @param interfacePackage The root package for metadata interfaces.
     */
    MetadataStandard(final Citation citation, final String interfacePackage) {
        this.citation         = citation;
        this.interfacePackage = interfacePackage;
        this.accessors        = new IdentityHashMap<Class<?>,Object>();
    }

    /**
     * Returns the metadata standard for the given class. The argument given to this method can be
     * either an interface defined by the standard, or a class implementing such interface. If the
     * class implements more than one interface, then the first interface recognized by this method,
     * in declaration order, will be retained.
     *
     * <p>The current implementation recognizes only the standards defined by the public static
     * constants defined in this class. A future SIS version may recognize user-defined constants.</p>
     *
     * @param  type The metadata standard interface, or an implementation class.
     * @return The metadata standard for the given type, or {@code null} if not found.
     */
    public static MetadataStandard forClass(final Class<?> type) {
        String name = type.getName();
        for (final MetadataStandard candidate : INSTANCES) {
            if (name.startsWith(candidate.interfacePackage)) {
                return candidate;
            }
        }
        for (final Class<?> interf : Classes.getAllInterfaces(type)) {
            name = interf.getName();
            for (final MetadataStandard candidate : INSTANCES) {
                if (name.startsWith(candidate.interfacePackage)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Clears the cache of accessors. This method is invoked when the classpath changed,
     * in order to discard the references to classes that may need to be unloaded.
     */
    static void clearCache() {
        for (final MetadataStandard standard : INSTANCES) {
            synchronized (standard.accessors) {
                standard.accessors.clear();
            }
        }
    }

    /**
     * Returns a bibliographical reference to the international standard.
     * The default implementation return the citation given at construction time.
     *
     * @return Bibliographical reference to the international standard.
     */
    public Citation getCitation() {
        return citation;
    }

    /**
     * Returns the accessor for the specified implementation class, or {@code null} if none.
     * The given class shall not be the standard interface, unless the metadata is read-only.
     * More specifically, the given {@code type} shall be one of the following:
     *
     * <ul>
     *   <li>The value of {@code metadata.getClass()};</li>
     *   <li>The value of {@link #getImplementation(Class)} after check for non-null value.</li>
     * </ul>
     *
     * @param  implementation The implementation class.
     * @param  mandatory Whether this method shall throw an exception or return {@code null}
     *         if no accessor is found for the given implementation class.
     * @return The accessor for the given implementation, or {@code null} if the given class does
     *         not implement a metadata interface of the expected package and {@code mandatory}
     *         is {@code false}.
     * @throws ClassCastException if the specified class does not implement a metadata interface
     *         of the expected package and {@code mandatory} is {@code true}.
     */
    final PropertyAccessor getAccessor(final Class<?> implementation, final boolean mandatory) {
        synchronized (accessors) {
            // Check for previously created accessors.
            final Object value = accessors.get(implementation);
            if (value instanceof PropertyAccessor) {
                return (PropertyAccessor) value;
            }
            // Check if we started some computation that we can finish.
            final Class<?> type;
            if (value != null) {
                type = (Class<?>) value;
            } else {
                // Nothing were computed. Try to compute now.
                type = findInterface(implementation);
                if (type == null) {
                    if (mandatory) {
                        throw new ClassCastException(Errors.format(Errors.Keys.UnknownType_1, implementation));
                    }
                    return null;
                }
            }
            final PropertyAccessor accessor = new PropertyAccessor(citation, type, implementation);
            accessors.put(implementation, accessor);
            return accessor;
        }
    }

    /**
     * Returns {@code true} if the given type is assignable to a type from this standard.
     * If this method returns {@code true}, then invoking {@link #getInterface(Class)} is
     * guaranteed to succeed without throwing an exception.
     *
     * @param  type The implementation class (can be {@code null}).
     * @return {@code true} if the given class is an interface of this standard,
     *         or implements an interface of this standard.
     */
    public boolean isMetadata(final Class<?> type) {
        if (type != null) {
            synchronized (accessors) {
                if (accessors.containsKey(type)) {
                    return true;
                }
                final Class<?> standard = findInterface(type);
                if (standard != null) {
                    accessors.put(type, standard);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the metadata interface implemented by the specified implementation.
     * Only one metadata interface can be implemented. If the given type is already
     * an interface from the standard, then it is returned directly.
     *
     * @param  type The standard interface or the implementation class.
     * @return The single interface, or {@code null} if none where found.
     */
    private Class<?> findInterface(Class<?> type) {
        if (type != null) {
            if (type.isInterface()) {
                if (type.getName().startsWith(interfacePackage)) {
                    return type;
                }
            } else {
                /*
                 * Gets every interfaces from the supplied package in declaration order,
                 * including the ones declared in the super-class.
                 */
                final Set<Class<?>> interfaces = new LinkedHashSet<Class<?>>();
                do {
                    getInterfaces(type, interfaces);
                    type = type.getSuperclass();
                } while (type != null);
                /*
                 * If we found more than one interface, removes the
                 * ones that are sub-interfaces of the other.
                 */
                for (final Iterator<Class<?>> it=interfaces.iterator(); it.hasNext();) {
                    final Class<?> candidate = it.next();
                    for (final Class<?> child : interfaces) {
                        if (candidate != child && candidate.isAssignableFrom(child)) {
                            it.remove();
                            break;
                        }
                    }
                }
                final Iterator<Class<?>> it = interfaces.iterator();
                if (it.hasNext()) {
                    final Class<?> candidate = it.next();
                    if (!it.hasNext()) {
                        return candidate;
                    }
                    // Found more than one interface; we don't know which one to pick.
                    // Returns 'null' for now; the caller will thrown an exception.
                }
            }
        }
        return null;
    }

    /**
     * Puts every interfaces for the given type in the specified collection.
     * This method invokes itself recursively for scanning parent interfaces.
     *
     * @see Classes#getAllInterfaces(Class)
     */
    private void getInterfaces(final Class<?> type, final Collection<Class<?>> interfaces) {
        for (final Class<?> candidate : type.getInterfaces()) {
            if (candidate.getName().startsWith(interfacePackage)) {
                interfaces.add(candidate);
            }
            getInterfaces(candidate, interfaces);
        }
    }

    /**
     * Returns the metadata interface implemented by the specified implementation class.
     * If the given type is already an interface from this standard, then it is returned
     * unchanged.
     *
     * {@note The word "interface" may be taken in a looser sense than the usual Java sense
     *        because if the given type is defined in this standard package, then it is returned
     *        unchanged. The standard package is usually made of interfaces and code lists only,
     *        but this is not verified by this method.}
     *
     * @param  type The implementation class.
     * @return The interface implemented by the given implementation class.
     * @throws ClassCastException if the specified implementation class does
     *         not implement an interface of this standard.
     *
     * @see AbstractMetadata#getInterface()
     */
    public Class<?> getInterface(final Class<?> type) throws ClassCastException {
        ensureNonNull("type", type);
        synchronized (accessors) {
            final Object value = accessors.get(type);
            if (value != null) {
                if (value instanceof PropertyAccessor) {
                    return ((PropertyAccessor) value).type;
                }
                return (Class<?>) value;
            }
            final Class<?> standard = findInterface(type);
            if (standard == null) {
                throw new ClassCastException(Errors.format(Errors.Keys.UnknownType_1, type));
            }
            accessors.put(type, standard);
            return standard;
        }
    }

    /**
     * Returns the implementation class for the given interface, or {@code null} if none.
     * The default implementation returns {@code null} if every cases. Subclasses shall
     * override this method in order to map GeoAPI interfaces to their implementation.
     *
     * @param  type The interface, typically from the {@code org.opengis.metadata} package.
     * @return The implementation class, or {@code null} if none.
     */
    protected Class<?> getImplementation(final Class<?> type) {
        return null;
    }

    /**
     * Returns the names of all properties defined in the given metadata type.
     * The property names appears both as keys and as values, but may be written differently.
     * The names may be {@linkplain KeyNamePolicy#UML_IDENTIFIER standard identifiers} (e.g.
     * as defined by ISO 19115), {@linkplain KeyNamePolicy#JAVABEANS_PROPERTY JavaBeans names},
     * {@linkplain KeyNamePolicy#METHOD_NAME method names} or {@linkplain KeyNamePolicy#SENTENCE
     * sentences} (usually in English).
     *
     * <p><b>Example:</b> The {@code value} in the following code is
     * <code>"alternateTitle<u>s</u>"</code> (note the plural):</p>
     *
     * {@preformat java
     *   MetadataStandard standard = MetadataStandard.ISO_19115;
     *   Map<String, String> names = standard.asNameMap(Citation.class, UML_IDENTIFIER, JAVABEANS_PROPERTY);
     *   String value = names.get("alternateTitle");  // alternateTitles
     * }
     *
     * The {@code keyPolicy} argument specify only the string representation of keys returned by the iterators.
     * No matter the key name policy, the {@code key} argument given to any {@link Map} method can be any of the
     * above-cited forms of property names.
     *
     * @param  type        The interface or implementation class of a metadata.
     * @param  keyPolicy   Determines the string representation of map keys.
     * @param  valuePolicy Determines the string representation of map values.
     * @return The names of all properties defined by the given metadata type.
     * @throws ClassCastException if the specified interface or implementation class does
     *         not extend or implement a metadata interface of the expected package.
     */
    public Map<String,String> asNameMap(Class<?> type, final KeyNamePolicy keyPolicy,
            final KeyNamePolicy valuePolicy) throws ClassCastException
    {
        ensureNonNull("type",        type);
        ensureNonNull("keyPolicy",   keyPolicy);
        ensureNonNull("valuePolicy", valuePolicy);
        final Class<?> implementation = getImplementation(type);
        if (implementation != null) {
            type = implementation;
        }
        return new NameMap(getAccessor(type, true), keyPolicy, valuePolicy);
    }

    /**
     * Returns the type of all properties, or their declaring type, defined in the given
     * metadata type. The keys in the returned map are the same than the keys in the above
     * {@linkplain #asNameMap name map}. The values are determined by the {@code valuePolicy}
     * argument, which can be {@linkplain TypeValuePolicy#ELEMENT_TYPE element type} or the
     * {@linkplain TypeValuePolicy#DECLARING_INTERFACE declaring interface} among others.
     *
     * <p><b>Example:</b> The {@code value} in the following code is {@code InternationalString.class}:</p>
     *
     * {@preformat java
     *   MetadataStandard  standard = MetadataStandard.ISO_19115;
     *   Map<String,Class<?>> types = standard.asTypeMap(Citation.class, UML_IDENTIFIER, ELEMENT_TYPE);
     *   Class<?> value = names.get("alternateTitle");  // InternationalString.class
     * }
     *
     * @param  type        The interface or implementation class of a metadata.
     * @param  keyPolicy   Determines the string representation of map keys.
     * @param  valuePolicy Whether the values shall be property types, the element types
     *         (same as property types except for collections) or the declaring interface or class.
     * @return The types or declaring type of all properties defined in the given metadata type.
     * @throws ClassCastException if the specified interface or implementation class does
     *         not extend or implement a metadata interface of the expected package.
     */
    public Map<String,Class<?>> asTypeMap(Class<?> type, final KeyNamePolicy keyPolicy,
            final TypeValuePolicy valuePolicy) throws ClassCastException
    {
        ensureNonNull("type",        type);
        ensureNonNull("keyPolicy",   keyPolicy);
        ensureNonNull("valuePolicy", valuePolicy);
        final Class<?> implementation = getImplementation(type);
        if (implementation != null) {
            type = implementation;
        }
        return new TypeMap(getAccessor(type, true), keyPolicy, valuePolicy);
    }

    /**
     * Returns information about all properties defined in the given metadata type.
     * The keys in the returned map are the same than the keys in the above
     * {@linkplain #asNameMap name map}. The values contain information inferred from
     * the ISO names, the {@link org.opengis.annotation.Obligation} enumeration and the
     * {@link org.apache.sis.measure.ValueRange} annotations.
     *
     * <p>In the particular case of Apache SIS implementation, all values in the information map
     * additionally implement the following interfaces:</p>
     * <ul>
     *   <li>{@link ReferenceIdentifier} with the following properties:
     *     <ul>
     *       <li>The {@linkplain ReferenceIdentifier#getAuthority() authority} is this metadata standard {@linkplain #getCitation() citation}.</li>
     *       <li>The {@linkplain ReferenceIdentifier#getCodeSpace() codespace} is the standard name of the interface that contain the property.</li>
     *       <li>The {@linkplain ReferenceIdentifier#getCode() code} is the standard name of the property.</li>
     *     </ul>
     *   </li>
     *   <li>{@link CheckedContainer} with the following properties:
     *     <ul>
     *       <li>The {@linkplain CheckedContainer#getElementType() element type} is the type of property values
     *           as defined by {@link TypeValuePolicy#ELEMENT_TYPE}.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * In addition, for each map entry the value returned by {@link ExtendedElementInformation#getDomainValue()}
     * may optionally be an instance of any of the following classes:
     *
     * <ul>
     *   <li>{@link org.apache.sis.measure.NumberRange} if the valid values are constrained to some specific range.</li>
     * </ul>
     *
     * @param  type      The metadata interface or implementation class.
     * @param  keyPolicy Determines the string representation of map keys.
     * @return Information about all properties defined in the given metadata type.
     * @throws ClassCastException if the given type doesn't implement a metadata
     *         interface of the expected package.
     */
    public Map<String,ExtendedElementInformation> asInformationMap(Class<?> type,
            final KeyNamePolicy keyPolicy) throws ClassCastException
    {
        ensureNonNull("type",     type);
        ensureNonNull("keyNames", keyPolicy);
        final Class<?> implementation = getImplementation(type);
        if (implementation != null) {
            type = implementation;
        }
        return new InformationMap(getAccessor(type, true), keyPolicy);
    }

    /**
     * Returns a view of the specified metadata object as a {@link Map}.
     * The map is backed by the metadata object using Java reflection, so changes in the
     * underlying metadata object are immediately reflected in the map and conversely.
     *
     * <p>The map content is determined by the arguments: {@code metadata} determines the set of
     * keys, {@code keyPolicy} determines their {@code String} representations of those keys and
     * {@code valuePolicy} determines whether entries having a null value or an empty collection
     * shall be included in the map.</p>
     *
     * <p>The map supports the {@link Map#put(Object, Object) put(…)} and {@link Map#remove(Object)
     * remove(…)} operations if the underlying metadata object contains setter methods.
     * The keys are case-insensitive and can be either the JavaBeans property name or
     * the UML identifier.</p>
     *
     * @param  metadata The metadata object to view as a map.
     * @param  keyPolicy Determines the string representation of map keys.
     * @param  valuePolicy Whether the entries having null value or empty collection shall be
     *         included in the map.
     * @return A map view over the metadata object.
     * @throws ClassCastException if the metadata object doesn't implement a metadata
     *         interface of the expected package.
     *
     * @see AbstractMetadata#asMap()
     */
    public Map<String,Object> asValueMap(final Object metadata, final KeyNamePolicy keyPolicy,
            final ValueExistencePolicy valuePolicy) throws ClassCastException
    {
        ensureNonNull("metadata",    metadata);
        ensureNonNull("keyPolicy",   keyPolicy);
        ensureNonNull("valuePolicy", valuePolicy);
        return new ValueMap(metadata, getAccessor(metadata.getClass(), true), keyPolicy, valuePolicy);
    }

    /**
     * Returns the specified metadata object as a tree table.
     * In the current implementation, the tree is not live (i.e. changes in metadata are not
     * reflected in the tree). However it may be improved in a future SIS implementation.
     *
     * @param  metadata The metadata object to formats as a tree table.
     * @return A tree table representation of the specified metadata.
     * @throws ClassCastException if the metadata object doesn't implement a metadata
     *         interface of the expected package.
     *
     * @see AbstractMetadata#asTreeTable()
     */
    public TreeTable asTreeTable(final Object metadata) throws ClassCastException {
        throw new UnsupportedOperationException("Not yet implemented"); // TODO
    }

    /**
     * Replaces every properties in the specified metadata by their
     * {@linkplain ModifiableMetadata#unmodifiable() unmodifiable variant}.
     *
     * @throws ClassCastException if the specified implementation class do
     *         not implements a metadata interface of the expected package.
     *
     * @see ModifiableMetadata#freeze()
     */
    final void freeze(final Object metadata) throws ClassCastException {
        getAccessor(metadata.getClass(), true).freeze(metadata);
    }

    /**
     * Copies all metadata from source to target.
     * The source must implements the same metadata interface than the target.
     *
     * <p>If the source contains any null or empty properties, then those properties will
     * <strong>not</strong> overwrite the corresponding properties in the destination metadata.</p>
     *
     * @param  source The metadata to copy.
     * @param  target The target metadata.
     * @throws ClassCastException if the source or target object don't
     *         implements a metadata interface of the expected package.
     * @throws UnmodifiableMetadataException if the target metadata is unmodifiable,
     *         or if at least one setter method was required but not found.
     *
     * @see ModifiableMetadata#clone()
     */
    public void shallowCopy(final Object source, final Object target)
            throws ClassCastException, UnmodifiableMetadataException
    {
        ensureNonNull("target", target);
        final PropertyAccessor accessor = getAccessor(target.getClass(), true);
        if (!accessor.type.isInstance(source)) {
            ensureNonNull("source", source);
            throw new ClassCastException(Errors.format(Errors.Keys.IllegalArgumentClass_3,
                    "source", accessor.type, source.getClass()));
        }
        if (!accessor.shallowCopy(source, target)) {
            throw new UnmodifiableMetadataException(Errors.format(Errors.Keys.UnmodifiableMetadata));
        }
    }

    /**
     * Compares the two specified metadata objects.
     * The two metadata arguments shall be implementations of a metadata interface defined by
     * this {@code MetadataStandard}, otherwise an exception will be thrown. However the two
     * arguments do not need to be the same implementation class.
     *
     * {@section Shallow or deep comparisons}
     * This method implements a <cite>shallow</cite> comparison in that properties are compared by
     * invoking their {@code properties.equals(…)} method without <em>explicit</em> recursive call
     * to this {@code standard.equals(…)} method for children metadata. However the comparison will
     * do <em>implicit</em> recursive calls if the {@code properties.equals(…)} implementations
     * delegate their work to this {@code standard.equals(…)} method, as {@link AbstractMetadata} does.
     * In the later case, the final result is a deep comparison.
     *
     * @param metadata1 The first metadata object to compare.
     * @param metadata2 The second metadata object to compare.
     * @param mode      The strictness level of the comparison.
     * @return {@code true} if the given metadata objects are equals.
     * @throws ClassCastException if at least one metadata object don't
     *         implements a metadata interface of the expected package.
     *
     * @see AbstractMetadata#equals(Object, ComparisonMode)
     */
    public boolean equals(final Object metadata1, final Object metadata2,
            final ComparisonMode mode) throws ClassCastException
    {
        if (metadata1 == metadata2) {
            return true;
        }
        if (metadata1 == null || metadata2 == null) {
            return false;
        }
        final PropertyAccessor accessor = getAccessor(metadata1.getClass(), true);
        if (accessor.type != findInterface(metadata2.getClass())) {
            return false;
        }
        return accessor.equals(metadata1, metadata2, mode);
    }

    /**
     * Computes a hash code for the specified metadata. The hash code is defined as the sum
     * of hash code values of all non-empty properties, plus the hash code of the interface.
     * This is a similar contract than {@link java.util.Set#hashCode()} (except for the interface)
     * and ensures that the hash code value is insensitive to the ordering of properties.
     *
     * @param  metadata The metadata object to compute hash code.
     * @return A hash code value for the specified metadata.
     * @throws ClassCastException if the metadata object doesn't implement a metadata
     *         interface of the expected package.
     *
     * @see AbstractMetadata#hashCode()
     */
    public int hashCode(final Object metadata) throws ClassCastException {
        return getAccessor(metadata.getClass(), true).hashCode(metadata);
    }

    /**
     * Returns a string representation of this metadata standard.
     * This is for debugging purpose only and may change in any future version.
     */
    @Override
    public String toString() {
        return Classes.getShortClassName(this) + '[' + citation.getTitle() + ']';
    }
}
