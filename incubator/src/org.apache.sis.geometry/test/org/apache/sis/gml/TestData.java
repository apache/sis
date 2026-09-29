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
package org.apache.sis.gml;

import java.net.URL;
import java.io.InputStream;


/**
 * Identification of the data to use for testing purpose.
 *
 * @author  Johann Sorel (Geomatys)
 */
public enum TestData {
    /**
     * Test for GML 2.0.
     */
    V2("2/"),

    /**
     * Test for GML 3 (3.0, 3.1 and 3.2 together).
     */
    V3("3/");

    /**
     * The directory (relative to the {@code TestData.class} file) of the XML document.
     */
    private final String directory;

    static final String POINT             = "point.gml";
    static final String POINT_COORD       = "point_coord.gml";
    static final String LINE_STRING       = "linestring.gml";
    static final String LINE_STRING_COORD = "linestring_coord.gml";
    static final String LINEAR_RING       = "linearring.gml";
    static final String POLYGON           = "polygon.gml";
    static final String BOX               = "box.gml";
    static final String MULTI_POINT       = "multipoint.gml";
    static final String MULTI_LINE_STRING = "multilinestring.gml";
    static final String MULTI_POLYGON     = "multipolygon.gml";
    static final String MULTI_GEOMETRY    = "multigeometry.gml";
    static final String NO_NAMESPACE      = "no_namespace.gml";

    // --- GML 3-specific test data files (directory V3 only) ---
    static final String ENVELOPE                        = "envelope.gml";
    static final String LINE_STRING_POSLIST             = "linestring_poslist.gml";
    static final String POLYGON_EXTERIOR                = "polygon_exterior.gml";
    static final String MULTI_POINT_MEMBERS             = "multipoint_pointmembers.gml";
    static final String MULTI_CURVE                     = "multicurve.gml";
    static final String MULTI_SURFACE                   = "multisurface.gml";
    static final String TOLERANCE_NEW_STYLE_OLD_NAMESPACE    = "tolerance_new_style_old_namespace.gml";
    static final String TOLERANCE_LEGACY_STYLE_NEW_NAMESPACE = "tolerance_legacy_style_new_namespace.gml";
    static final String CURVE                = "curve.gml";
    static final String CURVE_ARC            = "curve_arc.gml";
    static final String CURVE_ARC_BY_CENTER  = "curve_arcbycenterpoint.gml";
    static final String CURVE_ARC_BY_BULGE   = "curve_arcbybulge.gml";
    static final String SURFACE              = "surface.gml";
    static final String RING                 = "ring.gml";
    static final String COMPOSITE_CURVE      = "compositecurve.gml";
    static final String COMPOSITE_SURFACE    = "compositesurface.gml";
    static final String ORIENTABLE_CURVE     = "orientablecurve.gml";
    static final String ORIENTABLE_SURFACE   = "orientablesurface.gml";
    static final String SOLID                = "solid.gml";
    static final String COMPOSITE_SOLID      = "compositesolid.gml";

    /**
     * Creates a new enumeration for documents in the specified sub-directory.
     */
    private TestData(final String directory) {
        this.directory = directory;
    }

    /**
     * Returns the URL to the specified XML file.
     *
     * @param  filename  name of the file to open.
     * @return URL to the XML document to use for testing purpose.
     */
    public final URL getURL(final String filename) {
        // Call to `getResource(…)` is caller sensitive: it must be in the same module.
        return TestData.class.getResource(directory.concat(filename));
    }

    /**
     * Opens the stream to the specified XML file.
     *
     * @param  filename  name of the file to open.
     * @return stream opened on the XML document to use for testing purpose.
     */
    public final InputStream openStream(final String filename) {
        // Call to `getResourceAsStream(…)` is caller sensitive: it must be in the same module.
        return TestData.class.getResourceAsStream(directory.concat(filename));
    }
}
