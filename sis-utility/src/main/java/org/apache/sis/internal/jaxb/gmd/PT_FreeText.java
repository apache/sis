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
import org.apache.sis.internal.jaxb.MarshalContext;
import org.apache.sis.internal.jaxb.gco.GO_CharacterString;
import org.apache.sis.util.type.DefaultInternationalString;
import org.apache.sis.util.type.SimpleInternationalString;


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
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.3
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
    public PT_FreeText() {
    }

    /**
     * Constructs a {@linkplain TextGroup text group} from a {@link DefaultInternationalString}
     * which could contains several localized strings.
     *
     * <p>The {@code <gco:CharacterString> element will typically be set for the {@code null} locale,
     * which is the "unlocalized" string (not the same thing than the string in the default locale).
     * Note that the {@link TextGroup} constructor works better if the {@code <gco:CharacterString>}
     * have been set for the {@code null} locale (the default behavior). If a different locale were
     * set, the list of localized strings in {@code TextGroup} may contains an element which
     * duplicate the {@code <gco:CharacterString>} element, or the unlocalized string normally
     * written in {@code <gco:CharacterString>} may be missing.</p>
     *
     * @param context The current (un)marshalling context, or {@code null} if none.
     * @param text    An international string which could have several translations
     *                embedded for the same text.
     *
     * @see org.apache.sis.xml.XML#LOCALE
     */
    private PT_FreeText(final MarshalContext context, final DefaultInternationalString text) {
        super(text.toString(context != null ? context.getLocale() : null));
        final Set<Locale> locales = text.getLocales();
        int n = locales.size();
        if (locales.contains(null)) {
            n--;
        }
        textGroup = new TextGroup[n];
        int i = 0;
        for (final Locale locale : locales) {
            if (locale != null) {
                textGroup[i++] = new TextGroup(locale, text.toString(locale));
            }
        }
    }

    /**
     * Constructs a {@linkplain TextGroup text group} from the given {@link InternationalString}
     * if it contains at least one non-null locale. Otherwise returns {@code null}, meaning that
     * the simpler {@link GO_CharacterString} construct should be used instead.
     *
     * @param context The current (un)marshalling context, or {@code null} if none.
     * @param text    An international string which could have several translations
     *                embedded for the same text.
     * @return A {@code PT_FreeText} instance if the given text has several translations,
     *         or {@code null} otherwise.
     */
    @SuppressWarnings("fallthrough")
    public static PT_FreeText create(final MarshalContext context, final InternationalString text) {
        if (text instanceof DefaultInternationalString) {
            final DefaultInternationalString df = (DefaultInternationalString) text;
            final Set<Locale> locales = df.getLocales();
            switch (locales.size()) {
                case 0:  break;
                case 1:  if (locales.contains(null)) break; // Otherwise fallthrough
                default: return new PT_FreeText(context, df);
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
    public boolean contains(final String search) {
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
     * Returns the international string for this {@code PT_FreeText}.
     *
     * @param  defaultValue The unlocalized string to give to {@link DefaultInternationalString},
     *         or {@code null} if none.
     * @return The international string, or {@code null} if none.
     *         This is usually the {@link #text} value.
     */
    public InternationalString toInternationalString(final String defaultValue) {
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
