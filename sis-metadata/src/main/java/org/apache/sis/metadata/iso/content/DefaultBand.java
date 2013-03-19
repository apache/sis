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

import org.opengis.metadata.content.Band;
import org.opengis.metadata.content.BandDefinition;
import org.opengis.metadata.content.PolarizationOrientation;
import org.opengis.metadata.content.TransferFunctionType;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

public class DefaultBand extends DefaultRangeDimension implements Band {

    private Double maxValue;

    private Double minValue;

    private Double peakResponse;

    private Unit<Length> units;

    private Integer bitsPerValue;

    private Integer toneGradation;

    private Double scaleFactor;

    private Double offset;

    private BandDefinition bandBoundaryDefinition;

    private Double nominalSpatialResolution;

    private TransferFunctionType transferFunctionType;

    private PolarizationOrientation transmittedPolarization;

    private PolarizationOrientation detectedPolarization;

    @Override
    public synchronized Double getMaxValue() {
        return maxValue;
    }

    @Override
    public synchronized Double getMinValue() {
        return minValue;
    }

    @Override
    public synchronized Double getPeakResponse() {
        return peakResponse;
    }

    @Override
    public synchronized Unit<Length> getUnits() {
        return units;
    }

    @Override
    public synchronized Integer getBitsPerValue() {
        return bitsPerValue;
    }

    @Override
    public synchronized Integer getToneGradation() {
        return toneGradation;
    }

    @Override
    public synchronized Double getScaleFactor() {
        return scaleFactor;
    }

    @Override
    public synchronized Double getOffset() {
        return offset;
    }

    @Override
    public synchronized BandDefinition getBandBoundaryDefinition() {
        return bandBoundaryDefinition;
    }

    @Override
    public synchronized Double getNominalSpatialResolution() {
        return nominalSpatialResolution;
    }

    @Override
    public synchronized TransferFunctionType getTransferFunctionType() {
        return transferFunctionType;
    }

    @Override
    public synchronized PolarizationOrientation getTransmittedPolarization() {
        return transmittedPolarization;
    }

    @Override
    public synchronized PolarizationOrientation getDetectedPolarization() {
        return detectedPolarization;
    }
}
