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
package org.apache.sis.storage.json;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonInclude(Include.NON_EMPTY)
public abstract class DataTransferObject {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Catch all unknown fields
     */
    protected final Map<String, Object> unknownFields = new HashMap<>();

    /**
     * All unknown fields
     */
    @JsonAnyGetter
    public Map<String, Object> otherFields() {
        return unknownFields;
    }

    @JsonAnySetter
    public void setOtherField(String name, Object value) {
        unknownFields.put(name, value);
    }

    /**
     * Create a copy of this object.
     * The copy is created by transforming the object to a JsonNode
     * and back to a class instance using jackson Object mapper.
     *
     * @return json copy of this object.
     * @throws JsonProcessingException
     */
    public DataTransferObject copy() throws JsonProcessingException {
        return MAPPER.treeToValue(MAPPER.valueToTree(this), this.getClass());
    }

    /**
     * Compare objects based on there JSON representation using jackson mapper.
     * Objects are transformed to JSONNode and compared using JsonNode.equals
     *
     * @param obj to compare
     * @return true if objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().equals(obj.getClass())) return false;
        final JsonNode node1 = MAPPER.valueToTree(obj);
        final JsonNode node2 = MAPPER.valueToTree(this);
        return node1.equals(node2);
    }

    /**
     * @return class hashcode
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public final String toString() {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException ex) {
            return this.getClass().getName() + " : [Error: could not map class instance as json]";
        }
    }

}
