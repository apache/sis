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
import org.apache.sis.util.resources.Errors;


/**
 * Specifies the false-color channel selection for a multi-spectral raster source.
 * It can be used for rasters such as a multi-band satellite-imagery.
 * Either red, green, and blue channels are selected, or a single grayscale channel is selected.
 * Contrast enhancement may be applied to each channel in isolation.
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
@XmlType(name = "ChannelSelectionType", propOrder = {
    "red",
    "green",
    "blue",
    "gray"
})
@XmlRootElement(name = "ChannelSelection")
public class ChannelSelection<R> extends StyleElement<R> {
    /**
     * The red channel, or {@code null} if none.
     * This property is mutually exclusive with {@link #gray}.
     */
    @XmlElement(name = "RedChannel")
    protected SelectedChannel<R> red;

    /**
     * The green channel, or {@code null} if none.
     * This property is mutually exclusive with {@link #gray}.
     */
    @XmlElement(name = "GreenChannel")
    protected SelectedChannel<R> green;

    /**
     * The blue channel, or {@code null} if none.
     * This property is mutually exclusive with {@link #gray}.
     */
    @XmlElement(name = "BlueChannel")
    protected SelectedChannel<R> blue;

    /**
     * The gray channel, or {@code null} if none.
     * This property is mutually exclusive with {@link #red}, {@link #green} and {@link #blue}.
     */
    @XmlElement(name = "GrayChannel")
    protected SelectedChannel<R> gray;

    /**
     * For JAXB unmarshalling only.
     */
    private ChannelSelection() {
        // Thread-local factory will be used.
    }

    /**
     * Creates an initially empty channel selection.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public ChannelSelection(final StyleFactory<R> factory) {
        super(factory);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public ChannelSelection(final ChannelSelection<R> source) {
        super(source);
        red   = source.red;
        green = source.green;
        blue  = source.blue;
        gray  = source.gray;
    }

    /**
     * Gets the channels to be used, or {@code null} if none.
     * If non-null, then the array length is either 1 or 3.
     * An array of length 1 contains the grayscale channel.
     * An array of length 3 contains red, green and blue channels, in that order.
     *
     * @return array of channels, or {@code null} if none.
     *
     * @todo Replace null value by some default value.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})        // Generic array creation.
    public SelectedChannel<R>[] getChannels() {
        if (red != null || green != null || blue != null) {
            return new SelectedChannel[] {red, green, blue};
        } else if (gray != null) {
            return new SelectedChannel[] {gray};
        } else {
            return null;
        }
    }

    /**
     * Sets the channels to be used.
     * The number of channels shall be 0, 1 or 3.
     * If the given array is null or empty, then the channel selection is cleared.
     * If only one channel is specified, then it is interpreted as grayscale.
     * If three channels are specified, then they are interpreted as red, green and blue channels in that order.
     * Otherwise an exception is thrown.
     *
     * @param  values  array of channels, or {@code null} if none.
     * @throws IllegalArgumentException if the length of the specified array is not 0, 1 or 3.
     */
    @SafeVarargs
    public final void setChannels(final SelectedChannel<R>... values) {
        red   = null;
        green = null;
        blue  = null;
        gray  = null;
        if (values != null) {
            switch (values.length) {
                case 0: break;
                case 1: gray = values[0]; break;
                case 3: {
                    red   = values[0];
                    green = values[1];
                    blue  = values[2];
                    break;
                }
                default: {
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.UnexpectedArrayLength_2, 3, values.length));
                }
            }
        }
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {red, green, blue, gray};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public ChannelSelection<R> clone() {
        final var clone = (ChannelSelection<R>) super.clone();
        clone.selfClone();
        return clone;
    }

    /**
     * Clones the mutable style fields of this element.
     */
    private void selfClone() {
        if (red   != null) red   = red  .clone();
        if (green != null) green = green.clone();
        if (blue  != null) blue  = blue .clone();
        if (gray  != null) gray  = gray .clone();
    }
}
