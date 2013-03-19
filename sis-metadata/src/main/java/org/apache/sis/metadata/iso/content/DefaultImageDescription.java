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

import org.opengis.metadata.Identifier;
import org.opengis.metadata.content.ImageDescription;
import org.opengis.metadata.content.ImagingCondition;

public class DefaultImageDescription extends DefaultCoverageDescription implements ImageDescription {

    private Double illuminationElevationAngle;

    private Double illuminationAzimuthAngle;

    private ImagingCondition imagingCondition;

    private Identifier imageQualityCode;

    private Double cloudCoverPercentage;

    private Identifier processingLevelCode;

    private Integer compressionGenerationQuantity;

    private Boolean triangulationIndicator;

    private Boolean radiometricCalibrationDataAvailable;

    private Boolean cameraCalibrationInformationAvailable;

    private Boolean filmDistortionInformationAvailable;

    private Boolean lensDistortionInformationAvailable;

    @Override
    public synchronized Double getIlluminationElevationAngle() {
        return illuminationElevationAngle;
    }

    @Override
    public synchronized Double getIlluminationAzimuthAngle() {
        return illuminationAzimuthAngle;
    }

    @Override
    public synchronized ImagingCondition getImagingCondition() {
        return imagingCondition;
    }

    @Override
    public synchronized Identifier getImageQualityCode() {
        return imageQualityCode;
    }

    @Override
    public synchronized Double getCloudCoverPercentage() {
        return cloudCoverPercentage;
    }

    @Override
    public synchronized Identifier getProcessingLevelCode() {
        return processingLevelCode;
    }

    @Override
    public synchronized Integer getCompressionGenerationQuantity() {
        return compressionGenerationQuantity;
    }

    @Override
    public synchronized Boolean getTriangulationIndicator() {
        return triangulationIndicator;
    }

    @Override
    public synchronized Boolean isRadiometricCalibrationDataAvailable() {
        return radiometricCalibrationDataAvailable;
    }

    @Override
    public synchronized Boolean isCameraCalibrationInformationAvailable() {
        return cameraCalibrationInformationAvailable;
    }

    @Override
    public synchronized Boolean isFilmDistortionInformationAvailable() {
        return filmDistortionInformationAvailable;
    }

    @Override
    public synchronized Boolean isLensDistortionInformationAvailable() {
        return lensDistortionInformationAvailable;
    }
}
