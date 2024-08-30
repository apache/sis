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
package org.apache.sis.storage.gsf.specific;

import java.lang.invoke.*;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.MemoryLayout.PathElement.*;
import org.apache.sis.storage.gsf.GSF;
import org.apache.sis.storage.gsf.StructClass;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class EM3Specific extends StructClass {

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_INT.withName("model_number"),
        GSF.C_INT.withName("ping_number"),
        GSF.C_INT.withName("serial_number"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("surface_velocity"),
        GSF.C_DOUBLE.withName("transducer_depth"),
        GSF.C_INT.withName("valid_beams"),
        GSF.C_INT.withName("sample_rate"),
        GSF.C_DOUBLE.withName("depth_difference"),
        GSF.C_INT.withName("offset_multiplier"),
        MemoryLayout.paddingLayout(4),
        MemoryLayout.sequenceLayout(2, EM3RunTime.LAYOUT).withName("run_time")
    ).withName("t_gsfEM3Specific");

    public EM3Specific(MemorySegment struct) {
        super(struct);
    }

    public EM3Specific(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfInt model_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("model_number"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int model_number
     * }
     */
    public static final OfInt model_numberLAYOUT() {
        return model_numberLAYOUT;
    }

    private static final long model_number$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int model_number
     * }
     */
    public static final long model_number$offset() {
        return model_number$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int model_number
     * }
     */
    public static int model_number(MemorySegment struct) {
        return struct.get(model_numberLAYOUT, model_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int model_number
     * }
     */
    public static void model_number(MemorySegment struct, int fieldValue) {
        struct.set(model_numberLAYOUT, model_number$OFFSET, fieldValue);
    }

    private static final OfInt ping_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_number"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int ping_number
     * }
     */
    public static final OfInt ping_numberLAYOUT() {
        return ping_numberLAYOUT;
    }

    private static final long ping_number$OFFSET = 4;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int ping_number
     * }
     */
    public static final long ping_number$offset() {
        return ping_number$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ping_number
     * }
     */
    public static int ping_number(MemorySegment struct) {
        return struct.get(ping_numberLAYOUT, ping_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ping_number
     * }
     */
    public static void ping_number(MemorySegment struct, int fieldValue) {
        struct.set(ping_numberLAYOUT, ping_number$OFFSET, fieldValue);
    }

    private static final OfInt serial_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("serial_number"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int serial_number
     * }
     */
    public static final OfInt serial_numberLAYOUT() {
        return serial_numberLAYOUT;
    }

    private static final long serial_number$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int serial_number
     * }
     */
    public static final long serial_number$offset() {
        return serial_number$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int serial_number
     * }
     */
    public static int serial_number(MemorySegment struct) {
        return struct.get(serial_numberLAYOUT, serial_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int serial_number
     * }
     */
    public static void serial_number(MemorySegment struct, int fieldValue) {
        struct.set(serial_numberLAYOUT, serial_number$OFFSET, fieldValue);
    }

    private static final OfDouble surface_velocityLAYOUT = (OfDouble)LAYOUT.select(groupElement("surface_velocity"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public static final OfDouble surface_velocityLAYOUT() {
        return surface_velocityLAYOUT;
    }

    private static final long surface_velocity$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public static final long surface_velocity$offset() {
        return surface_velocity$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public static double surface_velocity(MemorySegment struct) {
        return struct.get(surface_velocityLAYOUT, surface_velocity$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public static void surface_velocity(MemorySegment struct, double fieldValue) {
        struct.set(surface_velocityLAYOUT, surface_velocity$OFFSET, fieldValue);
    }

    private static final OfDouble transducer_depthLAYOUT = (OfDouble)LAYOUT.select(groupElement("transducer_depth"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double transducer_depth
     * }
     */
    public static final OfDouble transducer_depthLAYOUT() {
        return transducer_depthLAYOUT;
    }

    private static final long transducer_depth$OFFSET = 24;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double transducer_depth
     * }
     */
    public static final long transducer_depth$offset() {
        return transducer_depth$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double transducer_depth
     * }
     */
    public static double transducer_depth(MemorySegment struct) {
        return struct.get(transducer_depthLAYOUT, transducer_depth$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double transducer_depth
     * }
     */
    public static void transducer_depth(MemorySegment struct, double fieldValue) {
        struct.set(transducer_depthLAYOUT, transducer_depth$OFFSET, fieldValue);
    }

    private static final OfInt valid_beamsLAYOUT = (OfInt)LAYOUT.select(groupElement("valid_beams"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int valid_beams
     * }
     */
    public static final OfInt valid_beamsLAYOUT() {
        return valid_beamsLAYOUT;
    }

    private static final long valid_beams$OFFSET = 32;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int valid_beams
     * }
     */
    public static final long valid_beams$offset() {
        return valid_beams$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int valid_beams
     * }
     */
    public static int valid_beams(MemorySegment struct) {
        return struct.get(valid_beamsLAYOUT, valid_beams$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int valid_beams
     * }
     */
    public static void valid_beams(MemorySegment struct, int fieldValue) {
        struct.set(valid_beamsLAYOUT, valid_beams$OFFSET, fieldValue);
    }

    private static final OfInt sample_rateLAYOUT = (OfInt)LAYOUT.select(groupElement("sample_rate"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int sample_rate
     * }
     */
    public static final OfInt sample_rateLAYOUT() {
        return sample_rateLAYOUT;
    }

    private static final long sample_rate$OFFSET = 36;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int sample_rate
     * }
     */
    public static final long sample_rate$offset() {
        return sample_rate$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int sample_rate
     * }
     */
    public static int sample_rate(MemorySegment struct) {
        return struct.get(sample_rateLAYOUT, sample_rate$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int sample_rate
     * }
     */
    public static void sample_rate(MemorySegment struct, int fieldValue) {
        struct.set(sample_rateLAYOUT, sample_rate$OFFSET, fieldValue);
    }

    private static final OfDouble depth_differenceLAYOUT = (OfDouble)LAYOUT.select(groupElement("depth_difference"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double depth_difference
     * }
     */
    public static final OfDouble depth_differenceLAYOUT() {
        return depth_differenceLAYOUT;
    }

    private static final long depth_difference$OFFSET = 40;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double depth_difference
     * }
     */
    public static final long depth_difference$offset() {
        return depth_difference$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double depth_difference
     * }
     */
    public static double depth_difference(MemorySegment struct) {
        return struct.get(depth_differenceLAYOUT, depth_difference$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double depth_difference
     * }
     */
    public static void depth_difference(MemorySegment struct, double fieldValue) {
        struct.set(depth_differenceLAYOUT, depth_difference$OFFSET, fieldValue);
    }

    private static final OfInt offset_multiplierLAYOUT = (OfInt)LAYOUT.select(groupElement("offset_multiplier"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int offset_multiplier
     * }
     */
    public static final OfInt offset_multiplierLAYOUT() {
        return offset_multiplierLAYOUT;
    }

    private static final long offset_multiplier$OFFSET = 48;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int offset_multiplier
     * }
     */
    public static final long offset_multiplier$offset() {
        return offset_multiplier$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int offset_multiplier
     * }
     */
    public static int offset_multiplier(MemorySegment struct) {
        return struct.get(offset_multiplierLAYOUT, offset_multiplier$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int offset_multiplier
     * }
     */
    public static void offset_multiplier(MemorySegment struct, int fieldValue) {
        struct.set(offset_multiplierLAYOUT, offset_multiplier$OFFSET, fieldValue);
    }

    private static final SequenceLayout run_timeLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("run_time"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * gsfEM3RunTime run_time[2]
     * }
     */
    public static final SequenceLayout run_timeLAYOUT() {
        return run_timeLAYOUT;
    }

    private static final long run_time$OFFSET = 56;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * gsfEM3RunTime run_time[2]
     * }
     */
    public static final long run_time$offset() {
        return run_time$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * gsfEM3RunTime run_time[2]
     * }
     */
    public static MemorySegment run_time(MemorySegment struct) {
        return struct.asSlice(run_time$OFFSET, run_timeLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * gsfEM3RunTime run_time[2]
     * }
     */
    public static void run_time(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, run_time$OFFSET, run_timeLAYOUT.byteSize());
    }

    private static long[] run_time$DIMS = { 2 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * gsfEM3RunTime run_time[2]
     * }
     */
    public static long[] run_time$dimensions() {
        return run_time$DIMS;
    }
    private static final MethodHandle run_time$ELEM_HANDLE = run_timeLAYOUT.sliceHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * gsfEM3RunTime run_time[2]
     * }
     */
    public static MemorySegment run_time(MemorySegment struct, long index0) {
        try {
            return (MemorySegment)run_time$ELEM_HANDLE.invokeExact(struct, 0L, index0);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * gsfEM3RunTime run_time[2]
     * }
     */
    public static void run_time(MemorySegment struct, long index0, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, run_time(struct, index0), 0L, EM3RunTime.LAYOUT.byteSize());
    }

}

