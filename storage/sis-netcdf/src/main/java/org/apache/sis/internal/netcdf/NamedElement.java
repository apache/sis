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
package org.apache.sis.internal.netcdf;

import org.apache.sis.util.Characters;
import org.apache.sis.util.CharSequences;


/**
 * Base class of netCDF dimension, variable or attribute.
 * All those objects share in common a {@link #getName()} method.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
public abstract class NamedElement {
    /**
     * For subclasses constructors.
     */
    protected NamedElement() {
    }

    /**
     * Returns the dimension, variable or attribute name.
     *
     * @return the name of this element.
     */
    public abstract String getName();

    /**
     * Returns {@code true} if the given names are considered equals for the purpose of netCDF decoder.
     * Two names are considered similar if they are equal ignoring case and characters that are not valid
     * for an Unicode identifier.
     *
     * @param  s1  the first characters sequence to compare, or {@code null}.
     * @param  s2  the second characters sequence to compare, or {@code null}.
     * @return whether the two characters sequences are considered similar names.
     */
    protected static boolean similar(final CharSequence s1, final CharSequence s2) {
        return CharSequences.equalsFiltered(s1, s2, Characters.Filter.UNICODE_IDENTIFIER, true);
    }

    /**
     * Returns a string representation of this element. Current implementation returns only the element class and name.
     *
     * @return string representation of this element for debugging purposes.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[\"" + getName() + "\"]";
    }
}
