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
package org.apache.sis.services.catalog.response;

/**
 *
 * @author haonguyen
 */
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;



/**
 * @author annik
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="data")
//@XmlCustomizer(ResDataCustomiser.class)
public class TestType
{
    @XmlAttribute(name="name")
    private String name;

    @XmlValue
    private String value;

    /** Getter.
     * @return the name
     */
    public String getName() {
        return name;
    }

    /** Setter.
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /** Getter.
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /** Setter.
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }
}