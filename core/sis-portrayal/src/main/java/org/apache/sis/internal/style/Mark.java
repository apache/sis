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

import java.util.Optional;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;


/**
 * Predefined shapes that can be drawn at the points of the geometry.
 * This is an alternative to {@link ExternalGraphic} for
 * {@linkplain Graphic#graphicalSymbols graphical symbols}.
 * When marks are provided in the bottom of the graphical symbol list,
 * it allows a style to be specified that can produce a usable result in a best-effort basis.
 *
 * <!-- Following list of authors contains credits to OGC GeoAPI 2 contributors. -->
 * @author  Johann Sorel (Geomatys)
 * @author  Chris Dillard (SYS Technologies)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
@XmlType(name = "MarkType", propOrder = {
    "wellKnownName",
//  "onlineResource",       // XML encoding not yet available.
//  "inlineContent",        // Idem.
    "format",
    "markIndex",
    "fill",
    "stroke"
})
@XmlRootElement(name = "Mark")
public class Mark extends GraphicalSymbol {
    /**
     * Literal for a predefined well-known name which can be used as a mark.
     */
    public static final Literal<Feature,String> SQUARE, CIRCLE, TRIANGLE, STAR, CROSS, X;
    static {
        final var FF = FF();
        SQUARE   = FF.literal("square");
        CIRCLE   = FF.literal("circle");
        TRIANGLE = FF.literal("triangle");
        STAR     = FF.literal("star");
        CROSS    = FF.literal("cross");
        X        = FF.literal("x");
    }

    /**
     * Expression whose value will indicate the symbol to draw, or {@code null} for the default value.
     *
     * @see #getWellKnownName()
     * @see #setWellKnownName(Expression)
     */
    @XmlElement(name = "WellKnownName")
    protected Expression<Feature,String> wellKnownName;

    /**
     * Information about how the interior of marks should be filled, or {@code null} for no fill.
     * If no value has been explicitly set (including null value),
     * then a default fill will be lazily created when first requested.
     *
     * @see #getFill()
     * @see #setFill(Fill)
     */
    @XmlElement(name = "Fill")
    protected Fill fill;

    /**
     * Whether {@link #fill} has been explicitly set to some value, including null.
     * If {@code false}, then a default fill will be created when first needed.
     */
    private boolean isFillSet;

    /**
     * Information about styled lines, or {@code null} if mark lines should not be drawn.
     * If no value has been explicitly set (including null value),
     * then a default stroke will be lazily created when first needed.
     *
     * @see #getStroke()
     * @see #setStroke(Stroke)
     */
    @XmlElement(name = "Stroke")
    protected Stroke stroke;

    /**
     * Whether {@link #stroke} has been explicitly set to some value, including null.
     * If {@code false}, then a default stroke will be created when first requested.
     */
    private boolean isStrokeSet;

    /**
     * Individual mark to select in a mark archive, or {@code null} if none.
     *
     * @see #getMarkIndex()
     * @see #setMarkIndex(Expression)
     */
    @XmlElement(name = "MarkIndex")
    protected Expression<Feature,Integer> markIndex;

    /**
     * Invoked by JAXB before unmarshalling this mark.
     * OGC 05-077r4 said that if the fill or the stroke is not specified,
     * then no fill or stroke should be applied.
     */
    private void beforeUnmarshal(Unmarshaller caller, Object parent) {
        isFillSet   = true;
        isStrokeSet = true;
    }

    /**
     * Invoked by JAXB before marshalling this mark.
     * Creates the default fill and stroke if needed.
     */
    private void beforeMarshal(Marshaller caller) {
        if (fill   == null && !isFillSet)   fill   = new Fill();
        if (stroke == null && !isStrokeSet) stroke = new Stroke();
    }

    /**
     * Creates a mark initialized to a gray square with black outline.
     * The size is specified by {@link Graphic#getSize()} and should be 6 pixels by default.
     */
    public Mark() {
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public Mark(final Mark source) {
        super(source);
        wellKnownName = source.wellKnownName;
        fill          = source.fill;
        stroke        = source.stroke;
    }

    /**
     * Returns the expression whose value will indicate the symbol to draw.
     * Allowed values include at least "square", "circle", "triangle", "star", "cross", and "x".
     * Renderings of these marks may be made solid or hollow depending on
     * {@linkplain #getFill() fill} and {@linkplain #getStroke() stroke} elements.
     *
     * <p>The well-known name may be ignored if the mark is also provided
     * by {@linkplain #getInlineContent() inline content}
     * or {@linkplain #getOnlineResource() online resource}.</p>
     *
     * @return well-known name of the mark to render.
     *
     * @see #getOnlineResource()
     * @see #getInlineContent()
     */
    public Expression<Feature,String> getWellKnownName() {
        final var value = wellKnownName;
        return (value != null) ? value : SQUARE;
    }

    /**
     * Sets the expression whose value will indicate the symbol to draw.
     * If this method is never invoked, then the default value is {@link #SQUARE}.
     *
     * @param  value  well-known name of the mark to render, or {@code null} for resetting the default.
     */
    public void setWellKnownName(final Expression<Feature,String> value) {
        wellKnownName = value;
    }

    /**
     * Returns the object that indicates how the mark should be filled.
     * If absent, then the marks are not to be filled at all.
     *
     * <p>The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this mark, and conversely.</p>
     *
     * @return information about how the interior of polygons should be filled.
     *
     * @see #getStroke()
     */
    public Optional<Fill> getFill() {
        if (!isFillSet) {
            isFillSet = true;
            fill = new Fill();
        }
        return Optional.ofNullable(fill);
    }

    /**
     * Sets information about how the interior of marks should be filled.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is the {@linkplain Fill#Fill() default fill}.
     *
     * @param  value  new information about the fill, or {@code null} for no fill.
     */
    public void setFill(final Fill value) {
        isFillSet = true;
        fill = value;
    }

    /**
     * Returns the object that indicates how the edges of the mark will be drawn.
     * This is used for the edges of marks.
     * Absent means that the edges will not be drawn at all.
     *
     * <p>The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this mark, and conversely.</p>
     *
     * @return information about styled lines, or {@code null} if lines should not be drawn.
     *
     * @see #getFill()
     */
    public Optional<Stroke> getStroke() {
        if (!isStrokeSet) {
            isStrokeSet = true;
            stroke = new Stroke();
        }
        return Optional.ofNullable(stroke);
    }

    /**
     * Sets information about styled lines.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is the {@linkplain Stroke#Stroke() default stroke}.
     *
     * @param  value  new information about styled lines, or {@code null} if lines should not be drawn.
     */
    public void setStroke(final Stroke value) {
        isStrokeSet = true;
        stroke = value;
    }

    /**
     * Returns an individual mark to select in a mark archive.
     * For example it can be the index of a glyph to select in a TrueType fond file.
     *
     * @return individual mark to select in a mark archive.
     */
    public Optional<Expression<Feature,Integer>> getMarkIndex() {
        return Optional.ofNullable(markIndex);
    }

    /**
     * Sets an individual mark to select in a mark archive.
     *
     * @param  value  new index of an individual mark to select, or {@code null} if none.
     */
    public void setMarkIndex(final Expression<Feature,Integer> value) {
        markIndex = value;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {wellKnownName, fill, isFillSet, stroke, isStrokeSet, markIndex};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public Mark clone() {
        final var clone = (Mark) super.clone();
        clone.selfClone();
        return clone;
    }

    /**
     * Clones the mutable style fields of this element.
     */
    private void selfClone() {
        if (fill   != null) fill   = fill.clone();
        if (stroke != null) stroke = stroke.clone();
    }
}
