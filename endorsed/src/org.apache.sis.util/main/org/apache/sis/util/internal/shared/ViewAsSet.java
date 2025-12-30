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
package org.apache.sis.util.internal.shared;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.IntFunction;
import org.apache.sis.util.collection.Containers;


/**
 * A collection viewed as an unmodifiable set.
 * It is caller's responsibility to ensure that the collection contains no duplicated values.
 * The collection may be a list, which result in an inefficient {@link #contains(Object)} method.
 * This class should be used only for sets that we want to construct quickly and that are not expected
 * to be used intensively. Usually, the only operation performed on the set will be to iterate over all elements.
 *
 * @todo Create a sub-type implementing {@link java.util.SequencedSet} with JDK21.
 *
 * @param  <E>  type of elements in the set.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ViewAsSet<E> extends AbstractSet<E> {
    /**
     * The source of elements.
     */
    private final Collection<E> elements;

    /**
     * Creates a new view for the given elements.
     *
     * @param  elements  the elements to view as a set.
     */
    public ViewAsSet(final Collection<E> elements) {
        this.elements = elements;
    }

    @Override public int         size()                       {return elements.size();}
    @Override public boolean     isEmpty()                    {return elements.isEmpty();}
    @Override public boolean     contains(Object o)           {return elements.contains(o);}
    @Override public boolean     containsAll(Collection<?> c) {return elements.containsAll(c);}
    @Override public Iterator<E> iterator()                   {return elements.iterator();}
    @Override public Object[]    toArray()                    {return elements.toArray();}
    @Override public <T> T[]     toArray(T[] a)               {return elements.toArray(a);}
    @Override public <T> T[]     toArray(IntFunction<T[]> g)  {return elements.toArray(g);}

    /**
     * Returns in an unmodifiable set the names of all enumeration values of the given type.
     * The names are obtained by {@link Enum#name()}, which guarantees that there is no duplicated values.
     * The iteration order is the declaration order of the enumeration values.
     *
     * @param  type  type of the enumeration for which to get the names.
     * @return the names viewed as an unmodifiable set.
     */
    public static Set<String> namesOf(final Class<? extends Enum<?>> type) {
        return new ViewAsSet<>(Containers.derivedList(Arrays.asList(type.getEnumConstants()), Enum::name));
    }
}
