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
package org.apache.sis.style.se1;

import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlRootElement;


/**
 * Mapping of fixed-numeric pixel values to colors.
 * It can be used for defining the colors of a palette-type raster source.
 * For example, a DEM raster giving elevations in meters above sea level
 * can be translated to a colored image.
 *
 * <p>This is a placeholder for future development.
 * OGC 05-077r4 standard defines the dependent classes,
 * but there is too many of them for this initial draft.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param <R>  the type of data to style, such as {@code Feature} or {@code Coverage}.
 */
@XmlType(name = "ColorMapType", propOrder = {
//  "categorize",
//  "interpolate",
//  "jenks"
})
@XmlRootElement(name = "ColorMap")
public class ColorMap<R> extends StyleElement<R> {
    /**
     * For JAXB unmarshalling only.
     */
    private ColorMap() {
        // Thread-local factory will be used.
    }

    /**
     * Creates an initially empty color map.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public ColorMap(final StyleFactory<R> factory) {
        super(factory);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public ColorMap(final ColorMap<R> source) {
        super(source);
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public ColorMap<R> clone() {
        final var clone = (ColorMap<R>) super.clone();
        return clone;
    }
}
