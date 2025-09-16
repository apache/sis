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
package org.apache.sis.storage.gpx;


/**
 * GPX tag names in XML files and their namespaces. Also used for feature property names.
 * Unless otherwise noticed by a "(v1.0)" or "(v1.1)" text in the javadoc, tags in this class
 * apply to all supported GPX versions.
 *
 * @author  Johann Sorel (Geomatys)
 */
final class Tags {
    /**
     * GPX scope name used for feature type names.
     */
    static final String PREFIX = "gpx";

    /**
     * GPX XML namespace (common root to all versions).
     */
    static final String NAMESPACE = "http://www.topografix.com/GPX/";

    /**
     * GPX 1.0 XML namespace (v1.0).
     */
    static final String NAMESPACE_V10 = NAMESPACE + "1/0";

    /**
     * GPX 1.1 XML namespace (v1.1).
     */
    static final String NAMESPACE_V11 = NAMESPACE + "1/1";

    /** Main GPX XML tags.              */  static final String GPX             = "gpx";
    /** A tag used a bit everywhere.    */  static final String NAME            = "name";
    /** A tag used a bit everywhere.    */  static final String URL             = "url";
    /** A tag used a bit everywhere.    */  static final String URL_NAME        = "urlname";
    /** A tag used a bit everywhere.    */  static final String LINK            = "link";
    /** A tag used a bit everywhere.    */  static final String DESCRIPTION     = "desc";
    /** A tag used a bit everywhere.    */  static final String COMMENT         = "cmt";
    /** A tag used a bit everywhere.    */  static final String SOURCE          = "src";
    /** A tag used a bit everywhere.    */  static final String TYPE            = "type";
    /** A tag used a bit everywhere.    */  static final String NUMBER          = "number";
    /** A metadata tag (v1.1+).         */  static final String METADATA        = "metadata";
    /** A metadata tag.                 */  static final String TIME            = "time";
    /** A metadata tag.                 */  static final String KEYWORDS        = "keywords";
    /** Attribute in v1.0, tag in v1.1. */  static final String AUTHOR          = "author";
    /** A person tag.                   */  static final String EMAIL           = "email";
    /** A copyright tag (v1.1+).        */  static final String COPYRIGHT       = "copyright";
    /** A copyright tag (v1.1+).        */  static final String YEAR            = "year";
    /** A copyright tag (v1.1+).        */  static final String LICENSE         = "license";
    /** A bounds tag.                   */  static final String BOUNDS          = "bounds";
    /** A link tag (v1.1+).             */  static final String TEXT            = "text";
    /** A way Point tag.                */  static final String WAY_POINT       = "wpt";
    /** A way Point tag.                */  static final String ELEVATION       = "ele";
    /** A way Point tag.                */  static final String MAGNETIC_VAR    = "magvar";
    /** A way Point tag.                */  static final String GEOID_HEIGHT    = "geoidheight";
    /** A way Point tag.                */  static final String SYMBOL          = "sym";
    /** A way Point tag.                */  static final String FIX             = "fix";
    /** A way Point tag.                */  static final String SATELITTES      = "sat";
    /** A way Point tag.                */  static final String HDOP            = "hdop";
    /** A way Point tag.                */  static final String VDOP            = "vdop";
    /** A way Point tag.                */  static final String PDOP            = "pdop";
    /** A way Point tag.                */  static final String AGE_OF_GPS_DATA = "ageofdgpsdata";
    /** A way Point tag.                */  static final String DGPS_ID         = "dgpsid";
    /** A route tag.                    */  static final String ROUTES          = "rte";
    /** A route tag.                    */  static final String ROUTE_POINTS    = "rtept";
    /** A track tag.                    */  static final String TRACKS          = "trk";
    /** A track tag.                    */  static final String TRACK_SEGMENTS  = "trkseg";
    /** A track tag.                    */  static final String TRACK_POINTS    = "trkpt";

    /**
     * Do not allow instantiation of this class.
     */
    private Tags() {
    }
}
