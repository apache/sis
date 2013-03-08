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

import java.util.Set;
import java.util.EnumSet;
import java.util.Iterator;
import java.io.Serializable;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.resources.Errors;


/**
 * Fallback to be used when the first converter failed.
 * In case of failure, the error of the first (primary) converter is reported.
 *
 * <p>The primary converter is expected more generic than the fallback converter. We try the generic
 * converter first because we expect that if the user wanted the specific subclass, he would have
 * asked explicitly for it. Trying the generic converter first is both closer to what the user
 * asked and less likely to throw many exceptions before we found a successful conversion.</p>
 *
 * <p>Instances are created by the {@link #merge(ObjectConverter, ObjectConverter)} method.
 * It is invoked when a new converter is {@linkplain ConverterRegistry#register(ObjectConverter)
 * registered} for the same source and target class than an existing converter.</p>
 *
 * @param <S> The base type of source objects.
 * @param <T> The base type of converted objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
final class FallbackConverter<S,T> extends ClassPair<S,T> implements ObjectConverter<S,T>, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -6588190939281568858L;

    /**
     * The primary converter, to be tried first.
     */
    private ObjectConverter<S, ? extends T> primary;

    /**
     * The fallback converter. Its target type should not be assignable from the primary target
     * type, except if both converters have the same target type. We intend {@linkplain #primary}
     * to be the most generic converter, because we assume that if the user wanted a more specific
     * type he would have asked explicitly for it. In addition this layout reduces the amount of
     * exceptions to be thrown and caught before we found a successful conversion.
     */
    private ObjectConverter<S, ? extends T> fallback;

    /**
     * Creates a converter using the given primary and fallback converters. This method may
     * interchange the two converters in order to meet the {@linkplain #fallback} contract.
     *
     * @param sourceClass The {@linkplain #getSourceClass() source class}.
     * @param targetClass The {@linkplain #getTargetClass() target class}.
     * @param primary     A first converter.
     * @param fallback    A second converter.
     *
     * @see #create(ObjectConverter, ObjectConverter)
     */
    private FallbackConverter(final Class<S> sourceClass, final Class<T> targetClass,
                              final ObjectConverter<S, ? extends T> primary,
                              final ObjectConverter<S, ? extends T> fallback)
    {
        super(sourceClass, targetClass);
        if (swap(primary, fallback)) {
            this.primary  = fallback;
            this.fallback = primary;
        } else {
            this.primary  = primary;
            this.fallback = fallback;
        }
        assert sourceClass.equals          (primary .getSourceClass()) : primary;
        assert sourceClass.equals          (fallback.getSourceClass()) : fallback;
        assert targetClass.isAssignableFrom(primary .getTargetClass()) : primary;
        assert targetClass.isAssignableFrom(fallback.getTargetClass()) : fallback;
    }

    /**
     * Returns {@code true} if the given primary and fallback converters should be interchanged.
     * This method may invoke itself recursively.
     *
     * @param  primary  The primary converter to test.
     * @param  fallback The fallback converter to test.
     * @return {@code true} if the given primary and fallback converters should be interchanged.
     */
    private static <S> boolean swap(final ObjectConverter<S,?> primary, final ObjectConverter<S,?> fallback) {
        assert !primary.equals(fallback) : primary;
        if (primary instanceof FallbackConverter<?,?>) {
            final FallbackConverter<S,?> candidate = (FallbackConverter<S,?>) primary;
            return swap(candidate.primary, fallback) && swap(candidate.fallback, fallback);
        } else {
            final Class<?> t1 = primary .getTargetClass();
            final Class<?> t2 = fallback.getTargetClass();
            return !t1.isAssignableFrom(t2) && t2.isAssignableFrom(t1);
        }
    }

    /**
     * Appends the given {@code converter} in the given tree of fallback converters.
     * This method may create a new {@code FallbackConverter} if the given converter
     * can not be inserted in the given tree.
     *
     * <p>This method has no information about {@code <T>} type because of parameterized types
     * erasure, and should not need that information if we didn't made a mistake in this class.
     * Nevertheless for safety, callers are encouraged to verify themselves as below:</p>
     *
     * {@preformat java
     *     Class<T> targetClass = ...;
     *     FallbackConverter<S, ? extends T> converter = merge(...);
     *     assert targetClass.isAssignableFrom(converter.getTargetClass()) : converter;
     * }
     *
     * @param  <S> The base type of source objects.
     * @param  <T> The base type of converted objects.
     * @param  existing The existing tree of converters, or {@code null} if none.
     * @param  converter A new fallback to insert in the converters tree, or {@code null}.
     * @return A tree of converters which contains the given {@code converter}. May be either
     *         {@code existing}, {@code converter} or a new {@code FallbackConverter} instance.
     */
    public static <S,T> ObjectConverter<S, ? extends T> merge(
                  final ObjectConverter<S, ? extends T> existing,
                  final ObjectConverter<S, ? extends T> converter)
    {
        if (converter == null) return existing;
        if (existing  == null) return converter;
        if (existing instanceof FallbackConverter<?,?>) {
            /*
             * If we can merge into the existing tree of converters, return that tree
             * after the merge. Otherwise we will create a new FallbackConverter instance.
             */
            if (((FallbackConverter<S, ? extends T>) existing).tryMerge(converter)) {
                return existing;
            }
        }
        return create(existing, converter);
    }

    /**
     * Creates a converter using the given primary and fallback converters. This method may
     * interchange the two converters in order to meet the {@linkplain #fallback} contract.
     *
     * <p>This method has no information about {@code <T>} type because of parameterized types
     * erasure, and should not need that information if we didn't made a mistake in this class.
     * Nevertheless for safety, direct or indirect callers are encouraged to verify themselves
     * as below:</p>
     *
     * {@preformat java
     *     Class<T> targetClass = ...;
     *     FallbackConverter<S, ? extends T> converter = create(...);
     *     assert targetClass.isAssignableFrom(converter.getTargetClass()) : converter;
     * }
     *
     * @param primary  The primary converter.
     * @param fallback The fallback converter.
     */
    private static <S,T> FallbackConverter<S, ? extends T> create(
            final ObjectConverter<S, ? extends T> primary,
            final ObjectConverter<S, ? extends T> fallback)
    {
        final Class<S>           source  = primary .getSourceClass();
        final Class<? extends T> target1 = primary .getTargetClass();
        final Class<? extends T> target2 = fallback.getTargetClass();
        Class<?> target = Classes.findCommonClass(target1, target2);
        if (target.equals(Object.class)) {
            /*
             * If there is no common parent class other than Object, looks for a common interface.
             * We perform this special processing for Object.class because this class is handled
             * in a special way by the Java language anyway: all interfaces are specialization of
             * Object (in the sense "are assignable to"), so Object can be considered as a common
             * root for both classes and interfaces.
             */
            final Set<Class<?>> interfaces = Classes.findCommonInterfaces(target1, target2);
            interfaces.removeAll(Classes.getAllInterfaces(source));
            final Iterator<Class<?>> it = interfaces.iterator();
            if (it.hasNext()) {
                /*
                 * Arbitrarily retains the first interface. At this point there is hopefully
                 * only one occurrence anyway. If there is more than one interface, they appear
                 * in declaration order so the first one is assumed the "main" interface.
                 */
                target = it.next();
            }
        }
        /*
         * We perform an unchecked cast because in theory <T> is the common super class.
         * However we can not check at run time because generic types are implemented by
         * erasure. If there is no logical error in our algorithm, the cast should be ok.
         * Nevertheless callers are encouraged to verify as documented in the Javadoc.
         */
        @SuppressWarnings({"unchecked","rawtypes"})
        final FallbackConverter<S, ? extends T> converter =
                new FallbackConverter(source, target, primary, fallback);
        return converter;
    }

    /**
     * Tries to insert the given converter in this tree of converters. This is possible
     * only if the target class of the given converter is equals or more specialized
     * than the target class of this converter.
     *
     * @param  converter The converter to try to insert in this tree of converters.
     * @return {@code true} if the insertion has been done, or {@code false} otherwise.
     */
    private boolean tryMerge(final ObjectConverter<S,?> converter) { // Do NOT synchronize here.
        if (!targetClass.isAssignableFrom(converter.getTargetClass())) {
            return false; // Can not merge because of incompatible type.
        }
        @SuppressWarnings("unchecked")
        final FallbackConverter<S, ? extends T> child =
                merge((ObjectConverter<S, ? extends T>) converter);
        if (child != null) {
            // Didn't merged in this tree, but found a child
            // which looks like a better insertion point.
            return child.tryMerge(converter);
        }
        return true;
    }

    /**
     * Inserts the given converter in this tree of fallback converters. If this method detects
     * that the insertion should be done in a child of this tree, then this method returns that
     * child. It is caller responsibility to invoke this method again on the child. We proceed
     * that way in order to release the synchronization lock before to acquire the child lock,
     * in order to reduce the risk of dead-lock.
     *
     * @param  converter The converter to insert in this tree of converters.
     * @return {@code null} if the insertion has been done, or a non-null value
     *         if the insertion should be done in the returned converter instead.
     */
    private synchronized FallbackConverter<S, ? extends T> merge(final ObjectConverter<S, ? extends T> converter) {
        final Class<? extends T> childClass = converter.getTargetClass();
        /*
         * First searches on the fallback side of the tree since they are expected
         * to contain the most specialized classes. Go down the tree until we find
         * the last node capable to accept the converter. Only after that point we
         * may switch the search to the primary side of the tree.
         */
        Class<? extends T> candidateClass = fallback.getTargetClass();
        if (candidateClass.isAssignableFrom(childClass)) {
            /*
             * The new converter could be inserted at this point. Checks if we can
             * continue to walk down the tree, looking for a more specialized node.
             */
            if (fallback instanceof FallbackConverter<?,?>) {
                /*
                 * If (candidateClass != childClass), we could have a situation like below:
                 *
                 * Adding:  String ⇨ Number
                 * to:      String ⇨ Number            : FallbackConverter
                 *          ├───String ⇨ Short
                 *          └───String ⇨ Number        : FallbackConverter
                 *              ├───String ⇨ Integer
                 *              └───String ⇨ Long
                 *
                 * We don't want to insert the generic Number converter between specialized
                 * ones (Integer and Long). So rather than going down the tree in this case,
                 * we will stop the search as if the above "isAssignableFrom" check failed.
                 * Otherwise return the insertion point, which is 'fallback', for recursive
                 * invocation by the caller.
                 */
                if (candidateClass != childClass) {
                    return (FallbackConverter<S, ? extends T>) fallback;
                }
            } else {
                /*
                 * Splits at this point the node in two branches. The previous converter
                 * will be the primary branch and the new converter will be the fallback
                 * branch. The "primary vs fallback" contract is respected since we know
                 * at this point that the new converter is more specialized,  because of
                 * the isAssignableFrom(...) check performed above.
                 */
                fallback = create(fallback, converter);
                return null;
            }
        }
        /*
         * We were looking in the fallback branch. Now look in the primary branch
         * of the same node. The same comments than above apply.
         */
        candidateClass = primary.getTargetClass();
        if (candidateClass.isAssignableFrom(childClass)) {
            if (primary instanceof FallbackConverter<?,?>) {
                if (candidateClass != childClass) {
                    return (FallbackConverter<S, ? extends T>) primary;
                }
            } else {
                primary = create(primary, converter);
                return null;
            }
        }
        /*
         * The branch can not hold the converter. If we can't go down anymore in any
         * of the two branches, insert the converter at the point we have reached so
         * far. If the converter is more generic, inserts it as the primary branch in
         * order to respect the "more generic first" contract.
         */
        if (childClass.isAssignableFrom(primary .getTargetClass()) &&
           !childClass.isAssignableFrom(fallback.getTargetClass()))
        {
            primary = create(primary, converter);
        } else {
            fallback = create(fallback, converter);
        }
        return null;
    }

    /**
     * Returns the primary or fallback converter.
     *
     * @param asPrimary {@code true} for the primary branch, or {@code false} for the fallback branch.
     * @return the requested converter.
     */
    final ObjectConverter<S,? extends T> getConverter(final boolean asPrimary) {
        assert Thread.holdsLock(this);
        return asPrimary ? primary : fallback;
    }

    /**
     * Returns the base type of source objects.
     */
    @Override
    public final Class<S> getSourceClass() {
        return sourceClass;
    }

    /**
     * Returns the base type of target objects.
     */
    @Override
    public final Class<T> getTargetClass() {
        return targetClass;
    }

    /**
     * Returns the manner in which source values (<var>S</var>) are mapped to target values.
     * This is the intersection of the properties of the primary and fallback converters.
     */
    @Override
    public final Set<FunctionProperty> properties() {
        final ObjectConverter<S, ? extends T> primary, fallback;
        synchronized (this) {
            primary  = this.primary;
            fallback = this.fallback;
        }
        Set<FunctionProperty> properties = primary.properties();
        if (!(primary instanceof FallbackConverter<?,?>)) {
            properties = EnumSet.copyOf(properties);
            properties.remove(FunctionProperty.INVERTIBLE);
        }
        properties.retainAll(fallback.properties());
        return properties;
    }

    /**
     * Converts the given object, using the fallback if needed.
     */
    @Override
    public T convert(final S source) throws UnconvertibleObjectException {
        final ObjectConverter<S, ? extends T> primary, fallback;
        synchronized (this) {
            primary  = this.primary;
            fallback = this.fallback;
        }
        try {
            return primary.convert(source);
        } catch (UnconvertibleObjectException exception) {
            try {
                return fallback.convert(source);
            } catch (UnconvertibleObjectException failure) {
                exception.addSuppressed(failure);
                throw exception;
            }
        }
    }

    /**
     * {@code FallbackConverter} are not convertible. This is because the parameterized
     * types are defined as {@code <S, ? extends T>}. The inverse of those types would
     * be {@code <? extends S, T>}, which is not compatible with the design of this class.
     */
    @Override
    public ObjectConverter<T, S> inverse() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.NonInvertibleConversion));
    }
}
