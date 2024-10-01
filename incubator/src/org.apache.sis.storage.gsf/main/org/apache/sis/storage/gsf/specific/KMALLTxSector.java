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

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfInt txSectorNumbLAYOUT = (OfInt)LAYOUT.select(groupElement("txSectorNumb"));

    private static final long txSectorNumb$OFFSET = 0;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int txSectorNumb
     * }
     */
    public int txSectorNumb() {
        return struct.get(txSectorNumbLAYOUT, txSectorNumb$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int txSectorNumb
     * }
     */
    public void txSectorNumb(int fieldValue) {
        struct.set(txSectorNumbLAYOUT, txSectorNumb$OFFSET, fieldValue);
    }

    private static final OfInt txArrNumberLAYOUT = (OfInt)LAYOUT.select(groupElement("txArrNumber"));

    private static final long txArrNumber$OFFSET = 4;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int txArrNumber
     * }
     */
    public int txArrNumber() {
        return struct.get(txArrNumberLAYOUT, txArrNumber$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int txArrNumber
     * }
     */
    public void txArrNumber(int fieldValue) {
        struct.set(txArrNumberLAYOUT, txArrNumber$OFFSET, fieldValue);
    }

    private static final OfInt txSubArrayLAYOUT = (OfInt)LAYOUT.select(groupElement("txSubArray"));

    private static final long txSubArray$OFFSET = 8;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int txSubArray
     * }
     */
    public int txSubArray() {
        return struct.get(txSubArrayLAYOUT, txSubArray$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int txSubArray
     * }
     */
    public void txSubArray(int fieldValue) {
        struct.set(txSubArrayLAYOUT, txSubArray$OFFSET, fieldValue);
    }

    private static final OfDouble sectorTransmitDelay_secLAYOUT = (OfDouble)LAYOUT.select(groupElement("sectorTransmitDelay_sec"));

    private static final long sectorTransmitDelay_sec$OFFSET = 16;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double sectorTransmitDelay_sec
     * }
     */
    public double sectorTransmitDelay_sec() {
        return struct.get(sectorTransmitDelay_secLAYOUT, sectorTransmitDelay_sec$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double sectorTransmitDelay_sec
     * }
     */
    public void sectorTransmitDelay_sec(double fieldValue) {
        struct.set(sectorTransmitDelay_secLAYOUT, sectorTransmitDelay_sec$OFFSET, fieldValue);
    }

    private static final OfDouble tiltAngleReTx_degLAYOUT = (OfDouble)LAYOUT.select(groupElement("tiltAngleReTx_deg"));

    private static final long tiltAngleReTx_deg$OFFSET = 24;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tiltAngleReTx_deg
     * }
     */
    public double tiltAngleReTx_deg() {
        return struct.get(tiltAngleReTx_degLAYOUT, tiltAngleReTx_deg$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tiltAngleReTx_deg
     * }
     */
    public void tiltAngleReTx_deg(double fieldValue) {
        struct.set(tiltAngleReTx_degLAYOUT, tiltAngleReTx_deg$OFFSET, fieldValue);
    }

    private static final OfDouble txNominalSourceLevel_dBLAYOUT = (OfDouble)LAYOUT.select(groupElement("txNominalSourceLevel_dB"));

    private static final long txNominalSourceLevel_dB$OFFSET = 32;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double txNominalSourceLevel_dB
     * }
     */
    public double txNominalSourceLevel_dB() {
        return struct.get(txNominalSourceLevel_dBLAYOUT, txNominalSourceLevel_dB$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double txNominalSourceLevel_dB
     * }
     */
    public void txNominalSourceLevel_dB(double fieldValue) {
        struct.set(txNominalSourceLevel_dBLAYOUT, txNominalSourceLevel_dB$OFFSET, fieldValue);
    }

    private static final OfDouble txFocusRange_mLAYOUT = (OfDouble)LAYOUT.select(groupElement("txFocusRange_m"));

    private static final long txFocusRange_m$OFFSET = 40;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double txFocusRange_m
     * }
     */
    public double txFocusRange_m() {
        return struct.get(txFocusRange_mLAYOUT, txFocusRange_m$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double txFocusRange_m
     * }
     */
    public void txFocusRange_m(double fieldValue) {
        struct.set(txFocusRange_mLAYOUT, txFocusRange_m$OFFSET, fieldValue);
    }

    private static final OfDouble centreFreq_HzLAYOUT = (OfDouble)LAYOUT.select(groupElement("centreFreq_Hz"));

    private static final long centreFreq_Hz$OFFSET = 48;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double centreFreq_Hz
     * }
     */
    public double centreFreq_Hz() {
        return struct.get(centreFreq_HzLAYOUT, centreFreq_Hz$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double centreFreq_Hz
     * }
     */
    public void centreFreq_Hz(double fieldValue) {
        struct.set(centreFreq_HzLAYOUT, centreFreq_Hz$OFFSET, fieldValue);
    }

    private static final OfDouble signalBandWidth_HzLAYOUT = (OfDouble)LAYOUT.select(groupElement("signalBandWidth_Hz"));

    private static final long signalBandWidth_Hz$OFFSET = 56;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double signalBandWidth_Hz
     * }
     */
    public double signalBandWidth_Hz() {
        return struct.get(signalBandWidth_HzLAYOUT, signalBandWidth_Hz$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double signalBandWidth_Hz
     * }
     */
    public void signalBandWidth_Hz(double fieldValue) {
        struct.set(signalBandWidth_HzLAYOUT, signalBandWidth_Hz$OFFSET, fieldValue);
    }

    private static final OfDouble totalSignalLength_secLAYOUT = (OfDouble)LAYOUT.select(groupElement("totalSignalLength_sec"));

    private static final long totalSignalLength_sec$OFFSET = 64;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double totalSignalLength_sec
     * }
     */
    public double totalSignalLength_sec() {
        return struct.get(totalSignalLength_secLAYOUT, totalSignalLength_sec$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double totalSignalLength_sec
     * }
     */
    public void totalSignalLength_sec(double fieldValue) {
        struct.set(totalSignalLength_secLAYOUT, totalSignalLength_sec$OFFSET, fieldValue);
    }

    private static final OfInt pulseShadingLAYOUT = (OfInt)LAYOUT.select(groupElement("pulseShading"));

    private static final long pulseShading$OFFSET = 72;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int pulseShading
     * }
     */
    public int pulseShading() {
        return struct.get(pulseShadingLAYOUT, pulseShading$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int pulseShading
     * }
     */
    public void pulseShading(int fieldValue) {
        struct.set(pulseShadingLAYOUT, pulseShading$OFFSET, fieldValue);
    }

    private static final OfInt signalWaveFormLAYOUT = (OfInt)LAYOUT.select(groupElement("signalWaveForm"));

    private static final long signalWaveForm$OFFSET = 76;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int signalWaveForm
     * }
     */
    public int signalWaveForm() {
        return struct.get(signalWaveFormLAYOUT, signalWaveForm$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int signalWaveForm
     * }
     */
    public void signalWaveForm(int fieldValue) {
        struct.set(signalWaveFormLAYOUT, signalWaveForm$OFFSET, fieldValue);
    }

    private static final SequenceLayout spare1LAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare1"));

    private static final long spare1$OFFSET = 80;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare1[20]
     * }
     */
    public MemorySegment spare1() {
        return struct.asSlice(spare1$OFFSET, spare1LAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char spare1[20]
     * }
     */
    public void spare1(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare1$OFFSET, spare1LAYOUT.byteSize());
    }

    private static final VarHandle spare1$ELEM_HANDLE = spare1LAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char spare1[20]
     * }
     */
    public byte spare1(long index0) {
        return (byte)spare1$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char spare1[20]
     * }
     */
    public void spare1(long index0, byte fieldValue) {
        spare1$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }
}
