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
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.gmx.Anchor;
import org.apache.sis.internal.jaxb.gmx.FileName;
import org.apache.sis.internal.jaxb.gmx.MimeFileType;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Messages;


/**
 * JAXB wrapper for string value in a {@code <gco:CharacterString>}, {@code <gmx:Anchor>},
 * {@code <gmx:FileName>} or {@code <gmx:MimeFileType>} element, for ISO-19139 compliance.
 *
 * <p>{@code FileName} and {@code MimeFileType} are possible substitutions for {@code CharacterString}.
 * They make sense only in {@link org.apache.sis.metadata.iso.identification.DefaultBrowseGraphic} or
 * other classes using URI, but the XML schema does not prevent their usage in place of other strings.
 * Consequently we unconditionally accept {@code FileName} and {@code MimeFileType} at unmarshalling time.
 * However marshalling will use the appropriate element for the kind of property to marshal.</p>
 *
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.4
 * @module
 *
 * @see org.apache.sis.internal.jaxb.gmd.PT_FreeText
 */
@XmlType(name = "CharacterString_PropertyType")
@XmlSeeAlso({
    org.apache.sis.internal.jaxb.gmd.PT_FreeText.class,
    org.apache.sis.internal.jaxb.gmd.LanguageCode.class,
    org.apache.sis.internal.jaxb.gmd.Country.class
})
public class GO_CharacterString {
    /*
     * Numerical values below are ordered: if two or more values are defined (thoerically not legal,
     * but we try to be robust), the value associated to the highest constant has precedence.
     */
    /**
     * Value assigned to {@link #type} if the character string
     * shall be marshalled as a {@code <gmx:MimeFileType>} element.
     */
    public static final byte MIME_TYPE = 1;

    /**
     * Value assigned to {@link #type} if the character string
     * shall be marshalled as a {@code <gmx:FileName>} element.
     */
    public static final byte FILENAME = 2;

    /**
     * Value assigned to {@link #type} if the current {@link #text}
     * has been found in a {@code <gco:CharacterString>} element.
     */
    private static final byte ANCHOR = 3;

    /**
     * The XML element names for each possible {@link #type} values.
     */
    private static final String[] NAMES = new String[4];
    static {
        NAMES[0]         = "CharacterString";
        NAMES[MIME_TYPE] = "MimeFileType";
        NAMES[FILENAME]  = "FileName";
        NAMES[ANCHOR]    = "Anchor";
    }

    /**
     * The text or anchor value, or {@code null} if none. May be an instance
     * of {@link Anchor}, which needs to be handled in a special way.
     */
    private CharSequence text;

    /**
     * 0 if the text shall be marshalled as a {@code <gco:CharacterString>} or an anchor,
     * or one of the static constants in this class otherwise.
     *
     * @see #FILENAME
     * @see #MIME_TYPE
     */
    public byte type;

    /**
     * Empty constructor for JAXB and subclasses.
     */
    public GO_CharacterString() {
    }

    /**
     * Builds a wrapper for the given text.
     *
     * @param text The string to marshal, or {@code null} if none.
     */
    protected GO_CharacterString(final CharSequence text) {
        this.text = text;
    }

    /**
     * Sets the {@link #text} field to the given value.
     * If the given value overwrites a previous one, a warning is emitted.
     *
     * @param value    The value to set.
     * @param property 0 or one of the {@link #MIME_TYPE}, {@link #FILENAME} or {@link #ANCHOR} constants.
     */
    private void setText(final CharSequence value, byte property) {
        if (text != null && !value.equals(text)) {
            /*
             * The given value overwrite a previous one. Determine which value will be discarded
             * using the 'type' value as a criterion, then emit a warning.
             */
            byte discarded = type;
            boolean noset = false;
            if (discarded > property) {
                discarded = property;
                property  = type;
                noset     = true;
            }
            Context.warningOccured(Context.current(), value, getClass(), "setText",
                    Messages.class, Messages.Keys.DiscardedExclusiveProperty_2, NAMES[discarded], NAMES[property]);
            if (noset) {
                return;
            }
        }
        text = value;
        type = property;
    }

    /**
     * Returns the text in a {@code <gco:CharacterString>} element, or {@code null} if none.
     *
     * @return The text, or {@code null}.
     */
    @XmlElement(name = "CharacterString")
    public final String getCharacterString() {
        if (type == 0) {
            final CharSequence text = this.text;
            if (text != null && !(text instanceof Anchor)) {
                return text.toString();
            }
        }
        return null;
    }

    /**
     * Sets the value to the given string. This method is called by JAXB at unmarshalling time.
     *
     * @param value The new text.
     */
    public final void setCharacterString(String value) {
        value = CharSequences.trimWhitespaces(value);
        if (value != null && !value.isEmpty()) {
            setText(value, (byte) 0);
        }
    }

    /**
     * Returns the text in a {@code <gmx:FileName>} element, or {@code null} if none.
     */
    @XmlElement(name = "FileName", namespace = Namespaces.GMX)
    final FileName getFileName() {
        if (type == FILENAME) {
            final CharSequence text = this.text;
            if (text != null && !(text instanceof Anchor)) {
                return new FileName(text.toString());
            }
        }
        return null;
    }

    /**
     * Invoked by JAXB for setting the filename.
     */
    final void setFileName(final FileName file) {
        if (file != null) {
            final String value = CharSequences.trimWhitespaces(file.toString());
            if (value != null && !value.isEmpty()) {
                setText(value, FILENAME);
            }
        }
    }

    /**
     * Returns the text in a {@code <gmx:MimeFileType>} element, or {@code null} if none.
     */
    @XmlElement(name = "MimeFileType", namespace = Namespaces.GMX)
    final MimeFileType getMimeFileType() {
        if (type == MIME_TYPE) {
            final CharSequence text = this.text;
            if (text != null && !(text instanceof Anchor)) {
                return new MimeFileType(text.toString());
            }
        }
        return null;
    }

    /**
     * Invoked by JAXB for setting the MIME type.
     */
    final void setMimeFileType(final MimeFileType type) {
        if (type != null) {
            final String value = CharSequences.trimWhitespaces(type.toString());
            if (value != null && !value.isEmpty()) {
                setText(value, MIME_TYPE);
            }
        }
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
        setText(anchor, ANCHOR);
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
     *
     * @return The text as a string (may be null).
     */
    @Override
    public final String toString() {
        final CharSequence text = this.text;
        return (text != null) ? text.toString() : null; // NOSONAR: Really want to return null.
    }
}
