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
package org.apache.sis.metadata.iso.content;

import javax.measure.unit.Unit;
import javax.measure.quantity.Length;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import org.opengis.annotation.UML;
import org.opengis.metadata.content.Band;
import org.opengis.metadata.content.BandDefinition;
import org.opengis.metadata.content.PolarizationOrientation;
import org.opengis.metadata.content.TransferFunctionType;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.measure.ValueRange;

import static org.apache.sis.internal.metadata.MetadataUtilities.ensurePositive;

// Branch-specific imports
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Range of wavelengths in the electromagnetic spectrum.
 *
 * <p><b>Limitations:</b></p>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
@XmlType(name = "MD_Band_Type", propOrder = {
    "peakResponse",
    "bitsPerValue",
    "toneGradation",
    "scaleFactor",
    "offset",
    "bandBoundaryDefinition",
    "nominalSpatialResolution",
    "transferFunctionType",
    "transmittedPolarization",
    "detectedPolarization"
})
@XmlRootElement(name = "MD_Band")
@XmlSeeAlso(org.apache.sis.internal.jaxb.gmi.MI_Band.class)
public class DefaultBand extends DefaultSampleDimension implements Band {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2474871120376144737L;

    /**
     * Shortest wavelength that the sensor is capable of collecting within a designated band.
     */
    private Double boundMin;

    /**
     * Longest wavelength that the sensor is capable of collecting within a designated band.
     */
    private Double boundMax;

    /**
     * Units in which sensor wavelengths are expressed.
     */
    private Unit<Length> boundUnits;

    /**
     * Designation of criterion for defining maximum and minimum wavelengths for a spectral band.
     */
    private BandDefinition bandBoundaryDefinition;

    /**
     * Wavelength at which the response is the highest.
     */
    private Double peakResponse;

    /**
     * Number of discrete numerical values in the grid data.
     */
    private Integer toneGradation;

    /**
     * Polarization of the radiation transmitted.
     */
    private PolarizationOrientation transmittedPolarization;

    /**
     * Polarization of the radiation detected.
     */
    private PolarizationOrientation detectedPolarization;

    /**
     * Constructs an initially empty band.
     */
    public DefaultBand() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * <div class="note"><b>Note on properties validation:</b>
     * This constructor does not verify the property values of the given metadata (e.g. whether it contains
     * unexpected negative values). This is because invalid metadata exist in practice, and verifying their
     * validity in this copy constructor is often too late. Note that this is not the only hole, as invalid
     * metadata instances can also be obtained by unmarshalling an invalid XML document.
     * </div>
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Band)
     */
    public DefaultBand(final Band object) {
        super(object);
        if (object != null) {
            if (object instanceof DefaultBand) {
                final DefaultBand c = (DefaultBand) object;
                boundMin   = c.getBoundMin();
                boundMax   = c.getBoundMax();
                boundUnits = c.getBoundUnits();
            }
            peakResponse             = object.getPeakResponse();
            toneGradation            = object.getToneGradation();
            bandBoundaryDefinition   = object.getBandBoundaryDefinition();
            transmittedPolarization  = object.getTransmittedPolarization();
            detectedPolarization     = object.getDetectedPolarization();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultBand}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultBand} instance is created using the
     *       {@linkplain #DefaultBand(Band) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultBand castOrCopy(final Band object) {
        if (object == null || object instanceof DefaultBand) {
            return (DefaultBand) object;
        }
        return new DefaultBand(object);
    }

    /**
     * Returns the shortest wavelength that the sensor is capable of collecting within a designated band.
     * The units of measurement is given by {@link #getBoundUnits()}.
     *
     * @return Shortest wavelength that the sensor is capable of collecting within a designated band,
     *         or {@code null} if unspecified.
     *
     * @since 0.5
     */
    @ValueRange(minimum = 0)
/// @XmlElement(name = "boundMin")
    @UML(identifier="boundMin", obligation=OPTIONAL, specification=ISO_19115)
    public Double getBoundMin() {
        return boundMin;
    }

    /**
     * Sets the shortest wavelength that the sensor is capable of collecting within a designated band.
     *
     * @param newValue The new shortest wavelength, or {@code null}.
     * @throws IllegalArgumentException if the given value is negative.
     *
     * @since 0.5
     */
    public void setBoundMin(final Double newValue) {
        checkWritePermission();
        if (ensurePositive(DefaultBand.class, "boundMin", false, newValue)) {
            boundMin = newValue;
        }
    }

    /**
     * Returns the longest wavelength that the sensor is capable of collecting within a designated band.
     * The units of measurement is given by {@link #getUnits()}.
     *
     * @return Longest wavelength that the sensor is capable of collecting within a designated band,
     *         or {@code null} if unspecified.
     *
     * @since 0.5
     */
    @ValueRange(minimum = 0)
/// @XmlElement(name = "boundMax")
    @UML(identifier="boundMax", obligation=OPTIONAL, specification=ISO_19115)
    public Double getBoundMax() {
        return boundMax;
    }

    /**
     * Sets the longest wavelength that the sensor is capable of collecting within a designated band.
     *
     * @param newValue The new longest wavelength, or {@code null}.
     * @throws IllegalArgumentException if the given value is negative.
     *
     * @since 0.5
     */
    public void setBoundMax(final Double newValue) {
        checkWritePermission();
        if (ensurePositive(DefaultBand.class, "boundMax", false, newValue)) {
            boundMax = newValue;
        }
    }

    /**
     * Returns units in which sensor wavelengths are expressed.
     *
     * @return Units in which sensor wavelengths are expressed.
     *
     * @since 0.5
     */
/// @XmlElement(name = "boundUnits")
    @UML(identifier="boundUnits", obligation=OPTIONAL, specification=ISO_19115)
    public Unit<Length> getBoundUnits() {
        return boundUnits;
    }

    /**
     * Sets a new units in which sensor wavelengths are expressed.
     *
     * @param newValue the new unit.
     *
     * @since 0.5
     */
    public void setBoundUnits(final Unit<Length> newValue) {
        checkWritePermission();
        boundUnits = newValue;
    }

    /**
     * Returns the designation of criterion for defining maximum and minimum wavelengths for a spectral band.
     *
     * @return Criterion for defining maximum and minimum wavelengths, or {@code null}.
     */
    @Override
    @XmlElement(name = "bandBoundaryDefinition", namespace = Namespaces.GMI)
    public BandDefinition getBandBoundaryDefinition() {
        return bandBoundaryDefinition;
    }

    /**
     * Sets designation of criterion for defining maximum and minimum wavelengths for a spectral band.
     *
     * @param newValue The new band definition.
     */
    public void setBandBoundaryDefinition(final BandDefinition newValue) {
        checkWritePermission();
        bandBoundaryDefinition = newValue;
    }

    /**
     * Returns the units of data as a unit of length.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, the units of wavelength is rather {@code boundUnits}.
     * The restriction for units of length in this {@code units} property may be relaxed in GeoAPI 4.0.
     * </div>
     *
     * @return The units of data.
     */
    @Override
    public Unit<Length> getUnits() {
        final Unit<?> units = super.getUnits();
        return (units != null) ? units.asType(Length.class) : null;
    }

    /**
     * Sets the units of data as a unit of length.
     *
     * <div class="warning"><b>Upcoming precondition change — relaxation</b><br>
     * The current implementation requires the unit to be an instance of {@code Unit<Length>},
     * otherwise a {@link ClassCastException} is thrown. This is because the value returned by
     * {@link #getUnits()} was restricted by ISO 19115:2003 to units of length.
     * However this restriction may be relaxed in GeoAPI 4.0.
     * </div>
     *
     * @param newValue The new units of data as an instance of {@code Unit<Length>}.
     */
    @Override
    public void setUnits(final Unit<?> newValue) {
        super.setUnits(newValue.asType(Length.class));
    }

    /**
     * Returns the wavelength at which the response is the highest.
     * The units of measurement is given by {@link #getUnits()}.
     *
     * @return Wavelength at which the response is the highest, or {@code null} if unspecified.
     */
    @Override
    @ValueRange(minimum = 0)
    @XmlElement(name = "peakResponse")
    public Double getPeakResponse() {
        return peakResponse;
    }

    /**
     * Sets the wavelength at which the response is the highest.
     *
     * @param newValue The new peak response, or {@code null}.
     * @throws IllegalArgumentException if the given value is negative.
     */
    public void setPeakResponse(final Double newValue) {
        checkWritePermission();
        if (ensurePositive(DefaultBand.class, "peakResponse", false, newValue)) {
            peakResponse = newValue;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ValueRange(minimum = 1)
    @XmlElement(name = "bitsPerValue")
    public Integer getBitsPerValue() {
        return super.getBitsPerValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBitsPerValue(final Integer newValue) {
        super.setBitsPerValue(newValue);
    }

    /**
     * Returns the number of discrete numerical values in the grid data.
     *
     * @return Number of discrete numerical values in the grid data, or {@code null} if none.
     */
    @Override
    @ValueRange(minimum = 0)
    @XmlElement(name = "toneGradation")
    public Integer getToneGradation() {
        return toneGradation;
    }

    /**
     * Sets the number of discrete numerical values in the grid data.
     *
     * @param newValue The new tone gradation.
     */
    public void setToneGradation(final Integer newValue) {
        checkWritePermission();
        if (ensurePositive(DefaultBand.class, "toneGradation", false, newValue)) {
            toneGradation = newValue;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @XmlElement(name = "scaleFactor")
    public Double getScaleFactor() {
        return super.getScaleFactor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setScaleFactor(final Double newValue) {
        super.setScaleFactor(newValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @XmlElement(name = "offset")
    public Double getOffset() {
        return super.getOffset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOffset(final Double newValue) {
        super.setOffset(newValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ValueRange(minimum = 0, isMinIncluded = false)
    @XmlElement(name = "nominalSpatialResolution", namespace = Namespaces.GMI)
    public Double getNominalSpatialResolution() {
        return super.getNominalSpatialResolution();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNominalSpatialResolution(final Double newValue) {
        super.setNominalSpatialResolution(newValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @XmlElement(name = "transferFunctionType", namespace = Namespaces.GMI)
    public TransferFunctionType getTransferFunctionType() {
        return super.getTransferFunctionType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTransferFunctionType(final TransferFunctionType newValue) {
        super.setTransferFunctionType(newValue);
    }

    /**
     * Returns the polarization of the radiation transmitted.
     *
     * @return Polarization of the radiation transmitted, or {@code null}.
     */
    @Override
    @XmlElement(name = "transmittedPolarization", namespace = Namespaces.GMI)
    public PolarizationOrientation getTransmittedPolarization() {
        return transmittedPolarization;
    }

    /**
     * Sets the polarization of the radiation transmitted.
     *
     * @param newValue The new transmitted polarization.
     */
    public void setTransmittedPolarization(final PolarizationOrientation newValue) {
        checkWritePermission();
        transmittedPolarization = newValue;
    }

    /**
     * Returns polarization of the radiation detected.
     *
     * @return Polarization of the radiation detected, or {@code null}.
     */
    @Override
    @XmlElement(name = "detectedPolarization", namespace = Namespaces.GMI)
    public PolarizationOrientation getDetectedPolarization() {
        return detectedPolarization;
    }

    /**
     * Sets the polarization of the radiation detected.
     *
     * @param newValue The new detected polarization.
     */
    public void setDetectedPolarization(final PolarizationOrientation newValue) {
        checkWritePermission();
        detectedPolarization = newValue;
    }
}
