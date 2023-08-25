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
 * The location inside a graphic or label to use as an "anchor" for positioning it relative to a point.
 * The coordinates are given as (<var>x</var>,<var>y</var>) floating-point numbers between 0 and 1 inclusive.
 * The bounding box of the graphic/label to be rendered is considered to be in a coordinate space from 0
 * (lower-left corner) to 1 (upper-right corner), and the anchor position is specified as a point in this space.
 * The default anchor point is <var>x</var> = 0.5, <var>y</var> = 0.5,
 * which is at the middle height and middle length of the graphic/label.
 *
 * <!-- Following list of authors contains credits to OGC GeoAPI 2 contributors. -->
 * @author  Johann Sorel (Geomatys)
 * @author  Ian Turton (CCG)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @param <R>  the type of data to style, such as {@code Feature} or {@code Coverage}.
 *
 * @since 1.5
 */
@XmlType(name = "AnchorPointType", propOrder = {
    "anchorPointX",
    "anchorPointY"
})
@XmlRootElement(name = "AnchorPoint")
public class AnchorPoint<R> extends StyleElement<R> {
    /**
     * The <var>x</var> coordinate of the anchor point.
     * This property is mandatory.
     *
     * @see #getAnchorPointX()
     * @see #setAnchorPointX(Expression)
     */
    @XmlElement(name = "AnchorPointX", required = true)
    protected Expression<R, ? extends Number> anchorPointX;

    /**
     * The <var>y</var> coordinate of the anchor point.
     * This property is mandatory.
     *
     * @see #getAnchorPointY()
     * @see #setAnchorPointY(Expression)
     */
    @XmlElement(name = "AnchorPointY", required = true)
    protected Expression<R, ? extends Number> anchorPointY;

    /**
     * For JAXB unmarshalling only.
     */
    private AnchorPoint() {
        // Thread-local factory will be used.
    }

    /**
     * Creates an anchor point initialized to <var>x</var> = 0.5 and <var>y</var> = 0.5.
     * This initial position is the center of the graphic/label.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public AnchorPoint(final StyleFactory<R> factory) {
        super(factory);
        anchorPointX = anchorPointY = factory.half;
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public AnchorPoint(final AnchorPoint<R> source) {
        super(source);
        anchorPointX = source.anchorPointX;
        anchorPointY = source.anchorPointY;
    }

    /**
     * Returns the <var>x</var> coordinate of the anchor point.
     * It should be a floating point number between 0 and 1 inclusive.
     *
     * @return the expression fetching the <var>x</var> coordinate.
     */
    public Expression<R, ? extends Number> getAnchorPointX() {
        return anchorPointX;
    }

    /**
     * Sets the <var>x</var> coordinate of the anchor point.
     * If this method is never invoked, then the default value is literal 0.5.
     *
     * @param  value  new <var>x</var> coordinate, or {@code null} for resetting the default value.
     */
    public void setAnchorPointX(final Expression<R, ? extends Number> value) {
        anchorPointX = defaultToHalf(value);
    }

    /**
     * Returns the <var>y</var> coordinate of the anchor point.
     * It should be a floating point number between 0 and 1 inclusive.
     *
     * @return the expression fetching the <var>y</var> coordinate.
     */
    public Expression<R, ? extends Number> getAnchorPointY() {
        return anchorPointY;
    }

    /**
     * Sets the <var>y</var> coordinate of the anchor point.
     * If this method is never invoked, then the default value is literal 0.5.
     *
     * @param  value  new <var>y</var> coordinate, or {@code null} for resetting the default value.
     */
    public void setAnchorPointY(final Expression<R, ? extends Number> value) {
        anchorPointY = defaultToHalf(value);
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {anchorPointX, anchorPointY};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public AnchorPoint<R> clone() {
        return (AnchorPoint<R>) super.clone();
    }
}
