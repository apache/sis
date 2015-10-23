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
package org.apache.sis.internal.jaxb.gmd;

import java.util.Set;
import java.util.Locale;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import org.opengis.util.InternationalString;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.gco.GO_CharacterString;
import org.apache.sis.util.iso.DefaultInternationalString;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.ArraysExt;


/**
 * JAXB wrapper for ISO-19139 {@code <PT_FreeText>} element mapped to {@link InternationalString}.
 * It will be used in order to marshal and unmarshal international strings localized in several
 * language, using the {@link DefaultInternationalString} implementation class. Example:
 *
 * {@preformat xml
 *   <gmd:title xsi:type="gmd:PT_FreeText_PropertyType">
 *     <gco:CharacterString>Some title in english is present in this node</gco:CharacterString>
 *     <gmd:PT_FreeText>
 *       <gmd:textGroup>
 *         <gmd:LocalisedCharacterString locale="#locale-fra">Un titre en français</gmd:LocalisedCharacterString>
 *       </gmd:textGroup>
 *     </gmd:PT_FreeText>
 *   </gmd:title>
 * }
 *
 * If there is more than one locale, the whole {@code <gmd:textGroup>} block is repeated for each
 * locale, instead than repeating {@code <gmd:LocalisedCharacterString>} inside the same group as
 * we could expect. However at unmarshalling time, both forms are accepted. See GEOTK-152 for more
 * information.
 *
 * <p>The {@code <gco:CharacterString>} element is inherited from the {@link GO_CharacterString}
 * parent class.</p>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.6
 * @module
 *
 * @see <a href="http://jira.geotoolkit.org/browse/GEOTK-152">GEOTK-152</a>
 */
@XmlType(name = "PT_FreeText_PropertyType")
public final class PT_FreeText extends GO_CharacterString {
    /**
     * A set of {@link LocalisedCharacterString}, representing the {@code <gmd:textGroup>} element.
     * The array shall contain one element for each locale.
     *
     * @see <a href="http://jira.geotoolkit.org/browse/GEOTK-152">GEOTK-152</a>
     */
    @XmlElementWrapper(name = "PT_FreeText")
    @XmlElement(required = true)
    private TextGroup[] textGroup;

    /**
     * Empty constructor used only by JAXB.
     */
    private PT_FreeText() {
    }

    /**
     * Constructs a {@code PT_FreeText} containing the given text groups.
     *
     * <p>The {@code <gco:CharacterString>} element will typically be set for the {@link Locale#ROOT},
     * which is the "unlocalized" string (not the same thing than the string in the default locale).
     * Note that the {@link TextGroup} constructor works better if the {@code <gco:CharacterString>}
     * have been set for the {@code ROOT} locale (the default behavior). If a different locale were
     * set, the list of localized strings in {@code TextGroup} may contains an element which
     * duplicate the {@code <gco:CharacterString>} element, or the unlocalized string normally
     * written in {@code <gco:CharacterString>} may be missing.</p>
     *
     * @param text The text to write in the {@code <gco:CharacterString>} element.
     * @param textGroup The text group elements.
     *
     * @see org.apache.sis.xml.XML#LOCALE
     */
    private PT_FreeText(final String text, final TextGroup[] textGroup) {
        super(text);
        this.textGroup = textGroup;
    }

    /**
     * Constructs a {@linkplain TextGroup text group} from the given {@link InternationalString}
     * if it contains at least one non-root locale. Otherwise returns {@code null}, meaning that
     * the simpler {@link GO_CharacterString} construct should be used instead.
     *
     * @param text An international string which could have several translations embedded for the same text.
     * @return A {@code PT_FreeText} instance if the given text has several translations, or {@code null} otherwise.
     */
    public static PT_FreeText create(final InternationalString text) {
        if (text instanceof DefaultInternationalString) {
            final DefaultInternationalString df = (DefaultInternationalString) text;
            final Set<Locale> locales = df.getLocales();
            final TextGroup[] textGroup = new TextGroup[locales.size()];
            int n = 0;
            for (final Locale locale : locales) {
                if (locale != null && !locale.equals(Locale.ROOT)) {
                    textGroup[n++] = new TextGroup(locale, text.toString(locale));
                }
            }
            if (n != 0) {
                /*
                 * Invoke toString(Locale) instead than toString() even if the locale is null,
                 * since the desired fallback is typically Locale.ROOT instead than the system
                 * default. It is usually safer to avoid null value, but in this particular case
                 * the implementation (DefaultInternationalString) is known to support null.
                 */
                final Context context = Context.current();
                return new PT_FreeText(df.toString(context != null ? context.getLocale() : null),
                        ArraysExt.resize(textGroup, n));
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if this {@code PT_FreeText} contains the given localized text.
     * This method searches only in the localized text. The content of the {@link #text}
     * field is intentionally omitted since it is usually the text we are searching for!
     * (this method is used for detecting duplicated values).
     *
     * @param  search The text to search (usually the {@link #text} value).
     * @return {@code true} if the given text has been found.
     */
    private boolean contains(final String search) {
        final TextGroup[] textGroup = this.textGroup;
        if (textGroup != null) {
            for (final TextGroup group : textGroup) {
                if (group != null) {
                    final LocalisedCharacterString[] localised = group.localized;
                    if (localised != null) {
                        for (final LocalisedCharacterString candidate : localised) {
                            if (search.equals(candidate.text)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns the content of this {@code <gco:CharacterString>} as an {@code InternationalString}.
     *
     * @return The character sequence for this {@code <gco:CharacterString>}.
     */
    @Override
    protected CharSequence toCharSequence() {
        String defaultValue = toString(); // May be null.
        if (defaultValue != null && contains(defaultValue)) {
            /*
             * If the <gco:CharacterString> value is repeated in one of the
             * <gmd:LocalisedCharacterString> elements, keep only the localized
             * version  (because it specifies the locale, while the unlocalized
             * string saids nothing on that matter).
             */
            defaultValue = null;
        }
        /*
         * Create the international string with all locales found in the <gml:textGroup>
         * element. If the <gml:textGroup> element is missing or empty, then we will use
         * an instance of SimpleInternationalString instead than the more heavy
         * DefaultInternationalString.
         */
        DefaultInternationalString i18n = null;
        final TextGroup[] textGroup = this.textGroup;
        if (textGroup != null) {
            for (final TextGroup group : textGroup) {
                if (group != null) {
                    final LocalisedCharacterString[] localised = group.localized;
                    if (localised != null) {
                        for (final LocalisedCharacterString text : localised) {
                            if (text != null) {
                                if (i18n == null) {
                                    i18n = new DefaultInternationalString(defaultValue);
                                }
                                i18n.add(text.locale, text.text);
                            }
                        }
                    }
                }
            }
        }
        if (i18n == null && defaultValue != null) {
            return new SimpleInternationalString(defaultValue);
        }
        return i18n;
    }
}
