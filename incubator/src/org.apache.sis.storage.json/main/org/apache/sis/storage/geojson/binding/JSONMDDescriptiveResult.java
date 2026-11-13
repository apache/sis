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
package org.apache.sis.storage.geojson.binding;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.sis.metadata.iso.quality.DefaultDescriptiveResult;
import org.opengis.metadata.quality.DescriptiveResult;

/**
 * Binding of the {@code DescriptiveResult} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19157/-1/dqc/1.0.0/dqc.json">dqc.json#DescriptiveResult</a>
 */
@JsonPropertyOrder({
    JSONMDQualityResult.PROPERTY_TYPE,
    JSONMDQualityResult.PROPERTY_DATE_TIME,
    JSONMDQualityResult.PROPERTY_RESULT_SCOPE,
    JSONMDDescriptiveResult.PROPERTY_STATEMENT
})
public final class JSONMDDescriptiveResult extends JSONMDQualityResult {

    /**
     * Value of the {@code type} member.
     */
    public static final String TYPE = "DescriptiveResult";
    public static final String PROPERTY_STATEMENT = "statement";

    @JsonProperty(PROPERTY_STATEMENT)
    private String statement;

    public JSONMDDescriptiveResult() {
    }

    public JSONMDDescriptiveResult(final DescriptiveResult source, final JSONMDEncoder encoder) {
        super(source, encoder);
        statement = encoder.text(source.getStatement());
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(final String statement) {
        this.statement = statement;
    }

    @Override
    public DefaultDescriptiveResult toMetadata(final JSONMDDecoder decoder) {
        final DefaultDescriptiveResult target = new DefaultDescriptiveResult();
        fill(target, decoder);
        target.setStatement(decoder.text(statement));
        return target;
    }
}
