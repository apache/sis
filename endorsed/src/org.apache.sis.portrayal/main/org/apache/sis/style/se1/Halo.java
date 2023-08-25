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
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

// Specific to the main branch:
import org.apache.sis.filter.Expression;


/**
 * Fill that is applied to the backgrounds of font glyphs.
 * The use of halos improves the readability of text labels.
 *
 * <!-- Following list of authors contains credits to OGC GeoAPI 2 contributors. -->
 * @author  Johann Sorel (Geomatys)
 * @author  Chris Dillard (SYS Technologies)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @param <R>  the type of data to style, such as {@code Feature} or {@code Coverage}.
 *
 * @since 1.5
 */
@XmlType(name = "HaloType", propOrder = {
    "radius",
    "fill"
})
@XmlRootElement(name = "Halo")
public class Halo<R> extends StyleElement<R> {
    /**
     * Radius (in pixels) of the  the halo around the text, or {@code null} for the default value.
     *
     * @see #getRadius()
     * @see #setRadius(Expression)
     */
    @XmlElement(name = "Radius")
    protected Expression<R, ? extends Number> radius;

    /**
     * How the halo area around the text should be filled, or {@code null} for the default value.
     *
     * @see #getFill()
     * @see #setFill(Fill)
     */
    @XmlElement(name = "Fill")
    protected Fill<R> fill;

    /**
     * For JAXB unmarshalling only.
     */
    private Halo() {
        // Thread-local factory will be used.
    }

    /**
     * Creates an halo initialized to a white color and a radius of 1 pixel.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public Halo(final StyleFactory<R> factory) {
        super(factory);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public Halo(final Halo<R> source) {
        super(source);
        fill   = source.fill;
        radius = source.radius;
    }

    /**
     * Returns the expression fetching the pixel radius of the halo around the text.
     * This is the absolute size of a halo radius in pixels.
     * It extends the area to the outside edge of glyphs and the inside edge of "holes" in the glyphs.
     * Negative values are not allowed.
     *
     * @return radius (in pixels) of the  the halo around the text.
     */
    public Expression<R, ? extends Number> getRadius() {
        return defaultToOne(radius);
    }

    /**
     * Sets the expression fetching the pixel radius of the halo around the text.
     * If this method is never invoked, then the default value is 1 pixel.
     *
     * @param  value  new radius (in pixels), or {@code null} for resetting the default value.
     */
    public void setRadius(Expression<R, ? extends Number> value) {
        radius = value;
    }

    /**
     * Returns the fill for the halo area around the text.
     * The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this halo, and conversely.
     *
     * @return graphic, color and opacity of the text to draw.
     */
    public Fill<R> getFill() {
        if (fill == null) {
            fill = factory.createFill();
            fill.setColor(factory.white);
        }
        return fill;
    }

    /**
     * Sets the fill for the halo area around the text.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is a solid white color.
     * That default value is standardized by OGC 05-077r4.
     *
     * @param  value  new fill of the text to draw, or {@code null} for resetting the default value.
     */
    public void setFill(final Fill<R> value) {
        fill = value;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {radius, fill};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public Halo<R> clone() {
        final var clone = (Halo<R>) super.clone();
        clone.selfClone();
        return clone;
    }

    /**
     * Clones the mutable style fields of this element.
     */
    private void selfClone() {
        if (fill != null) fill = fill.clone();
    }
}
