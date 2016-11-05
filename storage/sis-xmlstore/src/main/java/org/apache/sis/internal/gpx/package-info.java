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


/**
 * Reads and writes data in GPS Exchange Format (GPX).
 * The GPX format can be used to describe waypoints, tracks, and routes.
 * Example (from Wikipedia):
 *
 * {@preformat xml
 *   <gpx version="1.1" creator="Oregon 400t">
 *     <metadata>
 *       <link href="http://www.garmin.com">
 *         <text>Garmin International</text>
 *       </link>
 *       <time>2009-10-17T22:58:43Z</time>
 *     </metadata>
 *     <trk>
 *       <name>Example GPX Document</name>
 *       <trkseg>
 *         <trkpt lat="47.644548" lon="-122.326897">
 *           <ele>4.46</ele>
 *           <time>2009-10-17T18:37:26Z</time>
 *         </trkpt>
 *         <trkpt lat="47.644548" lon="-122.326897">
 *           <ele>4.94</ele>
 *           <time>2009-10-17T18:37:31Z</time>
 *         </trkpt>
 *         <trkpt lat="47.644548" lon="-122.326897">
 *           <ele>6.87</ele>
 *           <time>2009-10-17T18:37:34Z</time>
 *         </trkpt>
 *       </trkseg>
 *     </trk>
 *   </gpx>
 * }
 *
 * The GPX 1.1 specification enforces the following conventions:
 * <ul>
 *   <li>All coordinates are relative to the WGS84 datum.</li>
 *   <li>All measurements are in metric units.</li>
 * </ul>
 *
 * @see <a href="https://en.wikipedia.org/wiki/GPS_Exchange_Format">GPS Exchange Format on Wikipedia</a>
 * @see <a href="http://www.topografix.com/GPX/1/1/">GPX 1.1 Schema Documentation</a>
 *
 * @author  Johann Sorel (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, namespace = GPXConstants.GPX_NAMESPACE_V11, xmlns = {
    @XmlNs(prefix = "gpx", namespaceURI = GPXConstants.GPX_NAMESPACE_V11)
})
package org.apache.sis.internal.gpx;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
