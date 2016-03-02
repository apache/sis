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

import java.util.Arrays;
import java.util.Set;
import java.util.EnumSet;
import java.util.Iterator;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Debug;


/**
 * Fallback to be used when the first converter failed.
 * In case of failure, the error of the first (primary) converter is reported.
 *
 * <p>The primary converter is expected more generic than the fallback converter. We try the generic
 * converter first because we expect that if the user wanted the specific subclass, he would have
 * asked explicitly for it. Trying the generic converter first is both closer to what the user
 * asked and less likely to throw many exceptions before we found a successful conversion.</p>
 *
 * <p>All converters in a {@code FallbackConverter} tree have the same source class {@code <S>},
 * and different target classes {@code <? extends T>} <strong>not</strong> equal to {@code <T>}.
 * The tree should never have two classes {@code <T1>} and {@code <T2>} such as one is assignable
 * from the other.</p>
 *
 * <p>Instances are created by the {@link #merge(ObjectConverter, ObjectConverter)} method.
 * It is invoked when a new converter is {@linkplain ConverterRegistry#register(ObjectConverter)
 * registered} for the same source and target class than an existing converter.</p>
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable, and thus inherently thread-safe,
 * if the converters given to the static factory method are also immutable.
 *
 * @param <S> The base type of source objects.
 * @param <T> The base type of converted objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class FallbackConverter<S,T> extends SystemConverter<S,T> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6331789192804695560L;

    /**
     * The primary converter, to be tried first.
     */
    final ObjectConverter<S, ? extends T> primary;

    /**
     * The fallback converter. Its target type should not be assignable from the primary target
     * type, except if both converters have the same target type. We intend {@linkplain #primary}
     * to be the most generic converter, because we assume that if the user wanted a more specific
     * type he would have asked explicitly for it. In addition this layout reduces the amount of
     * exceptions to be thrown and caught before we found a successful conversion.
     */
    final ObjectConverter<S, ? extends T> fallback;

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
        if (needSwap(primary, fallback.getClass())) {
            this.primary  = fallback;
            this.fallback = primary;
        } else {
            this.primary  = primary;
            this.fallback = fallback;
        }
    }

    /**
     * Returns {@code true} if the given primary and fallback converters should be interchanged.
     * This method may invoke itself recursively.
     *
     * @param  primary The primary converter to test.
     * @param  fallbackClass The target class of the fallback converter to test.
     * @return {@code true} if the given primary and fallback converters should be interchanged.
     */
    private static <S> boolean needSwap(final ObjectConverter<S,?> primary, final Class<?> fallbackClass) {
        if (primary instanceof FallbackConverter<?,?>) {
            final FallbackConverter<S,?> candidate = (FallbackConverter<S,?>) primary;
            return needSwap(candidate.primary,  fallbackClass) &&
                   needSwap(candidate.fallback, fallbackClass);
        } else {
            final Class<?> targetClass = primary.getTargetClass();
            return fallbackClass.isAssignableFrom(targetClass) && // This condition is more likely to fail first.
                    !targetClass.isAssignableFrom(fallbackClass);
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
     * In the current implementation, the {@code primary} converter can be either an arbitrary
     * {@code ObjectConverter}, or a previously created {@code FallbackConverter}. However the
     * {@code fallback} converter shall <strong>not</strong> be a {@code FallbackConverter}.
     * This restriction exists because the tree built in such case would probably not be the
     * desired one. It should be okay if only SIS code deal with {@code FallbackConverter}.
     *
     * @param  <S> The base type of source objects.
     * @param  <T> The base type of converted objects.
     * @param  primary The first converter, which may be a {@code Fallback} tree.
     * @param  fallback A new fallback to insert in the converters tree.
     * @return A tree of converters which contains the given {@code converter}. May be either
     *         {@code existing}, {@code converter} or a new {@code FallbackConverter} instance.
     */
    public static <S,T> ObjectConverter<S, ? extends T> merge(
                  final ObjectConverter<S, ? extends T> primary,
                  final ObjectConverter<S, ? extends T> fallback)
    {
        ArgumentChecks.ensureNonNull("primary",  primary);
        ArgumentChecks.ensureNonNull("fallback", fallback);
        assert !(fallback instanceof FallbackConverter<?,?>) : fallback; // See javadoc
        final ObjectConverter<S, ? extends T> candidate = mergeIfSubtype(primary, fallback, null);
        if (candidate != null) {
            return candidate;
        }
        final Class<S>           source  = primary .getSourceClass();
        final Class<? extends T> target1 = primary .getTargetClass();
        final Class<? extends T> target2 = fallback.getTargetClass();
        Class<?> target = Classes.findCommonClass(target1, target2);
        if (target == Object.class) {
            /*
             * If there is no common parent class other than Object, looks for a common interface.
             * We perform this special processing for Object.class because this class is handled
             * in a special way by the Java language anyway: all interfaces are specialization of
             * Object (in the sense "are assignable to"), so Object can be considered as a common
             * root for both classes and interfaces.
             */
            final Set<Class<?>> interfaces = Classes.findCommonInterfaces(target1, target2);
            interfaces.removeAll(Arrays.asList(Classes.getAllInterfaces(source)));
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
        assert target.isAssignableFrom(target1) : target1;
        assert target.isAssignableFrom(target2) : target2;
        @SuppressWarnings({"unchecked","rawtypes"})
        final FallbackConverter<S, ? extends T> converter =
                new FallbackConverter(source, target, primary, fallback);
        return converter;
    }

    /**
     * Merges if the {@code converter} target class of is a subtype of the {@code branch}
     * target class. Otherwise returns {@code null}.
     *
     * <p>The {@code branch} can be either an arbitrary {@code ObjectConverter}, or a previously
     * created {@code FallbackConverter}. However the {@code converter} shall be a new instance,
     * <strong>not</strong> a {@code FallbackConverter} instance.
     * See {@link #merge(ObjectConverter, ObjectConverter)} javadoc for more information.</p>
     *
     * @param  <S> The source class of the {@code branch} converter.
     * @param  <T> The target class of the {@code branch} converter
     * @param  branch The converter to eventually merge with {@code converter}.
     * @param  converter The converter to eventually merge with {@code branch}.
     * @param  parentTarget To be given verbatim to {@link #merge(ObjectConverter, Class)}.
     * @return The merged converter, or {@code null} if the {@code converter}
     *         target class is not a subtype of the {@code branch} target class.
     */
    private static <S,T> ObjectConverter<S, ? extends T> mergeIfSubtype(
            final ObjectConverter<S,T> branch,
            final ObjectConverter<S,?> converter,
            final Class<? super T> parentTarget)
    {
        if (branch.equals(converter)) {
            return branch;
        }
        final Class<T> targetClass = branch.getTargetClass();
        if (!targetClass.isAssignableFrom(converter.getTargetClass())) {
            return null;
        }
        /*
         * At this point we know that 'converter.targetClass' is <T> or a subtype of <T>,
         * so the cast below is safe. If the branch is an instance of FallbackConverter,
         * continue to follow that branch.
         */
        @SuppressWarnings("unchecked")
        final ObjectConverter<S, ? extends T> checked = (ObjectConverter<S, ? extends T>) converter;
        if (branch instanceof FallbackConverter<?,?>) {
            /*
             * Will follow either 'branch.fallback' or 'branch.primary', depending which one
             * is the most appropriate. If none can be followed, then the result will be the
             * same than in the 'else' block.
             */
            return ((FallbackConverter<S,T>) branch).merge(checked, parentTarget);
        } else {
            /*
             * Both 'branch' and 'checked' are ordinary converters (not FallbackConverter).
             */
            return new FallbackConverter<S,T>(branch.getSourceClass(), targetClass, branch, checked);
        }
    }

    /**
     * Merge {@code this} with an other converter whose target class is a subtype of
     * this {@link #targetClass}. If either {@link #fallback} or {@link #primary} are
     * other {@code FallbackConverter} instances, then this method will follow those
     * branches.
     *
     * @param  converter The converter to merge with {@code this}.
     * @param  parentTarget If this method is invoked recursively, the target class
     *         of the parent {@code FallbackConverter}. Otherwise {@code null}.
     * @return The merged converter.
     */
    private ObjectConverter<S, ? extends T> merge(final ObjectConverter<S, ? extends T> converter,
            final Class<? super T> parentTarget)
    {
        ObjectConverter<S, ? extends T> candidate;
        final ObjectConverter<S, ? extends T> newPrimary, newFallback;
        candidate = mergeIfSubtype(fallback, converter, targetClass);
        if (candidate != null) {
            newPrimary  = primary;
            newFallback = candidate;
        } else {
            candidate = mergeIfSubtype(primary, converter, targetClass);
            if (candidate != null) {
                newPrimary  = candidate;
                newFallback = fallback;
            } else if (targetClass != parentTarget) {
                newPrimary  = this;
                newFallback = converter;
            } else {
                /*
                 * If the we can not follow any of the 'primary' and 'fallback' branch,
                 * and if the target class of this FallbackConverter is the same than
                 * the target class of the parent, then do not create. We will let the
                 * parent FallbackConverter do the creation itself in order to chain the
                 * converters in the order they have been declared.
                 */
                return null;
            }
        }
        return new FallbackConverter<S,T>(sourceClass, targetClass, newPrimary, newFallback);
    }

    /**
     * Returns the manner in which source values (<var>S</var>) are mapped to target values.
     * This is the intersection of the properties of the primary and fallback converters.
     */
    @Override
    public final Set<FunctionProperty> properties() {
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
    public T apply(final S source) throws UnconvertibleObjectException {
        try {
            return primary.apply(source);
        } catch (UnconvertibleObjectException exception) {
            try {
                return fallback.apply(source);
            } catch (UnconvertibleObjectException failure) {
                // addSuppressed(failure) on the JDK7 branch.
                throw exception;
            }
        }
    }

    /**
     * Creates a node for the given converter and adds it to the given tree.
     * This method invokes itself recursively for scanning through fallbacks.
     *
     * @param converter The converter for which to create a tree.
     * @param addTo The node in which to add the converter.
     */
    private void toTree(final ObjectConverter<?,?> converter, TreeTable.Node addTo) {
        if (converter instanceof FallbackConverter<?,?>) {
            final boolean isNew = converter.getTargetClass() != targetClass;
            if (isNew) {
                addTo = addTo.newChild();
            }
            ((FallbackConverter<?,?>) converter).toTree(addTo, isNew);
        } else {
            Column.toTree(converter, addTo);
        }
    }

    /**
     * Adds a simplified tree representation of this {@code FallbackConverter}
     * to the given node.
     *
     * @param addTo The node in which to add the converter.
     * @param isNew {@code true} if {@code addTo} is a newly created node.
     */
    final void toTree(final TreeTable.Node addTo, final boolean isNew) {
        if (isNew) {
            addTo.setValue(Column.SOURCE, sourceClass);
            addTo.setValue(Column.TARGET, targetClass);
        }
        toTree(primary,  addTo);
        toTree(fallback, addTo);
    }

    /**
     * Returns a tree representation of this converter.
     * The tree leaves represent the backing converters.
     */
    @Debug
    @Override
    public String toString() {
        final TreeTable table = Column.createTable();
        toTree(table.getRoot(), true);
        return Column.format(table);
    }
}
