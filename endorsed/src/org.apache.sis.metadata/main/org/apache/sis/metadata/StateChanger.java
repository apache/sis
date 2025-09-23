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
import java.util.EnumSet;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import org.apache.sis.util.internal.shared.Cloner;
import org.apache.sis.util.internal.shared.CollectionsExt;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;
import org.apache.sis.util.collection.CodeListSet;
import org.apache.sis.metadata.iso.identification.DefaultRepresentativeFraction;


/**
 * Invokes {@link ModifiableMetadata#transitionTo(ModifiableMetadata.State)} recursively on metadata elements.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class StateChanger extends MetadataVisitor<Boolean> {
    /**
     * The {@code StateChanger} instance in current use. The clean way would have been to pass
     * the instance in argument to {@link ModifiableMetadata#transitionTo(ModifiableMetadata.State)}.
     * But above-cited method ix public and we do not want to expose {@code StateChanger} in public API.
     * This thread-local is a workaround for that situation.
     */
    private static final ThreadLocal<StateChanger> VISITORS = ThreadLocal.withInitial(StateChanger::new);

    /**
     * The state to apply on all metadata objects.
     */
    private ModifiableMetadata.State target;

    /**
     * The cloner, created when first needed.
     */
    private Cloner cloner;

    /**
     * Creates a new {@code StateChanger} instance.
     */
    private StateChanger() {
    }

    /**
     * Applies a state change on the given metadata object. This is the implementation
     * {@link ModifiableMetadata#transitionTo(ModifiableMetadata.State)} public method.
     *
     * <p>This is conceptually an instance (non-static) method. But the {@code this} value is not known
     * by the caller, because doing otherwise would force us to give public visibility to classes that we
     * want to keep package-private. The {@link #VISITORS} thread local variable is used as a workaround
     * for providing {@code this} instance without making {@code StateChanger} public.</p>
     */
    static void applyTo(final ModifiableMetadata.State target, final ModifiableMetadata metadata) {
        final StateChanger changer = VISITORS.get();
        final ModifiableMetadata.State previous = changer.target;
        changer.target = target;
        changer.walk(metadata.getStandard(), null, metadata, true);
        changer.target = previous;
    }

    /**
     * Returns the thread-local variable that created this {@code StateChanger} instance.
     * {@link ThreadLocal#remove()} will be invoked after {@link MetadataVisitor} finished
     * to walk through the given metadata and all its children.
     */
    @Override
    final ThreadLocal<StateChanger> creator() {
        return VISITORS;
    }

    /**
     * Notifies {@link MetadataVisitor} that we want to visit all writable properties.
     *
     * @param  accessor  ignored.
     * @return {@link Filter#WRITABLE}, for iterating over all writable properties.
     */
    @Override
    Filter preVisit(final PropertyAccessor accessor) {
        return Filter.WRITABLE;
    }

    /**
     * Invoked for metadata instances on which to apply a change of state.
     *
     * @param  type    ignored (can be {@code null}).
     * @param  object  the object to transition to a different state.
     * @return the given object or a copy of the given object with its state changed.
     */
    @Override
    final Object visit(final Class<?> type, final Object object) throws CloneNotSupportedException {
        return applyTo(object);
    }

    /**
     * Recursively changes the state of all elements in the given array.
     */
    private void applyToAll(final Object[] array) throws CloneNotSupportedException {
        for (int i=0; i < array.length; i++) {
            array[i] = applyTo(array[i]);
        }
    }

    /**
     * Returns the given object, or a copy of the given object, with its state changed.
     * This method performs the following heuristic tests:
     *
     * <ul>
     *   <li>If the specified object is an instance of {@code ModifiableMetadata}, then
     *       {@link ModifiableMetadata#transitionTo(ModifiableMetadata.State)} is invoked on that object.</li>
     *   <li>Otherwise, if the object is a {@linkplain Collection collection}, then the
     *       content is copied into a new collection of similar type, with values replaced
     *       by their unmodifiable variant.</li>
     *   <li>Otherwise, if the object implements the {@link Cloneable} interface,
     *       then a clone is returned.</li>
     *   <li>Otherwise, the object is assumed immutable and returned unchanged.</li>
     * </ul>
     *
     * @param  object  the object to transition to a different state.
     * @return the given object or a copy of the given object with its state changed.
     */
    private Object applyTo(final Object object) throws CloneNotSupportedException {
        /*
         * CASE 1 - The object is an org.apache.sis.metadata.* implementation.
         *          It may have its own algorithm for changing its state.
         */
        if (object instanceof ModifiableMetadata) {
            ((ModifiableMetadata) object).transitionTo(target);
            return object;
        }
        if (object instanceof DefaultRepresentativeFraction) {
            if (target.isUnmodifiable()) {
                ((DefaultRepresentativeFraction) object).freeze();
                return object;
            }
        }
        /*
         * CASE 2 - The object is a collection. All elements are replaced by their
         *          unmodifiable variant and stored in a new collection of similar
         *          type.
         */
        if (object instanceof Collection<?>) {
            Collection<?> collection = (Collection<?>) object;
            final boolean isSet = (collection instanceof Set<?>);
            final Object[] array = collection.toArray();
            switch (array.length) {
                case 0: {
                    collection = isSet ? Collections.EMPTY_SET
                                       : Collections.EMPTY_LIST;
                    break;
                }
                case 1: {
                    final Object value = applyTo(array[0]);
                    collection = isSet ? Collections.singleton(value)
                                       : Collections.singletonList(value);
                    break;
                }
                default: {
                    if (isSet) {
                        if (collection instanceof EnumSet<?>) {
                            collection = Collections.unmodifiableSet(((EnumSet<?>) collection).clone());
                        } else if (collection instanceof CodeListSet<?>) {
                            collection = Collections.unmodifiableSet(((CodeListSet<?>) collection).clone());
                        } else {
                            applyToAll(array);
                            collection = CollectionsExt.immutableSet(false, array);
                        }
                    } else {
                        /*
                         * Do not use the SIS Checked* classes since we don't need type checking anymore.
                         * Conservatively assumes a List if we are not sure to have a Set since the list
                         * is less destructive (no removal of duplicated values).
                         */
                        applyToAll(array);
                        collection = UnmodifiableArrayList.wrap(array);
                    }
                    break;
                }
            }
            return collection;
        }
        /*
         * CASE 3 - The object is a map. Copies all entries in a new map and replaces all values
         *          by their unmodifiable variant. The keys are assumed already immutable.
         */
        if (object instanceof Map<?,?>) {
            final Map<Object,Object> map = new LinkedHashMap<>((Map<?,?>) object);
            for (final Map.Entry<Object,Object> entry : map.entrySet()) {
                entry.setValue(applyTo(entry.getValue()));
            }
            return CollectionsExt.unmodifiableOrCopy(map);
        }
        /*
         * CASE 4 - The object is presumed cloneable.
         */
        if (object instanceof Cloneable) {
            if (cloner == null) {
                cloner = new Cloner(false);
            }
            return cloner.clone(object);
        }
        /*
         * CASE 5 - Any other case. The object is assumed immutable and returned unchanged.
         */
        return object;
    }
}
