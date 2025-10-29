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
package org.apache.sis.util.collection;

import java.io.Serializable;
import java.util.List;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.function.Function;
import java.util.function.Consumer;


/**
 * A list in which values are derived from another list using a given function.
 * The conversion is done on-the-fly every times that an element is accessed.
 * Consequently, this wrapper should be used only for elements that are cheap to wrap.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <S>  type of elements in the source list.
 * @param  <E>  type of elements in this list.
 */
final class DerivedList<S,E> extends AbstractList<E> implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5616103170191124327L;

    /**
     * The list of source elements.
     */
    @SuppressWarnings("serial")         // Not statically typed as Serializable.
    private final List<S> source;

    /**
     * The function for deriving an element in this list from an element in the source list.
     */
    @SuppressWarnings("serial")
    private final Function<S,E> adapter;

    /**
     * Creates a new derived list wrapping the given list.
     *
     * @param  source   the list of source elements.
     * @param  adapter  the function for deriving an element in this list from an element in the source list.
     */
    DerivedList(final List<S> source, final Function<S,E> adapter) {
        this.source  = source;
        this.adapter = adapter;
    }

    /**
     * Delegates to the wrapped list.
     */
    @Override public boolean   isEmpty()        {return source.isEmpty();}
    @Override public int       size()           {return source.size();}
    @Override public E         get(int i)       {return adapter.apply(source.get(i));}
    @Override public E         remove(int i)    {return adapter.apply(source.remove(i));}
    @Override public Stream<E> stream()         {return source.stream().map(adapter);};
    @Override public Stream<E> parallelStream() {return source.parallelStream().map(adapter);};

    /**
     * Returns a view of the portion of this list.
     */
    @Override public List<E> subList(int fromIndex, int toIndex) {
        return new DerivedList<>(source.subList(fromIndex, toIndex), adapter);
    }

    /**
     * Applies the given action on all elements in the list.
     */
    @Override public void forEach(final Consumer<? super E> action) {
        source.forEach(adapt(adapter, action));
    }

    /**
     * Creates a consumer of {@code <S>} which will delegate to a consumer of {@code <E>} objects.
     * This is used for allowing {@code forEach(…)} methods to delegate to underlying source.
     */
    private static <S,E> Consumer<S> adapt(final Function<S,E> adapter, final Consumer<? super E> action) {
        return (e) -> action.accept(adapter.apply(e));
    }

    /**
     * Returns an iterator over the elements in this list.
     *
     * @return a new iterator.
     */
    @Override
    public Iterator<E> iterator() {
        return new Iter<>(source.iterator(), adapter);
    }

    /**
     * An iterator over the elements in the source list, converted on-the-fly to elements of type {@code <E>}.
     * Contrarily to {@link DerivedIterator}, this iterator does not skip null elements.
     */
    private static final class Iter<S,E> implements Iterator<E> {
        /** The iterator over source elements. */
        private final Iterator<S> source;

        /** The function for deriving an element in this list from an element in the source list. */
        private final Function<S,E> adapter;

        /** Creates a new iterator wrapping the given source. */
        Iter(final Iterator<S> source, final Function<S,E> adapter) {
            this.source  = source;
            this.adapter = adapter;
        }

        /** Delegates to the wrapped iterator. */
        @Override public boolean hasNext() {return source.hasNext();}
        @Override public E       next()    {return adapter.apply(source.next());}
        @Override public void    remove()  {source.remove();}

        /** Applies the given action on all remaining elements in the iterator. */
        @Override public void forEachRemaining(final Consumer<? super E> action) {
            source.forEachRemaining(adapt(adapter, action));
        }
    }
}
