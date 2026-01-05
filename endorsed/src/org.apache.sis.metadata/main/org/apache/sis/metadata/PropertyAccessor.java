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

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.AbstractMap;
import java.util.Locale;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import org.opengis.annotation.UML;
import org.opengis.annotation.Obligation;
import org.opengis.metadata.ExtendedElementInformation;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.math.NumberType;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.internal.shared.Unsafe;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.internal.shared.ViewAsSet;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.xml.IdentifiedObject;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.pending.jdk.JDK19;
import static org.apache.sis.metadata.PropertyComparator.*;
import static org.apache.sis.metadata.ValueExistencePolicy.isNullOrEmpty;


/**
 * The getter methods declared in a GeoAPI interface, together with setter methods (if any)
 * declared in the SIS implementation. An instance of {@code PropertyAccessor} gives access
 * to all public properties of an instance of a metadata object. It uses reflection for this
 * purpose, a little bit like the <cite>Java Beans</cite> framework.
 *
 * <p>This accessor groups the properties in two categories:</p>
 *
 * <ul>
 *   <li>The standard properties defined by the GeoAPI (or other standard) interfaces.
 *       Those properties are the only ones accessible by most methods in this class, except
 *       {@link #equals(Object, Object, ComparisonMode)} and {@link #walkWritable(MetadataVisitor, Object, Object)}.</li>
 *
 *   <li>Extra properties defined by the {@link IdentifiedObject} interface. Those properties
 *       invisible in the ISO 19115-1 model, but appears in ISO 19115-3 XML marshalling. So we
 *       do the same in the SIS implementation: invisible in map and tree view, but visible in
 *       XML marshalling.</li>
 * </ul>
 *
 * <h2>Thread safety</h2>
 * The same {@code PropertyAccessor} instance can be safely used by many threads without synchronization
 * on the part of the caller. Subclasses shall make sure that any overridden methods remain safe to call
 * from multiple threads, because the same {@code PropertyAccessor} instances are typically used by many
 * {@link ModifiableMetadata} instances.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
class PropertyAccessor {
    /**
     * Enumeration constants for the {@code mode} argument in the
     * {@link #count(Object, ValueExistencePolicy, int)} method.
     */
    static final int COUNT_FIRST=0, COUNT_SHALLOW=1, COUNT_DEEP=2;

    /**
     * Enumeration constants for the {@code mode} argument
     * in the {@link #set(int, Object, Object, int)} method.
     */
    static final int RETURN_NULL=0, RETURN_PREVIOUS=1, APPEND=2, IGNORE_READ_ONLY=3;

    /**
     * Additional getter to declare in every list of getter methods that do not already provide
     * their own {@code getIdentifiers()} method. We handle this method specially because it is
     * needed for XML marshalling in ISO 19115-3 compliant document, while not part of abstract
     * ISO 19115-1 specification.
     *
     * @see IdentifiedObject#getIdentifiers()
     */
    private static final Method EXTRA_GETTER;
    static {
        try {
            EXTRA_GETTER = IdentifiedObject.class.getMethod("getIdentifiers", (Class<?>[]) null);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);                                // Should never happen.
        }
    }

    /**
     * The implemented metadata interface.
     */
    final Class<?> type;

    /**
     * The implementation class, or {@link #type} if none.
     * The following condition must hold:
     *
     * {@snippet lang="java" :
     *     assert type.isAssignableFrom(implementation);
     *     }
     *
     * <h4>Design note</h4>
     * We could enforce the above-cited restriction with type parameter: if the {@link #type} field is declared
     * as {@code Class<T>}, then this {@code implementation} field would be declared as {@code Class<? extends T>}.
     * However, this is not useful for this internal class because the {@code <T>} type is never known; we have the
     * {@code <?>} type everywhere except in tests, which result in compiler warnings at {@code PropertyAccessor}
     * construction.
     */
    final Class<?> implementation;

    /**
     * Number of {@link #getters} methods that can be used, regardless of whether the methods are visible
     * or hidden to the user. This is either {@code getters.length} or {@code getters.length-1}, depending
     * on whether the {@link #EXTRA_GETTER} method needs to be skipped or not.
     */
    private final int allCount;

    /**
     * Numbers of methods to show to the user. This is always equal or lower than {@link #allCount}.
     * This count may be lower than {@code allCount} for two reasons:
     *
     * <ul>
     *   <li>The {@link #EXTRA_GETTER} method is not part of the international standard.</li>
     *   <li>The interface contains deprecated methods from an older international standard.
     *       Example: changes caused by the upgrade from ISO 19115:2003 to ISO 19115:2014.</li>
     * </ul>
     */
    private final int standardCount;

    /**
     * The public getter methods. This array should not contain any null element.
     * They are the methods defined in the interface, not the implementation class.
     *
     * <p>This array shall not contains any {@code null} elements.</p>
     */
    private final Method[] getters;

    /**
     * The corresponding setter methods, or {@code null} if none. This array must have
     * the same length as {@link #getters}. For every {@code getters[i]} element,
     * {@code setters[i]} is the corresponding setter or {@code null} if there is none.
     */
    private final Method[] setters;

    /**
     * The JavaBeans property names. They are computed at construction time, {@linkplain String#intern() interned}
     * then cached. Those names are often the same as field names (at least in SIS implementation), so it is
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
     *   <li>Element type of {@link Map} collection is {@link Map.Entry}.</li>
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
     * annotations. Consequently, the map may contain many entries for the same value if some
     * method names are different than the UML identifiers.</p>
     *
     * @see #indexOf(String, boolean)
     */
    private final Map<String,Integer> mapping;

    /**
     * The last converter used. This is remembered on the assumption that the same converter
     * will often be reused for the same property. This optimization can reduce the cost of
     * looking for a converter, and also reduce thread contention since it reduces the number
     * of calls to the synchronized {@link ObjectConverters#find(Class, Class)} method.
     *
     * @see #convert(Object[], Class)
     */
    private transient volatile ObjectConverter<?,?> lastConverter;

    /**
     * The property information, including the name and restrictions on valid values.
     * The array will be created when first needed. A {@code null} element means that
     * the information at that index has not yet been computed.
     *
     * @see #information(Citation, int)
     */
    private transient ExtendedElementInformation[] informations;

    /**
     * Creates a new property accessor for the specified metadata implementation.
     *
     * @param  type            the interface implemented by the metadata class.
     * @param  implementation  the class of the metadata implementation, or {@code type} if none.
     * @param  standardImpl    the implementation specified by the {@link MetadataStandard}, or {@code null} if none.
     *                         This is the same as {@code implementation} unless a custom implementation is used.
     */
    @SuppressWarnings("LocalVariableHidesMemberVariable")
    PropertyAccessor(final Class<?> type, final Class<?> implementation, final Class<?> standardImpl) {
        assert type.isAssignableFrom(implementation) : implementation;
        this.type           = type;
        this.implementation = implementation;
        this.getters        = getGetters(type, implementation, standardImpl);
        int allCount        = getters.length;
        int standardCount   = allCount;
        if (allCount != 0 && getters[allCount-1] == EXTRA_GETTER) {
            if (!EXTRA_GETTER.getDeclaringClass().isAssignableFrom(implementation)) {
                allCount--;                                 // The extra getter method does not exist.
            }
            standardCount--;
        }
        while (standardCount != 0) {                        // Skip deprecated methods.
            if (!isDeprecated(standardCount - 1)) {
                break;
            }
            standardCount--;
        }
        this.allCount      = allCount;
        this.standardCount = standardCount;
        /*
         * Compute all information derived from getters: setters, property names, value types.
         */
        mapping      = JDK19.newHashMap(allCount);
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
            addMapping(name, index);
            addMappingWithLowerCase(names[i] = toPropertyName(name, base), index);
            final UML annotation = getter.getAnnotation(UML.class);
            if (annotation != null) {
                addMappingWithLowerCase(annotation.identifier().intern(), index);
            }
            /*
             * Now try to infer the setter from the getter. We replace the "get" prefix by
             * "set" and look for a parameter of the same type as the getter return type.
             */
            Class<?> returnType = getter.getReturnType();
            arguments[0] = returnType;
            if (name.length() > base) {
                final int lo = name.codePointAt(base);
                final int up = Character.toUpperCase(lo);
                final int length = name.length();
                final var buffer = new StringBuilder(length - base + 5).append(SET);
                if (lo != up) {
                    buffer.appendCodePoint(up).append(name, base + Character.charCount(lo), length);
                } else {
                    buffer.append(name, base, length);
                }
                name = buffer.toString();
            }
            /*
             * Note: we want PUBLIC methods only.  For example, the referencing module defines
             * setters as private methods for use by JAXB only. We don't want to allow access
             * to those setters.
             */
            Method setter = null;
            try {
                setter = implementation.getMethod(name, arguments);
            } catch (NoSuchMethodException e) {
                /*
                 * If we found no setter method expecting an argument of the same type as the
                 * argument returned by the GeoAPI method,  try again with the type returned by
                 * the implementation class. It is typically the same type, but sometimes it may
                 * be a parent type.
                 *
                 * It is a necessary condition that the type returned by the getter is assignable
                 * to the type expected by the setter.  This contract is required by the `FINAL`
                 * state among others.
                 */
                try {
                    getter = implementation.getMethod(getter.getName(), (Class<?>[]) null);
                } catch (NoSuchMethodException error) {
                    /*
                     * Should never happen, since the implementation class
                     * implements the interface where the getter come from.
                     */
                    throw new AssertionError(error);
                }
                if (returnType != (returnType = getter.getReturnType())) {
                    arguments[0] = returnType;
                    try {
                        setter = implementation.getMethod(name, arguments);
                    } catch (NoSuchMethodException ignore) {
                        /*
                         * There is no setter, which may be normal. At this stage
                         * the `setter` variable should still have the null value.
                         */
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
             * by the getter declared in the implementation with a covariant return type. Our intent
             * is to get a type which can be accepted by the setter.
             */
            Class<?> elementType = getter.getReturnType();
            if (Collection.class.isAssignableFrom(elementType) || Classes.isParameterizedProperty(elementType)) {
                elementType = Classes.boundOfParameterizedProperty(getter);
                if (elementType == null) {
                    // Subclass has erased parameterized type. Use method declared in the interface.
                    elementType = Classes.boundOfParameterizedProperty(getters[i]);
                }
            } else if (Map.class.isAssignableFrom(elementType)) {
                elementType = Map.Entry.class;
            }
            elementTypes[i] = NumberType.primitiveToWrapper(elementType);
        }
        this.setters = setters;
    }

    /**
     * Adds the given (name, index) pair to {@link #mapping}, making sure we don't
     * overwrite an existing entry with different value.
     */
    private void addMapping(final String name, final Integer index) {
        if (!name.isEmpty()) {
            final Integer old = mapping.put(name, index);
            if (old != null && !old.equals(index)) {
                /*
                 * Same identifier for two different properties. If one is deprecated while the
                 * other is not, then the non-deprecated identifier overwrite the deprecated one.
                 */
                final boolean deprecated = isDeprecated(index);
                if (deprecated == isDeprecated(old)) {
                    throw new IllegalStateException(Errors.format(Errors.Keys.DuplicatedIdentifier_1,
                            Classes.getShortName(type) + '.' + name));
                }
                if (deprecated) {
                    mapping.put(name, old);                 // Restore the undeprecated method.
                }
            }
        }
    }

    /**
     * Adds the given (name, index) pair and its lower-case variant.
     */
    private void addMappingWithLowerCase(final String name, final Integer index) {
        addMapping(name, index);
        final String lower = name.toLowerCase(Locale.ROOT);
        if (!lower.equals(name)) {
            addMapping(lower, index);
        }
    }

    /**
     * Returns the getters. The returned array should never be modified,
     * since it may be shared among many instances of {@code PropertyAccessor}.
     *
     * @param  type            the metadata interface.
     * @param  implementation  the class of metadata implementations, or {@code type} if none.
     * @param  standardImpl    the implementation specified by the {@link MetadataStandard}, or {@code null} if none.
     * @return the getters declared in the given interface (never {@code null}).
     */
    private static Method[] getGetters(final Class<?> type, final Class<?> implementation, final Class<?> standardImpl) {
        /*
         * Indices map is used for choosing what to do in case of name collision.
         */
        Method[] getters = (MetadataStandard.IMPLEMENTATION_CAN_ALTER_API ? implementation : type).getMethods();
        final Map<String,Integer> indices = JDK19.newHashMap(getters.length);
        boolean hasExtraGetter = false;
        int count = 0;
        for (Method candidate : getters) {
            if (Classes.isPossibleGetter(candidate)) {
                final String name = candidate.getName();
                if (name.startsWith(SET) || SpecialCases.exclude(type, name)) {
                    continue;
                }
                /*
                 * The candidate method should be declared in the interface. If not, then we require it to have
                 * a @UML annotation. The latter case happen when the Apache SIS implementation contains methods
                 * for a new international standard not yet reflected in the GeoAPI interfaces.
                 */
                if (MetadataStandard.IMPLEMENTATION_CAN_ALTER_API) {
                    if (type == implementation) {
                        if (!type.isInterface() && !candidate.isAnnotationPresent(UML.class)) {
                            continue;           // @UML considered optional only for interfaces.
                        }
                    } else try {
                        candidate = type.getMethod(name, (Class[]) null);
                    } catch (NoSuchMethodException e) {
                        if (!candidate.isAnnotationPresent(UML.class)) {
                            continue;           // Not a method from an interface, and no @UML in implementation.
                        }
                    }
                }
                /*
                 * At this point, we are ready to accept the method. Before doing so, check if the method override
                 * another method defined in a parent class with a covariant return type. The JVM considers such
                 * cases as two different methods, while from a Java developer point of view this is the same method.
                 */
                final Integer pi = indices.put(name, count);
                if (pi != null) {
                    final Class<?> pt = getters[pi].getReturnType();
                    final Class<?> ct = candidate  .getReturnType();
                    if (ct.isAssignableFrom(pt)) {
                        continue;                       // Previous type was more accurate.
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
         * Sort the standard methods before to add the extra methods (if any) in order to keep
         * the extra methods last. The code checking for extra methods requires them to be last.
         */
        Arrays.sort(getters, 0, count, new PropertyComparator(implementation, standardImpl));
        if (!hasExtraGetter) {
            if (getters.length == count) {
                getters = Arrays.copyOf(getters, count+1);
            }
            getters[count++] = EXTRA_GETTER;
        }
        getters = ArraysExt.resize(getters, count);
        return getters;
    }

    /**
     * Returns the number of properties that can be read.
     * This is the properties to show in map or tree, <strong>not</strong> including
     * hidden properties like deprecated methods or {@link #EXTRA_GETTER} method.
     *
     * @see #count(Object, ValueExistencePolicy, int)
     */
    final int count() {
        return standardCount;
    }

    /**
     * Returns the index of the specified property, or -1 if none.
     * The search is case-insensitive.
     *
     * @param  name       the name of the property to search.
     * @param  mandatory  whether this method shall throw an exception or return {@code -1}
     *                    if the given name is not found.
     * @return the index of the given name, or -1 if none and {@code mandatory} is {@code false}.
     * @throws IllegalArgumentException if the name is not found and {@code mandatory} is {@code true}.
     */
    @SuppressWarnings("StringEquality")
    final int indexOf(final String name, final boolean mandatory) {
        Integer index = mapping.get(name);
        if (index == null) {
            /*
             * Make a second try with lower cases only if the first try failed, because
             * most of the time the key name will have exactly the expected case and using
             * directly the given String instance allow usage of its cached hash code value.
             */
            final String key = CharSequences.replace(name, " ", "").toString().toLowerCase(Locale.ROOT).strip();
            if (key == name || (index = mapping.get(key)) == null) {        // Identity comparison is okay here.
                if (!mandatory) {
                    return -1;
                }
                throw new IllegalArgumentException(Errors.format(Errors.Keys.PropertyNotFound_2, type, name));
            }
        }
        return index;
    }

    /**
     * Returns whether the property at the given index is mandatory, optional or conditional.
     *
     * @param  index  the index of the property for which to get the obligation.
     * @return the obligation at the given index, or {@code null} if none or if the index is out of bounds.
     */
    final Obligation obligation(final int index) {
        if (index >= 0 && index < names.length) {
            final UML uml = getters[index].getAnnotation(UML.class);
            if (uml != null) {
                return uml.obligation();
            }
        }
        return null;
    }

    /**
     * Returns the name of the property at the given index, or {@code null} if none.
     *
     * @param  index      the index of the property for which to get the name.
     * @param  keyPolicy  the kind of name to return.
     * @return the name of the given kind at the given index, or {@code null} if the index is out of bounds.
     */
    @SuppressWarnings("fallthrough")
    @Workaround(library="JDK", version="10")                        // Actually apply to String.intern() below.
    final String name(final int index, final KeyNamePolicy keyPolicy) {
        if (index >= 0 && index < names.length) {
            switch (keyPolicy) {
                case UML_IDENTIFIER: {
                    final UML uml = getters[index].getAnnotation(UML.class);
                    if (uml != null) {
                        /*
                         * Workaround here: I though that annotation strings were interned like any other constants,
                         * but it does not seem to be the case as of JDK 10. To check if a future JDK release still
                         * needs this explicit call to String.intern(), try to remove the ".intern()" part and run
                         * the NameMapTest.testStringIntern() method.
                         */
                        return uml.identifier().intern();
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
     * @param  index   the index of the property.
     * @param  policy  the kind of type to return.
     * @return the type of property values, or {@code null} if unknown.
     */
    Class<?> type(final int index, final TypeValuePolicy policy) {
        if (index >= 0 && index < allCount) {
            switch (policy) {
                case ELEMENT_TYPE: {
                    return elementTypes[index];
                }
                case PROPERTY_TYPE: {
                    final Class<?> returnType = getters[index].getReturnType();
                    return Classes.isParameterizedProperty(returnType) ? elementTypes[index] : returnType;
                }
                case DECLARING_INTERFACE: {
                    return getters[index].getDeclaringClass();
                }
                case DECLARING_CLASS: {
                    Method getter = getters[index];
                    if (implementation != type) try {
                        getter = implementation.getMethod(getter.getName(), (Class<?>[]) null);
                    } catch (NoSuchMethodException error) {
                        /*
                         * Should never happen, since the implementation class
                         * implements the interface where the getter come from.
                         */
                        throw new AssertionError(error);
                    }
                    return getter.getDeclaringClass();
                }
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if the type at the given index is {@link Collection} or {@link Map}.
     */
    final boolean isCollectionOrMap(final int index) {
        if (index >= 0 && index < allCount) {
            final Class<?> pt = getters[index].getReturnType();
            return Collection.class.isAssignableFrom(pt) || Map.class.isAssignableFrom(pt);
        }
        return false;
    }

    /**
     * Returns {@code true} if the type at the given index is {@link Map}.
     */
    final boolean isMap(final int index) {
        return (index >= 0 && index < allCount) &&  elementTypes[index] == Map.Entry.class;
    }

    /**
     * Returns {@code true} if the property at the given index is deprecated, either in the interface that
     * declare the method or in the implementation class. A method may be deprecated in the implementation
     * but not in the interface when the implementation has been updated for a new standard
     * while the interface is still reflecting the old standard.
     */
    private boolean isDeprecated(final int index) {
        return PropertyComparator.isDeprecated(implementation, getters[index]);
    }

    /**
     * Returns the information for the property at the given index.
     * The information are created when first needed.
     *
     * @param  standard  the standard which define the {@link #type} interface.
     * @param  index     the index of the property for which to get the information.
     * @return the information for the property at the given index, or {@code null} if the index is out of bounds.
     *
     * @see PropertyInformation
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    final synchronized ExtendedElementInformation information(final Citation standard, final int index) {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        ExtendedElementInformation[] informations = this.informations;
        if (informations == null) {
            this.informations = informations = new PropertyInformation<?>[standardCount];
        }
        if (index < 0 || index >= informations.length) {
            return null;
        }
        ExtendedElementInformation information = informations[index];
        if (information == null) {
            final Class<?> elementType = elementTypes[index];
            final String   name        = name(index, KeyNamePolicy.UML_IDENTIFIER);
            final Method   getter      = getters[index];
            final ValueRange range;
            try {
                range = implementation.getMethod(getter.getName(), (Class<?>[]) null).getAnnotation(ValueRange.class);
            } catch (NoSuchMethodException error) {
                /*
                 * Should never happen, since the implementation class
                 * implements the interface where the getter come from.
                 */
                throw new AssertionError(error);
            }
            information = new PropertyInformation<>(standard, name, getter, elementType, range);
            informations[index] = information;
        }
        return information;
    }

    /**
     * Returns a remark or warning to format with the value at the given index, or {@code null} if none.
     * This is provided when the value may look surprising, for example the longitude values in a geographic
     * bounding box crossing the anti-meridian.
     */
    CharSequence remarks(int index, Object metadata) {
        return null;
    }

    /**
     * Returns {@code true} if the given collection is immutable (implies unmodifiable).
     * In case of doubt, this method returns {@code false}.
     * A null collection is considered as immutable.
     *
     * @param  collection  the collection, or {@code null}.
     * @return {@code true} if the given collection is null or immutable.
     *
     * @see CheckedContainer.Mutability#IMMUTABLE
     */
    private static boolean isImmutable(final Collection<?> collection) {
        if (collection == null) {
            return true;
        }
        if (collection instanceof CheckedContainer<?>) {
            return ((CheckedContainer<?>) collection).getMutability() == CheckedContainer.Mutability.IMMUTABLE;
        }
        return false;
    }

    /**
     * Returns a snapshot of the given collection. The returned collection will not be affected
     * by changes in the given collection after this method call. This method makes no guarantees
     * about whether the returned collection is modifiable.
     *
     * <h4>Difference with standard methods</h4>
     * This method differs from {@link List#copyOf(Collection)} and {@link Set#copyOf(Collection)}
     * in that it accepts null elements, preserves element order, does not copy the collections marked
     * by <abbr>SIS</abbr> as immutable, and is cheap for the other cases (does not recreate a hash table).
     * The latter is at the cost of inefficient {@link Set#contains(Object)} method.
     *
     * <p>This method tries to be cheap because, while the snapshot is necessary for compliance with the
     * method contract of the collection framework, very often the returned object will be just ignored.</p>
     *
     * @param  <E>   the type of elements in the collection.
     * @param  data  the collection for which to take a snapshot, or {@code null} if none.
     * @return a snapshot of the given collection, or {@code data} itself if null or unmodifiable.
     */
    static <E> Collection<E> snapshot(final Collection<E> data) {
        if (isImmutable(data)) {
            return data;
        }
        final boolean isSet = (data instanceof Set<?>);
        switch (data.size()) {
            case 0: {
                return isSet ? Collections.emptySet() : Collections.emptyList();
            }
            case 1: {
                final E value = data.iterator().next();
                return isSet ? Collections.singleton(value) : Collections.singletonList(value);
            }
            default: {
                @SuppressWarnings("unchecked")
                final var copy = (List<E>) Arrays.asList(data.toArray());
                return isSet ? new ViewAsSet<>(copy) : copy;
            }
        }
    }

    /**
     * Returns a snapshot of the given map. The returned map will not be affected by changes in the given map
     * after this method call. This method makes no guarantees about whether the returned map is modifiable.
     *
     * <h4>Difference with standard methods</h4>
     * This method differs from {@link Map#copyOf(Map)} in that it accepts null elements, preserves element order,
     * and is cheap (does not recreate a hash table). The latter is at the cost of inefficient {@code get(Object)}
     * and related methods.
     *
     * <p>This method tries to be cheap because, while the snapshot is necessary for compliance with the
     * method contract of the collection framework, very often the returned object will be just ignored.</p>
     *
     * @param  <K>   the type of keys in the map.
     * @param  <V>   the type of values in the map.
     * @param  data  the map to copy, or {@code null}.
     * @return a snapshot of the given map, or {@code data} itself if null or unmodifiable.
     */
    static <K,V> Map<K,V> snapshot(final Map<K,V> data) {
        if (data == null) {
            return null;
        }
        switch (data.size()) {
            case 0: {
                return Collections.emptyMap();
            }
            case 1: {
                final Map.Entry<K,V> entry = data.entrySet().iterator().next();
                return Collections.singletonMap(entry.getKey(), entry.getValue());
            }
            default: {
                @SuppressWarnings("unchecked")
                final List<Map.Entry<K,V>> copy = Arrays.asList(data.entrySet().toArray(Map.Entry[]::new));
                final var entries = new ViewAsSet<>(copy);
                return new AbstractMap<K,V>() {
                    @Override public Set<Map.Entry<K,V>> entrySet() {return entries;}
                };
            }
        }
    }

    /**
     * Returns {@code true} if the {@link #implementation} class has at least one setter method.
     */
    final boolean isWritable() {
        return setters != null;
    }

    /**
     * Returns {@code true} if the property at the given index is writable.
     */
    final boolean isWritable(final int index) {
        return (index >= 0) && (index < allCount) && (setters != null) && (setters[index] != null);
    }

    /**
     * Returns the value for the specified metadata, or {@code null} if none.
     * If the given index is out of bounds, then this method returns {@code null},
     * so it is safe to invoke this method even if {@link #indexOf(String, boolean)}
     * returned -1.
     *
     * @param  index     the index of the property for which to get a value.
     * @param  metadata  the metadata object to query.
     * @return the value, or {@code null} if none or if the given is out of bounds.
     * @throws BackingStoreException if the implementation threw a checked exception.
     */
    Object get(final int index, final Object metadata) throws BackingStoreException {
        return (index >= 0 && index < allCount) ? get(getters[index], metadata) : null;
    }

    /**
     * Gets a value from the specified metadata. We do not expect any checked exception to be
     * thrown, since classes in the {@code org.opengis.metadata} packages do not declare any.
     * However if a checked exception is throw anyway (maybe in user defined "standard"), it
     * will be wrapped in a {@link BackingStoreException}. Unchecked exceptions are propagated.
     *
     * @param  method    the method to use for the query.
     * @param  metadata  the metadata object to query.
     * @throws BackingStoreException if the implementation threw a checked exception.
     *
     * @see #set(Method, Object, Object[])
     */
    private static Object get(final Method method, final Object metadata) throws BackingStoreException {
        assert (method.getReturnType() != Void.TYPE) : method;
        try {
            try {
                return method.invoke(metadata, (Object[]) null);
            } catch (IllegalArgumentException e) {
                /*
                 * May happen if the getter method is defined only in the implementation class — not in the interface —
                 * but the given metadata object is an instance of another implementation class than the expected one.
                 * Example: CI_Citation.graphics didn't existed in ISO 19115:2003 and has been added in ISO 19115:2014.
                 * Consequently, there is no Citation.getGraphics() method in GeoAPI 3.0 interfaces (only in GeoAPI 3.1),
                 * but there is a DefaultCitation.getGraphics() method in Apache SIS implementation since some versions
                 * are ahead of GeoAPI. But if the given `metadata` instance is a different implementation of Citation
                 * interface, then attempt to invoke DefaultCitation.getGraphics() fail with IllegalArgumentException.
                 * In such case, we check if that implementation has a public method with exactly same signature.
                 * If yes, we try to invoke that method before to give up.
                 */
                if (method.getDeclaringClass().isInstance(metadata)) {
                    throw e;                                // Exception thrown for another reason. This is probably a bug.
                }
                if (MetadataStandard.IMPLEMENTATION_CAN_ALTER_API) try {
                    final Method specific = metadata.getClass().getMethod(method.getName(), method.getParameterTypes());
                    if (method.getReturnType().equals(specific.getReturnType())) {
                        return specific.invoke(metadata, (Object[]) null);
                    }
                } catch (NoSuchMethodException ex) {
                    // Ignore.
                }
                return null;
            }
        } catch (IllegalAccessException e) {
            /*
             * Should never happen since `getters` should contain only public methods.
             */
            throw new AssertionError(method.toString(), e);
        } catch (InvocationTargetException e) {
            /*
             * Exception in user code (not a wrong usage of reflection).
             */
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
     * Sets a value for the specified metadata and returns the old value if {@code mode} is
     * {@link #RETURN_PREVIOUS}. The {@code mode} argument can be one of the following:
     *
     * <ul>
     *   <li>RETURN_NULL:      Set the value and returns {@code null}.</li>
     *   <li>RETURN_PREVIOUS:  Set the value and returns the previous value. If the previous value was a
     *                         collection or a map, then that value is copied in a new collection or map
     *                         before the new value is set because the setter methods typically copy the
     *                         new collection in their existing instance.</li>
     *   <li>APPEND:           Set the value only if it does not overwrite an existing value, then returns
     *                         {@link Boolean#TRUE} if the metadata changed as a result of this method call,
     *                         {@link Boolean#FALSE} if the metadata didn't changed or {@code null} if the
     *                         value cannot be set because another value already exists.</li>
     *   <li>IGNORE_READ_ONLY: Set the value and returns {@code null} on success. If the property is read-only,
     *                         do not throw an exception; returns exception class instead.</li>
     * </ul>
     *
     * <p>The {@code APPEND} mode has an additional side effect: it sets the {@code append} argument to
     * {@code true} in the call to the {@link #convert(Method, Object, Object, Object[], Class, boolean)}
     * method. See the {@code convert} javadoc for more information.</p>
     *
     * <p>If the given index is out of bounds, then this method does nothing and return {@code null}.
     * We do that because the {@link ValueMap#remove(Object)} method may invoke this method with
     * an index of -1 if the {@link #indexOf(String, boolean)} method didn't found the property name.
     * However, the given value will be silently discarded, so index out-of-bounds shall be used only
     * in the context of {@code remove} operations (this is not verified).</p>
     *
     * @param  index     the index of the property to set.
     * @param  metadata  the metadata object on which to set the value.
     * @param  value     the new value.
     * @param  mode      whether this method should first fetches the old value,
     *                   as one of the constants listed in this method javadoc.
     * @return the old value, or {@code null} if {@code mode} was {@code RETURN_NULL} or {@code IGNORE_READ_ONLY}.
     * @throws UnmodifiableMetadataException if the property for the given key is read-only.
     * @throws ClassCastException if the given value is not of the expected type.
     * @throws BackingStoreException if the implementation threw a checked exception.
     */
    Object set(final int index, final Object metadata, final Object value, final int mode)
            throws UnmodifiableMetadataException, ClassCastException, BackingStoreException
    {
        if (index < 0 || index >= allCount) {
            return null;
        }
        if (setters != null) {
            final Method getter = getters[index];
            final Method setter = setters[index];
            if (setter != null) {
                final Object oldValue;
                final Object snapshot;                      // Copy of oldValue before modification.
                switch (mode) {
                    case IGNORE_READ_ONLY:
                    case RETURN_NULL: {
                        oldValue = null;
                        snapshot = null;
                        break;
                    }
                    case APPEND: {
                        oldValue = get(getter, metadata);
                        snapshot = null;
                        break;
                    }
                    case RETURN_PREVIOUS: {
                        oldValue = get(getter, metadata);
                        if (oldValue instanceof Collection<?>) {
                            snapshot = snapshot((Collection<?>) oldValue);
                        } else if (oldValue instanceof Map<?,?>) {
                            snapshot = snapshot((Map<?,?>) oldValue);
                        } else {
                            snapshot = oldValue;
                        }
                        break;
                    }
                    default: throw new AssertionError(mode);
                }
                /*
                 * Converts the new value to a type acceptable for the setter method (if possible).
                 * If the new value is a singleton while the expected type is a collection, then the `convert`
                 * method added the singleton in the existing collection, which may result in no change if the
                 * collection is a Set and the new value already exists in that Set. If we detect that there is
                 * no change, then we don't need to invoke the setter method. Note that we conservatively assume
                 * that there is always a change in RETURN_NULL mode since we don't know the previous value.
                 */
                final Object[] newValues = new Object[] {value};
                Boolean changed = convert(getter, metadata, oldValue, newValues, elementTypes[index], mode == APPEND);
                if (changed == null) {
                    changed = (mode == RETURN_NULL) || (mode == IGNORE_READ_ONLY) || (newValues[0] != oldValue);
                    if (changed && mode == APPEND && !isNullOrEmpty(oldValue)) {
                        /*
                         * If `convert` did not added the value in a collection and if a value already
                         * exists, do not modify the existing value. Exit now with "no change" status.
                         */
                        return null;
                    }
                }
                if (changed) {
                    set(setter, metadata, newValues);
                }
                return (mode == APPEND) ? changed : snapshot;
            }
        }
        if (mode == IGNORE_READ_ONLY) {
            return UnmodifiableMetadataException.class;
        }
        throw new UnmodifiableMetadataException(Errors.format(
                Errors.Keys.CanNotSetPropertyValue_1, type.getSimpleName() + '.' + names[index]));
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
     * @param  setter     the method to use for setting the new value.
     * @param  metadata   the metadata object to query.
     * @param  newValues  the argument to give to the method to be invoked.
     * @throws BackingStoreException if the implementation threw a checked exception.
     *
     * @see #get(Method, Object)
     */
    private static void set(final Method setter, final Object metadata, final Object[] newValues)
            throws BackingStoreException
    {
        try {
            setter.invoke(metadata, newValues);
        } catch (IllegalAccessException e) {
            // Should never happen since `setters` should contain only public methods.
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
     * The values are converted in-place in the {@code newValues} array. We use an array instead
     * of a single argument and return value because an array will be needed anyway for invoking
     * the {@link #convert(Object[], Class)} and {@link Method#invoke(Object, Object[])} methods.
     *
     * <h4>The collection special case</h4>
     * If the metadata property is a collection, then there is a choice:
     *
     * <ul>
     *   <li>If {@code append} is {@code true}, then the new value (which may itself be a collection)
     *       is unconditionally added to the previous collection.</li>
     *   <li>If {@code append} is {@code false} and the new value is <strong>not</strong> a collection,
     *       then the new value is added to the existing collection. In other words, we behave as a
     *       <i>multi-values map</i> for the properties that allow multi-values.</li>
     *   <li>Otherwise the new collection replaces the previous collection. All previous values
     *       are discarded.</li>
     * </ul>
     *
     * Adding new values to the previous collection may or may not change the original metadata
     * depending on whether those collections are live or not. In Apache SIS implementation,
     * those collections are live. However, this method can be though as if the collections were
     * not live, since the caller will invoke the setter method with the collection anyway.
     *
     * @param  getter       the method to use for fetching the previous value.
     * @param  metadata     the metadata object to query and modify.
     * @param  oldValue     the value returned by {@code get(getter, metadata)}, or {@code null} if unknown.
     *                      This parameter is only an optimization for avoiding to invoke the getter method
     *                      twice if the value is already known.
     * @param  newValues    the argument to convert. The content of this array will be modified in-place.
     *                      Current implementation requires an array of length 1, however this restriction
     *                      may be relaxed in a future SIS version if needed.
     * @param  elementType  the target type (if singleton) or the type of elements in the collection.
     * @param  append       if {@code true} and the value is a collection, then that collection will be added
     *                      to any previously existing collection instead of replacing it.
     * @return if the given value has been added to an existing collection, then whether that existing
     *         collection has been modified as a result of this method call. Otherwise {@code null}.
     * @throws ClassCastException if the element of the {@code arguments} array is not of the expected type.
     * @throws BackingStoreException if the implementation threw a checked exception.
     */
    private Boolean convert(final Method getter, final Object metadata, Object oldValue, final Object[] newValues,
            Class<?> elementType, final boolean append) throws ClassCastException, BackingStoreException
    {
        assert newValues.length == 1;
        Object newValue = newValues[0];
        Class<?> targetType = getter.getReturnType();
        if (newValue == null) {
            // Cannot test elementType, because it has been converted to the wrapper class.
            if (targetType.isPrimitive()) {
                newValues[0] = NumberType.forNumberClass(targetType).nilValue();
            }
            return null;
        }
        Boolean changed = null;
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
                if (!it.hasNext()) {                            // If empty, process like null argument.
                    newValues[0] = null;
                    return null;
                }
                final Object next = it.next();
                if (!it.hasNext()) {                            // Singleton
                    newValue = next;
                }
                /*
                 * Other cases: let the collection unchanged. It is likely to
                 * cause an exception later. The message should be appropriate.
                 */
            }
            targetType = NumberType.primitiveToWrapper(targetType);
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
            final boolean isCollection = (newValue instanceof Collection<?>);
            final Object[] elements = isCollection ? ((Collection<?>) newValue).toArray() : new Object[] {newValue};
            final List<Object> elementList = Arrays.asList(elements);         // Converted later (see above comment).
            newValue = elementList;         // Still contains the same values, but now guaranteed to be a collection.
            Collection<?> addTo = null;
            if (!isCollection || append) {
                if (oldValue == null) {
                    oldValue = get(getter, metadata);
                }
                if (oldValue != null) {
                    addTo = (Collection<?>) oldValue;
                    if (addTo instanceof CheckedContainer<?>) {
                        // Get the explicitly-specified element type.
                        elementType = ((CheckedContainer<?>) addTo).getElementType();
                    }
                    newValue = addTo;
                }
            }
            if (elementType != null) {
                convert(elements, elementType);
            }
            /*
             * We now have objects of the appropriate type. If we have a singleton to be added
             * in an existing collection, add it now. In that case the `newValue` should refer
             * to the `addTo` collection. We rely on the ModifiableMetadata.writeCollection(…)
             * optimization for detecting that the new collection is the same instance as
             * the old one so there is nothing to do. We could exit from the method, but let
             * it continues in case the user override the `setFoo(…)` method.
             */
            if (addTo != null) {
                /*
                 * Unsafe addition into a collection. In SIS implementation, the collection is
                 * actually an instance of CheckedCollection, so the check will be performed at
                 * runtime. However, other implementations could use unchecked collection.
                 * There is not much we can do...
                 */
                changed = Unsafe.addAll(addTo, elementList);
            }
        }
        /*
         * If the expected type was not a collection, the conversion of user value happen
         * here. Otherwise conversion from List to Set (if needed) happen here.
         */
        newValues[0] = newValue;
        convert(newValues, targetType);
        return changed;
    }

    /**
     * Converts values in the specified array to the given type.
     * The array content is modified in-place. This method accepts an array instead of
     * a single value because the values to convert may be the content of a collection.
     *
     * @param  elements    the array which contains element to convert.
     * @param  targetType  the base type of target elements.
     * @throws ClassCastException if an element cannot be converted.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private void convert(final Object[] elements, final Class<?> targetType) throws ClassCastException {
        boolean hasNewConverter = false;
        ObjectConverter converter = null;               // Intentionally raw type. Checks are done below.
        for (int i=0; i<elements.length; i++) {
            final Object value = elements[i];
            if (value != null) {
                final Class<?> sourceType = value.getClass();
                if (!targetType.isAssignableFrom(sourceType)) try {
                    if (converter == null) {
                        converter = lastConverter;      // Volatile field - read only if needed.
                    }
                    /*
                     * Require the exact same classes, not parent or subclass,
                     * otherwise the converter could be stricter than necessary.
                     */
                    if (converter == null || converter.getSourceClass() != sourceType
                                          || converter.getTargetClass() != targetType)
                    {
                        converter = ObjectConverters.find(sourceType, targetType);
                        hasNewConverter = true;
                    }
                    elements[i] = converter.apply(value);
                } catch (UnconvertibleObjectException cause) {
                    throw (ClassCastException) new ClassCastException(Errors.format(
                            Errors.Keys.IllegalClass_2, targetType, sourceType)).initCause(cause);
                }
            }
        }
        if (hasNewConverter) {
            lastConverter = converter;          // Volatile field - store only if needed.
        }
    }

    /**
     * Counts the number of non-null or non-empty properties.
     * The {@code mode} argument can be one of the following:
     *
     * <ul>
     *   <li>COUNT_FIRST:   stop at the first property found. This mode is used for testing if a
     *                      metadata is empty or not, without the need to known the exact count.</li>
     *   <li>COUNT_SHALLOW: count all properties, counting collections as one property.</li>
     *   <li>COUNT_DEEP:    count all properties, counting collections as the number of
     *                      properties returned by {@link Collection#size()}.</li>
     * </ul>
     *
     * @param  mode         kinds of count, as described above.
     * @param  valuePolicy  the behavior of the count toward null or empty values.
     * @throws BackingStoreException if the implementation threw a checked exception.
     *
     * @see #count()
     */
    final int count(final Object metadata, final ValueExistencePolicy valuePolicy, final int mode)
            throws BackingStoreException
    {
        assert type.isInstance(metadata) : metadata;
        if (valuePolicy == ValueExistencePolicy.ALL && mode != COUNT_DEEP) {
            return count();
        }
        int count = 0;
        // Use `standardCount` instead of `allCount` for ignoring deprecated methods.
        for (int i=0; i<standardCount; i++) {
            final Object value = get(getters[i], metadata);
            if (!valuePolicy.isSkipped(value)) {
                switch (mode) {
                    case COUNT_FIRST:{
                        return 1;
                    }
                    case COUNT_SHALLOW:{
                        count++;
                        break;
                    }
                    case COUNT_DEEP: {
                        /*
                         * Count always at least one element because if the user wanted to skip null or empty
                         * collections, then `valuePolicy.isSkipped(value)` above would have returned `true`.
                         */
                        count += isCollectionOrMap(i) ? Math.max(size(value), 1) : 1;
                        break;
                    }
                    default: throw new AssertionError(mode);
                }
            }
        }
        return count;
    }

    /**
     * Returns the number of elements if the given object is a collection or a map.
     * Otherwise returns 0 if the given object if null or 1 otherwise.
     *
     * @param  c  the collection or map for which to get the size, or {@code null}.
     * @return the size or pseudo-size of the given object.
     */
    static int size(final Object c) {
        if (c == null) {
            return 0;
        } else if (c instanceof Collection<?>) {
            return ((Collection<?>) c).size();
        } else if (c instanceof Map<?,?>) {
            return ((Map<?,?>) c).size();
        } else {
            return 1;
        }
    }

    /**
     * Compares the two specified metadata objects. This method implements a <em>shallow</em> comparison,
     * i.e. all metadata properties are compared using their {@code properties.equals(…)} method
     * without explicit calls to this {@code accessor.equals(…)} method for children.
     * However, the final result may still be a deep comparison.
     *
     * @param  metadata1  the first metadata object to compare. This object determines the accessor.
     * @param  metadata2  the second metadata object to compare.
     * @param  mode       the strictness level of the comparison.
     * @throws BackingStoreException if the implementation threw a checked exception.
     *
     * @see MetadataStandard#equals(Object, Object, ComparisonMode)
     */
    public boolean equals(final Object metadata1, final Object metadata2, final ComparisonMode mode)
            throws BackingStoreException
    {
        assert type.isInstance(metadata1) : metadata1;
        assert type.isInstance(metadata2) : metadata2;
        for (int i=0; i<standardCount; i++) {
            final Method method = getters[i];
            final Object value1 = get(method, metadata1);
            final Object value2 = get(method, metadata2);
            if (isNullOrEmpty(value1) && isNullOrEmpty(value2)) {
                /*
                 * Consider empty collections/arrays as equal to null.
                 * Empty strings are also considered equal to null (this is more questionable).
                 */
                continue;
            }
            final boolean equals;
            if ((value1 instanceof Double || value1 instanceof Float) &&
                (value2 instanceof Double || value2 instanceof Float))
            {
                equals = Numerics.epsilonEqual(((Number) value1).doubleValue(),
                                               ((Number) value2).doubleValue(), mode);
            } else {
                equals = Utilities.deepEquals(value1, value2, mode);
            }
            if (!equals) {
                assert (mode != ComparisonMode.DEBUG) : type.getSimpleName() + '.' + names[i] + " differ.";
                return false;
            }
        }
        /*
         * One final check for the IdentifiedObjects.getIdentifiers() collection.
         */
        if (mode == ComparisonMode.STRICT && EXTRA_GETTER.getDeclaringClass().isInstance(metadata2)) {
            final Object value1 = get(EXTRA_GETTER, metadata1);
            final Object value2 = get(EXTRA_GETTER, metadata2);
            if (!isNullOrEmpty(value1) || !isNullOrEmpty(value2)) {
                return Utilities.deepEquals(value1, value2, mode);
            }
        }
        return true;
    }

    /**
     * Invokes {@link MetadataVisitor#visit(Class, Object)} for all non-null properties in the given metadata.
     * This method is not recursive, i.e. it does not traverse the children of the elements in the given metadata.
     *
     * @param  visitor   the object on which to invoke {@link MetadataVisitor#visit(Class, Object)}.
     * @param  metadata  the metadata instance for which to visit the non-null properties.
     * @throws Exception if an error occurred while visiting a property.
     */
    final void walkReadable(final MetadataVisitor<?> visitor, final Object metadata) throws Exception {
        assert type.isInstance(metadata) : metadata;
        for (int i=0; i<standardCount; i++) {
            visitor.setCurrentProperty(names[i]);
            final Object value = get(getters[i], metadata);
            if (value != null) {
                final Object result = visitor.visit(elementTypes[i], value);
                if (result != value) {
                    if (result == MetadataVisitor.SKIP_SIBLINGS) break;
                    set(i, metadata, result, IGNORE_READ_ONLY);
                }
            }
        }
    }

    /**
     * Invokes {@link MetadataVisitor#visit(Class, Object)} for all writable properties in the given metadata.
     * This method is not recursive, i.e. it does not traverse the children of the elements in the given metadata.
     *
     * <h4>Constraint</h4>
     * In current implementation, if {@code source} and {@code target} are not the same,
     * then {@code target} is assumed empty. The intent is to skip easily null or empty properties.
     *
     * @param  visitor   the object on which to invoke {@link MetadataVisitor#visit(Class, Object)}.
     * @param  source    the metadata from which to read properties. May be the same as {@code target}.
     * @param  target    the metadata instance where to write properties.
     * @throws Exception if an error occurred while visiting a property.
     */
    final void walkWritable(final MetadataVisitor<?> visitor, final Object source, final Object target) throws Exception {
        assert type.isInstance(source) : source;
        assert type.isInstance(target) : target;
        if (setters == null || !implementation.isInstance(target)) {
            return;
        }
        final Object[] arguments = new Object[1];
        for (int i=0; i<allCount; i++) {
            visitor.setCurrentProperty(names[i]);
            final Method setter = setters[i];
            if (setter != null) {
                if (setter.isAnnotationPresent(Deprecated.class)) {
                    /*
                     * We need to skip deprecated setter methods, because those methods may delegate
                     * their work to other setter methods in different objects and those objects may
                     * have been made unmodifiable by previous iteration in this loop.  If we do not
                     * skip them, we may get an UnmodifiableMetadataException in the call to set(…).
                     *
                     * Note that in some cases, only the setter method is deprecated, not the getter.
                     * This happen when Apache SIS classes represent a more recent ISO standard than
                     * the GeoAPI interfaces.
                     */
                    continue;
                }
                final Object value = get(getters[i], source);
                final Object result = visitor.visit(elementTypes[i], value);
                if (source == target ? (result != value) : !isNullOrEmpty(result)) {    // See "constraint" in Javadoc
                    if (result == MetadataVisitor.SKIP_SIBLINGS) break;
                    arguments[0] = result;
                    set(setter, target, arguments);
                    /*
                     * We invoke the set(…) method variant that do not perform type conversion
                     * because we do not want it to replace the immutable collections created
                     * by ModifiableMetadata.unmodifiable(source). Conversions should not be
                     * required anyway because the getter method should have returned a value
                     * compatible with the setter method - this contract is ensured by the
                     * way the PropertyAccessor constructor selected the setter methods.
                     */
                }
            }
        }
    }

    /**
     * Returns a string representation of this accessor for debugging purpose.
     * Output example:
     *
     * <pre class="text">PropertyAccessor[13 getters &amp; 13 setters in DefaultCitation:Citation]</pre>
     */
    @Override
    public String toString() {
        final var buffer = new StringBuilder(60);
        buffer.append("PropertyAccessor[").append(standardCount).append(" getters");
        final int extra = allCount - standardCount;
        if (extra != 0) {
            buffer.append(" (+").append(extra).append(" ext.)");
        }
        if (setters != null) {
            int c = 0;
            for (final Method setter : setters) {
                if (setter != null) {
                    c++;
                }
            }
            buffer.append(" & ").append(c).append(" setters");
        }
        buffer.append(" in ").append(Classes.getShortName(implementation));
        if (type != implementation) {
            buffer.append(':').append(Classes.getShortName(type));
        }
        return buffer.append(']').toString();
    }
}
