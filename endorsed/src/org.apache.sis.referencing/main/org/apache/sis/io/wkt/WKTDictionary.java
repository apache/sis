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
package org.apache.sis.io.wkt;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.function.Predicate;
import java.util.function.Consumer;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.io.LineNumberReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.ParsePosition;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.factory.FactoryDataException;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.shared.ReferencingUtilities;
import org.apache.sis.referencing.internal.shared.WKTKeywords;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.collection.FrequencySortedSet;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.Identifier;


/**
 * A factory providing CRS objects parsed from WKT definitions associated to authority codes.
 * Each WKT definition is associated to a key according the <var>authority:version:code</var>
 * pattern where <var>code</var> is mandatory and <var>authority:version</var> are optional.
 * Coordinate Reference Systems or other kinds of objects are created from WKT definitions
 * when a {@code create(…)} method is invoked for the first time for a given key.
 *
 * <h2>Sub-classing and instantiation</h2>
 * {@linkplain #WKTDictionary(Citation) Newly constructed} {@code WKTDictionary} are initially empty.
 * The dictionary can be populated in the following ways:
 *
 * <ul>
 *   <li>Invoke {@link #load(BufferedReader)} for reading definitions from file(s).</li>
 *   <li>Invoke {@link #addDefinitions(Stream)} for providing definitions from an arbitrary source.</li>
 *   <li>Override {@link #fetchDefinition(DefaultIdentifier)} in a subclass for fetching WKT definitions
 *       on-the-fly (for example from the {@code "spatial_ref_sys"} table of a spatial database.</li>
 * </ul>
 *
 * Sub-classing may be necessary even if {@code fetchDefinition(…)} is not overridden
 * because {@code WKTDictionary} does not implement any of the
 * {@link org.opengis.referencing.crs.CRSAuthorityFactory},
 * {@link org.opengis.referencing.cs.CSAuthorityFactory} or
 * {@link org.opengis.referencing.datum.DatumAuthorityFactory}.
 * The choice of interfaces to implement is left to subclasses.
 *
 * <h3>Example</h3>
 * Extend the set of Coordinate Reference Systems recognized
 * by {@link org.apache.sis.referencing.CRS#forCode(String)}.
 * The additional CRS are defined by Well-Known Text strings in a {@code "MyCRS.txt"} file.
 * First step is to create a CRS factory with those definitions:
 *
 * {@snippet lang="java" :
 *     public final class MyCRS extends WKTDictionary implements CRSAuthorityFactory {
 *         MyCRS() throws IOException, FactoryException {
 *             super(new DefaultCitation("MyAuthority"));
 *             try (BufferedReader source = Files.newBufferedReader(Path.of("MyCRS.txt"))) {
 *                 load(source);
 *             }
 *         }
 *     }
 *     }
 *
 * The second step is to declare this factory in the {@code module-info.java} file
 * as a provider of the {@code org.opengis.referencing.crs.CRSAuthorityFactory} service.
 * That file shall contain the class name of above {@code MyCRS} class.
 *
 * <h2>Errors management</h2>
 * Well-Known Text parsing is performed in two steps, each of them executed at a different time:
 *
 * <h3>Early validation</h3>
 * WKT strings added by {@code load(…)} or {@code addDefinitions(…)} methods are verified
 * for matching quotes, balanced parenthesis or brackets, and valid number or date formats.
 * If a syntax error is detected, the loading process is interrupted at the point the error occurred;
 * CRS definitions after the error location are not loaded.
 * However, WKT keywords and geodetic parameters (e.g. map projections) are not validated at this stage.
 *
 * <h3>Late validation</h3>
 * WKT keywords and geodetic parameters inside WKT elements are validated only when {@link #createObject(String)}
 * is invoked. If an error occurs at this stage, only the CRS (or other geodetic object) for the code given to
 * the {@code createFoo(…)} method become invalid. Objects associated to other codes are not impacted.
 *
 * <h2>Multi-threading</h2>
 * This class is thread-safe but not necessarily concurrent.
 * This class is designed for a relatively small number of WKT;
 * it is not a replacement for database-backed factory such as
 * {@link org.apache.sis.referencing.factory.sql.EPSGFactory}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 * @since   1.1
 */
public class WKTDictionary extends GeodeticAuthorityFactory {
    /**
     * The organization or specification that defines the codes recognized by this factory.
     * May be {@code null} if not yet determined.
     *
     * @see #updateAuthority()
     * @see #getAuthority()
     */
    private volatile Citation authority;

    /**
     * Authorities declared in all {@code "ID[CITATION[…]]"} elements found in WKT definitions.
     * This set is {@code null} if an {@link #authority} value has been explicitly specified at
     * construction time. If non-null, this is used for creating a default {@link #authority}.
     */
    private final Set<String> authorities;

    /**
     * Code spaces of authority codes recognized by this factory.
     * This set is computed from the {@code "ID[…]"} elements found in WKT definitions.
     * Code spaces are sorted with most frequently used space first.
     *
     * @see #getCodeSpaces()
     */
    private final FrequencySortedSet<String> codespaces;

    /**
     * Cache of authority codes computed by {@link #getAuthorityCodes(Class)}.
     * This cache can be cleared at any time; values are recomputed when needed.
     */
    private final Map<Class<?>, Set<String>> codeCaches;

    /**
     * The parser to use for creating geodetic objects from WKT definitions.
     * Subclasses can modify the {@code WKTFormat} configuration in their constructor,
     * but should not use it directly after construction (for thread safety reasons).
     */
    protected final WKTFormat parser;

    /**
     * The write lock for {@link #parser} and the read/write locks for {@link #definitions} accesses.
     * All {@link #parser} usages after {@code WKTDictionary} construction shall be synchronized by
     * the {@link ReadWriteLock#writeLock()}.
     *
     * <h4>Implementation note</h4>
     * We manage the locks ourselves instead of using a {@link java.util.concurrent.ConcurrentHashMap}
     * because if a {@link #definitions} value needs to be computed, then we need to block all other
     * threads anyway since {@link #parser} is not thread-safe. Consequently, the high concurrency
     * capability provided by {@code ConcurrentHashMap} does not help us in this case.
     */
    private final ReadWriteLock lock;

    /**
     * CRS definitions associated to <var>authority:version:code</var> keys.
     * Keys are authority codes, ignoring code space (authority) and version.
     * For example, in "EPSG:9.1:4326" the key would be only "4326".
     * Values can be one of the following 4 types:
     *
     * <ol>
     *   <li>{@link StoredTree}: this is the initial state when there are no duplicated codes.
     *       This is the root of a tree of WKT keywords with their values as children.
     *       A tree can be parsed later as an {@link IdentifiedObject} when first requested.</li>
     *   <li>{@link IdentifiedObject}: the result of parsing the {@link StoredTree}
     *       when {@link #createObject(String)} is invoked for a given authority code.
     *       The parsing result replaces the previous {@link StoredTree} value.</li>
     *   <li>{@link Disambiguation}: if the same code is used by two or more authorities or versions,
     *       then above-cited {@link StoredTree} or {@link IdentifiedObject} alternatives are wrapped
     *       in a {@link Disambiguation} object.</li>
     *   <li>{@link String} if parsing failed, in which case the string is the error message.</li>
     * </ol>
     *
     * <h4>Synchronization</h4>
     * All read operations in this map shall be synchronized by the <code>{@linkplain #lock}.readLock()</code>
     * and write operations synchronized by the <code>{@linkplain #lock}.writeLock()</code>.
     *
     * @see #addDefinition(StoredTree)
     * @see #createObject(String)
     */
    private final Map<String,Object> definitions;

    /**
     * A special kind of value used in the {@link #definitions} map when the same code is used by more
     * than one authority and version. In the common case where a {@link WKTDictionary} instance
     * contains definitions for only one namespace and version, this class will never be instantiated.
     */
    private static final class Disambiguation {
        /**
         * The previous {@code Disambiguation} in a linked list, or {@code null} if we reached the end of list.
         * The use of a linked list should be efficient enough if the number of {@code Disambiguation}s for a
         * given code is small.
         */
        private final Disambiguation previous;

        /**
         * The authority (or other kind of code space) providing CRS definitions.
         */
        private final String codespace;

        /**
         * Version of the CRS definition, or {@code null} if unspecified.
         */
        private final String version;

        /**
         * The value as an {@link StoredTree} before parsing or an {@link IdentifiedObject} after parsing.
         * They are the kind of types documented in {@link WKTDictionary#definitions}, excluding other
         * {@code Disambiguation} instances.
         */
        Object value;

        /**
         * Creates a new {@code Disambiguation} instance as a wrapper around the given identifier object.
         * This constructor may be invoked if {@link WKTDictionary} has been used for creating some
         * objects before new definitions are added. It should rarely happen.
         *
         * @param  object  the CRS (or other geodetic object) to wrap.
         */
        private Disambiguation(final IdentifiedObject object) {
            /*
             * Identifier should never be null because `WKTDictionary` accepts only definitions having
             * an `ID[…]` or `AUTHORITY[…]` element. A WKT can contain at most one of those elements.
             */
            final Identifier id = Containers.peekFirst(object.getIdentifiers());
            codespace = id.getCodeSpace();
            version   = id.getVersion();
            value     = object;
            previous  = null;
        }

        /**
         * Creates a new {@code Disambiguation} instance as a wrapper around the given identifier object.
         *
         * @param  object  definition in WKT of the CRS (or other geodetic object) to wrap.
         */
        private Disambiguation(final StoredTree object) {
            final Object[] fullId = new Object[3];
            object.peekIdentifiers(fullId);
            codespace = trimOrNull(fullId[0]);
            version   = trimOrNull(fullId[2]);
            value     = object;
            previous  = null;
        }

        /**
         * Creates a new {@code Disambiguation} instance identified by {@code codespace:version:code}.
         *
         * @param  codespace  the authority (or other kind of code space) providing CRS definitions.
         * @param  version    version of the CRS definition, or {@code null} if unspecified.
         * @param  code       code allocated by the authority for the CRS definition.
         * @param  oldValue   previous value for the same code, or {@code null} if none.
         * @param  newValue   the CRS (or other geodetic object) definition.
         * @throws IllegalArgumentException if <var>authority:version:code</var> identifier is already used.
         *
         * @see WKTDictionary#addDefinition(StoredTree)
         */
        Disambiguation(final String codespace, final String version, final String code,
                       final Object oldValue, final Object newValue)
        {
            this.codespace = codespace;
            this.version   = version;
            this.value     = newValue;
            if (oldValue instanceof Disambiguation) {
                previous = (Disambiguation) oldValue;
            } else if (oldValue instanceof StoredTree) {
                previous = new Disambiguation((StoredTree) oldValue);
            } else if (oldValue instanceof IdentifiedObject) {
                previous = new Disambiguation((IdentifiedObject) oldValue);
            } else {
                previous = null;            // Discard previous parsing failure (a `String` instance).
                return;
            }
            Disambiguation check = previous;
            do {
                if (Strings.equalsIgnoreCase(codespace, check.codespace) &&
                    Strings.equalsIgnoreCase(version,   check.version))
                {
                    throw new IllegalArgumentException(Errors.format(
                            Errors.Keys.DuplicatedIdentifier_1, identifier(code)));
                }
                check = check.previous;
            } while (check != null);
        }

        /**
         * Finds the {@code Disambiguation} for the given authority and version.
         *
         * @param  choices    end of a linked list of {@code Disambiguation}s, or {@code null} if none.
         * @param  codespace  the authority providing CRS definitions, or {@code null} if unspecified.
         * @param  version    version of the CRS definition, or {@code null} if unspecified.
         * @param  code       code allocated by the authority for the CRS definition.
         * @return container for the given authority and version, or {@code null} if none.
         * @throws NoSuchAuthorityCodeException if the given authority and version are ambiguous.
         */
        static Disambiguation find(Disambiguation choices, final String codespace, final String version, final String code)
                throws NoSuchAuthorityCodeException
        {
            Disambiguation found = null;
            for (boolean isExact = false; choices != null; choices = choices.previous) {
                if (codespace == null || codespace.equalsIgnoreCase(choices.codespace)) {
                    if (Strings.equalsIgnoreCase(version, choices.version)) {
                        if (!isExact) {
                            isExact = true;
                            found = choices;        // Silently discard previous value since we have a better match.
                            continue;
                        }
                    } else if (isExact) {
                        continue;                   // Ignore this value since previous one was a better match.
                    }
                    if (isExact && found != null) {
                        final String identifier = identifier(codespace, version, code);
                        throw new NoSuchAuthorityCodeException(Errors.format(Errors.Keys.AmbiguousName_3,
                                choices.identifier(code), found.identifier(code), identifier),
                                codespace, code, identifier);
                    }
                    found = choices;
                }
            }
            return found;
        }

        /**
         * Adds all authority codes to the given set.
         *
         * @param  choices  end of a linked list of {@code Disambiguation}s.
         * @param  code     authority code (code space and version may vary).
         * @param  filter   filter to apply of elements to add in the set.
         * @param  addTo    where to add the {@code codespace:version:code} tuples.
         *
         * @see WKTDictionary#getAuthorityCodes(Class)
         */
        static void list(Disambiguation choices, final String code, final Predicate<Object> filter, final Set<String> addTo) {
            do {
                if (filter.test(choices.value)) {
                    addTo.add(choices.identifier(code));
                }
                choices = choices.previous;
            } while (choices != null);
        }

        /**
         * Creates an <var>authority:version:code</var> identifier with the given code.
         * This is used for formatting error messages.
         */
        private String identifier(final String code) {
            return identifier(codespace, version, code);
        }

        /**
         * Creates an <var>authority:version:code</var> identifier with the given code.
         * This is used for formatting error messages.
         */
        private static String identifier(final String codespace, final String version, final String code) {
            return Strings.orEmpty(codespace) + Constants.DEFAULT_SEPARATOR +
                   Strings.orEmpty(version)   + Constants.DEFAULT_SEPARATOR +
                   Strings.orEmpty(code);
        }
    }

    /**
     * Creates an initially empty factory. The authority can specified explicitly or inferred from the WKTs.
     * In the latter case (when the given authority is {@code null}), an authority will be inferred from all
     * {@code ID[…]} or {@code AUTHORITY[…]} elements found in WKT strings as below, in preference order:
     *
     * <ol>
     *   <li>Most frequent {@code CITATION[…]} value.</li>
     *   <li>If there is no citation, then most frequent code space
     *       in {@code ID[…]} or {@code AUTHORITY[…]} elements.</li>
     * </ol>
     *
     * The WKT strings are specified by calls to {@link #load(BufferedReader)} or {@link #addDefinitions(Stream)}
     * after construction.
     *
     * @param  authority  organization that defines the codes recognized by this factory, or {@code null}.
     */
    public WKTDictionary(final Citation authority) {
        /*
         * Note: we do not allow users to specify their own `WKTFormat` instance because current
         * `WKTDictionary` implementation invokes package-private methods. If user supplies
         * a `WKTFormat` with overridden public methods, (s)he may be surprised to see that those
         * methods are not invoked.
         */
        definitions = new HashMap<>();
        codeCaches  = new HashMap<>();
        codespaces  = new FrequencySortedSet<>(true);
        parser      = new WKTFormat();
        lock        = new ReentrantReadWriteLock();
        authorities = (authority != null) ? null : new FrequencySortedSet<>(true);
        this.authority = authority;
    }

    /**
     * If {@link #authority} is not yet defined, computes a value from {@code ID[…]} found
     * in all WKT strings. This method should be invoked after new WKTs have been added.
     */
    private void updateAuthority() {
        codeCaches.clear();
        if (authorities != null) {
            String name = Containers.peekFirst(authorities);        // Most frequently declared authority.
            if (name == null) {
                name = Containers.peekFirst(codespaces);            // Most frequently declared codespace.
            }
            authority = Citations.fromName(name);                   // May still be null.
        }
    }

    /**
     * Adds to this factory all definitions read from the given source.
     * Each Coordinate Reference System (or other geodetic object) is defined by a string in WKT format.
     * The key associated to each object is given by the {@code ID[…]} or {@code AUTHORITY[…]} element,
     * which is typically the last element of a WKT string and is mandatory for definitions in this file.
     *
     * <p>WKT strings can span many lines. All lines after the first line shall be indented with at least
     * one white space. Non-indented lines start new definitions.</p>
     *
     * <p>Blank lines and lines starting with the {@code #} character (ignoring white spaces) are ignored.</p>
     *
     * <h4>Aliases for WKT fragments</h4>
     * Files with more than one WKT definition tend to repeat the same WKT fragments many times.
     * For example, the same {@code BaseGeogCRS[…]} element may be repeated in every {@code ProjectedCRS} definitions.
     * Redundant fragments can be replaced by aliases for making the file more compact,
     * easier to read, faster to parse and with smaller memory footprint.
     *
     * <p>Each line starting with "<code>SET &lt;<var>identifier</var>&gt;=&lt;<var>WKT</var>&gt;</code>"
     * defines an alias for a fragment of WKT string. The WKT can span many lines as described above.
     * Aliases are local to the file where they are defined.
     * Aliases can be expanded in other WKT strings by "<code>$&lt;<var>identifier</var>&gt;</code>".</p>
     *
     * <h4>Validation</h4>
     * This method verifies that definitions have matching quotes, balanced parenthesis or brackets,
     * and valid number or date formats. It does not verify WKT keywords or geodetic parameters.
     * See class javadoc for more details.
     *
     * <h4>Example</h4>
     * An example is <a href="./doc-files/ESRI.txt">available here</a>.
     *
     * @param  source  the source of WKT definitions.
     * @throws FactoryException if the definition file cannot be read.
     */
    public void load(final BufferedReader source) throws FactoryException {
        Objects.requireNonNull(source);
        lock.writeLock().lock();
        try {
            final Loader loader = new Loader(source);
            try {
                loader.read();
            } catch (IOException e) {
                throw new FactoryException(loader.canNotRead(null, e), e);
            } catch (ParseException | IllegalArgumentException e) {
                throw new FactoryDataException(loader.canNotRead(null, e), e);
            } finally {
                loader.restore();
                updateAuthority();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Implementation of {@link WKTDictionary#load(BufferedReader)} method.
     * Caller must own the write lock before to instantiate and use this class.
     */
    private final class Loader {
        /**
         * Keyword recognized by {@link WKTDictionary#load(BufferedReader)}.
         */
        private static final String SET = "SET";

        /** The source of WKT definitions. */
        private final BufferedReader source;

        /** Temporary buffer where to put the WKT to parse. */
        private final StringBuilder buffer;

        /** If the WKT being parsed is an alias, the alias key. Otherwise {@code null}. */
        private String aliasKey;

        /** Argument for {@link #addAliasOrDefinition()}. */
        private final ParsePosition pos;

        /** Zero-based number of current line. Equivalent to {@link LineNumberReader#getLineNumber()}. */
        private int lineNumber;

        /** Aliases that existed before in {@link #parser} before loading started. */
        private final Set<String> aliases;

        /** Creates a new loader. */
        Loader(final BufferedReader source) {
            this.source = source;
            buffer  = new StringBuilder(500);
            pos     = new ParsePosition(0);
            aliases = new HashSet<>(parser.getFragmentNames());
        }

        /**
         * Restores {@link #parser} to its initial state. This method should be invoked
         * in a finally block regardless if the parsing succeeded or failed.
         */
        final void restore() {
            parser.getFragmentNames().retainAll(aliases);
            parser.clear();
        }

        /**
         * Returns an error message saying "Cannot read WKT at line X". The message is followed
         * by a "Caused by" phrase specified either as a string or an exception. At least one of
         * {@code cause} and {@code e} shall be non-null.
         */
        final String canNotRead(String cause, final Exception e) {
            final Locale locale = parser.getErrorLocale();
            if (cause == null) {
                cause = Exceptions.getLocalizedMessage(e, locale);
            }
            return Resources.forLocale(locale).getString(Resources.Keys.CanNotParseWKT_2, getLineNumber(), cause);
        }

        /**
         * Returns the one-based line number of the last line read.
         * Actually this method returns the zero-based line number of current position,
         * but since current position is after the last line read, this is equivalent
         * to line number of last line read + 1.
         *
         * @return one-based line number of current position.
         */
        private int getLineNumber() {
            if (source instanceof LineNumberReader) {
                // In case an unusual implementation counts lines in a different way than we do.
                lineNumber = ((LineNumberReader) source).getLineNumber();
            }
            return lineNumber;
        }

        /**
         * Adds to the enclosing factory all definitions read from the given source.
         * See {@link WKTDictionary#load(BufferedReader)} for a format description.
         *
         * @throws IOException if an error occurred while reading lines.
         * @throws ParseException if an error occurred while parsing a WKT.
         * @throws FactoryDataException if the file has a syntax error.
         * @throws IllegalArgumentException if a {@code codespace:version:code} tuple or an alias is assigned twice.
         */
        final void read() throws IOException, ParseException, FactoryDataException {
            final String lineSeparator = System.lineSeparator();
            int indentation = 0;
            String line;
            while ((line = source.readLine()) != null) {
                lineNumber++;
                final int length = line.length();
                int defStart = CharSequences.skipLeadingWhitespaces(line, 0, length);
                if (defStart < length && line.charAt(defStart) == '#') continue;        // Skip comment lines.
                /*
                 * If the line is indented compared to the first line, we presume that it is the continuation
                 * of previous line and skip the check for "SET" keyword. If the line is not indented,
                 * previous buffer content need to be parsed before we start a new WKT definition.
                 */
                if (defStart > indentation) {
                    defStart = indentation;
                } else {
                    addAliasOrDefinition();
                    indentation = defStart;
                    if (line.regionMatches(true, defStart, SET, 0, SET.length())) {
                        final int keyStart = CharSequences.skipLeadingWhitespaces(line, defStart + SET.length(), length);
                        if (keyStart > defStart) {             // `true` if "SET" is followed by at least one white space.
                            defStart = line.indexOf('=', keyStart);
                            if (defStart <= keyStart) {
                                throw new FactoryDataException(resources().getString(
                                            Resources.Keys.SyntaxErrorForAlias_1, getLineNumber()));
                            }
                            final int keyEnd = CharSequences.skipTrailingWhitespaces(line, keyStart, defStart);
                            defStart = CharSequences.skipLeadingWhitespaces(line, defStart + 1, length);
                            final String key = line.substring(keyStart, keyEnd);
                            if (!CharSequences.isUnicodeIdentifier(key)) {
                                String c = parser.errors().getString(Errors.Keys.NotAUnicodeIdentifier_1, key);
                                throw new FactoryDataException(canNotRead(c, null));
                            }
                            aliasKey = key;
                        }
                    }
                }
                /*
                 * Copy non-empty lines in the buffer, omitting indentation and trailing spaces.
                 * The leading spaces after indentation are kept in order to have a more readable
                 * WKT string in error message if parsing fail.
                 */
                final int end = CharSequences.skipTrailingWhitespaces(line, defStart, length);
                if (defStart < end) {
                    if (buffer.length() != 0) buffer.append(lineSeparator);
                    buffer.append(line, defStart, end);
                }
            }
            addAliasOrDefinition();
            parser.logWarnings(WKTDictionary.class, "load");
        }

        /**
         * Parses the current {@link #buffer} content as a WKT elements (possibly with children elements).
         * This method does not build the full {@link IdentifiedObject}; this latter part will be done only
         * when first needed.
         *
         * <p>If {@link #aliasKey} is non-null, the first WKT is taken as a {@linkplain WKTFormat#addFragment
         * fragment} associated to the given alias. All other WKT (if any) are taken as definitions of CRS or
         * other objects.</p>
         *
         * @throws ParseException if an error occurred while parsing the WKT string.
         * @throws FactoryDataException if there is unparsed text after the WKT.
         * @throws IllegalArgumentException if a {@code codespace:version:code} tuple or an alias is assigned twice.
         */
        private void addAliasOrDefinition() throws ParseException, FactoryDataException {
            if (buffer.length() != 0) {
                pos.setIndex(0);
                final String wkt = buffer.toString();
                final StoredTree tree = parser.textToTree(wkt, pos, aliasKey);
                final int end = pos.getIndex();
                if (end < wkt.length()) {           // Trailing white spaces already removed by `read(…)`.
                    throw new FactoryDataException(unexpectedText(getLineNumber(), wkt, end));
                }
                if (aliasKey != null) {
                    parser.addFragment(aliasKey, tree);
                    aliasKey = null;
                } else {
                    addDefinition(tree);
                }
                buffer.setLength(0);
            }
        }
    }

    /**
     * Adds the definition of a CRS (or other geodetic objects) from a tree of WKT elements.
     * The authority code is inferred from the {@code ID[…]} or {@code AUTHORITY[…]} element.
     * Caller must own the write lock before to invoke this method.
     * {@link #updateAuthority()} should be invoked after this method.
     *
     * @param  tree  a tree of WKT elements.
     * @throws IllegalArgumentException if a {@code codespace:version:code} tuple is assigned twice.
     * @throws FactoryDataException if the WKT does not have an {@code ID[…]} or {@code AUTHORITY[…]} element.
     *
     * @see #definitions
     */
    private void addDefinition(final StoredTree tree) throws FactoryDataException {
        final Object[] fullId = new Object[authorities == null ? 4 : 3];
        tree.peekIdentifiers(fullId);                   // Codespace, code, version, (authority).
        final String code = trimOrNull(fullId[1]);
        if (code == null) {
            throw new FactoryDataException(resources().getString(Resources.Keys.MissingAuthorityCode_1, tree));
        }
        final String codespace = trimOrNull(fullId[0]);
        definitions.merge(code, tree, (oldValue, newValue) -> {
            return new Disambiguation(codespace, trimOrNull(fullId[2]), code, oldValue, newValue);
        });
        codespaces.add(codespace);
        if (fullId.length >= 4) {
            final String title = trimOrNull(fullId[3]);
            if (title != null) {
                authorities.add(title);
            }
        }
    }

    /**
     * Adds definitions of CRS (or other geodetic objects) from Well-Known Texts. Blank strings are ignored.
     * Each non-blank {@link String} shall contain the complete definition of exactly one geodetic object.
     * A geodetic object cannot have its definition split in two or more {@link String}s.
     *
     * <p>The key associated to each object is given by the {@code ID[…]} or {@code AUTHORITY[…]} element,
     * which is typically the last element of a WKT string and is mandatory. WKT strings can contain line
     * separators for human readability.</p>
     *
     * @param  objects  CRS (or other geodetic objects) definitions as WKT strings.
     * @throws FactoryException if a WKT cannot be parsed, or does not contain an {@code ID[…]} or
     *         {@code AUTHORITY[…]} element, or if the same {@code codespace:version:code} tuple is
     *         used for two objects.
     */
    public void addDefinitions(final Stream<String> objects) throws FactoryException {
        /*
         * We work with iterator because we do not support parallelism yet.
         * However, a future version may support that, which is why argument
         * type is a `Stream`.
         */
        final Iterator<String> it = objects.iterator();
        final ParsePosition pos = new ParsePosition(0);
        lock.writeLock().lock();
        try {
            int lineNumber = 1;
            try {
                while (it.hasNext()) {
                    final String wkt = it.next();
                    final StoredTree tree = parser.textToTree(wkt, pos, null);
                    final int end = pos.getIndex();
                    if (end < CharSequences.skipTrailingWhitespaces(wkt, 0, wkt.length())) {
                        throw new FactoryDataException(unexpectedText(lineNumber, wkt, end));
                    }
                    addDefinition(tree);
                    pos.setIndex(0);
                    lineNumber++;
                }
                parser.logWarnings(WKTDictionary.class, "addDefinitions");
            } catch (ParseException | IllegalArgumentException e) {
                throw new FactoryDataException(resources().getString(
                        Resources.Keys.CanNotParseWKT_2, lineNumber, e.getLocalizedMessage()));
            } finally {
                parser.clear();
                updateAuthority();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Parses immediately the given WKT and caches the result under the given identifier. This method is invoked
     * only if subclass overrides {@link #fetchDefinition(DefaultIdentifier)} for producing WKT on-the-fly.
     *
     * @param  codespace          the authority (or other kind of code space) providing CRS definitions.
     * @param  version            version of the CRS definition, or {@code null} if unspecified.
     * @param  code               code allocated by the authority for the CRS definition.
     * @param  wkt                the Well-Known Text to parse immediately.
     * @param  defaultIdentifier  identifier to assign to the object if the WKT does not provide one.
     * @return the parsed object.
     * @throws FactoryException if parsing failed.
     */
    private IdentifiedObject parseAndAdd(final String codespace, final String version,
            final String code, final String wkt, final Identifier defaultIdentifier) throws FactoryException
    {
        ArgumentChecks.ensureNonEmpty("code", code);
        ArgumentChecks.ensureNonEmpty("wkt",  wkt);
        lock.writeLock().lock();
        try {
            try {
                parser.setDefaultIdentifier(defaultIdentifier);
                final Object object = parser.parseObject(wkt);
                if (!(object instanceof IdentifiedObject)) {
                    throw new FactoryDataException(parser.errors().getString(
                            Errors.Keys.UnexpectedTypeForReference_3, code, IdentifiedObject.class, object.getClass()));
                }
                final Disambiguation entry = (Disambiguation) definitions.compute(code, (key, oldValue) -> {
                    return new Disambiguation(codespace, version, code, oldValue, object);
                });
                codespaces.add(entry.codespace);
                return (IdentifiedObject) object;
            } catch (ParseException | IllegalArgumentException e) {
                throw new FactoryDataException(e.getLocalizedMessage());
            } finally {
                parser.setDefaultIdentifier(null);
                parser.clear();
                updateAuthority();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Fetches the Well-Known Text for a user-specified identifier not found in this {@code WKTDictionary}.
     * Subclasses can override this method if WKT strings are not {@linkplain #load(BufferedReader) loaded}
     * or {@linkplain #addDefinitions(Stream) specified} in advance, but instead fetched when first needed.
     * An example of such scenario is WKTs provided by the {@code "spatial_ref_sys"} table of a spatial database.
     * If no WKT is found for the given identifier, then this method returns {@code null}.
     *
     * <p>On input, {@code identifier} contains only the pieces of information provided by user. For example if user
     * invoked {@code createGeographicCRS("Foo")}, then the identifier {@linkplain DefaultIdentifier#getCode() code}
     * will be {@code "Foo"} but the {@linkplain DefaultIdentifier#getCodeSpace() codespace} and
     * {@linkplain DefaultIdentifier#getVersion() version} will be undefined ({@code null}).
     * On output, {@code identifier} should be completed with missing code space and version (if available).</p>
     *
     * <h4>Overriding</h4>
     * The default implementation returns {@code null}. If a subclass overrides this method, then it should
     * also override {@link #getAuthorityCodes(Class)} because {@code WKTDictionary} does not know the codes
     * that this method can recognize.
     *
     * @param  identifier  the code specified by user, possible with code space and version.
     * @return Well-Known Text (WKT) for the given identifier, or {@code null} if none.
     * @throws FactoryException if an error occurred while fetching the WKT.
     */
    protected String fetchDefinition(DefaultIdentifier identifier) throws FactoryException {
        return null;
    }

    /**
     * Produces an error message for unexpected characters at the end of WKT string.
     *
     * @param  lineNumber  line where the error occurred.
     * @param  wkt         the WKT being parsed.
     * @param  end         end of WKT parsing.
     * @return message to give to exception constructor.
     */
    private String unexpectedText(final int lineNumber, final String wkt, final int end) {
        return resources().getString(Resources.Keys.UnexpectedTextAtLine_2, lineNumber, CharSequences.token(wkt, end));
    }

    /**
     * Convenience methods for resources in the language used for error messages.
     */
    private Resources resources() {
        return Resources.forLocale(parser.getErrorLocale());
    }

    /**
     * Trims the leading and trailing spaces of the string representation of given object.
     * If null, empty or contains only spaces, then this method returns {@code null}.
     */
    private static String trimOrNull(final Object value) {
        return (value != null) ? Strings.trimOrNull(value.toString()) : null;
    }

    /**
     * Adds all definition values to the given supplier. This is for testing purposes only.
     * This method performs no locking because it is not needed for current JUnit tests.
     *
     * @see StoredTree#forEachValue(Consumer)
     */
    final void forEachValue(final Consumer<Object> addTo) {
        for (final Object value : definitions.values()) {
            if (value instanceof Disambiguation) {
                Disambiguation choices = (Disambiguation) value;
                do {
                    addTo.accept(choices.value);
                    choices = choices.previous;
                } while (choices != null);
            } else {
                addTo.accept(value);
            }
        }
    }

    /**
     * Returns the authority or specification that defines the codes recognized by this factory.
     * This is the first of the following values, in preference order:
     *
     * <ol>
     *   <li>The authority explicitly specified at construction time.</li>
     *   <li>A citation built from the most frequent value found in {@code CITATION} elements.</li>
     *   <li>A citation built from the most frequent value found in {@code ID} or {@code AUTHORITY} elements.</li>
     * </ol>
     *
     * @return the organization responsible for CRS definitions, or {@code null} if unknown.
     */
    @Override
    public Citation getAuthority() {
        return authority;
    }

    /**
     * Returns all namespaces recognized by this factory. Those namespaces can appear before codes in
     * calls to {@code createFoo(String)} methods, for example {@code "ESRI"} in {@code "ESRI:102018"}.
     * Namespaces are case-insensitive.
     *
     * @return the namespaces recognized by this factory.
     */
    @Override
    public Set<String> getCodeSpaces() {
        lock.readLock().lock();
        try {
            switch (codespaces.size()) {
                case 0:  return Collections.emptySet();
                case 1:  return Collections.singleton(codespaces.first());    // Most common case.
                default: return new LinkedHashSet<>(codespaces);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the set of authority codes for objects of the given type.
     * The {@code type} argument specifies the base type of identified objects.
     *
     * @param  type  the spatial reference objects type.
     * @return the set of authority codes for spatial reference objects of the given type.
     * @throws FactoryException if an error occurred while fetching the codes.
     */
    @Override
    public Set<String> getAuthorityCodes(Class<? extends IdentifiedObject> type) throws FactoryException {
        if (!type.isInterface()) {
            type = ReferencingUtilities.getInterface(IdentifiedObject.class, type);
        }
        Set<String> codes;
        lock.readLock().lock();
        try {
            codes = codeCaches.get(type);
        } finally {
            lock.readLock().unlock();
        }
        if (codes == null) {
            final String[] keywords = WKTKeywords.forType(type);
            final Class<? extends IdentifiedObject> baseType = type;                // Because lambdas require final.
            final Predicate<Object> filter = (element) -> {
                if (element instanceof StoredTree) {
                    return (keywords == null) || ArraysExt.containsIgnoreCase(keywords, ((StoredTree) element).keyword());
                } else {
                    return baseType.isInstance(element);
                }
            };
            lock.writeLock().lock();
            try {
                codes = codeCaches.get(type);                           // In case it has been computed concurrently.
                if (codes == null) {
                    codes = new HashSet<>();
                    for (final Map.Entry<String,Object> entry : definitions.entrySet()) {
                        final String code  = entry.getKey();
                        final Object value = entry.getValue();
                        if (value instanceof Disambiguation) {
                            Disambiguation.list((Disambiguation) value, code, filter, codes);
                        } else if (filter.test(value)) {
                            codes.add(code);
                        }
                    }
                    /*
                     * Verify if an existing collection (assigned to another type) provides the same values.
                     * If we find one, share the same instance for reducing memory usage.
                     */
                    for (final Set<String> other : codeCaches.values()) {
                        if (codes.equals(other)) {
                            codes = other;
                            break;
                        }
                    }
                    codes = Set.copyOf(codes);
                    codeCaches.put(type, codes);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        return codes;
    }

    /**
     * Gets a description of the object corresponding to a code.
     *
     * @param  type  the type of object for which to get a description.
     * @param  code  value allocated by authority.
     * @return a description of the object, or {@code null} if {@code null} if none.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the query failed for some other reason.
     *
     * @since 1.5
     */
    @Override
    public Optional<InternationalString> getDescriptionText(Class<? extends IdentifiedObject> type, final String code)
            throws FactoryException
    {
        final InternationalString name;
        final Object value = getOrCreate(code, false);
        if (value instanceof IdentifiedObject) {
            name = IdentifiedObjects.getDisplayName((IdentifiedObject) value);
        } else {
            final String text = String.valueOf(value);
            if (!(value instanceof StoredTree)) {
                // Exception message saved in a previous invocation of `getOrCreate(…)`.
                throw new FactoryException(text);
            }
            name = new SimpleInternationalString(text);
        }
        return Optional.ofNullable(name);
    }

    /**
     * Returns an arbitrary object from a code.
     *
     * @param  code  value allocated by authority.
     * @return the object for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     */
    @Override
    public IdentifiedObject createObject(final String code) throws FactoryException {
        final Object value = getOrCreate(code, true);
        if (value instanceof IdentifiedObject) {
            return (IdentifiedObject) value;
        } else {
            // Exception message saved in a previous invocation of `getOrCreate(…)`.
            throw new FactoryException(String.valueOf(value));
        }
    }

    /**
     * Returns the object associated to the given code.
     *
     * @param  code    value allocated by authority.
     * @param  create  whether to create {@link IdentifiedObject} from {@link StoredTree}.
     * @return the object for the given code, possibly as a {@link StoredTree} if {@code create} is {@code false}.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     */
    private Object getOrCreate(final String code, final boolean create) throws FactoryException {
        /*
         * Separate the authority from the rest of the code. The CharSequences.skipWhitespaces(…)
         * methods are robust to negative index and will work even if code.indexOf(…) returned -1.
         */
        String codespace = null;
        String version   = null;
        String localCode = code;
        int afterAuthority = code.indexOf(Constants.DEFAULT_SEPARATOR);
        int end = CharSequences.skipTrailingWhitespaces(code, 0, afterAuthority);
        int start = CharSequences.skipLeadingWhitespaces(code, 0, end);
        if (start < end) {
            codespace = code.substring(start, end);
            /*
             * Separate the version from the rest of the code. The version is optional. The code may have no room
             * for version (e.g. "EPSG:4326"), or specify an empty version (e.g. "EPSG::4326"). If the version is
             * equals to an empty string, it will be considered as no version.
             */
            int afterVersion = code.indexOf(Constants.DEFAULT_SEPARATOR, ++afterAuthority);
            start = CharSequences.skipLeadingWhitespaces(code, afterAuthority, afterVersion);
            end = CharSequences.skipTrailingWhitespaces(code, start, afterVersion++);
            if (start < end) {
                version = code.substring(start, end);
            }
            start = Math.max(afterAuthority, afterVersion);
            end = code.length();
            localCode = CharSequences.trimWhitespaces(code, start, end).toString();
        }
        /*
         * At this point we separated codespace, code and version. First, verify that codespace is valid.
         * Then get CRS definition as an `IdentifiedObject` or an `StoredTree` (the `Disambiguation` case
         * is resolved as an `IdentifiedObject` or `StoredTree`).
         */
        Disambiguation choices = null;
        Object value = null;
        lock.readLock().lock();
        try {
            boolean valid = Strings.isNullOrEmpty(codespace) || codespaces.contains(codespace);
            if (!valid) {
                for (final String cs : codespaces) {            // More costly check if no exact match.
                    valid = cs.equalsIgnoreCase(codespace);
                    if (valid) break;
                }
            }
            if (valid) {
                value = definitions.get(localCode);
                if (value instanceof Disambiguation) {
                    choices = Disambiguation.find((Disambiguation) value, codespace, version, localCode);
                    value = (choices != null) ? choices.value : null;
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        /*
         * If the value has not been found, check if subclass has a mechanism for fetching WKT
         * when first needed. It happens for example if subclass get WKT definitions from the
         * "spatial_ref_syst" table of a database.
         */
        if (value == null) {
            final DefaultIdentifier identifier = new DefaultIdentifier(codespace, localCode, version);
            final String wkt = fetchDefinition(identifier);
            if (wkt != null) {
                return parseAndAdd(codespace, version, localCode, wkt, identifier);
            }
            throw new NoSuchAuthorityCodeException(parser.errors().getString(
                    Errors.Keys.NoSuchValue_1, code), codespace, localCode, code);
        }
        /*
         * At this point we got a value which may be one of the following classes:
         *
         *   - `StoredTree`       — if this method is invoked for the first time for the given code.
         *   - `IdentifiedObject` — if we already built the geodetic object in a previous invocation of this method.
         *   - `String`           — if a previous invocation for given code failed to build the geodetic object.
         *                          In this case, the string is the exception message.
         *
         * If `StoredTree`, try to replace that value by an `IdentifiedObject` (on success) or `String` (on failure).
         * Must be done under write lock because `parser` is not thread-safe.
         */
        if (create && value instanceof StoredTree) {
            lock.writeLock().lock();
            try {
                if (choices != null) {
                    value = choices.value;              // Check again in case value has been computed concurrently.
                } else {
                    value = definitions.get(localCode);
                }
                if (value instanceof StoredTree) {
                    ParseException cause = null;
                    try {
                        value = parser.buildFromTree((StoredTree) value);
                        parser.logWarnings(WKTDictionary.class, "createObject");    // `createObject` is the public facade.
                    } catch (ParseException e) {
                        cause = e;
                        value = e.getLocalizedMessage();
                        if (value == null) {
                            value = e.getClass().getSimpleName();
                        }
                    }
                    if (choices != null) {
                        choices.value = value;          // Save result for future uses.
                    } else {
                        definitions.put(localCode, value);
                    }
                    codeCaches.clear();
                    if (cause != null) {
                        throw new FactoryException(resources().getString(
                                Resources.Keys.CanNotInstantiateGeodeticObject_1, code), cause);
                    }
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        return value;
    }
}
