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
package org.apache.sis.storage.geopackage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Logger;

/**
 * Geopackage constants.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Gpkg {

    /**
     * from : <a href="https://www.iana.org/assignments/media-types/media-types.xhtml">https://www.iana.org/assignments/media-types/media-types.xhtml</a>
     */
    public static final String MIME_TYPE = "application/geopackage+sqlite3";

    static final byte[] SIGNATURE = new byte[]{'S','Q','L','i','t','e',' ','f','o','r','m','a','t',' ','3',0x00};

    public static final String TABLE_SPATIAL_REF_SYS = "gpkg_spatial_ref_sys";
    public static final String TABLE_CONTENTS = "gpkg_contents";
    public static final String TABLE_GEOMETRY_COLUMNS = "gpkg_geometry_columns";
    public static final String TABLE_TILE_MATRIX_SET = "gpkg_tile_matrix_set";
    public static final String TABLE_TILE_MATRIX = "gpkg_tile_matrix";
    public static final String TABLE_EXTENSIONS = "gpkg_extensions";

    public static final String DATATYPE_FEATURES = "features";
    public static final String DATATYPE_ATTRIBUTES = "attributes";
    public static final String DATATYPE_TILES = "tiles";

    public static final Logger LOGGER = Logger.getLogger("org.apache.sis.storage.geopackage");

    public static final List<GpkgContentHandler> CONTENT_HANDLERS;

    static {
        final List<GpkgContentHandler> lst = new ArrayList<>();
        final ServiceLoader<GpkgContentHandler> loader = ServiceLoader.load(GpkgContentHandler.class);
        for (GpkgContentHandler handler : loader) {
            lst.add(handler);
        }
        CONTENT_HANDLERS = Collections.unmodifiableList(lst);
    }

    private Gpkg() {}

}
