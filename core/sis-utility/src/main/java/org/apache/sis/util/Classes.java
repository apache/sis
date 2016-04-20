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
package org.apache.sis.util;

import java.util.Set;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.lang.reflect.Type;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.WildcardType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;

import static org.apache.sis.util.collection.Containers.hashMapCapacity;


/**
 * Static methods working on {@link Class} objects.
 * This class defines helper methods for working with reflection.
 * Some functionalities are:
 *
 * <ul>
 *   <li>Add or remove dimension to an array type
 *       ({@link #changeArrayDimension(Class, int) changeArrayDimension})</li>
 *   <li>Find the common parent of two or more classes
 *       ({@link #findCommonClass(Class, Class) findCommonClass},
 *        {@link #findCommonInterfaces(Class, Class) findCommonInterfaces})</li>
 *   <li>Getting the bounds of a parameterized field or method
 *       ({@link #boundOfParameterizedProperty(Method) boundOfParameterizedProperty})</li>
 *   <li>Getting a short class name ({@link #getShortName(Class) getShortName},
 *       {@link #getShortClassName(Object) getShortClassName})</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
public final class Classes extends Static {
    /**
     * An empty array of classes.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private static final Class<Object>[] EMPTY_ARRAY = new Class[0];

    /**
     * Methods to be rejected by {@link #isPossibleGetter(Method)}. They are mostly methods inherited
     * from {@link Object}. Only no-argument methods having a non-void return value need to be
     * declared in this list.
     *
     * <p>Note that testing {@code type.getDeclaringClass().equals(Object.class)}
     * is not sufficient because those methods may be overridden in subclasses.</p>
     */
    private static final String[] EXCLUDES = {
        "clone", "getClass", "hashCode", "toString", "toWKT"
    };

    /**
     * Do not allow instantiation of this class.
     */
    private Classes() {
    }

    /**
     * Changes the array dimension by the given amount. The given class can be a primitive type,
     * a Java object, or an array of the above. If the given {@code dimension} is positive, then
     * the array dimension will be increased by that amount. For example a change of dimension 1
     * will change a {@code int} class into {@code int[]}, and a {@code String[]} class into
     * {@code String[][]}. A change of dimension 2 is like applying a change of dimension 1 two
     * times.
     *
     * <p>The change of dimension can also be negative. For example a change of dimension -1 will
     * change a {@code String[]} class into a {@code String}. More specifically:</p>
     *
     * <ul>
     *   <li>If the given {@code element} is null, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given {@code dimension} change is 0, then the given {@code element}
     *       is returned unchanged.</li>
     *   <li>Otherwise if the given {@code dimension} change is negative, then
     *       {@link Class#getComponentType()} is invoked {@code abs(dimension)} times.
     *       The result is a {@code null} value if {@code abs(dimension)} is greater
     *       than the array dimension.</li>
     *   <li>Otherwise if {@code element} is {@link Void#TYPE}, then this method returns
     *       {@code Void.TYPE} since arrays of {@code void} don't exist.</li>
     *   <li>Otherwise this method returns a class that represents an array of the given
     *       class augmented by the given amount of dimensions.</li>
     * </ul>
     *
     * @param  element The type of elements in the array.
     * @param  dimension The change of dimension, as a negative or positive number.
     * @return The type of an array of the given element type augmented by the given
     *         number of dimensions (which may be negative), or {@code null}.
     */
    public static Class<?> changeArrayDimension(Class<?> element, int dimension) {
        if (dimension != 0 && element != null) {
            if (dimension < 0) {
                do element = element.getComponentType();
                while (element!=null && ++dimension != 0);
            } else if (element != Void.TYPE) {
                final StringBuilder buffer = new StringBuilder();
                do buffer.insert(0, '[');
                while (--dimension != 0);
                if (element.isPrimitive()) {
                    buffer.append(Numbers.getInternal(element));
                } else if (element.isArray()) {
                    buffer.append(element.getName());
                } else {
                    buffer.append('L').append(element.getName()).append(';');
                }
                final String name = buffer.toString();
                try {
                    element = Class.forName(name);
                } catch (ClassNotFoundException e) {
                    throw new TypeNotPresentException(name, e);
                    // Should never happen because we are creating an array of an existing class.
                }
            }
        }
        return element;
    }

    /**
     * Returns the upper bounds of the parameterized type of the given property.
     * If the property does not have a parameterized type, returns {@code null}.
     *
     * <p>This method is typically used for fetching the type of elements in a collection.
     * We do not provide a method working from a {@link Class} instance because of the way
     * parameterized types are implemented in Java (by erasure).</p>
     *
     * <b>Examples:</b> When invoking this method for a field of the type below:
     * <ul>
     *   <li>{@code Set<Number>} returns {@code Number.class}.</li>
     *
     *   <li>{@code Set<? extends Number>} returns {@code Number.class} as well, since that
     *       collection can not (in theory) contain instances of super-classes; {@code Number}
     *       is the <cite>upper bound</cite>.</li>
     *
     *   <li>{@code Set<? super Number>} returns {@code Object.class}, because that collection
     *       is allowed to contain such elements.</li>
     *
     *   <li>{@code Set} returns {@code null} because that collection is un-parameterized.</li>
     * </ul>
     *
     * @param  field The field for which to obtain the parameterized type.
     * @return The upper bound of parameterized type, or {@code null} if the given field
     *         is not of a parameterized type.
     */
    public static Class<?> boundOfParameterizedProperty(final Field field) {
        return getActualTypeArgument(field.getGenericType());
    }

    /**
     * If the given method is a getter or a setter for a parameterized property, returns the
     * upper bounds of the parameterized type. Otherwise returns {@code null}. This method
     * provides the same semantic than {@link #boundOfParameterizedProperty(Field)}, but
     * works on a getter or setter method rather then the field. See the javadoc of above
     * method for more details.
     *
     * <p>This method is typically used for fetching the type of elements in a collection.
     * We do not provide a method working from a {@link Class} instance because of the way
     * parameterized types are implemented in Java (by erasure).</p>
     *
     * @param  method The getter or setter method for which to obtain the parameterized type.
     * @return The upper bound of parameterized type, or {@code null} if the given method
     *         do not operate on an object of a parameterized type.
     */
    public static Class<?> boundOfParameterizedProperty(final Method method) {
        Class<?> c = getActualTypeArgument(method.getGenericReturnType());
        if (c == null) {
            final Type[] parameters = method.getGenericParameterTypes();
            if (parameters != null && parameters.length == 1) {
                c = getActualTypeArgument(parameters[0]);
            }
        }
        return c;
    }

    /**
     * Delegates to {@link ParameterizedType#getActualTypeArguments} and returns the result as a
     * {@link Class}, provided that every objects are of the expected classes and the result was
     * an array of length 1 (so there is no ambiguity). Otherwise returns {@code null}.
     */
    private static Class<?> getActualTypeArgument(Type type) {
        if (type instanceof ParameterizedType) {
            Type[] p = ((ParameterizedType) type).getActualTypeArguments();
            while (p != null && p.length == 1) {
                type = p[0];
                if (type instanceof WildcardType) {
                    p = ((WildcardType) type).getUpperBounds();
                    continue;
                }
                /*
                 * At this point we are not going to continue the loop anymore.
                 * Check if we have an array, then check the (component) class.
                 */
                if (type instanceof ParameterizedType) {
                    // Example: replace ParameterDescriptor<?> by ParameterDescriptor
                    // before we test if (type instanceof Class<?>).
                    type = ((ParameterizedType) type).getRawType();
                }
                int dimension = 0;
                while (type instanceof GenericArrayType) {
                    type = ((GenericArrayType) type).getGenericComponentType();
                    dimension++;
                }
                if (type instanceof Class<?>) {
                    return changeArrayDimension((Class<?>) type, dimension);
                }
                break; // Unknown type.
            }
        }
        return null;
    }

    /**
     * Returns the class of the specified object, or {@code null} if {@code object} is null.
     * This method is also useful for fetching the class of an object known only by its bound
     * type. As of Java 6, the usual pattern:
     *
     * {@preformat java
     *     Number n = 0;
     *     Class<? extends Number> c = n.getClass();
     * }
     *
     * doesn't seem to work if {@link Number} is replaced by a parameterized type {@code T}.
     *
     * @param  <T> The type of the given object.
     * @param  object The object for which to get the class, or {@code null}.
     * @return The class of the given object, or {@code null} if the given object was null.
     */
    @SuppressWarnings("unchecked")
    @Workaround(library="JDK", version="1.7")
    public static <T> Class<? extends T> getClass(final T object) {
        return (object != null) ? (Class<? extends T>) object.getClass() : null;
    }

    /**
     * Returns the classes of all objects in the given collection. If the given collection
     * contains some null elements, then the returned set will contains a null element as well.
     * The returned set is modifiable and can be freely updated by the caller.
     *
     * <p>Note that interfaces are not included in the returned set.</p>
     *
     * @param  <T> The base type of elements in the given collection.
     * @param  objects The collection of objects.
     * @return The set of classes of all objects in the given collection.
     */
    private static <T> Set<Class<? extends T>> getClasses(final Iterable<? extends T> objects) {
        final Set<Class<? extends T>> types = new LinkedHashSet<Class<? extends T>>();
        for (final T object : objects) {
            types.add(getClass(object));
        }
        return types;
    }

    /**
     * Returns every interfaces implemented, directly or indirectly, by the given class or interface.
     * This is similar to {@link Class#getInterfaces()} except that this method searches recursively
     * in the super-interfaces. For example if the given type is {@link java.util.ArrayList}, then
     * the returned set will contains {@link java.util.List} (which is implemented directly)
     * together with its parent interfaces {@link Collection} and {@link Iterable}.
     *
     * @param  <T>  The compile-time type of the {@code Class} argument.
     * @param  type The class or interface for which to get all implemented interfaces.
     * @return All implemented interfaces (not including the given {@code type} if it was an
     *         interface), or an empty array if none.
     *
     * @see Class#getInterfaces()
     */
    @SuppressWarnings({"unchecked","rawtypes"})                             // Generic array creation.
    public static <T> Class<? super T>[] getAllInterfaces(final Class<T> type) {
        final Set<Class<?>> interfaces = getInterfaceSet(type);
        return (interfaces != null) ? interfaces.toArray(new Class[interfaces.size()]) : EMPTY_ARRAY;
    }

    /**
     * Implementation of {@link #getAllInterfaces(Class)} returning a {@link Set}.
     * The public API exposes the method returning an array instead than a set for
     * the following reasons:
     *
     * <ul>
     *   <li>Consistency with other methods ({@link #getLeafInterfaces(Class, Class)},
     *       {@link Class#getInterfaces()}).</li>
     *   <li>Because arrays in Java are covariant, while the {@code Set} are not.
     *       Consequently callers can cast {@code Class<? super T>[]} to {@code Class<?>[]}
     *       while they can not cast {@code Set<Class<? super T>>} to {@code Set<Class<?>>}.</li>
     * </ul>
     *
     * @param  type The class or interface for which to get all implemented interfaces.
     * @return All implemented interfaces (not including the given {@code type} if it was an
     *         interface), or {@code null} if none. Callers can freely modify the returned set.
     */
    static Set<Class<?>> getInterfaceSet(Class<?> type) {
        Set<Class<?>> interfaces = null;
        while (type != null) {
            interfaces = getInterfaceSet(type, interfaces);
            type = type.getSuperclass();
        }
        return interfaces;
    }

    /**
     * Adds to the given set every interfaces implemented by the given class or interface.
     *
     * @param  type  The type for which to add the interfaces in the given set.
     * @param  addTo The set where to add interfaces, or {@code null} if not yet created.
     * @return The given set (may be {@code null}), or a new set if the given set was null
     *         and at least one interface has been found.
     */
    private static Set<Class<?>> getInterfaceSet(final Class<?> type, Set<Class<?>> addTo) {
        final Class<?>[] interfaces = type.getInterfaces();
        for (final Class<?> candidate : interfaces) {
            if (addTo == null) {
                addTo = new LinkedHashSet<Class<?>>(hashMapCapacity(interfaces.length));
            }
            if (addTo.add(candidate)) {
                getInterfaceSet(candidate, addTo);
            }
        }
        return addTo;
    }

    /**
     * Returns the interfaces implemented by the given class and assignable to the given base
     * interface, or an empty array if none. If more than one interface extends the given base,
     * then the most specialized interfaces are returned. For example if the given class
     * implements both the {@link Set} and {@link Collection} interfaces, then the returned
     * array contains only the {@code Set} interface.
     *
     * <div class="section">Example</div>
     * {@code getLeafInterfaces(ArrayList.class, Collection.class)} returns an array of length 1
     * containing {@code List.class}.
     *
     * @param  <T>  The type of the {@code baseInterface} class argument.
     * @param  type A class for which the implemented interfaces are desired.
     * @param  baseInterface The base type of the interfaces to search.
     * @return The leaf interfaces matching the given criterion, or an empty array if none.
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<? extends T>[] getLeafInterfaces(Class<?> type, final Class<T> baseInterface) {
        int count = 0;
        Class<?>[] types = EMPTY_ARRAY;
        while (type != null) {
            final Class<?>[] candidates = type.getInterfaces();
next:       for (final Class<?> candidate : candidates) {
                if (baseInterface == null || baseInterface.isAssignableFrom(candidate)) {
                    /*
                     * At this point, we have an interface to be included in the returned array.
                     * If a more specialized interface existed before 'candidate', forget the
                     * candidate.
                     */
                    for (int i=0; i<count; i++) {
                        final Class<?> old = types[i];
                        if (candidate.isAssignableFrom(old)) {
                            continue next;                      // A more specialized interface already exists.
                        }
                        if (old.isAssignableFrom(candidate)) {
                            types[i] = candidate;               // This interface specializes a previous interface.
                            continue next;
                        }
                    }
                    if (types == EMPTY_ARRAY) {
                        types = candidates;
                    }
                    if (count >= types.length) {
                        types = Arrays.copyOf(types, types.length + candidates.length);
                    }
                    types[count++] = candidate;
                }
            }
            type = type.getSuperclass();
        }
        return (Class[]) ArraysExt.resize(types, count);
    }

    /**
     * Returns the most specific class implemented by the objects in the given collection.
     * If there is more than one specialized class, returns their {@linkplain #findCommonClass
     * most specific common super class}.
     *
     * <p>This method searches for classes only, not interfaces.</p>
     *
     * @param  objects A collection of objects. May contains duplicated values and null values.
     * @return The most specialized class, or {@code null} if the given collection does not contain
     *         at least one non-null element.
     */
    public static Class<?> findSpecializedClass(final Iterable<?> objects) {
        final Set<Class<?>> types = getClasses(objects);
        types.remove(null);
        /*
         * Removes every classes in the types collection which are assignable from an other
         * class from the same collection. As a result, the collection should contains only
         * leaf classes.
         */
        for (final Iterator<Class<?>> it=types.iterator(); it.hasNext();) {
            final Class<?> candidate = it.next();
            for (final Class<?> type : types) {
                if (candidate != type && candidate.isAssignableFrom(type)) {
                    it.remove();
                    break;
                }
            }
        }
        return common(types);
    }

    /**
     * Returns the most specific class which is a common parent of all the specified classes.
     * This method is not public in order to make sure that it contains only classes, not
     * interfaces, since our implementation is not designed for multi-inheritances.
     *
     * @param  types The collection where to search for a common parent.
     * @return The common parent, or {@code null} if the given collection is empty.
     */
    private static Class<?> common(final Set<Class<?>> types) {
        final Iterator<Class<?>> it = types.iterator();
        if (!it.hasNext()) {
            return null;
        }
        Class<?> type = it.next();
        while (it.hasNext()) {
            type = findCommonClass(type, it.next());
        }
        return type;
    }

    /**
     * Returns the most specific class which {@linkplain Class#isAssignableFrom is assignable from}
     * the type of all given objects. If no element in the given collection has a type assignable
     * from the type of all other elements, then this method searches for a common
     * {@linkplain Class#getSuperclass super class}.
     *
     * <p>This method searches for classes only, not interfaces.</p>
     *
     * @param  objects A collection of objects. May contains duplicated values and null values.
     * @return The most specific class common to all supplied objects, or {@code null} if the
     *         given collection does not contain at least one non-null element.
     */
    public static Class<?> findCommonClass(final Iterable<?> objects) {
        final Set<Class<?>> types = getClasses(objects);
        types.remove(null);
        return common(types);
    }

    /**
     * Returns the most specific class which {@linkplain Class#isAssignableFrom is assignable from}
     * the given classes or a parent of those classes. This method returns either {@code c1},
     * {@code c2} or a common parent of {@code c1} and {@code c2}.
     *
     * <p>This method considers classes only, not the interfaces.</p>
     *
     * @param  c1 The first class, or {@code null}.
     * @param  c2 The second class, or {@code null}.
     * @return The most specific class common to the supplied classes, or {@code null}
     *         if both {@code c1} and {@code c2} are null.
     */
    public static Class<?> findCommonClass(Class<?> c1, Class<?> c2) {
        if (c1 == null) return c2;
        if (c2 == null) return c1;
        do {
            if (c1.isAssignableFrom(c2)) {
                return c1;
            }
            if (c2.isAssignableFrom(c1)) {
                return c2;
            }
            c1 = c1.getSuperclass();
            c2 = c2.getSuperclass();
        } while (c1 != null && c2 != null);
        return Object.class;
    }

    /**
     * Returns the interfaces which are implemented by the two given classes. The returned set
     * does not include the parent interfaces. For example if the two given objects implement the
     * {@link Collection} interface, then the returned set will contains the {@code Collection}
     * type but not the {@link Iterable} type, since it is implied by the collection type.
     *
     * @param  c1 The first class.
     * @param  c2 The second class.
     * @return The interfaces common to both classes, or an empty set if none.
     *         Callers can freely modify the returned set.
     */
    public static Set<Class<?>> findCommonInterfaces(final Class<?> c1, final Class<?> c2) {
        final Set<Class<?>> interfaces = getInterfaceSet(c1);
        final Set<Class<?>> buffer     = getInterfaceSet(c2);               // To be recycled.
        if (interfaces == null || buffer == null) {
            return Collections.emptySet();
        }
        interfaces.retainAll(buffer);
        for (Iterator<Class<?>> it=interfaces.iterator(); it.hasNext();) {
            final Class<?> candidate = it.next();
            buffer.clear();     // Safe because the buffer can not be Collections.EMPTY_SET at this point.
            getInterfaceSet(candidate, buffer);
            if (interfaces.removeAll(buffer)) {
                it = interfaces.iterator();
            }
        }
        return interfaces;
    }

    /**
     * Returns {@code true} if the two specified objects implements exactly the same set
     * of interfaces. Only interfaces assignable to {@code baseInterface} are compared.
     * Declaration order doesn't matter.
     *
     * <div class="note"><b>Example:</b>
     * in ISO 19111, different interfaces exist for different coordinate system (CS) geometries
     * ({@code CartesianCS}, {@code PolarCS}, etc.). One can check if two implementations have
     * the same geometry with the following code:
     *
     * {@preformat java
     *     if (implementSameInterfaces(cs1, cs2, CoordinateSystem.class)) {
     *         // The two Coordinate System are of the same kind.
     *     }
     * }
     * </div>
     *
     * @param object1 The first object to check for interfaces.
     * @param object2 The second object to check for interfaces.
     * @param baseInterface The parent of all interfaces to check.
     * @return {@code true} if both objects implement the same set of interfaces,
     *         considering only sub-interfaces of {@code baseInterface}.
     */
    public static boolean implementSameInterfaces(final Class<?> object1, final Class<?> object2, final Class<?> baseInterface) {
        if (object1 == object2) {
            return true;
        }
        if (object1 == null || object2 == null) {
            return false;
        }
        final Class<?>[] c1 = getLeafInterfaces(object1, baseInterface);
        final Class<?>[] c2 = getLeafInterfaces(object2, baseInterface);
        /*
         * For each interface in the 'c1' array, check if
         * this interface exists also in the 'c2' array.
         */
        int n = c2.length;
cmp:    for (final Class<?> c : c1) {
            for (int j=n; --j>=0;) {
                if (c == c2[j]) {
                    System.arraycopy(c2, j+1, c2, j, --n-j);
                    continue cmp;
                }
            }
            return false;                       // Interface not found in 'c2'.
        }
        return n == 0;                          // If n>0, at least one interface was not found in 'c1'.
    }

    /**
     * Returns the name of the given class without package name, but including the names of enclosing
     * classes if any. This method is similar to the {@link Class#getSimpleName()} method, except that
     * if the given class is an inner class, then the returned value is prefixed with the outer class
     * name. An other difference is that if the given class is local or anonymous, then this method
     * returns the name of the parent class.
     *
     * <p>The following table compares the various kind of names for some examples:</p>
     *
     * <table class="sis">
     *   <caption>Class name comparisons</caption>
     *   <tr>
     *     <th>Class</th>
     *     <th>{@code getName()}</th>
     *     <th>{@code getSimpleName()}</th>
     *     <th>{@code getCanonicalName()}</th>
     *     <th>{@code getShortName()}</th>
     *   </tr>
     *   <tr>
     *     <td>{@link String}</td>
     *     <td>{@code "java.lang.String"}</td>
     *     <td>{@code "String"}</td>
     *     <td>{@code "java.lang.String"}</td>
     *     <td>{@code "String"}</td>
     *   </tr>
     *   <tr>
     *     <td>{@code double[]}</td>
     *     <td>{@code "[D"}</td>
     *     <td>{@code "double[]"}</td>
     *     <td>{@code "double[]"}</td>
     *     <td>{@code "double[]"}</td>
     *   </tr>
     *   <tr>
     *     <td>{@link java.awt.geom.Point2D.Double}</td>
     *     <td>{@code "java.awt.geom.Point2D$Double"}</td>
     *     <td>{@code "Double"}</td>
     *     <td>{@code "java.awt.geom.Point2D.Double"}</td>
     *     <td>{@code "Point2D.Double"}</td>
     *   </tr>
     *   <tr>
     *     <td>Anonymous {@link Comparable}</td>
     *     <td>{@code "com.mycompany.myclass$1"}</td>
     *     <td>{@code ""}</td>
     *     <td>{@code null}</td>
     *     <td>{@code "Object"}</td>
     *   </tr>
     * </table>
     *
     * @param  classe The object class (may be {@code null}).
     * @return The simple name with outer class name (if any) of the first non-anonymous
     *         class in the hierarchy, or {@code "<*>"} if the given class is null.
     *
     * @see #getShortClassName(Object)
     * @see Class#getSimpleName()
     */
    public static String getShortName(Class<?> classe) {
        if (classe == null) {
            return "<*>";
        }
        while (classe.isAnonymousClass()) {
            classe = classe.getSuperclass();
        }
        String name = classe.getSimpleName();
        final Class<?> enclosing = classe.getEnclosingClass();
        if (enclosing != null) {
            name = getShortName(enclosing) + '.' + name;
        }
        return name;
    }

    /**
     * Returns the class name of the given object without package name, but including the enclosing class names
     * if any. Invoking this method is equivalent to invoking {@code getShortName(object.getClass())} except for
     * {@code null} value. See {@link #getShortName(Class)} for more information on the class name returned by
     * this method.
     *
     * @param  object The object (may be {@code null}).
     * @return The simple class name with outer class name (if any) of the first non-anonymous
     *         class in the hierarchy, or {@code "<*>"} if the given object is null.
     *
     * @see #getShortName(Class)
     */
    public static String getShortClassName(final Object object) {
        return getShortName(getClass(object));
    }

    /**
     * Returns {@code true} if the given type is assignable to one of the given allowed types.
     * More specifically, if at least one {@code allowedTypes[i]} element exists for which
     * <code>allowedTypes[i].{@linkplain Class#isAssignableFrom(Class) isAssignableFrom}(type)</code>
     * returns {@code true}, then this method returns {@code true}.
     *
     * <p>Special cases:</p>
     * <ul>
     *   <li>If {@code type} is null, then this method returns {@code false}.</li>
     *   <li>If {@code allowedTypes} is null, then this method returns {@code true}.
     *       This is to be interpreted as "no restriction on the allowed types".</li>
     *   <li>Any null element in the {@code allowedTypes} array are silently ignored.</li>
     * </ul>
     *
     * @param  type The type to be tested, or {@code null}.
     * @param  allowedTypes The allowed types.
     * @return {@code true} if the given type is assignable to one of the allowed types.
     */
    public static boolean isAssignableToAny(final Class<?> type, final Class<?>... allowedTypes) {
        if (type != null) {
            if (allowedTypes == null) {
                return true;
            }
            for (final Class<?> candidate : allowedTypes) {
                if (candidate != null && candidate.isAssignableFrom(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the given method may possibly be the getter method for a property.
     * This method implements the algorithm used by SIS in order to identify getter methods in
     * {@linkplain org.opengis.metadata metadata} interfaces. We do not rely on naming convention
     * (method names starting with "{@code get}" or "{@code is}" prefixes) because not every methods
     * follow such convention (e.g. {@link org.opengis.metadata.quality.ConformanceResult#pass()}).
     *
     * <p>The current implementation returns {@code true} if the given method meets all the
     * following conditions. Note that a {@code true} value is not a guaranteed that the given
     * method is really a getter. The caller is encouraged to perform additional checks if
     * possible.</p>
     *
     * <ul>
     *   <li>The method does no expect any argument.</li>
     *   <li>The method returns a value (anything except {@code void}).</li>
     *   <li>The method name is not {@link Object#clone() clone}, {@link Object#getClass() getClass},
     *       {@link Object#hashCode() hashCode}, {@link Object#toString() toString} or
     *       {@link org.opengis.referencing.IdentifiedObject#toWKT() toWKT}.</li>
     *   <li>The method is not {@linkplain Method#isSynthetic() synthetic}.</li>
     * </ul>
     *
     * <p>Those conditions may be updated in any future SIS version.</p>
     *
     * @param  method The method to inspect.
     * @return {@code true} if the given method may possibly be a non-deprecated getter method.
     */
    public static boolean isPossibleGetter(final Method method) {
        return method.getReturnType() != Void.TYPE &&
               method.getParameterTypes().length == 0 &&
              !method.isSynthetic() &&
              !ArraysExt.contains(EXCLUDES, method.getName());
    }
}
