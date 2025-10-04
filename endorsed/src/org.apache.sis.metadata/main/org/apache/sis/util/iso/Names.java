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
import java.lang.reflect.Type;
import org.opengis.util.TypeName;
import org.opengis.util.LocalName;
import org.opengis.util.MemberName;
import org.opengis.util.ScopedName;
import org.opengis.util.GenericName;
import org.opengis.util.NameSpace;
import org.opengis.util.NameFactory;
import org.opengis.util.InternationalString;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.OptionalCandidate;
import org.apache.sis.util.UnknownNameException;
import org.apache.sis.util.resources.Errors;


/**
 * Static methods for creating, parsing and formatting {@linkplain AbstractName generic names}.
 * This convenience class does not add new functionality compared to {@link DefaultNameFactory},
 * but makes some tasks easier by avoiding the need to find a factory, and by creating name and
 * their namespace in a single step.
 *
 * <h2>Relationship with Java Content Repository (JCR) names</h2>
 * In the Java standard {@link javax.xml.namespace.QName} class and in the Java Content Repository (JCR) specification,
 * a name is an ordered pair of ({@code namespace}, {@code localPart}) strings. A JCR name can take two lexical forms:
 * <i>expanded form</i> and <i>qualified form</i>. Those names are mapped to generic names as below:
 *
 * <blockquote><table class="sis" style="white-space: nowrap">
 *   <caption>Equivalence between JCR name and {@code GenericName}</caption>
 *   <tr>
 *     <th>JCR name</th>
 *     <th class="sep" colspan="2">GeoAPI equivalence</th>
 *   </tr><tr>
 *     <td><code>ExpandedName ::= '{' Namespace '}' LocalPart</code></td>
 *     <td class="sep"><code>GenericName.{@linkplain AbstractName#scope() scope()}.name().toString()</code></td>
 *     <td>= JCR {@code Namespace}</td>
 *   </tr><tr>
 *     <td></td>
 *     <td class="sep"><code>GenericName.{@linkplain AbstractName#toString() toString()}</code></td>
 *     <td>= JCR {@code LocalPart}</td>
 *   </tr><tr>
 *     <td class="hsep"><code>QualifiedName ::= [Prefix ':'] LocalPart</code></td>
 *     <td class="hsep sep"><code>ScopedName.{@linkplain AbstractName#scope() scope()}</code></td>
 *     <td class="hsep">= {@linkplain DefaultNameSpace#isGlobal() global namespace}</td>
 *   </tr><tr>
 *     <td></td>
 *     <td class="sep"><code>ScopedName.{@linkplain DefaultScopedName#head() head()}.toString()</code></td>
 *     <td>= JCR {@code Prefix}</td>
 *   </tr><tr>
 *     <td></td>
 *     <td class="sep"><code>ScopedName.{@linkplain DefaultScopedName#tail() tail()}.toString()</code></td>
 *     <td>= JCR {@code LocalPart}</td>
 *   </tr>
 * </table></blockquote>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @see DefaultNameFactory
 * @see DefaultNameSpace
 * @see DefaultScopedName
 * @see DefaultLocalName
 * @see DefaultTypeName
 * @see DefaultMemberName
 *
 * @since 0.5
 */
public final class Names {
    /**
     * Sequence numbers, created when first needed.
     *
     * @see #createMemberName(CharSequence, String, int)
     */
    private static final MemberName[] SEQUENCE_NUMBERS = new MemberName[16];

    /**
     * Do not allow instantiation of this class.
     */
    private Names() {
    }

    /**
     * Creates a namespace for the given name.
     *
     * @param  factory    the factory to use for creating the namespace.
     * @param  namespace  the namespace string, taken as a whole (not parsed).
     * @param  separator  the separator between the namespace and the local part, or {@code null} for the default.
     * @return the namespace object, or {@code null} if the given {@code namespace} was null or empty.
     */
    private static NameSpace createNameSpace(final NameFactory factory, final CharSequence namespace, final String separator) {
        if (namespace == null || namespace.length() == 0) {
            return null;
        }
        return factory.createNameSpace(factory.createLocalName(null, namespace),
                (separator == null) ? null : Map.of("separator.head", separator));
    }

    /**
     * Creates a local or scoped name in the given namespace.
     * The character sequences can be either {@link String} or {@link InternationalString} instances.
     * The {@code namespace} character sequences is taken verbatim, while {@code scopedName} is split
     * around the {@linkplain DefaultNameSpace#DEFAULT_SEPARATOR default separator}, which is {@code ":"}.
     *
     * @param  namespace   the namespace, or {@code null} for the global namespace.
     * @param  separator   the separator between the namespace and the generic name, or {@code null}
     *                     for the {@linkplain DefaultNameSpace#DEFAULT_SEPARATOR default separator}.
     * @param  scopedName  the name to parse using {@code ':'} as the separator between components.
     * @return a local or scoped name in the given namespace.
     *
     * @see DefaultNameFactory#parseGenericName(NameSpace, CharSequence)
     *
     * @todo Bug in current implementation: the {@code separator} argument is ignored if {@code namespace} is null.
     */
    public static GenericName parseGenericName(final CharSequence namespace, final String separator, final CharSequence scopedName) {
        ArgumentChecks.ensureNonNull("localPart", scopedName);
        final NameFactory factory = DefaultNameFactory.provider();
        return factory.parseGenericName(createNameSpace(factory, namespace, separator), scopedName);
    }

    /**
     * Creates a local or scoped name from an array of parsed names. This method returns a local name if the
     * length of the {@code parsedNames} array is 1, or a scoped named if the length of the array is 2 or more.
     * The first {@code parsedNames} element will be the {@linkplain AbstractName#head() head}
     * and the last {@code parsedNames} element will be the {@link AbstractName#tip() tip}.
     *
     * @param  namespace    the namespace, or {@code null} for the global namespace.
     * @param  separator    the separator between the namespace and the generic name, or {@code null}
     *                      for the {@linkplain DefaultNameSpace#DEFAULT_SEPARATOR default separator}.
     * @param  parsedNames  the local names as an array of {@link String} or {@link InternationalString} instances.
     *                      This array shall contain at least one element.
     * @return the generic name for the given parsed names.
     *
     * @since 1.0
     */
    public static GenericName createGenericName(final CharSequence namespace, final String separator, final CharSequence... parsedNames) {
        ArgumentChecks.ensureNonNull("parsedNames", parsedNames);
        final NameFactory factory = DefaultNameFactory.provider();
        return factory.createGenericName(createNameSpace(factory, namespace, separator), parsedNames);
    }

    /**
     * Creates a scoped name as the concatenation of the given generic name with a single character sequence.
     * The scope of the new name will be the scope of the {@code path} argument.
     * The tail is a local name created from the given character sequence.
     *
     * @param  scope      the first part to concatenate.
     * @param  separator  the separator between the head and the tail,
     *                    or {@code null} for inheriting the same separator as the given scope.
     * @param  name       the second part to concatenate.
     * @return a scoped name in the given namespace.
     *
     * @since 0.8
     */
    public static ScopedName createScopedName(final GenericName scope, final String separator, final CharSequence name) {
        // Current version does not perform any caching, but this is something we could add in the future.
        return new DefaultScopedName(scope, separator, name);
    }

    /**
     * Creates a name which is local in the given namespace.
     * The character sequences can be either {@link String} or {@link InternationalString} instances.
     * Those character sequences are taken verbatim; they are <em>not</em> parsed into their components.
     *
     * <div class="note"><b>Note:</b> it is possible to split the {@code namespace} and {@code localPart}
     * strings into smaller name components (e.g. namespaces contained in other namespaces). If such finer
     * grain control is desired, one can use {@link DefaultNameFactory} instead of this {@code Names} class.</div>
     *
     * The following table shows where the strings given in argument will go:
     *
     * <blockquote><table class="sis">
     *   <caption>Mapping from arguments to name components</caption>
     *   <tr><th>Argument</th> <th>Mapped to</th></tr>
     *   <tr><td>{@code namespace}</td> <td><code>name.{@linkplain DefaultLocalName#scope() scope()}.name().toString()</code></td></tr>
     *   <tr><td>{@code localPart}</td> <td><code>name.{@linkplain DefaultLocalName#toString() toString()}</code></td></tr>
     * </table></blockquote>
     *
     * <h4>Example</h4>
     * For a name created by {@code createLocalName("http://www.opengis.net/gml/srs/epsg.xml", "#", "4326")}:
     * <ul>
     *   <li><code>name.{@linkplain DefaultLocalName#toString() toString()}</code>
     *       returns the {@code "4326"} string.</li>
     *   <li><code>name.{@linkplain DefaultLocalName#scope() scope()}</code>
     *       returns the {@code "http://www.opengis.net/gml/srs/epsg.xml"} namespace.</li>
     *   <li><code>name.{@linkplain DefaultLocalName#toFullyQualifiedName() toFullyQualifiedName()}</code>
     *       returns the {@code "http://www.opengis.net/gml/srs/epsg.xml#4326"} name.</li>
     *   <li><code>{@linkplain #toExpandedString(GenericName) toExpandedString}(name)</code>
     *       returns the {@code "{http://www.opengis.net/gml/srs/epsg.xml}4326"} string.</li>
     * </ul>
     *
     * <h4>Performance note</h4>
     * This method is okay for <em>casual</em> use.
     * If many names need to be created in the same namespace,
     * then {@link DefaultNameFactory#createLocalName(NameSpace, CharSequence)}
     * is more efficient since it allows to create the {@code NameSpace} object only once.
     *
     * @param  namespace  the namespace, or {@code null} for the global namespace.
     * @param  separator  the separator between the namespace and the local part, or {@code null}
     *                    for the {@linkplain DefaultNameSpace#DEFAULT_SEPARATOR default separator}.
     * @param  localPart  the name which is locale in the given namespace.
     * @return a local name in the given namespace.
     *
     * @see DefaultNameFactory#createLocalName(NameSpace, CharSequence)
     */
    public static LocalName createLocalName(final CharSequence namespace, final String separator, final CharSequence localPart) {
        ArgumentChecks.ensureNonNull("localPart", localPart);
        final NameFactory factory = DefaultNameFactory.provider();
        return factory.createLocalName(createNameSpace(factory, namespace, separator), localPart);
    }

    /**
     * Creates a type name from the given character sequence and automatically inferred Java type.
     * The character sequences can be either {@link String} or {@link InternationalString} instances.
     * Those character sequences are taken verbatim; they are <em>not</em> parsed into their components.
     *
     * <h4>Example</h4>
     * {@code createTypeName("gco", ":", "Integer")} returns a name
     * which can be used for representing the type of {@code <gco:Integer>} elements in XML files.
     *
     * <h4>Performance note</h4>
     * This method is okay for <em>casual</em> use.
     * If many names need to be created in the same namespace,
     * then {@link DefaultNameFactory#createTypeName(NameSpace, CharSequence)}
     * is more efficient since it allows to create the {@code NameSpace} object only once.
     *
     * @param  namespace  the namespace, or {@code null} for the global namespace.
     * @param  separator  the separator between the namespace and the local part, or {@code null}
     *                    for the {@linkplain DefaultNameSpace#DEFAULT_SEPARATOR default separator}.
     * @param  localPart  the name which is locale in the given namespace.
     * @return a type name in the given namespace.
     *
     * @see DefaultNameFactory#createTypeName(NameSpace, CharSequence)
     */
    public static TypeName createTypeName(final CharSequence namespace, final String separator, final CharSequence localPart) {
        ArgumentChecks.ensureNonNull("localPart", localPart);
        final NameFactory factory = DefaultNameFactory.provider();
        return factory.createTypeName(createNameSpace(factory, namespace, separator), localPart);
    }

    /**
     * Creates a type name for the given class using naming convention documented in {@link DefaultTypeName}.
     * This method is a shortcut for {@link DefaultNameFactory#toTypeName(Class)}
     * and is the converse of {@link #toClass(TypeName)}.
     *
     * @param  valueClass  the type of values for which to infer a {@link TypeName} instance.
     * @return a type name for values of the given type.
     *
     * @see #createMemberName(CharSequence, String, CharSequence, Class)
     * @see DefaultNameFactory#toTypeName(Class)
     *
     * @since 1.3
     */
    public static TypeName createTypeName(final Class<?> valueClass) {
        ArgumentChecks.ensureNonNull("valueClass", valueClass);
        final var factory = DefaultNameFactory.provider();
        return factory.toTypeName(valueClass);    // SIS-specific method.
    }

    /**
     * Creates a member name for a record of the given name.
     * The given namespace is usually an instance of {@link TypeName}.
     *
     * @param  namespace  the name of the record which will contain this member name.
     * @param  localPart  the name which is locale in the given namespace.
     * @param  valueClass the type of values, used for inferring a {@link TypeName} instance.
     * @return a member name in the given namespace for values of the given type.
     */
    static MemberName createMemberName(final GenericName namespace, final CharSequence localPart, final Class<?> valueClass) {
        final var factory = DefaultNameFactory.provider();
        return factory.createMemberName(factory.createNameSpace(namespace, null), localPart, factory.toTypeName(valueClass));
    }

    /**
     * Creates a member name for values of the given class. A {@link TypeName} will be inferred
     * from the given {@code valueClass} as documented in the {@link DefaultTypeName} javadoc.
     *
     * <h4>Performance note</h4>
     * This method is okay for <em>casual</em> use. If many names need to be created,
     * then {@link DefaultNameFactory#createMemberName(NameSpace, CharSequence, TypeName)}
     * is more efficient since it allows to create the {@code NameSpace} and {@code TypeName} objects only once.
     *
     * @param  namespace  the namespace, or {@code null} for the global namespace.
     * @param  separator  the separator between the namespace and the local part, or {@code null}
     *                    for the {@linkplain DefaultNameSpace#DEFAULT_SEPARATOR default separator}.
     * @param  localPart  the name which is locale in the given namespace.
     * @param  valueClass the type of values, used for inferring a {@link TypeName} instance.
     * @return a member name in the given namespace for values of the given type.
     */
    public static MemberName createMemberName(final CharSequence namespace, final String separator,
            final CharSequence localPart, final Class<?> valueClass)
    {
        ArgumentChecks.ensureNonNull("localPart",  localPart);
        ArgumentChecks.ensureNonNull("valueClass", valueClass);
        final var factory = DefaultNameFactory.provider();
        return factory.createMemberName(createNameSpace(factory, namespace, separator), localPart,
               factory.toTypeName(valueClass));     // SIS-specific method.
    }

    /**
     * Creates a member name for attribute values of the given type.
     * This is a shortcut for {@link DefaultNameFactory#createMemberName(NameSpace, CharSequence, TypeName)}.
     * See {@linkplain #createMemberName(CharSequence, String, CharSequence, Class) performance note}.
     *
     * @param  namespace  the namespace, or {@code null} for the global namespace.
     * @param  separator  the separator between the namespace and the local part, or {@code null}
     *                    for the {@linkplain DefaultNameSpace#DEFAULT_SEPARATOR default separator}.
     * @param  localPart  the name which is locale in the given namespace.
     * @param  attributeType  the type of the data associated with the member.
     * @return a member name in the given namespace for values of the given type.
     *
     * @since 1.3
     */
    public static MemberName createMemberName(final CharSequence namespace, final String separator,
            final CharSequence localPart, final TypeName attributeType)
    {
        ArgumentChecks.ensureNonNull("localPart", localPart);
        ArgumentChecks.ensureNonNull("attributeType", attributeType);
        final DefaultNameFactory factory = DefaultNameFactory.provider();
        return factory.createMemberName(createNameSpace(factory, namespace, separator), localPart, attributeType);
    }

    /**
     * Creates a member name for the given sequence number. The member type will be {@code "OGC:Integer"}.
     * This method can be used for {@linkplain org.apache.sis.metadata.iso.content.DefaultRangeDimension#setSequenceIdentifier
     * setting band identifier in metadata} in the common case where band identifier are just numbers.
     *
     * @param  namespace  the namespace, or {@code null} for the global namespace.
     * @param  separator  the separator between the namespace and the local part, or {@code null}
     *                    for the {@linkplain DefaultNameSpace#DEFAULT_SEPARATOR default separator}.
     * @param  localPart  the sequence number to use as local part.
     * @return a member name in the given namespace with the given sequence number.
     *
     * @see org.opengis.metadata.content.RangeDimension#getSequenceIdentifier()
     *
     * @since 1.0
     */
    public static MemberName createMemberName(final CharSequence namespace, String separator, final int localPart) {
        if (DefaultNameSpace.DEFAULT_SEPARATOR_STRING.equals(separator)) {
            separator = null;       // For making test for caching easier.
        }
        final boolean cached = (namespace == null) && (separator == null) && localPart >= 0 && localPart < SEQUENCE_NUMBERS.length;
        MemberName name = null;
        if (cached) synchronized (SEQUENCE_NUMBERS) {
            name = SEQUENCE_NUMBERS[localPart];
        }
        if (name == null) {
            name = createMemberName(namespace, separator, Integer.toString(localPart), Integer.class);
            if (cached) synchronized (SEQUENCE_NUMBERS) {
                /*
                 * No need to check if a value has been set concurrently because `createMemberName(…)`
                 * already checked if an equal instance exists in the current JVM.
                 */
                SEQUENCE_NUMBERS[localPart] = name;
            }
        }
        return name;
    }

    /**
     * Returns the Java class associated to the given type name.
     * The method performs the following choices:
     *
     * <ul>
     *   <li>If the given type name is {@code null}, then this method returns {@code null}.</li>
     *   <li>Else if the value returned by {@link DefaultTypeName#toJavaType()} is a {@link Class}, returns that class.</li>
     *   <li>Else if the type name {@linkplain DefaultTypeName#scope() scope} is {@code "OGC"}, then:
     *     <ul>
     *       <li>If the name is {@code "CharacterString"}, {@code "Integer"}, {@code "Real"} or other recognized names
     *           (see {@link DefaultTypeName} javadoc), then the corresponding class is returned.</li>
     *       <li>Otherwise {@link UnknownNameException} is thrown.</li>
     *     </ul>
     *   </li>
     *   <li>Else if the scope is {@code "class"}, then:
     *     <ul>
     *       <li>If the name is accepted by {@link Class#forName(String)}, then that class is returned.</li>
     *       <li>Otherwise {@link UnknownNameException} is thrown.</li>
     *     </ul>
     *   </li>
     *   <li>Else if the scope {@linkplain DefaultNameSpace#isGlobal() is global}, then:
     *     <ul>
     *       <li>If the name is one of the names recognized in {@code "OGC"} scope (see above),
     *           then the corresponding class is returned.</li>
     *       <li>Otherwise {@code null} is returned. No exception is thrown because names in the global namespace
     *           could be anything; this method cannot be sure that the given name was wrong.</li>
     *     </ul>
     *   </li>
     *   <li>Otherwise {@code null} is returned,
     *       because this method cannot check the validity of names in other namespaces.</li>
     * </ul>
     *
     * @param  type  the type name from which to infer a Java class.
     * @return the Java class associated to the given {@code TypeName},
     *         or {@code null} if there is no mapping from the given name to a Java class.
     * @throws UnknownNameException if a mapping from the given name to a Java class was expected to exist
     *         (typically because of the {@linkplain DefaultTypeName#scope() scope}) but the lookup failed.
     *
     * @see #createTypeName(Class)
     * @see DefaultTypeName#toJavaType()
     *
     * @since 0.5
     */
    @OptionalCandidate
    public static Class<?> toClass(final TypeName type) throws UnknownNameException {
        if (type == null) {
            return null;
        }
        if (type instanceof DefaultTypeName) {
            final Type t = ((DefaultTypeName) type).toJavaType().orElse(null);
            if (t instanceof Class<?>) {
                return (Class<?>) t;
            }
        }
        ClassNotFoundException cause;
        try {
            final Class<?> c = TypeNames.toClass(TypeNames.namespace(type.scope()), type.toString());
            if (c != Void.TYPE) {
                return c;
            }
            cause = null;
        } catch (ClassNotFoundException e) {
            cause = e;
        }
        throw new UnknownNameException(Errors.format(Errors.Keys.UnknownType_1, type.toFullyQualifiedName()), cause);
    }

    /**
     * Formats the given name in <i>expanded form</i> close to the Java Content Repository (JCR) definition.
     * The expanded form is defined as below:
     *
     * <blockquote><pre> ExpandedName ::= '{' NameSpace '}' LocalPart
     * NameSpace    ::= name.{@linkplain AbstractName#scope() scope()}.{@linkplain DefaultNameSpace#name() name()}.toString()
     * LocalPart    ::= name.{@linkplain AbstractName#toString() toString()}</pre></blockquote>
     *
     * @param  name  the generic name to format in expanded form, or {@code null}.
     * @return expanded form of the given generic name, or {@code null} if the given name was null.
     *
     * @see DefaultNameSpace#toString()
     */
    public static String toExpandedString(final GenericName name) {
        if (name == null) {
            return null;
        }
        final String localPart = name.toString();
        final NameSpace scope = name.scope();
        if (scope == null || scope.isGlobal()) {
            return localPart;
        }
        final String ns = scope.name().toString();
        return new StringBuilder(ns.length() + localPart.length() + 2)
                .append('{').append(ns).append('}').append(localPart).toString();
    }
}
