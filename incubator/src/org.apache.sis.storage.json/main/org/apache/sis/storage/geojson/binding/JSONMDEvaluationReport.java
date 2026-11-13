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
import org.apache.sis.metadata.iso.quality.DefaultEvaluationReportInformation;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.quality.StandaloneQualityReportInformation;

/**
 * Binding of the {@code QualityEvaluationReportInformation} JSON type.
 *
 * <p>Constraints:
 * <ul>
 *   <li>{@code qualityEvaluationReportDetails} and {@code elementReport} have no equivalent in SIS,
 *       they are never written and are ignored with a warning when read.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19157/-1/dqc/1.0.0/dqc.json">dqc.json#QualityEvaluationReportInformation</a>
 */
@JsonPropertyOrder({
    JSONMDEvaluationReport.PROPERTY_REPORT_REFERENCE,
    JSONMDEvaluationReport.PROPERTY_ABSTRACT,
    JSONMDEvaluationReport.PROPERTY_QUALITY_EVALUATION_REPORT_DETAILS,
    JSONMDEvaluationReport.PROPERTY_ELEMENT_REPORT
})
public final class JSONMDEvaluationReport extends DataTransferObject {

    public static final String PROPERTY_REPORT_REFERENCE = "reportReference";
    public static final String PROPERTY_ABSTRACT = "abstract";
    public static final String PROPERTY_QUALITY_EVALUATION_REPORT_DETAILS = "qualityEvaluationReportDetails";
    public static final String PROPERTY_ELEMENT_REPORT = "elementReport";

    @JsonProperty(PROPERTY_REPORT_REFERENCE)
    private JSONMDCitation reportReference;
    @JsonProperty(PROPERTY_ABSTRACT)
    private String abstracts;
    @JsonProperty(PROPERTY_QUALITY_EVALUATION_REPORT_DETAILS)
    private String qualityEvaluationReportDetails;
    @JsonProperty(PROPERTY_ELEMENT_REPORT)
    private List<JSONMDQualityElement> elementReport;

    public JSONMDEvaluationReport() {
    }

    public JSONMDEvaluationReport(final StandaloneQualityReportInformation source, final JSONMDEncoder encoder) {
        reportReference = (source.getReportReference() == null) ? null : new JSONMDCitation(source.getReportReference(), encoder);
        abstracts       = encoder.text(source.getAbstract());
    }

    public JSONMDCitation getReportReference() {
        return reportReference;
    }

    public void setReportReference(final JSONMDCitation reportReference) {
        this.reportReference = reportReference;
    }

    public String getAbstract() {
        return abstracts;
    }

    public void setAbstract(final String abstracts) {
        this.abstracts = abstracts;
    }

    public String getQualityEvaluationReportDetails() {
        return qualityEvaluationReportDetails;
    }

    public void setQualityEvaluationReportDetails(final String qualityEvaluationReportDetails) {
        this.qualityEvaluationReportDetails = qualityEvaluationReportDetails;
    }

    public List<JSONMDQualityElement> getElementReport() {
        return elementReport;
    }

    public void setElementReport(final List<JSONMDQualityElement> elementReport) {
        this.elementReport = elementReport;
    }

    public DefaultEvaluationReportInformation toMetadata(final JSONMDDecoder decoder) {
        if (qualityEvaluationReportDetails != null) {
            decoder.warning("Quality evaluation report details are not supported and are ignored.");
        }
        if (elementReport != null && !elementReport.isEmpty()) {
            decoder.warning("Quality evaluation report elements are not supported and are ignored.");
        }
        final DefaultEvaluationReportInformation target = new DefaultEvaluationReportInformation();
        if (reportReference != null) {
            target.setReportReference(reportReference.toMetadata(decoder));
        }
        target.setAbstract(decoder.text(abstracts));
        return target;
    }
}
