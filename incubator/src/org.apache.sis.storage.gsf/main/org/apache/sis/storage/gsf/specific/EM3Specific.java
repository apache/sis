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

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfInt model_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("model_number"));

    private static final long model_number$OFFSET = 0;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int model_number
     * }
     */
    public int model_number() {
        return struct.get(model_numberLAYOUT, model_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int model_number
     * }
     */
    public void model_number(int fieldValue) {
        struct.set(model_numberLAYOUT, model_number$OFFSET, fieldValue);
    }

    private static final OfInt ping_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_number"));

    private static final long ping_number$OFFSET = 4;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ping_number
     * }
     */
    public int ping_number() {
        return struct.get(ping_numberLAYOUT, ping_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ping_number
     * }
     */
    public void ping_number(int fieldValue) {
        struct.set(ping_numberLAYOUT, ping_number$OFFSET, fieldValue);
    }

    private static final OfInt serial_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("serial_number"));

    private static final long serial_number$OFFSET = 8;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int serial_number
     * }
     */
    public int serial_number() {
        return struct.get(serial_numberLAYOUT, serial_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int serial_number
     * }
     */
    public void serial_number(int fieldValue) {
        struct.set(serial_numberLAYOUT, serial_number$OFFSET, fieldValue);
    }

    private static final OfDouble surface_velocityLAYOUT = (OfDouble)LAYOUT.select(groupElement("surface_velocity"));

    private static final long surface_velocity$OFFSET = 16;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public double surface_velocity() {
        return struct.get(surface_velocityLAYOUT, surface_velocity$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public void surface_velocity(double fieldValue) {
        struct.set(surface_velocityLAYOUT, surface_velocity$OFFSET, fieldValue);
    }

    private static final OfDouble transducer_depthLAYOUT = (OfDouble)LAYOUT.select(groupElement("transducer_depth"));

    private static final long transducer_depth$OFFSET = 24;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double transducer_depth
     * }
     */
    public double transducer_depth() {
        return struct.get(transducer_depthLAYOUT, transducer_depth$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double transducer_depth
     * }
     */
    public void transducer_depth(double fieldValue) {
        struct.set(transducer_depthLAYOUT, transducer_depth$OFFSET, fieldValue);
    }

    private static final OfInt valid_beamsLAYOUT = (OfInt)LAYOUT.select(groupElement("valid_beams"));

    private static final long valid_beams$OFFSET = 32;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int valid_beams
     * }
     */
    public int valid_beams() {
        return struct.get(valid_beamsLAYOUT, valid_beams$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int valid_beams
     * }
     */
    public void valid_beams(int fieldValue) {
        struct.set(valid_beamsLAYOUT, valid_beams$OFFSET, fieldValue);
    }

    private static final OfInt sample_rateLAYOUT = (OfInt)LAYOUT.select(groupElement("sample_rate"));

    private static final long sample_rate$OFFSET = 36;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int sample_rate
     * }
     */
    public int sample_rate() {
        return struct.get(sample_rateLAYOUT, sample_rate$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int sample_rate
     * }
     */
    public void sample_rate(int fieldValue) {
        struct.set(sample_rateLAYOUT, sample_rate$OFFSET, fieldValue);
    }

    private static final OfDouble depth_differenceLAYOUT = (OfDouble)LAYOUT.select(groupElement("depth_difference"));

    private static final long depth_difference$OFFSET = 40;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double depth_difference
     * }
     */
    public double depth_difference() {
        return struct.get(depth_differenceLAYOUT, depth_difference$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double depth_difference
     * }
     */
    public void depth_difference(double fieldValue) {
        struct.set(depth_differenceLAYOUT, depth_difference$OFFSET, fieldValue);
    }

    private static final OfInt offset_multiplierLAYOUT = (OfInt)LAYOUT.select(groupElement("offset_multiplier"));

    private static final long offset_multiplier$OFFSET = 48;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int offset_multiplier
     * }
     */
    public int offset_multiplier() {
        return struct.get(offset_multiplierLAYOUT, offset_multiplier$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int offset_multiplier
     * }
     */
    public void offset_multiplier(int fieldValue) {
        struct.set(offset_multiplierLAYOUT, offset_multiplier$OFFSET, fieldValue);
    }

    private static final SequenceLayout run_timeLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("run_time"));

    private static final long run_time$OFFSET = 56;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * gsfEM3RunTime run_time[2]
     * }
     */
    public MemorySegment run_time() {
        return struct.asSlice(run_time$OFFSET, run_timeLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * gsfEM3RunTime run_time[2]
     * }
     */
    public void run_time(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, run_time$OFFSET, run_timeLAYOUT.byteSize());
    }

    private static final MethodHandle run_time$ELEM_HANDLE = run_timeLAYOUT.sliceHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * gsfEM3RunTime run_time[2]
     * }
     */
    public MemorySegment run_time(long index0) {
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
    public void run_time(long index0, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, run_time(index0), 0L, EM3RunTime.LAYOUT.byteSize());
    }
}
