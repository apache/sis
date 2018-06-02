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
package org.apache.sis.services.catalog;

import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author haonguyen
 */
@XmlType(name = "BoundingBox", namespace = Namespaces.OWS, propOrder = {
    "lowerCorner",
    "upperCorner",
    
})
@XmlRootElement(name = "BoundingBox", namespace = Namespaces.OWS)
public class BoundingBox {
    private List<Double> lowerCorner;
    private List<Double> upperCorner;
    private String crs;
    private String dimensions;
    @XmlElement(name = "LowerCorner",namespace=Namespaces.OWS)
    @XmlList
    public List<Double> getLowerCorner() {
            return lowerCorner;
    }

    public void setLowerCorner(List<Double> lowerCorner) {
            this.lowerCorner = lowerCorner;
    }
    @XmlElement(name = "UpperCorner",namespace=Namespaces.OWS)
    @XmlList
    public List<Double> getUpperCorner() {
            return upperCorner;
    }

    public void setUpperCorner(List<Double> upperCorner) {
            this.upperCorner = upperCorner;
    }

    
    
}
