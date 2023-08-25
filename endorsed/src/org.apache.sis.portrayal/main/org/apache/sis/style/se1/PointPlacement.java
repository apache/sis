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
 * Instructions about how a text label is positioned relative to a point.
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
@XmlType(name = "PointPlacementType", propOrder = {
    "anchorPoint",
    "displacement",
    "rotation"
})
@XmlRootElement(name = "PointPlacement")
public class PointPlacement<R> extends LabelPlacement<R> {
    /**
     * Location to use for anchoring the label to the point, or {@code null} for lazily constructed default.
     *
     * @see #getAnchorPoint()
     * @see #setAnchorPoint(AnchorPoint)
     */
    @XmlElement(name = "AnchorPoint")
    protected AnchorPoint<R> anchorPoint;

    /**
     * Two-dimensional displacements from the "hot-spot" point, or {@code null} for lazily constructed default.
     *
     * @see #getDisplacement()
     * @see #setDisplacement(Displacement)
     */
    @XmlElement(name = "Displacement")
    protected Displacement<R> displacement;

    /**
     * Expression fetching the rotation of the label when it is drawn.
     *
     * @see #getRotation()
     * @see #setRotation(Expression)
     */
    @XmlElement(name = "Rotation")
    protected Expression<R, ? extends Number> rotation;

    /**
     * For JAXB unmarshalling only.
     */
    private PointPlacement() {
        // Thread-local factory will be used.
    }

    /**
     * Creates a point placement initialized to anchor at the middle and no displacement.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public PointPlacement(final StyleFactory<R> factory) {
        super(factory);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public PointPlacement(final PointPlacement<R> source) {
        super(source);
        anchorPoint  = source.anchorPoint;
        displacement = source.displacement;
        rotation     = source.rotation;
    }

    /**
     * Returns the location inside of a label to use for anchoring the label to the point.
     * The coordinates are given as (<var>x</var>,<var>y</var>) floating-point numbers
     * relative the the label bounding box, where (0,0) is the lower-left corner and
     * (1,1) is the upper-right corner.
     *
     * <p>The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this graphic, and conversely.</p>
     *
     * @return the anchor point.
     */
    public AnchorPoint<R> getAnchorPoint() {
        if (anchorPoint == null) {
            anchorPoint = factory.createAnchorPoint();
        }
        return anchorPoint;
    }

    /**
     * Sets the location inside of a label to use for anchoring the label to the point.
     * The given instance is stored by reference, it is not cloned. If this method is never invoked,
     * then the default value is a {@linkplain AnchorPoint#AnchorPoint() default anchor point}.
     *
     * @param  value  new anchor point, or {@code null} for resetting the default value.
     */
    public void setAnchorPoint(final AnchorPoint<R> value) {
        anchorPoint = value;
    }

    /**
     * Returns the two-dimensional displacements from the "hot-spot" point.
     * The displacements are in units of pixels above and to the right of the point.
     *
     * @return two-dimensional displacements from the "hot-spot" point.
     *
     * @see Graphic#getDisplacement()
     * @see PolygonSymbolizer#getDisplacement()
     */
    public Displacement<R> getDisplacement() {
        if (displacement == null) {
            displacement = factory.createDisplacement();
        }
        return displacement;
    }

    /**
     * Sets the two-dimensional displacements from the "hot-spot" point.
     * The given instance is stored by reference, it is not cloned. If this method is never invoked,
     * then the default value is a {@linkplain Displacement#Displacement() default displacement}.
     *
     * @param  value  new displacements from the "hot-spot" point, or {@code null} for resetting the default value.
     */
    public void setDisplacement(final Displacement<R> value) {
        displacement = value;
    }

    /**
     * Returns the expression fetching the rotation of the label when it is drawn.
     * This is the clockwise rotation of the label in degrees from the normal direction for a font
     * (left-to-right for Latin languages).
     *
     * @return rotation of the label when it is drawn.
     */
    public Expression<R, ? extends Number> getRotation() {
        return defaultToZero(rotation);
    }

    /**
     * Sets the expression fetching the rotation of the label when it is drawn.
     * If this method is never invoked, then the default value is literal zero.
     *
     * @param  value  new rotation of the label, or {@code null} for resetting the default value.
     */
    public void setRotation(final Expression<R, ? extends Number> value) {
        rotation = value;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {anchorPoint, displacement, rotation};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public PointPlacement<R> clone() {
        final var clone = (PointPlacement<R>) super.clone();
        clone.selfClone();
        return clone;
    }

    /**
     * Clones the mutable style fields of this element.
     */
    private void selfClone() {
        if (anchorPoint  != null) anchorPoint  = anchorPoint .clone();
        if (displacement != null) displacement = displacement.clone();
    }
}
