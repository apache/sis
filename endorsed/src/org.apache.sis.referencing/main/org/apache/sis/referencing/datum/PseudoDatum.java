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

import java.util.Set;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.time.temporal.Temporal;
import java.io.Serializable;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ObjectDomain;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.crs.*;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.GeodeticException;


/**
 * A datum ensemble viewed as if it was a single datum for the sake of simplicity.
 * This pseudo-datum is a non-standard mechanism used by the Apache <abbr>SIS</abbr> implementation
 * for handling datum and datum ensemble in a uniform way. For example, {@code PseudoDatum.of(crs)}
 * allows to {@linkplain IdentifiedObjects#isHeuristicMatchForName compare the datum name} without
 * the need to check which one of the {@code getDatum()} or {@code getDatumEnsemble()} methods
 * returns a non-null value.
 *
 * <p>{@code PseudoDatum} instances should live only for a short time.
 * They should not be stored as {@link SingleCRS} properties.
 * If a {@code PseudoDatum} instances is given to the constructor of an Apache <abbr>SIS</abbr> class,
 * the constructor will automatically unwraps the {@linkplain #ensemble}.</p>
 *
 * <h2>Default method implementations</h2>
 * Unless otherwise specified in the Javadoc, all methods in this class delegate
 * to the same method in the wrapper datum {@linkplain #ensemble}.
 *
 * <h2>Object comparisons</h2>
 * The {@link #equals(Object)} method returns {@code true} only if the two compared objects are instances
 * of the same class, which implies that the {@code Object} argument must be a {@code PseudoDatum}.
 * The {@link #equals(Object, ComparisonMode)} method with a non-strict comparison mode compares
 * the wrapped datum ensemble, which implies that:
 *
 * <ul>
 *   <li>A pseudo-datum is never equal to a real datum, regardless the names and identifiers of the compared objects.</li>
 *   <li>A pseudo-datum can be equal to another pseudo-datum or to a datum ensemble.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @param <D> the type of datum contained in this ensemble.
 *
 * @since 1.5
 */
public abstract class PseudoDatum<D extends Datum> implements Datum, LenientComparable, Serializable {
    /**
     * For cross-versions compatibility.
     */
    private static final long serialVersionUID = 3889895625961827486L;

    /**
     * The datum ensemble wrapped by this pseudo-datum.
     */
    @SuppressWarnings("serial")     // Most SIS implementations are serializable.
    public final DatumEnsemble<D> ensemble;

    /**
     * Creates a new pseudo-datum.
     *
     * @param ensemble the datum ensemble wrapped by this pseudo-datum.
     */
    protected PseudoDatum(final DatumEnsemble<D> ensemble) {
        this.ensemble = Objects.requireNonNull(ensemble);
    }

    /**
     * Returns the datum of the given <abbr>CRS</abbr> if presents, or the datum ensemble otherwise.
     * This is an alternative to the {@code of(…)} methods when the caller does not need to view the
     * object as a datum.
     *
     * @param  crs  the <abbr>CRS</abbr> from which to get the datum or ensemble, or {@code null}.
     * @return the datum if present, or the datum ensemble otherwise, or {@code null}.
     */
    public static IdentifiedObject getDatumOrEnsemble(final SingleCRS crs) {
        if (crs == null) return null;
        final Datum datum = crs.getDatum();
        if (datum != null) {
            if (datum instanceof PseudoDatum<?>) {
                return ((PseudoDatum) datum).ensemble;
            }
            return datum;
        }
        return crs.getDatumEnsemble();
    }

    /**
     * Returns the datum or pseudo-datum of the given geodetic <abbr>CRS</abbr>.
     * If the given <abbr>CRS</abbr> is associated to a non-null datum, then this method returns that datum.
     * Otherwise, this method returns the <abbr>CRS</abbr> datum ensemble wrapped in a pseudo-datum.
     * In the latter case, the pseudo-datum implementations of the {@link GeodeticDatum#getEllipsoid()}
     * and {@link GeodeticDatum#getPrimeMeridian()} methods expect an ellipsoid or prime meridian which
     * is the same for all {@linkplain #ensemble} members.
     * If this condition does not hold, a {@link GeodeticException} will be thrown.
     *
     * @param  crs  the coordinate reference system for which to get the datum or datum ensemble.
     * @return the datum or pseudo-datum of the given <abbr>CRS</abbr>.
     * @throws NullPointerException if the given argument is {@code null},
     *         or if both the datum and datum ensemble are null.
     */
    public static GeodeticDatum of(final GeodeticCRS crs) {
        GeodeticDatum datum = crs.getDatum();
        if (datum == null) {
            datum = new PseudoDatum.Geodetic(crs.getDatumEnsemble());
        }
        return datum;
    }

    /**
     * Returns the datum or pseudo-datum of the given vertical <abbr>CRS</abbr>.
     * If the given <abbr>CRS</abbr> is associated to a non-null datum, then this method returns that datum.
     * Otherwise, this method returns the <abbr>CRS</abbr> datum ensemble wrapped in a pseudo-datum.
     *
     * @param  crs  the coordinate reference system for which to get the datum or datum ensemble.
     * @return the datum or pseudo-datum of the given <abbr>CRS</abbr>.
     * @throws NullPointerException if the given argument is {@code null},
     *         or if both the datum and datum ensemble are null.
     */
    public static VerticalDatum of(final VerticalCRS crs) {
        VerticalDatum datum = crs.getDatum();
        if (datum == null) {
            datum = new PseudoDatum.Vertical(crs.getDatumEnsemble());
        }
        return datum;
    }

    /**
     * Returns the datum or pseudo-datum of the given temporal <abbr>CRS</abbr>.
     * If the given <abbr>CRS</abbr> is associated to a non-null datum, then this method returns that datum.
     * Otherwise, this method returns the <abbr>CRS</abbr> datum ensemble wrapped in a pseudo-datum.
     * In the latter case, the pseudo-datum implementations of the {@link TemporalDatum#getOrigin()}
     * expects a temporal origin which is the same for all {@linkplain #ensemble} members.
     * If this condition does not hold, a {@link GeodeticException} will be thrown.
     *
     * @param  crs  the coordinate reference system for which to get the datum or datum ensemble.
     * @return the datum or pseudo-datum of the given <abbr>CRS</abbr>.
     * @throws NullPointerException if the given argument is {@code null},
     *         or if both the datum and datum ensemble are null.
     */
    public static TemporalDatum of(final TemporalCRS crs) {
        TemporalDatum datum = crs.getDatum();
        if (datum == null) {
            datum = new PseudoDatum.Time(crs.getDatumEnsemble());
        }
        return datum;
    }

    /**
     * Returns the datum or pseudo-datum of the given parametric <abbr>CRS</abbr>.
     * If the given <abbr>CRS</abbr> is associated to a non-null datum, then this method returns that datum.
     * Otherwise, this method returns the <abbr>CRS</abbr> datum ensemble wrapped in a pseudo-datum.
     *
     * @param  crs  the coordinate reference system for which to get the datum or datum ensemble.
     * @return the datum or pseudo-datum of the given <abbr>CRS</abbr>.
     * @throws NullPointerException if the given argument is {@code null},
     *         or if both the datum and datum ensemble are null.
     */
    public static ParametricDatum of(final ParametricCRS crs) {
        ParametricDatum datum = crs.getDatum();
        if (datum == null) {
            datum = new PseudoDatum.Parametric(crs.getDatumEnsemble());
        }
        return datum;
    }

    /**
     * Returns the datum or pseudo-datum of the given engineering <abbr>CRS</abbr>.
     * If the given <abbr>CRS</abbr> is associated to a non-null datum, then this method returns that datum.
     * Otherwise, this method returns the <abbr>CRS</abbr> datum ensemble wrapped in a pseudo-datum.
     *
     * @param  crs  the coordinate reference system for which to get the datum or datum ensemble.
     * @return the datum or pseudo-datum of the given <abbr>CRS</abbr>.
     * @throws NullPointerException if the given argument is {@code null},
     *         or if both the datum and datum ensemble are null.
     */
    public static EngineeringDatum of(final EngineeringCRS crs) {
        EngineeringDatum datum = crs.getDatum();
        if (datum == null) {
            datum = new PseudoDatum.Engineering(crs.getDatumEnsemble());
        }
        return datum;
    }

    /**
     * Returns the GeoAPI interface of the ensemble members.
     * It should also be the interface implemented by this class.
     *
     * @return the GeoAPI interface of the ensemble members.
     */
    public abstract Class<D> getInterface();

    /**
     * Returns the primary name by which the datum ensemble is identified.
     *
     * @return {@code ensemble.getName()}.
     * @hidden
     */
    @Override
    public Identifier getName() {
        return ensemble.getName();
    }

    /**
     * Returns alternative names by which the datum ensemble is identified.
     *
     * @return {@code ensemble.getAlias()}.
     * @hidden
     */
    @Override
    public Collection<GenericName> getAlias() {
        return ensemble.getAlias();
    }

    /**
     * Returns an identifier which references elsewhere the datum ensemble information.
     *
     * @return {@code ensemble.getIdentifiers()}.
     * @hidden
     */
    @Override
    public Set<Identifier> getIdentifiers() {
        return ensemble.getIdentifiers();
    }

    /**
     * Returns the usage of the datum ensemble.
     *
     * @return {@code ensemble.getDomains()}.
     * @hidden
     */
    @Override
    public Collection<ObjectDomain> getDomains() {
        return ensemble.getDomains();
    }

    /**
     * Returns an anchor definition which is common to all members of the datum ensemble.
     * If the value is not the same for all members (including the case where a member
     * has an empty value), then this method returns an empty value.
     *
     * @return the common anchor definition, or empty if there is no common value.
     */
    @Override
    public Optional<InternationalString> getAnchorDefinition() {
        return getCommonOptionalValue(Datum::getAnchorDefinition);
    }

    /**
     * Returns an anchor epoch which is common to all members of the datum ensemble.
     * If the value is not the same for all members (including the case where a member
     * has an empty value), then this method returns an empty value.
     *
     * @return the common anchor epoch, or empty if there is no common value.
     */
    @Override
    public Optional<Temporal> getAnchorEpoch() {
        return getCommonOptionalValue(Datum::getAnchorEpoch);
    }

    /**
     * Returns a publication date which is common to all members of the datum ensemble.
     * If the value is not the same for all members (including the case where a member
     * has an empty value), then this method returns an empty value.
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
     * this method returns an empty value.
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
     * @return a value common to all members, or {@code null} if none.
     */
    final <V> Optional<V> getCommonOptionalValue(final Function<D, Optional<V>> getter) {
        final Iterator<D> it = ensemble.getMembers().iterator();
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
        final Iterator<D> it = ensemble.getMembers().iterator();
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
     * Returns comments on or information about the datum ensemble.
     *
     * @return {@code ensemble.getRemarks()}.
     * @hidden
     */
    @Override
    public Optional<InternationalString> getRemarks() {
        return ensemble.getRemarks();
    }

    /**
     * Formats a <i>Well-Known Text</i> (WKT) for the datum ensemble.
     *
     * @return {@code ensemble.toWKT()}.
     * @hidden
     */
    @Override
    public String toWKT() {
        return ensemble.toWKT();
    }

    /**
     * Returns a string representation of the datum ensemble.
     *
     * @return {@code ensemble.toString()}.
     * @hidden
     */
    @Override
    public String toString() {
        return ensemble.toString();
    }

    /**
     * Returns a hash-code value of this pseudo-datum.
     *
     * @return a hash-code value of this pseudo-datum.
     */
    @Override
    public int hashCode() {
        return ensemble.hashCode() ^ getClass().hashCode();
    }

    /**
     * Compares this pseudo-datum to the given object for equality.
     * The two objects are equal if they are of the same classes and
     * the wrapped {@link #ensemble} are equal.
     *
     * @param  other  the object to compare with this pseudo-datum.
     * @return whether the two objects are equal.
     */
    @Override
    public boolean equals(final Object other) {
        return (other != null) && other.getClass() == getClass() && ensemble.equals(((PseudoDatum<?>) other).ensemble);
    }

    /**
     * Compares this object with the given object for equality.
     * If the comparison mode is strict, then this method delegates to {@link #equals(Object)}.
     * Otherwise, this method unwrap the ensembles, then compare the ensembles.
     *
     * @param  other  the object to compare to {@code this}.
     * @param  mode   the strictness level of the comparison.
     * @return {@code true} if both objects are equal according the given comparison mode.
     */
    @Override
    public boolean equals(Object other, final ComparisonMode mode) {
        if (mode == ComparisonMode.STRICT) {
            return equals(other);
        }
        if (other instanceof PseudoDatum<?>) {
            other = ((PseudoDatum<?>) other).ensemble;
        }
        return Utilities.deepEquals(ensemble, other, mode);
    }

    /**
     * A pseudo-datum for an ensemble of geodetic datum.
     */
    private static final class Geodetic extends PseudoDatum<GeodeticDatum> implements GeodeticDatum {
        /** For cross-versions compatibility. */
        private static final long serialVersionUID = 7669230365507661290L;

        /** Creates a new pseudo-datum wrapping the given ensemble. */
        Geodetic(final DatumEnsemble<GeodeticDatum> ensemble) {
            super(ensemble);
        }

        /**
         * Returns the GeoAPI interface implemented by this pseudo-datum.
         */
        @Override
        public Class<GeodeticDatum> getInterface() {
            return GeodeticDatum.class;
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
    }

    /**
     * A pseudo-datum for an ensemble of vertical datum.
     */
    private static final class Vertical extends PseudoDatum<VerticalDatum> implements VerticalDatum {
        /** For cross-versions compatibility. */
        private static final long serialVersionUID = 7242417944400289818L;

        /** Creates a new pseudo-datum wrapping the given ensemble. */
        Vertical(final DatumEnsemble<VerticalDatum> ensemble) {
            super(ensemble);
        }

        /**
         * Returns the GeoAPI interface implemented by this pseudo-datum.
         */
        @Override
        public Class<VerticalDatum> getInterface() {
            return VerticalDatum.class;
        }

        /**
         * Returns a realization method which is common to all members of the datum ensemble.
         * If the value is not the same for all members (including the case where a member
         * has an empty value), then this method returns an empty value.
         *
         * @return the common realization method, or empty if there is no common value.
         */
        @Override
        public Optional<RealizationMethod> getRealizationMethod() {
            return getCommonOptionalValue(VerticalDatum::getRealizationMethod);
        }
    }

    /**
     * A pseudo-datum for an ensemble of temporal datum.
     */
    private static final class Time extends PseudoDatum<TemporalDatum> implements TemporalDatum {
        /** For cross-versions compatibility. */
        private static final long serialVersionUID = -4208563828181087035L;

        /** Creates a new pseudo-datum wrapping the given ensemble. */
        Time(final DatumEnsemble<TemporalDatum> ensemble) {
            super(ensemble);
        }

        /**
         * Returns the GeoAPI interface implemented by this pseudo-datum.
         */
        @Override
        public Class<TemporalDatum> getInterface() {
            return TemporalDatum.class;
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
     * A pseudo-datum for an ensemble of parametric datum.
     */
    private static final class Parametric extends PseudoDatum<ParametricDatum> implements ParametricDatum {
        /** For cross-versions compatibility. */
        private static final long serialVersionUID = -8277774591738789437L;

        /** Creates a new pseudo-datum wrapping the given ensemble. */
        Parametric(final DatumEnsemble<ParametricDatum> ensemble) {
            super(ensemble);
        }

        /** Returns the GeoAPI interface implemented by this pseudo-datum. */
        @Override public Class<ParametricDatum> getInterface() {
            return ParametricDatum.class;
        }
    }

    /**
     * A pseudo-datum for an ensemble of engineering datum.
     */
    private static final class Engineering extends PseudoDatum<EngineeringDatum> implements EngineeringDatum {
        /** For cross-versions compatibility. */
        private static final long serialVersionUID = -8978468990963666861L;

        /** Creates a new pseudo-datum wrapping the given ensemble. */
        Engineering(final DatumEnsemble<EngineeringDatum> ensemble) {
            super(ensemble);
        }

        /** Returns the GeoAPI interface implemented by this pseudo-datum. */
        @Override public Class<EngineeringDatum> getInterface() {
            return EngineeringDatum.class;
        }
    }
}
