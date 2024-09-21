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

import java.lang.foreign.AddressLayout;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.panama.LibraryLoader;
import org.apache.sis.storage.panama.LibraryStatus;
import org.apache.sis.storage.panama.NativeFunctions;
import org.apache.sis.util.logging.Logging;

/**
 * Binding for GSFLib version 3.09.
 * Version 3.10 introduce breaking changes.
 *
 * @author Johann Sorel (Geomatys)
 */
public class GSF extends NativeFunctions {

    public static final MemorySegment NULL = MemorySegment.ofAddress(0L);
    public static final int EOF = -1;

    public static final ValueLayout.OfBoolean C_BOOL = ValueLayout.JAVA_BOOLEAN;
    public static final ValueLayout.OfByte C_CHAR = ValueLayout.JAVA_BYTE;
    public static final ValueLayout.OfShort C_SHORT = ValueLayout.JAVA_SHORT;
    public static final ValueLayout.OfInt C_INT = ValueLayout.JAVA_INT;
    public static final ValueLayout.OfLong C_LONG_LONG = ValueLayout.JAVA_LONG;
    public static final ValueLayout.OfFloat C_FLOAT = ValueLayout.JAVA_FLOAT;
    public static final ValueLayout.OfDouble C_DOUBLE = ValueLayout.JAVA_DOUBLE;
    public static final AddressLayout C_POINTER = ValueLayout.ADDRESS.withTargetLayout(MemoryLayout.sequenceLayout(java.lang.Long.MAX_VALUE, ValueLayout.JAVA_BYTE));
    public static final ValueLayout.OfLong C_LONG = ValueLayout.JAVA_LONG;


    public static final int GSF_MAX_RECORD_SIZE = 524288;
    public static final int GSF_MAX_OPEN_FILES = 4;
    public static final int GSF_CREATE = 1;
    public static final int GSF_READONLY = 2;
    public static final int GSF_UPDATE = 3;
    public static final int GSF_READONLY_INDEX = 4;
    public static final int GSF_UPDATE_INDEX = 5;
    public static final int GSF_APPEND = 6;
    public static final int GSF_REWIND = 1;
    public static final int GSF_END_OF_FILE = 2;
    public static final int GSF_PREVIOUS_RECORD = 3;
    public static final int GSF_SHORT_SIZE = 2;
    public static final int GSF_LONG_SIZE = 4;
    public static final int GSF_NEXT_RECORD = 0;
    public static final int NUM_REC_TYPES = 13;
    public static final int GSF_MAX_PING_ARRAY_SUBRECORDS = 30;
    public static final int GSF_VERSION_SIZE = 12;
    public static final int GSF_SEABAT_WIDE_MODE = 1;
    public static final int GSF_SEABAT_9002 = 2;
    public static final int GSF_SEABAT_STBD_HEAD = 4;
    public static final int GSF_SEABAT_9003 = 8;
    public static final int GSF_8101_WIDE_MODE = 1;
    public static final int GSF_8101_TWO_HEADS = 2;
    public static final int GSF_8101_STBD_HEAD = 4;
    public static final int GSF_8101_AMPLITUDE = 8;
    public static final int GSF_7100_PITCH_STAB = 1;
    public static final int GSF_7100_ROLL_STAB = 1;
    public static final int GSF_2112_SVP_CORRECTION = 1;
    public static final int GSF_2112_LOW_FREQUENCY = 2;
    public static final int GSF_2112_AUTO_DEPTH_GATE = 4;
    public static final int GSF_2112_POOR_QUALITY = 1;
    public static final int GSF_2112_DATA_SOURCE_WMT = 16;
    public static final int GSF_MKII_LOW_FREQUENCY = 1;
    public static final int GSF_MKII_SOURCE_MODE = 2;
    public static final int GSF_MKII_SOURCE_POWER = 4;
    public static final int GSF_MKII_STBD_HEAD = 8;
    public static final int GSF_8100_WIDE_MODE = 1;
    public static final int GSF_8100_TWO_HEADS = 2;
    public static final int GSF_8100_STBD_HEAD = 4;
    public static final int GSF_8100_AMPLITUDE = 8;
    public static final int GSF_8100_PITCH_STAB = 16;
    public static final int GSF_8100_ROLL_STAB = 32;
    public static final int GSF_SB_MPP_SOURCE_UNKNOWN = 0;
    public static final int GSF_SB_MPP_SOURCE_GPS_3S = 1;
    public static final int GSF_SB_MPP_SOURCE_GPS_TASMAN = 2;
    public static final int GSF_SB_MPP_SOURCE_DGPS_TRIMBLE = 3;
    public static final int GSF_SB_MPP_SOURCE_DGPS_TASMAN = 4;
    public static final int GSF_SB_MPP_SOURCE_DGPS_MAG = 5;
    public static final int GSF_SB_MPP_SOURCE_RANGE_MFIX = 6;
    public static final int GSF_SB_MPP_SOURCE_RANGE_TRIS = 7;
    public static final int GSF_SB_MPP_SOURCE_RANGE_OTHER = 8;
    public static final int GSF_MAX_EM4_SECTORS = 9;
    public static final int GSF_MAX_EM3_SECTORS = 20;
    public static final int GSF_MAX_KMALL_SECTORS = 9;
    public static final int GSF_EM_WAVEFORM_CW = 0;
    public static final int GSF_EM_WAVEFORM_FM_UP = 1;
    public static final int GSF_EM_WAVEFORM_FM_DOWN = 2;
    public static final int GSF_EM_MODE_VERY_SHALLOW = 0;
    public static final int GSF_EM_MODE_SHALLOW = 1;
    public static final int GSF_EM_MODE_MEDIUM = 2;
    public static final int GSF_EM_MODE_DEEP = 3;
    public static final int GSF_EM_MODE_VERY_DEEP = 4;
    public static final int GSF_EM_MODE_EXTRA_DEEP = 5;
    public static final int GSF_EM_MODE_MASK = 7;
    public static final int GSF_EM_MODE_DS_OFF = 192;
    public static final int GSF_EM_MODE_DS_FIXED = 64;
    public static final int GSF_EM_MODE_DS_DYNAMIC = 128;
    public static final int GSF_EM_VALID_1_PPS = 1;
    public static final int GSF_EM_VALID_POSITION = 2;
    public static final int GSF_EM_VALID_ATTITUDE = 4;
    public static final int GSF_EM_VALID_CLOCK = 8;
    public static final int GSF_EM_VALID_HEADING = 16;
    public static final int GSF_EM_PU_ACTIVE = 32;
    public static final int GSF_MAX_KMALL_EXTRA_CLASSES = 11;
    public static final int GSF_MAX_KMALL_EXTRA_DETECT = 1024;
    public static final int GSF_KMALL_MRZ = 1;
    public static final int PORT_PING = 0;
    public static final int STBD_PING = 1;
    public static final int GSF_FIELD_SIZE_DEFAULT = 0;
    public static final int GSF_FIELD_SIZE_ONE = 16;
    public static final int GSF_FIELD_SIZE_TWO = 32;
    public static final int GSF_FIELD_SIZE_FOUR = 64;
    public static final int GSF_FIELD_SIZE_COUNT = 3;
    public static final int GSF_DISABLE_COMPRESSION = 0;
    public static final int GSF_ENABLE_COMPRESSION = 1;
    public static final int GSF_MAX_PROCESSING_PARAMETERS = 128;
    public static final int GSF_MAX_SENSOR_PARAMETERS = 128;
    public static final int GSF_OPERATOR_LENGTH = 64;
    public static final int GSF_HOST_NAME_LENGTH = 64;
    public static final int GSF_MAX_OFFSETS = 2;
    public static final int GSF_COMPENSATED = 1;
    public static final int GSF_UNCOMPENSATED = 0;
    public static final int GSF_TRUE_DEPTHS = 1;
    public static final int GSF_DEPTHS_RE_1500_MS = 2;
    public static final int GSF_DEPTH_CALC_UNKNOWN = 3;
    public static final int GSF_TRUE = 1;
    public static final int GSF_FALSE = 0;
    public static final int GSF_NUMBER_PROCESSING_PARAMS = 49;
    public static final int GSF_PLATFORM_TYPE_SURFACE_SHIP = 0;
    public static final int GSF_PLATFORM_TYPE_AUV = 1;
    public static final int GSF_PLATFORM_TYPE_ROTV = 2;
    public static final int GSF_HORIZONTAL_PITCH_AXIS = 1;
    public static final int GSF_ROTATED_PITCH_AXIS = 2;
    public static final int GSF_H_DATUM_ADI = 1;
    public static final int GSF_H_DATUM_ARF = 2;
    public static final int GSF_H_DATUM_ARS = 3;
    public static final int GSF_H_DATUM_AUA = 4;
    public static final int GSF_H_DATUM_BAT = 5;
    public static final int GSF_H_DATUM_BID = 6;
    public static final int GSF_H_DATUM_BUR = 7;
    public static final int GSF_H_DATUM_CAI = 8;
    public static final int GSF_H_DATUM_CAM = 9;
    public static final int GSF_H_DATUM_CAP = 10;
    public static final int GSF_H_DATUM_CAA = 11;
    public static final int GSF_H_DATUM_CHO = 12;
    public static final int GSF_H_DATUM_CHU = 13;
    public static final int GSF_H_DATUM_COA = 14;
    public static final int GSF_H_DATUM_ENB = 15;
    public static final int GSF_H_DATUM_EUR = 16;
    public static final int GSF_H_DATUM_GDA = 17;
    public static final int GSF_H_DATUM_GEO = 18;
    public static final int GSF_H_DATUM_GHA = 19;
    public static final int GSF_H_DATUM_GSB = 20;
    public static final int GSF_H_DATUM_GSF = 21;
    public static final int GSF_H_DATUM_GUA = 22;
    public static final int GSF_H_DATUM_HEN = 23;
    public static final int GSF_H_DATUM_HER = 24;
    public static final int GSF_H_DATUM_HJO = 25;
    public static final int GSF_H_DATUM_HTN = 26;
    public static final int GSF_H_DATUM_IDA = 27;
    public static final int GSF_H_DATUM_IND = 28;
    public static final int GSF_H_DATUM_IRE = 29;
    public static final int GSF_H_DATUM_KEA = 30;
    public static final int GSF_H_DATUM_LIB = 31;
    public static final int GSF_H_DATUM_LOC = 32;
    public static final int GSF_H_DATUM_LUZ = 33;
    public static final int GSF_H_DATUM_MER = 34;
    public static final int GSF_H_DATUM_MET = 35;
    public static final int GSF_H_DATUM_MOL = 36;
    public static final int GSF_H_DATUM_NAN = 37;
    public static final int GSF_H_DATUM_NAR = 38;
    public static final int GSF_H_DATUM_NAS = 39;
    public static final int GSF_H_DATUM_NIG = 40;
    public static final int GSF_H_DATUM_OGB = 41;
    public static final int GSF_H_DATUM_OHA = 42;
    public static final int GSF_H_DATUM_OSI = 43;
    public static final int GSF_H_DATUM_PLN = 44;
    public static final int GSF_H_DATUM_PRP = 45;
    public static final int GSF_H_DATUM_QUO = 46;
    public static final int GSF_H_DATUM_SIB = 47;
    public static final int GSF_H_DATUM_TAN = 48;
    public static final int GSF_H_DATUM_TIL = 49;
    public static final int GSF_H_DATUM_TOK = 50;
    public static final int GSF_H_DATUM_UND = 51;
    public static final int GSF_H_DATUM_VOI = 52;
    public static final int GSF_H_DATUM_WGA = 53;
    public static final int GSF_H_DATUM_WGB = 54;
    public static final int GSF_H_DATUM_WGC = 55;
    public static final int GSF_H_DATUM_WGD = 56;
    public static final int GSF_H_DATUM_WGE = 57;
    public static final int GSF_H_DATUM_WGS = 58;
    public static final int GSF_H_DATUM_XXX = 59;
    public static final int GSF_H_DATUM_YAC = 60;
    public static final int GSF_V_DATUM_UNKNOWN = 1;
    public static final int GSF_V_DATUM_MLLW = 2;
    public static final int GSF_V_DATUM_MLW = 3;
    public static final int GSF_V_DATUM_ALAT = 4;
    public static final int GSF_V_DATUM_ESLW = 5;
    public static final int GSF_V_DATUM_ISLW = 6;
    public static final int GSF_V_DATUM_LAT = 7;
    public static final int GSF_V_DATUM_LLW = 8;
    public static final int GSF_V_DATUM_LNLW = 9;
    public static final int GSF_V_DATUM_LWD = 10;
    public static final int GSF_V_DATUM_MLHW = 11;
    public static final int GSF_V_DATUM_MLLWS = 12;
    public static final int GSF_V_DATUM_MLWN = 13;
    public static final int GSF_V_DATUM_MSL = 14;
    public static final int GSF_V_DATUM_ALLW = 15;
    public static final int GSF_V_DATUM_LNT = 16;
    public static final int GSF_V_DATUM_AMLWS = 17;
    public static final int GSF_V_DATUM_AMLLW = 18;
    public static final int GSF_V_DATUM_MLWS = 19;
    public static final int GSF_V_DATUM_AMSL = 20;
    public static final int GSF_V_DATUM_AMLW = 21;
    public static final int GSF_V_DATUM_AISLW = 22;
    public static final int GSF_V_DATUM_ALLWS = 23;
    public static final int GSF_NORMAL = 0;

    public static final int FLT_EVAL_METHOD = 0;
    public static final int FLT_RADIX = 2;
    public static final int FLT_MANT_DIG = 24;
    public static final int DBL_MANT_DIG = 53;
    public static final int LDBL_MANT_DIG = 64;
    public static final int DECIMAL_DIG = 21;
    public static final int FLT_DIG = 6;
    public static final int DBL_DIG = 15;
    public static final int LDBL_DIG = 18;
    public static final int FLT_MIN_EXP = -125;
    public static final int DBL_MIN_EXP = -1021;
    public static final int LDBL_MIN_EXP = -16381;
    public static final int FLT_MIN_10_EXP = -37;
    public static final int DBL_MIN_10_EXP = -307;
    public static final int LDBL_MIN_10_EXP = -4931;
    public static final int FLT_MAX_EXP = 128;
    public static final int DBL_MAX_EXP = 1024;
    public static final int LDBL_MAX_EXP = 16384;
    public static final int FLT_MAX_10_EXP = 38;
    public static final int DBL_MAX_10_EXP = 308;
    public static final int LDBL_MAX_10_EXP = 4932;
    public static final float FLT_MAX = 3.4028234663852886E38f;
    public static final double DBL_MAX = 1.7976931348623157E308d;
    public static final float FLT_EPSILON = 1.1920928955078125E-7f;
    public static final double DBL_EPSILON = 2.220446049250313E-16d;
    public static final float FLT_MIN = 1.1754943508222875E-38f;
    public static final double DBL_MIN = 2.2250738585072014E-308d;
    public static final float FLT_TRUE_MIN = 1.401298464324817E-45f;
    public static final double DBL_TRUE_MIN = 4.9E-324d;
    public static final int FLT_DECIMAL_DIG = 9;
    public static final int DBL_DECIMAL_DIG = 17;
    public static final int LDBL_DECIMAL_DIG = 21;
    public static final int FLT_HAS_SUBNORM = 1;
    public static final int DBL_HAS_SUBNORM = 1;
    public static final int LDBL_HAS_SUBNORM = 1;
    public static final int LITTLE_ENDIAN = 1234;
    public static final int BIG_ENDIAN = 4321;
    public static final int PDP_ENDIAN = 3412;
    public static final int BYTE_ORDER = 1234;
    public static final long _SIGSET_NWORDS = 16;
    public static final int __NFDBITS = 64;
    public static final int FD_SETSIZE = 1024;
    public static final int NFDBITS = 64;
    public static final int __PTHREAD_RWLOCK_ELISION_EXTRA = 0;

    public static final int GSF_RECORD_HEADER = 1;
    public static final int GSF_RECORD_SWATH_BATHYMETRY_PING = 2;
    public static final int GSF_RECORD_SOUND_VELOCITY_PROFILE = 3;
    public static final int GSF_RECORD_PROCESSING_PARAMETERS = 4;
    public static final int GSF_RECORD_SENSOR_PARAMETERS = 5;
    public static final int GSF_RECORD_COMMENT = 6;
    public static final int GSF_RECORD_HISTORY = 7;
    public static final int GSF_RECORD_NAVIGATION_ERROR = 8;
    public static final int GSF_RECORD_SWATH_BATHY_SUMMARY = 9;
    public static final int GSF_RECORD_SINGLE_BEAM_PING = 10;
    public static final int GSF_RECORD_HV_NAVIGATION_ERROR = 11;
    public static final int GSF_RECORD_ATTITUDE = 12;
    public static final int GSF_SWATH_BATHY_SUBRECORD_DEPTH_ARRAY = 1;
    public static final int GSF_SWATH_BATHY_SUBRECORD_ACROSS_TRACK_ARRAY = 2;
    public static final int GSF_SWATH_BATHY_SUBRECORD_ALONG_TRACK_ARRAY = 3;
    public static final int GSF_SWATH_BATHY_SUBRECORD_TRAVEL_TIME_ARRAY = 4;
    public static final int GSF_SWATH_BATHY_SUBRECORD_BEAM_ANGLE_ARRAY = 5;
    public static final int GSF_SWATH_BATHY_SUBRECORD_MEAN_CAL_AMPLITUDE_ARRAY = 6;
    public static final int GSF_SWATH_BATHY_SUBRECORD_MEAN_REL_AMPLITUDE_ARRAY = 7;
    public static final int GSF_SWATH_BATHY_SUBRECORD_ECHO_WIDTH_ARRAY = 8;
    public static final int GSF_SWATH_BATHY_SUBRECORD_QUALITY_FACTOR_ARRAY = 9;
    public static final int GSF_SWATH_BATHY_SUBRECORD_RECEIVE_HEAVE_ARRAY = 10;
    public static final int GSF_SWATH_BATHY_SUBRECORD_DEPTH_ERROR_ARRAY = 11;
    public static final int GSF_SWATH_BATHY_SUBRECORD_ACROSS_TRACK_ERROR_ARRAY = 12;
    public static final int GSF_SWATH_BATHY_SUBRECORD_ALONG_TRACK_ERROR_ARRAY = 13;
    public static final int GSF_SWATH_BATHY_SUBRECORD_NOMINAL_DEPTH_ARRAY = 14;
    public static final int GSF_SWATH_BATHY_SUBRECORD_QUALITY_FLAGS_ARRAY = 15;
    public static final int GSF_SWATH_BATHY_SUBRECORD_BEAM_FLAGS_ARRAY = 16;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SIGNAL_TO_NOISE_ARRAY = 17;
    public static final int GSF_SWATH_BATHY_SUBRECORD_BEAM_ANGLE_FORWARD_ARRAY = 18;
    public static final int GSF_SWATH_BATHY_SUBRECORD_VERTICAL_ERROR_ARRAY = 19;
    public static final int GSF_SWATH_BATHY_SUBRECORD_HORIZONTAL_ERROR_ARRAY = 20;
    public static final int GSF_SWATH_BATHY_SUBRECORD_INTENSITY_SERIES_ARRAY = 21;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SECTOR_NUMBER_ARRAY = 22;
    public static final int GSF_SWATH_BATHY_SUBRECORD_DETECTION_INFO_ARRAY = 23;
    public static final int GSF_SWATH_BATHY_SUBRECORD_INCIDENT_BEAM_ADJ_ARRAY = 24;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SYSTEM_CLEANING_ARRAY = 25;
    public static final int GSF_SWATH_BATHY_SUBRECORD_DOPPLER_CORRECTION_ARRAY = 26;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SONAR_VERT_UNCERT_ARRAY = 27;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SONAR_HORZ_UNCERT_ARRAY = 28;
    public static final int GSF_SWATH_BATHY_SUBRECORD_DETECTION_WINDOW_ARRAY = 29;
    public static final int GSF_SWATH_BATHY_SUBRECORD_MEAN_ABS_COEF_ARRAY = 30;
    public static final int GSF_SWATH_BATHY_SUBRECORD_UNKNOWN = 0;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SCALE_FACTORS = 100;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SEABEAM_SPECIFIC = 102;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM12_SPECIFIC = 103;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM100_SPECIFIC = 104;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM950_SPECIFIC = 105;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM121A_SPECIFIC = 106;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM121_SPECIFIC = 107;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SASS_SPECIFIC = 108;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SEAMAP_SPECIFIC = 109;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SEABAT_SPECIFIC = 110;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM1000_SPECIFIC = 111;
    public static final int GSF_SWATH_BATHY_SUBRECORD_TYPEIII_SEABEAM_SPECIFIC = 112;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SB_AMP_SPECIFIC = 113;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SEABAT_II_SPECIFIC = 114;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SEABAT_8101_SPECIFIC = 115;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SEABEAM_2112_SPECIFIC = 116;
    public static final int GSF_SWATH_BATHY_SUBRECORD_ELAC_MKII_SPECIFIC = 117;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM3000_SPECIFIC = 118;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM1002_SPECIFIC = 119;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM300_SPECIFIC = 120;
    public static final int GSF_SWATH_BATHY_SUBRECORD_CMP_SASS_SPECIFIC = 121;
    public static final int GSF_SWATH_BATHY_SUBRECORD_RESON_8101_SPECIFIC = 122;
    public static final int GSF_SWATH_BATHY_SUBRECORD_RESON_8111_SPECIFIC = 123;
    public static final int GSF_SWATH_BATHY_SUBRECORD_RESON_8124_SPECIFIC = 124;
    public static final int GSF_SWATH_BATHY_SUBRECORD_RESON_8125_SPECIFIC = 125;
    public static final int GSF_SWATH_BATHY_SUBRECORD_RESON_8150_SPECIFIC = 126;
    public static final int GSF_SWATH_BATHY_SUBRECORD_RESON_8160_SPECIFIC = 127;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM120_SPECIFIC = 128;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM3002_SPECIFIC = 129;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM3000D_SPECIFIC = 130;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM3002D_SPECIFIC = 131;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM121A_SIS_SPECIFIC = 132;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM710_SPECIFIC = 133;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM302_SPECIFIC = 134;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM122_SPECIFIC = 135;
    public static final int GSF_SWATH_BATHY_SUBRECORD_GEOSWATH_PLUS_SPECIFIC = 136;
    public static final int GSF_SWATH_BATHY_SUBRECORD_KLEIN_5410_BSS_SPECIFIC = 137;
    public static final int GSF_SWATH_BATHY_SUBRECORD_RESON_7125_SPECIFIC = 138;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM2000_SPECIFIC = 139;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM300_RAW_SPECIFIC = 140;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM1002_RAW_SPECIFIC = 141;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM2000_RAW_SPECIFIC = 142;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM3000_RAW_SPECIFIC = 143;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM120_RAW_SPECIFIC = 144;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM3002_RAW_SPECIFIC = 145;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM3000D_RAW_SPECIFIC = 146;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM3002D_RAW_SPECIFIC = 147;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM121A_SIS_RAW_SPECIFIC = 148;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM2040_SPECIFIC = 149;
    public static final int GSF_SWATH_BATHY_SUBRECORD_DELTA_T_SPECIFIC = 150;
    public static final int GSF_SWATH_BATHY_SUBRECORD_R2SONIC_2022_SPECIFIC = 151;
    public static final int GSF_SWATH_BATHY_SUBRECORD_R2SONIC_2024_SPECIFIC = 152;
    public static final int GSF_SWATH_BATHY_SUBRECORD_R2SONIC_2020_SPECIFIC = 153;
    public static final int GSF_SWATH_BATHY_SUBRECORD_RESON_TSERIES_SPECIFIC = 155;
    public static final int GSF_SWATH_BATHY_SUBRECORD_KMALL_SPECIFIC = 156;
    public static final int GSF_SINGLE_BEAM_SUBRECORD_UNKNOWN = 0;
    public static final int GSF_SINGLE_BEAM_SUBRECORD_ECHOTRAC_SPECIFIC = 201;
    public static final int GSF_SINGLE_BEAM_SUBRECORD_BATHY2000_SPECIFIC = 202;
    public static final int GSF_SINGLE_BEAM_SUBRECORD_MGD77_SPECIFIC = 203;
    public static final int GSF_SINGLE_BEAM_SUBRECORD_BDB_SPECIFIC = 204;
    public static final int GSF_SINGLE_BEAM_SUBRECORD_NOSHDB_SPECIFIC = 205;
    public static final int GSF_SWATH_BATHY_SB_SUBRECORD_ECHOTRAC_SPECIFIC = 206;
    public static final int GSF_SWATH_BATHY_SB_SUBRECORD_BATHY2000_SPECIFIC = 207;
    public static final int GSF_SWATH_BATHY_SB_SUBRECORD_MGD77_SPECIFIC = 208;
    public static final int GSF_SWATH_BATHY_SB_SUBRECORD_BDB_SPECIFIC = 209;
    public static final int GSF_SWATH_BATHY_SB_SUBRECORD_NOSHDB_SPECIFIC = 210;
    public static final int GSF_SWATH_BATHY_SB_SUBRECORD_PDD_SPECIFIC = 211;
    public static final int GSF_SWATH_BATHY_SB_SUBRECORD_NAVISOUND_SPECIFIC = 212;
    public static final double GSF_NULL_LATITUDE = 91.0d;
    public static final double GSF_NULL_LONGITUDE = 181.0d;
    public static final double GSF_NULL_HEADING = 361.0d;
    public static final double GSF_NULL_COURSE = 361.0d;
    public static final double GSF_NULL_SPEED = 99.0d;
    public static final double GSF_NULL_PITCH = 99.0d;
    public static final double GSF_NULL_ROLL = 99.0d;
    public static final double GSF_NULL_HEAVE = 99.0d;
    public static final double GSF_NULL_DRAFT = 0.0d;
    public static final double GSF_NULL_DEPTH_CORRECTOR = 99.99d;
    public static final double GSF_NULL_TIDE_CORRECTOR = 99.99d;
    public static final double GSF_NULL_SOUND_SPEED_CORRECTION = 99.99d;
    public static final double GSF_NULL_HORIZONTAL_ERROR = -1.0d;
    public static final double GSF_NULL_VERTICAL_ERROR = -1.0d;
    public static final double GSF_NULL_HEIGHT = 9999.99d;
    public static final double GSF_NULL_SEP = 9999.99d;
    public static final double GSF_NULL_SEP_UNCERTAINTY = 0.0d;
    public static final double GSF_NULL_DEPTH = 0.0d;
    public static final double GSF_NULL_ACROSS_TRACK = 0.0d;
    public static final double GSF_NULL_ALONG_TRACK = 0.0d;
    public static final double GSF_NULL_TRAVEL_TIME = 0.0d;
    public static final double GSF_NULL_BEAM_ANGLE = 0.0d;
    public static final double GSF_NULL_MC_AMPLITUDE = 0.0d;
    public static final double GSF_NULL_MR_AMPLITUDE = 0.0d;
    public static final double GSF_NULL_ECHO_WIDTH = 0.0d;
    public static final double GSF_NULL_QUALITY_FACTOR = 0.0d;
    public static final double GSF_NULL_RECEIVE_HEAVE = 0.0d;
    public static final double GSF_NULL_DEPTH_ERROR = 0.0d;
    public static final double GSF_NULL_ACROSS_TRACK_ERROR = 0.0d;
    public static final double GSF_NULL_ALONG_TRACK_ERROR = 0.0d;
    public static final double GSF_NULL_NAV_POS_ERROR = 0.0d;
    public static final double GSF_BEAM_WIDTH_UNKNOWN = -1.0d;
    public static final int GSF_GEOSWATH_PLUS_PORT_PING = 0;
    public static final int GSF_GEOSWATH_PLUS_STBD_PING = 1;
    public static final int GSF_IGNORE_PING = 1;
    public static final int GSF_PING_USER_FLAG_01 = 2;
    public static final int GSF_PING_USER_FLAG_02 = 4;
    public static final int GSF_PING_USER_FLAG_03 = 8;
    public static final int GSF_PING_USER_FLAG_04 = 16;
    public static final int GSF_PING_USER_FLAG_05 = 32;
    public static final int GSF_PING_USER_FLAG_06 = 64;
    public static final int GSF_PING_USER_FLAG_07 = 128;
    public static final int GSF_PING_USER_FLAG_08 = 256;
    public static final int GSF_PING_USER_FLAG_09 = 512;
    public static final int GSF_PING_USER_FLAG_10 = 1024;
    public static final int GSF_PING_USER_FLAG_11 = 2048;
    public static final int GSF_PING_USER_FLAG_12 = 4096;
    public static final int GSF_PING_USER_FLAG_13 = 8192;
    public static final int GSF_PING_USER_FLAG_14 = 16384;
    public static final int GSF_PING_USER_FLAG_15 = 32768;
    public static final int GSF_IGNORE_BEAM = 1;
    public static final int GSF_BEAM_USER_FLAG_01 = 2;
    public static final int GSF_BEAM_USER_FLAG_02 = 4;
    public static final int GSF_BEAM_USER_FLAG_03 = 8;
    public static final int GSF_BEAM_USER_FLAG_04 = 16;
    public static final int GSF_BEAM_USER_FLAG_05 = 32;
    public static final int GSF_BEAM_USER_FLAG_06 = 64;
    public static final int GSF_BEAM_USER_FLAG_07 = 128;
    public static final int GSF_INTENSITY_LINEAR = 1;
    public static final int GSF_INTENSITY_CALIBRATED = 2;
    public static final int GSF_INTENSITY_POWER = 4;
    public static final int GSF_INTENSITY_GAIN = 8;

    public static final double GSF_UNKNOWN_PARAM_VALUE = 2.2250738585072014E-308d;

    private static final Map<Integer,String> ERRORCODES;
    static {
        ERRORCODES = new HashMap<>();
        ERRORCODES.put(-99,"GSF_UNKNOWN_PARAM_INT");
        ERRORCODES.put(-1,"GSF_FOPEN_ERROR");
        ERRORCODES.put(-2,"GSF_UNRECOGNIZED_FILE");
        ERRORCODES.put(-3,"GSF_BAD_ACCESS_MODE");
        ERRORCODES.put(-4,"GSF_READ_ERROR");
        ERRORCODES.put(-5,"GSF_WRITE_ERROR");
        ERRORCODES.put(-6,"GSF_INSUFFICIENT_SIZE");
        ERRORCODES.put(-7,"GSF_RECORD_SIZE_ERROR");
        ERRORCODES.put(-8,"GSF_CHECKSUM_FAILURE");
        ERRORCODES.put(-9,"GSF_FILE_CLOSE_ERROR");
        ERRORCODES.put(-10,"GSF_TOO_MANY_ARRAY_SUBRECORDS");
        ERRORCODES.put(-11,"GSF_TOO_MANY_OPEN_FILES");
        ERRORCODES.put(-12,"GSF_MEMORY_ALLOCATION_FAILED");
        ERRORCODES.put(-13,"GSF_UNRECOGNIZED_RECORD_ID");
        ERRORCODES.put(-14,"GSF_STREAM_DECODE_FAILURE");
        ERRORCODES.put(-15,"GSF_BAD_SEEK_OPTION");
        ERRORCODES.put(-16,"GSF_FILE_SEEK_ERROR");
        ERRORCODES.put(-17,"GSF_UNRECOGNIZED_SENSOR_ID");
        ERRORCODES.put(-18,"GSF_UNRECOGNIZED_DATA_RECORD");
        ERRORCODES.put(-19,"GSF_UNRECOGNIZED_ARRAY_SUBRECORD_ID");
        ERRORCODES.put(-20,"GSF_UNRECOGNIZED_SUBRECORD_ID");
        ERRORCODES.put(-21,"GSF_ILLEGAL_SCALE_FACTOR_MULTIPLIER");
        ERRORCODES.put(-22,"GSF_CANNOT_REPRESENT_PRECISION");
        ERRORCODES.put(-23,"GSF_READ_TO_END_OF_FILE");
        ERRORCODES.put(-24,"GSF_BAD_FILE_HANDLE");
        ERRORCODES.put(-25,"GSF_HEADER_RECORD_ENCODE_OR_DECODE_FAILED");
        ERRORCODES.put(-26,"GSF_MB_PING_RECORD_ENCODE_OR_DECODE_FAILED");
        ERRORCODES.put(-27,"GSF_SVP_RECORD_ENCODE_OR_DECODE_FAILED");
        ERRORCODES.put(-28,"GSF_PROCESS_PARAM_RECORD_ENCODE_OR_DECODE_FAILED");
        ERRORCODES.put(-29,"GSF_SENSOR_PARAM_RECORD_ENCODE_OR_DECODE_FAILED");
        ERRORCODES.put(-30,"GSF_COMMENT_RECORD_ENCODE_OR_DECODE_FAILED");
        ERRORCODES.put(-31,"GSF_HISTORY_RECORD_ENCODE_OR_DECODE_FAILED");
        ERRORCODES.put(-32,"GSF_NAV_ERROR_RECORD_ENCODE_OR_DECODE_FAILED");
        ERRORCODES.put(-33,"GSF_SETVBUF_ERROR");
        ERRORCODES.put(-34,"GSF_FLUSH_ERROR");
        ERRORCODES.put(-35,"GSF_FILE_TELL_ERROR");
        ERRORCODES.put(-36,"GSF_INDEX_FILE_OPEN_ERROR");
        ERRORCODES.put(-37,"GSF_CORRUPT_INDEX_FILE_ERROR");
        ERRORCODES.put(-38,"GSF_SCALE_INDEX_CALLOC_ERROR");
        ERRORCODES.put(-39,"GSF_RECORD_TYPE_NOT_AVAILABLE");
        ERRORCODES.put(-40,"GSF_SUMMARY_RECORD_DECODE_FAILED");
        ERRORCODES.put(-41,"GSF_SUMMARY_RECORD_ENCODE_FAILED");
        ERRORCODES.put(-42,"GSF_INVALID_NUM_BEAMS");
        ERRORCODES.put(-43,"GSF_INVALID_RECORD_NUMBER");
        ERRORCODES.put(-44,"GSF_INDEX_FILE_READ_ERROR");
        ERRORCODES.put(-45,"GSF_PARAM_SIZE_FIXED");
        ERRORCODES.put(-46,"GSF_SINGLE_BEAM_ENCODE_FAILED");
        ERRORCODES.put(-47,"GSF_HV_NAV_ERROR_RECORD_ENCODE_FAILED");
        ERRORCODES.put(-48,"GSF_HV_NAV_ERROR_RECORD_DECODE_FAILED");
        ERRORCODES.put(-49,"GSF_ATTITUDE_RECORD_ENCODE_FAILED");
        ERRORCODES.put(-50,"GSF_ATTITUDE_RECORD_DECODE_FAILED");
        ERRORCODES.put(-51,"GSF_OPEN_TEMP_FILE_FAILED");
        ERRORCODES.put(-52,"GSF_PARTIAL_RECORD_AT_END_OF_FILE");
        ERRORCODES.put(-53,"GSF_QUALITY_FLAGS_DECODE_ERROR");
        ERRORCODES.put(-55,"GSF_COMPRESSION_UNSUPPORTED");
        ERRORCODES.put(-56,"GSF_COMPRESSION_FAILED");
    }

    /**
     * The global instance, created when first needed.
     * This field shall be read and updated in a synchronized block.
     * It may be reset to {@code null} if <abbr>GSF</abbr> reported a fatal error.
     *
     * @see #global(boolean)
     */
    private static GSF global;

    /**
     * Whether an error occurred during initialization of {@link #global}.
     * Shall be read and updated in the same synchronization block as {@link #global}.
     */
    private static LibraryStatus globalStatus;

    /**
     * {@snippet lang=c : int gsfOpen(const char *filename, const int mode, int *handle) }.
     */
    final MethodHandle gsfOpen;
    /**
     * {@snippet lang=c : int gsfOpenBuffered(const char *filename, const int mode, int *handle, int buf_size) }
     */
    final MethodHandle gsfOpenBuffered;
    /**
     * {@snippet lang=c : int gsfClose(const int handle) }
     */
    final MethodHandle gsfClose;
    /**
     * {@snippet lang=c : int gsfSeek(int handle, int option)}
     */
    final MethodHandle gsfSeek;
    /**
     * {@snippet lang=c : int gsfRead(int handle, int desiredRecord, gsfDataID *dataID, gsfRecords *rec, unsigned char *stream, int max_size) }
     */
    final MethodHandle gsfRead;
    /**
     * {@snippet lang=c : int gsfWrite(int handle, gsfDataID *id, gsfRecords *record) }
     */
    final MethodHandle gsfWrite;
    /**
     * {@snippet lang=c : int gsfLoadScaleFactor(gsfScaleFactors *sf, unsigned int subrecordID, char c_flag, double precision, int offset) }
     */
    final MethodHandle gsfLoadScaleFactor;
    /**
     * {@snippet lang=c : int gsfGetScaleFactor(int handle, unsigned int subrecordID, unsigned char *c_flag, double *multiplier, double *offset) }
     */
    final MethodHandle gsfGetScaleFactor;
    /**
     * {@snippet lang=c : void gsfFree(gsfRecords *rec) }
     * <p>
     * This function frees all dynamically allocated memory from a gsfRecords data structure, and then clears
     * all the data elements in the structure.
     */
    final MethodHandle gsfFree;
    /**
     * {@snippet lang=c : void gsfPrintError(FILE *fp) }
     */
    final MethodHandle gsfPrintError;
    /**
     * {@snippet lang=c : int gsfIntError() }
     */
    final MethodHandle gsfIntError;
    /**
     * {@snippet lang=c : const char *gsfStringError() }
     */
    final MethodHandle gsfStringError;
    /**
     * {@snippet lang=c : int gsfIndexTime(int handle, int recordID, int record_number, time_t *sec, long *nsec) }
     */
    final MethodHandle gsfIndexTime;
    /**
     * {@snippet lang=c : int gsfPercent(int handle) }
     */
    final MethodHandle gsfPercent;
    /**
     * {@snippet lang=c : int gsfGetNumberRecords(int handle, int desiredRecord) }
     */
    final MethodHandle gsfGetNumberRecords;
    /**
     * {@snippet lang=c : int gsfCopyRecords(gsfRecords *target, const gsfRecords *source) }
     */
    final MethodHandle gsfCopyRecords;
    /**
     * {@snippet lang=c : int gsfPutMBParams(const gsfMBParams *p, gsfRecords *rec, int handle, int numArrays) }
     */
    final MethodHandle gsfPutMBParams;
    /**
     * {@snippet lang=c : int gsfGetMBParams(const gsfRecords *rec, gsfMBParams *p, int *numArrays) }
     */
    final MethodHandle gsfGetMBParams;
    /**
     * {@snippet lang=c : int gsfGetSwathBathyBeamWidths(const gsfRecords *data, double *fore_aft, double *athwartship) }
     */
    final MethodHandle gsfGetSwathBathyBeamWidths;
    /**
     * {@snippet lang=c : int gsfIsStarboardPing(const gsfRecords *data) }
     */
    final MethodHandle gsfIsStarboardPing;
    /**
     * {@snippet lang=c : int gsfLoadDepthScaleFactorAutoOffset(gsfSwathBathyPing *ping, unsigned int subrecordID, int reset, double min_depth, double max_depth, double *last_corrector, char c_flag, double precision) }
     */
    final MethodHandle gsfLoadDepthScaleFactorAutoOffset;
    /**
     * {@snippet lang=c : int gsfGetSwathBathyArrayMinMax(const gsfSwathBathyPing *ping, unsigned int subrecordID, double *min_value, double *max_value) }
     */
    final MethodHandle gsfGetSwathBathyArrayMinMax;
    /**
     * {@snippet lang=c : const char *gsfGetSonarTextName(const gsfSwathBathyPing *ping) }
     */
    final MethodHandle gsfGetSonarTextName;
    /**
     * {@snippet lang=c : int gsfFileSupportsRecalculateXYZ(int handle, int *status) }
     */
    final MethodHandle gsfFileSupportsRecalculateXYZ;
    /**
     * {@snippet lang=c : int gsfFileSupportsRecalculateTPU(int handle, int *status) }
     */
    final MethodHandle gsfFileSupportsRecalculateTPU;
    /**
     * {@snippet lang=c : int gsfFileSupportsRecalculateNominalDepth(int handle, int *status) }
     */
    final MethodHandle gsfFileSupportsRecalculateNominalDepth;
    /**
     * {@snippet lang=c : int gsfFileContainsMBAmplitude(int handle, int *status) }
     */
    final MethodHandle gsfFileContainsMBAmplitude;
    /**
     * {@snippet lang=c : int gsfFileContainsMBImagery(int handle, int *status) }
     */
    final MethodHandle gsfFileContainsMBImagery;
    /**
     * {@snippet lang=c : int gsfIsNewSurveyLine(int handle, const gsfRecords *rec, double azimuth_change, double *last_heading) }
     */
    final MethodHandle gsfIsNewSurveyLine;
    /**
     * {@snippet lang=c : void gsfInitializeMBParams(gsfMBParams *p) }
     */
    final MethodHandle gsfInitializeMBParams;
    /**
     * {@snippet lang=c : int gsfStat(const char *filename, long long *sz) }
     */
    final MethodHandle gsfStat;
    /**
     * {@snippet lang=c : GSF_POSITION *gsfGetPositionDestination(GSF_POSITION gp, GSF_POSITION_OFFSETS offsets, double hdg, double dist_step) }
     */
    final MethodHandle gsfGetPositionDestination;
    /**
     * {@snippet lang=c : GSF_POSITION_OFFSETS *gsfGetPositionOffsets(GSF_POSITION gp_from, GSF_POSITION gp_to, double hdg, double dist_step) }
     */
    final MethodHandle gsfGetPositionOffsets;

    /**
     * Creates the handles for all <abbr>GSF</abbr> functions which will be needed.
     *
     * @param  loader  the object used for loading the library.
     * @throws NoSuchElementException if a <abbr>GSF</abbr> function has not been found in the library.
     */
    private GSF(final LibraryLoader<GSF> loader) {
        super(loader);

        final FunctionDescriptor p = FunctionDescriptor.of(GSF.C_POINTER);
        final FunctionDescriptor i_i = FunctionDescriptor.of(GSF.C_INT, GSF.C_INT);
        final FunctionDescriptor i_i_i = FunctionDescriptor.of(GSF.C_INT, GSF.C_INT, GSF.C_INT);
        final FunctionDescriptor i_p_p = FunctionDescriptor.of(GSF.C_INT, GSF.C_POINTER, GSF.C_POINTER);
        final FunctionDescriptor i_p_p_p = FunctionDescriptor.of(GSF.C_INT, GSF.C_POINTER, GSF.C_POINTER, GSF.C_POINTER);
        final FunctionDescriptor i_i_p = FunctionDescriptor.of(GSF.C_INT, GSF.C_INT, GSF.C_POINTER);

        gsfOpen                                 = lookup("gsfOpen",                                 FunctionDescriptor.of(GSF.C_INT, GSF.C_POINTER, GSF.C_INT, GSF.C_POINTER));
        gsfOpenBuffered                         = lookup("gsfOpenBuffered",                         FunctionDescriptor.of(GSF.C_INT, GSF.C_POINTER, GSF.C_INT, GSF.C_POINTER, GSF.C_INT));
        gsfClose                                = lookup("gsfClose",                                i_i);
        gsfSeek                                 = lookup("gsfSeek",                                 i_i_i);
        gsfRead                                 = lookup("gsfRead",                                 FunctionDescriptor.of(GSF.C_INT, GSF.C_INT, GSF.C_INT, GSF.C_POINTER, GSF.C_POINTER, GSF.C_POINTER, GSF.C_INT));
        gsfWrite                                = lookup("gsfWrite",                                FunctionDescriptor.of(GSF.C_INT, GSF.C_INT, GSF.C_POINTER, GSF.C_POINTER));
        gsfLoadScaleFactor                      = lookup("gsfLoadScaleFactor",                      FunctionDescriptor.of(GSF.C_INT, GSF.C_POINTER, GSF.C_INT, GSF.C_CHAR, GSF.C_DOUBLE, GSF.C_INT));
        gsfGetScaleFactor                       = lookup("gsfGetScaleFactor",                       FunctionDescriptor.of(GSF.C_INT, GSF.C_INT, GSF.C_INT, GSF.C_POINTER, GSF.C_POINTER, GSF.C_POINTER));
        gsfFree                                 = lookup("gsfFree",                                 FunctionDescriptor.ofVoid(GSF.C_POINTER));
        gsfPrintError                           = lookup("gsfPrintError",                           p);
        gsfIntError                             = lookup("gsfIntError",                             FunctionDescriptor.of(GSF.C_INT));
        gsfStringError                          = lookup("gsfStringError",                          p);
        gsfIndexTime                            = lookup("gsfIndexTime",                            FunctionDescriptor.of(GSF.C_INT, GSF.C_INT, GSF.C_INT, GSF.C_INT, GSF.C_POINTER, GSF.C_POINTER));
        gsfPercent                              = lookup("gsfPercent",                              i_i);
        gsfGetNumberRecords                     = lookup("gsfGetNumberRecords",                     i_i_i);
        gsfCopyRecords                          = lookup("gsfCopyRecords",                          i_p_p);
        gsfPutMBParams                          = lookup("gsfPutMBParams",                          FunctionDescriptor.of(GSF.C_INT, GSF.C_POINTER, GSF.C_POINTER, GSF.C_INT, GSF.C_INT));
        gsfGetMBParams                          = lookup("gsfGetMBParams",                          i_p_p_p);
        gsfGetSwathBathyBeamWidths              = lookup("gsfGetSwathBathyBeamWidths",              i_p_p_p);
        gsfIsStarboardPing                      = lookup("gsfIsStarboardPing",                      FunctionDescriptor.of(GSF.C_INT, GSF.C_POINTER));
        gsfLoadDepthScaleFactorAutoOffset       = lookup("gsfLoadDepthScaleFactorAutoOffset",       FunctionDescriptor.of(GSF.C_INT, GSF.C_POINTER, GSF.C_INT, GSF.C_INT, GSF.C_DOUBLE, GSF.C_DOUBLE, GSF.C_POINTER, GSF.C_CHAR, GSF.C_DOUBLE));
        gsfGetSwathBathyArrayMinMax             = lookup("gsfGetSwathBathyArrayMinMax",             FunctionDescriptor.of(GSF.C_INT, GSF.C_POINTER, GSF.C_INT, GSF.C_POINTER, GSF.C_POINTER));
        gsfGetSonarTextName                     = lookup("gsfGetSonarTextName",                     FunctionDescriptor.of(GSF.C_POINTER, GSF.C_POINTER));
        gsfFileSupportsRecalculateXYZ           = lookup("gsfFileSupportsRecalculateXYZ",           i_i_p);
        gsfFileSupportsRecalculateTPU           = lookup("gsfFileSupportsRecalculateTPU",           i_i_p);
        gsfFileSupportsRecalculateNominalDepth  = lookup("gsfFileSupportsRecalculateNominalDepth",  i_i_p);
        gsfFileContainsMBAmplitude              = lookup("gsfFileContainsMBAmplitude",              i_i_p);
        gsfFileContainsMBImagery                = lookup("gsfFileContainsMBImagery",                i_i_p);
        gsfIsNewSurveyLine                      = lookup("gsfIsNewSurveyLine",                      FunctionDescriptor.of(GSF.C_INT, GSF.C_INT, GSF.C_POINTER, GSF.C_DOUBLE, GSF.C_POINTER));
        gsfInitializeMBParams                   = lookup("gsfInitializeMBParams",                   p);
        gsfStat                                 = lookup("gsfStat",                                 i_p_p);
        gsfGetPositionDestination               = lookup("gsfGetPositionDestination",               FunctionDescriptor.of(GSF.C_POINTER, Position.LAYOUT, PositionOffsets.LAYOUT, GSF.C_DOUBLE, GSF.C_DOUBLE));
        gsfGetPositionOffsets                   = lookup("gsfGetPositionOffsets",                   FunctionDescriptor.of(GSF.C_POINTER, Position.LAYOUT, Position.LAYOUT, GSF.C_DOUBLE, GSF.C_DOUBLE));
    }

    public Arena getArena() {
        return arena();
    }

    public int open(MemorySegment filename, int mode, MemorySegment handle) throws Throwable {
        return (int) gsfOpen.invokeExact(filename, mode, handle);
    }

    public int openBuffered(MemorySegment filename, int mode, MemorySegment handle, int buf_size) throws Throwable {
        return (int)gsfOpenBuffered.invokeExact(filename, mode, handle, buf_size);
    }

    public int close(int handle) throws Throwable {
        return (int) gsfClose.invokeExact(handle);
    }

    public int seek(int handle, int option) throws Throwable {
        return (int)gsfSeek.invokeExact(handle, option);
    }

    /**
     * @return This function returns the number of bytes read if successful, or -1 if an error occurred.
     * @throws Throwable
     */
    public int read(int handle, int desiredRecord, MemorySegment dataID, MemorySegment rec, MemorySegment stream, int max_size) throws Throwable {
        return (int)gsfRead.invokeExact(handle, desiredRecord, dataID, rec, stream, max_size);
    }

    public int write(int handle, MemorySegment id, MemorySegment record_) throws Throwable {
        return (int)gsfWrite.invokeExact(handle, id, record_);
    }

    public int loadScaleFactor(MemorySegment sf, int subrecordID, byte c_flag, double precision, int offset) throws Throwable {
        return (int)gsfLoadScaleFactor.invokeExact(sf, subrecordID, c_flag, precision, offset);
    }

    public int getScaleFactor(int handle, int subrecordID, MemorySegment c_flag, MemorySegment multiplier, MemorySegment offset) throws Throwable {
        return (int)gsfGetScaleFactor.invokeExact(handle, subrecordID, c_flag, multiplier, offset);
    }

    /**
     * This function frees all dynamically allocated memory from a gsfRecords data structure, and then clears
     * all the data elements in the structure.
     *
     * NOTE : in version 3.09 and 3.10 this function should not be used or it might corrupt
     * memory and cause a JVM crash.
     * Tthis error happens because GSFLib keeps an internal Records in a table for each opened file
     * and copies the pointer in the return Records.
     * When we free our record, it frees the pointed tables but the pointers are not reset in the internal Records
     * causing errors later when it is used.
     */
    public void free(MemorySegment rec) throws Throwable {
        gsfFree.invokeExact(rec);
    }

    public void printError(MemorySegment fp) throws Throwable {
        gsfPrintError.invokeExact(fp);
    }

    public int intError() throws Throwable {
        return (int)gsfIntError.invokeExact();
    }

    public MemorySegment stringError() throws Throwable {
        return (MemorySegment)gsfStringError.invokeExact();
    }

    public int indexTime(int handle, int recordID, int record_number, MemorySegment sec, MemorySegment nsec) throws Throwable {
        return (int)gsfIndexTime.invokeExact(handle, recordID, record_number, sec, nsec);
    }

    public int percent(int handle) throws Throwable {
        return (int)gsfPercent.invokeExact(handle);
    }

    public int getNumberRecords(int handle, int desiredRecord) throws Throwable {
        return (int)gsfGetNumberRecords.invokeExact(handle, desiredRecord);
    }

    public int copyRecords(MemorySegment target, MemorySegment source) throws Throwable {
        return (int)gsfCopyRecords.invokeExact(target, source);
    }

    public int putMBParams(MemorySegment p, MemorySegment rec, int handle, int numArrays) throws Throwable {
        return (int)gsfPutMBParams.invokeExact(p, rec, handle, numArrays);
    }

    public int getMBParams(MemorySegment rec, MemorySegment p, MemorySegment numArrays) throws Throwable {
        return (int)gsfGetMBParams.invokeExact(rec, p, numArrays);
    }

    public int getSwathBathyBeamWidths(MemorySegment data, MemorySegment fore_aft, MemorySegment athwartship) throws Throwable {
        return (int)gsfGetSwathBathyBeamWidths.invokeExact(data, fore_aft, athwartship);
    }

    public int isStarboardPing(MemorySegment data) throws Throwable {
        return (int)gsfIsStarboardPing.invokeExact(data);
    }

    public int loadDepthScaleFactorAutoOffset(MemorySegment ping, int subrecordID, int reset, double min_depth, double max_depth, MemorySegment last_corrector, byte c_flag, double precision) throws Throwable {
        return (int)gsfLoadDepthScaleFactorAutoOffset.invokeExact(ping, subrecordID, reset, min_depth, max_depth, last_corrector, c_flag, precision);
    }

    public int getSwathBathyArrayMinMax(MemorySegment ping, int subrecordID, MemorySegment min_value, MemorySegment max_value) throws Throwable {
        return (int)gsfGetSwathBathyArrayMinMax.invokeExact(ping, subrecordID, min_value, max_value);
    }

    public MemorySegment getSonarTextName(MemorySegment ping) throws Throwable {
        return (MemorySegment)gsfGetSonarTextName.invokeExact(ping);
    }

    public int fileSupportsRecalculateXYZ(int handle, MemorySegment status) throws Throwable {
        return (int)gsfFileSupportsRecalculateXYZ.invokeExact(handle, status);
    }

    public int fileSupportsRecalculateTPU(int handle, MemorySegment status) throws Throwable {
        return (int)gsfFileSupportsRecalculateTPU.invokeExact(handle, status);
    }

    public int fileSupportsRecalculateNominalDepth(int handle, MemorySegment status) throws Throwable {
        return (int)gsfFileSupportsRecalculateNominalDepth.invokeExact(handle, status);
    }

    public int fileContainsMBAmplitude(int handle, MemorySegment status) throws Throwable {
        return (int)gsfFileContainsMBAmplitude.invokeExact(handle, status);
    }

    public int fileContainsMBImagery(int handle, MemorySegment status) throws Throwable {
        return (int)gsfFileContainsMBImagery.invokeExact(handle, status);
    }

    public int isNewSurveyLine(int handle, MemorySegment rec, double azimuth_change, MemorySegment last_heading) throws Throwable {
        return (int)gsfIsNewSurveyLine.invokeExact(handle, rec, azimuth_change, last_heading);
    }

    public void initializeMBParams(MemorySegment p) throws Throwable {
        gsfInitializeMBParams.invokeExact(p);
    }

    public int stat(MemorySegment filename, MemorySegment sz) throws Throwable {
        return (int)gsfStat.invokeExact(filename, sz);
    }

    public MemorySegment getPositionDestination(MemorySegment gp, MemorySegment offsets, double hdg, double dist_step) throws Throwable {
        return (MemorySegment)gsfGetPositionDestination.invokeExact(gp, offsets, hdg, dist_step);
    }

    public MemorySegment getPositionOffsets(MemorySegment gp_from, MemorySegment gp_to, double hdg, double dist_step) throws Throwable {
        return (MemorySegment)gsfGetPositionOffsets.invokeExact(gp_from, gp_to, hdg, dist_step);
    }

    /**
     * Raise an exception if code is an error code.
     */
    public void catchError(int error) throws GSFException {
        if (error == 0) return;
        String message = ERRORCODES.get(error);
        if (message == null) message = "UNKNOWN ERROR CODE";
        throw new GSFException(error, message);
    }

    /**
     * Returns the helper class for loading the <abbr>GSF</abbr> library.
     * If {@code def} is true, then this method tries to load the library
     * now and stores the result in {@link #global} and {@link #globalStatus}.
     *
     * @param  now  whether this method is invoked for the default (global) library.
     *         In such case, the caller must be synchronized and {@link #global} must be initially null.
     * @return the library loader for <abbr>GSF</abbr>.
     */
    private static LibraryLoader<GSF> load(final boolean now) {
        final var loader = new LibraryLoader<>(GSF::new);
        if (now) {
            try {
                global = loader.global("gsf");
            } finally {
                globalStatus = loader.status();
            }
            if (global != null) {
                if (GSFStoreProvider.LOGGER.isLoggable(Level.CONFIG)) {
                    log("open", new LogRecord(Level.CONFIG, "Opening LibGSF"));
                }
            }
        }
        return loader;
    }

    /**
     * Loads the <abbr>GSF</abbr> library from the given file.
     * Callers should register the returned instance in a {@link java.lang.ref.Cleaner}.
     *
     * @param  library  the library to load.
     * @return handles to native functions needed by this module.
     * @throws IllegalArgumentException if the GSF library has not been found.
     * @throws NoSuchElementException if a <abbr>GSF</abbr> function has not been found in the library.
     * @throws IllegalCallerException if this Apache SIS module is not authorized to call native methods.
     */
    static GSF load(final Path library) {
        return load(false).load(library);
    }

    /**
     * Returns an instance using the <abbr>GSF</abbr> library loaded from the default library path.
     * The handles are valid for the Java Virtual Machine lifetime, i.e. it uses the global arena.
     * If this method has already been invoked, this method returns the previously created instance.
     *
     * <p>If the <abbr>GSF</abbr> library is not found, the current default is {@link SymbolLookup#loaderLookup()}
     * for allowing users to invoke {@link System#loadLibrary(String)} as a fallback. This policy may be revised in
     * any future version of Apache <abbr>SIS</abbr>.</p>
     *
     * @return handles to native functions needed by this module.
     * @throws DataStoreException if the native library has not been found or if SIS is not allowed to call
     *         native functions, and {@code onError} is null.
     */
    static synchronized GSF global() throws DataStoreException {
        if (globalStatus == null) {
            load(true).validate();
        }
        globalStatus.report(null);
        return global;
    }

    /**
     * Same as {@link #global}, but logs a warning instead of throwing an exception in case of error.
     *
     * @param  caller  the name of the method which is invoking this method.
     * @return handles to native functions needed by this module, or empty if not available.
     */
    static synchronized Optional<GSF> tryGlobal(final String caller) {
        if (globalStatus == null) {
            load(true).getError(GSFStoreProvider.NAME).ifPresent((record) -> log(caller, record));
        }
        return Optional.ofNullable(global);
    }

    /**
     * Logs the given record as if was produced by the {@link GSFStoreProvider}, which is the public class.
     *
     * @param  caller  the method name to report as the caller.
     * @param  record  the error to log.
     */
    private static void log(final String caller, final LogRecord record) {
        Logging.completeAndLog(GSFStoreProvider.LOGGER, GSFStoreProvider.class, caller, record);
    }

}
