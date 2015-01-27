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
import java.util.AbstractQueue;
import java.util.Collections;
import java.util.Iterator;
import java.util.Queue;


/**
 * An immutable and serializable empty queue.
 *
 * @param  <E> Type of elements in the collection.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class EmptyQueue<E> extends AbstractQueue<E> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2798927118201392605L;

    /**
     * The singleton instance to be returned by {@link Collections#emptyQueue()}.
     * This is not parameterized on intend.
     */
    @SuppressWarnings("rawtypes")
    static final Queue INSTANCE = new EmptyQueue();

    /**
     * Do not allow instantiation except for the singleton.
     */
    private EmptyQueue() {
    }

    @Override public void        clear()    {}
    @Override public boolean     isEmpty()  {return true;}
    @Override public int         size()     {return 0;}
    @Override public Iterator<E> iterator() {return Collections.<E>emptySet().iterator();}
    @Override public boolean     offer(E e) {return false;}
    @Override public E           poll()     {return null;}
    @Override public E           peek()     {return null;}

    /**
     * Returns the singleton instance on deserialization.
     */
    protected Object readResolve() {
        return INSTANCE;
    }
}
