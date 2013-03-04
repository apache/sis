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
import java.util.Arrays;
import java.util.Locale;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Collection;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import net.jcip.annotations.ThreadSafe;
import org.opengis.annotation.UML;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.xml.IdentifiedObject;

import static org.apache.sis.util.collection.CollectionsExt.modifiableCopy;
import static org.apache.sis.util.collection.CollectionsExt.hashMapCapacity;
import static org.apache.sis.internal.util.Utilities.floatEpsilonEqual;


/**
 * The getter methods declared in a GeoAPI interface, together with setter methods (if any)
 * declared in the SIS implementation. An instance of {@code PropertyAccessor} gives access
 * to all public attributes of an instance of a metadata object. It uses reflection for this
 * purpose, a little bit like the <cite>Java Beans</cite> framework.
 *
 * <p>This accessor groups the properties in two categories:</p>
 *
 * <ul>
 *   <li>The standard properties defined by the GeoAPI (or other standard) interfaces.
 *       Those properties are the only one accessible by most methods in this class,
 *       except {@link #equals(Object, Object, ComparisonMode, boolean)},
 *       {@link #shallowCopy(Object, Object)} and {@link #freeze(Object)}.</li>
 *
 *   <li>Extra properties defined by the {@link IdentifiedObject} interface. Those properties
 *       invisible in the ISO 19115 model, but appears in ISO 19139 XML marshalling. So we do
 *       the same in the SIS implementation: invisible in map and tree view, but visible in
 *       XML marshalling.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
@ThreadSafe
final class PropertyAccessor {
    /**
     * The prefix for getters on boolean values.
     */
    private static final String IS = "is";

    /**
     * The prefix for getters (general case).
     */
    private static final String GET = "get";

    /**
     * The prefix for setters.
     */
    private static final String SET = "set";

    /**
     * Getters shared between many instances of this class. Two different implementations
     * may share the same getters but different setters.
     *
     * {@note In the particular case of <code>Class</code> keys, <code>IdentityHashMap</code> and
     *        <code>HashMap</code> have identical behavior since <code>Class</code> is final and
     *        does not override the <code>equals(Object)</code> and <code>hashCode()</code> methods.
     *        The <code>IdentityHashMap</code> Javadoc claims that it is faster than the regular
     *        <code>HashMap</code>. But maybe the most interesting property is that it allocates
     *        less objects since <code>IdentityHashMap</code> implementation doesn't need the chain
     *        of objects created by <code>HashMap</code>.}
     */
    private static final Map<Class<?>, Method[]> SHARED_GETTERS = new IdentityHashMap<>();

    /**
     * Additional getter to declare in every list of getter methods that do not already provide
     * their own {@code getIdentifiers()} method. We handle this method specially because it is
     * needed for XML marshalling in ISO 19139 compliant document, while not part of abstract
     * ISO 19115 specification.
     *
     * @see IdentifiedObject#getIdentifiers()
     */
    private static final Method EXTRA_GETTER;
    static {
        try {
            EXTRA_GETTER = IdentifiedObject.class.getMethod("getIdentifiers", (Class<?>[]) null);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e); // Should never happen.
        }
    }

    /**
     * The standard which define the {@link #type} interface.
     */
    private final Citation standard;

    /**
     * The implemented metadata interface.
     */
    final Class<?> type;

    /**
     * The implementation class. The following condition must hold:
     *
     * {@preformat java
     *     type.isAssignableFrom(implementation);
     * }
     */
    final Class<?> implementation;

    /**
     * Number of {@link #getters} methods to use. This is either {@code getters.length}
     * or {@code getters.length-1}, depending on whether the {@link #EXTRA_GETTER} method
     * needs to be skipped or not.
     */
    private final int standardCount, allCount;

    /**
     * The getter methods. This array should not contain any null element.
     * They are the methods defined in the interface, not the implementation class.
     *
     * <p>This array shall not contains any {@code null} elements.</p>
     */
    private final Method[] getters;

    /**
     * The corresponding setter methods, or {@code null} if none. This array must have
     * the same length than {@link #getters}. For every {@code getters[i]} element,
     * {@code setters[i]} is the corresponding setter or {@code null} if there is none.
     */
    private final Method[] setters;

    /**
     * The JavaBeans property names. They are computed at construction time,
     * {@linkplain String#intern() interned} then cached. Those names are often
     * the same than field names (at least in SIS implementation), so it is
     * reasonable to intern them in order to share {@code String} instances.
     *
     * <p>This array shall not contains any {@code null} elements.</p>
     *
     * @see #name(int, KeyNamePolicy)
     */
    private final String[] names;

    /**
     * The types of elements for the corresponding getter and setter methods. If a getter
     * method returns a collection, then this is the type of elements in that collection.
     * Otherwise this is the type of the returned value itself.
     *
     * <p>Notes:</p>
     * <ul>
     *   <li>Primitive types like {@code double} or {@code int} are converted to their wrapper types.</li>
     *   <li>This array may contain null values if the type of elements in a collection is unknown
     *       (i.e. the collection is not parameterized).</li>
     * </ul>
     */
    private final Class<?>[] elementTypes;

    /**
     * Index of getter or setter for a given name. Original names are duplicated with the same name
     * converted to lower cases according {@link Locale#ROOT} conventions, for case-insensitive searches.
     * This map shall be considered as immutable after construction.
     *
     * <p>The keys in this map are both inferred from the method names and fetched from the UML
     * annotations. Consequently the map may contain many entries for the same value if some
     * method names are different than the UML identifiers.</p>
     *
     * @see #indexOf(String)
     */
    private final Map<String,Integer> mapping;

    /**
     * The last converter used. This is remembered on the assumption that the same converter
     * will often be reused for the same property. This optimization can reduce the cost of
     * looking for a converter, and also reduce thread contention since it reduces the number
     * of calls to the synchronized {@link ObjectConverters#find(Class, Class)} method.
     *
     * @see #set(int, Object, Object, boolean)
     */
    private transient volatile ObjectConverter<?,?> converter;

    /**
     * The property descriptions, including the name and restrictions on valid values.
     * The array will be created when first needed. A {@code null} element means that
     * the descriptor at that index has not yet been computed.
     *
     * @see #descriptor(int)
     */
    private transient ParameterDescriptor<?>[] descriptors;

    /**
     * Creates a new property accessor for the specified metadata implementation.
     *
     * @param  standard The standard which define the {@code type} interface.
     * @param  type The interface implemented by the metadata, which must be
     *         the value returned by {@link #getStandardType(Class, String)}.
     * @param  implementation The class of metadata implementations.
     */
    PropertyAccessor(final Citation standard, final Class<?> type, final Class<?> implementation) {
        assert type.isAssignableFrom(implementation) : implementation;
        this.standard       = standard;
        this.type           = type;
        this.implementation = implementation;
        this.getters        = getGetters(type);
        int allCount = getters.length;
        int standardCount = allCount;
        if (allCount != 0 && getters[allCount-1] == EXTRA_GETTER) {
            if (!EXTRA_GETTER.getDeclaringClass().isAssignableFrom(implementation)) {
                allCount--; // The extra getter method does not exist.
            }
            standardCount--;
        }
        this.allCount      = allCount;
        this.standardCount = standardCount;
        /*
         * Compute all information derived from getters: setters, property names, value types.
         */
        mapping      = new HashMap<>(hashMapCapacity(allCount));
        names        = new String[allCount];
        elementTypes = new Class<?>[allCount];
        Method[] setters = null;
        final Class<?>[] arguments = new Class<?>[1];
        for (int i=0; i<allCount; i++) {
            /*
             * Fetch the getter and remind its name. We do the same for
             * the UML tag attached to the getter, if any.
             */
            final Integer index = i;
            Method getter  = getters[i];
            String name    = getter.getName();
            final int base = prefix(name).length();
            addMapping(names[i] = toPropertyName(name, base), index);
            addMapping(name, index);
            final UML annotation = getter.getAnnotation(UML.class);
            if (annotation != null) {
                addMapping(annotation.identifier(), index);
            }
            /*
             * Now try to infer the setter from the getter. We replace the "get" prefix by
             * "set" and look for a parameter of the same type than the getter return type.
             */
            Class<?> returnType = getter.getReturnType();
            arguments[0] = returnType;
            if (name.length() > base) {
                final char lo = name.charAt(base);
                final char up = Character.toUpperCase(lo);
                final int length = name.length();
                final StringBuilder buffer = new StringBuilder(length - base + 3).append(SET);
                if (lo != up) {
                    buffer.append(up).append(name, base+1, length);
                } else {
                    buffer.append(name, base, length);
                }
                name = buffer.toString();
            }
            /*
             * Note: we want PUBLIC methods only.  For example the referencing module defines
             * setters as private methods for use by JAXB only. We don't want to allow access
             * to those setters.
             */
            Method setter = null;
            try {
                setter = implementation.getMethod(name, arguments);
            } catch (NoSuchMethodException e) {
                /*
                 * If we found no setter method expecting an argument of the same type than the
                 * argument returned by the GeoAPI method,  try again with the type returned by
                 * the implementation class. It is typically the same type, but sometime it may
                 * be a subtype.
                 *
                 * It is a necessary condition that the type returned by the getter is assignable
                 * to the type expected by the setter.  This contract is required by the 'freeze'
                 * method among others.
                 */
                try {
                    getter = implementation.getMethod(getter.getName(), (Class<?>[]) null);
                } catch (NoSuchMethodException error) {
                    // Should never happen, since the implementation class
                    // implements the interface where the getter come from.
                    throw new AssertionError(error);
                }
                if (returnType != (returnType = getter.getReturnType())) {
                    arguments[0] = returnType;
                    try {
                        setter = implementation.getMethod(name, arguments);
                    } catch (NoSuchMethodException ignore) {
                        // There is no setter, which may be normal. At this stage
                        // the 'setter' variable should still have the null value.
                    }
                }
            }
            if (setter != null) {
                if (setters == null) {
                    setters = new Method[allCount];
                }
                setters[i] = setter;
            }
            /*
             * Get the type of elements returned by the getter. We perform this step last because
             * the search for a setter above may have replaced the getter declared in the interface
             * by the getter declared in the implementation with a covariant return type. Our intend
             * is to get a type which can be accepted by the setter.
             */
            Class<?> elementType = getter.getReturnType();
            if (Collection.class.isAssignableFrom(elementType)) {
                elementType = Classes.boundOfParameterizedAttribute(getter);
            }
            elementTypes[i] = Numbers.primitiveToWrapper(elementType);
        }
        this.setters = setters;
    }

    /**
     * Adds the given (name, index) pair to {@link #mapping}, making sure we don't
     * overwrite an existing entry with different value.
     */
    private void addMapping(String name, final Integer index) throws IllegalArgumentException {
        if (!name.isEmpty()) {
            String original;
            do {
                final Integer old = mapping.put(name, index);
                if (old != null && !old.equals(index)) {
                    throw new IllegalStateException(Errors.format(Errors.Keys.DuplicatedValue_1,
                            Classes.getShortName(type) + '.' + name));
                }
                original = name;
                name = CharSequences.trimWhitespaces(name.toLowerCase(Locale.ROOT));
            } while (!name.equals(original));
        }
    }

    /**
     * Returns the getters. The returned array should never be modified,
     * since it may be shared among many instances of {@code PropertyAccessor}.
     *
     * @param  type The metadata interface.
     * @return The getters declared in the given interface (never {@code null}).
     */
    private static Method[] getGetters(final Class<?> type) {
        synchronized (SHARED_GETTERS) {
            Method[] getters = SHARED_GETTERS.get(type);
            if (getters == null) {
                getters = type.getMethods();
                // Following is similar in purpose to the PropertyAccessor.mapping field,
                // but index values are different because of the call to Arrays.sort(...).
                final Map<String,Integer> mapping = new HashMap<>(hashMapCapacity(getters.length));
                boolean hasExtraGetter = false;
                int count = 0;
                for (final Method candidate : getters) {
                    if (Classes.isPossibleGetter(candidate)) {
                        final String name = candidate.getName();
                        if (name.startsWith(SET)) { // Paranoiac check.
                            continue;
                        }
                        /*
                         * At this point, we are ready to accept the method. Before doing so,
                         * check if the method override an other method defined in a parent
                         * class with a covariant return type. The JVM considers such cases
                         * as two different methods, while from a Java developer point of
                         * view this is the same method (GEOTK-205).
                         */
                        final Integer pi = mapping.put(name, count);
                        if (pi != null) {
                            final Class<?> pt = getters[pi].getReturnType();
                            final Class<?> ct = candidate  .getReturnType();
                            if (ct.isAssignableFrom(pt)) {
                                continue; // Previous type was more accurate.
                            }
                            if (pt.isAssignableFrom(ct)) {
                                getters[pi] = candidate;
                                continue;
                            }
                            throw new ClassCastException(Errors.format(Errors.Keys.IllegalArgumentClass_3,
                                    Classes.getShortName(type) + '.' + name, ct, pt));
                        }
                        getters[count++] = candidate;
                        if (!hasExtraGetter) {
                            hasExtraGetter = name.equals(EXTRA_GETTER.getName());
                        }
                    }
                }
                /*
                 * Sort the standard methods before to add the extra methods (if any) in order to
                 * keep the extra methods last. The code checking for the extra methods require
                 * them to be last.
                 */
                Arrays.sort(getters, 0, count, PropertyComparator.INSTANCE);
                if (!hasExtraGetter) {
                    if (getters.length == count) {
                        getters = Arrays.copyOf(getters, count+1);
                    }
                    getters[count++] = EXTRA_GETTER;
                }
                getters = ArraysExt.resize(getters, count);
                SHARED_GETTERS.put(type, getters);
            }
            return getters;
        }
    }

    /**
     * Returns the prefix of the specified method name. If the method name doesn't starts with
     * a prefix (for example {@link org.opengis.metadata.quality.ConformanceResult#pass()}),
     * then this method returns an empty string.
     */
    private static String prefix(final String name) {
        if (name.startsWith(GET)) {
            return GET;
        }
        if (name.startsWith(IS)) {
            return IS;
        }
        if (name.startsWith(SET)) {
            return SET;
        }
        return "";
    }

    /**
     * Returns {@code true} if the specified string starting at the specified index contains
     * no lower case characters. The characters don't have to be in upper case however (e.g.
     * non-alphabetic characters)
     */
    private static boolean isAcronym(final String name, int offset) {
        final int length = name.length();
        while (offset < length) {
            final int c = name.codePointAt(offset);
            if (Character.isLowerCase(c)) {
                return false;
            }
            offset += Character.charCount(c);
        }
        return true;
    }

    /**
     * Removes the {@code "get"} or {@code "is"} prefix and turn the first character after the
     * prefix into lower case. For example the method name {@code "getTitle"} will be replaced
     * by the property name {@code "title"}. We will perform this operation only if there is
     * at least 1 character after the prefix.
     *
     * @param  name The method name (can not be {@code null}).
     * @param  base Must be the result of {@code prefix(name).length()}.
     * @return The property name (never {@code null}).
     */
    private static String toPropertyName(String name, final int base) {
        final int length = name.length();
        if (length > base) {
            if (isAcronym(name, base)) {
                name = name.substring(base);
            } else {
                final int up = name.codePointAt(base);
                final int lo = Character.toLowerCase(up);
                if (up != lo) {
                    name = new StringBuilder(length - base).appendCodePoint(lo)
                            .append(name, base + Character.charCount(up), length).toString();
                } else {
                    name = name.substring(base);
                }
            }
        }
        return CharSequences.trimWhitespaces(name).intern();
    }

    /**
     * Returns the number of properties that can be read.
     */
    final int count() {
        return standardCount;
    }

    /**
     * Returns the index of the specified property, or -1 if none.
     * The search is case-insensitive.
     *
     * @param  name The name of the property to search.
     * @param  mandatory Whether this method shall throw an exception or return {@code -1}
     *         if the given name is not found.
     * @return The index of the given name, or -1 if none and {@code mandatory} is {@code false}.
     * @throws IllegalArgumentException if the name is not found and {@code mandatory} is {@code true}.
     */
    final int indexOf(final String name, final boolean mandatory) {
        Integer index = mapping.get(name);
        if (index == null) {
            /*
             * Make a second try with lower cases only if the first try failed, because
             * most of the time the key name will have exactly the expected case and using
             * directly the given String instance allow usage of its cached hash code value.
             */
            final String key = CharSequences.trimWhitespaces(name.replace(" ", "").toLowerCase(Locale.ROOT));
            if (key == name || (index = mapping.get(key)) == null) { // Identity comparison is okay here.
                if (!mandatory) {
                    return -1;
                }
                throw new IllegalArgumentException(Errors.format(Errors.Keys.NoSuchProperty_2, name, type));
            }
        }
        return index;
    }

    /**
     * Returns the declaring class of the getter at the given index.
     *
     * @param  index The index of the property for which to get the declaring class.
     * @return The declaring class at the given index, or {@code null} if the index is out of bounds.
     */
    final Class<?> getDeclaringClass(final int index) {
        if (index >= 0 && index < names.length) {
            return getters[index].getDeclaringClass();
        }
        return null;
    }

    /**
     * Returns the name of the property at the given index, or {@code null} if none.
     *
     * @param  index The index of the property for which to get the name.
     * @param  keyName The kind of name to return.
     * @return The name of the given kind at the given index,
     *         or {@code null} if the index is out of bounds.
     */
    @SuppressWarnings("fallthrough")
    final String name(final int index, final KeyNamePolicy keyName) {
        if (index >= 0 && index < names.length) {
            switch (keyName) {
                case UML_IDENTIFIER: {
                    final UML uml = getters[index].getAnnotation(UML.class);
                    if (uml != null) {
                        return uml.identifier();
                    }
                    // Fallthrough
                }
                case JAVABEANS_PROPERTY: {
                    return names[index];
                }
                case METHOD_NAME: {
                    return getters[index].getName();
                }
                case SENTENCE: {
                    return CharSequences.camelCaseToSentence(names[index]).toString();
                }
            }
        }
        return null;
    }

    /**
     * Returns the type of the property at the given index. The returned type is usually
     * a GeoAPI interface (at least in the case of SIS implementation).
     *
     * <p>If the given policy is {@code ELEMENT_TYPE}, then:</p>
     * <ul>
     *   <li>Primitive types like {@code double} or {@code int} are converted to their wrapper types.</li>
     *   <li>If the property is a collection, then returns the type of collection elements.</li>
     * </ul>
     *
     * @param  index The index of the property.
     * @param  policy The kind of type to return.
     * @return The type of property values, or {@code null} if unknown.
     */
    final Class<?> type(final int index, final TypeValuePolicy policy) {
        if (index >= 0 && index < standardCount) {
            switch (policy) {
                case ELEMENT_TYPE: {
                    return elementTypes[index];
                }
                case PROPERTY_TYPE: {
                    return getters[index].getReturnType();
                }
                case DECLARING_INTERFACE: {
                    return getters[index].getDeclaringClass();
                }
                case DECLARING_CLASS: {
                    Method getter = getters[index];
                    if (implementation != type) try {
                        getter = implementation.getMethod(getter.getName(), (Class<?>[]) null);
                    } catch (NoSuchMethodException error) {
                        // Should never happen, since the implementation class
                        // implements the interface where the getter come from.
                        throw new AssertionError(error);
                    }
                    return getter.getDeclaringClass();
                }
            }
        }
        return null;
    }

    /**
     * Returns the descriptor for the property at the given index.
     * The descriptor are created when first needed.
     *
     * @param  index The index of the property for which to get the descriptor.
     * @return The descriptor for the property at the given index,
     *         or {@code null} if the index is out of bounds.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    final synchronized ParameterDescriptor<?> descriptor(final int index) {
        ParameterDescriptor[] descriptors = this.descriptors;
        if (descriptors == null) {
            this.descriptors = descriptors = new PropertyDescriptor<?>[standardCount];
        }
        if (index < 0 || index >= descriptors.length) {
            return null;
        }
        ParameterDescriptor<?> descriptor = descriptors[index];
        if (descriptor == null) {
            final Class<?> elementType = elementTypes[index];
            final Citation standard    = this.standard;
            final String   name        = name(index, KeyNamePolicy.UML_IDENTIFIER);
            final Method   getter      = getters[index];
            ValueRange range = null;
            if (implementation != type) {
                final int e = Numbers.getEnumConstant(elementType);
                if (e >= Numbers.BYTE && e <= Numbers.DOUBLE) try {
                    range = implementation.getMethod(getter.getName(), (Class<?>[]) null)
                            .getAnnotation(ValueRange.class);
                } catch (NoSuchMethodException error) {
                    // Should never happen, since the implementation class
                    // implements the interface where the getter come from.
                    throw new AssertionError(error);
                }
            }
            if (range != null) {
                descriptor = new PropertyDescriptor.Bounded(elementType, standard, name, getter, range);
            } else {
                descriptor = new PropertyDescriptor<>(elementType, standard, name, getter);
            }
            descriptors[index] = descriptor;
        }
        return descriptor;
    }

    /**
     * Returns {@code true} if the property at the given index is writable.
     */
    final boolean isWritable(final int index) {
        return (index >= 0) && (index < standardCount) && (setters != null) && (setters[index] != null);
    }

    /**
     * Returns the value for the specified metadata, or {@code null} if none.
     *
     * @throws BackingStoreException If the implementation threw a checked exception.
     */
    final Object get(final int index, final Object metadata) throws BackingStoreException {
        return (index >= 0 && index < standardCount) ? get(getters[index], metadata) : null;
    }

    /**
     * Gets a value from the specified metadata. We do not expect any checked exception to be
     * thrown, since classes in the {@code org.opengis.metadata} packages do not declare any.
     * However if a checked exception is throw anyway (maybe in user defined "standard"), it
     * will be wrapped in a {@link BackingStoreException}. Unchecked exceptions are propagated.
     *
     * @param  method The method to use for the query.
     * @param  metadata The metadata object to query.
     * @throws BackingStoreException If the implementation threw a checked exception.
     *
     * @see #set(Method, Object, Object[])
     */
    private static Object get(final Method method, final Object metadata) throws BackingStoreException {
        assert (method.getReturnType() != Void.TYPE) : method;
        try {
            return method.invoke(metadata, (Object[]) null);
        } catch (IllegalAccessException e) {
            // Should never happen since 'getters' should contains only public methods.
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getTargetException();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new BackingStoreException(cause);
        }
    }

    /**
     * Sets a value for the specified metadata and returns the old value if {@code getOld} is
     * {@code true}. If the old value was a collection or a map, then this value is copied in
     * a new collection or map before the new value is set, because the setter methods typically
     * copy the new collection in their existing instance.
     *
     * @param  index    The index of the property to set.
     * @param  metadata The metadata object on which to set the value.
     * @param  value    The new value.
     * @param  getOld   {@code true} if this method should first fetches the old value.
     * @return The old value, or {@code null} if {@code getOld} was {@code false}.
     * @throws UnmodifiableMetadataException if the attribute for the given key is read-only.
     * @throws ClassCastException if the given value is not of the expected type.
     * @throws BackingStoreException if the implementation threw a checked exception.
     */
    final Object set(final int index, final Object metadata, final Object value, final boolean getOld)
            throws UnmodifiableMetadataException, ClassCastException, BackingStoreException
    {
        if (index >= 0 && index < standardCount && setters != null) {
            final Method getter = getters[index];
            final Method setter = setters[index];
            if (setter != null) {
                Object old;
                if (getOld) {
                    old = get(getter, metadata);
                    if (old instanceof Collection<?>) {
                        old = modifiableCopy((Collection<?>) old);
                    } else if (old instanceof Map<?,?>) {
                        old = modifiableCopy((Map<?,?>) old);
                    }
                } else {
                    old = null;
                }
                final Object[] newValues = new Object[] {value};
                converter = convert(getter, metadata, newValues, elementTypes[index], converter);
                set(setter, metadata, newValues);
                return old;
            }
        }
        throw new UnmodifiableMetadataException(Errors.format(Errors.Keys.CanNotSetPropertyValue_1, names[index]));
    }

    /**
     * Sets a value for the specified metadata. This method does not attempt any conversion of
     * argument values. Conversion of type, if needed, must have been applied before to call
     * this method.
     *
     * <p>We do not expect any checked exception to be thrown, since classes in the
     * {@code org.opengis.metadata} packages do not declare any. However if a checked
     * exception is throw anyway, then it will be wrapped in a {@link BackingStoreException}.
     * Unchecked exceptions are propagated.</p>
     *
     * @param  setter    The method to use for setting the new value.
     * @param  metadata  The metadata object to query.
     * @param  newValues The argument to give to the method to be invoked.
     * @throws BackingStoreException If the implementation threw a checked exception.
     *
     * @see #get(Method, Object)
     */
    private static void set(final Method setter, final Object metadata, final Object[] newValues)
            throws BackingStoreException
    {
        try {
            setter.invoke(metadata, newValues);
        } catch (IllegalAccessException e) {
            // Should never happen since 'setters' should contains only public methods.
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getTargetException();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new BackingStoreException(cause);
        }
    }

    /**
     * Converts a value to the type required by a setter method.
     *
     * @param getter      The method to use for fetching the previous value.
     * @param metadata    The metadata object to query.
     * @param newValues   The argument to convert. It must be an array of length 1.
     *                    The content of this array will be modified in-place.
     * @param elementType The type required by the setter method.
     * @param converter   The last converter used, or {@code null} if none.
     *                    This converter is provided only as a hint and doesn't need to be accurate.
     * @return The last converter used, or {@code null}.
     * @throws ClassCastException if the element of the {@code arguments} array is not of the expected type.
     * @throws BackingStoreException If the implementation threw a checked exception.
     */
    private static ObjectConverter<?,?> convert(final Method getter, final Object metadata,
            final Object[] newValues, Class<?> elementType, ObjectConverter<?,?> converter)
            throws ClassCastException, BackingStoreException
    {
        assert newValues.length == 1;
        Object newValue = newValues[0];
        if (newValue == null) {
            // Can't test elementType, because it has been converted to the wrapper class.
            final Class<?> type = getter.getReturnType();
            if (type.isPrimitive()) {
                newValues[0] = Numbers.valueOfNil(type);
            }
        } else {
            Class<?> targetType = getter.getReturnType();
            if (!Collection.class.isAssignableFrom(targetType)) {
                /*
                 * We do not expect a collection. The provided argument should not be a
                 * collection neither. It should be some class convertible to targetType.
                 *
                 * If nevertheless the user provided a collection and this collection contains
                 * no more than 1 element, then as a convenience we will extract the singleton
                 * element and process it as if it had been directly provided in argument.
                 */
                if (newValue instanceof Collection<?>) {
                    final Iterator<?> it = ((Collection<?>) newValue).iterator();
                    if (!it.hasNext()) { // If empty, process like null argument.
                        newValues[0] = null;
                        return converter;
                    }
                    final Object next = it.next();
                    if (!it.hasNext()) { // Singleton
                        newValue = next;
                    }
                    // Other cases: let the collection unchanged. It is likely to
                    // cause an exception later. The message should be appropriate.
                }
                // Getter type (targetType) shall be the same than the setter type (elementType).
                assert elementType == Numbers.primitiveToWrapper(targetType) : elementType;
                targetType = elementType; // Ensure that we use primitive wrapper.
            } else {
                /*
                 * We expect a collection. Collections are handled in one of the two ways below:
                 *
                 *   - If the user gives a collection, the user's collection replaces any
                 *     previous one. The content of the previous collection is discarded.
                 *
                 *   - If the user gives a single value, it will be added to the existing
                 *     collection (if any). The previous values are not discarded. This
                 *     allow for incremental filling of a property.
                 *
                 * The code below prepares an array of elements to be converted and wraps that
                 * array in a List (to be converted to a Set after this block if required). It
                 * is okay to convert the elements after the List creation since the list is a
                 * wrapper.
                 */
                final Collection<?> addTo;
                final Object[] elements;
                if (newValue instanceof Collection<?>) {
                    elements = ((Collection<?>) newValue).toArray();
                    newValue = Arrays.asList(elements); // Content will be converted later.
                    addTo = null;
                } else {
                    elements = new Object[] {newValue};
                    newValue = addTo = (Collection<?>) get(getter, metadata);
                    if (addTo == null) {
                        // No previous collection. Create one.
                        newValue = Arrays.asList(elements);
                    } else if (addTo instanceof CheckedContainer<?>) {
                        // Get the explicitly-specified element type.
                        elementType = ((CheckedContainer<?>) addTo).getElementType();
                    }
                }
                if (elementType != null) {
                    converter = convert(elements, elementType, converter);
                }
                /*
                 * We now have objects of the appropriate type. If we have a singleton to be added
                 * in an existing collection, add it now. In that case the 'newValue' should refer
                 * to the 'addTo' collection. We rely on ModifiableMetadata.copyCollection(...)
                 * optimization for detecting that the new collection is the same instance than
                 * the old one so there is nothing to do. We could exit from the method, but let
                 * it continues in case the user override the 'setFoo(...)' method.
                 */
                if (addTo != null) {
                    /*
                     * Unsafe addition into a collection. In SIS implementation, the collection is
                     * actually an instance of CheckedCollection, so the check will be performed at
                     * runtime. However other implementations could use unchecked collection.
                     * There is not much we can do...
                     */
                    // No @SuppressWarnings because this is a real hole.
                    ((Collection<Object>) addTo).add(elements[0]);
                }
            }
            /*
             * If the expected type was not a collection, the conversion of user value happen
             * here. Otherwise conversion from List to Set (if needed) happen here.
             */
            newValues[0] = newValue;
            converter = convert(newValues, targetType, converter);
        }
        return converter;
    }

    /**
     * Converts values in the specified array to the given type.
     * The given converter will be used if suitable, or a new one fetched otherwise.
     *
     * @param  elements   The array which contains element to convert.
     * @param  targetType The base type of target elements.
     * @param  converter  The proposed converter, or {@code null}.
     * @return The last converter used, or {@code null}.
     * @throws ClassCastException If an element can't be converted.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private static ObjectConverter<?,?> convert(final Object[] elements, final Class<?> targetType,
            ObjectConverter<?,?> converter) throws ClassCastException
    {
        for (int i=0; i<elements.length; i++) {
            final Object value = elements[i];
            if (value != null) {
                final Class<?> sourceType = value.getClass();
                if (!targetType.isAssignableFrom(sourceType)) try {
                    if (converter == null
                            || !converter.getSourceClass().isAssignableFrom(sourceType)
                            || !targetType.isAssignableFrom(converter.getTargetClass()))
                    {
                        converter = ObjectConverters.find(sourceType, targetType);
                    }
                    elements[i] = ((ObjectConverter) converter).convert(value);
                } catch (UnconvertibleObjectException cause) {
                    final ClassCastException e = new ClassCastException(Errors.format(
                            Errors.Keys.IllegalClass_2, targetType, sourceType));
                    e.initCause(cause);
                    throw e;
                }
            }
        }
        return converter;
    }

    /**
     * Compares the two specified metadata objects. This method implements a <cite>shallow</cite>
     * comparison, i.e. all metadata properties are compared using their {@code properties.equals(…)}
     * method without explicit calls to this {@code accessor.equals(…)} method for children.
     * However the final result may still be a deep comparison.
     *
     * @param  metadata1 The first metadata object to compare. This object determines the accessor.
     * @param  metadata2 The second metadata object to compare.
     * @param  mode      The strictness level of the comparison.
     * @throws BackingStoreException If the implementation threw a checked exception.
     *
     * @see MetadataStandard#equals(Object, Object, ComparisonMode)
     */
    public boolean equals(final Object metadata1, final Object metadata2, final ComparisonMode mode)
            throws BackingStoreException
    {
        assert type.isInstance(metadata1) : metadata1;
        assert type.isInstance(metadata2) : metadata2;
        final int count = (mode == ComparisonMode.STRICT &&
                EXTRA_GETTER.getDeclaringClass().isInstance(metadata2)) ? allCount : standardCount;
        for (int i=0; i<count; i++) {
            final Method method = getters[i];
            final Object value1 = get(method, metadata1);
            final Object value2 = get(method, metadata2);
            if (isNullOrEmpty(value1) && isNullOrEmpty(value2)) {
                // Consider empty collections/arrays as equal to null.
                // Empty strings are also considered equal to null (this is more questionable).
                continue;
            }
            if (!Utilities.deepEquals(value1, value2, mode)) {
                if (mode.ordinal() >= ComparisonMode.APPROXIMATIVE.ordinal() && floatEpsilonEqual(value1, value2)) {
                    continue; // Accept this slight difference.
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Copies all non-empty metadata from source to target. The source can be any implementation
     * of the metadata interface, but the target must be the implementation expected by this class.
     *
     * <p>If the source contains any null or empty properties, then those properties will
     * <strong>not</strong> overwrite the corresponding properties in the destination metadata.</p>
     *
     * @param  source The metadata to copy.
     * @param  target The target metadata.
     * @return {@code true} in case of success, or {@code false} if at least
     *         one setter method was not found.
     * @throws UnmodifiableMetadataException if the target metadata is unmodifiable.
     * @throws BackingStoreException If the implementation threw a checked exception.
     */
    public boolean shallowCopy(final Object source, final Object target)
            throws UnmodifiableMetadataException, BackingStoreException
    {
        // Because this PropertyAccesssor is designed for the target, we must
        // check if the extra methods are suitable for the source object.
        ObjectConverter<?,?> converter = this.converter;
        boolean success = true;
        assert type.isInstance(source) : Classes.getClass(source);
        final Object[] arguments = new Object[1];
        for (int i=0; i<standardCount; i++) {
            final Method getter = getters[i];
            arguments[0] = get(getter, source);
            if (!isNullOrEmpty(arguments[0])) {
                if (setters == null) {
                    return false;
                }
                final Method setter = setters[i];
                if (setter != null) {
                    converter = convert(getter, target, arguments, elementTypes[i], converter);
                    set(setter, target, arguments);
                } else {
                    success = false;
                }
            }
        }
        this.converter = converter;
        return success;
    }

    /**
     * Replaces every properties in the specified metadata by their
     * {@linkplain ModifiableMetadata#unmodifiable unmodifiable variant}.
     *
     * @throws BackingStoreException If the implementation threw a checked exception.
     */
    final void freeze(final Object metadata) throws BackingStoreException {
        assert implementation.isInstance(metadata) : metadata;
        if (setters != null) try {
            final Object[] arguments = new Object[1];
            final Cloner cloner = new Cloner();
            for (int i=0; i<allCount; i++) {
                final Method setter = setters[i];
                if (setter != null) {
                    final Method getter = getters[i];
                    final Object source = get(getter, metadata);
                    final Object target = cloner.clone(source);
                    if (source != target) {
                        arguments[0] = target;
                        set(setter, metadata, arguments);
                        /*
                         * We invoke the set(...) method which do not perform type conversion
                         * because we don't want it to replace the immutable collection created
                         * by ModifiableMetadata.unmodifiable(source). Conversion should not be
                         * required anyway because the getter method should have returned a value
                         * compatible with the setter method - this contract is ensured by the
                         * way the PropertyAccessor constructor selected the setter methods.
                         */
                    }
                }
            }
        } catch (CloneNotSupportedException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    /**
     * Computes a hash code for the specified metadata. The hash code is defined as the
     * sum of hash code values of all non-empty properties. This is a similar contract
     * than {@link java.util.Set#hashCode()} and ensures that the hash code value is
     * insensitive to the ordering of properties.
     *
     * @throws BackingStoreException If the implementation threw a checked exception.
     */
    public int hashCode(final Object metadata) throws BackingStoreException {
        assert type.isInstance(metadata) : metadata;
        int code = 0;
        for (int i=0; i<standardCount; i++) {
            final Object value = get(getters[i], metadata);
            if (!isNullOrEmpty(value)) {
                code += value.hashCode();
            }
        }
        return code;
    }

    /**
     * Counts the number of non-empty properties.
     *
     * @throws BackingStoreException If the implementation threw a checked exception.
     */
    public int count(final Object metadata, final int max) throws BackingStoreException {
        assert type.isInstance(metadata) : metadata;
        int count = 0;
        for (int i=0; i<standardCount; i++) {
            if (!isNullOrEmpty(get(getters[i], metadata))) {
                if (++count >= max) {
                    break;
                }
            }
        }
        return count;
    }

    /**
     * Returns {@code true} if the specified object is null or an empty collection, array or string.
     *
     * <p>This method intentionally does not inspect array or collection elements, since this method
     * is invoked from methods doing shallow copy or comparison. If we were inspecting elements,
     * we would need to add a check against infinite recursivity.</p>
     */
    static boolean isNullOrEmpty(final Object value) {
        return value == null
                || ((value instanceof CharSequence)  && ((CharSequence) value).length() == 0)
                || ((value instanceof Collection<?>) && ((Collection<?>) value).isEmpty())
                || ((value instanceof Map<?,?>)      && ((Map<?,?>) value).isEmpty())
                || (value.getClass().isArray()       && Array.getLength(value) == 0);
    }
}
