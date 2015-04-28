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

/**
 * Addition to the collection framework. Most classes in this package implement interfaces
 * from the <cite>Java Collection Framework</cite> defined in the {@link java.util} package.
 *
 * <ul class="verbose">
 *   <li>
 *     {@link org.apache.sis.util.collection.WeakHashSet} provides a way to ensure that
 *     a factory returns unique instances for all values that are equal in the sense of
 *     {@link java.util.Objects#deepEquals(Object, Object) Objects.deepEquals(Object, Object)}.
 *     The values that were created in previous factory operations are retained by
 *     {@linkplain java.lang.ref.WeakReference weak references} for reuse.
 *   </li><li>
 *     {@link org.apache.sis.util.collection.Cache} and
 *     {@link org.apache.sis.util.collection.WeakValueHashMap} are {@link java.util.Map java.util.Map}
 *     implementations that may be used for some caching or pseudo-caching functionalities. The
 *     {@link org.apache.sis.util.collection.Cache} implementation is the most full-featured one
 *     and supports concurrency, while the other implementations are more lightweight, sometime
 *     thread-safe but without concurrency support.
 *   </li><li>
 *     {@linkplain org.apache.sis.util.collection.Containers#derivedMap Derived Map} and
 *     {@linkplain org.apache.sis.util.collection.Containers#derivedSet derived Set} are wrapper collections
 *     in which the keys or the values are derived on-the-fly from the content of an other collection.
 *     The can also be used for creating filtered views.
 *   </li><li>
 *     {@link org.apache.sis.util.collection.IntegerList},
 *     {@link org.apache.sis.util.collection.CodeListSet} and
 *     {@link org.apache.sis.util.collection.RangeSet} are collections specialized for a particular kind
 *     of content, providing more efficient storage than what we would get with the general-purpose
 *     collection implementations.
 *   </li><li>
 *     {@link org.apache.sis.util.collection.FrequencySortedSet} provides specialized ways to
 *     organize its elements.
 *   </li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
package org.apache.sis.util.collection;
