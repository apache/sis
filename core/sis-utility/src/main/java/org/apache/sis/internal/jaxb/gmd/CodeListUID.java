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
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import org.opengis.util.CodeList;
import org.apache.sis.util.iso.Types;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.Schemas;


/**
 * Stores information about {@link CodeList} in order to marshal in the way defined by ISO-19139.
 * This class provides the {@link #codeList} and {@link #codeListValue} attributes to be marshalled.
 * Those attributes should be unique for each code.
 *
 * <div class="note">"UID" in the class name stands for "Unique Identifier".</div>
 *
 * This object is wrapped by {@link CodeListAdapter} or, in the special case of {@link Locale} type,
 * by {@link LanguageCode} or {@link Country}.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 *
 * @see CodeListAdapter
 */
@XmlType(name = "CodeList", propOrder = {"codeList", "codeListValue", "codeSpace"})
public final class CodeListUID {
    /**
     * Returns the URL to a given code list in the given XML file.
     * This method concatenates the base schema URL with the given identifier.
     * Some examples of strings returned by this method are:
     *
     * <ul>
     *   <li>{@code "http://schemas.opengis.net/iso/19139/20070417/resources/Codelist/gmxCodelists.xml#LanguageCode"}</li>
     *   <li>{@code "http://schemas.opengis.net/iso/19139/20070417/resources/Codelist/gmxCodelists.xml#MD_CharacterSetCode"}</li>
     *   <li>{@code "http://schemas.opengis.net/iso/19139/20070417/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode"}</li>
     * </ul>
     *
     * @param  context    The current (un)marshalling context, or {@code null} if none.
     * @param  identifier The UML identifier of the code list.
     * @return The URL to the given code list in the given schema.
     */
    private static String schema(final Context context, final String identifier) {
        return Context.schema(context, "gmd", Schemas.METADATA_ROOT)
                .append(Schemas.CODELISTS_PATH) // Future SIS version may switch between localized/unlocalized file.
                .append('#').append(identifier).toString();
    }

    /**
     * The {@code codeList} attribute in the XML element.
     */
    @XmlAttribute(required = true)
    public String codeList;

    /**
     * The {@code codeListValue} attribute in the XML element.
     */
    @XmlAttribute(required = true)
    public String codeListValue;

    /**
     * The optional {@code codeSpace} attribute in the XML element. The default value is
     * {@code null}. If a value is provided in this field, then {@link #value} should be
     * set as well.
     *
     * <p>This attribute is set to the 3-letters language code of the {@link #value} attribute,
     * as returned by {@link Locale#getISO3Language()}.</p>
     */
    @XmlAttribute
    public String codeSpace;

    /**
     * The optional value to write in the XML element. The default value is {@code null}.
     * If a value is provided in this field, then {@link #codeSpace} is the language code
     * of this field or {@code null} for English.
     */
    @XmlValue
    public String value;

    /**
     * Default empty constructor for JAXB.
     */
    CodeListUID() {
    }

    /**
     * Builds a code list with the given attributes.
     *
     * @param context       The current (un)marshalling context, or {@code null} if none.
     * @param codeList      The {@code codeList} attribute, to be concatenated after the {@code "#"} symbol.
     * @param codeListValue The {@code codeListValue} attribute, to be declared in the XML element.
     * @param codeSpace     The 3-letters language code of the {@code value} attribute, or {@code null} if none.
     * @param value         The value in the language specified by the {@code codeSpace} attribute, or {@code null} if none.
     */
    public CodeListUID(final Context context, final String codeList, final String codeListValue,
            final String codeSpace, final String value)
    {
        this.codeList      = schema(context, codeList);
        this.codeListValue = codeListValue;
        this.codeSpace     = codeSpace;
        this.value         = value;
    }

    /**
     * Builds a value for {@link CodeListAdapter} elements.
     * This constructors stores the values that will be used for marshalling.
     *
     * @param context The current (un)marshalling context, or {@code null} if none.
     * @param code    The code list to wrap.
     */
    public CodeListUID(final Context context, final CodeList<?> code) {
        final String classID = Types.getListName(code);
        final String fieldID = Types.getCodeName(code);
        codeList = schema(context, classID);
        /*
         * Get the localized name of the field identifier, if possible.
         * This code partially duplicates Types.getCodeTitle(CodeList).
         * This duplication exists because this constructor stores more information in
         * an opportunist way. If this constructor is updated, please consider updating
         * the Types.getCodeTitle(CodeList) method accordingly.
         */
        final Locale locale = context.getLocale();
        if (locale != null) {
            final String key = classID + '.' + fieldID;
            try {
                value = ResourceBundle.getBundle("org.opengis.metadata.CodeLists",
                        locale, CodeList.class.getClassLoader()).getString(key);
            } catch (MissingResourceException e) {
                Context.warningOccured(context, CodeListAdapter.class, "marshal", e, false);
            }
        }
        if (value != null) {
            codeSpace = Context.converter(context).toLanguageCode(context, locale);
        } else {
            // Fallback when no value is defined for the code list. Build a value from the
            // most descriptive name (excluding the field name), which is usually the UML
            // name except for CharacterSet in which case it is a string like "UTF-8".
            value = Types.getCodeLabel(code);
        }
        codeListValue = fieldID;
    }

    /**
     * Returns the identifier to use for fetching a {@link CodeList} instance.
     * This is normally the {@link #codeListValue} attribute. However if the
     * code list is actually used as an enumeration, then the above attribute
     * is null and we have to use directly the {@linkplain #value} instead.
     *
     * @return The identifier to be given to the {@code CodeList.valueOf(…)} method.
     */
    @Override
    public String toString() {
        String id = codeListValue;
        if (id == null) {
            id = value;
        }
        return id;
    }
}
