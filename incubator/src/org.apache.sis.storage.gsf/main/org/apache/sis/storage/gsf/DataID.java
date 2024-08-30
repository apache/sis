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
public final class DataID extends StructClass {

    private static final OfInt LAYOUT_CHECKSUMFLAG;
    private static final OfInt LAYOUT_RESERVED;
    private static final OfInt LAYOUT_RECORDID;
    private static final OfInt LAYOUT_RECORD_NUMBER;
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        LAYOUT_CHECKSUMFLAG = GSF.C_INT.withName("checksumFlag"),
        LAYOUT_RESERVED = GSF.C_INT.withName("reserved"),
        LAYOUT_RECORDID = GSF.C_INT.withName("recordID"),
        LAYOUT_RECORD_NUMBER = GSF.C_INT.withName("record_number")
    ).withName("t_gsfDataID");

    public DataID(MemorySegment struct) {
        super(struct);
    }

    public DataID(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    public int getChecksumFlag() {
        return struct.get(LAYOUT_CHECKSUMFLAG, 0);
    }

    public int getReserved() {
        return struct.get(LAYOUT_RESERVED, 4);
    }

    public int getRecordId() {
        return struct.get(LAYOUT_RECORDID, 8);
    }

    public int getRecordNumber() {
        return struct.get(LAYOUT_RECORD_NUMBER, 12);
    }

}