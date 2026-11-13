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
import org.apache.sis.metadata.iso.identification.DefaultUsage;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.identification.Usage;
import org.opengis.temporal.TemporalPrimitive;

/**
 * Binding of the {@code MD_Usage} JSON type.
 *
 * <p>Constraints:
 * <ul>
 *   <li>{@code identifiedIssues} has a single value in JSON, other SIS values are ignored with a warning.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1.5"
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#MD_Usage</a>
 */
@JsonPropertyOrder({
    JSONMDUsage.PROPERTY_SPECIFIC_USAGE,
    JSONMDUsage.PROPERTY_USAGE_DATE_TIME,
    JSONMDUsage.PROPERTY_USER_DETERMINED_LIMITATIONS,
    JSONMDUsage.PROPERTY_USER_CONTACT_INFO,
    JSONMDUsage.PROPERTY_RESPONSE,
    JSONMDUsage.PROPERTY_ADDITIONAL_DOCUMENTATION,
    JSONMDUsage.PROPERTY_IDENTIFIED_ISSUES
})
public final class JSONMDUsage extends DataTransferObject {

    public static final String PROPERTY_SPECIFIC_USAGE = "specificUsage";
    public static final String PROPERTY_USAGE_DATE_TIME = "usageDateTime";
    public static final String PROPERTY_USER_DETERMINED_LIMITATIONS = "userDeterminedLimitations";
    public static final String PROPERTY_USER_CONTACT_INFO = "userContactInfo";
    public static final String PROPERTY_RESPONSE = "response";
    public static final String PROPERTY_ADDITIONAL_DOCUMENTATION = "additionalDocumentation";
    public static final String PROPERTY_IDENTIFIED_ISSUES = "identifiedIssues";

    @JsonProperty(PROPERTY_SPECIFIC_USAGE)
    private String specificUsage;
    @JsonProperty(PROPERTY_USAGE_DATE_TIME)
    private List<JSONMDInstantOrInterval> usageDateTime;
    @JsonProperty(PROPERTY_USER_DETERMINED_LIMITATIONS)
    private String userDeterminedLimitations;
    @JsonProperty(PROPERTY_USER_CONTACT_INFO)
    private List<JSONMDResponsibility> userContactInfo;
    @JsonProperty(PROPERTY_RESPONSE)
    private List<String> response;
    @JsonProperty(PROPERTY_ADDITIONAL_DOCUMENTATION)
    private List<JSONMDCitation> additionalDocumentation;
    @JsonProperty(PROPERTY_IDENTIFIED_ISSUES)
    private JSONMDCitation identifiedIssues;

    public JSONMDUsage() {
    }

    public JSONMDUsage(final Usage source, final JSONMDEncoder encoder) {
        specificUsage             = encoder.text(source.getSpecificUsage());
        usageDateTime             = encoder.list(source.getUsageDates(), encoder::time);
        userDeterminedLimitations = encoder.text(source.getUserDeterminedLimitations());
        userContactInfo           = encoder.list(source.getUserContactInfo(), (Responsibility e) -> new JSONMDResponsibility(e, encoder));
        response                  = encoder.texts(source.getResponses());
        additionalDocumentation   = encoder.list(source.getAdditionalDocumentation(), (Citation e) -> new JSONMDCitation(e, encoder));
        identifiedIssues          = encoder.first(source.getIdentifiedIssues(), (Citation e) -> new JSONMDCitation(e, encoder),
                                                  PROPERTY_IDENTIFIED_ISSUES);
    }

    public String getSpecificUsage() {
        return specificUsage;
    }

    public void setSpecificUsage(final String specificUsage) {
        this.specificUsage = specificUsage;
    }

    public List<JSONMDInstantOrInterval> getUsageDateTime() {
        return usageDateTime;
    }

    public void setUsageDateTime(final List<JSONMDInstantOrInterval> usageDateTime) {
        this.usageDateTime = usageDateTime;
    }

    public String getUserDeterminedLimitations() {
        return userDeterminedLimitations;
    }

    public void setUserDeterminedLimitations(final String userDeterminedLimitations) {
        this.userDeterminedLimitations = userDeterminedLimitations;
    }

    public List<JSONMDResponsibility> getUserContactInfo() {
        return userContactInfo;
    }

    public void setUserContactInfo(final List<JSONMDResponsibility> userContactInfo) {
        this.userContactInfo = userContactInfo;
    }

    public List<String> getResponse() {
        return response;
    }

    public void setResponse(final List<String> response) {
        this.response = response;
    }

    public List<JSONMDCitation> getAdditionalDocumentation() {
        return additionalDocumentation;
    }

    public void setAdditionalDocumentation(final List<JSONMDCitation> additionalDocumentation) {
        this.additionalDocumentation = additionalDocumentation;
    }

    public JSONMDCitation getIdentifiedIssues() {
        return identifiedIssues;
    }

    public void setIdentifiedIssues(final JSONMDCitation identifiedIssues) {
        this.identifiedIssues = identifiedIssues;
    }

    public DefaultUsage toMetadata(final JSONMDDecoder decoder) {
        final DefaultUsage target = new DefaultUsage();
        target.setSpecificUsage(decoder.text(specificUsage));
        /*
         * Workaround for DefaultUsage.setUsageDates(…) which swaps source and target collections
         * in SIS 2.0-SNAPSHOT: add the values directly to the live collection.
         */
        final List<TemporalPrimitive> dates = decoder.list(usageDateTime, JSONMDInstantOrInterval::toTemporal);
        target.getUsageDates().addAll(dates);
        target.setUserDeterminedLimitations(decoder.text(userDeterminedLimitations));
        target.setUserContactInfo(decoder.list(userContactInfo, (JSONMDResponsibility e) -> e.toMetadata(decoder)));
        target.setResponses(decoder.texts(response));
        target.setAdditionalDocumentation(decoder.list(additionalDocumentation, (JSONMDCitation e) -> e.toMetadata(decoder)));
        if (identifiedIssues != null) {
            target.setIdentifiedIssues(List.of(identifiedIssues.toMetadata(decoder)));
        }
        return target;
    }
}
