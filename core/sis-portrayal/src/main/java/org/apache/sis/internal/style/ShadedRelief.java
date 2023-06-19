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
package org.apache.sis.internal.style;

import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;


/**
 * Relief shading (or “hill shading”) applied to an image for a three-dimensional visual effect.
 *
 * <!-- Following list of authors contains credits to OGC GeoAPI 2 contributors. -->
 * @author  Ian Turton (CCG)
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
@XmlType(name = "ShadedReliefType", propOrder = {
    "brightnessOnly",
    "reliefFactor"
})
@XmlRootElement(name = "ShadedRelief")
public class ShadedRelief extends StyleElement {
    /**
     * Default value for {@link #getReliefFactor()}.
     * No standard value is specified by OGC 05-077r4.
     */
    private static final Expression<Feature,Double> DEFAULT_VALUE = literal(55.0);

    /**
     * Whether to apply the shading to the image generated so far by other layers.
     *
     * @see #isBrightnessOnly()
     * @see #setBrightnessOnly(Expression)
     *
     * @todo Needs an adapter from expression to plain boolean.
     */
    @XmlElement(name = "BrightnessOnly")
    protected Expression<Feature,Boolean> brightnessOnly;

    /**
     * Amount of exaggeration to use for the height of the hills, or {@code null} for the default value.
     *
     * @see #getReliefFactor()
     * @see #setReliefFactor(Expression)
     */
    @XmlElement(name = "ReliefFactor")
    protected Expression<Feature, ? extends Number> reliefFactor;

    /**
     * Creates a shaded relief initialized to implementation-specific default values.
     */
    public ShadedRelief() {
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public ShadedRelief(final ShadedRelief source) {
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
    public Expression<Feature,Boolean> isBrightnessOnly() {
        return defaultToFalse(brightnessOnly);
    }

    /**
     * Sets whether to apply the shading to the image generated so far by other layers.
     * If this method is never invoked, then the default value is literal false.
     *
     * @param  value  new policy, or {@code null} for resetting the default value.
     */
    public void setBrightnessOnly(final Expression<Feature,Boolean> value) {
        brightnessOnly = value;
    }

    /**
     * Returns the amount of exaggeration to use for the height of the hills.
     * A value of around 55 gives reasonable results for Earth-based DEMs.
     *
     * @return amount of exaggeration to use for the height of the hills.
     */
    public Expression<Feature, ? extends Number> getReliefFactor() {
        final var value = reliefFactor;
        return (value != null) ? value : DEFAULT_VALUE;
    }

    /**
     * Sets the amount of exaggeration to use for the height of the hills.
     * If this method is never invoked, then the default value is implementation-specific.
     *
     * @param  value  new amount of exaggeration, or {@code null} for resetting the default value.
     */
    public void setReliefFactor(final Expression<Feature, ? extends Number> value) {
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
    public ShadedRelief clone() {
        final var clone = (ShadedRelief) super.clone();
        return clone;
    }
}
