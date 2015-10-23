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

import java.util.List;
import java.util.Locale;
import java.util.Iterator;
import java.util.ConcurrentModificationException;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlTransient;
import org.opengis.util.NameFactory;
import org.opengis.util.NameSpace;
import org.opengis.util.LocalName;
import org.opengis.util.ScopedName;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.util.resources.Errors;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Base class for sequence of identifiers rooted within the context of a {@linkplain DefaultNameSpace namespace}.
 * Names shall be <em>immutable</em> and thread-safe. A name can be local to a namespace.
 * See the {@linkplain org.apache.sis.util.iso package javadoc} for an illustration of name anatomy.
 *
 * <p>The easiest way to create a name is to use the {@link Names#createLocalName(CharSequence, String, CharSequence)}
 * convenience static method. That method supports the common case where the name is made only of a
 * (<var>namespace</var>, <var>local part</var>) pair of strings. However generic names allows finer grain.
 * For example the above-cited strings can both be split into smaller name components.
 * If such finer grain control is desired, {@link DefaultNameFactory} can be used instead of {@link Names}.</p>
 *
 * <div class="section">Natural ordering</div>
 * This class has a natural ordering that is inconsistent with {@link #equals(Object)}.
 * See {@link #compareTo(GenericName)} for more information.
 *
 * <div class="section">Note for implemetors</div>
 * Subclasses need only to implement the following methods:
 * <ul>
 *   <li>{@link #scope()}</li>
 *   <li>{@link #getParsedNames()}</li>
 * </ul>
 *
 * Subclasses shall make sure that any overridden methods remain safe to call from multiple threads
 * and do not change any public {@code GenericName} state.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */

/*
 * JAXB annotation would be @XmlType(name ="CodeType"), but this can not be used here
 * since "CodeType" is used for various classes (including LocalName and ScopedName).
 */
@XmlTransient
public abstract class AbstractName implements GenericName, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 667242702456713391L;

    /**
     * A view of this name as a fully-qualified one.
     * Will be created only when first needed.
     */
    transient GenericName fullyQualified;

    /**
     * The string representation of this name, to be returned by {@link #toString()} or
     * {@link #toInternationalString()}. This field will initially references a {@link String}
     * object when first needed, and may be replaced by a {@link InternationalString} object
     * later if such object is asked for.
     */
    transient CharSequence asString;

    /**
     * The cached hash code, or {@code 0} if not yet computed.
     */
    private transient int hash;

    /**
     * Creates a new instance of generic name.
     */
    protected AbstractName() {
    }

    /**
     * Returns a SIS name implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of {@link LocalName}, then this
     *       method delegates to {@link DefaultLocalName#castOrCopy(LocalName)}.</li>
     *   <li>Otherwise if the given object is already an instance of {@code AbstractName},
     *       then it is returned unchanged.</li>
     *   <li>Otherwise a new instance of an {@code AbstractName} subclass is created using the
     *       {@link DefaultNameFactory#createGenericName(NameSpace, CharSequence[])} method.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static AbstractName castOrCopy(final GenericName object) {
        if (object instanceof LocalName) {
            return DefaultLocalName.castOrCopy((LocalName) object);
        }
        if (object == null || object instanceof AbstractName) {
            return (AbstractName) object;
        }
        /*
         * Recreates a new name for the given name in order to get
         * a SIS implementation from an arbitrary implementation.
         */
        final List<? extends LocalName> parsedNames = object.getParsedNames();
        final CharSequence[] names = new CharSequence[parsedNames.size()];
        int i=0;
        for (final LocalName component : parsedNames) {
            names[i++] = component.toInternationalString();
        }
        if (i != names.length) {
            throw new ConcurrentModificationException(Errors.format(Errors.Keys.UnexpectedChange_1, "parsedNames"));
        }
        /*
         * Following cast should be safe because DefaultFactories.forBuildin(Class) filters the factories in
         * order to return the Apache SIS implementation, which is known to create AbstractName instances.
         */
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
        return (AbstractName) factory.createGenericName(object.scope(), names);
    }

    /**
     * Returns the scope (name space) in which this name is local. For example if a
     * {@linkplain #toFullyQualifiedName() fully qualified name} is {@code "org.opengis.util.Record"}
     * and if this instance is the {@code "util.Record"} part, then its scope is
     * {@linkplain DefaultNameSpace#name() named} {@code "org.opengis"}.
     *
     * <p>Continuing with the above example, the full {@code "org.opengis.util.Record"} name has
     * no scope. If this method is invoked on such name, then the SIS implementation returns a
     * global scope instance (i.e. an instance for which {@link DefaultNameSpace#isGlobal()}
     * returns {@code true}) which is unique and named {@code "global"}.</p>
     *
     * @return The scope of this name.
     */
    @Override
    public abstract NameSpace scope();

    /**
     * Indicates the number of levels specified by this name. The default implementation returns
     * the size of the list returned by the {@link #getParsedNames()} method.
     *
     * @return The depth of this name.
     */
    @Override
    public int depth() {
        return getParsedNames().size();
    }

    /**
     * Returns the size of the backing array. This is used only has a hint for optimizations
     * in attempts to share internal arrays. The {@link DefaultScopedName} class is the only
     * one to override this method. For other classes, the {@link #depth()} can be assumed.
     */
    int arraySize() {
        return depth();
    }

    /**
     * Returns the sequence of {@linkplain DefaultLocalName local names} making this generic name.
     * The length of this sequence is the {@linkplain #depth() depth}. It does not include the
     * {@linkplain #scope() scope}.
     *
     * @return The local names making this generic name, without the {@linkplain #scope() scope}.
     *         Shall never be {@code null} neither empty.
     */
    @Override
    public abstract List<? extends LocalName> getParsedNames();

    /**
     * Returns the first element in the sequence of {@linkplain #getParsedNames() parsed names}.
     * For any {@code LocalName}, this is always {@code this}.
     *
     * <div class="note"><b>Example:</b>
     * If {@code this} name is {@code "org.opengis.util.Record"}
     * (no matter its scope, then this method returns {@code "org"}.</div>
     *
     * @return The first element in the list of {@linkplain #getParsedNames() parsed names}.
     */
    @Override
    public LocalName head() {
        return getParsedNames().get(0);
    }

    /**
     * Returns the last element in the sequence of {@linkplain #getParsedNames() parsed names}.
     * For any {@code LocalName}, this is always {@code this}.
     *
     * <div class="note"><b>Example:</b>
     * If {@code this} name is {@code "org.opengis.util.Record"}
     * (no matter its scope, then this method returns {@code "Record"}.</div>
     *
     * @return The last element in the list of {@linkplain #getParsedNames() parsed names}.
     */
    @Override
    public LocalName tip() {
        final List<? extends LocalName> names = getParsedNames();
        return names.get(names.size() - 1);
    }

    /**
     * Returns a view of this name as a fully-qualified name. The {@linkplain #scope() scope}
     * of a fully qualified name is {@linkplain DefaultNameSpace#isGlobal() global}.
     * If the scope of this name is already global, then this method returns {@code this}.
     *
     * @return The fully-qualified name (never {@code null}).
     */
    @Override
    public synchronized GenericName toFullyQualifiedName() {
        if (fullyQualified == null) {
            final NameSpace scope = scope();
            if (scope.isGlobal()) {
                fullyQualified = this;
            } else {
                final GenericName prefix = scope.name();
                assert prefix.scope().isGlobal() : prefix;
                fullyQualified = new DefaultScopedName(prefix, this);
            }
        }
        return fullyQualified;
    }

    /**
     * Returns this name expanded with the specified scope. One may represent this operation
     * as a concatenation of the specified {@code scope} with {@code this}. For example if
     * {@code this} name is {@code "util.Record"} and the given {@code scope} argument is
     * {@code "org.opengis"}, then {@code this.push(scope)} shall return
     * {@code "org.opengis.util.Record"}.
     *
     * @param  scope The name to use as prefix.
     * @return A concatenation of the given scope with this name.
     */
    @Override
    public ScopedName push(final GenericName scope) {
        return new DefaultScopedName(scope, this);
    }

    /**
     * Returns the separator to write before the given name. If the scope of the given name
     * is a {@link DefaultNameSpace} instance, then this method returns its head separator.
     * We really want {@link DefaultNameSpace#headSeparator}, not {@link DefaultNameSpace#separator}.
     * See {@link DefaultNameSpace#child(CharSequence)} for details.
     *
     * @param  name The name after which to write a separator.
     * @return The separator to write after the given name.
     */
    static String separator(final GenericName name) {
        if (name != null) {
            final NameSpace scope = name.scope();
            if (scope instanceof DefaultNameSpace) {
                return ((DefaultNameSpace) scope).headSeparator;
            }
        }
        return DefaultNameSpace.DEFAULT_SEPARATOR_STRING;
    }

    /**
     * Returns a string representation of this generic name. This string representation
     * is local-independent. It contains all elements listed by {@link #getParsedNames()}
     * separated by a namespace-dependent character (usually {@code ':'} or {@code '/'}).
     * This rule implies that the result may or may not be fully qualified.
     * Special cases:
     *
     * <ul>
     *   <li><code>{@linkplain #toFullyQualifiedName()}.toString()</code> is guaranteed to
     *       contain the {@linkplain #scope() scope} (if any).</li>
     *   <li><code>{@linkplain #tip()}.toString()</code> is guaranteed to not contain
     *       any scope.</li>
     * </ul>
     *
     * @return A local-independent string representation of this name.
     */
    @Override
    public synchronized String toString() {
        if (asString == null) {
            boolean insertSeparator = false;
            final StringBuilder buffer = new StringBuilder();
            for (final LocalName name : getParsedNames()) {
                if (insertSeparator) {
                    buffer.append(separator(name));
                }
                insertSeparator = true;
                buffer.append(name);
            }
            asString = buffer.toString();
        }
        // Note: there is no need to invoke InternationalString.toString(Locale.ROOT) for
        // the unlocalized version, because our International inner class is implemented in
        // such a way that InternationalString.toString() returns AbstractName.toString().
        return asString.toString();
    }

    /**
     * Returns a local-dependent string representation of this generic name.
     * This string is similar to the one returned by {@link #toString()} except that each element
     * has been localized in the {@linkplain InternationalString#toString(Locale) specified locale}.
     * If no international string is available, then this method returns an implementation mapping
     * to {@link #toString()} for all locales.
     *
     * @return A localizable string representation of this name.
     */
    @Override
    public synchronized InternationalString toInternationalString() {
        if (!(asString instanceof InternationalString)) {
            asString = new International(toString(), getParsedNames());
        }
        return (InternationalString) asString;
    }

    /**
     * An international string built from a snapshot of {@link GenericName}.
     * This class is immutable is the list given to the constructor is immutable.
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     */
    private static final class International extends SimpleInternationalString {
        /**
         * Serial number for inter-operability with different versions.
         */
        private static final long serialVersionUID = -5259001179796274879L;

        /**
         * The sequence of {@linkplain DefaultLocalName local names} making this generic name.
         * This is the value returned by {@link AbstractName#getParsedNames()}.
         */
        private final List<? extends LocalName> parsedNames;

        /**
         * Constructs a new international string from the specified {@link AbstractName} fields.
         *
         * @param asString The string representation of the enclosing abstract name.
         * @param parsedNames The value returned by {@link AbstractName#getParsedNames()}.
         */
        International(final String asString, final List<? extends LocalName> parsedNames) {
            super(asString);
            this.parsedNames = parsedNames;
        }

        /**
         * Returns a string representation for the specified locale.
         */
        @Override
        public String toString(final Locale locale) {
            boolean insertSeparator = false;
            final StringBuilder buffer = new StringBuilder();
            for (final LocalName name : parsedNames) {
                if (insertSeparator) {
                    buffer.append(separator(name));
                }
                insertSeparator = true;
                buffer.append(name.toInternationalString().toString(locale));
            }
            return buffer.toString();
        }

        /**
         * Compares this international string with the specified object for equality.
         */
        @Override
        public boolean equals(final Object object) {
            if (object == this) {
                return true;
            }
            if (super.equals(object)) {
                final International that = (International) object;
                return Objects.equals(parsedNames, that.parsedNames);
            }
            return false;
        }

        /**
         * Returns a hash code value for this international text.
         */
        @Override
        public int hashCode() {
            return parsedNames.hashCode() ^ (int) serialVersionUID;
        }
    }

    /**
     * Compares this name with the specified name for order. Returns a negative integer,
     * zero, or a positive integer as this name lexicographically precedes, is equal to,
     * or follows the specified name. The comparison is performed in the following way:
     *
     * <ul>
     *   <li>For each element of the {@linkplain #getParsedNames() list of parsed names} taken
     *       in iteration order, compare the {@link LocalName}. If a name lexicographically
     *       precedes or follows the corresponding element of the specified name, returns
     *       a negative or a positive integer respectively.</li>
     *   <li>If all elements in both names are lexicographically equal, then if this name has less
     *       or more elements than the specified name, returns a negative or a positive integer
     *       respectively.</li>
     *   <li>Otherwise, returns 0.</li>
     * </ul>
     *
     * @param name The other name to compare with this name.
     * @return -1 if this name precedes the given one, +1 if it follows, 0 if equals.
     */
    @Override
    public int compareTo(final GenericName name) {
        final Iterator<? extends LocalName> thisNames = this.getParsedNames().iterator();
        final Iterator<? extends LocalName> thatNames = name.getParsedNames().iterator();
        while (thisNames.hasNext()) {
            if (!thatNames.hasNext()) {
                return +1;
            }
            final LocalName thisNext = thisNames.next();
            final LocalName thatNext = thatNames.next();
            if (thisNext == this && thatNext == name) {
                // Never-ending loop: usually an implementation error
                throw new IllegalStateException(Errors.format(Errors.Keys.CircularReference));
            }
            final int compare = thisNext.compareTo(thatNext);
            if (compare != 0) {
                return compare;
            }
        }
        return thatNames.hasNext() ? -1 : 0;
    }

    /**
     * Compares this generic name with the specified object for equality.
     * The default implementation returns {@code true} if the {@linkplain #scope() scopes}
     * and the lists of {@linkplain #getParsedNames() parsed names} are equal.
     *
     * @param object The object to compare with this name for equality.
     * @return {@code true} if the given object is equal to this name.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object != null && object.getClass() == getClass()) {
            final AbstractName that = (AbstractName) object;
            return Objects.equals(scope(), that.scope()) &&
                   Objects.equals(getParsedNames(), that.getParsedNames());
        }
        return false;
    }

    /**
     * Returns a hash code value for this generic name.
     */
    @Override
    public int hashCode() {
        if (hash == 0) {
            int code = computeHashCode();
            if (code == 0) {
                code = -1;
            }
            hash = code;
        }
        return hash;
    }

    /**
     * Invoked by {@link #hashCode()} for computing the hash code value when first needed.
     */
    int computeHashCode() {
        return Objects.hash(scope(), getParsedNames()) ^ (int) serialVersionUID;
    }
}
