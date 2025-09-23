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
package org.apache.sis.xml.bind.cat;

import java.util.Locale;
import java.util.MissingResourceException;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import org.opengis.util.CodeList;
import org.apache.sis.util.iso.Types;
import org.apache.sis.xml.bind.Context;
import static org.apache.sis.metadata.internal.shared.ImplementationHelper.ISO_NAMESPACE;

// Specific to the main branch:
import java.util.ResourceBundle;
import org.apache.sis.util.internal.shared.CodeLists;


/**
 * Stores information about {@link CodeList} in order to marshal in the way defined by ISO 19115-3.
 * This class provides the {@link #codeList} and {@link #codeListValue} attributes to be marshalled.
 * Those attributes should be unique for each code.
 *
 * <div class="note">"UID" in the class name stands for "Unique Identifier".</div>
 *
 * This object is wrapped by {@link CodeListAdapter} or, in the special case of {@link Locale} type, by
 * {@link org.apache.sis.xml.bind.lan.LanguageCode} or {@link org.apache.sis.xml.bind.lan.Country}.
 *
 * <h2>Base URLs</h2>
 * This class defines constants for URL to schema directories or definition files used in code list elements.
 * SIS does not use those URL directly for downloading files (downloading an XML schema from ISO servers
 * require {@code "https"} protocol, but the URLs in this class use {@code "http"} for historical reasons).
 * Example:
 *
 * {@snippet lang="xml" :
 *   <gmi:MI_SensorTypeCode
 *       codeList="http://standards.iso.org/…snip…/codelists.xml#CI_SensorTypeCode"
 *       codeListValue="RADIOMETER">Radiometer</gmi:MI_SensorTypeCode>
 *   }
 *
 * <p>Constants in this class are organized in three groups:</p>
 * <ul>
 *   <li>Constants with the {@code _ROOT} suffix are {@code "http://"} URL to a root directory.</li>
 *   <li>Constants with the {@code _PATH} suffix are relative paths to concatenate to a {@code _ROOT}
 *       constant in order to get the full path to a file.</li>
 *   <li>Constants with the {@code _XSD} suffix are {@code "http://"} URL to a the XSD definition file.</li>
 * </ul>
 *
 * <h3>Note on multi-lingual files</h3>
 * Some files are available in two variants: with and without {@code "ML_"} prefix, which stands for "Multi Lingual".
 * Some examples are {@code "[ML_]gmxCodelists.xml"} and {@code "[ML_]gmxUom.xml"}. The following assumptions hold:
 *
 * <ul>
 *   <li>All code lists defined in a {@code ML_foo.xml} file exist also in {@code foo.xml}.</li>
 *   <li>The converse of above point is not necessarily true:
 *       the {@code ML_foo.xml} file may contain only a subset of {@code foo.xml}.</li>
 *   <li>All English descriptions in {@code ML_foo.xml} file are strictly identical to the ones in {@code foo.xml}.</li>
 *   <li>Descriptions in other languages than English exist only in {@code ML_foo.xml}.</li>
 * </ul>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 *
 * @see CodeListAdapter
 */
@XmlType(name = "CodeList", propOrder = {"codeList", "codeListValue", "codeSpace"})
public final class CodeListUID {
    /**
     * The namespace root of ISO 19115 objects.
     * This is the namespace used by default in Apache SIS.
     *
     * <h4>Historical note</h4>
     * The XSD files can be downloaded from that URL as well, but we sometimes experiment unexpected end of file.
     * The same files can be downloaded from <a href="https://schemas.isotc211.org/">ISO/TC 211 repository</a>,
     * which seems more stable. The TC 211 repository is used for downloads, while the standard ISO URL is sill
     * used for namespaces.
     */
    public static final String METADATA_ROOT = ISO_NAMESPACE + "19115/";

    /**
     * The root directory of OGC metadata schemas.
     * This is the schema used by default in Apache SIS.
     * Some alternatives to this URL are:
     *
     * <ul>
     *   <li>http://schemas.opengis.net/iso/19139/20070417/</li>
     *   <li>http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/</li>
     * </ul>
     */
    public static final String METADATA_ROOT_LEGACY = "http://www.isotc211.org/2005/";

    /**
     * The string to append to {@link #METADATA_ROOT} for obtaining the path to the definitions of code lists.
     */
    public static final String CODELISTS_PATH = "resources/Codelist/cat/codelists.xml";

    /**
     * The string to append to {@link #METADATA_ROOT_LEGACY} or one of its alternative for obtaining the path
     * to the definitions of code lists.
     *
     * <p>A localized version of this file exists also with the {@code "ML_gmxCodelists.xml"} filename
     * instead of {@code "gmxCodelists.xml"}</p>
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-154">SIS-154</a>
     */
    public static final String CODELISTS_PATH_LEGACY = "resources/Codelist/gmxCodelists.xml";

    /**
     * The string to append to {@link #METADATA_ROOT} or one of its alternative for obtaining the path
     * to the definitions of units of measurement.
     *
     * <p>A localized version of this file exists also with the {@code "ML_gmxUom.xml"} filename
     * instead of {@code "gmxUom.xml"}</p>
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-154">SIS-154</a>
     */
    public static final String UOM_PATH = "resources/uom/gmxUom.xml";

    /**
     * Returns the URL to a given code list in the given XML file.
     * This method concatenates the base schema URL with the given identifier.
     * Some examples of strings returned by this method are:
     *
     * <ul>
     *   <li>{@code "http://standards.iso.org/iso/19115/resources/Codelist/cat/codelists.xml#LanguageCode"}</li>
     *   <li>{@code "http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#LanguageCode"}</li>
     *   <li>{@code "http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#MD_CharacterSetCode"}</li>
     *   <li>{@code "http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode"}</li>
     * </ul>
     *
     * @param  context     the current (un)marshalling context, or {@code null} if none.
     * @param  identifier  the UML identifier of the code list.
     * @return the URL to the given code list in the given schema.
     *
     * @see org.apache.sis.xml.XML#SCHEMAS
     */
    private static String schema(final Context context, final String identifier) {
        final String prefix, root, path;
        if (Context.isFlagSet(context, Context.LEGACY_METADATA)) {
            prefix = "gmd";
            root = METADATA_ROOT_LEGACY;
            path = CODELISTS_PATH_LEGACY;       // Future SIS version may switch between localized/unlocalized file.
        } else {
            prefix = "cat";
            root = METADATA_ROOT;
            path = CODELISTS_PATH;
        }
        return Context.schema(context, prefix, root).append(path).append('#').append(identifier).toString();
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
     * <p>This attribute is set to the 3-letters language code of the {@linkplain #value},
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
    public CodeListUID() {
    }

    /**
     * Builds a code list with the given attributes.
     *
     * @param context        the current (un)marshalling context, or {@code null} if none.
     * @param codeList       the {@code codeList} attribute, to be concatenated after the {@code "#"} symbol.
     * @param codeListValue  the {@code codeListValue} attribute, to be declared in the XML element.
     * @param codeSpace      the 3-letters language code of the {@code value} attribute, or {@code null} if none.
     * @param value          the value in the language specified by the {@code codeSpace} attribute, or {@code null} if none.
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
     * @param context  the current (un)marshalling context, or {@code null} if none.
     * @param code     the code list to wrap.
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
                value = ResourceBundle.getBundle(CodeLists.RESOURCES,
                        locale, CodeList.class.getClassLoader()).getString(key);
            } catch (MissingResourceException e) {
                Context.warningOccured(context, CodeListAdapter.class, "marshal", e, false);
            }
        }
        if (value != null) {
            codeSpace = Context.converter(context).toLanguageCode(context, locale);
        } else {
            /*
             * Fallback when no value is defined for the code list. Build a value from the
             * most descriptive name (excluding the field name), which is usually the UML.
             */
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
     * @return the identifier to be given to the {@code CodeList.valueOf(…)} method.
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
