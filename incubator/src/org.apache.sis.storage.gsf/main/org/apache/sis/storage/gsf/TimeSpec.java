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

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout.OfLong;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class TimeSpec extends StructClass{

    private static final OfLong tv_secLAYOUT = GSF.C_LONG.withName("tv_sec");
    private static final OfLong tv_nsecLAYOUT = GSF.C_LONG.withName("tv_nsec");
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        tv_secLAYOUT,
        tv_nsecLAYOUT
    ).withName("timespec");

    public TimeSpec(MemorySegment struct) {
        super(struct);
    }

    public TimeSpec(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    public long getTvSec() {
        return struct.get(tv_secLAYOUT, 0L);
    }

    public long getTvNsec() {
        return struct.get(tv_nsecLAYOUT, 8L);
    }

}