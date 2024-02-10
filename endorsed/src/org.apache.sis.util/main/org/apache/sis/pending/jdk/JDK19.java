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

import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import static org.apache.sis.util.collection.Containers.hashMapCapacity;


/**
 * Place holder for some functionalities defined in a JDK more recent than Java 11.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class JDK19 {
    /**
     * Place holder for {@code Float.PRECISION}.
     */
    public static final int FLOAT_PRECISION = 24;

    /**
     * Place holder for {@code Double.PRECISION}.
     */
    public static final int DOUBLE_PRECISION = 53;

    /**
     * Do not allow instantiation of this class.
     */
    private JDK19() {
    }

    public static <T> HashSet<T> newHashSet(int n) {
        return new HashSet<>(hashMapCapacity(n));
    }

    public static <K,V> HashMap<K,V> newHashMap(int n) {
        return new HashMap<>(hashMapCapacity(n));
    }

    public static <T> LinkedHashSet<T> newLinkedHashSet(int n) {
        return new LinkedHashSet<>(hashMapCapacity(n));
    }

    public static <K, V> LinkedHashMap<K,V> newLinkedHashMap(int n) {
        return new LinkedHashMap<>(hashMapCapacity(n));
    }
}
