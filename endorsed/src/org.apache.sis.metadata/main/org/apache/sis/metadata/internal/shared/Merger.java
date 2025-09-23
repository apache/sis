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
package org.apache.sis.metadata.internal.shared;

import java.util.Map;
import java.util.Iterator;
import java.util.Collection;
import java.util.LinkedList;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.function.BiFunction;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.AbstractMetadata;
import org.apache.sis.metadata.InvalidMetadataException;
import org.apache.sis.metadata.ModifiableMetadata;
import org.apache.sis.metadata.KeyNamePolicy;
import org.apache.sis.metadata.ValueExistencePolicy;
import org.apache.sis.util.CorruptedObjectException;
import org.apache.sis.util.Classes;
import org.apache.sis.util.internal.shared.Unsafe;
import org.apache.sis.util.resources.Errors;


/**
 * Merges the content of two metadata instances.
 * For each non-null and {@linkplain ValueExistencePolicy#NON_EMPTY non-empty} property
 * value from the <var>source</var> metadata, the merge operation is defined as below:
 *
 * <ul>
 *   <li>If the target metadata does not have a non-null and non-empty value for the same property, then the
 *     reference to the value from the source metadata is stored <em>as-is</em> in the target metadata.</li>
 *   <li>Otherwise if the target value is a collection, then:
 *     <ul>
 *       <li>For each element of the source collection, a corresponding element of the target collection is searched.
 *         A pair of source and target elements is established if the pair meets all of the following conditions:
 *         <ul>
 *           <li>The {@linkplain MetadataStandard#getInterface(Class) standard type} of the source element
 *               is assignable to the type of the target element.</li>
 *           <li>There is no conflict, i.e. no property value that are not collection and not equal.
 *               This condition can be modified by overriding {@link #resolve(Object, ModifiableMetadata)}.</li>
 *         </ul>
 *         If such pair is found, then the merge operation if performed recursively
 *         for that pair of source and target elements.</li>
 *       <li>All other source elements will be added as new elements in the target collection.</li>
 *     </ul>
 *   </li>
 *   <li>Otherwise the {@link #copy(Object, ModifiableMetadata) copy(…)} method is invoked.</li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Benjamin Garcia (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public class Merger {
    /**
     * The source and target values that have already been copied, for avoiding never-ending loop in cyclic graphs.
     * The value is {@link Boolean#FALSE} if the key is a source, or {@link Boolean#TRUE} if the key is a target.
     */
    private final Map<Object,Boolean> done;

    /**
     * The locale to use for formatting error messages, or {@code null} for the default locale.
     */
    protected final Locale locale;

    /**
     * Creates a new merger.
     *
     * @param  locale  the locale to use for formatting error messages, or {@code null} for the default locale.
     */
    public Merger(final Locale locale) {
        done = new IdentityHashMap<>();
        this.locale = locale;
    }

    /**
     * Returns the resources for error messages.
     */
    private Errors errors() {
        return Errors.forLocale(locale);
    }

    /**
     * Returns the name of the given property in the given metadata instance.
     * This is used for formatting error messages.
     */
    private static String name(final ModifiableMetadata target, final String propertyName) {
        return Classes.getShortName(target.getInterface()) + '.' + propertyName;
    }

    /**
     * Merges the data from the given source into the given target.
     * See class javadoc for a description of the merge process.
     *
     * @param  source  the source metadata to merge into the target. Will never be modified.
     * @param  target  the target metadata where to merge values. Will be modified as a result of this call.
     * @throws ClassCastException if the source and target are not instances of the same metadata standard.
     * @throws InvalidMetadataException if the {@code target} metadata cannot hold all {@code source} properties,
     *         for example because the source class is a more specialized type than the target class.
     * @throws IllegalArgumentException if this method detects a cross-reference between source and target metadata.
     */
    public final void copy(final Object source, final ModifiableMetadata target) {
        if (!copy(source, target, false)) {
            throw new InvalidMetadataException(errors().getString(Errors.Keys.IllegalArgumentClass_3, "target",
                    target.getStandard().getInterface(source.getClass()), Classes.getClass(target)));
        }
    }

    /**
     * Implementation of {@link #copy(Object, ModifiableMetadata)} method,
     * to be invoked recursively for all child properties to merge.
     *
     * @param  dryRun  {@code true} for simulating the merge operation instead of performing the actual merge.
     *                 Used for verifying if there is a merge conflict before to perform the actual operation.
     * @return {@code true} if the merge operation is valid, or {@code false} if the given arguments are valid
     *         metadata but the merge operation can nevertheless not be executed because it could cause data lost.
     */
    @SuppressWarnings("fallthrough")
    private boolean copy(final Object source, final ModifiableMetadata target, final boolean dryRun) {
        /*
         * Verify if the given source can be merged with the target. If this is not the case, action
         * taken will depend on the caller: it may either skips the value or throws an exception.
         */
        final MetadataStandard standard = target.getStandard();
        if (!standard.getInterface(source.getClass()).isInstance(target)) {
            return false;
        }
        /*
         * Only after we verified that the merge operation is theoretically allowed, remember that
         * we are going to merge those two metadata and verify that we are not in an infinite loop.
         * We will also verify that the target metadata does not contain a source, or vice-versa.
         */
        {   // For keeping `sourceDone` and `targetDone` more local.
            final Boolean sourceDone = done.put(source, Boolean.FALSE);
            final Boolean targetDone = done.put(target, Boolean.TRUE);
            if (sourceDone != null || targetDone != null) {
                if (Boolean.FALSE.equals(sourceDone) && Boolean.TRUE.equals(targetDone)) {
                    /*
                     * At least, the `source` and `target` status are consistent. Pretend that we have already
                     * merged those metadata since actually the merge operation is probably underway by the caller.
                     */
                    return true;
                } else {
                    throw new IllegalArgumentException(errors().getString(Errors.Keys.CrossReferencesNotSupported));
                }
            }
        }
        /*
         * Get views of metadata as maps. Those maps are live: write operations
         * on those maps will be reflected on the metadata objects and conversely.
         */
        final Map<String,Object> targetMap = target.asMap();
        final Map<String,Object> sourceMap;
        if (source instanceof AbstractMetadata) {
            sourceMap = ((AbstractMetadata) source).asMap();          // Gives to subclasses a chance to override.
        } else {
            sourceMap = standard.asValueMap(source, null, KeyNamePolicy.JAVABEANS_PROPERTY, ValueExistencePolicy.NON_EMPTY);
        }
        /*
         * Iterate on source values in order to find the objects that need to be copied or merged.
         * If the value does not exist in the target map, then it can be copied directly.
         */
        boolean success = true;
        for (final Map.Entry<String,Object> entry : sourceMap.entrySet()) {
            final String propertyName = entry.getKey();
            final Object sourceValue  = entry.getValue();
            final Object targetValue  = dryRun ? targetMap.get(propertyName)
                                               : targetMap.putIfAbsent(propertyName, sourceValue);
            if (targetValue != null) {
                if (targetValue instanceof ModifiableMetadata) {
                    final var md = (ModifiableMetadata) targetValue;
                    success = copy(sourceValue, md, dryRun);
                    if (!success) {
                        /*
                         * This exception may happen if the source is a subclass of the target. This is the converse
                         * of what we usually have in Java (we can assign a sub-type to a more generic Java variable)
                         * but happen here because if the source is a sub-type, we may not be able to copy all values
                         * from the source to the target. We do not use ClassCastException type in the hope to reduce
                         * confusion.
                         */
                        if (dryRun) break;
                        throw new InvalidMetadataException(errors().getString(Errors.Keys.IllegalPropertyValueClass_3,
                                    name(target, propertyName), md.getInterface(), Classes.getClass(sourceValue)));
                    }
                } else if (targetValue instanceof Collection<?>) {
                    /*
                     * If the merge is executed in dry run, there is no need to verify the collection elements since
                     * in case of conflict, it is always possible to append the source values as new elements at the
                     * end of the collection.
                     */
                    if (dryRun) continue;
                    /*
                     * If the target value is a collection, then the source value should be a collection too
                     * (otherwise the two objects would not be implementing the same standard, in which case
                     * a ClassCastException is conform to this method contract). The loop tries to merge the
                     * source elements to target elements that are specialized enough.
                     */
                    final Collection<?> targetList = (Collection<?>) targetValue;
                    final Collection<?> sourceList = new LinkedList<>((Collection<?>) sourceValue);
                    for (final Object element : targetList) {
                        if (element instanceof ModifiableMetadata) {
                            final Iterator<?> it = sourceList.iterator();
distribute:                 while (it.hasNext()) {
                                final Object value = it.next();
                                switch (resolve(value, (ModifiableMetadata) element)) {
                                    default: throw new UnsupportedOperationException();
                                    case SEPARATE: break;               // do nothing.
                                    case MERGE: {
                                        /*
                                         * If enabled, copy(…, true) call verified that the merge can be done, including
                                         * by recursive checks in all children. The intent is to have a "all or nothing"
                                         * behavior, before the copy(…, false) call below starts to modify the values.
                                         */
                                        if (!copy(value, (ModifiableMetadata) element, false)) break;
                                        // Fall through
                                    }
                                    case IGNORE: {
                                        it.remove();
                                        break distribute;   // Merge at most one source element to each target element.
                                    }
                                }
                            }
                        }
                    }
                    /*
                     * Add remaining elements one-by-one. In such case, the Apache SIS metadata implementation
                     * shall add the elements to the collection instead of replacing the whole collection by
                     * a singleton. As a partial safety check, we verify that the collection instance contains
                     * all the previous values.
                     */
                    for (final Object element : sourceList) {
                        final Object old = targetMap.put(propertyName, element);
                        if (old instanceof Collection<?>) {
                            final Collection<?> oldList = (Collection<?>) old;
                            if (oldList.size() <= targetList.size()) {
                                /*
                                 * Above was only a cheap check based on collection size only.
                                 * Below is a more expensive check if assertions are enabled.
                                 */
                                assert targetList.containsAll(oldList) : propertyName;
                                continue;
                            }
                        }
                        throw new InvalidMetadataException(errors().getString(
                                Errors.Keys.UnsupportedImplementation_1, Classes.getShortClassName(targetList)));
                    }
                } else if (targetValue instanceof Map<?,?>) {
                    success = new ForMap<>(target, propertyName, (Map<?,?>) sourceValue, (Map<?,?>) targetValue).run(dryRun);
                } else {
                    success = targetValue.equals(sourceValue);
                    if (!success) {
                        if (dryRun) break;
                        merge(target, propertyName, sourceValue, targetValue);
                        success = true;         // If no exception has been thrown by `merged`, assume the conflict solved.
                    }
                }
            }
        }
        if (dryRun) {
            if (!Boolean.FALSE.equals(done.remove(source)) || !Boolean.TRUE.equals(done.remove(target))) {
                throw new CorruptedObjectException();           // Should never happen.
            }
        }
        return success;
    }

    /**
     * Helper class for merging the content of two maps where values may be other metadata objects.
     */
    private final class ForMap<V> implements BiFunction<V,V,V> {
        /** Used only in case of non-merged values. */ private final ModifiableMetadata parent;
        /** Used only in case of non-merged values. */ private final String property;
        /** The map to copy. Will not be modified.  */ private final Map<?,?> source;
        /** Where to write copied or merged values. */ private final Map<?,?> target;

        /** Creates a new merger for maps. */
        ForMap(final ModifiableMetadata parent, final String property,
                final Map<?,?> source, final Map<?,?> target)
        {
            this.parent   = parent;
            this.property = property;
            this.source   = source;
            this.target   = target;
        }

        /**
         * Executes the merge process between the maps specified at construction time.
         *
         * @param  dryRun  {@code true} for verifying if there is a merge conflict
         *                 instead that performing the actual merge operation.
         */
        final boolean run(final boolean dryRun) {
            for (final Map.Entry<?,?> pe : source.entrySet()) {
                final Object newValue = pe.getValue();
                if (dryRun) {
                    final Object oldValue;
                    if (newValue != null && (oldValue = target.get(pe.getKey())) != null) {
                        if (newValue instanceof ModifiableMetadata && copy(oldValue, (ModifiableMetadata) newValue, true)) {
                            continue;
                        }
                        return false;               // Copying maps would overwrite at least one value.
                    }
                } else {
                    /*
                     * The two maps have been fetched by calls to the same getter method on the two metadata objects,
                     * so they should have the same types declared by the method signature. However we cannot be sure
                     * that the user didn't override the method with specialized types.
                     */
                    Unsafe.merge(target, pe.getKey(), newValue, this);
                }
            }
            return true;
        }

        /**
         * Invoked when an entry is about to be written in the target map, but a value already exists for that entry.
         *
         * @param  oldValue  the metadata value that already exists.
         * @param  newValue  the metadata value to copy in the target.
         * @return the value to copy in the target (merged) map.
         */
        @Override
        public V apply(final V oldValue, final V newValue) {
            if (newValue instanceof ModifiableMetadata) {
                switch (resolve(oldValue, (ModifiableMetadata) newValue)) {
                    default: throw new UnsupportedOperationException();
                    case IGNORE: break;
                    case MERGE: {
                        if (!copy(oldValue, (ModifiableMetadata) newValue, false)) {
                            merge(parent, property, oldValue, newValue);
                        }
                        break;
                    }
                }
            }
            return (newValue != null) ? newValue : oldValue;
        }
    }

    /**
     * The action to perform when a <var>source</var> metadata element is about to be written in an existing
     * <var>target</var> element. Many metadata elements defined by ISO 19115 allows multi-occurrence, i.e.
     * are stored in {@link Collection}. When a value <var>A</var> is about to be added in an existing collection
     * which already contains values <var>B</var> and <var>C</var>, then different scenarios are possible.
     *
     * <p>For <var>A</var> ⟶ {<var>B</var>, <var>C</var>}:</p>
     * <ul>
     *   <li>Value <var>A</var> may overwrite some values of <var>B</var>. This action is executed if
     *       <code>{@linkplain Merger#resolve Merger.resolve}(A, B)</code> returns {@link #MERGE}.</li>
     *   <li>Value <var>A</var> may overwrite some values of <var>C</var>. This action is executed if
     *       <code>{@linkplain Merger#resolve Merger.resolve}(A, B)</code> returns {@link #SEPARATE},
     *       then {@code Merger.resolve(A, C)} returns {@link #MERGE}.</li>
     *   <li>Value <var>A</var> may be added as a new value after <var>B</var> and <var>C</var>.
     *       This action is executed if <code>{@linkplain Merger#resolve Merger.resolve}(A, B)</code>
     *       <strong>and</strong> {@code Merger.resolve(A, C)} return {@link #SEPARATE}.</li>
     *   <li>Value <var>A</var> may be discarded. This action is executed if
     *       <code>{@linkplain Merger#resolve Merger.resolve}(A, B)</code>
     *       <strong>or</strong> {@code Merger.resolve(A, C)} return {@link #IGNORE}.</li>
     * </ul>
     *
     * @see Merger#resolve(Object, ModifiableMetadata)
     */
    public enum Resolution {
        /**
         * Indicates that <var>source</var> values should be written in <var>target</var> attributes of existing
         * metadata element. No new metadata object is created. If a value already exists in the target metadata,
         * then the {@link Merger#merge(ModifiableMetadata, String, Object, Object) merge(…)} method will be invoked.
         */
        MERGE,

        /**
         * Indicates that <var>source</var> values should be written in another metadata element.
         */
        SEPARATE,

        /**
         * Indicates that <var>source</var> values should be discarded.
         */
        IGNORE
    }

    /**
     * Invoked when a source metadata element is about to be written in an existing target element.
     * The default implementation returns {@link Resolution#MERGE} if writing in the given target
     * would only fill holes, without overwriting any existing value. Otherwise this method returns
     * {@code Resolution#SEPARATE}.
     *
     * @param  source  the source metadata to copy.
     * @param  target  where the source metadata would be copied if this method returns {@link Resolution#MERGE}.
     * @return {@link Resolution#MERGE} for writing {@code source} into {@code target}, or
     *         {@link Resolution#SEPARATE} for writing {@code source} in a separated metadata element, or
     *         {@link Resolution#IGNORE} for discarding {@code source}.
     */
    protected Resolution resolve(Object source, ModifiableMetadata target) {
        return copy(source, target, true) ? Resolution.MERGE : Resolution.SEPARATE;
    }

    /**
     * Invoked when {@code Merger} cannot merge a metadata value by itself.
     * The default implementation throws an {@link InvalidMetadataException}.
     * Subclasses can override this method if they want to perform a different processing.
     *
     * @param target        the metadata instance in which the value should have been written.
     * @param propertyName  the name of the property to write.
     * @param sourceValue   the value to write.
     * @param targetValue   the value that already exist in the target metadata.
     */
    protected void merge(ModifiableMetadata target, String propertyName, Object sourceValue, Object targetValue) {
        throw new InvalidMetadataException(errors().getString(Errors.Keys.ValueAlreadyDefined_1, name(target, propertyName)));
    }
}
