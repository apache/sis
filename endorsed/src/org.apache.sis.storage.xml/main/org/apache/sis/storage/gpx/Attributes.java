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
 * GPX attribute names in XML files.
 * Contrarily to {@link Tags}, attributes in GPX files have no namespace.
 * Unless otherwise noticed by a "(v1.0)" or "(v1.1)" text in the javadoc,
 * attributes in this class apply to all supported GPX versions.
 *
 * @author  Johann Sorel (Geomatys)
 */
final class Attributes {
    /** A main GPX attribute.           */ static final String VERSION   = "version";
    /** A main GPX attribute.           */ static final String CREATOR   = "creator";
    /** A copyright attribute (v1.1+).  */ static final String AUTHOR    = "author";
    /** A bounds attribute.             */ static final String MIN_X     = "minlon";
    /** A bounds attribute.             */ static final String MAX_X     = "maxlon";
    /** A bounds attribute.             */ static final String MIN_Y     = "minlat";
    /** A bounds attribute.             */ static final String MAX_Y     = "maxlat";
    /** A link attribute (v1.1+).       */ static final String HREF      = "href";
    /** A way point attribute.          */ static final String LATITUDE  = "lat";
    /** A way point attribute.          */ static final String LONGITUDE = "lon";

    /**
     * Do not allow instantiation of this class.
     */
    private Attributes() {
    }
}
