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
        return new Iterable<>() {
            @Override public Iterator<E> iterator() {
                final ListIterator<E> it = list.listIterator(list.size());
                return new Iterator<E>() {
                    @Override public boolean hasNext() {return it.hasPrevious();}
                    @Override public E       next()    {return it.previous();}
                };
            }
        };
    }
}
