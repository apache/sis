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
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.apache.sis.internal.util.Cloner;
import org.apache.sis.util.collection.CodeListSet;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.metadata.iso.identification.DefaultRepresentativeFraction;


/**
 * Returns unmodifiable view of metadata elements of arbitrary type.
 * Despite the {@code Cloner} parent class name, this class actually
 * tries to avoid creating new clones as much as possible.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
final class Freezer extends Cloner {
    /**
     * The {@code Freezer} instance in current use. The clean way would have been to pass the {@code Freezer}
     * instance in argument to all {@code freeze()} and {@code unmodifiable()} methods in metadata packages.
     * But above-cited methods are public, and we do not want to expose {@code Freezer} in public API for now.
     * This thread-local is a workaround for that situation.
     */
    private static final ThreadLocal<Freezer> CURRENT = ThreadLocal.withInitial(Freezer::new);

    /**
     * All objects made immutable during iteration over children properties.
     * Keys and values are the same instances. This is used for sharing unique instances when possible.
     */
    private final Map<Object,Object> existings;

    /**
     * Usage count, for determining when to clean {@link #CURRENT}.
     */
    private int useCount;

    /**
     * Creates a new {@code Freezer} instance.
     */
    private Freezer() {
        existings = new HashMap<>(32);
    }

    /**
     * Returns the freezer in current use, or a new one if none.
     * Callers <strong>must</strong> invoke {@link #release()} in a {@code finally} block.
     */
    static Freezer acquire() {
        final Freezer freezer = CURRENT.get();
        freezer.useCount++;
        return freezer;
    }

    /**
     * Release this freezer after usage.
     */
    final void release() {
        if (--useCount == 0) {
            CURRENT.remove();
        }
    }

    /**
     * Returns a unique instance of the given object (metadata or value).
     */
    private Object unique(final Object object) {
        if (object != null) {
            final Object c = existings.putIfAbsent(object, object);
            if (c != null) {
                return c;
            }
        }
        return object;
    }

    /**
     * Tells {@link Cloner#clone(Object)} to return the original object
     * if no public {@code clone()} method is found.
     */
    @Override
    protected boolean isCloneRequired(final Object object) {
        return false;
    }

    /**
     * Recursively clones all elements in the given array.
     */
    private void clones(final Object[] array) throws CloneNotSupportedException {
        for (int i=0; i < array.length; i++) {
            array[i] = clone(array[i]);
        }
    }

    /**
     * Returns an unmodifiable copy of the specified object.
     * This method performs the following heuristic tests:
     *
     * <ul>
     *   <li>If the specified object is an instance of {@code ModifiableMetadata},
     *       then {@link ModifiableMetadata#unmodifiable()} is invoked on that object.</li>
     *   <li>Otherwise, if the object is a {@linkplain Collection collection}, then the
     *       content is copied into a new collection of similar type, with values replaced
     *       by their unmodifiable variant.</li>
     *   <li>Otherwise, if the object implements the {@link Cloneable} interface,
     *       then a clone is returned.</li>
     *   <li>Otherwise, the object is assumed immutable and returned unchanged.</li>
     * </ul>
     *
     * @param  object  the object to convert in an immutable one.
     * @return a presumed immutable view of the specified object.
     */
    @Override
    public Object clone(final Object object) throws CloneNotSupportedException {
        /*
         * CASE 1 - The object is an org.apache.sis.metadata.* implementation. It may have
         *          its own algorithm for creating an unmodifiable view of metadata.
         */
        if (object instanceof ModifiableMetadata) {
            return unique(((ModifiableMetadata) object).unmodifiable());
        }
        if (object instanceof DefaultRepresentativeFraction) {
            final DefaultRepresentativeFraction c = ((DefaultRepresentativeFraction) object).clone();
            c.freeze();
            return unique(c);
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
                    final Object value = clone(array[0]);
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
                            clones(array);
                            collection = CollectionsExt.immutableSet(false, array);
                        }
                    } else {
                        /*
                         * Do not use the SIS Checked* classes since we don't need type checking anymore.
                         * Conservatively assumes a List if we are not sure to have a Set since the list
                         * is less destructive (no removal of duplicated values).
                         */
                        clones(array);
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
                entry.setValue(clone(entry.getValue()));
            }
            return CollectionsExt.unmodifiableOrCopy(map);
        }
        /*
         * CASE 4 - The object is presumed cloneable.
         */
        if (object instanceof Cloneable) {
            return unique(super.clone(object));
        }
        /*
         * CASE 5 - Any other case. The object is assumed immutable and returned unchanged.
         */
        return unique(object);
    }
}
