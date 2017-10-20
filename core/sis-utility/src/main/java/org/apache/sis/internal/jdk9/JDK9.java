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
package org.apache.sis.internal.jdk9;

import java.util.List;
import java.util.Collections;
import org.apache.sis.internal.util.UnmodifiableArrayList;


/**
 * Place holder for some functionalities defined only in JDK9.
 * This file will be deleted on the SIS JDK9 branch.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public final class JDK9 {
    /**
     * Do not allow instantiation of this class.
     */
    private JDK9() {
    }

    /**
     * Placeholder for {@code List.of(...)}.
     *
     * @param  <E>       type of elements.
     * @param  elements  the elements to put in an unmodifiable list.
     * @return an unmodifiable list of the given elements.
     */
    @SafeVarargs
    public static <E> List<E> listOf(final E... elements) {
        switch (elements.length) {
            case 0:  return Collections.emptyList();
            case 1:  return Collections.singletonList(elements[0]);
            default: return UnmodifiableArrayList.wrap(elements);
        }
    }
}
