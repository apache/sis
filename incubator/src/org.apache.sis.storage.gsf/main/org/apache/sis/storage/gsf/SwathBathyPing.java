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
package org.apache.sis.storage.gsf;

import java.lang.foreign.*;

import static java.lang.foreign.ValueLayout.*;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SwathBathyPing extends StructClass {

    private static final GroupLayout LAYOUT_PING_TIME;
    private static final OfDouble LAYOUT_LATITUDE;
    private static final OfDouble LAYOUT_LONGITUDE;
    private static final OfDouble LAYOUT_HEIGHT;
    private static final OfDouble LAYOUT_SEP;
    private static final OfShort LAYOUT_NUMBER_BEAMS;
    private static final OfShort LAYOUT_CENTER_BEAM;
    private static final OfShort LAYOUT_PING_FLAGS;
    private static final OfShort LAYOUT_RESERVED;
    private static final OfDouble LAYOUT_TIDE_CORRECTOR;
    private static final OfDouble LAYOUT_GPS_TIDE_CORRECTOR;
    private static final OfDouble LAYOUT_DEPTH_CORRECTOR;
    private static final OfDouble LAYOUT_HEADING;
    private static final OfDouble LAYOUT_PITCH;
    private static final OfDouble LAYOUT_ROLL;
    private static final OfDouble LAYOUT_HEAVE;
    private static final OfDouble LAYOUT_COURSE;
    private static final OfDouble LAYOUT_SPEED;
    private static final GroupLayout LAYOUT_SCALEFACTORS;
    private static final AddressLayout LAYOUT_DEPTH;
    private static final AddressLayout LAYOUT_NOMINAL_DEPTH;
    private static final AddressLayout LAYOUT_ACROSS_TRACK;
    private static final AddressLayout LAYOUT_ALONG_TRACK;
    private static final AddressLayout LAYOUT_TRAVEL_TIME;
    private static final AddressLayout LAYOUT_BEAM_ANGLE;
    private static final AddressLayout LAYOUT_MC_AMPLITUDE;
    private static final AddressLayout LAYOUT_MR_AMPLITUDE;
    private static final AddressLayout LAYOUT_ECHO_WIDTH;
    private static final AddressLayout LAYOUT_QUALITY_FACTOR;
    private static final AddressLayout LAYOUT_RECEIVE_HEAVE;
    private static final AddressLayout LAYOUT_DEPTH_ERROR;
    private static final AddressLayout LAYOUT_ACROSS_TRACK_ERROR;
    private static final AddressLayout LAYOUT_ALONG_TRACK_ERROR;
    private static final AddressLayout LAYOUT_QUALITY_FLAGS;
    private static final AddressLayout LAYOUT_BEAM_FLAGS;
    private static final AddressLayout LAYOUT_SIGNAL_TO_NOISE;
    private static final AddressLayout LAYOUT_BEAM_ANGLE_FORWARD;
    private static final AddressLayout LAYOUT_VERTICAL_ERROR;
    private static final AddressLayout LAYOUT_HORIZONTAL_ERROR;
    private static final AddressLayout LAYOUT_SECTOR_NUMBER;
    private static final AddressLayout LAYOUT_DETECTION_INFO;
    private static final AddressLayout LAYOUT_INCIDENT_BEAM_ADJ;
    private static final AddressLayout LAYOUT_SYSTEM_CLEANING;
    private static final AddressLayout LAYOUT_DOPPLER_CORR;
    private static final AddressLayout LAYOUT_SONAR_VERT_UNCERT;
    private static final AddressLayout LAYOUT_SONAR_HORZ_UNCERT;
    private static final AddressLayout LAYOUT_DETECTION_WINDOW;
    private static final AddressLayout LAYOUT_MEAN_ABS_COEFF;
    private static final OfInt LAYOUT_SENSOR_ID;
    private static final GroupLayout LAYOUT_SENSOR_DATA;
    private static final AddressLayout LAYOUT_BRB_INTEN;
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        LAYOUT_PING_TIME = TimeSpec.LAYOUT.withName("ping_time"),
        LAYOUT_LATITUDE = GSF.C_DOUBLE.withName("latitude"),
        LAYOUT_LONGITUDE = GSF.C_DOUBLE.withName("longitude"),
        LAYOUT_HEIGHT = GSF.C_DOUBLE.withName("height"),
        LAYOUT_SEP = GSF.C_DOUBLE.withName("sep"),
        LAYOUT_NUMBER_BEAMS = GSF.C_SHORT.withName("number_beams"),
        LAYOUT_CENTER_BEAM = GSF.C_SHORT.withName("center_beam"),
        LAYOUT_PING_FLAGS = GSF.C_SHORT.withName("ping_flags"),
        LAYOUT_RESERVED = GSF.C_SHORT.withName("reserved"),
        LAYOUT_TIDE_CORRECTOR = GSF.C_DOUBLE.withName("tide_corrector"),
        LAYOUT_GPS_TIDE_CORRECTOR = GSF.C_DOUBLE.withName("gps_tide_corrector"),
        LAYOUT_DEPTH_CORRECTOR = GSF.C_DOUBLE.withName("depth_corrector"),
        LAYOUT_HEADING = GSF.C_DOUBLE.withName("heading"),
        LAYOUT_PITCH = GSF.C_DOUBLE.withName("pitch"),
        LAYOUT_ROLL = GSF.C_DOUBLE.withName("roll"),
        LAYOUT_HEAVE = GSF.C_DOUBLE.withName("heave"),
        LAYOUT_COURSE = GSF.C_DOUBLE.withName("course"),
        LAYOUT_SPEED = GSF.C_DOUBLE.withName("speed"),
        LAYOUT_SCALEFACTORS = ScaleFactors.LAYOUT.withName("scaleFactors"),
        LAYOUT_DEPTH = GSF.C_POINTER.withName("depth"),
        LAYOUT_NOMINAL_DEPTH = GSF.C_POINTER.withName("nominal_depth"),
        LAYOUT_ACROSS_TRACK = GSF.C_POINTER.withName("across_track"),
        LAYOUT_ALONG_TRACK = GSF.C_POINTER.withName("along_track"),
        LAYOUT_TRAVEL_TIME = GSF.C_POINTER.withName("travel_time"),
        LAYOUT_BEAM_ANGLE = GSF.C_POINTER.withName("beam_angle"),
        LAYOUT_MC_AMPLITUDE = GSF.C_POINTER.withName("mc_amplitude"),
        LAYOUT_MR_AMPLITUDE = GSF.C_POINTER.withName("mr_amplitude"),
        LAYOUT_ECHO_WIDTH = GSF.C_POINTER.withName("echo_width"),
        LAYOUT_QUALITY_FACTOR = GSF.C_POINTER.withName("quality_factor"),
        LAYOUT_RECEIVE_HEAVE = GSF.C_POINTER.withName("receive_heave"),
        LAYOUT_DEPTH_ERROR = GSF.C_POINTER.withName("depth_error"),
        LAYOUT_ACROSS_TRACK_ERROR = GSF.C_POINTER.withName("across_track_error"),
        LAYOUT_ALONG_TRACK_ERROR = GSF.C_POINTER.withName("along_track_error"),
        LAYOUT_QUALITY_FLAGS = GSF.C_POINTER.withName("quality_flags"),
        LAYOUT_BEAM_FLAGS = GSF.C_POINTER.withName("beam_flags"),
        LAYOUT_SIGNAL_TO_NOISE = GSF.C_POINTER.withName("signal_to_noise"),
        LAYOUT_BEAM_ANGLE_FORWARD = GSF.C_POINTER.withName("beam_angle_forward"),
        LAYOUT_VERTICAL_ERROR = GSF.C_POINTER.withName("vertical_error"),
        LAYOUT_HORIZONTAL_ERROR = GSF.C_POINTER.withName("horizontal_error"),
        LAYOUT_SECTOR_NUMBER = GSF.C_POINTER.withName("sector_number"),
        LAYOUT_DETECTION_INFO = GSF.C_POINTER.withName("detection_info"),
        LAYOUT_INCIDENT_BEAM_ADJ = GSF.C_POINTER.withName("incident_beam_adj"),
        LAYOUT_SYSTEM_CLEANING = GSF.C_POINTER.withName("system_cleaning"),
        LAYOUT_DOPPLER_CORR = GSF.C_POINTER.withName("doppler_corr"),
        LAYOUT_SONAR_VERT_UNCERT = GSF.C_POINTER.withName("sonar_vert_uncert"),
        LAYOUT_SONAR_HORZ_UNCERT = GSF.C_POINTER.withName("sonar_horz_uncert"),
        LAYOUT_DETECTION_WINDOW = GSF.C_POINTER.withName("detection_window"),
        LAYOUT_MEAN_ABS_COEFF = GSF.C_POINTER.withName("mean_abs_coeff"),
        LAYOUT_SENSOR_ID = GSF.C_INT.withName("sensor_id"),
        MemoryLayout.paddingLayout(4),
        LAYOUT_SENSOR_DATA = SensorSpecific.LAYOUT.withName("sensor_data"),
        LAYOUT_BRB_INTEN = GSF.C_POINTER.withName("brb_inten")
    ).withName("t_gsfSwathBathyPing");

    public SwathBathyPing(MemorySegment struct) {
        super(struct);
    }

    public SwathBathyPing(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    public TimeSpec getPingTime() {
        return new TimeSpec(struct.asSlice(0, LAYOUT_PING_TIME.byteSize()));
    }

    public double getLatitude() {
        return struct.get(LAYOUT_LATITUDE, 16);
    }

    public double getLongitude() {
        return struct.get(LAYOUT_LONGITUDE, 24);
    }

    public double getHeight() {
        return struct.get(LAYOUT_HEIGHT, 32);
    }

    public double getSep() {
        return struct.get(LAYOUT_SEP, 40);
    }

    public short getNumberBeams() {
        return struct.get(LAYOUT_NUMBER_BEAMS, 48);
    }

    public short getCenterBeam() {
        return struct.get(LAYOUT_CENTER_BEAM, 50);
    }

    public short getPingFlags() {
        return struct.get(LAYOUT_PING_FLAGS, 52);
    }

    public short getReserved() {
        return struct.get(LAYOUT_RESERVED, 54);
    }

    public double getTideCorrector() {
        return struct.get(LAYOUT_TIDE_CORRECTOR, 56);
    }

    public double getGpsTildeCorrector() {
        return struct.get(LAYOUT_GPS_TIDE_CORRECTOR, 64);
    }

    public double getDepthCorrector() {
        return struct.get(LAYOUT_DEPTH_CORRECTOR, 72);
    }

    public double getHeading() {
        return struct.get(LAYOUT_HEADING, 80);
    }

    public double getPitch() {
        return struct.get(LAYOUT_PITCH, 88);
    }

    public double getRoll() {
        return struct.get(LAYOUT_ROLL, 96);
    }

    public double getHeave() {
        return struct.get(LAYOUT_HEAVE, 104);
    }

    public double getCourse() {
        return struct.get(LAYOUT_COURSE, 112);
    }

    public double getSpeed() {
        return struct.get(LAYOUT_SPEED, 120);
    }

    public ScaleFactors getScaleFactors() {
        return new ScaleFactors(struct.asSlice(128, LAYOUT_SCALEFACTORS.byteSize()));
    }

    public double[] getDepth() {
        return getDoubles(struct.get(LAYOUT_DEPTH, 856), 0, getNumberBeams());
    }

    public double[] getNominalDepth() {
        return getDoubles(struct.get(LAYOUT_NOMINAL_DEPTH, 864), 0, getNumberBeams());
    }

    public double[] getAcrossTrack() {
        return getDoubles(struct.get(LAYOUT_ACROSS_TRACK, 872), 0, getNumberBeams());
    }

    public double[] getAlongTrack() {
        return getDoubles(struct.get(LAYOUT_ALONG_TRACK, 880), 0, getNumberBeams());
    }

    public double[] getTravelTime() {
        return getDoubles(struct.get(LAYOUT_TRAVEL_TIME, 888), 0, getNumberBeams());
    }

    public double[] getBeamAngle() {
        return getDoubles(struct.get(LAYOUT_BEAM_ANGLE, 896), 0, getNumberBeams());
    }

    public double[] getMcAmplitude() {
        return getDoubles(struct.get(LAYOUT_MC_AMPLITUDE, 904), 0, getNumberBeams());
    }

    public double[] getMrAmplitude() {
        return getDoubles(struct.get(LAYOUT_MR_AMPLITUDE, 912), 0, getNumberBeams());
    }

    public double[] getEchoWidth() {
        return getDoubles(struct.get(LAYOUT_ECHO_WIDTH, 920), 0, getNumberBeams());
    }

    public double[] getQualityFactor() {
        return getDoubles(struct.get(LAYOUT_QUALITY_FACTOR, 928), 0, getNumberBeams());
    }

    public double[] getReceiveHeave() {
        return getDoubles(struct.get(LAYOUT_RECEIVE_HEAVE, 936), 0, getNumberBeams());
    }

    public double[] getDepthError() {
        return getDoubles(struct.get(LAYOUT_DEPTH_ERROR, 944), 0, getNumberBeams());
    }

    public double[] getAcrossTrackError() {
        return getDoubles(struct.get(LAYOUT_ACROSS_TRACK_ERROR, 952), 0, getNumberBeams());
    }

    public double[] getAlongTrackError() {
        return getDoubles(struct.get(LAYOUT_ALONG_TRACK_ERROR, 960), 0, getNumberBeams());
    }

    public double[] getQualityFlags() {
        return getDoubles(struct.get(LAYOUT_QUALITY_FLAGS, 968), 0, getNumberBeams());
    }

    public double[] getBeamFlags() {
        return getDoubles(struct.get(LAYOUT_BEAM_FLAGS, 976), 0, getNumberBeams());
    }

    public double[] getSignalToNoise() {
        return getDoubles(struct.get(LAYOUT_SIGNAL_TO_NOISE, 984), 0, getNumberBeams());
    }

    public double[] getBeamAngleForward() {
        return getDoubles(struct.get(LAYOUT_BEAM_ANGLE_FORWARD, 992), 0, getNumberBeams());
    }

    public double[] getVerticalError() {
        return getDoubles(struct.get(LAYOUT_VERTICAL_ERROR, 1000), 0, getNumberBeams());
    }

    public double[] getHorizontalError() {
        return getDoubles(struct.get(LAYOUT_HORIZONTAL_ERROR, 1008), 0, getNumberBeams());
    }

    public double[] getSectorNumber() {
        return getDoubles(struct.get(LAYOUT_SECTOR_NUMBER, 1016), 0, getNumberBeams());
    }

    public double[] getDetectionInfo() {
        return getDoubles(struct.get(LAYOUT_DETECTION_INFO, 1024), 0, getNumberBeams());
    }

    public double[] getIncidentBeamAdj() {
        return getDoubles(struct.get(LAYOUT_INCIDENT_BEAM_ADJ, 1032), 0, getNumberBeams());
    }

    public double[] getSystemCleaning() {
        return getDoubles(struct.get(LAYOUT_SYSTEM_CLEANING, 1040), 0, getNumberBeams());
    }

    public double[] getDopplerCorr() {
        return getDoubles(struct.get(LAYOUT_DOPPLER_CORR, 1048), 0, getNumberBeams());
    }

    public double[] getSonarVertUncert() {
        return getDoubles(struct.get(LAYOUT_SONAR_VERT_UNCERT, 1056), 0, getNumberBeams());
    }

    public double[] getSonarHorzUncert() {
        return getDoubles(struct.get(LAYOUT_SONAR_HORZ_UNCERT, 1064), 0, getNumberBeams());
    }

    public int[] getDetectionWindow() {
        return getInts(struct.get(LAYOUT_DETECTION_WINDOW, 1072), 0, getNumberBeams());
    }

    public double[] getMeanAbsCoeff() {
        return getDoubles(struct.get(LAYOUT_MEAN_ABS_COEFF, 1080), 0, getNumberBeams());
    }

    public int getSensorId() {
        return struct.get(LAYOUT_SENSOR_ID, 1088);
    }

    //causes JVM to crash
//    public SensorSpecific getSensorData() {
//        return new SensorSpecific(struct.asSlice(1096, sensor_dataLAYOUT.byteSize()));
//    }

    //causes JVM to crash
//    public MemorySegment getBrbInten() {
//        return struct.get(brb_intenLAYOUT, 3008);
//    }

}