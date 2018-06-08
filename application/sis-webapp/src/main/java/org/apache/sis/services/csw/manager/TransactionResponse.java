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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author haonguyen
 */
@XmlType(name = "TransactionResponseType", namespace = Namespaces.CSW, propOrder = {    
    "transactionSummary",
    "insertResult",
})
@XmlRootElement(name = "TransactionResponse")
public class TransactionResponse {
    private TransactionSummary transactionSummary;
    private InsertResult insertResult;
    private String version ;
    @XmlElement(name = "TransactionSummary")
    public TransactionSummary getTransactionSummary() {
        return transactionSummary;
    }

    public void setTransactionSummary(TransactionSummary transactionSummary) {
        this.transactionSummary = transactionSummary;
    }
    @XmlElement(name = "InsertResult")
    public InsertResult getInsertResult() {
        return insertResult;
    }

    public void setInsertResult(InsertResult insertResult) {
        this.insertResult = insertResult;
    }
    @XmlAttribute
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
    
}
