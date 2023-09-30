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

import java.util.List;
import java.util.ArrayList;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlRootElement;

// Specific to the main branch:
import org.apache.sis.filter.Expression;


/**
 * Identification of a font of a certain family, style, and size.
 *
 * <!-- Following list of authors contains credits to OGC GeoAPI 2 contributors. -->
 * @author  Johann Sorel (Geomatys)
 * @author  Chris Dillard (SYS Technologies)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param <R>  the type of data to style, such as {@code Feature} or {@code Coverage}.
 */
@XmlType(name = "FontType")
@XmlRootElement(name = "Font")
public class Font<R> extends StyleElement<R> {
    /**
     * Family names of the font to use, in preference order.
     *
     * <p>In XML documents, this is encoded inside a {@code <SvgParameter name="font-family">} element.</p>
     *
     * @see #family()
     */
    private List<Expression<R,String>> family;

    /**
     * Style (normal or italic) to use for a font.
     *
     * <p>In XML documents, this is encoded inside a {@code <SvgParameter name="font-style">} element.</p>
     *
     * @see #getStyle()
     * @see #setStyle(Expression)
     */
    protected Expression<R,String> style;

    /**
     * Amount of weight or boldness to use for a font.
     *
     * <p>In XML documents, this is encoded inside a {@code <SvgParameter name="font-weight">} element.</p>
     *
     * @see #getWeight()
     * @see #setWeight(Expression)
     */
    protected Expression<R,String> weight;

    /**
     * Size (in pixels) to use for the font.
     *
     * <p>In XML documents, this is encoded inside a {@code <SvgParameter name="font-size">} element.</p>
     *
     * @see #getSize()
     * @see #setSize(Expression)
     */
    protected Expression<R, ? extends Number> size;

    /**
     * For JAXB unmarshalling only.
     */
    private Font() {
        // Thread-local factory will be used.
    }

    /**
     * Creates a font initialized to normal style, normal weight and a size of 10 pixels.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public Font(final StyleFactory<R> factory) {
        super(factory);
        family = new ArrayList<>();
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public Font(final Font<R> source) {
        super(source);
        family = new ArrayList<>(source.family);
        style  = source.style;
        weight = source.weight;
        size   = source.size;
    }

    /**
     * Returns the family names of the font to use, in preference order.
     * Allowed values are system-dependent.
     *
     * <p>The returned collection is <em>live</em>:
     * changes in that collection are reflected into this object, and conversely.</p>
     *
     * @return the family names in preference order, as a live collection.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<Expression<R,String>> family() {
        return family;
    }

    /**
     * Returns the style (normal or italic) to use for a font.
     * The allowed values are "normal", "italic", and "oblique".
     *
     * @return style to use for a font.
     */
    public Expression<R,String> getStyle() {
        final var value = style;
        return (value != null) ? value : factory.normal;
    }

    /**
     * Sets the style (normal or italic) to use for a font.
     * If this method is never invoked, then the default value is the "normal" literal.
     *
     * @param  value  new style to use for a font, or {@code null} for resetting the default value.
     */
    public void setStyle(final Expression<R,String> value) {
        style = value;
    }

    /**
     * Returns the amount of weight or boldness to use for a font.
     * The allowed values are "normal" and "bold".
     *
     * @return amount of weight or boldness to use for a font.
     */
    public Expression<R,String> getWeight() {
        final var value = weight;
        return (value != null) ? value : factory.normal;
    }

    /**
     * Sets the amount of weight or boldness to use for a font.
     * If this method is never invoked, then the default value is the "normal" literal.
     *
     * @param  value  new amount of weight to use for a font, or {@code null} for resetting the default value.
     */
    public void setWeight(final Expression<R,String> value) {
        weight = value;
    }

    /**
     * Returns the size (in pixels) to use for the font.
     *
     * @return size (in pixels) to use for the font.
     */
    public Expression<R, ? extends Number> getSize() {
        final var value = size;
        return (value != null) ? value : factory.ten;
    }

    /**
     * Sets the size (in pixels) to use for the font.
     * If this method is never invoked, then the default value is 10 pixels.
     * That default value is standardized by OGC 05-077r4.
     *
     * @param  value  new size to use for the font, or {@code null} for resetting the default value.
     */
    public void setSize(final Expression<R, ? extends Number> value) {
        size = value;
    }

    /*
     * TODO: we need a private method like below for formatting above SVG parameters:
     * See Stroke for more detais.
     */

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {family, style, weight, size};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public Font<R> clone() {
        final var clone = (Font<R>) super.clone();
        clone.selfClone();
        return clone;
    }

    /**
     * Clones the mutable style fields of this element.
     */
    private void selfClone() {
        family = new ArrayList<>(family);
    }
}
