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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import org.opengis.metadata.extent.GeographicBoundingBox;

/**
 * @author Thi Phuong Hao NGUYEN
 * @author Minh Chinh VU
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"LowerCorner", "UpperCorner"})

public class BoundingBox {

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
     * Return a space-separated list of minimal coordinate values for each
     * dimension.
     */
    public String getLowerCorner() {
        return lowerCorner;
    }

    /**
     * Set a space-separated list of minimal coordinate values for each
     * dimension.
     */
    public void setLowerCorner(String LowerCorner) {
        this.lowerCorner = LowerCorner;
    }

    /**
     * Return a space-separated list of maximal coordinate values for each
     * dimension.
     */
    public void setUpperCorner(String UpperCorner) {
        this.upperCorner = UpperCorner;
    }

    /**
     * Set a space-separated list of maximal coordinate values for each
     * dimension.
     */
    public String getUpperCorner() {
        return upperCorner;
    }

    /**
     * Contructor.
     */
    BoundingBox() {
    }

    /**
     * Creates a new bounding box initialized to the values given by an ISO
     * 19115 geographic extent. This constructor is invoked before marshalling
     * with JAXB.
     */
    BoundingBox(final GeographicBoundingBox bbox) {
        final StringBuilder buffer = new StringBuilder(20);
        this.lowerCorner = format(buffer, bbox.getWestBoundLongitude(), bbox.getSouthBoundLatitude());
        this.upperCorner = format(buffer, bbox.getEastBoundLongitude(), bbox.getNorthBoundLatitude());
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
