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

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.content.ImageDescription;
import org.opengis.metadata.content.ImagingCondition;
import org.apache.sis.measure.ValueRange;

import static org.apache.sis.internal.metadata.MetadataUtilities.getBoolean;
import static org.apache.sis.internal.metadata.MetadataUtilities.setBoolean;


/**
 * Information about an image's suitability for use.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_ImageDescription_Type", propOrder = {
    "illuminationElevationAngle",
    "illuminationAzimuthAngle",
    "imagingCondition",
    "imageQualityCode",
    "cloudCoverPercentage",
    "processingLevelCode",
    "compressionGenerationQuantity",
    "triangulationIndicator",
    "radiometricCalibrationDataAvailable",
    "cameraCalibrationInformationAvailable",
    "filmDistortionInformationAvailable",
    "lensDistortionInformationAvailable"
})
@XmlRootElement(name = "MD_ImageDescription")
@XmlSeeAlso(org.apache.sis.internal.jaxb.gmi.MI_ImageDescription.class)
public class DefaultImageDescription extends DefaultCoverageDescription implements ImageDescription {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1756867502303578674L;

    /**
     * Mask for the {@code triangulationIndicator} {@link Boolean} value.
     * Needs 2 bits since the values can be {@code true}, {@code false} or {@code null}.
     *
     * @see #booleans
     */
    private static final short TRIANGULATION_MASK = 3;

    /**
     * Mask for the {@code radiometricCalibrationDataAvailable} {@link Boolean} value.
     * Needs 2 bits since the values can be {@code true}, {@code false} or {@code null}.
     *
     * @see #booleans
     */
    private static final short RADIOMETRIC_MASK = TRIANGULATION_MASK << 2;

    /**
     * Mask for the {@code cameraCalibrationInformationAvailable} {@link Boolean} value.
     * Needs 2 bits since the values can be {@code true}, {@code false} or {@code null}.
     *
     * @see #booleans
     */
    private static final short CAMERA_MASK = RADIOMETRIC_MASK << 2;

    /**
     * Mask for the {@code filmDistortionInformationAvailable} {@link Boolean} value.
     * Needs 2 bits since the values can be {@code true}, {@code false} or {@code null}.
     *
     * @see #booleans
     */
    private static final short FILM_MASK = CAMERA_MASK << 2;

    /**
     * Mask for the {@code lensDistortionInformationAvailable} {@link Boolean} value.
     * Needs 2 bits since the values can be {@code true}, {@code false} or {@code null}.
     *
     * @see #booleans
     */
    private static final short LENS_MASK = FILM_MASK << 2;

    /**
     * Illumination elevation measured in degrees clockwise from the target plane
     * at intersection of the optical line of sight with the Earths surface.
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
    private Identifier imageQualityCode;

    /**
     * Area of the dataset obscured by clouds, expressed as a percentage of the spatial extent.
     */
    private Double cloudCoverPercentage;

    /**
     * Image distributor's code that identifies the level of radiometric and geometric
     * processing that has been applied.
     */
    private Identifier processingLevelCode;

    /**
     * Count of the number of lossy compression cycles performed on the image.
     */
    private Integer compressionGenerationQuantity;

    /**
     * The set of {@link Boolean} values. Bits are read and written using the {@code *_MASK} constants.
     *
     * @see #TRIANGULATION_MASK
     * @see #RADIOMETRIC_MASK
     * @see #CAMERA_MASK
     * @see #FILM_MASK
     * @see #LENS_MASK
     */
    private short booleans;

    /**
     * Constructs an initially empty image description.
     */
    public DefaultImageDescription() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from.
     *
     * @see #castOrCopy(ImageDescription)
     */
    public DefaultImageDescription(final ImageDescription object) {
        super(object);
        illuminationElevationAngle            = object.getIlluminationElevationAngle();
        illuminationAzimuthAngle              = object.getIlluminationAzimuthAngle();
        imagingCondition                      = object.getImagingCondition();
        imageQualityCode                      = object.getImageQualityCode();
        cloudCoverPercentage                  = object.getCloudCoverPercentage();
        processingLevelCode                   = object.getProcessingLevelCode();
        compressionGenerationQuantity         = object.getCompressionGenerationQuantity();

        int flags;
        flags = setBoolean(0,     TRIANGULATION_MASK, object.getTriangulationIndicator());
        flags = setBoolean(flags, RADIOMETRIC_MASK,   object.isRadiometricCalibrationDataAvailable());
        flags = setBoolean(flags, CAMERA_MASK,        object.isCameraCalibrationInformationAvailable());
        flags = setBoolean(flags, FILM_MASK,          object.isFilmDistortionInformationAvailable());
        flags = setBoolean(flags, LENS_MASK,          object.isLensDistortionInformationAvailable());
        booleans = (short) flags;
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultImageDescription}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultImageDescription} instance is created using the
     *       {@linkplain #DefaultImageDescription(ImageDescription) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultImageDescription castOrCopy(final ImageDescription object) {
        if (object == null || object instanceof DefaultImageDescription) {
            return (DefaultImageDescription) object;
        }
        return new DefaultImageDescription(object);
    }

    /**
     * Returns the illumination elevation measured in degrees clockwise from the target plane at
     * intersection of the optical line of sight with the Earth's surface.
     * For images from a scanning device, refer to the centre pixel of the image.
     *
     * <p>The horizon is at 0°, straight up has an elevation of 90°.</p>
     */
    @Override
    @ValueRange(minimum=0, maximum=180)
    @XmlElement(name = "illuminationElevationAngle")
    public Double getIlluminationElevationAngle() {
        return illuminationElevationAngle;
    }

    /**
     * Sets the illumination elevation measured in degrees clockwise from the target plane at
     * intersection of the optical line of sight with the Earth's surface. For images from a
     * scanning device, refer to the centre pixel of the image.
     *
     * @param newValue The new illumination elevation angle.
     */
    public void setIlluminationElevationAngle(final Double newValue) {
        checkWritePermission();
        illuminationElevationAngle = newValue;
    }

    /**
     * Returns the illumination azimuth measured in degrees clockwise from true north at the time
     * the image is taken. For images from a scanning device, refer to the centre pixel of the
     * image.
     */
    @Override
    @ValueRange(minimum=0, maximum=360)
    @XmlElement(name = "illuminationAzimuthAngle")
    public Double getIlluminationAzimuthAngle() {
        return illuminationAzimuthAngle;
    }

    /**
     * Sets the illumination azimuth measured in degrees clockwise from true north at the time the
     * image is taken. For images from a scanning device, refer to the centre pixel of the image.
     *
     * @param newValue The new illumination azimuth angle.
     */
    public void setIlluminationAzimuthAngle(final Double newValue) {
        checkWritePermission();
        illuminationAzimuthAngle = newValue;
    }

    /**
     * Returns the conditions affected the image.
     */
    @Override
    @XmlElement(name = "imagingCondition")
    public ImagingCondition getImagingCondition() {
        return imagingCondition;
    }

    /**
     * Sets the conditions affected the image.
     *
     * @param newValue The new imaging condition.
     */
    public void setImagingCondition(final ImagingCondition newValue) {
        checkWritePermission();
        imagingCondition = newValue;
    }

    /**
     * Returns the identifier that specifies the image quality.
     */
    @Override
    @XmlElement(name = "imageQualityCode")
    public Identifier getImageQualityCode() {
        return imageQualityCode;
    }

    /**
     * Sets the identifier that specifies the image quality.
     *
     * @param newValue The new image quality code.
     */
    public void setImageQualityCode(final Identifier newValue) {
        checkWritePermission();
        imageQualityCode = newValue;
    }

    /**
     * Returns the area of the dataset obscured by clouds, expressed as a percentage of the spatial extent.
     */
    @Override
    @ValueRange(minimum=0, maximum=100)
    @XmlElement(name = "cloudCoverPercentage")
    public Double getCloudCoverPercentage() {
        return cloudCoverPercentage;
    }

    /**
     * Sets the area of the dataset obscured by clouds, expressed as a percentage of the spatial extent.
     *
     * @param newValue The new cloud cover percentage.
     */
    public void setCloudCoverPercentage(final Double newValue) {
        checkWritePermission();
        cloudCoverPercentage = newValue;
    }

    /**
     * Returns the image distributor's code that identifies the level of radiometric and geometric
     * processing that has been applied.
     */
    @Override
    @XmlElement(name = "processingLevelCode")
    public Identifier getProcessingLevelCode() {
        return processingLevelCode;
    }

    /**
     * Sets the image distributor's code that identifies the level of radiometric and geometric
     * processing that has been applied.
     *
     * @param newValue The new processing level code.
     */
    public void setProcessingLevelCode(final Identifier newValue) {
        checkWritePermission();
        processingLevelCode = newValue;
    }

    /**
     * Returns the count of the number of lossy compression cycles performed on the image.
     */
    @Override
    @ValueRange(minimum=0)
    @XmlElement(name = "compressionGenerationQuantity")
    public Integer getCompressionGenerationQuantity() {
        return compressionGenerationQuantity;
    }

    /**
     * Sets the count of the number the number of lossy compression cycles performed on the image.
     *
     * @param newValue The new compression generation quantity.
     */
    public void setCompressionGenerationQuantity(final Integer newValue) {
        checkWritePermission();
        compressionGenerationQuantity = newValue;
    }

    /**
     * Returns the indication of whether or not triangulation has been performed upon the image.
     */
    @Override
    @XmlElement(name = "triangulationIndicator")
    public Boolean getTriangulationIndicator() {
        return getBoolean(booleans, TRIANGULATION_MASK);
    }

    /**
     * Sets the indication of whether or not triangulation has been performed upon the image.
     *
     * @param newValue The new triangulation indicator.
     */
    public void setTriangulationIndicator(final Boolean newValue) {
        checkWritePermission();
        booleans = (short) setBoolean(booleans, TRIANGULATION_MASK, newValue);
    }

    /**
     * Returns the indication of whether or not the radiometric calibration information for
     * generating the radiometrically calibrated standard data product is available.
     */
    @Override
    @XmlElement(name = "radiometricCalibrationDataAvailability")
    public Boolean isRadiometricCalibrationDataAvailable() {
        return getBoolean(booleans, RADIOMETRIC_MASK);
    }

    /**
     * Sets the indication of whether or not the radiometric calibration information for generating
     * the radiometrically calibrated standard data product is available.
     *
     * @param newValue {@code true} if radiometric calibration data are available.
     */
    public void setRadiometricCalibrationDataAvailable(final Boolean newValue) {
        checkWritePermission();
        booleans = (short) setBoolean(booleans, RADIOMETRIC_MASK, newValue);
    }

    /**
     * Returns the indication of whether or not constants are available which allow for camera
     * calibration corrections.
     */
    @Override
    @XmlElement(name = "cameraCalibrationInformationAvailability")
    public Boolean isCameraCalibrationInformationAvailable() {
        return getBoolean(booleans, CAMERA_MASK);
    }

    /**
     * Sets the indication of whether or not constants are available which allow for camera
     * calibration corrections.
     *
     * @param newValue {@code true} if camera calibration information are available.
     */
    public void setCameraCalibrationInformationAvailable(final Boolean newValue) {
        checkWritePermission();
        booleans = (short) setBoolean(booleans, CAMERA_MASK, newValue);
    }

    /**
     * Returns the indication of whether or not Calibration Reseau information is available.
     */
    @Override
    @XmlElement(name = "filmDistortionInformationAvailability")
    public Boolean isFilmDistortionInformationAvailable() {
        return getBoolean(booleans, FILM_MASK);
    }

    /**
     * Sets the indication of whether or not Calibration Reseau information is available.
     *
     * @param newValue {@code true} if film distortion information are available.
     */
    public void setFilmDistortionInformationAvailable(final Boolean newValue) {
        checkWritePermission();
        booleans = (short) setBoolean(booleans, FILM_MASK, newValue);
    }

    /**
     * Returns the indication of whether or not lens aberration correction information is available.
     */
    @Override
    @XmlElement(name = "lensDistortionInformationAvailability")
    public Boolean isLensDistortionInformationAvailable() {
        return getBoolean(booleans, LENS_MASK);
    }

    /**
     * Sets the indication of whether or not lens aberration correction information is available.
     *
     * @param newValue {@code true} if lens distortion information are available.
     */
    public void setLensDistortionInformationAvailable(final Boolean newValue) {
        checkWritePermission();
        booleans = (short) setBoolean(booleans, LENS_MASK, newValue);
    }
}
