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
package org.apache.sis.storage.geojson.binding;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import org.apache.sis.metadata.iso.distribution.DefaultDigitalTransferOptions;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.distribution.DigitalTransferOptions;
import org.opengis.metadata.distribution.Format;
import org.opengis.metadata.distribution.Medium;

/**
 * Binding of the {@code MD_DigitalTransferOptions} JSON type.
 *
 * <p>The transfer frequency is written as an ISO 8601 duration.
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1.5"
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#MD_DigitalTransferOptions</a>
 */
@JsonPropertyOrder({
    JSONMDDigitalTransferOptions.PROPERTY_UNITS_OF_DISTRIBUTION,
    JSONMDDigitalTransferOptions.PROPERTY_TRANSFER_SIZE,
    JSONMDDigitalTransferOptions.PROPERTY_ON_LINE,
    JSONMDDigitalTransferOptions.PROPERTY_OFF_LINE,
    JSONMDDigitalTransferOptions.PROPERTY_TRANSFER_FREQUENCY,
    JSONMDDigitalTransferOptions.PROPERTY_DISTRIBUTION_FORMAT
})
public final class JSONMDDigitalTransferOptions extends DataTransferObject {

    public static final String PROPERTY_UNITS_OF_DISTRIBUTION = "unitsOfDistribution";
    public static final String PROPERTY_TRANSFER_SIZE = "transferSize";
    public static final String PROPERTY_ON_LINE = "onLine";
    public static final String PROPERTY_OFF_LINE = "offLine";
    public static final String PROPERTY_TRANSFER_FREQUENCY = "transferFrequency";
    public static final String PROPERTY_DISTRIBUTION_FORMAT = "distributionFormat";

    @JsonProperty(PROPERTY_UNITS_OF_DISTRIBUTION)
    private String unitsOfDistribution;
    @JsonProperty(PROPERTY_TRANSFER_SIZE)
    private Double transferSize;
    @JsonProperty(PROPERTY_ON_LINE)
    private List<JSONMDOnlineResource> onLine;
    @JsonProperty(PROPERTY_OFF_LINE)
    private List<JSONMDMedium> offLine;
    @JsonProperty(PROPERTY_TRANSFER_FREQUENCY)
    private String transferFrequency;
    @JsonProperty(PROPERTY_DISTRIBUTION_FORMAT)
    private List<JSONMDFormat> distributionFormat;

    public JSONMDDigitalTransferOptions() {
    }

    public JSONMDDigitalTransferOptions(final DigitalTransferOptions source, final JSONMDEncoder encoder) {
        unitsOfDistribution = encoder.text(source.getUnitsOfDistribution());
        transferSize        = source.getTransferSize();
        onLine              = encoder.list(source.getOnLines(), (OnlineResource e) -> new JSONMDOnlineResource(e, encoder));
        offLine             = encoder.list(source.getOffLines(), (Medium e) -> new JSONMDMedium(e, encoder));
        transferFrequency   = encoder.duration(source.getTransferFrequency());
        distributionFormat  = encoder.list(source.getDistributionFormats(), (Format e) -> new JSONMDFormat(e, encoder));
    }

    public String getUnitsOfDistribution() {
        return unitsOfDistribution;
    }

    public void setUnitsOfDistribution(final String unitsOfDistribution) {
        this.unitsOfDistribution = unitsOfDistribution;
    }

    public Double getTransferSize() {
        return transferSize;
    }

    public void setTransferSize(final Double transferSize) {
        this.transferSize = transferSize;
    }

    public List<JSONMDOnlineResource> getOnLine() {
        return onLine;
    }

    public void setOnLine(final List<JSONMDOnlineResource> onLine) {
        this.onLine = onLine;
    }

    public List<JSONMDMedium> getOffLine() {
        return offLine;
    }

    public void setOffLine(final List<JSONMDMedium> offLine) {
        this.offLine = offLine;
    }

    public String getTransferFrequency() {
        return transferFrequency;
    }

    public void setTransferFrequency(final String transferFrequency) {
        this.transferFrequency = transferFrequency;
    }

    public List<JSONMDFormat> getDistributionFormat() {
        return distributionFormat;
    }

    public void setDistributionFormat(final List<JSONMDFormat> distributionFormat) {
        this.distributionFormat = distributionFormat;
    }

    public DefaultDigitalTransferOptions toMetadata(final JSONMDDecoder decoder) {
        final DefaultDigitalTransferOptions target = new DefaultDigitalTransferOptions();
        target.setUnitsOfDistribution(decoder.text(unitsOfDistribution));
        target.setTransferSize(transferSize);
        target.setOnLines(decoder.list(onLine, (JSONMDOnlineResource e) -> e.toMetadata(decoder)));
        target.setOffLines(decoder.list(offLine, (JSONMDMedium e) -> e.toMetadata(decoder)));
        target.setTransferFrequency(decoder.duration(transferFrequency));
        target.setDistributionFormats(decoder.list(distributionFormat, (JSONMDFormat e) -> e.toMetadata(decoder)));
        return target;
    }
}
