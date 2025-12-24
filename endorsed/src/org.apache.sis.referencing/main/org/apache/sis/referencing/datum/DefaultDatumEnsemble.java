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
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.EngineeringDatum;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.util.InternationalString;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.GeodeticException;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.PositionalAccuracyConstant;
import org.apache.sis.referencing.internal.shared.WKTKeywords;
import org.apache.sis.referencing.internal.shared.WKTUtilities;
import org.apache.sis.metadata.internal.shared.NameToIdentifier;
import org.apache.sis.metadata.internal.shared.SecondaryTrait;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.Containers;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import java.util.Optional;
import java.time.temporal.Temporal;
import org.opengis.referencing.crs.ParametricCRS;
import org.opengis.referencing.datum.DatumEnsemble;
import org.opengis.referencing.datum.ParametricDatum;
import org.opengis.referencing.datum.RealizationMethod;


/**
 * Collection of datums which for low accuracy requirements may be considered
 * to be insignificantly different from each other.
 * Coordinate transformations between different {@linkplain #getMembers() members} of an ensemble
 * can be omitted if the {@linkplain #getEnsembleAccuracy() ensemble accuracy} is sufficient.
 *
 * <h2>Ensemble viewed as a datum</h2>
 * While a {@code DatumEnsemble} is theoretically <em>not</em> a {@code Datum},
 * it is sometime convenient to handle the ensemble <em>as if</em> it was a datum.
 * Therefore, Apache <abbr>SIS</abbr> implementation of {@link DatumEnsemble} implements also the {@link Datum} interface.
 * This is a non-standard approach for making easier to handle {@code Datum} and {@code DatumEnsemble} with the same code.
 * A similar approach is used in the <abbr>EPSG</abbr> database, which stores ensembles in the same table as the datums.
 *
 * <p>It is illegal to use this double inheritance for returning a {@code DatumEnsemble} from
 * {@link org.opengis.referencing.crs.SingleCRS#getDatum()}, or for creating a {@code DatumEnsemble}
 * containing a nested {@code DatumEnsemble}. The Apache <abbr>SIS</abbr> constructors verify these constraints.
 * For getting a single object which may be a datum or an ensemble, see the {@link DatumOrEnsemble} static methods.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @param <D> the type of datum members contained in this ensemble.
 *
 * @since 1.5
 */
@SecondaryTrait(Datum.class)
public class DefaultDatumEnsemble<D extends Datum> extends AbstractIdentifiedObject implements DatumEnsemble<D>, Datum {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2757133322734036975L;

    /**
     * Datum or reference frames which are members of this ensemble.
     *
     * @see #getMembers()
     */
    @SuppressWarnings("serial")                     // Standard Java implementations are serializable.
    private final List<D> members;

    /**
     * Inaccuracy introduced through use of this ensemble.
     * This property is mandatory.
     *
     * @see #getEnsembleAccuracy()
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
     * @param  memberType  the base type of datum members contained in this ensemble.
     * @throws ClassCastException if a member is not an instance of {@code memberType}.
     * @throws IllegalArgumentException if a member is an instance of {@link DatumEnsemble}, of if at least two
     *         different {@linkplain AbstractDatum#getConventionalRS() conventional reference systems} are found.
     *
     * @see #create(Map, Class, Collection, PositionalAccuracy)
     */
    protected DefaultDatumEnsemble(final Map<String,?> properties,
                                   final Collection<? extends D> members,
                                   final PositionalAccuracy accuracy,
                                   final Class<D> memberType)
    {
        super(properties);
        ArgumentChecks.ensureNonNull("accuracy", accuracy);
        ensembleAccuracy = accuracy;
        this.members = List.copyOf(members);
        validate(memberType);
    }

    /**
     * Creates a new ensemble with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  ensemble    the ensemble to copy.
     * @param  memberType  the base type of datum members contained in this ensemble.
     * @throws ClassCastException if a member is not an instance of {@code memberType}.
     * @throws IllegalArgumentException if a member is an instance of {@link DatumEnsemble}, of if at least two
     *         different {@linkplain AbstractDatum#getConventionalRS() conventional reference systems} are found.
     *
     * @see #castOrCopy(DatumEnsemble)
     */
    protected DefaultDatumEnsemble(final DatumEnsemble<? extends D> ensemble, final Class<D> memberType) {
        super(ensemble);
        members = List.copyOf(ensemble.getMembers());
        ensembleAccuracy = Objects.requireNonNull(ensemble.getEnsembleAccuracy());
        validate(memberType);
    }

    /**
     * Verifies this ensemble. All members shall be instances of the specified type and shall have
     * the same conventional reference system. No member can be an instance of {@link DatumEnsemble}.
     *
     * @param  memberType  the base type of datum members contained in this ensemble.
     * @throws ClassCastException if a member is not an instance of {@code memberType}.
     * @throws IllegalArgumentException if a member is an instance of {@link DatumEnsemble}, of if at least two
     *         different {@linkplain AbstractDatum#getConventionalRS() conventional reference systems} are found.
     */
    private void validate(final Class<D> memberType) {
        IdentifiedObject rs = null;
        for (final D datum : members) {
            if (datum instanceof DatumEnsemble<?>) {
                throw new IllegalArgumentException(
                        Errors.format(Errors.Keys.IllegalPropertyValueClass_2, "members", DatumEnsemble.class));
            }
            if (!memberType.isInstance(datum)) {
                throw new ClassCastException(
                        Errors.format(Errors.Keys.IllegalClass_2, memberType, Classes.getClass(datum)));
            }
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
     * Creates a datum ensemble from the given properties. The content of the {@code properties} map is described
     * {@linkplain #DefaultDatumEnsemble(Map, Collection, PositionalAccuracy, Class) in the constructor}.
     * The returned ensemble may implement the {@link GeodeticDatum}, {@link VerticalDatum}, {@link TemporalDatum},
     * {@link ParametricDatum} or {@link EngineeringDatum} interface if all members are instances of the same interface.
     *
     * @param  <D>         the type of datum members contained in the ensemble to create.
     * @param  properties  the properties to be given to the identified object.
     * @param  memberType  type of members, or {@code null} for automatic.
     * @param  members     datum or reference frames which are members of this ensemble.
     * @param  accuracy    inaccuracy introduced through use of this ensemble (mandatory).
     * @return the datum ensemble.
     * @throws IllegalArgumentException if a member is an instance of {@link DatumEnsemble}, of if at least two
     *         different {@linkplain AbstractDatum#getConventionalRS() conventional reference systems} are found.
     */
    public static <D extends Datum> DefaultDatumEnsemble<D> create(
            final Map<String,?> properties,
            final Class<D> memberType,
            final Collection<? extends D> members,
            final PositionalAccuracy accuracy)
    {
        return Factory.forMemberType(
                memberType != null ? memberType : Datum.class,
                null, properties, List.copyOf(members), accuracy);
    }

    /**
     * Returns a SIS ensemble implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise, if the given object is already an instance of
     *       {@code DefaultDatumEnsemble}, then it is returned unchanged.</li>
     *   <li>Otherwise, a new {@code DefaultDatumEnsemble} instance is created using the
     *       {@linkplain #DefaultDatumEnsemble(DatumEnsemble, Class) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation,
     *       because the other properties contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * The returned ensemble may implement the {@link GeodeticDatum}, {@link VerticalDatum}, {@link TemporalDatum},
     * {@link ParametricDatum} or {@link EngineeringDatum} interface if all members are instances of the same interface.
     *
     * @param  <D>     the type of datum members contained in the ensemble.
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static <D extends Datum> DefaultDatumEnsemble<D> castOrCopy(final DatumEnsemble<D> object) {
        if (object == null || object instanceof DefaultDatumEnsemble<?>) {
            return (DefaultDatumEnsemble<D>) object;
        }
        return Factory.forMemberType(Datum.class, object, null, List.copyOf(object.getMembers()), object.getEnsembleAccuracy());
    }

    /**
     * Returns this datum ensemble as a collection of datum of the given type.
     * This method casts the datum members, it does not copy or rewrite them.
     * However, the returned {@code DatumEnsemble} may be a different instance.
     *
     * @param  <N>         compile-time value of {@code memberType}.
     * @param  memberType  the new desired type of datum members.
     * @return an ensemble of datum of the given type.
     * @throws ClassCastException if at least one member is not an instance of the specified type.
     */
    public <N extends Datum> DefaultDatumEnsemble<N> cast(final Class<N> memberType) {
        for (final D member : members) {
            if (!memberType.isInstance(member)) {
                throw new ClassCastException(Errors.format(Errors.Keys.IllegalClass_2, memberType, Classes.getClass(member)));
            }
        }
        /*
         * At this point, we verified that all members are of the requested type.
         * Now verify if the ensemble as a whole is also of the requested type.
         * This part is not mandatory however.
         */
        @SuppressWarnings("unchecked")
        DefaultDatumEnsemble<N> ensemble = (DefaultDatumEnsemble<N>) this;
        if (!memberType.isInstance(ensemble)) {
            ensemble = Factory.forMemberType(memberType, ensemble, null, ensemble.members, ensemble.ensembleAccuracy);
        }
        return ensemble;
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

    /*
     * NOTE: a previous version provided the following method:
     *
     *     public Class<? super D> getMemberInterface() {
     *         return Datum.class;
     *     }
     *
     * It has been removed because, to be safe, it would require a `Class<D>` argument in the static methods.
     * Even the use of `? super D` wildcard is potentially unsafe if members implement two datum interfaces.
     * See the comment inside `Factory.forMemberType(…)` method body for more discussion.
     * We could use `? extends Datum`, but its usefulness is uncertain.
     */

    /**
     * Returns the datum or reference frames which are members of this ensemble.
     * The returned list is immutable.
     *
     * @return datum or reference frames which are members of this ensemble.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")     // Collection is unmodifiable.
    public final Collection<D> getMembers() {               // Must be final for type safety. See `forMemberType(…)`
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
     * Returns an estimation of positional accuracy in metres, or {@code NaN} if unknown.
     * If at least one {@linkplain org.apache.sis.metadata.iso.quality.DefaultQuantitativeResult quantitative
     * result} is found with a linear unit, then returns the largest result value converted to metres.
     *
     * @return the accuracy estimation (always in meters), or NaN if unknown.
     *
     * @see org.apache.sis.referencing.CRS#getLinearAccuracy(CoordinateOperation)
     */
    public double getLinearAccuracy() {
        return PositionalAccuracyConstant.getLinearAccuracy(Containers.singletonOrEmpty(getEnsembleAccuracy()));
    }

    /**
     * Returns an anchor definition which is common to all members of the datum ensemble.
     * If the value is not the same for all members, or if at least one member returned
     * an empty value, then this method returns an empty value.
     *
     * @return the common anchor definition, or empty if there is no common value.
     */
    @Override
    public Optional<InternationalString> getAnchorDefinition() {
        return getCommonOptionalValue(Datum::getAnchorDefinition);
    }

    /**
     * Returns an anchor epoch which is common to all members of the datum ensemble.
     * If the value is not the same for all members, or if at least one member returned
     * an empty value, then this method returns an empty value.
     *
     * @return the common anchor epoch, or empty if there is no common value.
     */
    @Override
    public Optional<Temporal> getAnchorEpoch() {
        return getCommonOptionalValue(Datum::getAnchorEpoch);
    }

    /**
     * Returns a publication date which is common to all members of the datum ensemble.
     * If the value is not the same for all members, or if at least one member returned
     * an empty value, then this method returns an empty value.
     *
     * @return the common publication date, or empty if there is no common value.
     */
    @Override
    public Optional<Temporal> getPublicationDate() {
        return getCommonOptionalValue(Datum::getPublicationDate);
    }

    /**
     * Returns a conventional reference system which is common to all members of the datum ensemble.
     * The returned value should never be empty, because it is illegal for a datum ensemble to have
     * members with different conventional reference system. If this case nevertheless happens,
     * then this method returns an empty value.
     *
     * @return the common conventional reference system, or empty if there is no common value.
     */
    @Override
    public Optional<IdentifiedObject> getConventionalRS() {
        return getCommonOptionalValue(Datum::getConventionalRS);
    }

    /**
     * Returns an optional value which is common to all ensemble members.
     * If all members do not have the same value, returns an empty value.
     *
     * @param  <V>     type of value.
     * @param  getter  method to invoke on each member for getting the value.
     * @return a value common to all members, or empty if there is no common value.
     */
    final <V> Optional<V> getCommonOptionalValue(final Function<D, Optional<V>> getter) {
        final Iterator<D> it = members.iterator();
check:  if (it.hasNext()) {
            final Optional<V> value = getter.apply(it.next());
            if (value.isPresent()) {
                while (it.hasNext()) {
                    if (!value.equals(getter.apply(it.next()))) {
                        break check;
                    }
                }
                return value;
            }
        }
        return Optional.empty();
    }

    /**
     * Returns a mandatory value which is common to all ensemble members.
     *
     * @param  <V>     type of value.
     * @param  getter  method to invoke on each member for getting the value.
     * @return a value common to all members.
     * @throws NoSuchElementException if the ensemble does not contain at least one member.
     * @throws GeodeticException if the value is not the same for all members of the datum ensemble.
     */
    final <V> V getCommonMandatoryValue(final Function<D, V> getter) {
        final Iterator<D> it = members.iterator();
        final V value = getter.apply(it.next());   // Mandatory.
        if (it.hasNext()) {
            final V other = getter.apply(it.next());
            if (!Objects.equals(value, other)) {
                throw new GeodeticException(Errors.format(Errors.Keys.NonUniformValue_2,
                        (value instanceof IdentifiedObject) ? IdentifiedObjects.getDisplayName((IdentifiedObject) value) : value,
                        (other instanceof IdentifiedObject) ? IdentifiedObjects.getDisplayName((IdentifiedObject) other) : other));
            }
        }
        return value;
    }

    /**
     * Returns {@code true} if either the {@linkplain #getName() primary name} or at least
     * one {@linkplain #getAlias() alias} matches the given string according heuristic rules.
     * This method performs the comparison documented in the
     * {@linkplain AbstractDatum#isHeuristicMatchForName(String) datum-class}.
     *
     * @param  name  the name to compare.
     * @return {@code true} if the primary name or at least one alias matches the specified {@code name}.
     */
    @Override
    public boolean isHeuristicMatchForName(final String name) {
        return NameToIdentifier.isHeuristicMatchForName(super.getName(), super.getAlias(), name, AbstractDatum.Simplifier.INSTANCE);
    }

    /**
     * Compares the specified object with this ensemble for equality.
     *
     * @param  object  the object to compare to {@code this}.
     * @param  mode    the strictness level of the comparison.
     * @return {@code true} if both objects are equal.
     *
     * @hidden because nothing new to said.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (!super.equals(object, mode)) {
            return false;
        }
        switch (mode) {
            case STRICT: {
                final var that = (DefaultDatumEnsemble<?>) object;
                return members.equals(that.members) && ensembleAccuracy.equals(that.ensembleAccuracy);
            }
            default: {
                final var that = (DatumEnsemble<?>) object;
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
     *
     * @hidden because nothing new to said.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        for (final Datum member : getMembers()) {
            formatter.newLine();
            formatter.appendFormattable(member, AbstractDatum::castOrCopy);
        }
        complete(formatter);
        formatter.newLine();
        WKTUtilities.appendElementIfPositive(WKTKeywords.EnsembleAccuracy, getLinearAccuracy(), formatter);
        formatter.newLine();
        if (!formatter.getConvention().supports(Convention.WKT2_2019)) {
            formatter.setInvalidWKT(this, null);
        }
        return WKTKeywords.Ensemble;
    }

    /**
     * Completes the <abbr>WKT</abbr> formatting with elements to insert after members and before accuracy.
     *
     * @param  formatter  the formatter where to format the inner content of this WKT element.
     */
    void complete(final Formatter formatter) {
    }

    /**
     * An ensemble viewed as a low-accuracy geodetic datum.
     *
     * @see #create(Map, Class, Collection, PositionalAccuracy)
     * @see #castOrCopy(DatumEnsemble)
     * @see DatumOrEnsemble#of(GeodeticCRS)
     * @see DatumOrEnsemble#asTargetDatum(GeodeticCRS, GeodeticCRS)
     */
    static final class Geodetic extends DefaultDatumEnsemble<GeodeticDatum> implements GeodeticDatum {
        /** For cross-versions compatibility. */
        private static final long serialVersionUID = 7669230365507661290L;

        /** Returns the given ensemble as a pseudo-datum. */
        static GeodeticDatum datum(DatumEnsemble<GeodeticDatum> ensemble) {
            return (ensemble == null || ensemble instanceof GeodeticDatum)
                    ? (GeodeticDatum) ensemble : new Geodetic(ensemble);
        }

        /** Creates a new ensemble as a copy of the given ensemble. */
        Geodetic(DatumEnsemble<? extends GeodeticDatum> ensemble) {
            super(ensemble, GeodeticDatum.class);
        }

        /** Creates a new ensemble from the given properties. */
        Geodetic(Map<String,?> properties, List<? extends GeodeticDatum> members, PositionalAccuracy accuracy) {
            super(properties, members, accuracy, GeodeticDatum.class);
        }

        /**
         * Returns the ellipsoid which is indirectly (through a datum) associated to this datum ensemble.
         * If all members of the ensemble use the same ellipsoid, then this method returns that ellipsoid.
         *
         * @return the ellipsoid indirectly associated to this datum ensemble.
         * @throws NoSuchElementException if the ensemble does not contain at least one member.
         * @throws GeodeticException if the ellipsoid is not the same for all members of the datum ensemble.
         */
        @Override
        public Ellipsoid getEllipsoid() {
            return getCommonMandatoryValue(GeodeticDatum::getEllipsoid);
        }

        /**
         * Returns the prime meridian which is indirectly (through a datum) associated to this datum ensemble.
         * If all members of the ensemble use the same prime meridian, then this method returns that meridian.
         *
         * @return the prime meridian indirectly associated to this datum ensemble.
         * @throws NoSuchElementException if the ensemble does not contain at least one member.
         * @throws GeodeticException if the prime meridian is not the same for all members of the datum ensemble.
         */
        @Override
        public PrimeMeridian getPrimeMeridian() {
            return getCommonMandatoryValue(GeodeticDatum::getPrimeMeridian);
        }

        /**
         * Completes the <abbr>WKT</abbr> formatting with elements to insert after members and before accuracy.
         * It includes the ellipsoid, but not the prime meridian which is formatted outside the ensemble.
         */
        @Override
        void complete(final Formatter formatter) {
            formatter.newLine();
            try {
                formatter.appendFormattable(getEllipsoid(), DefaultEllipsoid::castOrCopy);
            } catch (GeodeticException e) {
                formatter.setInvalidWKT(this, e);
            }
        }
    }

    /**
     * An ensemble viewed as a low-accuracy vertical datum.
     *
     * @see #create(Map, Class, Collection, PositionalAccuracy)
     * @see #castOrCopy(DatumEnsemble)
     * @see DatumOrEnsemble#of(VerticalCRS)
     * @see DatumOrEnsemble#asTargetDatum(VerticalCRS, VerticalCRS)
     */
    static final class Vertical extends DefaultDatumEnsemble<VerticalDatum> implements VerticalDatum {
        /** For cross-versions compatibility. */
        private static final long serialVersionUID = 7242417944400289818L;

        /** Returns the given ensemble as a pseudo-datum. */
        static VerticalDatum datum(DatumEnsemble<VerticalDatum> ensemble) {
            return (ensemble == null || ensemble instanceof VerticalDatum)
                    ? (VerticalDatum) ensemble : new Vertical(ensemble);
        }

        /** Creates a new ensemble as a copy of the given ensemble. */
        Vertical(DatumEnsemble<? extends VerticalDatum> ensemble) {
            super(ensemble, VerticalDatum.class);
        }

        /** Creates a new ensemble from the given properties. */
        Vertical(Map<String,?> properties, List<? extends VerticalDatum> members, PositionalAccuracy accuracy) {
            super(properties, members, accuracy, VerticalDatum.class);
        }

        /**
         * Returns a realization method which is common to all members of the datum ensemble.
         * If the value is not the same for all members, or if at least one member has an empty value,
         * then this method returns an empty value.
         *
         * @return the common realization method, or empty if there is no common value.
         */
        @Override
        public Optional<RealizationMethod> getRealizationMethod() {
            return getCommonOptionalValue(VerticalDatum::getRealizationMethod);
        }
    }

    /**
     * An ensemble viewed as a low-accuracy temporal datum.
     *
     * @see #create(Map, Class, Collection, PositionalAccuracy)
     * @see #castOrCopy(DatumEnsemble)
     * @see DatumOrEnsemble#of(TemporalCRS)
     * @see DatumOrEnsemble#asTargetDatum(TemporalCRS, TemporalCRS)
     */
    static final class Time extends DefaultDatumEnsemble<TemporalDatum> implements TemporalDatum {
        /** For cross-versions compatibility. */
        private static final long serialVersionUID = -4208563828181087035L;

        /** Returns the given ensemble as a pseudo-datum. */
        static TemporalDatum datum(DatumEnsemble<TemporalDatum> ensemble) {
            return (ensemble == null || ensemble instanceof TemporalDatum)
                    ? (TemporalDatum) ensemble : new Time(ensemble);
        }

        /** Creates a new ensemble as a copy of the given ensemble. */
        Time(DatumEnsemble<? extends TemporalDatum> ensemble) {
            super(ensemble, TemporalDatum.class);
        }

        /** Creates a new ensemble from the given properties. */
        Time(Map<String,?> properties, List<? extends TemporalDatum> members, PositionalAccuracy accuracy) {
            super(properties, members, accuracy, TemporalDatum.class);
        }

        /**
         * Returns the temporal origin which is indirectly (through a datum) associated to this datum ensemble.
         * If all members of the ensemble use the same temporal origin, then this method returns that origin.
         *
         * @return the temporal origin indirectly associated to this datum ensemble.
         * @throws NoSuchElementException if the ensemble does not contain at least one member.
         * @throws GeodeticException if the temporal origin is not the same for all members of the datum ensemble.
         */
        @Override
        public Temporal getOrigin() {
            return getCommonMandatoryValue(TemporalDatum::getOrigin);
        }
    }

    /**
     * An ensemble viewed as a low-accuracy parametric datum.
     *
     * @see #create(Map, Class, Collection, PositionalAccuracy)
     * @see #castOrCopy(DatumEnsemble)
     * @see DatumOrEnsemble#of(ParametricCRS)
     * @see DatumOrEnsemble#asTargetDatum(ParametricCRS, ParametricCRS)
     */
    static final class Parametric extends DefaultDatumEnsemble<ParametricDatum> implements ParametricDatum {
        /** For cross-versions compatibility. */
        private static final long serialVersionUID = -8277774591738789437L;

        /** Returns the given ensemble as a pseudo-datum. */
        static ParametricDatum datum(DatumEnsemble<ParametricDatum> ensemble) {
            return (ensemble == null || ensemble instanceof ParametricDatum)
                    ? (ParametricDatum) ensemble : new Parametric(ensemble);
        }

        /** Creates a new ensemble as a copy of the given ensemble. */
        Parametric(DatumEnsemble<? extends ParametricDatum> ensemble) {
            super(ensemble, ParametricDatum.class);
        }

        /** Creates a new ensemble from the given properties. */
        Parametric(Map<String,?> properties, List<? extends ParametricDatum> members, PositionalAccuracy accuracy) {
            super(properties, members, accuracy, ParametricDatum.class);
        }
    }

    /**
     * An ensemble viewed as a low-accuracy engineering datum.
     *
     * @see #create(Map, Class, Collection, PositionalAccuracy)
     * @see #castOrCopy(DatumEnsemble)
     * @see DatumOrEnsemble#of(EngineeringCRS)
     * @see DatumOrEnsemble#asTargetDatum(EngineeringCRS, EngineeringCRS)
     */
    static final class Engineering extends DefaultDatumEnsemble<EngineeringDatum> implements EngineeringDatum {
        /** For cross-versions compatibility. */
        private static final long serialVersionUID = -8978468990963666861L;

        /** Returns the given ensemble as a pseudo-datum. */
        static EngineeringDatum datum(DatumEnsemble<EngineeringDatum> ensemble) {
            return (ensemble == null || ensemble instanceof EngineeringDatum)
                    ? (EngineeringDatum) ensemble : new Engineering(ensemble);
        }

        /** Creates a new ensemble as a copy of the given ensemble. */
        Engineering(DatumEnsemble<? extends EngineeringDatum> ensemble) {
            super(ensemble, EngineeringDatum.class);
        }

        /** Creates a new ensemble from the given properties. */
        Engineering(Map<String,?> properties, List<? extends EngineeringDatum> members, PositionalAccuracy accuracy) {
            super(properties, members, accuracy, EngineeringDatum.class);
        }
    }

    /**
     * Factory methods for finding the base type of all members of a list and instantiate
     * the {@link DefaultDatumEnsemble} subclass corresponding to the type of all members.
     * Each instance of {@code Factory} is immutable and thread-safe.
     * There is one instance for each supported type.
     *
     * @param  <D>  base type of all members in the ensembles constructed by this factory instance.
     *
     * @see #create(Map, Class, Collection, PositionalAccuracy)
     * @see #castOrCopy(DatumEnsemble)
     */
    private static abstract class Factory<D extends Datum> {
        /**
         * Base type of all members in the ensembles constructed by this factory instance.
         */
        private final Class<D> memberType;

        /**
         * Creates a new factory for ensembles in which all members are instances of the given type.
         *
         * @param  memberType  base type of all members in the ensembles constructed by this factory instance.
         */
        private Factory(final Class<D> memberType) {
            this.memberType = memberType;
        }

        /**
         * Finds a common type for all members in the given list, then creates a datum ensemble of that type.
         * In principle, the {@code <D>} type should be restricted to one of the types hard-coded in this class.
         * However, it is okay if {@code <D>} is a custom subclass because it appears only in the following places:
         *
         * <ul>
         *   <li>{@link #getMembers()}, which is a read-only collection (it is safe to cast {@code List<? extends D>}
         *       as {@code List<D>} when no write operation is allowed). That method is made final for enforcing this
         *       assumption.</li>
         * </ul>
         *
         * Exactly one of {@code object} and {@code properties} should be non-null.
         *
         * @param  <D>          base type of all members in the ensembles to create.
         * @param  memberType   the base class of datum type to take in account.
         * @param  object       the source ensemble to copy, or {@code null} if none.
         * @param  properties   the properties of the ensemble to create, or {@code null}.
         * @param  members      members of the ensemble to copy or create.
         * @param  accuracy     inaccuracy of the ensemble. Mandatory if {@code properties} is non-null.
         * @return the copied or created ensemble.
         * @throws ClassCastException if at least one member is not a {@link Datum}.
         *         Should never happen if the parameterized type of {@code members} is respected.
         */
        static <D extends Datum> DefaultDatumEnsemble<D> forMemberType(
                final Class<? extends Datum> memberType,
                final DatumEnsemble<? extends D> object,
                final Map<String,?> properties,
                final List<? extends D> members,
                final PositionalAccuracy accuracy)
        {
            Object illegal = null;
nextType:   for (final Factory<?> factory : FACTORIES) {
                if (memberType.isAssignableFrom(factory.memberType)) {
                    for (final Object member : members) {
                        if (!factory.memberType.isInstance(member)) {
                            illegal = member;
                            continue nextType;
                        }
                    }
                    /*
                     * A more correct type would be `Factory<? super D>` because of the use of `isInstance(member)`
                     * instead of strict class equality. However, even `? super D` is not guaranteed to be correct,
                     * because nothing prevent a member from implementing two interfaces. In such case, the type of
                     * the first matching factory could be unrelated to `D` (e.g., `D` is `ParametricDatum` but all
                     * members also implement `VerticalDatum`). However, despite this uncertainty, the cast is okay
                     * because `D` appears only in `getMembers()` which returns a read-only collection. There is no
                     * method returning `Class<D>` and no guarantees that the returned object will implement `D`.
                     */
                    @SuppressWarnings("unchecked")
                    final var selected = (Factory<D>) factory;      // See above comment.
                    if (object != null) {
                        return selected.copy(object);
                    } else {
                        return selected.create(properties, members, accuracy);
                    }
                }
            }
            throw new ClassCastException(Errors.format(Errors.Keys.IllegalClass_2, memberType, Classes.getClass(illegal)));
        }

        /**
         * Creates a new ensemble of the type associated with this factory instance.
         *
         * @param  properties  the properties to be given to the identified object.
         * @param  members     datum or reference frames which are members of this ensemble.
         * @param  accuracy    inaccuracy introduced through use of this ensemble (mandatory).
         * @return the datum ensemble.
         * @throws IllegalArgumentException if a member is an instance of {@link DatumEnsemble}, of if at least two
         *         different {@linkplain AbstractDatum#getConventionalRS() conventional reference systems} are found.
         *
         * @see #create(Map, Class, Collection, PositionalAccuracy)
         */
        abstract DefaultDatumEnsemble<D> create(Map<String,?> properties, List<? extends D> members, PositionalAccuracy accuracy);

        /**
         * Creates a new ensemble with the same values as the specified one.
         *
         * @param  ensemble  the ensemble to copy.
         * @throws IllegalArgumentException if a member is an instance of {@link DatumEnsemble}, of if at least two
         *         different {@linkplain AbstractDatum#getConventionalRS() conventional reference systems} are found.
         *
         * @see #castOrCopy(DatumEnsemble)
         */
        abstract DefaultDatumEnsemble<D> copy(DatumEnsemble<? extends D> object);

        /**
         * Factories for all datum types supported by this class. The types are (in order) {@link GeodeticDatum},
         * {@link VerticalDatum}, {@link TemporalDatum}, {@link ParametricDatum} and {@link EngineeringDatum}.
         * The types are tested in iteration order. For example, if a member implements both {@code VerticalDatum}
         * {@code ParametricDatum} interface, then {@code VerticalDatum} has precedence.
         */
        private static final Factory<?>[] FACTORIES = {
            new Factory<GeodeticDatum>(GeodeticDatum.class) {
                @Override
                DefaultDatumEnsemble<GeodeticDatum> copy(DatumEnsemble<? extends GeodeticDatum> object) {
                    return new Geodetic(object);
                }

                @Override
                DefaultDatumEnsemble<GeodeticDatum> create(Map<String,?> properties,
                        List<? extends GeodeticDatum> members, PositionalAccuracy accuracy)
                {
                    return new Geodetic(properties, members, accuracy);
                }
            },

            new Factory<VerticalDatum>(VerticalDatum.class) {
                @Override
                DefaultDatumEnsemble<VerticalDatum> copy(DatumEnsemble<? extends VerticalDatum> object) {
                    return new Vertical(object);
                }

                @Override
                DefaultDatumEnsemble<VerticalDatum> create(Map<String,?> properties,
                        List<? extends VerticalDatum> members, PositionalAccuracy accuracy)
                {
                    return new Vertical(properties, members, accuracy);
                }
            },

            new Factory<TemporalDatum>(TemporalDatum.class) {
                @Override
                DefaultDatumEnsemble<TemporalDatum> copy(DatumEnsemble<? extends TemporalDatum> object) {
                    return new Time(object);
                }

                @Override
                DefaultDatumEnsemble<TemporalDatum> create(Map<String,?> properties,
                        List<? extends TemporalDatum> members, PositionalAccuracy accuracy)
                {
                    return new Time(properties, members, accuracy);
                }
            },

            new Factory<ParametricDatum>(ParametricDatum.class) {
                @Override
                DefaultDatumEnsemble<ParametricDatum> copy(DatumEnsemble<? extends ParametricDatum> object) {
                    return new Parametric(object);
                }

                @Override
                DefaultDatumEnsemble<ParametricDatum> create(Map<String,?> properties,
                        List<? extends ParametricDatum> members, PositionalAccuracy accuracy)
                {
                    return new Parametric(properties, members, accuracy);
                }
            },

            new Factory<EngineeringDatum>(EngineeringDatum.class) {
                @Override
                DefaultDatumEnsemble<EngineeringDatum> copy(DatumEnsemble<? extends EngineeringDatum> object) {
                    return new Engineering(object);
                }

                @Override
                DefaultDatumEnsemble<EngineeringDatum> create(Map<String,?> properties,
                        List<? extends EngineeringDatum> members, PositionalAccuracy accuracy)
                {
                    return new Engineering(properties, members, accuracy);
                }
            },

            new Factory<Datum>(Datum.class) {
                @Override
                DefaultDatumEnsemble<Datum> copy(DatumEnsemble<? extends Datum> object) {
                    return new DefaultDatumEnsemble<>(object, Datum.class);
                }

                @Override
                DefaultDatumEnsemble<Datum> create(Map<String,?> properties,
                        List<? extends Datum> members, PositionalAccuracy accuracy)
                {
                    return new DefaultDatumEnsemble<>(properties, members, accuracy, Datum.class);
                }
            }
        };
    }
}
