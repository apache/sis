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
import org.apache.sis.filter.Expression;


/**
 * Relief shading (or “hill shading”) applied to an image for a three-dimensional visual effect.
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
@XmlType(name = "ShadedReliefType", propOrder = {
    "brightnessOnly",
    "reliefFactor"
})
@XmlRootElement(name = "ShadedRelief")
public class ShadedRelief<R> extends StyleElement<R> {
    /**
     * Whether to apply the shading to the image generated so far by other layers.
     *
     * @see #isBrightnessOnly()
     * @see #setBrightnessOnly(Expression)
     *
     * @todo Needs an adapter from expression to plain boolean.
     */
    @XmlElement(name = "BrightnessOnly")
    protected Expression<R,Boolean> brightnessOnly;

    /**
     * Amount of exaggeration to use for the height of the hills, or {@code null} for the default value.
     *
     * @see #getReliefFactor()
     * @see #setReliefFactor(Expression)
     */
    @XmlElement(name = "ReliefFactor")
    protected Expression<R, ? extends Number> reliefFactor;

    /**
     * For JAXB unmarshalling only.
     */
    private ShadedRelief() {
        // Thread-local factory will be used.
    }

    /**
     * Creates a shaded relief initialized to implementation-specific default values.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public ShadedRelief(final StyleFactory<R> factory) {
        super(factory);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public ShadedRelief(final ShadedRelief<R> source) {
        super(source);
        brightnessOnly = source.brightnessOnly;
        reliefFactor   = source.reliefFactor;
    }

    /**
     * Returns whether to apply the shading to the image generated so far by other layers.
     * If {@code false}, then the shading is applied only on the layer being rendered by
     * the current {@link RasterSymbolizer}.
     *
     * @return whether to apply the shading to the image generated so far by other layers.
     */
    public Expression<R,Boolean> isBrightnessOnly() {
        return defaultToFalse(brightnessOnly);
    }

    /**
     * Sets whether to apply the shading to the image generated so far by other layers.
     * If this method is never invoked, then the default value is literal false.
     *
     * @param  value  new policy, or {@code null} for resetting the default value.
     */
    public void setBrightnessOnly(final Expression<R,Boolean> value) {
        brightnessOnly = value;
    }

    /**
     * Returns the amount of exaggeration to use for the height of the hills.
     * A value of around 55 gives reasonable results for Earth-based DEMs.
     *
     * @return amount of exaggeration to use for the height of the hills.
     */
    public Expression<R, ? extends Number> getReliefFactor() {
        final var value = reliefFactor;
        return (value != null) ? value : factory.relief;
    }

    /**
     * Sets the amount of exaggeration to use for the height of the hills.
     * If this method is never invoked, then the default value is implementation-specific.
     *
     * @param  value  new amount of exaggeration, or {@code null} for resetting the default value.
     */
    public void setReliefFactor(final Expression<R, ? extends Number> value) {
        reliefFactor = value;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {brightnessOnly, reliefFactor};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public ShadedRelief<R> clone() {
        return (ShadedRelief<R>) super.clone();
    }
}
