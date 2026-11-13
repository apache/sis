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
import java.util.List;
import org.apache.sis.metadata.iso.identification.DefaultKeywords;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.identification.KeywordType;
import org.opengis.metadata.identification.Keywords;

/**
 * Binding of the {@code MD_Keywords} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#MD_Keywords</a>
 */
@JsonPropertyOrder({
    JSONMDKeywords.PROPERTY_KEYWORD,
    JSONMDKeywords.PROPERTY_TYPE,
    JSONMDKeywords.PROPERTY_THESAURUS_NAME
})
public final class JSONMDKeywords extends DataTransferObject {

    public static final String PROPERTY_KEYWORD = "keyword";
    public static final String PROPERTY_TYPE = "type";
    public static final String PROPERTY_THESAURUS_NAME = "thesaurusName";

    @JsonProperty(PROPERTY_KEYWORD)
    private List<String> keyword;
    @JsonProperty(PROPERTY_TYPE)
    private String type;
    @JsonProperty(PROPERTY_THESAURUS_NAME)
    private JSONMDCitation thesaurusName;

    public JSONMDKeywords() {
    }

    public JSONMDKeywords(final Keywords source, final JSONMDEncoder encoder) {
        keyword       = encoder.texts(source.getKeywords());
        type          = encoder.code(source.getType());
        thesaurusName = (source.getThesaurusName() == null) ? null : new JSONMDCitation(source.getThesaurusName(), encoder);
    }

    public List<String> getKeyword() {
        return keyword;
    }

    public void setKeyword(final List<String> keyword) {
        this.keyword = keyword;
    }

    /**
     * Returns the keyword type code (not a JSON type discriminator).
     */
    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public JSONMDCitation getThesaurusName() {
        return thesaurusName;
    }

    public void setThesaurusName(final JSONMDCitation thesaurusName) {
        this.thesaurusName = thesaurusName;
    }

    public DefaultKeywords toMetadata(final JSONMDDecoder decoder) {
        final DefaultKeywords target = new DefaultKeywords();
        target.setKeywords(decoder.texts(keyword));
        target.setType(decoder.code(KeywordType.class, type, KeywordType::valueOf));
        if (thesaurusName != null) {
            target.setThesaurusName(thesaurusName.toMetadata(decoder));
        }
        return target;
    }
}
