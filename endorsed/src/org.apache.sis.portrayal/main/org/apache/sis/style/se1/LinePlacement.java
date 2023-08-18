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

// Branch-dependent imports
import org.apache.sis.filter.Expression;


/**
 * Instructions about where and how a text label should be rendered relative to a line.
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
@XmlType(name = "LinePlacementType", propOrder = {
    "perpendicularOffset",
    "isRepeated",
    "initialGap",
    "gap",
    "isAligned",
    "generalizeLine"
})
@XmlRootElement(name = "LinePlacement")
public class LinePlacement<R> extends LabelPlacement<R> {
    /**
     * Perpendicular distance away from a line where to draw a label, or {@code null} for the default value.
     *
     * @see #getPerpendicularOffset()
     * @see #setPerpendicularOffset(Expression)
     */
    @XmlElement(name = "PerpendicularOffset")
    protected Expression<R, ? extends Number> perpendicularOffset;

    /**
     * Whether the label will be repeatedly drawn along the line, or {@code null} for the default value.
     *
     * @see #isRepeated()
     * @see #setRepeated(Expression)
     *
     * @todo Needs an adapter from expression to plain boolean.
     */
    @XmlElement(name = "IsRepeated")
    protected Expression<R,Boolean> isRepeated;

    /**
     * How far away the first label will be drawn, or {@code null} for the default value.
     *
     * @see #getInitialGap()
     * @see #setInitialGap(Expression)
     */
    @XmlElement(name = "InitialGap")
    protected Expression<R, ? extends Number> initialGap;

    /**
     * Distance between two labels, or {@code null} for the default value.
     *
     * @see #getGap()
     * @see #setGap(Expression)
     */
    @XmlElement(name = "Gap")
    protected Expression<R, ? extends Number> gap;

    /**
     * Whether labels are aligned to the line geometry, or {@code null} for the default value.
     * If false, labels are drawn horizontally.
     *
     * @see #isAligned()
     * @see #setAligned(Expression)
     *
     * @todo Needs an adapter from expression to plain boolean.
     */
    @XmlElement(name = "IsAligned")
    protected Expression<R,Boolean> isAligned;

    /**
     * Whether to allow the geometry to be generalized, or {@code null} for the default value.
     *
     * @see #getGeneralizeLine()
     * @see #setGeneralizeLine(Expression)
     *
     * @todo Needs an adapter from expression to plain boolean.
     */
    @XmlElement(name = "GeneralizeLine")
    protected Expression<R,Boolean> generalizeLine;

    /**
     * For JAXB unmarshalling only.
     */
    private LinePlacement() {
        // Thread-local factory will be used.
    }

    /**
     * Creates a line placement initialized to no offset, no repetition and no gap.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public LinePlacement(final StyleFactory<R> factory) {
        super(factory);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public LinePlacement(final LinePlacement<R> source) {
        super(source);
        perpendicularOffset = source.perpendicularOffset;
        isRepeated          = source.isRepeated;
        initialGap          = source.initialGap;
        gap                 = source.gap;
        isAligned           = source.isAligned;
        generalizeLine      = source.generalizeLine;
    }

    /**
     * Returns a perpendicular distance away from a line where to draw a label.
     * The distance units of measurement is given by {@link Symbolizer#getUnitOfMeasure()}.
     * The value is positive to the left-hand side of the line string.
     * Negative numbers mean right. The default offset is 0.
     *
     * @return perpendicular distance away from a line where to draw a label.
     */
    public Expression<R, ? extends Number> getPerpendicularOffset() {
        return defaultToZero(perpendicularOffset);
    }

    /**
     * Sets a perpendicular distance away from a line where to draw a label.
     * If this method is never invoked, then the default value is literal 0.
     * That default value is standardized by OGC 05-077r4.
     *
     * @param  value  new distance to apply for drawing label, or {@code null} for resetting the default value.
     */
    public void setPerpendicularOffset(final Expression<R, ? extends Number> value) {
        perpendicularOffset = value;
    }

    /**
     * Returns whether the label will be repeatedly drawn along the line.
     * If {@code true}, the repetition will use the {@linkplain #getInitialGap() initial gap}
     * and {@linkplain #getGap() gap} for defining the spaces at the beginning and between labels.
     *
     * @return whether the label will be repeatedly drawn along the line.
     */
    public Expression<R,Boolean> isRepeated() {
        return defaultToFalse(isRepeated);
    }

    /**
     * Sets whether the label will be repeatedly drawn along the line.
     * If this method is never invoked, then the default value is literal false.
     *
     * @param  value  whether the label will be repeated, or {@code null} for resetting the default value.
     */
    public void setRepeated(final Expression<R,Boolean> value) {
        isRepeated = value;
    }

    /**
     * Returns how far away the first label will be drawn relative to the start of the rendering line.
     *
     * @return distance of first label relative to the rendering start.
     */
    public Expression<R, ? extends Number> getInitialGap() {
        return defaultToZero(initialGap);
    }

    /**
     * Sets how far away the first label will be drawn relative to the start of the rendering line.
     * If this method is never invoked, then the default value is literal 0.
     *
     * @param  value  new distance relative to rendering start, or {@code null} for resetting the default value.
     */
    public void setInitialGap(final Expression<R, ? extends Number> value) {
        initialGap = value;
    }

    /**
     * Returns the distance between two labels.
     *
     * @return distance between two labels.
     */
    public Expression<R, ? extends Number> getGap() {
        return defaultToZero(gap);
    }

    /**
     * Sets the distance between two labels.
     * If this method is never invoked, then the default value is literal 0.
     *
     * @param  value  new distance between two labels, or {@code null} for resetting the default value.
     */
    public void setGap(final Expression<R, ? extends Number> value) {
        gap = value;
    }

    /**
     * Returns whether labels are aligned to the line geometry or drawn horizontally.
     *
     * @return whether labels are aligned to the line geometry or drawn horizontally.
     */
    public Expression<R,Boolean> isAligned() {
        return defaultToTrue(isAligned);
    }

    /**
     * Sets whether labels are aligned to the line geometry or drawn horizontally.
     * If this method is never invoked, then the default value is literal true.
     * That default value is standardized by OGC 05-077r4.
     *
     * @param  value  whether labels are aligned to the line geometry, or {@code null} for resetting the default value.
     */
    public void setAligned(final Expression<R,Boolean> value) {
        isAligned = value;
    }

    /**
     * Returns whether to allow the geometry to be generalized for label placement.
     *
     * @return whether to allow the geometry to be generalized for label placement.
     */
    public Expression<R,Boolean> getGeneralizeLine() {
        return defaultToFalse(generalizeLine);
    }

    /**
     * Sets whether to allow the geometry to be generalized for label placement.
     * If this method is never invoked, then the default value is literal false.
     *
     * @param  value whether to allow the geometry to be generalized, or {@code null} for resetting the default value.
     */
    public void setGeneralizeLine(final Expression<R,Boolean> value) {
        generalizeLine = value;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {perpendicularOffset, isRepeated, initialGap, gap, isAligned, generalizeLine};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public LinePlacement<R> clone() {
        return (LinePlacement<R>) super.clone();
    }
}
