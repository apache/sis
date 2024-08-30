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
public final class ProcessingParameters extends StructClass {

    private static final GroupLayout LAYOUT_PARAM_TIME;
    private static final OfInt LAYOUT_NUMBER_PARAMETERS;
    private static final SequenceLayout LAYOUT_PARAM_SIZE;
    private static final SequenceLayout LAYOUT_PARAM;
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        LAYOUT_PARAM_TIME = TimeSpec.LAYOUT.withName("param_time"),
        LAYOUT_NUMBER_PARAMETERS = GSF.C_INT.withName("number_parameters"),
        LAYOUT_PARAM_SIZE = MemoryLayout.sequenceLayout(128, GSF.C_SHORT).withName("param_size"),
        MemoryLayout.paddingLayout(4),
        LAYOUT_PARAM = MemoryLayout.sequenceLayout(128, GSF.C_POINTER).withName("param")
    ).withName("t_gsfProcessingParameters");

    public ProcessingParameters(MemorySegment struct) {
        super(struct);
    }

    public ProcessingParameters(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    public TimeSpec getParamTime() {
        return new TimeSpec(struct.asSlice(0, LAYOUT_PARAM_TIME.byteSize()));
    }

    public int getNumberParameters() {
        return struct.get(LAYOUT_NUMBER_PARAMETERS, 16);
    }

    public short[] getParamSize() {
        return getShorts(20, getNumberParameters());
    }
}