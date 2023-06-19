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

import java.util.Arrays;
import jakarta.xml.bind.annotation.XmlTransient;
import org.apache.sis.filter.DefaultFilterFactory;
import org.opengis.util.InternationalString;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Literal;


/**
 * Base class of all style objects.
 * This base class can not be extended directly.
 * Instead, one of the subclasses can be extended.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
@XmlTransient
public abstract class StyleElement implements Cloneable {
    /**
     * Version number of the Symbology Encoding Implementation Specification standard currently implemented.
     * This version number may change in future Apache SIS releases if new standards are published.
     * The current value is {@value}.
     */
    public static final String VERSION = "1.1.0";

    /**
     * Literal commonly used as a default value.
     *
     * @see #defaultToFalse(Expression)
     */
    private static final Literal<Feature,Boolean> FALSE = literal(Boolean.FALSE);

    /**
     * Literal commonly used as a default value.
     *
     * @see #defaultToTrue(Expression)
     */
    private static final Literal<Feature,Boolean> TRUE = literal(Boolean.TRUE);

    /**
     * Literal commonly used as a default value.
     *
     * @see #defaultToZero(Expression)
     */
    static final Literal<Feature,Double> LITERAL_ZERO = literal(0.0);

    /**
     * Literal commonly used as a default value.
     *
     * @see #defaultToOne(Expression)
     */
    private static final Literal<Feature,Double> LITERAL_ONE = literal(1.0);

    /**
     * Creates a new style element.
     * Intentionally restricted to this package because {@link #properties()} is package-private.
     */
    StyleElement() {
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    StyleElement(final StyleElement source) {
        // No property to copy yet, but some may be added in the future.
    }

    /**
     * The factory for creating default expressions.
     */
    static FilterFactory<Feature,Object,Object> FF() {
        return DefaultFilterFactory.forFeatures();
    }

    /**
     * Returns a literal for the given value.
     * This is used by convenience constructors.
     *
     * @param  <E>     type of value.
     * @param  value   the value for which to return a literal.
     * @return literal for the given value.
     */
    static <E> Literal<Feature,E> literal(final E value) {
        return FF().literal(value);
    }

    /**
     * Returns the given expression if non-null, or {@link #FALSE} otherwise.
     */
    static Expression<Feature,Boolean> defaultToFalse(Expression<Feature,Boolean> value) {
        return (value != null) ? value : FALSE;
    }

    /**
     * Returns the given expression if non-null, or {@link #TRUE} otherwise.
     */
    static Expression<Feature,Boolean> defaultToTrue(Expression<Feature,Boolean> value) {
        return (value != null) ? value : TRUE;
    }

    /**
     * Returns the given expression if non-null, or {@link #LITERAL_ZERO} otherwise.
     */
    static Expression<Feature, ? extends Number> defaultToZero(Expression<Feature, ? extends Number> value) {
        return (value != null) ? value : LITERAL_ZERO;
    }

    /**
     * Returns the given expression if non-null, or {@link #LITERAL_ONE} otherwise.
     */
    static Expression<Feature, ? extends Number> defaultToOne(Expression<Feature, ? extends Number> value) {
        return (value != null) ? value : LITERAL_ONE;
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
    public StyleElement clone() {
        try {
            return (StyleElement) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);    // Should never happen since we are cloneable.
        }
    }
}
