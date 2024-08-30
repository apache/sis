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
public final class EM3RawSpecific extends StructClass {

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_INT.withName("model_number"),
        GSF.C_INT.withName("ping_counter"),
        GSF.C_INT.withName("serial_number"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("surface_velocity"),
        GSF.C_DOUBLE.withName("transducer_depth"),
        GSF.C_INT.withName("valid_detections"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("sampling_frequency"),
        GSF.C_DOUBLE.withName("vehicle_depth"),
        GSF.C_DOUBLE.withName("depth_difference"),
        GSF.C_INT.withName("offset_multiplier"),
        MemoryLayout.sequenceLayout(16, GSF.C_CHAR).withName("spare_1"),
        GSF.C_INT.withName("transmit_sectors"),
        MemoryLayout.sequenceLayout(20, EM3RawTxSector.LAYOUT).withName("sector"),
        MemoryLayout.sequenceLayout(16, GSF.C_CHAR).withName("spare_2"),
        EMRunTime.LAYOUT.withName("run_time"),
        EMPUStatus.LAYOUT.withName("pu_status")
    ).withName("t_gsfEM3RawSpecific");

    public EM3RawSpecific(MemorySegment struct) {
        super(struct);
    }

    public EM3RawSpecific(SegmentAllocator allocator) {
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

    private static final OfInt ping_counterLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_counter"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int ping_counter
     * }
     */
    public static final OfInt ping_counterLAYOUT() {
        return ping_counterLAYOUT;
    }

    private static final long ping_counter$OFFSET = 4;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int ping_counter
     * }
     */
    public static final long ping_counter$offset() {
        return ping_counter$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ping_counter
     * }
     */
    public static int ping_counter(MemorySegment struct) {
        return struct.get(ping_counterLAYOUT, ping_counter$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ping_counter
     * }
     */
    public static void ping_counter(MemorySegment struct, int fieldValue) {
        struct.set(ping_counterLAYOUT, ping_counter$OFFSET, fieldValue);
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

    private static final OfInt valid_detectionsLAYOUT = (OfInt)LAYOUT.select(groupElement("valid_detections"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int valid_detections
     * }
     */
    public static final OfInt valid_detectionsLAYOUT() {
        return valid_detectionsLAYOUT;
    }

    private static final long valid_detections$OFFSET = 32;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int valid_detections
     * }
     */
    public static final long valid_detections$offset() {
        return valid_detections$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int valid_detections
     * }
     */
    public static int valid_detections(MemorySegment struct) {
        return struct.get(valid_detectionsLAYOUT, valid_detections$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int valid_detections
     * }
     */
    public static void valid_detections(MemorySegment struct, int fieldValue) {
        struct.set(valid_detectionsLAYOUT, valid_detections$OFFSET, fieldValue);
    }

    private static final OfDouble sampling_frequencyLAYOUT = (OfDouble)LAYOUT.select(groupElement("sampling_frequency"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double sampling_frequency
     * }
     */
    public static final OfDouble sampling_frequencyLAYOUT() {
        return sampling_frequencyLAYOUT;
    }

    private static final long sampling_frequency$OFFSET = 40;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double sampling_frequency
     * }
     */
    public static final long sampling_frequency$offset() {
        return sampling_frequency$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double sampling_frequency
     * }
     */
    public static double sampling_frequency(MemorySegment struct) {
        return struct.get(sampling_frequencyLAYOUT, sampling_frequency$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double sampling_frequency
     * }
     */
    public static void sampling_frequency(MemorySegment struct, double fieldValue) {
        struct.set(sampling_frequencyLAYOUT, sampling_frequency$OFFSET, fieldValue);
    }

    private static final OfDouble vehicle_depthLAYOUT = (OfDouble)LAYOUT.select(groupElement("vehicle_depth"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double vehicle_depth
     * }
     */
    public static final OfDouble vehicle_depthLAYOUT() {
        return vehicle_depthLAYOUT;
    }

    private static final long vehicle_depth$OFFSET = 48;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double vehicle_depth
     * }
     */
    public static final long vehicle_depth$offset() {
        return vehicle_depth$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double vehicle_depth
     * }
     */
    public static double vehicle_depth(MemorySegment struct) {
        return struct.get(vehicle_depthLAYOUT, vehicle_depth$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double vehicle_depth
     * }
     */
    public static void vehicle_depth(MemorySegment struct, double fieldValue) {
        struct.set(vehicle_depthLAYOUT, vehicle_depth$OFFSET, fieldValue);
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

    private static final long depth_difference$OFFSET = 56;

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

    private static final long offset_multiplier$OFFSET = 64;

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

    private static final SequenceLayout spare_1LAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare_1"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char spare_1[16]
     * }
     */
    public static final SequenceLayout spare_1LAYOUT() {
        return spare_1LAYOUT;
    }

    private static final long spare_1$OFFSET = 68;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char spare_1[16]
     * }
     */
    public static final long spare_1$offset() {
        return spare_1$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare_1[16]
     * }
     */
    public static MemorySegment spare_1(MemorySegment struct) {
        return struct.asSlice(spare_1$OFFSET, spare_1LAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char spare_1[16]
     * }
     */
    public static void spare_1(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare_1$OFFSET, spare_1LAYOUT.byteSize());
    }

    private static long[] spare_1$DIMS = { 16 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * unsigned char spare_1[16]
     * }
     */
    public static long[] spare_1$dimensions() {
        return spare_1$DIMS;
    }
    private static final VarHandle spare_1$ELEM_HANDLE = spare_1LAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char spare_1[16]
     * }
     */
    public static byte spare_1(MemorySegment struct, long index0) {
        return (byte)spare_1$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char spare_1[16]
     * }
     */
    public static void spare_1(MemorySegment struct, long index0, byte fieldValue) {
        spare_1$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

    private static final OfInt transmit_sectorsLAYOUT = (OfInt)LAYOUT.select(groupElement("transmit_sectors"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int transmit_sectors
     * }
     */
    public static final OfInt transmit_sectorsLAYOUT() {
        return transmit_sectorsLAYOUT;
    }

    private static final long transmit_sectors$OFFSET = 84;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int transmit_sectors
     * }
     */
    public static final long transmit_sectors$offset() {
        return transmit_sectors$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int transmit_sectors
     * }
     */
    public static int transmit_sectors(MemorySegment struct) {
        return struct.get(transmit_sectorsLAYOUT, transmit_sectors$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int transmit_sectors
     * }
     */
    public static void transmit_sectors(MemorySegment struct, int fieldValue) {
        struct.set(transmit_sectorsLAYOUT, transmit_sectors$OFFSET, fieldValue);
    }

    private static final SequenceLayout sectorLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("sector"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * t_gsfEM3RawTxSector sector[20]
     * }
     */
    public static final SequenceLayout sectorLAYOUT() {
        return sectorLAYOUT;
    }

    private static final long sector$OFFSET = 88;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * t_gsfEM3RawTxSector sector[20]
     * }
     */
    public static final long sector$offset() {
        return sector$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * t_gsfEM3RawTxSector sector[20]
     * }
     */
    public static MemorySegment sector(MemorySegment struct) {
        return struct.asSlice(sector$OFFSET, sectorLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * t_gsfEM3RawTxSector sector[20]
     * }
     */
    public static void sector(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, sector$OFFSET, sectorLAYOUT.byteSize());
    }

    private static long[] sector$DIMS = { 20 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * t_gsfEM3RawTxSector sector[20]
     * }
     */
    public static long[] sector$dimensions() {
        return sector$DIMS;
    }
    private static final MethodHandle sector$ELEM_HANDLE = sectorLAYOUT.sliceHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * t_gsfEM3RawTxSector sector[20]
     * }
     */
    public static MemorySegment sector(MemorySegment struct, long index0) {
        try {
            return (MemorySegment)sector$ELEM_HANDLE.invokeExact(struct, 0L, index0);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * t_gsfEM3RawTxSector sector[20]
     * }
     */
    public static void sector(MemorySegment struct, long index0, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, sector(struct, index0), 0L, EM3RawTxSector.LAYOUT.byteSize());
    }

    private static final SequenceLayout spare_2LAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare_2"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char spare_2[16]
     * }
     */
    public static final SequenceLayout spare_2LAYOUT() {
        return spare_2LAYOUT;
    }

    private static final long spare_2$OFFSET = 1528;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char spare_2[16]
     * }
     */
    public static final long spare_2$offset() {
        return spare_2$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare_2[16]
     * }
     */
    public static MemorySegment spare_2(MemorySegment struct) {
        return struct.asSlice(spare_2$OFFSET, spare_2LAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char spare_2[16]
     * }
     */
    public static void spare_2(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare_2$OFFSET, spare_2LAYOUT.byteSize());
    }

    private static long[] spare_2$DIMS = { 16 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * unsigned char spare_2[16]
     * }
     */
    public static long[] spare_2$dimensions() {
        return spare_2$DIMS;
    }
    private static final VarHandle spare_2$ELEM_HANDLE = spare_2LAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char spare_2[16]
     * }
     */
    public static byte spare_2(MemorySegment struct, long index0) {
        return (byte)spare_2$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char spare_2[16]
     * }
     */
    public static void spare_2(MemorySegment struct, long index0, byte fieldValue) {
        spare_2$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

    private static final GroupLayout run_timeLAYOUT = (GroupLayout)LAYOUT.select(groupElement("run_time"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * t_gsfEMRunTime run_time
     * }
     */
    public static final GroupLayout run_timeLAYOUT() {
        return run_timeLAYOUT;
    }

    private static final long run_time$OFFSET = 1544;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * t_gsfEMRunTime run_time
     * }
     */
    public static final long run_time$offset() {
        return run_time$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * t_gsfEMRunTime run_time
     * }
     */
    public static MemorySegment run_time(MemorySegment struct) {
        return struct.asSlice(run_time$OFFSET, run_timeLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * t_gsfEMRunTime run_time
     * }
     */
    public static void run_time(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, run_time$OFFSET, run_timeLAYOUT.byteSize());
    }

    private static final GroupLayout pu_statusLAYOUT = (GroupLayout)LAYOUT.select(groupElement("pu_status"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * t_gsfEMPUStatus pu_status
     * }
     */
    public static final GroupLayout pu_statusLAYOUT() {
        return pu_statusLAYOUT;
    }

    private static final long pu_status$OFFSET = 1744;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * t_gsfEMPUStatus pu_status
     * }
     */
    public static final long pu_status$offset() {
        return pu_status$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * t_gsfEMPUStatus pu_status
     * }
     */
    public static MemorySegment pu_status(MemorySegment struct) {
        return struct.asSlice(pu_status$OFFSET, pu_statusLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * t_gsfEMPUStatus pu_status
     * }
     */
    public static void pu_status(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, pu_status$OFFSET, pu_statusLAYOUT.byteSize());
    }

}

