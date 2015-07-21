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
import java.util.Iterator;
import java.util.ConcurrentModificationException;
import javax.xml.bind.annotation.XmlTransient;
import org.opengis.util.NameSpace;
import org.opengis.util.LocalName;
import org.opengis.util.ScopedName;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.UnmodifiableArrayList;


/**
 * A composite of a {@linkplain DefaultNameSpace name space} (as a {@linkplain DefaultLocalName local name})
 * and a {@linkplain AbstractName generic name} valid in that name space.
 * See the {@linkplain ScopedName GeoAPI javadoc} for more information.
 *
 * <p>{@code DefaultScopedName} can be instantiated by any of the following methods:</p>
 * <ul>
 *   <li>{@link DefaultNameFactory#createGenericName(NameSpace, CharSequence[])} with an array of length 2 or more.</li>
 *   <li>{@link DefaultNameFactory#parseGenericName(NameSpace, CharSequence)} with at least one occurrence of the separator in the path.</li>
 *   <li>Similar static convenience methods in {@link Names}.</li>
 * </ul>
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus inherently thread-safe if the {@link NameSpace} and all {@link CharSequence}
 * elements in the arguments given to the constructor are also immutable. Subclasses shall make sure that any
 * overridden methods remain safe to call from multiple threads and do not change any public {@code LocalName}
 * state.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see DefaultNameSpace
 * @see DefaultLocalName
 */

/*
 * JAXB annotation would be @XmlType(name ="CodeType"), but this can not be used here
 * since "CodeType" is used for various classes (including GenericName and LocalName).
 * (Un)marhalling of this class needs to be handled by a JAXB adapter.
 */
@XmlTransient
public class DefaultScopedName extends AbstractName implements ScopedName {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1363103337249930577L;

    /**
     * The immutable list of parsed names.
     */
    private final UnmodifiableArrayList<? extends LocalName> parsedNames;

    /**
     * The tail or path, computed when first needed.
     */
    private transient GenericName tail, path;

    /**
     * Creates a new scoped names from the given list of local names. This constructor is
     * not public because we do not check if the given local names have the proper scope.
     *
     * @param names The names to gives to the new scoped name.
     */
    static AbstractName create(final UnmodifiableArrayList<? extends DefaultLocalName> names) {
        ArgumentChecks.ensureNonNull("names", names);
        switch (names.size()) {
            default: return new DefaultScopedName(names);
            case 1:  return names.get(0);
            case 0:  throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "names"));
        }
    }

    /**
     * Creates a new scoped names from the given list of local names. This constructor is
     * not public because it does not check if the given local names have the proper scope.
     *
     * @param names The names to gives to the new scoped name.
     */
    private DefaultScopedName(final UnmodifiableArrayList<? extends LocalName> names) {
        parsedNames = names;
    }

    /**
     * Constructs a scoped name from the specified list of strings.
     * If any of the given names is an instance of {@link InternationalString}, then its
     * {@link InternationalString#toString(java.util.Locale) toString(Locale.ROOT)}
     * method will be invoked for fetching an unlocalized name.
     * Otherwise the {@link CharSequence#toString()} method will be used.
     *
     * @param scope The scope of this name, or {@code null} for the global scope.
     * @param names The local names. This list must have at least two elements.
     */
    protected DefaultScopedName(final NameSpace scope, final List<? extends CharSequence> names) {
        ArgumentChecks.ensureNonNull("names", names);
        final int size = names.size();
        ArgumentChecks.ensureSizeBetween("names", 2, Integer.MAX_VALUE, size);
        DefaultNameSpace ns = DefaultNameSpace.castOrCopy(scope);
        final boolean global = ns.isGlobal();
        int i = 0;
        final LocalName[] locals = new LocalName[size];
        final Iterator<? extends CharSequence> it = names.iterator();
        /*
         * Builds the parsed name list by creating DefaultLocalName instances now.
         * Note that we expect at least 2 valid entries (because of the check we
         * did before), so we don't check hasNext() for the two first entries.
         */
        CharSequence name = it.next();
        do {
            ArgumentChecks.ensureNonNullElement("names", i, name);
            locals[i++] = new DefaultLocalName(ns, name);
            ns = ns.child(name);
            name = it.next();
        } while (it.hasNext());
        /*
         * At this point, we have almost finished to build the parsed names array.
         * The last name is the tip, which we want to live in the given namespace.
         * If this namespace is global, then the fully qualified name is this name.
         * In this case we assign the reference now in order to avoid letting
         * tip.toFullyQualifiedName() creates a new object later.
         */
        final DefaultLocalName tip = ns.local(name, null);
        if (global) {
            tip.fullyQualified = fullyQualified = this;
        }
        locals[i++] = tip;
        if (i != size) { // Paranoiac check.
            throw new ConcurrentModificationException(Errors.format(Errors.Keys.UnexpectedChange_1, "names"));
        }
        // Following line is safe because 'parsedNames' type is <? extends LocalName>.
        parsedNames = UnmodifiableArrayList.wrap(locals);
    }

    /**
     * Constructs a scoped name as the concatenation of the given generic names.
     * The scope of the new name will be the scope of the {@code path} argument.
     *
     * @param path The first part to concatenate.
     * @param tail The second part to concatenate.
     */
    @SuppressWarnings("SuspiciousToArrayCall")
    protected DefaultScopedName(final GenericName path, final GenericName tail) {
        ArgumentChecks.ensureNonNull("path", path);
        ArgumentChecks.ensureNonNull("tail", tail);
        final List<? extends LocalName> parsedPath = path.getParsedNames();
        final List<? extends LocalName> parsedTail = tail.getParsedNames();
        int index = parsedPath.size();
        LocalName[] locals = new LocalName[index + parsedTail.size()];
        locals = parsedPath.toArray(locals);
        /*
         * We have copied the LocalNames from the path unconditionally.  Now we need to process the
         * LocalNames from the tail. If the tail scope follows the path scope, we can just copy the
         * names without further processing (easy case). Otherwise we need to create new instances.
         *
         * Note that by contract, GenericName shall contain at least 1 element. This assumption
         * appears in two places: it.next() invoked once before any it.hasNext(), and testing for
         * locals[index-1] element (so we assume index > 0).
         */
        final Iterator<? extends LocalName> it = parsedTail.iterator();
        LocalName name = it.next();
        final LocalName lastName  = locals[index-1];
        final NameSpace lastScope = lastName.scope();
        final NameSpace tailScope = name.scope();
        if (tailScope instanceof DefaultNameSpace && ((DefaultNameSpace) tailScope).parent() == lastScope) {
            /*
             * If the tail is actually the tip (a LocalName), remember the tail so we
             * don't need to create it again later. Then copy the tail after the path.
             */
            if (path instanceof LocalName) {
                this.tail = tail;
            }
            while (true) {
                locals[index++] = name;
                if (!it.hasNext()) break;
                name = it.next();
            }
        } else {
            /*
             * There is no continuity in the chain of scopes, so we need to create new
             * LocalName instances.
             */
            DefaultNameSpace scope = DefaultNameSpace.castOrCopy(lastScope);
            CharSequence label = name(lastName);
            while (true) {
                scope = scope.child(label);
                label = name(name);
                name  = new DefaultLocalName(scope, label);
                locals[index++] = name;
                if (!it.hasNext()) break;
                name = it.next();
            }
        }
        if (index != locals.length) { // Paranoiac check.
            throw new ConcurrentModificationException(Errors.format(Errors.Keys.UnexpectedChange_1, "tail"));
        }
        // Following line is safe because 'parsedNames' type is <? extends LocalName>.
        parsedNames = UnmodifiableArrayList.wrap(locals);
        if (tail instanceof LocalName) {
            this.path = path;
        }
    }

    /**
     * Returns the name to be given to {@link DefaultLocalName} constructors.
     */
    private static CharSequence name(final GenericName name) {
        if (name instanceof DefaultLocalName) {
            return ((DefaultLocalName) name).name;
        }
        final InternationalString label = name.toInternationalString();
        return (label != null) ? label : name.toString();
    }

    /**
     * Returns the size of the backing array. This is used only has a hint for optimizations
     * in attempts to share internal arrays.
     */
    @Override
    final int arraySize() {
        return parsedNames.arraySize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NameSpace scope() {
        return head().scope();
    }

    /**
     * Returns every elements in the sequence of {@linkplain #getParsedNames() parsed names}
     * except for the {@linkplain #head() head}.
     *
     * @return All elements except the first one in the in the list of {@linkplain #getParsedNames() parsed names}.
     */
    @Override
    public synchronized GenericName tail() {
        if (tail == null) {
            final int size = parsedNames.size();
            switch (size) {
                default: tail = new DefaultScopedName(parsedNames.subList(1, size)); break;
                case 2:  tail = parsedNames.get(1); break;
                case 1:  // fall through
                case 0:  throw new AssertionError(size);
            }
        }
        return tail;
    }

    /**
     * Returns every element in the sequence of {@linkplain #getParsedNames() parsed names}
     * except for the {@linkplain #tip() tip}.
     *
     * @return All elements except the last one in the in the list of {@linkplain #getParsedNames() parsed names}.
     */
    @Override
    public synchronized GenericName path() {
        if (path == null) {
            final int size = parsedNames.size();
            switch (size) {
                default: path = new DefaultScopedName(parsedNames.subList(0, size-1)); break;
                case 2:  path = parsedNames.get(0); break;
                case 1:  // fall through
                case 0:  throw new AssertionError(size);
            }
        }
        return path;
    }

    /**
     * Returns the sequence of local name for this generic name.
     */
    @Override
    public List<? extends LocalName> getParsedNames() {
        return parsedNames;
    }
}
