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
package org.apache.sis.internal.jaxb.gmx;

import java.net.URI;
import java.util.Locale;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import org.opengis.util.InternationalString;
import org.apache.sis.xml.XLink;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * The {@code Anchor} element, which is included in {@code CharacterString} elements.
 * In XML documents,  anchors are values with {@code XLink} attributes used in places
 * where we would normally expect a character sequence. Since Java properties of type
 * {@code CharSequence} can not return {@code XLink},  we workaround that restriction
 * by providing this {@code Anchor} class as a {@code XLink} subtype implementing the
 * {@link InternationalString} interface, so it can be used with the above-cited Java
 * properties.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@XmlType(name = "Anchor_Type")
public final class Anchor extends XLink implements InternationalString {
    /**
     * Defined as a matter of principle (this class is not expected to be serialized).
     */
    private static final long serialVersionUID = -7896134857052775101L;

    /**
     * Often a short textual description of the URI target.
     * This is the value returned by {@link #toString()}.
     */
    @XmlValue
    private String value;

    /**
     * Creates a uninitialized {@code Anchor}.
     * This constructor is required by JAXB.
     */
    public Anchor() {
    }

    /**
     * Creates an {@code Anchor} initialized to the given {@code xlink} value.
     *
     * @param xlink The {@code xlink} from which to copy the attributes.
     * @param value Often a short textual description of the URI target.
     */
    public Anchor(final XLink xlink, final String value) {
        super(xlink);
        this.value = value;
    }

    /**
     * Creates an {@code Anchor} initialized to the given {@code href} value.
     *
     * @param href  A URI to an external resources or an identifier.
     * @param value Often a short textual description of the URI target.
     */
    public Anchor(final URI href, final String value) {
        setHRef(href);
        this.value = value;
    }

    /**
     * Returns the text as a string, or {@code null} if none. The null value is needed for proper
     * working of {@link org.apache.sis.internal.jaxb.gco.GO_CharacterString#toString()} method.
     */
    @Override
    public String toString() {
        return value;
    }

    /**
     * Returns the text as a string, or {@code null} if none.
     *
     * @param  locale Ignored in current implementation.
     * @return The anchor text, or {@code null} if none.
     */
    @Override
    public String toString(final Locale locale) {
        return value;
    }

    /**
     * Returns the number of characters in the value.
     */
    @Override
    public int length() {
        return (value != null) ? value.length() : 0;
    }

    /**
     * Returns the character at the given index.
     */
    @Override
    public char charAt(final int index) {
        return (value != null ? value : "").charAt(index);
    }

    /**
     * Returns the sequence of characters in the given range of index.
     * The returned object is an anchor with the same attribute values.
     * It is caller responsibility to determine if those attributes are still
     * appropriate for the sub-sequence.
     */
    @Override
    public CharSequence subSequence(final int start, final int end) {
        String original = value;
        if (original == null) {
            original = "";
        }
        final String substring = original.substring(start, end);
        if (substring == original) { // Identity comparison is ok here.
            return this;
        }
        return new Anchor(this, substring);
    }

    /**
     * Compares the value of this object with the given international string for order.
     * Null values are sorted last.
     *
     * @param other The string to compare with this anchor type.
     */
    @Override
    public int compareTo(final InternationalString other) {
        final String ot;
        if (other == null || (ot = other.toString()) == null) {
            return (value != null) ? -1 : 0;
        }
        return (value != null) ? value.compareTo(ot) : +1;
    }

    /**
     * Compares this {@code Anchor} with the given object for equality.
     *
     * @param object The object to compare with this anchor type.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (super.equals(object)) {
            final Anchor that = (Anchor) object;
            return Objects.equals(this.value, that.value);
        }
        return false;
    }

    /**
     * Returns a hash code value for this anchor type.
     */
    @Override
    public int hashCode() {
        return super.hashCode()*31 + Objects.hashCode(value);
    }
}
