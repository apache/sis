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

import java.util.Iterator;
import java.util.Collection;
import org.apache.sis.util.Emptiable;
import org.apache.sis.util.collection.Containers;
import static org.apache.sis.metadata.ValueExistencePolicy.*;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.util.ControlledVocabulary;


/**
 * Implementation of {@link AbstractMetadata#isEmpty()} and {@link ModifiableMetadata#prune()} methods.
 *
 * The {@link #visited} map inherited by this class is the thread-local map of metadata objects already tested.
 * Keys are metadata instances, and values are the results of the {@code metadata.isEmpty()} operation.
 * If the final operation requested by the user is {@code isEmpty()}, then this map will contain one of
 * few {@code false} values since the walk in the tree will stop at the first {@code false} value found.
 * If the final operation requested by the user is {@code prune()}, then this map will contain a mix of
 * {@code false} and {@code true} values since the operation will unconditionally walk through the entire tree.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Pruner extends MetadataVisitor<Boolean> {
    /**
     * Provider of visitor instances.
     */
    private static final ThreadLocal<Pruner> VISITORS = ThreadLocal.withInitial(Pruner::new);

    /**
     * {@code true} for removing empty properties.
     */
    private boolean prune;

    /**
     * Whether the metadata is empty.
     */
    private boolean isEmpty;

    /**
     * Creates a new object which will test or prune metadata properties.
     */
    private Pruner() {
    }

    /**
     * Returns the thread-local variable that created this {@code Pruner} instance.
     */
    @Override
    final ThreadLocal<Pruner> creator() {
        return VISITORS;
    }

    /**
     * Returns {@code true} if all properties in the given metadata are null or empty.
     * This method is the entry point for the {@link AbstractMetadata#isEmpty()} and
     * {@link ModifiableMetadata#prune()} public methods.
     *
     * @param  metadata  the metadata object.
     * @param  prune     {@code true} for deleting empty entries.
     * @return {@code true} if all metadata properties are null or empty.
     */
    static boolean isEmpty(final AbstractMetadata metadata, final boolean prune) {
        final Pruner visitor = VISITORS.get();
        final boolean p = visitor.prune;
        visitor.prune = prune;
        final Boolean r = visitor.walk(metadata.getStandard(), metadata.getInterface(), metadata, false);
        visitor.prune = p;
        return (r != null) && r;        // If there is a cycle (r == null), then the metadata is non-empty.
    }

    /**
     * Marks a metadata instance as empty before we start visiting its non-null properties.
     * If the metadata does not contain any property, then the {@link #isEmpty} field will stay {@code true}.
     *
     * @return {@link Filter#NON_EMPTY} since this visitor is not restricted to writable properties.
     *         We need to visit all readable properties even for pruning operation since we need to
     *         determine if the metadata is empty.
     */
    @Override
    Filter preVisit(final PropertyAccessor accessor) {
        isEmpty = true;
        return Filter.NON_EMPTY;
    }

    /**
     * Invoked for each element in the metadata to test or prune. This method is invoked only for new elements
     * not yet processed by {@code Pruner}. The element may be a value object or a collection. For convenience
     * we will proceed as if we had only collections, wrapping value object in a singleton collection.
     *
     * @param  type   the type of elements. Note that this is not necessarily the type
     *                of given {@code element} argument if the latter is a collection.
     * @param  value  value of the metadata element being visited.
     */
    @Override
    Object visit(final Class<?> type, final Object value) {
        final boolean isEmptyMetadata = isEmpty;    // Save the value in case it is overwritten by recursive invocations.
        boolean isEmptyValue = true;
        final Collection<?> values = Containers.toCollection(value);
        for (final Iterator<?> it = values.iterator(); it.hasNext();) {
            final Object element = it.next();
            if (!isNullOrEmpty(element)) {
                /*
                 * At this point, 'element' is not an empty CharSequence, Collection or array.
                 * It may be another metadata, a Java primitive type or user-defined object.
                 *
                 *  - For AbstractMetadata, delegate to the public API in case it has been overriden.
                 *  - For user-defined Emptiable, delegate to the user's isEmpty() method. Note that
                 *    we test at different times depending if 'prune' is true of false.
                 */
                boolean isEmptyElement = false;
                if (element instanceof AbstractMetadata) {
                    final AbstractMetadata md = (AbstractMetadata) element;
                    if (prune) md.prune();
                    isEmptyElement = md.isEmpty();
                } else if (!prune && element instanceof Emptiable) {
                    isEmptyElement = ((Emptiable) element).isEmpty();
                    // If 'prune' is true, we will rather test for Emptiable after our pruning attempt.
                } else if (!(element instanceof ControlledVocabulary)) {
                    final MetadataStandard standard = MetadataStandard.forClass(element.getClass());
                    if (standard != null) {
                        /*
                         * For implementation that are not subtype of AbstractMetadata but nevertheless
                         * implement some metadata interfaces, we will invoke recursively this method.
                         */
                        final Boolean r = walk(standard, type, element, false);
                        if (r != null) {
                            isEmptyElement = r;
                            if (!isEmptyElement && element instanceof Emptiable) {
                                isEmptyElement = ((Emptiable) element).isEmpty();
                            }
                        }
                    } else if (element instanceof Number) {
                        isEmptyElement = Double.isNaN(((Number) element).doubleValue());
                    } else if (element instanceof Boolean) {
                        // Typically methods of the kind 'isFooAvailable()'.
                        isEmptyElement = !((Boolean) element);
                    }
                }
                if (!isEmptyElement) {
                    /*
                     * At this point, we have determined that the property is not empty.
                     * If we are not removing empty nodes, there is no need to continue.
                     */
                    if (!prune) {
                        isEmpty = false;
                        return SKIP_SIBLINGS;
                    }
                    isEmptyValue = false;
                    continue;
                }
            }
            /*
             * Found an empty element. Remove it if the element is part of a collection,
             * then move to the next element in the collection (not yet the next property).
             */
            if (prune && values == value) {
                it.remove();
            }
        }
        /*
         * If all elements were empty, set the whole property to 'null'.
         */
        isEmpty = isEmptyMetadata & isEmptyValue;
        return isEmptyValue & prune ? null : value;
    }

    /**
     * Returns the result of visiting all elements in the metadata.
     */
    @Override
    Boolean result() {
        return isEmpty;
    }
}
