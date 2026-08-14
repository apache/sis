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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.sis.storage.json.DataTransferObject;


/**
 * FeatureTime
 */
@JsonPropertyOrder({
    JSONFGTime.PROPERTY_DATE,
    JSONFGTime.PROPERTY_TIMESTAMP,
    JSONFGTime.PROPERTY_INTERVAL
})
public class JSONFGTime extends DataTransferObject {

    public static final String PROPERTY_DATE = "date";
    public static final String PROPERTY_TIMESTAMP = "timestamp";
    public static final String PROPERTY_INTERVAL = "interval";

    private String date;

    private String timestamp;

    private List<String> interval = new ArrayList<>();

    public JSONFGTime() {
    }

    /**
     * Get date
     *
     * @return date
     */
    @JsonProperty(PROPERTY_DATE)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public String getDate() {
        return date;
    }

    @JsonProperty(PROPERTY_DATE)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setDate(String date) {
        this.date = date;
    }

    /**
     * Get timestamp
     *
     * @return timestamp
     */
    @JsonProperty(PROPERTY_TIMESTAMP)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public String getTimestamp() {
        return timestamp;
    }

    @JsonProperty(PROPERTY_TIMESTAMP)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Get interval
     *
     * @return interval
     */
    @JsonProperty(PROPERTY_INTERVAL)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public List<String> getInterval() {
        return interval;
    }

    @JsonProperty(PROPERTY_INTERVAL)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setInterval(List<String> interval) {
        this.interval = interval;
    }

    /**
     * Return true if this Feature_time object is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JSONFGTime other = (JSONFGTime) o;
        return Objects.equals(this.date, other.date)
                && Objects.equals(this.timestamp, other.timestamp)
                && Objects.equals(this.interval, other.interval);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, timestamp, interval);
    }

}
