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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
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
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.apache.sis.util.Static;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.metadata.privy.Identifiers;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.GeodeticException;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.crs.ParametricCRS;
import org.opengis.referencing.datum.DatumEnsemble;
import org.opengis.referencing.datum.ParametricDatum;
import org.opengis.referencing.datum.RealizationMethod;


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
     * The {@value} keyword which sometime appear at the end of a datum ensemble name.
     */
    private static final String ENSEMBLE = "ensemble";

    /**
     * The datum of a <abbr>CRS</abbr> or specified by the user, or {@code null} if none.
     */
    private final Datum datum;

    /**
     * The ensemble of a <abbr>CRS</abbr> or specified by the user, or {@code null} if none.
     */
    private final DatumEnsemble<?> ensemble;

    /**
     * Criterion for deciding if two properties should be considered equal.
     */
    private final ComparisonMode mode;

    /**
     * For internal usage only. The fact that we may create instances of this class is a hidden implementation details.
     */
    private DatumOrEnsemble(final Datum datum, final DatumEnsemble<?> ensemble, final ComparisonMode mode) {
        this.datum    = datum;
        this.ensemble = ensemble;
        this.mode     = mode;
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
     * If the two given coordinate reference systems are associated to equal datum (ignoring metadata),
     * then this method returns the <var>target</var> datum. Otherwise, this method returns
     * the largest ensemble which fully contains the datum or datum ensemble of the other <abbr>CRS</abbr>.
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
     * If the two given coordinate reference systems are associated to equal (ignoring metadata) datum,
     * then this method returns the <var>target</var> datum. Otherwise, this method returns a pseudo-datum
     * for the largest ensemble which fully contains the datum or datum ensemble of the other <abbr>CRS</abbr>.
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
     * @param  sourceCRS    the source <abbr>CRS</abbr> of a coordinate operation.
     * @param  sourceDatum  the datum of the source <abbr>CRS</abbr>.
     * @param  targetCRS    the target <abbr>CRS</abbr> of a coordinate operation.
     * @param  targetDatum  the datum of the target <abbr>CRS</abbr>.
     * @param  constructor  function to invoke for wrapping a datum ensemble in a pseudo-datum.
     * @return datum or pseudo-datum of the coordinate operation result if it is okay to ignore datum shift.
     */
    @SuppressWarnings("unchecked")          // Casts are safe because callers know the method signature of <D>.
    private static <C extends SingleCRS, D extends Datum, R extends IdentifiedObject> Optional<R> asTargetDatum(
            final C sourceCRS, final R sourceDatum,
            final C targetCRS, final R targetDatum,
            final Function<DatumEnsemble<D>, R> constructor)
    {
        if (sourceDatum != null && Utilities.equalsIgnoreMetadata(sourceDatum, targetDatum)) {
            return Optional.of(targetDatum);
        }
        DatumEnsemble<D> sourceEnsemble;
        DatumEnsemble<D> targetEnsemble;
        DatumEnsemble<D> selected;
        if ((isMember(selected = targetEnsemble = (DatumEnsemble<D>) targetCRS.getDatumEnsemble(), sourceDatum)) ||
            (isMember(selected = sourceEnsemble = (DatumEnsemble<D>) sourceCRS.getDatumEnsemble(), targetDatum)) ||
            (sourceEnsemble != null && sourceEnsemble == targetEnsemble))     // Optimization for a common case.
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
        if (ensemble != null && datum != null) {
            for (final Datum member : ensemble.getMembers()) {
                if (Utilities.equalsIgnoreMetadata(datum, member)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * If the given object is a pseudo-datum for a geodetic ensemble, returns that ensemble.
     * This method is the converse of {@link #asDatum(GeodeticCRS)} and recognizes only the
     * pseudo-datum created by this class of by {@link DefaultDatumEnsemble} static methods.
     *
     * @param  datum  the object which may be a pseudo-datum for an ensemble, or {@code null}.
     * @return the given object cast to a datum ensemble if the cast is valid.
     */
    public static Optional<DatumEnsemble<GeodeticDatum>> asEnsemble(final GeodeticDatum datum) {
        if (datum instanceof DefaultDatumEnsemble.Geodetic) {
            return Optional.of((DefaultDatumEnsemble.Geodetic) datum);
        }
        return Optional.empty();
    }

    /**
     * If the given object is a pseudo-datum for a vertical ensemble, returns that ensemble.
     * This method is the converse of {@link #asDatum(VerticalCRS)} and recognizes only the
     * pseudo-datum created by this class of by {@link DefaultDatumEnsemble} static methods.
     *
     * @param  datum  the object which may be a pseudo-datum for an ensemble, or {@code null}.
     * @return the given object cast to a datum ensemble if the cast is valid.
     */
    public static Optional<DatumEnsemble<VerticalDatum>> asEnsemble(final VerticalDatum datum) {
        if (datum instanceof DefaultDatumEnsemble.Vertical) {
            return Optional.of((DefaultDatumEnsemble.Vertical) datum);
        }
        return Optional.empty();
    }

    /**
     * If the given object is a pseudo-datum for a temporal ensemble, returns that ensemble.
     * This method is the converse of {@link #asDatum(TemporalCRS)} and recognizes only the
     * pseudo-datum created by this class of by {@link DefaultDatumEnsemble} static methods.
     *
     * @param  datum  the object which may be a pseudo-datum for an ensemble, or {@code null}.
     * @return the given object cast to a datum ensemble if the cast is valid.
     */
    public static Optional<DatumEnsemble<TemporalDatum>> asEnsemble(final TemporalDatum datum) {
        if (datum instanceof DefaultDatumEnsemble.Time) {
            return Optional.of((DefaultDatumEnsemble.Time) datum);
        }
        return Optional.empty();
    }

    /**
     * If the given object is a pseudo-datum for a parametric ensemble, returns that ensemble.
     * This method is the converse of {@link #asDatum(ParametricCRS)} and recognizes only the
     * pseudo-datum created by this class of by {@link DefaultDatumEnsemble} static methods.
     *
     * @param  datum  the object which may be a pseudo-datum for an ensemble, or {@code null}.
     * @return the given object cast to a datum ensemble if the cast is valid.
     */
    public static Optional<DatumEnsemble<ParametricDatum>> asEnsemble(final ParametricDatum datum) {
        if (datum instanceof DefaultDatumEnsemble.Parametric) {
            return Optional.of((DefaultDatumEnsemble.Parametric) datum);
        }
        return Optional.empty();
    }

    /**
     * If the given object is a pseudo-datum for a engineering ensemble, returns that ensemble.
     * This method is the converse of {@link #asDatum(EngineeringCRS)} and recognizes only the
     * pseudo-datum created by this class of by {@link DefaultDatumEnsemble} static methods.
     *
     * @param  datum  the object which may be a pseudo-datum for an ensemble, or {@code null}.
     * @return the given object cast to a datum ensemble if the cast is valid.
     */
    public static Optional<DatumEnsemble<EngineeringDatum>> asEnsemble(final EngineeringDatum datum) {
        if (datum instanceof DefaultDatumEnsemble.Engineering) {
            return Optional.of((DefaultDatumEnsemble.Engineering) datum);
        }
        return Optional.empty();
    }

    /**
     * Returns whether a legacy definition of a datum may be considered as equivalent to the given datum ensemble.
     * This is {@code true} if all reference frames (both the specified datum and the ensemble members) have the
     * same properties (ellipsoid and prime meridians in the geodetic case), and the datum and datum ensemble either
     * have a common identifier or an {@linkplain IdentifiedObjects#isHeuristicMatchForName(IdentifiedObject, String)
     * heuristic match of name}.
     *
     * <p>This method does not verify if the given datum is a member of the given ensemble.
     * If the datum was a member, then the two objects would <em>not</em> be conceptually equal.
     * We would rather have one object clearly identified as more accurate than the other.</p>
     *
     * <h4>Use case</h4>
     * This method is for interoperability between the old and new definitions of <abbr>WGS</abbr> 1984 (<abbr>EPSG</abbr>:4326).
     * Before <abbr>ISO</abbr> 19111:2019, the <i>datum ensemble</i> concept did not existed in the <abbr>OGC</abbr>/<abbr>ISO</abbr> standards
     * and <abbr>WGS</abbr> 1984 was defined as a {@link Datum}.
     * In recent standards, <abbr>WGS</abbr> 1984 is defined as a {@link DatumEnsemble}, but the old definition is still encountered.
     * For example, a <abbr>CRS</abbr> may have been parsed from a <abbr title="Geographic Markup Language">GML</abbr> document,
     * or from a <abbr title="Well-Known Text">WKT</abbr> 1 string, or from a <abbr>ISO</abbr> 19162:2015 string, <i>etc.</i>
     * This method can be used for detecting such situations.
     * While <abbr>WGS</abbr> 1984 is the main use case, this method can be used for any datum in the same situation.
     *
     * @param  ensemble  the datum ensemble, or {@code null}.
     * @param  datum     the datum, or {@code null}.
     * @param  mode      the criterion for comparing ellipsoids and prime meridians.
     * @return whether the two objects could be considered as equal if the concept of datum ensemble did not existed.
     */
    public static boolean isLegacyDatum(final DatumEnsemble<?> ensemble, final Datum datum, final ComparisonMode mode) {
        if (ensemble == null || datum == null) {
            return false;
        }
        // Two null values are not considered equal because they are not of the same type.
        if (ensemble == datum) {
            return true;
        }
        final var c = new DatumOrEnsemble(datum, ensemble, mode);
        if (!(c.isPropertyEqual(GeodeticDatum.class, GeodeticDatum::getEllipsoid,         Objects::nonNull) &&
              c.isPropertyEqual(GeodeticDatum.class, GeodeticDatum::getPrimeMeridian,     Objects::nonNull) &&
              c.isPropertyEqual(VerticalDatum.class, VerticalDatum::getRealizationMethod, Optional::isPresent)))
        {
            return false;
        }
        final Boolean match = Identifiers.hasCommonIdentifier(ensemble.getIdentifiers(), datum.getIdentifiers());
        if (match != null) {
            return match;
        }
        /*
         * We could not answer the question using identifiers. Try using the names.
         * The primary name is likely to not match, because ensemble names in EPSG
         * dataset often ends with "ensemble" while datum names often do not. But
         * we are more interrested in the ensemble's aliases in the next line.
         */
        if (IdentifiedObjects.isHeuristicMatchForName(ensemble, datum.getName().getCode())) {
            return true;
        }
        /*
         * Try to remove the "ensemble" prefix in the datum ensemble name and try again.
         * This time, the comparison will also check `datum` aliases instead of `ensemble`.
         */
        String name = ensemble.getName().getCode();
        if (name.endsWith(ENSEMBLE)) {
            int i = name.length() - ENSEMBLE.length();
            if (i > (i = CharSequences.skipTrailingWhitespaces(name, 0, i))) {
                name = name.substring(0, i);    // Remove the "ensemble" suffix.
            }
        }
        return IdentifiedObjects.isHeuristicMatchForName(datum, name);
    }

    /**
     * Returns the ellipsoid used by the given coordinate reference system.
     * This method searches in the following locations:
     *
     * <ul>
     *   <li>If the given <abbr>CRS</abbr> is an instance of {@link SingleCRS} and its datum
     *       is a {@link GeodeticDatum}, then this method returns the datum ellipsoid.</li>
     *   <li>Otherwise, if the given <abbr>CRS</abbr> is an instance of {@link SingleCRS}, is associated to a
     *       {@link DatumEnsemble}, and all members of the ensemble have equal (ignoring metadata) ellipsoid,
     *       then returns that ellipsoid.</li>
     *   <li>Otherwise, if the given <abbr>CRS</abbr> is an instance of {@link CompoundCRS}, then this method
     *       searches recursively in each component until a geodetic reference frame is found.</li>
     *   <li>Otherwise, this method returns an empty value.</li>
     * </ul>
     *
     * This method may return an empty value if the ellipsoid is not equal (ignoring metadata) for all members of the ensemble.
     *
     * @param  crs  the coordinate reference system for which to get the ellipsoid.
     * @return the ellipsoid, or an empty value if none or not equivalent for all members of the ensemble.
     */
    public static Optional<Ellipsoid> getEllipsoid(final CoordinateReferenceSystem crs) {
        return Optional.ofNullable(getProperty(crs, GeodeticDatum.class, GeodeticDatum::getEllipsoid, Objects::nonNull));
    }

    /**
     * Returns the prime meridian used by the given coordinate reference system.
     * This method applies the same rules as {@link #getEllipsoid(CoordinateReferenceSystem)}.
     *
     * @param  crs  the coordinate reference system for which to get the prime meridian.
     * @return the prime meridian, or an empty value if none or not equivalent for all members of the ensemble.
     *
     * @see org.apache.sis.referencing.CRS#getGreenwichLongitude(GeodeticCRS)
     */
    public static Optional<PrimeMeridian> getPrimeMeridian(final CoordinateReferenceSystem crs) {
        return Optional.ofNullable(getProperty(crs, GeodeticDatum.class, GeodeticDatum::getPrimeMeridian, Objects::nonNull));
    }

    /**
     * Returns the realization method used by the given coordinate reference system.
     * This method searches in the following locations:
     *
     * <ul>
     *   <li>If the given <abbr>CRS</abbr> is an instance of {@link SingleCRS} and its datum
     *       is a {@link VerticalDatum}, then this method returns the realization method.</li>
     *   <li>Otherwise, if the given <abbr>CRS</abbr> is an instance of {@link SingleCRS}, is associated to a
     *       {@link DatumEnsemble}, and all members of the ensemble have equal (ignoring metadata) realization
     *       methods, then returns that method.</li>
     *   <li>Otherwise, if the given <abbr>CRS</abbr> is an instance of {@link CompoundCRS}, then this method
     *       searches recursively in each component until a vertical reference frame is found.</li>
     *   <li>Otherwise, this method returns an empty value.</li>
     * </ul>
     *
     * This method may return an empty value if a datum ensemble contains different realization methods.
     *
     * @param  crs  the coordinate reference system for which to get the realization method.
     * @return the realization method, or an empty value if none or not equal for all members.
     *
     * @since 2.0 (temporary version number until this branch is released)
     */
    public static Optional<RealizationMethod> getRealizationMethod(final CoordinateReferenceSystem crs) {
        Optional<RealizationMethod> common = getProperty(crs, VerticalDatum.class, VerticalDatum::getRealizationMethod, Optional::isPresent);
        return (common != null) ? common : Optional.empty();
    }

    /**
     * Implementation of {@code getEllipsoid(CRS)} and {@code getPrimeMeridian(CRS)}.
     *
     * @param  <P>      the type of property to get.
     * @param  <D>      the type of datum expected by the given {@code getter}.
     * @param  crs      the coordinate reference system for which to get the ellipsoid or prime meridian.
     * @param  getter   the method to invoke on {@link Datum} instances for getting the property.
     * @param  nonNull  test about whether a property value is non-null or present.
     * @return the property value, or {@code null} if none or not equal for all members.
     */
    private static <P, D extends Datum> P getProperty(final CoordinateReferenceSystem crs, final Class<D> datumType,
                                                      final Function<D, P> getter, final Predicate<P> nonNull)
    {
        if (crs instanceof SingleCRS) {
            final var scrs = (SingleCRS) crs;
            final Datum datum = scrs.getDatum();
            if (datumType.isInstance(datum)) {
                @SuppressWarnings("unchecked")
                P property = getter.apply((D) datum);
                if (nonNull.test(property)) {
                    return property;
                }
            }
            final var c = new DatumOrEnsemble(datum, scrs.getDatumEnsemble(), ComparisonMode.IGNORE_METADATA);
            return c.getEnsembleProperty(null, datumType, getter, nonNull);
        } else if (crs instanceof CompoundCRS) {
            for (final CoordinateReferenceSystem c : ((CompoundCRS) crs).getComponents()) {
                final P property = getProperty(c, datumType, getter, nonNull);
                if (property != null) {
                    return property;
                }
            }
        }
        return null;
    }

    /**
     * Returns a property of ensemble member if it is the same for all members.
     * Returns {@code null} if the value is absent or not equal for all members.
     * If {@code common} is non-null, then this method take in account only the
     * property values equal to {@code common}
     * (i.e., it searches if the value is present).
     *
     * @param  <P>      the type of property to get.
     * @param  <D>      the type of datum expected by the given {@code getter}.
     * @param  common   if non-null, ignore all properties not equal to {@code common}.
     * @param  getter   the method to invoke on {@link Datum} instances for getting the property.
     * @param  nonNull  test about whether a property value is non-null or present.
     * @return the property value, or {@code null} if none or not equal for all members.
     */
    private <P, D extends Datum> P getEnsembleProperty(P common, final Class<D> datumType, final Function<D,P> getter, final Predicate<P> nonNull) {
        final boolean searching = (common != null);
        if (ensemble != null) {
            for (Datum member : ensemble.getMembers()) {
                if (datumType.isInstance(member)) {
                    @SuppressWarnings("unchecked")
                    final P property = getter.apply((D) member);
                    if (nonNull.test(property)) {
                        if (common == null) {
                            common = property;
                        } else if (Utilities.deepEquals(property, common, mode) == searching) {
                            return searching ? common : null;
                        }
                    }
                }
            }
        }
        return searching ? null : common;
    }

    /**
     * Checks whether the datum and datum ensemble have equal values for a given property
     *
     * @param  <P>      the type of property to get.
     * @param  <D>      the type of datum expected by the given {@code getter}.
     * @param  getter   the method to invoke on {@link Datum} instances for getting the property.
     * @param  nonNull  test about whether a property value is non-null or present.
     * @return whether the property values are equal.
     */
    @SuppressWarnings("unchecked")
    private <P, D extends Datum> boolean isPropertyEqual(final Class<D> datumType, final Function<D,P> getter, final Predicate<P> nonNull) {
        P property = null;
        if (datumType.isInstance(datum)) {
            property = getter.apply((D) datum);
            if (!nonNull.test(property)) {
                // Property unspecified in the datum. Accept any value in the ensemble.
                return true;
            }
        }
        return getEnsembleProperty(property, datumType, getter, nonNull) == property;
    }

    /**
     * If the given object is a datum ensemble or a <abbr>CRS</abbr> associated to a datum ensemble, returns its accuracy.
     *
     * @param  object  the object from which to get the ensemble accuracy, or {@code null}.
     * @return the datum ensemble accuracy if the given object is a datum ensemble.
     * @throws NullPointerException if the given object should provide an accuracy but didn't.
     *
     * @see org.apache.sis.referencing.CRS#getLinearAccuracy(CoordinateOperation)
     */
    public static Optional<PositionalAccuracy> getAccuracy(final IdentifiedObject object) {
        final DatumEnsemble<?> ensemble;
        if (object instanceof DatumEnsemble<?>) {
            ensemble = (DatumEnsemble<?>) object;
        } else if (object instanceof SingleCRS) {
            ensemble = ((SingleCRS) object).getDatumEnsemble();
            if (ensemble == null) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
        return Optional.of(ensemble.getEnsembleAccuracy());     // Intentional NullPointerException if this property is null.
    }
}
