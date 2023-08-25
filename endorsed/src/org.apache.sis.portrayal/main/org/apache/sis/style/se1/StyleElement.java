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

import java.awt.Color;
import java.util.Arrays;
import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.util.InternationalString;
import org.apache.sis.util.ArgumentChecks;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;


/**
 * Base class of all style objects.
 * This base class cannot be extended directly.
 * Instead, one of the subclasses can be extended.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @param <R>  the type of data to style, such as {@code Feature} or {@code Coverage}.
 *
 * @since 1.5
 */
@XmlTransient
public abstract class StyleElement<R> implements Cloneable {
    /**
     * The factory to use for creating expressions and child elements.
     * This is typically the same factory than the one used for creating this element.
     *
     * @see FeatureTypeStyle#FACTORY
     * @see CoverageStyle#FACTORY
     */
    protected final StyleFactory<R> factory;

    /**
     * Creates a new style element.
     * Intentionally restricted to this package because {@link #properties()} is package-private.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    StyleElement(final StyleFactory<R> factory) {
        ArgumentChecks.ensureNonNull("factory", factory);
        this.factory = factory;
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    StyleElement(final StyleElement<R> source) {
        factory = source.factory;
    }

    /**
     * Creates a style element for XML unmarshalling.
     * <em>This constructor is unsafe</em> and should be used only by JAXB reflection.
     *
     * @todo Allow the factory to be set according the parent {@link AbstractStyle} being unmarshalled.
     *       We will need to use {@link ThreadLocal}.
     */
    StyleElement() {
        factory = null;     // TODO
    }

    /**
     * Returns a literal for the given value.
     * This is a convenience method for use with setter methods that expect an expression.
     *
     * @param  <E>    type of value.
     * @param  value  the value for which to return a literal, or {@code null} if none.
     * @return literal for the given value, or {@code null} if the given value was null.
     */
    public final <E> Literal<R,E> literal(final E value) {
        return (value == null) ? null : factory.filterFactory.literal(value);
    }

    /**
     * Returns the given expression if non-null, or literal {@code true} otherwise.
     * This is a convenience method for the implementation of getter methods when
     * a default value exists.
     *
     * @param  value  the value for which to apply a default value if {@code null}.
     * @return the given value if non-null, or {@code true} literal otherwise.
     */
    protected final Expression<R,Boolean> defaultToTrue(final Expression<R,Boolean> value) {
        return (value != null) ? value : factory.enabled;
    }

    /**
     * Returns the given expression if non-null, or literal {@code false} otherwise.
     * This is a convenience method for the implementation of getter methods when
     * a default value exists.
     *
     * @param  value  the value for which to apply a default value if {@code null}.
     * @return the given value if non-null, or a {@code false} literal otherwise.
     */
    protected final Expression<R,Boolean> defaultToFalse(final Expression<R,Boolean> value) {
        return (value != null) ? value : factory.disabled;
    }

    /**
     * Returns the given expression if non-null, or literal {@code 0.0} otherwise.
     * This is a convenience method for the implementation of getter methods when
     * a default value exists.
     *
     * @param  value  the value for which to apply a default value if {@code null}.
     * @return the given value if non-null, or {@code 0.0} literal otherwise.
     */
    protected final Expression<R, ? extends Number> defaultToZero(final Expression<R, ? extends Number> value) {
        return (value != null) ? value : factory.zero;
    }

    /**
     * Returns the given expression if non-null, or literal {@code 0.5} otherwise.
     * This is a convenience method for the implementation of getter methods when
     * a default value exists.
     *
     * @param  value  the value for which to apply a default value if {@code null}.
     * @return the given value if non-null, or {@code 0.5} literal otherwise.
     */
    protected final Expression<R, ? extends Number> defaultToHalf(final Expression<R, ? extends Number> value) {
        return (value != null) ? value : factory.half;
    }

    /**
     * Returns the given expression if non-null, or literal {@code 1.0} otherwise.
     * This is a convenience method for the implementation of getter methods when
     * a default value exists.
     *
     * @param  value  the value for which to apply a default value if {@code null}.
     * @return the given value if non-null, or {@code 1.0} literal otherwise.
     */
    protected final Expression<R, ? extends Number> defaultToOne(final Expression<R, ? extends Number> value) {
        return (value != null) ? value : factory.one;
    }

    /**
     * Returns the opacity of the alpha value of the given color.
     * If the color is totally opaque, then this method returns {@code null}.
     *
     * @param  color  color from which to get the opacity.
     * @return opacity derived from the alpha value of the color, or {@code null} if totally opaque.
     */
    final Expression<R, ? extends Number> opacity(final Color color) {
        final int alpha = color.getAlpha();
        return (alpha != 255) ? literal(alpha / 256d) : null;
        // Divide by 256 instead of 255 in order to get round numbers for alpha values 64, 128, etc.
    }

    /**
     * Returns all properties contained in the subclasses.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     *
     * @return all properties.
     */
    abstract Object[] properties();

    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return getClass().hashCode() + Arrays.hashCode(properties());
    }

    /**
     * Compares this element with the given object for equality.
     *
     * @param  obj  the other object to compare with this.
     * @return whether the other object is equal to this.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        return (obj != null) && (obj.getClass() == getClass()) &&
                Arrays.equals(properties(), ((StyleElement) obj).properties());
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     * ISO 19115 metadata and {@link InternationalString} members, if any,
     * are not cloned neither in current Apache SIS version.
     *
     * @return a clone of this element.
     */
    @Override
    @SuppressWarnings("unchecked")
    public StyleElement<R> clone() {
        try {
            return (StyleElement<R>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);    // Should never happen since we are cloneable.
        }
    }
}
