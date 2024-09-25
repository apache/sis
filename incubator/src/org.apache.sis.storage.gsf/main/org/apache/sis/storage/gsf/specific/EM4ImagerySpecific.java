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

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfDouble sampling_frequencyLAYOUT = (OfDouble)LAYOUT.select(groupElement("sampling_frequency"));

    private static final long sampling_frequency$OFFSET = 0;

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

    private static final OfDouble mean_absorptionLAYOUT = (OfDouble)LAYOUT.select(groupElement("mean_absorption"));

    private static final long mean_absorption$OFFSET = 8;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double mean_absorption
     * }
     */
    public double mean_absorption() {
        return struct.get(mean_absorptionLAYOUT, mean_absorption$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double mean_absorption
     * }
     */
    public void mean_absorption(double fieldValue) {
        struct.set(mean_absorptionLAYOUT, mean_absorption$OFFSET, fieldValue);
    }

    private static final OfDouble tx_pulse_lengthLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_pulse_length"));

    private static final long tx_pulse_length$OFFSET = 16;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_pulse_length
     * }
     */
    public double tx_pulse_length() {
        return struct.get(tx_pulse_lengthLAYOUT, tx_pulse_length$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_pulse_length
     * }
     */
    public void tx_pulse_length(double fieldValue) {
        struct.set(tx_pulse_lengthLAYOUT, tx_pulse_length$OFFSET, fieldValue);
    }

    private static final OfInt range_normLAYOUT = (OfInt)LAYOUT.select(groupElement("range_norm"));

    private static final long range_norm$OFFSET = 24;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int range_norm
     * }
     */
    public int range_norm() {
        return struct.get(range_normLAYOUT, range_norm$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int range_norm
     * }
     */
    public void range_norm(int fieldValue) {
        struct.set(range_normLAYOUT, range_norm$OFFSET, fieldValue);
    }

    private static final OfInt start_tvg_rampLAYOUT = (OfInt)LAYOUT.select(groupElement("start_tvg_ramp"));

    private static final long start_tvg_ramp$OFFSET = 28;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int start_tvg_ramp
     * }
     */
    public int start_tvg_ramp() {
        return struct.get(start_tvg_rampLAYOUT, start_tvg_ramp$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int start_tvg_ramp
     * }
     */
    public void start_tvg_ramp(int fieldValue) {
        struct.set(start_tvg_rampLAYOUT, start_tvg_ramp$OFFSET, fieldValue);
    }

    private static final OfInt stop_tvg_rampLAYOUT = (OfInt)LAYOUT.select(groupElement("stop_tvg_ramp"));

    private static final long stop_tvg_ramp$OFFSET = 32;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int stop_tvg_ramp
     * }
     */
    public int stop_tvg_ramp() {
        return struct.get(stop_tvg_rampLAYOUT, stop_tvg_ramp$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int stop_tvg_ramp
     * }
     */
    public void stop_tvg_ramp(int fieldValue) {
        struct.set(stop_tvg_rampLAYOUT, stop_tvg_ramp$OFFSET, fieldValue);
    }

    private static final OfDouble bsnLAYOUT = (OfDouble)LAYOUT.select(groupElement("bsn"));

    private static final long bsn$OFFSET = 40;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double bsn
     * }
     */
    public double bsn() {
        return struct.get(bsnLAYOUT, bsn$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double bsn
     * }
     */
    public void bsn(double fieldValue) {
        struct.set(bsnLAYOUT, bsn$OFFSET, fieldValue);
    }

    private static final OfDouble bsoLAYOUT = (OfDouble)LAYOUT.select(groupElement("bso"));

    private static final long bso$OFFSET = 48;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double bso
     * }
     */
    public double bso() {
        return struct.get(bsoLAYOUT, bso$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double bso
     * }
     */
    public void bso(double fieldValue) {
        struct.set(bsoLAYOUT, bso$OFFSET, fieldValue);
    }

    private static final OfDouble tx_beam_widthLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_beam_width"));

    private static final long tx_beam_width$OFFSET = 56;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_beam_width
     * }
     */
    public double tx_beam_width() {
        return struct.get(tx_beam_widthLAYOUT, tx_beam_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_beam_width
     * }
     */
    public void tx_beam_width(double fieldValue) {
        struct.set(tx_beam_widthLAYOUT, tx_beam_width$OFFSET, fieldValue);
    }

    private static final OfDouble tvg_cross_overLAYOUT = (OfDouble)LAYOUT.select(groupElement("tvg_cross_over"));

    private static final long tvg_cross_over$OFFSET = 64;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tvg_cross_over
     * }
     */
    public double tvg_cross_over() {
        return struct.get(tvg_cross_overLAYOUT, tvg_cross_over$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tvg_cross_over
     * }
     */
    public void tvg_cross_over(double fieldValue) {
        struct.set(tvg_cross_overLAYOUT, tvg_cross_over$OFFSET, fieldValue);
    }

    private static final OfShort offsetLAYOUT = (OfShort)LAYOUT.select(groupElement("offset"));

    private static final long offset$OFFSET = 72;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * short offset
     * }
     */
    public short offset() {
        return struct.get(offsetLAYOUT, offset$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * short offset
     * }
     */
    public void offset(short fieldValue) {
        struct.set(offsetLAYOUT, offset$OFFSET, fieldValue);
    }

    private static final OfShort scaleLAYOUT = (OfShort)LAYOUT.select(groupElement("scale"));

    private static final long scale$OFFSET = 74;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * short scale
     * }
     */
    public short scale() {
        return struct.get(scaleLAYOUT, scale$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * short scale
     * }
     */
    public void scale(short fieldValue) {
        struct.set(scaleLAYOUT, scale$OFFSET, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    private static final long spare$OFFSET = 76;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare[20]
     * }
     */
    public MemorySegment spare() {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char spare[20]
     * }
     */
    public void spare(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char spare[20]
     * }
     */
    public byte spare(long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char spare[20]
     * }
     */
    public void spare(long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }
}
