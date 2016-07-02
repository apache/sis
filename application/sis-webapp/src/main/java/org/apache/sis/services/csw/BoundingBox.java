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
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;


/**
 * Bounding box for identifying a geographic area of interest.
 * This class has the same semantic than the {@code GeographicBoundingBox} class in ISO 19115.
 *
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @since   0.8
 * @version 0.8
 * @module
 */
@XmlRootElement(namespace = Element.OWS, name = "BoundingBox")
final class BoundingBox extends Element implements GeographicBoundingBox {
    /**
     * The ISO 19115 geographic bounding box.
     */
    private final DefaultGeographicBoundingBox extent;

    private String[] version;
    private String[] outputFormat;
    private String parameterName;

    /**
     * Creates a new, initially empty, bounding box.
     */
    BoundingBox() {
        extent = new DefaultGeographicBoundingBox();
    }

    /**
     * Returns the western-most coordinate of the limit of the resource's extent,
     * expressed in longitude in decimal degrees (positive east).
     */
    @Override
    @XmlElement(name = "westBoundLongitude")
    public double getWestBoundLongitude() {
        return extent.getWestBoundLongitude();
    }

    /**
     * Sets the western-most coordinate of the limit of the resource's extent.
     */
    public void setWestBoundLongitude(double westBoundLongitude) {
        extent.setWestBoundLongitude(westBoundLongitude);
    }

    /**
     * Returns the southern-most coordinate of the limit of the resource's extent,
     * expressed in latitude in decimal degrees (positive north).
     */
    @Override
    @XmlElement(name = "southBoundLatitude")
    public double getSouthBoundLatitude() {
        return extent.getSouthBoundLatitude();
    }

    /**
     * Sets the southern-most coordinate of the limit of the resource's extent.
     */
    public void setSouthBoundLatitude(double southBoundLatitude) {
        extent.setSouthBoundLatitude(southBoundLatitude);
    }

    /**
     * Returns the eastern-most coordinate of the limit of the resource's extent,
     * expressed in longitude in decimal degrees (positive east).
     */
    @Override
    @XmlElement(name = "eastBoundLongitude")
    public double getEastBoundLongitude() {
        return extent.getEastBoundLongitude();
    }

    /**
     * Sets the eastern-most coordinate of the limit of the resource's extent.
     */
    public void setEastBoundLongitude(double eastBoundLongitude) {
        extent.setEastBoundLongitude(eastBoundLongitude);
    }

    /**
     * Returns the northern-most, coordinate of the limit of the resource's extent,
     * expressed in latitude in decimal degrees (positive north).
     */
    @Override
    @XmlElement(name = "northBoundLatitude")
    public double getNorthBoundLatitude() {
        return extent.getNorthBoundLatitude();
    }

    /**
     * Sets the northern-most, coordinate of the limit of the resource's extent.
     */
    public void setNorthBoundLongitude(double northBoundLatitude) {
        extent.setNorthBoundLatitude(northBoundLatitude);
    }

    /**
     * Indication of whether the bounding polygon encompasses an area covered by the data
     * (<cite>inclusion</cite>) or an area where data is not present (<cite>exclusion</cite>).
     * For CSW bounding box, this property should always be {@link Boolean#TRUE}.
     */
    @Override
    public Boolean getInclusion() {
        return extent.getInclusion();
    }

    @XmlElement(name = "Version")
    public String[] getVersion() {
        return version;
    }

    public void setVersion(final String[] version) {
        this.version = version;
    }

    @XmlElement(name = "OutputFormat")
    public String[] getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(final String[] outputFormat) {
        this.outputFormat = outputFormat;
    }

    @XmlElement(name = "parameterName")
    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }
}
