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
 * Instructions about how to draw styled lines.
 * Stroke objects are contained by {@link LineSymbolizer} and {@link PolygonSymbolizer}.
 * There are three basic types of strokes: solid-color, {@link GraphicFill} (stipple),
 * and repeated linear {@link GraphicStroke}.
 * A repeated linear graphic is plotted linearly and has its graphic symbol bent around the curves
 * of the line string, and a graphic fill has the pixels of the line rendered with a repeating area-fill pattern.
 * If neither a {@linkplain #getGraphicFill() graphic fill} nor {@linkplain #getGraphicStroke() graphic stroke}
 * element is given, then the line symbolizer will render a solid color.
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
@XmlType(name = "StrokeType", propOrder = {
    "graphicFill",
    "graphicStroke",
//  "svgParameter"
})
@XmlRootElement(name = "Stroke")
public class Stroke<R> extends StyleElement<R> implements Translucent<R> {
    /**
     * Graphic for tiling the (thin) area of the line, or {@code null} if none.
     * This property and {@link #graphicStroke} are mutually exclusive.
     *
     * @see #getGraphicFill()
     * @see #setGraphicFill(GraphicFill)
     */
    @XmlElement(name = "GraphicFill")
    protected GraphicFill<R> graphicFill;

    /**
     * Graphic to repeat along the path of the lines, or {@code null} if none.
     * This property and {@link #graphicFill} are mutually exclusive.
     *
     * @see #getGraphicStroke()
     * @see #setGraphicStroke(GraphicStroke)
     */
    @XmlElement(name = "GraphicStroke")
    protected GraphicStroke<R> graphicStroke;

    /**
     * Color of the line if it is to be solid-color filled, or {@code null} for the default value.
     * The default value specified by OGC 05-077r4 standard is black.
     *
     * <p>This property is used when both {@link #graphicFill} and {@link #graphicStroke} are null.
     * In XML documents, this is encoded inside a {@code <SvgParameter name="stroke">} element.</p>
     *
     * @see #getColor()
     * @see #setColor(Expression)
     */
    protected Expression<R,Color> color;

    /**
     * Level of translucency as a floating point number between 0 and 1 (inclusive), or {@code null} the default value.
     * The default value specified by OGC 05-077r4 standard is 1.
     *
     * <p>In XML documents, this is encoded inside a {@code <SvgParameter name="stroke-opacity">} element.</p>
     *
     * @see #getOpacity()
     * @see #setOpacity(Expression)
     */
    protected Expression<R, ? extends Number> opacity;

    /**
     * Absolute width of the line stroke as a positive floating point number, or {@code null} for the default value.
     * The default value specified by OGC 05-077r4 standard is 1.
     *
     * <p>In XML documents, this is encoded inside a {@code <SvgParameter name="stroke-width">} element.</p>
     *
     * @see #getWidth()
     * @see #setWidth(Expression)
     */
    protected Expression<R, ? extends Number> width;

    /**
     * How the various segments of a (thick) line string should be joined, or {@code null} the default value.
     * The default value is implementation-specific.
     *
     * <p>In XML documents, this is encoded inside a {@code <SvgParameter name="stroke-linejoin">} element.</p>
     *
     * @see #getLineJoin()
     * @see #setLineJoin(Expression)
     */
    protected Expression<R,String> lineJoin;

    /**
     * How the beginning and ending segments of a line string will be terminated, or {@code null} the default value.
     * The default value is implementation-specific.
     *
     * <p>In XML documents, this is encoded inside a {@code <SvgParameter name="stroke-linecap">} element.</p>
     *
     * @see #getLineCap()
     * @see #setLineCap(Expression)
     */
    protected Expression<R,String> lineCap;

    /**
     * Dash pattern as a space-separated sequence of numbers, or {@code null} for a solid line.
     *
     * <p>In XML documents, this is encoded inside a {@code <SvgParameter name="stroke-dasharray">} element.</p>
     *
     * @see #getDashArray()
     * @see #setDashArray(Expression)
     */
    protected Expression<R,float[]> dashArray;

    /**
     * Distance offset into the dash array to begin drawing, or {@code null} for the default value.
     * The default value is zero.
     *
     * <p>In XML documents, this is encoded inside a {@code <SvgParameter name="stroke-dashoffset">} element.</p>
     *
     * @see #getDashOffset()
     * @see #setDashOffset(Expression)
     */
    protected Expression<R,Integer> dashOffset;

    /**
     * For JAXB unmarshalling only.
     */
    private Stroke() {
        // Thread-local factory will be used.
    }

    /**
     * Creates a stroke initialized to solid line of black opaque color, 1 pixel width.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public Stroke(final StyleFactory<R> factory) {
        super(factory);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public Stroke(final Stroke<R> source) {
        super(source);
        graphicFill   = source.graphicFill;
        graphicStroke = source.graphicStroke;
        color         = source.color;
        opacity       = source.opacity;
        width         = source.width;
        lineJoin      = source.lineJoin;
        lineCap       = source.lineCap;
        dashArray     = source.dashArray;
        dashOffset    = source.dashOffset;
    }

    /**
     * Indicates that line should be drawn by tiling the (thin) area of the line with the given graphic.
     * Between {@code getGraphicFill()} and {@link #getGraphicStroke()}, only one may return a non-null value
     * because a {@code Stroke} can have a {@code GraphicFill} or a {@code GraphicStroke}, but not both.
     *
     * <p>The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this stroke, and conversely.</p>
     *
     * @return graphic for tiling the (thin) area of the line.
     *
     * @see Fill#getGraphicFill()
     */
    public Optional<GraphicFill<R>> getGraphicFill() {
        return Optional.ofNullable(graphicFill);
    }

    /**
     * Specifies that line should be drawn by tiling the (thin) area of the line with the given graphic.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is absence.
     *
     * <p>Setting a non-null value causes {@link #getGraphicStroke()} to
     * return {@code null} because those two properties are mutually exclusive.</p>
     *
     * @param  value  new graphic for tiling the (thin) area of the line, or {@code null} if none.
     *
     * @see Fill#setGraphicFill(GraphicFill)
     */
    public void setGraphicFill(final GraphicFill<R> value) {
        graphicFill = value;
        if (value != null) {
            graphicStroke = null;
        }
    }

    /**
     * Indicates that lines should be drawn by repeatedly plotting the given graphic.
     * The graphic is repeated along the path of the lines, rotating it according to the orientation of the line.
     * Between {@link #getGraphicFill()} and {@code getGraphicStroke()}, only one may return a non-null value
     * because a {@code Stroke} can have a {@link GraphicFill} or a {@link GraphicStroke}, but not both.
     *
     * <p>The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this stroke, and conversely.</p>
     *
     * @return graphic to repeat along the path of the lines.
     */
    public Optional<GraphicStroke<R>> getGraphicStroke() {
        return Optional.ofNullable(graphicStroke);
    }

    /**
     * Specifies that lines should be drawn by repeatedly plotting the given graphic.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is absence.
     *
     * <p>Setting a non-null value causes {@link #getGraphicFill()} to
     * return {@code null} because those two properties are mutually exclusive.</p>
     *
     * @param  value  new graphic to repeat along the path of the lines, or {@code null} if none.
     */
    public void setGraphicStroke(final GraphicStroke<R> value) {
        graphicStroke = value;
        if (value != null) {
            graphicFill = null;
        }
    }

    /**
     * Indicates the color of the line if it is to be solid-color filled.
     * This is used when both {@linkplain #getGraphicFill() graphic fill}
     * and {@linkplain #getGraphicStroke() graphic stroke} are null.
     *
     * @return color of the line if it is to be solid-color filled.
     *
     * @see Fill#getColor()
     */
    public Expression<R,Color> getColor() {
        final var value = color;
        return (value != null) ? value : factory.black;
    }

    /**
     * Sets the color of the line if it is to be solid-color filled.
     * If this method is never invoked, then the default value is black.
     * That default value is standardized by OGC 05-077r4.
     *
     * <p>Setting a non-null value clears the {@linkplain #getGraphicFill() graphic fill} and the
     * {@linkplain #getGraphicStroke() graphic stroke} because those three properties are mutually exclusive.</p>
     *
     * @param  value  color of the line if solid-color filled, or {@code null} for resetting the default value.
     *
     * @see Fill#setColor(Expression)
     */
    public void setColor(final Expression<R,Color> value) {
        color = value;
        if (value != null) {
            graphicFill   = null;
            graphicStroke = null;
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
     * @see Fill#getOpacity()
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

    /**
     * Gives the absolute width of the line stroke as a floating point number.
     * The unit of measurement is given by {@link Symbolizer#getUnitOfMeasure()}.
     * Fractional numbers are allowed, but negative numbers are not.
     *
     * @return absolute width of the line stroke as a positive floating point number.
     */
    public Expression<R, ? extends Number> getWidth() {
        return defaultToOne(width);
    }

    /**
     * Sets the absolute width of the line stroke as a floating point number.
     * If this method is never invoked, then the default value 1.0.
     * That default value is standardized by OGC 05-077r4.
     *
     * @param  value  new width of the line stroke, or {@code null} for resetting the default value.
     */
    public void setWidth(final Expression<R, ? extends Number> value) {
        width = value;
    }

    /**
     * Indicates how the various segments of a (thick) line string should be joined.
     * Valid values are "miter", "round", and "bevel".
     *
     * @return how segments of a (thick) line string should be joined.
     */
    public Expression<R,String> getLineJoin() {
        final var value = lineJoin;
        return (value != null) ? value : factory.bevel;
    }

    /**
     * Sets how the various segments of a (thick) line string should be joined.
     * If this method is never invoked, then the default value is literal "bevel".
     * That default value is implementation-specific.
     *
     * @param  value  how segments of a line string should be joined, or {@code null} for resetting the default value.
     */
    public void setLineJoin(final Expression<R,String> value) {
        lineJoin = value;
    }

    /**
     * Indicates how the beginning and ending segments of a line string will be terminated.
     * Valid values are "butt", "round", and "square".
     *
     * @return how the beginning and ending segments of a line string will be terminated.
     */
    public Expression<R,String> getLineCap() {
        final var value = lineCap;
        return (value != null) ? value : factory.square;
    }

    /**
     * Sets how the beginning and ending segments of a line string will be terminated.
     * If this method is never invoked, then the default value is literal "square".
     * That default value is implementation-specific.
     *
     * @param  value  how a line string should be terminated, or {@code null} for resetting the default value.
     */
    public void setLineCap(final Expression<R,String> value) {
        lineCap = value;
    }

    /**
     * Indicates the dash pattern as a space-separated sequence of floating point numbers.
     * The first number represents the length of the first dash to draw.
     * The second number represents the length of space to leave.
     * This continues to the end of the list then repeats.
     * If {@code null}, then lines will be drawn as solid and unbroken.
     *
     * @return dash pattern as a space-separated sequence of numbers, or empty for a solid line.
     */
    public Optional<Expression<R,float[]>> getDashArray() {
        return Optional.ofNullable(dashArray);
    }

    /**
     * Sets the dash pattern as a space-separated sequence of floating point numbers.
     * If this method is never invoked, then the default value is {@code null} (solid line).
     *
     * @param  value  new dash pattern as a space-separated sequence of numbers, or {@code null} for a solid line.
     */
    public void setDashArray(final Expression<R,float[]> value) {
        dashArray = value;
    }

    /**
     * Indicates the distance offset into the dash array to begin drawing.
     *
     * @return distance offset into the dash array to begin drawing.
     */
    public Expression<R,Integer> getDashOffset() {
        final var value = dashOffset;
        return (value != null) ? value : factory.zeroAsInt;
    }

    /**
     * Sets the distance offset into the dash array to begin drawing.
     * If this method is never invoked, then the default value is 0.
     *
     * @param  value  new distance offset into the dash array, or {@code null} for resetting the default value.
     */
    public void setDashOffset(final Expression<R,Integer> value) {
        dashOffset = value;
    }

    /*
     * TODO: we need a private method like below for formatting above SVG parameters:
     *
     *     @XmlElement(name = "SvgParameter")
     *     private List<SvgParameter> svgParameters();
     *
     * Where:
     *
     *     class SvgParameter {
     *         @XmlAttribute(required = true)
     *         private String name;
     *
     *         @XmlMixed
     *         @XmlElementRef(name = "expression", namespace = "http://www.opengis.net/ogc")
     *         private List<Expression<?,?>> content;
     *     }
     *
     * See 05-077r4 §11.1.3.
     */

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {graphicFill, graphicStroke, color, opacity, width, lineJoin, lineCap, dashArray, dashOffset};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public Stroke<R> clone() {
        final var clone = (Stroke<R>) super.clone();
        clone.selfClone();
        return clone;
    }

    /**
     * Clones the mutable style fields of this element.
     */
    private void selfClone() {
        if (graphicFill   != null) graphicFill   = graphicFill.clone();
        if (graphicStroke != null) graphicStroke = graphicStroke.clone();
    }
}
