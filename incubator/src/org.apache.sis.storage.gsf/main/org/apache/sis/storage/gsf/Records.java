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
package org.apache.sis.storage.gsf;

import java.lang.foreign.*;
import java.util.*;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Records extends StructClass{

    private static final GroupLayout LAYOUT_HEADER;
    private static final GroupLayout LAYOUT_SUMMARY;
    private static final GroupLayout LAYOUT_MB_PING;
    private static final GroupLayout LAYOUT_SB_PING;
    private static final GroupLayout LAYOUT_SVP;
    private static final GroupLayout LAYOUT_PROCESS_PARAMETERS;
    private static final GroupLayout LAYOUT_SENSOR_PARAMETERS;
    private static final GroupLayout LAYOUT_COMMENT;
    private static final GroupLayout LAYOUT_HISTORY;
    private static final GroupLayout LAYOUT_NAV_ERROR;
    private static final GroupLayout LAYOUT_HV_NAV_ERROR;
    private static final GroupLayout LAYOUT_ATTITUDE;
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        LAYOUT_HEADER = Header.LAYOUT.withName("header"),
        MemoryLayout.paddingLayout(4),
        LAYOUT_SUMMARY = SwathBathySummary.LAYOUT.withName("summary"),
        LAYOUT_MB_PING = SwathBathyPing.LAYOUT.withName("mb_ping"),
        LAYOUT_SB_PING = SingleBeamPing.LAYOUT.withName("sb_ping"),
        LAYOUT_SVP = SVP.LAYOUT.withName("svp"),
        LAYOUT_PROCESS_PARAMETERS = ProcessingParameters.LAYOUT.withName("process_parameters"),
        LAYOUT_SENSOR_PARAMETERS = SensorParameters.LAYOUT.withName("sensor_parameters"),
        LAYOUT_COMMENT = Comment.LAYOUT.withName("comment"),
        LAYOUT_HISTORY = History.LAYOUT.withName("history"),
        LAYOUT_NAV_ERROR = NavigationError.LAYOUT.withName("nav_error"),
        LAYOUT_HV_NAV_ERROR = HVNavigationError.LAYOUT.withName("hv_nav_error"),
        LAYOUT_ATTITUDE = Attitude.LAYOUT.withName("attitude")
    ).withName("t_gsfRecords");

    private DataID dataId;

    public Records( MemorySegment struct) {
        super(struct);
    }

    public Records(SegmentAllocator allocator) {
        super(allocator);
    }

    public DataID getDataId() {
        return dataId;
    }

    public void setDataId(DataID dataId) {
        this.dataId = dataId;
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    public Header getHeader() {
        return new Header(struct.asSlice(0, LAYOUT_HEADER.byteSize()));
    }

    public SwathBathySummary getSummary() {
        return new SwathBathySummary(struct.asSlice(16, LAYOUT_SUMMARY.byteSize()));
    }

    public SwathBathyPing getMbPing() {
        return new SwathBathyPing(struct.asSlice(96, LAYOUT_MB_PING.byteSize()));
    }

    public SingleBeamPing getSbPing() {
        return new SingleBeamPing(struct.asSlice(3112, LAYOUT_SB_PING.byteSize()));
    }

    public SVP getSvp() {
        return new SVP(struct.asSlice(3240, LAYOUT_SVP.byteSize()));
    }

    public ProcessingParameters getProcessParameters() {
        return new ProcessingParameters(struct.asSlice(3312, LAYOUT_PROCESS_PARAMETERS.byteSize()));
    }

    public SensorParameters getSensorParameters() {
        return new SensorParameters(struct.asSlice(4616, LAYOUT_SENSOR_PARAMETERS.byteSize()));
    }

    public Comment getComment() {
        return new Comment(struct.asSlice(5920, LAYOUT_COMMENT.byteSize()));
    }

    public History getHistory() {
        return new History(struct.asSlice(5952, LAYOUT_HISTORY.byteSize()));
    }

    public NavigationError getNavError() {
        return new NavigationError(struct.asSlice(6120, LAYOUT_NAV_ERROR.byteSize()));
    }

    public HVNavigationError getHVNavError() {
        return new HVNavigationError(struct.asSlice(6160, LAYOUT_HV_NAV_ERROR.byteSize()));
    }

    public Attitude getAttitude() {
        return new Attitude(struct.asSlice(6224, LAYOUT_ATTITUDE.byteSize()));
    }

    @Override
    public String toString() {
        final long dataType = dataId.getRecordId() & 0b11111111111L;
        final List<String> attributes = new ArrayList<>();
        if (dataType == GSF.GSF_RECORD_HEADER) attributes.add("header : " + getHeader());
        else if (dataType == GSF.GSF_RECORD_SWATH_BATHYMETRY_PING) attributes.add("mb_ping : " + getMbPing());
        else if (dataType == GSF.GSF_RECORD_SOUND_VELOCITY_PROFILE) attributes.add("svp : " + getSvp());
        else if (dataType == GSF.GSF_RECORD_PROCESSING_PARAMETERS) attributes.add("process_parameters : " + getProcessParameters());
        else if (dataType == GSF.GSF_RECORD_SENSOR_PARAMETERS) attributes.add("sensor_parameters : " + getSensorParameters());
        else if (dataType == GSF.GSF_RECORD_COMMENT) attributes.add("comment : " + getComment());
        else if (dataType == GSF.GSF_RECORD_HISTORY) attributes.add("history : " + getHistory());
        else if (dataType == GSF.GSF_RECORD_NAVIGATION_ERROR) attributes.add("nav_error : " + getNavError());
        else if (dataType == GSF.GSF_RECORD_SWATH_BATHY_SUMMARY) attributes.add("summary : " + getSummary());
        else if (dataType == GSF.GSF_RECORD_SINGLE_BEAM_PING) attributes.add("sb_ping : " + getSbPing());
        else if (dataType == GSF.GSF_RECORD_HV_NAVIGATION_ERROR) attributes.add("hv_nav_error : " + getHVNavError());
        else if (dataType == GSF.GSF_RECORD_ATTITUDE) attributes.add("attitude : " + getAttitude());
        return toStringTree("Record", attributes);
    }
}

