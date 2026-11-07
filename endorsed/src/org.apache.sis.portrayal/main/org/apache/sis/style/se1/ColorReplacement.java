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
 * Replacement of colors in an external graphic.
 * The replacement is defined by a function, which is usually a {@link org.apache.sis.style.se1.function.Recode}
 * function using the color of each pixel of the external graphic as its lookup value, and mapping some colors
 * to replacement colors. Colors without replacement are left unchanged. Renderers may not support other kinds
 * of expressions.
 *
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param <R>  the type of data to style, such as {@code Feature} or {@code Coverage}.
 */
@XmlType(name = "ColorReplacementType")
@XmlRootElement(name = "ColorReplacement")
public class ColorReplacement<R> extends StyleElement<R> {
    /**
     * The function computing replacement colors, or {@code null} if none.
     *
     * @see #getFunction()
     * @see #setFunction(Expression)
     */
    protected Expression<R,?> function;

    /**
     * For JAXB unmarshalling only.
     */
    private ColorReplacement() {
        // Thread-local factory will be used.
    }

    /**
     * Creates an initially empty color replacement.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public ColorReplacement(final StyleFactory<R> factory) {
        super(factory);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public ColorReplacement(final ColorReplacement<R> source) {
        super(source);
        function = source.function;
    }

    /**
     * Returns the function computing replacement colors.
     * It is usually a {@link org.apache.sis.style.se1.function.Recode} function.
     *
     * @return the function computing replacement colors.
     */
    public Optional<Expression<R,?>> getFunction() {
        return Optional.ofNullable(function);
    }

    /**
     * Sets the function computing replacement colors.
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
    public ColorReplacement<R> clone() {
        final var clone = (ColorReplacement<R>) super.clone();
        return clone;
    }
}
