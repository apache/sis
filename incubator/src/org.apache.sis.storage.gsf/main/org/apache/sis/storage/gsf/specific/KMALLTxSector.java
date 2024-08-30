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
public final class KMALLTxSector extends StructClass {

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_INT.withName("txSectorNumb"),
        GSF.C_INT.withName("txArrNumber"),
        GSF.C_INT.withName("txSubArray"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("sectorTransmitDelay_sec"),
        GSF.C_DOUBLE.withName("tiltAngleReTx_deg"),
        GSF.C_DOUBLE.withName("txNominalSourceLevel_dB"),
        GSF.C_DOUBLE.withName("txFocusRange_m"),
        GSF.C_DOUBLE.withName("centreFreq_Hz"),
        GSF.C_DOUBLE.withName("signalBandWidth_Hz"),
        GSF.C_DOUBLE.withName("totalSignalLength_sec"),
        GSF.C_INT.withName("pulseShading"),
        GSF.C_INT.withName("signalWaveForm"),
        MemoryLayout.sequenceLayout(20, GSF.C_CHAR).withName("spare1"),
        MemoryLayout.paddingLayout(4)
    ).withName("t_gsfKMALLTxSector");

    public KMALLTxSector(MemorySegment struct) {
        super(struct);
    }

    public KMALLTxSector(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfInt txSectorNumbLAYOUT = (OfInt)LAYOUT.select(groupElement("txSectorNumb"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int txSectorNumb
     * }
     */
    public static final OfInt txSectorNumbLAYOUT() {
        return txSectorNumbLAYOUT;
    }

    private static final long txSectorNumb$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int txSectorNumb
     * }
     */
    public static final long txSectorNumb$offset() {
        return txSectorNumb$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int txSectorNumb
     * }
     */
    public static int txSectorNumb(MemorySegment struct) {
        return struct.get(txSectorNumbLAYOUT, txSectorNumb$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int txSectorNumb
     * }
     */
    public static void txSectorNumb(MemorySegment struct, int fieldValue) {
        struct.set(txSectorNumbLAYOUT, txSectorNumb$OFFSET, fieldValue);
    }

    private static final OfInt txArrNumberLAYOUT = (OfInt)LAYOUT.select(groupElement("txArrNumber"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int txArrNumber
     * }
     */
    public static final OfInt txArrNumberLAYOUT() {
        return txArrNumberLAYOUT;
    }

    private static final long txArrNumber$OFFSET = 4;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int txArrNumber
     * }
     */
    public static final long txArrNumber$offset() {
        return txArrNumber$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int txArrNumber
     * }
     */
    public static int txArrNumber(MemorySegment struct) {
        return struct.get(txArrNumberLAYOUT, txArrNumber$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int txArrNumber
     * }
     */
    public static void txArrNumber(MemorySegment struct, int fieldValue) {
        struct.set(txArrNumberLAYOUT, txArrNumber$OFFSET, fieldValue);
    }

    private static final OfInt txSubArrayLAYOUT = (OfInt)LAYOUT.select(groupElement("txSubArray"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int txSubArray
     * }
     */
    public static final OfInt txSubArrayLAYOUT() {
        return txSubArrayLAYOUT;
    }

    private static final long txSubArray$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int txSubArray
     * }
     */
    public static final long txSubArray$offset() {
        return txSubArray$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int txSubArray
     * }
     */
    public static int txSubArray(MemorySegment struct) {
        return struct.get(txSubArrayLAYOUT, txSubArray$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int txSubArray
     * }
     */
    public static void txSubArray(MemorySegment struct, int fieldValue) {
        struct.set(txSubArrayLAYOUT, txSubArray$OFFSET, fieldValue);
    }

    private static final OfDouble sectorTransmitDelay_secLAYOUT = (OfDouble)LAYOUT.select(groupElement("sectorTransmitDelay_sec"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double sectorTransmitDelay_sec
     * }
     */
    public static final OfDouble sectorTransmitDelay_secLAYOUT() {
        return sectorTransmitDelay_secLAYOUT;
    }

    private static final long sectorTransmitDelay_sec$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double sectorTransmitDelay_sec
     * }
     */
    public static final long sectorTransmitDelay_sec$offset() {
        return sectorTransmitDelay_sec$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double sectorTransmitDelay_sec
     * }
     */
    public static double sectorTransmitDelay_sec(MemorySegment struct) {
        return struct.get(sectorTransmitDelay_secLAYOUT, sectorTransmitDelay_sec$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double sectorTransmitDelay_sec
     * }
     */
    public static void sectorTransmitDelay_sec(MemorySegment struct, double fieldValue) {
        struct.set(sectorTransmitDelay_secLAYOUT, sectorTransmitDelay_sec$OFFSET, fieldValue);
    }

    private static final OfDouble tiltAngleReTx_degLAYOUT = (OfDouble)LAYOUT.select(groupElement("tiltAngleReTx_deg"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double tiltAngleReTx_deg
     * }
     */
    public static final OfDouble tiltAngleReTx_degLAYOUT() {
        return tiltAngleReTx_degLAYOUT;
    }

    private static final long tiltAngleReTx_deg$OFFSET = 24;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double tiltAngleReTx_deg
     * }
     */
    public static final long tiltAngleReTx_deg$offset() {
        return tiltAngleReTx_deg$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tiltAngleReTx_deg
     * }
     */
    public static double tiltAngleReTx_deg(MemorySegment struct) {
        return struct.get(tiltAngleReTx_degLAYOUT, tiltAngleReTx_deg$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tiltAngleReTx_deg
     * }
     */
    public static void tiltAngleReTx_deg(MemorySegment struct, double fieldValue) {
        struct.set(tiltAngleReTx_degLAYOUT, tiltAngleReTx_deg$OFFSET, fieldValue);
    }

    private static final OfDouble txNominalSourceLevel_dBLAYOUT = (OfDouble)LAYOUT.select(groupElement("txNominalSourceLevel_dB"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double txNominalSourceLevel_dB
     * }
     */
    public static final OfDouble txNominalSourceLevel_dBLAYOUT() {
        return txNominalSourceLevel_dBLAYOUT;
    }

    private static final long txNominalSourceLevel_dB$OFFSET = 32;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double txNominalSourceLevel_dB
     * }
     */
    public static final long txNominalSourceLevel_dB$offset() {
        return txNominalSourceLevel_dB$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double txNominalSourceLevel_dB
     * }
     */
    public static double txNominalSourceLevel_dB(MemorySegment struct) {
        return struct.get(txNominalSourceLevel_dBLAYOUT, txNominalSourceLevel_dB$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double txNominalSourceLevel_dB
     * }
     */
    public static void txNominalSourceLevel_dB(MemorySegment struct, double fieldValue) {
        struct.set(txNominalSourceLevel_dBLAYOUT, txNominalSourceLevel_dB$OFFSET, fieldValue);
    }

    private static final OfDouble txFocusRange_mLAYOUT = (OfDouble)LAYOUT.select(groupElement("txFocusRange_m"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double txFocusRange_m
     * }
     */
    public static final OfDouble txFocusRange_mLAYOUT() {
        return txFocusRange_mLAYOUT;
    }

    private static final long txFocusRange_m$OFFSET = 40;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double txFocusRange_m
     * }
     */
    public static final long txFocusRange_m$offset() {
        return txFocusRange_m$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double txFocusRange_m
     * }
     */
    public static double txFocusRange_m(MemorySegment struct) {
        return struct.get(txFocusRange_mLAYOUT, txFocusRange_m$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double txFocusRange_m
     * }
     */
    public static void txFocusRange_m(MemorySegment struct, double fieldValue) {
        struct.set(txFocusRange_mLAYOUT, txFocusRange_m$OFFSET, fieldValue);
    }

    private static final OfDouble centreFreq_HzLAYOUT = (OfDouble)LAYOUT.select(groupElement("centreFreq_Hz"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double centreFreq_Hz
     * }
     */
    public static final OfDouble centreFreq_HzLAYOUT() {
        return centreFreq_HzLAYOUT;
    }

    private static final long centreFreq_Hz$OFFSET = 48;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double centreFreq_Hz
     * }
     */
    public static final long centreFreq_Hz$offset() {
        return centreFreq_Hz$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double centreFreq_Hz
     * }
     */
    public static double centreFreq_Hz(MemorySegment struct) {
        return struct.get(centreFreq_HzLAYOUT, centreFreq_Hz$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double centreFreq_Hz
     * }
     */
    public static void centreFreq_Hz(MemorySegment struct, double fieldValue) {
        struct.set(centreFreq_HzLAYOUT, centreFreq_Hz$OFFSET, fieldValue);
    }

    private static final OfDouble signalBandWidth_HzLAYOUT = (OfDouble)LAYOUT.select(groupElement("signalBandWidth_Hz"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double signalBandWidth_Hz
     * }
     */
    public static final OfDouble signalBandWidth_HzLAYOUT() {
        return signalBandWidth_HzLAYOUT;
    }

    private static final long signalBandWidth_Hz$OFFSET = 56;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double signalBandWidth_Hz
     * }
     */
    public static final long signalBandWidth_Hz$offset() {
        return signalBandWidth_Hz$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double signalBandWidth_Hz
     * }
     */
    public static double signalBandWidth_Hz(MemorySegment struct) {
        return struct.get(signalBandWidth_HzLAYOUT, signalBandWidth_Hz$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double signalBandWidth_Hz
     * }
     */
    public static void signalBandWidth_Hz(MemorySegment struct, double fieldValue) {
        struct.set(signalBandWidth_HzLAYOUT, signalBandWidth_Hz$OFFSET, fieldValue);
    }

    private static final OfDouble totalSignalLength_secLAYOUT = (OfDouble)LAYOUT.select(groupElement("totalSignalLength_sec"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double totalSignalLength_sec
     * }
     */
    public static final OfDouble totalSignalLength_secLAYOUT() {
        return totalSignalLength_secLAYOUT;
    }

    private static final long totalSignalLength_sec$OFFSET = 64;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double totalSignalLength_sec
     * }
     */
    public static final long totalSignalLength_sec$offset() {
        return totalSignalLength_sec$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double totalSignalLength_sec
     * }
     */
    public static double totalSignalLength_sec(MemorySegment struct) {
        return struct.get(totalSignalLength_secLAYOUT, totalSignalLength_sec$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double totalSignalLength_sec
     * }
     */
    public static void totalSignalLength_sec(MemorySegment struct, double fieldValue) {
        struct.set(totalSignalLength_secLAYOUT, totalSignalLength_sec$OFFSET, fieldValue);
    }

    private static final OfInt pulseShadingLAYOUT = (OfInt)LAYOUT.select(groupElement("pulseShading"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int pulseShading
     * }
     */
    public static final OfInt pulseShadingLAYOUT() {
        return pulseShadingLAYOUT;
    }

    private static final long pulseShading$OFFSET = 72;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int pulseShading
     * }
     */
    public static final long pulseShading$offset() {
        return pulseShading$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int pulseShading
     * }
     */
    public static int pulseShading(MemorySegment struct) {
        return struct.get(pulseShadingLAYOUT, pulseShading$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int pulseShading
     * }
     */
    public static void pulseShading(MemorySegment struct, int fieldValue) {
        struct.set(pulseShadingLAYOUT, pulseShading$OFFSET, fieldValue);
    }

    private static final OfInt signalWaveFormLAYOUT = (OfInt)LAYOUT.select(groupElement("signalWaveForm"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int signalWaveForm
     * }
     */
    public static final OfInt signalWaveFormLAYOUT() {
        return signalWaveFormLAYOUT;
    }

    private static final long signalWaveForm$OFFSET = 76;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int signalWaveForm
     * }
     */
    public static final long signalWaveForm$offset() {
        return signalWaveForm$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int signalWaveForm
     * }
     */
    public static int signalWaveForm(MemorySegment struct) {
        return struct.get(signalWaveFormLAYOUT, signalWaveForm$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int signalWaveForm
     * }
     */
    public static void signalWaveForm(MemorySegment struct, int fieldValue) {
        struct.set(signalWaveFormLAYOUT, signalWaveForm$OFFSET, fieldValue);
    }

    private static final SequenceLayout spare1LAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare1"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char spare1[20]
     * }
     */
    public static final SequenceLayout spare1LAYOUT() {
        return spare1LAYOUT;
    }

    private static final long spare1$OFFSET = 80;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char spare1[20]
     * }
     */
    public static final long spare1$offset() {
        return spare1$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare1[20]
     * }
     */
    public static MemorySegment spare1(MemorySegment struct) {
        return struct.asSlice(spare1$OFFSET, spare1LAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char spare1[20]
     * }
     */
    public static void spare1(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare1$OFFSET, spare1LAYOUT.byteSize());
    }

    private static long[] spare1$DIMS = { 20 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * unsigned char spare1[20]
     * }
     */
    public static long[] spare1$dimensions() {
        return spare1$DIMS;
    }
    private static final VarHandle spare1$ELEM_HANDLE = spare1LAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char spare1[20]
     * }
     */
    public static byte spare1(MemorySegment struct, long index0) {
        return (byte)spare1$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char spare1[20]
     * }
     */
    public static void spare1(MemorySegment struct, long index0, byte fieldValue) {
        spare1$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

}

