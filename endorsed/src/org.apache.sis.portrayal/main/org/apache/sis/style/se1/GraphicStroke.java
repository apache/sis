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

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;


/**
 * Repeated-linear-graphic stroke.
 * A graphic stroke can only be used by a {@link Stroke}.
 * This class contains a {@link Graphic} property,
 * but its encapsulation in {@code GraphicStroke} means that
 * the graphic should be bent around the curves of the line string.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @param <R>  the type of data to style, such as {@code Feature} or {@code Coverage}.
 *
 * @since 1.5
 */
@XmlType(name = "GraphicStrokeType", propOrder = {
    "graphic",
    "initialGap",
    "gap"
})
@XmlRootElement(name = "GraphicStroke")
public class GraphicStroke<R> extends StyleElement<R> implements GraphicalElement<R> {
    /**
     * The graphic to be repeated, or {@code null} for lazily constructed default.
     * This property is mandatory: a null value <em>shall</em> be replaced by a default value when first requested.
     *
     * @see #getGraphic()
     * @see #setGraphic(Graphic)
     */
    protected Graphic<R> graphic;

    /**
     * How far away the first graphic will be drawn, or {@code null} for the default value.
     *
     * @see #getInitialGap()
     * @see #setInitialGap(Expression)
     */
    @XmlElement(name = "InitialGap")
    protected Expression<R, ? extends Number> initialGap;

    /**
     * Distance between two graphics, or {@code null} for the default value.
     *
     * @see #getGap()
     * @see #setGap(Expression)
     */
    @XmlElement(name = "Gap")
    protected Expression<R, ? extends Number> gap;

    /**
     * For JAXB unmarshalling only.
     */
    private GraphicStroke() {
        // Thread-local factory will be used.
    }

    /**
     * Creates a graphic stroke initialized to a default graphic and no gap.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public GraphicStroke(final StyleFactory<R> factory) {
        super(factory);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public GraphicStroke(final GraphicStroke<R> source) {
        super(source);
        graphic    = source.graphic;
        initialGap = source.initialGap;
        gap        = source.gap;
    }

    /**
     * Returns the graphic to be repeated.
     * The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this stroke, and conversely.
     *
     * @return the graphic to be repeated.
     *
     * @see GraphicFill#getGraphic()
     */
    @Override
    @XmlElement(name = "Graphic", required = true)
    public final Graphic<R> getGraphic() {
        if (graphic == null) {
            graphic = factory.createGraphic();
        }
        return graphic;
    }

    /**
     * Sets the graphic to be repeated.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is a {@linkplain Graphic#Graphic() default instance}.
     *
     * @param  value  new graphic, or {@code null} for resetting the default value.
     *
     * @see GraphicFill#setGraphic(Graphic)
     */
    @Override
    public final void setGraphic(final Graphic<R> value) {
        graphic = value;
    }

    /**
     * Returns how far away the first graphic will be drawn relative to the start of the rendering line.
     *
     * @return distance of first graphic relative to the rendering start.
     */
    public Expression<R, ? extends Number> getInitialGap() {
        return defaultToZero(initialGap);
    }

    /**
     * Sets how far away the first graphic will be drawn relative to the start of the rendering line.
     * If this method is never invoked, then the default value is literal 0.
     *
     * @param  value  new distance relative to rendering start, or {@code null} for resetting the default value.
     */
    public void setInitialGap(final Expression<R, ? extends Number> value) {
        initialGap = value;
    }

    /**
     * Returns the distance between two graphics.
     *
     * @return distance between two graphics.
     */
    public Expression<R, ? extends Number> getGap() {
        return defaultToZero(gap);
    }

    /**
     * Sets the distance between two graphics.
     * If this method is never invoked, then the default value is literal 0.
     *
     * @param  value  new distance between two graphics, or {@code null} for resetting the default value.
     */
    public void setGap(final Expression<R, ? extends Number> value) {
        gap = value;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {graphic, initialGap, gap};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public GraphicStroke<R> clone() {
        final var clone = (GraphicStroke<R>) super.clone();
        clone.selfClone();
        return clone;
    }

    /**
     * Clones the mutable style fields of this element.
     */
    private void selfClone() {
        if (graphic != null) graphic = graphic.clone();
    }
}
