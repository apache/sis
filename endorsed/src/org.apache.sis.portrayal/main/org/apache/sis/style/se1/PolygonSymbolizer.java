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

import java.util.Optional;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

// Specific to the main branch:
import org.apache.sis.filter.Expression;


/**
 * Instructions about how to draw on a map the lines and the interior of polygons.
 * Holes are not filled but borders around the holes are stroked.
 * Islands within holes are filled and stroked, and so on.
 *
 * <p>If the geometry is not a polygon, then
 * line strings should be closed for filling but not for stroking.
 * Points should be rendered as small squares, and
 * rasters should be rendered using the coverage extent as the polygon.</p>
 *
 * <p>The fill should be rendered first, then the stroke should be rendered on top of the fill.
 * A missing stroke element means that the geometry will not be stroked.</p>
 *
 * <!-- Following list of authors contains credits to OGC GeoAPI 2 contributors. -->
 * @author  Johann Sorel (Geomatys)
 * @author  Chris Dillard (SYS Technologies)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param <R>  the type of data to style, such as {@code Feature} or {@code Coverage}.
 */
@XmlType(name = "PolygonSymbolizerType", propOrder = {
    "fill",
    "stroke",
    "displacement",
    "perpendicularOffset"
})
@XmlRootElement(name = "PolygonSymbolizer")
public class PolygonSymbolizer<R> extends Symbolizer<R> {
    /**
     * Information about how the interior of polygons should be filled, or {@code null} for no fill.
     * If no value has been explicitly set (including null value),
     * then a default fill will be lazily created when first requested.
     *
     * @see #getFill()
     * @see #setFill(Fill)
     */
    @XmlElement(name = "Fill")
    protected Fill<R> fill;

    /**
     * Whether {@link #fill} has been explicitly set to some value, including null.
     * If {@code false}, then a default fill will be created when first needed.
     */
    private boolean isFillSet;

    /**
     * Information about styled lines, or {@code null} if lines should not be drawn.
     * If no value has been explicitly set (including null value),
     * then a default stroke will be lazily created when first needed.
     *
     * @see #getStroke()
     * @see #setStroke(Stroke)
     */
    @XmlElement(name = "Stroke")
    protected Stroke<R> stroke;

    /**
     * Whether {@link #stroke} has been explicitly set to some value, including null.
     * If {@code false}, then a default stroke will be created when first requested.
     */
    private boolean isStrokeSet;

    /**
     * Displacement from the "hot-spot" point, or {@code null} for lazily constructed default.
     *
     * @see #getDisplacement()
     * @see #setDisplacement(Displacement)
     */
    @XmlElement(name = "Displacement")
    protected Displacement<R> displacement;

    /**
     * Distance to apply for drawing lines in parallel to geometry, or {@code null} for the default value.
     *
     * @see #getPerpendicularOffset()
     * @see #setPerpendicularOffset(Expression)
     */
    @XmlElement(name = "PerpendicularOffset")
    protected Expression<R, ? extends Number> perpendicularOffset;

    /**
     * Invoked by JAXB before marshalling this polyogon symbolizer.
     * Creates the default fill and stroke if needed.
     */
    private void beforeMarshal(Marshaller caller) {
        if (fill   == null && !isFillSet)   fill   = factory.createFill();
        if (stroke == null && !isStrokeSet) stroke = factory.createStroke();
    }

    /**
     * For JAXB unmarshalling only. This constructor disables the lazy creation of default values.
     * This is because OGC 05-077r4 said that if the fill or the stroke is not specified,
     * then no fill or stroke should be applied.
     */
    private PolygonSymbolizer() {
        // Thread-local factory will be used.
        isFillSet   = true;
        isStrokeSet = true;
    }

    /**
     * Creates a polygon symbolizer initialized to the default fill and default stroke.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public PolygonSymbolizer(final StyleFactory<R> factory) {
        super(factory);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public PolygonSymbolizer(final PolygonSymbolizer<R> source) {
        super(source);
        fill                = source.fill;
        stroke              = source.stroke;
        isFillSet           = source.isFillSet;
        isStrokeSet         = source.isStrokeSet;
        displacement        = source.displacement;
        perpendicularOffset = source.perpendicularOffset;
    }

    /**
     * Returns information about how the interior of polygons should be filled.
     * If absent, then the polygons are not to be filled at all.
     *
     * <p>The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this stroke, and conversely.</p>
     *
     * @return information about how the interior of polygons should be filled.
     *
     * @see #getStroke()
     * @see #isVisible()
     */
    public Optional<Fill<R>> getFill() {
        if (!isFillSet) {
            isFillSet = true;
            fill = factory.createFill();
        }
        return Optional.ofNullable(fill);
    }

    /**
     * Sets information about how the interior of polygons should be filled.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is the {@linkplain Fill#Fill() default fill}.
     *
     * @param  value  new information about the fill, or {@code null} for no fill.
     */
    public void setFill(final Fill<R> value) {
        isFillSet = true;
        fill = value;
    }

    /**
     * Returns information about styled lines.
     * This is used for the edges of polygons.
     * Absent means that lines should not be drawn
     *
     * <p>The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this stroke, and conversely.</p>
     *
     * @return information about styled lines.
     *
     * @see #getFill()
     * @see #isVisible()
     */
    public Optional<Stroke<R>> getStroke() {
        if (!isStrokeSet) {
            isStrokeSet = true;
            stroke = factory.createStroke();
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
    public void setStroke(final Stroke<R> value) {
        isStrokeSet = true;
        stroke = value;
    }

    /**
     * Returns the two-dimensional displacement from the "hot-spot" point.
     * This element may be used to avoid over-plotting of multiple polygons for one geometry.
     * It may also be used for creating shadows of polygon geometries.
     * The unit of measurement is given by {@link Symbolizer#getUnitOfMeasure()}.
     * Positive values are to the right of the point.
     * The default displacement is <var>x</var> = 0, <var>y</var> = 0,
     *
     * <p>The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this stroke, and conversely.</p>
     *
     * @return displacement from the "hot-spot" point.
     *
     * @see Graphic#getDisplacement()
     * @see PointPlacement#getDisplacement()
     */
    public Displacement<R> getDisplacement() {
        if (displacement == null) {
            displacement = factory.createDisplacement();
        }
        return displacement;
    }

    /**
     * Sets the two-dimensional displacement from the "hot-spot" point.
     * The given instance is stored by reference, it is not cloned. If this method is never invoked,
     * then the default value is a {@linkplain Displacement#Displacement() default displacement}.
     *
     * @param  value  new displacement from the "hot-spot" point, or {@code null} for resetting the default value.
     */
    public void setDisplacement(final Displacement<R> value) {
        displacement = value;
    }

    /**
     * Returns a distance to apply for drawing lines in parallel to the original polygon.
     * This property allow to draw polygons smaller or larger than their actual geometry.
     * The distance units of measurement is given by {@link #getUnitOfMeasure()}.
     * The value is positive outside the polygon.
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
     * Returns {@code true} if this symbolizer has a fill and/or a stroke.
     * If both {@code setFill(null)} and {@code setStroke(null)} have been
     * explicitly invoked with a null argument value, then this method returns {@code false}.
     *
     * @return whether this symbolizer has a fill and/or a stroke.
     *
     * @see #setFill(Fill)
     * @see #setStroke(Stroke)
     */
    @Override
    public boolean isVisible() {
        return fill != null || stroke != null || !(isFillSet | isStrokeSet);
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {fill, isFillSet, stroke, isStrokeSet, displacement, perpendicularOffset};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public PolygonSymbolizer<R> clone() {
        final var clone = (PolygonSymbolizer<R>) super.clone();
        clone.selfClone();
        return clone;
    }

    /**
     * Clones the mutable style fields of this element.
     */
    private void selfClone() {
        if (stroke       != null) stroke       = stroke.clone();
        if (fill         != null) fill         = fill.clone();
        if (displacement != null) displacement = displacement.clone();
    }
}
