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

import java.awt.Color;
import java.util.Optional;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;


/**
 * Instructions about how to fill the interior of polygons.
 * A fill can be a solid color or a repeated {@link GraphicFill}.
 *
 * <!-- Following list of authors contains credits to OGC GeoAPI 2 contributors. -->
 * @author  Johann Sorel (Geomatys)
 * @author  Chris Dillard (SYS Technologies)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param <R>  the type of data to style, such as {@code Feature} or {@code Coverage}.
 */
@XmlType(name = "FillType", propOrder = {
    "graphicFill",
//  "svgParameter"
})
@XmlRootElement(name = "Fill")
public class Fill<R> extends StyleElement<R> implements Translucent<R> {
    /**
     * The image to use for filling the area, or {@code null} if a solid color should be used.
     *
     * @see #getGraphicFill()
     * @see #setGraphicFill(GraphicFill)
     */
    @XmlElement(name = "GraphicFill")
    protected GraphicFill<R> graphicFill;

    /**
     * Color of the interior if it is to be solid-color filled, or {@code null} for the default value.
     * The default value specified by OGC 05-077r4 standard is gray.
     *
     * <p>This property is used when {@link #graphicFill} is null.
     * In XML documents, this is encoded inside a {@code <SvgParameter name="fill">} element.</p>
     *
     * @see #getColor()
     * @see #setColor(Expression)
     */
    protected Expression<R,Color> color;

    /**
     * Level of translucency as a floating point number between 0 and 1 (inclusive), or {@code null} the default value.
     * The default value specified by OGC 05-077r4 standard is 1.
     *
     * <p>In XML documents, this is encoded inside a {@code <SvgParameter name="fill-opacity">} element.</p>
     *
     * @see #getOpacity()
     * @see #setOpacity(Expression)
     */
    protected Expression<R, ? extends Number> opacity;

    /**
     * For JAXB unmarshalling only.
     */
    private Fill() {
        // Thread-local factory will be used.
    }

    /**
     * Creates an opaque fill initialized to the gray color.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public Fill(final StyleFactory<R> factory) {
        super(factory);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public Fill(final Fill<R> source) {
        super(source);
        graphicFill = source.graphicFill;
        color       = source.color;
        opacity     = source.opacity;
    }

    /**
     * If filling with tiled copies of an image, returns the image that should be drawn.
     * Otherwise returns an empty value, which means that a solid color should be used.
     *
     * <p>The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this fill, and conversely.</p>
     *
     * @return image to repeat for filling the area.
     *
     * @see Stroke#getGraphicFill()
     */
    public Optional<GraphicFill<R>> getGraphicFill() {
        return Optional.ofNullable(graphicFill);
    }

    /**
     * Specifies that area should be filled with the given graphic.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new image to repeat for filling the area, or {@code null} if none.
     *
     * @see Stroke#setGraphicFill(GraphicFill)
     */
    public void setGraphicFill(final GraphicFill<R> value) {
        graphicFill = value;
    }

    /**
     * Indicates the color of the area if it is to be solid-color filled.
     * This is used when {@linkplain #getGraphicFill() graphic fill} is null.
     *
     * @return color of the area if it is to be solid-color filled.
     *
     * @see Stroke#getColor()
     */
    public Expression<R,Color> getColor() {
        final var value = color;
        return (value != null) ? value : factory.gray;
    }

    /**
     * Sets the color of the area if it is to be solid-color filled.
     * If this method is never invoked, then the default value is gray.
     * That default value is standardized by OGC 05-077r4.
     *
     * <p>Setting a non-null value clears the {@linkplain #getGraphicFill() graphic fill}
     * because those two properties are mutually exclusive.</p>
     *
     * @param  value  color of the area if solid-color filled, or {@code null} for resetting the default value.
     *
     * @see Stroke#setColor(Expression)
     */
    public void setColor(final Expression<R,Color> value) {
        color = value;
        if (value != null) {
            graphicFill = null;
        }
    }

    /**
     * Sets the color and opacity together.
     * The opacity is derived from the alpha value of the given color.
     *
     * @param  value  new color and opacity, or {@code null} for resetting the defaults.
     */
    public void setColorAndOpacity(Color value) {
        if (value  == null) {
            color   = null;
            opacity = null;
        } else {
            if ((opacity = opacity(value)) != null) {
                value = new Color(value.getRGB() | 0xFF000000);
            }
            color = literal(value);
        }
    }

    /**
     * Indicates the level of translucency as a floating point number between 0 and 1 (inclusive).
     * A value of zero means completely transparent. A value of 1.0 means completely opaque.
     *
     * @return the level of translucency as a floating point number between 0 and 1 (inclusive).
     *
     * @see Stroke#getOpacity()
     * @see Graphic#getOpacity()
     * @see RasterSymbolizer#getOpacity()
     */
    @Override
    public Expression<R, ? extends Number> getOpacity() {
        return defaultToOne(opacity);
    }

    /**
     * Sets the level of translucency as a floating point number between 0 and 1 (inclusive).
     * If this method is never invoked, then the default value is literal 1 (totally opaque).
     * That default value is standardized by OGC 05-077r4.
     *
     * @param  value  new level of translucency, or {@code null} for resetting the default value.
     */
    @Override
    public void setOpacity(final Expression<R, ? extends Number> value) {
        opacity = value;
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
        return new Object[] {graphicFill, color, opacity};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public Fill<R> clone() {
        final var clone = (Fill<R>) super.clone();
        clone.selfClone();
        return clone;
    }

    /**
     * Clones the mutable style fields of this element.
     */
    private void selfClone() {
        if (graphicFill != null) graphicFill = graphicFill.clone();
    }
}
