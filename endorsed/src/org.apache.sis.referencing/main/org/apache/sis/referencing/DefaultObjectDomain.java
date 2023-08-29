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
import org.opengis.metadata.extent.Extent;
import org.opengis.util.InternationalString;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.referencing.util.WKTKeywords;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.metadata.iso.extent.DefaultExtent;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.ObjectDomain;


/**
 * Default implementation of scope and domain of validity of a CRS-related object.
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
     * The <i>"not known"</i> text is standardized by ISO 19111.
     * The text may be localized.
     *
     * @see #isDefined(InternationalString)
     */
    public static final InternationalString UNKNOWN_SCOPE = Vocabulary.formatInternational(Vocabulary.Keys.NotKnown);

    /**
     * The extent used by default when the domain of validity was not specified.
     *
     * @see #isDefined(Extent)
     */
    public static final Extent UNKNOWN_EXTENT;
    static {
        final var domainOfValidity = new DefaultExtent(UNKNOWN_SCOPE, null, null, null);
        domainOfValidity.transitionTo(DefaultExtent.State.FINAL);
        UNKNOWN_EXTENT = domainOfValidity;
    }

    /**
     * Description of domain of usage, or limitations of usage, for which the object is valid.
     *
     * @see #getScope()
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private final InternationalString scope;

    /**
     * Area for which the object is valid.
     *
     * @see #getDomainOfValidity()
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private final Extent domainOfValidity;

    /**
     * Creates a new domain with the given scope and extent.
     * If any value is {@code null}, the text will be set to "not known" (potentially localized).
     * The <i>"not known"</i> text is standardized by ISO 19111 for the scope.
     *
     * @param scope             description of domain of usage, or limitations of usage.
     * @param domainOfValidity  area for which the object is valid.
     */
    public DefaultObjectDomain(InternationalString scope, Extent domainOfValidity) {
        if (scope == null) {
            scope = UNKNOWN_SCOPE;
        }
        if (domainOfValidity == null) {
            domainOfValidity = UNKNOWN_EXTENT;
        }
        this.scope = scope;
        this.domainOfValidity = domainOfValidity;
    }

    /**
     * Returns a description of usage, or limitations of usage, for which this object is valid.
     *
     * @return the domain of usage.
     */
    @Override
    public InternationalString getScope() {
        return scope;
    }

    /**
     * Returns the spatial and temporal extent in which this object is valid.
     *
     * @return the area or time frame of usage.
     */
    @Override
    public Extent getDomainOfValidity() {
        return domainOfValidity;
    }

    /**
     * Returns {@code true} if the given scope is neither null or not known.
     *
     * @param  scope  the scope to test.
     * @return {@code true} if the given scope is not null and not {@link #UNKNOWN_SCOPE}.
     */
    public static boolean isDefined(final InternationalString scope) {
        return (scope != null) && (scope != UNKNOWN_SCOPE);
    }

    /**
     * Returns {@code true} if the given extent is neither null or not known.
     *
     * @param  domainOfValidity  the extent to test.
     * @return {@code true} if the given extent is not null and not {@link #UNKNOWN_EXTENT}.
     */
    public static boolean isDefined(final Extent domainOfValidity) {
        return (domainOfValidity != null) && (domainOfValidity != UNKNOWN_EXTENT);
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
     * @param  mode    {@link ComparisonMode#STRICT STRICT} or
     *                 {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA}.
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
     * Formats the inner part of the <cite>Well Known Text</cite> (WKT) representation for this object.
     * The default implementation writes the following elements:
     *
     * <ul>
     *   <li>The object {@linkplain #getScope() scope}.</li>
     *   <li>The geographic description of the {@linkplain #getDomainOfValidity() domain of validity}.</li>
     *   <li>The geographic bounding box of the domain of validity.</li>
     * </ul>
     *
     * @param  formatter  the formatter where to format the inner content of this WKT element.
     * @return the {@linkplain org.apache.sis.io.wkt.KeywordCase#CAMEL_CASE CamelCase} keyword
     *         for the WKT element, or {@code null} if unknown.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        formatter.append(scope, domainOfValidity);
        return WKTKeywords.Usage;
    }
}
