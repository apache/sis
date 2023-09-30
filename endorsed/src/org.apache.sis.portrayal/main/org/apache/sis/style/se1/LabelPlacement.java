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

import jakarta.xml.bind.annotation.XmlSeeAlso;


/**
 * Position of a label relative to a point, line string or polygon.
 * It can be either a {@link PointPlacement} or a {@link LinePlacement}.
 * The former allows a graphic to be plotted directly at the point,
 * which might be useful to label a city.
 * The latter allows to draw a label along a line,
 * which might be useful for labeling a road or a river.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param <R>  the type of data to style, such as {@code Feature} or {@code Coverage}.
 */
@XmlSeeAlso({
    PointPlacement.class,
    LinePlacement.class
})
public abstract class LabelPlacement<R> extends StyleElement<R> {
    /**
     * Creates a new label placement.
     * Intentionally restricted to this package because {@link #properties()} is package-private.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public LabelPlacement(final StyleFactory<R> factory) {
        super(factory);
    }

    /**
     * For JAXB unmarshalling only.
     */
    LabelPlacement() {
        // Thread-local factory will be used.
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    LabelPlacement(final LabelPlacement<R> source) {
        super(source);
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public LabelPlacement<R> clone() {
        return (LabelPlacement<R>) super.clone();
    }
}
