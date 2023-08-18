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
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;

// Branch-dependent imports
import org.apache.sis.filter.Expression;


/**
 * Information about a channel to use in a multi-spectral source.
 * Channels are identified by data-dependent character identifiers.
 * Commonly, channels will be labelled as "1", "2", <i>etc</i>.
 * A set of selected channels is contained in {@link ChannelSelection}.
 *
 * <!-- Following list of authors contains credits to OGC GeoAPI 2 contributors. -->
 * @author  Ian Turton (CCG)
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @param <R>  the type of data to style, such as {@code Feature} or {@code Coverage}.
 *
 * @since 1.5
 */
@XmlType(name = "SelectedChannelType", propOrder = {
    "sourceChannelName",
    "contrastEnhancement"
})
// No root element is specified in OGC 05-077r4.
public class SelectedChannel<R> extends StyleElement<R> {
    /**
     * The channel's name, or {@code null} if unspecified.
     *
     * @see #getSourceChannelName()
     * @see #setSourceChannelName(Expression)
     *
     * @todo Needs an adapter from expression to plain string.
     */
    @XmlElement(name = "SourceChannelName", required = true)
    protected Expression<R,String> sourceChannelName;

    /**
     * Contrast enhancement applied to the selected channel in isolation, or {@code null} if none.
     *
     * @see #getContrastEnhancement()
     * @see #setContrastEnhancement(ContrastEnhancement)
     */
    @XmlElement(name = "ContrastEnhancement")
    protected ContrastEnhancement<R> contrastEnhancement;

    /**
     * For JAXB unmarshalling only.
     */
    private SelectedChannel() {
        // Thread-local factory will be used.
    }

    /**
     * Creates an initially empty selected channel.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public SelectedChannel(final StyleFactory<R> factory) {
        super(factory);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public SelectedChannel(final SelectedChannel<R> source) {
        super(source);
        sourceChannelName   = source.sourceChannelName;
        contrastEnhancement = source.contrastEnhancement;
    }

    /**
     * Returns the channel's name.
     *
     * @return the channel's name, or {@code null} if unspecified.
     *
     * @todo Shall never be {@code null}. We need to think about some default value.
     */
    public Expression<R,String> getSourceChannelName() {
        return sourceChannelName;
    }

    /**
     * Sets the channel's name.
     *
     * @param  value  the channel's name, or {@code null} if unspecified.
     */
    public void setSourceChannelName(final Expression<R,String> value) {
        sourceChannelName = value;
    }

    /**
     * Returns the contrast enhancement applied to the selected channel in isolation.
     * The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this stroke, and conversely.
     *
     * @return contrast enhancement for the selected channel.
     *
     * @see RasterSymbolizer#getContrastEnhancement()
     */
    public Optional<ContrastEnhancement<R>> getContrastEnhancement() {
        return Optional.ofNullable(contrastEnhancement);
    }

    /**
     * Sets the contrast enhancement applied to the selected channel in isolation.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new contrast enhancement, or {@code null} if none.
     *
     * @see RasterSymbolizer#setContrastEnhancement(ContrastEnhancement)
     */
    public void setContrastEnhancement(final ContrastEnhancement<R> value) {
        contrastEnhancement = value;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {sourceChannelName, contrastEnhancement};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public SelectedChannel<R> clone() {
        final var clone = (SelectedChannel<R>) super.clone();
        clone.selfClone();
        return clone;
    }

    /**
     * Clones the mutable style fields of this element.
     */
    private void selfClone() {
        if (contrastEnhancement != null) {
            contrastEnhancement = contrastEnhancement.clone();
        }
    }
}
