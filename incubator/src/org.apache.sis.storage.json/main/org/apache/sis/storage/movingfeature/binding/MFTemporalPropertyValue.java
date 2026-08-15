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
package org.apache.sis.storage.movingfeature.binding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.sis.storage.json.DataTransferObject;


/**
 * Value of one arbitrarily-named entry of a {@link MFTemporalPropertyGroup}, per Moving
 * Features JSON (MF-JSON, OGC 16-140r1). Merges the specification's {@code measureType}
 * ({@link #TYPE_MEASURE}, numeric {@code values}, optional {@code form}),
 * {@code textType} ({@link #TYPE_TEXT}, string/boolean {@code values}) and
 * {@code imageType} ({@link #TYPE_IMAGE}, string reference {@code values}) into a single
 * class rather than three near-duplicates; {@code values} is loosely typed as
 * {@code List<Object>} since its element type depends on {@link #getType()}.
 *
 * <p>Since {@link MFTemporalPropertyGroup} does not declare a Java field per named entry
 * (names are arbitrary, chosen by the data producer), instances of this class are found
 * in / should be put into {@link MFTemporalPropertyGroup#otherFields()}, keyed by name.</p>
 */
@JsonPropertyOrder({
    MFTemporalPropertyValue.PROPERTY_TYPE,
    MFTemporalPropertyValue.PROPERTY_VALUES,
    MFTemporalPropertyValue.PROPERTY_INTERPOLATION,
    MFTemporalPropertyValue.PROPERTY_FORM,
    MFTemporalPropertyValue.PROPERTY_DESCRIPTION
})
public class MFTemporalPropertyValue extends DataTransferObject {

    public static final String TYPE_MEASURE = "Measure";
    public static final String TYPE_TEXT = "Text";
    public static final String TYPE_IMAGE = "Image";

    public static final String INTERPOLATION_DISCRETE = "Discrete";
    public static final String INTERPOLATION_STEP = "Step";
    public static final String INTERPOLATION_LINEAR = "Linear";
    public static final String INTERPOLATION_REGRESSION = "Regression";

    public static final String PROPERTY_TYPE = "type";
    public static final String PROPERTY_VALUES = "values";
    public static final String PROPERTY_INTERPOLATION = "interpolation";
    public static final String PROPERTY_FORM = "form";
    public static final String PROPERTY_DESCRIPTION = "description";

    private String type;

    private List<Object> values = new ArrayList<>();

    private String interpolation;

    private String form;

    private String description;

    public MFTemporalPropertyValue() {
    }

    /**
     * Get type
     *
     * @return type
     */
    @JsonProperty(PROPERTY_TYPE)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public String getType() {
        return type;
    }

    @JsonProperty(PROPERTY_TYPE)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get values
     *
     * @return values
     */
    @JsonProperty(PROPERTY_VALUES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public List<Object> getValues() {
        return values;
    }

    @JsonProperty(PROPERTY_VALUES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setValues(List<Object> values) {
        this.values = values;
    }

    /**
     * Get interpolation
     *
     * @return interpolation
     */
    @JsonProperty(PROPERTY_INTERPOLATION)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public String getInterpolation() {
        return interpolation;
    }

    @JsonProperty(PROPERTY_INTERPOLATION)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setInterpolation(String interpolation) {
        this.interpolation = interpolation;
    }

    /**
     * Get form, meaningful when {@link #getType()} is {@link #TYPE_MEASURE}.
     *
     * @return form
     */
    @JsonProperty(PROPERTY_FORM)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public String getForm() {
        return form;
    }

    @JsonProperty(PROPERTY_FORM)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setForm(String form) {
        this.form = form;
    }

    /**
     * Get description
     *
     * @return description
     */
    @JsonProperty(PROPERTY_DESCRIPTION)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public String getDescription() {
        return description;
    }

    @JsonProperty(PROPERTY_DESCRIPTION)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Return true if this MFTemporalPropertyValue object is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MFTemporalPropertyValue other = (MFTemporalPropertyValue) o;
        return Objects.equals(this.type, other.type)
                && Objects.equals(this.values, other.values)
                && Objects.equals(this.interpolation, other.interpolation)
                && Objects.equals(this.form, other.form)
                && Objects.equals(this.description, other.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, values, interpolation, form, description);
    }

}
