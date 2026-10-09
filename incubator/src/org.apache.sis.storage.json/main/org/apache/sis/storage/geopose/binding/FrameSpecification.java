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
package org.apache.sis.storage.geopose.binding;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Specification of a GeoPose reference frame.
 *
 * <p>For the {@code "/geopose/1.0"} authority and the {@code "LTP-ENU"} identifier, the
 * parameter string locates the origin of a local tangent plane east-north-up frame, as in
 * {@code "longitude=-122.3000000&latitude=47.7000000&height=11.000"}. That string is parsed by
 * {@code org.apache.sis.storage.geopose.FrameSpecificationParser}.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0, definition FrameSpecification"
 */
@JsonPropertyOrder({
    FrameSpecification.JSON_PROPERTY_AUTHORITY,
    FrameSpecification.JSON_PROPERTY_ID,
    FrameSpecification.JSON_PROPERTY_PARAMETERS
})
public class FrameSpecification extends SpecificationReference {

    public FrameSpecification() {
    }

    public FrameSpecification(final String authority, final String id, final String parameters) {
        super(authority, id, parameters);
    }
}
