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
package org.apache.sis.style.sld1;

import java.util.Arrays;


/**
 * Base class of all Styled Layer Descriptor elements.
 * This base class cannot be extended directly.
 * Instead, one of the subclasses can be extended.
 *
 * @author  Johann Sorel (Geomatys)
 */
public abstract class SLDElement implements Cloneable {
    /**
     * Creates a new element.
     * Intentionally restricted to this package because {@link #properties()} is package-private.
     */
    SLDElement() {
    }

    /**
     * Returns all properties contained in the subclasses.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     *
     * @return all properties.
     */
    abstract Object[] properties();

    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return getClass().hashCode() + Arrays.hashCode(properties());
    }

    /**
     * Compares this element with the given object for equality.
     *
     * @param  obj  the other object to compare with this.
     * @return whether the other object is equal to this.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        return (obj != null) && (obj.getClass() == getClass()) &&
                Arrays.equals(properties(), ((SLDElement) obj).properties());
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but filters, features and online resources are not on the assumption
     * that they are immutable or external.
     *
     * @return a clone of this element.
     */
    @Override
    public SLDElement clone() {
        try {
            return (SLDElement) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);    // Should never happen since we are cloneable.
        }
    }
}
