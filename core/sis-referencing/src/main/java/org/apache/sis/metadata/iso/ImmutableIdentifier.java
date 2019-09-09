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
package org.apache.sis.metadata.iso;

import java.util.Map;
import java.util.Locale;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.util.InternationalString;


/**
 * Immutable value uniquely identifying an object within a namespace, together with a version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since 0.3
 * @module
 *
 * @deprecated Moved to {@link org.apache.sis.referencing} for anticipation with Jigsaw modules.
 */
@Deprecated
public class ImmutableIdentifier extends org.apache.sis.referencing.ImmutableIdentifier {

    /**
     * Creates a new identifier from the specified one. This is a copy constructor which
     * get the code, codespace, authority and version from the given identifier.
     *
     * @param identifier  the identifier to copy.
     *
     * @see #castOrCopy(Identifier)
     */
    public ImmutableIdentifier(final Identifier identifier) {
        super(identifier);
    }

    /**
     * Creates a new identifier from the specified code and authority.
     *
     * @param authority  the person or party responsible for maintenance of the namespace, or {@code null} if not available.
     * @param codeSpace  identifier or namespace in which the code is valid, or {@code null} if not available.
     *                   This is often an abbreviation of the authority name.
     * @param code       alphanumeric value identifying an instance in the namespace. The code can not be null.
     */
    public ImmutableIdentifier(final Citation authority, final String codeSpace, final String code) {
        super(authority, codeSpace, code);
    }

    /**
     * Creates a new identifier from the specified code and authority,
     * with an optional version number and description.
     *
     * @param authority    the person or party responsible for maintenance of the namespace, or {@code null} if not available.
     * @param codeSpace    identifier or namespace in which the code is valid, or {@code null} if not available.
     *                     This is often an abbreviation of the authority name.
     * @param code         alphanumeric value identifying an instance in the namespace. The code can not be null.
     * @param version      the version identifier for the namespace as specified by the code authority, or {@code null} if none.
     * @param description  natural language description of the meaning of the code value, or {@code null} if none.
     */
    public ImmutableIdentifier(final Citation authority, final String codeSpace,
            final String code, final String version, final InternationalString description)
    {
        super(authority, codeSpace, code, version, description);
    }

    /**
     * Constructs an identifier from the given properties. Keys are strings from the table below.
     * The map given in argument shall contain an entry at least for the
     * {@value org.opengis.metadata.Identifier#CODE_KEY} key.
     * Other properties listed in the table below are optional.
     *
     * <table class="sis">
     *   <caption>Recognized properties</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.metadata.Identifier#CODE_KEY}</td>
     *     <td>{@link String}</td>
     *     <td>{@link #getCode()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.metadata.Identifier#CODESPACE_KEY}</td>
     *     <td>{@link String}</td>
     *     <td>{@link #getCodeSpace()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.metadata.Identifier#AUTHORITY_KEY}</td>
     *     <td>{@link String} or {@link Citation}</td>
     *     <td>{@link #getAuthority()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.metadata.Identifier#VERSION_KEY}</td>
     *     <td>{@link String}</td>
     *     <td>{@link #getVersion()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.metadata.Identifier#DESCRIPTION_KEY}</td>
     *     <td>{@link String} or {@link InternationalString}</td>
     *     <td>{@link #getDescription()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.apache.sis.referencing.AbstractIdentifiedObject#LOCALE_KEY}</td>
     *     <td>{@link Locale}</td>
     *     <td>(none)</td>
     *   </tr>
     * </table>
     *
     * <div class="section">Localization</div>
     * {@code "description"} is a localizable attributes which may have a language and country
     * code suffix. For example the {@code "description_fr"} property stands for description in
     * {@linkplain Locale#FRENCH French} and the {@code "description_fr_CA"} property stands
     * for description in {@linkplain Locale#CANADA_FRENCH French Canadian}.
     *
     * <p>The {@code "locale"} property applies only to exception messages, if any.
     * After successful construction, {@code ImmutableIdentifier} instances do not keep the locale
     * since localizations are deferred to the {@link InternationalString#toString(Locale)} method.</p>
     *
     * @param  properties  the properties to be given to this identifier.
     * @throws IllegalArgumentException if a property has an illegal value.
     */
    public ImmutableIdentifier(final Map<String,?> properties) throws IllegalArgumentException {
        super(properties);
    }

    /**
     * Returns a SIS identifier implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code ImmutableIdentifier}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code ImmutableIdentifier} instance is created using the
     *       {@linkplain #ImmutableIdentifier(Identifier) copy constructor} and returned.
     *       Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static ImmutableIdentifier castOrCopy(final Identifier object) {
        if (object == null || object instanceof ImmutableIdentifier) {
            return (ImmutableIdentifier) object;
        }
        return new ImmutableIdentifier(object);
    }
}
