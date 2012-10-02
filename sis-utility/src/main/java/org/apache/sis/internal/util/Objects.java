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


/**
 * Place holder for {@link java.util.Objects}. This class will be deleted when we will be allowed
 * to compile for JDK7.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from GeoAPI)
 * @version 0.3
 * @module
 */
public final class Objects {
    /**
     * Do not allow instantiation of this class.
     */
    private Objects() {
    }

    /**
     * See JDK7 javadoc.
     *
     * @param value Reference to check against null value.
     * @param message Exception message.
     */
    public static void requireNonNull(final Object value, final String message) {
        if (value == null) {
            throw new NullPointerException(message);
        }
    }

    /**
     * See JDK7 javadoc.
     *
     * @param  o1 First object to compare.
     * @param  o2 Second object to compare.
     * @return {@code true} if both objects are equal.
     */
    public static boolean equals(final Object o1, final Object o2) {
        return (o1 == o2) || (o1 != null && o1.equals(o2));
    }
}
