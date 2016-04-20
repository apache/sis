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
import java.util.HashMap;
import java.util.SortedMap;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import org.opengis.annotation.UML;
import org.opengis.util.CodeList;
import org.opengis.util.InternationalString;
import org.apache.sis.util.Static;
import org.apache.sis.util.Locales;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.internal.system.Loggers;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.JDK8;


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
 *     <li>{@link #forCodeName(Class, String, boolean)}</li>
 *     <li>{@link #forEnumName(Class, String)}</li>
 *   </ul></li>
 * </ul>
 *
 * <div class="section">Substituting a free text by a code list</div>
 * The ISO standard allows to substitute some character strings in the <cite>"free text"</cite> domain
 * by a {@link CodeList} value.
 *
 * <div class="note"><b>Example:</b>
 * in the following XML fragment, the {@code <gmi:type>} value is normally a {@code <gco:CharacterString>}
 * but has been replaced by a {@code SensorType} code below:
 *
 * {@preformat xml
 *   <gmi:MI_Instrument>
 *     <gmi:type>
 *       <gmi:MI_SensorTypeCode
 *           codeList="http://navigator.eumetsat.int/metadata_schema/eum/resources/Codelist/eum_gmxCodelists.xml#CI_SensorTypeCode"
 *           codeListValue="RADIOMETER">Radiometer</gmi:MI_SensorTypeCode>
 *     </gmi:type>
 *   </gmi:MI_Instrument>
 * }
 * </div>
 *
 * Such substitution can be done with:
 *
 * <ul>
 *   <li>{@link #getCodeTitle(CodeList)} for getting the {@link InternationalString} instance
 *       to store in a metadata property.</li>
 *   <li>{@link #forCodeTitle(CharSequence)} for retrieving the {@link CodeList} previously stored as an
 *       {@code InternationalString}.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
public final class Types extends Static {
    /**
     * The separator character between class name and attribute name in resource files.
     */
    private static final char SEPARATOR = '.';

    /**
     * The types for ISO 19115 UML identifiers. The keys are UML identifiers. Values
     * are either class names as {@link String} objects, or the {@link Class} instances.
     * This map will be built only when first needed.
     *
     * @see #forStandardName(String)
     */
    private static Map<Object,Object> typeForNames;

    /**
     * Do not allow instantiation of this class.
     */
    private Types() {
    }

    /**
     * Returns the ISO name for the given class, or {@code null} if none.
     * This method can be used for GeoAPI interfaces or {@link CodeList}.
     *
     * <div class="note"><b>Examples:</b>
     * <ul>
     *   <li><code>getStandardName({@linkplain org.opengis.metadata.citation.Citation}.class)</code>
     *       (an interface) returns {@code "CI_Citation"}.</li>
     *   <li><code>getStandardName({@linkplain org.opengis.referencing.cs.AxisDirection}.class)</code>
     *       (a code list) returns {@code "CS_AxisDirection"}.</li>
     * </ul>
     * </div>
     *
     * This method looks for the {@link UML} annotation on the given type. It does not search for
     * parent classes or interfaces if the given type is not directly annotated (i.e. {@code @UML}
     * annotations are not inherited). If no annotation is found, then this method does not fallback
     * on the Java name since, as the name implies, this method is about standard names.
     *
     * @param  type The GeoAPI interface or code list from which to get the ISO name, or {@code null}.
     * @return The ISO name for the given type, or {@code null} if none or if the given type is {@code null}.
     *
     * @see #forStandardName(String)
     */
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
     * <div class="note"><b>Examples:</b>
     * <ul>
     *   <li>{@code getListName(ParameterDirection.IN_OUT)}      returns {@code "SV_ParameterDirection"}.</li>
     *   <li>{@code getListName(AxisDirection.NORTH)}            returns {@code "CS_AxisDirection"}.</li>
     *   <li>{@code getListName(CharacterSet.UTF_8)}             returns {@code "MD_CharacterSetCode"}.</li>
     *   <li>{@code getListName(ImagingCondition.BLURRED_IMAGE)} returns {@code "MD_ImagingConditionCode"}.</li>
     * </ul>
     * </div>
     *
     * @param  code The code for which to get the class name, or {@code null}.
     * @return The ISO (preferred) or Java (fallback) class name, or {@code null} if the given code is null.
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
     * <div class="note"><b>Examples:</b>
     * <ul>
     *   <li>{@code getCodeName(ParameterDirection.IN_OUT)}      returns {@code "in/out"}.</li>
     *   <li>{@code getCodeName(AxisDirection.NORTH)}            returns {@code "north"}.</li>
     *   <li>{@code getCodeName(CharacterSet.UTF_8)}             returns {@code "utf8"}.</li>
     *   <li>{@code getCodeName(ImagingCondition.BLURRED_IMAGE)} returns {@code "blurredImage"}.</li>
     * </ul>
     * </div>
     *
     * @param  code The code for which to get the name, or {@code null}.
     * @return The UML identifiers or programmatic name for the given code,
     *         or {@code null} if the given code is null.
     *
     * @see #getCodeLabel(CodeList)
     * @see #getCodeTitle(CodeList)
     * @see #getDescription(CodeList)
     * @see #forCodeName(Class, String, boolean)
     */
    public static String getCodeName(final CodeList<?> code) {
        if (code == null) {
            return null;
        }
        final String id = code.identifier();
        return (id != null && !id.isEmpty()) ? id : code.name();
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
     * <div class="note"><b>Examples:</b>
     * <ul>
     *   <li>{@code getCodeLabel(AxisDirection.NORTH)} returns {@code "North"}.</li>
     *   <li>{@code getCodeLabel(CharacterSet.UTF_8)} returns {@code "UTF-8"}.</li>
     *   <li>{@code getCodeLabel(ImagingCondition.BLURRED_IMAGE)} returns {@code "Blurred image"}.</li>
     * </ul>
     * </div>
     *
     * @param  code The code from which to get a title, or {@code null}.
     * @return A unlocalized title for the given code, or {@code null} if the given code is null.
     *
     * @see #getCodeName(CodeList)
     * @see #getCodeTitle(CodeList)
     * @see #getDescription(CodeList)
     */
    public static String getCodeLabel(final CodeList<?> code) {
        if (code == null) {
            return null;
        }
        String id = code.identifier();
        final String name = code.name();
        if (id == null) {
            id = name;
        }
        for (final String candidate : code.names()) {
            if (!candidate.equals(name) && candidate.length() >= id.length()) {
                id = candidate;
            }
        }
        return CharSequences.camelCaseToSentence(id).toString();
    }

    /**
     * Returns the title of the given enumeration or code list value. Title are usually much shorter than descriptions.
     * English titles are often the same than the {@linkplain #getCodeLabel(CodeList) code labels}.
     *
     * <p>The code or enumeration value given in argument to this method can be retrieved from the returned title
     * with the {@link #forCodeTitle(CharSequence)} method. See <cite>Substituting a free text by a code list</cite>
     * in this class javadoc for more information.</p>
     *
     * @param  code The code for which to get the title, or {@code null}.
     * @return The title, or {@code null} if the given code is null.
     *
     * @see #getDescription(CodeList)
     * @see #forCodeTitle(CharSequence)
     */
    public static InternationalString getCodeTitle(final CodeList<?> code) {
        return (code != null) ? new CodeTitle(code) : null;
    }

    /**
     * Returns the description of the given enumeration or code list value, or {@code null} if none.
     * For a description of the code list as a whole instead than a particular code,
     * see {@link Types#getDescription(Class)}.
     *
     * @param  code The code for which to get the localized description, or {@code null}.
     * @return The description, or {@code null} if none or if the given code is null.
     *
     * @see #getCodeTitle(CodeList)
     * @see #getDescription(Class)
     */
    public static InternationalString getDescription(final CodeList<?> code) {
        if (code != null) {
            final String resources = getResources(code.getClass().getName());
            if (resources != null) {
                return new Description(resources, Description.resourceKey(code));
            }
        }
        return null;
    }

    /**
     * Returns a description for the given class, or {@code null} if none.
     * This method can be used for GeoAPI interfaces or {@link CodeList}.
     *
     * @param  type The GeoAPI interface or code list from which to get the description, or {@code null}.
     * @return The description, or {@code null} if none or if the given type is {@code null}.
     *
     * @see #getDescription(CodeList)
     */
    public static InternationalString getDescription(final Class<?> type) {
        final String name = getStandardName(type);
        if (name != null) {
            final String resources = getResources(type.getName());
            if (resources != null) {
                return new Description(resources, name);
            }
        }
        return null;
    }

    /**
     * Returns a description for the given property, or {@code null} if none.
     * The given type shall be a GeoAPI interface, and the given property shall
     * be a UML identifier. If any of the input argument is {@code null}, then
     * this method returns {@code null}.
     *
     * @param  type     The GeoAPI interface from which to get the description of a property, or {@code null}.
     * @param  property The ISO name of the property for which to get the description, or {@code null}.
     * @return The description, or {@code null} if none or if the given type or property name is {@code null}.
     */
    public static InternationalString getDescription(final Class<?> type, final String property) {
        if (property != null) {
            final String name = getStandardName(type);
            if (name != null) {
                final String resources = getResources(type.getName());
                if (resources != null) {
                    return new Description(resources, name + SEPARATOR + property);
                }
            }
        }
        return null;
    }

    /**
     * The {@link InternationalString} returned by the {@code Types.getDescription(…)} methods.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     */
    private static class Description extends ResourceInternationalString {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -6202647167398898834L;

        /**
         * The class loader to use for fetching GeoAPI resources.
         * Since the resources are bundled in the GeoAPI JAR file,
         * we use the instance that loaded GeoAPI for more determinist behavior.
         */
        private static final ClassLoader CLASSLOADER = UML.class.getClassLoader();

        /**
         * Creates a new international string from the specified resource bundle and key.
         *
         * @param resources The name of the resource bundle, as a fully qualified class name.
         * @param key       The key for the resource to fetch.
         */
        Description(final String resources, final String key) {
            super(resources, key);
        }

        /**
         * Loads the resources using the class loader used for loading GeoAPI interfaces.
         */
        @Override
        protected final ResourceBundle getBundle(final Locale locale) {
            return ResourceBundle.getBundle(resources, locale, CLASSLOADER);
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
                Logging.recoverableException(Logging.getLogger(Loggers.LOCALIZATION), ResourceInternationalString.class, "toString", e);
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
     * The code below is a duplicated - in a different way - of {@code CodeListUID(CodeList)}
     * constructor ({@link org.apache.sis.internal.jaxb.code package}). This duplication exists
     * because {@code CodeListUID} constructor stores more information in an opportunist way.
     * If this method is updated, please update {@code CodeListUID(CodeList)} accordingly.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
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
         * @param code The code list for which to create a title.
         */
        CodeTitle(final CodeList<?> code) {
            super("org.opengis.metadata.CodeLists", resourceKey(code));
            this.code = code;
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
     * Returns the resource name for the given GeoAPI type, or {@code null} if none.
     *
     * @param  classname The fully qualified name of the GeoAPI type.
     * @return The resource bundle to load, or {@code null} if none.
     */
    static String getResources(final String classname) {
        String resources = "org.opengis.metadata.Descriptions";
        if (classname.regionMatches(0, resources, 0, 21)) { // 21 is the location after the last dot.
            return resources;
        }
        // Add more checks here (maybe in a loop) if there is more resource candidates.
        return null;
    }

    /**
     * Returns all known values for the given type of code list.
     * Note that the size of the returned array may growth between different invocations of this method,
     * since users can add their own codes to an existing list.
     *
     * @param <T> The compile-time type given as the {@code codeType} parameter.
     * @param codeType The type of code list.
     * @return The list of values for the given code list, or an empty array if none.
     *
     * @see Class#getEnumConstants()
     */
    @SuppressWarnings("unchecked")
    public static <T extends CodeList<?>> T[] getCodeValues(final Class<T> codeType) {
        Object values;
        try {
            values = codeType.getMethod("values", (Class<?>[]) null).invoke(null, (Object[]) null);
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new UndeclaredThrowableException(cause);
        } catch (NoSuchMethodException e) {
            values = Array.newInstance(codeType, 0);
        } catch (IllegalAccessException e) {
            values = Array.newInstance(codeType, 0);
        }
        return (T[]) values;
    }

    /**
     * Returns the GeoAPI interface for the given ISO name, or {@code null} if none.
     * The identifier argument shall be the value documented in the {@link UML#identifier()}
     * annotation associated with the GeoAPI interface.
     *
     * <div class="note"><b>Examples:</b>
     * <ul>
     *   <li>{@code forStandardName("CI_Citation")}      returns <code>{@linkplain org.opengis.metadata.citation.Citation}.class</code></li>
     *   <li>{@code forStandardName("CS_AxisDirection")} returns <code>{@linkplain org.opengis.referencing.cs.AxisDirection}.class</code></li>
     * </ul>
     * </div>
     *
     * Only identifiers for the stable part of GeoAPI are recognized. This method does not handle
     * the identifiers for the {@code geoapi-pending} module.
     *
     * @param  identifier The ISO {@linkplain UML} identifier, or {@code null}.
     * @return The GeoAPI interface, or {@code null} if the given identifier is {@code null} or unknown.
     */
    public static synchronized Class<?> forStandardName(final String identifier) {
        if (identifier == null) {
            return null;
        }
        if (typeForNames == null) {
            final Class<?> c = Types.class;
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
            } catch (IllegalArgumentException e) {
                throw new BackingStoreException(e);
            }
            typeForNames = new HashMap<Object,Object>(props);
            JDK8.putIfAbsent(typeForNames, "MI_SensorTypeCode", "org.apache.sis.internal.metadata.SensorType");
        }
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
        typeForNames.put(identifier, type);
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
     * @param <T>      The compile-time type given as the {@code enumType} parameter.
     * @param enumType The type of enumeration.
     * @param name     The name of the enumeration value to obtain, or {@code null}.
     * @return A value matching the given name, or {@code null} if the name is null
     *         or if no matching enumeration is found.
     *
     * @see Enum#valueOf(Class, String)
     *
     * @since 0.5
     */
    public static <T extends Enum<T>> T forEnumName(final Class<T> enumType, String name) {
        name = CharSequences.trimWhitespaces(name);
        if (name != null && !name.isEmpty()) try {
            return Enum.valueOf(enumType, name);
        } catch (IllegalArgumentException e) {
            final T[] values = enumType.getEnumConstants();
            if (values == null) {
                throw e;
            }
            for (final Enum<?> code : values) {
                if (CodeListFilter.accept(code.name(), name)) {
                    return enumType.cast(code);
                }
            }
        }
        return null;
    }

    /**
     * Returns the code of the given type that matches the given name, or optionally returns a new one if none
     * match the name. This method performs the same work than the GeoAPI {@code CodeList.valueOf(…)} method,
     * except that this method is more tolerant on string comparisons when looking for an existing code:
     *
     * <ul>
     *   <li>Name comparisons are case-insensitive.</li>
     *   <li>Only {@linkplain Character#isLetterOrDigit(int) letter and digit} characters are compared.
     *       Spaces and punctuation characters like {@code '_'} and {@code '-'} are ignored.</li>
     * </ul>
     *
     * If no match is found, then a new code is created only if the {@code canCreate} argument is {@code true}.
     * Otherwise this method returns {@code null}.
     *
     * @param <T>        The compile-time type given as the {@code codeType} parameter.
     * @param codeType   The type of code list.
     * @param name       The name of the code to obtain, or {@code null}.
     * @param canCreate  {@code true} if this method is allowed to create new code.
     * @return A code matching the given name, or {@code null} if the name is null
     *         or if no matching code is found and {@code canCreate} is {@code false}.
     *
     * @see #getCodeName(CodeList<?>)
     * @see CodeList#valueOf(Class, String)
     */
    public static <T extends CodeList<T>> T forCodeName(final Class<T> codeType, String name, final boolean canCreate) {
        name = CharSequences.trimWhitespaces(name);
        if (name == null || name.isEmpty()) {
            return null;
        }
        // -------- Begin workaround for GeoAPI 3.0 (TODO: remove after upgrade to GeoAPI 3.1) ------------
        final String typeName = codeType.getName();
        try {
            // Forces initialization of the given class in order
            // to register its list of static final constants.
            Class.forName(typeName, true, codeType.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new TypeNotPresentException(typeName, e); // Should never happen.
        }
        // -------- End workaround ------------------------------------------------------------------------
        return CodeList.valueOf(codeType, new CodeListFilter(name, canCreate));
    }

    /**
     * Returns the code list or enumeration value for the given title, or {@code null} if none.
     * The current implementation performs the following choice:
     *
     * <ul>
     *   <li>If the given title is a value returned by a previous call to {@link #getCodeTitle(CodeList<?>)},
     *       returns the code or enumeration value used for creating that title.</li>
     *   <li>Otherwise returns {@code null}.</li>
     * </ul>
     *
     * @param  title The title for which to get a code or enumeration value, or {@code null}.
     * @return The code or enumeration value associated with the given title, or {@code null}.
     *
     * @since 0.7
     *
     * @see #getCodeTitle(CodeList<?>)
     */
    public static CodeList<?> forCodeTitle(final CharSequence title) {
        return (title instanceof CodeTitle) ? ((CodeTitle) title).code : null;
    }

    /**
     * Returns an international string for the values in the given properties map, or {@code null} if none.
     * This method is used when a property in a {@link java.util.Map} may have many localized variants.
     * For example the given map may contains a {@code "remarks"} property defined by values associated to
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
     *       by their 2-letters counterparts on a <cite>best effort</cite> basis.</li>
     *   <li>The value for the decoded locale is added in the international string to be returned.</li>
     * </ul>
     *
     * @param  properties The map from which to get the string values for an international string, or {@code null}.
     * @param  prefix     The prefix of keys to use for creating the international string.
     * @return The international string, or {@code null} if the given map is null or does not contain values
     *         associated to keys starting with the given prefix.
     * @throws IllegalArgumentException If a key starts by the given prefix and:
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
            if (sorted.comparator() == null) { // We want natural ordering.
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
                continue; // Tolerance for Map that accept null keys.
            }
            if (!key.startsWith(prefix)) {
                if (isSorted) break; // If the map is sorted, there is no need to check next entries.
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
                } catch (RuntimeException e) { // IllformedLocaleException on the JDK7 branch.
                    throw new IllegalArgumentException(Errors.getResources(properties).getString(
                            Errors.Keys.IllegalLanguageCode_1, '(' + key.substring(0, s) + '）' + key.substring(s), e));
                }
            }
            final Object value = entry.getValue();
            if (value != null) {
                if (!(value instanceof CharSequence)) {
                    throw new IllegalArgumentException(Errors.getResources(properties)
                            .getString(Errors.Keys.IllegalPropertyValueClass_2, key, value.getClass()));
                }
                if (i18n == null) {
                    i18n = (CharSequence) value;
                    firstLocale = locale;
                } else {
                    if (dis == null) {
                        dis = new DefaultInternationalString();
                        dis.add(firstLocale, i18n);
                        i18n = dis;
                    }
                    dis.add(locale, (CharSequence) value);
                }
            }
        }
        return toInternationalString(i18n);
    }

    /**
     * Returns the given characters sequence as an international string. If the given sequence is
     * null or an instance of {@link InternationalString}, then this method returns it unchanged.
     * Otherwise, this method copies the {@link InternationalString#toString()} value in a new
     * {@link SimpleInternationalString} instance and returns it.
     *
     * @param  string The characters sequence to convert, or {@code null}.
     * @return The given sequence as an international string,
     *         or {@code null} if the given sequence was null.
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
     * {@linkplain #toInternationalString(CharSequence) casted} in the new array.
     *
     * <p>If a defensive copy of the {@code strings} array is wanted, then the caller needs to check
     * if the returned array is the same instance than the one given in argument to this method.</p>
     *
     * @param  strings The characters sequences to convert, or {@code null}.
     * @return The given array as an array of type {@code InternationalString[]},
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
}
