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
package org.apache.sis.referencing.operation;

import java.io.Serializable;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.operation.Formula;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.io.wkt.ElementKind;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.iso.Types;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Specification of the coordinate operation method formula.
 * A formula may be {@linkplain #getFormula() given textually},
 * or may be a {@linkplain #getCitation() reference to a publication}.
 *
 * <p>{@code Formula} is for human reading.
 * The object that actually does the work of applying formula to coordinate values is
 * {@link org.opengis.referencing.operation.MathTransform}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.5
 * @since   0.5
 * @module
 *
 * @see DefaultOperationMethod
 * @see org.apache.sis.referencing.operation.transform.AbstractMathTransform
 * @see org.apache.sis.referencing.operation.transform.MathTransformProvider
 */
public class DefaultFormula extends FormattableObject implements Formula, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1929966748615362698L;

    /**
     * Formula(s) or procedure used by the operation method.
     */
    private final InternationalString formula;

    /**
     * Reference to a publication giving the formula(s) or procedure used by the coordinate operation method.
     */
    private final Citation citation;

    /**
     * Creates a new formula. This constructor is not public because of {@code Formula} object should not have
     * both the formula literal and the citation. But we use this constructor an unmarshalling time if the XML
     * document have both. Having both is not valid GML, but SIS is tolerant to this situation.
     */
    DefaultFormula(final InternationalString formula, final Citation citation) {
        this.formula  = formula;
        this.citation = citation;
    }

    /**
     * Creates a new formula from the given string.
     *
     * @param formula The formula.
     */
    public DefaultFormula(final CharSequence formula) {
        ensureNonNull("formula", formula);
        this.formula = Types.toInternationalString(formula);
        this.citation = null;
    }

    /**
     * Creates a new formula from the given citation.
     *
     * @param citation The citation.
     */
    public DefaultFormula(final Citation citation) {
        ensureNonNull("citation", citation);
        this.citation = citation;
        this.formula  = null;
    }

    /**
     * Creates a new formula with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param formula The formula to copy.
     *
     * @see #castOrCopy(Formula)
     */
    protected DefaultFormula(final Formula formula) {
        ensureNonNull("formula", formula);
        this.citation = formula.getCitation();
        this.formula  = formula.getFormula();
    }

    /**
     * Returns a SIS formula implementation with the same values than the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultFormula castOrCopy(final Formula object) {
        return (object == null) || (object instanceof DefaultFormula)
               ? (DefaultFormula) object : new DefaultFormula(object);
    }

    /**
     * Returns the formula(s) or procedure used by the operation method, or {@code null} if none.
     */
    @Override
    public InternationalString getFormula() {
        return formula;
    }

    /**
     * Returns the reference to a publication giving the formula(s) or procedure used by the
     * coordinate operation method, or {@code null} if none.
     */
    @Override
    public Citation getCitation() {
        return citation;
    }

    /**
     * Returns a hash code value for this formula.
     */
    @Override
    public int hashCode() {
        int code = (int) serialVersionUID;
        if (formula  != null) code += formula .hashCode();
        if (citation != null) code += citation.hashCode() * 31;
        return code;
    }

    /**
     * Compares this formula with the given object for equality.
     *
     * @param  object The object to compare with this formula.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object != null && object.getClass() == getClass()) {
            final DefaultFormula that = (DefaultFormula) object;
            return Objects.equals(this.formula,  that.formula) &&
                   Objects.equals(this.citation, that.citation);
        }
        return false;
    }

    /**
     * Formats this formula as a pseudo-<cite>Well Known Text</cite> element.
     *
     * <div class="note"><b>Compatibility note:</b>
     * ISO 19162 does not define a WKT representation for {@code Formula} objects.
     * The text formatted by this method is SIS-specific and causes the text to be
     * flagged as {@linkplain Formatter#setInvalidWKT(Class, Exception) invalid WKT}.
     * The WKT content of this element may change in any future SIS version.</div>
     *
     * @return {@code "Formula"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        InternationalString text = null;
        final Citation citation = getCitation();    // Gives to users a chance to override properties.
        if (citation != null) {
            text = citation.getTitle();
        }
        if (text == null) {
            text = getFormula();
        }
        if (text != null) {
            formatter.append(text.toString(formatter.getLocale()), ElementKind.REMARKS);
        }
        formatter.setInvalidWKT(Formula.class, null);
        return WKTKeywords.Formula;
    }
}
