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

import java.util.Locale;
import javax.xml.bind.annotation.XmlElement;
import org.apache.sis.util.Debug;

// Related to JDK7
import org.apache.sis.internal.jdk7.JDK7;


/**
 * A set of strings localized in different languages. This adapter represents the
 * {@code <gmd:textGroup>} element defined for embedded translations in ISO-19139
 * standard. See {@link PT_FreeText} class javadoc for an example.
 *
 * <p>If a localized string has a {@code null} locale, then this string will not be
 * included in this text group because that string should be already included in
 * the {@code <gco:CharacterString>} element of the parent {@link PT_FreeText}  (at
 * least in default behavior - actually the above may not be true anymore if the
 * marshaller {@link org.apache.sis.xml.XML#LOCALE} property has been set).</p>
 *
 * <p>The {@code TextGroup} name suggests that this object can contain many localized strings.
 * However it appears that despite its name, {@code TextGroup} shall always contains exactly 1
 * localized strings and the whole {@code TextGroup} element shall be repeated for each additional
 * languages. SIS uses the ISO 19139 compliant form for marshalling, but accepts both forms during
 * unmarshalling. More specifically, the name suggests that the format should be:</p>
 *
 * {@preformat xml
 *   <gco:CharacterString>Apache SIS, projet OpenSource</gco:CharacterString>
 *   <gmd:PT_FreeText>
 *     <gmd:textGroup>
 *       <gmd:LocalisedCharacterString locale="#locale-eng">Apache SIS, OpenSource Project</gmd:LocalisedCharacterString>
 *       <gmd:LocalisedCharacterString locale="#locale-ita">Apache SIS, progetto OpenSource</gmd:LocalisedCharacterString>
 *       <gmd:LocalisedCharacterString locale="#locale-fra">Apache SIS, projet OpenSource</gmd:LocalisedCharacterString>
 *     </gmd:textGroup>
 *   </gmd:PT_FreeText>
 * }
 *
 * But the actual official format is:
 *
 * {@preformat xml
 *   <gco:CharacterString>Apache SIS, projet OpenSource</gco:CharacterString>
 *   <gmd:PT_FreeText>
 *     <gmd:textGroup>
 *       <gmd:LocalisedCharacterString locale="#locale-eng">Apache SIS, OpenSource Project</gmd:LocalisedCharacterString>
 *     </gmd:textGroup>
 *     <gmd:textGroup>
 *       <gmd:LocalisedCharacterString locale="#locale-ita">Apache SIS, progetto OpenSource</gmd:LocalisedCharacterString>
 *     </gmd:textGroup>
 *     <gmd:textGroup>
 *       <gmd:LocalisedCharacterString locale="#locale-fra">Apache SIS, projet OpenSource</gmd:LocalisedCharacterString>
 *     </gmd:textGroup>
 *   </gmd:PT_FreeText>
 * }
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see LocalisedCharacterString
 * @see <a href="http://jira.geotoolkit.org/browse/GEOTK-152">GEOTK-152</a>
 */
final class TextGroup {
    /**
     * The set of {@linkplain LocalisedCharacterString localized string}.
     * JAXB uses this field at marshalling-time in order to wrap {@code N}
     * {@code <LocalisedCharacterString>} elements inside a single {@code <textGroup>} element.
     *
     * <p>In ISO 19139 compliant documents, the length of this array shall be exactly 1,
     * as in the second example of class javadoc. However SIS allows arbitrary length
     * (as in the first example of class javadoc) for compatibility and convenience reasons.</p>
     */
    @XmlElement(name = "LocalisedCharacterString")
    protected LocalisedCharacterString[] localized;

    /**
     * Empty constructor only used by JAXB.
     */
    public TextGroup() {
    }

    /**
     * Constructs a {@linkplain TextGroup text group} for a single locale. This constructor
     * puts exactly one string in the {@code TextGroup}, as required by ISO 19139. However
     * it would be possible to declare an other constructor allowing the more compact form
     * (the smaller ) if there is a need for that in the future.
     *
     * @param locale The string language.
     * @param text The string.
     */
    TextGroup(final Locale locale, final String text) {
        localized = new LocalisedCharacterString[] {
            new LocalisedCharacterString(locale, text)
        };
    }

    /**
     * Returns a string representation of this text group for debugging purpose.
     * Example:
     *
     * {@preformat text
     *   TextGroup
     *   ├─ LocalisedCharacterString[#locale-eng, “A text”]
     *   └─ LocalisedCharacterString[#locale-fra, “Un texte”]
     * }
     *
     * @see LocalisedCharacterString#toString()
     */
    @Debug
    @Override
    public String toString() {
        final String lineSeparator = JDK7.lineSeparator();
        final StringBuilder buffer = new StringBuilder(160).append(getClass().getSimpleName()).append(lineSeparator);
        if (localized != null) {
            int corner = 0;
            for (LocalisedCharacterString string : localized) {
                corner = buffer.length();
                buffer.append("├─ ").append(string).append(lineSeparator);
            }
            if (corner != 0) {
                buffer.setCharAt(corner, '└');
            }
        }
        return buffer.toString();
    }
}
