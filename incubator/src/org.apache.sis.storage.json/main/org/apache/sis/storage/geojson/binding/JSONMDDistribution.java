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
import org.apache.sis.metadata.iso.distribution.DefaultDistribution;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.distribution.DigitalTransferOptions;
import org.opengis.metadata.distribution.Distribution;
import org.opengis.metadata.distribution.Distributor;
import org.opengis.metadata.distribution.Format;

/**
 * Binding of the {@code MD_Distribution} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#MD_Distribution</a>
 */
@JsonPropertyOrder({
    JSONMDDistribution.PROPERTY_DESCRIPTION,
    JSONMDDistribution.PROPERTY_DISTRIBUTION_FORMAT,
    JSONMDDistribution.PROPERTY_DISTRIBUTOR,
    JSONMDDistribution.PROPERTY_TRANSFER_OPTIONS
})
public final class JSONMDDistribution extends DataTransferObject {

    public static final String PROPERTY_DESCRIPTION = "description";
    public static final String PROPERTY_DISTRIBUTION_FORMAT = "distributionFormat";
    public static final String PROPERTY_DISTRIBUTOR = "distributor";
    public static final String PROPERTY_TRANSFER_OPTIONS = "transferOptions";

    @JsonProperty(PROPERTY_DESCRIPTION)
    private String description;
    @JsonProperty(PROPERTY_DISTRIBUTION_FORMAT)
    private List<JSONMDFormat> distributionFormat;
    @JsonProperty(PROPERTY_DISTRIBUTOR)
    private List<JSONMDDistributor> distributor;
    @JsonProperty(PROPERTY_TRANSFER_OPTIONS)
    private List<JSONMDDigitalTransferOptions> transferOptions;

    public JSONMDDistribution() {
    }

    public JSONMDDistribution(final Distribution source, final JSONMDEncoder encoder) {
        description        = encoder.text(source.getDescription());
        distributionFormat = encoder.list(source.getDistributionFormats(), (Format e) -> new JSONMDFormat(e, encoder));
        distributor        = encoder.list(source.getDistributors(), (Distributor e) -> new JSONMDDistributor(e, encoder));
        transferOptions    = encoder.list(source.getTransferOptions(),
                                (DigitalTransferOptions e) -> new JSONMDDigitalTransferOptions(e, encoder));
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public List<JSONMDFormat> getDistributionFormat() {
        return distributionFormat;
    }

    public void setDistributionFormat(final List<JSONMDFormat> distributionFormat) {
        this.distributionFormat = distributionFormat;
    }

    public List<JSONMDDistributor> getDistributor() {
        return distributor;
    }

    public void setDistributor(final List<JSONMDDistributor> distributor) {
        this.distributor = distributor;
    }

    public List<JSONMDDigitalTransferOptions> getTransferOptions() {
        return transferOptions;
    }

    public void setTransferOptions(final List<JSONMDDigitalTransferOptions> transferOptions) {
        this.transferOptions = transferOptions;
    }

    public DefaultDistribution toMetadata(final JSONMDDecoder decoder) {
        final DefaultDistribution target = new DefaultDistribution();
        target.setDescription(decoder.text(description));
        target.setDistributionFormats(decoder.list(distributionFormat, (JSONMDFormat e) -> e.toMetadata(decoder)));
        target.setDistributors(decoder.list(distributor, (JSONMDDistributor e) -> e.toMetadata(decoder)));
        target.setTransferOptions(decoder.list(transferOptions, (JSONMDDigitalTransferOptions e) -> e.toMetadata(decoder)));
        return target;
    }
}
