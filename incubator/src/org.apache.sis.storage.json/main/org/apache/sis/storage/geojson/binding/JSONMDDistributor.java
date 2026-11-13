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
import org.apache.sis.metadata.iso.distribution.DefaultDistributor;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.distribution.DigitalTransferOptions;
import org.opengis.metadata.distribution.Distributor;
import org.opengis.metadata.distribution.Format;

/**
 * Binding of the {@code MD_Distributor} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#MD_Distributor</a>
 */
@JsonPropertyOrder({
    JSONMDDistributor.PROPERTY_DISTRIBUTOR_CONTACT,
    JSONMDDistributor.PROPERTY_DISTRIBUTOR_FORMAT,
    JSONMDDistributor.PROPERTY_DISTRIBUTOR_TRANSFER_OPTIONS
})
public final class JSONMDDistributor extends DataTransferObject {

    public static final String PROPERTY_DISTRIBUTOR_CONTACT = "distributorContact";
    public static final String PROPERTY_DISTRIBUTOR_FORMAT = "distributorFormat";
    public static final String PROPERTY_DISTRIBUTOR_TRANSFER_OPTIONS = "distributorTransferOptions";

    @JsonProperty(PROPERTY_DISTRIBUTOR_CONTACT)
    private JSONMDResponsibility distributorContact;
    @JsonProperty(PROPERTY_DISTRIBUTOR_FORMAT)
    private List<JSONMDFormat> distributorFormat;
    @JsonProperty(PROPERTY_DISTRIBUTOR_TRANSFER_OPTIONS)
    private List<JSONMDDigitalTransferOptions> distributorTransferOptions;

    public JSONMDDistributor() {
    }

    public JSONMDDistributor(final Distributor source, final JSONMDEncoder encoder) {
        distributorContact         = (source.getDistributorContact() == null) ? null
                                   : new JSONMDResponsibility(source.getDistributorContact(), encoder);
        distributorFormat          = encoder.list(source.getDistributorFormats(), (Format e) -> new JSONMDFormat(e, encoder));
        distributorTransferOptions = encoder.list(source.getDistributorTransferOptions(),
                                        (DigitalTransferOptions e) -> new JSONMDDigitalTransferOptions(e, encoder));
    }

    public JSONMDResponsibility getDistributorContact() {
        return distributorContact;
    }

    public void setDistributorContact(final JSONMDResponsibility distributorContact) {
        this.distributorContact = distributorContact;
    }

    public List<JSONMDFormat> getDistributorFormat() {
        return distributorFormat;
    }

    public void setDistributorFormat(final List<JSONMDFormat> distributorFormat) {
        this.distributorFormat = distributorFormat;
    }

    public List<JSONMDDigitalTransferOptions> getDistributorTransferOptions() {
        return distributorTransferOptions;
    }

    public void setDistributorTransferOptions(final List<JSONMDDigitalTransferOptions> distributorTransferOptions) {
        this.distributorTransferOptions = distributorTransferOptions;
    }

    public DefaultDistributor toMetadata(final JSONMDDecoder decoder) {
        final DefaultDistributor target = new DefaultDistributor();
        if (distributorContact != null) {
            target.setDistributorContact(distributorContact.toMetadata(decoder));
        }
        target.setDistributorFormats(decoder.list(distributorFormat, (JSONMDFormat e) -> e.toMetadata(decoder)));
        target.setDistributorTransferOptions(decoder.list(distributorTransferOptions,
                (JSONMDDigitalTransferOptions e) -> e.toMetadata(decoder)));
        return target;
    }
}
