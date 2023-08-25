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
 * Instructions about how to draw on a map the lines of a geometry.
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
@XmlType(name = "LineSymbolizerType", propOrder = {
    "stroke",
    "perpendicularOffset"
})
@XmlRootElement(name = "LineSymbolizer")
public class LineSymbolizer<R> extends Symbolizer<R> {
    /**
     * Information about how to draw lines, or {@code null} for lazily constructed default.
     *
     * @see #getStroke()
     * @see #setStroke(Stroke)
     */
    @XmlElement(name = "Stroke")
    protected Stroke<R> stroke;

    /**
     * Distance to apply for drawing lines in parallel to geometry, or {@code null} for the default value.
     *
     * @see #getPerpendicularOffset()
     * @see #setPerpendicularOffset(Expression)
     */
    @XmlElement(name = "PerpendicularOffset")
    protected Expression<R, ? extends Number> perpendicularOffset;

    /**
     * For JAXB unmarshalling only.
     */
    private LineSymbolizer() {
        // Thread-local factory will be used.
    }

    /**
     * Creates a line symbolizer with the default stroke and no perpendicular offset.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public LineSymbolizer(final StyleFactory<R> factory) {
        super(factory);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public LineSymbolizer(final LineSymbolizer<R> source) {
        super(source);
        stroke = source.stroke;
        perpendicularOffset = source.perpendicularOffset;
    }

    /**
     * Returns the object containing all the information necessary to draw styled lines.
     * The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this symbolizer, and conversely.
     *
     * @return information about how to draw lines.
     */
    public Stroke<R> getStroke() {
        if (stroke == null) {
            stroke = factory.createStroke();
        }
        return stroke;
    }

    /**
     * Sets the object containing all the information necessary to draw styled lines.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is a {@linkplain Stroke#Stroke() default instance}.
     *
     * @param  value  new information about how to draw lines, or {@code null} for resetting the default value.
     */
    public void setStroke(final Stroke<R> value) {
        stroke = value;
    }

    /**
     * Returns a distance to apply for drawing lines in parallel to the original geometry.
     * These parallel lines have to be constructed so that the distance between
     * original geometry and drawn line stays equal.
     * This construction may result in drawn lines that are
     * actually smaller or longer than the original geometry.
     *
     * <p>The distance units of measurement is given by {@link #getUnitOfMeasure()}.
     * The value is positive to the left-hand side of the line string.
     * Negative numbers mean right. The default offset is 0.</p>
     *
     * @return distance to apply for drawing lines in parallel to the original geometry.
     */
    public Expression<R, ? extends Number> getPerpendicularOffset() {
        return defaultToZero(perpendicularOffset);
    }

    /**
     * Sets a distance to apply for drawing lines in parallel to the original geometry.
     * If this method is never invoked, then the default value is literal 0.
     * That default value is standardized by OGC 05-077r4.
     *
     * @param  value  new distance to apply for drawing lines, or {@code null} for resetting the default value.
     */
    public void setPerpendicularOffset(final Expression<R, ? extends Number> value) {
        perpendicularOffset = value;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {perpendicularOffset, stroke};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public LineSymbolizer<R> clone() {
        final var clone = (LineSymbolizer<R>) super.clone();
        clone.selfClone();
        return clone;
    }

    /**
     * Clones the mutable style fields of this element.
     */
    private void selfClone() {
        stroke = stroke.clone();
    }
}
