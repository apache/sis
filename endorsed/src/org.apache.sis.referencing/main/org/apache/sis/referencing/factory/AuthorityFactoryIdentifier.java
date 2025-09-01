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
package org.apache.sis.referencing.factory;

import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.cs.CSAuthorityFactory;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
import org.apache.sis.util.Classes;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.metadata.privy.NameMeaning;
import org.apache.sis.referencing.internal.Resources;


/**
 * Identification of an authority factory by its type, namespace and version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class AuthorityFactoryIdentifier {
    /**
     * The locale to use for identifiers. This is not necessarily the same locale
     * than the one used for logging or error messages.
     */
    private static final Locale IDENTIFIER_LOCALE = Locale.US;

    /**
     * The type of the authority factory. Order matter: specialized factories shall be first.
     * If two factories are equally specialized, the most frequently used ones should be first.
     *
     * @see MultiAuthoritiesFactory#providers
     */
    static enum Type {
        /**
         * Factory needed is {@link CRSAuthorityFactory}.
        */
        CRS(CRSAuthorityFactory.class),

        /**
         * Factory needed is {@link CSAuthorityFactory}.
         */
        CS(CSAuthorityFactory.class),

        /**
         * Factory needed is {@link DatumAuthorityFactory}.
         */
        DATUM(DatumAuthorityFactory.class),

        /**
         * Factory needed is {@link CoordinateOperationAuthorityFactory}.
         */
        OPERATION(CoordinateOperationAuthorityFactory.class),

        /**
         * Factory needed is the Apache-SIS specific {@link GeodeticAuthorityFactory}.
         */
        GEODETIC(GeodeticAuthorityFactory.class),

        /**
         * Factory needed is {@link AuthorityFactory}, the base interface of all factories.
         */
        ANY(AuthorityFactory.class);

        /**
         * The interface of the authority factory.
         */
        final Class<? extends AuthorityFactory> api;

        /**
         * Creates a new enumeration value.
         *
         * @param  base  the interface of the authority factory.
         */
        private Type(final Class<? extends AuthorityFactory> api) {
            this.api = api;
        }

        /**
         * Returns whether the <abbr>API</abbr> can create any or unspecified type of objects.
         */
        final boolean isGeneric() {
            return ordinal() >= GEODETIC.ordinal();
        }
    }

    /**
     * The type of the factory which is needed.
     */
    final Type type;

    /**
     * The authority of the factory, in upper case. The upper case policy should be kept
     * consistent with {@link org.apache.sis.metadata.privy.NameMeaning#AUTHORITIES}.
     *
     * <h4>Example</h4>
     * In the {@code "urn:ogc:def:crs:EPSG:8.2:4326"} URN, this is {@code "EPSG"}.
     *
     * @see org.apache.sis.util.privy.DefinitionURI
     * @see org.apache.sis.metadata.privy.NameMeaning
     */
    private String authority;

    /**
     * The version part of a <abbr>URI</abbr>, or {@code null} if none.
     * If the version contains alphabetic characters, they should be in lower case.
     *
     * <h4>Example</h4>
     * In the {@code "urn:ogc:def:crs:EPSG:8.2:4326"} URN, this is {@code "8.2"}.
     *
     * @see #hasVersion()
     * @see #unversioned(String)
     * @see #versionOf(Citation)
     */
    private String version;

    /**
     * Creates a new identifier for a factory of the given type, authority and version.
     * The given authority shall be already in upper cases and the version in lower cases
     * (this is not verified by this constructor).
     *
     * @param  type       the type of the factory which is needed.
     * @param  authority  the authority of the factory, in upper case.
     * @param  version    the version part of a <abbr>URI</abbr>, or {@code null} if none.
     */
    private AuthorityFactoryIdentifier(final Type type, final String authority, final String version) {
        this.type      = type;
        this.authority = authority;
        this.version   = version;
    }

    /**
     * Creates a new identifier for a factory of the given type, authority and version.
     * Only the version can be null.
     */
    static AuthorityFactoryIdentifier create(final Class<? extends AuthorityFactory> type,
            final String authority, final String version)
    {
        for (Type i : Type.values()) {
            if (i.api.isAssignableFrom(type)) {
                return create(i, authority, version);
            }
        }
        throw new IllegalArgumentException();   // Should never happen since above loop should have found ANY.
    }

    /**
     * Creates a new identifier for a factory of the given type, authority and version.
     * Only the version can be null.
     *
     * @param  type       the type of the factory which is needed.
     * @param  authority  the authority of the factory, case-insensitive.
     * @param  version    the version part of a <abbr>URI</abbr>, or {@code null} if none.
     * @return identifier for a factory of the given type, authority and version.
     */
    static AuthorityFactoryIdentifier create(final Type type, final String authority, final String version) {
        return new AuthorityFactoryIdentifier(type, authority.toUpperCase(IDENTIFIER_LOCALE),
                           (version == null) ? null : version.toLowerCase(IDENTIFIER_LOCALE));
    }

    /**
     * Returns an identifier for a factory of the same type as this identifier,
     * but a different authority and no version.
     *
     * @param  newAuthority  the authority of the factory, case-insensitive.
     * @return identifier for a factory of the given authority.
     */
    AuthorityFactoryIdentifier unversioned(final String newAuthority) {
        if (version == null && newAuthority.equals(authority)) {
            return this;
        }
        return new AuthorityFactoryIdentifier(type, newAuthority.toUpperCase(IDENTIFIER_LOCALE), null);
    }

    /**
     * Creates a new identifier for the same type and authority than this identifier, but a different version
     * extracted from the given authority.
     *
     * @param  factory  the factory's authority, or {@code null} for creating an identifier without version.
     * @return an identifier for the version of the given authority, or {@code this} if the version is the same.
     */
    AuthorityFactoryIdentifier versionOf(final Citation factory) {
        String newVersion = NameMeaning.getVersion(factory);
        if (newVersion != null) {
            newVersion = newVersion.toLowerCase(IDENTIFIER_LOCALE);
        }
        if (Objects.equals(version, newVersion)) {
            return this;
        }
        return new AuthorityFactoryIdentifier(type, authority, newVersion);
    }

    /**
     * Creates a new identifier for the same authority and version than this identifier, but a different factory.
     *
     * @param  type       the type of the factory which is needed.
     * @return identifier for a factory of the given type.
     */
    AuthorityFactoryIdentifier newType(final Type newType) {
        return new AuthorityFactoryIdentifier(newType, authority, version);
    }

    /**
     * Ensures that the authority and version use shared {@link String} instances. This method is invoked only when
     * we have determined that this {@code AuthorityFactoryIdentifier} instance will be used as a key in a hash map.
     */
    AuthorityFactoryIdentifier intern() {
        authority = authority.intern();
        if (version != null) {
            version = version.intern();
        }
        return this;
    }

    /**
     * Returns a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return type.hashCode() + 31*authority.hashCode() + Objects.hashCode(version);
    }

    /**
     * Compares the given object with this identifier.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof AuthorityFactoryIdentifier) {
            final AuthorityFactoryIdentifier that = (AuthorityFactoryIdentifier) other;
            if (type == that.type && authority.equals(that.authority)) {
                return Objects.equals(version, that.version);
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the given identifier is for the same authority as this identifier.
     */
    boolean isSameAuthority(final AuthorityFactoryIdentifier other) {
        return authority.equals(other.authority);
    }

    /**
     * Returns the authority with the version, if any.
     */
    CharSequence getAuthorityAndVersion() {
        CharSequence name = authority;
        if (hasVersion()) {
            name = Vocabulary.formatInternational(Vocabulary.Keys.Version_2, name, version);
        }
        return name;
    }

    /**
     * Returns {@code true} if this identifier is for a specific dataset version.
     */
    boolean hasVersion() {
        return version != null;
    }

    /**
     * Logs a message reporting a conflict between the factory identified by this {@code AuthorityFactoryIdentifier}
     * and another factory, if this instance has not already logged a warning. This method assumes that it is invoked
     * by the {@code MultiAuthoritiesFactory.getAuthorityFactory(…)} method.
     *
     * @param  used  the factory which will be used.
     */
    void logConflict(final AuthorityFactory used) {
        log(Resources.forLocale(null).createLogRecord(Level.WARNING, Resources.Keys.IgnoredServiceProvider_3,
                type.api, getAuthorityAndVersion(), Classes.getClass(used)));
    }

    /**
     * Logs a warning about a factory not found for the requested version, in which case
     * {@code AuthorityFactoryIdentifier} fallback on a default version.
     */
    void logFallback() {
        log(Resources.forLocale(null).createLogRecord(Level.WARNING, Resources.Keys.FallbackDefaultFactoryVersion_2,
                authority, version));
    }

    /**
     * Do the logging of the warning prepared by the above methods.
     * This method declares {@code MultiAuthoritiesFactory.getAuthorityFactory(…)}
     * as the source of the log since it is the nearest public API.
     */
    private void log(final LogRecord record) {
        Logging.completeAndLog(GeodeticAuthorityFactory.LOGGER,
                MultiAuthoritiesFactory.class, "getAuthorityFactory", record);
    }

    /**
     * Returns a string representation of this identifier for debugging purpose.
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        buffer.append(type).append(Constants.DEFAULT_SEPARATOR).append(authority);
        if (version != null) {
            buffer.append(Constants.DEFAULT_SEPARATOR).append(version);
        }
        return buffer.toString();
    }
}
