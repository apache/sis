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

import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.util.List;
import java.util.Objects;

/**
 * A CoverageJSON object with the type "CoverageCollection" is a coverage collection object.
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonbNillable(false)
@JsonbPropertyOrder({"type","domainType","parameters","parameterGroups","referencing","coverages"})
public final class CoverageCollection extends CoverageJsonObject {

    /**
     * A coverage collection object MAY have the member "domainType" with a
     * string value to indicate that the coverage collection only contains
     * coverages of the given domain type. See the section Common Domain Types
     * for details. Custom domain types may be used as recommended in the
     * section Extensions.
     *
     * If a coverage collection object has the member "domainType", then this
     * member is inherited to all included coverages.
     */
    public String domainType;
    /**
     * A coverage collection object MAY have a member with the name "parameters"
     * where the value is an object where each member has as name a short
     * identifier and as value a parameter object.
     */
    public Parameter parameters;
    /**
     * A coverage collection object MAY have a member with the name
     * "parameterGroups" where the value is an array of ParameterGroup objects.
     */
    public List<ParameterGroup> parameterGroups;
    /**
     * A coverage collection object MAY have a member with the name "referencing"
     * where the value is an array of reference system connection objects.
     */
    public List<ReferenceSystemConnection> referencing;
    /**
     * A coverage collection object MUST have a member with the name "coverages".
     * The value corresponding to "coverages" is an array. Each element in the
     * array is a coverage object as defined above.
     */
    public List<Coverage> coverages;

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof CoverageCollection)) return false;

        final CoverageCollection cdt = ((CoverageCollection) other);
        return super.equals(other)
            && Objects.equals(domainType, cdt.domainType)
            && Objects.equals(parameters, cdt.parameters)
            && Objects.equals(coverages, cdt.coverages)
            && Objects.equals(parameterGroups, cdt.parameterGroups)
            && Objects.equals(referencing, cdt.referencing);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(
                domainType,
                coverages,
                parameters,
                parameterGroups,
                referencing);
    }
}
