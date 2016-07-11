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
package org.apache.sis.services.csw;

import org.opengis.metadata.extent.GeographicBoundingBox;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * Bounding box for identifying a geographic area of interest.
 *
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @since   0.8
 * @version 0.8
 * @module
 */
@XmlRootElement(namespace = Element.OWS, name = "BoundingBox")
final class BoundingBox extends Element {
    /**
     * A space-separated list of minimal coordinate values for each dimension.
     */
    @XmlElement(namespace = Element.OWS, name = "LowerCorner")
    private String lowerCorner;

    /**
     * A space-separated list of maximal coordinate values for each dimension.
     */
    @XmlElement(namespace = Element.OWS, name = "UpperCorner")
    private String upperCorner;

    /**
     * Creates a new, initially empty, bounding box.
     * This constructor is invoked by JAXB at unmarshalling time.
     */
    BoundingBox() {
    }

    /**
     * Creates a new bounding box initialized to the values given by an ISO 19115 geographic extent.
     * This constructor is invoked before marshalling with JAXB.
     */
    BoundingBox(final GeographicBoundingBox bbox) {
        final StringBuilder buffer = new StringBuilder(20);
        lowerCorner = format(buffer, bbox.getWestBoundLongitude(), bbox.getSouthBoundLatitude());
        upperCorner = format(buffer, bbox.getEastBoundLongitude(), bbox.getNorthBoundLatitude());
    }

    /**
     * Formats a corner.
     */
    private static String format(final StringBuilder buffer, final double min, final double max) {
        final String coord = buffer.append(min).append(' ').append(max).toString();
        buffer.setLength(0);
        return coord;
    }
}
