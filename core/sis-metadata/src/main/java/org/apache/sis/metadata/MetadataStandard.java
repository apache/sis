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
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.ExtendedElementInformation;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.Semaphores;
import org.apache.sis.internal.system.SystemListener;
import org.apache.sis.internal.simple.SimpleCitation;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.util.ArgumentChecks.ensureNonNullElement;

// Branch-specific imports
import org.apache.sis.internal.jdk8.JDK8;
import org.apache.sis.internal.jdk8.BiFunction;


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
 * <div class="section">Defining new {@code MetadataStandard} instances</div>
 * Users should use the pre-defined constants when applicable.
 * However if new instances need to be defined, then there is a choice:
 *
 * <ul>
 *   <li>For <em>read-only</em> metadata, {@code MetadataStandard} can be instantiated directly.
 *       Only getter methods will be used and all operations that modify the metadata properties
 *       will throw an {@link UnmodifiableMetadataException}.</li>
 *   <li>For <em>read/write</em> metadata, the {@link #getImplementation(Class)}
 *       method must be overridden in a {@code MetadataStandard} subclass.</li>
 * </ul>
 *
 * <div class="section">Thread safety</div>
 * The same {@code MetadataStandard} instance can be safely used by many threads without synchronization
 * on the part of the caller. Subclasses shall make sure that any overridden methods remain safe to call
 * from multiple threads, because the same {@code MetadataStandard} instances are typically referenced
 * by a large amount of {@link ModifiableMetadata}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 *
 * @see AbstractMetadata
 */
public class MetadataStandard implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7549790450195184843L;

    /**
     * {@code true} if implementations can alter the API defined in the interfaces by
     * adding or removing properties. If {@code true}, then {@link PropertyAccessor}
     * will check for {@link Deprecated} and {@link org.opengis.annotation.UML}
     * annotations in the implementation classes in addition to the interfaces.
     *
     * <p>A value of {@code true} is useful when Apache SIS implements a newer standard
     * than GeoAPI, but have a slight performance cost at construction time. Performance
     * after construction should be the same.</p>
     */
    static final boolean IMPLEMENTATION_CAN_ALTER_API = true;

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
     * An instance working on ISO 19123 standard as defined by GeoAPI interfaces
     * in the {@link org.opengis.coverage} package and sub-packages.
     */
    public static final MetadataStandard ISO_19123;
    static {
        final String[] prefix = {"Default", "Abstract"};
        final String[] acronyms = {"CoordinateSystem", "CS", "CoordinateReferenceSystem", "CRS"};

        // If new StandardImplementation instances are added below, please update StandardImplementation.readResolve().
        ISO_19115 = new StandardImplementation("ISO 19115", "org.opengis.metadata.", "org.apache.sis.metadata.iso.", prefix, null, null);
        ISO_19111 = new StandardImplementation("ISO 19111", "org.opengis.referencing.", "org.apache.sis.referencing.", prefix, acronyms, new MetadataStandard[] {ISO_19115});
        ISO_19123 = new MetadataStandard      ("ISO 19123", "org.opengis.coverage.", new MetadataStandard[] {ISO_19111});
        INSTANCES = new MetadataStandard[] {
            ISO_19111,
            ISO_19115,
            ISO_19123
        };
        SystemListener.add(new SystemListener(Modules.METADATA) {
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
    final Citation citation;

    /**
     * The root package for metadata interfaces. Must have a trailing {@code '.'}.
     */
    final String interfacePackage;

    /**
     * The dependencies, or {@code null} if none.
     *
     * Note: the {@code null} value is for serialization compatibility.
     */
    private final MetadataStandard[] dependencies;

    /**
     * Accessors for the specified implementation classes.
     * The only legal value types are:
     *
     * <ul>
     *   <li>{@link MetadataStandard} if type is handled by {@linkplain #dependencies} rather than this standard.</li>
     *   <li>{@link Class} if we found the interface for the type but did not yet created the {@link PropertyAccessor}.</li>
     *   <li>{@link PropertyAccessor} otherwise.</li>
     * </ul>
     */
    private final transient ConcurrentMap<Class<?>, Object> accessors; // written by reflection on deserialization.

    /**
     * Creates a new instance working on implementation of interfaces defined in the specified package.
     *
     * <div class="note"><b>Example:</b>: For the ISO 19115 standard reflected by GeoAPI interfaces,
     * {@code interfacePackage} shall be the {@link org.opengis.metadata} package.</div>
     *
     * @param citation         Bibliographical reference to the international standard.
     * @param interfacePackage The root package for metadata interfaces.
     * @param dependencies     The dependencies to other metadata standards.
     */
    public MetadataStandard(final Citation citation, final Package interfacePackage, MetadataStandard... dependencies) {
        ensureNonNull("citation",         citation);
        ensureNonNull("interfacePackage", interfacePackage);
        ensureNonNull("dependencies",     dependencies);
        this.citation         = citation;
        this.interfacePackage = interfacePackage.getName() + '.';
        this.accessors        = new ConcurrentHashMap<Class<?>,Object>(); // Also defined in readObject(…)
        if (dependencies.length == 0) {
            this.dependencies = null;
        } else {
            this.dependencies = dependencies = dependencies.clone();
            for (int i=0; i<dependencies.length; i++) {
                ensureNonNullElement("dependencies", i, dependencies[i]);
            }
        }
    }

    /**
     * Creates a new instance working on implementation of interfaces defined in the
     * specified package. This constructor is used only for the pre-defined constants.
     *
     * @param citation         Bibliographical reference to the international standard.
     * @param interfacePackage The root package for metadata interfaces.
     * @param dependencies     The dependencies to other metadata standards, or {@code null} if none.
     */
    MetadataStandard(final String citation, final String interfacePackage, final MetadataStandard[] dependencies) {
        this.citation         = new SimpleCitation(citation);
        this.interfacePackage = interfacePackage;
        this.accessors        = new ConcurrentHashMap<Class<?>,Object>();
        this.dependencies     = dependencies; // No clone, since this constructor is for internal use only.
    }

    /**
     * Returns {@code true} if class or interface of the given name is supported by this standard.
     * This method verifies if the class is a member of the package given at construction time or
     * a sub-package. This method does not verify if the type is supported by a dependency.
     *
     * @param  classname The name of the type to verify.
     * @return {@code true} if the given type is supported by this standard.
     */
    final boolean isSupported(final String classname) {
        return classname.startsWith(interfacePackage);
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
        String classname = type.getName();
        for (final MetadataStandard candidate : INSTANCES) {
            if (candidate.isSupported(classname)) {
                return candidate;
            }
        }
        for (final Class<?> interf : Classes.getAllInterfaces(type)) {
            classname = interf.getName();
            for (final MetadataStandard candidate : INSTANCES) {
                if (candidate.isSupported(classname)) {
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
            standard.accessors.clear();
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
     *         of the expected package and {@code mandatory} is {@code true}.
     */
    final PropertyAccessor getAccessor(final Class<?> implementation, final boolean mandatory) {
        /*
         * Check for accessors created by previous calls to this method.
         * Values are added to this cache but never cleared.
         */
        final Object value = accessors.get(implementation);
        if (value instanceof PropertyAccessor) {
            return (PropertyAccessor) value;
        }
        /*
         * Check if we started some computation that we can finish. A partial computation exists
         * when we already found the Class<?> for the interface, but didn't created the accessor.
         */
        final Class<?> type;
        if (value instanceof Class<?>) {
            type = (Class<?>) value; // Stored result of previous call to findInterface(…).
            assert type == findInterface(implementation) : implementation;
        } else {
            /*
             * Nothing was computed, we need to start from scratch. The first step is to find
             * the interface implemented by the given class. If we can not find an interface,
             * we will delegate to the dependencies and store the result for avoiding redoing
             * this search next time.
             */
            type = findInterface(implementation);
            if (type == null) {
                if (dependencies != null) {
                    for (final MetadataStandard dependency : dependencies) {
                        final PropertyAccessor accessor = dependency.getAccessor(implementation, false);
                        if (accessor != null) {
                            accessors.put(implementation, accessor); // Ok to overwrite existing instance here.
                            return accessor;
                        }
                    }
                }
                if (mandatory) {
                    throw new ClassCastException(Errors.format(Errors.Keys.UnknownType_1, implementation));
                }
                return null;
            }
        }
        /*
         * Found the interface for which to create an accessor. Creates the accessor now, unless an accessor
         * has been created concurrently in another thread in which case the later will be returned.
         */
        return (PropertyAccessor) JDK8.compute(accessors, implementation, new BiFunction<Class<?>, Object, Object>() {
            @Override public Object apply(final Class<?> k, final Object v) {
                if (v instanceof PropertyAccessor) {
                    return v;
                }
                final PropertyAccessor accessor;
                if (SpecialCases.isSpecialCase(type)) {
                    accessor = new SpecialCases(citation, type, implementation);
                } else {
                    accessor = new PropertyAccessor(citation, type, implementation);
                }
                return accessor;
            }
        });
    }

    /**
     * Returns {@code true} if the given type is assignable to a type from this standard or one of its dependencies.
     * If this method returns {@code true}, then invoking {@link #getInterface(Class)} is guaranteed to succeed
     * without throwing an exception.
     *
     * @param  type The implementation class (can be {@code null}).
     * @return {@code true} if the given class is an interface of this standard,
     *         or implements an interface of this standard.
     */
    public boolean isMetadata(final Class<?> type) {
        if (type != null) {
            if (accessors.containsKey(type)) {
                return true;
            }
            if (dependencies != null) {
                for (final MetadataStandard dependency : dependencies) {
                    if (dependency.isMetadata(type)) {
                        accessors.putIfAbsent(type, dependency);
                        return true;
                    }
                }
            }
            /*
             * At this point, all cached values (including those in dependencies) have been checked.
             * Performs the 'findInterface' computation only in last resort. Current implementation
             * does not store negative results in order to avoid filling the cache with unrelated classes.
             */
            final Class<?> standardType = findInterface(type);
            if (standardType != null) {
                accessors.putIfAbsent(type, standardType);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the given implementation class, normally rejected by {@link #findInterface(Class)},
     * should be accepted as a pseudo-interface. We use this undocumented feature when Apache SIS experiments a new
     * API which is not yet published in GeoAPI. This happen for example when upgrading Apache SIS public API from
     * the ISO 19115:2003 standard to the ISO 19115:2014 version, but GeoAPI interfaces are still the old version.
     * In such case, API that would normally be present in GeoAPI interfaces are temporarily available only in
     * Apache SIS implementation classes.
     */
    boolean isPendingAPI(final Class<?> type) {
        return false;
    }

    /**
     * Returns the metadata interface implemented by the specified implementation.
     * Only one metadata interface can be implemented. If the given type is already
     * an interface from the standard, then it is returned directly.
     *
     * <p>This method ignores dependencies. Fallback on metadata standard dependencies shall be done by the caller.</p>
     *
     * @param  type The standard interface or the implementation class.
     * @return The single interface, or {@code null} if none where found.
     */
    private Class<?> findInterface(final Class<?> type) {
        if (type != null) {
            if (type.isInterface()) {
                if (isSupported(type.getName())) {
                    return type;
                }
            } else {
                /*
                 * Gets every interfaces from the supplied package in declaration order,
                 * including the ones declared in the super-class.
                 */
                final Set<Class<?>> interfaces = new LinkedHashSet<Class<?>>();
                for (Class<?> t=type; t!=null; t=t.getSuperclass()) {
                    getInterfaces(t, interfaces);
                }
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
                } else if (IMPLEMENTATION_CAN_ALTER_API && isPendingAPI(type)) {
                    /*
                     * Found no interface. According to our method contract we should return null.
                     * However we make an exception if the implementation class has a UML annotation.
                     * The reason is that when upgrading  API  from ISO 19115:2003 to ISO 19115:2014,
                     * implementations are provided in Apache SIS before the corresponding interfaces
                     * are published on GeoAPI. The reason why GeoAPI is slower to upgrade is that we
                     * have to go through a voting process inside the Open Geospatial Consortium (OGC).
                     * So we use those implementation classes as a temporary substitute for the interfaces.
                     */
                    return type;
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
            if (isSupported(candidate.getName())) {
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
     * <div class="note"><b>Note:</b>
     * The word "interface" may be taken in a looser sense than the usual Java sense because
     * if the given type is defined in this standard package, then it is returned unchanged.
     * The standard package is usually made of interfaces and code lists only, but this is
     * not verified by this method.</div>
     *
     * @param  <T>  The compile-time {@code type}.
     * @param  type The implementation class.
     * @return The interface implemented by the given implementation class.
     * @throws ClassCastException if the specified implementation class does
     *         not implement an interface of this standard.
     *
     * @see AbstractMetadata#getInterface()
     */
    @SuppressWarnings("unchecked")
    public <T> Class<? super T> getInterface(final Class<T> type) throws ClassCastException {
        ensureNonNull("type", type);
        final Class<?> interf;
        final Object value = accessors.get(type);
        if (value instanceof PropertyAccessor) {
            interf = ((PropertyAccessor) value).type;
        } else if (value instanceof Class<?>) {
            interf = (Class<?>) value;
        } else if (value instanceof MetadataStandard) {
            interf = ((MetadataStandard) value).getInterface(type);
        } else {
            interf = findInterface(type);
            if (interf != null) {
                accessors.putIfAbsent(type, interf);
            } else {
                if (dependencies != null) {
                    for (final MetadataStandard dependency : dependencies) {
                        if (dependency.isMetadata(type)) {
                            accessors.putIfAbsent(type, dependency);
                            return dependency.getInterface(type);
                        }
                    }
                }
                throw new ClassCastException(Errors.format(Errors.Keys.UnknownType_1, type));
            }
        }
        assert interf.isAssignableFrom(type) : type;
        return (Class<? super T>) interf;
    }

    /**
     * Returns the implementation class for the given interface, or {@code null} if none.
     * The default implementation returns {@code null} if every cases. Subclasses shall
     * override this method in order to map GeoAPI interfaces to their implementation.
     *
     * @param  <T>  The compile-time {@code type}.
     * @param  type The interface, typically from the {@code org.opengis.metadata} package.
     * @return The implementation class, or {@code null} if none.
     */
    public <T> Class<? extends T> getImplementation(final Class<T> type) {
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
     * <div class="note"><b>Example:</b>
     * The following code prints <code>"alternateTitle<u>s</u>"</code> (note the plural):
     *
     * {@preformat java
     *   MetadataStandard standard = MetadataStandard.ISO_19115;
     *   Map<String, String> names = standard.asNameMap(Citation.class, UML_IDENTIFIER, JAVABEANS_PROPERTY);
     *   String value = names.get("alternateTitle");
     *   System.out.println(value); // alternateTitles
     * }
     * </div>
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
     * <p><b>Example:</b> the following code prints the
     * {@link org.opengis.util.InternationalString} class name:</p>
     *
     * {@preformat java
     *   MetadataStandard  standard = MetadataStandard.ISO_19115;
     *   Map<String,Class<?>> types = standard.asTypeMap(Citation.class, UML_IDENTIFIER, ELEMENT_TYPE);
     *   Class<?> value = types.get("alternateTitle");
     *   System.out.println(value);  // class org.opengis.util.InternationalString
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
     *       <li>The {@linkplain Identifier#getAuthority() authority} is this metadata standard {@linkplain #getCitation() citation}.</li>
     *       <li>The {@linkplain ReferenceIdentifier#getCodeSpace() codespace} is the standard name of the interface that contain the property.</li>
     *       <li>The {@linkplain Identifier#getCode() code} is the standard name of the property.</li>
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
     * <div class="note"><b>Note:</b>
     * The rational for implementing {@code CheckedContainer} is to consider each {@code ExtendedElementInformation}
     * instance as the set of all possible values for the property. If the information had a {@code contains(E)} method,
     * it would return {@code true} if the given value is valid for that property.</div>
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
     *
     * @see org.apache.sis.metadata.iso.DefaultExtendedElementInformation
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
     * <p>The map content is determined by the arguments: {@code metadata} determines the set of
     * keys, {@code keyPolicy} determines their {@code String} representations of those keys and
     * {@code valuePolicy} determines whether entries having a null value or an empty collection
     * shall be included in the map.</p>
     *
     * <div class="section">Supported operations</div>
     * The map supports the {@link Map#put(Object, Object) put(…)} and {@link Map#remove(Object)
     * remove(…)} operations if the underlying metadata object contains setter methods.
     * The {@code remove(…)} method is implemented by a call to {@code put(…, null)}.
     * Note that whether the entry appears as effectively removed from the map or just cleared
     * (i.e. associated to a null value) depends on the {@code valuePolicy} argument.
     *
     * <div class="section">Keys and values</div>
     * The keys are case-insensitive and can be either the JavaBeans property name, the getter method name
     * or the {@linkplain org.opengis.annotation.UML#identifier() UML identifier}. The value given to a call
     * to the {@code put(…)} method shall be an instance of the type expected by the corresponding setter method,
     * or an instance of a type {@linkplain org.apache.sis.util.ObjectConverters#find(Class, Class) convertible}
     * to the expected type.
     *
     * <div class="section">Multi-values entries</div>
     * Calls to {@code put(…)} replace the previous value, with one noticeable exception: if the metadata
     * property associated to the given key is a {@link java.util.Collection} but the given value is a single
     * element (not a collection), then the given value is {@linkplain java.util.Collection#add(Object) added}
     * to the existing collection. In other words, the returned map behaves as a <cite>multi-values map</cite>
     * for the properties that allow multiple values. If the intend is to unconditionally discard all previous
     * values, then make sure that the given value is a collection when the associated metadata property expects
     * such collection.
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
     * The tree table is backed by the metadata object using Java reflection, so changes in the
     * underlying metadata object are immediately reflected in the tree table and conversely.
     *
     * <p>The returned {@code TreeTable} instance contains the following columns:</p>
     * <ul class="verbose">
     *   <li>{@link org.apache.sis.util.collection.TableColumn#IDENTIFIER}<br>
     *       The {@linkplain org.opengis.annotation.UML#identifier() UML identifier} if any,
     *       or the Java Beans property name otherwise, of a metadata property. For example
     *       in a tree table view of {@link org.apache.sis.metadata.iso.citation.DefaultCitation},
     *       there is a node having the {@code "title"} identifier.</li>
     *
     *   <li>{@link org.apache.sis.util.collection.TableColumn#INDEX}<br>
     *       If the metadata property is a collection, then the zero-based index of the element in that collection.
     *       Otherwise {@code null}. For example in a tree table view of {@code DefaultCitation}, if the
     *       {@code "alternateTitle"} collection contains two elements, then there is a node with index 0
     *       for the first element and an other node with index 1 for the second element.
     *
     *       <div class="note"><b>Note:</b>
     *       The {@code (IDENTIFIER, INDEX)} pair can be used as a primary key for uniquely identifying a node
     *       in a list of children. That uniqueness is guaranteed only for the children of a given node;
     *       the same keys may appear in the children of any other nodes.</div></li>
     *
     *   <li>{@link org.apache.sis.util.collection.TableColumn#NAME}<br>
     *       A human-readable name for the node, derived from the identifier and the index.
     *       This is the column shown in the default {@link #toString()} implementation and
     *       may be localizable.</li>
     *
     *   <li>{@link org.apache.sis.util.collection.TableColumn#TYPE}<br>
     *       The base type of the value (usually an interface).</li>
     *
     *   <li>{@link org.apache.sis.util.collection.TableColumn#VALUE}<br>
     *       The metadata value for the node. Values in this column are writable if the underlying
     *       metadata class have a setter method for the property represented by the node.</li>
     * </ul>
     *
     * <div class="section">Write operations</div>
     * Only the {@code VALUE} column may be writable, with one exception: newly created children need
     * to have their {@code IDENTIFIER} set before any other operation. For example the following code
     * adds a title to a citation:
     *
     * {@preformat java
     *     TreeTable.Node node = ...; // The node for a DefaultCitation.
     *     TreeTable.Node child = node.newChild();
     *     child.setValue(TableColumn.IDENTIFIER, "title");
     *     child.setValue(TableColumn.VALUE, "Le petit prince");
     *     // Nothing else to do - the child node has been added.
     * }
     *
     * Nodes can be removed by invoking the {@link java.util.Iterator#remove()} method on the
     * {@linkplain org.apache.sis.util.collection.TreeTable.Node#getChildren() children} iterator.
     * Note that whether the child appears as effectively removed from the node or just cleared
     * (i.e. associated to a null value) depends on the {@code valuePolicy} argument.
     *
     * @param  metadata The metadata object to view as a tree table.
     * @param  valuePolicy Whether the property having null value or empty collection shall be
     *         included in the tree.
     * @return A tree table representation of the specified metadata.
     * @throws ClassCastException if the metadata object doesn't implement a metadata
     *         interface of the expected package.
     *
     * @see AbstractMetadata#asTreeTable()
     */
    public TreeTable asTreeTable(final Object metadata, final ValueExistencePolicy valuePolicy)
            throws ClassCastException
    {
        ensureNonNull("metadata",    metadata);
        ensureNonNull("valuePolicy", valuePolicy);
        return new TreeTableView(this, metadata, valuePolicy);
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
     * Compares the two specified metadata objects.
     * The two metadata arguments shall be implementations of a metadata interface defined by
     * this {@code MetadataStandard}, otherwise an exception will be thrown. However the two
     * arguments do not need to be the same implementation class.
     *
     * <div class="section">Shallow or deep comparisons</div>
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
        final Class<?> type1 = metadata1.getClass();
        final Class<?> type2 = metadata2.getClass();
        if (type1 != type2 && mode == ComparisonMode.STRICT) {
            return false;
        }
        final PropertyAccessor accessor = getAccessor(type1, true);
        if (type1 != type2 && (!accessor.type.isAssignableFrom(type2) || accessor.type != getAccessor(type2, false).type)) {
            /*
             * Note: the check for (accessor.type != getAccessor(…).type) would have been enough, but checking
             * for isAssignableFrom(…) first can avoid the (relatively costly) creation of new PropertyAccessor.
             */
            return false;
        }
        /*
         * At this point, we have to perform the actual property-by-property comparison.
         * Cycle may exist in metadata tree, so we have to keep trace of pair in process
         * of being compared for avoiding infinite recursivity.
         */
        final ObjectPair pair = new ObjectPair(metadata1, metadata2);
        final Set<ObjectPair> inProgress = ObjectPair.CURRENT.get();
        if (inProgress.add(pair)) {
            /*
             * The NULL_COLLECTION semaphore prevents creation of new empty collections by getter methods
             * (a consequence of lazy instantiation). The intend is to avoid creation of unnecessary objects
             * for all unused properties. Users should not see behavioral difference, except if they override
             * some getters with an implementation invoking other getters. However in such cases, users would
             * have been exposed to null values at XML marshalling time anyway.
             */
            final boolean allowNull = Semaphores.queryAndSet(Semaphores.NULL_COLLECTION);
            try {
                return accessor.equals(metadata1, metadata2, mode);
            } finally {
                inProgress.remove(pair);
                if (!allowNull) {
                    Semaphores.clear(Semaphores.NULL_COLLECTION);
                }
            }
        } else {
            /*
             * If we get here, a cycle has been found. Returns 'true' in order to allow the caller to continue
             * comparing other properties. It is okay because someone else is comparing those two same objects,
             * and that later comparison will do the actual check for property values.
             */
            return true;
        }
    }

    /**
     * Computes a hash code for the specified metadata. The hash code is defined as the sum
     * of hash code values of all non-empty properties, plus the hash code of the interface.
     * This is a similar contract than {@link java.util.Set#hashCode()} (except for the interface)
     * and ensures that the hash code value is insensitive to the ordering of properties.
     *
     * @param  metadata The metadata object to compute hash code.
     * @return A hash code value for the specified metadata, or 0 if the given metadata is null.
     * @throws ClassCastException if the metadata object doesn't implement a metadata
     *         interface of the expected package.
     *
     * @see AbstractMetadata#hashCode()
     */
    public int hashCode(final Object metadata) throws ClassCastException {
        if (metadata != null) {
            final Map<Object,Object> inProgress = RecursivityGuard.HASH_CODES.get();
            if (inProgress.put(metadata, Boolean.TRUE) == null) {
                // See comment in 'equals(…) about NULL_COLLECTION semaphore purpose.
                final boolean allowNull = Semaphores.queryAndSet(Semaphores.NULL_COLLECTION);
                try {
                    return getAccessor(metadata.getClass(), true).hashCode(metadata);
                } finally {
                    inProgress.remove(metadata);
                    if (!allowNull) {
                        Semaphores.clear(Semaphores.NULL_COLLECTION);
                    }
                }
            }
            /*
             * If we get there, a cycle has been found. We can not compute a hash code value for that metadata.
             * However it should not be a problem since this metadata is part of a bigger metadata object, and
             * that enclosing object has other properties for computing its hash code. We just need the result
             * to be consistent, wich should be the case if properties ordering is always the same.
             */
        }
        return 0;
    }

    /**
     * Returns a string representation of this metadata standard.
     * This is for debugging purpose only and may change in any future version.
     */
    @Debug
    @Override
    public String toString() {
        return Classes.getShortClassName(this) + '[' + citation.getTitle() + ']';
    }

    /**
     * Assigns a {@link ConcurrentMap} instance to the given field.
     * Used on deserialization only.
     */
    @SuppressWarnings("rawtypes")
    final void setMapForField(final Class<?> classe, final String name) {
        try {
            final Field field = classe.getDeclaredField(name);
            field.setAccessible(true);
            field.set(this, new ConcurrentHashMap());
        } catch (Exception e) { // (ReflectiveOperationException) on JDK7 branch.
            throw new AssertionError(e); // Should never happen (tested by MetadataStandardTest).
        }
    }

    /**
     * Invoked during deserialization for restoring the transient fields.
     *
     * @param  in The input stream from which to deserialize a metadata standard.
     * @throws IOException If an I/O error occurred while reading or if the stream contains invalid data.
     * @throws ClassNotFoundException If the class serialized on the stream is not on the classpath.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        setMapForField(MetadataStandard.class, "accessors");
    }
}
