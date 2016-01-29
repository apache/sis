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
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.cs.CSAuthorityFactory;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;
import org.apache.sis.util.iso.DefaultNameSpace;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.metadata.NameMeaning;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Identification of an authority factory by its type, namespace and version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class AuthorityFactoryIdentifier {
    /**
     * Factory needed is {@link CRSAuthorityFactory}.
     */
    static final byte CRS = 0;

    /**
     * Factory needed is {@link CSAuthorityFactory}.
     */
    static final byte CS = 1;

    /**
     * Factory needed is {@link DatumAuthorityFactory}.
     */
    static final byte DATUM = 2;

    /**
     * Factory needed is {@link CoordinateOperationAuthorityFactory}.
     */
    static final byte OPERATION = 3;

    /**
     * Factory needed is the Apache-SIS specific {@link GeodeticAuthorityFactory}.
     */
    static final byte GEODETIC = 4;

    /**
     * Factory needed is {@link AuthorityFactory}, the base interface of all factories.
     */
    static final byte ANY = 5;

    /**
     * The interfaces or abstract base classes for the above constants.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final Class<? extends AuthorityFactory> TYPES[] = new Class[6];
    static {
        TYPES[CRS]       = CRSAuthorityFactory.class;
        TYPES[CS]        = CSAuthorityFactory.class;
        TYPES[DATUM]     = DatumAuthorityFactory.class;
        TYPES[OPERATION] = CoordinateOperationAuthorityFactory.class;
        TYPES[GEODETIC]  = GeodeticAuthorityFactory.class;
        TYPES[ANY]       = AuthorityFactory.class;
    }

    /**
     * The type of the factory needed, as one of the {@link #CRS}, {@link #CS}, {@link #DATUM},
     * {@link #OPERATION}, {@link #GEODETIC} or {@link #ANY} constants.
     */
    final byte type;

    /**
     * The authority of the factory, in upper case. The upper case policy should be kept
     * consistent with {@link org.apache.sis.internal.metadata.NameMeaning#AUTHORITIES}.
     *
     * <div class="note"><b>Example:</b>
     * In the {@code "urn:ogc:def:crs:EPSG:8.2:4326"} URN, this is {@code "EPSG"}.</div>
     *
     * @see #getAuthority()
     * @see org.apache.sis.internal.util.DefinitionURI
     * @see org.apache.sis.internal.metadata.NameMeaning
     */
    private String authority;

    /**
     * The version part of a URI, or {@code null} if none.
     * If the version contains alphabetic characters, they should be in lower case.
     *
     * <div class="note"><b>Example:</b>
     * In the {@code "urn:ogc:def:crs:EPSG:8.2:4326"} URN, this is {@code "8.2"}.</div>
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
     */
    private AuthorityFactoryIdentifier(final byte type, final String authority, final String version) {
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
        for (byte i=0; i<TYPES.length; i++) {
            if (TYPES[i].isAssignableFrom(type)) {
                return create(i, authority, version);
            }
        }
        throw new IllegalArgumentException();   // Should never happen since above loop should have found ANY.
    }

    /**
     * Creates a new identifier for a factory of the given type, authority and version.
     * Only the version can be null.
     */
    static AuthorityFactoryIdentifier create(final byte type, final String authority, final String version) {
        return new AuthorityFactoryIdentifier(type, authority.toUpperCase(Locale.US),
                           (version == null) ? null : version.toLowerCase(Locale.US));
    }

    /**
     * Returns an identifier for a factory of the same type than this identifier,
     * but a different authority and no version.
     */
    AuthorityFactoryIdentifier unversioned(final String newAuthority) {
        if (version == null && newAuthority.equals(authority)) {
            return this;
        }
        return new AuthorityFactoryIdentifier(type, newAuthority.toUpperCase(Locale.US), null);
    }

    /**
     * Creates a new identifier for the same type and authority than this identifier, but a different version
     * extracted from the given authority.
     *
     * @param  factory The factory's authority, or {@code null} for creating an identifier without version.
     * @return An identifier for the version of the given authority, or {@code this} if the version is the same.
     */
    AuthorityFactoryIdentifier versionOf(final Citation factory) {
        String newVersion = NameMeaning.getVersion(factory);
        if (newVersion != null) {
            newVersion = newVersion.toLowerCase(Locale.US);
        }
        if (Objects.equals(version, newVersion)) {
            return this;
        }
        return new AuthorityFactoryIdentifier(type, authority, newVersion);
    }

    /**
     * Creates a new identifier for the same authority and version than this identifier, but a different factory.
     */
    AuthorityFactoryIdentifier newType(final byte newType) {
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
        return type + 31*authority.hashCode() + Objects.hashCode(version);
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
     * Returns {@code true} if the given identifier is for the same authority than this identifier.
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
     * @param used The factory which will be used.
     */
    void logConflict(final AuthorityFactory used) {
        log(Messages.getResources(null).getLogRecord(Level.WARNING, Messages.Keys.IgnoredServiceProvider_3,
                TYPES[type], getAuthorityAndVersion(), Classes.getClass(used)));
    }

    /**
     * Logs a warning about a factory not found for the requested version, in which case
     * {@code AuthorityFactoryIdentifier} fallback on a default version.
     */
    void logFallback() {
        log(Messages.getResources(null).getLogRecord(Level.WARNING, Messages.Keys.FallbackDefaultFactoryVersion_2,
                authority, version));
    }

    /**
     * Do the logging of the warning prepared by the above methods.
     */
    private void log(final LogRecord record) {
        record.setLoggerName(Loggers.CRS_FACTORY);
        // MultiAuthoritiesFactory.getAuthorityFactory(…) is the nearest public API.
        Logging.log(MultiAuthoritiesFactory.class, "getAuthorityFactory", record);
    }

    /**
     * Returns a string representation of this identifier for debugging purpose.
     */
    @Debug
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        buffer.append(Classes.getShortName(TYPES[type])).append(DefaultNameSpace.DEFAULT_SEPARATOR).append(authority);
        if (version != null) {
            buffer.append(DefaultNameSpace.DEFAULT_SEPARATOR).append(version);
        }
        return buffer.toString();
    }
}
