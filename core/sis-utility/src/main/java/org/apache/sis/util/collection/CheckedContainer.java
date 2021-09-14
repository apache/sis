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

import java.util.Collection;


/**
 * A container that ensures that all elements are assignable to a given base type.
 * Checked containers are usually {@link Collection}, but not always.
 *
 * <h2>Constraint</h2>
 * If a class implements both {@code CheckedContainer} and {@code Collection},
 * then the parameterized type shall be the same type. Example:
 *
 * {@preformat java
 *     class MyList<E> extends AbstractList<E> implements CheckedContainer<E> { ... }
 * }
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 0.3
 *
 * @param <E>  the base type of elements in the container.
 *
 * @since 0.3
 * @module
 */
public interface CheckedContainer<E> {
    /**
     * Returns the base type of all elements in this container.
     *
     * @return the element type.
     */
    Class<E> getElementType();
}
