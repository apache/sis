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

import java.util.ArrayDeque;
import java.util.Set;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.io.Serializable;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.crs.*;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.GeodeticException;

// Specific to the main and geoapi-3.1 branches:
import java.util.Date;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.metadata.extent.Extent;

// Specific to the main branch:
import static org.apache.sis.pending.geoapi.referencing.MissingMethods.getDatumEnsemble;


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
    public final DefaultDatumEnsemble<D> ensemble;

    /**
     * Creates a new pseudo-datum.
     *
     * @param ensemble the datum ensemble wrapped by this pseudo-datum.
     */
    protected PseudoDatum(final DefaultDatumEnsemble<D> ensemble) {
        this.ensemble = Objects.requireNonNull(ensemble);
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
            datum = new PseudoDatum.Geodetic(getDatumEnsemble(crs));
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
            datum = new PseudoDatum.Vertical(getDatumEnsemble(crs));
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
            datum = new PseudoDatum.Time(getDatumEnsemble(crs));
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
            datum = new PseudoDatum.Engineering(getDatumEnsemble(crs));
        }
        return datum;
    }

    /**
     * Returns the datum or pseudo-datum of the result of an operation between the given geodetic <abbr>CRS</abbr>s.
     * If the two given coordinate reference systems are associated to the same datum, then this method returns
     * the <var>target</var> datum. Otherwise, this method returns a pseudo-datum for the largest ensemble which
     * fully contains the datum or datum ensemble of the other <abbr>CRS</abbr>. If none of the <var>source</var>
     * or <var>target</var> datum ensembles met that criterion, then this method returns an empty value.
     * A non-empty value means that it is okay, for low accuracy requirements, to ignore the datum shift.
     *
     * @param  source  the source <abbr>CRS</abbr> of a coordinate operation.
     * @param  target  the target <abbr>CRS</abbr> of a coordinate operation.
     * @return datum or pseudo-datum of the coordinate operation result if it is okay to ignore datum shift.
     */
    public static Optional<GeodeticDatum> ofOperation(final GeodeticCRS source, final GeodeticCRS target) {
        return ofOperation(source, source.getDatum(),
                           target, target.getDatum(),
                           Geodetic::new);
    }

    /**
     * Returns the datum or pseudo-datum of the result of an operation between the given vertical <abbr>CRS</abbr>s.
     * See {@link #ofOperation(GeodeticCRS, GeodeticCRS)} for more information.
     *
     * @param  source  the source <abbr>CRS</abbr> of a coordinate operation.
     * @param  target  the target <abbr>CRS</abbr> of a coordinate operation.
     * @return datum or pseudo-datum of the coordinate operation result if it is okay to ignore datum shift.
     */
    public static Optional<VerticalDatum> ofOperation(final VerticalCRS source, final VerticalCRS target) {
        return ofOperation(source, source.getDatum(),
                           target, target.getDatum(),
                           Vertical::new);
    }

    /**
     * Returns the datum or pseudo-datum of the result of an operation between the given temporal <abbr>CRS</abbr>s.
     * See {@link #ofOperation(GeodeticCRS, GeodeticCRS)} for more information.
     *
     * @param  source  the source <abbr>CRS</abbr> of a coordinate operation.
     * @param  target  the target <abbr>CRS</abbr> of a coordinate operation.
     * @return datum or pseudo-datum of the coordinate operation result if it is okay to ignore datum shift.
     */
    public static Optional<TemporalDatum> ofOperation(final TemporalCRS source, final TemporalCRS target) {
        return ofOperation(source, source.getDatum(),
                           target, target.getDatum(),
                           Time::new);
    }

    /**
     * Returns the datum or pseudo-datum of the result of an operation between the given engineering <abbr>CRS</abbr>s.
     * See {@link #ofOperation(GeodeticCRS, GeodeticCRS)} for more information.
     *
     * @param  source  the source <abbr>CRS</abbr> of a coordinate operation.
     * @param  target  the target <abbr>CRS</abbr> of a coordinate operation.
     * @return datum or pseudo-datum of the coordinate operation result if it is okay to ignore datum shift.
     */
    public static Optional<EngineeringDatum> ofOperation(final EngineeringCRS source, final EngineeringCRS target) {
        return ofOperation(source, source.getDatum(),
                           target, target.getDatum(),
                           Engineering::new);
    }

    /**
     * Returns the datum or pseudo-datum of a coordinate operation from <var>source</var> to <var>target</var>.
     * If the two given coordinate reference systems are associated to the same datum, then this method returns
     * the <var>target</var> datum. Otherwise, this method returns a pseudo-datum for the largest ensemble which
     * fully contains the datum or datum ensemble of the other <abbr>CRS</abbr>. If none of the <var>source</var>
     * or <var>target</var> datum ensembles met that criterion, then this method returns an empty value.
     * A non-empty value means that it is okay, for low accuracy requirements, to ignore the datum shift.
     *
     * @param  source       the source <abbr>CRS</abbr> of a coordinate operation.
     * @param  sourceDatum  the datum of the source <abbr>CRS</abbr>.
     * @param  target       the target <abbr>CRS</abbr> of a coordinate operation.
     * @param  targetDatum  the datum of the target <abbr>CRS</abbr>.
     * @param  constructor  function to invoke for wrapping a datum ensemble in a pseudo-datum.
     * @return datum or pseudo-datum of the coordinate operation result if it is okay to ignore datum shift.
     */
    @SuppressWarnings("unchecked")          // Casts are safe because callers know the method signature of <D>.
    private static <C extends SingleCRS, D extends Datum, R extends IdentifiedObject> Optional<R> ofOperation(
            final C source, final R sourceDatum,
            final C target, final R targetDatum,
            final Function<DefaultDatumEnsemble<D>, R> constructor)
    {
        if (sourceDatum != null && Utilities.equalsIgnoreMetadata(sourceDatum, targetDatum)) {
            return Optional.of(targetDatum);
        }
        DefaultDatumEnsemble<D> sourceEnsemble;
        DefaultDatumEnsemble<D> targetEnsemble;
        DefaultDatumEnsemble<D> selected;
        if ((isMember(selected = targetEnsemble = (DefaultDatumEnsemble<D>) getDatumEnsemble(target), sourceDatum)) ||
            (isMember(selected = sourceEnsemble = (DefaultDatumEnsemble<D>) getDatumEnsemble(source), targetDatum)))
        {
            return Optional.of(constructor.apply(selected));
        }
        if (sourceEnsemble != null && targetEnsemble != null) {
            selected = targetEnsemble;
            Collection<D> large = targetEnsemble.getMembers();
            Collection<D> small = sourceEnsemble.getMembers();
            if (small.size() > large.size()) {
                selected = sourceEnsemble;
                var t = large;
                large = small;
                small = t;
            }
            small = new ArrayDeque<>(small);
            for (final Datum member : large) {
                final Iterator<D> it = small.iterator();
                while (it.hasNext()) {
                    if (Utilities.equalsIgnoreMetadata(member, it.next())) {
                        it.remove();
                        if (small.isEmpty()) {
                            /*
                             * Found all members of the smaller ensemble. Take the larger ensemble,
                             * as it contains both ensembles and should have conservative accuracy.
                             */
                            return Optional.of(constructor.apply(selected));
                        }
                        break;      // For removing only the first match.
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns whether the given datum is a member of the given ensemble.
     *
     * @param  datum     the datum to test, or {@code null}.
     * @param  ensemble  the ensemble to test, or {@code null}.
     * @return whether the ensemble contains the given datum.
     */
    private static boolean isMember(final DefaultDatumEnsemble<?> ensemble, final IdentifiedObject datum) {
        if (ensemble != null) {
            for (final Datum member : ensemble.getMembers()) {
                if (Utilities.equalsIgnoreMetadata(datum, member)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the datum or ensemble of a coordinate operation from <var>source</var> to <var>target</var>.
     * If the two given coordinate reference systems are associated to the same datum, then this method returns
     * the <var>target</var> datum. Otherwise, this method returns the largest ensemble which fully contains the
     * datum or datum ensemble of the other <abbr>CRS</abbr>. If none of the <var>source</var> or <var>target</var>
     * datum ensembles met that criterion, then this method returns an empty value.
     * A non-empty value means that it is okay, for low accuracy requirements, to ignore the datum shift.
     *
     * <p>This is an alternative to the {@code ofOperation(…)} methods when the caller does not need to view
     * the returned object as a datum.</p>
     *
     * @param  source  the source <abbr>CRS</abbr> of a coordinate operation, or {@code null}.
     * @param  target  the target <abbr>CRS</abbr> of a coordinate operation, or {@code null}.
     * @return datum or datum ensemble of the coordinate operation result if it is okay to ignore datum shift.
     */
    public static Optional<IdentifiedObject> getDatumOrEnsemble(final SingleCRS source, final SingleCRS target) {
        if (source == null) return Optional.ofNullable(getDatumOrEnsemble(target));
        if (target == null) return Optional.ofNullable(getDatumOrEnsemble(source));
        return ofOperation(source, source.getDatum(),
                           target, target.getDatum(),
                           (ensemble) -> ensemble);
    }

    /**
     * Returns the datum of the given <abbr>CRS</abbr> if presents, or the datum ensemble otherwise.
     * This is an alternative to the {@code of(…)} methods when the caller does not need to view the
     * returned object as a datum.
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
        return getDatumEnsemble(crs);
    }

    /**
     * If the given object is a datum ensemble or a wrapper for a datum ensemble, returns its accuracy.
     * This method recognizes the {@link DefaultDatumEnsemble} and {@link PseudoDatum} types.
     *
     * @param  object  the object from which to get the ensemble accuracy, or {@code null}.
     * @return the datum ensemble accuracy if the given object is a datum ensemble or a wrapper.
     * @throws NullPointerException if the given object should provide an accuracy but didn't.
     */
    public static Optional<PositionalAccuracy> getEnsembleAccuracy(final IdentifiedObject object) {
        final DefaultDatumEnsemble<?> ensemble;
        if (object instanceof DefaultDatumEnsemble<?>) {
            ensemble = (DefaultDatumEnsemble<?>) object;
        } else if (object instanceof PseudoDatum<?>) {
            ensemble = ((PseudoDatum<?>) object).ensemble;
        } else {
            return Optional.empty();
        }
        return Optional.of(ensemble.getEnsembleAccuracy());     // Intentional NullPointerException if this property is null.
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
    public ReferenceIdentifier getName() {
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
    public Set<ReferenceIdentifier> getIdentifiers() {
        return ensemble.getIdentifiers();
    }

    /**
     * Returns the domain of validity common to all datum members, if any.
     *
     * @return value common to all ensemble members, or {@code null} if none.
     * @hidden
     */
    @Override
    @Deprecated
    public Extent getDomainOfValidity() {
        return getCommonNullableValue(Datum::getDomainOfValidity);
    }

    /**
     * Returns the scope common to all datum members, if any.
     *
     * @return value common to all ensemble members, or {@code null} if none.
     * @hidden
     */
    @Override
    @Deprecated
    public InternationalString getScope() {
        return getCommonNullableValue(Datum::getScope);
    }

    /**
     * Returns the anchor point common to all datum members, if any.
     *
     * @return value common to all ensemble members, or {@code null} if none.
     * @hidden
     */
    @Override
    @Deprecated
    public InternationalString getAnchorPoint() {
        return getCommonNullableValue(Datum::getAnchorPoint);
    }

    /**
     * Returns the realization epoch common to all datum members, if any.
     *
     * @return value common to all ensemble members, or {@code null} if none.
     * @hidden
     */
    @Override
    @Deprecated
    public Date getRealizationEpoch() {
        return getCommonNullableValue(Datum::getRealizationEpoch);
    }

    /**
     * Returns an optional value which is common to all ensemble members.
     * If all members do not have the same value, returns {@code null}.
     *
     * @param  <V>     type of value.
     * @param  getter  method to invoke on each member for getting the value.
     * @return a value common to all members, or {@code null} if none.
     */
    final <V> V getCommonNullableValue(final Function<D, V> getter) {
        final Iterator<D> it = ensemble.getMembers().iterator();
check:  if (it.hasNext()) {
            final V value = getter.apply(it.next());
            if (value != null) {
                while (it.hasNext()) {
                    if (!value.equals(getter.apply(it.next()))) {
                        break check;
                    }
                }
                return value;
            }
        }
        return null;
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
    public InternationalString getRemarks() {
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
        Geodetic(final DefaultDatumEnsemble<GeodeticDatum> ensemble) {
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
        Vertical(final DefaultDatumEnsemble<VerticalDatum> ensemble) {
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
         * @deprecated Replaced by {@link #getRealizationMethod()}.
         */
        @Override
        @Deprecated
        public VerticalDatumType getVerticalDatumType() {
            return getCommonNullableValue(VerticalDatum::getVerticalDatumType);
        }
    }

    /**
     * A pseudo-datum for an ensemble of temporal datum.
     */
    private static final class Time extends PseudoDatum<TemporalDatum> implements TemporalDatum {
        /** For cross-versions compatibility. */
        private static final long serialVersionUID = -4208563828181087035L;

        /** Creates a new pseudo-datum wrapping the given ensemble. */
        Time(final DefaultDatumEnsemble<TemporalDatum> ensemble) {
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
        public Date getOrigin() {
            return getCommonMandatoryValue(TemporalDatum::getOrigin);
        }
    }

    /**
     * A pseudo-datum for an ensemble of engineering datum.
     */
    private static final class Engineering extends PseudoDatum<EngineeringDatum> implements EngineeringDatum {
        /** For cross-versions compatibility. */
        private static final long serialVersionUID = -8978468990963666861L;

        /** Creates a new pseudo-datum wrapping the given ensemble. */
        Engineering(final DefaultDatumEnsemble<EngineeringDatum> ensemble) {
            super(ensemble);
        }

        /** Returns the GeoAPI interface implemented by this pseudo-datum. */
        @Override public Class<EngineeringDatum> getInterface() {
            return EngineeringDatum.class;
        }
    }
}
