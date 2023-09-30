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

import java.util.List;
import java.util.ArrayList;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;


/**
 * A "graphic symbol" with an inherent shape, color(s), and possibly size.
 * A "graphic" can be very informally defined as "a little picture"
 * and can be of either a raster or vector-graphic source type.
 *
 * <!-- Following list of authors contains credits to OGC GeoAPI 2 contributors. -->
 * @author  Johann Sorel (Geomatys)
 * @author  Chris Dillard (SYS Technologies)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param <R>  the type of data to style, such as {@code Feature} or {@code Coverage}.
 */
@XmlType(name = "GraphicType", propOrder = {
    "graphicalSymbols",
    "opacity",
    "size",
    "rotation",
    "anchorPoint",
    "displacement"
})
@XmlRootElement(name = "Graphic")
public class Graphic<R> extends StyleElement<R> implements Translucent<R> {
    /**
     * List of external image files or marks that comprise this graphic.
     * All elements of the list must be instances of either {@link Mark} or {@link ExternalGraphic}.
     * If empty it is to be treated a single default mark.
     *
     * @see #graphicalSymbols()
     */
    @XmlElements({
        @XmlElement(name = "Mark", type = Mark.class),
        @XmlElement(name = "ExternalGraphic", type = ExternalGraphic.class)
    })
    private List<GraphicalSymbol<R>> graphicalSymbols;

    /**
     * Level of translucency as a floating point number, or {@code null} for the default value.
     *
     * @see #getOpacity()
     * @see #setOpacity(Expression)
     *
     * @todo Needs a JAXB adapter to {@code ParameterValueType}.
     */
    @XmlElement(name = "Opacity")
    protected Expression<R, ? extends Number> opacity;

    /**
     * Absolute size of the graphic as a floating point number, or {@code null} for the default value.
     *
     * @see #getSize()
     * @see #setSize(Expression)
     *
     * @todo Needs a JAXB adapter to {@code ParameterValueType}.
     */
    @XmlElement(name = "Size")
    protected Expression<R, ? extends Number> size;

    /**
     * Rotation angle of the graphic when it is drawn, or {@code null} for the default value.
     *
     * @see #getRotation()
     * @see #setRotation(Expression)
     *
     * @todo Needs a JAXB adapter to {@code ParameterValueType}.
     */
    @XmlElement(name = "Rotation")
    protected Expression<R, ? extends Number> rotation;

    /**
     * Location to use for anchoring the graphic to the geometry, or {@code null} for lazily constructed default.
     *
     * @see #getAnchorPoint()
     * @see #setAnchorPoint(AnchorPoint)
     */
    @XmlElement(name = "AnchorPoint")
    protected AnchorPoint<R> anchorPoint;

    /**
     * Displacement from the "hot-spot" point, or {@code null} for lazily constructed default.
     *
     * @see #getDisplacement()
     * @see #setDisplacement(Displacement)
     */
    @XmlElement(name = "Displacement")
    protected Displacement<R> displacement;

    /**
     * For JAXB unmarshalling only.
     */
    private Graphic() {
        // Thread-local factory will be used.
    }

    /**
     * Creates a graphic initialized to opaque default mark, default size and no rotation.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public Graphic(final StyleFactory<R> factory) {
        super(factory);
        graphicalSymbols = new ArrayList<>();
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public Graphic(final Graphic<R> source) {
        super(source);
        graphicalSymbols = new ArrayList<>(source.graphicalSymbols);
        opacity          = source.opacity;
        size             = source.size;
        rotation         = source.rotation;
        anchorPoint      = source.anchorPoint;
        displacement     = source.displacement;
    }

    /**
     * Returns the list of external image files or marks that comprise this graphic.
     * All elements of the list shall be instances of either {@link Mark} or {@link ExternalGraphic}.
     * The list may contain multiple external URLs and marks with the semantic that they all provide
     * the equivalent graphic in different {@linkplain GraphicalSymbol#getFormat() formats}.
     *
     * <p>If the list is empty, it is to be treated as a single default mark.
     * That default is a square with with a 50%-gray fill and a black outline,
     * with a size of 6 pixels, unless an explicit {@linkplain #getSize() size} is specified.</p>
     *
     * <p>The returned collection is <em>live</em>:
     * changes in that collection are reflected into this object, and conversely.</p>
     *
     * @return list of marks or external graphics, as a live collection.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<GraphicalSymbol<R>> graphicalSymbols() {
        return graphicalSymbols;
    }

    /**
     * Indicates the level of translucency as a floating point number between 0 and 1 (inclusive).
     * A value of zero means completely transparent. A value of 1.0 means completely opaque.
     *
     * @return the level of translucency as a floating point number between 0 and 1 (inclusive).
     *
     * @see Fill#getOpacity()
     * @see Stroke#getOpacity()
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
     * Returns the absolute size of the graphic as a floating point number.
     * If a size is specified, the height of the graphic will be scaled to
     * that size and the aspect ratio will be used for computing the width.
     * The unit of measurement is given by {@link Symbolizer#getUnitOfMeasure()}.
     * Value shall be positive.
     *
     * <h4>Default value</h4>
     * The default size of an image format is the inherent size of the image.
     * The default size of an image format without an inherent size is defined
     * to be 16 pixels in height and the corresponding aspect in width.
     * If no image or mark is specified, then the size of the default mark is 6 pixels.
     * That default value is standardized by OGC 05-077r4.
     *
     * @return absolute size of the graphic as a floating point number, or {@code null} for the default value.
     */
    public Expression<R, ? extends Number> getSize() {
        final var value = size;
        if (value == null && graphicalSymbols.isEmpty()) {
            return factory.six;
        }
        return value;
    }

    /**
     * Sets the absolute size of the graphic as a floating point number.
     * If this method is never invoked, then the default value is {@code null}.
     *
     * @param  value  new absolute size of the graphic, or {@code null} for the default value.
     */
    public void setSize(final Expression<R, ? extends Number> value) {
        size = value;
    }

    /**
     * Returns the rotation angle of the graphic when it is drawn.
     * The rotation is in the clockwise direction around the graphic center point in decimal degrees.
     * Negative values mean counter-clockwise rotation. The default value is 0.0 (no rotation).
     *
     * <p>The point within the graphic about which it is rotated is format dependent.
     * If a format does not include an inherent rotation point,
     * then the point of rotation should be the centroid.</p>
     *
     * @return the rotation angle of the graphic when it is drawn.
     */
    public Expression<R, ? extends Number> getRotation() {
        return defaultToZero(rotation);
    }

    /**
     * Sets the rotation angle of the graphic when it is drawn.
     * If this method is never invoked, then the default value is literal 0.
     *
     * @param  value  new rotation angle of the graphic, or {@code null} for resetting the default value.
     */
    public void setRotation(final Expression<R, ? extends Number> value) {
        rotation = value;
    }

    /**
     * Returns the location inside of a graphic to use for anchoring the graphic to the main-geometry point.
     * The coordinates are given as (<var>x</var>,<var>y</var>) floating-point numbers
     * relative the graphic bounding box, where (0,0) is the lower-left corner and
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
     * Sets the location inside of a graphic to use for anchoring the graphic to the main-geometry point.
     * The given instance is stored by reference, it is not cloned. If this method is never invoked,
     * then the default value is a {@linkplain AnchorPoint#AnchorPoint() default anchor point}.
     *
     * @param  value  new anchor point, or {@code null} for resetting the default value.
     */
    public void setAnchorPoint(final AnchorPoint<R> value) {
        anchorPoint = value;
    }

    /**
     * Returns the two-dimensional displacement from the "hot-spot" point.
     * This element may be used to avoid over-plotting of multiple graphic symbols for the same point.
     * The unit of measurement is given by {@link Symbolizer#getUnitOfMeasure()}.
     * Positive values are to the right of the point.
     * The default displacement is <var>x</var> = 0, <var>y</var> = 0,
     *
     * <p>If displacement is used in conjunction with size and/or rotation
     * then the graphic symbol shall be scaled and/or rotated before it is displaced.</p>
     *
     * @return displacement from the "hot-spot" point.
     *
     * @see PolygonSymbolizer#getDisplacement()
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
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {graphicalSymbols, opacity, size, rotation, anchorPoint, displacement};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public Graphic<R> clone() {
        final var clone = (Graphic<R>) super.clone();
        clone.selfClone();
        return clone;
    }

    /**
     * Clones the mutable style fields of this element.
     */
    private void selfClone() {
        graphicalSymbols = new ArrayList<>(graphicalSymbols);
        graphicalSymbols.replaceAll(GraphicalSymbol::clone);
        if (anchorPoint  != null) anchorPoint  = anchorPoint .clone();
        if (displacement != null) displacement = displacement.clone();
    }
}
