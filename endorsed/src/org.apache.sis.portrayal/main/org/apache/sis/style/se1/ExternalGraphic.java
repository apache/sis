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
import jakarta.xml.bind.annotation.XmlRootElement;


/**
 * Reference to an external file that contains an image of some kind, such as a PNG or SVG.
 * This is an alternative to {@link Mark} for {@linkplain Graphic#graphicalSymbols graphical symbols}.
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
@XmlType(name = "ExternalGraphicType", propOrder = {
//  "onlineResource",       // XML encoding not yet available.
//  "inlineContent",        // Idem.
    "format",
    "colorReplacements"
})
@XmlRootElement(name = "ExternalGraphic")
public class ExternalGraphic<R> extends GraphicalSymbol<R> {
    /**
     * A list of colors to replace, or {@code null} if none.
     *
     * <h4>XML marshalling</h4>
     * A null value is not synonymous to an empty list.
     * An empty list causes an empty {@code <ColorReplacement/>} element to be marshalled,
     * while a {@code null} value results in no element being marshalled.
     *
     * @see #colorReplacements()
     */
    @XmlElement(name = "ColorReplacement")
    protected List<ColorReplacement<R>> colorReplacements;

    /**
     * For JAXB unmarshalling only.
     */
    private ExternalGraphic() {
        // Thread-local factory will be used.
    }

    /**
     * Creates an initially empty external graphic.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public ExternalGraphic(final StyleFactory<R> factory) {
        super(factory);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public ExternalGraphic(final ExternalGraphic<R> source) {
        super(source);
        final var value = source.colorReplacements;
        if (value != null) {
            colorReplacements = new ArrayList<>(value);
        }
    }

    /**
     * Returns a list of colors to replace.
     *
     * <p>The returned collection is <em>live</em>:
     * changes in that collection are reflected into this object, and conversely.</p>
     *
     * @return list of colors to replace, as a live collection.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<ColorReplacement<R>> colorReplacements() {
        if (colorReplacements == null) {
            colorReplacements = new ArrayList<>();
        }
        return colorReplacements;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {onlineResource, inlineContent, format, colorReplacements};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     * ISO 19115 metadata instances are also shared (not cloned).
     *
     * @return deep clone of all style elements.
     */
    @Override
    public ExternalGraphic<R> clone() {
        final var clone = (ExternalGraphic<R>) super.clone();
        clone.selfClone();
        return clone;
    }

    /**
     * Clones the mutable style fields of this element.
     */
    private void selfClone() {
        if (colorReplacements != null) {
            colorReplacements = new ArrayList<>(colorReplacements);
            colorReplacements.replaceAll(ColorReplacement::clone);
        }
    }
}
