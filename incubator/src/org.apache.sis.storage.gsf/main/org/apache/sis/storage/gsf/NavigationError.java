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
public final class NavigationError extends StructClass {

    private static final GroupLayout LAYOUT_NAV_ERROR_TIME;
    private static final OfInt LAYOUT_RECORD_ID;
    private static final OfDouble LAYOUT_LATITUDE_ERROR;
    private static final OfDouble LAYOUT_LONGITUDE_ERROR;

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        LAYOUT_NAV_ERROR_TIME = TimeSpec.LAYOUT.withName("nav_error_time"),
        LAYOUT_RECORD_ID = GSF.C_INT.withName("record_id"),
        MemoryLayout.paddingLayout(4),
        LAYOUT_LATITUDE_ERROR = GSF.C_DOUBLE.withName("latitude_error"),
        LAYOUT_LONGITUDE_ERROR = GSF.C_DOUBLE.withName("longitude_error")
    ).withName("t_gsfNavigationError");

    public NavigationError(MemorySegment struct) {
        super(struct);
    }

    public NavigationError(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * struct timespec nav_error_time
     * }
     */
    public TimeSpec getNavErrorTime() {
        return new TimeSpec(struct.asSlice(0, LAYOUT_NAV_ERROR_TIME.byteSize()));
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int record_id
     * }
     */
    public int getRecordId() {
        return struct.get(LAYOUT_RECORD_ID, 16);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double latitude_error
     * }
     */
    public double getLatitudeError() {
        return struct.get(LAYOUT_LATITUDE_ERROR, 24);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double longitude_error
     * }
     */
    public double getLongitudeError() {
        return struct.get(LAYOUT_LONGITUDE_ERROR, 32);
    }

}

