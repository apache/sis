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
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlRootElement;

// Branch-dependent imports
import org.apache.sis.filter.Expression;


/**
 * Instructions about how to drawn text on a map.
 * The {@linkplain #getGeometry() geometry} is interpreted as being either a point
 * or a line as needed by the {@linkplain #getLabelPlacement() label placement}.
 * If a given geometry is not of point or line, it shall be transformed into the appropriate type.
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
@XmlType(name = "TextSymbolizerType", propOrder = {
    "label",
    "font",
    "labelPlacement",
    "halo",
    "fill"
})
@XmlRootElement(name = "TextSymbolizer")
public class TextSymbolizer<R> extends Symbolizer<R> {
    /**
     * Text to display, or {@code null} if none.
     *
     * @see #getLabel()
     * @see #setLabel(Expression)
     */
    @XmlElement(name = "Label")
    protected Expression<R,String> label;

    /**
     * Font to apply on the text, or {@code null} for lazily constructed default.
     *
     * @see #getFont()
     * @see #setFont(Font)
     */
    @XmlElement(name = "Font")
    protected Font<R> font;

    /**
     * Indications about how the text should be placed with respect to the feature geometry.
     * If {@code null}, it will be lazily created when first needed.
     *
     * @see #getLabelPlacement()
     * @see #setLabelPlacement(LabelPlacement)
     */
    @XmlElementRef(name = "LabelPlacement")
    protected LabelPlacement<R> labelPlacement;

    /**
     * Indication about a halo to draw around the text, or {@code null} if none.
     *
     * @see #getHalo()
     * @see #setHalo(Halo)
     */
    @XmlElement(name = "Halo")
    protected Halo<R> halo;

    /**
     * Graphic, color and opacity of the text to draw, or {@code null} for lazily constructed default.
     *
     * @see #getFill()
     * @see #setFill(Fill)
     */
    @XmlElement(name = "Fill")
    protected Fill<R> fill;

    /**
     * For JAXB unmarshalling only.
     */
    private TextSymbolizer() {
        // Thread-local factory will be used.
    }

    /**
     * Creates a text symbolizer with default placement and default font.
     * The new symbolizer has no initial label.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public TextSymbolizer(final StyleFactory<R> factory) {
        super(factory);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public TextSymbolizer(final TextSymbolizer<R> source) {
        super(source);
        label          = source.label;
        font           = source.font;
        labelPlacement = source.labelPlacement;
        halo           = source.halo;
        fill           = source.fill;
    }

    /**
     * Returns the expression that will be evaluated to determine what text is displayed.
     * If {@code null}, then no text will be rendered.
     *
     * @return text to display, or {@code null} if none.
     *
     * @todo Replace {@code null} by a default expression searching for a default text property in the feature.
     */
    public Expression<R,String> getLabel() {
        return label;
    }

    /**
     * Sets the expression that will be evaluated to determine what text is displayed.
     * If this method is never invoked, then the default value is {@code null}.
     *
     * @param  value  new text to display, or {@code null} if none.
     */
    public void setLabel(final Expression<R,String> value) {
        label = value;
    }

    /**
     * Returns the font to apply on the text.
     * The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this symbolizer, and conversely.
     *
     * @return font to apply on the text.
     */
    public Font<R> getFont() {
        if (font == null) {
            font = factory.createFont();
        }
        return font;
    }

    /**
     * Sets the font to apply on the text.
     * The given instance is stored by reference, it is not cloned. If this method is never invoked,
     * then the default value is a {@linkplain Font#Font() default font}.
     *
     * @param  value  new font to apply on the text, or {@code null} for resetting the default value.
     */
    public void setFont(final Font<R> value) {
        font = value;
    }

    /**
     * Returns indications about how the text should be placed with respect to the feature geometry.
     * This object will either be an instance of {@link LinePlacement} or {@link PointPlacement}.
     *
     * <p>The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this symbolizer, and conversely.</p>
     *
     * @return how the text should be placed with respect to the feature geometry.
     */
    public LabelPlacement<R> getLabelPlacement() {
        if (labelPlacement == null) {
            labelPlacement = factory.createPointPlacement();
        }
        return labelPlacement;
    }

    /**
     * Sets indications about how the text should be placed with respect to the feature geometry.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is a
     * {@linkplain PointPlacement#PointPlacement() default point placement}.
     *
     * @param  value  new indications about text placement, or {@code null} for resetting the default value.
     */
    public void setLabelPlacement(final LabelPlacement<R> value) {
        labelPlacement = value;
    }

    /**
     * Returns indication about a halo to draw around the text.
     *
     * <p>The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this symbolizer, and conversely.</p>
     *
     * @return indication about a halo to draw around the text.
     */
    public Optional<Halo<R>> getHalo() {
        return Optional.ofNullable(halo);
    }

    /**
     * Sets indication about a halo to draw around the text.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new indication about a halo to draw around the text, or {@code null} if none.
     */
    public void setHalo(final Halo<R> value) {
        halo = value;
    }

    /**
     * Returns the graphic, color and opacity of the text to draw.
     * The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this symbolizer, and conversely.
     *
     * @return graphic, color and opacity of the text to draw.
     */
    public Fill<R> getFill() {
        if (fill == null) {
            fill = factory.createFill();
            fill.setColor(factory.black);
        }
        return fill;
    }

    /**
     * Sets the graphic, color and opacity of the text to draw.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is a solid black color.
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
        return new Object[] {label, font, labelPlacement, halo, fill};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public TextSymbolizer<R> clone() {
        final var clone = (TextSymbolizer<R>) super.clone();
        clone.selfClone();
        return clone;
    }

    /**
     * Clones the mutable style fields of this element.
     */
    private void selfClone() {
        if (font           != null) font           = font.clone();
        if (labelPlacement != null) labelPlacement = labelPlacement.clone();
        if (halo           != null) halo           = halo.clone();
        if (fill           != null) fill           = fill.clone();
    }
}
