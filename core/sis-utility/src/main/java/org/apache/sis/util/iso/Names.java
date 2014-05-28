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
import org.opengis.util.LocalName;
import org.opengis.util.GenericName;
import org.opengis.util.NameSpace;
import org.opengis.util.InternationalString;
import org.apache.sis.util.Static;
import org.apache.sis.internal.system.DefaultFactories;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * Static methods for creating, parsing and formatting {@linkplain AbstractName generic names}.
 * This convenience class does not add new functionality compared to {@link DefaultNameFactory},
 * but makes some tasks easier by avoiding the need to find a factory, and by creating name and
 * their namespace in a single step.
 *
 * {@section Relationship with Java Content Repository (JCR) names}
 * In the Java standard {@link javax.xml.namespace.QName} class and in the Java Content Repository (JCR) specification,
 * a name is an ordered pair of (<var>Name space</var>, <var>Local part</var>) strings. A JCR name can take two lexical
 * forms: <cite>expanded form</cite> and <cite>qualified form</cite>. Those names are mapped to generic names as below:
 *
 * <blockquote><table class="sis">
 *   <caption>Equivalence between JCR name and {@code GenericName}</caption>
 *   <tr>
 *     <th>JCR name</th>
 *     <th>GeoAPI equivalence</th>
 *   </tr><tr>
 *     <td><pre>ExpandedName  ::= '{' Namespace '}' LocalName</pre></td>
 *     <td>{@code GenericName} with its {@linkplain AbstractName#scope() scope} set to the JCR namespace.</td>
 *   </tr><tr>
 *     <td><pre>QualifiedName ::= [Prefix ':'] LocalName</pre></td>
 *     <td>{@code ScopedName} in the global namespace, with its {@linkplain DefaultScopedName#head() head} or
 *         {@linkplain DefaultScopedName#path() path} set to the JCR prefix.</td>
 *   </tr>
 * </table></blockquote>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
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
     * @param  namespace The namespace string, taken as a whole (not parsed).
     * @param  separator The separator between the namespace and the local part, or {@code null} for the default.
     * @return The namespace object.
     */
    private static NameSpace createNameSpace(final CharSequence namespace, final String separator) {
        if (namespace == null) {
            return null;
        }
        return DefaultFactories.NAMES.createNameSpace(
                DefaultFactories.NAMES.createLocalName(null, namespace),
                (separator == null) ? null : Collections.singletonMap("separator.head", separator));
    }

    /**
     * Creates a name which is local in the given namespace, using the
     * {@linkplain DefaultNameSpace#DEFAULT_SEPARATOR} default separator}.
     * The character sequences can be either {@link String} or {@link InternationalString} instances.
     * Those character sequences are taken verbatim; they are <em>not</em> parsed into their components.
     *
     * <p>This method creates a name with the following characteristics:</p>
     * <ul>
     *   <li><code>name.{@linkplain DefaultLocalName#scope() scope()}.name().toString()</code> contains the given {@code namespace}.</li>
     *   <li><code>name.{@linkplain DefaultLocalName#toString() toString()}</code> contains the given {@code localPart}.</li>
     * </ul>
     *
     * @param  namespace The namespace, or {@code null} for the global namespace.
     * @param  localPart The name which is locale in the given namespace.
     * @return A local name in the given namespace.
     */
    public static LocalName createLocalName(final CharSequence namespace, final CharSequence localPart) {
        ensureNonNull("localPart", localPart);
        return DefaultFactories.NAMES.createLocalName(createNameSpace(namespace, null), localPart);
    }

    /**
     * Creates a name which is local in the given namespace, using the given separator.
     * The character sequences can be either {@link String} or {@link InternationalString} instances.
     * Those character sequences are taken verbatim; they are <em>not</em> parsed into their components.
     *
     * <p>This method creates a name with the following characteristics:</p>
     * <ul>
     *   <li><code>name.{@linkplain DefaultLocalName#scope() scope()}.name().toString()</code> contains the given {@code namespace}.</li>
     *   <li><code>name.{@linkplain DefaultLocalName#toString() toString()}</code> contains the given {@code localPart}.</li>
     * </ul>
     *
     * @param  namespace The namespace, or {@code null} for the global namespace.
     * @param  separator The separator between the namespace and the local part.
     * @param  localPart The name which is locale in the given namespace.
     * @return A local name in the given namespace.
     */
    public static LocalName createLocalName(final CharSequence namespace, final String separator, final CharSequence localPart) {
        ensureNonNull("localPart", localPart);
        ensureNonNull("separator", separator);
        return DefaultFactories.NAMES.createLocalName(createNameSpace(namespace, separator), localPart);
    }

    /**
     * Formats the given name in <cite>expanded form</cite> close to the Java Content Repository (JCR) definition.
     * The expanded form is defined as below:
     *
     * <blockquote><pre> ExpandedName ::= '{' NameSpace '}' LocalPart
     * NameSpace    ::= {@linkplain AbstractName#scope() scope()}.{@linkplain DefaultNameSpace#name() name()}.toString()
     * LocalPart    ::= name.{@linkplain AbstractName#toString() toString()}</pre></blockquote>
     *
     * @param  name The generic name to format in expanded form.
     * @return Expanded form of the given generic name.
     *
     * @see DefaultNameSpace#toString()
     */
    public static String toExpandedString(final GenericName name) {
        ensureNonNull("name", name);
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
