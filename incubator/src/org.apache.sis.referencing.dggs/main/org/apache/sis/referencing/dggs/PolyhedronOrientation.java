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
package org.apache.sis.referencing.dggs;


/**
 * Orientation parameters of the base polyhedron.
 *
 * @author Johann Sorel (Geomatys)
 * @see https://docs.ogc.org/as/20-040r3/20-040r3.html#toc34
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#annex-dggrs-def
 */
public final class PolyhedronOrientation {

    private final double latitude;
    private final double longitude;
    private final double azimuth;
    private final String description;

    public PolyhedronOrientation(double latitude, double longitude, double azimuth, String description) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.azimuth = azimuth;
        this.description = description;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAzimuth() {
        return azimuth;
    }

    public String getDescription() {
        return description;
    }

}
