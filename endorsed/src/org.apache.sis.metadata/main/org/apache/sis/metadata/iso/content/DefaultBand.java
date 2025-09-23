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

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.opengis.metadata.content.Band;
import org.opengis.metadata.content.BandDefinition;
import org.opengis.metadata.content.TransferFunctionType;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.xml.bind.gco.GO_Real;
import org.apache.sis.xml.bind.gco.UnitAdapter;
import static org.apache.sis.metadata.internal.shared.ImplementationHelper.ensurePositive;

// Specific to the geoapi-4.0 branch:
import org.opengis.metadata.content.PolarisationOrientation;


/**
 * Range of wavelengths in the electromagnetic spectrum.
 * The following property is conditional (i.e. mandatory under some circumstances)
 * in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MD_Band}
 * {@code   └─units……} Units of data in each dimension included in the resource.</div>
 *
 * <h2>Limitations</h2>
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
 * @author  Cullen Rombach (Image Matters)
 * @version 1.4
 * @since   0.3
 */
@XmlType(name = "MD_Band_Type", propOrder = {
    "boundMax",
    "boundMin",
    "boundUnits",
    "peakResponse",
    "toneGradation",
    "bandBoundaryDefinition",
    "nominalSpatialResolution",
    "transferFunctionType",
    "transmittedPolarisation",
    "detectedPolarisation"
})
@XmlRootElement(name = "MD_Band")
@XmlSeeAlso(org.apache.sis.xml.bind.gmi.MI_Band.class)
public class DefaultBand extends DefaultSampleDimension implements Band {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 2492553738885938445L;

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
    @SuppressWarnings("serial")
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
    private PolarisationOrientation transmittedPolarisation;

    /**
     * Polarization of the radiation detected.
     */
    private PolarisationOrientation detectedPolarisation;

    /**
     * Constructs an initially empty band.
     */
    public DefaultBand() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * <h4>Note on properties validation</h4>
     * This constructor does not verify the property values of the given metadata (e.g. whether it contains
     * unexpected negative values). This is because invalid metadata exist in practice, and verifying their
     * validity in this copy constructor is often too late. Note that this is not the only hole, as invalid
     * metadata instances can also be obtained by unmarshalling an invalid XML document.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Band)
     */
    public DefaultBand(final Band object) {
        super(object);
        if (object != null) {
            boundMin                 = object.getBoundMin();
            boundMax                 = object.getBoundMax();
            boundUnits               = object.getBoundUnits();
            peakResponse             = object.getPeakResponse();
            toneGradation            = object.getToneGradation();
            bandBoundaryDefinition   = object.getBandBoundaryDefinition();
            transmittedPolarisation  = object.getTransmittedPolarisation();
            detectedPolarisation     = object.getDetectedPolarisation();
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
     *       {@linkplain #DefaultBand(Band) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
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
    @Override
    @ValueRange(minimum = 0)
    @XmlElement(name = "boundMin")
    @XmlJavaTypeAdapter(GO_Real.Since2014.class)
    public Double getBoundMin() {
        return boundMin;
    }

    /**
     * Sets the shortest wavelength that the sensor is capable of collecting within a designated band.
     *
     * @param  newValue  the new shortest wavelength, or {@code null}.
     * @throws IllegalArgumentException if the given value is negative.
     *
     * @since 0.5
     */
    public void setBoundMin(final Double newValue) {
        checkWritePermission(boundMin);
        if (ensurePositive(DefaultBand.class, "boundMin", false, newValue)) {
            boundMin = newValue;
        }
    }

    /**
     * Returns the longest wavelength that the sensor is capable of collecting within a designated band.
     * The units of measurement is given by {@link #getUnits()}.
     *
     * @return longest wavelength that the sensor is capable of collecting within a designated band,
     *         or {@code null} if unspecified.
     *
     * @since 0.5
     */
    @Override
    @ValueRange(minimum = 0)
    @XmlElement(name = "boundMax")
    @XmlJavaTypeAdapter(GO_Real.Since2014.class)
    public Double getBoundMax() {
        return boundMax;
    }

    /**
     * Sets the longest wavelength that the sensor is capable of collecting within a designated band.
     *
     * @param  newValue  the new longest wavelength, or {@code null}.
     * @throws IllegalArgumentException if the given value is negative.
     *
     * @since 0.5
     */
    public void setBoundMax(final Double newValue) {
        checkWritePermission(boundMax);
        if (ensurePositive(DefaultBand.class, "boundMax", false, newValue)) {
            boundMax = newValue;
        }
    }

    /**
     * Returns units in which sensor wavelengths are expressed.
     *
     * @return units in which sensor wavelengths are expressed.
     *
     * @since 0.5
     *
     * @see org.apache.sis.measure.Units#NANOMETRE
     */
    @Override
    @XmlElement(name = "boundUnits")
    @XmlJavaTypeAdapter(UnitAdapter.Since2014.class)
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
        checkWritePermission(boundUnits);
        boundUnits = newValue;
    }

    /**
     * Returns the designation of criterion for defining maximum and minimum wavelengths for a spectral band.
     *
     * @return criterion for defining maximum and minimum wavelengths, or {@code null}.
     */
    @Override
    @XmlElement(name = "bandBoundaryDefinition")
    public BandDefinition getBandBoundaryDefinition() {
        return bandBoundaryDefinition;
    }

    /**
     * Sets designation of criterion for defining maximum and minimum wavelengths for a spectral band.
     *
     * @param  newValue  the new band definition.
     */
    public void setBandBoundaryDefinition(final BandDefinition newValue) {
        checkWritePermission(bandBoundaryDefinition);
        bandBoundaryDefinition = newValue;
    }

    /**
     * Returns the wavelength at which the response is the highest.
     * The units of measurement is given by {@link #getBoundUnits()}.
     *
     * @return wavelength at which the response is the highest, or {@code null} if unspecified.
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
     * @param  newValue  the new peak response, or {@code null}.
     * @throws IllegalArgumentException if the given value is negative.
     */
    public void setPeakResponse(final Double newValue) {
        checkWritePermission(peakResponse);
        if (ensurePositive(DefaultBand.class, "peakResponse", false, newValue)) {
            peakResponse = newValue;
        }
    }

    /**
     * Returns the number of discrete numerical values in the grid data.
     *
     * @return number of discrete numerical values in the grid data, or {@code null} if none.
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
     * @param  newValue  the new tone gradation.
     */
    public void setToneGradation(final Integer newValue) {
        checkWritePermission(toneGradation);
        if (ensurePositive(DefaultBand.class, "toneGradation", false, newValue)) {
            toneGradation = newValue;
        }
    }

    /**
     * Returns the smallest distance between which separate points can be distinguished,
     * as specified in instrument design.
     *
     * @return {@inheritDoc}
     */
    @Override
    @ValueRange(minimum = 0, isMinIncluded = false)
    @XmlElement(name = "nominalSpatialResolution")
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
     * Returns type of transfer function to be used when scaling a physical value for a given element.
     *
     * @return {@inheritDoc}
     */
    @Override
    @XmlElement(name = "transferFunctionType")
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
     * @return polarization of the radiation transmitted, or {@code null}.
     */
    @Override
    @XmlElement(name = "transmittedPolarisation")
    public PolarisationOrientation getTransmittedPolarisation() {
        return transmittedPolarisation;
    }

    /**
     * Sets the polarization of the radiation transmitted.
     *
     * @param  newValue  the new transmitted polarization.
     */
    public void setTransmittedPolarisation(final PolarisationOrientation newValue) {
        checkWritePermission(transmittedPolarisation);
        transmittedPolarisation = newValue;
    }

    /**
     * Returns polarization of the radiation detected.
     *
     * @return polarization of the radiation detected, or {@code null}.
     */
    @Override
    @XmlElement(name = "detectedPolarisation")
    public PolarisationOrientation getDetectedPolarisation() {
        return detectedPolarisation;
    }

    /**
     * Sets the polarization of the radiation detected.
     *
     * @param  newValue  the new detected polarization.
     */
    public void setDetectedPolarisation(final PolarisationOrientation newValue) {
        checkWritePermission(detectedPolarisation);
        detectedPolarisation = newValue;
    }
}
