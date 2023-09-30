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

import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;


/**
 * Instructions about how to draw a graphic at a point.
 * if a line, polygon, or raster geometry is used with this symbolizer,
 * then some representative point such as the centroid should be used.
 *
 * <!-- Following list of authors contains credits to OGC GeoAPI 2 contributors. -->
 * @author  Johann Sorel (Geomatys)
 * @author  Chris Dillard (SYS Technologies)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param <R>  the type of data to style, such as {@code Feature} or {@code Coverage}.
 */
@XmlType(name = "PointSymbolizerType")
@XmlRootElement(name = "PointSymbolizer")
public class PointSymbolizer<R> extends Symbolizer<R> implements GraphicalElement<R> {
    /**
     * Information about how to draw graphics, or {@code null} for no graphic.
     * If no value has been explicitly set (including null value),
     * then a default graphic will be lazily created when first requested.
     *
     * @see #getGraphic()
     * @see #setGraphic(Graphic)
     */
    @XmlElement(name = "Graphic")
    protected Graphic<R> graphic;

    /**
     * Whether {@link #graphic} has been explicitly set to some value, including null.
     * If {@code false}, then a default graphic will be created when first needed.
     */
    private boolean isGraphicSet;

    /**
     * Invoked by JAXB before marshalling this point symbolizer.
     * Creates the default graphic if needed.
     */
    private void beforeMarshal(Marshaller caller) {
        if (graphic == null && !isGraphicSet) {
            graphic = factory.createGraphic();
        }
    }

    /**
     * For JAXB unmarshalling only. This constructor disables the lazy creation of default values.
     * This is because OGC 05-077r4 said that if the graphic is not specified, then none should be used.
     */
    private PointSymbolizer() {
        // Thread-local factory will be used.
        isGraphicSet = true;
    }

    /**
     * Creates a symbolizer initialized to a default graphic.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public PointSymbolizer(final StyleFactory<R> factory) {
        super(factory);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public PointSymbolizer(final PointSymbolizer<R> source) {
        super(source);
        graphic = source.graphic;
    }

    /**
     * Returns the graphic that will be drawn at each point of the geometry.
     * The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this fill, and conversely.
     *
     * @return information about how to draw graphics.
     *
     * @see #isVisible()
     */
    @Override
    public Graphic<R> getGraphic() {
        if (graphic == null) {
            graphic = factory.createGraphic();
        }
        return graphic;
    }

    /**
     * Specifies the graphic that will be drawn at each point of the geometry.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is a {@linkplain Graphic#Graphic() default instance}.
     * If this method is invoked with an explicit {@code null}, then nothing will be plotted.
     *
     * @param  value  new information about how to draw graphics, or {@code null} for none.
     */
    @Override
    public void setGraphic(final Graphic<R> value) {
        isGraphicSet = true;
        graphic = value;
    }

    /**
     * Returns {@code true} if this symbolizer has a graphic.
     * If {@code setGraphic(null)} has been explicitly invoked with a null argument value,
     * then this method returns {@code false}.
     *
     * @return whether this symbolizer has a graphic.
     *
     * @see #setGraphic(Graphic)
     */
    @Override
    public boolean isVisible() {
        return graphic != null || !isGraphicSet;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {graphic, isGraphicSet};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public PointSymbolizer<R> clone() {
        final var clone = (PointSymbolizer<R>) super.clone();
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
