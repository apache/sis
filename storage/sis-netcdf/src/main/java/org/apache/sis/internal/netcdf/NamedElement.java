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

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.Locale;
import java.util.Map;
import org.apache.sis.internal.util.CollectionsExt;
import org.opengis.parameter.InvalidParameterCardinalityException;


/**
 * Base class of NetCDF dimension, variable or attribute.
 * All those objects share in common a {@link #getName()} method.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
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
     * Creates a (<cite>name</cite>, <cite>element</cite>) mapping for the given array of elements.
     * If the name of an element is not all lower cases, then this method also adds an entry for the
     * lower cases version of that name in order to allow case-insensitive searches.
     *
     * <p>Code searching in the returned map shall ask for the original (non lower-case) name
     * <strong>before</strong> to ask for the lower-cases version of that name.</p>
     *
     * @param  <E>          the type of elements.
     * @param  elements     the elements to store in the map, or {@code null} if none.
     * @param  namesLocale  the locale to use for creating the "all lower cases" names.
     * @return a (<cite>name</cite>, <cite>element</cite>) mapping with lower cases entries where possible.
     * @throws InvalidParameterCardinalityException if the same name is used for more than one element.
     */
    public static <E extends NamedElement> Map<String,E> toCaseInsensitiveNameMap(final E[] elements, final Locale namesLocale) {
        return CollectionsExt.toCaseInsensitiveNameMap(new AbstractList<Map.Entry<String,E>>() {
            @Override
            public int size() {
                return elements.length;
            }

            @Override
            public Map.Entry<String,E> get(final int index) {
                final E e = elements[index];
                return new AbstractMap.SimpleImmutableEntry<>(e.getName(), e);
            }
        }, namesLocale);
    }
}
