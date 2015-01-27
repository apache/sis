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
package org.apache.sis.internal.util;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;


/**
 * An immutable, serializable empty sorted set.
 * This class exists only on the JDK6 and JDK7 branches;
 * it will be removed on the JDK8 branch.
 *
 * @param  <E> Type of elements in the collection.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class EmptySortedSet<E> extends AbstractSet<E> implements SortedSet<E>, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -2404255437755421772L;

    /**
     * The unique instance of this set.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    static final SortedSet INSTANCE = new EmptySortedSet();

    /**
     * Do not allow instantiation except for the unique instance.
     */
    private EmptySortedSet() {
    }

    @Override public void          clear()                      {}
    @Override public Comparator<E> comparator()                 {return null;}
    @Override public Iterator<E>   iterator()                   {return Collections.<E>emptySet().iterator();}
    @Override public int           size()                       {return 0;}
    @Override public boolean       isEmpty()                    {return true;}
    @Override public boolean       contains(Object obj)         {return false;}
    @Override public boolean       containsAll(Collection<?> c) {return c.isEmpty();}
    @Override public E             first()                      {throw new NoSuchElementException();}
    @Override public E             last()                       {throw new NoSuchElementException();}
    @Override public SortedSet<E>  subSet(E from, E to)         {return this;}
    @Override public SortedSet<E>  headSet(E toElement)         {return this;}
    @Override public SortedSet<E>  tailSet(E fromElement)       {return this;}

    /**
     * Returns the unique instance on deserialization.
     */
    private Object readResolve() {
        return INSTANCE;
    }
}
