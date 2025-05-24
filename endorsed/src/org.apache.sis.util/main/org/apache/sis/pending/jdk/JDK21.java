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
package org.apache.sis.pending.jdk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;


/**
 * Place holder for some functionalities defined in a JDK more recent than Java 11.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class JDK21 {
    /**
     * Do not allow instantiation of this class.
     */
    private JDK21() {
    }

    /**
     * Placeholder for {@code SequencedCollection.getFirst()}.
     *
     * @param  <E>        type of elements in the collection.
     * @param  sequenced  the sequenced collection for which from which to get an element.
     * @return the requested element.
     */
    public static <E> E getFirst(final List<E> sequenced) {
        try {
            return sequenced.get(0);
        } catch (IndexOutOfBoundsException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Placeholder for {@code SequencedCollection.getLast()}.
     *
     * @param  <E>        type of elements in the collection.
     * @param  sequenced  the sequenced collection for which from which to get an element.
     * @return the requested element.
     */
    public static <E> E getLast(final List<E> sequenced) {
        try {
            return sequenced.get(sequenced.size() - 1);
        } catch (IndexOutOfBoundsException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Placeholder for {@code SequencedMap.putFirst(K, V)}.
     *
     * @param  <K>        type of keys in the map.
     * @param  <V>        type of values in the map.
     * @param  sequenced  the sequenced map for which to put an element first.
     */
    public static <K,V> void putFirst(final LinkedHashMap<K,V> sequenced, final K key, final V value) {
        @SuppressWarnings("unchecked")
        final var copy = (LinkedHashMap<K,V>) sequenced.clone();
        sequenced.clear();
        sequenced.put(key, value);
        sequenced.putAll(copy);
    }

    /**
     * Placeholder for {@code SequencedCollection.reversed()}.
     *
     * @param  <E>        type of elements in the collection.
     * @param  sequenced  the sequenced collection for which to get elements in reverse order.
     * @return elements of the given collection in reverse order.
     */
    public static <E> Iterable<E> reversed(final Collection<E> sequenced) {
        final List<E> list;
        if (sequenced instanceof List<?>) {
            list = (List<E>) sequenced;
        } else {
            list = new ArrayList<>(sequenced);
        }
        return () -> {
            final ListIterator<E> it = list.listIterator(list.size());
            return new Iterator<E>() {
                @Override public boolean hasNext() {return it.hasPrevious();}
                @Override public E       next()    {return it.previous();}
            };
        };
    }

    /**
     * Appends the given character <var>n</var> times.
     *
     * @param  buffer  the buffer where to append the character.
     * @param  c       the character to repeat.
     * @param  count   number of times to repeat the given character.
     */
    public static void repeat(final StringBuilder buffer, final char c, int count) {
        while (--count >= 0) {
            buffer.append(c);
        }
    }
}
