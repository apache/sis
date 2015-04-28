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
package org.apache.sis.util;


/**
 * Interface of classes for which empty instances may exist.
 * This interface is typically used for filtering empty elements from a collection or a tree of objects.
 * Some examples of emptiable classes are:
 *
 * <ul>
 *   <li>{@link org.apache.sis.measure.Range} when the lower bounds is equals to the upper bounds and at least
 *       one bound is exclusive.</li>
 *   <li>{@link org.apache.sis.metadata.AbstractMetadata} when no property value has been given to the metadata,
 *       or all properties are themselves empty.</li>
 *   <li>{@link org.apache.sis.geometry.AbstractEnvelope} when the span, surface or volume inside the envelope
 *       is zero.</li>
 * </ul>
 *
 * SIS collections do <strong>not</strong> implement this interface even if they provide a {@code isEmpty()} method,
 * for consistency with collections in {@code java.util} and other libraries. This policy avoid duplicated calls to
 * {@code isEmpty()} methods when the caller need to check for both {@code Collection} and {@code Emptiable} interfaces.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public interface Emptiable {
    /**
     * Returns {@code true} if this instance is empty. The definition of "emptiness" may vary between implementations.
     * For example {@link org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox#isEmpty()} returns {@code true}
     * if all values are {@code NaN} (i.e. uninitialized) while {@link org.apache.sis.geometry.AbstractEnvelope#isEmpty()}
     * returns {@code true} if the geometric surface is zero.
     *
     * @return {@code true} if this instance is empty, or {@code false} otherwise.
     */
    boolean isEmpty();
}
