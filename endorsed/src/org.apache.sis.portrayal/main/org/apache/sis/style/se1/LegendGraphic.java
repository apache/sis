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


/**
 * A graphic to do displayed in a legend for a rule.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @param <R>  the type of data to style, such as {@code Feature} or {@code Coverage}.
 *
 * @since 1.5
 */
@XmlType(name = "LegendGraphicType")
@XmlRootElement(name = "LegendGraphic")
public class LegendGraphic<R> extends StyleElement<R> implements GraphicalElement<R> {
    /**
     * The graphic to use as a legend, or {@code null} for lazily constructed default.
     * This property is mandatory: a null value <em>shall</em> be replaced by a default value when first requested.
     *
     * @see #getGraphic()
     * @see #setGraphic(Graphic)
     */
    protected Graphic<R> graphic;

    /**
     * For JAXB unmarshalling only.
     */
    private LegendGraphic() {
        // Thread-local factory will be used.
    }

    /**
     * Creates a legend initialized to the default graphic.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public LegendGraphic(final StyleFactory<R> factory) {
        super(factory);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public LegendGraphic(final LegendGraphic<R> source) {
        super(source);
        graphic = source.graphic;
    }

    /**
     * Returns the graphic of the legend.
     * The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this fill, and conversely.
     *
     * @return graphic of the legend.
     */
    @Override
    @XmlElement(name = "Graphic", required = true)
    public final Graphic<R> getGraphic() {
        if (graphic == null) {
            graphic = factory.createGraphic();
        }
        return graphic;
    }

    /**
     * Sets the graphic of the legend.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is a {@linkplain Graphic#Graphic() default instance}.
     *
     * @param  value  new graphic of the legend, or {@code null} for resetting the default value.
     */
    @Override
    public final void setGraphic(final Graphic<R> value) {
        graphic = value;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {graphic};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public LegendGraphic<R> clone() {
        final var clone = (LegendGraphic<R>) super.clone();
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
