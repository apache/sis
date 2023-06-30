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
 * Stipple-fill repeated graphic.
 * A graphic fill can be used by a {@link Fill} or by a {@link Stroke}.
 * This class contains only a {@link Graphic} property,
 * but its encapsulation in {@code GraphicFill} means that the graphic
 * should be repeated in all the interior of the shape to be drawn.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
@XmlType(name = "GraphicFillType")
@XmlRootElement(name = "GraphicFill")
public class GraphicFill extends StyleElement implements GraphicalElement {
    /**
     * The graphic to be repeated, or {@code null} for lazily constructed default.
     * This property is mandatory: a null value <em>shall</em> be replaced by a default value when first requested.
     *
     * @see #getGraphic()
     * @see #setGraphic(Graphic)
     */
    protected Graphic graphic;

    /**
     * Creates a graphic fill initialized to a default graphic.
     */
    public GraphicFill() {
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public GraphicFill(final GraphicFill source) {
        super(source);
        graphic = source.graphic;
    }

    /**
     * Returns the graphic to be repeated.
     * The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this fill, and conversely.
     *
     * @return the graphic to be repeated.
     *
     * @see GraphicStroke#getGraphic()
     */
    @Override
    @XmlElement(name = "Graphic", required = true)
    public final Graphic getGraphic() {
        if (graphic == null) {
            graphic = new Graphic();
        }
        return graphic;
    }

    /**
     * Sets the graphic to be repeated.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is a {@linkplain Graphic#Graphic() default instance}.
     *
     * @param  value  new graphic, or {@code null} for resetting the default value.
     *
     * @see GraphicStroke#setGraphic(Graphic)
     */
    @Override
    public final void setGraphic(final Graphic value) {
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
    public GraphicFill clone() {
        final var clone = (GraphicFill) super.clone();
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
