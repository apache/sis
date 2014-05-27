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

import java.util.Collection;
import java.util.Collections;
import org.opengis.util.LocalName;
import org.opengis.util.GenericName;
import org.opengis.util.NameSpace;
import org.opengis.util.NameFactory;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.apache.sis.util.Static;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.system.DefaultFactories;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * Provides static methods for creating, parsing and formatting {@linkplain AbstractName generic names}.
 *
 * <p>Those convenience methods delegate their work to a default {@linkplain DefaultNameFactory name factory}.
 * Users can get more control by using the name factory directly.</p>
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
     * Converts the given value to an array of generic names. If the given value is an instance of
     * {@link GenericName}, {@link String} or any other type enumerated below, then it is converted
     * and returned in an array of length 1. If the given value is an array or a collection, then an
     * array of same length is returned where each element has been converted.
     *
     * <p>Allowed types or element types are:</p>
     * <ul>
     *   <li>{@link GenericName}, to be casted and returned as-is.</li>
     *   <li>{@link CharSequence} (usually a {@link String} or an {@link InternationalString}),
     *       to be parsed as a generic name using the {@link DefaultNameSpace#DEFAULT_SEPARATOR ':'} separator.</li>
     *   <li>{@link Identifier}, its {@linkplain Identifier#getCode() code} to be parsed as a generic name
     *       using the {@link DefaultNameSpace#DEFAULT_SEPARATOR ':'} separator.</li>
     * </ul>
     *
     * If {@code value} is an array or a collection containing {@code null} elements,
     * then the corresponding element in the returned array will also be {@code null}.
     *
     * @param  value The object to cast into an array of generic names, or {@code null}.
     * @return The generic names, or {@code null} if the given {@code value} was null.
     *         Note that it may be the {@code value} reference itself casted to {@code GenericName[]}.
     * @throws ClassCastException if {@code value} can't be casted.
     */
    public static GenericName[] toGenericNames(Object value) throws ClassCastException {
        if (value == null) {
            return null;
        }
        return toGenericNames(value, DefaultFactories.NAMES);
    }

    /**
     * Implementation of {@link #toGenericName(Object)} using the given factory.
     */
    static GenericName[] toGenericNames(Object value, final NameFactory factory) throws ClassCastException {
        GenericName name = toGenericName(value, factory);
        if (name != null) {
            return new GenericName[] {
                name
            };
        }
        /*
         * Above code checked for a singleton. Now check for a collection or an array.
         */
        final Object[] values;
        if (value instanceof Object[]) {
            values = (Object[]) value;
            if (values instanceof GenericName[]) {
                return (GenericName[]) values;
            }
        } else if (value instanceof Collection<?>) {
            values = ((Collection<?>) value).toArray();
        } else {
            throw new ClassCastException(Errors.format(Errors.Keys.IllegalArgumentClass_2,
                    "value", value.getClass()));
        }
        final GenericName[] names = new GenericName[values.length];
        for (int i=0; i<values.length; i++) {
            value = values[i];
            if (value != null) {
                name = toGenericName(value, factory);
                if (name == null) {
                    throw new ClassCastException(Errors.format(Errors.Keys.IllegalArgumentClass_2,
                            "value[" + i + ']', value.getClass()));
                }
                names[i] = name;
            }
        }
        return names;
    }

    /**
     * Creates a generic name from the given value. The value may be an instance of
     * {@link GenericName}, {@link Identifier} or {@link CharSequence}. If the given
     * object is not recognized, then this method returns {@code null}.
     *
     * @param  value The object to convert.
     * @param  factory The factory to use for creating names.
     * @return The converted object, or {@code null} if {@code value} is not convertible.
     */
    private static GenericName toGenericName(final Object value, final NameFactory factory) {
        if (value instanceof GenericName) {
            return (GenericName) value;
        }
        if (value instanceof Identifier) {
            return factory.parseGenericName(null, ((Identifier) value).getCode());
        }
        if (value instanceof CharSequence) {
            return factory.parseGenericName(null, (CharSequence) value);
        }
        return null;
    }

    /**
     * Formats the given name in <cite>expanded form</cite> close to the Java Content Repository (JCR) definition.
     * The expanded form is defined as below:
     *
     * <blockquote>ExpandedName ::= '{' NameSpace '}' LocalPart
     * NameSpace    ::= {@linkplain AbstractName#scope() scope()}.{@linkplain DefaultNameSpace#name() name()}.toString()
     * LocalPart    ::= name.{@linkplain AbstractName#toString() toString()}</blockquote>
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
