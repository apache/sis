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
package org.apache.sis.storage.geopose.binding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.sis.storage.json.DataTransferObject;


/**
 * GeoPose time series whose poses are separated by a constant duration.
 *
 * <p>Because the spacing is constant, the poses carry no individual time; the time of the
 * pose at index <var>i</var> is the series start instant plus <var>i</var> times the
 * inter-pose duration.</p>
 *
 * <ul>
 *   <li>All five members are mandatory.</li>
 *   <li>{@code innerFrameSeries} holds at least one frame.</li>
 *   <li>{@code interPoseDuration} is expressed in milliseconds.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0, standardization target Regular Series"
 */
@JsonPropertyOrder({
    RegularSeries.JSON_PROPERTY_HEADER,
    RegularSeries.JSON_PROPERTY_INTER_POSE_DURATION,
    RegularSeries.JSON_PROPERTY_OUTER_FRAME,
    RegularSeries.JSON_PROPERTY_INNER_FRAME_SERIES,
    RegularSeries.JSON_PROPERTY_TRAILER
})
public class RegularSeries extends DataTransferObject {

    public static final String JSON_PROPERTY_HEADER = "header";
    private SeriesHeader header;

    public static final String JSON_PROPERTY_INTER_POSE_DURATION = "interPoseDuration";
    private long interPoseDuration;

    public static final String JSON_PROPERTY_OUTER_FRAME = "outerFrame";
    private FrameSpecification outerFrame;

    public static final String JSON_PROPERTY_INNER_FRAME_SERIES = "innerFrameSeries";
    private List<FrameSpecification> innerFrameSeries = new ArrayList<>();

    public static final String JSON_PROPERTY_TRAILER = "trailer";
    private SeriesTrailer trailer;

    public RegularSeries() {
    }

    public RegularSeries(final SeriesHeader header, final long interPoseDuration,
            final FrameSpecification outerFrame, final List<FrameSpecification> innerFrameSeries,
            final SeriesTrailer trailer)
    {
        this.header = header;
        this.interPoseDuration = interPoseDuration;
        this.outerFrame = outerFrame;
        this.innerFrameSeries = innerFrameSeries;
        this.trailer = trailer;
    }

    /**
     * Returns the opening block of the series.
     *
     * @return the series header
     */
    @JsonProperty(JSON_PROPERTY_HEADER)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public SeriesHeader getHeader() {
        return header;
    }

    @JsonProperty(JSON_PROPERTY_HEADER)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setHeader(final SeriesHeader header) {
        this.header = header;
    }

    /**
     * Returns the constant duration between two consecutive poses.
     *
     * @return the duration in milliseconds
     */
    @JsonProperty(JSON_PROPERTY_INTER_POSE_DURATION)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public long getInterPoseDuration() {
        return interPoseDuration;
    }

    @JsonProperty(JSON_PROPERTY_INTER_POSE_DURATION)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setInterPoseDuration(final long interPoseDuration) {
        this.interPoseDuration = interPoseDuration;
    }

    /**
     * Returns the frame in which every pose of the series is expressed.
     *
     * @return the outer frame
     */
    @JsonProperty(JSON_PROPERTY_OUTER_FRAME)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public FrameSpecification getOuterFrame() {
        return outerFrame;
    }

    @JsonProperty(JSON_PROPERTY_OUTER_FRAME)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setOuterFrame(final FrameSpecification outerFrame) {
        this.outerFrame = outerFrame;
    }

    /**
     * Returns the successive poses of the series.
     *
     * @return at least one frame
     */
    @JsonProperty(JSON_PROPERTY_INNER_FRAME_SERIES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public List<FrameSpecification> getInnerFrameSeries() {
        return innerFrameSeries;
    }

    @JsonProperty(JSON_PROPERTY_INNER_FRAME_SERIES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setInnerFrameSeries(final List<FrameSpecification> innerFrameSeries) {
        this.innerFrameSeries = innerFrameSeries;
    }

    /**
     * Returns the closing block of the series.
     *
     * @return the series trailer
     */
    @JsonProperty(JSON_PROPERTY_TRAILER)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public SeriesTrailer getTrailer() {
        return trailer;
    }

    @JsonProperty(JSON_PROPERTY_TRAILER)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setTrailer(final SeriesTrailer trailer) {
        this.trailer = trailer;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final RegularSeries other = (RegularSeries) o;
        return this.interPoseDuration == other.interPoseDuration
            && Objects.equals(this.header,           other.header)
            && Objects.equals(this.outerFrame,       other.outerFrame)
            && Objects.equals(this.innerFrameSeries, other.innerFrameSeries)
            && Objects.equals(this.trailer,          other.trailer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(header, interPoseDuration, outerFrame, innerFrameSeries, trailer);
    }
}
