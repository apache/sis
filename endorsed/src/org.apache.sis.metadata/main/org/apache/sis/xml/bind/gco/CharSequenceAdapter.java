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
package org.apache.sis.xml.bind.gco;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.opengis.util.InternationalString;
import org.apache.sis.util.CharSequences;
import org.apache.sis.xml.XLink;
import org.apache.sis.xml.ReferenceResolver;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.xml.bind.gcx.Anchor;
import org.apache.sis.xml.bind.lan.PT_FreeText;


/**
 * JAXB adapter wrapping the string value in a {@code <gco:CharacterString>} element, for ISO 19115-3 compliance.
 * A {@link CharSequenceAdapter} can handle the following types:
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
 *
 * @see StringAdapter
 * @see InternationalStringAdapter
 */
public class CharSequenceAdapter extends XmlAdapter<GO_CharacterString, CharSequence> {
    /**
     * Constructor for JAXB only.
     */
    public CharSequenceAdapter() {
    }

    /**
     * Converts a string read from a XML stream to the object containing the value.
     * JAXB calls automatically this method at unmarshalling time.
     *
     * @param  value  the adapter for this metadata value.
     * @return a {@link CharSequence} which represents the metadata value.
     */
    @Override
    public final CharSequence unmarshal(final GO_CharacterString value) {
        return (value != null) ? value.toCharSequence() : null;
    }

    /**
     * Converts a {@linkplain CharSequence character sequence} to the object to be marshalled
     * in a XML file or stream. JAXB calls automatically this method at marshalling time.
     *
     * @param  value  the string value.
     * @return the wrapper for the given character sequence, or {@code null}.
     */
    @Override
    public GO_CharacterString marshal(final CharSequence value) {
        return wrap(value);
    }

    /**
     * Converts a {@linkplain CharSequence character sequence} to the object to be marshalled
     * in a XML file or stream.
     *
     * @param  value  the character representation of the object being marshalled.
     * @return the wrapper for the given character sequence, or {@code null}.
     */
    static GO_CharacterString wrap(CharSequence value) {
        if (value instanceof String) {
            return wrap(Context.current(), value, (String) value);  // Slightly more efficient variant of this method.
        }
        /*
         * <mdb:someElement xsi:type="lan:PT_FreeText_PropertyType">
         *   <gco:CharacterString>...</gco:CharacterString>
         *   <lan:PT_FreeText>
         *     ... see PT_FreeText ...
         *   </lan:PT_FreeText>
         * </mdb:someElement>
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
         * Substitute <gco:CharacterString> by <gcx:Anchor> if a linkage is found.
         */
        if (!(value instanceof Anchor)) {
            final String key = Strings.trimOrNull(value.toString());
            if (key != null) {
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
         * │ <mdb:someElement>                                │ <mdb:someElement>              │
         * │   <gco:CharacterString>...</gco:CharacterString> │   <gcx:Anchor>...</gcx:Anchor> │
         * │ </mdb:someElement>                               │ </mdb:someElement>             │
         * └──────────────────────────────────────────────────┴────────────────────────────────┘
         */
        return new GO_CharacterString(value);
    }

    /**
     * Converts the string representation of an object to be marshalled in a XML file or stream.
     * This method is a copy of {@link #wrap(CharSequence)} simplified for the case when we know
     * that the character sequence being marshalled is a string.
     *
     * @param  context  the current (un)marshalling context, or {@code null} if none.
     * @param  object   the object being marshalled (e.g. {@code URI} or {@code Locale}).
     * @param  string   the string representation of the object being marshalled.
     * @return the wrapper for the given character sequence, or {@code null}.
     */
    public static GO_CharacterString wrap(final Context context, final Object object, final String string) {
        final CharSequence text = value(context, object, string);
        return (text != null) ? new GO_CharacterString(text) : null;
    }

    /**
     * Same as {@link #wrap(Context, Object, String)}, but returns directly the {@link GO_CharacterString#text}
     * value without wrapping in a {@code GO_CharacterString} instance.
     *
     * @param  context  the current (un)marshalling context, or {@code null} if none.
     * @param  object   the object being marshalled (e.g. {@code URI} or {@code Locale}).
     * @param  string   the string representation of the object being marshalled.
     * @return the text value for the given character sequence, or {@code null}.
     */
    public static CharSequence value(final Context context, final Object object, String string) {
        string = Strings.trimOrNull(string);
        if (string != null) {
            final XLink linkage = Context.resolver(context).anchor(context, object, string);
            if (linkage != null) {
                if (linkage instanceof Anchor) {
                    return (Anchor) linkage;
                } else {
                    return new Anchor(linkage, string);
                }
            }
        }
        return string;
    }

    /**
     * Wraps the value only if marshalling ISO 19115-3 element.
     * Otherwise (i.e. if marshalling a legacy ISO 19139:2007 document), omit the element.
     */
    public static final class Since2014 extends CharSequenceAdapter {
        /** Empty constructor used only by JAXB. */
        public Since2014() {
        }

        /**
         * Wraps the given value in an ISO 19115-3 element, unless we are marshalling an older document.
         *
         * @return a non-null value only if marshalling ISO 19115-3 or newer.
         */
        @Override public GO_CharacterString marshal(final CharSequence value) {
            return FilterByVersion.CURRENT_METADATA.accept() ? super.marshal(value) : null;
        }
    }
}
