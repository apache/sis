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

import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
import org.apache.sis.storage.gsf.GSF;
import org.apache.sis.storage.gsf.StructClass;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class KMALLSpecific extends StructClass {
    private static final OfInt LAYOUT_GSFKMALLVERSION;
    private static final OfInt LAYOUT_DGMTYPE;
    private static final OfInt LAYOUT_DGMVERSION;
    private static final OfInt LAYOUT_SYSTEMID;
    private static final OfInt LAYOUT_ECHOSOUNDERID;
    private static final SequenceLayout LAYOUT_SPARE1;
    private static final OfInt LAYOUT_NUMBYTESCMNPART;
    private static final OfInt LAYOUT_PINGCNT;
    private static final OfInt LAYOUT_RXFANSPERPING;
    private static final OfInt LAYOUT_RXFANINDEX;
    private static final OfInt LAYOUT_SWATHSPERPING;
    private static final OfInt LAYOUT_SWATHALONGPOSITION;
    private static final OfInt LAYOUT_TXTRANSDUCERIND;
    private static final OfInt LAYOUT_RXTRANSDUCERIND;
    private static final OfInt LAYOUT_NUMRXTRANSDUCERS;
    private static final OfInt LAYOUT_ALGORITHMTYPE;
    private static final SequenceLayout LAYOUT_SPARE2;
    private static final OfInt LAYOUT_NUMBYTESINFODATA;
    private static final OfDouble LAYOUT_PINGRATE_HZ;
    private static final OfInt LAYOUT_BEAMSPACING;
    private static final OfInt LAYOUT_DEPTHMODE;
    private static final OfInt LAYOUT_SUBDEPTHMODE;
    private static final OfInt LAYOUT_DISTANCEBTWSWATH;
    private static final OfInt LAYOUT_DETECTIONMODE;
    private static final OfInt LAYOUT_PULSEFORM;
    private static final OfDouble LAYOUT_FREQUENCYMODE_HZ;
    private static final OfDouble LAYOUT_FREQRANGELOWLIM_HZ;
    private static final OfDouble LAYOUT_FREQRANGEHIGHLIM_HZ;
    private static final OfDouble LAYOUT_MAXTOTALTXPULSELENGTH_SEC;
    private static final OfDouble LAYOUT_MAXEFFTXPULSELENGTH_SEC;
    private static final OfDouble LAYOUT_MAXEFFTXBANDWIDTH_HZ;
    private static final OfDouble LAYOUT_ABSCOEFF_DBPERKM;
    private static final OfDouble LAYOUT_PORTSECTOREDGE_DEG;
    private static final OfDouble LAYOUT_STARBSECTOREDGE_DEG;
    private static final OfDouble LAYOUT_PORTMEANCOV_DEG;
    private static final OfDouble LAYOUT_STARBMEANCOV_DEG;
    private static final OfDouble LAYOUT_PORTMEANCOV_M;
    private static final OfDouble LAYOUT_STARBMEANCOV_M;
    private static final OfInt LAYOUT_MODEANDSTABILISATION;
    private static final OfInt LAYOUT_RUNTIMEFILTER1;
    private static final OfInt LAYOUT_RUNTIMEFILTER2;
    private static final OfInt LAYOUT_PIPETRACKINGSTATUS;
    private static final OfDouble LAYOUT_TRANSMITARRAYSIZEUSED_DEG;
    private static final OfDouble LAYOUT_RECEIVEARRAYSIZEUSED_DEG;
    private static final OfDouble LAYOUT_TRANSMITPOWER_DB;
    private static final OfInt LAYOUT_SLRAMPUPTIMEREMAINING;
    private static final OfDouble LAYOUT_YAWANGLE_DEG;
    private static final OfInt LAYOUT_NUMTXSECTORS;
    private static final OfInt LAYOUT_NUMBYTESPERTXSECTOR;
    private static final OfDouble LAYOUT_HEADINGVESSEL_DEG;
    private static final OfDouble LAYOUT_SOUNDSPEEDATTXDEPTH_MPERSEC;
    private static final OfDouble LAYOUT_TXTRANSDUCERDEPTH_M;
    private static final OfDouble LAYOUT_Z_WATERLEVELREREFPOINT_M;
    private static final OfDouble LAYOUT_X_KMALLTOALL_M;
    private static final OfDouble LAYOUT_Y_KMALLTOALL_M;
    private static final OfInt LAYOUT_LATLONGINFO;
    private static final OfInt LAYOUT_POSSENSORSTATUS;
    private static final OfInt LAYOUT_ATTITUDESENSORSTATUS;
    private static final OfDouble LAYOUT_LATITUDE_DEG;
    private static final OfDouble LAYOUT_LONGITUDE_DEG;
    private static final OfDouble LAYOUT_ELLIPSOIDHEIGHTREREFPOINT_M;
    private static final SequenceLayout LAYOUT_SPARE3;
    private static final SequenceLayout LAYOUT_SECTOR;
    private static final OfInt LAYOUT_NUMBYTESRXINFO;
    private static final OfInt LAYOUT_NUMSOUNDINGSMAXMAIN;
    private static final OfInt LAYOUT_NUMSOUNDINGSVALIDMAIN;
    private static final OfInt LAYOUT_NUMBYTESPERSOUNDING;
    private static final OfDouble LAYOUT_WCSAMPLERATE;
    private static final OfDouble LAYOUT_SEABEDIMAGESAMPLERATE;
    private static final OfDouble LAYOUT_BSNORMAL_DB;
    private static final OfDouble LAYOUT_BSOBLIQUE_DB;
    private static final OfInt LAYOUT_EXTRADETECTIONALARMFLAG;
    private static final OfInt LAYOUT_NUMEXTRADETECTIONS;
    private static final OfInt LAYOUT_NUMEXTRADETECTIONCLASSES;
    private static final OfInt LAYOUT_NUMBYTESPERCLASS;
    private static final SequenceLayout LAYOUT_SPARE4;
    private static final SequenceLayout LAYOUT_EXTRADETCLASSINFO;
    private static final SequenceLayout LAYOUT_SPARE5;

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        LAYOUT_GSFKMALLVERSION = GSF.C_INT.withName("gsfKMALLVersion"),
        LAYOUT_DGMTYPE = GSF.C_INT.withName("dgmType"),
        LAYOUT_DGMVERSION = GSF.C_INT.withName("dgmVersion"),
        LAYOUT_SYSTEMID = GSF.C_INT.withName("systemID"),
        LAYOUT_ECHOSOUNDERID = GSF.C_INT.withName("echoSounderID"),
        LAYOUT_SPARE1 = MemoryLayout.sequenceLayout(8, GSF.C_CHAR).withName("spare1"),
        LAYOUT_NUMBYTESCMNPART = GSF.C_INT.withName("numBytesCmnPart"),
        LAYOUT_PINGCNT = GSF.C_INT.withName("pingCnt"),
        LAYOUT_RXFANSPERPING = GSF.C_INT.withName("rxFansPerPing"),
        LAYOUT_RXFANINDEX = GSF.C_INT.withName("rxFanIndex"),
        LAYOUT_SWATHSPERPING = GSF.C_INT.withName("swathsPerPing"),
        LAYOUT_SWATHALONGPOSITION = GSF.C_INT.withName("swathAlongPosition"),
        LAYOUT_TXTRANSDUCERIND = GSF.C_INT.withName("txTransducerInd"),
        LAYOUT_RXTRANSDUCERIND = GSF.C_INT.withName("rxTransducerInd"),
        LAYOUT_NUMRXTRANSDUCERS = GSF.C_INT.withName("numRxTransducers"),
        LAYOUT_ALGORITHMTYPE = GSF.C_INT.withName("algorithmType"),
        LAYOUT_SPARE2 = MemoryLayout.sequenceLayout(16, GSF.C_CHAR).withName("spare2"),
        LAYOUT_NUMBYTESINFODATA = GSF.C_INT.withName("numBytesInfoData"),
        LAYOUT_PINGRATE_HZ = GSF.C_DOUBLE.withName("pingRate_Hz"),
        LAYOUT_BEAMSPACING = GSF.C_INT.withName("beamSpacing"),
        LAYOUT_DEPTHMODE = GSF.C_INT.withName("depthMode"),
        LAYOUT_SUBDEPTHMODE = GSF.C_INT.withName("subDepthMode"),
        LAYOUT_DISTANCEBTWSWATH = GSF.C_INT.withName("distanceBtwSwath"),
        LAYOUT_DETECTIONMODE = GSF.C_INT.withName("detectionMode"),
        LAYOUT_PULSEFORM = GSF.C_INT.withName("pulseForm"),
        LAYOUT_FREQUENCYMODE_HZ = GSF.C_DOUBLE.withName("frequencyMode_Hz"),
        LAYOUT_FREQRANGELOWLIM_HZ = GSF.C_DOUBLE.withName("freqRangeLowLim_Hz"),
        LAYOUT_FREQRANGEHIGHLIM_HZ = GSF.C_DOUBLE.withName("freqRangeHighLim_Hz"),
        LAYOUT_MAXTOTALTXPULSELENGTH_SEC = GSF.C_DOUBLE.withName("maxTotalTxPulseLength_sec"),
        LAYOUT_MAXEFFTXPULSELENGTH_SEC = GSF.C_DOUBLE.withName("maxEffTxPulseLength_sec"),
        LAYOUT_MAXEFFTXBANDWIDTH_HZ = GSF.C_DOUBLE.withName("maxEffTxBandWidth_Hz"),
        LAYOUT_ABSCOEFF_DBPERKM = GSF.C_DOUBLE.withName("absCoeff_dBPerkm"),
        LAYOUT_PORTSECTOREDGE_DEG = GSF.C_DOUBLE.withName("portSectorEdge_deg"),
        LAYOUT_STARBSECTOREDGE_DEG = GSF.C_DOUBLE.withName("starbSectorEdge_deg"),
        LAYOUT_PORTMEANCOV_DEG = GSF.C_DOUBLE.withName("portMeanCov_deg"),
        LAYOUT_STARBMEANCOV_DEG = GSF.C_DOUBLE.withName("starbMeanCov_deg"),
        LAYOUT_PORTMEANCOV_M = GSF.C_DOUBLE.withName("portMeanCov_m"),
        LAYOUT_STARBMEANCOV_M = GSF.C_DOUBLE.withName("starbMeanCov_m"),
        LAYOUT_MODEANDSTABILISATION = GSF.C_INT.withName("modeAndStabilisation"),
        LAYOUT_RUNTIMEFILTER1 = GSF.C_INT.withName("runtimeFilter1"),
        LAYOUT_RUNTIMEFILTER2 = GSF.C_INT.withName("runtimeFilter2"),
        LAYOUT_PIPETRACKINGSTATUS = GSF.C_INT.withName("pipeTrackingStatus"),
        LAYOUT_TRANSMITARRAYSIZEUSED_DEG = GSF.C_DOUBLE.withName("transmitArraySizeUsed_deg"),
        LAYOUT_RECEIVEARRAYSIZEUSED_DEG = GSF.C_DOUBLE.withName("receiveArraySizeUsed_deg"),
        LAYOUT_TRANSMITPOWER_DB = GSF.C_DOUBLE.withName("transmitPower_dB"),
        LAYOUT_SLRAMPUPTIMEREMAINING = GSF.C_INT.withName("SLrampUpTimeRemaining"),
        MemoryLayout.paddingLayout(4),
        LAYOUT_YAWANGLE_DEG = GSF.C_DOUBLE.withName("yawAngle_deg"),
        LAYOUT_NUMTXSECTORS = GSF.C_INT.withName("numTxSectors"),
        LAYOUT_NUMBYTESPERTXSECTOR = GSF.C_INT.withName("numBytesPerTxSector"),
        LAYOUT_HEADINGVESSEL_DEG = GSF.C_DOUBLE.withName("headingVessel_deg"),
        LAYOUT_SOUNDSPEEDATTXDEPTH_MPERSEC = GSF.C_DOUBLE.withName("soundSpeedAtTxDepth_mPerSec"),
        LAYOUT_TXTRANSDUCERDEPTH_M = GSF.C_DOUBLE.withName("txTransducerDepth_m"),
        LAYOUT_Z_WATERLEVELREREFPOINT_M = GSF.C_DOUBLE.withName("z_waterLevelReRefPoint_m"),
        LAYOUT_X_KMALLTOALL_M = GSF.C_DOUBLE.withName("x_kmallToall_m"),
        LAYOUT_Y_KMALLTOALL_M = GSF.C_DOUBLE.withName("y_kmallToall_m"),
        LAYOUT_LATLONGINFO = GSF.C_INT.withName("latLongInfo"),
        LAYOUT_POSSENSORSTATUS = GSF.C_INT.withName("posSensorStatus"),
        LAYOUT_ATTITUDESENSORSTATUS = GSF.C_INT.withName("attitudeSensorStatus"),
        MemoryLayout.paddingLayout(4),
        LAYOUT_LATITUDE_DEG = GSF.C_DOUBLE.withName("latitude_deg"),
        LAYOUT_LONGITUDE_DEG = GSF.C_DOUBLE.withName("longitude_deg"),
        LAYOUT_ELLIPSOIDHEIGHTREREFPOINT_M = GSF.C_DOUBLE.withName("ellipsoidHeightReRefPoint_m"),
        LAYOUT_SPARE3 = MemoryLayout.sequenceLayout(32, GSF.C_CHAR).withName("spare3"),
        LAYOUT_SECTOR = MemoryLayout.sequenceLayout(9, KMALLTxSector.LAYOUT).withName("sector"),
        LAYOUT_NUMBYTESRXINFO = GSF.C_INT.withName("numBytesRxInfo"),
        LAYOUT_NUMSOUNDINGSMAXMAIN= GSF.C_INT.withName("numSoundingsMaxMain"),
        LAYOUT_NUMSOUNDINGSVALIDMAIN = GSF.C_INT.withName("numSoundingsValidMain"),
        LAYOUT_NUMBYTESPERSOUNDING = GSF.C_INT.withName("numBytesPerSounding"),
        LAYOUT_WCSAMPLERATE = GSF.C_DOUBLE.withName("WCSampleRate"),
        LAYOUT_SEABEDIMAGESAMPLERATE = GSF.C_DOUBLE.withName("seabedImageSampleRate"),
        LAYOUT_BSNORMAL_DB = GSF.C_DOUBLE.withName("BSnormal_dB"),
        LAYOUT_BSOBLIQUE_DB = GSF.C_DOUBLE.withName("BSoblique_dB"),
        LAYOUT_EXTRADETECTIONALARMFLAG = GSF.C_INT.withName("extraDetectionAlarmFlag"),
        LAYOUT_NUMEXTRADETECTIONS = GSF.C_INT.withName("numExtraDetections"),
        LAYOUT_NUMEXTRADETECTIONCLASSES = GSF.C_INT.withName("numExtraDetectionClasses"),
        LAYOUT_NUMBYTESPERCLASS = GSF.C_INT.withName("numBytesPerClass"),
        LAYOUT_SPARE4 = MemoryLayout.sequenceLayout(32, GSF.C_CHAR).withName("spare4"),
        LAYOUT_EXTRADETCLASSINFO = MemoryLayout.sequenceLayout(11, KMALLExtraDet.LAYOUT).withName("extraDetClassInfo"),
        LAYOUT_SPARE5 = MemoryLayout.sequenceLayout(32, GSF.C_CHAR).withName("spare5")
    ).withName("t_gsfKMALLSpecific");


    public KMALLSpecific(MemorySegment struct) {
        super(struct);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int gsfKMALLVersion
     * }
     */
    public int getGsfKMALLVersion() {
        return struct.get(LAYOUT_GSFKMALLVERSION, 0);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int dgmType
     * }
     */
    public int getDgmType() {
        return struct.get(LAYOUT_DGMTYPE, 4);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int dgmVersion
     * }
     */
    public int getDdgmVersion() {
        return struct.get(LAYOUT_DGMVERSION, 8);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int systemID
     * }
     */
    public int getSystemID() {
        return struct.get(LAYOUT_SYSTEMID, 12);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int echoSounderID
     * }
     */
    public int getEchoSounderID() {
        return struct.get(LAYOUT_ECHOSOUNDERID, 16);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare1[8]
     * }
     */
    public MemorySegment getSpare1() {
        return struct.asSlice(20, LAYOUT_SPARE1.byteSize());
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int numBytesCmnPart
     * }
     */
    public int getNumBytesCmnPart() {
        return struct.get(LAYOUT_NUMBYTESCMNPART, 28);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int pingCnt
     * }
     */
    public int getPingCnt() {
        return struct.get(LAYOUT_PINGCNT, 32);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int rxFansPerPing
     * }
     */
    public int getRxFansPerPing() {
        return struct.get(LAYOUT_RXFANSPERPING, 36);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int rxFanIndex
     * }
     */
    public int getRxFanIndex() {
        return struct.get(LAYOUT_RXFANINDEX, 40);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int swathsPerPing
     * }
     */
    public int getSwathsPerPing() {
        return struct.get(LAYOUT_SWATHSPERPING, 44);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int swathAlongPosition
     * }
     */
    public int getSwathAlongPosition() {
        return struct.get(LAYOUT_SWATHALONGPOSITION, 48);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int txTransducerInd
     * }
     */
    public int getTxTransducerInd() {
        return struct.get(LAYOUT_TXTRANSDUCERIND, 52);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int rxTransducerInd
     * }
     */
    public int getRxTransducerInd() {
        return struct.get(LAYOUT_RXTRANSDUCERIND, 56);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int numRxTransducers
     * }
     */
    public int getNumRxTransducers() {
        return struct.get(LAYOUT_NUMRXTRANSDUCERS, 60);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int algorithmType
     * }
     */
    public int getAlgorithmType() {
        return struct.get(LAYOUT_ALGORITHMTYPE, 64);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare2[16]
     * }
     */
    public MemorySegment getSpare2() {
        return struct.asSlice(68, LAYOUT_SPARE2.byteSize());
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int numBytesInfoData
     * }
     */
    public int getNumBytesInfoData() {
        return struct.get(LAYOUT_NUMBYTESINFODATA, 84);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double pingRate_Hz
     * }
     */
    public double getPingRate_Hz() {
        return struct.get(LAYOUT_PINGRATE_HZ, 88);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int beamSpacing
     * }
     */
    public int getBeamSpacing() {
        return struct.get(LAYOUT_BEAMSPACING, 96);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int depthMode
     * }
     */
    public int getDepthMode() {
        return struct.get(LAYOUT_DEPTHMODE, 100);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int subDepthMode
     * }
     */
    public int getSubDepthMode() {
        return struct.get(LAYOUT_SUBDEPTHMODE, 104);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int distanceBtwSwath
     * }
     */
    public int getDistanceBtwSwath() {
        return struct.get(LAYOUT_DISTANCEBTWSWATH, 108);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int detectionMode
     * }
     */
    public int getDetectionMode() {
        return struct.get(LAYOUT_DETECTIONMODE, 112);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int pulseForm
     * }
     */
    public int getPulseForm() {
        return struct.get(LAYOUT_PULSEFORM, 116);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double frequencyMode_Hz
     * }
     */
    public double getFrequencyMode_Hz() {
        return struct.get(LAYOUT_FREQUENCYMODE_HZ, 120);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double freqRangeLowLim_Hz
     * }
     */
    public double getFreqRangeLowLim_Hz() {
        return struct.get(LAYOUT_FREQRANGELOWLIM_HZ, 128);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double freqRangeHighLim_Hz
     * }
     */
    public double getFreqRangeHighLim_Hz() {
        return struct.get(LAYOUT_FREQRANGEHIGHLIM_HZ, 136);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double maxTotalTxPulseLength_sec
     * }
     */
    public double getMaxTotalTxPulseLength_sec() {
        return struct.get(LAYOUT_MAXTOTALTXPULSELENGTH_SEC, 144);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double maxEffTxPulseLength_sec
     * }
     */
    public double getMaxEffTxPulseLength_sec() {
        return struct.get(LAYOUT_MAXEFFTXPULSELENGTH_SEC, 152);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double maxEffTxBandWidth_Hz
     * }
     */
    public double getMaxEffTxBandWidth_Hz() {
        return struct.get(LAYOUT_MAXEFFTXBANDWIDTH_HZ, 160);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double absCoeff_dBPerkm
     * }
     */
    public double getAbsCoeff_dBPerkm() {
        return struct.get(LAYOUT_ABSCOEFF_DBPERKM, 168);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double portSectorEdge_deg
     * }
     */
    public double getPortSectorEdge_deg() {
        return struct.get(LAYOUT_PORTSECTOREDGE_DEG, 176);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double starbSectorEdge_deg
     * }
     */
    public double getStarbSectorEdge_deg() {
        return struct.get(LAYOUT_STARBSECTOREDGE_DEG, 184);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double portMeanCov_deg
     * }
     */
    public double getPortMeanCov_deg() {
        return struct.get(LAYOUT_PORTMEANCOV_DEG, 192);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double starbMeanCov_deg
     * }
     */
    public double getStarbMeanCov_deg() {
        return struct.get(LAYOUT_STARBMEANCOV_DEG, 200);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double portMeanCov_m
     * }
     */
    public double getPortMeanCov_m() {
        return struct.get(LAYOUT_PORTMEANCOV_M, 208);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double starbMeanCov_m
     * }
     */
    public double getStarbMeanCov_m() {
        return struct.get(LAYOUT_STARBMEANCOV_M, 216);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int modeAndStabilisation
     * }
     */
    public int getModeAndStabilisation() {
        return struct.get(LAYOUT_MODEANDSTABILISATION, 224);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int runtimeFilter1
     * }
     */
    public int getRuntimeFilter1() {
        return struct.get(LAYOUT_RUNTIMEFILTER1, 228);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int runtimeFilter2
     * }
     */
    public int getRuntimeFilter2() {
        return struct.get(LAYOUT_RUNTIMEFILTER2, 232);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int pipeTrackingStatus
     * }
     */
    public int getPipeTrackingStatus() {
        return struct.get(LAYOUT_PIPETRACKINGSTATUS, 236);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double transmitArraySizeUsed_deg
     * }
     */
    public double getTransmitArraySizeUsed_deg() {
        return struct.get(LAYOUT_TRANSMITARRAYSIZEUSED_DEG, 240);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double receiveArraySizeUsed_deg
     * }
     */
    public double getReceiveArraySizeUsed_deg() {
        return struct.get(LAYOUT_RECEIVEARRAYSIZEUSED_DEG, 248);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double transmitPower_dB
     * }
     */
    public double getTransmitPower_dB() {
        return struct.get(LAYOUT_TRANSMITPOWER_DB, 256);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int SLrampUpTimeRemaining
     * }
     */
    public int getSLrampUpTimeRemaining() {
        return struct.get(LAYOUT_SLRAMPUPTIMEREMAINING, 264);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double yawAngle_deg
     * }
     */
    public double getYawAngle_deg() {
        return struct.get(LAYOUT_YAWANGLE_DEG, 272);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int numTxSectors
     * }
     */
    public int getNumTxSectors() {
        return struct.get(LAYOUT_NUMTXSECTORS, 280);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int numBytesPerTxSector
     * }
     */
    public int getNumBytesPerTxSector() {
        return struct.get(LAYOUT_NUMBYTESPERTXSECTOR, 284);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double headingVessel_deg
     * }
     */
    public double getHeadingVessel_deg() {
        return struct.get(LAYOUT_HEADINGVESSEL_DEG, 288);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double soundSpeedAtTxDepth_mPerSec
     * }
     */
    public double getSoundSpeedAtTxDepth_mPerSec() {
        return struct.get(LAYOUT_SOUNDSPEEDATTXDEPTH_MPERSEC, 296);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double txTransducerDepth_m
     * }
     */
    public double getTxTransducerDepth_m() {
        return struct.get(LAYOUT_TXTRANSDUCERDEPTH_M, 304);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double z_waterLevelReRefPoint_m
     * }
     */
    public double getZwaterLevelReRefPoint_m() {
        return struct.get(LAYOUT_Z_WATERLEVELREREFPOINT_M, 312);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double x_kmallToall_m
     * }
     */
    public double getXkmallToall_m() {
        return struct.get(LAYOUT_X_KMALLTOALL_M, 320);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double y_kmallToall_m
     * }
     */
    public double getYkmallToall_m() {
        return struct.get(LAYOUT_Y_KMALLTOALL_M, 328);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int latLongInfo
     * }
     */
    public int getLatLongInfo() {
        return struct.get(LAYOUT_LATLONGINFO, 336);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int posSensorStatus
     * }
     */
    public int getPosSensorStatus() {
        return struct.get(LAYOUT_POSSENSORSTATUS, 340);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int attitudeSensorStatus
     * }
     */
    public int getAttitudeSensorStatus() {
        return struct.get(LAYOUT_ATTITUDESENSORSTATUS, 344);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double latitude_deg
     * }
     */
    public double getLatitudeDeg() {
        return struct.get(LAYOUT_LATITUDE_DEG, 352);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double longitude_deg
     * }
     */
    public double getLongitudeDeg() {
        return struct.get(LAYOUT_LONGITUDE_DEG, 360);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double ellipsoidHeightReRefPoint_m
     * }
     */
    public double getEllipsoidHeightReRefPoint_m() {
        return struct.get(LAYOUT_ELLIPSOIDHEIGHTREREFPOINT_M, 368);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare3[32]
     * }
     */
    public String getSpare3() {
        return new String(getBytes(376, 32));
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * t_gsfKMALLTxSector sector[9]
     * }
     */
    public MemorySegment getSector() {
        return struct.asSlice(408, LAYOUT_SECTOR.byteSize());
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int numBytesRxInfo
     * }
     */
    public int getNumBytesRxInfo() {
        return struct.get(LAYOUT_NUMBYTESRXINFO, 1344);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int numSoundingsMaxMain
     * }
     */
    public int getNumSoundingsMaxMain() {
        return struct.get(LAYOUT_NUMSOUNDINGSMAXMAIN, 1348);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int numSoundingsValidMain
     * }
     */
    public int getNumSoundingsValidMain() {
        return struct.get(LAYOUT_NUMSOUNDINGSVALIDMAIN, 1352);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int numBytesPerSounding
     * }
     */
    public int getNumBytesPerSounding() {
        return struct.get(LAYOUT_NUMBYTESPERSOUNDING, 1356);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double WCSampleRate
     * }
     */
    public double getWCSampleRate() {
        return struct.get(LAYOUT_WCSAMPLERATE, 1360);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double seabedImageSampleRate
     * }
     */
    public double getSeabedImageSampleRate() {
        return struct.get(LAYOUT_SEABEDIMAGESAMPLERATE, 1368);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double BSnormal_dB
     * }
     */
    public double getBSnormal_dB() {
        return struct.get(LAYOUT_BSNORMAL_DB, 1376);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double BSoblique_dB
     * }
     */
    public double getBSoblique_dB() {
        return struct.get(LAYOUT_BSOBLIQUE_DB, 1384);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int extraDetectionAlarmFlag
     * }
     */
    public int getExtraDetectionAlarmFlag() {
        return struct.get(LAYOUT_EXTRADETECTIONALARMFLAG, 1392);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int numExtraDetections
     * }
     */
    public int getNumExtraDetections() {
        return struct.get(LAYOUT_NUMEXTRADETECTIONS, 1396);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int numExtraDetectionClasses
     * }
     */
    public int getNumExtraDetectionClasses() {
        return struct.get(LAYOUT_NUMEXTRADETECTIONCLASSES, 1400);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int numBytesPerClass
     * }
     */
    public int getNumBytesPerClass() {
        return struct.get(LAYOUT_NUMBYTESPERCLASS, 1404);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare4[32]
     * }
     */
    public String getSpare4() {
        return new String(getBytes(1408, 32));
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * t_gsfKMALLExtraDet extraDetClassInfo[11]
     * }
     */
    public MemorySegment getExtraDetClassInfo() {
        return struct.asSlice(1440, LAYOUT_EXTRADETCLASSINFO.byteSize());
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare5[32]
     * }
     */
    public String getSpare5() {
        return new String(getBytes(1880, 32));
    }
}
