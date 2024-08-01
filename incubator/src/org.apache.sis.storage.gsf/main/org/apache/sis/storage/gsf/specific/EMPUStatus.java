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
public final class EMPUStatus extends StructClass {


    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_DOUBLE.withName("pu_cpu_load"),
        GSF.C_SHORT.withName("sensor_status"),
        MemoryLayout.paddingLayout(2),
        GSF.C_INT.withName("achieved_port_coverage"),
        GSF.C_INT.withName("achieved_stbd_coverage"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("yaw_stabilization"),
        MemoryLayout.sequenceLayout(16, GSF.C_CHAR).withName("spare")
    ).withName("t_gsfEMPUStatus");

    public EMPUStatus(MemorySegment struct) {
        super(struct);
    }

    public EMPUStatus(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfDouble pu_cpu_loadLAYOUT = (OfDouble)LAYOUT.select(groupElement("pu_cpu_load"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double pu_cpu_load
     * }
     */
    public static final OfDouble pu_cpu_loadLAYOUT() {
        return pu_cpu_loadLAYOUT;
    }

    private static final long pu_cpu_load$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double pu_cpu_load
     * }
     */
    public static final long pu_cpu_load$offset() {
        return pu_cpu_load$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double pu_cpu_load
     * }
     */
    public static double pu_cpu_load(MemorySegment struct) {
        return struct.get(pu_cpu_loadLAYOUT, pu_cpu_load$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double pu_cpu_load
     * }
     */
    public static void pu_cpu_load(MemorySegment struct, double fieldValue) {
        struct.set(pu_cpu_loadLAYOUT, pu_cpu_load$OFFSET, fieldValue);
    }

    private static final OfShort sensor_statusLAYOUT = (OfShort)LAYOUT.select(groupElement("sensor_status"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned short sensor_status
     * }
     */
    public static final OfShort sensor_statusLAYOUT() {
        return sensor_statusLAYOUT;
    }

    private static final long sensor_status$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned short sensor_status
     * }
     */
    public static final long sensor_status$offset() {
        return sensor_status$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short sensor_status
     * }
     */
    public static short sensor_status(MemorySegment struct) {
        return struct.get(sensor_statusLAYOUT, sensor_status$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short sensor_status
     * }
     */
    public static void sensor_status(MemorySegment struct, short fieldValue) {
        struct.set(sensor_statusLAYOUT, sensor_status$OFFSET, fieldValue);
    }

    private static final OfInt achieved_port_coverageLAYOUT = (OfInt)LAYOUT.select(groupElement("achieved_port_coverage"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int achieved_port_coverage
     * }
     */
    public static final OfInt achieved_port_coverageLAYOUT() {
        return achieved_port_coverageLAYOUT;
    }

    private static final long achieved_port_coverage$OFFSET = 12;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int achieved_port_coverage
     * }
     */
    public static final long achieved_port_coverage$offset() {
        return achieved_port_coverage$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int achieved_port_coverage
     * }
     */
    public static int achieved_port_coverage(MemorySegment struct) {
        return struct.get(achieved_port_coverageLAYOUT, achieved_port_coverage$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int achieved_port_coverage
     * }
     */
    public static void achieved_port_coverage(MemorySegment struct, int fieldValue) {
        struct.set(achieved_port_coverageLAYOUT, achieved_port_coverage$OFFSET, fieldValue);
    }

    private static final OfInt achieved_stbd_coverageLAYOUT = (OfInt)LAYOUT.select(groupElement("achieved_stbd_coverage"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int achieved_stbd_coverage
     * }
     */
    public static final OfInt achieved_stbd_coverageLAYOUT() {
        return achieved_stbd_coverageLAYOUT;
    }

    private static final long achieved_stbd_coverage$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int achieved_stbd_coverage
     * }
     */
    public static final long achieved_stbd_coverage$offset() {
        return achieved_stbd_coverage$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int achieved_stbd_coverage
     * }
     */
    public static int achieved_stbd_coverage(MemorySegment struct) {
        return struct.get(achieved_stbd_coverageLAYOUT, achieved_stbd_coverage$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int achieved_stbd_coverage
     * }
     */
    public static void achieved_stbd_coverage(MemorySegment struct, int fieldValue) {
        struct.set(achieved_stbd_coverageLAYOUT, achieved_stbd_coverage$OFFSET, fieldValue);
    }

    private static final OfDouble yaw_stabilizationLAYOUT = (OfDouble)LAYOUT.select(groupElement("yaw_stabilization"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double yaw_stabilization
     * }
     */
    public static final OfDouble yaw_stabilizationLAYOUT() {
        return yaw_stabilizationLAYOUT;
    }

    private static final long yaw_stabilization$OFFSET = 24;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double yaw_stabilization
     * }
     */
    public static final long yaw_stabilization$offset() {
        return yaw_stabilization$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double yaw_stabilization
     * }
     */
    public static double yaw_stabilization(MemorySegment struct) {
        return struct.get(yaw_stabilizationLAYOUT, yaw_stabilization$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double yaw_stabilization
     * }
     */
    public static void yaw_stabilization(MemorySegment struct, double fieldValue) {
        struct.set(yaw_stabilizationLAYOUT, yaw_stabilization$OFFSET, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public static final SequenceLayout spareLAYOUT() {
        return spareLAYOUT;
    }

    private static final long spare$OFFSET = 32;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public static final long spare$offset() {
        return spare$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public static MemorySegment spare(MemorySegment struct) {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public static void spare(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static long[] spare$DIMS = { 16 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public static long[] spare$dimensions() {
        return spare$DIMS;
    }
    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public static byte spare(MemorySegment struct, long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public static void spare(MemorySegment struct, long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

}

