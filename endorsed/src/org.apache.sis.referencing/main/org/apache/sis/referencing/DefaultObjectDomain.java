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
package org.apache.sis.referencing;

import java.util.Objects;
import java.io.Serializable;
import java.io.ObjectStreamException;
import org.opengis.metadata.extent.Extent;
import org.opengis.util.InternationalString;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.xml.NilObject;
import org.apache.sis.xml.NilReason;
import org.apache.sis.metadata.iso.extent.DefaultExtent;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.ObjectDomain;


/**
 * Scope and domain of validity of a CRS-related object.
 * Those two properties are mandatory according ISO 19111.
 * If a property is unspecified (by passing {@code null} to the constructor),
 * then this class substitutes the null value by a <i>"not known"</i> text in an
 * object implementing the {@link NilObject} interface with {@link NilReason#UNKNOWN}.
 * The use of <i>"not known"</i> text is an ISO 19111 recommendation.
 *
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thus thread-safe if the property values
 * given to the constructor are also immutable.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.4
 */
public class DefaultObjectDomain extends FormattableObject implements ObjectDomain, LenientComparable, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5797839090924498526L;

    /**
     * The text used by default when the scope was not specified.
     * The <i>"not known"</i> text is recommended by the ISO 19111 standard.
     * The text may be localized.
     */
    private static final class UnknownScope extends Vocabulary.International implements NilObject {
        /** For cross-version interoperability. */
        private static final long serialVersionUID = 7235301883912422934L;

        /** The singleton instance. */
        static final UnknownScope INSTANCE = new UnknownScope();

        /** Creates the singleton instance. */
        private UnknownScope() {
            super(Vocabulary.Keys.NotKnown);
        }

        /**
         * {@return a reason saying that the extent is unknown}.
         */
        @Override
        public NilReason getNilReason() {
            return NilReason.UNKNOWN;
        }

        /**
         * Returns the unique instance on deserialization.
         *
         * @return the object to use after deserialization.
         * @throws ObjectStreamException if the serialized object contains invalid data.
         */
        private Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * The extent used by default when the scope was not specified.
     */
    private static final class UnknownExtent extends DefaultExtent implements NilObject {
        /** For cross-version interoperability. */
        private static final long serialVersionUID = 662383891780679068L;

        /** The singleton instance. */
        static final UnknownExtent INSTANCE = new UnknownExtent();

        /** Creates the singleton instance. */
        private UnknownExtent() {
            super(UnknownScope.INSTANCE, null, null, null);
            transitionTo(DefaultExtent.State.FINAL);
        }

        /**
         * {@return a reason saying that the extent is unknown}.
         */
        @Override
        public NilReason getNilReason() {
            return NilReason.UNKNOWN;
        }

        /**
         * Returns the unique instance on deserialization.
         *
         * @return the object to use after deserialization.
         * @throws ObjectStreamException if the serialized object contains invalid data.
         */
        private Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Description of domain of usage, or limitations of usage, for which the object is valid.
     * This is {@code null} (i.e. is not replaced by the <i>"not known"</i> text) if the value given
     * to the {@linkplain #DefaultObjectDomain(InternationalString, Extent) constructor} was null.
     *
     * @see #getScope()
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    protected final InternationalString scope;

    /**
     * Area for which the object is valid.
     * This is {@code null} (i.e. is not replaced by the <i>"not known"</i> text) if the value given
     * to the {@linkplain #DefaultObjectDomain(InternationalString, Extent) constructor} was null.
     *
     * @see #getDomainOfValidity()
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    protected final Extent domainOfValidity;

    /**
     * Creates a new domain with the given scope and extent.
     * If any value is {@code null}, the text will be set to "not known" (potentially localized).
     * The <i>"not known"</i> text is standardized by ISO 19111 for the scope.
     *
     * @param scope             description of domain of usage, or limitations of usage.
     * @param domainOfValidity  area for which the object is valid.
     */
    public DefaultObjectDomain(final InternationalString scope, final Extent domainOfValidity) {
        this.scope = scope;
        this.domainOfValidity = domainOfValidity;
    }

    /**
     * Creates a new domain with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  domain  the domain to copy.
     *
     * @see #castOrCopy(ObjectDomain)
     */
    public DefaultObjectDomain(final ObjectDomain domain) {
        scope = domain.getScope();
        domainOfValidity = domain.getDomainOfValidity();
    }

    /**
     * Returns a SIS datum implementation with the same values as the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultObjectDomain castOrCopy(final ObjectDomain object) {
        return (object == null) || (object instanceof DefaultObjectDomain)
                ? (DefaultObjectDomain) object : new DefaultObjectDomain(object);
    }

    /**
     * Returns a description of usage, or limitations of usage, for which this object is valid.
     * If no scope was specified to the constructor, then this method returns <i>"not known"</i>
     * in an instance implementing the {@link NilObject} interface with {@link NilReason#UNKNOWN}.
     *
     * @return the domain of usage.
     */
    @Override
    public InternationalString getScope() {
        return (scope != null) ? scope : UnknownScope.INSTANCE;
    }

    /**
     * Returns the spatial and temporal extent in which this object is valid.
     * If no extent was specified to the constructor, then this method returns <i>"not known"</i>
     * in an instance implementing the {@link NilObject} interface with {@link NilReason#UNKNOWN}.
     *
     * @return the area or time frame of usage.
     */
    @Override
    public Extent getDomainOfValidity() {
        return (domainOfValidity != null) ? domainOfValidity : UnknownExtent.INSTANCE;
    }

    /**
     * Compares the specified object with this object for equality.
     * This method is implemented as below (omitting assertions):
     *
     * {@snippet lang="java" :
     *     return equals(other, ComparisonMode.STRICT);
     *     }
     *
     * Subclasses shall override {@link #equals(Object, ComparisonMode)} instead of this method.
     *
     * @param  object  the other object (may be {@code null}).
     * @return {@code true} if both objects are equal.
     */
    @Override
    public final boolean equals(final Object object) {
        final boolean eq = equals(object, ComparisonMode.STRICT);
        // If objects are equal, then they must have the same hash code value.
        assert !eq || hashCode() == object.hashCode() : this;
        return eq;
    }

    /**
     * Compares this object system with the specified object for equality.
     *
     * @param  object  the object to compare to {@code this}.
     * @param  mode    the strictness level of the comparison.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (mode == ComparisonMode.STRICT) {
            if (object != null && object.getClass() == getClass()) {
                final var that = (DefaultObjectDomain) object;
                return Objects.equals(scope, that.scope) &&
                       Objects.equals(domainOfValidity, that.domainOfValidity);
            }
        } else {
            if (object instanceof ObjectDomain) {
                final var that = (ObjectDomain) object;
                return Utilities.deepEquals(getScope(), that.getScope(), mode) &&
                       Utilities.deepEquals(getDomainOfValidity(), that.getDomainOfValidity(), mode);
            }
        }
        return false;
    }

    /**
     * Returns a hash code value for this domain.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return Objects.hash(scope, domainOfValidity);
    }

    /**
     * Formats the inner part of the <i>Well Known Text</i> (WKT) representation for this object.
     * The default implementation writes the following elements:
     *
     * <ul>
     *   <li>The object {@linkplain #scope}.</li>
     *   <li>The geographic description of the {@linkplain #domainOfValidity domain of validity}.</li>
     *   <li>The geographic bounding box of the domain of validity.</li>
     * </ul>
     *
     * @param  formatter  the formatter where to format the inner content of this WKT element.
     * @return the {@linkplain org.apache.sis.io.wkt.KeywordCase#CAMEL_CASE CamelCase} keyword
     *         for the WKT element, or {@code null} if unknown.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        formatter.newLine();
        // Use the fields directly in order to keep null values.
        formatter.append(scope, domainOfValidity);
        return WKTKeywords.Usage;
    }
}
