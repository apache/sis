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

import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.lang.reflect.Type;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Modifier;
import org.opengis.annotation.UML;
import org.apache.sis.pending.jdk.JDK19;


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
 * @version 1.6
 * @since   0.3
 */
public final class Classes {
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
     * a Java object, or an array of the above. If the given {@code change} is positive, then the
     * array dimension will be increased by that amount. For example, a change of +1 dimension will
     * change an {@code int} class into {@code int[]}, and a {@code String[]} class into {@code String[][]}.
     * A change of +2 dimensions is like applying two times a change of +1 dimension.
     *
     * <p>The change of dimension can also be negative. For example, a change of -1 dimension will
     * change a {@code String[]} class into a {@code String}. More specifically:</p>
     *
     * <ul>
     *   <li>If the given {@code element} is null, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given {@code change} is 0, then the given {@code element} is returned unchanged.</li>
     *   <li>Otherwise if the given {@code change} is negative, then {@link Class#getComponentType()} is invoked
     *       {@code abs(change)} times. The result is a {@code null} value if {@code abs(change)} is greater than
     *       the array dimension.</li>
     *   <li>Otherwise if {@code element} is {@link Void#TYPE}, then this method returns {@code Void.TYPE}
     *       since arrays of {@code void} do not exist.</li>
     *   <li>Otherwise this method returns a class that represents an array of the given class augmented by
     *       the given number of dimensions.</li>
     * </ul>
     *
     * @param  element  the type of elements in the array.
     * @param  change   the change of dimension, as a negative or positive number.
     * @return the type of an array of the given element type augmented by the given
     *         number of dimensions (which may be negative), or {@code null}.
     */
    public static Class<?> changeArrayDimension(Class<?> element, int change) {
        if (change != 0 && element != null) {
            if (change < 0) {
                do element = element.getComponentType();
                while (element!=null && ++change != 0);
            } else if (element != Void.TYPE) {
                // TODO: use Class.arrayType() with JDK12.
                final StringBuilder buffer = new StringBuilder();
                do buffer.insert(0, '[');
                while (--change != 0);
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
     * If the property has more than one parameterized type, then the parameter
     * examined by this method depends on the property type:
     * <ul>
     *   <li>If {@link Map}, then this method returns the type of keys in map entries.</li>
     *   <li>For all other types, this method expects exactly one parameterized type
     *       for avoiding ambiguity. If this is not the case, {@code null} is returned.</li>
     * </ul>
     *
     * This method is used for fetching the type of elements in a collection.
     * This information cannot be obtained from a {@link Class} instance
     * because of the way parameterized types are implemented in Java (by erasure).
     *
     * <h4>Examples</h4>
     * When invoking this method for a field of the following types:
     * <ul>
     *   <li>{@code Map<String,Number>}: returns {@code String.class}, the type of keys.</li>
     *   <li>{@code Set<Number>}: returns {@code Number.class}.</li>
     *   <li>{@code Set<? extends Number>}: returns {@code Number.class} as well,
     *       because that collection cannot contain instances of super-classes.
     *       {@code Number} is the <i>upper bound</i>.</li>
     *   <li>{@code Set<? super Number>}: returns {@code Object.class},
     *       because that collection is allowed to contain such elements.</li>
     *   <li>{@code Set}: returns {@code null} because that collection is declared with raw type.</li>
     *   <li>{@code Long}: returns {@code null} because that type is not parameterized.</li>
     * </ul>
     *
     * @param  field  the field for which to obtain the parameterized type.
     * @return the upper bound of parameterized type, or {@code null} if the given field
     *         is not of a parameterized type.
     *
     * @see #isParameterizedProperty(Class)
     */
    public static Class<?> boundOfParameterizedProperty(final Field field) {
        return getActualTypeArgument(field.getGenericType());
    }

    /**
     * If the given method is a getter or a setter for a parameterized property,
     * returns the upper bounds of the parameterized type.
     * Otherwise returns {@code null}.
     * This method provides the same semantic as {@link #boundOfParameterizedProperty(Field)},
     * but works on a getter or setter method rather than a field.
     * See {@link #boundOfParameterizedProperty(Field)} javadoc for details.
     *
     * <p>This method is used for fetching the type of elements in a collection.
     * This information cannot be obtained from a {@link Class} instance
     * because of the way parameterized types are implemented in Java (by erasure).</p>
     *
     * @param  method  the getter or setter method for which to obtain the parameterized type.
     * @return the upper bound of parameterized type, or {@code null} if the given method
     *         is not a getter or setter for a property of a parameterized type.
     *
     * @see #isParameterizedProperty(Class)
     */
    public static Class<?> boundOfParameterizedProperty(final Method method) {
        final Type[] parameters = method.getGenericParameterTypes();
        final Type type;
        switch (parameters.length) {
            case 0:  type = method.getGenericReturnType(); break;
            case 1:  type = parameters[0]; break;
            default: return null;
        }
        return getActualTypeArgument(type);
    }

    /**
     * Returns a single bound declared in a parameterized class or a parameterized method.
     * The {@code typeOrMethod} argument is usually a {@link Class} for a collection type.
     * If the given argument is a non-parameterized class, then this method searches for
     * the first parameterized super-class (see example below).
     * If no parameterized declaration is found, then this method returns {@code null}.
     * If the declaration has more than one parameterized type, then this method applies
     * the same heuristic rule as {@link #boundOfParameterizedProperty(Field)}
     * (see the javadoc of that method for details).
     *
     * <h4>Examples</h4>
     * When invoking this method with the following {@link Class} argument values:
     * <ul>
     *   <li>{@code List.class}: returns {@code Object.class} because {@link java.util.List} is declared as
     *       {@code List<E>} (implicitly {@code <E extends Object>}).</li>
     *   <li>{@code Map.class}: returns {@code Object.class} because {@link java.util.Map} is declared as
     *       {@code Map<K,V>} and, as an heuristic rule, we return the key type of map entry.</li>
     *   <li>{@code PrinterStateReasons.class}: returns {@code PrinterStateReason.class} because
     *       {@link javax.print.attribute.standard.PrinterStateReasons} is not parameterized but extends
     *       {@code HashMap<PrinterStateReason,Severity>}.</li>
     *   <li>{@code Long.class}: returns {@code null} because that type is not parameterized.</li>
     * </ul>
     *
     * This method is used as a fallback when {@code boundOfParameterizedProperty(…)} cannot be used.
     *
     * @param  typeOrMethod  the {@link Class} or {@link Method} from which to get the bounds of its parameter.
     * @return the upper bound of parameterized class or method, or {@code null} if this method cannot identify
     *         a single parameterized type to return.
     *
     * @see #boundOfParameterizedProperty(Field)
     * @see #boundOfParameterizedProperty(Method)
     *
     * @since 1.3
     */
    public static Class<?> boundOfParameterizedDeclaration(final GenericDeclaration typeOrMethod) {
        final TypeVariable<?>[] parameters = typeOrMethod.getTypeParameters();
        final int i = chooseSingleType(typeOrMethod, parameters.length);
        if (i >= 0) {
            Class<?> bounds = null;
            for (final Type p : parameters[i].getBounds()) {
                if (p instanceof Class<?>) {
                    bounds = findCommonClass(bounds, (Class<?>) p);
                }
            }
            return bounds;
        }
        return getActualTypeArgument(typeOrMethod);
    }

    /**
     * Returns the type argument of the given type or the first parameterized parent type.
     * For example if the given type is {@code List<String>}, then this method returns {@code String.class}.
     * This method expects a fixed number of parameterized types (currently 2 if the given type is {@code Map}
     * and 1 for all other types), otherwise it returns {@code null}.
     *
     * @see ParameterizedType#getActualTypeArguments()
     */
    private static Class<?> getActualTypeArgument(Object typeOrMethod) {
        while (typeOrMethod instanceof Class<?>) {
            typeOrMethod = ((Class<?>) typeOrMethod).getGenericSuperclass();
        }
        if (typeOrMethod instanceof ParameterizedType) {
            final ParameterizedType p = (ParameterizedType) typeOrMethod;
            final Type[] parameters = p.getActualTypeArguments();
            final int i = chooseSingleType(p.getRawType(), parameters.length);
            if (i >= 0) {
                Type type = parameters[i];
                while (type instanceof WildcardType) {
                    final Type[] bounds = ((WildcardType) type).getUpperBounds();
                    if (bounds.length != 1) return null;
                    type = bounds[0];
                }
                /*
                 * If we have an array, unroll it until we get the class of array components.
                 * The array will be reconstructed at the end of this method, but as a class
                 * instead of as a generic type (i.e. we convert Type[][]… to Class[][]…).
                 */
                int dimension = 0;
                while (type instanceof GenericArrayType) {
                    type = ((GenericArrayType) type).getGenericComponentType();
                    dimension++;
                }
                /*
                 * Then replace (for example) `ParameterDescriptor<?>` by `ParameterDescriptor`
                 * in order to get an instance of `Class` instead of other kind of `Type`.
                 */
                if (type instanceof ParameterizedType) {
                    type = ((ParameterizedType) type).getRawType();
                }
                if (type instanceof Class<?>) {
                    return changeArrayDimension((Class<?>) type, dimension);
                }
            }
        }
        return null;
    }

    /**
     * Chooses (using heuristic rules) a single element in an array of type arguments.
     * The given raw type should be a {@link Class} instance when possible, usually a collection type.
     * The given count shall be the number of parameters, for example 2 in {@code Map<String,Integer>}.
     *
     * @param  rawType  the parameterized class, as a {@link Class} instance if possible.
     * @param  count    length of the array of type parameters in which to select a single type.
     * @return index of the parameter to select in an array of length {@code count}, or -1 if none.
     */
    @SuppressWarnings("fallthrough")
    private static int chooseSingleType(final Object rawType, final int count) {
        switch (count) {
            case 2: if (!(rawType instanceof Class<?>) && Map.class.isAssignableFrom((Class<?>) rawType)) break;
                    // Else fallthrough.
            case 1: return 0;
        }
        return -1;
    }

    /**
     * Returns the class of the specified object, or {@code null} if {@code object} is null.
     * This method is also useful for fetching the class of an object known only by its bound
     * type. As of Java 6, the usual pattern:
     *
     * {@snippet lang="java" :
     *     Number n = 0;
     *     Class<? extends Number> c = n.getClass();
     *     }
     *
     * doesn't seem to work if {@link Number} is replaced by a parameterized type {@code T}.
     *
     * @param  <T>     the type of the given object.
     * @param  object  the object for which to get the class, or {@code null}.
     * @return the class of the given object, or {@code null} if the given object was null.
     */
    @SuppressWarnings("unchecked")
    @Workaround(library="JDK", version="1.7")
    public static <T> Class<? extends T> getClass(final T object) {
        return (object != null) ? (Class<? extends T>) object.getClass() : null;
    }

    /**
     * Returns the classes of all objects in the given collection. If the given collection
     * contains some null elements, then the returned set will contain a null element as well.
     * The returned set is modifiable and can be freely updated by the caller.
     *
     * <p>Note that interfaces are not included in the returned set.</p>
     *
     * @param  <T>      the base type of elements in the given collection.
     * @param  objects  the collection of objects.
     * @return the set of classes of all objects in the given collection.
     *
     * @deprecated To be removed after removal of public deprecated methods.
     */
    @Deprecated(since = "1.6", forRemoval = true)
    private static <T> Set<Class<? extends T>> getClasses(final Iterable<? extends T> objects) {
        final Set<Class<? extends T>> types = new LinkedHashSet<>();
        for (final T object : objects) {
            types.add(getClass(object));
        }
        return types;
    }

    /**
     * Returns the first type or super-type (including interface) considered "standard" in Apache SIS sense.
     * This method applies the following heuristic rules, in that order:
     *
     * <ul>
     *   <li>If the given type implements at least one interface having the {@link UML} annotation,
     *       then the first annotated interface is returned.</li>
     *   <li>Otherwise the first public class or parent class is returned.</li>
     * </ul>
     *
     * Those heuristic rules may be adjusted in any future Apache SIS version.
     *
     * @param  <T>   the compile-time type argument.
     * @param  type  the type for which to get the standard interface or class. May be {@code null}.
     * @return a standard interface implemented by {@code type}, or otherwise the most specific public class.
     *         Is {@code null} if the given {@code type} argument was null.
     *
     * @since 1.0
     */
    public static <T> Class<? super T> getStandardType(final Class<T> type) {
        for (final Class<? super T> candidate : getAllInterfaces(type)) {
            if (candidate.isAnnotationPresent(UML.class)) {
                return candidate;
            }
        }
        for (Class<? super T> candidate = type; candidate != null; candidate = candidate.getSuperclass()) {
            if (isPublic(candidate)) {
                return candidate;
            }
        }
        return type;
    }

    /**
     * Returns every interfaces implemented, directly or indirectly, by the given class or interface.
     * This is similar to {@link Class#getInterfaces()} except that this method searches recursively
     * in the super-interfaces. For example if the given type is {@link java.util.ArrayList}, then
     * the returned set will contain {@link java.util.List} (which is implemented directly)
     * together with its parent interfaces {@link Collection} and {@link Iterable}.
     *
     * <h4>Elements ordering</h4>
     * All interfaces implemented directly by the given type are first and in the order they are declared
     * in the {@code implements} or {@code extends} clause. Parent interfaces are next.
     *
     * @param  <T>   the compile-time type of the {@code Class} argument.
     * @param  type  the class or interface for which to get all implemented interfaces.
     * @return all implemented interfaces (not including the given {@code type} if it was an interface),
     *         or an empty array if none.
     *
     * @see Class#getInterfaces()
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public static <T> Class<? super T>[] getAllInterfaces(final Class<T> type) {
        final Set<Class<?>> interfaces = getInterfaceSet(type);
        return (interfaces != null) ? interfaces.toArray(Class[]::new) : EMPTY_ARRAY;
    }

    /**
     * Implementation of {@link #getAllInterfaces(Class)} returning a {@link Set}.
     * The public API exposes the method returning an array instead of a set for
     * the following reasons:
     *
     * <ul>
     *   <li>Consistency with other methods ({@link #getLeafInterfaces(Class, Class)},
     *       {@link Class#getInterfaces()}).</li>
     *   <li>Because arrays in Java are covariant, while the {@code Set} are not.
     *       Consequently, callers can cast {@code Class<? super T>[]} to {@code Class<?>[]}
     *       while they cannot cast {@code Set<Class<? super T>>} to {@code Set<Class<?>>}.</li>
     * </ul>
     *
     * See {@link #getInterfaceSet(Class, Set)} javadoc for a note about elements order.
     *
     * @param  type  the class or interface for which to get all implemented interfaces.
     * @return all implemented interfaces (not including the given {@code type} if it was an interface),
     *         or {@code null} if none. Callers can freely modify the returned set.
     */
    private static Set<Class<?>> getInterfaceSet(Class<?> type) {
        Set<Class<?>> interfaces = null;
        while (type != null) {
            interfaces = getInterfaceSet(type, interfaces);
            type = type.getSuperclass();
        }
        return interfaces;
    }

    /**
     * Adds to the given set every interfaces implemented by the given class or interface.
     * This method invokes itself recursively for adding parent interfaces.
     * The given type is <em>not</em> added to the set.
     *
     * <h4>Elements ordering</h4>
     * All interfaces directly implemented by the given type are added first. Then parent interfaces
     * are added recursively. The goal is to increase the chances to have the most specific types first.
     * Example: suppose a class implementing two interfaces: {@code A extends C} and {@code B extends C}.
     * If the parents of A were added immediately after A, we would get {A, C, B} order.
     * But if instead we add all directly implemented interfaces before to add parents,
     * then we get {A, B, C} order, which is better.
     *
     * @param  type   the type for which to add the interfaces in the given set.
     * @param  addTo  the set where to add interfaces, or {@code null} if not yet created.
     * @return the given set (may be {@code null}), or a new set if the given set was null
     *         and at least one interface has been found.
     */
    private static Set<Class<?>> getInterfaceSet(final Class<?> type, Set<Class<?>> addTo) {
        final Class<?>[] interfaces = type.getInterfaces();
        for (int i=0; i<interfaces.length; i++) {
            if (addTo == null) {
                addTo = JDK19.newLinkedHashSet(interfaces.length);
            }
            if (!addTo.add(interfaces[i])) {
                interfaces[i] = null;           // Remember that this interface is already present.
            }
        }
        for (final Class<?> candidate : interfaces) {
            if (candidate != null) {
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
     * <h4>Example</h4>
     * {@code getLeafInterfaces(ArrayList.class, Collection.class)} returns an array of length 1
     * containing {@code List.class}.
     *
     * @param  <T>   the type of the {@code baseInterface} class argument.
     * @param  type  a class for which the implemented interfaces are desired, or {@code null}.
     * @param  baseInterface  the base type of the interfaces to search.
     * @return the leaf interfaces matching the given criterion, or an empty array if none.
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
     * Returns the most specific class of the objects in the given collection.
     * If there is more than one specialized class,
     * returns their {@linkplain #findCommonClass most specific common super class}.
     *
     * <p>This method searches for classes only, not interfaces.</p>
     *
     * @param  objects  a collection of objects. May contains duplicated values and null values.
     * @return the most specialized class, or {@code null} if the given collection does not contain
     *         at least one non-null element.
     *
     * @deprecated This method is confusing as it works on instances instead of classes.
     */
    @Deprecated(since = "1.6", forRemoval = true)
    public static Class<?> findSpecializedClass(final Iterable<?> objects) {
        final Set<Class<?>> types = getClasses(objects);
        types.remove(null);
        /*
         * Removes every classes in the types collection which are assignable from another
         * class from the same collection. As a result, the collection should contain only
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
     * @param  types  the collection where to search for a common parent.
     * @return the common parent, or {@code null} if the given collection is empty.
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
     * @param  objects  a collection of objects. May contains duplicated values and null values.
     * @return the most specific class common to all supplied objects, or {@code null} if the
     *         given collection does not contain at least one non-null element.
     *
     * @deprecated This method is confusing as it works on instances while {@link #findCommonClass(Class, Class)}
     *             works on classes.
     */
    @Deprecated(since = "1.6", forRemoval = true)
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
     * @param  c1  the first class, or {@code null}.
     * @param  c2  the second class, or {@code null}.
     * @return the most specific class common to the supplied classes, or {@code null}
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
     * {@link Collection} interface, then the returned set will contain the {@code Collection}
     * type but not the {@link Iterable} type, since it is implied by the collection type.
     *
     * @param  c1  the first class.
     * @param  c2  the second class.
     * @return the interfaces common to both classes, or an empty set if none.
     *         Callers can freely modify the returned set.
     */
    public static Set<Class<?>> findCommonInterfaces(final Class<?> c1, final Class<?> c2) {
        final Set<Class<?>> interfaces = getInterfaceSet(c1);
        final Set<Class<?>> buffer     = getInterfaceSet(c2);               // To be recycled.
        if (interfaces == null || buffer == null) {
            return Collections.emptySet();
        }
        interfaces.retainAll(buffer);
        for (Iterator<Class<?>> it = interfaces.iterator(); it.hasNext();) {
            final Class<?> candidate = it.next();
            buffer.clear();     // Safe because the buffer cannot be Collections.EMPTY_SET at this point.
            getInterfaceSet(candidate, buffer);
            if (interfaces.removeAll(buffer)) {
                it = interfaces.iterator();
            }
        }
        return interfaces;
    }

    /**
     * Returns {@code true} if the two specified objects implements exactly the same set of interfaces.
     * Only interfaces assignable to {@code baseInterface} are compared.
     * Declaration order does not matter.
     *
     * <h4>Example</h4>
     * in ISO 19111, different interfaces exist for different coordinate system (CS) geometries
     * ({@code CartesianCS}, {@code PolarCS}, etc.). One can check if two implementations have
     * the same geometry with the following code:
     *
     * {@snippet lang="java" :
     *     if (implementSameInterfaces(cs1, cs2, CoordinateSystem.class)) {
     *         // The two Coordinate Systems are of the same kind.
     *     }
     *     }
     *
     * @param  object1  the first object to check for interfaces.
     * @param  object2  the second object to check for interfaces.
     * @param  baseInterface  the parent of all interfaces to check.
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
     * name. Another difference is that if the given class is local or anonymous, then this method
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
     *   </tr><tr>
     *     <td>{@link String}</td>
     *     <td>{@code "java.lang.String"}</td>
     *     <td>{@code "String"}</td>
     *     <td>{@code "java.lang.String"}</td>
     *     <td>{@code "String"}</td>
     *   </tr><tr>
     *     <td>{@code double[]}</td>
     *     <td>{@code "[D"}</td>
     *     <td>{@code "double[]"}</td>
     *     <td>{@code "double[]"}</td>
     *     <td>{@code "double[]"}</td>
     *   </tr><tr>
     *     <td>{@link java.awt.geom.Point2D.Double}</td>
     *     <td>{@code "java.awt.geom.Point2D$Double"}</td>
     *     <td>{@code "Double"}</td>
     *     <td>{@code "java.awt.geom.Point2D.Double"}</td>
     *     <td>{@code "Point2D.Double"}</td>
     *   </tr><tr>
     *     <td>Anonymous {@link Comparable}</td>
     *     <td>{@code "com.mycompany.myclass$1"}</td>
     *     <td>{@code ""}</td>
     *     <td>{@code null}</td>
     *     <td>{@code "Object"}</td>
     *   </tr>
     * </table>
     *
     * @param  classe  the object class (may be {@code null}).
     * @return the simple name with outer class name (if any) of the first non-anonymous
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
     * @param  object  the object (may be {@code null}).
     * @return the simple class name with outer class name (if any) of the first non-anonymous
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
     * @param  type  the type to be tested, or {@code null}.
     * @param  allowedTypes  the allowed types.
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
     * @param  method  the method to inspect.
     * @return {@code true} if the given method may possibly be a non-deprecated getter method.
     */
    public static boolean isPossibleGetter(final Method method) {
        return method.getReturnType() != Void.TYPE &&
               method.getParameterCount() == 0 &&
              !method.isSynthetic() &&
              !ArraysExt.contains(EXCLUDES, method.getName());
    }

    /**
     * Returns whether the actual type of a property is the parameterized type according SIS.
     * The given {@code type} argument should be the type of a field or the return type of a
     * {@linkplain #isPossibleGetter(Method) getter method}. If this method returns {@code true},
     * then a {@code boundOfParameterizedProperty(…)} method needs to be invoked in order to get
     * the actual property type.
     *
     * <p>The current implementation tests only if the given type is {@link Optional}.
     * More types may be added in future Apache SIS versions, depending on API evolutions.
     * Note that collections are intentionally <em>not</em> recognized by this method,
     * because they usually need to be handled in a special way by the caller.</p>
     *
     * @param  type  the field type or getter method return type to test.
     * @return whether a {@code boundOfParameterizedProperty(…)} method need to be invoked
     *         for getting the actual property type.
     *
     * @see #boundOfParameterizedProperty(Field)
     * @see #boundOfParameterizedProperty(Method)
     *
     * @since 1.5
     */
    public static boolean isParameterizedProperty(final Class<?> type) {
        return type == Optional.class;
    }

    /**
     * Returns {@code true} if the given class is non-null, public and exported.
     *
     * @param  type  the class to test, or {@code null}.
     * @return whether the given class is part of public API.
     *
     * @since 1.5
     */
    public static boolean isPublic(final Class<?> type) {
        return (type != null) && Modifier.isPublic(type.getModifiers())
                && type.getModule().isExported(type.getPackageName());
    }
}
