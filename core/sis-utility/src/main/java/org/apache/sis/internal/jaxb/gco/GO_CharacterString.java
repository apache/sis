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
package org.apache.sis.internal.jaxb.gco;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import org.apache.sis.xml.Namespaces;
import org.apache.sis.internal.jaxb.gmd.PT_FreeText;
import org.apache.sis.internal.jaxb.gmd.LanguageCode;
import org.apache.sis.internal.jaxb.gmd.Country;
import org.apache.sis.internal.jaxb.gmx.Anchor;
import org.apache.sis.util.CharSequences;


/**
 * JAXB wrapper for string value in a {@code <gco:CharacterString>} or {@code <gmx:Anchor>} element,
 * for ISO-19139 compliance.
 *
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.3
 * @module
 *
 * @see org.apache.sis.internal.jaxb.gmd.PT_FreeText
 */
@XmlType(name = "CharacterString_PropertyType")
@XmlSeeAlso({PT_FreeText.class, LanguageCode.class, Country.class})
public class GO_CharacterString {
    /**
     * The text or anchor value, or {@code null} if none. May be an instance
     * of {@link Anchor}, which needs to be handled in a special way.
     */
    private CharSequence text;

    /**
     * Empty constructor for JAXB and subclasses.
     */
    protected GO_CharacterString() {
    }

    /**
     * Builds an wrapper for the given text.
     *
     * @param text The string to marshal, or {@code null} if none.
     */
    protected GO_CharacterString(final CharSequence text) {
        this.text = text;
    }

    /**
     * Builds an wrapper as a copy of the given one.
     *
     * @param text The wrapper to copy, or {@code null} if none.
     */
    protected GO_CharacterString(final GO_CharacterString text) {
        if (text != null) {
            this.text = text.text;
        }
    }

    /**
     * Returns the text. This method is called by JAXB at marshalling time.
     *
     * @return The text, or {@code null}.
     */
    @XmlElement(name = "CharacterString")
    public final String getCharacterString() {
        final CharSequence text = this.text;
        return (text == null || text instanceof Anchor) ? null : text.toString();
    }

    /**
     * Sets the value to the given string. This method is called by JAXB at unmarshalling time.
     *
     * @param text The new text.
     */
    public final void setCharacterString(String text) {
        text = CharSequences.trimWhitespaces(text);
        if (text != null && text.isEmpty()) {
            text = null;
        }
        this.text = text;
    }

    /**
     * Returns the text associated with a reference.
     * This method is called by JAXB at marshalling time.
     *
     * @return The anchor, or {@code null}.
     */
    @XmlElement(name = "Anchor", namespace = Namespaces.GMX)
    public final Anchor getAnchor() {
        final CharSequence text = this.text;
        return (text instanceof Anchor) ? (Anchor) text : null;
    }

    /**
     * Sets the value for the metadata string.
     * This method is called by JAXB at unmarshalling time.
     *
     * @param anchor The new anchor.
     */
    public final void setAnchor(final Anchor anchor) {
        this.text = anchor;
    }

    /**
     * Returns the content of this {@code <gco:CharacterString>} as a {@code String},
     * an {@code InternationalString} or an {@code Anchor}. This method is overridden
     * by {@code PT_FreeText} in order to handle the international string case.
     *
     * @return The character sequence for this {@code <gco:CharacterString>}.
     */
    public CharSequence toCharSequence() {
        final CharSequence text = CharSequences.trimWhitespaces(this.text);
        if (text != null && (text.length() != 0 || text instanceof Anchor)) { // Anchor may contain attributes.
            return text;
        }
        return null;
    }

    /**
     * Returns the text as a string, or {@code null} if none.
     * The null value is expected by various {@code PT_FreeText}.
     *
     * {@note Returning <code>null</code> is unusual and not a recommended practice.
     * But precedents exist (for example Swing <code>DefaultMutableTreeNode</code>)
     * and this class is not for public usage.}
     */
    @Override
    public final String toString() {
        final CharSequence text = this.text;
        return (text != null) ? text.toString() : null; // NOSONAR: Really want to return null.
    }
}
