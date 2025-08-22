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
package org.apache.sis.referencing.crs;

import java.util.Map;
import java.util.Objects;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.datum.Datum;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.metadata.privy.ImplementationHelper;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.datum.DatumEnsemble;
import org.opengis.metadata.Identifier;


/**
 * Base class of <abbr>CRS</abbr> associated to a datum.
 *
 * @param  <D>  the type of datum associated to this <abbr>CRS</abbr>.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
@XmlType(name = "AbstractSingleCRSType")
@XmlRootElement(name = "AbstractSingleCRS")
@XmlSeeAlso({
    AbstractDerivedCRS.class,
    DefaultGeodeticCRS.class,
    DefaultVerticalCRS.class,
    DefaultTemporalCRS.class,
    DefaultParametricCRS.class,
    DefaultEngineeringCRS.class,
    DefaultImageCRS.class
})
class AbstractSingleCRS<D extends Datum> extends AbstractCRS implements SingleCRS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 2876221982955686798L;

    /**
     * The datum, or {@code null} if the <abbr>CRS</abbr> is associated only to a datum ensemble.
     *
     * <p><b>Consider this field as final!</b>
     * This field is non-final only for construction convenience and for unmarshalling.</p>
     *
     * @see #getDatum()
     */
    @SuppressWarnings("serial")     // Most SIS implementations are serializable.
    private D datum;

    /**
     * Collection of reference frames which for low accuracy requirements may be considered to be
     * insignificantly different from each other. May be {@code null} if there is no such ensemble.
     *
     * @see #getDatumEnsemble()
     */
    @SuppressWarnings("serial")     // Most SIS implementations are serializable.
    private final DatumEnsemble<D> ensemble;

    /**
     * Creates a coordinate reference system from the given properties, datum and coordinate system.
     * At least one of the {@code datum} and {@code ensemble} arguments shall be non-null.
     * The properties given in argument follow the same rules as for the
     * {@linkplain AbstractReferenceSystem#AbstractReferenceSystem(Map) super-class constructor}.
     *
     * @param  properties  the properties to be given to the coordinate reference system.
     * @param  datumType   GeoAPI interface of the datum or members of the datum ensemble.
     * @param  datum       the datum, or {@code null} if the CRS is associated only to a datum ensemble.
     * @param  ensemble    collection of reference frames which for low accuracy requirements may be considered to be
     *                     insignificantly different from each other, or {@code null} if there is no such ensemble.
     * @param  cs          the coordinate system.
     */
    AbstractSingleCRS(final Map<String,?> properties,
                      final Class<D> datumType,
                      final D datum,
                      final DatumEnsemble<D> ensemble,
                      final CoordinateSystem cs)
    {
        super(properties, cs);
        if (datum instanceof DatumEnsemble<?>) {
            throw new IllegalArgumentException(Errors.forProperties(properties)
                    .getString(Errors.Keys.IllegalArgumentClass_2, "datum", DatumEnsemble.class.getSimpleName()));
        }
        this.datum    = datum;
        this.ensemble = ensemble;
        checkDatum(properties);
    }

    /**
     * Verifies the consistency between the datum and the ensemble.
     * At least one of the {@link #datum} and {@link #ensemble} arguments shall be non-null.
     *
     * @param  properties  user-specified properties given at construction time, or {@code null} if none.
     * @throws NullPointerException if both {@link #datum} and {@link #ensemble} are null.
     * @throws IllegalArgumentException if the datum is not a member of the ensemble.
     */
    private void checkDatum(final Map<String,?> properties) {
        if (ensemble == null) {
            ArgumentChecks.ensureNonNull("datum", datum);
        } else if (datum != null) {
            for (final D member : ensemble.getMembers()) {
                if (Utilities.equalsIgnoreMetadata(datum, member)) {
                    return;
                }
            }
            throw new IllegalArgumentException(Resources.forProperties(properties)
                        .getString(Resources.Keys.NotAMemberOfDatumEnsemble_2,
                                   IdentifiedObjects.getDisplayName(ensemble),
                                   IdentifiedObjects.getDisplayName(datum)));
        } else {
            ArgumentChecks.ensureNonEmpty("ensemble", ensemble.getMembers());
        }
    }

    /**
     * Creates a new CRS derived from the specified one, but with different axis order or unit.
     *
     * @param original  the original CRS from which to derive a new one.
     * @param id        new identifier for this CRS, or {@code null} if none.
     * @param cs        coordinate system with new axis order or units of measurement.
     */
    AbstractSingleCRS(final AbstractSingleCRS<D> original, final Identifier id, final AbstractCS cs) {
        super(original, id, cs);
        datum    = original.datum;
        ensemble = original.ensemble;
    }

    /**
     * Constructs a new coordinate reference system with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * <h4>Type safety</h4>
     * This constructor shall be invoked only by subclass constructors with a method signature where
     * the <abbr>CRS</abbr> type is an interface with {@code getDatum()} and {@code getDatumEnsemble()}
     * methods overridden with return type {@code <D>}.
     *
     * @param  crs  the coordinate reference system to copy.
     */
    @SuppressWarnings("unchecked")              // See "Type safety" in above Javadoc.
    AbstractSingleCRS(final SingleCRS crs) {
        super(crs);
        datum = (D) crs.getDatum();
        if (datum instanceof DatumEnsemble<?>) {
            throw new IllegalArgumentException(
                    Errors.format(Errors.Keys.IllegalPropertyValueClass_2, "datum", DatumEnsemble.class));
        }
        ensemble = (DatumEnsemble<D>) crs.getDatumEnsemble();
        checkDatum(null);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The default implementation returns {@code SingleCRS.class}.
     * Subclasses implementing a more specific GeoAPI interface shall override this method.
     *
     * @return the coordinate reference system interface implemented by this class.
     */
    @Override
    public Class<? extends SingleCRS> getInterface() {
        return SingleCRS.class;
    }

    /**
     * Returns the datum, or {@code null} if this <abbr>CRS</abbr> is associated only to a datum ensemble.
     *
     * @return the datum, or {@code null} if none.
     */
    @Override
    public D getDatum() {
        return datum;
    }

    /**
     * Returns the datum ensemble, or {@code null} if none.
     *
     * @return the datum ensemble, or {@code null} if none.
     */
    @Override
    public DatumEnsemble<D> getDatumEnsemble() {
        return ensemble;
    }

    /**
     * Compares this coordinate reference system with the specified object for equality.
     *
     * @param  object  the object to compare to {@code this}.
     * @param  mode    whether to perform a strict or lenient comparison.
     * @return {@code true} if both objects are equal.
     *
     * @hidden because nothing new to said.
     */
    @Override
    public boolean equals(final Object object, ComparisonMode mode) {
        if (super.equals(object, mode)) {
            if (mode == ComparisonMode.STRICT) {
                final var that = (AbstractSingleCRS<?>) object;
                return Objects.equals(datum, that.datum) && Objects.equals(ensemble, that.ensemble);
            }
            final var that = (SingleCRS) object;
            final var d1   = this.getDatum();
            final var d2   = that.getDatum();
            if (mode == ComparisonMode.DEBUG) {
                mode = ComparisonMode.ALLOW_VARIANT;    // For avoiding too early `AssertionError`.
            }
            if (Utilities.deepEquals(d1, d2, mode)) {
                if (d1 != null && d2 != null && mode.allowsVariant()) {
                    return true;
                }
                return Utilities.deepEquals(getDatumEnsemble(), that.getDatumEnsemble(), mode);
            } else if (mode.allowsVariant()) {
                return DatumOrEnsemble.isLegacyDatum(this.getDatumEnsemble(), d2, mode) ||
                       DatumOrEnsemble.isLegacyDatum(that.getDatumEnsemble(), d1, mode);
            }
        }
        return false;
    }

    /**
     * Invoked by {@code hashCode()} for computing the hash code when first needed.
     *
     * @return the hash code value. This value may change in any future Apache SIS version.
     *
     * @hidden because nothing new to said.
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + Objects.hash(datum, ensemble);
    }




    /*
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                                  ┃
     ┃                               XML support with JAXB                              ┃
     ┃                                                                                  ┃
     ┃        The following methods are invoked by JAXB using reflection (even if       ┃
     ┃        they are private) or are helpers for other methods invoked by JAXB.       ┃
     ┃        Those methods can be safely removed if Geographic Markup Language         ┃
     ┃        (GML) support is not needed.                                              ┃
     ┃                                                                                  ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     */

    /**
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflection.
     */
    AbstractSingleCRS() {
        ensemble = null;
        /*
         * The coordinate system is mandatory for SIS working. We do not verify its presence here
         * because the verification would have to be done in an `afterMarshal(…)` method and throwing
         * an exception in that method causes the whole unmarshalling to fail. But the SC_CRS adapter
         * does some verifications.
         */
    }

    /**
     * Sets the datum to the given value.
     * This method is indirectly invoked by JAXB at unmarshalling time.
     *
     * @param  name  the property name, used only in case of error message to format. Can be null for auto-detect.
     * @throws IllegalStateException if the datum has already been set.
     */
    final void setDatum(final String name, final D value) {
        if (datum == null) {
            datum = value;
        } else {
            ImplementationHelper.propertyAlreadySet(AbstractSingleCRS.class, "setDatum", name);
        }
    }
}
