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
import org.apache.sis.metadata.iso.distribution.DefaultFormat;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.distribution.Distributor;
import org.opengis.metadata.distribution.Format;
import org.opengis.metadata.distribution.Medium;

/**
 * Binding of the {@code MD_Format} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#MD_Format</a>
 */
@JsonPropertyOrder({
    JSONMDFormat.PROPERTY_FORMAT_SPECIFICATION_CITATION,
    JSONMDFormat.PROPERTY_AMENDMENT_NUMBER,
    JSONMDFormat.PROPERTY_FILE_DECOMPRESSION_TECHNIQUE,
    JSONMDFormat.PROPERTY_MEDIUM,
    JSONMDFormat.PROPERTY_FORMAT_DISTRIBUTOR
})
public final class JSONMDFormat extends DataTransferObject {

    public static final String PROPERTY_FORMAT_SPECIFICATION_CITATION = "formatSpecificationCitation";
    public static final String PROPERTY_AMENDMENT_NUMBER = "amendmentNumber";
    public static final String PROPERTY_FILE_DECOMPRESSION_TECHNIQUE = "fileDecompressionTechnique";
    public static final String PROPERTY_MEDIUM = "medium";
    public static final String PROPERTY_FORMAT_DISTRIBUTOR = "formatDistributor";

    @JsonProperty(PROPERTY_FORMAT_SPECIFICATION_CITATION)
    private JSONMDCitation formatSpecificationCitation;
    @JsonProperty(PROPERTY_AMENDMENT_NUMBER)
    private String amendmentNumber;
    @JsonProperty(PROPERTY_FILE_DECOMPRESSION_TECHNIQUE)
    private String fileDecompressionTechnique;
    @JsonProperty(PROPERTY_MEDIUM)
    private List<JSONMDMedium> medium;
    @JsonProperty(PROPERTY_FORMAT_DISTRIBUTOR)
    private List<JSONMDDistributor> formatDistributor;

    public JSONMDFormat() {
    }

    public JSONMDFormat(final Format source, final JSONMDEncoder encoder) {
        formatSpecificationCitation = (source.getFormatSpecificationCitation() == null) ? null
                                    : new JSONMDCitation(source.getFormatSpecificationCitation(), encoder);
        amendmentNumber             = encoder.text(source.getAmendmentNumber());
        fileDecompressionTechnique  = encoder.text(source.getFileDecompressionTechnique());
        medium                      = encoder.list(source.getMedia(), (Medium e) -> new JSONMDMedium(e, encoder));
        formatDistributor           = encoder.list(source.getFormatDistributors(), (Distributor e) -> new JSONMDDistributor(e, encoder));
    }

    public JSONMDCitation getFormatSpecificationCitation() {
        return formatSpecificationCitation;
    }

    public void setFormatSpecificationCitation(final JSONMDCitation formatSpecificationCitation) {
        this.formatSpecificationCitation = formatSpecificationCitation;
    }

    public String getAmendmentNumber() {
        return amendmentNumber;
    }

    public void setAmendmentNumber(final String amendmentNumber) {
        this.amendmentNumber = amendmentNumber;
    }

    public String getFileDecompressionTechnique() {
        return fileDecompressionTechnique;
    }

    public void setFileDecompressionTechnique(final String fileDecompressionTechnique) {
        this.fileDecompressionTechnique = fileDecompressionTechnique;
    }

    public List<JSONMDMedium> getMedium() {
        return medium;
    }

    public void setMedium(final List<JSONMDMedium> medium) {
        this.medium = medium;
    }

    public List<JSONMDDistributor> getFormatDistributor() {
        return formatDistributor;
    }

    public void setFormatDistributor(final List<JSONMDDistributor> formatDistributor) {
        this.formatDistributor = formatDistributor;
    }

    public DefaultFormat toMetadata(final JSONMDDecoder decoder) {
        final DefaultFormat target = new DefaultFormat();
        if (formatSpecificationCitation != null) {
            target.setFormatSpecificationCitation(formatSpecificationCitation.toMetadata(decoder));
        }
        target.setAmendmentNumber(decoder.text(amendmentNumber));
        target.setFileDecompressionTechnique(decoder.text(fileDecompressionTechnique));
        target.setMedia(decoder.list(medium, (JSONMDMedium e) -> e.toMetadata(decoder)));
        target.setFormatDistributors(decoder.list(formatDistributor, (JSONMDDistributor e) -> e.toMetadata(decoder)));
        return target;
    }
}
