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
import org.apache.sis.metadata.iso.distribution.DefaultMedium;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.distribution.Medium;
import org.opengis.metadata.distribution.MediumFormat;

/**
 * Binding of the {@code MD_Medium} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#MD_Medium</a>
 */
@JsonPropertyOrder({
    JSONMDMedium.PROPERTY_NAME,
    JSONMDMedium.PROPERTY_DENSITY,
    JSONMDMedium.PROPERTY_DENSITY_UNITS,
    JSONMDMedium.PROPERTY_VOLUMES,
    JSONMDMedium.PROPERTY_MEDIUM_FORMAT,
    JSONMDMedium.PROPERTY_MEDIUM_NOTE,
    JSONMDMedium.PROPERTY_IDENTIFIER
})
public final class JSONMDMedium extends DataTransferObject {

    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_DENSITY = "density";
    public static final String PROPERTY_DENSITY_UNITS = "densityUnits";
    public static final String PROPERTY_VOLUMES = "volumes";
    public static final String PROPERTY_MEDIUM_FORMAT = "mediumFormat";
    public static final String PROPERTY_MEDIUM_NOTE = "mediumNote";
    public static final String PROPERTY_IDENTIFIER = "identifier";

    @JsonProperty(PROPERTY_NAME)
    private JSONMDCitation name;
    @JsonProperty(PROPERTY_DENSITY)
    private Double density;
    @JsonProperty(PROPERTY_DENSITY_UNITS)
    private String densityUnits;
    @JsonProperty(PROPERTY_VOLUMES)
    private Integer volumes;
    @JsonProperty(PROPERTY_MEDIUM_FORMAT)
    private List<String> mediumFormat;
    @JsonProperty(PROPERTY_MEDIUM_NOTE)
    private String mediumNote;
    @JsonProperty(PROPERTY_IDENTIFIER)
    private JSONMDIdentifier identifier;

    public JSONMDMedium() {
    }

    public JSONMDMedium(final Medium source, final JSONMDEncoder encoder) {
        name         = (source.getName() == null) ? null : new JSONMDCitation(source.getName(), encoder);
        density      = source.getDensity();
        densityUnits = encoder.unit(source.getDensityUnits());
        volumes      = source.getVolumes();
        mediumFormat = encoder.codes(source.getMediumFormats());
        mediumNote   = encoder.text(source.getMediumNote());
        identifier   = (source.getIdentifier() == null) ? null : new JSONMDIdentifier(source.getIdentifier(), encoder);
    }

    public JSONMDCitation getName() {
        return name;
    }

    public void setName(final JSONMDCitation name) {
        this.name = name;
    }

    public Double getDensity() {
        return density;
    }

    public void setDensity(final Double density) {
        this.density = density;
    }

    public String getDensityUnits() {
        return densityUnits;
    }

    public void setDensityUnits(final String densityUnits) {
        this.densityUnits = densityUnits;
    }

    public Integer getVolumes() {
        return volumes;
    }

    public void setVolumes(final Integer volumes) {
        this.volumes = volumes;
    }

    public List<String> getMediumFormat() {
        return mediumFormat;
    }

    public void setMediumFormat(final List<String> mediumFormat) {
        this.mediumFormat = mediumFormat;
    }

    public String getMediumNote() {
        return mediumNote;
    }

    public void setMediumNote(final String mediumNote) {
        this.mediumNote = mediumNote;
    }

    public JSONMDIdentifier getIdentifier() {
        return identifier;
    }

    public void setIdentifier(final JSONMDIdentifier identifier) {
        this.identifier = identifier;
    }

    public DefaultMedium toMetadata(final JSONMDDecoder decoder) {
        final DefaultMedium target = new DefaultMedium();
        if (name != null) {
            target.setName(name.toMetadata(decoder));
        }
        target.setDensity(density);
        target.setDensityUnits(decoder.unit(densityUnits));
        target.setVolumes(volumes);
        target.setMediumFormats(decoder.codes(MediumFormat.class, mediumFormat, MediumFormat::valueOf));
        target.setMediumNote(decoder.text(mediumNote));
        if (identifier != null) {
            target.setIdentifier(identifier.toMetadata(decoder));
        }
        return target;
    }
}
