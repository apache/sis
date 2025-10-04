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
package org.apache.sis.util.iso;

import java.util.Map;
import java.util.SortedMap;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.IllformedLocaleException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.function.Function;
import java.io.IOException;
import org.opengis.annotation.UML;
import org.opengis.util.CodeList;
import org.opengis.util.InternationalString;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.DefaultInternationalString;
import org.apache.sis.util.ResourceInternationalString;
import org.apache.sis.util.Locales;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.OptionalCandidate;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.internal.shared.CodeLists;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.pending.jdk.JDK19;
import org.apache.sis.system.Modules;

// Specific to the main branch:
import java.io.InputStream;


/**
 * Static methods working on GeoAPI types and {@link CodeList} values.
 * This class provides:
 *
 * <ul>
 *   <li>Methods for fetching the ISO name or description of a code list:<ul>
 *     <li>{@link #getStandardName(Class)}   for ISO name</li>
 *     <li>{@link #getListName(CodeList)}    for ISO name</li>
 *     <li>{@link #getDescription(Class)}    for a description</li>
 *   </ul></li>
 *   <li>Methods for fetching the ISO name or description of a code value:<ul>
 *     <li>{@link #getCodeName(CodeList)}    for ISO name,</li>
 *     <li>{@link #getCodeTitle(CodeList)}   for a label or title</li>
 *     <li>{@link #getDescription(CodeList)} for a more verbose description</li>
 *   </ul></li>
 *   <li>Methods for fetching an instance from a name (converse of above {@code get} methods):<ul>
 *     <li>{@link #forCodeName(Class, String, Function)}</li>
 *     <li>{@link #forEnumName(Class, String)}</li>
 *   </ul></li>
 * </ul>
 *
 * <h2>Substituting a free text by a code list</h2>
 * The ISO standard allows to substitute some character strings in the <q>free text</q> domain
 * by a {@link CodeList} value. Such substitution can be done with:
 *
 * <ul>
 *   <li>{@link #getCodeTitle(CodeList)} for getting the {@link InternationalString} instance
 *       to store in a metadata property.</li>
 *   <li>{@link #forCodeTitle(CharSequence)} for retrieving the {@link CodeList} previously stored as an
 *       {@code InternationalString}.</li>
 * </ul>
 *
 * <h2>Example</h2>
 * In the following XML fragment, the {@code <mac:type>} value is normally a {@code <gco:CharacterString>}
 * but has been replaced by a {@code SensorType} code below:
 *
 * {@snippet lang="xml" :
 *   <mac:MI_Instrument>
 *     <mac:type>
 *       <gmi:MI_SensorTypeCode
 *           codeList="http://standards.iso.org/…snip…/codelists.xml#CI_SensorTypeCode"
 *           codeListValue="RADIOMETER">Radiometer</gmi:MI_SensorTypeCode>
 *     </mac:type>
 *   </mac:MI_Instrument>
 *   }
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 * @since   0.3
 */
public final class Types {
    /**
     * The separator character between class name and attribute name in resource files.
     */
    private static final char SEPARATOR = '.';

    /**
     * The logger for metadata.
     */
    private static final Logger LOGGER = Logger.getLogger(Modules.METADATA);

    /**
     * The types for ISO 19115 UML identifiers. The keys are UML identifiers.
     * Values are either class names as {@link String} objects, or the {@link Class} instances.
     * This map will be built only when first needed.
     *
     * @see #forStandardName(String)
     */
    private static Map<String,Object> typeForNames;

    /**
     * Do not allow instantiation of this class.
     */
    private Types() {
    }

    /**
     * Returns the ISO name for the given class, or {@code null} if none.
     * This method can be used for GeoAPI interfaces or {@link CodeList}.
     *
     * <h4>Examples</h4>
     * <ul>
     *   <li><code>getStandardName({@linkplain org.opengis.metadata.citation.Citation}.class)</code>
     *       (an interface) returns {@code "CI_Citation"}.</li>
     *   <li><code>getStandardName({@linkplain org.opengis.referencing.cs.AxisDirection}.class)</code>
     *       (a code list) returns {@code "CS_AxisDirection"}.</li>
     * </ul>
     *
     * <h4>Implementation note</h4>
     * This method looks for the {@link UML} annotation on the given type. It does not search for
     * parent classes or interfaces if the given type is not directly annotated (i.e. {@code @UML}
     * annotations are not inherited). If no annotation is found, then this method does not fallback
     * on the Java name since, as the name implies, this method is about standard names.
     *
     * @param  type  the GeoAPI interface or code list from which to get the ISO name, or {@code null}.
     * @return the ISO name for the given type, or {@code null} if none or if the given type is {@code null}.
     *
     * @see #forStandardName(String)
     */
    @OptionalCandidate
    public static String getStandardName(final Class<?> type) {
        if (type != null) {
            final UML uml = type.getAnnotation(UML.class);
            if (uml != null) {
                final String id = uml.identifier();
                if (id != null && !id.isEmpty()) {
                    /*
                     * Workaround: I though that annotation strings were interned like any other constants,
                     * but it does not seem to be the case as of JDK7.  To verify if this explicit call to
                     * String.intern() is still needed in a future JDK release, see the workaround comment
                     * in the org.apache.sis.metadata.PropertyAccessor.name(…) method.
                     */
                    return id.intern();
                }
            }
        }
        return null;
    }

    /**
     * Returns the ISO classname (if available) or the Java classname (as a fallback) of the given
     * enumeration or code list value. This method uses the {@link UML} annotation if it exists, or
     * fallback on the {@linkplain Class#getSimpleName() simple class name} otherwise.
     *
     * <h4>Examples</h4>
     * <ul>
     *   <li>{@code getListName(ParameterDirection.IN_OUT)}      returns {@code "SV_ParameterDirection"}.</li>
     *   <li>{@code getListName(AxisDirection.NORTH)}            returns {@code "CS_AxisDirection"}.</li>
     *   <li>{@code getListName(TopicCategory.INLAND_WATERS)}    returns {@code "MD_TopicCategoryCode"}.</li>
     *   <li>{@code getListName(ImagingCondition.BLURRED_IMAGE)} returns {@code "MD_ImagingConditionCode"}.</li>
     * </ul>
     *
     * @param  code  the code for which to get the class name, or {@code null}.
     * @return the ISO (preferred) or Java (fallback) class name, or {@code null} if the given code is null.
     */
    public static String getListName(final CodeList<?> code) {
        if (code == null) {
            return null;
        }
        final Class<?> type = code.getClass();
        final String id = getStandardName(type);
        return (id != null) ? id : type.getSimpleName();
    }

    /**
     * Returns the ISO name (if available) or the Java name (as a fallback) of the given enumeration or code list
     * value. If the value has no {@link UML} identifier, then the programmatic name is used as a fallback.
     *
     * <h4>Examples</h4>
     * <ul>
     *   <li>{@code getCodeName(ParameterDirection.IN_OUT)}      returns {@code "in/out"}.</li>
     *   <li>{@code getCodeName(AxisDirection.NORTH)}            returns {@code "north"}.</li>
     *   <li>{@code getCodeName(TopicCategory.INLAND_WATERS)}    returns {@code "inlandWaters"}.</li>
     *   <li>{@code getCodeName(ImagingCondition.BLURRED_IMAGE)} returns {@code "blurredImage"}.</li>
     * </ul>
     *
     * @param  code  the code for which to get the name, or {@code null}.
     * @return the UML identifiers or programmatic name for the given code, or {@code null} if the given code is null.
     *
     * @see #getCodeLabel(CodeList)
     * @see #getCodeTitle(CodeList)
     * @see #getDescription(CodeList)
     * @see #forCodeName(Class, String, Function)
     */
    public static String getCodeName(final CodeList<?> code) {
        if (code == null) {
            return null;
        }
        String id = null;
        if (code instanceof CodeList<?>) {
            id = ((CodeList<?>) code).identifier();
        }
        if (id == null) {
            id = code.name();
            for (String name : code.names()) {
                if (!name.equals(id)) {
                    id = name;
                    break;
                }
            }
        }
        return id.isEmpty() ? code.name() : id;
    }

    /**
     * Returns a unlocalized title for the given enumeration or code list value.
     * This method builds a title using heuristics rules, which should give reasonable
     * results without the need of resource bundles. For better results, consider using
     * {@link #getCodeTitle(CodeList)} instead.
     *
     * <p>The current heuristic implementation iterates over {@linkplain CodeList#names() all code names},
     * selects the longest one excluding the {@linkplain CodeList#name() field name} if possible, then
     * {@linkplain CharSequences#camelCaseToSentence(CharSequence) makes a sentence} from that name.</p>
     *
     * <h4>Examples</h4>
     * <ul>
     *   <li>{@code getCodeLabel(AxisDirection.NORTH)} returns {@code "North"}.</li>
     *   <li>{@code getCodeLabel(TopicCategory.INLAND_WATERS)} returns {@code "Inland waters"}.</li>
     *   <li>{@code getCodeLabel(ImagingCondition.BLURRED_IMAGE)} returns {@code "Blurred image"}.</li>
     * </ul>
     *
     * @param  code  the code from which to get a title, or {@code null}.
     * @return a unlocalized title for the given code, or {@code null} if the given code is null.
     *
     * @see #getCodeName(CodeList)
     * @see #getCodeTitle(CodeList)
     * @see #getDescription(CodeList)
     */
    public static String getCodeLabel(final CodeList<?> code) {
        if (code == null) {
            return null;
        }
        final String name = code.name();
        String id = getCodeName(code);
        for (final String candidate : code.names()) {
            if (!candidate.equals(name) && candidate.length() >= id.length()) {
                id = candidate;
            }
        }
        return CharSequences.camelCaseToSentence(id).toString();
    }

    /**
     * Returns the title of the given enumeration or code list value. Title are usually much shorter than descriptions.
     * English titles are often the same as the {@linkplain #getCodeLabel(CodeList) code labels}.
     *
     * <p>The code or enumeration value given in argument to this method can be retrieved from the returned title
     * with the {@link #forCodeTitle(CharSequence)} method. See <cite>Substituting a free text by a code list</cite>
     * in this class javadoc for more information.</p>
     *
     * @param  code  the code for which to get the title, or {@code null}.
     * @return the title, or {@code null} if the given code is null.
     *
     * @see #getDescription(CodeList)
     * @see #forCodeTitle(CharSequence)
     */
    public static InternationalString getCodeTitle(final CodeList<?> code) {
        return (code != null) ? new CodeTitle(code) : null;
    }

    /**
     * Returns the description of the given enumeration or code list value, or {@code null} if none.
     * For a description of the code list as a whole instead of a particular code,
     * see {@link Types#getDescription(Class)}.
     *
     * @param  code  the code for which to get the localized description, or {@code null}.
     * @return the description, or {@code null} if none or if the given code is null.
     *
     * @see #getCodeTitle(CodeList)
     * @see #getDescription(Class)
     */
    @OptionalCandidate
    public static InternationalString getDescription(final CodeList<?> code) {
        if (code != null && hasResources(code.getClass())) {
            return new Description(Description.resourceKey(code));
        }
        return null;
    }

    /**
     * Returns a description for the given class, or {@code null} if none.
     * This method can be used for GeoAPI interfaces or {@link CodeList}.
     *
     * @param  type  the GeoAPI interface or code list from which to get the description, or {@code null}.
     * @return the description, or {@code null} if none or if the given type is {@code null}.
     *
     * @see #getDescription(CodeList)
     */
    @OptionalCandidate
    public static InternationalString getDescription(final Class<?> type) {
        final String name = getStandardName(type);
        if (name != null && hasResources(type)) {
            return new Description(name);
        }
        return null;
    }

    /**
     * Returns a description for the given property, or {@code null} if none.
     * The given type shall be a GeoAPI interface, and the given property shall
     * be a UML identifier. If any of the input argument is {@code null}, then
     * this method returns {@code null}.
     *
     * @param  type      the GeoAPI interface from which to get the description of a property, or {@code null}.
     * @param  property  the ISO name of the property for which to get the description, or {@code null}.
     * @return the description, or {@code null} if none or if the given type or property name is {@code null}.
     */
    public static InternationalString getDescription(final Class<?> type, final String property) {
        if (property != null) {
            final String name = getStandardName(type);
            if (name != null && hasResources(type)) {
                return new Description(name + SEPARATOR + property);
            }
        }
        return null;
    }

    /**
     * The {@link InternationalString} returned by the {@code Types.getDescription(…)} methods.
     *
     * @author  Martin Desruisseaux (Geomatys)
     */
    private static class Description extends ResourceInternationalString {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -6202647167398898834L;

        /**
         * The class loader to use for fetching GeoAPI resources.
         */
        private static final ClassLoader CLASSLOADER = Types.class.getClassLoader();

        /**
         * Creates a new international string from the specified resource bundle and key.
         *
         * @param key  the key for the resource to fetch.
         */
        Description(final String key) {
            super(key);
        }

        /**
         * Loads the GeoAPI resources. A cache is managed by {@link ResourceBundle}.
         */
        @Override
        protected ResourceBundle getBundle(final Locale locale) {
            return ResourceBundle.getBundle("org.opengis.metadata.Descriptions", locale, CLASSLOADER);
        }

        /**
         * Returns the description for the given locale, or fallback on a default description
         * if no resources exist for that locale.
         */
        @Override
        public final String toString(final Locale locale) {
            try {
                return super.toString(locale);
            } catch (MissingResourceException e) {
                Logging.ignorableException(Messages.LOGGER, ResourceInternationalString.class, "toString", e);
                return fallback();
            }
        }

        /**
         * Returns a fallback if no resource is found.
         */
        String fallback() {
            return CharSequences.camelCaseToSentence(key.substring(key.lastIndexOf(SEPARATOR) + 1)).toString();
        }

        /**
         * Returns the resource key for the given code list.
         */
        static String resourceKey(final CodeList<?> code) {
            String key = getCodeName(code);
            if (key.indexOf(SEPARATOR) < 0) {
                key = getListName(code) + SEPARATOR + key;
            }
            return key;
        }
    }

    /**
     * The {@link InternationalString} returned by the {@code Types.getCodeTitle(…)} method.
     * The code below is a duplicated - in a different way - of {@code CodeListUID(CodeList)} constructor.
     * This duplication exists because {@code CodeListUID} constructor stores more information in an opportunist way.
     * If this class is updated, please update {@code CodeListUID(CodeList)} accordingly.
     *
     * @author  Martin Desruisseaux (Geomatys)
     */
    private static final class CodeTitle extends Description {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 3306532357801489365L;

        /**
         * The code list for which to create a title.
         */
        final CodeList<?> code;

        /**
         * Creates a new international string for the given code list element.
         *
         * @param  code  the code list for which to create a title.
         */
        CodeTitle(final CodeList<?> code) {
            super(resourceKey(code));
            this.code = code;
        }

        /**
         * Loads the GeoAPI resources. A cache is managed by {@link ResourceBundle}.
         */
        @Override
        protected ResourceBundle getBundle(final Locale locale) {
            return ResourceBundle.getBundle(CodeLists.RESOURCES, locale);
        }

        /**
         * Returns a fallback if no resource is found.
         */
        @Override
        String fallback() {
            return getCodeLabel(code);
        }
    }

    /**
     * Returns whether the specified class is expected to have GeoAPI resources.
     *
     * @param  type  the type to test.
     * @return whether the given class is expected to have resources.
     */
    private static boolean hasResources(final Class<?> type) {
        return type.getName().startsWith("org.opengis.metadata.");
    }

    /**
     * Returns the Java type (usually a GeoAPI interface) for the given ISO name, or {@code null} if none.
     * The identifier argument shall be the value documented in the {@link UML#identifier()} annotation on
     * the Java type.
     *
     * <h4>Examples</h4>
     * <ul>
     *   <li>{@code forStandardName("CI_Citation")}      returns <code>{@linkplain org.opengis.metadata.citation.Citation}.class</code></li>
     *   <li>{@code forStandardName("CS_AxisDirection")} returns <code>{@linkplain org.opengis.referencing.cs.AxisDirection}.class</code></li>
     * </ul>
     *
     * <h4>Implementation note</h4>
     * The package prefix (e.g. {@code "CI_"} in {@code "CI_Citation"}) can be omitted.
     * The flexibility is provided for allowing transition to newer ISO standards,
     * which are dropping the package prefixes.
     * For example, {@code "CS_AxisDirection"} in ISO 19111:2007
     * has been renamed {@code "AxisDirection"} in ISO 19111:2019.
     *
     * <p>Only identifiers for the stable part of GeoAPI or for some Apache SIS classes are recognized.
     * This method does not handle the identifiers for interfaces in the {@code geoapi-pending} module.</p>
     *
     * <h4>Future evolution</h4>
     * When a new ISO type does not yet have a corresponding GeoAPI interface,
     * this method may temporarily return an Apache SIS class instead, until a future version can use the interface.
     * For example, {@code forStandardName("CI_Individual")} returns
     * <code>{@linkplain org.apache.sis.metadata.iso.citation.DefaultIndividual}.class</code> in Apache SIS versions
     * that depend on GeoAPI 3.0, but the return type may be changed to {@code Individual.class} when Apache SIS will
     * be upgraded to GeoAPI 3.1.
     *
     * @param  identifier  the ISO {@linkplain UML} identifier, or {@code null}.
     * @return the GeoAPI interface, or {@code null} if the given identifier is {@code null} or unknown.
     */
    public static synchronized Class<?> forStandardName(final String identifier) {
        if (identifier == null) {
            return null;
        }
        if (typeForNames == null) {
            final Class<Types> c = Types.class;
            final InputStream in = c.getResourceAsStream("class-index.properties");
            if (in == null) {
                throw new MissingResourceException("class-index.properties", c.getName(), identifier);
            }
            final Properties props = new Properties();
            try {
                props.load(in);
                in.close();
            } catch (IOException e) {
                throw new BackingStoreException(e);
            }
            /*
             * Copy all map entries from Properties to a new HashMap for avoiding Properties synchronization.
             * Also use internized strings because those Strings are programmatic names or annotation values
             * which are expected to be internized anyway when the corresponding classes are loaded.
             */
            typeForNames = JDK19.newHashMap(2 * props.size());
            for (final Map.Entry<Object,Object> e : props.entrySet()) {
                final String key   = ((String) e.getKey()).intern();
                final String value = ((String) e.getValue()).intern();
                typeForNames.put(key, value);

                // Heuristic rule for omitting the prefix (e.g. "CI_" in "CI_Citation").
                if (key.length() > 3 && key.charAt(2) == '_' && Character.isUpperCase(key.charAt(1))) {
                    typeForNames.putIfAbsent(key.substring(3).intern(), value);
                }
            }
            // Following code list is not defined in ISO 19115-2 but appears in XML schemas.
            typeForNames.putIfAbsent("MI_SensorTypeCode", "org.apache.sis.xml.bind.metadata.replace.SensorType");
        }
        /*
         * Get the interface class for the given identifier, loading the class when first needed.
         */
        final Object value = typeForNames.get(identifier);
        if (value == null || value instanceof Class<?>) {
            return (Class<?>) value;
        }
        final Class<?> type;
        try {
            type = Class.forName((String) value);
        } catch (ClassNotFoundException e) {
            throw new TypeNotPresentException((String) value, e);
        }
        typeForNames.put(identifier.intern(), type);
        return type;
    }

    /**
     * Returns the enumeration value of the given type that matches the given name, or {@code null} if none.
     * This method is similar to the standard {@code Enum.valueOf(…)} method, except that this method is more
     * tolerant on string comparisons:
     *
     * <ul>
     *   <li>Name comparisons are case-insensitive.</li>
     *   <li>Only {@linkplain Character#isLetterOrDigit(int) letter and digit} characters are compared.
     *       Spaces and punctuation characters like {@code '_'} and {@code '-'} are ignored.</li>
     * </ul>
     *
     * If there is no match, this method returns {@code null} — it does not thrown an exception,
     * unless the given class is not an enumeration.
     *
     * @param  <T>       the compile-time type given as the {@code enumType} parameter.
     * @param  enumType  the type of enumeration.
     * @param  name      the name of the enumeration value to obtain, or {@code null}.
     * @return a value matching the given name, or {@code null} if the name is null
     *         or if no matching enumeration is found.
     *
     * @see Enum#valueOf(Class, String)
     *
     * @since 0.5
     */
    @OptionalCandidate
    public static <T extends Enum<T>> T forEnumName(final Class<T> enumType, String name) {
        try {
            return CodeLists.forEnumName(enumType, name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Returns the code of the given type that matches the given name, or optionally returns a new one if none
     * match the name. This method performs the same work as the GeoAPI {@code CodeList.valueOf(…)} method,
     * except that this method is more tolerant on string comparisons when looking for an existing code:
     *
     * <ul>
     *   <li>Name comparisons are case-insensitive.</li>
     *   <li>Only {@linkplain Character#isLetterOrDigit(int) letter and digit} characters are compared.
     *       Spaces and punctuation characters like {@code '_'} and {@code '-'} are ignored.</li>
     * </ul>
     *
     * If no match is found, then a new code is created only if the {@code creator} argument is non-null.
     * That argument should be a lambda function to the {@code valueOf(String)} method of the code list class.
     * Example:
     *
     * {@snippet lang="java" :
     *   AxisDirection dir = Types.forCodeName(AxisDirection.class, name, AxisDirection::valueOf);
     *   }
     *
     * If the {@code constructor} is null and no existing code matches the given name,
     * then this method returns {@code null}.
     *
     * @param  <T>          the compile-time type given as the {@code codeType} parameter.
     * @param  codeType     the type of code list.
     * @param  name         the name of the code to obtain, or {@code null}.
     * @param  constructor  the constructor to use if a new code needs to be created,
     *                      or {@code null} for not creating any new code.
     * @return a code matching the given name, or {@code null} if the name is null
     *         or if no matching code is found and {@code constructor} is {@code null}.
     *
     * @see #getCodeName(ControlledVocabulary)
     * @see CodeList#valueOf(Class, String, Function)
     *
     * @since 1.5
     */
    @OptionalCandidate
    public static <T extends CodeList<T>> T forCodeName(final Class<T> codeType, String name,
            final Function<? super String, ? extends T> constructor)
    {
        name = Strings.trimOrNull(name);
        if (name == null) {
            return null;        // Avoid initialization of the <T> class.
        }
        T code = CodeLists.forCodeName(codeType, name);
        if (code == null && constructor != null) {
            code = constructor.apply(name);
        }
        return code;
    }

    /**
     * Returns the code list or enumeration value for the given title, or {@code null} if none.
     * The current implementation performs the following choice:
     *
     * <ul>
     *   <li>If the given title is a value returned by a previous call to {@link #getCodeTitle(CodeList)},
     *       returns the code or enumeration value used for creating that title.</li>
     *   <li>Otherwise returns {@code null}.</li>
     * </ul>
     *
     * @param  title  the title for which to get a code or enumeration value, or {@code null}.
     * @return the code or enumeration value associated with the given title, or {@code null}.
     *
     * @see #getCodeTitle(CodeList)
     *
     * @since 0.7
     */
    public static CodeList<?> forCodeTitle(final CharSequence title) {
        return (title instanceof CodeTitle) ? ((CodeTitle) title).code : null;
    }

    /**
     * Returns an international string for the values in the given properties map, or {@code null} if none.
     * This method is used when a property in a {@link java.util.Map} may have many localized variants.
     * For example, the given map may contains a {@code "remarks"} property defined by values associated to
     * the {@code "remarks_en"} and {@code "remarks_fr"} keys, for English and French locales respectively.
     *
     * <p>If the given map is {@code null}, then this method returns {@code null}.
     * Otherwise this method iterates over the entries having a key that starts with the specified prefix,
     * followed by the {@code '_'} character. For each such key:</p>
     *
     * <ul>
     *   <li>If the key is exactly equals to {@code prefix}, selects {@link Locale#ROOT}.</li>
     *   <li>Otherwise the characters after {@code '_'} are parsed as an ISO language and country code
     *       by the {@link Locales#parse(String, int)} method. Note that 3-letters codes are replaced
     *       by their 2-letters counterparts on a <em>best effort</em> basis.</li>
     *   <li>The value for the decoded locale is added in the international string to be returned.</li>
     * </ul>
     *
     * @param  properties  the map from which to get the string values for an international string, or {@code null}.
     * @param  prefix      the prefix of keys to use for creating the international string.
     * @return the international string, or {@code null} if the given map is null or does not contain values
     *         associated to keys starting with the given prefix.
     * @throws IllegalArgumentException if a key starts by the given prefix and:
     *         <ul>
     *           <li>The key suffix is an illegal {@link Locale} code,</li>
     *           <li>or the value associated to that key is a not a {@link CharSequence}.</li>
     *         </ul>
     *
     * @see Locales#parse(String, int)
     * @see DefaultInternationalString#DefaultInternationalString(Map)
     *
     * @since 0.4
     */
    @OptionalCandidate
    public static InternationalString toInternationalString(Map<String,?> properties, final String prefix)
            throws IllegalArgumentException
    {
        ArgumentChecks.ensureNonEmpty("prefix", prefix);
        if (properties == null) {
            return null;
        }
        /*
         * If the given map is an instance of SortedMap using the natural ordering of keys,
         * we can skip all keys that lexicographically precedes the given prefix.
         */
        boolean isSorted = false;
        if (properties instanceof SortedMap<?,?>) {
            final SortedMap<String,?> sorted = (SortedMap<String,?>) properties;
            if (sorted.comparator() == null) {                                      // We want natural ordering.
                properties = sorted.tailMap(prefix);
                isSorted = true;
            }
        }
        /*
         * Now iterates over the map entry and lazily create the InternationalString
         * only when first needed. In most cases, we have 0 or 1 matching entry.
         */
        CharSequence i18n = null;
        Locale firstLocale = null;
        DefaultInternationalString dis = null;
        final int offset = prefix.length();
        for (final Map.Entry<String,?> entry : properties.entrySet()) {
            final String key = entry.getKey();
            if (key == null) {
                continue;                       // Tolerance for Map that accept null keys.
            }
            if (!key.startsWith(prefix)) {
                if (isSorted) break;            // If the map is sorted, there is no need to check next entries.
                continue;
            }
            final Locale locale;
            if (key.length() == offset) {
                locale = Locale.ROOT;
            } else {
                final char c = key.charAt(offset);
                if (c != '_') {
                    if (isSorted && c > '_') break;
                    continue;
                }
                final int s = offset + 1;
                try {
                    locale = Locales.parse(key, s);
                } catch (IllformedLocaleException e) {
                    throw new IllegalArgumentException(Errors.forProperties(properties).getString(
                            Errors.Keys.IllegalLanguageCode_1, '(' + key.substring(0, s) + '）' + key.substring(s), e));
                }
            }
            final Object value = entry.getValue();
            if (value != null) {
                if (!(value instanceof CharSequence)) {
                    throw new IllegalArgumentException(Errors.forProperties(properties)
                            .getString(Errors.Keys.IllegalPropertyValueClass_2, key, value.getClass()));
                }
                if (i18n == null) {
                    i18n = (CharSequence) value;
                    firstLocale = locale;
                } else {
                    if (dis == null) {
                        dis = new DefaultInternationalString();
                        add(dis, firstLocale, i18n);
                        i18n = dis;
                    }
                    add(dis, locale, (CharSequence) value);
                }
            }
        }
        return toInternationalString(i18n);
    }

    /**
     * Adds the given character sequence. If the given sequence is another {@link InternationalString} instance,
     * then only the string for the given locale is added.
     *
     * @param  locale  the locale for the {@code string} value.
     * @param  string  the character sequence to add.
     * @throws IllegalArgumentException if a different string value was already set for the given locale.
     */
    private static void add(final DefaultInternationalString dis, final Locale locale, final CharSequence string) {
        final boolean i18n = (string instanceof InternationalString);
        dis.add(locale, i18n ? ((InternationalString) string).toString(locale) : string.toString());
        if (i18n && !(string instanceof SimpleInternationalString)) {
            /*
             * If the string may have more than one locale, log a warning telling that some locales
             * may have been ignored. We declare Types.toInternationalString(…) as the source since
             * it is the public facade invoking this method.
             */
            final LogRecord record = Messages.forLocale(null).createLogRecord(Level.WARNING, Messages.Keys.LocalesDiscarded);
            Logging.completeAndLog(LOGGER, Types.class, "toInternationalString", record);
        }
    }

    /**
     * Returns the given characters sequence as an international string. If the given sequence is
     * null or an instance of {@link InternationalString}, then this method returns it unchanged.
     * Otherwise, this method copies the {@link InternationalString#toString()} value in a new
     * {@link SimpleInternationalString} instance and returns it.
     *
     * @param  string  the characters sequence to convert, or {@code null}.
     * @return the given sequence as an international string, or {@code null} if the given sequence was null.
     *
     * @see DefaultNameFactory#createInternationalString(Map)
     */
    public static InternationalString toInternationalString(final CharSequence string) {
        if (string == null || string instanceof InternationalString) {
            return (InternationalString) string;
        }
        return new SimpleInternationalString(string.toString());
    }

    /**
     * Returns the given array of {@code CharSequence}s as an array of {@code InternationalString}s.
     * If the given array is null or an instance of {@code InternationalString[]}, then this method
     * returns it unchanged. Otherwise a new array of type {@code InternationalString[]} is created
     * and every elements from the given array is copied or
     * {@linkplain #toInternationalString(CharSequence) cast} in the new array.
     *
     * <p>If a defensive copy of the {@code strings} array is wanted, then the caller needs to check
     * if the returned array is the same instance as the one given in argument to this method.</p>
     *
     * @param  strings  the characters sequences to convert, or {@code null}.
     * @return the given array as an array of type {@code InternationalString[]},
     *         or {@code null} if the given array was null.
     */
    public static InternationalString[] toInternationalStrings(final CharSequence... strings) {
        if (strings == null || strings instanceof InternationalString[]) {
            return (InternationalString[]) strings;
        }
        final InternationalString[] copy = new InternationalString[strings.length];
        for (int i=0; i<strings.length; i++) {
            copy[i] = toInternationalString(strings[i]);
        }
        return copy;
    }

    /**
     * Returns the given international string in the given locale, or {@code null} if the given string is null.
     * If the given locale is {@code null}, then the {@code i18n} default locale is used.
     *
     * @param  i18n    the international string to get as a localized string, or {@code null} if none.
     * @param  locale  the desired locale, or {@code null} for the {@code i18n} default locale.
     * @return the localized string, or {@code null} if {@code i18n} is {@code null}.
     *
     * @since 0.8
     */
    public static String toString(final InternationalString i18n, final Locale locale) {
        return (i18n == null) ? null : (locale == null) ? i18n.toString() : i18n.toString(locale);
    }
}
