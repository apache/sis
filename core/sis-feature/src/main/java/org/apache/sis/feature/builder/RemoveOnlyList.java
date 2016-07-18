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
package org.apache.sis.feature.builder;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;


/**
 * Wraps another list in a new list allowing only read and remove operations.
 * Addition of new values are not allowed through this {@code RemoveOnlyList}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class RemoveOnlyList<E> extends AbstractList<E> {
    /**
     * The original list to wrap.
     */
    private final List<E> elements;

    /**
     * Creates a new list wrapping the given list.
     */
    RemoveOnlyList(final List<E> elements) {
        this.elements = elements;
    }

    @Override public void        clear()           {       elements.clear();}
    @Override public int         size()            {return elements.size();}
    @Override public E           get(int index)    {return elements.get(index);}
    @Override public E           remove(int index) {return elements.get(index);}
    @Override public Iterator<E> iterator()        {return elements.iterator();}
}
