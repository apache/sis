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

import java.util.Optional;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlRootElement;

// Specific to the main branch:
import org.apache.sis.filter.Expression;


/**
 * Mapping of fixed-numeric pixel values to colors.
 * It can be used for defining the colors of a palette-type raster source.
 * For example, a DEM raster giving elevations in meters above sea level
 * can be translated to a colored image.
 *
 * <p>The mapping is defined by a function, which is an {@link Expression} computing colors.
 * Symbology Encoding defines two functions for that purpose, both using the raster data
 * ({@code "Rasterdata"}) as their lookup value:</p>
 * <ul>
 *   <li>{@link org.apache.sis.style.se1.function.Categorize}: intervals of values mapped to distinct colors,</li>
 *   <li>{@link org.apache.sis.style.se1.function.Interpolate}: colors interpolated between data values.</li>
 * </ul>
 * Renderers may not support other kinds of expressions.
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
     * The function computing colors from the raster data, or {@code null} if none.
     *
     * @see #getFunction()
     * @see #setFunction(Expression)
     */
    protected Expression<R,?> function;

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
        function = source.function;
    }

    /**
     * Returns the function computing colors from the raster data. It is usually a
     * {@link org.apache.sis.style.se1.function.Categorize} or {@link org.apache.sis.style.se1.function.Interpolate} function.
     *
     * @return the function computing colors.
     */
    public Optional<Expression<R,?>> getFunction() {
        return Optional.ofNullable(function);
    }

    /**
     * Sets the function computing colors from the raster data.
     *
     * @param  value  new function, or {@code null} if none.
     */
    public void setFunction(final Expression<R,?> value) {
        function = value;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {function};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public ColorMap<R> clone() {
        return (ColorMap<R>) super.clone();
    }
}
