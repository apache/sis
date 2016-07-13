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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Thi Phuong Hao NGUYEN
 * @author Minh Chinh VU
 */
@XmlRootElement(namespace = "http://www.opengis.net/ows")
public class BoundingBox {

    private String[] Version;
    private String[] OutputFormat;
    private String parameterName;
    private double westBoundLongitude;
    private double northBoundLatitude;
    private double southBoundLatitude;
    private double eastBoundLongitude;

    @XmlElement(namespace = "http://www.opengis.net/ows", name = "Version")
    public String[] getVersion() {
        return Version;
    }

    public void setVersion(String[] Version) {
        this.Version = Version;
    }

    @XmlElement(namespace = "http://www.opengis.net/ows", name = "westBoundLongitude")
    public double getWestBoundLongitude() {
        return westBoundLongitude;
    }

    public void setWestBoundLongitude(double westBoundLongitude) {
        this.westBoundLongitude = westBoundLongitude;
    }

    @XmlElement(namespace = "http://www.opengis.net/ows", name = "northBoundLatitude")
    public double getNorthBoundLatitude() {
        return northBoundLatitude;
    }

    public void setNorthBoundLatitude(double northBoundLongitude) {
        this.northBoundLatitude = northBoundLongitude;
    }

    @XmlElement(namespace = "http://www.opengis.net/ows", name = "southBoundLatitude")
    public double getSouthBoundLatitude() {
        return southBoundLatitude;
    }

    public void setSouthBoundLatitude(double southBoundLongitude) {
        this.southBoundLatitude = southBoundLongitude;
    }

    @XmlElement(namespace = "http://www.opengis.net/ows", name = "eastBoundLongitude")
    public double getEastBoundLongitude() {
        return eastBoundLongitude;
    }

    public void setEastBoundLongitude(double eastBoundLongitude) {
        this.eastBoundLongitude = eastBoundLongitude;
    }

    @XmlElement(namespace = "http://www.opengis.net/ows", name = "OutputFormat")
    public String[] getOutputFormat() {
        return OutputFormat;
    }

    public void setOutputFormat(String[] OutputFormat) {
        this.OutputFormat = OutputFormat;
    }

    @XmlElement(namespace = "http://www.opengis.net/ows", name = "parameterName")
    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

}
