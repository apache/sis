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
 * GeoPose time series whose poses are separated by varying durations.
 *
 * <p>Because the spacing varies, each pose carries its own validity time, so the elements are
 * {@link FrameAndTime} pairs rather than bare frames.</p>
 *
 * <ul>
 *   <li>All four members are mandatory.</li>
 *   <li>{@code innerFrameAndTimeSeries} holds at least one pair.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0, standardization target Irregular Series"
 */
@JsonPropertyOrder({
    IrregularSeries.JSON_PROPERTY_HEADER,
    IrregularSeries.JSON_PROPERTY_OUTER_FRAME,
    IrregularSeries.JSON_PROPERTY_INNER_FRAME_AND_TIME_SERIES,
    IrregularSeries.JSON_PROPERTY_TRAILER
})
public class IrregularSeries extends DataTransferObject {

    public static final String JSON_PROPERTY_HEADER = "header";
    private SeriesHeader header;

    public static final String JSON_PROPERTY_OUTER_FRAME = "outerFrame";
    private FrameSpecification outerFrame;

    public static final String JSON_PROPERTY_INNER_FRAME_AND_TIME_SERIES = "innerFrameAndTimeSeries";
    private List<FrameAndTime> innerFrameAndTimeSeries = new ArrayList<>();

    public static final String JSON_PROPERTY_TRAILER = "trailer";
    private SeriesTrailer trailer;

    public IrregularSeries() {
    }

    public IrregularSeries(final SeriesHeader header, final FrameSpecification outerFrame,
            final List<FrameAndTime> innerFrameAndTimeSeries, final SeriesTrailer trailer)
    {
        this.header = header;
        this.outerFrame = outerFrame;
        this.innerFrameAndTimeSeries = innerFrameAndTimeSeries;
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
     * Returns the successive poses of the series, each with its own time.
     *
     * @return at least one frame and time pair
     */
    @JsonProperty(JSON_PROPERTY_INNER_FRAME_AND_TIME_SERIES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public List<FrameAndTime> getInnerFrameAndTimeSeries() {
        return innerFrameAndTimeSeries;
    }

    @JsonProperty(JSON_PROPERTY_INNER_FRAME_AND_TIME_SERIES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setInnerFrameAndTimeSeries(final List<FrameAndTime> innerFrameAndTimeSeries) {
        this.innerFrameAndTimeSeries = innerFrameAndTimeSeries;
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
        final IrregularSeries other = (IrregularSeries) o;
        return Objects.equals(this.header,                  other.header)
            && Objects.equals(this.outerFrame,              other.outerFrame)
            && Objects.equals(this.innerFrameAndTimeSeries, other.innerFrameAndTimeSeries)
            && Objects.equals(this.trailer,                 other.trailer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(header, outerFrame, innerFrameAndTimeSeries, trailer);
    }
}
