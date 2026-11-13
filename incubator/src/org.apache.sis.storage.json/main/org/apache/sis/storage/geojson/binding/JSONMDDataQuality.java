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
import org.apache.sis.metadata.iso.quality.DefaultDataQuality;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.quality.Element;

/**
 * Binding of the {@code DataQuality} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19157/-1/dqc/1.0.0/dqc.json">dqc.json#DataQuality</a>
 */
@JsonPropertyOrder({
    JSONMDDataQuality.PROPERTY_SCOPE,
    JSONMDDataQuality.PROPERTY_REPORT,
    JSONMDDataQuality.PROPERTY_QUALITY_EVALUATION_REPORT
})
public final class JSONMDDataQuality extends DataTransferObject {

    public static final String PROPERTY_SCOPE = "scope";
    public static final String PROPERTY_REPORT = "report";
    public static final String PROPERTY_QUALITY_EVALUATION_REPORT = "qualityEvaluationReport";

    @JsonProperty(PROPERTY_SCOPE)
    private JSONMDScope scope;
    @JsonProperty(PROPERTY_REPORT)
    private List<JSONMDQualityElement> report;
    @JsonProperty(PROPERTY_QUALITY_EVALUATION_REPORT)
    private JSONMDEvaluationReport qualityEvaluationReport;

    public JSONMDDataQuality() {
    }

    public JSONMDDataQuality(final DataQuality source, final JSONMDEncoder encoder) {
        scope                   = (source.getScope() == null) ? null : new JSONMDScope(source.getScope(), encoder);
        report                  = encoder.list(source.getReports(), (Element e) -> JSONMDQualityElement.create(e, encoder));
        qualityEvaluationReport = (source.getStandaloneQualityReport() == null) ? null
                                : new JSONMDEvaluationReport(source.getStandaloneQualityReport(), encoder);
    }

    public JSONMDScope getScope() {
        return scope;
    }

    public void setScope(final JSONMDScope scope) {
        this.scope = scope;
    }

    public List<JSONMDQualityElement> getReport() {
        return report;
    }

    public void setReport(final List<JSONMDQualityElement> report) {
        this.report = report;
    }

    public JSONMDEvaluationReport getQualityEvaluationReport() {
        return qualityEvaluationReport;
    }

    public void setQualityEvaluationReport(final JSONMDEvaluationReport qualityEvaluationReport) {
        this.qualityEvaluationReport = qualityEvaluationReport;
    }

    public DefaultDataQuality toMetadata(final JSONMDDecoder decoder) {
        final DefaultDataQuality target = new DefaultDataQuality();
        if (scope != null) {
            target.setScope(scope.toMetadata(decoder));
        }
        target.setReports(decoder.list(report, (JSONMDQualityElement e) -> e.toMetadata(decoder)));
        if (qualityEvaluationReport != null) {
            target.setStandaloneQualityReport(qualityEvaluationReport.toMetadata(decoder));
        }
        return target;
    }
}
