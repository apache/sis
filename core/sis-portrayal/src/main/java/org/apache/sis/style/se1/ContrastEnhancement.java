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
import org.opengis.filter.Expression;
import org.opengis.style.ContrastMethod;


/**
 * Contrast enhancement for an image or an individual image channel.
 * In the case of a color image, the relative grayscale brightness of a pixel color is used.
 *
 * <!-- Following list of authors contains credits to OGC GeoAPI 2 contributors. -->
 * @author  Johann Sorel (Geomatys)
 * @author  Ian Turton (CCG)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @param <R>  the type of data to style, such as {@code Feature} or {@code Coverage}.
 *
 * @since 1.5
 */
@XmlType(name = "ContrastEnhancementType", propOrder = {
//  "normalize",
//  "histogram",
    "gammaValue"
})
@XmlRootElement(name = "ContrastEnhancement")
public class ContrastEnhancement<R> extends StyleElement<R> {
    /**
     * Method to use for applying contrast enhancement, or {@code null} for the default value.
     * The default value depends on whether or not a {@linkplain #gammaValue gamma value} is defined.
     *
     * @see #getMethod()
     * @see #setMethod(ContrastMethod)
     *
     * @todo Marshall as empty "Normalize" or "Histogram" XML element.
     */
    protected ContrastMethod method;

    /**
     * How much to brighten or dim an image, or {@code null} for the default value.
     *
     * @see #getGammaValue()
     * @see #setGammaValue(Expression)
     *
     * @todo Add a JAXB adapter for marshalling as a plain number.
     */
    @XmlElement(name = "GammaValue")
    protected Expression<R, ? extends Number> gammaValue;

    /**
     * For JAXB unmarshalling only.
     */
    private ContrastEnhancement() {
        // Thread-local factory will be used.
    }

    /**
     * Creates a contrast enhancement initialized to no operation.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public ContrastEnhancement(final StyleFactory<R> factory) {
        super(factory);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public ContrastEnhancement(final ContrastEnhancement<R> source) {
        super(source);
        method     = source.method;
        gammaValue = source.gammaValue;
    }

    /**
     * Returns the method to use for applying contrast enhancement.
     *
     * @return method to use for applying contrast enhancement.
     */
    public ContrastMethod getMethod() {
        final var value = method;
        if (value != null) {
            return value;
        }
        return (gammaValue != null) ? ContrastMethod.GAMMA : ContrastMethod.NONE;
    }

    /**
     * Sets the method to use for applying contrast enhancement.
     * Setting this method to anything else than {@link ContrastMethod#GAMMA}
     * clears the {@linkplain #getGammaValue() gamma value}.
     *
     * @param  value  new method to use, or {@code null} for none.
     */
    public void setMethod(final ContrastMethod value) {
        method = value;
        if (value != ContrastMethod.GAMMA) {
            gammaValue = null;
        }
    }

    /**
     * Tells how much to brighten (values greater than 1) or dim (values less than 1) an image.
     * A value of 1 means no change.
     *
     * @return expression to control gamma adjustment.
     */
    public Expression<R, ? extends Number> getGammaValue() {
        return defaultToOne(gammaValue);
    }

    /**
     * Sets how much to brighten (values greater than 1) or dim (values less than 1) an image.
     * Setting a non-null value sets the method to {@link ContrastMethod#GAMMA}.
     * If this method is never invoked, then the default value is literal 1.
     *
     * @param  value  new expression to control gamma adjustment, or {@code null} for the default.
     */
    public void setGammaValue(final Expression<R, ? extends Number> value) {
        gammaValue = value;
        if (value != null) {
            method = null;
        }
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {method, gammaValue};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public ContrastEnhancement<R> clone() {
        return (ContrastEnhancement<R>) super.clone();
    }
}
