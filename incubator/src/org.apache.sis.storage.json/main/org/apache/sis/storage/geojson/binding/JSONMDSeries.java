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
import org.apache.sis.metadata.iso.citation.DefaultSeries;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.citation.Series;

/**
 * Binding of the {@code CI_Series} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#CI_Series</a>
 */
@JsonPropertyOrder({
    JSONMDSeries.PROPERTY_NAME,
    JSONMDSeries.PROPERTY_ISSUE_IDENTIFICATION,
    JSONMDSeries.PROPERTY_PAGE
})
public final class JSONMDSeries extends DataTransferObject {

    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_ISSUE_IDENTIFICATION = "issueIdentification";
    public static final String PROPERTY_PAGE = "page";

    @JsonProperty(PROPERTY_NAME)
    private String name;
    @JsonProperty(PROPERTY_ISSUE_IDENTIFICATION)
    private String issueIdentification;
    @JsonProperty(PROPERTY_PAGE)
    private String page;

    public JSONMDSeries() {
    }

    public JSONMDSeries(final Series source, final JSONMDEncoder encoder) {
        name                = encoder.text(source.getName());
        issueIdentification = encoder.text(source.getIssueIdentification());
        page                = encoder.text(source.getPage());
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getIssueIdentification() {
        return issueIdentification;
    }

    public void setIssueIdentification(final String issueIdentification) {
        this.issueIdentification = issueIdentification;
    }

    public String getPage() {
        return page;
    }

    public void setPage(final String page) {
        this.page = page;
    }

    public DefaultSeries toMetadata(final JSONMDDecoder decoder) {
        final DefaultSeries target = new DefaultSeries();
        target.setName(decoder.text(name));
        target.setIssueIdentification(decoder.text(issueIdentification));
        target.setPage(decoder.text(page));
        return target;
    }
}
