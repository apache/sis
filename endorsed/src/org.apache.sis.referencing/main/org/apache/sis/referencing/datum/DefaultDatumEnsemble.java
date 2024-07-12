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
import java.util.Objects;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.Convention;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.DatumEnsemble;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;


/**
 * Collection of datums which for low accuracy requirements may be considered
 * to be insignificantly different from each other.
 *
 * @author  OGC Topic 2 (for abstract model and documentation)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 *
 * @param <D> the type of datum contained in this ensemble.
 *
 * @since 1.5
 */
public class DefaultDatumEnsemble<D extends Datum> extends AbstractIdentifiedObject implements DatumEnsemble<D> {
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
     *     <td>{@value org.opengis.referencing.IdentifiedObject#DOMAINS_KEY}</td>
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
     * Creates a new ensemble with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  ensemble  the ensemble to copy.
     */
    protected DefaultDatumEnsemble(final DatumEnsemble<? extends D> ensemble) {
        super(ensemble);
        members = List.copyOf(ensemble.getMembers());
        ensembleAccuracy = Objects.requireNonNull(ensemble.getEnsembleAccuracy());
        validate();
    }

    /**
     * Returns a SIS ensemble implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultDatumEnsemble}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultDatumEnsemble} instance is created using the
     *       {@linkplain #DefaultDatumEnsemble(DatumEnsemble) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation,
     *       because the other properties contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  <D>     the type of datum contained in the ensemble.
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static <D extends Datum> DefaultDatumEnsemble<D> castOrCopy(final DatumEnsemble<D> object) {
        if (object == null || object instanceof DefaultDatumEnsemble<?>) {
            return (DefaultDatumEnsemble<D>) object;
        } else {
            return new DefaultDatumEnsemble<>(object);
        }
    }

    /**
     * Verifies this ensemble. All members shall have the same conventional reference system.
     */
    private void validate() {
        IdentifiedObject rs = null;
        for (final D datum : members) {
            final IdentifiedObject dr = datum.getConventionalRS().orElse(null);
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
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code DatumEnsemble.class}.
     *
     * <h4>Note for implementers</h4>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code DatumEnsemble}
     * sub-interface. Overriding possibility is left mostly for implementers who wish to extend GeoAPI with their
     * own set of interfaces.
     *
     * @return the datum interface implemented by this class.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends DatumEnsemble<D>> getInterface() {
        return (Class) DatumEnsemble.class;
    }

    /**
     * Returns the datum or reference frames which are members of this ensemble.
     *
     * @return datum or reference frames which are members of this ensemble.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")     // Collection is unmodifiable.
    public Collection<D> getMembers() {
        return members;
    }

    /**
     * Returns the inaccuracy introduced through use of this ensemble.
     *
     * @return inaccuracy introduced through use of this ensemble.
     */
    @Override
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
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (!super.equals(object, mode)) {
            return false;
        }
        switch (mode) {
            case STRICT: {
                final var that = (DefaultDatumEnsemble) object;
                return members.equals(that.members) && ensembleAccuracy.equals(that.ensembleAccuracy);
            }
            default: {
                final var that = (DatumEnsemble) object;
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
     * @hidden because not useful.
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
