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
package org.apache.sis.internal.style;

import java.awt.Color;
import java.util.Optional;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.sis.util.ArgumentChecks;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;


/**
 * Instructions about how to fill the interior of polygons.
 * A fill can be a solid color or a repeated {@link GraphicFill}.
 *
 * <!-- Following list of authors contains credits to OGC GeoAPI 2 contributors. -->
 * @author  Johann Sorel (Geomatys)
 * @author  Chris Dillard (SYS Technologies)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
@XmlType(name = "FillType", propOrder = {
    "graphicFill",
//  "svgParameter"
})
@XmlRootElement(name = "Fill")
public class Fill extends StyleElement implements Translucent {
    /**
     * Literal for a predefined color which can be used as fill color.
     */
    public static final Literal<Feature,Color> BLACK, GRAY, WHITE;
    static {
        final var FF = FF();
        BLACK = FF.literal(Color.BLACK);
        GRAY  = FF.literal(Color.GRAY);
        WHITE = FF.literal(Color.WHITE);
    }

    /**
     * The image to use for filling the area, or {@code null} if a solid color should be used.
     *
     * @see #getGraphicFill()
     * @see #setGraphicFill(GraphicFill)
     */
    @XmlElement(name = "GraphicFill")
    protected GraphicFill graphicFill;

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
    protected Expression<Feature,Color> color;

    /**
     * Level of translucency as a floating point number between 0 and 1 (inclusive), or {@code null} the default value.
     * The default value specified by OGC 05-077r4 standard is 1.
     *
     * <p>In XML documents, this is encoded inside a {@code <SvgParameter name="fill-opacity">} element.</p>
     *
     * @see #getOpacity()
     * @see #setOpacity(Expression)
     */
    protected Expression<Feature, ? extends Number> opacity;

    /**
     * Returns the opacity of the alpha value of the given color.
     * If the color is totally opaque, then this method returns {@code null}.
     *
     * @param  color  color from which to get the opacity.
     * @return opacity derived from the alpha value of the color, or {@code null} if totally opaque.
     */
    static Expression<Feature, ? extends Number> opacity(final Color color) {
        final int alpha = color.getAlpha();
        return (alpha != 255) ? literal(alpha / 256d) : null;
        // Divide by 256 instead of 255 in order to get round numbers for alpha values 64, 128, etc.
    }

    /**
     * Creates an opaque fill initialized to the gray color.
     */
    public Fill() {
    }

    /**
     * Creates a fill initialized to the given color.
     * The opacity is derived from the alpha value of the given color.
     *
     * @param  color  the initial color.
     */
    public Fill(Color color) {
        ArgumentChecks.ensureNonNull("color", color);
        if ((opacity = opacity(color)) != null) {
            color = new Color(color.getRGB() | 0xFF000000);
        }
        this.color = literal(color);
    }

    /**
     * Creates an opaque fill initialized to the specified color.
     *
     * @param  color  the initial color.
     */
    Fill(final Expression<Feature,Color> color) {
        this.color = color;
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public Fill(final Fill source) {
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
    public Optional<GraphicFill> getGraphicFill() {
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
    public void setGraphicFill(final GraphicFill value) {
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
    public Expression<Feature,Color> getColor() {
        final var value = color;
        return (value != null) ? value : GRAY;
    }

    /**
     * Sets the color of the area if it is to be solid-color filled.
     * If this method is never invoked, then the default value is {@link Fill#GRAY}.
     * That default value is standardized by OGC 05-077r4.
     *
     * <p>Setting a non-null value clears the {@linkplain #getGraphicFill() graphic fill}
     * because those two properties are mutually exclusive.</p>
     *
     * @param  value  color of the area if solid-color filled, or {@code null} for resetting the default value.
     *
     * @see Stroke#setColor(Expression)
     */
    public void setColor(final Expression<Feature,Color> value) {
        color = value;
        if (value != null) {
            graphicFill = null;
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
    public Expression<Feature, ? extends Number> getOpacity() {
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
    public void setOpacity(final Expression<Feature, ? extends Number> value) {
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
    public Fill clone() {
        final var clone = (Fill) super.clone();
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
