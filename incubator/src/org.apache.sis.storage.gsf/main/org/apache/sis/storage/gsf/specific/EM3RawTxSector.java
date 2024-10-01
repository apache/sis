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
public final class EM3RawTxSector extends StructClass {
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_DOUBLE.withName("tilt_angle"),
        GSF.C_DOUBLE.withName("focus_range"),
        GSF.C_DOUBLE.withName("signal_length"),
        GSF.C_DOUBLE.withName("transmit_delay"),
        GSF.C_DOUBLE.withName("center_frequency"),
        GSF.C_INT.withName("waveform_id"),
        GSF.C_INT.withName("sector_number"),
        GSF.C_DOUBLE.withName("signal_bandwidth"),
        MemoryLayout.sequenceLayout(16, GSF.C_CHAR).withName("spare")
    ).withName("t_gsfEM3RawTxSector");

    public EM3RawTxSector(MemorySegment struct) {
        super(struct);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfDouble tilt_angleLAYOUT = (OfDouble)LAYOUT.select(groupElement("tilt_angle"));

    private static final long tilt_angle$OFFSET = 0;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tilt_angle
     * }
     */
    public double tilt_angle() {
        return struct.get(tilt_angleLAYOUT, tilt_angle$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tilt_angle
     * }
     */
    public void tilt_angle(double fieldValue) {
        struct.set(tilt_angleLAYOUT, tilt_angle$OFFSET, fieldValue);
    }

    private static final OfDouble focus_rangeLAYOUT = (OfDouble)LAYOUT.select(groupElement("focus_range"));

    private static final long focus_range$OFFSET = 8;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double focus_range
     * }
     */
    public double focus_range() {
        return struct.get(focus_rangeLAYOUT, focus_range$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double focus_range
     * }
     */
    public void focus_range(double fieldValue) {
        struct.set(focus_rangeLAYOUT, focus_range$OFFSET, fieldValue);
    }

    private static final OfDouble signal_lengthLAYOUT = (OfDouble)LAYOUT.select(groupElement("signal_length"));

    private static final long signal_length$OFFSET = 16;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double signal_length
     * }
     */
    public double signal_length() {
        return struct.get(signal_lengthLAYOUT, signal_length$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double signal_length
     * }
     */
    public void signal_length(double fieldValue) {
        struct.set(signal_lengthLAYOUT, signal_length$OFFSET, fieldValue);
    }

    private static final OfDouble transmit_delayLAYOUT = (OfDouble)LAYOUT.select(groupElement("transmit_delay"));

    private static final long transmit_delay$OFFSET = 24;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double transmit_delay
     * }
     */
    public double transmit_delay() {
        return struct.get(transmit_delayLAYOUT, transmit_delay$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double transmit_delay
     * }
     */
    public void transmit_delay(double fieldValue) {
        struct.set(transmit_delayLAYOUT, transmit_delay$OFFSET, fieldValue);
    }

    private static final OfDouble center_frequencyLAYOUT = (OfDouble)LAYOUT.select(groupElement("center_frequency"));

    private static final long center_frequency$OFFSET = 32;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double center_frequency
     * }
     */
    public double center_frequency() {
        return struct.get(center_frequencyLAYOUT, center_frequency$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double center_frequency
     * }
     */
    public void center_frequency(double fieldValue) {
        struct.set(center_frequencyLAYOUT, center_frequency$OFFSET, fieldValue);
    }

    private static final OfInt waveform_idLAYOUT = (OfInt)LAYOUT.select(groupElement("waveform_id"));

    private static final long waveform_id$OFFSET = 40;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int waveform_id
     * }
     */
    public int waveform_id() {
        return struct.get(waveform_idLAYOUT, waveform_id$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int waveform_id
     * }
     */
    public void waveform_id(int fieldValue) {
        struct.set(waveform_idLAYOUT, waveform_id$OFFSET, fieldValue);
    }

    private static final OfInt sector_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("sector_number"));

    private static final long sector_number$OFFSET = 44;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int sector_number
     * }
     */
    public int sector_number() {
        return struct.get(sector_numberLAYOUT, sector_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int sector_number
     * }
     */
    public void sector_number(int fieldValue) {
        struct.set(sector_numberLAYOUT, sector_number$OFFSET, fieldValue);
    }

    private static final OfDouble signal_bandwidthLAYOUT = (OfDouble)LAYOUT.select(groupElement("signal_bandwidth"));

    private static final long signal_bandwidth$OFFSET = 48;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double signal_bandwidth
     * }
     */
    public double signal_bandwidth() {
        return struct.get(signal_bandwidthLAYOUT, signal_bandwidth$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double signal_bandwidth
     * }
     */
    public void signal_bandwidth(double fieldValue) {
        struct.set(signal_bandwidthLAYOUT, signal_bandwidth$OFFSET, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    private static final long spare$OFFSET = 56;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public MemorySegment spare() {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public void spare(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public byte spare(long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public void spare(long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }
}
