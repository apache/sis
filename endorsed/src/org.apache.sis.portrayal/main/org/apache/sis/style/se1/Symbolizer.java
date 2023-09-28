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

import java.util.Objects;
import java.util.Optional;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import javax.measure.Unit;
import org.apache.sis.measure.Units;
import org.apache.sis.util.resources.Errors;

// Specific to the main branch:
import org.apache.sis.filter.Expression;


/**
 * Description of how a feature is to appear on a map.
 * A symbolizer describes how the shape should appear,
 * together with graphical properties such as color and opacity.
 * A symbolizer is obtained by specifying one of a small number of different types
 * and then supplying parameters to override its default behavior.
 * The predefined type of symbolizers are
 * {@linkplain LineSymbolizer line},
 * {@linkplain PolygonSymbolizer polygon},
 * {@linkplain PointSymbolizer point},
 * {@linkplain TextSymbolizer text}, and
 * {@linkplain RasterSymbolizer raster} symbolizers.
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
@XmlType(name = "SymbolizerType", propOrder = {
    "name",
    "description",
    "geometry"
})
@XmlSeeAlso({
    LineSymbolizer.class,
    PointSymbolizer.class,
    PolygonSymbolizer.class,
    TextSymbolizer.class,
    RasterSymbolizer.class
})
@XmlRootElement(name = "Symbolizer")
public abstract class Symbolizer<R> extends StyleElement<R> {
    /**
     * Name for this style, or {@code null} if none.
     *
     * @see #getName()
     * @see #setName(String)
     */
    @XmlElement(name = "Name")
    protected String name;

    /**
     * Information for user interfaces, or {@code null} if none.
     *
     * @see #getDescription()
     * @see #setDescription(Description)
     */
    @XmlElement(name = "Description")
    protected Description<R> description;

    /**
     * Expression fetching the geometry to draw, or {@code null} for the default geometries.
     * The Symbology Encoding restrict the XML representation to {@code <ogc:PropertyName>}
     * (defined in the Filter Specification), but Apache SIS accepts any expression.
     *
     * @see #getGeometry()
     * @see #setGeometry(Expression)
     */
    @XmlElement(name = "Geometry")
    protected Expression<R,?> geometry;

    /**
     * Unit of measurement for all lengths inside this symbolizer, or {@code null} for the default value.
     * The recommended XML representations of this attribute are like below:
     *
     * <ul>
     *   <li>uom="http://www.opengeospatial.org/se/units/metre"</li>
     *   <li>uom="http://www.opengeospatial.org/se/units/foot"</li>
     *   <li>uom="http://www.opengeospatial.org/se/units/pixel"</li>
     * </ul>
     *
     * @todo Recommended XML representation is not yet implemented.
     *
     * @see #getUnitOfMeasure()
     * @see #setUnitOfMeasure(Unit)
     */
    @XmlAttribute(name = "uom")
    @XmlSchemaType(name = "anyURI")
    protected Unit<?> unit;

    /**
     * For JAXB unmarshalling only.
     */
    Symbolizer() {
        // Thread-local factory will be used.
    }

    /**
     * Creates a symbolizer initialized to default geometries and pixel unit of measurement.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public Symbolizer(final StyleFactory<R> factory) {
        super(factory);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    protected Symbolizer(final Symbolizer<R> source) {
        super(source);
        name        = source.name;
        geometry    = source.geometry;
        description = source.description;
        unit        = source.unit;
    }

    /**
     * Returns the name for this symbolizer.
     * This can be any string that uniquely identifies this symbolizer within a given canvas.
     * It is not meant to be human-friendly. For a human-friendly label,
     * see the {@linkplain Description#getTitle() title} instead.
     *
     * @return a name for this symbolizer.
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Sets a name for this symbolizer.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new name for this symbolizer, or {@code null} if none.
     */
    public void setName(final String value) {
        name = value;
    }

    /**
     * Returns the description of this style.
     * The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this symbolizer, and conversely.
     *
     * @return information for user interfaces.
     */
    public Optional<Description<R>> getDescription() {
        return Optional.ofNullable(description);
    }

    /**
     * Sets a description of this style.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new information for user interfaces, or {@code null} if none.
     */
    public void setDescription(final Description<R> value) {
        description = value;
    }

    /**
     * Returns the unit of measurement for all lengths inside this symbolizer.
     * The unit applies to stroke width, size, gap, initial gap, displacement and perpendicular offset.
     * Two types of units are allowed:
     *
     * <ul>
     *   <li>{@link Units#PIXEL} is interpreted as a paper unit, referring to the size of the map.</li>
     *   <li>{@linkplain Units#isLinear(Unit) Linear units} such as metre and foot are “ground” units
     *       referring to the actual size of real-world objects.</li>
     * </ul>
     *
     * @return unit of measurement for all lengths inside this symbolizer.
     */
    public Unit<?> getUnitOfMeasure() {
        final var value = unit;
        return (value != null) ? value : Units.PIXEL;
    }

    /**
     * Sets the unit of measurement for all lengths inside this symbolizer.
     * If this method is never invoked, then the default value is {@link Units#PIXEL}.
     * That default value is standardized by OGC 05-077r4.
     *
     * @param  value  new unit of measurement for lengths, or {@code null} for resetting the default value.
     * @throws IllegalArgumentException if the specified value is neither {@link Units#PIXEL} or a linear unit.
     */
    public void setUnitOfMeasure(Unit<?> value) {
        if (value != null && value != Units.PIXEL && !Units.isLinear(value)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.NonLinearUnit_1, value));
        }
        unit = value;
    }

    /**
     * Returns the expression used for fetching the geometry to draw.
     * The value in the referenced feature property should be a geometry
     * from a {@linkplain org.apache.sis.setup.GeometryLibrary supported library},
     * expect in the particular case of {@link RasterSymbolizer} where the value
     * should be a {@link org.apache.sis.coverage.BandedCoverage}.
     *
     * @return expression fetching the geometry or the coverage to draw.
     */
    public Expression<R,?> getGeometry() {
        final var value = geometry;
        return (value != null) ? value : factory.defaultGeometry;
    }

    /**
     * Sets the expression used for fetching the geometry to draw.
     * If this method is never invoked, then the default value is
     * a {@code ValueReference} fetching {@code "sis:geometry"}.
     *
     * @todo The default expression may change in a future version.
     *       According SE specification, the default should fetch all geometries.
     *
     * @param  value  new expression fetching the geometry to draw, or {@code null} for resetting the default value.
     */
    public void setGeometry(final Expression<R,?> value) {
        geometry = value;
    }

    /**
     * Returns {@code true} if this symbolizer has enough information for displaying something.
     *
     * @return whether this symbolizer can display something.
     */
    public boolean isVisible() {
        return true;
    }

    /**
     * Returns a hash code value for this symbolizer.
     *
     * @return a hash code value for this symbolizer.
     */
    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(name, description, geometry, unit);
    }

    /**
     * Compares this symbolizer with the given object for equality.
     *
     * @param  obj  the other object to compare with this.
     * @return whether the other object is equal to this.
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final var other = (Symbolizer) obj;
        return Objects.equals(name,        other.name)
            && Objects.equals(description, other.description)
            && Objects.equals(geometry,    other.geometry)
            && Objects.equals(unit,        other.unit);
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public Symbolizer<R> clone() {
        final var clone = (Symbolizer<R>) super.clone();
        clone.selfClone();
        return clone;
    }

    /**
     * Clones the mutable style fields of this element.
     */
    private void selfClone() {
        if (description != null) description = description.clone();
    }
}
