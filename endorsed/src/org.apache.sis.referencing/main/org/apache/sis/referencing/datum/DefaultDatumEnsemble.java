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
package org.apache.sis.referencing.datum;

import java.util.Map;
import java.util.List;
import java.util.Collection;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.datum.Datum;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.resources.Errors;


/**
 * Collection of datums which for low accuracy requirements may be considered
 * to be insignificantly different from each other.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @param <D> the type of datum contained in this ensemble.
 *
 * @since 1.5
 */
public class DefaultDatumEnsemble<D extends Datum> extends AbstractIdentifiedObject {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2757133322734036975L;

    /**
     * Datum or reference frames which are members of this ensemble.
     */
    @SuppressWarnings("serial")                     // Standard Java implementations are serializable.
    private final List<D> members;

    /**
     * Inaccuracy introduced through use of this ensemble.
     * This property is mandatory.
     */
    @SuppressWarnings("serial")                     // Most SIS implementations are serializable.
    private final PositionalAccuracy ensembleAccuracy;

    /**
     * Creates a datum ensemble from the given properties.
     * The properties given in argument follow the same rules as for the
     * {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link Identifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link Identifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>{@code "domains"}</td>
     *     <td>{@link org.opengis.referencing.ObjectDomain} (optionally as array)</td>
     *     <td>{@link #getDomains()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties  the properties to be given to the identified object.
     * @param  members     datum or reference frames which are members of this ensemble.
     * @param  accuracy    inaccuracy introduced through use of this ensemble (mandatory).
     * @throws IllegalArgumentException if at least two different
     *         {@linkplain AbstractDatum#getConventionalRS() conventional reference systems} are found.
     */
    public DefaultDatumEnsemble(Map<String,?> properties, Collection<? extends D> members, PositionalAccuracy accuracy) {
        super(properties);
        ArgumentChecks.ensureNonNull("accuracy", accuracy);
        ensembleAccuracy = accuracy;
        this.members = List.copyOf(members);
        validate();
    }

    /**
     * Verifies this ensemble. All members shall have the same conventional reference system.
     * No member can be an instance of {@link PseudoDatum}.
     */
    private void validate() {
        IdentifiedObject rs = null;
        for (final D datum : members) {
            if (datum instanceof PseudoDatum<?>) {
                throw new IllegalArgumentException(
                        Errors.format(Errors.Keys.IllegalPropertyValueClass_2, "members", PseudoDatum.class));
            }
            if (!(datum instanceof AbstractDatum)) continue;
            final IdentifiedObject dr = ((AbstractDatum) datum).getConventionalRS().orElse(null);
            if (dr != null) {
                if (rs == null) {
                    rs = dr;
                } else if (!rs.equals(dr)) {
                    throw new IllegalArgumentException(Resources.format(Resources.Keys.ShallHaveSameConventionalRS));
                }
            }
        }
    }

    /**
     * Returns the datum or reference frames which are members of this ensemble.
     *
     * @return datum or reference frames which are members of this ensemble.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")     // Collection is unmodifiable.
    public Collection<D> getMembers() {
        return members;
    }

    /**
     * Returns the inaccuracy introduced through use of this ensemble.
     *
     * @return inaccuracy introduced through use of this ensemble.
     */
    public PositionalAccuracy getEnsembleAccuracy() {
        return ensembleAccuracy;
    }

    /**
     * Compares the specified object with this ensemble for equality.
     * If the {@code mode} argument value is {@link ComparisonMode#STRICT STRICT} or
     * {@link ComparisonMode#BY_CONTRACT BY_CONTRACT}, then all available properties are compared including the
     * {@linkplain #getDomains() domains}.
     *
     * @param  object  the object to compare to {@code this}.
     * @param  mode    {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *                 {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for comparing only
     *                 properties relevant to coordinate transformations.
     * @return {@code true} if both objects are equal.
     *
     * @hidden because nothing new to said.
     */
    @Override
    public boolean equals(Object object, final ComparisonMode mode) {
        if (mode != ComparisonMode.STRICT && object instanceof PseudoDatum<?>) {
            object = ((PseudoDatum<?>) object).ensemble;
        }
        if (!super.equals(object, mode)) {
            return false;
        }
        switch (mode) {
            case STRICT: {
                final var that = (DefaultDatumEnsemble<?>) object;
                return members.equals(that.members) && ensembleAccuracy.equals(that.ensembleAccuracy);
            }
            default: {
                if (!(object instanceof DefaultDatumEnsemble<?>)) {
                    return false;
                }
                final var that = (DefaultDatumEnsemble<?>) object;
                return Utilities.deepEquals(getMembers(), that.getMembers(), mode) &&
                       Utilities.deepEquals(getEnsembleAccuracy(), that.getEnsembleAccuracy(), mode);
            }
        }
    }

    /**
     * Invoked by {@code hashCode()} for computing the hash code when first needed.
     * See {@link org.apache.sis.referencing.AbstractIdentifiedObject#computeHashCode()}
     * for more information.
     *
     * @return the hash code value. This value may change in any future Apache SIS version.
     *
     * @hidden because nothing new to said.
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + 7*members.hashCode() + 37*ensembleAccuracy.hashCode();
    }

    /**
     * Formats the inner part of the <i>Well Known Text</i> (WKT) representation for this ensemble.
     * See {@link AbstractIdentifiedObject#formatTo(Formatter)} for more information.
     *
     * @param  formatter  the formatter where to format the inner content of this WKT element.
     * @return the {@linkplain org.apache.sis.io.wkt.KeywordCase#CAMEL_CASE CamelCase} keyword
     *         for the WKT element, or {@code null} if unknown.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        if (Convention.WKT2_2015.compareTo(formatter.getConvention()) >= 0) {
            formatter.setInvalidWKT(this, null);
        }
        return WKTKeywords.Ensemble;
    }
}
