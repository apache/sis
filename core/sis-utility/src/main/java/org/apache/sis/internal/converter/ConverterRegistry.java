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
package org.apache.sis.internal.converter;

import java.util.Map;
import java.util.LinkedHashMap;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.resources.Errors;


/**
 * A collection of {@link ObjectConverter} instances.
 * A converter from the given <var>source type</var> to the given <var>target type</var> can be
 * obtained by a call to {@link #find(Class, Class)}. If no converter exists for the given
 * source and target types, then this registry searches for a suitable converter accepting a
 * parent class of the given source type, or returning a sub-class of the given target type.
 *
 * <p>New instances of {@code ConverterRegistry} are initially empty. Custom converters must be
 * explicitly {@linkplain #register(ObjectConverter) registered}. However a system-wide registry
 * initialized with default converters is provided by the {@link SystemRegistry#INSTANCE} constant.</p>
 *
 * <div class="section">Note about conversions from interfaces</div>
 * {@code ConverterRegistry} is primarily designed for handling converters from classes to other classes.
 * Handling of interfaces are not prohibited (and actually sometime supported), but their behavior may be
 * more ambiguous than in the case of classes because of multi-inheritance in interface hierarchy.
 *
 * <div class="section">Thread safety</div>
 * This base class is thread-safe. Subclasses shall make sure that any overridden methods remain safe to call
 * from multiple threads.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public class ConverterRegistry {
    /**
     * The map of converters of any kind. For any key of type {@code ClassPair<S,T>},
     * the value shall be of type {@code ObjectConverter<? super S, ? extends T>}.
     * To ensure this constraint, values should be read and written using only the
     * type-safe {@link #get(ClassPair)} and {@link #put(ClassPair, ObjectConverter)}
     * methods.
     *
     * <p>In the special case where the value is actually {@code SystemConverter<S,T>},
     * then the key and the value may be the same instance (in order to save object
     * allocations).</p>
     *
     * <div class="section">Synchronization note</div>
     * Synchronization if performed by {@code synchronized(converters)} statements. We tried
     * {@code ReadWriteLock}, but this is not very convenient because read operations may be
     * followed by write operations at any time if the requested converter is not in the cache.
     * Furthermore profiling has not identified this class as a noticeable contention point.
     */
    private final Map<ClassPair<?,?>, ObjectConverter<?,?>> converters;

    /**
     * {@code true} if this {@code ConverterRegistry} has been initialized.
     *
     * @see #initialize()
     */
    private boolean isInitialized;

    /**
     * Creates an initially empty set of object converters.
     */
    public ConverterRegistry() {
        converters = new LinkedHashMap<ClassPair<?,?>, ObjectConverter<?,?>>();
    }

    /**
     * Invoked when this {@code ConverterRegistry} needs to be initialized. This method
     * is automatically invoked the first time that {@link #register(ObjectConverter)}
     * or {@link #find(Class, Class)} is invoked.
     *
     * <p>The default implementation does nothing. Subclasses can override this method
     * in order to register a default set of converters. For example a subclass could
     * fetch the {@code ObjectConverter} instances from the {@code META-INF/services}
     * directories as below:</p>
     *
     * {@preformat java
     *     ClassLoader loader = getClass().getClassLoader();
     *     for (ObjectConverter<?,?> converter : ServiceLoader.load(ObjectConverter.class, loader)) {
     *         register(converter);
     *     }
     * }
     */
    protected void initialize() {
    }

    /**
     * Removes all converters from this registry and set this {@code ServiceRegistry}
     * state to <cite>uninitialized</cite>. The {@link #initialize()} method will be
     * invoked again when first needed.
     */
    public void clear() {
        synchronized (converters) {
            converters.clear();
            isInitialized = false;
        }
    }

    /**
     * Gets the value from the {@linkplain #converters} map for the given key.
     */
    private <S,T> ObjectConverter<? super S, ? extends T> get(final ClassPair<S,T> key) {
        assert Thread.holdsLock(converters);
        return key.cast(converters.get(key));
    }

    /**
     * Puts the given value in the {@linkplain #converters} map for the given key.
     */
    @SuppressWarnings("unchecked")
    private <S,T> void put(ClassPair<S,T> key, final ObjectConverter<? super S, ? extends T> converter) {
        assert key.getClass() == ClassPair.class; // See SystemConverter.equals(Object)
        assert key.cast(converter) != null : converter;
        assert Thread.holdsLock(converters);
        if (converter instanceof SystemConverter<?,?> &&
            converter.getSourceClass() == key.sourceClass &&
            converter.getTargetClass() == key.targetClass)
        {
            /*
             * Opportunistically share the same instance for the keys and the values, in order
             * to reduce a little bit the amount of objects in the JVM. However we must remove
             * any old value from the map using the old key, otherwise put operation may fail.
             * See SystemConverter.equals(Object) for more explanation.
             */
            converters.remove(key);
            key = (SystemConverter<S,T>) converter;
        }
        converters.put(key, converter);
    }

    /**
     * If {@code existing} or one of its children is equals to the given {@code converter},
     * returns it. Otherwise returns {@code null}.
     *
     * @param  <S> The {@code converter} source class.
     * @param  <T> The {@code converter} target class.
     * @param  converter The converter to replace by an existing converter, if possible.
     * @param  existing Existing converter to test.
     * @return A converter equals to {@code converter}, or {@code null} if none.
     */
    @SuppressWarnings("unchecked")
    private static <S,T> ObjectConverter<S,T> findEquals(ObjectConverter<S,T> converter,
            final ObjectConverter<S, ? extends T> existing)
    {
        if (converter instanceof FallbackConverter<?,?>) {
            final FallbackConverter<S,T> fc = (FallbackConverter<S,T>) converter;
            converter = findEquals(fc, fc.primary);
            if (converter == null) {
                converter = findEquals(fc, fc.fallback);
            }
        } else if (converter.equals(existing)) {
            converter = (ObjectConverter<S,T>) existing;
        } else {
            converter = null;
        }
        return converter;
    }

    /**
     * Returns a converter equals to the given {@code converter}, or {@code null} if none.
     *
     * @param  <S> The {@code converter} source class.
     * @param  <T> The {@code converter} target class.
     * @param  converter The converter to replace by an existing converter, if possible.
     * @return A converter equals to {@code converter}, or {@code null} if none.
     */
    @SuppressWarnings("unchecked")
    final <S,T> ObjectConverter<S,T> findEquals(final SystemConverter<S,T> converter) {
        ObjectConverter<? super S, ? extends T> existing;
        synchronized (converters) {
            existing = get(converter);
        }
        if (existing != null && existing.getSourceClass() == converter.getSourceClass()) {
            return findEquals(converter, (ObjectConverter<S, ? extends T>) existing);
        }
        return null;
    }

    /**
     * Registers a new converter. This method should be invoked only once for a given converter,
     * for example in class static initializer. For example if a {@code Angle} class is defined,
     * the static initializer of that class could register a converter from {@code Angle} to
     * {@code Double}.
     *
     * <p>This method registers the converter for the {@linkplain ObjectConverter#getTargetClass()
     * target class}, some parents of the target class (see below) and every interfaces except
     * {@link Cloneable} which are implemented by the target class and not by the source class.
     * For example a converter producing {@link Double} can be used for clients that just ask
     * for a {@link Number}.</p>
     *
     * <div class="section">Which super-classes of the target class are registered</div>
     * Consider a converter from class {@code S} to class {@code T} where the two classes
     * are related in a hierarchy as below:
     *
     * {@preformat text
     *   C1
     *   └───C2
     *       ├───C3
     *       │   └───S
     *       └───C4
     *           └───T
     * }
     *
     * Invoking this method will register the given converter for all the following cases:
     *
     * <ul>
     *   <li>{@code S} → {@code T}</li>
     *   <li>{@code S} → {@code C4}</li>
     * </ul>
     *
     * No {@code S} → {@code C2} or {@code S} → {@code C1} converter will be registered,
     * because an identity converter would be sufficient for those cases.
     *
     * <div class="section">Which sub-classes of the source class are registered</div>
     * Sub-classes of the source class will be registered on a case-by-case basis when the
     * {@link #find(Class, Class)} is invoked, because we can not know the set of all
     * sub-classes in advance (and would not necessarily want to register all of them anyway).
     *
     * @param <S> The class of source value.
     * @param <T> The class of target (converted) values.
     * @param converter The converter to register.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public <S,T> void register(final ObjectConverter<S,T> converter) {
        ArgumentChecks.ensureNonNull("converter", converter);
        /*
         * If the given converter is a FallbackConverter (maybe obtained from an other
         * ConverterRegistry), unwraps it and registers its component individually.
         */
        if (converter instanceof FallbackConverter<?,?>) {
            final FallbackConverter<S,T> fc = (FallbackConverter<S,T>) converter;
            register(fc.primary);
            register(fc.fallback);
            return;
        }
        /*
         * Registers an individual converter.
         */
        final Class<S> sourceClass = converter.getSourceClass();
        final Class<T> targetClass = converter.getTargetClass();
        final Class<?> stopAt = Classes.findCommonClass(sourceClass, targetClass);
        ArgumentChecks.ensureNonNull("sourceClass", sourceClass);
        ArgumentChecks.ensureNonNull("targetClass", targetClass);
        synchronized (converters) {
            /*
             * If this registry has not yet been initialized, initializes it before we search
             * for the place where to put the given converter in the hierarchy of converters.
             */
            if (!isInitialized) {
                isInitialized = true; // Before 'initialize()' for preventing infinite recursivity.
                initialize();
            }
            for (Class<? super T> i=targetClass; i!=null && i!=stopAt; i=i.getSuperclass()) {
                register(new ClassPair(sourceClass, i), converter); // Checked in the JDK7 branch.
            }
            /*
             * At this point, the given class and parent classes have been registered.
             * Now registers interfaces, except for the special cases coded below.
             */
            for (final Class<? super T> i : Classes.getAllInterfaces(targetClass)) {
                if (i.isAssignableFrom(sourceClass)) {
                    /*
                     * Target interface is already implemented by the source, so
                     * there is no reason to convert the source to that interface.
                     */
                    continue;
                }
                if (Cloneable.class.isAssignableFrom(i)) {
                    /*
                     * Exclude this special case. If we were accepting it, we would basically
                     * provide converters from immutable to mutable objects (e.g. from String
                     * to Locale), which is not something we would like to encourage. Even if
                     * the user really wanted a mutable object, in order to modify it he needs
                     * to known the exact type, so asking for a conversion to Cloneable is too
                     * vague.
                     */
                    continue;
                }
                if (sourceClass == Number.class && Comparable.class.isAssignableFrom(i)) {
                    /*
                     * Exclude this special case. java.lang.Number does not implement Comparable,
                     * but its subclasses do. Accepting this case would lead ConverterRegistry to
                     * offer converters from Number to String, which is not the best move if the
                     * user want to compare numbers.
                     */
                    continue;
                }
                if (sourceClass == String.class && Iterable.class.isAssignableFrom(i)) {
                    /*
                     * Exclude the case of String to Iterables (including collections), because
                     * there is too many ways to perform such conversion. For example we do not
                     * want find(String, Iterable) to select a conversion to java.nio.file.Path
                     * (which implements Iterable).
                     */
                    continue;
                }
                if (!i.isAssignableFrom(sourceClass)) {
                    register(new ClassPair(sourceClass, i), converter); // Checked in the JDK7 branch.
                }
            }
        }
    }

    /**
     * Registers the given converter under the given key. If a previous converter is already
     * registered for the given key, then there is a choice:
     *
     * <ul>
     *   <li>If one converter is defined exactly for the {@code <S,T>} classes while the
     *       other converter is not, then the most accurate converter will have precedence.</li>
     *   <li>Otherwise the new converter is registered in addition of the old one in a
     *       chain of fallbacks.</li>
     * </ul>
     *
     * @param key The key under which to register the converter.
     * @param converter The converter to register.
     */
    @SuppressWarnings("unchecked")
    private <S,T> void register(final ClassPair<S,T> key, ObjectConverter<S, ? extends T> converter) {
        final ObjectConverter<? super S, ? extends T> existing = get(key);
        if (existing != null) {
            /*
             * An other converter already exists for the given key. If the converters are
             * equal (i.e. the user registered the same converter twice), do nothing.
             */
            if (existing.equals(converter)) {
                return;
            }
            /*
             * FallbackConverters are created only for converters having the same source class.
             * If this is not the case, the new converter will replace the existing one because
             * its source is more specific:  the source of 'converter' is of type <S> while the
             * source of 'existing' is of type <? super S>.
             */
            assert converter.getSourceClass() == key.sourceClass; // Enforced by parameterized type.
            if (existing.getSourceClass() == key.sourceClass) {
                final boolean oldIsExact = isExactlyFor(existing,  key.targetClass);
                final boolean newIsExact = isExactlyFor(converter, key.targetClass);
                if (oldIsExact & !newIsExact) {
                    /*
                     * The existing converter was defined exactly for the <S,T> classes, while the
                     * new one was defined for an other target. Do not touch the old converter and
                     * discard the new one. The new converter is not really lost since it should
                     * have been registered in a previous iteration for its own <S,T> classes.
                     */
                    return;
                }
                if (newIsExact == oldIsExact) {
                    /*
                     * If no converter is considered more accurate than the other, keep both of
                     * them in a fallback chain. Note that the cast to <S,…> is safe because we
                     * checked the source class in the above 'if' statement.
                     */
                    converter = FallbackConverter.merge((ObjectConverter<S, ? extends T>) existing, converter);
                    assert key.targetClass.isAssignableFrom(converter.getTargetClass()) : converter; // See FallbackConverter.merge javadoc.
                }
            }
        }
        put(key, converter);
    }

    /**
     * Returns {@code true} if the given converter has exactly the given target class.
     * If the given converter is a {@link FallbackConverter}, then all children shall
     * have the same target type too.
     */
    private static boolean isExactlyFor(final ObjectConverter<?,?> converter, final Class<?> targetClass) {
        if (converter.getTargetClass() != targetClass) {
            return false;
        }
        if (converter instanceof FallbackConverter<?,?>) {
            final FallbackConverter<?,?> fc = (FallbackConverter<?,?>) converter;
            return isExactlyFor(fc.primary, targetClass) && isExactlyFor(fc.fallback, targetClass);
        }
        return true;
    }

    /**
     * Returns a converter for exactly the given source and target classes.
     * The default implementation invokes {@link #find(Class, Class)}, then
     * ensures that the converter source and target classes are the same ones
     * than the classes given in argument to this method.
     *
     * @param  <S> The source class.
     * @param  <T> The target class.
     * @param  sourceClass The source class.
     * @param  targetClass The target class, or {@code Object.class} for any.
     * @return The converter from the specified source class to the target class.
     * @throws UnconvertibleObjectException if no converter is found for the given classes.
     */
    @SuppressWarnings("unchecked")
    public <S,T> ObjectConverter<S,T> findExact(final Class<S> sourceClass, final Class<T> targetClass)
            throws UnconvertibleObjectException
    {
        final ObjectConverter<? super S, ? extends T> candidate = find(sourceClass, targetClass);
        if (candidate.getSourceClass() == sourceClass &&
            candidate.getTargetClass() == targetClass)
        {
            return (ObjectConverter<S,T>) candidate;
        }
        throw new UnconvertibleObjectException(Errors.format(Errors.Keys.CanNotConvertFromType_2, sourceClass, targetClass));
    }

    /**
     * Returns a converter suitable for the given source and target classes.
     * This method may return a converter accepting more generic sources or
     * converting to more specific targets.
     *
     * @param  <S> The source class.
     * @param  <T> The target class.
     * @param  sourceClass The source class.
     * @param  targetClass The target class, or {@code Object.class} for any.
     * @return The converter from the specified source class to the target class.
     * @throws UnconvertibleObjectException if no converter is found for the given classes.
     */
    public <S,T> ObjectConverter<? super S, ? extends T> find(final Class<S> sourceClass, final Class<T> targetClass)
            throws UnconvertibleObjectException
    {
        final ClassPair<S,T> key = new ClassPair<S,T>(sourceClass, targetClass);
        synchronized (converters) {
            ObjectConverter<? super S, ? extends T> converter = get(key);
            if (converter != null) {
                return converter;
            }
            /*
             * If the user is invoking this method for the firt time, regiter the converers
             * declared in all "META-INF/services/org.apache.sis.util.ObjectConverter" files
             * found on the classpath and try again.
             */
            if (!isInitialized) {
                isInitialized = true; // Before 'initialize()' for preventing infinite recursivity.
                initialize();
                converter = get(key);
                if (converter != null) {
                    return converter;
                }
            }
            /*
             * At this point, no converter were found explicitly for the given key. Searches a
             * converter accepting some super-class of S, and if we find any cache the result.
             * This is the complement of the search performed in the register(ObjectConverter)
             * method, which looked for the parents of the target class. Here we process the
             * case of the source class.
             */
            ClassPair<? super S, T> candidate = key;
            while ((candidate = candidate.parentSource()) != null) {
                converter = get(candidate);
                if (converter != null) {
                    put(key, converter);
                    return converter;
                }
            }
            /*
             * No converter found. Gives a chance to subclasses to provide dynamically-generated
             * converter.
             */
            converter = createConverter(sourceClass, targetClass);
            if (converter != null) {
                put(key, converter);
                return converter;
            }
            /*
             * Still no converter found. If the source and target classes are array classes,
             * search a converter for their components.
             */
            final Class<?> sourceComponent = sourceClass.getComponentType();
            if (sourceComponent != null) {
                final Class<?> targetComponent = targetClass.getComponentType();
                if (targetComponent != null) {
                    converter = new ArrayConverter<S,T>(sourceClass, targetClass, find(
                            Numbers.primitiveToWrapper(sourceComponent),
                            Numbers.primitiveToWrapper(targetComponent)));
                    put(key, converter);
                    return converter;
                }
            }
        }
        throw new UnconvertibleObjectException(Errors.format(Errors.Keys.CanNotConvertFromType_2, sourceClass, targetClass));
    }

    /**
     * Creates a new converter for the given source and target types, or {@code null} if none.
     * This method is invoked by <code>{@linkplain #find find}(source, target)</code> when no
     * registered converter were found for the given types.
     *
     * <p>The default implementation checks for the trivial case where an identity converter
     * would fit, and returns {@code null} in all other cases.
     * Subclasses can override this method in order to generate some converters dynamically.</p>
     *
     * @param  <S> The source class.
     * @param  <T> The target class.
     * @param  sourceClass The source class.
     * @param  targetClass The target class, or {@code Object.class} for any.
     * @return A newly generated converter from the specified source class to the target class,
     *         or {@code null} if none.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected <S,T> ObjectConverter<S,T> createConverter(final Class<S> sourceClass, final Class<T> targetClass) {
        if (targetClass.isAssignableFrom(sourceClass)) {
            return new IdentityConverter(sourceClass, targetClass, null);
        }
        return null;
    }

    /**
     * Returns a string representation of registered converters for debugging purpose.
     * The converters are show in a tree where all real converters are leafs. Parents
     * of those leafs are {@link FallbackConverter}s which delegate their work to the
     * leafs.
     *
     * @return A string representation of registered converters.
     */
    @Debug
    @Override
    public String toString() {
        final TreeTable table = Column.createTable();
        final TreeTable.Node root = table.getRoot();
        root.setValue(Column.TARGET, getClass());
        synchronized (converters) {
            for (final Map.Entry<ClassPair<?,?>, ObjectConverter<?,?>> entry : converters.entrySet()) {
                TreeTable.Node addTo = root;
                final ClassPair<?,?> key = entry.getKey();
                final ObjectConverter<?,?> converter = entry.getValue();
                if (converter.getSourceClass() != key.sourceClass ||
                    converter.getTargetClass() != key.targetClass)
                {
                    /*
                     * If we enter this block, then the converter is not really for this
                     * (source, target) classes pair. Instead, we are leveraging a converter
                     * which was defined for an other ClassPair.  We show this fact be first
                     * showing this ClassPair, then the actual converter (source, target) as
                     * below:
                     *
                     *     Number      ← String             (the ClassPair key)
                     *       └─Integer ← String             (the ObjectConverter value)
                     *
                     * This is the same idea than the formatting done by FallbackConverter,
                     * except that there is only one child. Actually this can be though as
                     * a lightweight fallback converter.
                     */
                    addTo = addTo.newChild();
                    addTo.setValue(Column.SOURCE, key.sourceClass);
                    addTo.setValue(Column.TARGET, key.targetClass);
                }
                if (converter instanceof FallbackConverter<?,?>) {
                    ((FallbackConverter<?,?>) converter).toTree(addTo.newChild(), true);
                } else {
                    Column.toTree(converter, addTo);
                }
            }
        }
        return Column.format(table);
    }
}
