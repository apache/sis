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
package org.apache.sis.referencing.internal;

import java.lang.reflect.Type;
import java.util.Objects;
import org.apache.sis.util.Classes;
import org.apache.sis.util.LenientComparable;


/**
 * Declaration of which standard interfaces are implemented by a <abbr>SIS</abbr> class.
 * Used for some {@link LenientComparable#getStandardType()} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class ParameterizedType implements java.lang.reflect.ParameterizedType {
    /**
     * Value returned by {@link #getRawType()}.
     */
    private final Class<?> rawType;

    /**
     * Creates a new parameterized type.
     *
     * @param  rawType  value returned by {@link #getRawType()}.
     */
    protected ParameterizedType(final Class<?> rawType) {
        this.rawType  = rawType;
    }

    /**
     * Returns the class that declares the parameterized types.
     *
     * @return the class that declares the parameterized types.
     */
    @Override
    public final Type getRawType() {
        return rawType;
    }

    /**
     * Returns the single type argument.
     *
     * @return the single type argument.
     */
    protected abstract Class<?> getActualTypeArgument();

    /**
     * Returns the type argument.
     *
     * @return an array of length 1 containing the type argument.
     */
    @Override
    public final Type[] getActualTypeArguments() {
        return new Type[] {getActualTypeArgument()};
    }

    /**
     * Returns {@code null} since this object is used for describing a top-level class.
     *
     * @return {@code null}.
     */
    @Override
    public final Type getOwnerType() {
        return null;
    }

    /**
     * Returns a string representation of this type.
     * Short class names are used.
     *
     * @return a short string representation.
     */
    @Override
    public final String toString() {
        return Classes.getShortName(rawType) + '<' + Classes.getShortName(getActualTypeArgument()) + '>';
    }

    /**
     * Returns a hash code value for this parameterized type.
     *
     * @return a hash code value.
     */
    @Override
    public final int hashCode() {
        return Objects.hash(rawType, getActualTypeArgument());
    }

    /**
     * Compares this parameterized type with the given object for equality.
     *
     * @param  obj  the other object.
     * @return whether the two objects are equal.
     */
    @Override
    public final boolean equals(final Object obj) {
        if (obj instanceof ParameterizedType) {
            final var other = (ParameterizedType) obj;
            return rawType == other.rawType && getActualTypeArgument() == other.getActualTypeArgument();
        }
        return false;
    }
}
