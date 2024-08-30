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
import org.apache.sis.storage.gsf.TimeSpec;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DeltaTSpecific extends StructClass {

    private static final SequenceLayout LAYOUT_DECODE_FILE_TYPE;
    private static final OfByte LAYOUT_VERSION;
    private static final OfInt LAYOUT_PING_BYTE_SIZE;
    private static final GroupLayout LAYOUT_INTERROGATION_TIME;
    private static final OfInt LAYOUT_SAMPLES_PER_BEAM;
    private static final OfDouble LAYOUT_SECTOR_SIZE;
    private static final OfDouble LAYOUT_START_ANGLE;
    private static final OfDouble LAYOUT_ANGLE_INCREMENT;
    private static final OfInt LAYOUT_ACOUSTIC_RANGE;
    private static final OfInt LAYOUT_ACOUSTIC_FREQUENCY;
    private static final OfDouble LAYOUT_SOUND_VELOCITY;
    private static final OfDouble LAYOUT_RANGE_RESOLUTION;
    private static final OfDouble LAYOUT_PROFILE_TILT_ANGLE;
    private static final OfDouble LAYOUT_REPETITION_RATE;
    private static final OfLong LAYOUT_PING_NUMBER;
    private static final OfByte LAYOUT_INTENSITY_FLAG;
    private static final OfDouble LAYOUT_PING_LATENCY;
    private static final OfDouble LAYOUT_DATA_LATENCY;
    private static final OfByte LAYOUT_SAMPLE_RATE_FLAG;
    private static final OfByte LAYOUT_OPTION_FLAGS;
    private static final OfInt LAYOUT_NUM_PINGS_AVG;
    private static final OfDouble LAYOUT_CENTER_PING_TIME_OFFSET;
    private static final OfByte LAYOUT_USER_DEFINED_BYTE;
    private static final OfDouble LAYOUT_ALTITUDE;
    private static final OfByte LAYOUT_EXTERNAL_SENSOR_FLAGS;
    private static final OfDouble LAYOUT_PULSE_LENGTH;
    private static final OfDouble LAYOUT_FORE_AFT_BEAMWIDTH;
    private static final OfDouble LAYOUT_ATHWARTSHIPS_BEAMWIDTH;
    private static final SequenceLayout LAYOUT_SPARSE;

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        LAYOUT_DECODE_FILE_TYPE = MemoryLayout.sequenceLayout(4, GSF.C_CHAR).withName("decode_file_type"),
        LAYOUT_VERSION = GSF.C_CHAR.withName("version"),
        MemoryLayout.paddingLayout(3),
        LAYOUT_PING_BYTE_SIZE = GSF.C_INT.withName("ping_byte_size"),
        MemoryLayout.paddingLayout(4),
        LAYOUT_INTERROGATION_TIME = TimeSpec.LAYOUT.withName("interrogation_time"),
        LAYOUT_SAMPLES_PER_BEAM = GSF.C_INT.withName("samples_per_beam"),
        MemoryLayout.paddingLayout(4),
        LAYOUT_SECTOR_SIZE = GSF.C_DOUBLE.withName("sector_size"),
        LAYOUT_START_ANGLE = GSF.C_DOUBLE.withName("start_angle"),
        LAYOUT_ANGLE_INCREMENT = GSF.C_DOUBLE.withName("angle_increment"),
        LAYOUT_ACOUSTIC_RANGE = GSF.C_INT.withName("acoustic_range"),
        LAYOUT_ACOUSTIC_FREQUENCY = GSF.C_INT.withName("acoustic_frequency"),
        LAYOUT_SOUND_VELOCITY = GSF.C_DOUBLE.withName("sound_velocity"),
        LAYOUT_RANGE_RESOLUTION = GSF.C_DOUBLE.withName("range_resolution"),
        LAYOUT_PROFILE_TILT_ANGLE = GSF.C_DOUBLE.withName("profile_tilt_angle"),
        LAYOUT_REPETITION_RATE = GSF.C_DOUBLE.withName("repetition_rate"),
        LAYOUT_PING_NUMBER = GSF.C_LONG.withName("ping_number"),
        LAYOUT_INTENSITY_FLAG = GSF.C_CHAR.withName("intensity_flag"),
        MemoryLayout.paddingLayout(7),
        LAYOUT_PING_LATENCY = GSF.C_DOUBLE.withName("ping_latency"),
        LAYOUT_DATA_LATENCY = GSF.C_DOUBLE.withName("data_latency"),
        LAYOUT_SAMPLE_RATE_FLAG = GSF.C_CHAR.withName("sample_rate_flag"),
        LAYOUT_OPTION_FLAGS = GSF.C_CHAR.withName("option_flags"),
        MemoryLayout.paddingLayout(2),
        LAYOUT_NUM_PINGS_AVG = GSF.C_INT.withName("num_pings_avg"),
        LAYOUT_CENTER_PING_TIME_OFFSET = GSF.C_DOUBLE.withName("center_ping_time_offset"),
        LAYOUT_USER_DEFINED_BYTE = GSF.C_CHAR.withName("user_defined_byte"),
        MemoryLayout.paddingLayout(7),
        LAYOUT_ALTITUDE = GSF.C_DOUBLE.withName("altitude"),
        LAYOUT_EXTERNAL_SENSOR_FLAGS = GSF.C_CHAR.withName("external_sensor_flags"),
        MemoryLayout.paddingLayout(7),
        LAYOUT_PULSE_LENGTH = GSF.C_DOUBLE.withName("pulse_length"),
        LAYOUT_FORE_AFT_BEAMWIDTH = GSF.C_DOUBLE.withName("fore_aft_beamwidth"),
        LAYOUT_ATHWARTSHIPS_BEAMWIDTH = GSF.C_DOUBLE.withName("athwartships_beamwidth"),
        LAYOUT_SPARSE = MemoryLayout.sequenceLayout(32, GSF.C_CHAR).withName("spare")
    ).withName("t_gsfDeltaTSpecific");

    public DeltaTSpecific(MemorySegment struct) {
        super(struct);
    }

    public DeltaTSpecific(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char decode_file_type[4]
     * }
     */
    public String getDecodeFileType() {
        return new String(getBytes(0, 4));
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char version
     * }
     */
    public byte getVersion() {
        return struct.get(LAYOUT_VERSION, 4);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ping_byte_size
     * }
     */
    public int ping_byte_size() {
        return struct.get(LAYOUT_PING_BYTE_SIZE, 8);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * struct timespec interrogation_time
     * }
     */
    public MemorySegment getInterrogationTime() {
        return struct.asSlice(16, LAYOUT_INTERROGATION_TIME.byteSize());
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int samples_per_beam
     * }
     */
    public int getSamplesPerBeam() {
        return struct.get(LAYOUT_SAMPLES_PER_BEAM, 32);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double sector_size
     * }
     */
    public double getSectorSize() {
        return struct.get(LAYOUT_SECTOR_SIZE, 40);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double start_angle
     * }
     */
    public double getStartAngle() {
        return struct.get(LAYOUT_START_ANGLE, 48);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double angle_increment
     * }
     */
    public double getAngleIncrement() {
        return struct.get(LAYOUT_ANGLE_INCREMENT, 56);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int acoustic_range
     * }
     */
    public int getAcousticRange() {
        return struct.get(LAYOUT_ACOUSTIC_RANGE, 64);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int acoustic_frequency
     * }
     */
    public int getAcousticFrequency() {
        return struct.get(LAYOUT_ACOUSTIC_FREQUENCY, 68);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double sound_velocity
     * }
     */
    public double getSoundVelocity() {
        return struct.get(LAYOUT_SOUND_VELOCITY, 72);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double range_resolution
     * }
     */
    public double getRangeResolution() {
        return struct.get(LAYOUT_RANGE_RESOLUTION, 80);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double profile_tilt_angle
     * }
     */
    public double getProfileTiltAngle() {
        return struct.get(LAYOUT_PROFILE_TILT_ANGLE, 88);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double repetition_rate
     * }
     */
    public double getRepetitionRate() {
        return struct.get(LAYOUT_REPETITION_RATE, 96);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned long ping_number
     * }
     */
    public long getPingNumber() {
        return struct.get(LAYOUT_PING_NUMBER, 104);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char intensity_flag
     * }
     */
    public byte getIntensityFlag() {
        return struct.get(LAYOUT_INTENSITY_FLAG, 112);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double ping_latency
     * }
     */
    public double getPingLatency() {
        return struct.get(LAYOUT_PING_LATENCY, 120);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double data_latency
     * }
     */
    public double getDataLatency() {
        return struct.get(LAYOUT_DATA_LATENCY, 128);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char sample_rate_flag
     * }
     */
    public byte getSampleRateFlag() {
        return struct.get(LAYOUT_SAMPLE_RATE_FLAG, 136);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char option_flags
     * }
     */
    public byte getOptionFlags() {
        return struct.get(LAYOUT_OPTION_FLAGS, 137);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int num_pings_avg
     * }
     */
    public int getNumPingsAvg() {
        return struct.get(LAYOUT_NUM_PINGS_AVG, 140);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double center_ping_time_offset
     * }
     */
    public double getCenterPingTimeOffset() {
        return struct.get(LAYOUT_CENTER_PING_TIME_OFFSET, 144);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char user_defined_byte
     * }
     */
    public byte getUserDefinedByte() {
        return struct.get(LAYOUT_USER_DEFINED_BYTE, 152);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double altitude
     * }
     */
    public double getAltitude() {
        return struct.get(LAYOUT_ALTITUDE, 160);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char external_sensor_flags
     * }
     */
    public byte getExternalSensorFlags() {
        return struct.get(LAYOUT_EXTERNAL_SENSOR_FLAGS, 168);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double pulse_length
     * }
     */
    public double getPulseLength() {
        return struct.get(LAYOUT_PULSE_LENGTH, 176);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double fore_aft_beamwidth
     * }
     */
    public double getForeAftBeamwidth() {
        return struct.get(LAYOUT_FORE_AFT_BEAMWIDTH, 184);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double athwartships_beamwidth
     * }
     */
    public double getAthwartshipsBeamwidth() {
        return struct.get(LAYOUT_ATHWARTSHIPS_BEAMWIDTH, 192);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare[32]
     * }
     */
    public String getSpare() {
        return new String(getBytes(200, 32));
    }

}

