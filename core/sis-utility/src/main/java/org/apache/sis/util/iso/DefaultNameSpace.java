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
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.io.Serializable;
import org.opengis.util.NameSpace;
import org.opengis.util.LocalName;
import org.opengis.util.ScopedName;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.apache.sis.util.Debug;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.internal.util.UnmodifiableArrayList;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * A domain in which {@linkplain AbstractName names} given by character strings are defined.
 * This implementation does not support localization in order to avoid ambiguity when testing
 * two namespaces for {@linkplain #equals(Object) equality}.
 *
 * <p>{@code DefaultNameSpace} can be instantiated by any of the following methods:</p>
 * <ul>
 *   <li>{@link DefaultNameFactory#createNameSpace(GenericName, Map)}</li>
 * </ul>
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus inherently thread-safe if the {@link NameSpace} and {@link CharSequence}
 * arguments given to the constructor are also immutable. Subclasses shall make sure that any overridden methods
 * remain safe to call from multiple threads and do not change any public {@code NameSpace} state.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see DefaultScopedName
 * @see DefaultLocalName
 * @see DefaultTypeName
 * @see DefaultMemberName
 * @see DefaultNameFactory
 */
public class DefaultNameSpace implements NameSpace, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8272640747799127007L;

    /**
     * The default separator, which is {@code ':'}. The separator is inserted between
     * the namespace and any {@linkplain GenericName generic name} in that namespace.
     */
    public static final char DEFAULT_SEPARATOR = ':';

    /**
     * {@link #DEFAULT_SEPARATOR} as a {@link String}.
     */
    static final String DEFAULT_SEPARATOR_STRING = ":";

    /**
     * The parent namespace, or {@code null} if the parent is the unique {@code GLOBAL} instance.
     * We don't use direct reference to {@code GLOBAL} because {@code null} is used as a sentinel
     * value for stopping iterative searches (using GLOBAL would have higher risk of never-ending
     * loops in case of bug), and in order to reduce the stream size during serialization.
     *
     * @see #parent()
     */
    private final DefaultNameSpace parent;

    /**
     * The name of this namespace, usually as a {@link String} or an {@link InternationalString}.
     */
    private final CharSequence name;

    /**
     * The separator to insert between the namespace and the {@linkplain AbstractName#head() head}
     * of any name in that namespace.
     */
    final String headSeparator;

    /**
     * The separator to insert between the {@linkplain AbstractName#getParsedNames() parsed names}
     * of any name in that namespace.
     */
    final String separator;

    /**
     * The fully qualified name for this namespace.
     * Will be created when first needed.
     */
    private transient AbstractName path;

    /**
     * The children created in this namespace. The values are restricted to the following types:
     *
     * <ul>
     *   <li>{@link DefaultNameSpace}</li>
     *   <li>{@link DefaultLocalName}</li>
     * </ul>
     *
     * No other type should be allowed. The main purpose of this map is to hold child namespaces.
     * However we can (in an opportunist way) handles local names as well. In case of conflict,
     * the namespace will have precedence.
     *
     * <p>This field is initialized by {@link #init()} soon after {@code DefaultNameSpace} creation
     * and shall be treated like a final field from that point.</p>
     */
    private transient WeakValueHashMap<String,Object> childs;

    /**
     * Creates the global namespace. This constructor can be invoked by {@link GlobalNameSpace} only.
     */
    DefaultNameSpace() {
        this.parent        = null;
        this.name          = "global";
        this.headSeparator = DEFAULT_SEPARATOR_STRING;
        this.separator     = DEFAULT_SEPARATOR_STRING;
        init();
    }

    /**
     * Creates a new namespace with the given separator.
     *
     * @param parent
     *          The parent namespace, or {@code null} if none.
     * @param name
     *          The name of the new namespace, usually as a {@link String}
     *          or an {@link InternationalString}.
     * @param headSeparator
     *          The separator to insert between the namespace and the
     *          {@linkplain AbstractName#head() head} of any name in that namespace.
     * @param separator
     *          The separator to insert between the {@linkplain AbstractName#getParsedNames()
     *          parsed names} of any name in that namespace.
     */
    protected DefaultNameSpace(final DefaultNameSpace parent, CharSequence name,
                               final String headSeparator, final String separator)
    {
        this.parent = (parent != GlobalNameSpace.GLOBAL) ? parent : null;
        ensureNonNull("name",          name);
        ensureNonNull("headSeparator", headSeparator);
        ensureNonNull("separator",     separator);
        if (!(name instanceof InternationalString)) {
            name = name.toString();
        }
        this.name          = name;
        this.headSeparator = headSeparator;
        this.separator     = separator;
        init();
    }

    /**
     * Initializes the transient fields.
     */
    private void init() {
        childs = new WeakValueHashMap<String,Object>(String.class);
    }

    /**
     * Wraps the given namespace in a {@code DefaultNameSpace} implementation.
     * This method returns an existing instance when possible.
     *
     * @param  ns The namespace to wrap, or {@code null} for the global one.
     * @return The given namespace as a {@code DefaultNameSpace} implementation.
     */
    static DefaultNameSpace castOrCopy(final NameSpace ns) {
        if (ns == null) {
            return GlobalNameSpace.GLOBAL;
        }
        if (ns instanceof DefaultNameSpace) {
            return (DefaultNameSpace) ns;
        }
        return forName(ns.name(), DEFAULT_SEPARATOR_STRING, DEFAULT_SEPARATOR_STRING);
    }

    /**
     * Returns a namespace having the given name and separators.
     * This method returns an existing instance when possible.
     *
     * @param name
     *          The name for the namespace to obtain, or {@code null}.
     * @param headSeparator
     *          The separator to insert between the namespace and the
     *          {@linkplain AbstractName#head() head} of any name in that namespace.
     * @param separator
     *          The separator to insert between the {@linkplain AbstractName#getParsedNames()
     *          parsed names} of any name in that namespace.
     * @return A namespace having the given name, or {@code null} if name was null.
     */
    static DefaultNameSpace forName(final GenericName name, final String headSeparator, final String separator) {
        if (name == null) {
            return null;
        }
        final List<? extends LocalName> parsedNames = name.getParsedNames();
        final ListIterator<? extends LocalName> it = parsedNames.listIterator(parsedNames.size());
        NameSpace scope;
        /*
         * Searches for the last parsed name having a DefaultNameSpace implementation as its
         * scope. It should be the tip in most cases. If we don't find any, we will recreate
         * the whole chain starting with the global scope.
         */
        do {
            if (!it.hasPrevious()) {
                scope = GlobalNameSpace.GLOBAL;
                break;
            }
            scope = it.previous().scope();
        } while (!(scope instanceof DefaultNameSpace));
        /*
         * We have found a scope. Adds to it the supplemental names.
         * In most cases we should have only the tip to add.
         */
        DefaultNameSpace ns = (DefaultNameSpace) scope;
        while (it.hasNext()) {
            final LocalName tip = it.next();
            ns = ns.child(tip.toString(), tip.toInternationalString(), headSeparator, separator);
        }
        return ns;
    }

    /**
     * Indicates whether this namespace is a "top level" namespace.  Global, or top-level
     * namespaces are not contained within another namespace. The global namespace has no
     * parent.
     *
     * @return {@code true} if this namespace is the global namespace.
     */
    @Override
    public boolean isGlobal() {
        return false;               // To be overridden by GlobalNameSpace.
    }

    /**
     * Returns the parent namespace, replacing null parent by {@link GlobalNameSpace#GLOBAL}.
     */
    final DefaultNameSpace parent() {
        return (parent != null) ? parent : GlobalNameSpace.GLOBAL;
    }

    /**
     * Returns the depth of the given namespace.
     *
     * @param ns The namespace for which to get the depth, or {@code null}.
     * @return The depth of the given namespace.
     */
    private static int depth(DefaultNameSpace ns) {
        int depth = 0;
        if (ns != null) do {
            depth++;
            ns = ns.parent;
        } while (ns != null && !ns.isGlobal());
        return depth;
    }

    /**
     * Represents the identifier of this namespace. Namespace identifiers shall be
     * {@linkplain AbstractName#toFullyQualifiedName() fully-qualified names} where
     * the following condition holds:
     *
     * {@preformat java
     *     assert name.scope().isGlobal() == true;
     * }
     *
     * @return The identifier of this namespace.
     */
    @Override
    public GenericName name() {
        final int depth;
        synchronized (this) {
            if (path != null) {
                return path;
            }
            depth = depth(this);
            final DefaultLocalName[] names = new DefaultLocalName[depth];
            DefaultNameSpace scan = this;
            for (int i=depth; --i>=0;) {
                names[i] = new DefaultLocalName(scan.parent, scan.name);
                scan = scan.parent;
            }
            assert depth(scan) == 0 || scan.isGlobal();
            path = DefaultScopedName.create(UnmodifiableArrayList.wrap(names));
            GenericName truncated = path;
            for (int i=depth; --i>=0;) {
                names[i].fullyQualified = truncated;
                truncated = (truncated instanceof ScopedName) ? ((ScopedName) truncated).path() : null;
            }
        }
        /*
         * At this point the name is created and ready to be returned. As an optimization,
         * defines the name of parents now in order to share subarea of the array we just
         * created. The goal is to have less objects in memory.
         */
        AbstractName truncated = path;
        DefaultNameSpace scan = parent;
        while (scan != null && !scan.isGlobal()) {
            /*
             * If we have a parent, then depth >= 2 and consequently the name is a ScopedName.
             * Actually it should be an instance of DefaultScopedName - we known that since we
             * created it ourself with the DefaultScopedName.create(...) method call - and we
             * know that its tail() implementation creates instance of AbstractName. Given all
             * the above, none of the casts on the line below should ever fails, unless there
             * is bug in this package.
             */
            truncated = (AbstractName) ((ScopedName) truncated).path();
            synchronized (scan) {
                if (scan.path == null || scan.path.arraySize() < depth) {
                    scan.path = truncated;
                }
            }
            scan = scan.parent;
        }
        return path;
    }

    /**
     * Returns a child namespace of the given name. The returned namespace will
     * have this namespace as its parent, and will use the same separator.
     *
     * <p>The {@link #headSeparator} is not inherited by the children on intend, because this
     * method is used only by {@link DefaultScopedName} constructors in order to create a
     * sequence of parsed local names. For example in {@code "http://www.opengeospatial.org"}
     * the head separator is {@code "://"} for {@code "www"} (which is having this namespace),
     * but it is {@code "."} for all children ({@code "opengeospatial"} and {@code "org"}).</p>
     *
     * @param  name The name of the child namespace.
     * @return The child namespace. It may be an existing instance.
     */
    final DefaultNameSpace child(final CharSequence name) {
        return child(key(name), name, separator, separator);
    }

    /**
     * Returns a key to be used in the {@linkplain #childs} pool from the given name.
     * The key must be the unlocalized version of the given string.
     *
     * @param name The name.
     * @return A key from the given name.
     */
    private static String key(final CharSequence name) {
        return (name instanceof InternationalString) ?
                ((InternationalString) name).toString(Locale.ROOT) : name.toString();
    }

    /**
     * Returns a child namespace of the given name and separator.
     * The returned namespace will have this namespace as its parent.
     *
     * @param key
     *          The unlocalized name of the child namespace, to be used as a key in the cache.
     * @param name
     *          The name of the child namespace, or {@code null} if same than key.
     * @param headSeparator
     *          The separator to insert between the namespace and the
     *          {@linkplain AbstractName#head() head} of any name in that namespace.
     * @param separator
     *          The separator to insert between the {@linkplain AbstractName#getParsedNames()
     *          parsed names} of any name in that namespace.
     * @return The child namespace. It may be an existing instance.
     */
    private DefaultNameSpace child(final String key, CharSequence name,
            final String headSeparator, final String separator)
    {
        ensureNonNull("key", key);
        if (name == null) {
            name = key;
        }
        final WeakValueHashMap<String,Object> childs = this.childs;     // Paranoiac protection against accidental changes.
        DefaultNameSpace child;
        synchronized (childs) {
            final Object existing = childs.get(key);
            if (existing instanceof DefaultNameSpace) {
                child = (DefaultNameSpace) existing;
                if (!child.separator    .equals(separator) ||
                    !child.headSeparator.equals(headSeparator) ||
                    !child.name         .equals(name))                  // Same test than equalsIgnoreParent.
                {
                    child = new DefaultNameSpace(this, name, headSeparator, separator);
                    /*
                     * Do not cache that instance. Actually we can't guess if that instance
                     * would be more appropriate for caching purpose than the old one. We
                     * just assume that keeping the oldest one is more conservative.
                     */
                }
            } else {
                child = new DefaultNameSpace(this, name, headSeparator, separator);
                if (childs.put(key, child) != existing) {
                    throw new AssertionError();                         // Paranoiac check.
                }
            }
        }
        assert child.parent() == this;
        return child;
    }

    /**
     * Returns a name which is local in this namespace. The returned name will have this
     * namespace as its {@linkplain DefaultLocalName#scope() scope}. This method may returns
     * an existing instance on a "best effort" basis, but this is not guaranteed.
     *
     * @param  name      The name of the instance to create.
     * @param  candidate The instance to cache if no instance was found for the given name,
     *                   or {@code null} if none.
     * @return A name which is local in this namespace.
     */
    final DefaultLocalName local(final CharSequence name, final DefaultLocalName candidate) {
        ensureNonNull("name", name);
        final String key = name.toString();
        final WeakValueHashMap<String,Object> childs = this.childs;     // Paranoiac protection against accidental changes.
        DefaultLocalName child;
        synchronized (childs) {
            final Object existing = childs.get(key);
            if (existing instanceof DefaultLocalName) {
                child = (DefaultLocalName) existing;
                if (name.equals(child.name)) {
                    assert (child.scope != null ? child.scope : GlobalNameSpace.GLOBAL) == this;
                    return child;
                }
            }
            if (candidate != null) {
                child = candidate;
            } else {
                child = new DefaultLocalName(this, name);
            }
            // Cache only if the slot is not already occupied by a NameSpace.
            if (!(existing instanceof DefaultNameSpace)) {
                if (childs.put(key, child) != existing) {
                    throw new AssertionError();                         // Paranoiac check.
                }
            }
        }
        return child;
    }

    /**
     * Returns a JCR-like lexical form representation of this namespace.
     * Following the <cite>Java Content Repository</cite> (JCR) convention,
     * this method returns the string representation of {@linkplain #name()} between curly brackets.
     *
     * <div class="note"><b>Example:</b> if the name of this namespace is “<code>org.apache.sis</code>”,
     * then this method returns “<code>{org.apache.sis}</code>”.</div>
     *
     * <div class="section">Usage</div>
     * With this convention, it would be possible to create an <cite>expanded form</cite> of a generic name
     * (except for escaping of illegal characters) with a simple concatenation as in the following code example:
     *
     * {@preformat java
     *     GenericName name = ...; // A name
     *     println("Expanded form = " + name.scope() + name);
     * }
     *
     * However the convention followed by this {@code DefaultNameSpace} implementation is not specified in the
     * {@link NameSpace} contract. This implementation follows the JCR convention for debugging convenience,
     * but applications needing better guarantees should use {@link Names#toExpandedString(GenericName)} instead.
     *
     * @return A JCR-like lexical form of this namespace.
     *
     * @see Names#toExpandedString(GenericName)
     */
    @Debug
    @Override
    public String toString() {
        return new StringBuilder(name.length() + 2).append('{').append(name).append('}').toString();
    }

    /**
     * Returns {@code true} if this namespace is equal to the given object.
     *
     * @param object The object to compare with this namespace.
     * @return {@code true} if the given object is equal to this namespace.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            final DefaultNameSpace that = (DefaultNameSpace) object;
            return equalsIgnoreParent(that) && Objects.equals(this.parent, that.parent);
        }
        return false;
    }

    /**
     * Returns {@code true} if the namespace is equal to the given one, ignoring the parent.
     *
     * @param that The namespace to compare with this one.
     * @return {@code true} if both namespaces are equal, ignoring the parent.
     */
    private boolean equalsIgnoreParent(final DefaultNameSpace that) {
        return Objects.equals(this.headSeparator, that.headSeparator) &&
               Objects.equals(this.separator,     that.separator) &&
               Objects.equals(this.name,          that.name);               // Most expensive test last.
    }

    /**
     * Returns a hash code value for this namespace.
     */
    @Override
    public int hashCode() {
        return Objects.hash(parent, name, separator);
    }

    /**
     * If an instance already exists for the deserialized namespace, returns that instance.
     * Otherwise completes the initialization of the deserialized instance.
     *
     * <p>Because of its package-private access, this method is <strong>not</strong> invoked if
     * the deserialized class is a subclass defined in an other package. This is the intended
     * behavior since we don't want to replace an instance of a user-defined class.</p>
     *
     * @return The unique instance.
     */
    Object readResolve() {
        final DefaultNameSpace p = parent();
        final String key = key(name);
        final WeakValueHashMap<String,Object> pool = p.childs;
        synchronized (pool) {
            final Object existing = pool.get(key);
            if (existing instanceof DefaultNameSpace) {
                if (equalsIgnoreParent((DefaultNameSpace) existing)) {
                    return existing;
                } else {
                    // Exit from the synchronized block.
                }
            } else {
                init();
                if (pool.put(key, this) != existing) {
                    throw new AssertionError();             // Paranoiac check.
                }
                return this;
            }
        }
        init();
        return this;
    }
}
