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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.sis.storage.json.DataTransferObject;


/**
 * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
 * A CoverageJSON document can be extended with custom members and types in a
 * robust and interoperable way. For that, it makes use of absolute URIs and
 * compact URIs (prefix:suffix) in order to avoid conflicts with other extensions
 * and future versions of the format. A central registry of compact URI prefixes
 * is provided which anyone can extend and which is a simple mapping from compact
 * URI prefix to namespace URI in order to avoid collisions with other extensions
 * that are based on compact URIs as well. Extensions that do not follow this
 * approach MAY use simple names instead of absolute or compact URIs but have to
 * accept the consequence of the document being less interoperable and future-proof.
 * In certain use cases this is not an issue and may be a preferred solution for
 * simplicity reasons, for example, if such CoverageJSON documents are only used
 * internally and are not meant to be shared to a wider audience.
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes ( {
    @Type(name = "Coverage", value = Coverage.class),
    @Type(name = "CoverageCollection", value = CoverageCollection.class),
    @Type(name = "Domain", value = Domain.class),
    @Type(name = "NdArray", value = NdArray.class),
    @Type(name = "Parameter", value = Parameter.class),
    @Type(name = "ParameterGroup", value = ParameterGroup.class),

    //system subtypes
    @Type(name = "GeographicCRS", value = GeographicCRS.class),
    @Type(name = "ProjectedCRS", value = ProjectedCRS.class),
    @Type(name = "IdentifierRS", value = IdentifierRS.class),
    @Type(name = "VerticalCRS", value = VerticalCRS.class),
    @Type(name = "TemporalRS", value = TemporalRS.class)
})
public class CoverageJsonObject extends DataTransferObject {

    public String type;

    public CoverageJsonObject() {
    }
}
