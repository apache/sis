/*
 * Copyright 2016 desruisseaux.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.internal.gpx;

import java.util.Collections;
import java.util.Set;


/**
 * Base class of GPX element.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
class Element {
    /**
     * Creates a new element.
     */
    Element() {
    }

    /**
     * Returns the given object as a singleton if the given condition is {@code true},
     * or an empty set if the given condition is {@code false}.
     *
     * @param  obj  the object (usually {@code this}) to return in a singleton if the condition is true.
     */
    static <T> Set<T> thisOrEmpty(final T obj, final boolean condition) {
        return condition ? Collections.singleton(obj) : Collections.emptySet();
    }
}
