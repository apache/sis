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
import static java.lang.foreign.ValueLayout.*;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class HVNavigationError extends StructClass {

    private static final GroupLayout LAYOUT_NAV_ERROR_TIME;
    private static final OfInt LAYOUT_RECORD_ID;
    private static final OfDouble LAYOUT_HORIZONTAL_ERROR;
    private static final OfDouble LAYOUT_VERTICAL_ERROR;
    private static final OfDouble LAYOUT_SEP_UNCERTAINTY;
    private static final SequenceLayout LAYOUT_SPARE;
    private static final AddressLayout LAYOUT_POSITION_TYPE;
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        LAYOUT_NAV_ERROR_TIME = TimeSpec.LAYOUT.withName("nav_error_time"),
        LAYOUT_RECORD_ID = GSF.C_INT.withName("record_id"),
        MemoryLayout.paddingLayout(4),
        LAYOUT_HORIZONTAL_ERROR = GSF.C_DOUBLE.withName("horizontal_error"),
        LAYOUT_VERTICAL_ERROR = GSF.C_DOUBLE.withName("vertical_error"),
        LAYOUT_SEP_UNCERTAINTY = GSF.C_DOUBLE.withName("SEP_uncertainty"),
        LAYOUT_SPARE = MemoryLayout.sequenceLayout(2, GSF.C_CHAR).withName("spare"),
        MemoryLayout.paddingLayout(6),
        LAYOUT_POSITION_TYPE = GSF.C_POINTER.withName("position_type")
    ).withName("t_gsfHVNavigationError");

    public HVNavigationError(MemorySegment struct) {
        super(struct);
    }

    public HVNavigationError(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    public TimeSpec getNavErrorTime() {
        return new TimeSpec(struct.asSlice(0, LAYOUT_NAV_ERROR_TIME.byteSize()));
    }

    public int getRecordId() {
        return struct.get(LAYOUT_RECORD_ID, 16);
    }

    public double getHorizontalError() {
        return struct.get(LAYOUT_HORIZONTAL_ERROR, 24);
    }

    public double getVerticalError() {
        return struct.get(LAYOUT_VERTICAL_ERROR, 32);
    }

    public double getSEPUncertainty() {
        return struct.get(LAYOUT_SEP_UNCERTAINTY, 40);
    }

    public byte[] getSpare() {
        return getBytes(48, (int) LAYOUT_SPARE.byteSize());
    }

    public MemorySegment getPositionType() {
        return struct.get(LAYOUT_POSITION_TYPE, 56);
    }

}
