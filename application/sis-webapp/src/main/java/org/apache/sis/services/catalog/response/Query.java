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
public class Query {
    private String resultSetID;
    private String resultType;
    private String retrievedData;
    private int cursorPosition;
    private int hits;

    /**
     * Get result ID 
     * @return Id Result
     */
    public String getResultSetID() {
        return resultSetID;
    }

    /**
     * Set result ID
     * @param resultSetID
     */
    public void setResultSetID(String resultSetID) {
        this.resultSetID = resultSetID;
    }

    /**
     * Get type the server responded to the query request
     * @return CodeList type with allowed values of "dataset","datasetcollection"
     * and "service"
     */
    public String getResultType() {
        return resultType;
    }

    /**
     * Set type the server responded to the query request
     * @param resultType
     */
    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    /**
     * Get a subset of the results of this query request, organised and formatted as
     * specified in the presentation, messageFormat, and sortField parameters.
     * @return Data Set of resource descriptions/records
     */
    public String getRetrievedData() {
        return retrievedData;
    }

    /**
     * Set a subset of the results of this query request, organised and formatted as
     * specified in the presentation, messageFormat, and sortField parameters.
     * @param retrievedData
     */
    public void setRetrievedData(String retrievedData) {
        this.retrievedData = retrievedData;
    }

    /**
     * Get last result set resource returned for this operation request.
     * @return Positive
     */
    public int getCursorPosition() {
        return cursorPosition;
    }

    /**
     * Set last result set resource returned for this operation request.
     * @param cursorPosition
     */
    public void setCursorPosition(int cursorPosition) {
        this.cursorPosition = cursorPosition;
    }

    /**
     * Get number of entries in the result set.
     * @return hits (Non-negative integer)
     */
    public int getHits() {
        return hits;
    }

    /**
     * Set number of entries in the result set.
     * @param hits
     */
    public void setHits(int hits) {
        this.hits = hits;
    }
    
}
