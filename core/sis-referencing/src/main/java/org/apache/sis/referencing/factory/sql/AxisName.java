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
package org.apache.sis.referencing.factory.sql;

import org.apache.sis.internal.jdk7.Objects;
import org.apache.sis.util.Debug;


/**
 * A (name, description) pair for a coordinate system axis.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class AxisName {
    /**
     * The coordinate system axis name (never {@code null}).
     */
    final String name;

    /**
     * The coordinate system axis description, or {@code null} if none.
     */
    final String description;

    /**
     * Creates a new coordinate system axis name.
     */
    AxisName(final String name, final String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Returns a hash code for this object.
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Compares this name with the specified object for equality.
     */
    @Override
    public boolean equals(final Object object) {
        if (object instanceof AxisName) {
            final AxisName that = (AxisName) object;
            return name.equals(that.name) && Objects.equals(description, that.description);
        }
        return false;
    }

    /**
     * Returns a string representation of this object, for debugging purpose only.
     */
    @Debug
    @Override
    public String toString() {
        return name;
    }
}
