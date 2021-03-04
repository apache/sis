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
package org.apache.sis.io.wkt;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Collection;
import java.util.Iterator;


/**
 * A mutable set containing either {@code null} or a single element.
 * If more than one element is added, only the first one is kept.
 * This is for use with {@link StoredTree#toElements(AbstractParser, Collection, int)}
 * in the common case where we expect exactly one element.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class SingletonElement extends AbstractSet<Element> {
    /**
     * The singleton element, or {@code null} if none.
     */
    Element value;

    /**
     * Creates an initially empty singleton.
     */
    SingletonElement() {
    }

    /**
     * Returns {@code true} if no value has been specified yet.
     */
    @Override
    public boolean isEmpty() {
        return value == null;
    }

    /**
     * Returns the number of elements in this set, which can not be greater than 1.
     */
    @Override
    public int size() {
        return isEmpty() ? 0 : 1;
    }

    /**
     * Returns an iterator over the elements in this set.
     */
    @Override
    public Iterator<Element> iterator() {
        return (isEmpty() ? Collections.<Element>emptySet() : Collections.singleton(value)).iterator();
    }

    /**
     * Adds the given value if this set is empty.
     */
    @Override
    public boolean add(final Element n) {
        if (isEmpty()) {
            value = n;
            return true;
        }
        return false;
    }
}
