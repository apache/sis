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
package org.apache.sis.internal.gpx;

import org.apache.sis.util.Static;


/**
 * GPX XML tags and feature types.
 *
 * @author  Johann Sorel (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class GPXConstants extends Static {
    /*
     * Main GPX XML tags.
     */
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_GPX = "gpx";
    /** Used in versions 1.0 and 1.1. */
    public static final String ATT_GPX_VERSION = "version";
    /** Used in versions 1.0 and 1.1. */
    public static final String ATT_GPX_CREATOR = "creator";

    /*
     * Attributes used a bit everywhere.
     */
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_NAME = "name";
    /** Used in version 1.0. */
    public static final String TAG_URL = "url";
    /** Used in version 1.0. */
    public static final String TAG_URLNAME = "urlname";
    /** Used in version 1.1. */
    public static final String TAG_LINK = "link";
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_DESC = "desc";
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_CMT = "cmt";
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_SRC = "src";
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_TYPE = "type";
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_NUMBER = "number";

    /*
     * Metadata tags.
     */
    /** Used in version 1.1. */
    public static final String TAG_METADATA = "metadata";
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_METADATA_TIME = "time";
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_METADATA_KEYWORDS = "keywords";

    /*
     * Person tags.
     */
    /** Used in version 1.0 (as attribute) and 1.1 (as tag) */
    public static final String TAG_AUTHOR = "author";
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_AUTHOR_EMAIL = "email";

    /*
     * Copyright tags.
     */
    /** Used in version 1.1. */
    public static final String TAG_COPYRIGHT = "copyright";
    /** Used in version 1.1. */
    public static final String TAG_COPYRIGHT_YEAR = "year";
    /** Used in version 1.1. */
    public static final String TAG_COPYRIGHT_LICENSE = "license";
    /** Used in version 1.1. */
    public static final String ATT_COPYRIGHT_AUTHOR = "author";

    /*
     * Bounds tags.
     */
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_BOUNDS = "bounds";
    /** Used in versions 1.0 and 1.1. */
    public static final String ATT_BOUNDS_MINLAT = "minlat";
    /** Used in versions 1.0 and 1.1. */
    public static final String ATT_BOUNDS_MINLON = "minlon";
    /** Used in versions 1.0 and 1.1. */
    public static final String ATT_BOUNDS_MAXLAT = "maxlat";
    /** Used in versions 1.0 and 1.1. */
    public static final String ATT_BOUNDS_MAXLON = "maxlon";

    /*
     * Link tags.
     */
    /** Used in version 1.1. */
    public static final String TAG_LINK_TEXT = "text";
    /** Used in version 1.1. */
    public static final String TAG_LINK_TYPE = "type";
    /** Used in version 1.1. */
    public static final String ATT_LINK_HREF = "href";

    /*
     * WPT tags.
     */
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_WPT = "wpt";
    /** Used in versions 1.0 and 1.1. */
    public static final String ATT_WPT_LAT = "lat";
    /** Used in versions 1.0 and 1.1. */
    public static final String ATT_WPT_LON = "lon";
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_WPT_ELE = "ele";
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_WPT_TIME = "time";
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_WPT_MAGVAR = "magvar";
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_WPT_GEOIHEIGHT = "geoidheight";
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_WPT_SYM = "sym";
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_WPT_FIX = "fix";
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_WPT_SAT = "sat";
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_WPT_HDOP = "hdop";
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_WPT_VDOP = "vdop";
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_WPT_PDOP = "pdop";
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_WPT_AGEOFGPSDATA = "ageofdgpsdata";
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_WPT_DGPSID = "dgpsid";

    /*
     * RTE tags.
     */
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_RTE = "rte";
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_RTE_RTEPT = "rtept";

    /*
     * TRK tags.
     */
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_TRK = "trk";
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_TRK_SEG = "trkseg";
    /** Used in versions 1.0 and 1.1. */
    public static final String TAG_TRK_SEG_PT = "trkpt";

    /**
     * GPX scope name used for feature type names.
     */
    public static final String GPX_NAMESPACE = "http://www.topografix.com";

    /**
     * GPX 1.1 XML namespace.
     */
    public static final String GPX_NAMESPACE_V11 = "http://www.topografix.com/GPX/1/1";
    /**
     * GPX 1.0 XML namespace.
     */
    public static final String GPX_NAMESPACE_V10 = "http://www.topografix.com/GPX/1/0";

    /**
     * Do not allow instantiation of this class.
     */
    private GPXConstants() {
    }
}
