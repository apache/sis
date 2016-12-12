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
 * GPX attribute names in XML files.
 *
 * @todo may be refactored.
 *
 * @author  Johann Sorel (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class Constants extends Static {
    /*
     * Main GPX XML tags.
     */
    /** Used in versions 1.0 and 1.1. */
    public static final String ATT_GPX_VERSION = "version";
    /** Used in versions 1.0 and 1.1. */
    public static final String ATT_GPX_CREATOR = "creator";

    /*
     * Copyright tags.
     */
    /** Used in version 1.1. */
    public static final String ATT_COPYRIGHT_AUTHOR = "author";

    /*
     * Bounds tags.
     */
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
    public static final String ATT_LINK_HREF = "href";

    /*
     * WPT tags.
     */
    /** Used in versions 1.0 and 1.1. */
    public static final String ATT_WPT_LAT = "lat";
    /** Used in versions 1.0 and 1.1. */
    public static final String ATT_WPT_LON = "lon";

    /**
     * Do not allow instantiation of this class.
     */
    private Constants() {
    }
}
