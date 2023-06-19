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

import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;


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
 * @since   1.5
 */
@XmlType(name = "AnchorPointType", propOrder = {
    "anchorPointX",
    "anchorPointY"
})
@XmlRootElement(name = "AnchorPoint")
public class AnchorPoint extends StyleElement {
    /**
     * Literal used as default value.
     */
    private static final Literal<Feature,Double> LITERAL_HALF = literal(0.5);;

    /**
     * The <var>x</var> coordinate of the anchor point.
     * This property is mandatory.
     *
     * @see #getAnchorPointX()
     * @see #setAnchorPointX(Expression)
     */
    @XmlElement(name = "AnchorPointX", required = true)
    protected Expression<Feature, ? extends Number> anchorPointX;

    /**
     * The <var>y</var> coordinate of the anchor point.
     * This property is mandatory.
     *
     * @see #getAnchorPointY()
     * @see #setAnchorPointY(Expression)
     */
    @XmlElement(name = "AnchorPointY", required = true)
    protected Expression<Feature, ? extends Number> anchorPointY;

    /**
     * Creates a anchor point initialized to <var>x</var> = 0.5 and <var>y</var> = 0.5.
     * This initial position is the center of the graphic/label.
     */
    public AnchorPoint() {
        anchorPointX = LITERAL_HALF;
        anchorPointY = LITERAL_HALF;
    }

    /**
     * Creates a new anchor point initialized to the given position.
     *
     * @param  x  the initial <var>x</var> position.
     * @param  y  the initial <var>y</var> position.
     */
    public AnchorPoint(final double x, final double y) {
        anchorPointX = literal(x);
        anchorPointY = literal(y);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public AnchorPoint(final AnchorPoint source) {
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
    public Expression<Feature, ? extends Number> getAnchorPointX() {
        return anchorPointX;
    }

    /**
     * Sets the <var>x</var> coordinate of the anchor point.
     * If this method is never invoked, then the default value is literal 0.5.
     *
     * @param  value  new <var>x</var> coordinate, or {@code null} for resetting the default value.
     */
    public void setAnchorPointX(final Expression<Feature, ? extends Number> value) {
        anchorPointX = (value != null) ? value : LITERAL_HALF;
    }

    /**
     * Returns the <var>y</var> coordinate of the anchor point.
     * It should be a floating point number between 0 and 1 inclusive.
     *
     * @return the expression fetching the <var>y</var> coordinate.
     */
    public Expression<Feature, ? extends Number> getAnchorPointY() {
        return anchorPointY;
    }

    /**
     * Sets the <var>y</var> coordinate of the anchor point.
     * If this method is never invoked, then the default value is literal 0.5.
     *
     * @param  value  new <var>y</var> coordinate, or {@code null} for resetting the default value.
     */
    public void setAnchorPointY(final Expression<Feature, ? extends Number> value) {
        anchorPointY = (value != null) ? value : LITERAL_HALF;
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
    public AnchorPoint clone() {
        final var clone = (AnchorPoint) super.clone();
        return clone;
    }
}
