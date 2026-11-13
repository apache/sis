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
import org.apache.sis.metadata.iso.identification.DefaultBrowseGraphic;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.constraint.Constraints;
import org.opengis.metadata.identification.BrowseGraphic;

/**
 * Binding of the {@code MD_BrowseGraphic} type, an extension to the ISO 19115-4 Core profile.
 *
 * <p>Property names and encoding follow the rules applied to the Core profile types.
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1.3"
 */
@JsonPropertyOrder({
    JSONMDBrowseGraphic.PROPERTY_FILE_NAME,
    JSONMDBrowseGraphic.PROPERTY_FILE_DESCRIPTION,
    JSONMDBrowseGraphic.PROPERTY_FILE_TYPE,
    JSONMDBrowseGraphic.PROPERTY_IMAGE_CONSTRAINTS,
    JSONMDBrowseGraphic.PROPERTY_LINKAGE
})
public final class JSONMDBrowseGraphic extends DataTransferObject {

    public static final String PROPERTY_FILE_NAME = "fileName";
    public static final String PROPERTY_FILE_DESCRIPTION = "fileDescription";
    public static final String PROPERTY_FILE_TYPE = "fileType";
    public static final String PROPERTY_IMAGE_CONSTRAINTS = "imageConstraints";
    public static final String PROPERTY_LINKAGE = "linkage";

    @JsonProperty(PROPERTY_FILE_NAME)
    private String fileName;
    @JsonProperty(PROPERTY_FILE_DESCRIPTION)
    private String fileDescription;
    @JsonProperty(PROPERTY_FILE_TYPE)
    private String fileType;
    @JsonProperty(PROPERTY_IMAGE_CONSTRAINTS)
    private List<JSONMDConstraints> imageConstraints;
    @JsonProperty(PROPERTY_LINKAGE)
    private List<JSONMDOnlineResource> linkage;

    public JSONMDBrowseGraphic() {
    }

    public JSONMDBrowseGraphic(final BrowseGraphic source, final JSONMDEncoder encoder) {
        fileName         = encoder.uri(source.getFileName());
        fileDescription  = encoder.text(source.getFileDescription());
        fileType         = source.getFileType();
        imageConstraints = encoder.list(source.getImageConstraints(), (Constraints e) -> JSONMDConstraints.create(e, encoder));
        linkage          = encoder.list(source.getLinkages(), (OnlineResource e) -> new JSONMDOnlineResource(e, encoder));
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public String getFileDescription() {
        return fileDescription;
    }

    public void setFileDescription(final String fileDescription) {
        this.fileDescription = fileDescription;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(final String fileType) {
        this.fileType = fileType;
    }

    public List<JSONMDConstraints> getImageConstraints() {
        return imageConstraints;
    }

    public void setImageConstraints(final List<JSONMDConstraints> imageConstraints) {
        this.imageConstraints = imageConstraints;
    }

    public List<JSONMDOnlineResource> getLinkage() {
        return linkage;
    }

    public void setLinkage(final List<JSONMDOnlineResource> linkage) {
        this.linkage = linkage;
    }

    public DefaultBrowseGraphic toMetadata(final JSONMDDecoder decoder) {
        final DefaultBrowseGraphic target = new DefaultBrowseGraphic();
        target.setFileName(decoder.uri(fileName));
        target.setFileDescription(decoder.text(fileDescription));
        target.setFileType(fileType);
        target.setImageConstraints(decoder.list(imageConstraints, (JSONMDConstraints e) -> e.toMetadata(decoder)));
        target.setLinkages(decoder.list(linkage, (JSONMDOnlineResource e) -> e.toMetadata(decoder)));
        return target;
    }
}
