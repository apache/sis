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

import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.opengis.util.InternationalString;
import org.apache.sis.util.CharSequences;
import org.apache.sis.xml.XLink;
import org.apache.sis.xml.ReferenceResolver;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.gmx.Anchor;
import org.apache.sis.internal.jaxb.gmd.PT_FreeText;


/**
 * JAXB adapter in order to wrap the string value with a {@code <gco:CharacterString>} element,
 * for ISO-19139 compliance. A {@link CharSequenceAdapter} can handle the following types:
 *
 * <ul>
 *   <li>{@link InternationalString}, which may be mapped to {@link PT_FreeText} elements.</li>
 *   <li>{@link String} (actually any character sequences other than {@code InternationalString}).</li>
 *   <li>{@link Anchor}, which can be substituted to any of the above if the {@link ReferenceResolver}
 *       in the current marshalling context maps the given text to a {@code xlink}.</li>
 * </ul>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.6
 * @module
 *
 * @see StringAdapter
 * @see InternationalStringAdapter
 */
public final class CharSequenceAdapter extends XmlAdapter<GO_CharacterString, CharSequence> {
    /**
     * Constructor for JAXB only.
     */
    private CharSequenceAdapter() {
    }

    /**
     * Converts a string read from a XML stream to the object containing the value.
     * JAXB calls automatically this method at unmarshalling time.
     *
     * @param value The adapter for this metadata value.
     * @return A {@link CharSequence} which represents the metadata value.
     */
    @Override
    public CharSequence unmarshal(final GO_CharacterString value) {
        return (value != null) ? value.toCharSequence() : null;
    }

    /**
     * Converts a {@linkplain CharSequence character sequence} to the object to be marshalled
     * in a XML file or stream. JAXB calls automatically this method at marshalling time.
     *
     * @param  value The string value.
     * @return The wrapper for the given character sequence, or {@code null}.
     */
    @Override
    public GO_CharacterString marshal(final CharSequence value) {
        return wrap(value);
    }

    /**
     * Converts a {@linkplain CharSequence character sequence} to the object to be marshalled
     * in a XML file or stream.
     *
     * @param  value The character representation of the object being marshalled.
     * @return The wrapper for the given character sequence, or {@code null}.
     */
    static GO_CharacterString wrap(CharSequence value) {
        if (value instanceof String) {
            return wrap(Context.current(), value, (String) value);  // Slightly more efficient variant of this method.
        }
        /*
         * <gmd:someElement xsi:type="gmd:PT_FreeText_PropertyType">
         *   <gco:CharacterString>...</gco:CharacterString>
         *   <gmd:PT_FreeText>
         *     ... see PT_FreeText ...
         *   </gmd:PT_FreeText>
         * </gmd:someElement>
         */
        if (value instanceof InternationalString) {
            final PT_FreeText ft = PT_FreeText.create((InternationalString) value);
            if (ft != null) {
                return ft;
            }
        }
        /*
         * Invoking (indirectly) CharSequence.subSequence(…) may change the kind of object.
         * We know that Anchor is safe, and that most InternationalString implementations
         * lost the localized strings. This is why we trim the white spaces only here.
         */
        value = CharSequences.trimWhitespaces(value);
        if (value == null || value.length() == 0) {
            return null;
        }
        /*
         * Substitute <gco:CharacterString> by <gmx:Anchor> if a linkage is found.
         */
        if (!(value instanceof Anchor)) {
            final String key = CharSequences.trimWhitespaces(value.toString());
            if (key != null && !key.isEmpty()) {
                final Context context = Context.current();
                final XLink linkage = Context.resolver(context).anchor(context, value, key);
                if (linkage != null) {
                    if (linkage instanceof Anchor) {
                        value = (Anchor) linkage;
                    } else {
                        value = new Anchor(linkage, key);
                    }
                }
            }
        }
        /*
         * At this stage, the value (typically a String or InternationalString) may
         * have been replaced by an Anchor. The output will be one of the following:
         *
         * ┌──────────────────────────────────────────────────┬────────────────────────────────┐
         * │ <gmd:someElement>                                │ <gmd:someElement>              │
         * │   <gco:CharacterString>...</gco:CharacterString> │   <gmx:Anchor>...</gmx:Anchor> │
         * │ </gmd:someElement>                               │ </gmd:someElement>             │
         * └──────────────────────────────────────────────────┴────────────────────────────────┘
         */
        return new GO_CharacterString(value);
    }

    /**
     * Converts the string representation of an object to be marshalled in a XML file or stream.
     * This method is a copy of {@link #wrap(CharSequence)} simplified for the case when we know
     * that the character sequence being marshalled is a string.
     *
     * @param  context The current (un)marshalling context, or {@code null} if none.
     * @param  object  The object being marshalled (e.g. {@code URI} or {@code Locale}).
     * @param  string  The string representation of the object being marshalled.
     * @return The wrapper for the given character sequence, or {@code null}.
     */
    public static GO_CharacterString wrap(final Context context, final Object object, final String string) {
        final CharSequence text = value(context, object, string);
        return (text != null) ? new GO_CharacterString(text) : null;
    }

    /**
     * Same as {@link #wrap(Context, Object, String)}, but returns directly the {@link GO_CharacterString#text}
     * value without wrapping in a {@code GO_CharacterString} instance.
     *
     * @param  context The current (un)marshalling context, or {@code null} if none.
     * @param  object  The object being marshalled (e.g. {@code URI} or {@code Locale}).
     * @param  string  The string representation of the object being marshalled.
     * @return The text value for the given character sequence, or {@code null}.
     */
    public static CharSequence value(final Context context, final Object object, String string) {
        string = CharSequences.trimWhitespaces(string);
        if (string == null || string.isEmpty()) {
            return null;
        }
        final XLink linkage = Context.resolver(context).anchor(context, object, string);
        if (linkage != null) {
            if (linkage instanceof Anchor) {
                return (Anchor) linkage;
            } else {
                return new Anchor(linkage, string);
            }
        }
        return string;
    }
}
