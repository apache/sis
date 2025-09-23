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

import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.content.ImageDescription;
import org.opengis.metadata.content.ImagingCondition;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.measure.ValueRange;
import static org.apache.sis.metadata.internal.shared.ImplementationHelper.ensureInRange;
import static org.apache.sis.metadata.internal.shared.ImplementationHelper.ensurePositive;


/**
 * Information about an image's suitability for use.
 * The following property is mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MD_ImageDescription}
 * {@code   └─attributeDescription……} Description of the attribute described by the measurement value.</div>
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
 * @version 1.4
 * @since   0.3
 */
@XmlType(name = "MD_ImageDescription_Type", propOrder = {
    "illuminationElevationAngle",
    "illuminationAzimuthAngle",
    "imagingCondition",
    "imageQualityCode",
    "cloudCoverPercentage",
    "processingLevel",
    "compressionGenerationQuantity",
    "triangulationIndicator",
    "radiometricCalibrationDataAvailable",
    "cameraCalibrationInformationAvailable",
    "filmDistortionInformationAvailable",
    "lensDistortionInformationAvailable"
})
@XmlRootElement(name = "MD_ImageDescription")
@XmlSeeAlso(org.apache.sis.xml.bind.gmi.MI_ImageDescription.class)
public class DefaultImageDescription extends DefaultCoverageDescription implements ImageDescription {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -239683653229623567L;

    /**
     * Illumination elevation measured in degrees clockwise from the target plane
     * at intersection of the optical line of sight with the Earth's surface.
     * For images from a scanning device, refer to the centre pixel of the image.
     */
    private Double illuminationElevationAngle;

    /**
     * Illumination azimuth measured in degrees clockwise from true north at the time the image is taken.
     * For images from a scanning device, refer to the centre pixel of the image.
     */
    private Double illuminationAzimuthAngle;

    /**
     * Conditions affected the image.
     */
    private ImagingCondition imagingCondition;

    /**
     * Specifies the image quality.
     */
    @SuppressWarnings("serial")
    private Identifier imageQualityCode;

    /**
     * Area of the dataset obscured by clouds, expressed as a percentage of the spatial extent.
     */
    private Double cloudCoverPercentage;

    /**
     * Count of the number of lossy compression cycles performed on the image.
     */
    private Integer compressionGenerationQuantity;

    /**
     * Indication of whether or not triangulation has been performed upon the image.
     * May be {@code null} is unspecified.
     */
    private Boolean triangulationIndicator;

    /**
     * Indication of whether or not the radiometric calibration information for
     * generating the radiometrically calibrated standard data product is available.
     */
    private Boolean radiometricCalibrationDataAvailable;

    /**
     * Indication of whether or not constants are available which allow for camera calibration corrections.
     */
    private Boolean cameraCalibrationInformationAvailable;

    /**
     * Indication of whether or not Calibration Reseau information is available.
     */
    private Boolean filmDistortionInformationAvailable;

    /**
     * Indication of whether or not lens aberration correction information is available.
     */
    private Boolean lensDistortionInformationAvailable;

    /**
     * Constructs an initially empty image description.
     */
    public DefaultImageDescription() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * <h4>Note on properties validation</h4>
     * This constructor does not verify the property values of the given metadata (e.g. whether
     * a value is out of range). This is because invalid metadata exist in practice, and verifying their
     * validity in this copy constructor is often too late. Note that this is not the only hole, as invalid
     * metadata instances can also be obtained by unmarshalling an invalid XML document.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(ImageDescription)
     */
    public DefaultImageDescription(final ImageDescription object) {
        super(object);
        if (object != null) {
            illuminationElevationAngle            = object.getIlluminationElevationAngle();
            illuminationAzimuthAngle              = object.getIlluminationAzimuthAngle();
            imagingCondition                      = object.getImagingCondition();
            imageQualityCode                      = object.getImageQualityCode();
            cloudCoverPercentage                  = object.getCloudCoverPercentage();
            compressionGenerationQuantity         = object.getCompressionGenerationQuantity();
            triangulationIndicator                = object.getTriangulationIndicator();
            radiometricCalibrationDataAvailable   = object.isRadiometricCalibrationDataAvailable();
            cameraCalibrationInformationAvailable = object.isCameraCalibrationInformationAvailable();
            filmDistortionInformationAvailable    = object.isFilmDistortionInformationAvailable();
            lensDistortionInformationAvailable    = object.isLensDistortionInformationAvailable();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultImageDescription}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultImageDescription} instance is created using the
     *       {@linkplain #DefaultImageDescription(ImageDescription) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultImageDescription castOrCopy(final ImageDescription object) {
        if (object == null || object instanceof DefaultImageDescription) {
            return (DefaultImageDescription) object;
        }
        return new DefaultImageDescription(object);
    }

    /**
     * Returns the illumination elevation measured in degrees clockwise from the target plane
     * at intersection of the optical line of sight with the Earth's surface.
     * For images from a scanning device, refer to the centre pixel of the image.
     *
     * <p>The horizon is at 0°, straight up has an elevation of 90°.</p>
     *
     * @return a value between -90° and +90°, or {@code null} if unspecified.
     */
    @Override
    @ValueRange(minimum = -90, maximum = +90)
    @XmlElement(name = "illuminationElevationAngle")
    public Double getIlluminationElevationAngle() {
        return illuminationElevationAngle;
    }

    /**
     * Sets the illumination elevation measured in degrees clockwise from the target plane
     * at intersection of the optical line of sight with the Earth's surface.
     * For images from a scanning device, refer to the centre pixel of the image.
     *
     * @param  newValue  the new illumination elevation angle, or {@code null}.
     * @throws IllegalArgumentException if the given value is out of range.
     */
    public void setIlluminationElevationAngle(final Double newValue) {
        checkWritePermission(illuminationElevationAngle);
        if (ensureInRange(DefaultImageDescription.class, "illuminationElevationAngle", -90, +90, newValue)) {
            illuminationElevationAngle = newValue;
        }
    }

    /**
     * Returns the illumination azimuth measured in degrees clockwise from true north at the time the image is taken.
     * For images from a scanning device, refer to the centre pixel of the image.
     *
     * @return a value between 0° and 360°, or {@code null} if unspecified.
     */
    @Override
    @ValueRange(minimum = 0, maximum = 360)
    @XmlElement(name = "illuminationAzimuthAngle")
    public Double getIlluminationAzimuthAngle() {
        return illuminationAzimuthAngle;
    }

    /**
     * Sets the illumination azimuth measured in degrees clockwise from true north at the time the image is taken.
     * For images from a scanning device, refer to the centre pixel of the image.
     *
     * @param  newValue  the new illumination azimuth angle, or {@code null}.
     * @throws IllegalArgumentException if the given value is out of range.
     */
    public void setIlluminationAzimuthAngle(final Double newValue) {
        checkWritePermission(illuminationAzimuthAngle);
        if (ensureInRange(DefaultImageDescription.class, "illuminationAzimuthAngle", 0, 360, newValue)) {
            illuminationAzimuthAngle = newValue;
        }
    }

    /**
     * Returns the conditions which affected the image.
     *
     * @return conditions which affected the image, or {@code null} if unspecified.
     */
    @Override
    @XmlElement(name = "imagingCondition")
    public ImagingCondition getImagingCondition() {
        return imagingCondition;
    }

    /**
     * Sets the conditions that affected the image.
     *
     * @param  newValue  the new imaging condition.
     */
    public void setImagingCondition(final ImagingCondition newValue) {
        checkWritePermission(imagingCondition);
        imagingCondition = newValue;
    }

    /**
     * Returns a code in producer’s codespace that specifies the image quality.
     *
     * @return the image quality, or {@code null} if unspecified.
     */
    @Override
    @XmlElement(name = "imageQualityCode")
    public Identifier getImageQualityCode() {
        return imageQualityCode;
    }

    /**
     * Sets a code in producer’s codespace that specifies the image quality.
     *
     * @param  newValue  the new image quality code.
     */
    public void setImageQualityCode(final Identifier newValue) {
        checkWritePermission(imageQualityCode);
        imageQualityCode = newValue;
    }

    /**
     * Returns the area of the dataset obscured by clouds, expressed as a percentage of the spatial extent.
     *
     * @return a value between 0 and 100, or {@code null} if unspecified.
     */
    @Override
    @ValueRange(minimum = 0, maximum = 100)
    @XmlElement(name = "cloudCoverPercentage")
    public Double getCloudCoverPercentage() {
        return cloudCoverPercentage;
    }

    /**
     * Sets the area of the dataset obscured by clouds, expressed as a percentage of the spatial extent.
     *
     * @param  newValue  the new cloud cover percentage, or {@code null}.
     * @throws IllegalArgumentException if the given value is out of range.
     */
    public void setCloudCoverPercentage(final Double newValue) {
        checkWritePermission(cloudCoverPercentage);
        if (ensureInRange(DefaultImageDescription.class, "cloudCoverPercentage", 0, 100, newValue)) {
            cloudCoverPercentage = newValue;
        }
    }

    /**
     * Returns the count of the number of lossy compression cycles performed on the image.
     *
     * @return the number of lossy compression cycles performed on the image, or {@code null} if unspecified.
     */
    @Override
    @ValueRange(minimum = 0)
    @XmlElement(name = "compressionGenerationQuantity")
    public Integer getCompressionGenerationQuantity() {
        return compressionGenerationQuantity;
    }

    /**
     * Sets the count of the number the number of lossy compression cycles performed on the image.
     *
     * @param  newValue  the new compression generation quantity.
     * @throws IllegalArgumentException if the given value is negative.
     */
    public void setCompressionGenerationQuantity(final Integer newValue) {
        checkWritePermission(compressionGenerationQuantity);
        if (ensurePositive(DefaultImageDescription.class, "compressionGenerationQuantity", false, newValue)) {
            compressionGenerationQuantity = newValue;
        }
    }

    /**
     * Returns the indication of whether or not triangulation has been performed upon the image.
     *
     * @return whether or not triangulation has been performed upon the image, or {@code null} if unspecified.
     */
    @Override
    @XmlElement(name = "triangulationIndicator")
    public Boolean getTriangulationIndicator() {
        return triangulationIndicator;
    }

    /**
     * Sets the indication of whether or not triangulation has been performed upon the image.
     *
     * @param  newValue  the new triangulation indicator.
     */
    public void setTriangulationIndicator(final Boolean newValue) {
        checkWritePermission(triangulationIndicator);
        triangulationIndicator = newValue;
    }

    /**
     * Returns the indication of whether or not the radiometric calibration information for
     * generating the radiometrically calibrated standard data product is available.
     *
     * @return whether or not the radiometric calibration information is available, or {@code null} if unspecified.
     */
    @Override
    @XmlElement(name = "radiometricCalibrationDataAvailability")
    public Boolean isRadiometricCalibrationDataAvailable() {
        return radiometricCalibrationDataAvailable;
    }

    /**
     * Sets the indication of whether or not the radiometric calibration information for generating
     * the radiometrically calibrated standard data product is available.
     *
     * @param  newValue  {@code true} if radiometric calibration data are available.
     */
    public void setRadiometricCalibrationDataAvailable(final Boolean newValue) {
        checkWritePermission(radiometricCalibrationDataAvailable);
        radiometricCalibrationDataAvailable = newValue;
    }

    /**
     * Returns the indication of whether or not constants are available which allow for camera calibration corrections.
     *
     * @return whether or not constants are available for camera calibration corrections, or {@code null} if unspecified.
     */
    @Override
    @XmlElement(name = "cameraCalibrationInformationAvailability")
    public Boolean isCameraCalibrationInformationAvailable() {
        return cameraCalibrationInformationAvailable;
    }

    /**
     * Sets the indication of whether or not constants are available which allow for camera calibration corrections.
     *
     * @param  newValue  {@code true} if camera calibration information are available.
     */
    public void setCameraCalibrationInformationAvailable(final Boolean newValue) {
        checkWritePermission(cameraCalibrationInformationAvailable);
        cameraCalibrationInformationAvailable = newValue;
    }

    /**
     * Returns the indication of whether or not Calibration Reseau information is available.
     *
     * @return whether or not Calibration Reseau information is available, or {@code null} if unspecified.
     */
    @Override
    @XmlElement(name = "filmDistortionInformationAvailability")
    public Boolean isFilmDistortionInformationAvailable() {
        return filmDistortionInformationAvailable;
    }

    /**
     * Sets the indication of whether or not Calibration Reseau information is available.
     *
     * @param  newValue  {@code true} if film distortion information are available.
     */
    public void setFilmDistortionInformationAvailable(final Boolean newValue) {
        checkWritePermission(filmDistortionInformationAvailable);
        filmDistortionInformationAvailable = newValue;
    }

    /**
     * Returns the indication of whether or not lens aberration correction information is available.
     *
     * @return whether or not lens aberration correction information is available, or {@code null} if unspecified.
     */
    @Override
    @XmlElement(name = "lensDistortionInformationAvailability")
    public Boolean isLensDistortionInformationAvailable() {
        return lensDistortionInformationAvailable;
    }

    /**
     * Sets the indication of whether or not lens aberration correction information is available.
     *
     * @param  newValue  {@code true} if lens distortion information are available.
     */
    public void setLensDistortionInformationAvailable(final Boolean newValue) {
        checkWritePermission(lensDistortionInformationAvailable);
        lensDistortionInformationAvailable = newValue;
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
     * An attribute which was defined in {@code ImageDescription} by ISO 19115:2003,
     * and which moved to the parent class in ISO 19115:2014 revision. We handle the
     * two versions separately for proper attribute ordering, and for avoiding this
     * attribute to be written for subtypes other than {@code ImageDescription}.
     */
    @XmlElement(name = "processingLevelCode", namespace = LegacyNamespaces.GMD)
    private Identifier getProcessingLevel() {
        return FilterByVersion.LEGACY_METADATA.accept() ? getProcessingLevelCode() : null;
    }

    /**
     * Invoked by JAXB at unmarshalling time.
     */
    @SuppressWarnings("unused")
    private void setProcessingLevel(final Identifier newValue) {
        setProcessingLevelCode(newValue);
    }
}
