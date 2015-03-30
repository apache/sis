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

import java.util.Collections;
import org.opengis.util.TypeName;
import org.opengis.util.LocalName;
import org.opengis.util.MemberName;
import org.opengis.util.GenericName;
import org.opengis.util.NameSpace;
import org.opengis.util.NameFactory;
import org.opengis.util.InternationalString;
import org.apache.sis.util.Static;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.util.UnknownNameException;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * Static methods for creating, parsing and formatting {@linkplain AbstractName generic names}.
 * This convenience class does not add new functionality compared to {@link DefaultNameFactory},
 * but makes some tasks easier by avoiding the need to find a factory, and by creating name and
 * their namespace in a single step.
 *
 * <div class="section">Relationship with Java Content Repository (JCR) names</div>
 * In the Java standard {@link javax.xml.namespace.QName} class and in the Java Content Repository (JCR) specification,
 * a name is an ordered pair of ({@code namespace}, {@code localPart}) strings. A JCR name can take two lexical forms:
 * <cite>expanded form</cite> and <cite>qualified form</cite>. Those names are mapped to generic names as below:
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
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see DefaultNameFactory
 * @see DefaultNameSpace
 * @see DefaultScopedName
 * @see DefaultLocalName
 * @see DefaultTypeName
 * @see DefaultMemberName
 */
public final class Names extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private Names() {
    }

    /**
     * Creates a namespace for the given name.
     *
     * @param  factory   The factory to use for creating the namespace.
     * @param  namespace The namespace string, taken as a whole (not parsed).
     * @param  separator The separator between the namespace and the local part, or {@code null} for the default.
     * @return The namespace object.
     */
    private static NameSpace createNameSpace(final NameFactory factory, final CharSequence namespace, final String separator) {
        if (namespace == null || namespace.length() == 0) {
            return null;
        }
        return factory.createNameSpace(factory.createLocalName(null, namespace),
                (separator == null) ? null : Collections.singletonMap("separator.head", separator));
    }

    /**
     * Creates a local or scoped name in the given namespace.
     * The character sequences can be either {@link String} or {@link InternationalString} instances.
     * The {@code namespace} character sequences is taken verbatim, while {@code scopedName} is parsed
     * as described in {@linkplain DefaultNameFactory#createGenericName(NameSpace, CharSequence...) name factory}.
     *
     * @param  namespace  The namespace, or {@code null} for the global namespace.
     * @param  separator  The separator between the namespace and the scoped name.
     * @param  scopedName The name to parse.
     * @return A local or scoped name in the given namespace.
     */
    public static GenericName parseGenericName(final CharSequence namespace, final String separator, final CharSequence scopedName) {
        ensureNonNull("localPart", scopedName);
        ensureNonNull("separator", separator);
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
        return factory.parseGenericName(createNameSpace(factory, namespace, separator), scopedName);
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
     * <div class="note"><b>Example:</b>
     * for a name created by {@code createLocalName("http://www.opengis.net/gml/srs/epsg.xml", "#", "4326")}:
     * <blockquote><table class="compact" summary="Examples of return values for a name built by this method.">
     *   <tr><td>• <code>name.{@linkplain DefaultLocalName#toString() toString()}</code></td>
     *       <td>returns the {@code "4326"} string.</td></tr>
     *   <tr><td>• <code>name.{@linkplain DefaultLocalName#scope() scope()}</code></td>
     *       <td>returns the {@code "http://www.opengis.net/gml/srs/epsg.xml"} namespace.</td></tr>
     *   <tr><td>• <code>name.{@linkplain DefaultLocalName#toFullyQualifiedName() toFullyQualifiedName()}</code></td>
     *       <td>returns the {@code "http://www.opengis.net/gml/srs/epsg.xml#4326"} name.
     *   <tr><td>• <code>{@linkplain #toExpandedString(GenericName) toExpandedString}(name)</code></td>
     *       <td>returns the {@code "{http://www.opengis.net/gml/srs/epsg.xml}4326"} string.</td></tr>
     * </table></blockquote></div>
     *
     * <div class="note"><b>Performance note:</b> this method is okay for <em>casual</em> use. If many names need
     * to be created in the same namespace, then {@link DefaultNameFactory#createLocalName(NameSpace, CharSequence)}
     * is more efficient since it allows to create the {@code NameSpace} object only once.</div>
     *
     * @param  namespace The namespace, or {@code null} for the global namespace.
     * @param  separator The separator between the namespace and the local part.
     * @param  localPart The name which is locale in the given namespace.
     * @return A local name in the given namespace.
     */
    public static LocalName createLocalName(final CharSequence namespace, final String separator, final CharSequence localPart) {
        ensureNonNull("localPart", localPart);
        ensureNonNull("separator", separator);
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
        return factory.createLocalName(createNameSpace(factory, namespace, separator), localPart);
    }

    /**
     * Creates a type name which is local in the given namespace.
     * The character sequences can be either {@link String} or {@link InternationalString} instances.
     * Those character sequences are taken verbatim; they are <em>not</em> parsed into their components.
     *
     * <div class="note"><b>Example:</b> {@code createTypeName("gco", ":", "Integer")} returns a name
     * which can be used for representing the type of {@code <gco:Integer>} elements in XML files.</div>
     *
     * <div class="note"><b>Performance note:</b> this method is okay for <em>casual</em> use. If many names need
     * to be created in the same namespace, then {@link DefaultNameFactory#createTypeName(NameSpace, CharSequence)}
     * is more efficient since it allows to create the {@code NameSpace} object only once.</div>
     *
     * @param  namespace The namespace, or {@code null} for the global namespace.
     * @param  separator The separator between the namespace and the local part.
     * @param  localPart The name which is locale in the given namespace.
     * @return A type name in the given namespace.
     */
    public static TypeName createTypeName(final CharSequence namespace, final String separator, final CharSequence localPart) {
        ensureNonNull("localPart", localPart);
        ensureNonNull("separator", separator);
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
        return factory.createTypeName(createNameSpace(factory, namespace, separator), localPart);
    }

    /**
     * Creates a member name for values of the given class. A {@link TypeName} will be inferred
     * from the given {@code valueClass} as documented in the {@link DefaultTypeName} javadoc.
     *
     * <div class="note"><b>Performance note:</b> this method is okay for <em>casual</em> use. If many names
     * need to be created, then {@link DefaultNameFactory#createMemberName(NameSpace, CharSequence, TypeName)}
     * is more efficient since it allows to create the {@code NameSpace} and {@code TypeName} objects only once.</div>
     *
     * @param  namespace  The namespace, or {@code null} for the global namespace.
     * @param  separator  The separator between the namespace and the local part.
     * @param  localPart  The name which is locale in the given namespace.
     * @param  valueClass The type of values, used for inferring a {@link TypeName} instance.
     * @return A member name in the given namespace for values of the given type.
     */
    public static MemberName createMemberName(final CharSequence namespace, final String separator,
            final CharSequence localPart, final Class<?> valueClass)
    {
        ensureNonNull("localPart",  localPart);
        ensureNonNull("separator",  separator);
        ensureNonNull("valueClass", valueClass);
        final DefaultNameFactory factory = DefaultFactories.forBuildin(NameFactory.class, DefaultNameFactory.class);
        return factory.createMemberName(createNameSpace(factory, namespace, separator), localPart,
                factory.toTypeName(valueClass));    // SIS-specific method.
    }

    /**
     * Returns the Java class associated to the given type name.
     * The method performs the following choices:
     *
     * <ul>
     *   <li>If the given type name is {@code null}, then this method returns {@code null}.</li>
     *   <li>Else if the given type name is an instance of {@code DefaultTypeName},
     *       then this method delegates to {@link DefaultTypeName#toClass()}.</li>
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
     *           could be anything, so we can not be sure that the given name was wrong.</li>
     *     </ul>
     *   </li>
     *   <li>Otherwise {@code null} is returned, since this method can not check the validity of names in other
     *       namespaces.</li>
     * </ul>
     *
     * @param  type The type name from which to infer a Java class.
     * @return The Java class associated to the given {@code TypeName},
     *         or {@code null} if there is no mapping from the given name to a Java class.
     * @throws UnknownNameException if a mapping from the given name to a Java class was expected to exist
     *         (typically because of the {@linkplain DefaultTypeName#scope() scope}) but the operation failed.
     *
     * @see DefaultTypeName#toClass()
     * @see DefaultNameFactory#toTypeName(Class)
     *
     * @since 0.5
     */
    public static Class<?> toClass(final TypeName type) throws UnknownNameException {
        if (type == null) {
            return null;
        }
        Class<?> c;
        if (type instanceof DefaultTypeName) {
            c = ((DefaultTypeName) type).toClass();
        } else {
            try {
                c = TypeNames.toClass(TypeNames.namespace(type.scope()), type.toString());
            } catch (ClassNotFoundException e) {
                throw new UnknownNameException(TypeNames.unknown(type), e);
            }
            if (c == null) {
                throw new UnknownNameException(TypeNames.unknown(type));
            }
            if (c == Void.TYPE) {
                c = null;
            }
        }
        return c;
    }

    /**
     * Formats the given name in <cite>expanded form</cite> close to the Java Content Repository (JCR) definition.
     * The expanded form is defined as below:
     *
     * <blockquote><pre> ExpandedName ::= '{' NameSpace '}' LocalPart
     * NameSpace    ::= name.{@linkplain AbstractName#scope() scope()}.{@linkplain DefaultNameSpace#name() name()}.toString()
     * LocalPart    ::= name.{@linkplain AbstractName#toString() toString()}</pre></blockquote>
     *
     * @param  name The generic name to format in expanded form, or {@code null}.
     * @return Expanded form of the given generic name, or {@code null} if the given name was null.
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
