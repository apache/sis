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
package org.apache.sis.storage.coveragejson.binding;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
 * A CoverageJSON object with the type "Coverage" is a coverage object.
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonPropertyOrder({"type","id","domain","parameters","parameterGroups","ranges"})
public final class Coverage extends CoverageJsonObject {
    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * If a coverage has a commonly used identifier, that identifier SHOULD be
     * included as a member of the coverage object with the name "id".
     */
    public String id;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * A coverage object MUST have a member with the name "domain" where the
     * value is either a domain object or a URL.
     *
     * If the value of "domain" is a URL and the referenced domain has a
     * "domainType" member, then the coverage object SHOULD have the member
     * "domainType" where the value MUST equal that of the referenced domain.
     *
     * If the coverage object is part of a coverage collection which has a
     * "domainType" member then that member SHOULD be omitted in the coverage
     * object.
     */
    @JsonDeserialize(using = DomainDeserializer.class)
    public Object domain;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * A coverage object MAY have a member with the name "parameters" where the
     * value is an object where each member has as name a short identifier and
     * as value a parameter object. The identifier corresponds to the commonly
     * known concept of “variable name” and is merely used in clients for
     * conveniently accessing the corresponding range object.
     *
     * A coverage object MUST have a "parameters" member if the coverage object
     * is not part of a coverage collection or if the coverage collection does
     * not have a "parameters" member.
     */
    public Map<String,Parameter> parameters;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * A coverage object MAY have a member with the name "parameterGroups" where
     * the value is an array of ParameterGroup objects.
     */
    public List<ParameterGroup> parameterGroups;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * A coverage object MUST have a member with the name "ranges" where the
     * value is a range set object. Any member of a range set object has as
     * name any of the names in a "parameters" object in scope and as value
     * either an NdArray or TiledNdArray object or a URL resolving to a
     * CoverageJSON document of such object. A "parameters" member in scope is
     * either within the enclosing coverage object or, if part of a coverage
     * collection, in the parent coverage collection object. The shape and axis
     * names of each NdArray or TiledNdArray object MUST correspond to the
     * domain axes defined by "domain", while single-valued axes MAY be omitted.
     * If the referenced parameter object has a "categoryEncoding" member, then
     * each non-null array element of the "values" member of the NdArray object,
     * or the linked NdArray objects within a TiledNdArray object, MUST be equal
     * to one of the values defined in the "categoryEncoding" object and be
     * interpreted as the matching category.
     */
    public Map<String,NdArray> ranges;

    public Coverage() {
        type = "Coverage";
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof Coverage)) return false;

        final Coverage cdt = ((Coverage) other);
        return super.equals(other)
            && Objects.equals(id, cdt.id)
            && Objects.equals(domain, cdt.domain)
            && Objects.equals(parameters, cdt.parameters)
            && Objects.equals(parameterGroups, cdt.parameterGroups)
            && Objects.equals(ranges, cdt.ranges);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(
                id,
                domain,
                parameters,
                parameterGroups,
                ranges);
    }

    public static class DomainDeserializer extends JsonDeserializer<Object> {
        public DomainDeserializer() {
        }

        @Override
        public Object deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException, JacksonException {
            final JsonNode rootNode = ctxt.readTree(parser);

            if (rootNode.isTextual()) {
                return rootNode.textValue();
            } else if (rootNode.isObject()) {
                return ctxt.readTreeAsValue(rootNode, Domain.class);
            } else {
                throw new IOException("Unexpected json element");
            }
        }
    }
}
