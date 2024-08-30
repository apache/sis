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
public final class EM4ImagerySpecific extends StructClass {


    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_DOUBLE.withName("sampling_frequency"),
        GSF.C_DOUBLE.withName("mean_absorption"),
        GSF.C_DOUBLE.withName("tx_pulse_length"),
        GSF.C_INT.withName("range_norm"),
        GSF.C_INT.withName("start_tvg_ramp"),
        GSF.C_INT.withName("stop_tvg_ramp"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("bsn"),
        GSF.C_DOUBLE.withName("bso"),
        GSF.C_DOUBLE.withName("tx_beam_width"),
        GSF.C_DOUBLE.withName("tvg_cross_over"),
        GSF.C_SHORT.withName("offset"),
        GSF.C_SHORT.withName("scale"),
        MemoryLayout.sequenceLayout(20, GSF.C_CHAR).withName("spare")
    ).withName("t_gsfEM4ImagerySpecific");

    public EM4ImagerySpecific(MemorySegment struct) {
        super(struct);
    }

    public EM4ImagerySpecific(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
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

    private static final long sampling_frequency$OFFSET = 0;

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

    private static final OfDouble mean_absorptionLAYOUT = (OfDouble)LAYOUT.select(groupElement("mean_absorption"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double mean_absorption
     * }
     */
    public static final OfDouble mean_absorptionLAYOUT() {
        return mean_absorptionLAYOUT;
    }

    private static final long mean_absorption$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double mean_absorption
     * }
     */
    public static final long mean_absorption$offset() {
        return mean_absorption$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double mean_absorption
     * }
     */
    public static double mean_absorption(MemorySegment struct) {
        return struct.get(mean_absorptionLAYOUT, mean_absorption$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double mean_absorption
     * }
     */
    public static void mean_absorption(MemorySegment struct, double fieldValue) {
        struct.set(mean_absorptionLAYOUT, mean_absorption$OFFSET, fieldValue);
    }

    private static final OfDouble tx_pulse_lengthLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_pulse_length"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double tx_pulse_length
     * }
     */
    public static final OfDouble tx_pulse_lengthLAYOUT() {
        return tx_pulse_lengthLAYOUT;
    }

    private static final long tx_pulse_length$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double tx_pulse_length
     * }
     */
    public static final long tx_pulse_length$offset() {
        return tx_pulse_length$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_pulse_length
     * }
     */
    public static double tx_pulse_length(MemorySegment struct) {
        return struct.get(tx_pulse_lengthLAYOUT, tx_pulse_length$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_pulse_length
     * }
     */
    public static void tx_pulse_length(MemorySegment struct, double fieldValue) {
        struct.set(tx_pulse_lengthLAYOUT, tx_pulse_length$OFFSET, fieldValue);
    }

    private static final OfInt range_normLAYOUT = (OfInt)LAYOUT.select(groupElement("range_norm"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int range_norm
     * }
     */
    public static final OfInt range_normLAYOUT() {
        return range_normLAYOUT;
    }

    private static final long range_norm$OFFSET = 24;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int range_norm
     * }
     */
    public static final long range_norm$offset() {
        return range_norm$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int range_norm
     * }
     */
    public static int range_norm(MemorySegment struct) {
        return struct.get(range_normLAYOUT, range_norm$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int range_norm
     * }
     */
    public static void range_norm(MemorySegment struct, int fieldValue) {
        struct.set(range_normLAYOUT, range_norm$OFFSET, fieldValue);
    }

    private static final OfInt start_tvg_rampLAYOUT = (OfInt)LAYOUT.select(groupElement("start_tvg_ramp"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int start_tvg_ramp
     * }
     */
    public static final OfInt start_tvg_rampLAYOUT() {
        return start_tvg_rampLAYOUT;
    }

    private static final long start_tvg_ramp$OFFSET = 28;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int start_tvg_ramp
     * }
     */
    public static final long start_tvg_ramp$offset() {
        return start_tvg_ramp$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int start_tvg_ramp
     * }
     */
    public static int start_tvg_ramp(MemorySegment struct) {
        return struct.get(start_tvg_rampLAYOUT, start_tvg_ramp$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int start_tvg_ramp
     * }
     */
    public static void start_tvg_ramp(MemorySegment struct, int fieldValue) {
        struct.set(start_tvg_rampLAYOUT, start_tvg_ramp$OFFSET, fieldValue);
    }

    private static final OfInt stop_tvg_rampLAYOUT = (OfInt)LAYOUT.select(groupElement("stop_tvg_ramp"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int stop_tvg_ramp
     * }
     */
    public static final OfInt stop_tvg_rampLAYOUT() {
        return stop_tvg_rampLAYOUT;
    }

    private static final long stop_tvg_ramp$OFFSET = 32;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int stop_tvg_ramp
     * }
     */
    public static final long stop_tvg_ramp$offset() {
        return stop_tvg_ramp$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int stop_tvg_ramp
     * }
     */
    public static int stop_tvg_ramp(MemorySegment struct) {
        return struct.get(stop_tvg_rampLAYOUT, stop_tvg_ramp$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int stop_tvg_ramp
     * }
     */
    public static void stop_tvg_ramp(MemorySegment struct, int fieldValue) {
        struct.set(stop_tvg_rampLAYOUT, stop_tvg_ramp$OFFSET, fieldValue);
    }

    private static final OfDouble bsnLAYOUT = (OfDouble)LAYOUT.select(groupElement("bsn"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double bsn
     * }
     */
    public static final OfDouble bsnLAYOUT() {
        return bsnLAYOUT;
    }

    private static final long bsn$OFFSET = 40;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double bsn
     * }
     */
    public static final long bsn$offset() {
        return bsn$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double bsn
     * }
     */
    public static double bsn(MemorySegment struct) {
        return struct.get(bsnLAYOUT, bsn$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double bsn
     * }
     */
    public static void bsn(MemorySegment struct, double fieldValue) {
        struct.set(bsnLAYOUT, bsn$OFFSET, fieldValue);
    }

    private static final OfDouble bsoLAYOUT = (OfDouble)LAYOUT.select(groupElement("bso"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double bso
     * }
     */
    public static final OfDouble bsoLAYOUT() {
        return bsoLAYOUT;
    }

    private static final long bso$OFFSET = 48;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double bso
     * }
     */
    public static final long bso$offset() {
        return bso$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double bso
     * }
     */
    public static double bso(MemorySegment struct) {
        return struct.get(bsoLAYOUT, bso$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double bso
     * }
     */
    public static void bso(MemorySegment struct, double fieldValue) {
        struct.set(bsoLAYOUT, bso$OFFSET, fieldValue);
    }

    private static final OfDouble tx_beam_widthLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_beam_width"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double tx_beam_width
     * }
     */
    public static final OfDouble tx_beam_widthLAYOUT() {
        return tx_beam_widthLAYOUT;
    }

    private static final long tx_beam_width$OFFSET = 56;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double tx_beam_width
     * }
     */
    public static final long tx_beam_width$offset() {
        return tx_beam_width$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_beam_width
     * }
     */
    public static double tx_beam_width(MemorySegment struct) {
        return struct.get(tx_beam_widthLAYOUT, tx_beam_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_beam_width
     * }
     */
    public static void tx_beam_width(MemorySegment struct, double fieldValue) {
        struct.set(tx_beam_widthLAYOUT, tx_beam_width$OFFSET, fieldValue);
    }

    private static final OfDouble tvg_cross_overLAYOUT = (OfDouble)LAYOUT.select(groupElement("tvg_cross_over"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double tvg_cross_over
     * }
     */
    public static final OfDouble tvg_cross_overLAYOUT() {
        return tvg_cross_overLAYOUT;
    }

    private static final long tvg_cross_over$OFFSET = 64;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double tvg_cross_over
     * }
     */
    public static final long tvg_cross_over$offset() {
        return tvg_cross_over$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tvg_cross_over
     * }
     */
    public static double tvg_cross_over(MemorySegment struct) {
        return struct.get(tvg_cross_overLAYOUT, tvg_cross_over$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tvg_cross_over
     * }
     */
    public static void tvg_cross_over(MemorySegment struct, double fieldValue) {
        struct.set(tvg_cross_overLAYOUT, tvg_cross_over$OFFSET, fieldValue);
    }

    private static final OfShort offsetLAYOUT = (OfShort)LAYOUT.select(groupElement("offset"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * short offset
     * }
     */
    public static final OfShort offsetLAYOUT() {
        return offsetLAYOUT;
    }

    private static final long offset$OFFSET = 72;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * short offset
     * }
     */
    public static final long offset$offset() {
        return offset$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * short offset
     * }
     */
    public static short offset(MemorySegment struct) {
        return struct.get(offsetLAYOUT, offset$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * short offset
     * }
     */
    public static void offset(MemorySegment struct, short fieldValue) {
        struct.set(offsetLAYOUT, offset$OFFSET, fieldValue);
    }

    private static final OfShort scaleLAYOUT = (OfShort)LAYOUT.select(groupElement("scale"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * short scale
     * }
     */
    public static final OfShort scaleLAYOUT() {
        return scaleLAYOUT;
    }

    private static final long scale$OFFSET = 74;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * short scale
     * }
     */
    public static final long scale$offset() {
        return scale$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * short scale
     * }
     */
    public static short scale(MemorySegment struct) {
        return struct.get(scaleLAYOUT, scale$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * short scale
     * }
     */
    public static void scale(MemorySegment struct, short fieldValue) {
        struct.set(scaleLAYOUT, scale$OFFSET, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char spare[20]
     * }
     */
    public static final SequenceLayout spareLAYOUT() {
        return spareLAYOUT;
    }

    private static final long spare$OFFSET = 76;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char spare[20]
     * }
     */
    public static final long spare$offset() {
        return spare$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare[20]
     * }
     */
    public static MemorySegment spare(MemorySegment struct) {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char spare[20]
     * }
     */
    public static void spare(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static long[] spare$DIMS = { 20 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * unsigned char spare[20]
     * }
     */
    public static long[] spare$dimensions() {
        return spare$DIMS;
    }
    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char spare[20]
     * }
     */
    public static byte spare(MemorySegment struct, long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char spare[20]
     * }
     */
    public static void spare(MemorySegment struct, long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

}

