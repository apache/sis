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
package org.apache.sis.internal.referencing.provider;

import javax.xml.bind.annotation.XmlTransient;


/**
 * The provider for "<cite>Oblique Mercator</cite>" projection specified by two points on the central line,
 * with easting/northing specified at projection center instead of coordinate system natural origin.
 * This projection is specific to ESRI software.
 *
 * @author  Rueben Schulz (UBC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
@XmlTransient
public final class ObliqueMercatorTwoPointsCenter extends ObliqueMercatorTwoPoints {
    /**
     * For compatibility with different versions during deserialization.
     */
    private static final long serialVersionUID = -4386924772861986539L;

    /**
     * Constructs a new provider.
     */
    public ObliqueMercatorTwoPointsCenter() {
        super("Hotine_Oblique_Mercator_Two_Point_Center",
              ObliqueMercatorCenter.EASTING_AT_CENTRE,
              ObliqueMercatorCenter.NORTHING_AT_CENTRE);
    }
}
