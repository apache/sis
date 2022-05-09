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
package org.apache.sis.test.xml;

import java.util.Set;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;


/**
 * An unmodifiable set used as a sentinel value meaning "all" elements.
 * All {@code add(…)} operations are no-op since the set already contains everything.
 * All {@code contains(…)} operations always return {@code true}.
 * The latter is not strictly correct since we should check the type,
 * but this is not needed for the purpose of this internal class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @param <E>  ignored.
 *
 * @since 1.0
 * @module
 */
final class InfiniteSet<E> extends AbstractSet<E> {
    /**
     * The singleton instance. This is not parameterized on intend.
     */
    @SuppressWarnings("rawtypes")
    static final Set INSTANCE = new InfiniteSet();

    /** For the unique {@link #INSTANCE} only. */
    private InfiniteSet() {}

    @Override public int         size()                            {return Integer.MAX_VALUE;}
    @Override public boolean     isEmpty()                         {return false;}
    @Override public boolean     add(E e)                          {return false;}
    @Override public boolean     addAll(Collection<? extends E> c) {return false;}
    @Override public boolean     contains(Object v)                {return true;}
    @Override public boolean     containsAll(Collection<?> c)      {return true;}
    @Override public Iterator<E> iterator()                        {throw new UnsupportedOperationException();}
    @Override public String      toString()                        {return "InfiniteSet";}
}
