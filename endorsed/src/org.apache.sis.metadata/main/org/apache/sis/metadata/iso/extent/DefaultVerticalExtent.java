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
package org.apache.sis.metadata.iso.extent;

import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.metadata.extent.VerticalExtent;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.xml.NilReason;
import org.apache.sis.xml.bind.gco.GO_Real;
import org.apache.sis.metadata.privy.ReferencingServices;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.resources.Errors;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coordinate.MismatchedCoordinateMetadataException;


/**
 * Vertical domain of dataset.
 * The following properties are mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code EX_VerticalExtent}
 * {@code   ├─minimumValue……} The lowest vertical extent contained in the dataset.
 * {@code   ├─maximumValue……} The highest vertical extent contained in the dataset.
 * {@code   └─verticalCRS………} Information about the vertical coordinate reference system to which the maximum and minimum elevation values are measured. The CRS identification includes unit of measure.</div>
 *
 * In addition to the standard properties, SIS provides the following methods:
 * <ul>
 *   <li>{@link #setBounds(Envelope)} for setting the extent from the given envelope.</li>
 * </ul>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 *   <li>Coordinate Reference System cannot be specified by identifier only; they have to be specified in full.
 *       See <a href="https://issues.apache.org/jira/browse/SIS-397">SIS-397</a>.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @version 1.4
 * @since   0.3
 */
@XmlType(name = "EX_VerticalExtent_Type", propOrder = {
    "minimumValue",
    "maximumValue",
    "verticalCRS"
})
@XmlRootElement(name = "EX_VerticalExtent")
public class DefaultVerticalExtent extends ISOMetadata implements VerticalExtent {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1963873471175296153L;

    /**
     * The lowest vertical extent contained in the dataset.
     */
    private Double minimumValue;

    /**
     * The highest vertical extent contained in the dataset.
     */
    private Double maximumValue;

    /**
     * Provides information about the vertical coordinate reference system
     * to which the maximum and minimum elevation values are measured.
     * The <abbr>CRS</abbr> identification includes unit of measure.
     *
     * @see #getVerticalCRS()
     */
    @SuppressWarnings("serial")     // Apache SIS implementations are serializable.
    private VerticalCRS verticalCRS;

    /**
     * Constructs an initially empty vertical extent.
     */
    public DefaultVerticalExtent() {
    }

    /**
     * Creates a vertical extent initialized to the specified values.
     *
     * @param minimumValue  the lowest vertical extent contained in the dataset, or {@link Double#NaN} if none.
     * @param maximumValue  the highest vertical extent contained in the dataset, or {@link Double#NaN} if none.
     * @param verticalCRS   the information about the vertical coordinate reference system, or {@code null}.
     */
    public DefaultVerticalExtent(final double minimumValue,
                                 final double maximumValue,
                                 final VerticalCRS verticalCRS)
    {
        if (!Double.isNaN(minimumValue)) this.minimumValue = minimumValue;
        if (!Double.isNaN(maximumValue)) this.maximumValue = maximumValue;
        this.verticalCRS = verticalCRS;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(VerticalExtent)
     */
    public DefaultVerticalExtent(final VerticalExtent object) {
        super(object);
        if (object != null) {
            minimumValue = object.getMinimumValue();
            maximumValue = object.getMaximumValue();
            verticalCRS  = object.getVerticalCRS();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultVerticalExtent}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultVerticalExtent} instance is created using the
     *       {@linkplain #DefaultVerticalExtent(VerticalExtent) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultVerticalExtent castOrCopy(final VerticalExtent object) {
        if (object == null || object instanceof DefaultVerticalExtent) {
            return (DefaultVerticalExtent) object;
        }
        return new DefaultVerticalExtent(object);
    }

    /**
     * Returns the lowest vertical extent contained in the dataset.
     *
     * @return the lowest vertical extent, or {@code null}.
     */
    @Override
    @XmlElement(name = "minimumValue", required = true)
    @XmlJavaTypeAdapter(GO_Real.class)
    public Double getMinimumValue() {
        return minimumValue;
    }

    /**
     * Sets the lowest vertical extent contained in the dataset.
     *
     * @param  newValue  the new minimum value.
     */
    public void setMinimumValue(final Double newValue) {
        checkWritePermission(minimumValue);
        minimumValue = newValue;
    }

    /**
     * Returns the highest vertical extent contained in the dataset.
     *
     * @return the highest vertical extent, or {@code null}.
     */
    @Override
    @XmlElement(name = "maximumValue", required = true)
    @XmlJavaTypeAdapter(GO_Real.class)
    public Double getMaximumValue() {
        return maximumValue;
    }

    /**
     * Sets the highest vertical extent contained in the dataset.
     *
     * @param  newValue  the new maximum value.
     */
    public void setMaximumValue(final Double newValue) {
        checkWritePermission(maximumValue);
        maximumValue = newValue;
    }

    /**
     * Provides information about the vertical coordinate reference system
     * to which the maximum and minimum elevation values are measured.
     * The <abbr>CRS</abbr> identification includes unit of measure.
     *
     * @return the vertical <abbr>CRS</abbr>, or {@code null}.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-397">SIS-397</a>
     */
    @Override
    @XmlElement(name = "verticalCRS")
    public VerticalCRS getVerticalCRS() {
        return verticalCRS;
    }

    /**
     * Sets the information about the vertical coordinate reference system to
     * which the maximum and minimum elevation values are measured.
     *
     * @param  newValue  the new vertical CRS.
     */
    public void setVerticalCRS(final VerticalCRS newValue) {
        checkWritePermission(verticalCRS);
        verticalCRS = newValue;
    }

    /**
     * Returns an arbitrary value, or {@code null} if both minimum and maximum are null.
     * This is used for verifying if the bounds are already set or partially set.
     */
    private Double value() {
        return (minimumValue != null) ? minimumValue : maximumValue;
    }

    /**
     * Sets this vertical extent to values inferred from the specified envelope. The envelope can
     * be multi-dimensional, in which case the {@linkplain Envelope#getCoordinateReferenceSystem()
     * envelope CRS} must have a vertical component.
     *
     * <p><b>Note:</b> this method is available only if the referencing module is on the module path.</p>
     *
     * @param  envelope  the envelope to use for setting this vertical extent.
     * @throws UnsupportedOperationException if the referencing module is not on the module path.
     * @throws TransformException if the envelope cannot be transformed to a vertical extent.
     *
     * @see DefaultExtent#addElements(Envelope)
     * @see DefaultGeographicBoundingBox#setBounds(Envelope)
     * @see DefaultTemporalExtent#setBounds(Envelope)
     */
    public void setBounds(final Envelope envelope) throws TransformException {
        checkWritePermission(value());
        if (!ReferencingServices.getInstance().setBounds(envelope, this)) {
            throw new NotSpatioTemporalException(1, envelope);
        }
    }

    /**
     * Sets this vertical extent to the intersection of this extent with the specified one.
     * The {@linkplain org.apache.sis.referencing.crs.DefaultVerticalCRS#getDatum() vertical datum}
     * must be the same (ignoring metadata) for both extents; this method does not perform datum shift.
     * However, this method can perform unit conversions.
     *
     * <p>If there is no intersection between the two extents, then this method sets both minimum and
     * maximum values to {@linkplain Double#NaN}. If either this extent or the specified extent has NaN
     * bounds, then the corresponding bounds of the intersection result will also be NaN.</p>
     *
     * @param  other  the vertical extent to intersect with this extent.
     * @throws MismatchedCoordinateMetadataException if the two extents do not use the same datum, ignoring metadata.
     *
     * @see Extents#intersection(VerticalExtent, VerticalExtent)
     * @see org.apache.sis.geometry.GeneralEnvelope#intersect(Envelope)
     *
     * @since 0.8
     */
    public void intersect(final VerticalExtent other) throws MismatchedCoordinateMetadataException {
        checkWritePermission(value());
        Double min = other.getMinimumValue();
        Double max = other.getMaximumValue();
        try {
            final MathTransform1D cv = getConversionFrom(other.getVerticalCRS());
            if (isReversing(cv, min, max)) {
                Double tmp = min;
                min = max;
                max = tmp;
            }
            /*
             * If minimumValue is NaN, keep it unchanged (because x > minimumValue is false)
             * in order to preserve the NilReason. Conversely if min is NaN, then we want to
             * take it without conversion for preserving its NilReason.
             */
            if (min != null) {
                if (minimumValue == null || min.isNaN() || (min = convert(cv, min)) > minimumValue) {
                    minimumValue = min;
                }
            }
            if (max != null) {
                if (maximumValue == null || max.isNaN() || (max = convert(cv, max)) < maximumValue) {
                    maximumValue = max;
                }
            }
        } catch (UnsupportedOperationException | FactoryException | ClassCastException | TransformException e) {
            throw new MismatchedCoordinateMetadataException(Errors.format(Errors.Keys.IncompatiblePropertyValue_1, "verticalCRS"), e);
        }
        if (minimumValue != null && maximumValue != null && minimumValue > maximumValue) {
            minimumValue = maximumValue = NilReason.MISSING.createNilObject(Double.class);
        }
    }

    /**
     * Returns the conversion from the given CRS to the CRS of this extent, or {@code null} if none or unknown.
     * The returned {@code MathTransform1D} may apply unit conversions or axis direction reversal, but usually
     * not datum shift.
     *
     * @param  source  the CRS from which to perform the conversions, or {@code null} if unknown.
     * @return the conversion from {@code source}, or {@code null} if none or unknown.
     * @throws UnsupportedOperationException if the {@code org.apache.sis.referencing} module is not on the module path.
     * @throws FactoryException if the coordinate operation factory is not available.
     * @throws ClassCastException if the conversion is not an instance of {@link MathTransform1D}.
     */
    private MathTransform1D getConversionFrom(final VerticalCRS source) throws FactoryException {
        if (source != null && verticalCRS != null && !Utilities.deepEquals(verticalCRS, source, ComparisonMode.COMPATIBILITY)) {
            final MathTransform1D cv = ReferencingServices.getInstance().findTransform(source, verticalCRS);
            if (!cv.isIdentity()) {
                return cv;
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if the given conversion seems to change the axis direction.
     * This happen for example with conversions from "Elevation" axis to "Depth" axis.
     * In case of doubt, this method returns {@code false}.
     *
     * <h4>Alternatives</h4>
     * We could compare axis directions instead, but it would not work with user-defined directions
     * or user-defined unit conversions with negative scale factor (should never happen, but we are
     * paranoiac). We could compare the minimum and maximum values after conversions, but it would
     * not work if one or both values are {@code null} or {@code NaN}. Since we want to preserve
     * {@link NilReason}, we still need to know if axes are reversed in order to put the nil reason
     * in the right location.
     *
     * @param  cv      the conversion computed by {@link #getConversionFrom(VerticalCRS)} (may be {@code null}).
     * @param  sample  the minimum or the maximum value.
     * @param  other   the minimum or maximum value at the opposite bound.
     * @return {@code true} if the axis direction is reversed at the given value.
     */
    private static boolean isReversing(final MathTransform1D cv, Double sample, final Double other) throws TransformException {
        if (cv == null) {
            return false;
        }
        if (sample == null || sample.isNaN()) {
            sample = other;
        } else if (other != null && !other.isNaN()) {
            sample = (sample + other) / 2;
        }
        return MathFunctions.isNegative(cv.derivative(sample != null ? sample : Double.NaN));
    }

    /**
     * Converts the given value with the given transform if non-null. This converter can generally
     * not perform datum shift; the operation is merely unit conversion and change of axis direction.
     */
    private static Double convert(MathTransform1D tr, Double value) throws TransformException {
        return (tr != null) ? tr.transform(value) : value;
    }
}
