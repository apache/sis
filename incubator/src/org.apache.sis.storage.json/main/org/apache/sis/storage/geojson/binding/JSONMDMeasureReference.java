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
import org.apache.sis.metadata.iso.quality.DefaultMeasureReference;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.quality.MeasureReference;

/**
 * Binding of the {@code MeasureReference} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19157/-1/dqc/1.0.0/dqc.json">dqc.json#MeasureReference</a>
 */
@JsonPropertyOrder({
    JSONMDMeasureReference.PROPERTY_MEASURE_IDENTIFICATION,
    JSONMDMeasureReference.PROPERTY_NAME_OF_MEASURE,
    JSONMDMeasureReference.PROPERTY_MEASURE_DESCRIPTION
})
public final class JSONMDMeasureReference extends DataTransferObject {

    public static final String PROPERTY_MEASURE_IDENTIFICATION = "measureIdentification";
    public static final String PROPERTY_NAME_OF_MEASURE = "nameOfMeasure";
    public static final String PROPERTY_MEASURE_DESCRIPTION = "measureDescription";

    @JsonProperty(PROPERTY_MEASURE_IDENTIFICATION)
    private JSONMDIdentifier measureIdentification;
    @JsonProperty(PROPERTY_NAME_OF_MEASURE)
    private List<String> nameOfMeasure;
    @JsonProperty(PROPERTY_MEASURE_DESCRIPTION)
    private String measureDescription;

    public JSONMDMeasureReference() {
    }

    public JSONMDMeasureReference(final MeasureReference source, final JSONMDEncoder encoder) {
        measureIdentification = (source.getMeasureIdentification() == null) ? null
                              : new JSONMDIdentifier(source.getMeasureIdentification(), encoder);
        nameOfMeasure         = encoder.texts(source.getNamesOfMeasure());
        measureDescription    = encoder.text(source.getMeasureDescription());
    }

    public JSONMDIdentifier getMeasureIdentification() {
        return measureIdentification;
    }

    public void setMeasureIdentification(final JSONMDIdentifier measureIdentification) {
        this.measureIdentification = measureIdentification;
    }

    public List<String> getNameOfMeasure() {
        return nameOfMeasure;
    }

    public void setNameOfMeasure(final List<String> nameOfMeasure) {
        this.nameOfMeasure = nameOfMeasure;
    }

    public String getMeasureDescription() {
        return measureDescription;
    }

    public void setMeasureDescription(final String measureDescription) {
        this.measureDescription = measureDescription;
    }

    public DefaultMeasureReference toMetadata(final JSONMDDecoder decoder) {
        final DefaultMeasureReference target = new DefaultMeasureReference();
        if (measureIdentification != null) {
            target.setMeasureIdentification(measureIdentification.toMetadata(decoder));
        }
        target.setNamesOfMeasure(decoder.texts(nameOfMeasure));
        target.setMeasureDescription(decoder.text(measureDescription));
        return target;
    }
}
