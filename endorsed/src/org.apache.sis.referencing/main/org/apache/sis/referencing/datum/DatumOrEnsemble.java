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
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.EngineeringDatum;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.apache.sis.util.Static;
import org.apache.sis.util.Utilities;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.GeodeticException;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.crs.ParametricCRS;
import org.opengis.referencing.datum.DatumEnsemble;
import org.opengis.referencing.datum.ParametricDatum;


/**
 * Utility methods for working on objects that may be {@code Datum} or {@code DatumEnsemble}.
 * The methods in this class view an ensemble as if it was a datum for the sake of simplicity.
 * For example, {@code DatumOrEnsemble.of(crs)} allows to
 * {@linkplain IdentifiedObjects#isHeuristicMatchForName compare the datum name}
 * without the need to check which one of the {@code getDatum()} or {@code getDatumEnsemble()}
 * methods returns a non-null value.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public final class DatumOrEnsemble extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private DatumOrEnsemble() {
    }

    /**
     * Returns the datum of the given <abbr>CRS</abbr> if presents, or the datum ensemble otherwise.
     * This is an alternative to the {@code asDatum(…)} methods when the caller does not need to view
     * the returned object as a datum.
     *
     * @param  crs  the <abbr>CRS</abbr> from which to get the datum or ensemble, or {@code null}.
     * @return the datum if present, or the datum ensemble otherwise, or {@code null}.
     */
    public static IdentifiedObject of(final SingleCRS crs) {
        if (crs == null) return null;
        final Datum datum = crs.getDatum();
        return (datum != null) ? datum : crs.getDatumEnsemble();
    }

    /**
     * Returns the datum or ensemble of a coordinate operation from <var>source</var> to <var>target</var>.
     * If the two given coordinate reference systems are associated to the same datum, then this method returns
     * the <var>target</var> datum. Otherwise, this method returns the largest ensemble which fully contains the
     * datum or datum ensemble of the other <abbr>CRS</abbr>.
     * That largest common ensemble is interpreted as the new target of the operation result.
     * If none of the <var>source</var> or <var>target</var> datum ensembles met the above criteria,
     * then this method returns an empty value.
     * A non-empty value means that it is okay, for low accuracy requirements, to ignore the datum shift.
     *
     * <p>This is an alternative to the {@code asTargetDatum(…)} methods
     * when the caller does not need to view the returned object as a datum.</p>
     *
     * @param  source  the source <abbr>CRS</abbr> of a coordinate operation.
     * @param  target  the target <abbr>CRS</abbr> of a coordinate operation.
     * @return datum or datum ensemble of the coordinate operation result if it is okay to ignore datum shift.
     * @throws NullPointerException if any argument is null.
     */
    public static Optional<IdentifiedObject> ofTarget(final SingleCRS source, final SingleCRS target) {
        return asTargetDatum(source, source.getDatum(),
                             target, target.getDatum(),
                             (ensemble) -> ensemble);
    }

    /**
     * Returns the datum (preferred) or ensemble (fallback) of the given geodetic <abbr>CRS</abbr>.
     * If the given <abbr>CRS</abbr> is associated to a non-null datum, then this method returns that datum.
     * Otherwise, this method returns the <abbr>CRS</abbr> datum ensemble as a pseudo-datum.
     *
     * <h4>Common properties</h4>
     * If an ensemble is viewed as a pseudo-datum, then the implementation of the {@link GeodeticDatum#getEllipsoid()}
     * and {@link GeodeticDatum#getPrimeMeridian()} methods will verify that all members have the same ellipsoid or
     * prime meridian respectively. If this condition does not hold, then a {@link GeodeticException} will be thrown.
     * Note that this verification is done when the above-cited methods are invoked, not when this {@code asDatum(…)}
     * method is invoked.
     *
     * @param  crs  the coordinate reference system for which to get the datum or datum ensemble, or {@code null}.
     * @return the datum or pseudo-datum of the given <abbr>CRS</abbr>, or {@code null}.
     */
    public static GeodeticDatum asDatum(final GeodeticCRS crs) {
        if (crs == null) return null;
        final GeodeticDatum datum = crs.getDatum();
        return (datum != null) ? datum : DefaultDatumEnsemble.Geodetic.datum(crs.getDatumEnsemble());
    }

    /**
     * Returns the datum (preferred) or ensemble (fallback) of the given vertical <abbr>CRS</abbr>.
     * If the given <abbr>CRS</abbr> is associated to a non-null datum, then this method returns that datum.
     * Otherwise, this method returns the <abbr>CRS</abbr> datum ensemble as a pseudo-datum.
     *
     * @param  crs  the coordinate reference system for which to get the datum or datum ensemble, or {@code null}.
     * @return the datum or pseudo-datum of the given <abbr>CRS</abbr>, or {@code null}.
     */
    public static VerticalDatum asDatum(final VerticalCRS crs) {
        if (crs == null) return null;
        final VerticalDatum datum = crs.getDatum();
        return (datum != null) ? datum : DefaultDatumEnsemble.Vertical.datum(crs.getDatumEnsemble());
    }

    /**
     * Returns the datum (preferred) or ensemble (fallback) of the given temporal <abbr>CRS</abbr>.
     * If the given <abbr>CRS</abbr> is associated to a non-null datum, then this method returns that datum.
     * Otherwise, this method returns the <abbr>CRS</abbr> datum ensemble as a pseudo-datum.
     *
     * <h4>Common property</h4>
     * If an ensemble is viewed as a pseudo-datum, then the implementation of the {@link TemporalDatum#getOrigin()}
     * method will verify that all members have the same origin. If this condition does not hold,
     * then a {@link GeodeticException} will be thrown. Note that this verification is done when
     * the above-cited method is invoked, not when this {@code asDatum(…)} method is invoked.
     *
     * @param  crs  the coordinate reference system for which to get the datum or datum ensemble, or {@code null}.
     * @return the datum or pseudo-datum of the given <abbr>CRS</abbr>, or {@code null}.
     */
    public static TemporalDatum asDatum(final TemporalCRS crs) {
        if (crs == null) return null;
        final TemporalDatum datum = crs.getDatum();
        return (datum != null) ? datum : DefaultDatumEnsemble.Time.datum(crs.getDatumEnsemble());
    }

    /**
     * Returns the datum (preferred) or ensemble (fallback) of the given parametric <abbr>CRS</abbr>.
     * If the given <abbr>CRS</abbr> is associated to a non-null datum, then this method returns that datum.
     * Otherwise, this method returns the <abbr>CRS</abbr> datum ensemble as a pseudo-datum.
     *
     * @param  crs  the coordinate reference system for which to get the datum or datum ensemble, or {@code null}.
     * @return the datum or pseudo-datum of the given <abbr>CRS</abbr>, or {@code null}.
     *
     * @since 2.0 (temporary version number until this branch is released)
     */
    public static ParametricDatum asDatum(final ParametricCRS crs) {
        if (crs == null) return null;
        final ParametricDatum datum = crs.getDatum();
        return (datum != null) ? datum : DefaultDatumEnsemble.Parametric.datum(crs.getDatumEnsemble());
    }

    /**
     * Returns the datum (preferred) or ensemble (fallback) of the given engineering <abbr>CRS</abbr>.
     * If the given <abbr>CRS</abbr> is associated to a non-null datum, then this method returns that datum.
     * Otherwise, this method returns the <abbr>CRS</abbr> datum ensemble as a pseudo-datum.
     *
     * @param  crs  the coordinate reference system for which to get the datum or datum ensemble, or {@code null}.
     * @return the datum or pseudo-datum of the given <abbr>CRS</abbr>, or {@code null}.
     */
    public static EngineeringDatum asDatum(final EngineeringCRS crs) {
        if (crs == null) return null;
        final EngineeringDatum datum = crs.getDatum();
        return (datum != null) ? datum : DefaultDatumEnsemble.Engineering.datum(crs.getDatumEnsemble());
    }

    /**
     * Returns the datum or pseudo-datum of the result of an operation between the given geodetic <abbr>CRS</abbr>s.
     * If the two given coordinate reference systems are associated to the same datum, then this method returns
     * the <var>target</var> datum. Otherwise, this method returns a pseudo-datum for the largest ensemble which
     * fully contains the datum or datum ensemble of the other <abbr>CRS</abbr>.
     * That largest common ensemble is interpreted as the new target of the operation result.
     * If none of the <var>source</var> or <var>target</var> datum ensembles met the above criteria,
     * then this method returns an empty value.
     * A non-empty value means that it is okay, for low accuracy requirements, to ignore the datum shift.
     *
     * @param  source  the source <abbr>CRS</abbr> of a coordinate operation.
     * @param  target  the target <abbr>CRS</abbr> of a coordinate operation.
     * @return datum or pseudo-datum of the coordinate operation result if it is okay to ignore datum shift.
     * @throws NullPointerException if any argument is null.
     */
    public static Optional<GeodeticDatum> asTargetDatum(final GeodeticCRS source, final GeodeticCRS target) {
        return asTargetDatum(source, source.getDatum(),
                             target, target.getDatum(),
                             DefaultDatumEnsemble.Geodetic::datum);
    }

    /**
     * Returns the datum or pseudo-datum of the result of an operation between the given vertical <abbr>CRS</abbr>s.
     * See {@link #asTargetDatum(GeodeticCRS, GeodeticCRS)} for more information.
     *
     * @param  source  the source <abbr>CRS</abbr> of a coordinate operation.
     * @param  target  the target <abbr>CRS</abbr> of a coordinate operation.
     * @return datum or pseudo-datum of the coordinate operation result if it is okay to ignore datum shift.
     * @throws NullPointerException if any argument is null.
     */
    public static Optional<VerticalDatum> asTargetDatum(final VerticalCRS source, final VerticalCRS target) {
        return asTargetDatum(source, source.getDatum(),
                             target, target.getDatum(),
                             DefaultDatumEnsemble.Vertical::datum);
    }

    /**
     * Returns the datum or pseudo-datum of the result of an operation between the given temporal <abbr>CRS</abbr>s.
     * See {@link #asTargetDatum(GeodeticCRS, GeodeticCRS)} for more information.
     *
     * @param  source  the source <abbr>CRS</abbr> of a coordinate operation.
     * @param  target  the target <abbr>CRS</abbr> of a coordinate operation.
     * @return datum or pseudo-datum of the coordinate operation result if it is okay to ignore datum shift.
     * @throws NullPointerException if any argument is null.
     */
    public static Optional<TemporalDatum> asTargetDatum(final TemporalCRS source, final TemporalCRS target) {
        return asTargetDatum(source, source.getDatum(),
                             target, target.getDatum(),
                             DefaultDatumEnsemble.Time::datum);
    }

    /**
     * Returns the datum or pseudo-datum of the result of an operation between the given parametric <abbr>CRS</abbr>s.
     * See {@link #asTargetDatum(GeodeticCRS, GeodeticCRS)} for more information.
     *
     * @param  source  the source <abbr>CRS</abbr> of a coordinate operation.
     * @param  target  the target <abbr>CRS</abbr> of a coordinate operation.
     * @return datum or pseudo-datum of the coordinate operation result if it is okay to ignore datum shift.
     * @throws NullPointerException if any argument is null.
     */
    public static Optional<ParametricDatum> asTargetDatum(final ParametricCRS source, final ParametricCRS target) {
        return asTargetDatum(source, source.getDatum(),
                             target, target.getDatum(),
                             DefaultDatumEnsemble.Parametric::datum);
    }

    /**
     * Returns the datum or pseudo-datum of the result of an operation between the given engineering <abbr>CRS</abbr>s.
     * See {@link #asTargetDatum(GeodeticCRS, GeodeticCRS)} for more information.
     *
     * @param  source  the source <abbr>CRS</abbr> of a coordinate operation.
     * @param  target  the target <abbr>CRS</abbr> of a coordinate operation.
     * @return datum or pseudo-datum of the coordinate operation result if it is okay to ignore datum shift.
     * @throws NullPointerException if any argument is null.
     */
    public static Optional<EngineeringDatum> asTargetDatum(final EngineeringCRS source, final EngineeringCRS target) {
        return asTargetDatum(source, source.getDatum(),
                             target, target.getDatum(),
                             DefaultDatumEnsemble.Engineering::datum);
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
    private static <C extends SingleCRS, D extends Datum, R extends IdentifiedObject> Optional<R> asTargetDatum(
            final C source, final R sourceDatum,
            final C target, final R targetDatum,
            final Function<DatumEnsemble<D>, R> constructor)
    {
        if (sourceDatum != null && Utilities.equalsIgnoreMetadata(sourceDatum, targetDatum)) {
            return Optional.of(targetDatum);
        }
        DatumEnsemble<D> sourceEnsemble;
        DatumEnsemble<D> targetEnsemble;
        DatumEnsemble<D> selected;
        if ((isMember(selected = targetEnsemble = (DatumEnsemble<D>) target.getDatumEnsemble(), sourceDatum)) ||
            (isMember(selected = sourceEnsemble = (DatumEnsemble<D>) source.getDatumEnsemble(), targetDatum)))
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
    private static boolean isMember(final DatumEnsemble<?> ensemble, final IdentifiedObject datum) {
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
     * Returns the ellipsoid used by the given coordinate reference system.
     * More specifically:
     *
     * <ul>
     *   <li>If the given <abbr>CRS</abbr> is an instance of {@link SingleCRS} and its datum
     *       is a {@link GeodeticDatum}, then this method returns the datum ellipsoid.</li>
     *   <li>Otherwise, if the given <abbr>CRS</abbr> is associated to a {@link DatumEnsemble} and all members
     *       of the ensemble have equal (ignoring metadata) ellipsoid, then returns that ellipsoid.</li>
     *   <li>Otherwise, if the given <abbr>CRS</abbr> is an instance of {@link CompoundCRS}, then this method
     *       searches recursively in each component until a geodetic reference frame is found.</li>
     *   <li>Otherwise, this method returns an empty value.</li>
     * </ul>
     *
     * Note that this method does not check if a compound <abbr>CRS</abbr> contains more than one ellipsoid
     * (it should never be the case). Note also that this method may return an empty value even
     * if the <abbr>CRS</abbr> is geodetic.
     *
     * @param  crs  the coordinate reference system for which to get the ellipsoid.
     * @return the ellipsoid, or an empty value if none or inconsistent.
     */
    public static Optional<Ellipsoid> getEllipsoid(final CoordinateReferenceSystem crs) {
        return getGeodeticProperty(crs, GeodeticDatum::getEllipsoid);
    }

    /**
     * Returns the prime meridian used by the given coordinate reference system.
     * This method applies the same rules as {@link #getEllipsoid(CoordinateReferenceSystem)}.
     *
     * @param  crs  the coordinate reference system for which to get the prime meridian.
     * @return the prime meridian, or an empty value if none or inconsistent.
     *
     * @see org.apache.sis.referencing.CRS#getGreenwichLongitude(GeodeticCRS)
     */
    public static Optional<PrimeMeridian> getPrimeMeridian(final CoordinateReferenceSystem crs) {
        return getGeodeticProperty(crs, GeodeticDatum::getPrimeMeridian);
    }

    /**
     * Implementation of {@code getEllipsoid(CRS)} and {@code getPrimeMeridian(CRS)}.
     *
     * @param  <P>     the type of object to get.
     * @param  crs     the coordinate reference system for which to get the ellipsoid or prime meridian.
     * @param  getter  the method to invoke on {@link GeodeticDatum} instances.
     * @return the ellipsoid or prime meridian, or an empty value if none of inconsistent.
     */
    private static <P> Optional<P> getGeodeticProperty(final CoordinateReferenceSystem crs, final Function<GeodeticDatum, P> getter) {
single: if (crs instanceof SingleCRS) {
            final var scrs = (SingleCRS) crs;
            final Datum datum = scrs.getDatum();
            if (datum instanceof GeodeticDatum) {
                P property = getter.apply((GeodeticDatum) datum);
                if (property != null) {
                    return Optional.of(property);
                }
            }
            final DatumEnsemble<?> ensemble = scrs.getDatumEnsemble();
            if (ensemble != null) {
                P common = null;
                for (Datum member : ensemble.getMembers()) {
                    if (member instanceof GeodeticDatum) {
                        final P property = getter.apply((GeodeticDatum) member);
                        if (property != null) {
                            if (common == null) {
                                common = property;
                            } else if (!Utilities.equalsIgnoreMetadata(property, common)) {
                                break single;
                            }
                        }
                    }
                }
                return Optional.ofNullable(common);
            }
        }
        if (crs instanceof CompoundCRS) {
            for (final CoordinateReferenceSystem c : ((CompoundCRS) crs).getComponents()) {
                final Optional<P> property = getGeodeticProperty(c, getter);
                if (property.isPresent()) {
                    return property;
                }
            }
        }
        return Optional.empty();
    }

    /**
     * If the given object is a datum ensemble, returns its accuracy.
     *
     * @param  object  the object from which to get the ensemble accuracy, or {@code null}.
     * @return the datum ensemble accuracy if the given object is a datum ensemble.
     * @throws NullPointerException if the given object should provide an accuracy but didn't.
     */
    public static Optional<PositionalAccuracy> getAccuracy(final IdentifiedObject object) {
        final DatumEnsemble<?> ensemble;
        if (object instanceof DatumEnsemble<?>) {
            ensemble = (DatumEnsemble<?>) object;
        } else {
            return Optional.empty();
        }
        return Optional.of(ensemble.getEnsembleAccuracy());     // Intentional NullPointerException if this property is null.
    }
}
