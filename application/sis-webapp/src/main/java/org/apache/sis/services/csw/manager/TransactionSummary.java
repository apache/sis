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
package org.apache.sis.services.csw.manager;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author haonguyen
 */
@XmlType(name = "TransactionSummaryType", namespace = Namespaces.CSW, propOrder = {    
    "totalInserted",
    "totalUpdated",
    "totalDeleted",
})
@XmlRootElement(name = "TransactionSummary")
public class TransactionSummary {
    private int totalInserted;
    private int totalUpdated;
    private int totalDeleted;
    @XmlElement
    public int getTotalInserted() {
        return totalInserted;
    }

    public void setTotalInserted(int totalInserted) {
        this.totalInserted = totalInserted;
    }
    @XmlElement
    public int getTotalUpdated() {
        return totalUpdated;
    }

    public void setTotalUpdated(int totalUpdated) {
        this.totalUpdated = totalUpdated;
    }
    @XmlElement
    public int getTotalDeleted() {
        return totalDeleted;
    }

    public void setTotalDeleted(int totalDeleted) {
        this.totalDeleted = totalDeleted;
    }
    
}
