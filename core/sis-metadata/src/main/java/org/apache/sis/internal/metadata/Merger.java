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
package org.apache.sis.internal.metadata;

import java.util.Map;
import java.util.Iterator;
import java.util.Collection;
import java.util.LinkedList;
import java.util.IdentityHashMap;
import java.util.Locale;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.AbstractMetadata;
import org.apache.sis.metadata.InvalidMetadataException;
import org.apache.sis.metadata.ModifiableMetadata;
import org.apache.sis.metadata.KeyNamePolicy;
import org.apache.sis.metadata.ValueExistencePolicy;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Classes;


/**
 * Merges the content of two metadata instances.
 * For each non-null and {@linkplain ValueExistencePolicy#NON_EMPTY non-empty} property
 * value from the <var>source</var> metadata, the merge operation is defined as below:
 *
 * <ul>
 *   <li>If the target metadata does not have a non-null and non-empty value for the same property, then reference
 *       to the value from the source metadata is stored <cite>as-is</cite> in the target metadata.</li>
 *   <li>Otherwise if the target value is a collection, then:
 *     <ul>
 *       <li>Each element of the source collection which is an instance of a
 *           {@linkplain MetadataStandard#getInterface(Class) standard type}
 *           assignable to the type of an element of the target collection, this {@code merge(…)}
 *           method will be invoked recursively for that pair of source and target elements.</li>
 *       <li>All other source elements will be added as new elements in the target collection.</li>
 *     </ul>
 *   </li>
 *   <li>Otherwise the {@link #unmerged unmerged(…)} method is invoked.</li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Benjamin Garcia (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
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
        return Errors.getResources(locale);
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
     * <p>Note that this method will be invoked recursively for all child properties to merge.</p>
     *
     * @param  source  the source metadata to merge into the target. This metadata will not be modified.
     * @param  target  the target metadata where to merge values.
     * @return {@code true} if the merge has been performed, or {@code false} if the {@code target} type
     *         is not compatible with the {@code source} type.
     * @throws ClassCastException if the source and target are not instances of the same metadata standard.
     * @throws InvalidMetadataException if a property of the {@code target} metadata is not specialized
     *         enough for holding a {@code source} property value.
     * @throws IllegalArgumentException if this method detects a cross-reference between source and target.
     */
    public boolean merge(final Object source, final ModifiableMetadata target) {
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
         * We will also verify that the target metadata does not contain a source, or vis-versa.
         */
        final Boolean sourceDone = done.put(source, Boolean.FALSE);
        final Boolean targetDone = done.put(target, Boolean.TRUE);
        if (sourceDone != null || targetDone != null) {
            if (Boolean.FALSE.equals(sourceDone) && Boolean.TRUE.equals(targetDone)) {
                /*
                 * At least, the 'source' and 'target' status are consistent. Pretend that we have already
                 * merged those metadata since actually the merge operation is probably underway by the caller.
                 */
                return true;
            } else {
                throw new IllegalArgumentException(errors().getString(Errors.Keys.CrossReferencesNotSupported));
            }
        }
        /*
         * Get views of metadata as maps. Those maps are live: write operations
         * on those maps will be reflected on the metadata objects and conversely.
         */
        Map<String, Class<?>>    typeMap   = null;
        final Map<String,Object> targetMap = target.asMap();
        final Map<String,Object> sourceMap;
        if (source instanceof AbstractMetadata) {
            sourceMap = ((AbstractMetadata) source).asMap();          // Gives to subclasses a chance to override.
        } else {
            sourceMap = standard.asValueMap(source, KeyNamePolicy.JAVABEANS_PROPERTY, ValueExistencePolicy.NON_EMPTY);
        }
        /*
         * Iterate on source values in order to find the objects that need to be copied or merged.
         * If the value does not exist in the target map, then it can be copied directly.
         */
        for (final Map.Entry<String,Object> entry : sourceMap.entrySet()) {
            final String propertyName = entry.getKey();
            final Object sourceValue  = entry.getValue();
            final Object targetValue  = targetMap.putIfAbsent(propertyName, sourceValue);
            if (targetValue != null) {
                if (targetValue instanceof ModifiableMetadata) {
                    if (!merge(sourceValue, (ModifiableMetadata) targetValue)) {
                        /*
                         * This exception may happen if the source is a subclass of the target. This is the converse
                         * of what we usually have in Java (we can assign a sub-type to a more generic Java variable)
                         * but happen here because if the source is a sub-type, we may not be able to copy all values
                         * from the source to the target. We do not use ClassCastException type in the hope to reduce
                         * confusion.
                         */
                        throw new InvalidMetadataException(errors().getString(Errors.Keys.IllegalPropertyValueClass_3,
                                name(target, propertyName), ((ModifiableMetadata) targetValue).getInterface(),
                                Classes.getClass(sourceValue)));
                    }
                } else if (targetValue instanceof Collection<?>) {
                    /*
                     * If the target value is a collection, then the source value should be a collection too
                     * (otherwise the two objects would not be implementing the same standard, in which case
                     * a ClassCastException is conform to this method contract). The loop tries to merge the
                     * source elements to target elements that are specialized enough.
                     */
                    final Collection<?> sourceList = new LinkedList<>((Collection<?>) sourceValue);
                    for (final Object element : (Collection<?>) targetValue) {
                        if (element instanceof ModifiableMetadata) {
                            final Iterator<?> it = sourceList.iterator();
                            while (it.hasNext()) {
                                if (merge(it.next(), (ModifiableMetadata) element)) {
                                    it.remove();
                                    break;          // Merge at most one source element to each target element.
                                }
                            }
                        }
                    }
                    /*
                     * Add remaining elements one-by-one. In such case, the Apache SIS metadata implementation
                     * shall add the elements to the collection instead than replacing the whole collection by
                     * a singleton. As a partial safety check, we verify that the collection instance does not
                     * change.
                     */
                    for (final Object element : sourceList) {
                        if (targetMap.put(propertyName, element) != targetValue) {
                            throw new InvalidMetadataException(errors().getString(
                                    Errors.Keys.UnsupportedImplementation_1, Classes.getShortClassName(targetValue)));
                        }
                    }
                } else {
                    unmerged(target, propertyName, sourceValue, targetValue);
                }
            }
        }
        return true;
    }

    /**
     * Invoked when a metadata value can not be merged.
     * The default implementation throws an {@link InvalidMetadataException}.
     * Subclasses can override this method if they want to perform a different processing.
     *
     * @param target        the metadata instance in which the value should have been written.
     * @param propertyName  the name of the property to write.
     * @param sourceValue   the value to write.
     * @param targetValue   the value that already exist in the target metadata.
     */
    protected void unmerged(ModifiableMetadata target, String propertyName, Object sourceValue, Object targetValue) {
        throw new InvalidMetadataException(errors().getString(Errors.Keys.ValueAlreadyDefined_1, name(target, propertyName)));
    }
}
