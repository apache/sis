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
public final class EM4 extends StructClass {
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
        GSF.C_INT.withName("doppler_corr_scale"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("vehicle_depth"),
        MemoryLayout.sequenceLayout(16, GSF.C_CHAR).withName("spare_1"),
        GSF.C_INT.withName("transmit_sectors"),
        MemoryLayout.paddingLayout(4),
        MemoryLayout.sequenceLayout(9, EM4TxSector.LAYOUT).withName("sector"),
        MemoryLayout.sequenceLayout(16, GSF.C_CHAR).withName("spare_2"),
        EMRunTime.LAYOUT.withName("run_time"),
        EMPUStatus.LAYOUT.withName("pu_status")
    ).withName("t_gsfEM4Specific");

    public EM4(MemorySegment struct) {
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

    private static final OfInt ping_counterLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_counter"));

    private static final long ping_counter$OFFSET = 4;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ping_counter
     * }
     */
    public int ping_counter() {
        return struct.get(ping_counterLAYOUT, ping_counter$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ping_counter
     * }
     */
    public void ping_counter(int fieldValue) {
        struct.set(ping_counterLAYOUT, ping_counter$OFFSET, fieldValue);
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

    private static final OfInt valid_detectionsLAYOUT = (OfInt)LAYOUT.select(groupElement("valid_detections"));

    private static final long valid_detections$OFFSET = 32;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int valid_detections
     * }
     */
    public int valid_detections() {
        return struct.get(valid_detectionsLAYOUT, valid_detections$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int valid_detections
     * }
     */
    public void valid_detections(int fieldValue) {
        struct.set(valid_detectionsLAYOUT, valid_detections$OFFSET, fieldValue);
    }

    private static final OfDouble sampling_frequencyLAYOUT = (OfDouble)LAYOUT.select(groupElement("sampling_frequency"));

    private static final long sampling_frequency$OFFSET = 40;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double sampling_frequency
     * }
     */
    public double sampling_frequency() {
        return struct.get(sampling_frequencyLAYOUT, sampling_frequency$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double sampling_frequency
     * }
     */
    public void sampling_frequency(double fieldValue) {
        struct.set(sampling_frequencyLAYOUT, sampling_frequency$OFFSET, fieldValue);
    }

    private static final OfInt doppler_corr_scaleLAYOUT = (OfInt)LAYOUT.select(groupElement("doppler_corr_scale"));

    private static final long doppler_corr_scale$OFFSET = 48;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int doppler_corr_scale
     * }
     */
    public int doppler_corr_scale() {
        return struct.get(doppler_corr_scaleLAYOUT, doppler_corr_scale$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int doppler_corr_scale
     * }
     */
    public void doppler_corr_scale(int fieldValue) {
        struct.set(doppler_corr_scaleLAYOUT, doppler_corr_scale$OFFSET, fieldValue);
    }

    private static final OfDouble vehicle_depthLAYOUT = (OfDouble)LAYOUT.select(groupElement("vehicle_depth"));

    private static final long vehicle_depth$OFFSET = 56;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double vehicle_depth
     * }
     */
    public double vehicle_depth() {
        return struct.get(vehicle_depthLAYOUT, vehicle_depth$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double vehicle_depth
     * }
     */
    public void vehicle_depth(double fieldValue) {
        struct.set(vehicle_depthLAYOUT, vehicle_depth$OFFSET, fieldValue);
    }

    private static final SequenceLayout spare_1LAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare_1"));

    private static final long spare_1$OFFSET = 64;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare_1[16]
     * }
     */
    public MemorySegment spare_1() {
        return struct.asSlice(spare_1$OFFSET, spare_1LAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char spare_1[16]
     * }
     */
    public void spare_1(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare_1$OFFSET, spare_1LAYOUT.byteSize());
    }

    private static final VarHandle spare_1$ELEM_HANDLE = spare_1LAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char spare_1[16]
     * }
     */
    public byte spare_1(long index0) {
        return (byte)spare_1$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char spare_1[16]
     * }
     */
    public void spare_1(long index0, byte fieldValue) {
        spare_1$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

    private static final OfInt transmit_sectorsLAYOUT = (OfInt)LAYOUT.select(groupElement("transmit_sectors"));

    private static final long transmit_sectors$OFFSET = 80;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int transmit_sectors
     * }
     */
    public int transmit_sectors() {
        return struct.get(transmit_sectorsLAYOUT, transmit_sectors$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int transmit_sectors
     * }
     */
    public void transmit_sectors(int fieldValue) {
        struct.set(transmit_sectorsLAYOUT, transmit_sectors$OFFSET, fieldValue);
    }

    private static final SequenceLayout sectorLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("sector"));

    private static final long sector$OFFSET = 88;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * t_gsfEM4TxSector sector[9]
     * }
     */
    public MemorySegment sector() {
        return struct.asSlice(sector$OFFSET, sectorLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * t_gsfEM4TxSector sector[9]
     * }
     */
    public void sector(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, sector$OFFSET, sectorLAYOUT.byteSize());
    }

    private static final MethodHandle sector$ELEM_HANDLE = sectorLAYOUT.sliceHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * t_gsfEM4TxSector sector[9]
     * }
     */
    public MemorySegment sector(long index0) {
        try {
            return (MemorySegment)sector$ELEM_HANDLE.invokeExact(struct, 0L, index0);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * t_gsfEM4TxSector sector[9]
     * }
     */
    public void sector(long index0, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, sector(index0), 0L, EM4TxSector.LAYOUT.byteSize());
    }

    private static final SequenceLayout spare_2LAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare_2"));

    private static final long spare_2$OFFSET = 808;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare_2[16]
     * }
     */
    public MemorySegment spare_2() {
        return struct.asSlice(spare_2$OFFSET, spare_2LAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char spare_2[16]
     * }
     */
    public void spare_2(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare_2$OFFSET, spare_2LAYOUT.byteSize());
    }

    private static final VarHandle spare_2$ELEM_HANDLE = spare_2LAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char spare_2[16]
     * }
     */
    public byte spare_2(long index0) {
        return (byte)spare_2$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char spare_2[16]
     * }
     */
    public void spare_2(long index0, byte fieldValue) {
        spare_2$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

    private static final GroupLayout run_timeLAYOUT = (GroupLayout)LAYOUT.select(groupElement("run_time"));

    private static final long run_time$OFFSET = 824;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * t_gsfEMRunTime run_time
     * }
     */
    public MemorySegment run_time() {
        return struct.asSlice(run_time$OFFSET, run_timeLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * t_gsfEMRunTime run_time
     * }
     */
    public void run_time(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, run_time$OFFSET, run_timeLAYOUT.byteSize());
    }

    private static final GroupLayout pu_statusLAYOUT = (GroupLayout)LAYOUT.select(groupElement("pu_status"));

    private static final long pu_status$OFFSET = 1024;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * t_gsfEMPUStatus pu_status
     * }
     */
    public MemorySegment pu_status() {
        return struct.asSlice(pu_status$OFFSET, pu_statusLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * t_gsfEMPUStatus pu_status
     * }
     */
    public void pu_status(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, pu_status$OFFSET, pu_statusLAYOUT.byteSize());
    }
}
