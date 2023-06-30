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
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;


/**
 * A two-dimensional displacements from the original geometry.
 * Displacements may be used to avoid over-plotting, or for supplying shadows.
 * The displacements units depend on the context:
 * in {@linkplain Symbolizer#getUnitOfMeasure() symbolizer unit of measurements}
 * when the displacement is applied by a {@link PolygonSymbolizer},
 * but in pixels when the displacement is applied by a {@link TextSymbolizer}.
 * The displacements are to the right of the point.
 * The default displacement is <var>x</var> = 0, <var>y</var> = 0,
 *
 * <!-- Following list of authors contains credits to OGC GeoAPI 2 contributors. -->
 * @author  Johann Sorel (Geomatys)
 * @author  Ian Turton (CCG)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 *
 * @see PointPlacement#getDisplacement()
 * @see Graphic#getDisplacement()
 * @see PolygonSymbolizer#getDisplacement()
 */
@XmlType(name = "DisplacementType", propOrder = {
    "displacementX",
    "displacementY"
})
@XmlRootElement(name = "Displacement")
public class Displacement extends StyleElement {
    /**
     * The <var>x</var> offset from the geometry point.
     * This property is mandatory.
     *
     * @see #getDisplacementX()
     * @see #setDisplacementX(Expression)
     */
    @XmlElement(name = "DisplacementX", required = true)
    protected Expression<Feature, ? extends Number> displacementX;

    /**
     * The <var>y</var> offset from the geometry point.
     * This property is mandatory.
     *
     * @see #getDisplacementY()
     * @see #setDisplacementY(Expression)
     */
    @XmlElement(name = "DisplacementY", required = true)
    protected Expression<Feature, ? extends Number> displacementY;

    /**
     * Creates a displacement initialized to zero offsets.
     */
    public Displacement() {
        displacementX = LITERAL_ZERO;
        displacementY = LITERAL_ZERO;
    }

    /**
     * Creates a new displacement initialized to the given offsets.
     *
     * @param  x  the initial <var>x</var> displacement.
     * @param  y  the initial <var>y</var> displacement.
     */
    public Displacement(final double x, final double y) {
        displacementX = literal(x);
        displacementY = literal(y);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public Displacement(final Displacement source) {
        super(source);
        displacementX = source.displacementX;
        displacementY = source.displacementY;
    }

    /**
     * Returns an expression fetching an <var>x</var> offset from the geometry point.
     *
     * @return <var>x</var> offset from the geometry point.
     */
    public Expression<Feature, ? extends Number> getDisplacementX() {
        return displacementX;
    }

    /**
     * Sets the expression fetching an <var>x</var> offset.
     * If this method is never invoked, then the default value is literal 0.
     *
     * @param  value  new <var>x</var> offset, or {@code null} for resetting the default value.
     */
    public void setDisplacementX(final Expression<Feature, ? extends Number> value) {
        displacementX = defaultToZero(value);
    }

    /**
     * Returns an expression fetching an <var>y</var> offset from the geometry point.
     *
     * @return <var>y</var> offset from the geometry point.
     */
    public Expression<Feature, ? extends Number> getDisplacementY() {
        return displacementY;
    }

    /**
     * Sets the expression fetching an <var>y</var> offset.
     * If this method is never invoked, then the default value is literal 0.
     *
     * @param  value  new <var>y</var> offset, or {@code null} for resetting the default value.
     */
    public void setDisplacementY(final Expression<Feature, ? extends Number> value) {
        displacementY = defaultToZero(value);
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {displacementX, displacementY};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public Displacement clone() {
        final var clone = (Displacement) super.clone();
        return clone;
    }
}
