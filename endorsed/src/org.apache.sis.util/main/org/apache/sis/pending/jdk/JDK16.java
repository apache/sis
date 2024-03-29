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
package org.apache.sis.pending.jdk;

import java.lang.ref.Reference;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Collectors;


/**
 * Place holder for some functionalities defined in a JDK more recent than Java 11.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class JDK16 {
    /**
     * Do not allow instantiation of this class.
     */
    private JDK16() {
    }

    /**
     * Place holder for {@link Reference#refersTo(Object)} method added in JDK16.
     *
     * @param  <T>  type of elements in the reference.
     * @param  ref  the reference to check, or {@code null}.
     * @param  obj  the value to compare to the reference value.
     * @return whether the reference value is the given object.
     */
    public static <T> boolean refersTo(Reference<T> ref, T obj) {
        return ref.get() == obj;
    }

    /**
     * Place holder for {@link Stream#toList()} method added in JDK16.
     *
     * @param  <T>  type of elements in the stream.
     * @param  s    the stream to convert to a list.
     * @return the stream content as a list.
     */
    public static <T> List<T> toList(final Stream<T> s) {
        return s.collect(Collectors.toList());
    }
}
